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

import com.l2jserver.Config;
import com.l2jserver.gameserver.model.L2ItemInstance;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;

/**
 * This class ...
 *
 * @version $Revision: 1.4.2.1.2.3 $ $Date: 2005/03/27 15:29:39 $
 */
public final class TradeStart extends L2GameServerPacket
{
	private static final String _S__2E_TRADESTART = "[S] 14 TradeStart";
	private L2PcInstance _activeChar;
	private L2ItemInstance[] _itemList;
	
	public TradeStart(L2PcInstance player)
	{
		_activeChar = player;
		_itemList = _activeChar.getInventory().getAvailableItems(true, (_activeChar.isGM() && Config.GM_TRADE_RESTRICTED_ITEMS));
	}
	
	@Override
	protected final void writeImpl()
	{//0x2e TradeStart   d h (h dddhh dhhh)
		if (_activeChar.getActiveTradeList() == null || _activeChar.getActiveTradeList().getPartner() == null)
			return;
		
		writeC(0x14);
		writeD(_activeChar.getActiveTradeList().getPartner().getObjectId());
		//writeD((_activeChar != null || _activeChar.getTransactionRequester() != null)? _activeChar.getTransactionRequester().getObjectId() : 0);
		
		writeH(_itemList.length);
		for (L2ItemInstance item : _itemList)
		{
			writeD(item.getObjectId());
			writeD(item.getItem().getItemId());
			writeD(item.getLocationSlot());
			writeQ(item.getCount());
			writeH(item.getItem().getType2());
			writeH(item.getCustomType1());
			writeH(0x00);
			writeD(item.getItem().getBodyPart());
			writeH(item.getEnchantLevel());
			writeH(item.getCustomType2());
			// Player cannot sell/buy augmented, shadow or time-limited items
			// probably so hardcode values here
			writeD(0x00);	// Augment
			writeD(-1);		// Mana
			writeD(-9999);	// Time
			writeH(item.getAttackElementType());
			writeH(item.getAttackElementPower());
			for (byte i = 0; i < 6; i++)
			{
				writeH(item.getElementDefAttr(i));
			}
			// Enchant Effects
			writeH(0x00);
			writeH(0x00);
			writeH(0x00);
		}
	}
	
	/* (non-Javadoc)
	 * @see com.l2jserver.gameserver.serverpackets.ServerBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__2E_TRADESTART;
	}
}
