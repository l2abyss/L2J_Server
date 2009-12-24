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
package com.l2jserver.gameserver.network.serverpackets;

import java.util.List;

import com.l2jserver.gameserver.datatables.SkillTreeTable;
import com.l2jserver.gameserver.model.L2EnchantSkillLearn;
import com.l2jserver.gameserver.model.L2EnchantSkillLearn.EnchantSkillDetail;

import javolution.util.FastList;

public final class ExEnchantSkillInfo extends L2GameServerPacket
{
	private static final String _S__FE_18_EXENCHANTSKILLINFO = "[S] FE:2a ExEnchantSkillInfo";
	private FastList<Integer> _routes; //skill lvls for each route

	private final int _id;
	private final int _lvl;
	private boolean _maxEnchanted = false;

	public ExEnchantSkillInfo(int id, int lvl)
	{
		_routes = new FastList<Integer>();
		_id = id;
		_lvl = lvl;

		L2EnchantSkillLearn enchantLearn = SkillTreeTable.getInstance().getSkillEnchantmentBySkillId(_id);
		// do we have this skill?
		if (enchantLearn != null)
		{
			// skill already enchanted?
			if (_lvl > 100)
			{
				int route = (_lvl / 100) -1;
				if (_lvl % 100 >= enchantLearn.getEnchantRoutes()[route].size())
					_maxEnchanted = true;
				
				// get detail for next level
				EnchantSkillDetail esd = enchantLearn.getEnchantSkillDetail(_lvl);

				// if it exists add it
				if (esd != null)
				{
					_routes.add(esd.getLevel()); // current enchant add firts
				}

				int diff = (_lvl % 100) - 1; // indexed form 0

				for (List<EnchantSkillDetail> esd1 : enchantLearn.getEnchantRoutes())
				{
					if (esd1 == null)
						continue;
					if (esd1.get(0).getLevel() == _lvl) // skip current
						continue;
					// add other levels of all routes - same lvl as enchanted
					// lvl
					_routes.add(esd1.get(diff).getLevel());
				}

			}
			else
			// not already enchanted
			{
				for (List<EnchantSkillDetail> esd : enchantLearn.getEnchantRoutes())
				{
					if (esd == null)
						continue;
					// add first level (+1) of all routes
					_routes.add(esd.get(0).getLevel());
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.l2jserver.gameserver.serverpackets.ServerBasePacket#writeImpl()
	 */
	@Override
	protected void writeImpl()
	{
		writeC(0xfe);
		writeH(0x2a);
		writeD(_id);
		writeD(_lvl);
		writeD(_maxEnchanted ? 0 : 1);
		writeD(_lvl > 100 ? 1 : 0); // enchanted?
		writeD(_routes.size());

		for (Integer level : _routes)
		{
			writeD(level);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.l2jserver.gameserver.BasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__FE_18_EXENCHANTSKILLINFO;
	}

}