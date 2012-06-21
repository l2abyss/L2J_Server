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

import com.l2jserver.Config;
import com.l2jserver.gameserver.SevenSigns;
import com.l2jserver.gameserver.model.skills.funcs.Func;
import com.l2jserver.gameserver.model.stats.Env;
import com.l2jserver.gameserver.model.stats.Stats;

/**
 * @author UnAfraid
 */
public class FuncGatesMDefMod extends Func
{
	private static final FuncGatesMDefMod _fmm_instance = new FuncGatesMDefMod();
	
	public static Func getInstance()
	{
		return _fmm_instance;
	}
	
	private FuncGatesMDefMod()
	{
		super(Stats.MAGIC_DEFENCE, 0x20, null);
	}
	
	@Override
	public void calc(Env env)
	{
		if (SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE) == SevenSigns.CABAL_DAWN)
		{
			env.mulValue(Config.ALT_SIEGE_DAWN_GATES_MDEF_MULT);
		}
		else if (SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE) == SevenSigns.CABAL_DUSK)
		{
			env.mulValue(Config.ALT_SIEGE_DUSK_GATES_MDEF_MULT);
		}
	}
}