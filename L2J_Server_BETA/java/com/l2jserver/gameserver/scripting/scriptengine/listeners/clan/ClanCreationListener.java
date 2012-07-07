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
import com.l2jserver.gameserver.scripting.scriptengine.events.ClanCreationEvent;
import com.l2jserver.gameserver.scripting.scriptengine.events.ClanLevelUpEvent;
import com.l2jserver.gameserver.scripting.scriptengine.impl.L2JListener;

/**
 * This class notifies of clan creation and
 * @author TheOne
 */
public abstract class ClanCreationListener extends L2JListener
{
	public ClanCreationListener()
	{
		register();
	}
	
	/**
	 * Fired when a clan is created
	 * @param event
	 */
	public abstract void onClanCreate(ClanCreationEvent event);
	
	/**
	 * Fired when a clan levels up
	 * @param event
	 * @return
	 */
	public abstract boolean onClanLevelUp(ClanLevelUpEvent event);
	
	@Override
	public void register()
	{
		L2Clan.addClanCreationListener(this);
	}
	
	@Override
	public void unregister()
	{
		L2Clan.removeClanCreationListener(this);
	}
}
