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
package net.sf.l2j.gameserver.network.clientpackets;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.FriendAddRequest;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 * This class ...
 *
 * @version $Revision: 1.3.4.2 $ $Date: 2005/03/27 15:29:30 $
 */
public final class RequestFriendInvite extends L2GameClientPacket
{
	private static final String _C__5E_REQUESTFRIENDINVITE = "[C] 5E RequestFriendInvite";
	private static Logger _log = Logger.getLogger(RequestFriendInvite.class.getName());

	private String _name;

	@Override
	protected void readImpl()
	{
		_name = readS();
	}

	@Override
	protected void runImpl()
	{
		SystemMessage sm;
		Connection con = null;
		L2PcInstance activeChar = getClient().getActiveChar();

        if (activeChar == null)
            return;

        L2PcInstance friend = L2World.getInstance().getPlayer(_name);

    	if (friend == null)
        {
    	    //Target is not found in the game.
    	    sm = new SystemMessage(SystemMessageId.THE_USER_YOU_REQUESTED_IS_NOT_IN_GAME);
    	    activeChar.sendPacket(sm);
    	    sm = null;
    	    return;
    	}
        else if (friend == activeChar)
        {
    	    //You cannot add yourself to your own friend list.
    	    sm = new SystemMessage(SystemMessageId.YOU_CANNOT_ADD_YOURSELF_TO_OWN_FRIEND_LIST);
    	    activeChar.sendPacket(sm);
    	    sm = null;
    	    return;
    	}
    	String name = friend.getName();
		try
		{
		    con = L2DatabaseFactory.getInstance().getConnection();
		    PreparedStatement statement = con.prepareStatement("SELECT charId FROM character_friends WHERE charId=? AND friendId=?");
		    statement.setInt(1, activeChar.getObjectId());
		    statement.setInt(2, friend.getObjectId());
		    ResultSet rset = statement.executeQuery();

            if (rset.next())
            {
    			//Player already is in your friendlist
    			sm = new SystemMessage(SystemMessageId.S1_ALREADY_IN_FRIENDS_LIST);
    			sm.addString(name);
		    }
            else
            {
		        if (!friend.isProcessingRequest())
		        {
		    	    //requets to become friend
    			    activeChar.onTransactionRequest(friend);
    			    sm = new SystemMessage(SystemMessageId.C1_REQUESTED_TO_BECOME_FRIENDS);
    			    sm.addString(name);
    			    FriendAddRequest ajf = new FriendAddRequest(activeChar.getName());
    			    friend.sendPacket(ajf);
    			}
                else
                {
    			    sm = new SystemMessage(SystemMessageId.C1_IS_BUSY_TRY_LATER);
    			    sm.addString(name);
    			}
		    }

			activeChar.sendPacket(sm);
			sm = null;
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
		    _log.log(Level.WARNING, "could not add friend objectid: ", e);
		}
		finally
		{
		    try { con.close(); } catch (Exception e) {}
		}
	}

	@Override
	public String getType()
	{
		return _C__5E_REQUESTFRIENDINVITE;
	}
}