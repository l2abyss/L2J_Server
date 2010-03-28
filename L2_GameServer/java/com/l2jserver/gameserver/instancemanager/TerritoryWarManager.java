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
package com.l2jserver.gameserver.instancemanager;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Calendar;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2jserver.Config;
import com.l2jserver.L2DatabaseFactory;
import com.l2jserver.gameserver.Announcements;
import com.l2jserver.gameserver.ThreadPoolManager;
import com.l2jserver.gameserver.datatables.ClanTable;
import com.l2jserver.gameserver.datatables.NpcTable;
import com.l2jserver.gameserver.datatables.ResidentialSkillTable;
import com.l2jserver.gameserver.model.L2Clan;
import com.l2jserver.gameserver.model.L2Skill;
import com.l2jserver.gameserver.model.L2Spawn;
import com.l2jserver.gameserver.model.L2World;
import com.l2jserver.gameserver.model.Location;
import com.l2jserver.gameserver.model.TerritoryWard;
import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.model.actor.instance.L2DoorInstance;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.actor.instance.L2SiegeFlagInstance;
import com.l2jserver.gameserver.model.entity.Castle;
import com.l2jserver.gameserver.model.entity.Fort;
import com.l2jserver.gameserver.model.quest.Quest;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.CharInfo;
import com.l2jserver.gameserver.network.serverpackets.ExBrExtraUserInfo;
import com.l2jserver.gameserver.network.serverpackets.L2GameServerPacket;
import com.l2jserver.gameserver.network.serverpackets.RelationChanged;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;
import com.l2jserver.gameserver.network.serverpackets.UserInfo;
import com.l2jserver.gameserver.templates.chars.L2NpcTemplate;
import com.l2jserver.gameserver.util.Util;
import com.l2jserver.util.L2Properties;

import javolution.util.FastList;
import javolution.util.FastMap;

public class TerritoryWarManager
{
	private static final Logger _log = Logger.getLogger(TerritoryWarManager.class.getName());
	
	public static final TerritoryWarManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	// =========================================================
	// Data Field
	public static String qn = "TerritoryWarSuperClass";
	public static int DEFENDERMAXCLANS; // Max number of clans
	public static int DEFENDERMAXPLAYERS; // Max number of individual player
	public static int CLANMINLEVEL;
	public static int PLAYERMINLEVEL;
	public static int MINTWBADGEFORNOBLESS;
	public static int MINTWBADGEFORSTRIDERS;
	public static int MINTWBADGEFORBIGSTRIDER;
	public static Long WARLENGTH;
	public static boolean PLAYER_WITH_WARD_CAN_BE_KILLED_IN_PEACEZONE;
	public final Map<Integer,Integer> TERRITORY_ITEM_IDS;
	
	// Territory War settings
	private FastMap<Integer, FastList<L2Clan>> _registeredClans;
	private FastMap<Integer, FastList<Integer>> _registeredMercenaries;
	private FastMap<Integer, Territory> _territoryList;
	private FastList<Integer> _disguisedPlayers;
	private FastList<TerritoryWard> _territoryWards;
	private FastMap<L2Clan, L2SiegeFlagInstance> _clanFlags;
	private Map<Integer,Integer[]> _participantPoints = new FastMap<Integer,Integer[]>();
	private Calendar _startTWDate = Calendar.getInstance();
	private boolean _isRegistrationOver = true;
	private boolean _isTWChannelOpen = false;
	private boolean _isTWInProgress = false;
	protected ScheduledFuture<?> _scheduledStartTWTask = null;
	protected ScheduledFuture<?> _scheduledEndTWTask = null;
	protected ScheduledFuture<?> _scheduledRewardOnlineTask = null;
	
	// =========================================================
	// Constructor
	private TerritoryWarManager()
	{
		_log.info("Initializing TerritoryWarManager");

		// init lists
		_registeredClans = new FastMap<Integer, FastList<L2Clan>>();
		_registeredMercenaries = new FastMap<Integer, FastList<Integer>>();
		_territoryList = new FastMap<Integer, Territory>();
		_territoryWards = new FastList<TerritoryWard>();
		_clanFlags = new FastMap<L2Clan, L2SiegeFlagInstance>();
		_disguisedPlayers = new FastList<Integer>();
		TERRITORY_ITEM_IDS = new FastMap<Integer,Integer>();

		// Constant data
		TERRITORY_ITEM_IDS.put(81, 13757);
		TERRITORY_ITEM_IDS.put(82, 13758);
		TERRITORY_ITEM_IDS.put(83, 13759);
		TERRITORY_ITEM_IDS.put(84, 13760);
		TERRITORY_ITEM_IDS.put(85, 13761);
		TERRITORY_ITEM_IDS.put(86, 13762);
		TERRITORY_ITEM_IDS.put(87, 13763);
		TERRITORY_ITEM_IDS.put(88, 13764);
		TERRITORY_ITEM_IDS.put(89, 13765);
		// load data from database
		load();
	}
	
	// =========================================================
	// Method - Public
	public int getRegisteredTerritoryId(L2PcInstance player)
	{
		if (player == null || !_isTWChannelOpen || player.getLevel() < PLAYERMINLEVEL)
			return 0;
		if (player.getClan() != null)
		{
			if (player.getClan().getHasCastle() > 0)
				return player.getClan().getHasCastle() + 80;
			for(int cId:_registeredClans.keySet())
				if (_registeredClans.get(cId).contains(player.getClan()))
					return cId + 80;
		}
		for(int cId:_registeredMercenaries.keySet())
			if (_registeredMercenaries.get(cId).contains(player.getObjectId()))
				return cId + 80;
		return 0;
	}
	
	public boolean isAllyField(L2PcInstance player, int fieldId)
	{
		if (player == null || player.getSiegeSide() == 0)
			return false;
		else if ((player.getSiegeSide() - 80) == fieldId)
			return true;
		else if (fieldId > 100 && _territoryList.containsKey((player.getSiegeSide() - 80))
				&& _territoryList.get((player.getSiegeSide() - 80)).getFortId() == fieldId)
			return true;
		return false;

	}
	
