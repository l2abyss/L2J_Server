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
package com.l2jserver.gameserver.model.conditions;

import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.base.PlayerState;
import com.l2jserver.gameserver.model.stats.Env;

/**
 * The Class ConditionPlayerState.
 * @author mkizub
 */
public class ConditionPlayerState extends Condition
{
	private final PlayerState _check;
	private final boolean _required;
	
	/**
	 * Instantiates a new condition player state.
	 * @param check the player state to be verified.
	 * @param required the required value.
	 */
	public ConditionPlayerState(PlayerState check, boolean required)
	{
		_check = check;
		_required = required;
	}
	
	/**
	 * 
	 */
	@Override
	public boolean testImpl(Env env)
	{
		final L2Character character = env.getCharacter();
		final L2PcInstance player = env.getPlayer();
		switch (_check)
		{
			case RESTING:
				if (player != null)
				{
					return (player.isSitting() == _required);
				}
				return !_required;
			case MOVING:
				return character.isMoving() == _required;
			case RUNNING:
				return character.isRunning() == _required;
			case STANDING:
				if (player != null)
				{
					return (_required != (player.isSitting() || player.isMoving()));
				}
				return (_required != character.isMoving());
			case FLYING:
				return (character.isFlying() == _required);
			case BEHIND:
				return (character.isBehindTarget() == _required);
			case FRONT:
				return (character.isInFrontOfTarget() == _required);
			case CHAOTIC:
				if (player != null)
				{
					return ((player.getKarma() > 0) == _required);
				}
				return !_required;
			case OLYMPIAD:
				if (player != null)
				{
					return (player.isInOlympiadMode() == _required);
				}
				return !_required;
		}
		return !_required;
	}
}
