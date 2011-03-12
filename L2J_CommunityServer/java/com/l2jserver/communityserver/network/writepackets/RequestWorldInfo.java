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

public final class RequestWorldInfo extends BaseWritePacket
{
	public static final byte SERVER_LOAD						= 0;
	public static final byte PLAYER_DATA_UPDATE					= 1;
	public static final byte CLAN_DATA_UPDATE					= 2;
	public static final byte CLAN_NOTICE_UPDATE					= 3;
	public static final byte CLAN_NOTICE_FLAG					= 4;
	public static final byte CLAN_NOTICE_DATA					= 5;
	public RequestWorldInfo(final int type, int intbuffer, String strbuffer, boolean boobuffer)
	{
		writeC(0x01);
		switch (type)
		{
			case SERVER_LOAD:
				writeC(0x00);
				break;
			case PLAYER_DATA_UPDATE:
				writeC(0x01);
				super.writeD(intbuffer);
				break;
			case CLAN_DATA_UPDATE:
				writeC(0x02);
				super.writeD(intbuffer);
				break;
			case CLAN_NOTICE_UPDATE:
				writeC(0x03);
				super.writeD(intbuffer);
				super.writeS(strbuffer);
				super.writeC((boobuffer ? 1:0));
				break;
			case CLAN_NOTICE_FLAG:
				writeC(0x04);
				super.writeD(intbuffer);
				super.writeC((boobuffer ? 1:0));
				break;
			case CLAN_NOTICE_DATA:
				writeC(0x05);
				super.writeD(intbuffer);
				break;
		}
	}
}
