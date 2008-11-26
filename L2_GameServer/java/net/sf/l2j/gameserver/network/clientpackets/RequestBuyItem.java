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
package net.sf.l2j.gameserver.network.clientpackets;

import java.util.List;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.TradeController;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2TradeList;
import net.sf.l2j.gameserver.model.L2TradeList.L2TradeItem;
import net.sf.l2j.gameserver.model.actor.instance.L2CastleChamberlainInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2ClanHallManagerInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2FishermanInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2MercManagerInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2MerchantInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetManagerInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.item.L2Item;
import net.sf.l2j.gameserver.util.Util;

/**
 * This class ...
 *
 * @version $Revision: 1.12.4.4 $ $Date: 2005/03/27 15:29:30 $
 */
public final class RequestBuyItem extends L2GameClientPacket
{
	private static final String _C__1F_REQUESTBUYITEM = "[C] 1F RequestBuyItem";
	private static Logger _log = Logger.getLogger(RequestBuyItem.class.getName());

	private int _listId;
	private int _count;
	private int[] _items; // count*2

	@Override
	protected void readImpl()
	{
		_listId = readD();
		_count = readD();
//		 count*8 is the size of a for iteration of each item
		if(_count * 2 < 0 || _count * 8 > _buf.remaining() || _count > Config.MAX_ITEM_IN_PACKET) _count = 0;


		_items = new int[_count * 2];
		for (int i = 0; i < _count; i++)
		{
			int itemId   = readD(); _items[i * 2 + 0] = itemId;
			long cnt      = readD();
			if (cnt > Integer.MAX_VALUE || cnt < 0)
			{
			    _count=0; _items = null;
			    return;
			}
			_items[i * 2 + 1] = (int)cnt;
		}
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
        if (player == null) return;

        // Alt game - Karma punishment
        if (!Config.ALT_GAME_KARMA_PLAYER_CAN_SHOP && player.getKarma() > 0) return;

        L2Object target = player.getTarget();
        if (!player.isGM() && (target == null								// No target (ie GM Shop)
        		|| !(target instanceof L2MerchantInstance || target instanceof L2FishermanInstance || target instanceof L2MercManagerInstance || target instanceof L2ClanHallManagerInstance || target instanceof L2CastleChamberlainInstance)	// Target not a merchant, fisherman or mercmanager
			    || !player.isInsideRadius(target, L2NpcInstance.INTERACTION_DISTANCE, false, false) 	// Distance is too far
			        )) return;

        boolean ok = true;
        String htmlFolder = "";

        if (target != null)
        {
            /*else if (target instanceof L2MercManagerInstance)
                ok = true;
            else if (target instanceof L2ClanHallManagerInstance)
                ok = true;
            else if (target instanceof L2CastleChamberlainInstance)
                ok = true;*/
            
        	if (target instanceof L2FishermanInstance)
            {
        		htmlFolder = "fisherman";
            }
            else if (target instanceof L2MerchantInstance)
            {
                htmlFolder = "merchant";
            }
            else if (target instanceof L2PetManagerInstance)
            {
                htmlFolder = "petmanager";
            }
        	else
            {
        		ok = false;
            }
        }
        else
        {
        	ok = false;
        }

        L2MerchantInstance merchant = null;

        if (ok)
        	merchant = (L2MerchantInstance) target;
        else if (!ok && !player.isGM())
        {
        	player.sendMessage("Invalid Target: Seller must be targetted");
        	return;
        }

        L2TradeList list = null;

        if (merchant != null)
        {
        	List<L2TradeList> lists = TradeController.getInstance().getBuyListByNpcId(merchant.getNpcId());

        	if(!player.isGM() )
        	{
        		if (lists == null)
        		{
        			Util.handleIllegalPlayerAction(player,"Warning!! Character "+player.getName()+" of account "+player.getAccountName()+" sent a false BuyList list_id.",Config.DEFAULT_PUNISH);
        			return;
        		}
	        	for (L2TradeList tradeList : lists)
	        	{
	        		if (tradeList.getListId() == _listId)
	        		{
	        			list = tradeList;
	        		}
	        	}
        	}
        	else
        	{
        		list = TradeController.getInstance().getBuyList(_listId);
        	}
        }
        else
        	list = TradeController.getInstance().getBuyList(_listId);
        if (list == null)
        {
        	Util.handleIllegalPlayerAction(player,"Warning!! Character "+player.getName()+" of account "+player.getAccountName()+" sent a false BuyList list_id.",Config.DEFAULT_PUNISH);
        	return;
        }

        _listId = list.getListId();

        if(_count < 1)
		{
            sendPacket(ActionFailed.STATIC_PACKET);
		    return;
		}
		double castleTaxRate = 0;
        double baseTaxRate = 0;
		if (merchant != null) 
        {
            castleTaxRate = merchant.getMpc().getCastleTaxRate();
            baseTaxRate = merchant.getMpc().getBaseTaxRate();
        }
		long subTotal = 0;
		int castleTax = 0;
        int baseTax = 0;

		// Check for buylist validity and calculates summary values
		long slots = 0;
		long weight = 0;
		for (int i = 0; i < _count; i++)
		{
			int itemId = _items[i * 2 + 0];
			int count  = _items[i * 2 + 1];
			int price = -1;

            L2TradeItem tradeItem = list.getItemById(itemId);
			if (tradeItem == null)
			{
				Util.handleIllegalPlayerAction(player,"Warning!! Character "+player.getName()+" of account "+player.getAccountName()+" sent a false BuyList list_id.",Config.DEFAULT_PUNISH);
				return;
			}

            L2Item template = ItemTable.getInstance().getTemplate(itemId);

            if (template == null) 
            {
                continue;
            }
            
            if (count > Integer.MAX_VALUE || (!template.isStackable() && count > 1))
			{
				Util.handleIllegalPlayerAction(player,"Warning!! Character "+player.getName()+" of account "+player.getAccountName()+" tried to purchase invalid quantity of items at the same time.",Config.DEFAULT_PUNISH);
				SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_EXCEEDED_QUANTITY_THAT_CAN_BE_INPUTTED);
				sendPacket(sm);
				sm = null;

				return;
			}


            price = list.getPriceForItemId(itemId);
            if (itemId >= 3960 && itemId <= 4026) 
            {
                price *= Config.RATE_SIEGE_GUARDS_PRICE;
            }
			if (price < 0)
			{
				_log.warning("ERROR, no price found .. wrong buylist ??");
                sendPacket(ActionFailed.STATIC_PACKET);
                return;
			}

			if (price == 0 && !player.isGM() && Config.ONLY_GM_ITEMS_FREE)
			{
				player.sendMessage("Ohh Cheat dont work? You have a problem now!");
				Util.handleIllegalPlayerAction(player,"Warning!! Character "+player.getName()+" of account "+player.getAccountName()+" tried buy item for 0 adena.", Config.DEFAULT_PUNISH);
				return;
			}

            if (tradeItem.hasLimitedStock())
            {
                // tryin to buy more then avaliable
                if (count > tradeItem.getCurrentCount())
                {
                    return;
                }
            }
            
			subTotal += (long)count * price;	// Before tax
			castleTax = (int) (subTotal * castleTaxRate);
            baseTax = (int) (subTotal * baseTaxRate);
            if (subTotal + castleTax + baseTax > Integer.MAX_VALUE)
            {
                Util.handleIllegalPlayerAction(player,"Warning!! Character "+player.getName()+" of account "+player.getAccountName()+" tried to purchase over "+Integer.MAX_VALUE+" adena worth of goods.", Config.DEFAULT_PUNISH);
                return;
            }

            weight += (long)count * template.getWeight();
			if (!template.isStackable()) slots += count;
            else if (player.getInventory().getItemByItemId(itemId) == null) slots++;
		}

