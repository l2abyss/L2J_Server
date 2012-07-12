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

import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.items.L2Henna;

/**
 * @author KenM, Zoey76
 */
public final class GMHennaInfo extends L2GameServerPacket
{
	private final L2PcInstance _activeChar;
	
	public GMHennaInfo(L2PcInstance player)
	{
		_activeChar = player;
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0xF0);
		writeC(_activeChar.getHennaStatINT()); // equip INT
		writeC(_activeChar.getHennaStatSTR()); // equip STR
		writeC(_activeChar.getHennaStatCON()); // equip CON
		writeC(_activeChar.getHennaStatMEN()); // equip MEN
		writeC(_activeChar.getHennaStatDEX()); // equip DEX
		writeC(_activeChar.getHennaStatWIT()); // equip WIT
		writeD(3); // Slots
		writeD(3 - _activeChar.getHennaEmptySlots());
		for (L2Henna henna : _activeChar.getHennaList())
		{
			if (henna != null)
			{
				writeD(henna.getDyeId());
				writeD(0x01);
			}
		}
	}
}
