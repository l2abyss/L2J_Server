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

import net.sf.l2j.gameserver.model.TradeList;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 * This class ...
 *
 * @version $Revision: 1.7.2.2.2.3 $ $Date: 2005/03/27 15:29:39 $
 */
public class PrivateStoreListBuy extends L2GameServerPacket
{
//	private static final String _S__D1_PRIVATEBUYLISTBUY = "[S] b8 PrivateBuyListBuy";
	private static final String _S__D1_PRIVATESTORELISTBUY = "[S] be PrivateStoreListBuy";
	private L2PcInstance _storePlayer;
	private L2PcInstance _activeChar;
	private int _playerAdena;
	private TradeList.TradeItem[] _items;

	public PrivateStoreListBuy(L2PcInstance player, L2PcInstance storePlayer)
	{
		_storePlayer = storePlayer;
		_activeChar = player;
		_playerAdena = _activeChar.getAdena();
		_storePlayer.getSellList().updateItems(); // Update SellList for case inventory content has changed
		_items = _storePlayer.getBuyList().getAvailableItems(_activeChar.getInventory());
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xbe);
		writeD(_storePlayer.getObjectId());
		writeD(_playerAdena);

		writeD(_items.length);

		for (TradeList.TradeItem item : _items)
		{
			writeD(item.getObjectId());
			writeD(item.getItem().getItemId());
			writeH(item.getEnchant());
			writeD(item.getCount()); //give max possible sell amount

			writeD(item.getItem().getReferencePrice());
			writeH(0);

			writeD(item.getItem().getBodyPart());
			writeH(item.getItem().getType2());
			writeD(item.getPrice());//buyers price

			writeD(item.getCount());  // maximum possible tradecount
			
			// T1
            writeD(item.getAttackAttrElement());
            writeD(item.getAttackAttrElementVal());
            writeD(item.getDefAttrFire());
            writeD(item.getDefAttrWater());
            writeD(item.getDefAttrWind());
            writeD(item.getDefAttrEarth());
            writeD(item.getDefAttrHoly());
            writeD(item.getDefAttrUnholy());
		}
	}

	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.serverpackets.ServerBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__D1_PRIVATESTORELISTBUY;
	}
}
