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

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.l2jserver.Config;
import com.l2jserver.gameserver.engines.DocumentParser;
import com.l2jserver.gameserver.model.L2ArmorSet;
import com.l2jserver.gameserver.model.holders.SkillHolder;

/**
 * @author godson, Luno, UnAfraid
 */
public class ArmorSetsTable extends DocumentParser
{
	private Map<Integer, L2ArmorSet> _armorSets;
	
	public static ArmorSetsTable getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private ArmorSetsTable()
	{
		_armorSets = new HashMap<>();
		load();
	}
	
	private void load()
	{
		loadDirectory(new File(Config.DATAPACK_ROOT, "data/stats/armorsets"));
		_log.log(Level.INFO, getClass().getSimpleName() + ": Loaded " + _armorSets.size() + " Armor sets.");
	}
	
	@Override
	protected void parseDoc(Document doc)
	{
		NamedNodeMap attrs;
		L2ArmorSet set;
		for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("set".equalsIgnoreCase(d.getNodeName()))
					{
						set = new L2ArmorSet();
						for (Node a = d.getFirstChild(); a != null; a = a.getNextSibling())
						{
							attrs = a.getAttributes();
							switch (a.getNodeName())
							{
								case "chest":
								{
									set.addChest(getIntValue(attrs.getNamedItem("id")));
									break;
								}
								case "feet":
								{
									set.addFeet(getIntValue(attrs.getNamedItem("id")));
									break;
								}
								case "gloves":
								{
									set.addGloves(getIntValue(attrs.getNamedItem("id")));
									break;
								}
								case "head":
								{
									set.addHead(getIntValue(attrs.getNamedItem("id")));
									break;
								}
								case "legs":
								{
									set.addLegs(getIntValue(attrs.getNamedItem("id")));
									break;
								}
								case "shield":
								{
									set.addShield(getIntValue(attrs.getNamedItem("id")));
									break;
								}
								case "skill":
								{
									int skillId = getIntValue(attrs.getNamedItem("id"));
									int skillLevel = getIntValue(attrs.getNamedItem("level"));
									set.addSkill(new SkillHolder(skillId, skillLevel));
									break;
								}
								case "shield_skill":
								{
									int skillId = getIntValue(attrs.getNamedItem("id"));
									int skillLevel = getIntValue(attrs.getNamedItem("level"));
									set.addShieldSkill(new SkillHolder(skillId, skillLevel));
									break;
								}
								case "enchant6skill":
								{
									int skillId = getIntValue(attrs.getNamedItem("id"));
									int skillLevel = getIntValue(attrs.getNamedItem("level"));
									set.addEnchant6Skill(new SkillHolder(skillId, skillLevel));
									break;
								}
								case "con":
								{
									// TODO: Implement me
									break;
								}
								case "dex":
								{
									// TODO: Implement me
									break;
								}
								case "str":
								{
									// TODO: Implement me
									break;
								}
								case "men":
								{
									// TODO: Implement me
									break;
								}
								case "wit":
								{
									// TODO: Implement me
									break;
								}
								case "int":
								{
									// TODO: Implement me
									break;
								}
							}
						}
						_armorSets.put(set.getChestId(), set);
					}
				}
			}
		}
	}
	
	private int getIntValue(Node n)
	{
		return Integer.parseInt(n.getNodeValue());
	}
	
	public boolean setExists(int chestId)
	{
		return _armorSets.containsKey(chestId);
	}
	
	public L2ArmorSet getSet(int chestId)
	{
		return _armorSets.get(chestId);
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final ArmorSetsTable _instance = new ArmorSetsTable();
	}
}
