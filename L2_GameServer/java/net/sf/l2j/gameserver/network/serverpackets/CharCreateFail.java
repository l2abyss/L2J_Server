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
package net.sf.l2j.gameserver.network.serverpackets;



/**
 * This class ...
 *
 * @version $Revision: 1.4.2.1.2.3 $ $Date: 2005/03/27 15:29:39 $
 */
public class CharCreateFail extends L2GameServerPacket
{
	private static final String _S__26_CHARCREATEFAIL = "[S] 10 CharCreateFail";

	public static final int REASON_CREATION_FAILED = 0x00;
	public static final int REASON_TOO_MANY_CHARACTERS = 0x01;
	public static final int REASON_NAME_ALREADY_EXISTS = 0x02;
	public static final int REASON_16_ENG_CHARS = 0x03;

	private int _error;

	public CharCreateFail(int errorCode)
	{
		_error = errorCode;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x10);
		writeD(_error);
	}

	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.serverpackets.ServerBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__26_CHARCREATEFAIL;
	}

}
