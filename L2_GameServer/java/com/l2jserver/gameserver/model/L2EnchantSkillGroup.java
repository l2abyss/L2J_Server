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

import java.util.List;

import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;

import javolution.util.FastList;

public final class L2EnchantSkillGroup
{
	private final int _id;
	private List<EnchantSkillDetail> _enchantDetails = new FastList<EnchantSkillDetail>();

	public L2EnchantSkillGroup(int id)
	{
		_id = id;
	}

	public void addEnchantDetail(EnchantSkillDetail detail)
	{
		_enchantDetails.add(detail);
	}
	
	public int getId()
	{
		return _id;
	}
	
	public List<EnchantSkillDetail> getEnchantGroupDetails()
	{
		return _enchantDetails;
	}
	
	public static class EnchantSkillDetail
	{
		private final int _level;
		private final int _adenaCost;
		private final int _expCost;
		private final int _spCost;
		private final byte[] _rate;
		
		public EnchantSkillDetail(int lvl, int adena, int exp, int sp, byte rate76, byte rate77, byte rate78, byte rate79, byte rate80, byte rate81, byte rate82, byte rate83, byte rate84, byte rate85)
		{
			_level = lvl;
			_adenaCost = adena;
			_expCost = exp;
			_spCost = sp;
			_rate = new byte[10];
			_rate[0] = rate76;
			_rate[1] = rate77;
			_rate[2] = rate78;
			_rate[3] = rate79;
			_rate[4] = rate80;
			_rate[5] = rate81;
			_rate[6] = rate82;
			_rate[7] = rate83;
			_rate[8] = rate84;
			_rate[9] = rate85;
		}
		
		/**
		 * @return Returns the level.
		 */
		public int getLevel()
		{
			return _level;
		}
		
		/**
		 * @return Returns the spCost.
		 */
		public int getSpCost()
		{
			return _spCost;
		}
		public int getExpCost()
		{
			return _expCost;
		}
		public int getAdenaCost()
		{
			return _adenaCost;
		}
		
		public byte getRate(L2PcInstance ply)
		{
			if (ply.getLevel() < 76)
				return 0;
			return _rate[ply.getLevel() - 76];
		}
	}
}