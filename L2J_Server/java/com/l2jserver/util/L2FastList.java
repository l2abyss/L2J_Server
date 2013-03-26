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
package com.l2jserver.util;

import java.util.Collection;

import javolution.util.FastList;

import com.l2jserver.gameserver.model.interfaces.IL2Procedure;

/**
 * A custom version of FastList with extension for iterating without using temporary collection<br>
 * It's provide synchronization lock when iterating if needed<br>
 * <br>
 * @author Julian
 * @version 1.0.1 (2008-02-07)<br>
 *          1.0.0 - Initial version.<br>
 *          1.0.1 - Made forEachP() final.<br>
 * @author UnAfraid
 * @version 1.0.2 (20012-08-19)<br>
 *          1.0.2 - Using IL2Procedure instead of IForEach.
 * @param <T>
 */
public class L2FastList<T> extends FastList<T>
{
	private static final long serialVersionUID = 8354641653178203420L;
	
	public L2FastList()
	{
		this(false);
	}
	
	public L2FastList(int initialCapacity)
	{
		this(initialCapacity, false);
	}
	
	public L2FastList(Collection<? extends T> c)
	{
		this(c, false);
	}
	
	public L2FastList(boolean shared)
	{
		super();
		if (shared)
		{
			shared();
		}
	}
	
	public L2FastList(int initialCapacity, boolean shared)
	{
		super(initialCapacity);
		if (shared)
		{
			shared();
		}
	}
	
	public L2FastList(Collection<? extends T> c, boolean shared)
	{
		super(c);
		if (shared)
		{
			shared();
		}
	}
	
	/**
	 * Public method that iterate entire collection.<br>
	 * <br>
	 * @param proc - a class method that must be executed on every element of collection.<br>
	 * @return - returns true if entire collection is iterated, false if it`s been interrupted by<br>
	 *         check method (IL2Procedure.execute(T))<br>
	 */
	public boolean executeForEach(IL2Procedure<T> proc)
	{
		for (T e : this)
		{
			if (!proc.execute(e))
			{
				return false;
			}
		}
		return true;
	}
}
