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

import java.util.logging.Logger;

import javolution.text.TextBuilder;
import com.l2jserver.communityserver.cache.HtmCache;
import com.l2jserver.communityserver.communityboard.CommunityBoard;
import com.l2jserver.communityserver.communityboard.CommunityBoardManager;
import com.l2jserver.communityserver.model.L2Player;

public final class FriendBoard extends CommunityBoard
{
	private static Logger _log = Logger.getLogger(FriendBoard.class.getName());
	public FriendBoard(final CommunityBoardManager mgr)
	{
		super(mgr);
	}
	
	@Override
	public void parseCmd(final int playerObjId, final String cmd)
	{
		if (cmd.equals("_bbsfriend"))
			showMainPage(playerObjId, false);
		else if (cmd.split(";")[1].equalsIgnoreCase("mail"))
		{
			showMailWrite(playerObjId);
		}
		else if (cmd.split(";")[1].equalsIgnoreCase("select"))
		{
			L2Player player = super.getCommunityBoardManager().getPlayer(playerObjId);
			Integer friendId = Integer.valueOf(cmd.split(";")[2]);
			if (!player.getSelectedFriendsList().contains(friendId))
				player.selectFriend(friendId);
			showMainPage(playerObjId, false);
		}
		else if (cmd.split(";")[1].equalsIgnoreCase("deselect"))
		{
			L2Player player = super.getCommunityBoardManager().getPlayer(playerObjId);
			player.deSelectFriend(Integer.valueOf(cmd.split(";")[2]));
			showMainPage(playerObjId, false);
		}
		else if (cmd.split(";")[1].equalsIgnoreCase("delconfirm"))
		{
			showMainPage(playerObjId, true);
		}
		else
			_log.info("Friend command missing: " + cmd.split(";")[1]);
	}
	
	public final void showMainPage(final int playerObjId, boolean delMsg)
	{
		String content = HtmCache.getInstance().getHtm("data/staticfiles/html/friend.htm");
		TextBuilder fList = new TextBuilder();
		L2Player player = super.getCommunityBoardManager().getPlayer(playerObjId);
		for (int f:player.getFriendList())
		{
			L2Player friend = super.getCommunityBoardManager().getPlayer(f);
			fList.append("<a action=\"bypass _bbsfriend;select;" + friend.getObjId() + "\">" + friend.getName() + "</a> (" + (friend.isOnline() ? "On":"Off") + ") &nbsp;");
		}
		content = content.replaceAll("%friendslist%", fList.toString());
		fList.clear();
		for (int f:player.getSelectedFriendsList())
		{
			L2Player friend = super.getCommunityBoardManager().getPlayer(f);
			fList.append("<a action=\"bypass _bbsfriend;deselect;" + friend.getObjId() + "\">" + friend.getName() + "</a>;");
		}
		content = content.replaceAll("%selectedFriendsList%", fList.toString());
		if (delMsg)
			content = content.replaceAll("%deleteMSG%", "<br>\nAre you sure you want to delete all messages from your Friends List? <button value = \"OK\" action=\"bypass _bssfriend;delall\" back=\"l2ui_ch3.smallbutton2_down\" width=65 height=20 fore=\"l2ui_ch3.smallbutton2\">");
		else
			content = content.replaceAll("%deleteMSG%", "");
		super.send(playerObjId, content);
	}
	
	public final void showMailWrite(final int playerObjId)
	{
		String title = " ";
		String message = " ";
		String content = HtmCache.getInstance().getHtm("data/staticfiles/html/mail-write.htm");
		content = content.replaceAll("%maillink%", "<a action=\"bypass _bbsfriend\">&\\$904;</a> > &\\$915;");

		content = content.replaceAll("%playerObjId%", String.valueOf(playerObjId));
		content = content.replaceAll("%postId%", "-1");
		
		String toList = "";
		L2Player player = super.getCommunityBoardManager().getPlayer(playerObjId);
		for (int f:player.getSelectedFriendsList())
		{
			if (toList.equals(""))
				toList += super.getCommunityBoardManager().getPlayer(f).getName();
			else
				toList += (";" + super.getCommunityBoardManager().getPlayer(f).getName());
		}

		super.sendWrite(playerObjId, content, message, title, toList);

	}

	@Override
	public final void parseWrite(final int playerObjId, final String ar1, final String ar2, final String ar3, final String ar4, final String ar5)
	{
		
	}
}