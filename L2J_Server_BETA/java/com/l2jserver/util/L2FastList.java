/*
 * Copyright (C) 2004-2014 L2J Server
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

/**
 * A custom version of {@code FastList} with constructors that allow the constructed {@code FastList} to be shared without calling {@link FastList#shared()} method. <br>
 * @author Julian
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
}
