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
package com.l2jserver.gameserver.scripting.scriptengine.listeners.character;

import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.scripting.scriptengine.events.DeathEvent;
import com.l2jserver.gameserver.scripting.scriptengine.impl.L2JListener;

/**
 * Death/Kill listener<br>
 * Works for NPCs and Players
 * @author TheOne
 */
public abstract class DeathListener extends L2JListener
{
	private L2Character _character = null;
	private boolean _isGlobal = false;
	
	/**
	 * constructor To have a global listener, set character to null
	 * @param character
	 */
	public DeathListener(L2Character character)
	{
		if (character == null)
		{
			_isGlobal = true;
		}
		_character = character;
		register();
	}
	
	/**
	 * The character just killed the target<br>
	 * If you use this listener as global, use: onDeathGlobal()
	 * @param event
	 * @return
	 */
	public abstract boolean onKill(DeathEvent event);
	
	/**
	 * The character was just killed by the target<br>
	 * If you use this listener as global, use: onDeathGlobal()
	 * @param event
	 * @return
	 */
	public abstract boolean onDeath(DeathEvent event);
	
	/**
	 * Returns the character
	 * @return
	 */
	public L2Character getCharacter()
	{
		return _character;
	}
	
	@Override
	public void register()
	{
		if (_isGlobal)
		{
			L2Character.addGlobalDeathListener(this);
		}
		else
		{
			_character.addDeathListener(this);
		}
	}
	
	@Override
	public void unregister()
	{
		if (_isGlobal)
		{
			L2Character.removeGlobalDeathListener(this);
		}
		else
		{
			_character.removeDeathListener(this);
		}
	}
}
