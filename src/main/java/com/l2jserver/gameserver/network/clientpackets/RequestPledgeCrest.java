/*
 * Copyright (C) 2004-2015 L2J Server
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
package com.l2jserver.gameserver.network.clientpackets;

import com.l2jserver.Config;
import com.l2jserver.gameserver.data.sql.impl.CrestTable;
import com.l2jserver.gameserver.model.L2Crest;
import com.l2jserver.gameserver.network.serverpackets.PledgeCrest;

/**
 * This class ...
 * @version $Revision: 1.4.4.4 $ $Date: 2005/03/27 15:29:30 $
 */
public final class RequestPledgeCrest extends L2GameClientPacket
{
	private static final String _C__68_REQUESTPLEDGECREST = "[C] 68 RequestPledgeCrest";
	
	private int _crestId;
	
	@Override
	protected void readImpl()
	{
		_crestId = readD();
	}
	
	@Override
	protected void runImpl()
	{
		if (_crestId == 0)
		{
			return;
		}
		
		if (Config.DEBUG)
		{
			_log.fine("crestid " + _crestId + " requested");
		}

		byte[] data = null;
		final L2Crest crest = CrestTable.getInstance().getCrest(_crestId);
		if (crest != null)
		{
			data = CrestTable.getInstance().getCrest(_crestId).getData();
		}

		if (data != null)
		{
			sendPacket(new PledgeCrest(_crestId, data));
		}
		else
		{
			if (Config.DEBUG)
			{
					_log.fine("crest is missing:" + _crestId);
			}
		}
	}
	
	@Override
	public String getType()
	{
		return _C__68_REQUESTPLEDGECREST;
	}
	
	@Override
	protected boolean triggersOnActionRequest()
	{
		return false;
	}
}
