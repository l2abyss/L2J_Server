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

import java.util.logging.Logger;

import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.events.listeners.IDamageDealtEventListener;
import com.l2jserver.gameserver.model.actor.events.listeners.IDamageReceivedEventListener;
import com.l2jserver.gameserver.model.actor.events.listeners.IDeathEventListener;
import com.l2jserver.gameserver.model.actor.events.listeners.ITeleportedEventListener;
import com.l2jserver.gameserver.model.skills.L2Skill;

/**
 * @author UnAfraid
 */
public class CharEvents extends AbstractCharEvents
{
	protected static final Logger _log = Logger.getLogger(CharEvents.class.getName());
	
	private final L2Character _activeChar;
	
	public CharEvents(L2Character activeChar)
	{
		_activeChar = activeChar;
	}
	
	/**
	 * Fired whenever current char dies.
	 * @param killer
	 * @return {@code true} if current char can die, {@code false} otherwise.
	 */
	public boolean onDeath(L2Character killer)
	{
		if (!getActingPlayer().fireDeathListeners(killer))
		{
			return false;
		}
		
		// Notify event listeners.
		if (hasEventListeners())
		{
			for (IDeathEventListener listener : getEventListeners(IDeathEventListener.class))
			{
				listener.onDeath(getActingPlayer(), killer);
			}
		}
		return true;
	}
	
	/**
	 * Fired whenever current char deal damage.
	 * @param damage
	 * @param target
	 * @param skill
	 * @param crit
	 */
	public void onDamageDealt(double damage, L2Character target, L2Skill skill, boolean crit)
	{
		if (hasEventListeners())
		{
			for (IDamageDealtEventListener listener : getEventListeners(IDamageDealtEventListener.class))
			{
				listener.onDamageDealtEvent(getActingPlayer(), target, damage, skill, crit);
			}
		}
	}
	
	/**
	 * Fired whenever current char receive damage.
	 * @param damage
	 * @param attacker
	 * @param skill
	 * @param crit
	 */
	public void onDamageReceived(double damage, L2Character attacker, L2Skill skill, boolean crit)
	{
		if (hasEventListeners())
		{
			for (IDamageReceivedEventListener listener : getEventListeners(IDamageReceivedEventListener.class))
			{
				listener.onDamageReceivedEvent(attacker, getActingPlayer(), damage, skill, crit);
			}
		}
	}
	
	/**
	 * Fired whenever current char is teleported.
	 */
	public void onTeleported()
	{
		if (hasEventListeners())
		{
			for (ITeleportedEventListener listener : getEventListeners(ITeleportedEventListener.class))
			{
				listener.onTeleported(getActingPlayer());
			}
		}
	}
	
	public boolean onExperienceReceived(long exp)
	{
		return true;
	}
	
	/**
	 * @return current char.
	 */
	public L2Character getActingPlayer()
	{
		return _activeChar;
	}
}
