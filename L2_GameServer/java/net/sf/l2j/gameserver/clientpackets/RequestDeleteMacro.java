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
package net.sf.l2j.gameserver.clientpackets;

import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;

public final class RequestDeleteMacro extends L2GameClientPacket
{
	private int _id;

	private static final String _C__C2_REQUESTDELETEMACRO = "[C] C2 RequestDeleteMacro";

	@Override
	protected void readImpl()
	{
		_id = readD();
	}

	@Override
	protected void runImpl()
	{
		if (getClient().getActiveChar() == null)
		    return;
		getClient().getActiveChar().deleteMacro(_id);
	    SystemMessage sm = new SystemMessage(SystemMessageId.S1_S2);
	    sm.addString("Delete macro id="+_id);
		sendPacket(sm);
		sm = null;
	}

	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _C__C2_REQUESTDELETEMACRO;
	}

}
