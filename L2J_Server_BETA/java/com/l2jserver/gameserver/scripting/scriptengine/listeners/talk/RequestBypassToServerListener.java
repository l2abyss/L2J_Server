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
package com.l2jserver.gameserver.scripting.scriptengine.listeners.talk;

import com.l2jserver.gameserver.network.clientpackets.RequestBypassToServer;
import com.l2jserver.gameserver.scripting.scriptengine.events.RequestBypassToServerEvent;
import com.l2jserver.gameserver.scripting.scriptengine.impl.L2JListener;

/**
 * @author UnAfraid
 */
public abstract class RequestBypassToServerListener extends L2JListener
{
	public RequestBypassToServerListener()
	{
		register();
	}
	
	/**
	 * @param event
	 */
	public abstract void onRequestBypassToServer(RequestBypassToServerEvent event);
	
	@Override
	public void register()
	{
		RequestBypassToServer.addBypassListener(this);
	}
	
	@Override
	public void unregister()
	{
		RequestBypassToServer.removeBypassListener(this);
	}
}