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
 * @author JIV
 *
 */
public class ExRotation extends L2GameServerPacket
{
	private static final String _S__77_BEGINROTATION = "[S] FE:C0 ExRotation";
	
	private final int _charObjId, _degree;
	
	public ExRotation(int charId, int degree)
	{
		_charObjId = charId;
		_degree = degree;
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0xFE);
		writeH(0xC1);
		writeD(_charObjId);
		writeD(_degree);
	}
	
	@Override
	public String getType()
	{
		return _S__77_BEGINROTATION;
	}
	
}
