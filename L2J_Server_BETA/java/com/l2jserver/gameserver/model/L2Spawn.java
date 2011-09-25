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

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastList;

import com.l2jserver.Config;
import com.l2jserver.gameserver.GeoData;
import com.l2jserver.gameserver.Territory;
import com.l2jserver.gameserver.ThreadPoolManager;
import com.l2jserver.gameserver.idfactory.IdFactory;
import com.l2jserver.gameserver.model.actor.L2Attackable;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.model.actor.instance.L2MonsterInstance;
import com.l2jserver.gameserver.templates.chars.L2NpcTemplate;
import com.l2jserver.util.Rnd;

/**
 * This class manages the spawn and respawn of a group of L2NpcInstance that are in the same are and have the same type.
 *
 * <B><U> Concept</U> :</B><BR><BR>
 * L2NpcInstance can be spawned either in a random position into a location area (if Lox=0 and Locy=0), either at an exact position.
 * The heading of the L2NpcInstance can be a random heading if not defined (value= -1) or an exact heading (ex : merchant...).<BR><BR>
 *
 * @author Nightmare
 * @version $Revision: 1.9.2.3.2.8 $ $Date: 2005/03/27 15:29:32 $
 */
public class L2Spawn
{
	protected static final Logger _log = Logger.getLogger(L2Spawn.class.getName());
	
	/** The link on the L2NpcTemplate object containing generic and static properties of this spawn (ex : RewardExp, RewardSP, AggroRange...) */
	private L2NpcTemplate _template;
	
	// private String _location = DEFAULT_LOCATION;
	
	/** The identifier of the location area where L2NpcInstance can be spwaned */
	private int _location;
	
	/** The maximum number of L2NpcInstance that can manage this L2Spawn */
	private int _maximumCount;
	
	/** The current number of L2NpcInstance managed by this L2Spawn */
	private int _currentCount;
	
	/** The current number of SpawnTask in progress or stand by of this L2Spawn */
	protected int _scheduledCount;
	
	/** The X position of the spwan point */
	private int _locX;
	
	/** The Y position of the spwan point */
	private int _locY;
	
	/** The Z position of the spwan point */
	private int _locZ;
	
	/** The heading of L2NpcInstance when they are spawned */
	private int _heading;
	
	/** The delay between a L2NpcInstance remove and its re-spawn */
	private int _respawnDelay;
	
	/** Minimum delay RaidBoss */
	private int _respawnMinDelay;
	
	/** Maximum delay RaidBoss */
	private int _respawnMaxDelay;
	
	private int _instanceId = 0;
	
	/** The generic constructor of L2NpcInstance managed by this L2Spawn */
	private Constructor<?> _constructor;
	
	/** If True a L2NpcInstance is respawned each time that another is killed */
	private boolean _doRespawn;
	
	/** If true then spawn is custom */
	private boolean _customSpawn;
	
	private L2Npc _lastSpawn;
	private static List<SpawnListener> _spawnListeners = new FastList<SpawnListener>();
	
	/** The task launching the function doSpawn() */
	class SpawnTask implements Runnable
	{
		//L2NpcInstance _instance;
		//int _objId;
		private L2Npc _oldNpc;
		
		public SpawnTask(/*int objid*/L2Npc pOldNpc)
		{
			//_objId= objid;
			_oldNpc = pOldNpc;
		}
		
