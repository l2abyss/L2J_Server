package com.l2jserver.gameserver.model.entity;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.l2jserver.Config;
import com.l2jserver.gameserver.Announcements;
import com.l2jserver.gameserver.ThreadPoolManager;
import com.l2jserver.gameserver.datatables.DoorTable;
import com.l2jserver.gameserver.datatables.NpcTable;
import com.l2jserver.gameserver.idfactory.IdFactory;
import com.l2jserver.gameserver.instancemanager.InstanceManager;
import com.l2jserver.gameserver.instancemanager.MapRegionManager;
import com.l2jserver.gameserver.model.IL2Procedure;
import com.l2jserver.gameserver.model.L2Spawn;
import com.l2jserver.gameserver.model.L2World;
import com.l2jserver.gameserver.model.L2WorldRegion;
import com.l2jserver.gameserver.model.Location;
import com.l2jserver.gameserver.model.StatsSet;
import com.l2jserver.gameserver.model.actor.L2Attackable;
import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.model.actor.instance.L2DoorInstance;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.actor.templates.L2DoorTemplate;
import com.l2jserver.gameserver.model.actor.templates.L2NpcTemplate;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.clientpackets.Say2;
import com.l2jserver.gameserver.network.serverpackets.CreatureSay;
import com.l2jserver.gameserver.network.serverpackets.L2GameServerPacket;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;
import com.l2jserver.util.L2FastList;
import com.l2jserver.util.L2FastMap;

/**
 * Main class for game instances.
 * @author evill33t, GodKratos
 */
public class Instance
{
	private static final Logger _log = Logger.getLogger(Instance.class.getName());
	
	private final int _id;
	private String _name;
	
	private final L2FastList<Integer> _players = new L2FastList<>(true);
	private final List<L2Npc> _npcs = new L2FastList<>(true);
	private final Map<Integer, L2DoorInstance> _doors = new L2FastMap<>(true);
	private Location _spawnLoc = null;
	private boolean _allowSummon = true;
	private long _emptyDestroyTime = -1;
	private long _lastLeft = -1;
	private long _instanceStartTime = -1;
	private long _instanceEndTime = -1;
	private boolean _isPvPInstance = false;
	private boolean _showTimer = false;
	private boolean _isTimerIncrease = true;
	private String _timerText = "";
	
	protected ScheduledFuture<?> _checkTimeUpTask = null;
	
	public Instance(int id)
	{
		_id = id;
		_instanceStartTime = System.currentTimeMillis();
	}
	
	public Instance(int id, String name)
	{
		_id = id;
		_name = name;
		_instanceStartTime = System.currentTimeMillis();
	}
	
	/**
	 * @return the ID of this instance.
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * @return the name of this instance
	 */
	public String getName()
	{
		return _name;
	}
	
	public void setName(String name)
	{
		_name = name;
	}
	
	/**
	 * @return whether summon friend type skills are allowed for this instance
	 */
	public boolean isSummonAllowed()
	{
		return _allowSummon;
	}
	
	/**
	 * Sets the status for the instance for summon friend type skills
	 * @param b
	 */
	public void setAllowSummon(boolean b)
	{
		_allowSummon = b;
	}
	
	/**
	 * Returns true if entire instance is PvP zone
	 * @return
	 */
	public boolean isPvPInstance()
	{
		return _isPvPInstance;
	}
	
	/**
	 * Sets PvP zone status of the instance
	 * @param b
	 */
	public void setPvPInstance(boolean b)
	{
		_isPvPInstance = b;
	}
	
	/**
	 * Set the instance duration task
	 * @param duration in milliseconds
	 */
	public void setDuration(int duration)
	{
		if (_checkTimeUpTask != null)
		{
			_checkTimeUpTask.cancel(true);
		}
		
		_checkTimeUpTask = ThreadPoolManager.getInstance().scheduleGeneral(new CheckTimeUp(duration), 500);
		_instanceEndTime = System.currentTimeMillis() + duration + 500;
	}
	
	/**
	 * Set time before empty instance will be removed
	 * @param time in milliseconds
	 */
	public void setEmptyDestroyTime(long time)
	{
		_emptyDestroyTime = time;
	}
	
	/**
	 * Checks if the player exists within this instance
	 * @param objectId
	 * @return true if player exists in instance
	 */
	public boolean containsPlayer(int objectId)
	{
		return _players.contains(objectId);
	}
	
	/**
	 * Adds the specified player to the instance
	 * @param objectId Players object ID
	 */
	public void addPlayer(int objectId)
	{
		_players.add(objectId);
	}
	
