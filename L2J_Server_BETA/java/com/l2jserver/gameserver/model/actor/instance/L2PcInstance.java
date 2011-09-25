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

import gnu.trove.TIntArrayList;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.util.FastSet;

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
import com.l2jserver.gameserver.datatables.CharSummonTable;
import com.l2jserver.gameserver.datatables.CharTemplateTable;
import com.l2jserver.gameserver.datatables.ClanTable;
import com.l2jserver.gameserver.datatables.EnchantGroupsTable;
import com.l2jserver.gameserver.datatables.ExperienceTable;
import com.l2jserver.gameserver.datatables.FishTable;
import com.l2jserver.gameserver.datatables.HennaTable;
import com.l2jserver.gameserver.datatables.HeroSkillTable;
import com.l2jserver.gameserver.datatables.ItemTable;
import com.l2jserver.gameserver.datatables.NobleSkillTable;
import com.l2jserver.gameserver.datatables.NpcTable;
import com.l2jserver.gameserver.datatables.PetDataTable;
import com.l2jserver.gameserver.datatables.SkillTable;
import com.l2jserver.gameserver.datatables.SkillTreesData;
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
import com.l2jserver.gameserver.instancemanager.GrandBossManager;
import com.l2jserver.gameserver.instancemanager.HandysBlockCheckerManager;
import com.l2jserver.gameserver.instancemanager.InstanceManager;
import com.l2jserver.gameserver.instancemanager.ItemsOnGroundManager;
import com.l2jserver.gameserver.instancemanager.MapRegionManager;
import com.l2jserver.gameserver.instancemanager.QuestManager;
import com.l2jserver.gameserver.instancemanager.SiegeManager;
import com.l2jserver.gameserver.instancemanager.TerritoryWarManager;
import com.l2jserver.gameserver.model.BlockList;
import com.l2jserver.gameserver.model.CharEffectList;
import com.l2jserver.gameserver.model.Elementals;
import com.l2jserver.gameserver.model.FishData;
import com.l2jserver.gameserver.model.L2AccessLevel;
import com.l2jserver.gameserver.model.L2Clan;
import com.l2jserver.gameserver.model.L2ClanMember;
import com.l2jserver.gameserver.model.L2ContactList;
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
import com.l2jserver.gameserver.model.L2Party.messageType;
import com.l2jserver.gameserver.model.L2PetData;
import com.l2jserver.gameserver.model.L2PetLevelData;
import com.l2jserver.gameserver.model.L2PremiumItem;
import com.l2jserver.gameserver.model.L2Radar;
import com.l2jserver.gameserver.model.L2RecipeList;
import com.l2jserver.gameserver.model.L2Request;
import com.l2jserver.gameserver.model.L2ShortCut;
import com.l2jserver.gameserver.model.L2Skill;
import com.l2jserver.gameserver.model.L2Skill.SkillTargetType;
import com.l2jserver.gameserver.model.L2SkillLearn;
import com.l2jserver.gameserver.model.L2Transformation;
import com.l2jserver.gameserver.model.L2UIKeysSettings;
import com.l2jserver.gameserver.model.L2World;
import com.l2jserver.gameserver.model.L2WorldRegion;
import com.l2jserver.gameserver.model.Location;
import com.l2jserver.gameserver.model.MacroList;
import com.l2jserver.gameserver.model.PartyMatchRoom;
import com.l2jserver.gameserver.model.PartyMatchRoomList;
import com.l2jserver.gameserver.model.PartyMatchWaitingList;
import com.l2jserver.gameserver.model.ShortCuts;
import com.l2jserver.gameserver.model.TerritoryWard;
import com.l2jserver.gameserver.model.TradeList;
import com.l2jserver.gameserver.model.actor.L2Attackable;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.L2Decoy;
import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.model.actor.L2Playable;
import com.l2jserver.gameserver.model.actor.L2Summon;
import com.l2jserver.gameserver.model.actor.L2Trap;
import com.l2jserver.gameserver.model.actor.L2Vehicle;
import com.l2jserver.gameserver.model.actor.appearance.PcAppearance;
import com.l2jserver.gameserver.model.actor.knownlist.PcKnownList;
import com.l2jserver.gameserver.model.actor.position.PcPosition;
import com.l2jserver.gameserver.model.actor.stat.PcStat;
import com.l2jserver.gameserver.model.actor.status.PcStatus;
import com.l2jserver.gameserver.model.base.ClassId;
import com.l2jserver.gameserver.model.base.ClassLevel;
import com.l2jserver.gameserver.model.base.PlayerClass;
import com.l2jserver.gameserver.model.base.Race;
import com.l2jserver.gameserver.model.base.SubClass;
import com.l2jserver.gameserver.model.entity.Castle;
import com.l2jserver.gameserver.model.entity.Duel;
import com.l2jserver.gameserver.model.entity.Fort;
import com.l2jserver.gameserver.model.entity.Hero;
import com.l2jserver.gameserver.model.entity.Instance;
import com.l2jserver.gameserver.model.entity.L2Event;
import com.l2jserver.gameserver.model.entity.Siege;
import com.l2jserver.gameserver.model.entity.TvTEvent;
import com.l2jserver.gameserver.model.itemcontainer.Inventory;
import com.l2jserver.gameserver.model.itemcontainer.ItemContainer;
import com.l2jserver.gameserver.model.itemcontainer.PcFreight;
import com.l2jserver.gameserver.model.itemcontainer.PcInventory;
import com.l2jserver.gameserver.model.itemcontainer.PcRefund;
import com.l2jserver.gameserver.model.itemcontainer.PcWarehouse;
import com.l2jserver.gameserver.model.itemcontainer.PetInventory;
import com.l2jserver.gameserver.model.multisell.PreparedListContainer;
import com.l2jserver.gameserver.model.olympiad.OlympiadGameManager;
import com.l2jserver.gameserver.model.olympiad.OlympiadGameTask;
import com.l2jserver.gameserver.model.olympiad.OlympiadManager;
import com.l2jserver.gameserver.model.quest.Quest;
import com.l2jserver.gameserver.model.quest.QuestState;
import com.l2jserver.gameserver.model.quest.State;
import com.l2jserver.gameserver.model.zone.type.L2BossZone;
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
import com.l2jserver.gameserver.network.serverpackets.ExBasicActionList;
import com.l2jserver.gameserver.network.serverpackets.ExBrExtraUserInfo;
import com.l2jserver.gameserver.network.serverpackets.ExDominionWarStart;
import com.l2jserver.gameserver.network.serverpackets.ExDuelUpdateUserInfo;
import com.l2jserver.gameserver.network.serverpackets.ExFishingEnd;
import com.l2jserver.gameserver.network.serverpackets.ExFishingStart;
import com.l2jserver.gameserver.network.serverpackets.ExGetBookMarkInfoPacket;
import com.l2jserver.gameserver.network.serverpackets.ExGetOnAirShip;
import com.l2jserver.gameserver.network.serverpackets.ExOlympiadMode;
import com.l2jserver.gameserver.network.serverpackets.ExPrivateStoreSetWholeMsg;
import com.l2jserver.gameserver.network.serverpackets.ExSetCompassZoneCode;
import com.l2jserver.gameserver.network.serverpackets.ExSpawnEmitter;
import com.l2jserver.gameserver.network.serverpackets.ExStartScenePlayer;
import com.l2jserver.gameserver.network.serverpackets.ExStorageMaxCount;
import com.l2jserver.gameserver.network.serverpackets.ExVitalityPointInfo;
import com.l2jserver.gameserver.network.serverpackets.ExVoteSystemInfo;
import com.l2jserver.gameserver.network.serverpackets.FriendStatusPacket;
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
import com.l2jserver.gameserver.skills.Env;
import com.l2jserver.gameserver.skills.Formulas;
import com.l2jserver.gameserver.skills.Stats;
import com.l2jserver.gameserver.skills.l2skills.L2SkillSiegeFlag;
import com.l2jserver.gameserver.skills.l2skills.L2SkillSummon;
import com.l2jserver.gameserver.skills.l2skills.L2SkillTrap;
import com.l2jserver.gameserver.taskmanager.AttackStanceTaskManager;
import com.l2jserver.gameserver.templates.chars.L2PcTemplate;
import com.l2jserver.gameserver.templates.effects.EffectTemplate;
import com.l2jserver.gameserver.templates.item.L2Armor;
import com.l2jserver.gameserver.templates.item.L2ArmorType;
import com.l2jserver.gameserver.templates.item.L2EtcItem;
import com.l2jserver.gameserver.templates.item.L2EtcItemType;
import com.l2jserver.gameserver.templates.item.L2Henna;
import com.l2jserver.gameserver.templates.item.L2Item;
import com.l2jserver.gameserver.templates.item.L2Weapon;
import com.l2jserver.gameserver.templates.item.L2WeaponType;
import com.l2jserver.gameserver.templates.skills.L2EffectType;
import com.l2jserver.gameserver.templates.skills.L2SkillType;
import com.l2jserver.gameserver.util.FloodProtectors;
import com.l2jserver.gameserver.util.L2TIntObjectHashMap;
import com.l2jserver.gameserver.util.PlayerEventStatus;
import com.l2jserver.gameserver.util.Point3D;
import com.l2jserver.gameserver.util.Util;
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
	private static final String INSERT_CHARACTER = "INSERT INTO characters (account_name,charId,char_name,level,maxHp,curHp,maxCp,curCp,maxMp,curMp,face,hairStyle,hairColor,sex,exp,sp,karma,fame,pvpkills,pkkills,clanid,race,classid,deletetime,cancraft,title,title_color,accesslevel,online,isin7sdungeon,clan_privs,wantspeace,base_class,newbie,nobless,power_grade,createDate) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
	private static final String UPDATE_CHARACTER = "UPDATE characters SET level=?,maxHp=?,curHp=?,maxCp=?,curCp=?,maxMp=?,curMp=?,face=?,hairStyle=?,hairColor=?,sex=?,heading=?,x=?,y=?,z=?,exp=?,expBeforeDeath=?,sp=?,karma=?,fame=?,pvpkills=?,pkkills=?,clanid=?,race=?,classid=?,deletetime=?,title=?,title_color=?,accesslevel=?,online=?,isin7sdungeon=?,clan_privs=?,wantspeace=?,base_class=?,onlinetime=?,punish_level=?,punish_timer=?,newbie=?,nobless=?,power_grade=?,subpledge=?,lvl_joined_academy=?,apprentice=?,sponsor=?,varka_ketra_ally=?,clan_join_expiry_time=?,clan_create_expiry_time=?,char_name=?,death_penalty_level=?,bookmarkslot=?,vitality_points=?,language=? WHERE charId=?";
	private static final String RESTORE_CHARACTER = "SELECT account_name, charId, char_name, level, maxHp, curHp, maxCp, curCp, maxMp, curMp, face, hairStyle, hairColor, sex, heading, x, y, z, exp, expBeforeDeath, sp, karma, fame, pvpkills, pkkills, clanid, race, classid, deletetime, cancraft, title, title_color, accesslevel, online, char_slot, lastAccess, clan_privs, wantspeace, base_class, onlinetime, isin7sdungeon, punish_level, punish_timer, newbie, nobless, power_grade, subpledge, lvl_joined_academy, apprentice, sponsor, varka_ketra_ally,clan_join_expiry_time,clan_create_expiry_time,death_penalty_level,bookmarkslot,vitality_points,createDate,language FROM characters WHERE charId=?";
	
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
	
	// Character Transformation SQL String Definitions:
	private static final String SELECT_CHAR_TRANSFORM = "SELECT transform_id FROM characters WHERE charId=?";
	private static final String UPDATE_CHAR_TRANSFORM = "UPDATE characters SET transform_id=? WHERE charId=?";
	
	public static final int REQUEST_TIMEOUT = 15;
	public static final int STORE_PRIVATE_NONE = 0;
	public static final int STORE_PRIVATE_SELL = 1;
	public static final int STORE_PRIVATE_BUY = 3;
	public static final int STORE_PRIVATE_MANUFACTURE = 5;
	public static final int STORE_PRIVATE_PACKAGE_SELL = 8;
	
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
	private Calendar _createDate = Calendar.getInstance();
	
	private String _lang = null;
	private String _htmlPrefix = null;
	
	private volatile boolean _isOnline = false;
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
	private L2PetLevelData _leveldata;
	private int _curFeed;
	protected Future<?> _mountFeedTask;
	private ScheduledFuture<?> _dismountTask;
	private boolean _petItems = false;
	
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
	
	private final L2ContactList _contactList = new L2ContactList(this);
	
	private int _bookmarkslot = 0; // The Teleport Bookmark Slot
	
	private List<TeleportBookmark> tpbookmark = new FastList<TeleportBookmark>();
	
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
	
	/** Boat and AirShip */
	private L2Vehicle _vehicle = null;
	private Point3D _inVehiclePosition;
	
	public ScheduledFuture<?> _taskforfish;
	private int _mountType;
	private int _mountNpcId;
	private int _mountLevel;
	/** Store object used to summon the strider you are mounting **/
	private int _mountObjectID = 0;
	
	public int _telemode = 0;
	
	private boolean _inCrystallize;
	private boolean _inCraftMode;
	
	private long _offlineShopStart = 0;
	
	private L2Transformation _transformation;
	private int _transformationId = 0;
	
	/** The table containing all L2RecipeList of the L2PcInstance */
	private final Map<Integer, L2RecipeList> _dwarvenRecipeBook = new FastMap<Integer, L2RecipeList>();
	private final Map<Integer, L2RecipeList> _commonRecipeBook = new FastMap<Integer, L2RecipeList>();
	
	/** Premium Items */
	private final Map<Integer, L2PremiumItem> _premiumItems = new FastMap<Integer, L2PremiumItem>();
	
	/** True if the L2PcInstance is sitting */
	private boolean _waitTypeSitting;
	
	/** Location before entering Observer Mode */
	private int _lastX;
	private int _lastY;
	private int _lastZ;
	private boolean _observerMode = false;
	
	/** Stored from last ValidatePosition **/
	private final Point3D _lastServerPosition = new Point3D(0, 0, 0);
	
	/** The number of recommendation obtained by the L2PcInstance */
	private int _recomHave; // how much I was recommended by others
	/** The number of recommendation that the L2PcInstance can give */
	private int _recomLeft; // how many recommendations I can give to others
	/** Recommendation Bonus task **/
	private ScheduledFuture<?> _recoBonusTask;
	/** Recommendation task **/
	private ScheduledFuture<?> _recoGiveTask;
	/** Recommendation Two Hours bonus **/
	private boolean _recoTwoHoursGiven = false;
	
	private final PcInventory _inventory = new PcInventory(this);
	private final PcFreight _freight = new PcFreight(this);
	private PcWarehouse _warehouse;
	private PcRefund _refund;
	
	/** The Private Store type of the L2PcInstance (STORE_PRIVATE_NONE=0, STORE_PRIVATE_SELL=1, sellmanage=2, STORE_PRIVATE_BUY=3, buymanage=4, STORE_PRIVATE_MANUFACTURE=5) */
	private int _privatestore;
	
	private TradeList _activeTradeList;
	private ItemContainer _activeWarehouse;
	private L2ManufactureList _createList;
	private TradeList _sellList;
	private TradeList _buyList;
	
	// Multisell
	private PreparedListContainer _currentMultiSell = null;
	
	/** Bitmask used to keep track of one-time/newbie quest rewards*/
	private int _newbie;
	
	private boolean _noble = false;
	private boolean _hero = false;
	
	/** The L2FolkInstance corresponding to the last Folk wich one the player talked. */
	private L2Npc _lastFolkNpc = null;
	
	/** Last NPC Id talked on a quest */
	private int _questNpcObject = 0;
	
	/** The table containing all Quests began by the L2PcInstance */
	private final Map<String, QuestState> _quests = new FastMap<String, QuestState>();
	
	/** The list containing all shortCuts of this L2PcInstance */
	private final ShortCuts _shortCuts = new ShortCuts(this);
	
	/** The list containing all macroses of this L2PcInstance */
	private final MacroList _macroses = new MacroList(this);
	
	private final List<L2PcInstance> _snoopListener = new FastList<L2PcInstance>();
	private final List<L2PcInstance> _snoopedPlayer = new FastList<L2PcInstance>();
	
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
	// after Freya players can control more than one tamed beast
	private List<L2TamedBeastInstance> _tamedBeast = null;
	
	private boolean _minimapAllowed = false;
	
	// client radar
	//TODO: This needs to be better intergrated and saved/loaded
	private L2Radar _radar;
	
	// Party matching
	// private int _partymatching = 0;
	private int _partyroom = 0;
	// private int _partywait = 0;
	
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
	
	// charges
	private final AtomicInteger _charges = new AtomicInteger();
	private ScheduledFuture<?> _chargeTask = null;
	
	// Absorbed Souls
	private int _souls = 0;
	private ScheduledFuture<?> _soulTask = null;
	
	// WorldPosition used by TARGET_SIGNET_GROUND
	private Point3D _currentSkillWorldPosition;
	
	private L2AccessLevel _accessLevel;
	
	private boolean _messageRefusal = false;    // message refusal mode
	
	private boolean _silenceMode = false;     // silence mode
	private TIntArrayList _silenceModeExcluded = new TIntArrayList();     // silence mode
	private boolean _dietMode = false;          // ignore weight penalty
	private boolean _tradeRefusal = false;       // Trade refusal
	private boolean _exchangeRefusal = false;   // Exchange refusal
	
	private L2Party _party;
	
	// this is needed to find the inviting player for Party response
	// there can only be one active party request at once
	private L2PcInstance _activeRequester;
	private long _requestExpireTime = 0;
	private final L2Request _request = new L2Request(this);
	private L2ItemInstance _arrowItem;
	private L2ItemInstance _boltItem;
	
	// Used for protection after teleport
	private long _protectEndTime = 0;
	
	public boolean isSpawnProtected()
	{
		return _protectEndTime > GameTimeController.getGameTicks();
	}
	
	private long _teleportProtectEndTime = 0;
	
	public boolean isTeleportProtected()
	{
		return _teleportProtectEndTime > GameTimeController.getGameTicks();
	}

	// protects a char from agro mobs when getting up from fake death
	private long _recentFakeDeathEndTime = 0;
	private boolean _isFakeDeath;
	
	/** The fists L2Weapon of the L2PcInstance (used when no weapon is equiped) */
	private L2Weapon _fistsWeaponItem;
	
	private final Map<Integer, String> _chars = new FastMap<Integer, String>();
	
	//private byte _updateKnownCounter = 0;
	
	private int _expertiseArmorPenalty = 0;
	private int _expertiseWeaponPenalty = 0;
	
	private boolean _isEnchanting = false;
	private L2ItemInstance _activeEnchantItem = null;
	private L2ItemInstance _activeEnchantSupportItem = null;
	private L2ItemInstance _activeEnchantAttrItem = null;
	private long _activeEnchantTimestamp = 0;
	
	protected boolean _inventoryDisable = false;
	
	protected L2TIntObjectHashMap<L2CubicInstance> _cubics = new L2TIntObjectHashMap<L2CubicInstance>();
	
	/** Active shots. */
	protected FastSet<Integer> _activeSoulShots = new FastSet<Integer>().shared();
	
	public final ReentrantLock soulShotLock = new ReentrantLock();
	
	/** Event parameters */
	private PlayerEventStatus eventStatus = null;
	
	private byte _handysBlockCheckerEventArena = -1;
	
	/** new loto ticket **/
	private final int _loto[] = new int[5];
	//public static int _loto_nums[] = {0,1,2,3,4,5,6,7,8,9,};
	/** new race ticket **/
	private final int _race[] = new int[2];
	
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
	private final List<String> _validBypass = new FastList<String>();
	private final List<String> _validBypass2 = new FastList<String>();
	
	private Forum _forumMail;
	private Forum _forumMemo;
	
	/** Current skill in use. Note that L2Character has _lastSkillCast, but
	 * this has the button presses */
	private SkillDat _currentSkill;
	private SkillDat _currentPetSkill;
	
	/** Skills queued because a skill is already in progress */
	private SkillDat _queuedSkill;
	
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
	
	// during fall validations will be disabled for 10 ms.
	private static final int FALLING_VALIDATION_DELAY = 10000;
	private volatile long _fallingTimestamp = 0;
	
	private int _multiSocialTarget = 0;
	private int _multiSociaAction = 0;
	
	private int _movieId = 0;
	
	private String _adminConfirmCmd = null;
	
	private volatile long _lastItemAuctionInfoRequest = 0;
	
	private Future<?> _PvPRegTask;
	
	private long _pvpFlagLasts;
	
	private long _notMoveUntil = 0;
	
	public void setPvpFlagLasts(long time)
	{
		_pvpFlagLasts = time;
	}
	
	public long getPvpFlagLasts()
	{
		return _pvpFlagLasts;
	}
	
	public void startPvPFlag()
	{
		updatePvPFlag(1);
		
		_PvPRegTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new PvPFlag(), 1000, 1000);
	}
	
	public void stopPvpRegTask()
	{
		if (_PvPRegTask != null)
			_PvPRegTask.cancel(true);
	}
	
	public void stopPvPFlag()
	{
		stopPvpRegTask();
		
		updatePvPFlag(0);
		
		_PvPRegTask = null;
	}
	
	/** Task lauching the function stopPvPFlag() */
	private class PvPFlag implements Runnable
	{
		public PvPFlag()
		{
			
		}
		
		@Override
		public void run()
		{
			try
			{
				if (System.currentTimeMillis() > getPvpFlagLasts())
				{
					stopPvPFlag();
				}
				else if (System.currentTimeMillis() > (getPvpFlagLasts() - 20000))
				{
					updatePvPFlag(2);
				}
				else
				{
					updatePvPFlag(1);
					// Start a new PvP timer check
					//checkPvPFlag();
				}
			}
			catch (Exception e)
			{
				_log.log(Level.WARNING, "error in pvp flag task:", e);
			}
		}
	}
	
	// Character UI
	private L2UIKeysSettings _uiKeySettings;
	
	/** Herbs Task Time **/
	private int _herbstask = 0;
	/** Task for Herbs */
	
	private class HerbTask implements Runnable
	{
		private final String _process;
		private final int _itemId;
		private final long _count;
		private final L2Object _reference;
		private final boolean _sendMessage;
		
		HerbTask(String process, int itemId, long count, L2Object reference, boolean sendMessage)
		{
			_process = process;
			_itemId = itemId;
			_count = count;
			_reference = reference;
			_sendMessage = sendMessage;
		}
		
		@Override
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
	
	private class ShortBuffTask implements Runnable
	{
		@Override
		public void run()
		{
			if (L2PcInstance.this == null)
				return;
			
			L2PcInstance.this.sendPacket(new ShortBuffStatusUpdate(0, 0, 0));
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
	public static class SkillDat
	{
		private final L2Skill _skill;
		private final boolean _ctrlPressed;
		private final boolean _shiftPressed;
		
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
	private final SummonRequest _summonRequest = new SummonRequest();
	
	private static class SummonRequest
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
	private final GatesRequest _gatesRequest = new GatesRequest();
	
	private static class GatesRequest
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
	
	// Save responder name for log it
	private String _lastPetitionGmName = null;

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
	 * @param sex 
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
		player.setCreateDate(Calendar.getInstance());
		
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
		if (getClient() == null)
			return getAccountNamePlayer();
		return getClient().getAccountName();
	}
	
	public String getAccountNamePlayer()
	{
		return _accountName;
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
		if(getBlockCheckerArena() != -1)
		{
			result |= RelationChanged.RELATION_INSIEGE;
			HandysBlockCheckerManager.ArenaParticipantsHolder holder = HandysBlockCheckerManager.getInstance().getHolder(getBlockCheckerArena());
			if(holder.getPlayerTeam(this) == 0)
				result |= RelationChanged.RELATION_ENEMY;
			else
				result |= RelationChanged.RELATION_ALLY;
			result |= RelationChanged.RELATION_ATTACKER;
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
	 * @param app 
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
	 * @return the base L2PcTemplate link to the L2PcInstance.
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
	
	/** Return the Level of the L2PcInstance. */
	@Override
	public final int getLevel() { return getStat().getLevel(); }
	
	/**
	 * @return the _newbie rewards state of the L2PcInstance.
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
	 * {@link #deleteMe()}</li> <li>Save player data into DB <BR>
	 * {@link L2GameClient#saveCharToDisk()}</li> <BR>
	 */
	public void logout()
	{
		logout(true);
	}
	
	/**
	 * Manage Logout Task: <li>Remove player from world <BR>
	 * {@link #deleteMe()}</li> <li>Save player data into DB <BR>
	 * {@link L2GameClient#saveCharToDisk()}</li> <BR>
	 * <BR>
	 * @param closeClient
	 */
	public void logout(boolean closeClient)
	{
		try
		{
			closeNetConnection(closeClient);
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Exception on logout(): " + e.getMessage(), e);
		}
	}
	
	
	/**
	 * @return a table containing all Common L2RecipeList of the L2PcInstance.
	 */
	public L2RecipeList[] getCommonRecipeBook()
	{
		return _commonRecipeBook.values().toArray(new L2RecipeList[_commonRecipeBook.values().size()]);
	}
	
	/**
	 * @return a table containing all Dwarf L2RecipeList of the L2PcInstance.
	 */
	public L2RecipeList[] getDwarvenRecipeBook()
	{
		return _dwarvenRecipeBook.values().toArray(new L2RecipeList[_dwarvenRecipeBook.values().size()]);
	}
	
	/**
	 * Add a new L2RecipList to the table _commonrecipebook containing all L2RecipeList of the L2PcInstance <BR><BR>
	 *
	 * @param recipe The L2RecipeList to add to the _recipebook
	 * @param saveToDb 
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
	 * @param saveToDb 
	 */
	public void registerDwarvenRecipeList(L2RecipeList recipe, boolean saveToDb)
	{
		_dwarvenRecipeBook.put(recipe.getId(), recipe);
		
		if (saveToDb)
			insertNewRecipeData(recipe.getId(), true);
	}
	
	/**
	 * @param recipeId The Identifier of the L2RecipeList to check in the player's recipe books
	 * @return {@code true}if player has the recipe on Common or Dwarven Recipe book else returns {@code false}
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
	 * @param recipeId The Identifier of the L2RecipeList to remove from the _recipebook
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
			L2DatabaseFactory.close(con);
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
			L2DatabaseFactory.close(con);
		}
	}
	
	/**
	 * @return the Id for the last talked quest NPC.
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
	 * @param quest The name of the quest
	 * @return the QuestState object corresponding to the quest name.
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
	
	private QuestState[] addToQuestStateArray(QuestState[] questStateArray, QuestState state)
	{
		int len = questStateArray.length;
		QuestState[] tmp = new QuestState[len+1];
		for (int i=0; i < len; i++)
			tmp[i] = questStateArray[i];
		tmp[len] = state;
		return tmp;
	}
	
	/**
	 * @return a table containing all Quest in progress from the table _quests.
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
	 * @param npc
	 * @return a table containing all QuestState to modify after a L2Attackable killing.
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
	 * @param npc
	 * @return a table containing all QuestState to modify after a L2Attackable killing.
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
	 * @param npcId The Identifier of the NPC
	 * @return a table containing all QuestState from the table _quests in which the L2PcInstance must talk to the NPC.
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
	private List<QuestState> _notifyQuestOfDeathList;
	
	/**
	 * Add QuestState instance that is to be notified of L2PcInstance's death.<BR><BR>
	 *
	 * @param qs The QuestState that subscribe to this event
	 *
	 */
	public void addNotifyQuestOfDeath (QuestState qs)
	{
		if (qs == null)
			return;
		
		if (!getNotifyQuestOfDeath().contains(qs))
			getNotifyQuestOfDeath().add(qs);
	}
	
	/**
	 * Remove QuestState instance that is to be notified of L2PcInstance's death.<BR><BR>
	 *
	 * @param qs The QuestState that subscribe to this event
	 *
	 */
	public void removeNotifyQuestOfDeath (QuestState qs)
	{
		if (qs == null || _notifyQuestOfDeathList == null)
			return;
		
		_notifyQuestOfDeathList.remove(qs);
	}
	
	/**
	 * @return a list of QuestStates which registered for notify of death of this L2PcInstance.
	 */
	public final List<QuestState> getNotifyQuestOfDeath()
	{
		if (_notifyQuestOfDeathList == null)
		{
			synchronized(this)
			{
				if (_notifyQuestOfDeathList == null)
					_notifyQuestOfDeathList = new FastList<QuestState>();
			}
		}
		
		return _notifyQuestOfDeathList;
	}
	
	public final boolean isNotifyQuestOfDeathEmpty()
	{
		return _notifyQuestOfDeathList == null || _notifyQuestOfDeathList.isEmpty();
	}
	
	/**
	 * @return a table containing all L2ShortCut of the L2PcInstance.
	 */
	public L2ShortCut[] getAllShortCuts()
	{
		return _shortCuts.getAllShortCuts();
	}
	
	/**
	 * @param slot The slot in wich the shortCuts is equiped
	 * @param page The page of shortCuts containing the slot
	 * @return the L2ShortCut of the L2PcInstance corresponding to the position (page-slot).
	 */
	public L2ShortCut getShortCut(int slot, int page)
	{
		return _shortCuts.getShortCut(slot, page);
	}
	
	/**
	 * Add a L2shortCut to the L2PcInstance _shortCuts<BR><BR>
	 * @param shortcut 
	 */
	public void registerShortCut(L2ShortCut shortcut)
	{
		_shortCuts.registerShortCut(shortcut);
	}
	
	/**
	 * Delete the L2ShortCut corresponding to the position (page-slot) from the L2PcInstance _shortCuts.<BR><BR>
	 * @param slot 
	 * @param page 
	 */
	public void deleteShortCut(int slot, int page)
	{
		_shortCuts.deleteShortCut(slot, page);
	}
	
	/**
	 * Add a L2Macro to the L2PcInstance _macroses<BR><BR>
	 * @param macro 
	 */
	public void registerMacro(L2Macro macro)
	{
		_macroses.registerMacro(macro);
	}
	
	/**
	 * Delete the L2Macro corresponding to the Identifier from the L2PcInstance _macroses.<BR><BR>
	 * @param id 
	 */
	public void deleteMacro(int id)
	{
		_macroses.deleteMacro(id);
	}
	
	/**
	 * @return all L2Macro of the L2PcInstance.
	 */
	public MacroList getMacroses()
	{
		return _macroses;
	}
	
	/**
	 * Set the siege state of the L2PcInstance.<BR><BR>
	 * @param siegeState 1 = attacker, 2 = defender, 0 = not involved
	 */
	public void setSiegeState(byte siegeState)
	{
		_siegeState = siegeState;
	}
	/**
	 * Get the siege state of the L2PcInstance.<BR><BR>
	 * @return 1 = attacker, 2 = defender, 0 = not involved
	 */
	public byte getSiegeState()
	{
		return _siegeState;
	}
	
	/**
	 * Set the siege Side of the L2PcInstance.<BR><BR>
	 * @param val 
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
	 * @param pvpFlag 
	 */
	public void setPvpFlag(int pvpFlag)
	{
		_pvpFlag = (byte)pvpFlag;
	}
	
	@Override
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
		
		for (L2PcInstance target : plrs)
		{
			target.sendPacket(new RelationChanged(this, getRelation(target), isAutoAttackable(target)));
			if (getPet() != null)
				target.sendPacket(new RelationChanged(getPet(), getRelation(target), isAutoAttackable(target)));
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
		
		if (isInsideZone(ZONE_ALTERED))
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
	 * @return True if the L2PcInstance can Craft Dwarven Recipes.
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
	 * @return True if the L2PcInstance can Craft Dwarven Recipes.
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
	 * @return the PK counter of the L2PcInstance.
	 */
	public int getPkKills()
	{
		return _pkKills;
	}
	
	/**
	 * Set the PK counter of the L2PcInstance.<BR><BR>
	 * @param pkKills 
	 */
	public void setPkKills(int pkKills)
	{
		_pkKills = pkKills;
	}
	
	/**
	 * @return the _deleteTimer of the L2PcInstance.
	 */
	public long getDeleteTimer()
	{
		return _deleteTimer;
	}
	
	/**
	 * Set the _deleteTimer of the L2PcInstance.<BR><BR>
	 * @param deleteTimer 
	 */
	public void setDeleteTimer(long deleteTimer)
	{
		_deleteTimer = deleteTimer;
	}
	
	/**
	 * @return the current weight of the L2PcInstance.
	 */
	public int getCurrentLoad()
	{
		return _inventory.getTotalWeight();
	}

	/**
	 * @return the number of recommendation obtained by the L2PcInstance.
	 */
	public int getRecomHave()
	{
		return _recomHave;
	}
	
	/**
	 * Increment the number of recommendation obtained by the L2PcInstance (Max : 255).<BR><BR>
	 */
	protected void incRecomHave()
	{
		if (_recomHave < 255)
			_recomHave++;
	}
	
	/**
	 * Set the number of recommendation obtained by the L2PcInstance (Max : 255).<BR><BR>
	 * @param value 
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
	 * Set the number of recommendation obtained by the L2PcInstance (Max : 255).<BR><BR>
	 * @param value 
	 */
	public void setRecomLeft(int value)
	{
		if (value > 255)
			_recomLeft = 255;
		else if (value < 0)
			_recomLeft = 0;
		else
			_recomLeft = value;
	}
	
	/**
	 * @return the number of recommendation that the L2PcInstance can give.
	 */
	public int getRecomLeft()
	{
		return _recomLeft;
	}
	
	/**
	 * Increment the number of recommendation that the L2PcInstance can give.<BR><BR>
	 */
	protected void decRecomLeft()
	{
		if (_recomLeft > 0)
			_recomLeft--;
	}
	
	public void giveRecom(L2PcInstance target)
	{
		target.incRecomHave();
		decRecomLeft();
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
	@Override
	public int getKarma()
	{
		return _karma;
	}
	
	/**
	 * Set the Karma of the L2PcInstance and send a Server->Client packet StatusUpdate (broadcast).<BR><BR>
	 * @param karma 
	 */
	public void setKarma(int karma)
	{
		if (karma < 0) karma = 0;
		if (_karma == 0 && karma > 0)
		{
			Collection<L2Object> objs = getKnownList().getKnownObjects().values();

			for (L2Object object : objs)
			{
				if (!(object instanceof L2GuardInstance))
					continue;
				
				if (((L2GuardInstance) object).getAI().getIntention() == CtrlIntention.AI_INTENTION_IDLE)
					((L2GuardInstance) object).getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE, null);
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
	 * @return the max weight that the L2PcInstance can load.
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
			if (item != null && item.isEquipped() &&
					((item.getItemType() != L2EtcItemType.ARROW) && (item.getItemType() != L2EtcItemType.BOLT)))
			{
				int crystaltype = item.getItem().getCrystalType();
				
				if (crystaltype > getExpertiseLevel())
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
		armorPenalty = armorPenalty - getExpertiseLevel();
		
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
		weaponPenalty = weaponPenalty - getExpertiseLevel();
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
					SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
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
	
	public void checkSShotsMatch(L2ItemInstance equipped, L2ItemInstance unequipped)
	{
		if (unequipped == null)
			return;
		
		unequipped.setChargedSoulshot(L2ItemInstance.CHARGED_NONE);
		unequipped.setChargedSpiritshot(L2ItemInstance.CHARGED_NONE);
		
		// on retail auto shots never disabled on uneqip
		/*if (unequipped.getItem().getType2() == L2Item.TYPE2_WEAPON &&
				(equipped == null ? true : equipped.getItem().getItemGradeSPlus() != unequipped.getItem().getItemGradeSPlus()))
		{
			disableAutoShotByCrystalType(unequipped.getItem().getItemGradeSPlus());
		}*/
	}
	
	public void useEquippableItem(L2ItemInstance item, boolean abortAttack)
	{
		// Equip or unEquip
		L2ItemInstance[] items = null;
		final boolean isEquiped = item.isEquipped();
		final int oldInvLimit = getInventoryLimit();
		SystemMessage sm = null;
		if ((item.getItem().getBodyPart() & L2Item.SLOT_MULTI_ALLWEAPON) != 0)
		{
			L2ItemInstance old = getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
			checkSShotsMatch(item, old);
		}
		
		if (isEquiped)
		{
			if (item.getEnchantLevel() > 0)
			{
				sm = SystemMessage.getSystemMessage(SystemMessageId.EQUIPMENT_S1_S2_REMOVED);
				sm.addNumber(item.getEnchantLevel());
				sm.addItemName(item);
			}
			else
			{
				sm = SystemMessage.getSystemMessage(SystemMessageId.S1_DISARMED);
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
			items = getInventory().equipItemAndRecord(item);
			
			if (item.isEquipped())
			{
				if (item.getEnchantLevel() > 0)
				{
					sm = SystemMessage.getSystemMessage(SystemMessageId.S1_S2_EQUIPPED);
					sm.addNumber(item.getEnchantLevel());
					sm.addItemName(item);
				}
				else
				{
					sm = SystemMessage.getSystemMessage(SystemMessageId.S1_EQUIPPED);
					sm.addItemName(item);
				}
				sendPacket(sm);
				
				// Consume mana - will start a task if required; returns if item is not a shadow item
				item.decreaseMana(false);
				
				if ((item.getItem().getBodyPart() & L2Item.SLOT_MULTI_ALLWEAPON) != 0)
				{
					rechargeAutoSoulShot(true, true, false);
				}
			}
			else
				sendPacket(SystemMessageId.CANNOT_EQUIP_ITEM_DUE_TO_BAD_CONDITION);
		}
		refreshExpertisePenalty();

		broadcastUserInfo();
		
		InventoryUpdate iu = new InventoryUpdate();
		iu.addItems(Arrays.asList(items));
		sendPacket(iu);
		
		if (abortAttack) abortAttack();
		
		if (getInventoryLimit() != oldInvLimit)
			sendPacket(new ExStorageMaxCount(this));
	}
	
	/**
	 * @return the the PvP Kills of the L2PcInstance (Number of player killed during a PvP).
	 */
	public int getPvpKills()
	{
		return _pvpKills;
	}
	
	/**
	 * Set the the PvP Kills of the L2PcInstance (Number of player killed during a PvP).<BR><BR>
	 * @param pvpKills 
	 */
	public void setPvpKills(int pvpKills)
	{
		_pvpKills = pvpKills;
	}
	
	/**
	 * @return the Fame of this L2PcInstance
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
	 * @return the ClassId object of the L2PcInstance contained in L2PcTemplate.
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
				SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.CLAN_MEMBER_S1_EXPELLED);
				msg.addPcName(this);
				_clan.broadcastToOnlineMembers(msg);
				_clan.broadcastToOnlineMembers(new PledgeShowMemberListDelete(getName()));
				_clan.removeClanMember(getObjectId(), 0);
				sendPacket(SystemMessage.getSystemMessage(SystemMessageId.ACADEMY_MEMBERSHIP_TERMINATED));
				
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
				sendPacket(SystemMessage.getSystemMessage(SystemMessageId.THIRD_CLASS_TRANSFER));
			else
				sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CLASS_TRANSFER));
			
			// Update class icon in party and clan
			if (isInParty())
				getParty().broadcastToPartyMembers(new PartySmallWindowUpdate(this));
			
			if (getClan() != null)
				getClan().broadcastToOnlineMembers(new PledgeShowMemberListUpdate(this));
			
			//Add AutoGet skills and normal skills and/or learnByFS depending on configurations.
			rewardSkills();
			
			if (!isGM() && Config.DECREASE_SKILL_LEVEL)
				checkPlayerSkills();
		}
		finally
		{
			_subclassLock.unlock();
		}
	}
	
	/**
	 * @return the Experience of the L2PcInstance.
	 */
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
	 * @return the fists weapon of the L2PcInstance (used when no weapon is equipped).
	 */
	public L2Weapon getFistsWeaponItem()
	{
		return _fistsWeaponItem;
	}

	/**
	 * @param classId 
	 * @return the fists weapon of the L2PcInstance Class (used when no weapon is equipped).
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
	 * This method reward all AutoGet skills and Normal skills if Auto-Learn configuration is true.<br>
	 */
	public void rewardSkills()
	{
		//Give all normal skills if activated Auto-Learn is activated, included AutoGet skills.
		if (Config.AUTO_LEARN_SKILLS)
		{
			giveAvailableSkills(Config.AUTO_LEARN_FS_SKILLS, true);
		}
		else
		{
			giveAvailableAutoGetSkills();
		}
		
		checkItemRestriction();
		sendSkillList();
	}
	
	/**
	 * Re-give all skills which aren't saved to database, like Noble, Hero, Clan Skills.<br>
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
			clan.addSkillEffects(this);
			
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
	 * Give all available skills to the player.<br>
	 * @param includedByFs
	 * @param includeAutoGet
	 * @return skillCounter, the amount of new skills added.
	 */
	public int giveAvailableSkills(boolean includedByFs, boolean includeAutoGet)
	{
		int unLearnable = 0;
		int skillCounter = 0;
		
		// Get available skills
		FastList<L2SkillLearn> skills = SkillTreesData.getInstance().getAvailableSkills(this, getClassId(), includedByFs, includeAutoGet);
		
		while (skills.size() > unLearnable)
		{
			for (L2SkillLearn s: skills)
			{
				L2Skill sk = SkillTable.getInstance().getInfo(s.getSkillId(), s.getSkillLevel());
				if (sk == null || (sk.getId() == L2Skill.SKILL_DIVINE_INSPIRATION && !Config.AUTO_LEARN_DIVINE_INSPIRATION && !isGM()))
				{
					unLearnable++;
					continue;
				}
				
				if (getSkillLevel(sk.getId()) == -1)
				{
					skillCounter++;
				}
				
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
			
			//Get new available skills, some skills depend of previous skills to be available.
			skills = SkillTreesData.getInstance().getAvailableSkills(this, getClassId(), includedByFs, includeAutoGet);
		}
		
		sendMessage("You have learned " + skillCounter + " new skills.");
		return skillCounter;
	}
	
	/**
	 * Give all available AutoGet skills to the player.<br>
	 */
	public void giveAvailableAutoGetSkills()
	{
		// Get available skills
		final FastList<L2SkillLearn> autoGetSkills = SkillTreesData.getInstance().getAvailableAutoGetSkills(this);
		
		if ((autoGetSkills != null) && !autoGetSkills.isEmpty())
		{
			for (L2SkillLearn s: autoGetSkills)
			{
				final L2Skill skill = SkillTable.getInstance().getInfo(s.getSkillId(), s.getSkillLevel());
				if (skill != null)
				{
					addSkill(skill, true);
				}
				else
				{
					_log.warning("Skipped null autoGet Skill for player:" + getName() + "[" + getObjectId() + "]");
				}
			}
		}
	}
	
	/**
	 * Set the Experience value of the L2PcInstance. 
	 * @param exp
	 */
	public void setExp(long exp)
	{
		if (exp < 0)
			exp = 0;
		
		getStat().setExp(exp);
	}
	
	/**
	 * @return the Race object of the L2PcInstance.
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
	
	/**
	 * @return the SP amount of the L2PcInstance.
	 */
	public int getSp() { return getStat().getSp(); }
	
	/**
	 * Set the SP amount of the L2PcInstance.
	 * @param sp
	 */
	public void setSp(int sp)
	{
		if (sp < 0)
			sp = 0;
		
		super.getStat().setSp(sp);
	}
	
	/**
	 * @param castleId 
	 * @return true if this L2PcInstance is a clan leader in ownership of the passed castle
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
	 * @return the Clan Identifier of the L2PcInstance.
	 */
	public int getClanId()
	{
		return _clanId;
	}
	
	/**
	 * @return the Clan Crest Identifier of the L2PcInstance or 0.
	 */
	public int getClanCrestId()
	{
		if (_clan != null)
			return _clan.getCrestId();
		
		return 0;
	}
	
	/**
	 * @return The Clan CrestLarge Identifier or 0
	 */
	public int getClanCrestLargeId()
	{
		if (_clan != null)
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
	 * @param objectId 
	 */
	public void removeItemFromShortCut(int objectId)
	{
		_shortCuts.deleteShortCutByObjectId(objectId);
	}
	
	/**
	 * @return True if the L2PcInstance is sitting.
	 */
	public boolean isSitting()
	{
		return _waitTypeSitting;
	}
	
	/**
	 * Set _waitTypeSitting to given value
	 * @param state 
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
		sitDown(true);
	}
	
	public void sitDown(boolean checkCast)
	{
		if (checkCast && isCastingNow())
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
			ThreadPoolManager.getInstance().scheduleGeneral(new SitDownTask(), 2500);
			setIsParalyzed(true);
		}
	}
	/**
	 * Sit down Task
	 */
	private class SitDownTask implements Runnable
	{
		@Override
		public void run()
		{
			L2PcInstance.this.setIsParalyzed(false);
			L2PcInstance.this.getAI().setIntention(CtrlIntention.AI_INTENTION_REST);
		}
	}
	/**
	 * Stand up Task
	 */
	private class StandUpTask implements Runnable
	{
		@Override
		public void run()
		{
			L2PcInstance.this.setIsSitting(false);
			L2PcInstance.this.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		}
	}
	/**
	 * Stand up the L2PcInstance, set the AI Intention to AI_INTENTION_IDLE and send a Server->Client ChangeWaitType packet (broadcast)<BR><BR>
	 */
	public void standUp()
	{
		if (L2Event.isParticipant(this) && getEventStatus().eventSitForced)
		{
			sendMessage("A dark force beyond your mortal understanding makes your knees to shake when you try to stand up...");
		}
		else if (_waitTypeSitting && !isInStoreMode() && !isAlikeDead())
		{
			if (_effects.isAffected(CharEffectList.EFFECT_FLAG_RELAXING))
			{
				stopEffects(L2EffectType.RELAXING);
			}
			
			broadcastPacket(new ChangeWaitType (this, ChangeWaitType.WT_STANDING));
			// Schedule a stand up task to wait for the animation to finish
			ThreadPoolManager.getInstance().scheduleGeneral(new StandUpTask(), 2500);
		}
	}
	
	/**
	 * @return the PcWarehouse object of the L2PcInstance.
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
	 * @return the PcFreight object of the L2PcInstance.
	 */ 
	public PcFreight getFreight() 
	{ 
		return _freight; 
	} 
	
	/**
	 * @return true if refund list is not empty
	 */
	public boolean hasRefund()
	{
		return _refund != null && _refund.getSize() > 0 && Config.ALLOW_REFUND;
	}
	
	/**
	 * @return refund object or create new if not exist
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
	 * @return the Identifier of the L2PcInstance.
	 */
	@Deprecated
	public int getCharId()
	{
		return _charId;
	}
	
	/**
	 * Set the Identifier of the L2PcInstance.<BR><BR>
	 * @param charId 
	 */
	public void setCharId(int charId)
	{
		_charId = charId;
	}
	
	/**
	 * @return the Adena amount of the L2PcInstance.
	 */
	public long getAdena()
	{
		return _inventory.getAdena();
	}
	
	/**
	 * @return the Ancient Adena amount of the L2PcInstance.
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
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.EARNED_S1_ADENA);
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
	 * @return boolean informing if the action was successful
	 */
	public boolean reduceAdena(String process, long count, L2Object reference, boolean sendMessage)
	{
		if (count > getAdena())
		{
			if (sendMessage) sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_NOT_ENOUGH_ADENA));
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
				SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_DISAPPEARED_ADENA);
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
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.EARNED_S2_S1_S);
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
	 * @return boolean informing if the action was successful
	 */
	public boolean reduceAncientAdena(String process, long count, L2Object reference, boolean sendMessage)
	{
		if (count > getAncientAdena())
		{
			if (sendMessage)
				sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_NOT_ENOUGH_ADENA));
			
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
				if (count > 1)
				{
					SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S2_S1_DISAPPEARED);
					sm.addItemName(PcInventory.ANCIENT_ADENA_ID);
					sm.addItemNumber(count);
					sendPacket(sm);
				}
				else
				{
					SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_DISAPPEARED);
					sm.addItemName(PcInventory.ANCIENT_ADENA_ID);
					sendPacket(sm);
				}
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
					SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_PICKED_UP_S1_S2);
					sm.addItemName(item);
					sm.addItemNumber(item.getCount());
					sendPacket(sm);
				}
				else if (item.getEnchantLevel() > 0)
				{
					SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_PICKED_UP_A_S1_S2);
					sm.addNumber(item.getEnchantLevel());
					sm.addItemName(item);
					sendPacket(sm);
				}
				else
				{
					SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_PICKED_UP_S1);
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
			StatusUpdate su = new StatusUpdate(this);
			su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
			sendPacket(su);
			
			// If over capacity, drop the item
			if (!isGM() && !_inventory.validateCapacity(0, item.isQuestItem()) && newitem.isDropable() && (!newitem.isStackable() || newitem.getLastChange() != L2ItemInstance.MODIFIED))
				dropItem("InvDrop", newitem, null, true, true);
			
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
					fort.getSiege().announceToPlayer(SystemMessage.getSystemMessage(SystemMessageId.C1_ACQUIRED_THE_FLAG), this.getName());
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
	 * @return 
	 */
	public L2ItemInstance addItem(String process, int itemId, long count, L2Object reference, boolean sendMessage)
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
				return null;
			}
			// Sends message to client if requested
			if (sendMessage && ((!isCastingNow() && item.getItemType() == L2EtcItemType.HERB)
					|| item.getItemType() != L2EtcItemType.HERB))
			{
				if (count > 1)
				{
					if (process.equalsIgnoreCase("sweep") || process.equalsIgnoreCase("Quest"))
					{
						SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.EARNED_S2_S1_S);
						sm.addItemName(itemId);
						sm.addItemNumber(count);
						sendPacket(sm);
					}
					else
					{
						SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_PICKED_UP_S1_S2);
						sm.addItemName(itemId);
						sm.addItemNumber(count);
						sendPacket(sm);
					}
				}
				else
				{
					if (process.equalsIgnoreCase("sweep") || process.equalsIgnoreCase("Quest"))
					{
						SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.EARNED_ITEM_S1);
						sm.addItemName(itemId);
						sendPacket(sm);
					}
					else
					{
						SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_PICKED_UP_S1);
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
						handler.useItem(this, herb, false);
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
				if (!isGM() && !_inventory.validateCapacity(0, item.isQuestItem()) && createdItem.isDropable() && (!createdItem.isStackable() || createdItem.getLastChange() != L2ItemInstance.MODIFIED))
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
						fort.getSiege().announceToPlayer(SystemMessage.getSystemMessage(SystemMessageId.C1_ACQUIRED_THE_FLAG), this.getName());
					}
				}
				// Territory Ward
				else if (createdItem.getItemId() >= 13560 && createdItem.getItemId() <= 13568)
				{
					TerritoryWard ward = TerritoryWarManager.getInstance().getTerritoryWard(createdItem.getItemId() - 13479);
					if (ward != null)
						ward.activate(this, createdItem);
				}
				return createdItem;
			}
		}
		return null;
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
	 * @param count 
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
				sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
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
		StatusUpdate su = new StatusUpdate(this);
		su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
		sendPacket(su);
		
		// Sends message to client if requested
		if (sendMessage)
		{
			if (count > 1)
			{
				SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S2_S1_DISAPPEARED);
				sm.addItemName(item);
				sm.addItemNumber(count);
				sendPacket(sm);
			}
			else 
			{
				SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_DISAPPEARED);
				sm.addItemName(item);
				sendPacket(sm);
			}
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
				sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
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
				sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
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
		if (itemId == 57)
			return reduceAdena(process, count, reference, sendMessage);
		
		L2ItemInstance item = _inventory.getItemByItemId(itemId);
		
		if (item == null || item.getCount() < count || _inventory.destroyItemByItemId(process, itemId, count, this, reference) == null)
		{
			if (sendMessage)
				sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
			
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
		StatusUpdate su = new StatusUpdate(this);
		su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
		sendPacket(su);
		
		// Sends message to client if requested
		if (sendMessage)
		{
			if (count > 1)
			{
				SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S2_S1_DISAPPEARED);
				sm.addItemName(itemId);
				sm.addItemNumber(count);
				sendPacket(sm);
			}
			else 
			{
				SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_DISAPPEARED);
				sm.addItemName(itemId);
				sendPacket(sm);
			}
		}
		
		return true;
	}

	/**
	 * Transfers item to another ItemContainer and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param objectId : int Item Identifier of the item to be transfered
	 * @param count : long Quantity of items to be transfered
	 * @param target
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
		StatusUpdate playerSU = new StatusUpdate(this);
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
			playerSU = new StatusUpdate(targetPlayer);
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
		}
		return newItem;
	}
	
	/**
	 * Drop item from inventory and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process String Identifier of process triggering this action
	 * @param item L2ItemInstance to be dropped
	 * @param reference L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage boolean Specifies whether to send message to Client about this action
	 * @param protectItem whether or not dropped item must be protected temporary against other players
	 * @return boolean informing if the action was successfull
	 */
	public boolean dropItem(String process, L2ItemInstance item, L2Object reference, boolean sendMessage, boolean protectItem)
	{
		item = _inventory.dropItem(process, item, this, reference);
		
		if (item == null)
		{
			if (sendMessage)
				sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
			
			return false;
		}
		
		item.dropMe(this, getX() + Rnd.get(50) - 25, getY() + Rnd.get(50) - 25, getZ() + 20);
		
		if (Config.AUTODESTROY_ITEM_AFTER > 0
				&& Config.DESTROY_DROPPED_PLAYER_ITEM
				&& !Config.LIST_PROTECTED_ITEMS.contains(item.getItemId())) {
			if ((item.isEquipable() && Config.DESTROY_EQUIPABLE_PLAYER_ITEM)
					|| !item.isEquipable())
				ItemsAutoDestroy.getInstance().addItem(item);
		}
		
		// protection against auto destroy dropped item
		if (Config.DESTROY_DROPPED_PLAYER_ITEM) {
			if (!item.isEquipable()
					|| (item.isEquipable() && Config.DESTROY_EQUIPABLE_PLAYER_ITEM))
				item.setProtected(false);
			else
				item.setProtected(true);
		} else
			item.setProtected(true);

		// retail drop protection 
		if (protectItem)
			item.getDropProtection().protect(this);
		
		// Send inventory update packet
		if (!Config.FORCE_INVENTORY_UPDATE)
		{
			InventoryUpdate playerIU = new InventoryUpdate();
			playerIU.addItem(item);
			sendPacket(playerIU);
		}
		else sendPacket(new ItemList(this, false));
		
		// Update current load as well
		StatusUpdate su = new StatusUpdate(this);
		su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
		sendPacket(su);
		
		// Sends message to client if requested
		if (sendMessage)
		{
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_DROPPED_S1);
			sm.addItemName(item);
			sendPacket(sm);
		}
		
		return true;
	}
	
	public boolean dropItem(String process, L2ItemInstance item, L2Object reference, boolean sendMessage)
	{
		return dropItem(process, item, reference, sendMessage, false);
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
	 * @param protectItem 
	 * @return L2ItemInstance corresponding to the new item or the updated item in inventory
	 */
	public L2ItemInstance dropItem(String process, int objectId, long count, int x, int y, int z, L2Object reference, boolean sendMessage, boolean protectItem)
	{
		L2ItemInstance invitem = _inventory.getItemByObjectId(objectId);
		L2ItemInstance item = _inventory.dropItem(process, objectId, count, this, reference);
		
		if (item == null)
		{
			if (sendMessage)
				sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
			
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
		
		// retail drop protection 
		if (protectItem)
			item.getDropProtection().protect(this);
		
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
		StatusUpdate su = new StatusUpdate(this);
		su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
		sendPacket(su);
		
		// Sends message to client if requested
		if (sendMessage)
		{
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_DROPPED_S1);
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
		if (getPet() != null && getPet().getControlObjectId() == objectId || getMountObjectID() == objectId)
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

		// We cannot put a Weapon with Augmention in WH while casting (Possible Exploit)
		if (item.isAugmented() && (isCastingNow() || this.isCastingSimultaneouslyNow()))
			return null;
		
		return item;
	}
	
	/**
	 * Set _protectEndTime according settings.
	 * @param protect 
	 */
	public void setProtection(boolean protect)
	{
		if (Config.DEVELOPER && (protect || _protectEndTime > 0))
			_log.warning(getName() + ": Protection " + (protect?"ON " + (GameTimeController.getGameTicks() + Config.PLAYER_SPAWN_PROTECTION * GameTimeController.TICKS_PER_SECOND) :"OFF") + " (currently " + GameTimeController.getGameTicks() + ")");
		
		_protectEndTime = protect ? GameTimeController.getGameTicks() + Config.PLAYER_SPAWN_PROTECTION * GameTimeController.TICKS_PER_SECOND : 0;
	}
	
	public void setTeleportProtection(boolean protect)
	{
		if (Config.DEVELOPER && (protect || _teleportProtectEndTime > 0))
			_log.warning(getName() + ": Tele Protection " + (protect?"ON " + (GameTimeController.getGameTicks() + Config.PLAYER_TELEPORT_PROTECTION * GameTimeController.TICKS_PER_SECOND) :"OFF") + " (currently " + GameTimeController.getGameTicks() + ")");
		
		_teleportProtectEndTime = protect? GameTimeController.getGameTicks() + Config.PLAYER_TELEPORT_PROTECTION * GameTimeController.TICKS_PER_SECOND : 0;
	}
	
	/**
	 * Set protection from agro mobs when getting up from fake death, according settings.
	 * @param protect 
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
	 * @return the client owner of this char.
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
	 * @param closeClient 
	 */
	private void closeNetConnection(boolean closeClient)
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
		_reuseTimeStamps.remove(skill.getReuseHashCode());
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
					sendPacket(SystemMessage.getSystemMessage(SystemMessageId.A_MALICIOUS_SKILL_CANNOT_BE_USED_IN_PEACE_ZONE));
					return false;
				}
				if (getTrap() != null
						&& getTrap().getSkill().getId() == ((L2SkillTrap)skill).getTriggerSkillId())
				{
					SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
					sm.addSkillName(skill);
					sendPacket(sm);
					return false;
				}
				break;
			}
			case SUMMON:
			{
				if (!((L2SkillSummon)skill).isCubic() && (getPet() != null || isMounted() || CharSummonTable.getInstance().getPets().contains(getObjectId()) || CharSummonTable.getInstance().getPets().contains(getObjectId())))
				{
					if (Config.DEBUG)
						_log.fine("player has a pet already. ignore summon skill");
					
					sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_ALREADY_HAVE_A_PET));
					return false;
				}
			}
		}
		
		// TODO: Should possibly be checked only in L2PcInstance's useMagic
		// Can't use Hero and resurrect skills during Olympiad
		if (isInOlympiadMode() &&
				(skill.isHeroSkill() || skill.getSkillType() == L2SkillType.RESURRECT))
		{
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.THIS_SKILL_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT);
			sendPacket(sm);
			return false;
		}
		
		final int charges = getCharges();
		// Check if the spell using charges or not in AirShip
		if ((skill.getMaxCharges() == 0 && charges < skill.getNumCharges())
				|| (isInAirShip() && skill.getSkillType() != L2SkillType.REFUEL))
		{
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
			sm.addSkillName(skill);
			sendPacket(sm);
			return false;
		}
		
		return true;
	}
	
	/**
	 * Returns true if cp update should be done, false if not
	 * @param barPixels 
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
	 * @param barPixels 
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
		StatusUpdate su = new StatusUpdate(this);
		su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
		su.addAttribute(StatusUpdate.CUR_MP, (int) getCurrentMp());
		su.addAttribute(StatusUpdate.CUR_CP, (int) getCurrentCp());
		su.addAttribute(StatusUpdate.MAX_CP, 	   getMaxCp());
		sendPacket(su);
		
		final boolean needCpUpdate = needCpUpdate(352);
		final boolean needHpUpdate = needHpUpdate(352);
		// Check if a party is in progress and party window update is usefull
		L2Party party = _party;
		if (party != null
				&& (needCpUpdate
						|| needHpUpdate
						|| needMpUpdate(352)))
		{
			if (Config.DEBUG)
				_log.fine("Send status for party window of " + getObjectId() + "(" + getName() + ") to his party. CP: " + getCurrentCp() + " HP: " + getCurrentHp() + " MP: " + getCurrentMp());
			// Send the Server->Client packet PartySmallWindowUpdate with current HP, MP and Level to all other L2PcInstance of the Party
			PartySmallWindowUpdate update = new PartySmallWindowUpdate(this);
			party.broadcastToPartyMembers(this, update);
		}
		
		if (isInOlympiadMode()
				&& isOlympiadStart()
				&& (needCpUpdate || needHpUpdate))
		{
			final OlympiadGameTask game = OlympiadGameManager.getInstance().getOlympiadTask(getOlympiadGameId());
			if (game != null && game.isBattleStarted())
				game.getZone().broadcastStatusUpdate(this);
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
		if (TerritoryWarManager.getInstance().isTWInProgress()
				&& (TerritoryWarManager.getInstance().checkIsRegistered(-1, getObjectId())
						|| TerritoryWarManager.getInstance().checkIsRegistered(-1, getClan())))
		{
			broadcastPacket(new ExDominionWarStart(this));
		}
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
		for (L2PcInstance player : plrs)
		{
			if (player == null)
				continue;
			player.sendPacket(mov);
			if (mov instanceof CharInfo)
			{
				int relation = getRelation(player);
				Integer oldrelation = getKnownList().getKnownRelations().get(player.getObjectId());
				if (oldrelation != null && oldrelation != relation)
				{
					player.sendPacket(new RelationChanged(this, relation, isAutoAttackable(player)));
					if (getPet() != null)
						player.sendPacket(new RelationChanged(getPet(), relation, isAutoAttackable(player)));
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
					Integer oldrelation = getKnownList().getKnownRelations().get(player.getObjectId());
					if (oldrelation != null && oldrelation != relation)
					{
						player.sendPacket(new RelationChanged(this, relation, isAutoAttackable(player)));
						if (getPet() != null)
							player.sendPacket(new RelationChanged(getPet(), relation, isAutoAttackable(player)));
					}
				}
			}	
		}
	}
	
	/**
	 * @return the Alliance Identifier of the L2PcInstance.
	 */
	public int getAllyId()
	{
		if (_clan == null)
			return 0;
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
	
	private class GameGuardCheck implements Runnable
	{
		/**
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run()
		{
			L2GameClient client = L2PcInstance.this.getClient();
			if (client != null && !client.isAuthedGG() && L2PcInstance.this.isOnline())
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
	 * @param id SystemMessageId
	 */
	public void sendPacket(SystemMessageId id)
	{
		sendPacket(SystemMessage.getSystemMessage(id));
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
	 * @param item 
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
			_log.warning(this+" trying to pickup wrong target."+getTarget());
			return;
		}
		
		L2ItemInstance target = (L2ItemInstance) object;
		
		// Send a Server->Client packet ActionFailed to this L2PcInstance
		sendPacket(ActionFailed.STATIC_PACKET);
		
		// Send a Server->Client packet StopMove to this L2PcInstance
		StopMove sm = new StopMove(this);
		if (Config.DEBUG)
			_log.fine("pickup pos: "+ target.getX() + " "+target.getY()+ " "+target.getZ() );
		sendPacket(sm);
		
		SystemMessage smsg = null;
		synchronized (target)
		{
			// Check if the target to pick up is visible
			if (!target.isVisible())
			{
				// Send a Server->Client packet ActionFailed to this L2PcInstance
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			if (!target.getDropProtection().tryPickUp(this))
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				smsg = SystemMessage.getSystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1);
				smsg.addItemName(target);
				sendPacket(smsg);
				return;
			}
			
			if ( ((isInParty() && getParty().getLootDistribution() == L2Party.ITEM_LOOTER) || !isInParty()) && !_inventory.validateCapacity(target))
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(SystemMessage.getSystemMessage(SystemMessageId.SLOTS_FULL));
				return;
			}
			
			if (isInvul() && !isGM())
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				smsg = SystemMessage.getSystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1);
				smsg.addItemName(target);
				sendPacket(smsg);
				return;
			}
			
			if (target.getOwnerId() != 0 && target.getOwnerId() != getObjectId() && !isInLooterParty(target.getOwnerId()))
			{
				if (target.getItemId() == 57)
				{
					smsg = SystemMessage.getSystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1_ADENA);
					smsg.addItemNumber(target.getCount());
				}
				else if (target.getCount() > 1)
				{
					smsg = SystemMessage.getSystemMessage(SystemMessageId.FAILED_TO_PICKUP_S2_S1_S);
					smsg.addItemName(target);
					smsg.addItemNumber(target.getCount());
				}
				else
				{
					smsg = SystemMessage.getSystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1);
					smsg.addItemName(target);
				}
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(smsg);
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
				handler.useItem(this, target, false);
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
					smsg = SystemMessage.getSystemMessage(SystemMessageId.ANNOUNCEMENT_C1_PICKED_UP_S2_S3);
					smsg.addPcName(this);
					smsg.addNumber(target.getEnchantLevel());
					smsg.addItemName(target.getItemId());
					broadcastPacket(smsg, 1400);
				}
				else
				{
					smsg = SystemMessage.getSystemMessage(SystemMessageId.ANNOUNCEMENT_C1_PICKED_UP_S2);
					smsg.addPcName(this);
					smsg.addItemName(target.getItemId());
					broadcastPacket(smsg, 1400);
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
			else
			{
				addItem("Pickup", target, null, true);
				//Auto-Equip arrows/bolts if player has a bow/crossbow and player picks up arrows/bolts.
				final L2EtcItem etcItem = target.getEtcItem();
				if (etcItem != null)
				{
					final L2EtcItemType itemType = etcItem.getItemType();
					if (itemType == L2EtcItemType.ARROW)
					{
						checkAndEquipArrows();
					}
					else if (itemType == L2EtcItemType.BOLT)
					{
						checkAndEquipBolts();
					}
				}
			}
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
				sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NO_PRIVATE_STORE_HERE));
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
				sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NO_PRIVATE_STORE_HERE));
			sendPacket(ActionFailed.STATIC_PACKET);
		}
	}
	
	public final PreparedListContainer getMultiSell()
	{
		return _currentMultiSell;
	}
	
	public final void setMultiSell(PreparedListContainer list)
	{
		_currentMultiSell = list;
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
			SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.YOU_ALREADY_POLYMORPHED_AND_CANNOT_POLYMORPH_AGAIN);
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
		sendPacket(ExBasicActionList.getStaticPacket(this));
		broadcastUserInfo();
	}
	
	@Override
	public synchronized void untransform()
	{
		if (_transformation != null)
		{
			setQueuedSkill(null, false, false);
			setTransformAllowedSkills(new int[]{});
			_transformation.onUntransform();
			_transformation = null;
			stopEffects(L2EffectType.TRANSFORMATION);
			sendSkillList();
			sendPacket(new SkillCoolTime(this));
			sendPacket(ExBasicActionList.getStaticPacket(this));
			broadcastUserInfo();
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
			L2DatabaseFactory.close(con);
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
			L2DatabaseFactory.close(con);
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
			
			// vehicles cant be targeted
			else if (newTarget instanceof L2Vehicle)
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
	
	public void setMarryRequest(boolean state)
	{
		_marryrequest = state;
	}
	
	public boolean isMarryRequest()
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
			L2PcInstance ptarget = L2World.getInstance().getPlayer(_engageid);
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
	 * Return the secondary L2Item item (always equiped in the left hand).<BR>
	 * Arrows, Shield..<BR>
	 * 
	 */
	@Override
	public L2Item getSecondaryWeaponItem()
	{
		L2ItemInstance item = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		if (item != null)
			return item.getItem();
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
	 * @param killer
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
				stopFakeDeath(true);
		}
		
		if (killer != null)
		{
			L2PcInstance pk = killer.getActingPlayer();
			
			TvTEvent.onKill(killer, this);
			
			if (L2Event.isParticipant(pk) && pk != null)
				pk.getEventStatus().kills.add(this);
			
			//announce pvp/pk
			if (Config.ANNOUNCE_PK_PVP && pk != null && !pk.isGM())
			{
				String msg = "";
				if (getPvpFlag() == 0)
				{
					msg = Config.ANNOUNCE_PK_MSG.replace("$killer", pk.getName()).replace("$target", getName());
					if (Config.ANNOUNCE_PK_PVP_NORMAL_MESSAGE)
					{
						SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1);
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
						SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1);
						sm.addString(msg);
						Announcements.getInstance().announceToAll(sm);
					}
					else
						Announcements.getInstance().announceToAll(msg);
				}
			}

			broadcastStatusUpdate();
			// Clear resurrect xp calculation
			setExpBeforeDeath(0);
			
			// Issues drop of Cursed Weapon.
			if (isCursedWeaponEquipped())
			{
				CursedWeaponsManager.getInstance().drop(_cursedWeaponEquippedId, killer);
			}
			else if (isCombatFlagEquipped())
			{
				//TODO: Fort siege during TW??
				if (TerritoryWarManager.getInstance().isTWInProgress())
					TerritoryWarManager.getInstance().dropCombatFlag(this, true, false);
				else
				{
					Fort fort = FortManager.getInstance().getFort(this);
					if (fort != null)
						FortSiegeManager.getInstance().dropCombatFlag(this, fort.getFortId());
					else
					{
						int slot = getInventory().getSlotFromItem(getInventory().getItemByItemId(9819));
						getInventory().unEquipItemInBodySlot(slot);
						destroyItem("CombatFlag", getInventory().getItemByItemId(9819), null, true);
					}
				}
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
					//If player is Lucky shouldn't get penalized.
					if (Config.ALT_GAME_DELEVEL && !isLucky())
					{
						// Reduce the Experience of the L2PcInstance in function of the calculated Death Penalty
						// NOTE: deathPenalty +- Exp will update karma
						// Penalty is lower if the player is at war with the pk (war has to be declared)
						final boolean siegeNpc = (killer instanceof L2DefenderInstance) || (killer instanceof L2FortCommanderInstance);
						final boolean atWar = (pk != null) && (getClan() != null) && (getClan().isAtWarWith(pk.getClanId()));
						deathPenalty(atWar, (pk != null), siegeNpc);
					}
					else if (!(isInsideZone(ZONE_PVP) && !isInSiege()) || (pk == null))
					{
						onDieUpdateKarma(); // Update karma if delevel is not allowed
					}
				}
			}
		}
		
		setPvpFlag(0); // Clear the pvp flag
		
		// Unsummon Cubics
		if (!_cubics.isEmpty())
		{
			for (L2CubicInstance cubic : _cubics.getValues(new L2CubicInstance[_cubics.size()]))
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
		else if (isAffected(CharEffectList.EFFECT_FLAG_CHARM_OF_COURAGE) && this.isInSiege())
		{
			reviveRequest(this, null, false);
		}
		return true;
	}
	
	private void onDieDropItem(L2Character killer)
	{
		if (L2Event.isParticipant(this) || killer == null)
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
							getPet() != null && getPet().getControlObjectId() == itemDrop.getItemId() || // Control Item of active pet
							Arrays.binarySearch(Config.KARMA_LIST_NONDROPPABLE_ITEMS, itemDrop.getItemId()) >= 0 || // Item listed in the non droppable item list
							Arrays.binarySearch(Config.KARMA_LIST_NONDROPPABLE_PET_ITEMS, itemDrop.getItemId()) >= 0 // Item listed in the non droppable pet item list
					) continue;
					
					if (itemDrop.isEquipped())
					{
						// Set proper chance according to Item type of equipped Item
						itemDropPercent = itemDrop.getItem().getType2() == L2Item.TYPE2_WEAPON ? dropEquipWeapon : dropEquip;
						getInventory().unEquipItemInSlot(itemDrop.getLocationSlot());
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
			// SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.THERE_IS_S1_HOUR_AND_S2_MINUTE_LEFT_OF_THE_FIXED_USAGE_TIME);
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
	 * @param target 
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
	 * @param target
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
	 * TODO: Unhardcode by implementing Lucky effect (Support for effects on passive skills required).
	 * @return Returns {@code true} if player has Lucky skill and is level 9 or less.
	 */
	public boolean isLucky()
	{
		return ((getLevel() <= 9) && (getKnownSkill(L2Skill.SKILL_LUCKY) != null));
	}
	
	/**
	 * Restore the specified % of experience this L2PcInstance has
	 * lost and sends a Server->Client StatusUpdate packet.<BR><BR>
	 * @param restorePercent 
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
	 * @param atwar 
	 * @param killed_by_pc 
	 * @param killed_by_siege_npc 
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
		if (!L2Event.isParticipant(this))
			if (lvl < ExperienceTable.getInstance().getMaxLevel())
				lostExp = Math.round((getStat().getExpForLevel(lvl+1) - getStat().getExpForLevel(lvl)) * percentLost /100);
			else
				lostExp = Math.round((getStat().getExpForLevel(ExperienceTable.getInstance().getMaxLevel()) - getStat().getExpForLevel(ExperienceTable.getInstance().getMaxLevel() - 1)) * percentLost /100);
		
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
	
	public boolean isPartyWaiting()
	{
		return PartyMatchWaitingList.getInstance().getPlayers().contains(this);
	}
	
	public void setPartyRoom(int id)
	{
		_partyroom = id;
	}
	
	public int getPartyRoom()
	{
		return _partyroom;
	}
	
	public boolean isInPartyMatchRoom()
	{
		return _partyroom > 0;
	}
	
	/**
	 * Stop the HP/MP/CP Regeneration task.<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Set the RegenActive flag to False </li>
	 * <li>Stop the HP/MP/CP Regeneration task </li><BR><BR>
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
		stopRecoBonusTask();
		stopRecoGiveTask();
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
	 * @return the L2Decoy of the L2PcInstance or null.
	 */
	public L2Decoy getDecoy()
	{
		return _decoy;
	}
	
	/**
	 * @return the L2Trap of the L2PcInstance or null.
	 */
	public L2Trap getTrap()
	{
		return _trap;
	}
	
	/**
	 * Set the L2Summon of the L2PcInstance.<BR><BR>
	 * @param summon 
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
	 * @param decoy 
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
	 * @return the L2Summon of the L2PcInstance or null.
	 */
	public List<L2TamedBeastInstance> getTrainedBeasts()
	{
		return _tamedBeast;
	}
	
	/**
	 * Set the L2Summon of the L2PcInstance.<BR><BR>
	 * @param tamedBeast 
	 */
	public void addTrainedBeast(L2TamedBeastInstance tamedBeast)
	{
		if (_tamedBeast == null)
			_tamedBeast = new FastList<L2TamedBeastInstance>();
		_tamedBeast.add(tamedBeast);
	}
	
	/**
	 * @return the L2PcInstance requester of a transaction (ex : FriendInvite, JoinAlly, JoinParty...).
	 */
	public L2Request getRequest()
	{
		return _request;
	}
	
	/**
	 * Set the L2PcInstance requester of a transaction (ex : FriendInvite, JoinAlly, JoinParty...).<BR><BR>
	 * @param requester 
	 */
	public void setActiveRequester(L2PcInstance requester)
	{
		_activeRequester = requester;
	}
	
	/**
	 * @return the L2PcInstance requester of a transaction (ex : FriendInvite, JoinAlly, JoinParty...).
	 */
	public L2PcInstance getActiveRequester()
	{
		L2PcInstance requester = _activeRequester;
		if (requester != null)
		{
			if (requester.isRequestExpired() && _activeTradeList == null)
				_activeRequester = null;
		}
		return _activeRequester;
	}
	
	/**
	 * @return True if a transaction is in progress.
	 */
	public boolean isProcessingRequest()
	{
		return getActiveRequester() != null || _requestExpireTime > GameTimeController.getGameTicks();
	}
	
	/**
	 * @return True if a transaction is in progress.
	 */
	public boolean isProcessingTransaction()
	{
		return getActiveRequester() != null || _activeTradeList != null || _requestExpireTime > GameTimeController.getGameTicks();
	}
	
	/**
	 * Select the Warehouse to be used in next activity.<BR><BR>
	 * @param partner 
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
	 * @param warehouse 
	 */
	public void setActiveWarehouse(ItemContainer warehouse)
	{
		_activeWarehouse = warehouse;
	}
	
	/**
	 * @return active Warehouse.
	 */
	public ItemContainer getActiveWarehouse()
	{
		return _activeWarehouse;
	}
	
	/**
	 * Select the TradeList to be used in next activity.<BR><BR>
	 * @param tradeList 
	 */
	public void setActiveTradeList(TradeList tradeList)
	{
		_activeTradeList = tradeList;
	}
	
	/**
	 * @return active TradeList.
	 */
	public TradeList getActiveTradeList()
	{
		return _activeTradeList;
	}
	
	public void onTradeStart(L2PcInstance partner)
	{
		_activeTradeList = new TradeList(this);
		_activeTradeList.setPartner(partner);
		
		SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.BEGIN_TRADE_WITH_C1);
		msg.addPcName(partner);
		sendPacket(msg);
		sendPacket(new TradeStart(this));
	}
	
	public void onTradeConfirm(L2PcInstance partner)
	{
		SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.C1_CONFIRMED_TRADE);
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
		SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.C1_CANCELED_TRADE);
		msg.addPcName(partner);
		sendPacket(msg);
	}
	
	public void onTradeFinish(boolean successfull)
	{
		_activeTradeList = null;
		sendPacket(new TradeDone(1));
		if (successfull)
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.TRADE_SUCCESSFUL));
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
	 * @return the _createList object of the L2PcInstance.
	 */
	public L2ManufactureList getCreateList()
	{
		return _createList;
	}
	
	/**
	 * Set the _createList object of the L2PcInstance.<BR><BR>
	 * @param x 
	 */
	public void setCreateList(L2ManufactureList x)
	{
		_createList = x;
	}
	
	/**
	 * @return the _buyList object of the L2PcInstance.
	 */
	public TradeList getSellList()
	{
		if (_sellList == null) _sellList = new TradeList(this);
		return _sellList;
	}
	
	/**
	 * @return the _buyList object of the L2PcInstance.
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
	 * @param type 
	 */
	public void setPrivateStoreType(int type)
	{
		_privatestore = type;
		
		if (Config.OFFLINE_DISCONNECT_FINISHED && _privatestore == STORE_PRIVATE_NONE && (getClient() == null || getClient().isDetached()))
		{
			deleteMe();
		}
	}
	
	/**
	 * <B><U> Values </U> :</B><BR><BR>
	 * <li>0 : STORE_PRIVATE_NONE</li>
	 * <li>1 : STORE_PRIVATE_SELL</li>
	 * <li>2 : sellmanage</li><BR>
	 * <li>3 : STORE_PRIVATE_BUY</li><BR>
	 * <li>4 : buymanage</li><BR>
	 * <li>5 : STORE_PRIVATE_MANUFACTURE</li><BR>
	 * @return the Private Store type of the L2PcInstance.
	 */
	public int getPrivateStoreType()
	{
		return _privatestore;
	}
	
	/**
	 * Set the _clan object, _clanId, _clanLeader Flag and title of the L2PcInstance.<BR><BR>
	 * @param clan 
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
			_activeWarehouse = null;
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
	 * @return the _clan object of the L2PcInstance.
	 */
	public L2Clan getClan()
	{
		return _clan;
	}
	
	/**
	 * @return True if the L2PcInstance is the leader of its clan.
	 */
	public boolean isClanLeader()
	{
		if (getClan() == null)
		{
			return false;
		}
		return getObjectId() == getClan().getLeaderId();
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
	 * Disarm the player's weapon.
	 * @return true if the player was disarmed, false otherwise.
	 */
	public boolean disarmWeapons()
	{
		// Don't allow disarming a cursed weapon
		if (isCursedWeaponEquipped()) return false;
		
		// Don't allow disarming a Combat Flag or Territory Ward
		if (isCombatFlagEquipped()) return false;
		
		// Unequip the weapon
		L2ItemInstance wpn = getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
		if (wpn != null)
		{
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
					sm = SystemMessage.getSystemMessage(SystemMessageId.EQUIPMENT_S1_S2_REMOVED);
					sm.addNumber(unequiped[0].getEnchantLevel());
					sm.addItemName(unequiped[0]);
				}
				else
				{
					sm = SystemMessage.getSystemMessage(SystemMessageId.S1_DISARMED);
					sm.addItemName(unequiped[0]);
				}
				sendPacket(sm);
			}
		}
		return true;
	}
	
	/**
	 * Disarm the player's shield.<BR><BR>
	 * @return 
	 */
	public boolean disarmShield()
	{
		L2ItemInstance sld = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		if (sld != null)
		{
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
					sm = SystemMessage.getSystemMessage(SystemMessageId.EQUIPMENT_S1_S2_REMOVED);
					sm.addNumber(unequiped[0].getEnchantLevel());
					sm.addItemName(unequiped[0]);
				}
				else
				{
					sm = SystemMessage.getSystemMessage(SystemMessageId.S1_DISARMED);
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
		setMountObjectID(pet.getControlObjectId());
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
				sendPacket(SystemMessage.getSystemMessage(SystemMessageId.STRIDER_CANT_BE_RIDDEN_WHILE_DEAD));
				return false;
			}
			else if (pet.isDead())
			{
				//A dead strider cannot be ridden.
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(SystemMessage.getSystemMessage(SystemMessageId.DEAD_STRIDER_CANT_BE_RIDDEN));
				return false;
			}
			else if (pet.isInCombat() || pet.isRooted())
			{
				//A strider in battle cannot be ridden
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(SystemMessage.getSystemMessage(SystemMessageId.STRIDER_IN_BATLLE_CANT_BE_RIDDEN));
				return false;
				
			}
			else if (isInCombat())
			{
				//A strider cannot be ridden while in battle
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(SystemMessage.getSystemMessage(SystemMessageId.STRIDER_CANT_BE_RIDDEN_WHILE_IN_BATTLE));
				return false;
			}
			else if (isSitting())
			{
				//A strider can be ridden only when standing
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(SystemMessage.getSystemMessage(SystemMessageId.STRIDER_CAN_BE_RIDDEN_ONLY_WHILE_STANDING));
				return false;
			}
			else if (isFishing())
			{
				//You can't mount, dismount, break and drop items while fishing
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CANNOT_DO_WHILE_FISHING_2));
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
				//FIXME: Wrong Message
				sendMessage("You cannot mount a steed while holding a flag.");
				return false;
			}
			else if (pet.isHungry())
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(SystemMessage.getSystemMessage(SystemMessageId.HUNGRY_STRIDER_NOT_MOUNT));
				return false;
			}
			else if (!Util.checkIfInRange(200, this, pet, true))
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(SystemMessage.getSystemMessage(SystemMessageId.TOO_FAR_AWAY_FROM_FENRIR_TO_MOUNT));
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
				sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NO_DISMOUNT_HERE));
				return false;
			}
			else if (isHungry())
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(SystemMessage.getSystemMessage(SystemMessageId.HUNGRY_STRIDER_NOT_MOUNT));
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
		else if (weaponItem.getItemType() == L2WeaponType.DUALDAGGER)
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
		return super.isInvul() || _teleportProtectEndTime > GameTimeController.getGameTicks();
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
	 * @param party 
	 */
	public void setParty(L2Party party)
	{
		_party = party;
	}
	
	/**
	 * Set the _party object of the L2PcInstance AND join it.<BR><BR>
	 * @param party 
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
			_party.removePartyMember(this, messageType.Disconnected);
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
	 * @param level 
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
	 * @return the _accessLevel of the L2PcInstance.
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
	 * @param broadcastType 
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
	 * @param flag 
	 */
	public void setKarmaFlag(int flag)
	{
		sendPacket(new UserInfo(this));
		sendPacket(new ExBrExtraUserInfo(this));
		Collection<L2PcInstance> plrs = getKnownList().getKnownPlayers().values();
		for (L2PcInstance player : plrs)
		{
			player.sendPacket(new RelationChanged(this, getRelation(player), isAutoAttackable(player)));
			if (getPet() != null)
				player.sendPacket(new RelationChanged(getPet(), getRelation(player), isAutoAttackable(player)));
		}	
	}
	
	/**
	 * Send a Server->Client StatusUpdate packet with Karma to the L2PcInstance and all L2PcInstance to inform (broadcast).<BR><BR>
	 */
	public void broadcastKarma()
	{
		StatusUpdate su = new StatusUpdate(this);
		su.addAttribute(StatusUpdate.KARMA, getKarma());
		sendPacket(su);
		
		Collection<L2PcInstance> plrs = getKnownList().getKnownPlayers().values();
		for (L2PcInstance player : plrs)
		{
			player.sendPacket(new RelationChanged(this, getRelation(player), isAutoAttackable(player)));
			if (getPet() != null)
				player.sendPacket(new RelationChanged(getPet(), getRelation(player), isAutoAttackable(player)));
		}
	}
	
	/**
	 * Set the online Flag to True or False and update the characters table of the database with online status and lastAccess (called when login and logout).<BR><BR>
	 * @param isOnline 
	 * @param updateInDb 
	 */
	public void setOnlineStatus(boolean isOnline, boolean updateInDb)
	{
		if (_isOnline != isOnline)
			_isOnline = isOnline;
		
		// Update the characters table of the database with online status and lastAccess (called when login and logout)
		if (updateInDb)
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
			statement.setInt(1, isOnlineInt());
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
			L2DatabaseFactory.close(con);
		}
	}
	
	/**
	 * Create a new player in the characters table of the database.<BR><BR>
	 * @return 
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
			statement.setInt(29, isOnlineInt());
			statement.setInt(30, isIn7sDungeon() ? 1 : 0);
			statement.setInt(31, getClanPrivileges());
			statement.setInt(32, getWantsPeace());
			statement.setInt(33, getBaseClass());
			statement.setInt(34, getNewbie());
			statement.setInt(35, isNoble() ? 1 :0);
			statement.setLong(36, 0);
			statement.setDate(37, new Date(getCreateDate().getTimeInMillis()));
			
			statement.executeUpdate();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Could not insert char data: " + e.getMessage(), e);
			return false;
		}
		finally
		{
			L2DatabaseFactory.close(con);
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
				player.setPowerGrade(rset.getInt("power_grade"));
				player.setPledgeType(rset.getInt("subpledge"));
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
					int pledgeClass = 0;
					
					pledgeClass = player.getClan().getClanMember(objectId).calculatePledgeClass(player);
					
					if (player.isNoble() && pledgeClass < 5)
						pledgeClass = 5;
					
					if (player.isHero() && pledgeClass < 8)
						pledgeClass = 8;
					
					player.setPledgeClass(pledgeClass);
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
				player.getCreateDate().setTime(rset.getDate("createDate"));
				
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
			
			// Set Hero status if it applies
			if (Hero.getInstance().getHeroes() != null && Hero.getInstance().getHeroes().containsKey(objectId))
				player.setHero(true);
			
			// Retrieve from the database all skills of this L2PcInstance and add them to _skills
			// Retrieve from the database all items of this L2PcInstance and add them to _inventory
			player.getInventory().restore();
			player.getFreight().restore(); 
			if (!Config.WAREHOUSE_CACHE)
				player.getWarehouse();
			
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
			L2DatabaseFactory.close(con);
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
	 * @param player 
	 * @return 
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
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Could not restore classes for " + player.getName() + ": " + e.getMessage(), e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
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
		
		// Retrieve from the database the recipe book of this L2PcInstance.
		restoreRecipeBook(true);
		
		// Restore Recipe Shop list
		if(Config.STORE_RECIPE_SHOPLIST)
			restoreRecipeShopList();
		
		// Load Premium Item List
		loadPremiumItemList();
		
		// Check for items in pet inventory
		checkPetInvItems();
	}
	
	/**
	 * Restore recipe book data for this L2PcInstance.
	 * @param loadCommon 
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
			_log.log(Level.SEVERE, "Could not restore recipe book data:" + e.getMessage(), e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
	}
	
	public Map<Integer, L2PremiumItem> getPremiumItemList()
	{
		return _premiumItems;
	}
	
	private void loadPremiumItemList()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			String sql = "SELECT itemNum, itemId, itemCount, itemSender FROM character_premium_items WHERE charId=?";
			PreparedStatement statement = con.prepareStatement(sql);
			statement.setInt(1, getObjectId());
			ResultSet rset = statement.executeQuery();
			while (rset.next())
			{
				int itemNum = rset.getInt("itemNum");
				int itemId = rset.getInt("itemId");
				long itemCount = rset.getLong("itemCount");
				String itemSender = rset.getString("itemSender");
				_premiumItems.put(itemNum, new L2PremiumItem(itemId, itemCount, itemSender));
			}
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Could not restore premium items: "+ e.getMessage(), e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
	}
	
	public void updatePremiumItem(int itemNum, long newcount)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE character_premium_items SET itemCount=? WHERE charId=? AND itemNum=? ");
			statement.setLong(1, newcount);
			statement.setInt(2, getObjectId());
			statement.setInt(3, itemNum);
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Could not update premium items: "+ e.getMessage(), e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
	}
	
	public void deletePremiumItem(int itemNum)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM character_premium_items WHERE charId=? AND itemNum=? ");
			statement.setInt(1, getObjectId());
			statement.setInt(2, itemNum);
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Could not delete premium item: " + e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
	}
	
	/**
	 * Update L2PcInstance stats in the characters table of the database.<BR><BR>
	 * @param storeActiveEffects 
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
	
	@Override
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
			statement.setInt(13, _observerMode ? _lastX : getX());
			statement.setInt(14, _observerMode ? _lastY : getY());
			statement.setInt(15, _observerMode ? _lastZ : getZ());
			statement.setLong(16, exp);
			statement.setLong(17, getExpBeforeDeath());
			statement.setInt(18, sp);
			statement.setInt(19, getKarma());
			statement.setInt(20, getFame());
			statement.setInt(21, getPvpKills());
			statement.setInt(22, getPkKills());
			statement.setInt(23, getClanId());
			statement.setInt(24, getRace().ordinal());
			statement.setInt(25, getClassId().getId());
			statement.setLong(26, getDeleteTimer());
			statement.setString(27, getTitle());
			statement.setInt(28, getAppearance().getTitleColor());
			statement.setInt(29, getAccessLevel().getLevel());
			statement.setInt(30, isOnlineInt());
			statement.setInt(31, isIn7sDungeon() ? 1 : 0);
			statement.setInt(32, getClanPrivileges());
			statement.setInt(33, getWantsPeace());
			statement.setInt(34, getBaseClass());
			
			long totalOnlineTime = _onlineTime;
			
			if (_onlineBeginTime > 0)
				totalOnlineTime += (System.currentTimeMillis()-_onlineBeginTime)/1000;
			
			statement.setLong(35, totalOnlineTime);
			statement.setInt(36, getPunishLevel().value());
			statement.setLong(37, getPunishTimer());
			statement.setInt(38, getNewbie());
			statement.setInt(39, isNoble() ? 1 : 0);
			statement.setInt(40, getPowerGrade());
			statement.setInt(41, getPledgeType());
			statement.setInt(42,getLvlJoinedAcademy());
			statement.setLong(43,getApprentice());
			statement.setLong(44,getSponsor());
			statement.setInt(45, getAllianceWithVarkaKetra());
			statement.setLong(46, getClanJoinExpiryTime());
			statement.setLong(47, getClanCreateExpiryTime());
			statement.setString(48, getName());
			statement.setLong(49, getDeathPenaltyBuffLevel());
			statement.setInt(50, getBookMarkSlot());
			statement.setInt(51, getVitalityPoints());
			statement.setString(52, getLang());
			statement.setInt(53, getObjectId());
			
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Could not store char base data: "+this+" - " + e.getMessage(), e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
	}
	
	private void storeCharSub()
	{
		if (getTotalSubClasses() <= 0)
			return;
		
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(UPDATE_CHAR_SUBCLASS);
			for (SubClass subClass : getSubClasses().values())
			{
				statement.setLong(1, subClass.getExp());
				statement.setInt(2, subClass.getSp());
				statement.setInt(3, subClass.getLevel());
				statement.setInt(4, subClass.getClassId());
				statement.setInt(5, getObjectId());
				statement.setInt(6, subClass.getClassIndex());
				
				statement.execute();
				statement.clearParameters();
			}
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Could not store sub class data for " + getName() + ": " + e.getMessage(), e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
	}
	
	@Override
	public void storeEffect(boolean storeEffects)
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
						
						if (_reuseTimeStamps.containsKey(skill.getReuseHashCode()))
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
			for (int hash : _reuseTimeStamps.keys())
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
			L2DatabaseFactory.close(con);
		}
	}
	
	/**
	 * @return True if the L2PcInstance is on line.
	 */
	public boolean isOnline()
	{
		return _isOnline;
	}
	
	public int isOnlineInt()
	{
		if (_isOnline && getClient() != null)
			return getClient().isDetached() ? 2 : 1;
		return 0;
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
	 * @param store 
	 *
	 * @return The L2Skill replaced or null if just added a new L2Skill
	 */
	public L2Skill addSkill(L2Skill newSkill, boolean store)
	{
		// Add a skill to the L2PcInstance _skills and its Func objects to the calculator set of the L2PcInstance
		L2Skill oldSkill = super.addSkill(newSkill);
		
		// Add or update a L2PcInstance skill in the character_skills table of the database
		if (store)
		{
			storeSkill(newSkill, oldSkill, -1);
		}
		return oldSkill;
	}
	
	@Override
	public L2Skill removeSkill(L2Skill skill, boolean store)
	{
		if (store)
			return removeSkill(skill);
		return super.removeSkill(skill, true);
	}
	
	public L2Skill removeSkill(L2Skill skill, boolean store, boolean cancelEffect)
	{
		if (store)
			return removeSkill(skill);
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
	 * @return The L2Skill removed
	 */
	@Override
	public L2Skill removeSkill(L2Skill skill)
	{
		// Remove a skill from the L2Character and its Func objects from calculator set of the L2Character
		final L2Skill oldSkill = super.removeSkill(skill);
		if (oldSkill != null)
		{
			Connection con = null;
			try
			{
				// Remove or update a L2PcInstance skill from the character_skills table of the database
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement(DELETE_SKILL_FROM_CHAR);
				
				statement.setInt(1, oldSkill.getId());
				statement.setInt(2, getObjectId());
				statement.setInt(3, getClassIndex());
				statement.execute();
				statement.close();
			}
			catch (Exception e)
			{
				_log.log(Level.WARNING, "Error could not delete skill: " + e.getMessage(), e);
			}
			finally
			{
				L2DatabaseFactory.close(con);
			}
		}
		
		if (transformId() > 0 || isCursedWeaponEquipped())
			return oldSkill;
		
		final L2ShortCut[] allShortCuts = getAllShortCuts();
		for (L2ShortCut sc : allShortCuts)
		{
			if ((sc != null) && (skill != null) && (sc.getId() == skill.getId()) && (sc.getType() == L2ShortCut.TYPE_SKILL))
				deleteShortCut(sc.getSlot(), sc.getPage());
		}
		return oldSkill;
	}
	
	/**
	 * Add or update a L2PcInstance skill in the character_skills table of the database.<br>
	 * If newClassIndex > -1, the skill will be stored with that class index, not the current one.
	 * @param newSkill 
	 * @param oldSkill 
	 * @param newClassIndex 
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
			_log.log(Level.WARNING, "Error could not store char skills: " + e.getMessage(), e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
	}
	
	/**
	 * Retrieve from the database all skills of this L2PcInstance and add them to _skills.
	 */
	private void restoreSkills()
	{
		Connection con = null;
		try
		{
			// Retrieve all skills of this L2PcInstance from the database
			con = L2DatabaseFactory.getInstance().getConnection();
			final PreparedStatement statement = con.prepareStatement(RESTORE_SKILLS_FOR_CHAR);
			
			statement.setInt(1, getObjectId());
			statement.setInt(2, getClassIndex());
			final ResultSet rset = statement.executeQuery();
			
			// Go though the recordset of this SQL query
			while (rset.next())
			{
				final int id = rset.getInt("skill_id");
				final int level = rset.getInt("skill_level");
				
				// Create a L2Skill object for each record
				final L2Skill skill = SkillTable.getInstance().getInfo(id, level);
				
				if (skill == null)
				{
					_log.warning("Skipped null skill Id: " + id + " Level: " + level + " while restoring player skills for playerObjId: " + getObjectId());
					continue;
				}
				
				// Add the L2Skill object to the L2Character _skills and its Func objects to the calculator set of the L2Character
				super.addSkill(skill);
				
				if (Config.SKILL_CHECK_ENABLE && (!isGM() || Config.SKILL_CHECK_GM))
				{
					if (!SkillTreesData.getInstance().isSkillAllowed(this, skill))
					{
						Util.handleIllegalPlayerAction(this, "Player " + getName() + " has invalid skill " + skill.getName() +
								" ("+skill.getId() + "/" + skill.getLevel() + "), class:" + getTemplate().className, 1);
						if (Config.SKILL_CHECK_REMOVE)
							removeSkill(skill);
					}
				}
			}
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Could not restore character " + this + " skills: " + e.getMessage(), e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
	}
	
	/**
	 * Retrieve from the database all skill effects of this L2PcInstance and add them to the player.
	 */
	@Override
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
				if (skill.hasEffects())
				{
					Env env = new Env();
					env.player = this;
					env.target = this;
					env.skill = skill;
					L2Effect ef;
					for (EffectTemplate et : skill.getEffectTemplates())
					{
						ef = et.getEffect(env);
						if (ef != null)
						{
							ef.setCount(effectCount);
							ef.setFirstTime(effectCurTime);
							ef.scheduleEffect();
						}
					}
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
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Could not restore "+this+" active effect data: " + e.getMessage(), e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
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
			L2DatabaseFactory.close(con);
		}
		
		// Calculate Henna modifiers of this L2PcInstance
		recalcHennaStats();
	}
	
	/**
	 * @return the number of Henna empty slot of the L2PcInstance.
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
	 * @param slot 
	 * @return 
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
			L2DatabaseFactory.close(con);
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
		
		SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.EARNED_S2_S1_S);
		sm.addItemName(henna.getItemIdDye());
		sm.addItemNumber(henna.getAmountDyeRequire() / 2);
		sendPacket(sm);
		
		sendPacket(SystemMessage.getSystemMessage(SystemMessageId.SYMBOL_DELETED));
		
		return true;
	}
	
	/**
	 * Add a Henna to the L2PcInstance, save update in the character_hennas table of the database and send Server->Client HennaInfo/UserInfo packet to this L2PcInstance.<BR><BR>
	 * @param henna 
	 * @return 
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
					L2DatabaseFactory.close(con);
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
	 * @param slot 
	 * @return the Henna of this L2PcInstance corresponding to the selected slot.
	 */
	public L2HennaInstance getHenna(int slot)
	{
		if (slot < 1 || slot > 3)
			return null;
		
		return _henna[slot - 1];
	}
	
	/**
	 * @return the INT Henna modifier of this L2PcInstance.
	 */
	public int getHennaStatINT()
	{
		return _hennaINT;
	}
	
	/**
	 * @return the STR Henna modifier of this L2PcInstance.
	 */
	public int getHennaStatSTR()
	{
		return _hennaSTR;
	}
	
	/**
	 * @return the CON Henna modifier of this L2PcInstance.
	 */
	public int getHennaStatCON()
	{
		return _hennaCON;
	}
	
	/**
	 * @return the MEN Henna modifier of this L2PcInstance.
	 */
	public int getHennaStatMEN()
	{
		return _hennaMEN;
	}
	
	/**
	 * @return the WIT Henna modifier of this L2PcInstance.
	 */
	public int getHennaStatWIT()
	{
		return _hennaWIT;
	}
	
	/**
	 * @return the DEX Henna modifier of this L2PcInstance.
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
	@Override
	public boolean useMagic(L2Skill skill, boolean forceUse, boolean dontMove)
	{
		// Check if the skill is active
		if (skill.isPassive())
		{
			// just ignore the passive skill request. why does the client send it anyway ??
			// Send a Server->Client packet ActionFailed to the L2PcInstance
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
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
				return false;
			}
			
			if (Config.DEBUG && getQueuedSkill() != null)
				_log.info(getQueuedSkill().getSkill().getName() + " is already queued for " + getName() + ".");
			
			// Create a new SkillDat object and queue it in the player _queuedSkill
			setQueuedSkill(skill, forceUse, dontMove);
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
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
			return false;
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
			case TARGET_AURA_CORPSE_MOB:
				target = this;
				break;
			default:
				
				// Get the first target of the list
				target = skill.getFirstOfTargetList(this);
				break;
		}
		
		// Notify the AI with AI_INTENTION_CAST and target
		getAI().setIntention(CtrlIntention.AI_INTENTION_CAST, skill, target);
		return true;
	}
	
	private boolean checkUseMagicConditions(L2Skill skill, boolean forceUse, boolean dontMove)
	{
		L2SkillType sklType = skill.getSkillType();
		
		//************************************* Check Player State *******************************************
		
		// Abnormal effects(ex : Stun, Sleep...) are checked in L2Character useMagic()
		if (isOutOfControl() || isParalyzed() || isStunned() || isSleeping())
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// Check if the player is dead
		if (isDead())
		{
			// Send a Server->Client packet ActionFailed to the L2PcInstance
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		if (isFishing() && (sklType != L2SkillType.PUMPING &&
				sklType != L2SkillType.REELING && sklType != L2SkillType.FISHING))
		{
			//Only fishing skills are available
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.ONLY_FISHING_SKILLS_NOW));
			return false;
		}
		
		if (inObserverMode())
		{
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.OBSERVERS_CANNOT_PARTICIPATE));
			abortCast();
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// Check if the caster is sitting
		if (isSitting())
		{
			// Send a System Message to the caster
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CANT_MOVE_SITTING));
			
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
			case TARGET_AREA_SUMMON:
			case TARGET_AURA_CORPSE_MOB:
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
			
			if (_reuseTimeStamps.containsKey(skill.getReuseHashCode()))
			{
				int remainingTime = (int)(_reuseTimeStamps.get(skill.getReuseHashCode()).getRemaining()/1000);
				int hours = remainingTime/3600;
				int minutes = (remainingTime%3600)/60;
				int seconds = (remainingTime%60);
				if (hours > 0)
				{
					sm = SystemMessage.getSystemMessage(SystemMessageId.S2_HOURS_S3_MINUTES_S4_SECONDS_REMAINING_FOR_REUSE_S1);
					sm.addSkillName(skill);
					sm.addNumber(hours);
					sm.addNumber(minutes);
				}
				else if (minutes > 0)
				{
					sm = SystemMessage.getSystemMessage(SystemMessageId.S2_MINUTES_S3_SECONDS_REMAINING_FOR_REUSE_S1);
					sm.addSkillName(skill);
					sm.addNumber(minutes);
				}
				else
				{
					sm = SystemMessage.getSystemMessage(SystemMessageId.S2_SECONDS_REMAINING_FOR_REUSE_S1);
					sm.addSkillName(skill);
				}
				
				sm.addNumber(seconds);
			}
			else
			{
				sm = SystemMessage.getSystemMessage(SystemMessageId.S1_PREPARED_FOR_REUSE);
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
				sendPacket(SystemMessage.getSystemMessage(SystemMessageId.THERE_IS_NOT_ENOUGH_SOUL));
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
				sendPacket(SystemMessage.getSystemMessage(SystemMessageId.TARGET_IN_PEACEZONE));
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
					sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_CANNOT_ATTACK_A_MEMBER_OF_THE_SAME_TERRITORY));
				else
					sendPacket(SystemMessage.getSystemMessage(SystemMessageId.FORCED_ATTACK_IS_IMPOSSIBLE_AGAINST_SIEGE_SIDE_TEMPORARY_ALLIED_MEMBERS));
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
			// Check for Event Mob's
			if (target instanceof L2EventMonsterInstance && ((L2EventMonsterInstance)target).eventSkillAttackBlocked())
			{
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
					case TARGET_AURA_CORPSE_MOB:
					case TARGET_CLAN:
					case TARGET_PARTY_CLAN:
					case TARGET_ALLY:
					case TARGET_PARTY:
					case TARGET_SELF:
					case TARGET_GROUND:
					case TARGET_AREA_SUMMON:
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
						sendPacket(SystemMessage.getSystemMessage(SystemMessageId.TARGET_TOO_FAR));
						
						// Send a Server->Client packet ActionFailed to the L2PcInstance
						sendPacket(ActionFailed.STATIC_PACKET);
						return false;
					}
				}
				else if (skill.getCastRange() > 0 && !isInsideRadius(target, skill.getCastRange()+getTemplate().collisionRadius, false, false))
				{
					// Send a System Message to the caster
					sendPacket(SystemMessage.getSystemMessage(SystemMessageId.TARGET_TOO_FAR));
					
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
				SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
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
				sendPacket(SystemMessage.getSystemMessage(SystemMessageId.TARGET_IN_PEACEZONE));
				
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
				case TARGET_AURA_CORPSE_MOB:
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
				sendPacket(SystemMessage.getSystemMessage(SystemMessageId.INCORRECT_TARGET));
				
				// Send a Server->Client packet ActionFailed to the L2PcInstance
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
		}
		
		// Check if the skill is Drain Soul (Soul Crystals) and if the target is a MOB
		if (sklType == L2SkillType.DRAIN_SOUL)
		{
			if (!(target instanceof L2MonsterInstance))
			{
				// Send a System Message to the L2PcInstance
				sendPacket(SystemMessage.getSystemMessage(SystemMessageId.INCORRECT_TARGET));
				
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
					sendPacket(SystemMessage.getSystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
					
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
					sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CANT_SEE_TARGET));
					sendPacket(ActionFailed.STATIC_PACKET);
					return false;
				}
			}
			else if (!GeoData.getInstance().canSeeTarget(this, target))
			{
				sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CANT_SEE_TARGET));
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
			sm = SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
			sm.addSkillName(skill);
		}
		else if (!castle.getArtefacts().contains(getTarget()))
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.INCORRECT_TARGET);
		}
		else if (!castle.getSiege().getIsInProgress())
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
			sm.addSkillName(skill);
		}
		else if (!Util.checkIfInRange(200, this, getTarget(), true))
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.DIST_TOO_FAR_CASTING_STOPPED);
		}
		else if (castle.getSiege().getAttackerClan(getClan()) == null)
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
			sm.addSkillName(skill);
		}
		else
		{
			if (!isCheckOnly)
			{
				sm = SystemMessage.getSystemMessage(SystemMessageId.OPPONENT_STARTED_ENGRAVING);
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
			sm = SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
			sm.addSkillName(skill);
		}
		else if (fort.getFlagPole() != getTarget())
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.INCORRECT_TARGET);
		}
		else if (!fort.getSiege().getIsInProgress())
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
			sm.addSkillName(skill);
		}
		else if (!Util.checkIfInRange(200, this, getTarget(), true))
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.DIST_TOO_FAR_CASTING_STOPPED);
		}
		else if (fort.getSiege().getAttackerClan(getClan()) == null)
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
			sm.addSkillName(skill);
		}
		else
		{
			if (!isCheckOnly)
				fort.getSiege().announceToPlayer(SystemMessage.getSystemMessage(SystemMessageId.S1_TRYING_RAISE_FLAG),getClan().getName());
			return true;
		}
		
		sendPacket(sm);
		return false;
	}
	
	public boolean isInLooterParty(int LooterId)
	{
		L2PcInstance looter = L2World.getInstance().getPlayer(LooterId);
		
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
			SkillDat skilldat = getCurrentSkill();
			SkillDat skilldatpet = getCurrentPetSkill();
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
			else if ((skilldat != null && !skilldat.isCtrlPressed() && skill.isOffensive() && !srcIsSummon)
					|| (skilldatpet != null && !skilldatpet.isCtrlPressed() && skill.isOffensive() && srcIsSummon))
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
	 * @return True if the L2PcInstance is a Mage.
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
	 * @return 
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
	 * @return the type of Pet mounted (0 : none, 1 : Strider, 2 : Wyvern, 3: Wolf).
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
	
	public final void stopAllEffectsNotStayOnSubclassChange()
	{
		for (L2Effect effect : _effects.getAllEffects())
		{
			if (effect != null && !effect.getSkill().isStayOnSubclassChange())
				effect.exit(true);
		}
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
			for (L2CubicInstance cubic : _cubics.getValues(new L2CubicInstance[_cubics.size()]))
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
			for (L2CubicInstance cubic : _cubics.getValues(new L2CubicInstance[_cubics.size()]))
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
	 * @return True if the Inventory is disabled.
	 */
	public boolean isInventoryDisabled()
	{
		return _inventoryDisable;
	}
	
	private class InventoryEnable implements Runnable
	{
		@Override
		public void run()
		{
			_inventoryDisable = false;
		}
	}
	
	public L2TIntObjectHashMap<L2CubicInstance> getCubics()
	{
		return _cubics;
	}
	
	/**
	 * Add a L2CubicInstance to the L2PcInstance _cubics.
	 * @param id 
	 * @param level 
	 * @param matk 
	 * @param activationtime 
	 * @param activationchance 
	 * @param maxcount 
	 * @param totalLifetime 
	 * @param givenByOther 
	 */
	public void addCubic(int id, int level, double matk, int activationtime, int activationchance, int maxcount, int totalLifetime, boolean givenByOther)
	{
		if (Config.DEBUG)
			_log.info("L2PcInstance(" + getName() + "): addCubic(" + id + "|" + level + "|" + matk + ")");
		L2CubicInstance cubic = new L2CubicInstance(this, id, level, (int) matk, activationtime, activationchance, maxcount, totalLifetime, givenByOther);
		
		_cubics.put(id, cubic);
	}
	
	/**
	 * @param id a L2CubicInstance from the L2PcInstance _cubics.
	 */
	public void delCubic(int id)
	{
		_cubics.remove(id);
	}
	
	/**
	 * @param id 
	 * @return the L2CubicInstance corresponding to the Identifier of the L2PcInstance _cubics.
	 */
	public L2CubicInstance getCubic(int id)
	{
		return _cubics.get(id);
	}
	
	/**
	 * @return the modifier corresponding to the Enchant Effect of the Active Weapon (Min : 127).
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
	 * @param folkNpc 
	 */
	public void setLastFolkNPC(L2Npc folkNpc)
	{
		_lastFolkNpc = folkNpc;
	}
	
	/**
	 * @return the _lastFolkNpc of the L2PcInstance corresponding to the last Folk wich one the player talked.
	 */
	public L2Npc getLastFolkNPC()
	{
		return _lastFolkNpc;
	}
	
	/**
	 * @return True if L2PcInstance is a participant in the Festival of Darkness.
	 */
	public boolean isFestivalParticipant()
	{
		return SevenSignsFestival.getInstance().isParticipant(this);
	}
	
	public void addAutoSoulShot(int itemId)
	{
		_activeSoulShots.add(itemId);
	}
	
	public boolean removeAutoSoulShot(int itemId)
	{
		return _activeSoulShots.remove(itemId);
	}
	
	public Set<Integer> getAutoSoulShot()
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
			for (int itemId : _activeSoulShots)
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
										handler.useItem(this, item, false);
									
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
										handler.useItem(this, item, false);
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
										handler.useItem(this, item, false);
									
									break;
							}
						}
						else
						{
							if (itemId == 6645 || itemId == 20332)
							{
								handler = ItemHandler.getInstance().getItemHandler(item.getEtcItem());
								
								if (handler != null)
									handler.useItem(this, item, false);
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
			_log.log(Level.WARNING, toString() , npe);
		}
	}
	
	/**
	 * Cancel autoshot for all shots matching crystaltype<BR>
	 * {@link L2Item#getCrystalType()}
	 * 
	 * @param crystalType int type to disable
	 */
	public void disableAutoShotByCrystalType(int crystalType)
	{
		for (int itemId : _activeSoulShots)
		{
			if (ItemTable.getInstance().getTemplate(itemId).getCrystalType() == crystalType)
				disableAutoShot(itemId);
		}
	}
	
	/**
	 * Cancel autoshot use for shot itemId
	 * @param itemId int id to disable
	 * @return true if canceled.
	 */
	public boolean disableAutoShot(int itemId)
	{
		if (_activeSoulShots.contains(itemId))
		{
			removeAutoSoulShot(itemId);
			sendPacket(new ExAutoSoulShot(itemId, 0));
			
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.AUTO_USE_OF_S1_CANCELLED);
			sm.addString(ItemTable.getInstance().getTemplate(itemId).getName());
			sendPacket(sm);
			return true;
		}
		return false;
	}
	
	/**
	 * Cancel all autoshots for player
	 */
	public void disableAutoShotsAll()
	{
		for (int itemId : _activeSoulShots)
		{
			sendPacket(new ExAutoSoulShot(itemId, 0));
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.AUTO_USE_OF_S1_CANCELLED);
			sm.addString(ItemTable.getInstance().getTemplate(itemId).getName());
			sendPacket(sm);
		}
		_activeSoulShots.clear();
	}
	
	private ScheduledFuture<?> _taskWarnUserTakeBreak;
	
	private class WarnUserTakeBreak implements Runnable
	{
		@Override
		public void run()
		{
			if (L2PcInstance.this.isOnline())
			{
				SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.PLAYING_FOR_LONG_TIME);
				L2PcInstance.this.sendPacket(msg);
			}
			else
				stopWarnUserTakeBreak();
		}
	}
	
	private class RentPetTask implements Runnable
	{
		@Override
		public void run()
		{
			stopRentPet();
		}
	}
	
	private class WaterTask implements Runnable
	{
		@Override
		public void run()
		{
			double reduceHp = getMaxHp()/100.0;
			
			if (reduceHp < 1)
				reduceHp = 1;
			
			reduceCurrentHp(reduceHp,L2PcInstance.this,false, false, null);
			//reduced hp, becouse not rest
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.DROWN_DAMAGE_S1);
			sm.addNumber((int)reduceHp);
			sendPacket(sm);
			
		}
	}
	
	private class LookingForFishTask implements Runnable
	{
		boolean _isNoob, _isUpperGrade;
		int _fishType, _fishGutsCheck;
		long _endTaskTime;
		
		protected LookingForFishTask(int fishWaitTime, int fishGutsCheck, int fishType, boolean isNoob, boolean isUpperGrade)
		{
			_fishGutsCheck = fishGutsCheck;
			_endTaskTime = System.currentTimeMillis() + fishWaitTime + 10000;
			_fishType = fishType;
			_isNoob = isNoob;
			_isUpperGrade = isUpperGrade;
		}
		
		@Override
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
		_lastX = getX();
		_lastY = getY();
		_lastZ = getZ();
		
		stopEffects(L2EffectType.HIDE);
		
		_observerMode = true;
		setTarget(null);
		setIsParalyzed(true);
		startParalyze();
		setIsInvul(true);
		getAppearance().setInvisible();
		//sendPacket(new GMHide(1));
		sendPacket(new ObservationMode(x, y, z));
		getKnownList().removeAllKnownObjects(); // reinit knownlist
		setXYZ(x, y, z);
		broadcastUserInfo();
	}
	
	public void setLastCords(int x, int y, int z)
	{
		_lastX = getX();
		_lastY = getY();
		_lastZ = getZ();
	}
	
	public void enterOlympiadObserverMode(Location loc, int id)
	{
		if (getPet() != null)
			getPet().unSummon(this);
		
		stopEffects(L2EffectType.HIDE);
		
		if (!getCubics().isEmpty())
		{
			for (L2CubicInstance cubic : _cubics.getValues(new L2CubicInstance[_cubics.size()]))
			{
				cubic.stopAction();
				cubic.cancelDisappear();
			}
			
			getCubics().clear();
		}
		
		if (getParty() != null)
			getParty().removePartyMember(this, messageType.Expelled);
		
		_olympiadGameId = id;
		if (isSitting())
			standUp();
		if (!_observerMode)
		{
			_lastX = getX();
			_lastY = getY();
			_lastZ = getZ();
		}
		
		_observerMode = true;
		setTarget(null);
		setIsInvul(true);
		getAppearance().setInvisible();
		//sendPacket(new GMHide(1));
		teleToLocation(loc, false);
		sendPacket(new ExOlympiadMode(3));
		
		broadcastUserInfo();
	}
	
	public void leaveObserverMode()
	{
		setTarget(null);
		getKnownList().removeAllKnownObjects(); // reinit knownlist
		setXYZ(_lastX, _lastY, _lastZ);
		setIsParalyzed(false);
		stopParalyze(false);
		//sendPacket(new GMHide(0));
		if (!AdminCommandAccessRights.getInstance().hasAccess("admin_invis", getAccessLevel()))
			getAppearance().setVisible();
		if (!AdminCommandAccessRights.getInstance().hasAccess("admin_invul", getAccessLevel()))
			setIsInvul(false);
		if (getAI() != null)
			getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		
		setFalling(); // prevent receive falling damage
		_observerMode = false;
		setLastCords(0, 0, 0);
		sendPacket(new ObservationReturn(this));
		broadcastUserInfo();
	}
	
	public void leaveOlympiadObserverMode()
	{
		if (_olympiadGameId == -1)
			return;
		_olympiadGameId = -1;
		_observerMode = false;
		setTarget(null);
		sendPacket(new ExOlympiadMode(0));
		setInstanceId(0);
		teleToLocation(_lastX, _lastY, _lastZ, true);
		//sendPacket(new GMHide(0));
		if (!AdminCommandAccessRights.getInstance().hasAccess("admin_invis", getAccessLevel()))
			getAppearance().setVisible();
		if (!AdminCommandAccessRights.getInstance().hasAccess("admin_invul", getAccessLevel()))
			setIsInvul(false);
		if (getAI() != null)
			getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		setLastCords(0, 0, 0);
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
	
	public int getLastX()
	{
		return _lastX;
	}
	
	public int getLastY()
	{
		return _lastY;
	}
	
	public int getLastZ()
	{
		return _lastZ;
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
		SystemMessage sm = SystemMessage.getSystemMessage(_noDuelReason);
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
		if (isMounted() || isInBoat()) { _noDuelReason = SystemMessageId.C1_CANNOT_DUEL_BECAUSE_C1_IS_CURRENTLY_RIDING_A_BOAT_STEED_OR_STRIDER; return false; }
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
	 * @param classId
	 * @param classIndex
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
				statement.close();
			}
			catch (Exception e) {
				_log.log(Level.WARNING, "WARNING: Could not add character sub class for " + getName() + ": " + e.getMessage(), e);
				return false;
			}
			finally
			{
				L2DatabaseFactory.close(con);
			}
			
			// Commit after database INSERT incase exception is thrown.
			getSubClasses().put(newClass.getClassIndex(), newClass);
			
			if (Config.DEBUG)
				_log.info(getName() + " added class ID " + classId + " as a sub class at index " + classIndex + ".");
			
			final ClassId subTemplate = ClassId.values()[classId];
			final FastMap<Integer, L2SkillLearn> skillTree = SkillTreesData.getInstance().getCompleteClassSkillTree(subTemplate);
			final FastMap<Integer, L2Skill> prevSkillList = new FastMap<Integer, L2Skill>();
			
			for (L2SkillLearn skillInfo : skillTree.values())
			{
				if (skillInfo.getGetLevel() <= 40)
				{
					L2Skill prevSkill = prevSkillList.get(skillInfo.getSkillId());
					L2Skill newSkill = SkillTable.getInstance().getInfo(skillInfo.getSkillId(), skillInfo.getSkillLevel());
					
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
	 * @param classIndex
	 * @param newClassId
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
				_log.log(Level.WARNING, "Could not modify sub class for " + getName() + " to class index " + classIndex + ": " + e.getMessage(), e);
				
				// This must be done in order to maintain data consistency.
				getSubClasses().remove(classIndex);
				return false;
			}
			finally
			{
				L2DatabaseFactory.close(con);
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
	 * @return 
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
			
			if (getPet() instanceof L2SummonInstance)
				getPet().unSummon(this);
			
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
					_log.log(Level.WARNING, "Could not switch " + getName() + "'s sub class to class index " + classIndex + ": " + e.getMessage(), e);
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
			 */
			
			for (L2Skill oldSkill : getAllSkills())
				super.removeSkill(oldSkill);
			
			stopAllEffectsExceptThoseThatLastThroughDeath();
			stopAllEffectsNotStayOnSubclassChange();
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
			
			broadcastPacket(new SocialAction(this, SocialAction.LEVEL_UP));
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
			//ThreadPoolManager.getInstance().removeGeneral((Runnable)_taskWarnUserTakeBreak);
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
				teleToLocation(MapRegionManager.TeleportWhereType.Town);
			
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
				teleToLocation(MapRegionManager.TeleportWhereType.Town);
				setIsIn7sDungeon(false);
				sendMessage("You have been teleported to the nearest town due to the beginning of the Seal Validation period.");
			}
		}
		else
		{
			if (!isGM() && isIn7sDungeon() && SevenSigns.getInstance().getPlayerCabal(getObjectId()) == SevenSigns.CABAL_NULL)
			{
				teleToLocation(MapRegionManager.TeleportWhereType.Town);
				setIsIn7sDungeon(false);
				sendMessage("You have been teleported to the nearest town because you have not signed for any cabal.");
			}
		}
		
		// jail task
		updatePunishState();
		
		if (isGM())
		{
			if (isInvul())
				sendMessage("Entering world in Invulnerable mode.");
			if (getAppearance().getInvisible())
				sendMessage("Entering world in Invisible mode.");
			if (isSilenceMode())
				sendMessage("Entering world in Silence mode.");
		}
		
		revalidateZone(true);
		
		notifyFriends();
		if (!isGM() && Config.DECREASE_SKILL_LEVEL)
			checkPlayerSkills();
	}
	
	public long getLastAccess()
	{
		return _lastAccess;
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
	public void setName(String value)
	{
		super.setName(value);
		if (Config.CACHE_CHAR_NAMES)
			CharNameTable.getInstance().addName(this);
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
		if (isResurrectionBlocked())
			return;
		
		if (_reviveRequested == 1)
		{
			if (_revivePet == Pet)
			{
				Reviver.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.RES_HAS_ALREADY_BEEN_PROPOSED)); // Resurrection is already been proposed.
			}
			else
			{
				if (Pet)
					Reviver.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CANNOT_RES_PET2)); // A pet cannot be resurrected while it's owner is in the process of resurrecting.
				else
					Reviver.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.MASTER_CANNOT_RES)); // While a pet is attempting to resurrect, it cannot help in resurrecting its master.
			}
			return;
		}
		if((Pet && getPet() != null && getPet().isDead()) || (!Pet && isDead()))
		{
			_reviveRequested = 1;
			int restoreExp = 0;
			if (isPhoenixBlessed())
				_revivePower=100;
			else if (isAffected(CharEffectList.EFFECT_FLAG_CHARM_OF_COURAGE))
				_revivePower=0;
			else
				_revivePower = Formulas.calculateSkillResurrectRestorePercent(skill.getPower(), Reviver);
			
			restoreExp = (int)Math.round((getExpBeforeDeath() - getExp()) * _revivePower / 100);
			
			_revivePet = Pet;
			
			if (isAffected(CharEffectList.EFFECT_FLAG_CHARM_OF_COURAGE))
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
		if (isSpawnProtected())
		{
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_ARE_NO_LONGER_PROTECTED_FROM_AGGRESSIVE_MONSTERS));
			
			if (Config.RESTORE_SERVITOR_ON_RECONNECT && getPet() == null && CharSummonTable.getInstance().getServitors().containsKey(getObjectId()))
				CharSummonTable.getInstance().restoreServitor(this);
			if (Config.RESTORE_PET_ON_RECONNECT && getPet() == null && CharSummonTable.getInstance().getPets().containsKey(getObjectId()))
				CharSummonTable.getInstance().restorePet(this);
		}
		if (isTeleportProtected())
			sendMessage("Teleport spawn protection ended.");
		setProtection(false);
		setTeleportProtection(false);
	}
	
	/**
	 * Expertise of the L2PcInstance (None=0, D=1, C=2, B=3, A=4, S=5, S80=6, S84=7)
	 * @return int Expertise skill level.
	 */
	public int getExpertiseLevel()
	{
		int level = getSkillLevel(239);
		if (level < 0)
			level = 0;
		return level;
	}
	
	@Override
	public void teleToLocation(int x, int y, int z, int heading, boolean allowRandomOffset)
	{
		if (getVehicle() != null && !getVehicle().isTeleporting())
			setVehicle(null);
		
		if (isFlyingMounted() && z < -1005)
			z = -1005;
		
		super.teleToLocation(x, y, z, heading, allowRandomOffset);
	}
	
	@Override
	public final void onTeleported()
	{
		super.onTeleported();
		
		if (isInAirShip())
			getAirShip().sendInfo(this);
		
		// Force a revalidation
		revalidateZone(true);
		
		checkItemRestriction();
		
		if ((Config.PLAYER_TELEPORT_PROTECTION > 0) && !isInOlympiadMode())
			setTeleportProtection(true);
		
		// Trained beast is after teleport lost
		if (getTrainedBeasts() != null)
		{
			for(L2TamedBeastInstance tamedBeast : getTrainedBeasts())
				tamedBeast.deleteMe();
			getTrainedBeasts().clear();
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
			if ((_teleportWatchdog == null) && (Config.TELEPORT_WATCHDOG_TIMEOUT > 0))
			{
				synchronized(this)
				{
					if (_teleportWatchdog == null)
						_teleportWatchdog = ThreadPoolManager.getInstance().scheduleGeneral(new TeleportWatchdog(), Config.TELEPORT_WATCHDOG_TIMEOUT * 1000);
				}
			}
		}
		else
		{
			if (_teleportWatchdog != null)
			{
				_teleportWatchdog.cancel(false);
				//ThreadPoolManager.getInstance().removeGeneral((Runnable)_teleportWatchdog);
				_teleportWatchdog = null;
			}
		}
	}
	
	private class TeleportWatchdog implements Runnable
	{
		private final L2PcInstance _player;
		
		TeleportWatchdog()
		{
			_player = L2PcInstance.this;
		}
		
		@Override
		public void run()
		{
			if (_player == null || !_player.isTeleporting())
				return;
			
			if (Config.DEBUG)
				_log.warning("Player " + _player.getName() + " teleport timeout expired");
			_player.onTeleported();
		}
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
	public void reduceCurrentHp(double value, L2Character attacker, boolean awake, boolean isDOT, L2Skill skill)
	{
		if (skill != null)
			getStatus().reduceHp(value, attacker, awake, isDOT, skill.isToggle(), skill.getDmgDirectlyToHP());
		else
			getStatus().reduceHp(value, attacker, awake, isDOT, false, false);

		// notify the tamed beast of attacks
		if (getTrainedBeasts() != null )
			for(L2TamedBeastInstance tamedBeast : getTrainedBeasts())
				tamedBeast.onOwnerGotAttacked(attacker);
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
		_log.warning("[L2PcInstance] player ["+getName()+"] sent invalid bypass '"+cmd+"'.");
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
	 * @param objectId item object id
	 * @param action just for login porpouse
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
		if (getPet() != null && getPet().getControlObjectId() == objectId || getMountObjectID() == objectId)
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
		return _vehicle != null && _vehicle.isBoat();
	}
	
	/**
	 * @return
	 */
	public L2BoatInstance getBoat()
	{
		return (L2BoatInstance)_vehicle;
	}
	
	/**
	 * @return Returns the inAirShip.
	 */
	public boolean isInAirShip()
	{
		return _vehicle != null && _vehicle.isAirShip();
	}
	
	/**
	 * @return
	 */
	public L2AirShipInstance getAirShip()
	{
		return (L2AirShipInstance) _vehicle;
	}
	
	public L2Vehicle getVehicle()
	{
		return _vehicle;
	}
	
	public void setVehicle(L2Vehicle v)
	{
		if (v == null && _vehicle != null)
			_vehicle.removePassenger(this);
		
		_vehicle = v;
	}
	
	public boolean isInVehicle()
	{
		return _vehicle != null;
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
	public Point3D getInVehiclePosition()
	{
		return _inVehiclePosition;
	}
	
	public void setInVehiclePosition(Point3D pt)
	{
		_inVehiclePosition = pt;
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
	@Override
	public void deleteMe()
	{
		cleanup();
		store();
		super.deleteMe();
	}
	
	private synchronized void cleanup()
	{
		// Set the online Flag to True or False and update the characters table of the database with online status and lastAccess (called when login and logout)
		try
		{
			if (!isOnline())
				_log.log(Level.SEVERE, "deleteMe() called on offline character "+this, new RuntimeException());
			setOnlineStatus(false, true);
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "deleteMe()", e);
		}
		
 		try
 		{
			if(Config.ENABLE_BLOCK_CHECKER_EVENT && getBlockCheckerArena() != -1)
				HandysBlockCheckerManager.getInstance().onDisconnect(this);
		}
		catch(Exception e)
		{
			_log.log(Level.SEVERE, "deleteMe()", e);
		}
		
		try
		{
			_isOnline = false;
			abortAttack();
			abortCast();
			stopMove(null);
			setDebug(null);
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
					FortSiegeManager.getInstance().dropCombatFlag(this, fort.getFortId());
				else
				{
					int slot = getInventory().getSlotFromItem(getInventory().getItemByItemId(9819));
					getInventory().unEquipItemInBodySlot(slot);
					destroyItem("CombatFlag", getInventory().getItemByItemId(9819), null, true);
				}
			}
			else if (isCombatFlagEquipped())
			{
				TerritoryWarManager.getInstance().dropCombatFlag(this, false, false);
			}
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "deleteMe()", e);
		}
		
		try
		{
			PartyMatchWaitingList.getInstance().removePlayer(this);
			if (_partyroom != 0)
			{
				PartyMatchRoom room = PartyMatchRoomList.getInstance().getRoom(_partyroom);
				if (room != null)
				{
					room.deleteMember(this);
				}
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
		// Recommendations must be saved before task (timer) is canceled
		try
		{
			storeRecommendations();
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
		
		// Remove from world regions zones
		final L2WorldRegion oldRegion = getWorldRegion();
		
		if (oldRegion != null)
			oldRegion.removeFromZones(this);
		
		// Remove the L2PcInstance from the world
		try
		{
			decayMe();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "deleteMe()", e);
		}
		
		// If a Party is in progress, leave it (and festival party)
		if (isInParty())
		{
			try
			{
				leaveParty();
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "deleteMe()", e);
			}
		}
		
		if (OlympiadManager.getInstance().isRegistered(this) || getOlympiadGameId() != -1) // handle removal from olympiad game
			OlympiadManager.getInstance().removeDisconnectedCompetitor(this);
		
		// If the L2PcInstance has Pet, unsummon it
		if (getPet() != null)
		{
			try
			{
				getPet().setRestoreSummon(true);
				
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
			cancelActiveTrade();
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
				setXYZInvisible(_lastX, _lastY, _lastZ);
			
			if (getVehicle() != null)
				getVehicle().oustPlayer(this);
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
			getFreight().deleteMe(); 
		} 
		catch (Exception e) 
	 	{ 
			_log.log(Level.SEVERE, "deleteMe()", e); 
	 	} 

		
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
			_log.log(Level.WARNING, "Exception on deleteMe() changeCommunityBoard: " + e.getMessage(), e);
		}
		
		try
		{
			notifyFriends();
			getBlockList().playerLogout();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Exception on deleteMe() notifyFriends: " + e.getMessage(), e);
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
		sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CAST_LINE_AND_START_FISHING));
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
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.BAIT_LOST_FISH_GOT_AWAY));
		_fishCombat = null;
		_lure = null;
		//Ends fishing
		broadcastPacket(new ExFishingEnd(win, this));
		sendPacket(SystemMessage.getSystemMessage(SystemMessageId.REEL_LINE_AND_STOP_FISHING));
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
	 * @return the current skill in use or return null.
	 */
	public SkillDat getCurrentSkill()
	{
		return _currentSkill;
	}
	
	/**
	 * Create a new SkillDat object and set the player _currentSkill.<BR><BR>
	 * @param currentSkill 
	 * @param ctrlPressed 
	 * @param shiftPressed 
	 */
	public void setCurrentSkill(L2Skill currentSkill, boolean ctrlPressed, boolean shiftPressed)
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
	 * @return the current pet skill in use or return null.
	 */
	public SkillDat getCurrentPetSkill()
	{
		return _currentPetSkill;
	}
	
	/**
	 * Create a new SkillDat object and set the player _currentPetSkill.<br><br>
	 * @param currentSkill 
	 * @param ctrlPressed 
	 * @param shiftPressed 
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
	 * @param queuedSkill 
	 * @param ctrlPressed 
	 * @param shiftPressed 
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
					_punishTask = ThreadPoolManager.getInstance().scheduleGeneral(new PunishTask(), _punishTimer);
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
					_punishTask = ThreadPoolManager.getInstance().scheduleGeneral(new PunishTask(), _punishTimer);
					sendMessage("You are in jail for "+delayInMinutes+" minutes.");
				}
				
				if (!TvTEvent.isInactive() && TvTEvent.isPlayerParticipant(getObjectId()))
					TvTEvent.removeParticipant(getObjectId());
				if (OlympiadManager.getInstance().isRegisteredInComp(this))
					OlympiadManager.getInstance().removeDisconnectedCompetitor(this);
				
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
				_punishTask = ThreadPoolManager.getInstance().scheduleGeneral(new PunishTask(), _punishTimer);
				sendMessage("You are still "+getPunishLevel().string()+" for "+Math.round(_punishTimer/60000f)+" minutes.");
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
			//ThreadPoolManager.getInstance().removeGeneral((Runnable)_punishTask);
			_punishTask = null;
		}
	}
	
	private class PunishTask implements Runnable
	{
		@Override
		public void run()
		{
			L2PcInstance.this.setPunishLevel(PunishLevel.NONE, 0);
		}
	}
	public void startFameTask(long delay, int fameFixRate)
	{
		if (getLevel() < 40 || getClassId().level() < 2)
			return;
		if (_fameTask == null)
			_fameTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new FameTask(fameFixRate), delay, delay);
	}
	
	public void stopFameTask()
	{
		if (_fameTask != null)
		{
			_fameTask.cancel(false);
			//ThreadPoolManager.getInstance().removeGeneral((Runnable)_fameTask);
			_fameTask = null;
		}
	}
	
	private class FameTask implements Runnable
	{
		private final L2PcInstance _player;
		private final int _value;
		
		protected FameTask(int value)
		{
			_player = L2PcInstance.this;
			_value = value;
		}
		
		@Override
		public void run()
		{
			if (_player == null || (_player.isDead() && !Config.FAME_FOR_DEAD_PLAYERS))
				return;
			if ((_player.getClient() == null || _player.getClient().isDetached()) && !Config.OFFLINE_FAME)
				return;
			_player.setFame(_player.getFame() + _value);
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.ACQUIRED_S1_REPUTATION_SCORE);
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
			//ThreadPoolManager.getInstance().removeGeneral((Runnable)_vitalityTask);
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
		
		@Override
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
	 * @param power
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
	
	public final void setIsRidingStrider(boolean mode)
	{
		_isRidingStrider = mode;
	}
	
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
	 * @param npc
	 */
	public void absorbSoul(L2Skill skill, L2Npc npc)
	{
		if (_souls >= skill.getNumSouls())
		{
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.SOUL_CANNOT_BE_INCREASED_ANYMORE);
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
		
		SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.YOUR_SOUL_HAS_INCREASED_BY_S1_SO_IT_IS_NOW_AT_S2);
		sm.addNumber(count);
		sm.addNumber(_souls);
		sendPacket(sm);
		
		restartSoulTask();
		
		sendPacket(new EtcStatusUpdate(this));
	}
	
	/**
	 * Decreases existing Souls.
	 * @param count
	 * @param skill 
	 * @return 
	 */
	public boolean decreaseSouls(int count, L2Skill skill)
	{
		if (getSouls() <= 0 && skill.getSoulConsumeCount() > 0)
		{
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.THERE_IS_NOT_ENOUGH_SOUL));
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
			_soulTask = ThreadPoolManager.getInstance().scheduleGeneral(new SoulTask(), 600000);
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
			//ThreadPoolManager.getInstance().removeGeneral((Runnable)_soulTask);
			_soulTask = null;
		}
	}
	
	private class SoulTask implements Runnable
	{
		@Override
		public void run()
		{
			L2PcInstance.this.clearSouls();
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
		_shortBuffTask = ThreadPoolManager.getInstance().scheduleGeneral(new ShortBuffTask(), time*1000);
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
				&& !isLucky()
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
		SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.DEATH_PENALTY_LEVEL_S1_ADDED);
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
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.DEATH_PENALTY_LEVEL_S1_ADDED);
			sm.addNumber(getDeathPenaltyBuffLevel());
			sendPacket(sm);
		}
		else
		{
			sendPacket(new EtcStatusUpdate(this));
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.DEATH_PENALTY_LIFTED));
		}
	}
	
	public void restoreDeathPenaltyBuffLevel()
	{
		if(getDeathPenaltyBuffLevel() > 0)
			addSkill(SkillTable.getInstance().getInfo(5076, getDeathPenaltyBuffLevel()), false);
	}
	
	private final L2TIntObjectHashMap<TimeStamp> _reuseTimeStamps = new L2TIntObjectHashMap<TimeStamp>();
	private boolean _canFeed;
	private int _eventEffectId = 0;
	private boolean _isInSiege;
	private boolean _isInHideoutSiege = false; //TODO: Implement it.
	
	public TimeStamp[] getReuseTimeStamps()
	{
		return _reuseTimeStamps.getValues(new TimeStamp[_reuseTimeStamps.size()]);
	}
	
	public L2TIntObjectHashMap<TimeStamp> getReuseTimeStamp()
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
	 * @param skill
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
	 * @param skill
	 * @param reuse
	 * @param systime
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
			if (target instanceof L2PcInstance)
			{
				SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_EVADED_C2_ATTACK);
				sm.addPcName((L2PcInstance) target);
				sm.addCharName(this);
				target.sendPacket(sm);
			}
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.C1_ATTACK_WENT_ASTRAY).addPcName(this));
			return;
		}
		
		// Check if hit is critical
		if (pcrit)
		{
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.C1_HAD_CRITICAL_HIT).addPcName(this));
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
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CRITICAL_HIT_MAGIC));
		
		if (isInOlympiadMode() &&
				target instanceof L2PcInstance &&
				((L2PcInstance)target).isInOlympiadMode() &&
				((L2PcInstance)target).getOlympiadGameId() == getOlympiadGameId())
		{
			OlympiadGameManager.getInstance().notifyCompetitorDamage(this, damage);
		}
		
		final SystemMessage sm;
		
		if (target.isInvul() && !(target instanceof L2Npc))
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.ATTACK_WAS_BLOCKED);
		}
		else if (target instanceof L2DoorInstance || target instanceof L2ControlTowerInstance)
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_DID_S1_DMG);
			sm.addNumber(damage);
		}
		else
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.C1_GAVE_C2_DAMAGE_OF_S3);
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
	/**
	 * Request Teleport 
	 * @param requester 
	 * @param skill 
	 * @return
	 */
	public boolean teleportRequest(L2PcInstance requester, L2Skill skill)
	{
		if (_summonRequest.getTarget() != null && requester != null)
			return false;
		_summonRequest.setTarget(requester, skill);
		return true;
	}
	
	/**
	 * Action teleport 
	 * @param answer 
	 * @param requesterId
	 */
	public void teleportAnswer(int answer, int requesterId)
	{
		if (_summonRequest.getTarget() == null)
			return;
		if (answer == 1 && _summonRequest.getTarget().getObjectId() == requesterId)
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
				SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_REQUIRED_FOR_SUMMONING);
				sm.addItemName(summonSkill.getTargetConsumeId());
				targetChar.sendPacket(sm);
				return;
			}
			targetChar.getInventory().destroyItemByItemId("Consume", itemConsumeId, itemConsumeCount, summonerChar, targetChar);
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_DISAPPEARED);
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
			summonerChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.THIS_ITEM_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT));
			return false;
		}
		
		if (summonerChar.inObserverMode())
		{
			return false;
		}
		
		if (!TvTEvent.onEscapeUse(summonerChar.getObjectId()))
		{
			summonerChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOUR_TARGET_IS_IN_AN_AREA_WHICH_BLOCKS_SUMMONING));
			return false;
		}
		
		if (summonerChar.isInsideZone(L2Character.ZONE_NOSUMMONFRIEND) || summonerChar.isFlyingMounted())
		{
			summonerChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOUR_TARGET_IS_IN_AN_AREA_WHICH_BLOCKS_SUMMONING));
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
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_IS_DEAD_AT_THE_MOMENT_AND_CANNOT_BE_SUMMONED);
			sm.addPcName(targetChar);
			summonerChar.sendPacket(sm);
			return false;
		}
		
		if (targetChar.isInStoreMode())
		{
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_CURRENTLY_TRADING_OR_OPERATING_PRIVATE_STORE_AND_CANNOT_BE_SUMMONED);
			sm.addPcName(targetChar);
			summonerChar.sendPacket(sm);
			return false;
		}
		
		if (targetChar.isRooted() || targetChar.isInCombat())
		{
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_IS_ENGAGED_IN_COMBAT_AND_CANNOT_BE_SUMMONED);
			sm.addPcName(targetChar);
			summonerChar.sendPacket(sm);
			return false;
		}
		
		if (targetChar.isInOlympiadMode())
		{
			summonerChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_CANNOT_SUMMON_PLAYERS_WHO_ARE_IN_OLYMPIAD));
			return false;
		}
		
		if (targetChar.isFestivalParticipant() || targetChar.isFlyingMounted())
		{
			summonerChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOUR_TARGET_IS_IN_AN_AREA_WHICH_BLOCKS_SUMMONING));
			return false;
		}
		
		if (targetChar.inObserverMode())
		{
			summonerChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.C1_STATE_FORBIDS_SUMMONING).addCharName(targetChar));
			return false;
		}
		
		if (targetChar.isCombatFlagEquipped())
		{
			summonerChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOUR_TARGET_IS_IN_AN_AREA_WHICH_BLOCKS_SUMMONING));
			return false;
		}
		
		if (!TvTEvent.onEscapeUse(targetChar.getObjectId()))
		{
			summonerChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOUR_TARGET_IS_IN_AN_AREA_WHICH_BLOCKS_SUMMONING));
			return false;
		}
		
		if (targetChar.isInsideZone(L2Character.ZONE_NOSUMMONFRIEND))
		{
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_IN_SUMMON_BLOCKING_AREA);
			sm.addString(targetChar.getName());
			summonerChar.sendPacket(sm);
			return false;
		}
		
		if (summonerChar.getInstanceId() > 0)
		{
			Instance summonerInstance = InstanceManager.getInstance().getInstance(summonerChar.getInstanceId());
			if (!Config.ALLOW_SUMMON_TO_INSTANCE || !summonerInstance.isSummonAllowed())
			{
				summonerChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_MAY_NOT_SUMMON_FROM_YOUR_CURRENT_LOCATION));
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
					summonerChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOUR_TARGET_IS_IN_AN_AREA_WHICH_BLOCKS_SUMMONING));
					return false;
				}
			}
			else
			{
				if (targetCabal == SevenSigns.CABAL_NULL)
				{
					summonerChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOUR_TARGET_IS_IN_AN_AREA_WHICH_BLOCKS_SUMMONING));
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
	
	public void checkItemRestriction()
	{
		for (int i = 0; i < Inventory.PAPERDOLL_TOTALSLOTS; i++)
		{
			L2ItemInstance equippedItem = getInventory().getPaperdollItem(i);
			if (equippedItem != null && (!equippedItem.getItem().checkCondition(this, this, false)
					|| (isInOlympiadMode() && equippedItem.isOlyRestrictedItem())))
			{
				getInventory().unEquipItemInSlot(i);
				
				InventoryUpdate iu = new InventoryUpdate();
				iu.addModifiedItem(equippedItem);
				sendPacket(iu);
				
				SystemMessage sm = null;
				if (equippedItem.getItem().getBodyPart() == L2Item.SLOT_BACK)
				{
					sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CLOAK_REMOVED_BECAUSE_ARMOR_SET_REMOVED));
					return;
				}
				
				if (equippedItem.getEnchantLevel() > 0)
				{
					sm = SystemMessage.getSystemMessage(SystemMessageId.EQUIPMENT_S1_S2_REMOVED);
					sm.addNumber(equippedItem.getEnchantLevel());
					sm.addItemName(equippedItem);
				}
				else
				{
					sm = SystemMessage.getSystemMessage(SystemMessageId.S1_DISARMED);
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
	private class FeedTask implements Runnable
	{
		@Override
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
					sendPacket(SystemMessage.getSystemMessage(SystemMessageId.OUT_OF_FEED_MOUNT_CANCELED));
				}
				
				int[] foodIds = getPetData(getMountNpcId()).getFood();
				if (foodIds.length == 0)
					return;
				L2ItemInstance food = null;
				for (int id : foodIds)
				{
					//TODO: possibly pet inv?
					food = getInventory().getItemByItemId(id);
					if (food != null)
						break;
				}
				
				if (food != null && isHungry())
				{
					IItemHandler handler = ItemHandler.getInstance().getItemHandler(food.getEtcItem());
					if (handler != null)
					{
						handler.useItem(L2PcInstance.this, food, false);
						SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.PET_TOOK_S1_BECAUSE_HE_WAS_HUNGRY);
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
			_controlItemId = getPet().getControlObjectId();
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
			//ThreadPoolManager.getInstance().removeGeneral((Runnable)_mountFeedTask);
			_mountFeedTask = null;
			if (Config.DEBUG) _log.fine("Pet [#"+_mountNpcId+"] feed task stop");
		}
	}
	
	private final void clearPetData()
	{
		_data = null;
	}
	
	private final L2PetData getPetData(int npcId)
	{
		if (_data == null)
			_data = PetDataTable.getInstance().getPetData(npcId);
		return _data;
	}
	
	private final L2PetLevelData getPetLevelData(int npcId)
	{
		if (_leveldata == null)
			_leveldata = PetDataTable.getInstance().getPetData(npcId).getPetLevelData(getMountLevel());
		return _leveldata;
	}
	
	public int getCurrentFeed() { return _curFeed; }
	
	private int getFeedConsume()
	{
		// if pet is attacking
		if (isAttackingNow())
			return getPetLevelData(_mountNpcId).getPetFeedBattle();
		return getPetLevelData(_mountNpcId).getPetFeedNormal();
	}
	
	public void setCurrentFeed(int num)
	{
		_curFeed = num > getMaxFeed() ? getMaxFeed() : num;
		SetupGauge sg = new SetupGauge(3, getCurrentFeed()*10000/getFeedConsume(), getMaxFeed()*10000/getFeedConsume());
		sendPacket(sg);
	}
	
	private int getMaxFeed()
	{
		return getPetLevelData(_mountNpcId).getPetMaxFeed();
	}
	
	private boolean isHungry()
	{
		return _canFeed ? (getCurrentFeed() < (getPetData(getMountNpcId()).getHungry_limit() / 100f * getPetLevelData(getMountNpcId()).getPetMaxFeed())):false;
	}
	
	private class Dismount implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				L2PcInstance.this.dismount();
			}
			catch (Exception e)
			{
				_log.log(Level.WARNING, "Exception on dismount(): " + e.getMessage(), e);
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
				L2DatabaseFactory.close(con);
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
	
	public void setisInHideoutSiege(boolean isInHideoutSiege)
	{
		_isInHideoutSiege = isInHideoutSiege;
	}
	
	public boolean isInHideoutSiege()
	{
		return _isInHideoutSiege;
	}
	
	public FloodProtectors getFloodProtectors()
	{
		return getClient().getFloodProtectors();
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
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.FORCE_MAXLEVEL_REACHED));
			return;
		}
		
		// if no charges - start clear task
		if (_charges.get() == 0)
			restartChargeTask();
		
		if (_charges.addAndGet(count) >= max)
		{
			_charges.set(max);
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.FORCE_MAXLEVEL_REACHED));
		}
		else
		{
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.FORCE_INCREASED_TO_S1);
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
		_chargeTask = ThreadPoolManager.getInstance().scheduleGeneral(new ChargeTask(), 600000);
	}
	
	/**
	 * Stops the Charges Clearing Task.
	 */
	public void stopChargeTask()
	{
		if (_chargeTask != null)
		{
			_chargeTask.cancel(false);
			//ThreadPoolManager.getInstance().removeGeneral((Runnable)_chargeTask);
			_chargeTask = null;
		}
	}
	
	private class ChargeTask implements Runnable
	{
		
		@Override
		public void run()
		{
			L2PcInstance.this.clearCharges();
		}
	}
	
	public static class TeleportBookmark
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
				catch (Exception e)
				{
					_log.log(Level.WARNING, "Could not update character teleport bookmark data: " + e.getMessage(), e);
				}
				finally
				{
					L2DatabaseFactory.close(con);
				}
				
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
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Could not delete character teleport bookmark data: " + e.getMessage(), e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
		
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
			sendPacket(SystemMessage.getSystemMessage(2359));
			return;
		}
		SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_DISAPPEARED);
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
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_DURING_A_BATTLE));
			return false;
		}
		else if (this.isInSiege() || this.getSiegeState() != 0)
		{
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_WHILE_PARTICIPATING));
			return false;
		}
		else if (this.isInDuel())
		{
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_DURING_A_DUEL));
			return false;
		}
		else if (this.isFlying())
		{
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_WHILE_FLYING));
			return false;
		}
		else if (this.isInOlympiadMode())
		{
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_WHILE_PARTICIPATING_IN_AN_OLYMPIAD_MATCH));
			return false;
		}
		else if (this.isParalyzed())
		{
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_WHILE_YOU_ARE_PARALYZED));
			return false;
		}
		else if (this.isDead())
		{
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_WHILE_YOU_ARE_DEAD));
			return false;
		}
		else if (type == 1 && (isIn7sDungeon() || (isInParty() && getParty().isInDimensionalRift())))
		{
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_TO_REACH_THIS_AREA));
			return false;
		}
		else if (this.isInBoat() || this.isInAirShip() || this.isInJail() || this.isInsideZone(ZONE_NOSUMMONFRIEND))
		{
			if(type == 0)
				sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_IN_THIS_AREA));
			else if (type == 1)
				sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_TO_REACH_THIS_AREA));
			return false;
		}
		else if (this.isInWater())
		{
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_UNDERWATER));
			return false;
		}
		else if (type == 1 && (this.isInsideZone(ZONE_SIEGE) || this.isInsideZone(ZONE_CLANHALL) || this.isInsideZone(ZONE_JAIL) || this.isInsideZone(ZONE_CASTLE) || this.isInsideZone(ZONE_NOSUMMONFRIEND) || this.isInsideZone(ZONE_FORT)))
		{
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_TO_REACH_THIS_AREA));
			return false;
		}
		else if (isInsideZone(ZONE_NOBOOKMARK))
		{
			if(type == 0)
				sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_IN_THIS_AREA));
			else if (type == 1)
				sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_TO_REACH_THIS_AREA));
			return false;
		}
		/* TODO: Instant Zone still not implement
    	else if (this.isInsideZone(ZONE_INSTANT))
    	{
    		sendPacket(SystemMessage.getSystemMessage(2357));
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
			sendPacket(SystemMessage.getSystemMessage(2358));
			return;
		}
		
		if(getInventory().getInventoryItemCount(20033, 0) == 0)
		{
			sendPacket(SystemMessage.getSystemMessage(6501));
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
		
		SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_DISAPPEARED);
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
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Could not insert character teleport bookmark data: " + e.getMessage(), e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
		
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
			L2DatabaseFactory.close(con);
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
			Integer oldrelation = getKnownList().getKnownRelations().get(activeChar.getObjectId());
			if (oldrelation != null && oldrelation != relation1)
			{
				activeChar.sendPacket(new RelationChanged(this, relation1, isAutoAttackable(activeChar)));
				if (getPet() != null)
					activeChar.sendPacket(new RelationChanged(getPet(), relation1, isAutoAttackable(activeChar)));
			}
			oldrelation = activeChar.getKnownList().getKnownRelations().get(getObjectId());
			if (oldrelation != null && oldrelation != relation2)
			{
				sendPacket(new RelationChanged(activeChar, relation2, activeChar.isAutoAttackable(this)));
				if (activeChar.getPet() != null)
					sendPacket(new RelationChanged(activeChar.getPet(), relation2, activeChar.isAutoAttackable(this)));
			}
			activeChar.sendPacket(new GetOnVehicle(getObjectId(), getBoat().getObjectId(), getInVehiclePosition()));
		}
		else if(isInAirShip())
		{
			getPosition().setWorldPosition(getAirShip().getPosition().getWorldPosition());
			
			activeChar.sendPacket(new CharInfo(this));
			activeChar.sendPacket(new ExBrExtraUserInfo(this));
			int relation1 = getRelation(activeChar);
			int relation2 = activeChar.getRelation(this);
			Integer oldrelation = getKnownList().getKnownRelations().get(activeChar.getObjectId());
			if (oldrelation != null && oldrelation != relation1)
			{
				activeChar.sendPacket(new RelationChanged(this, relation1, isAutoAttackable(activeChar)));
				if (getPet() != null)
					activeChar.sendPacket(new RelationChanged(getPet(), relation1, isAutoAttackable(activeChar)));
			}
			oldrelation = activeChar.getKnownList().getKnownRelations().get(getObjectId());
			if (oldrelation != null && oldrelation != relation2)
			{
				sendPacket(new RelationChanged(activeChar, relation2, activeChar.isAutoAttackable(this)));
				if (activeChar.getPet() != null)
					sendPacket(new RelationChanged(activeChar.getPet(), relation2, activeChar.isAutoAttackable(this)));
			}
			activeChar.sendPacket(new ExGetOnAirShip(this, getAirShip()));
		}
		else
		{
			activeChar.sendPacket(new CharInfo(this));
			activeChar.sendPacket(new ExBrExtraUserInfo(this));
			int relation1 = getRelation(activeChar);
			int relation2 = activeChar.getRelation(this);
			Integer oldrelation = getKnownList().getKnownRelations().get(activeChar.getObjectId());
			if (oldrelation != null && oldrelation != relation1)
			{
				activeChar.sendPacket(new RelationChanged(this, relation1, isAutoAttackable(activeChar)));
				if (getPet() != null)
					activeChar.sendPacket(new RelationChanged(getPet(), relation1, isAutoAttackable(activeChar)));
			}
			oldrelation = activeChar.getKnownList().getKnownRelations().get(getObjectId());
			if (oldrelation != null && oldrelation != relation2)
			{
				sendPacket(new RelationChanged(activeChar, relation2, activeChar.isAutoAttackable(this)));
				if (activeChar.getPet() != null)
					sendPacket(new RelationChanged(activeChar.getPet(), relation2, activeChar.isAutoAttackable(this)));
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
		if (isTransformed() || isInStance())
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
	 * Set the _createDate of the L2PcInstance.<BR><BR>
	 * @param createDate 
	 */
	public void setCreateDate(Calendar createDate)
	{
		_createDate = createDate;
	}
	
	/**
	 * @return the _createDate of the L2PcInstance.
	 */
	public Calendar getCreateDate()
	{
		return _createDate;
	}
	
	/**
	 * @return number of days to char birthday.<BR><BR>
	 */
	public int checkBirthDay()
	{
		Calendar now = Calendar.getInstance();
		
		// "Characters with a February 29 creation date will receive a gift on February 28."
		if (_createDate.get(Calendar.DAY_OF_MONTH) == 29 && _createDate.get(Calendar.MONTH) == 1)
			_createDate.add(Calendar.HOUR_OF_DAY, -24);
		
		if (now.get(Calendar.MONTH) == _createDate.get(Calendar.MONTH)
			&& now.get(Calendar.DAY_OF_MONTH) == _createDate.get(Calendar.DAY_OF_MONTH)
			&& now.get(Calendar.YEAR) != _createDate.get(Calendar.YEAR))
		{
			return 0;
		}
		
		int i;
		for (i = 1; i < 6; i++)
		{
			now.add(Calendar.HOUR_OF_DAY, 24);
			if (now.get(Calendar.MONTH) == _createDate.get(Calendar.MONTH)
				&& now.get(Calendar.DAY_OF_MONTH) == _createDate.get(Calendar.DAY_OF_MONTH)
				&& now.get(Calendar.YEAR) != _createDate.get(Calendar.YEAR))
				return i;
		}
		return -1;
	}
	
	
	/**
	 * list of character friends
	 * 
	 */
	private final List<Integer> _friendList = new FastList<Integer>();
	
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
			_log.log(Level.WARNING, "Error found in " + getName() + "'s FriendList: " + e.getMessage(), e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
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
			L2PcInstance friend = L2World.getInstance().getPlayer(id);
			if (friend != null)
				friend.sendPacket(pkt);
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
	 * While at silenceMode, checks if this PC Instance blocks PMs for this user
	 */
	public boolean isSilenceMode(int objId)
	{
		if (Config.SILENCE_MODE_EXCLUDE && _silenceMode)
			return !_silenceModeExcluded.contains(objId);
		
		return _silenceMode;
	}
	
	/**
	 * @param mode the _silenceMode to set
	 */
	public void setSilenceMode(boolean mode)
	{
		_silenceMode = mode;
		_silenceModeExcluded.clear(); // Clear the excluded list on each setSilenceMode
		sendPacket(new EtcStatusUpdate(this));
	}
	
	public void addSilenceModeExcluded(int playerObjId)
	{
		_silenceModeExcluded.add(playerObjId);
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
			L2DatabaseFactory.close(con);
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
			L2DatabaseFactory.close(con);
		}
	}
	
	public double getCollisionRadius()
	{
		if (getAppearance().getSex())
			return getBaseTemplate().fCollisionRadius_female;
		return getBaseTemplate().fCollisionRadius;
	}
	
	public double getCollisionHeight()
	{
		if (getAppearance().getSex())
			return getBaseTemplate().fCollisionHeight_female;
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
	 * @param z 
	 * @return true if character falling now on the start of fall return false for correct coord sync!
	 */
	public final boolean isFalling(int z)
	{
		if (isDead()
				|| isFlying()
				|| isFlyingMounted()
				|| isInsideZone(ZONE_WATER))
			return false;
		
		if (System.currentTimeMillis() < _fallingTimestamp)
			return true;
		
		final int deltaZ = getZ() - z;
		if (deltaZ <= getBaseTemplate().getFallHeight())
			return false;
		
		final int damage = (int)Formulas.calcFallDam(this, deltaZ);
		if (damage > 0)
		{
			reduceCurrentHp(Math.min(damage, getCurrentHp() - 1), null, false, true, null);
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.FALL_DAMAGE_S1).addNumber(damage));
		}
		
		setFalling();
		
		return false;
	}
	
	/**
	 * Set falling timestamp
	 */
	public final void setFalling()
	{
		_fallingTimestamp = System.currentTimeMillis() + FALLING_VALIDATION_DELAY;
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
	
	/**
	 * Update last item auction request timestamp to current
	 */
	public void updateLastItemAuctionRequest()
	{
		_lastItemAuctionInfoRequest = System.currentTimeMillis();
	}
	
	/**
	 * @return true if receiving item auction requests<br>
	 * (last request was in 2 seconds before)
	 */
	public boolean isItemAuctionPolling()
	{
		return System.currentTimeMillis() - _lastItemAuctionInfoRequest < 2000;
	}
	
	/**
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
	
	public long getOfflineStartTime()
	{
		return _offlineShopStart;
	}
	
	public void setOfflineStartTime(long time)
	{
		_offlineShopStart = time;
	}
	
	/**
	 * Remove player from BossZones (used on char logout/exit)
	 */
	public void removeFromBossZone()
	{
		try
		{
			for (L2BossZone _zone : GrandBossManager.getInstance().getZones())
			{
				_zone.removePlayer(this);
			}
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Exception on removeFromBossZone(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * Check all player skills for skill level. If player level is lower than skill learn level - 9, skill level is decreased to next possible level.
	 */
	public void checkPlayerSkills()
	{
		for (int id : _skills.keys())
		{
			int level = getSkillLevel(id);
			if (level >= 100) // enchanted skill
				level = SkillTable.getInstance().getMaxLevel(id);
			final L2SkillLearn learn = SkillTreesData.getInstance().getClassSkill(id, level, getClassId());
			// not found - not a learn skill?
			if (learn == null)
			{
				continue;
			}
			// player level is too low for such skill level
			if (getLevel() < (learn.getGetLevel() - 9))
				deacreaseSkillLevel(id);
		}
	}
	
	private void deacreaseSkillLevel(int id)
	{
		int nextLevel = -1;
		final FastMap<Integer, L2SkillLearn> skillTree = SkillTreesData.getInstance().getCompleteClassSkillTree(getClassId());
		
		for (L2SkillLearn sl : skillTree.values())
		{
			if (sl.getSkillId() == id && nextLevel < sl.getSkillLevel() && getLevel() >= (sl.getGetLevel() - 9))
			{
				// next possible skill level
				nextLevel = sl.getSkillLevel();
			}
		}
		
		if (nextLevel == -1) // there is no lower skill
		{
			_log.info("Removing skill id "+id+ " level "+getSkillLevel(id)+" from player "+this);
			removeSkill(_skills.get(id), true);
		}
		else // replace with lower one
		{
			_log.info("Decreasing skill id "+id+" from "+getSkillLevel(id)+" to "+nextLevel+" for "+this);
			addSkill(SkillTable.getInstance().getInfo(id, nextLevel), true);
		}
	}
	
	public boolean canMakeSocialAction()
	{
		if (getPrivateStoreType() == 0 && getActiveRequester() == null
				&& !isAlikeDead() && (!isAllSkillsDisabled() || isInDuel())
				&& !isCastingNow() && !isCastingSimultaneouslyNow()
				&& getAI().getIntention() == CtrlIntention.AI_INTENTION_IDLE
				&& !AttackStanceTaskManager.getInstance().getAttackStanceTask(this)
				&& !isInOlympiadMode())
		{
			return true;
		}
		return false;
	}
	
	public void setMultiSocialAction(int id, int targetId)
	{
		_multiSociaAction = id;
		_multiSocialTarget = targetId;
	}
	
	public int getMultiSociaAction()
	{
		return _multiSociaAction;
	}
	
	public int getMultiSocialTarget()
	{
		return _multiSocialTarget;
	}
	
	public List<TeleportBookmark> getTpbookmark()
	{
		return tpbookmark;
	}
	
	public int getBookmarkslot()
	{
		return _bookmarkslot;
	}
	
	/**
	 * @return
	 */
	public int getQuestInventoryLimit()
	{
		return Config.INVENTORY_MAXIMUM_QUEST_ITEMS;
	}
	
	public boolean canAttackCharacter(L2Character cha)
	{
		if(cha instanceof L2Attackable)
		{
			return true;
		}
		else if(cha instanceof L2Playable)
		{
			if(cha.isInsideZone(L2Character.ZONE_PVP) && !cha.isInsideZone(L2Character.ZONE_SIEGE))
				return true;
			
			L2PcInstance target;
			if (cha instanceof L2Summon)
				target = ((L2Summon) cha).getOwner();
			else
				target = (L2PcInstance) cha;
			
			if (isInDuel() && target.isInDuel() && (target.getDuelId() == getDuelId()))
			{
				return true;
			}
			else if (isInParty() && target.isInParty())
			{
				if(getParty() == target.getParty())
					return false;
				if((getParty().getCommandChannel() != null || target.getParty().getCommandChannel() != null) && (getParty().getCommandChannel() == target.getParty().getCommandChannel()))
					return false;
			}
			else if (getClan() != null && target.getClan() != null)
			{
				if (getClanId() == target.getClanId())
					return false;
				if ((getAllyId() > 0 || target.getAllyId() > 0) && (getAllyId() == target.getAllyId()))
					return false;
				if(getClan().isAtWarWith(target.getClan().getClanId()) && target.getClan().isAtWarWith(getClan().getClanId()))
					return true;
			}
			else if (getClan() == null || target.getClan() == null)
			{
				if(target.getPvpFlag() == 0 && target.getKarma() == 0)
					return false;
			}
		}
		return true;
	}
	
	/**
	 * Test if player inventory is under 80% capaity
	 * @param includeQuestInv check also quest inventory
	 * @return
	 */
	public boolean isInventoryUnder80(boolean includeQuestInv)
	{
		if (getInventory().getSize(false) <= (getInventoryLimit() * 0.8))
		{
			if (includeQuestInv)
			{
				if (getInventory().getSize(true) <= (getQuestInventoryLimit() * 0.8))
					return true;
			}
			else
				return true;
		}
		return false;
	}
	
	public boolean havePetInvItems()
	{
		return _petItems;
	}
	
	public void setPetInvItems(boolean haveit)
	{
		_petItems = haveit;
	}
	
	private void checkPetInvItems()
	{
		Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT object_id FROM `items` WHERE `owner_id`=? AND (`loc`='PET' OR `loc`='PET_EQUIP') LIMIT 1;");
			statement.setInt(1, getObjectId());
			ResultSet rset = statement.executeQuery();
			if (rset.next() && rset.getInt("object_id") > 0)
				setPetInvItems(true);
			else
				setPetInvItems(false);			 
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Could not check Items in Pet Inventory for playerId: "+getObjectId(), e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
	}

	public String getAdminConfirmCmd()
	{
		return _adminConfirmCmd;
	}

	public void setAdminConfirmCmd(String adminConfirmCmd)
	{
		_adminConfirmCmd = adminConfirmCmd;
	}
	
	public void setBlockCheckerArena(byte arena)
	{
		_handysBlockCheckerEventArena = arena;
	}

	public int getBlockCheckerArena()
	{
		return _handysBlockCheckerEventArena;
	}

	/**
	 * Load L2PcInstance Recommendations data.<BR><BR>
	 * @return 
	 */
	private long loadRecommendations()
	{
		long _time_left = 0;
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT rec_have,rec_left,time_left FROM character_reco_bonus WHERE charId=? LIMIT 1");
			statement.setInt(1, getObjectId());
			ResultSet rset = statement.executeQuery();
			
			if(rset.next())
			{
				setRecomHave(rset.getInt("rec_have"));
				setRecomLeft(rset.getInt("rec_left"));				
				_time_left = rset.getLong("time_left");
			}
			else
			{
				_time_left = 3600000;
			}
			
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Could not restore Recommendations for player: "+getObjectId(), e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
		return _time_left;
	}

	/**
	 * Update L2PcInstance Recommendations data.<BR><BR>
	 */
	public void storeRecommendations()
	{
		long _recoTaskEnd = 0;
		if(_recoBonusTask != null)
			_recoTaskEnd = Math.max(0, _recoBonusTask.getDelay(TimeUnit.MILLISECONDS));
		
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("INSERT INTO character_reco_bonus (charId,rec_have,rec_left,time_left) VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE rec_have=?, rec_left=?, time_left=?");
			statement.setInt(1, getObjectId());
			statement.setInt(2, getRecomHave());
			statement.setInt(3, getRecomLeft());
			statement.setLong(4, _recoTaskEnd);
			// Update part
			statement.setInt(5, getRecomHave());
			statement.setInt(6, getRecomLeft());
			statement.setLong(7, _recoTaskEnd);
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Could not update Recommendations for player: "+getObjectId(), e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
	}

	public void checkRecoBonusTask()
	{
		// Load data
		long _task_time = loadRecommendations();
		
		if(_task_time > 0)
		{
			// Add 20 recos on first login
			if(_task_time == 3600000)
				setRecomLeft(getRecomLeft()+20);
			// If player have some timeleft, start bonus task
			_recoBonusTask = ThreadPoolManager.getInstance().scheduleGeneral(new RecoBonusTaskEnd(), _task_time);
		}
		// Create task to give new recommendations
		_recoGiveTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new RecoGiveTask(), 7200000, 3600000);
		// Store new data
		storeRecommendations();
	}
	
	public void stopRecoBonusTask()
	{
		if (_recoBonusTask != null)
		{
			_recoBonusTask.cancel(false);
			_recoBonusTask = null;
		}
	}
	
	public void stopRecoGiveTask()
	{
		if (_recoGiveTask != null)
		{
			_recoGiveTask.cancel(false);
			_recoGiveTask = null;
		}
	}
	
	private class RecoGiveTask implements Runnable
	{
		@Override
		public void run()
		{
			if (L2PcInstance.this == null)
			{
				//stopRecoGiveTask(); why is this here? it will lead to NPE
				return;
			}
			int reco_to_give;
			// 10 recommendations to give out after 2 hours of being logged in
			// 1 more recommendation to give out every hour after that.
			if(_recoTwoHoursGiven)
				reco_to_give = 1;
			else
				reco_to_give = 10;
			
			_recoTwoHoursGiven = true;
			
			setRecomLeft(getRecomLeft()+reco_to_give);
			
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_OBTAINED_S1_RECOMMENDATIONS);
			sm.addNumber(reco_to_give);
			L2PcInstance.this.sendPacket(sm);
			L2PcInstance.this.sendPacket(new UserInfo(L2PcInstance.this));
		}
	}
	
	private class RecoBonusTaskEnd implements Runnable
	{
		@Override
		public void run()
		{
			if (L2PcInstance.this == null)
				return;
			
			L2PcInstance.this.sendPacket(new ExVoteSystemInfo(L2PcInstance.this));
		}
	}
	
	public int getRecomBonusTime()
	{
		if(_recoBonusTask != null)
			return (int) Math.max(0, _recoBonusTask.getDelay(TimeUnit.SECONDS));
		
		return 0;
	}
		
	public int getRecomBonusType()
	{
		// Maintain = 1
		return 0;
	}
	
	public void setLastPetitionGmName(String gmName)
	{
		_lastPetitionGmName = gmName;
	}
	
	public String getLastPetitionGmName()
	{
		return _lastPetitionGmName;
	}
	
	public L2ContactList getContactList()
	{
		return _contactList;
	}
	
	public void setEventStatus()
	{
		eventStatus = new PlayerEventStatus(this);
	}
	
	public void setEventStatus(PlayerEventStatus pes)
	{
		eventStatus = pes;
	}
	
	public PlayerEventStatus getEventStatus()
	{
		return eventStatus;
	}
	
	public long getNotMoveUntil()
	{
		return _notMoveUntil;
	}
	
	public void updateNotMoveUntil()
	{
		_notMoveUntil = System.currentTimeMillis() + Config.PLAYER_MOVEMENT_BLOCK_TIME;
	}
}
