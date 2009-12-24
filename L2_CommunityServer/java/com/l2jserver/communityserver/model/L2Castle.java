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
package com.l2jserver.communityserver.model;

/**
 * This class describes a castle instance
 *
 */
public class L2Castle
{
	private int _castleId;
	private String _name;
	private int _ownerId;
	private int _tax;
	private long _siegeDate;

	
	public L2Castle(int castleId, String name, int ownerId, int tax, long siegeDate)
	{
		_castleId = castleId;
		_name = name;
		_ownerId = ownerId;
		_tax = tax;
		_siegeDate = siegeDate;
	}
	
	public int getId()
	{
		return _castleId;
	}
	
	public String getName()
	{
		return _name;
	}
	
	public int getOwnerId()
	{
		return _ownerId;
	}
	
	public long getSiegeDate()
	{
		return _siegeDate;
	}
	
	public int getTax()
	{
		return _tax;
	}
}

