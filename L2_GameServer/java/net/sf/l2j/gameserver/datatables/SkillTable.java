/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.datatables;

import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntObjectHashMap;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.skills.SkillsEngine;

/**
 * 
 */
public class SkillTable
{
	private final TIntObjectHashMap<L2Skill> _skills;
	private final TIntIntHashMap _skillMaxLevel;
	
	public static SkillTable getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private SkillTable()
	{
		_skills = new TIntObjectHashMap<L2Skill>();
		_skillMaxLevel = new TIntIntHashMap();
		reload();
	}
	
	public void reload()
	{
		_skills.clear();
		SkillsEngine.getInstance().loadAllSkills(_skills);
		
		_skillMaxLevel.clear();
		for (final L2Skill skill : _skills.getValues(new L2Skill[_skills.size()]))
		{
			final int skillId = skill.getId();
			final int skillLvl = skill.getLevel();
			final int maxLvl = _skillMaxLevel.get(skillId);
			
			if (skillLvl > maxLvl)
				_skillMaxLevel.put(skillId, skillLvl);
		}
	}
	
	/**
	 * Provides the skill hash
	 * 
	 * @param skill
	 *            The L2Skill to be hashed
	 * @return getSkillHashCode(skill.getId(), skill.getLevel())
	 */
	public static int getSkillHashCode(L2Skill skill)
	{
		return getSkillHashCode(skill.getId(), skill.getLevel());
	}
	
	/**
	 * Centralized method for easier change of the hashing sys
	 * 
	 * @param skillId
	 *            The Skill Id
	 * @param skillLevel
	 *            The Skill Level
	 * @return The Skill hash number
	 */
	public static int getSkillHashCode(int skillId, int skillLevel)
	{
		return skillId * 1021 + skillLevel;
	}
	
	public final L2Skill getInfo(final int skillId, final int level)
	{
		final L2Skill result = _skills.get(getSkillHashCode(skillId, level));
		if (result != null)
			return result;

		// skill/level not found, fix for transformation scripts
		final int maxLvl = _skillMaxLevel.get(skillId);
		// requested level too high
		if (maxLvl > 0 && level > maxLvl)
			return _skills.get(getSkillHashCode(skillId, maxLvl));

		return null;
	}
	
	public final int getMaxLevel(final int skillId)
	{
		return _skillMaxLevel.get(skillId);
	}
	
	/**
	 * Returns an array with siege skills. If addNoble == true, will add also Advanced headquarters.
	 */
	public L2Skill[] getSiegeSkills(boolean addNoble)
	{
		L2Skill[] temp = null;
		
		if (addNoble)
		{
			temp = new L2Skill[3];
			temp[2] = _skills.get(SkillTable.getSkillHashCode(326, 1));
		}
		else
			temp = new L2Skill[2];
		
		temp[0] = _skills.get(SkillTable.getSkillHashCode(246, 1));
		temp[1] = _skills.get(SkillTable.getSkillHashCode(247, 1));
		
		return temp;
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final SkillTable _instance = new SkillTable();
	}
}
