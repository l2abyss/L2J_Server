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

//import java.util.Calendar; //signed time related
//import java.util.logging.Logger;

import java.util.Collection;

import com.l2jserver.gameserver.datatables.ClanTable;
import com.l2jserver.gameserver.instancemanager.CHSiegeManager;
import com.l2jserver.gameserver.model.L2Clan;
import com.l2jserver.gameserver.model.L2SiegeClan;
import com.l2jserver.gameserver.model.entity.Castle;
import com.l2jserver.gameserver.model.entity.ClanHall;
/**
 * Populates the Siege Attacker List in the SiegeInfo Window<BR>
 * <BR>
 * packet type id 0xca<BR>
 * format: cddddddd + dSSdddSSd<BR>
 * <BR>
 * c = ca<BR>
 * d = CastleID<BR>
 * d = unknow (0x00)<BR>
 * d = unknow (0x01)<BR>
 * d = unknow (0x00)<BR>
 * d = Number of Attackers Clans?<BR>
 * d = Number of Attackers Clans<BR>
 * { //repeats<BR>
 * d = ClanID<BR>
 * S = ClanName<BR>
 * S = ClanLeaderName<BR>
 * d = ClanCrestID<BR>
 * d = signed time (seconds)<BR>
 * d = AllyID<BR>
 * S = AllyName<BR>
 * S = AllyLeaderName<BR>
 * d = AllyCrestID<BR>
 *
 * @author KenM
 */
public final class SiegeAttackerList extends L2GameServerPacket
{
	private static final String _S__CA_SiegeAttackerList = "[S] ca SiegeAttackerList";
	//private static Logger _log = Logger.getLogger(SiegeAttackerList.class.getName());
	private Castle _castle;
	private ClanHall _hall;
	
	public SiegeAttackerList(Castle castle)
	{
		_castle = castle;
	}
	
	public SiegeAttackerList(ClanHall hall)
	{
		_hall = hall;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0xca);
		
		if(_castle != null)
		{
			writeD(_castle.getCastleId());
			writeD(0x00); //0
			writeD(0x01); //1
			writeD(0x00); //0
			int size = _castle.getSiege().getAttackerClans().size();
			if (size > 0)
			{
				L2Clan clan;
			
				writeD(size);
				writeD(size);
				for(L2SiegeClan siegeclan : _castle.getSiege().getAttackerClans())
				{
					clan = ClanTable.getInstance().getClan(siegeclan.getClanId());
					if (clan == null) continue;
				
					writeD(clan.getClanId());
					writeS(clan.getName());
					writeS(clan.getLeaderName());
					writeD(clan.getCrestId());
					writeD(0x00); //signed time (seconds) (not storated by L2J)
					writeD(clan.getAllyId());
					writeS(clan.getAllyName());
					writeS(""); //AllyLeaderName
					writeD(clan.getAllyCrestId());
				}
			}
			else
			{
				writeD(0x00);
				writeD(0x00);
			}
		}
		else 
		{
			writeD(_hall.getId());
			writeD(0x00); //0
			writeD(0x01); //1
			writeD(0x00); //0
			final Collection<L2SiegeClan> attackers = CHSiegeManager.getInstance().getSiegableHall(_hall.getId()).getAttackers();
			final int size = attackers.size();
			if (size > 0)
			{
				writeD(size);
				writeD(size);
				for(L2SiegeClan sClan : attackers)
				{
					final L2Clan clan = ClanTable.getInstance().getClan(sClan.getClanId());
					if (clan == null) continue;
				
					writeD(clan.getClanId());
					writeS(clan.getName());
					writeS(clan.getLeaderName());
					writeD(clan.getCrestId());
					writeD(0x00); //signed time (seconds) (not storated by L2J)
					writeD(clan.getAllyId());
					writeS(clan.getAllyName());
					writeS(""); //AllyLeaderName
					writeD(clan.getAllyCrestId());
				}
			}
			else
			{
				writeD(0x00);
				writeD(0x00);
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see com.l2jserver.gameserver.serverpackets.ServerBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__CA_SiegeAttackerList;
	}
	
}
