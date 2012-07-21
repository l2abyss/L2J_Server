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
package com.l2jserver.gameserver.scripting.scriptengine.listeners.clan;

import com.l2jserver.gameserver.model.L2Clan;
import com.l2jserver.gameserver.model.itemcontainer.ClanWarehouse;
import com.l2jserver.gameserver.scripting.scriptengine.events.ClanWarehouseAddItemEvent;
import com.l2jserver.gameserver.scripting.scriptengine.events.ClanWarehouseDeleteItemEvent;
import com.l2jserver.gameserver.scripting.scriptengine.events.ClanWarehouseTransferEvent;
import com.l2jserver.gameserver.scripting.scriptengine.impl.L2JListener;

/**
 * @author TheOne
 */
public abstract class ClanWarehouseListener extends L2JListener
{
	private final ClanWarehouse _clanWarehouse;
	
	public ClanWarehouseListener(L2Clan clan)
	{
		_clanWarehouse = (ClanWarehouse) clan.getWarehouse();
		register();
	}
	
	/**
	 * An item was just added
	 * @param event
	 * @return
	 */
	public abstract boolean onAddItem(ClanWarehouseAddItemEvent event);
	
	/**
	 * An item was just deleted
	 * @param event
	 * @return
	 */
	public abstract boolean onDeleteItem(ClanWarehouseDeleteItemEvent event);
	
	/**
	 * An item was just transfered
	 * @param event
	 * @return
	 */
	public abstract boolean onTransferItem(ClanWarehouseTransferEvent event);
	
	@Override
	public void register()
	{
		_clanWarehouse.addWarehouseListener(this);
	}
	
	@Override
	public void unregister()
	{
		_clanWarehouse.removeWarehouseListener(this);
	}
	
	/**
	 * Returns the clan warehouse attached to this listener
	 * @return
	 */
	public ClanWarehouse getWarehouse()
	{
		return _clanWarehouse;
	}
}
