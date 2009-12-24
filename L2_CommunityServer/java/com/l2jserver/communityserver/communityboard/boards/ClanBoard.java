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
package com.l2jserver.communityserver.communityboard.boards;

import java.text.DateFormat;
import java.util.Date;
import java.util.logging.Logger;

import javolution.text.TextBuilder;

import com.l2jserver.communityserver.Config;
import com.l2jserver.communityserver.cache.HtmCache;
import com.l2jserver.communityserver.communityboard.CommunityBoard;
import com.l2jserver.communityserver.communityboard.CommunityBoardManager;
import com.l2jserver.communityserver.model.Forum;
import com.l2jserver.communityserver.model.L2Clan;
import com.l2jserver.communityserver.model.L2Player;
import com.l2jserver.communityserver.model.Post;
import com.l2jserver.communityserver.model.Topic;
import com.l2jserver.communityserver.model.Topic.ConstructorType;
import com.l2jserver.communityserver.network.writepackets.PlayerSendMessage;
import com.l2jserver.communityserver.network.writepackets.RequestWorldInfo;

public final class ClanBoard extends CommunityBoard
{
	private static Logger _log = Logger.getLogger(ClanBoard.class.getName());
	public ClanBoard(final CommunityBoardManager mgr)
	{
		super(mgr);
	}
	
	@Override
	public void parseCmd(final int playerObjId, final String cmd)
	{
		if (cmd.equals("_bbsclan") || cmd.equals("_bbsclan;clan;0"))
			showMainPage(playerObjId, 1);
		else if (cmd.split(";")[1].equalsIgnoreCase("list"))
			showMainPage(playerObjId, Integer.valueOf(cmd.split(";")[2]));
		else if (cmd.split(";")[1].equalsIgnoreCase("clan"))
		{
			if (super.getCommunityBoardManager().getClan(Integer.valueOf(cmd.split(";")[2])).getClanLevel() < Config.MIN_CLAN_LVL_FOR_FORUM)
			{
				if (Config.MIN_CLAN_LVL_FOR_FORUM == 2)
					super.getCommunityBoardManager().getGST().sendPacket(new PlayerSendMessage(playerObjId,PlayerSendMessage.NO_CB_IN_MY_CLAN,""));
				else
				{
					String message = "There are no communities in my clan. Clan communities are allowed for clans with skill levels of " + Config.MIN_CLAN_LVL_FOR_FORUM + " and higher.";
					super.getCommunityBoardManager().getGST().sendPacket(new PlayerSendMessage(playerObjId,PlayerSendMessage.TEXT_MESSAGE,message));
				}
				return;
			}
			showClanPage(playerObjId, Integer.valueOf(cmd.split(";")[2]));
		}
		else if (cmd.split(";")[1].equalsIgnoreCase("notice"))
		{
			if (cmd.split(";").length == 3)
			{
				boolean val = cmd.split(";")[2].equalsIgnoreCase("true");
				super.getCommunityBoardManager().getPlayersClan(playerObjId).setNoticeEnabled(val);
				super.getCommunityBoardManager().getGST().sendPacket(new RequestWorldInfo(RequestWorldInfo.CLAN_NOTICE_FLAG,super.getCommunityBoardManager().getPlayer(playerObjId).getClanId(),null,val));
			}
			showNoticePage(playerObjId);	
		}
		else if (cmd.split(";")[1].equalsIgnoreCase("management"))
		{
			if (super.getCommunityBoardManager().getPlayersClan(playerObjId).getLordObjId() != playerObjId)
				super.getCommunityBoardManager().getGST().sendPacket(new PlayerSendMessage(playerObjId,PlayerSendMessage.ONLY_THE_CLAN_LEADER_IS_ENABLED,""));
			else
				showClanManagementPage(playerObjId, Integer.valueOf(cmd.split(";")[2]));
		}
		else if (cmd.split(";")[1].equalsIgnoreCase("mail"))
			showClanMailPage(playerObjId, Integer.valueOf(cmd.split(";")[2]));
		else if (cmd.split(";")[1].equalsIgnoreCase("permission"))
		{
			int topicId = (cmd.split(";")[2].equalsIgnoreCase("cbb") ? Topic.BULLETIN:Topic.ANNOUNCE);
			L2Player player = super.getCommunityBoardManager().getPlayer(playerObjId);
			Forum clanForum = getCommunityBoardManager().getClanForum(player.getClanId());
			int perNon = clanForum.gettopic(topicId).getPermissions();
			int perMem = perNon % 10;
			perNon = (perNon - perMem) / 10;
			if (cmd.split(";")[3].equalsIgnoreCase("non"))
				perNon = (perNon + 1) % 3;
			else
				perMem = (perMem + 1) % 3;
			clanForum.gettopic(topicId).setPermissions(perNon * 10 + perMem);
			showClanManagementPage(playerObjId, player.getClanId());
		}
		else
			_log.info("Clan command missing: " + cmd.split(";")[1]);
	}
	
