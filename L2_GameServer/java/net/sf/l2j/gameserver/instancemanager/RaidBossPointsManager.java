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
import java.sql.SQLException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
  * @author Kerberos
  */

public class RaidBossPointsManager
{
	private final static Logger _log = Logger.getLogger(RaidBossPointsManager.class.getName());
	protected static FastMap<Integer, Map<Integer, Integer>> _points;
	protected static FastMap<Integer, Map<Integer, Integer>> _list;

	public final static void init()
	{
		_list = new FastMap<Integer, Map<Integer, Integer>>();
		FastList<Integer> _chars = new FastList<Integer>();
		java.sql.Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT * FROM `character_raid_points`");
			ResultSet rset = statement.executeQuery();
			while(rset.next())
			{
				_chars.add(rset.getInt("charId"));
			}
			rset.close();
			statement.close();
			for(FastList.Node<Integer> n = _chars.head(), end = _chars.tail(); (n = n.getNext()) != end;)
			{
				int charId = n.getValue();
				FastMap<Integer, Integer> values = new FastMap<Integer, Integer>();
				statement = con.prepareStatement("SELECT * FROM `character_raid_points` WHERE `charId`=?");
				statement.setInt(1, charId);
				rset = statement.executeQuery();
				while(rset.next())
				{
					values.put(rset.getInt("boss_id"), rset.getInt("points"));
				}
				rset.close();
				statement.close();
				_list.put(charId, values);
			}
		}
		catch (SQLException e)
		{
			_log.warning("RaidPointsManager: Couldnt load raid points ");
		}
		catch (Exception e)
		{
			_log.warning(e.getMessage());
		}
		finally
		{
			try
			{
				con.close();
			}
			catch(Exception e)
			{
				_log.warning(e.getMessage());
			}
		}
	}
	public final static void loadPoints(L2PcInstance player)
	{
		if (_points == null)
			_points = new FastMap<Integer, Map<Integer, Integer>>();
		java.sql.Connection con = null;

		try
		{
			FastMap<Integer, Integer> tmpScore = new FastMap<Integer, Integer>();
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			
			statement = con.prepareStatement("SELECT boss_id,points FROM character_raid_points WHERE charId=?");
			statement.setInt(1, player.getObjectId());
			ResultSet rs = statement.executeQuery();
			while (rs.next())
			{
				int raidId = rs.getInt("boss_id");
				int points = rs.getInt("points");
				tmpScore.put(raidId, points);
			}
			rs.close();
			statement.close();
			_points.put(player.getObjectId(), tmpScore);

		}
		catch (SQLException e)
		{
			_log.warning("RaidPointsManager: Couldnt load raid points for character :" +player.getName());
		}
		catch (Exception e)
		{
			_log.warning(e.getMessage());
		}
		finally
		{
			try
			{
				con.close();
			}
			catch(Exception e)
			{
				_log.warning(e.getMessage());
			}
		}
	}

    public final static void updatePointsInDB(L2PcInstance player, int raidId, int points)
    {
        java.sql.Connection con = null;
        try
        {
            con = L2DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement;
            statement = con.prepareStatement("REPLACE INTO character_raid_points (`charId`,`boss_id`,`points`) VALUES (?,?,?)");
            statement.setInt(1, player.getObjectId());
            statement.setInt(2, raidId);
            statement.setInt(3, points);
			statement.executeUpdate();
            statement.close();
        } catch (Exception e) {
			_log.log(Level.WARNING, "could not update char raid points:", e);
        } finally {
            try { con.close(); } catch (Exception e) {}
        }
	}

    public final static void addPoints(L2PcInstance player, int bossId, int points)
    {
    	int ownerId = player.getObjectId();
    	Map<Integer, Integer> tmpPoint = new FastMap<Integer, Integer>();
		if (_points == null)
			_points = new FastMap<Integer, Map<Integer, Integer>>();
    	tmpPoint = _points.get(ownerId);
    	if(tmpPoint == null || tmpPoint.isEmpty())
    	{
    		tmpPoint = new FastMap<Integer, Integer>();
    		tmpPoint.put(bossId, points);
    		updatePointsInDB(player, bossId, points);
    	}
    	else
    	{
    		int currentPoins = tmpPoint.containsKey(bossId) ? tmpPoint.get(bossId).intValue() : 0;
    		tmpPoint.remove(bossId);
    		tmpPoint.put(bossId, currentPoins == 0 ? points : currentPoins + points);
    		updatePointsInDB(player, bossId, currentPoins == 0 ? points : currentPoins + points);
    	}
    	_points.remove(ownerId);
    	_points.put(ownerId, tmpPoint);
    	_list.remove(ownerId);
    	_list.put(ownerId, tmpPoint);
    }

	public final static int getPointsByOwnerId(int ownerId)
	{
		Map<Integer, Integer> tmpPoint = new FastMap<Integer, Integer>();
		if (_points == null)
			_points = new FastMap<Integer, Map<Integer, Integer>>();
		tmpPoint = _points.get(ownerId);
		int totalPoints = 0;
		
		if (tmpPoint == null || tmpPoint.isEmpty())
			return 0;
		
		for(int bossId : tmpPoint.keySet())
		{
			totalPoints += tmpPoint.get(bossId);
		}
		return totalPoints;
	}

	public final static Map<Integer, Integer> getList(L2PcInstance player)
	{
		return _list.get(player.getObjectId());
	}

	public final static void cleanUp()
	{
		java.sql.Connection con = null;
        try
        {
            con = L2DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement;
            statement = con.prepareStatement("DELETE from character_raid_points WHERE charId > 0");
			statement.executeUpdate();
            statement.close();
            _points.clear();
            _points = new FastMap<Integer, Map<Integer, Integer>>();
            _list.clear();
            _list = new FastMap<Integer, Map<Integer, Integer>>();
        } catch (Exception e) {
			_log.log(Level.WARNING, "could not clean raid points: ", e);
        } finally {
            try { con.close(); } catch (Exception e) {}
        }
	}

	public final static int calculateRanking(L2PcInstance player)
	{
		Map<Integer, Integer> tmpRanking = new FastMap<Integer, Integer>();
		Map<Integer, Map<Integer, Integer>> tmpPoints = new FastMap<Integer, Map<Integer, Integer>>();
		int totalPoints;
		
		for(int ownerId : _list.keySet())
		{
			totalPoints = getPointsByOwnerId(ownerId);
			if(totalPoints != 0)
			{
				tmpRanking.put(ownerId, totalPoints);
			}
		}
		Vector<Entry<Integer, Integer>> list = new Vector<Map.Entry<Integer, Integer>>(tmpRanking.entrySet());
		
		Collections.sort(list, new Comparator<Map.Entry<Integer, Integer>>(){
			public int compare(Map.Entry<Integer, Integer> entry, Map.Entry<Integer, Integer> entry1)
			{
				return entry.getValue().equals(entry1.getValue()) ? 0 : entry.getValue() < entry1.getValue() ? 1 : -1;
			}
		});

		int ranking = 0;
		for(Map.Entry<Integer, Integer> entry : list)
		{
			Map<Integer, Integer> tmpPoint = new FastMap<Integer, Integer>();
			
			if (tmpPoints.get(entry.getKey()) != null)
				tmpPoint = tmpPoints.get(entry.getKey());
			
			tmpPoint.put(-1, ranking);
			
			tmpPoints.put(entry.getKey(), tmpPoint);
			
			ranking++;
		}
		Map<Integer, Integer> rank = tmpPoints.get(player.getObjectId());
		if (rank != null)
			return rank.get(-1);
		return 0;
	}
}