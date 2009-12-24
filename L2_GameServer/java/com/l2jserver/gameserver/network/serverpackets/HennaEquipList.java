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

import com.l2jserver.gameserver.model.L2HennaInstance;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;

public class HennaEquipList extends L2GameServerPacket
{
	private static final String _S__E2_HennaEquipList = "[S] ee HennaEquipList";

	private L2PcInstance _player;
	private L2HennaInstance[] _hennaEquipList;

	public HennaEquipList(L2PcInstance player,L2HennaInstance[] hennaEquipList)
	{
		_player = player;
		_hennaEquipList = hennaEquipList;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xee);
		writeQ(_player.getAdena()); //activeChar current amount of adena
		writeD(3); //available equip slot
		writeD(_hennaEquipList.length);

		for (L2HennaInstance temp: _hennaEquipList)
		{
			// Player must have at least one dye in inventory
			// to be able to see the henna that can be applied with it.
			if ((_player.getInventory().getItemByItemId(temp.getItemIdDye())) != null)
			{
				writeD(temp.getSymbolId()); //symbolId
				writeD(temp.getItemIdDye()); //itemId of dye
				writeQ(temp.getAmountDyeRequire()); //amount of dye require
				writeQ(temp.getPrice()); //amount of adena required
				writeD(1); //meet the requirement or not
			}
			else
			{
				writeD(0x00);
				writeD(0x00);
				writeQ(0x00);
				writeQ(0x00);
				writeD(0x00);
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.l2jserver.gameserver.serverpackets.ServerBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__E2_HennaEquipList;
	}
}