	public final void showMainPage(final int playerObjId, int index)
	{
		L2Clan ownClan = super.getCommunityBoardManager().getPlayersClan(playerObjId);
		String content = HtmCache.getInstance().getHtm("data/staticfiles/html/clanlist.htm");
		if (ownClan != null)
		{
			content = content.replaceAll("%clanid%", String.valueOf(ownClan.getClanId()));
			content = content.replaceAll("%clanhomename%", ownClan.getName());
		}
		else
		{
			content = content.replaceAll("%clanid%", "0");
			content = content.replaceAll("%clanhomename%", "");			
		}
		
		TextBuilder cList = new TextBuilder();
		int i = 0;
		for (L2Clan c: super.getCommunityBoardManager().getClanList())
		{
			if (c == null)
				continue;
			if (i > ((index - 1) * 10 + 9))
 			{
 				break;
 			}
			if (i++ >= ((index - 1) * 10))
 			{
				cList.append("<img src=\"L2UI.SquareBlank\" width=\"750\" height=\"3\">");
				cList.append("<table border=0 cellspacing=0 cellpadding=0 width=610>");
				cList.append("<tr> ");
				cList.append("<td FIXWIDTH=5></td>");
				cList.append("<td FIXWIDTH=240 align=center><a action=\"bypass _bbsclan;clan;" + c.getClanId() + "\">" + c.getName() + "</a></td>");
				cList.append("<td FIXWIDTH=240 align=center>" + c.getLordName() + "</td>");
				cList.append("<td FIXWIDTH=100 align=center>" + c.getClanLevel() + "</td>");
				cList.append("<td FIXWIDTH=160 align=center>" + c.getMembersCount() + "</td>");
				cList.append("<td FIXWIDTH=5></td>");
				cList.append("</tr>");
				cList.append("<tr><td height=5></td></tr>");
				cList.append("</table>");
				cList.append("<img src=\"L2UI.SquareBlank\" width=\"750\" height=\"3\">");
				cList.append("<img src=\"L2UI.SquareGray\" width=\"750\" height=\"1\">");
 			}
		}
		content = content.replaceAll("%clanlist%", cList.toString());
		cList.clear();
 		if (index == 1)
 		{
			cList.append("<td><button action=\"\" back=\"l2ui_ch3.prev1_down\" fore=\"l2ui_ch3.prev1\" width=16 height=16 ></td>");
 		}
 		else
 		{
			cList.append("<td><button action=\"bypass _bbsclan;list;" + (index - 1) + "\" back=\"l2ui_ch3.prev1_down\" fore=\"l2ui_ch3.prev1\" width=16 height=16 ></td>");
 		}

 		int nbp;
		nbp = super.getCommunityBoardManager().getClanList().size() / 10;
		if (nbp * 10 != super.getCommunityBoardManager().getClanList().size())
 		{
 			nbp++;
 		}
		for (i = 1; i <= nbp; i++)
 		{
 			if (i == index)
 			{
				cList.append("<td> " + i + " </td>");
 			}
 			else
 			{
				cList.append("<td><a action=\"bypass _bbsclan;list;" + i + "\"> " + i + " </a></td>");
 			}
 		}
 		if (index == nbp)
 		{
			cList.append("<td><button action=\"\" back=\"l2ui_ch3.next1_down\" fore=\"l2ui_ch3.next1\" width=16 height=16 ></td>");
 		}
 		else
 		{
			cList.append("<td><button action=\"bypass _bbsclan;list;" + (index + 1) + "\" back=\"l2ui_ch3.next1_down\" fore=\"l2ui_ch3.next1\" width=16 height=16 ></td>");
 		}
		content = content.replaceAll("%clanlistlength%", cList.toString());

		super.send(playerObjId, content);
	}

	public final String getAnnoTemplate(Post p, int clanId)
	{
		TextBuilder template = new TextBuilder();
		template.append("<tr><td height=10></td></tr>");
		template.append("<tr>");
		template.append("<td fixWIDTH=100 align=center valign=top>[&\\$429;]</td>");
		template.append("<td fixWIDTH=460 align=left valign=top><a action=\"bypass _bbscpost;read;announce;" + clanId + ";" + p.getID() + "\">" + p.getTypeName() + p.getTitle() + "</a></td>");
		template.append("<td fixWIDTH=80 align=right valign=top>&\\$418; :</td>");
		template.append("<td fixWIDTH=100 align=right valign=top>" + DateFormat.getInstance().format(new Date(p.getDate())) + "</td>");
		template.append("<td FIXWIDTH=10></td>");
		template.append("</tr>");
		template.append("<tr><td height=2></td></tr>");
		return template.toString();
	}
	
