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

import java.util.Map;
import java.util.Map.Entry;

import javolution.util.FastMap;

/**
 * @author -Wooden-
 * @author UnAfraid
 * Thanks mrTJO
 */
public class PackageToList extends L2GameServerPacket
{
	private final String _S__C8_PACKAGETOLIST = "[S] C8 PackageToList";
	
	private final Map<Integer,String> _players = new FastMap<Integer, String>();

	public PackageToList(Map<Integer, String> chars)
	{
		_players.putAll(chars);
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0xC8);
		writeD(_players.size());
		for (Entry<Integer,String> entry : _players.entrySet())
		{
			writeD(entry.getKey());
			writeS(entry.getValue());
		}
	}

	@Override
	public String getType()
	{
		return _S__C8_PACKAGETOLIST;
	}
	
}
