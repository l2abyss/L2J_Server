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
package com.l2jserver.gameserver.model.effects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2jserver.gameserver.GameTimeController;
import com.l2jserver.gameserver.ThreadPoolManager;
import com.l2jserver.gameserver.datatables.SkillTable;
import com.l2jserver.gameserver.model.ChanceCondition;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.interfaces.IChanceSkillTrigger;
import com.l2jserver.gameserver.model.skills.L2Skill;
import com.l2jserver.gameserver.model.skills.funcs.Func;
import com.l2jserver.gameserver.model.skills.funcs.FuncTemplate;
import com.l2jserver.gameserver.model.skills.funcs.Lambda;
import com.l2jserver.gameserver.model.stats.Env;
import com.l2jserver.gameserver.model.stats.Formulas;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.AbnormalStatusUpdate;
import com.l2jserver.gameserver.network.serverpackets.ExOlympiadSpelledInfo;
import com.l2jserver.gameserver.network.serverpackets.MagicSkillLaunched;
import com.l2jserver.gameserver.network.serverpackets.MagicSkillUse;
import com.l2jserver.gameserver.network.serverpackets.PartySpelled;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;

/**
 * Abstract effect implementation.
 * @author Zoey76
 */
public abstract class L2Effect implements IChanceSkillTrigger
{
	protected static final Logger _log = Logger.getLogger(L2Effect.class.getName());
	/** The character that creates this effect. */
	private final L2Character _effector;
	/** The character that is affected by this effect. */
	private final L2Character _effected;
	/** The skill that launched this effect. */
	private final L2Skill _skill;
	/** The value on an update. */
	private final Lambda _lambda;
	/** The current state. */
	private EffectState _state;
	/** The game ticks at the start of this effect. */
	protected int _periodStartTicks;
	protected int _periodFirstTime;
	/** The effect template. */
	private final EffectTemplate _template;
	/** Effect tick count. */
	private int _tickCount;
	/** Effect's abnormal time. */
	private final int _abnormalTime;
	/** If {@code true} then it's a self-effect. */
	private boolean _isSelfEffect = false;
	/** If {@code true} then prevent exit update. */
	private boolean _preventExitUpdate;
	private volatile ScheduledFuture<?> _currentFuture;
	/** If {@code true} then this effect is in use. */
	private boolean _inUse = false;
	/** If {@code true} then this effect's start condition are meet. */
	private boolean _startConditionsCorrect = true;
	/** If {@code true} then this effect has been cancelled. */
	private boolean _isRemoved = false;
	
