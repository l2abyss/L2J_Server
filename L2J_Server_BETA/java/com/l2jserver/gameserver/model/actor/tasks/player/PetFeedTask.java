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

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2jserver.gameserver.handler.IItemHandler;
import com.l2jserver.gameserver.handler.ItemHandler;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.items.instance.L2ItemInstance;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;

/**
 * Task dedicated for feeding player's pet.
 * @author UnAfraid
 */
public class PetFeedTask implements Runnable
{
	private static final Logger _log = Logger.getLogger(PetFeedTask.class.getName());
	
	private final L2PcInstance _player;
	
	public PetFeedTask(L2PcInstance player)
	{
		_player = player;
	}
	
	@Override
	public void run()
	{
		if (_player != null)
		{
			try
			{
				if (!_player.isMounted() || (_player.getMountNpcId() == 0))
				{
					_player.stopFeed();
					return;
				}
				
				if (_player.getCurrentFeed() > _player.getFeedConsume())
				{
					// eat
					_player.setCurrentFeed(_player.getCurrentFeed() - _player.getFeedConsume());
				}
				else
				{
					// go back to pet control item, or simply said, unsummon it
					_player.setCurrentFeed(0);
					_player.stopFeed();
					_player.dismount();
					_player.sendPacket(SystemMessageId.OUT_OF_FEED_MOUNT_CANCELED);
				}
				
				List<Integer> foodIds = _player.getPetData(_player.getMountNpcId()).getFood();
				if (foodIds.isEmpty())
				{
					return;
				}
				L2ItemInstance food = null;
				for (int id : foodIds)
				{
					// TODO: possibly pet inv?
					food = _player.getInventory().getItemByItemId(id);
					if (food != null)
					{
						break;
					}
				}
				
				if ((food != null) && _player.isHungry())
				{
					IItemHandler handler = ItemHandler.getInstance().getHandler(food.getEtcItem());
					if (handler != null)
					{
						handler.useItem(_player, food, false);
						SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.PET_TOOK_S1_BECAUSE_HE_WAS_HUNGRY);
						sm.addItemName(food.getItemId());
						_player.sendPacket(sm);
					}
				}
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "Mounted Pet [NpcId: " + _player.getMountNpcId() + "] a feed task error has occurred", e);
			}
		}
	}
}
