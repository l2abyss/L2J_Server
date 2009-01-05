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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastMap;
import net.sf.l2j.L2DatabaseFactory;

public class NpcBufferTable
{
	protected static Logger _log = Logger.getLogger(NpcBufferTable.class.getName());
	
	private class NpcBufferSkills
	{
		private int _npcId = 0;
		private Map<Integer, Integer> _skillId = new FastMap<Integer, Integer>();
		private Map<Integer, Integer> _skillLevels = new FastMap<Integer, Integer>();
		private Map<Integer, Integer> _skillFeeIds = new FastMap<Integer, Integer>();
		private Map<Integer, Integer> _skillFeeAmounts = new FastMap<Integer, Integer>();
		
		public NpcBufferSkills(int npcId)
		{
			_npcId = npcId;
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
			Integer skillId = _skillId.get(buffGroup);
			Integer skillLevel = _skillLevels.get(buffGroup);
			Integer skillFeeId = _skillFeeIds.get(buffGroup);
			Integer skillFeeAmount = _skillFeeAmounts.get(buffGroup);
			
			if (skillId == null || skillLevel == null || skillFeeId == null || skillFeeAmount == null)
				return null;
			
			return new int[]
			{
				skillId, skillLevel, skillFeeId, skillFeeAmount
			};
		}
		
		public int getNpcId()
		{
			return _npcId;
		}
	}
	
	private static NpcBufferTable _instance = null;
	private Map<Integer, NpcBufferSkills> _buffers = new FastMap<Integer, NpcBufferSkills>();
	
	private NpcBufferTable()
	{
		Connection con = null;
		int skillCount = 0;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			
			PreparedStatement statement = con.prepareStatement("SELECT `npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group` FROM `npc_buffer` ORDER BY `npc_id` ASC");
			ResultSet rset = statement.executeQuery();
			
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
						_buffers.put(lastNpcId, skills);
					
					skills = new NpcBufferSkills(npcId);
					skills.addSkill(skillId, skillLevel, skillFeeId, skillFeeAmount, buffGroup);
				}
				else
					skills.addSkill(skillId, skillLevel, skillFeeId, skillFeeAmount, buffGroup);
				
				lastNpcId = npcId;
				skillCount++;
			}
			
			_buffers.put(lastNpcId, skills);
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "NpcBufferSkillIdsTable: Error reading npc_buffer table: " + e.getMessage(), e);
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
		
		_log.info("NpcBufferSkillIdsTable: Loaded " + _buffers.size() + " buffers and " + skillCount + " skills.");
	}
	
	public static NpcBufferTable getInstance()
	{
		if (_instance == null)
			_instance = new NpcBufferTable();
		
		return _instance;
	}
	
	public int[] getSkillInfo(int npcId, int buffGroup)
	{
		NpcBufferSkills skills = _buffers.get(npcId);
		
		if (skills == null)
			return null;
		
		return skills.getSkillGroupInfo(buffGroup);
	}
}