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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.entity.Fort;

public class FortManager
{
	protected static final Logger _log = Logger.getLogger(FortManager.class.getName());
	// =========================================================
	private static FortManager _instance;
	public FastMap<Integer, Integer> _envoyCastles = new FastMap<Integer, Integer>();
	
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
    private List<Fort> _forts;
    
    // =========================================================
    // Constructor
    public FortManager()
    {
    	
    }

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
    
	public int getEnvoyCastle(int npcId)
	{
		return _envoyCastles.get(npcId);
	}
}
