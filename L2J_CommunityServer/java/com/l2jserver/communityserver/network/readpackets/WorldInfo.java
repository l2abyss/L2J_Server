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
package com.l2jserver.communityserver.network.readpackets;

//import java.util.logging.Logger;

import com.l2jserver.communityserver.model.Forum;
import com.l2jserver.communityserver.model.L2Castle;
import com.l2jserver.communityserver.model.L2Player;
import com.l2jserver.communityserver.model.L2Clan;
import com.l2jserver.communityserver.model.Post;
import com.l2jserver.communityserver.model.Topic;
import com.l2jserver.communityserver.network.GameServerThread;
import com.l2jserver.communityserver.network.netcon.BaseReadPacket;
import com.l2jserver.communityserver.network.writepackets.PlayerSendMessage;
import com.l2jserver.communityserver.network.writepackets.RequestWorldInfo;

public final class WorldInfo extends BaseReadPacket
{
	// private static Logger _log = Logger.getLogger(WorldInfo.class.getName());
	private final GameServerThread _gst;
	private final int _type;
	public final int INIT_PACKET = 0x00;
	public final int PLAYER_PACKET = 0x01;
	public final int CLAN_PACKET = 0x02;
	public final int CLAN_NOTICE_PACKET = 0x03;
	
	public WorldInfo(final byte[] data, final GameServerThread gst, final int type)
	{
		super(data);
		_gst = gst;
		_type = type;
	}
	
	@Override
	public final void run()
	{
		switch (_type)
		{
			case INIT_PACKET:
				// clan data
				final int type = super.readD();
				int i;
				switch (type)
				{
					// information
					case 0:
						_gst.addNeededPacket(super.readD());
						break;
					// player
					case 1:
						final int playersCount = super.readD();
						for (i = 0; i < playersCount;i++)
						{
							int playerObjId = super.readD();
							String name = super.readS();
							String accountName = super.readS();
							int playerLevel = super.readD();
							int playerClanId = super.readD();
							int accessLevel = super.readD();
							boolean isOnline = (super.readC() == 1 ? true : false);
							L2Player newPlayer = new L2Player(playerObjId, name, accountName, playerLevel, accessLevel, playerClanId, isOnline);
							int friendSize = super.readD();
							for (int j = 0; j < friendSize; j++)
							{
								newPlayer.addFriend(super.readD());
							}
							_gst.getCommunityBoardManager().addPlayer(newPlayer);
						}
						break;
					// clan
					case 2:
						final int clanCount = super.readD();
						int clanId;
						String clanName;
						int level;
						int lordObjId;
						String lordName;
						int members;
						String allyName;
						for (i = 0; i < clanCount;i++)
						{
							clanId = super.readD();
							clanName = super.readS();
							level = super.readD();
							lordObjId = super.readD();
							lordName = super.readS();
							members = super.readD();
							boolean isNoticeEnabled =(super.readC() == 1 ? true : false);
							allyName = super.readS();
							int allySize = super.readD();
							int[] alliance = new int[allySize];
							for (int j = 0; j < allySize; j++)
							{
								alliance[j] = super.readD();
							}
							_gst.getCommunityBoardManager().addClan(new L2Clan(clanId, clanName, level, lordObjId, lordName, members, allyName, alliance, isNoticeEnabled));
						}
						break;
					// castle
					case 3:
						final int castleCount = super.readD();
						for (i = 0; i < castleCount;i++)
						{
							int castleId = super.readD();
							String castleName = super.readS();
							int ownerId = super.readD();
							int tax = super.readD();
							long siegeDate = ((long)super.readD()) * 1000;
							_gst.getCommunityBoardManager().addCastle(new L2Castle(castleId, castleName, ownerId, tax, siegeDate));
						}
						break;
				}
				break;
			case PLAYER_PACKET:
				final int isFull = super.readC();
				switch (isFull)
				{
					// full player packet
					case 0x00:
						int playerObjId = super.readD();
						String name = super.readS();
						String accountName = super.readS();
						int playerLevel = super.readD();
						int playerClanId = super.readD();
						int accessLevel = super.readD();
						boolean isOnline = (super.readC() == 1 ? true : false);
						int friendSize = super.readD();
						int[] friendIDs = new int[friendSize];
						for (int j = 0; j < friendSize; j++)
						{
							friendIDs[j] = super.readD();
						}
						_gst.getCommunityBoardManager().updatePlayer(playerObjId, name, accountName, playerLevel, accessLevel, playerClanId, isOnline, friendIDs);
						break;
					// status packet
					case 0x01:
						playerObjId = super.readD();
						isOnline = (super.readC() == 1 ? true : false);
						if (_gst.getCommunityBoardManager().getPlayer(playerObjId) == null)
						{
							_gst.getCommunityBoardManager().getGST().sendPacket(new RequestWorldInfo(RequestWorldInfo.PLAYER_DATA_UPDATE, playerObjId, null, false));
							return;
						}
						L2Player player = _gst.getCommunityBoardManager().getPlayer(playerObjId);
						L2Clan playersClan = _gst.getCommunityBoardManager().getClan(player.getClanId()); 
						player.setIsOnline(isOnline);
						if (isOnline)
						{
							Forum f = _gst.getCommunityBoardManager().getPlayerForum(playerObjId);
							int unReaded = 0;
							for (Post p : f.gettopic(Topic.INBOX).getAllPosts())
								if (p.getReadCount() == 0)
									unReaded++;
							if (unReaded >= 1)
								_gst.getCommunityBoardManager().getGST().sendPacket(new PlayerSendMessage(playerObjId,1227,String.valueOf(unReaded)));
						}
						if (playersClan != null && playersClan.getLordObjId() == playerObjId && !playersClan.isNoticeLoaded())
							_gst.sendPacket(new RequestWorldInfo(RequestWorldInfo.CLAN_NOTICE_DATA, playersClan.getClanId(), "", false));
						break;
				}
				break;
			case CLAN_PACKET:
				int clanId = super.readD();
				String clanName = super.readS();
				int level = super.readD();
				int lordObjId = super.readD();
				String lordName = super.readS();
				int members = super.readD();
				boolean isNoticeEnabled =(super.readC() == 1 ? true : false);
				String allyName = super.readS();
				int allySize = super.readD();
				int[] alliance = new int[allySize];
				for (int j = 0; j < allySize; j++)
				{
					alliance[j] = super.readD();
				}
				_gst.getCommunityBoardManager().updateClan(clanId, clanName, level, lordObjId, lordName, members, allyName, alliance, isNoticeEnabled);
				break;
			case CLAN_NOTICE_PACKET:
				clanId = super.readD();
				String notice = super.readS();
				_gst.getCommunityBoardManager().getClan(clanId).setNotice(notice);
				break;
		}
	}
}
