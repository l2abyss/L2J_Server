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

import java.util.concurrent.atomic.AtomicInteger;

import com.l2jserver.gameserver.model.Location;
import com.l2jserver.gameserver.model.interfaces.IPositionable;

/**
 * @author Unknown, UnAfraid
 */
public class Point3D implements IPositionable
{
	private final AtomicInteger _x = new AtomicInteger();
	private final AtomicInteger _y = new AtomicInteger();
	private final AtomicInteger _z = new AtomicInteger();
	private final AtomicInteger _heading = new AtomicInteger();
	private final AtomicInteger _instanceId = new AtomicInteger();
	
	public Point3D(int x, int y, int z)
	{
		_x.set(x);
		_y.set(y);
		_z.set(z);
	}
	
	public Point3D(int x, int y, int z, int heading)
	{
		_x.set(x);
		_y.set(y);
		_z.set(z);
		_heading.set(heading);
	}
	
	public Point3D(int x, int y, int z, int heading, int instanceId)
	{
		_x.set(x);
		_y.set(y);
		_z.set(z);
		_heading.set(heading);
		_instanceId.set(instanceId);
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
	
	@Override
	public int getY()
	{
		return _y.get();
	}
	
	@Override
	public int getZ()
	{
		return _z.get();
	}
	
	@Override
	public int getHeading()
	{
		return _heading.get();
	}
	
	@Override
	public int getInstanceId()
	{
		return _instanceId.get();
	}
	
	@Override
	public Location getLocation()
	{
		return new Location(getX(), getY(), getZ(), getHeading(), getInstanceId());
	}
	
	@Override
	public void setX(int x)
	{
		_x.set(x);
	}
	
	@Override
	public void setY(int y)
	{
		_y.set(y);
	}
	
	@Override
	public void setZ(int z)
	{
		_z.set(z);
	}
	
	@Override
	public void setHeading(int heading)
	{
		_heading.set(heading);
	}
	
	@Override
	public void setInstanceId(int instanceId)
	{
		_instanceId.set(instanceId);
	}
	
	@Override
	public void setLocation(Location loc)
	{
		_x.set(loc.getX());
		_y.set(loc.getY());
		_z.set(loc.getZ());
		_heading.set(loc.getHeading());
		_instanceId.set(loc.getInstanceId());
	}
	
	@Override
	public void setXYZ(int x, int y, int z)
	{
		_x.set(x);
		_y.set(y);
		_z.set(z);
	}
	
	public final Point3D getWorldPosition()
	{
		return this;
	}
	
	@Override
	public String toString()
	{
		return "(" + _x + ", " + _y + ", " + _z + ")";
	}
}
