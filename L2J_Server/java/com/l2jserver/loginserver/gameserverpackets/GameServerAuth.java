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
package com.l2jserver.loginserver.gameserverpackets;

import java.util.logging.Logger;

import com.l2jserver.util.network.BaseRecievePacket;


/**
 * Format: cccddb
 * c desired ID
 * c accept alternative ID
 * c reserve Host
 * s ExternalHostName
 * s InetranlHostName
 * d max players
 * d hexid size
 * b hexid
 * @author -Wooden-
 *
 */
public class GameServerAuth extends BaseRecievePacket
{
	protected static Logger _log = Logger.getLogger(GameServerAuth.class.getName());
	private byte[] _hexId;
	private int _desiredId;
	private boolean _hostReserved;
	private boolean _acceptAlternativeId;
	private int _maxPlayers;
	private int _port;
	private String[] _hosts;
	
	/**
	 * @param decrypt
	 */
	public GameServerAuth(byte[] decrypt)
	{
		super(decrypt);
		_desiredId = readC();
		_acceptAlternativeId = (readC() == 0 ? false : true);
		_hostReserved = (readC() == 0 ? false : true);
		_port = readH();
		_maxPlayers = readD();
		int size = readD();
		_hexId = readB(size);
		size = 2 * readD();
		_hosts = new String[size];
		for (int i = 0; i < size; i++)
			_hosts[i] = readS();
	}
	
	/**
	 * @return
	 */
	public byte[] getHexID()
	{
		return _hexId;
	}
	
	public boolean getHostReserved()
	{
		return _hostReserved;
	}
	
	public int getDesiredID()
	{
		return _desiredId;
	}
	
	public boolean acceptAlternateID()
	{
		return _acceptAlternativeId;
	}
	
	/**
	 * @return Returns the max players.
	 */
	public int getMaxPlayers()
	{
		return _maxPlayers;
	}
	
	public String[] getHosts()
	{
		return _hosts;
	}
	
	/**
	 * @return Returns the port.
	 */
	public int getPort()
	{
		return _port;
	}
}
