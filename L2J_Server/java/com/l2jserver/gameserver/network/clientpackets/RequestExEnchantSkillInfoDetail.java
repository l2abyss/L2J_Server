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
package com.l2jserver.gameserver.network.clientpackets;

import java.util.logging.Logger;

import com.l2jserver.gameserver.datatables.EnchantGroupsTable;
import com.l2jserver.gameserver.model.L2EnchantSkillLearn;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.network.serverpackets.ExEnchantSkillInfoDetail;


/**
 * Format (ch) ddd c: (id) 0xD0 h: (subid) 0x31 d: type d: skill id d: skill lvl
 * 
 * @author -Wooden-
 * 
 */
public final class RequestExEnchantSkillInfoDetail extends L2GameClientPacket
{
	protected static final Logger _log = Logger.getLogger(RequestExEnchantSkillInfoDetail.class.getName());
	
	private int _type;
	private int _skillId;
	private int _skillLvl;
	
	@Override
	protected void readImpl()
	{
		_type = readD();
		_skillId = readD();
		_skillLvl = readD();
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.l2jserver.gameserver.clientpackets.ClientBasePacket#runImpl()
	 */
	@Override
	protected void runImpl()
	{
		if (_skillId <= 0 || _skillLvl <= 0) // minimal sanity check
			return;

		L2PcInstance activeChar = getClient().getActiveChar();
		
		if (activeChar == null)
			return;
		
		int reqSkillLvl = -2;
		
		if (_type == 0 || _type == 1)
			reqSkillLvl = _skillLvl - 1; // enchant
		else if (_type == 2)
			reqSkillLvl = _skillLvl + 1; // untrain
		else if (_type == 3)
			reqSkillLvl = _skillLvl; // change route
		
		int playerSkillLvl = activeChar.getSkillLevel(_skillId);
		
		// dont have such skill
		if (playerSkillLvl == -1)
			return;
		
		// if reqlvl is 100,200,.. check base skill lvl enchant
		if ((reqSkillLvl % 100) == 0)
		{
			L2EnchantSkillLearn esl = EnchantGroupsTable.getInstance().getSkillEnchantmentBySkillId(_skillId);
			if (esl != null)
			{
				// if player dont have min level to enchant
				if (playerSkillLvl != esl.getBaseLevel())
					return;
			}
			// enchant data dont exist?
			else
				return;
		}
		else if (playerSkillLvl != reqSkillLvl)
		{
			// change route is different skill lvl but same enchant
			if (_type == 3 && ((playerSkillLvl % 100) != (_skillLvl % 100)))
				return;
		}
		
		// send skill enchantment detail
		ExEnchantSkillInfoDetail esd = new ExEnchantSkillInfoDetail(_type, _skillId, _skillLvl, activeChar);
		activeChar.sendPacket(esd);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.l2jserver.gameserver.BasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return "[C] D0:46 RequestExEnchantSkillInfo";
	}
	
}
