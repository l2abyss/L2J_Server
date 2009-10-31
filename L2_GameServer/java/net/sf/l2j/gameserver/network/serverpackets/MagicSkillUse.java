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

import net.sf.l2j.gameserver.model.actor.L2Character;

/**
 *
 * sample
 *
 * 0000: 5a  d8 a8 10 48  d8 a8 10 48  10 04 00 00  01 00 00    Z...H...H.......
 * 0010: 00  f0 1a 00 00  68 28 00 00                         .....h(..
 *
 * format   dddddd dddh (h)
 *
 * @version $Revision: 1.4.2.1.2.4 $ $Date: 2005/03/27 15:29:39 $
 */
public final class MagicSkillUse extends L2GameServerPacket
{
	private static final String _S__5A_MAGICSKILLUSER = "[S] 48 MagicSkillUser";
	private int _targetId, _tx, _ty, _tz;
	private int _skillId;
	private int _skillLevel;
	private int _hitTime;
	private int _reuseDelay;
	private int _charObjId, _x, _y, _z;
	private int _flags;

	public MagicSkillUse(L2Character cha, L2Character target, int skillId, int skillLevel, int hitTime, int reuseDelay)
	{
		_charObjId = cha.getObjectId();
		_targetId = target.getObjectId();
		_skillId = skillId;
		_skillLevel = skillLevel;
		_hitTime = hitTime;
		_reuseDelay = reuseDelay;
		_x = cha.getX();
		_y = cha.getY();
		_z = cha.getZ();
		_tx = target.getX();
		_ty = target.getY();
		_tz = target.getZ();
		_flags |= 0x20;
	}

	public MagicSkillUse(L2Character cha, int skillId, int skillLevel, int hitTime, int reuseDelay)
	{
		_charObjId = cha.getObjectId();
		_targetId = cha.getTargetId();
		_skillId = skillId;
		_skillLevel = skillLevel;
		_hitTime = hitTime;
		_reuseDelay = reuseDelay;
		_x = cha.getX();
		_y = cha.getY();
		_z = cha.getZ();
		_tx = cha.getX();
		_ty = cha.getY();
		_tz = cha.getZ();
		_flags |= 0x20;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x48);
		writeD(_charObjId);
		writeD(_targetId);
		writeD(_skillId);
		writeD(_skillLevel);
		writeD(_hitTime);
		writeD(_reuseDelay);
		writeD(_x);
		writeD(_y);
		writeD(_z);
		writeD(0x00); // unknown
		writeD(_tx);
		writeD(_ty);
		writeD(_tz);
	}

	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.serverpackets.ServerBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__5A_MAGICSKILLUSER;
	}
}
