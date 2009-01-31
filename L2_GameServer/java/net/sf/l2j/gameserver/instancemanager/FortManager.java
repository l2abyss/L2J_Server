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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.datatables.SpawnTable;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.entity.Fort;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;

public class FortManager
{
	protected static final Logger _log = Logger.getLogger(FortManager.class.getName());
	// =========================================================
	private static FortManager _instance;
	protected FastMap<Integer, Integer> _envoyCastles = new FastMap<Integer, Integer>();
	protected FastMap<Integer, FastList<L2Spawn>> _npcCommanders = new FastMap<Integer, FastList<L2Spawn>>();
	protected FastMap<Integer, FastList<L2Spawn>> _siegeNpcs = new FastMap<Integer, FastList<L2Spawn>>();
	protected FastMap<Integer, FastList<L2Spawn>> _specialEnvoys = new FastMap<Integer, FastList<L2Spawn>>();
	protected FastList<L2Spawn> _npcCommandersSpawns;
	protected FastList<L2Spawn> _siegeNpcsSpawns;
	protected FastList<L2Spawn> _specialEnvoysSpawns;
	protected int _respawnTime;

	public static final FortManager getInstance()
    {
        if (_instance == null)
        {
            _log.info("Initializing FortManager");
            _instance = new FortManager();
            _instance.load();
        }
        return _instance;
    }
    
    // =========================================================
    // Data Field
    private Fort _fort;
    private List<Fort> _forts;
    
    // =========================================================
    // Constructor
    public FortManager() {}
	public FortManager(Fort fort)
	{
		_fort = fort;
		initNpcs(); // load and spawn npcs
		initSiegeNpcs(); // load suspicious merchants
		spawnSuspiciousMerchant();// spawn suspicious merchants
		initNpcCommanders(); // npc Commanders (not monsters)
		spawnNpcCommanders(); // spawn npc Commanders
		initSpecialEnvoys(); // envoys from castles
		if (_fort.getOwnerClan() != null && _fort.getFortState() == 0)
		{
			spawnSpecialEnvoys();
			ThreadPoolManager.getInstance().scheduleGeneral(_fort.new ScheduleSpecialEnvoysDeSpawn(_fort), 1*60*60*1000); // Prepare 1hr task for special envoys despawn
		}
	}
    // =========================================================
    // Method - Public

    public final int findNearestFortIndex(L2Object obj)
    {
        int index = getFortIndex(obj);
        if (index < 0)
        {
            double closestDistance = 99999999;
            double distance;
            Fort fort;
            for (int i = 0; i < getForts().size(); i++)
            {
                fort = getForts().get(i);
                if (fort == null) continue;
                distance = fort.getDistance(obj);
                if (closestDistance > distance)
                {
                    closestDistance = distance;
                    index = i;
                }
            }
        }
        return index;
    }
    