	/**
	 * Removes the specified player from the instance list.
	 * @param objectId the player's object Id
	 */
	public void removePlayer(Integer objectId)
	{
		_players.remove(objectId);
		if (_players.isEmpty() && (_emptyDestroyTime >= 0))
		{
			_lastLeft = System.currentTimeMillis();
			setDuration((int) (_instanceEndTime - System.currentTimeMillis() - 500));
		}
	}
	
	public void addNpc(L2Npc npc)
	{
		_npcs.add(npc);
	}
	
	public void removeNpc(L2Npc npc)
	{
		if (npc.getSpawn() != null)
		{
			npc.getSpawn().stopRespawn();
		}
		_npcs.remove(npc);
	}
	
	/**
	 * Adds a door into the instance
	 * @param doorId - from doorData.xml
	 * @param set - StatsSet for initializing door
	 */
	private void addDoor(int doorId, StatsSet set)
	{
		if (_doors.containsKey(doorId))
		{
			_log.warning("Door ID " + doorId + " already exists in instance " + getId());
			return;
		}
		
		L2DoorTemplate temp = DoorTable.getInstance().getDoorTemplate(doorId);
		L2DoorInstance newdoor = new L2DoorInstance(IdFactory.getInstance().getNextId(), temp, set);
		newdoor.setInstanceId(getId());
		newdoor.setCurrentHp(newdoor.getMaxHp());
		newdoor.spawnMe(temp.posX, temp.posY, temp.posZ);
		_doors.put(doorId, newdoor);
	}
	
	public List<Integer> getPlayers()
	{
		return _players;
	}
	
	public List<L2Npc> getNpcs()
	{
		return _npcs;
	}
	
	public Collection<L2DoorInstance> getDoors()
	{
		return _doors.values();
	}
	
	public L2DoorInstance getDoor(int id)
	{
		return _doors.get(id);
	}
	
	public long getInstanceEndTime()
	{
		return _instanceEndTime;
	}
	
	public long getInstanceStartTime()
	{
		return _instanceStartTime;
	}
	
	public boolean isShowTimer()
	{
		return _showTimer;
	}
	
	public boolean isTimerIncrease()
	{
		return _isTimerIncrease;
	}
	
	public String getTimerText()
	{
		return _timerText;
	}
	
	/**
	 * @return the spawn location for this instance to be used when leaving the instance
	 */
	public Location getSpawnLoc()
	{
		return _spawnLoc;
	}
	
	/**
	 * Sets the spawn location for this instance to be used when leaving the instance
	 * @param loc
	 */
	public void setSpawnLoc(Location loc)
	{
		_spawnLoc = loc;
	}
	
	public void removePlayers()
	{
		_players.executeForEach(new EjectProcedure());
		_players.clear();
	}
	
	public void removeNpcs()
	{
		for (L2Npc mob : _npcs)
		{
			if (mob != null)
			{
				if (mob.getSpawn() != null)
				{
					mob.getSpawn().stopRespawn();
				}
				mob.deleteMe();
			}
		}
		_npcs.clear();
	}
	
	public void removeDoors()
	{
		for (L2DoorInstance door : _doors.values())
		{
			if (door != null)
			{
				L2WorldRegion region = door.getWorldRegion();
				door.decayMe();
				
				if (region != null)
				{
					region.removeVisibleObject(door);
				}
				
				door.getKnownList().removeAllKnownObjects();
				L2World.getInstance().removeObject(door);
			}
		}
		_doors.clear();
	}
	
	public void loadInstanceTemplate(String filename)
	{
		Document doc = null;
		File xml = new File(Config.DATAPACK_ROOT, "data/instances/" + filename);
		
		try
		{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			doc = factory.newDocumentBuilder().parse(xml);
			
			for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
			{
				if ("instance".equalsIgnoreCase(n.getNodeName()))
				{
					parseInstance(n);
				}
			}
		}
		catch (IOException e)
		{
			_log.log(Level.WARNING, "Instance: can not find " + xml.getAbsolutePath() + " ! " + e.getMessage(), e);
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Instance: error while loading " + xml.getAbsolutePath() + " ! " + e.getMessage(), e);
		}
	}
	
