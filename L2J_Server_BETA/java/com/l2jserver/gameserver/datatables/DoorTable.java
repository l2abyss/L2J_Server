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

import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.l2jserver.gameserver.engines.DocumentParser;
import com.l2jserver.gameserver.idfactory.IdFactory;
import com.l2jserver.gameserver.instancemanager.InstanceManager;
import com.l2jserver.gameserver.instancemanager.MapRegionManager;
import com.l2jserver.gameserver.model.StatsSet;
import com.l2jserver.gameserver.model.actor.instance.L2DoorInstance;
import com.l2jserver.gameserver.model.actor.templates.L2DoorTemplate;
import com.l2jserver.gameserver.pathfinding.AbstractNodeLoc;

public class DoorTable extends DocumentParser
{
	private static final Logger _log = Logger.getLogger(DoorTable.class.getName());
	
	private final TIntObjectHashMap<L2DoorInstance> _doors = new TIntObjectHashMap<>();
	private static final TIntObjectHashMap<Set<Integer>> _groups = new TIntObjectHashMap<>();
	private final TIntObjectHashMap<ArrayList<L2DoorInstance>> _regions = new TIntObjectHashMap<>();
	
	protected DoorTable()
	{
		load();
	}
	
	@Override
	public void load()
	{
		parseDatapackFile("data/doorData.xml");
	}
	
	public void reloadAll()
	{
		_doors.clear();
		_groups.clear();
		_regions.clear();
		load();
	}
	
	@Override
	protected void parseDocument()
	{
		NamedNodeMap attrs;
		Node att;
		StatsSet set;
		for (Node a = getCurrentDocument().getFirstChild(); a != null; a = a.getNextSibling())
		{
			if ("list".equalsIgnoreCase(a.getNodeName()))
			{
				for (Node b = a.getFirstChild(); b != null; b = b.getNextSibling())
				{
					if ("door".equalsIgnoreCase(b.getNodeName()))
					{
						attrs = b.getAttributes();
						set = new StatsSet();
						set.set("baseHpMax", 1);  // Avoid doors without HP value created dead due to default value 0 in L2CharTemplate
						for (int i = 0; i < attrs.getLength(); i++)
						{
							att = attrs.item(i);
							set.set(att.getNodeName(), att.getNodeValue());
						}
						makeDoor(set);
					}
				}
			}
		}
		
		_log.info("DoorTable: Loaded " + _doors.size() + " Door Templates for " + _regions.size() + " regions."); 
	}
	
	public void insertCollisionData(StatsSet set)
	{
		int posX, posY, nodeX, nodeY, height;
		height = set.getInteger("height");
		String[] pos = set.getString("node1").split(",");
		nodeX = Integer.parseInt(pos[0]);
		nodeY = Integer.parseInt(pos[1]);
		pos = set.getString("node2").split(",");
		posX = Integer.parseInt(pos[0]);
		posY = Integer.parseInt(pos[1]);
		int collisionRadius; // (max) radius for movement checks
		collisionRadius = Math.min(Math.abs(nodeX - posX), Math.abs(nodeY - posY));
		if (collisionRadius < 20)
			collisionRadius = 20;

		set.set("collision_radius", collisionRadius);
		set.set("collision_height", height);
	}
	
	/**
	 * @param set
	 */
	private void makeDoor(StatsSet set)
	{
		insertCollisionData(set);
		L2DoorTemplate template = new L2DoorTemplate(set);
		L2DoorInstance door = new L2DoorInstance(IdFactory.getInstance().getNextId(), template, set);
		door.setCurrentHp(door.getMaxHp());
		door.spawnMe(template.posX, template.posY, template.posZ);
		putDoor(door, MapRegionManager.getInstance().getMapRegionLocId(door.getX(), door.getY()));
	}

	public L2DoorTemplate getDoorTemplate(int doorId)
	{
		return _doors.get(doorId).getTemplate();
	}
	
	public L2DoorInstance getDoor(int doorId)
	{
		return _doors.get(doorId);
	}

