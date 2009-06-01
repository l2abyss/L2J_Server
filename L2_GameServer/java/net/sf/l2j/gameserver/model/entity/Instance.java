package net.sf.l2j.gameserver.model.entity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;

import javolution.util.FastList;
import javolution.util.FastSet;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.Announcements;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.DoorTable;
import net.sf.l2j.gameserver.datatables.MapRegionTable;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.instancemanager.InstanceManager;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.L2WorldRegion;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2DoorInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.clientpackets.Say2;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;

/** 
 * @author evill33t, GodKratos
 * 
 */
public class Instance
{
	private final static Logger _log = Logger.getLogger(Instance.class.getName());

	private int _id;
	private String _name;
	private FastSet<Integer> _players = new FastSet<Integer>();
	private FastList<L2Npc> _npcs = new FastList<L2Npc>();
	private FastList<L2DoorInstance> _doors = new FastList<L2DoorInstance>();
	private int[] _spawnLoc = new int[3];
	private boolean _allowSummon = true;

	protected ScheduledFuture<?> _CheckTimeUpTask = null;

	public Instance(int id)
	{
		_id = id;
	}

	/**
	 *  Returns the ID of this instance.
	 */
	public int getId()
	{
		return _id;
	}

	/**
	 *  Returns the name of this instance
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
	 * Returns whether summon friend type skills are allowed for this instance
	 */
	public boolean isSummonAllowed()
	{
		return _allowSummon;
	}
	
	/**
	 * Sets the status for the instance for summon friend type skills
	 */
	public void setAllowSummon(boolean b)
	{
		_allowSummon = b;
	}
	
	/**
	 * Set the instance duration task
	 * @param duration in minutes
	 */
	public void setDuration(int duration)
	{
		if (_CheckTimeUpTask != null)
			_CheckTimeUpTask.cancel(true);

		_CheckTimeUpTask = ThreadPoolManager.getInstance().scheduleGeneral(new CheckTimeUp(duration * 60000), 15000);
	}

	/**
	 * Checks if the player exists within this instance
	 * @param objectId
	 * @return true if player exists in instance
	 */
	public boolean containsPlayer(int objectId)
	{
		if (_players.contains(objectId))
			return true;
		return false;
	}

	/**
	 * Adds the specified player to the instance
	 * @param objectId Players object ID
	 */
	public void addPlayer(int objectId)
	{
		if (!_players.contains(objectId))
			_players.add(objectId);
	}
	
	/**
	 * Removes the specified player from the instance list
	 * @param objectId Players object ID
	 */
	public void removePlayer(int objectId)
	{
		if (_players.contains(objectId))
			_players.remove(objectId);
	}

	/**
	 * Removes the player from the instance by setting InstanceId to 0 and teleporting to nearest town.
	 * @param objectId
	 */
	public void ejectPlayer(int objectId)
	{
		L2PcInstance player = (L2PcInstance) L2World.getInstance().findObject(objectId);
		if (player != null && player.getInstanceId() == this.getId())
		{
			player.setInstanceId(0);
			player.sendMessage("You were removed from the instance");
			if (getSpawnLoc()[0] != 0 && getSpawnLoc()[1] != 0 && getSpawnLoc()[2] != 0)
				player.teleToLocation(getSpawnLoc()[0], getSpawnLoc()[1], getSpawnLoc()[2]);
			else
				player.teleToLocation(MapRegionTable.TeleportWhereType.Town);
		}
	}

	public void removeNpc(L2Spawn spawn)
	{
		_npcs.remove(spawn);
	}
	
