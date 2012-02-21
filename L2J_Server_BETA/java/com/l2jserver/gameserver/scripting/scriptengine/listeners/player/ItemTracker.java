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
package com.l2jserver.gameserver.scripting.scriptengine.listeners.player;

import java.util.List;

import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.itemcontainer.ItemContainer;
import com.l2jserver.gameserver.model.itemcontainer.PcInventory;
import com.l2jserver.gameserver.model.items.instance.L2ItemInstance;
import com.l2jserver.gameserver.scripting.scriptengine.impl.L2JListener;

/**
 * This listener allows you to track items with certain item ids.<br>
 * This can be very useful if you want to make a script which will track all high value items and will log their movements/creation/drop/etc.<br>
 * It can be a great tool to catch hackers on your server ;)
 * @author TheOne
 */
public abstract class ItemTracker extends L2JListener
{
	private List<Integer> _itemIds;
	
	public ItemTracker(List<Integer> itemIds)
	{
		_itemIds = itemIds;
		register();
	}
	
	/**
	 * The item has been dropped
	 * @param item
	 * @param player
	 */
	public abstract void onDrop(L2ItemInstance item, L2PcInstance player);
	
	/**
	 * The item has been added to the inventory
	 * @param item
	 * @param player
	 */
	public abstract void onAddToInventory(L2ItemInstance item, L2PcInstance player);
	
	/**
	 * Notifies when the item is destroyed
	 * @param item
	 * @param player
	 */
	public abstract void onDestroy(L2ItemInstance item, L2PcInstance player);
	
	/**
	 * Notifies when the item is transfered or traded
	 * @param item
	 * @param player
	 * @param target
	 */
	public abstract void onTransfer(L2ItemInstance item, L2PcInstance player, ItemContainer target);
	
	@Override
	public void register()
	{
		PcInventory.addItemTracker(this);
	}
	
	@Override
	public void unregister()
	{
		PcInventory.removeItemTracker(this);
	}
	
	/**
	 * Checks if this item is tracked
	 * @param itemId
	 * @return
	 */
	public boolean containsItemId(int itemId)
	{
		return _itemIds.contains(itemId);
	}
}
