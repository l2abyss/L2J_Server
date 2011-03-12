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

import java.util.StringTokenizer;

import com.l2jserver.communityserver.cache.HtmCache;
import com.l2jserver.communityserver.communityboard.CommunityBoard;
import com.l2jserver.communityserver.communityboard.CommunityBoardManager;

public class TopBoard extends CommunityBoard
{
	public TopBoard(final CommunityBoardManager mgr)
	{
		super(mgr);
	}
	
	@Override
	public final void parseCmd(final int playerObjId, final String cmd)
	{
		String file = "";
		String content = "";
		if (cmd.equalsIgnoreCase("_bbshome"))
			file = "index.htm";
		else if (cmd.startsWith("_bbshome;"))
		{
			StringTokenizer st = new StringTokenizer(cmd, ";");
			st.nextToken();
			file = st.nextToken();
		}
		if (file.isEmpty())
			content = "<html><body><br><br><center>Error: no file name </center></body></html>";
		else
			content = HtmCache.getInstance().getHtm(this.getCommunityBoardManager().getSQLDPId(),"html/" + file);
		if (content == null)
			content = "<html><body><br><br><center>404 :File Not foud: '" + file + "' </center></body></html>";
		
		super.send(playerObjId, content);
	}
	
	@Override
	public final void parseWrite(final int playerObjId, final String ar1, final String ar2, final String ar3, final String ar4, final String ar5)
	{
		
	}
}