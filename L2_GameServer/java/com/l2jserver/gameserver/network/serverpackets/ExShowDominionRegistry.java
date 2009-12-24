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

import com.l2jserver.gameserver.datatables.ClanTable;
import com.l2jserver.gameserver.instancemanager.CastleManager;
import com.l2jserver.gameserver.model.L2Clan;
import com.l2jserver.gameserver.model.entity.Castle;

/**
 * format: dSSSdddddddd (dd(d))
 * @author  GodKratos
 */
public class ExShowDominionRegistry extends L2GameServerPacket
{
	private int _territoryId;
	private int _clanReq = 0x00;
	private int _mercReq = 0x00;
	private String _clanName = "";
	private String _clanLeader = "";
	private String _allyName = "";
	private int _warTime = (int) (Calendar.getInstance().getTimeInMillis() / 1000);
	private int _currentTime = (int) (Calendar.getInstance().getTimeInMillis() / 1000);

	public ExShowDominionRegistry(int castleId)
	{
		_territoryId = 0x50 + castleId;
		int owner = CastleManager.getInstance().getCastleById(castleId).getOwnerId();
		if (owner != 0)
		{
			try
			{
				L2Clan clan = ClanTable.getInstance().getClan(owner);
				_clanName = clan.getName();
				_clanLeader = clan.getLeaderName();
				_allyName = clan.getAllyName();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	/**
	 * @see com.l2jserver.gameserver.network.serverpackets.L2GameServerPacket#getType()
	 */
	@Override
	public String getType()
	{
		return "[S] FE:90 ExShowDominionRegistry";
	}

	/**
	 * @see com.l2jserver.gameserver.network.serverpackets.L2GameServerPacket#writeImpl()
	 */
	@Override
	protected void writeImpl()
	{
		writeC(0xfe);
		writeH(0x90);
		writeD(_territoryId); // Current Territory Id
		writeS(_clanName); // Owners Clan
		writeS(_clanLeader); // Owner Clan Leader
		writeS(_allyName); // Owner Alliance
		writeD(_clanReq); // Clan Request
		writeD(_mercReq); // Merc Request
		writeD(_warTime); // War Time
		writeD(_currentTime); // Current Time
		writeD(0x00); // unknown
		writeD(0x00); // unknown
		writeD(0x01); // unknown
		List<Castle> castles = CastleManager.getInstance().getCastles();
		writeD(castles.size());
		for (Castle castle : castles)
		{
			writeD(0x50 + castle.getCastleId()); // Territory Id
			writeD(0x01); // Emblem Count
			writeD(0x50 + castle.getCastleId()); // Emblem ID - should be in for loop for emblem
												 // count
		}
	}
}
