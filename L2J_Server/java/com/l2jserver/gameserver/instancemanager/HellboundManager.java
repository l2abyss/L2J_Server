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
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

import javolution.util.FastList;

import com.l2jserver.Config;
import com.l2jserver.L2DatabaseFactory;
import com.l2jserver.gameserver.ThreadPoolManager;
import com.l2jserver.gameserver.datatables.NpcTable;
import com.l2jserver.gameserver.datatables.SpawnTable;
import com.l2jserver.gameserver.model.L2Spawn;
import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.templates.chars.L2NpcTemplate;
import com.l2jserver.util.Rnd;

/**
 * @author _DS_, GKR
 */
public class HellboundManager
{
	private static final Logger _log = Logger.getLogger(HellboundManager.class.getName());
	
	private static final String LOAD_SPAWNS = "SELECT npc_templateid, locx, locy, locz, heading, " +
		"respawn_delay, respawn_random, min_hellbound_level, " +
		"max_hellbound_level FROM hellbound_spawnlist ORDER BY npc_templateid";
	
	private int _level = 0;
	private int _trust = 0;
	private int _maxTrust = 0;
	private int _minTrust = 0;
	
	private ScheduledFuture<?> _engine = null;
	private final List<HellboundSpawn> _population;
	
	private HellboundManager()
	{
		_population = new FastList<HellboundSpawn>();
		
		loadData();
		loadSpawns();
	}
	
	public final int getLevel()
	{
		return _level;
	}
	
	public final synchronized void updateTrust(int t, boolean useRates)
	{
		if (isLocked())
		{
			return;
		}
		
		int reward = t;
		if (useRates)
		{
			reward = (int) (t > 0 ? Config.RATE_HB_TRUST_INCREASE * t : Config.RATE_HB_TRUST_DECREASE * t);
		}
		
		final int trust = Math.max(_trust + reward, _minTrust);
		if (_maxTrust > 0)
		{
			_trust = Math.min(trust, _maxTrust);
		}
		else
		{
			_trust = trust;
		}
	}
	
	public final void setLevel(int lvl)
	{
		_level = lvl;
	}
	
	public final int getTrust()
	{
		return _trust;
	}
	
	public final int getMaxTrust()
	{
		return _maxTrust;
	}
	
	public final int getMinTrust()
	{
		return _minTrust;
	}
	
	public final void setMaxTrust(int trust)
	{
		_maxTrust = trust;
		if ((_maxTrust > 0) && (_trust > _maxTrust))
		{
			_trust = _maxTrust;
		}
	}
	
	public final void setMinTrust(int trust)
	{
		_minTrust = trust;
		
		if (_trust >= _maxTrust)
		{
			_trust = _minTrust;
		}
	}
	
	/**
	 * @return true if Hellbound is locked
	 */
	public final boolean isLocked()
	{
		return _level == 0;
	}
	
	public final void unlock()
	{
		if (_level == 0)
		{
			setLevel(1);
		}
	}
	
	public final void registerEngine(Runnable r, int interval)
	{
		if (_engine != null)
		{
			_engine.cancel(false);
		}
		_engine = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(r, interval, interval);
	}
	