	public final String getCbbTemplate(Post p, int clanId)
	{
		TextBuilder template = new TextBuilder();
		template.append("<table border=0 cellspacing=0 cellpadding=0 width=750>");
		template.append("<tr><td height=8></td></tr>");
		template.append("<tr>");
		template.append("<td FIXWIDTH=45 align=center>[New]</td>");
		template.append("<td FIXWIDTH=400><a action=\"bypass _bbscpost;read;cbb;" + clanId + ";" + p.getID() + "\">" + p.getTypeName() + p.getTitle() + "</a></td>");
		template.append("<td FIXWIDTH=100 align=center>" + super.getCommunityBoardManager().getPlayer(p.getOwnerId()).getName() + "</td>");
		template.append("<td FIXWIDTH=100 align=center>" + DateFormat.getInstance().format(new Date(p.getDate())) + "</td>");
		template.append("<td FIXWIDTH=100 align=center>" + p.getReadCount() + "</td>");
		template.append("<td FIXWIDTH=5></td>");
		template.append("</tr>");
		template.append("</table>");
		template.append("<img src=\"L2UI.squareblank\" width=\"1\" height=\"5\">");
		template.append("<img src=\"L2UI.squaregray\" width=\"750\" height=\"1\">");
		return template.toString();
	}
	
	public final void showClanPage(final int playerObjId, int clanId)
	{
		L2Player player = super.getCommunityBoardManager().getPlayer(playerObjId);
		L2Clan clan = super.getCommunityBoardManager().getClan(clanId);
		String content;
		if (player.getClanId() != clanId)
			content = HtmCache.getInstance().getHtm("data/staticfiles/html/clanhome.htm");
		else if (clan.getLordObjId() == playerObjId)
			content = HtmCache.getInstance().getHtm("data/staticfiles/html/clanhome-leader.htm");
		else
			content = HtmCache.getInstance().getHtm("data/staticfiles/html/clanhome-member.htm");
		
		Forum clanForum = super.getCommunityBoardManager().getClanForum(clanId);
		Post[] p = clanForum.gettopic(Topic.ANNOUNCE).getLastTwoPosts();
		if (p[0] != null)
		{
			String cbb = getAnnoTemplate(p[0], clanId);
			if (p[1] != null)
				cbb += getAnnoTemplate(p[1], clanId);
			content = content.replaceAll("%advert%", cbb);
		}
		else
			content = content.replaceAll("%advert%", "");

		p = clanForum.gettopic(Topic.BULLETIN).getLastTwoPosts();
		if (p[0] != null)
		{
			String cbb = getCbbTemplate(p[0], clanId);
			if (p[1] != null)
				cbb += getCbbTemplate(p[1], clanId);
			content = content.replaceAll("%clanbbs%", cbb);
		}
		else
			content = content.replaceAll("%clanbbs%", "");
		content = content.replaceAll("%clanIntro%", clan.getIndtroduction());
		content = content.replaceAll("%clanid%", String.valueOf(clanId));
		content = content.replaceAll("%clanName%", clan.getName());
		content = content.replaceAll("%clanLvL%", String.valueOf(clan.getClanLevel()));
		content = content.replaceAll("%clanMembers%", String.valueOf(clan.getMembersCount()));
		content = content.replaceAll("%clanLeader%", clan.getLordName());
		String ally = "";
		for (int i : clan.getAllianceClanIdList())
		{
			if (ally == "")
				ally += super.getCommunityBoardManager().getClan(i).getName();
			else
				ally += ", " + super.getCommunityBoardManager().getClan(i).getName();
		}
		content = content.replaceAll("%allyName%", ally);
		
		super.send(playerObjId, content);
	}

