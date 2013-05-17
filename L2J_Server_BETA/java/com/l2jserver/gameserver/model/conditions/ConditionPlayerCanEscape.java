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
package com.l2jserver.gameserver.model.conditions;

import com.l2jserver.gameserver.instancemanager.GrandBossManager;
import com.l2jserver.gameserver.model.PcCondOverride;
import com.l2jserver.gameserver.model.entity.TvTEvent;
import com.l2jserver.gameserver.model.stats.Env;

/**
 * Player Can Escape condition implementation.
 * @author Adry_85
 */
public class ConditionPlayerCanEscape extends Condition
{
	private final boolean _val;
	
	public ConditionPlayerCanEscape(boolean val)
	{
		_val = val;
	}
	
	@Override
	public boolean testImpl(Env env)
	{
		boolean canTeleport = true;
		if (env.getPlayer() == null)
		{
			canTeleport = false;
		}
		
		if (!TvTEvent.onEscapeUse(env.getPlayer().getActingPlayer().getObjectId()))
		{
			canTeleport = false;
		}
		
		if (env.getPlayer().getActingPlayer().isInDuel())
		{
			canTeleport = false;
		}
		
		if (env.getPlayer().isAfraid())
		{
			canTeleport = false;
		}
		
		if (env.getPlayer().getActingPlayer().isCombatFlagEquipped())
		{
			canTeleport = false;
		}
		
		if (env.getPlayer().isFlying() || env.getPlayer().getActingPlayer().isFlyingMounted())
		{
			canTeleport = false;
		}
		
		if (env.getPlayer().getActingPlayer().isInOlympiadMode())
		{
			canTeleport = false;
		}
		
		if ((GrandBossManager.getInstance().getZone(env.getPlayer()) != null) && !env.getPlayer().canOverrideCond(PcCondOverride.SKILL_CONDITIONS))
		{
			canTeleport = false;
		}
		return (_val == canTeleport);
	}
}