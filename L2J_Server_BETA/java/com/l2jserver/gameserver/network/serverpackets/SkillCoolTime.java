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

import com.l2jserver.gameserver.model.TimeStamp;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author KenM, Zoey76
 */
public class SkillCoolTime extends L2GameServerPacket
{
	private final List<TimeStamp> _skillReuseTimeStamps = new ArrayList<>();
	
	public SkillCoolTime(L2PcInstance cha)
	{
		for (TimeStamp ts : cha.getSkillReuseTimeStamps().values())
		{
			if (ts.hasNotPassed())
			{
				_skillReuseTimeStamps.add(ts);
			}
		}
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0xC7);
		writeD(_skillReuseTimeStamps.size()); // list size
		for (TimeStamp ts : _skillReuseTimeStamps)
		{
			writeD(ts.getSkillId());
			writeD(0x00);
			writeD((int) ts.getReuse() / 1000);
			writeD((int) ts.getRemaining() / 1000);
		}
	}
}
