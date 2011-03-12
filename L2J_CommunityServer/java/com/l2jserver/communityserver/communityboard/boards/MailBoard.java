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
import com.l2jserver.communityserver.model.L2Player;
import com.l2jserver.communityserver.model.Topic;
import com.l2jserver.communityserver.model.Post;
import com.l2jserver.communityserver.model.Topic.ConstructorType;
import com.l2jserver.communityserver.network.writepackets.PlayerSendMessage;

public final class MailBoard extends CommunityBoard
{
	private static Logger _log = Logger.getLogger(MailBoard.class.getName());
	public MailBoard(final CommunityBoardManager mgr)
	{
		super(mgr);
	}
	
	@Override
	public void parseCmd(final int playerObjId, final String cmd)
	{
		Forum playerForum = getCommunityBoardManager().getPlayerForum(playerObjId);
		if (playerForum == null)
		{
			String content = HtmCache.getInstance().getHtm("data/staticfiles/html/nopage.htm");
			super.send(playerObjId, content);
		}
		if (cmd.equals("_bbsmail"))
			showPage(playerObjId, playerForum, Topic.INBOX, 1);
		else if (cmd.split(";")[1].equalsIgnoreCase("sent"))
			showPage(playerObjId, playerForum, Topic.OUTBOX, 1);
		else if (cmd.split(";")[1].equalsIgnoreCase("archive"))
			showPage(playerObjId, playerForum, Topic.ARCHIVE, 1);
		else if (cmd.split(";")[1].equalsIgnoreCase("tarchive"))
			showPage(playerObjId, playerForum, Topic.TEMP_ARCHIVE, 1);
		else if (cmd.split(";")[1].equalsIgnoreCase("crea"))
			showWrite(playerObjId, null, 0);
		else if (cmd.split(";")[1].equalsIgnoreCase("reply"))
		{
			Topic t = playerForum.gettopic(Integer.valueOf(cmd.split(";")[2]));
			Post p = t.getPost(Integer.valueOf(cmd.split(";")[3]));
			showWrite(playerObjId, p, 1);
		}
		else if (cmd.split(";")[1].equalsIgnoreCase("forward"))
		{
			Topic t = playerForum.gettopic(Integer.valueOf(cmd.split(";")[2]));
			Post p = t.getPost(Integer.valueOf(cmd.split(";")[3]));
			showWrite(playerObjId, p, 2);
		}
		else if (cmd.split(";")[1].equalsIgnoreCase("store"))
		{
			Topic t = playerForum.gettopic(Integer.valueOf(cmd.split(";")[2]));
			Post p = t.getPost(Integer.valueOf(cmd.split(";")[3]));
			t.rmPostByID(p.getID());
			int postId = playerForum.gettopic(Topic.OUTBOX).getNewPostId();
			p.setTopic(Topic.ARCHIVE, postId);
			playerForum.gettopic(Topic.ARCHIVE).addPost(p);
			showPage(playerObjId, playerForum, Topic.INBOX, 1);
		}
		else if (cmd.split(";")[1].equalsIgnoreCase("del"))
		{
			playerForum.gettopic(Integer.valueOf(cmd.split(";")[2])).rmPostByID(Integer.valueOf(cmd.split(";")[3]));
			showPage(playerObjId, playerForum, Topic.INBOX, 1);
		}
		else if (cmd.split(";")[1].equalsIgnoreCase("read"))
		{
			Topic t = playerForum.gettopic(Integer.valueOf(cmd.split(";")[2]));
			Post p = t.getPost(Integer.valueOf(cmd.split(";")[3]));
			showMail(playerObjId, playerForum, t, p);
		}
		else
			_log.info("Mail command missing: " + cmd.split(";")[1]);
	}
	
