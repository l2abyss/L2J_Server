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
package com.l2jserver.gameserver.model.skills.funcs.formulas;

import com.l2jserver.gameserver.model.conditions.ConditionUsingItemType;
import com.l2jserver.gameserver.model.items.type.L2WeaponType;
import com.l2jserver.gameserver.model.skills.funcs.Func;
import com.l2jserver.gameserver.model.stats.Env;
import com.l2jserver.gameserver.model.stats.Stats;

/**
 * @author UnAfraid
 */
public class FuncBowAtkRange extends Func
{
	private static final FuncBowAtkRange _fbar_instance = new FuncBowAtkRange();
	
	public static Func getInstance()
	{
		return _fbar_instance;
	}
	
	private FuncBowAtkRange()
	{
		super(Stats.POWER_ATTACK_RANGE, 0x10, null);
		setCondition(new ConditionUsingItemType(L2WeaponType.BOW.mask()));
	}
	
	@Override
	public void calc(Env env)
	{
		if (!cond.test(env))
		{
			return;
		}
		// default is 40 and with bow should be 500
		env.addValue(460);
	}
}