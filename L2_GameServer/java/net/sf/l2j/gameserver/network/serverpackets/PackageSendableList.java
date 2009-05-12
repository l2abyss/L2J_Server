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
package net.sf.l2j.gameserver.network.serverpackets;

import net.sf.l2j.gameserver.model.L2ItemInstance;

/**
 *
 *
 * @author  -Wooden-
 */
public class PackageSendableList extends L2GameServerPacket
{
	private static final String _S__C3_PACKAGESENDABLELIST = "[S] d2 PackageSendableList";
	private L2ItemInstance[] _items;
	private int _playerObjId;

	public PackageSendableList(L2ItemInstance[] items, int playerObjId)
	{
		_items = items;
		_playerObjId = playerObjId;
	}

	/**
	 * @see net.sf.l2j.gameserver.serverpackets.ServerBasePacket#writeImpl()
	 */
	@Override
	protected
	void writeImpl()
	{
		writeC(0xd2);

		writeD(_playerObjId);
		writeQ(getClient().getActiveChar().getAdena());
		writeD(_items.length);
		for(L2ItemInstance item : _items) // format inside the for taken from SellList part use should be about the same
		{
			writeH(item.getItem().getType1());
			writeD(item.getObjectId());
			writeD(item.getItemId());
			writeQ(item.getCount());
			writeH(item.getItem().getType2());
			writeH(item.getCustomType1());
			writeD(item.getItem().getBodyPart());
			writeH(item.getEnchantLevel());
			writeH(item.getCustomType2());
			writeH(0x00);
			writeD(item.getObjectId()); // some item identifier later used by client to answer (see RequestPackageSend) not item id nor object id maybe some freight system id??
			//T1
			writeH(item.getAttackElementType());
			writeH(item.getAttackElementPower());
			for (byte i = 0; i < 6; i++)
			{
				writeH(item.getElementDefAttr(i));
			}
		}
	}

	/**
	 * @see net.sf.l2j.gameserver.BasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__C3_PACKAGESENDABLELIST;
	}

}
