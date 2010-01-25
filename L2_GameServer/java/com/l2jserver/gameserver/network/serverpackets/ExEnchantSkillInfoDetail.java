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

import com.l2jserver.Config;
import com.l2jserver.gameserver.datatables.SkillTreeTable;
import com.l2jserver.gameserver.model.L2EnchantSkillLearn;
import com.l2jserver.gameserver.model.L2EnchantSkillLearn.EnchantSkillDetail;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;

/**
 * 
 * @author KenM
 */
public class ExEnchantSkillInfoDetail extends L2GameServerPacket
{
	private static final int TYPE_NORMAL_ENCHANT = 0;
	private static final int TYPE_SAFE_ENCHANT = 1;
	private static final int TYPE_UNTRAIN_ENCHANT = 2;
	private static final int TYPE_CHANGE_ENCHANT = 3;

	private int bookId = 0;
	private int reqCount = 0;
	private int multi = 1;
	private final int _type;
	private final int _skillid;
	private final int _skilllvl;
	private final int _chance;
	private int _sp;
	private final int _adenacount;

	public ExEnchantSkillInfoDetail(int type, int skillid, int skilllvl, L2PcInstance ply)
	{

		L2EnchantSkillLearn enchantLearn = SkillTreeTable.getInstance().getSkillEnchantmentBySkillId(skillid);
		EnchantSkillDetail esd = null;
		// do we have this skill?
		if (enchantLearn != null)
		{
			if (skilllvl > 100)
			{
				int route = (skilllvl / 100) -1;
				if (skilllvl % 100 == 0)
					esd = enchantLearn.getEnchantRoutes()[route].get(0);
				else if (skilllvl % 100 >= enchantLearn.getEnchantRoutes()[route].size())
					esd = enchantLearn.getEnchantRoutes()[route].get(enchantLearn.getEnchantRoutes()[route].size()-1);
				else
					esd = enchantLearn.getEnchantSkillDetail(skilllvl);
			}
			else
				//find first enchant route
				for (List<EnchantSkillDetail> list : enchantLearn.getEnchantRoutes())
					if (list != null)
					{
						esd = list.get(0);
						break;
					}
		}

		if (esd == null)
			throw new IllegalArgumentException("Skill "+skillid + " dont have enchant data for level "+skilllvl);

		if (type == 0)
			multi = SkillTreeTable.NORMAL_ENCHANT_COST_MULTIPLIER;
		else if (type == 1)
			multi = SkillTreeTable.SAFE_ENCHANT_COST_MULTIPLIER;
		_chance = esd.getRate(ply);
		_sp = esd.getSpCost();
		if (type == TYPE_UNTRAIN_ENCHANT)
			_sp = (int) (0.8 * _sp);
		_adenacount = esd.getAdena() * multi;
		_type = type;
		_skillid = skillid;
		_skilllvl = skilllvl;

		switch (type)
		{
		case TYPE_NORMAL_ENCHANT:
			bookId = SkillTreeTable.NORMAL_ENCHANT_BOOK;
			reqCount = ((_skilllvl % 100 > 1) ? 0 : 1) ;
			break;
		case TYPE_SAFE_ENCHANT:
			bookId = SkillTreeTable.SAFE_ENCHANT_BOOK;
			reqCount = 1;
			break;
		case TYPE_UNTRAIN_ENCHANT:
			bookId = SkillTreeTable.UNTRAIN_ENCHANT_BOOK;
			reqCount = 1;
			break;
		case TYPE_CHANGE_ENCHANT:
			bookId = SkillTreeTable.CHANGE_ENCHANT_BOOK;
			reqCount = 1;
			break;
		default:
			return;
		}
		
		if (type != TYPE_SAFE_ENCHANT && !Config.ES_SP_BOOK_NEEDED)
			reqCount = 0;
	}

	/**
	 * @see com.l2jserver.gameserver.network.serverpackets.L2GameServerPacket#getType()
	 */
	@Override
	public String getType()
	{
		return "[S] FE:5E ExEnchantSkillInfoDetail";
	}

	/**
	 * @see com.l2jserver.gameserver.network.serverpackets.L2GameServerPacket#writeImpl()
	 */
	@Override
	protected void writeImpl()
	{
		writeC(0xfe);
		writeH(0x5e);

		writeD(_type);
		writeD(_skillid);
		writeD(_skilllvl);
		writeD(_sp * multi); // sp
		writeD(_chance); // exp
		writeD(2); // items count?
		writeD(57); // adena //TODO unhardcode me
		writeD(_adenacount); // adena count
		writeD(bookId); // ItemId Required
		writeD(reqCount);
	}

}
