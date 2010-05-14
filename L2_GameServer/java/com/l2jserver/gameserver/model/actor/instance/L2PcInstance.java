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
package com.l2jserver.gameserver.model.actor.instance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

import javolution.util.FastList;
import javolution.util.FastMap;

import com.l2jserver.Config;
import com.l2jserver.L2DatabaseFactory;
import com.l2jserver.gameserver.Announcements;
import com.l2jserver.gameserver.GameTimeController;
import com.l2jserver.gameserver.GeoData;
import com.l2jserver.gameserver.GmListTable;
import com.l2jserver.gameserver.ItemsAutoDestroy;
import com.l2jserver.gameserver.LoginServerThread;
import com.l2jserver.gameserver.RecipeController;
import com.l2jserver.gameserver.SevenSigns;
import com.l2jserver.gameserver.SevenSignsFestival;
import com.l2jserver.gameserver.ThreadPoolManager;
import com.l2jserver.gameserver.Universe;
import com.l2jserver.gameserver.ai.CtrlIntention;
import com.l2jserver.gameserver.ai.L2CharacterAI;
import com.l2jserver.gameserver.ai.L2PlayerAI;
import com.l2jserver.gameserver.ai.L2SummonAI;
import com.l2jserver.gameserver.cache.HtmCache;
import com.l2jserver.gameserver.cache.WarehouseCacheManager;
import com.l2jserver.gameserver.communitybbs.BB.Forum;
import com.l2jserver.gameserver.communitybbs.Manager.ForumsBBSManager;
import com.l2jserver.gameserver.communitybbs.Manager.RegionBBSManager;
import com.l2jserver.gameserver.datatables.AccessLevels;
import com.l2jserver.gameserver.datatables.AdminCommandAccessRights;
import com.l2jserver.gameserver.datatables.CharNameTable;
import com.l2jserver.gameserver.datatables.CharTemplateTable;
import com.l2jserver.gameserver.datatables.ClanTable;
import com.l2jserver.gameserver.datatables.EnchantGroupsTable;
import com.l2jserver.gameserver.datatables.FishTable;
import com.l2jserver.gameserver.datatables.HennaTable;
import com.l2jserver.gameserver.datatables.HeroSkillTable;
import com.l2jserver.gameserver.datatables.ItemTable;
import com.l2jserver.gameserver.datatables.MapRegionTable;
import com.l2jserver.gameserver.datatables.NobleSkillTable;
import com.l2jserver.gameserver.datatables.NpcTable;
import com.l2jserver.gameserver.datatables.PetDataTable;
import com.l2jserver.gameserver.datatables.SkillTable;
import com.l2jserver.gameserver.datatables.SkillTreeTable;
import com.l2jserver.gameserver.handler.IItemHandler;
import com.l2jserver.gameserver.handler.ItemHandler;
import com.l2jserver.gameserver.instancemanager.AntiFeedManager;
import com.l2jserver.gameserver.instancemanager.CastleManager;
import com.l2jserver.gameserver.instancemanager.CoupleManager;
import com.l2jserver.gameserver.instancemanager.CursedWeaponsManager;
import com.l2jserver.gameserver.instancemanager.DimensionalRiftManager;
import com.l2jserver.gameserver.instancemanager.DuelManager;
import com.l2jserver.gameserver.instancemanager.FortManager;
import com.l2jserver.gameserver.instancemanager.FortSiegeManager;
import com.l2jserver.gameserver.instancemanager.InstanceManager;
import com.l2jserver.gameserver.instancemanager.ItemsOnGroundManager;
import com.l2jserver.gameserver.instancemanager.QuestManager;
import com.l2jserver.gameserver.instancemanager.SiegeManager;
import com.l2jserver.gameserver.instancemanager.TerritoryWarManager;
import com.l2jserver.gameserver.model.BlockList;
import com.l2jserver.gameserver.model.Elementals;
import com.l2jserver.gameserver.model.FishData;
import com.l2jserver.gameserver.model.L2AccessLevel;
import com.l2jserver.gameserver.model.L2CharPosition;
import com.l2jserver.gameserver.model.L2Clan;
import com.l2jserver.gameserver.model.L2ClanMember;
import com.l2jserver.gameserver.model.L2Effect;
import com.l2jserver.gameserver.model.L2EnchantSkillLearn;
import com.l2jserver.gameserver.model.L2Fishing;
import com.l2jserver.gameserver.model.L2HennaInstance;
import com.l2jserver.gameserver.model.L2ItemInstance;
import com.l2jserver.gameserver.model.L2Macro;
import com.l2jserver.gameserver.model.L2ManufactureItem;
import com.l2jserver.gameserver.model.L2ManufactureList;
import com.l2jserver.gameserver.model.L2Object;
import com.l2jserver.gameserver.model.L2Party;
import com.l2jserver.gameserver.model.L2PetData;
import com.l2jserver.gameserver.model.L2Radar;
import com.l2jserver.gameserver.model.L2RecipeList;
import com.l2jserver.gameserver.model.L2Request;
import com.l2jserver.gameserver.model.L2ShortCut;
import com.l2jserver.gameserver.model.L2Skill;
import com.l2jserver.gameserver.model.L2SkillLearn;
import com.l2jserver.gameserver.model.L2Transformation;
import com.l2jserver.gameserver.model.L2UIKeysSettings;
import com.l2jserver.gameserver.model.L2World;
import com.l2jserver.gameserver.model.L2WorldRegion;
import com.l2jserver.gameserver.model.MacroList;
import com.l2jserver.gameserver.model.ShortCuts;
import com.l2jserver.gameserver.model.TerritoryWard;
import com.l2jserver.gameserver.model.TradeList;
import com.l2jserver.gameserver.model.L2Skill.SkillTargetType;
import com.l2jserver.gameserver.model.actor.L2Attackable;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.L2Decoy;
import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.model.actor.L2Playable;
import com.l2jserver.gameserver.model.actor.L2Summon;
import com.l2jserver.gameserver.model.actor.L2Trap;
import com.l2jserver.gameserver.model.actor.appearance.PcAppearance;
import com.l2jserver.gameserver.model.actor.knownlist.PcKnownList;
import com.l2jserver.gameserver.model.actor.position.PcPosition;
import com.l2jserver.gameserver.model.actor.stat.PcStat;
import com.l2jserver.gameserver.model.actor.status.PcStatus;
import com.l2jserver.gameserver.model.base.ClassId;
import com.l2jserver.gameserver.model.base.ClassLevel;
import com.l2jserver.gameserver.model.base.Experience;
import com.l2jserver.gameserver.model.base.PlayerClass;
import com.l2jserver.gameserver.model.base.Race;
import com.l2jserver.gameserver.model.base.SubClass;
import com.l2jserver.gameserver.model.entity.Castle;
import com.l2jserver.gameserver.model.entity.Duel;
import com.l2jserver.gameserver.model.entity.Fort;
import com.l2jserver.gameserver.model.entity.Instance;
import com.l2jserver.gameserver.model.entity.L2Event;
import com.l2jserver.gameserver.model.entity.Siege;
import com.l2jserver.gameserver.model.entity.TvTEvent;
import com.l2jserver.gameserver.model.itemcontainer.Inventory;
import com.l2jserver.gameserver.model.itemcontainer.ItemContainer;
import com.l2jserver.gameserver.model.itemcontainer.PcInventory;
import com.l2jserver.gameserver.model.itemcontainer.PcRefund;
import com.l2jserver.gameserver.model.itemcontainer.PcWarehouse;
import com.l2jserver.gameserver.model.itemcontainer.PetInventory;
import com.l2jserver.gameserver.model.olympiad.Olympiad;
import com.l2jserver.gameserver.model.quest.Quest;
import com.l2jserver.gameserver.model.quest.QuestState;
import com.l2jserver.gameserver.model.quest.State;
import com.l2jserver.gameserver.network.L2GameClient;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.communityserver.CommunityServerThread;
import com.l2jserver.gameserver.network.communityserver.writepackets.WorldInfo;
import com.l2jserver.gameserver.network.serverpackets.ActionFailed;
import com.l2jserver.gameserver.network.serverpackets.ChangeWaitType;
import com.l2jserver.gameserver.network.serverpackets.CharInfo;
import com.l2jserver.gameserver.network.serverpackets.ConfirmDlg;
import com.l2jserver.gameserver.network.serverpackets.EtcStatusUpdate;
import com.l2jserver.gameserver.network.serverpackets.ExAutoSoulShot;
import com.l2jserver.gameserver.network.serverpackets.ExBrExtraUserInfo;
import com.l2jserver.gameserver.network.serverpackets.ExDuelUpdateUserInfo;
import com.l2jserver.gameserver.network.serverpackets.ExFishingEnd;
import com.l2jserver.gameserver.network.serverpackets.ExFishingStart;
import com.l2jserver.gameserver.network.serverpackets.ExGetBookMarkInfoPacket;
import com.l2jserver.gameserver.network.serverpackets.ExGetOnAirShip;
import com.l2jserver.gameserver.network.serverpackets.ExOlympiadMode;
import com.l2jserver.gameserver.network.serverpackets.ExOlympiadUserInfo;
import com.l2jserver.gameserver.network.serverpackets.ExPrivateStoreSetWholeMsg;
import com.l2jserver.gameserver.network.serverpackets.ExSetCompassZoneCode;
import com.l2jserver.gameserver.network.serverpackets.ExSpawnEmitter;
import com.l2jserver.gameserver.network.serverpackets.ExStartScenePlayer;
import com.l2jserver.gameserver.network.serverpackets.ExStorageMaxCount;
import com.l2jserver.gameserver.network.serverpackets.ExVitalityPointInfo;
import com.l2jserver.gameserver.network.serverpackets.FriendStatusPacket;
import com.l2jserver.gameserver.network.serverpackets.GMHide;
import com.l2jserver.gameserver.network.serverpackets.GameGuardQuery;
import com.l2jserver.gameserver.network.serverpackets.GetOnVehicle;
import com.l2jserver.gameserver.network.serverpackets.HennaInfo;
import com.l2jserver.gameserver.network.serverpackets.InventoryUpdate;
import com.l2jserver.gameserver.network.serverpackets.ItemList;
import com.l2jserver.gameserver.network.serverpackets.L2GameServerPacket;
import com.l2jserver.gameserver.network.serverpackets.LeaveWorld;
import com.l2jserver.gameserver.network.serverpackets.MagicSkillUse;
import com.l2jserver.gameserver.network.serverpackets.NicknameChanged;
import com.l2jserver.gameserver.network.serverpackets.NpcHtmlMessage;
import com.l2jserver.gameserver.network.serverpackets.ObservationMode;
import com.l2jserver.gameserver.network.serverpackets.ObservationReturn;
import com.l2jserver.gameserver.network.serverpackets.PartySmallWindowUpdate;
import com.l2jserver.gameserver.network.serverpackets.PetInventoryUpdate;
import com.l2jserver.gameserver.network.serverpackets.PlaySound;
import com.l2jserver.gameserver.network.serverpackets.PledgeShowMemberListDelete;
import com.l2jserver.gameserver.network.serverpackets.PledgeShowMemberListUpdate;
import com.l2jserver.gameserver.network.serverpackets.PrivateStoreListBuy;
import com.l2jserver.gameserver.network.serverpackets.PrivateStoreListSell;
import com.l2jserver.gameserver.network.serverpackets.PrivateStoreManageListBuy;
import com.l2jserver.gameserver.network.serverpackets.PrivateStoreManageListSell;
import com.l2jserver.gameserver.network.serverpackets.PrivateStoreMsgBuy;
import com.l2jserver.gameserver.network.serverpackets.PrivateStoreMsgSell;
import com.l2jserver.gameserver.network.serverpackets.QuestList;
import com.l2jserver.gameserver.network.serverpackets.RecipeShopMsg;
import com.l2jserver.gameserver.network.serverpackets.RecipeShopSellList;
import com.l2jserver.gameserver.network.serverpackets.RelationChanged;
import com.l2jserver.gameserver.network.serverpackets.Ride;
import com.l2jserver.gameserver.network.serverpackets.ServerClose;
import com.l2jserver.gameserver.network.serverpackets.SetupGauge;
import com.l2jserver.gameserver.network.serverpackets.ShortBuffStatusUpdate;
import com.l2jserver.gameserver.network.serverpackets.ShortCutInit;
import com.l2jserver.gameserver.network.serverpackets.SkillCoolTime;
import com.l2jserver.gameserver.network.serverpackets.SkillList;
import com.l2jserver.gameserver.network.serverpackets.Snoop;
import com.l2jserver.gameserver.network.serverpackets.SocialAction;
import com.l2jserver.gameserver.network.serverpackets.StatusUpdate;
import com.l2jserver.gameserver.network.serverpackets.StopMove;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;
import com.l2jserver.gameserver.network.serverpackets.TargetSelected;
import com.l2jserver.gameserver.network.serverpackets.TargetUnselected;
import com.l2jserver.gameserver.network.serverpackets.TradeDone;
import com.l2jserver.gameserver.network.serverpackets.TradeOtherDone;
import com.l2jserver.gameserver.network.serverpackets.TradeStart;
import com.l2jserver.gameserver.network.serverpackets.UserInfo;
import com.l2jserver.gameserver.skills.AbnormalEffect;
import com.l2jserver.gameserver.skills.Formulas;
import com.l2jserver.gameserver.skills.Stats;
import com.l2jserver.gameserver.skills.l2skills.L2SkillSiegeFlag;
import com.l2jserver.gameserver.skills.l2skills.L2SkillSummon;
import com.l2jserver.gameserver.skills.l2skills.L2SkillTrap;
import com.l2jserver.gameserver.taskmanager.AttackStanceTaskManager;
import com.l2jserver.gameserver.templates.chars.L2PcTemplate;
import com.l2jserver.gameserver.templates.item.L2Armor;
import com.l2jserver.gameserver.templates.item.L2ArmorType;
import com.l2jserver.gameserver.templates.item.L2EtcItemType;
import com.l2jserver.gameserver.templates.item.L2Henna;
import com.l2jserver.gameserver.templates.item.L2Item;
import com.l2jserver.gameserver.templates.item.L2Weapon;
import com.l2jserver.gameserver.templates.item.L2WeaponType;
import com.l2jserver.gameserver.templates.skills.L2EffectType;
import com.l2jserver.gameserver.templates.skills.L2SkillType;
import com.l2jserver.gameserver.util.FloodProtectors;
import com.l2jserver.gameserver.util.Util;
import com.l2jserver.util.Point3D;
import com.l2jserver.util.Rnd;

/**
 * This class represents all player characters in the world.
 * There is always a client-thread connected to this (except if a player-store is activated upon logout).<BR><BR>
 *
 * @version $Revision: 1.66.2.41.2.33 $ $Date: 2005/04/11 10:06:09 $
 */
public final class L2PcInstance extends L2Playable
{
    // Character Skill SQL String Definitions:
	private static final String RESTORE_SKILLS_FOR_CHAR = "SELECT skill_id,skill_level FROM character_skills WHERE charId=? AND class_index=?";
	private static final String ADD_NEW_SKILL = "INSERT INTO character_skills (charId,skill_id,skill_level,class_index) VALUES (?,?,?,?)";
	private static final String UPDATE_CHARACTER_SKILL_LEVEL = "UPDATE character_skills SET skill_level=? WHERE skill_id=? AND charId=? AND class_index=?";
	private static final String DELETE_SKILL_FROM_CHAR = "DELETE FROM character_skills WHERE skill_id=? AND charId=? AND class_index=?";
	private static final String DELETE_CHAR_SKILLS = "DELETE FROM character_skills WHERE charId=? AND class_index=?";

	// Character Skill Save SQL String Definitions:
	private static final String ADD_SKILL_SAVE = "INSERT INTO character_skills_save (charId,skill_id,skill_level,effect_count,effect_cur_time,reuse_delay,systime,restore_type,class_index,buff_index) VALUES (?,?,?,?,?,?,?,?,?,?)";
	private static final String RESTORE_SKILL_SAVE = "SELECT skill_id,skill_level,effect_count,effect_cur_time, reuse_delay, systime, restore_type FROM character_skills_save WHERE charId=? AND class_index=? ORDER BY buff_index ASC";
	private static final String DELETE_SKILL_SAVE = "DELETE FROM character_skills_save WHERE charId=? AND class_index=?";

	// Character Character SQL String Definitions:
    private static final String INSERT_CHARACTER = "INSERT INTO characters (account_name,charId,char_name,level,maxHp,curHp,maxCp,curCp,maxMp,curMp,face,hairStyle,hairColor,sex,exp,sp,karma,fame,pvpkills,pkkills,clanid,race,classid,deletetime,cancraft,title,title_color,accesslevel,online,isin7sdungeon,clan_privs,wantspeace,base_class,newbie,nobless,power_grade,last_recom_date,createTime) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
	private static final String UPDATE_CHARACTER = "UPDATE characters SET level=?,maxHp=?,curHp=?,maxCp=?,curCp=?,maxMp=?,curMp=?,face=?,hairStyle=?,hairColor=?,sex=?,heading=?,x=?,y=?,z=?,exp=?,expBeforeDeath=?,sp=?,karma=?,fame=?,pvpkills=?,pkkills=?,rec_have=?,rec_left=?,clanid=?,race=?,classid=?,deletetime=?,title=?,title_color=?,accesslevel=?,online=?,isin7sdungeon=?,clan_privs=?,wantspeace=?,base_class=?,onlinetime=?,punish_level=?,punish_timer=?,newbie=?,nobless=?,power_grade=?,subpledge=?,last_recom_date=?,lvl_joined_academy=?,apprentice=?,sponsor=?,varka_ketra_ally=?,clan_join_expiry_time=?,clan_create_expiry_time=?,char_name=?,death_penalty_level=?,bookmarkslot=?,vitality_points=?,language=? WHERE charId=?";
    private static final String RESTORE_CHARACTER = "SELECT account_name, charId, char_name, level, maxHp, curHp, maxCp, curCp, maxMp, curMp, face, hairStyle, hairColor, sex, heading, x, y, z, exp, expBeforeDeath, sp, karma, fame, pvpkills, pkkills, clanid, race, classid, deletetime, cancraft, title, title_color, rec_have, rec_left, accesslevel, online, char_slot, lastAccess, clan_privs, wantspeace, base_class, onlinetime, isin7sdungeon, punish_level, punish_timer, newbie, nobless, power_grade, subpledge, last_recom_date, lvl_joined_academy, apprentice, sponsor, varka_ketra_ally,clan_join_expiry_time,clan_create_expiry_time,death_penalty_level,bookmarkslot,vitality_points,createTime,language FROM characters WHERE charId=?";

    // Character Teleport Bookmark:
    private static final String INSERT_TP_BOOKMARK = "INSERT INTO character_tpbookmark (charId,Id,x,y,z,icon,tag,name) values (?,?,?,?,?,?,?,?)";
    private static final String UPDATE_TP_BOOKMARK = "UPDATE character_tpbookmark SET icon=?,tag=?,name=? where charId=? AND Id=?";	
    private static final String RESTORE_TP_BOOKMARK = "SELECT Id,x,y,z,icon,tag,name FROM character_tpbookmark WHERE charId=?";	
    private static final String DELETE_TP_BOOKMARK = "DELETE FROM character_tpbookmark WHERE charId=? AND Id=?";
    
    
    // Character Subclass SQL String Definitions:
    private static final String RESTORE_CHAR_SUBCLASSES = "SELECT class_id,exp,sp,level,class_index FROM character_subclasses WHERE charId=? ORDER BY class_index ASC";
    private static final String ADD_CHAR_SUBCLASS = "INSERT INTO character_subclasses (charId,class_id,exp,sp,level,class_index) VALUES (?,?,?,?,?,?)";
    private static final String UPDATE_CHAR_SUBCLASS = "UPDATE character_subclasses SET exp=?,sp=?,level=?,class_id=? WHERE charId=? AND class_index =?";
    private static final String DELETE_CHAR_SUBCLASS = "DELETE FROM character_subclasses WHERE charId=? AND class_index=?";

    // Character Henna SQL String Definitions:
	private static final String RESTORE_CHAR_HENNAS = "SELECT slot,symbol_id FROM character_hennas WHERE charId=? AND class_index=?";
	private static final String ADD_CHAR_HENNA = "INSERT INTO character_hennas (charId,symbol_id,slot,class_index) VALUES (?,?,?,?)";
	private static final String DELETE_CHAR_HENNA = "DELETE FROM character_hennas WHERE charId=? AND slot=? AND class_index=?";
	private static final String DELETE_CHAR_HENNAS = "DELETE FROM character_hennas WHERE charId=? AND class_index=?";

	// Character Shortcut SQL String Definitions:
	private static final String DELETE_CHAR_SHORTCUTS = "DELETE FROM character_shortcuts WHERE charId=? AND class_index=?";

	// Character Recommendation SQL String Definitions:
	private static final String RESTORE_CHAR_RECOMS = "SELECT charId,target_id FROM character_recommends WHERE charId=?";
	private static final String ADD_CHAR_RECOM = "INSERT INTO character_recommends (charId,target_id) VALUES (?,?)";
	private static final String DELETE_CHAR_RECOMS = "DELETE FROM character_recommends WHERE charId=?";

	// Character Transformation SQL String Definitions:
	private static final String SELECT_CHAR_TRANSFORM = "SELECT transform_id FROM characters WHERE charId=?";
	private static final String UPDATE_CHAR_TRANSFORM = "UPDATE characters SET transform_id=? WHERE charId=?";

	public static final int REQUEST_TIMEOUT = 15;
	public static final int STORE_PRIVATE_NONE = 0;
	public static final int STORE_PRIVATE_SELL = 1;
	public static final int STORE_PRIVATE_BUY = 3;
	public static final int STORE_PRIVATE_MANUFACTURE = 5;
	public static final int STORE_PRIVATE_PACKAGE_SELL = 8;

	/** The table containing all minimum level needed for each Expertise (None, D, C, B, A, S, S80, S84)*/
    private static final int[] EXPERTISE_LEVELS =
    {
	    SkillTreeTable.getInstance().getExpertiseLevel(0), //NONE
	    SkillTreeTable.getInstance().getExpertiseLevel(1), //D
	    SkillTreeTable.getInstance().getExpertiseLevel(2), //C
	    SkillTreeTable.getInstance().getExpertiseLevel(3), //B
	    SkillTreeTable.getInstance().getExpertiseLevel(4), //A
	    SkillTreeTable.getInstance().getExpertiseLevel(5), //S
	    SkillTreeTable.getInstance().getExpertiseLevel(6), //S80
	    SkillTreeTable.getInstance().getExpertiseLevel(7)  //S84
    };

	private static final int[] COMMON_CRAFT_LEVELS = { 5, 20, 28, 36, 43, 49, 55, 62 };
	
	public class AIAccessor extends L2Character.AIAccessor
	{
		protected AIAccessor()
		{
			
		}
		
		public L2PcInstance getPlayer()
		{
			return L2PcInstance.this;
		}
		
		public void doPickupItem(L2Object object)
		{
			L2PcInstance.this.doPickupItem(object);
		}
		
		public void doInteract(L2Character target)
		{
			L2PcInstance.this.doInteract(target);
		}
		
		@Override
		public void doAttack(L2Character target)
		{
			super.doAttack(target);
			
			// cancel the recent fake-death protection instantly if the player attacks or casts spells
			getPlayer().setRecentFakeDeath(false);
		}
		
		@Override
		public void doCast(L2Skill skill)
		{
			super.doCast(skill);
			
			// cancel the recent fake-death protection instantly if the player attacks or casts spells
			getPlayer().setRecentFakeDeath(false);
		}
	}

	private L2GameClient _client;

	private String _accountName;
	private long _deleteTimer;
	private long _creationTime; 

	private String _lang = null;
	private String _htmlPrefix = null;

	private boolean _isOnline = false;
	private long _onlineTime;
	private long _onlineBeginTime;
	private long _lastAccess;
	private long _uptime;

	private final ReentrantLock _subclassLock = new ReentrantLock();
	protected int _baseClass;
	protected int _activeClass;
	protected int _classIndex = 0;
	
	/** data for mounted pets */
	private int _controlItemId;
	private L2PetData _data;
	private int _curFeed;
	protected Future<?> _mountFeedTask;
	private ScheduledFuture<?> _dismountTask;
	
	/** The list of sub-classes this character has. */
    private Map<Integer, SubClass> _subClasses;
    
	private PcAppearance _appearance;

	/** The Identifier of the L2PcInstance */
	private int _charId = 0x00030b7a;

	/** The Experience of the L2PcInstance before the last Death Penalty */
	private long _expBeforeDeath;

	/** The Karma of the L2PcInstance (if higher than 0, the name of the L2PcInstance appears in red) */
	private int _karma;

	/** The number of player killed during a PvP (the player killed was PvP Flagged) */
	private int _pvpKills;

	/** The PK counter of the L2PcInstance (= Number of non PvP Flagged player killed) */
	private int _pkKills;

	/** The PvP Flag state of the L2PcInstance (0=White, 1=Purple) */
	private byte _pvpFlag;
	
	/** The Fame of this L2PcInstance */
	private int _fame;
	private ScheduledFuture<?> _fameTask;

	/** Vitality recovery task */
	private ScheduledFuture<?> _vitalityTask;

	private ScheduledFuture<?> _teleportWatchdog;

	/** The Siege state of the L2PcInstance */
	private byte _siegeState = 0;

	/** The id of castle/fort which the L2PcInstance is registered for siege */
	private int _siegeSide = 0;

	private int _curWeightPenalty = 0;

	private int _lastCompassZone; // the last compass zone update send to the client

	private boolean _isIn7sDungeon = false;
	
	public int _bookmarkslot = 0; // The Teleport Bookmark Slot
	
	public FastList<TeleportBookmark> tpbookmark = new FastList<TeleportBookmark>();
	
	private final FloodProtectors _floodProtectors = new FloodProtectors(this);

    private PunishLevel _punishLevel = PunishLevel.NONE;
	private long _punishTimer = 0;
	private ScheduledFuture<?> _punishTask;
    
    public enum PunishLevel
	{
		NONE(0, ""),
		CHAT(1, "chat banned"),
		JAIL(2, "jailed"),
		CHAR(3, "banned"),
		ACC(4, "banned");
		
		private final int punValue;
		private final String punString;
		
		PunishLevel(int value, String string)
		{
			punValue = value;
			punString = string;
		}
		
		public int value()
		{
			return punValue;
		}
		
		public String string()
		{
			return punString;
		}
	}
	
	/** Olympiad */
	private boolean _inOlympiadMode = false;
	private boolean _OlympiadStart = false;
	private int _olympiadGameId = -1;
	private int _olympiadSide = -1;
	public int olyBuff = 0;

    /** Duel */
    private boolean _isInDuel = false;
    private int _duelState = Duel.DUELSTATE_NODUEL;
    private int _duelId = 0;
    private SystemMessageId _noDuelReason = SystemMessageId.THERE_IS_NO_OPPONENT_TO_RECEIVE_YOUR_CHALLENGE_FOR_A_DUEL;

	/** Boat */
	private boolean _inBoat;
    private L2BoatInstance _boat;
    private Point3D _inBoatPosition;
    
    /** AirShip */
    private L2AirShipInstance _airShip;
	private boolean _inAirShip;
	private Point3D _inAirShipPosition;

	public ScheduledFuture<?> _taskforfish;
	private int _mountType;
    private int _mountNpcId;
    private int _mountLevel;
    /** Store object used to summon the strider you are mounting **/
	private int _mountObjectID = 0;

	public int _telemode = 0;

	public boolean _exploring = false;
	
	private boolean _inCrystallize;
	private boolean _inCraftMode;
    
    private L2Transformation _transformation;
    
    private int _transformationId = 0;

	/** The table containing all L2RecipeList of the L2PcInstance */
	private Map<Integer, L2RecipeList> _dwarvenRecipeBook = new FastMap<Integer, L2RecipeList>();
	private Map<Integer, L2RecipeList> _commonRecipeBook = new FastMap<Integer, L2RecipeList>();

		/** True if the L2PcInstance is sitting */
	private boolean _waitTypeSitting;

	/** True if the L2PcInstance is using the relax skill */
	private boolean _relax;

	/** Location before entering Observer Mode */
	private int _obsX;
	private int _obsY;
	private int _obsZ;
	private boolean _observerMode = false;

	/** Stored from last ValidatePosition **/
	private Point3D _lastServerPosition = new Point3D(0, 0, 0);

	/** Previous coordinate sent to party in ValidatePosition **/
	private Point3D _lastPartyPosition = new Point3D(0, 0, 0);
	
	/** The number of recommendation obtained by the L2PcInstance */
	private int _recomHave; // how much I was recommended by others

	/** The number of recommendation that the L2PcInstance can give */
	private int _recomLeft; // how many recommendations I can give to others

	/** Date when recommendation points were updated last time */
	private long _lastRecomUpdate;
	/** List with the recommendations that I've give */
	private List<Integer> _recomChars = new FastList<Integer>();

	/** The random number of the L2PcInstance */
	//private static final Random _rnd = new Random();

	private PcInventory _inventory = new PcInventory(this);
	private PcWarehouse _warehouse;
	private PcRefund _refund;

	/** The Private Store type of the L2PcInstance (STORE_PRIVATE_NONE=0, STORE_PRIVATE_SELL=1, sellmanage=2, STORE_PRIVATE_BUY=3, buymanage=4, STORE_PRIVATE_MANUFACTURE=5) */
	private int _privatestore;

	private TradeList _activeTradeList;
	private ItemContainer _activeWarehouse;
	private L2ManufactureList _createList;
	private TradeList _sellList;
	private TradeList _buyList;

	/** Bitmask used to keep track of one-time/newbie quest rewards*/
	private int _newbie;

	private boolean _noble = false;
	private boolean _hero = false;

	/** The L2FolkInstance corresponding to the last Folk wich one the player talked. */
	private L2Npc _lastFolkNpc = null;

	/** Last NPC Id talked on a quest */
	private int _questNpcObject = 0;

	/** The table containing all Quests began by the L2PcInstance */
	private Map<String, QuestState> _quests = new FastMap<String, QuestState>();

	/** The list containing all shortCuts of this L2PcInstance */
	private ShortCuts _shortCuts = new ShortCuts(this);

	/** The list containing all macroses of this L2PcInstance */
	private MacroList _macroses = new MacroList(this);

	private List<L2PcInstance> _snoopListener = new FastList<L2PcInstance>();
	private List<L2PcInstance> _snoopedPlayer = new FastList<L2PcInstance>();

	private ClassId _skillLearningClassId;

	// hennas
	private final L2HennaInstance[] _henna = new L2HennaInstance[3];
	private int _hennaSTR;
	private int _hennaINT;
	private int _hennaDEX;
	private int _hennaMEN;
	private int _hennaWIT;
	private int _hennaCON;

	/** The L2Summon of the L2PcInstance */
	private L2Summon _summon = null;
	/** The L2Decoy of the L2PcInstance */
	private L2Decoy _decoy = null;
	/** The L2Trap of the L2PcInstance */
	private L2Trap _trap = null;
	/** The L2Agathion of the L2PcInstance */
	private int _agathionId = 0;
	// apparently, a L2PcInstance CAN have both a summon AND a tamed beast at the same time!!
	private L2TamedBeastInstance _tamedBeast = null;

	private boolean _minimapAllowed = false;

	// client radar
	//TODO: This needs to be better intergrated and saved/loaded
	private L2Radar _radar;

	// these values are only stored temporarily
	private boolean _partyMatchingAutomaticRegistration;
	private boolean _partyMatchingShowLevel;
	private boolean _partyMatchingShowClass;
	private String _partyMatchingMemo;

	// Clan related attributes
	/** The Clan Identifier of the L2PcInstance */
	private int _clanId;

	/** The Clan object of the L2PcInstance */
	private L2Clan _clan;

	/** Apprentice and Sponsor IDs */
    private int _apprentice = 0;
    private int _sponsor = 0;

	private long _clanJoinExpiryTime;
	private long _clanCreateExpiryTime;

	private int _powerGrade = 0;
	private int _clanPrivileges = 0;

    /** L2PcInstance's pledge class (knight, Baron, etc.)*/
    private int _pledgeClass = 0;
    private int _pledgeType = 0;

    /** Level at which the player joined the clan as an academy member*/
    private int _lvlJoinedAcademy = 0;

	private int _wantsPeace = 0;

	//Death Penalty Buff Level
	private int _deathPenaltyBuffLevel = 0;
	
	// self resurrect during siege
	private boolean _charmOfCourage = false;

	// charges
	private AtomicInteger _charges = new AtomicInteger();
	private ScheduledFuture<?> _chargeTask = null;
	
	// Absorbed Souls
    private int _souls = 0;
    private ScheduledFuture<?> _soulTask = null;

    // WorldPosition used by TARGET_SIGNET_GROUND
    private Point3D _currentSkillWorldPosition;
    
    private L2AccessLevel _accessLevel;

	private boolean _messageRefusal = false;    // message refusal mode
	
	private boolean _silenceMode = false;     // silence mode
	private boolean _dietMode = false;          // ignore weight penalty
	private boolean _tradeRefusal = false;       // Trade refusal
	private boolean _exchangeRefusal = false;   // Exchange refusal

	private L2Party _party;

	// this is needed to find the inviting player for Party response
	// there can only be one active party request at once
	private L2PcInstance _activeRequester;
	private long _requestExpireTime = 0;
	private L2Request _request = new L2Request(this);
	private L2ItemInstance _arrowItem;
    private L2ItemInstance _boltItem;

	// Used for protection after teleport
	private long _protectEndTime = 0;
	public boolean isSpawnProtected() { return (_protectEndTime > 0); }

	// protects a char from agro mobs when getting up from fake death
	private long _recentFakeDeathEndTime = 0;
	private boolean _isFakeDeath;

	/** The fists L2Weapon of the L2PcInstance (used when no weapon is equiped) */
	private L2Weapon _fistsWeaponItem;

	private final Map<Integer, String> _chars = new FastMap<Integer, String>();

	//private byte _updateKnownCounter = 0;

	/** The current higher Expertise of the L2PcInstance (None=0, D=1, C=2, B=3, A=4, S=5, S80=6, S84=7)*/
	private int _expertiseIndex; // index in EXPERTISE_LEVELS
	private int _expertiseArmorPenalty = 0;
	private int _expertiseWeaponPenalty = 0;

	private boolean _isEnchanting = false;
	private L2ItemInstance _activeEnchantItem = null;
	private L2ItemInstance _activeEnchantSupportItem = null;
	private L2ItemInstance _activeEnchantAttrItem = null;
	private long _activeEnchantTimestamp = 0;

	protected boolean _inventoryDisable = false;

	protected Map<Integer, L2CubicInstance> _cubics = new FastMap<Integer, L2CubicInstance>().shared();

	/** Active shots. A FastSet variable would actually suffice but this was changed to fix threading stability... */
	protected Map<Integer, Integer> _activeSoulShots = new FastMap<Integer, Integer>().shared();

	public final ReentrantLock soulShotLock = new ReentrantLock();

	/** Event parameters */
	public int eventX;
	public int eventY;
	public int eventZ;
	public int eventkarma;
	public int eventpvpkills;
	public int eventpkkills;
	public String eventTitle;
	public LinkedList<String> kills = new LinkedList<String>();
	public boolean eventSitForced = false;
	public boolean atEvent = false;

	/** new loto ticket **/
	private int _loto[] = new int[5];
	//public static int _loto_nums[] = {0,1,2,3,4,5,6,7,8,9,};
	/** new race ticket **/
	private int _race[] = new int[2];

	private final BlockList _blockList = new BlockList(this);

	private int _team = 0;

    /** lvl of alliance with ketra orcs or varka silenos, used in quests and aggro checks
     *  [-5,-1] varka, 0 neutral, [1,5] ketra
     * */
    private int _alliedVarkaKetra = 0;

    private L2Fishing _fishCombat;
    private boolean _fishing = false;
    private int _fishx = 0;
    private int _fishy = 0;
    private int _fishz = 0;

    private int[] _transformAllowedSkills = {};
    private ScheduledFuture<?> _taskRentPet;
    private ScheduledFuture<?> _taskWater;

	/** Bypass validations */
	private List<String> _validBypass = new FastList<String>();
	private List<String> _validBypass2 = new FastList<String>();

	private Forum _forumMail;
	private Forum _forumMemo;

    /** Current skill in use. Note that L2Character has _lastSkillCast, but
     * this has the button presses */
	private SkillDat _currentSkill;
    private SkillDat _currentPetSkill;

    /** Skills queued because a skill is already in progress */
    private SkillDat _queuedSkill;

    /* Flag to disable equipment/skills while wearing formal wear **/
    private boolean _IsWearingFormalWear = false;

	private int _cursedWeaponEquippedId = 0;
	private boolean _combatFlagEquippedId = false;

	private int _reviveRequested = 0;
	private double _revivePower = 0;
	private boolean _revivePet = false;

	private double _cpUpdateIncCheck = .0;
	private double _cpUpdateDecCheck = .0;
	private double _cpUpdateInterval = .0;
	private double _mpUpdateIncCheck = .0;
	private double _mpUpdateDecCheck = .0;
	private double _mpUpdateInterval = .0;
	
	/* not used any more
	private boolean _isRidingFenrirWolf	= false;
	private boolean _isRidingWFenrirWolf = false;
	private boolean _isRidingGreatSnowWolf = false;
	*/
	private boolean _isRidingStrider = false;
	private boolean _isFlyingMounted = false;
	
	/** Char Coords from Client */
	private int _clientX;
	private int _clientY;
	private int _clientZ;
	private int _clientHeading;
	
	private int _movieId = 0;
	
	// Character UI
	private L2UIKeysSettings _uiKeySettings;
	
	/** Herbs Task Time **/
	private int _herbstask = 0;
	/** Task for Herbs */
	public class HerbTask implements Runnable
	{
		private String _process;
		private int _itemId;
		private long _count;
		private L2Object _reference;
		private boolean _sendMessage;
		
		HerbTask(String process, int itemId, long count, L2Object reference, boolean sendMessage)
		{
			_process = process;
			_itemId = itemId;
			_count = count;
			_reference = reference;
			_sendMessage = sendMessage;
		}
		
		public void run()
		{
			try
			{
				addItem(_process, _itemId, _count, _reference, _sendMessage);
			}
			catch (Exception e)
			{
				_log.log(Level.WARNING, "", e);
			}
		}
	}

	/** ShortBuff clearing Task */
	ScheduledFuture<?> _shortBuffTask = null;

	public class ShortBuffTask implements Runnable
	{
		private L2PcInstance _player = null;
		
		public ShortBuffTask(L2PcInstance activeChar)
		{
			_player = activeChar;
		}
		
		public void run()
		{
			if (_player == null)
				return;
			
			_player.sendPacket(new ShortBuffStatusUpdate(0, 0, 0));
			setShortBuffTaskSkillId(0);
		}
	}

	// L2JMOD Wedding
	private boolean _married = false;
	private int _partnerId = 0;
	private int _coupleId = 0;
	private boolean _engagerequest = false;
	private int _engageid = 0;
	private boolean _marryrequest = false;
	private boolean _marryaccepted = false;

    /** Skill casting information (used to queue when several skills are cast in a short time) **/
    public class SkillDat
    {
        private L2Skill _skill;
        private boolean _ctrlPressed;
        private boolean _shiftPressed;

        protected SkillDat(L2Skill skill, boolean ctrlPressed, boolean shiftPressed)
        {
            _skill = skill;
            _ctrlPressed = ctrlPressed;
            _shiftPressed = shiftPressed;
        }

        public boolean isCtrlPressed()
        {
            return _ctrlPressed;
        }

        public boolean isShiftPressed()
        {
            return _shiftPressed;
        }

        public L2Skill getSkill()
        {
            return _skill;
        }

        public int getSkillId()
        {
            return (getSkill() != null) ? getSkill().getId() : -1;
        }
    }

    //summon friend
    private SummonRequest _summonRequest = new SummonRequest();

    public class SummonRequest
    {
    	private L2PcInstance _target = null;
    	private L2Skill _skill = null;

    	public void setTarget(L2PcInstance destination, L2Skill skill)
    	{
    		_target = destination;
    		_skill = skill;
    	}

    	public L2PcInstance getTarget()
    	{
    		return _target;
    	}
    	
    	public L2Skill getSkill()
    	{
    		return _skill;
    	}
    }
    
    // open/close gates
    private GatesRequest _gatesRequest = new GatesRequest();
    
    public class GatesRequest
    {
    	private L2DoorInstance _target = null;
    	public void setTarget(L2DoorInstance door)
    	{
    		_target = door;
    	}
    	public L2DoorInstance getDoor()
    	{
    		return _target;
    	}
    }
	/**
	 * Create a new L2PcInstance and add it in the characters table of the database.<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Create a new L2PcInstance with an account name </li>
	 * <li>Set the name, the Hair Style, the Hair Color and  the Face type of the L2PcInstance</li>
	 * <li>Add the player in the characters table of the database</li><BR><BR>
	 *
	 * @param objectId Identifier of the object to initialized
	 * @param template The L2PcTemplate to apply to the L2PcInstance
	 * @param accountName The name of the L2PcInstance
	 * @param name The name of the L2PcInstance
	 * @param hairStyle The hair style Identifier of the L2PcInstance
	 * @param hairColor The hair color Identifier of the L2PcInstance
	 * @param face The face type Identifier of the L2PcInstance
	 *
	 * @return The L2PcInstance added to the database or null
	 *
	 */
	public static L2PcInstance create(int objectId, L2PcTemplate template, String accountName, String name, byte hairStyle, byte hairColor, byte face, boolean sex)
	{
		// Create a new L2PcInstance with an account name
		PcAppearance app = new PcAppearance(face, hairColor, hairStyle, sex);
		L2PcInstance player = new L2PcInstance(objectId, template, accountName, app);
		
		// Set the name of the L2PcInstance
		player.setName(name);
		
		// Set Character's create time 
		player.setCreateTime(System.currentTimeMillis());
		
		// Set the base class ID to that of the actual class ID.
		player.setBaseClass(player.getClassId());
		// Kept for backwards compabitility.
		player.setNewbie(1);
		// Add the player in the characters table of the database
		boolean ok = player.createDb();
		
		if (!ok)
			return null;
		
		return player;
	}

	public static L2PcInstance createDummyPlayer(int objectId, String name)
	{
		// Create a new L2PcInstance with an account name
		L2PcInstance player = new L2PcInstance(objectId);
		player.setName(name);

		return player;
	}

	public String getAccountName()
	{
		if (getClient() == null || getClient().isDetached())
			return "disconnected";
		return getClient().getAccountName();
	}

	public Map<Integer, String> getAccountChars()
	{
		return _chars;
	}

	public int getRelation(L2PcInstance target)
	{
		int result = 0;

		if (getClan() != null)
		{
			result |= RelationChanged.RELATION_CLAN_MEMBER;
			if (getClan() == target.getClan())
				result |= RelationChanged.RELATION_CLAN_MATE;
			if (getAllyId() != 0)
				result |= RelationChanged.RELATION_ALLY_MEMBER;
		}
		if (isClanLeader())
			result |= RelationChanged.RELATION_LEADER;
		if (getParty() != null && getParty() == target.getParty())
		{
			result |= RelationChanged.RELATION_HAS_PARTY;
			for (int i = 0; i < getParty().getPartyMembers().size(); i++)
			{
				if (getParty().getPartyMembers().get(i) != this)
					continue;
				switch (i)
				{
					case 0:
						result |= RelationChanged.RELATION_PARTYLEADER; // 0x10
						break;
					case 1:
						result |= RelationChanged.RELATION_PARTY4; // 0x8
						break;
					case 2:
						result |= RelationChanged.RELATION_PARTY3+RelationChanged.RELATION_PARTY2+RelationChanged.RELATION_PARTY1; // 0x7
						break;
					case 3:
						result |= RelationChanged.RELATION_PARTY3+RelationChanged.RELATION_PARTY2; // 0x6
						break;
					case 4:
						result |= RelationChanged.RELATION_PARTY3+RelationChanged.RELATION_PARTY1; // 0x5
						break;
					case 5:
						result |= RelationChanged.RELATION_PARTY3; // 0x4
						break;
					case 6:
						result |= RelationChanged.RELATION_PARTY2+RelationChanged.RELATION_PARTY1; // 0x3
						break;
					case 7:
						result |= RelationChanged.RELATION_PARTY2; // 0x2
						break;
					case 8:
						result |= RelationChanged.RELATION_PARTY1; // 0x1
						break;
				}
			}	
		}
		if (getSiegeState() != 0)
		{
			if (TerritoryWarManager.getInstance().getRegisteredTerritoryId(this) != 0)
				result |= RelationChanged.RELATION_TERRITORY_WAR;
			else
			{
				result |= RelationChanged.RELATION_INSIEGE;
				if (getSiegeState() != target.getSiegeState())
					result |= RelationChanged.RELATION_ENEMY;
				else
					result |= RelationChanged.RELATION_ALLY;
				if (getSiegeState() == 1)
					result |= RelationChanged.RELATION_ATTACKER;
			}
		}
		if (getClan() != null && target.getClan() != null)
		{
			if (target.getPledgeType() != L2Clan.SUBUNIT_ACADEMY
				&& getPledgeType() != L2Clan.SUBUNIT_ACADEMY
				&& target.getClan().isAtWarWith(getClan().getClanId()))
			{
				result |= RelationChanged.RELATION_1SIDED_WAR;
				if (getClan().isAtWarWith(target.getClan().getClanId()))
					result |= RelationChanged.RELATION_MUTUAL_WAR;
			}
		}
		return result;
	}

	/**
	 * Retrieve a L2PcInstance from the characters table of the database and add it in _allObjects of the L2world (call restore method).<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Retrieve the L2PcInstance from the characters table of the database </li>
	 * <li>Add the L2PcInstance object in _allObjects </li>
	 * <li>Set the x,y,z position of the L2PcInstance and make it invisible</li>
	 * <li>Update the overloaded status of the L2PcInstance</li><BR><BR>
	 *
	 * @param objectId Identifier of the object to initialized
	 *
	 * @return The L2PcInstance loaded from the database
	 *
	 */
	public static L2PcInstance load(int objectId)
	{
		return restore(objectId);
	}

	private void initPcStatusUpdateValues()
	{
		_cpUpdateInterval = getMaxCp() / 352.0;
		_cpUpdateIncCheck = getMaxCp();
		_cpUpdateDecCheck = getMaxCp() - _cpUpdateInterval;
		_mpUpdateInterval = getMaxMp() / 352.0;
		_mpUpdateIncCheck = getMaxMp();
		_mpUpdateDecCheck = getMaxMp() - _mpUpdateInterval;
	}

	/**
	 * Constructor of L2PcInstance (use L2Character constructor).<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Call the L2Character constructor to create an empty _skills slot and copy basic Calculator set to this L2PcInstance </li>
	 * <li>Set the name of the L2PcInstance</li><BR><BR>
	 *
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method SET the level of the L2PcInstance to 1</B></FONT><BR><BR>
	 *
	 * @param objectId Identifier of the object to initialized
	 * @param template The L2PcTemplate to apply to the L2PcInstance
	 * @param accountName The name of the account including this L2PcInstance
	 *
	 */
	private L2PcInstance(int objectId, L2PcTemplate template, String accountName, PcAppearance app)
	{
		super(objectId, template);
		setInstanceType(InstanceType.L2PcInstance);
        super.initCharStatusUpdateValues();
        initPcStatusUpdateValues();

		_accountName  = accountName;
        app.setOwner(this);
		_appearance   = app;

		// Create an AI
		_ai = new L2PlayerAI(new L2PcInstance.AIAccessor());

		// Create a L2Radar object
		_radar = new L2Radar(this);

		// Retrieve from the database all skills of this L2PcInstance and add them to _skills
		// Retrieve from the database all items of this L2PcInstance and add them to _inventory
		getInventory().restore();
		if (!Config.WAREHOUSE_CACHE)
			getWarehouse();
		startVitalityTask();
	}

	private L2PcInstance(int objectId)
	{
		super(objectId, null);
		setInstanceType(InstanceType.L2PcInstance);
        super.initCharStatusUpdateValues();
        initPcStatusUpdateValues();
	}

	@Override
	public final PcKnownList getKnownList()
	{
		return (PcKnownList)super.getKnownList();
	}
	
    @Override
    public void initKnownList()
    {
    	setKnownList(new PcKnownList(this));
    }

	@Override
	public final PcStat getStat()
	{
		return (PcStat)super.getStat();
	}
    
    @Override
    public void initCharStat()
    {
    	setStat(new PcStat(this));
    }

	@Override
	public final PcStatus getStatus()
	{
		return (PcStatus)super.getStatus();
	}
	
    @Override
    public void initCharStatus()
    {
    	setStatus(new PcStatus(this));
    }
	
	@Override
	public PcPosition getPosition()
	{
		return (PcPosition)super.getPosition();
	}
	
    @Override
    public void initPosition()
    {
    	setObjectPosition(new PcPosition(this));
    }

	public final PcAppearance getAppearance()
	{
		return _appearance;
	}

	/**
	 * Return the base L2PcTemplate link to the L2PcInstance.<BR><BR>
	 */
	public final L2PcTemplate getBaseTemplate()
	{
		return CharTemplateTable.getInstance().getTemplate(_baseClass);
	}

	/** Return the L2PcTemplate link to the L2PcInstance. */
	@Override
	public final L2PcTemplate getTemplate() { return (L2PcTemplate)super.getTemplate(); }

	public void setTemplate(ClassId newclass) { super.setTemplate(CharTemplateTable.getInstance().getTemplate(newclass)); }

	/**
	 * Return the AI of the L2PcInstance (create it if necessary).<BR><BR>
	 */
	@Override
	public L2CharacterAI getAI()
	{
		L2CharacterAI ai = _ai; // copy handle
    	if (ai == null)
    	{
    		synchronized(this)
    		{
    			if (_ai == null) _ai = new L2PlayerAI(new L2PcInstance.AIAccessor());
    			return _ai;
    		}
    	}
    	return ai;
	}


	/**
	 * Calculate a destination to explore the area and set the AI Intention to AI_INTENTION_MOVE_TO.<BR><BR>
	 */
	public void explore()
	{
		if (!_exploring) return;

		if(getMountType() == 2)
			return;

		// Calculate the destination point (random)

		int x = getX()+Rnd.nextInt(6000)-3000;
		int y = getY()+Rnd.nextInt(6000)-3000;

		if (x > Universe.MAX_X) x = Universe.MAX_X;
		if (x < Universe.MIN_X) x = Universe.MIN_X;
		if (y > Universe.MAX_Y) y = Universe.MAX_Y;
		if (y < Universe.MIN_Y) y = Universe.MIN_Y;

		int z = getZ();

		L2CharPosition pos = new L2CharPosition(x,y,z,0);

		// Set the AI Intention to AI_INTENTION_MOVE_TO
		getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO,pos);

	}

	/** Return the Level of the L2PcInstance. */
	@Override
	public final int getLevel() { return getStat().getLevel(); }
	
	/**
	 * Return the _newbie rewards state of the L2PcInstance.<BR><BR>
	 */
	public int getNewbie()
	{
		return _newbie;
	}

	/**
	 * Set the _newbie rewards state of the L2PcInstance.<BR><BR>
	 *
	 * @param newbieRewards The Identifier of the _newbie state<BR><BR>
	 *
	 */
	public void setNewbie(int newbieRewards)
	{
		_newbie = newbieRewards;
	}


	public void setBaseClass(int baseClass)
	{
		_baseClass = baseClass;
	}

	public void setBaseClass(ClassId classId)
	{
		_baseClass = classId.ordinal();
	}

	public boolean isInStoreMode() { return (getPrivateStoreType() > 0); }
//	public boolean isInCraftMode() { return (getPrivateStoreType() == STORE_PRIVATE_MANUFACTURE); }

	public boolean isInCraftMode()
	{
		return _inCraftMode;
	}

	public void isInCraftMode(boolean b)
	{
		_inCraftMode = b;
	}

	/**
	 * Manage Logout Task: <li>Remove player from world <BR>
	 * {@link L2PcInstance#deleteMe()}</li> <li>Save player data into DB <BR>
	 * {@link L2GameClient#saveCharToDisk(L2PcInstance)}</li> <BR>
	 * <BR>
	 */
	public void logout()
	{
		logout(true);
	}

	/**
	 * Manage Logout Task: <li>Remove player from world <BR>
	 * {@link L2PcInstance#deleteMe()}</li> <li>Save player data into DB <BR>
	 * {@link L2GameClient#saveCharToDisk(L2PcInstance)}</li> <BR>
	 * <BR>
	 * @param closeClient
	 */
	public void logout(boolean closeClient)
	{
		try
		{
			deleteMe(closeClient);
			L2GameClient.saveCharToDisk(this, false);
		}
		catch (Exception e)
		{
			_log.info("logout(): " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Return a table containing all Common L2RecipeList of the L2PcInstance.<BR><BR>
	 */
	public L2RecipeList[] getCommonRecipeBook()
	{
		return _commonRecipeBook.values().toArray(new L2RecipeList[_commonRecipeBook.values().size()]);
	}

	/**
	 * Return a table containing all Dwarf L2RecipeList of the L2PcInstance.<BR><BR>
	 */
	public L2RecipeList[] getDwarvenRecipeBook()
	{
		return _dwarvenRecipeBook.values().toArray(new L2RecipeList[_dwarvenRecipeBook.values().size()]);
	}

	/**
	 * Add a new L2RecipList to the table _commonrecipebook containing all L2RecipeList of the L2PcInstance <BR><BR>
	 *
	 * @param recipe The L2RecipeList to add to the _recipebook
	 *
	 */
	public void registerCommonRecipeList(L2RecipeList recipe, boolean saveToDb)
	{
		_commonRecipeBook.put(recipe.getId(), recipe);
		
		if (saveToDb)
			insertNewRecipeData(recipe.getId(), false);
	}

	/**
	 * Add a new L2RecipList to the table _recipebook containing all L2RecipeList of the L2PcInstance <BR><BR>
	 *
	 * @param recipe The L2RecipeList to add to the _recipebook
	 *
	 */
	public void registerDwarvenRecipeList(L2RecipeList recipe, boolean saveToDb)
	{
		_dwarvenRecipeBook.put(recipe.getId(), recipe);
		
		if (saveToDb)
			insertNewRecipeData(recipe.getId(), true);
	}

	/**
	 * @param RecipeID The Identifier of the L2RecipeList to check in the player's recipe books
	 *
	 * @return
	 * <b>TRUE</b> if player has the recipe on Common or Dwarven Recipe book else returns <b>FALSE</b>
	 */
	public boolean hasRecipeList(int recipeId)
	{
		if (_dwarvenRecipeBook.containsKey(recipeId))
			return true;
		else if(_commonRecipeBook.containsKey(recipeId))
			return true;
		else
			return false;
	}

	/**
	 * Tries to remove a L2RecipList from the table _DwarvenRecipeBook or from table _CommonRecipeBook, those table contain all L2RecipeList of the L2PcInstance <BR><BR>
	 *
	 * @param RecipeID The Identifier of the L2RecipeList to remove from the _recipebook
	 *
	 */
	public void unregisterRecipeList(int recipeId)
	{
		if (_dwarvenRecipeBook.remove(recipeId) != null)
			 deleteRecipeData(recipeId, true);
		else if (_commonRecipeBook.remove(recipeId) != null)
			deleteRecipeData(recipeId, false);
		else
			_log.warning("Attempted to remove unknown RecipeList: "+recipeId);

		L2ShortCut[] allShortCuts = getAllShortCuts();

		for (L2ShortCut sc : allShortCuts)
		{
			if (sc != null && sc.getId() == recipeId && sc.getType() == L2ShortCut.TYPE_RECIPE)
				deleteShortCut(sc.getSlot(), sc.getPage());
		}
	}
	
	private void insertNewRecipeData(int recipeId, boolean isDwarf)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("INSERT INTO character_recipebook (charId, id, classIndex, type) values(?,?,?,?)");
			statement.setInt(1, getObjectId());
			statement.setInt(2,recipeId);
			statement.setInt(3, isDwarf ? _classIndex : 0);
			statement.setInt(4, isDwarf ? 1 : 0);
			statement.execute();
			statement.close();
		}
		catch (SQLException e)
		{
			if(_log.isLoggable(Level.SEVERE))
				_log.log(Level.SEVERE, "SQL exception while inserting recipe: "+recipeId+" from character "+getObjectId(), e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e){}
		}		
	}
	
	private void deleteRecipeData(int recipeId, boolean isDwarf)
	{
		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM character_recipebook WHERE charId=? AND id=? AND classIndex=?");
			statement.setInt(1, getObjectId());
			statement.setInt(2, recipeId);
			statement.setInt(3, isDwarf ? _classIndex : 0);
			statement.execute();
			statement.close();
		}
		catch (SQLException e)
		{
			if (_log.isLoggable(Level.SEVERE))
				_log.log(Level.SEVERE, "SQL exception while deleting recipe: "+recipeId+" from character "+getObjectId(), e);
		}
		finally
		{
			try
			{
				con.close();
			} catch (Exception e) {}
		}
	}

	/**
	 * Returns the Id for the last talked quest NPC.<BR><BR>
	 */
	public int getLastQuestNpcObject()
	{
		return _questNpcObject;
	}

	public void setLastQuestNpcObject(int npcId)
	{
		_questNpcObject = npcId;
	}

	/**
	 * Return the QuestState object corresponding to the quest name.<BR><BR>
	 *
	 * @param quest The name of the quest
	 *
	 */
	public QuestState getQuestState(String quest)
	{
		return _quests.get(quest);
	}

	/**
	 * Add a QuestState to the table _quest containing all quests began by the L2PcInstance.<BR><BR>
	 *
	 * @param qs The QuestState to add to _quest
	 *
	 */
	public void setQuestState(QuestState qs)
	{
		_quests.put(qs.getQuestName(), qs);
	}


	/**
	 * Remove a QuestState from the table _quest containing all quests began by the L2PcInstance.<BR><BR>
	 *
	 * @param quest The name of the quest
	 *
	 */
	public void delQuestState(String quest)
	{
		_quests.remove(quest);
	}

	private QuestState[] addToQuestStateArray(QuestState[] questStateArray, QuestState state) {
		int len = questStateArray.length;
		QuestState[] tmp = new QuestState[len+1];
		for (int i=0; i < len; i++)
			tmp[i] = questStateArray[i];
		tmp[len] = state;
		return tmp;
	}

	/**
	 * Return a table containing all Quest in progress from the table _quests.<BR><BR>
	 */
	public Quest[] getAllActiveQuests()
	{
		FastList<Quest> quests = new FastList<Quest>();

		for (QuestState qs : _quests.values())
		{
			if (qs == null)
				continue;
			
			if (qs.getQuest() == null)
				continue;
			
		    int questId = qs.getQuest().getQuestIntId();
			if ((questId>19999) || (questId<1))
				continue;

			if (!qs.isStarted() && !Config.DEVELOPER)
				continue;

			quests.add(qs.getQuest());
		}

		return quests.toArray(new Quest[quests.size()]);
	}

	/**
	 * Return a table containing all QuestState to modify after a L2Attackable killing.<BR><BR>
	 *
	 * @param npcId The Identifier of the L2Attackable attacked
	 *
	 */
	public QuestState[] getQuestsForAttacks(L2Npc npc)
	{
		// Create a QuestState table that will contain all QuestState to modify
		QuestState[] states = null;

		// Go through the QuestState of the L2PcInstance quests
		for (Quest quest : npc.getTemplate().getEventQuests(Quest.QuestEventType.ON_ATTACK))
		{
			// Check if the Identifier of the L2Attackable attck is needed for the current quest
			if (getQuestState(quest.getName())!=null)
			{
				// Copy the current L2PcInstance QuestState in the QuestState table
				if (states == null)
					states = new QuestState[]{getQuestState(quest.getName())};
				else
					states = addToQuestStateArray(states, getQuestState(quest.getName()));
			}
		}

		// Return a table containing all QuestState to modify
		return states;
	}

	/**
	 * Return a table containing all QuestState to modify after a L2Attackable killing.<BR><BR>
	 *
	 * @param npcId The Identifier of the L2Attackable killed
	 *
	 */
	public QuestState[] getQuestsForKills(L2Npc npc)
	{
		// Create a QuestState table that will contain all QuestState to modify
		QuestState[] states = null;

		// Go through the QuestState of the L2PcInstance quests
		for (Quest quest : npc.getTemplate().getEventQuests(Quest.QuestEventType.ON_KILL))
		{
			// Check if the Identifier of the L2Attackable killed is needed for the current quest
			if (getQuestState(quest.getName())!=null)
			{
				// Copy the current L2PcInstance QuestState in the QuestState table
				if (states == null)
					states = new QuestState[]{getQuestState(quest.getName())};
				else
					states = addToQuestStateArray(states, getQuestState(quest.getName()));
			}
		}

		// Return a table containing all QuestState to modify
		return states;
	}

	/**
	 * Return a table containing all QuestState from the table _quests in which the L2PcInstance must talk to the NPC.<BR><BR>
	 *
	 * @param npcId The Identifier of the NPC
	 *
	 */
	public QuestState[] getQuestsForTalk(int npcId)
	{
		// Create a QuestState table that will contain all QuestState to modify
		QuestState[] states = null;

		// Go through the QuestState of the L2PcInstance quests
		Quest[] quests = NpcTable.getInstance().getTemplate(npcId).getEventQuests(Quest.QuestEventType.ON_TALK);
		if (quests != null)
		{
			for (Quest quest: quests)
			{
				if (quest != null)
				{
					// Copy the current L2PcInstance QuestState in the QuestState table
					if (getQuestState(quest.getName())!=null)
					{
						if (states == null)
							states = new QuestState[]{getQuestState(quest.getName())};
						else
							states = addToQuestStateArray(states, getQuestState(quest.getName()));
					}
				}
			}
		}

		// Return a table containing all QuestState to modify
		return states;
	}

	public QuestState processQuestEvent(String quest, String event)
	{
		QuestState retval = null;
		if (event == null)
			event = "";
		if (!_quests.containsKey(quest))
			return retval;
		QuestState qs = getQuestState(quest);
		if (qs == null && event.length() == 0)
			return retval;
		if (qs == null) {
			Quest q = QuestManager.getInstance().getQuest(quest);
			if (q == null)
				return retval;
			qs = q.newQuestState(this);
		}
		if (qs != null) {
			if (getLastQuestNpcObject() > 0)
			{
				L2Object object = L2World.getInstance().findObject(getLastQuestNpcObject());
				if (object instanceof L2Npc && isInsideRadius(object, L2Npc.INTERACTION_DISTANCE, false, false))
                {
					L2Npc npc = (L2Npc)object;
					QuestState[] states = getQuestsForTalk(npc.getNpcId());

					if (states != null)
					{
						for (QuestState state : states)
						{
							if (state.getQuest().getName().equals(qs.getQuest().getName()))
							{
								if (qs.getQuest().notifyEvent(event, npc, this))
									showQuestWindow(quest, State.getStateName(qs.getState()));

								retval = qs;
							}
						}
						sendPacket(new QuestList());
					}
				}
			}
		}

		return retval;
	}

	private void showQuestWindow(String questId, String stateId)
	{
		String path = "data/scripts/quests/"+questId+"/"+stateId+".htm";
		String content = HtmCache.getInstance().getHtm(getHtmlPrefix(), path);  //TODO path for quests html

		if (content != null)
		{
			if (Config.DEBUG)
				_log.fine("Showing quest window for quest "+questId+" state "+stateId+" html path: " + path);

			NpcHtmlMessage npcReply = new NpcHtmlMessage(5);
			npcReply.setHtml(content);
			sendPacket(npcReply);
		}

		sendPacket( ActionFailed.STATIC_PACKET );
	}

	/** List of all QuestState instance that needs to be notified of this L2PcInstance's or its pet's death */
	private List<QuestState> _NotifyQuestOfDeathList = new FastList<QuestState>();

	/**
	 * Add QuestState instance that is to be notified of L2PcInstance's death.<BR><BR>
	 *
	 * @param qs The QuestState that subscribe to this event
	 *
	 */
	public void addNotifyQuestOfDeath (QuestState qs)
	{
		if (qs == null || _NotifyQuestOfDeathList.contains(qs))
			return;

		_NotifyQuestOfDeathList.add(qs);
	}

	/**
	 * Remove QuestState instance that is to be notified of L2PcInstance's death.<BR><BR>
	 *
	 * @param qs The QuestState that subscribe to this event
	 *
	 */
	public void removeNotifyQuestOfDeath (QuestState qs)
	{
		if (qs == null || !_NotifyQuestOfDeathList.contains(qs))
			return;

		_NotifyQuestOfDeathList.remove(qs);
	}

	/**
	 * Return a list of QuestStates which registered for notify of death of this L2PcInstance.<BR><BR>
	 */
	public final List<QuestState> getNotifyQuestOfDeath ()
	{
		if (_NotifyQuestOfDeathList == null)
			_NotifyQuestOfDeathList = new FastList<QuestState>();

		return _NotifyQuestOfDeathList;
	}

	/**
	 * Return a table containing all L2ShortCut of the L2PcInstance.<BR><BR>
	 */
	public L2ShortCut[] getAllShortCuts()
	{
		return _shortCuts.getAllShortCuts();
	}

	/**
	 * Return the L2ShortCut of the L2PcInstance corresponding to the position (page-slot).<BR><BR>
	 *
	 * @param slot The slot in wich the shortCuts is equiped
	 * @param page The page of shortCuts containing the slot
	 *
	 */
	public L2ShortCut getShortCut(int slot, int page)
	{
		return _shortCuts.getShortCut(slot, page);
	}

	/**
	 * Add a L2shortCut to the L2PcInstance _shortCuts<BR><BR>
	 */
	public void registerShortCut(L2ShortCut shortcut)
	{
		_shortCuts.registerShortCut(shortcut);
	}

	/**
	 * Delete the L2ShortCut corresponding to the position (page-slot) from the L2PcInstance _shortCuts.<BR><BR>
	 */
	public void deleteShortCut(int slot, int page)
	{
		_shortCuts.deleteShortCut(slot, page);
	}

	/**
	 * Add a L2Macro to the L2PcInstance _macroses<BR><BR>
	 */
	public void registerMacro(L2Macro macro)
	{
		_macroses.registerMacro(macro);
	}

	/**
	 * Delete the L2Macro corresponding to the Identifier from the L2PcInstance _macroses.<BR><BR>
	 */
	public void deleteMacro(int id)
	{
		_macroses.deleteMacro(id);
	}

	/**
	 * Return all L2Macro of the L2PcInstance.<BR><BR>
	 */
	public MacroList getMacroses()
	{
		return _macroses;
	}

	/**
	 * Set the siege state of the L2PcInstance.<BR><BR>
	 * 1 = attacker, 2 = defender, 0 = not involved
	 */
	public void setSiegeState(byte siegeState)
	{
		_siegeState = siegeState;
	}
	/**
	 * Get the siege state of the L2PcInstance.<BR><BR>
	 * 1 = attacker, 2 = defender, 0 = not involved
	 */
	public byte getSiegeState()
	{
		return _siegeState;
	}

	/**
	 * Set the siege Side of the L2PcInstance.<BR><BR>
	 */
	public void setSiegeSide(int val)
	{
		_siegeSide = val;
	}
	
	public boolean isRegisteredOnThisSiegeField(int val)
	{
		if (_siegeSide != val && (_siegeSide < 81 || _siegeSide > 89))
			return false;
		return true;
	}
	
	public int getSiegeSide()
	{
		return _siegeSide;
	}
	
	/**
	 * Set the PvP Flag of the L2PcInstance.<BR><BR>
	 */
	public void setPvpFlag(int pvpFlag)
	{
		_pvpFlag = (byte)pvpFlag;
	}

	public byte getPvpFlag()
	{
		return _pvpFlag;
	}

	@Override
	public void updatePvPFlag(int value)
	{
		if (getPvpFlag() == value)
			return;
		setPvpFlag(value);

		sendPacket(new UserInfo(this));
		sendPacket(new ExBrExtraUserInfo(this));

		// If this player has a pet update the pets pvp flag as well
		if (getPet() != null) sendPacket(new RelationChanged(getPet(), getRelation(this), false));

		Collection<L2PcInstance> plrs = getKnownList().getKnownPlayers().values();
		//synchronized (getKnownList().getKnownPlayers())
		{
			for (L2PcInstance target : plrs)
			{
				target.sendPacket(new RelationChanged(this, getRelation(target), isAutoAttackable(target)));
				if (getPet() != null)
					target.sendPacket(new RelationChanged(getPet(), getRelation(target), isAutoAttackable(target)));
			}
		}
	}

	@Override
    public void revalidateZone(boolean force)
    {
    	// Cannot validate if not in  a world region (happens during teleport)
    	if (getWorldRegion() == null) return;

    	// This function is called too often from movement code
    	if (force) _zoneValidateCounter = 4;
    	else
    	{
    		_zoneValidateCounter--;
    		if (_zoneValidateCounter < 0)
    			_zoneValidateCounter = 4;
    		else return;
    	}

    	getWorldRegion().revalidateZones(this);
    	
        if (Config.ALLOW_WATER)
            checkWaterState();

        if (isInsideZone(ZONE_DANGERAREA))
        {
        	if (_lastCompassZone == ExSetCompassZoneCode.ALTEREDZONE) return;
        	_lastCompassZone = ExSetCompassZoneCode.ALTEREDZONE;
        	ExSetCompassZoneCode cz = new ExSetCompassZoneCode(ExSetCompassZoneCode.ALTEREDZONE);
        	sendPacket(cz);
        }
        else if (isInsideZone(ZONE_SIEGE))
        {
        	if (_lastCompassZone == ExSetCompassZoneCode.SIEGEWARZONE2) return;
        	_lastCompassZone = ExSetCompassZoneCode.SIEGEWARZONE2;
        	ExSetCompassZoneCode cz = new ExSetCompassZoneCode(ExSetCompassZoneCode.SIEGEWARZONE2);
        	sendPacket(cz);
        }
        else if (isInsideZone(ZONE_PVP))
        {
        	if (_lastCompassZone == ExSetCompassZoneCode.PVPZONE) return;
        	_lastCompassZone = ExSetCompassZoneCode.PVPZONE;
        	ExSetCompassZoneCode cz = new ExSetCompassZoneCode(ExSetCompassZoneCode.PVPZONE);
        	sendPacket(cz);
        }
        else if (isIn7sDungeon())
        {
        	if (_lastCompassZone == ExSetCompassZoneCode.SEVENSIGNSZONE) return;
        	_lastCompassZone = ExSetCompassZoneCode.SEVENSIGNSZONE;
        	ExSetCompassZoneCode cz = new ExSetCompassZoneCode(ExSetCompassZoneCode.SEVENSIGNSZONE);
        	sendPacket(cz);
        }
        else if (isInsideZone(ZONE_PEACE))
        {
        	if (_lastCompassZone == ExSetCompassZoneCode.PEACEZONE) return;
        	_lastCompassZone = ExSetCompassZoneCode.PEACEZONE;
        	ExSetCompassZoneCode cz = new ExSetCompassZoneCode(ExSetCompassZoneCode.PEACEZONE);
        	sendPacket(cz);
        }
        else
        {
        	if (_lastCompassZone == ExSetCompassZoneCode.GENERALZONE) return;
        	if (_lastCompassZone == ExSetCompassZoneCode.SIEGEWARZONE2) updatePvPStatus();
        	_lastCompassZone = ExSetCompassZoneCode.GENERALZONE;
        	ExSetCompassZoneCode cz = new ExSetCompassZoneCode(ExSetCompassZoneCode.GENERALZONE);
        	sendPacket(cz);
        }
    }

	/**
	 * Return True if the L2PcInstance can Craft Dwarven Recipes.<BR><BR>
	 */
	public boolean hasDwarvenCraft()
	{
		return getSkillLevel(L2Skill.SKILL_CREATE_DWARVEN) >= 1;
	}

	public int getDwarvenCraft()
	{
		return getSkillLevel(L2Skill.SKILL_CREATE_DWARVEN);
	}

	/**
	 * Return True if the L2PcInstance can Craft Dwarven Recipes.<BR><BR>
	 */
	public boolean hasCommonCraft()
	{
		return getSkillLevel(L2Skill.SKILL_CREATE_COMMON) >= 1;
	}

	public int getCommonCraft()
	{
		return getSkillLevel(L2Skill.SKILL_CREATE_COMMON);
	}

	/**
	 * Return the PK counter of the L2PcInstance.<BR><BR>
	 */
	public int getPkKills()
	{
		return _pkKills;
	}

	/**
	 * Set the PK counter of the L2PcInstance.<BR><BR>
	 */
	public void setPkKills(int pkKills)
	{
		_pkKills = pkKills;
	}

	/**
	 * Return the _deleteTimer of the L2PcInstance.<BR><BR>
	 */
	public long getDeleteTimer()
	{
		return _deleteTimer;
	}

	/**
	 * Set the _deleteTimer of the L2PcInstance.<BR><BR>
	 */
	public void setDeleteTimer(long deleteTimer)
	{
		_deleteTimer = deleteTimer;
	}

	/**
	 * Return the current weight of the L2PcInstance.<BR><BR>
	 */
	public int getCurrentLoad()
	{
		return _inventory.getTotalWeight();
	}
	/**
	 * Return date of las update of recomPoints
	 */
	public long getLastRecomUpdate()
	{
		return _lastRecomUpdate;
	}
	public void setLastRecomUpdate(long date)
	{
		_lastRecomUpdate = date;
	}
	/**
	 * Return the number of recommandation obtained by the L2PcInstance.<BR><BR>
	 */
	public int getRecomHave()
	{
		return _recomHave;
	}

	/**
	 * Increment the number of recommandation obtained by the L2PcInstance (Max : 255).<BR><BR>
	 */
	protected void incRecomHave()
	{
		if (_recomHave < 255)
			_recomHave++;
	}

	/**
	 * Set the number of recommandation obtained by the L2PcInstance (Max : 255).<BR><BR>
	 */
	public void setRecomHave(int value)
	{
		if (value > 255)
			_recomHave = 255;
		else if (value < 0)
			_recomHave = 0;
		else
			_recomHave = value;
	}



	/**
	 * Return the number of recommandation that the L2PcInstance can give.<BR><BR>
	 */
	public int getRecomLeft()
	{
		return _recomLeft;
	}

	/**
	 * Increment the number of recommandation that the L2PcInstance can give.<BR><BR>
	 */
	protected void decRecomLeft()
	{
		if (_recomLeft > 0)
			_recomLeft--;
	}

	public void giveRecom(L2PcInstance target)
	{
		if (Config.ALT_RECOMMEND)
		{
			Connection con = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement(ADD_CHAR_RECOM);
				statement.setInt(1, getObjectId());
				statement.setInt(2, target.getObjectId());
				statement.execute();
				statement.close();
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "Failed updating character recommendations.", e);
			}
			finally
			{
				try { con.close(); } catch (Exception e) {}
			}
		}
		target.incRecomHave();
		decRecomLeft();
		_recomChars.add(target.getObjectId());
	}

	public boolean canRecom(L2PcInstance target)
	{
		return !_recomChars.contains(target.getObjectId());
	}

	/**
	 * Set the exp of the L2PcInstance before a death
	 * @param exp
	 */
	public void setExpBeforeDeath(long exp)
	{
		_expBeforeDeath = exp;
	}

	public long getExpBeforeDeath()
	{
		return _expBeforeDeath;
	}

	/**
	 * Return the Karma of the L2PcInstance.<BR><BR>
	 */
	public int getKarma()
	{
		return _karma;
	}

	/**
	 * Set the Karma of the L2PcInstance and send a Server->Client packet StatusUpdate (broadcast).<BR><BR>
	 */
	public void setKarma(int karma)
	{
		if (karma < 0) karma = 0;
		if (_karma == 0 && karma > 0)
		{
			Collection<L2Object> objs = getKnownList().getKnownObjects().values();
			//synchronized (getKnownList().getKnownObjects())
			{
				for (L2Object object : objs)
				{
					if (!(object instanceof L2GuardInstance))
						continue;
					
					if (((L2GuardInstance) object).getAI().getIntention() == CtrlIntention.AI_INTENTION_IDLE)
						((L2GuardInstance) object).getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE, null);
				}
			}
		}
		else if (_karma > 0 && karma == 0)
		{
			// Send a Server->Client StatusUpdate packet with Karma and PvP Flag to the L2PcInstance and all L2PcInstance to inform (broadcast)
			setKarmaFlag(0);
		}

		_karma = karma;
		broadcastKarma();
	}

	/**
	 * Return the max weight that the L2PcInstance can load.<BR><BR>
	 */
	public int getMaxLoad()
	{
		// Weight Limit = (CON Modifier*69000)*Skills
		// Source http://l2p.bravehost.com/weightlimit.html (May 2007)
		// Fitted exponential curve to the data
		int con = getCON();
		if (con < 1) return 31000;
		if (con > 59) return 176000;
		double baseLoad = Math.pow(1.029993928, con)*30495.627366;
		return (int)calcStat(Stats.MAX_LOAD, baseLoad*Config.ALT_WEIGHT_LIMIT, this, null);
	}

	public int getExpertiseArmorPenalty()
	{
		return _expertiseArmorPenalty;
	}

	public int getExpertiseWeaponPenalty()
	{
		return _expertiseWeaponPenalty;
	}

	public int getWeightPenalty()
	{
		if (_dietMode)
			return 0;
		return _curWeightPenalty;
	}

	/**
	 * Update the overloaded status of the L2PcInstance.<BR><BR>
	 */
	public void refreshOverloaded()
	{
		int maxLoad = getMaxLoad();
		if (maxLoad > 0)
		{
			long weightproc = (long)getCurrentLoad() * 1000 / maxLoad;
			weightproc *= calcStat(Stats.WEIGHT_LIMIT, 1, this, null);
			int newWeightPenalty;
			if (weightproc < 500 || _dietMode)
			{
				newWeightPenalty = 0;
			}
			else if (weightproc < 666)
			{
				newWeightPenalty = 1;
			}
			else if ( weightproc < 800)
			{
				newWeightPenalty = 2;
			}
			else if (weightproc < 1000)
			{
				newWeightPenalty = 3;
			}
			else
			{
				newWeightPenalty = 4;
			}

			if (_curWeightPenalty != newWeightPenalty)
			{
				_curWeightPenalty = newWeightPenalty;
				if (newWeightPenalty > 0 && !_dietMode)
				{
					super.addSkill(SkillTable.getInstance().getInfo(4270,newWeightPenalty));
					setIsOverloaded(getCurrentLoad() > maxLoad);
				}
				else
				{
					super.removeSkill(getKnownSkill(4270));
					setIsOverloaded(false);
				}
				sendPacket(new UserInfo(this));
				sendPacket(new EtcStatusUpdate(this));
				broadcastPacket(new CharInfo(this));
				broadcastPacket(new ExBrExtraUserInfo(this));
			}
		}
	}

	public void refreshExpertisePenalty()
	{
		if (!Config.EXPERTISE_PENALTY)
			return;
		
		int armorPenalty = 0;
		int weaponPenalty = 0;
		
		for (L2ItemInstance item : getInventory().getItems())
		{
			if (item != null && item.isEquipped())
			{
				int crystaltype = item.getItem().getCrystalType();
				
				if (crystaltype > getExpertiseIndex())
				{
					if (item.isWeapon() && crystaltype > weaponPenalty)
						weaponPenalty = crystaltype;
					else if (crystaltype > armorPenalty)
						armorPenalty = crystaltype;
				}
			}
		}
		
		boolean changed = false;
		
		// calc armor penalty
		armorPenalty = armorPenalty - getExpertiseIndex();
		
		if (armorPenalty < 0)
			armorPenalty = 0;
		else if (armorPenalty > 4)
			armorPenalty = 4;
		
		if (getExpertiseArmorPenalty() != armorPenalty || getSkillLevel(6213) != armorPenalty)
		{
			_expertiseArmorPenalty = armorPenalty;
			
			if (_expertiseArmorPenalty > 0)
				super.addSkill(SkillTable.getInstance().getInfo(6213, _expertiseArmorPenalty)); // level used to be newPenalty
			else
				super.removeSkill(getKnownSkill(6213));
			
			changed = true;
		}
		
		// calc weapon penalty
		weaponPenalty = weaponPenalty - getExpertiseIndex();
		if (weaponPenalty < 0)
			weaponPenalty = 0;
		else if (weaponPenalty > 4)
			weaponPenalty = 4;
		
		if (getExpertiseWeaponPenalty() != weaponPenalty || getSkillLevel(6209) != weaponPenalty)
		{
			_expertiseWeaponPenalty = weaponPenalty;
			
			if (_expertiseWeaponPenalty > 0)
				super.addSkill(SkillTable.getInstance().getInfo(6209, _expertiseWeaponPenalty)); // level used to be newPenalty
			else
				super.removeSkill(getKnownSkill(6209));
			
			changed = true;
		}
		
		if (changed)
			sendPacket(new EtcStatusUpdate(this));
	}

    public void checkIfWeaponIsAllowed()
    {
        // Override for Gamemasters
        if (isGM())
            return;

        // Iterate through all effects currently on the character.
        for (L2Effect currenteffect : getAllEffects())
        {
            L2Skill effectSkill = currenteffect.getSkill();

            // Ignore all buff skills that are party related (ie. songs, dances) while still remaining weapon dependant on cast though.
            if (!effectSkill.isOffensive() && !(effectSkill.getTargetType() == SkillTargetType.TARGET_PARTY && effectSkill.getSkillType() == L2SkillType.BUFF))
            {
                // Check to rest to assure current effect meets weapon requirements.
            	if (!effectSkill.getWeaponDependancy(this))
                {
            		SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
            		sm.addSkillName(effectSkill);
            		sendPacket(sm);

                    if (Config.DEBUG)
                        _log.info("   | Skill "+effectSkill.getName()+" has been disabled for ("+getName()+"); Reason: Incompatible Weapon Type.");

                    currenteffect.exit();
                }
            }

            continue;
        }
    }

    public void checkSSMatch(L2ItemInstance equipped, L2ItemInstance unequipped)
    {
    	if (unequipped == null)
    		return;

		if (unequipped.getItem().getType2() == L2Item.TYPE2_WEAPON &&
				(equipped == null ? true : equipped.getItem().getCrystalType() != unequipped.getItem().getCrystalType()))
		{
			for (L2ItemInstance ss : getInventory().getItems())
			{
				if (ss == null)
					continue;
				
				int _itemId = ss.getItemId();

				if (((_itemId >= 2509 && _itemId <= 2514) ||
						(_itemId >= 3947 && _itemId <= 3952) ||
						(_itemId <= 1804 && _itemId >= 1808) ||
						_itemId == 5789 || _itemId == 5790 || _itemId == 1835
						|| (_itemId >=22072 && _itemId <=22086)) &&
						ss.getItem().getCrystalType() == unequipped.getItem().getCrystalType())
				{
                    sendPacket(new ExAutoSoulShot(_itemId, 0));

                    SystemMessage sm = new SystemMessage(SystemMessageId.AUTO_USE_OF_S1_CANCELLED);
                    sm.addString(ss.getItemName());
                    sendPacket(sm);
				}
			}
		}
    }

    public void useEquippableItem(L2ItemInstance item, boolean abortAttack)
	{
		// Equip or unEquip
        L2ItemInstance[] items = null;
        final boolean isEquiped = item.isEquipped();
        final int oldInvLimit = getInventoryLimit();
        SystemMessage sm = null;
        L2ItemInstance old = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LRHAND);
        if (old == null)
        	old = getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);

        checkSSMatch(item, old);

        if (isEquiped)
        {
            if (item.getEnchantLevel() > 0)
            {
            	sm = new SystemMessage(SystemMessageId.EQUIPMENT_S1_S2_REMOVED);
            	sm.addNumber(item.getEnchantLevel());
            	sm.addItemName(item);
            }
            else
            {
	            sm = new SystemMessage(SystemMessageId.S1_DISARMED);
	            sm.addItemName(item);
            }
            sendPacket(sm);

            int slot = getInventory().getSlotFromItem(item);
            // we cant unequip talisman by body slot
            if (slot == L2Item.SLOT_DECO)
            	items = getInventory().unEquipItemInSlotAndRecord(item.getLocationSlot());
            else
            	items = getInventory().unEquipItemInBodySlotAndRecord(slot);
        }
        else
        {
        	int tempBodyPart = item.getItem().getBodyPart();
        	L2ItemInstance tempItem = getInventory().getPaperdollItemByL2ItemId(tempBodyPart);

        	//check if the item replaces a wear-item
        	if (tempItem != null && tempItem.isWear())
        	{
        		// dont allow an item to replace a wear-item
        		return;
        	}
        	else if (tempBodyPart == L2Item.SLOT_LR_HAND)
        	{
        		// this may not remove left OR right hand equipment
        		tempItem = getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
        		if (tempItem != null && tempItem.isWear()) return;

        		tempItem = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
        		if (tempItem != null && tempItem.isWear()) return;
        	}
        	else if (tempBodyPart == L2Item.SLOT_FULL_ARMOR)
        	{
        		// this may not remove chest or leggins
        		tempItem = getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST);
        		if (tempItem != null && tempItem.isWear()) return;

        		tempItem = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LEGS);
        		if (tempItem != null && tempItem.isWear()) return;
        	}

			if (item.getEnchantLevel() > 0)
			{
				sm = new SystemMessage(SystemMessageId.S1_S2_EQUIPPED);
				sm.addNumber(item.getEnchantLevel());
				sm.addItemName(item);
			}
			else
			{
				sm = new SystemMessage(SystemMessageId.S1_EQUIPPED);
				sm.addItemName(item);
			}
			sendPacket(sm);

			items = getInventory().equipItemAndRecord(item);

            // Consume mana - will start a task if required; returns if item is not a shadow item
            item.decreaseMana(false);
        }
        sm = null;

        refreshExpertisePenalty();

		/**
		// remove effects which doesn't match weapon type (but not party buffs)
		if (item.getItem().getType2() == L2Item.TYPE2_WEAPON)
			checkIfWeaponIsAllowed();
		**/

		broadcastUserInfo();
		
		InventoryUpdate iu = new InventoryUpdate();
		iu.addItems(Arrays.asList(items));
		sendPacket(iu);
		
		if (abortAttack) abortAttack();
		
		if (getInventoryLimit() != oldInvLimit)
			sendPacket(new ExStorageMaxCount(this));
	}
    
	/**
	 * Return the the PvP Kills of the L2PcInstance (Number of player killed during a PvP).<BR><BR>
	 */
	public int getPvpKills()
	{
		return _pvpKills;
	}

	/**
	 * Set the the PvP Kills of the L2PcInstance (Number of player killed during a PvP).<BR><BR>
	 */
	public void setPvpKills(int pvpKills)
	{
		_pvpKills = pvpKills;
	}
	
	/**
	 * Return the Fame of this L2PcInstance <BR><BR>
	 * @return
	 */
	public int getFame()
	{
		return _fame;
	}
	
	/**
	 * Set the Fame of this L2PcInstane <BR><BR>
	 * @param fame
	 */
	public void setFame(int fame)
	{
		if (fame > Config.MAX_PERSONAL_FAME_POINTS)
			_fame = Config.MAX_PERSONAL_FAME_POINTS;
		else
			_fame = fame;
	}

	/**
	 * Return the ClassId object of the L2PcInstance contained in L2PcTemplate.<BR><BR>
	 */
	public ClassId getClassId()
	{
		return getTemplate().classId;
	}

	/**
	 * Set the template of the L2PcInstance.<BR><BR>
	 *
	 * @param Id The Identifier of the L2PcTemplate to set to the L2PcInstance
	 *
	 */
	public void setClassId(int Id)
	{
    	if (!_subclassLock.tryLock())
    		return;

    	try
    	{
    		if (getLvlJoinedAcademy() != 0 && _clan != null && PlayerClass.values()[Id].getLevel() == ClassLevel.Third)
            {
    			if(getLvlJoinedAcademy() <= 16)
    				_clan.addReputationScore(Config.JOIN_ACADEMY_MAX_REP_SCORE, true);
                else if(getLvlJoinedAcademy() >= 39)
                	_clan.addReputationScore(Config.JOIN_ACADEMY_MIN_REP_SCORE, true);
                else
                	_clan.addReputationScore((Config.JOIN_ACADEMY_MAX_REP_SCORE-(getLvlJoinedAcademy()-16)*20), true);
    			setLvlJoinedAcademy(0);
                //oust pledge member from the academy, cuz he has finished his 2nd class transfer
                SystemMessage msg = new SystemMessage(SystemMessageId.CLAN_MEMBER_S1_EXPELLED);
                msg.addPcName(this);
                _clan.broadcastToOnlineMembers(msg);
                _clan.broadcastToOnlineMembers(new PledgeShowMemberListDelete(getName()));
                _clan.removeClanMember(getObjectId(), 0);
                sendPacket(new SystemMessage(SystemMessageId.ACADEMY_MEMBERSHIP_TERMINATED));

                // receive graduation gift
                getInventory().addItem("Gift",8181,1,this,null); // give academy circlet
            }
    		if (isSubClassActive())
    		{
    			getSubClasses().get(_classIndex).setClassId(Id);
    		}
    		setTarget(this);
    		broadcastPacket(new MagicSkillUse(this, 5103, 1, 1000, 0));
    		setClassTemplate(Id);
    		if (getClassId().level() == 3)
    			sendPacket(new SystemMessage(SystemMessageId.THIRD_CLASS_TRANSFER));
    		else
    			sendPacket(new SystemMessage(SystemMessageId.CLASS_TRANSFER));
    		
    		// Update class icon in party and clan
    		if (isInParty())
    			getParty().broadcastToPartyMembers(new PartySmallWindowUpdate(this));

    		if (getClan() != null)
    			getClan().broadcastToOnlineMembers(new PledgeShowMemberListUpdate(this));
    		
    		if (Config.AUTO_LEARN_SKILLS)
    			rewardSkills();
    	}
    	finally
    	{
    		_subclassLock.unlock();
    	}
	}

	/** Return the Experience of the L2PcInstance. */
	public long getExp() { return getStat().getExp(); }

	public void setActiveEnchantAttrItem(L2ItemInstance stone)
	{
		_activeEnchantAttrItem = stone;
	}

	public L2ItemInstance getActiveEnchantAttrItem()
	{
		return _activeEnchantAttrItem;
	}

	public void setActiveEnchantItem(L2ItemInstance scroll)
	{
		// If we dont have a Enchant Item, we are not enchanting.
		if (scroll == null)
		{
			setActiveEnchantSupportItem(null);
			setActiveEnchantTimestamp(0);
			setIsEnchanting(false);
		}
		_activeEnchantItem = scroll;
	}

	public L2ItemInstance getActiveEnchantItem()
	{
		return _activeEnchantItem;
	}

	public void setActiveEnchantSupportItem(L2ItemInstance item)
	{
		_activeEnchantSupportItem = item;
	}

	public L2ItemInstance getActiveEnchantSupportItem()
	{
		return _activeEnchantSupportItem;
	}

	public long getActiveEnchantTimestamp()
	{
		return _activeEnchantTimestamp;
	}

	public void setActiveEnchantTimestamp(long val)
	{
		_activeEnchantTimestamp = val;
	}

	public void setIsEnchanting(boolean val)
	{
		_isEnchanting = val;
	}
	
	public boolean isEnchanting()
	{
		return _isEnchanting;
	}

	/**
	 * Set the fists weapon of the L2PcInstance (used when no weapon is equiped).<BR><BR>
	 *
	 * @param weaponItem The fists L2Weapon to set to the L2PcInstance
	 *
	 */
	public void setFistsWeaponItem(L2Weapon weaponItem)
	{
		_fistsWeaponItem = weaponItem;
	}

	/**
	 * Return the fists weapon of the L2PcInstance (used when no weapon is equiped).<BR><BR>
	 */
	public L2Weapon getFistsWeaponItem()
	{
		return _fistsWeaponItem;
	}

	/**
	 * Return the fists weapon of the L2PcInstance Class (used when no weapon is equiped).<BR><BR>
	 */
	public L2Weapon findFistsWeaponItem(int classId)
	{
		L2Weapon weaponItem = null;
		if ((classId >= 0x00) && (classId <= 0x09))
		{
			//human fighter fists
			L2Item temp = ItemTable.getInstance().getTemplate(246);
			weaponItem = (L2Weapon)temp;
		}
		else if ((classId >= 0x0a) && (classId <= 0x11))
		{
			//human mage fists
			L2Item temp = ItemTable.getInstance().getTemplate(251);
			weaponItem = (L2Weapon)temp;
		}
		else if ((classId >= 0x12) && (classId <= 0x18))
		{
			//elven fighter fists
			L2Item temp = ItemTable.getInstance().getTemplate(244);
			weaponItem = (L2Weapon)temp;
		}
		else if ((classId >= 0x19) && (classId <= 0x1e))
		{
			//elven mage fists
			L2Item temp = ItemTable.getInstance().getTemplate(249);
			weaponItem = (L2Weapon)temp;
		}
		else if ((classId >= 0x1f) && (classId <= 0x25))
		{
			//dark elven fighter fists
			L2Item temp = ItemTable.getInstance().getTemplate(245);
			weaponItem = (L2Weapon)temp;
		}
		else if ((classId >= 0x26) && (classId <= 0x2b))
		{
			//dark elven mage fists
			L2Item temp = ItemTable.getInstance().getTemplate(250);
			weaponItem = (L2Weapon)temp;
		}
		else if ((classId >= 0x2c) && (classId <= 0x30))
		{
			//orc fighter fists
			L2Item temp = ItemTable.getInstance().getTemplate(248);
			weaponItem = (L2Weapon)temp;
		}
		else if ((classId >= 0x31) && (classId <= 0x34))
		{
			//orc mage fists
			L2Item temp = ItemTable.getInstance().getTemplate(252);
			weaponItem = (L2Weapon)temp;
		}
		else if ((classId >= 0x35) && (classId <= 0x39))
		{
			//dwarven fists
			L2Item temp = ItemTable.getInstance().getTemplate(247);
			weaponItem = (L2Weapon)temp;
		}

		return weaponItem;
	}

	/**
	 * Give Expertise skill of this level and remove beginner Lucky skill.<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Get the Level of the L2PcInstance </li>
	 * <li>If L2PcInstance Level is 5, remove beginner Lucky skill </li>
	 * <li>Add the Expertise skill corresponding to its Expertise level</li>
	 * <li>Update the overloaded status of the L2PcInstance</li><BR><BR>
	 *
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T give other free skills (SP needed = 0)</B></FONT><BR><BR>
	 *
	 */
	public void rewardSkills()
	{
		// Get the Level of the L2PcInstance
		int lvl = getLevel();

		// Remove beginner Lucky skill
		if (lvl == 10)
		{
			L2Skill skill = SkillTable.FrequentSkill.LUCKY.getSkill();
			skill = removeSkill(skill);

			if (Config.DEBUG && skill != null) _log.fine("removed skill 'Lucky' from "+getName());
		}

		// Calculate the current higher Expertise of the L2PcInstance
		for (int i=0; i < EXPERTISE_LEVELS.length; i++)
		{
			if (lvl >= EXPERTISE_LEVELS[i])
				setExpertiseIndex(i);
		}

		// Add the Expertise skill corresponding to its Expertise level
		if (getExpertiseIndex() > 0)
		{
			L2Skill skill = SkillTable.getInstance().getInfo(239, getExpertiseIndex());
			addSkill(skill, true);

			if (Config.DEBUG) _log.fine("awarded "+getName()+" with new expertise.");

		}
		else
		{
			if (Config.DEBUG) _log.fine("No skills awarded at lvl: "+lvl);
		}

		//Active skill dwarven craft

		if (getSkillLevel(1321) < 1 && getRace() == Race.Dwarf)
		{
			L2Skill skill = SkillTable.FrequentSkill.DWARVEN_CRAFT.getSkill();
			addSkill(skill, true);
		}

		//Active skill common craft
		if (getSkillLevel(1322) < 1)
		{
			L2Skill skill = SkillTable.FrequentSkill.COMMON_CRAFT.getSkill();
			addSkill(skill, true);
		}

		for(int i = 0; i < COMMON_CRAFT_LEVELS.length; i++)
		{
			if(lvl >= COMMON_CRAFT_LEVELS[i] && getSkillLevel(1320) < (i+1))
			{
				L2Skill skill = SkillTable.getInstance().getInfo(1320, (i+1));
				addSkill(skill, true);
			}
		}

		// Auto-Learn skills if activated
		if (Config.AUTO_LEARN_SKILLS)
		{
			giveAvailableSkills();
		}
		checkItemRestriction();
		sendSkillList(); 
	}

	/**
	 * Regive all skills which aren't saved to database, like Noble, Hero, Clan Skills<BR><BR>
	 *
	 */
	public void regiveTemporarySkills()
	{
		// Do not call this on enterworld or char load

		// Add noble skills if noble
		if (isNoble())
			setNoble(true);

		// Add Hero skills if hero
		if (isHero())
			setHero(true);

		// Add clan skills 
		if (getClan() != null)
		{
			L2Clan clan = getClan();
			if (clan.getReputationScore() >= 0)
			{
				L2Skill[] skills = getClan().getAllSkills();
				for (L2Skill sk : skills)
				{
					if(sk.getMinPledgeClass() <= getPledgeClass())
						addSkill(sk, false);
				}
			}
			if (clan.getLevel() >= SiegeManager.getInstance().getSiegeClanMinLevel() && isClanLeader())
				SiegeManager.getInstance().addSiegeSkills(this);
			if (getClan().getHasCastle() > 0)
				CastleManager.getInstance().getCastleByOwner(getClan()).giveResidentialSkills(this);
			if (getClan().getHasFort() > 0)
				FortManager.getInstance().getFortByOwner(getClan()).giveResidentialSkills(this);
		}
		
		// Reload passive skills from armors / jewels / weapons
		getInventory().reloadEquippedItems();
		
		// Add Death Penalty Buff Level
		restoreDeathPenaltyBuffLevel();
	}

	/**
	 * Give all available skills to the player.<br><br>
	 *
	 */
	private void giveAvailableSkills()
	{
		int unLearnable = 0;
		int skillCounter = 0;

		// Get available skills
		L2SkillLearn[] skills = SkillTreeTable.getInstance().getAvailableSkills(this, getClassId());
		while (skills.length > unLearnable)
		{
			for (L2SkillLearn s: skills)
			{
				L2Skill sk = SkillTable.getInstance().getInfo(s.getId(), s.getLevel());
				if (sk == null || (sk.getId() == L2Skill.SKILL_DIVINE_INSPIRATION && !Config.AUTO_LEARN_DIVINE_INSPIRATION))
				{
					unLearnable++;
					continue;
				}

				if (getSkillLevel(sk.getId()) == -1)
					skillCounter++;

				// fix when learning toggle skills
				if (sk.isToggle())
				{
					L2Effect toggleEffect = getFirstEffect(sk.getId());
					if (toggleEffect != null)
					{
						// stop old toggle skill effect, and give new toggle skill effect back
						toggleEffect.exit();
						sk.getEffects(this, this);
					}
				}

				addSkill(sk, true);
			}

			// Get new available skills
			skills = SkillTreeTable.getInstance().getAvailableSkills(this, getClassId());
		}

		sendMessage("You have learned " + skillCounter + " new skills.");
	}

	/** Set the Experience value of the L2PcInstance. */
	public void setExp(long exp)
	{
		if (exp < 0)
			exp = 0;
		
		getStat().setExp(exp);
	}

	/**
	 * Return the Race object of the L2PcInstance.<BR><BR>
	 */
	public Race getRace()
	{
		if (!isSubClassActive())
			return getTemplate().race;

		L2PcTemplate charTemp = CharTemplateTable.getInstance().getTemplate(_baseClass);
		return charTemp.race;
	}

    public L2Radar getRadar()
    {
        return _radar;
    }

    /* Return true if Hellbound minimap allowed */
    public boolean isMinimapAllowed()
    {
    	return _minimapAllowed;
    }

    /* Enable or disable minimap on Hellbound */
    public void setMinimapAllowed(boolean b)
    {
    	_minimapAllowed = b;
    }

	/** Return the SP amount of the L2PcInstance. */
	public int getSp() { return getStat().getSp(); }

	/** Set the SP amount of the L2PcInstance. */
	public void setSp(int sp)
	{
		if (sp < 0)
			sp = 0;
		
		super.getStat().setSp(sp);
	}

	/**
	 * Return true if this L2PcInstance is a clan leader in
	 * ownership of the passed castle
	 */
	public boolean isCastleLord(int castleId)
	{
		L2Clan clan = getClan();

		// player has clan and is the clan leader, check the castle info
		if ((clan != null) && (clan.getLeader().getPlayerInstance() == this))
		{
			// if the clan has a castle and it is actually the queried castle, return true
			Castle castle = CastleManager.getInstance().getCastleByOwner(clan);
			if ((castle != null) && (castle == CastleManager.getInstance().getCastleById(castleId)))
				return true;
		}

		return false;
	}
	/**
	 * Return the Clan Identifier of the L2PcInstance.<BR><BR>
	 */
	public int getClanId()
	{
		return _clanId;
	}

	/**
	 * Return the Clan Crest Identifier of the L2PcInstance or 0.<BR><BR>
	 */
	public int getClanCrestId()
	{
		if (_clan != null && _clan.hasCrest())
			return _clan.getCrestId();

		return 0;
	}

	/**
	 * @return The Clan CrestLarge Identifier or 0
	 */
	public int getClanCrestLargeId()
	{
		if (_clan != null && _clan.hasCrestLarge())
			return _clan.getCrestLargeId();

		return 0;
	}

	public long getClanJoinExpiryTime()
	{
		return _clanJoinExpiryTime;
	}

	public void setClanJoinExpiryTime(long time)
	{
		_clanJoinExpiryTime = time;
	}

	public long getClanCreateExpiryTime()
	{
		return _clanCreateExpiryTime;
	}

	public void setClanCreateExpiryTime(long time)
	{
		_clanCreateExpiryTime = time;
	}

	public void setOnlineTime(long time)
	{
		_onlineTime = time;
		_onlineBeginTime = System.currentTimeMillis();
	}

	/**
	 * Return the PcInventory Inventory of the L2PcInstance contained in _inventory.<BR><BR>
	 */
	@Override
	public PcInventory getInventory()
	{
		return _inventory;
	}

	/**
	 * Delete a ShortCut of the L2PcInstance _shortCuts.<BR><BR>
	 */
	public void removeItemFromShortCut(int objectId)
	{
		_shortCuts.deleteShortCutByObjectId(objectId);
	}

	/**
	 * Return True if the L2PcInstance is sitting.<BR><BR>
	 */
	public boolean isSitting()
	{
		return _waitTypeSitting;
	}

	/**
	 * Set _waitTypeSitting to given value
	 */
	public void setIsSitting(boolean state)
	{
		_waitTypeSitting = state;
	}
	/**
	 * Sit down the L2PcInstance, set the AI Intention to AI_INTENTION_REST and send a Server->Client ChangeWaitType packet (broadcast)<BR><BR>
	 */
	public void sitDown()
	{
		if ((isCastingNow() || isCastingSimultaneouslyNow()) && !_relax)
		{
			sendMessage("Cannot sit while casting");
			return;
		}

		if (!_waitTypeSitting && !isAttackingDisabled() && !isOutOfControl() && !isImmobilized())
		{
			breakAttack();
			setIsSitting(true);
			broadcastPacket(new ChangeWaitType (this, ChangeWaitType.WT_SITTING));
			// Schedule a sit down task to wait for the animation to finish
			ThreadPoolManager.getInstance().scheduleGeneral(new SitDownTask(this), 2500);
			setIsParalyzed(true);
		}
	}
	/**
	 * Sit down Task
	 */
	class SitDownTask implements Runnable
	{
		L2PcInstance _player;
		SitDownTask(L2PcInstance player)
		{
			_player = player;
		}
		public void run()
		{
			_player.setIsParalyzed(false);
			_player.getAI().setIntention(CtrlIntention.AI_INTENTION_REST);
		}
	}
	/**
	 * Stand up Task
	 */
	class StandUpTask implements Runnable
	{
		L2PcInstance _player;
		StandUpTask(L2PcInstance player)
		{
			_player = player;
		}
		public void run()
		{
			_player.setIsSitting(false);
			_player.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		}
	}
	/**
	 * Stand up the L2PcInstance, set the AI Intention to AI_INTENTION_IDLE and send a Server->Client ChangeWaitType packet (broadcast)<BR><BR>
	 */
	public void standUp()
	{
		if (L2Event.active && eventSitForced)
		{
			sendMessage("A dark force beyond your mortal understanding makes your knees to shake when you try to stand up ...");
		}
		else if (_waitTypeSitting && !isInStoreMode() && !isAlikeDead())
		{
			if (_relax)
			{
				setRelax(false);
				stopEffects(L2EffectType.RELAXING);
			}

			broadcastPacket(new ChangeWaitType (this, ChangeWaitType.WT_STANDING));
			// Schedule a stand up task to wait for the animation to finish
			ThreadPoolManager.getInstance().scheduleGeneral(new StandUpTask(this), 2500);
		}
	}

	/**
	 * Set the value of the _relax value. Must be True if using skill Relax and False if not.
	 */
	public void setRelax(boolean val)
	{
		_relax = val;
	}

	/**
	 * Return the PcWarehouse object of the L2PcInstance.<BR><BR>
	 */
	public PcWarehouse getWarehouse()
	{
		if (_warehouse == null)
		{
			_warehouse = new PcWarehouse(this);
			_warehouse.restore();
		}
		if (Config.WAREHOUSE_CACHE)
			WarehouseCacheManager.getInstance().addCacheTask(this);
		return _warehouse;
	}

	/**
	 * Free memory used by Warehouse
	 */
	public void clearWarehouse()
	{
		if (_warehouse != null)
			_warehouse.deleteMe();
		_warehouse = null;
	}

	/**
	 * Returns true if refund list is not empty
	 */
	public boolean hasRefund()
	{
		return _refund != null && _refund.getSize() > 0 && Config.ALLOW_REFUND;
	}

	/**
	 * Returns refund object or create new if not exist
	 */
	public PcRefund getRefund()
	{
		if (_refund == null)
			_refund = new PcRefund(this);
		return _refund;
	}

	/**
	 * Clear refund
	 */
	public void clearRefund()
	{
		if (_refund != null)
			_refund.deleteMe();
		_refund = null;
	}

	/**
	 * Return the Identifier of the L2PcInstance.<BR><BR>
	 */
	public int getCharId()
	{
		return _charId;
	}

	/**
	 * Set the Identifier of the L2PcInstance.<BR><BR>
	 */
	public void setCharId(int charId)
	{
		_charId = charId;
	}

	/**
	 * Return the Adena amount of the L2PcInstance.<BR><BR>
	 */
	public long getAdena()
	{
		return _inventory.getAdena();
	}

    /**
     * Return the Ancient Adena amount of the L2PcInstance.<BR><BR>
     */
	public long getAncientAdena()
	{
	    return _inventory.getAncientAdena();
	}

	/**
	 * Add adena to Inventory of the L2PcInstance and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param count : int Quantity of adena to be added
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 */
	public void addAdena(String process, long count, L2Object reference, boolean sendMessage)
	{
		if (sendMessage)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.EARNED_ADENA);
			sm.addItemNumber(count);
			sendPacket(sm);
		}

		if (count > 0)
		{
			_inventory.addAdena(process, count, this, reference);

			// Send update packet
			if (!Config.FORCE_INVENTORY_UPDATE)
			{
				InventoryUpdate iu = new InventoryUpdate();
				iu.addItem(_inventory.getAdenaInstance());
				sendPacket(iu);
			}
			else sendPacket(new ItemList(this, false));
		}
	}

	/**
	 * Reduce adena in Inventory of the L2PcInstance and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param count : long Quantity of adena to be reduced
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successfull
	 */
	public boolean reduceAdena(String process, long count, L2Object reference, boolean sendMessage)
	{
		if (count > getAdena())
		{
			if (sendMessage) sendPacket(new SystemMessage(SystemMessageId.YOU_NOT_ENOUGH_ADENA));
			return false;
		}

		if (count > 0)
		{
			L2ItemInstance adenaItem = _inventory.getAdenaInstance();
			if (!_inventory.reduceAdena(process, count, this, reference))
				return false;

			// Send update packet
			if (!Config.FORCE_INVENTORY_UPDATE)
			{
				InventoryUpdate iu = new InventoryUpdate();
				iu.addItem(adenaItem);
				sendPacket(iu);
			}
			else sendPacket(new ItemList(this, false));

			if (sendMessage)
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.DISAPPEARED_ADENA);
				sm.addItemNumber(count);
				sendPacket(sm);
			}
		}

		return true;
	}

	/**
	 * Add ancient adena to Inventory of the L2PcInstance and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 *
	 * @param process : String Identifier of process triggering this action
	 * @param count : int Quantity of ancient adena to be added
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 */
	public void addAncientAdena(String process, long count, L2Object reference, boolean sendMessage)
	{
	    if (sendMessage)
	    {
	        SystemMessage sm = new SystemMessage(SystemMessageId.EARNED_S2_S1_S);
	        sm.addItemName(PcInventory.ANCIENT_ADENA_ID);
            sm.addItemNumber(count);
	        sendPacket(sm);
	    }

	    if (count > 0)
	    {
	        _inventory.addAncientAdena(process, count, this, reference);

	        if (!Config.FORCE_INVENTORY_UPDATE)
	        {
	            InventoryUpdate iu = new InventoryUpdate();
	            iu.addItem(_inventory.getAncientAdenaInstance());
	            sendPacket(iu);
	        }
	        else sendPacket(new ItemList(this, false));
	    }
	}

	/**
	 * Reduce ancient adena in Inventory of the L2PcInstance and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param count : long Quantity of ancient adena to be reduced
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successfull
	 */
	public boolean reduceAncientAdena(String process, long count, L2Object reference, boolean sendMessage)
	{
		if (count > getAncientAdena())
		{
			if (sendMessage)
				sendPacket(new SystemMessage(SystemMessageId.YOU_NOT_ENOUGH_ADENA));
			
			return false;
		}
		
		if (count > 0)
		{
			L2ItemInstance ancientAdenaItem = _inventory.getAncientAdenaInstance();
			if (!_inventory.reduceAncientAdena(process, count, this, reference))
				return false;
			
			if (!Config.FORCE_INVENTORY_UPDATE)
			{
				InventoryUpdate iu = new InventoryUpdate();
				iu.addItem(ancientAdenaItem);
				sendPacket(iu);
			}
			else
			{
				sendPacket(new ItemList(this, false));
			}
			
			if (sendMessage)
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.S2_S1_DISAPPEARED);
				sm.addItemName(PcInventory.ANCIENT_ADENA_ID);
				sm.addItemNumber(count);
				sendPacket(sm);
			}
		}
		
		return true;
	}

	/**
	 * Adds item to inventory and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param item : L2ItemInstance to be added
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 */
	public void addItem(String process, L2ItemInstance item, L2Object reference, boolean sendMessage)
	{
		if (item.getCount() > 0)
		{
			// Sends message to client if requested
			if (sendMessage)
			{
				if (item.getCount() > 1)
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.YOU_PICKED_UP_S1_S2);
					sm.addItemName(item);
					sm.addItemNumber(item.getCount());
					sendPacket(sm);
				}
				else if (item.getEnchantLevel() > 0)
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.YOU_PICKED_UP_A_S1_S2);
					sm.addNumber(item.getEnchantLevel());
					sm.addItemName(item);
					sendPacket(sm);
				}
				else
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.YOU_PICKED_UP_S1);
					sm.addItemName(item);
					sendPacket(sm);
				}
			}

			// Add the item to inventory
			L2ItemInstance newitem = _inventory.addItem(process, item, this, reference);

			// Send inventory update packet
			if (!Config.FORCE_INVENTORY_UPDATE)
			{
				InventoryUpdate playerIU = new InventoryUpdate();
				playerIU.addItem(newitem);
				sendPacket(playerIU);
			}
			else
			{
			    sendPacket(new ItemList(this, false));
			}

			// Update current load as well
			StatusUpdate su = new StatusUpdate(getObjectId());
			su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
			sendPacket(su);

            // If over capacity, drop the item
            if (!isGM() && !_inventory.validateCapacity(0) && newitem.isDropable())
                dropItem("InvDrop", newitem, null, true);

			// Cursed Weapon
            else if(CursedWeaponsManager.getInstance().isCursed(newitem.getItemId()))
			{
				CursedWeaponsManager.getInstance().activate(this, newitem);
			}
			
			// Combat Flag
            else if(FortSiegeManager.getInstance().isCombat(item.getItemId()))
            {
            	if(FortSiegeManager.getInstance().activateCombatFlag(this, item))
            	{
            		Fort fort = FortManager.getInstance().getFort(this);
            		fort.getSiege().announceToPlayer(new SystemMessage(SystemMessageId.C1_ACQUIRED_THE_FLAG), this.getName());
            	}
            }
			// Territory Ward
			else if (item.getItemId() >= 13560 && item.getItemId() <= 13568)
			{
				TerritoryWard ward = TerritoryWarManager.getInstance().getTerritoryWard(item.getItemId() - 13479);
				if (ward != null)
					ward.activate(this, item);
			}
		}
	}

	/**
	 * Adds item to Inventory and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param itemId : int Item Identifier of the item to be added
	 * @param count : long Quantity of items to be added
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 */
	public void addItem(String process, int itemId, long count, L2Object reference, boolean sendMessage)
	{
		if (count > 0)
		{
			L2ItemInstance item = null;
			if(ItemTable.getInstance().getTemplate(itemId) != null)
			{
				item = ItemTable.getInstance().createDummyItem(itemId);
			}
			else
			{
				_log.log(Level.SEVERE, "Item doesn't exist so cannot be added. Item ID: " + itemId);
				return;
			}
			// Sends message to client if requested
			if (sendMessage && ((!isCastingNow() && item.getItemType() == L2EtcItemType.HERB)
					|| item.getItemType() != L2EtcItemType.HERB))
			{
				if (count > 1)
				{
					if (process.equalsIgnoreCase("sweep") || process.equalsIgnoreCase("Quest"))
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.EARNED_S2_S1_S);
						sm.addItemName(itemId);
						sm.addItemNumber(count);
						sendPacket(sm);
					}
					else
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.YOU_PICKED_UP_S1_S2);
						sm.addItemName(itemId);
						sm.addItemNumber(count);
						sendPacket(sm);
					}
				}
				else
				{
					if (process.equalsIgnoreCase("sweep") || process.equalsIgnoreCase("Quest"))
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.EARNED_ITEM);
						sm.addItemName(itemId);
						sendPacket(sm);
					}
					else
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.YOU_PICKED_UP_S1);
						sm.addItemName(itemId);
						sendPacket(sm);
					}
				}
			}
			//Auto use herbs - autoloot
			if (item.getItemType() == L2EtcItemType.HERB) //If item is herb dont add it to iv :]
			{
				if(!isCastingNow())
				{
					L2ItemInstance herb = new L2ItemInstance(_charId, itemId);
					IItemHandler handler = ItemHandler.getInstance().getItemHandler(herb.getEtcItem());
					if (handler == null)
						_log.warning("No item handler registered for Herb - item ID " + herb.getItemId() + ".");
					else
					{
						handler.useItem(this, herb);
						if(_herbstask>=100)
							_herbstask -=100;
					}
				}
				else
				{
					_herbstask += 100;
					ThreadPoolManager.getInstance().scheduleAi(new HerbTask(process, itemId, count, reference, sendMessage), _herbstask);
				}
			}
			else
			{
				// Add the item to inventory
				L2ItemInstance createdItem = _inventory.addItem(process, itemId, count, this, reference);

				// If over capacity, drop the item
				if (!isGM() && !_inventory.validateCapacity(0) && createdItem.isDropable())
					dropItem("InvDrop", createdItem, null, true);

				// Cursed Weapon
				else if(CursedWeaponsManager.getInstance().isCursed(createdItem.getItemId()))
					CursedWeaponsManager.getInstance().activate(this, createdItem);

				// Combat Flag
				else if(FortSiegeManager.getInstance().isCombat(createdItem.getItemId()))
				{
					if( FortSiegeManager.getInstance().activateCombatFlag(this, item))
					{
						Fort fort = FortManager.getInstance().getFort(this);
						fort.getSiege().announceToPlayer(new SystemMessage(SystemMessageId.C1_ACQUIRED_THE_FLAG), this.getName());
					}
				}
				// Territory Ward
				else if (createdItem.getItemId() >= 13560 && createdItem.getItemId() <= 13568)
				{
					TerritoryWard ward = TerritoryWarManager.getInstance().getTerritoryWard(createdItem.getItemId() - 13479);
					if (ward != null)
						ward.activate(this, createdItem);
				}
			}
		}
	}

	/**
	 * Destroy item from inventory and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param item : L2ItemInstance to be destroyed
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successfull
	 */
	public boolean destroyItem(String process, L2ItemInstance item, L2Object reference, boolean sendMessage)
	{
		return this.destroyItem(process, item, item.getCount(), reference, sendMessage);
	}
    
    /**
     * Destroy item from inventory and send a Server->Client InventoryUpdate packet to the L2PcInstance.
     * @param process : String Identifier of process triggering this action
     * @param item : L2ItemInstance to be destroyed
     * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
     * @param sendMessage : boolean Specifies whether to send message to Client about this action
     * @return boolean informing if the action was successfull
     */
    public boolean destroyItem(String process, L2ItemInstance item, long count, L2Object reference, boolean sendMessage)
    {
        item = _inventory.destroyItem(process, item, count, this, reference);

        if (item == null)
        {
            if (sendMessage)
            {
                sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
            }
            return false;
        }

        // Send inventory update packet
        if (!Config.FORCE_INVENTORY_UPDATE)
        {
            InventoryUpdate playerIU = new InventoryUpdate();
            playerIU.addItem(item);
            sendPacket(playerIU);
        }
        else 
        {
            sendPacket(new ItemList(this, false));
        }

        // Update current load as well
        StatusUpdate su = new StatusUpdate(getObjectId());
        su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
        sendPacket(su);

        // Sends message to client if requested
        if (sendMessage)
        {
            SystemMessage sm = new SystemMessage(SystemMessageId.S2_S1_DISAPPEARED);
            sm.addItemName(item);
            sm.addItemNumber(count);
            sendPacket(sm);
        }

        return true;
    }

	/**
	 * Destroys item from inventory and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param objectId : int Item Instance identifier of the item to be destroyed
	 * @param count : int Quantity of items to be destroyed
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successfull
	 */
	@Override
	public boolean destroyItem(String process, int objectId, long count, L2Object reference, boolean sendMessage)
	{
        L2ItemInstance item = _inventory.getItemByObjectId(objectId);

		if (item == null)
		{
			if (sendMessage)
            {
                sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
            }
            
			return false;
		}
        return this.destroyItem(process, item, count, reference, sendMessage);
	}

	/**
	 * Destroys shots from inventory without logging and only occasional saving to database.
	 * Sends a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param objectId : int Item Instance identifier of the item to be destroyed
	 * @param count : int Quantity of items to be destroyed
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successfull
	 */
	public boolean destroyItemWithoutTrace(String process, int objectId, long count, L2Object reference, boolean sendMessage)
	{
        L2ItemInstance item = _inventory.getItemByObjectId(objectId);

        if (item == null || item.getCount() < count)
		{
			if (sendMessage)
            {
                sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
            }
            
			return false;
		}
        
        return this.destroyItem(null, item, count, reference, sendMessage);
	}

	/**
	 * Destroy item from inventory by using its <B>itemId</B> and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param itemId : int Item identifier of the item to be destroyed
	 * @param count : int Quantity of items to be destroyed
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successfull
	 */
	@Override
	public boolean destroyItemByItemId(String process, int itemId, long count, L2Object reference, boolean sendMessage)
	{
        L2ItemInstance item = _inventory.getItemByItemId(itemId);

        if (item == null || item.getCount() < count || _inventory.destroyItemByItemId(process, itemId, count, this, reference) == null)
		{
			if (sendMessage)
                sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));

			return false;
		}

		// Send inventory update packet
		if (!Config.FORCE_INVENTORY_UPDATE)
		{
			InventoryUpdate playerIU = new InventoryUpdate();
			playerIU.addItem(item);
			sendPacket(playerIU);
		}
        else sendPacket(new ItemList(this, false));

		// Update current load as well
		StatusUpdate su = new StatusUpdate(getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
		sendPacket(su);

		// Sends message to client if requested
		if (sendMessage)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.S2_S1_DISAPPEARED);
            sm.addItemName(itemId);
            sm.addItemNumber(count);
			sendPacket(sm);
		}

		return true;
	}

	/**
	 * Destroy all weared items from inventory and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successfull
	 */
	public void destroyWearedItems(String process, L2Object reference, boolean sendMessage)
	{

        // Go through all Items of the inventory
        for (L2ItemInstance item : getInventory().getItems())
		{
            // Check if the item is a Try On item in order to remove it
			if (item.isWear())
			{
                if (item.isEquipped())
                    getInventory().unEquipItemInSlotAndRecord(item.getLocationSlot());

				if (_inventory.destroyItem(process, item, this, reference) == null)
				{
					_log.warning("Player " + getName() + " can't destroy weared item: " + item.getName() + "[ " + item.getObjectId() + " ]");
					continue;
				}

				// Send an Unequipped Message in system window of the player for each Item
				SystemMessage sm = new SystemMessage(SystemMessageId.S1_DISARMED);
				sm.addItemName(item);
				sendPacket(sm);

			}
		}


        // Send the StatusUpdate Server->Client Packet to the player with new CUR_LOAD (0x0e) information
		StatusUpdate su = new StatusUpdate(getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
		sendPacket(su);

        // Send the ItemList Server->Client Packet to the player in order to refresh its Inventory
        ItemList il = new ItemList(getInventory().getItems(), true);
        sendPacket(il);

        // Send a Server->Client packet UserInfo to this L2PcInstance and CharInfo to all L2PcInstance in its _KnownPlayers
		broadcastUserInfo();

		// Sends message to client if requested
		sendMessage("Trying-on mode has ended.");

	}

	/**
	 * Transfers item to another ItemContainer and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param itemId : int Item Identifier of the item to be transfered
	 * @param count : long Quantity of items to be transfered
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the new item or the updated item in inventory
	 */
	public L2ItemInstance transferItem(String process, int objectId, long count, Inventory target, L2Object reference)
	{
		L2ItemInstance oldItem = checkItemManipulation(objectId, count, "transfer");
		if (oldItem == null) return null;
		L2ItemInstance newItem = getInventory().transferItem(process, objectId, count, target, this, reference);
		if (newItem == null) return null;

		// Send inventory update packet
		if (!Config.FORCE_INVENTORY_UPDATE)
		{
			InventoryUpdate playerIU = new InventoryUpdate();

			if (oldItem.getCount() > 0 && oldItem != newItem)
                playerIU.addModifiedItem(oldItem);
			else
                playerIU.addRemovedItem(oldItem);

			sendPacket(playerIU);
		}
		else sendPacket(new ItemList(this, false));

		// Update current load as well
		StatusUpdate playerSU = new StatusUpdate(getObjectId());
		playerSU.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
		sendPacket(playerSU);

		// Send target update packet
		if (target instanceof PcInventory)
		{
			L2PcInstance targetPlayer = ((PcInventory)target).getOwner();

			if (!Config.FORCE_INVENTORY_UPDATE)
			{
				InventoryUpdate playerIU = new InventoryUpdate();

				if (newItem.getCount() > count)
                    playerIU.addModifiedItem(newItem);
				else
                    playerIU.addNewItem(newItem);

				targetPlayer.sendPacket(playerIU);
			}
			else targetPlayer.sendPacket(new ItemList(targetPlayer, false));

			// Update current load as well
			playerSU = new StatusUpdate(targetPlayer.getObjectId());
			playerSU.addAttribute(StatusUpdate.CUR_LOAD, targetPlayer.getCurrentLoad());
			targetPlayer.sendPacket(playerSU);
		}
		else if (target instanceof PetInventory)
		{
			PetInventoryUpdate petIU = new PetInventoryUpdate();

			if (newItem.getCount() > count)
                petIU.addModifiedItem(newItem);
			else
                petIU.addNewItem(newItem);

			((PetInventory)target).getOwner().getOwner().sendPacket(petIU);
			getPet().getInventory().refreshWeight();
		}
		return newItem;
	}

	/**
	 * Drop item from inventory and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param item : L2ItemInstance to be dropped
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successfull
	 */
	public boolean dropItem(String process, L2ItemInstance item, L2Object reference, boolean sendMessage)
	{
		item = _inventory.dropItem(process, item, this, reference);

		if (item == null)
		{
			if (sendMessage)
                sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));

			return false;
		}

		item.dropMe(this, getX() + Rnd.get(50) - 25, getY() + Rnd.get(50) - 25, getZ() + 20);

		 if (Config.AUTODESTROY_ITEM_AFTER >0 && Config.DESTROY_DROPPED_PLAYER_ITEM && !Config.LIST_PROTECTED_ITEMS.contains(item.getItemId()))
			 {
			 if ( (item.isEquipable() && Config.DESTROY_EQUIPABLE_PLAYER_ITEM) || !item.isEquipable())
			 ItemsAutoDestroy.getInstance().addItem(item);
			 }
		 if (Config.DESTROY_DROPPED_PLAYER_ITEM){
		 	if (!item.isEquipable() || (item.isEquipable()  && Config.DESTROY_EQUIPABLE_PLAYER_ITEM ))
		 		item.setProtected(false);
			else
				item.setProtected(true);
		 	}
	   		else
	   			item.setProtected(true);

		// Send inventory update packet
		if (!Config.FORCE_INVENTORY_UPDATE)
		{
			InventoryUpdate playerIU = new InventoryUpdate();
			playerIU.addItem(item);
			sendPacket(playerIU);
		}
		else sendPacket(new ItemList(this, false));

		// Update current load as well
		StatusUpdate su = new StatusUpdate(getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
		sendPacket(su);

		// Sends message to client if requested
		if (sendMessage)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.YOU_DROPPED_S1);
			sm.addItemName(item);
			sendPacket(sm);
		}

		return true;
	}

	/**
	 * Drop item from inventory by using its <B>objectID</B> and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param objectId : int Item Instance identifier of the item to be dropped
	 * @param count : long Quantity of items to be dropped
	 * @param x : int coordinate for drop X
	 * @param y : int coordinate for drop Y
	 * @param z : int coordinate for drop Z
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return L2ItemInstance corresponding to the new item or the updated item in inventory
	 */
	public L2ItemInstance dropItem(String process, int objectId, long count, int x, int y, int z, L2Object reference, boolean sendMessage)
	{
		L2ItemInstance invitem = _inventory.getItemByObjectId(objectId);
		L2ItemInstance item = _inventory.dropItem(process, objectId, count, this, reference);

		if (item == null)
		{
			if (sendMessage)
                sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));

			return null;
		}

		item.dropMe(this, x, y, z);

		 if (Config.AUTODESTROY_ITEM_AFTER >0 && Config.DESTROY_DROPPED_PLAYER_ITEM && !Config.LIST_PROTECTED_ITEMS.contains(item.getItemId()))
			 {
			 if ( (item.isEquipable() && Config.DESTROY_EQUIPABLE_PLAYER_ITEM) || !item.isEquipable())
			 ItemsAutoDestroy.getInstance().addItem(item);
			 }
		 if (Config.DESTROY_DROPPED_PLAYER_ITEM){
		 	if (!item.isEquipable() || (item.isEquipable()  && Config.DESTROY_EQUIPABLE_PLAYER_ITEM ))
		 		item.setProtected(false);
		 	else
		 		item.setProtected(true);
		 }
	   	 else
	   		 item.setProtected(true);

		// Send inventory update packet
		if (!Config.FORCE_INVENTORY_UPDATE)
		{
			InventoryUpdate playerIU = new InventoryUpdate();
			playerIU.addItem(invitem);
			sendPacket(playerIU);
		}
		else
		{
		    sendPacket(new ItemList(this, false));
		}

		// Update current load as well
		StatusUpdate su = new StatusUpdate(getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
		sendPacket(su);

		// Sends message to client if requested
		if (sendMessage)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.YOU_DROPPED_S1);
			sm.addItemName(item);
			sendPacket(sm);
		}

		return item;
	}

	public L2ItemInstance checkItemManipulation(int objectId, long count, String action)
	{
        //TODO: if we remove objects that are not visisble from the L2World, we'll have to remove this check
		if (L2World.getInstance().findObject(objectId) == null)
		{
			_log.finest(getObjectId()+": player tried to " + action + " item not available in L2World");
			return null;
		}

		L2ItemInstance item = getInventory().getItemByObjectId(objectId);

		if (item == null || item.getOwnerId() != getObjectId())
		{
			_log.finest(getObjectId()+": player tried to " + action + " item he is not owner of");
			return null;
		}

		if (count < 0 || (count > 1 && !item.isStackable()))
		{
			_log.finest(getObjectId()+": player tried to " + action + " item with invalid count: "+ count);
			return null;
		}

		if (count > item.getCount())
		{
			_log.finest(getObjectId()+": player tried to " + action + " more items than he owns");
			return null;
		}

		// Pet is summoned and not the item that summoned the pet AND not the buggle from strider you're mounting
		if (getPet() != null && getPet().getControlItemId() == objectId || getMountObjectID() == objectId)
		{
			if (Config.DEBUG)
				_log.finest(getObjectId()+": player tried to " + action + " item controling pet");

			return null;
		}

		if (getActiveEnchantItem() != null && getActiveEnchantItem().getObjectId() == objectId)
		{
			if (Config.DEBUG)
				_log.finest(getObjectId()+":player tried to " + action + " an enchant scroll he was using");

			return null;
		}

		if (item.isWear())
		{
			// cannot drop/trade wear-items
			return null;
		}
		
		// We cannot put a Weapon with Augmention in WH while casting (Possible Exploit)
		if (item.isAugmented() && (isCastingNow() || this.isCastingSimultaneouslyNow()))
			return null;

		return item;
	}

	/**
	 * Set _protectEndTime according settings.
	 */
	public void setProtection(boolean protect)
	{
		if (Config.DEVELOPER && (protect || _protectEndTime > 0))
            _log.warning(getName() + ": Protection " + (protect?"ON " + (GameTimeController.getGameTicks() + Config.PLAYER_SPAWN_PROTECTION * GameTimeController.TICKS_PER_SECOND) :"OFF") + " (currently " + GameTimeController.getGameTicks() + ")");

		_protectEndTime = protect ? GameTimeController.getGameTicks() + Config.PLAYER_SPAWN_PROTECTION * GameTimeController.TICKS_PER_SECOND : 0;
	}

	/**
	 * Set protection from agro mobs when getting up from fake death, according settings.
	 */
	public void setRecentFakeDeath(boolean protect)
	{
		_recentFakeDeathEndTime = protect ? GameTimeController.getGameTicks() + Config.PLAYER_FAKEDEATH_UP_PROTECTION * GameTimeController.TICKS_PER_SECOND : 0;
	}

	public boolean isRecentFakeDeath()
	{
		return _recentFakeDeathEndTime > GameTimeController.getGameTicks();
	}
	
	public final boolean isFakeDeath()
	{
		return _isFakeDeath;
	}
	
	public final void setIsFakeDeath(boolean value)
	{
		_isFakeDeath = value;
	}
	
	@Override
	public final boolean isAlikeDead()
	{
		if (super.isAlikeDead())
			return true;
		
		return isFakeDeath();
	}

	/**
	 * Get the client owner of this char.<BR><BR>
	 */
	public L2GameClient getClient()
	{
		return _client;
	}

	public void setClient(L2GameClient client)
	{
		_client = client;
	}

	/**
	 * Close the active connection with the client.<BR><BR>
	 */
	public void closeNetConnection(boolean closeClient)
	{
		L2GameClient client = _client;
		if (client != null)
		{
			if (client.isDetached())
			{
				client.cleanMe(true);
			}
			else
			{
				if (!client.getConnection().isClosed())
				{
					if (closeClient)
						client.close(LeaveWorld.STATIC_PACKET);
					else
						client.close(ServerClose.STATIC_PACKET);
				}
			}
		}
	}
	
	public Point3D getCurrentSkillWorldPosition()
	{
		return _currentSkillWorldPosition;
	}
	 
	public void setCurrentSkillWorldPosition(Point3D worldPosition)
	{
		_currentSkillWorldPosition = worldPosition;
	}

	/**
	 * 
	 * @see com.l2jserver.gameserver.model.actor.L2Character#enableSkill(com.l2jserver.gameserver.model.L2Skill)
	 */
	@Override
	public void enableSkill(L2Skill skill)
	{
		super.enableSkill(skill);
		_reuseTimeStamps.remove(Integer.valueOf(skill.getReuseHashCode()));
	}

	/**
	 * 
	 * @see com.l2jserver.gameserver.model.actor.L2Character#checkDoCastConditions(com.l2jserver.gameserver.model.L2Skill)
	 */
	@Override
	protected boolean checkDoCastConditions(L2Skill skill)
	{
		if (!super.checkDoCastConditions(skill))
			return false;

		switch (skill.getSkillType())
		{
			case SUMMON_TRAP:
			{
				if (isInsideZone(ZONE_PEACE))
				{
					sendPacket(new SystemMessage(SystemMessageId.A_MALICIOUS_SKILL_CANNOT_BE_USED_IN_PEACE_ZONE));
					return false;
				}
				if (getTrap() != null
						&& getTrap().getSkill().getId() == ((L2SkillTrap)skill).getTriggerSkillId())
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
					sm.addSkillName(skill);
					sendPacket(sm);
					return false;
				}
				break;
			}
			case SUMMON:
			{
				if (!((L2SkillSummon)skill).isCubic() && (getPet() != null || isMounted()))
				{
					if (Config.DEBUG)
						_log.fine("player has a pet already. ignore summon skill");
					
					sendPacket(new SystemMessage(SystemMessageId.YOU_ALREADY_HAVE_A_PET));
					return false;
				}
			}
		}

		// TODO: Should possibly be checked only in L2PcInstance's useMagic
		// Can't use Hero and resurrect skills during Olympiad
		if (isInOlympiadMode() &&
				(skill.isHeroSkill() || skill.getSkillType() == L2SkillType.RESURRECT))
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.THIS_SKILL_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT);
			sendPacket(sm);
			return false;
		}

		final int charges = getCharges();
		// Check if the spell using charges or not in AirShip
		if ((skill.getMaxCharges() == 0 && charges < skill.getNumCharges())
				|| isInAirShip())
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
			sm.addSkillName(skill);
			sendPacket(sm);
			return false;
		}

		return true;
	}

	/**
	 * Returns true if cp update should be done, false if not
	 * @return boolean
	 */
	private boolean needCpUpdate(int barPixels)
	{
		double currentCp = getCurrentCp();

	    if (currentCp <= 1.0 || getMaxCp() < barPixels)
	        return true;

	    if (currentCp <= _cpUpdateDecCheck || currentCp >= _cpUpdateIncCheck)
	    {
	    	if (currentCp == getMaxCp())
	    	{
	    		_cpUpdateIncCheck = currentCp + 1;
	    		_cpUpdateDecCheck = currentCp - _cpUpdateInterval;
	    	}
	    	else
	    	{
	    		double doubleMulti = currentCp / _cpUpdateInterval;
		    	int intMulti = (int)doubleMulti;

	    		_cpUpdateDecCheck = _cpUpdateInterval * (doubleMulti < intMulti ? intMulti-- : intMulti);
	    		_cpUpdateIncCheck = _cpUpdateDecCheck + _cpUpdateInterval;
	    	}

	    	return true;
	    }

	    return false;
	}

	/**
	 * Returns true if mp update should be done, false if not
	 * @return boolean
	 */
	private boolean needMpUpdate(int barPixels)
	{
		double currentMp = getCurrentMp();

	    if (currentMp <= 1.0 || getMaxMp() < barPixels)
	        return true;

	    if (currentMp <= _mpUpdateDecCheck || currentMp >= _mpUpdateIncCheck)
	    {
	    	if (currentMp == getMaxMp())
	    	{
	    		_mpUpdateIncCheck = currentMp + 1;
	    		_mpUpdateDecCheck = currentMp - _mpUpdateInterval;
	    	}
	    	else
	    	{
	    		double doubleMulti = currentMp / _mpUpdateInterval;
		    	int intMulti = (int)doubleMulti;

	    		_mpUpdateDecCheck = _mpUpdateInterval * (doubleMulti < intMulti ? intMulti-- : intMulti);
	    		_mpUpdateIncCheck = _mpUpdateDecCheck + _mpUpdateInterval;
	    	}

	    	return true;
	    }

	    return false;
	}

	/**
	 * Send packet StatusUpdate with current HP,MP and CP to the L2PcInstance and only current HP, MP and Level to all other L2PcInstance of the Party.<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Send the Server->Client packet StatusUpdate with current HP, MP and CP to this L2PcInstance </li><BR>
	 * <li>Send the Server->Client packet PartySmallWindowUpdate with current HP, MP and Level to all other L2PcInstance of the Party </li><BR><BR>
	 *
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T SEND current HP and MP to all L2PcInstance of the _statusListener</B></FONT><BR><BR>
	 *
	 */
	@Override
	public void broadcastStatusUpdate()
	{
		//TODO We mustn't send these informations to other players
		// Send the Server->Client packet StatusUpdate with current HP and MP to all L2PcInstance that must be informed of HP/MP updates of this L2PcInstance
		//super.broadcastStatusUpdate();

		// Send the Server->Client packet StatusUpdate with current HP, MP and CP to this L2PcInstance
		StatusUpdate su = new StatusUpdate(getObjectId());
		su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
		su.addAttribute(StatusUpdate.CUR_MP, (int) getCurrentMp());
		su.addAttribute(StatusUpdate.CUR_CP, (int) getCurrentCp());
		su.addAttribute(StatusUpdate.MAX_CP, 	   getMaxCp());
		sendPacket(su);

		final boolean needCpUpdate = needCpUpdate(352);
		final boolean needHpUpdate = needHpUpdate(352);
		// Check if a party is in progress and party window update is usefull
		if (isInParty()
				&& (needCpUpdate
						|| needHpUpdate
						|| needMpUpdate(352)))
		{
			if (Config.DEBUG)
				_log.fine("Send status for party window of " + getObjectId() + "(" + getName() + ") to his party. CP: " + getCurrentCp() + " HP: " + getCurrentHp() + " MP: " + getCurrentMp());
			// Send the Server->Client packet PartySmallWindowUpdate with current HP, MP and Level to all other L2PcInstance of the Party
			PartySmallWindowUpdate update = new PartySmallWindowUpdate(this);
			getParty().broadcastToPartyMembers(this, update);
		}

        if (isInOlympiadMode()
        		&& isOlympiadStart()
        		&& (needCpUpdate || needHpUpdate))
        {
        	Collection<L2PcInstance> players = getKnownList().getKnownPlayers().values();

        	//synchronized (getKnownList().getKnownPlayers())
        	if (players != null && !players.isEmpty())
			{
            	ExOlympiadUserInfo olyInfo = new ExOlympiadUserInfo(this);

            	for (L2PcInstance player : players)
				{
					if (player != null
							&& player.isInOlympiadMode()
							&& player.getOlympiadGameId() == _olympiadGameId)
					{
						player.sendPacket(olyInfo);
					}
				}
			}

        	players = Olympiad.getInstance().getSpectators(_olympiadGameId);
            if(players != null && !players.isEmpty())
            {
            	ExOlympiadUserInfo olyInfo = new ExOlympiadUserInfo(this);

            	for(L2PcInstance spectator : players)
                {
                    if (spectator == null)
                    	continue;
                    spectator.sendPacket(olyInfo);
                }
            }
        }
        // In duel MP updated only with CP or HP
        if (isInDuel()
        		&& (needCpUpdate || needHpUpdate))
        {
        	ExDuelUpdateUserInfo update = new ExDuelUpdateUserInfo(this);
        	DuelManager.getInstance().broadcastToOppositTeam(this, update);
        }
	}
    
	/**
	 * Send a Server->Client packet UserInfo to this L2PcInstance and CharInfo to all L2PcInstance in its _KnownPlayers.<BR><BR>
	 *
	 * <B><U> Concept</U> :</B><BR><BR>
	 * Others L2PcInstance in the detection area of the L2PcInstance are identified in <B>_knownPlayers</B>.
	 * In order to inform other players of this L2PcInstance state modifications, server just need to go through _knownPlayers to send Server->Client Packet<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Send a Server->Client packet UserInfo to this L2PcInstance (Public and Private Data)</li>
	 * <li>Send a Server->Client packet CharInfo to all L2PcInstance in _KnownPlayers of the L2PcInstance (Public data only)</li><BR><BR>
	 *
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : DON'T SEND UserInfo packet to other players instead of CharInfo packet.
	 * Indeed, UserInfo packet contains PRIVATE DATA as MaxHP, STR, DEX...</B></FONT><BR><BR>
	 *
	 */
	public final void broadcastUserInfo()
	{
		// Send a Server->Client packet UserInfo to this L2PcInstance
		sendPacket(new UserInfo(this));

		// Send a Server->Client packet CharInfo to all L2PcInstance in _KnownPlayers of the L2PcInstance
		if (Config.DEBUG)
            _log.fine("players to notify:" + getKnownList().getKnownPlayers().size() + " packet: [S] 03 CharInfo");

		broadcastPacket(new CharInfo(this));
		broadcastPacket(new ExBrExtraUserInfo(this));
	}

	public final void broadcastTitleInfo()
	{
		// Send a Server->Client packet UserInfo to this L2PcInstance
		sendPacket(new UserInfo(this));
		sendPacket(new ExBrExtraUserInfo(this));

		// Send a Server->Client packet TitleUpdate to all L2PcInstance in _KnownPlayers of the L2PcInstance
		if (Config.DEBUG)
            _log.fine("players to notify:" + getKnownList().getKnownPlayers().size() + " packet: [S] cc TitleUpdate");

		broadcastPacket(new NicknameChanged(this));
	}

	@Override
	public final void broadcastPacket(L2GameServerPacket mov)
	{
		if (!(mov instanceof CharInfo))
			sendPacket(mov);

		mov.setInvisible(getAppearance().getInvisible());

		Collection<L2PcInstance> plrs = getKnownList().getKnownPlayers().values();
		//synchronized (getKnownList().getKnownPlayers())
		{
			for (L2PcInstance player : plrs)
			{
				if (player == null)
					continue;
				player.sendPacket(mov);
				if (mov instanceof CharInfo)
				{
					int relation = getRelation(player);
					if (getKnownList().getKnownRelations().get(player.getObjectId()) != null
					        && getKnownList().getKnownRelations().get(player.getObjectId()) != relation)
					{
						player.sendPacket(new RelationChanged(this, relation, player.isAutoAttackable(this)));
						if (getPet() != null)
							player.sendPacket(new RelationChanged(getPet(), relation, player.isAutoAttackable(this)));
					}
				}
			}
		}
	}

	@Override
	public void broadcastPacket(L2GameServerPacket mov, int radiusInKnownlist)
	{
		if (!(mov instanceof CharInfo))
			sendPacket(mov);

		mov.setInvisible(getAppearance().getInvisible());

		Collection<L2PcInstance> plrs = getKnownList().getKnownPlayers().values();
		//synchronized (getKnownList().getKnownPlayers())
		{
			for (L2PcInstance player : plrs)
			{
				if (player == null)
					continue;
				if (isInsideRadius(player, radiusInKnownlist, false, false))
				{
					player.sendPacket(mov);
					if (mov instanceof CharInfo)
					{
						int relation = getRelation(player);
						if (getKnownList().getKnownRelations().get(player.getObjectId()) != null
						        && getKnownList().getKnownRelations().get(player.getObjectId()) != relation)
						{
							player.sendPacket(new RelationChanged(this, relation, player.isAutoAttackable(this)));
							if (getPet() != null)
								player.sendPacket(new RelationChanged(getPet(), relation, player.isAutoAttackable(this)));
						}
					}
				}
			}
		}
	}

	/**
	 * Return the Alliance Identifier of the L2PcInstance.<BR><BR>
	 */
	public int getAllyId()
	{
		if (_clan == null)
			return 0;
		else
			return _clan.getAllyId();
	}

	public int getAllyCrestId()
	{
		if (getClanId() == 0)
		{
			return 0;
		}
		if (getClan().getAllyId() == 0)
		{
			return 0;
		}
		return getClan().getAllyCrestId();
	}

	/**
	 * Manage hit process (called by Hit Task of L2Character).<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>If the attacker/target is dead or use fake death, notify the AI with EVT_CANCEL and send a Server->Client packet ActionFailed (if attacker is a L2PcInstance)</li>
	 * <li>If attack isn't aborted, send a message system (critical hit, missed...) to attacker/target if they are L2PcInstance </li>
	 * <li>If attack isn't aborted and hit isn't missed, reduce HP of the target and calculate reflection damage to reduce HP of attacker if necessary </li>
	 * <li>if attack isn't aborted and hit isn't missed, manage attack or cast break of the target (calculating rate, sending message...) </li><BR><BR>
	 *
	 * @param target The L2Character targeted
	 * @param damage Nb of HP to reduce
	 * @param crit True if hit is critical
	 * @param miss True if hit is missed
	 * @param soulshot True if SoulShot are charged
	 * @param shld True if shield is efficient
	 *
	 */
	@Override
	protected void onHitTimer(L2Character target, int damage, boolean crit, boolean miss, boolean soulshot, byte shld)
	{
		super.onHitTimer(target, damage, crit, miss, soulshot, shld);
	}
    
    public void queryGameGuard()
    {
        this.getClient().setGameGuardOk(false);
        this.sendPacket(new GameGuardQuery());
        if (Config.GAMEGUARD_ENFORCE)
        {
            ThreadPoolManager.getInstance().scheduleGeneral(new GameGuardCheck(), 30*1000);
        }
    }
    
    class GameGuardCheck implements Runnable
    {
        /**
         * @see java.lang.Runnable#run()
         */
        public void run()
        {
            L2GameClient client = L2PcInstance.this.getClient();
            if (client != null && !client.isAuthedGG() && L2PcInstance.this.isOnline() == 1)
            {
                GmListTable.broadcastMessageToGMs("Client "+client+" failed to reply GameGuard query and is being kicked!");
                _log.info("Client "+client+" failed to reply GameGuard query and is being kicked!");
                client.close(LeaveWorld.STATIC_PACKET);
            }
        }
    }

	/**
	 * Send a Server->Client packet StatusUpdate to the L2PcInstance.<BR><BR>
	 */
	@Override
	public void sendPacket(L2GameServerPacket packet)
	{
		if (_client != null)
		{
			_client.sendPacket(packet);
		}
	}
	
	/**
	 * Send SystemMessage packet.<BR><BR>
	 * @param id: SystemMessageId
	 */
	public void sendPacket(SystemMessageId id)
	{
		sendPacket(new SystemMessage(id));		
	}

	/**
	 * Manage Interact Task with another L2PcInstance.<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>If the private store is a STORE_PRIVATE_SELL, send a Server->Client PrivateBuyListSell packet to the L2PcInstance</li>
	 * <li>If the private store is a STORE_PRIVATE_BUY, send a Server->Client PrivateBuyListBuy packet to the L2PcInstance</li>
	 * <li>If the private store is a STORE_PRIVATE_MANUFACTURE, send a Server->Client RecipeShopSellList packet to the L2PcInstance</li><BR><BR>
	 *
	 * @param target The L2Character targeted
	 *
	 */
	public void doInteract(L2Character target)
	{
		if (target instanceof L2PcInstance)
		{
			L2PcInstance temp = (L2PcInstance) target;
			sendPacket(ActionFailed.STATIC_PACKET);

			if (temp.getPrivateStoreType() == STORE_PRIVATE_SELL || temp.getPrivateStoreType() == STORE_PRIVATE_PACKAGE_SELL)
				sendPacket(new PrivateStoreListSell(this, temp));
			else if (temp.getPrivateStoreType() == STORE_PRIVATE_BUY)
				sendPacket(new PrivateStoreListBuy(this, temp));
			else if (temp.getPrivateStoreType() == STORE_PRIVATE_MANUFACTURE)
				sendPacket(new RecipeShopSellList(this, temp));

		}
		else
		{
			// _interactTarget=null should never happen but one never knows ^^;
			if (target != null)
				target.onAction(this);
		}
	}

	/**
	 * Manage AutoLoot Task.<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Send a System Message to the L2PcInstance : YOU_PICKED_UP_S1_ADENA or YOU_PICKED_UP_S1_S2</li>
	 * <li>Add the Item to the L2PcInstance inventory</li>
	 * <li>Send a Server->Client packet InventoryUpdate to this L2PcInstance with NewItem (use a new slot) or ModifiedItem (increase amount)</li>
	 * <li>Send a Server->Client packet StatusUpdate to this L2PcInstance with current weight</li><BR><BR>
	 *
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : If a Party is in progress, distribute Items between party members</B></FONT><BR><BR>
	 *
	 * @param target The L2ItemInstance dropped
	 *
	 */
	public void doAutoLoot(L2Attackable target, L2Attackable.RewardItem item)
	{
		if (isInParty()&&ItemTable.getInstance().getTemplate(item.getItemId()).getItemType() != L2EtcItemType.HERB)  
			getParty().distributeItem(this, item, false, target);
		else if (item.getItemId() == 57) addAdena("Loot", item.getCount(), target, true);
		else addItem("Loot", item.getItemId(), item.getCount(), target, true);
	}


	/**
	 * Manage Pickup Task.<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Send a Server->Client packet StopMove to this L2PcInstance </li>
	 * <li>Remove the L2ItemInstance from the world and send server->client GetItem packets </li>
	 * <li>Send a System Message to the L2PcInstance : YOU_PICKED_UP_S1_ADENA or YOU_PICKED_UP_S1_S2</li>
	 * <li>Add the Item to the L2PcInstance inventory</li>
	 * <li>Send a Server->Client packet InventoryUpdate to this L2PcInstance with NewItem (use a new slot) or ModifiedItem (increase amount)</li>
	 * <li>Send a Server->Client packet StatusUpdate to this L2PcInstance with current weight</li><BR><BR>
	 *
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : If a Party is in progress, distribute Items between party members</B></FONT><BR><BR>
	 *
	 * @param object The L2ItemInstance to pick up
	 *
	 */
	protected void doPickupItem(L2Object object)
	{
		if (isAlikeDead() || isFakeDeath()) return;

		// Set the AI Intention to AI_INTENTION_IDLE
		getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);

		// Check if the L2Object to pick up is a L2ItemInstance
		if (! (object instanceof L2ItemInstance))
		{
			// dont try to pickup anything that is not an item :)
			_log.warning("trying to pickup wrong target."+getTarget());
			return;
		}

		L2ItemInstance target = (L2ItemInstance) object;

		// Send a Server->Client packet ActionFailed to this L2PcInstance
		sendPacket(ActionFailed.STATIC_PACKET);

		// Send a Server->Client packet StopMove to this L2PcInstance
		StopMove sm = new StopMove(getObjectId(), getX(), getY(), getZ(), getHeading());
		if (Config.DEBUG) _log.fine("pickup pos: "+ target.getX() + " "+target.getY()+ " "+target.getZ() );
		sendPacket(sm);

		synchronized (target)
		{
			// Check if the target to pick up is visible
			if (!target.isVisible())
			{
				// Send a Server->Client packet ActionFailed to this L2PcInstance
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}

			if ( ((isInParty() && getParty().getLootDistribution() == L2Party.ITEM_LOOTER) || !isInParty()) && !_inventory.validateCapacity(target))
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(new SystemMessage(SystemMessageId.SLOTS_FULL));
				return;
			}

            if (isInvul() && !isGM())
            {
                sendPacket(ActionFailed.STATIC_PACKET);
                SystemMessage smsg = new SystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1);
                smsg.addItemName(target);
                sendPacket(smsg);
                return;
            }

	        if (target.getOwnerId() != 0 && target.getOwnerId() != getObjectId() && !isInLooterParty(target.getOwnerId()))
            {
                sendPacket(ActionFailed.STATIC_PACKET);

                if (target.getItemId() == 57)
                {
                    SystemMessage smsg = new SystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1_ADENA);
                    smsg.addItemNumber(target.getCount());
                    sendPacket(smsg);
                }
                else if (target.getCount() > 1)
                {
                    SystemMessage smsg = new SystemMessage(SystemMessageId.FAILED_TO_PICKUP_S2_S1_S);
                    smsg.addItemName(target);
                    smsg.addItemNumber(target.getCount());
                    sendPacket(smsg);
                }
                else
                {
                    SystemMessage smsg = new SystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1);
                    smsg.addItemName(target);
                    sendPacket(smsg);
                }

                return;
            }
            
            // You can pickup only 1 combat flag
            if(FortSiegeManager.getInstance().isCombat(target.getItemId()) )
            {
                if (!FortSiegeManager.getInstance().checkIfCanPickup(this))
                    return ;
            }
            
	        if(target.getItemLootShedule() != null
	        		&& (target.getOwnerId() == getObjectId() || isInLooterParty(target.getOwnerId())))
	        	target.resetOwnerTimer();

			// Remove the L2ItemInstance from the world and send server->client GetItem packets
			target.pickupMe(this);
			 if(Config.SAVE_DROPPED_ITEM) // item must be removed from ItemsOnGroundManager if is active
				 ItemsOnGroundManager.getInstance().removeObject(target);

		}

        //Auto use herbs - pick up
		if (target.getItemType() == L2EtcItemType.HERB)
        {
			IItemHandler handler = ItemHandler.getInstance().getItemHandler(target.getEtcItem());
			if (handler == null)
				_log.fine("No item handler registered for item ID " + target.getItemId() + ".");
			else
				handler.useItem(this, target);
            	ItemTable.getInstance().destroyItem("Consume", target, this, null);
        }
		// Cursed Weapons are not distributed
		else if(CursedWeaponsManager.getInstance().isCursed(target.getItemId()))
		{
			addItem("Pickup", target, null, true);
		}
		else if(FortSiegeManager.getInstance().isCombat(target.getItemId()) )
        {
            addItem("Pickup", target, null, true);
        }
		else
		{
			// if item is instance of L2ArmorType or L2WeaponType broadcast an "Attention" system message
			if(target.getItemType() instanceof L2ArmorType || target.getItemType() instanceof L2WeaponType)
			{
				if (target.getEnchantLevel() > 0)
				{
					SystemMessage msg = new SystemMessage(SystemMessageId.ANNOUNCEMENT_C1_PICKED_UP_S2_S3);
					msg.addPcName(this);
					msg.addNumber(target.getEnchantLevel());
					msg.addItemName(target.getItemId());
					broadcastPacket(msg, 1400);
				}
				else
				{
					SystemMessage msg = new SystemMessage(SystemMessageId.ANNOUNCEMENT_C1_PICKED_UP_S2);
					msg.addPcName(this);
					msg.addItemName(target.getItemId());
					broadcastPacket(msg, 1400);
				}
			}

			// Check if a Party is in progress
			if (isInParty()) getParty().distributeItem(this, target);
			// Target is adena
			else if (target.getItemId() == 57 && getInventory().getAdenaInstance() != null)
			{
				addAdena("Pickup", target.getCount(), null, true);
				ItemTable.getInstance().destroyItem("Pickup", target, this, null);
			}
			// Target is regular item
			else addItem("Pickup", target, null, true);
		}
	}
	
	public boolean canOpenPrivateStore()
	{
		return !isAlikeDead() && !isInOlympiadMode() && !isMounted() 
				&& !isInsideZone(ZONE_NOSTORE) && !isCastingNow();
	}
	
	public void tryOpenPrivateBuyStore()
	{
		// Player shouldn't be able to set stores if he/she is alike dead (dead or fake death)
		if (canOpenPrivateStore())
		{
			if (getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_BUY || getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_BUY + 1)
			{
				setPrivateStoreType(L2PcInstance.STORE_PRIVATE_NONE);
			}
			if (getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_NONE)
			{
				if (isSitting())
				{
					standUp();
				}
				setPrivateStoreType(L2PcInstance.STORE_PRIVATE_BUY + 1);
				sendPacket(new PrivateStoreManageListBuy(this));
			}
		}
		else
		{
			if (isInsideZone(ZONE_NOSTORE))
				sendPacket(new SystemMessage(SystemMessageId.NO_PRIVATE_STORE_HERE));
			sendPacket(ActionFailed.STATIC_PACKET);
		}
	}
	
	public void tryOpenPrivateSellStore(boolean isPackageSale)
	{
		// Player shouldn't be able to set stores if he/she is alike dead (dead or fake death)
		if (canOpenPrivateStore())
		{
			if (getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_SELL || getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_SELL + 1 || getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_PACKAGE_SELL)
			{
				this.setPrivateStoreType(L2PcInstance.STORE_PRIVATE_NONE);
			}
			
			if (getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_NONE)
			{
				if (isSitting())
				{
					standUp();
				}
				setPrivateStoreType(L2PcInstance.STORE_PRIVATE_SELL + 1);
				sendPacket(new PrivateStoreManageListSell(this, isPackageSale));
			}
		}
		else
		{
			if (isInsideZone(ZONE_NOSTORE))
				sendPacket(new SystemMessage(SystemMessageId.NO_PRIVATE_STORE_HERE));
			sendPacket(ActionFailed.STATIC_PACKET);
		}
	}
    
    @Override
    public boolean isTransformed()
    {
        return _transformation != null && !_transformation.isStance();
    }

    public boolean isInStance()
    {
    	return _transformation != null && _transformation.isStance();
    }

    public void transform(L2Transformation transformation)
    {
        if (_transformation != null)
        {
            // You already polymorphed and cannot polymorph again.
            SystemMessage msg = new SystemMessage(SystemMessageId.YOU_ALREADY_POLYMORPHED_AND_CANNOT_POLYMORPH_AGAIN);
            sendPacket(msg);
            return;
        }
        setQueuedSkill(null, false, false);
        if(isMounted())
        {
        	// Get off the strider or something else if character is mounted
        	dismount();
        }
        
        _transformation = transformation;
        stopAllToggles();
        transformation.onTransform();
        sendSkillList();
        sendPacket(new SkillCoolTime(this));
        broadcastUserInfo();
    }
    
    @Override
    public void untransform()
    {
        if (_transformation != null)
        {
        	setTransformAllowedSkills(new int[]{});
            _transformation.onUntransform();
            _transformation = null;
            stopEffects(L2EffectType.TRANSFORMATION);
            broadcastUserInfo();
            sendSkillList();
            sendPacket(new SkillCoolTime(this));
        }
    }
    
    public L2Transformation getTransformation()
    {
        return _transformation;
    }
    
    /**
     * This returns the transformation Id of the current transformation.
     * For example, if a player is transformed as a Buffalo, and then picks up the Zariche,
     * the transform Id returned will be that of the Zariche, and NOT the Buffalo.
     * @return Transformation Id
     */
    public int getTransformationId()
    {
        return (_transformation == null ? 0 : _transformation.getId());
    }
    
    /**
     * This returns the transformation Id stored inside the character table, selected by the method: transformSelectInfo()
     * For example, if a player is transformed as a Buffalo, and then picks up the Zariche,
     * the transform Id returned will be that of the Buffalo, and NOT the Zariche.
     * @return Transformation Id
     */
    public int transformId()
    {
       return _transformationId;
    }
    
    /**
     * This is a simple query that inserts the transform Id into the character table for future reference.
     */
    public void transformInsertInfo()
    {
    	_transformationId = getTransformationId();
    	
    	if (_transformationId == L2Transformation.TRANSFORM_AKAMANAH
    			|| _transformationId == L2Transformation.TRANSFORM_ZARICHE)
    		return;
    	
    	Connection con = null;
        try
        {
            con = L2DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement = con.prepareStatement(UPDATE_CHAR_TRANSFORM);
            
            statement.setInt(1, _transformationId);
            statement.setInt(2, getObjectId());
            
            statement.execute();
            statement.close();
        }
        catch (Exception e)
        {
        	_log.log(Level.SEVERE, "Transformation insert info: ",e);
        }
        finally
        {
            try { con.close(); } catch (Exception e) {}
        }
    }
    
    /**
     * This selects the current 
     * @return transformation Id
     */
    public int transformSelectInfo()
    {
    	Connection con = null;
        try
        {
            con = L2DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement = con.prepareStatement(SELECT_CHAR_TRANSFORM); 
            
            statement.setInt(1, getObjectId());
            ResultSet rset = statement.executeQuery();
            rset.next();
            _transformationId = rset.getInt("transform_id");
            
            rset.close();
            statement.close();
        }
        catch (Exception e)
        {
        	_log.log(Level.SEVERE, "Transformation select info: ",e);
        }
        finally
        {
            try { con.close(); } catch (Exception e) {}
        }
        return _transformationId;
    }

	/**
	 * Set a target.<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Remove the L2PcInstance from the _statusListener of the old target if it was a L2Character </li>
	 * <li>Add the L2PcInstance to the _statusListener of the new target if it's a L2Character </li>
	 * <li>Target the new L2Object (add the target to the L2PcInstance _target, _knownObject and L2PcInstance to _KnownObject of the L2Object)</li><BR><BR>
	 *
	 * @param newTarget The L2Object to target
	 *
	 */
	@Override
	public void setTarget(L2Object newTarget)
	{
		
		if(newTarget!=null)
		{
			boolean isParty=(( (newTarget instanceof L2PcInstance) && isInParty() && getParty().getPartyMembers().contains(newTarget)));
		
			// Check if the new target is visible
			if (!isParty && !newTarget.isVisible())
				newTarget = null;

			// Prevents /target exploiting
			if (newTarget != null && !isParty && Math.abs(newTarget.getZ() - getZ()) > 1000)
				newTarget = null;
		}
		if(!isGM())
		{
			// Can't target and attack festival monsters if not participant
			if((newTarget instanceof L2FestivalMonsterInstance) && !isFestivalParticipant())
				newTarget = null;

			// Can't target and attack rift invaders if not in the same room
			else if(isInParty() && getParty().isInDimensionalRift())
			{
				byte riftType = getParty().getDimensionalRift().getType();
				byte riftRoom = getParty().getDimensionalRift().getCurrentRoom();

				if (newTarget != null && !DimensionalRiftManager.getInstance().getRoom(riftType, riftRoom).checkIfInZone(newTarget.getX(), newTarget.getY(), newTarget.getZ()))
					newTarget = null;
			}
		}

		// Get the current target
		L2Object oldTarget = getTarget();

		if (oldTarget != null)
		{
			if (oldTarget.equals(newTarget))
				return; // no target change

			// Remove the L2PcInstance from the _statusListener of the old target if it was a L2Character
			if (oldTarget instanceof L2Character)
				((L2Character) oldTarget).removeStatusListener(this);
		}

		// Add the L2PcInstance to the _statusListener of the new target if it's a L2Character
		if (newTarget instanceof L2Character)
		{
			((L2Character) newTarget).addStatusListener(this);
			TargetSelected my = new TargetSelected(getObjectId(), newTarget.getObjectId(), getX(), getY(), getZ());
			broadcastPacket(my);
		}
		if (newTarget == null && getTarget() != null)
		{
			broadcastPacket(new TargetUnselected(this));
		}
		
		// Target the new L2Object (add the target to the L2PcInstance _target, _knownObject and L2PcInstance to _KnownObject of the L2Object)
		super.setTarget(newTarget);
	}

	/**
	 * Return the active weapon instance (always equiped in the right hand).<BR><BR>
	 */
	@Override
	public L2ItemInstance getActiveWeaponInstance()
	{
		return getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
	}

	/**
	 * Return the active weapon item (always equiped in the right hand).<BR><BR>
	 */
	@Override
	public L2Weapon getActiveWeaponItem()
	{
		L2ItemInstance weapon = getActiveWeaponInstance();

		if (weapon == null)
			return getFistsWeaponItem();

		return (L2Weapon) weapon.getItem();
	}

	public L2ItemInstance getChestArmorInstance()
	{
		return getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST);
	}

    public L2ItemInstance getLegsArmorInstance()
    {
        return getInventory().getPaperdollItem(Inventory.PAPERDOLL_LEGS);
    }

	public L2Armor getActiveChestArmorItem()
	{
		L2ItemInstance armor = getChestArmorInstance();

		if (armor == null)
			return null;

		return (L2Armor) armor.getItem();
	}

    public L2Armor getActiveLegsArmorItem()
    {
        L2ItemInstance legs = getLegsArmorInstance();

        if (legs == null)
            return null;

        return (L2Armor) legs.getItem();
    }

	public boolean isWearingHeavyArmor()
	{
        L2ItemInstance legs = getLegsArmorInstance();
        L2ItemInstance armor = getChestArmorInstance();
        
	    if (armor != null && legs != null)
        {
            if ((L2ArmorType)legs.getItemType() == L2ArmorType.HEAVY 
                    && ((L2ArmorType)armor.getItemType() == L2ArmorType.HEAVY))
                return true;
        }
	    if (armor != null)
	    {
	        if ((getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST).getItem().getBodyPart() == L2Item.SLOT_FULL_ARMOR 
	                && (L2ArmorType)armor.getItemType() == L2ArmorType.HEAVY))
	            return true;
	    }
		return false;
	}

	public boolean isWearingLightArmor()
	{
        L2ItemInstance legs = getLegsArmorInstance();
        L2ItemInstance armor = getChestArmorInstance();

	    if (armor != null && legs != null)
        {
            if ((L2ArmorType)legs.getItemType() == L2ArmorType.LIGHT 
                    && ((L2ArmorType)armor.getItemType() == L2ArmorType.LIGHT))
                return true;
        }
        if (armor != null)
        {
            if ((getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST).getItem().getBodyPart() == L2Item.SLOT_FULL_ARMOR 
                    && (L2ArmorType)armor.getItemType() == L2ArmorType.LIGHT))
                return true;
        }
		return false;
	}

	public boolean isWearingMagicArmor()
	{
        L2ItemInstance legs = getLegsArmorInstance();
        L2ItemInstance armor = getChestArmorInstance();

	    if (armor != null && legs != null)
        {
            if ((L2ArmorType)legs.getItemType() == L2ArmorType.MAGIC 
                    && ((L2ArmorType)armor.getItemType() == L2ArmorType.MAGIC))
                return true;
        }
        if (armor != null)
        {
            if ((getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST).getItem().getBodyPart() == L2Item.SLOT_FULL_ARMOR 
                    && (L2ArmorType)armor.getItemType() == L2ArmorType.MAGIC))
                return true;
        }
		return false;
	}

	public boolean isWearingFormalWear()
    {
        return _IsWearingFormalWear;
    }

    public void setIsWearingFormalWear(boolean value)
    {
        _IsWearingFormalWear = value;
    }
    public boolean isMarried()
    {
        return _married;
    }

    public void setMarried(boolean state)
    {
        _married = state;
    }

    public boolean isEngageRequest()
    {
        return _engagerequest;
    }

    public void setEngageRequest(boolean state,int playerid)
    {
        _engagerequest = state;
        _engageid = playerid;
    }

    public void setMaryRequest(boolean state)
    {
        _marryrequest = state;
    }

    public boolean isMaryRequest()
    {
        return _marryrequest;
    }

    public void setMarryAccepted(boolean state)
    {
        _marryaccepted = state;
    }

    public boolean isMarryAccepted()
    {
        return _marryaccepted;
    }

    public int getEngageId()
    {
        return _engageid;
    }

    public int getPartnerId()
    {
        return _partnerId;
    }

    public void setPartnerId(int partnerid)
    {
        _partnerId = partnerid;
    }

    public int getCoupleId()
    {
        return _coupleId;
    }

    public void setCoupleId(int coupleId)
    {
        _coupleId = coupleId;
    }

    public void engageAnswer(int answer)
    {
        if(_engagerequest==false)
            return;
        else if(_engageid==0)
            return;
        else
        {
            L2PcInstance ptarget = (L2PcInstance)L2World.getInstance().findObject(_engageid);
            setEngageRequest(false,0);
            if(ptarget!=null)
            {
                if (answer == 1)
                {
                    CoupleManager.getInstance().createCouple(ptarget, L2PcInstance.this);
                    ptarget.sendMessage("Request to Engage has been >ACCEPTED<");
                }
                else
                    ptarget.sendMessage("Request to Engage has been >DENIED<!");
            }
        }
    }

	/**
	 * Return the secondary weapon instance (always equiped in the left hand).<BR><BR>
	 */
	@Override
	public L2ItemInstance getSecondaryWeaponInstance()
	{
		return getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
	}

	/**
	 * Return the secondary weapon item (always equiped in the left hand) or the fists weapon.<BR><BR>
	 */
	@Override
	public L2Weapon getSecondaryWeaponItem()
	{
		L2ItemInstance weapon = getSecondaryWeaponInstance();

		if (weapon == null)
			return getFistsWeaponItem();

		L2Item item = weapon.getItem();

		if (item instanceof L2Weapon)
			return (L2Weapon) item;

		return null;
	}


	/**
	 *
     * Kill the L2Character, Apply Death Penalty, Manage gain/loss Karma and Item Drop.<BR><BR>
     *
     * <B><U> Actions</U> :</B><BR><BR>
     * <li>Reduce the Experience of the L2PcInstance in function of the calculated Death Penalty </li>
     * <li>If necessary, unsummon the Pet of the killed L2PcInstance </li>
     * <li>Manage Karma gain for attacker and Karam loss for the killed L2PcInstance </li>
     * <li>If the killed L2PcInstance has Karma, manage Drop Item</li>
     * <li>Kill the L2PcInstance </li><BR><BR>
     *
     *
     * @param i The HP decrease value
     * @param attacker The L2Character who attacks
     *
     *
	 * @see com.l2jserver.gameserver.model.actor.L2Playable#doDie(com.l2jserver.gameserver.model.actor.L2Character)
	 */
	@Override
	public boolean doDie(L2Character killer)
	{
		// Kill the L2PcInstance
		if (!super.doDie(killer))
			return false;
		
		if (isMounted())
			stopFeed();
		synchronized (this)
		{
			if (isFakeDeath())
				stopFakeDeath(null);
		}
		
		if (killer != null)
		{
			L2PcInstance pk = killer.getActingPlayer();
			
			TvTEvent.onKill(killer, this);
			
			if (atEvent && pk != null)
			{
				pk.kills.add(getName());
			}
			
			//announce pvp/pk
			if (Config.ANNOUNCE_PK_PVP && pk != null && !pk.isGM())
			{
				String msg = "";
				if (getPvpFlag() == 0)
				{
					msg = Config.ANNOUNCE_PK_MSG.replace("$killer", pk.getName()).replace("$target", getName());
					if (Config.ANNOUNCE_PK_PVP_NORMAL_MESSAGE)
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.S1);
						sm.addString(msg);
						Announcements.getInstance().announceToAll(sm);
					}
					else	
						Announcements.getInstance().announceToAll(msg);
				}
				else if (getPvpFlag() != 0)
				{
					msg = Config.ANNOUNCE_PVP_MSG.replace("$killer", pk.getName()).replace("$target", getName());
					if (Config.ANNOUNCE_PK_PVP_NORMAL_MESSAGE)
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.S1);
						sm.addString(msg);
						Announcements.getInstance().announceToAll(sm);
					}
					else
						Announcements.getInstance().announceToAll(msg);
				}
			}
			
			// Clear resurrect xp calculation
			setExpBeforeDeath(0);
			
			// Issues drop of Cursed Weapon.
			if (isCursedWeaponEquipped())
			{
				CursedWeaponsManager.getInstance().drop(_cursedWeaponEquippedId, killer);
			}
			else if (isCombatFlagEquipped())
			{
				if (TerritoryWarManager.getInstance().isTWInProgress())
					TerritoryWarManager.getInstance().dropCombatFlag(this, true);
				else
					FortSiegeManager.getInstance().dropCombatFlag(this);
			}
			else
			{
				if (pk == null || !pk.isCursedWeaponEquipped())
				{
					onDieDropItem(killer); // Check if any item should be dropped
					
					if (!(isInsideZone(ZONE_PVP) && !isInsideZone(ZONE_SIEGE)))
					{
						if (pk != null && pk.getClan() != null && getClan() != null && !isAcademyMember() && !(pk.isAcademyMember()) )
						{
							if ((_clan.isAtWarWith(pk.getClanId()) && pk.getClan().isAtWarWith(_clan.getClanId()))
									|| (isInSiege() && pk.isInSiege()))
							{
								if (AntiFeedManager.getInstance().check(killer, this))
								{
									// 	when your reputation score is 0 or below, the other clan cannot acquire any reputation points
									if (getClan().getReputationScore() > 0)
										pk.getClan().addReputationScore(Config.REPUTATION_SCORE_PER_KILL, false);
									// 	when the opposing sides reputation score is 0 or below, your clans reputation score does not decrease
									if (pk.getClan().getReputationScore() > 0)
										_clan.takeReputationScore(Config.REPUTATION_SCORE_PER_KILL, false);
								}
							}
						}
					}
					if (Config.ALT_GAME_DELEVEL)
					{
						// Reduce the Experience of the L2PcInstance in function of the calculated Death Penalty
						// NOTE: deathPenalty +- Exp will update karma
						// Penalty is lower if the player is at war with the pk (war has to be declared)
						if (getSkillLevel(L2Skill.SKILL_LUCKY) < 0 || getStat().getLevel() > 9)
						{
							boolean siege_npc = false;
							if (killer instanceof L2DefenderInstance || killer instanceof L2FortCommanderInstance)
								siege_npc = true;
							deathPenalty(pk != null && getClan() != null && getClan().isAtWarWith(pk.getClanId()), pk != null, siege_npc);
						}
						
					}
					else
					{
						if (!(isInsideZone(ZONE_PVP) && !isInSiege()) || pk == null)
							onDieUpdateKarma(); // Update karma if delevel is not allowed
					}
				}
			}
		}
		
		setPvpFlag(0); // Clear the pvp flag
		
		// Unsummon Cubics
		if (!_cubics.isEmpty())
		{
			for (L2CubicInstance cubic : _cubics.values())
			{
				cubic.stopAction();
				cubic.cancelDisappear();
			}
			
			_cubics.clear();
		}
		
		if (_fusionSkill != null)
			abortCast();
		
		for (L2Character character : getKnownList().getKnownCharacters())
			if (character.getFusionSkill() != null && character.getFusionSkill().getTarget() == this)
				character.abortCast();
		
		if (isInParty() && getParty().isInDimensionalRift())
			getParty().getDimensionalRift().getDeadMemberList().add(this);
		
		if (getAgathionId() != 0)
			setAgathionId(0);
		
		// calculate death penalty buff
		calculateDeathPenaltyBuffLevel(killer);
		
		stopRentPet();
		stopWaterTask();
		
		AntiFeedManager.getInstance().setLastDeathTime(getObjectId());

		if (isPhoenixBlessed())
			reviveRequest(this, null, false);
		else if (getCharmOfCourage() && this.isInSiege())
		{
			reviveRequest(this, null, false);
		}
		return true;
	}

	private void onDieDropItem(L2Character killer)
	{
		if (atEvent || killer == null)
            return;

		L2PcInstance pk = killer.getActingPlayer();
		if (getKarma() <= 0 && pk != null
                && pk.getClan() != null && getClan() != null
                && (
                		pk.getClan().isAtWarWith(getClanId())
                        // || this.getClan().isAtWarWith(((L2PcInstance)killer).getClanId())
                   )
           )
			return;

		if ((!isInsideZone(ZONE_PVP) || pk == null) && (!isGM() || Config.KARMA_DROP_GM))
		{
			boolean isKarmaDrop = false;
			boolean isKillerNpc = (killer instanceof L2Npc);
			int pkLimit = Config.KARMA_PK_LIMIT;

			int dropEquip           = 0;
			int dropEquipWeapon     = 0;
			int dropItem            = 0;
			int dropLimit           = 0;
			int dropPercent         = 0;

			if (getKarma() > 0 && getPkKills() >= pkLimit)
			{
				isKarmaDrop = true;
				dropPercent = Config.KARMA_RATE_DROP;
				dropEquip = Config.KARMA_RATE_DROP_EQUIP;
				dropEquipWeapon = Config.KARMA_RATE_DROP_EQUIP_WEAPON;
				dropItem = Config.KARMA_RATE_DROP_ITEM;
				dropLimit = Config.KARMA_DROP_LIMIT;
			}
			else if (isKillerNpc && getLevel() > 4 && !isFestivalParticipant())
			{
				dropPercent = Config.PLAYER_RATE_DROP;
				dropEquip = Config.PLAYER_RATE_DROP_EQUIP;
				dropEquipWeapon = Config.PLAYER_RATE_DROP_EQUIP_WEAPON;
				dropItem = Config.PLAYER_RATE_DROP_ITEM;
				dropLimit = Config.PLAYER_DROP_LIMIT;
			}

			if (dropPercent > 0 && Rnd.get(100) < dropPercent)
			{
				int dropCount = 0;

				int itemDropPercent = 0;

				for (L2ItemInstance itemDrop : getInventory().getItems())
				{
					// Don't drop
					if (
							itemDrop.isShadowItem() || // Dont drop Shadow Items
							itemDrop.isTimeLimitedItem() || // Dont drop Time Limited Items
							!itemDrop.isDropable() ||
							itemDrop.getItemId() == 57 || // Adena
							itemDrop.getItem().getType2() == L2Item.TYPE2_QUEST ||                  // Quest Items
							getPet() != null && getPet().getControlItemId() == itemDrop.getItemId() || // Control Item of active pet
							Arrays.binarySearch(Config.KARMA_LIST_NONDROPPABLE_ITEMS, itemDrop.getItemId()) >= 0 || // Item listed in the non droppable item list
							Arrays.binarySearch(Config.KARMA_LIST_NONDROPPABLE_PET_ITEMS, itemDrop.getItemId()) >= 0 // Item listed in the non droppable pet item list
					) continue;

					if (itemDrop.isEquipped())
					{
						// Set proper chance according to Item type of equipped Item
						itemDropPercent = itemDrop.getItem().getType2() == L2Item.TYPE2_WEAPON ? dropEquipWeapon : dropEquip;
						getInventory().unEquipItemInSlotAndRecord(itemDrop.getLocationSlot());
					}
					else itemDropPercent = dropItem; // Item in inventory

					// NOTE: Each time an item is dropped, the chance of another item being dropped gets lesser (dropCount * 2)
					if (Rnd.get(100) < itemDropPercent)
					{
						dropItem("DieDrop", itemDrop, killer, true);

						if (isKarmaDrop)
							_log.warning(getName() + " has karma and dropped id = " + itemDrop.getItemId() + ", count = " + itemDrop.getCount());
						else
							_log.warning(getName() + " dropped id = " + itemDrop.getItemId() + ", count = " + itemDrop.getCount());

						if (++dropCount >= dropLimit)
							break;
					}
				}
			}
		}
	}

	private void onDieUpdateKarma()
	{
		// Karma lose for server that does not allow delevel
		if ( getKarma() > 0 )
		{
			// this formula seems to work relatively well:
			// baseKarma * thisLVL * (thisLVL/100)
			// Calculate the new Karma of the attacker : newKarma = baseKarma*pkCountMulti*lvlDiffMulti
			double karmaLost = Config.KARMA_LOST_BASE;
			karmaLost *= getLevel(); // multiply by char lvl
			karmaLost *= (getLevel() / 100.0); // divide by 0.charLVL
			karmaLost = Math.round(karmaLost);
			if ( karmaLost < 0 ) karmaLost = 1;

			// Decrease Karma of the L2PcInstance and Send it a Server->Client StatusUpdate packet with Karma and PvP Flag if necessary
			setKarma(getKarma() - (int)karmaLost);
		}
	}

	public void onKillUpdatePvPKarma(L2Character target)
	{
		if (target == null) return;
		if (!(target instanceof L2Playable)) return;

		L2PcInstance targetPlayer = target.getActingPlayer();

		if (targetPlayer == null) return;                                          // Target player is null
		if (targetPlayer == this) return;                                          // Target player is self

		if (isCursedWeaponEquipped())
		{
			CursedWeaponsManager.getInstance().increaseKills(_cursedWeaponEquippedId);
			// Custom message for time left
			// CursedWeapon cw = CursedWeaponsManager.getInstance().getCursedWeapon(_cursedWeaponEquipedId);
			// SystemMessage msg = new SystemMessage(SystemMessageId.THERE_IS_S1_HOUR_AND_S2_MINUTE_LEFT_OF_THE_FIXED_USAGE_TIME);
			// int timeLeftInHours = (int)(((cw.getTimeLeft()/60000)/60));
			// msg.addItemName(_cursedWeaponEquipedId);
			// msg.addNumber(timeLeftInHours);
			// sendPacket(msg);
			return;
		}

		// If in duel and you kill (only can kill l2summon), do nothing
		if (isInDuel() && targetPlayer.isInDuel()) return;

        // If in Arena, do nothing
		if (isInsideZone(ZONE_PVP) || targetPlayer.isInsideZone(ZONE_PVP))
			return;

        // Check if it's pvp
		if (
				(
						checkIfPvP(target) &&            //   Can pvp and
						targetPlayer.getPvpFlag() != 0   //   Target player has pvp flag set
				) ||                                     // or
				(
						isInsideZone(ZONE_PVP) &&         		 //   Player is inside pvp zone and
						targetPlayer.isInsideZone(ZONE_PVP) 	 //   Target player is inside pvp zone
				)
		)
			increasePvpKills(target);
		else
		// Target player doesn't have pvp flag set
		{
			// check about wars
			if (targetPlayer.getClan() != null && getClan() != null
					&& getClan().isAtWarWith(targetPlayer.getClanId())
					&& targetPlayer.getClan().isAtWarWith(getClanId())
					&& targetPlayer.getPledgeType() != L2Clan.SUBUNIT_ACADEMY
					&& getPledgeType() != L2Clan.SUBUNIT_ACADEMY)
			{
				// 'Both way war' -> 'PvP Kill'
				increasePvpKills(target);
				return;
			}

            // 'No war' or 'One way war' -> 'Normal PK'
			if (targetPlayer.getKarma() > 0)                                        // Target player has karma
			{
				if (Config.KARMA_AWARD_PK_KILL)
                    increasePvpKills(target);
			}
			else if (targetPlayer.getPvpFlag() == 0)                                                                    // Target player doesn't have karma
			{
				increasePkKillsAndKarma(target);
				//Unequip adventurer items
				checkItemRestriction();
			}
		}
	}

	/**
	 * Increase the pvp kills count and send the info to the player
	 *
	 */
	public void increasePvpKills(L2Character target)
	{
		if (target instanceof L2PcInstance
				&& AntiFeedManager.getInstance().check(this, target))
		{
			// Add karma to attacker and increase its PK counter
			setPvpKills(getPvpKills() + 1);

			// Send a Server->Client UserInfo packet to attacker with its Karma and PK Counter
			sendPacket(new UserInfo(this));
			sendPacket(new ExBrExtraUserInfo(this));
		}
	}

	/**
	 * Increase pk count, karma and send the info to the player
	 *
	 * @param targLVL : level of the killed player
	 * @param increasePk : true if PK counter should be increased too
	 */
	public void increasePkKillsAndKarma(L2Character target)
	{
		int baseKarma           = Config.KARMA_MIN_KARMA;
		int newKarma            = baseKarma;
		int karmaLimit          = Config.KARMA_MAX_KARMA;

		int pkLVL               = getLevel();
		int pkPKCount           = getPkKills();

		int targLVL             = target.getLevel();

		int lvlDiffMulti = 0;
		int pkCountMulti = 0;

		// Check if the attacker has a PK counter greater than 0
		if (pkPKCount > 0)
			pkCountMulti = pkPKCount / 2;
		else
			pkCountMulti = 1;

		if (pkCountMulti < 1)
			pkCountMulti = 1;

		// Calculate the level difference Multiplier between attacker and killed L2PcInstance
		if (pkLVL > targLVL)
			lvlDiffMulti = pkLVL / targLVL;
		else
			lvlDiffMulti = 1;

		if (lvlDiffMulti < 1)
			lvlDiffMulti = 1;

		// Calculate the new Karma of the attacker : newKarma = baseKarma*pkCountMulti*lvlDiffMulti
		newKarma *= pkCountMulti;
		newKarma *= lvlDiffMulti;

		// Make sure newKarma is less than karmaLimit and higher than baseKarma
		if (newKarma < baseKarma)
			newKarma = baseKarma;
		if (newKarma > karmaLimit)
			newKarma = karmaLimit;

		// Fix to prevent overflow (=> karma has a  max value of 2 147 483 647)
		if (getKarma() > (Integer.MAX_VALUE - newKarma))
			newKarma = Integer.MAX_VALUE - getKarma();

		// Add karma to attacker and increase its PK counter
		setKarma(getKarma() + newKarma);
		if (target instanceof L2PcInstance
				&& AntiFeedManager.getInstance().check(this, target))
			setPkKills(getPkKills() + 1);

		// Send a Server->Client UserInfo packet to attacker with its Karma and PK Counter
		sendPacket(new UserInfo(this));
		sendPacket(new ExBrExtraUserInfo(this));
	}

	public int calculateKarmaLost(long exp)
	{
		// KARMA LOSS
		// When a PKer gets killed by another player or a L2MonsterInstance, it loses a certain amount of Karma based on their level.
		// this (with defaults) results in a level 1 losing about ~2 karma per death, and a lvl 70 loses about 11760 karma per death...
		// You lose karma as long as you were not in a pvp zone and you did not kill urself.
		// NOTE: exp for death (if delevel is allowed) is based on the players level

		long expGained = Math.abs(exp);
		expGained /= Config.KARMA_XP_DIVIDER;

		// FIXME Micht : Maybe this code should be fixed and karma set to a long value
		int karmaLost = 0;
		if (expGained > Integer.MAX_VALUE)
			karmaLost = Integer.MAX_VALUE;
		else
			karmaLost = (int)expGained;

		if (karmaLost < Config.KARMA_LOST_BASE) karmaLost = Config.KARMA_LOST_BASE;
		if (karmaLost > getKarma()) karmaLost = getKarma();

		return karmaLost;
	}

	public void updatePvPStatus()
	{
		if (isInsideZone(ZONE_PVP)) return;
		setPvpFlagLasts(System.currentTimeMillis() + Config.PVP_NORMAL_TIME);

		if (getPvpFlag() == 0)
			startPvPFlag();
	}

	public void updatePvPStatus(L2Character target)
	{
		L2PcInstance player_target = target.getActingPlayer();

        if (player_target == null)
           return;

        if ((isInDuel() && player_target.getDuelId() == getDuelId())) return;
        if ((!isInsideZone(ZONE_PVP) || !player_target.isInsideZone(ZONE_PVP)) && player_target.getKarma() == 0)
        {
        	if (checkIfPvP(player_target))
        		setPvpFlagLasts(System.currentTimeMillis() + Config.PVP_PVP_TIME);
        	else
        		setPvpFlagLasts(System.currentTimeMillis() + Config.PVP_NORMAL_TIME);
			if (getPvpFlag() == 0)
				startPvPFlag();
        }
	}

	/**
	 * Restore the specified % of experience this L2PcInstance has
	 * lost and sends a Server->Client StatusUpdate packet.<BR><BR>
	 */
	public void restoreExp(double restorePercent)
	{
		if (getExpBeforeDeath() > 0)
		{
			// Restore the specified % of lost experience.
			getStat().addExp(Math.round((getExpBeforeDeath() - getExp()) * restorePercent / 100));
			setExpBeforeDeath(0);
		}
	}

	/**
	 * Reduce the Experience (and level if necessary) of the L2PcInstance in function of the calculated Death Penalty.<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Calculate the Experience loss </li>
	 * <li>Set the value of _expBeforeDeath </li>
	 * <li>Set the new Experience value of the L2PcInstance and Decrease its level if necessary </li>
	 * <li>Send a Server->Client StatusUpdate packet with its new Experience </li><BR><BR>
	 *
	 */
	public void deathPenalty(boolean atwar, boolean killed_by_pc, boolean killed_by_siege_npc)
	{
		// TODO Need Correct Penalty
		// Get the level of the L2PcInstance
		final int lvl = getLevel();

		int clan_luck = getSkillLevel(L2Skill.SKILL_CLAN_LUCK);
		
		double clan_luck_modificator = 1.0;	
		
		if (!killed_by_pc)
		{
			switch (clan_luck)
			{
				case 3:
					clan_luck_modificator = 0.8;
					break;
				case 2:
					clan_luck_modificator = 0.8;
					break;
				case 1:
					clan_luck_modificator = 0.88;
					break;
				default:
					clan_luck_modificator = 1.0;
					break;				
			}
		}
		else
		{
			switch (clan_luck)
			{
				case 3:
					clan_luck_modificator = 0.5;
					break;
				case 2:
					clan_luck_modificator = 0.5;
					break;
				case 1:
					clan_luck_modificator = 0.5;
					break;
				default:
					clan_luck_modificator = 1.0;
					break;				
			}
		}	
		
		//The death steal you some Exp
		double percentLost = Config.PLAYER_XP_PERCENT_LOST[getLevel()]*clan_luck_modificator;

		if (getKarma() > 0)
            percentLost *= Config.RATE_KARMA_EXP_LOST;

		if (isFestivalParticipant() || atwar)
            percentLost /= 4.0;

		// Calculate the Experience loss
		long lostExp = 0;
		if (!atEvent)
			if (lvl < Experience.MAX_LEVEL)
				lostExp = Math.round((getStat().getExpForLevel(lvl+1) - getStat().getExpForLevel(lvl)) * percentLost /100);
			else
				lostExp = Math.round((getStat().getExpForLevel(Experience.MAX_LEVEL) - getStat().getExpForLevel(Experience.MAX_LEVEL - 1)) * percentLost /100);

		// Get the Experience before applying penalty
		setExpBeforeDeath(getExp());

		// No xp loss inside pvp zone unless
		// - it's a siege zone and you're NOT participating
		// - you're killed by a non-pc whose not belong to the siege
		if (isInsideZone(ZONE_PVP))
		{
			// No xp loss for siege participants inside siege zone
			if (isInsideZone(ZONE_SIEGE))
			{
				if (isInSiege() && (killed_by_pc || killed_by_siege_npc))
					lostExp = 0;
			}
			else if (killed_by_pc)
				lostExp = 0;
		}

        if (Config.DEBUG)
            _log.fine(getName() + " died and lost " + lostExp + " experience.");

		// Set the new Experience value of the L2PcInstance
		getStat().addExp(-lostExp);
	}

	/**
	 * @param b
	 */
	public void setPartyMatchingAutomaticRegistration(boolean b)
	{
		_partyMatchingAutomaticRegistration = b;
	}

	/**
	 * @param b
	 */
	public void setPartyMatchingShowLevel(boolean b)
	{
		_partyMatchingShowLevel = b;
	}

	/**
	 * @param b
	 */
	public void setPartyMatchingShowClass(boolean b)
	{
		_partyMatchingShowClass = b;
	}

	/**
	 * @param memo
	 */
	public void setPartyMatchingMemo(String memo)
	{
		_partyMatchingMemo = memo;
	}

	public boolean isPartyMatchingAutomaticRegistration()
	{
		return _partyMatchingAutomaticRegistration;
	}

	public String getPartyMatchingMemo()
	{
		return _partyMatchingMemo;
	}

	public boolean isPartyMatchingShowClass()
	{
		return _partyMatchingShowClass;
	}

	public boolean isPartyMatchingShowLevel()
	{
		return _partyMatchingShowLevel;
	}

	/**
	 * Manage the increase level task of a L2PcInstance (Max MP, Max MP, Recommandation, Expertise and beginner skills...).<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Send a Server->Client System Message to the L2PcInstance : YOU_INCREASED_YOUR_LEVEL </li>
	 * <li>Send a Server->Client packet StatusUpdate to the L2PcInstance with new LEVEL, MAX_HP and MAX_MP </li>
	 * <li>Set the current HP and MP of the L2PcInstance, Launch/Stop a HP/MP/CP Regeneration Task and send StatusUpdate packet to all other L2PcInstance to inform (exclusive broadcast)</li>
	 * <li>Recalculate the party level</li>
	 * <li>Recalculate the number of Recommandation that the L2PcInstance can give</li>
	 * <li>Give Expertise skill of this level and remove beginner Lucky skill</li><BR><BR>
	 *
	 */
	public void increaseLevel()
	{
		// Set the current HP and MP of the L2Character, Launch/Stop a HP/MP/CP Regeneration Task and send StatusUpdate packet to all other L2PcInstance to inform (exclusive broadcast)
		setCurrentHpMp(getMaxHp(),getMaxMp());
		setCurrentCp(getMaxCp());
	}

	/**
	 * Stop the HP/MP/CP Regeneration task.<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Set the RegenActive flag to False </li>
	 * <li>Stop the HP/MP/CP Regeneration task </li><BR><BR>
	 *
	 */
	public void stopAllTimers()
	{
		stopHpMpRegeneration();
		stopWarnUserTakeBreak();
		stopWaterTask();
		stopFeed();
		clearPetData();
		storePetFood(_mountNpcId);
		stopRentPet();
		stopPvpRegTask();
		stopPunishTask(true);
		stopSoulTask();
		stopChargeTask();
		stopFameTask();
		stopVitalityTask();
	}

	/**
	 * Return the L2Summon of the L2PcInstance or null.<BR><BR>
	 */
	@Override
	public L2Summon getPet()
	{
		return _summon;
	}
	
	/**
     * Return the L2Decoy of the L2PcInstance or null.<BR><BR>
     */
    public L2Decoy getDecoy()
    {
        return _decoy;
    }
    
     /**
     * Return the L2Trap of the L2PcInstance or null.<BR><BR>
     */
    public L2Trap getTrap()
    {
    	return _trap;
    }

	/**
	 * Set the L2Summon of the L2PcInstance.<BR><BR>
	 */
	public void setPet(L2Summon summon)
	{
		_summon = summon;
		// update attack element value display
		if ((_summon == null || _summon instanceof L2SummonInstance)
				&& getClassId().isSummoner() && getAttackElement() != Elementals.NONE)
			sendPacket(new UserInfo(this));
			
	}
    /**
     * Set the L2Decoy of the L2PcInstance.<BR><BR>
     */
    public void setDecoy(L2Decoy decoy)
    {
        _decoy = decoy;
    }
    
    /**
     * Set the L2Trap of this L2PcInstance<BR><BR>
     * @param trap
     */
    public void setTrap(L2Trap trap)
    {
    	_trap = trap;
    }
    
	/**
	 * Return the L2Summon of the L2PcInstance or null.<BR><BR>
	 */
	public L2TamedBeastInstance getTrainedBeast()
	{
		return _tamedBeast;
	}

	/**
	 * Set the L2Summon of the L2PcInstance.<BR><BR>
	 */
	public void setTrainedBeast(L2TamedBeastInstance tamedBeast)
	{
		_tamedBeast = tamedBeast;
	}

	/**
	 * Return the L2PcInstance requester of a transaction (ex : FriendInvite, JoinAlly, JoinParty...).<BR><BR>
	 */
	public L2Request getRequest()
	{
		return _request;
	}

	/**
	 * Set the L2PcInstance requester of a transaction (ex : FriendInvite, JoinAlly, JoinParty...).<BR><BR>
	 */
	public synchronized void setActiveRequester(L2PcInstance requester)
	{
		_activeRequester = requester;
	}

	/**
	 * Return the L2PcInstance requester of a transaction (ex : FriendInvite, JoinAlly, JoinParty...).<BR><BR>
	 */
	public synchronized L2PcInstance getActiveRequester()
	{
		if (_activeRequester != null)
		{
			if (_activeRequester.isRequestExpired() && _activeTradeList == null)
				_activeRequester = null;
		}
		return _activeRequester;
	}

	/**
	 * Return True if a transaction is in progress.<BR><BR>
	 */
	public boolean isProcessingRequest()
	{
		return getActiveRequester() != null || _requestExpireTime > GameTimeController.getGameTicks();
	}

	/**
	 * Return True if a transaction is in progress.<BR><BR>
	 */
	public boolean isProcessingTransaction()
	{
		return getActiveRequester() != null || _activeTradeList != null || _requestExpireTime > GameTimeController.getGameTicks();
	}

	/**
	 * Select the Warehouse to be used in next activity.<BR><BR>
	 */
	public void onTransactionRequest(L2PcInstance partner)
	{
		_requestExpireTime = GameTimeController.getGameTicks() + REQUEST_TIMEOUT * GameTimeController.TICKS_PER_SECOND;
		partner.setActiveRequester(this);
	}
	
	/**
	 * Return true if last request is expired.
	 * @return
	 */
	public boolean isRequestExpired()
	{
		return !(_requestExpireTime > GameTimeController.getGameTicks());
	}

	/**
	 * Select the Warehouse to be used in next activity.<BR><BR>
	 */
	public void onTransactionResponse()
    {
        _requestExpireTime = 0;
    }

	/**
	 * Select the Warehouse to be used in next activity.<BR><BR>
	 */
	public void setActiveWarehouse(ItemContainer warehouse)
    {
        _activeWarehouse = warehouse;
    }

	/**
	 * Return active Warehouse.<BR><BR>
	 */
	public ItemContainer getActiveWarehouse()
    {
        return _activeWarehouse;
    }

	/**
	 * Select the TradeList to be used in next activity.<BR><BR>
	 */
	public void setActiveTradeList(TradeList tradeList)
    {
        _activeTradeList = tradeList;
    }

	/**
	 * Return active TradeList.<BR><BR>
	 */
	public TradeList getActiveTradeList()
    {
        return _activeTradeList;
    }

	public void onTradeStart(L2PcInstance partner)
	{
		_activeTradeList = new TradeList(this);
		_activeTradeList.setPartner(partner);

		SystemMessage msg = new SystemMessage(SystemMessageId.BEGIN_TRADE_WITH_C1);
		msg.addPcName(partner);
		sendPacket(msg);
		sendPacket(new TradeStart(this));
	}

	public void onTradeConfirm(L2PcInstance partner)
	{
		SystemMessage msg = new SystemMessage(SystemMessageId.C1_CONFIRMED_TRADE);
		msg.addPcName(partner);
		sendPacket(msg);
		sendPacket(new TradeOtherDone());
	}

	public void onTradeCancel(L2PcInstance partner)
	{
		if (_activeTradeList == null)
            return;

		_activeTradeList.lock();
		_activeTradeList = null;

		sendPacket(new TradeDone(0));
		SystemMessage msg = new SystemMessage(SystemMessageId.C1_CANCELED_TRADE);
		msg.addPcName(partner);
		sendPacket(msg);
	}

	public void onTradeFinish(boolean successfull)
	{
		_activeTradeList = null;
		sendPacket(new TradeDone(1));
        if (successfull)
            sendPacket(new SystemMessage(SystemMessageId.TRADE_SUCCESSFUL));
	}

	public void startTrade(L2PcInstance partner)
	{
		onTradeStart(partner);
		partner.onTradeStart(this);
	}

	public void cancelActiveTrade()
	{
		if (_activeTradeList == null)
            return;

		L2PcInstance partner = _activeTradeList.getPartner();
		if (partner != null) partner.onTradeCancel(this);
		onTradeCancel(this);
	}

	/**
	 * Return the _createList object of the L2PcInstance.<BR><BR>
	 */
	public L2ManufactureList getCreateList()
	{
		return _createList;
	}

	/**
	 * Set the _createList object of the L2PcInstance.<BR><BR>
	 */
	public void setCreateList(L2ManufactureList x)
	{
		_createList = x;
	}

	/**
	 * Return the _buyList object of the L2PcInstance.<BR><BR>
	 */
	public TradeList getSellList()
	{
		if (_sellList == null) _sellList = new TradeList(this);
		return _sellList;
	}

	/**
	 * Return the _buyList object of the L2PcInstance.<BR><BR>
	 */
	public TradeList getBuyList()
	{
		if (_buyList == null) _buyList = new TradeList(this);
		return _buyList;
	}

	/**
	 * Set the Private Store type of the L2PcInstance.<BR><BR>
	 *
	 * <B><U> Values </U> :</B><BR><BR>
	 * <li>0 : STORE_PRIVATE_NONE</li>
	 * <li>1 : STORE_PRIVATE_SELL</li>
	 * <li>2 : sellmanage</li><BR>
	 * <li>3 : STORE_PRIVATE_BUY</li><BR>
	 * <li>4 : buymanage</li><BR>
	 * <li>5 : STORE_PRIVATE_MANUFACTURE</li><BR>
	 *
	 */
	public void setPrivateStoreType(int type)
	{
		_privatestore = type;
	}

	/**
	 * Return the Private Store type of the L2PcInstance.<BR><BR>
	 *
	 * <B><U> Values </U> :</B><BR><BR>
	 * <li>0 : STORE_PRIVATE_NONE</li>
	 * <li>1 : STORE_PRIVATE_SELL</li>
	 * <li>2 : sellmanage</li><BR>
	 * <li>3 : STORE_PRIVATE_BUY</li><BR>
	 * <li>4 : buymanage</li><BR>
	 * <li>5 : STORE_PRIVATE_MANUFACTURE</li><BR>
	 *
	 */
	public int getPrivateStoreType()
	{
		return _privatestore;
	}

	/**
	 * Set the _skillLearningClassId object of the L2PcInstance.<BR><BR>
	 */
	public void setSkillLearningClassId(ClassId classId)
	{
		_skillLearningClassId = classId;
	}

	/**
	 * Return the _skillLearningClassId object of the L2PcInstance.<BR><BR>
	 */
	public ClassId getSkillLearningClassId()
	{
		return _skillLearningClassId;
	}

	/**
	 * Set the _clan object, _clanId, _clanLeader Flag and title of the L2PcInstance.<BR><BR>
	 */
	public void setClan(L2Clan clan)
	{
		_clan = clan;
		setTitle("");

		if (clan == null)
		{
			_clanId = 0;
			_clanPrivileges = 0;
			_pledgeType = 0;
			_powerGrade = 0;
			_lvlJoinedAcademy = 0;
			_apprentice = 0;
			_sponsor = 0;
			if (_isOnline)
				CommunityServerThread.getInstance().sendPacket(new WorldInfo(this, null, WorldInfo.TYPE_UPDATE_PLAYER_DATA));
			return;
		}

		if (!clan.isMember(getObjectId()))
		{
			// char has been kicked from clan
			setClan(null);
			return;
		}

		_clanId = clan.getClanId();
		if (_isOnline)
			CommunityServerThread.getInstance().sendPacket(new WorldInfo(this, null, WorldInfo.TYPE_UPDATE_PLAYER_DATA));
	}

	/**
	 * Return the _clan object of the L2PcInstance.<BR><BR>
	 */
	public L2Clan getClan()
	{
		return _clan;
	}

	/**
	 * Return True if the L2PcInstance is the leader of its clan.<BR><BR>
	 */
	public boolean isClanLeader()
	{
		if (getClan() == null)
		{
			return false;
		}
		else
		{
			return getObjectId() == getClan().getLeaderId();
		}
	}

	/**
	 * Reduce the number of arrows/bolts owned by the L2PcInstance and send it Server->Client Packet InventoryUpdate or ItemList (to unequip if the last arrow was consummed).<BR><BR>
	 */
	@Override
	protected void reduceArrowCount(boolean bolts)
	{
	    L2ItemInstance arrows = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);

	    if (arrows == null)
        {
            getInventory().unEquipItemInSlot(Inventory.PAPERDOLL_LHAND);
            if (bolts) 
                _boltItem = null;
            else
                _arrowItem = null;
            sendPacket(new ItemList(this,false));
            return;
        }
	    
	    // Adjust item quantity
        if (arrows.getCount() > 1)
        {
            synchronized(arrows)
            {
                arrows.changeCountWithoutTrace(-1, this, null);
                arrows.setLastChange(L2ItemInstance.MODIFIED);

                // could do also without saving, but let's save approx 1 of 10
                if(GameTimeController.getGameTicks() % 10 == 0)
                    arrows.updateDatabase();
                _inventory.refreshWeight();
            }
        }
        else
        {
            // Destroy entire item and save to database
            _inventory.destroyItem("Consume", arrows, this, null);
            
            getInventory().unEquipItemInSlot(Inventory.PAPERDOLL_LHAND);
            if (bolts) 
                _boltItem = null;
            else
                _arrowItem = null;

            if (Config.DEBUG) _log.fine("removed arrows count");
            sendPacket(new ItemList(this,false));
            return;
        }
	    
		if (!Config.FORCE_INVENTORY_UPDATE)
		{
		    InventoryUpdate iu = new InventoryUpdate();
		    iu.addModifiedItem(arrows);
		    sendPacket(iu);
		}
		else sendPacket(new ItemList(this, false));
	}

	/**
	 * Equip arrows needed in left hand and send a Server->Client packet ItemList to the L2PcINstance then return True.<BR><BR>
	 */
	@Override
	protected boolean checkAndEquipArrows()
	{
		// Check if nothing is equiped in left hand
		if (getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND) == null)
		{
			// Get the L2ItemInstance of the arrows needed for this bow
			_arrowItem = getInventory().findArrowForBow(getActiveWeaponItem());

			if (_arrowItem != null)
			{
				// Equip arrows needed in left hand
				getInventory().setPaperdollItem(Inventory.PAPERDOLL_LHAND, _arrowItem);

				// Send a Server->Client packet ItemList to this L2PcINstance to update left hand equipement
				ItemList il = new ItemList(this, false);
				sendPacket(il);
			}
		}
		else
		{
			// Get the L2ItemInstance of arrows equiped in left hand
			_arrowItem = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		}

		return _arrowItem != null;
	}

    /**
     * Equip bolts needed in left hand and send a Server->Client packet ItemList to the L2PcINstance then return True.<BR><BR>
     */
    @Override
    protected boolean checkAndEquipBolts()
    {
        // Check if nothing is equiped in left hand
        if (getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND) == null)
        {
            // Get the L2ItemInstance of the arrows needed for this bow
            _boltItem = getInventory().findBoltForCrossBow(getActiveWeaponItem());

            if (_boltItem != null)
            {
                // Equip arrows needed in left hand
                getInventory().setPaperdollItem(Inventory.PAPERDOLL_LHAND, _boltItem);

                // Send a Server->Client packet ItemList to this L2PcINstance to update left hand equipement
                ItemList il = new ItemList(this, false);
                sendPacket(il);
            }
        }
        else
        {
            // Get the L2ItemInstance of arrows equiped in left hand
            _boltItem = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
        }

        return _boltItem != null;
    }
	/**
	 * Disarm the player's weapon.<BR><BR>
	 */
	public boolean disarmWeapons()
	{
        // Don't allow disarming a cursed weapon
        if (isCursedWeaponEquipped()) return false;

        // Don't allow disarming a Combat Flag or Territory Ward
        if (isCombatFlagEquipped()) return false;

        // Unequip the weapon
        L2ItemInstance wpn = getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
        if (wpn == null) wpn = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LRHAND);
        if (wpn != null)
        {
        	if (wpn.isWear())
        		return false;

            L2ItemInstance[] unequiped = getInventory().unEquipItemInBodySlotAndRecord(wpn.getItem().getBodyPart());
            InventoryUpdate iu = new InventoryUpdate();
            for (L2ItemInstance itm: unequiped)
                iu.addModifiedItem(itm);
            sendPacket(iu);

            abortAttack();
            broadcastUserInfo();

            // this can be 0 if the user pressed the right mousebutton twice very fast
            if (unequiped.length > 0)
            {
                SystemMessage sm = null;
                if (unequiped[0].getEnchantLevel() > 0)
                {
                    sm = new SystemMessage(SystemMessageId.EQUIPMENT_S1_S2_REMOVED);
                    sm.addNumber(unequiped[0].getEnchantLevel());
                    sm.addItemName(unequiped[0]);
                }
                else
                {
                    sm = new SystemMessage(SystemMessageId.S1_DISARMED);
                    sm.addItemName(unequiped[0]);
                }
                sendPacket(sm);
            }
        }
        return true;
	}

	/**
	 * Disarm the player's shield.<BR><BR>
	 */
	public boolean disarmShield()
	{
		L2ItemInstance sld = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		if (sld != null)
		{
			if (sld.isWear())
				return false;

			L2ItemInstance[] unequiped = getInventory().unEquipItemInBodySlotAndRecord(sld.getItem().getBodyPart());
			InventoryUpdate iu = new InventoryUpdate();
			for (L2ItemInstance itm: unequiped)
				iu.addModifiedItem(itm);
			sendPacket(iu);

			abortAttack();
			broadcastUserInfo();

			// this can be 0 if the user pressed the right mousebutton twice very fast
			if (unequiped.length > 0)
			{
				SystemMessage sm = null;
				if (unequiped[0].getEnchantLevel() > 0)
				{
					sm = new SystemMessage(SystemMessageId.EQUIPMENT_S1_S2_REMOVED);
					sm.addNumber(unequiped[0].getEnchantLevel());
					sm.addItemName(unequiped[0]);
				}
				else
				{
					sm = new SystemMessage(SystemMessageId.S1_DISARMED);
					sm.addItemName(unequiped[0]);
				}
				sendPacket(sm);
			}
		}
		return true;
	}

    public boolean mount(L2Summon pet)
    {
        if (!disarmWeapons())
            return false;
        if (!disarmShield())
        	return false;
        if (isTransformed())
        	return false;

        stopAllToggles();
        Ride mount = new Ride(this, true, pet.getTemplate().npcId);
        setMount(pet.getNpcId(), pet.getLevel(), mount.getMountType());
        setMountObjectID(pet.getControlItemId());
        clearPetData();
        startFeed(pet.getNpcId());
        broadcastPacket(mount);
        
        // Notify self and others about speed change
        broadcastUserInfo();
        
        pet.unSummon(this);
        
        return true;
    }
    
    public boolean mount(int npcId, int controlItemObjId,boolean useFood)
    {
        if (!disarmWeapons())
            return false;
        if (!disarmShield())
        	return false;
        if (isTransformed())
        	return false;

        stopAllToggles();
        Ride mount = new Ride(this, true, npcId);
        if (setMount(npcId, getLevel(), mount.getMountType()))
        {
        	clearPetData();
        	setMountObjectID(controlItemObjId);
        	broadcastPacket(mount);
        
        	// Notify self and others about speed change
        	broadcastUserInfo();
        	if (useFood)
        		startFeed(npcId);
        	return true;
        }
        return false;
    }
    
    public boolean mountPlayer(L2Summon pet)
    {
    	if (pet != null && pet.isMountable() && !isMounted() && !isBetrayed())
    	{
    		if (isDead())
    		{
    			//A strider cannot be ridden when dead
    			sendPacket(ActionFailed.STATIC_PACKET);
    			sendPacket(new SystemMessage(SystemMessageId.STRIDER_CANT_BE_RIDDEN_WHILE_DEAD));
    			return false;
    		}
    		else if (pet.isDead())
    		{
    			//A dead strider cannot be ridden.
    			sendPacket(ActionFailed.STATIC_PACKET);
    			sendPacket(new SystemMessage(SystemMessageId.DEAD_STRIDER_CANT_BE_RIDDEN));
    			return false;
    		}
    		else if (pet.isInCombat() || pet.isRooted())
    		{
    			//A strider in battle cannot be ridden
    			sendPacket(ActionFailed.STATIC_PACKET);
    			sendPacket(new SystemMessage(SystemMessageId.STRIDER_IN_BATLLE_CANT_BE_RIDDEN));
    			return false;
    			
    		}
    		else if (isInCombat())
    		{
    			//A strider cannot be ridden while in battle
    			sendPacket(ActionFailed.STATIC_PACKET);
    			sendPacket(new SystemMessage(SystemMessageId.STRIDER_CANT_BE_RIDDEN_WHILE_IN_BATTLE));
    			return false;
    		}
    		else if (isSitting())
    		{
    			//A strider can be ridden only when standing
    			sendPacket(ActionFailed.STATIC_PACKET);
    			sendPacket(new SystemMessage(SystemMessageId.STRIDER_CAN_BE_RIDDEN_ONLY_WHILE_STANDING));
    			return false;
    		}
    		else if (isFishing())
    		{
    			//You can't mount, dismount, break and drop items while fishing
    			sendPacket(ActionFailed.STATIC_PACKET);
    			sendPacket(new SystemMessage(SystemMessageId.CANNOT_DO_WHILE_FISHING_2));
    			return false;
    		}
    		else if (isTransformed() || isCursedWeaponEquipped())
    		{
    			// no message needed, player while transformed doesn't have mount action
    			sendPacket(ActionFailed.STATIC_PACKET);
    			return false;
    		}
    		else if (getInventory().getItemByItemId(9819) != null)
    		{
    			sendPacket(ActionFailed.STATIC_PACKET);
    			sendPacket(new SystemMessage(SystemMessageId.YOU_CANNOT_MOUNT_A_STEED_WHILE_HOLDING_A_FLAG)); // TODO: confirm this message
    			return false;
    		}
    		else if (pet.isHungry())
    		{
    			sendPacket(ActionFailed.STATIC_PACKET);
    			sendPacket(new SystemMessage(SystemMessageId.HUNGRY_STRIDER_NOT_MOUNT));
    			return false;
    		}
    		else if (!Util.checkIfInRange(200, this, pet, true))
    		{
    			sendPacket(ActionFailed.STATIC_PACKET);
    			sendPacket(new SystemMessage(SystemMessageId.TOO_FAR_AWAY_FROM_FENRIR_TO_MOUNT));
    			return false;				
    		}
    		else if (!pet.isDead() && !isMounted())
    		{
    			mount(pet);
    		}
    	}
    	else if (isRentedPet())
    	{
    		stopRentPet();
    	}
    	else if (isMounted())
    	{
    		if (getMountType() == 2 && this.isInsideZone(L2Character.ZONE_NOLANDING))
    		{
    			sendPacket(ActionFailed.STATIC_PACKET);
    			sendPacket(new SystemMessage(SystemMessageId.NO_DISMOUNT_HERE));
    			return false;
    		}
    		else if (isHungry())
    		{
    			sendPacket(ActionFailed.STATIC_PACKET);
    			sendPacket(new SystemMessage(SystemMessageId.HUNGRY_STRIDER_NOT_MOUNT));
    			return false;
    		}
    		else
    			dismount();
    	}
    	return true;
    }
    
	public boolean dismount()
	{
		boolean wasFlying = isFlying();
		
		sendPacket(new SetupGauge(3, 0, 0));
		int petId = _mountNpcId;
	    if (setMount(0, 0, 0))
	    {
	    	stopFeed();
	    	clearPetData();
	        if (wasFlying) 
	            removeSkill(SkillTable.FrequentSkill.WYVERN_BREATH.getSkill());
	        Ride dismount = new Ride(this, false, 0);
	        broadcastPacket(dismount);
	        setMountObjectID(0);
	        storePetFood(petId);
            // Notify self and others about speed change
            this.broadcastUserInfo();
	        return true;
	    }
        return false;
	}

	/**
	 * Return True if the L2PcInstance use a dual weapon.<BR><BR>
	 */
	@Override
	public boolean isUsingDualWeapon()
	{
		L2Weapon weaponItem = getActiveWeaponItem();
		if (weaponItem == null) return false;

		if (weaponItem.getItemType() == L2WeaponType.DUAL)
			return true;
		else if (weaponItem.getItemType() == L2WeaponType.DUALFIST)
			return true;
		else if (weaponItem.getItemId() == 248) // orc fighter fists
			return true;
		else if (weaponItem.getItemId() == 252) // orc mage fists
			return true;
		else
			return false;
	}

	public void setUptime(long time)
	{
		_uptime = time;
	}

	public long getUptime()
	{
		return System.currentTimeMillis()-_uptime;
	}

	/**
	 * Return True if the L2PcInstance is invulnerable.<BR><BR>
	 */
	@Override
	public boolean isInvul()
	{
		return _isInvul  || _isTeleporting ||  _protectEndTime > GameTimeController.getGameTicks();
	}

	/**
	 * Return True if the L2PcInstance has a Party in progress.<BR><BR>
	 */
	@Override
	public boolean isInParty()
	{
		return _party != null;
	}

	/**
	 * Set the _party object of the L2PcInstance (without joining it).<BR><BR>
	 */
	public void setParty(L2Party party)
	{
		_party = party;
	}

	/**
	 * Set the _party object of the L2PcInstance AND join it.<BR><BR>
	 */
	public void joinParty(L2Party party)
	{
		if (party != null)
		{
            // First set the party otherwise this wouldn't be considered
            // as in a party into the L2Character.updateEffectIcons() call.
            _party = party;
			party.addPartyMember(this);
		}
	}


	/**
	 * Manage the Leave Party task of the L2PcInstance.<BR><BR>
	 */
	public void leaveParty()
	{
		if (isInParty())
		{
			_party.removePartyMember(this);
			_party = null;
		}
	}

	/**
	 * Return the _party object of the L2PcInstance.<BR><BR>
	 */
	@Override
	public L2Party getParty()
	{
		return _party;
	}

	/**
	 * Return True if the L2PcInstance is a GM.<BR><BR>
	 */
	@Override
	public boolean isGM()
	{
		return getAccessLevel().isGm();
	}

	/**
	 * Set the _accessLevel of the L2PcInstance.<BR><BR>
	 */
	public void setAccessLevel(int level)
	{
		if (level == AccessLevels._masterAccessLevelNum)
		{
			_log.warning( "Master access level set for character " + getName() + "! Just a warning to be careful ;)" );
			_accessLevel = AccessLevels._masterAccessLevel;
		}
		else if (level == AccessLevels._userAccessLevelNum)
			_accessLevel = AccessLevels._userAccessLevel;
		else
		{
			L2AccessLevel accessLevel = AccessLevels.getInstance().getAccessLevel(level);

			if (accessLevel == null)
			{
				if (level < 0)
				{
					AccessLevels.getInstance().addBanAccessLevel(level);
					_accessLevel = AccessLevels.getInstance().getAccessLevel(level);
				}
				else
				{
					_log.warning( "Tryed to set unregistered access level " + level + " to character " + getName() + ". Setting access level without privileges!" );
					_accessLevel = AccessLevels._userAccessLevel;
				}
			}
			else
				_accessLevel = accessLevel;
		}
		
		getAppearance().setNameColor(_accessLevel.getNameColor());
		getAppearance().setTitleColor(_accessLevel.getTitleColor());
		broadcastUserInfo();
		
		CharNameTable.getInstance().addName(this);
	}

	public void setAccountAccesslevel(int level)
	{
		LoginServerThread.getInstance().sendAccessLevel(getAccountName(),level);
	}

	/**
	 * Return the _accessLevel of the L2PcInstance.<BR><BR>
	 */
	public L2AccessLevel getAccessLevel()
	{
		if (Config.EVERYBODY_HAS_ADMIN_RIGHTS)
			return AccessLevels._masterAccessLevel;
		else if ( _accessLevel == null ) /* This is here because inventory etc. is loaded before access level on login, so it is not null */
			setAccessLevel(AccessLevels._userAccessLevelNum);

		return _accessLevel;
	}

	@Override
	public double getLevelMod()
	{
		return (100.0 - 11 + getLevel()) / 100.0;
	}

	/**
	 * Update Stats of the L2PcInstance client side by sending Server->Client packet UserInfo/StatusUpdate to this L2PcInstance and CharInfo/StatusUpdate to all L2PcInstance in its _KnownPlayers (broadcast).<BR><BR>
	 */
	public void updateAndBroadcastStatus(int broadcastType)
	{
		refreshOverloaded();
		refreshExpertisePenalty();
		// Send a Server->Client packet UserInfo to this L2PcInstance and CharInfo to all L2PcInstance in its _KnownPlayers (broadcast)
		if (broadcastType == 1)
		{
			sendPacket(new UserInfo(this));
			sendPacket(new ExBrExtraUserInfo(this));
		}
		if (broadcastType == 2) broadcastUserInfo();
	}

	/**
	 * Send a Server->Client StatusUpdate packet with Karma and PvP Flag to the L2PcInstance and all L2PcInstance to inform (broadcast).<BR><BR>
	 */
	public void setKarmaFlag(int flag)
	{
		sendPacket(new UserInfo(this));
		sendPacket(new ExBrExtraUserInfo(this));
		Collection<L2PcInstance> plrs = getKnownList().getKnownPlayers().values();
		//synchronized (getKnownList().getKnownPlayers())
		{
			for (L2PcInstance player : plrs)
			{
				player.sendPacket(new RelationChanged(this, getRelation(player), isAutoAttackable(player)));
				if (getPet() != null)
					player.sendPacket(new RelationChanged(getPet(), getRelation(player), isAutoAttackable(player)));
			}
		}
	}

	/**
	 * Send a Server->Client StatusUpdate packet with Karma to the L2PcInstance and all L2PcInstance to inform (broadcast).<BR><BR>
	 */
	public void broadcastKarma()
	{
        StatusUpdate su = new StatusUpdate(getObjectId());
        su.addAttribute(StatusUpdate.KARMA, getKarma());
        sendPacket(su);

        Collection<L2PcInstance> plrs = getKnownList().getKnownPlayers().values();
        //synchronized (getKnownList().getKnownPlayers())
		{
			for (L2PcInstance player : plrs)
			{
				player.sendPacket(new RelationChanged(this, getRelation(player), isAutoAttackable(player)));
				if (getPet() != null)
					player.sendPacket(new RelationChanged(getPet(), getRelation(player), isAutoAttackable(player)));
			}
		}
	}

	/**
	 * Set the online Flag to True or False and update the characters table of the database with online status and lastAccess (called when login and logout).<BR><BR>
	 */
	public void setOnlineStatus(boolean isOnline)
	{
		if (_isOnline != isOnline)
			_isOnline = isOnline;

		// Update the characters table of the database with online status and lastAccess (called when login and logout)
		updateOnlineStatus();
	}
    
    public void setIsIn7sDungeon(boolean isIn7sDungeon)
    {
        _isIn7sDungeon = isIn7sDungeon;
    }

	/**
	 * Update the characters table of the database with online status and lastAccess of this L2PcInstance (called when login and logout).<BR><BR>
	 */
	public void updateOnlineStatus()
	{
		Connection con = null;
		try
        {
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE characters SET online=?, lastAccess=? WHERE charId=?");
			statement.setInt(1, isOnline());
			statement.setLong(2, System.currentTimeMillis());
			statement.setInt(3, getObjectId());
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Failed updating character online status.", e);
		}
		finally
		{
			try { con.close(); } catch (Exception e) {}
		}
	}

	/**
	 * Create a new player in the characters table of the database.<BR><BR>
	 */
	private boolean createDb()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(INSERT_CHARACTER);
			
			statement.setString(1, _accountName);
			statement.setInt(2, getObjectId());
			statement.setString(3, getName());
			statement.setInt(4, getLevel());
			statement.setInt(5, getMaxHp());
			statement.setDouble(6, getCurrentHp());
			statement.setInt(7, getMaxCp());
			statement.setDouble(8, getCurrentCp());
			statement.setInt(9, getMaxMp());
			statement.setDouble(10, getCurrentMp());
			statement.setInt(11, getAppearance().getFace());
			statement.setInt(12, getAppearance().getHairStyle());
			statement.setInt(13, getAppearance().getHairColor());
			statement.setInt(14, getAppearance().getSex()? 1 : 0);
			statement.setLong(15, getExp());
			statement.setInt(16, getSp());
			statement.setInt(17, getKarma());
			statement.setInt(18, getFame());
			statement.setInt(19, getPvpKills());
			statement.setInt(20, getPkKills());
			statement.setInt(21, getClanId());
			statement.setInt(22, getRace().ordinal());
			statement.setInt(23, getClassId().getId());
			statement.setLong(24, getDeleteTimer());
			statement.setInt(25, hasDwarvenCraft() ? 1 : 0);
			statement.setString(26, getTitle());
			statement.setInt(27, getAppearance().getTitleColor());
			statement.setInt(28, getAccessLevel().getLevel());
			statement.setInt(29, isOnline());
            statement.setInt(30, isIn7sDungeon() ? 1 : 0);
			statement.setInt(31, getClanPrivileges());
			statement.setInt(32, getWantsPeace());
			statement.setInt(33, getBaseClass());
			statement.setInt(34, getNewbie());
			statement.setInt(35, isNoble() ? 1 :0);
			statement.setLong(36, 0);
			statement.setLong(37,System.currentTimeMillis());
			statement.setLong(38,getCreateTime()); 
			
			statement.executeUpdate();
			statement.close();
		}
		catch (Exception e)
		{
			_log.severe("Could not insert char data: " + e);
			return false;
		}
		finally
		{
			try { con.close(); } catch (Exception e) {}
		}
		return true;
	}

	/**
	 * Retrieve a L2PcInstance from the characters table of the database and add it in _allObjects of the L2world.<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Retrieve the L2PcInstance from the characters table of the database </li>
	 * <li>Add the L2PcInstance object in _allObjects </li>
	 * <li>Set the x,y,z position of the L2PcInstance and make it invisible</li>
	 * <li>Update the overloaded status of the L2PcInstance</li><BR><BR>
	 *
	 * @param objectId Identifier of the object to initialized
	 *
	 * @return The L2PcInstance loaded from the database
	 *
	 */
	private static L2PcInstance restore(int objectId)
	{
		L2PcInstance player = null;
		Connection con = null;

		try
		{
			// Retrieve the L2PcInstance from the characters table of the database
			con = L2DatabaseFactory.getInstance().getConnection();

			PreparedStatement statement = con.prepareStatement(RESTORE_CHARACTER);
			statement.setInt(1, objectId);
			ResultSet rset = statement.executeQuery();

			double currentCp = 0;
			double currentHp = 0;
			double currentMp = 0;

			while (rset.next())
			{
				final int activeClassId = rset.getInt("classid");
				final boolean female = rset.getInt("sex")!=0;
				final L2PcTemplate template = CharTemplateTable.getInstance().getTemplate(activeClassId);
				PcAppearance app = new PcAppearance(rset.getByte("face"), rset.getByte("hairColor"), rset.getByte("hairStyle"), female);

				player = new L2PcInstance(objectId, template, rset.getString("account_name"), app);
				player.setName(rset.getString("char_name"));
				player._lastAccess = rset.getLong("lastAccess");

				player.getStat().setExp(rset.getLong("exp"));
				player.setExpBeforeDeath(rset.getLong("expBeforeDeath"));
				player.getStat().setLevel(rset.getByte("level"));
				player.getStat().setSp(rset.getInt("sp"));

				player.setWantsPeace(rset.getInt("wantspeace"));

				player.setHeading(rset.getInt("heading"));

				player.setKarma(rset.getInt("karma"));
				player.setFame(rset.getInt("fame"));
				player.setPvpKills(rset.getInt("pvpkills"));
				player.setPkKills(rset.getInt("pkkills"));
				player.setOnlineTime(rset.getLong("onlinetime"));
				player.setNewbie(rset.getInt("newbie"));
				player.setNoble(rset.getInt("nobless")==1);

				player.setClanJoinExpiryTime(rset.getLong("clan_join_expiry_time"));
				if (player.getClanJoinExpiryTime() < System.currentTimeMillis())
				{
					player.setClanJoinExpiryTime(0);
				}
				player.setClanCreateExpiryTime(rset.getLong("clan_create_expiry_time"));
				if (player.getClanCreateExpiryTime() < System.currentTimeMillis())
				{
					player.setClanCreateExpiryTime(0);
				}

				int clanId	= rset.getInt("clanid");
				player.setPowerGrade((int)rset.getLong("power_grade"));
				player.setPledgeType(rset.getInt("subpledge"));
				player.setLastRecomUpdate(rset.getLong("last_recom_date"));
				//player.setApprentice(rset.getInt("apprentice"));

				if (clanId > 0)
				{
					player.setClan(ClanTable.getInstance().getClan(clanId));
				}

				if (player.getClan() != null)
                {
                    if (player.getClan().getLeaderId() != player.getObjectId())
                    {
                        if (player.getPowerGrade() == 0)
                        {
                        	player.setPowerGrade(5);
                        }
                    	player.setClanPrivileges(player.getClan().getRankPrivs(player.getPowerGrade()));
                    }
                    else
                    {
                    	player.setClanPrivileges(L2Clan.CP_ALL);
                    	player.setPowerGrade(1);
                    }
                }
                else
            	{
                	player.setClanPrivileges(L2Clan.CP_NOTHING);
            	}

				player.setDeleteTimer(rset.getLong("deletetime"));

				player.setTitle(rset.getString("title"));
				player.getAppearance().setTitleColor(rset.getInt("title_color"));
				player.setAccessLevel(rset.getInt("accesslevel"));
				player.setFistsWeaponItem(player.findFistsWeaponItem(activeClassId));
				player.setUptime(System.currentTimeMillis());

				currentHp = rset.getDouble("curHp");
				currentCp = rset.getDouble("curCp");
				currentMp = rset.getDouble("curMp");

				//Check recs
				player.checkRecom(rset.getInt("rec_have"),rset.getInt("rec_left"));

				player._classIndex = 0;
				try { player.setBaseClass(rset.getInt("base_class")); }
				catch (Exception e) { player.setBaseClass(activeClassId); }

				// Restore Subclass Data (cannot be done earlier in function)
                if (restoreSubClassData(player))
                {
                	if (activeClassId != player.getBaseClass())
                	{
                    	for (SubClass subClass : player.getSubClasses().values())
                    		if (subClass.getClassId() == activeClassId)
                    			player._classIndex = subClass.getClassIndex();
                	}
                }
                if (player.getClassIndex() == 0 && activeClassId != player.getBaseClass())
                {
                	// Subclass in use but doesn't exist in DB -
                	// a possible restart-while-modifysubclass cheat has been attempted.
                	// Switching to use base class
                	player.setClassId(player.getBaseClass());
                	_log.warning("Player "+player.getName()+" reverted to base class. Possibly has tried a relogin exploit while subclassing.");
                }
                else player._activeClass = activeClassId;

                player.setApprentice(rset.getInt("apprentice"));
                player.setSponsor(rset.getInt("sponsor"));
                player.setLvlJoinedAcademy(rset.getInt("lvl_joined_academy"));
                player.setIsIn7sDungeon(rset.getInt("isin7sdungeon") == 1);
                player.setPunishLevel(rset.getInt("punish_level"));
                if (player.getPunishLevel() != PunishLevel.NONE)
                	player.setPunishTimer(rset.getLong("punish_timer"));
                else
                	player.setPunishTimer(0);
                
                CursedWeaponsManager.getInstance().checkPlayer(player);

                player.setAllianceWithVarkaKetra(rset.getInt("varka_ketra_ally"));

                player.setDeathPenaltyBuffLevel(rset.getInt("death_penalty_level"));

                player.setVitalityPoints(rset.getInt("vitality_points"), true);

                // Add the L2PcInstance object in _allObjects
				//L2World.getInstance().storeObject(player);

				// Set the x,y,z position of the L2PcInstance and make it invisible
				player.setXYZInvisible(rset.getInt("x"), rset.getInt("y"), rset.getInt("z"));

				// Set Teleport Bookmark Slot
				player.setBookMarkSlot(rset.getInt("BookmarkSlot"));
				
				//character creation Time 
				player.setCreateTime(rset.getLong("createTime")); 

				// Language
				player.setLang(rset.getString("language"));

				// Retrieve the name and ID of the other characters assigned to this account.
				PreparedStatement stmt = con.prepareStatement("SELECT charId, char_name FROM characters WHERE account_name=? AND charId<>?");
				stmt.setString(1, player._accountName);
				stmt.setInt(2, objectId);
				ResultSet chars = stmt.executeQuery();

				while (chars.next())
				{
					Integer charId = chars.getInt("charId");
					String charName = chars.getString("char_name");
					player._chars.put(charId, charName);
				}

				chars.close();
				stmt.close();
				break;
			}

			rset.close();
			statement.close();

			// Retrieve from the database all secondary data of this L2PcInstance
			// and reward expertise/lucky skills if necessary.
			// Note that Clan, Noblesse and Hero skills are given separately and not here.
			player.restoreCharData();
			player.rewardSkills();
			
		    // buff and status icons
	        if (Config.STORE_SKILL_COOLTIME)
	            player.restoreEffects();

			// Restore current Cp, HP and MP values
			player.setCurrentCp(currentCp);
			player.setCurrentHp(currentHp);
			player.setCurrentMp(currentMp);
			
			if (currentHp < 0.5) {
				player.setIsDead(true);
				player.stopHpMpRegeneration();
			}

			// Restore pet if exists in the world
			player.setPet(L2World.getInstance().getPet(player.getObjectId()));
			if (player.getPet() != null)
				player.getPet().setOwner(player);
			
			// Update the overloaded status of the L2PcInstance
			player.refreshOverloaded();
			// Update the expertise status of the L2PcInstance
			player.refreshExpertisePenalty();
			
			player.restoreFriendList();
			
			if (Config.STORE_UI_SETTINGS)
				player.restoreUISettings();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Failed loading character.", e);
		}
		finally
		{
			try { con.close(); } catch (Exception e) {}
		}

		return player;
	}

	/**
	 * @return
	 */
	public Forum getMail()
	{
		if (_forumMail == null)
		{
			setMail(ForumsBBSManager.getInstance().getForumByName("MailRoot").getChildByName(getName()));

			if (_forumMail == null)
			{
				ForumsBBSManager.getInstance().createNewForum(getName(),ForumsBBSManager.getInstance().getForumByName("MailRoot"),Forum.MAIL,Forum.OWNERONLY,getObjectId());
				setMail(ForumsBBSManager.getInstance().getForumByName("MailRoot").getChildByName(getName()));
			}
		}

		return _forumMail;
	}

	/**
	 * @param forum
	 */
	public void setMail(Forum forum)
	{
		_forumMail = forum;
	}

	/**
	 * @return
	 */
	public Forum getMemo()
	{
		if (_forumMemo == null)
		{
			setMemo(ForumsBBSManager.getInstance().getForumByName("MemoRoot").getChildByName(_accountName));
			
			if (_forumMemo == null)
			{
				ForumsBBSManager.getInstance().createNewForum(_accountName, ForumsBBSManager.getInstance().getForumByName("MemoRoot"), Forum.MEMO, Forum.OWNERONLY, getObjectId());
				setMemo(ForumsBBSManager.getInstance().getForumByName("MemoRoot").getChildByName(_accountName));
			}
		}

		return _forumMemo;
	}

	/**
	 * @param forum
	 */
	public void setMemo(Forum forum)
	{
		_forumMemo = forum;
	}

    /**
     * Restores sub-class data for the L2PcInstance, used to check the current
     * class index for the character.
     */
    private static boolean restoreSubClassData(L2PcInstance player)
    {
        Connection con = null;

        try
        {
            con = L2DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement = con.prepareStatement(RESTORE_CHAR_SUBCLASSES);
            statement.setInt(1, player.getObjectId());

            ResultSet rset = statement.executeQuery();

            while (rset.next())
            {
                SubClass subClass = new SubClass();
                subClass.setClassId(rset.getInt("class_id"));
                subClass.setLevel(rset.getByte("level"));
                subClass.setExp(rset.getLong("exp"));
                subClass.setSp(rset.getInt("sp"));
                subClass.setClassIndex(rset.getInt("class_index"));

                // Enforce the correct indexing of _subClasses against their class indexes.
                player.getSubClasses().put(subClass.getClassIndex(), subClass);
            }

            statement.close();
        }
        catch (Exception e) {
            _log.warning("Could not restore classes for " + player.getName() + ": " + e);
            e.printStackTrace();
        }
        finally {
            try { con.close(); } catch (Exception e) {}
        }

        return true;
    }

	/**
	 * Restores secondary data for the L2PcInstance, based on the current class index.
	 */
	private void restoreCharData()
	{
		// Retrieve from the database all skills of this L2PcInstance and add them to _skills.
		restoreSkills();

		// Retrieve from the database all macroses of this L2PcInstance and add them to _macroses.
		_macroses.restore();

		// Retrieve from the database all shortCuts of this L2PcInstance and add them to _shortCuts.
		_shortCuts.restore();

		// Retrieve from the database all henna of this L2PcInstance and add them to _henna.
		restoreHenna();

		// Retrieve from the database all teleport bookmark of this L2PcInstance and add them to _tpbookmark.
		restoreTeleportBookmark();

		// Retrieve from the database all recom data of this L2PcInstance and add to _recomChars.
		if (Config.ALT_RECOMMEND) restoreRecom();

		// Retrieve from the database the recipe book of this L2PcInstance.
		restoreRecipeBook(true);
		
		// Restore Recipe Shop list
		if(Config.STORE_RECIPE_SHOPLIST)
			restoreRecipeShopList();
	}

	/**
	 * Restore recipe book data for this L2PcInstance.
	 */
	private void restoreRecipeBook(boolean loadCommon)
	{
		Connection con = null;

		try {
			con = L2DatabaseFactory.getInstance().getConnection();
			String sql = loadCommon ? "SELECT id, type, classIndex FROM character_recipebook WHERE charId=?" :
											"SELECT id FROM character_recipebook WHERE charId=? AND classIndex=? AND type = 1";
			PreparedStatement statement = con.prepareStatement(sql);
			statement.setInt(1, getObjectId());
			if (!loadCommon)
				statement.setInt(2, _classIndex);
			ResultSet rset = statement.executeQuery();

			_dwarvenRecipeBook.clear();
			
			L2RecipeList recipe;
			while (rset.next()) {
				recipe = RecipeController.getInstance().getRecipeList(rset.getInt("id"));

				if (loadCommon)
				{
					if (rset.getInt(2) == 1)
					{
						if (rset.getInt(3) == _classIndex)
							registerDwarvenRecipeList(recipe, false);
					}
					else
						registerCommonRecipeList(recipe, false);
				}
				else
					registerDwarvenRecipeList(recipe, false);
			}

			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			if (_log.isLoggable(Level.SEVERE))
				_log.warning("Could not restore recipe book data:" + e);
		}
		finally {
			try { con.close(); } catch (Exception e) {}
		}
	}

	/**
	 * Update L2PcInstance stats in the characters table of the database.<BR><BR>
	 */
	public synchronized void store(boolean storeActiveEffects)
	{
		//update client coords, if these look like true
        // if (isInsideRadius(getClientX(), getClientY(), 1000, true))
        //    setXYZ(getClientX(), getClientY(), getClientZ());

		storeCharBase();
		storeCharSub();
		storeEffect(storeActiveEffects);
		transformInsertInfo();
		if(Config.STORE_RECIPE_SHOPLIST)
			storeRecipeShopList();
		if (Config.STORE_UI_SETTINGS)
			storeUISettings();
		SevenSigns.getInstance().saveSevenSignsData(getObjectId());
	}
	
	public void store()
	{
		store(true);
	}

	private void storeCharBase()
	{
		Connection con = null;

		try
		{
			// Get the exp, level, and sp of base class to store in base table
			int currentClassIndex = getClassIndex();
			_classIndex = 0;
			long exp     = getStat().getExp();
			int level   = getStat().getLevel();
			int sp      = getStat().getSp();
			_classIndex = currentClassIndex;

			con = L2DatabaseFactory.getInstance().getConnection();
			// Update base class
			PreparedStatement statement = con.prepareStatement(UPDATE_CHARACTER);
			
			statement.setInt(1, level);
			statement.setInt(2, getMaxHp());
			statement.setDouble(3, getCurrentHp());
			statement.setInt(4, getMaxCp());
			statement.setDouble(5, getCurrentCp());
			statement.setInt(6, getMaxMp());
			statement.setDouble(7, getCurrentMp());
			statement.setInt(8, getAppearance().getFace());
			statement.setInt(9, getAppearance().getHairStyle());
			statement.setInt(10, getAppearance().getHairColor());
			statement.setInt(11, getAppearance().getSex()? 1 : 0);
			statement.setInt(12, getHeading());
			statement.setInt(13, _observerMode ? _obsX : getX());
			statement.setInt(14, _observerMode ? _obsY : getY());
			statement.setInt(15, _observerMode ? _obsZ : getZ());
			statement.setLong(16, exp);
			statement.setLong(17, getExpBeforeDeath());
			statement.setInt(18, sp);
			statement.setInt(19, getKarma());
			statement.setInt(20, getFame());
			statement.setInt(21, getPvpKills());
			statement.setInt(22, getPkKills());
			statement.setInt(23, getRecomHave());
			statement.setInt(24, getRecomLeft());
			statement.setInt(25, getClanId());
			statement.setInt(26, getRace().ordinal());
			statement.setInt(27, getClassId().getId());
			statement.setLong(28, getDeleteTimer());
			statement.setString(29, getTitle());
			statement.setInt(30, getAppearance().getTitleColor());
			statement.setInt(31, getAccessLevel().getLevel());
			statement.setInt(32, isOnline());
            statement.setInt(33, isIn7sDungeon() ? 1 : 0);
			statement.setInt(34, getClanPrivileges());
			statement.setInt(35, getWantsPeace());
			statement.setInt(36, getBaseClass());

			long totalOnlineTime = _onlineTime;

			if (_onlineBeginTime > 0)
				totalOnlineTime += (System.currentTimeMillis()-_onlineBeginTime)/1000;

            statement.setLong(37, totalOnlineTime);
            statement.setInt(38, getPunishLevel().value());
            statement.setLong(39, getPunishTimer());
            statement.setInt(40, getNewbie());
            statement.setInt(41, isNoble() ? 1 : 0);
            statement.setLong(42, getPowerGrade());
            statement.setInt(43, getPledgeType());
            statement.setLong(44,getLastRecomUpdate());
            statement.setInt(45,getLvlJoinedAcademy());
            statement.setLong(46,getApprentice());
            statement.setLong(47,getSponsor());
            statement.setInt(48, getAllianceWithVarkaKetra());
			statement.setLong(49, getClanJoinExpiryTime());
			statement.setLong(50, getClanCreateExpiryTime());
			statement.setString(51, getName());
			statement.setLong(52, getDeathPenaltyBuffLevel());
			statement.setInt(53, getBookMarkSlot());
			statement.setInt(54, getVitalityPoints());
			statement.setString(55, getLang());
            statement.setInt(56, getObjectId());

			statement.execute();
			statement.close();
		}
		catch (Exception e) { _log.warning("Could not store char base data: "+ e); }
		finally { try { con.close(); } catch (Exception e) {} }
	}

	private void storeCharSub()
	{
		Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(UPDATE_CHAR_SUBCLASS);
			
			if (getTotalSubClasses() > 0)
			{
				for (SubClass subClass : getSubClasses().values())
				{
					statement.setLong(1, subClass.getExp());
					statement.setInt(2, subClass.getSp());
					statement.setInt(3, subClass.getLevel());
					statement.setInt(4, subClass.getClassId());
					statement.setInt(5, getObjectId());
					statement.setInt(6, subClass.getClassIndex());
					
					statement.execute();
				}
			}
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("Could not store sub class data for " + getName() + ": " + e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
			}
		}
	}
    
    private void storeEffect(boolean storeEffects)
	{
		if (!Config.STORE_SKILL_COOLTIME)
			return;
		
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			
			
			// Delete all current stored effects for char to avoid dupe
			PreparedStatement statement = con.prepareStatement(DELETE_SKILL_SAVE);
			
			statement.setInt(1, getObjectId());
			statement.setInt(2, getClassIndex());
			statement.execute();
			statement.close();
			
			int buff_index = 0;
			
			final List<Integer> storedSkills = new FastList<Integer>();
			
			// Store all effect data along with calulated remaining
			// reuse delays for matching skills. 'restore_type'= 0.
			statement = con.prepareStatement(ADD_SKILL_SAVE);

			if (storeEffects)
			{
				for (L2Effect effect : getAllEffects())
				{
					if (effect == null)
						continue;

					switch (effect.getEffectType())
					{
						case HEAL_OVER_TIME:
						case COMBAT_POINT_HEAL_OVER_TIME:
						// TODO: Fix me.
						case HIDE:
							continue;
					}

					L2Skill skill = effect.getSkill();
					if (storedSkills.contains(skill.getReuseHashCode()))
						continue;

					storedSkills.add(skill.getReuseHashCode());

					if (!effect.isHerbEffect() && effect.getInUse()
					        && !skill.isToggle())
					{
						
						statement.setInt(1, getObjectId());
						statement.setInt(2, skill.getId());
						statement.setInt(3, skill.getLevel());
						statement.setInt(4, effect.getCount());
						statement.setInt(5, effect.getTime());
						
						if (_reuseTimeStamps.containsKey(Integer.valueOf(skill.getReuseHashCode())))
						{
							TimeStamp t = _reuseTimeStamps.get(skill.getReuseHashCode());
							statement.setLong(6, t.hasNotPassed() ? t.getReuse() : 0);
							statement.setDouble(7, t.hasNotPassed() ? t.getStamp() : 0);
						}
						else
						{
							statement.setLong(6, 0);
							statement.setDouble(7, 0);
						}
						
						statement.setInt(8, 0);
						statement.setInt(9, getClassIndex());
						statement.setInt(10, ++buff_index);
						statement.execute();
					}
				}
			}
			
			// Store the reuse delays of remaining skills which
			// lost effect but still under reuse delay. 'restore_type' 1.
			for (int hash : _reuseTimeStamps.keySet())
			{
				if (storedSkills.contains(hash))
					continue;

				TimeStamp t = _reuseTimeStamps.get(hash);
				if (t != null && t.hasNotPassed())
				{
					storedSkills.add(hash);

					statement.setInt(1, getObjectId());
					statement.setInt(2, t.getSkillId());
					statement.setInt(3, t.getSkillLvl());
					statement.setInt(4, -1);
					statement.setInt(5, -1);
					statement.setLong(6, t.getReuse());
					statement.setDouble(7, t.getStamp());
					statement.setInt(8, 1);
					statement.setInt(9, getClassIndex());
					statement.setInt(10, ++buff_index);
					statement.execute();
				}
			}
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Could not store char effect data: ", e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
			}
		}
	}

	/**
	 * Return True if the L2PcInstance is on line.<BR>
	 * <BR>
	 */
	public int isOnline()
	{
		return (_isOnline ? 1 : 0);
	}

    public boolean isIn7sDungeon()
    {
        return _isIn7sDungeon;
    }

	/**
	 * Add a skill to the L2PcInstance _skills and its Func objects to the calculator set of the L2PcInstance and save update in the character_skills table of the database.<BR><BR>
	 *
	 * <B><U> Concept</U> :</B><BR><BR>
	 * All skills own by a L2PcInstance are identified in <B>_skills</B><BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Replace oldSkill by newSkill or Add the newSkill </li>
	 * <li>If an old skill has been replaced, remove all its Func objects of L2Character calculator set</li>
	 * <li>Add Func objects of newSkill to the calculator set of the L2Character </li><BR><BR>
	 *
	 * @param newSkill The L2Skill to add to the L2Character
	 *
	 * @return The L2Skill replaced or null if just added a new L2Skill
	 *
	 */
	public L2Skill addSkill(L2Skill newSkill, boolean store)
	{
		// Add a skill to the L2PcInstance _skills and its Func objects to the calculator set of the L2PcInstance
		L2Skill oldSkill = super.addSkill(newSkill);

		// Add or update a L2PcInstance skill in the character_skills table of the database
		if (store) storeSkill(newSkill, oldSkill, -1);

		return oldSkill;
	}
	
	@Override
	public L2Skill removeSkill(L2Skill skill, boolean store)
	{
		if (store)
			return removeSkill(skill);
		else
			return super.removeSkill(skill, true);
	}
	
	public L2Skill removeSkill(L2Skill skill, boolean store, boolean cancelEffect)
	{
		if (store)
			return removeSkill(skill);
		else
			return super.removeSkill(skill, cancelEffect);
	}

	/**
	 * Remove a skill from the L2Character and its Func objects from calculator set of the L2Character and save update in the character_skills table of the database.<BR><BR>
	 *
	 * <B><U> Concept</U> :</B><BR><BR>
	 * All skills own by a L2Character are identified in <B>_skills</B><BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Remove the skill from the L2Character _skills </li>
	 * <li>Remove all its Func objects from the L2Character calculator set</li><BR><BR>
	 *
	 * <B><U> Overridden in </U> :</B><BR><BR>
	 * <li> L2PcInstance : Save update in the character_skills table of the database</li><BR><BR>
	 *
	 * @param skill The L2Skill to remove from the L2Character
	 *
	 * @return The L2Skill removed
	 *
	 */
	@Override
	public L2Skill removeSkill(L2Skill skill)
	{
		// Remove a skill from the L2Character and its Func objects from calculator set of the L2Character
		L2Skill oldSkill = super.removeSkill(skill);

		Connection con = null;

		try
		{
			// Remove or update a L2PcInstance skill from the character_skills table of the database
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(DELETE_SKILL_FROM_CHAR);

			if (oldSkill != null)
			{
				statement.setInt(1, oldSkill.getId());
				statement.setInt(2, getObjectId());
				statement.setInt(3, getClassIndex());
				statement.execute();
			}
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("Error could not delete skill: " + e);
		}
		finally
		{
			try { con.close(); } catch (Exception e) {}
		}

		if (this.transformId() > 0 || this.isCursedWeaponEquipped())
			return oldSkill;

		    L2ShortCut[] allShortCuts = getAllShortCuts();

		    for (L2ShortCut sc : allShortCuts)
		    {
			    if (sc != null && skill != null && sc.getId() == skill.getId() && sc.getType() == L2ShortCut.TYPE_SKILL)
			        deleteShortCut(sc.getSlot(), sc.getPage());
		    }

		return oldSkill;
	}

	/**
	 * Add or update a L2PcInstance skill in the character_skills table of the database.
	 * <BR><BR>
	 * If newClassIndex > -1, the skill will be stored with that class index, not the current one.
	 */
	private void storeSkill(L2Skill newSkill, L2Skill oldSkill, int newClassIndex)
	{
		int classIndex = _classIndex;

		if (newClassIndex > -1)
			classIndex = newClassIndex;

		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;

			if (oldSkill != null && newSkill != null)
			{
				statement = con.prepareStatement(UPDATE_CHARACTER_SKILL_LEVEL);
				statement.setInt(1, newSkill.getLevel());
				statement.setInt(2, oldSkill.getId());
				statement.setInt(3, getObjectId());
				statement.setInt(4, classIndex);
				statement.execute();
				statement.close();
			}
			else if (newSkill != null)
			{
				statement = con.prepareStatement(ADD_NEW_SKILL);
				statement.setInt(1, getObjectId());
				statement.setInt(2, newSkill.getId());
				statement.setInt(3, newSkill.getLevel());
				statement.setInt(4, classIndex);
				statement.execute();
				statement.close();
			}
			else
			{
				_log.warning("could not store new skill. its NULL");
			}
		}
		catch (Exception e)
		{
			_log.warning("Error could not store char skills: " + e);
		}
		finally
		{
			try { con.close(); } catch (Exception e) {}
		}
	}

	/**
	 * Retrieve from the database all skills of this L2PcInstance and add them to _skills.<BR><BR>
	 */
	private void restoreSkills()
	{
		Connection con = null;

		try
		{
			// Retrieve all skills of this L2PcInstance from the database
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(RESTORE_SKILLS_FOR_CHAR);
			
			statement.setInt(1, getObjectId());
			statement.setInt(2, getClassIndex());
			ResultSet rset = statement.executeQuery();

			// Go though the recordset of this SQL query
			while (rset.next())
			{
				int id = rset.getInt("skill_id");
				int level = rset.getInt("skill_level");

				if (id > 9000 && id < 9007)
					continue; // fake skills for base stats

				// Create a L2Skill object for each record
				L2Skill skill = SkillTable.getInstance().getInfo(id, level);

				// Add the L2Skill object to the L2Character _skills and its Func objects to the calculator set of the L2Character
				super.addSkill(skill);
			}

			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("Could not restore character "+this+ " skills: " + e);
		}
		finally
		{
			try { con.close(); } catch (Exception e) {}
		}
	}

    /**
     * Retrieve from the database all skill effects of this L2PcInstance and add them to the player.<BR><BR>
     */
	public void restoreEffects()
	{
		Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			ResultSet rset;

			statement = con.prepareStatement(RESTORE_SKILL_SAVE);
			statement.setInt(1, getObjectId());
			statement.setInt(2, getClassIndex());
			rset = statement.executeQuery();

			while (rset.next())
			{
				int effectCount = rset.getInt("effect_count");
				int effectCurTime = rset.getInt("effect_cur_time");
				long reuseDelay = rset.getLong("reuse_delay");
				long systime = rset.getLong("systime");
				int restoreType = rset.getInt("restore_type");

				final L2Skill skill = SkillTable.getInstance().getInfo(rset.getInt("skill_id"), rset.getInt("skill_level"));
				if (skill == null)
					continue;

				final long remainingTime = systime - System.currentTimeMillis();
				if (remainingTime > 10)
				{
					disableSkill(skill, remainingTime);
					addTimeStamp(skill, reuseDelay, systime);
				}

				/**
				 *  Restore Type 1
				 *  The remaning skills lost effect upon logout but
				 *  were still under a high reuse delay.
				 */
				if (restoreType > 0)
					continue;

				/**
				*  Restore Type 0
				*  These skill were still in effect on the character
				*  upon logout. Some of which were self casted and
				*  might still have had a long reuse delay which also
				*  is restored.
				*  
				*/
				for (L2Effect effect : skill.getEffects(this, this))
                {
					effect.setCount(effectCount);
					effect.setFirstTime(effectCurTime);
                }
			}

			rset.close();
			statement.close();

			statement = con.prepareStatement(DELETE_SKILL_SAVE);
			statement.setInt(1, getObjectId());
			statement.setInt(2, getClassIndex());
			statement.executeUpdate();
			statement.close();
		}
		catch (Exception e) {
			_log.warning("Could not restore "+this+" active effect data: " + e);
			e.printStackTrace();
		}
		finally {
			try {con.close();} catch (Exception e) {}
		}
	}

	/**
	 * Retrieve from the database all Henna of this L2PcInstance, add them to _henna and calculate stats of the L2PcInstance.<BR><BR>
	 */
	private void restoreHenna()
	{
		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(RESTORE_CHAR_HENNAS);
			statement.setInt(1, getObjectId());
			statement.setInt(2, getClassIndex());
			ResultSet rset = statement.executeQuery();

			for (int i=0;i<3;i++)
				_henna[i]=null;

			while (rset.next())
			{
				int slot = rset.getInt("slot");

				if (slot<1 || slot>3)
                    continue;

				int symbol_id = rset.getInt("symbol_id");

				L2HennaInstance sym = null;

				if (symbol_id != 0)
				{
					L2Henna tpl = HennaTable.getInstance().getTemplate(symbol_id);

					if (tpl != null)
					{
						sym = new L2HennaInstance(tpl);
						_henna[slot-1] = sym;
					}
				}
			}

			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
		    _log.log(Level.SEVERE, "Failed restoing character "+this+" hennas.", e);
		}
		finally
		{
			try { con.close(); } catch (Exception e) {}
		}

		// Calculate Henna modifiers of this L2PcInstance
		recalcHennaStats();
	}

	/**
	 * Retrieve from the database all Recommendation data of this L2PcInstance, add to _recomChars and calculate stats of the L2PcInstance.<BR><BR>
	 */
	private void restoreRecom()
	{
		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(RESTORE_CHAR_RECOMS);
			statement.setInt(1, getObjectId());
			ResultSet rset = statement.executeQuery();
			while (rset.next())
			{
				_recomChars.add(rset.getInt("target_id"));
			}

			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Failed restoring character "+this+" recommendations.", e);
		}
		finally
		{
			try { con.close(); } catch (Exception e) {}
		}
	}

	/**
	 * Return the number of Henna empty slot of the L2PcInstance.<BR><BR>
	 */
	public int getHennaEmptySlots()
	{
		int totalSlots = 0;
		if (getClassId().level() == 1)
			totalSlots = 2;
		else
			totalSlots = 3;

		for (int i = 0; i < 3; i++)
		{
			if (_henna[i] != null)
				totalSlots--;
		}

		if (totalSlots <= 0)
			return 0;

		return totalSlots;
	}

	/**
	 * Remove a Henna of the L2PcInstance, save update in the character_hennas table of the database and send Server->Client HennaInfo/UserInfo packet to this L2PcInstance.<BR><BR>
	 */
	public boolean removeHenna(int slot)
	{
		if (slot < 1 || slot > 3)
			return false;

		slot--;

		if (_henna[slot] == null)
			return false;

		L2HennaInstance henna = _henna[slot];
		_henna[slot] = null;

		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(DELETE_CHAR_HENNA);

			statement.setInt(1, getObjectId());
			statement.setInt(2, slot+1);
			statement.setInt(3, getClassIndex());

			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Failed remocing character henna.", e);
		}
		finally
		{
			try { con.close(); } catch (Exception e) {}
		}

		// Calculate Henna modifiers of this L2PcInstance
		recalcHennaStats();

		// Send Server->Client HennaInfo packet to this L2PcInstance
		sendPacket(new HennaInfo(this));

		// Send Server->Client UserInfo packet to this L2PcInstance
		sendPacket(new UserInfo(this));
		sendPacket(new ExBrExtraUserInfo(this));
		// Add the recovered dyes to the player's inventory and notify them.
		getInventory().addItem("Henna", henna.getItemIdDye(), henna.getAmountDyeRequire() / 2, this, null);

		reduceAdena("Henna", henna.getPrice() / 5, this, false);

		SystemMessage sm = new SystemMessage(SystemMessageId.EARNED_S2_S1_S);
        sm.addItemName(henna.getItemIdDye());
        sm.addItemNumber(henna.getAmountDyeRequire() / 2);
		sendPacket(sm);

		sendPacket(new SystemMessage(SystemMessageId.SYMBOL_DELETED));

		return true;
	}

	/**
	 * Add a Henna to the L2PcInstance, save update in the character_hennas table of the database and send Server->Client HennaInfo/UserInfo packet to this L2PcInstance.<BR><BR>
	 */
	public boolean addHenna(L2HennaInstance henna)
	{
		for (int i = 0; i < 3; i++)
		{
			if (_henna[i] == null)
			{
				_henna[i] = henna;

				// Calculate Henna modifiers of this L2PcInstance
				recalcHennaStats();

				Connection con = null;

				try
				{
					con = L2DatabaseFactory.getInstance().getConnection();
					PreparedStatement statement = con.prepareStatement(ADD_CHAR_HENNA);
					
					statement.setInt(1, getObjectId());
					statement.setInt(2, henna.getSymbolId());
					statement.setInt(3, i+1);
					statement.setInt(4, getClassIndex());
					
					statement.execute();
					statement.close();
				}
				catch (Exception e)
				{
					_log.log(Level.SEVERE, "Failed saving character henna.", e);
				}
				finally
				{
					try { con.close(); } catch (Exception e) {}
				}

				// Send Server->Client HennaInfo packet to this L2PcInstance
				sendPacket(new HennaInfo(this));

				// Send Server->Client UserInfo packet to this L2PcInstance
				sendPacket(new UserInfo(this));
				sendPacket(new ExBrExtraUserInfo(this));

				return true;
			}
		}
		return false;
	}

	/**
	 * Calculate Henna modifiers of this L2PcInstance.<BR><BR>
	 */
	private void recalcHennaStats()
	{
		_hennaINT = 0;
		_hennaSTR = 0;
		_hennaCON = 0;
		_hennaMEN = 0;
		_hennaWIT = 0;
		_hennaDEX = 0;

		for (int i=0;i<3;i++)
		{
			if (_henna[i]==null)continue;
			_hennaINT += _henna[i].getStatINT();
			_hennaSTR += _henna[i].getStatSTR();
			_hennaMEN += _henna[i].getStatMEM();
			_hennaCON += _henna[i].getStatCON();
			_hennaWIT += _henna[i].getStatWIT();
			_hennaDEX += _henna[i].getStatDEX();
		}

		if (_hennaINT>5)_hennaINT=5;
		if (_hennaSTR>5)_hennaSTR=5;
		if (_hennaMEN>5)_hennaMEN=5;
		if (_hennaCON>5)_hennaCON=5;
		if (_hennaWIT>5)_hennaWIT=5;
		if (_hennaDEX>5)_hennaDEX=5;
	}

	/**
	 * Return the Henna of this L2PcInstance corresponding to the selected slot.<BR><BR>
	 */
	public L2HennaInstance getHenna(int slot)
	{
		if (slot < 1 || slot > 3)
            return null;

		return _henna[slot - 1];
	}

	/**
	 * Return the INT Henna modifier of this L2PcInstance.<BR><BR>
	 */
	public int getHennaStatINT()
	{
		return _hennaINT;
	}

	/**
	 * Return the STR Henna modifier of this L2PcInstance.<BR><BR>
	 */
	public int getHennaStatSTR()
	{
		return _hennaSTR;
	}

	/**
	 * Return the CON Henna modifier of this L2PcInstance.<BR><BR>
	 */
	public int getHennaStatCON()
	{
		return _hennaCON;
	}

	/**
	 * Return the MEN Henna modifier of this L2PcInstance.<BR><BR>
	 */
	public int getHennaStatMEN()
	{
		return _hennaMEN;
	}

	/**
	 * Return the WIT Henna modifier of this L2PcInstance.<BR><BR>
	 */
	public int getHennaStatWIT()
	{
		return _hennaWIT;
	}

	/**
	 * Return the DEX Henna modifier of this L2PcInstance.<BR><BR>
	 */
	public int getHennaStatDEX()
	{
		return _hennaDEX;
	}

	/**
	 * Return True if the L2PcInstance is autoAttackable.<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Check if the attacker isn't the L2PcInstance Pet </li>
	 * <li>Check if the attacker is L2MonsterInstance</li>
	 * <li>If the attacker is a L2PcInstance, check if it is not in the same party </li>
	 * <li>Check if the L2PcInstance has Karma </li>
	 * <li>If the attacker is a L2PcInstance, check if it is not in the same siege clan (Attacker, Defender) </li><BR><BR>
	 *
	 */
	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		// Check if the attacker isn't the L2PcInstance Pet
		if (attacker == this || attacker == getPet())
			return false;

		// TODO: check for friendly mobs
		// Check if the attacker is a L2MonsterInstance
		if (attacker instanceof L2MonsterInstance)
			return true;

		// Check if the attacker is not in the same party
		if (getParty() != null && getParty().getPartyMembers().contains(attacker))
			return false;

		// Check if the attacker is in olympia and olympia start
		if (attacker instanceof L2PcInstance && ((L2PcInstance)attacker).isInOlympiadMode() ){
			if (isInOlympiadMode() && isOlympiadStart() && ((L2PcInstance)attacker).getOlympiadGameId()==getOlympiadGameId())
				return true;
			else
				return false;
		}

		// Check if the attacker is in TvT and TvT is started
		if (TvTEvent.isStarted() && TvTEvent.isPlayerParticipant(getObjectId()))
			return true;

		// Check if the attacker is not in the same clan
		if (getClan() != null && attacker != null && getClan().isMember(attacker.getObjectId()))
			return false;

        if(attacker instanceof L2Playable && isInsideZone(ZONE_PEACE))
            return false;

		// Check if the L2PcInstance has Karma
		if (getKarma() > 0 || getPvpFlag() > 0)
			return true;

		// Check if the attacker is a L2Playable
		if (attacker instanceof L2Playable)
		{
			// Get L2PcInstance
			L2PcInstance cha = attacker.getActingPlayer();
			
			// is AutoAttackable if both players are in the same duel and the duel is still going on
			if ( getDuelState() == Duel.DUELSTATE_DUELLING
					&& getDuelId() == cha.getDuelId())
				return true;
			// Check if the L2PcInstance is in an arena or a siege area
			if (isInsideZone(ZONE_PVP) && cha.isInsideZone(ZONE_PVP))
				return true;

			if (getClan() != null)
			{
				Siege siege = SiegeManager.getInstance().getSiege(getX(), getY(), getZ());
				if (siege != null)
				{
					// Check if a siege is in progress and if attacker and the L2PcInstance aren't in the Defender clan
					if (siege.checkIsDefender(cha.getClan()) &&
							siege.checkIsDefender(getClan()))
						return false;

					// Check if a siege is in progress and if attacker and the L2PcInstance aren't in the Attacker clan
					if (siege.checkIsAttacker(cha.getClan()) &&
							siege.checkIsAttacker(getClan()))
						return false;
				}

				// Check if clan is at war
				if (getClan() != null && cha.getClan() != null
				                           && getClan().isAtWarWith(cha.getClanId())
				                           && cha.getClan().isAtWarWith(getClanId())
				                           && getWantsPeace() == 0
				                           && cha.getWantsPeace() == 0
				                           && !isAcademyMember())
				return true;
			}
		}
		else if (attacker instanceof L2DefenderInstance)
		{
			if (getClan() != null)
			{
				Siege siege = SiegeManager.getInstance().getSiege(this);
				return (siege != null && siege.checkIsAttacker(getClan()));
			}
		}

		return false;
	}


	/**
	 * Check if the active L2Skill can be casted.<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Check if the skill isn't toggle and is offensive </li>
	 * <li>Check if the target is in the skill cast range </li>
	 * <li>Check if the skill is Spoil type and if the target isn't already spoiled </li>
	 * <li>Check if the caster owns enought consummed Item, enough HP and MP to cast the skill </li>
	 * <li>Check if the caster isn't sitting </li>
	 * <li>Check if all skills are enabled and this skill is enabled </li><BR><BR>
	 * <li>Check if the caster own the weapon needed </li><BR><BR>
	 * <li>Check if the skill is active </li><BR><BR>
	 * <li>Check if all casting conditions are completed</li><BR><BR>
	 * <li>Notify the AI with AI_INTENTION_CAST and target</li><BR><BR>
	 *
	 * @param skill The L2Skill to use
	 * @param forceUse used to force ATTACK on players
	 * @param dontMove used to prevent movement, if not in range
	 *
	 */
	public void useMagic(L2Skill skill, boolean forceUse, boolean dontMove)
	{
		// Check if the skill is active
        if (skill.isPassive())
        {
            // just ignore the passive skill request. why does the client send it anyway ??
            // Send a Server->Client packet ActionFailed to the L2PcInstance
            sendPacket(ActionFailed.STATIC_PACKET);
            return;
        }
		
		//************************************* Check Casting in Progress *******************************************

		// If a skill is currently being used, queue this one if this is not the same
        if (isCastingNow())
        {
        	SkillDat currentSkill = getCurrentSkill();
        	// Check if new skill different from current skill in progress
        	if (currentSkill != null && skill.getId() == currentSkill.getSkillId())
            {
            	sendPacket(ActionFailed.STATIC_PACKET);
            	return;
            }

            if (Config.DEBUG && getQueuedSkill() != null)
                _log.info(getQueuedSkill().getSkill().getName() + " is already queued for " + getName() + ".");

            // Create a new SkillDat object and queue it in the player _queuedSkill
            setQueuedSkill(skill, forceUse, dontMove);
            sendPacket(ActionFailed.STATIC_PACKET);
            return;
        }
        setIsCastingNow(true);
        // Create a new SkillDat object and set the player _currentSkill
        // This is used mainly to save & queue the button presses, since L2Character has
        // _lastSkillCast which could otherwise replace it
        setCurrentSkill(skill, forceUse, dontMove);

        if (getQueuedSkill() != null) // wiping out previous values, after casting has been aborted
        	setQueuedSkill(null, false, false);
        
        if (!checkUseMagicConditions(skill, forceUse, dontMove))
        {
        	setIsCastingNow(false);
        	return;
        }
        
		// Check if the target is correct and Notify the AI with AI_INTENTION_CAST and target
		L2Object target = null;

		switch (skill.getTargetType())
		{
			case TARGET_AURA:    // AURA, SELF should be cast even if no target has been found
			case TARGET_FRONT_AURA:
			case TARGET_BEHIND_AURA:
			case TARGET_GROUND:
			case TARGET_SELF:
				target = this;
				break;
			default:

                // Get the first target of the list
			    target = skill.getFirstOfTargetList(this);
			    break;
		}

		// Notify the AI with AI_INTENTION_CAST and target
		getAI().setIntention(CtrlIntention.AI_INTENTION_CAST, skill, target);
    }
	
	private boolean checkUseMagicConditions(L2Skill skill, boolean forceUse, boolean dontMove)
	{
		L2SkillType sklType = skill.getSkillType();
	    
        //************************************* Check Player State *******************************************

        // Abnormal effects(ex : Stun, Sleep...) are checked in L2Character useMagic()

      	if (isOutOfControl())
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}

    	// Check if the player is dead
        if (isDead())
        {
        	abortCast();
        	// Send a Server->Client packet ActionFailed to the L2PcInstance
            sendPacket(ActionFailed.STATIC_PACKET);
            return false;
        }
        
        if (isFishing() && (sklType != L2SkillType.PUMPING &&
        		sklType != L2SkillType.REELING && sklType != L2SkillType.FISHING))
        {
            //Only fishing skills are available
            sendPacket(new SystemMessage(SystemMessageId.ONLY_FISHING_SKILLS_NOW));
            return false;
        }
        
        if (inObserverMode())
        {
        	sendPacket(new SystemMessage(SystemMessageId.OBSERVERS_CANNOT_PARTICIPATE));
            abortCast();
            sendPacket(ActionFailed.STATIC_PACKET);
            return false;
        }
        
        // Check if the caster is sitting
        if (isSitting())
        {
            // Send a System Message to the caster
            sendPacket(new SystemMessage(SystemMessageId.CANT_MOVE_SITTING));

            // Send a Server->Client packet ActionFailed to the L2PcInstance
            sendPacket(ActionFailed.STATIC_PACKET);
            return false;
        }

		// Check if the skill type is TOGGLE
		if (skill.isToggle())
		{
			// Get effects of the skill
			L2Effect effect = getFirstEffect(skill.getId());

			if (effect != null)
			{
				effect.exit();

				// Send a Server->Client packet ActionFailed to the L2PcInstance
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
		}
		
    	// Check if the player uses "Fake Death" skill 
		// Note: do not check this before TOGGLE reset
        if (isFakeDeath())
        {
        	// Send a Server->Client packet ActionFailed to the L2PcInstance
            sendPacket(ActionFailed.STATIC_PACKET);
            return false;
        }

	    //************************************* Check Target *******************************************
        // Create and set a L2Object containing the target of the skill
        L2Object target = null;
        SkillTargetType sklTargetType = skill.getTargetType();
        Point3D worldPosition = getCurrentSkillWorldPosition();
        
        if (sklTargetType == SkillTargetType.TARGET_GROUND && worldPosition == null)
        {
        	_log.info("WorldPosition is null for skill: "+skill.getName() + ", player: " + getName() + ".");
        	sendPacket(ActionFailed.STATIC_PACKET);
        	return false;
        }
        
        switch (sklTargetType)
        {
        	// Target the player if skill type is AURA, PARTY, CLAN or SELF
        	case TARGET_AURA:
        	case TARGET_FRONT_AURA:
        	case TARGET_BEHIND_AURA:
        	case TARGET_PARTY:
        	case TARGET_ALLY:
        	case TARGET_CLAN:
        	case TARGET_PARTY_CLAN:
        	case TARGET_GROUND:
        	case TARGET_SELF:
        		target = this;
        		break;
        	case TARGET_PET:
        	case TARGET_SUMMON:
        		target = getPet();
        		break;
        	default:
        		target = getTarget();
        	break;
        }

        // Check the validity of the target
        if (target == null)
        {
            sendPacket(ActionFailed.STATIC_PACKET);
            return false;
        }

        // skills can be used on Walls and Doors only during siege
        if(target instanceof L2DoorInstance )
        {
        	boolean isCastle = (((L2DoorInstance) target).getCastle() != null
                    && ((L2DoorInstance) target).getCastle().getCastleId() > 0
                    && ((L2DoorInstance) target).getCastle().getSiege().getIsInProgress());
        	boolean isFort = (((L2DoorInstance) target).getFort() != null
                    && ((L2DoorInstance) target).getFort().getFortId() > 0
                    && ((L2DoorInstance) target).getFort().getSiege().getIsInProgress()
                    && !((L2DoorInstance) target).getIsCommanderDoor());
        	if ((!isCastle && !isFort)&&(((L2DoorInstance) target).isUnlockable() && skill.getSkillType() != L2SkillType.UNLOCK))
        		return false;
        }

        // Are the target and the player in the same duel?
        if (isInDuel())
        {        	
        	// Get L2PcInstance
    		if (target instanceof L2Playable)
    		{
    			// Get L2PcInstance
    			L2PcInstance cha = target.getActingPlayer();
    			if (cha.getDuelId() != getDuelId())
    			{
            		sendMessage("You cannot do this while duelling.");
            		sendPacket(ActionFailed.STATIC_PACKET);
            		return false;    				
    			}
    		}
        }

        //************************************* Check skill availability *******************************************

  		// Check if it's ok to summon
        // siege golem (13), Wild Hog Cannon (299), Swoop Cannon (448)
        if ((skill.getId() == 13 || skill.getId() == 299 || skill.getId() == 448)
        		&& ((!SiegeManager.getInstance().checkIfOkToSummon(this, false) 
                && !FortSiegeManager.getInstance().checkIfOkToSummon(this, false))||(SevenSigns.getInstance().checkSummonConditions(this))))
			return false;
        
        // Check if this skill is enabled (ex : reuse time)
        if (isSkillDisabled(skill))
        {
        	SystemMessage sm = null;
				
        	if (_reuseTimeStamps.containsKey(Integer.valueOf(skill.getReuseHashCode())))
        	{
        		int remainingTime = (int)(_reuseTimeStamps.get(Integer.valueOf(skill.getReuseHashCode())).getRemaining()/1000);
        		int hours = remainingTime/3600;
        		int minutes = (remainingTime%3600)/60;
        		int seconds = (remainingTime%60);
        		if (hours > 0)
        		{
        			sm = new SystemMessage(SystemMessageId.S2_HOURS_S3_MINUTES_S4_SECONDS_REMAINING_FOR_REUSE_S1);
        			sm.addSkillName(skill);
        			sm.addNumber(hours);
        			sm.addNumber(minutes);
        		}
        		else if (minutes > 0)
        		{
        			sm = new SystemMessage(SystemMessageId.S2_MINUTES_S3_SECONDS_REMAINING_FOR_REUSE_S1);
        			sm.addSkillName(skill);
        			sm.addNumber(minutes);
        		}
        		else
        		{
        			sm = new SystemMessage(SystemMessageId.S2_SECONDS_REMAINING_FOR_REUSE_S1);
        			sm.addSkillName(skill);
        		}
					
        		sm.addNumber(seconds);
        	}
        	else
        	{
        		sm = new SystemMessage(SystemMessageId.S1_PREPARED_FOR_REUSE);
        		sm.addSkillName(skill);
        	}
				
        	sendPacket(sm);
        	return false;
        }

        //************************************* Check Consumables *******************************************

        // Check if spell consumes a Soul
        if (skill.getSoulConsumeCount() > 0)
        {
            if (getSouls() < skill.getSoulConsumeCount())
            {
                sendPacket(new SystemMessage(SystemMessageId.THERE_IS_NOT_ENOUGH_SOUL));
                sendPacket(ActionFailed.STATIC_PACKET);
                return false;
            }
        }
        //************************************* Check casting conditions *******************************************

        // Check if all casting conditions are completed
        if (!skill.checkCondition(this, target, false))
        {
        	// Send a Server->Client packet ActionFailed to the L2PcInstance
        	sendPacket(ActionFailed.STATIC_PACKET);
        	return false;
        }

        //************************************* Check Skill Type *******************************************

        // Check if this is offensive magic skill
        if (skill.isOffensive())
		{
			if ((isInsidePeaceZone(this, target)) && !getAccessLevel().allowPeaceAttack())
			{
				// If L2Character or target is in a peace zone, send a system message TARGET_IN_PEACEZONE a Server->Client packet ActionFailed
				sendPacket(new SystemMessage(SystemMessageId.TARGET_IN_PEACEZONE));
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}

			if (isInOlympiadMode() && !isOlympiadStart()){
				// if L2PcInstance is in Olympia and the match isn't already start, send a Server->Client packet ActionFailed
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}

			if (target.getActingPlayer() != null && getSiegeState() > 0 && isInsideZone(L2Character.ZONE_SIEGE)
					&& target.getActingPlayer().getSiegeState() == getSiegeState()
					&& target.getActingPlayer() != this && target.getActingPlayer().getSiegeSide() == getSiegeSide())
			{
				// 
				if (TerritoryWarManager.getInstance().isTWInProgress())
					sendPacket(new SystemMessage(SystemMessageId.YOU_CANNOT_ATTACK_A_MEMBER_OF_THE_SAME_TERRITORY));
				else
					sendPacket(new SystemMessage(SystemMessageId.FORCED_ATTACK_IS_IMPOSSIBLE_AGAINST_SIEGE_SIDE_TEMPORARY_ALLIED_MEMBERS));
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}

            // Check if the target is attackable
            if (!target.isAttackable() && !getAccessLevel().allowPeaceAttack())
			{
				// If target is not attackable, send a Server->Client packet ActionFailed
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}

			// Check if a Forced ATTACK is in progress on non-attackable target
			if (!target.isAutoAttackable(this) && !forceUse)
			{
				switch (sklTargetType)
				{
					case TARGET_AURA:
					case TARGET_FRONT_AURA:
					case TARGET_BEHIND_AURA:
					case TARGET_CLAN:
					case TARGET_PARTY_CLAN:
					case TARGET_ALLY:
					case TARGET_PARTY:
					case TARGET_SELF:
					case TARGET_GROUND:
						break;
					default: // Send a Server->Client packet ActionFailed to the L2PcInstance
						sendPacket(ActionFailed.STATIC_PACKET);
						return false;
				}
			}

			// Check if the target is in the skill cast range
			if (dontMove)
			{
				// Calculate the distance between the L2PcInstance and the target
				if (sklTargetType == SkillTargetType.TARGET_GROUND)
				{
					if (!isInsideRadius(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), skill.getCastRange()+getTemplate().collisionRadius, false, false))
					{
						// Send a System Message to the caster
						sendPacket(new SystemMessage(SystemMessageId.TARGET_TOO_FAR));

						// Send a Server->Client packet ActionFailed to the L2PcInstance
						sendPacket(ActionFailed.STATIC_PACKET);
						return false;
					}
				}
				else if (skill.getCastRange() > 0 && !isInsideRadius(target, skill.getCastRange()+getTemplate().collisionRadius, false, false))
				{
					// Send a System Message to the caster
					sendPacket(new SystemMessage(SystemMessageId.TARGET_TOO_FAR));

					// Send a Server->Client packet ActionFailed to the L2PcInstance
					sendPacket(ActionFailed.STATIC_PACKET);
					return false;
				}
			}
		}
        
        if(skill.getSkillType() == L2SkillType.INSTANT_JUMP)
        {
        	// You cannot jump while rooted right ;)
        	if(isMovementDisabled())
        	{
        		// Sends message that skill cannot be used...
        		SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
        		sm.addSkillName(skill.getId());
        		sendPacket(sm);
        		
        		// Send a Server->Client packet ActionFailed to the L2PcInstance
        		sendPacket(ActionFailed.STATIC_PACKET);
        		
				return false;
        	}
        	// And this skill cannot be used in peace zone, not even on NPCs!
        	if(this.isInsideZone(L2Character.ZONE_PEACE))
        	{
        		//Sends a sys msg to client
        		sendPacket(new SystemMessage(SystemMessageId.TARGET_IN_PEACEZONE));
        		
        		// Send a Server->Client packet ActionFailed to the L2PcInstance
				sendPacket(ActionFailed.STATIC_PACKET);
				
				return false;
        	}
        	
        }
		// Check if the skill is defensive
        if (!skill.isOffensive() && target instanceof L2MonsterInstance && !forceUse && !skill.isNeutral())
		{
			// check if the target is a monster and if force attack is set.. if not then we don't want to cast.
        	switch (sklTargetType)
			{
				case TARGET_PET:
				case TARGET_SUMMON:
				case TARGET_AURA:
				case TARGET_FRONT_AURA:
				case TARGET_BEHIND_AURA:
				case TARGET_CLAN:
				case TARGET_PARTY_CLAN:
				case TARGET_SELF:
				case TARGET_PARTY:
				case TARGET_ALLY:
				case TARGET_CORPSE_MOB:
				case TARGET_AREA_CORPSE_MOB:
				case TARGET_GROUND:
					break;
				default:
				{
					switch (sklType)
					{
						case BEAST_FEED:
						case DELUXE_KEY_UNLOCK:
						case UNLOCK:
							break;
						default:
							sendPacket(ActionFailed.STATIC_PACKET);
						return false;
					}
					break;
				}
			}
		}

		// Check if the skill is Spoil type and if the target isn't already spoiled
		if (sklType == L2SkillType.SPOIL)
		{
			if (!(target instanceof L2MonsterInstance))
			{
				// Send a System Message to the L2PcInstance
				sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));

				// Send a Server->Client packet ActionFailed to the L2PcInstance
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
		}
		
		// Check if the skill is Sweep type and if conditions not apply
		if (sklType == L2SkillType.SWEEP && target instanceof L2Attackable)
		{
			int spoilerId = ((L2Attackable) target).getIsSpoiledBy();
			
			if (((L2Attackable) target).isDead())
			{
				if (!((L2Attackable) target).isSpoil())
				{
					// Send a System Message to the L2PcInstance
					sendPacket(new SystemMessage(SystemMessageId.SWEEPER_FAILED_TARGET_NOT_SPOILED));
					
					// Send a Server->Client packet ActionFailed to the L2PcInstance
					sendPacket(ActionFailed.STATIC_PACKET);
					return false;
				}
				
				if (getObjectId() != spoilerId && !isInLooterParty(spoilerId))
				{
					// Send a System Message to the L2PcInstance
					sendPacket(new SystemMessage(SystemMessageId.SWEEP_NOT_ALLOWED));
					
					// Send a Server->Client packet ActionFailed to the L2PcInstance
					sendPacket(ActionFailed.STATIC_PACKET);
					return false;
				}
			}
		}

		// Check if the skill is Drain Soul (Soul Crystals) and if the target is a MOB
		if (sklType == L2SkillType.DRAIN_SOUL)
		{
			if (!(target instanceof L2MonsterInstance))
			{
				// Send a System Message to the L2PcInstance
				sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));

				// Send a Server->Client packet ActionFailed to the L2PcInstance
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
		}

		// Check if this is a Pvp skill and target isn't a non-flagged/non-karma player
		switch (sklTargetType)
		{
			case TARGET_PARTY:
			case TARGET_ALLY:   // For such skills, checkPvpSkill() is called from L2Skill.getTargetList()
			case TARGET_CLAN:   // For such skills, checkPvpSkill() is called from L2Skill.getTargetList()
			case TARGET_PARTY_CLAN:   // For such skills, checkPvpSkill() is called from L2Skill.getTargetList()
			case TARGET_AURA:
			case TARGET_FRONT_AURA:
			case TARGET_BEHIND_AURA:
			case TARGET_GROUND:
			case TARGET_SELF:
				break;
			default:
				if (!checkPvpSkill(target, skill) && !getAccessLevel().allowPeaceAttack())
				{
					// Send a System Message to the L2PcInstance
					sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));

					// Send a Server->Client packet ActionFailed to the L2PcInstance
					sendPacket(ActionFailed.STATIC_PACKET);
					return false;
				}
		}

		// TODO: Unhardcode skillId 844 which is the outpost construct skill
		if ((sklTargetType == SkillTargetType.TARGET_HOLY && !checkIfOkToCastSealOfRule(CastleManager.getInstance().getCastle(this), false, skill))
				|| (sklTargetType == SkillTargetType.TARGET_FLAGPOLE && !checkIfOkToCastFlagDisplay(FortManager.getInstance().getFort(this), false, skill))
				|| (sklType == L2SkillType.SIEGEFLAG && !L2SkillSiegeFlag.checkIfOkToPlaceFlag(this, false, skill.getId() == 844))
				|| (sklType == L2SkillType.STRSIEGEASSAULT && !checkIfOkToUseStriderSiegeAssault())
				|| (sklType == L2SkillType.SUMMON_FRIEND && !(checkSummonerStatus(this) && checkSummonTargetStatus(target, this))))
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			abortCast();
			return false;
		}

		// GeoData Los Check here
		if (skill.getCastRange() > 0)
		{
			if (sklTargetType == SkillTargetType.TARGET_GROUND)
			{
				if (!GeoData.getInstance().canSeeTarget(this, worldPosition))
				{
					sendPacket(new SystemMessage(SystemMessageId.CANT_SEE_TARGET));
					sendPacket(ActionFailed.STATIC_PACKET);
					return false;
				}
			}
			else if (!GeoData.getInstance().canSeeTarget(this, target))
			{
				sendPacket(new SystemMessage(SystemMessageId.CANT_SEE_TARGET));
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
		}
		// finally, after passing all conditions
		return true;
	}
	
	public boolean checkIfOkToUseStriderSiegeAssault()
	{
		Castle castle = CastleManager.getInstance().getCastle(this);
		Fort fort = FortManager.getInstance().getFort(this);
		
		if ((castle == null) && (fort == null))
			return false;
		
		if (castle != null)
			return checkIfOkToUseStriderSiegeAssault(castle);
		else
			return checkIfOkToUseStriderSiegeAssault(fort);
		
	}
	
	public boolean checkIfOkToUseStriderSiegeAssault(Castle castle)
	{
		String text = "";
		
		if (castle == null || castle.getCastleId() <= 0)
			text = "You must be on castle ground to use strider siege assault";
		else if (!castle.getSiege().getIsInProgress())
			text = "You can only use strider siege assault during a siege.";
		else if (!(getTarget() instanceof L2DoorInstance))
			text = "You can only use strider siege assault on doors and walls.";
		else if (!isRidingStrider())
			text = "You can only use strider siege assault when on strider.";
		else
			return true;
		
		sendMessage(text);
		
		return false;
	}
	
	public boolean checkIfOkToUseStriderSiegeAssault(Fort fort)
	{
		String text = "";
		
		if (fort == null || fort.getFortId() <= 0)
			text = "You must be on fort ground to use strider siege assault";
		else if (!fort.getSiege().getIsInProgress())
			text = "You can only use strider siege assault during a siege.";
		else if (!(getTarget() instanceof L2DoorInstance))
			text = "You can only use strider siege assault on doors and walls.";
		else if (!isRidingStrider())
			text = "You can only use strider siege assault when on strider.";
		else
			return true;
		
		sendMessage(text);
		
		return false;
	}
	
	public boolean checkIfOkToCastSealOfRule(Castle castle, boolean isCheckOnly, L2Skill skill)
	{
		SystemMessage sm;
	
		if (castle == null || castle.getCastleId() <= 0)
		{
			sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
			sm.addSkillName(skill);
		}
		else if (!castle.getArtefacts().contains(getTarget()))
		{
			sm = new SystemMessage(SystemMessageId.INCORRECT_TARGET);
		}
		else if (!castle.getSiege().getIsInProgress())
		{
			sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
			sm.addSkillName(skill);
		}
		else if (!Util.checkIfInRange(200, this, getTarget(), true))
		{
			sm = new SystemMessage(SystemMessageId.DIST_TOO_FAR_CASTING_STOPPED);
		}
		else if (castle.getSiege().getAttackerClan(getClan()) == null)
		{
			sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
			sm.addSkillName(skill);
		}
		else
		{
			if (!isCheckOnly)
			{
				sm = new SystemMessage(SystemMessageId.OPPONENT_STARTED_ENGRAVING);
				castle.getSiege().announceToPlayer(sm, false);
			}
			return true;
		}
		
		sendPacket(sm);
		return false;
	}
	
	public boolean checkIfOkToCastFlagDisplay(Fort fort, boolean isCheckOnly,L2Skill skill)
	{
		SystemMessage sm;
	
		if (fort == null || fort.getFortId() <= 0)
		{
			sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
			sm.addSkillName(skill);
		}
		else if (fort.getFlagPole() != getTarget())
		{
			sm = new SystemMessage(SystemMessageId.INCORRECT_TARGET);
			sm.addSkillName(skill);
		}
		else if (!fort.getSiege().getIsInProgress())
		{
			sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
			sm.addSkillName(skill);
		}
		else if (!Util.checkIfInRange(200, this, getTarget(), true))
		{
			sm = new SystemMessage(SystemMessageId.DIST_TOO_FAR_CASTING_STOPPED);
		}
		else if (fort.getSiege().getAttackerClan(getClan()) == null)
		{
			sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
			sm.addSkillName(skill);
		}
		else
		{
			if (!isCheckOnly)
				fort.getSiege().announceToPlayer(new SystemMessage(SystemMessageId.S1_TRYING_RAISE_FLAG),getClan().getName());
			return true;
		}
		
		sendPacket(sm);
		return false;
	}

    public boolean isInLooterParty(int LooterId)
    {
    	L2PcInstance looter = (L2PcInstance)L2World.getInstance().findObject(LooterId);

    	// if L2PcInstance is in a CommandChannel
    	if (isInParty() && getParty().isInCommandChannel() && looter != null)
    		return getParty().getCommandChannel().getMembers().contains(looter);

    	if (isInParty() && looter != null)
            return getParty().getPartyMembers().contains(looter);

		return false;
    }

	/**
	 * Check if the requested casting is a Pc->Pc skill cast and if it's a valid pvp condition
	 * @param target L2Object instance containing the target
	 * @param skill L2Skill instance with the skill being casted
	 * @return False if the skill is a pvpSkill and target is not a valid pvp target
	 */
	public boolean checkPvpSkill(L2Object target, L2Skill skill)
	{
		return checkPvpSkill(target, skill, false);
	}
	
	/**
	 * Check if the requested casting is a Pc->Pc skill cast and if it's a valid pvp condition
	 * @param target L2Object instance containing the target
	 * @param skill L2Skill instance with the skill being casted
	 * @param srcIsSummon is L2Summon - caster?
	 * @return False if the skill is a pvpSkill and target is not a valid pvp target
	 */
	public boolean checkPvpSkill(L2Object target, L2Skill skill, boolean srcIsSummon)
	{
		// check for PC->PC Pvp status
		if(target instanceof L2Summon)
			target = target.getActingPlayer();
		if (
				target != null &&                                           			// target not null and
				target != this &&                                           			// target is not self and
				target instanceof L2PcInstance &&                           			// target is L2PcInstance and
				!(isInDuel() && ((L2PcInstance)target).getDuelId() == getDuelId()) &&	// self is not in a duel and attacking opponent
				!isInsideZone(ZONE_PVP) &&        						// Pc is not in PvP zone
				!((L2PcInstance)target).isInsideZone(ZONE_PVP)         	// target is not in PvP zone
		)
		{
			if(skill.isPvpSkill()) // pvp skill
			{
				if(getClan() != null && ((L2PcInstance)target).getClan() != null)
				{
					if(getClan().isAtWarWith(((L2PcInstance)target).getClan().getClanId()) && ((L2PcInstance)target).getClan().isAtWarWith(getClan().getClanId()))
						return true; // in clan war player can attack whites even with sleep etc.
				}
				if (
						((L2PcInstance)target).getPvpFlag() == 0 &&             //   target's pvp flag is not set and
						((L2PcInstance)target).getKarma() == 0                  //   target has no karma
					)
					return false;
			}
			else if ((getCurrentSkill() != null && !getCurrentSkill().isCtrlPressed() && skill.isOffensive() && !srcIsSummon) 
					|| (getCurrentPetSkill() != null && !getCurrentPetSkill().isCtrlPressed() && skill.isOffensive() && srcIsSummon))
			{
				if(getClan() != null && ((L2PcInstance)target).getClan() != null)
				{
					if(getClan().isAtWarWith(((L2PcInstance)target).getClan().getClanId()) && ((L2PcInstance)target).getClan().isAtWarWith(getClan().getClanId()))
						return true; // in clan war player can attack whites even without ctrl
				}
				if (
						((L2PcInstance)target).getPvpFlag() == 0 &&             //   target's pvp flag is not set and
						((L2PcInstance)target).getKarma() == 0                  //   target has no karma
					)
					return false;
			}
		}

		return true;
	}

	/**
	 * Return True if the L2PcInstance is a Mage.<BR><BR>
	 */
	public boolean isMageClass()
	{
		return getClassId().isMage();
	}


	public boolean isMounted()
	{
		return _mountType > 0;
	}

	/**
	 * Set the type of Pet mounted (0 : none, 1 : Stridder, 2 : Wyvern) and send a Server->Client packet InventoryUpdate to the L2PcInstance.<BR><BR>
	 */
	public boolean checkLandingState()
	{
		// Check if char is in a no landing zone
		if (isInsideZone(ZONE_NOLANDING))
			return true;
		else
			// if this is a castle that is currently being sieged, and the rider is NOT a castle owner
			// he cannot land.
			// castle owner is the leader of the clan that owns the castle where the pc is
			if (isInsideZone(ZONE_SIEGE) && !(getClan()!= null &&
							CastleManager.getInstance().getCastle(this) == CastleManager.getInstance().getCastleByOwner(getClan()) &&
							this == getClan().getLeader().getPlayerInstance()))
				return true;

		return false;
	}

	// returns false if the change of mount type fails.
	public boolean setMount(int npcId, int npcLevel, int mountType)
	{
		switch(mountType)
		{
			case 0:
                setIsFlying(false);
                /* not used any more
				setIsRidingFenrirWolf(false);
				setIsRidingWFenrirWolf(false);
				setIsRidingGreatSnowWolf(false);*/
				setIsRidingStrider(false);
                break; //Dismounted
			case 1:
                setIsRidingStrider(true);
                if(isNoble())
                {
                	L2Skill striderAssaultSkill = SkillTable.FrequentSkill.STRIDER_SIEGE_ASSAULT.getSkill();
                	addSkill(striderAssaultSkill, false); // not saved to DB
                }
                break;
			case 2:
                setIsFlying(true);
                break; //Flying Wyvern
			case 3:
				/* not used any more
				switch (npcId)
				{
					case 16041:
						setIsRidingFenrirWolf(true);
						break;
					case 16042:
						setIsRidingWFenrirWolf(true);
						break;
					case 16037:
						setIsRidingGreatSnowWolf(true);
						break;
				}*/
				break;
		}

		_mountType = mountType;
        _mountNpcId = npcId;
        _mountLevel = npcLevel;

		return true;
	}

	/**
	 * Return the type of Pet mounted (0 : none, 1 : Strider, 2 : Wyvern, 3: Wolf).<BR><BR>
	 */
	public int getMountType()
	{
		return _mountType;
	}
	
	@Override
	public final void stopAllEffects()
	{
		super.stopAllEffects();
		updateAndBroadcastStatus(2);
 	}

	@Override
	public final void stopAllEffectsExceptThoseThatLastThroughDeath()
	{
		super.stopAllEffectsExceptThoseThatLastThroughDeath();
		updateAndBroadcastStatus(2);
	}

	/**
	 * Stop all toggle-type effects
	 */
	public final void stopAllToggles()
	{
		_effects.stopAllToggles();
	}

	public final void stopCubics()
	{
		if (getCubics() != null)
		{
			boolean removed = false;
			for (L2CubicInstance cubic : getCubics().values())
			{
				cubic.stopAction();
				delCubic(cubic.getId());
				removed = true;
			}
			if (removed)
				broadcastUserInfo();
		}
	}

	public final void stopCubicsByOthers()
	{
		if (getCubics() != null)
		{
			boolean removed = false;
			for (L2CubicInstance cubic : getCubics().values())
			{
				if (cubic.givenByOther())
				{
					cubic.stopAction();
					delCubic(cubic.getId());
					removed = true;
				}
			}
			if (removed)
				broadcastUserInfo();
		}
	}

	/**
	 * Send a Server->Client packet UserInfo to this L2PcInstance and CharInfo to all L2PcInstance in its _KnownPlayers.<BR><BR>
	 *
	 * <B><U> Concept</U> :</B><BR><BR>
	 * Others L2PcInstance in the detection area of the L2PcInstance are identified in <B>_knownPlayers</B>.
	 * In order to inform other players of this L2PcInstance state modifications, server just need to go through _knownPlayers to send Server->Client Packet<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Send a Server->Client packet UserInfo to this L2PcInstance (Public and Private Data)</li>
	 * <li>Send a Server->Client packet CharInfo to all L2PcInstance in _KnownPlayers of the L2PcInstance (Public data only)</li><BR><BR>
	 *
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : DON'T SEND UserInfo packet to other players instead of CharInfo packet.
	 * Indeed, UserInfo packet contains PRIVATE DATA as MaxHP, STR, DEX...</B></FONT><BR><BR>
	 *
	 */
	@Override
	public void updateAbnormalEffect()
	{
		broadcastUserInfo();
	}

	/**
	 * Disable the Inventory and create a new task to enable it after 1.5s.<BR><BR>
	 */
	public void tempInventoryDisable()
	{
		_inventoryDisable  = true;

		ThreadPoolManager.getInstance().scheduleGeneral(new InventoryEnable(), 1500);
	}

	/**
	 * Return True if the Inventory is disabled.<BR><BR>
	 */
	public boolean isInventoryDisabled()
	{
		return _inventoryDisable;
	}

	class InventoryEnable implements Runnable
	{
		public void run()
		{
			_inventoryDisable = false;
		}
	}

	public Map<Integer, L2CubicInstance> getCubics()
	{
		return _cubics;
	}

	/**
	 * Add a L2CubicInstance to the L2PcInstance _cubics.<BR><BR>
	 */
	public void addCubic(int id, int level, double matk, int activationtime, int activationchance, int totalLifetime, boolean givenByOther)
	{
		if (Config.DEBUG)
			_log.info("L2PcInstance(" + getName() + "): addCubic(" + id + "|" + level + "|" + matk + ")");
		L2CubicInstance cubic = new L2CubicInstance(this, id, level, (int) matk, activationtime, activationchance, totalLifetime, givenByOther);

		_cubics.put(id, cubic);
	}

	/**
	 * Remove a L2CubicInstance from the L2PcInstance _cubics.<BR><BR>
	 */
	public void delCubic(int id)
	{
		_cubics.remove(id);
	}

	/**
	 * Return the L2CubicInstance corresponding to the Identifier of the L2PcInstance _cubics.<BR><BR>
	 */
	public L2CubicInstance getCubic(int id)
	{
		return _cubics.get(id);
	}

	/**
	 * Return the modifier corresponding to the Enchant Effect of the Active Weapon (Min : 127).<BR><BR>
	 */
	public int getEnchantEffect()
	{
		L2ItemInstance wpn = getActiveWeaponInstance();

		if (wpn == null)
			return 0;

		return Math.min(127, wpn.getEnchantLevel());
	}

	/**
	 * Set the _lastFolkNpc of the L2PcInstance corresponding to the last Folk wich one the player talked.<BR><BR>
	 */
	public void setLastFolkNPC(L2Npc folkNpc)
	{
		_lastFolkNpc = folkNpc;
	}

	/**
	 * Return the _lastFolkNpc of the L2PcInstance corresponding to the last Folk wich one the player talked.<BR><BR>
	 */
	public L2Npc getLastFolkNPC()
	{
		return _lastFolkNpc;
	}

	/**
	 * Return True if L2PcInstance is a participant in the Festival of Darkness.<BR><BR>
	 */
	public boolean isFestivalParticipant()
	{
		return SevenSignsFestival.getInstance().isParticipant(this);
	}

	public void addAutoSoulShot(int itemId)
	{
		_activeSoulShots.put(itemId, itemId);
	}

	public void removeAutoSoulShot(int itemId)
	{
		_activeSoulShots.remove(itemId);
	}

	public Map<Integer, Integer> getAutoSoulShot()
	{
		return _activeSoulShots;
	}

	public void rechargeAutoSoulShot(boolean physical, boolean magic, boolean summon)
	{
		L2ItemInstance item;
		IItemHandler handler;
		
		if (_activeSoulShots == null || _activeSoulShots.isEmpty())
			return;
		
		try
		{
			Collection<Integer> vals = _activeSoulShots.values();
			
			for (int itemId : vals)
			{
				item = getInventory().getItemByItemId(itemId);
				
				if (item != null)
				{
					if (magic)
					{
						if (!summon)
						{
							switch(itemId)
							{
								case 2509: case 2510: case 2511: case 2512:
								case 2513: case 2514: case 3947:
								case 3948: case 3949: case 3950:
								case 3951: case 3952: case 5790:
								case 22072: case 22073: case 22074:
								case 22075: case 22076: case 22077:
								case 22078: case 22079: case 22080:
								case 22081:
									handler = ItemHandler.getInstance().getItemHandler(item.getEtcItem());
									
									if (handler != null)
										handler.useItem(this, item);
									
									break;
							}
						}
						else
						{
							switch(itemId)
							{
								case 6646: case 6647:
								case 20333: case 20334:
									handler = ItemHandler.getInstance().getItemHandler(item.getEtcItem());
									
									if (handler != null)
										handler.useItem(this, item);
									break;
							}
						}
					}
					
					if (physical)
					{
						if (!summon)
						{
							switch(itemId)
							{
								case 1463: case 1464: case 1465:
								case 1466: case 1467: case 1835:
								case 5789: case 22082: case 22083:
								case 22084: case 22085: case 22086:
									
									handler = ItemHandler.getInstance().getItemHandler(item.getEtcItem());
									
									if (handler != null)
										handler.useItem(this, item);
									
									break;
							}
						}
						else
						{
							if (itemId == 6645 || itemId == 20332)
							{
								handler = ItemHandler.getInstance().getItemHandler(item.getEtcItem());
								
								if (handler != null)
									handler.useItem(this, item);
							}
						}
					}
				}
				else
				{
					removeAutoSoulShot(itemId);
				}
			}
		}
		catch (NullPointerException npe)
		{
			
		}
	}
	
	public boolean disableAutoShot(int itemId)
	{
		if (getAutoSoulShot().containsKey(itemId))
		{
			removeAutoSoulShot(itemId);
			sendPacket(new ExAutoSoulShot(itemId, 0));
			
			SystemMessage sm = new SystemMessage(SystemMessageId.AUTO_USE_OF_S1_CANCELLED);
			sm.addString(ItemTable.getInstance().getTemplate(itemId).getName());
			sendPacket(sm);
			return true;
		}
		else return false;
	}

	private ScheduledFuture<?> _taskWarnUserTakeBreak;

	class WarnUserTakeBreak implements Runnable
	{
		public void run()
		{
			if (L2PcInstance.this.isOnline() == 1)
			{
				SystemMessage msg = new SystemMessage(SystemMessageId.PLAYING_FOR_LONG_TIME);
				L2PcInstance.this.sendPacket(msg);
			}
			else
				stopWarnUserTakeBreak();
		}
	}

	class RentPetTask implements Runnable
	{
		public void run()
		{
			stopRentPet();
		}
	}

	class WaterTask implements Runnable
	{
		public void run()
		{
			double reduceHp = getMaxHp()/100.0;

			if (reduceHp < 1)
				reduceHp = 1;

			reduceCurrentHp(reduceHp,L2PcInstance.this,false, false, null);
			//reduced hp, becouse not rest
			SystemMessage sm = new SystemMessage(SystemMessageId.DROWN_DAMAGE_S1);
			sm.addNumber((int)reduceHp);
			sendPacket(sm);

		}
	}

    class LookingForFishTask implements Runnable
    {
    	boolean _isNoob, _isUpperGrade;
    	int _fishType, _fishGutsCheck, _gutsCheckTime;
    	long _endTaskTime;

    	protected LookingForFishTask(int fishWaitTime, int fishGutsCheck, int fishType, boolean isNoob, boolean isUpperGrade)
    	{
    		_fishGutsCheck = fishGutsCheck;
    		_endTaskTime = System.currentTimeMillis() + fishWaitTime + 10000;
    		_fishType = fishType;
    		_isNoob = isNoob;
    		_isUpperGrade = isUpperGrade;
    	}

        public void run()
        {
            if (System.currentTimeMillis() >= _endTaskTime) {
            	endFishing(false);
            	return;
            }
        	if (_fishType == -1)
        		return;
            int check = Rnd.get(1000);
            if(_fishGutsCheck > check)
            {
                stopLookingForFishTask();
                startFishCombat(_isNoob, _isUpperGrade);
            }
        }

    }

	public int getClanPrivileges()
    {
		return _clanPrivileges;
	}

	public void setClanPrivileges(int n)
    {
		_clanPrivileges = n;
	}

    // baron etc
	public void setPledgeClass(int classId)
    {
        _pledgeClass = classId;
		checkItemRestriction();
    }

    public int getPledgeClass()
    {
        return _pledgeClass;
    }

    public void setPledgeType(int typeId)
    {
        _pledgeType = typeId;
    }

    public int getPledgeType()
    {
        return _pledgeType;
    }

    public int getApprentice()
    {
    	return _apprentice;
    }

    public void setApprentice(int apprentice_id)
    {
    	_apprentice = apprentice_id;
    }

    public int getSponsor()
    {
    	return _sponsor;
    }

    public void setSponsor(int sponsor_id)
    {
    	_sponsor = sponsor_id;
    }
    
    public int getBookMarkSlot()
    {
    	return _bookmarkslot;
    }
    
    public void setBookMarkSlot(int slot)
    {
    	_bookmarkslot = slot;
    	sendPacket(new ExGetBookMarkInfoPacket(this));
    }

	@Override
	public void sendMessage(String message)
	{
		sendPacket(SystemMessage.sendString(message));
	}

	public void enterObserverMode(int x, int y, int z)
	{
		_obsX = getX();
		_obsY = getY();
		_obsZ = getZ();

		_observerMode = true;
        setTarget(null);
		stopMove(null);
		setIsParalyzed(true);
        setIsInvul(true);
		getAppearance().setInvisible();
		sendPacket(new GMHide(1));
		sendPacket(new ObservationMode(x, y, z));
		setXYZ(x, y, z);

		broadcastUserInfo();
	}
	
	public void setObserverCords(int x, int y, int z)
	{
		_obsX = getX();
		_obsY = getY();
		_obsZ = getZ();
	}

	public void enterOlympiadObserverMode(int x, int y, int z, int id, boolean storeCoords)
    {
        if (getPet() != null)
            getPet().unSummon(this);

        if (!getCubics().isEmpty())
        {
            for (L2CubicInstance cubic : getCubics().values())
            {
                cubic.stopAction();
                cubic.cancelDisappear();
            }

            getCubics().clear();
        }
        
        if (getParty() != null)
        	getParty().removePartyMember(this);

    	_olympiadGameId = id;
        if (isSitting())
            standUp();
        if (storeCoords)
        {
	        _obsX = getX();
	        _obsY = getY();
	        _obsZ = getZ();
        }

        _observerMode = true;
        setTarget(null);
        setIsInvul(true);
        getAppearance().setInvisible();
        sendPacket(new GMHide(1));
        teleToLocation(x, y, z, false);
        sendPacket(new ExOlympiadMode(3));

        broadcastUserInfo();
    }

	public void leaveObserverMode()
	{
		setTarget(null);
		setXYZ(_obsX, _obsY, _obsZ);
		setIsParalyzed(false);
		sendPacket(new GMHide(0));
		if (!AdminCommandAccessRights.getInstance().hasAccess("admin_invis", getAccessLevel()))
			getAppearance().setVisible();
		if (!AdminCommandAccessRights.getInstance().hasAccess("admin_invul", getAccessLevel()))
			setIsInvul(false);
		if (getAI() != null)
			getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);

        _observerMode = false;
		setObserverCords(0, 0, 0);
		sendPacket(new ObservationReturn(this));
		broadcastUserInfo();
	}

	public void leaveOlympiadObserverMode()
    {
		setTarget(null);
		sendPacket(new ExOlympiadMode(0));
        teleToLocation(_obsX, _obsY, _obsZ, true);
        sendPacket(new GMHide(0));
        if (!AdminCommandAccessRights.getInstance().hasAccess("admin_invis", getAccessLevel()))
			getAppearance().setVisible();
		if (!AdminCommandAccessRights.getInstance().hasAccess("admin_invul", getAccessLevel()))
			setIsInvul(false);
		if (getAI() != null)
            getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);

        Olympiad.removeSpectator(_olympiadGameId, this);
        _olympiadGameId = -1;
        _observerMode = false;
        setObserverCords(0, 0, 0);
		broadcastUserInfo();
    }

    public void setOlympiadSide(int i)
    {
        _olympiadSide = i;
    }

    public int getOlympiadSide()
    {
        return _olympiadSide;
    }

    public void setOlympiadGameId(int id)
    {
        _olympiadGameId = id;
    }

    public int getOlympiadGameId()
    {
        return _olympiadGameId;
    }

	public int getObsX()
	{
		return _obsX;
	}

	public int getObsY()
	{
		return _obsY;
	}

	public int getObsZ()
	{
		return _obsZ;
	}

	public boolean inObserverMode()
	{
		return _observerMode;
	}

	public int getTeleMode()
	{
		return _telemode;
	}

	public void setTeleMode(int mode)
	{
		_telemode = mode;
	}

	public void setLoto(int i, int val)
	{
		_loto[i] = val;
	}

	public int getLoto(int i)
	{
		return _loto[i];
	}

	public void setRace(int i, int val)
	{
		_race[i] = val;
	}

	public int getRace(int i)
	{
		return _race[i];
	}

	public boolean getMessageRefusal()
	{
		return _messageRefusal;
	}

	public void setMessageRefusal(boolean mode)
	{
		_messageRefusal = mode;
		sendPacket(new EtcStatusUpdate(this));
	}

	public void setDietMode(boolean mode)
	{
		_dietMode = mode;
	}

	public boolean getDietMode()
	{
		return _dietMode;
	}

	public void setTradeRefusal(boolean mode)
	{
		_tradeRefusal = mode;
	}

	public boolean getTradeRefusal()
	{
		return _tradeRefusal;
	}

	public void setExchangeRefusal(boolean mode)
	{
		_exchangeRefusal = mode;
	}

	public boolean getExchangeRefusal()
	{
		return _exchangeRefusal;
	}

	public BlockList getBlockList()
	{
		return _blockList;
	}

	public void setHero(boolean hero)
	{
		if (hero && _baseClass == _activeClass)
		{
			for (L2Skill s : HeroSkillTable.getHeroSkills())
				addSkill(s, false); //Dont Save Hero skills to database
		}
		else
		{
			for (L2Skill s : HeroSkillTable.getHeroSkills())
				super.removeSkill(s); //Just Remove skills from nonHero characters
		}
		_hero = hero;
		
		sendSkillList();
	}

	public void setIsInOlympiadMode(boolean b)
    {
    	_inOlympiadMode = b;
    }

	public void setIsOlympiadStart(boolean b)
    {
    	_OlympiadStart = b;
    }

	public boolean isOlympiadStart(){
		return _OlympiadStart;
	}

    public boolean isHero()
    {
        return _hero;
    }

    public boolean isInOlympiadMode()
    {
    	return _inOlympiadMode;
    }

	public boolean isInDuel()
	{
		return _isInDuel;
	}

	public int getDuelId()
	{
		return _duelId;
	}

	public void setDuelState(int mode)
	{
		_duelState = mode;
	}

	public int getDuelState()
	{
		return _duelState;
	}

	/**
	 * Sets up the duel state using a non 0 duelId.
	 * @param duelId 0=not in a duel
	 */
	public void setIsInDuel(int duelId)
	{
		if (duelId > 0)
		{
			_isInDuel = true;
			_duelState = Duel.DUELSTATE_DUELLING;
			_duelId = duelId;
		}
		else
		{
			if (_duelState == Duel.DUELSTATE_DEAD) 
			{ 
				enableAllSkills(); 
				getStatus().startHpMpRegeneration(); 
			}
			_isInDuel = false;
			_duelState = Duel.DUELSTATE_NODUEL;
			_duelId = 0;
		}
	}

	/**
	 * This returns a SystemMessage stating why
	 * the player is not available for duelling.
	 * @return S1_CANNOT_DUEL... message
	 */
	public SystemMessage getNoDuelReason()
	{
		SystemMessage sm = new SystemMessage(_noDuelReason);
		sm.addPcName(this);
		_noDuelReason = SystemMessageId.THERE_IS_NO_OPPONENT_TO_RECEIVE_YOUR_CHALLENGE_FOR_A_DUEL;
		return sm;
	}

	/**
	 * Checks if this player might join / start a duel.
	 * To get the reason use getNoDuelReason() after calling this function.
	 * @return true if the player might join/start a duel.
	 */
	public boolean canDuel()
	{
		if (isInCombat() || getPunishLevel() == PunishLevel.JAIL) { _noDuelReason = SystemMessageId.C1_CANNOT_DUEL_BECAUSE_C1_IS_CURRENTLY_ENGAGED_IN_BATTLE; return false; }
		if (isDead() || isAlikeDead() || (getCurrentHp() < getMaxHp()/2 || getCurrentMp() < getMaxMp()/2)) { _noDuelReason = SystemMessageId.C1_CANNOT_DUEL_BECAUSE_C1_HP_OR_MP_IS_BELOW_50_PERCENT; return false; }
		if (isInDuel()) { _noDuelReason = SystemMessageId.C1_CANNOT_DUEL_BECAUSE_C1_IS_ALREADY_ENGAGED_IN_A_DUEL; return false;}
		if (isInOlympiadMode()) { _noDuelReason = SystemMessageId.C1_CANNOT_DUEL_BECAUSE_C1_IS_PARTICIPATING_IN_THE_OLYMPIAD; return false; }
		if (isCursedWeaponEquipped()) { _noDuelReason = SystemMessageId.C1_CANNOT_DUEL_BECAUSE_C1_IS_IN_A_CHAOTIC_STATE; return false; }
		if (getPrivateStoreType() != STORE_PRIVATE_NONE) { _noDuelReason = SystemMessageId.C1_CANNOT_DUEL_BECAUSE_C1_IS_CURRENTLY_ENGAGED_IN_A_PRIVATE_STORE_OR_MANUFACTURE; return false; }
		if (isMounted() || isInBoat()) { _noDuelReason = SystemMessageId.C1_CANNOT_DUEL_BECAUSE_C1_IS_CURRENTLY_RIDING_A_BOAT_WYVERN_OR_STRIDER; return false; }
		if (isFishing()) { _noDuelReason = SystemMessageId.C1_CANNOT_DUEL_BECAUSE_C1_IS_CURRENTLY_FISHING; return false; }
		if (isInsideZone(ZONE_PVP) || isInsideZone(ZONE_PEACE) || isInsideZone(ZONE_SIEGE))
		{
			_noDuelReason = SystemMessageId.C1_CANNOT_MAKE_A_CHALLANGE_TO_A_DUEL_BECAUSE_C1_IS_CURRENTLY_IN_A_DUEL_PROHIBITED_AREA;
			return false;
		}
		return true;
	}

    public boolean isNoble()
    {
    	return _noble;
    }

    public void setNoble(boolean val)
    {
    	if (val)
    		for (L2Skill s : NobleSkillTable.getInstance().getNobleSkills())
    			addSkill(s, false); //Dont Save Noble skills to Sql
    	else
    		for (L2Skill s : NobleSkillTable.getInstance().getNobleSkills())
    			super.removeSkill(s); //Just Remove skills without deleting from Sql
    	_noble = val;
    	
    	sendSkillList();
    }

    public void setLvlJoinedAcademy(int lvl)
    {
    	_lvlJoinedAcademy = lvl;
    }

    public int getLvlJoinedAcademy()
    {
    	return _lvlJoinedAcademy;
    }

    public boolean isAcademyMember()
    {
      return _lvlJoinedAcademy > 0;
    }

	public void setTeam(int team)
	{
		_team = team;
		if (getPet() != null)
			getPet().broadcastStatusUpdate();
	}

	public int getTeam()
	{
		return _team;
	}

	public void setWantsPeace(int wantsPeace)
	{
		_wantsPeace = wantsPeace;
	}

	public int getWantsPeace()
	{
		return _wantsPeace;
	}

    public boolean isFishing()
    {
        return _fishing;
    }

    public void setFishing(boolean fishing)
    {
        _fishing = fishing;
    }

    public void setAllianceWithVarkaKetra(int sideAndLvlOfAlliance)
    {
    	// [-5,-1] varka, 0 neutral, [1,5] ketra
    	_alliedVarkaKetra = sideAndLvlOfAlliance;
    }

    public int getAllianceWithVarkaKetra()
    {
    	return _alliedVarkaKetra;
    }

    public boolean isAlliedWithVarka()
    {
        return (_alliedVarkaKetra < 0);
    }

    public boolean isAlliedWithKetra()
    {
        return (_alliedVarkaKetra > 0);
    }

	public void sendSkillList()
	{
		sendSkillList(this);
	}

	public void sendSkillList(L2PcInstance player)
	{
		boolean isDisabled = false;
		SkillList sl = new SkillList();
		if (player != null)
		{
			for (L2Skill s : player.getAllSkills())
			{
				if (s == null)
					continue;
				if (s.getId() > 9000 && s.getId() < 9007)
					continue; // Fake skills to change base stats
				if (_transformation != null && (!containsAllowedTransformSkill(s.getId()) && !s.allowOnTransform()))
					continue;
				if (player.getClan() != null)
					isDisabled = s.isClanSkill() && player.getClan().getReputationScore() < 0;

				boolean isEnchantable = SkillTable.getInstance().isEnchantable(s.getId());
				if (isEnchantable)
				{
					L2EnchantSkillLearn esl = EnchantGroupsTable.getInstance().getSkillEnchantmentBySkillId(s.getId());
					if (esl != null)
					{
						//if player dont have min level to enchant 
						if (s.getLevel() < esl.getBaseLevel())
							isEnchantable = false;
					}
					// if no enchant data
					else
						isEnchantable = false;
				}

				sl.addSkill(s.getId(), s.getLevel(), s.isPassive(), isDisabled, isEnchantable);
			}
		}

		sendPacket(sl);
	}
    
    /**
     * 1. Add the specified class ID as a subclass (up to the maximum number of <b>three</b>)
     * for this character.<BR>
     * 2. This method no longer changes the active _classIndex of the player. This is only
     * done by the calling of setActiveClass() method as that should be the only way to do so.
	 *
     * @param int classId
     * @param int classIndex
     * @return boolean subclassAdded
     */
    public boolean addSubClass(int classId, int classIndex)
    {
    	if (!_subclassLock.tryLock())
    		return false;

    	try
    	{
        	if (getTotalSubClasses() == Config.MAX_SUBCLASS || classIndex == 0)
        		return false;

        	if (getSubClasses().containsKey(classIndex))
        		return false;

        	// Note: Never change _classIndex in any method other than setActiveClass().

            SubClass newClass = new SubClass();
            newClass.setClassId(classId);
            newClass.setClassIndex(classIndex);

            Connection con = null;
            PreparedStatement statement = null;

            try
            {
                // Store the basic info about this new sub-class.
                con = L2DatabaseFactory.getInstance().getConnection();
                statement = con.prepareStatement(ADD_CHAR_SUBCLASS);
                
                statement.setInt(1, getObjectId());
                statement.setInt(2, newClass.getClassId());
                statement.setLong(3, newClass.getExp());
                statement.setInt(4, newClass.getSp());
                statement.setInt(5, newClass.getLevel());
                statement.setInt(6, newClass.getClassIndex()); // <-- Added
                
                statement.execute();
            }
            catch (Exception e) {
                _log.warning("WARNING: Could not add character sub class for " + getName() + ": " + e);
                return false;
            }
            finally
            {
            	try
            	{
            		statement.close();
            	}
            	catch (Exception e)
            	{
            	}
                try
                {
                	con.close();
                }
                catch (Exception e)
                {
                }
            }

            // Commit after database INSERT incase exception is thrown.
            getSubClasses().put(newClass.getClassIndex(), newClass);

            if (Config.DEBUG)
                _log.info(getName() + " added class ID " + classId + " as a sub class at index " + classIndex + ".");

    		ClassId subTemplate = ClassId.values()[classId];
    		Collection<L2SkillLearn> skillTree = SkillTreeTable.getInstance().getAllowedSkills(subTemplate);

    		if (skillTree == null)
    			return true;

    		Map<Integer, L2Skill> prevSkillList = new FastMap<Integer, L2Skill>();

    		for (L2SkillLearn skillInfo : skillTree)
    		{
    			if (skillInfo.getMinLevel() <= 40)
    			{
    				L2Skill prevSkill = prevSkillList.get(skillInfo.getId());
    				L2Skill newSkill = SkillTable.getInstance().getInfo(skillInfo.getId(), skillInfo.getLevel());

    				if (prevSkill != null && (prevSkill.getLevel() > newSkill.getLevel()))
    					continue;

    				prevSkillList.put(newSkill.getId(), newSkill);
    				storeSkill(newSkill, prevSkill, classIndex);
    			}
    		}

            if (Config.DEBUG)
                _log.info(getName() + " was given " + getAllSkills().length + " skills for their new sub class.");

            return true;
    	}
    	finally
    	{
    		_subclassLock.unlock();
    	}
    }

    /**
     * 1. Completely erase all existance of the subClass linked to the classIndex.<BR>
     * 2. Send over the newClassId to addSubClass()to create a new instance on this classIndex.<BR>
     * 3. Upon Exception, revert the player to their BaseClass to avoid further problems.<BR>
     *
     * @param int classIndex
     * @param int newClassId
     * @return boolean subclassAdded
     */
    public boolean modifySubClass(int classIndex, int newClassId)
    {
    	if (!_subclassLock.tryLock())
    		return false;

    	try
    	{
        	int oldClassId = getSubClasses().get(classIndex).getClassId();

            if (Config.DEBUG)
    	    	_log.info(getName() + " has requested to modify sub class index " + classIndex + " from class ID " + oldClassId + " to " + newClassId + ".");

        	Connection con = null;
            PreparedStatement statement = null;

            try
            {
                con = L2DatabaseFactory.getInstance().getConnection();

                // Remove all henna info stored for this sub-class.
                statement = con.prepareStatement(DELETE_CHAR_HENNAS);
                statement.setInt(1, getObjectId());
                statement.setInt(2, classIndex);
                statement.execute();
                statement.close();

                // Remove all shortcuts info stored for this sub-class.
                statement = con.prepareStatement(DELETE_CHAR_SHORTCUTS);
                statement.setInt(1, getObjectId());
                statement.setInt(2, classIndex);
                statement.execute();
                statement.close();

                // Remove all effects info stored for this sub-class.
                statement = con.prepareStatement(DELETE_SKILL_SAVE);
                statement.setInt(1, getObjectId());
                statement.setInt(2, classIndex);
                statement.execute();
                statement.close();

                // Remove all skill info stored for this sub-class.
                statement = con.prepareStatement(DELETE_CHAR_SKILLS);
                statement.setInt(1, getObjectId());
                statement.setInt(2, classIndex);
                statement.execute();
                statement.close();

                // Remove all basic info stored about this sub-class.
                statement = con.prepareStatement(DELETE_CHAR_SUBCLASS);
                statement.setInt(1, getObjectId());
                statement.setInt(2, classIndex);
                statement.execute();
                statement.close();
            }
            catch (Exception e)
            {
            	_log.warning("Could not modify sub class for " + getName() + " to class index " + classIndex + ": " + e);

            	// This must be done in order to maintain data consistency.
                getSubClasses().remove(classIndex);
            	return false;
            }
            finally
            {
            	try
            	{
            		statement.close();
            	}
            	catch (Exception e)
            	{
            	}
                try
                {
                	con.close();
                }
                catch (Exception e)
                {
                }
            }

            getSubClasses().remove(classIndex);
    	}
    	finally
    	{
    		_subclassLock.unlock();
    	}

    	return addSubClass(newClassId, classIndex);
    }

    public boolean isSubClassActive()
    {
        return _classIndex > 0;
    }

    public Map<Integer, SubClass> getSubClasses()
    {
        if (_subClasses == null)
            _subClasses = new FastMap<Integer, SubClass>();

        return _subClasses;
    }

    public int getTotalSubClasses()
    {
        return getSubClasses().size();
    }

	public int getBaseClass()
	{
		return _baseClass;
	}

	public int getActiveClass()
	{
		return _activeClass;
	}

	public int getClassIndex()
	{
		return _classIndex;
	}

	private void setClassTemplate(int classId)
	{
		_activeClass = classId;

		L2PcTemplate t = CharTemplateTable.getInstance().getTemplate(classId);

		if (t == null)
		{
			_log.severe("Missing template for classId: "+classId);
			throw new Error();
		}

		// Set the template of the L2PcInstance
		setTemplate(t);
	}

	/**
	 * Changes the character's class based on the given class index.
	 * <BR><BR>
	 * An index of zero specifies the character's original (base) class,
	 * while indexes 1-3 specifies the character's sub-classes respectively.
	 * <br><br>
	 * <font color="00FF00"/>WARNING: Use only on subclase change</font>
	 *
	 * @param classIndex
	 */
	public boolean setActiveClass(int classIndex)
	{
		if (!_subclassLock.tryLock())
			return false;

		try
		{
			// Cannot switch or change subclasses while transformed
			if (_transformation != null)
				return false;

			// Remove active item skills before saving char to database
			// because next time when choosing this class, weared items can
			// be different
			for (L2ItemInstance item : getInventory().getAugmentedItems())
			{
				if (item != null && item.isEquipped())
					item.getAugmentation().removeBonus(this);
			}

			// abort any kind of cast.
			abortCast();

			// Stop casting for any player that may be casting a force buff on this l2pcinstance.
			for (L2Character character : getKnownList().getKnownCharacters())
			{
				if (character.getFusionSkill() != null && character.getFusionSkill().getTarget() == this)
					character.abortCast();
			}

			/*
			 * 1. Call store() before modifying _classIndex to avoid skill effects rollover.
			 * 2. Register the correct _classId against applied 'classIndex'.
			 */
			store(Config.SUBCLASS_STORE_SKILL_COOLTIME);
			_reuseTimeStamps.clear();

			// clear charges
			_charges.set(0);
			stopChargeTask();

			if (classIndex == 0)
			{
				setClassTemplate(getBaseClass());
			}
			else
			{
				try
				{
					setClassTemplate(getSubClasses().get(classIndex).getClassId());
				}
				catch (Exception e)
				{
					_log.info("Could not switch " + getName() + "'s sub class to class index " + classIndex + ": " + e);
					return false;
				}
			}
			_classIndex = classIndex;

			if (isInParty())
				getParty().recalculatePartyLevel();

			/*
			 * Update the character's change in class status.
			 *
			 * 1. Remove any active cubics from the player.
			 * 2. Renovate the characters table in the database with the new class info, storing also buff/effect data.
			 * 3. Remove all existing skills.
			 * 4. Restore all the learned skills for the current class from the database.
			 * 5. Restore effect/buff data for the new class.
			 * 6. Restore henna data for the class, applying the new stat modifiers while removing existing ones.
			 * 7. Reset HP/MP/CP stats and send Server->Client character status packet to reflect changes.
			 * 8. Restore shortcut data related to this class.
			 * 9. Resend a class change animation effect to broadcast to all nearby players.
			 * 10.Unsummon any active servitor from the player.
			 */

			if (getPet() instanceof L2SummonInstance)
				getPet().unSummon(this);

			for (L2Skill oldSkill : getAllSkills())
				super.removeSkill(oldSkill);

			stopAllEffectsExceptThoseThatLastThroughDeath();
			stopCubics();

			restoreRecipeBook(false);

			// Restore any Death Penalty Buff
			restoreDeathPenaltyBuffLevel();

			restoreSkills();
			rewardSkills();
			regiveTemporarySkills();

			// Prevents some issues when changing between subclases that shares skills
			if (_disabledSkills != null && !_disabledSkills.isEmpty()) 
				_disabledSkills.clear();

			restoreEffects();
			updateEffectIcons();
			sendPacket(new EtcStatusUpdate(this));

			// if player has quest 422: Repent Your Sins, remove it
			QuestState st = getQuestState("422_RepentYourSins");
			if (st != null)
				st.exitQuest(true);

			for (int i = 0; i < 3; i++)
				_henna[i] = null;

			restoreHenna();
			sendPacket(new HennaInfo(this));

			if (getCurrentHp() > getMaxHp())
				setCurrentHp(getMaxHp());
			if (getCurrentMp() > getMaxMp())
				setCurrentMp(getMaxMp());
			if (getCurrentCp() > getMaxCp())
				setCurrentCp(getMaxCp());

			refreshOverloaded();
			refreshExpertisePenalty();
			broadcastUserInfo();

			// Clear resurrect xp calculation
			setExpBeforeDeath(0);

			_shortCuts.restore();
			sendPacket(new ShortCutInit(this));            

			broadcastPacket(new SocialAction(getObjectId(), SocialAction.LEVEL_UP));
			sendPacket(new SkillCoolTime(this));
			sendPacket(new ExStorageMaxCount(this));

			return true;
		}
		finally
		{
			_subclassLock.unlock();
		}
	}

    public boolean isLocked()
    {
    	return _subclassLock.isLocked();
    }

	public void stopWarnUserTakeBreak()
	{
		if (_taskWarnUserTakeBreak != null)
		{
			_taskWarnUserTakeBreak.cancel(true);
			ThreadPoolManager.getInstance().removeGeneral((Runnable)_taskWarnUserTakeBreak);
			_taskWarnUserTakeBreak = null;
		}
	}

	public void startWarnUserTakeBreak()
	{
		if (_taskWarnUserTakeBreak == null)
			_taskWarnUserTakeBreak = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new WarnUserTakeBreak(), 7200000, 7200000);
	}

	public void stopRentPet()
	{
		if (_taskRentPet != null)
		{
			// if the rent of a wyvern expires while over a flying zone, tp to down before unmounting
			if (checkLandingState() && getMountType()==2)
				teleToLocation(MapRegionTable.TeleportWhereType.Town);
			
			if (this.dismount())  // this should always be true now, since we teleported already
			{
				_taskRentPet.cancel(true);
				_taskRentPet = null;
			}
		}
	}

	public void startRentPet(int seconds)
	{
		if (_taskRentPet == null)
			_taskRentPet = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new RentPetTask(), seconds * 1000L, seconds * 1000L);
	}

	public boolean isRentedPet()
	{
		if (_taskRentPet != null)
			return true;

		return false;
	}

	public void stopWaterTask()
	{
		if (_taskWater != null)
		{
			_taskWater.cancel(false);

			_taskWater = null;
			sendPacket(new SetupGauge(2, 0));
		}
	}

	public void startWaterTask()
	{
		if (!isDead() && _taskWater == null)
		{
			int timeinwater = (int)calcStat(Stats.BREATH, 60000, this, null);

			sendPacket(new SetupGauge(2, timeinwater));
			_taskWater = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new WaterTask(), timeinwater, 1000);
		}
	}

	public boolean isInWater()
	{
		if (_taskWater != null)
			return true;

		return false;
	}

	public void checkWaterState()
	{
		if (isInsideZone(ZONE_WATER))
			startWaterTask();
		else 
			stopWaterTask();
	}

	public void onPlayerEnter()
	{
		startWarnUserTakeBreak();

        if (SevenSigns.getInstance().isSealValidationPeriod() || SevenSigns.getInstance().isCompResultsPeriod())
        {
            if (!isGM() && isIn7sDungeon() && SevenSigns.getInstance().getPlayerCabal(getObjectId()) != SevenSigns.getInstance().getCabalHighestScore())
            {
                teleToLocation(MapRegionTable.TeleportWhereType.Town);
                setIsIn7sDungeon(false);
                sendMessage("You have been teleported to the nearest town due to the beginning of the Seal Validation period.");
            }
        }
        else
        {
            if (!isGM() && isIn7sDungeon() && SevenSigns.getInstance().getPlayerCabal(getObjectId()) == SevenSigns.CABAL_NULL)
            {
                teleToLocation(MapRegionTable.TeleportWhereType.Town);
                setIsIn7sDungeon(false);
                sendMessage("You have been teleported to the nearest town because you have not signed for any cabal.");
            }
        }

        // jail task
        updatePunishState();

        if (isGM())
        {
        	if (_isInvul)
        		sendMessage("Entering world in Invulnerable mode.");
        	if (getAppearance().getInvisible())
        		sendMessage("Entering world in Invisible mode.");
        	if (isSilenceMode())
        		sendMessage("Entering world in Silence mode.");
        }

		revalidateZone(true);
		
		notifyFriends();
	}

	public long getLastAccess()
	{
		return _lastAccess;
	}

	private void checkRecom(int recsHave, int recsLeft)
	{
		Calendar check = Calendar.getInstance();
		check.setTimeInMillis(_lastRecomUpdate);
		check.add(Calendar.DAY_OF_MONTH,1);

		Calendar min = Calendar.getInstance();

		_recomHave = recsHave;
		_recomLeft = recsLeft;

		if (getStat().getLevel() < 10 || check.after(min) )
			return;

		restartRecom();
	}

	public void restartRecom()
	{
		if (Config.ALT_RECOMMEND)
		{
			Connection con = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement(DELETE_CHAR_RECOMS);
				statement.setInt(1, getObjectId());
				
				statement.execute();
				statement.close();

				_recomChars.clear();
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "Failed cleaning character recommendations.", e);
			}
			finally
			{
				try { con.close(); } catch (Exception e) {}
			}
		}

		if (getStat().getLevel() < 20)
		{
			_recomLeft = 3;
			_recomHave--;
		}
		else if (getStat().getLevel() < 40)
		{
			_recomLeft = 6;
			_recomHave -= 2;
		}
		else
		{
			_recomLeft = 9;
			_recomHave -= 3;
		}
		if (_recomHave < 0) _recomHave = 0;

		// If we have to update last update time, but it's now before 13, we should set it to yesterday
		Calendar update = Calendar.getInstance();
		if(update.get(Calendar.HOUR_OF_DAY) < 13) update.add(Calendar.DAY_OF_MONTH,-1);
		update.set(Calendar.HOUR_OF_DAY,13);
		_lastRecomUpdate = update.getTimeInMillis();
	}

	
	@Override
	public void doRevive()
	{
		super.doRevive();
		stopEffects(L2EffectType.CHARMOFCOURAGE);
		updateEffectIcons();
		sendPacket(new EtcStatusUpdate(this));
		_reviveRequested = 0;
		_revivePower = 0;

		if (isMounted())
			startFeed(_mountNpcId);
		if (isInParty() && getParty().isInDimensionalRift())
		{
			if (!DimensionalRiftManager.getInstance().checkIfInPeaceZone(getX(), getY(), getZ()))
				getParty().getDimensionalRift().memberRessurected(this);
		}
	}

	@Override
	public void doRevive(double revivePower)
	{
		// Restore the player's lost experience,
		// depending on the % return of the skill used (based on its power).
		restoreExp(revivePower);
		doRevive();
	}

	public void reviveRequest(L2PcInstance Reviver, L2Skill skill, boolean Pet)
	{
		if (_reviveRequested == 1)
		{
			if (_revivePet == Pet)
			{
				Reviver.sendPacket(new SystemMessage(SystemMessageId.RES_HAS_ALREADY_BEEN_PROPOSED)); // Resurrection is already been proposed.
			}
			else
			{
				if (Pet)
					Reviver.sendPacket(new SystemMessage(SystemMessageId.CANNOT_RES_PET2)); // A pet cannot be resurrected while it's owner is in the process of resurrecting.
				else
					Reviver.sendPacket(new SystemMessage(SystemMessageId.MASTER_CANNOT_RES)); // While a pet is attempting to resurrect, it cannot help in resurrecting its master.
			}
			return;
		}
		if((Pet && getPet() != null && getPet().isDead()) || (!Pet && isDead()))
		{
			_reviveRequested = 1;
			int restoreExp = 0;
			if (isPhoenixBlessed())
				_revivePower=100;
			else if (getCharmOfCourage())
				_revivePower=0;
			else
				_revivePower = Formulas.calculateSkillResurrectRestorePercent(skill.getPower(), Reviver.getWIT());
			
			restoreExp = (int)Math.round((getExpBeforeDeath() - getExp()) * _revivePower / 100);
			    
			_revivePet = Pet;

			if (getCharmOfCourage())
			{
				ConfirmDlg dlg = new ConfirmDlg(SystemMessageId.RESURRECT_USING_CHARM_OF_COURAGE.getId());
				dlg.addTime(60000);
				sendPacket(dlg);
				return;
			}
			ConfirmDlg dlg = new ConfirmDlg(SystemMessageId.RESSURECTION_REQUEST_BY_C1_FOR_S2_XP.getId());
			dlg.addPcName(Reviver);
			dlg.addString(String.valueOf(restoreExp));
			sendPacket(dlg);
		}
	}

	public void reviveAnswer(int answer)
	{
		if (_reviveRequested != 1 || (!isDead() && !_revivePet) || (_revivePet && getPet() != null && !getPet().isDead()))
			return;
		//If character refuses a PhoenixBless autoress, cancel all buffs he had
		if (answer == 0 && isPhoenixBlessed())
		{
		    stopPhoenixBlessing(null);
		    stopAllEffectsExceptThoseThatLastThroughDeath();
		}
		if (answer == 1)
		{
			if (!_revivePet)
			{
				if (_revivePower != 0)
					doRevive(_revivePower);
				else
					doRevive();
			}
			else if (getPet() != null)
			{
				if (_revivePower != 0)
					getPet().doRevive(_revivePower);
				else
					getPet().doRevive();
			}
		}
		_reviveRequested = 0;
		_revivePower = 0;
	}

	public boolean isReviveRequested()
	{
		return (_reviveRequested == 1);
	}

	public boolean isRevivingPet()
	{
		return _revivePet;
	}

	public void removeReviving()
	{
		_reviveRequested = 0;
		_revivePower = 0;
	}

	public void onActionRequest()
	{
		setProtection(false);
	}

	/**
	 * @param expertiseIndex The expertiseIndex to set.
	 */
	public void setExpertiseIndex(int expertiseIndex)
	{
		_expertiseIndex = expertiseIndex;
	}

	/**
	 * @return Returns the expertiseIndex.
	 */
	public int getExpertiseIndex()
	{
		return _expertiseIndex;
	}

	@Override
	public final void onTeleported()
	{
		super.onTeleported();
		
		// Force a revalidation
		revalidateZone(true);

		checkItemRestriction();

		if ((Config.PLAYER_SPAWN_PROTECTION > 0) && !isInOlympiadMode())
			setProtection(true);
		
		// Trained beast is after teleport lost
		if (getTrainedBeast() != null)
		{
			getTrainedBeast().decayMe();
			setTrainedBeast(null);
		}
		
		// Modify the position of the pet if necessary
		L2Summon pet = getPet(); 
		if (pet != null)
		{
			pet.setFollowStatus(false);
			pet.teleToLocation(getPosition().getX(), getPosition().getY(), getPosition().getZ(), false);
			((L2SummonAI)pet.getAI()).setStartFollowController(true);
			pet.setFollowStatus(true);
			pet.updateAndBroadcastStatus(0);
		}

		TvTEvent.onTeleported(this);
	}

	@Override
	public void setIsTeleporting(boolean teleport)
	{
		setIsTeleporting(teleport, true);
	}
	
	public void setIsTeleporting(boolean teleport, boolean useWatchDog)
	{
		super.setIsTeleporting(teleport);
		if (!useWatchDog)
			return;
		if (teleport)
		{
			if (_teleportWatchdog == null && Config.TELEPORT_WATCHDOG_TIMEOUT > 0)
				_teleportWatchdog = ThreadPoolManager.getInstance().scheduleGeneral(new TeleportWatchdog(this), Config.TELEPORT_WATCHDOG_TIMEOUT * 1000);
		}
		else
		{
			if (_teleportWatchdog != null)
			{
				_teleportWatchdog.cancel(false);
				ThreadPoolManager.getInstance().removeGeneral((Runnable)_teleportWatchdog);
				_teleportWatchdog = null;
			}
		}
	}

	class TeleportWatchdog implements Runnable
	{
		private final L2PcInstance _player;

		TeleportWatchdog(L2PcInstance player)
		{
			_player = player;
		}

		public void run()
		{
			if (_player == null || !_player.isTeleporting())
				return;

			if (Config.DEBUG)
				_log.warning("Player " + _player.getName() + " teleport timeout expired");
			_player.onTeleported();
		}
	}

	public void setLastPartyPosition(int x, int y, int z)
	{
		_lastPartyPosition.setXYZ(x,y,z);
	}

	public int getLastPartyPositionDistance(int x, int y, int z)
	{
		double dx = (x - _lastPartyPosition.getX());
		double dy = (y - _lastPartyPosition.getY());
		double dz = (z - _lastPartyPosition.getZ());

		return (int)Math.sqrt(dx*dx + dy*dy + dz*dz);
	}

	public void setLastServerPosition(int x, int y, int z)
	{
		_lastServerPosition.setXYZ(x,y,z);
	}
	
	public Point3D getLastServerPosition()
	{
		return _lastServerPosition;
	}

	public boolean checkLastServerPosition(int x, int y, int z)
	{
		return _lastServerPosition.equals(x,y,z);
	}

	public int getLastServerDistance(int x, int y, int z)
	{
		double dx = (x - _lastServerPosition.getX());
		double dy = (y - _lastServerPosition.getY());
		double dz = (z - _lastServerPosition.getZ());

		return (int)Math.sqrt(dx*dx + dy*dy + dz*dz);
	}

	@Override
	public void addExpAndSp(long addToExp, int addToSp)
	{
		getStat().addExpAndSp(addToExp, addToSp, false);
	}

	public void addExpAndSp(long addToExp, int addToSp, boolean useVitality)
	{
		getStat().addExpAndSp(addToExp, addToSp, useVitality);
	}

	public void removeExpAndSp(long removeExp, int removeSp)
	{
		getStat().removeExpAndSp(removeExp, removeSp, true);
	}
	
	public void removeExpAndSp(long removeExp, int removeSp, boolean sendMessage)
	{
		getStat().removeExpAndSp(removeExp, removeSp, sendMessage);
	}

	@Override
	public void reduceCurrentHp(double i, L2Character attacker, L2Skill skill)
    {
    	getStatus().reduceHp(i, attacker);

    	// notify the tamed beast of attacks
    	if (getTrainedBeast() != null )
    		getTrainedBeast().onOwnerGotAttacked(attacker);
    }
	@Override
	public void reduceCurrentHp(double value, L2Character attacker, boolean awake, boolean isDOT, L2Skill skill)
	{
		getStatus().reduceHp(value, attacker, awake, isDOT, false);

    	// notify the tamed beast of attacks
    	if (getTrainedBeast() != null )
    		getTrainedBeast().onOwnerGotAttacked(attacker);
	}

	public void broadcastSnoop(int type, String name, String _text)
	{
		if(!_snoopListener.isEmpty())
		{
			Snoop sn = new Snoop(getObjectId(),getName(),type,name,_text);

            for (L2PcInstance pci : _snoopListener)
				if (pci != null)
					pci.sendPacket(sn);
		}
	}

	public void addSnooper(L2PcInstance pci )
	{
		if(!_snoopListener.contains(pci))
			_snoopListener.add(pci);
	}

	public void removeSnooper(L2PcInstance pci )
	{
		_snoopListener.remove(pci);
	}

	public void addSnooped(L2PcInstance pci )
	{
		if(!_snoopedPlayer.contains(pci))
			_snoopedPlayer.add(pci);
	}

	public void removeSnooped(L2PcInstance pci )
	{
		_snoopedPlayer.remove(pci);
	}

	public void addBypass(String bypass)
	{
		if (bypass == null) return;
		synchronized(_validBypass)
		{
			_validBypass.add(bypass);
		}
		//_log.warning("[BypassAdd]"+getName()+" '"+bypass+"'");
	}

	public void addBypass2(String bypass)
	{
		if (bypass == null) return;
		synchronized(_validBypass2)
		{
			_validBypass2.add(bypass);
		}
		//_log.warning("[BypassAdd]"+getName()+" '"+bypass+"'");
	}

	public boolean validateBypass(String cmd)
	{
		if (!Config.BYPASS_VALIDATION)
			return true;

		synchronized (_validBypass)
		{
			for (String bp : _validBypass)
			{
				if (bp == null)
					continue;
				
				//_log.warning("[BypassValidation]"+getName()+" '"+bp+"'");
				if (bp.equals(cmd))
					return true;
			}
		}
		
		synchronized (_validBypass2)
		{
			for (String bp : _validBypass2)
			{
				if (bp == null)
					continue;
				
				//_log.warning("[BypassValidation]"+getName()+" '"+bp+"'");
				if (cmd.startsWith(bp))
					return true;
			}
		}
		_log.warning("[L2PcInstance] player ["+getName()+"] sent invalid bypass '"+cmd+"', ban this player!");
		return false;
	}

	/**
	 * Performs following tests:<br>
	 * <li> Inventory contains item
	 * <li> Item owner id == this.owner id
	 * <li> It isnt pet control item while mounting pet or pet summoned
	 * <li> It isnt active enchant item
	 * <li> It isnt cursed weapon/item
	 * <li> It isnt wear item
	 * <br>
	 * 
	 * @param objectId: item object id
	 * @param action: just for login porpouse
	 * @return
	 */
	public boolean validateItemManipulation(int objectId, String action)
	{
		L2ItemInstance item = getInventory().getItemByObjectId(objectId);

		if (item == null || item.getOwnerId() != getObjectId())
		{
			_log.finest(getObjectId()+": player tried to " + action + " item he is not owner of");
			return false;
		}

		// Pet is summoned and not the item that summoned the pet AND not the buggle from strider you're mounting
		if (getPet() != null && getPet().getControlItemId() == objectId || getMountObjectID() == objectId)
		{
			if (Config.DEBUG)
				_log.finest(getObjectId()+": player tried to " + action + " item controling pet");

			return false;
		}

		if(getActiveEnchantItem() != null && getActiveEnchantItem().getObjectId() == objectId)
		{
			if (Config.DEBUG)
				_log.finest(getObjectId()+":player tried to " + action + " an enchant scroll he was using");

			return false;
		}

		if (CursedWeaponsManager.getInstance().isCursed(item.getItemId()))
		{
			// can not trade a cursed weapon
			return false;
		}

		if (item.isWear())
		{
			// cannot drop/trade wear-items
			return false;
		}

		return true;
	}

	public void clearBypass()
	{
		synchronized (_validBypass)
		{
			_validBypass.clear();
		}
		synchronized (_validBypass2)
		{
			_validBypass2.clear();
		}
	}

	/**
	 * @return Returns the inBoat.
	 */
	public boolean isInBoat()
	{
		return _inBoat;
	}

	/**
	 * @param inBoat The inBoat to set.
	 */
	public void setInBoat(boolean inBoat)
	{
		_inBoat = inBoat;
	}

	/**
	 * @return
	 */
	public L2BoatInstance getBoat()
	{
		return _boat;
	}

	/**
	 * @param boat
	 */
	public void setBoat(L2BoatInstance boat)
	{
		_boat = boat;
	}

	/**
	 * @return Returns the inAirShip.
	 */
	public boolean isInAirShip()
	{
		return _inAirShip;
	}

	/**
	 * @param inAirShip The inAirShip to set.
	 */
	public void setInAirShip(boolean inAirShip)
	{
		_inAirShip = inAirShip;
	}

	/**
	 * @return
	 */
	public L2AirShipInstance getAirShip()
	{
		return _airShip;
	}

	/**
	 * @param airShip
	 */
	public void setAirShip(L2AirShipInstance airShip)
	{
		if (airShip != null)
			setInAirShip(true);
		else
			setInAirShip(false);
		_airShip = airShip;
	}
	
	public void setInCrystallize(boolean inCrystallize)
	{
		_inCrystallize = inCrystallize;
	}

	public boolean isInCrystallize()
	{
		return _inCrystallize;
	}

	/**
	 * @return
	 */
	public Point3D getInBoatPosition()
	{
		return _inBoatPosition;
	}

	public void setInBoatPosition(Point3D pt)
	{
		_inBoatPosition = pt;
	}

	/**
	 * @return
	 */
	public Point3D getInAirShipPosition()
	{
		return _inAirShipPosition;
	}

	public void setInAirShipPosition(Point3D pt)
	{
		_inAirShipPosition = pt;
	}

	/**
	 * Manage the delete task of a L2PcInstance (Leave Party, Unsummon pet, Save its inventory in the database, Remove it from the world...).<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>If the L2PcInstance is in observer mode, set its position to its position before entering in observer mode </li>
	 * <li>Set the online Flag to True or False and update the characters table of the database with online status and lastAccess </li>
	 * <li>Stop the HP/MP/CP Regeneration task </li>
	 * <li>Cancel Crafting, Attak or Cast </li>
	 * <li>Remove the L2PcInstance from the world </li>
	 * <li>Stop Party and Unsummon Pet </li>
	 * <li>Update database with items in its inventory and remove them from the world </li>
	 * <li>Remove all L2Object from _knownObjects and _knownPlayer of the L2Character then cancel Attak or Cast and notify AI </li>
	 * <li>Close the connection with the client </li><BR><BR>
	 *
	 */
	public void deleteMe(boolean closeClient)
	{
		try
		{
			abortAttack();
			abortCast();
			stopMove(null);
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "deleteMe()", e);
		}
		
		//remove combat flag
		try
		{
			if (getInventory().getItemByItemId(9819) != null)
			{
				Fort fort = FortManager.getInstance().getFort(this);
				if (fort != null)
					FortSiegeManager.getInstance().dropCombatFlag(this);
				else
				{
					int slot = getInventory().getSlotFromItem(getInventory().getItemByItemId(9819));
					getInventory().unEquipItemInBodySlotAndRecord(slot);
					destroyItem("CombatFlag", getInventory().getItemByItemId(9819), null, true);
				}
			}
			else if (isCombatFlagEquipped())
			{
				TerritoryWarManager.getInstance().dropCombatFlag(this, false);
			}
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "deleteMe()", e);
		}
		
		try
		{
			if (isFlying())
			{
				removeSkill(SkillTable.getInstance().getInfo(4289, 1));
			}
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "deleteMe()", e);
		}
		
		try
		{
			CommunityServerThread.getInstance().sendPacket(new WorldInfo(this, null,WorldInfo.TYPE_UPDATE_PLAYER_STATUS));
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "deleteMe()", e);
		}
		
		// Set the online Flag to True or False and update the characters table of the database with online status and lastAccess (called when login and logout)
		try
		{
			setOnlineStatus(false);
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "deleteMe()", e);
		}
		
		// Stop the HP/MP/CP Regeneration task (scheduled tasks)
		try
		{
			stopAllTimers();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "deleteMe()", e);
		}
		
		try
		{
			setIsTeleporting(false);
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "deleteMe()", e);
		}

		// Stop crafting, if in progress
		try
		{
			RecipeController.getInstance().requestMakeItemAbort(this);
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "deleteMe()", e);
		}
		
		// Cancel Attak or Cast
		try
		{
			setTarget(null);
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "deleteMe()", e);
		}
		
		try
		{
			if (_fusionSkill != null)
				abortCast();
			
			for (L2Character character : getKnownList().getKnownCharacters())
				if (character.getFusionSkill() != null && character.getFusionSkill().getTarget() == this)
					character.abortCast();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "deleteMe()", e);
		}
		
		try
		{
			for (L2Effect effect : getAllEffects())
			{
				if (effect.getSkill().isToggle())
				{
					effect.exit();
					continue;
				}

				switch (effect.getEffectType())
				{
					case SIGNET_GROUND:
					case SIGNET_EFFECT:
						effect.exit();
						break;
				}
			}
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "deleteMe()", e);
		}

		try
		{
			final int arena = Olympiad.getSpectatorArena(this);
			if (arena >= 0)
				Olympiad.removeSpectator(arena, this);
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "deleteMe()", e);
		}

		// Remove from world regions zones
		L2WorldRegion oldRegion = getWorldRegion();
		
		// Remove the L2PcInstance from the world
		try
		{
			decayMe();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "deleteMe()", e);
		}
		
		if (oldRegion != null)
			oldRegion.removeFromZones(this);
		
		// If a Party is in progress, leave it (and festival party)
		if (isInParty())
			try
			{
				leaveParty();
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "deleteMe()", e);
			}
		
		if (Olympiad.getInstance().isRegistered(this) || getOlympiadGameId() != -1) // handle removal from olympiad game
			Olympiad.getInstance().removeDisconnectedCompetitor(this);
		
		// If the L2PcInstance has Pet, unsummon it
		if (getPet() != null)
		{
			try
			{
				getPet().unSummon(this);
				// dead pet wasnt unsummoned, broadcast npcinfo changes (pet will be without owner name - means owner offline)
				if (getPet() != null)
					getPet().broadcastNpcInfo(0);
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "deleteMe()", e);
			}// returns pet to control item
		}
		
		if (getClan() != null)
		{
			// set the status for pledge member list to OFFLINE
			try
			{
				L2ClanMember clanMember = getClan().getClanMember(getObjectId());
				if (clanMember != null)
					clanMember.setPlayerInstance(null);
					
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "deleteMe()", e);
			}
		}
		
		if (getActiveRequester() != null)
		{
			// deals with sudden exit in the middle of transaction
			setActiveRequester(null);
		}
		
		// If the L2PcInstance is a GM, remove it from the GM List
		if (isGM())
		{
			try
			{
				GmListTable.getInstance().deleteGm(this);
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "deleteMe()", e);
			}
		}

		try
		{
			// Check if the L2PcInstance is in observer mode to set its position to its position
			// before entering in observer mode
			if (inObserverMode())
				setXYZInvisible(_obsX, _obsY, _obsZ);
			else if (isInAirShip())
				getAirShip().oustPlayer(this);
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "deleteMe()", e);
		}

		// remove player from instance and set spawn location if any
		try
		{
			final int instanceId = getInstanceId();
			if (instanceId != 0 && !Config.RESTORE_PLAYER_INSTANCE)
			{
				final Instance inst = InstanceManager.getInstance().getInstance(instanceId);
				if (inst != null)
				{
					inst.removePlayer(getObjectId());
					final int[] spawn = inst.getSpawnLoc();
					if (spawn[0] != 0 && spawn[1] != 0 && spawn[2] != 0)
					{
						final int x = spawn[0] + Rnd.get(-30, 30);
						final int y = spawn[1] + Rnd.get(-30, 30);
						setXYZInvisible(x, y, spawn[2]);
						if (getPet() != null) // dead pet
						{
							getPet().teleToLocation(x, y, spawn[2]);
							getPet().setInstanceId(0);
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "deleteMe()", e);
		}

		// TvT Event removal
		try
		{
			TvTEvent.onLogout(this);
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "deleteMe()", e);
		}

		// Update database with items in its inventory and remove them from the world
		try
		{
			getInventory().deleteMe();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "deleteMe()", e);
		}
		
		// Update database with items in its warehouse and remove them from the world
		try
		{
			clearWarehouse();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "deleteMe()", e);
		}
		if (Config.WAREHOUSE_CACHE)
			WarehouseCacheManager.getInstance().remCacheTask(this);
		
		try
		{
			clearRefund();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "deleteMe()", e);
		}
		
		if (isCursedWeaponEquipped())
		{
			try
			{
				CursedWeaponsManager.getInstance().getCursedWeapon(_cursedWeaponEquippedId).setPlayer(null);
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "deleteMe()", e);
			}
		}
		
		// Remove all L2Object from _knownObjects and _knownPlayer of the L2Character then cancel Attak or Cast and notify AI
		try
		{
			getKnownList().removeAllKnownObjects();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "deleteMe()", e);
		}
		
		// Close the connection with the client
		closeNetConnection(closeClient);
		
		if (getClanId() > 0)
			getClan().broadcastToOtherOnlineMembers(new PledgeShowMemberListUpdate(this), this);
		//ClanTable.getInstance().getClan(getClanId()).broadcastToOnlineMembers(new PledgeShowMemberListAdd(this));
		
		for (L2PcInstance player : _snoopedPlayer)
			player.removeSnooper(this);
		
		for (L2PcInstance player : _snoopListener)
			player.removeSnooped(this);
		
		// Remove L2Object object from _allObjects of L2World
		L2World.getInstance().removeObject(this);
		L2World.getInstance().removeFromAllPlayers(this); // force remove in case of crash during teleport
		
		//update bbs
		try
		{
			RegionBBSManager.getInstance().changeCommunityBoard();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		try
		{
			notifyFriends();
			getBlockList().playerLogout();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private FishData _fish;

   /*  startFishing() was stripped of any pre-fishing related checks, namely the fishing zone check.
    * Also worthy of note is the fact the code to find the hook landing position was also striped. The
    * stripped code was moved into fishing.java. In my opinion it makes more sense for it to be there
    * since all other skill related checks were also there. Last but not least, moving the zone check
    * there, fixed a bug where baits would always be consumed no matter if fishing actualy took place.
    * startFishing() now takes up 3 arguments, wich are acurately described as being the hook landing 
    * coordinates.
    */
	public void startFishing(int _x, int _y, int _z)
    {
        stopMove(null);
        setIsImmobilized(true);
        _fishing = true;
        _fishx = _x;
        _fishy = _y;
        _fishz = _z;
        //broadcastUserInfo();
        //Starts fishing
    	int lvl = GetRandomFishLvl();
		int group = GetRandomGroup();
		int type = GetRandomFishType(group);
		List<FishData> fishs = FishTable.getInstance().getfish(lvl, type, group);
		if (fishs == null || fishs.isEmpty())
		{
			sendMessage("Error - Fishes are not definied");
			endFishing(false);
			return;
		}
		int check = Rnd.get(fishs.size());
		// Use a copy constructor else the fish data may be over-written below
		_fish = new FishData(fishs.get(check));
		fishs.clear();
		fishs = null;
        sendPacket(new SystemMessage(SystemMessageId.CAST_LINE_AND_START_FISHING));
        if (!GameTimeController.getInstance().isNowNight() && _lure.isNightLure())
        	_fish.setType(-1);
		//sendMessage("Hook x,y: " + _x + "," + _y + " - Water Z, Player Z:" + _z + ", " + getZ()); //debug line, uncoment to show coordinates used in fishing.
        broadcastPacket(new ExFishingStart(this,_fish.getType(),_x,_y,_z,_lure.isNightLure()));
        sendPacket(new PlaySound(1, "SF_P_01", 0, 0, 0, 0, 0));
        startLookingForFishTask();
    }
    public void stopLookingForFishTask()
    {
        if (_taskforfish != null)
        {
            _taskforfish.cancel(false);
            _taskforfish = null;
        }
    }
    public void startLookingForFishTask()
    {
        if (!isDead() && _taskforfish == null)
        {
            int checkDelay = 0;
            boolean isNoob = false;
            boolean isUpperGrade = false;

            if (_lure != null)
            {
        		int lureid = _lure.getItemId();
        		isNoob = _fish.getGroup() == 0;
        		isUpperGrade = _fish.getGroup() == 2;
                if (lureid == 6519 || lureid == 6522 || lureid == 6525 || lureid == 8505 || lureid == 8508 || lureid == 8511) //low grade
                	checkDelay = Math.round((float)(_fish.getGutsCheckTime() * (1.33)));
                else if (lureid == 6520 || lureid == 6523 || lureid == 6526 || (lureid >= 8505 && lureid <= 8513) || (lureid >= 7610 && lureid <= 7613) || (lureid >= 7807 && lureid <= 7809) || (lureid >= 8484 && lureid <= 8486)) //medium grade, beginner, prize-winning & quest special bait
                	checkDelay = Math.round((float)(_fish.getGutsCheckTime() * (1.00)));
                else if (lureid == 6521 || lureid == 6524 || lureid == 6527 || lureid == 8507 || lureid == 8510 || lureid == 8513) //high grade
                	checkDelay = Math.round((float)(_fish.getGutsCheckTime() * (0.66)));
            }
            _taskforfish = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new LookingForFishTask(_fish.getWaitTime(), _fish.getFishGuts(), _fish.getType(), isNoob, isUpperGrade), 10000, checkDelay);
        }
    }

	private int GetRandomGroup()
	{
		switch (_lure.getItemId()) {
			case 7807: //green for beginners
			case 7808: //purple for beginners
			case 7809: //yellow for beginners
			case 8486: //prize-winning for beginners
				return 0;
			case 8485: //prize-winning luminous
			case 8506: //green luminous
			case 8509: //purple luminous
			case 8512: //yellow luminous
				return 2;
			default:
				return 1;
		}
	}
	private int GetRandomFishType(int group)
	{
		int check = Rnd.get(100);
		int type = 1;
		switch (group) {
			case 0:	//fish for novices
				switch (_lure.getItemId()) {
					case 7807: //green lure, preferred by fast-moving (nimble) fish (type 5)
						if (check <= 54)
							type = 5;
						else if (check <= 77)
							type = 4;
						else
							type = 6;
						break;
					case 7808: //purple lure, preferred by fat fish (type 4)
						if (check <= 54)
							type = 4;
						else if (check <= 77)
							type = 6;
						else
							type = 5;
						break;
					case 7809: //yellow lure, preferred by ugly fish (type 6)
						if (check <= 54)
							type = 6;
						else if (check <= 77)
							type = 5;
						else
							type = 4;
						break;
					case 8486:	//prize-winning fishing lure for beginners
						if (check <= 33)
							type = 4;
						else if (check <= 66)
							type = 5;
						else
							type = 6;
						break;
				}
				break;
			case 1:	//normal fish
				switch (_lure.getItemId()) {
					case 7610:
					case 7611:
					case 7612:
					case 7613:
						type = 3;
						break;
					case 6519:  //all theese lures (green) are prefered by fast-moving (nimble) fish (type 1)
					case 8505:
					case 6520:
					case 6521:
					case 8507:
						if (check <= 54)
							type = 1;
						else if (check <= 74)
							type = 0;
						else if (check <= 94)
							type = 2;
						else
							type = 3;
						break;
					case 6522:	 //all theese lures (purple) are prefered by fat fish (type 0)
					case 8508:
					case 6523:
					case 6524:
					case 8510:
						if (check <= 54)
							type = 0;
						else if (check <= 74)
							type = 1;
						else if (check <= 94)
							type = 2;
						else
							type = 3;
						break;
					case 6525:	//all theese lures (yellow) are prefered by ugly fish (type 2)
					case 8511:
					case 6526:
					case 6527:
					case 8513:
						if (check <= 55)
							type = 2;
						else if (check <= 74)
							type = 1;
						else if (check <= 94)
							type = 0;
						else
							type = 3;
						break;
					case 8484:	//prize-winning fishing lure
						if (check <= 33)
							type = 0;
						else if (check <= 66)
							type = 1;
						else
							type = 2;
						break;
				}
				break;
			case 2:	//upper grade fish, luminous lure
				switch (_lure.getItemId()) {
					case 8506: //green lure, preferred by fast-moving (nimble) fish (type 8)
						if (check <= 54)
							type = 8;
						else if (check <= 77)
							type = 7;
						else
							type = 9;
						break;
					case 8509: //purple lure, preferred by fat fish (type 7)
						if (check <= 54)
							type = 7;
						else if (check <= 77)
							type = 9;
						else
							type = 8;
						break;
					case 8512: //yellow lure, preferred by ugly fish (type 9)
						if (check <= 54)
							type = 9;
						else if (check <= 77)
							type = 8;
						else
							type = 7;
						break;
					case 8485: //prize-winning fishing lure
						if (check <= 33)
							type = 7;
						else if (check <= 66)
							type = 8;
						else
							type = 9;
						break;
				}
		}
		return type;
	}
	private int GetRandomFishLvl()
	{
		int skilllvl = getSkillLevel(1315);
		final L2Effect e = getFirstEffect(2274);
		if (e != null)
			skilllvl = (int)e.getSkill().getPower();
		if (skilllvl <= 0) return 1;
		int randomlvl;
		int check = Rnd.get(100);

		if (check <= 50)
		{
			randomlvl = skilllvl;
		}
		else if (check <= 85)
		{
			randomlvl = skilllvl - 1;
			if (randomlvl <= 0)
			{
				randomlvl = 1;
			}
		}
		else
		{
			randomlvl = skilllvl + 1;
			if (randomlvl > 27) randomlvl = 27;
		}

		return randomlvl;
	}
	public void startFishCombat(boolean isNoob, boolean isUpperGrade)
    {
        _fishCombat = new L2Fishing (this, _fish, isNoob, isUpperGrade);
    }
    
	public void endFishing(boolean win)
    {
        _fishing = false;
        _fishx = 0;
        _fishy = 0;
        _fishz = 0;
        //broadcastUserInfo();
        if (_fishCombat == null)
            sendPacket(new SystemMessage(SystemMessageId.BAIT_LOST_FISH_GOT_AWAY));
        _fishCombat = null;
        _lure = null;
        //Ends fishing
        broadcastPacket(new ExFishingEnd(win, this));
        sendPacket(new SystemMessage(SystemMessageId.REEL_LINE_AND_STOP_FISHING));
        setIsImmobilized(false);
        stopLookingForFishTask();
    }

    public L2Fishing getFishCombat()
    {
        return _fishCombat;
    }

    public int getFishx()
    {
        return _fishx;
    }

    public int getFishy()
    {
        return _fishy;
    }

    public int getFishz()
    {
        return _fishz;
    }

    public void setLure (L2ItemInstance lure)
    {
        _lure = lure;
    }

 	public L2ItemInstance getLure()
	{
 		return _lure;
	}

	public int getInventoryLimit()
	{
		int ivlim;
		if (isGM())
			ivlim = Config.INVENTORY_MAXIMUM_GM;
		else if (getRace() == Race.Dwarf)
			ivlim = Config.INVENTORY_MAXIMUM_DWARF;
		else
        	ivlim = Config.INVENTORY_MAXIMUM_NO_DWARF;
		ivlim += (int)getStat().calcStat(Stats.INV_LIM, 0, null, null);

		return ivlim;
	}

	public int getWareHouseLimit()
	{
		int whlim;
		if (getRace() == Race.Dwarf)
               whlim = Config.WAREHOUSE_SLOTS_DWARF;
		else
			whlim = Config.WAREHOUSE_SLOTS_NO_DWARF;

		whlim += (int)getStat().calcStat(Stats.WH_LIM, 0, null, null);

		return whlim;
	}
	
	public int getPrivateSellStoreLimit()
	{
		int pslim;
	
		if (getRace() == Race.Dwarf)
			pslim = Config.MAX_PVTSTORESELL_SLOTS_DWARF;
        else
        	pslim = Config.MAX_PVTSTORESELL_SLOTS_OTHER;

		pslim += (int)getStat().calcStat(Stats.P_SELL_LIM, 0, null, null);

		return pslim;
	}

	public int getPrivateBuyStoreLimit()
	{
		int pblim;
		
		if (getRace() == Race.Dwarf)
			pblim = Config.MAX_PVTSTOREBUY_SLOTS_DWARF;
		else
			pblim = Config.MAX_PVTSTOREBUY_SLOTS_OTHER;
		pblim += (int)getStat().calcStat(Stats.P_BUY_LIM, 0, null, null);

		return pblim;
	}
	
    public int getDwarfRecipeLimit()
    {
        int recdlim = Config.DWARF_RECIPE_LIMIT;
        recdlim += (int)getStat().calcStat(Stats.REC_D_LIM, 0, null, null);
        return recdlim;
    }

    public int getCommonRecipeLimit()
    {
        int recclim = Config.COMMON_RECIPE_LIMIT;
        recclim += (int)getStat().calcStat(Stats.REC_C_LIM, 0, null, null);
        return recclim;
    }

    /**
     * @return Returns the mountNpcId.
     */
    public int getMountNpcId()
    {
        return _mountNpcId;
    }
    
    /**
     * @return Returns the mountLevel.
     */
    public int getMountLevel()
    {
    	return _mountLevel;
    }

    public void setMountObjectID(int newID)
	{
	    _mountObjectID = newID;
	}

	public int getMountObjectID()
	{
	    return _mountObjectID;
	}

    private L2ItemInstance _lure = null;
	public int _shortBuffTaskSkillId = 0;

    /**
     * Get the current skill in use or return null.<BR><BR>
     *
     */
    public SkillDat getCurrentSkill()
    {
        return _currentSkill;
    }


    /**
     * Create a new SkillDat object and set the player _currentSkill.<BR><BR>
     *
     */
    public void setCurrentSkill(L2Skill currentSkill, boolean ctrlPressed,
                                boolean shiftPressed)
    {
        if (currentSkill == null)
        {
            if (Config.DEBUG)
                _log.info("Setting current skill: NULL for " + getName() + ".");

            _currentSkill = null;
            return;
        }

        if (Config.DEBUG)
            _log.info("Setting current skill: " + currentSkill.getName() + " (ID: " + currentSkill.getId() + ") for " + getName() + ".");

        _currentSkill = new SkillDat(currentSkill, ctrlPressed, shiftPressed);
    }
    
    /**
     * Get the current pet skill in use or return null.<br><br>
     * 
     */
    public SkillDat getCurrentPetSkill()
    {
    	return _currentPetSkill;
    }

    /**
     * Create a new SkillDat object and set the player _currentPetSkill.<br><br>
     * 
     */
    public void setCurrentPetSkill(L2Skill currentSkill, boolean ctrlPressed, boolean shiftPressed)
    {
        if (currentSkill == null)
        {
            if (Config.DEBUG)
                _log.info("Setting current pet skill: NULL for " + getName() + ".");

            _currentPetSkill = null;
            return;
        }

        if (Config.DEBUG)
            _log.info("Setting current Pet skill: " + currentSkill.getName() + " (ID: " + currentSkill.getId() + ") for " + getName() + ".");

        _currentPetSkill = new SkillDat(currentSkill, ctrlPressed, shiftPressed);    	
    }


    public SkillDat getQueuedSkill()
    {
        return _queuedSkill;
    }


    /**
     * Create a new SkillDat object and queue it in the player _queuedSkill.<BR><BR>
     *
     */
    public void setQueuedSkill(L2Skill queuedSkill, boolean ctrlPressed, boolean shiftPressed)
    {
        if (queuedSkill == null)
        {
            if (Config.DEBUG)
                _log.info("Setting queued skill: NULL for " + getName() + ".");

            _queuedSkill = null;
            return;
        }

        if (Config.DEBUG)
            _log.info("Setting queued skill: " + queuedSkill.getName() + " (ID: " + queuedSkill.getId() + ") for " + getName() + ".");

        _queuedSkill = new SkillDat(queuedSkill, ctrlPressed, shiftPressed);
    }

    /**
     * returns punishment level of player
     * @return
     */
    public PunishLevel getPunishLevel()
    {
        return _punishLevel;
    }

    /**
     * @return True if player is jailed
     */
    public boolean isInJail()
    {
    	return _punishLevel == PunishLevel.JAIL;
    }
    
    /**
     * @return True if player is chat banned
     */
    public boolean isChatBanned()
    {
    	return _punishLevel == PunishLevel.CHAT;
    }
    
    public void setPunishLevel(int state)
    {
    	switch (state){
    		case 0 :
    		{
    			_punishLevel = PunishLevel.NONE;
    			break;
    		}
    		case 1 :
    		{
    			_punishLevel = PunishLevel.CHAT;
    			break;
    		}
    		case 2 :
    		{
    			_punishLevel = PunishLevel.JAIL;
    			break;
    		}
    		case 3 :
    		{
    			_punishLevel = PunishLevel.CHAR;
    			break;
    		}
    		case 4 :
    		{
    			_punishLevel = PunishLevel.ACC;
    			break;
    		}
    	}
    }

    /** 
     * Sets punish level for player based on delay
     * @param state
     * @param delayInMinutes
     * 		0 - Indefinite
     */
    public void setPunishLevel(PunishLevel state, int delayInMinutes)
    {
    	long delayInMilliseconds = delayInMinutes * 60000L;
    	switch (state)
    	{
    		case NONE: // Remove Punishments
	    	{
	    		switch (_punishLevel)
	    		{
	    			case CHAT:
		    		{
		    			_punishLevel = state;
		    			stopPunishTask(true);
		    			sendPacket(new EtcStatusUpdate(this));
			            sendMessage("Your Chat ban has been lifted");
			            break;
		    		}
	    			case JAIL:
		    		{
		    			_punishLevel = state;
		    			// Open a Html message to inform the player
		        		NpcHtmlMessage htmlMsg = new NpcHtmlMessage(0);
			            String jailInfos = HtmCache.getInstance().getHtm(getHtmlPrefix(), "data/html/jail_out.htm");
			            if (jailInfos != null)
			                htmlMsg.setHtml(jailInfos);
			            else
			                htmlMsg.setHtml("<html><body>You are free for now, respect server rules!</body></html>");
			            sendPacket(htmlMsg);
			            stopPunishTask(true);
			            teleToLocation(17836, 170178, -3507, true);  // Floran
			            break;
		    		}
	    		}
	    		break;
	    	}
    		case CHAT: // Chat Ban
	    	{
	    		// not allow player to escape jail using chat ban
	    		if (_punishLevel == PunishLevel.JAIL)
	    			break;
	    		_punishLevel = state;
	    		_punishTimer = 0;
	    		sendPacket(new EtcStatusUpdate(this));
	    		// Remove the task if any
	    		stopPunishTask(false);
	    		
	    		if (delayInMinutes > 0)
	    		{
	    			_punishTimer = delayInMilliseconds;
	    			
	    			// start the countdown
	    			_punishTask = ThreadPoolManager.getInstance().scheduleGeneral(new PunishTask(this), _punishTimer);
	                sendMessage("You are chat banned for "+delayInMinutes+" minutes.");
	    		}
	    		else
	    			sendMessage("You have been chat banned");
	    		break;
	    		
	    	}
    		case JAIL: // Jail Player
	    	{
	    		_punishLevel = state;
	    		_punishTimer = 0;
		        // Remove the task if any
		        stopPunishTask(false);
	
	            if (delayInMinutes > 0)
	            {
	                _punishTimer = delayInMilliseconds;
	
	                // start the countdown
	                _punishTask = ThreadPoolManager.getInstance().scheduleGeneral(new PunishTask(this), _punishTimer);
	                sendMessage("You are in jail for "+delayInMinutes+" minutes.");
	            }
	            
	            if (!TvTEvent.isInactive() && TvTEvent.isPlayerParticipant(getObjectId()))
	                TvTEvent.removeParticipant(getObjectId());
	            if (Olympiad.getInstance().isRegisteredInComp(this))
	                Olympiad.getInstance().removeDisconnectedCompetitor(this);
	
	            // Open a Html message to inform the player
	            NpcHtmlMessage htmlMsg = new NpcHtmlMessage(0);
	            String jailInfos = HtmCache.getInstance().getHtm(getHtmlPrefix(), "data/html/jail_in.htm");
	            if (jailInfos != null)
	                htmlMsg.setHtml(jailInfos);
	            else
	                htmlMsg.setHtml("<html><body>You have been put in jail by an admin.</body></html>");
	            sendPacket(htmlMsg);
	            setInstanceId(0);
	            setIsIn7sDungeon(false);
	
	            teleToLocation(-114356, -249645, -2984, false);  // Jail
	            break;
	        }
			case CHAR: // Ban Character
			{
				setAccessLevel(-100);
				logout();
				break;
			}
			case ACC: // Ban Account
			{
				setAccountAccesslevel(-100);
				logout();
				break;
			}
	    	default:
	    	{
	    		_punishLevel = state;
	    		break;
	    	}
    	}

        // store in database
        storeCharBase();
    }

    public long getPunishTimer()
    {
        return _punishTimer;
    }

    public void setPunishTimer(long time)
    {
        _punishTimer = time;
    }

    private void updatePunishState()
    {
    	if (getPunishLevel() != PunishLevel.NONE)
        {
            // If punish timer exists, restart punishtask.
            if (_punishTimer > 0)
            {
                _punishTask = ThreadPoolManager.getInstance().scheduleGeneral(new PunishTask(this), _punishTimer);
                sendMessage("You are still "+getPunishLevel().string()+" for "+Math.round(_punishTimer/60000)+" minutes.");
            }
            if (getPunishLevel() == PunishLevel.JAIL)
            {
            	// If player escaped, put him back in jail
                if (!isInsideZone(ZONE_JAIL))
                    teleToLocation(-114356,-249645,-2984, true);
            }
        }
    }

    public void stopPunishTask(boolean save)
    {
        if (_punishTask != null)
        {
            if (save)
            {
            	long delay = _punishTask.getDelay(TimeUnit.MILLISECONDS);
            	if (delay < 0)
            		delay = 0;
            	setPunishTimer(delay);
            }
            _punishTask.cancel(false);
            ThreadPoolManager.getInstance().removeGeneral((Runnable)_punishTask);
            _punishTask = null;
        }
    }

    private class PunishTask implements Runnable
    {
        L2PcInstance _player;
        // protected long _startedAt;

        protected PunishTask(L2PcInstance player)
        {
            _player = player;
            // _startedAt = System.currentTimeMillis();
        }

        public void run()
        {
            _player.setPunishLevel(PunishLevel.NONE, 0);
        }
    }
    public void startFameTask(long delay, int fameFixRate)
    {
    	if (getLevel() < 40 || getClassId().level() < 2)
    		return;
    	if (_fameTask == null)
    		_fameTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new FameTask(this, fameFixRate), delay, delay);
    }
    
    public void stopFameTask()
    {
        if (_fameTask != null)
        {
            _fameTask.cancel(false);
            ThreadPoolManager.getInstance().removeGeneral((Runnable)_fameTask);
            _fameTask = null;
        }
    }
    
    private class FameTask implements Runnable
    {
        private final L2PcInstance _player;
        private final int _value;

        protected FameTask(L2PcInstance player, int value)
        {
            _player = player;
            _value = value;
        }

        public void run()
        {
        	if (_player == null || (_player.isDead() && !Config.FAME_FOR_DEAD_PLAYERS))
        		return;
        	if ((_player.getClient() == null || _player.getClient().isDetached()) && !Config.OFFLINE_FAME)
        		return;
            _player.setFame(_player.getFame() + _value);
            SystemMessage sm = new SystemMessage(SystemMessageId.ACQUIRED_S1_REPUTATION_SCORE);
            sm.addNumber(_value);
            _player.sendPacket(sm);
            _player.sendPacket(new UserInfo(_player));
        }
    }

    public void startVitalityTask()
    {
    	if (Config.ENABLE_VITALITY && _vitalityTask == null)
    	{
    		_vitalityTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new VitalityTask(this), 1000, 60000);
    	}
    }

    public void stopVitalityTask()
    {
    	if (_vitalityTask != null)
    	{
    		_vitalityTask.cancel(false);
    		ThreadPoolManager.getInstance().removeGeneral((Runnable)_vitalityTask);
    		_vitalityTask = null;
    	}
    }

    private class VitalityTask implements Runnable
    {
    	private final L2PcInstance _player;

    	protected VitalityTask(L2PcInstance player)
    	{
    		_player = player;
    	}

    	public void run()
    	{
    		if (!_player.isInsideZone(L2Character.ZONE_PEACE))
    			return;

    		if (_player.getVitalityPoints() >= PcStat.MAX_VITALITY_POINTS)
    			return;

    		_player.updateVitalityPoints(Config.RATE_RECOVERY_VITALITY_PEACE_ZONE, false, false);
    		_player.sendPacket(new ExVitalityPointInfo(getVitalityPoints()));
    	}
    }

    /**
	 * @return
	 */
	public int getPowerGrade()
	{
		return _powerGrade;
	}

	/**
	 * @return
	 */
	public void setPowerGrade(int power)
	{
		_powerGrade = power;
	}

	public boolean isCursedWeaponEquipped()
	{
		return _cursedWeaponEquippedId != 0;
	}

	public void setCursedWeaponEquippedId(int value)
	{
		_cursedWeaponEquippedId = value;
	}

	public int getCursedWeaponEquippedId()
	{
		return _cursedWeaponEquippedId;
	}
	
	@Override
	public boolean isAttackingDisabled()
	{
		return (super.isAttackingDisabled() || _combatFlagEquippedId);
	}
	
	public boolean isCombatFlagEquipped()
    {
        return _combatFlagEquippedId ;
    }

    public void setCombatFlagEquipped(boolean value)
    {
        _combatFlagEquippedId = value;
    }

	public boolean getCharmOfCourage()
	{
		return _charmOfCourage;
	}

	public void setCharmOfCourage(boolean val) 
	{
		_charmOfCourage = val;
		sendPacket(new EtcStatusUpdate(this));
	}
	
	public final void setIsRidingStrider(boolean mode)
	{
		_isRidingStrider = mode;
	}
	
	/* not used anymore
	public final void setIsRidingFenrirWolf(boolean mode)
	{
		_isRidingFenrirWolf = mode;
	}
	public final void setIsRidingWFenrirWolf(boolean mode)
	{
		_isRidingWFenrirWolf = mode;
	}
	public final void setIsRidingGreatSnowWolf(boolean mode)
	{
		_isRidingGreatSnowWolf = mode;
	}
	public final boolean isRidingFenrirWolf()
	{ 
		return _isRidingFenrirWolf;
	}
	public final boolean isRidingWFenrirWolf()
	{ 
		return _isRidingWFenrirWolf;
	}
	public final boolean isRidingGreatSnowWolf()
	{ 
		return _isRidingGreatSnowWolf;
	}*/
	
	public final boolean isRidingStrider()
	{ 
		return _isRidingStrider;
	}

    /**
     * Returns the Number of Souls this L2PcInstance got.
     * @return
     */
    public int getSouls()
    {
        return _souls;
    }

    /**
     * Absorbs a Soul from a Npc.
     * @param skill
     * @param target
     */
    public void absorbSoul(L2Skill skill, L2Npc npc)
    {
        if (_souls >= skill.getNumSouls())
        {
            SystemMessage sm = new SystemMessage(SystemMessageId.SOUL_CANNOT_BE_INCREASED_ANYMORE);
            sendPacket(sm);
            return;
        }

        increaseSouls(1);

        if (npc != null)
        	broadcastPacket(new ExSpawnEmitter(this, npc), 500);
    }
    
    /**
     * Increase Souls
     * @param count
     */
    public void increaseSouls(int count)
    {
        if (count < 0 || count > 45)
            return;

        _souls += count;

        if (getSouls() > 45)
            _souls = 45;

        SystemMessage sm = new SystemMessage(SystemMessageId.YOUR_SOUL_HAS_INCREASED_BY_S1_SO_IT_IS_NOW_AT_S2);
        sm.addNumber(count);
        sm.addNumber(_souls);
        sendPacket(sm);

        restartSoulTask();

        sendPacket(new EtcStatusUpdate(this));
    }

    /**
     * Decreases existing Souls.
     * @param count
     */
    public boolean decreaseSouls(int count, L2Skill skill)
    {
        if (getSouls() <= 0 && skill.getSoulConsumeCount() > 0)
        {
        	sendPacket(new SystemMessage(SystemMessageId.THERE_IS_NOT_ENOUGH_SOUL));
            return false;
        }

        _souls -= count;

        if (getSouls() < 0)
            _souls = 0;

        if (getSouls() == 0)
            stopSoulTask();
        else
            restartSoulTask();

        sendPacket(new EtcStatusUpdate(this));
        return true;
    }

    /**
     * Clear out all Souls from this L2PcInstance
     */
    public void clearSouls()
    {
        _souls = 0;
        stopSoulTask();
        sendPacket(new EtcStatusUpdate(this));
    }
    
    /**
     * Starts/Restarts the SoulTask to Clear Souls after 10 Mins.
     */
    private void restartSoulTask()
    {
    	synchronized(this)
    	{
    		if (_soulTask != null)
    		{
    			_soulTask.cancel(false);
    			_soulTask = null;
    		}
    		_soulTask = ThreadPoolManager.getInstance().scheduleGeneral(new SoulTask(this), 600000);
    	}
    }

    /**
     * Stops the Clearing Task.
     */
    public void stopSoulTask()
    {
        if (_soulTask != null)
        {
            _soulTask.cancel(false);
            ThreadPoolManager.getInstance().removeGeneral((Runnable)_soulTask);
            _soulTask = null;
        }
    }

    private class SoulTask implements Runnable
    {
        L2PcInstance _player;
        
        protected SoulTask(L2PcInstance player)
        {
            _player = player;
        }
        
        public void run()
        {
            _player.clearSouls();
        }
    }
    
    /**
     *
     * @param magicId
     * @param level
     * @param time
     */
    public void shortBuffStatusUpdate(int magicId, int level, int time)
	{
		if (_shortBuffTask != null)
		{
			_shortBuffTask.cancel(false);
			_shortBuffTask = null;
		}
		_shortBuffTask = ThreadPoolManager.getInstance().scheduleGeneral(new ShortBuffTask(this), time*1000);
		setShortBuffTaskSkillId(magicId);
		
		sendPacket(new ShortBuffStatusUpdate(magicId, level, time));
	}
    
    public void setShortBuffTaskSkillId(int id)
    {
    	_shortBuffTaskSkillId = id;
    }
    public int getDeathPenaltyBuffLevel()
    {
    	  return _deathPenaltyBuffLevel;
    }

    public void setDeathPenaltyBuffLevel(int level)
    {
    	 _deathPenaltyBuffLevel = level;
    }

    public void calculateDeathPenaltyBuffLevel(L2Character killer)
    {
    	if((getKarma() > 0 || Rnd.get(1,100) <= Config.DEATH_PENALTY_CHANCE)
    			&& !(killer instanceof L2PcInstance) && !(this.isGM())
    			&& !(this.getCharmOfLuck() && killer.isRaid())
    			&& !isPhoenixBlessed()
    			&& !(TvTEvent.isStarted() && TvTEvent.isPlayerParticipant(getObjectId()))
    			&& !(this.isInsideZone(L2Character.ZONE_PVP)||this.isInsideZone(L2Character.ZONE_SIEGE))) 
    		
    		increaseDeathPenaltyBuffLevel();
	}

    public void increaseDeathPenaltyBuffLevel()
    {
    	if(getDeathPenaltyBuffLevel() >= 15) //maximum level reached
    		return;
    	
    	if(getDeathPenaltyBuffLevel() != 0)
    	{
    		L2Skill skill = SkillTable.getInstance().getInfo(5076, getDeathPenaltyBuffLevel());

    		if(skill != null)
    			removeSkill(skill, true);
    	}

    	_deathPenaltyBuffLevel++;

    	addSkill(SkillTable.getInstance().getInfo(5076, getDeathPenaltyBuffLevel()), false);
    	sendPacket(new EtcStatusUpdate(this));
    	SystemMessage sm = new SystemMessage(SystemMessageId.DEATH_PENALTY_LEVEL_S1_ADDED);
    	sm.addNumber(getDeathPenaltyBuffLevel());
    	sendPacket(sm);
    }

    public void reduceDeathPenaltyBuffLevel()
    {
    	if(getDeathPenaltyBuffLevel() <= 0)
    		return;

    	L2Skill skill = SkillTable.getInstance().getInfo(5076, getDeathPenaltyBuffLevel());

    	if(skill != null)
    		removeSkill(skill, true);

    	_deathPenaltyBuffLevel--;

    	if(getDeathPenaltyBuffLevel() > 0)
    	{
    		addSkill(SkillTable.getInstance().getInfo(5076, getDeathPenaltyBuffLevel()), false);
    		sendPacket(new EtcStatusUpdate(this));
    		SystemMessage sm = new SystemMessage(SystemMessageId.DEATH_PENALTY_LEVEL_S1_ADDED);
    		sm.addNumber(getDeathPenaltyBuffLevel());
    		sendPacket(sm);
    	}
    	else
    	{
    		sendPacket(new EtcStatusUpdate(this));
    		sendPacket(new SystemMessage(SystemMessageId.DEATH_PENALTY_LIFTED));
    	}
    }

    public void restoreDeathPenaltyBuffLevel()
    {
    	L2Skill skill = SkillTable.getInstance().getInfo(5076, getDeathPenaltyBuffLevel());

    	if(skill != null)
    		removeSkill(skill, true);

    	if(getDeathPenaltyBuffLevel() > 0)
    	{
    		addSkill(SkillTable.getInstance().getInfo(5076, getDeathPenaltyBuffLevel()), false);
    	}
    }

	private FastMap<Integer, TimeStamp> _reuseTimeStamps = new FastMap<Integer, TimeStamp>().shared();
	private boolean _canFeed;
	private int _eventEffectId = 0;
	private boolean _isInSiege;

    public Collection<TimeStamp> getReuseTimeStamps()
    {
        return _reuseTimeStamps.values();
    }
    
    public FastMap<Integer, TimeStamp> getReuseTimeStamp()
    {
    	return _reuseTimeStamps;
    }
    
	/**
	 * Simple class containing all neccessary information to maintain
	 * valid timestamps and reuse for skills upon relog. Filter this
	 * carefully as it becomes redundant to store reuse for small delays.
	 * @author  Yesod
	 */
	public static class TimeStamp
	{
		private final int _skillId;
		private final int _skillLvl;
		private final long _reuse;
		private final long _stamp;

		public TimeStamp(L2Skill skill, long reuse)
		{
			_skillId = skill.getId();
			_skillLvl = skill.getLevel();
			_reuse = reuse;
			_stamp = System.currentTimeMillis()+ reuse;
		}

		public TimeStamp(L2Skill skill, long reuse, long systime)
		{
			_skillId = skill.getId();
			_skillLvl = skill.getLevel();
			_reuse = reuse;
			_stamp = systime;
		}

		public long getStamp()
		{
				return _stamp;
		}

		public int getSkillId()
        {
            return _skillId;
        }

		public int getSkillLvl()
		{
			return _skillLvl;
		}

		public long getReuse()
        {
            return _reuse;
        }
        
        public long getRemaining()
        {
            return Math.max(_stamp - System.currentTimeMillis(), 0);
        }

		/* Check if the reuse delay has passed and
		 * if it has not then update the stored reuse time
		 * according to what is currently remaining on
		 * the delay. */
		public boolean hasNotPassed()
		{
			return System.currentTimeMillis() < _stamp;
		}
	}

	/**
	 * Index according to skill id the current
	 * timestamp of use.
	 * @param skillid
	 * @param reuse delay
	 */
	@Override
	public void addTimeStamp(L2Skill skill, long reuse)
	{
		_reuseTimeStamps.put(skill.getReuseHashCode(), new TimeStamp(skill, reuse));
	}

	/**
	 * Index according to skill this TimeStamp
	 * instance for restoration purposes only.
	 * @param TimeStamp
	 */
	public void addTimeStamp(L2Skill skill, long reuse, long systime)
	{
		_reuseTimeStamps.put(skill.getReuseHashCode(), new TimeStamp(skill, reuse, systime));
	}

	@Override
	public L2PcInstance getActingPlayer()
	{
		return this;
	}

	@Override
	public final void sendDamageMessage(L2Character target, int damage, boolean mcrit, boolean pcrit, boolean miss)
    {
		// Check if hit is missed
		if (miss)
		{
			sendPacket(new SystemMessage(SystemMessageId.C1_ATTACK_WENT_ASTRAY).addString(getName()));
			return;
		}

		// Check if hit is critical
		if (pcrit)
        {
			sendPacket(new SystemMessage(SystemMessageId.C1_HAD_CRITICAL_HIT).addPcName(this));
            if (target instanceof L2Npc && getSkillLevel(467) > 0)
            {
                L2Skill skill = SkillTable.getInstance().getInfo(467,getSkillLevel(467));
                if (Rnd.get(100) < skill.getCritChance())
                {
                    absorbSoul(skill,((L2Npc)target));
                }
            }
        }
        if (mcrit)
            sendPacket(new SystemMessage(SystemMessageId.CRITICAL_HIT_MAGIC));

        if (isInOlympiadMode() &&
        		target instanceof L2PcInstance &&
        		((L2PcInstance)target).isInOlympiadMode() &&
        		((L2PcInstance)target).getOlympiadGameId() == getOlympiadGameId())
        {
        	Olympiad.getInstance().notifyCompetitorDamage(this, damage, getOlympiadGameId());
        }

		final SystemMessage sm;
		
		if (target.isInvul() && !(target instanceof L2NpcInstance))
		{
			sm = new SystemMessage(SystemMessageId.ATTACK_WAS_BLOCKED);
		}
		else if (target instanceof L2DoorInstance || target instanceof L2ControlTowerInstance)
		{
			sm = new SystemMessage(SystemMessageId.YOU_DID_S1_DMG);
			sm.addNumber(damage);
		}
		else
		{
			sm = new SystemMessage(SystemMessageId.C1_GAVE_C2_DAMAGE_OF_S3);
			sm.addPcName(this);
			sm.addCharName(target);
			sm.addNumber(damage);
		}
		
		sendPacket(sm);
	}


    /**
     * 
     * @param npcId
     */
    public void setAgathionId(int npcId)
    {
    	_agathionId = npcId;
    }
    
    /**
     * 
     * @return
     */
    public int getAgathionId()
    {
	    return _agathionId;
    }

    public int getVitalityPoints()
    {
    	return getStat().getVitalityPoints();
    }
	
	/**
	 * @return Vitality Level
	 */
	public int getVitalityLevel()
	{
		return getStat().getVitalityLevel();
	}

    public void setVitalityPoints(int points, boolean quiet)
    {
    	getStat().setVitalityPoints(points, quiet);
    }

    public void updateVitalityPoints(float points, boolean useRates, boolean quiet)
    {
    	getStat().updateVitalityPoints(points, useRates, quiet);
    }

    /*
     * Function for skill summon friend or Gate Chant.
     */
    /** Request Teleport **/
    public boolean teleportRequest(L2PcInstance requester, L2Skill skill)
    {
    	if (_summonRequest.getTarget() != null && requester != null)
    		return false;
    	_summonRequest.setTarget(requester, skill);
    	return true;
    }

    /** Action teleport **/
    public void teleportAnswer(int answer, int requesterId)
    {
    	if (_summonRequest.getTarget() == null)
    		return;
    	if (answer == 1 && _summonRequest.getTarget().getCharId() == requesterId)
    	{
    		teleToTarget(this, _summonRequest.getTarget(), _summonRequest.getSkill());
    	}
    	_summonRequest.setTarget(null, null);
    }
    
    public static void teleToTarget(L2PcInstance targetChar, L2PcInstance summonerChar, L2Skill summonSkill)
	{
		if (targetChar == null || summonerChar == null || summonSkill == null)
			return;

		if (!checkSummonerStatus(summonerChar))
			return;
		if (!checkSummonTargetStatus(targetChar, summonerChar))
			return;

		int itemConsumeId = summonSkill.getTargetConsumeId();
		int itemConsumeCount = summonSkill.getTargetConsume();
		if (itemConsumeId != 0 && itemConsumeCount != 0)
		{
			//Delete by rocknow
			if (targetChar.getInventory().getInventoryItemCount(itemConsumeId, 0) < itemConsumeCount)
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.S1_REQUIRED_FOR_SUMMONING);
				sm.addItemName(summonSkill.getTargetConsumeId());
				targetChar.sendPacket(sm);
				return;
			}
			targetChar.getInventory().destroyItemByItemId("Consume", itemConsumeId, itemConsumeCount, summonerChar, targetChar);
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_DISAPPEARED);
			sm.addItemName(summonSkill.getTargetConsumeId());
			targetChar.sendPacket(sm);
		}
		targetChar.teleToLocation(summonerChar.getX(), summonerChar.getY(), summonerChar.getZ(), true);
	}

	public static boolean checkSummonerStatus(L2PcInstance summonerChar)
	{
		if (summonerChar == null)
			return false;

		if (summonerChar.isInOlympiadMode())
		{
			summonerChar.sendPacket(new SystemMessage(SystemMessageId.THIS_ITEM_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT));
			return false;
		}

		if (summonerChar.inObserverMode())
		{
			return false;
		}

		if (!TvTEvent.onEscapeUse(summonerChar.getObjectId()))
		{
			summonerChar.sendPacket(new SystemMessage(SystemMessageId.YOUR_TARGET_IS_IN_AN_AREA_WHICH_BLOCKS_SUMMONING));
			return false;			
		}
		
		if (summonerChar.isInsideZone(L2Character.ZONE_NOSUMMONFRIEND) || summonerChar.isFlyingMounted())
		{
			summonerChar.sendPacket(new SystemMessage(SystemMessageId.YOUR_TARGET_IS_IN_AN_AREA_WHICH_BLOCKS_SUMMONING));
			return false;
		}
		return true;
	}

	public static boolean checkSummonTargetStatus(L2Object target, L2PcInstance summonerChar)
	{
		if (target == null || !(target instanceof L2PcInstance))
			return false;
		
		L2PcInstance targetChar = (L2PcInstance) target;
		
		if (targetChar.isAlikeDead())
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.C1_IS_DEAD_AT_THE_MOMENT_AND_CANNOT_BE_SUMMONED);
			sm.addPcName(targetChar);
			summonerChar.sendPacket(sm);
			return false;
		}

		if (targetChar.isInStoreMode())
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.C1_CURRENTLY_TRADING_OR_OPERATING_PRIVATE_STORE_AND_CANNOT_BE_SUMMONED);
			sm.addPcName(targetChar);
			summonerChar.sendPacket(sm);
			return false;
		}

		if (targetChar.isRooted() || targetChar.isInCombat())
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.C1_IS_ENGAGED_IN_COMBAT_AND_CANNOT_BE_SUMMONED);
			sm.addPcName(targetChar);
			summonerChar.sendPacket(sm);
			return false;
		}

		if (targetChar.isInOlympiadMode())
		{
			summonerChar.sendPacket(new SystemMessage(SystemMessageId.YOU_CANNOT_SUMMON_PLAYERS_WHO_ARE_IN_OLYMPIAD));
			return false;
		}

		if (targetChar.isFestivalParticipant() || targetChar.isFlyingMounted())
		{
			summonerChar.sendPacket(new SystemMessage(SystemMessageId.YOUR_TARGET_IS_IN_AN_AREA_WHICH_BLOCKS_SUMMONING));
			return false;
		}

		if (targetChar.inObserverMode())
		{
			summonerChar.sendPacket(new SystemMessage(SystemMessageId.C1_STATE_FORBIDS_SUMMONING).addCharName(targetChar));
			return false;
		}

		if (targetChar.isCombatFlagEquipped())
		{
			summonerChar.sendPacket(new SystemMessage(SystemMessageId.YOUR_TARGET_IS_IN_AN_AREA_WHICH_BLOCKS_SUMMONING));
			return false;
		}

		if (!TvTEvent.onEscapeUse(targetChar.getObjectId()))
		{
			summonerChar.sendPacket(new SystemMessage(SystemMessageId.YOUR_TARGET_IS_IN_AN_AREA_WHICH_BLOCKS_SUMMONING));
			return false;			
		}

		if (targetChar.isInsideZone(L2Character.ZONE_NOSUMMONFRIEND))
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.C1_IN_SUMMON_BLOCKING_AREA);
			sm.addString(targetChar.getName());
			summonerChar.sendPacket(sm);
			return false;
		}
		
		if (summonerChar.getInstanceId() > 0)
		{
			Instance summonerInstance = InstanceManager.getInstance().getInstance(summonerChar.getInstanceId());
			if (!Config.ALLOW_SUMMON_TO_INSTANCE || !summonerInstance.isSummonAllowed())
			{
				summonerChar.sendPacket(new SystemMessage(SystemMessageId.YOU_MAY_NOT_SUMMON_FROM_YOUR_CURRENT_LOCATION));
				return false;
			}
		}

		// on retail character can enter 7s dungeon with summon friend,
		// but will be teleported away by mobs
		// because currently this is not working in L2J we do not allowing summoning
		if (summonerChar.isIn7sDungeon())
		{
			int targetCabal = SevenSigns.getInstance().getPlayerCabal(targetChar.getObjectId());
			if (SevenSigns.getInstance().isSealValidationPeriod())
			{
				if (targetCabal != SevenSigns.getInstance().getCabalHighestScore())
				{
					summonerChar.sendPacket(new SystemMessage(SystemMessageId.YOUR_TARGET_IS_IN_AN_AREA_WHICH_BLOCKS_SUMMONING));
					return false;					
				}
			}
			else
			{
				if (targetCabal == SevenSigns.CABAL_NULL)
				{
					summonerChar.sendPacket(new SystemMessage(SystemMessageId.YOUR_TARGET_IS_IN_AN_AREA_WHICH_BLOCKS_SUMMONING));
					return false;
				}
			}
		}

		return true;
	}

	public void gatesRequest(L2DoorInstance door)
	{
		_gatesRequest.setTarget(door);
	}

	public void gatesAnswer(int answer, int type)
	{
		if (_gatesRequest.getDoor() == null)
			return;

		if (answer == 1 && getTarget() == _gatesRequest.getDoor() && type == 1)
			_gatesRequest.getDoor().openMe();
		else if (answer == 1 && getTarget() == _gatesRequest.getDoor() && type == 0)
			_gatesRequest.getDoor().closeMe();

		_gatesRequest.setTarget(null);
	}

	@Override
	public void setIsCastingNow(boolean value)
	{
		if (value == false)
			_currentSkill = null;

		super.setIsCastingNow(value);
	}

	public void checkItemRestriction()
	{
		for (int i = 0; i < Inventory.PAPERDOLL_TOTALSLOTS; i++)
		{
			L2ItemInstance equippedItem = getInventory().getPaperdollItem(i);
			if (equippedItem != null && !equippedItem.getItem().checkCondition(this, this, false))
			{
				getInventory().unEquipItemInSlotAndRecord(i);

				InventoryUpdate iu = new InventoryUpdate();
				iu.addModifiedItem(equippedItem);
				sendPacket(iu);

				if (equippedItem.isWear())
					continue;

				// prevent double message for two-hand weapons
				if (i == Inventory.PAPERDOLL_LRHAND)
					continue;

				SystemMessage sm = null;
				if (equippedItem.getItem().getBodyPart() == L2Item.SLOT_BACK)
				{
					sendPacket(new SystemMessage(SystemMessageId.CLOAK_REMOVED_BECAUSE_ARMOR_SET_REMOVED));
					return;
				}

				if (equippedItem.getEnchantLevel() > 0)
				{
					sm = new SystemMessage(SystemMessageId.EQUIPMENT_S1_S2_REMOVED);
					sm.addNumber(equippedItem.getEnchantLevel());
					sm.addItemName(equippedItem);
				}
				else
				{
					sm = new SystemMessage(SystemMessageId.S1_DISARMED);
					sm.addItemName(equippedItem);
				}
				sendPacket(sm);
			}
		}
	}

    public void setTransformAllowedSkills(int[] ids)
    {
    	_transformAllowedSkills = ids;
    }
    
    public boolean containsAllowedTransformSkill(int id)
    {
    	for (int i = 0; i < _transformAllowedSkills.length; i++)
    	{
    		if (_transformAllowedSkills[i] == id)
    		{
    			return true;
    		}
    	}
    	return false;
    }
    
    /** Section for mounted pets */
    class FeedTask implements Runnable
    {
    	public void run()
    	{
    		try
    		{
    			if (!isMounted())
    			{
    				stopFeed();
    				return;
    			}
    			
    			if (getCurrentFeed() > getFeedConsume())
    			{
    				// eat
    				setCurrentFeed(getCurrentFeed()-getFeedConsume());
    			}
    			else
    			{
    				// go back to pet control item, or simply said, unsummon it
    				setCurrentFeed(0);
    				stopFeed();
    				dismount();
    				sendPacket(new SystemMessage(SystemMessageId.OUT_OF_FEED_MOUNT_CANCELED));
    			}
    			
    			int[] foodIds = PetDataTable.getFoodItemId(getMountNpcId());
    			if (foodIds[0] == 0) return;
    			L2ItemInstance food = null;
    			food = getInventory().getItemByItemId(foodIds[0]);
    			
    			// use better strider food if exists
    			if (PetDataTable.isStrider(getMountNpcId()))
    			{
    				if (getInventory().getItemByItemId(foodIds[1]) != null)
    					food = getInventory().getItemByItemId(foodIds[1]);
    			}
    			if (food != null && isHungry())
    			{
    				IItemHandler handler = ItemHandler.getInstance().getItemHandler(food.getEtcItem());
    				if (handler != null)
    				{
    					handler.useItem(L2PcInstance.this, food);
    					SystemMessage sm = new SystemMessage(SystemMessageId.PET_TOOK_S1_BECAUSE_HE_WAS_HUNGRY);
    					sm.addItemName(food.getItemId());
    					sendPacket(sm);
    				}
    			}
    		}
    		catch (Exception e)
    		{
    			_log.log(Level.SEVERE, "Mounted Pet [NpcId: "+getMountNpcId()+"] a feed task error has occurred", e);
    		}
    	}
    }
    	
    protected synchronized void startFeed(int npcId)
    {
    	_canFeed = npcId > 0;
    	if (!isMounted())
    		return;
    	if (getPet() != null)
    	{
    		setCurrentFeed(((L2PetInstance) getPet()).getCurrentFed());
    		_controlItemId = getPet().getControlItemId();
    		sendPacket(new SetupGauge(3, getCurrentFeed()*10000/getFeedConsume(), getMaxFeed()*10000/getFeedConsume()));
    		if (!isDead())
    		{
    			_mountFeedTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new FeedTask(), 10000, 10000);
    		}
    	}
    	else if (_canFeed)
    	{
    		setCurrentFeed(getMaxFeed());
    		SetupGauge sg = new SetupGauge(3, getCurrentFeed()*10000/getFeedConsume(), getMaxFeed()*10000/getFeedConsume());
    		sendPacket(sg);
    		if (!isDead())
    		{
    			_mountFeedTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new FeedTask(), 10000, 10000);
    		}
    	}
    }
    
    protected synchronized void stopFeed()
    {
    	if (_mountFeedTask != null)
    	{
    		_mountFeedTask.cancel(false);
    		ThreadPoolManager.getInstance().removeGeneral((Runnable)_mountFeedTask);
    		_mountFeedTask = null;
    		if (Config.DEBUG) _log.fine("Pet [#"+_mountNpcId+"] feed task stop");
    	}
    }
    
    protected final void clearPetData()
    {
    	_data = null;
    }
    
    protected final L2PetData getPetData(int npcId)
    {
    	if (_data == null && getPet() != null)
    		_data = PetDataTable.getInstance().getPetData(getPet().getNpcId(), getPet().getLevel());
    	else if (_data == null && npcId > 0)
    	{
    		_data = PetDataTable.getInstance().getPetData(npcId, getLevel());
    	}
    	
    	return _data;
    }
    
    public int getCurrentFeed() { return _curFeed; }
    
    protected int getFeedConsume()
    {
    	// if pet is attacking
    	if (isAttackingNow())
    		return getPetData(_mountNpcId).getPetFeedBattle();
    	else
    		return getPetData(_mountNpcId).getPetFeedNormal();
    }
    
    public void setCurrentFeed(int num)
    {
    	_curFeed = num > getMaxFeed() ? getMaxFeed() : num;
    	SetupGauge sg = new SetupGauge(3, getCurrentFeed()*10000/getFeedConsume(), getMaxFeed()*10000/getFeedConsume());
    	sendPacket(sg);
    }
    
    protected int getMaxFeed()
    {
    	return getPetData(_mountNpcId).getPetMaxFeed();
    }
    
    protected boolean isHungry()
    {
    	return _canFeed ? (getCurrentFeed() < (0.55 * getPetData(getMountNpcId()).getPetMaxFeed())):false;
    }
    
	public class Dismount implements Runnable
	{
		public void run()
		{
			try
			{
				L2PcInstance.this.dismount();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	public void enteredNoLanding(int delay)
	{
		_dismountTask = ThreadPoolManager.getInstance().scheduleGeneral(new L2PcInstance.Dismount(), delay * 1000);
	}

	public void exitedNoLanding()
	{
		if (_dismountTask != null)
		{
			_dismountTask.cancel(true);
			_dismountTask = null;
		}
	}
    
    public void storePetFood(int petId)
    {
    	if (_controlItemId != 0 && petId != 0)
    	{
    		String req;
    		req = "UPDATE pets SET fed=? WHERE item_obj_id = ?";
    		Connection con = null;
    		try
    		{
    			con = L2DatabaseFactory.getInstance().getConnection();
    			PreparedStatement statement = con.prepareStatement(req);
    			statement.setInt(1, getCurrentFeed());
    			statement.setInt(2, _controlItemId);
    			statement.executeUpdate();
    			statement.close();
    			_controlItemId = 0;
    		}
    		catch (Exception e)
    		{
    			_log.log(Level.SEVERE, "Failed to store Pet [NpcId: "+petId+"] data", e);
    		}
    		finally
    		{
    			try
    			{
    				con.close();
    			}
    			catch (Exception e)
    			{
    			}
    		}
    	}
    }
    /** End of section for mounted pets */

    @Override
    public int getAttackElementValue(byte attribute)
    {
    	int value = super.getAttackElementValue(attribute);

    	// 20% if summon exist
    	if (getPet() != null && getClassId().isSummoner() && (getPet() instanceof L2SummonInstance))
    		return value / 5;

    	return value;
    }

	/**
	 * @return event effect id
	 */
	public int getEventEffectId()
	{
		return _eventEffectId ;
	}

	public void startEventEffect(AbnormalEffect mask)
	{
		_eventEffectId |= mask.getMask();
		broadcastUserInfo();
	}

	public void stopEventEffect(AbnormalEffect mask)
	{
		_eventEffectId &= ~mask.getMask();
		broadcastUserInfo();
	}

    public void setIsInSiege(boolean b)
    {
    	_isInSiege = b;
    }
    
    public boolean isInSiege()
    {
    	return _isInSiege;
    }
    
    public FloodProtectors getFloodProtectors()
    {
    	return _floodProtectors;
    }

    public boolean isFlyingMounted()
    {
	    return _isFlyingMounted;
    }
    
    public void setIsFlyingMounted(boolean val)
    {
    	_isFlyingMounted = val;
    	setIsFlying(val);
    }
    
    /**
     * Returns the Number of Charges this L2PcInstance got.
     * @return
     */
    public int getCharges()
    {
    	return _charges.get();
    }
    
    public synchronized void increaseCharges(int count, int max)
    {
    	if (_charges.get() >= max)
    	{
    		sendPacket(new SystemMessage(SystemMessageId.FORCE_MAXLEVEL_REACHED));
    		return;
    	}
    	else
    	{
    		// if no charges - start clear task
    		if (_charges.get() == 0)
    			restartChargeTask();
    			}
    	
    	if (_charges.addAndGet(count) >= max)
    	{
    		_charges.set(max);
    		sendPacket(new SystemMessage(SystemMessageId.FORCE_MAXLEVEL_REACHED));
    	}
    	else
    	{
    		SystemMessage sm = new SystemMessage(SystemMessageId.FORCE_INCREASED_TO_S1);
    		sm.addNumber(_charges.get());
    		sendPacket(sm);
    	}
    	
    	sendPacket(new EtcStatusUpdate(this));
    }
    
    public synchronized boolean decreaseCharges(int count)
    {
    	if (_charges.get() < count)
    		return false;
    	
    	if (_charges.addAndGet(-count) == 0)
    		stopChargeTask();
    	
    	sendPacket(new EtcStatusUpdate(this));
    	return true;
    }
    
    public void clearCharges()
    {
    	_charges.set(0);
    	sendPacket(new EtcStatusUpdate(this));
    }
    
    /**
     * Starts/Restarts the ChargeTask to Clear Charges after 10 Mins.
     */
    private void restartChargeTask()
    {
    	if (_chargeTask != null)
    	{
    		_chargeTask.cancel(false);
    		_chargeTask = null;
    	}
    	_chargeTask = ThreadPoolManager.getInstance().scheduleGeneral(new ChargeTask(this), 600000);
    }
    
    /**
     * Stops the Charges Clearing Task.
     */
    public void stopChargeTask()
    {
    	if (_chargeTask != null)
    	{
    		_chargeTask.cancel(false);
    		ThreadPoolManager.getInstance().removeGeneral((Runnable)_chargeTask);
    		_chargeTask = null;
    	}
    }
    
    private class ChargeTask implements Runnable
    {
    	L2PcInstance _player;
    	
    	protected ChargeTask(L2PcInstance player)
    	{
    		_player = player;
    	}
    	
    	public void run()
    	{
    		_player.clearCharges();
    	}
    }
    
    public class TeleportBookmark
    {
    	public int _id,_x,_y,_z,_icon;
    	public String _name,_tag;
    	
    	TeleportBookmark(int id, int x, int y, int z, int icon, String tag, String name)
    	{
    		_id = id;
    		_x = x;
    		_y = y;
    		_z = z;
    		_icon = icon;
    		_name = name;
    		_tag = tag;
    	}
    	
    }
    
    public void teleportBookmarkModify(int Id, int icon, String tag, String name)
    {
    	int count = 0;
    	int size = tpbookmark.size();
    	while(size > count)
    	{
    		if(tpbookmark.get(count)._id==Id)
    		{
    			tpbookmark.get(count)._icon = icon;
    			tpbookmark.get(count)._tag = tag;
    			tpbookmark.get(count)._name = name;
    			
    			Connection con = null;

    			try
    			{

    				con = L2DatabaseFactory.getInstance().getConnection();
    				PreparedStatement statement = con.prepareStatement(UPDATE_TP_BOOKMARK);
    				
    				statement.setInt(1, icon);
    				statement.setString(2, tag);
    				statement.setString(3, name);
    				statement.setInt(4, getObjectId());
    				statement.setInt(5, Id);

    				statement.execute();
    				statement.close();
    			}
    			catch (Exception e) { _log.warning("Could not update character teleport bookmark data: "+ e); }
    			finally { try { con.close(); } catch (Exception e) {} }
    			
    		}
    		count++;
    	}
    	
    	sendPacket(new ExGetBookMarkInfoPacket(this));
    	
    }
    
    public void teleportBookmarkDelete(int Id)
    {
		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(DELETE_TP_BOOKMARK);
			
			statement.setInt(1, getObjectId());
			statement.setInt(2, Id);

			statement.execute();
			statement.close();
		}
		catch (Exception e) { _log.warning("Could not delete character teleport bookmark data: "+ e); }
		finally { try { con.close(); } catch (Exception e) {} }
		
		int count = 0;
		int size = tpbookmark.size();
		
    	while(size > count)
    	{
    		if(tpbookmark.get(count)._id == Id)
    		{
    			tpbookmark.remove(count);
    			break;
    		}
    		count++;
    	}
    	
		sendPacket(new ExGetBookMarkInfoPacket(this));
    }
    
    public void teleportBookmarkGo(int Id)
    {
    	if(!teleportBookmarkCondition(0) || this == null)
    		return;
    	if (getInventory().getInventoryItemCount(13016, 0) == 0)
    	{
    		sendPacket(new SystemMessage(2359));
    		return;
    	}
    	SystemMessage sm = new SystemMessage(SystemMessageId.S1_DISAPPEARED);
    	sm.addItemName(13016);
    	sendPacket(sm);
    	int count = 0;
    	int size = tpbookmark.size();
    	while(size > count)
    	{
    		if(tpbookmark.get(count)._id == Id)
    		{
    			destroyItem("Consume", getInventory().getItemByItemId(13016).getObjectId(), 1, null, false);
    			this.teleToLocation(tpbookmark.get(count)._x, tpbookmark.get(count)._y, tpbookmark.get(count)._z);
    			break;
    		}
    		count++;
    	}
    	sendPacket(new ExGetBookMarkInfoPacket(this));
    }
    
    public boolean teleportBookmarkCondition(int type)
    {
    	if(this.isInCombat())
    	{
    		sendPacket(new SystemMessage(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_DURING_A_BATTLE));
    		return false;
    	}
    	else if (this.isInSiege() || this.getSiegeState() != 0)
    	{
    		sendPacket(new SystemMessage(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_WHILE_PARTICIPATING));
    		return false;
    	}
    	else if (this.isInDuel())
    	{
    		sendPacket(new SystemMessage(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_DURING_A_DUEL));
    		return false;
    	}
    	else if (this.isFlying())
    	{
    		sendPacket(new SystemMessage(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_WHILE_FLYING));
    		return false;
    	}
    	else if (this.isInOlympiadMode())
    	{
    		sendPacket(new SystemMessage(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_WHILE_PARTICIPATING_IN_AN_OLYMPIAD_MATCH));
    		return false;
    	}
    	else if (this.isParalyzed())
    	{
    		sendPacket(new SystemMessage(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_WHILE_YOU_ARE_PARALYZED));
    		return false;
    	}
    	else if (this.isDead())
    	{
    		sendPacket(new SystemMessage(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_WHILE_YOU_ARE_DEAD));
    		return false;
    	}
    	else if (type == 1 && (isIn7sDungeon() || (isInParty() && getParty().isInDimensionalRift())))
    	{
    		sendPacket(new SystemMessage(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_TO_REACH_THIS_AREA));
    		return false;
    	}
    	else if (this.isInBoat() || this.isInAirShip() || this.isInJail() || this.isInsideZone(ZONE_NOSUMMONFRIEND))
    	{
    		if(type == 0)
    			sendPacket(new SystemMessage(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_IN_THIS_AREA));
    		else if (type == 1)
    			sendPacket(new SystemMessage(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_TO_REACH_THIS_AREA));
    		return false;
    	}
    	else if (this.isInWater())
    	{
    		sendPacket(new SystemMessage(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_UNDERWATER));
    		return false;
    	}
    	else if (type == 1 && (this.isInsideZone(ZONE_SIEGE) || this.isInsideZone(ZONE_CLANHALL) || this.isInsideZone(ZONE_JAIL) || this.isInsideZone(ZONE_CASTLE) || this.isInsideZone(ZONE_NOSUMMONFRIEND) || this.isInsideZone(ZONE_FORT)))
    	{
    		sendPacket(new SystemMessage(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_TO_REACH_THIS_AREA));
    		return false;
    	}
    	/* TODO: Instant Zone still not implement
    	else if (this.isInsideZone(ZONE_INSTANT))
    	{
    		sendPacket(new SystemMessage(2357));
    		return;
    	}
    	*/
    	else 
    		return true;
    }
    
    public void teleportBookmarkAdd(int x,int y,int z,int icon, String tag, String name)
    {
    	if(this == null)
    		return;
    	
    	if(!teleportBookmarkCondition(1))
    		return;
    	
    	if(tpbookmark.size() >= _bookmarkslot)
    	{
    		sendPacket(new SystemMessage(2358));
    		return;
    	}
    	
    	if(getInventory().getInventoryItemCount(20033, 0) == 0)
    	{
    		sendPacket(new SystemMessage(6501));
    		return;
    	}

    	int count = 0;
    	int id = 1;
    	FastList<Integer> idlist = new FastList<Integer>();
    	
    	int size = tpbookmark.size();
    	
    	while(size > count)
    	{
    		idlist.add(tpbookmark.get(count)._id);
    		count++;
    	}
    	
    	for(int i=1; i<10; i++)
    	{
    		if(!idlist.contains(i))
    		{
    			id = i;
    			break;
    		}
    	}
    	
    	TeleportBookmark tpadd = new TeleportBookmark(id, x, y, z, icon, tag, name);
    	if(tpbookmark == null)
    		tpbookmark = new FastList<TeleportBookmark>();
    	
    	tpbookmark.add(tpadd);

    	destroyItem("Consume", getInventory().getItemByItemId(20033).getObjectId(), 1, null, false);
    	
    	SystemMessage sm = new SystemMessage(SystemMessageId.S1_DISAPPEARED);
    	sm.addItemName(20033);
    	sendPacket(sm);
    	
    	Connection con = null;

		try
		{

			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(INSERT_TP_BOOKMARK);
			
			statement.setInt(1, getObjectId());
			statement.setInt(2, id);
			statement.setInt(3, x);
			statement.setInt(4, y);
			statement.setInt(5, z);
			statement.setInt(6, icon);
			statement.setString(7, tag);
			statement.setString(8, name);


			statement.execute();
			statement.close();
		}
		catch (Exception e) { _log.warning("Could not insert character teleport bookmark data: "+ e); }
		finally { try { con.close(); } catch (Exception e) {} }
		
		sendPacket(new ExGetBookMarkInfoPacket(this));
    }
    
    public void restoreTeleportBookmark()
    {
    	if(tpbookmark == null)
    		tpbookmark = new FastList<TeleportBookmark>();
    	Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(RESTORE_TP_BOOKMARK);
			statement.setInt(1, getObjectId());
			ResultSet rset = statement.executeQuery();

			while (rset.next())
			{
				tpbookmark.add(new TeleportBookmark(rset.getInt("Id"), rset.getInt("x"), rset.getInt("y"), rset.getInt("z"), rset.getInt("icon"), rset.getString("tag"), rset.getString("name")));
			}

			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
		    _log.log(Level.SEVERE, "Failed restoing character teleport bookmark.", e);
		}
		finally
		{
			try { con.close(); } catch (Exception e) {}
		}
    }
    
    @Override
    public void sendInfo(L2PcInstance activeChar)
    {
    	if(isInBoat())
        {
        	getPosition().setWorldPosition(getBoat().getPosition().getWorldPosition());

            activeChar.sendPacket(new CharInfo(this));
            activeChar.sendPacket(new ExBrExtraUserInfo(this));
            int relation1 = getRelation(activeChar);
        	int relation2 = activeChar.getRelation(this);
        	if (getKnownList().getKnownRelations().get(activeChar.getObjectId()) != null && getKnownList().getKnownRelations().get(activeChar.getObjectId()) != relation1)
        	{
        		activeChar.sendPacket(new RelationChanged(this, relation1, activeChar.isAutoAttackable(this)));
        		if (getPet() != null)
        			activeChar.sendPacket(new RelationChanged(getPet(), relation1, activeChar.isAutoAttackable(this)));
        	}
        	if (activeChar.getKnownList().getKnownRelations().get(getObjectId()) != null && activeChar.getKnownList().getKnownRelations().get(getObjectId()) != relation2)
        	{
        		sendPacket(new RelationChanged(activeChar, relation2, isAutoAttackable(activeChar)));
        		if (activeChar.getPet() != null)
        			sendPacket(new RelationChanged(activeChar.getPet(), relation2, isAutoAttackable(activeChar)));
        	}
        	activeChar.sendPacket(new GetOnVehicle(this, getBoat(), getInBoatPosition().getX(), getInBoatPosition().getY(), getInBoatPosition().getZ()));
        	/*if(getBoat().GetVehicleDeparture() == null)
        	{

        		int xboat = getBoat().getX();
        		int yboat= getBoat().getY();
        		double modifier = Math.PI/2;
        		if (yboat == 0)
        		{
        			yboat = 1;
        		}
        		if(yboat < 0)
        		{
        			modifier = -modifier;
        		}
        		double angleboat = modifier - Math.atan(xboat/yboat);
        		int xp = getX();
        		int yp = getY();
        		modifier = Math.PI/2;
        		if (yp == 0)
        		{
        			yboat = 1;
        		}
        		if(yboat < 0)
        		{
        			modifier = -modifier;
        		}
        		double anglep = modifier - Math.atan(yp/xp);

        		double finx = Math.cos(anglep - angleboat)*Math.sqrt(xp *xp +yp*yp ) + Math.cos(angleboat)*Math.sqrt(xboat *xboat +yboat*yboat );
        		double finy = Math.sin(anglep - angleboat)*Math.sqrt(xp *xp +yp*yp ) + Math.sin(angleboat)*Math.sqrt(xboat *xboat +yboat*yboat );
        		//getPosition().setWorldPosition(getBoat().getX() - getInBoatPosition().x,getBoat().getY() - getInBoatPosition().y,getBoat().getZ()- getInBoatPosition().z);
        		getPosition().setWorldPosition((int)finx,(int)finy,getBoat().getZ()- getInBoatPosition().z);

        	}*/
        }
        else if(isInAirShip())
        {
        	getPosition().setWorldPosition(getAirShip().getPosition().getWorldPosition());

            activeChar.sendPacket(new CharInfo(this));
            activeChar.sendPacket(new ExBrExtraUserInfo(this));
            int relation1 = getRelation(activeChar);
        	int relation2 = activeChar.getRelation(this);
        	if (getKnownList().getKnownRelations().get(activeChar.getObjectId()) != null && getKnownList().getKnownRelations().get(activeChar.getObjectId()) != relation1)
        	{
        		activeChar.sendPacket(new RelationChanged(this, relation1, activeChar.isAutoAttackable(this)));
        		if (getPet() != null)
        			activeChar.sendPacket(new RelationChanged(getPet(), relation1, activeChar.isAutoAttackable(this)));
        	}
        	if (activeChar.getKnownList().getKnownRelations().get(getObjectId()) != null && activeChar.getKnownList().getKnownRelations().get(getObjectId()) != relation2)
        	{
        		sendPacket(new RelationChanged(activeChar, relation2, isAutoAttackable(activeChar)));
        		if (activeChar.getPet() != null)
        			sendPacket(new RelationChanged(activeChar.getPet(), relation2, isAutoAttackable(activeChar)));
        	}
        	activeChar.sendPacket(new ExGetOnAirShip(this, getAirShip()));
        }
        else
        {
        	activeChar.sendPacket(new CharInfo(this));
        	activeChar.sendPacket(new ExBrExtraUserInfo(this));
        	int relation1 = getRelation(activeChar);
        	int relation2 = activeChar.getRelation(this);
        	if (getKnownList().getKnownRelations().get(activeChar.getObjectId()) != null && getKnownList().getKnownRelations().get(activeChar.getObjectId()) != relation1)
        	{
        		activeChar.sendPacket(new RelationChanged(this, relation1, activeChar.isAutoAttackable(this)));
        		if (getPet() != null)
        			activeChar.sendPacket(new RelationChanged(getPet(), relation1, activeChar.isAutoAttackable(this)));
        	}
        	if (activeChar.getKnownList().getKnownRelations().get(getObjectId()) != null && activeChar.getKnownList().getKnownRelations().get(getObjectId()) != relation2)
        	{
        		sendPacket(new RelationChanged(activeChar, relation2, isAutoAttackable(activeChar)));
        		if (activeChar.getPet() != null)
        			sendPacket(new RelationChanged(activeChar.getPet(), relation2, isAutoAttackable(activeChar)));
        	}
        }
        if (getMountType() == 4)
        {
               // TODO: Remove when horse mounts fixed
               activeChar.sendPacket(new Ride(this, false, 0));
               activeChar.sendPacket(new Ride(this, true, getMountNpcId()));
        }

        switch (getPrivateStoreType())
        {
        	case L2PcInstance.STORE_PRIVATE_SELL:
        		activeChar.sendPacket(new PrivateStoreMsgSell(this));
        		break;
        	case L2PcInstance.STORE_PRIVATE_PACKAGE_SELL:
        		activeChar.sendPacket(new ExPrivateStoreSetWholeMsg(this));
        		break;
        	case L2PcInstance.STORE_PRIVATE_BUY:
        		activeChar.sendPacket(new PrivateStoreMsgBuy(this));
        		break;
        	case L2PcInstance.STORE_PRIVATE_MANUFACTURE:
        		activeChar.sendPacket(new RecipeShopMsg(this));
        		break;
        }
    }
    
    public void showQuestMovie(int id)
	{
		if (_movieId > 0) //already in movie
			return;
		abortAttack();
		abortCast();
		stopMove(null);
		_movieId = id;
		sendPacket(new ExStartScenePlayer(id));
	}
	
	public boolean isAllowedToEnchantSkills()
	{
		if (isLocked())
			return false;
		if (isTransformed())
			return false;
		if (AttackStanceTaskManager.getInstance().getAttackStanceTask(this))
			return false;
		if (isCastingNow() || isCastingSimultaneouslyNow())
			return false;
		if (isInBoat() || isInAirShip())
			return false;
		return true;
	}
	
	/** 
	 * Set the _creationTime of the L2PcInstance.<BR><BR> 
	 */
	public void setCreateTime(long creationTime)
	{
		_creationTime = creationTime;
	}

	/** 
	 * Return the _creationTime of the L2PcInstance.<BR><BR> 
	 */ 
	public long getCreateTime()
	{
		return _creationTime;
	}
	
	/**
	 * @return number of days to char birthday.<BR><BR> 
	 */
	public int checkBirthDay()
	{
		QuestState _state = getQuestState("CharacterBirthday");
		Calendar now = Calendar.getInstance();
		Calendar birth = Calendar.getInstance();
		now.setTimeInMillis(System.currentTimeMillis());
		birth.setTimeInMillis(_creationTime);
		
		if (_state != null && _state.getInt("Birthday") > now.get(Calendar.YEAR))
			return -1;
		
		// "Characters with a February 29 creation date will receive a gift on February 28."
		if (birth.get(Calendar.DAY_OF_MONTH) == 29 && birth.get(Calendar.MONTH) == 1)
			birth.add(Calendar.HOUR_OF_DAY, -24);
		
		if (now.get(Calendar.MONTH) == birth.get(Calendar.MONTH)
				&& now.get(Calendar.DAY_OF_MONTH) == birth.get(Calendar.DAY_OF_MONTH)
				&& now.get(Calendar.YEAR) != birth.get(Calendar.YEAR))
		{
			return 0;
		}
		else
		{
			int i;
			for (i = 1; i < 6; i++)
			{
				now.add(Calendar.HOUR_OF_DAY, 24);
				if (now.get(Calendar.MONTH) == birth.get(Calendar.MONTH)
						&& now.get(Calendar.DAY_OF_MONTH) == birth.get(Calendar.DAY_OF_MONTH)
						&& now.get(Calendar.YEAR) != birth.get(Calendar.YEAR))
					return i;
			}
		}
		return -1;
	}
	
	
	/**
	 * list of character friends
	 * 
	 */
	private List<Integer> _friendList = new FastList<Integer>();
	
	public List<Integer> getFriendList()
	{
		return _friendList;
	}
	
	public void restoreFriendList()
	{
		_friendList.clear();
		
		Connection con = null;
		
		try
		{
			String sqlQuery = "SELECT friendId FROM character_friends WHERE charId=? AND relation=0";
			
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(sqlQuery);
			statement.setInt(1, getObjectId());
			ResultSet rset = statement.executeQuery();
			
			int friendId;
			while (rset.next())
			{
				friendId = rset.getInt("friendId");
				if (friendId == getObjectId())
					continue;
				_friendList.add(friendId);
			}
			
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("Error found in " + getName() + "'s FriendList: " + e);
		}
		finally
		{
			try	{ con.close(); } catch (Exception e) {}
		}
	}

	/**
	 * 
	 */
	private void notifyFriends()
	{
		FriendStatusPacket pkt = new FriendStatusPacket(getObjectId());
		for(int id : _friendList)
		{
			L2Object obj = L2World.getInstance().findObject(id);
			if (obj != null)
				obj.sendPacket(pkt);
		}
	}

	/**
	 * @return the _silenceMode
	 */
	public boolean isSilenceMode()
	{
		return _silenceMode;
	}

	/**
	 * @param mode the _silenceMode to set
	 */
	public void setSilenceMode(boolean mode)
	{
		_silenceMode = mode;
		sendPacket(new EtcStatusUpdate(this));
	}
	
	private void storeRecipeShopList()
	{
		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			L2ManufactureList list = getCreateList();

			if (list != null && list.size() > 0)
			{
				int _position = 1;
				statement = con.prepareStatement("DELETE FROM character_recipeshoplist WHERE charId=? ");
				statement.setInt(1, getObjectId());
				statement.execute();
				statement.close();
				
				PreparedStatement statement2 = con.prepareStatement("INSERT INTO character_recipeshoplist (charId, Recipeid, Price, Pos) VALUES (?, ?, ?, ?)");
				for (L2ManufactureItem item : list.getList())
				{
					statement2.setInt(1, getObjectId());
					statement2.setInt(2, item.getRecipeId());
					statement2.setLong(3, item.getCost());
					statement2.setInt(4, _position);
					statement2.execute();
					statement2.clearParameters();
					_position++;
				}
				statement2.close();
			}
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Could not store recipe shop for playerID " + getObjectId() + ": ", e);
		}
		finally
		{
			try
			{
				if (con != null)
					con.close();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	private void restoreRecipeShopList()
	{
		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT Recipeid,Price FROM character_recipeshoplist WHERE charId=? ORDER BY Pos ASC");
			statement.setInt(1, getObjectId());
			ResultSet rset = statement.executeQuery();
			
			
			L2ManufactureList createList = new L2ManufactureList();
			while (rset.next())
			{
				createList.add(new L2ManufactureItem(rset.getInt("Recipeid"), rset.getLong("Price")));
			}
			setCreateList(createList);
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Could not restore recipe shop list data for playerId: "+getObjectId(), e);
		}
		finally
		{
			try
			{
				if (con != null)
					con.close();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
		}
	}

	public double getCollisionRadius()
	{
		if (getAppearance().getSex())
			return getBaseTemplate().fCollisionRadius_female;
		else
			return getBaseTemplate().fCollisionRadius;
	}
	
	public double getCollisionHeight()
	{
		if (getAppearance().getSex())
			return getBaseTemplate().fCollisionHeight_female;
		else
			return getBaseTemplate().fCollisionHeight;
	}
	
	public final int getClientX()
	{
		return _clientX;
	}
	public final int getClientY()
	{
		return _clientY;
	}
	public final int getClientZ()
	{
		return _clientZ;
	}
	public final int getClientHeading()
	{
		return _clientHeading;
	}
	public final void setClientX(int val)
	{
		_clientX=val;
	}
	public final void setClientY(int val)
	{
		_clientY=val;
	}
	public final void setClientZ(int val)
	{
		_clientZ=val;
	}
	public final void setClientHeading(int val)
	{
		_clientHeading=val;
	}

	/**
	 * @return the _movieId
	 */
	public int getMovieId()
	{
		return _movieId;
	}
	
	public void setMovieId(int id)
	{
		_movieId = id;
	}

	/* (non-Javadoc)
	 * @see com.l2jserver.gameserver.model.actor.L2Character#isMovementDisabled()
	 */
	@Override
	public boolean isMovementDisabled()
	{
		return super.isMovementDisabled() || _movieId > 0;
	}
	
	private void restoreUISettings()
	{
		_uiKeySettings = new L2UIKeysSettings(this);
	}
	
	private void storeUISettings()
	{
		if (_uiKeySettings == null)
			return;
		
		if (!_uiKeySettings.isSaved())
			_uiKeySettings.saveInDB();
	}
	
	public L2UIKeysSettings getUISettings()
	{
		return _uiKeySettings;
	}

	public String getHtmlPrefix()
	{
		if (!Config.L2JMOD_MULTILANG_ENABLE)
			return null;

		return _htmlPrefix;
	}

	public String getLang()
	{
		return _lang;
	}

	public boolean setLang(String lang)
	{
		boolean result = false;
		if (Config.L2JMOD_MULTILANG_ENABLE)
		{
			if (Config.L2JMOD_MULTILANG_ALLOWED.contains(lang))
			{
				_lang = lang;
				result = true;
			}
			else
				_lang = Config.L2JMOD_MULTILANG_DEFAULT;

			_htmlPrefix = "data/lang/" + _lang + "/";
		}
		else
		{
			_lang = null;
			_htmlPrefix = null;
		}

		return result;
	}
}
