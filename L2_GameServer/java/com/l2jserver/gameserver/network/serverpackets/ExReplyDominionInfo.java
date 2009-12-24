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

import java.util.Calendar;
import java.util.List;
import java.util.logging.Logger;

import com.l2jserver.gameserver.datatables.ClanTable;
import com.l2jserver.gameserver.instancemanager.CastleManager;
import com.l2jserver.gameserver.model.entity.Castle;

/**
 *
 * @author  JIV
 */
public class ExReplyDominionInfo extends L2GameServerPacket
{
	/**
	 * @see com.l2jserver.gameserver.network.serverpackets.L2GameServerPacket#getType()
	 */
	private static Logger _log = Logger.getLogger(ExReplyDominionInfo.class.getName());
	private int _warTime = (int) (Calendar.getInstance().getTimeInMillis() / 1000);

	/**
	 * @see com.l2jserver.gameserver.network.serverpackets.L2GameServerPacket#writeImpl()
	 */
	@Override
	protected void writeImpl()
	{
		writeC(0xfe);
		writeH(0x92);
		List<Castle> castles = CastleManager.getInstance().getCastles();
		writeD(castles.size());
		for (Castle castle : castles)
		{
			writeD(0x50 + castle.getCastleId()); // territory ID
			writeS(castle.getName().toLowerCase() + "_dominion"); // territory name
			if (castle.getOwnerId() > 0)
			{
				if (ClanTable.getInstance().getClan(castle.getOwnerId()) != null)
					writeS(ClanTable.getInstance().getClan(castle.getOwnerId()).getName());
				else
				{
					_log.warning("Castle owner with no name! Castle: " + castle.getName()
					        + " has an OwnerId = " + castle.getOwnerId()
					        + " who does not have a  name!");
					writeS("");
				}
			}
			else
				writeS("");

			writeD(1); // wards count
			writeD(0x50 + castle.getCastleId()); // territory ID's
			writeD(_warTime);
		}
	}

	@Override
	public String getType()
	{
		return "[S] FE:92 ExReplyDominionInfo";
	}
}
