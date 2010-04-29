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
package com.l2jserver.gameserver.network.communityserver.writepackets;

import java.util.List;
import java.util.logging.Logger;

import javolution.util.FastList;

import org.netcon.BaseWritePacket;

import com.l2jserver.gameserver.datatables.ClanTable;
import com.l2jserver.gameserver.model.L2Clan;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;

/**
 * @authors  Forsaiken, Gigiikun
 */
public final class WorldInfo extends BaseWritePacket
{
	public static final byte TYPE_INIT_PACKET			= 0;
	public static final byte TYPE_UPDATE_PLAYER_DATA	= 1;
	public static final byte TYPE_UPDATE_PLAYER_STATUS	= 2;
	public static final byte TYPE_UPDATE_CLAN_DATA		= 3;
	public static final byte TYPE_SEND_CLAN_NOTICE		= 4;
	@SuppressWarnings("unused")
	private static Logger _log = Logger.getLogger(WorldInfo.class.getName());
	
	public WorldInfo(L2PcInstance player, L2Clan clan, final byte type)
	{
		super.writeC(0x01);
		
		switch (type)
		{
			case TYPE_INIT_PACKET:
			{
				// this should never happen
				super.writeC(0x00);
			}

			case TYPE_UPDATE_PLAYER_DATA:
			{
				super.writeC(0x01);
				super.writeC(0x00);
				super.writeD(player.getObjectId());
				super.writeS(player.getName());
				super.writeS(player.getAccountName());
				super.writeD(player.getLevel());
				super.writeD(player.getClanId());
				super.writeD(player.getAccessLevel().getLevel());
				super.writeC(player.isOnline());
				List<Integer> list = player.getFriendList();
				super.writeD(list.size());
				for (int j : list)
					super.writeD(j);
				break;
			}

			case TYPE_UPDATE_PLAYER_STATUS:
			{
				super.writeC(0x01);
				super.writeC(0x01);
				super.writeD(player.getObjectId());
				super.writeC(player.isOnline());
				break;
			}

			case TYPE_UPDATE_CLAN_DATA:
			{
				super.writeC(0x02);
				super.writeD(clan.getClanId());
				super.writeS(clan.getName());
				super.writeD(clan.getLevel());
				super.writeD(clan.getLeader().getObjectId());
				super.writeS(clan.getLeader().getName());
				super.writeD(clan.getMembersCount());
				super.writeC((clan.isNoticeEnabled() ? 1:0));
				super.writeS(clan.getAllyName());
				FastList<Integer> allyClanIdList = FastList.newInstance();
				if (clan.getAllyId() != 0)
					for (L2Clan c : ClanTable.getInstance().getClans())
						if (clan.getAllyId() == c.getAllyId() && c != clan)
							allyClanIdList.add(c.getClanId());
				super.writeD(allyClanIdList.size());
				for (int k : allyClanIdList)
					super.writeD(k);
				FastList.recycle(allyClanIdList);
				break;
			}
			case TYPE_SEND_CLAN_NOTICE:
				super.writeC(0x03);
				super.writeD(clan.getClanId());
				super.writeS(clan.getNotice());
				break;
		}
	}
}
