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
package com.l2jserver.gameserver.scripting.scriptengine.listeners.character;

import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.scripting.scriptengine.events.AttackEvent;
import com.l2jserver.gameserver.scripting.scriptengine.impl.L2JListener;

/**
 * @author TheOne
 */
public abstract class AttackListener extends L2JListener
{
	private L2Character _character = null;
	
	/**
	 * Constructor.<br>
	 * AttackListener is not a static listener, it needs to be registered for each L2Character you wish to use it for.<br>
	 * It will work for both NPCs and Players
	 * @param character
	 */
	public AttackListener(L2Character character)
	{
		_character = character;
		register();
	}
	
	/**
	 * The player just attacked another character
	 * @param event
	 * @return
	 */
	public abstract boolean onAttack(AttackEvent event);
	
	/**
	 * The player was just attacked by another character
	 * @param event
	 * @return
	 */
	public abstract boolean isAttacked(AttackEvent event);
	
	@Override
	public void register()
	{
		_character.addAttackListener(this);
	}
	
	@Override
	public void unregister()
	{
		_character.removeAttackListener(this);
	}
	
	/**
	 * returns the L2Character this listener is attached to
	 * @return
	 */
	public L2Character getCharacter()
	{
		return _character;
	}
}
