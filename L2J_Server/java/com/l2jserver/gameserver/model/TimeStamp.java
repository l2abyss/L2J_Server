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

import com.l2jserver.gameserver.model.items.instance.L2ItemInstance;
import com.l2jserver.gameserver.model.skills.L2Skill;

/**
 * Simple class containing all necessary information to maintain<br>
 * valid time stamps and reuse for skills and items reuse upon re-login.<br>
 * <b>Filter this carefully as it becomes redundant to store reuse for small delays.</b>
 * @author Yesod, Zoey76
 */
public class TimeStamp
{
	private final int _id1; // Item or Skill Id.
	private final int _id2; // Item Object Id or Skill Level.
	private final long _reuse;
	private final long _stamp;
	private final int _group;
	
	/**
	 * @param skill the skill upon the stamp will be created.
	 * @param reuse the reuse time for this skill.
	 */
	public TimeStamp(L2Skill skill, long reuse)
	{
		_id1 = skill.getId();
		_id2 = skill.getLevel();
		_reuse = reuse;
		_stamp = System.currentTimeMillis() + reuse;
		_group = -1;
	}
	
	/**
	 * @param skill the skill upon the stamp will be created.
	 * @param reuse the reuse time for this skill.
	 * @param systime overrides the system time with a customized one.
	 */
	public TimeStamp(L2Skill skill, long reuse, long systime)
	{
		_id1 = skill.getId();
		_id2 = skill.getLevel();
		_reuse = reuse;
		_stamp = systime;
		_group = -1;
	}
	
	/**
	 * @param item the item upon the stamp will be created.
	 * @param reuse the reuse time for this item.
	 */
	public TimeStamp(L2ItemInstance item, long reuse)
	{
		_id1 = item.getItemId();
		_id2 = item.getObjectId();
		_reuse = reuse;
		_stamp = System.currentTimeMillis() + reuse;
		_group = item.getSharedReuseGroup();
	}
	
	/**
	 * @param item the item upon the stamp will be created.
	 * @param reuse the reuse time for this item.
	 * @param systime overrides the system time with a customized one.
	 */
	public TimeStamp(L2ItemInstance item, long reuse, long systime)
	{
		_id1 = item.getItemId();
		_id2 = item.getObjectId();
		_reuse = reuse;
		_stamp = systime;
		_group = item.getSharedReuseGroup();
	}
	
	/**
	 * @return the time stamp, either the system time where this time stamp was created or the custom time assigned.
	 */
	public long getStamp()
	{
		return _stamp;
	}
	
	/**
	 * @return the first Id for the item, the item Id.
	 */
	public int getItemId()
	{
		return _id1;
	}
	
	/**
	 * @return the second Id for the item, the item object Id.
	 */
	public int getItemObjectId()
	{
		return _id2;
	}
	
	/**
	 * @return the skill Id.
	 */
	public int getSkillId()
	{
		return _id1;
	}
	
	/**
	 * @return the skill level.
	 */
	public int getSkillLvl()
	{
		return _id2;
	}
	
	/**
	 * @return the reuse set for this Item/Skill.
	 */
	public long getReuse()
	{
		return _reuse;
	}
	
	/**
	 * @return the shared reuse group for the item, -1 for skills.
	 */
	public int getSharedReuseGroup()
	{
		return _group;
	}
	
	/**
	 * @return the remaining time for this time stamp to expire.
	 */
	public long getRemaining()
	{
		return Math.max(_stamp - System.currentTimeMillis(), 0);
	}
	
	/**
	 * Check if the reuse delay has passed and if it has not then update the stored reuse time according to what is currently remaining on the delay.
	 * @return {@code true} if this time stamp has expired, {@code false} otherwise.
	 */
	public boolean hasNotPassed()
	{
		return System.currentTimeMillis() < _stamp;
	}
}