	/**
	 * Adds a door into the instance
	 * @param doorId - from doors.csv
	 * @param open - initial state of the door 
	 */
	public void addDoor(int doorId, boolean open)
	{
		for (L2DoorInstance door: _doors)
		{
			if (door.getDoorId() == doorId)
			{
				_log.warning("Door ID " + doorId + " already exists in instance " + this.getId());
				return;
			}
		}
		
		L2DoorInstance temp = DoorTable.getInstance().getDoor(doorId);
		L2DoorInstance newdoor = new L2DoorInstance(IdFactory.getInstance().getNextId(), temp.getTemplate(), temp.getDoorId(), temp.getName(), temp.isUnlockable());
		newdoor.setInstanceId(getId());
		newdoor.setRange(temp.getXMin(), temp.getYMin(), temp.getZMin(), temp.getXMax(), temp.getYMax(), temp.getZMax());
		try
		{
			newdoor.setMapRegion(MapRegionTable.getInstance().getMapRegion(temp.getX(), temp.getY()));
		}
		catch (Exception e)
		{
			_log.severe("Error in door data, ID:" + temp.getDoorId());
		}
		newdoor.getStatus().setCurrentHpMp(newdoor.getMaxHp(), newdoor.getMaxMp());
		newdoor.setOpen(open);
		newdoor.getPosition().setXYZInvisible(temp.getX(), temp.getY(), temp.getZ());
		newdoor.spawnMe(newdoor.getX(), newdoor.getY(), newdoor.getZ());

		_doors.add(newdoor);
	}

	public FastSet<Integer> getPlayers()
	{
		return _players;
	}

	public FastList<L2Npc> getNpcs()
	{
		return _npcs;
	}

	public FastList<L2DoorInstance> getDoors()
	{
		return _doors;
	}
	
	public L2DoorInstance getDoor(int id)
	{
		for (L2DoorInstance temp: getDoors())
		{
			if (temp.getDoorId() == id)
				return temp;
		}
		return null;
	}
	
	/**
	 * Returns the spawn location for this instance to be used when leaving the instance
	 * @return int[3]
	 */
	public int[] getSpawnLoc()
	{
		return _spawnLoc;
	}

	public void removePlayers()
	{
		for (int objectId : _players)
		{
			ejectPlayer(objectId);
		}
		_players.clear();
	}

	public void removeNpcs()
	{
		for (L2Npc mob : _npcs)
		{
			if (mob != null)
			{
				mob.getSpawn().stopRespawn();
				mob.deleteMe();
			}
		}
		_npcs.clear();
	}
	
	public void removeDoors()
	{
		for (L2DoorInstance door: _doors)
		{
			if (door != null)
			{
				L2WorldRegion region = door.getWorldRegion();
				door.decayMe();
				
				if (region != null)
					region.removeVisibleObject(door);
				
				door.getKnownList().removeAllKnownObjects();
				L2World.getInstance().removeObject(door);
			}
		}
		_doors.clear();
	}

