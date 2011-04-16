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

import org.netcon.BaseWritePacket;

/**
 * @authors  Forsaiken, Gigiikun
 */
public final class RequestCommunityBoardWrite extends BaseWritePacket
{
	public RequestCommunityBoardWrite(final int playerObjId, final String url, final String arg1, final String arg2, final String arg3, final String arg4, final String arg5)
	{
		super.writeC(0x02);
		super.writeC(0x01);
		super.writeD(playerObjId);
		super.writeS(url);
		super.writeS(arg1);
		super.writeS(arg2);
		super.writeS(arg3);
		super.writeS(arg4);
		super.writeS(arg5);
	}
}
