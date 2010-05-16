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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2jserver.L2DatabaseFactory;
import com.l2jserver.gameserver.model.L2EnchantSkillGroup;
import com.l2jserver.gameserver.model.L2EnchantSkillLearn;
import com.l2jserver.gameserver.model.L2Skill;
import com.l2jserver.gameserver.model.L2EnchantSkillGroup.EnchantSkillDetail;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;

/**
 * This class ...
 *
 * @version $Revision: 1.13.2.2.2.8 $ $Date: 2005/04/06 16:13:25 $
 */
public class EnchantGroupsTable
{
	public static final int NORMAL_ENCHANT_COST_MULTIPLIER = 1;
	public static final int SAFE_ENCHANT_COST_MULTIPLIER = 5;
	
	public static final int NORMAL_ENCHANT_BOOK = 6622;
	public static final int SAFE_ENCHANT_BOOK = 9627;
	public static final int CHANGE_ENCHANT_BOOK = 9626;
	public static final int UNTRAIN_ENCHANT_BOOK = 9625;
	
	private static Logger _log = Logger.getLogger(EnchantGroupsTable.class.getName());
	
	private TIntObjectHashMap<L2EnchantSkillGroup> _enchantSkillGroups; //enchant skill group
	private TIntObjectHashMap<L2EnchantSkillLearn> _enchantSkillTrees; //enchant skill list
	
	public static EnchantGroupsTable getInstance()
	{
		return SingletonHolder._instance;
	}
	
	
	private EnchantGroupsTable()
	{
		load();
	}
	
	private void load()
	{
		int count = 0;
		Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			
			try
			{
				_enchantSkillGroups = new TIntObjectHashMap<L2EnchantSkillGroup>();
				_enchantSkillTrees = new TIntObjectHashMap<L2EnchantSkillLearn>();
				
				PreparedStatement statement = con.prepareStatement("SELECT group_id, level, adena, exp, sp, success_rate76, success_rate77, success_rate78, success_rate79, success_rate80, success_rate81, success_rate82, success_rate83, success_rate84, success_rate85 FROM enchant_skill_groups ORDER BY group_id, level");
				ResultSet enchantGroups = statement.executeQuery();
				
				int prevGroupId = -1;
				
				while (enchantGroups.next())
				{
					int id = enchantGroups.getInt("group_id");
					int lvl = enchantGroups.getInt("level");
					int adena = enchantGroups.getInt("adena");
					int exp = enchantGroups.getInt("exp");
					int sp = enchantGroups.getInt("sp");
					byte rate76 = enchantGroups.getByte("success_rate76");
					byte rate77 = enchantGroups.getByte("success_rate77");
					byte rate78 = enchantGroups.getByte("success_rate78");
					byte rate79 = enchantGroups.getByte("success_rate79");
					byte rate80 = enchantGroups.getByte("success_rate80");
					byte rate81 = enchantGroups.getByte("success_rate81");
					byte rate82 = enchantGroups.getByte("success_rate82");
					byte rate83 = enchantGroups.getByte("success_rate83");
					byte rate84 = enchantGroups.getByte("success_rate84");
					byte rate85 = enchantGroups.getByte("success_rate85");
					
					if (prevGroupId != id)
						prevGroupId = id;
					
					L2EnchantSkillGroup group = _enchantSkillGroups.get(id);
					if (group == null)
					{
						group = new L2EnchantSkillGroup(id);
						_enchantSkillGroups.put(id, group);
						count++;
					}
					EnchantSkillDetail esd = new EnchantSkillDetail(lvl, adena, exp, sp, rate76, rate77, rate78, rate79, rate80, rate81, rate82, rate83, rate84, rate85);
					group.addEnchantDetail(esd);
				}
				
				enchantGroups.close();
				statement.close();
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "Error while loading enchant skill groups ", e);
			}
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Error while loading enchant skill groups ", e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
			}
		}
		
		_log.info("EnchantGroupsTable: Loaded " + count + " groups.");
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
			
			EnchantSkillDetail esd = enchantSkillLearn.getEnchantSkillDetail(skill.getLevel());
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
			EnchantSkillDetail esd = enchantSkillLearn.getEnchantSkillDetail(skill.getLevel());
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
			EnchantSkillDetail esd = enchantSkillLearn.getEnchantSkillDetail(skill.getLevel());
			if (esd != null)
			{
				return esd.getRate(player);
			}
		}
		
		return 0;
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final EnchantGroupsTable _instance = new EnchantGroupsTable();
	}

	/**
	 * @return L2EnchantSkillGroup
	 */
	public L2EnchantSkillGroup getEnchantSkillGroupById(int id)
	{
		return _enchantSkillGroups.get(id);
	}
	
	public void reload()
	{
		load();
	}
}