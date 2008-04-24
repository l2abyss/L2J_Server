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
package net.sf.l2j.gameserver.pathfinding.worldnodes;

import net.sf.l2j.gameserver.pathfinding.AbstractNodeLoc;

/**
 *
 * @author -Nemesiss-
 */
public class NodeLoc extends AbstractNodeLoc
{
	private final int _x;
	private final int _y;
	private final short _z;

	public NodeLoc(int x, int y, short z)
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
		return _x;
	}

	/**
	 * @see net.sf.l2j.gameserver.pathfinding.AbstractNodeLoc#getY()
	 */
	@Override
	public int getY()
	{
		return _y;
	}

	/**
	 * @see net.sf.l2j.gameserver.pathfinding.AbstractNodeLoc#getZ()
	 */
	@Override
	public short getZ()
	{
		return _z;
	}

	/**
	 * @see net.sf.l2j.gameserver.pathfinding.AbstractNodeLoc#getNodeX()
	 */
	@Override
	public short getNodeX()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * @see net.sf.l2j.gameserver.pathfinding.AbstractNodeLoc#getNodeY()
	 */
	@Override
	public short getNodeY()
	{
		// TODO Auto-generated method stub
		return 0;
	}

}
