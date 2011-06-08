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
package com.l2jserver.gameserver.model;

import java.util.logging.Logger;

import com.l2jserver.gameserver.templates.StatsSet;

/**
 * @author Zoey76
 */
public final class L2SkillLearn
{
	private static final Logger _log = Logger.getLogger(L2SkillLearn.class.getName());
	
	private final String _skillName;
	private final int _skillId;
	private final int _skillLvl;
	private final int _getLevel;
	private final boolean _autoGet;
	private final int _levelUpSp;
	private final int[][] _itemsIdCount;
	private final int[] _races;
	private final int[] _preReqSkillIdLvl;
	private final int _socialClass;
	private final boolean _residenceSkill;
	private final int[] _residenceIds;
	private final int[][] _subClassLvlNumber;
	private final boolean _learnedByNpc;
	private final boolean _learnedByFS;
	
	/**
	 * Constructor for L2SkillLearn. 
	 * @param set the set with the L2SkillLearn data.
	 */
	public L2SkillLearn(StatsSet set)
	{
		_skillName = set.getString("skillName");
		_skillId = set.getInteger("skillId");
		_skillLvl = set.getInteger("skillLvl");
		_getLevel = set.getInteger("getLevel");
		_autoGet = set.getBool("autoGet", false);
		_levelUpSp = set.getInteger("levelUpSp", 0);
		if (!set.getString("itemsIdCount", "").isEmpty())
		{
			final String[] items = set.getString("itemsIdCount").split(";");
			_itemsIdCount = new int[items.length][2];
			int i = 0;
			for (String itemIdCount : items)
			{
				_itemsIdCount[i][0] = Integer.parseInt(itemIdCount.split(",")[0]);//Id
				_itemsIdCount[i][1] = Integer.parseInt(itemIdCount.split(",")[1]);//Count
				i++;
			}
		}
		else
		{
			_itemsIdCount = null;
		}
		if (!set.getString("race", "").isEmpty())
		{
			_races = set.getIntegerArray("race");
		}
		else
		{
			_races = null;
		}
		if (!set.getString("preReqSkillIdLvl", "").isEmpty())
		{
			_preReqSkillIdLvl = new int[2];
			try
			{
				_preReqSkillIdLvl[0] = Integer.parseInt(set.getString("preReqSkillIdLvl").split(",")[0]);
				_preReqSkillIdLvl[1] = Integer.parseInt(set.getString("preReqSkillIdLvl").split(",")[1]);
			}
			catch (Exception e)
			{
				_log.severe(getClass().getSimpleName() + ": Malformed preReqSkillIdLvl for Learn Skill Id " + _skillId + " and level " + _skillLvl + "!");
			}
		}
		else
		{
			_preReqSkillIdLvl = null;
		}
		_socialClass = set.getInteger("socialClass", 0);
		_residenceSkill = set.getBool("residenceSkill", false);
		if (!set.getString("residenceIds", "").isEmpty())
		{
			_residenceIds = set.getIntegerArray("residenceIds");
		}
		else
		{
			_residenceIds = null;
		}
		if (!set.getString("subClassLvlNumber", "").isEmpty())
		{
			final String[] subLvLNumList = set.getString("subClassLvlNumber").split(";");
			_subClassLvlNumber = new int[subLvLNumList.length][2];
			int i = 0;
			for (String subLvlNum : subLvLNumList)
			{
				_subClassLvlNumber[i][0] = Integer.parseInt(subLvlNum.split(",")[0]);
				_subClassLvlNumber[i][1] = Integer.parseInt(subLvlNum.split(",")[1]);
				i++;
			}
		}
		else
		{
			_subClassLvlNumber = null;
		}
		_learnedByNpc = set.getBool("learnedByNpc", false);
		_learnedByFS = set.getBool("learnedByFS", false);
	}
	
	/**
	 * @return the name of this skill.
	 */
	public String getName()
	{
		return _skillName;
	}
	
	/**
	 * @return the ID of this skill.
	 */
	public int getSkillId()
	{
		return _skillId;
	}
	
	/**
	 * @return the level of this skill.
	 */
	public int getSkillLevel()
	{
		return _skillLvl;
	}
	
	/**
	 * @return the minimum level required to acquire this skill.
	 */
	public int getGetLevel()
	{
		return _getLevel;
	}
	
	/**
	 * @return the amount of SP/Clan Reputation to acquire this skill.
	 */
	public int getLevelUpSp()
	{
		return _levelUpSp;
	}
	
	/**
	 * @return {@code true} if the skill is auto-get, this skill is automatically delivered.
	 */
	public boolean isAutoGet()
	{
		return _autoGet;
	}
	
	/**
	 * @return the multidimensional array with the item IDs and amounts required to acquire this skill.
	 */
	public int[][] getItemsIdCount()
	{
		return _itemsIdCount;
	}
	
	/**
	 * @return the array with the races that can acquire this skill.
	 */
	public int[] getRaces()
	{
		return _races;
	}
	
	/**
	 * @return the array with required skill IDs and levels to acquire this skill.
	 */
	public int[] getPreReqSkillIdLvl()
	{
		return _preReqSkillIdLvl;
	}
	
	/**
	 * @return the social class required to get this skill.
	 */
	public int getSocialClass()
	{
		return _socialClass;
	}
	
	/**
	 * @return {@code true} if this skill is a Residence skill.
	 */
	public boolean isResidencialSkill()
	{
		return _residenceSkill;
	}
	
	/**
	 * @return the array with the IDs where this skill is available.
	 */
	public int[] getRecidenceIds()
	{
		return _residenceIds;
	}
	
	/**
	 * @return the array with Sub-Class conditions, amount of subclasses and level. 
	 */
	public int[][] getSubClassConditions()
	{
		return _subClassLvlNumber;
	}
	
	/**
	 * @return {@code true} if this skill is learned from Npc.
	 */
	public boolean isLearnedByNpc()
	{
		return _learnedByNpc;
	}
	
	/**
	 * @return {@code true} if this skill is learned by Forgotten Scroll.
	 */
	public boolean isLearnedByFS()
	{
		return _learnedByFS;
	}
}
