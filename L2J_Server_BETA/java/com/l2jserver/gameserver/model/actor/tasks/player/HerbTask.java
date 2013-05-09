/*
 * Copyright (C) 2004-2013 L2J Server
 * 
 * This file is part of L2J Server.
 * 
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.l2jserver.gameserver.model.actor.tasks.player;

import com.l2jserver.gameserver.model.L2Object;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;

/**
 * Task dedicated to apply herbs on player.
 * @author UnAfraid
 */
public class HerbTask implements Runnable
{
	private final L2PcInstance _player;
	private final String _process;
	private final int _itemId;
	private final long _count;
	private final L2Object _reference;
	private final boolean _sendMessage;
	
	public HerbTask(L2PcInstance player, String process, int itemId, long count, L2Object reference, boolean sendMessage)
	{
		_player = player;
		_process = process;
		_itemId = itemId;
		_count = count;
		_reference = reference;
		_sendMessage = sendMessage;
	}
	
	@Override
	public void run()
	{
		if (_player != null)
		{
			_player.addItem(_process, _itemId, _count, _reference, _sendMessage);
		}
	}
}