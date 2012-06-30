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

import com.l2jserver.gameserver.model.actor.templates.L2PcTemplate;
import com.l2jserver.gameserver.model.skills.funcs.Func;
import com.l2jserver.gameserver.model.stats.Env;
import com.l2jserver.gameserver.model.stats.Stats;

/**
 * @author UnAfraid
 */
public class FuncMaxHpAdd extends Func
{
	private static final FuncMaxHpAdd _fmha_instance = new FuncMaxHpAdd();
	
	public static Func getInstance()
	{
		return _fmha_instance;
	}
	
	private FuncMaxHpAdd()
	{
		super(Stats.MAX_HP, 0x10, null);
	}
	
	@Override
	public void calc(Env env)
	{
		L2PcTemplate t = (L2PcTemplate) env.getCharacter().getTemplate();
		int lvl = env.getCharacter().getLevel() - t.getClassBaseLevel();
		double hpmod = t.getLvlHpMod() * lvl;
		double hpmax = (t.getLvlHpAdd() + hpmod) * lvl;
		double hpmin = (t.getLvlHpAdd() * lvl) + hpmod;
		env.addValue((hpmax + hpmin) / 2);
	}
}