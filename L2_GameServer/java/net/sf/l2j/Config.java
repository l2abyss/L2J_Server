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
package net.sf.l2j;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;

/**
 * This class contains global server configuration.<br>
 * It has static final fields initialized from configuration files.<br>
 * It's initialized at the very begin of startup, and later JIT will optimize
 * away debug/unused code.
 *
 * @author mkizub
 */
public final class Config
{
	protected static final Logger _log = Logger.getLogger(Config.class.getName());
	
	/******************************************************************************************************************************************************/
    /** L2J Property File Definitions Start Here**/
	/******************************************************************************************************************************************************/
	
    /** Properties file for Character Configurations */
    public static final String  CHARACTER_CONFIG_FILE							= "./config/Character.properties";
    /** Properties file for Dynamic Extensions*/
    public static final String  EXTENSIONS_CONFIG_FILE							= "./config/extensions.properties";
    /** Properties file for Feature Extensions*/
    public static final String  FEATURE_CONFIG_FILE								= "./config/Feature.properties";
    /** Properties file for Fortress Siege */
    public static final String FORTSIEGE_CONFIGURATION_FILE						= "./config/fortsiege.properties"; 
    /** Properties file for General Configurations */
    public static final String  GENERAL_CONFIG_FILE								= "./config/General.properties";
    /** Text file containing hexadecimal value of server ID */
    public static final String  HEXID_FILE										= "./config/hexid.txt";
    /** Properties file for the ID factory */
    public static final String  ID_CONFIG_FILE									= "./config/idfactory.properties";
    /** Properties file for l2j server version configurations */
    public static final String  SERVER_VERSION_FILE								= "./config/l2j-version.properties";
    /** Properties file for l2j datapack version configurations */
    public static final String  DATAPACK_VERSION_FILE							= "./config/l2jdp-version.properties";
    /** Properties file for the L2J Mod Confirugrations */
    public static final String  L2JMOD_CONFIG_FILE								= "./config/l2jmods.properties";
    /** Properties file for login server configurations */
    public static final String  LOGIN_CONFIGURATION_FILE						= "./config/loginserver.properties";
    /** Properties file for NPC Configurations */
    public static final String  NPC_CONFIG_FILE									= "./config/NPC.properties";
    /** Properties file for PVP Configurations */
    public static final String  PVP_CONFIG_FILE									= "./config/pvp.properties";
    /** Properties file for rates Configurations */
    public static final String  RATES_CONFIG_FILE								= "./config/rates.properties";
    /** Properties file for game server (connection and ingame) configurations */
    public static final String  CONFIGURATION_FILE								= "./config/server.properties";
    /** Properties file for siege Configurations */
    public static final String  SIEGE_CONFIGURATION_FILE						= "./config/siege.properties";
    /** Properties file for telnet configuration */
    public static final String  TELNET_FILE										= "./config/telnet.properties";

    /******************************************************************************************************************************************************/
    /** L2J Property File Definitions End Here**/
	/******************************************************************************************************************************************************/
	
	/******************************************************************************************************************************************************/
    /** L2J Variable Definitions Start Here**/
	/******************************************************************************************************************************************************/
	
	/** ************************************************** **/
	/** Character Settings -Begin                          **/
	/** ************************************************** **/
	
    public static int		MASTERACCESS_LEVEL;
    public static int		MASTERACCESS_NAME_COLOR;
    public static int		MASTERACCESS_TITLE_COLOR;
	public static boolean	ALT_GAME_DELEVEL;
    public static double	ALT_WEIGHT_LIMIT;
    public static int		RUN_SPD_BOOST;
    public static int		DEATH_PENALTY_CHANCE;
    public static double	RESPAWN_RESTORE_CP;
    public static double	RESPAWN_RESTORE_HP;
    public static double	RESPAWN_RESTORE_MP;
    public static boolean	ALT_GAME_TIREDNESS;
    public static boolean	ENABLE_MODIFY_SKILL_DURATION;
    public static Map<Integer, Integer> SKILL_DURATION_LIST;
    public static boolean   ENABLE_MODIFY_SKILL_REUSE;
    public static Map<Integer, Integer> SKILL_REUSE_LIST;
    public static boolean	AUTO_LEARN_SKILLS;
    public static boolean	AUTO_LOOT_HERBS;
    public static byte		BUFFS_MAX_AMOUNT;
    public static boolean	AUTO_LEARN_DIVINE_INSPIRATION;
    public static boolean	ALT_GAME_CANCEL_BOW;
    public static boolean	ALT_GAME_CANCEL_CAST;
    public static boolean	EFFECT_CANCELING;
    public static boolean	ALT_GAME_MAGICFAILURES;
    public static int		PLAYER_FAKEDEATH_UP_PROTECTION;
    public static boolean	STORE_SKILL_COOLTIME;
    public static boolean	ALT_GAME_SHIELD_BLOCKS;
    public static int		ALT_PERFECT_SHLD_BLOCK;
    public static boolean	ALLOW_CLASS_MASTERS;
    public static boolean	LIFE_CRYSTAL_NEEDED;
    public static boolean	SP_BOOK_NEEDED;
    public static boolean	ES_SP_BOOK_NEEDED;
    public static boolean	ALT_GAME_SKILL_LEARN;
    public static boolean	ALT_GAME_SUBCLASS_WITHOUT_QUESTS;
    public static int		MAX_RUN_SPEED;
    public static int		MAX_PCRIT_RATE;
    public static int		MAX_MCRIT_RATE;
    public static byte		MAX_SUBCLASS;
    public static int		MAX_PVTSTORESELL_SLOTS_DWARF;
    public static int		MAX_PVTSTORESELL_SLOTS_OTHER;
    public static int		MAX_PVTSTOREBUY_SLOTS_DWARF;
    public static int		MAX_PVTSTOREBUY_SLOTS_OTHER;
    public static int		INVENTORY_MAXIMUM_NO_DWARF;
    public static int		INVENTORY_MAXIMUM_DWARF;
    public static int		INVENTORY_MAXIMUM_GM;
    public static int		WAREHOUSE_SLOTS_DWARF;
    public static int		WAREHOUSE_SLOTS_NO_DWARF;
    public static int		WAREHOUSE_SLOTS_CLAN;
    public static int		FREIGHT_SLOTS;
    public static boolean	CASTLE_SHIELD;
    public static boolean	CLANHALL_SHIELD;
    public static boolean	APELLA_ARMORS;
    public static boolean	OATH_ARMORS;
    public static boolean	CASTLE_CROWN;
    public static boolean	CASTLE_CIRCLETS;
    public static boolean 	ALT_GAME_KARMA_PLAYER_CAN_BE_KILLED_IN_PEACEZONE;
    public static boolean 	ALT_GAME_KARMA_PLAYER_CAN_SHOP;
    public static boolean 	ALT_GAME_KARMA_PLAYER_CAN_TELEPORT;
    public static boolean 	ALT_GAME_KARMA_PLAYER_CAN_USE_GK;
    public static boolean 	ALT_GAME_KARMA_PLAYER_CAN_TRADE;
    public static boolean 	ALT_GAME_KARMA_PLAYER_CAN_USE_WAREHOUSE;
    public static boolean	IS_CRAFTING_ENABLED;
    public static int		DWARF_RECIPE_LIMIT;
    public static int		COMMON_RECIPE_LIMIT;
    public static boolean	ALT_GAME_CREATION;
    public static double	ALT_GAME_CREATION_SPEED;
    public static double	ALT_GAME_CREATION_XP_RATE;
    public static double	ALT_GAME_CREATION_SP_RATE;
    public static boolean	ALT_BLACKSMITH_USE_RECIPES;
    public static int		ALT_CLAN_JOIN_DAYS;
    public static int		ALT_CLAN_CREATE_DAYS;
    public static int		ALT_CLAN_DISSOLVE_DAYS;
    public static int		ALT_ALLY_JOIN_DAYS_WHEN_LEAVED;
    public static int		ALT_ALLY_JOIN_DAYS_WHEN_DISMISSED;
    public static int		ALT_ACCEPT_CLAN_DAYS_WHEN_DISMISSED;
    public static int		ALT_CREATE_ALLY_DAYS_WHEN_DISSOLVED;
    public static int		ALT_MAX_NUM_OF_CLANS_IN_ALLY;
    public static int		ALT_CLAN_MEMBERS_FOR_WAR;
    public static boolean	ALT_GAME_NEW_CHAR_ALWAYS_IS_NEWBIE;
    public static boolean	ALT_MEMBERS_CAN_WITHDRAW_FROM_CLANWH;
    public static boolean	REMOVE_CASTLE_CIRCLETS;
    public static int		ALT_PARTY_RANGE;
    public static int		ALT_PARTY_RANGE2;
    public static int		STARTING_ADENA;
    public static boolean	AUTO_LOOT;
    public static int		UNSTUCK_INTERVAL;
    public static int		PLAYER_SPAWN_PROTECTION;
    public static boolean	RESPAWN_RANDOM_ENABLED;
    public static int		RESPAWN_RANDOM_MAX_OFFSET;
    public static boolean	PETITIONING_ALLOWED;
    public static int		MAX_PETITIONS_PER_PLAYER;
    public static int		MAX_PETITIONS_PENDING;
    public static boolean	ALT_GAME_FREIGHTS;
    public static int		ALT_GAME_FREIGHT_PRICE;
    public static boolean	ALT_GAME_FREE_TELEPORT;
    public static boolean	ALT_RECOMMEND;
    public static int		DELETE_DAYS;
    public static float		ALT_GAME_EXPONENT_XP;
    public static float		ALT_GAME_EXPONENT_SP;
    public static String	PARTY_XP_CUTOFF_METHOD;
    public static double	PARTY_XP_CUTOFF_PERCENT;
    public static int		PARTY_XP_CUTOFF_LEVEL;
	
	/** ************************************************** **/
	/** Character Settings -End                            **/
	/** ************************************************** **/
    
    /** ************************************************** **/
	/** ClanHall Settings -Begin                           **/
	/** ************************************************** **/
    
    public static long 		CH_TELE_FEE_RATIO;
    public static int 		CH_TELE1_FEE;
    public static int 		CH_TELE2_FEE;
    public static long 		CH_ITEM_FEE_RATIO;
    public static int 		CH_ITEM1_FEE;
    public static int 		CH_ITEM2_FEE;
    public static int 		CH_ITEM3_FEE;
    public static long 		CH_MPREG_FEE_RATIO;
    public static int 		CH_MPREG1_FEE;
    public static int 		CH_MPREG2_FEE;
    public static int 		CH_MPREG3_FEE;
    public static int 		CH_MPREG4_FEE;
    public static int 		CH_MPREG5_FEE;
    public static long 		CH_HPREG_FEE_RATIO;
    public static int 		CH_HPREG1_FEE;
    public static int 		CH_HPREG2_FEE;
    public static int 		CH_HPREG3_FEE;
    public static int 		CH_HPREG4_FEE;
    public static int 		CH_HPREG5_FEE;
    public static int 		CH_HPREG6_FEE;
    public static int 		CH_HPREG7_FEE;
    public static int 		CH_HPREG8_FEE;
    public static int 		CH_HPREG9_FEE;
    public static int 		CH_HPREG10_FEE;
    public static int 		CH_HPREG11_FEE;
    public static int 		CH_HPREG12_FEE;
    public static int 		CH_HPREG13_FEE;
    public static long 		CH_EXPREG_FEE_RATIO;
    public static int 		CH_EXPREG1_FEE;
    public static int 		CH_EXPREG2_FEE;
    public static int 		CH_EXPREG3_FEE;
    public static int 		CH_EXPREG4_FEE;
    public static int 		CH_EXPREG5_FEE;
    public static int 		CH_EXPREG6_FEE;
    public static int 		CH_EXPREG7_FEE;
    public static long 		CH_SUPPORT_FEE_RATIO;
    public static int 		CH_SUPPORT1_FEE;
    public static int 		CH_SUPPORT2_FEE;
    public static int 		CH_SUPPORT3_FEE;
    public static int 		CH_SUPPORT4_FEE;
    public static int 		CH_SUPPORT5_FEE;
    public static int 		CH_SUPPORT6_FEE;
    public static int 		CH_SUPPORT7_FEE;
    public static int 		CH_SUPPORT8_FEE;
    public static long 		CH_CURTAIN_FEE_RATIO;
    public static int 		CH_CURTAIN1_FEE;
    public static int 		CH_CURTAIN2_FEE;
    public static long 		CH_FRONT_FEE_RATIO;
    public static int 		CH_FRONT1_FEE;
    public static int 		CH_FRONT2_FEE;
    
    /** ************************************************** **/
	/** ClanHall Settings -End                             **/
	/** ************************************************** **/

    /** ************************************************** **/
	/** Castle Settings -Begin                           **/
	/** ************************************************** **/

    public static long 		CS_TELE_FEE_RATIO;
    public static int 		CS_TELE1_FEE;
    public static int 		CS_TELE2_FEE;
    public static long 		CS_MPREG_FEE_RATIO;
    public static int 		CS_MPREG1_FEE;
    public static int 		CS_MPREG2_FEE;
    public static int 		CS_MPREG3_FEE;
    public static int 		CS_MPREG4_FEE;
    public static long 		CS_HPREG_FEE_RATIO;
    public static int 		CS_HPREG1_FEE;
    public static int 		CS_HPREG2_FEE;
    public static int 		CS_HPREG3_FEE;
    public static int 		CS_HPREG4_FEE;
    public static int 		CS_HPREG5_FEE;
    public static long 		CS_EXPREG_FEE_RATIO;
    public static int 		CS_EXPREG1_FEE;
    public static int 		CS_EXPREG2_FEE;
    public static int 		CS_EXPREG3_FEE;
    public static int 		CS_EXPREG4_FEE;
    public static long 		CS_SUPPORT_FEE_RATIO;
    public static int 		CS_SUPPORT1_FEE;
    public static int 		CS_SUPPORT2_FEE;
    public static int 		CS_SUPPORT3_FEE;
    public static int 		CS_SUPPORT4_FEE;
    
    /** ************************************************** **/
	/** Castlel Settings -End                             **/
	/** ************************************************** **/

    /** ************************************************** **/
	/** Feature Settings -Begin                            **/
	/** ************************************************** **/
    
    // TODO: Fill this area up!
    
    /** ************************************************** **/
	/** Feature Settings -End                              **/
	/** ************************************************** **/
    
    /** ************************************************** **/
	/** General Settings -Begin                            **/
	/** ************************************************** **/
	
	public static boolean	EVERYBODY_HAS_ADMIN_RIGHTS;
	public static boolean 	SERVER_LIST_BRACKET;
    public static boolean	SERVER_LIST_CLOCK;
    public static boolean	SERVER_GMONLY;
    public static boolean	GM_HERO_AURA;
    public static boolean	GM_STARTUP_INVULNERABLE;
    public static boolean	GM_STARTUP_INVISIBLE;
    public static boolean	GM_STARTUP_SILENCE;
    public static boolean	GM_STARTUP_AUTO_LIST;
    public static String	GM_ADMIN_MENU_STYLE;
    public static boolean	BYPASS_VALIDATION;
    public static boolean	GAMEGUARD_ENFORCE;
    public static boolean	GAMEGUARD_PROHIBITACTION;
    public static boolean	LOG_CHAT;
    public static boolean	LOG_ITEMS;
    public static boolean	GMAUDIT;
    public static boolean	DEBUG;
    public static boolean	ASSERT;
    public static boolean	DEVELOPER;
    public static boolean	ACCEPT_GEOEDITOR_CONN;
    public static boolean	TEST_SERVER;
    public static boolean	ALT_DEV_NO_QUESTS;
    public static boolean	ALT_DEV_NO_SPAWNS;
    public static boolean	SERVER_LIST_TESTSERVER;
    public static int		THREAD_P_EFFECTS;
    public static int		THREAD_P_GENERAL;
    public static int		GENERAL_PACKET_THREAD_CORE_SIZE;
    public static int		IO_PACKET_THREAD_CORE_SIZE;
    public static int		GENERAL_THREAD_CORE_SIZE;
    public static int		AI_MAX_THREAD;
    public static int		PACKET_LIFETIME;
    public static int		FLOODPROTECTOR_INITIALSIZE;
    public static boolean	ALLOW_DISCARDITEM;
    public static int		AUTODESTROY_ITEM_AFTER;
    public static int		HERB_AUTO_DESTROY_TIME;
    public static String	PROTECTED_ITEMS;
    public static List<Integer> LIST_PROTECTED_ITEMS = new FastList<Integer>();
    public static int		CHAR_STORE_INTERVAL;
    public static boolean	LAZY_ITEMS_UPDATE;
    public static boolean	UPDATE_ITEMS_ON_CHAR_STORE;
    public static boolean	DESTROY_DROPPED_PLAYER_ITEM;
    public static boolean	DESTROY_EQUIPABLE_PLAYER_ITEM;
    public static boolean	SAVE_DROPPED_ITEM;
    public static boolean	EMPTY_DROPPED_ITEM_TABLE_AFTER_LOAD;
    public static int		SAVE_DROPPED_ITEM_INTERVAL;
    public static boolean	CLEAR_DROPPED_ITEM_TABLE;
    public static boolean	AUTODELETE_INVALID_QUEST_DATA;
    public static boolean	PRECISE_DROP_CALCULATION;
    public static boolean	MULTIPLE_ITEM_DROP;
    public static boolean	FORCE_INVENTORY_UPDATE;
    public static boolean	LAZY_CACHE;
    public static int		MIN_NPC_ANIMATION;
    public static int		MAX_NPC_ANIMATION;
    public static int		MIN_MONSTER_ANIMATION;
    public static int		MAX_MONSTER_ANIMATION;
    public static int		COORD_SYNCHRONIZE;
    public static boolean	GRIDS_ALWAYS_ON;
    public static int		GRID_NEIGHBOR_TURNON_TIME;
    public static int		GRID_NEIGHBOR_TURNOFF_TIME;
    public static int		GEODATA;
    public static boolean	FORCE_GEODATA;
    public static boolean 	MOVE_BASED_KNOWNLIST;
    public static int		ZONE_TOWN;
    public static boolean	ACTIVATE_POSITION_RECORDER;
    public static String	DEFAULT_GLOBAL_CHAT;
    public static String	DEFAULT_TRADE_CHAT;
    public static boolean	ALLOW_WAREHOUSE;
    public static boolean	WAREHOUSE_CACHE;
    public static int		WAREHOUSE_CACHE_TIME;
    public static boolean	ALLOW_FREIGHT;
    public static boolean	ALLOW_WEAR;
    public static int		WEAR_DELAY;
    public static int		WEAR_PRICE;
    public static boolean	ALLOW_LOTTERY;
    public static boolean	ALLOW_RACE;
    public static boolean	ALLOW_WATER;
    public static boolean	ALLOW_RENTPET;
    public static boolean	ALLOWFISHING;
    public static boolean	ALLOW_BOAT;
    public static boolean	ALLOW_CURSED_WEAPONS;
    public static boolean	ALLOW_MANOR;
    public static boolean	ALLOW_NPC_WALKERS;
    public static boolean	ALLOW_PET_WALKERS;
    public static boolean	SERVER_NEWS;
    public static String	COMMUNITY_TYPE;
    public static boolean	BBS_SHOW_PLAYERLIST;
    public static String	BBS_DEFAULT;
    public static boolean	SHOW_LEVEL_COMMUNITYBOARD;
    public static boolean	SHOW_STATUS_COMMUNITYBOARD;
    public static int		NAME_PAGE_SIZE_COMMUNITYBOARD;
    public static int		NAME_PER_ROW_COMMUNITYBOARD;
    public static int		ALT_OLY_START_TIME;
    public static int		ALT_OLY_MIN;
    public static long		ALT_OLY_CPERIOD;
    public static long		ALT_OLY_BATTLE;
    public static long		ALT_OLY_BWAIT;
    public static long		ALT_OLY_IWAIT;
    public static long		ALT_OLY_WPERIOD;
    public static long		ALT_OLY_VPERIOD;
    public static int		ALT_OLY_CLASSED;
    public static int		ALT_OLY_NONCLASSED;
    public static int		ALT_OLY_BATTLE_REWARD_ITEM;
    public static int		ALT_OLY_CLASSED_RITEM_C;
    public static int		ALT_OLY_NONCLASSED_RITEM_C;
    public static int		ALT_OLY_COMP_RITEM;
    public static int		ALT_OLY_GP_PER_POINT;
    public static int		ALT_OLY_MIN_POINT_FOR_EXCH;
    public static int		ALT_OLY_HERO_POINTS;
    public static String	ALT_OLY_RESTRICTED_ITEMS;
    public static List<Integer> LIST_OLY_RESTRICTED_ITEMS = new FastList<Integer>();
    public static int		ALT_MANOR_REFRESH_TIME;
    public static int		ALT_MANOR_REFRESH_MIN;
    public static int		ALT_MANOR_APPROVE_TIME;
    public static int		ALT_MANOR_APPROVE_MIN;
    public static int		ALT_MANOR_MAINTENANCE_PERIOD;
    public static boolean	ALT_MANOR_SAVE_ALL_ACTIONS;
    public static int		ALT_MANOR_SAVE_PERIOD_RATE;
    public static int		ALT_LOTTERY_PRIZE;
    public static int		ALT_LOTTERY_TICKET_PRICE;
    public static float		ALT_LOTTERY_5_NUMBER_RATE;
    public static float		ALT_LOTTERY_4_NUMBER_RATE;
    public static float		ALT_LOTTERY_3_NUMBER_RATE;
    public static int		ALT_LOTTERY_2_AND_1_NUMBER_PRIZE;
    public static int		RIFT_MIN_PARTY_SIZE;
    public static int		RIFT_SPAWN_DELAY;
    public static int		RIFT_MAX_JUMPS;
    public static int		RIFT_AUTO_JUMPS_TIME_MIN;
    public static int		RIFT_AUTO_JUMPS_TIME_MAX;
    public static float		RIFT_BOSS_ROOM_TIME_MUTIPLY;
    public static int		RIFT_ENTER_COST_RECRUIT;
    public static int		RIFT_ENTER_COST_SOLDIER;
    public static int		RIFT_ENTER_COST_OFFICER;
    public static int		RIFT_ENTER_COST_CAPTAIN;
    public static int		RIFT_ENTER_COST_COMMANDER;
    public static int		RIFT_ENTER_COST_HERO;
    public static int		DEFAULT_PUNISH;
    public static int		DEFAULT_PUNISH_PARAM;
    public static boolean 	ONLY_GM_ITEMS_FREE;
    public static boolean 	JAIL_IS_PVP;
    public static boolean 	JAIL_DISABLE_CHAT;
    public static boolean	CUSTOM_SPAWNLIST_TABLE;
    public static boolean	SAVE_GMSPAWN_ON_CUSTOM;
    public static boolean	DELETE_GMSPAWN_ON_CUSTOM;
    public static boolean	CUSTOM_NPC_TABLE;
    public static boolean	CUSTOM_ETCITEM_TABLE;
    public static boolean	CUSTOM_ARMOR_TABLE;
    public static boolean	CUSTOM_ARMORSETS_TABLE;
    public static boolean	CUSTOM_WEAPON_TABLE;
    public static boolean	CUSTOM_TELEPORT_TABLE;
    public static boolean	CUSTOM_DROPLIST_TABLE;
    public static boolean	CUSTOM_MERCHANT_TABLES;
	
