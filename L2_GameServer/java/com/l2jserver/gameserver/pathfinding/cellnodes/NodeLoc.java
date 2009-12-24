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
package com.l2jserver.gameserver.pathfinding.cellnodes;
import com.l2jserver.gameserver.model.L2World;
import com.l2jserver.gameserver.pathfinding.AbstractNodeLoc;

/**
 *
 * @author -Nemesiss-
 */
public class NodeLoc extends AbstractNodeLoc
{
	private final int _x;
	private final int _y;
	private short _z;

	public NodeLoc(int x, int y, short z)
	{
		_x = x;
		_y = y;
		_z = z;
	}

	/**
	 * @see com.l2jserver.gameserver.pathfinding.AbstractNodeLoc#getX()
	 */
	@Override
	public int getX()
	{
		return (_x << 4) + L2World.MAP_MIN_X;
	}

	/**
	 * @see com.l2jserver.gameserver.pathfinding.AbstractNodeLoc#getY()
	 */
	@Override
	public int getY()
	{
		return (_y << 4) + L2World.MAP_MIN_Y;
	}

	/**
	 * @see com.l2jserver.gameserver.pathfinding.AbstractNodeLoc#getZ()
	 */
	@Override
	public short getZ()
	{
		return _z;
	}

	@Override
	public void setZ(short z)
	{
		_z = z;
	}
	
	/**
	 * @see com.l2jserver.gameserver.pathfinding.AbstractNodeLoc#getNodeX()
	 */
	@Override
	public int getNodeX()
	{
		// TODO Auto-generated method stub
		return _x;
	}

	/**
	 * @see com.l2jserver.gameserver.pathfinding.AbstractNodeLoc#getNodeY()
	 */
	@Override
	public int getNodeY()
	{
		// TODO Auto-generated method stub
		return _y;
	}

	/**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode()
    {
	    final int prime = 31;
	    int result = 1;
	    result = prime * result + _x;
	    result = prime * result + _y;
	    result = prime * result + _z;
	    return result;
    }

	/**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj)
    {
	    if (this == obj)
		    return true;
	    if (obj == null)
		    return false;
	    if (!(obj instanceof NodeLoc))
		    return false;
	    final NodeLoc other = (NodeLoc) obj;
	    if (_x != other._x)
		    return false;
	    if (_y != other._y)
		    return false;
	    if (_z != other._z)
		    return false;
	    return true;
    }

}
