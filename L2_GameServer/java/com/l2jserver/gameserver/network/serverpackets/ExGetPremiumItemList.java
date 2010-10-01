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

import com.l2jserver.gameserver.model.L2PremiumItem;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;

/**
 ** @author Gnacik
 */
public class ExGetPremiumItemList extends L2GameServerPacket
{
	private static final String _S__FE_86_EXGETPREMIUMITEMLIST = "[S] FE:86 ExGetPremiumItemList";
	
	private L2PcInstance _activeChar;
	
	private Map<Integer, L2PremiumItem> _list;
	
	public ExGetPremiumItemList(L2PcInstance activeChar)
	{
		_activeChar = activeChar;
		
		_list = _activeChar.getPremiumItemList();
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0xFE);
		writeH(0x86);
		if (!_list.isEmpty())
		{
			writeD(_list.size());
			
			for (Integer num : _list.keySet())
			{
				L2PremiumItem item = _list.get(num);
				
				writeD(num);
				writeD(_activeChar.getCharId());
				writeD(item.getItemId());
				writeQ(item.getCount());
				writeD(0);
				writeS(item.getSender());
			}
		}
	}
	
	@Override
	public String getType()
	{
		return _S__FE_86_EXGETPREMIUMITEMLIST;
	}
}
