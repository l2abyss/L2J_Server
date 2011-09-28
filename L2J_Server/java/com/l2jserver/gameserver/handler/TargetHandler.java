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

import java.io.File;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastMap;

import com.l2jserver.gameserver.model.L2Skill.SkillTargetType;
import com.l2jserver.gameserver.scripting.L2ScriptEngineManager;

/**
 * @author UnAfraid
 */
public class TargetHandler
{
	private static Logger _log = Logger.getLogger(TargetHandler.class.getName());
	
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
		Enum<SkillTargetType> ids = handler.getTargetType();
		if (_datatable.containsKey(ids))
			_log.log(Level.FINE, "Target Handler: " + ids.toString() + " is already registered into the map!");
		_datatable.put(ids, handler);
	}
	
	public ISkillTargetTypeHandler getSkillTarget(Enum<SkillTargetType> skillTargetType)
	{
		Enum<SkillTargetType> target = skillTargetType;
		return _datatable.get(target);
	}
	
	public void executeScript()
	{
		try
		{
			File file = new File(L2ScriptEngineManager.SCRIPT_FOLDER, "handlers/TargetMasterHandler.java");
			L2ScriptEngineManager.getInstance().executeScript(file);
		}
		catch (Exception e)
		{
			_log.warning("Problems while running TargetMansterHandler");
			e.printStackTrace();
		}
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
