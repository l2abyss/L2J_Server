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

import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.items.instance.L2ItemInstance;
import com.l2jserver.gameserver.scripting.scriptengine.impl.L2JListener;

/**
 * Global listener for items dropped by players
 * @author TheOne
 */
public abstract class DropListener extends L2JListener
{
	public DropListener()
	{
		register();
	}
	
	/**
	 * The item was dropped
	 * @param item
	 * @param dropper
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	public abstract boolean onDrop(L2ItemInstance item, L2PcInstance dropper, int x, int y, int z);
	
	/**
	 * The item was picked up
	 * @param item
	 * @param picker
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	public abstract boolean onPickup(L2ItemInstance item, L2PcInstance picker, int x, int y, int z);
	
	@Override
	public void register()
	{
		L2ItemInstance.addDropListener(this);
	}
	
	@Override
	public void unregister()
	{
		L2ItemInstance.removeDropListener(this);
	}
}
