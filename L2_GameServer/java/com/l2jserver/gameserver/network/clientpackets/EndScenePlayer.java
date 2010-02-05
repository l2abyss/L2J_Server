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
package com.l2jserver.gameserver.network.clientpackets;

import java.util.logging.Logger;

import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;

/**
 * 
 * @author JIV
 */
public final class EndScenePlayer extends L2GameClientPacket
{
	private static final String _C__d05b_EndScenePlayer = "[C] d0:5b EndScenePlayer";
	private static Logger _log = Logger.getLogger(EndScenePlayer.class.getName());
	
	private int _moviveId;
	
	@Override
	protected void readImpl()
	{
		_moviveId = readD();
	}
	
	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;
		if (_moviveId == 0)
			return;
		if (activeChar.getMovieId() != _moviveId)
		{
			_log.warning("Player "+getClient()+" sent EndScenePlayer with wrong movie id: "+_moviveId);
			return;
		}
		activeChar.setIsTeleporting(true, false); //just make sure not deleted from l2world
		activeChar.decayMe(); // to make sure everything got updated
		activeChar.spawnMe();
		activeChar.setIsTeleporting(false, false);
		activeChar.setMovieId(0);
	}
	
	/* (non-Javadoc)
	 * @see com.l2jserver.gameserver.clientpackets.ClientBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _C__d05b_EndScenePlayer;
	}
}
