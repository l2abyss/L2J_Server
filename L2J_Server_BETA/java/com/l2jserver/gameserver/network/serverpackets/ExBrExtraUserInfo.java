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

import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;


/**
 * @author Kerberos
 */
public class ExBrExtraUserInfo extends L2GameServerPacket
{
	private final int _charObjId;
	private final int _val;
	
	public ExBrExtraUserInfo(L2PcInstance player)
	{
		_charObjId = player.getObjectId();
		_val = player.getEventEffectId();
		_invisible = player.getAppearance().getInvisible();
	}
	
	@Override
	protected final void writeImpl()
	{
		
		writeC(0xFE);
		writeH(0xDA);
		writeD(_charObjId); //object ID of Player
		writeD(_val);		// event effect id
		//writeC(0x00);		// Event flag, added only if event is active
		
	}
	
	/* (non-Javadoc)
	 * @see com.l2jserver.gameserver.serverpackets.ServerBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return "[S] FE:DA ExBrExtraUSerInfo".intern();
	}
}
