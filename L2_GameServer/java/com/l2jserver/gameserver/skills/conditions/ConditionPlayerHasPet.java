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

import com.l2jserver.gameserver.model.L2ItemInstance;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.actor.instance.L2PetInstance;
import com.l2jserver.gameserver.skills.Env;

public class ConditionPlayerHasPet extends Condition
{
	private final ArrayList<Integer> _controlItemIds;

	public ConditionPlayerHasPet(ArrayList<Integer> itemIds)
	{
		_controlItemIds = itemIds;
	}

	@Override
	public boolean testImpl(Env env)
	{
		if (!(env.player instanceof L2PcInstance))
			return false;

		if (!(env.player.getPet() instanceof L2PetInstance))
			return false;

		final L2ItemInstance controlItem = ((L2PetInstance)env.player.getPet()).getControlItem();
		if (controlItem == null)
			return false;

		return _controlItemIds.contains(controlItem.getItemId());
	}
}