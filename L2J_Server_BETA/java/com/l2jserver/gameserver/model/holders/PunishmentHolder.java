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
package com.l2jserver.gameserver.model.holders;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.l2jserver.gameserver.model.punishment.PunishmentTask;
import com.l2jserver.gameserver.model.punishment.PunishmentType;

/**
 * @author UnAfraid
 */
public class PunishmentHolder
{
	private final Map<Object, Map<PunishmentType, PunishmentTask>> _holder = new ConcurrentHashMap<>();
	
	/**
	 * Stores the punishment task in the Map.
	 * @param task
	 */
	public void addPunishment(PunishmentTask task)
	{
		if (!task.isExpired())
		{
			if (!_holder.containsKey(task.getKey()))
			{
				_holder.put(task.getKey(), new ConcurrentHashMap<PunishmentType, PunishmentTask>());
			}
			_holder.get(task.getKey()).put(task.getType(), task);
		}
	}
	
	/**
	 * Removes previously stopped task from the Map.
	 * @param task
	 */
	public void stopPunishment(PunishmentTask task)
	{
		if (_holder.containsKey(task.getKey()))
		{
			task.stopPunishment();
			final Map<PunishmentType, PunishmentTask> punishments = _holder.get(task.getKey());
			punishments.remove(task.getType());
			if (punishments.isEmpty())
			{
				_holder.remove(task.getKey());
			}
		}
	}
	
	/**
	 * @param key
	 * @param type
	 * @return {@code true} if Map contains the current key and type, {@code false} otherwise.
	 */
	public boolean hasPunishment(Object key, PunishmentType type)
	{
		return getPunishment(key, type) != null;
	}
	
	/**
	 * @param key
	 * @param type
	 * @return {@link PunishmentTask} by specified key and type if exists, null otherwise.
	 */
	public PunishmentTask getPunishment(Object key, PunishmentType type)
	{
		if (_holder.containsKey(key))
		{
			return _holder.get(key).get(type);
		}
		return null;
	}
}
