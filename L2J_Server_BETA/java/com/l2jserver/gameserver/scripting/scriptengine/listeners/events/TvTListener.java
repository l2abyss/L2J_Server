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
package com.l2jserver.gameserver.scripting.scriptengine.listeners.events;

import com.l2jserver.gameserver.model.entity.TvTEvent;
import com.l2jserver.gameserver.scripting.scriptengine.events.TvtKillEvent;
import com.l2jserver.gameserver.scripting.scriptengine.impl.L2JListener;

/**
 * Notifies when a TvT event starts
 * @author TheOne
 */
public abstract class TvTListener extends L2JListener
{
	public TvTListener()
	{
		register();
	}
	
	/**
	 * Fired when a TvT event starts
	 */
	public abstract void onBegin();
	
	/**
	 * A player has been killed
	 * @param event
	 */
	public abstract void onKill(TvtKillEvent event);
	
	/**
	 * Fired when a TvT event ends
	 */
	public abstract void onEnd();
	
	/**
	 * Fired when TvT registration starts
	 */
	public abstract void onRegistrationStart();
	
	@Override
	public void register()
	{
		TvTEvent.addTvTListener(this);
	}
	
	@Override
	public void unregister()
	{
		TvTEvent.removeTvtListener(this);
	}
}