	public void putDoor(L2DoorInstance door, int region)
	{
		_doors.put(door.getDoorId(), door);
		
		if (_regions.contains(region))
			_regions.get(region).add(door);
		else
		{
			final ArrayList<L2DoorInstance> list = new ArrayList<>();
			list.add(door);
			_regions.put(region, list);
		}
	}

	public static void addDoorGroup(String groupName, int doorId)
	{
		Set<Integer> set = _groups.get(groupName.hashCode());
		if (set == null)
		{
			set = new HashSet<>();
			set.add(doorId);
			_groups.put(groupName.hashCode(), set);
		}
		else
		{
			set.add(doorId);
		}
	}
	
	public static Set<Integer> getDoorsByGroup(String groupName)
	{
		return _groups.get(groupName.hashCode());
	}
	
	public L2DoorInstance[] getDoors()
	{
		return _doors.values(new L2DoorInstance[0]);
	}

	public boolean checkIfDoorsBetween(AbstractNodeLoc start, AbstractNodeLoc end, int instanceId)
	{
		return checkIfDoorsBetween(start.getX(), start.getY(), start.getZ(), end.getX(), end.getY(), end.getZ(), instanceId);
	}
	
	public boolean checkIfDoorsBetween(int x, int y, int z, int tx, int ty, int tz, int instanceId)
	{
		return checkIfDoorsBetween(x, y, z, tx, ty, tz, instanceId, false);
	}

	/**
	 * @param x 
	 * @param y 
	 * @param z 
	 * @param tx 
	 * @param ty 
	 * @param tz 
	 * @param instanceId 
	 * @param doubleFaceCheck 
	 * @return
	 *  
	 * TODO: remove geodata checks from door table and convert door nodes to geo zones
	 */
	public boolean checkIfDoorsBetween(int x, int y, int z, int tx, int ty, int tz, int instanceId, boolean doubleFaceCheck)
	{
		ArrayList<L2DoorInstance> allDoors;
		if (instanceId > 0 && InstanceManager.getInstance().getInstance(instanceId) != null)
			allDoors = InstanceManager.getInstance().getInstance(instanceId).getDoors();
		else
			allDoors = _regions.get(MapRegionManager.getInstance().getMapRegionLocId(x, y));
		
		if (allDoors == null)
			return false;
		
		for (L2DoorInstance doorInst : allDoors)
		{
			//check dead and open
			if (doorInst.isDead() || doorInst.getOpen() || !doorInst.checkCollision() || doorInst.getX(0) == 0)
				continue;
			
			boolean intersectFace = false;
			for (int i = 0; i < 4; i++)
			{
				int j = i + 1 < 4 ? i + 1 : 0;
				// lower part of the multiplier fraction, if it is 0 we avoid an error and also know that the lines are parallel
				int denominator = (ty - y) * (doorInst.getX(i) - doorInst.getX(j)) - (tx - x) * (doorInst.getY(i) - doorInst.getY(j));
				if (denominator == 0)
					continue;
				
				// multipliers to the equations of the lines. If they are lower than 0 or bigger than 1, we know that segments don't intersect
				float multiplier1 = (float)((doorInst.getX(j) - doorInst.getX(i)) * (y - doorInst.getY(i)) - (doorInst.getY(j) - doorInst.getY(i)) * (x - doorInst.getX(i))) / denominator;
				float multiplier2 = (float)((tx - x) * (y - doorInst.getY(i)) - (ty - y) * (x - doorInst.getX(i))) / denominator;
				if (multiplier1 >= 0 && multiplier1 <= 1 && multiplier2 >= 0 && multiplier2 <= 1)
				{
					int intersectZ = Math.round(z + multiplier1 * (tz - z));
					// now checking if the resulting point is between door's min and max z
					if (intersectZ > doorInst.getZMin() && intersectZ < doorInst.getZMax())
					{
						if (!doubleFaceCheck || intersectFace)
							return true;
						intersectFace = true;
					}
				}
			}
		}
		return false;
	}
	
	public static DoorTable getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final DoorTable _instance = new DoorTable();
	}
}
