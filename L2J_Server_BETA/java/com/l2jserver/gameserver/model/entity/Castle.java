/*
 * Copyright (C) 2004-2013 L2J Server
 * 
 * This file is part of L2J Server.
 * 
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.l2jserver.gameserver.model.entity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastMap;

import com.l2jserver.Config;
import com.l2jserver.L2DatabaseFactory;
import com.l2jserver.gameserver.CastleUpdater;
import com.l2jserver.gameserver.SevenSigns;
import com.l2jserver.gameserver.ThreadPoolManager;
import com.l2jserver.gameserver.datatables.ClanTable;
import com.l2jserver.gameserver.datatables.DoorTable;
import com.l2jserver.gameserver.datatables.ManorData;
import com.l2jserver.gameserver.datatables.SkillTable;
import com.l2jserver.gameserver.datatables.SkillTreesData;
import com.l2jserver.gameserver.instancemanager.CastleManager;
import com.l2jserver.gameserver.instancemanager.CastleManorManager;
import com.l2jserver.gameserver.instancemanager.CastleManorManager.CropProcure;
import com.l2jserver.gameserver.instancemanager.CastleManorManager.SeedProduction;
import com.l2jserver.gameserver.instancemanager.FortManager;
import com.l2jserver.gameserver.instancemanager.TerritoryWarManager;
import com.l2jserver.gameserver.instancemanager.TerritoryWarManager.Territory;
import com.l2jserver.gameserver.instancemanager.ZoneManager;
import com.l2jserver.gameserver.model.L2Clan;
import com.l2jserver.gameserver.model.L2Object;
import com.l2jserver.gameserver.model.L2SkillLearn;
import com.l2jserver.gameserver.model.actor.instance.L2ArtefactInstance;
import com.l2jserver.gameserver.model.actor.instance.L2DoorInstance;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.itemcontainer.PcInventory;
import com.l2jserver.gameserver.model.skills.L2Skill;
import com.l2jserver.gameserver.model.zone.type.L2CastleZone;
import com.l2jserver.gameserver.model.zone.type.L2ResidenceTeleportZone;
import com.l2jserver.gameserver.model.zone.type.L2SiegeZone;
import com.l2jserver.gameserver.network.serverpackets.PlaySound;
import com.l2jserver.gameserver.network.serverpackets.PledgeShowInfoUpdate;

import gnu.trove.map.hash.TIntIntHashMap;

public class Castle
{
	protected static final Logger _log = Logger.getLogger(Castle.class.getName());
	
	private static final String CASTLE_MANOR_DELETE_PRODUCTION = "DELETE FROM castle_manor_production WHERE castle_id=?;";
	private static final String CASTLE_MANOR_DELETE_PRODUCTION_PERIOD = "DELETE FROM castle_manor_production WHERE castle_id=? AND period=?;";
	private static final String CASTLE_MANOR_DELETE_PROCURE = "DELETE FROM castle_manor_procure WHERE castle_id=?;";
	private static final String CASTLE_MANOR_DELETE_PROCURE_PERIOD = "DELETE FROM castle_manor_procure WHERE castle_id=? AND period=?;";
	private static final String CASTLE_UPDATE_CROP = "UPDATE castle_manor_procure SET can_buy=? WHERE crop_id=? AND castle_id=? AND period=?";
	private static final String CASTLE_UPDATE_SEED = "UPDATE castle_manor_production SET can_produce=? WHERE seed_id=? AND castle_id=? AND period=?";
	
	private int _castleId = 0;
	private final List<L2DoorInstance> _doors = new ArrayList<>();
	private String _name = "";
	private int _ownerId = 0;
	private Siege _siege = null;
	private Calendar _siegeDate;
	private boolean _isTimeRegistrationOver = true; // true if Castle Lords set the time, or 24h is elapsed after the siege
	private Calendar _siegeTimeRegistrationEndDate; // last siege end date + 1 day
	private int _taxPercent = 0;
	private double _taxRate = 0;
	private long _treasury = 0;
	private boolean _showNpcCrest = false;
	private L2SiegeZone _zone = null;
	private L2CastleZone _castleZone = null;
	private L2ResidenceTeleportZone _teleZone;
	private L2Clan _formerOwner = null;
	private final List<L2ArtefactInstance> _artefacts = new ArrayList<>(1);
	private final TIntIntHashMap _engrave = new TIntIntHashMap(1);
	private final Map<Integer, CastleFunction> _function;
	private final List<L2Skill> _residentialSkills = new ArrayList<>();
	private int _ticketBuyCount = 0;
	
	private List<CropProcure> _procure = new ArrayList<>();
	private List<SeedProduction> _production = new ArrayList<>();
	private List<CropProcure> _procureNext = new ArrayList<>();
	private List<SeedProduction> _productionNext = new ArrayList<>();
	private boolean _isNextPeriodApproved = false;
	
	/** Castle Functions */
	public static final int FUNC_TELEPORT = 1;
	public static final int FUNC_RESTORE_HP = 2;
	public static final int FUNC_RESTORE_MP = 3;
	public static final int FUNC_RESTORE_EXP = 4;
	public static final int FUNC_SUPPORT = 5;
	
	public class CastleFunction
	{
		private final int _type;
		private int _lvl;
		protected int _fee;
		protected int _tempFee;
		private final long _rate;
		private long _endDate;
		protected boolean _inDebt;
		public boolean _cwh;
		
		public CastleFunction(int type, int lvl, int lease, int tempLease, long rate, long time, boolean cwh)
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
			if (getOwnerId() <= 0)
			{
				return;
			}
			long currentTime = System.currentTimeMillis();
			if (_endDate > currentTime)
			{
				ThreadPoolManager.getInstance().scheduleGeneral(new FunctionTask(cwh), _endDate - currentTime);
			}
			else
			{
				ThreadPoolManager.getInstance().scheduleGeneral(new FunctionTask(cwh), 0);
			}
		}
		
		private class FunctionTask implements Runnable
		{
			public FunctionTask(boolean cwh)
			{
				_cwh = cwh;
			}
			
			@Override
			public void run()
			{
				try
				{
					if (getOwnerId() <= 0)
					{
						return;
					}
					if ((ClanTable.getInstance().getClan(getOwnerId()).getWarehouse().getAdena() >= _fee) || !_cwh)
					{
						int fee = _fee;
						if (getEndTime() == -1)
						{
							fee = _tempFee;
						}
						
						setEndTime(System.currentTimeMillis() + getRate());
						dbSave();
						if (_cwh)
						{
							ClanTable.getInstance().getClan(getOwnerId()).getWarehouse().destroyItemByItemId("CS_function_fee", PcInventory.ADENA_ID, fee, null, null);
						}
						ThreadPoolManager.getInstance().scheduleGeneral(new FunctionTask(true), getRate());
					}
					else
					{
						removeFunction(getType());
					}
				}
				catch (Exception e)
				{
					_log.log(Level.SEVERE, "", e);
				}
			}
		}
		
		public void dbSave()
		{
			try (Connection con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement ps = con.prepareStatement("REPLACE INTO castle_functions (castle_id, type, lvl, lease, rate, endTime) VALUES (?,?,?,?,?,?)"))
			{
				ps.setInt(1, getCastleId());
				ps.setInt(2, getType());
				ps.setInt(3, getLvl());
				ps.setInt(4, getLease());
				ps.setLong(5, getRate());
				ps.setLong(6, getEndTime());
				ps.execute();
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "Exception: Castle.updateFunctions(int type, int lvl, int lease, long rate, long time, boolean addNew): " + e.getMessage(), e);
			}
		}
	}
	
	public Castle(int castleId)
	{
		_castleId = castleId;
		/*
		 * if (_castleId == 7 || castleId == 9) // Goddard and Schuttgart _nbArtifact = 2;
		 */
		load();
		_function = new FastMap<>();
		final List<L2SkillLearn> residentialSkills = SkillTreesData.getInstance().getAvailableResidentialSkills(castleId);
		for (L2SkillLearn s : residentialSkills)
		{
			final L2Skill sk = SkillTable.getInstance().getInfo(s.getSkillId(), s.getSkillLevel());
			if (sk != null)
			{
				_residentialSkills.add(sk);
			}
			else
			{
				_log.warning("Castle Id: " + castleId + " has a null residential skill Id: " + s.getSkillId() + " level: " + s.getSkillLevel() + "!");
			}
		}
		if (getOwnerId() != 0)
		{
			loadFunctions();
		}
	}
	
	/**
	 * Return function with id
	 * @param type
	 * @return
	 */
	public CastleFunction getFunction(int type)
	{
		if (_function.containsKey(type))
		{
			return _function.get(type);
		}
		return null;
	}
	
	public synchronized void engrave(L2Clan clan, L2Object target)
	{
		if (!_artefacts.contains(target))
		{
			return;
		}
		_engrave.put(target.getObjectId(), clan.getClanId());
		if (_engrave.size() == _artefacts.size())
		{
			for (L2ArtefactInstance art : _artefacts)
			{
				if (_engrave.get(art.getObjectId()) != clan.getClanId())
				{
					return;
				}
			}
			_engrave.clear();
			setOwner(clan);
		}
	}
	
	// This method add to the treasury
	/**
	 * Add amount to castle instance's treasury (warehouse).
	 * @param amount
	 */
	public void addToTreasury(long amount)
	{
		// check if owned
		if (getOwnerId() <= 0)
		{
			return;
		}
		
		if (_name.equalsIgnoreCase("Schuttgart") || _name.equalsIgnoreCase("Goddard"))
		{
			Castle rune = CastleManager.getInstance().getCastle("rune");
			if (rune != null)
			{
				long runeTax = (long) (amount * rune.getTaxRate());
				if (rune.getOwnerId() > 0)
				{
					rune.addToTreasury(runeTax);
				}
				amount -= runeTax;
			}
		}
		if (!_name.equalsIgnoreCase("aden") && !_name.equalsIgnoreCase("Rune") && !_name.equalsIgnoreCase("Schuttgart") && !_name.equalsIgnoreCase("Goddard")) // If current castle instance is not Aden, Rune, Goddard or Schuttgart.
		{
			Castle aden = CastleManager.getInstance().getCastle("aden");
			if (aden != null)
			{
				long adenTax = (long) (amount * aden.getTaxRate()); // Find out what Aden gets from the current castle instance's income
				if (aden.getOwnerId() > 0)
				{
					aden.addToTreasury(adenTax); // Only bother to really add the tax to the treasury if not npc owned
				}
				
				amount -= adenTax; // Subtract Aden's income from current castle instance's income
			}
		}
		
		addToTreasuryNoTax(amount);
	}
	
	/**
	 * Add amount to castle instance's treasury (warehouse), no tax paying.
	 * @param amount
	 * @return
	 */
	public boolean addToTreasuryNoTax(long amount)
	{
		if (getOwnerId() <= 0)
		{
			return false;
		}
		
		if (amount < 0)
		{
			amount *= -1;
			if (_treasury < amount)
			{
				return false;
			}
			_treasury -= amount;
		}
		else
		{
			if ((_treasury + amount) > PcInventory.MAX_ADENA)
			{
				_treasury = PcInventory.MAX_ADENA;
			}
			else
			{
				_treasury += amount;
			}
		}
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE castle SET treasury = ? WHERE id = ?"))
		{
			ps.setLong(1, getTreasury());
			ps.setInt(2, getCastleId());
			ps.execute();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, e.getMessage(), e);
		}
		return true;
	}
	
	/**
	 * Move non clan members off castle area and to nearest town.
	 */
	public void banishForeigners()
	{
		getCastleZone().banishForeigners(getOwnerId());
	}
	
	/**
	 * Return true if object is inside the zone
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	public boolean checkIfInZone(int x, int y, int z)
	{
		return getZone().isInsideZone(x, y, z);
	}
	
	public L2SiegeZone getZone()
	{
		if (_zone == null)
		{
			for (L2SiegeZone zone : ZoneManager.getInstance().getAllZones(L2SiegeZone.class))
			{
				if (zone.getSiegeObjectId() == getCastleId())
				{
					_zone = zone;
					break;
				}
			}
		}
		return _zone;
	}
	
	public L2CastleZone getCastleZone()
	{
		if (_castleZone == null)
		{
			for (L2CastleZone zone : ZoneManager.getInstance().getAllZones(L2CastleZone.class))
			{
				if (zone.getCastleId() == getCastleId())
				{
					_castleZone = zone;
					break;
				}
			}
		}
		return _castleZone;
	}
	
	public L2ResidenceTeleportZone getTeleZone()
	{
		if (_teleZone == null)
		{
			for (L2ResidenceTeleportZone zone : ZoneManager.getInstance().getAllZones(L2ResidenceTeleportZone.class))
			{
				if (zone.getResidenceId() == getCastleId())
				{
					_teleZone = zone;
					break;
				}
			}
		}
		return _teleZone;
	}
	
	public void oustAllPlayers()
	{
		getTeleZone().oustAllPlayers();
	}
	
	/**
	 * Get the objects distance to this castle
	 * @param obj
	 * @return
	 */
	public double getDistance(L2Object obj)
	{
		return getZone().getDistanceToZone(obj);
	}
	
	public void closeDoor(L2PcInstance activeChar, int doorId)
	{
		openCloseDoor(activeChar, doorId, false);
	}
	
	public void openDoor(L2PcInstance activeChar, int doorId)
	{
		openCloseDoor(activeChar, doorId, true);
	}
	
	public void openCloseDoor(L2PcInstance activeChar, int doorId, boolean open)
	{
		if (activeChar.getClanId() != getOwnerId())
		{
			return;
		}
		
		L2DoorInstance door = getDoor(doorId);
		if (door != null)
		{
			if (open)
			{
				door.openMe();
			}
			else
			{
				door.closeMe();
			}
		}
	}
	
	// This method is used to begin removing all castle upgrades
	public void removeUpgrade()
	{
		removeDoorUpgrade();
		for (Integer fc : _function.keySet())
		{
			removeFunction(fc);
		}
		_function.clear();
	}
	
	// This method updates the castle tax rate
	public void setOwner(L2Clan clan)
	{
		// Remove old owner
		if ((getOwnerId() > 0) && ((clan == null) || (clan.getClanId() != getOwnerId())))
		{
			L2Clan oldOwner = ClanTable.getInstance().getClan(getOwnerId()); // Try to find clan instance
			if (oldOwner != null)
			{
				if (_formerOwner == null)
				{
					_formerOwner = oldOwner;
					if (Config.REMOVE_CASTLE_CIRCLETS)
					{
						CastleManager.getInstance().removeCirclet(_formerOwner, getCastleId());
					}
				}
				try
				{
					L2PcInstance oldleader = oldOwner.getLeader().getPlayerInstance();
					if (oldleader != null)
					{
						if (oldleader.getMountType() == 2)
						{
							oldleader.dismount();
						}
					}
				}
				catch (Exception e)
				{
					_log.log(Level.WARNING, "Exception in setOwner: " + e.getMessage(), e);
				}
				oldOwner.setCastleId(0); // Unset has castle flag for old owner
				for (L2PcInstance member : oldOwner.getOnlineMembers(0))
				{
					removeResidentialSkills(member);
					member.sendSkillList();
				}
			}
		}
		
		updateOwnerInDB(clan); // Update in database
		setShowNpcCrest(false);
		
		// if clan have fortress, remove it
		if ((clan != null) && (clan.getFortId() > 0))
		{
			FortManager.getInstance().getFortByOwner(clan).removeOwner(true);
		}
		
		if (getSiege().getIsInProgress())
		{
			getSiege().midVictory(); // Mid victory phase of siege
		}
		
		TerritoryWarManager.getInstance().getTerritory(_castleId).setOwnerClan(clan);
		
		if (clan != null)
		{
			for (L2PcInstance member : clan.getOnlineMembers(0))
			{
				giveResidentialSkills(member);
				member.sendSkillList();
			}
		}
	}
	
	public void removeOwner(L2Clan clan)
	{
		if (clan != null)
		{
			_formerOwner = clan;
			if (Config.REMOVE_CASTLE_CIRCLETS)
			{
				CastleManager.getInstance().removeCirclet(_formerOwner, getCastleId());
			}
			for (L2PcInstance member : clan.getOnlineMembers(0))
			{
				removeResidentialSkills(member);
				member.sendSkillList();
			}
			clan.setCastleId(0);
			clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));
		}
		
		updateOwnerInDB(null);
		if (getSiege().getIsInProgress())
		{
			getSiege().midVictory();
		}
		
		for (Integer fc : _function.keySet())
		{
			removeFunction(fc);
		}
		_function.clear();
	}
	
	// This method updates the castle tax rate
	public void setTaxPercent(L2PcInstance activeChar, int taxPercent)
	{
		int maxTax;
		switch (SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE))
		{
			case SevenSigns.CABAL_DAWN:
				maxTax = 25;
				break;
			case SevenSigns.CABAL_DUSK:
				maxTax = 5;
				break;
			default: // no owner
				maxTax = 15;
		}
		
		if ((taxPercent < 0) || (taxPercent > maxTax))
		{
			activeChar.sendMessage("Tax value must be between 0 and " + maxTax + ".");
			return;
		}
		
		setTaxPercent(taxPercent);
		activeChar.sendMessage(getName() + " castle tax changed to " + taxPercent + "%.");
	}
	
	public void setTaxPercent(int taxPercent)
	{
		_taxPercent = taxPercent;
		_taxRate = _taxPercent / 100.0;
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE castle SET taxPercent = ? WHERE id = ?"))
		{
			ps.setInt(1, taxPercent);
			ps.setInt(2, getCastleId());
			ps.execute();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, e.getMessage(), e);
		}
	}
	
	/**
	 * Respawn all doors on castle grounds.
	 */
	public void spawnDoor()
	{
		spawnDoor(false);
	}
	
	/**
	 * Respawn all doors on castle grounds<BR>
	 * <BR>
	 * @param isDoorWeak
	 */
	public void spawnDoor(boolean isDoorWeak)
	{
		for (L2DoorInstance door : _doors)
		{
			if (door.isDead())
			{
				door.doRevive();
				if (isDoorWeak)
				{
					door.setCurrentHp(door.getMaxHp() / 2);
				}
				else
				{
					door.setCurrentHp(door.getMaxHp());
				}
			}
			
			if (door.getOpen())
			{
				door.closeMe();
			}
		}
		loadDoorUpgrade(); // Check for any upgrade the doors may have
	}
	
	// This method upgrade door
	public void upgradeDoor(int doorId, int hp, int pDef, int mDef)
	{
		L2DoorInstance door = getDoor(doorId);
		if (door == null)
		{
			return;
		}
		
		door.setCurrentHp(door.getMaxHp() + hp);
		
		saveDoorUpgrade(doorId, hp, pDef, mDef);
	}
	
	// This method loads castle
	private void load()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps1 = con.prepareStatement("SELECT * FROM castle WHERE id = ?");
			PreparedStatement ps2 = con.prepareStatement("SELECT clan_id FROM clan_data WHERE hasCastle = ?"))
		{
			ps1.setInt(1, getCastleId());
			try (ResultSet rs = ps1.executeQuery())
			{
				while (rs.next())
				{
					_name = rs.getString("name");
					// _OwnerId = rs.getInt("ownerId");
					
					_siegeDate = Calendar.getInstance();
					_siegeDate.setTimeInMillis(rs.getLong("siegeDate"));
					_siegeTimeRegistrationEndDate = Calendar.getInstance();
					_siegeTimeRegistrationEndDate.setTimeInMillis(rs.getLong("regTimeEnd"));
					_isTimeRegistrationOver = rs.getBoolean("regTimeOver");
					
					_taxPercent = rs.getInt("taxPercent");
					_treasury = rs.getLong("treasury");
					
					_showNpcCrest = rs.getBoolean("showNpcCrest");
					
					_ticketBuyCount = rs.getInt("ticketBuyCount");
				}
			}
			_taxRate = _taxPercent / 100.0;
			
			ps2.setInt(1, getCastleId());
			try (ResultSet rs = ps2.executeQuery())
			{
				while (rs.next())
				{
					_ownerId = rs.getInt("clan_id");
				}
			}
			
			if (getOwnerId() > 0)
			{
				L2Clan clan = ClanTable.getInstance().getClan(getOwnerId()); // Try to find clan instance
				ThreadPoolManager.getInstance().scheduleGeneral(new CastleUpdater(clan, 1), 3600000); // Schedule owner tasks to start running
			}
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Exception: loadCastleData(): " + e.getMessage(), e);
		}
	}
	
	/** Load All Functions */
	private void loadFunctions()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT * FROM castle_functions WHERE castle_id = ?"))
		{
			ps.setInt(1, getCastleId());
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					_function.put(rs.getInt("type"), new CastleFunction(rs.getInt("type"), rs.getInt("lvl"), rs.getInt("lease"), 0, rs.getLong("rate"), rs.getLong("endTime"), true));
				}
			}
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Exception: Castle.loadFunctions(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * Remove function In List and in DB
	 * @param functionType
	 */
	public void removeFunction(int functionType)
	{
		_function.remove(functionType);
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement("DELETE FROM castle_functions WHERE castle_id=? AND type=?"))
		{
			ps.setInt(1, getCastleId());
			ps.setInt(2, functionType);
			ps.execute();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Exception: Castle.removeFunctions(int functionType): " + e.getMessage(), e);
		}
	}
	
	public boolean updateFunctions(L2PcInstance player, int type, int lvl, int lease, long rate, boolean addNew)
	{
		if (player == null)
		{
			return false;
		}
		if (lease > 0)
		{
			if (!player.destroyItemByItemId("Consume", PcInventory.ADENA_ID, lease, null, true))
			{
				return false;
			}
		}
		if (addNew)
		{
			_function.put(type, new CastleFunction(type, lvl, lease, 0, rate, 0, false));
		}
		else
		{
			if ((lvl == 0) && (lease == 0))
			{
				removeFunction(type);
			}
			else
			{
				int diffLease = lease - _function.get(type).getLease();
				if (diffLease > 0)
				{
					_function.remove(type);
					_function.put(type, new CastleFunction(type, lvl, lease, 0, rate, -1, false));
				}
				else
				{
					_function.get(type).setLease(lease);
					_function.get(type).setLvl(lvl);
					_function.get(type).dbSave();
				}
			}
		}
		return true;
	}
	
	public void activateInstance()
	{
		loadDoor();
	}
	
	// This method loads castle door data from database
	private void loadDoor()
	{
		for (L2DoorInstance door : DoorTable.getInstance().getDoors())
		{
			if ((door.getCastle() != null) && (door.getCastle().getCastleId() == getCastleId()))
			{
				_doors.add(door);
			}
		}
	}
	
	// This method loads castle door upgrade data from database
	private void loadDoorUpgrade()
	{
		StringBuilder doorIds = new StringBuilder(100);
		for (L2DoorInstance door : getDoors())
		{
			doorIds.append(door.getDoorId()).append(',');
		}
		doorIds.deleteCharAt(doorIds.length() - 1);
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			Statement s = con.createStatement();
			ResultSet rs = s.executeQuery("SELECT * FROM castle_doorupgrade WHERE doorId IN (" + doorIds.toString() + ")"))
		{
			while (rs.next())
			{
				upgradeDoor(rs.getInt("id"), rs.getInt("hp"), rs.getInt("pDef"), rs.getInt("mDef"));
			}
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Exception: loadCastleDoorUpgrade(): " + e.getMessage(), e);
		}
	}
	
	private void removeDoorUpgrade()
	{
		StringBuilder doorIds = new StringBuilder(100);
		for (L2DoorInstance door : getDoors())
		{
			doorIds.append(door.getDoorId()).append(',');
		}
		doorIds.deleteCharAt(doorIds.length() - 1);
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			Statement s = con.createStatement())
		{
			s.executeQuery("DELETE FROM castle_doorupgrade WHERE doorId IN (" + doorIds.toString() + ")");
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Exception: removeDoorUpgrade(): " + e.getMessage(), e);
		}
	}
	
	private void saveDoorUpgrade(int doorId, int hp, int pDef, int mDef)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement("INSERT INTO castle_doorupgrade (doorId, hp, pDef, mDef) values (?,?,?,?)"))
		{
			ps.setInt(1, doorId);
			ps.setInt(2, hp);
			ps.setInt(3, pDef);
			ps.setInt(4, mDef);
			ps.execute();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Exception: saveDoorUpgrade(int doorId, int hp, int pDef, int mDef): " + e.getMessage(), e);
		}
	}
	
	private void updateOwnerInDB(L2Clan clan)
	{
		if (clan != null)
		{
			_ownerId = clan.getClanId(); // Update owner id property
		}
		else
		{
			_ownerId = 0; // Remove owner
			resetManor();
		}
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			// NEED TO REMOVE HAS CASTLE FLAG FROM CLAN_DATA
			// SHOULD BE CHECKED FROM CASTLE TABLE
			PreparedStatement statement = con.prepareStatement("UPDATE clan_data SET hasCastle = 0 WHERE hasCastle = ?");
			statement.setInt(1, getCastleId());
			statement.execute();
			statement.close();
			
			statement = con.prepareStatement("UPDATE clan_data SET hasCastle = ? WHERE clan_id = ?");
			statement.setInt(1, getCastleId());
			statement.setInt(2, getOwnerId());
			statement.execute();
			statement.close();
			
			// Announce to clan members
			if (clan != null)
			{
				clan.setCastleId(getCastleId()); // Set has castle flag for new owner
				clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));
				clan.broadcastToOnlineMembers(new PlaySound(1, "Siege_Victory", 0, 0, 0, 0, 0));
				ThreadPoolManager.getInstance().scheduleGeneral(new CastleUpdater(clan, 1), 3600000); // Schedule owner tasks to start running
			}
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Exception: updateOwnerInDB(L2Clan clan): " + e.getMessage(), e);
		}
	}
	
	public final int getCastleId()
	{
		return _castleId;
	}
	
	public final L2DoorInstance getDoor(int doorId)
	{
		if (doorId <= 0)
		{
			return null;
		}
		
		for (L2DoorInstance door : getDoors())
		{
			if (door.getDoorId() == doorId)
			{
				return door;
			}
		}
		return null;
	}
	
	public final List<L2DoorInstance> getDoors()
	{
		return _doors;
	}
	
	public final String getName()
	{
		return _name;
	}
	
	public final int getOwnerId()
	{
		return _ownerId;
	}
	
	public final Siege getSiege()
	{
		if (_siege == null)
		{
			_siege = new Siege(new Castle[]
			{
				this
			});
		}
		return _siege;
	}
	
	public final Calendar getSiegeDate()
	{
		return _siegeDate;
	}
	
	public boolean getIsTimeRegistrationOver()
	{
		return _isTimeRegistrationOver;
	}
	
	public void setIsTimeRegistrationOver(boolean val)
	{
		_isTimeRegistrationOver = val;
	}
	
	public Calendar getTimeRegistrationOverDate()
	{
		if (_siegeTimeRegistrationEndDate == null)
		{
			_siegeTimeRegistrationEndDate = Calendar.getInstance();
		}
		return _siegeTimeRegistrationEndDate;
	}
	
	public final int getTaxPercent()
	{
		return _taxPercent;
	}
	
	public final double getTaxRate()
	{
		return _taxRate;
	}
	
	public final long getTreasury()
	{
		return _treasury;
	}
	
	public final boolean getShowNpcCrest()
	{
		return _showNpcCrest;
	}
	
	public final void setShowNpcCrest(boolean showNpcCrest)
	{
		if (_showNpcCrest != showNpcCrest)
		{
			_showNpcCrest = showNpcCrest;
			updateShowNpcCrest();
		}
	}
	
	public List<SeedProduction> getSeedProduction(int period)
	{
		return (period == CastleManorManager.PERIOD_CURRENT ? _production : _productionNext);
	}
	
	public List<CropProcure> getCropProcure(int period)
	{
		return (period == CastleManorManager.PERIOD_CURRENT ? _procure : _procureNext);
	}
	
	public void setSeedProduction(List<SeedProduction> seed, int period)
	{
		if (period == CastleManorManager.PERIOD_CURRENT)
		{
			_production = seed;
		}
		else
		{
			_productionNext = seed;
		}
	}
	
	public void setCropProcure(List<CropProcure> crop, int period)
	{
		if (period == CastleManorManager.PERIOD_CURRENT)
		{
			_procure = crop;
		}
		else
		{
			_procureNext = crop;
		}
	}
	
	public SeedProduction getSeed(int seedId, int period)
	{
		for (SeedProduction seed : getSeedProduction(period))
		{
			if (seed.getId() == seedId)
			{
				return seed;
			}
		}
		return null;
	}
	
	public CropProcure getCrop(int cropId, int period)
	{
		for (CropProcure crop : getCropProcure(period))
		{
			if (crop.getId() == cropId)
			{
				return crop;
			}
		}
		return null;
	}
	
	public long getManorCost(int period)
	{
		List<CropProcure> procure;
		List<SeedProduction> production;
		
		if (period == CastleManorManager.PERIOD_CURRENT)
		{
			procure = _procure;
			production = _production;
		}
		else
		{
			procure = _procureNext;
			production = _productionNext;
		}
		
		long total = 0;
		if (production != null)
		{
			for (SeedProduction seed : production)
			{
				total += ManorData.getInstance().getSeedBuyPrice(seed.getId()) * seed.getStartProduce();
			}
		}
		if (procure != null)
		{
			for (CropProcure crop : procure)
			{
				total += crop.getPrice() * crop.getStartAmount();
			}
		}
		return total;
	}
	
	// save manor production data
	public void saveSeedData()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			try (PreparedStatement ps1 = con.prepareStatement(CASTLE_MANOR_DELETE_PRODUCTION))
			{
				ps1.setInt(1, getCastleId());
				ps1.execute();
			}
			
			if (_production != null)
			{
				int count = 0;
				StringBuilder query = new StringBuilder();
				query.append("INSERT INTO castle_manor_production VALUES ");
				String values[] = new String[_production.size()];
				for (SeedProduction s : _production)
				{
					values[count++] = "(" + getCastleId() + "," + s.getId() + "," + s.getCanProduce() + "," + s.getStartProduce() + "," + s.getPrice() + "," + CastleManorManager.PERIOD_CURRENT + ")";
				}
				if (values.length > 0)
				{
					query.append(values[0]);
					for (int i = 1; i < values.length; i++)
					{
						query.append(',');
						query.append(values[i]);
					}
					try (PreparedStatement ps2 = con.prepareStatement(query.toString()))
					{
						ps2.execute();
					}
				}
			}
			
			if (_productionNext != null)
			{
				int count = 0;
				String query = "INSERT INTO castle_manor_production VALUES ";
				String values[] = new String[_productionNext.size()];
				for (SeedProduction s : _productionNext)
				{
					values[count++] = "(" + getCastleId() + "," + s.getId() + "," + s.getCanProduce() + "," + s.getStartProduce() + "," + s.getPrice() + "," + CastleManorManager.PERIOD_NEXT + ")";
				}
				if (values.length > 0)
				{
					query += values[0];
					for (int i = 1; i < values.length; i++)
					{
						query += "," + values[i];
					}
					try (PreparedStatement ps3 = con.prepareStatement(query))
					{
						ps3.execute();
					}
				}
			}
		}
		catch (Exception e)
		{
			_log.info("Error adding seed production data for castle " + getName() + ": " + e.getMessage());
		}
	}
	
	// save manor production data for specified period
	public void saveSeedData(int period)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement(CASTLE_MANOR_DELETE_PRODUCTION_PERIOD);
			statement.setInt(1, getCastleId());
			statement.setInt(2, period);
			statement.execute();
			statement.close();
			
			List<SeedProduction> prod = null;
			prod = getSeedProduction(period);
			
			if (prod != null)
			{
				int count = 0;
				StringBuilder query = new StringBuilder();
				query.append("INSERT INTO castle_manor_production VALUES ");
				String values[] = new String[prod.size()];
				for (SeedProduction s : prod)
				{
					values[count++] = "(" + getCastleId() + "," + s.getId() + "," + s.getCanProduce() + "," + s.getStartProduce() + "," + s.getPrice() + "," + period + ")";
				}
				if (values.length > 0)
				{
					query.append(values[0]);
					for (int i = 1; i < values.length; i++)
					{
						query.append(',').append(values[i]);
					}
					statement = con.prepareStatement(query.toString());
					statement.execute();
					statement.close();
				}
			}
		}
		catch (Exception e)
		{
			_log.info("Error adding seed production data for castle " + getName() + ": " + e.getMessage());
		}
	}
	
	// save crop procure data
	public void saveCropData()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			try (PreparedStatement ps1 = con.prepareStatement(CASTLE_MANOR_DELETE_PROCURE))
			{
				ps1.setInt(1, getCastleId());
				ps1.execute();
			}
			
			if (!_procure.isEmpty())
			{
				int count = 0;
				StringBuilder query = new StringBuilder();
				query.append("INSERT INTO castle_manor_procure VALUES ");
				String values[] = new String[_procure.size()];
				for (CropProcure cp : _procure)
				{
					values[count++] = "(" + getCastleId() + "," + cp.getId() + "," + cp.getAmount() + "," + cp.getStartAmount() + "," + cp.getPrice() + "," + cp.getReward() + "," + CastleManorManager.PERIOD_CURRENT + ")";
				}
				if (values.length > 0)
				{
					query.append(values[0]);
					for (int i = 1; i < values.length; i++)
					{
						query.append(',');
						query.append(values[i]);
					}
					try (PreparedStatement ps2 = con.prepareStatement(query.toString()))
					{
						ps2.execute();
					}
				}
			}
			if (!_procureNext.isEmpty())
			{
				int count = 0;
				String query = "INSERT INTO castle_manor_procure VALUES ";
				String values[] = new String[_procureNext.size()];
				for (CropProcure cp : _procureNext)
				{
					values[count++] = "(" + getCastleId() + "," + cp.getId() + "," + cp.getAmount() + "," + cp.getStartAmount() + "," + cp.getPrice() + "," + cp.getReward() + "," + CastleManorManager.PERIOD_NEXT + ")";
				}
				if (values.length > 0)
				{
					query += values[0];
					for (int i = 1; i < values.length; i++)
					{
						query += "," + values[i];
					}
					try (PreparedStatement ps3 = con.prepareStatement(query))
					{
						ps3.execute();
					}
				}
			}
		}
		catch (Exception e)
		{
			_log.info("Error adding crop data for castle " + getName() + ": " + e.getMessage());
		}
	}
	
	// save crop procure data for specified period
	public void saveCropData(int period)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement(CASTLE_MANOR_DELETE_PROCURE_PERIOD);
			statement.setInt(1, getCastleId());
			statement.setInt(2, period);
			statement.execute();
			statement.close();
			
			List<CropProcure> proc = null;
			proc = getCropProcure(period);
			
			if ((proc != null) && (proc.size() > 0))
			{
				int count = 0;
				StringBuilder query = new StringBuilder();
				query.append("INSERT INTO castle_manor_procure VALUES ");
				String values[] = new String[proc.size()];
				
				for (CropProcure cp : proc)
				{
					values[count++] = "(" + getCastleId() + "," + cp.getId() + "," + cp.getAmount() + "," + cp.getStartAmount() + "," + cp.getPrice() + "," + cp.getReward() + "," + period + ")";
				}
				if (values.length > 0)
				{
					query.append(values[0]);
					for (int i = 1; i < values.length; i++)
					{
						query.append(',');
						query.append(values[i]);
					}
					statement = con.prepareStatement(query.toString());
					statement.execute();
					statement.close();
				}
			}
		}
		catch (Exception e)
		{
			_log.info("Error adding crop data for castle " + getName() + ": " + e.getMessage());
		}
	}
	
	public void updateCrop(int cropId, long amount, int period)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(CASTLE_UPDATE_CROP))
		{
			ps.setLong(1, amount);
			ps.setInt(2, cropId);
			ps.setInt(3, getCastleId());
			ps.setInt(4, period);
			ps.execute();
		}
		catch (Exception e)
		{
			_log.info("Error adding crop data for castle " + getName() + ": " + e.getMessage());
		}
	}
	
	public void updateSeed(int seedId, long amount, int period)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(CASTLE_UPDATE_SEED))
		{
			ps.setLong(1, amount);
			ps.setInt(2, seedId);
			ps.setInt(3, getCastleId());
			ps.setInt(4, period);
			ps.execute();
		}
		catch (Exception e)
		{
			_log.info("Error adding seed production data for castle " + getName() + ": " + e.getMessage());
		}
	}
	
	public boolean isNextPeriodApproved()
	{
		return _isNextPeriodApproved;
	}
	
	public void setNextPeriodApproved(boolean val)
	{
		_isNextPeriodApproved = val;
	}
	
	public void updateClansReputation()
	{
		if (_formerOwner != null)
		{
			if (_formerOwner != ClanTable.getInstance().getClan(getOwnerId()))
			{
				int maxreward = Math.max(0, _formerOwner.getReputationScore());
				_formerOwner.takeReputationScore(Config.LOOSE_CASTLE_POINTS, true);
				L2Clan owner = ClanTable.getInstance().getClan(getOwnerId());
				if (owner != null)
				{
					owner.addReputationScore(Math.min(Config.TAKE_CASTLE_POINTS, maxreward), true);
				}
			}
			else
			{
				_formerOwner.addReputationScore(Config.CASTLE_DEFENDED_POINTS, true);
			}
		}
		else
		{
			L2Clan owner = ClanTable.getInstance().getClan(getOwnerId());
			if (owner != null)
			{
				owner.addReputationScore(Config.TAKE_CASTLE_POINTS, true);
			}
		}
	}
	
	public void updateShowNpcCrest()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE castle SET showNpcCrest = ? WHERE id = ?"))
		{
			ps.setString(1, String.valueOf(getShowNpcCrest()));
			ps.setInt(2, getCastleId());
			ps.execute();
		}
		catch (Exception e)
		{
			_log.info("Error saving showNpcCrest for castle " + getName() + ": " + e.getMessage());
		}
	}
	
	public List<L2Skill> getResidentialSkills()
	{
		return _residentialSkills;
	}
	
	public void giveResidentialSkills(L2PcInstance player)
	{
		for (L2Skill sk : _residentialSkills)
		{
			player.addSkill(sk, false);
		}
		Territory territory = TerritoryWarManager.getInstance().getTerritory(getCastleId());
		if ((territory != null) && territory.getOwnedWardIds().contains(getCastleId() + 80))
		{
			for (int wardId : territory.getOwnedWardIds())
			{
				final List<L2SkillLearn> territorySkills = SkillTreesData.getInstance().getAvailableResidentialSkills(wardId);
				for (L2SkillLearn s : territorySkills)
				{
					final L2Skill sk = SkillTable.getInstance().getInfo(s.getSkillId(), s.getSkillLevel());
					if (sk != null)
					{
						player.addSkill(sk, false);
					}
					else
					{
						_log.warning("Trying to add a null skill for Territory Ward Id: " + wardId + ", skill Id: " + s.getSkillId() + " level: " + s.getSkillLevel() + "!");
					}
				}
			}
		}
	}
	
	public void removeResidentialSkills(L2PcInstance player)
	{
		for (L2Skill sk : _residentialSkills)
		{
			player.removeSkill(sk, false, true);
		}
		if (TerritoryWarManager.getInstance().getTerritory(getCastleId()) != null)
		{
			for (int wardId : TerritoryWarManager.getInstance().getTerritory(getCastleId()).getOwnedWardIds())
			{
				final List<L2SkillLearn> territorySkills = SkillTreesData.getInstance().getAvailableResidentialSkills(wardId);
				for (L2SkillLearn s : territorySkills)
				{
					final L2Skill sk = SkillTable.getInstance().getInfo(s.getSkillId(), s.getSkillLevel());
					if (sk != null)
					{
						player.removeSkill(sk, false, true);
					}
					else
					{
						_log.warning("Trying to remove a null skill for Territory Ward Id: " + wardId + ", skill Id: " + s.getSkillId() + " level: " + s.getSkillLevel() + "!");
					}
				}
			}
		}
	}
	
	/**
	 * Register Artefact to castle
	 * @param artefact
	 */
	public void registerArtefact(L2ArtefactInstance artefact)
	{
		_artefacts.add(artefact);
	}
	
	public List<L2ArtefactInstance> getArtefacts()
	{
		return _artefacts;
	}
	
	public void resetManor()
	{
		setCropProcure(new ArrayList<CropProcure>(), CastleManorManager.PERIOD_CURRENT);
		setCropProcure(new ArrayList<CropProcure>(), CastleManorManager.PERIOD_NEXT);
		setSeedProduction(new ArrayList<SeedProduction>(), CastleManorManager.PERIOD_CURRENT);
		setSeedProduction(new ArrayList<SeedProduction>(), CastleManorManager.PERIOD_NEXT);
		if (Config.ALT_MANOR_SAVE_ALL_ACTIONS)
		{
			saveCropData();
			saveSeedData();
		}
	}
	
	/**
	 * @return the tickets exchanged for this castle
	 */
	public int getTicketBuyCount()
	{
		return _ticketBuyCount;
	}
	
	/**
	 * Set the exchanged tickets count.<br>
	 * Performs database update.
	 * @param count the ticket count to set
	 */
	public void setTicketBuyCount(int count)
	{
		_ticketBuyCount = count;
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE castle SET ticketBuyCount = ? WHERE id = ?"))
		{
			statement.setInt(1, _ticketBuyCount);
			statement.setInt(2, _castleId);
			statement.execute();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, e.getMessage(), e);
		}
	}
	
	@Override
	public String toString()
	{
		return _name + "(" + _castleId + ")";
	}
}
