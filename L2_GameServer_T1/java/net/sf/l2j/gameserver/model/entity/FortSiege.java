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
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.datatables.MapRegionTable;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.instancemanager.FortSiegeGuardManager;
import net.sf.l2j.gameserver.instancemanager.FortSiegeManager;
import net.sf.l2j.gameserver.instancemanager.MercTicketManager;
import net.sf.l2j.gameserver.instancemanager.FortSiegeManager.SiegeSpawn;
import net.sf.l2j.gameserver.model.CombatFlag;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2SiegeClan;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.L2SiegeClan.SiegeClanType;
import net.sf.l2j.gameserver.model.actor.instance.L2CommanderInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.serverpackets.FortressSiegeInfo;
import net.sf.l2j.gameserver.serverpackets.RelationChanged;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.serverpackets.UserInfo;

public class FortSiege
{
    protected static final Logger _log = Logger.getLogger(FortSiege.class.getName());

    public static enum TeleportWhoType { All, Attacker, DefenderNotOwner, Owner, Spectator  }

    // ===============================================================
    // Schedule task
    public class ScheduleEndSiegeTask implements Runnable
    {
        private Fort _fortInst;

        public ScheduleEndSiegeTask(Fort pFort)
        {
            _fortInst = pFort;
        }

        public void run()
        {
            if (!getIsInProgress()) return;

            try
            {
                long timeRemaining = _siegeEndDate.getTimeInMillis()
                    - Calendar.getInstance().getTimeInMillis();
                if (timeRemaining > 3600000)
                {
                    ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleEndSiegeTask(_fortInst),
                                                                    timeRemaining - 3600000); // Prepare task for 1 hr left.
                }
                else if ((timeRemaining <= 3600000) && (timeRemaining > 600000))
                {
                    announceToPlayer(Math.round(timeRemaining / 60000) + " minute(s) until "
                        + getFort().getName() + " siege conclusion.", true);
                    ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleEndSiegeTask(_fortInst),
                                                                    timeRemaining - 600000); // Prepare task for 10 minute left.
                }
                else if ((timeRemaining <= 600000) && (timeRemaining > 300000))
                {
                    announceToPlayer(Math.round(timeRemaining / 60000) + " minute(s) until "
                        + getFort().getName() + " siege conclusion.", true);
                    ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleEndSiegeTask(_fortInst),
                                                                    timeRemaining - 300000); // Prepare task for 5 minute left.
                }
                else if ((timeRemaining <= 300000) && (timeRemaining > 10000))
                {
                    announceToPlayer(Math.round(timeRemaining / 60000) + " minute(s) until "
                        + getFort().getName() + " siege conclusion.", true);
                    ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleEndSiegeTask(_fortInst),
                                                                    timeRemaining - 10000); // Prepare task for 10 seconds count down
                }
                else if ((timeRemaining <= 10000) && (timeRemaining > 0))
                {
                    announceToPlayer(getFort().getName() + " siege "
                        + Math.round(timeRemaining / 1000) + " second(s) left!", true);
                    ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleEndSiegeTask(_fortInst),
                                                                    timeRemaining); // Prepare task for second count down
                }
                else
                {
                    _fortInst.getSiege().endSiege();
                }
            }
            catch (Throwable t)
            {

            }
        }
    }
    public class ScheduleStartSiegeTask implements Runnable
    {
        private Fort _fortInst;

        public ScheduleStartSiegeTask(Fort pFort)
        {
            _fortInst = pFort;
        }

        public void run()
        {
            if (getIsInProgress()) return;

            try
            {
                long timeRemaining = getSiegeDate().getTimeInMillis()
                    - Calendar.getInstance().getTimeInMillis();
                if (timeRemaining > 86400000)
                {
                    ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleStartSiegeTask(_fortInst),
                                                                    timeRemaining - 86400000); // Prepare task for 24 before siege start to end registration
                }
                else if ((timeRemaining <= 86400000) && (timeRemaining > 13600000))
                {
                    ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleStartSiegeTask(_fortInst),
                                                                    timeRemaining - 13600000); // Prepare task for 1 hr left before siege start.
                }
                else if ((timeRemaining <= 13600000) && (timeRemaining > 600000))
                {
                    announceToPlayer(Math.round(timeRemaining / 60000) + " minute(s) until "
                        + getFort().getName() + " siege begin.", false);
                    ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleStartSiegeTask(_fortInst),
                                                                    timeRemaining - 600000); // Prepare task for 10 minute left.
                }
                else if ((timeRemaining <= 600000) && (timeRemaining > 300000))
                {
                    announceToPlayer("The registration term for " + getFort().getName() + " has ended.", false);
                    _isRegistrationOver = true;
                    clearSiegeWaitingClan();
                    ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleStartSiegeTask(_fortInst),
                                                                    timeRemaining - 300000); // Prepare task for 5 minute left.
                }
                else if ((timeRemaining <= 300000) && (timeRemaining > 10000))
                {
                    announceToPlayer(Math.round(timeRemaining / 60000) + " minute(s) until "
                        + getFort().getName() + " siege begin.", false);
                    ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleStartSiegeTask(_fortInst),
                                                                    timeRemaining - 10000); // Prepare task for 10 seconds count down
                }
                else if ((timeRemaining <= 10000) && (timeRemaining > 0))
                {
                    announceToPlayer(getFort().getName() + " siege "
                        + Math.round(timeRemaining / 1000) + " second(s) to start!", false);
                    ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleStartSiegeTask(_fortInst),
                                                                    timeRemaining); // Prepare task for second count down
                }
                else
                {
                    _fortInst.getSiege().startSiege();
                }
            }
            catch (Throwable t)
            {

            }
        }
    }

    // =========================================================
    // Data Field
    // Attacker and Defender
    private List<L2SiegeClan> _attackerClans = new FastList<L2SiegeClan>(); // L2SiegeClan

    private List<L2SiegeClan> _defenderClans = new FastList<L2SiegeClan>(); // L2SiegeClan
    private List<L2SiegeClan> _defenderWaitingClans = new FastList<L2SiegeClan>(); // L2SiegeClan
    private int _defenderRespawnDelayPenalty;

    // Fort setting
    private List<L2CommanderInstance> _commanders = new FastList<L2CommanderInstance>();
    private Fort[] _fort;
    private boolean _isInProgress = false;
    private boolean _isScheduled = false;
    private boolean _isNormalSide = true; // true = Atk is Atk, false = Atk is Def
    protected boolean _isRegistrationOver = false;
    protected Calendar _siegeEndDate;
    private FortSiegeGuardManager _siegeGuardManager;
    protected Calendar _siegeRegistrationEndDate;

    // =========================================================
    // Constructor
    public FortSiege(Fort[] fort)
    {
        _fort = fort;
        //_siegeGuardManager = new SiegeGuardManager(getFort());

        checkAutoTask();
    }

    // =========================================================
    // Siege phases
    /**
     * When siege ends<BR><BR>
     */
    public void endSiege()
    {
        if (getIsInProgress())
        {
            announceToPlayer("The siege of " + getFort().getName() + " has finished!", false);

            if (getFort().getOwnerId() <= 0)
                announceToPlayer("The siege of " + getFort().getName() + " has ended in a draw.",
                                 false);

            removeFlags(); // Removes all flags. Note: Remove flag before teleporting players
            unSpawnFlags();
            teleportPlayer(FortSiege.TeleportWhoType.Attacker, MapRegionTable.TeleportWhereType.Town); // Teleport to the second closest town
            teleportPlayer(FortSiege.TeleportWhoType.DefenderNotOwner, MapRegionTable.TeleportWhereType.Town); // Teleport to the second closest town
            teleportPlayer(FortSiege.TeleportWhoType.Spectator, MapRegionTable.TeleportWhereType.Town); // Teleport to the second closest town
            _isInProgress = false; // Flag so that siege instance can be started
            updatePlayerSiegeStateFlags(true);
            saveFortSiege(); // Save fort specific data
            clearSiegeClan(); // Clear siege clan from db
            removeCommander(); // Remove commander from this fort
            _siegeGuardManager.unspawnSiegeGuard(); // Remove all spawned siege guard from this fort
            if (getFort().getOwnerId() > 0) _siegeGuardManager.removeMercs();
            getFort().spawnDoor(); // Respawn door to fort
            getFort().getZone().updateZoneStatusForCharactersInside();
        }
    }

    private void removeDefender(L2SiegeClan sc)
    {
        if (sc != null) getDefenderClans().remove(sc);
    }

    private void removeAttacker(L2SiegeClan sc)
    {
        if (sc != null) getAttackerClans().remove(sc);
    }

    private void addDefender(L2SiegeClan sc, SiegeClanType type)
    {
        if(sc == null) return;
        sc.setType(type);
        getDefenderClans().add(sc);
    }

    private void addAttacker(L2SiegeClan sc)
    {
        if(sc == null) return;
        sc.setType(SiegeClanType.ATTACKER);
        getAttackerClans().add(sc);
    }

    /**
     * When control of fort changed during siege<BR><BR>
     */
    public void midVictory()
    {
        if (getIsInProgress()) // Siege still in progress
        {
            // defenders to attacker
            for (L2SiegeClan sc : getDefenderClans())
            {
                if(sc != null) {
                    removeDefender(sc);
                    addAttacker(sc);
                }
            }

            // owner as defender
            L2SiegeClan sc_newowner = getAttackerClan(getFort().getOwnerId());
            removeAttacker(sc_newowner);
            addDefender(sc_newowner, SiegeClanType.OWNER);
            endSiege();
            return;

        }
    }

    /**
     * When siege starts<BR><BR>
     */
    public void startSiege()
    {
        if (!getIsInProgress())
        {
            if (getAttackerClans().size() <= 0)
            {
                SystemMessage sm;
                if (getFort().getOwnerId() <= 0)
                    sm = new SystemMessage(SystemMessageId.SIEGE_OF_S1_HAS_BEEN_CANCELED_DUE_TO_LACK_OF_INTEREST);
                else
                    sm = new SystemMessage(SystemMessageId.S1_SIEGE_WAS_CANCELED_BECAUSE_NO_CLANS_PARTICIPATED);
                sm.addString(getFort().getName());
                Announcements.getInstance().announceToAll(sm);
                return;
            }

            _isNormalSide = true; // Atk is now atk
            _isInProgress = true; // Flag so that same siege instance cannot be started again
            _isScheduled = false;

            loadSiegeClan(); // Load siege clan from db
            updatePlayerSiegeStateFlags(false);
            teleportPlayer(FortSiege.TeleportWhoType.Attacker, MapRegionTable.TeleportWhereType.Town); // Teleport to the closest town
            //teleportPlayer(Siege.TeleportWhoType.Spectator, MapRegionTable.TeleportWhereType.Town);      // Teleport to the second closest town
            spawnCommander(getFort().getFortId()); // Spawn commander
            //spawnControlTower(getFort().getFortId()); // Spawn control tower
            getFort().spawnDoor(); // Spawn door
            spawnSiegeGuard(); // Spawn siege guard
            MercTicketManager.getInstance().deleteTickets(getFort().getFortId()); // remove the tickets from the ground
            _defenderRespawnDelayPenalty = 0; // Reset respawn delay

            getFort().getZone().updateZoneStatusForCharactersInside();

            // Schedule a task to prepare auto siege end
            _siegeEndDate = Calendar.getInstance();
            _siegeEndDate.add(Calendar.MINUTE, FortSiegeManager.getInstance().getSiegeLength());
            ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleEndSiegeTask(getFort()), 1000); // Prepare auto end task

            announceToPlayer("The siege of " + getFort().getName() + " has started!", false);
            saveFortSiege();
            FortSiegeManager.getInstance().addSiege(this);

        }
    }

    // =========================================================
    // Method - Public
    /**
     * Announce to player.<BR><BR>
     * @param message The String of the message to send to player
     * @param inAreaOnly The boolean flag to show message to players in area only.
     */
    public void announceToPlayer(String message, boolean inAreaOnly)
    {
        if (inAreaOnly)
        {
            getFort().getZone().announceToPlayers(message);
            return;
        }

        // Get all players
        for (L2PcInstance player : L2World.getInstance().getAllPlayers())
        {
                player.sendMessage(message);
        }
    }

    public void updatePlayerSiegeStateFlags(boolean clear)
    {
        L2Clan clan;
        for(L2SiegeClan siegeclan : getAttackerClans())
        {
             clan = ClanTable.getInstance().getClan(siegeclan.getClanId());
             for (L2PcInstance member : clan.getOnlineMembers(""))
             {
                 if (clear) member.setSiegeState((byte)0);
                 else member.setSiegeState((byte)1);
                 member.sendPacket(new UserInfo(member));
                 for (L2PcInstance player : member.getKnownList().getKnownPlayers().values()) {
                    player.sendPacket(new RelationChanged(member, member.getRelation(player), member.isAutoAttackable(player)));
                 }
             }
        }
        for(L2SiegeClan siegeclan : getDefenderClans())
        {
            clan = ClanTable.getInstance().getClan(siegeclan.getClanId());
            for (L2PcInstance member : clan.getOnlineMembers(""))
            {
                if (clear) member.setSiegeState((byte)0);
                else member.setSiegeState((byte)2);
                member.sendPacket(new UserInfo(member));
                for (L2PcInstance player : member.getKnownList().getKnownPlayers().values()) {
                    player.sendPacket(new RelationChanged(member, member.getRelation(player), member.isAutoAttackable(player)));
                }
            }
        }
    }

    /**
     * Approve clan as defender for siege<BR><BR>
     * @param clanId The int of player's clan id
     */
    public void approveSiegeDefenderClan(int clanId)
    {
        if (clanId <= 0) return;
        saveSiegeClan(ClanTable.getInstance().getClan(clanId), 0, true);
        loadSiegeClan();
    }

    /** Return true if object is inside the zone */
    public boolean checkIfInZone(L2Object object)
    {
        return checkIfInZone(object.getX(), object.getY(), object.getZ());
    }

    /** Return true if object is inside the zone */
    public boolean checkIfInZone(int x, int y, int z)
    {
        return (getIsInProgress() && (getFort().checkIfInZone(x, y, z))); // Fort zone during siege
    }

    /**
     * Return true if clan is attacker<BR><BR>
     * @param clan The L2Clan of the player
     */
    public boolean checkIsAttacker(L2Clan clan)
    {
        return (getAttackerClan(clan) != null);
    }

    /**
     * Return true if clan is defender<BR><BR>
     * @param clan The L2Clan of the player
     */
    public boolean checkIsDefender(L2Clan clan)
    {
        return (getDefenderClan(clan) != null);
    }

    /**
     * Return true if clan is defender waiting approval<BR><BR>
     * @param clan The L2Clan of the player
     */
    public boolean checkIsDefenderWaiting(L2Clan clan)
    {
        return (getDefenderWaitingClan(clan) != null);
    }

    /** Clear all registered siege clans from database for fort */
    public void clearSiegeClan()
    {
        java.sql.Connection con = null;
        try
        {
            con = L2DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement = con.prepareStatement("DELETE FROM fortsiege_clans WHERE fort_id=?");
            statement.setInt(1, getFort().getFortId());
            statement.execute();
            statement.close();

            if (getFort().getOwnerId() > 0)
            {
                PreparedStatement statement2 = con.prepareStatement("DELETE FROM fortsiege_clans WHERE clan_id=?");
                statement2.setInt(1, getFort().getOwnerId());
                statement2.execute();
                statement2.close();
            }

            getAttackerClans().clear();
            getDefenderClans().clear();
            getDefenderWaitingClans().clear();
        }
        catch (Exception e)
        {
            _log.warning("Exception: clearSiegeClan(): " + e.getMessage());
            e.printStackTrace();
        }
        finally
        {
            try
            {
                con.close();
            }
            catch (Exception e)
            {
            }
        }
    }

    /** Set the date for the next siege. */
    private void clearSiegeDate()
    {
        getFort().getSiegeDate().setTimeInMillis(0);
        _isRegistrationOver = false; // Allow registration for next siege
    }
    
    /** Clear all siege clans waiting for approval from database for fort */
    public void clearSiegeWaitingClan()
    {
        java.sql.Connection con = null;
        try
        {
            con = L2DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement = con.prepareStatement("DELETE FROM fortsiege_clans WHERE fort_id=? and type = 2");
            statement.setInt(1, getFort().getFortId());
            statement.execute();
            statement.close();

            getDefenderWaitingClans().clear();
        }
        catch (Exception e)
        {
            _log.warning("Exception: clearSiegeWaitingClan(): " + e.getMessage());
            e.printStackTrace();
        }
        finally
        {
            try
            {
                con.close();
            }
            catch (Exception e)
            {
            }
        }
    }

    /** Return list of L2PcInstance registered as attacker in the zone. */
    public List<L2PcInstance> getAttackersInZone()
    {
        List<L2PcInstance> players = new FastList<L2PcInstance>();
        L2Clan clan;
        for(L2SiegeClan siegeclan : getAttackerClans())
        {
             clan = ClanTable.getInstance().getClan(siegeclan.getClanId());
             for (L2PcInstance player : clan.getOnlineMembers(""))
             {
                if (checkIfInZone(player.getX(), player.getY(), player.getZ())) players.add(player);
             }
        }
        return players;
    }

    /** Return list of L2PcInstance registered as defender but not owner in the zone. */
    public List<L2PcInstance> getDefendersButNotOwnersInZone()
    {
        List<L2PcInstance> players = new FastList<L2PcInstance>();
        L2Clan clan;
        for(L2SiegeClan siegeclan : getDefenderClans())
        {
             clan = ClanTable.getInstance().getClan(siegeclan.getClanId());
             if (clan.getClanId() == getFort().getOwnerId()) continue;
             for (L2PcInstance player : clan.getOnlineMembers(""))
             {
                if (checkIfInZone(player.getX(), player.getY(), player.getZ())) players.add(player);
             }
        }
        return players;
    }

    /** Return list of L2PcInstance in the zone. */
    public List<L2PcInstance> getPlayersInZone()
    {
        return getFort().getZone().getAllPlayers();
    }

    /** Return list of L2PcInstance owning the fort in the zone. */
    public List<L2PcInstance> getOwnersInZone()
    {
        List<L2PcInstance> players = new FastList<L2PcInstance>();
        L2Clan clan;
        for(L2SiegeClan siegeclan : getDefenderClans())
        {
             clan = ClanTable.getInstance().getClan(siegeclan.getClanId());
             if (clan.getClanId() != getFort().getOwnerId()) continue;
             for (L2PcInstance player : clan.getOnlineMembers(""))
             {
                if (checkIfInZone(player.getX(), player.getY(), player.getZ())) players.add(player);
             }
        }
        return players;
    }

    /** Return list of L2PcInstance not registered as attacker or defender in the zone. */
    public List<L2PcInstance> getSpectatorsInZone()
    {
        List<L2PcInstance> players = new FastList<L2PcInstance>();

        for (L2PcInstance player : L2World.getInstance().getAllPlayers())
        {
            // quick check from player states, which don't include siege number however
            if (!player.isInsideZone(L2Character.ZONE_SIEGE) || player.getSiegeState() != 0) continue;
            if ( checkIfInZone(player.getX(), player.getY(), player.getZ()))
                players.add(player);
        }

       return players;
    }

    /** Control Tower was skilled */
    public void killedCT(L2NpcInstance ct)
    {
        _defenderRespawnDelayPenalty += FortSiegeManager.getInstance().getControlTowerLosePenalty(); // Add respawn penalty to defenders for each control tower lose
    }

    /** Commanderr was skilled */
    public void killedCommander(L2CommanderInstance ct)
    {
        if (_commanders != null)
        {
            _commanders.remove(ct);
            if ( _commanders.size() == 0 )
                spawnFlag(getFort().getFortId());
                //System.out.println("Commander empty !");
        }

    }
    
    /** Remove the flag that was killed */
    public void killedFlag(L2NpcInstance flag)
    {
        if (flag == null) return;
        for (int i = 0; i < getAttackerClans().size(); i++)
        {
            if (getAttackerClan(i).removeFlag(flag)) return;
        }
    }

    /** Display list of registered clans */
    public void listRegisterClan(L2PcInstance player)
    {
        player.sendPacket(new FortressSiegeInfo(getFort()));
    }

    /**
     * Register clan as attacker<BR><BR>
     * @param player The L2PcInstance of the player trying to register
     */
    public void registerAttacker(L2PcInstance player)
    {
        registerAttacker(player, false);
    }

    public void registerAttacker(L2PcInstance player, boolean force)
    {

        if(player.getClan() == null) return;
        int allyId = 0;
        if (getFort().getOwnerId() != 0)
            allyId = ClanTable.getInstance().getClan(getFort().getOwnerId()).getAllyId();
        if (allyId != 0)
        {
            if(player.getClan().getAllyId() == allyId && !force)
            {
                player.sendMessage("You cannot register as an attacker because your alliance owns the fort");
                return;
            }
        }
        if (force || checkIfCanRegister(player))
        {
            saveSiegeClan(player.getClan(), 1, false); // Save to database
            // if the first registering we start the timer
            if (getAttackerClans().size() == 1)
                startAutoTask(true);
        }
    }

    /**
     * Register clan as defender<BR><BR>
     * @param player The L2PcInstance of the player trying to register
     */
    public void registerDefender(L2PcInstance player)
    {
        registerDefender(player, false);
    }

    public void registerDefender(L2PcInstance player, boolean force)
    {
        if (getFort().getOwnerId() <= 0) player.sendMessage("You cannot register as a defender because "
            + getFort().getName() + " is owned by NPC.");
        else if (force || checkIfCanRegister(player)) saveSiegeClan(player.getClan(), 2, false); // Save to database
    }

    /**
     * Remove clan from siege<BR><BR>
     * @param clanId The int of player's clan id
     */
    public void removeSiegeClan(int clanId)
    {

        java.sql.Connection con = null;
        try
        {
            con = L2DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement;
            if ( clanId != 0 )
                statement = con.prepareStatement("DELETE FROM fortsiege_clans WHERE fort_id=? and clan_id=?");
            else
                statement = con.prepareStatement("DELETE FROM fortsiege_clans WHERE fort_id=?");
                
            statement.setInt(1, getFort().getFortId());
            if ( clanId != 0 ) 
                statement.setInt(2, clanId);
            statement.execute();
            statement.close();

            loadSiegeClan();
        }
        catch (Exception e)
        {
        }
        finally
        {
            try
            {
                con.close();
            }
            catch (Exception e)
            {
            }
        }
    }

    /**
     * Remove clan from siege<BR><BR>
     * @param player The L2PcInstance of player/clan being removed
     */
    public void removeSiegeClan(L2Clan clan)
    {
        if (clan == null || clan.getHasFort() == getFort().getFortId()
            || !FortSiegeManager.getInstance().checkIsRegistered(clan, getFort().getFortId())) return;
        removeSiegeClan(clan.getClanId());
    }

    /**
     * Remove clan from siege<BR><BR>
     * @param player The L2PcInstance of player/clan being removed
     */
    public void removeSiegeClan(L2PcInstance player)
    {
        removeSiegeClan(player.getClan());
    }

    /**
     * Start the auto tasks<BR>
     * <BR>
     */
    public void checkAutoTask()
    {
        if (getFort().getSiegeDate().getTimeInMillis() < Calendar.getInstance().getTimeInMillis())
        {
            clearSiegeDate();
            saveSiegeDate();
            removeSiegeClan(0); // remove all clans
            return;
        }
        
        startAutoTask(false);
    }
    
    /**
     * Start the auto tasks<BR><BR>
     */
    public void startAutoTask(boolean setTime)
    {
        if (setTime) setSiegeDateTime();
        System.out.println("Siege of " + getFort().getName() + ": " + getFort().getSiegeDate().getTime());
        setIsScheduled(true);
        loadSiegeClan();
        // Schedule registration end
        _siegeRegistrationEndDate = Calendar.getInstance();
        _siegeRegistrationEndDate.setTimeInMillis(getFort().getSiegeDate().getTimeInMillis());
        _siegeRegistrationEndDate.add(Calendar.MINUTE, -10);
        // Schedule siege auto start
        ThreadPoolManager.getInstance().scheduleGeneral(new FortSiege.ScheduleStartSiegeTask(getFort()), 1000);
    }

    /**
     * Teleport players
     */
    public void teleportPlayer(TeleportWhoType teleportWho,
                               MapRegionTable.TeleportWhereType teleportWhere)
    {
        List<L2PcInstance> players;
        switch (teleportWho)
        {
            case Owner:
                players = getOwnersInZone();
                break;
            case Attacker:
                players = getAttackersInZone();
                break;
            case DefenderNotOwner:
                players = getDefendersButNotOwnersInZone();
                break;
            case Spectator:
                players = getSpectatorsInZone();
                break;
            default:
                players = getPlayersInZone();
        }
        ;

        for (L2PcInstance player : players)
        {
            if (player.isGM() || player.isInJail()) continue;
            player.teleToLocation(teleportWhere);
        }
    }

    // =========================================================
    // Method - Private
    /**
     * Add clan as attacker<BR><BR>
     * @param clanId The int of clan's id
     */
    private void addAttacker(int clanId)
    {
        getAttackerClans().add(new L2SiegeClan(clanId, SiegeClanType.ATTACKER)); // Add registered attacker to attacker list
    }

    /**
     * Add clan as defender<BR><BR>
     * @param clanId The int of clan's id
     */
    private void addDefender(int clanId)
    {
        getDefenderClans().add(new L2SiegeClan(clanId, SiegeClanType.DEFENDER)); // Add registered defender to defender list
    }

    /**
     * <p>Add clan as defender with the specified type</p>
     * @param clanId The int of clan's id
     * @param type the type of the clan
     */
    private void addDefender(int clanId, SiegeClanType type)
    {
        getDefenderClans().add(new L2SiegeClan(clanId, type));
    }

    /**
     * Add clan as defender waiting approval<BR><BR>
     * @param clanId The int of clan's id
     */
    private void addDefenderWaiting(int clanId)
    {
        getDefenderWaitingClans().add(new L2SiegeClan(clanId, SiegeClanType.DEFENDER_PENDING)); // Add registered defender to defender list
    }

    /**
     * Return true if the player can register.<BR><BR>
     * @param player The L2PcInstance of the player trying to register
     */
    private boolean checkIfCanRegister(L2PcInstance player)
    {
        if (getIsRegistrationOver()) player.sendMessage("The deadline to register for the siege of "
            + getFort().getName() + " has passed.");
        else if (getIsInProgress()) player.sendMessage("This is not the time for siege registration and so registration and cancellation cannot be done.");
        else if (player.getClan() == null
            || player.getClan().getLevel() < FortSiegeManager.getInstance().getSiegeClanMinLevel()) player.sendMessage("Only clans with Level "
            + FortSiegeManager.getInstance().getSiegeClanMinLevel()
            + " and higher may register for a fort siege.");
        else if (player.getClan().getHasFort() > 0) player.sendMessage("You cannot register because your clan already own a fort.");
        else if (player.getClan().getClanId() == getFort().getOwnerId())
            player.sendPacket(new SystemMessage(SystemMessageId.CLAN_THAT_OWNS_CASTLE_IS_AUTOMATICALLY_REGISTERED_DEFENDING));
        else if (FortSiegeManager.getInstance().checkIsRegistered(player.getClan(), getFort().getFortId())) player.sendMessage("You are already registered in a Siege.");
        else return true;

        return false;
    }

    private void setSiegeDateTime()
    {
        Calendar newDate = Calendar.getInstance();
        newDate.add(Calendar.MINUTE, 60);
        getFort().setSiegeDate(newDate);
        saveSiegeDate();
    }
    
    /** Load siege clans. */
    private void loadSiegeClan()
    {
        java.sql.Connection con = null;
        try
        {
            getAttackerClans().clear();
            getDefenderClans().clear();
            getDefenderWaitingClans().clear();

            // Add fort owner as defender (add owner first so that they are on the top of the defender list)
            if (getFort().getOwnerId() > 0)
                addDefender(getFort().getOwnerId(), SiegeClanType.OWNER);

            PreparedStatement statement = null;
            ResultSet rs = null;

            con = L2DatabaseFactory.getInstance().getConnection();

            statement = con.prepareStatement("SELECT clan_id,type FROM fortsiege_clans where fort_id=?");
            statement.setInt(1, getFort().getFortId());
            rs = statement.executeQuery();

            int typeId;
            while (rs.next())
            {
                typeId = rs.getInt("type");
                if (typeId == 0) addDefender(rs.getInt("clan_id"));
                else if (typeId == 1) addAttacker(rs.getInt("clan_id"));
                else if (typeId == 2) addDefenderWaiting(rs.getInt("clan_id"));
            }

            rs.close();
            statement.close();
        }
        catch (Exception e)
        {
            _log.warning("Exception: loadSiegeClan(): " + e.getMessage());
            e.printStackTrace();
        }
        finally
        {
            try
            {
                con.close();
            }
            catch (Exception e)
            {
            }
        }
    }

    /** Remove artifacts spawned. */
    private void removeCommander()
    {
        if (_commanders != null)
        {
            // Remove all instance of artifact for this fort
            for (L2CommanderInstance commander : _commanders)
            {
                if (commander != null) commander.decayMe();
            }
            _commanders = null;
        }
    }

    /** Remove all flags. */
    private void removeFlags()
    {
        for (L2SiegeClan sc : getAttackerClans())
        {
            if (sc != null) sc.removeFlags();
        }
        for (L2SiegeClan sc : getDefenderClans())
        {
            if (sc != null) sc.removeFlags();
        }
    }

    /** Save fort siege related to database. */
    private void saveFortSiege()
    {
        clearSiegeDate(); // clear siege date
        saveSiegeDate(); // Save the new date
        setIsScheduled(false);
    }

    /** Save siege date to database. */
    private void saveSiegeDate()
    {
        java.sql.Connection con = null;
        try
        {
            con = L2DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement = con.prepareStatement("Update fort set siegeDate = ? where id = ?");
            statement.setLong(1, getSiegeDate().getTimeInMillis());
            statement.setInt(2, getFort().getFortId());
            statement.execute();

            statement.close();
        }
        catch (Exception e)
        {
            _log.warning("Exception: saveSiegeDate(): " + e.getMessage());
            e.printStackTrace();
        }
        finally
        {
            try
            {
                con.close();
            }
            catch (Exception e)
            {
            }
        }
    }

    /**
     * Save registration to database.<BR><BR>
     * @param clan The L2Clan of player
     * @param typeId -1 = owner 0 = defender, 1 = attacker, 2 = defender waiting
     */
    private void saveSiegeClan(L2Clan clan, int typeId, boolean isUpdateRegistration)
    {
        if (clan.getHasFort() > 0) return;

        java.sql.Connection con = null;
        try
        {
            if (typeId == 0 || typeId == 2 || typeId == -1)
            {
                if (getDefenderClans().size() + getDefenderWaitingClans().size() >= FortSiegeManager.getInstance().getDefenderMaxClans())
                    return;
            }
            else
            {
                if (getAttackerClans().size() >= FortSiegeManager.getInstance().getAttackerMaxClans())
                    return;
            }

            con = L2DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement;
            if (!isUpdateRegistration)
            {
                statement = con.prepareStatement("INSERT INTO fortsiege_clans (clan_id,fort_id,type,fort_owner) values (?,?,?,0)");
                statement.setInt(1, clan.getClanId());
                statement.setInt(2, getFort().getFortId());
                statement.setInt(3, typeId);
                statement.execute();
                statement.close();
            }
            else
            {
                statement = con.prepareStatement("Update fortsiege_clans set type = ? where fort_id = ? and clan_id = ?");
                statement.setInt(1, typeId);
                statement.setInt(2, getFort().getFortId());
                statement.setInt(3, clan.getClanId());
                statement.execute();
                statement.close();
            }

            if (typeId == 0 || typeId == -1)
            {
                addDefender(clan.getClanId());
                announceToPlayer(clan.getName() + " has been registered to defend "
                    + getFort().getName(), false);
            }
            else if (typeId == 1)
            {
                addAttacker(clan.getClanId());
                announceToPlayer(clan.getName() + " has been registered to attack "
                    + getFort().getName(), false);
            }
            else if (typeId == 2)
            {
                addDefenderWaiting(clan.getClanId());
                announceToPlayer(clan.getName() + " has requested to defend " + getFort().getName(),
                                 false);
            }
        }
        catch (Exception e)
        {
            _log.warning("Exception: saveSiegeClan(L2Clan clan, int typeId, boolean isUpdateRegistration): "
                + e.getMessage());
            e.printStackTrace();
        }
        finally
        {
            try
            {
                con.close();
            }
            catch (Exception e)
            {
            }
        }
    }

    /** Spawn artifact. */
    private void spawnCommander(int Id)
    {
        //Set commanders array size if one does not exist
        if (_commanders == null)
            _commanders = new FastList<L2CommanderInstance>();

        for (SiegeSpawn _sp: FortSiegeManager.getInstance().getCommanderSpawnList(Id))
        {
            L2CommanderInstance commander;

            commander = new L2CommanderInstance(IdFactory.getInstance().getNextId(), NpcTable.getInstance().getTemplate(_sp.getNpcId()));
            commander.setCurrentHpMp(commander.getMaxHp(), commander.getMaxMp());
            commander.setHeading(_sp.getLocation().getHeading());
            commander.spawnMe(_sp.getLocation().getX(),_sp.getLocation().getY(),_sp.getLocation().getZ() + 50);

            _commanders.add(commander);
        }
    }

    private void spawnFlag(int Id)
    {
        for (CombatFlag cf: FortSiegeManager.getInstance().getFlagList(Id))
        {
            cf.spawnMe();
        }

    }

    private void unSpawnFlags()
    {

        for (CombatFlag cf: FortSiegeManager.getInstance().getFlagList(getFort().getFortId()))
        {
            cf.unSpawnMe();
        }

    }

    /**
     * Spawn siege guard.<BR><BR>
     */
    private void spawnSiegeGuard()
    {
        getSiegeGuardManager().spawnSiegeGuard();

        // Register guard to the closest Control Tower
        // When CT dies, so do all the guards that it controls
//        if (getSiegeGuardManager().getSiegeGuardSpawn().size() > 0 && _controlTowers.size() > 0)
//        {
//            L2ControlTowerInstance closestCt;
//            double distance, x, y, z;
//            double distanceClosest = 0;
//            for (L2Spawn spawn : getSiegeGuardManager().getSiegeGuardSpawn())
//            {
//                if (spawn == null) continue;
//                closestCt = null;
//                distanceClosest = 0;
//                for (L2ControlTowerInstance ct : _controlTowers)
//                {
//                    if (ct == null) continue;
//                    x = (spawn.getLocx() - ct.getX());
//                    y = (spawn.getLocy() - ct.getY());
//                    z = (spawn.getLocz() - ct.getZ());
//
//                    distance = (x * x) + (y * y) + (z * z);
//
//                    if (closestCt == null || distance < distanceClosest)
//                    {
//                        closestCt = ct;
//                        distanceClosest = distance;
//                    }
//                }
//
//                if (closestCt != null) closestCt.registerGuard(spawn);
//            }
//        }
    }

    public final L2SiegeClan getAttackerClan(L2Clan clan)
    {
        if (clan == null) return null;
        return getAttackerClan(clan.getClanId());
    }

    public final L2SiegeClan getAttackerClan(int clanId)
    {
        for (L2SiegeClan sc : getAttackerClans())
            if (sc != null && sc.getClanId() == clanId) return sc;
        return null;
    }

    public final List<L2SiegeClan> getAttackerClans()
    {
        if (_isNormalSide) return _attackerClans;
        return _defenderClans;
    }

    public final int getAttackerRespawnDelay()
    {
        return (FortSiegeManager.getInstance().getAttackerRespawnDelay());
    }

    public final Fort getFort()
    {
        if (_fort == null || _fort.length <= 0) return null;
        return _fort[0];
    }

    public final L2SiegeClan getDefenderClan(L2Clan clan)
    {
        if (clan == null) return null;
        return getDefenderClan(clan.getClanId());
    }

    public final L2SiegeClan getDefenderClan(int clanId)
    {
        for (L2SiegeClan sc : getDefenderClans())
            if (sc != null && sc.getClanId() == clanId) return sc;
        return null;
    }

    public final List<L2SiegeClan> getDefenderClans()
    {
        if (_isNormalSide) return _defenderClans;
        return _attackerClans;
    }

    public final L2SiegeClan getDefenderWaitingClan(L2Clan clan)
    {
        if (clan == null) return null;
        return getDefenderWaitingClan(clan.getClanId());
    }

    public final L2SiegeClan getDefenderWaitingClan(int clanId)
    {
        for (L2SiegeClan sc : getDefenderWaitingClans())
            if (sc != null && sc.getClanId() == clanId) return sc;
        return null;
    }

    public final List<L2SiegeClan> getDefenderWaitingClans()
    {
        return _defenderWaitingClans;
    }

    public final int getDefenderRespawnDelay()
    {
        return (FortSiegeManager.getInstance().getDefenderRespawnDelay() + _defenderRespawnDelayPenalty);
    }

    public final boolean getIsInProgress()
    {
        return _isInProgress;
    }

    public final boolean getIsScheduled()
    {
        return _isScheduled;
    }

    public final void setIsScheduled(boolean isScheduled)
    {
        _isScheduled = isScheduled;
    }
    
    public final boolean getIsRegistrationOver()
    {
        return _isRegistrationOver;
    }

    public final Calendar getSiegeDate()
    {
        return getFort().getSiegeDate();
    }

    public List<L2NpcInstance> getFlag(L2Clan clan)
    {
        if (clan != null)
        {
            L2SiegeClan sc = getAttackerClan(clan);
            if (sc != null) return sc.getFlag();
        }
        return null;
    }

    public final FortSiegeGuardManager getSiegeGuardManager()
    {
        if (_siegeGuardManager == null)
        {
            _siegeGuardManager = new FortSiegeGuardManager(getFort());
        }
        return _siegeGuardManager;
    }
}