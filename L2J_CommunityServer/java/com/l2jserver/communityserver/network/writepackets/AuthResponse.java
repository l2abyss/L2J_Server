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

public final class AuthResponse extends BaseWritePacket
{
	public static final byte AUTHED						= 0;
	public static final byte REASON_WRONG_HEX_ID		= 1;
	public static final byte REASON_HEX_ID_IN_USE		= 2;
	public static final byte REASON_WRONG_SQL_DP_ID		= 3;
	public static final byte REASON_SQL_DP_ID_IN_USE	= 4;
	
	public AuthResponse(final int status)
	{
		writeC(0x00);
		writeC(0x01);
		writeC(status);
	}
}