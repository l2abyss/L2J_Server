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

import com.l2jserver.gameserver.model.L2Clan;
import com.l2jserver.gameserver.scripting.scriptengine.events.impl.L2Event;
import com.l2jserver.gameserver.scripting.scriptengine.impl.L2Script.EventStage;

/**
 * @author TheOne
 */
public class ClanWarEvent implements L2Event
{
	private L2Clan clan1;
	private L2Clan clan2;
	private EventStage stage;
	
	/**
	 * @return the clan1
	 */
	public L2Clan getClan1()
	{
		return clan1;
	}
	
	/**
	 * @param clan1 the clan1 to set
	 */
	public void setClan1(L2Clan clan1)
	{
		this.clan1 = clan1;
	}
	
	/**
	 * @return the clan2
	 */
	public L2Clan getClan2()
	{
		return clan2;
	}
	
	/**
	 * @param clan2 the clan2 to set
	 */
	public void setClan2(L2Clan clan2)
	{
		this.clan2 = clan2;
	}
	
	/**
	 * @return the stage
	 */
	public EventStage getStage()
	{
		return stage;
	}
	
	/**
	 * @param stage the stage to set
	 */
	public void setStage(EventStage stage)
	{
		this.stage = stage;
	}
}
