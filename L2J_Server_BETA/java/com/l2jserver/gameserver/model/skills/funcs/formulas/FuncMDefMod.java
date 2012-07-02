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

import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.itemcontainer.Inventory;
import com.l2jserver.gameserver.model.skills.funcs.Func;
import com.l2jserver.gameserver.model.stats.BaseStats;
import com.l2jserver.gameserver.model.stats.Env;
import com.l2jserver.gameserver.model.stats.Stats;

/**
 * @author UnAfraid
 */
public class FuncMDefMod extends Func
{
	private static final FuncMDefMod _fmm_instance = new FuncMDefMod();
	
	public static Func getInstance()
	{
		return _fmm_instance;
	}
	
	private FuncMDefMod()
	{
		super(Stats.MAGIC_DEFENCE, 0x20, null);
	}
	
	@Override
	public void calc(Env env)
	{
		if (env.getCharacter().isPlayer())
		{
			L2PcInstance p = env.getPlayer();
			if (p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LFINGER) != null)
			{
				env.subValue(5);
			}
			if (p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RFINGER) != null)
			{
				env.subValue(5);
			}
			if (p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LEAR) != null)
			{
				env.subValue(9);
			}
			if (p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_REAR) != null)
			{
				env.subValue(9);
			}
			if (p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_NECK) != null)
			{
				env.subValue(13);
			}
			env.mulValue(BaseStats.MEN.calcBonus(env.getPlayer()) * env.getPlayer().getLevelMod());
		}
		else if (env.getCharacter().isPet())
		{
			if (env.getCharacter().getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_NECK) != 0)
			{
				env.subValue(13);
				env.mulValue(BaseStats.MEN.calcBonus(env.getCharacter()) * env.getCharacter().getLevelMod());
			}
			else
			{
				env.mulValue(BaseStats.MEN.calcBonus(env.getCharacter()) * env.getCharacter().getLevelMod());
			}
		}
		else
		{
			env.mulValue(BaseStats.MEN.calcBonus(env.getCharacter()) * env.getCharacter().getLevelMod());
		}
	}
}