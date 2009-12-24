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
import java.util.Collection;
import java.util.Date;
import java.util.logging.Logger;

import javolution.text.TextBuilder;
import javolution.util.FastList;
import com.l2jserver.communityserver.cache.HtmCache;
import com.l2jserver.communityserver.communityboard.CommunityBoard;
import com.l2jserver.communityserver.communityboard.CommunityBoardManager;
import com.l2jserver.communityserver.model.Comment;
import com.l2jserver.communityserver.model.Forum;
import com.l2jserver.communityserver.model.Topic;
import com.l2jserver.communityserver.model.Post;
import com.l2jserver.communityserver.model.Topic.ConstructorType;
import com.l2jserver.communityserver.network.writepackets.PlayerSendMessage;

public final class ClanPostBoard extends CommunityBoard
{
	private static Logger _log = Logger.getLogger(ClanPostBoard.class.getName());
	public ClanPostBoard(final CommunityBoardManager mgr)
	{
		super(mgr);
	}
	
	@Override
	public void parseCmd(final int playerObjId, final String cmd)
	{
		int clanId = Integer.valueOf(cmd.split(";")[3]);
		Forum clanForum = getCommunityBoardManager().getClanForum(clanId);
		int type;
		if (cmd.split(";")[2].equalsIgnoreCase("announce"))
			type = Topic.ANNOUNCE;
		else if (cmd.split(";")[2].equalsIgnoreCase("cbb"))
			type = Topic.BULLETIN;
		else
		{
			_log.info("Clan Post Board command error: " + cmd);
			return;
		}
		int perNon = clanForum.gettopic(type).getPermissions();
		int perMem = perNon % 10;
		perNon = (perNon - perMem) / 10;
		boolean isPlayerMember = getCommunityBoardManager().getPlayer(playerObjId).getClanId() == clanId;
		boolean isLeader = getCommunityBoardManager().getClan(clanId).getLordObjId() == playerObjId;
		if (!isLeader && ((isPlayerMember && perMem == 0) || (!isPlayerMember && perNon == 0)))
		{
			// TODO: this way Clan Post Board command missing part could be missed
			super.getCommunityBoardManager().getGST().sendPacket(new PlayerSendMessage(playerObjId,PlayerSendMessage.NO_READ_PERMISSION,""));
			return;
		}
		
		if (cmd.split(";")[1].equalsIgnoreCase("list"))
		{
			int index = 1;
			if (cmd.split(";").length == 5)
				index = Integer.valueOf(cmd.split(";")[4]);
			showPage(playerObjId, clanForum, type, index);
		}
		else if (cmd.split(";")[1].equalsIgnoreCase("read"))
		{
			Topic t = clanForum.gettopic(type);
			Post p = t.getPost(Integer.valueOf(cmd.split(";")[4]));
			if (p == null)
				_log.info("Missing post: " + cmd);
			else
			{
				if (cmd.split(";").length > 5)
				{
					_log.info("Index: " + cmd.split(";")[5] + ";" + cmd.split(";")[6]);
					showPost(playerObjId, t, p, clanId, type, Integer.valueOf(cmd.split(";")[5]), Integer.valueOf(cmd.split(";")[6]));
				}
				else
					showPost(playerObjId, t, p, clanId, type, 1, 1);
			}
		}
		else if (!isLeader && ((isPlayerMember && perMem == 1) || (!isPlayerMember && perNon == 1)))
		{
			// TODO: this way Clan Post Board command missing part could be missed
			super.getCommunityBoardManager().getGST().sendPacket(new PlayerSendMessage(playerObjId,PlayerSendMessage.NO_WRITE_PERMISSION,""));
			return;
		}
		else if (cmd.split(";")[1].equalsIgnoreCase("crea"))
		{
			showWrite(playerObjId, null, clanId, type);
		}
		else if (cmd.split(";")[1].equalsIgnoreCase("del"))
		{
			clanForum.gettopic(type).rmPostByID(Integer.valueOf(cmd.split(";")[4]));
			showPage(playerObjId, clanForum, type, 1);
		}
		else if (cmd.split(";")[1].equalsIgnoreCase("delcom"))
		{
			Topic t = clanForum.gettopic(type);
			Post p = t.getPost(Integer.valueOf(cmd.split(";")[4]));
			p.rmCommentByID(Integer.valueOf(cmd.split(";")[5]));
			showPost(playerObjId, t, p, clanId, type, 1, 1);
		}
		else if (cmd.split(";")[1].equalsIgnoreCase("edit"))
		{
			Post p = clanForum.gettopic(type).getPost(Integer.valueOf(cmd.split(";")[4]));
			showWrite(playerObjId, p, clanId, type);
		}
		else if (cmd.split(";")[1].equalsIgnoreCase("reply"))
		{
			Post p = clanForum.gettopic(type).getPost(Integer.valueOf(cmd.split(";")[4]));
			showReply(playerObjId, p, clanId, type);
		}
		else
			_log.info("Clan Post Board command missing: " + cmd.split(";")[1]);
	}
	
