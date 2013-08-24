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
package com.l2jserver.gameserver.datatables;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.l2jserver.Config;
import com.l2jserver.L2DatabaseFactory;
import com.l2jserver.gameserver.idfactory.IdFactory;
import com.l2jserver.gameserver.model.L2PetData;
import com.l2jserver.gameserver.model.actor.instance.L2MerchantSummonInstance;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.actor.instance.L2PetInstance;
import com.l2jserver.gameserver.model.actor.instance.L2ServitorInstance;
import com.l2jserver.gameserver.model.actor.instance.L2SiegeSummonInstance;
import com.l2jserver.gameserver.model.actor.templates.L2NpcTemplate;
import com.l2jserver.gameserver.model.items.instance.L2ItemInstance;
import com.l2jserver.gameserver.model.skills.l2skills.L2SkillSummon;
import com.l2jserver.gameserver.network.serverpackets.PetItemList;

/**
 * @author Nyaran
 */
public class CharSummonTable
{
	private static final Logger _log = Logger.getLogger(CharSummonTable.class.getName());
	private static final Map<Integer, Integer> _pets = new ConcurrentHashMap<>();
	private static final Map<Integer, Integer> _servitors = new ConcurrentHashMap<>();
	
	// SQL
	private static final String INIT_PET = "SELECT ownerId, item_obj_id FROM pets WHERE restore = 'true'";
	private static final String INIT_SUMMONS = "SELECT ownerId, summonSkillId FROM character_summons";
	private static final String LOAD_SUMMON = "SELECT curHp, curMp, time FROM character_summons WHERE ownerId = ? AND summonSkillId = ?";
	private static final String REMOVE_SUMMON = "DELETE FROM character_summons WHERE ownerId = ?";
	private static final String SAVE_SUMMON = "REPLACE INTO character_summons (ownerId,summonSkillId,curHp,curMp,time) VALUES (?,?,?,?,?)";
	
	public Map<Integer, Integer> getPets()
	{
		return _pets;
	}
	
	public Map<Integer, Integer> getServitors()
	{
		return _servitors;
	}
	
	public void init()
	{
		if (Config.RESTORE_SERVITOR_ON_RECONNECT)
		{
			try (Connection con = L2DatabaseFactory.getInstance().getConnection();
				Statement s = con.createStatement();
				ResultSet rs = s.executeQuery(INIT_SUMMONS))
			{
				while (rs.next())
				{
					_servitors.put(rs.getInt("ownerId"), rs.getInt("summonSkillId"));
				}
			}
			catch (Exception e)
			{
				_log.warning(getClass().getSimpleName() + ": Error while loading saved servitor: " + e);
			}
		}
		
		if (Config.RESTORE_PET_ON_RECONNECT)
		{
			try (Connection con = L2DatabaseFactory.getInstance().getConnection();
				Statement s = con.createStatement();
				ResultSet rs = s.executeQuery(INIT_PET))
			{
				while (rs.next())
				{
					_pets.put(rs.getInt("ownerId"), rs.getInt("item_obj_id"));
				}
			}
			catch (Exception e)
			{
				_log.warning(getClass().getSimpleName() + ": Error while loading saved pet: " + e);
			}
		}
	}
	
