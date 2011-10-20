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

import java.util.Map;

import javolution.util.FastMap;

import com.l2jserver.gameserver.model.L2Skill.SkillTargetType;

/**
 * @author UnAfraid
 */
public class TargetHandler
{
	private final Map<Enum<SkillTargetType>, ISkillTargetTypeHandler> _datatable;
	
	public static TargetHandler getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private TargetHandler()
	{
		_datatable = new FastMap<Enum<SkillTargetType>, ISkillTargetTypeHandler>();
	}
	
	public void registerSkillTargetType(ISkillTargetTypeHandler handler)
	{
		_datatable.put(handler.getTargetType(), handler);
	}
	
	public ISkillTargetTypeHandler getSkillTarget(Enum<SkillTargetType> skillTargetType)
	{
		return _datatable.get(skillTargetType);
	}
	
	public int size()
	{
		return _datatable.size();
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final TargetHandler _instance = new TargetHandler();
	}
}