	private void parseInstance(Node n) throws Exception
	{
		L2Spawn spawnDat;
		L2NpcTemplate npcTemplate;
		String name = null;
		name = n.getAttributes().getNamedItem("name").getNodeValue();
		setName(name);
		
		Node a;
		Node first = n.getFirstChild();
		for (n = first; n != null; n = n.getNextSibling())
		{
			if ("activityTime".equalsIgnoreCase(n.getNodeName()))
			{
				a = n.getAttributes().getNamedItem("val");
				if (a != null)
				{
					_checkTimeUpTask = ThreadPoolManager.getInstance().scheduleGeneral(new CheckTimeUp(Integer.parseInt(a.getNodeValue()) * 60000), 15000);
					_instanceEndTime = System.currentTimeMillis() + (Long.parseLong(a.getNodeValue()) * 60000) + 15000;
				}
			}
			//@formatter:off
			/*			
 			else if ("timeDelay".equalsIgnoreCase(n.getNodeName()))
			{
				a = n.getAttributes().getNamedItem("val");
				if (a != null)
					instance.setTimeDelay(Integer.parseInt(a.getNodeValue()));
			}
			*/
			//@formatter:on
			else if ("allowSummon".equalsIgnoreCase(n.getNodeName()))
			{
				a = n.getAttributes().getNamedItem("val");
				if (a != null)
				{
					setAllowSummon(Boolean.parseBoolean(a.getNodeValue()));
				}
			}
			else if ("emptyDestroyTime".equalsIgnoreCase(n.getNodeName()))
			{
				a = n.getAttributes().getNamedItem("val");
				if (a != null)
				{
					_emptyDestroyTime = Long.parseLong(a.getNodeValue()) * 1000;
				}
			}
			else if ("showTimer".equalsIgnoreCase(n.getNodeName()))
			{
				a = n.getAttributes().getNamedItem("val");
				if (a != null)
				{
					_showTimer = Boolean.parseBoolean(a.getNodeValue());
				}
				a = n.getAttributes().getNamedItem("increase");
				if (a != null)
				{
					_isTimerIncrease = Boolean.parseBoolean(a.getNodeValue());
				}
				a = n.getAttributes().getNamedItem("text");
				if (a != null)
				{
					_timerText = a.getNodeValue();
				}
			}
			else if ("PvPInstance".equalsIgnoreCase(n.getNodeName()))
			{
				a = n.getAttributes().getNamedItem("val");
				if (a != null)
				{
					setPvPInstance(Boolean.parseBoolean(a.getNodeValue()));
				}
			}
			else if ("doorlist".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					int doorId = 0;
					if ("door".equalsIgnoreCase(d.getNodeName()))
					{
						doorId = Integer.parseInt(d.getAttributes().getNamedItem("doorId").getNodeValue());
						StatsSet set = new StatsSet();
						for (Node bean = d.getFirstChild(); bean != null; bean = bean.getNextSibling())
						{
							if ("set".equalsIgnoreCase(bean.getNodeName()))
							{
								NamedNodeMap attrs = bean.getAttributes();
								String setname = attrs.getNamedItem("name").getNodeValue();
								String value = attrs.getNamedItem("val").getNodeValue();
								set.set(setname, value);
							}
						}
						addDoor(doorId, set);
					}
				}
			}
			else if ("spawnlist".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					int npcId = 0, x = 0, y = 0, z = 0, respawn = 0, heading = 0, delay = -1;
					
					if ("spawn".equalsIgnoreCase(d.getNodeName()))
					{
						
						npcId = Integer.parseInt(d.getAttributes().getNamedItem("npcId").getNodeValue());
						x = Integer.parseInt(d.getAttributes().getNamedItem("x").getNodeValue());
						y = Integer.parseInt(d.getAttributes().getNamedItem("y").getNodeValue());
						z = Integer.parseInt(d.getAttributes().getNamedItem("z").getNodeValue());
						heading = Integer.parseInt(d.getAttributes().getNamedItem("heading").getNodeValue());
						respawn = Integer.parseInt(d.getAttributes().getNamedItem("respawn").getNodeValue());
						if (d.getAttributes().getNamedItem("onKillDelay") != null)
						{
							delay = Integer.parseInt(d.getAttributes().getNamedItem("onKillDelay").getNodeValue());
						}
						
						npcTemplate = NpcTable.getInstance().getTemplate(npcId);
						if (npcTemplate != null)
						{
							spawnDat = new L2Spawn(npcTemplate);
							spawnDat.setLocx(x);
							spawnDat.setLocy(y);
							spawnDat.setLocz(z);
							spawnDat.setAmount(1);
							spawnDat.setHeading(heading);
							spawnDat.setRespawnDelay(respawn);
							if (respawn == 0)
							{
								spawnDat.stopRespawn();
							}
							else
							{
								spawnDat.startRespawn();
							}
							spawnDat.setInstanceId(getId());
							L2Npc spawned = spawnDat.doSpawn();
							if ((delay >= 0) && (spawned instanceof L2Attackable))
							{
								((L2Attackable) spawned).setOnKillDelay(delay);
							}
						}
						else
						{
							_log.warning("Instance: Data missing in NPC table for ID: " + npcId + " in Instance " + getId());
						}
					}
				}
			}
			else if ("spawnpoint".equalsIgnoreCase(n.getNodeName()))
			{
				try
				{
					int x = Integer.parseInt(n.getAttributes().getNamedItem("spawnX").getNodeValue());
					int y = Integer.parseInt(n.getAttributes().getNamedItem("spawnY").getNodeValue());
					int z = Integer.parseInt(n.getAttributes().getNamedItem("spawnZ").getNodeValue());
					_spawnLoc = new Location(x, y, z);
				}
				catch (Exception e)
				{
					_log.log(Level.WARNING, "Error parsing instance xml: " + e.getMessage(), e);
					_spawnLoc = null;
				}
			}
		}
	}
	
	protected void doCheckTimeUp(int remaining)
	{
		CreatureSay cs = null;
		int timeLeft;
		int interval;
		
		if (_players.isEmpty() && (_emptyDestroyTime == 0))
		{
			remaining = 0;
			interval = 500;
		}
		else if (_players.isEmpty() && (_emptyDestroyTime > 0))
		{
			
			Long emptyTimeLeft = (_lastLeft + _emptyDestroyTime) - System.currentTimeMillis();
			if (emptyTimeLeft <= 0)
			{
				interval = 0;
				remaining = 0;
			}
			else if ((remaining > 300000) && (emptyTimeLeft > 300000))
			{
				interval = 300000;
				remaining = remaining - 300000;
			}
			else if ((remaining > 60000) && (emptyTimeLeft > 60000))
			{
				interval = 60000;
				remaining = remaining - 60000;
			}
			else if ((remaining > 30000) && (emptyTimeLeft > 30000))
			{
				interval = 30000;
				remaining = remaining - 30000;
			}
			else
			{
				interval = 10000;
				remaining = remaining - 10000;
			}
		}
		else if (remaining > 300000)
		{
			timeLeft = remaining / 60000;
			interval = 300000;
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.DUNGEON_EXPIRES_IN_S1_MINUTES);
			sm.addString(Integer.toString(timeLeft));
			Announcements.getInstance().announceToInstance(sm, getId());
			remaining = remaining - 300000;
		}
		else if (remaining > 60000)
		{
			timeLeft = remaining / 60000;
			interval = 60000;
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.DUNGEON_EXPIRES_IN_S1_MINUTES);
			sm.addString(Integer.toString(timeLeft));
			Announcements.getInstance().announceToInstance(sm, getId());
			remaining = remaining - 60000;
		}
		else if (remaining > 30000)
		{
			timeLeft = remaining / 1000;
			interval = 30000;
			cs = new CreatureSay(0, Say2.ALLIANCE, "Notice", timeLeft + " seconds left.");
			remaining = remaining - 30000;
		}
		else
		{
			timeLeft = remaining / 1000;
			interval = 10000;
			cs = new CreatureSay(0, Say2.ALLIANCE, "Notice", timeLeft + " seconds left.");
			remaining = remaining - 10000;
		}
		if (cs != null)
		{
			_players.executeForEach(new BroadcastPacket(cs));
		}
		cancelTimer();
		if (remaining >= 10000)
		{
			_checkTimeUpTask = ThreadPoolManager.getInstance().scheduleGeneral(new CheckTimeUp(remaining), interval);
		}
		else
		{
			_checkTimeUpTask = ThreadPoolManager.getInstance().scheduleGeneral(new TimeUp(), interval);
		}
	}
	
	public void cancelTimer()
	{
		if (_checkTimeUpTask != null)
		{
			_checkTimeUpTask.cancel(true);
		}
	}
	
	public class CheckTimeUp implements Runnable
	{
		private final int _remaining;
		
		public CheckTimeUp(int remaining)
		{
			_remaining = remaining;
		}
		
		@Override
		public void run()
		{
			doCheckTimeUp(_remaining);
		}
	}
	
	public class TimeUp implements Runnable
	{
		@Override
		public void run()
		{
			InstanceManager.getInstance().destroyInstance(getId());
		}
	}
	
	public final class EjectProcedure implements IL2Procedure<Integer>
	{
		@Override
		public boolean execute(Integer objectId)
		{
			final L2PcInstance player = L2World.getInstance().getPlayer(objectId);
			if ((player != null) && (player.getInstanceId() == getId()))
			{
				player.setInstanceId(0);
				player.sendMessage("You were removed from the instance");
				if (getSpawnLoc() != null)
				{
					player.teleToLocation(getSpawnLoc(), true);
				}
				else
				{
					player.teleToLocation(MapRegionManager.TeleportWhereType.Town);
				}
			}
			return true;
		}
	}
	
	public final class BroadcastPacket implements IL2Procedure<Integer>
	{
		private final L2GameServerPacket _packet;
		
		public BroadcastPacket(L2GameServerPacket packet)
		{
			_packet = packet;
		}
		
		@Override
		public boolean execute(Integer objectId)
		{
			final L2PcInstance player = L2World.getInstance().getPlayer(objectId);
			if ((player != null) && (player.getInstanceId() == getId()))
			{
				player.sendPacket(_packet);
			}
			return true;
		}
	}
}