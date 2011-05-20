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
package com.l2jserver.gameserver.model.actor.instance;

import gnu.trove.TIntObjectHashMap;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastList;

import com.l2jserver.Config;
import com.l2jserver.L2DatabaseFactory;
import com.l2jserver.gameserver.ThreadPoolManager;
import com.l2jserver.gameserver.datatables.CharSummonTable;
import com.l2jserver.gameserver.datatables.SkillTable;
import com.l2jserver.gameserver.datatables.SummonEffectsTable;
import com.l2jserver.gameserver.datatables.SummonEffectsTable.SummonEffect;
import com.l2jserver.gameserver.model.L2Effect;
import com.l2jserver.gameserver.model.L2Object;
import com.l2jserver.gameserver.model.L2Skill;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.L2Summon;
import com.l2jserver.gameserver.network.serverpackets.SetSummonRemainTime;
import com.l2jserver.gameserver.skills.Env;
import com.l2jserver.gameserver.skills.l2skills.L2SkillSummon;
import com.l2jserver.gameserver.templates.chars.L2NpcTemplate;
import com.l2jserver.gameserver.templates.effects.EffectTemplate;


public class L2SummonInstance extends L2Summon
{
	protected static final Logger log = Logger.getLogger(L2SummonInstance.class.getName());
	
	private static final String ADD_SKILL_SAVE = "INSERT INTO character_summon_skills_save (ownerId,ownerClassIndex,summonSkillId,skill_id,skill_level,effect_count,effect_cur_time,buff_index) VALUES (?,?,?,?,?,?,?,?)";
	private static final String RESTORE_SKILL_SAVE = "SELECT skill_id,skill_level,effect_count,effect_cur_time,buff_index FROM character_summon_skills_save WHERE ownerId=? AND ownerClassIndex=? AND summonSkillId=? ORDER BY buff_index ASC";
	private static final String DELETE_SKILL_SAVE = "DELETE FROM character_summon_skills_save WHERE ownerId=? AND ownerClassIndex=? AND summonSkillId=?";

	private float _expPenalty = 0; // exp decrease multiplier (i.e. 0.3 (= 30%) for shadow)
	private int _itemConsumeId;
	private int _itemConsumeCount;
	private int _itemConsumeSteps;
	private final int _totalLifeTime;
	private final int _timeLostIdle;
	private final int _timeLostActive;
	private int _timeRemaining;
	private int _nextItemConsumeTime;
	public int lastShowntimeRemaining; // Following FbiAgent's example to avoid sending useless packets
	
	private Future<?> _summonLifeTask;
	
	private int _referenceSkill;
	
