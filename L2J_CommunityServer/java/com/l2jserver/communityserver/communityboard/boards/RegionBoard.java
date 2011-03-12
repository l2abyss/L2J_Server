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

import javolution.text.TextBuilder;
import com.l2jserver.communityserver.cache.HtmCache;
import com.l2jserver.communityserver.communityboard.CommunityBoard;
import com.l2jserver.communityserver.communityboard.CommunityBoardManager;
import com.l2jserver.communityserver.model.L2Castle;
import com.l2jserver.communityserver.model.L2Clan;

public final class RegionBoard extends CommunityBoard
{
	public RegionBoard(final CommunityBoardManager mgr)
	{
		super(mgr);
	}
	
	@Override
	public void parseCmd(final int playerObjId, final String cmd)
	{
		// this board is disabled on retail, and its not fully implemented here, so for now it is disabled
		super.send(playerObjId, "");
		/*if (cmd.equals("_bbsloc"))
			showMainPage(playerObjId);
		else
			showCastlePage(playerObjId, super.getCommunityBoardManager().getCastle(Integer.valueOf(cmd.split(";")[1])));*/
	}
	
	public final void showMainPage(final int playerObjId)
	{
		String content = HtmCache.getInstance().getHtm("data/staticfiles/html/castlelist.htm");
		TextBuilder cList = new TextBuilder();
		for (L2Castle c : super.getCommunityBoardManager().getCastleList())
		{
			L2Clan cl = super.getCommunityBoardManager().getClan(c.getOwnerId());
			String cName = "NPC Clan";
			String aName = "";
			if (cl != null)
			{
				cName = cl.getName();
				aName = cl.getAllianceName();
			}
			cList.append("<table border=0 cellspacing=0 cellpadding=5 width=750>");
			cList.append("<tr>");
			cList.append("<td FIXWIDTH=5></td>");
			cList.append("<td FIXWIDTH=150><a action=\"bypass _bbsloc;" + c.getId() + "\">&^" + c.getId() + ";</a></td>");
			cList.append("<td FIXWIDTH=155>" + cName + "</td>");
			cList.append("<td FIXWIDTH=155>" + aName + "</td>");
			cList.append("<td FIXWIDTH=140 align=center>" + c.getTax() + "</td>");
			cList.append("<td FIXWIDTH=140 align=center>" + c.getTax() + "</td>");
			cList.append("<td FIXWIDTH=5></td>");
			cList.append("</tr>");
			cList.append("</table>");
			cList.append("<img src=\"L2UI.Squaregray\" width=\"740\" height=\"1\">");
		}
		content = content.replaceAll("%castleList%", cList.toString());
		super.send(playerObjId, content);
	}
	
	public final void showCastlePage(final int playerObjId, L2Castle castle)
	{
		String content = HtmCache.getInstance().getHtm("data/staticfiles/html/castle.htm");
		L2Clan cl = super.getCommunityBoardManager().getClan(castle.getOwnerId());
		content = content.replaceAll("%castleId%", String.valueOf(castle.getId()));
		content = content.replaceAll("%siegeDate%", DateFormat.getInstance().format(new Date(castle.getSiegeDate())));
		content = content.replaceAll("%tax%", String.valueOf(castle.getTax()));
		int clanId = 0;
		String clanName = "NPC Clan";
		String clanLord = "NPC";
		String clanAlly = "none";
		if (cl != null)
		{
			clanId = cl.getClanId();
			clanName = cl.getName();
			clanLord = cl.getLordName();
			clanAlly = cl.getAllianceName();
		}
		content = content.replaceAll("%clanid%", String.valueOf(clanId));
		content = content.replaceAll("%lord%", clanLord);
		content = content.replaceAll("%clanName%", clanName);
		content = content.replaceAll("%allyName%", clanAlly);
		
		super.send(playerObjId, content);
	}

	@Override
	public final void parseWrite(final int playerObjId, final String ar1, final String ar2, final String ar3, final String ar4, final String ar5)
	{
		
	}
}