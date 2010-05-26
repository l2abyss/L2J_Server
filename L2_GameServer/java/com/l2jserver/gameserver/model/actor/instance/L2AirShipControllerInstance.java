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

import com.l2jserver.gameserver.instancemanager.AirShipManager;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.NpcSay;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;
import com.l2jserver.gameserver.templates.chars.L2NpcTemplate;

/**
 * NPC to control passengers stepping in/out of the airship
 *
 * @author  DrHouse
 */
public class L2AirShipControllerInstance extends L2NpcInstance
{
	private L2AirShipInstance _ship = null;

	/**
	 * @param objectId
	 * @param template
	 */
	public L2AirShipControllerInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
		setInstanceType(InstanceType.L2AirShipControllerInstance);
		AirShipManager.getInstance().registerATC(this);
	}

	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		if (command.equalsIgnoreCase("board"))
		{
			if (player.getPet() != null)
			{
				player.sendPacket(new SystemMessage(SystemMessageId.RELEASE_PET_ON_BOAT));
				return;
			}
			if (player.isTransformed())
			{
				player.sendPacket(new SystemMessage(SystemMessageId.CANT_POLYMORPH_ON_BOAT));
				return;
			}
			if (player.isFlyingMounted())
			{
				player.sendPacket(new SystemMessage(SystemMessageId.YOU_CANNOT_MOUNT_NOT_MEET_REQUEIREMENTS));
				return;
			}

			if (_ship != null)
			{
				_ship.addPassenger(player);
				return;
			}
		}
		else
			super.onBypassFeedback(player, command);
	}

	public void broadcastMessage(String message)
	{
		broadcastPacket(new NpcSay(getObjectId(), 1, getNpcId(), message));
	}

	public void dockShip(L2AirShipInstance ship)
	{
		_ship = ship;
	}
}