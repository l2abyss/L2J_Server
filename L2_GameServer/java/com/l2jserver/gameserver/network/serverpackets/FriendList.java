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

import javolution.util.FastList;

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
	private List<FriendInfo> _info;
	
	private class FriendInfo
	{
		int objId;
		String name;
		boolean online;
	
		public FriendInfo(int objId, String name, boolean online)
		{
			this.objId = objId;
			this.name = name;
			this.online = online;
		}
	}
	
	public FriendList(L2PcInstance player)
	{
		_info = new FastList<FriendInfo>(player.getFriendList().size());
		for (int objId : player.getFriendList())
		{
			String name = CharNameTable.getInstance().getNameById(objId);
			L2PcInstance player1 = L2World.getInstance().getPlayer(objId);
			boolean online = false;
			if (player1 != null && player1.isOnline() == 1)
				online = true;
			_info.add(new FriendInfo(objId, name, online));
		}
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x75);
		writeD(_info.size());
		for (FriendInfo info : _info)
		{
			writeD(info.objId); // character id
			writeS(info.name);
			writeD(info.online ? 0x01 : 0x00); // online
			writeD(info.online ? info.objId : 0x00); // object id if online
		}
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
