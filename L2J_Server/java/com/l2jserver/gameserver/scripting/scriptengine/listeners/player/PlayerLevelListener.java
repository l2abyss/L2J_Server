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
import com.l2jserver.gameserver.model.actor.stat.PcStat;
import com.l2jserver.gameserver.scripting.scriptengine.events.PlayerLevelChangeEvent;
import com.l2jserver.gameserver.scripting.scriptengine.impl.L2JListener;

/**
 * Listens for player level changes<br>
 * If you wish to have a global listener for all the players logged in, set the L2PcInstance to null.<br>
 * @author TheOne
 */
public abstract class PlayerLevelListener extends L2JListener
{
	
	/**
	 * constructor
	 * @param player
	 */
	public PlayerLevelListener(L2PcInstance player)
	{
		super.player = player;
		register();
	}
	
	/**
	 * The player's level has changed
	 * @param event
	 */
	public abstract void levelChanged(PlayerLevelChangeEvent event);
	
	@Override
	public void register()
	{
		if (player == null)
		{
			PcStat.addGlobalLevelListener(this);
			return;
		}
		player.getStat().addLevelListener(this);
		
	}
	
	@Override
	public void unregister()
	{
		if (player == null)
		{
			PcStat.removeGlobalLevelListener(this);
			return;
		}
		player.getStat().removeLevelListener(this);
	}
}
