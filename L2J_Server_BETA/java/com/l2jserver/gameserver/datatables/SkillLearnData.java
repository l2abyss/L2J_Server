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
package com.l2jserver.gameserver.datatables;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Node;

import com.l2jserver.gameserver.engines.DocumentParser;
import com.l2jserver.gameserver.model.actor.templates.L2NpcTemplate;
import com.l2jserver.gameserver.model.base.ClassId;

/**
 * Holds all skill learn data for all npcs.
 * @author xban1x
 */
public final class SkillLearnData extends DocumentParser
{
	private final Map<Integer, List<ClassId>> _skillLearn = new HashMap<>();
	
	protected SkillLearnData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		parseDatapackFile("data/skillLearn.xml");
		_log.info(getClass().getSimpleName() + ": Loaded " + _skillLearn.size() + " Skill Learn data.");
	}
	
	@Override
	protected void parseDocument()
	{
		for (Node node = getCurrentDocument().getFirstChild(); node != null; node = node.getNextSibling())
		{
			if ("list".equalsIgnoreCase(node.getNodeName()))
			{
				for (Node list_node = node.getFirstChild(); list_node != null; list_node = list_node.getNextSibling())
				{
					if ("npc".equalsIgnoreCase(list_node.getNodeName()))
					{
						final List<ClassId> classIds = new ArrayList<>();
						for (Node c = list_node.getFirstChild(); c != null; c = c.getNextSibling())
						{
							if ("classId".equalsIgnoreCase(c.getNodeName()))
							{
								classIds.add(ClassId.getClassId(Integer.parseInt(c.getTextContent())));
							}
						}
						_skillLearn.put(parseInteger(list_node.getAttributes(), "id"), classIds);
					}
				}
			}
		}
	}
	
	public void setAllNpcSkillLearn(Map<Integer, L2NpcTemplate> npcs)
	{
		for (int npcId : _skillLearn.keySet())
		{
			final L2NpcTemplate npc = npcs.get(npcId);
			if (npc == null)
			{
				_log.warning(getClass().getSimpleName() + ": Error getting NPC template Id " + npcId + " while trying to load skill trainer data.");
				continue;
			}
			
			npc.addTeachInfo(_skillLearn.get(npcId));
		}
	}
	
	public void setNpcSkillLearn(int npcId)
	{
		final L2NpcTemplate npc = NpcTable.getInstance().getTemplate(npcId);
		if (npc == null)
		{
			_log.warning(getClass().getSimpleName() + ": Error getting NPC template Id " + npcId + " while trying to load skill trainer data.");
			return;
		}
		
		npc.addTeachInfo(_skillLearn.get(npcId));
	}
	
	public static SkillLearnData getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected final static SkillLearnData _instance = new SkillLearnData();
	}
}
