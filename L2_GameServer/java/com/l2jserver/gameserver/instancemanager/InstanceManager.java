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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2jserver.Config;
import com.l2jserver.L2DatabaseFactory;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.entity.Instance;

import javolution.io.UTF8StreamReader;
import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.xml.stream.XMLStreamConstants;
import javolution.xml.stream.XMLStreamException;
import javolution.xml.stream.XMLStreamReaderImpl;


/** 
 * @author evill33t, GodKratos
 * 
 */
public class InstanceManager
{
	private final static Logger _log = Logger.getLogger(InstanceManager.class.getName());
	private FastMap<Integer, Instance> _instanceList = new FastMap<Integer, Instance>();
	private FastMap<Integer, InstanceWorld> _instanceWorlds = new FastMap<Integer, InstanceWorld>();
	private int _dynamic = 300000;
	
	// InstanceId Names
	private final static Map<Integer, String> _instanceIdNames = new FastMap<Integer, String>();
	private Map<Integer,Map<Integer,Long>> _playerInstanceTimes = new FastMap<Integer, Map<Integer,Long>>();
	
	private static final String ADD_INSTANCE_TIME = "INSERT INTO character_instance_time (charId,instanceId,time) values (?,?,?) ON DUPLICATE KEY UPDATE time=?";
	private static final String RESTORE_INSTANCE_TIMES = "SELECT instanceId,time FROM character_instance_time WHERE charId=?";
	private static final String DELETE_INSTANCE_TIME = "DELETE FROM character_instance_time WHERE charId=? AND instanceId=?";
	
	public long getInstanceTime(int playerObjId, int id)
	{
		if (!_playerInstanceTimes.containsKey(playerObjId))
			restoreInstanceTimes(playerObjId);
		if (_playerInstanceTimes.get(playerObjId).containsKey(id))
			return _playerInstanceTimes.get(playerObjId).get(id);
		return -1;
	}

	public Map<Integer,Long> getAllInstanceTimes(int playerObjId)
	{
		if (!_playerInstanceTimes.containsKey(playerObjId))
			restoreInstanceTimes(playerObjId);
		return _playerInstanceTimes.get(playerObjId);
	}