	private String replace(String txt, int type)
	{
		String content = txt;
		switch (type)
		{
			case Topic.ANNOUNCE:
				content = content.replaceAll("%link%", "<a action=\"bypass _bbscpost;list;announce;%clanid%\">Announcement</a>");
				content = content.replaceAll("%type%", "announce");
				content = content.replaceAll("%topicId%", String.valueOf(Topic.ANNOUNCE));
				content = content.replaceAll("%combobox%", "Advertise;Miscellaneous");
				break;
			case Topic.BULLETIN:
				content = content.replaceAll("%link%", "<a action=\"bypass _bbscpost;list;cbb;%clanid%\">Free Community</a>");
				content = content.replaceAll("%type%", "cbb");
				content = content.replaceAll("%topicId%", String.valueOf(Topic.BULLETIN));
				content = content.replaceAll("%combobox%", "Information;Miscellaneous");
				break;
		}

		return content;
	}
	
	public final void showPage(final int playerObjId, Forum f, int type, int index)
	{
		String content = HtmCache.getInstance().getHtm("data/staticfiles/html/clanpost.htm");
		if (f == null)
		{
			_log.info("Forum is NULL!!!");
			super.send(playerObjId, content);
			return;
		}
		
		Topic t = f.gettopic(type);
		TextBuilder mList = new TextBuilder();
		int i = 0;
		for (Post p : t.getAllPosts())
		{
			if (i > ((index - 1) * 10 + 9))
 			{
 				break;
 			}
			if (i++ >= ((index - 1) * 10))
 			{
				mList.append("<img src=\"L2UI.SquareBlank\" width=\"750\" height=\"3\">");
				mList.append("<table border=0 cellspacing=0 cellpadding=0 width=750>");
				mList.append("<tr> ");
				mList.append("<td FIXWIDTH=5></td>");
				mList.append("<td FIXWIDTH=80 align=center>" + p.getID() + "</td>");
				mList.append("<td FIXWIDTH=340><a action=\"bypass _bbscpost;read;%type%;" + f.getOwner() + ";" + p.getID() + "\">" + p.getTypeName() + p.getTitle() + "</a></td>");
				mList.append("<td FIXWIDTH=120 align=center>" + getCommunityBoardManager().getPlayer(p.getOwnerId()).getName() + "</td>");
				mList.append("<td FIXWIDTH=120 align=center>" + DateFormat.getInstance().format(new Date(p.getDate())) + "</td>");
				mList.append("<td FIXWIDTH=80 align=center>" + p.getReadCount() + "</td>");
				mList.append("<td FIXWIDTH=5></td>");
				mList.append("</tr>");
				mList.append("<tr><td height=5></td></tr>");
				mList.append("</table>");
				mList.append("<img src=\"L2UI.SquareBlank\" width=\"750\" height=\"3\">");
				mList.append("<img src=\"L2UI.SquareGray\" width=\"750\" height=\"1\">");
 			}
		}
		content = content.replaceAll("%postList%", mList.toString());
		mList.clear();
 		if (index == 1)
 		{
			mList.append("<td><button action=\"\" back=\"l2ui_ch3.prev1_down\" fore=\"l2ui_ch3.prev1\" width=16 height=16 ></td>");
 		}
 		else
 		{
			mList.append("<td><button action=\"bypass _bbscpost;list;%type%;%clanid%;" + (index - 1) + "\" back=\"l2ui_ch3.prev1_down\" fore=\"l2ui_ch3.prev1\" width=16 height=16 ></td>");
 		}

 		int nbp;
		nbp = t.getAllPosts().size() / 10;
		if (nbp * 10 != t.getAllPosts().size())
 		{
 			nbp++;
 		}
		for (i = 1; i <= nbp; i++)
 		{
 			if (i == index)
 			{
				mList.append("<td> " + i + " </td>");
 			}
 			else
 			{
				mList.append("<td><a action=\"bypass _bbscpost;list;%type%;%clanid%;" + i + "\"> " + i + " </a></td>");
 			}
 		}
 		if (index == nbp)
 		{
			mList.append("<td><button action=\"\" back=\"l2ui_ch3.next1_down\" fore=\"l2ui_ch3.next1\" width=16 height=16 ></td>");
 		}
 		else
 		{
			mList.append("<td><button action=\"bypass _bbscpost;list;%type%;%clanid%;" + (index + 1) + "\" back=\"l2ui_ch3.next1_down\" fore=\"l2ui_ch3.next1\" width=16 height=16 ></td>");
 		}
		content = content.replaceAll("%postListLength%", mList.toString());
		content = replace(content, type);
		content = content.replaceAll("%clanid%", String.valueOf(f.getOwner()));

		super.send(playerObjId, content);
	}
	
