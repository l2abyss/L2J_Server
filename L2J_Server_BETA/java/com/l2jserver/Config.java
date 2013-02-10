/*
 * Copyright (C) 2004-2013 L2J Server
 * 
 * This file is part of L2J Server.
 * 
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.l2jserver;

import info.tak11.subnet.Subnet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.l2jserver.gameserver.engines.DocumentParser;
import com.l2jserver.gameserver.model.itemcontainer.PcInventory;
import com.l2jserver.gameserver.util.FloodProtectorConfig;
import com.l2jserver.util.L2Properties;
import com.l2jserver.util.StringUtil;

/**
 * This class loads all the game server related configurations from files.<br>
 * The files are usually located in config folder in server root folder.<br>
 * Each configuration has a default value (that should reflect retail behavior).
 */
public final class Config
{
	private static final Logger _log = Logger.getLogger(Config.class.getName());
	
	// --------------------------------------------------
	// Constants
	// --------------------------------------------------
	public static final String EOL = System.getProperty("line.separator");
	
	// --------------------------------------------------
	// L2J Property File Definitions
	// --------------------------------------------------
	public static final String CHARACTER_CONFIG_FILE = "./config/Character.properties";
	public static final String FEATURE_CONFIG_FILE = "./config/Feature.properties";
	public static final String FORTSIEGE_CONFIGURATION_FILE = "./config/FortSiege.properties";
	public static final String GENERAL_CONFIG_FILE = "./config/General.properties";
	public static final String HEXID_FILE = "./config/hexid.txt";
	public static final String ID_CONFIG_FILE = "./config/IdFactory.properties";
	public static final String SERVER_VERSION_FILE = "./config/l2j-version.properties";
	public static final String DATAPACK_VERSION_FILE = "./config/l2jdp-version.properties";
	public static final String L2JMOD_CONFIG_FILE = "./config/L2JMods.properties";
	public static final String LOGIN_CONFIGURATION_FILE = "./config/LoginServer.properties";
	public static final String NPC_CONFIG_FILE = "./config/NPC.properties";
	public static final String PVP_CONFIG_FILE = "./config/PVP.properties";
	public static final String RATES_CONFIG_FILE = "./config/Rates.properties";
	public static final String CONFIGURATION_FILE = "./config/Server.properties";
	public static final String IP_CONFIG_FILE = "./config/ipconfig.xml";
	public static final String SIEGE_CONFIGURATION_FILE = "./config/Siege.properties";
	public static final String TW_CONFIGURATION_FILE = "./config/TerritoryWar.properties";
	public static final String TELNET_FILE = "./config/Telnet.properties";
	public static final String FLOOD_PROTECTOR_FILE = "./config/FloodProtector.properties";
	public static final String MMO_CONFIG_FILE = "./config/MMO.properties";
	public static final String OLYMPIAD_CONFIG_FILE = "./config/Olympiad.properties";
	public static final String COMMUNITY_CONFIGURATION_FILE = "./config/CommunityServer.properties";
	public static final String GRANDBOSS_CONFIG_FILE = "./config/GrandBoss.properties";
	public static final String GRACIASEEDS_CONFIG_FILE = "./config/GraciaSeeds.properties";
	public static final String CHAT_FILTER_FILE = "./config/chatfilter.txt";
	public static final String SECURITY_CONFIG_FILE = "./config/Security.properties";
	public static final String EMAIL_CONFIG_FILE = "./config/Email.properties";
	public static final String CH_SIEGE_FILE = "./config/ConquerableHallSiege.properties";
	
	// --------------------------------------------------
	// L2J Variable Definitions
	// --------------------------------------------------
	public static boolean ALT_GAME_DELEVEL;
	public static boolean DECREASE_SKILL_LEVEL;
	public static double ALT_WEIGHT_LIMIT;
	public static int RUN_SPD_BOOST;
	public static int DEATH_PENALTY_CHANCE;
	public static double RESPAWN_RESTORE_CP;
	public static double RESPAWN_RESTORE_HP;
	public static double RESPAWN_RESTORE_MP;
	public static boolean ALT_GAME_TIREDNESS;
	public static boolean ENABLE_MODIFY_SKILL_DURATION;
	public static Map<Integer, Integer> SKILL_DURATION_LIST;
	public static boolean ENABLE_MODIFY_SKILL_REUSE;
	public static Map<Integer, Integer> SKILL_REUSE_LIST;
	public static boolean AUTO_LEARN_SKILLS;
	public static boolean AUTO_LEARN_FS_SKILLS;
	public static boolean AUTO_LOOT_HERBS;
	public static byte BUFFS_MAX_AMOUNT;
	public static byte TRIGGERED_BUFFS_MAX_AMOUNT;
	public static byte DANCES_MAX_AMOUNT;
	public static boolean DANCE_CANCEL_BUFF;
	public static boolean DANCE_CONSUME_ADDITIONAL_MP;
	public static boolean AUTO_LEARN_DIVINE_INSPIRATION;
	public static boolean ALT_GAME_CANCEL_BOW;
	public static boolean ALT_GAME_CANCEL_CAST;
	public static boolean EFFECT_CANCELING;
	public static boolean ALT_GAME_MAGICFAILURES;
	public static int PLAYER_FAKEDEATH_UP_PROTECTION;
	public static boolean STORE_SKILL_COOLTIME;
	public static boolean SUBCLASS_STORE_SKILL_COOLTIME;
	public static boolean SUMMON_STORE_SKILL_COOLTIME;
	public static boolean ALT_GAME_SHIELD_BLOCKS;
	public static int ALT_PERFECT_SHLD_BLOCK;
	public static boolean ALLOW_CLASS_MASTERS;
	public static ClassMasterSettings CLASS_MASTER_SETTINGS;
	public static boolean ALLOW_ENTIRE_TREE;
	public static boolean ALTERNATE_CLASS_MASTER;
	public static boolean LIFE_CRYSTAL_NEEDED;
	public static boolean ES_SP_BOOK_NEEDED;
	public static boolean DIVINE_SP_BOOK_NEEDED;
	public static boolean ALT_GAME_SKILL_LEARN;
	public static boolean ALT_GAME_SUBCLASS_WITHOUT_QUESTS;
	public static boolean ALT_GAME_SUBCLASS_EVERYWHERE;
	public static boolean ALLOW_TRANSFORM_WITHOUT_QUEST;
	public static int FEE_DELETE_TRANSFER_SKILLS;
	public static int FEE_DELETE_SUBCLASS_SKILLS;
	public static boolean RESTORE_SERVITOR_ON_RECONNECT;
	public static boolean RESTORE_PET_ON_RECONNECT;
	public static double MAX_BONUS_EXP;
	public static double MAX_BONUS_SP;
	public static int MAX_RUN_SPEED;
	public static int MAX_PCRIT_RATE;
	public static int MAX_MCRIT_RATE;
	public static int MAX_PATK_SPEED;
	public static int MAX_MATK_SPEED;
	public static int MAX_EVASION;
	public static int MIN_ABNORMAL_STATE_SUCCESS_RATE;
	public static int MAX_ABNORMAL_STATE_SUCCESS_RATE;
	public static byte MAX_SUBCLASS;
	public static byte BASE_SUBCLASS_LEVEL;
	public static byte MAX_SUBCLASS_LEVEL;
	public static int MAX_PVTSTORESELL_SLOTS_DWARF;
	public static int MAX_PVTSTORESELL_SLOTS_OTHER;
	public static int MAX_PVTSTOREBUY_SLOTS_DWARF;
	public static int MAX_PVTSTOREBUY_SLOTS_OTHER;
	public static int INVENTORY_MAXIMUM_NO_DWARF;
	public static int INVENTORY_MAXIMUM_DWARF;
	public static int INVENTORY_MAXIMUM_GM;
	public static int INVENTORY_MAXIMUM_QUEST_ITEMS;
	public static int WAREHOUSE_SLOTS_DWARF;
	public static int WAREHOUSE_SLOTS_NO_DWARF;
	public static int WAREHOUSE_SLOTS_CLAN;
	public static int ALT_FREIGHT_SLOTS;
	public static int ALT_FREIGHT_PRICE;
	public static boolean ALT_GAME_KARMA_PLAYER_CAN_BE_KILLED_IN_PEACEZONE;
	public static boolean ALT_GAME_KARMA_PLAYER_CAN_SHOP;
	public static boolean ALT_GAME_KARMA_PLAYER_CAN_TELEPORT;
	public static boolean ALT_GAME_KARMA_PLAYER_CAN_USE_GK;
	public static boolean ALT_GAME_KARMA_PLAYER_CAN_TRADE;
	public static boolean ALT_GAME_KARMA_PLAYER_CAN_USE_WAREHOUSE;
	public static int MAX_PERSONAL_FAME_POINTS;
	public static int FORTRESS_ZONE_FAME_TASK_FREQUENCY;
	public static int FORTRESS_ZONE_FAME_AQUIRE_POINTS;
	public static int CASTLE_ZONE_FAME_TASK_FREQUENCY;
	public static int CASTLE_ZONE_FAME_AQUIRE_POINTS;
	public static boolean FAME_FOR_DEAD_PLAYERS;
	public static boolean IS_CRAFTING_ENABLED;
	public static boolean CRAFT_MASTERWORK;
	public static int DWARF_RECIPE_LIMIT;
	public static int COMMON_RECIPE_LIMIT;
	public static boolean ALT_GAME_CREATION;
	public static double ALT_GAME_CREATION_SPEED;
	public static double ALT_GAME_CREATION_XP_RATE;
	public static double ALT_GAME_CREATION_RARE_XPSP_RATE;
	public static double ALT_GAME_CREATION_SP_RATE;
	public static boolean ALT_BLACKSMITH_USE_RECIPES;
	public static int ALT_CLAN_JOIN_DAYS;
	public static int ALT_CLAN_CREATE_DAYS;
	public static int ALT_CLAN_DISSOLVE_DAYS;
	public static int ALT_ALLY_JOIN_DAYS_WHEN_LEAVED;
	public static int ALT_ALLY_JOIN_DAYS_WHEN_DISMISSED;
	public static int ALT_ACCEPT_CLAN_DAYS_WHEN_DISMISSED;
	public static int ALT_CREATE_ALLY_DAYS_WHEN_DISSOLVED;
	public static int ALT_MAX_NUM_OF_CLANS_IN_ALLY;
	public static int ALT_CLAN_MEMBERS_FOR_WAR;
	public static boolean ALT_MEMBERS_CAN_WITHDRAW_FROM_CLANWH;
	public static boolean REMOVE_CASTLE_CIRCLETS;
	public static int ALT_PARTY_RANGE;
	public static int ALT_PARTY_RANGE2;
	public static boolean ALT_LEAVE_PARTY_LEADER;
	public static boolean INITIAL_EQUIPMENT_EVENT;
	public static long STARTING_ADENA;
	public static byte STARTING_LEVEL;
	public static int STARTING_SP;
	public static long MAX_ADENA;
	public static boolean AUTO_LOOT;
	public static boolean AUTO_LOOT_RAIDS;
	public static int LOOT_RAIDS_PRIVILEGE_INTERVAL;
	public static int LOOT_RAIDS_PRIVILEGE_CC_SIZE;
	public static int UNSTUCK_INTERVAL;
	public static int TELEPORT_WATCHDOG_TIMEOUT;
	public static int PLAYER_SPAWN_PROTECTION;
	public static ArrayList<Integer> SPAWN_PROTECTION_ALLOWED_ITEMS;
	public static int PLAYER_TELEPORT_PROTECTION;
	public static boolean RANDOM_RESPAWN_IN_TOWN_ENABLED;
	public static boolean OFFSET_ON_TELEPORT_ENABLED;
	public static int MAX_OFFSET_ON_TELEPORT;
	public static boolean RESTORE_PLAYER_INSTANCE;
	public static boolean ALLOW_SUMMON_TO_INSTANCE;
	public static int EJECT_DEAD_PLAYER_TIME;
	public static boolean PETITIONING_ALLOWED;
	public static int MAX_PETITIONS_PER_PLAYER;
	public static int MAX_PETITIONS_PENDING;
	public static boolean ALT_GAME_FREE_TELEPORT;
	public static int DELETE_DAYS;
	public static float ALT_GAME_EXPONENT_XP;
	public static float ALT_GAME_EXPONENT_SP;
	public static String PARTY_XP_CUTOFF_METHOD;
	public static double PARTY_XP_CUTOFF_PERCENT;
	public static int PARTY_XP_CUTOFF_LEVEL;
	public static boolean DISABLE_TUTORIAL;
	public static boolean EXPERTISE_PENALTY;
	public static boolean STORE_RECIPE_SHOPLIST;
	public static boolean STORE_UI_SETTINGS;
	public static String[] FORBIDDEN_NAMES;
	public static boolean SILENCE_MODE_EXCLUDE;
	
	// --------------------------------------------------
	// ClanHall Settings
	// --------------------------------------------------
	public static long CH_TELE_FEE_RATIO;
	public static int CH_TELE1_FEE;
	public static int CH_TELE2_FEE;
	public static long CH_ITEM_FEE_RATIO;
	public static int CH_ITEM1_FEE;
	public static int CH_ITEM2_FEE;
	public static int CH_ITEM3_FEE;
	public static long CH_MPREG_FEE_RATIO;
	public static int CH_MPREG1_FEE;
	public static int CH_MPREG2_FEE;
	public static int CH_MPREG3_FEE;
	public static int CH_MPREG4_FEE;
	public static int CH_MPREG5_FEE;
	public static long CH_HPREG_FEE_RATIO;
	public static int CH_HPREG1_FEE;
	public static int CH_HPREG2_FEE;
	public static int CH_HPREG3_FEE;
	public static int CH_HPREG4_FEE;
	public static int CH_HPREG5_FEE;
	public static int CH_HPREG6_FEE;
	public static int CH_HPREG7_FEE;
	public static int CH_HPREG8_FEE;
	public static int CH_HPREG9_FEE;
	public static int CH_HPREG10_FEE;
	public static int CH_HPREG11_FEE;
	public static int CH_HPREG12_FEE;
	public static int CH_HPREG13_FEE;
	public static long CH_EXPREG_FEE_RATIO;
	public static int CH_EXPREG1_FEE;
	public static int CH_EXPREG2_FEE;
	public static int CH_EXPREG3_FEE;
	public static int CH_EXPREG4_FEE;
	public static int CH_EXPREG5_FEE;
	public static int CH_EXPREG6_FEE;
	public static int CH_EXPREG7_FEE;
	public static long CH_SUPPORT_FEE_RATIO;
	public static int CH_SUPPORT1_FEE;
	public static int CH_SUPPORT2_FEE;
	public static int CH_SUPPORT3_FEE;
	public static int CH_SUPPORT4_FEE;
	public static int CH_SUPPORT5_FEE;
	public static int CH_SUPPORT6_FEE;
	public static int CH_SUPPORT7_FEE;
	public static int CH_SUPPORT8_FEE;
	public static long CH_CURTAIN_FEE_RATIO;
	public static int CH_CURTAIN1_FEE;
	public static int CH_CURTAIN2_FEE;
	public static long CH_FRONT_FEE_RATIO;
	public static int CH_FRONT1_FEE;
	public static int CH_FRONT2_FEE;
	public static boolean CH_BUFF_FREE;
	
	// --------------------------------------------------
	// Castle Settings
	// --------------------------------------------------
	public static long CS_TELE_FEE_RATIO;
	public static int CS_TELE1_FEE;
	public static int CS_TELE2_FEE;
	public static long CS_MPREG_FEE_RATIO;
	public static int CS_MPREG1_FEE;
	public static int CS_MPREG2_FEE;
	public static int CS_MPREG3_FEE;
	public static int CS_MPREG4_FEE;
	public static long CS_HPREG_FEE_RATIO;
	public static int CS_HPREG1_FEE;
	public static int CS_HPREG2_FEE;
	public static int CS_HPREG3_FEE;
	public static int CS_HPREG4_FEE;
	public static int CS_HPREG5_FEE;
	public static long CS_EXPREG_FEE_RATIO;
	public static int CS_EXPREG1_FEE;
	public static int CS_EXPREG2_FEE;
	public static int CS_EXPREG3_FEE;
	public static int CS_EXPREG4_FEE;
	public static long CS_SUPPORT_FEE_RATIO;
	public static int CS_SUPPORT1_FEE;
	public static int CS_SUPPORT2_FEE;
	public static int CS_SUPPORT3_FEE;
	public static int CS_SUPPORT4_FEE;
	public static List<String> CL_SET_SIEGE_TIME_LIST;
	public static List<Integer> SIEGE_HOUR_LIST_MORNING;
	public static List<Integer> SIEGE_HOUR_LIST_AFTERNOON;
	
	// --------------------------------------------------
	// Fortress Settings
	// --------------------------------------------------
	public static long FS_TELE_FEE_RATIO;
	public static int FS_TELE1_FEE;
	public static int FS_TELE2_FEE;
	public static long FS_MPREG_FEE_RATIO;
	public static int FS_MPREG1_FEE;
	public static int FS_MPREG2_FEE;
	public static long FS_HPREG_FEE_RATIO;
	public static int FS_HPREG1_FEE;
	public static int FS_HPREG2_FEE;
	public static long FS_EXPREG_FEE_RATIO;
	public static int FS_EXPREG1_FEE;
	public static int FS_EXPREG2_FEE;
	public static long FS_SUPPORT_FEE_RATIO;
	public static int FS_SUPPORT1_FEE;
	public static int FS_SUPPORT2_FEE;
	public static int FS_BLOOD_OATH_COUNT;
	public static int FS_UPDATE_FRQ;
	public static int FS_MAX_SUPPLY_LEVEL;
	public static int FS_FEE_FOR_CASTLE;
	public static int FS_MAX_OWN_TIME;
	
	// --------------------------------------------------
	// Feature Settings
	// --------------------------------------------------
	public static int TAKE_FORT_POINTS;
	public static int LOOSE_FORT_POINTS;
	public static int TAKE_CASTLE_POINTS;
	public static int LOOSE_CASTLE_POINTS;
	public static int CASTLE_DEFENDED_POINTS;
	public static int FESTIVAL_WIN_POINTS;
	public static int HERO_POINTS;
	public static int ROYAL_GUARD_COST;
	public static int KNIGHT_UNIT_COST;
	public static int KNIGHT_REINFORCE_COST;
	public static int BALLISTA_POINTS;
	public static int BLOODALLIANCE_POINTS;
	public static int BLOODOATH_POINTS;
	public static int KNIGHTSEPAULETTE_POINTS;
	public static int REPUTATION_SCORE_PER_KILL;
	public static int JOIN_ACADEMY_MIN_REP_SCORE;
	public static int JOIN_ACADEMY_MAX_REP_SCORE;
	public static int RAID_RANKING_1ST;
	public static int RAID_RANKING_2ND;
	public static int RAID_RANKING_3RD;
	public static int RAID_RANKING_4TH;
	public static int RAID_RANKING_5TH;
	public static int RAID_RANKING_6TH;
	public static int RAID_RANKING_7TH;
	public static int RAID_RANKING_8TH;
	public static int RAID_RANKING_9TH;
	public static int RAID_RANKING_10TH;
	public static int RAID_RANKING_UP_TO_50TH;
	public static int RAID_RANKING_UP_TO_100TH;
	public static int CLAN_LEVEL_6_COST;
	public static int CLAN_LEVEL_7_COST;
	public static int CLAN_LEVEL_8_COST;
	public static int CLAN_LEVEL_9_COST;
	public static int CLAN_LEVEL_10_COST;
	public static int CLAN_LEVEL_11_COST;
	public static int CLAN_LEVEL_6_REQUIREMENT;
	public static int CLAN_LEVEL_7_REQUIREMENT;
	public static int CLAN_LEVEL_8_REQUIREMENT;
	public static int CLAN_LEVEL_9_REQUIREMENT;
	public static int CLAN_LEVEL_10_REQUIREMENT;
	public static int CLAN_LEVEL_11_REQUIREMENT;
	public static boolean ALLOW_WYVERN_DURING_SIEGE;
	
	// --------------------------------------------------
	// General Settings
	// --------------------------------------------------
	public static boolean EVERYBODY_HAS_ADMIN_RIGHTS;
	public static boolean DISPLAY_SERVER_VERSION;
	public static boolean SERVER_LIST_BRACKET;
	public static int SERVER_LIST_TYPE;
	public static int SERVER_LIST_AGE;
	public static boolean SERVER_GMONLY;
	public static boolean GM_HERO_AURA;
	public static boolean GM_STARTUP_INVULNERABLE;
	public static boolean GM_STARTUP_INVISIBLE;
	public static boolean GM_STARTUP_SILENCE;
	public static boolean GM_STARTUP_AUTO_LIST;
	public static boolean GM_STARTUP_DIET_MODE;
	public static String GM_ADMIN_MENU_STYLE;
	public static boolean GM_ITEM_RESTRICTION;
	public static boolean GM_SKILL_RESTRICTION;
	public static boolean GM_TRADE_RESTRICTED_ITEMS;
	public static boolean GM_RESTART_FIGHTING;
	public static boolean GM_ANNOUNCER_NAME;
	public static boolean GM_CRITANNOUNCER_NAME;
	public static boolean GM_GIVE_SPECIAL_SKILLS;
	public static boolean GM_GIVE_SPECIAL_AURA_SKILLS;
	public static boolean BYPASS_VALIDATION;
	public static boolean GAMEGUARD_ENFORCE;
	public static boolean GAMEGUARD_PROHIBITACTION;
	public static boolean LOG_CHAT;
	public static boolean LOG_AUTO_ANNOUNCEMENTS;
	public static boolean LOG_ITEMS;
	public static boolean LOG_ITEMS_SMALL_LOG;
	public static boolean LOG_ITEM_ENCHANTS;
	public static boolean LOG_SKILL_ENCHANTS;
	public static boolean GMAUDIT;
	public static boolean LOG_GAME_DAMAGE;
	public static int LOG_GAME_DAMAGE_THRESHOLD;
	public static boolean SKILL_CHECK_ENABLE;
	public static boolean SKILL_CHECK_REMOVE;
	public static boolean SKILL_CHECK_GM;
	public static boolean DEBUG;
	public static boolean PACKET_HANDLER_DEBUG;
	public static boolean DEVELOPER;
	public static boolean ACCEPT_GEOEDITOR_CONN;
	public static boolean ALT_DEV_NO_HANDLERS;
	public static boolean ALT_DEV_NO_QUESTS;
	public static boolean ALT_DEV_NO_SPAWNS;
	public static int THREAD_P_EFFECTS;
	public static int THREAD_P_GENERAL;
	public static int GENERAL_PACKET_THREAD_CORE_SIZE;
	public static int IO_PACKET_THREAD_CORE_SIZE;
	public static int GENERAL_THREAD_CORE_SIZE;
	public static int AI_MAX_THREAD;
	public static int CLIENT_PACKET_QUEUE_SIZE;
	public static int CLIENT_PACKET_QUEUE_MAX_BURST_SIZE;
	public static int CLIENT_PACKET_QUEUE_MAX_PACKETS_PER_SECOND;
	public static int CLIENT_PACKET_QUEUE_MEASURE_INTERVAL;
	public static int CLIENT_PACKET_QUEUE_MAX_AVERAGE_PACKETS_PER_SECOND;
	public static int CLIENT_PACKET_QUEUE_MAX_FLOODS_PER_MIN;
	public static int CLIENT_PACKET_QUEUE_MAX_OVERFLOWS_PER_MIN;
	public static int CLIENT_PACKET_QUEUE_MAX_UNDERFLOWS_PER_MIN;
	public static int CLIENT_PACKET_QUEUE_MAX_UNKNOWN_PER_MIN;
	public static boolean DEADLOCK_DETECTOR;
	public static int DEADLOCK_CHECK_INTERVAL;
	public static boolean RESTART_ON_DEADLOCK;
	public static boolean ALLOW_DISCARDITEM;
	public static int AUTODESTROY_ITEM_AFTER;
	public static int HERB_AUTO_DESTROY_TIME;
	public static List<Integer> LIST_PROTECTED_ITEMS;
	public static boolean DATABASE_CLEAN_UP;
	public static long CONNECTION_CLOSE_TIME;
	public static int CHAR_STORE_INTERVAL;
	public static boolean LAZY_ITEMS_UPDATE;
	public static boolean UPDATE_ITEMS_ON_CHAR_STORE;
	public static boolean DESTROY_DROPPED_PLAYER_ITEM;
	public static boolean DESTROY_EQUIPABLE_PLAYER_ITEM;
	public static boolean SAVE_DROPPED_ITEM;
	public static boolean EMPTY_DROPPED_ITEM_TABLE_AFTER_LOAD;
	public static int SAVE_DROPPED_ITEM_INTERVAL;
	public static boolean CLEAR_DROPPED_ITEM_TABLE;
	public static boolean AUTODELETE_INVALID_QUEST_DATA;
	public static boolean PRECISE_DROP_CALCULATION;
	public static boolean MULTIPLE_ITEM_DROP;
	public static boolean FORCE_INVENTORY_UPDATE;
	public static boolean LAZY_CACHE;
	public static boolean CACHE_CHAR_NAMES;
	public static int MIN_NPC_ANIMATION;
	public static int MAX_NPC_ANIMATION;
	public static int MIN_MONSTER_ANIMATION;
	public static int MAX_MONSTER_ANIMATION;
	public static int COORD_SYNCHRONIZE;
	public static boolean ENABLE_FALLING_DAMAGE;
	public static boolean GRIDS_ALWAYS_ON;
	public static int GRID_NEIGHBOR_TURNON_TIME;
	public static int GRID_NEIGHBOR_TURNOFF_TIME;
	public static int WORLD_X_MIN;
	public static int WORLD_X_MAX;
	public static int WORLD_Y_MIN;
	public static int WORLD_Y_MAX;
	public static int GEODATA;
	public static File GEODATA_DIR;
	public static File PATHNODE_DIR;
	public static boolean GEODATA_CELLFINDING;
	public static String PATHFIND_BUFFERS;
	public static float LOW_WEIGHT;
	public static float MEDIUM_WEIGHT;
	public static float HIGH_WEIGHT;
	public static boolean ADVANCED_DIAGONAL_STRATEGY;
	public static float DIAGONAL_WEIGHT;
	public static int MAX_POSTFILTER_PASSES;
	public static boolean DEBUG_PATH;
	public static boolean FORCE_GEODATA;
	public static boolean MOVE_BASED_KNOWNLIST;
	public static long KNOWNLIST_UPDATE_INTERVAL;
	public static int PEACE_ZONE_MODE;
	public static String DEFAULT_GLOBAL_CHAT;
	public static String DEFAULT_TRADE_CHAT;
	public static boolean ALLOW_WAREHOUSE;
	public static boolean WAREHOUSE_CACHE;
	public static int WAREHOUSE_CACHE_TIME;
	public static boolean ALLOW_REFUND;
	public static boolean ALLOW_MAIL;
	public static boolean ALLOW_ATTACHMENTS;
	public static boolean ALLOW_WEAR;
	public static int WEAR_DELAY;
	public static int WEAR_PRICE;
	public static boolean ALLOW_LOTTERY;
	public static boolean ALLOW_RACE;
	public static boolean ALLOW_WATER;
	public static boolean ALLOW_RENTPET;
	public static boolean ALLOWFISHING;
	public static boolean ALLOW_BOAT;
	public static int BOAT_BROADCAST_RADIUS;
	public static boolean ALLOW_CURSED_WEAPONS;
	public static boolean ALLOW_MANOR;
	public static boolean ALLOW_NPC_WALKERS;
	public static boolean ALLOW_PET_WALKERS;
	public static boolean SERVER_NEWS;
	public static int COMMUNITY_TYPE;
	public static boolean BBS_SHOW_PLAYERLIST;
	public static String BBS_DEFAULT;
	public static boolean SHOW_LEVEL_COMMUNITYBOARD;
	public static boolean SHOW_STATUS_COMMUNITYBOARD;
	public static int NAME_PAGE_SIZE_COMMUNITYBOARD;
	public static int NAME_PER_ROW_COMMUNITYBOARD;
	public static boolean USE_SAY_FILTER;
	public static String CHAT_FILTER_CHARS;
	public static int[] BAN_CHAT_CHANNELS;
	public static int ALT_OLY_START_TIME;
	public static int ALT_OLY_MIN;
	public static long ALT_OLY_CPERIOD;
	public static long ALT_OLY_BATTLE;
	public static long ALT_OLY_WPERIOD;
	public static long ALT_OLY_VPERIOD;
	public static int ALT_OLY_START_POINTS;
	public static int ALT_OLY_WEEKLY_POINTS;
	public static int ALT_OLY_CLASSED;
	public static int ALT_OLY_NONCLASSED;
	public static int ALT_OLY_TEAMS;
	public static int ALT_OLY_REG_DISPLAY;
	public static int[][] ALT_OLY_CLASSED_REWARD;
	public static int[][] ALT_OLY_NONCLASSED_REWARD;
	public static int[][] ALT_OLY_TEAM_REWARD;
	public static int ALT_OLY_COMP_RITEM;
	public static int ALT_OLY_MIN_MATCHES;
	public static int ALT_OLY_GP_PER_POINT;
	public static int ALT_OLY_HERO_POINTS;
	public static int ALT_OLY_RANK1_POINTS;
	public static int ALT_OLY_RANK2_POINTS;
	public static int ALT_OLY_RANK3_POINTS;
	public static int ALT_OLY_RANK4_POINTS;
	public static int ALT_OLY_RANK5_POINTS;
	public static int ALT_OLY_MAX_POINTS;
	public static int ALT_OLY_DIVIDER_CLASSED;
	public static int ALT_OLY_DIVIDER_NON_CLASSED;
	public static int ALT_OLY_MAX_WEEKLY_MATCHES;
	public static int ALT_OLY_MAX_WEEKLY_MATCHES_NON_CLASSED;
	public static int ALT_OLY_MAX_WEEKLY_MATCHES_CLASSED;
	public static int ALT_OLY_MAX_WEEKLY_MATCHES_TEAM;
	public static boolean ALT_OLY_LOG_FIGHTS;
	public static boolean ALT_OLY_SHOW_MONTHLY_WINNERS;
	public static boolean ALT_OLY_ANNOUNCE_GAMES;
	public static List<Integer> LIST_OLY_RESTRICTED_ITEMS;
	public static int ALT_OLY_ENCHANT_LIMIT;
	public static int ALT_OLY_WAIT_TIME;
	public static int ALT_MANOR_REFRESH_TIME;
	public static int ALT_MANOR_REFRESH_MIN;
	public static int ALT_MANOR_APPROVE_TIME;
	public static int ALT_MANOR_APPROVE_MIN;
	public static int ALT_MANOR_MAINTENANCE_PERIOD;
	public static boolean ALT_MANOR_SAVE_ALL_ACTIONS;
	public static int ALT_MANOR_SAVE_PERIOD_RATE;
	public static long ALT_LOTTERY_PRIZE;
	public static long ALT_LOTTERY_TICKET_PRICE;
	public static float ALT_LOTTERY_5_NUMBER_RATE;
	public static float ALT_LOTTERY_4_NUMBER_RATE;
	public static float ALT_LOTTERY_3_NUMBER_RATE;
	public static long ALT_LOTTERY_2_AND_1_NUMBER_PRIZE;
	public static boolean ALT_ITEM_AUCTION_ENABLED;
	public static int ALT_ITEM_AUCTION_EXPIRED_AFTER;
	public static long ALT_ITEM_AUCTION_TIME_EXTENDS_ON_BID;
	public static int FS_TIME_ATTACK;
	public static int FS_TIME_COOLDOWN;
	public static int FS_TIME_ENTRY;
	public static int FS_TIME_WARMUP;
	public static int FS_PARTY_MEMBER_COUNT;
	public static int RIFT_MIN_PARTY_SIZE;
	public static int RIFT_SPAWN_DELAY;
	public static int RIFT_MAX_JUMPS;
	public static int RIFT_AUTO_JUMPS_TIME_MIN;
	public static int RIFT_AUTO_JUMPS_TIME_MAX;
	public static float RIFT_BOSS_ROOM_TIME_MUTIPLY;
	public static int RIFT_ENTER_COST_RECRUIT;
	public static int RIFT_ENTER_COST_SOLDIER;
	public static int RIFT_ENTER_COST_OFFICER;
	public static int RIFT_ENTER_COST_CAPTAIN;
	public static int RIFT_ENTER_COST_COMMANDER;
	public static int RIFT_ENTER_COST_HERO;
	public static int DEFAULT_PUNISH;
	public static int DEFAULT_PUNISH_PARAM;
	public static boolean ONLY_GM_ITEMS_FREE;
	public static boolean JAIL_IS_PVP;
	public static boolean JAIL_DISABLE_CHAT;
	public static boolean JAIL_DISABLE_TRANSACTION;
	public static boolean CUSTOM_SPAWNLIST_TABLE;
	public static boolean SAVE_GMSPAWN_ON_CUSTOM;
	public static boolean CUSTOM_NPC_TABLE;
	public static boolean CUSTOM_NPC_SKILLS_TABLE;
	public static boolean CUSTOM_TELEPORT_TABLE;
	public static boolean CUSTOM_DROPLIST_TABLE;
	public static boolean CUSTOM_MERCHANT_TABLES;
	public static boolean CUSTOM_NPCBUFFER_TABLES;
	public static boolean CUSTOM_SKILLS_LOAD;
	public static boolean CUSTOM_ITEMS_LOAD;
	public static boolean CUSTOM_MULTISELL_LOAD;
	public static int ALT_BIRTHDAY_GIFT;
	public static String ALT_BIRTHDAY_MAIL_SUBJECT;
	public static String ALT_BIRTHDAY_MAIL_TEXT;
	public static boolean ENABLE_BLOCK_CHECKER_EVENT;
	public static int MIN_BLOCK_CHECKER_TEAM_MEMBERS;
	public static boolean HBCE_FAIR_PLAY;
	public static int PLAYER_MOVEMENT_BLOCK_TIME;
	public static boolean CLEAR_CREST_CACHE;
	public static int NORMAL_ENCHANT_COST_MULTIPLIER;
	public static int SAFE_ENCHANT_COST_MULTIPLIER;
	
