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

import java.util.ArrayList;
import java.util.List;

import com.l2jserver.gameserver.model.L2ItemInstance;
import com.l2jserver.gameserver.model.L2ItemInstance.ItemLocation;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.itemcontainer.Inventory;


/**
 * Format:(ch) d[dd]
 * 
 * @author -Wooden-
 */
public final class RequestSaveInventoryOrder extends L2GameClientPacket
{
	private static final String _C__D0_24_REQUESTSAVEINVENTORYORDER = "[C] D0:24 RequestSaveInventoryOrder";
	
	private List<InventoryOrder> _order;
	
	/** client limit */
	private static final int LIMIT  = 125;
	
	/**
	 * @see com.l2jserver.gameserver.network.clientpackets.L2GameClientPacket#readImpl()
	 */
	@Override
	protected void readImpl()
	{
		int sz = readD();
		sz = Math.min(sz, LIMIT);
		_order = new ArrayList<InventoryOrder>(sz);
		for (int i = 0; i < sz; i++)
		{
			int objectId = readD();
			int order = readD();
			_order.add(new InventoryOrder(objectId, order));
		}
	}
	
	/**
	 * @see com.l2jserver.util.network.BaseRecievePacket.ClientBasePacket#runImpl()
	 */
	@Override
	protected void runImpl()
	{
		L2PcInstance player = this.getClient().getActiveChar();
		if (player != null)
		{
			Inventory inventory = player.getInventory();
			for (InventoryOrder order : _order)
			{
				L2ItemInstance item = inventory.getItemByObjectId(order.objectID);
				if (item != null && item.getLocation() == ItemLocation.INVENTORY)
				{
					item.setLocation(ItemLocation.INVENTORY, order.order);
				}
			}
		}
	}
		
	private static class InventoryOrder
	{
		int order;
		
		int objectID;
		
		/**
		 * 
		 */
		public InventoryOrder(int id, int ord)
		{
			objectID = id;
			order = ord;
		}
	}
	
	@Override
	protected boolean triggersOnActionRequest()
	{
		return false;
	}

	/**
	 * @see com.l2jserver.gameserver.BasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _C__D0_24_REQUESTSAVEINVENTORYORDER;
	}
}