	public final void showWrite(final int playerObjId, Post p, int clanId, int type)
	{
		String title = " ";
		String message = " ";
		String content = HtmCache.getInstance().getHtm("data/staticfiles/html/clanpost-write.htm");
		content = replace(content, type);
		content = content.replaceAll("%clanid%", String.valueOf(clanId));
		if (p == null)
		{
			content = content.replaceAll("%job%", "new");
			content = content.replaceAll("%postId%", "-1");
		}
		else
		{
			content = content.replaceAll("%job%", "edit");
			content = content.replaceAll("%postId%", String.valueOf(p.getID()));
			title = p.getTitle();
			message = p.getText();
		}
		
		super.sendWrite(playerObjId, content, message, title, title);
	}

	public final void showReply(final int playerObjId, Post p, int clanId, int type)
	{
		if (p == null)
		{
			showWrite(playerObjId, p, clanId, type);
			return;
		}
		String title = " ";
		String message = " ";
		String content = HtmCache.getInstance().getHtm("data/staticfiles/html/clanpost-write.htm");
		content = replace(content, type);
		content = content.replaceAll("%clanid%", String.valueOf(clanId));
		content = content.replaceAll("%job%", "reply");
		content = content.replaceAll("%postId%", String.valueOf(p.getID()));
		
		super.sendWrite(playerObjId, content, message, title, " ");
	}
	
