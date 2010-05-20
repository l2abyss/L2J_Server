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

import com.l2jserver.gameserver.model.L2World;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.network.serverpackets.CSShowComBoard;

import org.netcon.BaseReadPacket;

/**
 * @authors  Forsaiken, Gigiikun
 */
public final class RequestPlayerShowBoard extends BaseReadPacket
{
	private static final Logger _log = Logger.getLogger(RequestPlayerShowBoard.class.getName());
	
	public RequestPlayerShowBoard(final byte[] data)
	{
		super(data);
	}
	
	@Override
	public final void run()
	{
		final int playerObjId = super.readD();
		final int length = super.readD();
		final byte[] html = super.readB(length);
		
		L2PcInstance player = L2World.getInstance().getPlayer(playerObjId);
		if (player == null)
		{
			_log.info("error: player not found!!!");
			return;
		}
		
		player.sendPacket(new CSShowComBoard(html));
	}
}