	/**
	 * Return true if the clan is registered<BR><BR>
	 * @param clan The L2Clan of the player
	 */
	public final boolean checkIsRegistered(int castleId, L2Clan clan)
	{
		if (clan == null)
			return false;
		else if (clan.getHasCastle() > 0)
			return (castleId == -1 ? true:(clan.getHasCastle() == castleId));
		if (castleId == -1)
		{
			for(int cId:_registeredClans.keySet())
				if (_registeredClans.get(cId).contains(clan))
					return true;
			return false;
		}
		else
			return _registeredClans.get(castleId).contains(clan);
	}
	
	/**
	 * Return true if the player is registered<BR><BR>
	 * @param integer The objectId of the player
	 */
	public final boolean checkIsRegistered(int castleId, int objId)
	{
		if (castleId == -1)
		{
			for(int cId:_registeredMercenaries.keySet())
				if (_registeredMercenaries.get(cId).contains(objId))
					return true;
			return false;
		}
		else
			return _registeredMercenaries.get(castleId).contains(objId);
	}
	
	public Territory getTerritory(int castleId)
	{
		return _territoryList.get(castleId);
	}
	
	public FastList<Territory> getAllTerritories()
	{
		FastList<Territory> ret = new FastList<Territory>();
		for (Territory t : _territoryList.values())
			if (t.getOwnerClan() != null)
				ret.add(t);
		return ret;
	}
	
	public Collection<L2Clan> getRegisteredClans(int castleId)
	{
		return _registeredClans.get(castleId);
	}
	
	public void addDisguisedPlayer(int playerObjId)
	{
		_disguisedPlayers.add(playerObjId);
	}
	
	public boolean isDisguised(int playerObjId)
	{
		return _disguisedPlayers.contains(playerObjId);
	}
	
	public Collection<Integer> getRegisteredMercenaries(int castleId)
	{
		return _registeredMercenaries.get(castleId);
	}
	
	public long getTWStartTimeInMillis()
	{
		return _startTWDate.getTimeInMillis();
	}
	
	public Calendar getTWStart()
	{
		return _startTWDate;
	}
	
