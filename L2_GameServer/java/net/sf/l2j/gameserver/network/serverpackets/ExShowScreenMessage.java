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
 * @author Kerberos
 *
 */
public class ExShowScreenMessage extends L2GameServerPacket
{
	private String _text;
	private int _time;

	public ExShowScreenMessage (String text, int time)
	{
		_text = text;
		_time = time;
	}

	@Override
	public String getType()
	{
		return "ExShowScreenMessage";
	}

	@Override
	protected void writeImpl()
	{
		writeC(0xfe);
		writeH(0x39);

		writeD(0x01);
		writeD(-1);
		writeD(0x02);
		writeD(0x00);
		writeD(0x00);
		writeD(0x00);

		writeD(0);
		writeD(0);

		writeD(_time);

		writeD(1);

		writeS(_text);
	}

}