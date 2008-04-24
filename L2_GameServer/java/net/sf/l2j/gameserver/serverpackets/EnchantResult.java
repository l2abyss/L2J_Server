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

public class EnchantResult extends L2GameServerPacket
{
	private static final String _S__81_ENCHANTRESULT = "[S] 87 EnchantResult";
	private int _unknown;

	public EnchantResult(int unknown)
	{
		_unknown = unknown;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x87);
		writeD(_unknown);
	}

	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.serverpackets.ServerBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__81_ENCHANTRESULT;
	}
}
