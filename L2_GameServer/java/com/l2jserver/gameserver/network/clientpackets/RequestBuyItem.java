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

import static com.l2jserver.gameserver.model.actor.L2Npc.INTERACTION_DISTANCE;
import static com.l2jserver.gameserver.model.itemcontainer.PcInventory.MAX_ADENA;

import java.util.List;
import java.util.logging.Logger;

import com.l2jserver.Config;
import com.l2jserver.gameserver.TradeController;
import com.l2jserver.gameserver.datatables.ItemTable;
import com.l2jserver.gameserver.model.L2Object;
import com.l2jserver.gameserver.model.L2TradeList;
import com.l2jserver.gameserver.model.L2TradeList.L2TradeItem;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.instance.L2MerchantInstance;
import com.l2jserver.gameserver.model.actor.instance.L2MerchantSummonInstance;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.ActionFailed;
import com.l2jserver.gameserver.network.serverpackets.ExBuySellListPacket;
import com.l2jserver.gameserver.network.serverpackets.StatusUpdate;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;
import com.l2jserver.gameserver.templates.item.L2Item;
import com.l2jserver.gameserver.util.Util;

/**
 * RequestBuyItem client packet class.
 */
public final class RequestBuyItem extends L2GameClientPacket
{
	private static final String _C__1F_REQUESTBUYITEM = "[C] 1F RequestBuyItem";
	private static Logger _log = Logger.getLogger(RequestBuyItem.class.getName());
	
	private static final int BATCH_LENGTH = 12; // length of the one item
	
	private int _listId;
	private Item[] _items = null;
	
	@Override
	protected void readImpl()
	{
		_listId = readD();
		int count = readD();
		if (count <= 0 || count > Config.MAX_ITEM_IN_PACKET || count * BATCH_LENGTH != _buf.remaining())
		{
			return;
		}
		
		_items = new Item[count];
		for (int i = 0; i < count; i++)
		{
			int itemId = readD();
			long cnt = readQ();
			if (itemId < 1 || cnt < 1)
			{
				_items = null;
				return;
			}
			_items[i] = new Item(itemId, cnt);
		}
	}
	
	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if (player == null)
			return;
		
		if (!player.getFloodProtectors().getTransaction().tryPerformAction("buy"))
		{
			player.sendMessage("You buying too fast.");
			return;
		}
		
		if (_items == null)
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Alt game - Karma punishment
		if (!Config.ALT_GAME_KARMA_PLAYER_CAN_SHOP && player.getKarma() > 0)
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		L2Object target = player.getTarget();
		if (!player.isGM() && (target == null // No target (ie GM Shop)
				|| !(target instanceof L2MerchantInstance || target instanceof L2MerchantSummonInstance) || player.getInstanceId() != target.getInstanceId() || !player.isInsideRadius(target, INTERACTION_DISTANCE, true, false))) // Distance is too far
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		L2Character merchant = null;
		if (target instanceof L2MerchantInstance || target instanceof L2MerchantSummonInstance)
			merchant = (L2Character) target;
		else if (!player.isGM())
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		L2TradeList list = null;
		
		double castleTaxRate = 0;
		double baseTaxRate = 0;
		
