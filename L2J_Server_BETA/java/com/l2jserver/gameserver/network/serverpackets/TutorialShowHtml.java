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
public final class TutorialShowHtml extends AbstractHtmlPacket
{
	private final int _npcObjectId;
	
	public TutorialShowHtml(String html)
	{
		super(html);
		_npcObjectId = 0;
	}
	
	/**
	 * This constructor is just here to be able to show a tutorial html<br>
	 * window bound to an npc.
	 * @param html
	 * @param npcObjectId
	 */
	public TutorialShowHtml(String html, int npcObjectId)
	{
		super(html);
		_npcObjectId = npcObjectId;
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0xA6);
		writeS(getHtml());
	}
	
	@Override
	public void clearHtmlActionCache()
	{
		L2PcInstance player = getClient().getActiveChar();
		if (_npcObjectId > 0)
		{
			player.setHtmlActionOriginObjectId(BypassScope.TUTORIAL_HTML, _npcObjectId);
		}
		else
		{
			player.setHtmlActionOriginObjectId(BypassScope.TUTORIAL_HTML, 0);
		}
		player.clearHtmlActions(BypassScope.TUTORIAL_HTML);
	}
	
	@Override
	public void addHtmlAction(String action)
	{
		getClient().getActiveChar().addHtmlAction(BypassScope.TUTORIAL_HTML, action);
	}
}