	public void setTWStartTimeInMillis(long time)
	{
		_startTWDate.setTimeInMillis(time);
		if (_isTWInProgress)
		{
			if (_scheduledEndTWTask != null)
				_scheduledEndTWTask.cancel(false);
			_scheduledEndTWTask = ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleEndTWTask(), 1000);
		}
		else
		{
			if (_scheduledStartTWTask != null)
				_scheduledStartTWTask.cancel(false);
			_scheduledStartTWTask = ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleStartTWTask(), 1000);
		}
	}
	
	public boolean isTWChannelOpen()
	{
		return _isTWChannelOpen;
	}
	
	public void registerClan(int castleId, L2Clan clan)
	{
		if (clan == null || (_registeredClans.get(castleId) != null && _registeredClans.get(castleId).contains(clan)))
			return;
		else if (_registeredClans.get(castleId) == null)
			_registeredClans.put(castleId, new FastList<L2Clan>());
		
		_registeredClans.get(castleId).add(clan);
		changeRegistration(castleId, clan.getClanId(), false);
	}
	
	public void registerMerc(int castleId, L2PcInstance player)
	{
		if (player == null || player.getLevel() < PLAYERMINLEVEL
				|| (_registeredMercenaries.get(castleId) != null && _registeredMercenaries.get(castleId).contains(player.getObjectId())))
			return;
		else if (_registeredMercenaries.get(castleId) == null)
			_registeredMercenaries.put(castleId, new FastList<Integer>());
		
		_registeredMercenaries.get(castleId).add(player.getObjectId());
		changeRegistration(castleId, player.getObjectId(), false);
	}
	
	public void removeClan(int castleId, L2Clan clan)
	{
		if (clan == null)
			return;
		else if (_registeredClans.get(castleId) != null && _registeredClans.get(castleId).contains(clan))
		{
			_registeredClans.get(castleId).remove(clan);
			changeRegistration(castleId, clan.getClanId(), true);
		}
	}

	public void removeMerc(int castleId, L2PcInstance player)
	{
		if (player == null)
			return;
		else if (_registeredMercenaries.get(castleId) != null && _registeredMercenaries.get(castleId).contains(player.getObjectId()))
		{
			_registeredMercenaries.get(castleId).remove(_registeredMercenaries.get(castleId).indexOf(player.getObjectId()));
			changeRegistration(castleId, player.getObjectId(), true);
		}
	}
	
	public boolean getIsRegistrationOver()
	{
		return _isRegistrationOver;
	}
	
	public boolean isTWInProgress()
	{
		return _isTWInProgress;
	}
	
	public void territoryCatapultDestroyed(int castleId)
	{
		if (_territoryList.get(castleId) != null)
			_territoryList.get(castleId).changeNPCsSpawn(2, false);
		for (L2DoorInstance door : CastleManager.getInstance().getCastleById(castleId).getDoors())
			door.openMe();
	}
	
	public L2Npc addTerritoryWard(int territoryId, int newOwnerId, int oldOwnerId)
	{
		L2Npc ret = null;
		if (_territoryList.get(newOwnerId) != null)
		{
			Territory terNew = _territoryList.get(newOwnerId);
			TerritoryNPCSpawn ward = terNew.getFreeWardSpawnPlace();
			if (ward != null)
			{
				ward._npcId = territoryId;
				ret = spawnNPC(36491 + territoryId, ward.getLocation());
				if (!isTWInProgress())
					ret.setIsInvul(true);
				ward.setNPC(ret);
				if (terNew.getOwnerClan() != null && terNew.getOwnedWardIds().contains(newOwnerId + 80))
					for(int wardId : terNew.getOwnedWardIds())
						if (ResidentialSkillTable.getInstance().getSkills(wardId) != null)
							for (L2Skill sk : ResidentialSkillTable.getInstance().getSkills(wardId))
								for (L2PcInstance member : terNew.getOwnerClan().getOnlineMembers(0))
									if (!member.isInOlympiadMode())
										member.addSkill(sk, false);
			}
			if (_territoryList.containsKey(oldOwnerId))
			{
				Territory terOld = _territoryList.get(oldOwnerId);
				terOld.removeWard(territoryId);
				updateTerritoryData(terOld);
				updateTerritoryData(terNew);
				SystemMessage sm = new SystemMessage(SystemMessageId.CLAN_S1_HAS_SUCCEDED_IN_CAPTURING_S2_TERRITORY_WARD);
				sm.addString(terNew.getOwnerClan().getName());
				sm.addString(ward.getNpc().getName().replaceAll(" Ward", ""));
				announceToParticipants(sm, 135000, 13500);
				if (terOld.getOwnerClan() != null)
				{
					if (ResidentialSkillTable.getInstance().getSkills(territoryId) != null)
						for (L2Skill sk : ResidentialSkillTable.getInstance().getSkills(territoryId))
							for (L2PcInstance member : terOld.getOwnerClan().getOnlineMembers(0))
								member.removeSkill(sk, false);
					if (!terOld.getOwnedWardIds().isEmpty() && !terOld.getOwnedWardIds().contains(oldOwnerId + 80))
						for(int wardId : terOld.getOwnedWardIds())
							if (ResidentialSkillTable.getInstance().getSkills(wardId) != null)
								for (L2Skill sk : ResidentialSkillTable.getInstance().getSkills(wardId))
									for (L2PcInstance member : terOld.getOwnerClan().getOnlineMembers(0))
										member.removeSkill(sk, false);
				}
			}
		}
		else
			_log.warning("TerritoryWarManager: Missing territory for new Ward owner: " + newOwnerId + ";" + territoryId);
		return ret;
	}
	
	public L2SiegeFlagInstance getHQForClan(L2Clan clan)
	{
		if (clan.getHasCastle() > 0)
			return _territoryList.get(clan.getHasCastle()).getHQ();
		return null;
	}
	
	public L2SiegeFlagInstance getHQForTerritory(int territoryId)
	{
		if (_territoryList.containsKey(territoryId - 80))
			return _territoryList.get(territoryId - 80).getHQ();
		return null;
	}
	
	public void setHQForClan(L2Clan clan, L2SiegeFlagInstance hq)
	{
		if (clan.getHasCastle() > 0)
			_territoryList.get(clan.getHasCastle()).setHQ(hq);
	}
	
	public void addClanFlag(L2Clan clan, L2SiegeFlagInstance flag)
	{
		_clanFlags.put(clan, flag);
	}
	
	public boolean isClanHasFlag(L2Clan clan)
	{
		return _clanFlags.containsKey(clan);
	}
	
	public L2SiegeFlagInstance getFlagForClan(L2Clan clan)
	{
		if (_clanFlags.containsKey(clan))
			return _clanFlags.get(clan);
		return null;
	}
	
	public void removeClanFlag(L2Clan clan)
	{
		_clanFlags.remove(clan);
	}
	
	public FastList<TerritoryWard> getAllTerritoryWards()
	{
		return _territoryWards;
	}
	
	public TerritoryWard getTerritoryWardForOwner(int castleId)
	{
		for(TerritoryWard twWard : _territoryWards)
			if (twWard.getTerritoryId() == castleId)
				return twWard;
		return null;
	}
	
	public TerritoryWard getTerritoryWard(int territoryId)
	{
		for(TerritoryWard twWard : _territoryWards)
			if (twWard.getTerritoryId() == territoryId)
				return twWard;
		return null;
	}
	
	public TerritoryWard getTerritoryWard(L2PcInstance player)
	{
		for(TerritoryWard twWard : _territoryWards)
			if (twWard.playerId == player.getObjectId())
				return twWard;
		return null;
	}
	
	public void dropCombatFlag(L2PcInstance player, boolean isKilled)
	{
		for(TerritoryWard twWard : _territoryWards)
			if (twWard.playerId == player.getObjectId())
			{
				twWard.dropIt();
				if (isTWInProgress())
					twWard.spawnMe();
				if (isKilled)
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.THE_CHAR_THAT_ACQUIRED_S1_WARD_HAS_BEEN_KILLED);
					sm.addString(twWard.itemInstance.getName().replaceAll(" Ward", ""));
					announceToParticipants(sm, 0, 0);
				}
			}
	}
	
	public void giveTWQuestPoint(L2PcInstance player)
	{
		if (!_participantPoints.containsKey(player.getObjectId()))
			_participantPoints.put(player.getObjectId(), new Integer[]{player.getSiegeSide(),0,0,0,0,0,0});
		_participantPoints.get(player.getObjectId())[2]++;
	}
	
	public void giveTWPoint(L2PcInstance killer, int victimSide, int type)
	{
		if (victimSide == 0)
			return;
		if (killer.getParty() != null && type < 5)
			for(L2PcInstance pl : killer.getParty().getPartyMembers())
			{
				if (pl.getSiegeSide() == victimSide || pl.getSiegeSide() == 0 || !Util.checkIfInRange(2000, killer, pl, false))
					continue;
				else if (!_participantPoints.containsKey(pl.getObjectId()))
					_participantPoints.put(pl.getObjectId(), new Integer[]{pl.getSiegeSide(),0,0,0,0,0,0});
				_participantPoints.get(pl.getObjectId())[type]++;
			}
		else
		{
			if (killer.getSiegeSide() == victimSide || killer.getSiegeSide() == 0)
				return;
			else if (!_participantPoints.containsKey(killer.getObjectId()))
				_participantPoints.put(killer.getObjectId(), new Integer[]{killer.getSiegeSide(),0,0,0,0,0,0});
			_participantPoints.get(killer.getObjectId())[type]++;
		}
	}
	
	public int[] calcReward(L2PcInstance player)
	{
		if (_participantPoints.containsKey(player.getObjectId()))
		{
			int[] reward = new int[2];
			Integer[] temp = _participantPoints.get(player.getObjectId());
			reward[0] = temp[0];
			reward[1] = 0;
			// badges for being online. if char was not online at least 10 mins
			// than he cant get rewards(also this will handle that player already get his/her rewards)
			if (temp[6] < 10)
				return reward;
			reward[1] += (temp[6] > 70 ? 7 : (int)(temp[6] * 0.1));
			// badges for player Quests
			reward[1] += temp[2] * 7;
			// badges for player Kills
			if (temp[1] < 50)
				reward[1] += temp[1] * 0.1;
			else if (temp[1] < 120)
				reward[1] += (5 + (temp[1] - 50) / 14);
			else
				reward[1] += 10;
			// badges for territory npcs
			reward[1] += temp[3];
			// badges for territory catapults
			reward[1] += temp[4] * 2;
			// badges for territory Wards
			reward[1] += (temp[5] > 0 ? 5 : 0);
			// badges for territory quest done
			reward[1] += Math.min(_territoryList.get(temp[0] - 80).getQuestDone()[0], 10);
			reward[1] += _territoryList.get(temp[0] - 80).getQuestDone()[1];
			reward[1] += _territoryList.get(temp[0] - 80).getOwnedWardIds().size();
			return reward;
		}
		return new int[]{0,0};
	}
	
	public void debugReward(L2PcInstance player)
	{
		player.sendMessage("Registred TerrId: " + player.getSiegeSide());
		if (_participantPoints.containsKey(player.getObjectId()))
		{
			Integer[] temp = _participantPoints.get(player.getObjectId());
			player.sendMessage("TerrId: " + temp[0]);
			player.sendMessage("PcKill: " + temp[1]);
			player.sendMessage("PcQuests: " + temp[2]);
			player.sendMessage("npcKill: " + temp[3]);
			player.sendMessage("CatatKill: " + temp[4]);
			player.sendMessage("WardKill: " + temp[5]);
			player.sendMessage("onlineTime: " + temp[6]);
		}
		else
			player.sendMessage("No points for you!");
		if (_territoryList.containsKey(player.getSiegeSide() - 80))
		{
			player.sendMessage("Your Territory's jobs:");
			player.sendMessage("npcKill: " + _territoryList.get(player.getSiegeSide() - 80).getQuestDone()[0]);
			player.sendMessage("WardCaptured: " + _territoryList.get(player.getSiegeSide() - 80).getQuestDone()[1]);
		}
	}
	
	public void resetReward(L2PcInstance player)
	{
		if (_participantPoints.containsKey(player.getObjectId()))
		{
			_participantPoints.get(player.getObjectId())[6] = 0;
		}
	}
	
	// =========================================================
	// Method - Private
	private L2Npc spawnNPC(int npcId, Location loc)
	{
		L2NpcTemplate template = NpcTable.getInstance().getTemplate(npcId);
		if (template != null)
		{
			L2Spawn spawnDat;
			try
			{
				spawnDat = new L2Spawn(template);
				spawnDat.setAmount(1);
				spawnDat.setLocx(loc.getX());
				spawnDat.setLocy(loc.getY());
				spawnDat.setLocz(loc.getZ());
				spawnDat.setHeading(loc.getHeading());
				spawnDat.stopRespawn();
				return spawnDat.spawnOne(false);
			}
			catch (Exception e)
			{
				_log.warning("Territory War Manager: " + e.getMessage());
				e.printStackTrace();
				return null;
			}
		}
		else
		{
			_log.warning("Territory War Manager: Data missing in NPC table for ID: " + npcId + ".");
			return null;
		}
	}
	
	private void changeRegistration(int castleId, int objId, boolean delete)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			if (delete)
				statement = con.prepareStatement("DELETE FROM territory_registrations WHERE castleId=? and registeredId=?");
			else
				statement = con.prepareStatement("INSERT INTO territory_registrations (castleId, registeredId) values (?,?)");
			statement.setInt(1, castleId);
			statement.setInt(2, objId);
			statement.execute();
		}
		catch (Exception e)
		{
			_log.warning("Exception: Territory War registration: " + e.getMessage());
			e.printStackTrace();
		}
		finally
		{
			try
			{
				statement.close();
			}
			catch (Exception e)
			{
			}
			try
			{
				con.close();
			}
			catch (Exception e)
			{
			}
		}
	}
	
	private void updateTerritoryData(Territory ter)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE territories SET ownedWardIds=? WHERE territoryId=?");
			String wardList = "";
			for (int i : ter.getOwnedWardIds())
				wardList += (i + ";");
			statement.setString(1, wardList);
			statement.setInt(2, ter.getTerritoryId());
			statement.execute();
		}
		catch (Exception e)
		{
			_log.warning("Exception: Territory Data update: " + e.getMessage());
			e.printStackTrace();
		}
		finally
		{
			try
			{
				statement.close();
			}
			catch (Exception e)
			{
			}
			try
			{
				con.close();
			}
			catch (Exception e)
			{
			}
		}
	}

	private final void load()
	{
		InputStream is = null;
		try
		{
			is = new FileInputStream(new File(Config.TW_CONFIGURATION_FILE));
			L2Properties territoryWarSettings = new L2Properties();
			territoryWarSettings.load(is);
			
			// Siege setting
			DEFENDERMAXCLANS = Integer.decode(territoryWarSettings.getProperty("DefenderMaxClans", "500"));
			DEFENDERMAXPLAYERS = Integer.decode(territoryWarSettings.getProperty("DefenderMaxPlayers", "500"));
			CLANMINLEVEL = Integer.decode(territoryWarSettings.getProperty("ClanMinLevel", "0"));
			PLAYERMINLEVEL = Integer.decode(territoryWarSettings.getProperty("PlayerMinLevel", "40"));
			WARLENGTH = Long.decode(territoryWarSettings.getProperty("WarLength", "120")) * 60000;
			PLAYER_WITH_WARD_CAN_BE_KILLED_IN_PEACEZONE = Boolean.parseBoolean(territoryWarSettings.getProperty("PlayerWithWardCanBeKilledInPeaceZone", "False"));
			MINTWBADGEFORNOBLESS = Integer.decode(territoryWarSettings.getProperty("MinTerritoryBadgeForNobless", "100"));
			MINTWBADGEFORSTRIDERS = Integer.decode(territoryWarSettings.getProperty("MinTerritoryBadgeForStriders", "50"));
			MINTWBADGEFORBIGSTRIDER = Integer.decode(territoryWarSettings.getProperty("MinTerritoryBadgeForBigStrider", "80"));
			
			Connection con = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement("SELECT * FROM territory_spawnlist");
				ResultSet rs = statement.executeQuery();
				
				while (rs.next())
				{
					int castleId = rs.getInt("castleId");
					int npcId = rs.getInt("npcId");
					Location loc = new Location(rs.getInt("x"),rs.getInt("y"),rs.getInt("z"),rs.getInt("heading"));
					int spawnType = rs.getInt("spawnType");
					if (!_territoryList.containsKey(castleId))
						_territoryList.put(castleId, new Territory(castleId));
					switch(spawnType)
					{
						case 0: // town npcs
						case 1: // fortress npcs
						case 2: // castle npcs
							_territoryList.get(castleId).getSpawnList().add(new TerritoryNPCSpawn(castleId, loc, npcId, spawnType, null));
							break;
						case 3: // ward spawns
							_territoryList.get(castleId).addWardSpawnPlace(loc);
							break;
						default:
							_log.warning("Territory War Manager: Unknown npc type for " + rs.getInt("id"));
					}
				}
				
				rs.close();
				statement.close();
			}
			catch (Exception e)
			{
				_log.warning("Territory War Manager: SpawnList error: " + e.getMessage());
				e.printStackTrace();
			}
			finally
			{
				try
				{
					con.close();
				}
				catch (Exception e)
				{
				}
			}
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement("SELECT * FROM territories");
				ResultSet rs = statement.executeQuery();
				
				while (rs.next())
				{
					int castleId = rs.getInt("castleId");
					int fortId = rs.getInt("fortId");
					String ownedWardIds = rs.getString("OwnedWardIds");
					
					Territory t = _territoryList.get(castleId);
					if (t != null)
					{
						t._fortId = fortId;
						if (CastleManager.getInstance().getCastleById(castleId).getOwnerId() > 0)
						{
							t.setOwnerClan(ClanTable.getInstance().getClan(CastleManager.getInstance().getCastleById(castleId).getOwnerId()));
							t.changeNPCsSpawn(0, true);
						}
						
						if (!ownedWardIds.isEmpty())
						{
							for(String wardId:ownedWardIds.split(";"))
								if (Integer.parseInt(wardId) > 0)
									addTerritoryWard(Integer.parseInt(wardId), castleId, 0);
						}
					}
				}
				rs.close();
				statement.close();
			}
			catch (Exception e)
			{
				_log.warning("Territory War Manager: territory list error(): " + e.getMessage());
				e.printStackTrace();
			}
			finally
			{
				try
				{
					con.close();
				}
				catch (Exception e)
				{
				}
			}
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement("SELECT * FROM territory_registrations");
				ResultSet rs = statement.executeQuery();
				
				while (rs.next())
				{
					int castleId = rs.getInt("castleId");
					int registeredId = rs.getInt("registeredId");
					if (ClanTable.getInstance().getClan(registeredId) != null)
					{
						if (_registeredClans.get(castleId) == null)
							_registeredClans.put(castleId, new FastList<L2Clan>());
						_registeredClans.get(castleId).add(ClanTable.getInstance().getClan(registeredId));
					}
					else
					{
						if (_registeredMercenaries.get(castleId) == null)
							_registeredMercenaries.put(castleId, new FastList<Integer>());
						_registeredMercenaries.get(castleId).add(registeredId);
					}
				}
				rs.close();
				statement.close();
			}
			catch (Exception e)
			{
				_log.warning("Territory War Manager: registration list error: " + e.getMessage());
				e.printStackTrace();
			}
			finally
			{
				try
				{
					con.close();
				}
				catch (Exception e)
				{
				}
			}
		}
		catch (Exception e)
		{
			//_initialized = false;
			_log.warning("Error while loading Territory War Manager!");
			e.printStackTrace();
		}
		finally
		{
			try
			{
				is.close();
			}
			catch (Exception e)
			{
			}
		}
	}
	
	private void startTerritoryWar()
	{
		if (_territoryList == null || _territoryList.size() < 2)
		{
			// change next TW date
			return;
		}
		_isTWInProgress = true;
		if (!updatePlayerTWStateFlags(false))
			return;

		// teleportPlayer(Siege.TeleportWhoType.Attacker, MapRegionTable.TeleportWhereType.Town); // Teleport to the closest town
		for(Territory t : _territoryList.values())
		{
			Castle castle = CastleManager.getInstance().getCastleById(t.getCastleId());
			Fort fort = FortManager.getInstance().getFortById(t.getFortId());
			// spawnControlTower(t.getCastleId()); // Spawn control tower
			if (castle != null)
			{
				t.changeNPCsSpawn(2, true);
				castle.spawnDoor(); // Spawn door
				castle.getZone().setIsActive(true);
				castle.getZone().updateZoneStatusForCharactersInside();
			}
			else
				_log.warning("TerritoryWarManager: Castle missing! CastleId: " + t.getCastleId());
			if (fort != null)
			{
				t.changeNPCsSpawn(1, true);
				fort.resetDoors(); // Spawn door
				fort.getZone().setIsActive(true);
				fort.getZone().updateZoneStatusForCharactersInside();
			}
			else
				_log.warning("TerritoryWarManager: Fort missing! FortId: " + t.getFortId());
			for(TerritoryNPCSpawn ward : t.getOwnedWard())
				if (ward.getNpc() != null && t.getOwnerClan() != null)
				{
					ward.getNpc().setIsInvul(false);
					ward.getNpc().broadcastStatusUpdate();
					_territoryWards.add(new TerritoryWard(ward.getNpcId(), ward.getLocation().getX(), ward.getLocation().getY(), ward.getLocation().getZ(), 0, ward.getNpcId() + 13479, t.getCastleId(), ward.getNpc()));
				}
			t.getQuestDone()[0] = 0; // killed npc
			t.getQuestDone()[1] = 0; // captured wards
		}
		_participantPoints.clear();

		SystemMessage sm = new SystemMessage(SystemMessageId.TERRITORY_WAR_HAS_BEGUN);
		Announcements.getInstance().announceToAll(sm);
	}
	
	private void endTerritoryWar()
	{
		_isTWInProgress = false;
		if (_territoryList == null || _territoryList.size() < 2)
		{
			// change next TW date
			return;
		}
		if (!updatePlayerTWStateFlags(true))
			return;

		// teleportPlayer(Siege.TeleportWhoType.Attacker, MapRegionTable.TeleportWhereType.Town); // Teleport to the closest town
		for(Territory t : _territoryList.values())
		{
			Castle castle = CastleManager.getInstance().getCastleById(t.getCastleId());
			Fort fort = FortManager.getInstance().getFortById(t.getFortId());

			if (castle != null)
			{
				castle.spawnDoor();
				t.changeNPCsSpawn(2, false);
				castle.getZone().setIsActive(false);
				castle.getZone().updateZoneStatusForCharactersInside();
			}
			else
				_log.warning("TerritoryWarManager: Castle missing! CastleId: " + t.getCastleId());

			if (fort != null)
			{
				t.changeNPCsSpawn(1, false);
				fort.getZone().setIsActive(false);
				fort.getZone().updateZoneStatusForCharactersInside();
			}
			else
				_log.warning("TerritoryWarManager: Fort missing! FortId: " + t.getFortId());
			
			if (t.getHQ() != null)
				t.getHQ().deleteMe();
			
			for(TerritoryNPCSpawn ward : t.getOwnedWard())
				if (ward.getNpc() != null)
				{
					if (ward.getNpc().isDecayed())
						ward.setNPC(ward.getNpc().getSpawn().doSpawn());
					ward.getNpc().setIsInvul(true);
					ward.getNpc().broadcastStatusUpdate();
				}
		}
		if (_territoryWards != null)
			for(TerritoryWard twWard : _territoryWards)
				twWard.unSpawnMe();
		_territoryWards.clear();
		for(L2SiegeFlagInstance flag : _clanFlags.values())
			flag.deleteMe();
		_clanFlags.clear();
		
		for(Integer castleId:_registeredClans.keySet())
			for(L2Clan clan:_registeredClans.get(castleId))
				changeRegistration(castleId, clan.getClanId(), true);
		for(Integer castleId:_registeredMercenaries.keySet())
			for(Integer pl_objId:_registeredMercenaries.get(castleId))
				changeRegistration(castleId, pl_objId, true);
		// change next TW date
		SystemMessage sm = new SystemMessage(SystemMessageId.TERRITORY_WAR_HAS_ENDED);
		Announcements.getInstance().announceToAll(sm);
	}

	private boolean updatePlayerTWStateFlags(boolean clear)
	{
		Quest twQuest = QuestManager.getInstance().getQuest(qn);
		if (twQuest == null)
		{
			_log.warning("TerritoryWarManager: missing main Quest!");
			return false;
		}
		for(int castleId : _registeredClans.keySet())
			for(L2Clan clan : _registeredClans.get(castleId))
				for(L2PcInstance player : clan.getOnlineMembers(0))
				{
					if (player == null)
						continue;
					if (clear)
					{
						player.setSiegeState((byte) 0);
						if (!_isTWChannelOpen)
							player.setSiegeSide(0);
					}
					else
					{
						if (player.getLevel() < PLAYERMINLEVEL || player.getClassId().level() < 2)
							continue;
						if (_isTWInProgress)
						{
							player.setSiegeState((byte) 1);
						}
						player.setSiegeSide(80 + castleId);
					}
					player.sendPacket(new UserInfo(player));
					player.sendPacket(new ExBrExtraUserInfo(player));
					for (L2PcInstance knownPlayer : player.getKnownList().getKnownPlayers().values())
					{
						try
						{
							knownPlayer.sendPacket(new RelationChanged(player, player.getRelation(knownPlayer), player.isAutoAttackable(knownPlayer)));
							if (player.getPet() != null)
								knownPlayer.sendPacket(new RelationChanged(player.getPet(), player.getRelation(knownPlayer), player.isAutoAttackable(knownPlayer)));
						}
						catch (NullPointerException e)
						{
						}
					}
				}
		for(int castleId : _registeredMercenaries.keySet())
			for(int objId : _registeredMercenaries.get(castleId))
			{
				L2PcInstance player = L2World.getInstance().getPlayer(objId);
				if (player == null)
					continue;
				if (clear)
				{
					player.setSiegeState((byte) 0);
					if (!_isTWChannelOpen)
						player.setSiegeSide(0);
				}
				else
				{
					if (_isTWInProgress)
					{
						player.setSiegeState((byte) 1);
					}
					player.setSiegeSide(80 + castleId);
				}
				player.sendPacket(new UserInfo(player));
				player.sendPacket(new ExBrExtraUserInfo(player));
				for (L2PcInstance knownPlayer : player.getKnownList().getKnownPlayers().values())
				{
					try
					{
						knownPlayer.sendPacket(new RelationChanged(player, player.getRelation(knownPlayer), player.isAutoAttackable(knownPlayer)));
						if (player.getPet() != null)
							knownPlayer.sendPacket(new RelationChanged(player.getPet(), player.getRelation(knownPlayer), player.isAutoAttackable(knownPlayer)));
					}
					catch (NullPointerException e)
					{
					}
				}
			}
		for(Territory terr : _territoryList.values())
			if (terr.getOwnerClan() != null)
				for(L2PcInstance player : terr.getOwnerClan().getOnlineMembers(0))
				{
					if (player == null)
						continue;
					if (clear)
					{
						player.setSiegeState((byte) 0);
						if (!_isTWChannelOpen)
							player.setSiegeSide(0);
					}
					else
					{
						if (player.getLevel() < PLAYERMINLEVEL || player.getClassId().level() < 2)
							continue;
						if (_isTWInProgress)
						{
							player.setSiegeState((byte) 1);
						}
						player.setSiegeSide(80 + terr.getCastleId());
					}
					player.sendPacket(new UserInfo(player));
					player.sendPacket(new ExBrExtraUserInfo(player));
					for (L2PcInstance knownPlayer : player.getKnownList().getKnownPlayers().values())
					{
						try
						{
							knownPlayer.sendPacket(new CharInfo(player));
							knownPlayer.sendPacket(new RelationChanged(player, player.getRelation(knownPlayer), player.isAutoAttackable(knownPlayer)));
							if (player.getPet() != null)
								knownPlayer.sendPacket(new RelationChanged(player.getPet(), player.getRelation(knownPlayer), player.isAutoAttackable(knownPlayer)));
						}
						catch (NullPointerException e)
						{
						}
					}
				}
		twQuest.setOnEnterWorld(_isTWInProgress);
		return true;
	}
	
	private class RewardOnlineParticipants implements Runnable
	{
		public RewardOnlineParticipants()
		{
		}

		public void run()
		{
			if (isTWInProgress())
			{
				for(L2PcInstance player : L2World.getInstance().getAllPlayers().values())
					if (player.getSiegeSide() > 0)
						giveTWPoint(player, 1000, 6);
			}
			else
				_scheduledRewardOnlineTask.cancel(false);
		}
	}

	private class ScheduleStartTWTask implements Runnable
	{
		public ScheduleStartTWTask()
		{
		}

		public void run()
		{
			_scheduledStartTWTask.cancel(false);
			try
			{
				long timeRemaining = _startTWDate.getTimeInMillis() - Calendar.getInstance().getTimeInMillis();
				if (timeRemaining > 7200000)
				{
					_isRegistrationOver = false;
					_scheduledStartTWTask = ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleStartTWTask(), timeRemaining - 7200000); // Prepare task for 2h before TW start to end registration
				}
				else if ((timeRemaining <= 7200000) && (timeRemaining > 1200000))
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.THE_TERRITORY_WAR_REGISTERING_PERIOD_ENDED);
					Announcements.getInstance().announceToAll(sm);
					_isRegistrationOver = true;
					_scheduledStartTWTask = ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleStartTWTask(), timeRemaining - 1200000); // Prepare task for 20 mins left before TW start.
				}
				else if ((timeRemaining <= 1200000) && (timeRemaining > 600000))
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.TERRITORY_WAR_BEGINS_IN_20_MINUTES);
					Announcements.getInstance().announceToAll(sm);
					_isTWChannelOpen = true;
					_isRegistrationOver = true;
					updatePlayerTWStateFlags(false);
					_scheduledStartTWTask = ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleStartTWTask(), timeRemaining - 600000); // Prepare task for 10 mins left before TW start.
				}
				else if ((timeRemaining <= 600000) && (timeRemaining > 300000))
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.TERRITORY_WAR_BEGINS_IN_10_MINUTES);
					Announcements.getInstance().announceToAll(sm);
					_isTWChannelOpen = true;
					_isRegistrationOver = true;
					updatePlayerTWStateFlags(false);
					_scheduledStartTWTask = ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleStartTWTask(), timeRemaining - 300000); // Prepare task for 5 mins left before TW start.
				}
				else if ((timeRemaining <= 300000) && (timeRemaining > 60000))
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.TERRITORY_WAR_BEGINS_IN_5_MINUTES);
					Announcements.getInstance().announceToAll(sm);
					_isTWChannelOpen = true;
					_isRegistrationOver = true;
					updatePlayerTWStateFlags(false);
					_scheduledStartTWTask = ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleStartTWTask(), timeRemaining - 60000); // Prepare task for 1 min left before TW start.
				}
				else if ((timeRemaining <= 60000) && (timeRemaining > 0))
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.TERRITORY_WAR_BEGINS_IN_1_MINUTE);
					Announcements.getInstance().announceToAll(sm);
					_isTWChannelOpen = true;
					_isRegistrationOver = true;
					updatePlayerTWStateFlags(false);
					_scheduledStartTWTask = ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleStartTWTask(), timeRemaining); // Prepare task for TW start.
				}
				else
				{
					_isTWChannelOpen = true;
					_isRegistrationOver = true;
					startTerritoryWar();
					_scheduledEndTWTask = ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleEndTWTask(), 1000); // Prepare task for TW end.
					_scheduledRewardOnlineTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new RewardOnlineParticipants(), 60000, 60000);
				}
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "", e);
			}
		}
	}
	
	private class ScheduleEndTWTask implements Runnable
	{
		public ScheduleEndTWTask()
		{
		}

		public void run()
		{
			try
			{
				_scheduledEndTWTask.cancel(false);
				long timeRemaining = _startTWDate.getTimeInMillis() + WARLENGTH - Calendar.getInstance().getTimeInMillis();
				if (timeRemaining > 3600000)
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.THE_TERRITORY_WAR_WILL_END_IN_S1_HOURS);
					sm.addNumber(2);
					announceToParticipants(sm, 0, 0);
					_scheduledEndTWTask = ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleEndTWTask(), timeRemaining - 3600000); // Prepare task for 1 hr left.
				}
				else if ((timeRemaining <= 3600000) && (timeRemaining > 600000))
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.THE_TERRITORY_WAR_WILL_END_IN_S1_MINUTES);
					sm.addNumber(Math.round(timeRemaining / 60000));
					announceToParticipants(sm, 0, 0);
					_scheduledEndTWTask = ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleEndTWTask(), timeRemaining - 600000); // Prepare task for 10 minute left.
				}
				else if ((timeRemaining <= 600000) && (timeRemaining > 300000))
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.THE_TERRITORY_WAR_WILL_END_IN_S1_MINUTES);
					sm.addNumber(Math.round(timeRemaining / 60000));
					announceToParticipants(sm, 0, 0);
					_scheduledEndTWTask = ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleEndTWTask(), timeRemaining - 300000); // Prepare task for 5 minute left.
				}
				else if ((timeRemaining <= 300000) && (timeRemaining > 10000))
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.THE_TERRITORY_WAR_WILL_END_IN_S1_MINUTES);
					sm.addNumber(Math.round(timeRemaining / 60000));
					announceToParticipants(sm, 0, 0);
					_scheduledEndTWTask = ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleEndTWTask(), timeRemaining - 10000); // Prepare task for 10 seconds count down
				}
				else if ((timeRemaining <= 10000) && (timeRemaining > 0))
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.S1_SECONDS_TO_THE_END_OF_TERRITORY_WAR);
					sm.addNumber(Math.round(timeRemaining / 1000));
					announceToParticipants(sm, 0, 0);
					_scheduledEndTWTask = ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleEndTWTask(), timeRemaining); // Prepare task for second count down
				}
				else
				{
					endTerritoryWar();
					// _scheduledStartTWTask = ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleStartTWTask(), 1000);
					ThreadPoolManager.getInstance().scheduleGeneral(new closeTerritoryChannelTask(), 600000);
				}
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "", e);
			}
		}
	}
	
	private class closeTerritoryChannelTask implements Runnable
	{
		public closeTerritoryChannelTask()
		{
		}

		public void run()
		{
			_isTWChannelOpen = false;
			_disguisedPlayers.clear();
			updatePlayerTWStateFlags(true);
		}
	}
	
	public void announceToParticipants(L2GameServerPacket sm, int exp, int sp)
	{
		// broadcast to clan members
		for(Territory ter : _territoryList.values())
			if (ter.getOwnerClan() != null)
				for (L2PcInstance member : ter.getOwnerClan().getOnlineMembers(0))
				{
					member.sendPacket(sm);
					if (exp > 0 || sp > 0)
						member.addExpAndSp(exp, sp);
				}
		for(FastList<L2Clan> list:_registeredClans.values())
			for(L2Clan c:list)
				for (L2PcInstance member : c.getOnlineMembers(0))
				{
					member.sendPacket(sm);
					if (exp > 0 || sp > 0)
						member.addExpAndSp(exp, sp);
				}
		// broadcast to mercenaries
		for(FastList<Integer> list:_registeredMercenaries.values())
			for(int objId:list)
			{
				L2PcInstance player = L2World.getInstance().getPlayer(objId);
				if (player != null && (player.getClan() == null || !checkIsRegistered(-1, player.getClan())))
				{
					player.sendPacket(sm);
					if (exp > 0 || sp > 0)
						player.addExpAndSp(exp, sp);
				}
			}
	}
	// =========================================================
	// Property - Public
	public class TerritoryNPCSpawn
	{
		private Location _location;
		private int _npcId;
		private int _castleId;
		private int _type;
		private L2Npc _npc;
		
		public TerritoryNPCSpawn(int castle_id, Location loc, int npc_id, int type, L2Npc npc)
		{
			_castleId = castle_id;
			_location = loc;
			_npcId = npc_id;
			_type = type;
			_npc = npc;
		}
		
		public int getCastleId()
		{
			return _castleId;
		}
		
		public int getNpcId()
		{
			return _npcId;
		}
		
		public int getType()
		{
			return _type;
		}
		
		public void setNPC(L2Npc npc)
		{
			_npc = npc;
		}
		
		public L2Npc getNpc()
		{
			return _npc;
		}
		
		public Location getLocation()
		{
			return _location;
		}
	}
	
	public class Territory
	{
		private final int _territoryId;
		private final int _castleId; // territory Castle
		private int _fortId; // territory Fortress
		private L2Clan _ownerClan;
		private FastList<TerritoryNPCSpawn> _spawnList;
		private TerritoryNPCSpawn[] _territoryWardSpawnPlaces;
		private boolean _isInProgress = false;
		private L2SiegeFlagInstance _territoryHQ = null;
		private int[] _questDone;
		
		public Territory(int castleId)
		{
			_castleId = castleId;
			_territoryId = castleId + 80;
			_spawnList = new FastList<TerritoryNPCSpawn>();
			_territoryWardSpawnPlaces = new TerritoryNPCSpawn[9];
			_questDone = new int[2];
		}
		
		private void addWardSpawnPlace(Location loc)
		{
			for(int i = 0; i < _territoryWardSpawnPlaces.length; i++)
				if (_territoryWardSpawnPlaces[i] == null)
				{
					_territoryWardSpawnPlaces[i] = new TerritoryNPCSpawn(_castleId, loc, 0, 4, null);
					return;
				}
		}
		
		private TerritoryNPCSpawn getFreeWardSpawnPlace()
		{
			for(int i = 0; i < _territoryWardSpawnPlaces.length; i++)
				if (_territoryWardSpawnPlaces[i] != null && _territoryWardSpawnPlaces[i].getNpc() == null)
					return _territoryWardSpawnPlaces[i];
			_log.log(Level.WARNING, "TerritoryWarManager: no free Ward spawn found for territory: " + _territoryId);
			for(int i = 0; i < _territoryWardSpawnPlaces.length; i++)
				if (_territoryWardSpawnPlaces[i] == null)
					_log.log(Level.WARNING, "TerritoryWarManager: territory ward spawn place " + i + " is null!");
				else if (_territoryWardSpawnPlaces[i].getNpc() != null)
					_log.log(Level.WARNING, "TerritoryWarManager: territory ward spawn place " + i + " has npc name: " + _territoryWardSpawnPlaces[i].getNpc().getName());
				else
					_log.log(Level.WARNING, "TerritoryWarManager: territory ward spawn place " + i + " is empty!");
			return null;
		}
		
		public FastList<TerritoryNPCSpawn> getSpawnList()
		{
			return _spawnList;
		}
		
		private void changeNPCsSpawn(int type, boolean isSpawn)
		{
			if (type < 0 || type > 3)
			{
				_log.log(Level.WARNING, "TerritoryWarManager: wrong type(" + type + ") for NPCs spawn change!");
				return;
			}
			for(TerritoryNPCSpawn twSpawn : _spawnList)
			{
				if (twSpawn.getType() != type)
					continue;
				if (isSpawn)
					twSpawn.setNPC(spawnNPC(twSpawn.getNpcId(), twSpawn.getLocation()));
				else
				{
					L2Npc npc = twSpawn.getNpc();
					if (npc != null && !npc.isDead())
						npc.deleteMe();
					twSpawn.setNPC(null);
				}
			}
		}
		
		private void removeWard(int wardId)
		{
			for(TerritoryNPCSpawn wardSpawn : _territoryWardSpawnPlaces)
				if (wardSpawn.getNpcId() == wardId)
				{
					wardSpawn.getNpc().deleteMe();
					wardSpawn.setNPC(null);
					wardSpawn._npcId = 0;
					return;
				}
			_log.log(Level.WARNING, "TerritoryWarManager: cant delete wardId: " + wardId + " for territory: " + _territoryId);
		}
		
		public int getTerritoryId()
		{
			return _territoryId;
		}
		
		public int getCastleId()
		{
			return _castleId;
		}
		
		public int getFortId()
		{
			return _fortId;
		}
		
		public L2Clan getOwnerClan()
		{
			return _ownerClan;
		}
		
		public void setOwnerClan(L2Clan newOwner)
		{
			_ownerClan = newOwner;
		}
		
		public void setHQ(L2SiegeFlagInstance hq)
		{
			_territoryHQ = hq;
		}
		
		public L2SiegeFlagInstance getHQ()
		{
			return _territoryHQ;
		}
		
		public TerritoryNPCSpawn[] getOwnedWard()
		{
			return _territoryWardSpawnPlaces;
		}
		
		public int[] getQuestDone()
		{
			return _questDone;
		}
		
		public FastList<Integer> getOwnedWardIds()
		{
			FastList<Integer> ret = new FastList<Integer>();
			for(TerritoryNPCSpawn wardSpawn : _territoryWardSpawnPlaces)
				if (wardSpawn.getNpcId() > 0)
					ret.add(wardSpawn.getNpcId());
			return ret;
		}
		
		public boolean getIsInProgress()
		{
			return _isInProgress;
		}
		
		public void setIsInProgress(boolean val)
		{
			_isInProgress = val;
		}
	}
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final TerritoryWarManager _instance = new TerritoryWarManager();
	}
}
