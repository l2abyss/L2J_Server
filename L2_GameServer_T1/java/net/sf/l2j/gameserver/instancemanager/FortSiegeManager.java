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
package net.sf.l2j.gameserver.instancemanager;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;
import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.CombatFlag;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.Location;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.Fort;
import net.sf.l2j.gameserver.model.entity.FortSiege;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;

public class FortSiegeManager
{
    private static final Logger _log = Logger.getLogger(FortSiegeManager.class.getName());

    // =========================================================
    private static FortSiegeManager _instance;
    public static final FortSiegeManager getInstance()
    {
        if (_instance == null)
        {
            _log.info("Initializing SiegeManager");
            _instance = new FortSiegeManager();
            _instance.load();
        }
        return _instance;
    }

    // =========================================================
    // Data Field
    private int _attackerMaxClans                              = 500; // Max number of clans
    private int _attackerRespawnDelay                          = 20000; // Time in ms. Changeable in siege.config
    private int _defenderMaxClans                              = 500; // Max number of clans
    private int _defenderRespawnDelay                          = 10000; // Time in ms. Changeable in siege.config

    // Fort Siege settings
    private FastMap<Integer,FastList<SiegeSpawn>>  _commanderSpawnList;
    private FastMap<Integer,FastList<CombatFlag>>  _flagList;

    private int _controlTowerLosePenalty                         = 20000; // Time in ms. Changeable in siege.config
    private int _flagMaxCount                                   = 1; // Changeable in siege.config
    private int _siegeClanMinLevel                             = 4; // Changeable in siege.config
    private int _siegeLength                                    = 120; // Time in minute. Changeable in siege.config
    private List<FortSiege> _sieges;

    // =========================================================
    // Constructor
    private FortSiegeManager()
    {
    }

    // =========================================================
    // Method - Public
    public final void addSiegeSkills(L2PcInstance character)
    {
        character.addSkill(SkillTable.getInstance().getInfo(246, 1), false);
        character.addSkill(SkillTable.getInstance().getInfo(247, 1), false);
    }

    /**
     * Return true if character summon<BR><BR>
     * @param activeChar The L2Character of the character can summon
     */
    public final boolean checkIfOkToSummon(L2Character activeChar, boolean isCheckOnly)
    {
        if (activeChar == null || !(activeChar instanceof L2PcInstance)) return false;

        SystemMessage sm = new SystemMessage(SystemMessageId.S1_S2);
        L2PcInstance player = (L2PcInstance)activeChar;
        Fort fort = FortManager.getInstance().getFort(player);

        if (fort == null || fort.getFortId() <= 0)
            sm.addString("You must be on fort ground to summon this");
        else if (!fort.getSiege().getIsInProgress())
            sm.addString("You can only summon this during a siege.");
        else if (player.getClanId() != 0 && fort.getSiege().getAttackerClan(player.getClanId()) == null)
            sm.addString("You can only summon this as a registered attacker.");
        else
            return true;

        if (!isCheckOnly) {player.sendPacket(sm);}
        return false;
    }

    /**
     * Return true if the clan is registered or owner of a fort<BR><BR>
     * @param clan The L2Clan of the player
     */
    public final boolean checkIsRegistered(L2Clan clan, int fortid)
    {
        if (clan == null) return false;

        if (clan.getHasFort() > 0) return true;

        java.sql.Connection con = null;
        boolean register = false;
        try
        {
            con = L2DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement = con.prepareStatement("SELECT clan_id FROM fortsiege_clans where clan_id=? and fort_id=?");
            statement.setInt(1, clan.getClanId());
            statement.setInt(2, fortid);
            ResultSet rs = statement.executeQuery();

            while (rs.next())
            {
                register = true;
                break;
            }

            rs.close();
            statement.close();
        }
        catch (Exception e)
        {
            _log.warning("Exception: checkIsRegistered(): " + e.getMessage());
            e.printStackTrace();
        }
        finally
        {
            try { con.close(); } catch (Exception e) {}
        }
        return register;
    }

    public final void removeSiegeSkills(L2PcInstance character)
    {
        character.removeSkill(SkillTable.getInstance().getInfo(246, 1));
        character.removeSkill(SkillTable.getInstance().getInfo(247, 1));
    }

