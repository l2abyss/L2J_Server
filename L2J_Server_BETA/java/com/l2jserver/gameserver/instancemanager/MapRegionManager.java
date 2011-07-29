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
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.l2jserver.Config;
import com.l2jserver.gameserver.SevenSigns;
import com.l2jserver.gameserver.model.L2MapRegion;
import com.l2jserver.gameserver.model.L2Object;
import com.l2jserver.gameserver.model.Location;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.actor.instance.L2SiegeFlagInstance;
import com.l2jserver.gameserver.model.entity.Castle;
import com.l2jserver.gameserver.model.entity.ClanHall;
import com.l2jserver.gameserver.model.entity.Fort;
import com.l2jserver.gameserver.model.entity.Instance;
import com.l2jserver.gameserver.model.zone.type.L2ClanHallZone;
import com.l2jserver.gameserver.model.zone.type.L2RespawnZone;

import javolution.util.FastMap;

/**
 * @author Nyaran
 *
 */
public class MapRegionManager
{
	protected static final Logger _log = Logger.getLogger(MapRegionManager.class.getName());
	private static Map<String, L2MapRegion> _regions;
	
	private static final String FILE = "MapRegion.xml";
	
	public static enum TeleportWhereType
	{
		Castle,
		Castle_banish,
		ClanHall,
		ClanHall_banish,
		SiegeFlag,
		Town,
		Fortress,
		Fortress_banish,
		Territory,
		Territory_banish
	}
	
	public static MapRegionManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private MapRegionManager()
	{
		_regions = new FastMap<String, L2MapRegion>();
		
		try
		{
			load();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			_log.log(Level.SEVERE, "Failed loading MapRegion", e);
		}
	}
	
	private static void load() throws Exception
	{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setIgnoringComments(true);
		File file = new File(Config.DATAPACK_ROOT + "/data/" + FILE);
		if (file.exists())
		{
			Document doc = factory.newDocumentBuilder().parse(file);
			for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
			{
				if ("list".equalsIgnoreCase(n.getNodeName()))
				{
					for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					{
						if ("region".equalsIgnoreCase(d.getNodeName()))
						{
							NamedNodeMap attrs = d.getAttributes();
							Node att;
							String name = "";
							String town = "";
							int locId = -1;
							int castle = -1;
							int bbs = -1;
							
							att = attrs.getNamedItem("name");
							if (att == null)
							{
								_log.severe("Missing name for MapRegion, skipping");
								continue;
							}
							name = att.getNodeValue();
							
							att = attrs.getNamedItem("town");
							if (att == null)
							{
								_log.severe("Missing town for MapRegion name: " + name + ", skipping");
								continue;
							}
							town = att.getNodeValue();
							
							att = attrs.getNamedItem("locId");
							if (att == null)
							{
								_log.severe("Missing locId for MapRegion name: " + name + ", skipping");
								continue;
							}
							locId = Integer.parseInt(att.getNodeValue());
							
							att = attrs.getNamedItem("castle");
							if (att == null)
							{
								_log.severe("Missing castle for MapRegion name: " + name + ", skipping");
								continue;
							}
							castle = Integer.parseInt(att.getNodeValue());
							
							att = attrs.getNamedItem("bbs");
							if (att == null)
							{
								_log.severe("Missing bbs for MapRegion name: " + name + ", skipping");
								continue;
							}
							bbs = Integer.parseInt(att.getNodeValue());
							
							L2MapRegion region = new L2MapRegion(name, town, locId, castle, bbs);
							for (Node c = d.getFirstChild(); c != null; c = c.getNextSibling())
							{
								if ("respawnPoint".equalsIgnoreCase(c.getNodeName()))
								{
									attrs = c.getAttributes();
									int spawnX = Integer.parseInt(attrs.getNamedItem("X").getNodeValue());
									int spawnY = Integer.parseInt(attrs.getNamedItem("Y").getNodeValue());
									int spawnZ = Integer.parseInt(attrs.getNamedItem("Z").getNodeValue());
									
									Node val = attrs.getNamedItem("isOther");
									boolean other = val != null && Boolean.parseBoolean(val.getNodeValue());
									
									val = attrs.getNamedItem("isChaotic");
									boolean chaotic = val != null && Boolean.parseBoolean(val.getNodeValue());
									
									val = attrs.getNamedItem("isBanish");
									boolean banish = val != null && Boolean.parseBoolean(val.getNodeValue());
									
									if (other)
										region.addOtherSpawn(spawnX, spawnY, spawnZ);
									else if (chaotic)
										region.addChaoticSpawn(spawnX, spawnY, spawnZ);
									else if (banish)
										region.addBanishSpawn(spawnX, spawnY, spawnZ);
									else
										region.addSpawn(spawnX, spawnY, spawnZ);
								}
								else if ("map".equalsIgnoreCase(c.getNodeName()))
								{
									attrs = c.getAttributes();
									int mapX = Integer.parseInt(attrs.getNamedItem("X").getNodeValue());
									int mapY = Integer.parseInt(attrs.getNamedItem("Y").getNodeValue());
									
									region.addMap(mapX, mapY);
								}
								else if ("banned".equalsIgnoreCase(c.getNodeName()))
								{
									attrs = c.getAttributes();
									String race = attrs.getNamedItem("race").getNodeValue();
									String point = attrs.getNamedItem("point").getNodeValue();
									
									region.addBannedRace(race, point);
								}
							}
							_regions.put(name, region);
						}
					}
				}
			}
		}
		else
			_log.severe("MapRegion file (" + file.getAbsolutePath() + ") doesnt exists.");
	}
	
