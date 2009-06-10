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
package net.sf.l2j.gameserver.datatables;

import java.util.Map;

import javolution.util.FastList;
import javolution.util.FastMap;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.skills.SkillsEngine;

/**
 * This class ...
 *
 * @version $Revision: 1.8.2.6.2.18 $ $Date: 2005/04/06 16:13:25 $
 */
public class SkillTable
{
	//private static Logger _log = Logger.getLogger(SkillTable.class.getName());
	private static Map<Integer, L2Skill> _skills = new FastMap<Integer, L2Skill>();
	private boolean _initialized = true;
	
	public static SkillTable getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private SkillTable()
	{
		SkillsEngine.getInstance().loadAllSkills(_skills);
	}
	
	public void reload()
	{
		final Map<Integer, L2Skill> skills = new FastMap<Integer, L2Skill>();
		SkillsEngine.getInstance().loadAllSkills(skills);
		_skills = skills;
	}
	
	public boolean isInitialized()
	{
		return _initialized;
	}
	
	/**
	 * Provides the skill hash
	 * @param skill The L2Skill to be hashed
	 * @return SkillTable.getSkillHashCode(skill.getId(), skill.getLevel())
	 */
	public static int getSkillHashCode(L2Skill skill)
	{
		return SkillTable.getSkillHashCode(skill.getId(), skill.getLevel());
	}
	
	/**
	 * Centralized method for easier change of the hashing sys
	 * @param skillId The Skill Id
	 * @param skillLevel The Skill Level
	 * @return The Skill hash number
	 */
	public static int getSkillHashCode(int skillId, int skillLevel)
	{
		return skillId * 1021 + skillLevel;
	}
	
	public L2Skill getInfo(int skillId, int level)
	{
		return _skills.get(getSkillHashCode(skillId, level));
	}
	
	public int getMaxLevel(int magicId, int level)
	{
		while (level < 100)
		{
			if (_skills.get(getSkillHashCode(magicId, ++level)) == null)
				return level - 1;
		}
		
		return level;
	}
	
	
	/**
	 * Returns an array with siege skills. If addNoble == true, will add also Advanced headquarters.
	 */
	public L2Skill[] getSiegeSkills(boolean addNoble)
	{
		FastList<L2Skill> list = new FastList<L2Skill>();
		
		list.add(_skills.get(SkillTable.getSkillHashCode(246, 1)));
		list.add(_skills.get(SkillTable.getSkillHashCode(247, 1)));
		
		if (addNoble)
			list.add(_skills.get(SkillTable.getSkillHashCode(326, 1)));
		
		L2Skill[] temp = new L2Skill[list.size()];
		list.toArray(temp);
		
		return temp;
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final SkillTable _instance = new SkillTable();
	}
}
