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

import com.l2jserver.gameserver.model.L2Skill;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.util.Util;

/**
 * @author Gnacik, Zoey76
 */
public class GMSkillTable
{
	private static final L2Skill[] _gmSkills = new L2Skill[46];
	private static final L2Skill[] _gmAuraSkills = new L2Skill[46];
	private static final int[] _gmSkillsId = { 7029, 7041, 7042, 7043, 7044, 7045, 7046, 7047, 7048, 7049, 7050, 7051, 7052, 7053, 7054, 7055, 7056, 7057, 7058, 7059, 7060, 7061, 7062, 7063, 7064, 7088, 7089, 7090, 7091, 7092, 7093, 7094, 7095, 7096, 23238, 23239, 23240, 23241, 23242, 23243, 23244, 23245, 23246, 23247, 23248, 23249 };
	private static final int[] _gmAuraSkillsId = { 7029, 23249, 23253, 23254, 23255, 23256, 23257, 23258, 23259, 23260, 23261, 23262, 23263, 23264, 23265, 23266, 23267, 23268, 23269, 23270, 23271, 23272, 23273, 23274, 23275, 23276, 23277, 23278, 23279, 23280, 23281, 23282, 23283, 23284, 23285, 23286, 23287, 23288, 23289, 23290, 23291, 23292, 23293, 23294, 23295, 23296 };
	
	private GMSkillTable()
	{
		for (int i = 0; i < _gmSkillsId.length; i++)
		{
			_gmSkills[i] = SkillTable.getInstance().getInfo(_gmSkillsId[i], 1);
			_gmAuraSkills[i] = SkillTable.getInstance().getInfo(_gmAuraSkillsId[i], 1);
		}
	}
	
	public static GMSkillTable getInstance()
	{
		return SingletonHolder._instance;
	}
	
	public static L2Skill[] getGMSkills()
	{
		return _gmSkills;
	}
	
	public static L2Skill[] getGMAuraSkills()
	{
		return _gmAuraSkills;
	}
	
	public static boolean isGMSkill(int skillid)
	{
		return Util.contains(_gmSkillsId, skillid) || Util.contains(_gmAuraSkillsId, skillid);
	}
	
	public void addSkills(L2PcInstance gmchar, boolean auraSkills)
	{
		final L2Skill[] skills = auraSkills ? getGMAuraSkills() : getGMSkills();
		for (L2Skill s : skills)
		{
			gmchar.addSkill(s, false); // Don't Save GM skills to database
		}
	}
	
	public void switchSkills(L2PcInstance gmchar, boolean toAuraSkills)
	{
		final L2Skill[] skills = toAuraSkills ? getGMSkills() : getGMAuraSkills();
		for (L2Skill s : skills)
		{
			gmchar.removeSkill(s, false); // Don't Save GM skills to database
		}
		addSkills(gmchar, toAuraSkills);
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final GMSkillTable _instance = new GMSkillTable();
	}
}
