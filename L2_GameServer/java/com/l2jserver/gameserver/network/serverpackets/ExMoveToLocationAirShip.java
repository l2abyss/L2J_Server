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
package com.l2jserver.gameserver.network.serverpackets;

import com.l2jserver.gameserver.model.actor.L2Character;

public class ExMoveToLocationAirShip extends L2GameServerPacket
{	
	private static final String _S__FE_65_EXAIRSHIPMOVETOLOCATION = "[S] FE:65 ExMoveToLocationAirShip";

	private L2Character _ship;

	public ExMoveToLocationAirShip(L2Character cha)
	{
		_ship = cha;
	}

	@Override
	protected void writeImpl()
	{
		writeC(0xfe);
		writeH(0x65);

		writeD(_ship.getObjectId());
		writeD(_ship.getXdestination());
		writeD(_ship.getYdestination());
		writeD(_ship.getZdestination());
		writeD(_ship.getX());
		writeD(_ship.getY());
		writeD(_ship.getZ());
	}

	@Override
	public String getType()
	{
		return _S__FE_65_EXAIRSHIPMOVETOLOCATION;
	}	
}