		public void run()
		{
			try
			{
				//doSpawn();
				respawnNpc(_oldNpc);
			}
			catch (Exception e)
			{
				_log.log(Level.WARNING, "", e);
			}
			
			_scheduledCount--;
		}
	}
	
	
	/**
	 * Constructor of L2Spawn.<BR><BR>
	 *
	 * <B><U> Concept</U> :</B><BR><BR>
	 * Each L2Spawn owns generic and static properties (ex : RewardExp, RewardSP, AggroRange...).
	 * All of those properties are stored in a different L2NpcTemplate for each type of L2Spawn.
	 * Each template is loaded once in the server cache memory (reduce memory use).
	 * When a new instance of L2Spawn is created, server just create a link between the instance and the template.
	 * This link is stored in <B>_template</B><BR><BR>
	 *
	 * Each L2NpcInstance is linked to a L2Spawn that manages its spawn and respawn (delay, location...).
	 * This link is stored in <B>_spawn</B> of the L2NpcInstance<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Set the _template of the L2Spawn </li>
	 * <li>Calculate the implementationName used to generate the generic constructor of L2NpcInstance managed by this L2Spawn</li>
	 * <li>Create the generic constructor of L2NpcInstance managed by this L2Spawn</li><BR><BR>
	 *
	 * @param mobTemplate The L2NpcTemplate to link to this L2Spawn
	 * @throws SecurityException 
	 * @throws ClassNotFoundException 
	 * @throws NoSuchMethodException 
	 */
	public L2Spawn(L2NpcTemplate mobTemplate) throws SecurityException, ClassNotFoundException, NoSuchMethodException
	{
		// Set the _template of the L2Spawn
		_template = mobTemplate;
		
		if (_template == null)
			return;
		
		// Create the generic constructor of L2NpcInstance managed by this L2Spawn
		Class<?>[] parameters = {int.class, Class.forName("com.l2jserver.gameserver.templates.chars.L2NpcTemplate")};
		_constructor = Class.forName("com.l2jserver.gameserver.model.actor.instance." + _template.type + "Instance").getConstructor(parameters);
	}
	
	/**
	 * @return the maximum number of L2NpcInstance that this L2Spawn can manage.
	 */
	public int getAmount()
	{
		return _maximumCount;
	}
	
	/**
	 * @return the Identifier of the location area where L2NpcInstance can be spwaned.
	 */
	public int getLocation()
	{
		return _location;
	}
	
	/**
	 * @return the X position of the spwan point.
	 */
	public int getLocx()
	{
		return _locX;
	}
	
	/**
	 * @return the Y position of the spwan point.
	 */
	public int getLocy()
	{
		return _locY;
	}
	
	/**
	 * @return the Z position of the spwan point.
	 */
	public int getLocz()
	{
		return _locZ;
	}
	
	/**
	 * @return the Identifier of the L2NpcInstance manage by this L2Spwan contained in the L2NpcTemplate.
	 */
	public int getNpcid()
	{
		return _template.npcId;
	}
	
	/**
	 * @return the heading of L2NpcInstance when they are spawned.
	 */
	public int getHeading()
	{
		return _heading;
	}
	
	/**
	 * @return the delay between a L2NpcInstance remove and its re-spawn.
	 */
	public int getRespawnDelay()
	{
		return _respawnDelay;
	}
	/**
	 * @return Min RaidBoss Spawn delay.
	 */
	public int getRespawnMinDelay()
	{
		return _respawnMinDelay;
	}
	/**
	 * @return Max RaidBoss Spawn delay.
	 */
	public int getRespawnMaxDelay()
	{
		return _respawnMaxDelay;
	}
	
	/**
	 * Set the maximum number of L2NpcInstance that this L2Spawn can manage.<BR><BR>
	 * @param amount 
	 */
	public void setAmount(int amount)
	{
		_maximumCount = amount;
	}
	
	/**
	 * Set the Identifier of the location area where L2NpcInstance can be spwaned.<BR><BR>
	 * @param location 
	 */
	public void setLocation(int location)
	{
		_location = location;
	}
	/**
	 * Set Minimum Respawn Delay.<BR><BR>
	 * @param date 
	 */
	public void setRespawnMinDelay(int date)
	{
		_respawnMinDelay = date;
	}
	/**
	 * Set Maximum Respawn Delay.<BR><BR>
	 * @param date 
	 */
	public void setRespawnMaxDelay(int date)
	{
		_respawnMaxDelay = date;
	}
	/**
	 * Set the X position of the spwan point.<BR><BR>
	 * @param locx 
	 */
	public void setLocx(int locx)
	{
		_locX = locx;
	}
	
