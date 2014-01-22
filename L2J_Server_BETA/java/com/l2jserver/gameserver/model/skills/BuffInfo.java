/*
 * Copyright (C) 2004-2013 L2J Server
 * 
 * This file is part of L2J Server.
 * 
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.l2jserver.gameserver.model.skills;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import com.l2jserver.Config;
import com.l2jserver.gameserver.GameTimeController;
import com.l2jserver.gameserver.ThreadPoolManager;
import com.l2jserver.gameserver.model.CharEffectList;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.effects.AbstractEffect;
import com.l2jserver.gameserver.model.effects.EffectTaskInfo;
import com.l2jserver.gameserver.model.effects.EffectTickTask;
import com.l2jserver.gameserver.model.stats.Env;
import com.l2jserver.gameserver.model.stats.Formulas;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;

/**
 * Buff Info.<br>
 * Complex DTO that holds all the information for a given buff (or debuff or dance/song) set of effects issued by an skill.
 * @author Zoey76
 */
public final class BuffInfo
{
	// Data
	/** Data. */
	private final Env _env;
	/** The effects. */
	private final List<AbstractEffect> _effects = new ArrayList<>(1);
	// Tasks
	/** Effect tasks for ticks. */
	private Map<AbstractEffect, EffectTaskInfo> _tasks;
	/** Task for effect ending. */
	private BuffTimeTask _effectTimeTask;
	/** Scheduled future. */
	private ScheduledFuture<?> _scheduledFutureTimeTask;
	// Time and ticks
	/** Abnormal time. */
	private int _abnormalTime;
	/** The game ticks at the start of this effect. */
	private final int _periodStartTicks;
	// Misc
	/** If {@code true} then this effect has been cancelled. */
	private boolean _isRemoved = false;
	/** If {@code true} then this effect is in use (or has been stop because an Herb took place). */
	private boolean _isInUse = true;
	
	/**
	 * Buff Info constructor.
	 * @param env the cast data
	 */
	public BuffInfo(Env env)
	{
		_env = env;
		_abnormalTime = Formulas.calcEffectAbnormalTime(env);
		_periodStartTicks = GameTimeController.getInstance().getGameTicks();
	}
	
	/**
	 * Gets the effects on this buff info.
	 * @return the effects
	 */
	public List<AbstractEffect> getEffects()
	{
		return _effects;
	}
	
	/**
	 * Adds an effect to this buff info.
	 * @param effect the effect to add
	 */
	public void addEffect(AbstractEffect effect)
	{
		_effects.add(effect);
	}
	
	/**
	 * Adds an effect task to this buff info.<br>
	 * Uses double-checked locking to initialize the map if it's necessary.
	 * @param effect the effect that owns the task
	 * @param effectTaskInfo the task info
	 */
	private void addTask(AbstractEffect effect, EffectTaskInfo effectTaskInfo)
	{
		if (_tasks == null)
		{
			synchronized (this)
			{
				if (_tasks == null)
				{
					_tasks = new ConcurrentHashMap<>();
				}
			}
		}
		_tasks.put(effect, effectTaskInfo);
	}
	
	/**
	 * Gets the task for the given effect.
	 * @param effect the effect
	 * @return the task
	 */
	private EffectTaskInfo getEffectTask(AbstractEffect effect)
	{
		return (_tasks == null) ? null : _tasks.get(effect);
	}
	
	/**
	 * Gets the skill that created this buff info.
	 * @return the skill
	 */
	public L2Skill getSkill()
	{
		return _env.getSkill();
	}
	
	public Env getEnv()
	{
		return _env;
	}
	
	/**
	 * Gets the calculated abnormal time.
	 * @return the abnormal time
	 */
	public int getAbnormalTime()
	{
		return _abnormalTime;
	}
	
	/**
	 * Sets the abnormal time.
	 * @param abnormalTime the abnormal time to set
	 */
	public void setAbnormalTime(int abnormalTime)
	{
		_abnormalTime = abnormalTime;
	}
	
	/**
	 * Gets the period start ticks.
	 * @return the period start
	 */
	public int getPeriodStartTicks()
	{
		return _periodStartTicks;
	}
	
	/**
	 * Get the remaining time in seconds for this buff info.
	 * @return the elapsed time
	 */
	public int getTime()
	{
		return _abnormalTime - ((GameTimeController.getInstance().getGameTicks() - _periodStartTicks) / GameTimeController.TICKS_PER_SECOND);
	}
	
