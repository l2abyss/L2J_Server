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

import com.l2jserver.gameserver.network.clientpackets.DlgAnswer;
import com.l2jserver.gameserver.scripting.scriptengine.events.DlgAnswerEvent;
import com.l2jserver.gameserver.scripting.scriptengine.impl.L2JListener;

/**
 * @author UnAfraid
 */
public abstract class DlgAnswerListener extends L2JListener
{
	private final int _messageId;
	
	public DlgAnswerListener(int messageId)
	{
		_messageId = messageId;
		register();
	}
	
	public int getMessageId()
	{
		return _messageId;
	}
	
	/**
	 * @param event
	 */
	public abstract void onDlgAnswer(DlgAnswerEvent event);
	
	@Override
	public void register()
	{
		DlgAnswer.addDlgAnswerListener(this);
	}
	
	@Override
	public void unregister()
	{
		DlgAnswer.removeDlgAnswerListener(this);
	}
}
