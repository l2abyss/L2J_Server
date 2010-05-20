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

import com.l2jserver.gameserver.datatables.CharNameTable;
import com.l2jserver.gameserver.model.L2World;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;

/**
 * Support for "Chat with Friends" dialog.
 *
 * This packet is sent only at login.
 *
 * Format: cd (dSdd)
 * d: Total Friend Count
 *
 * d: Player Object ID
 * S: Friend Name
 * d: Online/Offline
 * d: Unknown (0 if offline)
 *
 * @author Tempy
 * 
 */
public class FriendList extends L2GameServerPacket
{
	// private static Logger _log = Logger.getLogger(FriendList.class.getName());
	private static final String _S__FA_FRIENDLIST = "[S] 75 FriendList";
	private List<Integer> _friends;
	private L2PcInstance _activeChar;
	
	@Override
	public void runImpl()
	{
		if (getClient() != null && getClient().getActiveChar() != null)
		{
			_activeChar = getClient().getActiveChar();
			_friends = _activeChar.getFriendList();
		}
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x75);
		if (_friends != null)
		{
			writeD(_friends.size());
			for (int ObjId : _friends)
			{
				String name = CharNameTable.getInstance().getNameById(ObjId); //TODO move to constructor
				L2PcInstance player = L2World.getInstance().getPlayer(ObjId);
				boolean online = false;
				if (player != null && player.isOnline() == 1)
					online = true;
				writeD(ObjId); // character id
				writeS(name);
				writeD(online ? 0x01 : 0x00); // online
				writeD(online ? ObjId : 0x00); // object id if online
			}
		}
		else
			writeD(0);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.l2jserver.gameserver.serverpackets.ServerBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__FA_FRIENDLIST;
	}
}
