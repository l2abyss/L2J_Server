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
package com.l2jserver.gameserver.model.actor.position;

import java.util.logging.Logger;

import com.l2jserver.gameserver.model.L2Object;
import com.l2jserver.gameserver.model.L2World;
import com.l2jserver.gameserver.model.L2WorldRegion;
import com.l2jserver.gameserver.model.Location;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.util.Point3D;

public class ObjectPosition extends Point3D
{
	private static final Logger _log = Logger.getLogger(ObjectPosition.class.getName());
	
	private L2Object _activeObject;
	private L2WorldRegion _worldRegion; // Object localization : Used for items/chars that are seen in the world
	
	public ObjectPosition()
	{
		super(0, 0, 0);
		setWorldRegion(L2World.getInstance().getRegion(getWorldPosition()));
	}
	
	/**
	 * Set the x,y,z position of the L2Object and if necessary modify its _worldRegion.<BR>
	 * <BR>
	 * <B><U> Assert </U> :</B><BR>
	 * <BR>
	 * <li>_worldRegion != null</li><BR>
	 * <BR>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li>Update position during and after movement, or after teleport</li><BR>
	 * @param x
	 * @param y
	 * @param z
	 */
	@Override
	public final void setXYZ(int x, int y, int z)
	{
		assert getWorldRegion() != null;
		
		super.setXYZ(x, y, z);
		
		try
		{
			if (L2World.getInstance().getRegion(getWorldPosition()) != getWorldRegion())
			{
				updateWorldRegion();
			}
		}
		catch (Exception e)
		{
			_log.warning("Object Id at bad coords: (x: " + getX() + ", y: " + getY() + ", z: " + getZ() + ").");
			badCoords();
		}
	}
	
	/**
	 * Called on setXYZ exception.<BR>
	 * <BR>
	 * <B><U> Overwritten in </U> :</B><BR>
	 * <BR>
	 * <li>CharPosition</li> <li>PcPosition</li><BR>
	 */
	protected void badCoords()
	{
		if (_activeObject.isCharacter())
		{
			getActiveObject().decayMe();
		}
		else if (_activeObject.isPlayer())
		{
			((L2Character) getActiveObject()).teleToLocation(new Location(0, 0, 0), false);
			((L2Character) getActiveObject()).sendMessage("Error with your coords, Please ask a GM for help!");
		}
	}
	
	/**
	 * Set the x,y,z position of the L2Object and make it invisible.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * A L2Object is invisible if <B>_hidden</B>=true or <B>_worldregion</B>==null <BR>
	 * <BR>
	 * <B><U> Assert </U> :</B><BR>
	 * <BR>
	 * <li>_worldregion==null <I>(L2Object is invisible)</I></li><BR>
	 * <BR>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li>Create a Door</li> <li>Restore L2PcInstance</li><BR>
	 * @param x
	 * @param y
	 * @param z
	 */
	public final void setXYZInvisible(int x, int y, int z)
	{
		assert getWorldRegion() == null;
		if (x > L2World.MAP_MAX_X)
		{
			x = L2World.MAP_MAX_X - 5000;
		}
		if (x < L2World.MAP_MIN_X)
		{
			x = L2World.MAP_MIN_X + 5000;
		}
		if (y > L2World.MAP_MAX_Y)
		{
			y = L2World.MAP_MAX_Y - 5000;
		}
		if (y < L2World.MAP_MIN_Y)
		{
			y = L2World.MAP_MIN_Y + 5000;
		}
		
		setXYZ(x, y, z);
		getActiveObject().setIsVisible(false);
	}
	
	public final void setLocationInvisible(Location loc)
	{
		setXYZInvisible(loc.getX(), loc.getY(), loc.getZ());
	}
	
	/**
	 * checks if current object changed its region, if so, update references
	 */
	public void updateWorldRegion()
	{
		if (!getActiveObject().isVisible())
		{
			return;
		}
		
		L2WorldRegion newRegion = L2World.getInstance().getRegion(getWorldPosition());
		if (newRegion != getWorldRegion())
		{
			getWorldRegion().removeVisibleObject(getActiveObject());
			
			setWorldRegion(newRegion);
			
			// Add the L2Oject spawn to _visibleObjects and if necessary to _allplayers of its L2WorldRegion
			getWorldRegion().addVisibleObject(getActiveObject());
		}
	}
	
	public void setActiveObject(L2Object activeObject)
	{
		_activeObject = activeObject;
	}
	
	public L2Object getActiveObject()
	{
		return _activeObject;
	}
	
	public final void setWorldPosition(Point3D newPosition)
	{
		setXYZ(newPosition.getX(), newPosition.getY(), newPosition.getZ());
	}
	
	public final L2WorldRegion getWorldRegion()
	{
		return _worldRegion;
	}
	
	public final ObjectPosition getPosition()
	{
		return this;
	}
	
	public void setWorldRegion(L2WorldRegion value)
	{
		if ((getWorldRegion() != null) && (getActiveObject() instanceof L2Character)) // confirm revalidation of old region's zones
		{
			if (value != null)
			{
				getWorldRegion().revalidateZones((L2Character) getActiveObject()); // at world region change
			}
			else
			{
				getWorldRegion().removeFromZones((L2Character) getActiveObject()); // at world region change
			}
		}
		
		_worldRegion = value;
	}
}
