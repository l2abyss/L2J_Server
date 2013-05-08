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
		_tickCount = template.getTotalTickCount(); // Initial tick count is total tick count.
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
	
	public int getTickCount()
	{
		return _tickCount;
	}
	
	public int getTotalTickCount()
	{
		return _template.getTotalTickCount();
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
	 * @return {@code true} if this effect display an icon, {@code false} otherwise
	 */
	public boolean isIconDisplay()
	{
		return _template.isIconDisplay();
	}
	
	/**
	 * @return this effect's calculated abnormal time
	 */
	public int getAbnormalTime()
	{
		return _abnormalTime;
	}
	
	public int getTime()
	{
		return (GameTimeController.getInstance().getGameTicks() - _periodStartTicks) / GameTimeController.TICKS_PER_SECOND;
	}
	
	/**
	 * Get the elapsed time.
	 * @return the elapsed time of the task in seconds
	 */
	public int getTaskTime()
	{
		if (_tickCount == _template.getTotalTickCount())
		{
			return 0;
		}
		return Math.abs(((_tickCount - _template.getTotalTickCount()) + 1) * _abnormalTime) + getTime() + 1;
	}
	
	/**
	 * @return {@code true} if the effect is in use, {@code false} otherwise
	 */
	public boolean isInUse()
	{
		return _inUse;
	}
	
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
		Env env = new Env();
		env.setCharacter(_effector);
		env.setTarget(_effected);
		env.setSkill(_skill);
		return _lambda.calc(env);
	}
	
	private final void startEffectTask()
	{
		if (_abnormalTime > 0)
		{
			stopEffectTask();
			final int initialDelay = Math.max((_abnormalTime - _periodFirstTime) * 1000, 5);
			if (_tickCount > 1)
			{
				_currentFuture = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new EffectTask(), initialDelay, _abnormalTime * 1000);
			}
			else
			{
				_currentFuture = ThreadPoolManager.getInstance().scheduleEffect(new EffectTask(), initialDelay);
			}
		}
	}
	
	/**
	 * Stop the L2Effect task and send Server->Client update packet.<br>
	 * <B><U>Actions</U>:</B>
	 * <ul>
	 * <li>Cancel the effect in the the abnormal effect map of the L2Character</li>
	 * <li>Stop the task of the L2Effect, remove it and update client magic icon</li>
	 * </ul>
	 */
	public final void exit()
	{
		exit(false);
	}
	
	public final void exit(boolean preventExitUpdate)
	{
		_preventExitUpdate = preventExitUpdate;
		_state = EffectState.FINISHING;
		scheduleEffect();
	}
	
	/**
	 * Stop the task of the L2Effect, remove it and update client magic icon.<br>
	 * <B><U>Actions</U>:</B>
	 * <ul>
	 * <li>Cancel the task</li>
	 * <li>Stop and remove L2Effect from L2Character and update client magic icon</li>
	 * </ul>
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
	
	public final void scheduleEffect()
	{
		switch (_state)
		{
			case CREATED:
			{
				_state = EffectState.ACTING;
				
				if (_skill.isPVP() && isIconDisplay() && getEffected().isPlayer())
				{
					SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT);
					sm.addSkillName(_skill);
					getEffected().sendPacket(sm);
				}
				
				if (_abnormalTime != 0)
				{
					startEffectTask();
					return;
				}
				// effects not having count or period should start
				_startConditionsCorrect = onStart();
			}
			case ACTING:
			{
				if (_tickCount > 0)
				{
					_tickCount--;
					if (isInUse())
					{
						// effect has to be in use
						if (onActionTime() && _startConditionsCorrect && (_tickCount >= 0))
						{
							return; // false causes effect to finish right away
						}
					}
					else if (_tickCount > 0)
					{
						// do not finish it yet, in case reactivated
						return;
					}
				}
				_state = EffectState.FINISHING;
			}
			case FINISHING:
			{
				// If the time left is equal to zero, send the message
				if ((_tickCount == 0) && isIconDisplay() && getEffected().isPlayer())
				{
					SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_WORN_OFF);
					sm.addSkillName(_skill);
					getEffected().sendPacket(sm);
				}
				
				// if task is null - stopEffectTask does not remove effect
				if ((_currentFuture == null) && (getEffected() != null))
				{
					getEffected().getEffectList().remove(this);
				}
				
				// Stop the task of the L2Effect, remove it and update client magic icon
				stopEffectTask();
				
				// Cancel the effect in the the abnormal effect map of the L2Character
				if (isInUse() || !((_tickCount > 1) || (_abnormalTime > 0)))
				{
					if (_startConditionsCorrect)
					{
						onExit();
					}
				}
				
				if (_skill.getAfterEffectId() > 0)
				{
					L2Skill skill = SkillTable.getInstance().getInfo(_skill.getAfterEffectId(), _skill.getAfterEffectLvl());
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
	
	public final void addIcon(AbnormalStatusUpdate mi)
	{
		if (_state != EffectState.ACTING)
		{
			return;
		}
		
		final ScheduledFuture<?> future = _currentFuture;
		final L2Skill sk = getSkill();
		if (_template.getTotalTickCount() > 1)
		{
			if (sk.isStatic())
			{
				mi.addEffect(sk.getDisplayId(), sk.getDisplayLevel(), (_abnormalTime - getTaskTime()) * 1000);
			}
			else
			{
				mi.addEffect(sk.getDisplayId(), sk.getDisplayLevel(), -1);
			}
		}
		else if (future != null)
		{
			mi.addEffect(sk.getDisplayId(), getLevel(), (int) future.getDelay(TimeUnit.MILLISECONDS));
		}
		else if (_abnormalTime == -1)
		{
			mi.addEffect(sk.getDisplayId(), getLevel(), _abnormalTime);
		}
	}
	
	public final void addPartySpelledIcon(PartySpelled ps)
	{
		if (_state != EffectState.ACTING)
		{
			return;
		}
		
		final ScheduledFuture<?> future = _currentFuture;
		final L2Skill sk = getSkill();
		if (future != null)
		{
			ps.addPartySpelledEffect(sk.getDisplayId(), getLevel(), (int) future.getDelay(TimeUnit.MILLISECONDS));
		}
		else if (_abnormalTime == -1)
		{
			ps.addPartySpelledEffect(sk.getDisplayId(), getLevel(), _abnormalTime);
		}
	}
	
	public final void addOlympiadSpelledIcon(ExOlympiadSpelledInfo os)
	{
		if (_state != EffectState.ACTING)
		{
			return;
		}
		
		final ScheduledFuture<?> future = _currentFuture;
		final L2Skill sk = getSkill();
		if (future != null)
		{
			os.addEffect(sk.getDisplayId(), sk.getDisplayLevel(), (int) future.getDelay(TimeUnit.MILLISECONDS));
		}
		else if (_abnormalTime == -1)
		{
			os.addEffect(sk.getDisplayId(), sk.getDisplayLevel(), _abnormalTime);
		}
	}
	
	public int getLevel()
	{
		return getSkill().getLevel();
	}
	
	public int getPeriodStartTicks()
	{
		return _periodStartTicks;
	}
	
	public EffectTemplate getEffectTemplate()
	{
		return _template;
	}
	
	public double getEffectPower()
	{
		return _template.getEffectPower();
	}
	
	/**
	 * TODO: Unhardcode skill Id.
	 * @return {@code true} if effect itself can be stolen, {@code false} otherwise
	 */
	public boolean canBeStolen()
	{
		return (getEffectType() != L2EffectType.TRANSFORMATION) && !getSkill().isPassive() && !getSkill().isToggle() && !getSkill().isDebuff() && !getSkill().isHeroSkill() && !getSkill().isGMSkill() && !(getSkill().isStatic() && ((getSkill().getId() != 2274) && (getSkill().getId() != 2341))) && getSkill().canBeDispeled();
	}
	
	/**
	 * @return bit flag for current effect
	 */
	public int getEffectFlags()
	{
		return EffectFlag.NONE.getMask();
	}
	
	@Override
	public String toString()
	{
		return "Effect " + getClass().getSimpleName() + ", " + _skill + ", State: " + _state + ", Abnormal time: " + _abnormalTime;
	}
	
	public void decreaseForce()
	{
		
	}
	
	public void increaseEffect()
	{
		
	}
	
	public int getForceEffect()
	{
		return 0;
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
}