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

import gnu.trove.TIntObjectHashMap;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2jserver.Config;
import com.l2jserver.L2DatabaseFactory;
import com.l2jserver.gameserver.model.L2NpcWalkerNode;

/**
 * Main Table to Load Npc Walkers Routes and Chat SQL Table.<br>
 * 
 * @author Rayan RPG for L2Emu Project
 * 
 * @since 927 
 *
 */
public class NpcWalkerRoutesTable
{
	private final static Logger _log = Logger.getLogger(SpawnTable.class.getName());
	
	private TIntObjectHashMap<List<L2NpcWalkerNode>> _routes = new TIntObjectHashMap<List<L2NpcWalkerNode>>();
	
	public static NpcWalkerRoutesTable getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private NpcWalkerRoutesTable()
	{
		if (Config.ALLOW_NPC_WALKERS)
		{
			_log.info("Initializing Walkers Routes Table.");
			load();
		}
	}
	
	public void load()
	{
		_routes.clear();
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT route_id, npc_id, move_point, chatText, move_x, move_y, move_z, delay, running FROM walker_routes ORDER By move_point ASC");
			ResultSet rset = statement.executeQuery();
			L2NpcWalkerNode route;
			while (rset.next())
			{
				route = new L2NpcWalkerNode();
				route.setRouteId(rset.getInt("route_id"));
				int npcid = rset.getInt("npc_id");
				route.setNpcId(npcid);
				route.setMovePoint(rset.getString("move_point"));
				route.setChatText(rset.getString("chatText"));
				
				route.setMoveX(rset.getInt("move_x"));
				route.setMoveY(rset.getInt("move_y"));
				route.setMoveZ(rset.getInt("move_z"));
				route.setDelay(rset.getInt("delay"));
				route.setRunning(rset.getBoolean("running"));
				
				if (_routes.get(npcid) == null)
				{
					_routes.put(npcid, new ArrayList<L2NpcWalkerNode>());
				}
				_routes.get(npcid).add(route);
			}
			
			rset.close();
			statement.close();
			
			for (Object list : _routes.getValues())
				((ArrayList<?>)list).trimToSize();
			
			_log.info("WalkerRoutesTable: Loaded " + _routes.size() + " Npc Walker Routes.");
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "WalkerRoutesTable: Error while loading Npc Walkers Routes: " + e.getMessage(), e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
	}
	
	public List<L2NpcWalkerNode> getRouteForNpc(int id)
	{
		return _routes.get(id);
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final NpcWalkerRoutesTable _instance = new NpcWalkerRoutesTable();
	}
}
