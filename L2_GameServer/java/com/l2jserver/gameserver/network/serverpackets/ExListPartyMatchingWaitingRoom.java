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

import javolution.util.FastList;
import com.l2jserver.gameserver.model.PartyMatchWaitingList;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;

/**
* @author Gnacik
*/
public class ExListPartyMatchingWaitingRoom extends L2GameServerPacket
{
	@SuppressWarnings("unused")
	private final L2PcInstance _activeChar;
	@SuppressWarnings("unused")
	private int _page;
	private int _minlvl;
	private int _maxlvl;
	private int _mode;
	private FastList<L2PcInstance> _members;
	
	public ExListPartyMatchingWaitingRoom(L2PcInstance player, int page, int minlvl, int maxlvl, int mode)
	{
		 _activeChar = player;
		 _page = page;
		 _minlvl = minlvl;
		 _maxlvl = maxlvl;
		 _mode = mode;
		 _members = new FastList<L2PcInstance>();
	}
    
    @Override
    protected void writeImpl()
    {
    	writeC(0xfe);
        writeH(0x36);
        if (_mode == 0)
        {
        	writeD(0);
        	writeD(0);
        	return;
        }
        
        //if (_activeChar.isInPartyMatchRoom())
        //	return;
        
    	for(L2PcInstance cha : PartyMatchWaitingList.getInstance().getPlayers())
    	{
    		if(cha == null)
    			continue;
    		
    		if(!cha.isPartyWaiting())
    		{
    			PartyMatchWaitingList.getInstance().removePlayer(cha);
    			continue;
    		}
    		
    		if((cha.getLevel() < _minlvl) || (cha.getLevel() > _maxlvl))
    			continue;
    		
    		_members.add(cha);
    	}

    	int _count = 0;
		int _size = _members.size();
    	
        writeD(1);
        writeD(_size);
        while(_size > _count)
        {
        	writeS(_members.get(_count).getName());
        	writeD(_members.get(_count).getActiveClass());
        	writeD(_members.get(_count).getLevel());
        	_count++;
        }
    }
	
    @Override
    public String getType()
    {
        return "[S] FE:36 ExListPartyMatchingWaitingRoom";
    }
}