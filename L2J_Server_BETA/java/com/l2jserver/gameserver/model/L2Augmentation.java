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

import javolution.util.FastList;

import com.l2jserver.gameserver.datatables.AugmentationData;
import com.l2jserver.gameserver.datatables.SkillTable;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.skills.L2Skill;
import com.l2jserver.gameserver.model.skills.funcs.FuncAdd;
import com.l2jserver.gameserver.model.skills.funcs.LambdaConst;
import com.l2jserver.gameserver.model.stats.Stats;
import com.l2jserver.gameserver.network.serverpackets.SkillCoolTime;

/**
 * Used to store an augmentation and its boni
 * @author durgus
 */
public final class L2Augmentation
{
	private int _effectsId = 0;
	private AugmentationStatBoni _boni = null;
	private L2Skill _skill = null;
	
	public L2Augmentation(int effects, L2Skill skill)
	{
		_effectsId = effects;
		_boni = new AugmentationStatBoni(_effectsId);
		_skill = skill;
	}
	
	public L2Augmentation(int effects, int skill, int skillLevel)
	{
		this(effects, skill != 0 ? SkillTable.getInstance().getInfo(skill, skillLevel) : null);
	}
	
	public static class AugmentationStatBoni
	{
		private final Stats _stats[];
		private final float _values[];
		private boolean _active;
		
		public AugmentationStatBoni(int augmentationId)
		{
			_active = false;
			FastList<AugmentationData.AugStat> as = AugmentationData.getInstance().getAugStatsById(augmentationId);
			
			_stats = new Stats[as.size()];
			_values = new float[as.size()];
			
			int i = 0;
			for (AugmentationData.AugStat aStat : as)
			{
				_stats[i] = aStat.getStat();
				_values[i] = aStat.getValue();
				i++;
			}
		}
		
		public void applyBonus(L2PcInstance player)
		{
			// make sure the bonuses are not applied twice..
			if (_active)
			{
				return;
			}
			
			for (int i = 0; i < _stats.length; i++)
			{
				player.addStatFunc(new FuncAdd(_stats[i], 0x40, this, new LambdaConst(_values[i])));
			}
			
			_active = true;
		}
		
		public void removeBonus(L2PcInstance player)
		{
			// make sure the bonuses are not removed twice
			if (!_active)
			{
				return;
			}
			player.removeStatsOwner(this);
			
			_active = false;
		}
	}
	
	public int getAttributes()
	{
		return _effectsId;
	}
	
	/**
	 * Get the augmentation "id" used in serverpackets.
	 * @return augmentationId
	 */
	public int getAugmentationId()
	{
		return _effectsId;
	}
	
	public L2Skill getSkill()
	{
		return _skill;
	}
	
	/**
	 * Applies the bonuses to the player.
	 * @param player
	 */
	public void applyBonus(L2PcInstance player)
	{
		boolean updateTimeStamp = false;
		_boni.applyBonus(player);
		
		// add the skill if any
		if (_skill != null)
		{
			player.addSkill(_skill);
			if (_skill.isActive())
			{
				final long delay = player.getSkillRemainingReuseTime(_skill.getReuseHashCode());
				if (delay > 0)
				{
					player.disableSkill(_skill, delay);
					updateTimeStamp = true;
				}
			}
			player.sendSkillList();
			if (updateTimeStamp)
			{
				player.sendPacket(new SkillCoolTime(player));
			}
		}
	}
	
	/**
	 * Removes the augmentation bonuses from the player.
	 * @param player
	 */
	public void removeBonus(L2PcInstance player)
	{
		_boni.removeBonus(player);
		
		// remove the skill if any
		if (_skill != null)
		{
			if (_skill.isPassive())
			{
				player.removeSkill(_skill, false, true);
			}
			else
			{
				player.removeSkill(_skill, false, false);
			}
			
			player.sendSkillList();
		}
	}
}