	/**
	 * Verify if this buff info has been cancelled.
	 * @return {@code true} if this buff info has been cancelled, {@code false} otherwise
	 */
	public boolean isRemoved()
	{
		return _isRemoved;
	}
	
	/**
	 * Set the buff info to removed.
	 * @param val the value to set
	 */
	public void setRemoved(boolean val)
	{
		_isRemoved = val;
	}
	
	/**
	 * Verify if this buff info is in use.
	 * @return {@code true} if this buff info is in use, {@code false} otherwise
	 */
	public boolean isInUse()
	{
		return _isInUse;
	}
	
	/**
	 * Set the buff info to in use.
	 * @param val the value to set
	 */
	public void setInUse(boolean val)
	{
		_isInUse = val;
	}
	
	/**
	 * Gets the character that launched the buff.
	 * @return the effector
	 */
	public L2Character getEffector()
	{
		return _env.getCharacter();
	}
	
	/**
	 * Gets the target of the skill.
	 * @return the effected
	 */
	public L2Character getEffected()
	{
		return _env.getTarget();
	}
	
	/**
	 * Stops all the effects for this buff info.<br>
	 * Removes effects stats.<br>
	 * <b>It will not remove the buff info from the effect list</b>.<br>
	 * Instead call {@link CharEffectList#stopSkillEffects(boolean, L2Skill)}
	 * @param removed if {@code true} the skill will be handled as removed
	 */
	public void stopAllEffects(boolean removed)
	{
		setRemoved(removed);
		// Cancels the task that will end this buff info
		if ((_scheduledFutureTimeTask != null) && !_scheduledFutureTimeTask.isCancelled())
		{
			_scheduledFutureTimeTask.cancel(false);
		}
		
		// Remove stats
		removeStats();
		
		finishEffects();
	}
	
	public void initializeEffects()
	{
		// When effects are initialized, the successfully landed.
		if (_env.getTarget().isPlayer() && !_env.getSkill().isPassive())
		{
			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT);
			sm.addSkillName(_env.getSkill());
			_env.getTarget().sendPacket(sm);
		}
		
		// Creates a task that will stop all the effects.
		if (_abnormalTime > 0)
		{
			_effectTimeTask = new BuffTimeTask(this);
			_scheduledFutureTimeTask = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(_effectTimeTask, 0, 1000L);
		}
		
		applyAbnormalVisualEffects();
		
