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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastMap;

import com.l2jserver.gameserver.GameTimeController;
import com.l2jserver.gameserver.model.L2Spawn;
import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.model.actor.instance.L2RaidBossInstance;

/**
 *
 * @version $Revision: $ $Date: $
 * @author  godson
 */

public class DayNightSpawnManager
{
	
	private static Logger _log = Logger.getLogger(DayNightSpawnManager.class.getName());
	
	private List<L2Spawn> _dayCreatures;
	private List<L2Spawn> _nightCreatures;
	private Map<L2Spawn, L2RaidBossInstance> _bosses;
	
	//private static int _currentState;  // 0 = Day, 1 = Night
	
	public static DayNightSpawnManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private DayNightSpawnManager()
	{
		_dayCreatures = new ArrayList<L2Spawn>();
		_nightCreatures = new ArrayList<L2Spawn>();
		_bosses = new FastMap<L2Spawn, L2RaidBossInstance>();
		
		_log.info("DayNightSpawnManager: Day/Night handler initialized");
	}
	
	public void addDayCreature(L2Spawn spawnDat)
	{
		_dayCreatures.add(spawnDat);
	}
	
	public void addNightCreature(L2Spawn spawnDat)
	{
		_nightCreatures.add(spawnDat);
	}
	
	/**
	 * Spawn Day Creatures, and Unspawn Night Creatures
	 */
	public void spawnDayCreatures()
	{
		spawnCreatures(_nightCreatures, _dayCreatures, "night", "day");
	}
	
	/**
	 * Spawn Night Creatures, and Unspawn Day Creatures
	 */
	public void spawnNightCreatures()
	{
		spawnCreatures(_dayCreatures, _nightCreatures, "day", "night");
	}
	
	/**
	 * Manage Spawn/Respawn
	 * Arg 1 : List with spawns must be unspawned
	 * Arg 2 : List with spawns must be spawned
	 * Arg 3 : String for log info for unspawned L2NpcInstance
	 * Arg 4 : String for log info for spawned L2NpcInstance
	 */
	private void spawnCreatures(List<L2Spawn> unSpawnCreatures, List<L2Spawn> spawnCreatures, String UnspawnLogInfo,
			String SpawnLogInfo)
	{
		try
		{
			if (!unSpawnCreatures.isEmpty())
			{
				int i = 0;
				for (L2Spawn spawn: unSpawnCreatures)
				{
					if (spawn == null)
						continue;
					
					spawn.stopRespawn();
					L2Npc last = spawn.getLastSpawn();
					if (last != null)
					{
						last.deleteMe();
						i++;
					}
				}
				_log.info("DayNightSpawnManager: Removed " + i + " " + UnspawnLogInfo + " creatures");
			}
			
			int i = 0;
			for (L2Spawn spawnDat : spawnCreatures)
			{
				if (spawnDat == null)
					continue;
				spawnDat.startRespawn();
				spawnDat.doSpawn();
				i++;
			}
			
			_log.info("DayNightSpawnManager: Spawned " + i + " " + SpawnLogInfo + " creatures");
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Error while spawning creatures: " + e.getMessage(), e);
		}
	}
	
	private void changeMode(int mode)
	{
		if (_nightCreatures.isEmpty() && _dayCreatures.isEmpty())
			return;
		
		switch (mode)
		{
			case 0:
				spawnDayCreatures();
				specialNightBoss(0);
				break;
			case 1:
				spawnNightCreatures();
				specialNightBoss(1);
				break;
			default:
				_log.warning("DayNightSpawnManager: Wrong mode sent");
				break;
		}
	}
	
	public DayNightSpawnManager trim()
	{
		((ArrayList<?>)_nightCreatures).trimToSize();
		((ArrayList<?>)_dayCreatures).trimToSize();
		return this;
	}
	
	public void notifyChangeMode()
	{
		try
		{
			if (GameTimeController.getInstance().isNowNight())
				changeMode(1);
			else
				changeMode(0);
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Error while notifyChangeMode(): " + e.getMessage(), e);
		}
	}
	
	public void cleanUp()
	{
		_nightCreatures.clear();
		_dayCreatures.clear();
		_bosses.clear();
	}
	
	private void specialNightBoss(int mode)
	{
		try
		{
			for (L2Spawn spawn : _bosses.keySet())
			{
				L2RaidBossInstance boss = _bosses.get(spawn);
				
				if (boss == null && mode == 1)
				{
					boss = (L2RaidBossInstance) spawn.doSpawn();
					RaidBossSpawnManager.getInstance().notifySpawnNightBoss(boss);
					_bosses.remove(spawn);
					_bosses.put(spawn, boss);
					continue;
				}
				
				if (boss == null && mode == 0)
					continue;
				
				if (boss.getNpcId() == 25328 && boss.getRaidStatus().equals(RaidBossSpawnManager.StatusEnum.ALIVE))
					handleHellmans(boss, mode);
				return;
			}
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Error while specialNoghtBoss(): " + e.getMessage(), e);
		}
	}
	
	private void handleHellmans(L2RaidBossInstance boss, int mode)
	{
		switch (mode)
		{
			case 0:
				boss.deleteMe();
				_log.info("DayNightSpawnManager: Deleting Hellman raidboss");
				break;
			case 1:
				boss.spawnMe();
				_log.info("DayNightSpawnManager: Spawning Hellman raidboss");
				break;
		}
	}
	
	public L2RaidBossInstance handleBoss(L2Spawn spawnDat)
	{
		if (_bosses.containsKey(spawnDat))
			return _bosses.get(spawnDat);
		
		if (GameTimeController.getInstance().isNowNight())
		{
			L2RaidBossInstance raidboss = (L2RaidBossInstance) spawnDat.doSpawn();
			_bosses.put(spawnDat, raidboss);
			
			return raidboss;
		}
		else
			_bosses.put(spawnDat, null);
		
		return null;
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final DayNightSpawnManager _instance = new DayNightSpawnManager();
	}
}
