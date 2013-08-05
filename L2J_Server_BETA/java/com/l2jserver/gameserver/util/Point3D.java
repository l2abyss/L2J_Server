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
package com.l2jserver.gameserver.util;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

import com.l2jserver.gameserver.model.Location;
import com.l2jserver.gameserver.model.interfaces.IPositionable;

/**
 * @author Unknown, UnAfraid
 */
public class Point3D implements Serializable, IPositionable
{
	private static final long serialVersionUID = 4638345252031872576L;
	
	private final AtomicInteger _x = new AtomicInteger();
	private final AtomicInteger _y = new AtomicInteger();
	private final AtomicInteger _z = new AtomicInteger();
	
	public Point3D(int x, int y, int z)
	{
		_x.set(x);
		_y.set(y);
		_z.set(z);
	}
	
	public boolean equals(int x, int y, int z)
	{
		return (getX() == x) && (getY() == y) && (getZ() == z);
	}
	
	@Override
	public int getX()
	{
		return _x.get();
	}
	
	public void setX(int x)
	{
		_x.set(x);
	}
	
	@Override
	public int getY()
	{
		return _y.get();
	}
	
	public void setY(int y)
	{
		_y.set(y);
	}
	
	@Override
	public int getZ()
	{
		return _z.get();
	}
	
	public void setZ(int z)
	{
		_z.set(z);
	}
	
	public void setXYZ(int x, int y, int z)
	{
		_x.set(x);
		_y.set(y);
		_z.set(z);
	}
	
	@Override
	public Location getLocation()
	{
		return new Location(getX(), getY(), getZ());
	}
	
	@Override
	public String toString()
	{
		return "(" + _x + ", " + _y + ", " + _z + ")";
	}
	
	@Override
	public int hashCode()
	{
		return getX() ^ getY() ^ getZ();
	}
	
	@Override
	public boolean equals(Object o)
	{
		if (this == o)
		{
			return true;
		}
		if (o instanceof Point3D)
		{
			final Point3D point3D = (Point3D) o;
			return (point3D.getX() == getX()) && (point3D.getY() == getY()) && (point3D.getZ() == getZ());
		}
		return false;
	}
}