	/**
	 * Set the Y position of the spwan point.<BR><BR>
	 * @param locy 
	 */
	public void setLocy(int locy)
	{
		_locY = locy;
	}
	
	/**
	 * Set the Z position of the spwan point.<BR><BR>
	 * @param locz 
	 */
	public void setLocz(int locz)
	{
		_locZ = locz;
	}
	
	/**
	 * Set the heading of L2NpcInstance when they are spawned.<BR><BR>
	 * @param heading 
	 */
	public void setHeading(int heading)
	{
		_heading = heading;
	}
	
	/**
	 * Set the spawn as custom.<BR>
	 * @param custom 
	 */
	public void setCustom(boolean custom)
	{
		_customSpawn = custom;
	}
	
	/**
	 * @return type of spawn.
	 */
	public boolean isCustom()
	{
		return _customSpawn;
	}
	
	/**
	 * Decrease the current number of L2NpcInstance of this L2Spawn and if necessary create a SpawnTask to launch after the respawn Delay.<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Decrease the current number of L2NpcInstance of this L2Spawn </li>
	 * <li>Check if respawn is possible to prevent multiple respawning caused by lag </li>
	 * <li>Update the current number of SpawnTask in progress or stand by of this L2Spawn </li>
	 * <li>Create a new SpawnTask to launch after the respawn Delay </li><BR><BR>
	 *
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : A respawn is possible ONLY if _doRespawn=True and _scheduledCount + _currentCount < _maximumCount</B></FONT><BR><BR>
	 * @param oldNpc 
	 */
	public void decreaseCount(/*int npcId*/L2Npc oldNpc)
	{
		// sanity check
		if (_currentCount <= 0)
			return;
		
		// Decrease the current number of L2NpcInstance of this L2Spawn
		_currentCount--;
		
		// Check if respawn is possible to prevent multiple respawning caused by lag
		if (_doRespawn && _scheduledCount + _currentCount < _maximumCount )
		{
			// Update the current number of SpawnTask in progress or stand by of this L2Spawn
			_scheduledCount++;
			
			// Create a new SpawnTask to launch after the respawn Delay
			//ClientScheduler.getInstance().scheduleLow(new SpawnTask(npcId), _respawnDelay);
			ThreadPoolManager.getInstance().scheduleGeneral(new SpawnTask(oldNpc), _respawnDelay);
		}
	}
	
	/**
	 * Create the initial spawning and set _doRespawn to True.<BR><BR>
	 *
	 * @return The number of L2NpcInstance that were spawned
	 */
	public int init()
	{
		while (_currentCount < _maximumCount)
		{
			doSpawn();
		}
		_doRespawn = true;
		
		return _currentCount;
	}
	
	/**
	 * Create a L2NpcInstance in this L2Spawn.<BR><BR>
	 * @param val 
	 * @return 
	 */
	public L2Npc spawnOne(boolean val)
	{
		return doSpawn(val);
	}

	/**
	 * @return true if respawn enabled
	 */
	public boolean isRespawnEnabled()
	{
		return _doRespawn;
	}

	/**
	 * Set _doRespawn to False to stop respawn in this L2Spawn.<BR><BR>
	 */
	public void stopRespawn()
	{
		_doRespawn = false;
	}
	
	/**
	 * Set _doRespawn to True to start or restart respawn in this L2Spawn.<BR><BR>
	 */
	public void startRespawn()
	{
		_doRespawn = true;
	}
	
