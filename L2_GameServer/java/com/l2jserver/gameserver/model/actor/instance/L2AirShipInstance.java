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
package com.l2jserver.gameserver.model.actor.instance;

import java.util.logging.Logger;

import com.l2jserver.gameserver.ai.L2AirShipAI;
import com.l2jserver.gameserver.model.L2CharPosition;
import com.l2jserver.gameserver.model.Location;
import com.l2jserver.gameserver.model.actor.L2Vehicle;
import com.l2jserver.gameserver.network.serverpackets.ExAirShipInfo;
import com.l2jserver.gameserver.network.serverpackets.ExGetOffAirShip;
import com.l2jserver.gameserver.network.serverpackets.ExGetOnAirShip;
import com.l2jserver.gameserver.network.serverpackets.ExMoveToLocationAirShip;
import com.l2jserver.gameserver.network.serverpackets.ExStopMoveAirShip;
import com.l2jserver.gameserver.templates.chars.L2CharTemplate;
import com.l2jserver.util.Point3D;

/**
 * Flying airships. Very similar to Maktakien boats (see L2BoatInstance) but these do fly :P
 *
 * @author  DrHouse, reworked by DS
 */
public class L2AirShipInstance extends L2Vehicle
{
	protected static final Logger _airShiplog = Logger.getLogger(L2AirShipInstance.class.getName());
	
	public L2AirShipInstance(int objectId, L2CharTemplate template)
	{
		super(objectId, template);
		setInstanceType(InstanceType.L2AirShipInstance);
		setAI(new L2AirShipAI(new AIAccessor()));
	}

	@Override
	public boolean isAirShip()
	{
		return true;
	}

	@Override
	public boolean moveToNextRoutePoint()
	{
		final boolean result = super.moveToNextRoutePoint();
		if (result)
			broadcastPacket(new ExMoveToLocationAirShip(this));

		return result;
	}

	@Override
	public boolean addPassenger(L2PcInstance player)
	{
		if (!super.addPassenger(player))
			return false;

		player.setVehicle(this);
		player.setInVehiclePosition(new Point3D(0,0,0));
		player.broadcastPacket(new ExGetOnAirShip(player, this));
		player.getKnownList().removeAllKnownObjects();
		player.setXYZ(getX(), getY(), getZ());
		player.revalidateZone(true);
		return true;
	}

	@Override
	public void oustPlayer(L2PcInstance player)
	{
		super.oustPlayer(player);

		final Location loc = getOustLoc();
		if (player.isOnline() > 0)
		{
			player.broadcastPacket(new ExGetOffAirShip(player, this, loc.getX(), loc.getY(), loc.getZ()));
			player.getKnownList().removeAllKnownObjects();
			player.setXYZ(loc.getX(), loc.getY(), loc.getZ());
			player.revalidateZone(true);
		}
		else
			player.setXYZInvisible(loc.getX(), loc.getY(), loc.getZ());
	}

	@Override
	public void stopMove(L2CharPosition pos, boolean updateKnownObjects)
	{
		super.stopMove(pos, updateKnownObjects);

		broadcastPacket(new ExStopMoveAirShip(this));
	}

	@Override
	public void sendInfo(L2PcInstance activeChar)
	{
		activeChar.sendPacket(new ExAirShipInfo(this));
	}
}