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
package com.l2jserver.gameserver.network.serverpackets;
 
import java.util.List;

import com.l2jserver.gameserver.model.olympiad.AbstractOlympiadGame;
import com.l2jserver.gameserver.model.olympiad.OlympiadGameClassed;
import com.l2jserver.gameserver.model.olympiad.OlympiadGameNonClassed;
import com.l2jserver.gameserver.model.olympiad.OlympiadGameTask;
import com.l2jserver.gameserver.model.olympiad.OlympiadGameTeams;
 
/**
 * Format: (chd) ddd[dddS]
 * d: number of matches
 * d: unknown (always 0)
 * [
 *  d: arena
 *  d: match type
 *  d: status
 *  S: player 1 name
 *  S: player 2 name
 * ]
 * 
 * @author mrTJO
 */
public class ExOlympiadMatchList extends L2GameServerPacket
{
	private static final String _S__FE_D4_OLYMPIADMATCHLIST = "[S] FE:D4 ExOlympiadMatchList";
	private final List<OlympiadGameTask> _games;
	
	/**
	 * @param games: competitions list
	 */
	public ExOlympiadMatchList(List<OlympiadGameTask> games)
	{
		_games = games;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0xfe);
		writeH(0xd4);
		writeD(0x00);
		
		writeD(_games.size());
		writeD(0x00);
		
		for (OlympiadGameTask curGame : _games)
		{
			AbstractOlympiadGame game = curGame.getGame();
			if (game != null)
			{
				writeD(game.getStadiumId()); // Stadium Id (Arena 1 = 0)
				
				if (game instanceof OlympiadGameNonClassed)
					writeD(1);
				else if (game instanceof OlympiadGameClassed)
					writeD(2);
				else if (game instanceof OlympiadGameTeams)
					writeD(-1);
				else
					writeD(0);
				
				writeD(curGame.isRunning() ? 0x02 : 0x01); // (1 = Standby, 2 = Playing)
				writeS(game.getPlayerNames()[0]); // Player 1 Name
				writeS(game.getPlayerNames()[1]); // Player 2 Name
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see com.l2jserver.gameserver.serverpackets.ServerBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__FE_D4_OLYMPIADMATCHLIST;
	}
}
