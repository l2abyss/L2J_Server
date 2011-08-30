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

import java.util.ArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2jserver.gameserver.GameTimeController;
import com.l2jserver.gameserver.ThreadPoolManager;
import com.l2jserver.gameserver.datatables.SkillTable;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.actor.instance.L2SummonInstance;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.AbnormalStatusUpdate;
import com.l2jserver.gameserver.network.serverpackets.ExOlympiadSpelledInfo;
import com.l2jserver.gameserver.network.serverpackets.MagicSkillLaunched;
import com.l2jserver.gameserver.network.serverpackets.MagicSkillUse;
import com.l2jserver.gameserver.network.serverpackets.PartySpelled;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;
import com.l2jserver.gameserver.skills.AbnormalEffect;
import com.l2jserver.gameserver.skills.Env;
import com.l2jserver.gameserver.skills.funcs.Func;
import com.l2jserver.gameserver.skills.funcs.FuncTemplate;
import com.l2jserver.gameserver.skills.funcs.Lambda;
import com.l2jserver.gameserver.templates.effects.EffectTemplate;
import com.l2jserver.gameserver.templates.skills.L2EffectType;
import com.l2jserver.gameserver.templates.skills.L2SkillType;

/**
 * This class ...
 *
 * @version $Revision: 1.1.2.1.2.12 $ $Date: 2005/04/11 10:06:07 $
 */
public abstract class L2Effect
{
	protected static final Logger _log = Logger.getLogger(L2Effect.class.getName());
	
	public static enum EffectState
	{
		CREATED,
		ACTING,
		FINISHING
	}
	
	private static final Func[] _emptyFunctionSet = new Func[0];
	
	//member _effector is the instance of L2Character that cast/used the spell/skill that is
	//causing this effect.  Do not confuse with the instance of L2Character that
	//is being affected by this effect.
	private final L2Character _effector;
	
	//member _effected is the instance of L2Character that was affected
	//by this effect.  Do not confuse with the instance of L2Character that
	//casted/used this effect.
	private final L2Character _effected;
	
	//the skill that was used.
	private final L2Skill _skill;
	
	private final boolean _isHerbEffect;
	
	//or the items that was used.
	//private final L2Item _item;
	
	// the value of an update
	private final Lambda _lambda;
	
	// the current state
	private EffectState _state;
	
	// period, seconds
	private final int _abnormalTime;
	private int _periodStartTicks;
	private int _periodFirstTime;
	
	private EffectTemplate _template;
	
	// function templates
	private final FuncTemplate[] _funcTemplates;
	
	//initial count
	private int _totalCount;
	// counter
	private int _count;
	
	// abnormal effect mask
	private AbnormalEffect _abnormalEffect;
	// special effect mask
	private AbnormalEffect[] _specialEffect;
	// event effect mask
	private AbnormalEffect _eventEffect;
	// show icon
	private boolean _icon;
	// is self effect?
	private boolean _isSelfEffect = false;
	// is passive effect?
	private boolean _isPassiveEffect = false;

	public boolean preventExitUpdate;
	