	public final void doSpawn()
	{
		int added = 0;
		int deleted = 0;
		for (HellboundSpawn spawnDat : _population)
		{
			try
			{
				if (spawnDat == null)
				{
					continue;
				}
				
				L2Npc npc = spawnDat.getLastSpawn();
				if ((_level < spawnDat.getMinLvl()) || (_level > spawnDat.getMaxLvl()))
				{
					// npc should be removed
					spawnDat.stopRespawn();
					
					if ((npc != null) && npc.isVisible())
					{
						npc.deleteMe();
						deleted++;
					}
				}
				else
				{
					// npc should be added
					spawnDat.startRespawn();
					npc = spawnDat.getLastSpawn();
					if (npc == null)
					{
						npc = spawnDat.doSpawn();
						added++;
					}
					else
					{
						if (npc.isDecayed())
						{
							npc.setDecayed(false);
						}
						if (npc.isDead())
						{
							npc.doRevive();
						}
						if (!npc.isVisible())
						{
							added++;
						}
						
						npc.setCurrentHp(npc.getMaxHp());
						npc.setCurrentMp(npc.getMaxMp());
						// npc.spawnMe(spawnDat.getLocx(), spawnDat.getLocy(),
						// spawnDat.getLocz());
					}
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		
		if (added > 0)
		{
			_log.info("HellboundManager: Spawned " + added + " NPCs.");
		}
		if (deleted > 0)
		{
			_log.info("HellboundManager: Removed " + deleted + " NPCs.");
		}
	}
	
	public final void cleanUp()
	{
		saveData();
		
		if (_engine != null)
		{
			_engine.cancel(true);
			_engine = null;
		}
		_population.clear();
	}
	
	private final void loadData()
	{
		if (GlobalVariablesManager.getInstance().isVariableStored("HBLevel"))
		{
			_level = Integer.parseInt(GlobalVariablesManager.getInstance().getStoredVariable("HBLevel"));
			_trust = Integer.parseInt(GlobalVariablesManager.getInstance().getStoredVariable("HBTrust"));
		}
		else
		{
			saveData();
		}
	}
	
	public final void saveData()
	{
		GlobalVariablesManager.getInstance().storeVariable("HBLevel", String.valueOf(_level));
		GlobalVariablesManager.getInstance().storeVariable("HBTrust", String.valueOf(_trust));
	}
	
	private final void loadSpawns()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			final PreparedStatement statement = con.prepareStatement(LOAD_SPAWNS);
			final ResultSet rset = statement.executeQuery();
			
			HellboundSpawn spawnDat;
			L2NpcTemplate template;
			while (rset.next())
			{
				template = NpcTable.getInstance().getTemplate(rset.getInt("npc_templateid"));
				if (template != null)
				{
					spawnDat = new HellboundSpawn(template);
					spawnDat.setAmount(1);
					spawnDat.setLocx(rset.getInt("locx"));
					spawnDat.setLocy(rset.getInt("locy"));
					spawnDat.setLocz(rset.getInt("locz"));
					spawnDat.setHeading(rset.getInt("heading"));
					spawnDat.setRespawnDelay(rset.getInt("respawn_delay"));
					spawnDat.setRespawnMinDelay(0);
					spawnDat.setRespawnMaxDelay(0);
					int respawnRandom = (rset.getInt("respawn_random"));
					if (respawnRandom > 0) // Random respawn time, if needed
					{
						spawnDat.setRespawnMinDelay(Math.max(rset.getInt("respawn_delay") - respawnRandom, 1));
						spawnDat.setRespawnMaxDelay(rset.getInt("respawn_delay") + respawnRandom);
					}
					spawnDat.setMinLvl(rset.getInt("min_hellbound_level"));
					spawnDat.setMaxLvl(rset.getInt("max_hellbound_level"));
					
					// _population.put(spawnDat, null);
					_population.add(spawnDat);
					SpawnTable.getInstance().addNewSpawn(spawnDat, false);
				}
				else
				{
					_log.warning("HellboundManager: Data missing in NPC table for ID: " + rset.getInt("npc_templateid") + ".");
				}
			}
			rset.close();
		}
		catch (Exception e)
		{
			_log.warning("HellboundManager: problem while loading spawns: " + e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
		_log.config("HellboundManager: Loaded " + _population.size() + " npc spawn locations.");
	}
	
	public static final class HellboundSpawn extends L2Spawn
	{
		/** The delay between a L2NpcInstance remove and its re-spawn */
		private int _respawnDelay;
		
		private int _minLvl;
		private int _maxLvl;
		
		public HellboundSpawn(L2NpcTemplate mobTemplate) throws SecurityException, ClassNotFoundException, NoSuchMethodException
		{
			super(mobTemplate);
		}
		
		public final int getMinLvl()
		{
			return _minLvl;
		}
		
		public final void setMinLvl(int lvl)
		{
			_minLvl = lvl;
		}
		
		public final int getMaxLvl()
		{
			return _maxLvl;
		}
		
		public final void setMaxLvl(int lvl)
		{
			_maxLvl = lvl;
		}
		
		@Override
		public final void decreaseCount(L2Npc oldNpc)
		{
			if (getRespawnDelay() <= 0)
			{
				stopRespawn();
			}
			else if (getRespawnMaxDelay() > getRespawnMinDelay())
			{
				setRespawnDelay(Rnd.get(getRespawnMinDelay(), getRespawnMaxDelay()));
			}
			
			super.decreaseCount(oldNpc);
		}
		
		/**
		 * @param i delay in seconds
		 */
		@Override
		public void setRespawnDelay(int i)
		{
			_respawnDelay = i * 1000;
			
			super.setRespawnDelay(i);
		}
		
		@Override
		public int getRespawnDelay()
		{
			return _respawnDelay;
		}
	}
	
	public static final HellboundManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final HellboundManager _instance = new HellboundManager();
	}
}
