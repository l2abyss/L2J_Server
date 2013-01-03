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

import com.l2jserver.gameserver.model.L2Object;
import com.l2jserver.gameserver.model.StatsSet;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.skills.L2Skill;

/**
 * @author JIV
 */
public class L2SkillSweeper extends L2Skill
{
	private final boolean _absorbHp;
	private final int _absorbAbs;
	
	/**
	 * @param set
	 */
	public L2SkillSweeper(StatsSet set)
	{
		super(set);
		_absorbHp = set.getBool("absorbHp", true);
		_absorbAbs = set.getInteger("absorbAbs", -1);
	}
	
	@Override
	public void useSkill(L2Character caster, L2Object[] targets)
	{
		// not used
	}
	
	public boolean isAbsorbHp()
	{
		return _absorbHp;
	}
	
	public int getAbsorbAbs()
	{
		return _absorbAbs;
	}
	
}
