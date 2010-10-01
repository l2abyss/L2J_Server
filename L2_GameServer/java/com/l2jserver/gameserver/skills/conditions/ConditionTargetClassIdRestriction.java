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

import java.util.ArrayList;

import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.skills.Env;

/**
 * The Class ConditionTargetClassIdRestriction.
 */
public class ConditionTargetClassIdRestriction extends Condition
{
	private final ArrayList<Integer> _classIds;
	
	/**
	 * Instantiates a new condition target class id restriction.
	 *
	 * @param classId the class id
	 */
	public ConditionTargetClassIdRestriction(ArrayList<Integer> classId)
	{
		_classIds = classId;
	}
	
	/* (non-Javadoc)
	 * @see com.l2jserver.gameserver.skills.conditions.Condition#testImpl(com.l2jserver.gameserver.skills.Env)
	 */
	@Override
	public boolean testImpl(Env env)
	{
		if (!(env.target instanceof L2PcInstance))
			return false;
		return (_classIds.contains(((L2PcInstance)env.target).getClassId().getId()));
	}
}
