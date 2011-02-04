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
package com.l2jserver.gameserver.skills.conditions;

import com.l2jserver.gameserver.skills.Env;

/**
 * The Class ConditionMinDistance.
 *
 * @author  Didldak
 */
public class ConditionMinDistance extends Condition
{
	private final int _sqDistance;
	
	/**
	 * Instantiates a new condition min distance.
	 *
	 * @param sqDistance the sq distance
	 */
	public ConditionMinDistance(int sqDistance)
	{
		_sqDistance = sqDistance;
	}
	
	/* (non-Javadoc)
	 * @see com.l2jserver.gameserver.skills.conditions.Condition#testImpl(com.l2jserver.gameserver.skills.Env)
	 */
	@Override
	public boolean testImpl(Env env)
	{
		if (env.target == null)
			return false;
		return env.player.getDistanceSq(env.target) >= _sqDistance;
	}
}
