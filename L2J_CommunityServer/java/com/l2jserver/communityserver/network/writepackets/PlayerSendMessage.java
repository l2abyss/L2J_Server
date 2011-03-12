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
package com.l2jserver.communityserver.network.writepackets;

import com.l2jserver.communityserver.network.netcon.BaseWritePacket;

public final class PlayerSendMessage extends BaseWritePacket
{
	public static final int TEXT_MESSAGE						= 0;
	public static final int ONLY_THE_CLAN_LEADER_IS_ENABLED		= 236;
	public static final int NO_CB_IN_MY_CLAN					= 1050;
	public static final int NO_READ_PERMISSION					= 1070;
	public static final int NO_WRITE_PERMISSION					= 1071;
	public PlayerSendMessage(final int playerObjId, final int type, String message)
	{
		writeC(0x02);
		writeC(0x01);
		writeD(playerObjId);
		writeD(type);
		switch (type)
		{
			case TEXT_MESSAGE:
			case 1228:
			case 1370:
				writeS(message);
				break;
			case 1227:
				writeD(Integer.valueOf(message));
				break;
		}
	}
}