		if (merchant != null)
		{
			List<L2TradeList> lists;
			if (merchant instanceof L2MerchantInstance)
			{
				lists = TradeController.getInstance().getBuyListByNpcId(((L2MerchantInstance) merchant).getNpcId());
				castleTaxRate = ((L2MerchantInstance) merchant).getMpc().getCastleTaxRate();
				baseTaxRate = ((L2MerchantInstance) merchant).getMpc().getBaseTaxRate();
			}
			else
			{
				lists = TradeController.getInstance().getBuyListByNpcId(((L2MerchantSummonInstance) merchant).getNpcId());
				baseTaxRate = 50;
			}
			
			if (!player.isGM())
			{
				if (lists == null)
				{
					Util.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " sent a false BuyList list_id " + _listId, Config.DEFAULT_PUNISH);
					return;
				}
				for (L2TradeList tradeList : lists)
				{
					if (tradeList.getListId() == _listId)
						list = tradeList;
				}
			}
			else
				list = TradeController.getInstance().getBuyList(_listId);
		}
		else
			list = TradeController.getInstance().getBuyList(_listId);
		
		if (list == null)
		{
			Util.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " sent a false BuyList list_id " + _listId, Config.DEFAULT_PUNISH);
			return;
		}
		
		_listId = list.getListId();
		
		long subTotal = 0;
		long castleTax = 0;
		long baseTax = 0;
		
		// Check for buylist validity and calculates summary values
		long slots = 0;
		long weight = 0;
		for (Item i : _items)
		{
			long price = -1;
			
			L2TradeItem tradeItem = list.getItemById(i.getItemId());
			if (tradeItem == null)
			{
				Util.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " sent a false BuyList list_id " + _listId + " and item_id " + i.getItemId(), Config.DEFAULT_PUNISH);
				return;
			}
			
			L2Item template = ItemTable.getInstance().getTemplate(i.getItemId());
			if (template == null)
				continue;
			
			if (!template.isStackable() && i.getCount() > 1)
			{
				Util.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " tried to purchase invalid quantity of items at the same time.", Config.DEFAULT_PUNISH);
				SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_EXCEEDED_QUANTITY_THAT_CAN_BE_INPUTTED);
				sendPacket(sm);
				sm = null;
				return;
			}
			
			price = list.getPriceForItemId(i.getItemId());
			if (i.getItemId() >= 3960 && i.getItemId() <= 4026)
				price *= Config.RATE_SIEGE_GUARDS_PRICE;
			
			if (price < 0)
			{
				_log.warning("ERROR, no price found .. wrong buylist ??");
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			if (price == 0 && !player.isGM() && Config.ONLY_GM_ITEMS_FREE)
			{
				player.sendMessage("Ohh Cheat dont work? You have a problem now!");
				Util.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " tried buy item for 0 adena.", Config.DEFAULT_PUNISH);
				return;
			}
			
			if (tradeItem.hasLimitedStock())
			{
				// trying to buy more then avaliable
				if (i.getCount() > tradeItem.getCurrentCount())
					return;
			}
			
			if ((MAX_ADENA / i.getCount()) < price)
			{
				Util.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " tried to purchase over " + MAX_ADENA + " adena worth of goods.", Config.DEFAULT_PUNISH);
				return;
			}
			subTotal += i.getCount() * price; // Before tax
			castleTax = (long) (subTotal * castleTaxRate);
			baseTax = (long) (subTotal * baseTaxRate);
			if (subTotal + castleTax + baseTax > MAX_ADENA)
			{
				Util.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " tried to purchase over " + MAX_ADENA + " adena worth of goods.", Config.DEFAULT_PUNISH);
				return;
			}
			
			weight += i.getCount() * template.getWeight();
			if (!template.isStackable())
				slots += i.getCount();
			else if (player.getInventory().getItemByItemId(i.getItemId()) == null)
				slots++;
		}
		
		if (weight > Integer.MAX_VALUE || weight < 0 || !player.getInventory().validateWeight((int) weight))
		{
			sendPacket(new SystemMessage(SystemMessageId.WEIGHT_LIMIT_EXCEEDED));
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (slots > Integer.MAX_VALUE || slots < 0 || !player.getInventory().validateCapacity((int) slots))
		{
			sendPacket(new SystemMessage(SystemMessageId.SLOTS_FULL));
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Charge buyer and add tax to castle treasury if not owned by npc clan
		if ((subTotal < 0) || !player.reduceAdena("Buy", (subTotal + baseTax + castleTax), player.getLastFolkNPC(), false))
		{
			sendPacket(new SystemMessage(SystemMessageId.YOU_NOT_ENOUGH_ADENA));
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Proceed the purchase
		for (Item i : _items)
		{
			L2TradeItem tradeItem = list.getItemById(i.getItemId());
			if (tradeItem == null)
			{
				Util.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " sent a false BuyList list_id " + _listId + " and item_id " + i.getItemId(), Config.DEFAULT_PUNISH);
				continue;
			}
			
			if (tradeItem.hasLimitedStock())
			{
				if (tradeItem.decreaseCount(i.getCount()))
					player.getInventory().addItem("Buy", i.getItemId(), i.getCount(), player, merchant);
			}
			else
				player.getInventory().addItem("Buy", i.getItemId(), i.getCount(), player, merchant);
		}
		
		// add to castle treasury
		if (merchant instanceof L2MerchantInstance)
			((L2MerchantInstance) merchant).getCastle().addToTreasury(castleTax);
		
		StatusUpdate su = new StatusUpdate(player.getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
		player.sendPacket(su);
		player.sendPacket(new ExBuySellListPacket(player, list, castleTaxRate + baseTaxRate, true));
	}
	
	private class Item
	{
		private final int _itemId;
		private final long _count;
		
		public Item(int id, long num)
		{
			_itemId = id;
			_count = num;
		}
		
		public int getItemId()
		{
			return _itemId;
		}
		
		public long getCount()
		{
			return _count;
		}
	}
	
	/* (non-Javadoc)
	 * @see com.l2jserver.gameserver.clientpackets.ClientBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _C__1F_REQUESTBUYITEM;
	}
}