	public L2Npc doSpawn()
	{
		return doSpawn(false);
	}
	/**
	 * Create the L2NpcInstance, add it to the world and lauch its OnSpawn action.<BR><BR>
	 *
	 * <B><U> Concept</U> :</B><BR><BR>
	 * L2NpcInstance can be spawned either in a random position into a location area (if Lox=0 and Locy=0), either at an exact position.
	 * The heading of the L2NpcInstance can be a random heading if not defined (value= -1) or an exact heading (ex : merchant...).<BR><BR>
	 *
	 * <B><U> Actions for an random spawn into location area</U> : <I>(if Locx=0 and Locy=0)</I></B><BR><BR>
	 * <li>Get L2NpcInstance Init parameters and its generate an Identifier </li>
	 * <li>Call the constructor of the L2NpcInstance </li>
	 * <li>Calculate the random position in the location area (if Locx=0 and Locy=0) or get its exact position from the L2Spawn </li>
	 * <li>Set the position of the L2NpcInstance </li>
	 * <li>Set the HP and MP of the L2NpcInstance to the max </li>
	 * <li>Set the heading of the L2NpcInstance (random heading if not defined : value=-1) </li>
	 * <li>Link the L2NpcInstance to this L2Spawn </li>
	 * <li>Init other values of the L2NpcInstance (ex : from its L2CharTemplate for INT, STR, DEX...) and add it in the world </li>
	 * <li>Lauch the action OnSpawn fo the L2NpcInstance </li><BR><BR>
	 * <li>Increase the current number of L2NpcInstance managed by this L2Spawn  </li><BR><BR>
	 * @param isSummonSpawn 
	 * @return 
	 */
	public L2Npc doSpawn(boolean isSummonSpawn)
	{
		L2Npc mob = null;
		try
		{
			// Check if the L2Spawn is not a L2Pet or L2Minion or L2Decoy spawn
			if (_template.type.equalsIgnoreCase("L2Pet")
					|| _template.type.equalsIgnoreCase("L2Decoy")
					|| _template.type.equalsIgnoreCase("L2Trap")
					|| _template.type.equalsIgnoreCase("L2EffectPoint"))
			{
				_currentCount++;
				
				return mob;
			}
			
			// Get L2NpcInstance Init parameters and its generate an Identifier
			Object[] parameters = {IdFactory.getInstance().getNextId(), _template};
			
			// Call the constructor of the L2NpcInstance
			// (can be a L2ArtefactInstance, L2FriendlyMobInstance, L2GuardInstance, L2MonsterInstance, L2SiegeGuardInstance, L2BoxInstance,
			// L2FeedableBeastInstance, L2TamedBeastInstance, L2FolkInstance or L2TvTEventNpcInstance)
			Object  tmp = _constructor.newInstance(parameters);
			((L2Object)tmp).setInstanceId(_instanceId); // Must be done before object is spawned into visible world
			if (isSummonSpawn && tmp instanceof L2Character)
				((L2Character)tmp).setShowSummonAnimation(isSummonSpawn);
			// Check if the Instance is a L2NpcInstance
			if (!(tmp instanceof L2Npc))
				return mob;
			mob = (L2Npc)tmp;
			return initializeNpcInstance(mob);
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "NPC "+_template.npcId+" class not found", e);
		}
		return mob;
	}
	
	/**
	 * @param mob
	 * @return
	 */
	private L2Npc initializeNpcInstance(L2Npc mob)
	{
		int newlocx, newlocy, newlocz;
		
		// If Locx=0 and Locy=0, the L2NpcInstance must be spawned in an area defined by location
		if  (getLocx()==0 && getLocy()==0)
		{
			if (getLocation()==0)
				return mob;
			
			// Calculate the random position in the location area
			int p[] = Territory.getInstance().getRandomPoint(getLocation());
			
			// Set the calculated position of the L2NpcInstance
			newlocx = p[0];
			newlocy = p[1];
			newlocz = GeoData.getInstance().getSpawnHeight(newlocx, newlocy, p[2], p[3],this);
		}
		else
		{
			// The L2NpcInstance is spawned at the exact position (Lox, Locy, Locz)
			newlocx = getLocx();
			newlocy = getLocy();
			if (Config.GEODATA > 0)
				newlocz = GeoData.getInstance().getSpawnHeight(newlocx,newlocy,getLocz(),getLocz(),this);
			else newlocz = getLocz();
		}
		
		mob.stopAllEffects();
		
		mob.setIsDead(false);
		// Reset decay info
		mob.setDecayed(false);
		// Set the HP and MP of the L2NpcInstance to the max
		mob.setCurrentHpMp(mob.getMaxHp(), mob.getMaxMp());
		
		// Set the heading of the L2NpcInstance (random heading if not defined)
		if (getHeading() == -1)
		{
			mob.setHeading(Rnd.nextInt(61794));
		}
		else
		{
			mob.setHeading(getHeading());
		}
		
		if (mob instanceof L2Attackable)
		{
			((L2Attackable) mob).setChampion(false);
		}
		
		if (Config.L2JMOD_CHAMPION_ENABLE)
		{
			// Set champion on next spawn
			if
			(
					mob instanceof L2MonsterInstance
					&& !getTemplate().isQuestMonster
					&& !mob.isRaid()
					&& !((L2MonsterInstance)mob).isRaidMinion()
					&& Config.L2JMOD_CHAMPION_FREQUENCY > 0
					&& mob.getLevel()>=Config.L2JMOD_CHAMP_MIN_LVL
					&& mob.getLevel()<=Config.L2JMOD_CHAMP_MAX_LVL
					&& (Config.L2JMOD_CHAMPION_ENABLE_IN_INSTANCES || getInstanceId() == 0)
			)
			{
				int random = Rnd.get(100);
				
				if (random < Config.L2JMOD_CHAMPION_FREQUENCY)
					((L2Attackable) mob).setChampion(true);
			}
		}
		
		// Link the L2NpcInstance to this L2Spawn
		mob.setSpawn(this);
		
		// Init other values of the L2NpcInstance (ex : from its L2CharTemplate for INT, STR, DEX...) and add it in the world as a visible object
		mob.spawnMe(newlocx, newlocy, newlocz);
		
		L2Spawn.notifyNpcSpawned(mob);
		
		_lastSpawn = mob;
		
		if (Config.DEBUG)
			_log.finest("spawned Mob ID: "+_template.npcId+" ,at: "+mob.getX()+" x, "+mob.getY()+" y, "+mob.getZ()+" z");
		
		// Increase the current number of L2NpcInstance managed by this L2Spawn
		_currentCount++;
		return mob;
	}
	
	public static void addSpawnListener(SpawnListener listener)
	{
		synchronized (_spawnListeners)
		{
			_spawnListeners.add(listener);
		}
	}
	
	public static void removeSpawnListener(SpawnListener listener)
	{
		synchronized (_spawnListeners)
		{
			_spawnListeners.remove(listener);
		}
	}
	
	public static void notifyNpcSpawned(L2Npc npc)
	{
		synchronized (_spawnListeners)
		{
			for (SpawnListener listener : _spawnListeners)
			{
				listener.npcSpawned(npc);
			}
		}
	}
	
	/**
	 * @param i delay in seconds
	 */
	public void setRespawnDelay(int i)
	{
		if (i < 0)
			_log.warning("respawn delay is negative for spawn:"+this);
		
		if (i < 10)
			i = 10;
		
		_respawnDelay = i * 1000;
	}
	
	public L2Npc getLastSpawn()
	{
		return _lastSpawn;
	}
	
	/**
	 * @param oldNpc
	 */
	public void respawnNpc(L2Npc oldNpc)
	{
		if (_doRespawn)
		{
			oldNpc.refreshID();
			/*L2NpcInstance instance = */initializeNpcInstance(oldNpc);
		}
	}
	
	public L2NpcTemplate getTemplate()
	{
		return _template;
	}
	
	public int getInstanceId()
	{
		return _instanceId;
	}
	
	public void setInstanceId(int instanceId)
	{
		_instanceId = instanceId;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return "L2Spawn [_template=" + getNpcid() + ", _locX=" + _locX + ", _locY=" + _locY + ", _locZ=" + _locZ + ", _heading=" + _heading + "]";
	}
}
