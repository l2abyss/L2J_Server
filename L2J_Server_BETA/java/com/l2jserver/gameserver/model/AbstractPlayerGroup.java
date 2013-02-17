/*
 * Copyright (C) 2004-2013 L2J Server
 * 
 * This file is part of L2J Server.
 * 
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.l2jserver.gameserver.model;

import java.util.ArrayList;
import java.util.List;

import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.interfaces.IL2Procedure;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.CreatureSay;
import com.l2jserver.gameserver.network.serverpackets.L2GameServerPacket;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;
import com.l2jserver.util.Rnd;

/**
 * @author Battlecruiser
 */
public abstract class AbstractPlayerGroup
{
	/**
	 * @return all members of this group
	 */
	public abstract List<L2PcInstance> getMembers();
	
	/**
	 * @return object IDs of all members of this group
	 */
	public List<Integer> getMembersObjectId()
	{
		final List<Integer> ids = new ArrayList<>();
		forEachMember(new IL2Procedure<L2PcInstance>()
		{
			@Override
			public boolean execute(L2PcInstance member)
			{
				ids.add(member.getObjectId());
				return true;
			}
		});
		return ids;
	}
	
	/**
	 * @return leader of this group
	 */
	public abstract L2PcInstance getLeader();
	
	/**
	 * @return the leader's object ID
	 */
	public int getLeaderObjectId()
	{
		return getLeader().getObjectId();
	}
	
	/**
	 * @return count of all players in this group
	 */
	public int getMemberCount()
	{
		return getMembers().size();
	}
	
	/**
	 * @return level of this group
	 */
	public abstract int getLevel();
	
	/**
	 * Broadcast packet to every member of this group
	 * @param packet packet to broadcast
	 */
	public void broadcastPacket(final L2GameServerPacket packet)
	{
		forEachMember(new IL2Procedure<L2PcInstance>()
		{
			@Override
			public boolean execute(L2PcInstance member)
			{
				if (member != null)
				{
					member.sendPacket(packet);
				}
				return true;
			}
		});
	}
	
	/**
	 * Broadcasts a System Message to this group
	 * @param message System Message to broadcast
	 */
	public void broadcastMessage(SystemMessageId message)
	{
		broadcastPacket(SystemMessage.getSystemMessage(message));
	}
	
	/**
	 * Broadcasts a string message to this group
	 * @param text to broadcast
	 */
	public void broadcastString(String text)
	{
		broadcastPacket(SystemMessage.sendString(text));
	}
	
	public void broadcastCreatureSay(final CreatureSay msg, final L2PcInstance broadcaster)
	{
		forEachMember(new IL2Procedure<L2PcInstance>()
		{
			@Override
			public boolean execute(L2PcInstance member)
			{
				if ((member != null) && !BlockList.isBlocked(member, broadcaster))
				{
					member.sendPacket(msg);
				}
				return true;
			}
		});
	}
	
	/**
	 * @param player to be contained
	 * @return {@code true} if this group contains player
	 */
	public boolean containsPlayer(L2PcInstance player)
	{
		return getMembers().contains(player);
	}
	
	/**
	 * @return random member of this group
	 */
	public L2PcInstance getRandomPlayer()
	{
		return getMembers().get(Rnd.get(getMemberCount()));
	}
	
	/**
	 * Iterates over the group and executes procedure on each member
	 * @param procedure to be executed on members, <br>
	 *            if it returns {@code true}, loop will continue, <br>
	 *            if it returns {@code false}, loop will break
	 * @return {@code false} if it was interrupted by a {@code false} return of the procedure
	 */
	public boolean forEachMember(IL2Procedure<L2PcInstance> procedure)
	{
		for (L2PcInstance player : getMembers())
		{
			if (!procedure.execute(player))
			{
				return false;
			}
		}
		return true;
	}
}
