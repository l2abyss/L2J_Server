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

import com.l2jserver.gameserver.network.clientpackets.Say2;
import com.l2jserver.gameserver.scripting.scriptengine.events.ChatEvent;
import com.l2jserver.gameserver.scripting.scriptengine.impl.L2JListener;

/**
 * Listener to intercept player chat.<br>
 * Could be useful to improve or customize the integrated chat filters (i.e.: make it dependent on who's sending the message and the chat type... for example GMs could be unfiltered?).<br>
 * See Say2.java
 * @author TheOne
 */
public abstract class ChatFilterListener extends L2JListener
{
	public ChatFilterListener()
	{
		register();
	}
	
	/**
	 * Allows for filtering the text
	 * @param event
	 * @return
	 */
	public abstract String onTalk(ChatEvent event);
	
	@Override
	public void register()
	{
		Say2.addChatFilterListener(this);
	}
	
	@Override
	public void unregister()
	{
		Say2.removeChatFilterListener(this);
	}
}
