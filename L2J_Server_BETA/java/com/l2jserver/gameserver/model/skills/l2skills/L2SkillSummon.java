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

import com.l2jserver.gameserver.datatables.ExperienceTable;
import com.l2jserver.gameserver.datatables.NpcTable;
import com.l2jserver.gameserver.idfactory.IdFactory;
import com.l2jserver.gameserver.model.L2Object;
import com.l2jserver.gameserver.model.StatsSet;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.actor.instance.L2ServitorInstance;
import com.l2jserver.gameserver.model.actor.instance.L2SiegeSummonInstance;
import com.l2jserver.gameserver.model.actor.templates.L2NpcTemplate;
import com.l2jserver.gameserver.model.skills.L2Skill;

public class L2SkillSummon extends L2Skill
{
	private final float _expPenalty;
	// What is the total lifetime of summons (in milliseconds)
	private final int _summonTotalLifeTime;
	// How much lifetime is lost per second of idleness (non-fighting)
	private final int _summonTimeLostIdle;
	// How much time is lost per second of activity (fighting)
	private final int _summonTimeLostActive;
	
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
		
		_summonTotalLifeTime = set.getInt("summonTotalLifeTime", 1200000); // 20 minutes default
		_summonTimeLostIdle = set.getInt("summonTimeLostIdle", 0);
		_summonTimeLostActive = set.getInt("summonTimeLostActive", 0);
		
		_itemConsumeOT = set.getInt("itemConsumeCountOT", 0);
		_itemConsumeIdOT = set.getInt("itemConsumeIdOT", 0);
		_itemConsumeSteps = set.getInt("itemConsumeSteps", 0);
		
		_inheritElementals = set.getBoolean("inheritElementals", false);
		_elementalSharePercent = set.getDouble("inheritPercent", 1);
	}
	
	@Override
	public void useSkill(L2Character caster, L2Object[] targets)
	{
		if ((caster == null) || caster.isAlikeDead() || !caster.isPlayer())
		{
			return;
		}
		
		final L2PcInstance activeChar = caster.getActingPlayer();
		if (getNpcId() <= 0)
		{
			activeChar.sendMessage("Summon skill " + getId() + " not implemented yet.");
			return;
		}
		
		final L2NpcTemplate summonTemplate = NpcTable.getInstance().getTemplate(getNpcId());
		if (summonTemplate == null)
		{
			_log.warning("Summon attempt for nonexisting NPC ID:" + getNpcId() + ", skill ID:" + getId());
			return; // npcID doesn't exist
		}
		
		final int id = IdFactory.getInstance().getNextId();
		L2ServitorInstance summon;
		if (summonTemplate.isType("L2SiegeSummon"))
		{
			summon = new L2SiegeSummonInstance(id, summonTemplate, activeChar, this);
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
			_log.warning("Summon (" + summon.getName() + ") NpcID: " + summon.getId() + " has a level above " + ExperienceTable.getInstance().getMaxPetLevel() + ". Please rectify.");
		}
		else
		{
			summon.getStat().setExp(ExperienceTable.getInstance().getExpForLevel(summon.getLevel() % ExperienceTable.getInstance().getMaxPetLevel()));
		}
		
		summon.setCurrentHp(summon.getMaxHp());
		summon.setCurrentMp(summon.getMaxMp());
		summon.setHeading(activeChar.getHeading());
		summon.setRunning();
		activeChar.setPet(summon);
		summon.spawnMe(activeChar.getX() + 20, activeChar.getY() + 20, activeChar.getZ());
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
