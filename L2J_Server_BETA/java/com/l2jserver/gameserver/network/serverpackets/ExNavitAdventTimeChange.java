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

/**
 * @author mochitto
 *
 * Format: (ch)cd
 * c: state 0 - pause 1 - started
 * d: left time in ms max is 16000 its 4m and state is automatically changed to quit
 */
public class ExNavitAdventTimeChange extends L2GameServerPacket
{
	private static final String _S__FE_E1_EXNAVITADVENTTIMECHANGE = "[S] FE:E1 ExNavitAdventTimeChange";
	private final boolean _paused;
	private final int _time;
	
	public ExNavitAdventTimeChange(int time)
	{
		_time = time > 240000 ? 240000 : time;
		_paused = _time < 1;
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0xFE);
		writeH(0xE1);
		writeC(_paused ? 0x00 : 0x01);
		writeD(_time); // time in ms (16000 = 4mins = state quit)
	}
	
	@Override
	public String getType()
	{
		return _S__FE_E1_EXNAVITADVENTTIMECHANGE;
	}
}
