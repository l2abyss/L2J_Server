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
package net.sf.l2j.gameserver.model.entity;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Logger;

import javolution.util.FastList;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.Announcements;
import net.sf.l2j.gameserver.FortUpdater;
import net.sf.l2j.gameserver.SevenSigns;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.datatables.DoorTable;
import net.sf.l2j.gameserver.instancemanager.FortManager;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.instance.L2DoorInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.zone.type.L2FortZone;
import net.sf.l2j.gameserver.network.serverpackets.PlaySound;
import net.sf.l2j.gameserver.network.serverpackets.PledgeShowInfoUpdate;


public class Fort
{
    protected static final Logger _log = Logger.getLogger(Fort.class.getName());

    // =========================================================
    // Data Field
    private int _fortId                      = 0;
    private List<L2DoorInstance> _doors        = new FastList<L2DoorInstance>();
    private List<String> _doorDefault          = new FastList<String>();
    private String _name                     = "";
    private int _ownerId                     = 0;
    private FortSiege _siege                       = null;
    private Calendar _siegeDate;
    private Calendar _lastOwnedTime;             
    private L2FortZone _zone;
    private L2Clan _formerOwner                = null;
    
    // =========================================================
    // Constructor
    public Fort(int fortId)
    {
        _fortId = fortId;
        load();
        loadDoor();
    }

    // =========================================================
    // Method - Public

    public void EndOfSiege(L2Clan clan)
    {
        ThreadPoolManager.getInstance().scheduleGeneral(new endFortressSiege(this,clan), 1000);

    }
    public void Engrave(L2Clan clan, int objId)
    {
       getSiege().announceToPlayer("Clan " + clan.getName() + " has finished to raise the flag.", true);
       setOwner(clan);
    }

    // This method add to the treasury
    /** Add amount to fort instance's treasury (warehouse). */
    public void addToTreasury(int amount)
    {
            return;
    }

    /** Add amount to fort instance's treasury (warehouse), no tax paying. */
    public boolean addToTreasuryNoTax(int amount)
    {
        return true;
    }

    /**
     * Move non clan members off fort area and to nearest town.<BR><BR>
     */
    public void banishForeigners()
    {
        _zone.banishForeigners(getOwnerId());
    }
    
    /**
     * Return true if object is inside the zone
     */
    public boolean checkIfInZone(int x, int y, int z)
    {
        return _zone.isInsideZone(x, y, z);
    }
    
    /**
     * Sets this forts zone
     * @param zone
     */
    public void setZone(L2FortZone zone)
    {
        _zone = zone;
    }
    
    public L2FortZone getZone()
    {
        return _zone;
    }
    
    /**
     * Get the objects distance to this fort
     * @param obj
     * @return
     */
    public double getDistance(L2Object obj)
    {
        return _zone.getDistanceToZone(obj);
    }
    
    public void closeDoor(L2PcInstance activeChar, int doorId)
    {
        openCloseDoor(activeChar, doorId, false);
    }

    public void openDoor(L2PcInstance activeChar, int doorId)
    {
        openCloseDoor(activeChar, doorId, true);
    }

    public void openCloseDoor(L2PcInstance activeChar, int doorId, boolean open)
    {
        if (activeChar.getClanId() != getOwnerId())
            return;

        L2DoorInstance door = getDoor(doorId);
        if (door != null)
        {
            if (open)
                door.openMe();
            else
                door.closeMe();
        }
    }

    // This method is used to begin removing all fort upgrades
    public void removeUpgrade()
    {
        removeDoorUpgrade();
    }

