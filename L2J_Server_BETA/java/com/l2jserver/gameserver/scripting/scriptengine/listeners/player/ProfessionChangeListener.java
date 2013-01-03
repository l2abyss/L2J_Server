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
package com.l2jserver.gameserver.scripting.scriptengine.listeners.player;

import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.scripting.scriptengine.events.ProfessionChangeEvent;
import com.l2jserver.gameserver.scripting.scriptengine.impl.L2JListener;

/**
 * Listener for player profession change.<br>
 * Set player to null if you want to set a global listener for all players on server.
 * @author TheOne
 */
public abstract class ProfessionChangeListener extends L2JListener
{
	
	/**
	 * constructor
	 * @param player
	 */
	public ProfessionChangeListener(L2PcInstance player)
	{
		super.player = player;
		register();
	}
	
	/**
	 * Player's profession has changed
	 * @param event
	 */
	public abstract void professionChanged(ProfessionChangeEvent event);
	
	@Override
	public void register()
	{
		if (player != null)
		{
			player.addProfessionChangeListener(this);
			return;
		}
		L2PcInstance.addGlobalProfessionChangeListener(this);
	}
	
	@Override
	public void unregister()
	{
		if (player != null)
		{
			player.removeProfessionChangeListener(this);
			return;
		}
		L2PcInstance.removeGlobalProfessionChangeListener(this);
	}
}