		if (weight > Integer.MAX_VALUE || weight < 0 || !player.getInventory().validateWeight((int)weight))
		{
			sendPacket(new SystemMessage(SystemMessageId.WEIGHT_LIMIT_EXCEEDED));
			return;
		}

		if (slots > Integer.MAX_VALUE || slots < 0 || !player.getInventory().validateCapacity((int)slots))
		{
			sendPacket(new SystemMessage(SystemMessageId.SLOTS_FULL));
			return;
		}

		// Charge buyer and add tax to castle treasury if not owned by npc clan
		if ((subTotal < 0) || !player.reduceAdena("Buy", (int)(subTotal + baseTax + castleTax), player.getLastFolkNPC(), false))
		{
			sendPacket(new SystemMessage(SystemMessageId.YOU_NOT_ENOUGH_ADENA));
			return;
		}

		// Proceed the purchase
		for (int i=0; i < _count; i++)
		{
			int itemId = _items[i * 2 + 0];
			int count  = _items[i * 2 + 1];
			if (count < 0)
            {
                count = 0;
            }
            
            L2TradeItem tradeItem = list.getItemById(itemId);
            if (tradeItem == null)
			{
				Util.handleIllegalPlayerAction(player,"Warning!! Character "+player.getName()+" of account "+player.getAccountName()+" sent a false BuyList list_id.",Config.DEFAULT_PUNISH);
			}
            else
            {
                if (tradeItem.hasLimitedStock())
                {
                    if (tradeItem.decreaseCount(count))
                    {
                        player.getInventory().addItem("Buy", itemId, count, player, merchant);
                    }
                }
                else
                {
                    player.getInventory().addItem("Buy", itemId, count, player, merchant);
                }
                
            }
		}

		if (merchant != null)
		{
            // add to castle treasury
            merchant.getCastle().addToTreasury(castleTax);
            
			String html = HtmCache.getInstance().getHtm("data/html/"+ htmlFolder +"/" + merchant.getNpcId() + "-bought.htm");

			if (html != null)
			{
				NpcHtmlMessage boughtMsg = new NpcHtmlMessage(merchant.getObjectId());
				boughtMsg.setHtml(html.replaceAll("%objectId%", String.valueOf(merchant.getObjectId())));
				player.sendPacket(boughtMsg);
			}
		}

		StatusUpdate su = new StatusUpdate(player.getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
		player.sendPacket(su);
		player.sendPacket(new ItemList(player, true));
	}

	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _C__1F_REQUESTBUYITEM;
	}
}
