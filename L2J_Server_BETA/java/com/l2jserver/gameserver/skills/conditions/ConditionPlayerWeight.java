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
package com.l2jserver.gameserver.skills.conditions;

import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.skills.Env;
import com.l2jserver.gameserver.skills.Stats;



/**
 * The Class ConditionPlayerWeight.
 *
 * @author Kerberos
 */
public class ConditionPlayerWeight extends Condition {
	
	private final int _weight;
	
	/**
	 * Instantiates a new condition player weight.
	 *
	 * @param weight the weight
	 */
	public ConditionPlayerWeight(int weight)
	{
		_weight = weight;
	}
	
	/* (non-Javadoc)
	 * @see com.l2jserver.gameserver.skills.conditions.Condition#testImpl(com.l2jserver.gameserver.skills.Env)
	 */
	@Override
	public boolean testImpl(Env env)
	{
		if (env.player instanceof L2PcInstance)
		{
			if (((L2PcInstance) env.player).getMaxLoad() > 0)
			{
				int weightproc = ((L2PcInstance) env.player).getCurrentLoad() * 100 / ((L2PcInstance) env.player).getMaxLoad();
				weightproc*=(int)((L2PcInstance) env.player).calcStat(Stats.WEIGHT_LIMIT, 1, env.player, null);
				return weightproc < _weight || ((L2PcInstance) env.player).getDietMode();
			}
		}
		return true;
	}
}
