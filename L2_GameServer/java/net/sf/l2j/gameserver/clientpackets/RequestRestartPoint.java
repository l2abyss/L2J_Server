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

import java.util.logging.Logger;

import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.MapRegionTable;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.instancemanager.ClanHallManager;
import net.sf.l2j.gameserver.instancemanager.FortManager;
import net.sf.l2j.gameserver.model.L2SiegeClan;
import net.sf.l2j.gameserver.model.Location;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.Castle;
import net.sf.l2j.gameserver.model.entity.ClanHall;
import net.sf.l2j.gameserver.model.entity.Fort;
import net.sf.l2j.gameserver.util.IllegalPlayerAction;
import net.sf.l2j.gameserver.util.Util;


/**
 * This class ...
 *
 * @version $Revision: 1.7.2.3.2.6 $ $Date: 2005/03/27 15:29:30 $
 */
public final class RequestRestartPoint extends L2GameClientPacket
{
	private static final String _C__6d_REQUESTRESTARTPOINT = "[C] 6d RequestRestartPoint";
	private static Logger _log = Logger.getLogger(RequestRestartPoint.class.getName());

	protected int     _requestedPointType;
	protected boolean _continuation;


	@Override
	protected void readImpl()
	{
		_requestedPointType = readD();
	}

	class DeathTask implements Runnable
	{
		final L2PcInstance activeChar;
        
		DeathTask (L2PcInstance _activeChar)
		{
			activeChar = _activeChar;
		}

		public void run()
		{
		    Location loc = null;
            Castle castle = null;
            Fort fort = null;
            Boolean isInDefense = false;
		    
		    // force jail
		    if (activeChar.isInJail())
		    {
		        _requestedPointType = 27;
		    }
		    else if (activeChar.isFestivalParticipant())
		    {
		        _requestedPointType = 5;
		    }
		    switch (_requestedPointType)
		    {
		        case 1: // to clanhall
		            if (activeChar.getClan().getHasHideout() == 0)
		            {
		                //cheater
		                activeChar.sendMessage("You may not use this respawn point!");
		                Util.handleIllegalPlayerAction(activeChar, "Player " + activeChar.getName() + " used respawn cheat.", IllegalPlayerAction.PUNISH_KICK);
		                return;
		            }
		            loc = MapRegionTable.getInstance().getTeleToLocation(activeChar, MapRegionTable.TeleportWhereType.ClanHall);
		            
		            if (ClanHallManager.getInstance().getClanHallByOwner(activeChar.getClan())!= null &&
		                    ClanHallManager.getInstance().getClanHallByOwner(activeChar.getClan()).getFunction(ClanHall.FUNC_RESTORE_EXP) != null)
		            {
		                activeChar.restoreExp(ClanHallManager.getInstance().getClanHallByOwner(activeChar.getClan()).getFunction(ClanHall.FUNC_RESTORE_EXP).getLvl());
		            }
		            break;

		        case 2: // to castle
		            castle = CastleManager.getInstance().getCastle(activeChar);
                    
		            if (castle != null && castle.getSiege().getIsInProgress())
		            {
		                //siege in progress
		                if (castle.getSiege().checkIsDefender(activeChar.getClan()))
		                	loc = MapRegionTable.getInstance().getTeleToLocation(activeChar, MapRegionTable.TeleportWhereType.Castle);
		                // Just in case you lost castle while beeing dead.. Port to nearest Town.
		                else if (castle.getSiege().checkIsAttacker(activeChar.getClan()))
		                	loc = MapRegionTable.getInstance().getTeleToLocation(activeChar, MapRegionTable.TeleportWhereType.Town);
		                else
		                {
		                	//cheater
			                activeChar.sendMessage("You may not use this respawn point!");
			                Util.handleIllegalPlayerAction(activeChar, "Player " + activeChar.getName() + " used respawn cheat.", IllegalPlayerAction.PUNISH_KICK);
			                return;
		                }
		            }
		            else
		            {
		            	if (activeChar.getClan().getHasCastle() == 0)
			            {
			                //cheater
			                activeChar.sendMessage("You may not use this respawn point!");
			                Util.handleIllegalPlayerAction(activeChar, "Player " + activeChar.getName() + " used respawn cheat.", IllegalPlayerAction.PUNISH_KICK);
			                return;
			            }
		            	else
		            		loc = MapRegionTable.getInstance().getTeleToLocation(activeChar, MapRegionTable.TeleportWhereType.Castle);
		            }
		            break;

		        case 3: // to fortress
                    fort = FortManager.getInstance().getFort(activeChar);
                    
                    if (fort != null && fort.getSiege().getIsInProgress())
                    {
                        //siege in progress
                        if (fort.getSiege().checkIsDefender(activeChar.getClan()))
                            isInDefense = true;
                    }
		            if (activeChar.getClan().getHasFort() == 0  && !isInDefense )
		            {
		                //cheater
		                activeChar.sendMessage("You may not use this respawn point!");
		                Util.handleIllegalPlayerAction(activeChar, "Player " + activeChar.getName() + " used respawn cheat.", IllegalPlayerAction.PUNISH_KICK);
		                return;
		            }
		            loc = MapRegionTable.getInstance().getTeleToLocation(activeChar, MapRegionTable.TeleportWhereType.Fortress);
		            break;
		            
		        case 4: // to siege HQ
		            L2SiegeClan siegeClan = null;
		            castle = CastleManager.getInstance().getCastle(activeChar);
                    fort = FortManager.getInstance().getFort(activeChar);
		            
                    if (castle != null && castle.getSiege().getIsInProgress())
                        siegeClan = castle.getSiege().getAttackerClan(activeChar.getClan());
                    else if (fort != null && fort.getSiege().getIsInProgress())
                        siegeClan = fort.getSiege().getAttackerClan(activeChar.getClan());
		            
		            if (siegeClan == null || siegeClan.getFlag().size() == 0)
		            {
		                //cheater
		                activeChar.sendMessage("You may not use this respawn point!");
		                Util.handleIllegalPlayerAction(activeChar, "Player " + activeChar.getName() + " used respawn cheat.", IllegalPlayerAction.PUNISH_KICK);
		                return;
		            }
		            loc = MapRegionTable.getInstance().getTeleToLocation(activeChar, MapRegionTable.TeleportWhereType.SiegeFlag);
		            break;
		            
		        case 5: // Fixed or Player is a festival participant
		            if (!activeChar.isGM() && !activeChar.isFestivalParticipant())
		            {
		                //cheater
		                activeChar.sendMessage("You may not use this respawn point!");
		                Util.handleIllegalPlayerAction(activeChar, "Player " + activeChar.getName() + " used respawn cheat.", IllegalPlayerAction.PUNISH_KICK);
		                return;
		            }
		            loc = new Location(activeChar.getX(), activeChar.getY(), activeChar.getZ()); // spawn them where they died
		            break;
		            
		        case 27: // to jail
		            if (!activeChar.isInJail()) return;
		            loc = new Location(-114356, -249645, -2984);
		            break;
		            
		        default:
		            loc = MapRegionTable.getInstance().getTeleToLocation(activeChar, MapRegionTable.TeleportWhereType.Town);
		        break;
		    }
		    
		    //Teleport and revive
		    activeChar.setIsPendingRevive(true);
		    activeChar.teleToLocation(loc, true);
		}
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();

