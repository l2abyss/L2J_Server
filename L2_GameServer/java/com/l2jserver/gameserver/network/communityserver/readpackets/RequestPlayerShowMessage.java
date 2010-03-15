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
package com.l2jserver.gameserver.network.communityserver.readpackets;

import java.util.logging.Logger;

import com.l2jserver.gameserver.model.L2World;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.ExMailArrived;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;

import org.netcon.BaseReadPacket;

/**
 * @authors  Forsaiken, Gigiikun
 */
public final class RequestPlayerShowMessage extends BaseReadPacket
{
	private static final Logger _log = Logger.getLogger(RequestPlayerShowMessage.class.getName());
	
	public RequestPlayerShowMessage(final byte[] data)
	{
		super(data);
	}
	
	@Override
	public final void run()
	{
		final int playerObjId = super.readD();
		final int type = super.readD();
		
		L2PcInstance player = (L2PcInstance)L2World.getInstance().findObject(playerObjId);
		if (player == null)
		{
			_log.info("error: player not found!!!");
			return;
		}
		
		switch(type)
		{
			case -1: // mail arraived
				player.sendPacket(ExMailArrived.STATIC_PACKET);
				break;
			case 0: // text message
				player.sendMessage(super.readS());
				break;
			case 236:
				player.sendPacket(new SystemMessage(SystemMessageId.ONLY_THE_CLAN_LEADER_IS_ENABLED));
				break;
			case 1050:
				player.sendPacket(new SystemMessage(SystemMessageId.NO_CB_IN_MY_CLAN));
				break;
			case 1070:
				player.sendPacket(new SystemMessage(SystemMessageId.NO_READ_PERMISSION));
				break;
			case 1071:
				player.sendPacket(new SystemMessage(SystemMessageId.NO_WRITE_PERMISSION));
				break;
			case 1205:
				player.sendPacket(new SystemMessage(SystemMessageId.MAILBOX_FULL));
				break;
			case 1206:
				player.sendPacket(new SystemMessage(SystemMessageId.MEMOBOX_FULL));
				break;
			case 1227:
				try
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.S1_UNREAD_MESSAGES);
					final int number = super.readD();
					sm.addNumber(number);
					player.sendPacket(sm);
				}
				catch (Exception e)
				{
					_log.info("Incorrect packet from CBserver!");
				}
				break;
			case 1228:
				try
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.C1_BLOCKED_YOU_CANNOT_MAIL);
					final String name = super.readS();
					sm.addString(name);
					player.sendPacket(sm);
				}
				catch (Exception e)
				{
					_log.info("Incorrect packet from CBserver!");
				}
				break;
			case 1229:
				player.sendPacket(new SystemMessage(SystemMessageId.NO_MORE_MESSAGES_TODAY));
				break;
			case 1230:
				player.sendPacket(new SystemMessage(SystemMessageId.ONLY_FIVE_RECIPIENTS));
				break;
			case 1231:
				player.sendPacket(new SystemMessage(SystemMessageId.SENT_MAIL));
				break;
			case 1232:
				player.sendPacket(new SystemMessage(SystemMessageId.MESSAGE_NOT_SENT));
				break;
			case 1233:
				player.sendPacket(new SystemMessage(SystemMessageId.NEW_MAIL));
				break;
			case 1234:
				player.sendPacket(new SystemMessage(SystemMessageId.MAIL_STORED_IN_MAILBOX));
				break;
			case 1238:
				player.sendPacket(new SystemMessage(SystemMessageId.TEMP_MAILBOX_FULL));
				break;
			case 1370:
				try
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.CANNOT_MAIL_GM_C1);
					final String name = super.readS();
					sm.addString(name);
					player.sendPacket(sm);
				}
				catch (Exception e)
				{
					_log.info("Incorrect packet from CBserver!");
				}
				break;
			default:
				_log.info("error: Unknown message request from CB server: " + type);
		}
	}
}
