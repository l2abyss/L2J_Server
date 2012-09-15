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
package com.l2jserver.gameserver.network.serverpackets;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.mmocore.network.SendablePacket;

import com.l2jserver.Config;
import com.l2jserver.gameserver.network.L2GameClient;

/**
 * @author KenM
 */
public abstract class L2GameServerPacket extends SendablePacket<L2GameClient>
{
	protected final Logger _log = Logger.getLogger(getClass().getName());
	
	protected boolean _invisible = false;
	
	/**
	 * @return True if packet originated from invisible character.
	 */
	public boolean isInvisible()
	{
		return _invisible;
	}
	
	/**
	 * Set "invisible" boolean flag in the packet.<br>
	 * Packets from invisible characters will not be broadcasted to players.
	 * @param b
	 */
	public void setInvisible(boolean b)
	{
		_invisible = b;
	}
	
	@Override
	protected void write()
	{
		try
		{
			writeImpl();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Client: " + getClient().toString() + " - Failed writing: " + getClass().getSimpleName() + " - L2J Server Version: " + Config.SERVER_VERSION + " - DP Revision: " + Config.DATAPACK_VERSION + " ; " + e.getMessage(), e);
		}
	}
	
	public void runImpl()
	{
		
	}
	
	protected abstract void writeImpl();
}
