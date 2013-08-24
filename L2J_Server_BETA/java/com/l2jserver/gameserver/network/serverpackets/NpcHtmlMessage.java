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
public final class NpcHtmlMessage extends AbstractHtmlPacket
{
	private final int _npcObjId;
	private final int _itemId;
	
	public NpcHtmlMessage()
	{
		_npcObjId = 0;
		_itemId = 0;
	}
	
	public NpcHtmlMessage(int npcObjId)
	{
		if (npcObjId < 0)
		{
			throw new IllegalArgumentException();
		}
		
		_npcObjId = npcObjId;
		_itemId = 0;
	}
	
	public NpcHtmlMessage(String html)
	{
		super(html);
		_npcObjId = 0;
		_itemId = 0;
	}
	
	public NpcHtmlMessage(int npcObjId, String html)
	{
		super(html);
		
		if (npcObjId < 0)
		{
			throw new IllegalArgumentException();
		}
		
		_npcObjId = npcObjId;
		_itemId = 0;
	}
	
	public NpcHtmlMessage(int npcObjId, int itemId)
	{
		if ((npcObjId < 0) || (itemId < 0))
		{
			throw new IllegalArgumentException();
		}
		
		_npcObjId = npcObjId;
		_itemId = itemId;
	}
	
	public NpcHtmlMessage(int npcObjId, int itemId, String html)
	{
		super(html);
		
		if ((npcObjId < 0) || (itemId < 0))
		{
			throw new IllegalArgumentException();
		}
		
		_npcObjId = npcObjId;
		_itemId = itemId;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x19);
		
		writeD(_npcObjId);
		writeS(getHtml());
		writeD(_itemId);
	}
	
	@Override
	public void clearHtmlActionCache()
	{
		L2PcInstance player = getClient().getActiveChar();
		
		player.setHtmlActionOriginObjectId(BypassScope.NPC_ITEM_HTML, _npcObjId);
		
		if (_itemId > 0)
		{
			player.clearHtmlActions(BypassScope.NPC_ITEM_HTML);
		}
		else
		{
			player.clearHtmlActions(BypassScope.NPC_HTML);
		}
	}
	
	@Override
	public void addHtmlAction(String action)
	{
		if (_itemId > 0)
		{
			getClient().getActiveChar().addHtmlAction(BypassScope.NPC_ITEM_HTML, action);
		}
		else
		{
			getClient().getActiveChar().addHtmlAction(BypassScope.NPC_HTML, action);
		}
	}
}
