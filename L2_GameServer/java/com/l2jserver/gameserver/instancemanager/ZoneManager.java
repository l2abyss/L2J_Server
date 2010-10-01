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

import java.io.File;
import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.l2jserver.Config;
import com.l2jserver.L2DatabaseFactory;
import com.l2jserver.gameserver.model.L2Object;
import com.l2jserver.gameserver.model.L2World;
import com.l2jserver.gameserver.model.L2WorldRegion;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.zone.L2SpawnZone;
import com.l2jserver.gameserver.model.zone.L2ZoneType;
import com.l2jserver.gameserver.model.zone.form.ZoneCuboid;
import com.l2jserver.gameserver.model.zone.form.ZoneCylinder;
import com.l2jserver.gameserver.model.zone.form.ZoneNPoly;
import com.l2jserver.gameserver.model.zone.type.L2ArenaZone;
import com.l2jserver.gameserver.model.zone.type.L2BossZone;
import com.l2jserver.gameserver.model.zone.type.L2OlympiadStadiumZone;

/**
 * This class manages the zones
 * 
 * @author durgus
 */
public class ZoneManager
{
	private static final Logger _log = Logger.getLogger(ZoneManager.class.getName());
	
	//private final FastMap<Integer, L2ZoneType> _zones = new FastMap<Integer, L2ZoneType>();
	private final Map<Class<? extends L2ZoneType>, Map<Integer, ? extends L2ZoneType>> _classZones = new FastMap<Class<? extends L2ZoneType>, Map<Integer, ? extends L2ZoneType>>();
	private int _lastDynamicId = 300000;
	
	public static final ZoneManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	// =========================================================
	// Data Field
	
	// =========================================================
	// Constructor
	private ZoneManager()
	{
		load();
	}
	
	public void reload()
	{
		// int zoneCount = 0;
		
		// Get the world regions
		int count = 0;
		L2WorldRegion[][] worldRegions = L2World.getInstance().getAllWorldRegions();
		for (int x = 0; x < worldRegions.length; x++)
		{
			for (int y = 0; y < worldRegions[x].length; y++)
			{
				worldRegions[x][y].getZones().clear();
				count++;
			}
		}
		GrandBossManager.getInstance().getZones().clear();
		_log.info("Removed zones in " + count + " regions.");
		// Load the zones
		load();
	}
	
	// =========================================================
	// Method - Private
	
