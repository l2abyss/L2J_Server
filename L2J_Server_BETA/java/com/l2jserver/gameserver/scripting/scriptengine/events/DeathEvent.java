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

import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.scripting.scriptengine.events.impl.L2Event;

/**
 * @author TheOne
 */
public class DeathEvent implements L2Event
{
	private L2Character victim;
	private L2Character killer;
	
	/**
	 * @return the victim
	 */
	public L2Character getVictim()
	{
		return victim;
	}
	
	/**
	 * @param victim the victim to set
	 */
	public void setVictim(L2Character victim)
	{
		this.victim = victim;
	}
	
	/**
	 * @return the killer
	 */
	public L2Character getKiller()
	{
		return killer;
	}
	
	/**
	 * @param killer the killer to set
	 */
	public void setKiller(L2Character killer)
	{
		this.killer = killer;
	}
}
