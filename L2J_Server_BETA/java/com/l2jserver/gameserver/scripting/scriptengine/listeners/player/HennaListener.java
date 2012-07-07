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
import com.l2jserver.gameserver.scripting.scriptengine.events.HennaEvent;
import com.l2jserver.gameserver.scripting.scriptengine.impl.L2JListener;

/**
 * @author TheOne
 */
public abstract class HennaListener extends L2JListener
{
	public HennaListener()
	{
		register();
	}
	
	/**
	 * The given henna has just been added to the player
	 * @param event
	 * @return
	 */
	public abstract boolean onAddHenna(HennaEvent event);
	
	/**
	 * The given henna has just been removed from the player
	 * @param event
	 * @return
	 */
	public abstract boolean onRemoveHenna(HennaEvent event);
	
	@Override
	public void register()
	{
		L2PcInstance.addHennaListener(this);
	}
	
	@Override
	public void unregister()
	{
		L2PcInstance.removeHennaListener(this);
	}
}
