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

/**
 * @author mrTJO
 * Format: chddd
 * 
 * d: Arena
 * d: Answer
 */
public final class RequestExCubeGameReadyAnswer extends L2GameClientPacket
{
	private static final String _C__D0_5C_REQUESTEXCUBEGAMEREADYANSWER = "[C] D0:5C RequestExCubeGameReadyAnswer";
	private static Logger _log = Logger.getLogger(RequestExCubeGameReadyAnswer.class.getName());
	
	int _arena;
	int _answer;
	
	@Override
	protected void readImpl()
	{
		_arena = readD();
		_answer = readD();
	}
	
	@Override
	public void runImpl()
	{
		switch (_answer)
		{
			case 0:
				// Cancel
				break;
			case 1:
				// OK or Time Over
				break;
			default:
				_log.warning("Unknown Cube Game Answer ID: "+_answer);
				break;
		}
	}
	
	@Override
	public String getType()
	{
		return _C__D0_5C_REQUESTEXCUBEGAMEREADYANSWER;
	}
}