	/** ************************************************** **/
	/** General Settings -End                              **/
	/** ************************************************** **/
    
    /** ************************************************** **/
	/** L2JMods Settings -Begin                            **/
	/** ************************************************** **/
    
    public static boolean	L2JMOD_CHAMPION_ENABLE;
    public static boolean	L2JMOD_CHAMPION_PASSIVE;
    public static int		L2JMOD_CHAMPION_FREQUENCY;
    public static String	L2JMOD_CHAMP_TITLE;
    public static int		L2JMOD_CHAMP_MIN_LVL;
    public static int		L2JMOD_CHAMP_MAX_LVL;
    public static int		L2JMOD_CHAMPION_HP;
    public static int		L2JMOD_CHAMPION_REWARDS;
    public static int		L2JMOD_CHAMPION_ADENAS_REWARDS;
    public static float		L2JMOD_CHAMPION_HP_REGEN;
    public static float		L2JMOD_CHAMPION_ATK;
    public static float		L2JMOD_CHAMPION_SPD_ATK;
    public static int		L2JMOD_CHAMPION_REWARD_LOWER_LVL_ITEM_CHANCE;
    public static int		L2JMOD_CHAMPION_REWARD_HIGHER_LVL_ITEM_CHANCE;
    public static int		L2JMOD_CHAMPION_REWARD_ID;
    public static int		L2JMOD_CHAMPION_REWARD_QTY;
    public static boolean	TVT_EVENT_ENABLED;
    public static int		TVT_EVENT_INTERVAL;
    public static int		TVT_EVENT_PARTICIPATION_TIME;
    public static int		TVT_EVENT_RUNNING_TIME;
    public static int		TVT_EVENT_PARTICIPATION_NPC_ID;
    public static int[]		TVT_EVENT_PARTICIPATION_NPC_COORDINATES = new int[3];
    public static int		TVT_EVENT_MIN_PLAYERS_IN_TEAMS;
    public static int		TVT_EVENT_MAX_PLAYERS_IN_TEAMS;
    public static int		TVT_EVENT_RESPAWN_TELEPORT_DELAY;
    public static int		TVT_EVENT_START_LEAVE_TELEPORT_DELAY;
    public static String	TVT_EVENT_TEAM_1_NAME;
    public static int[]		TVT_EVENT_TEAM_1_COORDINATES = new int[3];
    public static String	TVT_EVENT_TEAM_2_NAME;
    public static int[]		TVT_EVENT_TEAM_2_COORDINATES = new int[3];
    public static List<int[]>	TVT_EVENT_REWARDS = new FastList<int[]>();
    public static boolean	TVT_EVENT_TARGET_TEAM_MEMBERS_ALLOWED;
    public static boolean	TVT_EVENT_SCROLL_ALLOWED;
    public static boolean	TVT_EVENT_POTIONS_ALLOWED;
    public static boolean	TVT_EVENT_SUMMON_BY_ITEM_ALLOWED;
    public static List<Integer>	TVT_DOORS_IDS_TO_OPEN = new ArrayList<Integer>();
    public static List<Integer>	TVT_DOORS_IDS_TO_CLOSE = new ArrayList<Integer>();
    public static boolean	TVT_REWARD_TEAM_TIE;
    public static byte		TVT_EVENT_MIN_LVL;
    public static byte		TVT_EVENT_MAX_LVL;
    public static boolean	L2JMOD_ALLOW_WEDDING;
    public static int L2JMOD_WEDDING_PRICE;
    public static boolean	L2JMOD_WEDDING_PUNISH_INFIDELITY;
    public static boolean	L2JMOD_WEDDING_TELEPORT;
    public static int		L2JMOD_WEDDING_TELEPORT_PRICE;
    public static int		L2JMOD_WEDDING_TELEPORT_DURATION;
    public static boolean	L2JMOD_WEDDING_SAMESEX;
    public static boolean	L2JMOD_WEDDING_FORMALWEAR;
    public static int		L2JMOD_WEDDING_DIVORCE_COSTS;
    public static boolean	BANKING_SYSTEM_ENABLED;
    public static int		BANKING_SYSTEM_GOLDBARS;
    public static int		BANKING_SYSTEM_ADENA;
    public static boolean	L2JMOD_ENABLE_WAREHOUSESORTING_CLAN;
    public static boolean	L2JMOD_ENABLE_WAREHOUSESORTING_PRIVATE;
    public static boolean	L2JMOD_ENABLE_WAREHOUSESORTING_FREIGHT;
    
    /** ************************************************** **/
	/** L2JMods Settings -End                              **/
	/** ************************************************** **/
    
	/** ************************************************** **/
	/** NPC Settings -Begin                                **/
	/** ************************************************** **/
	
	public static boolean	ANNOUNCE_MAMMON_SPAWN;
	public static boolean	ALT_MOB_AGRO_IN_PEACEZONE;
	public static boolean	ALT_GAME_VIEWNPC;
	public static int		MAX_DRIFT_RANGE;
    public static boolean	DEEPBLUE_DROP_RULES;
    public static boolean	SHOW_NPC_LVL;
    public static boolean	GUARD_ATTACK_AGGRO_MOB;
    public static boolean	ALLOW_WYVERN_UPGRADER;
    public static String	PET_RENT_NPC;
    public static List<Integer> LIST_PET_RENT_NPC   = new FastList<Integer>();
    public static int		WYVERN_SPEED;
    public static int		STRIDER_SPEED;
    public static int 		GREAT_WOLF_SPEED;
    public static double	RAID_HP_REGEN_MULTIPLIER;
    public static double	RAID_MP_REGEN_MULTIPLIER;
    public static double	RAID_PDEFENCE_MULTIPLIER;
    public static double	RAID_MDEFENCE_MULTIPLIER;
    public static double	RAID_MINION_RESPAWN_TIMER;
    public static float		RAID_MIN_RESPAWN_MULTIPLIER;
    public static float		RAID_MAX_RESPAWN_MULTIPLIER;
	
	/** ************************************************** **/
	/** NPC Settings -End                                  **/
	/** ************************************************** **/
    
    /** ************************************************** **/
	/** PvP Settings -Begin                                **/
	/** ************************************************** **/
    
    /** Minimum karma gain/loss */
    public static int		KARMA_MIN_KARMA;
    /** Maximum karma gain/loss */
    public static int		KARMA_MAX_KARMA;
    /** Number to divide the xp received by, to calculate karma lost on xp gain/lost */
    public static int		KARMA_XP_DIVIDER;
    /** The Minimum Karma lost if 0 karma is to be removed */
    public static int		KARMA_LOST_BASE;
    /** Can a GM drop item ? */
    public static boolean	KARMA_DROP_GM;
    /** Should award a pvp point for killing a player with karma ? */
    public static boolean	KARMA_AWARD_PK_KILL;
    /** Minimum PK required to drop */
    public static int		KARMA_PK_LIMIT;
    /** List of pet items that cannot be dropped (separated by ",") when PVP */
    public static String	KARMA_NONDROPPABLE_PET_ITEMS;
    /** List of items that cannot be dropped (separated by ",") when PVP*/
    public static String	KARMA_NONDROPPABLE_ITEMS;
    /** List of pet items that cannot be dropped when PVP */
    public static List<Integer> KARMA_LIST_NONDROPPABLE_PET_ITEMS   = new FastList<Integer>();
    /** List of items that cannot be dropped when PVP */
    public static List<Integer> KARMA_LIST_NONDROPPABLE_ITEMS       = new FastList<Integer>();
    
    /** ************************************************** **/
	/** PvP Settings -End                                  **/
	/** ************************************************** **/
    
    /** ************************************************** **/
	/** Rate Settings -Begin                               **/
	/** ************************************************** **/
    
    /** Rate for eXperience Point rewards */
    public static float		RATE_XP;
    /** Rate for Skill Point rewards */
    public static float		RATE_SP;
    /** Rate for party eXperience Point rewards */
    public static float		RATE_PARTY_XP;
    /** Rate for party Skill Point rewards */
    public static float		RATE_PARTY_SP;
    /** Rate for Quest rewards (XP and SP) */
    public static float		RATE_QUESTS_REWARD;
    /** Rate for drop adena */
    public static float		RATE_DROP_ADENA;
    /** Rate for cost of consumable */
    public static float		RATE_CONSUMABLE_COST;
    /** Rate for dropped items */
    public static float		RATE_DROP_ITEMS;
    /** Rate for dropped items by Raid Bosses */
    public static float		RATE_DROP_ITEMS_BY_RAID;
    /** Rate for spoiled items */
    public static float		RATE_DROP_SPOIL;
    /** Rate for manor items */
    public static int		RATE_DROP_MANOR;
    /** Rate for quest items */
    public static float		RATE_DROP_QUEST;
    /** Rate for karma and experience lose */
    public static float		RATE_KARMA_EXP_LOST;
    /** Rate siege guards prices */
    public static float		RATE_SIEGE_GUARDS_PRICE;
    /** Rate Common herbs */
    public static float		RATE_DROP_COMMON_HERBS;
    /** Rate MP/HP herbs */
    public static float		RATE_DROP_MP_HP_HERBS;
    /** Rate Common herbs */
    public static float		RATE_DROP_GREATER_HERBS;
    /** Rate Common herbs */
    public static float		RATE_DROP_SUPERIOR_HERBS;
    /** Rate Common herbs */
    public static float		RATE_DROP_SPECIAL_HERBS;
    /** Limit for player drop */
    public static int		PLAYER_DROP_LIMIT;
    /** Rate for drop */
    public static int		PLAYER_RATE_DROP;
    /** Rate for player's item drop */
    public static int		PLAYER_RATE_DROP_ITEM;
    /** Rate for player's equipment drop */
    public static int		PLAYER_RATE_DROP_EQUIP;
    /** Rate for player's equipment and weapon drop */
    public static int		PLAYER_RATE_DROP_EQUIP_WEAPON;
    /** Rate for experience rewards of the pet */
    public static float		PET_XP_RATE;
    /** Rate for food consumption of the pet */
    public static int		PET_FOOD_RATE;
    /** Rate for experience rewards of the Sin Eater */
    public static float		SINEATER_XP_RATE;
    /** Karma drop limit */
    public static int		KARMA_DROP_LIMIT;
    /** Karma drop rate */
    public static int		KARMA_RATE_DROP;
    /** Karma drop rate for item */
    public static int		KARMA_RATE_DROP_ITEM;
    /** Karma drop rate for equipment */
    public static int		KARMA_RATE_DROP_EQUIP;
    /** Karma drop rate for equipment and weapon */
    public static int		KARMA_RATE_DROP_EQUIP_WEAPON;
    
    /** ************************************************** **/
	/** Rate Settings -End                                 **/
	/** ************************************************** **/
    
    /** ************************************************** **/
	/** SevinSigns Settings -Begin                         **/
	/** ************************************************** **/
    
    public static boolean 	ALT_GAME_REQUIRE_CASTLE_DAWN;
    public static boolean 	ALT_GAME_REQUIRE_CLAN_CASTLE;
    public static int 		ALT_FESTIVAL_MIN_PLAYER;
    public static int 		ALT_MAXIMUM_PLAYER_CONTRIB;
    public static long		ALT_FESTIVAL_MANAGER_START;
    public static long		ALT_FESTIVAL_LENGTH;
    public static long		ALT_FESTIVAL_CYCLE_LENGTH;
    public static long		ALT_FESTIVAL_FIRST_SPAWN;
    public static long		ALT_FESTIVAL_FIRST_SWARM;
    public static long		ALT_FESTIVAL_SECOND_SPAWN;
    public static long		ALT_FESTIVAL_SECOND_SWARM;
    public static long		ALT_FESTIVAL_CHEST_SPAWN;
	
    /** ************************************************** **/
	/** SevinSigns Settings -End                           **/
	/** ************************************************** **/
    
    /** ************************************************** **/
	/** Server Settings -Begin                             **/
	/** ************************************************** **/
    
    /** Game Server ports */
    public static int		PORT_GAME;
    /** Login Server port */
    public static int		PORT_LOGIN;
    /** Login Server bind ip */
    public static String	LOGIN_BIND_ADDRESS;
    /** Number of login tries before IP ban gets activated, default 10*/
    public static int		LOGIN_TRY_BEFORE_BAN;
    /** Number of seconds the IP ban will last, default 10 minutes */
    public static int		LOGIN_BLOCK_AFTER_BAN;
    /** Hostname of the Game Server */
    public static String	GAMESERVER_HOSTNAME;
    /** Driver to access to database */
    public static String	DATABASE_DRIVER;
    /** Path to access to database */
    public static String	DATABASE_URL;
    /** Database login */
    public static String	DATABASE_LOGIN;
    /** Database password */
    public static String	DATABASE_PASSWORD;
    /** Maximum number of connections to the database */
    public static int		DATABASE_MAX_CONNECTIONS;
    /** Maximum number of players allowed to play simultaneously on server */
    public static int		MAXIMUM_ONLINE_USERS;
    /** Character name template */
    public static String	CNAME_TEMPLATE;
    /** Pet name template */
    public static String	PET_NAME_TEMPLATE;
    /** Maximum number of characters per account */
    public static int		MAX_CHARACTERS_NUMBER_PER_ACCOUNT;
    /** Datapack root directory */
    public static File		DATAPACK_ROOT;
    /** Accept alternate ID for server ? */
    public static boolean	ACCEPT_ALTERNATE_ID;
    /** ID for request to the server */
    public static int		REQUEST_ID;
    public static boolean	RESERVE_HOST_ON_LOGIN = false;
    /** Minimal protocol revision */
    public static int		MIN_PROTOCOL_REVISION;
    /** Maximal protocol revision */
    public static int		MAX_PROTOCOL_REVISION;
	
    /** ************************************************** **/
	/** Server Settings -End                           **/
	/** ************************************************** **/

    /******************************************************************************************************************************************************/
    /** L2J Variable Definitions End Here**/
	/******************************************************************************************************************************************************/
    public static int MAX_ITEM_IN_PACKET;

    public static boolean CHECK_KNOWN;

    /** Game Server login port */
    public static int        GAME_SERVER_LOGIN_PORT;
    /** Game Server login Host */
    public static String     GAME_SERVER_LOGIN_HOST;
    /** Internal Hostname */
    public static String     INTERNAL_HOSTNAME;
    /** External Hostname */
    public static String     EXTERNAL_HOSTNAME;
    public static int        PATH_NODE_RADIUS;
    public static int        NEW_NODE_ID;
    public static int        SELECTED_NODE_ID;
    public static int        LINKED_NODE_ID;
    public static String     NEW_NODE_TYPE;

    /** Time between 2 updates of IP */
    public static int        IP_UPDATE_TIME;

    // Server version
    /** Server version */
    public static String     SERVER_VERSION;
    /** Date of server build */
    public static String     SERVER_BUILD_DATE;

    // Datapack version
    /** Datapack version */
    public static String     DATAPACK_VERSION;    

    /** List of items that cannot be dropped (seperated by ",") */
    public static String        NONDROPPABLE_ITEMS;
    /** List of items that cannot be dropped */
    public static List<Integer> LIST_NONDROPPABLE_ITEMS       = new FastList<Integer>();

    /** Duration (in ms) while a player stay in PVP mode after hitting an innocent */
    public static int PVP_NORMAL_TIME;
    /** Duration (in ms) while a player stay in PVP mode after hitting a purple player */
    public static int PVP_PVP_TIME;


    // Packet information
    /** Count the amount of packets per minute ? */
    public static boolean  COUNT_PACKETS           = false;
    /** Dump packet count ? */
    public static boolean  DUMP_PACKET_COUNTS      = false;
    /** Time interval between 2 dumps */
    public static int      DUMP_INTERVAL_SECONDS   = 60;

    /** Enumeration for type of ID Factory */
    public static enum IdFactoryType
    {
        Compaction,
        BitSet,
        Stack
    }

    /** ID Factory type */
    public static IdFactoryType IDFACTORY_TYPE;
    /** Check for bad ID ? */
    public static boolean BAD_ID_CHECKING;

    /** Enumeration for type of maps object */
    public static enum ObjectMapType
    {
        L2ObjectHashMap,
        WorldObjectMap
    }

    /** Enumeration for type of set object */
    public static enum ObjectSetType
    {
        L2ObjectHashSet,
        WorldObjectSet
    }

    /** Type of map object */
    public static ObjectMapType   MAP_TYPE;
    /** Type of set object */
    public static ObjectSetType   SET_TYPE;

    /** Chance that an item will successfully be enchanted */
    public static int ENCHANT_CHANCE_WEAPON;
    public static int ENCHANT_CHANCE_ARMOR;
    public static int ENCHANT_CHANCE_JEWELRY;
    
    public static int BLESSED_ENCHANT_CHANCE_WEAPON;
    public static int BLESSED_ENCHANT_CHANCE_ARMOR;
    public static int BLESSED_ENCHANT_CHANCE_JEWELRY;

