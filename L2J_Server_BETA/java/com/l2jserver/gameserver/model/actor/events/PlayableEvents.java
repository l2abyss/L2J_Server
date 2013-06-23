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
package com.l2jserver.gameserver.model.actor.events;

import java.util.logging.Level;

import com.l2jserver.gameserver.model.actor.L2Playable;
import com.l2jserver.gameserver.model.actor.events.listeners.IExperienceReceivedEventListener;

/**
 * @author UnAfraid
 */
public class PlayableEvents extends CharEvents
{
	public PlayableEvents(L2Playable activeChar)
	{
		super(activeChar);
	}
	
	/**
	 * Fired whenever current char receives any exp.
	 * @param exp
	 * @return {@code true} if experience can be received, {@code false} otherwise.
	 */
	public boolean onExperienceReceived(long exp)
	{
		if (hasListeners())
		{
			for (IExperienceReceivedEventListener listener : getEventListeners(IExperienceReceivedEventListener.class))
			{
				try
				{
					if (!listener.onExperienceReceived(getActingPlayer(), exp))
					{
						return false;
					}
				}
				catch (Exception e)
				{
					_log.log(Level.WARNING, getClass().getSimpleName() + ": Exception caught: ", e);
				}
			}
		}
		return true;
	}
	
	@Override
	public L2Playable getActingPlayer()
	{
		return (L2Playable) super.getActingPlayer();
	}
}
