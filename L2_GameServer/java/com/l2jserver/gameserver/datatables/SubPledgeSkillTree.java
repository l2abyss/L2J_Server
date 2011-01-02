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

import gnu.trove.TIntObjectHashMap;

import java.io.File;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;

import javolution.util.FastList;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.l2jserver.Config;
import com.l2jserver.gameserver.model.L2Clan;
import com.l2jserver.gameserver.model.L2Skill;

/**
 * @author JIV
 *
 */
public class SubPledgeSkillTree
{
	private static final Logger _log = Logger.getLogger(SubPledgeSkillTree.class.getName());
	
	private TIntObjectHashMap<SubUnitSkill> skilltree = new TIntObjectHashMap<SubPledgeSkillTree.SubUnitSkill>();
	
	public SubPledgeSkillTree()
	{
		load();
	}
	
	public static SubPledgeSkillTree getInstance()
	{
		return SingletonHolder._instance;
	}
	
	public static class SubUnitSkill
	{
		private L2Skill skill;
		private int clanLvl;
		private int reputation;
		private int itemId;
		private int count;
		
		public SubUnitSkill(L2Skill skill, int clanLvl, int reputation, int itemId, int count)
		{
			super();
			this.skill = skill;
			this.clanLvl = clanLvl;
			this.reputation = reputation;
			this.itemId = itemId;
			this.count = count;
		}
		
		public L2Skill getSkill()
		{
			return skill;
		}
		public int getClanLvl()
		{
			return clanLvl;
		}
		public int getReputation()
		{
			return reputation;
		}
		public int getItemId()
		{
			return itemId;
		}
		public int getCount()
		{
			return count;
		}
	}
	
	public void reload()
	{
		load();
	}
	
	private void load()
	{
		skilltree.clear();
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setIgnoringComments(true);
		File file = new File(Config.DATAPACK_ROOT, "data/skillTrees/subpledgeskilltree.xml");
		Document doc = null;
		if (file.exists())
		{
			try
			{
				doc = factory.newDocumentBuilder().parse(file);
			}
			catch(Exception e)
			{
				_log.log(Level.WARNING, "Could not parse subpledgeskilltree.xml file: " + e.getMessage(), e);
			}
			
			for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
			{
				if ("skill_tree".equalsIgnoreCase(n.getNodeName()))
				{
					for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					{
						if ("skill".equalsIgnoreCase(d.getNodeName()))
						{
							NamedNodeMap attrs = d.getAttributes();
							Node att;
							int skillId;
							int skillLvl;
							int clanLvl;
							int reputation;
							int itemId;
							int count;
							
							att = attrs.getNamedItem("id");
							if (att == null)
							{
								_log.severe("[SubPledgeSkillTree] Missing id, skipping");
								continue;
							}
							skillId = Integer.parseInt(att.getNodeValue());
							
							att = attrs.getNamedItem("level");
							if (att == null)
							{
								_log.severe("[SubPledgeSkillTree] Missing level, skipping");
								continue;
							}
							skillLvl = Integer.parseInt(att.getNodeValue());
							
							att = attrs.getNamedItem("reputation");
							if (att == null)
							{
								_log.severe("[SubPledgeSkillTree] Missing reputation, skipping");
								continue;
							}
							reputation = Integer.parseInt(att.getNodeValue());
							
							att = attrs.getNamedItem("clan_level");
							if (att == null)
							{
								_log.severe("[SubPledgeSkillTree] Missing clan_level, skipping");
								continue;
							}
							clanLvl = Integer.parseInt(att.getNodeValue());
							
							att = attrs.getNamedItem("itemId");
							if (att == null)
							{
								_log.severe("[SubPledgeSkillTree] Missing itemId, skipping");
								continue;
							}
							itemId = Integer.parseInt(att.getNodeValue());
							
							att = attrs.getNamedItem("count");
							if (att == null)
							{
								_log.severe("[SubPledgeSkillTree] Missing count, skipping");
								continue;
							}
							count = Integer.parseInt(att.getNodeValue());
							
							L2Skill skill = SkillTable.getInstance().getInfo(skillId, skillLvl);
							if (skill == null)
							{
								_log.severe("[SubPledgeSkillTree] Skill "+skillId+" not exist, skipping");
								continue;
							}
							
							skilltree.put(SkillTable.getSkillHashCode(skill), new SubUnitSkill(skill, clanLvl, reputation, itemId, count));
						}
					}
				}
			}
		}
		_log.info(getClass().getSimpleName()+": Loaded "+skilltree.size()+" SubUnit Skills");
	}
	
	public SubUnitSkill getSkill(int skillhash)
	{
		return skilltree.get(skillhash);
	}
	
	public SubUnitSkill[] getAvailableSkills(L2Clan clan)
	{
		FastList<SubUnitSkill> list = FastList.newInstance();
		for (Object obj : skilltree.getValues())
		{
			SubUnitSkill skill = (SubUnitSkill) obj;
			if (skill.getClanLvl() <= clan.getLevel())
				list.add(skill);
		}
		
		Iterator<SubUnitSkill> it = list.iterator();
		while (it.hasNext())
		{
			SubUnitSkill sus = it.next();
			if (!clan.isLearnableSubSkill(sus.getSkill()))
				it.remove();
		}
		
		SubUnitSkill[] result = list.toArray(new SubUnitSkill[list.size()]);
		FastList.recycle(list);
		return result;
	}
	
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final SubPledgeSkillTree _instance = new SubPledgeSkillTree();
	}
}
