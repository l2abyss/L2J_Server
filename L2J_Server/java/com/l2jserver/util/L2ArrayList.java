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

import java.util.ArrayList;
import java.util.Collection;

import com.l2jserver.gameserver.model.interfaces.IL2Procedure;

/**
 * A custom version of ArrayList: Extension for iterating without using temporary collection<br>
 * Note that this implementation is not synchronized. If multiple threads access a array list concurrently, and at least one of the threads modifies the list structurally, it must be synchronized externally. This is typically accomplished by synchronizing on some object that naturally encapsulates
 * the list. If no such object exists, the list should be "wrapped" using the {@link L2FastList}. This is best done at creation time, to prevent accidental unsynchronized access.
 * @author UnAfraid
 * @param <T>
 */
public class L2ArrayList<T> extends ArrayList<T>
{
	private static final long serialVersionUID = 8354641653178203420L;
	
	public L2ArrayList()
	{
		super();
	}
	
	public L2ArrayList(Collection<? extends T> c)
	{
		super(c);
	}
	
	public L2ArrayList(int initialCapacity)
	{
		super(initialCapacity);
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
