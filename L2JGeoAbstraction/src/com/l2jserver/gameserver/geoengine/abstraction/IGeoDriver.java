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
package com.l2jserver.gameserver.geoengine.abstraction;

import com.l2jserver.gameserver.geoengine.Direction;

/**
 * @author FBIagent
 */
public interface IGeoDriver
{
	/**
	 * Translates world x into geo x.
	 * @param worldX world x
	 * @return geo x
	 */
	int getGeoX(int worldX);
	
	/**
	 * Translates world y into geo y.
	 * @param worldY world y
	 * @return geo y
	 */
	int getGeoY(int worldY);
	
	/**
	 * Translates geo x into world x.
	 * @param geoX geo x
	 * @return world x
	 */
	int getWorldX(int geoX);
	
	/**
	 * Translates geo y into world y.
	 * @param geoY geo y
	 * @return world y
	 */
	int getWorldY(int geoY);
	
	/**
	 * Checks the specified geodata position for available geodata.
	 * @param geoX geo x
	 * @param geoY geo y
	 * @return true when geodata is available, false otherwise
	 */
	boolean hasGeoPos(int geoX, int geoY);
	
	/**
	 * Method to get the nearest z value. If there is no geodata available<br>
	 * at the specified position, {@code worldZ} is returned.
	 * @param geoX geo x
	 * @param geoY geo y
	 * @param worldZ world z
	 * @return nearest z or worldZ(see description above)
	 */
	int getNearestZ(int geoX, int geoY, int worldZ);
	
	/**
	 * Method to get the next lower z value. If there is a layer with a z<br>
	 * equals to {@code worldZ} or there is no lower z, {@code worldZ} is<br>
	 * returned.
	 * @param geoX geo x
	 * @param geoY geo y
	 * @param worldZ world z
	 * @return next lower z or worldZ(see description above)
	 */
	int getNextLowerZ(int geoX, int geoY, int worldZ);
	
	/**
	 * Method to get the next higher z value. If there is a layer with a z<br>
	 * equals to {@code worldZ} or there is no higher z, {@code worldZ} is<br>
	 * returned.
	 * @param geoX geo x
	 * @param geoY geo y
	 * @param worldZ world z
	 * @return next higher z or worldZ(see description above)
	 */
	int getNextHigherZ(int geoX, int geoY, int worldZ);
	
	/**
	 * Checks if the neighbor directions specified can be entered from the<br>
	 * specified geodata position.
	 * @param geoX geo x
	 * @param geoY geo y
	 * @param worldZ world z
	 * @param first first direction
	 * @param more more directions
	 * @return true when the specified neighbor directions can be entered
	 */
	boolean canEnterNeighbors(int geoX, int geoY, int worldZ, Direction first, Direction... more);
	
	/**
	 * Checks if all neighbor directions can be entered from the specified<br>
	 * geodata position.
	 * @param geoX geo x
	 * @param geoY geo y
	 * @param worldZ world z
	 * @return true when all neighbor directions can be entered
	 */
	boolean canEnterAllNeighbors(int geoX, int geoY, int worldZ);
}
