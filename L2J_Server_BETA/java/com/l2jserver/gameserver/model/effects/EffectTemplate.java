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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2jserver.gameserver.handler.EffectHandler;
import com.l2jserver.gameserver.model.ChanceCondition;
import com.l2jserver.gameserver.model.conditions.Condition;
import com.l2jserver.gameserver.model.skills.funcs.FuncTemplate;
import com.l2jserver.gameserver.model.skills.funcs.Lambda;
import com.l2jserver.gameserver.model.stats.Env;

/**
 * Effect template class.
 * @author mkizub, Zoey76
 */
public class EffectTemplate
{
	private static final Logger _log = Logger.getLogger(EffectTemplate.class.getName());
	
	private final Class<?> _func;
	private final Constructor<?> _constructor;
	private final Condition _attachCond;
	// private final Condition _applyCond; // TODO: Use or cleanup.
	private final Lambda _lambda;
	private final int _totalTickCount;
	/** Custom abnormal time. */
	private final int _customAbnormalTime;
	private final AbnormalEffect _abnormalEffect;
	private final AbnormalEffect[] _specialEffect;
	private final AbnormalEffect _eventEffect;
	private List<FuncTemplate> _funcTemplates;
	private final boolean _showIcon;
	private final String _funcName;
	private final double _effectPower; // to handle chance
	private final int _triggeredId;
	private final int _triggeredLevel;
	private final ChanceCondition _chanceCondition;
	
	public EffectTemplate(Condition attachCond, Condition applyCond, String func, Lambda lambda, int totalTickCount, int abnormalTime, AbnormalEffect abnormalEffect, AbnormalEffect[] specialEffect, AbnormalEffect eventEffect, boolean showIcon, double power, int trigId, int trigLvl, ChanceCondition chanceCond)
	{
		_attachCond = attachCond;
		// _applyCond = applyCond;
		_lambda = lambda;
		_totalTickCount = totalTickCount;
		_customAbnormalTime = abnormalTime;
		_abnormalEffect = abnormalEffect;
		_specialEffect = specialEffect;
		_eventEffect = eventEffect;
		_showIcon = showIcon;
		_effectPower = power;
		_triggeredId = trigId;
		_triggeredLevel = trigLvl;
		_chanceCondition = chanceCond;
		_funcName = func;
		_func = EffectHandler.getInstance().getHandler(func);
		if (_func == null)
		{
			_log.warning(getClass().getSimpleName() + ": Requested Unexistent effect handler: " + func);
			throw new RuntimeException();
		}
		
		try
		{
			_constructor = _func.getConstructor(Env.class, EffectTemplate.class);
		}
		catch (NoSuchMethodException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	public L2Effect getEffect(Env env)
	{
		return getEffect(env, false);
	}
	
	public L2Effect getEffect(Env env, boolean ignoreTest)
	{
		if (!ignoreTest && ((_attachCond != null) && !_attachCond.test(env)))
		{
			return null;
		}
		
		try
		{
			L2Effect effect = (L2Effect) _constructor.newInstance(env, this);
			return effect;
		}
		catch (IllegalAccessException | InstantiationException e)
		{
			_log.log(Level.WARNING, "", e);
			return null;
		}
		catch (InvocationTargetException e)
		{
			_log.log(Level.WARNING, "Error creating new instance of Class " + _func + " Exception was: " + e.getTargetException().getMessage(), e.getTargetException());
			return null;
		}
	}
	
	/**
	 * Creates an L2Effect instance from an existing one and an Env object.
	 * @param env
	 * @param stolen
	 * @return the stolen effect
	 */
	public L2Effect getStolenEffect(Env env, L2Effect stolen)
	{
		Class<?> func = EffectHandler.getInstance().getHandler(_funcName);
		if (func == null)
		{
			throw new RuntimeException();
		}
		
		Constructor<?> stolenCons;
		try
		{
			stolenCons = func.getConstructor(Env.class, L2Effect.class);
		}
		catch (NoSuchMethodException e)
		{
			throw new RuntimeException(e);
		}
		
		try
		{
			final L2Effect effect = (L2Effect) stolenCons.newInstance(env, stolen);
			// if (_applyCond != null)
			// {
			// effect.setCondition(_applyCond);
			// }
			return effect;
		}
		catch (IllegalAccessException | InstantiationException e)
		{
			_log.log(Level.WARNING, "", e);
			return null;
		}
		catch (InvocationTargetException e)
		{
			_log.log(Level.WARNING, "Error creating new instance of Class " + func + " Exception was: " + e.getTargetException().getMessage(), e.getTargetException());
			return null;
		}
	}
	
	public void attach(FuncTemplate f)
	{
		if (_funcTemplates == null)
		{
			_funcTemplates = new ArrayList<>(1);
		}
		_funcTemplates.add(f);
	}
	
	public Lambda getLambda()
	{
		return _lambda;
	}
	
	public int getTotalTickCount()
	{
		return _totalTickCount;
	}
	
	public int getCustomAbnormalTime()
	{
		return _customAbnormalTime;
	}
	
	public AbnormalEffect getAbnormalEffect()
	{
		return _abnormalEffect;
	}
	
	public AbnormalEffect[] getSpecialEffect()
	{
		return _specialEffect;
	}
	
	public AbnormalEffect getEventEffect()
	{
		return _eventEffect;
	}
	
	public List<FuncTemplate> getFuncTemplates()
	{
		return _funcTemplates;
	}
	
	public boolean isIconDisplay()
	{
		return _showIcon;
	}
	
	public double getEffectPower()
	{
		return _effectPower;
	}
	
	public int getTriggeredId()
	{
		return _triggeredId;
	}
	
	public int getTriggeredLevel()
	{
		return _triggeredLevel;
	}
	
	public ChanceCondition getChanceCondition()
	{
		return _chanceCondition;
	}
}