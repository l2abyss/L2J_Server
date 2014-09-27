/*
 * Copyright (C) 2004-2014 L2J Server
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
package com.l2jserver.gameserver.model.skills;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.l2jserver.gameserver.model.actor.L2Character;

/**
 * @author UnAfraid
 */
public final class SkillChannelized
{
	private final Map<Integer, Map<Integer, L2Character>> _channelizers = new ConcurrentHashMap<>();
	
	public void addChannelizer(int skillId, L2Character channelizer)
	{
		if (!_channelizers.containsKey(skillId))
		{
			_channelizers.put(skillId, new ConcurrentHashMap<Integer, L2Character>());
		}
		_channelizers.get(skillId).put(channelizer.getObjectId(), channelizer);
	}
	
	public void removeChannelizer(int skillId, L2Character channelizer)
	{
		if (_channelizers.containsKey(skillId))
		{
			_channelizers.get(skillId).remove(channelizer.getObjectId());
		}
	}
	
	public int getChannerlizersSize(int skillId)
	{
		if (_channelizers.containsKey(skillId))
		{
			return _channelizers.get(skillId).size();
		}
		return 0;
	}
	
	public Map<Integer, L2Character> getChannelizers(int skillId)
	{
		return _channelizers.get(skillId);
	}
	
	public void abortChannelization()
	{
		for (Map<Integer, L2Character> map : _channelizers.values())
		{
			for (L2Character channelizer : map.values())
			{
				channelizer.abortCast();
			}
		}
		_channelizers.clear();
	}
	
	public boolean isChannelized()
	{
		for (Map<Integer, L2Character> map : _channelizers.values())
		{
			if (!map.isEmpty())
			{
				return true;
			}
		}
		return false;
	}
}