	// --------------------------------------------------
	// FloodProtector Settings
	// --------------------------------------------------
	public static FloodProtectorConfig FLOOD_PROTECTOR_USE_ITEM;
	public static FloodProtectorConfig FLOOD_PROTECTOR_ROLL_DICE;
	public static FloodProtectorConfig FLOOD_PROTECTOR_FIREWORK;
	public static FloodProtectorConfig FLOOD_PROTECTOR_ITEM_PET_SUMMON;
	public static FloodProtectorConfig FLOOD_PROTECTOR_HERO_VOICE;
	public static FloodProtectorConfig FLOOD_PROTECTOR_GLOBAL_CHAT;
	public static FloodProtectorConfig FLOOD_PROTECTOR_SUBCLASS;
	public static FloodProtectorConfig FLOOD_PROTECTOR_DROP_ITEM;
	public static FloodProtectorConfig FLOOD_PROTECTOR_SERVER_BYPASS;
	public static FloodProtectorConfig FLOOD_PROTECTOR_MULTISELL;
	public static FloodProtectorConfig FLOOD_PROTECTOR_TRANSACTION;
	public static FloodProtectorConfig FLOOD_PROTECTOR_MANUFACTURE;
	public static FloodProtectorConfig FLOOD_PROTECTOR_MANOR;
	public static FloodProtectorConfig FLOOD_PROTECTOR_SENDMAIL;
	public static FloodProtectorConfig FLOOD_PROTECTOR_CHARACTER_SELECT;
	public static FloodProtectorConfig FLOOD_PROTECTOR_ITEM_AUCTION;
	
	// --------------------------------------------------
	// L2JMods Settings
	// --------------------------------------------------
	public static boolean L2JMOD_CHAMPION_ENABLE;
	public static boolean L2JMOD_CHAMPION_PASSIVE;
	public static int L2JMOD_CHAMPION_FREQUENCY;
	public static String L2JMOD_CHAMP_TITLE;
	public static int L2JMOD_CHAMP_MIN_LVL;
	public static int L2JMOD_CHAMP_MAX_LVL;
	public static int L2JMOD_CHAMPION_HP;
	public static int L2JMOD_CHAMPION_REWARDS;
	public static float L2JMOD_CHAMPION_ADENAS_REWARDS;
	public static float L2JMOD_CHAMPION_HP_REGEN;
	public static float L2JMOD_CHAMPION_ATK;
	public static float L2JMOD_CHAMPION_SPD_ATK;
	public static int L2JMOD_CHAMPION_REWARD_LOWER_LVL_ITEM_CHANCE;
	public static int L2JMOD_CHAMPION_REWARD_HIGHER_LVL_ITEM_CHANCE;
	public static int L2JMOD_CHAMPION_REWARD_ID;
	public static int L2JMOD_CHAMPION_REWARD_QTY;
	public static boolean L2JMOD_CHAMPION_ENABLE_VITALITY;
	public static boolean L2JMOD_CHAMPION_ENABLE_IN_INSTANCES;
	public static boolean TVT_EVENT_ENABLED;
	public static boolean TVT_EVENT_IN_INSTANCE;
	public static String TVT_EVENT_INSTANCE_FILE;
	public static String[] TVT_EVENT_INTERVAL;
	public static int TVT_EVENT_PARTICIPATION_TIME;
	public static int TVT_EVENT_RUNNING_TIME;
	public static int TVT_EVENT_PARTICIPATION_NPC_ID;
	public static int[] TVT_EVENT_PARTICIPATION_NPC_COORDINATES = new int[4];
	public static int[] TVT_EVENT_PARTICIPATION_FEE = new int[2];
	public static int TVT_EVENT_MIN_PLAYERS_IN_TEAMS;
	public static int TVT_EVENT_MAX_PLAYERS_IN_TEAMS;
	public static int TVT_EVENT_RESPAWN_TELEPORT_DELAY;
	public static int TVT_EVENT_START_LEAVE_TELEPORT_DELAY;
	public static String TVT_EVENT_TEAM_1_NAME;
	public static int[] TVT_EVENT_TEAM_1_COORDINATES = new int[3];
	public static String TVT_EVENT_TEAM_2_NAME;
	public static int[] TVT_EVENT_TEAM_2_COORDINATES = new int[3];
	public static List<int[]> TVT_EVENT_REWARDS;
	public static boolean TVT_EVENT_TARGET_TEAM_MEMBERS_ALLOWED;
	public static boolean TVT_EVENT_SCROLL_ALLOWED;
	public static boolean TVT_EVENT_POTIONS_ALLOWED;
	public static boolean TVT_EVENT_SUMMON_BY_ITEM_ALLOWED;
	public static List<Integer> TVT_DOORS_IDS_TO_OPEN;
	public static List<Integer> TVT_DOORS_IDS_TO_CLOSE;
	public static boolean TVT_REWARD_TEAM_TIE;
	public static byte TVT_EVENT_MIN_LVL;
	public static byte TVT_EVENT_MAX_LVL;
	public static int TVT_EVENT_EFFECTS_REMOVAL;
	public static Map<Integer, Integer> TVT_EVENT_FIGHTER_BUFFS;
	public static Map<Integer, Integer> TVT_EVENT_MAGE_BUFFS;
	public static int TVT_EVENT_MAX_PARTICIPANTS_PER_IP;
	public static boolean TVT_ALLOW_VOICED_COMMAND;
	public static boolean L2JMOD_ALLOW_WEDDING;
	public static int L2JMOD_WEDDING_PRICE;
	public static boolean L2JMOD_WEDDING_PUNISH_INFIDELITY;
	public static boolean L2JMOD_WEDDING_TELEPORT;
	public static int L2JMOD_WEDDING_TELEPORT_PRICE;
	public static int L2JMOD_WEDDING_TELEPORT_DURATION;
	public static boolean L2JMOD_WEDDING_SAMESEX;
	public static boolean L2JMOD_WEDDING_FORMALWEAR;
	public static int L2JMOD_WEDDING_DIVORCE_COSTS;
	public static boolean L2JMOD_HELLBOUND_STATUS;
	public static boolean BANKING_SYSTEM_ENABLED;
	public static int BANKING_SYSTEM_GOLDBARS;
	public static int BANKING_SYSTEM_ADENA;
	public static boolean L2JMOD_ENABLE_WAREHOUSESORTING_CLAN;
	public static boolean L2JMOD_ENABLE_WAREHOUSESORTING_PRIVATE;
	public static boolean OFFLINE_TRADE_ENABLE;
	public static boolean OFFLINE_CRAFT_ENABLE;
	public static boolean OFFLINE_MODE_IN_PEACE_ZONE;
	public static boolean OFFLINE_MODE_NO_DAMAGE;
	public static boolean RESTORE_OFFLINERS;
	public static int OFFLINE_MAX_DAYS;
	public static boolean OFFLINE_DISCONNECT_FINISHED;
	public static boolean OFFLINE_SET_NAME_COLOR;
	public static int OFFLINE_NAME_COLOR;
	public static boolean OFFLINE_FAME;
	public static boolean L2JMOD_ENABLE_MANA_POTIONS_SUPPORT;
	public static boolean L2JMOD_DISPLAY_SERVER_TIME;
	public static boolean WELCOME_MESSAGE_ENABLED;
	public static String WELCOME_MESSAGE_TEXT;
	public static int WELCOME_MESSAGE_TIME;
	public static boolean L2JMOD_ANTIFEED_ENABLE;
	public static boolean L2JMOD_ANTIFEED_DUALBOX;
	public static boolean L2JMOD_ANTIFEED_DISCONNECTED_AS_DUALBOX;
	public static int L2JMOD_ANTIFEED_INTERVAL;
	public static boolean ANNOUNCE_PK_PVP;
	public static boolean ANNOUNCE_PK_PVP_NORMAL_MESSAGE;
	public static String ANNOUNCE_PK_MSG;
	public static String ANNOUNCE_PVP_MSG;
	public static boolean L2JMOD_CHAT_ADMIN;
	public static boolean L2JMOD_MULTILANG_ENABLE;
	public static List<String> L2JMOD_MULTILANG_ALLOWED = new ArrayList<>();
	public static String L2JMOD_MULTILANG_DEFAULT;
	public static boolean L2JMOD_MULTILANG_VOICED_ALLOW;
	public static boolean L2JMOD_MULTILANG_SM_ENABLE;
	public static List<String> L2JMOD_MULTILANG_SM_ALLOWED = new ArrayList<>();
	public static boolean L2JMOD_MULTILANG_NS_ENABLE;
	public static List<String> L2JMOD_MULTILANG_NS_ALLOWED = new ArrayList<>();
	public static boolean L2WALKER_PROTECTION;
	public static boolean L2JMOD_DEBUG_VOICE_COMMAND;
	public static int L2JMOD_DUALBOX_CHECK_MAX_PLAYERS_PER_IP;
	public static int L2JMOD_DUALBOX_CHECK_MAX_OLYMPIAD_PARTICIPANTS_PER_IP;
	public static int L2JMOD_DUALBOX_CHECK_MAX_L2EVENT_PARTICIPANTS_PER_IP;
	public static Map<Integer, Integer> L2JMOD_DUALBOX_CHECK_WHITELIST;
	public static boolean L2JMOD_ALLOW_CHANGE_PASSWORD;
	
	// --------------------------------------------------
	// NPC Settings
	// --------------------------------------------------
	public static boolean ANNOUNCE_MAMMON_SPAWN;
	public static boolean ALT_MOB_AGRO_IN_PEACEZONE;
	public static boolean ALT_ATTACKABLE_NPCS;
	public static boolean ALT_GAME_VIEWNPC;
	public static int MAX_DRIFT_RANGE;
	public static boolean DEEPBLUE_DROP_RULES;
	public static boolean DEEPBLUE_DROP_RULES_RAID;
	public static boolean SHOW_NPC_LVL;
	public static boolean SHOW_CREST_WITHOUT_QUEST;
	public static boolean ENABLE_RANDOM_ENCHANT_EFFECT;
	public static int MIN_NPC_LVL_DMG_PENALTY;
	public static Map<Integer, Float> NPC_DMG_PENALTY;
	public static Map<Integer, Float> NPC_CRIT_DMG_PENALTY;
	public static Map<Integer, Float> NPC_SKILL_DMG_PENALTY;
	public static int MIN_NPC_LVL_MAGIC_PENALTY;
	public static Map<Integer, Float> NPC_SKILL_CHANCE_PENALTY;
	public static int DECAY_TIME_TASK;
	public static int NPC_DECAY_TIME;
	public static int RAID_BOSS_DECAY_TIME;
	public static int SPOILED_DECAY_TIME;
	public static boolean GUARD_ATTACK_AGGRO_MOB;
	public static boolean ALLOW_WYVERN_UPGRADER;
	public static List<Integer> LIST_PET_RENT_NPC;
	public static double RAID_HP_REGEN_MULTIPLIER;
	public static double RAID_MP_REGEN_MULTIPLIER;
	public static double RAID_PDEFENCE_MULTIPLIER;
	public static double RAID_MDEFENCE_MULTIPLIER;
	public static double RAID_PATTACK_MULTIPLIER;
	public static double RAID_MATTACK_MULTIPLIER;
	public static double RAID_MINION_RESPAWN_TIMER;
	public static Map<Integer, Integer> MINIONS_RESPAWN_TIME;
	public static float RAID_MIN_RESPAWN_MULTIPLIER;
	public static float RAID_MAX_RESPAWN_MULTIPLIER;
	public static boolean RAID_DISABLE_CURSE;
	public static int RAID_CHAOS_TIME;
	public static int GRAND_CHAOS_TIME;
	public static int MINION_CHAOS_TIME;
	public static int INVENTORY_MAXIMUM_PET;
	public static double PET_HP_REGEN_MULTIPLIER;
	public static double PET_MP_REGEN_MULTIPLIER;
	public static List<Integer> NON_TALKING_NPCS;
	
	// --------------------------------------------------
	// PvP Settings
	// --------------------------------------------------
	public static int KARMA_MIN_KARMA;
	public static int KARMA_MAX_KARMA;
	public static int KARMA_XP_DIVIDER;
	public static int KARMA_LOST_BASE;
	public static boolean KARMA_DROP_GM;
	public static boolean KARMA_AWARD_PK_KILL;
	public static int KARMA_PK_LIMIT;
	public static String KARMA_NONDROPPABLE_PET_ITEMS;
	public static String KARMA_NONDROPPABLE_ITEMS;
	public static int[] KARMA_LIST_NONDROPPABLE_PET_ITEMS;
	public static int[] KARMA_LIST_NONDROPPABLE_ITEMS;
	
	// --------------------------------------------------
	// Rate Settings
	// --------------------------------------------------
	public static float RATE_XP;
	public static float RATE_SP;
	public static float RATE_PARTY_XP;
	public static float RATE_PARTY_SP;
	public static float RATE_CONSUMABLE_COST;
	public static float RATE_HB_TRUST_INCREASE;
	public static float RATE_HB_TRUST_DECREASE;
	public static float RATE_EXTRACTABLE;
	public static float RATE_DROP_ITEMS;
	public static float RATE_DROP_ITEMS_BY_RAID;
	public static float RATE_DROP_SPOIL;
	public static int RATE_DROP_MANOR;
	public static float RATE_QUEST_DROP;
	public static float RATE_QUEST_REWARD;
	public static float RATE_QUEST_REWARD_XP;
	public static float RATE_QUEST_REWARD_SP;
	public static float RATE_QUEST_REWARD_ADENA;
	public static boolean RATE_QUEST_REWARD_USE_MULTIPLIERS;
	public static float RATE_QUEST_REWARD_POTION;
	public static float RATE_QUEST_REWARD_SCROLL;
	public static float RATE_QUEST_REWARD_RECIPE;
	public static float RATE_QUEST_REWARD_MATERIAL;
	public static Map<Integer, Float> RATE_DROP_ITEMS_ID;
	public static float RATE_KARMA_EXP_LOST;
	public static float RATE_SIEGE_GUARDS_PRICE;
	public static float RATE_DROP_COMMON_HERBS;
	public static float RATE_DROP_HP_HERBS;
	public static float RATE_DROP_MP_HERBS;
	public static float RATE_DROP_SPECIAL_HERBS;
	public static int PLAYER_DROP_LIMIT;
	public static int PLAYER_RATE_DROP;
	public static int PLAYER_RATE_DROP_ITEM;
	public static int PLAYER_RATE_DROP_EQUIP;
	public static int PLAYER_RATE_DROP_EQUIP_WEAPON;
	public static float PET_XP_RATE;
	public static int PET_FOOD_RATE;
	public static float SINEATER_XP_RATE;
	public static int KARMA_DROP_LIMIT;
	public static int KARMA_RATE_DROP;
	public static int KARMA_RATE_DROP_ITEM;
	public static int KARMA_RATE_DROP_EQUIP;
	public static int KARMA_RATE_DROP_EQUIP_WEAPON;
	public static double[] PLAYER_XP_PERCENT_LOST;
	
	// --------------------------------------------------
	// Seven Signs Settings
	// --------------------------------------------------
	public static boolean ALT_GAME_CASTLE_DAWN;
	public static boolean ALT_GAME_CASTLE_DUSK;
	public static boolean ALT_GAME_REQUIRE_CLAN_CASTLE;
	public static int ALT_FESTIVAL_MIN_PLAYER;
	public static int ALT_MAXIMUM_PLAYER_CONTRIB;
	public static long ALT_FESTIVAL_MANAGER_START;
	public static long ALT_FESTIVAL_LENGTH;
	public static long ALT_FESTIVAL_CYCLE_LENGTH;
	public static long ALT_FESTIVAL_FIRST_SPAWN;
	public static long ALT_FESTIVAL_FIRST_SWARM;
	public static long ALT_FESTIVAL_SECOND_SPAWN;
	public static long ALT_FESTIVAL_SECOND_SWARM;
	public static long ALT_FESTIVAL_CHEST_SPAWN;
	public static double ALT_SIEGE_DAWN_GATES_PDEF_MULT;
	public static double ALT_SIEGE_DUSK_GATES_PDEF_MULT;
	public static double ALT_SIEGE_DAWN_GATES_MDEF_MULT;
	public static double ALT_SIEGE_DUSK_GATES_MDEF_MULT;
	public static boolean ALT_STRICT_SEVENSIGNS;
	public static boolean ALT_SEVENSIGNS_LAZY_UPDATE;
	public static int SSQ_DAWN_TICKET_QUANTITY;
	public static int SSQ_DAWN_TICKET_PRICE;
	public static int SSQ_DAWN_TICKET_BUNDLE;
	public static int SSQ_MANORS_AGREEMENT_ID;
	public static int SSQ_JOIN_DAWN_ADENA_FEE;
	
	// --------------------------------------------------
	// Server Settings
	// --------------------------------------------------
	public static int PORT_GAME;
	public static int PORT_LOGIN;
	public static String LOGIN_BIND_ADDRESS;
	public static int LOGIN_TRY_BEFORE_BAN;
	public static int LOGIN_BLOCK_AFTER_BAN;
	public static String GAMESERVER_HOSTNAME;
	public static String DATABASE_DRIVER;
	public static String DATABASE_URL;
	public static String DATABASE_LOGIN;
	public static String DATABASE_PASSWORD;
	public static int DATABASE_MAX_CONNECTIONS;
	public static int DATABASE_MAX_IDLE_TIME;
	public static int MAXIMUM_ONLINE_USERS;
	public static String CNAME_TEMPLATE;
	public static String PET_NAME_TEMPLATE;
	public static String CLAN_NAME_TEMPLATE;
	public static int MAX_CHARACTERS_NUMBER_PER_ACCOUNT;
	public static File DATAPACK_ROOT;
	public static boolean ACCEPT_ALTERNATE_ID;
	public static int REQUEST_ID;
	public static boolean RESERVE_HOST_ON_LOGIN = false;
	public static List<Integer> PROTOCOL_LIST;
	public static boolean LOG_LOGIN_CONTROLLER;
	public static boolean LOGIN_SERVER_SCHEDULE_RESTART;
	public static long LOGIN_SERVER_SCHEDULE_RESTART_TIME;
	
	// --------------------------------------------------
	// CommunityServer Settings
	// --------------------------------------------------
	public static boolean ENABLE_COMMUNITY_BOARD;
	public static String COMMUNITY_SERVER_ADDRESS;
	public static int COMMUNITY_SERVER_PORT;
	public static byte[] COMMUNITY_SERVER_HEX_ID;
	public static int COMMUNITY_SERVER_SQL_DP_ID;
	
	// --------------------------------------------------
	// MMO Settings
	// --------------------------------------------------
	public static int MMO_SELECTOR_SLEEP_TIME;
	public static int MMO_MAX_SEND_PER_PASS;
	public static int MMO_MAX_READ_PER_PASS;
	public static int MMO_HELPER_BUFFER_COUNT;
	public static boolean MMO_TCP_NODELAY;
	
	// --------------------------------------------------
	// Vitality Settings
	// --------------------------------------------------
	public static boolean ENABLE_VITALITY;
	public static boolean RECOVER_VITALITY_ON_RECONNECT;
	public static boolean ENABLE_DROP_VITALITY_HERBS;
	public static float RATE_VITALITY_LEVEL_1;
	public static float RATE_VITALITY_LEVEL_2;
	public static float RATE_VITALITY_LEVEL_3;
	public static float RATE_VITALITY_LEVEL_4;
	public static float RATE_DROP_VITALITY_HERBS;
	public static float RATE_RECOVERY_VITALITY_PEACE_ZONE;
	public static float RATE_VITALITY_LOST;
	public static float RATE_VITALITY_GAIN;
	public static float RATE_RECOVERY_ON_RECONNECT;
	public static int STARTING_VITALITY_POINTS;
	
	// --------------------------------------------------
	// No classification assigned to the following yet
	// --------------------------------------------------
	public static int MAX_ITEM_IN_PACKET;
	public static boolean CHECK_KNOWN;
	public static int GAME_SERVER_LOGIN_PORT;
	public static String GAME_SERVER_LOGIN_HOST;
	public static List<String> GAME_SERVER_SUBNETS;
	public static List<String> GAME_SERVER_HOSTS;
	public static String SERVER_VERSION;
	public static String SERVER_BUILD_DATE;
	public static String DATAPACK_VERSION;
	public static int PVP_NORMAL_TIME;
	public static int PVP_PVP_TIME;
	
	public static enum IdFactoryType
	{
		Compaction,
		BitSet,
		Stack
	}
	
	public static IdFactoryType IDFACTORY_TYPE;
	public static boolean BAD_ID_CHECKING;
	
	public static double ENCHANT_CHANCE;
	public static int MAX_ENCHANT_LEVEL;
	public static double ENCHANT_CHANCE_ELEMENT_STONE;
	public static double ENCHANT_CHANCE_ELEMENT_CRYSTAL;
	public static double ENCHANT_CHANCE_ELEMENT_JEWEL;
	public static double ENCHANT_CHANCE_ELEMENT_ENERGY;
	public static int ENCHANT_SAFE_MAX;
	public static int ENCHANT_SAFE_MAX_FULL;
	public static int[] ENCHANT_BLACKLIST;
	public static int AUGMENTATION_NG_SKILL_CHANCE;
	public static int AUGMENTATION_NG_GLOW_CHANCE;
	public static int AUGMENTATION_MID_SKILL_CHANCE;
	public static int AUGMENTATION_MID_GLOW_CHANCE;
	public static int AUGMENTATION_HIGH_SKILL_CHANCE;
	public static int AUGMENTATION_HIGH_GLOW_CHANCE;
	public static int AUGMENTATION_TOP_SKILL_CHANCE;
	public static int AUGMENTATION_TOP_GLOW_CHANCE;
	public static int AUGMENTATION_BASESTAT_CHANCE;
	public static int AUGMENTATION_ACC_SKILL_CHANCE;
	public static int[] AUGMENTATION_BLACKLIST;
	public static double HP_REGEN_MULTIPLIER;
	public static double MP_REGEN_MULTIPLIER;
	public static double CP_REGEN_MULTIPLIER;
	public static boolean IS_TELNET_ENABLED;
	public static boolean SHOW_LICENCE;
	public static boolean ACCEPT_NEW_GAMESERVER;
	public static int SERVER_ID;
	public static byte[] HEX_ID;
	public static boolean AUTO_CREATE_ACCOUNTS;
	public static boolean FLOOD_PROTECTION;
	public static int FAST_CONNECTION_LIMIT;
	public static int NORMAL_CONNECTION_TIME;
	public static int FAST_CONNECTION_TIME;
	public static int MAX_CONNECTION_PER_IP;
	
	// GrandBoss Settings
	
	public static int Antharas_Wait_Time;
	public static int Valakas_Wait_Time;
	public static int Interval_Of_Antharas_Spawn;
	public static int Random_Of_Antharas_Spawn;
	public static int Interval_Of_Valakas_Spawn;
	public static int Random_Of_Valakas_Spawn;
	public static int Interval_Of_Baium_Spawn;
	public static int Random_Of_Baium_Spawn;
	public static int Interval_Of_Core_Spawn;
	public static int Random_Of_Core_Spawn;
	public static int Interval_Of_Orfen_Spawn;
	public static int Random_Of_Orfen_Spawn;
	public static int Interval_Of_QueenAnt_Spawn;
	public static int Random_Of_QueenAnt_Spawn;
	public static int Interval_Of_Zaken_Spawn;
	public static int Random_Of_Zaken_Spawn;
	public static int BELETH_MIN_PLAYERS;
	public static int INTERVAL_OF_BELETH_SPAWN;
	public static int RANDOM_OF_BELETH_SPAWN;
	
	// Gracia Seeds Settings
	
	public static int SOD_TIAT_KILL_COUNT;
	public static long SOD_STAGE_2_LENGTH;
	
	// chatfilter
	public static ArrayList<String> FILTER_LIST;
	
	// Security
	public static boolean SECOND_AUTH_ENABLED;
	public static int SECOND_AUTH_MAX_ATTEMPTS;
	public static long SECOND_AUTH_BAN_TIME;
	public static String SECOND_AUTH_REC_LINK;
	
	// Email
	public static String EMAIL_SERVERINFO_NAME;
	public static String EMAIL_SERVERINFO_ADDRESS;
	public static boolean EMAIL_SYS_ENABLED;
	public static String EMAIL_SYS_HOST;
	public static int EMAIL_SYS_PORT;
	public static boolean EMAIL_SYS_SMTP_AUTH;
	public static String EMAIL_SYS_FACTORY;
	public static boolean EMAIL_SYS_FACTORY_CALLBACK;
	public static String EMAIL_SYS_USERNAME;
	public static String EMAIL_SYS_PASSWORD;
	public static String EMAIL_SYS_ADDRESS;
	public static String EMAIL_SYS_SELECTQUERY;
	public static String EMAIL_SYS_DBFIELD;
	
	// Conquerable Halls Settings
	public static int CHS_CLAN_MINLEVEL;
	public static int CHS_MAX_ATTACKERS;
	public static int CHS_MAX_FLAGS_PER_CLAN;
	public static boolean CHS_ENABLE_FAME;
	public static int CHS_FAME_AMOUNT;
	public static int CHS_FAME_FREQUENCY;
	
	/**
	 * This class initializes all global variables for configuration.<br>
	 * If the key doesn't appear in properties file, a default value is set by this class. {@link #CONFIGURATION_FILE} (properties file) for configuring your server.
	 */
	public static void load()
	{
		if (Server.serverMode == Server.MODE_GAMESERVER)
		{
			FLOOD_PROTECTOR_USE_ITEM = new FloodProtectorConfig("UseItemFloodProtector");
			FLOOD_PROTECTOR_ROLL_DICE = new FloodProtectorConfig("RollDiceFloodProtector");
			FLOOD_PROTECTOR_FIREWORK = new FloodProtectorConfig("FireworkFloodProtector");
			FLOOD_PROTECTOR_ITEM_PET_SUMMON = new FloodProtectorConfig("ItemPetSummonFloodProtector");
			FLOOD_PROTECTOR_HERO_VOICE = new FloodProtectorConfig("HeroVoiceFloodProtector");
			FLOOD_PROTECTOR_GLOBAL_CHAT = new FloodProtectorConfig("GlobalChatFloodProtector");
			FLOOD_PROTECTOR_SUBCLASS = new FloodProtectorConfig("SubclassFloodProtector");
			FLOOD_PROTECTOR_DROP_ITEM = new FloodProtectorConfig("DropItemFloodProtector");
			FLOOD_PROTECTOR_SERVER_BYPASS = new FloodProtectorConfig("ServerBypassFloodProtector");
			FLOOD_PROTECTOR_MULTISELL = new FloodProtectorConfig("MultiSellFloodProtector");
			FLOOD_PROTECTOR_TRANSACTION = new FloodProtectorConfig("TransactionFloodProtector");
			FLOOD_PROTECTOR_MANUFACTURE = new FloodProtectorConfig("ManufactureFloodProtector");
			FLOOD_PROTECTOR_MANOR = new FloodProtectorConfig("ManorFloodProtector");
			FLOOD_PROTECTOR_SENDMAIL = new FloodProtectorConfig("SendMailFloodProtector");
			FLOOD_PROTECTOR_CHARACTER_SELECT = new FloodProtectorConfig("CharacterSelectFloodProtector");
			FLOOD_PROTECTOR_ITEM_AUCTION = new FloodProtectorConfig("ItemAuctionFloodProtector");
			
			L2Properties serverSettings = new L2Properties();
			final File server = new File(CONFIGURATION_FILE);
			try (InputStream is = new FileInputStream(server))
			{
				serverSettings.load(is);
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "Error while loading Server settings!", e);
			}
			
			GAMESERVER_HOSTNAME = serverSettings.getProperty("GameserverHostname", "*");
			PORT_GAME = Integer.parseInt(serverSettings.getProperty("GameserverPort", "7777"));
			
			GAME_SERVER_LOGIN_PORT = Integer.parseInt(serverSettings.getProperty("LoginPort", "9014"));
			GAME_SERVER_LOGIN_HOST = serverSettings.getProperty("LoginHost", "127.0.0.1");
			
			REQUEST_ID = Integer.parseInt(serverSettings.getProperty("RequestServerID", "0"));
			ACCEPT_ALTERNATE_ID = Boolean.parseBoolean(serverSettings.getProperty("AcceptAlternateID", "True"));
			
			DATABASE_DRIVER = serverSettings.getProperty("Driver", "com.mysql.jdbc.Driver");
			DATABASE_URL = serverSettings.getProperty("URL", "jdbc:mysql://localhost/l2jgs");
			DATABASE_LOGIN = serverSettings.getProperty("Login", "root");
			DATABASE_PASSWORD = serverSettings.getProperty("Password", "");
			DATABASE_MAX_CONNECTIONS = Integer.parseInt(serverSettings.getProperty("MaximumDbConnections", "10"));
			DATABASE_MAX_IDLE_TIME = Integer.parseInt(serverSettings.getProperty("MaximumDbIdleTime", "0"));
			
			try
			{
				DATAPACK_ROOT = new File(serverSettings.getProperty("DatapackRoot", ".").replaceAll("\\\\", "/")).getCanonicalFile();
			}
			catch (IOException e)
			{
				_log.log(Level.WARNING, "Error setting datapack root!", e);
				DATAPACK_ROOT = new File(".");
			}
			
			CNAME_TEMPLATE = serverSettings.getProperty("CnameTemplate", ".*");
			PET_NAME_TEMPLATE = serverSettings.getProperty("PetNameTemplate", ".*");
			CLAN_NAME_TEMPLATE = serverSettings.getProperty("ClanNameTemplate", ".*");
			
			MAX_CHARACTERS_NUMBER_PER_ACCOUNT = Integer.parseInt(serverSettings.getProperty("CharMaxNumber", "7"));
			MAXIMUM_ONLINE_USERS = Integer.parseInt(serverSettings.getProperty("MaximumOnlineUsers", "100"));
			
			String[] protocols = serverSettings.getProperty("AllowedProtocolRevisions", "267;268;271;273").split(";");
			PROTOCOL_LIST = new ArrayList<>(protocols.length);
			for (String protocol : protocols)
			{
				try
				{
					PROTOCOL_LIST.add(Integer.parseInt(protocol.trim()));
				}
				catch (NumberFormatException e)
				{
					_log.log(Level.WARNING, "Wrong config protocol version: " + protocol + ". Skipped.");
				}
			}
			
			// Hosts and Subnets
			IPConfigData ipcd = new IPConfigData();
			GAME_SERVER_SUBNETS = ipcd.getSubnets();
			GAME_SERVER_HOSTS = ipcd.getHosts();
			
			// Load Community Properties file (if exists)
			L2Properties communityServerSettings = new L2Properties();
			final File community = new File(COMMUNITY_CONFIGURATION_FILE);
			try (InputStream is = new FileInputStream(community))
			{
				communityServerSettings.load(is);
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "Error while loading Community Server settings!", e);
			}
			
			ENABLE_COMMUNITY_BOARD = Boolean.parseBoolean(communityServerSettings.getProperty("EnableCommunityBoard", "False"));
			COMMUNITY_SERVER_ADDRESS = communityServerSettings.getProperty("CommunityServerHostname", "localhost");
			COMMUNITY_SERVER_PORT = Integer.parseInt(communityServerSettings.getProperty("CommunityServerPort", "9013"));
			COMMUNITY_SERVER_HEX_ID = new BigInteger(communityServerSettings.getProperty("CommunityServerHexId", "0"), 16).toByteArray();
			COMMUNITY_SERVER_SQL_DP_ID = Integer.parseInt(communityServerSettings.getProperty("CommunityServerSqlDpId", "200"));
			
