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
package com.l2jserver.gameserver.network.clientpackets;

import com.l2jserver.gameserver.model.L2Object;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.ExBrExtraUserInfo;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;
import com.l2jserver.gameserver.network.serverpackets.UserInfo;

public final class RequestEvaluate extends L2GameClientPacket
{
	private static final String _C__B9_REQUESTEVALUATE = "[C] B9 RequestEvaluate";
	
	private int _targetId;
	
	@Override
	protected void readImpl()
	{
		_targetId = readD();
	}
	
	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;
		
		L2Object object = activeChar.getTarget();
		
		if (!(object instanceof L2PcInstance))
		{
			if (object == null)
				activeChar.sendPacket(new SystemMessage(SystemMessageId.SELECT_TARGET));
			else
				activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
			return;
		}
		
		L2PcInstance target = (L2PcInstance) object;
		
		if (target.getObjectId() != _targetId)
			return;
		
		if (activeChar.getLevel() < 10)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.ONLY_LEVEL_SUP_10_CAN_RECOMMEND));
			return;
		}
		
		if (target == activeChar)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_CANNOT_RECOMMEND_YOURSELF));
			return;
		}
		
		if (activeChar.getRecomLeft() <= 0)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.NO_MORE_RECOMMENDATIONS_TO_HAVE));
			return;
		}
		
		if (target.getRecomHave() >= 255)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.YOUR_TARGET_NO_LONGER_RECEIVE_A_RECOMMENDATION));
			return;
		}
		
		if (!activeChar.canRecom(target))
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.THAT_CHARACTER_IS_RECOMMENDED));
			return;
		}
		
		activeChar.giveRecom(target);
		
		SystemMessage sm = null;
		sm = new SystemMessage(SystemMessageId.YOU_HAVE_RECOMMENDED_C1_YOU_HAVE_S2_RECOMMENDATIONS_LEFT);
		sm.addPcName(target);
		sm.addNumber(activeChar.getRecomLeft());
		activeChar.sendPacket(sm);
		
		sm = new SystemMessage(SystemMessageId.YOU_HAVE_BEEN_RECOMMENDED_BY_C1);
		sm.addPcName(activeChar);
		target.sendPacket(sm);
		sm = null;
		
		activeChar.sendPacket(new UserInfo(activeChar));
		sendPacket(new ExBrExtraUserInfo(activeChar));
		target.broadcastUserInfo();
	}
	
	/* (non-Javadoc)
	 * @see com.l2jserver.gameserver.clientpackets.ClientBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _C__B9_REQUESTEVALUATE;
	}
}
