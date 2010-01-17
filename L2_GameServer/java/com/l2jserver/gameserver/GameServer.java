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

package com.l2jserver.gameserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;


import org.mmocore.network.SelectorConfig;
import org.mmocore.network.SelectorThread;

import com.l2jserver.Config;
import com.l2jserver.L2DatabaseFactory;
import com.l2jserver.Server;
import com.l2jserver.gameserver.cache.CrestCache;
import com.l2jserver.gameserver.cache.HtmCache;
import com.l2jserver.gameserver.datatables.AccessLevels;
import com.l2jserver.gameserver.datatables.AdminCommandAccessRights;
import com.l2jserver.gameserver.datatables.ArmorSetsTable;
import com.l2jserver.gameserver.datatables.AugmentationData;
import com.l2jserver.gameserver.datatables.CharNameTable;
import com.l2jserver.gameserver.datatables.CharTemplateTable;
import com.l2jserver.gameserver.datatables.ClanTable;
import com.l2jserver.gameserver.datatables.DoorTable;
import com.l2jserver.gameserver.datatables.EnchantHPBonusData;
import com.l2jserver.gameserver.datatables.EventDroplist;
import com.l2jserver.gameserver.datatables.ExtractableItemsData;
import com.l2jserver.gameserver.datatables.ExtractableSkillsData;
import com.l2jserver.gameserver.datatables.FishTable;
import com.l2jserver.gameserver.datatables.HelperBuffTable;
import com.l2jserver.gameserver.datatables.HennaTable;
import com.l2jserver.gameserver.datatables.HennaTreeTable;
import com.l2jserver.gameserver.datatables.HeroSkillTable;
import com.l2jserver.gameserver.datatables.ItemTable;
import com.l2jserver.gameserver.datatables.PetDataTable;
import com.l2jserver.gameserver.datatables.LevelUpData;
import com.l2jserver.gameserver.datatables.MapRegionTable;
import com.l2jserver.gameserver.datatables.MerchantPriceConfigTable;
import com.l2jserver.gameserver.datatables.NobleSkillTable;
import com.l2jserver.gameserver.datatables.NpcBufferTable;
import com.l2jserver.gameserver.datatables.NpcTable;
import com.l2jserver.gameserver.datatables.NpcWalkerRoutesTable;
import com.l2jserver.gameserver.datatables.PetSkillsTable;
import com.l2jserver.gameserver.datatables.ResidentialSkillTable;
import com.l2jserver.gameserver.datatables.SkillSpellbookTable;
import com.l2jserver.gameserver.datatables.SkillTable;
import com.l2jserver.gameserver.datatables.SkillTreeTable;
import com.l2jserver.gameserver.datatables.SpawnTable;
import com.l2jserver.gameserver.datatables.StaticObjects;
import com.l2jserver.gameserver.datatables.SummonItemsData;
import com.l2jserver.gameserver.datatables.TeleportLocationTable;
import com.l2jserver.gameserver.geoeditorcon.GeoEditorListener;
import com.l2jserver.gameserver.handler.AdminCommandHandler;
import com.l2jserver.gameserver.handler.ChatHandler;
import com.l2jserver.gameserver.handler.ItemHandler;
import com.l2jserver.gameserver.handler.SkillHandler;
import com.l2jserver.gameserver.handler.UserCommandHandler;
import com.l2jserver.gameserver.handler.VoicedCommandHandler;
import com.l2jserver.gameserver.idfactory.IdFactory;
import com.l2jserver.gameserver.instancemanager.AirShipManager;
import com.l2jserver.gameserver.instancemanager.AuctionManager;
import com.l2jserver.gameserver.instancemanager.BoatManager;
import com.l2jserver.gameserver.instancemanager.CastleManager;
import com.l2jserver.gameserver.instancemanager.CastleManorManager;
import com.l2jserver.gameserver.instancemanager.ClanHallManager;
import com.l2jserver.gameserver.instancemanager.CoupleManager;
import com.l2jserver.gameserver.instancemanager.CursedWeaponsManager;
import com.l2jserver.gameserver.instancemanager.DayNightSpawnManager;
import com.l2jserver.gameserver.instancemanager.DimensionalRiftManager;
import com.l2jserver.gameserver.instancemanager.FortManager;
import com.l2jserver.gameserver.instancemanager.FortSiegeManager;
import com.l2jserver.gameserver.instancemanager.FourSepulchersManager;
import com.l2jserver.gameserver.instancemanager.GrandBossManager;
import com.l2jserver.gameserver.instancemanager.InstanceManager;
import com.l2jserver.gameserver.instancemanager.ItemsOnGroundManager;
import com.l2jserver.gameserver.instancemanager.MailManager;
import com.l2jserver.gameserver.instancemanager.MercTicketManager;
import com.l2jserver.gameserver.instancemanager.PetitionManager;
import com.l2jserver.gameserver.instancemanager.QuestManager;
import com.l2jserver.gameserver.instancemanager.RaidBossPointsManager;
import com.l2jserver.gameserver.instancemanager.RaidBossSpawnManager;
import com.l2jserver.gameserver.instancemanager.SiegeManager;
import com.l2jserver.gameserver.instancemanager.TransformationManager;
import com.l2jserver.gameserver.instancemanager.ZoneManager;
import com.l2jserver.gameserver.model.AutoChatHandler;
import com.l2jserver.gameserver.model.AutoSpawnHandler;
import com.l2jserver.gameserver.model.L2Manor;
import com.l2jserver.gameserver.model.L2Multisell;
import com.l2jserver.gameserver.model.L2World;
import com.l2jserver.gameserver.model.entity.Hero;
import com.l2jserver.gameserver.model.entity.TvTManager;
import com.l2jserver.gameserver.model.olympiad.Olympiad;
import com.l2jserver.gameserver.network.L2GameClient;
import com.l2jserver.gameserver.network.L2GamePacketHandler;
import com.l2jserver.gameserver.network.communityserver.CommunityServerThread;
import com.l2jserver.gameserver.pathfinding.PathFinding;
import com.l2jserver.gameserver.script.faenor.FaenorScriptEngine;
import com.l2jserver.gameserver.scripting.CompiledScriptCache;
import com.l2jserver.gameserver.scripting.L2ScriptEngineManager;
import com.l2jserver.gameserver.taskmanager.AutoAnnounceTaskManager;
import com.l2jserver.gameserver.taskmanager.KnownListUpdateTaskManager;
import com.l2jserver.gameserver.taskmanager.TaskManager;
import com.l2jserver.gameserver.util.DynamicExtension;
import com.l2jserver.status.Status;
import com.l2jserver.util.DeadLockDetector;
import com.l2jserver.util.IPv4Filter;

