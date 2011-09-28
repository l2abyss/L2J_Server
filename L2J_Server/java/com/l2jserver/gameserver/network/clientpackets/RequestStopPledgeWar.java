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

import gnu.trove.TObjectProcedure;

import com.l2jserver.gameserver.datatables.ClanTable;
import com.l2jserver.gameserver.model.L2Clan;
import com.l2jserver.gameserver.model.L2ClanMember;
import com.l2jserver.gameserver.model.L2World;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.ActionFailed;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;
import com.l2jserver.gameserver.taskmanager.AttackStanceTaskManager;


public final class RequestStopPledgeWar extends L2GameClientPacket
{
	private static final String _C__05_REQUESTSTOPPLEDGEWAR = "[C] 05 RequestStopPledgeWar";
	//private static Logger _log = Logger.getLogger(RequestStopPledgeWar.class.getName());
	
	private String _pledgeName;
	
	@Override
	protected void readImpl()
	{
		_pledgeName = readS();
	}
	
	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if (player == null) return;
		L2Clan playerClan = player.getClan();
		if (playerClan == null) return;
		
		L2Clan clan = ClanTable.getInstance().getClanByName(_pledgeName);
		
		if (clan == null)
		{
			player.sendMessage("No such clan.");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (!playerClan.isAtWarWith(clan.getClanId()))
		{
			player.sendMessage("You aren't at war with this clan.");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Check if player who does the request has the correct rights to do it
		if ((player.getClanPrivileges() & L2Clan.CP_CL_PLEDGE_WAR) != L2Clan.CP_CL_PLEDGE_WAR )
		{
			player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT));
			return;
		}
		
		//_log.info("RequestStopPledgeWar: By leader or authorized player: " + playerClan.getLeaderName() + " of clan: "
		//	+ playerClan.getName() + " to clan: " + _pledgeName);
		
		//        L2PcInstance leader = L2World.getInstance().getPlayer(clan.getLeaderName());
		//        if(leader != null && leader.isOnline() == 0)
		//        {
		//            player.sendMessage("Clan leader isn't online.");
		//            player.sendPacket(ActionFailed.STATIC_PACKET);
		//            return;
		//        }
		
		//        if (leader.isProcessingRequest())
		//        {
		//            SystemMessage sm = SystemMessage.getSystemMessage(SystemMessage.S1_IS_BUSY_TRY_LATER);
		//            sm.addString(leader.getName());
		//            player.sendPacket(sm);
		//            return;
		//        }
		
		for (L2ClanMember member : playerClan.getMembers())
		{
			if (member == null || member.getPlayerInstance() == null)
				continue;
			if (AttackStanceTaskManager.getInstance().getAttackStanceTask(member.getPlayerInstance()))
			{
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CANT_STOP_CLAN_WAR_WHILE_IN_COMBAT));
				return;
			}
		}
		
		ClanTable.getInstance().deleteclanswars(playerClan.getClanId(), clan.getClanId());
		L2World.getInstance().forEachPlayer(new ForEachPlayerBroadcastUserInfo(clan, player));
	}
	
	private final class ForEachPlayerBroadcastUserInfo implements TObjectProcedure<L2PcInstance>
	{
		L2PcInstance _player;
		L2Clan _cln;
		private ForEachPlayerBroadcastUserInfo(L2Clan clan, L2PcInstance player)
		{
			_cln = clan;
			_player = player;
		}
		@Override
		public final boolean execute(final L2PcInstance cha)
		{
			if (cha.getClan() == _player.getClan() || cha.getClan() == _cln)
				cha.broadcastUserInfo();
			return true;
		}
	}
	
	@Override
	public String getType()
	{
		return _C__05_REQUESTSTOPPLEDGEWAR;
	}
}