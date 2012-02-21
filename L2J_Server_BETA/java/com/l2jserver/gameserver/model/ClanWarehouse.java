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
package com.l2jserver.gameserver.model;

import javolution.util.FastList;

import com.l2jserver.Config;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.itemcontainer.ItemContainer;
import com.l2jserver.gameserver.model.itemcontainer.Warehouse;
import com.l2jserver.gameserver.model.items.instance.L2ItemInstance;
import com.l2jserver.gameserver.model.items.instance.L2ItemInstance.ItemLocation;
import com.l2jserver.gameserver.scripting.scriptengine.listeners.clan.ClanWarehouseListener;

public final class ClanWarehouse extends Warehouse
{
	private L2Clan _clan;
	
	private FastList<ClanWarehouseListener> clanWarehouseListeners = new FastList<ClanWarehouseListener>().shared();
	
	public ClanWarehouse(L2Clan clan)
	{
		_clan = clan;
	}
	
	@Override
	public String getName()
	{
		return "ClanWarehouse";
	}
	
	@Override
	public int getOwnerId()
	{
		return _clan.getClanId();
	}
	
	@Override
	public L2PcInstance getOwner()
	{
		return _clan.getLeader().getPlayerInstance();
	}
	
	@Override
	public ItemLocation getBaseLocation()
	{
		return ItemLocation.CLANWH;
	}
	
	public String getLocationId()
	{
		return "0";
	}
	
	public int getLocationId(boolean dummy)
	{
		return 0;
	}
	
	public void setLocationId(L2PcInstance dummy)
	{
	}
	
	@Override
	public boolean validateCapacity(int slots)
	{
		return (_items.size() + slots <= Config.WAREHOUSE_SLOTS_CLAN);
	}
	
	@Override
	public L2ItemInstance addItem(String process, int itemId, long count, L2PcInstance actor, Object reference)
	{
		L2ItemInstance item = getItemByItemId(itemId);
		for (ClanWarehouseListener listener : clanWarehouseListeners)
		{
			if (!listener.onAddItem(process, item, actor))
			{
				return null;
			}
		}
		return super.addItem(process, item, actor, reference);
	}
	
	@Override
	public L2ItemInstance addItem(String process, L2ItemInstance item, L2PcInstance actor, Object reference)
	{
		for (ClanWarehouseListener listener : clanWarehouseListeners)
		{
			if (!listener.onAddItem(process, item, actor))
			{
				return null;
			}
		}
		return super.addItem(process, item, actor, reference);
	}
	
	@Override
	public L2ItemInstance destroyItem(String process, L2ItemInstance item, long count, L2PcInstance actor, Object reference)
	{
		for (ClanWarehouseListener listener : clanWarehouseListeners)
		{
			if (!listener.onDeleteItem(process, item, count, actor))
			{
				return null;
			}
		}
		return super.destroyItem(process, item, count, actor, reference);
	}
	
	@Override
	public L2ItemInstance transferItem(String process, int objectId, long count, ItemContainer target, L2PcInstance actor, Object reference)
	{
		L2ItemInstance sourceitem = getItemByObjectId(objectId);
		for (ClanWarehouseListener listener : clanWarehouseListeners)
		{
			if (!listener.onTransferItem(process, sourceitem, count, target, actor))
			{
				return null;
			}
		}
		return super.transferItem(process, objectId, count, target, actor, reference);
	}
	
	// Listeners
	/**
	 * Adds a clan warehouse listener
	 * @param listener
	 */
	public void addWarehouseListener(ClanWarehouseListener listener)
	{
		if (!clanWarehouseListeners.contains(listener))
		{
			clanWarehouseListeners.add(listener);
		}
	}
	
	/**
	 * Removes a clan warehouse listener
	 * @param listener
	 */
	public void removeWarehouseListener(ClanWarehouseListener listener)
	{
		clanWarehouseListeners.remove(listener);
	}
}
