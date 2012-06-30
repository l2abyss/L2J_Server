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

import com.l2jserver.gameserver.model.skills.funcs.Func;
import com.l2jserver.gameserver.model.stats.BaseStats;
import com.l2jserver.gameserver.model.stats.Env;
import com.l2jserver.gameserver.model.stats.Stats;

/**
 * @author UnAfraid
 */
public class FuncMaxMpMul extends Func
{
	private static final FuncMaxMpMul _fmmm_instance = new FuncMaxMpMul();
	
	public static Func getInstance()
	{
		return _fmmm_instance;
	}
	
	private FuncMaxMpMul()
	{
		super(Stats.MAX_MP, 0x20, null);
	}
	
	@Override
	public void calc(Env env)
	{
		env.mulValue(BaseStats.MEN.calcBonus(env.getCharacter()));
	}
}