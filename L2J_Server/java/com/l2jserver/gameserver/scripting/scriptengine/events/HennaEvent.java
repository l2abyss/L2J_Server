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
package com.l2jserver.gameserver.scripting.scriptengine.events;

import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.items.L2Henna;
import com.l2jserver.gameserver.scripting.scriptengine.events.impl.L2Event;

/**
 * @author TheOne
 */
public class HennaEvent implements L2Event
{
	private L2PcInstance player;
	private L2Henna henna;
	private boolean add;
	
	/**
	 * @return the player
	 */
	public L2PcInstance getPlayer()
	{
		return player;
	}
	
	/**
	 * @param p the player to set
	 */
	public void setPlayer(L2PcInstance p)
	{
		player = p;
	}
	
	/**
	 * @return the henna
	 */
	public L2Henna getHenna()
	{
		return henna;
	}
	
	/**
	 * @param h
	 */
	public void setHenna(L2Henna h)
	{
		henna = h;
	}
	
	/**
	 * @return the add
	 */
	public boolean isAdd()
	{
		return add;
	}
	
	/**
	 * @param add the add to set
	 */
	public void setAdd(boolean add)
	{
		this.add = add;
	}
}