	public L2SummonInstance(int objectId, L2NpcTemplate template, L2PcInstance owner, L2Skill skill)
	{
		super(objectId, template, owner);
		setInstanceType(InstanceType.L2SummonInstance);
		setShowSummonAnimation(true);
		
		if (skill != null)
		{
			final L2SkillSummon summonSkill = (L2SkillSummon)skill;
			_itemConsumeId = summonSkill.getItemConsumeIdOT();
			_itemConsumeCount = summonSkill.getItemConsumeOT();
			_itemConsumeSteps = summonSkill.getItemConsumeSteps();
			_totalLifeTime = summonSkill.getTotalLifeTime();
			_timeLostIdle = summonSkill.getTimeLostIdle();
			_timeLostActive = summonSkill.getTimeLostActive();
			_referenceSkill = summonSkill.getId();
		}
		else
		{
			// defaults
			_itemConsumeId = 0;
			_itemConsumeCount = 0;
			_itemConsumeSteps = 0;
			_totalLifeTime = 1200000; // 20 minutes
			_timeLostIdle = 1000;
			_timeLostActive = 1000;
		}
		_timeRemaining = _totalLifeTime;
		lastShowntimeRemaining = _totalLifeTime;
		
		if (_itemConsumeId == 0)
			_nextItemConsumeTime = -1; // do not consume
		else if (_itemConsumeSteps == 0)
			_nextItemConsumeTime = -1; // do not consume
		else
			_nextItemConsumeTime = _totalLifeTime - _totalLifeTime / (_itemConsumeSteps + 1);
		
		// When no item consume is defined task only need to check when summon life time has ended.
		// Otherwise have to destroy items from owner's inventory in order to let summon live.
		int delay = 1000;
		
		if (Config.DEBUG && (_itemConsumeCount != 0))
			_log.warning("L2SummonInstance: Item Consume ID: " + _itemConsumeId + ", Count: " + _itemConsumeCount + ", Rate: " + _itemConsumeSteps + " times.");
		if (Config.DEBUG)
			_log.warning("L2SummonInstance: Task Delay " + (delay / 1000) + " seconds.");
		
		_summonLifeTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new SummonLifetime(getOwner(), this), delay, delay);
	}
	
	@Override
	public final int getLevel()
	{
		return (getTemplate() != null ? getTemplate().level : 0);
	}
	
	@Override
	public int getSummonType()
	{
		return 1;
	}
	
	public void setExpPenalty(float expPenalty)
	{
		_expPenalty = expPenalty;
	}
	
	public float getExpPenalty()
	{
		return _expPenalty;
	}
	
	public int getItemConsumeCount()
	{
		return _itemConsumeCount;
	}
	
	public int getItemConsumeId()
	{
		return _itemConsumeId;
	}
	
	public int getItemConsumeSteps()
	{
		return _itemConsumeSteps;
	}
	
	public int getNextItemConsumeTime()
	{
		return _nextItemConsumeTime;
	}
	
	public int getTotalLifeTime()
	{
		return _totalLifeTime;
	}
	
	public int getTimeLostIdle()
	{
		return _timeLostIdle;
	}
	
	public int getTimeLostActive()
	{
		return _timeLostActive;
	}
	
	public int getTimeRemaining()
	{
		return _timeRemaining;
	}
	
	public void setNextItemConsumeTime(int value)
	{
		_nextItemConsumeTime = value;
	}
	
	public void decNextItemConsumeTime(int value)
	{
		_nextItemConsumeTime -= value;
	}
	
	public void decTimeRemaining(int value)
	{
		_timeRemaining -= value;
	}
	
	public void addExpAndSp(int addToExp, int addToSp)
	{
		getOwner().addExpAndSp(addToExp, addToSp);
	}
	
	@Override
	public boolean doDie(L2Character killer)
	{
		if (!super.doDie(killer))
			return false;
		
		if (Config.DEBUG)
			_log.warning("L2SummonInstance: " + getTemplate().name + " (" + getOwner().getName() + ") has been killed.");
		
		if (_summonLifeTask != null)
		{
			_summonLifeTask.cancel(false);
			_summonLifeTask = null;
		}
		
		CharSummonTable.getInstance().removeServitor(getOwner());
		
		return true;
		
	}
	
	/**
	 * Servitors' skills automatically change their level based on the servitor's level.
	 * Until level 70, the servitor gets 1 lv of skill per 10 levels. After that, it is 1
	 * skill level per 5 servitor levels.  If the resulting skill level doesn't exist use
	 * the max that does exist!
	 *
	 * @see com.l2jserver.gameserver.model.actor.L2Character#doCast(com.l2jserver.gameserver.model.L2Skill)
	 */
	@Override
	public void doCast(L2Skill skill)
	{
		final int petLevel = getLevel();
		int skillLevel = petLevel/10;
		if(petLevel >= 70)
			skillLevel += (petLevel-65)/10;
		
		// adjust the level for servitors less than lv 10
		if (skillLevel < 1)
			skillLevel = 1;
		
		L2Skill skillToCast = SkillTable.getInstance().getInfo(skill.getId(),skillLevel);
		
		if (skillToCast != null)
			super.doCast(skillToCast);
		else
			super.doCast(skill);
	}
	
	@Override
	public void setRestoreSummon(boolean val)
	{
		_restoreSummon = val;
	}
	
	@Override
	public final void stopSkillEffects(int skillId)
	{
		super.stopSkillEffects(skillId);
		for (SummonEffect effect : SummonEffectsTable.getInstance().getServitorEffects(getOwner()).get(getReferenceSkill()))
		{
			if (effect.getSkill().getId() == skillId)
				SummonEffectsTable.getInstance().getServitorEffects(getOwner()).get(getReferenceSkill()).remove(effect);
		}
	}
	
	@Override
	public void store()
	{
		if (_referenceSkill == 0 || isDead())
			return;
		
		if (Config.RESTORE_SERVITOR_ON_RECONNECT)
			CharSummonTable.getInstance().saveSummon(this);
	}
	
	@Override
	public void storeEffect(boolean storeEffects)
	{
		if (!Config.SUMMON_STORE_SKILL_COOLTIME)
			return;
		
		if (getOwner().isInOlympiadMode())
			return;
		
		// Clear list for overwrite
		if (
				SummonEffectsTable.getInstance().getServitorEffectsOwner().contains(getOwner().getObjectId()) &&
				SummonEffectsTable.getInstance().getServitorEffectsOwner().get(getOwner().getObjectId()).contains(getOwner().getClassIndex()) &&
				SummonEffectsTable.getInstance().getServitorEffects(getOwner()).contains(getReferenceSkill())
			)
			SummonEffectsTable.getInstance().getServitorEffects(getOwner()).get(getReferenceSkill()).clear();
		
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			
			// Delete all current stored effects for summon to avoid dupe
			PreparedStatement statement = con.prepareStatement(DELETE_SKILL_SAVE);
			
			statement.setInt(1, getOwner().getObjectId());
			statement.setInt(2, getOwner().getClassIndex());
			statement.setInt(3, getReferenceSkill());
			
			statement.execute();
			statement.close();
			
			int buff_index = 0;
			
			final List<Integer> storedSkills = new FastList<Integer>();
			
			//Store all effect data along with calculated remaining
			statement = con.prepareStatement(ADD_SKILL_SAVE);
			
			if (storeEffects)
			{
				for (L2Effect effect : getAllEffects())
				{
					if (effect == null)
						continue;
					
					switch (effect.getEffectType())
					{
						case HEAL_OVER_TIME:
						case COMBAT_POINT_HEAL_OVER_TIME:
							// TODO: Fix me.
						case HIDE:
							continue;
					}
					
					L2Skill skill = effect.getSkill();
					if (storedSkills.contains(skill.getReuseHashCode()))
						continue;
					
					storedSkills.add(skill.getReuseHashCode());
					
					if (!effect.isHerbEffect() && effect.getInUse() && !skill.isToggle())
					{
						statement.setInt(1, getOwner().getObjectId());
						statement.setInt(2, getOwner().getClassIndex());
						statement.setInt(3, getReferenceSkill());
						statement.setInt(4, skill.getId());
						statement.setInt(5, skill.getLevel());
						statement.setInt(6, effect.getCount());
						statement.setInt(7, effect.getTime());
						statement.setInt(8, ++buff_index);
						statement.execute();
						
						if (!SummonEffectsTable.getInstance().getServitorEffectsOwner().contains(getOwner().getObjectId())) // Check if charId exists in map
							SummonEffectsTable.getInstance().getServitorEffectsOwner().put(getOwner().getObjectId(), new TIntObjectHashMap<TIntObjectHashMap<List<SummonEffect>>>());
						if (!SummonEffectsTable.getInstance().getServitorEffectsOwner().get(getOwner().getObjectId()).contains(getOwner().getClassIndex())) // Check if classIndex exists in charId map
							SummonEffectsTable.getInstance().getServitorEffectsOwner().get(getOwner().getObjectId()).put(getOwner().getClassIndex(), new TIntObjectHashMap<List<SummonEffect>>());
						if (!SummonEffectsTable.getInstance().getServitorEffects(getOwner()).contains(getReferenceSkill())) // Check is summonSkillId exists in charId+classIndex map
							SummonEffectsTable.getInstance().getServitorEffects(getOwner()).put(getReferenceSkill(), new FastList<SummonEffect>());

						SummonEffectsTable.getInstance().getServitorEffects(getOwner()).get(getReferenceSkill()).add(SummonEffectsTable.getInstance().new SummonEffect(skill, effect.getCount(), effect.getTime()));

					}
				}
				statement.close();
			}
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Could not store summon effect data: ", e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
	}
	
	@Override
	public void restoreEffects()
	{
		if (getOwner().isInOlympiadMode())
			return;
		
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			if (
					!SummonEffectsTable.getInstance().getServitorEffectsOwner().contains(getOwner().getObjectId()) ||
					!SummonEffectsTable.getInstance().getServitorEffectsOwner().get(getOwner().getObjectId()).contains(getOwner().getClassIndex()) ||
					!SummonEffectsTable.getInstance().getServitorEffects(getOwner()).contains(getReferenceSkill())
				)
			{
				statement = con.prepareStatement(RESTORE_SKILL_SAVE);
				statement.setInt(1, getOwner().getObjectId());
				statement.setInt(2, getOwner().getClassIndex());
				statement.setInt(3, getReferenceSkill());
				ResultSet rset = statement.executeQuery();
				
				while (rset.next())
				{
					int effectCount = rset.getInt("effect_count");
					int effectCurTime = rset.getInt("effect_cur_time");
					
					final L2Skill skill = SkillTable.getInstance().getInfo(rset.getInt("skill_id"), rset.getInt("skill_level"));
					if (skill == null)
						continue;
					
					if (skill.hasEffects())
					{
						if (!SummonEffectsTable.getInstance().getServitorEffectsOwner().contains(getOwner().getObjectId())) // Check if charId exists in map
							SummonEffectsTable.getInstance().getServitorEffectsOwner().put(getOwner().getObjectId(), new TIntObjectHashMap<TIntObjectHashMap<List<SummonEffect>>>());
						if (!SummonEffectsTable.getInstance().getServitorEffectsOwner().get(getOwner().getObjectId()).contains(getOwner().getClassIndex())) // Check if classIndex exists in charId map
							SummonEffectsTable.getInstance().getServitorEffectsOwner().get(getOwner().getObjectId()).put(getOwner().getClassIndex(), new TIntObjectHashMap<List<SummonEffect>>());
						if (!SummonEffectsTable.getInstance().getServitorEffects(getOwner()).contains(getReferenceSkill())) // Check is summonSkillId exists in charId+classIndex map
							SummonEffectsTable.getInstance().getServitorEffects(getOwner()).put(getReferenceSkill(), new FastList<SummonEffect>());
						
						SummonEffectsTable.getInstance().getServitorEffects(getOwner()).get(getReferenceSkill()).add(SummonEffectsTable.getInstance().new SummonEffect(skill, effectCount, effectCurTime));
					}
				}
				
				rset.close();
				statement.close();
			}
			
			statement = con.prepareStatement(DELETE_SKILL_SAVE);
			statement.setInt(1, getOwner().getObjectId());
			statement.setInt(2, getOwner().getClassIndex());
			statement.setInt(3, getReferenceSkill());
			statement.executeUpdate();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Could not restore " + this + " active effect data: " + e.getMessage(), e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
			if (
					!SummonEffectsTable.getInstance().getServitorEffectsOwner().contains(getOwner().getObjectId()) ||
					!SummonEffectsTable.getInstance().getServitorEffectsOwner().get(getOwner().getObjectId()).contains(getOwner().getClassIndex()) ||
					!SummonEffectsTable.getInstance().getServitorEffects(getOwner()).contains(getReferenceSkill())
				)
				return;

			for (SummonEffect se : SummonEffectsTable.getInstance().getServitorEffects(getOwner()).get(getReferenceSkill()))
			{
				Env env = new Env();
				env.player = this;
				env.target = this;
				env.skill = se.getSkill();
				L2Effect ef;
				for (EffectTemplate et : se.getSkill().getEffectTemplates())
				{
					ef = et.getEffect(env);
					if (ef != null)
					{
						ef.setCount(se.getEffectCount());
						ef.setFirstTime(se.getEffectCurTime());
						ef.scheduleEffect();
					}
				}
			}
		}
	}
	
	static class SummonLifetime implements Runnable
	{
		private L2PcInstance _activeChar;
		private L2SummonInstance _summon;
		
		SummonLifetime(L2PcInstance activeChar, L2SummonInstance newpet)
		{
			_activeChar = activeChar;
			_summon = newpet;
		}
		
		public void run()
		{
			if (Config.DEBUG)
				log.warning("L2SummonInstance: " + _summon.getTemplate().name + " (" + _activeChar.getName() + ") run task.");
			
			try
			{
				double oldTimeRemaining = _summon.getTimeRemaining();
				int maxTime = _summon.getTotalLifeTime();
				double newTimeRemaining;
				
				// if pet is attacking
				if (_summon.isAttackingNow())
				{
					_summon.decTimeRemaining(_summon.getTimeLostActive());
				}
				else
				{
					_summon.decTimeRemaining(_summon.getTimeLostIdle());
				}
				newTimeRemaining = _summon.getTimeRemaining();
				// check if the summon's lifetime has ran out
				if (newTimeRemaining < 0)
				{
					_summon.unSummon(_activeChar);
				}
				// check if it is time to consume another item
				else if ((newTimeRemaining <= _summon.getNextItemConsumeTime()) && (oldTimeRemaining > _summon.getNextItemConsumeTime()))
				{
					_summon.decNextItemConsumeTime(maxTime / (_summon.getItemConsumeSteps() + 1));
					
					// check if owner has enought itemConsume, if requested
					if (_summon.getItemConsumeCount() > 0 && _summon.getItemConsumeId() != 0 && !_summon.isDead() && !_summon.destroyItemByItemId("Consume", _summon.getItemConsumeId(), _summon.getItemConsumeCount(), _activeChar, true))
					{
						_summon.unSummon(_activeChar);
					}
				}
				
				// prevent useless packet-sending when the difference isn't visible.
				if ((_summon.lastShowntimeRemaining - newTimeRemaining) > maxTime / 352)
				{
					_summon.getOwner().sendPacket(new SetSummonRemainTime(maxTime, (int) newTimeRemaining));
					_summon.lastShowntimeRemaining = (int) newTimeRemaining;
					_summon.updateEffectIcons();
				}
			}
			catch (Exception e)
			{
				log.log(Level.SEVERE, "Error on player [" + _activeChar.getName() + "] summon item consume task.", e);
			}
		}
	}
	
	@Override
	public void unSummon(L2PcInstance owner)
	{
		if (Config.DEBUG)
			_log.warning("L2SummonInstance: " + getTemplate().name + " (" + owner.getName() + ") unsummoned.");
		
		if (_summonLifeTask != null)
		{
			_summonLifeTask.cancel(false);
			_summonLifeTask = null;
		}
		
		super.unSummon(owner);
		
		if (!_restoreSummon)
			CharSummonTable.getInstance().removeServitor(owner);
	}
	
	@Override
	public boolean destroyItem(String process, int objectId, long count, L2Object reference, boolean sendMessage)
	{
		return getOwner().destroyItem(process, objectId, count, reference, sendMessage);
	}
	
	@Override
	public boolean destroyItemByItemId(String process, int itemId, long count, L2Object reference, boolean sendMessage)
	{
		if (Config.DEBUG)
			_log.warning("L2SummonInstance: " + getTemplate().name + " (" + getOwner().getName() + ") consume.");
		
		return getOwner().destroyItemByItemId(process, itemId, count, reference, sendMessage);
	}
	
	@Override
	public byte getAttackElement()
	{
		if (getOwner() == null || !getOwner().getClassId().isSummoner())
			return super.getAttackElement();
		
		return getOwner().getAttackElement();
	}
	
	@Override
	public int getAttackElementValue(byte attribute)
	{
		if (getOwner() == null || !getOwner().getClassId().isSummoner() || getOwner().getExpertiseWeaponPenalty() > 0)
			return super.getAttackElementValue(attribute);
		
		// 80% of the owner (onwer already has only 20%)
		return 4 * getOwner().getAttackElementValue(attribute);
	}
	
	@Override
	public int getDefenseElementValue(byte attribute)
	{
		if (getOwner() == null || !getOwner().getClassId().isSummoner())
			return super.getDefenseElementValue(attribute);
		
		// bonus from owner
		return super.getDefenseElementValue(attribute) + getOwner().getDefenseElementValue(attribute);
	}
	
	public void setTimeRemaining(int time)
	{
		_timeRemaining = time;
	}
	
	public int getReferenceSkill()
	{
		return _referenceSkill;
	}
}
