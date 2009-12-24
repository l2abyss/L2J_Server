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

import com.l2jserver.Config;

import org.netcon.BaseWritePacket;

/**
 * @authors  Forsaiken, Gigiikun
 */
public final class GameServerAuth extends BaseWritePacket
{
	public GameServerAuth()
	{
		super.writeC(0x00);
		super.writeC(0x01);
		super.writeD(Config.COMMUNITY_SERVER_HEX_ID.length);
		super.writeB(Config.COMMUNITY_SERVER_HEX_ID);
		super.writeD(Config.COMMUNITY_SERVER_SQL_DP_ID);
	}
}