/**
 * This class ...
 * 
 * @version $Revision: 1.29.2.15.2.19 $ $Date: 2005/04/05 19:41:23 $
 */
public class GameServer
{
	private static final Logger _log = Logger.getLogger(GameServer.class.getName());
	
	private final SelectorThread<L2GameClient> _selectorThread;
	private final DeadLockDetector _deadDetectThread;
	private final IdFactory _idFactory;
	public static GameServer gameServer;
	private LoginServerThread _loginThread;
	private static Status _statusServer;
	public static final Calendar dateTimeServerStarted = Calendar.getInstance();
	
	public long getUsedMemoryMB()
	{
		return (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576; // ;
	}
	
	public SelectorThread<L2GameClient> getSelectorThread()
	{
		return _selectorThread;
	}
	
	public DeadLockDetector getDeadLockDetectorThread()
	{
		return _deadDetectThread;
	}
	
	public GameServer() throws Exception
	{
		long serverLoadStart = System.currentTimeMillis();
		
		gameServer = this;
		_log.finest("used mem:" + getUsedMemoryMB() + "MB");
		
		if (Config.SERVER_VERSION != null)
		{
			_log.info("L2J Server Version:    " + Config.SERVER_VERSION);
		}
		if (Config.DATAPACK_VERSION != null)
		{
			_log.info("L2J Datapack Version:  " + Config.DATAPACK_VERSION);
		}
		
		_idFactory = IdFactory.getInstance();
		
		if (!_idFactory.isInitialized())
		{
			_log.severe("Could not read object IDs from DB. Please Check Your Data.");
			throw new Exception("Could not initialize the ID factory");
		}
		
		ThreadPoolManager.getInstance();
		
		new File(Config.DATAPACK_ROOT, "data/crests").mkdirs();
		new File("log/game").mkdirs();
		
		// load script engines
		L2ScriptEngineManager.getInstance();
		
		// start game time control early
		GameTimeController.getInstance();
		
		// keep the references of Singletons to prevent garbage collection
		CharNameTable.getInstance();
		SkillTable.getInstance();
		
		ItemTable.getInstance();
		if (!ItemTable.getInstance().isInitialized())
		{
			_log.severe("Could not find the extraced files. Please Check Your Data.");
			throw new Exception("Could not initialize the item table");
		}
		
		// Load clan hall data before zone data and doors table
		ClanHallManager.getInstance();
		
		ExtractableItemsData.getInstance();
		ExtractableSkillsData.getInstance();
		SummonItemsData.getInstance();
		ZoneManager.getInstance();
		MerchantPriceConfigTable.getInstance().loadInstances();
		EnchantHPBonusData.getInstance();
		TradeController.getInstance();
		L2Multisell.getInstance();
		InstanceManager.getInstance();
		
		if (Config.ALLOW_NPC_WALKERS)
		{
			NpcWalkerRoutesTable.getInstance().load();
		}
		
		NpcBufferTable.getInstance();
		
		RecipeController.getInstance();
		
		SkillTreeTable.getInstance();
		PetSkillsTable.getInstance();
		ArmorSetsTable.getInstance();
		FishTable.getInstance();
		SkillSpellbookTable.getInstance();
		CharTemplateTable.getInstance();
		NobleSkillTable.getInstance();
		HeroSkillTable.getInstance();
		ResidentialSkillTable.getInstance();
		
		// Call to load caches
		HtmCache.getInstance();
		CrestCache.getInstance();
		
		ClanTable.getInstance();
		
		NpcTable.getInstance();
		if (!NpcTable.getInstance().isInitialized())
		{
			_log.severe("Could not find the extraced files. Please Check Your Data.");
			throw new Exception("Could not initialize the npc table");
		}
		
		HennaTable.getInstance();
		HennaTreeTable.getInstance();
		HelperBuffTable.getInstance();
		
		GeoData.getInstance();
		if (Config.GEODATA == 2)
			PathFinding.getInstance();
		
		CastleManager.getInstance().loadInstances();
		SiegeManager.getInstance().getSieges();
		FortManager.getInstance().loadInstances();
		FortSiegeManager.getInstance();
		
		TeleportLocationTable.getInstance();
		LevelUpData.getInstance();
		L2World.getInstance();
		SpawnTable.getInstance();
		RaidBossSpawnManager.getInstance();
		DayNightSpawnManager.getInstance().notifyChangeMode();
		GrandBossManager.getInstance().initZones();
		RaidBossPointsManager.init();
		FourSepulchersManager.getInstance().init();
		DimensionalRiftManager.getInstance();
		Announcements.getInstance();
		MapRegionTable.getInstance();
		EventDroplist.getInstance();
		
		DoorTable.getInstance();
		StaticObjects.getInstance();
		
		/** Load Manor data */
		L2Manor.getInstance();
		
		/** Load Manager */
		AuctionManager.getInstance();
		BoatManager.getInstance();
		CastleManorManager.getInstance();
		MercTicketManager.getInstance();
		// PartyCommandManager.getInstance();
		PetitionManager.getInstance();
		QuestManager.getInstance();
		TransformationManager.getInstance();
		AirShipManager.getInstance();
		
		try
		{
			_log.info("Loading Server Scripts");
			File scripts = new File(Config.DATAPACK_ROOT + "/data/scripts.cfg");
			if (!Config.ALT_DEV_NO_QUESTS)
				L2ScriptEngineManager.getInstance().executeScriptList(scripts);
		}
		catch (IOException ioe)
		{
			_log.severe("Failed loading scripts.cfg, no script going to be loaded");
		}
		try
		{
			CompiledScriptCache compiledScriptCache = L2ScriptEngineManager.getInstance().getCompiledScriptCache();
			if (compiledScriptCache == null)
			{
				_log.info("Compiled Scripts Cache is disabled.");
			}
			else
			{
				compiledScriptCache.purge();
				
				if (compiledScriptCache.isModified())
				{
					compiledScriptCache.save();
					_log.info("Compiled Scripts Cache was saved.");
				}
				else
				{
					_log.info("Compiled Scripts Cache is up-to-date.");
				}
			}
			
		}
		catch (IOException e)
		{
			_log.log(Level.SEVERE, "Failed to store Compiled Scripts Cache.", e);
		}
		QuestManager.getInstance().report();
		TransformationManager.getInstance().report();
		
		AugmentationData.getInstance();
		if (Config.SAVE_DROPPED_ITEM)
			ItemsOnGroundManager.getInstance();
		
		if (Config.AUTODESTROY_ITEM_AFTER > 0 || Config.HERB_AUTO_DESTROY_TIME > 0)
			ItemsAutoDestroy.getInstance();
		
		MonsterRace.getInstance();
		
		SevenSigns.getInstance().spawnSevenSignsNPC();
		SevenSignsFestival.getInstance();
		AutoSpawnHandler.getInstance();
		AutoChatHandler.getInstance();
		
		Olympiad.getInstance();
		Hero.getInstance();
		FaenorScriptEngine.getInstance();
		// Init of a cursed weapon manager
		CursedWeaponsManager.getInstance();
		
		_log.log(Level.CONFIG, "AutoChatHandler: Loaded " + AutoChatHandler.getInstance().size() + " handlers in total.");
		_log.log(Level.CONFIG, "AutoSpawnHandler: Loaded " + AutoSpawnHandler.getInstance().size() + " handlers in total.");
		
		AdminCommandHandler.getInstance();
		ChatHandler.getInstance();
		ItemHandler.getInstance();
		SkillHandler.getInstance();
		UserCommandHandler.getInstance();
		VoicedCommandHandler.getInstance();
		
		AccessLevels.getInstance();
		AdminCommandAccessRights.getInstance();
		
		if (Config.L2JMOD_ALLOW_WEDDING)
			CoupleManager.getInstance();
		
		TaskManager.getInstance();
		
		GmListTable.getInstance();
		
		// read pet stats from db
		PetDataTable.getInstance().loadPetsData();
		
		MerchantPriceConfigTable.getInstance().updateReferences();
		CastleManager.getInstance().activateInstances();
		FortManager.getInstance().activateInstances();

		if (Config.ALLOW_MAIL)
			MailManager.getInstance();

		Universe.getInstance();
		
		if (Config.ACCEPT_GEOEDITOR_CONN)
			GeoEditorListener.getInstance();
		
		Runtime.getRuntime().addShutdownHook(Shutdown.getInstance());
		
		_log.config("IdFactory: Free ObjectID's remaining: " + IdFactory.getInstance().size());
		
		// initialize the dynamic extension loader
		try
		{
			DynamicExtension.getInstance();
		}
		catch (Exception ex)
		{
			_log.log(Level.WARNING, "DynamicExtension could not be loaded and initialized", ex);
		}
		
		TvTManager.getInstance();
		KnownListUpdateTaskManager.getInstance();
		if (Config.DEADLOCK_DETECTOR)
		{
			_deadDetectThread = new DeadLockDetector();
			_deadDetectThread.setDaemon(true);
			_deadDetectThread.start();
		}
		else
			_deadDetectThread = null;
		System.gc();
		// maxMemory is the upper limit the jvm can use, totalMemory the size of
		// the current allocation pool, freeMemory the unused memory in the
		// allocation pool
		long freeMem = (Runtime.getRuntime().maxMemory() - Runtime.getRuntime().totalMemory() + Runtime.getRuntime().freeMemory()) / 1048576;
		long totalMem = Runtime.getRuntime().maxMemory() / 1048576;
		_log.info("GameServer Started, free memory " + freeMem + " Mb of " + totalMem + " Mb");
		
		_loginThread = LoginServerThread.getInstance();
		_loginThread.start();
		
		CommunityServerThread.initialize();
		
		final SelectorConfig sc = new SelectorConfig();
		sc.MAX_READ_PER_PASS = Config.MMO_MAX_READ_PER_PASS;
		sc.MAX_SEND_PER_PASS = Config.MMO_MAX_SEND_PER_PASS;
		sc.SLEEP_TIME = Config.MMO_SELECTOR_SLEEP_TIME;
		sc.HELPER_BUFFER_COUNT = Config.MMO_HELPER_BUFFER_COUNT;
		
		final L2GamePacketHandler gph = new L2GamePacketHandler();
		_selectorThread = new SelectorThread<L2GameClient>(sc, gph, gph, gph, new IPv4Filter());
		
		InetAddress bindAddress = null;
		if (!Config.GAMESERVER_HOSTNAME.equals("*"))
		{
			try
			{
				bindAddress = InetAddress.getByName(Config.GAMESERVER_HOSTNAME);
			}
			catch (UnknownHostException e1)
			{
				_log.severe("WARNING: The GameServer bind address is invalid, using all avaliable IPs. Reason: " + e1.getMessage());
				
				if (Config.DEVELOPER)
				{
					e1.printStackTrace();
				}
			}
		}
		
		try
		{
			_selectorThread.openServerSocket(bindAddress, Config.PORT_GAME);
		}
		catch (IOException e)
		{
			_log.severe("FATAL: Failed to open server socket. Reason: " + e.getMessage());
			if (Config.DEVELOPER)
			{
				e.printStackTrace();
			}
			System.exit(1);
		}
		_selectorThread.start();
		_log.config("Maximum Numbers of Connected Players: " + Config.MAXIMUM_ONLINE_USERS);
		long serverLoadEnd = System.currentTimeMillis();
		_log.info("Server Loaded in " + ((serverLoadEnd - serverLoadStart) / 1000) + " seconds");
		
		AutoAnnounceTaskManager.getInstance();
	}
	
	public static void main(String[] args) throws Exception
	{
		Server.serverMode = Server.MODE_GAMESERVER;
		// Local Constants
		final String LOG_FOLDER = "log"; // Name of folder for log file
		final String LOG_NAME = "./log.cfg"; // Name of log file
		
		/*** Main ***/
		// Create log folder
		File logFolder = new File(Config.DATAPACK_ROOT, LOG_FOLDER);
		logFolder.mkdir();
		
		// Create input stream for log file -- or store file data into memory
		InputStream is = new FileInputStream(new File(LOG_NAME));
		LogManager.getLogManager().readConfiguration(is);
		is.close();
		
		// Initialize config
		Config.load();
		L2DatabaseFactory.getInstance();
		gameServer = new GameServer();
		
		if (Config.IS_TELNET_ENABLED)
		{
			_statusServer = new Status(Server.serverMode);
			_statusServer.start();
		}
		else
		{
			_log.info("Telnet server is currently disabled.");
		}
	}
}