	public final void showPost(final int playerObjId, Topic t, Post p, int clanId, int type, int indexR, int indexC)
	{
		p.increaseReadCount();
		String content = HtmCache.getInstance().getHtm("data/staticfiles/html/clanpost-show.htm");
		content = content.replaceAll("%postTitle%", p.getTitle());
		content = content.replaceAll("%postId%", String.valueOf(p.getID()));
		Post parent = p;
		if (p.getParentId() != -1)
		{
			content = content.replaceAll("%postParentId%", String.valueOf(p.getParentId()));
			parent = t.getPost(p.getParentId());
		}
		else
			content = content.replaceAll("%postParentId%", String.valueOf(p.getID()));
		content = content.replaceAll("%postOwnerName%", getCommunityBoardManager().getPlayer(p.getOwnerId()).getName());
		content = content.replaceAll("%postReadCount%", String.valueOf(p.getReadCount()));
		content = content.replaceAll("%postDate%", DateFormat.getInstance().format(new Date(p.getDate())));
		content = content.replaceAll("%mes%", p.getText());
		
		// reply list
		TextBuilder mList = new TextBuilder();
		int i = 1;
		FastList<Post> childrenList = t.getChildrenPosts(parent); 
		for (Post child : childrenList)
		{
			if (i++ == indexR)
			{
				mList.append("<table border=0 cellspacing=5 cellpadding=0 WIDTH=750>");
				mList.append("<tr>");
				mList.append("<td WIDTH=60 align=center>" + child.getID() + "</td>");
				if (child != p)
					mList.append("<td width=415><a action=\"bypass _bbscpost;read;%type%;" + clanId + ";" + child.getID() + "\">" + child.getTypeName() + child.getTitle() + "</a></td>");
				else
					mList.append("<td width=415>" + child.getTypeName() + child.getTitle() + "</td>");
				mList.append("<td WIDTH=130 align=center>" + getCommunityBoardManager().getPlayer(child.getOwnerId()).getName() + "</td>");
				mList.append("<td WIDTH=80 align=center>" + DateFormat.getInstance().format(new Date(child.getDate())) + "</td>");
				mList.append("<td WIDTH=65 align=center>" + child.getReadCount() + "</td>");
				mList.append("</tr>");
				mList.append("</table>");
			}
		}
		content = content.replaceAll("%replyList%", mList.toString());
		if (indexR == 1)
			content = content.replaceAll("%prevReply%", "[Previous Reply]");
		else
			content = content.replaceAll("%prevReply%", "<a action=\"bypass _bbscpost;read;%type%;" + clanId + ";" + p.getID() + ";" + (indexR - 1) + ";" + indexC + "\">[Previous Reply]</a>");
		content = content.replaceAll("%replyCount%", indexR + "/" + childrenList.size());
		if (indexR == childrenList.size())
			content = content.replaceAll("%nextReply%", "[Next Reply]");
		else
			content = content.replaceAll("%nextReply%", "<a action=\"bypass _bbscpost;read;%type%;" + clanId + ";" + p.getID() + ";" + (indexR + 1) + ";" + indexC + "\">[Next Reply]</a>");
		
		// comment list
		mList.clear();
		i = 1;
		Collection<Comment> commentsList = p.getAllComments();
		int csize = commentsList.size(); 
		if (csize == 0)
			csize = 1;
		else
			for (Comment c : commentsList)
			{
				if (i++ == indexC)
				{
					mList.append("<tr><td><img src=\"L2UI.squaregray\" width=\"750\" height=\"1\"></td></tr>");
					mList.append("<tr><td>");
					mList.append("<table>");
					mList.append("<tr>");
					mList.append("<td WIDTH=100 valign=top>" + getCommunityBoardManager().getPlayer(c.getOwnerId()).getName() + "</td>");
					mList.append("<td width=10 valign=top><img src=\"L2UI.squaregray\" width=\"5\" height=\"28\"></td>");
					mList.append("<td FIXWIDTH=560 valign=top><font color=\"AAAAAA\">" + c.getText() + "</font></td>");
					mList.append("<td WIDTH=20 valign=top><a action=\"bypass _bbscpost;delcom;%type%;" + clanId + ";" + p.getID() + ";" + c.getID() + "\">&\\$425;</a></td>");
					mList.append("<td WIDTH=60 valign=top>" + DateFormat.getInstance().format(new Date(c.getDate())) + "</td>");
					mList.append("</tr>");
					mList.append("</table>");
					mList.append("</td></tr>");
				}
			}
		content = content.replaceAll("%commentList%", mList.toString());
		if (indexC == 1)
			content = content.replaceAll("%prevCom%", "[Previous Comment]");
		else
			content = content.replaceAll("%prevCom%", "<a action=\"bypass _bbscpost;read;%type%;" + clanId + ";" + p.getID() + ";" + indexR + ";" + (indexC - 1) + "\">[Previous Comment]</a>");
		content = content.replaceAll("%comCount%", indexC + "/" + csize);
		if (indexC == csize)
			content = content.replaceAll("%nextCom%", "[Next Comment]");
		else
			content = content.replaceAll("%nextCom%", "<a action=\"bypass _bbscpost;read;%type%;" + clanId + ";" + p.getID() + ";" + indexR + ";" + (indexC + 1) + "\">[Next Comment]</a>");

		content = replace(content, type);
		content = content.replaceAll("%clanid%", String.valueOf(clanId));
		super.send(playerObjId, content);
	}
	
