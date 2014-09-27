/*
 * Copyright (C) 2004-2014 L2J Server
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
package com.l2jserver.gameserver.model.conditions;

import com.l2jserver.gameserver.enums.Race;
import com.l2jserver.gameserver.model.stats.Env;

/**
 * The Class ConditionTargetRace.
 * @author Zealar
 */
public class ConditionTargetRace extends Condition
{
	private final Race _race;
	
	/**
	 * Instantiates a new condition target race.
	 * @param race containing the allowed race.
	 */
	public ConditionTargetRace(Race race)
	{
		_race = race;
	}
	
	@Override
	public boolean testImpl(Env env)
	{
		return _race == env.getTarget().getRace();
	}
}
