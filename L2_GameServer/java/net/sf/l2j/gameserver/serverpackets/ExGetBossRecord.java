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
package net.sf.l2j.gameserver.serverpackets;

/**
 * Format: ch ddd [ddd]
 * @author  KenM
 */
public class ExGetBossRecord extends L2GameServerPacket
{
	private static final String _S__FE_33_EXGETBOSSRECORD = "[S] FE:34 ExGetBossRecord";
	private int _unk1, _unk2;

	public ExGetBossRecord(int val1, int val2)
	{
		_unk1 = val1;
		_unk2 = val2;
	}

	/**
	 * @see net.sf.l2j.gameserver.serverpackets.ServerBasePacket#writeImpl()
	 */
	@Override
	protected void writeImpl()
	{
		writeC(0xfe);
		writeH(0x34);
		writeD(_unk1);
		writeD(_unk2);
		writeD(0x00); //list size
	}

	/**
	 * @see net.sf.l2j.gameserver.BasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__FE_33_EXGETBOSSRECORD;
	}

}
