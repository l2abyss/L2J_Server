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
package com.l2jserver.gameserver.handler;

import com.l2jserver.gameserver.model.L2Skill.SkillTargetType;

import javolution.util.FastMap;

/**
 * @author BiggBoss
 */
public class TargetListHandler
{
	private FastMap<SkillTargetType, ITargetListHandler> _datatable;
	
	private TargetListHandler()
	{
		_datatable = new FastMap<SkillTargetType, ITargetListHandler>();
	}
	
	public void registerNewTargetHandler(ITargetListHandler targetHandler)
	{
		SkillTargetType[] types = targetHandler.getTargetsType();
		for(SkillTargetType t : types)
		{
			_datatable.put(t, targetHandler);
		}
	}
	
	public ITargetListHandler getTargetHandler(SkillTargetType stt)
	{
		return _datatable.get(stt);
	}
	
	public int size()
	{
		return _datatable.size();
	}
	
	public static TargetListHandler getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		private static TargetListHandler _instance = new TargetListHandler();
	}
}
