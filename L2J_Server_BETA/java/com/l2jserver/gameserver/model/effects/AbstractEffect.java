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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import com.l2jserver.Config;
import com.l2jserver.gameserver.handler.EffectHandler;
import com.l2jserver.gameserver.model.ChanceCondition;
import com.l2jserver.gameserver.model.StatsSet;
import com.l2jserver.gameserver.model.conditions.Condition;
import com.l2jserver.gameserver.model.interfaces.IChanceSkillTrigger;
import com.l2jserver.gameserver.model.skills.BuffInfo;
import com.l2jserver.gameserver.model.skills.funcs.Func;
import com.l2jserver.gameserver.model.skills.funcs.FuncTemplate;
import com.l2jserver.gameserver.model.stats.Env;

/**
 * Abstract effect implementation.<br>
 * Instant effects should not override {@link #onExit(BuffInfo)}.<br>
 * Instant effects should not override {@link #canStart(BuffInfo)}, all checks should be done {@link #onStart(BuffInfo)}. Do not call super class methods {@link #onStart(BuffInfo)} nor {@link #onExit(BuffInfo)}.<br>
 * @since <a href="http://trac.l2jserver.com/changeset/6249">Changeset 6249</a> the "effect steal constructor" is deprecated.
 * @author Zoey76
 */
public abstract class AbstractEffect implements IChanceSkillTrigger
{
	protected static final Logger _log = Logger.getLogger(AbstractEffect.class.getName());
	
	// Conditions
	private final Condition _attachCond;
	// private final Condition _applyCond; // TODO: Use or cleanup.
	private List<FuncTemplate> _funcTemplates;
	private final String _name;
	private final double _val;
	/** Ticks. */
	private final int _ticks;
	private final int _triggeredId;
	private final int _triggeredLevel;
	private final ChanceCondition _chanceCondition;
	private final StatsSet _parameters;
	
	/**
	 * Abstract effect constructor.
	 * @param attachCond
	 * @param applyCond
	 * @param set
	 * @param params
	 */
	protected AbstractEffect(Condition attachCond, Condition applyCond, StatsSet set, StatsSet params)
	{
		_attachCond = attachCond;
		// _applyCond = applyCond;
		_name = set.getString("name");
		_val = set.getDouble("val", 0);
		_ticks = set.getInt("ticks", 0);
		_triggeredId = set.getInt("triggeredId", 0);
		_triggeredLevel = set.getInt("triggeredLevel", 1);
		_chanceCondition = ChanceCondition.parse(set.getString("chanceType", null), set.getInt("activationChance", -1), set.getInt("activationMinDamage", -1), set.getString("activationElements", null), set.getString("activationSkills", null), set.getBoolean("pvpChanceOnly", false));
		_parameters = params;
	}
	
