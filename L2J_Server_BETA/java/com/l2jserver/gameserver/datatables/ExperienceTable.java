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
package com.l2jserver.gameserver.datatables;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.l2jserver.gameserver.engines.DocumentParser;

/**
 * This class holds the Experience points for each level for players and pets.
 * @author mrTJO
 */
public final class ExperienceTable extends DocumentParser
{
	private final Map<Integer, Long> _expTable = new HashMap<>();
	
	private byte MAX_LEVEL;
	private byte MAX_PET_LEVEL;
	
	/**
	 * Instantiates a new experience table.
	 */
	protected ExperienceTable()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_expTable.clear();
		parseDatapackFile("data/stats/experience.xml");
		_log.info(getClass().getSimpleName() + ": Loaded " + _expTable.size() + " levels.");
		_log.info(getClass().getSimpleName() + ": Max Player Level is: " + (MAX_LEVEL - 1));
		_log.info(getClass().getSimpleName() + ": Max Pet Level is: " + (MAX_PET_LEVEL - 1));
	}
	
	@Override
	protected void parseDocument()
	{
		final Node table = getCurrentDocument().getFirstChild();
		final NamedNodeMap tableAttr = table.getAttributes();
		
		MAX_LEVEL = (byte) (Byte.parseByte(tableAttr.getNamedItem("maxLevel").getNodeValue()) + 1);
		MAX_PET_LEVEL = (byte) (Byte.parseByte(tableAttr.getNamedItem("maxPetLevel").getNodeValue()) + 1);
		
		NamedNodeMap attrs;
		for (Node n = table.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("experience".equals(n.getNodeName()))
			{
				attrs = n.getAttributes();
				_expTable.put(parseInteger(attrs, "level"), parseLong(attrs, "tolevel"));
			}
		}
	}
	
	/**
	 * Gets the exp for level.
	 * @param level the level required.
	 * @return the experience points required to reach the given level.
	 */
	public long getExpForLevel(int level)
	{
		return _expTable.get(level);
	}
	
	/**
	 * Gets the max level.
	 * @return the maximum level acquirable by a player.
	 */
	public byte getMaxLevel()
	{
		return MAX_LEVEL;
	}
	
	/**
	 * Gets the max pet level.
	 * @return the maximum level acquirable by a pet.
	 */
	public byte getMaxPetLevel()
	{
		return MAX_PET_LEVEL;
	}
	
	/**
	 * Gets the single instance of ExperienceTable.
	 * @return single instance of ExperienceTable
	 */
	public static ExperienceTable getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final ExperienceTable _instance = new ExperienceTable();
	}
}