	public final L2MapRegion getMapRegion(int locX, int locY)
	{
		for (L2MapRegion region : _regions.values())
		{
			if (region.isZoneInRegion(getMapRegionX(locX), getMapRegionY(locY)))
				return region;
		}
		return null;
	}
	
	public final int getMapRegionId(int locX, int locY)
	{
		L2MapRegion region = getMapRegion(locX, locY);
		if (region != null)
			return region.getLocId();
		
		if (Config.DEBUG_PATH)
			_log.log(Level.WARNING, "MapRegionManager: Player outside map regions at X,Y=" + locX + "," + locY);
		return 0;
	}
	
	public final L2MapRegion getMapRegion(L2Object obj)
	{
		return getMapRegion(obj.getX(), obj.getY());
	}
	
	public final int getMapRegionId(L2Object obj)
	{
		return getMapRegionId(obj.getX(), obj.getY());
	}
	
	public final int getMapRegionX(int posX)
	{
		return (posX >> 15) + 9 + 11;// + centerTileX;
	}
	
	public final int getMapRegionY(int posY)
	{
		return (posY >> 15) + 10 + 8;// + centerTileX;
	}
	
	/**
	 * Get town name by character position
	 * @param townId
	 * @return String
	 */
	public String getClosestTownName(L2Character activeChar)
	{
		L2MapRegion region = getMapRegion(activeChar);
		
		if (region == null)
			return "Aden Castle Town";
		
		return region.getTown();
	}
	
	public int getAreaCastle(L2Character activeChar)
	{
		L2MapRegion region = getMapRegion(activeChar);
		
		if (region == null)
			return 0;
		
		return region.getCastle();
	}
	
