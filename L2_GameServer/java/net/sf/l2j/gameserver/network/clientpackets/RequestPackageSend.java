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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.itemcontainer.ItemContainer;
import net.sf.l2j.gameserver.model.itemcontainer.PcFreight;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;
import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.item.L2EtcItemType;

import static net.sf.l2j.gameserver.model.actor.L2Npc.INTERACTION_DISTANCE;
import static net.sf.l2j.gameserver.model.itemcontainer.PcInventory.ADENA_ID;

/**
 *
 * @author  -Wooden-
 */
public final class RequestPackageSend extends L2GameClientPacket
{
	private static final String _C_9F_REQUESTPACKAGESEND = "[C] 9F RequestPackageSend";
	private static Logger _log = Logger.getLogger(RequestPackageSend.class.getName());

	private static final int BATCH_LENGTH = 12; // length of the one item

	private List<Item> _items = null;
	private int _objectID;

	@Override
	protected void readImpl()
	{
		_objectID = readD();
		int count = readD();
		if (count < 0
				|| count > Config.MAX_ITEM_IN_PACKET
				|| count * BATCH_LENGTH != _buf.remaining())
		{
			return;
		}
		_items = new ArrayList<Item>(count);
		for(int i = 0; i < count; i++)
		{
			int id = readD(); //this is some id sent in PackageSendableList
			long cnt = readQ();
			_items.add(new Item(id, cnt));
		}
	}

	/**
	 * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#runImpl()
	 */
	@Override
	protected
	void runImpl()
	{
		if (_items == null || _items.isEmpty())
			return;

		L2PcInstance player = getClient().getActiveChar();
		if (player == null)
			return;

		// Alt game - Karma punishment
		if (!Config.ALT_GAME_KARMA_PLAYER_CAN_USE_WAREHOUSE && player.getKarma() > 0)
			return;

		PcFreight freight = new PcFreight(null);
		freight.doQuickRestore(_objectID);

		player.setActiveWarehouse(freight);
		ItemContainer warehouse = player.getActiveWarehouse();
		if (warehouse == null)
			return;

		L2NpcInstance manager = player.getLastFolkNPC();
		if ((manager == null || !player.isInsideRadius(manager, INTERACTION_DISTANCE, false, false)) && !player.isGM())
			return;

		if (warehouse instanceof PcFreight && !player.getAccessLevel().allowTransaction())
		{
			player.sendMessage("Transactions are disable for your Access Level");
			return;
		}

    	// Freight price from config or normal price per item slot (30)
		long fee = _items.size() * Config.ALT_GAME_FREIGHT_PRICE;
		long currentAdena = player.getAdena();
		int slots = 0;

		Iterator<Item> iter = _items.iterator();
		while (iter.hasNext())
		{
			Item i = iter.next();
			// Check validity of requested item
			L2ItemInstance item = player.checkItemManipulation(i.objectId, i.count, "deposit");
			if (item == null)
			{
				_log.warning("Error depositing a warehouse object for char "+player.getName()+" (validity check)");
				iter.remove();
				continue;
			}

			if (!item.isTradeable() || item.getItemType() == L2EtcItemType.QUEST)
				return;

			// Calculate needed adena and slots
			if (item.getItemId() == ADENA_ID)
				currentAdena -= i.count;
			if (!item.isStackable())
				slots += i.count;
			else if (warehouse.getItemByItemId(item.getItemId()) == null)
				slots++;
		}

    	// Item Max Limit Check
		if (!warehouse.validateCapacity(slots))
		{
			sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_EXCEEDED_QUANTITY_THAT_CAN_BE_INPUTTED));
			return;
		}

		// Check if enough adena and charge the fee
		if (currentAdena < fee || !player.reduceAdena("Warehouse", fee, player.getLastFolkNPC(), false))
		{
			sendPacket(new SystemMessage(SystemMessageId.YOU_NOT_ENOUGH_ADENA));
			return;
		}

		// Proceed to the transfer
		InventoryUpdate playerIU = Config.FORCE_INVENTORY_UPDATE ? null : new InventoryUpdate();
		for (Item i : _items)
		{
			L2ItemInstance oldItem = player.getInventory().getItemByObjectId(i.objectId);
			if (oldItem == null)
			{
				_log.warning("Error depositing a warehouse object for char "+player.getName()+" (olditem == null)");
				continue;
			}

			if (oldItem.isHeroItem())
				continue;

			L2ItemInstance newItem = player.getInventory().transferItem("Warehouse", i.objectId, i.count, warehouse, player, manager);
			if (newItem == null)
			{
				_log.warning("Error depositing a warehouse object for char "+player.getName()+" (newitem == null)");
				continue;
			}

			if (playerIU != null)
			{
				if (oldItem.getCount() > 0 && oldItem != newItem)
					playerIU.addModifiedItem(oldItem);
				else
					playerIU.addRemovedItem(oldItem);
			}
		}

		// Send updated item list to the player
		if (playerIU != null)
			player.sendPacket(playerIU);
		else
			player.sendPacket(new ItemList(player, false));

		// Update current load status on player
		StatusUpdate su = new StatusUpdate(player.getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
		player.sendPacket(su);
	}

	/**
	 * @see net.sf.l2j.gameserver.BasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _C_9F_REQUESTPACKAGESEND;
	}

	private class Item
	{
		final int objectId;
		final long count;

		public Item(int i, long c)
		{
			objectId = i;
			count = c;
		}
	}
}