	public final void showPage(final int playerObjId, Forum f, int topicType, int index)
	{
		String content = HtmCache.getInstance().getHtm("data/staticfiles/html/mail.htm");
		switch (topicType)
		{
			case Topic.INBOX:
				content = content.replaceAll("%maillink%", "<a action=\"bypass _bbsmail\">Inbox</a>");
				content = content.replaceAll("%authrecive%", "Author");
				break;
			case Topic.OUTBOX:
				content = content.replaceAll("%maillink%", "<a action=\"bypass _bbsmail;sent\">Sent Box</a>");
				content = content.replaceAll("%authrecive%", "Recipient");
				break;
			case Topic.ARCHIVE:
				content = content.replaceAll("%maillink%", "<a action=\"bypass _bbsmail;archive\">Mail Archive</a>");
				content = content.replaceAll("%authrecive%", "Author");
				break;
			case Topic.TEMP_ARCHIVE:
				content = content.replaceAll("%maillink%", "<a action=\"bypass _bbsmail;tarchive\">Temporary Mail Archive</a>");
				content = content.replaceAll("%authrecive%", "Recipient");
				break;
		}
		content = content.replaceAll("%inbox%", String.valueOf(f.gettopic(Topic.INBOX).getAllPosts().size()));
		content = content.replaceAll("%outbox%", String.valueOf(f.gettopic(Topic.OUTBOX).getAllPosts().size()));
		content = content.replaceAll("%archive%", String.valueOf(f.gettopic(Topic.ARCHIVE).getAllPosts().size()));
		content = content.replaceAll("%tarchive%", String.valueOf(f.gettopic(Topic.TEMP_ARCHIVE).getAllPosts().size()));
		
		Topic t = f.gettopic(topicType);
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
				mList.append("<table border=0 cellspacing=0 cellpadding=5 width=755>");
				mList.append("<tr> ");
				mList.append("<td FIXWIDTH=5 align=center></td>");
				mList.append("<td FIXWIDTH=150 align=center>" + getCommunityBoardManager().getPlayer(p.getOwnerId()).getName() + "</td>");
				mList.append("<td FIXWIDTH=440><a action=\"bypass _bbsmail;read;" + t.getID() + ";" + p.getID() + "\">" + p.getTitle() + "</a></td>");
				mList.append("<td FIXWIDTH=150>" + DateFormat.getInstance().format(new Date(p.getDate())) + "</td>");
				mList.append("<td FIXWIDTH=5 align=center></td>");
				mList.append("</tr>");
				mList.append("</table>");
				mList.append("<img src=\"L2UI.Squaregray\" width=\"755\" height=\"1\">");
 			}
		}
		content = content.replaceAll("%maillist%", mList.toString());
		mList.clear();
 		if (index == 1)
 		{
			mList.append("<td><button action=\"\" back=\"l2ui_ch3.prev1_down\" fore=\"l2ui_ch3.prev1\" width=16 height=16 ></td>");
 		}
 		else
 		{
			mList.append("<td><button action=\"bypass _bbspost;list;" + (index - 1) + "\" back=\"l2ui_ch3.prev1_down\" fore=\"l2ui_ch3.prev1\" width=16 height=16 ></td>");
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
				mList.append("<td><a action=\"bypass _bbspost;list;" + i + "\"> " + i + " </a></td>");
 			}
 		}
 		if (index == nbp)
 		{
			mList.append("<td><button action=\"\" back=\"l2ui_ch3.next1_down\" fore=\"l2ui_ch3.next1\" width=16 height=16 ></td>");
 		}
 		else
 		{
			mList.append("<td><button action=\"bypass _bbspost;list;" + (index + 1) + "\" back=\"l2ui_ch3.next1_down\" fore=\"l2ui_ch3.next1\" width=16 height=16 ></td>");
 		}
		content = content.replaceAll("%maillistlength%", mList.toString());
		super.send(playerObjId, content);
	}
	
	public final void showMail(final int playerObjId, Forum f, Topic t, Post p)
	{
		String content = HtmCache.getInstance().getHtm("data/staticfiles/html/mail-show.htm");
		switch (t.getID())
		{
			case Topic.INBOX:
				content = content.replaceAll("%writer%", getCommunityBoardManager().getPlayer(p.getOwnerId()).getName());
				content = content.replaceAll("%receiver%", p.getRecipientList());
				content = content.replaceAll("%maillink%", "<a action=\"bypass _bbsmail\">Inbox</a>");
				break;
			case Topic.OUTBOX:
				content = content.replaceAll("%maillink%", "<a action=\"bypass _bbsmail;sent\">Sent Box</a>");
				content = content.replaceAll("%writer%", getCommunityBoardManager().getPlayer(playerObjId).getName());
				content = content.replaceAll("%receiver%", p.getRecipientList());
				break;
			case Topic.ARCHIVE:
				content = content.replaceAll("%maillink%", "<a action=\"bypass _bbsmail;archive\">Mail Archive</a>");
				content = content.replaceAll("%writer%", getCommunityBoardManager().getPlayer(p.getOwnerId()).getName());
				content = content.replaceAll("%receiver%", p.getRecipientList());
				break;
			case Topic.TEMP_ARCHIVE:
				content = content.replaceAll("%maillink%", "<a action=\"bypass _bbsmail;tarchive\">Temporary Mail Archive</a>");
				content = content.replaceAll("%writer%", getCommunityBoardManager().getPlayer(playerObjId).getName());
				content = content.replaceAll("%receiver%", p.getRecipientList());
				break;
		}
		content = content.replaceAll("%sentDate%", DateFormat.getInstance().format(new Date(p.getDate())));
		content = content.replaceAll("%delDate%", DateFormat.getInstance().format(new Date(p.getDate() + Config.MAIL_AUTO_DELETION_TIME)));
		content = content.replaceAll("%title%", p.getTitle());
		content = content.replaceAll("%mes%", p.getText());
		content = content.replaceAll("%topicId%", String.valueOf(t.getID()));
		content = content.replaceAll("%postId%", String.valueOf(p.getID()));
		p.increaseReadCount();
		
		super.send(playerObjId, content);
	}
	
	public final void showWrite(final int playerObjId, Post p, int type)
	{
		String title = " ";
		String message = " ";
		String toList = " ";
		String content = HtmCache.getInstance().getHtm("data/staticfiles/html/mail-write.htm");
		content = content.replaceAll("%maillink%", "<a action=\"bypass _bbsmail\">Inbox</a>");

		content = content.replaceAll("%playerObjId%", String.valueOf(playerObjId));
		if (p == null)
		{
			content = content.replaceAll("%postId%", "-1");
		}
		else
		{
			content = content.replaceAll("%postId%", String.valueOf(p.getID()));
			title = p.getTitle();
			message = p.getText();
			if (type == 1)
				toList = p.getRecipientList();
		}
		
		super.sendWrite(playerObjId, content, message, title, toList);

	}
	@Override
	public final void parseWrite(final int playerObjId, final String ar1, final String ar2, final String ar3, final String ar4, final String ar5)
	{
		int postId = Integer.valueOf(ar2);
		Forum senderForum = getCommunityBoardManager().getPlayerForum(playerObjId);
		String[] recipients = ar3.split(";");
		boolean isSended = false;
		Post p = null;
		for (String recipient : recipients)
		{
			L2Player receiver = getCommunityBoardManager().getPlayerByName(recipient);

			if (ar1.equalsIgnoreCase("new"))
			{
				if  (receiver != null)
				{
					Forum receiverForum = getCommunityBoardManager().getPlayerForum(receiver.getObjId());
					postId = receiverForum.gettopic(Topic.INBOX).getNewPostId();
					p = new Post(ConstructorType.CREATE, receiverForum.getSqlDPId(), postId, playerObjId, ar3, System.currentTimeMillis(), Topic.INBOX, receiverForum.getID(), super.edtiPlayerTxT(ar4), super.edtiPlayerTxT(ar5), 0, 0);
					receiverForum.gettopic(Topic.INBOX).addPost(p);
					if (receiver.isOnline())
					{
						super.getCommunityBoardManager().getGST().sendPacket(new PlayerSendMessage(receiver.getObjId(),-1,""));
						super.getCommunityBoardManager().getGST().sendPacket(new PlayerSendMessage(receiver.getObjId(),1233,""));
					}
					isSended = true;
				}
			}
			else if (ar1.equalsIgnoreCase("store"))
			{
				postId = senderForum.gettopic(Topic.TEMP_ARCHIVE).getNewPostId();
				p = new Post(ConstructorType.CREATE, senderForum.getSqlDPId(), postId, playerObjId, ar3, System.currentTimeMillis(), Topic.TEMP_ARCHIVE, senderForum.getID(), super.edtiPlayerTxT(ar4), super.edtiPlayerTxT(ar5), 0, 0);
				senderForum.gettopic(Topic.TEMP_ARCHIVE).addPost(p);
				super.getCommunityBoardManager().getGST().sendPacket(new PlayerSendMessage(playerObjId,1234,""));
				showPage(playerObjId, senderForum, Topic.TEMP_ARCHIVE, 1);
				return;
			}
			else
				_log.info("Mail Write command missing: " + ar1);
		}
		if (isSended)
		{
			postId = senderForum.gettopic(Topic.OUTBOX).getNewPostId();
			p = new Post(ConstructorType.CREATE, senderForum.getSqlDPId(), postId, playerObjId, ar3, System.currentTimeMillis(), Topic.OUTBOX, senderForum.getID(), super.edtiPlayerTxT(ar4), super.edtiPlayerTxT(ar5), 0, 0);
			senderForum.gettopic(Topic.OUTBOX).addPost(p);
		}
		showPage(playerObjId, senderForum, Topic.INBOX, 1);
	}
}