    // =========================================================
    // Method - Private
    private final void load()
    {
        try {
            InputStream is              = new FileInputStream(new File(Config.FORTSIEGE_CONFIGURATION_FILE));
            Properties siegeSettings    = new Properties();
            siegeSettings.load(is);
            is.close();

            // Siege setting
            _attackerMaxClans = Integer.decode(siegeSettings.getProperty("AttackerMaxClans", "500"));
            _attackerRespawnDelay = Integer.decode(siegeSettings.getProperty("AttackerRespawn", "30000"));
            _controlTowerLosePenalty = Integer.decode(siegeSettings.getProperty("CTLossPenalty", "20000"));
            _defenderMaxClans = Integer.decode(siegeSettings.getProperty("DefenderMaxClans", "500"));
            _defenderRespawnDelay = Integer.decode(siegeSettings.getProperty("DefenderRespawn", "20000"));
            _flagMaxCount = Integer.decode(siegeSettings.getProperty("MaxFlags", "1"));
            _siegeClanMinLevel = Integer.decode(siegeSettings.getProperty("SiegeClanMinLevel", "4"));
            _siegeLength = Integer.decode(siegeSettings.getProperty("SiegeLength", "120"));

            // Siege spawns settings
            _commanderSpawnList = new FastMap<Integer,FastList<SiegeSpawn>>();
            _flagList = new FastMap<Integer,FastList<CombatFlag>>();

            for (Fort fort: FortManager.getInstance().getForts())
            {
                FastList<SiegeSpawn> _commanderSpawns = new FastList<SiegeSpawn>();
                FastList<CombatFlag> _flagSpawns = new FastList<CombatFlag>();

                for (int i=1; i<5; i++)
                {
                    String _spawnParams = siegeSettings.getProperty(fort.getName() + "Commander" + Integer.toString(i), "");

                    if (_spawnParams.length() == 0) break;

                    StringTokenizer st = new StringTokenizer(_spawnParams.trim(), ",");

                    try
                    {
                        int x = Integer.parseInt(st.nextToken());
                        int y = Integer.parseInt(st.nextToken());
                        int z = Integer.parseInt(st.nextToken());
                        int heading = Integer.parseInt(st.nextToken());
                        int npc_id = Integer.parseInt(st.nextToken());

                        _commanderSpawns.add(new SiegeSpawn(fort.getFortId(),x,y,z,heading,npc_id));
                    }
                    catch (Exception e)
                    {
                        _log.warning("Error while loading commander(s) for "+fort.getName()+" fort.");
                    }
                }

                _commanderSpawnList.put(fort.getFortId(), _commanderSpawns);
                
                for (int i=1; i<4; i++)
                {
                    String _spawnParams = siegeSettings.getProperty(fort.getName() + "Flag" + Integer.toString(i), "");

                    if (_spawnParams.length() == 0) break;

                    StringTokenizer st = new StringTokenizer(_spawnParams.trim(), ",");

                    try
                    {
                        int x = Integer.parseInt(st.nextToken());
                        int y = Integer.parseInt(st.nextToken());
                        int z = Integer.parseInt(st.nextToken());
                        int flag_id = Integer.parseInt(st.nextToken());

                        _flagSpawns.add(new CombatFlag(fort.getFortId(),x,y,z,0,flag_id));
                    }
                    catch (Exception e)
                    {
                        _log.warning("Error while loading flag(s) for "+fort.getName()+" fort.");
                    }
                }
                _flagList.put(fort.getFortId(), _flagSpawns);
                
            }

        } catch (Exception e) {
            //_initialized = false;
            System.err.println("Error while loading fortsiege data.");
            e.printStackTrace();
        }
    }

    // =========================================================
    // Property - Public
    public final FastList<SiegeSpawn> getCommanderSpawnList(int _fortId)
    {
        if (_commanderSpawnList.containsKey(_fortId))
            return _commanderSpawnList.get(_fortId);
        else
            return null;
    }

    public final FastList<CombatFlag> getFlagList(int _fortId)
    {
        if (_flagList.containsKey(_fortId))
            return _flagList.get(_fortId);
        else
            return null;
    }
    
    public final int getAttackerMaxClans() { return _attackerMaxClans; }

    public final int getAttackerRespawnDelay() { return _attackerRespawnDelay; }

    public final int getControlTowerLosePenalty() { return _controlTowerLosePenalty; }

