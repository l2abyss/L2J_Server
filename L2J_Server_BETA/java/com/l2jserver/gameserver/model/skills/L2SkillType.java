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
package com.l2jserver.gameserver.model.skills;

import java.lang.reflect.Constructor;

import com.l2jserver.gameserver.model.StatsSet;
import com.l2jserver.gameserver.model.skills.l2skills.L2SkillDefault;
import com.l2jserver.gameserver.model.skills.l2skills.L2SkillSiegeFlag;
import com.l2jserver.gameserver.model.skills.l2skills.L2SkillSignet;
import com.l2jserver.gameserver.model.skills.l2skills.L2SkillSignetCasttime;
import com.l2jserver.gameserver.model.skills.l2skills.L2SkillSummon;

/**
 * Skill type enumerated.
 * @author nBd
 */
public enum L2SkillType
{
	// Damage
	SIGNET(L2SkillSignet.class),
	SIGNET_CASTTIME(L2SkillSignetCasttime.class),
	// reco
	GIVE_RECO,
	// Aggro
	AGGDAMAGE,
	AGGREDUCE,
	AGGREMOVE,
	AGGREDUCE_CHAR,
	AGGDEBUFF,
	// Fishing
	FISHING,
	PUMPING,
	REELING,
	// Misc
	UNLOCK,
	UNLOCK_SPECIAL,
	ENCHANT_ARMOR,
	ENCHANT_WEAPON,
	ENCHANT_ATTRIBUTE,
	SOULSHOT,
	SPIRITSHOT,
	SIEGEFLAG(L2SkillSiegeFlag.class),
	TAKECASTLE,
	TAKEFORT,
	DELUXE_KEY_UNLOCK,
	SOW,
	GET_PLAYER,
	DETECTION,
	DUMMY,
	// Summons
	SUMMON(L2SkillSummon.class),
	FEED_PET,
	
	BUFF,
	DEBUFF,
	CONT,
	FUSION,
	
	RESURRECT,
	DETECT_TRAP,
	REMOVE_TRAP,
	SHIFT_TARGET,
	
	// Skill is done within the core.
	COREDONE,
	// Nornil's Power (Nornil's Garden instance)
	NORNILS_POWER,
	// unimplemented
	NOTDONE,
	BALLISTA;
	
	private final Class<? extends L2Skill> _class;
	
	public L2Skill makeSkill(StatsSet set)
	{
		try
		{
			Constructor<? extends L2Skill> c = _class.getConstructor(StatsSet.class);
			
			return c.newInstance(set);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	private L2SkillType()
	{
		_class = L2SkillDefault.class;
	}
	
	private L2SkillType(Class<? extends L2Skill> classType)
	{
		_class = classType;
	}
}
