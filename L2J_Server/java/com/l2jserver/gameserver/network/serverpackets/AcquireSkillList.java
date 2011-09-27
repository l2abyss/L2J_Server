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
package com.l2jserver.gameserver.network.serverpackets;

import javolution.util.FastList;

/**
 * TODO: Gather samples from newer chronicles.
 * @version 1.4
 */
public final class AcquireSkillList extends L2GameServerPacket
{
	private static final String _S__90_AQUIRESKILLLIST = "[S] 90 AquireSkillList";
	
	/**
	 * Enumerate containing learning skill types.
	 */
	public enum SkillType
	{
		ClassTransform, //0
		Fishing, //1
		Pledge, //2
		SubPledge, //3
		Transfer, //4
		SubClass, //5
		Collect, //6
	}
	
	private FastList<Skill> _skills;
	private SkillType _skillType;
	
	/**
	 * Private class containing learning skill information.
	 */
	private static class Skill
	{
		public int id;
		public int nextLevel;
		public int maxLevel;
		public int spCost;
		public int requirements;
		
		public Skill(int pId, int pNextLevel, int pMaxLevel, int pSpCost, int pRequirements)
		{
			id = pId;
			nextLevel = pNextLevel;
			maxLevel = pMaxLevel;
			spCost = pSpCost;
			requirements = pRequirements;
		}
	}
	
	public AcquireSkillList(SkillType type)
	{
		_skillType = type;
	}
	
	public void addSkill(int id, int nextLevel, int maxLevel, int spCost, int requirements)
	{
		if (_skills == null)
		{
			_skills = new FastList<Skill>();
		}
		_skills.add(new Skill(id, nextLevel, maxLevel, spCost, requirements));
	}
	
	@Override
	protected void writeImpl()
	{
		if (_skills == null)
			return;
		
		writeC(0x90);
		writeD(_skillType.ordinal());
		writeD(_skills.size());
		
		for (Skill temp : _skills)
		{
			writeD(temp.id);
			writeD(temp.nextLevel);
			writeD(temp.maxLevel);
			writeD(temp.spCost);
			writeD(temp.requirements);
			if (_skillType == SkillType.SubPledge)
			{
				writeD(0); //TODO: ?
			}
		}
	}
	
	@Override
	public String getType()
	{
		return _S__90_AQUIRESKILLLIST;
	}
}
