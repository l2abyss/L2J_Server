/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.l2jserver.gameserver.network;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javolution.util.FastList;

import org.mmocore.network.MMOClient;
import org.mmocore.network.MMOConnection;

import com.l2jserver.Config;
import com.l2jserver.L2DatabaseFactory;
import com.l2jserver.gameserver.LoginServerThread;
import com.l2jserver.gameserver.ThreadPoolManager;
import com.l2jserver.gameserver.LoginServerThread.SessionKey;
import com.l2jserver.gameserver.datatables.CharNameTable;
import com.l2jserver.gameserver.datatables.ClanTable;
import com.l2jserver.gameserver.model.CharSelectInfoPackage;
import com.l2jserver.gameserver.model.L2Clan;
import com.l2jserver.gameserver.model.L2World;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.entity.L2Event;
import com.l2jserver.gameserver.model.entity.TvTEvent;
import com.l2jserver.gameserver.network.serverpackets.L2GameServerPacket;
import com.l2jserver.gameserver.network.serverpackets.ServerClose;
import com.l2jserver.gameserver.util.Util;
import com.l2jserver.util.EventData;

/**
 * Represents a client connected on Game Server
 * @author  KenM
 */
public final class L2GameClient extends MMOClient<MMOConnection<L2GameClient>>
{
	protected static final Logger _log = Logger.getLogger(L2GameClient.class.getName());
	protected static final Logger _logAccounting = Logger.getLogger("accounting");

	/**
	 * CONNECTED	- client has just connected
	 * AUTHED		- client has authed but doesnt has character attached to it yet
	 * IN_GAME		- client has selected a char and is in game
	 * @author  KenM
	 */
	public static enum GameClientState { CONNECTED, AUTHED, IN_GAME }

	public GameClientState state;

	// Info
    private String _accountName;
    private SessionKey _sessionId;
	private L2PcInstance _activeChar;
	private ReentrantLock _activeCharLock = new ReentrantLock();

	private boolean _isAuthedGG;
	private long _connectionStartTime;
	private List<Integer> _charSlotMapping = new FastList<Integer>();

	// Task
	protected final ScheduledFuture<?> _autoSaveInDB;
	protected ScheduledFuture<?> _cleanupTask = null;

	// Crypt
	private GameCrypt _crypt;

	// Flood protection
	public byte packetsSentInSec = 0;
	public int packetsSentStartTick = 0;
	
	public byte underflowReadsInMin = 0;
	public int underflowReadStartTick = 0;
	
	private boolean _isDetached = false;

	private boolean _protocol;

	public L2GameClient(MMOConnection<L2GameClient> con)
	{
		super(con);
		state = GameClientState.CONNECTED;
		_connectionStartTime = System.currentTimeMillis();
        _crypt = new GameCrypt();
        
        if (Config.CHAR_STORE_INTERVAL > 0)
        {
            _autoSaveInDB = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate( 
                    new AutoSaveTask(), 300000L, (Config.CHAR_STORE_INTERVAL*60000L)
            );
        }
        else
        {
            _autoSaveInDB = null;
        }
	}

	public byte[] enableCrypt()
	{
		byte[] key = BlowFishKeygen.getRandomKey();
        _crypt.setKey(key);
		return key;
	}

	public GameClientState getState()
	{
		return state;
	}

	public void setState(GameClientState pState)
	{
		state = pState;
	}

	public long getConnectionStartTime()
	{
		return _connectionStartTime;
	}

	@Override
	public boolean decrypt(ByteBuffer buf, int size)
	{
        _crypt.decrypt(buf.array(), buf.position(), size);
		return true;
	}

	@Override
	public boolean encrypt(final ByteBuffer buf, final int size)
	{
        _crypt.encrypt(buf.array(), buf.position(), size);
		buf.position(buf.position() + size);
		return true;
	}

	public L2PcInstance getActiveChar()
	{
		return _activeChar;
	}

	public void setActiveChar(L2PcInstance pActiveChar)
	{
		_activeChar = pActiveChar;
		if (_activeChar != null)
		{
			L2World.getInstance().storeObject(getActiveChar());
		}
	}

	public ReentrantLock getActiveCharLock()
	{
		return _activeCharLock;
	}

	public void setGameGuardOk(boolean val)
	{
		_isAuthedGG = val;
	}
    
    public boolean isAuthedGG()
    {
        return _isAuthedGG;
    }

	public void setAccountName(String pAccountName)
	{
		_accountName = pAccountName;
	}

	public String getAccountName()
	{
		return _accountName;
	}

	public void setSessionId(SessionKey sk)
	{
		_sessionId = sk;
	}

	public SessionKey getSessionId()
	{
		return _sessionId;
	}

