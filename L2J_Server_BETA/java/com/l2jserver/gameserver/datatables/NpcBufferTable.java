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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2jserver.Config;
import com.l2jserver.L2DatabaseFactory;

import gnu.trove.map.hash.TIntIntHashMap;

public class NpcBufferTable
{
	private static Logger _log = Logger.getLogger(NpcBufferTable.class.getName());
	
	private final Map<Integer, NpcBufferSkills> _buffers = new HashMap<>();
	
	private static class NpcBufferSkills
	{
		private final TIntIntHashMap _skillId = new TIntIntHashMap();
		private final TIntIntHashMap _skillLevels = new TIntIntHashMap();
		private final TIntIntHashMap _skillFeeIds = new TIntIntHashMap();
		private final TIntIntHashMap _skillFeeAmounts = new TIntIntHashMap();
		
		public NpcBufferSkills(int npcId)
		{
			//
		}
		
		public void addSkill(int skillId, int skillLevel, int skillFeeId, int skillFeeAmount, int buffGroup)
		{
			_skillId.put(buffGroup, skillId);
			_skillLevels.put(buffGroup, skillLevel);
			_skillFeeIds.put(buffGroup, skillFeeId);
			_skillFeeAmounts.put(buffGroup, skillFeeAmount);
		}
		
		public int[] getSkillGroupInfo(int buffGroup)
		{
			if (_skillId.containsKey(buffGroup) && _skillLevels.containsKey(buffGroup) && _skillFeeIds.containsKey(buffGroup) && _skillFeeAmounts.containsKey(buffGroup))
			{
				return new int[]
				{
					_skillId.get(buffGroup),
					_skillLevels.get(buffGroup),
					_skillFeeIds.get(buffGroup),
					_skillFeeAmounts.get(buffGroup)
				};
			}
			return null;
		}
	}
	
	protected NpcBufferTable()
	{
		int skillCount = 0;
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			Statement s = con.createStatement();
			ResultSet rset = s.executeQuery("SELECT `npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group` FROM `npc_buffer` ORDER BY `npc_id` ASC"))
		{
			int lastNpcId = 0;
			NpcBufferSkills skills = null;
			
			while (rset.next())
			{
				int npcId = rset.getInt("npc_id");
				int skillId = rset.getInt("skill_id");
				int skillLevel = rset.getInt("skill_level");
				int skillFeeId = rset.getInt("skill_fee_id");
				int skillFeeAmount = rset.getInt("skill_fee_amount");
				int buffGroup = rset.getInt("buff_group");
				
				if (npcId != lastNpcId)
				{
					if (lastNpcId != 0)
					{
						_buffers.put(lastNpcId, skills);
					}
					
					skills = new NpcBufferSkills(npcId);
					skills.addSkill(skillId, skillLevel, skillFeeId, skillFeeAmount, buffGroup);
				}
				else if (skills != null)
				{
					skills.addSkill(skillId, skillLevel, skillFeeId, skillFeeAmount, buffGroup);
				}
				
				lastNpcId = npcId;
				skillCount++;
			}
			
			if (lastNpcId != 0)
			{
				_buffers.put(lastNpcId, skills);
			}
		}
		catch (SQLException e)
		{
			_log.log(Level.SEVERE, getClass().getSimpleName() + ": Error reading npc_buffer table: " + e.getMessage(), e);
		}
		
		if (Config.CUSTOM_NPCBUFFER_TABLES)
		{
			try (Connection con = L2DatabaseFactory.getInstance().getConnection();
				Statement s = con.createStatement();
				ResultSet rset = s.executeQuery("SELECT `npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group` FROM `custom_npc_buffer` ORDER BY `npc_id` ASC"))
			{
				int lastNpcId = 0;
				NpcBufferSkills skills = null;
				while (rset.next())
				{
					int npcId = rset.getInt("npc_id");
					int skillId = rset.getInt("skill_id");
					int skillLevel = rset.getInt("skill_level");
					int skillFeeId = rset.getInt("skill_fee_id");
					int skillFeeAmount = rset.getInt("skill_fee_amount");
					int buffGroup = rset.getInt("buff_group");
					
					if (npcId != lastNpcId)
					{
						if (lastNpcId != 0)
						{
							_buffers.put(lastNpcId, skills);
						}
						
						skills = new NpcBufferSkills(npcId);
						skills.addSkill(skillId, skillLevel, skillFeeId, skillFeeAmount, buffGroup);
					}
					else if (skills != null)
					{
						skills.addSkill(skillId, skillLevel, skillFeeId, skillFeeAmount, buffGroup);
					}
					lastNpcId = npcId;
					skillCount++;
				}
				
				if (lastNpcId != 0)
				{
					_buffers.put(lastNpcId, skills);
				}
			}
			catch (SQLException e)
			{
				_log.log(Level.SEVERE, getClass().getSimpleName() + ": Error reading custom_npc_buffer table: " + e.getMessage(), e);
			}
		}
		_log.info(getClass().getSimpleName() + ": Loaded " + _buffers.size() + " buffers and " + skillCount + " skills.");
	}
	
	public int[] getSkillInfo(int npcId, int buffGroup)
	{
		final NpcBufferSkills skills = _buffers.get(npcId);
		return (skills == null) ? null : skills.getSkillGroupInfo(buffGroup);
	}
	
	public static NpcBufferTable getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final NpcBufferTable _instance = new NpcBufferTable();
	}
}