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
package com.l2jserver.communityserver.communityboard;

import java.util.logging.Logger;

import javolution.util.FastList;
import com.l2jserver.communityserver.network.writepackets.PlayerShowBoard;

public abstract class CommunityBoard
{
	private final CommunityBoardManager _mgr;
	private static Logger _log = Logger.getLogger(CommunityBoard.class.getName());
	
	protected CommunityBoard(final CommunityBoardManager mgr)
	{
		_mgr = mgr;
	}
	
	protected final CommunityBoardManager getCommunityBoardManager()
	{
		return _mgr;
	}

	protected final void sendWrite(final int playerObjId, final String html, String string, String string2, String string3)
	{
		try
		{
			string = edtiSavedTxT(string);
			string2 = edtiSavedTxT(string2);
			string3 = edtiSavedTxT(string3);
			_mgr.sendPacket(new PlayerShowBoard(playerObjId, html));
			FastList<String> arg = new FastList<String>();
			arg.add("0");
			arg.add("0");
			arg.add("0");
			arg.add("0");
			arg.add("0");
			arg.add("0");
			arg.add(_mgr.getPlayer(playerObjId).getName());
			arg.add(Integer.toString(playerObjId));
			arg.add(_mgr.getPlayer(playerObjId).getAccountName());
			arg.add("9");
			arg.add(string3);
			arg.add(string2);
			arg.add(string);
			arg.add(string3);
			arg.add(string3);
			arg.add("0");
			arg.add("0");
			_mgr.sendPacket(new PlayerShowBoard(playerObjId, arg));
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	protected final void send(final int playerObjId, final String text)
	{
		try
		{
			if (text.length() <= 4096)
			{
				_mgr.sendPacket(new PlayerShowBoard(playerObjId, text, (byte)0));
				_mgr.sendPacket(new PlayerShowBoard(playerObjId, null, (byte)1));
				_mgr.sendPacket(new PlayerShowBoard(playerObjId, null, (byte)2));
			}
			else if (text.length() <= 8192)
			{
				_mgr.sendPacket(new PlayerShowBoard(playerObjId, text.substring(0, 4096), (byte)0));
				_mgr.sendPacket(new PlayerShowBoard(playerObjId, text.substring(4096), (byte)1));
				_mgr.sendPacket(new PlayerShowBoard(playerObjId, null, (byte)2));
			}
			else if (text.length() <= 12288)
			{
				_mgr.sendPacket(new PlayerShowBoard(playerObjId, text.substring(0, 4096), (byte)0));
				_mgr.sendPacket(new PlayerShowBoard(playerObjId, text.substring(4096, 8192), (byte)1));
				_mgr.sendPacket(new PlayerShowBoard(playerObjId, text.substring(8192), (byte)2));
			}
			else
			{
				_log.warning("Text is too big!");
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	protected String edtiPlayerTxT(String txt)
	{
		if (txt == null)
			return ""; 
		txt = txt.replace(">", "&gt;");
		txt = txt.replace("<", "&lt;");
		txt = txt.replace("\n", "<br1>");
		txt = txt.replace("$", "\\$");
		return txt;
	}

	protected String edtiSavedTxT(String txt)
	{
		if (txt == null)
			return "";
		txt = txt.replace("&gt;", ">");
		txt = txt.replace("&lt;", "<");
		txt = txt.replace("<br1>", "\n");
		txt = txt.replace("\\$", "$");
		return txt;
	}

	public abstract void parseCmd(final int playerObjId, final String cmd);
	
	public abstract void parseWrite(final int playerObjId, String ar1, String ar2, String ar3, String ar4, String ar5);

}