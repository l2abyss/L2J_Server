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
package com.l2jserver.gameserver.model.interfaces;

import com.l2jserver.gameserver.model.Location;

/**
 * Positionable objects interface.
 * @author Zoey76
 */
public interface IPositionable
{
	public int getX();
	
	public int getY();
	
	public int getZ();
	
	public int getHeading();
	
	public int getInstanceId();
	
	public Location getLocation();
	
	public void setX(int x);
	
	public void setY(int y);
	
	public void setZ(int z);
	
	public void setHeading(int heading);
	
	public void setInstanceId(int instanceId);
	
	public void setLocation(Location loc);
}