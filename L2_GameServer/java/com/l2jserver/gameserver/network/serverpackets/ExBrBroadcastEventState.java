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
 * @author Kerberos
 * @author mrTJO
 * Packet Format: dddddddSS
 */
public class ExBrBroadcastEventState extends L2GameServerPacket
{
	private int _eventId;
	private int _eventState;
	
	public static final int APRIL_FOOLS = 20090401;
	public static final int EVAS_INFERNO = 20090801;
	public static final int HALLOWEEN_EVENT = 20091031;
	public static final int RAISING_RUDOLPH = 20091225;
	public static final int LOVERS_JUBILEE = 20100214;

	public ExBrBroadcastEventState(int eventId, int eventState)
	{
		_eventId = eventId;
		_eventState = eventState;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xfe);
		writeH(0xbd);
		writeD(_eventId);
		writeD(_eventState);
		// Parameters
		writeD(0x00);
		writeD(0x00);
		writeD(0x00);
		writeD(0x00);
		writeD(0x00);
		writeD(0x00);
		writeD(0x00);
		writeS(null);
		writeS(null);
	}

	/* (non-Javadoc)
	 * @see com.l2jserver.gameserver.serverpackets.ServerBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return "[S] FE:BD ExBrBroadcastEventState";
	}
}