    // This method updates the fort tax rate
    public void setOwner(L2Clan clan)
    {
        // Remove old owner
        if (getOwnerId() > 0 && (clan == null || clan.getClanId() != getOwnerId()))
        {
            L2Clan oldOwner = ClanTable.getInstance().getClan(getOwnerId());            // Try to find clan instance
            if (oldOwner != null)
            {
                if (_formerOwner == null)
                {
                    _formerOwner = oldOwner;
                }
                oldOwner.setHasFort(0);                                               // Unset has fort flag for old owner
                Announcements.getInstance().announceToAll(oldOwner.getName() + " has lost " + getName() + " fortress!");

            }
        }

	    // if clan have already fortress, remove it
	    if (clan.getHasFort() > 0)
	    	FortManager.getInstance().getFortByOwner(clan).removeOwner(clan);

        //if clan already have castle, dont store him in fortress
        if (clan.getHasCastle() <=0)
        	updateOwnerInDB(clan);                                                          // Update in database
        else
        {
        	getSiege().setHasCastle();
        	updateOwnerInDB(null);
        }

        if (getSiege().getIsInProgress())                                               // If siege in progress
            getSiege().midVictory();                                                    // Mid victory phase of siege

        updateClansReputation();
    }

    public void removeOwner(L2Clan clan)
    {
        if (clan != null)
        {
            _formerOwner = clan;
            clan.setHasFort(0);
             Announcements.getInstance().announceToAll(clan.getName() + " has lost " +getName() + " fort");
            clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));
        }

        updateOwnerInDB(null);
        if (getSiege().getIsInProgress())
            getSiege().midVictory();

        updateClansReputation();
    }

    // This method updates the fort tax rate
    public void setTaxPercent(L2PcInstance activeChar, int taxPercent)
    {
        int maxTax;
        switch(SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE))
        {
            case SevenSigns.CABAL_DAWN:
                maxTax = 25;
                break;
            case SevenSigns.CABAL_DUSK:
                maxTax = 5;
                break;
            default: // no owner
                maxTax = 15;
        }

        if (taxPercent < 0 || taxPercent > maxTax)
        {
            activeChar.sendMessage("Tax value must be between 0 and "+maxTax+".");
            return;
        }

        activeChar.sendMessage(getName() + " fort tax changed to " + taxPercent + "%.");
    }

    /**
     * Respawn all doors on fort grounds<BR><BR>
     */
    public void spawnDoor()
    {
        spawnDoor(false);
    }

    /**
     * Respawn all doors on fort grounds<BR><BR>
     */
    public void spawnDoor(boolean isDoorWeak)
    {
        for (int i = 0; i < getDoors().size(); i++)
        {
            L2DoorInstance door = getDoors().get(i);
            if (door.getCurrentHp() <= 0)
            {
                door.decayMe(); // Kill current if not killed already
                door = DoorTable.parseList(_doorDefault.get(i));
                if (isDoorWeak)
                    door.setCurrentHp(door.getMaxHp() / 2);
                door.spawnMe(door.getX(), door.getY(),door.getZ());
                getDoors().set(i, door);
            }
            else if (door.getOpen() == 0)
                door.closeMe();
        }
        loadDoorUpgrade(); // Check for any upgrade the doors may have
    }

    // This method upgrade door
    public void upgradeDoor(int doorId, int hp, int pDef, int mDef)
    {
        L2DoorInstance door = getDoor(doorId);
        if (door == null)
            return;

        if (door != null && door.getDoorId() == doorId)
        {
            door.setCurrentHp(door.getMaxHp() + hp);

            saveDoorUpgrade(doorId, hp, pDef, mDef);
            return;
        }
    }
    
    // =========================================================
    // Method - Private
    // This method loads fort
    private void load()
    {
        java.sql.Connection con = null;
        try
        {
            PreparedStatement statement;
            ResultSet rs;

            con = L2DatabaseFactory.getInstance().getConnection();

            statement = con.prepareStatement("Select * from fort where id = ?");
            statement.setInt(1, getFortId());
            rs = statement.executeQuery();

            while (rs.next())
            {
                _name = rs.getString("name");
                //_OwnerId = rs.getInt("ownerId");

                _siegeDate = Calendar.getInstance();
                _lastOwnedTime = Calendar.getInstance();
                _siegeDate.setTimeInMillis(rs.getLong("siegeDate"));
                _lastOwnedTime.setTimeInMillis(rs.getLong("lastOwnedTime"));
                _ownerId = rs.getInt("owner");
            }

            rs.close();
            statement.close();

            if (getOwnerId() > 0)
            {
                L2Clan clan = ClanTable.getInstance().getClan(getOwnerId());                        // Try to find clan instance
                clan.setHasFort(getFortId());
                ThreadPoolManager.getInstance().scheduleGeneral(new FortUpdater(clan, 1), 3600000);     // Schedule owner tasks to start running
            }

        }
        catch (Exception e)
        {
            _log.warning("Exception: loadFortData(): " + e.getMessage());
            e.printStackTrace();
        }
        finally {try { con.close(); } catch (Exception e) {}}
    }

    // This method loads fort door data from database
    private void loadDoor()
    {
        java.sql.Connection con = null;
        try
        {
            con = L2DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement = con.prepareStatement("Select * from fort_door where fortId = ?");
            statement.setInt(1, getFortId());
            ResultSet rs = statement.executeQuery();

            while (rs.next())
            {
                // Create list of the door default for use when respawning dead doors
                _doorDefault.add(rs.getString("name")
                        + ";" + rs.getInt("id")
                        + ";" + rs.getInt("x")
                        + ";" + rs.getInt("y")
                        + ";" + rs.getInt("z")
                        + ";" + rs.getInt("range_xmin")
                        + ";" + rs.getInt("range_ymin")
                        + ";" + rs.getInt("range_zmin")
                        + ";" + rs.getInt("range_xmax")
                        + ";" + rs.getInt("range_ymax")
                        + ";" + rs.getInt("range_zmax")
                        + ";" + rs.getInt("hp")
                        + ";" + rs.getInt("pDef")
                        + ";" + rs.getInt("mDef"));

                L2DoorInstance door = DoorTable.parseList(_doorDefault.get(_doorDefault.size() - 1));
                door.spawnMe(door.getX(), door.getY(),door.getZ());
                _doors.add(door);
                DoorTable.getInstance().putDoor(door);
            }

            rs.close();
            statement.close();
        }
        catch (Exception e)
        {
            _log.warning("Exception: loadFortDoor(): " + e.getMessage());
            e.printStackTrace();
        }
        finally {try { con.close(); } catch (Exception e) {}}
    }

    // This method loads fort door upgrade data from database
    private void loadDoorUpgrade()
    {
        java.sql.Connection con = null;
        try
        {
            con = L2DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement = con.prepareStatement("Select * from fort_doorupgrade where doorId in (Select Id from fort_door where fortId = ?)");
            statement.setInt(1, getFortId());
            ResultSet rs = statement.executeQuery();

            while (rs.next())
            {
                upgradeDoor(rs.getInt("id"), rs.getInt("hp"), rs.getInt("pDef"), rs.getInt("mDef"));
            }
            rs.close();
            statement.close();
        }
        catch (Exception e)
        {
            _log.warning("Exception: loadFortDoorUpgrade(): " + e.getMessage());
            e.printStackTrace();
        }
        finally {try { con.close(); } catch (Exception e) {}}
    }

    private void removeDoorUpgrade()
    {
        java.sql.Connection con = null;
        try
        {
            con = L2DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement = con.prepareStatement("delete from fort_doorupgrade where doorId in (select id from fort_door where fortId=?)");
            statement.setInt(1, getFortId());
            statement.execute();
            statement.close();
        }
        catch (Exception e)
        {
            _log.warning("Exception: removeDoorUpgrade(): " + e.getMessage());
            e.printStackTrace();
        }
        finally {try { con.close(); } catch (Exception e) {}}
    }

    private void saveDoorUpgrade(int doorId, int hp, int pDef, int mDef)
    {
        java.sql.Connection con = null;
        try
        {
            con = L2DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement = con.prepareStatement("INSERT INTO fort_doorupgrade (doorId, hp, pDef, mDef) values (?,?,?,?)");
            statement.setInt(1, doorId);
            statement.setInt(2, hp);
            statement.setInt(3, pDef);
            statement.setInt(4, mDef);
            statement.execute();
            statement.close();
        }
        catch (Exception e)
        {
            _log.warning("Exception: saveDoorUpgrade(int doorId, int hp, int pDef, int mDef): " + e.getMessage());
            e.printStackTrace();
        }
        finally
        {
            try { con.close(); } catch (Exception e) {}
        }
    }

    private void updateOwnerInDB(L2Clan clan)
    {
        if (clan != null)
            _ownerId = clan.getClanId();    // Update owner id property
        else
            _ownerId = 0;                   // Remove owner

        if (_ownerId != 0)
        	_lastOwnedTime.setTimeInMillis( System.currentTimeMillis());    
        else
        	_lastOwnedTime.setTimeInMillis(0);                   
        
        
        
        java.sql.Connection con = null;
        try
        {
            con = L2DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement;

            statement = con.prepareStatement("UPDATE fort SET owner=?,lastOwnedTime=? where id = ?");
            statement.setInt(1, getOwnerId());
           	statement.setLong(2, _lastOwnedTime.getTimeInMillis());
            statement.setInt(3, getFortId());
            statement.execute();
            statement.close();

            // ============================================================================

            // Announce to clan memebers
            if (clan != null)
            {
                clan.setHasFort(getFortId()); // Set has fort flag for new owner
                Announcements.getInstance().announceToAll(clan.getName() + " has taken " + getName() + " fort!");
                clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));
                clan.broadcastToOnlineMembers(new PlaySound(1, "Siege_Victory", 0, 0, 0, 0, 0));
                ThreadPoolManager.getInstance().scheduleGeneral(new FortUpdater(clan, 1), 3600000);   // Schedule owner tasks to start running
            }
        }
        catch (Exception e)
        {
            _log.warning("Exception: updateOwnerInDB(L2Clan clan): " + e.getMessage());
            e.printStackTrace();
        }
        finally
        {
            try { con.close(); } catch (Exception e) {}
        }
    }
    
    // =========================================================
    // Property
    public final int getFortId()
    {
        return _fortId;
    }
    public final int getOwnerId()
    {
        return _ownerId;
    }
    public final L2DoorInstance getDoor(int doorId)
    {
        if (doorId <= 0)
            return null;

        for (int i = 0; i < getDoors().size(); i++)
        {
            L2DoorInstance door = getDoors().get(i);
            if (door.getDoorId() == doorId)
                return door;
        }
        return null;
    }
    public final List<L2DoorInstance> getDoors()
    {
        return _doors;
    }
    public final FortSiege getSiege()
    {
        if (_siege == null) _siege = new FortSiege(new Fort[] {this});
        return _siege;
    }
    public final Calendar getSiegeDate() { return _siegeDate; }
    public final void setSiegeDate(Calendar siegeDate)
    {
        _siegeDate = siegeDate;
    }
    public final int getOwnedTime() 
    {
    	if (_lastOwnedTime.getTimeInMillis() == 0 )
    		return 0;
    	
    	return (int)((System.currentTimeMillis() - _lastOwnedTime.getTimeInMillis())/1000); 
    }
   
    public final String getName()
    {
        return _name;
    }
    public void updateClansReputation()
    {
        if (_formerOwner != null )
        {
            if (_formerOwner != ClanTable.getInstance().getClan(getOwnerId()))
            {
                int maxreward = Math.max(0,_formerOwner.getReputationScore());
                L2Clan owner = ClanTable.getInstance().getClan(getOwnerId());
                if (owner != null)
                {
                    owner.setReputationScore(owner.getReputationScore()+Math.min(500,maxreward), true);
                    owner.broadcastToOnlineMembers(new PledgeShowInfoUpdate(owner));
                }
            }
            else
                _formerOwner.setReputationScore(_formerOwner.getReputationScore()+250, true);

            _formerOwner.broadcastToOnlineMembers(new PledgeShowInfoUpdate(_formerOwner));
        }
        else
        {
            L2Clan owner = ClanTable.getInstance().getClan(getOwnerId());
            if (owner != null)
            {
                owner.setReputationScore(owner.getReputationScore()+500, true);
                owner.broadcastToOnlineMembers(new PledgeShowInfoUpdate(owner));
            }
        }
    }
    private class endFortressSiege implements Runnable
    {
        private Fort _f;
        private L2Clan _clan;
        
        public endFortressSiege(Fort f,L2Clan clan)
        {
            _f = f;
            _clan = clan;
        }
        public void run()
        {
            _f.Engrave(_clan,0);
        }
    
    }
}
