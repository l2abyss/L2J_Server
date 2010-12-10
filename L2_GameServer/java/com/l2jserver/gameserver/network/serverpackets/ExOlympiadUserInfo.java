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
import com.l2jserver.gameserver.model.olympiad.Participant;


/**
 * This class ...
 *
 * @version $Revision: 1.4.2.1.2.3 $ $Date: 2005/03/27 15:29:57 $
 *
 * @author godson
 */
public class ExOlympiadUserInfo extends L2GameServerPacket
{
	// chcdSddddd
	private static final String _S__FE_29_OLYMPIADUSERINFO = "[S] FE:7A ExOlympiadUserInfo";
	private L2PcInstance _player;
	private Participant _par = null;
	
	public ExOlympiadUserInfo(L2PcInstance player)
	{
		_player = player;
	}
	
	public ExOlympiadUserInfo(Participant par)
	{
		_par = par;
		_player = par.player;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0xfe);
		writeH(0x7a);
		if (_player != null)
		{
			writeC(_player.getOlympiadSide());
			writeD(_player.getObjectId());
			writeS(_player.getName());
			writeD(_player.getClassId().getId());
			writeD((int)_player.getCurrentHp());
			writeD(_player.getMaxVisibleHp());
			writeD((int)_player.getCurrentCp());
			writeD(_player.getMaxCp());
		}
		else
		{
			writeC(_par.side);
			writeD(_par.objectId);
			writeS(_par.name);
			writeD(_par.baseClass);
			writeD(0);
			writeD(100);
			writeD(0);
			writeD(100);
		}
	}
	
	/* (non-Javadoc)
	 * @see com.l2jserver.gameserver.serverpackets.ServerBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__FE_29_OLYMPIADUSERINFO;
	}
}