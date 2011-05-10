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

import gnu.trove.TIntObjectHashMap;

import java.util.List;

import com.l2jserver.gameserver.model.L2Skill;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author Nyaran
 */
public class SummonEffectsTable
{
	/** Servitors **/
	/*
	 * Map tree
	 * key: charObjectId, value: classIndex Map
	 * 		key: classIndex, value: servitors Map
	 * 			key: servitorSkillId, value: Effects list
	 */
	private TIntObjectHashMap<TIntObjectHashMap<TIntObjectHashMap<List<SummonEffect>>>> _servitorEffects = new TIntObjectHashMap<TIntObjectHashMap<TIntObjectHashMap<List<SummonEffect>>>>();

	public TIntObjectHashMap<TIntObjectHashMap<TIntObjectHashMap<List<SummonEffect>>>> getServitorEffectsOwner()
	{
		return _servitorEffects;
	}
	
	public TIntObjectHashMap<List<SummonEffect>> getServitorEffects(L2PcInstance owner)
	{
		return _servitorEffects.get(owner.getObjectId()).get(owner.getClassIndex());
	}
	
	
	/** Pets **/
	private TIntObjectHashMap<List<SummonEffect>> _petEffects = new TIntObjectHashMap<List<SummonEffect>>(); // key: petItemObjectId, value: Effects list
	
	public TIntObjectHashMap<List<SummonEffect>> getPetEffects()
	{
		return _petEffects;
	}
	
	
	/** Common **/
	public static SummonEffectsTable getInstance()
	{
		return SingletonHolder._instance;
	}
	
	public class SummonEffect
	{
		L2Skill _skill;
		int _effectCount;
		int _effectCurTime;
		
		public SummonEffect(L2Skill skill, int effectCount, int effectCurTime)
		{
			_skill = skill;
			_effectCount = effectCount;
			_effectCurTime = effectCurTime;
		}
		
		public L2Skill getSkill()
		{
			return _skill;
		}
		
		public int getEffectCount()
		{
			return _effectCount;
		}
		
		public int getEffectCurTime()
		{
			return _effectCurTime;
		}
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final SummonEffectsTable _instance = new SummonEffectsTable();
	}
}
