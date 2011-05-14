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
package com.l2jserver.gameserver.model;

/**
 * @author UnAfraid
 *
 */
public final class ItemHolder
{
	private final int _objectId;
	private final long _count;
	
	public ItemHolder(int objectId, long count)
	{
		_objectId = objectId;
		_count = count;
	}
	
	public int getObjectId()
	{
		return _objectId;
	}
	
	public long getCount()
	{
		return _count;
	}
	
	@Override
	public String toString()
	{
		return "[" + getClass().getSimpleName() + "]: objectId: " + _objectId + " count: " + _count;
	}
}