			// Load Feature L2Properties file (if exists)
			L2Properties Feature = new L2Properties();
			final File feature = new File(FEATURE_CONFIG_FILE);
			try (InputStream is = new FileInputStream(feature))
			{
				Feature.load(is);
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "Error while loading Features settings!", e);
			}
			
			CH_TELE_FEE_RATIO = Long.parseLong(Feature.getProperty("ClanHallTeleportFunctionFeeRatio", "604800000"));
			CH_TELE1_FEE = Integer.parseInt(Feature.getProperty("ClanHallTeleportFunctionFeeLvl1", "7000"));
			CH_TELE2_FEE = Integer.parseInt(Feature.getProperty("ClanHallTeleportFunctionFeeLvl2", "14000"));
			CH_SUPPORT_FEE_RATIO = Long.parseLong(Feature.getProperty("ClanHallSupportFunctionFeeRatio", "86400000"));
			CH_SUPPORT1_FEE = Integer.parseInt(Feature.getProperty("ClanHallSupportFeeLvl1", "2500"));
			CH_SUPPORT2_FEE = Integer.parseInt(Feature.getProperty("ClanHallSupportFeeLvl2", "5000"));
			CH_SUPPORT3_FEE = Integer.parseInt(Feature.getProperty("ClanHallSupportFeeLvl3", "7000"));
			CH_SUPPORT4_FEE = Integer.parseInt(Feature.getProperty("ClanHallSupportFeeLvl4", "11000"));
			CH_SUPPORT5_FEE = Integer.parseInt(Feature.getProperty("ClanHallSupportFeeLvl5", "21000"));
			CH_SUPPORT6_FEE = Integer.parseInt(Feature.getProperty("ClanHallSupportFeeLvl6", "36000"));
			CH_SUPPORT7_FEE = Integer.parseInt(Feature.getProperty("ClanHallSupportFeeLvl7", "37000"));
			CH_SUPPORT8_FEE = Integer.parseInt(Feature.getProperty("ClanHallSupportFeeLvl8", "52000"));
			CH_MPREG_FEE_RATIO = Long.parseLong(Feature.getProperty("ClanHallMpRegenerationFunctionFeeRatio", "86400000"));
			CH_MPREG1_FEE = Integer.parseInt(Feature.getProperty("ClanHallMpRegenerationFeeLvl1", "2000"));
			CH_MPREG2_FEE = Integer.parseInt(Feature.getProperty("ClanHallMpRegenerationFeeLvl2", "3750"));
			CH_MPREG3_FEE = Integer.parseInt(Feature.getProperty("ClanHallMpRegenerationFeeLvl3", "6500"));
			CH_MPREG4_FEE = Integer.parseInt(Feature.getProperty("ClanHallMpRegenerationFeeLvl4", "13750"));
			CH_MPREG5_FEE = Integer.parseInt(Feature.getProperty("ClanHallMpRegenerationFeeLvl5", "20000"));
			CH_HPREG_FEE_RATIO = Long.parseLong(Feature.getProperty("ClanHallHpRegenerationFunctionFeeRatio", "86400000"));
			CH_HPREG1_FEE = Integer.parseInt(Feature.getProperty("ClanHallHpRegenerationFeeLvl1", "700"));
			CH_HPREG2_FEE = Integer.parseInt(Feature.getProperty("ClanHallHpRegenerationFeeLvl2", "800"));
			CH_HPREG3_FEE = Integer.parseInt(Feature.getProperty("ClanHallHpRegenerationFeeLvl3", "1000"));
			CH_HPREG4_FEE = Integer.parseInt(Feature.getProperty("ClanHallHpRegenerationFeeLvl4", "1166"));
			CH_HPREG5_FEE = Integer.parseInt(Feature.getProperty("ClanHallHpRegenerationFeeLvl5", "1500"));
			CH_HPREG6_FEE = Integer.parseInt(Feature.getProperty("ClanHallHpRegenerationFeeLvl6", "1750"));
			CH_HPREG7_FEE = Integer.parseInt(Feature.getProperty("ClanHallHpRegenerationFeeLvl7", "2000"));
			CH_HPREG8_FEE = Integer.parseInt(Feature.getProperty("ClanHallHpRegenerationFeeLvl8", "2250"));
			CH_HPREG9_FEE = Integer.parseInt(Feature.getProperty("ClanHallHpRegenerationFeeLvl9", "2500"));
			CH_HPREG10_FEE = Integer.parseInt(Feature.getProperty("ClanHallHpRegenerationFeeLvl10", "3250"));
			CH_HPREG11_FEE = Integer.parseInt(Feature.getProperty("ClanHallHpRegenerationFeeLvl11", "3270"));
			CH_HPREG12_FEE = Integer.parseInt(Feature.getProperty("ClanHallHpRegenerationFeeLvl12", "4250"));
			CH_HPREG13_FEE = Integer.parseInt(Feature.getProperty("ClanHallHpRegenerationFeeLvl13", "5166"));
			CH_EXPREG_FEE_RATIO = Long.parseLong(Feature.getProperty("ClanHallExpRegenerationFunctionFeeRatio", "86400000"));
			CH_EXPREG1_FEE = Integer.parseInt(Feature.getProperty("ClanHallExpRegenerationFeeLvl1", "3000"));
			CH_EXPREG2_FEE = Integer.parseInt(Feature.getProperty("ClanHallExpRegenerationFeeLvl2", "6000"));
			CH_EXPREG3_FEE = Integer.parseInt(Feature.getProperty("ClanHallExpRegenerationFeeLvl3", "9000"));
			CH_EXPREG4_FEE = Integer.parseInt(Feature.getProperty("ClanHallExpRegenerationFeeLvl4", "15000"));
			CH_EXPREG5_FEE = Integer.parseInt(Feature.getProperty("ClanHallExpRegenerationFeeLvl5", "21000"));
			CH_EXPREG6_FEE = Integer.parseInt(Feature.getProperty("ClanHallExpRegenerationFeeLvl6", "23330"));
			CH_EXPREG7_FEE = Integer.parseInt(Feature.getProperty("ClanHallExpRegenerationFeeLvl7", "30000"));
			CH_ITEM_FEE_RATIO = Long.parseLong(Feature.getProperty("ClanHallItemCreationFunctionFeeRatio", "86400000"));
			CH_ITEM1_FEE = Integer.parseInt(Feature.getProperty("ClanHallItemCreationFunctionFeeLvl1", "30000"));
			CH_ITEM2_FEE = Integer.parseInt(Feature.getProperty("ClanHallItemCreationFunctionFeeLvl2", "70000"));
			CH_ITEM3_FEE = Integer.parseInt(Feature.getProperty("ClanHallItemCreationFunctionFeeLvl3", "140000"));
			CH_CURTAIN_FEE_RATIO = Long.parseLong(Feature.getProperty("ClanHallCurtainFunctionFeeRatio", "604800000"));
			CH_CURTAIN1_FEE = Integer.parseInt(Feature.getProperty("ClanHallCurtainFunctionFeeLvl1", "2000"));
			CH_CURTAIN2_FEE = Integer.parseInt(Feature.getProperty("ClanHallCurtainFunctionFeeLvl2", "2500"));
			CH_FRONT_FEE_RATIO = Long.parseLong(Feature.getProperty("ClanHallFrontPlatformFunctionFeeRatio", "259200000"));
			CH_FRONT1_FEE = Integer.parseInt(Feature.getProperty("ClanHallFrontPlatformFunctionFeeLvl1", "1300"));
			CH_FRONT2_FEE = Integer.parseInt(Feature.getProperty("ClanHallFrontPlatformFunctionFeeLvl2", "4000"));
			CH_BUFF_FREE = Boolean.parseBoolean(Feature.getProperty("AltClanHallMpBuffFree", "False"));
			
			CL_SET_SIEGE_TIME_LIST = new ArrayList<>();
			SIEGE_HOUR_LIST_MORNING = new ArrayList<>();
			SIEGE_HOUR_LIST_AFTERNOON = new ArrayList<>();
			String[] sstl = Feature.getProperty("CLSetSiegeTimeList", "").split(",");
			if (sstl.length != 0)
			{
				boolean isHour = false;
				for (String st : sstl)
				{
					if (st.equalsIgnoreCase("day") || st.equalsIgnoreCase("hour") || st.equalsIgnoreCase("minute"))
					{
						if (st.equalsIgnoreCase("hour"))
						{
							isHour = true;
						}
						CL_SET_SIEGE_TIME_LIST.add(st.toLowerCase());
					}
					else
					{
						_log.warning(StringUtil.concat("[CLSetSiegeTimeList]: invalid config property -> CLSetSiegeTimeList \"", st, "\""));
					}
				}
				if (isHour)
				{
					String[] shl = Feature.getProperty("SiegeHourList", "").split(",");
					for (String st : shl)
					{
						if (!st.equalsIgnoreCase(""))
						{
							int val = Integer.parseInt(st);
							if ((val > 23) || (val < 0))
							{
								_log.warning(StringUtil.concat("[SiegeHourList]: invalid config property -> SiegeHourList \"", st, "\""));
							}
							else if (val < 12)
							{
								SIEGE_HOUR_LIST_MORNING.add(val);
							}
							else
							{
								val -= 12;
								SIEGE_HOUR_LIST_AFTERNOON.add(val);
							}
						}
					}
					if (Config.SIEGE_HOUR_LIST_AFTERNOON.isEmpty() && Config.SIEGE_HOUR_LIST_AFTERNOON.isEmpty())
					{
						_log.warning("[SiegeHourList]: invalid config property -> SiegeHourList is empty");
						CL_SET_SIEGE_TIME_LIST.remove("hour");
					}
				}
			}
			CS_TELE_FEE_RATIO = Long.parseLong(Feature.getProperty("CastleTeleportFunctionFeeRatio", "604800000"));
			CS_TELE1_FEE = Integer.parseInt(Feature.getProperty("CastleTeleportFunctionFeeLvl1", "7000"));
			CS_TELE2_FEE = Integer.parseInt(Feature.getProperty("CastleTeleportFunctionFeeLvl2", "14000"));
			CS_SUPPORT_FEE_RATIO = Long.parseLong(Feature.getProperty("CastleSupportFunctionFeeRatio", "86400000"));
			CS_SUPPORT1_FEE = Integer.parseInt(Feature.getProperty("CastleSupportFeeLvl1", "7000"));
			CS_SUPPORT2_FEE = Integer.parseInt(Feature.getProperty("CastleSupportFeeLvl2", "21000"));
			CS_SUPPORT3_FEE = Integer.parseInt(Feature.getProperty("CastleSupportFeeLvl3", "37000"));
			CS_SUPPORT4_FEE = Integer.parseInt(Feature.getProperty("CastleSupportFeeLvl4", "52000"));
			CS_MPREG_FEE_RATIO = Long.parseLong(Feature.getProperty("CastleMpRegenerationFunctionFeeRatio", "86400000"));
			CS_MPREG1_FEE = Integer.parseInt(Feature.getProperty("CastleMpRegenerationFeeLvl1", "2000"));
			CS_MPREG2_FEE = Integer.parseInt(Feature.getProperty("CastleMpRegenerationFeeLvl2", "6500"));
			CS_MPREG3_FEE = Integer.parseInt(Feature.getProperty("CastleMpRegenerationFeeLvl3", "13750"));
			CS_MPREG4_FEE = Integer.parseInt(Feature.getProperty("CastleMpRegenerationFeeLvl4", "20000"));
			CS_HPREG_FEE_RATIO = Long.parseLong(Feature.getProperty("CastleHpRegenerationFunctionFeeRatio", "86400000"));
			CS_HPREG1_FEE = Integer.parseInt(Feature.getProperty("CastleHpRegenerationFeeLvl1", "1000"));
			CS_HPREG2_FEE = Integer.parseInt(Feature.getProperty("CastleHpRegenerationFeeLvl2", "1500"));
			CS_HPREG3_FEE = Integer.parseInt(Feature.getProperty("CastleHpRegenerationFeeLvl3", "2250"));
			CS_HPREG4_FEE = Integer.parseInt(Feature.getProperty("CastleHpRegenerationFeeLvl4", "3270"));
			CS_HPREG5_FEE = Integer.parseInt(Feature.getProperty("CastleHpRegenerationFeeLvl5", "5166"));
			CS_EXPREG_FEE_RATIO = Long.parseLong(Feature.getProperty("CastleExpRegenerationFunctionFeeRatio", "86400000"));
			CS_EXPREG1_FEE = Integer.parseInt(Feature.getProperty("CastleExpRegenerationFeeLvl1", "9000"));
			CS_EXPREG2_FEE = Integer.parseInt(Feature.getProperty("CastleExpRegenerationFeeLvl2", "15000"));
			CS_EXPREG3_FEE = Integer.parseInt(Feature.getProperty("CastleExpRegenerationFeeLvl3", "21000"));
			CS_EXPREG4_FEE = Integer.parseInt(Feature.getProperty("CastleExpRegenerationFeeLvl4", "30000"));
			
			FS_TELE_FEE_RATIO = Long.parseLong(Feature.getProperty("FortressTeleportFunctionFeeRatio", "604800000"));
			FS_TELE1_FEE = Integer.parseInt(Feature.getProperty("FortressTeleportFunctionFeeLvl1", "1000"));
			FS_TELE2_FEE = Integer.parseInt(Feature.getProperty("FortressTeleportFunctionFeeLvl2", "10000"));
			FS_SUPPORT_FEE_RATIO = Long.parseLong(Feature.getProperty("FortressSupportFunctionFeeRatio", "86400000"));
			FS_SUPPORT1_FEE = Integer.parseInt(Feature.getProperty("FortressSupportFeeLvl1", "7000"));
			FS_SUPPORT2_FEE = Integer.parseInt(Feature.getProperty("FortressSupportFeeLvl2", "17000"));
			FS_MPREG_FEE_RATIO = Long.parseLong(Feature.getProperty("FortressMpRegenerationFunctionFeeRatio", "86400000"));
			FS_MPREG1_FEE = Integer.parseInt(Feature.getProperty("FortressMpRegenerationFeeLvl1", "6500"));
			FS_MPREG2_FEE = Integer.parseInt(Feature.getProperty("FortressMpRegenerationFeeLvl2", "9300"));
			FS_HPREG_FEE_RATIO = Long.parseLong(Feature.getProperty("FortressHpRegenerationFunctionFeeRatio", "86400000"));
			FS_HPREG1_FEE = Integer.parseInt(Feature.getProperty("FortressHpRegenerationFeeLvl1", "2000"));
			FS_HPREG2_FEE = Integer.parseInt(Feature.getProperty("FortressHpRegenerationFeeLvl2", "3500"));
			FS_EXPREG_FEE_RATIO = Long.parseLong(Feature.getProperty("FortressExpRegenerationFunctionFeeRatio", "86400000"));
			FS_EXPREG1_FEE = Integer.parseInt(Feature.getProperty("FortressExpRegenerationFeeLvl1", "9000"));
			FS_EXPREG2_FEE = Integer.parseInt(Feature.getProperty("FortressExpRegenerationFeeLvl2", "10000"));
			FS_UPDATE_FRQ = Integer.parseInt(Feature.getProperty("FortressPeriodicUpdateFrequency", "360"));
			FS_BLOOD_OATH_COUNT = Integer.parseInt(Feature.getProperty("FortressBloodOathCount", "1"));
			FS_MAX_SUPPLY_LEVEL = Integer.parseInt(Feature.getProperty("FortressMaxSupplyLevel", "6"));
			FS_FEE_FOR_CASTLE = Integer.parseInt(Feature.getProperty("FortressFeeForCastle", "25000"));
			FS_MAX_OWN_TIME = Integer.parseInt(Feature.getProperty("FortressMaximumOwnTime", "168"));
			
			ALT_GAME_CASTLE_DAWN = Boolean.parseBoolean(Feature.getProperty("AltCastleForDawn", "True"));
			ALT_GAME_CASTLE_DUSK = Boolean.parseBoolean(Feature.getProperty("AltCastleForDusk", "True"));
			ALT_GAME_REQUIRE_CLAN_CASTLE = Boolean.parseBoolean(Feature.getProperty("AltRequireClanCastle", "False"));
			ALT_FESTIVAL_MIN_PLAYER = Integer.parseInt(Feature.getProperty("AltFestivalMinPlayer", "5"));
			ALT_MAXIMUM_PLAYER_CONTRIB = Integer.parseInt(Feature.getProperty("AltMaxPlayerContrib", "1000000"));
			ALT_FESTIVAL_MANAGER_START = Long.parseLong(Feature.getProperty("AltFestivalManagerStart", "120000"));
			ALT_FESTIVAL_LENGTH = Long.parseLong(Feature.getProperty("AltFestivalLength", "1080000"));
			ALT_FESTIVAL_CYCLE_LENGTH = Long.parseLong(Feature.getProperty("AltFestivalCycleLength", "2280000"));
			ALT_FESTIVAL_FIRST_SPAWN = Long.parseLong(Feature.getProperty("AltFestivalFirstSpawn", "120000"));
			ALT_FESTIVAL_FIRST_SWARM = Long.parseLong(Feature.getProperty("AltFestivalFirstSwarm", "300000"));
			ALT_FESTIVAL_SECOND_SPAWN = Long.parseLong(Feature.getProperty("AltFestivalSecondSpawn", "540000"));
			ALT_FESTIVAL_SECOND_SWARM = Long.parseLong(Feature.getProperty("AltFestivalSecondSwarm", "720000"));
			ALT_FESTIVAL_CHEST_SPAWN = Long.parseLong(Feature.getProperty("AltFestivalChestSpawn", "900000"));
			ALT_SIEGE_DAWN_GATES_PDEF_MULT = Double.parseDouble(Feature.getProperty("AltDawnGatesPdefMult", "1.1"));
			ALT_SIEGE_DUSK_GATES_PDEF_MULT = Double.parseDouble(Feature.getProperty("AltDuskGatesPdefMult", "0.8"));
			ALT_SIEGE_DAWN_GATES_MDEF_MULT = Double.parseDouble(Feature.getProperty("AltDawnGatesMdefMult", "1.1"));
			ALT_SIEGE_DUSK_GATES_MDEF_MULT = Double.parseDouble(Feature.getProperty("AltDuskGatesMdefMult", "0.8"));
			ALT_STRICT_SEVENSIGNS = Boolean.parseBoolean(Feature.getProperty("StrictSevenSigns", "True"));
			ALT_SEVENSIGNS_LAZY_UPDATE = Boolean.parseBoolean(Feature.getProperty("AltSevenSignsLazyUpdate", "True"));
			
			SSQ_DAWN_TICKET_QUANTITY = Integer.parseInt(Feature.getProperty("SevenSignsDawnTicketQuantity", "300"));
			SSQ_DAWN_TICKET_PRICE = Integer.parseInt(Feature.getProperty("SevenSignsDawnTicketPrice", "1000"));
			SSQ_DAWN_TICKET_BUNDLE = Integer.parseInt(Feature.getProperty("SevenSignsDawnTicketBundle", "10"));
			SSQ_MANORS_AGREEMENT_ID = Integer.parseInt(Feature.getProperty("SevenSignsManorsAgreementId", "6388"));
			SSQ_JOIN_DAWN_ADENA_FEE = Integer.parseInt(Feature.getProperty("SevenSignsJoinDawnFee", "50000"));
			
			TAKE_FORT_POINTS = Integer.parseInt(Feature.getProperty("TakeFortPoints", "200"));
			LOOSE_FORT_POINTS = Integer.parseInt(Feature.getProperty("LooseFortPoints", "0"));
			TAKE_CASTLE_POINTS = Integer.parseInt(Feature.getProperty("TakeCastlePoints", "1500"));
			LOOSE_CASTLE_POINTS = Integer.parseInt(Feature.getProperty("LooseCastlePoints", "3000"));
			CASTLE_DEFENDED_POINTS = Integer.parseInt(Feature.getProperty("CastleDefendedPoints", "750"));
			FESTIVAL_WIN_POINTS = Integer.parseInt(Feature.getProperty("FestivalOfDarknessWin", "200"));
			HERO_POINTS = Integer.parseInt(Feature.getProperty("HeroPoints", "1000"));
			ROYAL_GUARD_COST = Integer.parseInt(Feature.getProperty("CreateRoyalGuardCost", "5000"));
			KNIGHT_UNIT_COST = Integer.parseInt(Feature.getProperty("CreateKnightUnitCost", "10000"));
			KNIGHT_REINFORCE_COST = Integer.parseInt(Feature.getProperty("ReinforceKnightUnitCost", "5000"));
			BALLISTA_POINTS = Integer.parseInt(Feature.getProperty("KillBallistaPoints", "30"));
			BLOODALLIANCE_POINTS = Integer.parseInt(Feature.getProperty("BloodAlliancePoints", "500"));
			BLOODOATH_POINTS = Integer.parseInt(Feature.getProperty("BloodOathPoints", "200"));
			KNIGHTSEPAULETTE_POINTS = Integer.parseInt(Feature.getProperty("KnightsEpaulettePoints", "20"));
			REPUTATION_SCORE_PER_KILL = Integer.parseInt(Feature.getProperty("ReputationScorePerKill", "1"));
			JOIN_ACADEMY_MIN_REP_SCORE = Integer.parseInt(Feature.getProperty("CompleteAcademyMinPoints", "190"));
			JOIN_ACADEMY_MAX_REP_SCORE = Integer.parseInt(Feature.getProperty("CompleteAcademyMaxPoints", "650"));
			RAID_RANKING_1ST = Integer.parseInt(Feature.getProperty("1stRaidRankingPoints", "1250"));
			RAID_RANKING_2ND = Integer.parseInt(Feature.getProperty("2ndRaidRankingPoints", "900"));
			RAID_RANKING_3RD = Integer.parseInt(Feature.getProperty("3rdRaidRankingPoints", "700"));
			RAID_RANKING_4TH = Integer.parseInt(Feature.getProperty("4thRaidRankingPoints", "600"));
			RAID_RANKING_5TH = Integer.parseInt(Feature.getProperty("5thRaidRankingPoints", "450"));
			RAID_RANKING_6TH = Integer.parseInt(Feature.getProperty("6thRaidRankingPoints", "350"));
			RAID_RANKING_7TH = Integer.parseInt(Feature.getProperty("7thRaidRankingPoints", "300"));
			RAID_RANKING_8TH = Integer.parseInt(Feature.getProperty("8thRaidRankingPoints", "200"));
			RAID_RANKING_9TH = Integer.parseInt(Feature.getProperty("9thRaidRankingPoints", "150"));
			RAID_RANKING_10TH = Integer.parseInt(Feature.getProperty("10thRaidRankingPoints", "100"));
			RAID_RANKING_UP_TO_50TH = Integer.parseInt(Feature.getProperty("UpTo50thRaidRankingPoints", "25"));
			RAID_RANKING_UP_TO_100TH = Integer.parseInt(Feature.getProperty("UpTo100thRaidRankingPoints", "12"));
			CLAN_LEVEL_6_COST = Integer.parseInt(Feature.getProperty("ClanLevel6Cost", "5000"));
			CLAN_LEVEL_7_COST = Integer.parseInt(Feature.getProperty("ClanLevel7Cost", "10000"));
			CLAN_LEVEL_8_COST = Integer.parseInt(Feature.getProperty("ClanLevel8Cost", "20000"));
			CLAN_LEVEL_9_COST = Integer.parseInt(Feature.getProperty("ClanLevel9Cost", "40000"));
			CLAN_LEVEL_10_COST = Integer.parseInt(Feature.getProperty("ClanLevel10Cost", "40000"));
			CLAN_LEVEL_11_COST = Integer.parseInt(Feature.getProperty("ClanLevel11Cost", "75000"));
			CLAN_LEVEL_6_REQUIREMENT = Integer.parseInt(Feature.getProperty("ClanLevel6Requirement", "30"));
			CLAN_LEVEL_7_REQUIREMENT = Integer.parseInt(Feature.getProperty("ClanLevel7Requirement", "50"));
			CLAN_LEVEL_8_REQUIREMENT = Integer.parseInt(Feature.getProperty("ClanLevel8Requirement", "80"));
			CLAN_LEVEL_9_REQUIREMENT = Integer.parseInt(Feature.getProperty("ClanLevel9Requirement", "120"));
			CLAN_LEVEL_10_REQUIREMENT = Integer.parseInt(Feature.getProperty("ClanLevel10Requirement", "140"));
			CLAN_LEVEL_11_REQUIREMENT = Integer.parseInt(Feature.getProperty("ClanLevel11Requirement", "170"));
			ALLOW_WYVERN_DURING_SIEGE = Boolean.parseBoolean(Feature.getProperty("AllowRideWyvernDuringSiege", "True"));
			
			// Load Character L2Properties file (if exists)
			L2Properties Character = new L2Properties();
			final File chars = new File(CHARACTER_CONFIG_FILE);
			try (InputStream is = new FileInputStream(chars))
			{
				Character.load(is);
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "Error while loading Characters settings!", e);
			}
			
			ALT_GAME_DELEVEL = Boolean.parseBoolean(Character.getProperty("Delevel", "true"));
			DECREASE_SKILL_LEVEL = Boolean.parseBoolean(Character.getProperty("DecreaseSkillOnDelevel", "true"));
			ALT_WEIGHT_LIMIT = Double.parseDouble(Character.getProperty("AltWeightLimit", "1"));
			RUN_SPD_BOOST = Integer.parseInt(Character.getProperty("RunSpeedBoost", "0"));
			DEATH_PENALTY_CHANCE = Integer.parseInt(Character.getProperty("DeathPenaltyChance", "20"));
			RESPAWN_RESTORE_CP = Double.parseDouble(Character.getProperty("RespawnRestoreCP", "0")) / 100;
			RESPAWN_RESTORE_HP = Double.parseDouble(Character.getProperty("RespawnRestoreHP", "65")) / 100;
			RESPAWN_RESTORE_MP = Double.parseDouble(Character.getProperty("RespawnRestoreMP", "0")) / 100;
			HP_REGEN_MULTIPLIER = Double.parseDouble(Character.getProperty("HpRegenMultiplier", "100")) / 100;
			MP_REGEN_MULTIPLIER = Double.parseDouble(Character.getProperty("MpRegenMultiplier", "100")) / 100;
			CP_REGEN_MULTIPLIER = Double.parseDouble(Character.getProperty("CpRegenMultiplier", "100")) / 100;
			ALT_GAME_TIREDNESS = Boolean.parseBoolean(Character.getProperty("AltGameTiredness", "false"));
			ENABLE_MODIFY_SKILL_DURATION = Boolean.parseBoolean(Character.getProperty("EnableModifySkillDuration", "false"));
			
			// Create Map only if enabled
			if (ENABLE_MODIFY_SKILL_DURATION)
			{
				String[] propertySplit = Character.getProperty("SkillDurationList", "").split(";");
				SKILL_DURATION_LIST = new HashMap<>(propertySplit.length);
				for (String skill : propertySplit)
				{
					String[] skillSplit = skill.split(",");
					if (skillSplit.length != 2)
					{
						_log.warning(StringUtil.concat("[SkillDurationList]: invalid config property -> SkillDurationList \"", skill, "\""));
					}
					else
					{
						try
						{
							SKILL_DURATION_LIST.put(Integer.parseInt(skillSplit[0]), Integer.parseInt(skillSplit[1]));
						}
						catch (NumberFormatException nfe)
						{
							if (!skill.isEmpty())
							{
								_log.warning(StringUtil.concat("[SkillDurationList]: invalid config property -> SkillList \"", skillSplit[0], "\"", skillSplit[1]));
							}
						}
					}
				}
			}
			ENABLE_MODIFY_SKILL_REUSE = Boolean.parseBoolean(Character.getProperty("EnableModifySkillReuse", "false"));
			// Create Map only if enabled
			if (ENABLE_MODIFY_SKILL_REUSE)
			{
				String[] propertySplit = Character.getProperty("SkillReuseList", "").split(";");
				SKILL_REUSE_LIST = new HashMap<>(propertySplit.length);
				for (String skill : propertySplit)
				{
					String[] skillSplit = skill.split(",");
					if (skillSplit.length != 2)
					{
						_log.warning(StringUtil.concat("[SkillReuseList]: invalid config property -> SkillReuseList \"", skill, "\""));
					}
					else
					{
						try
						{
							SKILL_REUSE_LIST.put(Integer.parseInt(skillSplit[0]), Integer.parseInt(skillSplit[1]));
						}
						catch (NumberFormatException nfe)
						{
							if (!skill.isEmpty())
							{
								_log.warning(StringUtil.concat("[SkillReuseList]: invalid config property -> SkillList \"", skillSplit[0], "\"", skillSplit[1]));
							}
						}
					}
				}
			}
			
			AUTO_LEARN_SKILLS = Boolean.parseBoolean(Character.getProperty("AutoLearnSkills", "False"));
			AUTO_LEARN_FS_SKILLS = Boolean.parseBoolean(Character.getProperty("AutoLearnForgottenScrollSkills", "False"));
			AUTO_LOOT_HERBS = Boolean.parseBoolean(Character.getProperty("AutoLootHerbs", "false"));
			BUFFS_MAX_AMOUNT = Byte.parseByte(Character.getProperty("MaxBuffAmount", "20"));
			TRIGGERED_BUFFS_MAX_AMOUNT = Byte.parseByte(Character.getProperty("MaxTriggeredBuffAmount", "12"));
			DANCES_MAX_AMOUNT = Byte.parseByte(Character.getProperty("MaxDanceAmount", "12"));
			DANCE_CANCEL_BUFF = Boolean.parseBoolean(Character.getProperty("DanceCancelBuff", "false"));
			DANCE_CONSUME_ADDITIONAL_MP = Boolean.parseBoolean(Character.getProperty("DanceConsumeAdditionalMP", "true"));
			AUTO_LEARN_DIVINE_INSPIRATION = Boolean.parseBoolean(Character.getProperty("AutoLearnDivineInspiration", "false"));
			ALT_GAME_CANCEL_BOW = Character.getProperty("AltGameCancelByHit", "Cast").equalsIgnoreCase("bow") || Character.getProperty("AltGameCancelByHit", "Cast").equalsIgnoreCase("all");
			ALT_GAME_CANCEL_CAST = Character.getProperty("AltGameCancelByHit", "Cast").equalsIgnoreCase("cast") || Character.getProperty("AltGameCancelByHit", "Cast").equalsIgnoreCase("all");
			EFFECT_CANCELING = Boolean.parseBoolean(Character.getProperty("CancelLesserEffect", "True"));
			ALT_GAME_MAGICFAILURES = Boolean.parseBoolean(Character.getProperty("MagicFailures", "true"));
			PLAYER_FAKEDEATH_UP_PROTECTION = Integer.parseInt(Character.getProperty("PlayerFakeDeathUpProtection", "0"));
			STORE_SKILL_COOLTIME = Boolean.parseBoolean(Character.getProperty("StoreSkillCooltime", "true"));
			SUBCLASS_STORE_SKILL_COOLTIME = Boolean.parseBoolean(Character.getProperty("SubclassStoreSkillCooltime", "false"));
			SUMMON_STORE_SKILL_COOLTIME = Boolean.parseBoolean(Character.getProperty("SummonStoreSkillCooltime", "True"));
			ALT_GAME_SHIELD_BLOCKS = Boolean.parseBoolean(Character.getProperty("AltShieldBlocks", "false"));
			ALT_PERFECT_SHLD_BLOCK = Integer.parseInt(Character.getProperty("AltPerfectShieldBlockRate", "10"));
			ALLOW_CLASS_MASTERS = Boolean.parseBoolean(Character.getProperty("AllowClassMasters", "False"));
			ALLOW_ENTIRE_TREE = Boolean.parseBoolean(Character.getProperty("AllowEntireTree", "False"));
			ALTERNATE_CLASS_MASTER = Boolean.parseBoolean(Character.getProperty("AlternateClassMaster", "False"));
			if (ALLOW_CLASS_MASTERS || ALTERNATE_CLASS_MASTER)
			{
				CLASS_MASTER_SETTINGS = new ClassMasterSettings(Character.getProperty("ConfigClassMaster"));
			}
			LIFE_CRYSTAL_NEEDED = Boolean.parseBoolean(Character.getProperty("LifeCrystalNeeded", "true"));
			ES_SP_BOOK_NEEDED = Boolean.parseBoolean(Character.getProperty("EnchantSkillSpBookNeeded", "true"));
			DIVINE_SP_BOOK_NEEDED = Boolean.parseBoolean(Character.getProperty("DivineInspirationSpBookNeeded", "true"));
			ALT_GAME_SKILL_LEARN = Boolean.parseBoolean(Character.getProperty("AltGameSkillLearn", "false"));
			ALT_GAME_SUBCLASS_WITHOUT_QUESTS = Boolean.parseBoolean(Character.getProperty("AltSubClassWithoutQuests", "False"));
			ALT_GAME_SUBCLASS_EVERYWHERE = Boolean.parseBoolean(Character.getProperty("AltSubclassEverywhere", "False"));
			RESTORE_SERVITOR_ON_RECONNECT = Boolean.parseBoolean(Character.getProperty("RestoreServitorOnReconnect", "True"));
			RESTORE_PET_ON_RECONNECT = Boolean.parseBoolean(Character.getProperty("RestorePetOnReconnect", "True"));
			ALLOW_TRANSFORM_WITHOUT_QUEST = Boolean.parseBoolean(Character.getProperty("AltTransformationWithoutQuest", "False"));
			FEE_DELETE_TRANSFER_SKILLS = Integer.parseInt(Character.getProperty("FeeDeleteTransferSkills", "10000000"));
			FEE_DELETE_SUBCLASS_SKILLS = Integer.parseInt(Character.getProperty("FeeDeleteSubClassSkills", "10000000"));
			ENABLE_VITALITY = Boolean.parseBoolean(Character.getProperty("EnableVitality", "True"));
			RECOVER_VITALITY_ON_RECONNECT = Boolean.parseBoolean(Character.getProperty("RecoverVitalityOnReconnect", "True"));
			STARTING_VITALITY_POINTS = Integer.parseInt(Character.getProperty("StartingVitalityPoints", "20000"));
			MAX_BONUS_EXP = Double.parseDouble(Character.getProperty("MaxExpBonus", "3.5"));
			MAX_BONUS_SP = Double.parseDouble(Character.getProperty("MaxSpBonus", "3.5"));
			MAX_RUN_SPEED = Integer.parseInt(Character.getProperty("MaxRunSpeed", "300"));
			MAX_PCRIT_RATE = Integer.parseInt(Character.getProperty("MaxPCritRate", "500"));
			MAX_MCRIT_RATE = Integer.parseInt(Character.getProperty("MaxMCritRate", "200"));
			MAX_PATK_SPEED = Integer.parseInt(Character.getProperty("MaxPAtkSpeed", "1500"));
			MAX_MATK_SPEED = Integer.parseInt(Character.getProperty("MaxMAtkSpeed", "1999"));
			MAX_EVASION = Integer.parseInt(Character.getProperty("MaxEvasion", "250"));
			MIN_ABNORMAL_STATE_SUCCESS_RATE = Integer.parseInt(Character.getProperty("MinAbnormalStateSuccessRate", "10"));
			MAX_ABNORMAL_STATE_SUCCESS_RATE = Integer.parseInt(Character.getProperty("MaxAbnormalStateSuccessRate", "90"));
			MAX_SUBCLASS = Byte.parseByte(Character.getProperty("MaxSubclass", "3"));
			BASE_SUBCLASS_LEVEL = Byte.parseByte(Character.getProperty("BaseSubclassLevel", "40"));
			MAX_SUBCLASS_LEVEL = Byte.parseByte(Character.getProperty("MaxSubclassLevel", "80"));
			MAX_PVTSTORESELL_SLOTS_DWARF = Integer.parseInt(Character.getProperty("MaxPvtStoreSellSlotsDwarf", "4"));
			MAX_PVTSTORESELL_SLOTS_OTHER = Integer.parseInt(Character.getProperty("MaxPvtStoreSellSlotsOther", "3"));
			MAX_PVTSTOREBUY_SLOTS_DWARF = Integer.parseInt(Character.getProperty("MaxPvtStoreBuySlotsDwarf", "5"));
			MAX_PVTSTOREBUY_SLOTS_OTHER = Integer.parseInt(Character.getProperty("MaxPvtStoreBuySlotsOther", "4"));
			INVENTORY_MAXIMUM_NO_DWARF = Integer.parseInt(Character.getProperty("MaximumSlotsForNoDwarf", "80"));
			INVENTORY_MAXIMUM_DWARF = Integer.parseInt(Character.getProperty("MaximumSlotsForDwarf", "100"));
			INVENTORY_MAXIMUM_GM = Integer.parseInt(Character.getProperty("MaximumSlotsForGMPlayer", "250"));
			INVENTORY_MAXIMUM_QUEST_ITEMS = Integer.parseInt(Character.getProperty("MaximumSlotsForQuestItems", "100"));
			MAX_ITEM_IN_PACKET = Math.max(INVENTORY_MAXIMUM_NO_DWARF, Math.max(INVENTORY_MAXIMUM_DWARF, INVENTORY_MAXIMUM_GM));
			WAREHOUSE_SLOTS_DWARF = Integer.parseInt(Character.getProperty("MaximumWarehouseSlotsForDwarf", "120"));
			WAREHOUSE_SLOTS_NO_DWARF = Integer.parseInt(Character.getProperty("MaximumWarehouseSlotsForNoDwarf", "100"));
			WAREHOUSE_SLOTS_CLAN = Integer.parseInt(Character.getProperty("MaximumWarehouseSlotsForClan", "150"));
			ALT_FREIGHT_SLOTS = Integer.parseInt(Character.getProperty("MaximumFreightSlots", "200"));
			ALT_FREIGHT_PRICE = Integer.parseInt(Character.getProperty("FreightPrice", "1000"));
			ENCHANT_CHANCE = Double.parseDouble(Character.getProperty("EnchantChance", "66.66"));
			MAX_ENCHANT_LEVEL = Integer.parseInt(Character.getProperty("MaxEnchantLevel", "0"));
			ENCHANT_CHANCE_ELEMENT_STONE = Double.parseDouble(Character.getProperty("EnchantChanceElementStone", "50"));
			ENCHANT_CHANCE_ELEMENT_CRYSTAL = Double.parseDouble(Character.getProperty("EnchantChanceElementCrystal", "30"));
			ENCHANT_CHANCE_ELEMENT_JEWEL = Double.parseDouble(Character.getProperty("EnchantChanceElementJewel", "20"));
			ENCHANT_CHANCE_ELEMENT_ENERGY = Double.parseDouble(Character.getProperty("EnchantChanceElementEnergy", "10"));
			ENCHANT_SAFE_MAX = Integer.parseInt(Character.getProperty("EnchantSafeMax", "3"));
			ENCHANT_SAFE_MAX_FULL = Integer.parseInt(Character.getProperty("EnchantSafeMaxFull", "4"));
			String[] notenchantable = Character.getProperty("EnchantBlackList", "7816,7817,7818,7819,7820,7821,7822,7823,7824,7825,7826,7827,7828,7829,7830,7831,13293,13294,13296").split(",");
			ENCHANT_BLACKLIST = new int[notenchantable.length];
			for (int i = 0; i < notenchantable.length; i++)
			{
				ENCHANT_BLACKLIST[i] = Integer.parseInt(notenchantable[i]);
			}
			Arrays.sort(ENCHANT_BLACKLIST);
			
			AUGMENTATION_NG_SKILL_CHANCE = Integer.parseInt(Character.getProperty("AugmentationNGSkillChance", "15"));
			AUGMENTATION_NG_GLOW_CHANCE = Integer.parseInt(Character.getProperty("AugmentationNGGlowChance", "0"));
			AUGMENTATION_MID_SKILL_CHANCE = Integer.parseInt(Character.getProperty("AugmentationMidSkillChance", "30"));
			AUGMENTATION_MID_GLOW_CHANCE = Integer.parseInt(Character.getProperty("AugmentationMidGlowChance", "40"));
			AUGMENTATION_HIGH_SKILL_CHANCE = Integer.parseInt(Character.getProperty("AugmentationHighSkillChance", "45"));
			AUGMENTATION_HIGH_GLOW_CHANCE = Integer.parseInt(Character.getProperty("AugmentationHighGlowChance", "70"));
			AUGMENTATION_TOP_SKILL_CHANCE = Integer.parseInt(Character.getProperty("AugmentationTopSkillChance", "60"));
			AUGMENTATION_TOP_GLOW_CHANCE = Integer.parseInt(Character.getProperty("AugmentationTopGlowChance", "100"));
			AUGMENTATION_BASESTAT_CHANCE = Integer.parseInt(Character.getProperty("AugmentationBaseStatChance", "1"));
			AUGMENTATION_ACC_SKILL_CHANCE = Integer.parseInt(Character.getProperty("AugmentationAccSkillChance", "0"));
			
			String[] array = Character.getProperty("AugmentationBlackList", "6656,6657,6658,6659,6660,6661,6662,8191,10170,10314,13740,13741,13742,13743,13744,13745,13746,13747,13748,14592,14593,14594,14595,14596,14597,14598,14599,14600,14664,14665,14666,14667,14668,14669,14670,14671,14672,14801,14802,14803,14804,14805,14806,14807,14808,14809,15282,15283,15284,15285,15286,15287,15288,15289,15290,15291,15292,15293,15294,15295,15296,15297,15298,15299,16025,16026,21712,22173,22174,22175").split(",");
			AUGMENTATION_BLACKLIST = new int[array.length];
			
			for (int i = 0; i < array.length; i++)
			{
				AUGMENTATION_BLACKLIST[i] = Integer.parseInt(array[i]);
			}
			
			Arrays.sort(AUGMENTATION_BLACKLIST);
			
			ALT_GAME_KARMA_PLAYER_CAN_BE_KILLED_IN_PEACEZONE = Boolean.parseBoolean(Character.getProperty("AltKarmaPlayerCanBeKilledInPeaceZone", "false"));
			ALT_GAME_KARMA_PLAYER_CAN_SHOP = Boolean.parseBoolean(Character.getProperty("AltKarmaPlayerCanShop", "true"));
			ALT_GAME_KARMA_PLAYER_CAN_TELEPORT = Boolean.parseBoolean(Character.getProperty("AltKarmaPlayerCanTeleport", "true"));
			ALT_GAME_KARMA_PLAYER_CAN_USE_GK = Boolean.parseBoolean(Character.getProperty("AltKarmaPlayerCanUseGK", "false"));
			ALT_GAME_KARMA_PLAYER_CAN_TRADE = Boolean.parseBoolean(Character.getProperty("AltKarmaPlayerCanTrade", "true"));
			ALT_GAME_KARMA_PLAYER_CAN_USE_WAREHOUSE = Boolean.parseBoolean(Character.getProperty("AltKarmaPlayerCanUseWareHouse", "true"));
			MAX_PERSONAL_FAME_POINTS = Integer.parseInt(Character.getProperty("MaxPersonalFamePoints", "100000"));
			FORTRESS_ZONE_FAME_TASK_FREQUENCY = Integer.parseInt(Character.getProperty("FortressZoneFameTaskFrequency", "300"));
			FORTRESS_ZONE_FAME_AQUIRE_POINTS = Integer.parseInt(Character.getProperty("FortressZoneFameAquirePoints", "31"));
			CASTLE_ZONE_FAME_TASK_FREQUENCY = Integer.parseInt(Character.getProperty("CastleZoneFameTaskFrequency", "300"));
			CASTLE_ZONE_FAME_AQUIRE_POINTS = Integer.parseInt(Character.getProperty("CastleZoneFameAquirePoints", "125"));
			FAME_FOR_DEAD_PLAYERS = Boolean.parseBoolean(Character.getProperty("FameForDeadPlayers", "true"));
			IS_CRAFTING_ENABLED = Boolean.parseBoolean(Character.getProperty("CraftingEnabled", "true"));
			CRAFT_MASTERWORK = Boolean.parseBoolean(Character.getProperty("CraftMasterwork", "True"));
			DWARF_RECIPE_LIMIT = Integer.parseInt(Character.getProperty("DwarfRecipeLimit", "50"));
			COMMON_RECIPE_LIMIT = Integer.parseInt(Character.getProperty("CommonRecipeLimit", "50"));
			ALT_GAME_CREATION = Boolean.parseBoolean(Character.getProperty("AltGameCreation", "false"));
			ALT_GAME_CREATION_SPEED = Double.parseDouble(Character.getProperty("AltGameCreationSpeed", "1"));
			ALT_GAME_CREATION_XP_RATE = Double.parseDouble(Character.getProperty("AltGameCreationXpRate", "1"));
			ALT_GAME_CREATION_SP_RATE = Double.parseDouble(Character.getProperty("AltGameCreationSpRate", "1"));
			ALT_GAME_CREATION_RARE_XPSP_RATE = Double.parseDouble(Character.getProperty("AltGameCreationRareXpSpRate", "2"));
			ALT_BLACKSMITH_USE_RECIPES = Boolean.parseBoolean(Character.getProperty("AltBlacksmithUseRecipes", "true"));
			ALT_CLAN_JOIN_DAYS = Integer.parseInt(Character.getProperty("DaysBeforeJoinAClan", "1"));
			ALT_CLAN_CREATE_DAYS = Integer.parseInt(Character.getProperty("DaysBeforeCreateAClan", "10"));
			ALT_CLAN_DISSOLVE_DAYS = Integer.parseInt(Character.getProperty("DaysToPassToDissolveAClan", "7"));
			ALT_ALLY_JOIN_DAYS_WHEN_LEAVED = Integer.parseInt(Character.getProperty("DaysBeforeJoinAllyWhenLeaved", "1"));
			ALT_ALLY_JOIN_DAYS_WHEN_DISMISSED = Integer.parseInt(Character.getProperty("DaysBeforeJoinAllyWhenDismissed", "1"));
			ALT_ACCEPT_CLAN_DAYS_WHEN_DISMISSED = Integer.parseInt(Character.getProperty("DaysBeforeAcceptNewClanWhenDismissed", "1"));
			ALT_CREATE_ALLY_DAYS_WHEN_DISSOLVED = Integer.parseInt(Character.getProperty("DaysBeforeCreateNewAllyWhenDissolved", "1"));
			ALT_MAX_NUM_OF_CLANS_IN_ALLY = Integer.parseInt(Character.getProperty("AltMaxNumOfClansInAlly", "3"));
			ALT_CLAN_MEMBERS_FOR_WAR = Integer.parseInt(Character.getProperty("AltClanMembersForWar", "15"));
			ALT_MEMBERS_CAN_WITHDRAW_FROM_CLANWH = Boolean.parseBoolean(Character.getProperty("AltMembersCanWithdrawFromClanWH", "false"));
			REMOVE_CASTLE_CIRCLETS = Boolean.parseBoolean(Character.getProperty("RemoveCastleCirclets", "true"));
			ALT_PARTY_RANGE = Integer.parseInt(Character.getProperty("AltPartyRange", "1600"));
			ALT_PARTY_RANGE2 = Integer.parseInt(Character.getProperty("AltPartyRange2", "1400"));
			ALT_LEAVE_PARTY_LEADER = Boolean.parseBoolean(Character.getProperty("AltLeavePartyLeader", "False"));
			INITIAL_EQUIPMENT_EVENT = Boolean.parseBoolean(Character.getProperty("InitialEquipmentEvent", "False"));
			STARTING_ADENA = Long.parseLong(Character.getProperty("StartingAdena", "0"));
			STARTING_LEVEL = Byte.parseByte(Character.getProperty("StartingLevel", "1"));
			STARTING_SP = Integer.parseInt(Character.getProperty("StartingSP", "0"));
			MAX_ADENA = Long.parseLong(Character.getProperty("MaxAdena", "99900000000"));
			if (MAX_ADENA < 0)
			{
				MAX_ADENA = Long.MAX_VALUE;
			}
			AUTO_LOOT = Boolean.parseBoolean(Character.getProperty("AutoLoot", "false"));
			AUTO_LOOT_RAIDS = Boolean.parseBoolean(Character.getProperty("AutoLootRaids", "false"));
			LOOT_RAIDS_PRIVILEGE_INTERVAL = Integer.parseInt(Character.getProperty("RaidLootRightsInterval", "900")) * 1000;
			LOOT_RAIDS_PRIVILEGE_CC_SIZE = Integer.parseInt(Character.getProperty("RaidLootRightsCCSize", "45"));
			UNSTUCK_INTERVAL = Integer.parseInt(Character.getProperty("UnstuckInterval", "300"));
			TELEPORT_WATCHDOG_TIMEOUT = Integer.parseInt(Character.getProperty("TeleportWatchdogTimeout", "0"));
			PLAYER_SPAWN_PROTECTION = Integer.parseInt(Character.getProperty("PlayerSpawnProtection", "0"));
			String[] items = Character.getProperty("PlayerSpawnProtectionAllowedItems", "0").split(",");
			SPAWN_PROTECTION_ALLOWED_ITEMS = new ArrayList<>(items.length);
			for (String item : items)
			{
				Integer itm = 0;
				try
				{
					itm = Integer.parseInt(item);
				}
				catch (NumberFormatException nfe)
				{
					_log.warning("Player Spawn Protection: Wrong ItemId passed: " + item);
					_log.warning(nfe.getMessage());
				}
				if (itm != 0)
				{
					SPAWN_PROTECTION_ALLOWED_ITEMS.add(itm);
				}
			}
			SPAWN_PROTECTION_ALLOWED_ITEMS.trimToSize();
			PLAYER_TELEPORT_PROTECTION = Integer.parseInt(Character.getProperty("PlayerTeleportProtection", "0"));
			RANDOM_RESPAWN_IN_TOWN_ENABLED = Boolean.parseBoolean(Character.getProperty("RandomRespawnInTownEnabled", "True"));
			OFFSET_ON_TELEPORT_ENABLED = Boolean.parseBoolean(Character.getProperty("OffsetOnTeleportEnabled", "True"));
			MAX_OFFSET_ON_TELEPORT = Integer.parseInt(Character.getProperty("MaxOffsetOnTeleport", "50"));
			RESTORE_PLAYER_INSTANCE = Boolean.parseBoolean(Character.getProperty("RestorePlayerInstance", "False"));
			ALLOW_SUMMON_TO_INSTANCE = Boolean.parseBoolean(Character.getProperty("AllowSummonToInstance", "True"));
			EJECT_DEAD_PLAYER_TIME = 1000 * Integer.parseInt(Character.getProperty("EjectDeadPlayerTime", "60"));
			PETITIONING_ALLOWED = Boolean.parseBoolean(Character.getProperty("PetitioningAllowed", "True"));
			MAX_PETITIONS_PER_PLAYER = Integer.parseInt(Character.getProperty("MaxPetitionsPerPlayer", "5"));
			MAX_PETITIONS_PENDING = Integer.parseInt(Character.getProperty("MaxPetitionsPending", "25"));
			ALT_GAME_FREE_TELEPORT = Boolean.parseBoolean(Character.getProperty("AltFreeTeleporting", "False"));
			DELETE_DAYS = Integer.parseInt(Character.getProperty("DeleteCharAfterDays", "7"));
			ALT_GAME_EXPONENT_XP = Float.parseFloat(Character.getProperty("AltGameExponentXp", "0."));
			ALT_GAME_EXPONENT_SP = Float.parseFloat(Character.getProperty("AltGameExponentSp", "0."));
			PARTY_XP_CUTOFF_METHOD = Character.getProperty("PartyXpCutoffMethod", "level");
			PARTY_XP_CUTOFF_PERCENT = Double.parseDouble(Character.getProperty("PartyXpCutoffPercent", "3."));
			PARTY_XP_CUTOFF_LEVEL = Integer.parseInt(Character.getProperty("PartyXpCutoffLevel", "20"));
			DISABLE_TUTORIAL = Boolean.parseBoolean(Character.getProperty("DisableTutorial", "False"));
			EXPERTISE_PENALTY = Boolean.parseBoolean(Character.getProperty("ExpertisePenalty", "True"));
			STORE_RECIPE_SHOPLIST = Boolean.parseBoolean(Character.getProperty("StoreRecipeShopList", "False"));
			STORE_UI_SETTINGS = Boolean.parseBoolean(Character.getProperty("StoreCharUiSettings", "False"));
			FORBIDDEN_NAMES = Character.getProperty("ForbiddenNames", "").split(",");
			SILENCE_MODE_EXCLUDE = Boolean.parseBoolean(Character.getProperty("SilenceModeExclude", "False"));
			PLAYER_MOVEMENT_BLOCK_TIME = Integer.parseInt(Character.getProperty("NpcTalkBlockingTime", "0")) * 1000;
			
			// Load L2J Server Version L2Properties file (if exists)
			L2Properties serverVersion = new L2Properties();
			final File server_ver = new File(SERVER_VERSION_FILE);
			try (InputStream is = new FileInputStream(server_ver))
			{
				serverVersion.load(is);
			}
			catch (IOException e)
			{
				_log.log(Level.WARNING, "Error while loading Server version!", e);
			}
			
			SERVER_VERSION = serverVersion.getProperty("version", "Unsupported Custom Version.");
			SERVER_BUILD_DATE = serverVersion.getProperty("builddate", "Undefined Date.");
			
			// Load L2J Datapack Version L2Properties file (if exists)
			L2Properties dpVersion = new L2Properties();
			final File dp_ver = new File(DATAPACK_VERSION_FILE);
			try (InputStream is = new FileInputStream(dp_ver))
			{
				dpVersion.load(is);
			}
			catch (IOException e)
			{
				_log.log(Level.WARNING, "Error while loading Datapack version!", e);
			}
			
			DATAPACK_VERSION = dpVersion.getProperty("version", "Unsupported Custom Version.");
			
			// Load Telnet L2Properties file (if exists)
			L2Properties telnetSettings = new L2Properties();
			final File telnet = new File(TELNET_FILE);
			try (InputStream is = new FileInputStream(telnet))
			{
				telnetSettings.load(is);
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "Error while loading Telnet settings!", e);
			}
			
			IS_TELNET_ENABLED = Boolean.parseBoolean(telnetSettings.getProperty("EnableTelnet", "false"));
			
			// MMO
			L2Properties mmoSettings = new L2Properties();
			final File mmo = new File(MMO_CONFIG_FILE);
			try (InputStream is = new FileInputStream(mmo))
			{
				mmoSettings.load(is);
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "Error while loading MMO settings!", e);
			}
			
			MMO_SELECTOR_SLEEP_TIME = Integer.parseInt(mmoSettings.getProperty("SleepTime", "20"));
			MMO_MAX_SEND_PER_PASS = Integer.parseInt(mmoSettings.getProperty("MaxSendPerPass", "12"));
			MMO_MAX_READ_PER_PASS = Integer.parseInt(mmoSettings.getProperty("MaxReadPerPass", "12"));
			MMO_HELPER_BUFFER_COUNT = Integer.parseInt(mmoSettings.getProperty("HelperBufferCount", "20"));
			MMO_TCP_NODELAY = Boolean.parseBoolean(mmoSettings.getProperty("TcpNoDelay", "False"));
			
			// Load IdFactory L2Properties file (if exists)
			L2Properties IdFactory = new L2Properties();
			final File Id = new File(ID_CONFIG_FILE);
			try (InputStream is = new FileInputStream(Id))
			{
				IdFactory.load(is);
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "Error while loading IdFactory settings!", e);
			}
			
			IDFACTORY_TYPE = IdFactoryType.valueOf(IdFactory.getProperty("IDFactory", "BitSet"));
			BAD_ID_CHECKING = Boolean.parseBoolean(IdFactory.getProperty("BadIdChecking", "True"));
			
			// Load General L2Properties file (if exists)
			L2Properties General = new L2Properties();
			final File general = new File(GENERAL_CONFIG_FILE);
			try (InputStream is = new FileInputStream(general))
			{
				General.load(is);
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "Error while loading General settings!", e);
			}
			
			EVERYBODY_HAS_ADMIN_RIGHTS = Boolean.parseBoolean(General.getProperty("EverybodyHasAdminRights", "false"));
			DISPLAY_SERVER_VERSION = Boolean.parseBoolean(General.getProperty("DisplayServerRevision", "True"));
			SERVER_LIST_BRACKET = Boolean.parseBoolean(General.getProperty("ServerListBrackets", "false"));
			SERVER_LIST_TYPE = getServerTypeId(General.getProperty("ServerListType", "Normal").split(","));
			SERVER_LIST_AGE = Integer.parseInt(General.getProperty("ServerListAge", "0"));
			SERVER_GMONLY = Boolean.parseBoolean(General.getProperty("ServerGMOnly", "false"));
			GM_HERO_AURA = Boolean.parseBoolean(General.getProperty("GMHeroAura", "False"));
			GM_STARTUP_INVULNERABLE = Boolean.parseBoolean(General.getProperty("GMStartupInvulnerable", "False"));
			GM_STARTUP_INVISIBLE = Boolean.parseBoolean(General.getProperty("GMStartupInvisible", "False"));
			GM_STARTUP_SILENCE = Boolean.parseBoolean(General.getProperty("GMStartupSilence", "False"));
			GM_STARTUP_AUTO_LIST = Boolean.parseBoolean(General.getProperty("GMStartupAutoList", "False"));
			GM_STARTUP_DIET_MODE = Boolean.parseBoolean(General.getProperty("GMStartupDietMode", "False"));
			GM_ADMIN_MENU_STYLE = General.getProperty("GMAdminMenuStyle", "modern");
			GM_ITEM_RESTRICTION = Boolean.parseBoolean(General.getProperty("GMItemRestriction", "True"));
			GM_SKILL_RESTRICTION = Boolean.parseBoolean(General.getProperty("GMSkillRestriction", "True"));
			GM_TRADE_RESTRICTED_ITEMS = Boolean.parseBoolean(General.getProperty("GMTradeRestrictedItems", "False"));
			GM_RESTART_FIGHTING = Boolean.parseBoolean(General.getProperty("GMRestartFighting", "True"));
			GM_ANNOUNCER_NAME = Boolean.parseBoolean(General.getProperty("GMShowAnnouncerName", "False"));
			GM_CRITANNOUNCER_NAME = Boolean.parseBoolean(General.getProperty("GMShowCritAnnouncerName", "False"));
			GM_GIVE_SPECIAL_SKILLS = Boolean.parseBoolean(General.getProperty("GMGiveSpecialSkills", "False"));
			GM_GIVE_SPECIAL_AURA_SKILLS = Boolean.parseBoolean(General.getProperty("GMGiveSpecialAuraSkills", "False"));
			BYPASS_VALIDATION = Boolean.parseBoolean(General.getProperty("BypassValidation", "True"));
			GAMEGUARD_ENFORCE = Boolean.parseBoolean(General.getProperty("GameGuardEnforce", "False"));
			GAMEGUARD_PROHIBITACTION = Boolean.parseBoolean(General.getProperty("GameGuardProhibitAction", "False"));
			LOG_CHAT = Boolean.parseBoolean(General.getProperty("LogChat", "false"));
			LOG_AUTO_ANNOUNCEMENTS = Boolean.parseBoolean(General.getProperty("LogAutoAnnouncements", "False"));
			LOG_ITEMS = Boolean.parseBoolean(General.getProperty("LogItems", "false"));
			LOG_ITEMS_SMALL_LOG = Boolean.parseBoolean(General.getProperty("LogItemsSmallLog", "false"));
			LOG_ITEM_ENCHANTS = Boolean.parseBoolean(General.getProperty("LogItemEnchants", "false"));
			LOG_SKILL_ENCHANTS = Boolean.parseBoolean(General.getProperty("LogSkillEnchants", "false"));
			GMAUDIT = Boolean.parseBoolean(General.getProperty("GMAudit", "False"));
			LOG_GAME_DAMAGE = Boolean.parseBoolean(General.getProperty("LogGameDamage", "False"));
			LOG_GAME_DAMAGE_THRESHOLD = Integer.parseInt(General.getProperty("LogGameDamageThreshold", "5000"));
			SKILL_CHECK_ENABLE = Boolean.parseBoolean(General.getProperty("SkillCheckEnable", "False"));
			SKILL_CHECK_REMOVE = Boolean.parseBoolean(General.getProperty("SkillCheckRemove", "False"));
			SKILL_CHECK_GM = Boolean.parseBoolean(General.getProperty("SkillCheckGM", "True"));
			DEBUG = Boolean.parseBoolean(General.getProperty("Debug", "false"));
			PACKET_HANDLER_DEBUG = Boolean.parseBoolean(General.getProperty("PacketHandlerDebug", "false"));
			DEVELOPER = Boolean.parseBoolean(General.getProperty("Developer", "false"));
			ACCEPT_GEOEDITOR_CONN = Boolean.parseBoolean(General.getProperty("AcceptGeoeditorConn", "false"));
			ALT_DEV_NO_HANDLERS = Boolean.parseBoolean(General.getProperty("AltDevNoHandlers", "False"));
			ALT_DEV_NO_QUESTS = Boolean.parseBoolean(General.getProperty("AltDevNoQuests", "False"));
			ALT_DEV_NO_SPAWNS = Boolean.parseBoolean(General.getProperty("AltDevNoSpawns", "False"));
			THREAD_P_EFFECTS = Integer.parseInt(General.getProperty("ThreadPoolSizeEffects", "10"));
			THREAD_P_GENERAL = Integer.parseInt(General.getProperty("ThreadPoolSizeGeneral", "13"));
			IO_PACKET_THREAD_CORE_SIZE = Integer.parseInt(General.getProperty("UrgentPacketThreadCoreSize", "2"));
			GENERAL_PACKET_THREAD_CORE_SIZE = Integer.parseInt(General.getProperty("GeneralPacketThreadCoreSize", "4"));
			GENERAL_THREAD_CORE_SIZE = Integer.parseInt(General.getProperty("GeneralThreadCoreSize", "4"));
			AI_MAX_THREAD = Integer.parseInt(General.getProperty("AiMaxThread", "6"));
			CLIENT_PACKET_QUEUE_SIZE = Integer.parseInt(General.getProperty("ClientPacketQueueSize", "0"));
			if (CLIENT_PACKET_QUEUE_SIZE == 0)
			{
				CLIENT_PACKET_QUEUE_SIZE = MMO_MAX_READ_PER_PASS + 2;
			}
			CLIENT_PACKET_QUEUE_MAX_BURST_SIZE = Integer.parseInt(General.getProperty("ClientPacketQueueMaxBurstSize", "0"));
			if (CLIENT_PACKET_QUEUE_MAX_BURST_SIZE == 0)
			{
				CLIENT_PACKET_QUEUE_MAX_BURST_SIZE = MMO_MAX_READ_PER_PASS + 1;
			}
			CLIENT_PACKET_QUEUE_MAX_PACKETS_PER_SECOND = Integer.parseInt(General.getProperty("ClientPacketQueueMaxPacketsPerSecond", "80"));
			CLIENT_PACKET_QUEUE_MEASURE_INTERVAL = Integer.parseInt(General.getProperty("ClientPacketQueueMeasureInterval", "5"));
			CLIENT_PACKET_QUEUE_MAX_AVERAGE_PACKETS_PER_SECOND = Integer.parseInt(General.getProperty("ClientPacketQueueMaxAveragePacketsPerSecond", "40"));
			CLIENT_PACKET_QUEUE_MAX_FLOODS_PER_MIN = Integer.parseInt(General.getProperty("ClientPacketQueueMaxFloodsPerMin", "2"));
			CLIENT_PACKET_QUEUE_MAX_OVERFLOWS_PER_MIN = Integer.parseInt(General.getProperty("ClientPacketQueueMaxOverflowsPerMin", "1"));
			CLIENT_PACKET_QUEUE_MAX_UNDERFLOWS_PER_MIN = Integer.parseInt(General.getProperty("ClientPacketQueueMaxUnderflowsPerMin", "1"));
			CLIENT_PACKET_QUEUE_MAX_UNKNOWN_PER_MIN = Integer.parseInt(General.getProperty("ClientPacketQueueMaxUnknownPerMin", "5"));
			DEADLOCK_DETECTOR = Boolean.parseBoolean(General.getProperty("DeadLockDetector", "True"));
			DEADLOCK_CHECK_INTERVAL = Integer.parseInt(General.getProperty("DeadLockCheckInterval", "20"));
			RESTART_ON_DEADLOCK = Boolean.parseBoolean(General.getProperty("RestartOnDeadlock", "False"));
			ALLOW_DISCARDITEM = Boolean.parseBoolean(General.getProperty("AllowDiscardItem", "True"));
			AUTODESTROY_ITEM_AFTER = Integer.parseInt(General.getProperty("AutoDestroyDroppedItemAfter", "600"));
			HERB_AUTO_DESTROY_TIME = Integer.parseInt(General.getProperty("AutoDestroyHerbTime", "60")) * 1000;
			String[] split = General.getProperty("ListOfProtectedItems", "0").split(",");
			LIST_PROTECTED_ITEMS = new ArrayList<>(split.length);
			for (String id : split)
			{
				LIST_PROTECTED_ITEMS.add(Integer.parseInt(id));
			}
			DATABASE_CLEAN_UP = Boolean.parseBoolean(General.getProperty("DatabaseCleanUp", "true"));
			CONNECTION_CLOSE_TIME = Long.parseLong(General.getProperty("ConnectionCloseTime", "60000"));
			CHAR_STORE_INTERVAL = Integer.parseInt(General.getProperty("CharacterDataStoreInterval", "15"));
			LAZY_ITEMS_UPDATE = Boolean.parseBoolean(General.getProperty("LazyItemsUpdate", "false"));
			UPDATE_ITEMS_ON_CHAR_STORE = Boolean.parseBoolean(General.getProperty("UpdateItemsOnCharStore", "false"));
			DESTROY_DROPPED_PLAYER_ITEM = Boolean.parseBoolean(General.getProperty("DestroyPlayerDroppedItem", "false"));
			DESTROY_EQUIPABLE_PLAYER_ITEM = Boolean.parseBoolean(General.getProperty("DestroyEquipableItem", "false"));
			SAVE_DROPPED_ITEM = Boolean.parseBoolean(General.getProperty("SaveDroppedItem", "false"));
			EMPTY_DROPPED_ITEM_TABLE_AFTER_LOAD = Boolean.parseBoolean(General.getProperty("EmptyDroppedItemTableAfterLoad", "false"));
			SAVE_DROPPED_ITEM_INTERVAL = Integer.parseInt(General.getProperty("SaveDroppedItemInterval", "60")) * 60000;
			CLEAR_DROPPED_ITEM_TABLE = Boolean.parseBoolean(General.getProperty("ClearDroppedItemTable", "false"));
			AUTODELETE_INVALID_QUEST_DATA = Boolean.parseBoolean(General.getProperty("AutoDeleteInvalidQuestData", "False"));
			PRECISE_DROP_CALCULATION = Boolean.parseBoolean(General.getProperty("PreciseDropCalculation", "True"));
			MULTIPLE_ITEM_DROP = Boolean.parseBoolean(General.getProperty("MultipleItemDrop", "True"));
			FORCE_INVENTORY_UPDATE = Boolean.parseBoolean(General.getProperty("ForceInventoryUpdate", "False"));
			LAZY_CACHE = Boolean.parseBoolean(General.getProperty("LazyCache", "True"));
			CACHE_CHAR_NAMES = Boolean.parseBoolean(General.getProperty("CacheCharNames", "True"));
			MIN_NPC_ANIMATION = Integer.parseInt(General.getProperty("MinNPCAnimation", "10"));
			MAX_NPC_ANIMATION = Integer.parseInt(General.getProperty("MaxNPCAnimation", "20"));
			MIN_MONSTER_ANIMATION = Integer.parseInt(General.getProperty("MinMonsterAnimation", "5"));
			MAX_MONSTER_ANIMATION = Integer.parseInt(General.getProperty("MaxMonsterAnimation", "20"));
			MOVE_BASED_KNOWNLIST = Boolean.parseBoolean(General.getProperty("MoveBasedKnownlist", "False"));
			KNOWNLIST_UPDATE_INTERVAL = Long.parseLong(General.getProperty("KnownListUpdateInterval", "1250"));
			GRIDS_ALWAYS_ON = Boolean.parseBoolean(General.getProperty("GridsAlwaysOn", "False"));
			GRID_NEIGHBOR_TURNON_TIME = Integer.parseInt(General.getProperty("GridNeighborTurnOnTime", "1"));
			GRID_NEIGHBOR_TURNOFF_TIME = Integer.parseInt(General.getProperty("GridNeighborTurnOffTime", "90"));
			WORLD_X_MIN = Integer.parseInt(General.getProperty("WorldXMin", "10"));
			WORLD_X_MAX = Integer.parseInt(General.getProperty("WorldXMax", "26"));
			WORLD_Y_MIN = Integer.parseInt(General.getProperty("WorldYMin", "10"));
			WORLD_Y_MAX = Integer.parseInt(General.getProperty("WorldYMax", "26"));
			GEODATA = Integer.parseInt(General.getProperty("GeoData", "0"));
			try
			{
				GEODATA_DIR = new File(General.getProperty("GeodataDirectory", "data/geodata").replaceAll("\\\\", "/")).getCanonicalFile();
			}
			catch (IOException e)
			{
				_log.log(Level.WARNING, "Error setting geodata directory!", e);
				GEODATA_DIR = new File("data/geodata");
			}
			try
			{
				PATHNODE_DIR = new File(General.getProperty("PathnodeDirectory", "data/pathnode").replaceAll("\\\\", "/")).getCanonicalFile();
			}
			catch (IOException e)
			{
				_log.log(Level.WARNING, "Error setting pathnode directory!", e);
				PATHNODE_DIR = new File("data/pathnode");
			}
			GEODATA_CELLFINDING = Boolean.parseBoolean(General.getProperty("CellPathFinding", "False"));
			PATHFIND_BUFFERS = General.getProperty("PathFindBuffers", "100x6;128x6;192x6;256x4;320x4;384x4;500x2");
			LOW_WEIGHT = Float.parseFloat(General.getProperty("LowWeight", "0.5"));
			MEDIUM_WEIGHT = Float.parseFloat(General.getProperty("MediumWeight", "2"));
			HIGH_WEIGHT = Float.parseFloat(General.getProperty("HighWeight", "3"));
			ADVANCED_DIAGONAL_STRATEGY = Boolean.parseBoolean(General.getProperty("AdvancedDiagonalStrategy", "True"));
			DIAGONAL_WEIGHT = Float.parseFloat(General.getProperty("DiagonalWeight", "0.707"));
			MAX_POSTFILTER_PASSES = Integer.parseInt(General.getProperty("MaxPostfilterPasses", "3"));
			DEBUG_PATH = Boolean.parseBoolean(General.getProperty("DebugPath", "False"));
			FORCE_GEODATA = Boolean.parseBoolean(General.getProperty("ForceGeodata", "True"));
			COORD_SYNCHRONIZE = Integer.parseInt(General.getProperty("CoordSynchronize", "-1"));
			
			String str = General.getProperty("EnableFallingDamage", "auto");
			ENABLE_FALLING_DAMAGE = "auto".equalsIgnoreCase(str) ? GEODATA > 0 : Boolean.parseBoolean(str);
			
			PEACE_ZONE_MODE = Integer.parseInt(General.getProperty("PeaceZoneMode", "0"));
			DEFAULT_GLOBAL_CHAT = General.getProperty("GlobalChat", "ON");
			DEFAULT_TRADE_CHAT = General.getProperty("TradeChat", "ON");
			ALLOW_WAREHOUSE = Boolean.parseBoolean(General.getProperty("AllowWarehouse", "True"));
			WAREHOUSE_CACHE = Boolean.parseBoolean(General.getProperty("WarehouseCache", "False"));
			WAREHOUSE_CACHE_TIME = Integer.parseInt(General.getProperty("WarehouseCacheTime", "15"));
			ALLOW_REFUND = Boolean.parseBoolean(General.getProperty("AllowRefund", "True"));
			ALLOW_MAIL = Boolean.parseBoolean(General.getProperty("AllowMail", "True"));
			ALLOW_ATTACHMENTS = Boolean.parseBoolean(General.getProperty("AllowAttachments", "True"));
			ALLOW_WEAR = Boolean.parseBoolean(General.getProperty("AllowWear", "True"));
			WEAR_DELAY = Integer.parseInt(General.getProperty("WearDelay", "5"));
			WEAR_PRICE = Integer.parseInt(General.getProperty("WearPrice", "10"));
			ALLOW_LOTTERY = Boolean.parseBoolean(General.getProperty("AllowLottery", "True"));
			ALLOW_RACE = Boolean.parseBoolean(General.getProperty("AllowRace", "True"));
			ALLOW_WATER = Boolean.parseBoolean(General.getProperty("AllowWater", "True"));
			ALLOW_RENTPET = Boolean.parseBoolean(General.getProperty("AllowRentPet", "False"));
			ALLOWFISHING = Boolean.parseBoolean(General.getProperty("AllowFishing", "True"));
			ALLOW_MANOR = Boolean.parseBoolean(General.getProperty("AllowManor", "True"));
			ALLOW_BOAT = Boolean.parseBoolean(General.getProperty("AllowBoat", "True"));
			BOAT_BROADCAST_RADIUS = Integer.parseInt(General.getProperty("BoatBroadcastRadius", "20000"));
			ALLOW_CURSED_WEAPONS = Boolean.parseBoolean(General.getProperty("AllowCursedWeapons", "True"));
			ALLOW_NPC_WALKERS = Boolean.parseBoolean(General.getProperty("AllowNpcWalkers", "true"));
			ALLOW_PET_WALKERS = Boolean.parseBoolean(General.getProperty("AllowPetWalkers", "True"));
			SERVER_NEWS = Boolean.parseBoolean(General.getProperty("ShowServerNews", "False"));
			COMMUNITY_TYPE = Integer.parseInt(General.getProperty("CommunityType", "1"));
			BBS_SHOW_PLAYERLIST = Boolean.parseBoolean(General.getProperty("BBSShowPlayerList", "false"));
			BBS_DEFAULT = General.getProperty("BBSDefault", "_bbshome");
			SHOW_LEVEL_COMMUNITYBOARD = Boolean.parseBoolean(General.getProperty("ShowLevelOnCommunityBoard", "False"));
			SHOW_STATUS_COMMUNITYBOARD = Boolean.parseBoolean(General.getProperty("ShowStatusOnCommunityBoard", "False"));
			NAME_PAGE_SIZE_COMMUNITYBOARD = Integer.parseInt(General.getProperty("NamePageSizeOnCommunityBoard", "50"));
			NAME_PER_ROW_COMMUNITYBOARD = Integer.parseInt(General.getProperty("NamePerRowOnCommunityBoard", "5"));
			USE_SAY_FILTER = Boolean.parseBoolean(General.getProperty("UseChatFilter", "false"));
			CHAT_FILTER_CHARS = General.getProperty("ChatFilterChars", "^_^");
			String[] propertySplit4 = General.getProperty("BanChatChannels", "0;1;8;17").trim().split(";");
			BAN_CHAT_CHANNELS = new int[propertySplit4.length];
			try
			{
				int i = 0;
				for (String chatId : propertySplit4)
				{
					BAN_CHAT_CHANNELS[i++] = Integer.parseInt(chatId);
				}
			}
			catch (NumberFormatException nfe)
			{
				_log.log(Level.WARNING, nfe.getMessage(), nfe);
			}
			ALT_MANOR_REFRESH_TIME = Integer.parseInt(General.getProperty("AltManorRefreshTime", "20"));
			ALT_MANOR_REFRESH_MIN = Integer.parseInt(General.getProperty("AltManorRefreshMin", "00"));
			ALT_MANOR_APPROVE_TIME = Integer.parseInt(General.getProperty("AltManorApproveTime", "6"));
			ALT_MANOR_APPROVE_MIN = Integer.parseInt(General.getProperty("AltManorApproveMin", "00"));
			ALT_MANOR_MAINTENANCE_PERIOD = Integer.parseInt(General.getProperty("AltManorMaintenancePeriod", "360000"));
			ALT_MANOR_SAVE_ALL_ACTIONS = Boolean.parseBoolean(General.getProperty("AltManorSaveAllActions", "false"));
			ALT_MANOR_SAVE_PERIOD_RATE = Integer.parseInt(General.getProperty("AltManorSavePeriodRate", "2"));
			ALT_LOTTERY_PRIZE = Long.parseLong(General.getProperty("AltLotteryPrize", "50000"));
			ALT_LOTTERY_TICKET_PRICE = Long.parseLong(General.getProperty("AltLotteryTicketPrice", "2000"));
			ALT_LOTTERY_5_NUMBER_RATE = Float.parseFloat(General.getProperty("AltLottery5NumberRate", "0.6"));
			ALT_LOTTERY_4_NUMBER_RATE = Float.parseFloat(General.getProperty("AltLottery4NumberRate", "0.2"));
			ALT_LOTTERY_3_NUMBER_RATE = Float.parseFloat(General.getProperty("AltLottery3NumberRate", "0.2"));
			ALT_LOTTERY_2_AND_1_NUMBER_PRIZE = Long.parseLong(General.getProperty("AltLottery2and1NumberPrize", "200"));
			ALT_ITEM_AUCTION_ENABLED = Boolean.parseBoolean(General.getProperty("AltItemAuctionEnabled", "True"));
			ALT_ITEM_AUCTION_EXPIRED_AFTER = Integer.parseInt(General.getProperty("AltItemAuctionExpiredAfter", "14"));
			ALT_ITEM_AUCTION_TIME_EXTENDS_ON_BID = 1000 * (long) Integer.parseInt(General.getProperty("AltItemAuctionTimeExtendsOnBid", "0"));
			FS_TIME_ATTACK = Integer.parseInt(General.getProperty("TimeOfAttack", "50"));
			FS_TIME_COOLDOWN = Integer.parseInt(General.getProperty("TimeOfCoolDown", "5"));
			FS_TIME_ENTRY = Integer.parseInt(General.getProperty("TimeOfEntry", "3"));
			FS_TIME_WARMUP = Integer.parseInt(General.getProperty("TimeOfWarmUp", "2"));
			FS_PARTY_MEMBER_COUNT = Integer.parseInt(General.getProperty("NumberOfNecessaryPartyMembers", "4"));
			if (FS_TIME_ATTACK <= 0)
			{
				FS_TIME_ATTACK = 50;
			}
			if (FS_TIME_COOLDOWN <= 0)
			{
				FS_TIME_COOLDOWN = 5;
			}
			if (FS_TIME_ENTRY <= 0)
			{
				FS_TIME_ENTRY = 3;
			}
			if (FS_TIME_ENTRY <= 0)
			{
				FS_TIME_ENTRY = 3;
			}
			if (FS_TIME_ENTRY <= 0)
			{
				FS_TIME_ENTRY = 3;
			}
			RIFT_MIN_PARTY_SIZE = Integer.parseInt(General.getProperty("RiftMinPartySize", "5"));
			RIFT_MAX_JUMPS = Integer.parseInt(General.getProperty("MaxRiftJumps", "4"));
			RIFT_SPAWN_DELAY = Integer.parseInt(General.getProperty("RiftSpawnDelay", "10000"));
			RIFT_AUTO_JUMPS_TIME_MIN = Integer.parseInt(General.getProperty("AutoJumpsDelayMin", "480"));
			RIFT_AUTO_JUMPS_TIME_MAX = Integer.parseInt(General.getProperty("AutoJumpsDelayMax", "600"));
			RIFT_BOSS_ROOM_TIME_MUTIPLY = Float.parseFloat(General.getProperty("BossRoomTimeMultiply", "1.5"));
			RIFT_ENTER_COST_RECRUIT = Integer.parseInt(General.getProperty("RecruitCost", "18"));
			RIFT_ENTER_COST_SOLDIER = Integer.parseInt(General.getProperty("SoldierCost", "21"));
			RIFT_ENTER_COST_OFFICER = Integer.parseInt(General.getProperty("OfficerCost", "24"));
			RIFT_ENTER_COST_CAPTAIN = Integer.parseInt(General.getProperty("CaptainCost", "27"));
			RIFT_ENTER_COST_COMMANDER = Integer.parseInt(General.getProperty("CommanderCost", "30"));
			RIFT_ENTER_COST_HERO = Integer.parseInt(General.getProperty("HeroCost", "33"));
			DEFAULT_PUNISH = Integer.parseInt(General.getProperty("DefaultPunish", "2"));
			DEFAULT_PUNISH_PARAM = Integer.parseInt(General.getProperty("DefaultPunishParam", "0"));
			ONLY_GM_ITEMS_FREE = Boolean.parseBoolean(General.getProperty("OnlyGMItemsFree", "True"));
			JAIL_IS_PVP = Boolean.parseBoolean(General.getProperty("JailIsPvp", "False"));
			JAIL_DISABLE_CHAT = Boolean.parseBoolean(General.getProperty("JailDisableChat", "True"));
			JAIL_DISABLE_TRANSACTION = Boolean.parseBoolean(General.getProperty("JailDisableTransaction", "False"));
			CUSTOM_SPAWNLIST_TABLE = Boolean.parseBoolean(General.getProperty("CustomSpawnlistTable", "false"));
			SAVE_GMSPAWN_ON_CUSTOM = Boolean.parseBoolean(General.getProperty("SaveGmSpawnOnCustom", "false"));
			CUSTOM_NPC_TABLE = Boolean.parseBoolean(General.getProperty("CustomNpcTable", "false"));
			CUSTOM_NPC_SKILLS_TABLE = Boolean.parseBoolean(General.getProperty("CustomNpcSkillsTable", "false"));
			CUSTOM_TELEPORT_TABLE = Boolean.parseBoolean(General.getProperty("CustomTeleportTable", "false"));
			CUSTOM_DROPLIST_TABLE = Boolean.parseBoolean(General.getProperty("CustomDroplistTable", "false"));
			CUSTOM_MERCHANT_TABLES = Boolean.parseBoolean(General.getProperty("CustomMerchantTables", "false"));
			CUSTOM_NPCBUFFER_TABLES = Boolean.parseBoolean(General.getProperty("CustomNpcBufferTables", "false"));
			CUSTOM_SKILLS_LOAD = Boolean.parseBoolean(General.getProperty("CustomSkillsLoad", "false"));
			CUSTOM_ITEMS_LOAD = Boolean.parseBoolean(General.getProperty("CustomItemsLoad", "false"));
			CUSTOM_MULTISELL_LOAD = Boolean.parseBoolean(General.getProperty("CustomMultisellLoad", "false"));
			ALT_BIRTHDAY_GIFT = Integer.parseInt(General.getProperty("AltBirthdayGift", "22187"));
			ALT_BIRTHDAY_MAIL_SUBJECT = General.getProperty("AltBirthdayMailSubject", "Happy Birthday!");
			ALT_BIRTHDAY_MAIL_TEXT = General.getProperty("AltBirthdayMailText", "Hello Adventurer!! Seeing as you're one year older now, I thought I would send you some birthday cheer :) Please find your birthday pack attached. May these gifts bring you joy and happiness on this very special day." + EOL + EOL + "Sincerely, Alegria");
			ENABLE_BLOCK_CHECKER_EVENT = Boolean.parseBoolean(General.getProperty("EnableBlockCheckerEvent", "false"));
			MIN_BLOCK_CHECKER_TEAM_MEMBERS = Integer.parseInt(General.getProperty("BlockCheckerMinTeamMembers", "2"));
			if (MIN_BLOCK_CHECKER_TEAM_MEMBERS < 1)
			{
				MIN_BLOCK_CHECKER_TEAM_MEMBERS = 1;
			}
			else if (MIN_BLOCK_CHECKER_TEAM_MEMBERS > 6)
			{
				MIN_BLOCK_CHECKER_TEAM_MEMBERS = 6;
			}
			HBCE_FAIR_PLAY = Boolean.parseBoolean(General.getProperty("HBCEFairPlay", "false"));
			CLEAR_CREST_CACHE = Boolean.parseBoolean(General.getProperty("ClearClanCache", "false"));
			
			NORMAL_ENCHANT_COST_MULTIPLIER = Integer.parseInt(General.getProperty("NormalEnchantCostMultipiler", "1"));
			SAFE_ENCHANT_COST_MULTIPLIER = Integer.parseInt(General.getProperty("SafeEnchantCostMultipiler", "5"));
			
			// Load FloodProtector L2Properties file
			L2Properties FloodProtectors = new L2Properties();
			final File flood = new File(FLOOD_PROTECTOR_FILE);
			try (InputStream is = new FileInputStream(flood))
			{
				FloodProtectors.load(is);
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "Error while loading Flood Protector settings!", e);
			}
			loadFloodProtectorConfigs(FloodProtectors);
			
			// Load NPC L2Properties file (if exists)
			L2Properties NPC = new L2Properties();
			final File npc = new File(NPC_CONFIG_FILE);
			try (InputStream is = new FileInputStream(npc))
			{
				NPC.load(is);
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "Error while loading NPC settings!", e);
			}
			
			ANNOUNCE_MAMMON_SPAWN = Boolean.parseBoolean(NPC.getProperty("AnnounceMammonSpawn", "False"));
			ALT_MOB_AGRO_IN_PEACEZONE = Boolean.parseBoolean(NPC.getProperty("AltMobAgroInPeaceZone", "True"));
			ALT_ATTACKABLE_NPCS = Boolean.parseBoolean(NPC.getProperty("AltAttackableNpcs", "True"));
			ALT_GAME_VIEWNPC = Boolean.parseBoolean(NPC.getProperty("AltGameViewNpc", "False"));
			MAX_DRIFT_RANGE = Integer.parseInt(NPC.getProperty("MaxDriftRange", "300"));
			DEEPBLUE_DROP_RULES = Boolean.parseBoolean(NPC.getProperty("UseDeepBlueDropRules", "True"));
			DEEPBLUE_DROP_RULES_RAID = Boolean.parseBoolean(NPC.getProperty("UseDeepBlueDropRulesRaid", "True"));
			SHOW_NPC_LVL = Boolean.parseBoolean(NPC.getProperty("ShowNpcLevel", "False"));
			SHOW_CREST_WITHOUT_QUEST = Boolean.parseBoolean(NPC.getProperty("ShowCrestWithoutQuest", "False"));
			ENABLE_RANDOM_ENCHANT_EFFECT = Boolean.parseBoolean(NPC.getProperty("EnableRandomEnchantEffect", "False"));
			MIN_NPC_LVL_DMG_PENALTY = Integer.parseInt(NPC.getProperty("MinNPCLevelForDmgPenalty", "78"));
			NPC_DMG_PENALTY = parseConfigLine(NPC.getProperty("DmgPenaltyForLvLDifferences", "0.7, 0.6, 0.6, 0.55"));
			NPC_CRIT_DMG_PENALTY = parseConfigLine(NPC.getProperty("CritDmgPenaltyForLvLDifferences", "0.75, 0.65, 0.6, 0.58"));
			NPC_SKILL_DMG_PENALTY = parseConfigLine(NPC.getProperty("SkillDmgPenaltyForLvLDifferences", "0.8, 0.7, 0.65, 0.62"));
			MIN_NPC_LVL_MAGIC_PENALTY = Integer.parseInt(NPC.getProperty("MinNPCLevelForMagicPenalty", "78"));
			NPC_SKILL_CHANCE_PENALTY = parseConfigLine(NPC.getProperty("SkillChancePenaltyForLvLDifferences", "2.5, 3.0, 3.25, 3.5"));
			DECAY_TIME_TASK = Integer.parseInt(NPC.getProperty("DecayTimeTask", "5000"));
			NPC_DECAY_TIME = Integer.parseInt(NPC.getProperty("NpcDecayTime", "8500"));
			RAID_BOSS_DECAY_TIME = Integer.parseInt(NPC.getProperty("RaidBossDecayTime", "30000"));
			SPOILED_DECAY_TIME = Integer.parseInt(NPC.getProperty("SpoiledDecayTime", "18500"));
			ENABLE_DROP_VITALITY_HERBS = Boolean.parseBoolean(NPC.getProperty("EnableVitalityHerbs", "True"));
			GUARD_ATTACK_AGGRO_MOB = Boolean.parseBoolean(NPC.getProperty("GuardAttackAggroMob", "False"));
			ALLOW_WYVERN_UPGRADER = Boolean.parseBoolean(NPC.getProperty("AllowWyvernUpgrader", "False"));
			String[] listPetRentNpc = NPC.getProperty("ListPetRentNpc", "30827").split(",");
			LIST_PET_RENT_NPC = new ArrayList<>(listPetRentNpc.length);
			for (String id : listPetRentNpc)
			{
				LIST_PET_RENT_NPC.add(Integer.valueOf(id));
			}
			RAID_HP_REGEN_MULTIPLIER = Double.parseDouble(NPC.getProperty("RaidHpRegenMultiplier", "100")) / 100;
			RAID_MP_REGEN_MULTIPLIER = Double.parseDouble(NPC.getProperty("RaidMpRegenMultiplier", "100")) / 100;
			RAID_PDEFENCE_MULTIPLIER = Double.parseDouble(NPC.getProperty("RaidPDefenceMultiplier", "100")) / 100;
			RAID_MDEFENCE_MULTIPLIER = Double.parseDouble(NPC.getProperty("RaidMDefenceMultiplier", "100")) / 100;
			RAID_PATTACK_MULTIPLIER = Double.parseDouble(NPC.getProperty("RaidPAttackMultiplier", "100")) / 100;
			RAID_MATTACK_MULTIPLIER = Double.parseDouble(NPC.getProperty("RaidMAttackMultiplier", "100")) / 100;
			RAID_MIN_RESPAWN_MULTIPLIER = Float.parseFloat(NPC.getProperty("RaidMinRespawnMultiplier", "1.0"));
			RAID_MAX_RESPAWN_MULTIPLIER = Float.parseFloat(NPC.getProperty("RaidMaxRespawnMultiplier", "1.0"));
			RAID_MINION_RESPAWN_TIMER = Integer.parseInt(NPC.getProperty("RaidMinionRespawnTime", "300000"));
			final String[] propertySplit = NPC.getProperty("CustomMinionsRespawnTime", "").split(";");
			MINIONS_RESPAWN_TIME = new HashMap<>(propertySplit.length);
			for (String prop : propertySplit)
			{
				String[] propSplit = prop.split(",");
				if (propSplit.length != 2)
				{
					_log.warning(StringUtil.concat("[CustomMinionsRespawnTime]: invalid config property -> CustomMinionsRespawnTime \"", prop, "\""));
				}
				
				try
				{
					MINIONS_RESPAWN_TIME.put(Integer.valueOf(propSplit[0]), Integer.valueOf(propSplit[1]));
				}
				catch (NumberFormatException nfe)
				{
					if (!prop.isEmpty())
					{
						_log.warning(StringUtil.concat("[CustomMinionsRespawnTime]: invalid config property -> CustomMinionsRespawnTime \"", propSplit[0], "\"", propSplit[1]));
					}
				}
			}
			
			RAID_DISABLE_CURSE = Boolean.parseBoolean(NPC.getProperty("DisableRaidCurse", "False"));
			RAID_CHAOS_TIME = Integer.parseInt(NPC.getProperty("RaidChaosTime", "10"));
			GRAND_CHAOS_TIME = Integer.parseInt(NPC.getProperty("GrandChaosTime", "10"));
			MINION_CHAOS_TIME = Integer.parseInt(NPC.getProperty("MinionChaosTime", "10"));
			INVENTORY_MAXIMUM_PET = Integer.parseInt(NPC.getProperty("MaximumSlotsForPet", "12"));
			PET_HP_REGEN_MULTIPLIER = Double.parseDouble(NPC.getProperty("PetHpRegenMultiplier", "100")) / 100;
			PET_MP_REGEN_MULTIPLIER = Double.parseDouble(NPC.getProperty("PetMpRegenMultiplier", "100")) / 100;
			split = NPC.getProperty("NonTalkingNpcs", "18684,18685,18686,18687,18688,18689,18690,19691,18692,31557,31606,31671,31672,31673,31674,32026,32030,32031,32032,32306,32619,32620,32621").split(",");
			NON_TALKING_NPCS = new ArrayList<>(split.length);
			for (String npcId : split)
			{
				try
				{
					NON_TALKING_NPCS.add(Integer.parseInt(npcId));
				}
				catch (NumberFormatException nfe)
				{
					if (!npcId.isEmpty())
					{
						_log.warning("Could not parse " + npcId + " id for NonTalkingNpcs. Please check that all values are digits and coma separated.");
					}
				}
			}
			
			// Load Rates L2Properties file (if exists)
			final File rates = new File(RATES_CONFIG_FILE);
			L2Properties RatesSettings = new L2Properties();
			try (InputStream is = new FileInputStream(rates))
			{
				RatesSettings.load(is);
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "Error while loading Rates settings!", e);
			}
			
			RATE_XP = Float.parseFloat(RatesSettings.getProperty("RateXp", "1."));
			RATE_SP = Float.parseFloat(RatesSettings.getProperty("RateSp", "1."));
			RATE_PARTY_XP = Float.parseFloat(RatesSettings.getProperty("RatePartyXp", "1."));
			RATE_PARTY_SP = Float.parseFloat(RatesSettings.getProperty("RatePartySp", "1."));
			RATE_CONSUMABLE_COST = Float.parseFloat(RatesSettings.getProperty("RateConsumableCost", "1."));
			RATE_EXTRACTABLE = Float.parseFloat(RatesSettings.getProperty("RateExtractable", "1."));
			RATE_DROP_ITEMS = Float.parseFloat(RatesSettings.getProperty("RateDropItems", "1."));
			RATE_DROP_ITEMS_BY_RAID = Float.parseFloat(RatesSettings.getProperty("RateRaidDropItems", "1."));
			RATE_DROP_SPOIL = Float.parseFloat(RatesSettings.getProperty("RateDropSpoil", "1."));
			RATE_DROP_MANOR = Integer.parseInt(RatesSettings.getProperty("RateDropManor", "1"));
			RATE_QUEST_DROP = Float.parseFloat(RatesSettings.getProperty("RateQuestDrop", "1."));
			RATE_QUEST_REWARD = Float.parseFloat(RatesSettings.getProperty("RateQuestReward", "1."));
			RATE_QUEST_REWARD_XP = Float.parseFloat(RatesSettings.getProperty("RateQuestRewardXP", "1."));
			RATE_QUEST_REWARD_SP = Float.parseFloat(RatesSettings.getProperty("RateQuestRewardSP", "1."));
			RATE_QUEST_REWARD_ADENA = Float.parseFloat(RatesSettings.getProperty("RateQuestRewardAdena", "1."));
			RATE_QUEST_REWARD_USE_MULTIPLIERS = Boolean.parseBoolean(RatesSettings.getProperty("UseQuestRewardMultipliers", "False"));
			RATE_QUEST_REWARD_POTION = Float.parseFloat(RatesSettings.getProperty("RateQuestRewardPotion", "1."));
			RATE_QUEST_REWARD_SCROLL = Float.parseFloat(RatesSettings.getProperty("RateQuestRewardScroll", "1."));
			RATE_QUEST_REWARD_RECIPE = Float.parseFloat(RatesSettings.getProperty("RateQuestRewardRecipe", "1."));
			RATE_QUEST_REWARD_MATERIAL = Float.parseFloat(RatesSettings.getProperty("RateQuestRewardMaterial", "1."));
			RATE_HB_TRUST_INCREASE = Float.parseFloat(RatesSettings.getProperty("RateHellboundTrustIncrease", "1."));
			RATE_HB_TRUST_DECREASE = Float.parseFloat(RatesSettings.getProperty("RateHellboundTrustDecrease", "1."));
			
			RATE_VITALITY_LEVEL_1 = Float.parseFloat(RatesSettings.getProperty("RateVitalityLevel1", "1.5"));
			RATE_VITALITY_LEVEL_2 = Float.parseFloat(RatesSettings.getProperty("RateVitalityLevel2", "2."));
			RATE_VITALITY_LEVEL_3 = Float.parseFloat(RatesSettings.getProperty("RateVitalityLevel3", "2.5"));
			RATE_VITALITY_LEVEL_4 = Float.parseFloat(RatesSettings.getProperty("RateVitalityLevel4", "3."));
			RATE_RECOVERY_VITALITY_PEACE_ZONE = Float.parseFloat(RatesSettings.getProperty("RateRecoveryPeaceZone", "1."));
			RATE_VITALITY_LOST = Float.parseFloat(RatesSettings.getProperty("RateVitalityLost", "1."));
			RATE_VITALITY_GAIN = Float.parseFloat(RatesSettings.getProperty("RateVitalityGain", "1."));
			RATE_RECOVERY_ON_RECONNECT = Float.parseFloat(RatesSettings.getProperty("RateRecoveryOnReconnect", "4."));
			RATE_KARMA_EXP_LOST = Float.parseFloat(RatesSettings.getProperty("RateKarmaExpLost", "1."));
			RATE_SIEGE_GUARDS_PRICE = Float.parseFloat(RatesSettings.getProperty("RateSiegeGuardsPrice", "1."));
			RATE_DROP_COMMON_HERBS = Float.parseFloat(RatesSettings.getProperty("RateCommonHerbs", "1."));
			RATE_DROP_HP_HERBS = Float.parseFloat(RatesSettings.getProperty("RateHpHerbs", "1."));
			RATE_DROP_MP_HERBS = Float.parseFloat(RatesSettings.getProperty("RateMpHerbs", "1."));
			RATE_DROP_SPECIAL_HERBS = Float.parseFloat(RatesSettings.getProperty("RateSpecialHerbs", "1."));
			RATE_DROP_VITALITY_HERBS = Float.parseFloat(RatesSettings.getProperty("RateVitalityHerbs", "1."));
			PLAYER_DROP_LIMIT = Integer.parseInt(RatesSettings.getProperty("PlayerDropLimit", "3"));
			PLAYER_RATE_DROP = Integer.parseInt(RatesSettings.getProperty("PlayerRateDrop", "5"));
			PLAYER_RATE_DROP_ITEM = Integer.parseInt(RatesSettings.getProperty("PlayerRateDropItem", "70"));
			PLAYER_RATE_DROP_EQUIP = Integer.parseInt(RatesSettings.getProperty("PlayerRateDropEquip", "25"));
			PLAYER_RATE_DROP_EQUIP_WEAPON = Integer.parseInt(RatesSettings.getProperty("PlayerRateDropEquipWeapon", "5"));
			PET_XP_RATE = Float.parseFloat(RatesSettings.getProperty("PetXpRate", "1."));
			PET_FOOD_RATE = Integer.parseInt(RatesSettings.getProperty("PetFoodRate", "1"));
			SINEATER_XP_RATE = Float.parseFloat(RatesSettings.getProperty("SinEaterXpRate", "1."));
			KARMA_DROP_LIMIT = Integer.parseInt(RatesSettings.getProperty("KarmaDropLimit", "10"));
			KARMA_RATE_DROP = Integer.parseInt(RatesSettings.getProperty("KarmaRateDrop", "70"));
			KARMA_RATE_DROP_ITEM = Integer.parseInt(RatesSettings.getProperty("KarmaRateDropItem", "50"));
			KARMA_RATE_DROP_EQUIP = Integer.parseInt(RatesSettings.getProperty("KarmaRateDropEquip", "40"));
			KARMA_RATE_DROP_EQUIP_WEAPON = Integer.parseInt(RatesSettings.getProperty("KarmaRateDropEquipWeapon", "10"));
			
			// Initializing table
			PLAYER_XP_PERCENT_LOST = new double[Byte.MAX_VALUE + 1];
			
			// Default value
			for (int i = 0; i <= Byte.MAX_VALUE; i++)
			{
				PLAYER_XP_PERCENT_LOST[i] = 1.;
			}
			
			// Now loading into table parsed values
			try
			{
				String[] values = RatesSettings.getProperty("PlayerXPPercentLost", "0,39-7.0;40,75-4.0;76,76-2.5;77,77-2.0;78,78-1.5").split(";");
				
				for (String s : values)
				{
					int min;
					int max;
					double val;
					
					String[] vals = s.split("-");
					String[] mM = vals[0].split(",");
					
					min = Integer.parseInt(mM[0]);
					max = Integer.parseInt(mM[1]);
					val = Double.parseDouble(vals[1]);
					
					for (int i = min; i <= max; i++)
					{
						PLAYER_XP_PERCENT_LOST[i] = val;
					}
				}
			}
			catch (Exception e)
			{
				_log.log(Level.WARNING, "Error while loading Player XP percent lost!", e);
			}
			
			String[] rateDropItemsById = RatesSettings.getProperty("RateDropItemsById", "").split(";");
			RATE_DROP_ITEMS_ID = new HashMap<>(rateDropItemsById.length);
			if (!rateDropItemsById[0].isEmpty())
			{
				for (String item : rateDropItemsById)
				{
					String[] itemSplit = item.split(",");
					if (itemSplit.length != 2)
					{
						_log.warning(StringUtil.concat("Config.load(): invalid config property -> RateDropItemsById \"", item, "\""));
					}
					else
					{
						try
						{
							RATE_DROP_ITEMS_ID.put(Integer.valueOf(itemSplit[0]), Float.valueOf(itemSplit[1]));
						}
						catch (NumberFormatException nfe)
						{
							if (!item.isEmpty())
							{
								_log.warning(StringUtil.concat("Config.load(): invalid config property -> RateDropItemsById \"", item, "\""));
							}
						}
					}
				}
			}
			if (!RATE_DROP_ITEMS_ID.containsKey(PcInventory.ADENA_ID))
			{
				RATE_DROP_ITEMS_ID.put(PcInventory.ADENA_ID, RATE_DROP_ITEMS); // for Adena rate if not defined
			}
			
			// Load L2JMod L2Properties file (if exists)
			L2Properties L2JModSettings = new L2Properties();
			final File l2jmods = new File(L2JMOD_CONFIG_FILE);
			try (InputStream is = new FileInputStream(l2jmods))
			{
				L2JModSettings.load(is);
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "Error while loading L2JMod settings!", e);
			}
			
			L2JMOD_CHAMPION_ENABLE = Boolean.parseBoolean(L2JModSettings.getProperty("ChampionEnable", "false"));
			L2JMOD_CHAMPION_PASSIVE = Boolean.parseBoolean(L2JModSettings.getProperty("ChampionPassive", "false"));
			L2JMOD_CHAMPION_FREQUENCY = Integer.parseInt(L2JModSettings.getProperty("ChampionFrequency", "0"));
			L2JMOD_CHAMP_TITLE = L2JModSettings.getProperty("ChampionTitle", "Champion");
			L2JMOD_CHAMP_MIN_LVL = Integer.parseInt(L2JModSettings.getProperty("ChampionMinLevel", "20"));
			L2JMOD_CHAMP_MAX_LVL = Integer.parseInt(L2JModSettings.getProperty("ChampionMaxLevel", "60"));
			L2JMOD_CHAMPION_HP = Integer.parseInt(L2JModSettings.getProperty("ChampionHp", "7"));
			L2JMOD_CHAMPION_HP_REGEN = Float.parseFloat(L2JModSettings.getProperty("ChampionHpRegen", "1."));
			L2JMOD_CHAMPION_REWARDS = Integer.parseInt(L2JModSettings.getProperty("ChampionRewards", "8"));
			L2JMOD_CHAMPION_ADENAS_REWARDS = Float.parseFloat(L2JModSettings.getProperty("ChampionAdenasRewards", "1"));
			L2JMOD_CHAMPION_ATK = Float.parseFloat(L2JModSettings.getProperty("ChampionAtk", "1."));
			L2JMOD_CHAMPION_SPD_ATK = Float.parseFloat(L2JModSettings.getProperty("ChampionSpdAtk", "1."));
			L2JMOD_CHAMPION_REWARD_LOWER_LVL_ITEM_CHANCE = Integer.parseInt(L2JModSettings.getProperty("ChampionRewardLowerLvlItemChance", "0"));
			L2JMOD_CHAMPION_REWARD_HIGHER_LVL_ITEM_CHANCE = Integer.parseInt(L2JModSettings.getProperty("ChampionRewardHigherLvlItemChance", "0"));
			L2JMOD_CHAMPION_REWARD_ID = Integer.parseInt(L2JModSettings.getProperty("ChampionRewardItemID", "6393"));
			L2JMOD_CHAMPION_REWARD_QTY = Integer.parseInt(L2JModSettings.getProperty("ChampionRewardItemQty", "1"));
			L2JMOD_CHAMPION_ENABLE_VITALITY = Boolean.parseBoolean(L2JModSettings.getProperty("ChampionEnableVitality", "False"));
			L2JMOD_CHAMPION_ENABLE_IN_INSTANCES = Boolean.parseBoolean(L2JModSettings.getProperty("ChampionEnableInInstances", "False"));
			
			TVT_EVENT_ENABLED = Boolean.parseBoolean(L2JModSettings.getProperty("TvTEventEnabled", "false"));
			TVT_EVENT_IN_INSTANCE = Boolean.parseBoolean(L2JModSettings.getProperty("TvTEventInInstance", "false"));
			TVT_EVENT_INSTANCE_FILE = L2JModSettings.getProperty("TvTEventInstanceFile", "coliseum.xml");
			TVT_EVENT_INTERVAL = L2JModSettings.getProperty("TvTEventInterval", "20:00").split(",");
			TVT_EVENT_PARTICIPATION_TIME = Integer.parseInt(L2JModSettings.getProperty("TvTEventParticipationTime", "3600"));
			TVT_EVENT_RUNNING_TIME = Integer.parseInt(L2JModSettings.getProperty("TvTEventRunningTime", "1800"));
			TVT_EVENT_PARTICIPATION_NPC_ID = Integer.parseInt(L2JModSettings.getProperty("TvTEventParticipationNpcId", "0"));
			
			L2JMOD_ALLOW_WEDDING = Boolean.parseBoolean(L2JModSettings.getProperty("AllowWedding", "False"));
			L2JMOD_WEDDING_PRICE = Integer.parseInt(L2JModSettings.getProperty("WeddingPrice", "250000000"));
			L2JMOD_WEDDING_PUNISH_INFIDELITY = Boolean.parseBoolean(L2JModSettings.getProperty("WeddingPunishInfidelity", "True"));
			L2JMOD_WEDDING_TELEPORT = Boolean.parseBoolean(L2JModSettings.getProperty("WeddingTeleport", "True"));
			L2JMOD_WEDDING_TELEPORT_PRICE = Integer.parseInt(L2JModSettings.getProperty("WeddingTeleportPrice", "50000"));
			L2JMOD_WEDDING_TELEPORT_DURATION = Integer.parseInt(L2JModSettings.getProperty("WeddingTeleportDuration", "60"));
			L2JMOD_WEDDING_SAMESEX = Boolean.parseBoolean(L2JModSettings.getProperty("WeddingAllowSameSex", "False"));
			L2JMOD_WEDDING_FORMALWEAR = Boolean.parseBoolean(L2JModSettings.getProperty("WeddingFormalWear", "True"));
			L2JMOD_WEDDING_DIVORCE_COSTS = Integer.parseInt(L2JModSettings.getProperty("WeddingDivorceCosts", "20"));
			
			L2JMOD_ENABLE_WAREHOUSESORTING_CLAN = Boolean.parseBoolean(L2JModSettings.getProperty("EnableWarehouseSortingClan", "False"));
			L2JMOD_ENABLE_WAREHOUSESORTING_PRIVATE = Boolean.parseBoolean(L2JModSettings.getProperty("EnableWarehouseSortingPrivate", "False"));
			
			if (TVT_EVENT_PARTICIPATION_NPC_ID == 0)
			{
				TVT_EVENT_ENABLED = false;
				_log.warning("TvTEventEngine[Config.load()]: invalid config property -> TvTEventParticipationNpcId");
			}
			else
			{
				String[] tvtNpcCoords = L2JModSettings.getProperty("TvTEventParticipationNpcCoordinates", "0,0,0").split(",");
				if (tvtNpcCoords.length < 3)
				{
					TVT_EVENT_ENABLED = false;
					_log.warning("TvTEventEngine[Config.load()]: invalid config property -> TvTEventParticipationNpcCoordinates");
				}
				else
				{
					TVT_EVENT_REWARDS = new ArrayList<>();
					TVT_DOORS_IDS_TO_OPEN = new ArrayList<>();
					TVT_DOORS_IDS_TO_CLOSE = new ArrayList<>();
					TVT_EVENT_PARTICIPATION_NPC_COORDINATES = new int[4];
					TVT_EVENT_TEAM_1_COORDINATES = new int[3];
					TVT_EVENT_TEAM_2_COORDINATES = new int[3];
					TVT_EVENT_PARTICIPATION_NPC_COORDINATES[0] = Integer.parseInt(tvtNpcCoords[0]);
					TVT_EVENT_PARTICIPATION_NPC_COORDINATES[1] = Integer.parseInt(tvtNpcCoords[1]);
					TVT_EVENT_PARTICIPATION_NPC_COORDINATES[2] = Integer.parseInt(tvtNpcCoords[2]);
					if (tvtNpcCoords.length == 4)
					{
						TVT_EVENT_PARTICIPATION_NPC_COORDINATES[3] = Integer.parseInt(tvtNpcCoords[3]);
					}
					TVT_EVENT_MIN_PLAYERS_IN_TEAMS = Integer.parseInt(L2JModSettings.getProperty("TvTEventMinPlayersInTeams", "1"));
					TVT_EVENT_MAX_PLAYERS_IN_TEAMS = Integer.parseInt(L2JModSettings.getProperty("TvTEventMaxPlayersInTeams", "20"));
					TVT_EVENT_MIN_LVL = Byte.parseByte(L2JModSettings.getProperty("TvTEventMinPlayerLevel", "1"));
					TVT_EVENT_MAX_LVL = Byte.parseByte(L2JModSettings.getProperty("TvTEventMaxPlayerLevel", "80"));
					TVT_EVENT_RESPAWN_TELEPORT_DELAY = Integer.parseInt(L2JModSettings.getProperty("TvTEventRespawnTeleportDelay", "20"));
					TVT_EVENT_START_LEAVE_TELEPORT_DELAY = Integer.parseInt(L2JModSettings.getProperty("TvTEventStartLeaveTeleportDelay", "20"));
					TVT_EVENT_EFFECTS_REMOVAL = Integer.parseInt(L2JModSettings.getProperty("TvTEventEffectsRemoval", "0"));
					TVT_EVENT_MAX_PARTICIPANTS_PER_IP = Integer.parseInt(L2JModSettings.getProperty("TvTEventMaxParticipantsPerIP", "0"));
					TVT_ALLOW_VOICED_COMMAND = Boolean.parseBoolean(L2JModSettings.getProperty("TvTAllowVoicedInfoCommand", "false"));
					TVT_EVENT_TEAM_1_NAME = L2JModSettings.getProperty("TvTEventTeam1Name", "Team1");
					tvtNpcCoords = L2JModSettings.getProperty("TvTEventTeam1Coordinates", "0,0,0").split(",");
					if (tvtNpcCoords.length < 3)
					{
						TVT_EVENT_ENABLED = false;
						_log.warning("TvTEventEngine[Config.load()]: invalid config property -> TvTEventTeam1Coordinates");
					}
					else
					{
						TVT_EVENT_TEAM_1_COORDINATES[0] = Integer.parseInt(tvtNpcCoords[0]);
						TVT_EVENT_TEAM_1_COORDINATES[1] = Integer.parseInt(tvtNpcCoords[1]);
						TVT_EVENT_TEAM_1_COORDINATES[2] = Integer.parseInt(tvtNpcCoords[2]);
						TVT_EVENT_TEAM_2_NAME = L2JModSettings.getProperty("TvTEventTeam2Name", "Team2");
						tvtNpcCoords = L2JModSettings.getProperty("TvTEventTeam2Coordinates", "0,0,0").split(",");
						if (tvtNpcCoords.length < 3)
						{
							TVT_EVENT_ENABLED = false;
							_log.warning("TvTEventEngine[Config.load()]: invalid config property -> TvTEventTeam2Coordinates");
						}
						else
						{
							TVT_EVENT_TEAM_2_COORDINATES[0] = Integer.parseInt(tvtNpcCoords[0]);
							TVT_EVENT_TEAM_2_COORDINATES[1] = Integer.parseInt(tvtNpcCoords[1]);
							TVT_EVENT_TEAM_2_COORDINATES[2] = Integer.parseInt(tvtNpcCoords[2]);
							tvtNpcCoords = L2JModSettings.getProperty("TvTEventParticipationFee", "0,0").split(",");
							try
							{
								TVT_EVENT_PARTICIPATION_FEE[0] = Integer.parseInt(tvtNpcCoords[0]);
								TVT_EVENT_PARTICIPATION_FEE[1] = Integer.parseInt(tvtNpcCoords[1]);
							}
							catch (NumberFormatException nfe)
							{
								if (tvtNpcCoords.length > 0)
								{
									_log.warning("TvTEventEngine[Config.load()]: invalid config property -> TvTEventParticipationFee");
								}
							}
							tvtNpcCoords = L2JModSettings.getProperty("TvTEventReward", "57,100000").split(";");
							for (String reward : tvtNpcCoords)
							{
								String[] rewardSplit = reward.split(",");
								if (rewardSplit.length != 2)
								{
									_log.warning(StringUtil.concat("TvTEventEngine[Config.load()]: invalid config property -> TvTEventReward \"", reward, "\""));
								}
								else
								{
									try
									{
										TVT_EVENT_REWARDS.add(new int[]
										{
											Integer.parseInt(rewardSplit[0]),
											Integer.parseInt(rewardSplit[1])
										});
									}
									catch (NumberFormatException nfe)
									{
										if (!reward.isEmpty())
										{
											_log.warning(StringUtil.concat("TvTEventEngine[Config.load()]: invalid config property -> TvTEventReward \"", reward, "\""));
										}
									}
								}
							}
							
							TVT_EVENT_TARGET_TEAM_MEMBERS_ALLOWED = Boolean.parseBoolean(L2JModSettings.getProperty("TvTEventTargetTeamMembersAllowed", "true"));
							TVT_EVENT_SCROLL_ALLOWED = Boolean.parseBoolean(L2JModSettings.getProperty("TvTEventScrollsAllowed", "false"));
							TVT_EVENT_POTIONS_ALLOWED = Boolean.parseBoolean(L2JModSettings.getProperty("TvTEventPotionsAllowed", "false"));
							TVT_EVENT_SUMMON_BY_ITEM_ALLOWED = Boolean.parseBoolean(L2JModSettings.getProperty("TvTEventSummonByItemAllowed", "false"));
							TVT_REWARD_TEAM_TIE = Boolean.parseBoolean(L2JModSettings.getProperty("TvTRewardTeamTie", "false"));
							tvtNpcCoords = L2JModSettings.getProperty("TvTDoorsToOpen", "").split(";");
							for (String door : tvtNpcCoords)
							{
								try
								{
									TVT_DOORS_IDS_TO_OPEN.add(Integer.parseInt(door));
								}
								catch (NumberFormatException nfe)
								{
									if (!door.isEmpty())
									{
										_log.warning(StringUtil.concat("TvTEventEngine[Config.load()]: invalid config property -> TvTDoorsToOpen \"", door, "\""));
									}
								}
							}
							
							tvtNpcCoords = L2JModSettings.getProperty("TvTDoorsToClose", "").split(";");
							for (String door : tvtNpcCoords)
							{
								try
								{
									TVT_DOORS_IDS_TO_CLOSE.add(Integer.parseInt(door));
								}
								catch (NumberFormatException nfe)
								{
									if (!door.isEmpty())
									{
										_log.warning(StringUtil.concat("TvTEventEngine[Config.load()]: invalid config property -> TvTDoorsToClose \"", door, "\""));
									}
								}
							}
							
							tvtNpcCoords = L2JModSettings.getProperty("TvTEventFighterBuffs", "").split(";");
							if (!tvtNpcCoords[0].isEmpty())
							{
								TVT_EVENT_FIGHTER_BUFFS = new HashMap<>(tvtNpcCoords.length);
								for (String skill : tvtNpcCoords)
								{
									String[] skillSplit = skill.split(",");
									if (skillSplit.length != 2)
									{
										_log.warning(StringUtil.concat("TvTEventEngine[Config.load()]: invalid config property -> TvTEventFighterBuffs \"", skill, "\""));
									}
									else
									{
										try
										{
											TVT_EVENT_FIGHTER_BUFFS.put(Integer.parseInt(skillSplit[0]), Integer.parseInt(skillSplit[1]));
										}
										catch (NumberFormatException nfe)
										{
											if (!skill.isEmpty())
											{
												_log.warning(StringUtil.concat("TvTEventEngine[Config.load()]: invalid config property -> TvTEventFighterBuffs \"", skill, "\""));
											}
										}
									}
								}
							}
							
							tvtNpcCoords = L2JModSettings.getProperty("TvTEventMageBuffs", "").split(";");
							if (!tvtNpcCoords[0].isEmpty())
							{
								TVT_EVENT_MAGE_BUFFS = new HashMap<>(tvtNpcCoords.length);
								for (String skill : tvtNpcCoords)
								{
									String[] skillSplit = skill.split(",");
									if (skillSplit.length != 2)
									{
										_log.warning(StringUtil.concat("TvTEventEngine[Config.load()]: invalid config property -> TvTEventMageBuffs \"", skill, "\""));
									}
									else
									{
										try
										{
											TVT_EVENT_MAGE_BUFFS.put(Integer.parseInt(skillSplit[0]), Integer.parseInt(skillSplit[1]));
										}
										catch (NumberFormatException nfe)
										{
											if (!skill.isEmpty())
											{
												_log.warning(StringUtil.concat("TvTEventEngine[Config.load()]: invalid config property -> TvTEventMageBuffs \"", skill, "\""));
											}
										}
									}
								}
							}
						}
					}
				}
			}
			
			BANKING_SYSTEM_ENABLED = Boolean.parseBoolean(L2JModSettings.getProperty("BankingEnabled", "false"));
			BANKING_SYSTEM_GOLDBARS = Integer.parseInt(L2JModSettings.getProperty("BankingGoldbarCount", "1"));
			BANKING_SYSTEM_ADENA = Integer.parseInt(L2JModSettings.getProperty("BankingAdenaCount", "500000000"));
			
			OFFLINE_TRADE_ENABLE = Boolean.parseBoolean(L2JModSettings.getProperty("OfflineTradeEnable", "false"));
			OFFLINE_CRAFT_ENABLE = Boolean.parseBoolean(L2JModSettings.getProperty("OfflineCraftEnable", "false"));
			OFFLINE_MODE_IN_PEACE_ZONE = Boolean.parseBoolean(L2JModSettings.getProperty("OfflineModeInPaceZone", "False"));
			OFFLINE_MODE_NO_DAMAGE = Boolean.parseBoolean(L2JModSettings.getProperty("OfflineModeNoDamage", "False"));
			OFFLINE_SET_NAME_COLOR = Boolean.parseBoolean(L2JModSettings.getProperty("OfflineSetNameColor", "false"));
			OFFLINE_NAME_COLOR = Integer.decode("0x" + L2JModSettings.getProperty("OfflineNameColor", "808080"));
			OFFLINE_FAME = Boolean.parseBoolean(L2JModSettings.getProperty("OfflineFame", "true"));
			RESTORE_OFFLINERS = Boolean.parseBoolean(L2JModSettings.getProperty("RestoreOffliners", "false"));
			OFFLINE_MAX_DAYS = Integer.parseInt(L2JModSettings.getProperty("OfflineMaxDays", "10"));
			OFFLINE_DISCONNECT_FINISHED = Boolean.parseBoolean(L2JModSettings.getProperty("OfflineDisconnectFinished", "true"));
			
			L2JMOD_ENABLE_MANA_POTIONS_SUPPORT = Boolean.parseBoolean(L2JModSettings.getProperty("EnableManaPotionSupport", "false"));
			
			L2JMOD_DISPLAY_SERVER_TIME = Boolean.parseBoolean(L2JModSettings.getProperty("DisplayServerTime", "false"));
			
			WELCOME_MESSAGE_ENABLED = Boolean.parseBoolean(L2JModSettings.getProperty("ScreenWelcomeMessageEnable", "false"));
			WELCOME_MESSAGE_TEXT = L2JModSettings.getProperty("ScreenWelcomeMessageText", "Welcome to L2J server!");
			WELCOME_MESSAGE_TIME = Integer.parseInt(L2JModSettings.getProperty("ScreenWelcomeMessageTime", "10")) * 1000;
			
			L2JMOD_ANTIFEED_ENABLE = Boolean.parseBoolean(L2JModSettings.getProperty("AntiFeedEnable", "false"));
			L2JMOD_ANTIFEED_DUALBOX = Boolean.parseBoolean(L2JModSettings.getProperty("AntiFeedDualbox", "true"));
			L2JMOD_ANTIFEED_DISCONNECTED_AS_DUALBOX = Boolean.parseBoolean(L2JModSettings.getProperty("AntiFeedDisconnectedAsDualbox", "true"));
			L2JMOD_ANTIFEED_INTERVAL = 1000 * Integer.parseInt(L2JModSettings.getProperty("AntiFeedInterval", "120"));
			ANNOUNCE_PK_PVP = Boolean.parseBoolean(L2JModSettings.getProperty("AnnouncePkPvP", "False"));
			ANNOUNCE_PK_PVP_NORMAL_MESSAGE = Boolean.parseBoolean(L2JModSettings.getProperty("AnnouncePkPvPNormalMessage", "True"));
			ANNOUNCE_PK_MSG = L2JModSettings.getProperty("AnnouncePkMsg", "$killer has slaughtered $target");
			ANNOUNCE_PVP_MSG = L2JModSettings.getProperty("AnnouncePvpMsg", "$killer has defeated $target");
			
			L2JMOD_CHAT_ADMIN = Boolean.parseBoolean(L2JModSettings.getProperty("ChatAdmin", "false"));
			
			L2JMOD_MULTILANG_DEFAULT = L2JModSettings.getProperty("MultiLangDefault", "en");
			L2JMOD_MULTILANG_ENABLE = Boolean.parseBoolean(L2JModSettings.getProperty("MultiLangEnable", "false"));
			String[] allowed = L2JModSettings.getProperty("MultiLangAllowed", L2JMOD_MULTILANG_DEFAULT).split(";");
			L2JMOD_MULTILANG_ALLOWED = new ArrayList<>(allowed.length);
			for (String lang : allowed)
			{
				L2JMOD_MULTILANG_ALLOWED.add(lang);
			}
			
			if (!L2JMOD_MULTILANG_ALLOWED.contains(L2JMOD_MULTILANG_DEFAULT))
			{
				_log.warning("MultiLang[Config.load()]: default language: " + L2JMOD_MULTILANG_DEFAULT + " is not in allowed list !");
			}
			
			L2JMOD_HELLBOUND_STATUS = Boolean.parseBoolean(L2JModSettings.getProperty("HellboundStatus", "False"));
			L2JMOD_MULTILANG_VOICED_ALLOW = Boolean.parseBoolean(L2JModSettings.getProperty("MultiLangVoiceCommand", "True"));
			L2JMOD_MULTILANG_SM_ENABLE = Boolean.parseBoolean(L2JModSettings.getProperty("MultiLangSystemMessageEnable", "False"));
			allowed = L2JModSettings.getProperty("MultiLangSystemMessageAllowed", "").split(";");
			L2JMOD_MULTILANG_SM_ALLOWED = new ArrayList<>(allowed.length);
			for (String lang : allowed)
			{
				if (!lang.isEmpty())
				{
					L2JMOD_MULTILANG_SM_ALLOWED.add(lang);
				}
			}
			L2JMOD_MULTILANG_NS_ENABLE = Boolean.parseBoolean(L2JModSettings.getProperty("MultiLangNpcStringEnable", "False"));
			allowed = L2JModSettings.getProperty("MultiLangNpcStringAllowed", "").split(";");
			L2JMOD_MULTILANG_NS_ALLOWED = new ArrayList<>(allowed.length);
			for (String lang : allowed)
			{
				if (!lang.isEmpty())
				{
					L2JMOD_MULTILANG_NS_ALLOWED.add(lang);
				}
			}
			
			L2WALKER_PROTECTION = Boolean.parseBoolean(L2JModSettings.getProperty("L2WalkerProtection", "False"));
			L2JMOD_DEBUG_VOICE_COMMAND = Boolean.parseBoolean(L2JModSettings.getProperty("DebugVoiceCommand", "False"));
			
			L2JMOD_DUALBOX_CHECK_MAX_PLAYERS_PER_IP = Integer.parseInt(L2JModSettings.getProperty("DualboxCheckMaxPlayersPerIP", "0"));
			L2JMOD_DUALBOX_CHECK_MAX_OLYMPIAD_PARTICIPANTS_PER_IP = Integer.parseInt(L2JModSettings.getProperty("DualboxCheckMaxOlympiadParticipantsPerIP", "0"));
			L2JMOD_DUALBOX_CHECK_MAX_L2EVENT_PARTICIPANTS_PER_IP = Integer.parseInt(L2JModSettings.getProperty("DualboxCheckMaxL2EventParticipantsPerIP", "0"));
			String[] dualboxCheckWhiteList = L2JModSettings.getProperty("DualboxCheckWhitelist", "127.0.0.1,0").split(";");
			L2JMOD_DUALBOX_CHECK_WHITELIST = new HashMap<>(dualboxCheckWhiteList.length);
			for (String entry : dualboxCheckWhiteList)
			{
				String[] entrySplit = entry.split(",");
				if (entrySplit.length != 2)
				{
					_log.warning(StringUtil.concat("DualboxCheck[Config.load()]: invalid config property -> DualboxCheckWhitelist \"", entry, "\""));
				}
				else
				{
					try
					{
						int num = Integer.parseInt(entrySplit[1]);
						num = num == 0 ? -1 : num;
						L2JMOD_DUALBOX_CHECK_WHITELIST.put(InetAddress.getByName(entrySplit[0]).hashCode(), num);
					}
					catch (UnknownHostException e)
					{
						_log.warning(StringUtil.concat("DualboxCheck[Config.load()]: invalid address -> DualboxCheckWhitelist \"", entrySplit[0], "\""));
					}
					catch (NumberFormatException e)
					{
						_log.warning(StringUtil.concat("DualboxCheck[Config.load()]: invalid number -> DualboxCheckWhitelist \"", entrySplit[1], "\""));
					}
				}
			}
			L2JMOD_ALLOW_CHANGE_PASSWORD = Boolean.parseBoolean(L2JModSettings.getProperty("AllowChangePassword", "False"));
			
			// Load PvP L2Properties file (if exists)
			final File pvp = new File(PVP_CONFIG_FILE);
			L2Properties PVPSettings = new L2Properties();
			try (InputStream is = new FileInputStream(pvp))
			{
				PVPSettings.load(is);
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "Error while loading PVP settings!", e);
			}
			
			KARMA_MIN_KARMA = Integer.parseInt(PVPSettings.getProperty("MinKarma", "240"));
			KARMA_MAX_KARMA = Integer.parseInt(PVPSettings.getProperty("MaxKarma", "10000"));
			KARMA_XP_DIVIDER = Integer.parseInt(PVPSettings.getProperty("XPDivider", "260"));
			KARMA_LOST_BASE = Integer.parseInt(PVPSettings.getProperty("BaseKarmaLost", "0"));
			KARMA_DROP_GM = Boolean.parseBoolean(PVPSettings.getProperty("CanGMDropEquipment", "false"));
			KARMA_AWARD_PK_KILL = Boolean.parseBoolean(PVPSettings.getProperty("AwardPKKillPVPPoint", "true"));
			KARMA_PK_LIMIT = Integer.parseInt(PVPSettings.getProperty("MinimumPKRequiredToDrop", "5"));
			KARMA_NONDROPPABLE_PET_ITEMS = PVPSettings.getProperty("ListOfPetItems", "2375,3500,3501,3502,4422,4423,4424,4425,6648,6649,6650,9882");
			KARMA_NONDROPPABLE_ITEMS = PVPSettings.getProperty("ListOfNonDroppableItems", "57,1147,425,1146,461,10,2368,7,6,2370,2369,6842,6611,6612,6613,6614,6615,6616,6617,6618,6619,6620,6621,7694,8181,5575,7694,9388,9389,9390");
			
			String[] karma = KARMA_NONDROPPABLE_PET_ITEMS.split(",");
			KARMA_LIST_NONDROPPABLE_PET_ITEMS = new int[karma.length];
			
			for (int i = 0; i < karma.length; i++)
			{
				KARMA_LIST_NONDROPPABLE_PET_ITEMS[i] = Integer.parseInt(karma[i]);
			}
			
			karma = KARMA_NONDROPPABLE_ITEMS.split(",");
			KARMA_LIST_NONDROPPABLE_ITEMS = new int[karma.length];
			
			for (int i = 0; i < karma.length; i++)
			{
				KARMA_LIST_NONDROPPABLE_ITEMS[i] = Integer.parseInt(karma[i]);
			}
			
			// sorting so binarySearch can be used later
			Arrays.sort(KARMA_LIST_NONDROPPABLE_PET_ITEMS);
			Arrays.sort(KARMA_LIST_NONDROPPABLE_ITEMS);
			
			PVP_NORMAL_TIME = Integer.parseInt(PVPSettings.getProperty("PvPVsNormalTime", "120000"));
			PVP_PVP_TIME = Integer.parseInt(PVPSettings.getProperty("PvPVsPvPTime", "60000"));
			
			// Load Olympiad L2Properties file (if exists)
			final File oly = new File(OLYMPIAD_CONFIG_FILE);
			L2Properties Olympiad = new L2Properties();
			try (InputStream is = new FileInputStream(oly))
			{
				Olympiad.load(is);
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "Error while loading Olympiad settings!", e);
			}
			
			ALT_OLY_START_TIME = Integer.parseInt(Olympiad.getProperty("AltOlyStartTime", "18"));
			ALT_OLY_MIN = Integer.parseInt(Olympiad.getProperty("AltOlyMin", "00"));
			ALT_OLY_CPERIOD = Long.parseLong(Olympiad.getProperty("AltOlyCPeriod", "21600000"));
			ALT_OLY_BATTLE = Long.parseLong(Olympiad.getProperty("AltOlyBattle", "300000"));
			ALT_OLY_WPERIOD = Long.parseLong(Olympiad.getProperty("AltOlyWPeriod", "604800000"));
			ALT_OLY_VPERIOD = Long.parseLong(Olympiad.getProperty("AltOlyVPeriod", "86400000"));
			ALT_OLY_START_POINTS = Integer.parseInt(Olympiad.getProperty("AltOlyStartPoints", "10"));
			ALT_OLY_WEEKLY_POINTS = Integer.parseInt(Olympiad.getProperty("AltOlyWeeklyPoints", "10"));
			ALT_OLY_CLASSED = Integer.parseInt(Olympiad.getProperty("AltOlyClassedParticipants", "11"));
			ALT_OLY_NONCLASSED = Integer.parseInt(Olympiad.getProperty("AltOlyNonClassedParticipants", "11"));
			ALT_OLY_TEAMS = Integer.parseInt(Olympiad.getProperty("AltOlyTeamsParticipants", "6"));
			ALT_OLY_REG_DISPLAY = Integer.parseInt(Olympiad.getProperty("AltOlyRegistrationDisplayNumber", "100"));
			ALT_OLY_CLASSED_REWARD = parseItemsList(Olympiad.getProperty("AltOlyClassedReward", "13722,50"));
			ALT_OLY_NONCLASSED_REWARD = parseItemsList(Olympiad.getProperty("AltOlyNonClassedReward", "13722,40"));
			ALT_OLY_TEAM_REWARD = parseItemsList(Olympiad.getProperty("AltOlyTeamReward", "13722,85"));
			ALT_OLY_COMP_RITEM = Integer.parseInt(Olympiad.getProperty("AltOlyCompRewItem", "13722"));
			ALT_OLY_MIN_MATCHES = Integer.parseInt(Olympiad.getProperty("AltOlyMinMatchesForPoints", "15"));
			ALT_OLY_GP_PER_POINT = Integer.parseInt(Olympiad.getProperty("AltOlyGPPerPoint", "1000"));
			ALT_OLY_HERO_POINTS = Integer.parseInt(Olympiad.getProperty("AltOlyHeroPoints", "200"));
			ALT_OLY_RANK1_POINTS = Integer.parseInt(Olympiad.getProperty("AltOlyRank1Points", "100"));
			ALT_OLY_RANK2_POINTS = Integer.parseInt(Olympiad.getProperty("AltOlyRank2Points", "75"));
			ALT_OLY_RANK3_POINTS = Integer.parseInt(Olympiad.getProperty("AltOlyRank3Points", "55"));
			ALT_OLY_RANK4_POINTS = Integer.parseInt(Olympiad.getProperty("AltOlyRank4Points", "40"));
			ALT_OLY_RANK5_POINTS = Integer.parseInt(Olympiad.getProperty("AltOlyRank5Points", "30"));
			ALT_OLY_MAX_POINTS = Integer.parseInt(Olympiad.getProperty("AltOlyMaxPoints", "10"));
			ALT_OLY_DIVIDER_CLASSED = Integer.parseInt(Olympiad.getProperty("AltOlyDividerClassed", "5"));
			ALT_OLY_DIVIDER_NON_CLASSED = Integer.parseInt(Olympiad.getProperty("AltOlyDividerNonClassed", "5"));
			ALT_OLY_MAX_WEEKLY_MATCHES = Integer.parseInt(Olympiad.getProperty("AltOlyMaxWeeklyMatches", "70"));
			ALT_OLY_MAX_WEEKLY_MATCHES_NON_CLASSED = Integer.parseInt(Olympiad.getProperty("AltOlyMaxWeeklyMatchesNonClassed", "60"));
			ALT_OLY_MAX_WEEKLY_MATCHES_CLASSED = Integer.parseInt(Olympiad.getProperty("AltOlyMaxWeeklyMatchesClassed", "30"));
			ALT_OLY_MAX_WEEKLY_MATCHES_TEAM = Integer.parseInt(Olympiad.getProperty("AltOlyMaxWeeklyMatchesTeam", "10"));
			ALT_OLY_LOG_FIGHTS = Boolean.parseBoolean(Olympiad.getProperty("AltOlyLogFights", "false"));
			ALT_OLY_SHOW_MONTHLY_WINNERS = Boolean.parseBoolean(Olympiad.getProperty("AltOlyShowMonthlyWinners", "true"));
			ALT_OLY_ANNOUNCE_GAMES = Boolean.parseBoolean(Olympiad.getProperty("AltOlyAnnounceGames", "true"));
			String[] olyRestrictedItems = Olympiad.getProperty("AltOlyRestrictedItems", "6611,6612,6613,6614,6615,6616,6617,6618,6619,6620,6621,9388,9389,9390,17049,17050,17051,17052,17053,17054,17055,17056,17057,17058,17059,17060,17061,20759,20775,20776,20777,20778,14774").split(",");
			LIST_OLY_RESTRICTED_ITEMS = new ArrayList<>(olyRestrictedItems.length);
			for (String id : olyRestrictedItems)
			{
				LIST_OLY_RESTRICTED_ITEMS.add(Integer.parseInt(id));
			}
			ALT_OLY_ENCHANT_LIMIT = Integer.parseInt(Olympiad.getProperty("AltOlyEnchantLimit", "-1"));
			ALT_OLY_WAIT_TIME = Integer.parseInt(Olympiad.getProperty("AltOlyWaitTime", "120"));
			
			final File hex = new File(HEXID_FILE);
			try (InputStream is = new FileInputStream(hex))
			{
				L2Properties Settings = new L2Properties();
				Settings.load(is);
				SERVER_ID = Integer.parseInt(Settings.getProperty("ServerID"));
				HEX_ID = new BigInteger(Settings.getProperty("HexID"), 16).toByteArray();
			}
			catch (Exception e)
			{
				_log.warning("Could not load HexID file (" + HEXID_FILE + "). Hopefully login will give us one.");
			}
			
			// Grand bosses
			L2Properties GrandBossSettings = new L2Properties();
			final File grandboss = new File(GRANDBOSS_CONFIG_FILE);
			try (InputStream is = new FileInputStream(grandboss))
			{
				GrandBossSettings.load(is);
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "Error while loading Grand Bosses settings!", e);
			}
			
			Antharas_Wait_Time = Integer.parseInt(GrandBossSettings.getProperty("AntharasWaitTime", "30"));
			if ((Antharas_Wait_Time < 3) || (Antharas_Wait_Time > 60))
			{
				Antharas_Wait_Time = 30;
			}
			Antharas_Wait_Time = Antharas_Wait_Time * 60000;
			
			Valakas_Wait_Time = Integer.parseInt(GrandBossSettings.getProperty("ValakasWaitTime", "30"));
			if ((Valakas_Wait_Time < 3) || (Valakas_Wait_Time > 60))
			{
				Valakas_Wait_Time = 30;
			}
			Valakas_Wait_Time = Valakas_Wait_Time * 60000;
			
			Interval_Of_Antharas_Spawn = Integer.parseInt(GrandBossSettings.getProperty("IntervalOfAntharasSpawn", "264"));
			if ((Interval_Of_Antharas_Spawn < 1) || (Interval_Of_Antharas_Spawn > 480))
			{
				Interval_Of_Antharas_Spawn = 264;
			}
			Interval_Of_Antharas_Spawn = Interval_Of_Antharas_Spawn * 3600000;
			
			Random_Of_Antharas_Spawn = Integer.parseInt(GrandBossSettings.getProperty("RandomOfAntharasSpawn", "72"));
			if ((Random_Of_Antharas_Spawn < 1) || (Random_Of_Antharas_Spawn > 192))
			{
				Random_Of_Antharas_Spawn = 72;
			}
			Random_Of_Antharas_Spawn = Random_Of_Antharas_Spawn * 3600000;
			
			Interval_Of_Valakas_Spawn = Integer.parseInt(GrandBossSettings.getProperty("IntervalOfValakasSpawn", "264"));
			if ((Interval_Of_Valakas_Spawn < 1) || (Interval_Of_Valakas_Spawn > 480))
			{
				Interval_Of_Valakas_Spawn = 264;
			}
			Interval_Of_Valakas_Spawn = Interval_Of_Valakas_Spawn * 3600000;
			
			Random_Of_Valakas_Spawn = Integer.parseInt(GrandBossSettings.getProperty("RandomOfValakasSpawn", "72"));
			if ((Random_Of_Valakas_Spawn < 1) || (Random_Of_Valakas_Spawn > 192))
			{
				Random_Of_Valakas_Spawn = 72;
			}
			Random_Of_Valakas_Spawn = Random_Of_Valakas_Spawn * 3600000;
			
			Interval_Of_Baium_Spawn = Integer.parseInt(GrandBossSettings.getProperty("IntervalOfBaiumSpawn", "168"));
			if ((Interval_Of_Baium_Spawn < 1) || (Interval_Of_Baium_Spawn > 480))
			{
				Interval_Of_Baium_Spawn = 168;
			}
			Interval_Of_Baium_Spawn = Interval_Of_Baium_Spawn * 3600000;
			
			Random_Of_Baium_Spawn = Integer.parseInt(GrandBossSettings.getProperty("RandomOfBaiumSpawn", "48"));
			if ((Random_Of_Baium_Spawn < 1) || (Random_Of_Baium_Spawn > 192))
			{
				Random_Of_Baium_Spawn = 48;
			}
			Random_Of_Baium_Spawn = Random_Of_Baium_Spawn * 3600000;
			
			Interval_Of_Core_Spawn = Integer.parseInt(GrandBossSettings.getProperty("IntervalOfCoreSpawn", "60"));
			if ((Interval_Of_Core_Spawn < 1) || (Interval_Of_Core_Spawn > 480))
			{
				Interval_Of_Core_Spawn = 60;
			}
			Interval_Of_Core_Spawn = Interval_Of_Core_Spawn * 3600000;
			
			Random_Of_Core_Spawn = Integer.parseInt(GrandBossSettings.getProperty("RandomOfCoreSpawn", "24"));
			if ((Random_Of_Core_Spawn < 1) || (Random_Of_Core_Spawn > 192))
			{
				Random_Of_Core_Spawn = 24;
			}
			Random_Of_Core_Spawn = Random_Of_Core_Spawn * 3600000;
			
			Interval_Of_Orfen_Spawn = Integer.parseInt(GrandBossSettings.getProperty("IntervalOfOrfenSpawn", "48"));
			if ((Interval_Of_Orfen_Spawn < 1) || (Interval_Of_Orfen_Spawn > 480))
			{
				Interval_Of_Orfen_Spawn = 48;
			}
			Interval_Of_Orfen_Spawn = Interval_Of_Orfen_Spawn * 3600000;
			
			Random_Of_Orfen_Spawn = Integer.parseInt(GrandBossSettings.getProperty("RandomOfOrfenSpawn", "20"));
			if ((Random_Of_Orfen_Spawn < 1) || (Random_Of_Orfen_Spawn > 192))
			{
				Random_Of_Orfen_Spawn = 20;
			}
			Random_Of_Orfen_Spawn = Random_Of_Orfen_Spawn * 3600000;
			
			Interval_Of_QueenAnt_Spawn = Integer.parseInt(GrandBossSettings.getProperty("IntervalOfQueenAntSpawn", "36"));
			if ((Interval_Of_QueenAnt_Spawn < 1) || (Interval_Of_QueenAnt_Spawn > 480))
			{
				Interval_Of_QueenAnt_Spawn = 36;
			}
			Interval_Of_QueenAnt_Spawn = Interval_Of_QueenAnt_Spawn * 3600000;
			
			Random_Of_QueenAnt_Spawn = Integer.parseInt(GrandBossSettings.getProperty("RandomOfQueenAntSpawn", "17"));
			if ((Random_Of_QueenAnt_Spawn < 1) || (Random_Of_QueenAnt_Spawn > 192))
			{
				Random_Of_QueenAnt_Spawn = 17;
			}
			Random_Of_QueenAnt_Spawn = Random_Of_QueenAnt_Spawn * 3600000;
			
			Interval_Of_Zaken_Spawn = Integer.parseInt(GrandBossSettings.getProperty("IntervalOfZakenSpawn", "60"));
			if ((Interval_Of_Zaken_Spawn < 1) || (Interval_Of_Zaken_Spawn > 480))
			{
				Interval_Of_Zaken_Spawn = 60;
			}
			Interval_Of_Zaken_Spawn = Interval_Of_Zaken_Spawn * 3600000;
			
			Random_Of_Zaken_Spawn = Integer.parseInt(GrandBossSettings.getProperty("RandomOfZakenSpawn", "20"));
			if ((Random_Of_Zaken_Spawn < 1) || (Random_Of_Zaken_Spawn > 192))
			{
				Random_Of_Zaken_Spawn = 20;
			}
			Random_Of_Zaken_Spawn = Random_Of_Zaken_Spawn * 3600000;
			
			INTERVAL_OF_BELETH_SPAWN = Integer.parseInt(GrandBossSettings.getProperty("IntervalOfBelethSpawn", "192"));
			if ((INTERVAL_OF_BELETH_SPAWN < 1) || (INTERVAL_OF_BELETH_SPAWN > 480))
			{
				INTERVAL_OF_BELETH_SPAWN = 192;
			}
			INTERVAL_OF_BELETH_SPAWN *= 3600000;
			
			RANDOM_OF_BELETH_SPAWN = Integer.parseInt(GrandBossSettings.getProperty("RandomOfBelethSpawn", "148"));
			if ((RANDOM_OF_BELETH_SPAWN < 1) || (RANDOM_OF_BELETH_SPAWN > 192))
			{
				RANDOM_OF_BELETH_SPAWN = 148;
			}
			RANDOM_OF_BELETH_SPAWN *= 3600000;
			
			BELETH_MIN_PLAYERS = Integer.parseInt(GrandBossSettings.getProperty("BelethMinPlayers", "36"));
			
			// Gracia Seeds
			L2Properties GraciaSeedsSettings = new L2Properties();
			final File graciaseeds = new File(GRACIASEEDS_CONFIG_FILE);
			try (InputStream is = new FileInputStream(graciaseeds))
			{
				GraciaSeedsSettings.load(is);
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "Error while loading Gracia Seeds settings!", e);
			}
			
			// Seed of Destruction
			SOD_TIAT_KILL_COUNT = Integer.parseInt(GraciaSeedsSettings.getProperty("TiatKillCountForNextState", "10"));
			SOD_STAGE_2_LENGTH = Long.parseLong(GraciaSeedsSettings.getProperty("Stage2Length", "720")) * 60000;
			
			final File chat_filter = new File(CHAT_FILTER_FILE);
			try (FileReader fr = new FileReader(chat_filter);
				BufferedReader br = new BufferedReader(fr);
				LineNumberReader lnr = new LineNumberReader(br))
			{
				FILTER_LIST = new ArrayList<>();
				
				String line = null;
				while ((line = lnr.readLine()) != null)
				{
					if (line.trim().isEmpty() || (line.charAt(0) == '#'))
					{
						continue;
					}
					
					FILTER_LIST.add(line.trim());
				}
				_log.info("Loaded " + FILTER_LIST.size() + " Filter Words.");
			}
			catch (Exception e)
			{
				_log.log(Level.WARNING, "Error while loading chat filter words!", e);
			}
			
			// Security
			L2Properties SecuritySettings = new L2Properties();
			final File security = new File(SECURITY_CONFIG_FILE);
			try (InputStream is = new FileInputStream(security))
			{
				SecuritySettings.load(is);
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "Error while loading Security settings!", e);
			}
			
			// Second Auth Settings
			SECOND_AUTH_ENABLED = Boolean.parseBoolean(SecuritySettings.getProperty("SecondAuthEnabled", "false"));
			SECOND_AUTH_MAX_ATTEMPTS = Integer.parseInt(SecuritySettings.getProperty("SecondAuthMaxAttempts", "5"));
			SECOND_AUTH_BAN_TIME = Integer.parseInt(SecuritySettings.getProperty("SecondAuthBanTime", "480"));
			SECOND_AUTH_REC_LINK = SecuritySettings.getProperty("SecondAuthRecoveryLink", "5");
			
			L2Properties ClanHallSiege = new L2Properties();
			final File ch_siege = new File(CH_SIEGE_FILE);
			try (InputStream is = new FileInputStream(ch_siege))
			{
				ClanHallSiege.load(is);
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "Error while loading Clan Hall Siege settings!", e);
			}
			
			CHS_MAX_ATTACKERS = Integer.parseInt(ClanHallSiege.getProperty("MaxAttackers", "500"));
			CHS_CLAN_MINLEVEL = Integer.parseInt(ClanHallSiege.getProperty("MinClanLevel", "4"));
			CHS_MAX_FLAGS_PER_CLAN = Integer.parseInt(ClanHallSiege.getProperty("MaxFlagsPerClan", "1"));
			CHS_ENABLE_FAME = Boolean.parseBoolean(ClanHallSiege.getProperty("EnableFame", "false"));
			CHS_FAME_AMOUNT = Integer.parseInt(ClanHallSiege.getProperty("FameAmount", "0"));
			CHS_FAME_FREQUENCY = Integer.parseInt(ClanHallSiege.getProperty("FameFrequency", "0"));
		}
		else if (Server.serverMode == Server.MODE_LOGINSERVER)
		{
			L2Properties ServerSettings = new L2Properties();
			final File login = new File(LOGIN_CONFIGURATION_FILE);
			try (InputStream is = new FileInputStream(login))
			{
				ServerSettings.load(is);
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "Error while loading Login Server settings!", e);
			}
			
			GAME_SERVER_LOGIN_HOST = ServerSettings.getProperty("LoginHostname", "127.0.0.1");
			GAME_SERVER_LOGIN_PORT = Integer.parseInt(ServerSettings.getProperty("LoginPort", "9013"));
			
			LOGIN_BIND_ADDRESS = ServerSettings.getProperty("LoginserverHostname", "*");
			PORT_LOGIN = Integer.parseInt(ServerSettings.getProperty("LoginserverPort", "2106"));
			
			try
			{
				DATAPACK_ROOT = new File(ServerSettings.getProperty("DatapackRoot", ".").replaceAll("\\\\", "/")).getCanonicalFile();
			}
			catch (IOException e)
			{
				_log.log(Level.WARNING, "Error setting datapack root!", e);
				DATAPACK_ROOT = new File(".");
			}
			
			DEBUG = Boolean.parseBoolean(ServerSettings.getProperty("Debug", "false"));
			
			ACCEPT_NEW_GAMESERVER = Boolean.parseBoolean(ServerSettings.getProperty("AcceptNewGameServer", "True"));
			
			LOGIN_TRY_BEFORE_BAN = Integer.parseInt(ServerSettings.getProperty("LoginTryBeforeBan", "5"));
			LOGIN_BLOCK_AFTER_BAN = Integer.parseInt(ServerSettings.getProperty("LoginBlockAfterBan", "900"));
			
			LOG_LOGIN_CONTROLLER = Boolean.parseBoolean(ServerSettings.getProperty("LogLoginController", "true"));
			
			LOGIN_SERVER_SCHEDULE_RESTART = Boolean.parseBoolean(ServerSettings.getProperty("LoginRestartSchedule", "False"));
			LOGIN_SERVER_SCHEDULE_RESTART_TIME = Long.parseLong(ServerSettings.getProperty("LoginRestartTime", "24"));
			
			DATABASE_DRIVER = ServerSettings.getProperty("Driver", "com.mysql.jdbc.Driver");
			DATABASE_URL = ServerSettings.getProperty("URL", "jdbc:mysql://localhost/l2jls");
			DATABASE_LOGIN = ServerSettings.getProperty("Login", "root");
			DATABASE_PASSWORD = ServerSettings.getProperty("Password", "");
			DATABASE_MAX_CONNECTIONS = Integer.parseInt(ServerSettings.getProperty("MaximumDbConnections", "10"));
			DATABASE_MAX_IDLE_TIME = Integer.parseInt(ServerSettings.getProperty("MaximumDbIdleTime", "0"));
			
			SHOW_LICENCE = Boolean.parseBoolean(ServerSettings.getProperty("ShowLicence", "true"));
			
			AUTO_CREATE_ACCOUNTS = Boolean.parseBoolean(ServerSettings.getProperty("AutoCreateAccounts", "True"));
			
			FLOOD_PROTECTION = Boolean.parseBoolean(ServerSettings.getProperty("EnableFloodProtection", "True"));
			FAST_CONNECTION_LIMIT = Integer.parseInt(ServerSettings.getProperty("FastConnectionLimit", "15"));
			NORMAL_CONNECTION_TIME = Integer.parseInt(ServerSettings.getProperty("NormalConnectionTime", "700"));
			FAST_CONNECTION_TIME = Integer.parseInt(ServerSettings.getProperty("FastConnectionTime", "350"));
			MAX_CONNECTION_PER_IP = Integer.parseInt(ServerSettings.getProperty("MaxConnectionPerIP", "50"));
			
			// MMO
			final File mmo = new File(MMO_CONFIG_FILE);
			L2Properties mmoSettings = new L2Properties();
			try (InputStream is = new FileInputStream(mmo))
			{
				mmoSettings.load(is);
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "Error while loading MMO settings!", e);
			}
			
			MMO_SELECTOR_SLEEP_TIME = Integer.parseInt(mmoSettings.getProperty("SleepTime", "20"));
			MMO_MAX_SEND_PER_PASS = Integer.parseInt(mmoSettings.getProperty("MaxSendPerPass", "12"));
			MMO_MAX_READ_PER_PASS = Integer.parseInt(mmoSettings.getProperty("MaxReadPerPass", "12"));
			MMO_HELPER_BUFFER_COUNT = Integer.parseInt(mmoSettings.getProperty("HelperBufferCount", "20"));
			MMO_TCP_NODELAY = Boolean.parseBoolean(mmoSettings.getProperty("TcpNoDelay", "False"));
			
			// Load Telnet L2Properties file (if exists)
			L2Properties telnetSettings = new L2Properties();
			final File telnet = new File(TELNET_FILE);
			try (InputStream is = new FileInputStream(telnet))
			{
				telnetSettings.load(is);
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "Error while loading Telnet settings!", e);
			}
			
			IS_TELNET_ENABLED = Boolean.parseBoolean(telnetSettings.getProperty("EnableTelnet", "false"));
			
			// Email
			L2Properties emailSettings = new L2Properties();
			final File email = new File(EMAIL_CONFIG_FILE);
			try (InputStream is = new FileInputStream(email))
			{
				emailSettings.load(is);
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "Error while loading Email settings!", e);
			}
			
			EMAIL_SERVERINFO_NAME = emailSettings.getProperty("ServerInfoName", "Unconfigured L2J Server");
			EMAIL_SERVERINFO_ADDRESS = emailSettings.getProperty("ServerInfoAddress", "info@myl2jserver.com");
			
			EMAIL_SYS_ENABLED = Boolean.parseBoolean(emailSettings.getProperty("EmailSystemEnabled", "false"));
			EMAIL_SYS_HOST = emailSettings.getProperty("SmtpServerHost", "smtp.gmail.com");
			EMAIL_SYS_PORT = Integer.parseInt(emailSettings.getProperty("SmtpServerPort", "465"));
			EMAIL_SYS_SMTP_AUTH = Boolean.parseBoolean(emailSettings.getProperty("SmtpAuthRequired", "true"));
			EMAIL_SYS_FACTORY = emailSettings.getProperty("SmtpFactory", "javax.net.ssl.SSLSocketFactory");
			EMAIL_SYS_FACTORY_CALLBACK = Boolean.parseBoolean(emailSettings.getProperty("SmtpFactoryCallback", "false"));
			EMAIL_SYS_USERNAME = emailSettings.getProperty("SmtpUsername", "user@gmail.com");
			EMAIL_SYS_PASSWORD = emailSettings.getProperty("SmtpPassword", "password");
			EMAIL_SYS_ADDRESS = emailSettings.getProperty("EmailSystemAddress", "noreply@myl2jserver.com");
			EMAIL_SYS_SELECTQUERY = emailSettings.getProperty("EmailDBSelectQuery", "SELECT value FROM account_data WHERE account_name=? AND var='email_addr'");
			EMAIL_SYS_DBFIELD = emailSettings.getProperty("EmailDBField", "value");
		}
		else
		{
			_log.severe("Could not Load Config: server mode was not set!");
		}
	}
	
	/**
	 * Set a new value to a config parameter.
	 * @param pName the name of the parameter whose value to change
	 * @param pValue the new value of the parameter
	 * @return {@code true} if the value of the parameter was changed, {@code false} otherwise
	 */
	public static boolean setParameterValue(String pName, String pValue)
	{
		switch (pName.trim().toLowerCase())
		{
		// rates.properties
			case "ratexp":
				RATE_XP = Float.parseFloat(pValue);
				break;
			case "ratesp":
				RATE_SP = Float.parseFloat(pValue);
				break;
			case "ratepartyxp":
				RATE_PARTY_XP = Float.parseFloat(pValue);
				break;
			case "rateconsumablecost":
				RATE_CONSUMABLE_COST = Float.parseFloat(pValue);
				break;
			case "rateextractable":
				RATE_EXTRACTABLE = Float.parseFloat(pValue);
				break;
			case "ratedropitems":
				RATE_DROP_ITEMS = Float.parseFloat(pValue);
				break;
			case "ratedropadena":
				RATE_DROP_ITEMS_ID.put(PcInventory.ADENA_ID, Float.parseFloat(pValue));
				break;
			case "rateraiddropitems":
				RATE_DROP_ITEMS_BY_RAID = Float.parseFloat(pValue);
				break;
			case "ratedropspoil":
				RATE_DROP_SPOIL = Float.parseFloat(pValue);
				break;
			case "ratedropmanor":
				RATE_DROP_MANOR = Integer.parseInt(pValue);
				break;
			case "ratequestdrop":
				RATE_QUEST_DROP = Float.parseFloat(pValue);
				break;
			case "ratequestreward":
				RATE_QUEST_REWARD = Float.parseFloat(pValue);
				break;
			case "ratequestrewardxp":
				RATE_QUEST_REWARD_XP = Float.parseFloat(pValue);
				break;
			case "ratequestrewardsp":
				RATE_QUEST_REWARD_SP = Float.parseFloat(pValue);
				break;
			case "ratequestrewardadena":
				RATE_QUEST_REWARD_ADENA = Float.parseFloat(pValue);
				break;
			case "usequestrewardmultipliers":
				RATE_QUEST_REWARD_USE_MULTIPLIERS = Boolean.parseBoolean(pValue);
				break;
			case "ratequestrewardpotion":
				RATE_QUEST_REWARD_POTION = Float.parseFloat(pValue);
				break;
			case "ratequestrewardscroll":
				RATE_QUEST_REWARD_SCROLL = Float.parseFloat(pValue);
				break;
			case "ratequestrewardrecipe":
				RATE_QUEST_REWARD_RECIPE = Float.parseFloat(pValue);
				break;
			case "ratequestrewardmaterial":
				RATE_QUEST_REWARD_MATERIAL = Float.parseFloat(pValue);
				break;
			case "ratehellboundtrustincrease":
				RATE_HB_TRUST_INCREASE = Float.parseFloat(pValue);
				break;
			case "ratehellboundtrustdecrease":
				RATE_HB_TRUST_DECREASE = Float.parseFloat(pValue);
				break;
			case "ratevitalitylevel1":
				RATE_VITALITY_LEVEL_1 = Float.parseFloat(pValue);
				break;
			case "ratevitalitylevel2":
				RATE_VITALITY_LEVEL_2 = Float.parseFloat(pValue);
				break;
			case "ratevitalitylevel3":
				RATE_VITALITY_LEVEL_3 = Float.parseFloat(pValue);
				break;
			case "ratevitalitylevel4":
				RATE_VITALITY_LEVEL_4 = Float.parseFloat(pValue);
				break;
			case "raterecoverypeacezone":
				RATE_RECOVERY_VITALITY_PEACE_ZONE = Float.parseFloat(pValue);
				break;
			case "ratevitalitylost":
				RATE_VITALITY_LOST = Float.parseFloat(pValue);
				break;
			case "ratevitalitygain":
				RATE_VITALITY_GAIN = Float.parseFloat(pValue);
				break;
			case "raterecoveryonreconnect":
				RATE_RECOVERY_ON_RECONNECT = Float.parseFloat(pValue);
				break;
			case "ratekarmaexplost":
				RATE_KARMA_EXP_LOST = Float.parseFloat(pValue);
				break;
			case "ratesiegeguardsprice":
				RATE_SIEGE_GUARDS_PRICE = Float.parseFloat(pValue);
				break;
			case "ratecommonherbs":
				RATE_DROP_COMMON_HERBS = Float.parseFloat(pValue);
				break;
			case "ratehpherbs":
				RATE_DROP_HP_HERBS = Float.parseFloat(pValue);
				break;
			case "ratempherbs":
				RATE_DROP_MP_HERBS = Float.parseFloat(pValue);
				break;
			case "ratespecialherbs":
				RATE_DROP_SPECIAL_HERBS = Float.parseFloat(pValue);
				break;
			case "ratevitalityherbs":
				RATE_DROP_VITALITY_HERBS = Float.parseFloat(pValue);
				break;
			case "playerdroplimit":
				PLAYER_DROP_LIMIT = Integer.parseInt(pValue);
				break;
			case "playerratedrop":
				PLAYER_RATE_DROP = Integer.parseInt(pValue);
				break;
			case "playerratedropitem":
				PLAYER_RATE_DROP_ITEM = Integer.parseInt(pValue);
				break;
			case "playerratedropequip":
				PLAYER_RATE_DROP_EQUIP = Integer.parseInt(pValue);
				break;
			case "playerratedropequipweapon":
				PLAYER_RATE_DROP_EQUIP_WEAPON = Integer.parseInt(pValue);
				break;
			case "petxprate":
				PET_XP_RATE = Float.parseFloat(pValue);
				break;
			case "petfoodrate":
				PET_FOOD_RATE = Integer.parseInt(pValue);
				break;
			case "sineaterxprate":
				SINEATER_XP_RATE = Float.parseFloat(pValue);
				break;
			case "karmadroplimit":
				KARMA_DROP_LIMIT = Integer.parseInt(pValue);
				break;
			case "karmaratedrop":
				KARMA_RATE_DROP = Integer.parseInt(pValue);
				break;
			case "karmaratedropitem":
				KARMA_RATE_DROP_ITEM = Integer.parseInt(pValue);
				break;
			case "karmaratedropequip":
				KARMA_RATE_DROP_EQUIP = Integer.parseInt(pValue);
				break;
			case "karmaratedropequipweapon":
				KARMA_RATE_DROP_EQUIP_WEAPON = Integer.parseInt(pValue);
				break;
			case "autodestroydroppeditemafter":
				AUTODESTROY_ITEM_AFTER = Integer.parseInt(pValue);
				break;
			case "destroyplayerdroppeditem":
				DESTROY_DROPPED_PLAYER_ITEM = Boolean.parseBoolean(pValue);
				break;
			case "destroyequipableitem":
				DESTROY_EQUIPABLE_PLAYER_ITEM = Boolean.parseBoolean(pValue);
				break;
			case "savedroppeditem":
				SAVE_DROPPED_ITEM = Boolean.parseBoolean(pValue);
				break;
			case "emptydroppeditemtableafterload":
				EMPTY_DROPPED_ITEM_TABLE_AFTER_LOAD = Boolean.parseBoolean(pValue);
				break;
			case "savedroppediteminterval":
				SAVE_DROPPED_ITEM_INTERVAL = Integer.parseInt(pValue);
				break;
			case "cleardroppeditemtable":
				CLEAR_DROPPED_ITEM_TABLE = Boolean.parseBoolean(pValue);
				break;
			case "precisedropcalculation":
				PRECISE_DROP_CALCULATION = Boolean.parseBoolean(pValue);
				break;
			case "multipleitemdrop":
				MULTIPLE_ITEM_DROP = Boolean.parseBoolean(pValue);
				break;
			case "lowweight":
				LOW_WEIGHT = Float.parseFloat(pValue);
				break;
			case "mediumweight":
				MEDIUM_WEIGHT = Float.parseFloat(pValue);
				break;
			case "highweight":
				HIGH_WEIGHT = Float.parseFloat(pValue);
				break;
			case "advanceddiagonalstrategy":
				ADVANCED_DIAGONAL_STRATEGY = Boolean.parseBoolean(pValue);
				break;
			case "diagonalweight":
				DIAGONAL_WEIGHT = Float.parseFloat(pValue);
				break;
			case "maxpostfilterpasses":
				MAX_POSTFILTER_PASSES = Integer.parseInt(pValue);
				break;
			case "coordsynchronize":
				COORD_SYNCHRONIZE = Integer.parseInt(pValue);
				break;
			case "deletecharafterdays":
				DELETE_DAYS = Integer.parseInt(pValue);
				break;
			case "clientpacketqueuesize":
				CLIENT_PACKET_QUEUE_SIZE = Integer.parseInt(pValue);
				if (CLIENT_PACKET_QUEUE_SIZE == 0)
				{
					CLIENT_PACKET_QUEUE_SIZE = MMO_MAX_READ_PER_PASS + 1;
				}
				break;
			case "clientpacketqueuemaxburstsize":
				CLIENT_PACKET_QUEUE_MAX_BURST_SIZE = Integer.parseInt(pValue);
				if (CLIENT_PACKET_QUEUE_MAX_BURST_SIZE == 0)
				{
					CLIENT_PACKET_QUEUE_MAX_BURST_SIZE = MMO_MAX_READ_PER_PASS;
				}
				break;
			case "clientpacketqueuemaxpacketspersecond":
				CLIENT_PACKET_QUEUE_MAX_PACKETS_PER_SECOND = Integer.parseInt(pValue);
				break;
			case "clientpacketqueuemeasureinterval":
				CLIENT_PACKET_QUEUE_MEASURE_INTERVAL = Integer.parseInt(pValue);
				break;
			case "clientpacketqueuemaxaveragepacketspersecond":
				CLIENT_PACKET_QUEUE_MAX_AVERAGE_PACKETS_PER_SECOND = Integer.parseInt(pValue);
				break;
			case "clientpacketqueuemaxfloodspermin":
				CLIENT_PACKET_QUEUE_MAX_FLOODS_PER_MIN = Integer.parseInt(pValue);
				break;
			case "clientpacketqueuemaxoverflowspermin":
				CLIENT_PACKET_QUEUE_MAX_OVERFLOWS_PER_MIN = Integer.parseInt(pValue);
				break;
			case "clientpacketqueuemaxunderflowspermin":
				CLIENT_PACKET_QUEUE_MAX_UNDERFLOWS_PER_MIN = Integer.parseInt(pValue);
				break;
			case "clientpacketqueuemaxunknownpermin":
				CLIENT_PACKET_QUEUE_MAX_UNKNOWN_PER_MIN = Integer.parseInt(pValue);
				break;
			case "allowdiscarditem":
				ALLOW_DISCARDITEM = Boolean.parseBoolean(pValue);
				break;
			case "allowrefund":
				ALLOW_REFUND = Boolean.parseBoolean(pValue);
				break;
			case "allowwarehouse":
				ALLOW_WAREHOUSE = Boolean.parseBoolean(pValue);
				break;
			case "allowwear":
				ALLOW_WEAR = Boolean.parseBoolean(pValue);
				break;
			case "weardelay":
				WEAR_DELAY = Integer.parseInt(pValue);
				break;
			case "wearprice":
				WEAR_PRICE = Integer.parseInt(pValue);
				break;
			case "allowwater":
				ALLOW_WATER = Boolean.parseBoolean(pValue);
				break;
			case "allowrentpet":
				ALLOW_RENTPET = Boolean.parseBoolean(pValue);
				break;
			case "boatbroadcastradius":
				BOAT_BROADCAST_RADIUS = Integer.parseInt(pValue);
				break;
			case "allowcursedweapons":
				ALLOW_CURSED_WEAPONS = Boolean.parseBoolean(pValue);
				break;
			case "allowmanor":
				ALLOW_MANOR = Boolean.parseBoolean(pValue);
				break;
			case "allownpcwalkers":
				ALLOW_NPC_WALKERS = Boolean.parseBoolean(pValue);
				break;
			case "allowpetwalkers":
				ALLOW_PET_WALKERS = Boolean.parseBoolean(pValue);
				break;
			case "bypassvalidation":
				BYPASS_VALIDATION = Boolean.parseBoolean(pValue);
				break;
			case "communitytype":
				COMMUNITY_TYPE = Integer.parseInt(pValue);
				break;
			case "bbsshowplayerlist":
				BBS_SHOW_PLAYERLIST = Boolean.parseBoolean(pValue);
				break;
			case "bbsdefault":
				BBS_DEFAULT = pValue;
				break;
			case "showleveloncommunityboard":
				SHOW_LEVEL_COMMUNITYBOARD = Boolean.parseBoolean(pValue);
				break;
			case "showstatusoncommunityboard":
				SHOW_STATUS_COMMUNITYBOARD = Boolean.parseBoolean(pValue);
				break;
			case "namepagesizeoncommunityboard":
				NAME_PAGE_SIZE_COMMUNITYBOARD = Integer.parseInt(pValue);
				break;
			case "nameperrowoncommunityboard":
				NAME_PER_ROW_COMMUNITYBOARD = Integer.parseInt(pValue);
				break;
			case "showservernews":
				SERVER_NEWS = Boolean.parseBoolean(pValue);
				break;
			case "shownpclevel":
				SHOW_NPC_LVL = Boolean.parseBoolean(pValue);
				break;
			case "showcrestwithoutquest":
				SHOW_CREST_WITHOUT_QUEST = Boolean.parseBoolean(pValue);
				break;
			case "forceinventoryupdate":
				FORCE_INVENTORY_UPDATE = Boolean.parseBoolean(pValue);
				break;
			case "autodeleteinvalidquestdata":
				AUTODELETE_INVALID_QUEST_DATA = Boolean.parseBoolean(pValue);
				break;
			case "maximumonlineusers":
				MAXIMUM_ONLINE_USERS = Integer.parseInt(pValue);
				break;
			case "peacezonemode":
				PEACE_ZONE_MODE = Integer.parseInt(pValue);
				break;
			case "checkknownlist":
				CHECK_KNOWN = Boolean.parseBoolean(pValue);
				break;
			case "maxdriftrange":
				MAX_DRIFT_RANGE = Integer.parseInt(pValue);
				break;
			case "usedeepbluedroprules":
				DEEPBLUE_DROP_RULES = Boolean.parseBoolean(pValue);
				break;
			case "usedeepbluedroprulesraid":
				DEEPBLUE_DROP_RULES_RAID = Boolean.parseBoolean(pValue);
				break;
			case "guardattackaggromob":
				GUARD_ATTACK_AGGRO_MOB = Boolean.parseBoolean(pValue);
				break;
			case "cancellessereffect":
				EFFECT_CANCELING = Boolean.parseBoolean(pValue);
				break;
			case "maximumslotsfornodwarf":
				INVENTORY_MAXIMUM_NO_DWARF = Integer.parseInt(pValue);
				break;
			case "maximumslotsfordwarf":
				INVENTORY_MAXIMUM_DWARF = Integer.parseInt(pValue);
				break;
			case "maximumslotsforgmplayer":
				INVENTORY_MAXIMUM_GM = Integer.parseInt(pValue);
				break;
			case "maximumslotsforquestitems":
				INVENTORY_MAXIMUM_QUEST_ITEMS = Integer.parseInt(pValue);
				break;
			case "maximumwarehouseslotsfornodwarf":
				WAREHOUSE_SLOTS_NO_DWARF = Integer.parseInt(pValue);
				break;
			case "maximumwarehouseslotsfordwarf":
				WAREHOUSE_SLOTS_DWARF = Integer.parseInt(pValue);
				break;
			case "maximumwarehouseslotsforclan":
				WAREHOUSE_SLOTS_CLAN = Integer.parseInt(pValue);
				break;
			case "enchantchanceelementstone":
				ENCHANT_CHANCE_ELEMENT_STONE = Integer.parseInt(pValue);
				break;
			case "enchantchanceelementcrystal":
				ENCHANT_CHANCE_ELEMENT_CRYSTAL = Integer.parseInt(pValue);
				break;
			case "enchantchanceelementjewel":
				ENCHANT_CHANCE_ELEMENT_JEWEL = Integer.parseInt(pValue);
				break;
			case "enchantchanceelementenergy":
				ENCHANT_CHANCE_ELEMENT_ENERGY = Integer.parseInt(pValue);
				break;
			case "enchantsafemax":
				ENCHANT_SAFE_MAX = Integer.parseInt(pValue);
				break;
			case "enchantsafemaxfull":
				ENCHANT_SAFE_MAX_FULL = Integer.parseInt(pValue);
				break;
			case "augmentationngskillchance":
				AUGMENTATION_NG_SKILL_CHANCE = Integer.parseInt(pValue);
				break;
			case "augmentationngglowchance":
				AUGMENTATION_NG_GLOW_CHANCE = Integer.parseInt(pValue);
				break;
			case "augmentationmidskillchance":
				AUGMENTATION_MID_SKILL_CHANCE = Integer.parseInt(pValue);
				break;
			case "augmentationmidglowchance":
				AUGMENTATION_MID_GLOW_CHANCE = Integer.parseInt(pValue);
				break;
			case "augmentationhighskillchance":
				AUGMENTATION_HIGH_SKILL_CHANCE = Integer.parseInt(pValue);
				break;
			case "augmentationhighglowchance":
				AUGMENTATION_HIGH_GLOW_CHANCE = Integer.parseInt(pValue);
				break;
			case "augmentationtopskillchance":
				AUGMENTATION_TOP_SKILL_CHANCE = Integer.parseInt(pValue);
				break;
			case "augmentationtopglowchance":
				AUGMENTATION_TOP_GLOW_CHANCE = Integer.parseInt(pValue);
				break;
			case "augmentationbasestatchance":
				AUGMENTATION_BASESTAT_CHANCE = Integer.parseInt(pValue);
				break;
			case "hpregenmultiplier":
				HP_REGEN_MULTIPLIER = Double.parseDouble(pValue);
				break;
			case "mpregenmultiplier":
				MP_REGEN_MULTIPLIER = Double.parseDouble(pValue);
				break;
			case "cpregenmultiplier":
				CP_REGEN_MULTIPLIER = Double.parseDouble(pValue);
				break;
			case "raidhpregenmultiplier":
				RAID_HP_REGEN_MULTIPLIER = Double.parseDouble(pValue);
				break;
			case "raidmpregenmultiplier":
				RAID_MP_REGEN_MULTIPLIER = Double.parseDouble(pValue);
				break;
			case "raidpdefencemultiplier":
				RAID_PDEFENCE_MULTIPLIER = Double.parseDouble(pValue) / 100;
				break;
			case "raidmdefencemultiplier":
				RAID_MDEFENCE_MULTIPLIER = Double.parseDouble(pValue) / 100;
				break;
			case "raidpattackmultiplier":
				RAID_PATTACK_MULTIPLIER = Double.parseDouble(pValue) / 100;
				break;
			case "raidmattackmultiplier":
				RAID_MATTACK_MULTIPLIER = Double.parseDouble(pValue) / 100;
				break;
			case "raidminionrespawntime":
				RAID_MINION_RESPAWN_TIMER = Integer.parseInt(pValue);
				break;
			case "raidchaostime":
				RAID_CHAOS_TIME = Integer.parseInt(pValue);
				break;
			case "grandchaostime":
				GRAND_CHAOS_TIME = Integer.parseInt(pValue);
				break;
			case "minionchaostime":
				MINION_CHAOS_TIME = Integer.parseInt(pValue);
				break;
			case "startingadena":
				STARTING_ADENA = Long.parseLong(pValue);
				break;
			case "startinglevel":
				STARTING_LEVEL = Byte.parseByte(pValue);
				break;
			case "startingsp":
				STARTING_SP = Integer.parseInt(pValue);
				break;
			case "unstuckinterval":
				UNSTUCK_INTERVAL = Integer.parseInt(pValue);
				break;
			case "teleportwatchdogtimeout":
				TELEPORT_WATCHDOG_TIMEOUT = Integer.parseInt(pValue);
				break;
			case "playerspawnprotection":
				PLAYER_SPAWN_PROTECTION = Integer.parseInt(pValue);
				break;
			case "playerfakedeathupprotection":
				PLAYER_FAKEDEATH_UP_PROTECTION = Integer.parseInt(pValue);
				break;
			case "restoreplayerinstance":
				RESTORE_PLAYER_INSTANCE = Boolean.parseBoolean(pValue);
				break;
			case "allowsummontoinstance":
				ALLOW_SUMMON_TO_INSTANCE = Boolean.parseBoolean(pValue);
				break;
			case "partyxpcutoffmethod":
				PARTY_XP_CUTOFF_METHOD = pValue;
				break;
			case "partyxpcutoffpercent":
				PARTY_XP_CUTOFF_PERCENT = Double.parseDouble(pValue);
				break;
			case "partyxpcutofflevel":
				PARTY_XP_CUTOFF_LEVEL = Integer.parseInt(pValue);
				break;
			case "respawnrestorecp":
				RESPAWN_RESTORE_CP = Double.parseDouble(pValue) / 100;
				break;
			case "respawnrestorehp":
				RESPAWN_RESTORE_HP = Double.parseDouble(pValue) / 100;
				break;
			case "respawnrestoremp":
				RESPAWN_RESTORE_MP = Double.parseDouble(pValue) / 100;
				break;
			case "maxpvtstoresellslotsdwarf":
				MAX_PVTSTORESELL_SLOTS_DWARF = Integer.parseInt(pValue);
				break;
			case "maxpvtstoresellslotsother":
				MAX_PVTSTORESELL_SLOTS_OTHER = Integer.parseInt(pValue);
				break;
			case "maxpvtstorebuyslotsdwarf":
				MAX_PVTSTOREBUY_SLOTS_DWARF = Integer.parseInt(pValue);
				break;
			case "maxpvtstorebuyslotsother":
				MAX_PVTSTOREBUY_SLOTS_OTHER = Integer.parseInt(pValue);
				break;
			case "storeskillcooltime":
				STORE_SKILL_COOLTIME = Boolean.parseBoolean(pValue);
				break;
			case "subclassstoreskillcooltime":
				SUBCLASS_STORE_SKILL_COOLTIME = Boolean.parseBoolean(pValue);
				break;
			case "announcemammonspawn":
				ANNOUNCE_MAMMON_SPAWN = Boolean.parseBoolean(pValue);
				break;
			case "altgametiredness":
				ALT_GAME_TIREDNESS = Boolean.parseBoolean(pValue);
				break;
			case "enablefallingdamage":
				ENABLE_FALLING_DAMAGE = Boolean.parseBoolean(pValue);
				break;
			case "altgamecreation":
				ALT_GAME_CREATION = Boolean.parseBoolean(pValue);
				break;
			case "altgamecreationspeed":
				ALT_GAME_CREATION_SPEED = Double.parseDouble(pValue);
				break;
			case "altgamecreationxprate":
				ALT_GAME_CREATION_XP_RATE = Double.parseDouble(pValue);
				break;
			case "altgamecreationrarexpsprate":
				ALT_GAME_CREATION_RARE_XPSP_RATE = Double.parseDouble(pValue);
				break;
			case "altgamecreationsprate":
				ALT_GAME_CREATION_SP_RATE = Double.parseDouble(pValue);
				break;
			case "altweightlimit":
				ALT_WEIGHT_LIMIT = Double.parseDouble(pValue);
				break;
			case "altblacksmithuserecipes":
				ALT_BLACKSMITH_USE_RECIPES = Boolean.parseBoolean(pValue);
				break;
			case "altgameskilllearn":
				ALT_GAME_SKILL_LEARN = Boolean.parseBoolean(pValue);
				break;
			case "removecastlecirclets":
				REMOVE_CASTLE_CIRCLETS = Boolean.parseBoolean(pValue);
				break;
			case "reputationscoreperkill":
				REPUTATION_SCORE_PER_KILL = Integer.parseInt(pValue);
				break;
			case "altgamecancelbyhit":
				ALT_GAME_CANCEL_BOW = pValue.equalsIgnoreCase("bow") || pValue.equalsIgnoreCase("all");
				ALT_GAME_CANCEL_CAST = pValue.equalsIgnoreCase("cast") || pValue.equalsIgnoreCase("all");
				break;
			case "altshieldblocks":
				ALT_GAME_SHIELD_BLOCKS = Boolean.parseBoolean(pValue);
				break;
			case "altperfectshieldblockrate":
				ALT_PERFECT_SHLD_BLOCK = Integer.parseInt(pValue);
				break;
			case "delevel":
				ALT_GAME_DELEVEL = Boolean.parseBoolean(pValue);
				break;
			case "magicfailures":
				ALT_GAME_MAGICFAILURES = Boolean.parseBoolean(pValue);
				break;
			case "altmobagroinpeacezone":
				ALT_MOB_AGRO_IN_PEACEZONE = Boolean.parseBoolean(pValue);
				break;
			case "altgameexponentxp":
				ALT_GAME_EXPONENT_XP = Float.parseFloat(pValue);
				break;
			case "altgameexponentsp":
				ALT_GAME_EXPONENT_SP = Float.parseFloat(pValue);
				break;
			case "allowclassmasters":
				ALLOW_CLASS_MASTERS = Boolean.parseBoolean(pValue);
				break;
			case "allowentiretree":
				ALLOW_ENTIRE_TREE = Boolean.parseBoolean(pValue);
				break;
			case "alternateclassmaster":
				ALTERNATE_CLASS_MASTER = Boolean.parseBoolean(pValue);
				break;
			case "altpartyrange":
				ALT_PARTY_RANGE = Integer.parseInt(pValue);
				break;
			case "altpartyrange2":
				ALT_PARTY_RANGE2 = Integer.parseInt(pValue);
				break;
			case "altleavepartyleader":
				ALT_LEAVE_PARTY_LEADER = Boolean.parseBoolean(pValue);
				break;
			case "craftingenabled":
				IS_CRAFTING_ENABLED = Boolean.parseBoolean(pValue);
				break;
			case "craftmasterwork":
				CRAFT_MASTERWORK = Boolean.parseBoolean(pValue);
				break;
			case "lifecrystalneeded":
				LIFE_CRYSTAL_NEEDED = Boolean.parseBoolean(pValue);
				break;
			case "autoloot":
				AUTO_LOOT = Boolean.parseBoolean(pValue);
				break;
			case "autolootraids":
				AUTO_LOOT_RAIDS = Boolean.parseBoolean(pValue);
				break;
			case "autolootherbs":
				AUTO_LOOT_HERBS = Boolean.parseBoolean(pValue);
				break;
			case "altkarmaplayercanbekilledinpeacezone":
				ALT_GAME_KARMA_PLAYER_CAN_BE_KILLED_IN_PEACEZONE = Boolean.parseBoolean(pValue);
				break;
			case "altkarmaplayercanshop":
				ALT_GAME_KARMA_PLAYER_CAN_SHOP = Boolean.parseBoolean(pValue);
				break;
			case "altkarmaplayercanusegk":
				ALT_GAME_KARMA_PLAYER_CAN_USE_GK = Boolean.parseBoolean(pValue);
				break;
			case "altkarmaplayercanteleport":
				ALT_GAME_KARMA_PLAYER_CAN_TELEPORT = Boolean.parseBoolean(pValue);
				break;
			case "altkarmaplayercantrade":
				ALT_GAME_KARMA_PLAYER_CAN_TRADE = Boolean.parseBoolean(pValue);
				break;
			case "altkarmaplayercanusewarehouse":
				ALT_GAME_KARMA_PLAYER_CAN_USE_WAREHOUSE = Boolean.parseBoolean(pValue);
				break;
			case "maxpersonalfamepoints":
				MAX_PERSONAL_FAME_POINTS = Integer.parseInt(pValue);
				break;
			case "fortresszonefametaskfrequency":
				FORTRESS_ZONE_FAME_TASK_FREQUENCY = Integer.parseInt(pValue);
				break;
			case "fortresszonefameaquirepoints":
				FORTRESS_ZONE_FAME_AQUIRE_POINTS = Integer.parseInt(pValue);
				break;
			case "castlezonefametaskfrequency":
				CASTLE_ZONE_FAME_TASK_FREQUENCY = Integer.parseInt(pValue);
				break;
			case "castlezonefameaquirepoints":
				CASTLE_ZONE_FAME_AQUIRE_POINTS = Integer.parseInt(pValue);
				break;
			case "altcastlefordawn":
				ALT_GAME_CASTLE_DAWN = Boolean.parseBoolean(pValue);
				break;
			case "altcastlefordusk":
				ALT_GAME_CASTLE_DUSK = Boolean.parseBoolean(pValue);
				break;
			case "altrequireclancastle":
				ALT_GAME_REQUIRE_CLAN_CASTLE = Boolean.parseBoolean(pValue);
				break;
			case "altfreeteleporting":
				ALT_GAME_FREE_TELEPORT = Boolean.parseBoolean(pValue);
				break;
			case "altsubclasswithoutquests":
				ALT_GAME_SUBCLASS_WITHOUT_QUESTS = Boolean.parseBoolean(pValue);
				break;
			case "altsubclasseverywhere":
				ALT_GAME_SUBCLASS_EVERYWHERE = Boolean.parseBoolean(pValue);
				break;
			case "altmemberscanwithdrawfromclanwh":
				ALT_MEMBERS_CAN_WITHDRAW_FROM_CLANWH = Boolean.parseBoolean(pValue);
				break;
			case "dwarfrecipelimit":
				DWARF_RECIPE_LIMIT = Integer.parseInt(pValue);
				break;
			case "commonrecipelimit":
				COMMON_RECIPE_LIMIT = Integer.parseInt(pValue);
				break;
			case "championenable":
				L2JMOD_CHAMPION_ENABLE = Boolean.parseBoolean(pValue);
				break;
			case "championfrequency":
				L2JMOD_CHAMPION_FREQUENCY = Integer.parseInt(pValue);
				break;
			case "championminlevel":
				L2JMOD_CHAMP_MIN_LVL = Integer.parseInt(pValue);
				break;
			case "championmaxlevel":
				L2JMOD_CHAMP_MAX_LVL = Integer.parseInt(pValue);
				break;
			case "championhp":
				L2JMOD_CHAMPION_HP = Integer.parseInt(pValue);
				break;
			case "championhpregen":
				L2JMOD_CHAMPION_HP_REGEN = Float.parseFloat(pValue);
				break;
			case "championrewards":
				L2JMOD_CHAMPION_REWARDS = Integer.parseInt(pValue);
				break;
			case "championadenasrewards":
				L2JMOD_CHAMPION_ADENAS_REWARDS = Float.parseFloat(pValue);
				break;
			case "championatk":
				L2JMOD_CHAMPION_ATK = Float.parseFloat(pValue);
				break;
			case "championspdatk":
				L2JMOD_CHAMPION_SPD_ATK = Float.parseFloat(pValue);
				break;
			case "championrewardlowerlvlitemchance":
				L2JMOD_CHAMPION_REWARD_LOWER_LVL_ITEM_CHANCE = Integer.parseInt(pValue);
				break;
			case "championrewardhigherlvlitemchance":
				L2JMOD_CHAMPION_REWARD_HIGHER_LVL_ITEM_CHANCE = Integer.parseInt(pValue);
				break;
			case "championrewarditemid":
				L2JMOD_CHAMPION_REWARD_ID = Integer.parseInt(pValue);
				break;
			case "championrewarditemqty":
				L2JMOD_CHAMPION_REWARD_QTY = Integer.parseInt(pValue);
				break;
			case "championenableininstances":
				L2JMOD_CHAMPION_ENABLE_IN_INSTANCES = Boolean.parseBoolean(pValue);
				break;
			case "allowwedding":
				L2JMOD_ALLOW_WEDDING = Boolean.parseBoolean(pValue);
				break;
			case "weddingprice":
				L2JMOD_WEDDING_PRICE = Integer.parseInt(pValue);
				break;
			case "weddingpunishinfidelity":
				L2JMOD_WEDDING_PUNISH_INFIDELITY = Boolean.parseBoolean(pValue);
				break;
			case "weddingteleport":
				L2JMOD_WEDDING_TELEPORT = Boolean.parseBoolean(pValue);
				break;
			case "weddingteleportprice":
				L2JMOD_WEDDING_TELEPORT_PRICE = Integer.parseInt(pValue);
				break;
			case "weddingteleportduration":
				L2JMOD_WEDDING_TELEPORT_DURATION = Integer.parseInt(pValue);
				break;
			case "weddingallowsamesex":
				L2JMOD_WEDDING_SAMESEX = Boolean.parseBoolean(pValue);
				break;
			case "weddingformalwear":
				L2JMOD_WEDDING_FORMALWEAR = Boolean.parseBoolean(pValue);
				break;
			case "weddingdivorcecosts":
				L2JMOD_WEDDING_DIVORCE_COSTS = Integer.parseInt(pValue);
				break;
			case "tvteventenabled":
				TVT_EVENT_ENABLED = Boolean.parseBoolean(pValue);
				break;
			case "tvteventinterval":
				TVT_EVENT_INTERVAL = pValue.split(",");
				break;
			case "tvteventparticipationtime":
				TVT_EVENT_PARTICIPATION_TIME = Integer.parseInt(pValue);
				break;
			case "tvteventrunningtime":
				TVT_EVENT_RUNNING_TIME = Integer.parseInt(pValue);
				break;
			case "tvteventparticipationnpcid":
				TVT_EVENT_PARTICIPATION_NPC_ID = Integer.parseInt(pValue);
				break;
			case "enablewarehousesortingclan":
				L2JMOD_ENABLE_WAREHOUSESORTING_CLAN = Boolean.parseBoolean(pValue);
				break;
			case "enablewarehousesortingprivate":
				L2JMOD_ENABLE_WAREHOUSESORTING_PRIVATE = Boolean.parseBoolean(pValue);
				break;
			case "enablemanapotionsupport":
				L2JMOD_ENABLE_MANA_POTIONS_SUPPORT = Boolean.parseBoolean(pValue);
				break;
			case "displayservertime":
				L2JMOD_DISPLAY_SERVER_TIME = Boolean.parseBoolean(pValue);
				break;
			case "antifeedenable":
				L2JMOD_ANTIFEED_ENABLE = Boolean.parseBoolean(pValue);
				break;
			case "antifeeddualbox":
				L2JMOD_ANTIFEED_DUALBOX = Boolean.parseBoolean(pValue);
				break;
			case "antifeeddisconnectedasdualbox":
				L2JMOD_ANTIFEED_DISCONNECTED_AS_DUALBOX = Boolean.parseBoolean(pValue);
				break;
			case "antifeedinterval":
				L2JMOD_ANTIFEED_INTERVAL = 1000 * Integer.parseInt(pValue);
				break;
			case "minkarma":
				KARMA_MIN_KARMA = Integer.parseInt(pValue);
				break;
			case "maxkarma":
				KARMA_MAX_KARMA = Integer.parseInt(pValue);
				break;
			case "xpdivider":
				KARMA_XP_DIVIDER = Integer.parseInt(pValue);
				break;
			case "basekarmalost":
				KARMA_LOST_BASE = Integer.parseInt(pValue);
				break;
			case "cangmdropequipment":
				KARMA_DROP_GM = Boolean.parseBoolean(pValue);
				break;
			case "awardpkkillpvppoint":
				KARMA_AWARD_PK_KILL = Boolean.parseBoolean(pValue);
				break;
			case "minimumpkrequiredtodrop":
				KARMA_PK_LIMIT = Integer.parseInt(pValue);
				break;
			case "pvpvsnormaltime":
				PVP_NORMAL_TIME = Integer.parseInt(pValue);
				break;
			case "pvpvspvptime":
				PVP_PVP_TIME = Integer.parseInt(pValue);
				break;
			case "globalchat":
				DEFAULT_GLOBAL_CHAT = pValue;
				break;
			case "tradechat":
				DEFAULT_TRADE_CHAT = pValue;
				break;
			case "gmadminmenustyle":
				GM_ADMIN_MENU_STYLE = pValue;
				break;
			default:
				try
				{
					// TODO: stupid GB configs...
					if (!pName.startsWith("Interval_") && !pName.startsWith("Random_"))
					{
						pName = pName.toUpperCase();
					}
					Field clazField = Config.class.getField(pName);
					int modifiers = clazField.getModifiers();
					// just in case :)
					if (!Modifier.isStatic(modifiers) || !Modifier.isPublic(modifiers) || Modifier.isFinal(modifiers))
					{
						throw new SecurityException("Cannot modify non public, non static or final config!");
					}
					
					if (clazField.getType() == int.class)
					{
						clazField.setInt(clazField, Integer.parseInt(pValue));
					}
					else if (clazField.getType() == short.class)
					{
						clazField.setShort(clazField, Short.parseShort(pValue));
					}
					else if (clazField.getType() == byte.class)
					{
						clazField.setByte(clazField, Byte.parseByte(pValue));
					}
					else if (clazField.getType() == long.class)
					{
						clazField.setLong(clazField, Long.parseLong(pValue));
					}
					else if (clazField.getType() == float.class)
					{
						clazField.setFloat(clazField, Float.parseFloat(pValue));
					}
					else if (clazField.getType() == double.class)
					{
						clazField.setDouble(clazField, Double.parseDouble(pValue));
					}
					else if (clazField.getType() == boolean.class)
					{
						clazField.setBoolean(clazField, Boolean.parseBoolean(pValue));
					}
					else if (clazField.getType() == String.class)
					{
						clazField.set(clazField, pValue);
					}
					else
					{
						return false;
					}
				}
				catch (NoSuchFieldException e)
				{
					return false;
				}
				catch (Exception e)
				{
					_log.log(Level.WARNING, "", e);
					return false;
				}
		}
		return true;
	}
	
	/**
	 * Save hexadecimal ID of the server in the L2Properties file.<br>
	 * Check {@link #HEXID_FILE}.
	 * @param serverId the ID of the server whose hexId to save
	 * @param hexId the hexadecimal ID to store
	 */
	public static void saveHexid(int serverId, String hexId)
	{
		Config.saveHexid(serverId, hexId, HEXID_FILE);
	}
	
	/**
	 * Save hexadecimal ID of the server in the L2Properties file.
	 * @param serverId the ID of the server whose hexId to save
	 * @param hexId the hexadecimal ID to store
	 * @param fileName name of the L2Properties file
	 */
	public static void saveHexid(int serverId, String hexId, String fileName)
	{
		try
		{
			L2Properties hexSetting = new L2Properties();
			File file = new File(fileName);
			// Create a new empty file only if it doesn't exist
			file.createNewFile();
			try (OutputStream out = new FileOutputStream(file))
			{
				hexSetting.setProperty("ServerID", String.valueOf(serverId));
				hexSetting.setProperty("HexID", hexId);
				hexSetting.store(out, "the hexID to auth into login");
			}
		}
		catch (Exception e)
		{
			_log.warning(StringUtil.concat("Failed to save hex id to ", fileName, " File."));
			_log.warning("Config: " + e.getMessage());
		}
	}
	
	/**
	 * Loads flood protector configurations.
	 * @param properties the properties object containing the actual values of the flood protector configs
	 */
	private static void loadFloodProtectorConfigs(final L2Properties properties)
	{
		loadFloodProtectorConfig(properties, FLOOD_PROTECTOR_USE_ITEM, "UseItem", "4");
		loadFloodProtectorConfig(properties, FLOOD_PROTECTOR_ROLL_DICE, "RollDice", "42");
		loadFloodProtectorConfig(properties, FLOOD_PROTECTOR_FIREWORK, "Firework", "42");
		loadFloodProtectorConfig(properties, FLOOD_PROTECTOR_ITEM_PET_SUMMON, "ItemPetSummon", "16");
		loadFloodProtectorConfig(properties, FLOOD_PROTECTOR_HERO_VOICE, "HeroVoice", "100");
		loadFloodProtectorConfig(properties, FLOOD_PROTECTOR_GLOBAL_CHAT, "GlobalChat", "5");
		loadFloodProtectorConfig(properties, FLOOD_PROTECTOR_SUBCLASS, "Subclass", "20");
		loadFloodProtectorConfig(properties, FLOOD_PROTECTOR_DROP_ITEM, "DropItem", "10");
		loadFloodProtectorConfig(properties, FLOOD_PROTECTOR_SERVER_BYPASS, "ServerBypass", "5");
		loadFloodProtectorConfig(properties, FLOOD_PROTECTOR_MULTISELL, "MultiSell", "1");
		loadFloodProtectorConfig(properties, FLOOD_PROTECTOR_TRANSACTION, "Transaction", "10");
		loadFloodProtectorConfig(properties, FLOOD_PROTECTOR_MANUFACTURE, "Manufacture", "3");
		loadFloodProtectorConfig(properties, FLOOD_PROTECTOR_MANOR, "Manor", "30");
		loadFloodProtectorConfig(properties, FLOOD_PROTECTOR_SENDMAIL, "SendMail", "100");
		loadFloodProtectorConfig(properties, FLOOD_PROTECTOR_CHARACTER_SELECT, "CharacterSelect", "30");
		loadFloodProtectorConfig(properties, FLOOD_PROTECTOR_ITEM_AUCTION, "ItemAuction", "9");
	}
	
	/**
	 * Loads single flood protector configuration.
	 * @param properties properties file reader
	 * @param config flood protector configuration instance
	 * @param configString flood protector configuration string that determines for which flood protector configuration should be read
	 * @param defaultInterval default flood protector interval
	 */
	private static void loadFloodProtectorConfig(final L2Properties properties, final FloodProtectorConfig config, final String configString, final String defaultInterval)
	{
		config.FLOOD_PROTECTION_INTERVAL = Integer.parseInt(properties.getProperty(StringUtil.concat("FloodProtector", configString, "Interval"), defaultInterval));
		config.LOG_FLOODING = Boolean.parseBoolean(properties.getProperty(StringUtil.concat("FloodProtector", configString, "LogFlooding"), "False"));
		config.PUNISHMENT_LIMIT = Integer.parseInt(properties.getProperty(StringUtil.concat("FloodProtector", configString, "PunishmentLimit"), "0"));
		config.PUNISHMENT_TYPE = properties.getProperty(StringUtil.concat("FloodProtector", configString, "PunishmentType"), "none");
		config.PUNISHMENT_TIME = Integer.parseInt(properties.getProperty(StringUtil.concat("FloodProtector", configString, "PunishmentTime"), "0"));
	}
	
	public static int getServerTypeId(String[] serverTypes)
	{
		int tType = 0;
		for (String cType : serverTypes)
		{
			switch (cType.trim().toLowerCase())
			{
				case "Normal":
					tType |= 0x01;
					break;
				case "Relax":
					tType |= 0x02;
					break;
				case "Test":
					tType |= 0x04;
					break;
				case "NoLabel":
					tType |= 0x08;
					break;
				case "Restricted":
					tType |= 0x10;
					break;
				case "Event":
					tType |= 0x20;
					break;
				case "Free":
					tType |= 0x40;
					break;
				default:
					break;
			}
		}
		return tType;
	}
	
	public static class ClassMasterSettings
	{
		private final Map<Integer, Map<Integer, Integer>> _claimItems;
		private final Map<Integer, Map<Integer, Integer>> _rewardItems;
		private final Map<Integer, Boolean> _allowedClassChange;
		
		public ClassMasterSettings(String _configLine)
		{
			_claimItems = new HashMap<>(3);
			_rewardItems = new HashMap<>(3);
			_allowedClassChange = new HashMap<>(3);
			if (_configLine != null)
			{
				parseConfigLine(_configLine.trim());
			}
		}
		
		private void parseConfigLine(String _configLine)
		{
			StringTokenizer st = new StringTokenizer(_configLine, ";");
			
			while (st.hasMoreTokens())
			{
				// get allowed class change
				int job = Integer.parseInt(st.nextToken());
				
				_allowedClassChange.put(job, true);
				
				Map<Integer, Integer> _items = new HashMap<>();
				// parse items needed for class change
				if (st.hasMoreTokens())
				{
					StringTokenizer st2 = new StringTokenizer(st.nextToken(), "[],");
					
					while (st2.hasMoreTokens())
					{
						StringTokenizer st3 = new StringTokenizer(st2.nextToken(), "()");
						int _itemId = Integer.parseInt(st3.nextToken());
						int _quantity = Integer.parseInt(st3.nextToken());
						_items.put(_itemId, _quantity);
					}
				}
				
				_claimItems.put(job, _items);
				
				_items = new HashMap<>();
				// parse gifts after class change
				if (st.hasMoreTokens())
				{
					StringTokenizer st2 = new StringTokenizer(st.nextToken(), "[],");
					
					while (st2.hasMoreTokens())
					{
						StringTokenizer st3 = new StringTokenizer(st2.nextToken(), "()");
						int _itemId = Integer.parseInt(st3.nextToken());
						int _quantity = Integer.parseInt(st3.nextToken());
						_items.put(_itemId, _quantity);
					}
				}
				
				_rewardItems.put(job, _items);
			}
		}
		
		public boolean isAllowed(int job)
		{
			if (_allowedClassChange == null)
			{
				return false;
			}
			if (_allowedClassChange.containsKey(job))
			{
				return _allowedClassChange.get(job);
			}
			
			return false;
		}
		
		public Map<Integer, Integer> getRewardItems(int job)
		{
			if (_rewardItems.containsKey(job))
			{
				return _rewardItems.get(job);
			}
			
			return null;
		}
		
		public Map<Integer, Integer> getRequireItems(int job)
		{
			if (_claimItems.containsKey(job))
			{
				return _claimItems.get(job);
			}
			
			return null;
		}
	}
	
	/**
	 * @param line the string line to parse
	 * @return a parsed float map
	 */
	private static Map<Integer, Float> parseConfigLine(String line)
	{
		String[] propertySplit = line.split(",");
		Map<Integer, Float> ret = new HashMap<>(propertySplit.length);
		int i = 0;
		for (String value : propertySplit)
		{
			ret.put(i++, Float.parseFloat(value));
		}
		return ret;
	}
	
	/**
	 * Parse a config value from its string representation to a two-dimensional int array.<br>
	 * The format of the value to be parsed should be as follows: "item1Id,item1Amount;item2Id,item2Amount;...itemNId,itemNAmount".
	 * @param line the value of the parameter to parse
	 * @return the parsed list or {@code null} if nothing was parsed
	 */
	private static int[][] parseItemsList(String line)
	{
		final String[] propertySplit = line.split(";");
		if (propertySplit.length == 0)
		{
			// nothing to do here
			return null;
		}
		
		int i = 0;
		String[] valueSplit;
		final int[][] result = new int[propertySplit.length][];
		int[] tmp;
		for (String value : propertySplit)
		{
			valueSplit = value.split(",");
			if (valueSplit.length != 2)
			{
				_log.warning(StringUtil.concat("parseItemsList[Config.load()]: invalid entry -> \"", valueSplit[0], "\", should be itemId,itemNumber. Skipping to the next entry in the list."));
				continue;
			}
			
			tmp = new int[2];
			try
			{
				tmp[0] = Integer.parseInt(valueSplit[0]);
			}
			catch (NumberFormatException e)
			{
				_log.warning(StringUtil.concat("parseItemsList[Config.load()]: invalid itemId -> \"", valueSplit[0], "\", value must be an integer. Skipping to the next entry in the list."));
				continue;
			}
			try
			{
				tmp[1] = Integer.parseInt(valueSplit[1]);
			}
			catch (NumberFormatException e)
			{
				_log.warning(StringUtil.concat("parseItemsList[Config.load()]: invalid item number -> \"", valueSplit[1], "\", value must be an integer. Skipping to the next entry in the list."));
				continue;
			}
			result[i++] = tmp;
		}
		return result;
	}
	
	private static class IPConfigData extends DocumentParser
	{
		private static final List<String> _subnets = new ArrayList<>(5);
		private static final List<String> _hosts = new ArrayList<>(5);
		
		public IPConfigData()
		{
			load();
		}
		
		@Override
		public void load()
		{
			File f = new File(IP_CONFIG_FILE);
			if (f.exists())
			{
				_log.log(Level.INFO, "Network Config: ipconfig.xml exists using manual configuration...");
				parseFile(new File(IP_CONFIG_FILE));
			}
			else
			// Auto configuration...
			{
				_log.log(Level.INFO, "Network Config: ipconfig.xml doesn't exists using automatic configuration...");
				autoIpConfig();
			}
		}
		
		@Override
		protected void parseDocument()
		{
			NamedNodeMap attrs;
			for (Node n = getCurrentDocument().getFirstChild(); n != null; n = n.getNextSibling())
			{
				if ("gameserver".equalsIgnoreCase(n.getNodeName()))
				{
					for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					{
						if ("define".equalsIgnoreCase(d.getNodeName()))
						{
							attrs = d.getAttributes();
							_subnets.add(attrs.getNamedItem("subnet").getNodeValue());
							_hosts.add(attrs.getNamedItem("address").getNodeValue());
							
							if (_hosts.size() != _subnets.size())
							{
								_log.log(Level.WARNING, "Failed to Load " + IP_CONFIG_FILE + " File - subnets does not match server addresses.");
							}
						}
					}
					
					Node att = n.getAttributes().getNamedItem("address");
					if (att == null)
					{
						_log.log(Level.WARNING, "Failed to load " + IP_CONFIG_FILE + " file - default server address is missing.");
						_hosts.add("127.0.0.1");
					}
					else
					{
						_hosts.add(att.getNodeValue());
					}
					_subnets.add("0.0.0.0/0");
				}
			}
		}
		
		protected void autoIpConfig()
		{
			String externalIp = "127.0.0.1";
			try
			{
				URL autoIp = new URL("http://api.externalip.net/ip/");
				try (BufferedReader in = new BufferedReader(new InputStreamReader(autoIp.openStream())))
				{
					externalIp = in.readLine();
				}
			}
			catch (IOException e)
			{
				_log.log(Level.INFO, "Network Config: Failed to connect to api.externalip.net please check your internet connection using 127.0.0.1!");
				externalIp = "127.0.0.1";
			}
			
			try
			{
				Enumeration<NetworkInterface> niList = NetworkInterface.getNetworkInterfaces();
				
				Subnet sub = new Subnet();
				while (niList.hasMoreElements())
				{
					NetworkInterface ni = niList.nextElement();
					
					if (!ni.isUp() || ni.isVirtual())
					{
						continue;
					}
					
					if (!ni.isLoopback() && (ni.getHardwareAddress().length != 6))
					{
						continue;
					}
					
					for (InterfaceAddress ia : ni.getInterfaceAddresses())
					{
						if (ia.getAddress() instanceof Inet6Address)
						{
							continue;
						}
						
						sub.setIPAddress(ia.getAddress().getHostAddress());
						sub.setMaskedBits(ia.getNetworkPrefixLength());
						String subnet = sub.getSubnetAddress() + '/' + sub.getMaskedBits();
						if (!_subnets.contains(subnet) && !subnet.equals("0.0.0.0/0"))
						{
							_subnets.add(subnet);
							_hosts.add(sub.getIPAddress());
							_log.log(Level.INFO, "Network Config: Adding new subnet: " + subnet + " address: " + sub.getIPAddress());
						}
					}
				}
				
				// External host and subnet
				_hosts.add(externalIp);
				_subnets.add("0.0.0.0/0");
				_log.log(Level.INFO, "Network Config: Adding new subnet: 0.0.0.0/0 address: " + externalIp);
			}
			catch (SocketException e)
			{
				_log.log(Level.INFO, "Network Config: Configuration failed please configure manually using ipconfig.xml", e);
				System.exit(0);
			}
		}
		
		protected List<String> getSubnets()
		{
			if (_subnets.isEmpty())
			{
				return Arrays.asList("0.0.0.0/0");
			}
			return _subnets;
		}
		
		protected List<String> getHosts()
		{
			if (_hosts.isEmpty())
			{
				return Arrays.asList("127.0.0.1");
			}
			return _hosts;
		}
	}
}
