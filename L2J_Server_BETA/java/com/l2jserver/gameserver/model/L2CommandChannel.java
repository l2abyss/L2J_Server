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

import java.util.List;

import javolution.util.FastList;

import com.l2jserver.Config;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.interfaces.IL2Procedure;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.CreatureSay;
import com.l2jserver.gameserver.network.serverpackets.ExCloseMPCC;
import com.l2jserver.gameserver.network.serverpackets.ExMPCCPartyInfoUpdate;
import com.l2jserver.gameserver.network.serverpackets.ExOpenMPCC;
import com.l2jserver.gameserver.network.serverpackets.L2GameServerPacket;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;

/**
 * @author chris_00
 */
public class L2CommandChannel extends AbstractPlayerGroup
{
	private final List<L2Party> _partys;
	private L2PcInstance _commandLeader = null;
	private int _channelLvl;
	
	/**
	 * Creates a New Command Channel and Add the Leaders party to the CC
	 * @param leader
	 */
	public L2CommandChannel(L2PcInstance leader)
	{
		_commandLeader = leader;
		_partys = new FastList<L2Party>().shared();
		_partys.add(leader.getParty());
		_channelLvl = leader.getParty().getLevel();
		leader.getParty().setCommandChannel(this);
		leader.getParty().broadcastMessage(SystemMessageId.COMMAND_CHANNEL_FORMED);
		leader.getParty().broadcastPacket(ExOpenMPCC.STATIC_PACKET);
	}
	
	/**
	 * Adds a Party to the Command Channel
	 * @param party
	 */
	public void addParty(L2Party party)
	{
		if (party == null)
		{
			return;
		}
		// Update the CCinfo for existing players
		broadcastPacket(new ExMPCCPartyInfoUpdate(party, 1));
		
		_partys.add(party);
		if (party.getLevel() > _channelLvl)
		{
			_channelLvl = party.getLevel();
		}
		party.setCommandChannel(this);
		party.broadcastPacket(SystemMessage.getSystemMessage(SystemMessageId.JOINED_COMMAND_CHANNEL));
		party.broadcastPacket(ExOpenMPCC.STATIC_PACKET);
	}
	
	/**
	 * Removes a Party from the Command Channel
	 * @param party
	 */
	public void removeParty(L2Party party)
	{
		if (party == null)
		{
			return;
		}
		
		_partys.remove(party);
		_channelLvl = 0;
		for (L2Party pty : _partys)
		{
			if (pty.getLevel() > _channelLvl)
			{
				_channelLvl = pty.getLevel();
			}
		}
		party.setCommandChannel(null);
		party.broadcastPacket(new ExCloseMPCC());
		if (_partys.size() < 2)
		{
			broadcastPacket(SystemMessage.getSystemMessage(SystemMessageId.COMMAND_CHANNEL_DISBANDED));
			disbandChannel();
		}
		else
		{
			// Update the CCinfo for existing players
			broadcastPacket(new ExMPCCPartyInfoUpdate(party, 0));
		}
	}
	
	/**
	 * disbands the whole Command Channel
	 */
	public void disbandChannel()
	{
		if (_partys != null)
		{
			for (L2Party party : _partys)
			{
				if (party != null)
				{
					removeParty(party);
				}
			}
			_partys.clear();
		}
	}
	
	/**
	 * @return overall member count of the Command Channel
	 */
	@Override
	public int getMemberCount()
	{
		int count = 0;
		for (L2Party party : _partys)
		{
			if (party != null)
			{
				count += party.getMemberCount();
			}
		}
		return count;
	}
	
	/**
	 * Broadcast packet to every channel member
	 * @param gsp
	 * @deprecated
	 * @see L2CommandChannel#broadcastPacket(L2GameServerPacket)
	 */
	@Deprecated
	public void broadcastToChannelMembers(L2GameServerPacket gsp)
	{
		broadcastPacket(gsp);
	}
	
	@Deprecated
	public void broadcastCSToChannelMembers(CreatureSay gsp, L2PcInstance broadcaster)
	{
		broadcastCreatureSay(gsp, broadcaster);
	}
	
	/**
	 * @return list of Parties in Command Channel
	 */
	public List<L2Party> getPartys()
	{
		return _partys;
	}
	
	/**
	 * @return list of all Members in Command Channel
	 */
	@Override
	public List<L2PcInstance> getMembers()
	{
		List<L2PcInstance> members = new FastList<L2PcInstance>().shared();
		for (L2Party party : getPartys())
		{
			members.addAll(party.getMembers());
		}
		return members;
	}
	
	/**
	 * @return Level of CC
	 */
	@Override
	public int getLevel()
	{
		return _channelLvl;
	}
	
	/**
	 * @param leader the leader of the Command Channel
	 */
	public void setChannelLeader(L2PcInstance leader)
	{
		_commandLeader = leader;
	}
	
	/**
	 * @return the leader of the Command Channel
	 * @deprecated
	 * @see L2CommandChannel#getLeader()
	 */
	@Deprecated
	public L2PcInstance getChannelLeader()
	{
		return getLeader();
	}
	
	/**
	 * @param obj
	 * @return true if proper condition for RaidWar
	 */
	public boolean meetRaidWarCondition(L2Object obj)
	{
		if (!((obj instanceof L2Character) && ((L2Character) obj).isRaid()))
		{
			return false;
		}
		return (getMemberCount() >= Config.LOOT_RAIDS_PRIVILEGE_CC_SIZE);
	}
	
	/**
	 * @return the leader of the Command Channel
	 */
	@Override
	public L2PcInstance getLeader()
	{
		return _commandLeader;
	}
	
	@Override
	public boolean containsPlayer(L2PcInstance player)
	{
		if ((_partys != null) && !_partys.isEmpty())
		{
			for (L2Party party : _partys)
			{
				if (party.containsPlayer(player))
				{
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Iterates over CC without need to allocate any new list
	 * @see com.l2jserver.gameserver.model.AbstractPlayerGroup#forEachMember(IL2Procedure)
	 */
	@Override
	public boolean forEachMember(IL2Procedure<L2PcInstance> procedure)
	{
		if ((_partys != null) && !_partys.isEmpty())
		{
			for (L2Party party : _partys)
			{
				if (!party.forEachMember(procedure))
				{
					return false;
				}
			}
		}
		return true;
	}
}