	public void sendPacket(L2GameServerPacket gsp)
	{
		if (_isDetached) return;

		// Packets from invisible chars sends only to GMs
		if (gsp.isInvisible() && getActiveChar() != null && !getActiveChar().isGM())
			return;

		getConnection().sendPacket(gsp);
		gsp.runImpl();
	}
	
	public boolean isDetached()
	{
		return _isDetached;
	}
	
	public void isDetached(boolean b)
	{
		_isDetached = b;
	}

	/**
	 * Method to handle character deletion
	 * 
	 * @return a byte:
	 * <li>-1: Error: No char was found for such charslot, caught exception, etc...
	 * <li> 0: character is not member of any clan, proceed with deletion
	 * <li> 1: character is member of a clan, but not clan leader
	 * <li> 2: character is clan leader
	 */
	public byte markToDeleteChar(int charslot)
	{
		int objid = getObjectIdForSlot(charslot);
		
		if (objid < 0)
		    return -1;

		Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			
			PreparedStatement statement = con.prepareStatement("SELECT clanId FROM characters WHERE charId=?");
			statement.setInt(1, objid);			
			ResultSet rs = statement.executeQuery();
			
			rs.next();
			
			int clanId = rs.getInt(1);
			byte answer = 0;
			if (clanId != 0)
			{
				L2Clan clan = ClanTable.getInstance().getClan(clanId);
				
				if (clan == null)
					answer = 0; // jeezes!
				else if (clan.getLeaderId() == objid)
					answer = 2;
				else 
					answer = 1;
			}
			
			rs.close();
			statement.close();
			
		    // Setting delete time
			if (answer == 0)
			{
				if (Config.DELETE_DAYS == 0)
					deleteCharByObjId(objid);
				else
				{
					statement = con.prepareStatement("UPDATE characters SET deletetime=? WHERE charId=?");
					statement.setLong(1, System.currentTimeMillis() + Config.DELETE_DAYS*86400000L); // 24*60*60*1000 = 86400000
					statement.setInt(2, objid);
					statement.execute();
					statement.close();
				}

				LogRecord record = new LogRecord(Level.WARNING, "Delete");
		    	record.setParameters(new Object[]{objid, L2GameClient.this});
				_logAccounting.log(record);
			}
			
			return answer;
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Error updating delete time of character.", e);
			return -1;
		}
		finally
		{
			try { con.close(); } catch (Exception e) {}
		}
	}

	/**
	 * Save the L2PcInstance to the database.
	 */
	public static void saveCharToDisk(L2PcInstance cha, boolean storeItems)
	{
		try
		{
			cha.store();
			if (Config.UPDATE_ITEMS_ON_CHAR_STORE && storeItems)
			{
				cha.getInventory().updateDatabase();
				cha.getWarehouse().updateDatabase();
			}
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Error saving character..", e);
		}
	}

	public void markRestoredChar(int charslot) throws Exception
	{
		//have to make sure active character must be nulled
		/*if (getActiveChar() != null)
		{
			saveCharToDisk (getActiveChar());
			if (Config.DEBUG) _log.fine("active Char saved");
			this.setActiveChar(null);
		}*/

		int objid = getObjectIdForSlot(charslot);
    		if (objid < 0)
    		    return;
		Connection con = null;
		try
		{
		con = L2DatabaseFactory.getInstance().getConnection();
		PreparedStatement statement = con.prepareStatement("UPDATE characters SET deletetime=0 WHERE charId=?");
		statement.setInt(1, objid);
		statement.execute();
		statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Error restoring character.", e);
		}
		finally
		{
			try { con.close(); } catch (Exception e) {}
		}

		LogRecord record = new LogRecord(Level.WARNING, "Restore");
    	record.setParameters(new Object[]{objid, L2GameClient.this});
		_logAccounting.log(record);
	}




	public static void deleteCharByObjId(int objid)
	{
	    if (objid < 0)
	        return;
	    
	    CharNameTable.getInstance().removeName(objid);

	    Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement ;

        	statement = con.prepareStatement("DELETE FROM character_friends WHERE charId=? OR friendId=?");
			statement.setInt(1, objid);
			statement.setInt(2, objid);
			statement.execute();
			statement.close();

            statement = con.prepareStatement("DELETE FROM character_hennas WHERE charId=?");
            statement.setInt(1, objid);
            statement.execute();
            statement.close();

			statement = con.prepareStatement("DELETE FROM character_macroses WHERE charId=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();

			statement = con.prepareStatement("DELETE FROM character_quests WHERE charId=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();
			
			statement = con.prepareStatement("DELETE FROM character_quest_global_data WHERE charId=?");
			statement.setInt(1, objid);
			statement.executeUpdate();
			statement.close();

			statement = con.prepareStatement("DELETE FROM character_recipebook WHERE charId=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();

			statement = con.prepareStatement("DELETE FROM character_shortcuts WHERE charId=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();

			statement = con.prepareStatement("DELETE FROM character_skills WHERE charId=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();

			statement = con.prepareStatement("DELETE FROM character_skills_save WHERE charId=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();

			statement = con.prepareStatement("DELETE FROM character_subclasses WHERE charId=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();

            statement = con.prepareStatement("DELETE FROM heroes WHERE charId=?");
            statement.setInt(1, objid);
            statement.execute();
            statement.close();

            statement = con.prepareStatement("DELETE FROM olympiad_nobles WHERE charId=?");
            statement.setInt(1, objid);
            statement.execute();
            statement.close();

            statement = con.prepareStatement("DELETE FROM seven_signs WHERE charId=?");
            statement.setInt(1, objid);
            statement.execute();
            statement.close();

        	statement = con.prepareStatement("DELETE FROM pets WHERE item_obj_id IN (SELECT object_id FROM items WHERE items.owner_id=?)");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();

			statement = con.prepareStatement("DELETE FROM item_attributes WHERE itemId IN (SELECT object_id FROM items WHERE items.owner_id=?)");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();

			statement = con.prepareStatement("DELETE FROM items WHERE owner_id=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();

			statement = con.prepareStatement("DELETE FROM merchant_lease WHERE player_id=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();

			statement = con.prepareStatement("DELETE FROM character_raid_points WHERE charId=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();
			
			statement = con.prepareStatement("DELETE FROM character_recommends WHERE charId=? OR target_id=?");
			statement.setInt(1, objid);
			statement.setInt(2, objid);
			statement.execute();
			statement.close();

			statement = con.prepareStatement("DELETE FROM character_instance_time WHERE charId=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();

			statement = con.prepareStatement("DELETE FROM characters WHERE charId=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Error deleting character.", e);
		}
		finally
		{
			try { con.close(); } catch (Exception e) {}
		}
	}

	public L2PcInstance loadCharFromDisk(int charslot)
	{
		L2PcInstance character = L2PcInstance.load(getObjectIdForSlot(charslot));

        if (character != null)
		{

			// preinit some values for each login
			character.setRunning();	// running is default
			character.standUp();		// standing is default

			character.refreshOverloaded();
			character.refreshExpertisePenalty();
            character.setOnlineStatus(true);
		}
		else
		{
			_log.severe("could not restore in slot: "+ charslot);
		}

		//setCharacter(character);
		return character;
	}

	/**
     * @param chars
     */
    public void setCharSelection(CharSelectInfoPackage[] chars)
    {
        _charSlotMapping.clear();

        for (int i = 0; i < chars.length; i++)
        {
            int objectId = chars[i].getObjectId();
            _charSlotMapping.add(Integer.valueOf(objectId));
        }
    }

    public void close(L2GameServerPacket gsp)
    {
    	getConnection().close(gsp);
    }

    /**
     * @param charslot
     * @return
     */
    private int getObjectIdForSlot(int charslot)
    {
        if (charslot < 0 || charslot >= _charSlotMapping.size())
        {
            _log.warning(toString()+" tried to delete Character in slot "+charslot+" but no characters exits at that slot.");
            return -1;
        }
        Integer objectId = _charSlotMapping.get(charslot);
        return objectId.intValue();
    }

    @Override
    protected void onForcedDisconnection()
    {
    	LogRecord record = new LogRecord(Level.WARNING, "Disconnected abnormally");
    	record.setParameters(new Object[]{L2GameClient.this});
		_logAccounting.log(record);
    }

    @Override
    protected void onDisconnection()
	{
    	// no long running tasks here, do it async
    	try
    	{
    		ThreadPoolManager.getInstance().executeTask(new DisconnectTask());
    	}
    	catch (RejectedExecutionException e)
    	{
    		// server is closing
    	}
    }

    public void closeNow()
    {
    	super.getConnection().close(ServerClose.STATIC_PACKET);
    	cleanMe(true);
    }
    
    /**
     * Produces the best possible string representation of this client.
     */
    @Override
	public String toString()
	{
		try
		{
			InetAddress address = getConnection().getInetAddress();
			switch (getState())
			{
				case CONNECTED:
					return "[IP: "+(address == null ? "disconnected" : address.getHostAddress())+"]";
				case AUTHED:
					return "[Account: "+getAccountName()+" - IP: "+(address == null ? "disconnected" : address.getHostAddress())+"]";
				case IN_GAME:
					return "[Character: "+(getActiveChar() == null ? "disconnected" : getActiveChar().getName())+" - Account: "+getAccountName()+" - IP: "+(address == null ? "disconnected" : address.getHostAddress())+"]";
				default:
					throw new IllegalStateException("Missing state on switch");
			}
		}
		catch (NullPointerException e)
		{
			return "[Character read failed due to disconnect]";
		}
	}

	class DisconnectTask implements Runnable
	{

		/**
		 * @see java.lang.Runnable#run()
		 */
		public void run()
		{
			boolean fast = true;

			try
			{
				final L2PcInstance player = L2GameClient.this.getActiveChar();
				if (player != null && !isDetached())
				{
					isDetached(true);
					if (!player.isInOlympiadMode()
							&& !player.isFestivalParticipant()
							&& !TvTEvent.isPlayerParticipant(player.getObjectId()) && !player.isInJail())
					{
						if ((player.isInStoreMode() && Config.OFFLINE_TRADE_ENABLE)
								|| (player.isInCraftMode() && Config.OFFLINE_CRAFT_ENABLE))
						{
							player.leaveParty();
							if (Config.OFFLINE_SET_NAME_COLOR)
							{
								player.getAppearance().setNameColor(Config.OFFLINE_NAME_COLOR);
								player.broadcastUserInfo();
							}

							LogRecord record = new LogRecord(Level.INFO, "Entering offline mode");
					    	record.setParameters(new Object[]{L2GameClient.this});
							_logAccounting.log(record);
							return;
						}
					}
					if (player.isInCombat() || player.isLocked())
					{
						fast = false;
					}
				}
				cleanMe(fast);
			}
			catch (Exception e1)
			{
				_log.log(Level.WARNING, "Error while disconnecting client.", e1);
			}
		}
	}
	
	public void cleanMe(boolean fast)
	{
		try
		{
			synchronized(this)
			{
	            if (_cleanupTask == null)
				{
	            	_cleanupTask = ThreadPoolManager.getInstance().scheduleGeneral(new CleanupTask(), fast ? 5 : 15000L);
				}			
			}
		}
		catch (Exception e1)
		{
			_log.log(Level.WARNING, "Error during cleanup.", e1);			
		}
	}
		
	class CleanupTask implements Runnable
	{
		/**
		 * @see java.lang.Runnable#run()
		 */
		public void run()
		{
			try
			{
				// we are going to manually save the char bellow thus we can force the cancel
				if (_autoSaveInDB != null)
				{
					_autoSaveInDB.cancel(true);
					ThreadPoolManager.getInstance().removeGeneral((Runnable) _autoSaveInDB);
				}

				L2PcInstance player = L2GameClient.this.getActiveChar();
				if (player != null) // this should only happen on connection loss
				{
					if (player.isLocked())
					{
						_log.log(Level.WARNING, "Player "+player.getName()+" still performing subclass actions during disconnect.");
					}

					// we store all data from players who are disconnected while in an event in order to restore it in the next login
					if (player.atEvent)
					{
						EventData data = new EventData(player.eventX, player.eventY, player.eventZ, player.eventkarma, player.eventpvpkills, player.eventpkkills, player.eventTitle, player.kills,
								player.eventSitForced);
						L2Event.connectionLossData.put(player.getName(), data);
					}
					// to prevent call cleanMe() again
					isDetached(false);

					player.logout();

					player.setClient(null);
				}
				L2GameClient.this.setActiveChar(null);
			}
			catch (Exception e1)
			{
				_log.log(Level.WARNING, "Error while cleanup client.", e1);
			}
			finally
			{
				LoginServerThread.getInstance().sendLogout(L2GameClient.this.getAccountName());
			}
		}
	}

	class AutoSaveTask implements Runnable
	{
		public void run()
		{
			try
			{
				L2PcInstance player = L2GameClient.this.getActiveChar();
				if (player != null && player.isOnline() > 0) // safety precaution
				{
					saveCharToDisk(player, true);
					if (player.getPet() != null)
						player.getPet().store();
				}
			}
			catch (Exception e)
			{
			    _log.log(Level.SEVERE, "Error on AutoSaveTask.", e);
			}
		}
	}

	public boolean isProtocolOk()
	{
		return _protocol;
	}
	public void setProtocolOk(boolean b)
	{
		_protocol = b;
	}
	
	public boolean handleCheat(String punishment)
	{
		if (_activeChar != null)
		{
			Util.handleIllegalPlayerAction(_activeChar, toString()+": "+punishment, Config.DEFAULT_PUNISH);
			return true;
		}
		
		Logger _logAudit = Logger.getLogger("audit");
		_logAudit.log(Level.INFO, "AUDIT: Client "+toString()+" kicked for reason: "+punishment);
		closeNow();
		return false;
	}
}
