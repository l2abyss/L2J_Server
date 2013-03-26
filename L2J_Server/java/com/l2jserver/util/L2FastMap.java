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

import java.util.Map;

import javolution.util.FastMap;

import com.l2jserver.gameserver.model.interfaces.IL2EntryProcedure;
import com.l2jserver.gameserver.model.interfaces.IL2Procedure;

/**
 * A custom version of FastMap with extension for iterating without using temporary collection<br>
 * It's provide synchronization lock when iterating if needed<br>
 * @author Julian
 * @version 1.0.1 (2008-02-07)<br>
 *          Changes:<br>
 *          1.0.0 - Initial version.<br>
 *          1.0.1 - Made forEachP() final.<br>
 * @author UnAfraid
 * @version 1.0.2 (2012-08-19)<br>
 *          1.0.2 - Using IL2Procedure instead of I2ForEachKey/Value<br>
 * @param <K>
 * @param <V>
 */
public class L2FastMap<K, V> extends FastMap<K, V>
{
	private static final long serialVersionUID = 8503855490858805336L;
	
	public L2FastMap()
	{
		this(false);
	}
	
	public L2FastMap(Map<? extends K, ? extends V> map)
	{
		this(map, false);
	}
	
	public L2FastMap(int initialCapacity)
	{
		this(initialCapacity, false);
	}
	
	public L2FastMap(boolean shared)
	{
		super();
		if (shared)
		{
			shared();
		}
	}
	
	public L2FastMap(Map<? extends K, ? extends V> map, boolean shared)
	{
		super(map);
		if (shared)
		{
			shared();
		}
	}
	
	public L2FastMap(int initialCapacity, boolean shared)
	{
		super(initialCapacity);
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
	 *         check method (IL2EntryProcedure.execute())<br>
	 */
	public boolean executeForEachEntry(IL2EntryProcedure<K, V> proc)
	{
		for (Map.Entry<K, V> e : entrySet())
		{
			if (!proc.execute(e.getKey(), e.getValue()))
			{
				return false;
			}
		}
		return true;
	}
	
	public boolean executeForEachKey(IL2Procedure<K> proc)
	{
		for (K k : keySet())
		{
			if (!proc.execute(k))
			{
				return false;
			}
		}
		return true;
	}
	
	public boolean executeForEachValue(IL2Procedure<V> proc)
	{
		for (V v : values())
		{
			if (!proc.execute(v))
			{
				return false;
			}
		}
		return true;
	}
}
