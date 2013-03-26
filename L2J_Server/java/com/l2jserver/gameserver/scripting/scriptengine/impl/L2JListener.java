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
package com.l2jserver.gameserver.scripting.scriptengine.impl;

import java.util.logging.Logger;

import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;

/**
 * Convenience superclass for all the L2J listeners.<br>
 * Forces having the register() and unregister() methods in all its children.<br>
 * The register() method is called automatically when the listener is instanciated.<br>
 * These are 2 convenience methods to avoid having to register the listeners ourselves. This is particularly useful for our less advanced coders.
 * @author TheOne
 */
public abstract class L2JListener
{
	public static Logger log = Logger.getLogger(L2JListener.class.getName());
	
	public L2PcInstance player = null;
	
	/**
	 * Convenience method to add this listener in its proper place.<br>
	 * Called automatically by the superconstructor when the class is instanciated.
	 */
	public abstract void register();
	
	/**
	 * Convenience method to remove this listener
	 */
	public abstract void unregister();
	
	/**
	 * Returns the player attached to this listener
	 * @return
	 */
	public L2PcInstance getPlayer()
	{
		return player;
	}
}
