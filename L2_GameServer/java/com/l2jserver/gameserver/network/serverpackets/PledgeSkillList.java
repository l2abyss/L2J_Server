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

import com.l2jserver.gameserver.model.L2Clan;
import com.l2jserver.gameserver.model.L2Skill;

/**
 * Format: (ch) dd[dd][ddd]
 *
 * @author  -Wooden-
 */
public class PledgeSkillList extends L2GameServerPacket
{
	private static final String _S__FE_39_PLEDGESKILLLIST = "[S] FE:3a PledgeSkillList";
	private L2Skill[] _skills;
	private SubPledgeSkill[] _subSkills;
	
	public static class SubPledgeSkill
	{
		public SubPledgeSkill(int subType, int skillId, int skillLvl)
		{
			super();
			this.subType = subType;
			this.skillId = skillId;
			this.skillLvl = skillLvl;
		}
		int subType;
		int skillId;
		int skillLvl;
	}
	
	public PledgeSkillList(L2Clan clan)
	{
		_skills = clan.getAllSkills();
		_subSkills = clan.getAllSubSkills();
	}
	
	/**
	 * @see com.l2jserver.util.network.BaseSendablePacket.ServerBasePacket#writeImpl()
	 */
	@Override
	protected void writeImpl()
	{
		writeC(0xfe);
		writeH(0x3a);
		writeD(_skills.length);
		writeD(_subSkills.length); // squad skill lenght
		for(L2Skill sk : _skills)
		{
			writeD(sk.getId());
			writeD(sk.getLevel());
		}
		for(SubPledgeSkill sk : _subSkills)
		{
			writeD(sk.subType); // clan Sub-unit types
			writeD(sk.skillId);
			writeD(sk.skillLvl);
		}
	}
	
	/**
	 * @see com.l2jserver.gameserver.BasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__FE_39_PLEDGESKILLLIST;
	}
}