	private final void load()
	{
		_log.info("Loading zones...");
		Connection con = null;
		PreparedStatement statement = null;
		_classZones.clear();
		
		// Get the world regions
		L2WorldRegion[][] worldRegions = L2World.getInstance().getAllWorldRegions();
		
		// Load the zone xml
		try
		{
			// Get a sql connection here
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT x,y FROM zone_vertices WHERE id=? ORDER BY 'order' ASC ");
			
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			
			final File dir = new File(Config.DATAPACK_ROOT, "data/zones");
			if (!dir.exists())
			{
				_log.config("Dir " + dir.getAbsolutePath() + " not exists");
				return;
			}
			
			File[] files = dir.listFiles();
			FastList<File> hash = new FastList<File>(files.length);
			for (File f : files)
			{
				// default file first
				if ("zone.xml".equalsIgnoreCase(f.getName()))
					hash.addFirst(f);
				else if (f.getName().endsWith(".xml"))
					hash.add(f);
			}
			
			Document doc;
			NamedNodeMap attrs;
			Node attribute;
			int[][] coords;
			int zoneId, minZ, maxZ;
			String zoneType, zoneShape;
			
			for (File f : hash)
			{
				doc = factory.newDocumentBuilder().parse(f);
				for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
				{
					if ("list".equalsIgnoreCase(n.getNodeName()))
					{
						attrs = n.getAttributes();
						attribute = attrs.getNamedItem("enabled");
						if (attribute != null && !Boolean.parseBoolean(attribute.getNodeValue()))
							continue;
						
						for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
						{
							if ("zone".equalsIgnoreCase(d.getNodeName()))
							{
								attrs = d.getAttributes();
								
								attribute = attrs.getNamedItem("id");
								if (attribute != null)
									zoneId = Integer.parseInt(attribute.getNodeValue());
								else
									zoneId = _lastDynamicId++;
								
								attribute = attrs.getNamedItem("minZ");
								if (attribute != null)
									minZ = Integer.parseInt(attribute.getNodeValue());
								else
								{
									_log.warning("ZoneData: Missing minZ for zone: " + zoneId + " in file: " + f.getName());
									continue;
								}
								
								attribute = attrs.getNamedItem("maxZ");
								if (attribute != null)
									maxZ = Integer.parseInt(attribute.getNodeValue());
								else
								{
									_log.warning("ZoneData: Missing maxZ for zone: " + zoneId + " in file: " + f.getName());
									continue;
								}
								
								attribute = attrs.getNamedItem("type");
								if (attribute != null)
									zoneType = attribute.getNodeValue();
								else
								{
									_log.warning("ZoneData: Missing type for zone: " + zoneId + " in file: " + f.getName());
									continue;
								}
								
								attribute = attrs.getNamedItem("shape");
								if (attribute != null)
									zoneShape = attribute.getNodeValue();
								else
								{
									_log.warning("ZoneData: Missing shape for zone: " + zoneId + " in file: " + f.getName());
									continue;
								}
								
								// Create the zone
								Class<?> newZone;
								try
								{
									newZone = Class.forName("com.l2jserver.gameserver.model.zone.type.L2" + zoneType);
								}
								catch (ClassNotFoundException e)
								{
									_log.warning("ZoneData: No such zone type: " + zoneType + " in file: " + f.getName());
									continue;
								}
								
								Constructor<?> zoneConstructor = newZone.getConstructor(int.class);
								L2ZoneType temp = (L2ZoneType) zoneConstructor.newInstance(zoneId);
								
								// Get the zone shape from sql
								try
								{
									coords = null;
									int[] point;
									FastList<int[]> rs = FastList.newInstance();
									try
									{
										// loading from XML first
										for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
										{
											if ("node".equalsIgnoreCase(cd.getNodeName()))
											{
												attrs = cd.getAttributes();
												point = new int[2];
												point[0] = Integer.parseInt(attrs.getNamedItem("X").getNodeValue());
												point[1] = Integer.parseInt(attrs.getNamedItem("Y").getNodeValue());
												rs.add(point);
											}
										}
										
										// does not try to load dynamic zoneId from sql
										if (rs.size() == 0 && zoneId < 300000)
										{
											// loading from SQL
											try
											{
												statement.setInt(1, zoneId);
												ResultSet rset = statement.executeQuery();
												while (rset.next())
												{
													point = new int[2];
													point[0] = rset.getInt("x");
													point[1] = rset.getInt("y");
													rs.add(point);
												}
												rset.close();
											}
											finally
											{
												statement.clearParameters();
											}
										}
										
										coords = rs.toArray(new int[rs.size()][]);
									}
									finally
									{
										FastList.recycle(rs);
									}
									
									if (coords == null || coords.length == 0)
									{
										_log.warning("ZoneData: missing data for zone: " + zoneId +" in both XML and SQL, file: " + f.getName());
										continue;
									}
									
									// Create this zone. Parsing for cuboids is a
									// bit different than for other polygons
									// cuboids need exactly 2 points to be defined.
									// Other polygons need at least 3 (one per
									// vertex)
									if (zoneShape.equalsIgnoreCase("Cuboid"))
									{
										if (coords.length == 2)
											temp.setZone(new ZoneCuboid(coords[0][0], coords[1][0], coords[0][1], coords[1][1], minZ, maxZ));
										else
										{
											_log.warning("ZoneData: Missing cuboid vertex in sql data for zone: " + zoneId + " in file: " + f.getName());
											continue;
										}
									}
									else if (zoneShape.equalsIgnoreCase("NPoly"))
									{
										// nPoly needs to have at least 3 vertices
										if (coords.length > 2)
										{
											final int[] aX = new int[coords.length];
											final int[] aY = new int[coords.length];
											for (int i = 0; i < coords.length; i++)
											{
												aX[i] = coords[i][0];
												aY[i] = coords[i][1];
											}
											temp.setZone(new ZoneNPoly(aX, aY, minZ, maxZ));
										}
										else
										{
											_log.warning("ZoneData: Bad data for zone: " + zoneId + " in file: " + f.getName());
											continue;
										}
									}
									else if (zoneShape.equalsIgnoreCase("Cylinder"))
									{
										// A Cylinder zone requires a center point
										// at x,y and a radius
										attrs = d.getAttributes();
										final int zoneRad = Integer.parseInt(attrs.getNamedItem("rad").getNodeValue());
										if (coords.length == 1 && zoneRad > 0)
											temp.setZone(new ZoneCylinder(coords[0][0], coords[0][1], minZ, maxZ, zoneRad));
										else
										{
											_log.warning("ZoneData: Bad data for zone: " + zoneId + " in file: " + f.getName());
											continue;
										}
									}
									else
									{
										_log.warning("ZoneData: Unknown shape: " + zoneShape + " in file: " + f.getName());
										continue;
									}
								}
								catch (Exception e)
								{
									_log.log(Level.WARNING, "ZoneData: Failed to load zone " + zoneId + " coordinates: " + e.getMessage(), e);
								}
								
								// Check for additional parameters
								for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
								{
									if ("stat".equalsIgnoreCase(cd.getNodeName()))
									{
										attrs = cd.getAttributes();
										String name = attrs.getNamedItem("name").getNodeValue();
										String val = attrs.getNamedItem("val").getNodeValue();
										
										temp.setParameter(name, val);
									}
									else if ("spawn".equalsIgnoreCase(cd.getNodeName()) && temp instanceof L2SpawnZone)
									{
										attrs = cd.getAttributes();
										int spawnX = Integer.parseInt(attrs.getNamedItem("X").getNodeValue());
										int spawnY = Integer.parseInt(attrs.getNamedItem("Y").getNodeValue());
										int spawnZ = Integer.parseInt(attrs.getNamedItem("Z").getNodeValue());
										
										Node val = attrs.getNamedItem("isChaotic");
										if (val != null && Boolean.parseBoolean(val.getNodeValue()))
											((L2SpawnZone) temp).addChaoticSpawn(spawnX, spawnY, spawnZ);
										else
											((L2SpawnZone) temp).addSpawn(spawnX, spawnY, spawnZ);
									}
								}
								if (checkId(zoneId))
									_log.config("Caution: Zone (" + zoneId + ") from file: " + f.getName() + " overrides previos definition.");
								
								addZone(zoneId, temp);
								
								// Register the zone into any world region it
								// intersects with...
								// currently 11136 test for each zone :>
								int ax, ay, bx, by;
								for (int x = 0; x < worldRegions.length; x++)
								{
									for (int y = 0; y < worldRegions[x].length; y++)
									{
										ax = (x - L2World.OFFSET_X) << L2World.SHIFT_BY;
										bx = ((x + 1) - L2World.OFFSET_X) << L2World.SHIFT_BY;
										ay = (y - L2World.OFFSET_Y) << L2World.SHIFT_BY;
										by = ((y + 1) - L2World.OFFSET_Y) << L2World.SHIFT_BY;
										
										if (temp.getZone().intersectsRectangle(ax, bx, ay, by))
										{
											if (Config.DEBUG)
											{
												_log.info("Zone (" + zoneId + ") added to: " + x + " " + y);
											}
											worldRegions[x][y].addZone(temp);
										}
									}
								}
								
								// Special managers for granbosses...
								if (temp instanceof L2BossZone)
									GrandBossManager.getInstance().addZone((L2BossZone) temp);
							}
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Error while loading zones.", e);
			return;
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
		
		_log.info("Done: loaded " + _classZones.size() + " zone classes and "+getSize()+" zones.");
	}
	
	public int getSize()
	{
		int i = 0;
		for (Map<Integer, ? extends L2ZoneType> map : _classZones.values())
		{
			i += map.size();
		}
		return i;
	}
	
	public boolean checkId(int id)
	{
		for (Map<Integer, ? extends L2ZoneType> map : _classZones.values())
		{
			if (map.containsKey(id))
				return true;
		}
		return false;
	}
	
	/**
	 * Add new zone
	 *
	 * @param zone
	 */
	@SuppressWarnings("unchecked")
	public <T extends L2ZoneType> void addZone(Integer id, T zone)
	{
		//_zones.put(id, zone);
		Map<Integer, T> map = (Map<Integer, T>) _classZones.get(zone.getClass());
		if (map == null)
		{
			map = new FastMap<Integer, T>();
			map.put(id, zone);
			_classZones.put(zone.getClass(), map);
		}
		else
			map.put(id, zone);
	}
	
	/**
	 * Returns all zones registered with the ZoneManager.
	 * To minimise iteration processing retrieve zones from L2WorldRegion for a specific location instead.
	 * @return zones
	 * @see #getAllZones(Class)
	 */
	@Deprecated
	public Collection<L2ZoneType> getAllZones()
	{
		FastList<L2ZoneType> zones = new FastList<L2ZoneType>();
		for (Map<Integer, ? extends L2ZoneType> map : _classZones.values())
		{
			zones.addAll(map.values());
		}
		return zones;
	}
	
	/**
	 * Return all zones by class type
	 * @param <T>
	 * @param zoneType Zone class
	 * @return Collection of zones
	 */
	@SuppressWarnings("unchecked")
	public <T extends L2ZoneType> Collection<T> getAllZones(Class<T> zoneType)
	{
		return (Collection<T>) _classZones.get(zoneType).values();
	}
	
	/**
	 * Get zone by ID
	 * @param id
	 * @return
	 * @see #getZoneById(int, Class)
	 */
	public L2ZoneType getZoneById(int id)
	{
		for (Map<Integer, ? extends L2ZoneType> map : _classZones.values())
		{
			if (map.containsKey(id))
				return map.get(id);
		}
		return null;
	}
	
	/**
	 * Get zone by ID and zone class
	 * @param id
	 * @param zoneType
	 * @return zone
	 */
	@SuppressWarnings("unchecked")
	public <T extends L2ZoneType> T getZoneById(int id, Class<T> zoneType)
	{
		return (T) _classZones.get(zoneType).get(id);
	}
	
	/**
	 * Returns all zones from where the object is located
	 *
	 * @param object
	 * @return zones
	 */
	public FastList<L2ZoneType> getZones(L2Object object)
	{
		return getZones(object.getX(), object.getY(), object.getZ());
	}
	
	/**
	 * Returns zone from where the object is located by type
	 *
	 * @param object
	 * @param type
	 * @return zone
	 */
	public <T extends L2ZoneType> T getZone(L2Object object, Class<T> type)
	{
		if (object == null)
			return null;
		return getZone(object.getX(), object.getY(), object.getZ(), type);
	}
	
	/**
	 * Returns all zones from given coordinates (plane)
	 * 
	 * @param x
	 * @param y
	 * @return zones
	 */
	public FastList<L2ZoneType> getZones(int x, int y)
	{
		L2WorldRegion region = L2World.getInstance().getRegion(x, y);
		FastList<L2ZoneType> temp = new FastList<L2ZoneType>();
		for (L2ZoneType zone : region.getZones())
		{
			if (zone.isInsideZone(x, y))
				temp.add(zone);
		}
		return temp;
	}
	
	/**
	 * Returns all zones from given coordinates
	 *
	 * @param x
	 * @param y
	 * @param z
	 * @return zones
	 */
	public FastList<L2ZoneType> getZones(int x, int y, int z)
	{
		L2WorldRegion region = L2World.getInstance().getRegion(x, y);
		FastList<L2ZoneType> temp = new FastList<L2ZoneType>();
		for (L2ZoneType zone : region.getZones())
		{
			if (zone.isInsideZone(x, y, z))
				temp.add(zone);
		}
		return temp;
	}
	
	/**
	 * Returns zone from given coordinates
	 * 
	 * @param x
	 * @param y
	 * @param z
	 * @param type
	 * @return zone
	 */
	@SuppressWarnings("unchecked")
	public <T extends L2ZoneType> T getZone(int x, int y, int z, Class<T> type)
	{
		L2WorldRegion region = L2World.getInstance().getRegion(x, y);
		for (L2ZoneType zone : region.getZones())
		{
			if (zone.isInsideZone(x, y, z) && type.isInstance(zone))
				return (T) zone;
		}
		return null;
	}
	
	public final L2ArenaZone getArena(L2Character character)
	{
		if (character == null)
			return null;
		
		for (L2ZoneType temp : ZoneManager.getInstance().getZones(character.getX(), character.getY(), character.getZ()))
		{
			if (temp instanceof L2ArenaZone && temp.isCharacterInZone(character))
				return ((L2ArenaZone) temp);
		}
		
		return null;
	}
	
	public final L2OlympiadStadiumZone getOlympiadStadium(L2Character character)
	{
		if (character == null)
			return null;
		
		for (L2ZoneType temp : ZoneManager.getInstance().getZones(character.getX(), character.getY(), character.getZ()))
		{
			if (temp instanceof L2OlympiadStadiumZone && temp.isCharacterInZone(character))
				return ((L2OlympiadStadiumZone) temp);
		}
		return null;
	}
	
	/**
	 * For testing purposes only
	 * @param <T>
	 * @param obj
	 * @param type
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T extends L2ZoneType> T getClosestZone(L2Object obj, Class<T> type)
	{
		T zone = getZone(obj, type);
		if (zone == null)
		{
			double closestdis = Double.MAX_VALUE;
			for (T temp : (Collection<T>) _classZones.get(type).values())
			{
				double distance =  temp.getDistanceToZone(obj);
				if (distance < closestdis)
				{
					closestdis = distance;
					zone = temp;
				}
			}
			return zone;
		}
		else
			return zone;
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final ZoneManager _instance = new ZoneManager();
	}
}
