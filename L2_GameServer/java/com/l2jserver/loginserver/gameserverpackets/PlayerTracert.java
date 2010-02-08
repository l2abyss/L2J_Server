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

import com.l2jserver.loginserver.clientpackets.ClientBasePacket;

/**
 * @author mrTJO
 *
 */
public class PlayerTracert extends ClientBasePacket
{
	private String _account;
	private String _pcIp;
	private String _hop1;
	private String _hop2;
	private String _hop3;
	private String _hop4;

	/**
	 * @param decrypt
	 */
	public PlayerTracert(byte[] decrypt)
	{
		super(decrypt);
		_account = readS();
		_pcIp = readS();
		_hop1 = readS();
		_hop2 = readS();
		_hop3 = readS();
		_hop4 = readS();
	}

	/**
	 * @return Returns the account.
	 */
	public String getAccount()
	{
		return _account;
	}

	/**
	 * @return Returns PC IP.
	 */
	public String getPcIp()
	{
		return _pcIp;
	}
	
	/**
	 * @return Returns 1st Traceroute Hop.
	 */
	public String getFirstHop()
	{
		return _hop1;
	}

	/**
	 * @return Returns 2nd Traceroute Hop.
	 */
	public String getSecondHop()
	{
		return _hop2;
	}
	
	/**
	 * @return Returns 3rd Traceroute Hop.
	 */
	public String getThirdHop()
	{
		return _hop3;
	}
	
	/**
	 * @return Returns 4th Traceroute Hop.
	 */
	public String getFourthHop()
	{
		return _hop4;
	}
}