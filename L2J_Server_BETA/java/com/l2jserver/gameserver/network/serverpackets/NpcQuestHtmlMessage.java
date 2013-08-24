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

import com.l2jserver.gameserver.enums.BypassScope;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author Unknown, FBIagent
 */
public final class NpcQuestHtmlMessage extends AbstractHtmlPacket
{
	private final int _npcObjId;
	private final int _questId;
	
	public NpcQuestHtmlMessage(int npcObjId, int questId)
	{
		_npcObjId = npcObjId;
		_questId = questId;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0xFE);
		writeH(0x8D);
		writeD(_npcObjId);
		writeS(getHtml());
		writeD(_questId);
	}
	
	@Override
	public void clearHtmlActionCache()
	{
		L2PcInstance player = getClient().getActiveChar();
		if (_npcObjId > 0)
		{
			player.setHtmlActionOriginObjectId(BypassScope.NPC_QUEST_HTML, _npcObjId);
		}
		else
		{
			player.setHtmlActionOriginObjectId(BypassScope.NPC_QUEST_HTML, 0);
		}
		player.clearHtmlActions(BypassScope.NPC_QUEST_HTML);
	}
	
	@Override
	public void addHtmlAction(String action)
	{
		getClient().getActiveChar().addHtmlAction(BypassScope.NPC_QUEST_HTML, action);
	}
}
