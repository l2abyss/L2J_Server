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
package com.l2jserver.gameserver.scripting.scriptengine.listeners.clan;

import com.l2jserver.gameserver.model.L2Clan;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.scripting.scriptengine.impl.L2JListener;

/**
 * Clan join and leave notifiers
 * @author TheOne
 */
public abstract class ClanMembershipListener extends L2JListener
{
	public ClanMembershipListener()
	{
		register();
	}
	
	/**
	 * A player just joined the clan
	 * @param player
	 * @param clan
	 * @return
	 */
	public abstract boolean onJoin(L2PcInstance player, L2Clan clan);
	
	/**
	 * A player just left the clan
	 * @param playerObjId
	 * @param clan
	 * @return
	 */
	public abstract boolean onLeave(int playerObjId, L2Clan clan);
	
	/**
	 * Fired when the clan leader changes
	 * @param clan
	 * @param newLeader
	 * @param oldLeader
	 * @return
	 */
	public abstract boolean onLeaderChange(L2Clan clan, L2PcInstance newLeader, L2PcInstance oldLeader);
	
	@Override
	public void register()
	{
		L2Clan.addClanMembershipListener(this);
	}
	
	@Override
	public void unregister()
	{
		L2Clan.removeClanMembershipListener(this);
	}
}