	public Location getTeleToLocation(L2Character activeChar, TeleportWhereType teleportWhere)
	{
		int[] coord;
		
		if (activeChar instanceof L2PcInstance)
		{
			L2PcInstance player = ((L2PcInstance) activeChar);
			
			Castle castle = null;
			Fort fort = null;
			ClanHall clanhall = null;
			
			if (player.getClan() != null && !player.isFlyingMounted() && !player.isFlying()) // flying players in gracia cant use teleports to aden continent
			{
				// If teleport to clan hall
				if (teleportWhere == TeleportWhereType.ClanHall)
				{
					clanhall = ClanHallManager.getInstance().getClanHallByOwner(player.getClan());
					if (clanhall != null)
					{
						L2ClanHallZone zone = clanhall.getZone();
						if (zone != null && !player.isFlyingMounted())
						{
							if (player.getKarma() > 0)
								return zone.getChaoticSpawnLoc();
							else
								return zone.getSpawnLoc();
						}
					}
				}
				
				// If teleport to castle
				if (teleportWhere == TeleportWhereType.Castle)
				{
					castle = CastleManager.getInstance().getCastleByOwner(player.getClan());
					// Otherwise check if player is on castle or fortress ground
					// and player's clan is defender
					if (castle == null)
					{
						castle = CastleManager.getInstance().getCastle(player);
						if (!(castle != null && castle.getSiege().getIsInProgress() && castle.getSiege().getDefenderClan(player.getClan()) != null))
							castle = null;
					}
					
					if (castle != null && castle.getCastleId() > 0)
					{
						if (player.getKarma() > 0)
							return castle.getCastleZone().getChaoticSpawnLoc();
						else
							return castle.getCastleZone().getSpawnLoc();
					}
				}
				
				// If teleport to fortress
				if (teleportWhere == TeleportWhereType.Fortress)
				{
					fort = FortManager.getInstance().getFortByOwner(player.getClan());
					// Otherwise check if player is on castle or fortress ground
					// and player's clan is defender
					if (fort == null)
					{
						fort = FortManager.getInstance().getFort(player);
						if (!(fort != null && fort.getSiege().getIsInProgress() && fort.getOwnerClan() == player.getClan()))
							fort = null;
					}
					
					if (fort != null && fort.getFortId() > 0)
					{
						if (player.getKarma() > 0)
							return fort.getFortZone().getChaoticSpawnLoc();
						else
							return fort.getFortZone().getSpawnLoc();
					}
				}
				
				// If teleport to SiegeHQ
				if (teleportWhere == TeleportWhereType.SiegeFlag)
				{
					castle = CastleManager.getInstance().getCastle(player);
					fort = FortManager.getInstance().getFort(player);
					L2SiegeFlagInstance tw_flag = TerritoryWarManager.getInstance().getFlagForClan(player.getClan());
					if (tw_flag != null)
						return new Location(tw_flag.getX(), tw_flag.getY(), tw_flag.getZ());
					else if (castle != null)
					{
						if (castle.getSiege().getIsInProgress())
						{
							// Check if player's clan is attacker
							List<L2Npc> flags = castle.getSiege().getFlag(player.getClan());
							if (flags != null && !flags.isEmpty())
							{
								// Spawn to flag - Need more work to get player to the nearest flag
								L2Npc flag = flags.get(0);
								return new Location(flag.getX(), flag.getY(), flag.getZ());
							}
						}
						
					}
					else if (fort != null)
					{
						if (fort.getSiege().getIsInProgress())
						{
							// Check if player's clan is attacker
							List<L2Npc> flags = fort.getSiege().getFlag(player.getClan());
							if (flags != null && !flags.isEmpty())
							{
								// Spawn to flag - Need more work to get player to the nearest flag
								L2Npc flag = flags.get(0);
								return new Location(flag.getX(), flag.getY(), flag.getZ());
							}
						}
					}
				}
			}
			
			if (teleportWhere == TeleportWhereType.Castle_banish)
			{
				castle = CastleManager.getInstance().getCastle(player);
				if (castle != null)
					return castle.getCastleZone().getBanishSpawnLoc();
			}
			else if (teleportWhere == TeleportWhereType.Fortress_banish)
			{
				fort = FortManager.getInstance().getFort(activeChar);
				if (fort != null)
					return fort.getFortZone().getBanishSpawnLoc();
			}
			else if (teleportWhere == TeleportWhereType.ClanHall_banish)
			{
				clanhall = ClanHallManager.getInstance().getClanHall(activeChar);
				if (clanhall != null)
					return clanhall.getZone().getBanishSpawnLoc();
			}
			
			
			//Karma player land out of city
			if (player.getKarma() > 0)
			{
				try
				{
					L2RespawnZone zone = ZoneManager.getInstance().getZone(player, L2RespawnZone.class);
					if (zone != null)
						return getRestartRegion(activeChar, zone.getRespawnPoint((L2PcInstance) activeChar)).getChaoticSpawnLoc();
					else
						return getMapRegion(activeChar).getChaoticSpawnLoc();
				}
				catch (Exception e)
				{
					if (player.isFlyingMounted()) // prevent flying players to teleport outside of gracia
						return _regions.get("union_base_of_kserth").getChaoticSpawnLoc();
					else
						return _regions.get("talking_island_town").getChaoticSpawnLoc();
				}
			}
			
			//Checking if needed to be respawned in "far" town from the castle;
			castle = CastleManager.getInstance().getCastle(player);
			if (castle != null)
			{
				if (castle.getSiege().getIsInProgress())
				{
					// Check if player's clan is participating
					if ((castle.getSiege().checkIsDefender(player.getClan()) || castle.getSiege().checkIsAttacker(player.getClan())) && SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE) == SevenSigns.CABAL_DAWN)
						return castle.getCastleZone().getOtherSpawnLoc();
				}
			}
			
			// Checking if in an instance
			if (player.getInstanceId() > 0)
			{
				Instance inst = InstanceManager.getInstance().getInstance(player.getInstanceId());
				if (inst != null)
				{
					coord = inst.getSpawnLoc();
					if (coord[0] != 0 && coord[1] != 0 && coord[2] != 0)
						return new Location(coord[0], coord[1], coord[2]);
				}
			}
		}
		
		// Get the nearest town
		try
		{
			L2RespawnZone zone = ZoneManager.getInstance().getZone(activeChar, L2RespawnZone.class);
			if (zone != null)
				return getRestartRegion(activeChar, zone.getRespawnPoint((L2PcInstance) activeChar)).getSpawnLoc();
			else
				return getMapRegion(activeChar).getSpawnLoc();
		}
		catch (Exception e)
		{
			// port to the Talking Island if no closest town found
			if (Config.DEBUG)
				_log.log(Level.WARNING, "Not defined respawn point for coords loc X=" + activeChar.getX() + " Y=" + activeChar.getY() + " Z=" + activeChar.getZ());
			return _regions.get("talking_island_town").getSpawnLoc();
		}
	}
	
	public L2MapRegion getRestartRegion(L2Character activeChar, String point)
	{
		try
		{
			L2PcInstance player = ((L2PcInstance) activeChar);
			L2MapRegion region = _regions.get(point);
			
			if (region.getBannedRace().containsKey(player.getRace()))
				getRestartRegion(player, region.getBannedRace().get(player.getRace()));
			return region;
		}
		catch (Exception e)
		{
			return _regions.get("talking_island_town");
		}
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final MapRegionManager _instance = new MapRegionManager();
	}
	
}