    /** Maximum level of enchantment */
    public static int ENCHANT_MAX_WEAPON;
    public static int ENCHANT_MAX_ARMOR;
    public static int ENCHANT_MAX_JEWELRY;
    /** maximum level of safe enchantment for normal items*/
    public static int ENCHANT_SAFE_MAX;
    /** maximum level of safe enchantment for full body armor*/
    public static int ENCHANT_SAFE_MAX_FULL;

    // Character multipliers
    /** Multiplier for character HP regeneration */
    public static double  HP_REGEN_MULTIPLIER;
    /** Mutilplier for character MP regeneration */
    public static double  MP_REGEN_MULTIPLIER;
    /** Multiplier for character CP regeneration */
    public static double  CP_REGEN_MULTIPLIER;

    /** Is telnet enabled ? */
    public static boolean IS_TELNET_ENABLED;

    /** Show licence or not just after login (if false, will directly go to the Server List */
    public static boolean SHOW_LICENCE;
    /** Force GameGuard authorization in loginserver */
    public static boolean FORCE_GGAUTH;

    /** Accept new game server ? */
    public static boolean ACCEPT_NEW_GAMESERVER;

    /** Server ID used with the HexID */
    public static int SERVER_ID;
    /** Hexadecimal ID of the game server */
    public static byte[] HEX_ID;

    public static int MINIMUM_UPDATE_DISTANCE;
    public static int KNOWNLIST_FORGET_DELAY;
    public static int MINIMUN_UPDATE_TIME;    

    /** Allow auto-create account ? */
    public static boolean AUTO_CREATE_ACCOUNTS;


    public static boolean FLOOD_PROTECTION;
    public static int     FAST_CONNECTION_LIMIT;
    public static int     NORMAL_CONNECTION_TIME;
    public static int     FAST_CONNECTION_TIME;
    public static int     MAX_CONNECTION_PER_IP;  