	private final class EffectTask implements Runnable
	{
		public void run()
		{
			try
			{
				_periodFirstTime = 0;
				_periodStartTicks = GameTimeController.getGameTicks();
				L2Effect.this.scheduleEffect();
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "", e);
			}
		}
	}
	
	private ScheduledFuture<?> _currentFuture;
	
	/** The Identifier of the stack group */
	private final String _abnormalType;
	
	/** The position of the effect in the stack group */
	private final byte _abnormalLvl;
	
	private boolean _inUse = false;
	private boolean _startConditionsCorrect = true;
	
	/**
	 * For special behavior. See Formulas.calcEffectSuccess
	 */
	private double _effectPower;
	private L2SkillType _effectSkillType;
	
	/**
	 * <font color="FF0000"><b>WARNING: scheduleEffect nolonger inside constructor</b></font><br>
	 * So you must call it explicitly
	 */
	protected L2Effect(Env env, EffectTemplate template)
	{
		_state = EffectState.CREATED;
		_skill = env.skill;
		//_item = env._item == null ? null : env._item.getItem();
		_template = template;
		_effected = env.target;
		_effector = env.player;
		_lambda = template.lambda;
		_funcTemplates = template.funcTemplates;
		_count = template.counter;
		_totalCount = _count;
		
		// Support for retail herbs duration when _effected has a Summon
		int temp = template.abnormalTime;
		
		if ((_skill.getId() > 2277 && _skill.getId() < 2286) || (_skill.getId() >= 2512 && _skill.getId() <= 2514))
		{
			if (_effected instanceof L2SummonInstance ||
					(_effected instanceof L2PcInstance && ((L2PcInstance) _effected).getPet() instanceof L2SummonInstance))
			{
				temp /= 2;
			}
		}
		
		if (env.skillMastery)
			temp *= 2;
		
		_abnormalTime = temp;
		_abnormalEffect = template.abnormalEffect;
		_specialEffect = template.specialEffect;
		_eventEffect = template.eventEffect;
		_abnormalType = template.abnormalType;
		_abnormalLvl = template.abnormalLvl;
		_periodStartTicks = GameTimeController.getGameTicks();
		_periodFirstTime = 0;
		_icon = template.icon;
		_effectPower = template.effectPower;
		_effectSkillType = template.effectType;

		_isHerbEffect = _skill.getName().contains("Herb");

		/*
		 * Commented out by DrHouse:
		 * scheduleEffect can call onStart before effect is completly
		 * initialized on constructor (child classes constructor)
		 */
		//scheduleEffect();
	}
	
	/**
	 * Special constructor to "steal" buffs. Must be implemented on
	 * every child class that can be stolen.<br><br>
	 * 
	 * <font color="FF0000"><b>WARNING: scheduleEffect nolonger inside constructor</b></font>
	 * <br>So you must call it explicitly
	 * @param env
	 * @param effect
	 */
	protected L2Effect(Env env, L2Effect effect)
	{
		_template = effect._template;
		_state = EffectState.CREATED;
		_skill = env.skill;
		_effected = env.target;
		_effector = env.player;
		_lambda = _template.lambda;
		_funcTemplates = _template.funcTemplates;
		_count = effect.getCount();
		_totalCount = _template.counter;
		_abnormalTime = _template.abnormalTime;
		_abnormalEffect = _template.abnormalEffect;
		_specialEffect = _template.specialEffect;
		_eventEffect = _template.eventEffect;
		_abnormalType = _template.abnormalType;
		_abnormalLvl = _template.abnormalLvl;
		_periodStartTicks = effect.getPeriodStartTicks();
		_periodFirstTime = effect.getTime();
		_icon = _template.icon;
		
		_isHerbEffect = _skill.getName().contains("Herb");

		/*
		 * Commented out by DrHouse:
		 * scheduleEffect can call onStart before effect is completly
		 * initialized on constructor (child classes constructor)
		 */
		//scheduleEffect();
	}
	
	public int getCount()
	{
		return _count;
	}
	
	public int getTotalCount()
	{
		return _totalCount;
	}
	
	public void setCount(int newcount)
	{
		_count = Math.min(newcount, _totalCount); // sanity check
	}

	public void setFirstTime(int newFirstTime)
	{
		_periodFirstTime = Math.min(newFirstTime, _abnormalTime);
		_periodStartTicks -= _periodFirstTime * GameTimeController.TICKS_PER_SECOND;
	}

	public boolean getShowIcon()
	{
		return _icon;
	}
	
	public int getAbnormalTime()
	{
		return _abnormalTime;
	}
	
	public int getTime()
	{
		return (GameTimeController.getGameTicks() - _periodStartTicks) / GameTimeController.TICKS_PER_SECOND;
	}
	
	/**
	 * Returns the elapsed time of the task.
	 * @return Time in seconds.
	 */
	public int getTaskTime()
	{
		if (_count == _totalCount)
			return 0;
		return (Math.abs(_count - _totalCount + 1) * _abnormalTime) + getTime() + 1;
	}
	
	public boolean getInUse()
	{
		return _inUse;
	}
	
	public boolean setInUse(boolean inUse)
	{
		_inUse = inUse;
		if (_inUse)
			_startConditionsCorrect = onStart();
		else
			onExit();
		
		return _startConditionsCorrect;
	}
	
	public String getAbnormalType()
	{
		return _abnormalType;
	}
	
	public byte getAbnormalLvl()
	{
		return _abnormalLvl;
	}
	
	public final L2Skill getSkill()
	{
		return _skill;
	}
	
	public final L2Character getEffector()
	{
		return _effector;
	}
	
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
	
	public boolean isPassiveEffect()
	{
		return _isPassiveEffect;
	}
	
	public void setPassiveEffect()
	{
		_isPassiveEffect = true;
	}
	
	public boolean isHerbEffect()
	{
		return _isHerbEffect;
	}
	
	public final double calc()
	{
		Env env = new Env();
		env.player = _effector;
		env.target = _effected;
		env.skill = _skill;
		return _lambda.calc(env);
	}
	
	private final synchronized void startEffectTask()
	{
		if (_abnormalTime > 0)
		{
			stopEffectTask();
			final int initialDelay = Math.max((_abnormalTime - _periodFirstTime) * 1000, 5);
			if (_count > 1)
				_currentFuture = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new EffectTask(), initialDelay, _abnormalTime * 1000);
			else
				_currentFuture = ThreadPoolManager.getInstance().scheduleEffect(new EffectTask(), initialDelay);
		}
		if (_state == EffectState.ACTING)
		{
			if (isSeflEffectType())
				_effector.addEffect(this);
			else
				_effected.addEffect(this);
		}
	}
	
	/**
	 * Stop the L2Effect task and send Server->Client update packet.<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Cancel the effect in the the abnormal effect map of the L2Character </li>
	 * <li>Stop the task of the L2Effect, remove it and update client magic icon </li><BR><BR>
	 *
	 */
	public final void exit()
	{
		this.exit(false);
	}
	
	public final void exit(boolean preventUpdate)
	{
		preventExitUpdate = preventUpdate;
		_state = EffectState.FINISHING;
		scheduleEffect();
	}
	
	/**
	 * Stop the task of the L2Effect, remove it and update client magic icon.<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Cancel the task </li>
	 * <li>Stop and remove L2Effect from L2Character and update client magic icon </li><BR><BR>
	 *
	 */
	public final synchronized void stopEffectTask()
	{
		if (_currentFuture != null)
		{
			// Cancel the task
			_currentFuture.cancel(false);
			//ThreadPoolManager.getInstance().removeEffect(_currentTask);
			
			_currentFuture = null;
			
			if (isSeflEffectType() && getEffector() != null)
				getEffector().removeEffect(this);
			else if (getEffected() != null)
				getEffected().removeEffect(this);
		}
	}
	
	/** returns effect type */
	public abstract L2EffectType getEffectType();
	
	/** Notify started */
	public boolean onStart()
	{
		if (_abnormalEffect != AbnormalEffect.NULL)
			getEffected().startAbnormalEffect(_abnormalEffect);
		if (_specialEffect != null)
			getEffected().startSpecialEffect(_specialEffect);
		if (_eventEffect != AbnormalEffect.NULL && getEffected() instanceof L2PcInstance)
			getEffected().getActingPlayer().startEventEffect(_eventEffect);
		return true;
	}
	
	/**
	 * Cancel the effect in the the abnormal effect map of the effected L2Character.<BR><BR>
	 */
	public void onExit()
	{
		if (_abnormalEffect != AbnormalEffect.NULL)
			getEffected().stopAbnormalEffect(_abnormalEffect);
		if (_specialEffect != null)
			getEffected().stopSpecialEffect(_specialEffect);
		if (_eventEffect != AbnormalEffect.NULL && getEffected() instanceof L2PcInstance)
			getEffected().getActingPlayer().stopEventEffect(_eventEffect);
	}
	
	/** Return true for continuation of this effect */
	public abstract boolean onActionTime();
	
	public final void scheduleEffect()
	{
		switch (_state)
		{
			case CREATED:
			{
				_state = EffectState.ACTING;
				
				if (_skill.isPvpSkill() && _icon && getEffected() instanceof L2PcInstance)
				{
					SystemMessage smsg = SystemMessage.getSystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT);
					smsg.addSkillName(_skill);
					getEffected().sendPacket(smsg);
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
				if (_count > 0)
				{
					_count--;
					if (getInUse())
					{ // effect has to be in use
						if (onActionTime() && _startConditionsCorrect && _count > 0)
							return; // false causes effect to finish right away
					}
					else if (_count > 0)
					{ // do not finish it yet, in case reactivated
						return;
					}
				}
				_state = EffectState.FINISHING;
			}
			case FINISHING:
			{
				//If the time left is equal to zero, send the message
				if (_count == 0 && _icon && getEffected() instanceof L2PcInstance)
				{
					SystemMessage smsg3 = SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_WORN_OFF);
					smsg3.addSkillName(_skill);
					getEffected().sendPacket(smsg3);
				}
				// if task is null - stopEffectTask does not remove effect
				if (_currentFuture == null && getEffected() != null)
				{
					getEffected().removeEffect(this);
				}
				// Stop the task of the L2Effect, remove it and update client magic icon
				stopEffectTask();
				
				// Cancel the effect in the the abnormal effect map of the L2Character
				if (getInUse() || !(_count > 1 || _abnormalTime > 0))
					if (_startConditionsCorrect)
						onExit();
				
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
	
	public Func[] getStatFuncs()
	{
		if (_funcTemplates == null)
			return _emptyFunctionSet;
		ArrayList<Func> funcs = new ArrayList<Func>(_funcTemplates.length);
		
		Env env = new Env();
		env.player = getEffector();
		env.target = getEffected();
		env.skill = getSkill();
		Func f;
		
		for (FuncTemplate t : _funcTemplates)
		{
			f = t.getFunc(env, this); // effect is owner
			if (f != null)
				funcs.add(f);
		}
		if (funcs.isEmpty())
			return _emptyFunctionSet;
		
		return funcs.toArray(new Func[funcs.size()]);
	}
	
	public final void addIcon(AbnormalStatusUpdate mi)
	{
		if (_state != EffectState.ACTING)
			return;

		final ScheduledFuture<?> future = _currentFuture;
		final L2Skill sk = getSkill();
		if (_totalCount > 1)
		{
			if (sk.isPotion())
				mi.addEffect(sk.getId(), getLevel(), sk.getBuffDuration() - (getTaskTime() * 1000));
			else
				mi.addEffect(sk.getId(), getLevel(), -1);
		}
		else if (future != null)
			mi.addEffect(sk.getId(), getLevel(), (int) future.getDelay(TimeUnit.MILLISECONDS));
		else if (_abnormalTime == -1)
			mi.addEffect(sk.getId(), getLevel(), _abnormalTime);
	}
	
	public final void addPartySpelledIcon(PartySpelled ps)
	{
		if (_state != EffectState.ACTING)
			return;

		final ScheduledFuture<?> future = _currentFuture;
		final L2Skill sk = getSkill();
		if (future != null)
			ps.addPartySpelledEffect(sk.getId(), getLevel(), (int) future.getDelay(TimeUnit.MILLISECONDS));
		else if (_abnormalTime == -1)
			ps.addPartySpelledEffect(sk.getId(), getLevel(), _abnormalTime);
	}
	
	public final void addOlympiadSpelledIcon(ExOlympiadSpelledInfo os)
	{
		if (_state != EffectState.ACTING)
			return;

		final ScheduledFuture<?> future = _currentFuture;
		final L2Skill sk = getSkill();
		if (future != null)
			os.addEffect(sk.getId(), getLevel(), (int) future.getDelay(TimeUnit.MILLISECONDS));
		else if (_abnormalTime == -1)
			os.addEffect(sk.getId(), getLevel(), _abnormalTime);
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
		return _effectPower;
	}
	
	public L2SkillType getSkillType()
	{
		return _effectSkillType;
	}
	
	public boolean canBeStolen()
	{
		if(!effectCanBeStolen()
				|| this.getEffectType() == L2EffectType.TRANSFORMATION
				|| this.getSkill().isPassive()
				|| this.getSkill().isToggle()
				|| this.getSkill().isDebuff()
				|| this.getSkill().isHeroSkill()
				|| this.getSkill().isGMSkill()
				|| (this.getSkill().isPotion() && (this.getSkill().getId() != 2274 && this.getSkill().getId() != 2341)) // Hardcode for now :<
				|| this.isHerbEffect()
				|| !this.getSkill().canBeDispeled())
			return false;
		return true;
	}

	/**
	 * Return true if effect itself can be stolen
	 * @return
	 */
	protected boolean effectCanBeStolen()
	{
		return false;
	}

	/**
	 * Return bit flag for current effect
	 * @return int flag
	 */
	public int getEffectFlags()
	{
		return 0;
	}
	
	@Override
	public String toString()
	{
		return "L2Effect [_skill=" + _skill + ", _state=" + _state + ", _period=" + _abnormalTime + "]";
	}
	
	public boolean isSeflEffectType()
	{
		return false;
	}
}