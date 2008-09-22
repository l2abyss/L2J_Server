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

import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.TaskPriority;
import net.sf.l2j.gameserver.Universe;
import net.sf.l2j.gameserver.geoeditorcon.GeoEditorListener;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.PartyMemberPosition;
import net.sf.l2j.gameserver.network.serverpackets.ValidateLocation;
import net.sf.l2j.gameserver.network.serverpackets.ValidateLocationInVehicle;

/**
 * This class ...
 *
 * @version $Revision: 1.13.4.7 $ $Date: 2005/03/27 15:29:30 $
 */
public class ValidatePosition extends L2GameClientPacket
{
    private static Logger _log = Logger.getLogger(ValidatePosition.class.getName());
    private static final String _C__48_VALIDATEPOSITION = "[C] 48 ValidatePosition";

    /** urgent messages, execute immediately */
    public TaskPriority getPriority() { return TaskPriority.PR_HIGH; }

    private int _x;
    private int _y;
    private int _z;
    private int _heading;
    @SuppressWarnings("unused")
    private int _data;


    @Override
	protected void readImpl()
    {
        _x  = readD();
        _y  = readD();
        _z  = readD();
        _heading  = readD();
        _data  = readD();
    }

    @Override
	protected void runImpl()
    {
        L2PcInstance activeChar = getClient().getActiveChar();
        if (activeChar == null || activeChar.isTeleporting()) return;

        activeChar.setClientX(_x);
        activeChar.setClientY(_y);
        activeChar.setClientZ(_z);
        activeChar.setClientHeading(_heading); // No real need to validate heading.
        int realX = activeChar.getX();
        int realY = activeChar.getY();
        int realZ = activeChar.getZ();
        activeChar.setLastServerPosition(realX, realY, realZ);
        
        if(activeChar.getParty() != null && activeChar.getLastPartyPositionDistance(_x, _y, _z) > 150)
        {
            activeChar.setLastPartyPosition(_x, _y, _z);
        	activeChar.getParty().broadcastToPartyMembers(activeChar,new PartyMemberPosition(activeChar));
        }
        
        double dx = _x - realX;
        double dy = _y - realY;
        double dz = Math.abs(_z - realZ);
        double diffSq = (dx*dx + dy*dy);
        
        if (Config.DEVELOPER) {
            _log.fine("client pos: "+ _x + " "+ _y + " "+ _z +" head "+ _heading);
            _log.fine("server pos: "+ realX + " "+realY+ " "+realZ +" head "+activeChar.getHeading());
        }
        if (Config.ACCEPT_GEOEDITOR_CONN)
            if (GeoEditorListener.getInstance().getThread() != null  
            		&& GeoEditorListener.getInstance().getThread().isWorking()  
            		&& GeoEditorListener.getInstance().getThread().isSend(activeChar))
            	GeoEditorListener.getInstance().getThread().sendGmPosition(_x,_y,(short)_z);
        
        if (Config.ACTIVATE_POSITION_RECORDER 
        		&& !activeChar.isFlying() 
        		&& Universe.getInstance().shouldLog(activeChar.getObjectId()))
            Universe.getInstance().registerHeight(realX, realY, _z);
       
        if (activeChar.isFlying())
        {
        	activeChar.setXYZ(_x, _y, _z);
        }
        else if (diffSq < 250000) // if too large, messes observation
        {
            if (Config.COORD_SYNCHRONIZE == -1) // Only Z coordinate synched to server, mainly used when no geodata
            {
            	activeChar.setXYZ(realX,realY,_z);
            	return;
            }
            if (Config.COORD_SYNCHRONIZE == 1 && (!activeChar.isMoving() // Trusting client coordinates (should not be used with geodata)
            		|| !activeChar.validateMovementHeading(_heading))) 	 // Heading changed on client = possible obstacle
            {
            	// character is not moving, take coordinates from client
            	if (diffSq < 2500) // 50*50 - attack won't work fluently if even small differences are corrected
            		activeChar.setXYZ(realX, realY, _z);
            	else
            		activeChar.setXYZ(_x, _y, _z);
            	activeChar.setHeading(_heading);
            	return;
            }
        	if (Config.GEODATA > 0 
        		&& (diffSq > 10000 || dz > 500)) // Sync 2 (or other), 
        										 // intended for geodata. Only sends a validation packet to client 
            {								     // when too far from server calculated true coordinate.
        		// a new geodata check seems unnecessary
            	//short geoHeight = GeoData.getInstance().getSpawnHeight(realX, realY, realZ-30, realZ+30, activeChar.getObjectId()); 
            	//if (realZ != geoHeight) System.out.println("realZ:"+realZ+" spawnheight:"+geoHeight);
            	//activeChar.setXYZ(realX, realY, geoHeight);

            	//System.out.println("validating pos diffsq:"+diffSq+" dz:"+dz);
            	if (Config.DEVELOPER)
                     _log.info(activeChar.getName() + ": Synchronizing position Server --> Client");
            	if (activeChar.isInBoat())
            	{
            		sendPacket(new ValidateLocationInVehicle(activeChar));
            	}
            	else
            	{
            		activeChar.sendPacket(new ValidateLocation(activeChar));
            	}
            }
        	
        }
		
    }

    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#getType()
     */
    @Override
	public String getType()
    {
        return _C__48_VALIDATEPOSITION;
    }

 
}
