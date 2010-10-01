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

import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;

/**
 *
 * @author  KenM
 */
public final class ExBasicActionList extends L2GameServerPacket
{
	private static final String _S__FE_5E_EXBASICACTIONLIST = "[S] FE:5F ExBasicActionList";
	
	private static final int[] _actionsOnTransform = { 2, 3, 4, 5, 6, 7, 8, 9, 11, 40, 50, 55, 56, 63, 64, 65 };
	private static final int[] _defaultActionList;
	
	static
	{
		int count1 = 74; // 0 <-> (count1 - 1)
		int count2 = 99; // 1000 <-> (1000 + count2 - 1) //Update by rocknow
		int count3 = 16; // 5000 <-> (5000 + count3 - 1) //Update by rocknow
		_defaultActionList = new int[count1 + count2 + count3];
		int i;
		
		for (i = count1; i-- > 0;)
			_defaultActionList[i] = i;
		
		for (i = count2; i-- > 0;)
			_defaultActionList[count1 + i] = 1000 + i;
		
		for (i = count3; i-- > 0;)
			_defaultActionList[count1 + count2 + i] = 5000 + i;
	}
	
	private final static ExBasicActionList STATIC_PACKET_TRANSFORMED = new ExBasicActionList(_actionsOnTransform);
	private final static ExBasicActionList STATIC_PACKET = new ExBasicActionList(_defaultActionList);
	
	public final static ExBasicActionList getStaticPacket(final L2PcInstance player)
	{
		return player.isTransformed() ? STATIC_PACKET_TRANSFORMED : STATIC_PACKET;
	}
	
	private final int[] _actionIds;
	
	private ExBasicActionList(final int[] actionIds)
	{
		_actionIds = actionIds;
	}
	
	/**
	 * @see com.l2jserver.gameserver.network.serverpackets.L2GameServerPacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__FE_5E_EXBASICACTIONLIST;
	}
	
	/**
	 * @see com.l2jserver.gameserver.network.serverpackets.L2GameServerPacket#writeImpl()
	 */
	@Override
	protected void writeImpl()
	{
		writeC(0xfe);
		writeH(0x5f);
		writeD(_actionIds.length);
		for (int i = 0; i < _actionIds.length; i++)
		{
			writeD(_actionIds[i]);
		}
	}
}
