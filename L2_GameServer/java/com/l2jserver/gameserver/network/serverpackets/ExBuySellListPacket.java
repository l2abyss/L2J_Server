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

import javolution.util.FastList;

import com.l2jserver.Config;
import com.l2jserver.gameserver.model.L2ItemInstance;
import com.l2jserver.gameserver.model.L2TradeList;
import com.l2jserver.gameserver.model.L2TradeList.L2TradeItem;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.templates.item.L2Item;

/**
 * 
 * @author ShanSoft
 *
 */
public class ExBuySellListPacket extends L2GameServerPacket
{
	private static final String _S__B7_ExBuySellListPacket = "[S] B7 ExBuySellListPacket";

	private int _buyListId;
	private List<L2TradeItem> _buyList = new FastList<L2TradeItem>();
	private long _money;
	private double _taxRate = 0;
	private L2ItemInstance[] _sellList = null;
	private L2ItemInstance[] _refundList = null;
	private boolean _done;

	public ExBuySellListPacket(L2PcInstance player, L2TradeList list, boolean done)
	{
		_money = player.getAdena();
		_buyListId = list.getListId();
		for (L2TradeItem item : list.getItems())
		{
			if (item.hasLimitedStock() && item.getCurrentCount() <= 0)
				continue;
			_buyList.add(item);
		}
		_sellList = player.getInventory().getAvailableItems(true, false);
		if (player.hasRefund())
			_refundList = player.getRefund().getItems();
		_done = done;
	}

	public ExBuySellListPacket(L2PcInstance player, L2TradeList list, double taxRate, boolean done)
	{
		_money = player.getAdena();
		_taxRate = taxRate;
		_buyListId = list.getListId();
		for (L2TradeItem item : list.getItems())
		{
			if (item.hasLimitedStock() && item.getCurrentCount() <= 0)
				continue;
			_buyList.add(item);
		}
		_sellList = player.getInventory().getAvailableItems(false, false);
		if (player.hasRefund())
			_refundList = player.getRefund().getItems();
		_done = done;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xFE);
		writeH(0xB7);
		writeQ(_money);

		writeD(_buyListId);
		writeH(_buyList.size());
		for (L2TradeItem item : _buyList)
		{
			writeH(item.getTemplate().getType1());
			writeD(0x00); //objectId
			writeD(item.getItemId());
			writeQ(item.getCurrentCount() < 0 ? 0 : item.getCurrentCount());
			writeH(item.getTemplate().getType2());
			writeH(0x00); // ?
			if (item.getTemplate().getType1() != L2Item.TYPE1_ITEM_QUESTITEM_ADENA)
			{
				writeD(item.getTemplate().getBodyPart());
				writeH(0x00); // item enchant level
				writeH(0x00); // ?
				writeH(0x00);
			}
			else
			{
				writeD(0x00);
				writeH(0x00);
				writeH(0x00);
				writeH(0x00);
			}

			if (item.getItemId() >= 3960 && item.getItemId() <= 4026)//Config.RATE_SIEGE_GUARDS_PRICE-//'
				writeQ((long) (item.getPrice() * Config.RATE_SIEGE_GUARDS_PRICE * (1 + _taxRate)));
			else
				writeQ((long) (item.getPrice() * (1 + _taxRate)));

			// T1
			for (byte i = 0; i < 8; i++)
				writeH(0x00);
			writeH(0x00); // Enchant effect 1
			writeH(0x00); // Enchant effect 2
			writeH(0x00); // Enchant effect 3 
		}

		if (_sellList != null && _sellList.length > 0)
		{
			writeH(_sellList.length);
			for (L2ItemInstance item : _sellList)
			{
				writeH(item.getItem().getType1());
				writeD(item.getObjectId());
				writeD(item.getItemId());
				writeQ(item.getCount());
				writeH(item.getItem().getType2());
				writeH(0x00);
				writeD(item.getItem().getBodyPart());
				writeH(item.getEnchantLevel());
				writeH(0x00);
				writeH(0x00);
				writeQ(item.getItem().getReferencePrice()/2);

				// T1
				writeH(item.getAttackElementType());
				writeH(item.getAttackElementPower());
				for (byte i = 0; i < 6; i++)
					writeH(item.getElementDefAttr(i));
				writeH(0x00); // Enchant effect 1
				writeH(0x00); // Enchant effect 2
				writeH(0x00); // Enchant effect 3 
			}
		}
		else
			writeH(0x00);

		if (_refundList != null && _refundList.length > 0)
		{
			writeH(_refundList.length);
			int idx = 0;
			for (L2ItemInstance item : _refundList)
			{
				writeD(idx++);
				writeD(item.getItemId());
				writeQ(item.getCount());
				writeH(item.getItem().getType2());
				writeH(0x00); // ?
				writeH(item.getEnchantLevel());
				writeH(0x00); // ?
				writeQ(item.getCount() * item.getItem().getReferencePrice() / 2);

				// T1
				writeH(item.getAttackElementType());
				writeH(item.getAttackElementPower());
				for (byte i = 0; i < 6; i++)
					writeH(item.getElementDefAttr(i));
				writeH(0x00); // Enchant effect 1
				writeH(0x00); // Enchant effect 2
				writeH(0x00); // Enchant effect 3 
			}
		}
		else
			writeH(0x00);

		writeC(_done ? 0x01 : 0x00);

		_buyList.clear();
	}

	@Override
	public String getType()
	{
		return _S__B7_ExBuySellListPacket;
	}
}