	public static final AbstractEffect createEffect(Condition attachCond, Condition applyCond, StatsSet set, StatsSet params)
	{
		final String name = set.getString("name");
		final Class<? extends AbstractEffect> handler = EffectHandler.getInstance().getHandler(name);
		if (handler == null)
		{
			_log.warning(AbstractEffect.class.getSimpleName() + ": Requested unexistent effect handler: " + name);
			return null;
		}
		
		final Constructor<?> constructor;
		try
		{
			constructor = handler.getConstructor(Condition.class, Condition.class, StatsSet.class, StatsSet.class);
		}
		catch (NoSuchMethodException | SecurityException e1)
		{
			_log.warning(AbstractEffect.class.getSimpleName() + ": Requested unexistent constructor for effect handler: " + name);
			e1.printStackTrace();
			return null;
		}
		
		try
		{
			return (AbstractEffect) constructor.newInstance(attachCond, applyCond, set, params);
		}
		catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Tests the attach condition.
	 * @param env the data
	 * @return {@code true} if there isn't a condition to test or it's passed, {@code false} otherwise
	 */
	public boolean testConditions(Env env)
	{
		return (_attachCond == null) || _attachCond.test(env);
	}
	
	/**
	 * Attachs a function template.
	 * @param f the function
	 */
	public void attach(FuncTemplate f)
	{
		if (_funcTemplates == null)
		{
			_funcTemplates = new ArrayList<>(1);
		}
		_funcTemplates.add(f);
	}
	
	/**
	 * Gets the effect name.
	 * @return the name
	 */
	public String getName()
	{
		return _name;
	}
	
	/**
	 * Gets the generic value.
	 * @return the value
	 */
	public double getValue()
	{
		return _val;
	}
	
	/**
	 * Gets the effect ticks
	 * @return the ticks
	 */
	public int getTicks()
	{
		return _ticks;
	}
	
	public double getTicksMultiplier()
	{
		return (getTicks() * Config.EFFECT_TICK_RATIO) / 1000f;
	}
	
	public List<FuncTemplate> getFuncTemplates()
	{
		return _funcTemplates;
	}
	
	@Override
	public int getTriggeredChanceId()
	{
		return _triggeredId;
	}
	
	@Override
	public int getTriggeredChanceLevel()
	{
		return _triggeredLevel;
	}
	
	@Override
	public ChanceCondition getTriggeredChanceCondition()
	{
		return _chanceCondition;
	}
	
	/**
	 * Verify if this effect template has parameters.
	 * @return {@code true} if this effect template has parameters, {@code false} otherwise
	 */
	public boolean hasParameters()
	{
		return _parameters != null;
	}
	
	/**
	 * Get the parameters.
	 * @return the parameters of this effect template
	 */
	public StatsSet getParameters()
	{
		return _parameters;
	}
	
	/**
	 * Calculates whether this effects land or not.<br>
	 * If it lands will be scheduled and added to the character effect list.<br>
	 * Override in effect implementation to change behavior. <br>
	 * <b>Warning:</b> Must be used only for instant effects continuous effects will not call this they have their success handled by activate_rate.
	 * @param info the buff info
	 * @return {@code true} if this effect land, {@code false} otherwise
	 */
	public boolean calcSuccess(BuffInfo info)
	{
		return true;
	}
	
	/**
	 * Get this effect's type.<br>
	 * TODO: Remove.
	 * @return the effect type
	 */
	public L2EffectType getEffectType()
	{
		return L2EffectType.NONE;
	}
	
	/**
	 * Verify if the buff can start.<br>
	 * Used for continuous effects.
	 * @param info the buff info
	 * @return {@code true} if all the start conditions are meet, {@code false} otherwise
	 */
	public boolean canStart(BuffInfo info)
	{
		return true;
	}
	
	/**
	 * Called on effect start.
	 * @param info the buff info
	 */
	public void onStart(BuffInfo info)
	{
		
	}
	
	/**
	 * Called on each tick.<br>
	 * If the abnormal time is lesser than zero it will last forever.
	 * @param info the buff info
	 * @return if {@code true} this effect will continue forever, if {@code false} it will stop after abnormal time has passed
	 */
	public boolean onActionTime(BuffInfo info)
	{
		return false;
	}
	
	/**
	 * Called when the effect is exited.
	 * @param info the buff info
	 */
	public void onExit(BuffInfo info)
	{
		
	}
	
	/**
	 * Get this effect's stats functions.
	 * @param env the data
	 * @return a list of stat functions.
	 */
	public List<Func> getStatFuncs(Env env)
	{
		if (getFuncTemplates() == null)
		{
			return Collections.<Func> emptyList();
		}
		
		final List<Func> funcs = new ArrayList<>(getFuncTemplates().size());
		for (FuncTemplate t : getFuncTemplates())
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
		return "Effect " + _name;
	}
	
	public void decreaseForce()
	{
		
	}
	
	public void increaseEffect()
	{
		
	}
	
	public boolean checkCondition(Object obj)
	{
		return true;
	}
	
	@Override
	public boolean triggersChanceSkill()
	{
		return _triggeredId > 0;
	}
	
	/**
	 * Verify if this effect is an instant effect.
	 * @return {@code true} if this effect is instant, {@code false} otherwise
	 */
	public boolean isInstant()
	{
		return false;
	}
}