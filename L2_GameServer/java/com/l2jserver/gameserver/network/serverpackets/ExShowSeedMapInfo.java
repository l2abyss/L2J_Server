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
 * format: 0xfe cd(dddd)
 * FE - packet id
 * A1 00 - packet subid
 * d - seed count
 * d - x pos
 * d - y pos
 * d - z pos
 * d - sys msg no
 *
 */
public class ExShowSeedMapInfo extends L2GameServerPacket
{
	private static final String _S__FE_A1_EXSHOWSEEDMAPINFO = "[S] FE:A1 ExShowSeedMapInfo";
	
	/* (non-Javadoc)
	 * @see com.l2jserver.gameserver.network.serverpackets.L2GameServerPacket#writeImpl()
	 */
	@Override
	protected void writeImpl()
	{
		writeC(0xFE); // Id
		writeH(0xa1); // SubId
		
		writeD(0); // seed count
		// for (int i = 0; i < seedCount; i++)
		writeD(0); // x coord
		writeD(0); // y coord
		writeD(0); // z coord
		writeD(0); // sys msg id
	}
	
	@Override
	public String getType()
	{
		return _S__FE_A1_EXSHOWSEEDMAPINFO;
	}
}
