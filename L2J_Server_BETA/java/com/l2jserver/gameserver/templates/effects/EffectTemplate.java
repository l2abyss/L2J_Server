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
package com.l2jserver.gameserver.templates.effects;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2jserver.gameserver.model.ChanceCondition;
import com.l2jserver.gameserver.model.L2Effect;
import com.l2jserver.gameserver.skills.AbnormalEffect;
import com.l2jserver.gameserver.skills.Env;
import com.l2jserver.gameserver.skills.conditions.Condition;
import com.l2jserver.gameserver.skills.funcs.FuncTemplate;
import com.l2jserver.gameserver.skills.funcs.Lambda;
import com.l2jserver.gameserver.templates.skills.L2SkillType;


/**
 * @author mkizub
 * 
 */
public class EffectTemplate
{
	static Logger _log = Logger.getLogger(EffectTemplate.class.getName());
	
	private final Class<?> _func;
	
	private final Constructor<?> _constructor;
	public final Condition attachCond;
	public final Condition applayCond;
	public final Lambda lambda;
	public final int counter;
	public final int abnormalTime; // in seconds
	public final AbnormalEffect abnormalEffect;
	public final AbnormalEffect specialEffect;
	public final AbnormalEffect eventEffect;
	public FuncTemplate[] funcTemplates;
	public final String abnormalType;
	public final byte abnormalLvl;
	public final boolean icon;
	public final String funcName;
	public final double effectPower; // to thandle chance
	public final L2SkillType effectType; // to handle resistences etc...
	
	public final int triggeredId;
	public final int triggeredLevel;
	public final ChanceCondition chanceCondition;
	
	public EffectTemplate(Condition pAttachCond, Condition pApplayCond, String func, Lambda pLambda,
			int pCounter, int pAbnormalTime, AbnormalEffect pAbnormalEffect, AbnormalEffect pSpecialEffect,
			AbnormalEffect pEventEffect, String pAbnormalType, byte pAbnormalLvl, boolean showicon,
			double ePower, L2SkillType eType, int trigId, int trigLvl, ChanceCondition chanceCond)
	{
		attachCond = pAttachCond;
		applayCond = pApplayCond;
		lambda = pLambda;
		counter = pCounter;
		abnormalTime = pAbnormalTime;
		abnormalEffect = pAbnormalEffect;
		specialEffect = pSpecialEffect;
		eventEffect = pEventEffect;
		abnormalType = pAbnormalType;
		abnormalLvl = pAbnormalLvl;
		icon = showicon;
		funcName = func;
		effectPower = ePower;
		effectType = eType;
		
		triggeredId = trigId;
		triggeredLevel = trigLvl;
		chanceCondition = chanceCond;
		
		try
		{
			_func = Class.forName("com.l2jserver.gameserver.skills.effects.Effect" + func);
		}
		catch (ClassNotFoundException e)
		{
			throw new RuntimeException(e);
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
		if (attachCond != null && !attachCond.test(env))
			return null;
		try
		{
			L2Effect effect = (L2Effect) _constructor.newInstance(env, this);
			return effect;
		}
		catch (IllegalAccessException e)
		{
			_log.log(Level.WARNING, "", e);
			return null;
		}
		catch (InstantiationException e)
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
	 * 
	 * @param env
	 * @param stolen
	 * @return
	 */
	public L2Effect getStolenEffect(Env env, L2Effect stolen)
	{
		Class<?> func;
		Constructor<?> stolenCons;
		try
		{
			func = Class.forName("com.l2jserver.gameserver.skills.effects.Effect"
					+ stolen.getEffectTemplate().funcName);
		}
		catch (ClassNotFoundException e)
		{
			throw new RuntimeException(e);
		}
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
			L2Effect effect = (L2Effect) stolenCons.newInstance(env, stolen);
			// if (_applayCond != null)
			// effect.setCondition(_applayCond);
			return effect;
		}
		catch (IllegalAccessException e)
		{
			_log.log(Level.WARNING, "", e);
			return null;
		}
		catch (InstantiationException e)
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
		if (funcTemplates == null)
		{
			funcTemplates = new FuncTemplate[] { f };
		}
		else
		{
			int len = funcTemplates.length;
			FuncTemplate[] tmp = new FuncTemplate[len + 1];
			System.arraycopy(funcTemplates, 0, tmp, 0, len);
			tmp[len] = f;
			funcTemplates = tmp;
		}
	}
}