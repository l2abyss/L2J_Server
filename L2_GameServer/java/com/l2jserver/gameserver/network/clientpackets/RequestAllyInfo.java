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
package com.l2jserver.gameserver.network.clientpackets;

import com.l2jserver.gameserver.datatables.ClanTable;
import com.l2jserver.gameserver.model.L2Clan;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;

/**
 * This class ...
 *
 * @version $Revision: 1479 $ $Date: 2005-11-09 00:47:42 +0100 (mer., 09 nov. 2005) $
 */
public final class RequestAllyInfo extends L2GameClientPacket
{
	private static final String _C__8E_REQUESTALLYINFO = "[C] 8E RequestAllyInfo";
	
	
	@Override
	public void readImpl()
	{
		
	}
	
	@Override
	protected void runImpl()
	{
		final L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;
		
		SystemMessage sm;
		if (activeChar.getAllyId() == 0)
		{
			sm = new SystemMessage(SystemMessageId.NO_CURRENT_ALLIANCES);
			sendPacket(sm);
			return;
		}
		
		sm = new SystemMessage(SystemMessageId.ALLIANCE_INFO_HEAD);
		sendPacket(sm);
		sm = new SystemMessage(SystemMessageId.ALLIANCE_NAME_S1);
		sm.addString(activeChar.getClan().getAllyName());
		sendPacket(sm);
		
		int clanCount = 0;
		int totalMembers = 0;
		int onlineMembers = 0;
		for (L2Clan clan : ClanTable.getInstance().getClans())
		{
			if (clan.getAllyId() != activeChar.getAllyId())
				continue;
			
			clanCount++;
			totalMembers += clan.getMembersCount();
			onlineMembers += clan.getOnlineMembersCount();
		}
		sm = new SystemMessage(SystemMessageId.CONNECTION_S1_TOTAL_S2);
		sm.addNumber(onlineMembers);
		sm.addNumber(totalMembers);
		sendPacket(sm);
		
		final L2Clan leaderClan = ClanTable.getInstance().getClan(activeChar.getAllyId());
		sm = new SystemMessage(SystemMessageId.ALLIANCE_LEADER_S2_OF_S1);
		sm.addString(leaderClan.getName());
		sm.addString(leaderClan.getLeaderName());
		sendPacket(sm);
		
		sm = new SystemMessage(SystemMessageId.ALLIANCE_CLAN_TOTAL_S1);
		sm.addNumber(clanCount);
		sendPacket(sm);
		
		sm = new SystemMessage(SystemMessageId.CLAN_INFO_HEAD);
		for (L2Clan clan : ClanTable.getInstance().getClans())
		{
			if (clan.getAllyId() != activeChar.getAllyId())
				continue;
			
			sendPacket(sm); // send head or separator
			sm = new SystemMessage(SystemMessageId.CLAN_INFO_NAME);
			sm.addString(clan.getName());
			sendPacket(sm);
			sm = new SystemMessage(SystemMessageId.CLAN_INFO_LEADER);
			sm.addString(clan.getLeaderName());
			sendPacket(sm);
			sm = new SystemMessage(SystemMessageId.CLAN_INFO_LEVEL);
			sm.addNumber(clan.getLevel());
			sendPacket(sm);
			sm = new SystemMessage(SystemMessageId.CLAN_INFO_SEPARATOR);
		}
		sm = new SystemMessage(SystemMessageId.CLAN_INFO_FOOT);
		sendPacket(sm);
	}
	
	/* (non-Javadoc)
	 * @see com.l2jserver.gameserver.clientpackets.ClientBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _C__8E_REQUESTALLYINFO;
	}
}