	public void setInstanceTime(int playerObjId, int id, long time)
	{
		if (!_playerInstanceTimes.containsKey(playerObjId))
			restoreInstanceTimes(playerObjId);
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = null;
			statement = con.prepareStatement(ADD_INSTANCE_TIME);
			statement.setInt(1, playerObjId);
			statement.setInt(2, id);
			statement.setLong(3, time);
			statement.setLong(4, time);
			statement.execute();
			statement.close();
			_playerInstanceTimes.get(playerObjId).put(id, time);
		}
		catch (Exception e) { _log.log(Level.WARNING, "Could not insert character instance time data: "+ e.getMessage(), e); }
		finally { try { con.close(); } catch (Exception e) {} }
	}

	public void deleteInstanceTime(int playerObjId, int id)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = null;
			statement = con.prepareStatement(DELETE_INSTANCE_TIME);
			statement.setInt(1, playerObjId);
			statement.setInt(2, id);
			statement.execute();
			statement.close();
			_playerInstanceTimes.get(playerObjId).remove(id);
		}
		catch (Exception e) { _log.log(Level.WARNING, "Could not delete character instance time data: "+ e.getMessage(), e); }
		finally { try { con.close(); } catch (Exception e) {} }
	}

	public void restoreInstanceTimes(int playerObjId)
	{
		if (_playerInstanceTimes.containsKey(playerObjId))
			return; // already restored
		_playerInstanceTimes.put(playerObjId, new FastMap<Integer, Long>());
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(RESTORE_INSTANCE_TIMES);
			statement.setInt(1, playerObjId);
			ResultSet rset = statement.executeQuery();

			while (rset.next())
			{
				int id = rset.getInt("instanceId");
				long time = rset.getLong("time");
				if (time < System.currentTimeMillis())
					deleteInstanceTime(playerObjId, id);
				else
					_playerInstanceTimes.get(playerObjId).put(id, time);
			}

			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Could not delete character instance time data: "+ e.getMessage(), e);
		}
		finally
		{
			try { con.close(); } catch (Exception e) {}
		}
	}

	public String getInstanceIdName(int id)
	{
		if (_instanceIdNames.containsKey(id))
			return _instanceIdNames.get(id);
		return ("UnknownInstance");
	}
	
	private void loadInstanceNames()
	{
		InputStream in = null;
		try
		{
			in = new FileInputStream(Config.DATAPACK_ROOT + "/data/instancenames.xml");
			XMLStreamReaderImpl xpp = new XMLStreamReaderImpl();
			xpp.setInput(new UTF8StreamReader().setInput(in));
			for (int e = xpp.getEventType(); e != XMLStreamConstants.END_DOCUMENT; e = xpp.next())
			{
				if (e == XMLStreamConstants.START_ELEMENT)
				{
					if (xpp.getLocalName().toString().equals("instance"))
					{
						Integer id = Integer.valueOf(xpp.getAttributeValue(null, "id").toString());
						String name = xpp.getAttributeValue(null, "name").toString();
						_instanceIdNames.put(id, name);
					}
				}
			}
		}
		catch (FileNotFoundException e)
		{
			_log.warning("instancenames.xml could not be loaded: file not found");
		}
		catch (XMLStreamException xppe)
		{
			_log.log(Level.WARNING, "Error while loading instance names: " + xppe.getMessage(), xppe);
		}
		finally
		{
			try
			{
				in.close();
			}
			catch (Exception e)
			{
			}
		}
	}

	public class InstanceWorld
	{
		public int instanceId;
		public int templateId = -1;
		public FastList<Integer> allowed = new FastList<Integer>();
		public int status;
	}
	
	public void addWorld(InstanceWorld world)
	{
		_instanceWorlds.put(world.instanceId, world);
	}
	
	public InstanceWorld getWorld(int instanceId)
	{
		return _instanceWorlds.get(instanceId);
	}
	
	public InstanceWorld getPlayerWorld(L2PcInstance player)
	{
		for (InstanceWorld temp : _instanceWorlds.values())
		{
			if (temp == null)
				continue;
			// check if the player have a World Instance where he/she is allowed to enter
			if (temp.allowed.contains(player.getObjectId()))
				return temp;
		}
		return null;
	}
	
	private InstanceManager()
	{
		_log.info("Initializing InstanceManager");
		loadInstanceNames();
		_log.info("Loaded " + _instanceIdNames.size() + " instance names");
		createWorld();
	}
	
	public static final InstanceManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private void createWorld()
	{
		Instance themultiverse = new Instance(-1);
		themultiverse.setName("multiverse");
		_instanceList.put(-1, themultiverse);
		_log.info("Multiverse Instance created");
		
		Instance universe = new Instance(0);
		universe.setName("universe");
		_instanceList.put(0, universe);
		_log.info("Universe Instance created");
	}
	
	public void destroyInstance(int instanceid)
	{
		if (instanceid <= 0)
			return;
		Instance temp = _instanceList.get(instanceid);
		if (temp != null)
		{
			temp.removeNpcs();
			temp.removePlayers();
			temp.removeDoors();
			temp.cancelTimer();
			_instanceList.remove(instanceid);
			if (_instanceWorlds.containsKey(instanceid))
				_instanceWorlds.remove(instanceid);
		}
	}
	
	public Instance getInstance(int instanceid)
	{
		return _instanceList.get(instanceid);
	}
	
	public FastMap<Integer, Instance> getInstances()
	{
		return _instanceList;
	}
	
	public int getPlayerInstance(int objectId)
	{
		for (Instance temp : _instanceList.values())
		{
			if (temp == null)
				continue;
			// check if the player is in any active instance
			if (temp.containsPlayer(objectId))
				return temp.getId();
		}
		// 0 is default instance aka the world
		return 0;
	}
	
	public boolean createInstance(int id)
	{
		if (getInstance(id) != null)
			return false;
		
		Instance instance = new Instance(id);
		_instanceList.put(id, instance);
		return true;
	}
	
	public boolean createInstanceFromTemplate(int id, String template) throws FileNotFoundException
	{
		if (getInstance(id) != null)
			return false;
		
		Instance instance = new Instance(id);
		_instanceList.put(id, instance);
		instance.loadInstanceTemplate(template);
		return true;
	}
	
	/**
	 * Create a new instance with a dynamic instance id based on a template (or null)
	 * @param template xml file
	 * @return
	 */
	public int createDynamicInstance(String template)
	{
		
		while (getInstance(_dynamic) != null)
		{
			_dynamic++;
			if (_dynamic == Integer.MAX_VALUE)
			{
				_log.warning("InstanceManager: More then " + (Integer.MAX_VALUE - 300000) + " instances created");
				_dynamic = 300000;
			}
		}
		Instance instance = new Instance(_dynamic);
		_instanceList.put(_dynamic, instance);
		if (template != null)
		{
			try
			{
				instance.loadInstanceTemplate(template);
			}
			catch (FileNotFoundException e)
			{
				_log.log(Level.WARNING, "InstanceManager: Failed creating instance from template " + template + ", " + e.getMessage(), e);
			}
		}
		return _dynamic;
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final InstanceManager _instance = new InstanceManager();
	}
}
