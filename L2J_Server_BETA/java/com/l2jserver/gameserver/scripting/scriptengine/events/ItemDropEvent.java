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
import com.l2jserver.gameserver.model.items.instance.L2ItemInstance;
import com.l2jserver.gameserver.scripting.scriptengine.events.impl.L2Event;

/**
 * @author TheOne
 *
 */
public class ItemDropEvent implements L2Event
{
	private L2ItemInstance item;
	private L2PcInstance dropper;
	private int x;
	private int y;
	private int z;
	
	/**
	 * @return the item
	 */
	public L2ItemInstance getItem()
	{
		return item;
	}
	/**
	 * @param item the item to set
	 */
	public void setItem(L2ItemInstance item)
	{
		this.item = item;
	}
	/**
	 * @return the dropper
	 */
	public L2PcInstance getDropper()
	{
		return dropper;
	}
	/**
	 * @param dropper the dropper to set
	 */
	public void setDropper(L2PcInstance dropper)
	{
		this.dropper = dropper;
	}
	/**
	 * @return the x
	 */
	public int getX()
	{
		return x;
	}
	/**
	 * @param x the x to set
	 */
	public void setX(int x)
	{
		this.x = x;
	}
	/**
	 * @return the y
	 */
	public int getY()
	{
		return y;
	}
	/**
	 * @param y the y to set
	 */
	public void setY(int y)
	{
		this.y = y;
	}
	/**
	 * @return the z
	 */
	public int getZ()
	{
		return z;
	}
	/**
	 * @param z the z to set
	 */
	public void setZ(int z)
	{
		this.z = z;
	}
}