	protected final class EffectTask implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				_periodFirstTime = 0;
				_periodStartTicks = GameTimeController.getInstance().getGameTicks();
				scheduleEffect();
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "", e);
			}
		}
	}
	
	/**
	 * @param env DTO with required data
	 * @param template the effect template
	 */
	protected L2Effect(Env env, EffectTemplate template)
	{
		_state = EffectState.CREATED;
		_skill = env.getSkill();
		_template = template;
		_effected = env.getTarget();
		_effector = env.getCharacter();
		_lambda = template.getLambda();
		_tickCount = 0;
		_abnormalTime = Formulas.calcEffectAbnormalTime(env, template);
		_periodStartTicks = GameTimeController.getInstance().getGameTicks();
		_periodFirstTime = 0;
	}
	
	/**
	 * Special constructor to "steal" buffs.<br>
	 * Must be implemented on every child class that can be stolen.
	 * @param env DTO with required data
	 * @param effect the stolen effect, used as "template"
	 */
	protected L2Effect(Env env, L2Effect effect)
	{
		_template = effect._template;
		_state = EffectState.CREATED;
		_skill = env.getSkill();
		_effected = env.getTarget();
		_effector = env.getCharacter();
		_lambda = _template.getLambda();
		_tickCount = effect.getTickCount();
		_abnormalTime = effect.getAbnormalTime();
		_periodStartTicks = effect.getPeriodStartTicks();
		_periodFirstTime = effect.getTime();
	}
	
	/**
	 * Get the current tick count.
	 * @return the tick count
	 */
	public int getTickCount()
	{
		return _tickCount;
	}
	
	public void setCount(int newTickCount)
	{
		_tickCount = Math.min(newTickCount, _template.getTotalTickCount());
	}
	
	public void setFirstTime(int newFirstTime)
	{
		_periodFirstTime = Math.min(newFirstTime, _abnormalTime);
		_periodStartTicks -= _periodFirstTime * GameTimeController.TICKS_PER_SECOND;
	}
	
	/**
	 * Verify if this effect display an icon.
	 * @return {@code true} if this effect display an icon, {@code false} otherwise
	 */
	public boolean isIconDisplay()
	{
		return _template.isIconDisplay();
	}
	
	/**
	 * Get this effect's calculated abnormal time.
	 * @return the abnormal time
	 */
	public int getAbnormalTime()
	{
		return _abnormalTime;
	}
	
	/**
	 * Get the elapsed time from the beginning of this effect.
	 * @return the elapsed time
	 */
	public int getTime()
	{
		return (GameTimeController.getInstance().getGameTicks() - _periodStartTicks) / GameTimeController.TICKS_PER_SECOND;
	}
	
	/**
	 * Get the remaining time.
	 * @return the remaining time
	 */
	public int getTimeLeft()
	{
		if (_template.getTotalTickCount() > 0)
		{
			return (((_template.getTotalTickCount() - _tickCount) + 1) * (_abnormalTime / _template.getTotalTickCount())) - getTime();
		}
		return _abnormalTime - getTime();
	}
	
	/**
	 * Verify if the effect is in use.
	 * @return {@code true} if the effect is in use, {@code false} otherwise
	 */
	public boolean isInUse()
	{
		return _inUse;
	}
	
	/**
	 * Set the effect in use.<br>
	 * If is set to {@code true}, {@link #onStart()} is invoked, otherwise {@link #onExit()} is invoked.
	 * @param inUse the value to set
	 * @return {@link #_startConditionsCorrect}
	 */
	public boolean setInUse(boolean inUse)
	{
		_inUse = inUse;
		if (_inUse)
		{
			_startConditionsCorrect = onStart();
		}
		else
		{
			onExit();
		}
		return _startConditionsCorrect;
	}
	
	/**
	 * Get the skill that launched this effect.
	 * @return the skill related to this effect
	 */
	public final L2Skill getSkill()
	{
		return _skill;
	}
	
	/**
	 * Get the character that evoked this effect.
	 * @return the effector
	 */
	public final L2Character getEffector()
	{
		return _effector;
	}
	
	/**
	 * Get the character that received this effect.
	 * @return the effected
	 */
	public final L2Character getEffected()
	{
		return _effected;
	}
	
	public boolean isSelfEffect()
	{
		return _isSelfEffect;
	}
	
	public void setSelfEffect()
	{
		_isSelfEffect = true;
	}
	
	public final double calc()
	{
		final Env env = new Env();
		env.setCharacter(_effector);
		env.setTarget(_effected);
		env.setSkill(_skill);
		return _lambda.calc(env);
	}
	
	/**
	 * Calculates whether this effects land or not.<br>
	 * If it lands will be scheduled and added to the character effect list.<br>
	 * Override in effect implementation to change behavior.
	 * @return {@code true} if this effect land, {@code false} otherwise
	 */
	public boolean calcSuccess()
	{
		final Env env = new Env();
		env.setSkillMastery(Formulas.calcSkillMastery(getEffector(), getSkill()));
		env.setCharacter(getEffector());
		env.setTarget(getEffected());
		env.setSkill(getSkill());
		env.setEffect(this);
		return Formulas.calcEffectSuccess(env);
	}
	
	/**
	 * Start the effect task.<br>
	 * If the effect has ticks defined it will be scheduled.<br>
	 * If abnormal time is defined (greater than 1) the period will be calculated like abnormal time divided total tick count.<br>
	 * Otherwise it each tick will represent 1 second (1000 milliseconds).
	 */
	private final void startEffectTask()
	{
		if (isInstant())
		{
			_currentFuture = ThreadPoolManager.getInstance().scheduleEffect(new EffectTask(), 0);
			return;
		}
		
		stopEffectTask();
		final int delay = Math.max((_abnormalTime - _periodFirstTime) * 1000, 5); // Sanity check
		if (_template.getTotalTickCount() > 0)
		{
			// TODO: If default abnormal time is changed to 0, the first check below must be updated as well.
			final int period = ((_abnormalTime > 1) ? (_abnormalTime / _template.getTotalTickCount()) : _template.getTotalTickCount()) * 1000;
			_currentFuture = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new EffectTask(), delay / period, period);
		}
		else
		{
			_currentFuture = ThreadPoolManager.getInstance().scheduleEffect(new EffectTask(), delay);
		}
	}
	
	/**
	 * Exit this effect without preventing an update.
	 */
	public final void exit()
	{
		exit(false);
	}
	
	/**
	 * Exit this effect.
	 * @param preventExitUpdate
	 */
	public final void exit(boolean preventExitUpdate)
	{
		_preventExitUpdate = preventExitUpdate;
		_state = EffectState.FINISHING;
		scheduleEffect();
	}
	
	/**
	 * Stop the task of this effect, remove it and update client magic icon.
	 */
	public final void stopEffectTask()
	{
		if (_currentFuture != null)
		{
			// Cancel the task
			_currentFuture.cancel(false);
			_currentFuture = null;
			
			if (getEffected() != null)
			{
				getEffected().getEffectList().remove(this);
			}
		}
	}
	
	/**
	 * Get this effect's type.
	 * @return the effect type
	 */
	public abstract L2EffectType getEffectType();
	
	/**
	 * Notify started.
	 * @return {@code true} if all the start conditions are meet, {@code false} otherwise
	 */
	public boolean onStart()
	{
		if (_template.getAbnormalEffect() != AbnormalEffect.NULL)
		{
			getEffected().startAbnormalEffect(_template.getAbnormalEffect());
		}
		if (_template.getSpecialEffect() != null)
		{
			getEffected().startSpecialEffect(_template.getSpecialEffect());
		}
		if ((_template.getEventEffect() != AbnormalEffect.NULL) && getEffected().isPlayer())
		{
			getEffected().getActingPlayer().startEventEffect(_template.getEventEffect());
		}
		return true;
	}
	
	/**
	 * Cancel the effect in the the abnormal effect map of the effected L2Character.
	 */
	public void onExit()
	{
		if (_template.getAbnormalEffect() != AbnormalEffect.NULL)
		{
			getEffected().stopAbnormalEffect(_template.getAbnormalEffect());
		}
		if (_template.getSpecialEffect() != null)
		{
			getEffected().stopSpecialEffect(_template.getSpecialEffect());
		}
		if ((_template.getEventEffect() != AbnormalEffect.NULL) && getEffected().isPlayer())
		{
			getEffected().getActingPlayer().stopEventEffect(_template.getEventEffect());
		}
	}
	
	/**
	 * Method called on each tick.
	 * @return {@code true} for continuation of this effect, {@code false} otherwise
	 */
	public boolean onActionTime()
	{
		return false;
	}
	
	/**
	 * Schedule this effect.
	 */
	public final void scheduleEffect()
	{
		switch (_state)
		{
			case CREATED:
			{
				_state = isInstant() ? EffectState.FINISHING : EffectState.ACTING;
				
				if (_skill.isPVP() && isIconDisplay() && getEffected().isPlayer())
				{
					final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT);
					sm.addSkillName(_skill);
					getEffected().sendPacket(sm);
				}
				
				if (_abnormalTime != 0)
				{
					startEffectTask();
					return;
				}
				_startConditionsCorrect = onStart();
			}
			case ACTING:
			{
				if (isInUse())
				{
					_tickCount++; // Increase tick count.
					if (onActionTime() && _startConditionsCorrect)
					{
						return; // Do not finish.
					}
				}
				
				if (_tickCount <= _template.getTotalTickCount())
				{
					return; // Do not finish it yet, has remaining ticks.
				}
				
				_state = EffectState.FINISHING;
			}
			case FINISHING:
			{
				// Message
				if (getEffected().isPlayer() && isIconDisplay())
				{
					SystemMessageId smId = null;
					if (getSkill().isToggle())
					{
						smId = SystemMessageId.S1_HAS_BEEN_ABORTED;
					}
					else if (isRemoved())
					{
						smId = SystemMessageId.EFFECT_S1_DISAPPEARED;
					}
					else if (_tickCount >= _template.getTotalTickCount())
					{
						smId = SystemMessageId.S1_HAS_WORN_OFF;
					}
					
					if (smId != null)
					{
						final SystemMessage sm = SystemMessage.getSystemMessage(smId);
						sm.addSkillName(getSkill());
						getEffected().sendPacket(sm);
					}
				}
				
				// if task is null - stopEffectTask does not remove effect
				if ((_currentFuture == null) && (getEffected() != null))
				{
					getEffected().getEffectList().remove(this);
				}
				
				// Stop the task of this effect, remove it and update client magic icon.
				stopEffectTask();
				
				// Cancel the effect in the the abnormal effect list of the character.
				if (isInUse() || !((_tickCount > 1) || (_abnormalTime > 0)))
				{
					if (_startConditionsCorrect)
					{
						onExit();
					}
				}
				
				if (_skill.getAfterEffectId() > 0)
				{
					final L2Skill skill = SkillTable.getInstance().getInfo(_skill.getAfterEffectId(), _skill.getAfterEffectLvl());
					if (skill != null)
					{
						getEffected().broadcastPacket(new MagicSkillUse(_effected, skill.getId(), skill.getLevel(), 0, 0));
						getEffected().broadcastPacket(new MagicSkillLaunched(_effected, skill.getId(), skill.getLevel()));
						skill.getEffects(getEffected(), getEffected());
					}
				}
			}
		}
	}
	
	/**
	 * Get this effect's stats functions.
	 * @return a list of stat functions.
	 */
	public List<Func> getStatFuncs()
	{
		if (_template.getFuncTemplates() == null)
		{
			return Collections.<Func> emptyList();
		}
		
		final List<Func> funcs = new ArrayList<>(_template.getFuncTemplates().size());
		final Env env = new Env();
		env.setCharacter(_effector);
		env.setTarget(_effected);
		env.setSkill(_skill);
		for (FuncTemplate t : _template.getFuncTemplates())
		{
			final Func f = t.getFunc(env, this);
			if (f != null)
			{
				funcs.add(f);
			}
		}
		return funcs;
	}
	
	/**
	 * Add the abnormal status update data for this effect.
	 * @param mi the abnormal status packet
	 */
	public final void addIcon(AbnormalStatusUpdate mi)
	{
		if (_state != EffectState.ACTING)
		{
			return;
		}
		
		if (_abnormalTime == -1)
		{
			mi.addEffect(getSkill(), -1);
		}
		else
		{
			mi.addEffect(getSkill(), getTimeLeft());
		}
	}
	
	/**
	 * Add the party spelled data for this effect.
	 * @param ps the party spelled packet
	 */
	public final void addPartySpelledIcon(PartySpelled ps)
	{
		if (_state != EffectState.ACTING)
		{
			return;
		}
		
		final ScheduledFuture<?> future = _currentFuture;
		if (future != null)
		{
			ps.addPartySpelledEffect(getSkill(), (int) future.getDelay(TimeUnit.SECONDS));
		}
		else if (_abnormalTime == -1)
		{
			ps.addPartySpelledEffect(getSkill(), -1);
		}
	}
	
	/**
	 * Add the olympiad spelled data for this effect.
	 * @param os the olympiad spelled packet
	 */
	public final void addOlympiadSpelledIcon(ExOlympiadSpelledInfo os)
	{
		if (_state != EffectState.ACTING)
		{
			return;
		}
		
		final ScheduledFuture<?> future = _currentFuture;
		if (future != null)
		{
			os.addEffect(getSkill(), (int) future.getDelay(TimeUnit.SECONDS));
		}
		else if (_abnormalTime == -1)
		{
			os.addEffect(getSkill(), -1);
		}
	}
	
	public int getPeriodStartTicks()
	{
		return _periodStartTicks;
	}
	
	/**
	 * Get the effect template.
	 * @return the effect template
	 */
	public EffectTemplate getEffectTemplate()
	{
		return _template;
	}
	
	/**
	 * TODO: Unhardcode skill Id.
	 * @return {@code true} if effect itself can be stolen, {@code false} otherwise
	 */
	public boolean canBeStolen()
	{
		return !getSkill().isPassive() && !getSkill().isToggle() && !getSkill().isDebuff() && !getSkill().isHeroSkill() && !getSkill().isGMSkill() && !(getSkill().isStatic() && (getSkill().getId() != 2341)) && getSkill().canBeDispeled();
	}
	
	/**
	 * Get the effect flags.
	 * @return bit flag for current effect
	 */
	public int getEffectFlags()
	{
		return EffectFlag.NONE.getMask();
	}
	
	@Override
	public String toString()
	{
		return "Effect " + getClass().getSimpleName() + ", " + _skill + ", State: " + _state + ", Time: " + _abnormalTime + ", Remaining: " + getTimeLeft();
	}
	
	public void decreaseForce()
	{
		
	}
	
	public void increaseEffect()
	{
		
	}
	
	@Override
	public boolean triggersChanceSkill()
	{
		return false;
	}
	
	@Override
	public int getTriggeredChanceId()
	{
		return 0;
	}
	
	@Override
	public int getTriggeredChanceLevel()
	{
		return 0;
	}
	
	@Override
	public ChanceCondition getTriggeredChanceCondition()
	{
		return null;
	}
	
	public boolean isPreventExitUpdate()
	{
		return _preventExitUpdate;
	}
	
	public void setPreventExitUpdate(boolean val)
	{
		_preventExitUpdate = val;
	}
	
	/**
	 * Verify if this effect is an instant effect.
	 * @return {@code true} if this effect is instant, {@code false} otherwise
	 */
	public boolean isInstant()
	{
		return false;
	}
	
	/**
	 * Verify if this effect has been cancelled.
	 * @return {@code true} if this effect has been cancelled, {@code false} otherwise
	 */
	public boolean isRemoved()
	{
		return _isRemoved;
	}
	
	/**
	 * Set the effect to removed.
	 * @param val the value to set
	 */
	public void setRemoved(boolean val)
	{
		_isRemoved = val;
	}
}