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
package com.l2jserver.gameserver.model.entity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;

import com.l2jserver.Config;
import com.l2jserver.L2DatabaseFactory;
import com.l2jserver.gameserver.ThreadPoolManager;
import com.l2jserver.gameserver.datatables.ClanTable;
import com.l2jserver.gameserver.model.L2Clan;
import com.l2jserver.gameserver.model.actor.instance.L2DoorInstance;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.zone.type.L2ClanHallZone;
import com.l2jserver.gameserver.network.serverpackets.PledgeShowInfoUpdate;
import com.l2jserver.gameserver.templates.StatsSet;

public abstract class ClanHall
{
	protected static final Logger _log = Logger.getLogger(ClanHall.class.getName());
	
	private int _clanHallId;
	private List<L2DoorInstance> _doors;
	private String _name;
	private int _ownerId;
	private String _desc;
	private String _location;
	private L2ClanHallZone _zone;
	protected final int _chRate = 604800000;
	protected boolean _isFree = true;
	private Map<Integer, ClanHallFunction> _functions;
	
	/** Clan Hall Functions */
	public static final int FUNC_TELEPORT = 1;
	public static final int FUNC_ITEM_CREATE = 2;
	public static final int FUNC_RESTORE_HP = 3;
	public static final int FUNC_RESTORE_MP = 4;
	public static final int FUNC_RESTORE_EXP = 5;
	public static final int FUNC_SUPPORT = 6;
	public static final int FUNC_DECO_FRONTPLATEFORM = 7; 	//Only Auctionable Halls
	public static final int FUNC_DECO_CURTAINS = 8;			//Only Auctionable Halls
	
	public class ClanHallFunction
	{
		private int _type;
		private int _lvl;
		protected int _fee;
		protected int _tempFee;
		private long _rate;
		private long _endDate;
		protected boolean _inDebt;
		public boolean _cwh; // first activating clanhall function is payed from player inventory, any others from clan warehouse
		
		public ClanHallFunction(int type, int lvl, int lease, int tempLease, long rate, long time, boolean cwh)
		{
			_type = type;
			_lvl = lvl;
			_fee = lease;
			_tempFee = tempLease;
			_rate = rate;
			_endDate = time;
			initializeTask(cwh);
		}
		
		public int getType()
		{
			return _type;
		}
		
		public int getLvl()
		{
			return _lvl;
		}
		
		public int getLease()
		{
			return _fee;
		}
		
		public long getRate()
		{
			return _rate;
		}
		
		public long getEndTime()
		{
			return _endDate;
		}
		
		public void setLvl(int lvl)
		{
			_lvl = lvl;
		}
		
		public void setLease(int lease)
		{
			_fee = lease;
		}
		
		public void setEndTime(long time)
		{
			_endDate = time;
		}
		
		private void initializeTask(boolean cwh)
		{
			if (_isFree)
				return;
			long currentTime = System.currentTimeMillis();
			if (_endDate > currentTime)
				ThreadPoolManager.getInstance().scheduleGeneral(new FunctionTask(cwh), _endDate - currentTime);
			else
				ThreadPoolManager.getInstance().scheduleGeneral(new FunctionTask(cwh), 0);
		}
		
		private class FunctionTask implements Runnable
		{
			public FunctionTask(boolean cwh)
			{
				_cwh = cwh;
			}
			
			public void run()
			{
				try
				{
					if (_isFree)
						return;
					if (ClanTable.getInstance().getClan(getOwnerId()).getWarehouse().getAdena() >= _fee || !_cwh)
					{
						int fee = _fee;
						if (getEndTime() == -1)
							fee = _tempFee;
						
						setEndTime(System.currentTimeMillis() + getRate());
						dbSave();
						if (_cwh)
						{
							ClanTable.getInstance().getClan(getOwnerId()).getWarehouse().destroyItemByItemId("CH_function_fee", 57, fee, null, null);
							if (Config.DEBUG)
								_log.warning("deducted " + fee + " adena from " + getName() + " owner's cwh for function id : " + getType());
						}
						ThreadPoolManager.getInstance().scheduleGeneral(new FunctionTask(true), getRate());
					}
					else
						removeFunction(getType());
				}
				catch (Exception e)
				{
					_log.log(Level.SEVERE, "", e);
				}
			}
		}
		
