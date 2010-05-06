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

import com.l2jserver.gameserver.GameTimeController;
import com.l2jserver.gameserver.skills.Env;

/**
 * The Class ConditionGameTime.
 *
 * @author mkizub
 */
public class ConditionGameTime extends Condition
{
	
	/**
	 * The Enum CheckGameTime.
	 */
	public enum CheckGameTime
	{
		NIGHT
	}
	
	private final CheckGameTime _check;
	private final boolean _required;
	
	/**
	 * Instantiates a new condition game time.
	 *
	 * @param check the check
	 * @param required the required
	 */
	public ConditionGameTime(CheckGameTime check, boolean required)
	{
		_check = check;
		_required = required;
	}
	
	/**
	 * Test impl.
	 *
	 * @param env the env
	 * @return true, if successful
	 * @see com.l2jserver.gameserver.skills.conditions.Condition#testImpl(com.l2jserver.gameserver.skills.Env)
	 */
	@Override
	public boolean testImpl(Env env)
	{
		switch (_check)
		{
			case NIGHT:
				return GameTimeController.getInstance().isNowNight() == _required;
		}
		return !_required;
	}
}