    // =========================================================
    // Method - Private
    private final void load()
    {
        java.sql.Connection con = null;
        try
        {
            PreparedStatement statement;
            ResultSet rs;

            con = L2DatabaseFactory.getInstance().getConnection();

            statement = con.prepareStatement("Select id from fort order by id");
            rs = statement.executeQuery();

            while (rs.next())
            {
                getForts().add(new Fort(rs.getInt("id")));
            }

            rs.close();
            statement.close();

            _log.info("Loaded: " + getForts().size() + " fortress");
    		for (Fort fort : getForts())
    		{
    			fort.getSiege().getSiegeGuardManager().loadSiegeGuard();
    		}
        }
        catch (Exception e)
        {
            _log.warning("Exception: loadFortData(): " + e.getMessage());
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
            	_log.warning(""+e.getMessage());
            	e.printStackTrace();
            }
        }
    }
    
    // =========================================================
    // Property - Public
    public final Fort getFortById(int fortId)
    {
        for (Fort f : getForts())
        {
            if (f.getFortId() == fortId)
                return f;
        }
        return null;
    }
    
    public final Fort getFortByOwner(L2Clan clan)
    {
        for (Fort f : getForts())
        {
            if (f.getOwnerClan() == clan)
                return f;
        }
        return null;
    }

    public final Fort getFort(String name)
    {
        for (Fort f : getForts())
        {
            if (f.getName().equalsIgnoreCase(name.trim()))
                return f;
        }
        return null;
    }
    
    public final Fort getFort(int x, int y, int z)
    {
        for (Fort f : getForts())
        {
            if (f.checkIfInZone(x, y, z))
                return f;
        }
        return null;
    }
    
    public final Fort getFort(L2Object activeObject) { return getFort(activeObject.getX(), activeObject.getY(), activeObject.getZ()); }
    
    public final int getFortIndex(int fortId)
    {
        Fort fort;
        for (int i = 0; i < getForts().size(); i++)
        {
            fort = getForts().get(i);
            if (fort != null && fort.getFortId() == fortId) return i;
        }
        return -1;
    }

    public final int getFortIndex(L2Object activeObject)
    {
        return getFortIndex(activeObject.getX(), activeObject.getY(), activeObject.getZ());
    }
    
    public final int getFortIndex(int x, int y, int z)
    {
        Fort fort;
        for (int i = 0; i < getForts().size(); i++)
        {
            fort = getForts().get(i);
            if (fort != null && fort.checkIfInZone(x, y, z)) return i;
        }
        return -1;
    }
    
    public final List<Fort> getForts()
    {
        if (_forts == null) _forts = new FastList<Fort>();
        return _forts;
    }
	public final Fort getFort()
	{
		return _fort;
	}
    
    private void initNpcs()
    {
    	Connection con = null;

    	try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT * FROM fort_spawnlist Where fortId = ? and spawnType = ? ");
			statement.setInt(1, getFort().getFortId());
			statement.setInt(2, 0);
			ResultSet rset = statement.executeQuery();
			
			L2Spawn spawnDat;
			L2NpcTemplate template1;
			
			while (rset.next())
			{
				template1 = NpcTable.getInstance().getTemplate(rset.getInt("npcId"));
				if (template1 != null)
				{
					spawnDat = new L2Spawn(template1);
					spawnDat.setAmount(1);
					spawnDat.setLocx(rset.getInt("x"));
					spawnDat.setLocy(rset.getInt("y"));
					spawnDat.setLocz(rset.getInt("z"));
					spawnDat.setHeading(rset.getInt("heading"));
					spawnDat.setRespawnDelay(60);
					SpawnTable.getInstance().addNewSpawn(spawnDat, false);
					spawnDat.doSpawn();
					spawnDat.startRespawn();
				}
				else
				{
					_log.warning("FortManager.initNpcs: Data missing in NPC table for ID: "
					        + rset.getInt("npcId") + ".");
				}
			}
			
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			// problem with initializing spawn, go to next one
			_log.warning("FortManager.initNpcs: Spawn could not be initialized: "+ e.getMessage());
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
            	_log.warning(""+e.getMessage());
            	e.printStackTrace();
			}
		}
    }
    private void initNpcCommanders()
    {
    	Connection con = null;
    	_npcCommanders.clear();
    	try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement1 = con.prepareStatement("SELECT Distinct fortId FROM fort_spawnlist Where spawnType = ? ORDER BY fortId");

			statement1.setInt(1, 1);
			ResultSet rset1 = statement1.executeQuery();

			while (rset1.next())
			{
				int fortId = rset1.getInt("fortId");
				PreparedStatement statement2 = con.prepareStatement("SELECT id, npcId, x, y, z, heading FROM fort_spawnlist Where fortId = ? and spawnType = ? ORDER BY id");
				statement2.setInt(1, getFort().getFortId());
				statement2.setInt(2, 1);
				ResultSet rset2 = statement2.executeQuery();
				
				L2Spawn spawnDat;
				L2NpcTemplate template1;
				_npcCommandersSpawns = new FastList<L2Spawn>();
				while (rset2.next())
				{
					template1 = NpcTable.getInstance().getTemplate(rset2.getInt("npcId"));
					if (template1 != null)
					{
						spawnDat = new L2Spawn(template1);
						spawnDat.setAmount(1);
						spawnDat.setLocx(rset2.getInt("x"));
						spawnDat.setLocy(rset2.getInt("y"));
						spawnDat.setLocz(rset2.getInt("z"));
						spawnDat.setHeading(rset2.getInt("heading"));
						spawnDat.setRespawnDelay(60);
						_npcCommandersSpawns.add(spawnDat);
					}
					else
					{
						_log.warning("FortManager.initNpcCommanders: Data missing in NPC table for ID: "
					        + rset2.getInt("npcId") + ".");
					}
				}
				rset2.close();
				statement2.close();
				_npcCommanders.put(fortId, _npcCommandersSpawns);
			}
			rset1.close();
			statement1.close();
		}
		catch (Exception e)
		{
			// problem with initializing spawn, go to next one
			_log.warning("FortManager.initNpcCommanders: Spawn could not be initialized: "
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
            	_log.warning(""+e.getMessage());
            	e.printStackTrace();
			}
		}
    }
    private void initSiegeNpcs()
    {
    	Connection con = null;
    	_siegeNpcs.clear();
    	try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement1 = con.prepareStatement("SELECT Distinct fortId FROM fort_spawnlist Where spawnType = ? ORDER BY fortId");

			statement1.setInt(1, 2);
			ResultSet rset1 = statement1.executeQuery();

			while (rset1.next())
			{
				int fortId = rset1.getInt("fortId");
				PreparedStatement statement2 = con.prepareStatement("SELECT id, npcId, x, y, z, heading FROM fort_spawnlist Where fortId = ? and spawnType = ? ORDER BY id");
				statement2.setInt(1, getFort().getFortId());
				statement2.setInt(2, 2);
				ResultSet rset2 = statement2.executeQuery();
				
				L2Spawn spawnDat;
				L2NpcTemplate template1;
				_siegeNpcsSpawns = new FastList<L2Spawn>();
				while (rset2.next())
				{
					template1 = NpcTable.getInstance().getTemplate(rset2.getInt("npcId"));
					if (template1 != null)
					{
						spawnDat = new L2Spawn(template1);
						spawnDat.setAmount(1);
						spawnDat.setLocx(rset2.getInt("x"));
						spawnDat.setLocy(rset2.getInt("y"));
						spawnDat.setLocz(rset2.getInt("z"));
						spawnDat.setHeading(rset2.getInt("heading"));
						spawnDat.setRespawnDelay(60);
						_siegeNpcsSpawns.add(spawnDat);
					}
					else
					{
						_log.warning("FortManager.initSiegeNpcs: Data missing in NPC table for ID: "
					        + rset2.getInt("npcId") + ".");
					}
				}
				rset2.close();
				statement2.close();
				_siegeNpcs.put(fortId, _siegeNpcsSpawns);
			}
			rset1.close();
			statement1.close();
		}
		catch (Exception e)
		{
			// problem with initializing spawn, go to next one
			_log.warning("FortManager.initSiegeNpcs: Spawn could not be initialized: "
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
            	_log.warning(""+e.getMessage());
            	e.printStackTrace();
			}
		}
    }
    private void initSpecialEnvoys()
    {
    	Connection con = null;
    	_specialEnvoys.clear();
    	_envoyCastles.clear();
    	try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement1 = con.prepareStatement("SELECT Distinct fortId FROM fort_spawnlist Where spawnType = ? ORDER BY fortId");

			statement1.setInt(1, 3);
			ResultSet rset1 = statement1.executeQuery();

			while (rset1.next())
			{
				int fortId = rset1.getInt("fortId");
				PreparedStatement statement2 = con.prepareStatement("SELECT id, npcId, x, y, z, heading, castleId FROM fort_spawnlist Where fortId = ? and spawnType = ? ORDER BY id");
				statement2.setInt(1, getFort().getFortId());
				statement2.setInt(2, 3);
				ResultSet rset2 = statement2.executeQuery();
				
				L2Spawn spawnDat;
				L2NpcTemplate template1;
				_specialEnvoysSpawns = new FastList<L2Spawn>();
				while (rset2.next())
				{
					int castleId = rset2.getInt("castleId");
					int npcId = rset2.getInt("npcId");
					template1 = NpcTable.getInstance().getTemplate(npcId);
					if (template1 != null)
					{
						spawnDat = new L2Spawn(template1);
						spawnDat.setAmount(1);
						spawnDat.setLocx(rset2.getInt("x"));
						spawnDat.setLocy(rset2.getInt("y"));
						spawnDat.setLocz(rset2.getInt("z"));
						spawnDat.setHeading(rset2.getInt("heading"));
						spawnDat.setRespawnDelay(60);
						_specialEnvoysSpawns.add(spawnDat);
						_envoyCastles.put(npcId, castleId);
					}
					else
					{
						_log.warning("FortManager.initSpecialEnvoys: Data missing in NPC table for ID: "
					        + rset2.getInt("npcId") + ".");
					}
				}
				rset2.close();
				statement2.close();
				_specialEnvoys.put(fortId, _specialEnvoysSpawns);
			}
			rset1.close();
			statement1.close();
		}
		catch (Exception e)
		{
			// problem with initializing spawn, go to next one
			_log.warning("FortManager.initSpecialEnvoys: Spawn could not be initialized: "
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
            	_log.warning(""+e.getMessage());
            	e.printStackTrace();
			}
		}
    }
    public void spawnNpcCommanders()
    {
    	FastList<L2Spawn> monsterList = _npcCommanders.get(getFort().getFortId());
    	if (monsterList != null)
		{
			for (L2Spawn spawnDat : monsterList)
			{
				spawnDat.doSpawn();
				spawnDat.startRespawn();
			}
		}
    }
    public void despawnNpcCommanders()
    {
    	FastList<L2Spawn> monsterList = _npcCommanders.get(getFort().getFortId());
    	if (monsterList != null)
		{
			for (L2Spawn spawnDat : monsterList)
			{
				spawnDat.stopRespawn();
				spawnDat.getLastSpawn().deleteMe();
			}
		}
    }
    public void spawnSuspiciousMerchant()
    {
    	FastList<L2Spawn> monsterList = _siegeNpcs.get(getFort().getFortId());
    	if (monsterList != null)
		{
			for (L2Spawn spawnDat : monsterList)
			{
				spawnDat.doSpawn();
				spawnDat.startRespawn();
			}
		}
    }
    public void despawnSuspiciousMerchant()
    {
    	FastList<L2Spawn> monsterList = _siegeNpcs.get(getFort().getFortId());
    	if (monsterList != null)
		{
			for (L2Spawn spawnDat : monsterList)
			{
				spawnDat.stopRespawn();
				spawnDat.getLastSpawn().deleteMe();
			}
		}
    }
    public void spawnSpecialEnvoys()
    {
    	FastList<L2Spawn> monsterList = _specialEnvoys.get(getFort().getFortId());
    	if (monsterList != null)
		{
			for (L2Spawn spawnDat : monsterList)
			{
				spawnDat.doSpawn();
				spawnDat.startRespawn();
			}
		}
    }
	public void despawnSpecialEnvoys()
	{
		FastList<L2Spawn> monsterList = _specialEnvoys.get(getFort().getFortId());
		if (monsterList != null)
		{
			for (L2Spawn spawnDat : monsterList)
			{
				spawnDat.stopRespawn();
				spawnDat.getLastSpawn().deleteMe();
			}
		}
	}
	public int getEnvoyCastle(int npcId)
	{
		return _envoyCastles.get(npcId);
	}
}
