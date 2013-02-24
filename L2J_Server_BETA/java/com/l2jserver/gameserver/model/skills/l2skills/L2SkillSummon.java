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
package com.l2jserver.gameserver.model.skills.l2skills;

import com.l2jserver.Config;
import com.l2jserver.gameserver.datatables.ExperienceTable;
import com.l2jserver.gameserver.datatables.NpcTable;
import com.l2jserver.gameserver.idfactory.IdFactory;
import com.l2jserver.gameserver.model.L2Object;
import com.l2jserver.gameserver.model.StatsSet;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.instance.L2CubicInstance;
import com.l2jserver.gameserver.model.actor.instance.L2MerchantSummonInstance;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.actor.instance.L2ServitorInstance;
import com.l2jserver.gameserver.model.actor.instance.L2SiegeSummonInstance;
import com.l2jserver.gameserver.model.actor.templates.L2NpcTemplate;
import com.l2jserver.gameserver.model.skills.L2Skill;
import com.l2jserver.gameserver.model.skills.targets.L2TargetType;
import com.l2jserver.gameserver.network.SystemMessageId;

public class L2SkillSummon extends L2Skill
{
	public static final int SKILL_CUBIC_MASTERY = 143;
	
	private final float _expPenalty;
	private final boolean _isCubic;
	
	// cubic AI
	// Power for a cubic
	private final int _cubicPower;
	// Duration for a cubic
	private final int _cubicDuration;
	// Activation time for a cubic
	private final int _cubicDelay;
	// Maximum casts made by the cubic until it goes idle
	private final int _cubicMaxCount;
	// Activation chance for a cubic
	private final int _cubicSkillChance;
	
	// What is the total lifetime of summons (in millisecs)
	private final int _summonTotalLifeTime;
	// How much lifetime is lost per second of idleness (non-fighting)
	private final int _summonTimeLostIdle;
	// How much time is lost per second of activity (fighting)
	private final int _summonTimeLostActive;
	
	// item consume time in milliseconds
	private final int _itemConsumeTime;
	// item consume count over time
	private final int _itemConsumeOT;
	// item consume id over time
	private final int _itemConsumeIdOT;
	// how many times to consume an item
	private final int _itemConsumeSteps;
	// Inherit elementals from master
	private final boolean _inheritElementals;
	private final double _elementalSharePercent;
	
	public L2SkillSummon(StatsSet set)
	{
		super(set);
		
		_expPenalty = set.getFloat("expPenalty", 0.f);
		_isCubic = set.getBool("isCubic", false);
		
		_cubicPower = set.getInteger("cubicPower", 0);
		_cubicDuration = set.getInteger("cubicDuration", 0);
		_cubicDelay = set.getInteger("cubicDelay", 0);
		_cubicMaxCount = set.getInteger("cubicMaxCount", -1);
		_cubicSkillChance = set.getInteger("cubicSkillChance", 0);
		
		_summonTotalLifeTime = set.getInteger("summonTotalLifeTime", 1200000); // 20 minutes default
		_summonTimeLostIdle = set.getInteger("summonTimeLostIdle", 0);
		_summonTimeLostActive = set.getInteger("summonTimeLostActive", 0);
		
		_itemConsumeOT = set.getInteger("itemConsumeCountOT", 0);
		_itemConsumeIdOT = set.getInteger("itemConsumeIdOT", 0);
		_itemConsumeTime = set.getInteger("itemConsumeTime", 0);
		_itemConsumeSteps = set.getInteger("itemConsumeSteps", 0);
		
		_inheritElementals = set.getBool("inheritElementals", false);
		_elementalSharePercent = set.getDouble("inheritPercent", 1);
	}
	
