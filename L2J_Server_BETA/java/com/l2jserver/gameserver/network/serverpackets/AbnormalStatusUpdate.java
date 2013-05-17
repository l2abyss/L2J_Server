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
package com.l2jserver.gameserver.network.serverpackets;

import java.util.ArrayList;
import java.util.List;

import com.l2jserver.gameserver.model.holders.EffectDurationHolder;
import com.l2jserver.gameserver.model.skills.L2Skill;

public class AbnormalStatusUpdate extends L2GameServerPacket
{
	private final List<EffectDurationHolder> _effects = new ArrayList<>();
	
	public void addEffect(L2Skill skill, int duration)
	{
		switch (skill.getId())
		{
			case 2031:
			case 2032:
			case 2037:
			case 26025:
			case 26026:
				return;
		}
		_effects.add(new EffectDurationHolder(skill, duration));
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x85);
		writeH(_effects.size());
		for (EffectDurationHolder edh : _effects)
		{
			writeD(edh.getSkillId());
			writeH(edh.getSkillLvl());
			writeD(edh.getDuration());
		}
	}
}