    public final int getDefenderMaxClans() { return _defenderMaxClans; }

    public final int getDefenderRespawnDelay() { return (_defenderRespawnDelay); }

    public final int getFlagMaxCount() { return _flagMaxCount; }

    public final FortSiege getSiege(L2Object activeObject) { return getSiege(activeObject.getX(), activeObject.getY(), activeObject.getZ()); }

    public final FortSiege getSiege(int x, int y, int z)
    {
        for (Fort fort: FortManager.getInstance().getForts())
            if (fort.getSiege().checkIfInZone(x, y, z)) return fort.getSiege();
        return null;
    }

    public final int getSiegeClanMinLevel() { return _siegeClanMinLevel; }

    public final int getSiegeLength() { return _siegeLength; }

    public final List<FortSiege> getSieges()
    {
        if (_sieges == null) _sieges = new FastList<FortSiege>();
        return _sieges;
    }

    public final void addSiege(FortSiege fortSiege)
    {
    if (_sieges == null)
        _sieges = new FastList<FortSiege>();
    _sieges.add(fortSiege);
    }

    public final void removeSiege(FortSiege fortSiege)
    {
    if (_sieges == null)
        _sieges = new FastList<FortSiege>();
    _sieges.remove(fortSiege);
    }
    
    public boolean isCombat(int itemId)
    {
        return ( itemId == 9819);
    }
    
    public void activateCombatFlag(L2PcInstance player, L2ItemInstance item)
    {
        if (!checkIfCanPickup(player))
            return;
        
        Fort fort = FortManager.getInstance().getFort(player);
        
        FastList<CombatFlag> fcf =  _flagList.get(fort.getFortId());
        for ( CombatFlag cf : fcf)
        {
            if ( cf.itemInstance == item)
            {
                cf.activate(player, item);
            }
        }
    }
    
    public boolean checkIfCanPickup(L2PcInstance player)
    {
        // Cannot own 2 combat flag
        if (player.isCombatFlagEquipped()) 
        {
            player.sendMessage("You already have the combat flag");
            return false;
        }
       
        // here check if is siege is in progress
        // here check if is siege is attacker
        Fort fort = FortManager.getInstance().getFort(player);
        
        if (fort == null || fort.getFortId() <= 0)
        {
            player.sendMessage("You must be on fort ground to pickup Combat Flag");
            return false;
        }
        else if (!fort.getSiege().getIsInProgress())
        {
            player.sendMessage("You can only pickup Combat Flag during a siege.");
            return false;
        }
        else if (fort.getSiege().getAttackerClan(player.getClan()) == null)
        {
            player.sendMessage("You must be an attacker to pickup Combat Flag");
            return false;
        }
      
        return true;
    }
    
    public void dropCombatFlag(L2PcInstance player)
    {
        System.out.println("Player obj " + player.getObjectId());
        Fort fort = FortManager.getInstance().getFort(player);
        System.out.println("Fort " + fort.getName());
        
        
        FastList<CombatFlag> fcf =  _flagList.get(fort.getFortId());
        System.out.println("fast list size " + fcf.size());

        for ( CombatFlag cf : fcf)
        {
            if ( cf.playerId == player.getObjectId())
            {
                System.out.println("found cf ");
                cf.dropIt();
                cf.spawnMe();
            }
        }
        
    }
    
    public class  SiegeSpawn
    {
        Location _location;
        private int _npcId;
        private int _heading;
        private int _fortId;
        private int _hp;

        public SiegeSpawn(int fort_id, int x, int y, int z, int heading, int npc_id)
        {
            _fortId = fort_id;
            _location = new Location(x,y,z,heading);
            _heading = heading;
            _npcId = npc_id;
        }

        public SiegeSpawn(int fort_id, int x, int y, int z, int heading, int npc_id, int hp)
        {
            _fortId = fort_id;
            _location = new Location(x,y,z,heading);
            _heading = heading;
            _npcId = npc_id;
            _hp = hp;
        }

        public int getFortId()
        {
            return _fortId;
        }

        public int getNpcId()
        {
            return _npcId;
        }

        public int getHeading()
        {
            return _heading;
        }

        public int getHp()
        {
            return _hp;
        }

        public Location getLocation()
        {
            return _location;
        }
    }
}
