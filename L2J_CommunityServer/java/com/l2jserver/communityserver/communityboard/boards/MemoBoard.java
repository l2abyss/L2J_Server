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
import com.l2jserver.communityserver.cache.HtmCache;
import com.l2jserver.communityserver.communityboard.CommunityBoard;
import com.l2jserver.communityserver.communityboard.CommunityBoardManager;
import com.l2jserver.communityserver.model.Forum;
import com.l2jserver.communityserver.model.Topic;
import com.l2jserver.communityserver.model.Post;
import com.l2jserver.communityserver.model.Topic.ConstructorType;

public final class MemoBoard extends CommunityBoard
{
	private static Logger _log = Logger.getLogger(MemoBoard.class.getName());
	public MemoBoard(final CommunityBoardManager mgr)
	{
		super(mgr);
	}
	
	@Override
	public void parseCmd(final int playerObjId, final String cmd)
	{
		Forum playerForum = getCommunityBoardManager().getPlayerForum(playerObjId);
		if (cmd.equals("_bbsmemo"))
			showPage(playerObjId, playerForum, 1);
		else if (cmd.split(";")[1].equalsIgnoreCase("crea"))
			showWrite(playerObjId, null);
		else if (cmd.split(";")[1].equalsIgnoreCase("list"))
			showPage(playerObjId, playerForum, Integer.valueOf(cmd.split(";")[2]));
		else if (cmd.split(";")[1].equalsIgnoreCase("read"))
		{
			Topic t = playerForum.gettopic(Topic.MEMO);
			Post p = t.getPost(Integer.valueOf(cmd.split(";")[2]));
			if (p == null)
				_log.info("Memo read command: " + cmd.split(";")[2]);
			else
				showPost(playerObjId, p);
		}
		else if (cmd.split(";")[1].equalsIgnoreCase("del"))
		{
			playerForum.gettopic(Topic.MEMO).rmPostByID(Integer.valueOf(cmd.split(";")[2]));
			showPage(playerObjId, playerForum, 1);
		}
		else if (cmd.split(";")[1].equalsIgnoreCase("edit"))
		{
			Post p = playerForum.gettopic(Topic.MEMO).getPost(Integer.valueOf(cmd.split(";")[2]));
			showWrite(playerObjId, p);
		}
		else
			_log.info("Memo command missing: " + cmd.split(";")[1]);
	}
	
	public final void showPage(final int playerObjId, Forum f, int index)
	{
		String content = HtmCache.getInstance().getHtm("data/staticfiles/html/memo.htm");
		if (f == null)
		{
			_log.info("Forum is NULL!!!");
			super.send(playerObjId, content);
			return;
		}
		
		Topic t = f.gettopic(Topic.MEMO);
		
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
				mList.append("<td FIXWIDTH=511><a action=\"bypass _bbsmemo;read;" + p.getID() + "\">" + p.getTitle() + "</a></td>");
				mList.append("<td FIXWIDTH=148 align=center></td>");
				mList.append("<td FIXWIDTH=86 align=center>" + DateFormat.getInstance().format(new Date(p.getDate())) + "</td>");
				mList.append("<td FIXWIDTH=5></td>");
				mList.append("</tr>");
				mList.append("<tr><td height=5></td></tr>");
				mList.append("</table>");
				mList.append("<img src=\"L2UI.SquareBlank\" width=\"750\" height=\"3\">");
				mList.append("<img src=\"L2UI.SquareGray\" width=\"750\" height=\"1\">");				
 			}
		}
		content = content.replaceAll("%memoList%", mList.toString());
		mList.clear();
		mList.clear();
 		if (index == 1)
 		{
			mList.append("<td><button action=\"\" back=\"l2ui_ch3.prev1_down\" fore=\"l2ui_ch3.prev1\" width=16 height=16 ></td>");
 		}
 		else
 		{
			mList.append("<td><button action=\"bypass _bbsmemo;list;" + (index - 1) + "\" back=\"l2ui_ch3.prev1_down\" fore=\"l2ui_ch3.prev1\" width=16 height=16 ></td>");
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
				mList.append("<td><a action=\"bypass _bbsmemo;list;" + i + "\"> " + i + " </a></td>");
 			}
 		}
 		if (index == nbp)
 		{
			mList.append("<td><button action=\"\" back=\"l2ui_ch3.next1_down\" fore=\"l2ui_ch3.next1\" width=16 height=16 ></td>");
 		}
 		else
 		{
			mList.append("<td><button action=\"bypass _bbsmemo;list;" + (index + 1) + "\" back=\"l2ui_ch3.next1_down\" fore=\"l2ui_ch3.next1\" width=16 height=16 ></td>");
 		}
		content = content.replaceAll("%memoListLength%", mList.toString());

		super.send(playerObjId, content);
	}
	
	public final void showWrite(final int playerObjId, Post p)
	{
		String title = " ";
		String message = " ";
		String content = HtmCache.getInstance().getHtm("data/staticfiles/html/memo-write.htm");
		content = content.replaceAll("%playerObjId%", String.valueOf(playerObjId));
		if (p == null)
			content = content.replaceAll("%job%", "new");
		else
		{
			content = content.replaceAll("%job%", "edit");
			content = content.replaceAll("%postId%", String.valueOf(p.getID()));
			title = p.getTitle();
			message = p.getText();
		}
		
		super.sendWrite(playerObjId, content, message, title, title);
	}
	
	public final void showPost(final int playerObjId, Post p)
	{
		p.increaseReadCount();
		String content = HtmCache.getInstance().getHtm("data/staticfiles/html/memo-show.htm");
		content = content.replaceAll("%memoName%", p.getTitle());
		content = content.replaceAll("%postId%", String.valueOf(p.getID()));
		content = content.replaceAll("%memoOwnerName%", getCommunityBoardManager().getPlayer(p.getOwnerId()).getName());
		content = content.replaceAll("%postDate%", DateFormat.getInstance().format(new Date(p.getDate())));
		content = content.replaceAll("%mes%", p.getText());
		
		super.send(playerObjId, content);
	}
	
	@Override
	public final void parseWrite(final int playerObjId, final String ar1, final String ar2, final String ar3, final String ar4, final String ar5)
	{
		Forum playerForum = getCommunityBoardManager().getPlayerForum(playerObjId);
		if (ar1.equalsIgnoreCase("new"))
		{
			int postId = playerForum.gettopic(Topic.MEMO).getNewPostId();
			Post p = new Post(ConstructorType.CREATE, playerForum.getSqlDPId(), postId, playerObjId, "", System.currentTimeMillis(), Topic.MEMO, playerForum.getID(), super.edtiPlayerTxT(ar3), super.edtiPlayerTxT(ar4), 0, 0);
			playerForum.gettopic(Topic.MEMO).addPost(p);
		}
		else if (ar1.equalsIgnoreCase("edit"))
		{
			playerForum.gettopic(Topic.MEMO).getPost(Integer.valueOf(ar2)).updatePost(super.edtiPlayerTxT(ar3), super.edtiPlayerTxT(ar4));
		}
		else
			_log.info("Memo Write command missing: " + ar1);
		showPage(playerObjId, playerForum, 1);
	}
}