	@Override
	public final void parseWrite(final int playerObjId, final String ar1, final String ar2, final String ar3, final String ar4, final String ar5)
	{
		int clanId = Integer.valueOf(ar2.split(";")[0]);
		int topicId = Integer.valueOf(ar2.split(";")[1]);
		int postId = Integer.valueOf(ar2.split(";")[2]);
		Forum clanForum = getCommunityBoardManager().getClanForum(clanId);
		int perNon = clanForum.gettopic(topicId).getPermissions();
		int perMem = perNon % 10;
		perNon = (perNon - perMem) / 10;
		boolean isPlayerMember = getCommunityBoardManager().getPlayer(playerObjId).getClanId() == clanId; 
		boolean isLeader = getCommunityBoardManager().getClan(clanId).getLordObjId() == playerObjId;
		if (!isLeader && ((isPlayerMember && perMem != 2) || (!isPlayerMember && perNon != 2)))
		{
			super.getCommunityBoardManager().getGST().sendPacket(new PlayerSendMessage(playerObjId,PlayerSendMessage.NO_WRITE_PERMISSION,""));
			return;
		}

		int type = Post.ADVERTISE;
		if (ar5.equalsIgnoreCase("Information"))
			type = Post.INFORMATION;
		else if (ar5.equalsIgnoreCase("Miscellaneous"))
			type = Post.MISCELLANEOUS;

		if (ar1.equalsIgnoreCase("new"))
		{
			postId = clanForum.gettopic(topicId).getNewPostId();
			Post p = new Post(ConstructorType.CREATE, clanForum.getSqlDPId(), postId, playerObjId, "", System.currentTimeMillis(), topicId, clanForum.getID(), super.edtiPlayerTxT(ar3), super.edtiPlayerTxT(ar4), type, 0);
			clanForum.gettopic(topicId).addPost(p);
		}
		else if (ar1.equalsIgnoreCase("reply"))
		{
			int parentId = postId;
			postId = clanForum.gettopic(topicId).getNewPostId();
			Post p = new Post(ConstructorType.CREATE, clanForum.getSqlDPId(), postId, playerObjId, "", parentId, System.currentTimeMillis(), topicId, clanForum.getID(), super.edtiPlayerTxT(ar3), super.edtiPlayerTxT(ar4), type, 0);
			clanForum.gettopic(topicId).addPost(p);
		}
		else if (ar1.equalsIgnoreCase("edit"))
		{
			clanForum.gettopic(topicId).getPost(postId).updatePost(super.edtiPlayerTxT(ar3), super.edtiPlayerTxT(ar4), type);
		}
		else if (ar1.equalsIgnoreCase("com"))
		{
			Post p = clanForum.gettopic(topicId).getPost(postId);
			int comId = p.getNewCommentId();
			Comment c = new Comment(ConstructorType.CREATE, clanForum.getSqlDPId(), comId, playerObjId, System.currentTimeMillis(), postId, topicId, clanForum.getID(), super.edtiPlayerTxT(ar3));
			p.addComment(c);
			showPost(playerObjId, clanForum.gettopic(topicId), p, clanId, topicId, 1, 1);
			return;
		}
		else
			_log.info("Clan Post Write command missing: " + ar1);
		showPage(playerObjId, clanForum, topicId, 1);
	}
}