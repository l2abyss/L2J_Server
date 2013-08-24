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
package com.l2jserver.gameserver.model.actor.instance;

import com.l2jserver.gameserver.enums.InstanceType;
import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.model.actor.templates.L2NpcTemplate;
import com.l2jserver.gameserver.model.entity.Castle;
import com.l2jserver.gameserver.model.entity.Fort;
import com.l2jserver.gameserver.network.serverpackets.ActionFailed;
import com.l2jserver.gameserver.network.serverpackets.NpcHtmlMessage;
import com.l2jserver.gameserver.util.Util;

public class L2FortEnvoyInstance extends L2Npc
{
	public L2FortEnvoyInstance(int objectID, L2NpcTemplate template)
	{
		super(objectID, template);
		setInstanceType(InstanceType.L2FortEnvoyInstance);
	}
	
	@Override
	public void showChatWindow(L2PcInstance player)
	{
		String filePath;
		final Fort fortress = getFort();
		if (!player.isClanLeader() || (fortress.getFortId() != player.getClan().getFortId()))
		{
			filePath = "data/html/fortress/ambassador-not-leader.htm";
		}
		else if (fortress.getFortState() == 1)
		{
			filePath = "data/html/fortress/ambassador-rejected.htm";
		}
		else if (fortress.getFortState() == 2)
		{
			filePath = "data/html/fortress/ambassador-signed.htm";
		}
		else if (fortress.isBorderFortress())
		{
			// border fortresses may only declare independence
			filePath = "data/html/fortress/ambassador-border.htm";
		}
		else
		{
			// normal fortresses can swear fealty or declare independence
			filePath = "data/html/fortress/ambassador.htm";
		}
		
		player.sendPacket(ActionFailed.STATIC_PACKET);
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(player.getHtmlPrefix(), filePath);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%castleName%", String.valueOf(fortress.getCastleByAmbassador(getId()).getName()));
		player.sendPacket(html);
	}
	
	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		if (command.startsWith("select "))
		{
			String param = command.substring(7);
			Fort fortress = getFort();
			Castle castle = fortress.getCastleByAmbassador(getId());
			String filePath;
			
			if (castle.getOwnerId() == 0)
			{
				filePath = "data/html/fortress/ambassador-not-owned.htm";
			}
			else
			{
				int choice = Util.isDigit(param) ? Integer.parseInt(param) : 0;
				fortress.setFortState(choice, castle.getCastleId());
				filePath = (choice == 1) ? "data/html/fortress/ambassador-independent.htm" : "data/html/fortress/ambassador-signed.htm";
			}
			
			final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile(player.getHtmlPrefix(), filePath);
			html.replace("%castleName%", castle.getName());
			player.sendPacket(html);
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}
}