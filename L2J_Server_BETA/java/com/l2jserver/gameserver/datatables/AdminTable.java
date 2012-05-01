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
package com.l2jserver.gameserver.datatables;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import javolution.util.FastMap;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.l2jserver.Config;
import com.l2jserver.gameserver.engines.DocumentParser;
import com.l2jserver.gameserver.model.L2AccessLevel;
import com.l2jserver.gameserver.model.L2AdminCommandAccessRight;
import com.l2jserver.gameserver.model.StatsSet;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.L2GameServerPacket;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;

/**
 * @author UnAfraid
 */
public class AdminTable extends DocumentParser
{
	private Map<Integer, L2AccessLevel> _accessLevels;
	private Map<String, L2AdminCommandAccessRight> _adminCommandAccessRights;
	private Map<L2PcInstance, Boolean> _gmList;
	private int _highestLevel = 0;
	
	protected AdminTable()
	{
		_accessLevels = new HashMap<>();
		_adminCommandAccessRights = new HashMap<>();
		_gmList = new FastMap<L2PcInstance, Boolean>().shared();
		load();
	}
	
	@Override
	protected void parseDocument(Document doc)
	{
		NamedNodeMap attrs;
		Node attr;
		StatsSet set;
		L2AccessLevel level;
		L2AdminCommandAccessRight command;
		for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("access".equalsIgnoreCase(d.getNodeName()))
					{
						set = new StatsSet();
						attrs = d.getAttributes();
						for (int i = 0; i < attrs.getLength(); i++)
						{
							attr = attrs.item(i);
							set.set(attr.getNodeName(), attr.getNodeValue());
						}
						level = new L2AccessLevel(set);
						if (level.getLevel() >  _highestLevel)
						{
							_highestLevel = level.getLevel();
						}
						_accessLevels.put(level.getLevel(), level);
					}
					else if ("admin".equalsIgnoreCase(d.getNodeName()))
					{
						set = new StatsSet();
						attrs = d.getAttributes();
						for (int i = 0; i < attrs.getLength(); i++)
						{
							attr = attrs.item(i);
							set.set(attr.getNodeName(), attr.getNodeValue());
						}
						command = new L2AdminCommandAccessRight(set);
						_adminCommandAccessRights.put(command.getAdminCommand(), command);
					}
				}
			}
		}
	}
	
	private void load()
	{
		parseFile(new File(Config.DATAPACK_ROOT, "data/accessLevels.xml"));
		_log.log(Level.INFO, getClass().getSimpleName() + ": Loaded: " + _accessLevels.size() + " Access Levels");
		parseFile(new File(Config.DATAPACK_ROOT, "data/adminCommands.xml"));
		_log.log(Level.INFO, getClass().getSimpleName() + ": Loaded: " + _adminCommandAccessRights.size() + " Access Commands");
	}
	
	public void reload()
	{
		_accessLevels.clear();
		_adminCommandAccessRights.clear();
		load();
	}
	
	/**
	 * @return AccessLevels: the one and only instance of this class<br>
	 */
	public static AdminTable getInstance()
	{
		return SingletonHolder._instance;
	}
	
	
	/**
	 * Returns the access level by characterAccessLevel<br>
	 * <br>
	 * @param accessLevelNum as int<br>
	 * <br>
	 * @return AccessLevel: AccessLevel instance by char access level<br>
	 */
	public L2AccessLevel getAccessLevel(int accessLevelNum)
	{
		if (accessLevelNum < 0)
		{
			return _accessLevels.get(-1);
		}
		else if (!_accessLevels.containsKey(accessLevelNum))
		{
			_accessLevels.put(accessLevelNum, new L2AccessLevel());
		}
		return _accessLevels.get(accessLevelNum);
	}
	
	public L2AccessLevel getMasterAccessLevel()
	{
		return _accessLevels.get(_highestLevel);
	}
	
	public boolean hasAccessLevel(int id)
	{
		return _accessLevels.containsKey(id);
	}
	
	public boolean hasAccess(String adminCommand, L2AccessLevel accessLevel)
	{	
		L2AdminCommandAccessRight acar = _adminCommandAccessRights.get(adminCommand);
		
		if (acar == null)
		{
			// Trying to avoid the spam for next time when the gm would try to use the same command
			if (accessLevel.getLevel() > 0 && accessLevel.getLevel() == _highestLevel)
			{
				acar = new L2AdminCommandAccessRight(adminCommand, true, accessLevel.getLevel());
				_adminCommandAccessRights.put(adminCommand, acar);
				_log.info("AdminCommandAccessRights: No rights defined for admin command " + adminCommand + " auto setting accesslevel: " + accessLevel.getLevel() + " !");
			}
			else
			{
				_log.info("AdminCommandAccessRights: No rights defined for admin command " + adminCommand + " !");
				return false;
			}
		}
		
		return acar.hasAccess(accessLevel);
	}
	
	public boolean requireConfirm(String command)
	{
		L2AdminCommandAccessRight acar = _adminCommandAccessRights.get(command);
		if (acar == null)
		{
			_log.info("AdminCommandAccessRights: No rights defined for admin command " + command + ".");
			return false;
		}
		return acar.getRequireConfirm();
	}
	
	public List<L2PcInstance> getAllGms(boolean includeHidden)
	{
		List<L2PcInstance> tmpGmList = new ArrayList<>();
		
		for (Entry<L2PcInstance, Boolean> entry : _gmList.entrySet())
		{
			if (includeHidden || !entry.getValue())
			{
				tmpGmList.add(entry.getKey());
			}
		}
		
		return tmpGmList;
	}
	
	public List<String> getAllGmNames(boolean includeHidden)
	{
		List<String> tmpGmList = new ArrayList<>();
		
		for (Entry<L2PcInstance, Boolean> entry : _gmList.entrySet())
		{
			if (!entry.getValue())
			{
				tmpGmList.add(entry.getKey().getName());
			}
			else if (includeHidden)
			{
				tmpGmList.add(entry.getKey().getName() + " (invis)");
			}
		}
		
		return tmpGmList;
	}
	

	/**
	 * Add a L2PcInstance player to the Set _gmList
	 * @param player 
	 * @param hidden 
	 */
	public void addGm(L2PcInstance player, boolean hidden)
	{
		if (Config.DEBUG)
			_log.fine("added gm: " + player.getName());
		_gmList.put(player, hidden);
	}
	
	public void deleteGm(L2PcInstance player)
	{
		if (Config.DEBUG)
			_log.fine("deleted gm: " + player.getName());
		
		_gmList.remove(player);
	}
	
	/**
	 * GM will be displayed on clients gmlist
	 * @param player
	 */
	public void showGm(L2PcInstance player)
	{
		if (_gmList.containsKey(player))
			_gmList.put(player, false);
	}
	
	/**
	 * GM will no longer be displayed on clients gmlist
	 * @param player
	 */
	public void hideGm(L2PcInstance player)
	{
		if (_gmList.containsKey(player))
			_gmList.put(player, true);
	}
	
	public boolean isGmOnline(boolean includeHidden)
	{
		for (Entry<L2PcInstance, Boolean> entry : _gmList.entrySet())
		{
			if (includeHidden || !entry.getValue())
				return true;
		}
		
		return false;
	}
	
	public void sendListToPlayer(L2PcInstance player)
	{
		if (isGmOnline(player.isGM()))
		{
			player.sendPacket(SystemMessageId.GM_LIST);
			
			for (String name : getAllGmNames(player.isGM()))
			{
				SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.GM_C1);
				sm.addString(name);
				player.sendPacket(sm);
			}
		}
		else
		{
			player.sendPacket(SystemMessageId.NO_GM_PROVIDING_SERVICE_NOW);
		}
	}
	
	public void broadcastToGMs(L2GameServerPacket packet)
	{
		for (L2PcInstance gm : getInstance().getAllGms(true))
		{
			gm.sendPacket(packet);
		}
	}
	
	public void broadcastMessageToGMs(String message)
	{
		for (L2PcInstance gm : getInstance().getAllGms(true))
		{
			gm.sendMessage(message);
		}
	}
	
	private static class SingletonHolder
	{
		protected static final AdminTable _instance = new AdminTable();
	}
}
