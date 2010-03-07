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

import com.l2jserver.gameserver.datatables.EnchantGroupsTable;
import com.l2jserver.gameserver.model.L2EnchantSkillLearn;
import com.l2jserver.gameserver.model.L2EnchantSkillGroup.EnchantSkillDetail;

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

		L2EnchantSkillLearn enchantLearn = EnchantGroupsTable.getInstance().getSkillEnchantmentBySkillId(_id);
		// do we have this skill?
		if (enchantLearn != null)
		{
			// skill already enchanted?
			if (_lvl > 100)
			{
				_maxEnchanted = enchantLearn.isMaxEnchant(_lvl);
				
				// get detail for next level
				EnchantSkillDetail esd = enchantLearn.getEnchantSkillDetail(_lvl);

				// if it exists add it
				if (esd != null)
				{
					_routes.add(_lvl); // current enchant add firts
				}

				int skillLvL = (_lvl % 100);

				for (int route : enchantLearn.getAllRoutes())
				{
					if ((route * 100 + skillLvL) == _lvl) // skip current
						continue;
					// add other levels of all routes - same lvl as enchanted
					// lvl
					_routes.add(route * 100 + skillLvL);
				}

			}
			else
			// not already enchanted
			{
				for (int route : enchantLearn.getAllRoutes())
				{
					// add first level (+1) of all routes
					_routes.add(route * 100 + 1);
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