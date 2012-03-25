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
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastList;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.l2jserver.Config;
import com.l2jserver.gameserver.model.L2ArmorSet;
import com.l2jserver.gameserver.model.holders.SkillHolder;
import com.l2jserver.util.XMLParser;
import com.l2jserver.util.file.filter.XMLFilter;

/**
 * @author godson, Luno
 */
public class ArmorSetsTable
{
	private static Logger _log = Logger.getLogger(ArmorSetsTable.class.getName());
	
	private FastList<L2ArmorSet> _armorSets;
	
	public static ArmorSetsTable getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private ArmorSetsTable()
	{
		_armorSets = new FastList<>();
		loadData();
	}
	
	private final class Parser extends XMLParser
	{
		public Parser(File f)
		{
			super(f);
		}
		
		@Override
		public void parseDoc(Document doc)
		{
			NamedNodeMap attrs;
			L2ArmorSet set;
			Node att = null;
			for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
			{
				if ("list".equalsIgnoreCase(n.getNodeName()))
				{
					for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					{
						if ("set".equalsIgnoreCase(d.getNodeName()))
						{
							set = new L2ArmorSet();
							_armorSets.add(set);
							for (Node a = d.getFirstChild(); a != null; a = a.getNextSibling())
							{
								attrs = a.getAttributes();
								try
								{
									if (attrs != null)
										att = attrs.getNamedItem("id");
									String name = a.getNodeName();
									if ("chest".equalsIgnoreCase(name))
									{
										set.addChest(Integer.parseInt(att.getNodeValue()));
									}
									else if ("feet".equalsIgnoreCase(name))
									{
										set.addFeet(Integer.parseInt(att.getNodeValue()));
									}
									else if ("gloves".equalsIgnoreCase(name))
									{
										set.addGloves(Integer.parseInt(att.getNodeValue()));
									}
									else if ("head".equalsIgnoreCase(name))
									{
										set.addHead(Integer.parseInt(att.getNodeValue()));
									}
									else if ("legs".equalsIgnoreCase(name))
									{
										set.addLegs(Integer.parseInt(att.getNodeValue()));
									}
									else if ("shield".equalsIgnoreCase(name))
									{
										set.addShield(Integer.parseInt(att.getNodeValue()));
									}
									else if ("skill".equalsIgnoreCase(name))
									{
										int skillId = Integer.parseInt(att.getNodeValue());
										int skillLevel = Integer.parseInt(attrs.getNamedItem("level").getNodeValue());
										set.addSkill(new SkillHolder(skillId, skillLevel));										
									}
									else if ("shield_skill".equalsIgnoreCase(name))
									{
										int skillId = Integer.parseInt(att.getNodeValue());
										int skillLevel = Integer.parseInt(attrs.getNamedItem("level").getNodeValue());
										set.addShieldSkill(new SkillHolder(skillId, skillLevel));
									}
									else if ("enchant6skill".equalsIgnoreCase(name))
									{
										int skillId = Integer.parseInt(att.getNodeValue());
										int skillLevel = Integer.parseInt(attrs.getNamedItem("level").getNodeValue());
										set.addEnchant6Skill(new SkillHolder(skillId, skillLevel));
									}
									else if ("con".equalsIgnoreCase(name))
									{
										// TODO: Implement me
									}
									else if ("dex".equalsIgnoreCase(name))
									{
										// TODO: Implement me
									}
									else if ("str".equalsIgnoreCase(name))
									{
										// TODO: Implement me
									}
									else if ("men".equalsIgnoreCase(name))
									{
										// TODO: Implement me
									}
									else if ("wit".equalsIgnoreCase(name))
									{
										// TODO: Implement me
									}
									else if ("int".equalsIgnoreCase(name))
									{
										// TODO: Implement me
									}
								}
								catch (Exception e)
								{
									_log.log(Level.WARNING, "Error while parsing set id: " + d.getAttributes().getNamedItem("id").getNodeValue() + " " + e.getMessage(), e);
								}
							}
						}
					}
				}
			}
		}
	}
	
	private void loadData()
	{
		File dir = new File(Config.DATAPACK_ROOT, "data/stats/armorsets");
		if (dir.isDirectory())
		{
			for (File f : dir.listFiles(new XMLFilter()))
			{
				new Parser(f);
			}
		}
		else
		{
			_log.log(Level.WARNING, getClass().getSimpleName() + ": Couldn't find folder: " + dir.getAbsolutePath());
		}
		_log.log(Level.INFO, getClass().getSimpleName() + ": Loaded " + _armorSets.size() + " Armor sets.");
	}
	
	public boolean setExists(int chestId)
	{
		return getSet(chestId) != null;
	}
	
	public L2ArmorSet getSet(int chestId)
	{
		for (L2ArmorSet set : _armorSets)
		{
			if (set.containsChest(chestId))
				return set;
		}
		
		return null;
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final ArmorSetsTable _instance = new ArmorSetsTable();
	}
}
