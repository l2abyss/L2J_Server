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
package com.l2jserver.gameserver.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javolution.util.FastMap;

import com.l2jserver.Config;
import com.l2jserver.gameserver.model.base.Race;
import com.l2jserver.util.Rnd;

/**
 * @author Nyaran
 */
public class L2MapRegion
{
	private String _name = null;
	private String _town = null;
	private int _locId = -1;
	private int _castle = -1;
	private int _bbs = -1;
	private List<int[]> _maps = null;
	
	private List<Location> _spawnLocs = null;
	private List<Location> _otherSpawnLocs = null;
	private List<Location> _chaoticSpawnLocs = null;
	private List<Location> _banishSpawnLocs = null;
	
	private Map<Race, String> _bannedRace = new FastMap<Race, String>();
	
	public L2MapRegion(String name, String town, int locId, int castle, int bbs)
	{
		_name = name;
		_town = town;
		_locId = locId;
		_castle = castle;
		_bbs = bbs;
	}
	
	public final String getName()
	{
		return _name;
	}
	
	public final String getTown()
	{
		return _town;
	}
	
	public final int getLocId()
	{
		return _locId;
	}
	
	public final int getCastle()
	{
		return _castle;
	}
	
	public final int getBbs()
	{
		return _bbs;
	}
	
	public final void addMap(int x, int y)
	{
		if (_maps == null)
			_maps = new ArrayList<int[]>();
		
		_maps.add(new int[] { x, y });
	}
	
	public final List<int[]> getMaps()
	{
		return _maps;
	}
	
	public final boolean isZoneInRegion(int x, int y)
	{
		if (_maps == null)
			return false;
		
		for (int[] map : _maps)
		{
			if (map[0] == x && map[1] == y)
				return true;
		}
		return false;
	}
	
	// Respawn
	public final void addSpawn(int x, int y, int z)
	{
		if (_spawnLocs == null)
			_spawnLocs = new ArrayList<Location>();
		
		_spawnLocs.add(new Location(x, y, z));
	}
	
	public final void addOtherSpawn(int x, int y, int z)
	{
		if (_otherSpawnLocs == null)
			_otherSpawnLocs = new ArrayList<Location>();
		
		_otherSpawnLocs.add(new Location(x, y, z));
	}
	
	public final void addChaoticSpawn(int x, int y, int z)
	{
		if (_chaoticSpawnLocs == null)
			_chaoticSpawnLocs = new ArrayList<Location>();
		
		_chaoticSpawnLocs.add(new Location(x, y, z));
	}
	
	public final void addBanishSpawn(int x, int y, int z)
	{
		if (_banishSpawnLocs == null)
			_banishSpawnLocs = new ArrayList<Location>();
		
		_banishSpawnLocs.add(new Location(x, y, z));
	}
	
	public final List<Location> getSpawns()
	{
		return _spawnLocs;
	}
	
	public final Location getSpawnLoc()
	{
		if (Config.RANDOM_RESPAWN_IN_TOWN_ENABLED)
			return _spawnLocs.get(Rnd.get(_spawnLocs.size()));
		else
			return _spawnLocs.get(0);
	}
	
	public final Location getOtherSpawnLoc()
	{
		if (_otherSpawnLocs != null)
		{
			if (Config.RANDOM_RESPAWN_IN_TOWN_ENABLED)
				return _otherSpawnLocs.get(Rnd.get(_otherSpawnLocs.size()));
			else
				return _otherSpawnLocs.get(0);
		}
		else
			return getSpawnLoc();
	}
	
	public final Location getChaoticSpawnLoc()
	{
		if (_chaoticSpawnLocs != null)
		{
			if (Config.RANDOM_RESPAWN_IN_TOWN_ENABLED)
				return _chaoticSpawnLocs.get(Rnd.get(_chaoticSpawnLocs.size()));
			else
				return _chaoticSpawnLocs.get(0);
		}
		else
			return getSpawnLoc();
	}
	
	public final Location getBanishSpawnLoc()
	{
		if (_banishSpawnLocs != null)
		{
			if (Config.RANDOM_RESPAWN_IN_TOWN_ENABLED)
				return _banishSpawnLocs.get(Rnd.get(_banishSpawnLocs.size()));
			else
				return _banishSpawnLocs.get(0);
		}
		else
			return getSpawnLoc();
	}
	
	public final void addBannedRace(String race, String point)
	{
		_bannedRace.put(Race.valueOf(race), point);
	}
	
	public final Map<Race, String> getBannedRace()
	{
		return _bannedRace;
	}
}
