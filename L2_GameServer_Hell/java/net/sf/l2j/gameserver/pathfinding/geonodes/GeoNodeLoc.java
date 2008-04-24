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
package net.sf.l2j.gameserver.pathfinding.geonodes;

import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.pathfinding.AbstractNodeLoc;

/**
 *
 * @author -Nemesiss-
 */
public class GeoNodeLoc extends AbstractNodeLoc
{
	private final short _x;
	private final short _y;
	private final short _z;

	public GeoNodeLoc(short x, short y, short z)
	{
		_x = x;
		_y = y;
		_z = z;
	}

	/**
	 * @see net.sf.l2j.gameserver.pathfinding.AbstractNodeLoc#getX()
	 */
	@Override
	public int getX()
	{
		return   L2World.MAP_MIN_X  + _x * 128 + 48 ;
	}

	/**
	 * @see net.sf.l2j.gameserver.pathfinding.AbstractNodeLoc#getY()
	 */
	@Override
	public int getY()
	{
		return  L2World.MAP_MIN_Y + _y * 128 + 48 ;
	}

	/**
	 * @see net.sf.l2j.gameserver.pathfinding.AbstractNodeLoc#getZ()
	 */
	@Override
	public short getZ()
	{
		return _z;
	}

	@Override
	public short getNodeX()
	{
		return _x;
	}

	@Override
	public short getNodeY()
	{
		return _y;
	}

}
