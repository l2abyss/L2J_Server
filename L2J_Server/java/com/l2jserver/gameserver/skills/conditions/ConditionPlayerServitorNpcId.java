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

import java.util.ArrayList;

import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.skills.Env;

/**
 * The Class ConditionPlayerServitorNpcId.
 */
public class ConditionPlayerServitorNpcId extends Condition
{
	private final ArrayList<Integer> _npcIds;
	
	/**
	 * Instantiates a new condition player servitor npc id.
	 *
	 * @param npcIds the npc ids
	 */
	public ConditionPlayerServitorNpcId(ArrayList<Integer> npcIds)
	{
		if (npcIds.size() == 1 && npcIds.get(0) == 0)
			_npcIds = null;
		else
			_npcIds = npcIds;
	}
	
	/* (non-Javadoc)
	 * @see com.l2jserver.gameserver.skills.conditions.Condition#testImpl(com.l2jserver.gameserver.skills.Env)
	 */
	@Override
	public boolean testImpl(Env env)
	{
		if (!(env.player instanceof L2PcInstance))
			return false;
		
		if ((env.player.getPet() == null))
			return false;
		
		return _npcIds == null || _npcIds.contains(env.player.getPet().getNpcId());
	}
}