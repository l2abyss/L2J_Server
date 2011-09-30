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

import com.l2jserver.gameserver.model.actor.L2Character;

/**
 * This class ...
 *
 * @version $Revision: 1.3.2.1.2.3 $ $Date: 2005/03/27 15:29:39 $
 */
public class SocialAction extends L2GameServerPacket
{
	public static final int LEVEL_UP = 2122;
	
	private static final String _S__3D_SOCIALACTION = "[S] 27 SocialAction";
	private final int _charObjId;
	private final int _actionId;
	
	/**
	 * 0x3d SocialAction         dd
	 * @param cha
	 * @param actionId
	 */
	public SocialAction(L2Character cha, int actionId)
	{
		_charObjId = cha.getObjectId();
		_actionId = actionId;
	}
	
	public SocialAction(int objectId, int actionId)
	{
		_charObjId = objectId;
		_actionId = actionId;
	}

	
	@Override
	protected final void writeImpl()
	{
		writeC(0x27);
		writeD(_charObjId);
		writeD(_actionId);
	}
	
	/**
	 * @see com.l2jserver.gameserver.network.serverpackets.L2GameServerPacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__3D_SOCIALACTION;
	}
}