	public final void showClanManagementPage(final int playerObjId, int clanId)
	{
		L2Clan clan = super.getCommunityBoardManager().getPlayersClan(playerObjId);
		if (clan.getClanId() != clanId)
		{
			return;
		}
		Forum clanForum = getCommunityBoardManager().getClanForum(clanId);
		String content;
		content = HtmCache.getInstance().getHtm("data/staticfiles/html/clanhome-management.htm");
		
		content = content.replaceAll("%clanid%", String.valueOf(clanId));
		content = content.replaceAll("%clanName%", clan.getName());
		String[] perString = {"No Access","Read Access","Write Access","No Access"};
		int perNon = clanForum.gettopic(Topic.ANNOUNCE).getPermissions();
		int perMem = perNon % 10;
		perNon = (perNon - perMem) / 10;
		content = content.replaceAll("%curAnnoNonPer%", perString[perNon]);
		content = content.replaceAll("%curAnnoMemPer%", perString[perMem]);
		content = content.replaceAll("%nextAnnoNonPer%", perString[perNon + 1]);
		content = content.replaceAll("%nextAnnoMemPer%", perString[perMem + 1]);
		perNon = clanForum.gettopic(Topic.BULLETIN).getPermissions();
		perMem = perNon % 10;
		perNon = (perNon - perMem) / 10;
		content = content.replaceAll("%curBullNonPer%", perString[perNon]);
		content = content.replaceAll("%curBullMemPer%", perString[perMem]);
		content = content.replaceAll("%nextBullNonPer%", perString[perNon + 1]);
		content = content.replaceAll("%nextBullMemPer%", perString[perMem + 1]);
		super.sendWrite(playerObjId, content, clan.getIndtroduction(), "", "");
	}

	public final void showClanMailPage(final int playerObjId, int clanId)
	{
		L2Clan clan = super.getCommunityBoardManager().getPlayersClan(playerObjId);
		if (clan.getClanId() != clanId)
		{
			return;
		}
		String content;
		content = HtmCache.getInstance().getHtm("data/staticfiles/html/clanhome-mail.htm");
		
		content = content.replaceAll("%clanid%", String.valueOf(clanId));
		content = content.replaceAll("%clanName%", clan.getName());
		
		super.send(playerObjId, content);
	}
	
	public final void showNoticePage(final int playerObjId)
	{
		String content = HtmCache.getInstance().getHtm("data/staticfiles/html/clanhome-notice.htm");
		L2Clan clan = super.getCommunityBoardManager().getPlayersClan(playerObjId);
		content = content.replaceAll("%clanid%", String.valueOf(clan.getClanId()));
		content = content.replaceAll("%enabled%", (clan.isNoticeEnabled() ? "True":"False"));
		content = content.replaceAll("%flag%", (clan.isNoticeEnabled() ? "False":"True"));
		super.sendWrite(playerObjId, content, clan.getNotice(), "", "");
	}

	@Override
	public final void parseWrite(final int playerObjId, final String ar1, final String ar2, final String ar3, final String ar4, final String ar5)
	{
		if (ar1.equalsIgnoreCase("intro"))
		{
			L2Player player = super.getCommunityBoardManager().getPlayer(playerObjId);
			if (Integer.valueOf(ar2) != player.getClanId())
				return;
			L2Clan clan = super.getCommunityBoardManager().getClan(player.getClanId());
			String intro = super.edtiPlayerTxT(ar3);
			clan.setIntroduction(intro);
			super.getCommunityBoardManager().storeClanIntro(player.getClanId(), intro);
			showClanManagementPage(playerObjId, Integer.valueOf(ar2));
		}
		else if (ar1.equalsIgnoreCase("notice"))
		{
			String notice = super.edtiPlayerTxT(ar3);
			if (notice.length() > 4096)
				notice = notice.substring(0, 4096);
			L2Clan c = super.getCommunityBoardManager().getPlayersClan(playerObjId);
			c.setNotice(notice);
			super.getCommunityBoardManager().getGST().sendPacket(new RequestWorldInfo(RequestWorldInfo.CLAN_NOTICE_UPDATE,super.getCommunityBoardManager().getPlayer(playerObjId).getClanId(),notice,c.isNoticeEnabled()));
			showNoticePage(playerObjId);	
		}
		else if (ar1.equalsIgnoreCase("mail"))
		{
			L2Clan sender = super.getCommunityBoardManager().getPlayersClan(playerObjId);
			for (L2Player p: super.getCommunityBoardManager().getPlayerList())
			{
				if (p.getClanId() == sender.getClanId())
				{
					Forum receiverForum = getCommunityBoardManager().getPlayerForum(p.getObjId());
					int postId = receiverForum.gettopic(Topic.INBOX).getNewPostId();
					Post post = new Post(ConstructorType.CREATE, receiverForum.getSqlDPId(), postId, playerObjId, sender.getName(), System.currentTimeMillis(), Topic.INBOX, receiverForum.getID(), super.edtiPlayerTxT(ar4), super.edtiPlayerTxT(ar5), 0, 0);
					receiverForum.gettopic(Topic.INBOX).addPost(post);
					if (p.isOnline())
					{
						super.getCommunityBoardManager().getGST().sendPacket(new PlayerSendMessage(p.getObjId(),-1,""));
						super.getCommunityBoardManager().getGST().sendPacket(new PlayerSendMessage(p.getObjId(),1233,""));
					}
				}
			}
			showClanPage(playerObjId, sender.getClanId());
		}
		else
			_log.info("Memo Write command missing: " + ar1);	
	}
}