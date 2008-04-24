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

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 *
 * @author  Luca Baldi
 */
public final class RelationChanged extends L2GameServerPacket
{
	public static final int RELATION_PVP_FLAG     = 0x00002; // pvp ???
	public static final int RELATION_HAS_KARMA    = 0x00004; // karma ???
	public static final int RELATION_LEADER 	  = 0x00080; // leader
	public static final int RELATION_INSIEGE   	  = 0x00200; // true if in siege
	public static final int RELATION_ATTACKER     = 0x00400; // true when attacker
	public static final int RELATION_ALLY         = 0x00800; // blue siege icon, cannot have if red
	public static final int RELATION_ENEMY        = 0x01000; // true when red icon, doesn't matter with blue
	public static final int RELATION_MUTUAL_WAR   = 0x08000; // double fist
	public static final int RELATION_1SIDED_WAR   = 0x10000; // single fist

	private static final String _S__CE_RELATIONCHANGED = "[S] ce RelationChanged";

	private int _objId, _relation, _autoAttackable, _karma, _pvpFlag;

	public RelationChanged(L2PcInstance cha, int relation, boolean autoattackable)
	{
		_objId = cha.getObjectId();
		_relation = relation;
		_autoAttackable = autoattackable ? 1 : 0;
		_karma = cha.getKarma();
		_pvpFlag = cha.getPvpFlag();
	}

	/**
	 * @see net.sf.l2j.gameserver.serverpackets.ServerBasePacket#writeImpl()
	 */
	@Override
	protected final void writeImpl()
	{
		writeC(0xce);
		writeD(_objId);
		writeD(_relation);
		writeD(_autoAttackable);
		writeD(_karma);
		writeD(_pvpFlag);
	}

	/**
	 * @see net.sf.l2j.gameserver.BasePacket#getType()
	 */
	@Override
	public String getType()
	{
		// TODO Auto-generated method stub
		return _S__CE_RELATIONCHANGED;
	}

}
