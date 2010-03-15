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
package com.l2jserver.gameserver.network.communityserver.readpackets;

import java.util.logging.Logger;

import com.l2jserver.gameserver.network.communityserver.CommunityServerThread;

import org.netcon.BaseReadPacket;

/**
 * @authors  Forsaiken, Gigiikun
 */
public final class AuthResponse extends BaseReadPacket
{
	private static final Logger _log = Logger.getLogger(AuthResponse.class.getName());
	
	public static final byte AUTHED						= 0;
	public static final byte REASON_WRONG_HEX_ID		= 1;
	public static final byte REASON_HEX_ID_IN_USE		= 2;
	public static final byte REASON_WRONG_SQL_DP_ID		= 3;
	public static final byte REASON_SQL_DP_ID_IN_USE	= 4;
	
	private final CommunityServerThread _cst;
	
	public AuthResponse(final byte[] data, final CommunityServerThread cst)
	{
		super(data);
		_cst = cst;
	}
	
	@Override
	public final void run()
	{
		final int status = super.readC();
		
		switch (status)
		{
			case AUTHED:
				_cst.setAuthed(true);
				break;
		}
		
		_log.info("COMMUNITY_SERVER_THREAD: Auth "+status);
	}
}