		public void dbSave()
		{
			Connection con = null;
			try
			{
				PreparedStatement statement;
				
				con = L2DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement("REPLACE INTO clanhall_functions (hall_id, type, lvl, lease, rate, endTime) VALUES (?,?,?,?,?,?)");
				statement.setInt(1, getId());
				statement.setInt(2, getType());
				statement.setInt(3, getLvl());
				statement.setInt(4, getLease());
				statement.setLong(5, getRate());
				statement.setLong(6, getEndTime());
				statement.execute();
				statement.close();
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "Exception: ClanHall.updateFunctions(int type, int lvl, int lease, long rate, long time, boolean addNew): "
						+ e.getMessage(), e);
			}
			finally
			{
				L2DatabaseFactory.close(con);
			}
		}
	}
	
	public ClanHall(StatsSet set)
	{
		_clanHallId = set.getInteger("id");
		_name = set.getString("name");
		_ownerId = set.getInteger("ownerId");
		if (Config.DEBUG)
			_log.warning("Init Owner : " + _ownerId);
		_desc = set.getString("desc");
		_location = set.getString("location");
		_functions = new FastMap<Integer, ClanHallFunction>();
		
		if(_ownerId > 0)
		{
			L2Clan clan = ClanTable.getInstance().getClan(_ownerId);
			if(clan != null)
				clan.setHasHideout(getId());
			else
				free();
		}
	}
	
	/** Return Id Of Clan hall */
	public final int getId()
	{
		return _clanHallId;
	}
	
	/** Return name */
	public final String getName()
	{
		return _name;
	}
	
	/** Return OwnerId */
	public final int getOwnerId()
	{
		return _ownerId;
	}
	
	/** Return Desc */
	public final String getDesc()
	{
		return _desc;
	}
	
	/** Return Location */
	public final String getLocation()
	{
		return _location;
	}
	
	/** Return all DoorInstance */
	public final List<L2DoorInstance> getDoors()
	{
		if (_doors == null)
			_doors = new FastList<L2DoorInstance>();
		return _doors;
	}
	
	/** Return Door */
	public final L2DoorInstance getDoor(int doorId)
	{
		if (doorId <= 0)
			return null;
		for (L2DoorInstance door : getDoors())
		{
			if (door.getDoorId() == doorId)
				return door;
		}
		return null;
	}
	
	/** Return function with id */
	public ClanHallFunction getFunction(int type)
	{
		if (_functions.get(type) != null)
			return _functions.get(type);
		return null;
	}
	
	/**
	 * Sets this clan halls zone
	 * @param zone
	 */
	public void setZone(L2ClanHallZone zone)
	{
		_zone = zone;
	}
	
	/**
	 * Return true if object is inside the zone
	 */
	public boolean checkIfInZone(int x, int y, int z)
	{
		return getZone().isInsideZone(x, y, z);
	}
	
	/** Returns the zone of this clan hall */
	public L2ClanHallZone getZone()
	{
		return _zone;
	}
	
	/** Free this clan hall */
	public void free()
	{
		_ownerId = 0;
		_isFree = true;
		for (Map.Entry<Integer, ClanHallFunction> fc : _functions.entrySet())
			removeFunction(fc.getKey());
		_functions.clear();
		updateDb();
	}
	
	/** Set owner if clan hall is free */
	public void setOwner(L2Clan clan)
	{
		// Verify that this ClanHall is Free and Clan isn't null
		if (_ownerId > 0 || clan == null)
			return;
		_ownerId = clan.getClanId();
		_isFree = false;
		clan.setHasHideout(getId());
		// Annonce to Online member new ClanHall
		clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));
		updateDb();
	}
		
	/** Open or Close Door */
	public void openCloseDoor(L2PcInstance activeChar, int doorId, boolean open)
	{
		if (activeChar != null && activeChar.getClanId() == getOwnerId())
			openCloseDoor(doorId, open);
	}
	
	public void openCloseDoor(int doorId, boolean open)
	{
		openCloseDoor(getDoor(doorId), open);
	}
	
	public void openCloseDoor(L2DoorInstance door, boolean open)
	{
		if (door != null)
		{
			if (open)
				door.openMe();
			else
				door.closeMe();
		}
	}
	
	public void openCloseDoors(L2PcInstance activeChar, boolean open)
	{
		if (activeChar != null && activeChar.getClanId() == getOwnerId())
			openCloseDoors(open);
	}
	
	public void openCloseDoors(boolean open)
	{
		for (L2DoorInstance door : getDoors())
		{
			if (door != null)
			{
				if (open)
					door.openMe();
				else
					door.closeMe();
			}
		}
	}
	
	/** Banish Foreigner */
	public void banishForeigners()
	{
		_zone.banishForeigners(getOwnerId());
	}
	
	/** Load All Functions */
	protected void loadFunctions()
	{
		Connection con = null;
		try
		{
			PreparedStatement statement;
			ResultSet rs;
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("Select * from clanhall_functions where hall_id = ?");
			statement.setInt(1, getId());
			rs = statement.executeQuery();
			while (rs.next())
			{
				_functions.put(rs.getInt("type"), new ClanHallFunction(rs.getInt("type"), rs.getInt("lvl"), rs.getInt("lease"), 0, rs.getLong("rate"), rs.getLong("endTime"), true));
			}
			rs.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Exception: ClanHall.loadFunctions(): " + e.getMessage(), e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
	}
	
	/** Remove function In List and in DB */
	public void removeFunction(int functionType)
	{
		_functions.remove(functionType);
		Connection con = null;
		try
		{
			PreparedStatement statement;
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM clanhall_functions WHERE hall_id=? AND type=?");
			statement.setInt(1, getId());
			statement.setInt(2, functionType);
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Exception: ClanHall.removeFunctions(int functionType): " + e.getMessage(), e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
	}
	
	public boolean updateFunctions(L2PcInstance player, int type, int lvl, int lease, long rate, boolean addNew)
	{
		if (player == null)
			return false;
		if (Config.DEBUG)
			_log.warning("Called ClanHall.updateFunctions(int type, int lvl, int lease, long rate, boolean addNew) Owner : " + getOwnerId());
		if (lease > 0)
		{
			if (!player.destroyItemByItemId("Consume", 57, lease, null, true))
				return false;
		}
		if (addNew)
			_functions.put(type, new ClanHallFunction(type, lvl, lease, 0, rate, 0, false));
		else
		{
			if (lvl == 0 && lease == 0)
				removeFunction(type);
			else
			{
				int diffLease = lease - _functions.get(type).getLease();
				if (Config.DEBUG)
					_log.warning("Called ClanHall.updateFunctions diffLease : " + diffLease);
				if (diffLease > 0)
				{
					_functions.remove(type);
					_functions.put(type, new ClanHallFunction(type, lvl, lease, 0, rate, -1, false));
				}
				else
				{
					_functions.get(type).setLease(lease);
					_functions.get(type).setLvl(lvl);
					_functions.get(type).dbSave();
				}
			}
		}
		return true;
	}
	
	public int getGrade()
	{
		return 0;
	}
	
	public long getPaidUntil()
	{
		return 0;
	}
	
	public int getLease()
	{
		return 0;
	}
	
	public boolean isSiegableHall()
	{
		return false;
	}
	
	public abstract void updateDb();
}
