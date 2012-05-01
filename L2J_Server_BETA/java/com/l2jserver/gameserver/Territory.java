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
package com.l2jserver.gameserver;

import java.util.Map;
import java.util.logging.Logger;

import javolution.util.FastMap;

import com.l2jserver.gameserver.model.L2Territory;
import com.l2jserver.util.lib.SqlUtils;

/**
 * @author Balancer, Mr
 */
public class Territory
{
	private static Logger _log = Logger.getLogger(Territory.class.getName());
	
	private static final Map<Integer, L2Territory> _territory = new FastMap<>();
	
	public static Territory getInstance()
	{
		return SingletonHolder._instance;
	}
	
	protected Territory()
	{
		// load all data at server start
		reload_data();
	}
	
	public int[] getRandomPoint(int terr)
	{
		return _territory.get(terr).getRandomPoint();
	}
	
	public int getProcMax(int terr)
	{
		return _territory.get(terr).getProcMax();
	}
	
	public void reload_data()
	{
		_territory.clear();
		Integer[][] point = SqlUtils.get2DIntArray(new String[] { "loc_id", "loc_x", "loc_y", "loc_zmin", "loc_zmax", "proc" }, "locations", "loc_id > 0");
		for (Integer[] row : point)
		{
			//			_log.info("row = "+row[0]);
			Integer terr = row[0];
			if (terr == null)
			{
				_log.warning("Null territory!");
				continue;
			}
			
			if (_territory.get(terr) == null)
			{
				L2Territory t = new L2Territory(terr);
				_territory.put(terr, t);
			}
			_territory.get(terr).add(row[1], row[2], row[3], row[4], row[5]);
		}
	}
	
	private static class SingletonHolder
	{
		protected static final Territory _instance = new Territory();
	}
}
