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
import com.l2jserver.gameserver.network.clientpackets.EnterWorld;
import com.l2jserver.gameserver.scripting.scriptengine.impl.L2JListener;

/**
 * Gets triggered when a L2PcInstance is spawned in the world
 * @author TheOne
 */
public abstract class PlayerSpawnListener extends L2JListener
{
	public PlayerSpawnListener()
	{
		register();
	}
	
	/**
	 * Triggered when a player is spawned
	 * @param player
	 */
	public abstract void onSpawn(L2PcInstance player);
	
	@Override
	public void register()
	{
		EnterWorld.addSpawnListener(this);
		
	}
	
	@Override
	public void unregister()
	{
		EnterWorld.removeSpawnListener(this);
	}
}
