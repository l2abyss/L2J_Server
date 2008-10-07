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
package net.sf.l2j.gameserver.skills.effects;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Logger;

import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.conditions.Condition;
import net.sf.l2j.gameserver.skills.funcs.FuncTemplate;
import net.sf.l2j.gameserver.skills.funcs.Lambda;

/**
 * @author mkizub
 * 
 */
public final class EffectTemplate
{
	static Logger _log = Logger.getLogger(EffectTemplate.class.getName());
	
	private final Class<?> _func;
	
	private final Constructor<?> _constructor;
	public final Condition attachCond;
	public final Condition applayCond;
	public final Lambda lambda;
	public final int counter;
	public final int period; // in seconds
	public final int abnormalEffect;
	public FuncTemplate[] funcTemplates;
	public final String stackType;
	public final float stackOrder;
	public final boolean icon;
	public final String funcName;
	public EffectTemplate(Condition pAttachCond, Condition pApplayCond, String func, Lambda pLambda, int pCounter, int pPeriod, int pAbnormalEffect, String pStackType, float pStackOrder, boolean showicon)
	{
		attachCond = pAttachCond;
		applayCond = pApplayCond;
		lambda = pLambda;
		counter = pCounter;
		period = pPeriod;
		abnormalEffect = pAbnormalEffect;
		stackType = pStackType;
		stackOrder = pStackOrder;
		icon = showicon;
		funcName = func;
		try
		{
			_func = Class.forName("net.sf.l2j.gameserver.skills.effects.Effect" + func);
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
			e.printStackTrace();
			return null;
		}
		catch (InstantiationException e)
		{
			e.printStackTrace();
			return null;
		}
		catch (InvocationTargetException e)
		{
			_log.warning("Error creating new instance of Class " + _func + " Exception was:");
			e.getTargetException().printStackTrace();
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
				func = Class.forName("net.sf.l2j.gameserver.skills.effects.Effect"+stolen.getEffectTemplate().funcName);
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
				L2Effect effect = (L2Effect)stolenCons.newInstance(env, stolen);
				//if (_applayCond != null)
				//	effect.setCondition(_applayCond);
				return effect;
			}
			catch (IllegalAccessException e)
			{
				e.printStackTrace();
				return null;
			}
			catch (InstantiationException e)
			{
				e.printStackTrace();
				return null;
			}
			catch (InvocationTargetException e)
			{
				_log.warning("Error creating new instance of Class "+func+" Exception was:");
				e.getTargetException().printStackTrace();
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