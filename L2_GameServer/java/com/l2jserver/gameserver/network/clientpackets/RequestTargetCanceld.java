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

import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;

/**
 * This class ...
 *
 * @version $Revision: 1.3.4.2 $ $Date: 2005/03/27 15:29:30 $
 */
public final class RequestTargetCanceld extends L2GameClientPacket
{
	private static final String _C__37_REQUESTTARGETCANCELD = "[C] 37 RequestTargetCanceld";
	//private static Logger _log = Logger.getLogger(RequestTargetCanceld.class.getName());

    private int _unselect;

	@Override
	protected void readImpl()
	{
        _unselect = readH();
	}

	@Override
	protected void runImpl()
	{
		L2Character activeChar = getClient().getActiveChar();
        if (activeChar != null)
        {
        	if (((L2PcInstance)activeChar).isLockedTarget())
        	{
        		activeChar.sendPacket(new SystemMessage(SystemMessageId.FAILED_DISABLE_TARGET));
        		return;
        	}
            if (_unselect == 0)
            {
            	if (activeChar.isCastingNow() && activeChar.canAbortCast())
            		activeChar.abortCast();
            	else if (activeChar.getTarget() != null)
            		activeChar.setTarget(null);
            }
            else if (activeChar.getTarget() != null)
            	activeChar.setTarget(null);
        }
	}

	/* (non-Javadoc)
	 * @see com.l2jserver.gameserver.clientpackets.ClientBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _C__37_REQUESTTARGETCANCELD;
	}
}