		if (activeChar == null)
			return;

		//SystemMessage sm2 = new SystemMessage(SystemMessage.S1_S2);
		//sm2.addString("type:"+requestedPointType);
		//activeChar.sendPacket(sm2);

		if (activeChar.isFakeDeath())
		{
			activeChar.stopFakeDeath(null);
			return;
		}
		else if(!activeChar.isDead())
		{
			_log.warning("Living player ["+activeChar.getName()+"] called RestartPointPacket! Ban this player!");
			return;
		}

		Castle castle = CastleManager.getInstance().getCastle(activeChar.getX(), activeChar.getY(), activeChar.getZ());
		if (castle != null && castle.getSiege().getIsInProgress())
		{
			//DeathFinalizer df = new DeathFinalizer(10000);
			if (activeChar.getClan() != null && castle.getSiege().checkIsAttacker(activeChar.getClan()))
			{
				// Schedule respawn delay for attacker
				ThreadPoolManager.getInstance().scheduleGeneral(new DeathTask(activeChar), castle.getSiege().getAttackerRespawnDelay());
				activeChar.sendMessage("You will be re-spawned in " + castle.getSiege().getAttackerRespawnDelay()/1000 + " seconds");
			}
			else
			{
				// Schedule respawn delay for defender with penalty for CT lose
				ThreadPoolManager.getInstance().scheduleGeneral(new DeathTask(activeChar), castle.getSiege().getDefenderRespawnDelay());
				activeChar.sendMessage("You will be re-spawned in " + castle.getSiege().getDefenderRespawnDelay()/1000 + " seconds");
			}
			return;
		}
        
        // run immediatelly (no need to schedule)
        new DeathTask(activeChar).run();
	}



	/* (non-Javadoc)
     * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#getType()
     */
	@Override
	public String getType()
	{
		return _C__6d_REQUESTRESTARTPOINT;
	}
}
