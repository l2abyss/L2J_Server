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

import java.util.logging.Logger;

import com.l2jserver.Config;
import com.l2jserver.gameserver.model.L2ItemInstance;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;

import javolution.util.FastList;

/**
 * 0x53 WareHouseDepositList  dh (h dddhh dhhh d)
 *
 * @version $Revision: 1.4.2.1.2.4 $ $Date: 2005/03/27 15:29:39 $
 */
public final class WareHouseDepositList extends L2GameServerPacket
{
	public static final int PRIVATE = 1;
	public static final int CLAN = 4;
	public static final int CASTLE = 3; //not sure
	public static final int FREIGHT = 1;
	private static Logger _log = Logger.getLogger(WareHouseDepositList.class.getName());
	private static final String _S__53_WAREHOUSEDEPOSITLIST = "[S] 41 WareHouseDepositList";
	private final long _playerAdena;
	private final FastList<L2ItemInstance> _items;
	private final int _whType;

	public WareHouseDepositList(L2PcInstance player, int type)
	{
		_whType = type;
		_playerAdena = player.getAdena();
		_items = new FastList<L2ItemInstance>();

		final boolean isPrivate = _whType == PRIVATE; 
		for (L2ItemInstance temp : player.getInventory().getAvailableItems(true, isPrivate))
		{
			if (temp != null && temp.isDepositable(isPrivate))
				_items.add(temp);
		}
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x41);
		/* 0x01-Private Warehouse
		* 0x02-Clan Warehouse
		* 0x03-Castle Warehouse
		* 0x04-Warehouse */
		writeH(_whType);
		writeQ(_playerAdena);
		final int count = _items.size();
		if (Config.DEBUG)
			_log.fine("count:" + count);
		writeH(count);

		for (L2ItemInstance item : _items)
		{
			writeH(item.getItem().getType1());
			writeD(item.getObjectId());
			writeD(item.getItemId());
			writeQ(item.getCount());
			writeH(item.getItem().getType2());
			writeH(item.getCustomType1());
			writeD(item.getItem().getBodyPart());
			writeH(item.getEnchantLevel());
			writeH(0x00);
			writeH(item.getCustomType2());
			writeD(item.getObjectId());
			if (item.isAugmented())
			{
				writeD(0x0000FFFF & item.getAugmentation().getAugmentationId());
				writeD(item.getAugmentation().getAugmentationId() >> 16);
			}
			else
				writeQ(0x00);

			writeH(item.getAttackElementType());
			writeH(item.getAttackElementPower());
			for (byte i = 0; i < 6; i++)
				writeH(item.getElementDefAttr(i));

			writeD(item.getMana());
			// T2
			writeD(item.isTimeLimitedItem() ? (int) (item.getRemainingTime()/1000) : -1);

			writeH(0x00); // Enchant effect 1
			writeH(0x00); // Enchant effect 2
			writeH(0x00); // Enchant effect 3 
		}
		_items.clear();
	}

	/* (non-Javadoc)
	 * @see com.l2jserver.gameserver.serverpackets.ServerBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__53_WAREHOUSEDEPOSITLIST;
	}
}
