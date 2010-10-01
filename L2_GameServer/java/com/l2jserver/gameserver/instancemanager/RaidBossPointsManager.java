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
package com.l2jserver.gameserver.instancemanager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastMap;

import com.l2jserver.L2DatabaseFactory;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author Kerberos
 * JIV update 24.8.10
 * 
 */

public class RaidBossPointsManager
{
	private final static Logger _log = Logger.getLogger(RaidBossPointsManager.class.getName());
	
	private FastMap<Integer, Map<Integer, Integer>> _list;
	
	private final Comparator<Map.Entry<Integer, Integer>> _comparator = new Comparator<Map.Entry<Integer, Integer>>(){
		public int compare(Map.Entry<Integer, Integer> entry, Map.Entry<Integer, Integer> entry1)
		{
			return entry.getValue().equals(entry1.getValue()) ? 0 : entry.getValue() < entry1.getValue() ? 1 : -1;
		}
	};
	
	public static final RaidBossPointsManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	public RaidBossPointsManager()
	{
		init();
	}
	
	private final void init()
	{
		_list = new FastMap<Integer, Map<Integer, Integer>>();
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT `charId`,`boss_id`,`points` FROM `character_raid_points`");
			ResultSet rset = statement.executeQuery();
			while(rset.next())
			{
				int charId = rset.getInt("charId");
				int bossId = rset.getInt("boss_id");
				int points = rset.getInt("points");
				Map<Integer, Integer> values = _list.get(charId);
				if (values == null)
				{
					values = new FastMap<Integer, Integer>();
				}
				values.put(bossId, points);
				_list.put(charId, values);
			}
			rset.close();
			statement.close();
			_log.info(getClass().getSimpleName()+": Loaded "+_list.size()+" Characters Raid Points.");
		}
		catch (SQLException e)
		{
			_log.log(Level.WARNING, "RaidPointsManager: Couldnt load raid points ", e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
	}
	
	public final void updatePointsInDB(L2PcInstance player, int raidId, int points)
	{
		Connection con = null;
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
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "could not update char raid points:", e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
	}
	
	public final void addPoints(L2PcInstance player, int bossId, int points)
	{
		int ownerId = player.getObjectId();
		Map<Integer, Integer> tmpPoint = _list.get(ownerId);
		if(tmpPoint == null)
		{
			tmpPoint = new FastMap<Integer, Integer>();
			tmpPoint.put(bossId, points);
			updatePointsInDB(player, bossId, points);
		}
		else
		{
			int currentPoins = tmpPoint.containsKey(bossId) ? tmpPoint.get(bossId) : 0;
			currentPoins += points;
			tmpPoint.put(bossId, currentPoins);
			updatePointsInDB(player, bossId, currentPoins);
		}
		_list.put(ownerId, tmpPoint);
	}
	
	public final int getPointsByOwnerId(int ownerId)
	{
		Map<Integer, Integer> tmpPoint;
		tmpPoint = _list.get(ownerId);
		int totalPoints = 0;
		
		if (tmpPoint == null || tmpPoint.isEmpty())
			return 0;
		
		for(int points : tmpPoint.values())
		{
			totalPoints += points;
		}
		return totalPoints;
	}
	
	public final Map<Integer, Integer> getList(L2PcInstance player)
	{
		return _list.get(player.getObjectId());
	}
	
	public final void cleanUp()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			statement = con.prepareStatement("DELETE from character_raid_points WHERE charId > 0");
			statement.executeUpdate();
			statement.close();
			_list.clear();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "could not clean raid points: ", e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
	}
	
	public final int calculateRanking(int playerObjId)
	{
		Map<Integer, Integer> rank = getRankList();
		if (rank.containsKey(playerObjId))
			return rank.get(playerObjId);
		return 0;
	}
	
	public Map<Integer, Integer> getRankList()
	{
		Map<Integer, Integer> tmpRanking = new FastMap<Integer, Integer>();
		Map<Integer, Integer> tmpPoints = new FastMap<Integer, Integer>();
		
		for(int ownerId : _list.keySet())
		{
			int totalPoints = getPointsByOwnerId(ownerId);
			if(totalPoints != 0)
			{
				tmpPoints.put(ownerId, totalPoints);
			}
		}
		ArrayList<Entry<Integer, Integer>> list = new ArrayList<Map.Entry<Integer, Integer>>(tmpPoints.entrySet());
		
		Collections.sort(list, _comparator);
		
		int ranking = 1;
		for(Map.Entry<Integer, Integer> entry : list)
			tmpRanking.put(entry.getKey(), ranking++);
		
		return tmpRanking;
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final RaidBossPointsManager _instance = new RaidBossPointsManager();
	}
}