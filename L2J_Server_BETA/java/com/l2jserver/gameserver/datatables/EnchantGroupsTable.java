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
import java.util.logging.Level;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.l2jserver.Config;
import com.l2jserver.gameserver.engines.DocumentParser;
import com.l2jserver.gameserver.model.L2EnchantSkillGroup;
import com.l2jserver.gameserver.model.L2EnchantSkillGroup.EnchantSkillHolder;
import com.l2jserver.gameserver.model.L2EnchantSkillLearn;
import com.l2jserver.gameserver.model.StatsSet;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.skills.L2Skill;

/**
 * This class ...
 * @version $Revision: 1.13.2.2.2.8 $ $Date: 2005/04/06 16:13:25 $
 */
public class EnchantGroupsTable extends DocumentParser
{
	public static final int NORMAL_ENCHANT_COST_MULTIPLIER = Config.NORMAL_ENCHANT_COST_MULTIPLIER;
	public static final int SAFE_ENCHANT_COST_MULTIPLIER = Config.SAFE_ENCHANT_COST_MULTIPLIER;
	
	public static final int NORMAL_ENCHANT_BOOK = 6622;
	public static final int SAFE_ENCHANT_BOOK = 9627;
	public static final int CHANGE_ENCHANT_BOOK = 9626;
	public static final int UNTRAIN_ENCHANT_BOOK = 9625;
	
	private Map<Integer, L2EnchantSkillGroup> _enchantSkillGroups = new HashMap<>();
	private Map<Integer, L2EnchantSkillLearn> _enchantSkillTrees = new HashMap<>();
	
	protected EnchantGroupsTable()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_enchantSkillGroups.clear();
		_enchantSkillTrees.clear();
		parseDatapackFile("data/enchantSkillGroups.xml");
		int routes = 0;
		for (L2EnchantSkillGroup group : _enchantSkillGroups.values())
		{
			routes += group.getEnchantGroupDetails().size();
		}
		_log.info(getClass().getSimpleName() + ": Loaded " + _enchantSkillGroups.size() + " groups and " + routes + " routes.");
	}
	
	@Override
	protected void parseDocument()
	{
		NamedNodeMap attrs;
		StatsSet set;
		Node att;
		int id = 0;
		L2EnchantSkillGroup group;
		for (Node n = getCurrentDocument().getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("group".equalsIgnoreCase(d.getNodeName()))
					{
						attrs = d.getAttributes();
						id = parseInt(attrs, "id");
						
						group = _enchantSkillGroups.get(id);
						if (group == null)
						{
							group = new L2EnchantSkillGroup(id);
							_enchantSkillGroups.put(id, group);
						}
						
						for (Node b = d.getFirstChild(); b != null; b = b.getNextSibling())
						{
							if ("enchant".equalsIgnoreCase(b.getNodeName()))
							{
								attrs = b.getAttributes();
								set = new StatsSet();
								
								for (int i = 0; i < attrs.getLength(); i++)
								{
									att = attrs.item(i);
									set.set(att.getNodeName(), att.getNodeValue());
								}
								group.addEnchantDetail(new EnchantSkillHolder(set));
							}
						}
					}
				}
			}
		}
	}
	
	public int addNewRouteForSkill(int skillId, int maxLvL, int route, int group)
	{
		L2EnchantSkillLearn enchantableSkill = _enchantSkillTrees.get(skillId);
		if (enchantableSkill == null)
		{
			enchantableSkill = new L2EnchantSkillLearn(skillId, maxLvL);
			_enchantSkillTrees.put(skillId, enchantableSkill);
		}
		if (_enchantSkillGroups.containsKey(group))
		{
			enchantableSkill.addNewEnchantRoute(route, group);
			
			return _enchantSkillGroups.get(group).getEnchantGroupDetails().size();
		}
		_log.log(Level.SEVERE, "Error while loading generating enchant skill id: " + skillId + "; route: " + route + "; missing group: " + group);
		return 0;
	}
	
	public L2EnchantSkillLearn getSkillEnchantmentForSkill(L2Skill skill)
	{
		L2EnchantSkillLearn esl = this.getSkillEnchantmentBySkillId(skill.getId());
		// there is enchantment for this skill and we have the required level of it
		if (esl != null && skill.getLevel() >= esl.getBaseLevel())
		{
			return esl;
		}
		return null;
	}
	
	public L2EnchantSkillLearn getSkillEnchantmentBySkillId(int skillId)
	{
		return _enchantSkillTrees.get(skillId);
	}
	
	public int getEnchantSkillSpCost(L2Skill skill)
	{
		L2EnchantSkillLearn enchantSkillLearn = _enchantSkillTrees.get(skill.getId());
		if (enchantSkillLearn != null)
		{
			
			EnchantSkillHolder esd = enchantSkillLearn.getEnchantSkillHolder(skill.getLevel());
			if (esd != null)
			{
				return esd.getSpCost();
			}
		}
		
		return Integer.MAX_VALUE;
	}
	
	public int getEnchantSkillAdenaCost(L2Skill skill)
	{
		L2EnchantSkillLearn enchantSkillLearn = _enchantSkillTrees.get(skill.getId());
		if (enchantSkillLearn != null)
		{
			EnchantSkillHolder esd = enchantSkillLearn.getEnchantSkillHolder(skill.getLevel());
			if (esd != null)
			{
				return esd.getAdenaCost();
			}
		}
		
		return Integer.MAX_VALUE;
	}
	
	public byte getEnchantSkillRate(L2PcInstance player, L2Skill skill)
	{
		L2EnchantSkillLearn enchantSkillLearn = _enchantSkillTrees.get(skill.getId());
		if (enchantSkillLearn != null)
		{
			EnchantSkillHolder esd = enchantSkillLearn.getEnchantSkillHolder(skill.getLevel());
			if (esd != null)
			{
				return esd.getRate(player);
			}
		}
		
		return 0;
	}
	
	private static class SingletonHolder
	{
		protected static final EnchantGroupsTable _instance = new EnchantGroupsTable();
	}
	
	/**
	 * @param id
	 * @return L2EnchantSkillGroup
	 */
	public L2EnchantSkillGroup getEnchantSkillGroupById(int id)
	{
		return _enchantSkillGroups.get(id);
	}
	
	public static EnchantGroupsTable getInstance()
	{
		return SingletonHolder._instance;
	}
}