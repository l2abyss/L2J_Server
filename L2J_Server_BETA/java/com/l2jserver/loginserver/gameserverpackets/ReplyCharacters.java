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

import com.l2jserver.util.network.BaseRecievePacket;

/**
 * @author mrTJO
 * Thanks to mochitto
 */
public class ReplyCharacters extends BaseRecievePacket
{
	String _account;
	int _chars;
	long[] _charsList;
	/**
	 * @param decrypt
	 */
	public ReplyCharacters(byte[] decrypt)
	{
		super(decrypt);
		_account = readS();
		_chars = readC();
		int charsToDel = readC();
		_charsList = new long[charsToDel];
		for (int i = 0; i < charsToDel; i++)
		{
			_charsList[i] = readQ();
		}
	}
	
	/**
	 * @return Account Name
	 */
	public String getAccountName()
	{
		return _account;
	}
	
	/**
	 * @return Number of Characters on Server
	 */
	public int getCharsOnServer()
	{
		return _chars;
	}
	
	/**
	 * @return Number of Characters on Server Waiting for Delete
	 */
	public int getCharsWaitingDel()
	{
		return _charsList.length;
	}
	
	/**
	 * @return Array with Time to Character Delete
	 */
	public long[] getTimeToDelForChars()
	{
		return _charsList;
	}
}