	public void removeServitor(L2PcInstance activeChar)
	{
		_servitors.remove(activeChar.getObjectId());
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(REMOVE_SUMMON))
		{
			ps.setInt(1, activeChar.getObjectId());
			ps.execute();
		}
		catch (SQLException e)
		{
			_log.warning(getClass().getSimpleName() + ": Summon cannot be removed: " + e);
		}
	}
	
	public void restorePet(L2PcInstance activeChar)
	{
		final L2ItemInstance item = activeChar.getInventory().getItemByObjectId(_pets.get(activeChar.getObjectId()));
		if (item == null)
		{
			_log.warning(getClass().getSimpleName() + ": Null pet summoning item for: " + activeChar);
			return;
		}
		final L2PetData petData = PetDataTable.getInstance().getPetDataByItemId(item.getId());
		if (petData == null)
		{
			_log.warning(getClass().getSimpleName() + ": Null pet data for: " + activeChar + " and summoning item: " + item);
			return;
		}
		final L2NpcTemplate npcTemplate = NpcTable.getInstance().getTemplate(petData.getNpcId());
		if (npcTemplate == null)
		{
			_log.warning(getClass().getSimpleName() + ": Null pet NPC template for: " + activeChar + " and pet Id:" + petData.getNpcId());
			return;
		}
		
		final L2PetInstance pet = L2PetInstance.spawnPet(npcTemplate, activeChar, item);
		if (pet == null)
		{
			_log.warning(getClass().getSimpleName() + ": Null pet instance for: " + activeChar + " and pet NPC template:" + npcTemplate);
			return;
		}
		
		pet.setShowSummonAnimation(true);
		pet.setTitle(activeChar.getName());
		
		if (!pet.isRespawned())
		{
			pet.setCurrentHp(pet.getMaxHp());
			pet.setCurrentMp(pet.getMaxMp());
			pet.getStat().setExp(pet.getExpForThisLevel());
			pet.setCurrentFed(pet.getMaxFed());
		}
		
		pet.setRunning();
		
		if (!pet.isRespawned())
		{
			pet.storeMe();
		}
		
		activeChar.setPet(pet);
		
		pet.spawnMe(activeChar.getX() + 50, activeChar.getY() + 100, activeChar.getZ());
		pet.startFeed();
		item.setEnchantLevel(pet.getLevel());
		
		if (pet.getCurrentFed() <= 0)
		{
			pet.unSummon(activeChar);
		}
		else
		{
			pet.startFeed();
		}
		
		pet.setFollowStatus(true);
		
		pet.getOwner().sendPacket(new PetItemList(pet.getInventory().getItems()));
		pet.broadcastStatusUpdate();
	}
	
	public void restoreServitor(L2PcInstance activeChar)
	{
		int skillId = _servitors.get(activeChar.getObjectId());
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(LOAD_SUMMON))
		{
			ps.setInt(1, activeChar.getObjectId());
			ps.setInt(2, skillId);
			try (ResultSet rs = ps.executeQuery())
			{
				
				L2NpcTemplate summonTemplate;
				L2ServitorInstance summon;
				L2SkillSummon skill;
				
				while (rs.next())
				{
					int curHp = rs.getInt("curHp");
					int curMp = rs.getInt("curMp");
					int time = rs.getInt("time");
					
					skill = (L2SkillSummon) SkillTable.getInstance().getInfo(skillId, activeChar.getSkillLevel(skillId));
					if (skill == null)
					{
						removeServitor(activeChar);
						return;
					}
					
					summonTemplate = NpcTable.getInstance().getTemplate(skill.getNpcId());
					if (summonTemplate == null)
					{
						_log.warning(getClass().getSimpleName() + ": Summon attemp for nonexisting Skill ID:" + skillId);
						return;
					}
					
					final int id = IdFactory.getInstance().getNextId();
					if (summonTemplate.isType("L2SiegeSummon"))
					{
						summon = new L2SiegeSummonInstance(id, summonTemplate, activeChar, skill);
					}
					else if (summonTemplate.isType("L2MerchantSummon"))
					{
						// TODO: Confirm L2Merchant summon = new L2MerchantSummonInstance(id, summonTemplate, activeChar, skill);
						summon = new L2ServitorInstance(id, summonTemplate, activeChar, skill);
					}
					else
					{
						summon = new L2ServitorInstance(id, summonTemplate, activeChar, skill);
					}
					
					summon.setName(summonTemplate.getName());
					summon.setTitle(activeChar.getName());
					summon.setExpPenalty(skill.getExpPenalty());
					summon.setSharedElementals(skill.getInheritElementals());
					summon.setSharedElementalsValue(skill.getElementalSharePercent());
					
					if (summon.getLevel() >= ExperienceTable.getInstance().getMaxPetLevel())
					{
						summon.getStat().setExp(ExperienceTable.getInstance().getExpForLevel(ExperienceTable.getInstance().getMaxPetLevel() - 1));
						_log.warning(getClass().getSimpleName() + ": Summon (" + summon.getName() + ") NpcID: " + summon.getId() + " has a level above " + ExperienceTable.getInstance().getMaxPetLevel() + ". Please rectify.");
					}
					else
					{
						summon.getStat().setExp(ExperienceTable.getInstance().getExpForLevel(summon.getLevel() % ExperienceTable.getInstance().getMaxPetLevel()));
					}
					summon.setCurrentHp(curHp);
					summon.setCurrentMp(curMp);
					summon.setHeading(activeChar.getHeading());
					summon.setRunning();
					if (!(summon instanceof L2MerchantSummonInstance))
					{
						activeChar.setPet(summon);
					}
					
					summon.setTimeRemaining(time);
					
					summon.spawnMe(activeChar.getX() + 20, activeChar.getY() + 20, activeChar.getZ());
				}
			}
		}
		catch (SQLException e)
		{
			_log.warning(getClass().getSimpleName() + ": Servitor cannot be restored: " + e);
		}
	}
	
	public void saveSummon(L2ServitorInstance summon)
	{
		if ((summon == null) || (summon.getTimeRemaining() <= 0))
		{
			return;
		}
		
		_servitors.put(summon.getOwner().getObjectId(), summon.getReferenceSkill());
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(SAVE_SUMMON))
		{
			ps.setInt(1, summon.getOwner().getObjectId());
			ps.setInt(2, summon.getReferenceSkill());
			ps.setInt(3, (int) Math.round(summon.getCurrentHp()));
			ps.setInt(4, (int) Math.round(summon.getCurrentMp()));
			ps.setInt(5, summon.getTimeRemaining());
			ps.execute();
		}
		catch (Exception e)
		{
			_log.warning(getClass().getSimpleName() + ": Failed to store summon: " + summon + " from " + summon.getOwner() + ", error: " + e);
		}
		
	}
	
	public static CharSummonTable getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final CharSummonTable _instance = new CharSummonTable();
	}
}