	public void loadInstanceTemplate(String filename) throws FileNotFoundException
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
			_log.warning("Instance: can not find " + xml.getAbsolutePath() + " ! " + e);
		}
		catch (Exception e)
		{
			_log.warning("Instance: error while loading " + xml.getAbsolutePath() + " ! " + e);
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
					_CheckTimeUpTask = ThreadPoolManager.getInstance().scheduleGeneral(new CheckTimeUp(Integer.parseInt(a.getNodeValue()) * 60000), 15000);
			}
			/*			else if ("timeDelay".equalsIgnoreCase(n.getNodeName()))
						{
							a = n.getAttributes().getNamedItem("val");
							if (a != null)
								instance.setTimeDelay(Integer.parseInt(a.getNodeValue()));
						}*/
			else if ("allowSummon".equalsIgnoreCase(n.getNodeName()))
			{
				a = n.getAttributes().getNamedItem("val");
				if (a != null)
					setAllowSummon(Boolean.parseBoolean(a.getNodeValue()));
			}
			else if ("doorlist".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					int doorId = 0;
					boolean doorState = false;
					if ("door".equalsIgnoreCase(d.getNodeName()))
					{
						doorId = Integer.parseInt(d.getAttributes().getNamedItem("doorId").getNodeValue());
						if (d.getAttributes().getNamedItem("open") != null)
							doorState = Boolean.parseBoolean(d.getAttributes().getNamedItem("open").getNodeValue());
						addDoor(doorId, doorState);
					}
				}
			}
			else if ("spawnlist".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					int npcId = 0, x = 0, y = 0, z = 0, respawn = 0, heading = 0;

					if ("spawn".equalsIgnoreCase(d.getNodeName()))
					{

						npcId = Integer.parseInt(d.getAttributes().getNamedItem("npcId").getNodeValue());
						x = Integer.parseInt(d.getAttributes().getNamedItem("x").getNodeValue());
						y = Integer.parseInt(d.getAttributes().getNamedItem("y").getNodeValue());
						z = Integer.parseInt(d.getAttributes().getNamedItem("z").getNodeValue());
						heading = Integer.parseInt(d.getAttributes().getNamedItem("heading").getNodeValue());
						respawn = Integer.parseInt(d.getAttributes().getNamedItem("respawn").getNodeValue());

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
								spawnDat.stopRespawn();
							else
								spawnDat.startRespawn();
							spawnDat.setInstanceId(getId());
							L2Npc newmob = spawnDat.doSpawn();
							_npcs.add(newmob);
						}
						else
						{
							_log.warning("Instance: Data missing in NPC table for ID: " + npcTemplate + " in Instance " + getId());
						}
					}
				}
			}
			else if ("spawnpoint".equalsIgnoreCase(n.getNodeName()))
			{
				try
				{
					_spawnLoc[0] = Integer.parseInt(n.getAttributes().getNamedItem("spawnX").getNodeValue());
					_spawnLoc[1] = Integer.parseInt(n.getAttributes().getNamedItem("spawnY").getNodeValue());
					_spawnLoc[2] = Integer.parseInt(n.getAttributes().getNamedItem("spawnZ").getNodeValue());
				}
				catch (Exception e)
				{
					_log.warning("Error parsing instance xml: " + e);
					_spawnLoc = new int[3];
				}
			}
		}
		if (Config.DEBUG)
			_log.info(name + " Instance Template for Instance " + getId() + " loaded");
	}

	protected void doCheckTimeUp(int remaining)
	{
		CreatureSay cs = null;
		int timeLeft;
		int interval;

		if (remaining > 300000)
		{
			timeLeft = remaining / 60000;
			interval = 300000;
			SystemMessage sm = new SystemMessage(SystemMessageId.DUNGEON_EXPIRES_IN_S1_MINUTES);
			sm.addString(Integer.toString(timeLeft));
			Announcements.getInstance().announceToInstance(sm, getId());
			remaining = remaining - 300000;
		}
		else if (remaining > 60000)
		{
			timeLeft = remaining / 60000;
			interval = 60000;
			SystemMessage sm = new SystemMessage(SystemMessageId.DUNGEON_EXPIRES_IN_S1_MINUTES);
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
			for (int objectId : _players)
			{
				L2PcInstance player = (L2PcInstance) L2World.getInstance().findObject(objectId);
				if (player != null && player.getInstanceId() == getId())
				{
					player.sendPacket(cs);
				}
			}
		}
		if (_CheckTimeUpTask != null)
			_CheckTimeUpTask.cancel(true);
		if (remaining >= 10000)
			_CheckTimeUpTask = ThreadPoolManager.getInstance().scheduleGeneral(new CheckTimeUp(remaining), interval);
		else
			_CheckTimeUpTask = ThreadPoolManager.getInstance().scheduleGeneral(new TimeUp(), interval);
	}

	public void cancelTimer()
	{
		_CheckTimeUpTask.cancel(true);
	}

	public class CheckTimeUp implements Runnable
	{
		private int	_remaining;

		public CheckTimeUp(int remaining)
		{
			_remaining = remaining;
		}

		public void run()
		{
			doCheckTimeUp(_remaining);
		}
	}

	public class TimeUp implements Runnable
	{
		public void run()
		{
			InstanceManager.getInstance().destroyInstance(getId());
		}
	}
}