		for (AbstractEffect effect : _effects)
		{
			if (effect.isInstant())
			{
				continue;
			}
			
			// Call on start.
			effect.onStart(this);
			
			// If it's a continuous effect, if has ticks schedule a task with period, otherwise schedule a simple task to end it.
			if (effect.getTicks() > 0)
			{
				// The task for the effect ticks
				final EffectTickTask effectTask = new EffectTickTask(this, effect);
				final ScheduledFuture<?> scheduledFuture = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(effectTask, effect.getTicks() * Config.EFFECT_TICK_RATIO, effect.getTicks() * Config.EFFECT_TICK_RATIO);
				// Adds the task for ticking
				addTask(effect, new EffectTaskInfo(effectTask, scheduledFuture));
			}
			
			// Add stats.
			_env.getTarget().addStatFuncs(effect.getStatFuncs(_env));
		}
	}
	
	/**
	 * Called on each tick.<br>
	 * Verify if the effect should end and the effect task should be cancelled.
	 * @param effect the effect that is ticking
	 * @param tickCount the tick count
	 */
	public void onTick(AbstractEffect effect, int tickCount)
	{
		boolean continueForever = false;
		// If the effect is in use, allow it to affect the effected.
		if (_isInUse)
		{
			// Callback for on action time event.
			continueForever = effect.onActionTime(this);
		}
		
		if (!continueForever && _env.getSkill().isToggle())
		{
			final EffectTaskInfo task = getEffectTask(effect);
			if (task != null)
			{
				task.getScheduledFuture().cancel(true); // Allow to finish current run.
				_env.getTarget().getEffectList().remove(true, this); // Remove the buff from the effect list.
			}
		}
	}
	
	public void finishEffects()
	{
		removeAbnormalVisualEffects();
		
		for (AbstractEffect effect : _effects)
		{
			// Instant effects shouldn't call onExit(..).
			if ((effect != null) && !effect.isInstant())
			{
				effect.onExit(this);
			}
		}
		
		// Cancels the ticking task.
		if (_tasks != null)
		{
			for (Entry<AbstractEffect, EffectTaskInfo> entry : _tasks.entrySet())
			{
				entry.getValue().getScheduledFuture().cancel(true);
			}
		}
		
		// Sends the proper system message.
		SystemMessageId smId = null;
		if (_env.getSkill().isToggle())
		{
			smId = SystemMessageId.S1_HAS_BEEN_ABORTED;
		}
		else if (isRemoved())
		{
			smId = SystemMessageId.EFFECT_S1_HAS_BEEN_REMOVED;
		}
		else if (!_env.getSkill().isPassive())
		{
			smId = SystemMessageId.S1_HAS_WORN_OFF;
		}
		
		if (smId != null)
		{
			final SystemMessage sm = SystemMessage.getSystemMessage(smId);
			sm.addSkillName(_env.getSkill());
			_env.getTarget().sendPacket(sm);
		}
		
		if (this == _env.getTarget().getEffectList().getShortBuff())
		{
			_env.getTarget().getEffectList().shortBuffStatusUpdate(null);
		}
	}
	
	/**
	 * Applies all the abnormal visual effects to the effected.<br>
	 * Prevents multiple updates.
	 */
	private void applyAbnormalVisualEffects()
	{
		if ((_env.getTarget() == null) || (_env.getSkill() == null))
		{
			return;
		}
		
		if (_env.getSkill().hasAbnormalVisualEffects())
		{
			_env.getTarget().startAbnormalVisualEffect(false, _env.getSkill().getAbnormalVisualEffects());
		}
		
		if (_env.getTarget().isPlayer() && _env.getSkill().hasAbnormalVisualEffectsEvent())
		{
			_env.getTarget().startAbnormalVisualEffect(false, _env.getSkill().getAbnormalVisualEffectsEvent());
		}
		
		if (_env.getSkill().hasAbnormalVisualEffectsSpecial())
		{
			_env.getTarget().startAbnormalVisualEffect(false, _env.getSkill().getAbnormalVisualEffectsSpecial());
		}
		
		_env.getTarget().updateAbnormalEffect();
	}
	
	/**
	 * Removes all the abnormal visual effects from the effected.<br>
	 * Prevents multiple updates.
	 */
	private void removeAbnormalVisualEffects()
	{
		if ((_env.getTarget() == null) || (_env.getSkill() == null))
		{
			return;
		}
		
		if (_env.getSkill().hasAbnormalVisualEffects())
		{
			_env.getTarget().stopAbnormalVisualEffect(false, _env.getSkill().getAbnormalVisualEffects());
		}
		
		if (_env.getTarget().isPlayer() && _env.getSkill().hasAbnormalVisualEffectsEvent())
		{
			_env.getTarget().stopAbnormalVisualEffect(false, _env.getSkill().getAbnormalVisualEffectsEvent());
		}
		
		if (_env.getSkill().hasAbnormalVisualEffectsSpecial())
		{
			_env.getTarget().stopAbnormalVisualEffect(false, _env.getSkill().getAbnormalVisualEffectsSpecial());
		}
		
		_env.getTarget().updateAbnormalEffect();
	}
	
	/**
	 * Adds the buff stats.
	 */
	public void addStats()
	{
		for (AbstractEffect effect : _effects)
		{
			_env.getTarget().addStatFuncs(effect.getStatFuncs(_env));
		}
	}
	
	/**
	 * Removes the buff stats.
	 */
	public void removeStats()
	{
		for (AbstractEffect effect : _effects)
		{
			_env.getTarget().removeStatsOwner(effect);
		}
		_env.getTarget().removeStatsOwner(_env.getSkill());
	}
	
	/**
	 * Gets the effect tick count.
	 * @param effect the effect
	 * @return the current tick count
	 */
	public int getTickCount(AbstractEffect effect)
	{
		if (_tasks != null)
		{
			final EffectTaskInfo effectTaskInfo = _tasks.get(effect);
			if (effectTaskInfo != null)
			{
				return effectTaskInfo.getEffectTask().getTickCount();
			}
		}
		return 0;
	}
}