    /**
     * This class initializes all global variables for configuration.<br>
     * If key doesn't appear in properties file, a default value is setting on by this class.
     * @see CONFIGURATION_FILE (propertie file) for configuring your server.
     */
    public static void load()
    {
        if(Server.serverMode == Server.MODE_GAMESERVER)
        {
            _log.info("loading gameserver config");
            try {
                Properties serverSettings    = new Properties();
                InputStream is               = new FileInputStream(new File(CONFIGURATION_FILE));
                serverSettings.load(is);
                is.close();

                GAMESERVER_HOSTNAME     = serverSettings.getProperty("GameserverHostname");
                PORT_GAME               = Integer.parseInt(serverSettings.getProperty("GameserverPort", "7777"));

                EXTERNAL_HOSTNAME       = serverSettings.getProperty("ExternalHostname", "*");
                INTERNAL_HOSTNAME       = serverSettings.getProperty("InternalHostname", "*");

                GAME_SERVER_LOGIN_PORT  = Integer.parseInt(serverSettings.getProperty("LoginPort","9014"));
                GAME_SERVER_LOGIN_HOST  = serverSettings.getProperty("LoginHost","127.0.0.1");

                REQUEST_ID              = Integer.parseInt(serverSettings.getProperty("RequestServerID","0"));
                ACCEPT_ALTERNATE_ID     = Boolean.parseBoolean(serverSettings.getProperty("AcceptAlternateID","True"));

                DATABASE_DRIVER             = serverSettings.getProperty("Driver", "com.mysql.jdbc.Driver");
                DATABASE_URL                = serverSettings.getProperty("URL", "jdbc:mysql://localhost/l2jdb");
                DATABASE_LOGIN              = serverSettings.getProperty("Login", "root");
                DATABASE_PASSWORD           = serverSettings.getProperty("Password", "");
                DATABASE_MAX_CONNECTIONS    = Integer.parseInt(serverSettings.getProperty("MaximumDbConnections", "10"));

                DATAPACK_ROOT           = new File(serverSettings.getProperty("DatapackRoot", ".")).getCanonicalFile();

                CNAME_TEMPLATE          = serverSettings.getProperty("CnameTemplate", ".*");
                PET_NAME_TEMPLATE       = serverSettings.getProperty("PetNameTemplate", ".*");

                MAX_CHARACTERS_NUMBER_PER_ACCOUNT = Integer.parseInt(serverSettings.getProperty("CharMaxNumber", "0"));
                MAXIMUM_ONLINE_USERS        = Integer.parseInt(serverSettings.getProperty("MaximumOnlineUsers", "100"));

                MIN_PROTOCOL_REVISION   = Integer.parseInt(serverSettings.getProperty("MinProtocolRevision", "660"));
                MAX_PROTOCOL_REVISION   = Integer.parseInt(serverSettings.getProperty("MaxProtocolRevision", "665"));

                if (MIN_PROTOCOL_REVISION > MAX_PROTOCOL_REVISION)
                {
                    throw new Error("MinProtocolRevision is bigger than MaxProtocolRevision in server configuration file.");
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
                throw new Error("Failed to Load "+CONFIGURATION_FILE+" File.");
            }
            
            // Feature Config File
            try
            {
                Properties Feature    		= new Properties();
                InputStream is              = new FileInputStream(new File(FEATURE_CONFIG_FILE));
                Feature.load(is);
                is.close();
                
				CH_TELE_FEE_RATIO                                   = Long.parseLong(Feature.getProperty("ClanHallTeleportFunctionFeeRatio", "604800000"));
                CH_TELE1_FEE                                        = Integer.parseInt(Feature.getProperty("ClanHallTeleportFunctionFeeLvl1", "7000"));
                CH_TELE2_FEE                                        = Integer.parseInt(Feature.getProperty("ClanHallTeleportFunctionFeeLvl2", "14000"));
                CH_SUPPORT_FEE_RATIO                                = Long.parseLong(Feature.getProperty("ClanHallSupportFunctionFeeRatio", "86400000"));
                CH_SUPPORT1_FEE                                     = Integer.parseInt(Feature.getProperty("ClanHallSupportFeeLvl1", "2500"));
                CH_SUPPORT2_FEE                                     = Integer.parseInt(Feature.getProperty("ClanHallSupportFeeLvl2", "5000"));
                CH_SUPPORT3_FEE                                     = Integer.parseInt(Feature.getProperty("ClanHallSupportFeeLvl3", "7000"));
                CH_SUPPORT4_FEE                                     = Integer.parseInt(Feature.getProperty("ClanHallSupportFeeLvl4", "11000"));
                CH_SUPPORT5_FEE                                     = Integer.parseInt(Feature.getProperty("ClanHallSupportFeeLvl5", "21000"));
                CH_SUPPORT6_FEE                                     = Integer.parseInt(Feature.getProperty("ClanHallSupportFeeLvl6", "36000"));
                CH_SUPPORT7_FEE                                     = Integer.parseInt(Feature.getProperty("ClanHallSupportFeeLvl7", "37000"));
                CH_SUPPORT8_FEE                                     = Integer.parseInt(Feature.getProperty("ClanHallSupportFeeLvl8", "52000"));
                CH_MPREG_FEE_RATIO                                  = Long.parseLong(Feature.getProperty("ClanHallMpRegenerationFunctionFeeRatio", "86400000"));
                CH_MPREG1_FEE                                       = Integer.parseInt(Feature.getProperty("ClanHallMpRegenerationFeeLvl1", "2000"));
                CH_MPREG2_FEE                                       = Integer.parseInt(Feature.getProperty("ClanHallMpRegenerationFeeLvl2", "3750"));
                CH_MPREG3_FEE                                       = Integer.parseInt(Feature.getProperty("ClanHallMpRegenerationFeeLvl3", "6500"));
                CH_MPREG4_FEE                                       = Integer.parseInt(Feature.getProperty("ClanHallMpRegenerationFeeLvl4", "13750"));
                CH_MPREG5_FEE                                       = Integer.parseInt(Feature.getProperty("ClanHallMpRegenerationFeeLvl5", "20000"));
                CH_HPREG_FEE_RATIO                                  = Long.parseLong(Feature.getProperty("ClanHallHpRegenerationFunctionFeeRatio", "86400000"));
                CH_HPREG1_FEE                                       = Integer.parseInt(Feature.getProperty("ClanHallHpRegenerationFeeLvl1", "700"));
                CH_HPREG2_FEE                                       = Integer.parseInt(Feature.getProperty("ClanHallHpRegenerationFeeLvl2", "800"));
                CH_HPREG3_FEE                                       = Integer.parseInt(Feature.getProperty("ClanHallHpRegenerationFeeLvl3", "1000"));
                CH_HPREG4_FEE                                       = Integer.parseInt(Feature.getProperty("ClanHallHpRegenerationFeeLvl4", "1166"));
                CH_HPREG5_FEE                                       = Integer.parseInt(Feature.getProperty("ClanHallHpRegenerationFeeLvl5", "1500"));
                CH_HPREG6_FEE                                       = Integer.parseInt(Feature.getProperty("ClanHallHpRegenerationFeeLvl6", "1750"));
                CH_HPREG7_FEE                                       = Integer.parseInt(Feature.getProperty("ClanHallHpRegenerationFeeLvl7", "2000"));
                CH_HPREG8_FEE                                       = Integer.parseInt(Feature.getProperty("ClanHallHpRegenerationFeeLvl8", "2250"));
                CH_HPREG9_FEE                                       = Integer.parseInt(Feature.getProperty("ClanHallHpRegenerationFeeLvl9", "2500"));
                CH_HPREG10_FEE                                      = Integer.parseInt(Feature.getProperty("ClanHallHpRegenerationFeeLvl10", "3250"));
                CH_HPREG11_FEE                                      = Integer.parseInt(Feature.getProperty("ClanHallHpRegenerationFeeLvl11", "3270"));
                CH_HPREG12_FEE										= Integer.parseInt(Feature.getProperty("ClanHallHpRegenerationFeeLvl12", "4250"));
                CH_HPREG13_FEE                                      = Integer.parseInt(Feature.getProperty("ClanHallHpRegenerationFeeLvl13", "5166"));
                CH_EXPREG_FEE_RATIO                                 = Long.parseLong(Feature.getProperty("ClanHallExpRegenerationFunctionFeeRatio", "86400000"));
                CH_EXPREG1_FEE                                      = Integer.parseInt(Feature.getProperty("ClanHallExpRegenerationFeeLvl1", "3000"));
                CH_EXPREG2_FEE                                      = Integer.parseInt(Feature.getProperty("ClanHallExpRegenerationFeeLvl2", "6000"));
                CH_EXPREG3_FEE                                      = Integer.parseInt(Feature.getProperty("ClanHallExpRegenerationFeeLvl3", "9000"));
                CH_EXPREG4_FEE                                      = Integer.parseInt(Feature.getProperty("ClanHallExpRegenerationFeeLvl4", "15000"));
                CH_EXPREG5_FEE                                      = Integer.parseInt(Feature.getProperty("ClanHallExpRegenerationFeeLvl5", "21000"));
                CH_EXPREG6_FEE                                      = Integer.parseInt(Feature.getProperty("ClanHallExpRegenerationFeeLvl6", "23330"));
                CH_EXPREG7_FEE                                      = Integer.parseInt(Feature.getProperty("ClanHallExpRegenerationFeeLvl7", "30000"));
                CH_ITEM_FEE_RATIO                                   = Long.parseLong(Feature.getProperty("ClanHallItemCreationFunctionFeeRatio", "86400000"));
                CH_ITEM1_FEE                                        = Integer.parseInt(Feature.getProperty("ClanHallItemCreationFunctionFeeLvl1", "30000"));
                CH_ITEM2_FEE                                        = Integer.parseInt(Feature.getProperty("ClanHallItemCreationFunctionFeeLvl2", "70000"));
                CH_ITEM3_FEE                                        = Integer.parseInt(Feature.getProperty("ClanHallItemCreationFunctionFeeLvl3", "140000"));
                CH_CURTAIN_FEE_RATIO                                = Long.parseLong(Feature.getProperty("ClanHallCurtainFunctionFeeRatio", "604800000"));
                CH_CURTAIN1_FEE                                     = Integer.parseInt(Feature.getProperty("ClanHallCurtainFunctionFeeLvl1", "2000"));
                CH_CURTAIN2_FEE                                     = Integer.parseInt(Feature.getProperty("ClanHallCurtainFunctionFeeLvl2", "2500"));
                CH_FRONT_FEE_RATIO                                	= Long.parseLong(Feature.getProperty("ClanHallFrontPlatformFunctionFeeRatio", "259200000"));
                CH_FRONT1_FEE                                       = Integer.parseInt(Feature.getProperty("ClanHallFrontPlatformFunctionFeeLvl1", "1300"));
                CH_FRONT2_FEE                                       = Integer.parseInt(Feature.getProperty("ClanHallFrontPlatformFunctionFeeLvl2", "4000"));
                
                
                
                //
				CS_TELE_FEE_RATIO                                   = Long.parseLong(Feature.getProperty("CastleTeleportFunctionFeeRatio", "604800000"));
                CS_TELE1_FEE                                        = Integer.parseInt(Feature.getProperty("CastleTeleportFunctionFeeLvl1", "7000"));
                CS_TELE2_FEE                                        = Integer.parseInt(Feature.getProperty("CastleTeleportFunctionFeeLvl2", "14000"));
                CS_SUPPORT_FEE_RATIO                                = Long.parseLong(Feature.getProperty("CastleSupportFunctionFeeRatio", "86400000"));
                CS_SUPPORT1_FEE                                     = Integer.parseInt(Feature.getProperty("CastleSupportFeeLvl1", "7000"));
                CS_SUPPORT2_FEE                                     = Integer.parseInt(Feature.getProperty("CastleSupportFeeLvl2", "21000"));
                CS_SUPPORT3_FEE                                     = Integer.parseInt(Feature.getProperty("CastleSupportFeeLvl3", "37000"));
                CS_SUPPORT4_FEE                                     = Integer.parseInt(Feature.getProperty("CastleSupportFeeLvl4", "52000"));
                CS_MPREG_FEE_RATIO                                  = Long.parseLong(Feature.getProperty("CastleMpRegenerationFunctionFeeRatio", "86400000"));
                CS_MPREG1_FEE                                       = Integer.parseInt(Feature.getProperty("CastleMpRegenerationFeeLvl1", "2000"));
                CS_MPREG2_FEE                                       = Integer.parseInt(Feature.getProperty("CastleMpRegenerationFeeLvl2", "6500"));
                CS_MPREG3_FEE                                       = Integer.parseInt(Feature.getProperty("CastleMpRegenerationFeeLvl3", "13750"));
                CS_MPREG4_FEE                                       = Integer.parseInt(Feature.getProperty("CastleMpRegenerationFeeLvl4", "20000"));
                CS_HPREG_FEE_RATIO                                  = Long.parseLong(Feature.getProperty("CastleHpRegenerationFunctionFeeRatio", "86400000"));
                CS_HPREG1_FEE                                       = Integer.parseInt(Feature.getProperty("CastleHpRegenerationFeeLvl1", "1000"));
                CS_HPREG2_FEE                                       = Integer.parseInt(Feature.getProperty("CastleHpRegenerationFeeLvl2", "1500"));
                CS_HPREG3_FEE                                       = Integer.parseInt(Feature.getProperty("CastleHpRegenerationFeeLvl3", "2250"));
                CS_HPREG4_FEE                                      = Integer.parseInt(Feature.getProperty("CastleHpRegenerationFeeLvl14", "3270"));
                CS_HPREG5_FEE                                      = Integer.parseInt(Feature.getProperty("CastleHpRegenerationFeeLvl15", "5166"));
                CS_EXPREG_FEE_RATIO                                 = Long.parseLong(Feature.getProperty("CastleExpRegenerationFunctionFeeRatio", "86400000"));
                CS_EXPREG1_FEE                                      = Integer.parseInt(Feature.getProperty("CastleExpRegenerationFeeLvl1", "9000"));
                CS_EXPREG2_FEE                                      = Integer.parseInt(Feature.getProperty("CastleExpRegenerationFeeLvl2", "15000"));
                CS_EXPREG3_FEE                                      = Integer.parseInt(Feature.getProperty("CastleExpRegenerationFeeLvl3", "21000"));
                CS_EXPREG4_FEE                                      = Integer.parseInt(Feature.getProperty("CastleExpRegenerationFeeLvl4", "30000"));
                //
                
                
				ALT_GAME_REQUIRE_CASTLE_DAWN    					= Boolean.parseBoolean(Feature.getProperty("AltRequireCastleForDawn", "False"));
                ALT_GAME_REQUIRE_CLAN_CASTLE    					= Boolean.parseBoolean(Feature.getProperty("AltRequireClanCastle", "False"));
                ALT_FESTIVAL_MIN_PLAYER         					= Integer.parseInt(Feature.getProperty("AltFestivalMinPlayer", "5"));
                ALT_MAXIMUM_PLAYER_CONTRIB      					= Integer.parseInt(Feature.getProperty("AltMaxPlayerContrib", "1000000"));
                ALT_FESTIVAL_MANAGER_START      					= Long.parseLong(Feature.getProperty("AltFestivalManagerStart", "120000"));
                ALT_FESTIVAL_LENGTH             					= Long.parseLong(Feature.getProperty("AltFestivalLength", "1080000"));
                ALT_FESTIVAL_CYCLE_LENGTH       					= Long.parseLong(Feature.getProperty("AltFestivalCycleLength", "2280000"));
                ALT_FESTIVAL_FIRST_SPAWN        					= Long.parseLong(Feature.getProperty("AltFestivalFirstSpawn", "120000"));
                ALT_FESTIVAL_FIRST_SWARM       						= Long.parseLong(Feature.getProperty("AltFestivalFirstSwarm", "300000"));                ALT_FESTIVAL_SECOND_SPAWN       = Long.parseLong(Feature.getProperty("AltFestivalSecondSpawn", "540000"));
                ALT_FESTIVAL_SECOND_SWARM       					= Long.parseLong(Feature.getProperty("AltFestivalSecondSwarm", "720000"));
                ALT_FESTIVAL_CHEST_SPAWN       						= Long.parseLong(Feature.getProperty("AltFestivalChestSpawn", "900000"));

            }
            catch (Exception e)
            {
                e.printStackTrace();
                throw new Error("Failed to Load "+FEATURE_CONFIG_FILE+" File.");
            }
            
            // Character Config File
            try
            {
                Properties Character    	= new Properties();
                InputStream is              = new FileInputStream(new File(CHARACTER_CONFIG_FILE));
                Character.load(is);
                is.close();

                MASTERACCESS_LEVEL					= Integer.parseInt(Character.getProperty("MasterAccessLevel", "127"));
                MASTERACCESS_NAME_COLOR				= Integer.decode("0x" + Character.getProperty("MasterNameColor", "00FF00"));
                MASTERACCESS_TITLE_COLOR			= Integer.decode("0x" + Character.getProperty("MasterTitleColor", "00FF00"));
                ALT_GAME_DELEVEL					= Boolean.parseBoolean(Character.getProperty("Delevel", "true"));
                ALT_WEIGHT_LIMIT					= Double.parseDouble(Character.getProperty("AltWeightLimit", "1"));
                RUN_SPD_BOOST						= Integer.parseInt(Character.getProperty("RunSpeedBoost", "0"));
                DEATH_PENALTY_CHANCE				= Integer.parseInt(Character.getProperty("DeathPenaltyChance", "20"));
                RESPAWN_RESTORE_CP					= Double.parseDouble(Character.getProperty("RespawnRestoreCP", "0")) / 100;
                RESPAWN_RESTORE_HP					= Double.parseDouble(Character.getProperty("RespawnRestoreHP", "70")) / 100;
                RESPAWN_RESTORE_MP					= Double.parseDouble(Character.getProperty("RespawnRestoreMP", "70")) / 100;
                HP_REGEN_MULTIPLIER					= Double.parseDouble(Character.getProperty("HpRegenMultiplier", "100")) /100;
                MP_REGEN_MULTIPLIER					= Double.parseDouble(Character.getProperty("MpRegenMultiplier", "100")) /100;
                CP_REGEN_MULTIPLIER 				= Double.parseDouble(Character.getProperty("CpRegenMultiplier", "100")) /100;
                ALT_GAME_TIREDNESS					= Boolean.parseBoolean(Character.getProperty("AltGameTiredness", "false"));
                ENABLE_MODIFY_SKILL_DURATION		= Boolean.parseBoolean(Character.getProperty("EnableModifySkillDuration", "false"));
                // Create Map only if enabled
                if (ENABLE_MODIFY_SKILL_DURATION)
                {
                    SKILL_DURATION_LIST = new FastMap<Integer, Integer>();
                    String[] propertySplit;
                    propertySplit = Character.getProperty("SkillDurationList", "").split(";");
                    for (String skill : propertySplit)
                    {
                        String[] skillSplit = skill.split(",");
                        if (skillSplit.length != 2)
                        {
                            System.out.println("[SkillDurationList]: invalid config property -> SkillDurationList \"" + skill + "\"");
                        } else
                        {
                            try
                            {
                                SKILL_DURATION_LIST.put(Integer.parseInt(skillSplit[0]), Integer.parseInt(skillSplit[1]));
                            } catch (NumberFormatException nfe)
                            {
                                if (!skill.equals(""))
                                {
                                    System.out.println("[SkillDurationList]: invalid config property -> SkillList \"" + skillSplit[0] + "\"" + skillSplit[1]);
                                }
                            }
                        }
                    }
                }
                ENABLE_MODIFY_SKILL_REUSE = Boolean.parseBoolean(Character.getProperty("EnableModifySkillReuse", "false"));

                // Create Map only if enabled
                if (ENABLE_MODIFY_SKILL_REUSE)
                {
                    SKILL_REUSE_LIST = new FastMap<Integer, Integer>();
                    String[] propertySplit;
                    propertySplit = Character.getProperty("SkillReuseList", "").split(";");
                    for (String skill : propertySplit)
                    {
                        String[] skillSplit = skill.split(",");
                        if (skillSplit.length != 2)
                        {
                            System.out.println("[SkillReuseList]: invalid config property -> SkillReuseList \"" + skill + "\"");
                        } else
                        {
                            try
                            {
                                SKILL_REUSE_LIST.put(Integer.valueOf(skillSplit[0]), Integer.valueOf(skillSplit[1]));
                            } catch (NumberFormatException nfe)
                            {
                                if (!skill.equals(""))
                                {
                                    System.out.println("[SkillReuseList]: invalid config property -> SkillList \"" + skillSplit[0] + "\"" + skillSplit[1]);
                                }
                            }
                        }
                    }
                }
                
                AUTO_LEARN_SKILLS					= Boolean.parseBoolean(Character.getProperty("AutoLearnSkills", "false"));
                AUTO_LOOT_HERBS						= Boolean.parseBoolean(Character.getProperty("AutoLootHerbs", "true"));
                BUFFS_MAX_AMOUNT					= Byte.parseByte(Character.getProperty("maxbuffamount","24"));
                AUTO_LEARN_DIVINE_INSPIRATION		= Boolean.parseBoolean(Character.getProperty("AutoLearnDivineInspiration", "false"));
                ALT_GAME_CANCEL_BOW					= Character.getProperty("AltGameCancelByHit", "Cast").equalsIgnoreCase("bow") || Character.getProperty("AltGameCancelByHit", "Cast").equalsIgnoreCase("all");
                ALT_GAME_CANCEL_CAST				= Character.getProperty("AltGameCancelByHit", "Cast").equalsIgnoreCase("cast") || Character.getProperty("AltGameCancelByHit", "Cast").equalsIgnoreCase("all");
                EFFECT_CANCELING					= Boolean.parseBoolean(Character.getProperty("CancelLesserEffect", "True"));
                ALT_GAME_MAGICFAILURES				= Boolean.parseBoolean(Character.getProperty("MagicFailures", "false"));
                PLAYER_FAKEDEATH_UP_PROTECTION 		= Integer.parseInt(Character.getProperty("PlayerFakeDeathUpProtection", "0"));
                STORE_SKILL_COOLTIME				= Boolean.parseBoolean(Character.getProperty("StoreSkillCooltime", "true"));
                ALT_GAME_SHIELD_BLOCKS				= Boolean.parseBoolean(Character.getProperty("AltShieldBlocks", "false"));
                ALT_PERFECT_SHLD_BLOCK				= Integer.parseInt(Character.getProperty("AltPerfectShieldBlockRate", "10"));
                ALLOW_CLASS_MASTERS					= Boolean.parseBoolean(Character.getProperty("AllowClassMasters", "False"));
                LIFE_CRYSTAL_NEEDED					= Boolean.parseBoolean(Character.getProperty("LifeCrystalNeeded", "true"));
                SP_BOOK_NEEDED						= Boolean.parseBoolean(Character.getProperty("SpBookNeeded", "true"));
                ES_SP_BOOK_NEEDED					= Boolean.parseBoolean(Character.getProperty("EnchantSkillSpBookNeeded","true"));
                ALT_GAME_SKILL_LEARN				= Boolean.parseBoolean(Character.getProperty("AltGameSkillLearn", "false"));
                ALT_GAME_SUBCLASS_WITHOUT_QUESTS	= Boolean.parseBoolean(Character.getProperty("AltSubClassWithoutQuests", "False"));
                MAX_RUN_SPEED						= Integer.parseInt(Character.getProperty("MaxRunSpeed", "250"));
                MAX_PCRIT_RATE						= Integer.parseInt(Character.getProperty("MaxPCritRate", "500"));
                MAX_MCRIT_RATE						= Integer.parseInt(Character.getProperty("MaxMCritRate", "300"));
                MAX_SUBCLASS						= Byte.parseByte(Character.getProperty("MaxSubclass", "3"));
                MAX_PVTSTORESELL_SLOTS_DWARF		= Integer.parseInt(Character.getProperty("MaxPvtStoreSellSlotsDwarf", "4"));
                MAX_PVTSTORESELL_SLOTS_OTHER		= Integer.parseInt(Character.getProperty("MaxPvtStoreSellSlotsOther", "3"));
                MAX_PVTSTOREBUY_SLOTS_DWARF			= Integer.parseInt(Character.getProperty("MaxPvtStoreBuySlotsDwarf", "5"));
                MAX_PVTSTOREBUY_SLOTS_OTHER			= Integer.parseInt(Character.getProperty("MaxPvtStoreBuySlotsOther", "4"));
                INVENTORY_MAXIMUM_NO_DWARF			= Integer.parseInt(Character.getProperty("MaximumSlotsForNoDwarf", "80"));
                INVENTORY_MAXIMUM_DWARF				= Integer.parseInt(Character.getProperty("MaximumSlotsForDwarf", "100"));
                INVENTORY_MAXIMUM_GM				= Integer.parseInt(Character.getProperty("MaximumSlotsForGMPlayer", "250"));
                MAX_ITEM_IN_PACKET 					= Math.max(INVENTORY_MAXIMUM_NO_DWARF, Math.max(INVENTORY_MAXIMUM_DWARF, INVENTORY_MAXIMUM_GM));
                WAREHOUSE_SLOTS_DWARF				= Integer.parseInt(Character.getProperty("MaximumWarehouseSlotsForDwarf", "120"));
                WAREHOUSE_SLOTS_NO_DWARF			= Integer.parseInt(Character.getProperty("MaximumWarehouseSlotsForNoDwarf", "100"));
                WAREHOUSE_SLOTS_CLAN				= Integer.parseInt(Character.getProperty("MaximumWarehouseSlotsForClan", "150"));
                FREIGHT_SLOTS						= Integer.parseInt(Character.getProperty("MaximumFreightSlots", "20"));
                CASTLE_SHIELD						= Boolean.parseBoolean(Character.getProperty("CastleShieldRestriction", "True"));
                CLANHALL_SHIELD						= Boolean.parseBoolean(Character.getProperty("ClanHallShieldRestriction", "True"));
                APELLA_ARMORS						= Boolean.parseBoolean(Character.getProperty("ApellaArmorsRestriction", "True"));
                OATH_ARMORS							= Boolean.parseBoolean(Character.getProperty("OathArmorsRestriction", "True"));
                CASTLE_CROWN						= Boolean.parseBoolean(Character.getProperty("CastleLordsCrownRestriction", "True"));
                CASTLE_CIRCLETS						= Boolean.parseBoolean(Character.getProperty("CastleCircletsRestriction", "True"));
                ENCHANT_CHANCE_WEAPON				= Integer.parseInt(Character.getProperty("EnchantChanceWeapon", "68"));
                ENCHANT_CHANCE_ARMOR				= Integer.parseInt(Character.getProperty("EnchantChanceArmor", "52"));
                ENCHANT_CHANCE_JEWELRY				= Integer.parseInt(Character.getProperty("EnchantChanceJewelry", "54"));
                BLESSED_ENCHANT_CHANCE_WEAPON		= Integer.parseInt(Character.getProperty("BlessedEnchantChanceWeapon", "68"));
                BLESSED_ENCHANT_CHANCE_ARMOR		= Integer.parseInt(Character.getProperty("BlessedEnchantChanceArmor", "52"));
                BLESSED_ENCHANT_CHANCE_JEWELRY		= Integer.parseInt(Character.getProperty("BlessedEnchantChanceJewelry", "54"));
                ENCHANT_MAX_WEAPON					= Integer.parseInt(Character.getProperty("EnchantMaxWeapon", "255"));
                ENCHANT_MAX_ARMOR					= Integer.parseInt(Character.getProperty("EnchantMaxArmor", "255"));
                ENCHANT_MAX_JEWELRY					= Integer.parseInt(Character.getProperty("EnchantMaxJewelry", "255"));
                ENCHANT_SAFE_MAX					= Integer.parseInt(Character.getProperty("EnchantSafeMax", "3"));
                ENCHANT_SAFE_MAX_FULL				= Integer.parseInt(Character.getProperty("EnchantSafeMaxFull", "4"));
                ALT_GAME_KARMA_PLAYER_CAN_BE_KILLED_IN_PEACEZONE	= Boolean.parseBoolean(Character.getProperty("AltKarmaPlayerCanBeKilledInPeaceZone", "false"));
                ALT_GAME_KARMA_PLAYER_CAN_SHOP		= Boolean.parseBoolean(Character.getProperty("AltKarmaPlayerCanShop", "true"));
                ALT_GAME_KARMA_PLAYER_CAN_TELEPORT	= Boolean.parseBoolean(Character.getProperty("AltKarmaPlayerCanTeleport", "true"));
                ALT_GAME_KARMA_PLAYER_CAN_USE_GK	= Boolean.parseBoolean(Character.getProperty("AltKarmaPlayerCanUseGK", "false"));
                ALT_GAME_KARMA_PLAYER_CAN_TRADE		= Boolean.parseBoolean(Character.getProperty("AltKarmaPlayerCanTrade", "true"));
                ALT_GAME_KARMA_PLAYER_CAN_USE_WAREHOUSE	= Boolean.parseBoolean(Character.getProperty("AltKarmaPlayerCanUseWareHouse", "true"));
                IS_CRAFTING_ENABLED					= Boolean.parseBoolean(Character.getProperty("CraftingEnabled", "true"));
                DWARF_RECIPE_LIMIT					= Integer.parseInt(Character.getProperty("DwarfRecipeLimit","50"));
                COMMON_RECIPE_LIMIT					= Integer.parseInt(Character.getProperty("CommonRecipeLimit","50"));
                ALT_GAME_CREATION					= Boolean.parseBoolean(Character.getProperty("AltGameCreation", "false"));
                ALT_GAME_CREATION_SPEED				= Double.parseDouble(Character.getProperty("AltGameCreationSpeed", "1"));
                ALT_GAME_CREATION_XP_RATE			= Double.parseDouble(Character.getProperty("AltGameCreationRateXp", "1"));
                ALT_GAME_CREATION_SP_RATE			= Double.parseDouble(Character.getProperty("AltGameCreationRateSp", "1"));
                ALT_BLACKSMITH_USE_RECIPES			= Boolean.parseBoolean(Character.getProperty("AltBlacksmithUseRecipes", "true"));
                ALT_CLAN_JOIN_DAYS					= Integer.parseInt(Character.getProperty("DaysBeforeJoinAClan", "5"));
                ALT_CLAN_CREATE_DAYS				= Integer.parseInt(Character.getProperty("DaysBeforeCreateAClan", "10"));
                ALT_CLAN_DISSOLVE_DAYS				= Integer.parseInt(Character.getProperty("DaysToPassToDissolveAClan", "7"));
                ALT_ALLY_JOIN_DAYS_WHEN_LEAVED		= Integer.parseInt(Character.getProperty("DaysBeforeJoinAllyWhenLeaved", "1"));
                ALT_ALLY_JOIN_DAYS_WHEN_DISMISSED	= Integer.parseInt(Character.getProperty("DaysBeforeJoinAllyWhenDismissed", "1"));
                ALT_ACCEPT_CLAN_DAYS_WHEN_DISMISSED	= Integer.parseInt(Character.getProperty("DaysBeforeAcceptNewClanWhenDismissed", "1"));
                ALT_CREATE_ALLY_DAYS_WHEN_DISSOLVED	= Integer.parseInt(Character.getProperty("DaysBeforeCreateNewAllyWhenDissolved", "10"));
                ALT_MAX_NUM_OF_CLANS_IN_ALLY		= Integer.parseInt(Character.getProperty("AltMaxNumOfClansInAlly", "3"));
                ALT_CLAN_MEMBERS_FOR_WAR    		= Integer.parseInt(Character.getProperty("AltClanMembersForWar", "15"));
                //ALT_GAME_NEW_CHAR_ALWAYS_IS_NEWBIE	= Boolean.parseBoolean(Character.getProperty("AltNewCharAlwaysIsNewbie", "False"));
                ALT_MEMBERS_CAN_WITHDRAW_FROM_CLANWH= Boolean.parseBoolean(Character.getProperty("AltMembersCanWithdrawFromClanWH", "false"));
                REMOVE_CASTLE_CIRCLETS				= Boolean.parseBoolean(Character.getProperty("RemoveCastleCirclets", "true"));
                ALT_PARTY_RANGE						= Integer.parseInt(Character.getProperty("AltPartyRange", "1600"));
                ALT_PARTY_RANGE2					= Integer.parseInt(Character.getProperty("AltPartyRange2", "1400"));
                STARTING_ADENA						= Integer.parseInt(Character.getProperty("StartingAdena", "100"));
                AUTO_LOOT							= Boolean.parseBoolean(Character.getProperty("AutoLoot", "true"));
                UNSTUCK_INTERVAL					= Integer.parseInt(Character.getProperty("UnstuckInterval", "300"));
                PLAYER_SPAWN_PROTECTION				= Integer.parseInt(Character.getProperty("PlayerSpawnProtection", "0"));
                RESPAWN_RANDOM_ENABLED				= Boolean.parseBoolean(Character.getProperty("RespawnRandomInTown", "False"));
                RESPAWN_RANDOM_MAX_OFFSET			= Integer.parseInt(Character.getProperty("RespawnRandomMaxOffset", "50"));
                PETITIONING_ALLOWED					= Boolean.parseBoolean(Character.getProperty("PetitioningAllowed", "True"));
                MAX_PETITIONS_PER_PLAYER			= Integer.parseInt(Character.getProperty("MaxPetitionsPerPlayer", "5"));
                MAX_PETITIONS_PENDING				= Integer.parseInt(Character.getProperty("MaxPetitionsPending", "25"));
                ALT_GAME_FREIGHTS					= Boolean.parseBoolean(Character.getProperty("AltGameFreights", "false"));
                ALT_GAME_FREIGHT_PRICE				= Integer.parseInt(Character.getProperty("AltGameFreightPrice", "1000"));
                ALT_GAME_FREE_TELEPORT				= Boolean.parseBoolean(Character.getProperty("AltFreeTeleporting", "False"));
                ALT_RECOMMEND						= Boolean.parseBoolean(Character.getProperty("AltRecommend", "False"));
                DELETE_DAYS							= Integer.parseInt(Character.getProperty("DeleteCharAfterDays", "7"));
                ALT_GAME_EXPONENT_XP				= Float.parseFloat(Character.getProperty("AltGameExponentXp", "0."));
                ALT_GAME_EXPONENT_SP				= Float.parseFloat(Character.getProperty("AltGameExponentSp", "0."));
                PARTY_XP_CUTOFF_METHOD				= Character.getProperty("PartyXpCutoffMethod", "percentage");
                PARTY_XP_CUTOFF_PERCENT				= Double.parseDouble(Character.getProperty("PartyXpCutoffPercent", "3."));
                PARTY_XP_CUTOFF_LEVEL				= Integer.parseInt(Character.getProperty("PartyXpCutoffLevel", "30"));
            }
            catch (Exception e)
            {
                e.printStackTrace();
                throw new Error("Failed to Load "+CHARACTER_CONFIG_FILE+" File.");
            }

            /*
             * Load L2J Server Version Properties file (if exists)
             */
            try
            {
                Properties serverVersion    = new Properties();
                InputStream is              = new FileInputStream(new File(SERVER_VERSION_FILE));
                serverVersion.load(is);
                is.close();

                SERVER_VERSION      = serverVersion.getProperty("version", "Unsupported Custom Version.");
                SERVER_BUILD_DATE   = serverVersion.getProperty("builddate", "Undefined Date.");
            }
            catch (Exception e)
            {
                //Ignore Properties file if it doesnt exist
                SERVER_VERSION      = "Unsupported Custom Version.";
                SERVER_BUILD_DATE   = "Undefined Date.";
            }

            /*
             * Load L2J Datapack Version Properties file (if exists)
             */
            try
            {
                Properties serverVersion    = new Properties();
                InputStream is              = new FileInputStream(new File(DATAPACK_VERSION_FILE));
                serverVersion.load(is);
                is.close();

                DATAPACK_VERSION      = serverVersion.getProperty("version", "Unsupported Custom Version.");
            }
            catch (Exception e)
            {
                //Ignore Properties file if it doesnt exist
                DATAPACK_VERSION      = "Unsupported Custom Version.";
            }

            // telnet
            try
            {
                Properties telnetSettings   = new Properties();
                InputStream is              = new FileInputStream(new File(TELNET_FILE));
                telnetSettings.load(is);
                is.close();

                IS_TELNET_ENABLED   = Boolean.parseBoolean(telnetSettings.getProperty("EnableTelnet", "false"));
            }
            catch (Exception e)
            {
                e.printStackTrace();
                throw new Error("Failed to Load "+TELNET_FILE+" File.");
            }

            // id factory
            try
            {
                Properties idSettings   = new Properties();
                InputStream is          = new FileInputStream(new File(ID_CONFIG_FILE));
                idSettings.load(is);
                is.close();

                MAP_TYPE        = ObjectMapType.valueOf(idSettings.getProperty("L2Map", "WorldObjectMap"));
                SET_TYPE        = ObjectSetType.valueOf(idSettings.getProperty("L2Set", "WorldObjectSet"));
                IDFACTORY_TYPE  = IdFactoryType.valueOf(idSettings.getProperty("IDFactory", "Compaction"));
                BAD_ID_CHECKING = Boolean.parseBoolean(idSettings.getProperty("BadIdChecking", "True"));
            }
            catch (Exception e)
            {
                e.printStackTrace();
                throw new Error("Failed to Load "+ID_CONFIG_FILE+" File.");
            }

            // General Config File
            try
            {
                Properties General    		= new Properties();
                InputStream is              = new FileInputStream(new File(GENERAL_CONFIG_FILE));
                General.load(is);
                is.close();

                EVERYBODY_HAS_ADMIN_RIGHTS					= Boolean.parseBoolean(General.getProperty("EverybodyHasAdminRights", "false"));
                SERVER_LIST_BRACKET							= Boolean.parseBoolean(General.getProperty("ServerListBrackets", "false"));
                SERVER_LIST_CLOCK							= Boolean.parseBoolean(General.getProperty("ServerListClock", "false"));
                SERVER_GMONLY								= Boolean.parseBoolean(General.getProperty("ServerGMOnly", "false"));
                GM_HERO_AURA								= Boolean.parseBoolean(General.getProperty("GMHeroAura", "True"));
                GM_STARTUP_INVULNERABLE						= Boolean.parseBoolean(General.getProperty("GMStartupInvulnerable", "True"));
                GM_STARTUP_INVISIBLE						= Boolean.parseBoolean(General.getProperty("GMStartupInvisible", "True"));
                GM_STARTUP_SILENCE							= Boolean.parseBoolean(General.getProperty("GMStartupSilence", "True"));
                GM_STARTUP_AUTO_LIST						= Boolean.parseBoolean(General.getProperty("GMStartupAutoList", "True"));
                GM_ADMIN_MENU_STYLE							= General.getProperty("GMAdminMenuStyle", "modern");
                BYPASS_VALIDATION							= Boolean.parseBoolean(General.getProperty("BypassValidation", "True"));
                GAMEGUARD_ENFORCE							= Boolean.parseBoolean(General.getProperty("GameGuardEnforce", "False"));
                GAMEGUARD_PROHIBITACTION					= Boolean.parseBoolean(General.getProperty("GameGuardProhibitAction", "False"));
                LOG_CHAT									= Boolean.parseBoolean(General.getProperty("LogChat", "false"));
                LOG_ITEMS									= Boolean.parseBoolean(General.getProperty("LogItems", "false"));
                GMAUDIT										= Boolean.parseBoolean(General.getProperty("GMAudit", "False"));
                DEBUG										= Boolean.parseBoolean(General.getProperty("Debug", "false"));
                ASSERT										= Boolean.parseBoolean(General.getProperty("Assert", "false"));
                DEVELOPER									= Boolean.parseBoolean(General.getProperty("Developer", "false"));
                ACCEPT_GEOEDITOR_CONN						= Boolean.parseBoolean(General.getProperty("AcceptGeoeditorConn", "false"));
                TEST_SERVER									= Boolean.parseBoolean(General.getProperty("TestServer", "false"));
                SERVER_LIST_TESTSERVER						= Boolean.parseBoolean(General.getProperty("ListTestServers", "false"));
                ALT_DEV_NO_QUESTS							= Boolean.parseBoolean(General.getProperty("AltDevNoQuests", "False"));
                ALT_DEV_NO_SPAWNS							= Boolean.parseBoolean(General.getProperty("AltDevNoSpawns", "False"));
                THREAD_P_EFFECTS							= Integer.parseInt(General.getProperty("ThreadPoolSizeEffects", "6"));
                THREAD_P_GENERAL							= Integer.parseInt(General.getProperty("ThreadPoolSizeGeneral", "15"));
                IO_PACKET_THREAD_CORE_SIZE					=Integer.parseInt(General.getProperty("UrgentPacketThreadCoreSize", "2"));
                GENERAL_PACKET_THREAD_CORE_SIZE				= Integer.parseInt(General.getProperty("GeneralPacketThreadCoreSize", "4"));
                GENERAL_THREAD_CORE_SIZE					= Integer.parseInt(General.getProperty("GeneralThreadCoreSize", "4"));
                AI_MAX_THREAD								= Integer.parseInt(General.getProperty("AiMaxThread", "10"));
                PACKET_LIFETIME								= Integer.parseInt(General.getProperty("PacketLifeTime", "0"));
                FLOODPROTECTOR_INITIALSIZE					= Integer.parseInt(General.getProperty("FloodProtectorInitialSize", "50"));
                ALLOW_DISCARDITEM							= Boolean.parseBoolean(General.getProperty("AllowDiscardItem", "True"));
                AUTODESTROY_ITEM_AFTER						= Integer.parseInt(General.getProperty("AutoDestroyDroppedItemAfter", "0"));
                HERB_AUTO_DESTROY_TIME						= Integer.parseInt(General.getProperty("AutoDestroyHerbTime","15"))*1000;
                PROTECTED_ITEMS								= General.getProperty("ListOfProtectedItems");
                LIST_PROTECTED_ITEMS						= new FastList<Integer>();
                                							for (String id : PROTECTED_ITEMS.split(","))
                                							{
                                								LIST_PROTECTED_ITEMS.add(Integer.parseInt(id));
                                							}
                CHAR_STORE_INTERVAL							= Integer.parseInt(General.getProperty("CharacterDataStoreInterval", "15"));
                LAZY_ITEMS_UPDATE							= Boolean.parseBoolean(General.getProperty("LazyItemsUpdate", "false"));
                UPDATE_ITEMS_ON_CHAR_STORE					= Boolean.parseBoolean(General.getProperty("UpdateItemsOnCharStore", "false"));
                DESTROY_DROPPED_PLAYER_ITEM					= Boolean.parseBoolean(General.getProperty("DestroyPlayerDroppedItem", "false"));
                DESTROY_EQUIPABLE_PLAYER_ITEM				= Boolean.parseBoolean(General.getProperty("DestroyEquipableItem", "false"));
                SAVE_DROPPED_ITEM							= Boolean.parseBoolean(General.getProperty("SaveDroppedItem", "false"));
                EMPTY_DROPPED_ITEM_TABLE_AFTER_LOAD			= Boolean.parseBoolean(General.getProperty("EmptyDroppedItemTableAfterLoad", "false"));
                SAVE_DROPPED_ITEM_INTERVAL					= Integer.parseInt(General.getProperty("SaveDroppedItemInterval", "0"))*60000;
                CLEAR_DROPPED_ITEM_TABLE					= Boolean.parseBoolean(General.getProperty("ClearDroppedItemTable", "false"));
                AUTODELETE_INVALID_QUEST_DATA				= Boolean.parseBoolean(General.getProperty("AutoDeleteInvalidQuestData", "False"));
                PRECISE_DROP_CALCULATION					= Boolean.parseBoolean(General.getProperty("PreciseDropCalculation", "True"));
                MULTIPLE_ITEM_DROP							= Boolean.parseBoolean(General.getProperty("MultipleItemDrop", "True"));
                FORCE_INVENTORY_UPDATE						= Boolean.parseBoolean(General.getProperty("ForceInventoryUpdate", "False"));
                LAZY_CACHE									= Boolean.parseBoolean(General.getProperty("LazyCache", "False"));
                MIN_NPC_ANIMATION							= Integer.parseInt(General.getProperty("MinNPCAnimation", "10"));
                MAX_NPC_ANIMATION							= Integer.parseInt(General.getProperty("MaxNPCAnimation", "20"));
                MIN_MONSTER_ANIMATION						= Integer.parseInt(General.getProperty("MinMonsterAnimation", "5"));
                MAX_MONSTER_ANIMATION						= Integer.parseInt(General.getProperty("MaxMonsterAnimation", "20"));
                COORD_SYNCHRONIZE							= Integer.parseInt(General.getProperty("CoordSynchronize", "-1"));
                GRIDS_ALWAYS_ON								= Boolean.parseBoolean(General.getProperty("GridsAlwaysOn", "False"));
                GRID_NEIGHBOR_TURNON_TIME					= Integer.parseInt(General.getProperty("GridNeighborTurnOnTime", "1"));
                GRID_NEIGHBOR_TURNOFF_TIME					= Integer.parseInt(General.getProperty("GridNeighborTurnOffTime", "90"));
                GEODATA										= Integer.parseInt(General.getProperty("GeoData", "0"));
                FORCE_GEODATA								= Boolean.parseBoolean(General.getProperty("ForceGeoData", "True"));
                MOVE_BASED_KNOWNLIST						= Boolean.parseBoolean(General.getProperty("MoveBasedKnownlist", "False"));
                ZONE_TOWN									= Integer.parseInt(General.getProperty("ZoneTown", "0"));
                ACTIVATE_POSITION_RECORDER					= Boolean.parseBoolean(General.getProperty("ActivatePositionRecorder", "False"));
                DEFAULT_GLOBAL_CHAT							= General.getProperty("GlobalChat", "ON");
                DEFAULT_TRADE_CHAT							= General.getProperty("TradeChat", "ON");
                ALLOW_WAREHOUSE								= Boolean.parseBoolean(General.getProperty("AllowWarehouse", "True"));
                WAREHOUSE_CACHE								= Boolean.parseBoolean(General.getProperty("WarehouseCache", "False"));
                WAREHOUSE_CACHE_TIME						= Integer.parseInt(General.getProperty("WarehouseCacheTime", "15"));
                ALLOW_FREIGHT								= Boolean.parseBoolean(General.getProperty("AllowFreight", "True"));
                ALLOW_WEAR									= Boolean.parseBoolean(General.getProperty("AllowWear", "False"));
                WEAR_DELAY									= Integer.parseInt(General.getProperty("WearDelay", "5"));
                WEAR_PRICE									= Integer.parseInt(General.getProperty("WearPrice", "10"));
                ALLOW_LOTTERY								= Boolean.parseBoolean(General.getProperty("AllowLottery", "False"));
                ALLOW_RACE									= Boolean.parseBoolean(General.getProperty("AllowRace", "False"));
                ALLOW_WATER									= Boolean.parseBoolean(General.getProperty("AllowWater", "False"));
                ALLOW_RENTPET								= Boolean.parseBoolean(General.getProperty("AllowRentPet", "False"));
                FLOODPROTECTOR_INITIALSIZE					= Integer.parseInt(General.getProperty("FloodProtectorInitialSize", "50"));
                ALLOW_DISCARDITEM							= Boolean.parseBoolean(General.getProperty("AllowDiscardItem", "True"));
                ALLOWFISHING								= Boolean.parseBoolean(General.getProperty("AllowFishing", "False"));
                ALLOW_MANOR									= Boolean.parseBoolean(General.getProperty("AllowManor", "False"));
                ALLOW_BOAT									= Boolean.parseBoolean(General.getProperty("AllowBoat", "False"));
                ALLOW_CURSED_WEAPONS						= Boolean.parseBoolean(General.getProperty("AllowCursedWeapons", "False"));
                ALLOW_MANOR									= Boolean.parseBoolean(General.getProperty("AllowManor", "False"));
                ALLOW_NPC_WALKERS							= Boolean.parseBoolean(General.getProperty("AllowNpcWalkers", "true"));
                ALLOW_PET_WALKERS							= Boolean.parseBoolean(General.getProperty("AllowPetWalkers", "False"));
                SERVER_NEWS									= Boolean.parseBoolean(General.getProperty("ShowServerNews", "False"));
                COMMUNITY_TYPE								= General.getProperty("CommunityType", "old").toLowerCase();
                BBS_SHOW_PLAYERLIST							= Boolean.parseBoolean(General.getProperty("BBSShowPlayerList", "false"));
                BBS_DEFAULT									= General.getProperty("BBSDefault", "_bbshome");
                SHOW_LEVEL_COMMUNITYBOARD					= Boolean.parseBoolean(General.getProperty("ShowLevelOnCommunityBoard", "False"));
                SHOW_STATUS_COMMUNITYBOARD					= Boolean.parseBoolean(General.getProperty("ShowStatusOnCommunityBoard", "True"));
                NAME_PAGE_SIZE_COMMUNITYBOARD				= Integer.parseInt(General.getProperty("NamePageSizeOnCommunityBoard", "50"));
                NAME_PER_ROW_COMMUNITYBOARD					= Integer.parseInt(General.getProperty("NamePerRowOnCommunityBoard", "5"));
                ALT_OLY_START_TIME							= Integer.parseInt(General.getProperty("AltOlyStartTime", "18"));
                ALT_OLY_MIN									= Integer.parseInt(General.getProperty("AltOlyMin","00"));
                ALT_OLY_CPERIOD								= Long.parseLong(General.getProperty("AltOlyCPeriod","21600000"));
                ALT_OLY_BATTLE								= Long.parseLong(General.getProperty("AltOlyBattle","360000"));
                ALT_OLY_BWAIT								= Long.parseLong(General.getProperty("AltOlyBWait","600000"));
                ALT_OLY_IWAIT								= Long.parseLong(General.getProperty("AltOlyIWait","300000"));
                ALT_OLY_WPERIOD								= Long.parseLong(General.getProperty("AltOlyWPeriod","604800000"));
                ALT_OLY_VPERIOD								= Long.parseLong(General.getProperty("AltOlyVPeriod","86400000"));
                ALT_OLY_CLASSED								= Integer.parseInt(General.getProperty("AltOlyClassedParticipants","5"));
                ALT_OLY_NONCLASSED							= Integer.parseInt(General.getProperty("AltOlyNonClassedParticipants","9"));
                ALT_OLY_BATTLE_REWARD_ITEM					= Integer.parseInt(General.getProperty("AltOlyBattleRewItem","6651"));
                ALT_OLY_CLASSED_RITEM_C						= Integer.parseInt(General.getProperty("AltOlyClassedRewItemCount","50"));
                ALT_OLY_NONCLASSED_RITEM_C					= Integer.parseInt(General.getProperty("AltOlyNonClassedRewItemCount","30"));
                ALT_OLY_COMP_RITEM							= Integer.parseInt(General.getProperty("AltOlyCompRewItem","6651"));
                ALT_OLY_GP_PER_POINT						= Integer.parseInt(General.getProperty("AltOlyGPPerPoint","1000"));
                ALT_OLY_MIN_POINT_FOR_EXCH					= Integer.parseInt(General.getProperty("AltOlyMinPointForExchange","50"));
                ALT_OLY_HERO_POINTS							= Integer.parseInt(General.getProperty("AltOlyHeroPoints","300"));
                ALT_OLY_RESTRICTED_ITEMS					= General.getProperty("AltOlyRestrictedItems","0");
                LIST_OLY_RESTRICTED_ITEMS					= new FastList<Integer>();
                                							for (String id : ALT_OLY_RESTRICTED_ITEMS.split(","))
                                							{
                                								LIST_OLY_RESTRICTED_ITEMS.add(Integer.parseInt(id));
                                							}
                ALT_MANOR_REFRESH_TIME						= Integer.parseInt(General.getProperty("AltManorRefreshTime","20"));
                ALT_MANOR_REFRESH_MIN						= Integer.parseInt(General.getProperty("AltManorRefreshMin","00"));
                ALT_MANOR_APPROVE_TIME						= Integer.parseInt(General.getProperty("AltManorApproveTime","6"));
                ALT_MANOR_APPROVE_MIN						= Integer.parseInt(General.getProperty("AltManorApproveMin","00"));
                ALT_MANOR_MAINTENANCE_PERIOD				= Integer.parseInt(General.getProperty("AltManorMaintenancePeriod","360000"));
                ALT_MANOR_SAVE_ALL_ACTIONS					= Boolean.parseBoolean(General.getProperty("AltManorSaveAllActions","false"));
                ALT_MANOR_SAVE_PERIOD_RATE					= Integer.parseInt(General.getProperty("AltManorSavePeriodRate","2"));
                ALT_LOTTERY_PRIZE							= Integer.parseInt(General.getProperty("AltLotteryPrize","50000"));
                ALT_LOTTERY_TICKET_PRICE					= Integer.parseInt(General.getProperty("AltLotteryTicketPrice","2000"));
                ALT_LOTTERY_5_NUMBER_RATE					= Float.parseFloat(General.getProperty("AltLottery5NumberRate","0.6"));
                ALT_LOTTERY_4_NUMBER_RATE					= Float.parseFloat(General.getProperty("AltLottery4NumberRate","0.2"));
                ALT_LOTTERY_3_NUMBER_RATE					= Float.parseFloat(General.getProperty("AltLottery3NumberRate","0.2"));
                ALT_LOTTERY_2_AND_1_NUMBER_PRIZE			= Integer.parseInt(General.getProperty("AltLottery2and1NumberPrize","200"));
                RIFT_MIN_PARTY_SIZE							= Integer.parseInt(General.getProperty("RiftMinPartySize", "5"));
                RIFT_MAX_JUMPS								= Integer.parseInt(General.getProperty("MaxRiftJumps", "4"));
                RIFT_SPAWN_DELAY							= Integer.parseInt(General.getProperty("RiftSpawnDelay", "10000"));
                RIFT_AUTO_JUMPS_TIME_MIN					= Integer.parseInt(General.getProperty("AutoJumpsDelayMin", "480"));
                RIFT_AUTO_JUMPS_TIME_MAX					= Integer.parseInt(General.getProperty("AutoJumpsDelayMax", "600"));
                RIFT_BOSS_ROOM_TIME_MUTIPLY					= Float.parseFloat(General.getProperty("BossRoomTimeMultiply", "1.5"));
                RIFT_ENTER_COST_RECRUIT						= Integer.parseInt(General.getProperty("RecruitCost", "18"));
                RIFT_ENTER_COST_SOLDIER						= Integer.parseInt(General.getProperty("SoldierCost", "21"));
                RIFT_ENTER_COST_OFFICER						= Integer.parseInt(General.getProperty("OfficerCost", "24"));
                RIFT_ENTER_COST_CAPTAIN						= Integer.parseInt(General.getProperty("CaptainCost", "27"));
                RIFT_ENTER_COST_COMMANDER					= Integer.parseInt(General.getProperty("CommanderCost", "30"));
                RIFT_ENTER_COST_HERO						= Integer.parseInt(General.getProperty("HeroCost", "33"));
                DEFAULT_PUNISH								= Integer.parseInt(General.getProperty("DefaultPunish", "2"));
                DEFAULT_PUNISH_PARAM						= Integer.parseInt(General.getProperty("DefaultPunishParam", "0"));
                ONLY_GM_ITEMS_FREE							= Boolean.parseBoolean(General.getProperty("OnlyGMItemsFree", "True"));
                JAIL_IS_PVP									= Boolean.parseBoolean(General.getProperty("JailIsPvp", "True"));
                JAIL_DISABLE_CHAT							= Boolean.parseBoolean(General.getProperty("JailDisableChat", "True"));
                CUSTOM_SPAWNLIST_TABLE						= Boolean.valueOf(General.getProperty("CustomSpawnlistTable", "false"));
                SAVE_GMSPAWN_ON_CUSTOM						= Boolean.valueOf(General.getProperty("SaveGmSpawnOnCustom", "false"));
                DELETE_GMSPAWN_ON_CUSTOM					= Boolean.valueOf(General.getProperty("DeleteGmSpawnOnCustom", "false"));
                CUSTOM_NPC_TABLE							= Boolean.valueOf(General.getProperty("CustomNpcTable", "false"));
                CUSTOM_ETCITEM_TABLE						= Boolean.valueOf(General.getProperty("CustomEtcitemTable", "false"));
                CUSTOM_ARMOR_TABLE							= Boolean.valueOf(General.getProperty("CustomArmorTable", "false"));
                CUSTOM_ARMORSETS_TABLE						= Boolean.valueOf(General.getProperty("CustomArmorSetsTable", "false"));
                CUSTOM_WEAPON_TABLE							= Boolean.valueOf(General.getProperty("CustomWeaponTable", "false"));
                CUSTOM_TELEPORT_TABLE						= Boolean.valueOf(General.getProperty("CustomTeleportTable", "false"));
                CUSTOM_DROPLIST_TABLE						= Boolean.valueOf(General.getProperty("CustomDroplistTable", "false"));
                CUSTOM_MERCHANT_TABLES						= Boolean.valueOf(General.getProperty("CustomMerchantTables", "false"));
            }
            catch (Exception e)
            {
                e.printStackTrace();
                throw new Error("Failed to Load "+GENERAL_CONFIG_FILE+" File.");
            }
            
            // NPC settings
            try
            {
                Properties NPC  = new Properties();
                InputStream is          = new FileInputStream(new File(NPC_CONFIG_FILE));
                NPC.load(is);
                is.close();

                ANNOUNCE_MAMMON_SPAWN					= Boolean.parseBoolean(NPC.getProperty("AnnounceMammonSpawn", "True"));
                ALT_MOB_AGRO_IN_PEACEZONE				= Boolean.parseBoolean(NPC.getProperty("AltMobAgroInPeaceZone", "true"));
                ALT_GAME_VIEWNPC						= Boolean.parseBoolean(NPC.getProperty("AltGameViewNpc", "False"));
                MAX_DRIFT_RANGE							= Integer.parseInt(NPC.getProperty("MaxDriftRange", "300"));
                DEEPBLUE_DROP_RULES						= Boolean.parseBoolean(NPC.getProperty("UseDeepBlueDropRules", "True"));
                SHOW_NPC_LVL							= Boolean.parseBoolean(NPC.getProperty("ShowNpcLevel", "False"));
                GUARD_ATTACK_AGGRO_MOB					= Boolean.parseBoolean(NPC.getProperty("GuardAttackAggroMob", "False"));
                ALLOW_WYVERN_UPGRADER					= Boolean.parseBoolean(NPC.getProperty("AllowWyvernUpgrader", "False"));
                PET_RENT_NPC 							= NPC.getProperty("ListPetRentNpc", "30827");
                LIST_PET_RENT_NPC 						= new FastList<Integer>();
                                						for (String id : PET_RENT_NPC.split(","))
                                						{
                                							LIST_PET_RENT_NPC.add(Integer.parseInt(id));
                                						}
                WYVERN_SPEED							= Integer.parseInt(NPC.getProperty("WyvernSpeed", "100"));
                STRIDER_SPEED							= Integer.parseInt(NPC.getProperty("StriderSpeed", "80"));
                GREAT_WOLF_SPEED						= Integer.parseInt(NPC.getProperty("GreatWolfSpeed", "80"));
                RAID_HP_REGEN_MULTIPLIER				= Double.parseDouble(NPC.getProperty("RaidHpRegenMultiplier", "100")) /100;
                RAID_MP_REGEN_MULTIPLIER				= Double.parseDouble(NPC.getProperty("RaidMpRegenMultiplier", "100")) /100;
                RAID_PDEFENCE_MULTIPLIER				= Double.parseDouble(NPC.getProperty("RaidPDefenceMultiplier", "100")) /100;
                RAID_MDEFENCE_MULTIPLIER				= Double.parseDouble(NPC.getProperty("RaidMDefenceMultiplier", "100")) /100;
                RAID_MIN_RESPAWN_MULTIPLIER				= Float.parseFloat(NPC.getProperty("RaidMinRespawnMultiplier", "1.0"));
                RAID_MAX_RESPAWN_MULTIPLIER				= Float.parseFloat(NPC.getProperty("RaidMaxRespawnMultiplier", "1.0"));
                RAID_MINION_RESPAWN_TIMER				= Integer.parseInt(NPC.getProperty("RaidMinionRespawnTime", "300000"));

            }
            catch (Exception e)
            {
                e.printStackTrace();
                throw new Error("Failed to Load "+NPC_CONFIG_FILE+" File.");
            }

            // rates
            try
            {
                Properties ratesSettings    = new Properties();
                InputStream is              = new FileInputStream(new File(RATES_CONFIG_FILE));
                ratesSettings.load(is);
                is.close();

                RATE_XP                         = Float.parseFloat(ratesSettings.getProperty("RateXp", "1."));
                RATE_SP                         = Float.parseFloat(ratesSettings.getProperty("RateSp", "1."));
                RATE_PARTY_XP                   = Float.parseFloat(ratesSettings.getProperty("RatePartyXp", "1."));
                RATE_PARTY_SP                   = Float.parseFloat(ratesSettings.getProperty("RatePartySp", "1."));
                RATE_QUESTS_REWARD              = Float.parseFloat(ratesSettings.getProperty("RateQuestsReward", "1."));
                RATE_DROP_ADENA                 = Float.parseFloat(ratesSettings.getProperty("RateDropAdena", "1."));
                RATE_CONSUMABLE_COST            = Float.parseFloat(ratesSettings.getProperty("RateConsumableCost", "1."));
                RATE_DROP_ITEMS                 = Float.parseFloat(ratesSettings.getProperty("RateDropItems", "1."));
                RATE_DROP_ITEMS_BY_RAID         = Float.parseFloat(ratesSettings.getProperty("RateRaidDropItems", "1."));
                RATE_DROP_SPOIL                 = Float.parseFloat(ratesSettings.getProperty("RateDropSpoil", "1."));
                RATE_DROP_MANOR                 = Integer.parseInt(ratesSettings.getProperty("RateDropManor", "1"));
                RATE_DROP_QUEST                 = Float.parseFloat(ratesSettings.getProperty("RateDropQuest", "1."));
                RATE_KARMA_EXP_LOST             = Float.parseFloat(ratesSettings.getProperty("RateKarmaExpLost", "1."));
                RATE_SIEGE_GUARDS_PRICE         = Float.parseFloat(ratesSettings.getProperty("RateSiegeGuardsPrice", "1."));
                RATE_DROP_COMMON_HERBS             = Float.parseFloat(ratesSettings.getProperty("RateCommonHerbs", "15."));
                RATE_DROP_MP_HP_HERBS           = Float.parseFloat(ratesSettings.getProperty("RateHpMpHerbs", "10."));
                RATE_DROP_GREATER_HERBS         = Float.parseFloat(ratesSettings.getProperty("RateGreaterHerbs", "4."));
                RATE_DROP_SUPERIOR_HERBS        = Float.parseFloat(ratesSettings.getProperty("RateSuperiorHerbs", "0.8"))*10;
                RATE_DROP_SPECIAL_HERBS         = Float.parseFloat(ratesSettings.getProperty("RateSpecialHerbs", "0.2"))*10;

                PLAYER_DROP_LIMIT               = Integer.parseInt(ratesSettings.getProperty("PlayerDropLimit", "3"));
                PLAYER_RATE_DROP                = Integer.parseInt(ratesSettings.getProperty("PlayerRateDrop", "5"));
                PLAYER_RATE_DROP_ITEM           = Integer.parseInt(ratesSettings.getProperty("PlayerRateDropItem", "70"));
                PLAYER_RATE_DROP_EQUIP          = Integer.parseInt(ratesSettings.getProperty("PlayerRateDropEquip", "25"));
                PLAYER_RATE_DROP_EQUIP_WEAPON   = Integer.parseInt(ratesSettings.getProperty("PlayerRateDropEquipWeapon", "5"));

                PET_XP_RATE                     = Float.parseFloat(ratesSettings.getProperty("PetXpRate", "1."));
                PET_FOOD_RATE                   = Integer.parseInt(ratesSettings.getProperty("PetFoodRate", "1"));
                SINEATER_XP_RATE                = Float.parseFloat(ratesSettings.getProperty("SinEaterXpRate", "1."));

                KARMA_DROP_LIMIT                = Integer.parseInt(ratesSettings.getProperty("KarmaDropLimit", "10"));
                KARMA_RATE_DROP                 = Integer.parseInt(ratesSettings.getProperty("KarmaRateDrop", "70"));
                KARMA_RATE_DROP_ITEM            = Integer.parseInt(ratesSettings.getProperty("KarmaRateDropItem", "50"));
                KARMA_RATE_DROP_EQUIP           = Integer.parseInt(ratesSettings.getProperty("KarmaRateDropEquip", "40"));
                KARMA_RATE_DROP_EQUIP_WEAPON    = Integer.parseInt(ratesSettings.getProperty("KarmaRateDropEquipWeapon", "10"));

            }
            catch (Exception e) {
                e.printStackTrace();
                throw new Error("Failed to Load "+RATES_CONFIG_FILE+" File.");
            }
            
            try
            {
                Properties L2JModSettings  = new Properties();
                InputStream is          = new FileInputStream(new File(L2JMOD_CONFIG_FILE));
                L2JModSettings.load(is);
                is.close();

                L2JMOD_CHAMPION_ENABLE              	= Boolean.parseBoolean(L2JModSettings.getProperty("ChampionEnable", "false"));
                L2JMOD_CHAMPION_PASSIVE					= Boolean.parseBoolean(L2JModSettings.getProperty("ChampionPassive", "false"));
                L2JMOD_CHAMPION_FREQUENCY              	= Integer.parseInt(L2JModSettings.getProperty("ChampionFrequency", "0"));
                L2JMOD_CHAMP_TITLE                      = L2JModSettings.getProperty("ChampionTitle", "Champion");
                L2JMOD_CHAMP_MIN_LVL                	= Integer.parseInt(L2JModSettings.getProperty("ChampionMinLevel", "20"));
                L2JMOD_CHAMP_MAX_LVL                	= Integer.parseInt(L2JModSettings.getProperty("ChampionMaxLevel", "60"));
                L2JMOD_CHAMPION_HP                     	= Integer.parseInt(L2JModSettings.getProperty("ChampionHp", "7"));
                L2JMOD_CHAMPION_HP_REGEN               	= Float.parseFloat(L2JModSettings.getProperty("ChampionHpRegen", "1."));
                L2JMOD_CHAMPION_REWARDS                	= Integer.parseInt(L2JModSettings.getProperty("ChampionRewards", "8"));
                L2JMOD_CHAMPION_ADENAS_REWARDS         	= Integer.parseInt(L2JModSettings.getProperty("ChampionAdenasRewards", "1"));
                L2JMOD_CHAMPION_ATK                 	= Float.parseFloat(L2JModSettings.getProperty("ChampionAtk", "1."));
                L2JMOD_CHAMPION_SPD_ATK             	= Float.parseFloat(L2JModSettings.getProperty("ChampionSpdAtk", "1."));
                L2JMOD_CHAMPION_REWARD_LOWER_LVL_ITEM_CHANCE = Integer.parseInt(L2JModSettings.getProperty("ChampionRewardLowerLvlItemChance", "0"));
                L2JMOD_CHAMPION_REWARD_HIGHER_LVL_ITEM_CHANCE = Integer.parseInt(L2JModSettings.getProperty("ChampionRewardHigherLvlItemChance", "0"));
                L2JMOD_CHAMPION_REWARD_ID             	= Integer.parseInt(L2JModSettings.getProperty("ChampionRewardItemID", "6393"));
                L2JMOD_CHAMPION_REWARD_QTY            	= Integer.parseInt(L2JModSettings.getProperty("ChampionRewardItemQty", "1"));

                TVT_EVENT_ENABLED                        = Boolean.parseBoolean(L2JModSettings.getProperty("TvTEventEnabled", "false"));
                TVT_EVENT_INTERVAL                        = Integer.parseInt(L2JModSettings.getProperty("TvTEventInterval", "18000"));
                TVT_EVENT_PARTICIPATION_TIME            = Integer.parseInt(L2JModSettings.getProperty("TvTEventParticipationTime", "3600"));
                TVT_EVENT_RUNNING_TIME                    = Integer.parseInt(L2JModSettings.getProperty("TvTEventRunningTime", "1800"));
                TVT_EVENT_PARTICIPATION_NPC_ID            = Integer.parseInt(L2JModSettings.getProperty("TvTEventParticipationNpcId", "0"));

                /** L2JMOD Wedding system  */
                L2JMOD_ALLOW_WEDDING                    = Boolean.parseBoolean(L2JModSettings.getProperty("AllowWedding", "False"));
                L2JMOD_WEDDING_PRICE                    = Integer.parseInt(L2JModSettings.getProperty("WeddingPrice", "250000000"));
                L2JMOD_WEDDING_PUNISH_INFIDELITY        = Boolean.parseBoolean(L2JModSettings.getProperty("WeddingPunishInfidelity", "True"));
                L2JMOD_WEDDING_TELEPORT                 = Boolean.parseBoolean(L2JModSettings.getProperty("WeddingTeleport", "True"));
                L2JMOD_WEDDING_TELEPORT_PRICE           = Integer.parseInt(L2JModSettings.getProperty("WeddingTeleportPrice", "50000"));
                L2JMOD_WEDDING_TELEPORT_DURATION        = Integer.parseInt(L2JModSettings.getProperty("WeddingTeleportDuration", "60"));
                L2JMOD_WEDDING_SAMESEX                  = Boolean.parseBoolean(L2JModSettings.getProperty("WeddingAllowSameSex", "False"));
                L2JMOD_WEDDING_FORMALWEAR               = Boolean.parseBoolean(L2JModSettings.getProperty("WeddingFormalWear", "True"));
                L2JMOD_WEDDING_DIVORCE_COSTS            = Integer.parseInt(L2JModSettings.getProperty("WeddingDivorceCosts", "20"));
                
                L2JMOD_ENABLE_WAREHOUSESORTING_CLAN     = Boolean.valueOf(L2JModSettings.getProperty("EnableWarehouseSortingClan", "False"));
                L2JMOD_ENABLE_WAREHOUSESORTING_PRIVATE  = Boolean.valueOf(L2JModSettings.getProperty("EnableWarehouseSortingPrivate", "False"));
                L2JMOD_ENABLE_WAREHOUSESORTING_FREIGHT  = Boolean.valueOf(L2JModSettings.getProperty("EnableWarehouseSortingFreight", "False"));

                if (TVT_EVENT_PARTICIPATION_NPC_ID == 0)
                {
                    TVT_EVENT_ENABLED = false;
                    _log.warning("TvTEventEngine[Config.load()]: invalid config property -> TvTEventParticipationNpcId");
                }
                else
                {
                    String[] propertySplit                = L2JModSettings.getProperty("TvTEventParticipationNpcCoordinates", "0,0,0").split(",");

                    if (propertySplit.length < 3)
                    {
                        TVT_EVENT_ENABLED = false;
                        _log.warning("TvTEventEngine[Config.load()]: invalid config property -> TvTEventParticipationNpcCoordinates");
                    }
                    else
                    {
                        TVT_EVENT_PARTICIPATION_NPC_COORDINATES[0]  = Integer.parseInt(propertySplit[0]);
                        TVT_EVENT_PARTICIPATION_NPC_COORDINATES[1]  = Integer.parseInt(propertySplit[1]);
                        TVT_EVENT_PARTICIPATION_NPC_COORDINATES[2]  = Integer.parseInt(propertySplit[2]);

                        TVT_EVENT_MIN_PLAYERS_IN_TEAMS              = Integer.parseInt(L2JModSettings.getProperty("TvTEventMinPlayersInTeams", "1"));
                        TVT_EVENT_MAX_PLAYERS_IN_TEAMS              = Integer.parseInt(L2JModSettings.getProperty("TvTEventMaxPlayersInTeams", "20"));
                        TVT_EVENT_MIN_LVL							= (byte)Integer.parseInt(L2JModSettings.getProperty("TvTEventMinPlayerLevel", "1"));
                        TVT_EVENT_MAX_LVL							= (byte)Integer.parseInt(L2JModSettings.getProperty("TvTEventMaxPlayerLevel", "80"));
                        TVT_EVENT_RESPAWN_TELEPORT_DELAY            = Integer.parseInt(L2JModSettings.getProperty("TvTEventRespawnTeleportDelay", "20"));
                        TVT_EVENT_START_LEAVE_TELEPORT_DELAY        = Integer.parseInt(L2JModSettings.getProperty("TvTEventStartLeaveTeleportDelay", "20"));

                        TVT_EVENT_TEAM_1_NAME                       = L2JModSettings.getProperty("TvTEventTeam1Name", "Team1");
                        propertySplit                               = L2JModSettings.getProperty("TvTEventTeam1Coordinates", "0,0,0").split(",");

                        if (propertySplit.length < 3)
                        {
                            TVT_EVENT_ENABLED = false;
                            _log.warning("TvTEventEngine[Config.load()]: invalid config property -> TvTEventTeam1Coordinates");
                        }
                        else
                        {
                            TVT_EVENT_TEAM_1_COORDINATES[0]    = Integer.parseInt(propertySplit[0]);
                            TVT_EVENT_TEAM_1_COORDINATES[1]    = Integer.parseInt(propertySplit[1]);
                            TVT_EVENT_TEAM_1_COORDINATES[2]    = Integer.parseInt(propertySplit[2]);

                            TVT_EVENT_TEAM_2_NAME            = L2JModSettings.getProperty("TvTEventTeam2Name", "Team2");
                            propertySplit                    = L2JModSettings.getProperty("TvTEventTeam2Coordinates", "0,0,0").split(",");

                            if (propertySplit.length < 3)
                            {
                                TVT_EVENT_ENABLED= false;
                                _log.warning("TvTEventEngine[Config.load()]: invalid config property -> TvTEventTeam2Coordinates");
                            }
                            else
                            {
                                TVT_EVENT_TEAM_2_COORDINATES[0]    = Integer.parseInt(propertySplit[0]);
                                TVT_EVENT_TEAM_2_COORDINATES[1]    = Integer.parseInt(propertySplit[1]);
                                TVT_EVENT_TEAM_2_COORDINATES[2]    = Integer.parseInt(propertySplit[2]);
                                propertySplit                    = L2JModSettings.getProperty("TvTEventReward", "57,100000").split(";");

                                for (String reward : propertySplit)
                                {
                                	String[] rewardSplit = reward.split(",");

                                	if (rewardSplit.length != 2)
                                	    _log.warning("TvTEventEngine[Config.load()]: invalid config property -> TvTEventReward \"" + reward + "\"");
                                	else
                                	{
                                		try
                                		{
                                			TVT_EVENT_REWARDS.add(new int[]{Integer.parseInt(rewardSplit[0]), Integer.parseInt(rewardSplit[1])});
                                		}
                                		catch (NumberFormatException nfe)
                                		{
                                			if (!reward.equals(""))
                                			    _log.warning("TvTEventEngine[Config.load()]: invalid config property -> TvTEventReward \"" + reward + "\"");
                                		}
                                	}
                                }

                                TVT_EVENT_TARGET_TEAM_MEMBERS_ALLOWED	= Boolean.parseBoolean(L2JModSettings.getProperty("TvTEventTargetTeamMembersAllowed", "true"));
                                TVT_EVENT_SCROLL_ALLOWED				= Boolean.parseBoolean(L2JModSettings.getProperty("TvTEventScrollAllowed", "false"));
                                TVT_EVENT_POTIONS_ALLOWED				= Boolean.parseBoolean(L2JModSettings.getProperty("TvTEventPotionsAllowed", "false"));
                        		TVT_EVENT_SUMMON_BY_ITEM_ALLOWED		= Boolean.parseBoolean(L2JModSettings.getProperty("TvTEventSummonByItemAllowed", "false"));
                        		TVT_REWARD_TEAM_TIE						= Boolean.parseBoolean(L2JModSettings.getProperty("TvTRewardTeamTie", "false"));
                        		propertySplit							= L2JModSettings.getProperty("TvTDoorsToOpen", "").split(";");

                        		for (String door : propertySplit)
                        		{
                        			try
                        			{
                        				TVT_DOORS_IDS_TO_OPEN.add(Integer.parseInt(door));
                        			}
                        			catch (NumberFormatException nfe)
                        			{
                        				if (!door.equals(""))
                        				    _log.warning("TvTEventEngine[Config.load()]: invalid config property -> TvTDoorsToOpen \"" + door + "\"");
                        			}
                        		}
                        		
                        		propertySplit							= L2JModSettings.getProperty("TvTDoorsToClose", "").split(";");
                        		
                        		for (String door : propertySplit)
                        		{
                        			try
                        			{
                        				TVT_DOORS_IDS_TO_CLOSE.add(Integer.parseInt(door));
                        			}
                        			catch (NumberFormatException nfe)
                        			{
                        				if (!door.equals(""))
                        				    _log.warning("TvTEventEngine[Config.load()]: invalid config property -> TvTDoorsToClose \"" + door + "\"");
                        			}
                        		}
                            }
                        }
                    }
                }
                
                BANKING_SYSTEM_ENABLED	= Boolean.parseBoolean(L2JModSettings.getProperty("BankingEnabled", "false"));
                BANKING_SYSTEM_GOLDBARS	= Integer.parseInt(L2JModSettings.getProperty("BankingGoldbarCount", "1"));
                BANKING_SYSTEM_ADENA	= Integer.parseInt(L2JModSettings.getProperty("BankingAdenaCount", "500000000"));
            }
            catch (Exception e)
            {
                e.printStackTrace();
                throw new Error("Failed to Load "+L2JMOD_CONFIG_FILE+" File.");
            }
            // pvp config
            try
            {
                Properties pvpSettings      = new Properties();
                InputStream is              = new FileInputStream(new File(PVP_CONFIG_FILE));
                pvpSettings.load(is);
                is.close();

                /* KARMA SYSTEM */
                KARMA_MIN_KARMA     = Integer.parseInt(pvpSettings.getProperty("MinKarma", "240"));
                KARMA_MAX_KARMA     = Integer.parseInt(pvpSettings.getProperty("MaxKarma", "10000"));
                KARMA_XP_DIVIDER    = Integer.parseInt(pvpSettings.getProperty("XPDivider", "260"));
                KARMA_LOST_BASE     = Integer.parseInt(pvpSettings.getProperty("BaseKarmaLost", "0"));

                KARMA_DROP_GM               = Boolean.parseBoolean(pvpSettings.getProperty("CanGMDropEquipment", "false"));
                KARMA_AWARD_PK_KILL         = Boolean.parseBoolean(pvpSettings.getProperty("AwardPKKillPVPPoint", "true"));

                KARMA_PK_LIMIT                      = Integer.parseInt(pvpSettings.getProperty("MinimumPKRequiredToDrop", "5"));

                KARMA_NONDROPPABLE_PET_ITEMS    = pvpSettings.getProperty("ListOfPetItems", "2375,3500,3501,3502,4422,4423,4424,4425,6648,6649,6650,9882");
                KARMA_NONDROPPABLE_ITEMS        = pvpSettings.getProperty("ListOfNonDroppableItems", "57,1147,425,1146,461,10,2368,7,6,2370,2369,6842,6611,6612,6613,6614,6615,6616,6617,6618,6619,6620,6621,7694,8181,5575,7694,9388,9389,9390");

                KARMA_LIST_NONDROPPABLE_PET_ITEMS = new FastList<Integer>();
                for (String id : KARMA_NONDROPPABLE_PET_ITEMS.split(",")) {
                    KARMA_LIST_NONDROPPABLE_PET_ITEMS.add(Integer.parseInt(id));
                }

                KARMA_LIST_NONDROPPABLE_ITEMS = new FastList<Integer>();
                for (String id : KARMA_NONDROPPABLE_ITEMS.split(",")) {
                    KARMA_LIST_NONDROPPABLE_ITEMS.add(Integer.parseInt(id));
                }

                PVP_NORMAL_TIME = Integer.parseInt(pvpSettings.getProperty("PvPVsNormalTime", "15000"));
                PVP_PVP_TIME = Integer.parseInt(pvpSettings.getProperty("PvPVsPvPTime", "30000"));
            }
            catch (Exception e)
            {
                e.printStackTrace();
                throw new Error("Failed to Load "+PVP_CONFIG_FILE+" File.");
            }

            try
            {
                Properties Settings   = new Properties();
                InputStream is          = new FileInputStream(HEXID_FILE);
                Settings.load(is);
                is.close();
                SERVER_ID = Integer.parseInt(Settings.getProperty("ServerID"));
                HEX_ID = new BigInteger(Settings.getProperty("HexID"), 16).toByteArray();
            }
            catch (Exception e)
            {
                _log.warning("Could not load HexID file ("+HEXID_FILE+"). Hopefully login will give us one.");
            }
        }
        else if(Server.serverMode == Server.MODE_LOGINSERVER)
        {
            _log.info("loading login config");
            try
            {
                Properties serverSettings    = new Properties();
                InputStream is               = new FileInputStream(new File(LOGIN_CONFIGURATION_FILE));
                serverSettings.load(is);
                is.close();

                GAME_SERVER_LOGIN_HOST = serverSettings.getProperty("LoginHostname","*");
                GAME_SERVER_LOGIN_PORT = Integer.parseInt(serverSettings.getProperty("LoginPort","9013"));

                LOGIN_BIND_ADDRESS     = serverSettings.getProperty("LoginserverHostname", "*");
                PORT_LOGIN            = Integer.parseInt(serverSettings.getProperty("LoginserverPort", "2106"));

                DEBUG        = Boolean.parseBoolean(serverSettings.getProperty("Debug", "false"));
                DEVELOPER    = Boolean.parseBoolean(serverSettings.getProperty("Developer", "false"));
                ASSERT       = Boolean.parseBoolean(serverSettings.getProperty("Assert", "false"));

                ACCEPT_NEW_GAMESERVER = Boolean.parseBoolean(serverSettings.getProperty("AcceptNewGameServer","True"));
                REQUEST_ID = Integer.parseInt(serverSettings.getProperty("RequestServerID","0"));
                ACCEPT_ALTERNATE_ID = Boolean.parseBoolean(serverSettings.getProperty("AcceptAlternateID","True"));

                LOGIN_TRY_BEFORE_BAN    = Integer.parseInt(serverSettings.getProperty("LoginTryBeforeBan", "10"));
                LOGIN_BLOCK_AFTER_BAN   = Integer.parseInt(serverSettings.getProperty("LoginBlockAfterBan", "600"));

                DATAPACK_ROOT    = new File(serverSettings.getProperty("DatapackRoot", ".")).getCanonicalFile(); //FIXME: in login?

                INTERNAL_HOSTNAME   = serverSettings.getProperty("InternalHostname", "localhost");
                EXTERNAL_HOSTNAME   = serverSettings.getProperty("ExternalHostname", "localhost");

                DATABASE_DRIVER             = serverSettings.getProperty("Driver", "com.mysql.jdbc.Driver");
                DATABASE_URL                = serverSettings.getProperty("URL", "jdbc:mysql://localhost/l2jdb");
                DATABASE_LOGIN              = serverSettings.getProperty("Login", "root");
                DATABASE_PASSWORD           = serverSettings.getProperty("Password", "");
                DATABASE_MAX_CONNECTIONS    = Integer.parseInt(serverSettings.getProperty("MaximumDbConnections", "10"));

                SHOW_LICENCE = Boolean.parseBoolean(serverSettings.getProperty("ShowLicence", "true"));
                IP_UPDATE_TIME                = Integer.parseInt(serverSettings.getProperty("IpUpdateTime","15"));
                FORCE_GGAUTH = Boolean.parseBoolean(serverSettings.getProperty("ForceGGAuth", "false"));

                AUTO_CREATE_ACCOUNTS = Boolean.parseBoolean(serverSettings.getProperty("AutoCreateAccounts","True"));

                FLOOD_PROTECTION = Boolean.parseBoolean(serverSettings.getProperty("EnableFloodProtection","True"));
                FAST_CONNECTION_LIMIT = Integer.parseInt(serverSettings.getProperty("FastConnectionLimit","15"));
                NORMAL_CONNECTION_TIME = Integer.parseInt(serverSettings.getProperty("NormalConnectionTime","700"));
                FAST_CONNECTION_TIME = Integer.parseInt(serverSettings.getProperty("FastConnectionTime","350"));
                MAX_CONNECTION_PER_IP = Integer.parseInt(serverSettings.getProperty("MaxConnectionPerIP","50"));
            }
            catch (Exception e)
            {
                e.printStackTrace();
                throw new Error("Failed to Load "+CONFIGURATION_FILE+" File.");
            }

            // telnet
            try
            {
                Properties telnetSettings   = new Properties();
                InputStream is              = new FileInputStream(new File(TELNET_FILE));
                telnetSettings.load(is);
                is.close();

                IS_TELNET_ENABLED   = Boolean.parseBoolean(telnetSettings.getProperty("EnableTelnet", "false"));
            }
            catch (Exception e)
            {
                e.printStackTrace();
                throw new Error("Failed to Load "+TELNET_FILE+" File.");
            }

        }
        else
        {
            _log.severe("Could not Load Config: server mode was not set");
        }

    }

    /**
     * Set a new value to a game parameter from the admin console.
     * @param pName (String) : name of the parameter to change
     * @param pValue (String) : new value of the parameter
     * @return boolean : true if modification has been made
     * @link useAdminCommand
     */
    public static boolean setParameterValue(String pName, String pValue)
    {
        // Server settings
        if (pName.equalsIgnoreCase("RateXp")) RATE_XP = Float.parseFloat(pValue);
        else if (pName.equalsIgnoreCase("RateSp")) RATE_SP = Float.parseFloat(pValue);
        else if (pName.equalsIgnoreCase("RatePartyXp")) RATE_PARTY_XP = Float.parseFloat(pValue);
        else if (pName.equalsIgnoreCase("RatePartySp")) RATE_PARTY_SP = Float.parseFloat(pValue);
        else if (pName.equalsIgnoreCase("RateQuestsReward")) RATE_QUESTS_REWARD = Float.parseFloat(pValue);
        else if (pName.equalsIgnoreCase("RateDropAdena")) RATE_DROP_ADENA = Float.parseFloat(pValue);
        else if (pName.equalsIgnoreCase("RateConsumableCost")) RATE_CONSUMABLE_COST = Float.parseFloat(pValue);
        else if (pName.equalsIgnoreCase("RateDropItems")) RATE_DROP_ITEMS = Float.parseFloat(pValue);
        else if (pName.equalsIgnoreCase("RateRaidDropItems")) RATE_DROP_ITEMS_BY_RAID = Float.parseFloat(pValue);
        else if (pName.equalsIgnoreCase("RateDropSpoil")) RATE_DROP_SPOIL = Float.parseFloat(pValue);
        else if (pName.equalsIgnoreCase("RateDropManor")) RATE_DROP_MANOR = Integer.parseInt(pValue);
        else if (pName.equalsIgnoreCase("RateDropQuest")) RATE_DROP_QUEST = Float.parseFloat(pValue);
        else if (pName.equalsIgnoreCase("RateKarmaExpLost")) RATE_KARMA_EXP_LOST = Float.parseFloat(pValue);
        else if (pName.equalsIgnoreCase("RateSiegeGuardsPrice")) RATE_SIEGE_GUARDS_PRICE = Float.parseFloat(pValue);

        else if (pName.equalsIgnoreCase("PlayerDropLimit")) PLAYER_DROP_LIMIT = Integer.parseInt(pValue);
        else if (pName.equalsIgnoreCase("PlayerRateDrop")) PLAYER_RATE_DROP = Integer.parseInt(pValue);
        else if (pName.equalsIgnoreCase("PlayerRateDropItem")) PLAYER_RATE_DROP_ITEM = Integer.parseInt(pValue);
        else if (pName.equalsIgnoreCase("PlayerRateDropEquip")) PLAYER_RATE_DROP_EQUIP = Integer.parseInt(pValue);
        else if (pName.equalsIgnoreCase("PlayerRateDropEquipWeapon")) PLAYER_RATE_DROP_EQUIP_WEAPON = Integer.parseInt(pValue);

        else if (pName.equalsIgnoreCase("KarmaDropLimit")) KARMA_DROP_LIMIT = Integer.parseInt(pValue);
        else if (pName.equalsIgnoreCase("KarmaRateDrop")) KARMA_RATE_DROP = Integer.parseInt(pValue);
        else if (pName.equalsIgnoreCase("KarmaRateDropItem")) KARMA_RATE_DROP_ITEM = Integer.parseInt(pValue);
        else if (pName.equalsIgnoreCase("KarmaRateDropEquip")) KARMA_RATE_DROP_EQUIP = Integer.parseInt(pValue);
        else if (pName.equalsIgnoreCase("KarmaRateDropEquipWeapon")) KARMA_RATE_DROP_EQUIP_WEAPON = Integer.parseInt(pValue);

        else if (pName.equalsIgnoreCase("AutoDestroyDroppedItemAfter")) AUTODESTROY_ITEM_AFTER = Integer.parseInt(pValue);
        else if (pName.equalsIgnoreCase("DestroyPlayerDroppedItem")) DESTROY_DROPPED_PLAYER_ITEM = Boolean.parseBoolean(pValue);
        else if (pName.equalsIgnoreCase("DestroyEquipableItem")) DESTROY_EQUIPABLE_PLAYER_ITEM = Boolean.parseBoolean(pValue);
        else if (pName.equalsIgnoreCase("SaveDroppedItem")) SAVE_DROPPED_ITEM = Boolean.parseBoolean(pValue);
        else if (pName.equalsIgnoreCase("EmptyDroppedItemTableAfterLoad")) EMPTY_DROPPED_ITEM_TABLE_AFTER_LOAD = Boolean.parseBoolean(pValue);
        else if (pName.equalsIgnoreCase("SaveDroppedItemInterval")) SAVE_DROPPED_ITEM_INTERVAL = Integer.parseInt(pValue);
        else if (pName.equalsIgnoreCase("ClearDroppedItemTable")) CLEAR_DROPPED_ITEM_TABLE = Boolean.parseBoolean(pValue);

        else if (pName.equalsIgnoreCase("PreciseDropCalculation")) PRECISE_DROP_CALCULATION = Boolean.parseBoolean(pValue);
        else if (pName.equalsIgnoreCase("MultipleItemDrop")) MULTIPLE_ITEM_DROP = Boolean.parseBoolean(pValue);
        else if (pName.equalsIgnoreCase("CoordSynchronize")) COORD_SYNCHRONIZE = Integer.parseInt(pValue);
        else if (pName.equalsIgnoreCase("DeleteCharAfterDays")) DELETE_DAYS = Integer.parseInt(pValue);

        else if (pName.equalsIgnoreCase("AllowDiscardItem")) ALLOW_DISCARDITEM = Boolean.parseBoolean(pValue);
        else if (pName.equalsIgnoreCase("AllowFreight")) ALLOW_FREIGHT = Boolean.parseBoolean(pValue);
        else if (pName.equalsIgnoreCase("AllowWarehouse")) ALLOW_WAREHOUSE = Boolean.parseBoolean(pValue);
        else if (pName.equalsIgnoreCase("AllowWear")) ALLOW_WEAR = Boolean.parseBoolean(pValue);
        else if (pName.equalsIgnoreCase("WearDelay")) WEAR_DELAY = Integer.parseInt(pValue);
        else if (pName.equalsIgnoreCase("WearPrice")) WEAR_PRICE = Integer.parseInt(pValue);
        else if (pName.equalsIgnoreCase("AllowWater")) ALLOW_WATER = Boolean.parseBoolean(pValue);
        else if (pName.equalsIgnoreCase("AllowRentPet")) ALLOW_RENTPET = Boolean.parseBoolean(pValue);
        else if (pName.equalsIgnoreCase("AllowBoat")) ALLOW_BOAT = Boolean.parseBoolean(pValue);
        else if (pName.equalsIgnoreCase("AllowCursedWeapons")) ALLOW_CURSED_WEAPONS = Boolean.parseBoolean(pValue);
        else if (pName.equalsIgnoreCase("AllowManor")) ALLOW_MANOR = Boolean.parseBoolean(pValue);
        else if (pName.equalsIgnoreCase("AllowPetWalkers")) ALLOW_PET_WALKERS = Boolean.parseBoolean(pValue);
        else if (pName.equalsIgnoreCase("BypassValidation")) BYPASS_VALIDATION = Boolean.parseBoolean(pValue);

        else if (pName.equalsIgnoreCase("CommunityType")) COMMUNITY_TYPE = pValue.toLowerCase();
        else if (pName.equalsIgnoreCase("BBSShowPlayerList")) BBS_SHOW_PLAYERLIST = Boolean.parseBoolean(pValue);
        else if (pName.equalsIgnoreCase("BBSDefault")) BBS_DEFAULT = pValue;
        else if (pName.equalsIgnoreCase("ShowLevelOnCommunityBoard")) SHOW_LEVEL_COMMUNITYBOARD = Boolean.parseBoolean(pValue);
        else if (pName.equalsIgnoreCase("ShowStatusOnCommunityBoard")) SHOW_STATUS_COMMUNITYBOARD = Boolean.parseBoolean(pValue);
        else if (pName.equalsIgnoreCase("NamePageSizeOnCommunityBoard")) NAME_PAGE_SIZE_COMMUNITYBOARD = Integer.parseInt(pValue);
        else if (pName.equalsIgnoreCase("NamePerRowOnCommunityBoard")) NAME_PER_ROW_COMMUNITYBOARD = Integer.parseInt(pValue);

        else if (pName.equalsIgnoreCase("ShowServerNews")) SERVER_NEWS = Boolean.parseBoolean(pValue);
        else if (pName.equalsIgnoreCase("ShowNpcLevel")) SHOW_NPC_LVL = Boolean.parseBoolean(pValue);

        else if (pName.equalsIgnoreCase("ForceInventoryUpdate")) FORCE_INVENTORY_UPDATE = Boolean.parseBoolean(pValue);

        else if (pName.equalsIgnoreCase("AutoDeleteInvalidQuestData")) AUTODELETE_INVALID_QUEST_DATA = Boolean.parseBoolean(pValue);

        else if (pName.equalsIgnoreCase("MaximumOnlineUsers")) MAXIMUM_ONLINE_USERS = Integer.parseInt(pValue);

        else if (pName.equalsIgnoreCase("ZoneTown")) ZONE_TOWN = Integer.parseInt(pValue);

        else if (pName.equalsIgnoreCase("MaximumUpdateDistance")) MINIMUM_UPDATE_DISTANCE = Integer.parseInt(pValue);
        else if (pName.equalsIgnoreCase("MinimumUpdateTime")) MINIMUN_UPDATE_TIME = Integer.parseInt(pValue);
        else if (pName.equalsIgnoreCase("CheckKnownList")) CHECK_KNOWN = Boolean.parseBoolean(pValue);
        else if (pName.equalsIgnoreCase("KnownListForgetDelay")) KNOWNLIST_FORGET_DELAY = Integer.parseInt(pValue);

        // Other settings
        else if (pName.equalsIgnoreCase("UseDeepBlueDropRules")) DEEPBLUE_DROP_RULES = Boolean.parseBoolean(pValue);
        else if (pName.equalsIgnoreCase("AllowGuards")) GUARD_ATTACK_AGGRO_MOB = Boolean.parseBoolean(pValue);
        else if (pName.equalsIgnoreCase("CancelLesserEffect")) EFFECT_CANCELING = Boolean.parseBoolean(pValue);
        else if (pName.equalsIgnoreCase("WyvernSpeed")) WYVERN_SPEED = Integer.parseInt(pValue);
        else if (pName.equalsIgnoreCase("StriderSpeed")) STRIDER_SPEED = Integer.parseInt(pValue);
        else if (pName.equalsIgnoreCase("WolfSpeed")) GREAT_WOLF_SPEED = Integer.parseInt(pValue);

        else if (pName.equalsIgnoreCase("MaximumSlotsForNoDwarf")) INVENTORY_MAXIMUM_NO_DWARF = Integer.parseInt(pValue);
        else if (pName.equalsIgnoreCase("MaximumSlotsForDwarf")) INVENTORY_MAXIMUM_DWARF = Integer.parseInt(pValue);
        else if (pName.equalsIgnoreCase("MaximumSlotsForGMPlayer")) INVENTORY_MAXIMUM_GM = Integer.parseInt(pValue);

        else if (pName.equalsIgnoreCase("MaximumWarehouseSlotsForNoDwarf")) WAREHOUSE_SLOTS_NO_DWARF = Integer.parseInt(pValue);
        else if (pName.equalsIgnoreCase("MaximumWarehouseSlotsForDwarf")) WAREHOUSE_SLOTS_DWARF = Integer.parseInt(pValue);
        else if (pName.equalsIgnoreCase("MaximumWarehouseSlotsForClan")) WAREHOUSE_SLOTS_CLAN = Integer.parseInt(pValue);
        else if (pName.equalsIgnoreCase("MaximumFreightSlots")) FREIGHT_SLOTS = Integer.parseInt(pValue);

        else if (pName.equalsIgnoreCase("EnchantChanceWeapon")) ENCHANT_CHANCE_WEAPON = Integer.parseInt(pValue);
        else if (pName.equalsIgnoreCase("EnchantChanceArmor")) ENCHANT_CHANCE_ARMOR = Integer.parseInt(pValue);
        else if (pName.equalsIgnoreCase("EnchantChanceJewelry")) ENCHANT_CHANCE_JEWELRY = Integer.parseInt(pValue);
        else if (pName.equalsIgnoreCase("EnchantMaxWeapon")) ENCHANT_MAX_WEAPON = Integer.parseInt(pValue);
        else if (pName.equalsIgnoreCase("EnchantMaxArmor")) ENCHANT_MAX_ARMOR = Integer.parseInt(pValue);
        else if (pName.equalsIgnoreCase("EnchantMaxJewelry")) ENCHANT_MAX_JEWELRY = Integer.parseInt(pValue);
        else if (pName.equalsIgnoreCase("EnchantSafeMax")) ENCHANT_SAFE_MAX = Integer.parseInt(pValue);
        else if (pName.equalsIgnoreCase("EnchantSafeMaxFull")) ENCHANT_SAFE_MAX_FULL = Integer.parseInt(pValue);

        else if (pName.equalsIgnoreCase("HpRegenMultiplier")) HP_REGEN_MULTIPLIER = Double.parseDouble(pValue);
        else if (pName.equalsIgnoreCase("MpRegenMultiplier")) MP_REGEN_MULTIPLIER = Double.parseDouble(pValue);
        else if (pName.equalsIgnoreCase("CpRegenMultiplier")) CP_REGEN_MULTIPLIER = Double.parseDouble(pValue);

        else if (pName.equalsIgnoreCase("RaidHpRegenMultiplier")) RAID_HP_REGEN_MULTIPLIER = Double.parseDouble(pValue);
        else if (pName.equalsIgnoreCase("RaidMpRegenMultiplier")) RAID_MP_REGEN_MULTIPLIER = Double.parseDouble(pValue);
        else if (pName.equalsIgnoreCase("RaidPDefenceMultiplier")) RAID_PDEFENCE_MULTIPLIER = Double.parseDouble(pValue) /100;
        else if (pName.equalsIgnoreCase("RaidMDefenceMultiplier")) RAID_MDEFENCE_MULTIPLIER = Double.parseDouble(pValue) /100;
        else if (pName.equalsIgnoreCase("RaidMinionRespawnTime")) RAID_MINION_RESPAWN_TIMER =Integer.parseInt(pValue);

        else if (pName.equalsIgnoreCase("StartingAdena")) STARTING_ADENA = Integer.parseInt(pValue);
        else if (pName.equalsIgnoreCase("UnstuckInterval")) UNSTUCK_INTERVAL = Integer.parseInt(pValue);

        else if (pName.equalsIgnoreCase("PlayerSpawnProtection")) PLAYER_SPAWN_PROTECTION = Integer.parseInt(pValue);
        else if (pName.equalsIgnoreCase("PlayerFakeDeathUpProtection")) PLAYER_FAKEDEATH_UP_PROTECTION = Integer.parseInt(pValue);

        else if (pName.equalsIgnoreCase("PartyXpCutoffMethod")) PARTY_XP_CUTOFF_METHOD = pValue;
        else if (pName.equalsIgnoreCase("PartyXpCutoffPercent")) PARTY_XP_CUTOFF_PERCENT = Double.parseDouble(pValue);
        else if (pName.equalsIgnoreCase("PartyXpCutoffLevel")) PARTY_XP_CUTOFF_LEVEL = Integer.parseInt(pValue);

        else if (pName.equalsIgnoreCase("RespawnRestoreCP")) RESPAWN_RESTORE_CP = Double.parseDouble(pValue) / 100;
        else if (pName.equalsIgnoreCase("RespawnRestoreHP")) RESPAWN_RESTORE_HP = Double.parseDouble(pValue) / 100;
        else if (pName.equalsIgnoreCase("RespawnRestoreMP")) RESPAWN_RESTORE_MP = Double.parseDouble(pValue) / 100;

        else if (pName.equalsIgnoreCase("MaxPvtStoreSellSlotsDwarf")) MAX_PVTSTORESELL_SLOTS_DWARF = Integer.parseInt(pValue);
        else if (pName.equalsIgnoreCase("MaxPvtStoreSellSlotsOther")) MAX_PVTSTORESELL_SLOTS_OTHER = Integer.parseInt(pValue);
        else if (pName.equalsIgnoreCase("MaxPvtStoreBuySlotsDwarf")) MAX_PVTSTOREBUY_SLOTS_DWARF = Integer.parseInt(pValue);
        else if (pName.equalsIgnoreCase("MaxPvtStoreBuySlotsOther")) MAX_PVTSTOREBUY_SLOTS_OTHER = Integer.parseInt(pValue);

        else if (pName.equalsIgnoreCase("StoreSkillCooltime")) STORE_SKILL_COOLTIME = Boolean.parseBoolean(pValue);
        else if (pName.equalsIgnoreCase("AnnounceMammonSpawn")) ANNOUNCE_MAMMON_SPAWN = Boolean.parseBoolean(pValue);

        // Alternative settings
        else if (pName.equalsIgnoreCase("AltGameTiredness")) ALT_GAME_TIREDNESS = Boolean.parseBoolean(pValue);
        else if (pName.equalsIgnoreCase("AltGameCreation")) ALT_GAME_CREATION = Boolean.parseBoolean(pValue);
        else if (pName.equalsIgnoreCase("AltGameCreationSpeed")) ALT_GAME_CREATION_SPEED = Double.parseDouble(pValue);
        else if (pName.equalsIgnoreCase("AltGameCreationXpRate")) ALT_GAME_CREATION_XP_RATE = Double.parseDouble(pValue);
        else if (pName.equalsIgnoreCase("AltGameCreationSpRate")) ALT_GAME_CREATION_SP_RATE = Double.parseDouble(pValue);
        else if (pName.equalsIgnoreCase("AltWeightLimit")) ALT_WEIGHT_LIMIT = Double.parseDouble(pValue);
        else if (pName.equalsIgnoreCase("AltBlacksmithUseRecipes")) ALT_BLACKSMITH_USE_RECIPES = Boolean.parseBoolean(pValue);
        else if (pName.equalsIgnoreCase("AltGameSkillLearn")) ALT_GAME_SKILL_LEARN = Boolean.parseBoolean(pValue);
        else if (pName.equalsIgnoreCase("RemoveCastleCirclets")) REMOVE_CASTLE_CIRCLETS = Boolean.parseBoolean(pValue);
        else if (pName.equalsIgnoreCase("AltGameCancelByHit"))
        {
            ALT_GAME_CANCEL_BOW     = pValue.equalsIgnoreCase("bow") || pValue.equalsIgnoreCase("all");
            ALT_GAME_CANCEL_CAST    = pValue.equalsIgnoreCase("cast") || pValue.equalsIgnoreCase("all");
        }

        else if (pName.equalsIgnoreCase("AltShieldBlocks")) ALT_GAME_SHIELD_BLOCKS = Boolean.parseBoolean(pValue);
        else if (pName.equalsIgnoreCase("AltPerfectShieldBlockRate")) ALT_PERFECT_SHLD_BLOCK = Integer.parseInt(pValue);
        else if (pName.equalsIgnoreCase("Delevel")) ALT_GAME_DELEVEL = Boolean.parseBoolean(pValue);
        else if (pName.equalsIgnoreCase("MagicFailures")) ALT_GAME_MAGICFAILURES = Boolean.parseBoolean(pValue);
        else if (pName.equalsIgnoreCase("AltMobAgroInPeaceZone")) ALT_MOB_AGRO_IN_PEACEZONE = Boolean.parseBoolean(pValue);

        else if (pName.equalsIgnoreCase("AltGameExponentXp")) ALT_GAME_EXPONENT_XP = Float.parseFloat(pValue);
        else if (pName.equalsIgnoreCase("AltGameExponentSp")) ALT_GAME_EXPONENT_SP = Float.parseFloat(pValue);

        else if (pName.equalsIgnoreCase("AllowClassMasters")) ALLOW_CLASS_MASTERS = Boolean.parseBoolean(pValue);
        else if (pName.equalsIgnoreCase("AltGameFreights")) ALT_GAME_FREIGHTS = Boolean.parseBoolean(pValue);
        else if (pName.equalsIgnoreCase("AltGameFreightPrice")) ALT_GAME_FREIGHT_PRICE = Integer.parseInt(pValue);
        else if (pName.equalsIgnoreCase("AltPartyRange")) ALT_PARTY_RANGE = Integer.parseInt(pValue);
        else if (pName.equalsIgnoreCase("AltPartyRange2")) ALT_PARTY_RANGE2 = Integer.parseInt(pValue);

        else if (pName.equalsIgnoreCase("CraftingEnabled")) IS_CRAFTING_ENABLED = Boolean.parseBoolean(pValue);
        else if (pName.equalsIgnoreCase("LifeCrystalNeeded")) LIFE_CRYSTAL_NEEDED = Boolean.parseBoolean(pValue);
        else if (pName.equalsIgnoreCase("SpBookNeeded")) SP_BOOK_NEEDED = Boolean.parseBoolean(pValue);
        else if (pName.equalsIgnoreCase("AutoLoot")) AUTO_LOOT = Boolean.parseBoolean(pValue);
        else if (pName.equalsIgnoreCase("AutoLootHerbs")) AUTO_LOOT_HERBS = Boolean.parseBoolean(pValue);

        else if (pName.equalsIgnoreCase("AltKarmaPlayerCanBeKilledInPeaceZone")) ALT_GAME_KARMA_PLAYER_CAN_BE_KILLED_IN_PEACEZONE = Boolean.parseBoolean(pValue);
        else if (pName.equalsIgnoreCase("AltKarmaPlayerCanShop")) ALT_GAME_KARMA_PLAYER_CAN_SHOP = Boolean.parseBoolean(pValue);
        else if (pName.equalsIgnoreCase("AltKarmaPlayerCanUseGK")) ALT_GAME_KARMA_PLAYER_CAN_USE_GK = Boolean.parseBoolean(pValue);
        else if (pName.equalsIgnoreCase("AltKarmaPlayerCanTeleport")) ALT_GAME_KARMA_PLAYER_CAN_TELEPORT = Boolean.parseBoolean(pValue);
        else if (pName.equalsIgnoreCase("AltKarmaPlayerCanTrade")) ALT_GAME_KARMA_PLAYER_CAN_TRADE = Boolean.parseBoolean(pValue);
        else if (pName.equalsIgnoreCase("AltKarmaPlayerCanUseWareHouse")) ALT_GAME_KARMA_PLAYER_CAN_USE_WAREHOUSE = Boolean.parseBoolean(pValue);
        else if (pName.equalsIgnoreCase("AltRequireCastleForDawn")) ALT_GAME_REQUIRE_CASTLE_DAWN = Boolean.parseBoolean(pValue);
        else if (pName.equalsIgnoreCase("AltRequireClanCastle")) ALT_GAME_REQUIRE_CLAN_CASTLE = Boolean.parseBoolean(pValue);
        else if (pName.equalsIgnoreCase("AltFreeTeleporting")) ALT_GAME_FREE_TELEPORT = Boolean.parseBoolean(pValue);
        else if (pName.equalsIgnoreCase("AltSubClassWithoutQuests")) ALT_GAME_SUBCLASS_WITHOUT_QUESTS = Boolean.parseBoolean(pValue);
        //else if (pName.equalsIgnoreCase("AltNewCharAlwaysIsNewbie")) ALT_GAME_NEW_CHAR_ALWAYS_IS_NEWBIE = Boolean.parseBoolean(pValue);
        else if (pName.equalsIgnoreCase("AltMembersCanWithdrawFromClanWH")) ALT_MEMBERS_CAN_WITHDRAW_FROM_CLANWH = Boolean.parseBoolean(pValue);
        else if (pName.equalsIgnoreCase("DwarfRecipeLimit")) DWARF_RECIPE_LIMIT = Integer.parseInt(pValue);
        else if (pName.equalsIgnoreCase("CommonRecipeLimit")) COMMON_RECIPE_LIMIT = Integer.parseInt(pValue);
        else if (pName.equalsIgnoreCase("CastleShieldRestriction")) CASTLE_SHIELD = Boolean.parseBoolean(pValue);
        else if (pName.equalsIgnoreCase("ClanHallShieldRestriction")) CLANHALL_SHIELD = Boolean.parseBoolean(pValue);
        else if (pName.equalsIgnoreCase("ApellaArmorsRestriction")) APELLA_ARMORS = Boolean.parseBoolean(pValue);
        else if (pName.equalsIgnoreCase("OathArmorsRestriction")) OATH_ARMORS = Boolean.parseBoolean(pValue);
        else if (pName.equalsIgnoreCase("CastleLordsCrownRestriction")) CASTLE_CROWN = Boolean.parseBoolean(pValue);
        else if (pName.equalsIgnoreCase("CastleCircletsRestriction")) CASTLE_CIRCLETS = Boolean.parseBoolean(pValue);

        //ChampionMobs L2JMOD
        else if (pName.equalsIgnoreCase("ChampionEnable")) L2JMOD_CHAMPION_ENABLE =  Boolean.parseBoolean(pValue);
        else if (pName.equalsIgnoreCase("ChampionFrequency")) L2JMOD_CHAMPION_FREQUENCY = Integer.parseInt(pValue);
        else if (pName.equalsIgnoreCase("ChampionMinLevel")) L2JMOD_CHAMP_MIN_LVL = Integer.parseInt(pValue);
        else if (pName.equalsIgnoreCase("ChampionMaxLevel")) L2JMOD_CHAMP_MAX_LVL = Integer.parseInt(pValue);
        else if (pName.equalsIgnoreCase("ChampionHp")) L2JMOD_CHAMPION_HP = Integer.parseInt(pValue);
        else if (pName.equalsIgnoreCase("ChampionHpRegen")) L2JMOD_CHAMPION_HP_REGEN = Float.parseFloat(pValue);
        else if (pName.equalsIgnoreCase("ChampionRewards")) L2JMOD_CHAMPION_REWARDS = Integer.parseInt(pValue);
        else if (pName.equalsIgnoreCase("ChampionAdenasRewards")) L2JMOD_CHAMPION_ADENAS_REWARDS = Integer.parseInt(pValue);
        else if (pName.equalsIgnoreCase("ChampionAtk")) L2JMOD_CHAMPION_ATK = Float.parseFloat(pValue);
        else if (pName.equalsIgnoreCase("ChampionSpdAtk")) L2JMOD_CHAMPION_SPD_ATK = Float.parseFloat(pValue);
        else if (pName.equalsIgnoreCase("ChampionRewardLowerLvlItemChance")) L2JMOD_CHAMPION_REWARD_LOWER_LVL_ITEM_CHANCE = Integer.parseInt(pValue);
        else if (pName.equalsIgnoreCase("ChampionRewardHigherLvlItemChance")) L2JMOD_CHAMPION_REWARD_HIGHER_LVL_ITEM_CHANCE = Integer.parseInt(pValue);
        else if (pName.equalsIgnoreCase("ChampionRewardItemID")) L2JMOD_CHAMPION_REWARD_ID = Integer.parseInt(pValue);
        else if (pName.equalsIgnoreCase("ChampionRewardItemQty")) L2JMOD_CHAMPION_REWARD_QTY = Integer.parseInt(pValue);

        // L2JMOD Wedding System
        else if (pName.equalsIgnoreCase("AllowWedding")) L2JMOD_ALLOW_WEDDING = Boolean.parseBoolean(pValue);
        else if (pName.equalsIgnoreCase("WeddingPrice")) L2JMOD_WEDDING_PRICE = Integer.parseInt(pValue);
        else if (pName.equalsIgnoreCase("WeddingPunishInfidelity")) L2JMOD_WEDDING_PUNISH_INFIDELITY = Boolean.parseBoolean(pValue);
        else if (pName.equalsIgnoreCase("WeddingTeleport")) L2JMOD_WEDDING_TELEPORT = Boolean.parseBoolean(pValue);
        else if (pName.equalsIgnoreCase("WeddingTeleportPrice")) L2JMOD_WEDDING_TELEPORT_PRICE = Integer.parseInt(pValue);
        else if (pName.equalsIgnoreCase("WeddingTeleportDuration")) L2JMOD_WEDDING_TELEPORT_DURATION = Integer.parseInt(pValue);
        else if (pName.equalsIgnoreCase("WeddingAllowSameSex")) L2JMOD_WEDDING_SAMESEX = Boolean.parseBoolean(pValue);
        else if (pName.equalsIgnoreCase("WeddingFormalWear")) L2JMOD_WEDDING_FORMALWEAR = Boolean.parseBoolean(pValue);
        else if (pName.equalsIgnoreCase("WeddingDivorceCosts")) L2JMOD_WEDDING_DIVORCE_COSTS = Integer.parseInt(pValue);
        else if (pName.equalsIgnoreCase("TvTEventEnabled")) TVT_EVENT_ENABLED = Boolean.parseBoolean(pValue);
        else if (pName.equalsIgnoreCase("TvTEventInterval")) TVT_EVENT_INTERVAL = Integer.parseInt(pValue);
        else if (pName.equalsIgnoreCase("TvTEventParticipationTime")) TVT_EVENT_PARTICIPATION_TIME = Integer.parseInt(pValue);
        else if (pName.equalsIgnoreCase("TvTEventRunningTime")) TVT_EVENT_RUNNING_TIME = Integer.parseInt(pValue);
        else if (pName.equalsIgnoreCase("TvTEventParticipationNpcId")) TVT_EVENT_PARTICIPATION_NPC_ID = Integer.parseInt(pValue);
        
        // L2JMOD Warehouse Sorting
        else if (pName.equalsIgnoreCase("EnableWarehouseSortingClan")) L2JMOD_ENABLE_WAREHOUSESORTING_CLAN = Boolean.parseBoolean(pValue);
        else if (pName.equalsIgnoreCase("EnableWarehouseSortingPrivate")) L2JMOD_ENABLE_WAREHOUSESORTING_PRIVATE = Boolean.parseBoolean(pValue);
        else if (pName.equalsIgnoreCase("EnableWarehouseSortingFreight")) L2JMOD_ENABLE_WAREHOUSESORTING_FREIGHT = Boolean.parseBoolean(pValue);

        // PvP settings
        else if (pName.equalsIgnoreCase("MinKarma")) KARMA_MIN_KARMA = Integer.parseInt(pValue);
        else if (pName.equalsIgnoreCase("MaxKarma")) KARMA_MAX_KARMA = Integer.parseInt(pValue);
        else if (pName.equalsIgnoreCase("XPDivider")) KARMA_XP_DIVIDER = Integer.parseInt(pValue);
        else if (pName.equalsIgnoreCase("BaseKarmaLost")) KARMA_LOST_BASE = Integer.parseInt(pValue);

        else if (pName.equalsIgnoreCase("CanGMDropEquipment")) KARMA_DROP_GM = Boolean.parseBoolean(pValue);
        else if (pName.equalsIgnoreCase("AwardPKKillPVPPoint")) KARMA_AWARD_PK_KILL = Boolean.parseBoolean(pValue);
        else if (pName.equalsIgnoreCase("MinimumPKRequiredToDrop")) KARMA_PK_LIMIT = Integer.parseInt(pValue);

        else if (pName.equalsIgnoreCase("PvPVsNormalTime")) PVP_NORMAL_TIME = Integer.parseInt(pValue);
        else if (pName.equalsIgnoreCase("PvPVsPvPTime")) PVP_PVP_TIME = Integer.parseInt(pValue);
        else if (pName.equalsIgnoreCase("GlobalChat")) DEFAULT_GLOBAL_CHAT = pValue;
        else if (pName.equalsIgnoreCase("TradeChat"))  DEFAULT_TRADE_CHAT = pValue;
        else if (pName.equalsIgnoreCase("MenuStyle"))  GM_ADMIN_MENU_STYLE = pValue;
        else return false;
        return true;
    }

    // it has no instances
    private Config() {}

    /**
     * Save hexadecimal ID of the server in the properties file.
     * @param string (String) : hexadecimal ID of the server to store
     * @see HEXID_FILE
     * @see saveHexid(String string, String fileName)
     * @link LoginServerThread
     */
    public static void saveHexid(int serverId, String string)
    {
        Config.saveHexid(serverId, string, HEXID_FILE);
    }

    /**
     * Save hexadecimal ID of the server in the properties file.
     * @param hexId (String) : hexadecimal ID of the server to store
     * @param fileName (String) : name of the properties file
     */
    public static void saveHexid(int serverId, String hexId, String fileName)
    {
        try
        {
            Properties hexSetting = new Properties();
            File file = new File(fileName);
            //Create a new empty file only if it doesn't exist
            file.createNewFile();
            OutputStream out = new FileOutputStream(file);
            hexSetting.setProperty("ServerID",String.valueOf(serverId));
            hexSetting.setProperty("HexID",hexId);
            hexSetting.store(out,"the hexID to auth into login");
            out.close();
        }
        catch (Exception e)
        {
            _log.warning("Failed to save hex id to "+fileName+" File.");
            e.printStackTrace();
        }
    }

}