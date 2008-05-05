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
package net.sf.l2j.gameserver.network;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastList;
import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.LoginServerThread;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.LoginServerThread.SessionKey;
import net.sf.l2j.gameserver.communitybbs.Manager.RegionBBSManager;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.CharSelectInfoPackage;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.L2Event;
import net.sf.l2j.gameserver.serverpackets.L2GameServerPacket;
import net.sf.l2j.util.EventData;

import org.mmocore.network.MMOClient;
import org.mmocore.network.MMOConnection;

/**
 * Represents a client connected on Game Server
 * @author  KenM
 */
public final class L2GameClient extends MMOClient<MMOConnection<L2GameClient>>
{
	protected static final Logger _log = Logger.getLogger(L2GameClient.class.getName());

	/**
	 * CONNECTED	- client has just connected
	 * AUTHED		- client has authed but doesnt has character attached to it yet
	 * IN_GAME		- client has selected a char and is in game
	 * @author  KenM
	 */
	public static enum GameClientState { CONNECTED, AUTHED, IN_GAME };

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

	// Crypt
	private GameCrypt _crypt;

	// Flood protection
	public byte packetsSentInSec = 0;
	public int packetsSentStartTick = 0;

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
		getConnection().sendPacket(gsp);
		gsp.runImpl();
	}

	public L2PcInstance markToDeleteChar(int charslot) throws Exception
	{
		//have to make sure active character must be nulled
		/*if (getActiveChar() != null)
		{
			saveCharToDisk(getActiveChar());
			if (Config.DEBUG)
			{
				_log.fine("active Char saved");
			}
			this.setActiveChar(null);
		}*/

		int objid = getObjectIdForSlot(charslot);
		if (objid < 0)
		    return null;

		L2PcInstance character = L2PcInstance.load(objid);
		if (character.getClanId() != 0)
			return character;
        character.deleteMe();
		java.sql.Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE characters SET deletetime=? WHERE charId=?");
			statement.setLong(1, System.currentTimeMillis() + Config.DELETE_DAYS*86400000L); // 24*60*60*1000 = 86400000
			statement.setInt(2, objid);
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("Data error on update delete time of char: " + e);
		}
		finally
		{
			try { con.close(); } catch (Exception e) {}
		}
	    return null;
	}

	public L2PcInstance deleteChar(int charslot) throws Exception
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
    	    return null;

		L2PcInstance character = L2PcInstance.load(objid);
		if (character.getClanId() != 0)
			return character;

        character.deleteMe();
		deleteCharByObjId(objid);

		return null;
	}

	/**
	 * Save the L2PcInstance to the database.
	 */
	public static void saveCharToDisk(L2PcInstance cha)
	{
        try
        {
            cha.store();
            if (Config.UPDATE_ITEMS_ON_CHAR_STORE)
            {
                cha.getInventory().updateDatabase();
            }
        }
        catch(Exception e)
        {
            _log.severe("Error saving player character: "+e);
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
		java.sql.Connection con = null;
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
			_log.severe("Data error on restoring char: " + e);
		}
		finally
		{
			try { con.close(); } catch (Exception e) {}
		}
	}




	public static void deleteCharByObjId(int objid)
	{
	    if (objid < 0)
	        return;

	    java.sql.Connection con = null;

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

			statement = con.prepareStatement("DELETE FROM augmentations WHERE item_id IN (SELECT object_id FROM items WHERE items.owner_id=?)");
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


			statement = con.prepareStatement("DELETE FROM characters WHERE charId=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("Data error on deleting char: " + e);
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
            _charSlotMapping.add(new Integer(objectId));
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
    	_log.info("Client "+toString()+" disconnected abnormally.");
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

    /**
     * Produces the best possible string representation of this client.
     */
    @Override
	public String toString()
	{
		try
		{
			InetAddress address = getConnection().getSocket().getInetAddress();
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
			try
			{
				// Update BBS
				try
				{
					RegionBBSManager.getInstance().changeCommunityBoard();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}

				// we are going to mannually save the char bellow thus we can force the cancel
				if (_autoSaveInDB != null)
                {
				    _autoSaveInDB.cancel(true);
                }

	            L2PcInstance player = L2GameClient.this.getActiveChar();
				if (player != null)  // this should only happen on connection loss
				{

	                // we store all data from players who are disconnected while in an event in order to restore it in the next login
	                if (player.atEvent)
	                {
	                	EventData data = new EventData(player.eventX, player.eventY, player.eventZ, player.eventkarma, player.eventpvpkills, player.eventpkkills, player.eventTitle, player.kills, player.eventSitForced);
	                    L2Event.connectionLossData.put(player.getName(), data);
	                }
	                if (player.isFlying())
	                {
	                	player.removeSkill(SkillTable.getInstance().getInfo(4289, 1));
	                }
					// notify the world about our disconnect
					player.deleteMe();

					try
	                {
						saveCharToDisk(player);
					}
	                catch (Exception e2) { /* ignore any problems here */ }
				}
				L2GameClient.this.setActiveChar(null);
			}
			catch (Exception e1)
			{
				_log.log(Level.WARNING, "error while disconnecting client", e1);
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
				if (player != null)
				{
					saveCharToDisk(player);
				}
			}
			catch (Throwable e)
			{
				_log.severe(e.toString());
			}
		}
	}
}