	public boolean checkCondition(L2Character activeChar)
	{
		if (activeChar.isPlayer())
		{
			L2PcInstance player = activeChar.getActingPlayer();
			
			if (isCubic())
			{
				if (getTargetType() != L2TargetType.TARGET_SELF)
				{
					return true; // Player is always able to cast mass cubic skill
				}
				int mastery = player.getSkillLevel(SKILL_CUBIC_MASTERY);
				if (mastery < 0)
				{
					mastery = 0;
				}
				int count = player.getCubics().size();
				if (count > mastery)
				{
					activeChar.sendMessage("You already have " + count + " cubic(s).");
					return false;
				}
			}
			else
			{
				if (player.inObserverMode())
				{
					return false;
				}
				if (player.hasSummon())
				{
					activeChar.sendPacket(SystemMessageId.YOU_ALREADY_HAVE_A_PET);
					return false;
				}
			}
		}
		return super.checkCondition(activeChar, null, false);
	}
	
	@Override
	public void useSkill(L2Character caster, L2Object[] targets)
	{
		if (caster.isAlikeDead() || !caster.isPlayer())
		{
			return;
		}
		
		L2PcInstance activeChar = caster.getActingPlayer();
		
		if (getNpcId() == 0)
		{
			activeChar.sendMessage("Summon skill " + getId() + " not described yet");
			return;
		}
		
		if (_isCubic)
		{
			// Gnacik :
			// If skill is enchanted calculate cubic skill level based on enchant
			// 8 at 101 (+1 Power)
			// 12 at 130 (+30 Power)
			// Because 12 is max 5115-5117 skills
			// TODO: make better method of calculation, dunno how its calculated on offi
			int _cubicSkillLevel = getLevel();
			if (_cubicSkillLevel > 100)
			{
				_cubicSkillLevel = ((getLevel() - 100) / 7) + 8;
			}
			
			if (targets.length > 1) // Mass cubic skill
			{
				for (L2Object obj : targets)
				{
					if (!obj.isPlayer())
					{
						continue;
					}
					L2PcInstance player = obj.getActingPlayer();
					int mastery = player.getSkillLevel(SKILL_CUBIC_MASTERY);
					if (mastery < 0)
					{
						mastery = 0;
					}
					if ((mastery == 0) && !player.getCubics().isEmpty())
					{
						// Player can have only 1 cubic - we should replace old cubic with new one
						for (L2CubicInstance c : player.getCubics().values())
						{
							c.stopAction();
						}
						player.getCubics().clear();
					}
					// TODO: Should remove first cubic summoned and replace with new cubic
					if (player.getCubics().containsKey(getNpcId()))
					{
						L2CubicInstance cubic = player.getCubic(getNpcId());
						cubic.stopAction();
						cubic.cancelDisappear();
						player.delCubic(getNpcId());
					}
					if (player.getCubics().size() > mastery)
					{
						continue;
					}
					if (player == activeChar)
					{
						player.addCubic(getNpcId(), _cubicSkillLevel, _cubicPower, _cubicDelay, _cubicSkillChance, _cubicMaxCount, _cubicDuration, false);
					}
					else
					{
						player.addCubic(getNpcId(), _cubicSkillLevel, _cubicPower, _cubicDelay, _cubicSkillChance, _cubicMaxCount, _cubicDuration, true);
					}
					player.broadcastUserInfo();
				}
			}
			else
			// Normal cubic skill
			{
				int mastery = activeChar.getSkillLevel(SKILL_CUBIC_MASTERY);
				if (mastery < 0)
				{
					mastery = 0;
				}
				if (activeChar.getCubics().containsKey(getNpcId()))
				{
					L2CubicInstance cubic = activeChar.getCubic(getNpcId());
					cubic.stopAction();
					cubic.cancelDisappear();
					activeChar.delCubic(getNpcId());
				}
				if (activeChar.getCubics().size() > mastery)
				{
					if (Config.DEBUG)
					{
						_log.fine("player can't summon any more cubics. ignore summon skill");
					}
					activeChar.sendPacket(SystemMessageId.CUBIC_SUMMONING_FAILED);
					return;
				}
				activeChar.addCubic(getNpcId(), _cubicSkillLevel, _cubicPower, _cubicDelay, _cubicSkillChance, _cubicMaxCount, _cubicDuration, false);
				activeChar.broadcastUserInfo();
			}
			return;
		}
		
		if (activeChar.hasSummon() || activeChar.isMounted())
		{
			if (Config.DEBUG)
			{
				_log.fine("player has a pet already. ignore summon skill");
			}
			return;
		}
		
		L2ServitorInstance summon;
		L2NpcTemplate summonTemplate = NpcTable.getInstance().getTemplate(getNpcId());
		if (summonTemplate == null)
		{
			_log.warning("Summon attempt for nonexisting NPC ID:" + getNpcId() + ", skill ID:" + getId());
			return; // npcID doesn't exist
		}
		
		final int id = IdFactory.getInstance().getNextId();
		if (summonTemplate.isType("L2SiegeSummon"))
		{
			summon = new L2SiegeSummonInstance(id, summonTemplate, activeChar, this);
		}
		else if (summonTemplate.isType("L2MerchantSummon"))
		{
			summon = new L2MerchantSummonInstance(id, summonTemplate, activeChar, this);
		}
		else
		{
			summon = new L2ServitorInstance(id, summonTemplate, activeChar, this);
		}
		
		summon.setName(summonTemplate.getName());
		summon.setTitle(activeChar.getName());
		summon.setExpPenalty(_expPenalty);
		summon.setSharedElementals(_inheritElementals);
		summon.setSharedElementalsValue(_elementalSharePercent);
		
		if (summon.getLevel() >= ExperienceTable.getInstance().getMaxPetLevel())
		{
			summon.getStat().setExp(ExperienceTable.getInstance().getExpForLevel(ExperienceTable.getInstance().getMaxPetLevel() - 1));
			_log.warning("Summon (" + summon.getName() + ") NpcID: " + summon.getNpcId() + " has a level above " + ExperienceTable.getInstance().getMaxPetLevel() + ". Please rectify.");
		}
		else
		{
			summon.getStat().setExp(ExperienceTable.getInstance().getExpForLevel(summon.getLevel() % ExperienceTable.getInstance().getMaxPetLevel()));
		}
		
		summon.setCurrentHp(summon.getMaxHp());
		summon.setCurrentMp(summon.getMaxMp());
		summon.setHeading(activeChar.getHeading());
		summon.setRunning();
		if (!(summon instanceof L2MerchantSummonInstance))
		{
			activeChar.setPet(summon);
		}
		
		// L2World.getInstance().storeObject(summon);
		summon.spawnMe(activeChar.getX() + 20, activeChar.getY() + 20, activeChar.getZ());
	}
	
	public final boolean isCubic()
	{
		return _isCubic;
	}
	
	public final int getTotalLifeTime()
	{
		return _summonTotalLifeTime;
	}
	
	public final int getTimeLostIdle()
	{
		return _summonTimeLostIdle;
	}
	
	public final int getTimeLostActive()
	{
		return _summonTimeLostActive;
	}
	
	/**
	 * @return Returns the itemConsume count over time.
	 */
	public final int getItemConsumeOT()
	{
		return _itemConsumeOT;
	}
	
	/**
	 * @return Returns the itemConsumeId over time.
	 */
	public final int getItemConsumeIdOT()
	{
		return _itemConsumeIdOT;
	}
	
	public final int getItemConsumeSteps()
	{
		return _itemConsumeSteps;
	}
	
	/**
	 * @return Returns the itemConsume time in milliseconds.
	 */
	public final int getItemConsumeTime()
	{
		return _itemConsumeTime;
	}
	
	public final float getExpPenalty()
	{
		return _expPenalty;
	}
	
	public final boolean getInheritElementals()
	{
		return _inheritElementals;
	}
	
	public final double getElementalSharePercent()
	{
		return _elementalSharePercent;
	}
}
