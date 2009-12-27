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
package com.l2jserver.gameserver.network.clientpackets;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import com.l2jserver.Config;
import com.l2jserver.gameserver.network.serverpackets.KeyPacket;

/**
 * This class ...
 *
 * @version $Revision: 1.5.2.8.2.8 $ $Date: 2005/04/02 10:43:04 $
 */
public final class ProtocolVersion extends L2GameClientPacket
{
	private static final String _C__00_PROTOCOLVERSION = "[C] 00 ProtocolVersion";
	private static final Logger _log = Logger.getLogger(ProtocolVersion.class.getName());
	private static final Logger _logAccounting = Logger.getLogger("accounting");

	private int _version;

	@Override
	protected void readImpl()
	{
		_version  = readD();
	}

	@Override
	protected void runImpl()
	{
		// this packet is never encrypted
		if (_version == -2)
		{
			if (Config.DEBUG)
				_log.info("Ping received");
			// this is just a ping attempt from the new C2 client
			getClient().closeNow();
		}
		else if (_version < Config.MIN_PROTOCOL_REVISION
				|| _version > Config.MAX_PROTOCOL_REVISION)
		{
			LogRecord record = new LogRecord(Level.WARNING, "Wrong protocol");
			record.setParameters(new Object[]{_version, getClient()});
			_logAccounting.log(record);
			KeyPacket pk = new KeyPacket(getClient().enableCrypt(),0);
			getClient().sendPacket(pk);
			getClient().setProtocolOk(false);
			_log.info("Client: "+getClient().toString()+" -> Protocol Revision: " + _version + " is invalid. Minimum is "+Config.MIN_PROTOCOL_REVISION+" and Maximum is "+Config.MAX_PROTOCOL_REVISION+" are supported. Closing connection.");
		}
		else
		{
			if (Config.DEBUG)
				_log.fine("Client Protocol Revision is ok: "+_version);

			KeyPacket pk = new KeyPacket(getClient().enableCrypt(),1);
			getClient().sendPacket(pk);
			getClient().setProtocolOk(true);
		}
	}

	/* (non-Javadoc)
	 * @see com.l2jserver.gameserver.clientpackets.ClientBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _C__00_PROTOCOLVERSION;
	}
}
