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

import com.l2jserver.gameserver.model.L2Object;
import com.l2jserver.gameserver.model.L2Skill;
import com.l2jserver.gameserver.model.actor.L2Attackable;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;
import com.l2jserver.gameserver.skills.Env;

/**
 * Checks Sweeper conditions:
 * <ul>
 * 	<li>Minimum checks, player not null, skill not null.</li>
 * 	<li>Checks if the target isn't null, is dead and spoiled.</li>
 * 	<li>Checks if the sweeper player is the target spoiler, or is in the spoiler party.</li>
 * 	<li>Checks if the corpse is too old.</li>
 * 	<li>Checks inventory limit and weight max load won't be exceed after sweep.</li>
 * </ul>
 * If two or more conditions aren't meet at the same time, one message per condition will be shown.
 * @author Zoey76
 */
public class ConditionPlayerCanSweep extends Condition
{
	private final boolean _val;
	private static final int maxSweepTime = 15000;
	public ConditionPlayerCanSweep(boolean val)
	{
		_val = val;
	}
	
	@Override
	public boolean testImpl(Env env)
	{
		boolean canSweep = (env.player != null) && (env.player instanceof L2PcInstance);
		if (canSweep)
		{
			final L2PcInstance sweeper = env.player.getActingPlayer();
			final L2Skill sweep = env.skill;
			canSweep &= (sweep != null);
			if (canSweep)
			{
				final L2Object[] targets = sweep.getTargetList(sweeper);
				canSweep &= (targets != null);
				if (canSweep)
				{
					L2Attackable target;
					for (L2Object objTarget : targets)
					{
						canSweep &= (objTarget != null) && (objTarget instanceof L2Attackable);
						if (canSweep)
						{
							target = (L2Attackable) objTarget;
							canSweep &= target.isDead();
							if (canSweep)
							{
								canSweep &= target.isSpoil();
								if (canSweep)
								{
									canSweep &= target.checkSpoilOwner(sweeper, true);
									canSweep &= target.checkCorpseTime(sweeper, maxSweepTime, true);
									canSweep &= sweeper.getInventory().checkInventorySlotsAndWeight(target.getSpoilLootItems(), true, true);
								}
								else
								{
									sweeper.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.SWEEPER_FAILED_TARGET_NOT_SPOILED));
								}
							}
						}
					}
				}
			}
		}
		return (_val == canSweep);
	}
}
