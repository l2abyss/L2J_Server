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
package com.l2jserver.util;

import java.util.HashMap;
import java.util.Map;

import com.l2jserver.gameserver.model.IL2EntryProcedure;
import com.l2jserver.gameserver.model.IL2Procedure;

/**
 * A custom version of HashMap: Extension for iterating without using temporary collection<br>
 * Note that this implementation is not synchronized. If multiple threads access a hash map concurrently, and at least one of the threads modifies the map structurally, it must be synchronized externally. This is typically accomplished by synchronizing on some object that naturally encapsulates the
 * map. If no such object exists, the map should be "wrapped" using the {@link L2FastMap}. This is best done at creation time, to prevent accidental unsynchronized access.
 * @author UnAfraid
 * @param <K>
 * @param <V>
 */
public class L2HashMap<K, V> extends HashMap<K, V>
{
	private static final long serialVersionUID = 8503855490858805336L;
	
	private static final float DEFAULT_LOAD_FACTOR = 0.75f;
	
	public L2HashMap()
	{
		super();
	}
	
	public L2HashMap(Map<? extends K, ? extends V> map)
	{
		super(map);
	}
	
	public L2HashMap(int initialCapacity)
	{
		this(initialCapacity, DEFAULT_LOAD_FACTOR);
	}
	
	public L2HashMap(int initialCapacity, float loadFactor)
	{
		super(initialCapacity, loadFactor);
	}
	
	/**
	 * Public method that iterate entire collection.<br>
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