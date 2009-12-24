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
package com.l2jserver.gameserver.network.clientpackets;

import com.l2jserver.gameserver.instancemanager.BoatManager;
import com.l2jserver.gameserver.model.actor.instance.L2BoatInstance;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.network.serverpackets.GetOffVehicle;


/**
 * @author Maktakien
 *
 */
public final class RequestGetOffVehicle extends L2GameClientPacket
{
	 private int _id, _x, _y, _z;

	@Override
	protected void readImpl()
	{
		_id  = readD();
		_x  = readD();
		_y  = readD();
		_z  = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;
        L2BoatInstance boat = BoatManager.getInstance().getBoat(_id);
        GetOffVehicle Gon = new GetOffVehicle(activeChar,boat,_x,_y,_z);
        activeChar.broadcastPacket(Gon);
	}

	/* (non-Javadoc)
	 * @see com.l2jserver.gameserver.BasePacket#getType()
	 */
	@Override
	public String getType()
	{
		// TODO Auto-generated method stub
		return "[S] 5d GetOffVehicle";
	}

}
