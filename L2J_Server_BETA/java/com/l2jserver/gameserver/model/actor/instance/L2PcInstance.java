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
package com.l2jserver.gameserver.model.actor.instance;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
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
import com.l2jserver.gameserver.datatables.AdminTable;
import com.l2jserver.gameserver.datatables.CategoryData;
import com.l2jserver.gameserver.datatables.CharNameTable;
import com.l2jserver.gameserver.datatables.CharSummonTable;
import com.l2jserver.gameserver.datatables.CharTemplateTable;
import com.l2jserver.gameserver.datatables.ClanTable;
import com.l2jserver.gameserver.datatables.ClassListData;
import com.l2jserver.gameserver.datatables.EnchantSkillGroupsData;
import com.l2jserver.gameserver.datatables.ExperienceTable;
import com.l2jserver.gameserver.datatables.FishData;
import com.l2jserver.gameserver.datatables.HennaData;
import com.l2jserver.gameserver.datatables.ItemTable;
import com.l2jserver.gameserver.datatables.NpcTable;
import com.l2jserver.gameserver.datatables.PetDataTable;
import com.l2jserver.gameserver.datatables.RecipeData;
import com.l2jserver.gameserver.datatables.SkillTable;
import com.l2jserver.gameserver.datatables.SkillTable.FrequentSkill;
import com.l2jserver.gameserver.datatables.SkillTreesData;
import com.l2jserver.gameserver.handler.IItemHandler;
import com.l2jserver.gameserver.handler.ItemHandler;
import com.l2jserver.gameserver.idfactory.IdFactory;
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
import com.l2jserver.gameserver.instancemanager.PunishmentManager;
import com.l2jserver.gameserver.instancemanager.QuestManager;
import com.l2jserver.gameserver.instancemanager.SiegeManager;
import com.l2jserver.gameserver.instancemanager.TerritoryWarManager;
import com.l2jserver.gameserver.instancemanager.ZoneManager;
import com.l2jserver.gameserver.model.ArenaParticipantsHolder;
import com.l2jserver.gameserver.model.BlockList;
import com.l2jserver.gameserver.model.CategoryType;
import com.l2jserver.gameserver.model.L2AccessLevel;
import com.l2jserver.gameserver.model.L2Clan;
import com.l2jserver.gameserver.model.L2ClanMember;
import com.l2jserver.gameserver.model.L2ContactList;
import com.l2jserver.gameserver.model.L2EnchantSkillLearn;
import com.l2jserver.gameserver.model.L2ManufactureItem;
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
import com.l2jserver.gameserver.model.L2SkillLearn;
import com.l2jserver.gameserver.model.L2World;
import com.l2jserver.gameserver.model.L2WorldRegion;
import com.l2jserver.gameserver.model.Location;
import com.l2jserver.gameserver.model.Macro;
import com.l2jserver.gameserver.model.MacroList;
import com.l2jserver.gameserver.model.MountType;
import com.l2jserver.gameserver.model.PartyMatchRoom;
import com.l2jserver.gameserver.model.PartyMatchRoomList;
import com.l2jserver.gameserver.model.PartyMatchWaitingList;
import com.l2jserver.gameserver.model.PcCondOverride;
import com.l2jserver.gameserver.model.ShortCuts;
import com.l2jserver.gameserver.model.ShotType;
import com.l2jserver.gameserver.model.TeleportBookmark;
import com.l2jserver.gameserver.model.TeleportWhereType;
import com.l2jserver.gameserver.model.TerritoryWard;
import com.l2jserver.gameserver.model.TimeStamp;
import com.l2jserver.gameserver.model.TradeList;
import com.l2jserver.gameserver.model.UIKeysSettings;
import com.l2jserver.gameserver.model.actor.L2Attackable;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.L2Decoy;
import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.model.actor.L2Playable;
import com.l2jserver.gameserver.model.actor.L2Summon;
import com.l2jserver.gameserver.model.actor.L2Vehicle;
import com.l2jserver.gameserver.model.actor.appearance.PcAppearance;
import com.l2jserver.gameserver.model.actor.events.PlayerEvents;
import com.l2jserver.gameserver.model.actor.knownlist.PcKnownList;
import com.l2jserver.gameserver.model.actor.position.PcPosition;
import com.l2jserver.gameserver.model.actor.stat.PcStat;
import com.l2jserver.gameserver.model.actor.status.PcStatus;
import com.l2jserver.gameserver.model.actor.tasks.player.DismountTask;
import com.l2jserver.gameserver.model.actor.tasks.player.FameTask;
import com.l2jserver.gameserver.model.actor.tasks.player.GameGuardCheckTask;
import com.l2jserver.gameserver.model.actor.tasks.player.HerbTask;
import com.l2jserver.gameserver.model.actor.tasks.player.InventoryEnableTask;
import com.l2jserver.gameserver.model.actor.tasks.player.LookingForFishTask;
import com.l2jserver.gameserver.model.actor.tasks.player.PetFeedTask;
import com.l2jserver.gameserver.model.actor.tasks.player.PvPFlagTask;
import com.l2jserver.gameserver.model.actor.tasks.player.RecoBonusTaskEnd;
import com.l2jserver.gameserver.model.actor.tasks.player.RecoGiveTask;
import com.l2jserver.gameserver.model.actor.tasks.player.RentPetTask;
import com.l2jserver.gameserver.model.actor.tasks.player.ResetChargesTask;
import com.l2jserver.gameserver.model.actor.tasks.player.ResetSoulsTask;
import com.l2jserver.gameserver.model.actor.tasks.player.ShortBuffTask;
import com.l2jserver.gameserver.model.actor.tasks.player.SitDownTask;
import com.l2jserver.gameserver.model.actor.tasks.player.StandUpTask;
import com.l2jserver.gameserver.model.actor.tasks.player.TeleportWatchdogTask;
import com.l2jserver.gameserver.model.actor.tasks.player.VitalityTask;
import com.l2jserver.gameserver.model.actor.tasks.player.WarnUserTakeBreakTask;
import com.l2jserver.gameserver.model.actor.tasks.player.WaterTask;
import com.l2jserver.gameserver.model.actor.templates.L2PcTemplate;
import com.l2jserver.gameserver.model.actor.transform.Transform;
import com.l2jserver.gameserver.model.base.ClassId;
import com.l2jserver.gameserver.model.base.ClassLevel;
import com.l2jserver.gameserver.model.base.PlayerClass;
import com.l2jserver.gameserver.model.base.Race;
import com.l2jserver.gameserver.model.base.Sex;
import com.l2jserver.gameserver.model.base.SubClass;
import com.l2jserver.gameserver.model.effects.AbnormalEffect;
import com.l2jserver.gameserver.model.effects.EffectFlag;
import com.l2jserver.gameserver.model.effects.EffectTemplate;
import com.l2jserver.gameserver.model.effects.L2Effect;
import com.l2jserver.gameserver.model.effects.L2EffectType;
import com.l2jserver.gameserver.model.entity.Castle;
import com.l2jserver.gameserver.model.entity.Duel;
import com.l2jserver.gameserver.model.entity.Fort;
import com.l2jserver.gameserver.model.entity.Hero;
import com.l2jserver.gameserver.model.entity.Instance;
import com.l2jserver.gameserver.model.entity.L2Event;
import com.l2jserver.gameserver.model.entity.Siege;
import com.l2jserver.gameserver.model.entity.TvTEvent;
import com.l2jserver.gameserver.model.fishing.L2Fish;
import com.l2jserver.gameserver.model.fishing.L2Fishing;
import com.l2jserver.gameserver.model.holders.ItemHolder;
import com.l2jserver.gameserver.model.holders.SkillUseHolder;
import com.l2jserver.gameserver.model.itemcontainer.Inventory;
import com.l2jserver.gameserver.model.itemcontainer.ItemContainer;
import com.l2jserver.gameserver.model.itemcontainer.PcFreight;
import com.l2jserver.gameserver.model.itemcontainer.PcInventory;
import com.l2jserver.gameserver.model.itemcontainer.PcRefund;
import com.l2jserver.gameserver.model.itemcontainer.PcWarehouse;
import com.l2jserver.gameserver.model.itemcontainer.PetInventory;
import com.l2jserver.gameserver.model.items.L2Armor;
import com.l2jserver.gameserver.model.items.L2EtcItem;
import com.l2jserver.gameserver.model.items.L2Henna;
import com.l2jserver.gameserver.model.items.L2Item;
import com.l2jserver.gameserver.model.items.L2Weapon;
import com.l2jserver.gameserver.model.items.instance.L2ItemInstance;
import com.l2jserver.gameserver.model.items.type.L2ActionType;
import com.l2jserver.gameserver.model.items.type.L2ArmorType;
import com.l2jserver.gameserver.model.items.type.L2EtcItemType;
import com.l2jserver.gameserver.model.items.type.L2WeaponType;
import com.l2jserver.gameserver.model.multisell.PreparedListContainer;
import com.l2jserver.gameserver.model.olympiad.OlympiadGameManager;
import com.l2jserver.gameserver.model.olympiad.OlympiadGameTask;
import com.l2jserver.gameserver.model.olympiad.OlympiadManager;
import com.l2jserver.gameserver.model.punishment.PunishmentAffect;
import com.l2jserver.gameserver.model.punishment.PunishmentType;
import com.l2jserver.gameserver.model.quest.Quest;
import com.l2jserver.gameserver.model.quest.Quest.QuestEventType;
import com.l2jserver.gameserver.model.quest.QuestState;
import com.l2jserver.gameserver.model.quest.State;
import com.l2jserver.gameserver.model.skills.L2Skill;
import com.l2jserver.gameserver.model.skills.L2SkillType;
import com.l2jserver.gameserver.model.skills.l2skills.L2SkillSiegeFlag;
import com.l2jserver.gameserver.model.skills.targets.L2TargetType;
import com.l2jserver.gameserver.model.stats.Env;
import com.l2jserver.gameserver.model.stats.Formulas;
import com.l2jserver.gameserver.model.stats.Stats;
import com.l2jserver.gameserver.model.variables.AccountVariables;
import com.l2jserver.gameserver.model.variables.PlayerVariables;
import com.l2jserver.gameserver.model.zone.L2ZoneType;
import com.l2jserver.gameserver.model.zone.ZoneId;
import com.l2jserver.gameserver.model.zone.type.L2BossZone;
import com.l2jserver.gameserver.model.zone.type.L2NoRestartZone;
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
import com.l2jserver.gameserver.network.serverpackets.ExDominionWarStart;
import com.l2jserver.gameserver.network.serverpackets.ExDuelUpdateUserInfo;
import com.l2jserver.gameserver.network.serverpackets.ExFishingEnd;
import com.l2jserver.gameserver.network.serverpackets.ExFishingStart;
import com.l2jserver.gameserver.network.serverpackets.ExGetBookMarkInfoPacket;
import com.l2jserver.gameserver.network.serverpackets.ExGetOnAirShip;
import com.l2jserver.gameserver.network.serverpackets.ExOlympiadMode;
import com.l2jserver.gameserver.network.serverpackets.ExPrivateStoreSetWholeMsg;
import com.l2jserver.gameserver.network.serverpackets.ExSetCompassZoneCode;
import com.l2jserver.gameserver.network.serverpackets.ExStartScenePlayer;
import com.l2jserver.gameserver.network.serverpackets.ExStorageMaxCount;
import com.l2jserver.gameserver.network.serverpackets.ExUseSharedGroupItem;
import com.l2jserver.gameserver.network.serverpackets.FriendStatusPacket;
import com.l2jserver.gameserver.network.serverpackets.GameGuardQuery;
import com.l2jserver.gameserver.network.serverpackets.GetOnVehicle;
import com.l2jserver.gameserver.network.serverpackets.HennaInfo;
import com.l2jserver.gameserver.network.serverpackets.InventoryUpdate;
import com.l2jserver.gameserver.network.serverpackets.ItemList;
import com.l2jserver.gameserver.network.serverpackets.L2GameServerPacket;
import com.l2jserver.gameserver.network.serverpackets.LeaveWorld;
import com.l2jserver.gameserver.network.serverpackets.MagicSkillUse;
import com.l2jserver.gameserver.network.serverpackets.MyTargetSelected;
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
import com.l2jserver.gameserver.network.serverpackets.ValidateLocation;
import com.l2jserver.gameserver.scripting.scriptengine.events.EquipmentEvent;
import com.l2jserver.gameserver.scripting.scriptengine.events.HennaEvent;
import com.l2jserver.gameserver.scripting.scriptengine.events.ProfessionChangeEvent;
import com.l2jserver.gameserver.scripting.scriptengine.events.TransformEvent;
import com.l2jserver.gameserver.scripting.scriptengine.listeners.player.EquipmentListener;
import com.l2jserver.gameserver.scripting.scriptengine.listeners.player.EventListener;
import com.l2jserver.gameserver.scripting.scriptengine.listeners.player.HennaListener;
import com.l2jserver.gameserver.scripting.scriptengine.listeners.player.ProfessionChangeListener;
import com.l2jserver.gameserver.scripting.scriptengine.listeners.player.TransformListener;
import com.l2jserver.gameserver.taskmanager.AttackStanceTaskManager;
import com.l2jserver.gameserver.util.Broadcast;
import com.l2jserver.gameserver.util.FloodProtectors;
import com.l2jserver.gameserver.util.PlayerEventStatus;
import com.l2jserver.gameserver.util.Point3D;
import com.l2jserver.gameserver.util.Util;
import com.l2jserver.util.L2FastList;
import com.l2jserver.util.Rnd;

/**
 * This class represents all player characters in the world.<br>
 * There is always a client-thread connected to this (except if a player-store is activated upon logout).
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
	
	// Character Item Reuse Time String Definition:
	private static final String ADD_ITEM_REUSE_SAVE = "INSERT INTO character_item_reuse_save (charId,itemId,itemObjId,reuseDelay,systime) VALUES (?,?,?,?,?)";
	private static final String RESTORE_ITEM_REUSE_SAVE = "SELECT charId,itemId,itemObjId,reuseDelay,systime FROM character_item_reuse_save WHERE charId=?";
	private static final String DELETE_ITEM_REUSE_SAVE = "DELETE FROM character_item_reuse_save WHERE charId=?";
	
	// Character Character SQL String Definitions:
	private static final String INSERT_CHARACTER = "INSERT INTO characters (account_name,charId,char_name,level,maxHp,curHp,maxCp,curCp,maxMp,curMp,face,hairStyle,hairColor,sex,exp,sp,karma,fame,pvpkills,pkkills,clanid,race,classid,deletetime,cancraft,title,title_color,accesslevel,online,isin7sdungeon,clan_privs,wantspeace,base_class,newbie,nobless,power_grade,createDate) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
	private static final String UPDATE_CHARACTER = "UPDATE characters SET level=?,maxHp=?,curHp=?,maxCp=?,curCp=?,maxMp=?,curMp=?,face=?,hairStyle=?,hairColor=?,sex=?,heading=?,x=?,y=?,z=?,exp=?,expBeforeDeath=?,sp=?,karma=?,fame=?,pvpkills=?,pkkills=?,clanid=?,race=?,classid=?,deletetime=?,title=?,title_color=?,accesslevel=?,online=?,isin7sdungeon=?,clan_privs=?,wantspeace=?,base_class=?,onlinetime=?,newbie=?,nobless=?,power_grade=?,subpledge=?,lvl_joined_academy=?,apprentice=?,sponsor=?,clan_join_expiry_time=?,clan_create_expiry_time=?,char_name=?,death_penalty_level=?,bookmarkslot=?,vitality_points=?,language=? WHERE charId=?";
	private static final String RESTORE_CHARACTER = "SELECT * FROM characters WHERE charId=?";
	
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
	
	// Character zone restart time SQL String Definitions - L2Master mod
	private static final String DELETE_ZONE_RESTART_LIMIT = "DELETE FROM character_norestart_zone_time WHERE charId = ?";
	private static final String LOAD_ZONE_RESTART_LIMIT = "SELECT time_limit FROM character_norestart_zone_time WHERE charId = ?";
	private static final String UPDATE_ZONE_RESTART_LIMIT = "REPLACE INTO character_norestart_zone_time (charId, time_limit) VALUES (?,?)";
	
	private static final String COND_OVERRIDE_KEY = "cond_override";
	
	public static final String NEWBIE_KEY = "NEWBIE";
	
	public static final int ID_NONE = -1;
	
	public static final int REQUEST_TIMEOUT = 15;
	public static final int STORE_PRIVATE_NONE = 0;
	public static final int STORE_PRIVATE_SELL = 1;
	public static final int STORE_PRIVATE_BUY = 3;
	public static final int STORE_PRIVATE_MANUFACTURE = 5;
	public static final int STORE_PRIVATE_PACKAGE_SELL = 8;
	
	private static final List<HennaListener> HENNA_LISTENERS = new FastList<HennaListener>().shared();
	private static final List<EquipmentListener> GLOBAL_EQUIPMENT_LISTENERS = new FastList<EquipmentListener>().shared();
	private static final List<ProfessionChangeListener> GLOBAL_PROFESSION_CHANGE_LISTENERS = new FastList<ProfessionChangeListener>().shared();
	
	private final List<EquipmentListener> _equipmentListeners = new FastList<EquipmentListener>().shared();
	private final List<TransformListener> _transformListeners = new FastList<TransformListener>().shared();
	private final List<ProfessionChangeListener> _professionChangeListeners = new FastList<ProfessionChangeListener>().shared();
	private final List<EventListener> _eventListeners = new FastList<EventListener>().shared();
	
	public class AIAccessor extends L2Character.AIAccessor
	{
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
	
	private final String _accountName;
	private long _deleteTimer;
	private Calendar _createDate = Calendar.getInstance();
	
	private String _lang = null;
	private String _htmlPrefix = null;
	
	private volatile boolean _isOnline = false;
	private long _onlineTime;
	private long _onlineBeginTime;
	private long _lastAccess;
	private long _uptime;
	private long _zoneRestartLimitTime = 0;
	
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
	
	private final PcAppearance _appearance;
	
	/** The Identifier of the L2PcInstance */
	@Deprecated
	private final int _charId = 0x00030b7a;
	
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
	
	private volatile ScheduledFuture<?> _teleportWatchdog;
	
	/** The Siege state of the L2PcInstance */
	private byte _siegeState = 0;
	
	/** The id of castle/fort which the L2PcInstance is registered for siege */
	private int _siegeSide = 0;
	
	private int _curWeightPenalty = 0;
	
	private int _lastCompassZone; // the last compass zone update send to the client
	
	private boolean _isIn7sDungeon = false;
	
	private final L2ContactList _contactList = new L2ContactList(this);
	
	private int _bookmarkslot = 0; // The Teleport Bookmark Slot
	
	private final Map<Integer, TeleportBookmark> _tpbookmarks = new FastMap<>();
	
	private boolean _canFeed;
	private int _eventEffectId = 0;
	private boolean _isInSiege;
	private boolean _isInHideoutSiege = false;
	
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
	private MountType _mountType = MountType.NONE;
	private int _mountNpcId;
	private int _mountLevel;
	/** Store object used to summon the strider you are mounting **/
	private int _mountObjectID = 0;
	
	public int _telemode = 0;
	
	private boolean _inCrystallize;
	private boolean _inCraftMode;
	
	private long _offlineShopStart = 0;
	
	private Transform _transformation;
	
	/** The table containing all L2RecipeList of the L2PcInstance */
	private final Map<Integer, L2RecipeList> _dwarvenRecipeBook = new FastMap<>();
	private final Map<Integer, L2RecipeList> _commonRecipeBook = new FastMap<>();
	
	/** Premium Items */
	private final Map<Integer, L2PremiumItem> _premiumItems = new FastMap<>();
	
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
	protected boolean _recoTwoHoursGiven = false;
	
	private final PcInventory _inventory = new PcInventory(this);
	private final PcFreight _freight = new PcFreight(this);
	private PcWarehouse _warehouse;
	private PcRefund _refund;
	
	/** The Private Store type of the L2PcInstance (STORE_PRIVATE_NONE=0, STORE_PRIVATE_SELL=1, sellmanage=2, STORE_PRIVATE_BUY=3, buymanage=4, STORE_PRIVATE_MANUFACTURE=5) */
	private int _privatestore;
	
	private TradeList _activeTradeList;
	private ItemContainer _activeWarehouse;
	private volatile Map<Integer, L2ManufactureItem> _manufactureItems;
	private String _storeName = "";
	private TradeList _sellList;
	private TradeList _buyList;
	
	// Multisell
	private PreparedListContainer _currentMultiSell = null;
	
	/** Bitmask used to keep track of one-time/newbie quest rewards */
	private int _newbie;
	
	private boolean _noble = false;
	private boolean _hero = false;
	
	/** The L2FolkInstance corresponding to the last Folk which one the player talked. */
	private L2Npc _lastFolkNpc = null;
	
	/** Last NPC Id talked on a quest */
	private int _questNpcObject = 0;
	
	/** The table containing all Quests began by the L2PcInstance */
	private final Map<String, QuestState> _quests = new FastMap<>();
	
	/** The list containing all shortCuts of this L2PcInstance */
	private final ShortCuts _shortCuts = new ShortCuts(this);
	
	/**
	 * The list containing all macros of this L2PcInstance.
	 */
	private final MacroList _macros = new MacroList(this);
	
	private final List<L2PcInstance> _snoopListener = new FastList<>();
	private final List<L2PcInstance> _snoopedPlayer = new FastList<>();
	
	// hennas
	private final L2Henna[] _henna = new L2Henna[3];
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
	private L2TrapInstance _trap = null;
	/** The L2Agathion of the L2PcInstance */
	private int _agathionId = 0;
	// apparently, a L2PcInstance CAN have both a summon AND a tamed beast at the same time!!
	// after Freya players can control more than one tamed beast
	private List<L2TamedBeastInstance> _tamedBeast = null;
	
	private boolean _minimapAllowed = false;
	
	// client radar
	// TODO: This needs to be better integrated and saved/loaded
	private final L2Radar _radar;
	
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
	
	/** L2PcInstance's pledge class (knight, Baron, etc.) */
	private int _pledgeClass = 0;
	private int _pledgeType = 0;
	
	/** Level at which the player joined the clan as an academy member */
	private int _lvlJoinedAcademy = 0;
	
	private int _wantsPeace = 0;
	
	// Death Penalty Buff Level
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
	
	private boolean _messageRefusal = false; // message refusal mode
	
	private boolean _silenceMode = false; // silence mode
	private List<Integer> _silenceModeExcluded; // silence mode
	private boolean _dietMode = false; // ignore weight penalty
	private boolean _tradeRefusal = false; // Trade refusal
	private boolean _exchangeRefusal = false; // Exchange refusal
	
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
		return _protectEndTime > GameTimeController.getInstance().getGameTicks();
	}
	
	private long _teleportProtectEndTime = 0;
	
	public boolean isTeleportProtected()
	{
		return _teleportProtectEndTime > GameTimeController.getInstance().getGameTicks();
	}
	
	// protects a char from aggro mobs when getting up from fake death
	private long _recentFakeDeathEndTime = 0;
	private boolean _isFakeDeath;
	
	/** The fists L2Weapon of the L2PcInstance (used when no weapon is equipped) */
	private L2Weapon _fistsWeaponItem;
	
	private final Map<Integer, String> _chars = new FastMap<>();
	
	// private byte _updateKnownCounter = 0;
	
	private int _expertiseArmorPenalty = 0;
	private int _expertiseWeaponPenalty = 0;
	private int _expertisePenaltyBonus = 0;
	
	private boolean _isEnchanting = false;
	private int _activeEnchantItemId = ID_NONE;
	private int _activeEnchantSupportItemId = ID_NONE;
	private int _activeEnchantAttrItemId = ID_NONE;
	private long _activeEnchantTimestamp = 0;
	
	protected boolean _inventoryDisable = false;
	
	private final List<L2CubicInstance> _cubics = new CopyOnWriteArrayList<>();
	
	/** Active shots. */
	protected FastSet<Integer> _activeSoulShots = new FastSet<Integer>().shared();
	
	public final ReentrantLock soulShotLock = new ReentrantLock();
	
	/** Event parameters */
	private PlayerEventStatus eventStatus = null;
	
	private byte _handysBlockCheckerEventArena = -1;
	
	/** new loto ticket **/
	private final int _loto[] = new int[5];
	// public static int _loto_nums[] = {0,1,2,3,4,5,6,7,8,9,};
	/** new race ticket **/
	private final int _race[] = new int[2];
	
	private final BlockList _blockList = new BlockList(this);
	
	private L2Fishing _fishCombat;
	private boolean _fishing = false;
	private int _fishx = 0;
	private int _fishy = 0;
	private int _fishz = 0;
	
	private Set<Integer> _transformAllowedSkills;
	private ScheduledFuture<?> _taskRentPet;
	private ScheduledFuture<?> _taskWater;
	
	/** Bypass validations */
	private final List<String> _validBypass = new L2FastList<>(true);
	private final List<String> _validBypass2 = new L2FastList<>(true);
	
	private Forum _forumMail;
	private Forum _forumMemo;
	
	/** Current skill in use. Note that L2Character has _lastSkillCast, but this has the button presses */
	private SkillUseHolder _currentSkill;
	private SkillUseHolder _currentPetSkill;
	
	/** Skills queued because a skill is already in progress */
	private SkillUseHolder _queuedSkill;
	
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
	
	/** Map containing all custom skills of this player. */
	private Map<Integer, L2Skill> _customSkills = null;
	
	private boolean _canRevive = true;
	
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
		
		if (_PvPRegTask == null)
		{
			_PvPRegTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new PvPFlagTask(this), 1000, 1000);
		}
	}
	
	public void stopPvpRegTask()
	{
		if (_PvPRegTask != null)
		{
			_PvPRegTask.cancel(true);
			_PvPRegTask = null;
		}
	}
	
	public void stopPvPFlag()
	{
		stopPvpRegTask();
		
		updatePvPFlag(0);
		
		_PvPRegTask = null;
	}
	
	// Character UI
	private UIKeysSettings _uiKeySettings;
	
	/** Herbs Task Time **/
	private int _herbstask = 0;
	
	/** ShortBuff clearing Task */
	private ScheduledFuture<?> _shortBuffTask = null;
	
	// L2JMOD Wedding
	private boolean _married = false;
	private int _partnerId = 0;
	private int _coupleId = 0;
	private boolean _engagerequest = false;
	private int _engageid = 0;
	private boolean _marryrequest = false;
	private boolean _marryaccepted = false;
	
	// Save responder name for log it
	private String _lastPetitionGmName = null;
	
	/**
	 * Create a new L2PcInstance and add it in the characters table of the database.<br>
	 * <B><U> Actions</U> :</B>
	 * <ul>
	 * <li>Create a new L2PcInstance with an account name</li>
	 * <li>Set the name, the Hair Style, the Hair Color and the Face type of the L2PcInstance</li>
	 * <li>Add the player in the characters table of the database</li>
	 * </ul>
	 * @param template The L2PcTemplate to apply to the L2PcInstance
	 * @param accountName The name of the L2PcInstance
	 * @param name The name of the L2PcInstance
	 * @param app the player's appearance
	 * @return The L2PcInstance added to the database or null
	 */
	public static L2PcInstance create(L2PcTemplate template, String accountName, String name, PcAppearance app)
	{
		// Create a new L2PcInstance with an account name
		L2PcInstance player = new L2PcInstance(IdFactory.getInstance().getNextId(), template, accountName, app);
		// Set the name of the L2PcInstance
		player.setName(name);
		// Set Character's create time
		player.setCreateDate(Calendar.getInstance());
		// Set the base class ID to that of the actual class ID.
		player.setBaseClass(player.getClassId());
		// Kept for backwards compatibility.
		player.setNewbie(1);
		// Give 20 recommendations
		player.setRecomLeft(20);
		// Add the player in the characters table of the database
		return player.createDb() ? player : null;
	}
	
	public String getAccountName()
	{
		if (getClient() == null)
		{
			return getAccountNamePlayer();
		}
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
			{
				result |= RelationChanged.RELATION_CLAN_MATE;
			}
			if (getAllyId() != 0)
			{
				result |= RelationChanged.RELATION_ALLY_MEMBER;
			}
		}
		if (isClanLeader())
		{
			result |= RelationChanged.RELATION_LEADER;
		}
		if ((getParty() != null) && (getParty() == target.getParty()))
		{
			result |= RelationChanged.RELATION_HAS_PARTY;
			for (int i = 0; i < getParty().getMembers().size(); i++)
			{
				if (getParty().getMembers().get(i) != this)
				{
					continue;
				}
				switch (i)
				{
					case 0:
						result |= RelationChanged.RELATION_PARTYLEADER; // 0x10
						break;
					case 1:
						result |= RelationChanged.RELATION_PARTY4; // 0x8
						break;
					case 2:
						result |= RelationChanged.RELATION_PARTY3 + RelationChanged.RELATION_PARTY2 + RelationChanged.RELATION_PARTY1; // 0x7
						break;
					case 3:
						result |= RelationChanged.RELATION_PARTY3 + RelationChanged.RELATION_PARTY2; // 0x6
						break;
					case 4:
						result |= RelationChanged.RELATION_PARTY3 + RelationChanged.RELATION_PARTY1; // 0x5
						break;
					case 5:
						result |= RelationChanged.RELATION_PARTY3; // 0x4
						break;
					case 6:
						result |= RelationChanged.RELATION_PARTY2 + RelationChanged.RELATION_PARTY1; // 0x3
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
			{
				result |= RelationChanged.RELATION_TERRITORY_WAR;
			}
			else
			{
				result |= RelationChanged.RELATION_INSIEGE;
				if (getSiegeState() != target.getSiegeState())
				{
					result |= RelationChanged.RELATION_ENEMY;
				}
				else
				{
					result |= RelationChanged.RELATION_ALLY;
				}
				if (getSiegeState() == 1)
				{
					result |= RelationChanged.RELATION_ATTACKER;
				}
			}
		}
		if ((getClan() != null) && (target.getClan() != null))
		{
			if ((target.getPledgeType() != L2Clan.SUBUNIT_ACADEMY) && (getPledgeType() != L2Clan.SUBUNIT_ACADEMY) && target.getClan().isAtWarWith(getClan().getClanId()))
			{
				result |= RelationChanged.RELATION_1SIDED_WAR;
				if (getClan().isAtWarWith(target.getClan().getClanId()))
				{
					result |= RelationChanged.RELATION_MUTUAL_WAR;
				}
			}
		}
		if (getBlockCheckerArena() != -1)
		{
			result |= RelationChanged.RELATION_INSIEGE;
			ArenaParticipantsHolder holder = HandysBlockCheckerManager.getInstance().getHolder(getBlockCheckerArena());
			if (holder.getPlayerTeam(this) == 0)
			{
				result |= RelationChanged.RELATION_ENEMY;
			}
			else
			{
				result |= RelationChanged.RELATION_ALLY;
			}
			result |= RelationChanged.RELATION_ATTACKER;
		}
		return result;
	}
	
	/**
	 * Retrieve a L2PcInstance from the characters table of the database and add it in _allObjects of the L2world (call restore method).<br>
	 * <B><U> Actions</U> :</B>
	 * <ul>
	 * <li>Retrieve the L2PcInstance from the characters table of the database</li>
	 * <li>Add the L2PcInstance object in _allObjects</li>
	 * <li>Set the x,y,z position of the L2PcInstance and make it invisible</li>
	 * <li>Update the overloaded status of the L2PcInstance</li>
	 * </ul>
	 * @param objectId Identifier of the object to initialized
	 * @return The L2PcInstance loaded from the database
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
	 * Constructor of L2PcInstance (use L2Character constructor).<br>
	 * <B><U> Actions</U> :</B>
	 * <ul>
	 * <li>Call the L2Character constructor to create an empty _skills slot and copy basic Calculator set to this L2PcInstance</li>
	 * <li>Set the name of the L2PcInstance</li>
	 * </ul>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method SET the level of the L2PcInstance to 1</B></FONT>
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
		
		_accountName = accountName;
		app.setOwner(this);
		_appearance = app;
		
		// Create an AI
		_ai = new L2PlayerAI(new L2PcInstance.AIAccessor());
		
		// Create a L2Radar object
		_radar = new L2Radar(this);
		
		startVitalityTask();
	}
	
	@Override
	public final PcKnownList getKnownList()
	{
		return (PcKnownList) super.getKnownList();
	}
	
	@Override
	public void initKnownList()
	{
		setKnownList(new PcKnownList(this));
	}
	
	@Override
	public final PcStat getStat()
	{
		return (PcStat) super.getStat();
	}
	
	@Override
	public void initCharStat()
	{
		setStat(new PcStat(this));
	}
	
	@Override
	public final PcStatus getStatus()
	{
		return (PcStatus) super.getStatus();
	}
	
	@Override
	public void initCharStatus()
	{
		setStatus(new PcStatus(this));
	}
	
	@Override
	public void initCharEvents()
	{
		setCharEvents(new PlayerEvents(this));
	}
	
	@Override
	public PlayerEvents getEvents()
	{
		return (PlayerEvents) super.getEvents();
	}
	
	@Override
	public PcPosition getPosition()
	{
		return (PcPosition) super.getPosition();
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
	
	/**
	 * @return the L2PcTemplate link to the L2PcInstance.
	 */
	@Override
	public final L2PcTemplate getTemplate()
	{
		return (L2PcTemplate) super.getTemplate();
	}
	
	/**
	 * @param newclass
	 */
	public void setTemplate(ClassId newclass)
	{
		super.setTemplate(CharTemplateTable.getInstance().getTemplate(newclass));
	}
	
	/**
	 * Return the AI of the L2PcInstance (create it if necessary).
	 */
	@Override
	public L2CharacterAI getAI()
	{
		L2CharacterAI ai = _ai; // copy handle
		if (ai == null)
		{
			synchronized (this)
			{
				if (_ai == null)
				{
					_ai = new L2PlayerAI(new L2PcInstance.AIAccessor());
				}
				return _ai;
			}
		}
		return ai;
	}
	
	/** Return the Level of the L2PcInstance. */
	@Override
	public final int getLevel()
	{
		return getStat().getLevel();
	}
	
	@Override
	public double getLevelMod()
	{
		if (isTransformed())
		{
			double levelMod = getTransformation().getLevelMod(this);
			if (levelMod > -1)
			{
				return levelMod;
			}
		}
		return super.getLevelMod();
	}
	
	/**
	 * @return the _newbie rewards state of the L2PcInstance.
	 */
	public int getNewbie()
	{
		return _newbie;
	}
	
	/**
	 * Set the _newbie rewards state of the L2PcInstance.
	 * @param newbieRewards The Identifier of the _newbie state
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
	
	public boolean isInStoreMode()
	{
		return (getPrivateStoreType() > L2PcInstance.STORE_PRIVATE_NONE);
	}
	
	public boolean isInCraftMode()
	{
		return _inCraftMode;
	}
	
	public void isInCraftMode(boolean b)
	{
		_inCraftMode = b;
	}
	
	/**
	 * Manage Logout Task:
	 * <ul>
	 * <li>Remove player from world</li>
	 * <li>Save player data into DB</li>
	 * </ul>
	 */
	public void logout()
	{
		logout(true);
	}
	
	/**
	 * Manage Logout Task:
	 * <ul>
	 * <li>Remove player from world</li>
	 * <li>Save player data into DB</li>
	 * </ul>
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
	 * Add a new L2RecipList to the table _commonrecipebook containing all L2RecipeList of the L2PcInstance
	 * @param recipe The L2RecipeList to add to the _recipebook
	 * @param saveToDb
	 */
	public void registerCommonRecipeList(L2RecipeList recipe, boolean saveToDb)
	{
		_commonRecipeBook.put(recipe.getId(), recipe);
		
		if (saveToDb)
		{
			insertNewRecipeData(recipe.getId(), false);
		}
	}
	
	/**
	 * Add a new L2RecipList to the table _recipebook containing all L2RecipeList of the L2PcInstance
	 * @param recipe The L2RecipeList to add to the _recipebook
	 * @param saveToDb
	 */
	public void registerDwarvenRecipeList(L2RecipeList recipe, boolean saveToDb)
	{
		_dwarvenRecipeBook.put(recipe.getId(), recipe);
		
		if (saveToDb)
		{
			insertNewRecipeData(recipe.getId(), true);
		}
	}
	
	/**
	 * @param recipeId The Identifier of the L2RecipeList to check in the player's recipe books
	 * @return {@code true}if player has the recipe on Common or Dwarven Recipe book else returns {@code false}
	 */
	public boolean hasRecipeList(int recipeId)
	{
		return _dwarvenRecipeBook.containsKey(recipeId) || _commonRecipeBook.containsKey(recipeId);
	}
	
	/**
	 * Tries to remove a L2RecipList from the table _DwarvenRecipeBook or from table _CommonRecipeBook, those table contain all L2RecipeList of the L2PcInstance
	 * @param recipeId The Identifier of the L2RecipeList to remove from the _recipebook
	 */
	public void unregisterRecipeList(int recipeId)
	{
		if (_dwarvenRecipeBook.remove(recipeId) != null)
		{
			deleteRecipeData(recipeId, true);
		}
		else if (_commonRecipeBook.remove(recipeId) != null)
		{
			deleteRecipeData(recipeId, false);
		}
		else
		{
			_log.warning("Attempted to remove unknown RecipeList: " + recipeId);
		}
		
		for (L2ShortCut sc : getAllShortCuts())
		{
			if ((sc != null) && (sc.getId() == recipeId) && (sc.getType() == L2ShortCut.TYPE_RECIPE))
			{
				deleteShortCut(sc.getSlot(), sc.getPage());
			}
		}
	}
	
	private void insertNewRecipeData(int recipeId, boolean isDwarf)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("INSERT INTO character_recipebook (charId, id, classIndex, type) values(?,?,?,?)"))
		{
			statement.setInt(1, getObjectId());
			statement.setInt(2, recipeId);
			statement.setInt(3, isDwarf ? _classIndex : 0);
			statement.setInt(4, isDwarf ? 1 : 0);
			statement.execute();
		}
		catch (SQLException e)
		{
			if (_log.isLoggable(Level.SEVERE))
			{
				_log.log(Level.SEVERE, "SQL exception while inserting recipe: " + recipeId + " from character " + getObjectId(), e);
			}
		}
	}
	
	private void deleteRecipeData(int recipeId, boolean isDwarf)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM character_recipebook WHERE charId=? AND id=? AND classIndex=?"))
		{
			statement.setInt(1, getObjectId());
			statement.setInt(2, recipeId);
			statement.setInt(3, isDwarf ? _classIndex : 0);
			statement.execute();
		}
		catch (SQLException e)
		{
			if (_log.isLoggable(Level.SEVERE))
			{
				_log.log(Level.SEVERE, "SQL exception while deleting recipe: " + recipeId + " from character " + getObjectId(), e);
			}
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
	 * Add a QuestState to the table _quest containing all quests began by the L2PcInstance.
	 * @param qs The QuestState to add to _quest
	 */
	public void setQuestState(QuestState qs)
	{
		_quests.put(qs.getQuestName(), qs);
	}
	
	/**
	 * Verify if the player has the quest state.
	 * @param quest the quest state to check
	 * @return {@code true} if the player has the quest state, {@code false} otherwise
	 */
	public boolean hasQuestState(String quest)
	{
		return _quests.containsKey(quest);
	}
	
	/**
	 * Remove a QuestState from the table _quest containing all quests began by the L2PcInstance.
	 * @param quest The name of the quest
	 */
	public void delQuestState(String quest)
	{
		_quests.remove(quest);
	}
	
	private QuestState[] addToQuestStateArray(QuestState[] questStateArray, QuestState state)
	{
		final int len = questStateArray.length;
		QuestState[] tmp = new QuestState[len + 1];
		System.arraycopy(questStateArray, 0, tmp, 0, len);
		tmp[len] = state;
		return tmp;
	}
	
	/**
	 * @return a table containing all Quest in progress from the table _quests.
	 */
	public Quest[] getAllActiveQuests()
	{
		List<Quest> quests = new ArrayList<>();
		for (QuestState qs : _quests.values())
		{
			if ((qs == null) || (qs.getQuest() == null) || (!qs.isStarted() && !Config.DEVELOPER))
			{
				continue;
			}
			final int questId = qs.getQuest().getId();
			if ((questId > 19999) || (questId < 1))
			{
				continue;
			}
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
		for (Quest quest : npc.getTemplate().getEventQuests(QuestEventType.ON_ATTACK))
		{
			// Check if the Identifier of the L2Attackable attack is needed for the current quest
			if (getQuestState(quest.getName()) != null)
			{
				// Copy the current L2PcInstance QuestState in the QuestState table
				if (states == null)
				{
					states = new QuestState[]
					{
						getQuestState(quest.getName())
					};
				}
				else
				{
					states = addToQuestStateArray(states, getQuestState(quest.getName()));
				}
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
		for (Quest quest : npc.getTemplate().getEventQuests(QuestEventType.ON_KILL))
		{
			// Check if the Identifier of the L2Attackable killed is needed for the current quest
			if (getQuestState(quest.getName()) != null)
			{
				// Copy the current L2PcInstance QuestState in the QuestState table
				if (states == null)
				{
					states = new QuestState[]
					{
						getQuestState(quest.getName())
					};
				}
				else
				{
					states = addToQuestStateArray(states, getQuestState(quest.getName()));
				}
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
		List<Quest> quests = NpcTable.getInstance().getTemplate(npcId).getEventQuests(QuestEventType.ON_TALK);
		if (quests != null)
		{
			for (Quest quest : quests)
			{
				if (quest != null)
				{
					// Copy the current L2PcInstance QuestState in the QuestState table
					if (getQuestState(quest.getName()) != null)
					{
						if (states == null)
						{
							states = new QuestState[]
							{
								getQuestState(quest.getName())
							};
						}
						else
						{
							states = addToQuestStateArray(states, getQuestState(quest.getName()));
						}
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
		{
			event = "";
		}
		QuestState qs = getQuestState(quest);
		if ((qs == null) && event.isEmpty())
		{
			return retval;
		}
		if (qs == null)
		{
			Quest q = QuestManager.getInstance().getQuest(quest);
			if (q == null)
			{
				return retval;
			}
			qs = q.newQuestState(this);
		}
		if (qs != null)
		{
			if (getLastQuestNpcObject() > 0)
			{
				L2Object object = L2World.getInstance().findObject(getLastQuestNpcObject());
				if ((object instanceof L2Npc) && isInsideRadius(object, L2Npc.INTERACTION_DISTANCE, false, false))
				{
					L2Npc npc = (L2Npc) object;
					QuestState[] states = getQuestsForTalk(npc.getId());
					
					if (states != null)
					{
						for (QuestState state : states)
						{
							if (state.getQuest().getName().equals(qs.getQuest().getName()))
							{
								if (qs.getQuest().notifyEvent(event, npc, this))
								{
									showQuestWindow(quest, State.getStateName(qs.getState()));
								}
								
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
		String path = "data/scripts/quests/" + questId + "/" + stateId + ".htm";
		String content = HtmCache.getInstance().getHtm(getHtmlPrefix(), path); // TODO path for quests html
		
		if (content != null)
		{
			NpcHtmlMessage npcReply = new NpcHtmlMessage(5);
			npcReply.setHtml(content);
			sendPacket(npcReply);
		}
		
		sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	/** List of all QuestState instance that needs to be notified of this L2PcInstance's or its pet's death */
	private volatile List<QuestState> _notifyQuestOfDeathList;
	
	/**
	 * Add QuestState instance that is to be notified of L2PcInstance's death.
	 * @param qs The QuestState that subscribe to this event
	 */
	public void addNotifyQuestOfDeath(QuestState qs)
	{
		if (qs == null)
		{
			return;
		}
		
		if (!getNotifyQuestOfDeath().contains(qs))
		{
			getNotifyQuestOfDeath().add(qs);
		}
	}
	
	/**
	 * Remove QuestState instance that is to be notified of L2PcInstance's death.
	 * @param qs The QuestState that subscribe to this event
	 */
	public void removeNotifyQuestOfDeath(QuestState qs)
	{
		if ((qs == null) || (_notifyQuestOfDeathList == null))
		{
			return;
		}
		
		_notifyQuestOfDeathList.remove(qs);
	}
	
	/**
	 * @return a list of QuestStates which registered for notify of death of this L2PcInstance.
	 */
	public final List<QuestState> getNotifyQuestOfDeath()
	{
		if (_notifyQuestOfDeathList == null)
		{
			synchronized (this)
			{
				if (_notifyQuestOfDeathList == null)
				{
					_notifyQuestOfDeathList = new FastList<>();
				}
			}
		}
		
		return _notifyQuestOfDeathList;
	}
	
	public final boolean isNotifyQuestOfDeathEmpty()
	{
		return (_notifyQuestOfDeathList == null) || _notifyQuestOfDeathList.isEmpty();
	}
	
	/**
	 * @return a table containing all L2ShortCut of the L2PcInstance.
	 */
	public L2ShortCut[] getAllShortCuts()
	{
		return _shortCuts.getAllShortCuts();
	}
	
	/**
	 * @param slot The slot in which the shortCuts is equipped
	 * @param page The page of shortCuts containing the slot
	 * @return the L2ShortCut of the L2PcInstance corresponding to the position (page-slot).
	 */
	public L2ShortCut getShortCut(int slot, int page)
	{
		return _shortCuts.getShortCut(slot, page);
	}
	
	/**
	 * Add a L2shortCut to the L2PcInstance _shortCuts
	 * @param shortcut
	 */
	public void registerShortCut(L2ShortCut shortcut)
	{
		_shortCuts.registerShortCut(shortcut);
	}
	
	/**
	 * Updates the shortcut bars with the new skill.
	 * @param skillId the skill Id to search and update.
	 * @param skillLevel the skill level to update.
	 */
	public void updateShortCuts(int skillId, int skillLevel)
	{
		_shortCuts.updateShortCuts(skillId, skillLevel);
	}
	
	/**
	 * Delete the L2ShortCut corresponding to the position (page-slot) from the L2PcInstance _shortCuts.
	 * @param slot
	 * @param page
	 */
	public void deleteShortCut(int slot, int page)
	{
		_shortCuts.deleteShortCut(slot, page);
	}
	
	/**
	 * @param macro the macro to add to this L2PcInstance.
	 */
	public void registerMacro(Macro macro)
	{
		_macros.registerMacro(macro);
	}
	
	/**
	 * @param id the macro Id to delete.
	 */
	public void deleteMacro(int id)
	{
		_macros.deleteMacro(id);
	}
	
	/**
	 * @return all L2Macro of the L2PcInstance.
	 */
	public MacroList getMacros()
	{
		return _macros;
	}
	
	/**
	 * Set the siege state of the L2PcInstance.
	 * @param siegeState 1 = attacker, 2 = defender, 0 = not involved
	 */
	public void setSiegeState(byte siegeState)
	{
		_siegeState = siegeState;
	}
	
	/**
	 * Get the siege state of the L2PcInstance.
	 * @return 1 = attacker, 2 = defender, 0 = not involved
	 */
	public byte getSiegeState()
	{
		return _siegeState;
	}
	
	/**
	 * Set the siege Side of the L2PcInstance.
	 * @param val
	 */
	public void setSiegeSide(int val)
	{
		_siegeSide = val;
	}
	
	public boolean isRegisteredOnThisSiegeField(int val)
	{
		if ((_siegeSide != val) && ((_siegeSide < 81) || (_siegeSide > 89)))
		{
			return false;
		}
		return true;
	}
	
	public int getSiegeSide()
	{
		return _siegeSide;
	}
	
	/**
	 * Set the PvP Flag of the L2PcInstance.
	 * @param pvpFlag
	 */
	public void setPvpFlag(int pvpFlag)
	{
		_pvpFlag = (byte) pvpFlag;
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
		{
			return;
		}
		setPvpFlag(value);
		
		sendPacket(new UserInfo(this));
		sendPacket(new ExBrExtraUserInfo(this));
		
		// If this player has a pet update the pets pvp flag as well
		if (hasSummon())
		{
			sendPacket(new RelationChanged(getSummon(), getRelation(this), false));
		}
		
		Collection<L2PcInstance> plrs = getKnownList().getKnownPlayers().values();
		
		for (L2PcInstance target : plrs)
		{
			target.sendPacket(new RelationChanged(this, getRelation(target), isAutoAttackable(target)));
			if (hasSummon())
			{
				target.sendPacket(new RelationChanged(getSummon(), getRelation(target), isAutoAttackable(target)));
			}
		}
	}
	
	@Override
	public void revalidateZone(boolean force)
	{
		// Cannot validate if not in a world region (happens during teleport)
		if (getWorldRegion() == null)
		{
			return;
		}
		
		// This function is called too often from movement code
		if (force)
		{
			_zoneValidateCounter = 4;
		}
		else
		{
			_zoneValidateCounter--;
			if (_zoneValidateCounter < 0)
			{
				_zoneValidateCounter = 4;
			}
			else
			{
				return;
			}
		}
		
		getWorldRegion().revalidateZones(this);
		
		if (Config.ALLOW_WATER)
		{
			checkWaterState();
		}
		
		if (isInsideZone(ZoneId.ALTERED))
		{
			if (_lastCompassZone == ExSetCompassZoneCode.ALTEREDZONE)
			{
				return;
			}
			_lastCompassZone = ExSetCompassZoneCode.ALTEREDZONE;
			ExSetCompassZoneCode cz = new ExSetCompassZoneCode(ExSetCompassZoneCode.ALTEREDZONE);
			sendPacket(cz);
		}
		else if (isInsideZone(ZoneId.SIEGE))
		{
			if (_lastCompassZone == ExSetCompassZoneCode.SIEGEWARZONE2)
			{
				return;
			}
			_lastCompassZone = ExSetCompassZoneCode.SIEGEWARZONE2;
			ExSetCompassZoneCode cz = new ExSetCompassZoneCode(ExSetCompassZoneCode.SIEGEWARZONE2);
			sendPacket(cz);
		}
		else if (isInsideZone(ZoneId.PVP))
		{
			if (_lastCompassZone == ExSetCompassZoneCode.PVPZONE)
			{
				return;
			}
			_lastCompassZone = ExSetCompassZoneCode.PVPZONE;
			ExSetCompassZoneCode cz = new ExSetCompassZoneCode(ExSetCompassZoneCode.PVPZONE);
			sendPacket(cz);
		}
		else if (isIn7sDungeon())
		{
			if (_lastCompassZone == ExSetCompassZoneCode.SEVENSIGNSZONE)
			{
				return;
			}
			_lastCompassZone = ExSetCompassZoneCode.SEVENSIGNSZONE;
			ExSetCompassZoneCode cz = new ExSetCompassZoneCode(ExSetCompassZoneCode.SEVENSIGNSZONE);
			sendPacket(cz);
		}
		else if (isInsideZone(ZoneId.PEACE))
		{
			if (_lastCompassZone == ExSetCompassZoneCode.PEACEZONE)
			{
				return;
			}
			_lastCompassZone = ExSetCompassZoneCode.PEACEZONE;
			ExSetCompassZoneCode cz = new ExSetCompassZoneCode(ExSetCompassZoneCode.PEACEZONE);
			sendPacket(cz);
		}
		else
		{
			if (_lastCompassZone == ExSetCompassZoneCode.GENERALZONE)
			{
				return;
			}
			if (_lastCompassZone == ExSetCompassZoneCode.SIEGEWARZONE2)
			{
				updatePvPStatus();
			}
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
	 * Set the PK counter of the L2PcInstance.
	 * @param pkKills
	 */
	public void setPkKills(int pkKills)
	{
		if (!getEvents().onPKChange(_pkKills, pkKills))
		{
			return;
		}
		
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
	 * Set the _deleteTimer of the L2PcInstance.
	 * @param deleteTimer
	 */
	public void setDeleteTimer(long deleteTimer)
	{
		_deleteTimer = deleteTimer;
	}
	
	/**
	 * @return the number of recommendation obtained by the L2PcInstance.
	 */
	public int getRecomHave()
	{
		return _recomHave;
	}
	
	/**
	 * Increment the number of recommendation obtained by the L2PcInstance (Max : 255).
	 */
	protected void incRecomHave()
	{
		if (_recomHave < 255)
		{
			_recomHave++;
		}
	}
	
	/**
	 * Set the number of recommendation obtained by the L2PcInstance (Max : 255).
	 * @param value
	 */
	public void setRecomHave(int value)
	{
		_recomHave = Math.min(Math.max(value, 0), 255);
	}
	
	/**
	 * Set the number of recommendation obtained by the L2PcInstance (Max : 255).
	 * @param value
	 */
	public void setRecomLeft(int value)
	{
		_recomLeft = Math.min(Math.max(value, 0), 255);
	}
	
	/**
	 * @return the number of recommendation that the L2PcInstance can give.
	 */
	public int getRecomLeft()
	{
		return _recomLeft;
	}
	
	/**
	 * Increment the number of recommendation that the L2PcInstance can give.
	 */
	protected void decRecomLeft()
	{
		if (_recomLeft > 0)
		{
			_recomLeft--;
		}
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
	 * Return the Karma of the L2PcInstance.
	 */
	@Override
	public int getKarma()
	{
		return _karma;
	}
	
	/**
	 * Set the Karma of the L2PcInstance and send a Server->Client packet StatusUpdate (broadcast).
	 * @param karma
	 */
	public void setKarma(int karma)
	{
		if (!getEvents().onKarmaChange(_karma, karma))
		{
			return;
		}
		
		if (karma < 0)
		{
			karma = 0;
		}
		if ((_karma == 0) && (karma > 0))
		{
			Collection<L2Object> objs = getKnownList().getKnownObjects().values();
			
			for (L2Object object : objs)
			{
				if (!(object instanceof L2GuardInstance))
				{
					continue;
				}
				
				if (((L2GuardInstance) object).getAI().getIntention() == CtrlIntention.AI_INTENTION_IDLE)
				{
					((L2GuardInstance) object).getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE, null);
				}
			}
		}
		else if ((_karma > 0) && (karma == 0))
		{
			// Send a Server->Client StatusUpdate packet with Karma and PvP Flag to the L2PcInstance and all L2PcInstance to inform (broadcast)
			setKarmaFlag(0);
		}
		
		_karma = karma;
		broadcastKarma();
	}
	
	public int getExpertiseArmorPenalty()
	{
		return _expertiseArmorPenalty;
	}
	
	public int getExpertiseWeaponPenalty()
	{
		return _expertiseWeaponPenalty;
	}
	
	public int getExpertisePenaltyBonus()
	{
		return _expertisePenaltyBonus;
	}
	
	public void setExpertisePenaltyBonus(int bonus)
	{
		_expertisePenaltyBonus = bonus;
	}
	
	public int getWeightPenalty()
	{
		if (_dietMode)
		{
			return 0;
		}
		return _curWeightPenalty;
	}
	
	/**
	 * Update the overloaded status of the L2PcInstance.
	 */
	public void refreshOverloaded()
	{
		int maxLoad = getMaxLoad();
		if (maxLoad > 0)
		{
			long weightproc = (((getCurrentLoad() - getBonusWeightPenalty()) * 1000) / getMaxLoad());
			int newWeightPenalty;
			if ((weightproc < 500) || _dietMode)
			{
				newWeightPenalty = 0;
			}
			else if (weightproc < 666)
			{
				newWeightPenalty = 1;
			}
			else if (weightproc < 800)
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
				if ((newWeightPenalty > 0) && !_dietMode)
				{
					addSkill(SkillTable.getInstance().getInfo(4270, newWeightPenalty));
					setIsOverloaded(getCurrentLoad() > maxLoad);
				}
				else
				{
					removeSkill(getKnownSkill(4270), false, true);
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
		{
			return;
		}
		
		final int expertiseLevel = getExpertiseLevel();
		
		int armorPenalty = 0;
		int weaponPenalty = 0;
		int crystaltype;
		
		for (L2ItemInstance item : getInventory().getItems())
		{
			if ((item != null) && item.isEquipped() && ((item.getItemType() != L2EtcItemType.ARROW) && (item.getItemType() != L2EtcItemType.BOLT)))
			{
				crystaltype = item.getItem().getCrystalType();
				if (crystaltype > expertiseLevel)
				{
					if (item.isWeapon() && (crystaltype > weaponPenalty))
					{
						weaponPenalty = crystaltype;
					}
					else if (crystaltype > armorPenalty)
					{
						armorPenalty = crystaltype;
					}
				}
			}
		}
		
		boolean changed = false;
		final int bonus = getExpertisePenaltyBonus();
		
		// calc weapon penalty
		weaponPenalty = weaponPenalty - expertiseLevel - bonus;
		weaponPenalty = Math.min(Math.max(weaponPenalty, 0), 4);
		
		if ((getExpertiseWeaponPenalty() != weaponPenalty) || (getSkillLevel(FrequentSkill.WEAPON_GRADE_PENALTY.getId()) != weaponPenalty))
		{
			_expertiseWeaponPenalty = weaponPenalty;
			if (_expertiseWeaponPenalty > 0)
			{
				addSkill(SkillTable.getInstance().getInfo(FrequentSkill.WEAPON_GRADE_PENALTY.getId(), _expertiseWeaponPenalty));
			}
			else
			{
				removeSkill(getKnownSkill(FrequentSkill.WEAPON_GRADE_PENALTY.getId()), false, true);
			}
			changed = true;
		}
		
		// calc armor penalty
		armorPenalty = armorPenalty - expertiseLevel - bonus;
		armorPenalty = Math.min(Math.max(armorPenalty, 0), 4);
		
		if ((getExpertiseArmorPenalty() != armorPenalty) || (getSkillLevel(FrequentSkill.ARMOR_GRADE_PENALTY.getId()) != armorPenalty))
		{
			_expertiseArmorPenalty = armorPenalty;
			if (_expertiseArmorPenalty > 0)
			{
				addSkill(SkillTable.getInstance().getInfo(FrequentSkill.ARMOR_GRADE_PENALTY.getId(), _expertiseArmorPenalty));
			}
			else
			{
				removeSkill(getKnownSkill(FrequentSkill.ARMOR_GRADE_PENALTY.getId()), false, true);
			}
			changed = true;
		}
		
		if (changed)
		{
			sendPacket(new EtcStatusUpdate(this));
		}
	}
	
	public void useEquippableItem(L2ItemInstance item, boolean abortAttack)
	{
		// Equip or unEquip
		L2ItemInstance[] items = null;
		final boolean isEquiped = item.isEquipped();
		final int oldInvLimit = getInventoryLimit();
		SystemMessage sm = null;
		
		if (!fireEquipmentListeners(isEquiped, item))
		{
			return;
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
			// we can't unequip talisman by body slot
			if (slot == L2Item.SLOT_DECO)
			{
				items = getInventory().unEquipItemInSlotAndRecord(item.getLocationSlot());
			}
			else
			{
				items = getInventory().unEquipItemInBodySlotAndRecord(slot);
			}
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
					rechargeShots(true, true);
				}
			}
			else
			{
				sendPacket(SystemMessageId.CANNOT_EQUIP_ITEM_DUE_TO_BAD_CONDITION);
			}
		}
		refreshExpertisePenalty();
		
		broadcastUserInfo();
		
		InventoryUpdate iu = new InventoryUpdate();
		iu.addItems(Arrays.asList(items));
		sendPacket(iu);
		
		if (abortAttack)
		{
			abortAttack();
		}
		
		if (getInventoryLimit() != oldInvLimit)
		{
			sendPacket(new ExStorageMaxCount(this));
		}
	}
	
	/**
	 * @return the the PvP Kills of the L2PcInstance (Number of player killed during a PvP).
	 */
	public int getPvpKills()
	{
		return _pvpKills;
	}
	
	/**
	 * Set the the PvP Kills of the L2PcInstance (Number of player killed during a PvP).
	 * @param pvpKills
	 */
	public void setPvpKills(int pvpKills)
	{
		if (!getEvents().onPvPChange(_pvpKills, pvpKills))
		{
			return;
		}
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
	 * Set the Fame of this L2PcInstane
	 * @param fame
	 */
	public void setFame(int fame)
	{
		if (!getEvents().onFameChange(_fame, fame))
		{
			return;
		}
		_fame = (fame > Config.MAX_PERSONAL_FAME_POINTS) ? Config.MAX_PERSONAL_FAME_POINTS : fame;
	}
	
	/**
	 * @return the ClassId object of the L2PcInstance contained in L2PcTemplate.
	 */
	public ClassId getClassId()
	{
		return getTemplate().getClassId();
	}
	
	/**
	 * Set the template of the L2PcInstance.
	 * @param Id The Identifier of the L2PcTemplate to set to the L2PcInstance
	 */
	public void setClassId(int Id)
	{
		if (!_subclassLock.tryLock())
		{
			return;
		}
		
		try
		{
			if ((getLvlJoinedAcademy() != 0) && (_clan != null) && (PlayerClass.values()[Id].getLevel() == ClassLevel.Third))
			{
				if (getLvlJoinedAcademy() <= 16)
				{
					_clan.addReputationScore(Config.JOIN_ACADEMY_MAX_REP_SCORE, true);
				}
				else if (getLvlJoinedAcademy() >= 39)
				{
					_clan.addReputationScore(Config.JOIN_ACADEMY_MIN_REP_SCORE, true);
				}
				else
				{
					_clan.addReputationScore((Config.JOIN_ACADEMY_MAX_REP_SCORE - ((getLvlJoinedAcademy() - 16) * 20)), true);
				}
				setLvlJoinedAcademy(0);
				// oust pledge member from the academy, cuz he has finished his 2nd class transfer
				SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.CLAN_MEMBER_S1_EXPELLED);
				msg.addPcName(this);
				_clan.broadcastToOnlineMembers(msg);
				_clan.broadcastToOnlineMembers(new PledgeShowMemberListDelete(getName()));
				_clan.removeClanMember(getObjectId(), 0);
				sendPacket(SystemMessageId.ACADEMY_MEMBERSHIP_TERMINATED);
				
				// receive graduation gift
				getInventory().addItem("Gift", 8181, 1, this, null); // give academy circlet
			}
			if (isSubClassActive())
			{
				getSubClasses().get(_classIndex).setClassId(Id);
			}
			setTarget(this);
			broadcastPacket(new MagicSkillUse(this, 5103, 1, 1000, 0));
			setClassTemplate(Id);
			if (getClassId().level() == 3)
			{
				sendPacket(SystemMessageId.THIRD_CLASS_TRANSFER);
			}
			else
			{
				sendPacket(SystemMessageId.CLASS_TRANSFER);
			}
			
			// Update class icon in party and clan
			if (isInParty())
			{
				getParty().broadcastPacket(new PartySmallWindowUpdate(this));
			}
			
			if (getClan() != null)
			{
				getClan().broadcastToOnlineMembers(new PledgeShowMemberListUpdate(this));
			}
			
			// Add AutoGet skills and normal skills and/or learnByFS depending on configurations.
			rewardSkills();
			
			if (!canOverrideCond(PcCondOverride.SKILL_CONDITIONS) && Config.DECREASE_SKILL_LEVEL)
			{
				checkPlayerSkills();
			}
		}
		finally
		{
			_subclassLock.unlock();
		}
	}
	
	/**
	 * Used for AltGameSkillLearn to set a custom skill learning class Id.
	 */
	private ClassId _learningClass = getClassId();
	
	/**
	 * @return the custom skill learning class Id.
	 */
	public ClassId getLearningClass()
	{
		return _learningClass;
	}
	
	/**
	 * @param learningClass the custom skill learning class Id to set.
	 */
	public void setLearningClass(ClassId learningClass)
	{
		_learningClass = learningClass;
	}
	
	/**
	 * @return the Experience of the L2PcInstance.
	 */
	public long getExp()
	{
		return getStat().getExp();
	}
	
	public void setActiveEnchantAttrItemId(int objectId)
	{
		_activeEnchantAttrItemId = objectId;
	}
	
	public int getActiveEnchantAttrItemId()
	{
		return _activeEnchantAttrItemId;
	}
	
	public void setActiveEnchantItemId(int objectId)
	{
		// If we don't have a Enchant Item, we are not enchanting.
		if (objectId == ID_NONE)
		{
			setActiveEnchantSupportItemId(ID_NONE);
			setActiveEnchantTimestamp(0);
			setIsEnchanting(false);
		}
		_activeEnchantItemId = objectId;
	}
	
	public int getActiveEnchantItemId()
	{
		return _activeEnchantItemId;
	}
	
	public void setActiveEnchantSupportItemId(int objectId)
	{
		_activeEnchantSupportItemId = objectId;
	}
	
	public int getActiveEnchantSupportItemId()
	{
		return _activeEnchantSupportItemId;
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
	 * Set the fists weapon of the L2PcInstance (used when no weapon is equiped).
	 * @param weaponItem The fists L2Weapon to set to the L2PcInstance
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
			// human fighter fists
			L2Item temp = ItemTable.getInstance().getTemplate(246);
			weaponItem = (L2Weapon) temp;
		}
		else if ((classId >= 0x0a) && (classId <= 0x11))
		{
			// human mage fists
			L2Item temp = ItemTable.getInstance().getTemplate(251);
			weaponItem = (L2Weapon) temp;
		}
		else if ((classId >= 0x12) && (classId <= 0x18))
		{
			// elven fighter fists
			L2Item temp = ItemTable.getInstance().getTemplate(244);
			weaponItem = (L2Weapon) temp;
		}
		else if ((classId >= 0x19) && (classId <= 0x1e))
		{
			// elven mage fists
			L2Item temp = ItemTable.getInstance().getTemplate(249);
			weaponItem = (L2Weapon) temp;
		}
		else if ((classId >= 0x1f) && (classId <= 0x25))
		{
			// dark elven fighter fists
			L2Item temp = ItemTable.getInstance().getTemplate(245);
			weaponItem = (L2Weapon) temp;
		}
		else if ((classId >= 0x26) && (classId <= 0x2b))
		{
			// dark elven mage fists
			L2Item temp = ItemTable.getInstance().getTemplate(250);
			weaponItem = (L2Weapon) temp;
		}
		else if ((classId >= 0x2c) && (classId <= 0x30))
		{
			// orc fighter fists
			L2Item temp = ItemTable.getInstance().getTemplate(248);
			weaponItem = (L2Weapon) temp;
		}
		else if ((classId >= 0x31) && (classId <= 0x34))
		{
			// orc mage fists
			L2Item temp = ItemTable.getInstance().getTemplate(252);
			weaponItem = (L2Weapon) temp;
		}
		else if ((classId >= 0x35) && (classId <= 0x39))
		{
			// dwarven fists
			L2Item temp = ItemTable.getInstance().getTemplate(247);
			weaponItem = (L2Weapon) temp;
		}
		
		return weaponItem;
	}
	
	/**
	 * This method reward all AutoGet skills and Normal skills if Auto-Learn configuration is true.
	 */
	public void rewardSkills()
	{
		// Give all normal skills if activated Auto-Learn is activated, included AutoGet skills.
		if (Config.AUTO_LEARN_SKILLS)
		{
			giveAvailableSkills(Config.AUTO_LEARN_FS_SKILLS, true);
		}
		else
		{
			giveAvailableAutoGetSkills();
		}
		
		checkPlayerSkills();
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
		{
			setNoble(true);
		}
		
		// Add Hero skills if hero
		if (isHero())
		{
			setHero(true);
		}
		
		// Add clan skills
		if (getClan() != null)
		{
			L2Clan clan = getClan();
			clan.addSkillEffects(this);
			
			if ((clan.getLevel() >= SiegeManager.getInstance().getSiegeClanMinLevel()) && isClanLeader())
			{
				SiegeManager.getInstance().addSiegeSkills(this);
			}
			if (getClan().getCastleId() > 0)
			{
				CastleManager.getInstance().getCastleByOwner(getClan()).giveResidentialSkills(this);
			}
			if (getClan().getFortId() > 0)
			{
				FortManager.getInstance().getFortByOwner(getClan()).giveResidentialSkills(this);
			}
		}
		
		// Reload passive skills from armors / jewels / weapons
		getInventory().reloadEquippedItems();
		
		// Add Death Penalty Buff Level
		restoreDeathPenaltyBuffLevel();
	}
	
	/**
	 * Give all available skills to the player.
	 * @param includedByFs if {@code true} forgotten scroll skills present in the skill tree will be added
	 * @param includeAutoGet if {@code true} auto-get skills present in the skill tree will be added
	 * @return the amount of new skills earned
	 */
	public int giveAvailableSkills(boolean includedByFs, boolean includeAutoGet)
	{
		int skillCounter = 0;
		// Get available skills
		Collection<L2Skill> skills = SkillTreesData.getInstance().getAllAvailableSkills(this, getClassId(), includedByFs, includeAutoGet);
		for (L2Skill sk : skills)
		{
			if (getKnownSkill(sk.getId()) == sk)
			{
				continue;
			}
			
			if (getSkillLevel(sk.getId()) == -1)
			{
				skillCounter++;
			}
			
			// fix when learning toggle skills
			if (sk.isToggle())
			{
				final L2Effect toggleEffect = getFirstEffect(sk.getId());
				if (toggleEffect != null)
				{
					// stop old toggle skill effect, and give new toggle skill effect back
					toggleEffect.exit();
					sk.getEffects(this, this);
				}
			}
			
			addSkill(sk, true);
		}
		
		if (Config.AUTO_LEARN_SKILLS && (skillCounter > 0))
		{
			sendMessage("You have learned " + skillCounter + " new skills");
		}
		return skillCounter;
	}
	
	/**
	 * Give all available auto-get skills to the player.
	 */
	public void giveAvailableAutoGetSkills()
	{
		// Get available skills
		final List<L2SkillLearn> autoGetSkills = SkillTreesData.getInstance().getAvailableAutoGetSkills(this);
		final SkillTable st = SkillTable.getInstance();
		L2Skill skill;
		for (L2SkillLearn s : autoGetSkills)
		{
			skill = st.getInfo(s.getSkillId(), s.getSkillLevel());
			if (skill != null)
			{
				addSkill(skill, true);
			}
			else
			{
				_log.warning("Skipping null auto-get skill for player: " + toString());
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
		{
			exp = 0;
		}
		
		getStat().setExp(exp);
	}
	
	/**
	 * @return the Race object of the L2PcInstance.
	 */
	public Race getRace()
	{
		if (!isSubClassActive())
		{
			return getTemplate().getRace();
		}
		return CharTemplateTable.getInstance().getTemplate(_baseClass).getRace();
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
	public int getSp()
	{
		return getStat().getSp();
	}
	
	/**
	 * Set the SP amount of the L2PcInstance.
	 * @param sp
	 */
	public void setSp(int sp)
	{
		if (sp < 0)
		{
			sp = 0;
		}
		
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
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * @return the Clan Identifier of the L2PcInstance.
	 */
	@Override
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
		{
			return _clan.getCrestId();
		}
		
		return 0;
	}
	
	/**
	 * @return The Clan CrestLarge Identifier or 0
	 */
	public int getClanCrestLargeId()
	{
		if ((_clan != null) && ((_clan.getCastleId() != 0) || (_clan.getHideoutId() != 0)))
		{
			return _clan.getCrestLargeId();
		}
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
	
	public long getZoneRestartLimitTime()
	{
		return _zoneRestartLimitTime;
	}
	
	public void setZoneRestartLimitTime(long time)
	{
		_zoneRestartLimitTime = time;
	}
	
	public void storeZoneRestartLimitTime()
	{
		if (isInsideZone(ZoneId.NO_RESTART))
		{
			L2NoRestartZone zone = null;
			for (L2ZoneType tmpzone : ZoneManager.getInstance().getZones(this))
			{
				if (tmpzone instanceof L2NoRestartZone)
				{
					zone = (L2NoRestartZone) tmpzone;
					break;
				}
			}
			if (zone != null)
			{
				try (Connection con = L2DatabaseFactory.getInstance().getConnection();
					PreparedStatement statement = con.prepareStatement(UPDATE_ZONE_RESTART_LIMIT))
				{
					statement.setInt(1, getObjectId());
					statement.setLong(2, System.currentTimeMillis() + (zone.getRestartAllowedTime() * 1000));
					statement.execute();
				}
				catch (SQLException e)
				{
					_log.log(Level.WARNING, "Cannot store zone norestart limit for character " + getObjectId(), e);
				}
			}
		}
	}
	
	private void restoreZoneRestartLimitTime()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps1 = con.prepareStatement(LOAD_ZONE_RESTART_LIMIT))
		{
			ps1.setInt(1, getObjectId());
			try (ResultSet rset = ps1.executeQuery())
			{
				if (rset.next())
				{
					setZoneRestartLimitTime(rset.getLong("time_limit"));
					try (PreparedStatement ps2 = con.prepareStatement(DELETE_ZONE_RESTART_LIMIT))
					{
						ps2.setInt(1, getObjectId());
						ps2.executeUpdate();
					}
				}
			}
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Could not restore " + this + " zone restart time: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Return the PcInventory Inventory of the L2PcInstance contained in _inventory.
	 */
	@Override
	public PcInventory getInventory()
	{
		return _inventory;
	}
	
	/**
	 * Delete a ShortCut of the L2PcInstance _shortCuts.
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
	 * Sit down the L2PcInstance, set the AI Intention to AI_INTENTION_REST and send a Server->Client ChangeWaitType packet (broadcast)
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
			getAI().setIntention(CtrlIntention.AI_INTENTION_REST);
			broadcastPacket(new ChangeWaitType(this, ChangeWaitType.WT_SITTING));
			// Schedule a sit down task to wait for the animation to finish
			ThreadPoolManager.getInstance().scheduleGeneral(new SitDownTask(this), 2500);
			setIsParalyzed(true);
		}
	}
	
	/**
	 * Stand up the L2PcInstance, set the AI Intention to AI_INTENTION_IDLE and send a Server->Client ChangeWaitType packet (broadcast)
	 */
	public void standUp()
	{
		if (L2Event.isParticipant(this) && getEventStatus().eventSitForced)
		{
			sendMessage("A dark force beyond your mortal understanding makes your knees to shake when you try to stand up...");
		}
		else if (_waitTypeSitting && !isInStoreMode() && !isAlikeDead())
		{
			if (getEffectList().isAffected(EffectFlag.RELAXING))
			{
				stopEffects(L2EffectType.RELAXING);
			}
			
			broadcastPacket(new ChangeWaitType(this, ChangeWaitType.WT_STANDING));
			// Schedule a stand up task to wait for the animation to finish
			ThreadPoolManager.getInstance().scheduleGeneral(new StandUpTask(this), 2500);
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
		{
			WarehouseCacheManager.getInstance().addCacheTask(this);
		}
		return _warehouse;
	}
	
	/**
	 * Free memory used by Warehouse
	 */
	public void clearWarehouse()
	{
		if (_warehouse != null)
		{
			_warehouse.deleteMe();
		}
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
		return (_refund != null) && (_refund.getSize() > 0) && Config.ALLOW_REFUND;
	}
	
	/**
	 * @return refund object or create new if not exist
	 */
	public PcRefund getRefund()
	{
		if (_refund == null)
		{
			_refund = new PcRefund(this);
		}
		return _refund;
	}
	
	/**
	 * Clear refund
	 */
	public void clearRefund()
	{
		if (_refund != null)
		{
			_refund.deleteMe();
		}
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
			else
			{
				sendPacket(new ItemList(this, false));
			}
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
			if (sendMessage)
			{
				sendPacket(SystemMessageId.YOU_NOT_ENOUGH_ADENA);
			}
			return false;
		}
		
		if (count > 0)
		{
			L2ItemInstance adenaItem = _inventory.getAdenaInstance();
			if (!_inventory.reduceAdena(process, count, this, reference))
			{
				return false;
			}
			
			// Send update packet
			if (!Config.FORCE_INVENTORY_UPDATE)
			{
				InventoryUpdate iu = new InventoryUpdate();
				iu.addItem(adenaItem);
				sendPacket(iu);
			}
			else
			{
				sendPacket(new ItemList(this, false));
			}
			
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
			else
			{
				sendPacket(new ItemList(this, false));
			}
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
			{
				sendPacket(SystemMessageId.YOU_NOT_ENOUGH_ADENA);
			}
			
			return false;
		}
		
		if (count > 0)
		{
			L2ItemInstance ancientAdenaItem = _inventory.getAncientAdenaInstance();
			if (!_inventory.reduceAncientAdena(process, count, this, reference))
			{
				return false;
			}
			
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
			if (!canOverrideCond(PcCondOverride.ITEM_CONDITIONS) && !_inventory.validateCapacity(0, item.isQuestItem()) && newitem.isDropable() && (!newitem.isStackable() || (newitem.getLastChange() != L2ItemInstance.MODIFIED)))
			{
				dropItem("InvDrop", newitem, null, true, true);
			}
			else if (CursedWeaponsManager.getInstance().isCursed(newitem.getId()))
			{
				CursedWeaponsManager.getInstance().activate(this, newitem);
			}
			
			// Combat Flag
			else if (FortSiegeManager.getInstance().isCombat(item.getId()))
			{
				if (FortSiegeManager.getInstance().activateCombatFlag(this, item))
				{
					Fort fort = FortManager.getInstance().getFort(this);
					fort.getSiege().announceToPlayer(SystemMessage.getSystemMessage(SystemMessageId.C1_ACQUIRED_THE_FLAG), getName());
				}
			}
			// Territory Ward
			else if ((item.getId() >= 13560) && (item.getId() <= 13568))
			{
				TerritoryWard ward = TerritoryWarManager.getInstance().getTerritoryWard(item.getId() - 13479);
				if (ward != null)
				{
					ward.activate(this, item);
				}
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
			if (ItemTable.getInstance().getTemplate(itemId) != null)
			{
				item = ItemTable.getInstance().createDummyItem(itemId);
			}
			else
			{
				_log.log(Level.SEVERE, "Item doesn't exist so cannot be added. Item ID: " + itemId);
				return null;
			}
			// Sends message to client if requested
			if (sendMessage && ((!isCastingNow() && (item.getItemType() == L2EtcItemType.HERB)) || (item.getItemType() != L2EtcItemType.HERB)))
			{
				if (count > 1)
				{
					if (process.equalsIgnoreCase("Sweeper") || process.equalsIgnoreCase("Quest"))
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
					if (process.equalsIgnoreCase("Sweeper") || process.equalsIgnoreCase("Quest"))
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
			// Auto use herbs - autoloot
			if (item.getItemType() == L2EtcItemType.HERB) // If item is herb dont add it to iv :]
			{
				if (!isCastingNow())
				{
					L2ItemInstance herb = new L2ItemInstance(IdFactory.getInstance().getNextId(), itemId);
					IItemHandler handler = ItemHandler.getInstance().getHandler(herb.getEtcItem());
					if (handler == null)
					{
						_log.warning("No item handler registered for Herb - item ID " + herb.getId() + ".");
					}
					else
					{
						handler.useItem(this, herb, false);
						if (_herbstask >= 100)
						{
							_herbstask -= 100;
						}
					}
				}
				else
				{
					_herbstask += 100;
					ThreadPoolManager.getInstance().scheduleAi(new HerbTask(this, process, itemId, count, reference, sendMessage), _herbstask);
				}
			}
			else
			{
				// Add the item to inventory
				L2ItemInstance createdItem = _inventory.addItem(process, itemId, count, this, reference);
				
				// If over capacity, drop the item
				if (!canOverrideCond(PcCondOverride.ITEM_CONDITIONS) && !_inventory.validateCapacity(0, item.isQuestItem()) && createdItem.isDropable() && (!createdItem.isStackable() || (createdItem.getLastChange() != L2ItemInstance.MODIFIED)))
				{
					dropItem("InvDrop", createdItem, null, true);
				}
				else if (CursedWeaponsManager.getInstance().isCursed(createdItem.getId()))
				{
					CursedWeaponsManager.getInstance().activate(this, createdItem);
				}
				else if (FortSiegeManager.getInstance().isCombat(createdItem.getId()))
				{
					if (FortSiegeManager.getInstance().activateCombatFlag(this, item))
					{
						Fort fort = FortManager.getInstance().getFort(this);
						fort.getSiege().announceToPlayer(SystemMessage.getSystemMessage(SystemMessageId.C1_ACQUIRED_THE_FLAG), getName());
					}
				}
				// Territory Ward
				else if ((createdItem.getId() >= 13560) && (createdItem.getId() <= 13568))
				{
					TerritoryWard ward = TerritoryWarManager.getInstance().getTerritoryWard(createdItem.getId() - 13479);
					if (ward != null)
					{
						ward.activate(this, createdItem);
					}
				}
				return createdItem;
			}
		}
		return null;
	}
	
	/**
	 * @param process the process name
	 * @param item the item holder
	 * @param reference the reference object
	 * @param sendMessage if {@code true} a system message will be sent
	 */
	public void addItem(String process, ItemHolder item, L2Object reference, boolean sendMessage)
	{
		addItem(process, item.getId(), item.getCount(), reference, sendMessage);
	}
	
	/**
	 * Destroy item from inventory and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param item : L2ItemInstance to be destroyed
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successful
	 */
	public boolean destroyItem(String process, L2ItemInstance item, L2Object reference, boolean sendMessage)
	{
		return destroyItem(process, item, item.getCount(), reference, sendMessage);
	}
	
	/**
	 * Destroy item from inventory and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param item : L2ItemInstance to be destroyed
	 * @param count
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successful
	 */
	public boolean destroyItem(String process, L2ItemInstance item, long count, L2Object reference, boolean sendMessage)
	{
		item = _inventory.destroyItem(process, item, count, this, reference);
		
		if (item == null)
		{
			if (sendMessage)
			{
				sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
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
	 * @return boolean informing if the action was successful
	 */
	@Override
	public boolean destroyItem(String process, int objectId, long count, L2Object reference, boolean sendMessage)
	{
		L2ItemInstance item = _inventory.getItemByObjectId(objectId);
		
		if (item == null)
		{
			if (sendMessage)
			{
				sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
			}
			
			return false;
		}
		return destroyItem(process, item, count, reference, sendMessage);
	}
	
	/**
	 * Destroys shots from inventory without logging and only occasional saving to database. Sends a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param objectId : int Item Instance identifier of the item to be destroyed
	 * @param count : int Quantity of items to be destroyed
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successful
	 */
	public boolean destroyItemWithoutTrace(String process, int objectId, long count, L2Object reference, boolean sendMessage)
	{
		L2ItemInstance item = _inventory.getItemByObjectId(objectId);
		
		if ((item == null) || (item.getCount() < count))
		{
			if (sendMessage)
			{
				sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
			}
			
			return false;
		}
		
		return destroyItem(null, item, count, reference, sendMessage);
	}
	
	/**
	 * Destroy item from inventory by using its <B>itemId</B> and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param itemId : int Item identifier of the item to be destroyed
	 * @param count : int Quantity of items to be destroyed
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successful
	 */
	@Override
	public boolean destroyItemByItemId(String process, int itemId, long count, L2Object reference, boolean sendMessage)
	{
		if (itemId == PcInventory.ADENA_ID)
		{
			return reduceAdena(process, count, reference, sendMessage);
		}
		
		L2ItemInstance item = _inventory.getItemByItemId(itemId);
		
		if ((item == null) || (item.getCount() < count) || (_inventory.destroyItemByItemId(process, itemId, count, this, reference) == null))
		{
			if (sendMessage)
			{
				sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
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
		if (oldItem == null)
		{
			return null;
		}
		L2ItemInstance newItem = getInventory().transferItem(process, objectId, count, target, this, reference);
		if (newItem == null)
		{
			return null;
		}
		
		// Send inventory update packet
		if (!Config.FORCE_INVENTORY_UPDATE)
		{
			InventoryUpdate playerIU = new InventoryUpdate();
			
			if ((oldItem.getCount() > 0) && (oldItem != newItem))
			{
				playerIU.addModifiedItem(oldItem);
			}
			else
			{
				playerIU.addRemovedItem(oldItem);
			}
			
			sendPacket(playerIU);
		}
		else
		{
			sendPacket(new ItemList(this, false));
		}
		
		// Update current load as well
		StatusUpdate playerSU = new StatusUpdate(this);
		playerSU.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
		sendPacket(playerSU);
		
		// Send target update packet
		if (target instanceof PcInventory)
		{
			L2PcInstance targetPlayer = ((PcInventory) target).getOwner();
			
			if (!Config.FORCE_INVENTORY_UPDATE)
			{
				InventoryUpdate playerIU = new InventoryUpdate();
				
				if (newItem.getCount() > count)
				{
					playerIU.addModifiedItem(newItem);
				}
				else
				{
					playerIU.addNewItem(newItem);
				}
				
				targetPlayer.sendPacket(playerIU);
			}
			else
			{
				targetPlayer.sendPacket(new ItemList(targetPlayer, false));
			}
			
			// Update current load as well
			playerSU = new StatusUpdate(targetPlayer);
			playerSU.addAttribute(StatusUpdate.CUR_LOAD, targetPlayer.getCurrentLoad());
			targetPlayer.sendPacket(playerSU);
		}
		else if (target instanceof PetInventory)
		{
			PetInventoryUpdate petIU = new PetInventoryUpdate();
			
			if (newItem.getCount() > count)
			{
				petIU.addModifiedItem(newItem);
			}
			else
			{
				petIU.addNewItem(newItem);
			}
			
			((PetInventory) target).getOwner().sendPacket(petIU);
		}
		return newItem;
	}
	
	/**
	 * Use instead of calling {@link #addItem(String, L2ItemInstance, L2Object, boolean)} and {@link #destroyItemByItemId(String, int, long, L2Object, boolean)}<br>
	 * This method validates slots and weight limit, for stackable and non-stackable items.
	 * @param process a generic string representing the process that is exchanging this items
	 * @param reference the (probably NPC) reference, could be null
	 * @param coinId the item Id of the item given on the exchange
	 * @param cost the amount of items given on the exchange
	 * @param rewardId the item received on the exchange
	 * @param count the amount of items received on the exchange
	 * @param sendMessage if {@code true} it will send messages to the acting player
	 * @return {@code true} if the player successfully exchanged the items, {@code false} otherwise
	 */
	public boolean exchangeItemsById(String process, L2Object reference, int coinId, long cost, int rewardId, long count, boolean sendMessage)
	{
		final PcInventory inv = getInventory();
		if (!inv.validateCapacityByItemId(rewardId, count))
		{
			if (sendMessage)
			{
				sendPacket(SystemMessageId.SLOTS_FULL);
			}
			return false;
		}
		
		if (!inv.validateWeightByItemId(rewardId, count))
		{
			if (sendMessage)
			{
				sendPacket(SystemMessageId.WEIGHT_LIMIT_EXCEEDED);
			}
			return false;
		}
		
		if (destroyItemByItemId(process, coinId, cost, reference, sendMessage))
		{
			addItem(process, rewardId, count, reference, sendMessage);
			return true;
		}
		return false;
	}
	
	/**
	 * Drop item from inventory and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process String Identifier of process triggering this action
	 * @param item L2ItemInstance to be dropped
	 * @param reference L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage boolean Specifies whether to send message to Client about this action
	 * @param protectItem whether or not dropped item must be protected temporary against other players
	 * @return boolean informing if the action was successful
	 */
	public boolean dropItem(String process, L2ItemInstance item, L2Object reference, boolean sendMessage, boolean protectItem)
	{
		item = _inventory.dropItem(process, item, this, reference);
		
		if (item == null)
		{
			if (sendMessage)
			{
				sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
			}
			
			return false;
		}
		
		item.dropMe(this, (getX() + Rnd.get(50)) - 25, (getY() + Rnd.get(50)) - 25, getZ() + 20);
		
		if ((Config.AUTODESTROY_ITEM_AFTER > 0) && Config.DESTROY_DROPPED_PLAYER_ITEM && !Config.LIST_PROTECTED_ITEMS.contains(item.getId()))
		{
			if ((item.isEquipable() && Config.DESTROY_EQUIPABLE_PLAYER_ITEM) || !item.isEquipable())
			{
				ItemsAutoDestroy.getInstance().addItem(item);
			}
		}
		
		// protection against auto destroy dropped item
		if (Config.DESTROY_DROPPED_PLAYER_ITEM)
		{
			if (!item.isEquipable() || (item.isEquipable() && Config.DESTROY_EQUIPABLE_PLAYER_ITEM))
			{
				item.setProtected(false);
			}
			else
			{
				item.setProtected(true);
			}
		}
		else
		{
			item.setProtected(true);
		}
		
		// retail drop protection
		if (protectItem)
		{
			item.getDropProtection().protect(this);
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
			{
				sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
			}
			
			return null;
		}
		
		item.dropMe(this, x, y, z);
		
		if ((Config.AUTODESTROY_ITEM_AFTER > 0) && Config.DESTROY_DROPPED_PLAYER_ITEM && !Config.LIST_PROTECTED_ITEMS.contains(item.getId()))
		{
			if ((item.isEquipable() && Config.DESTROY_EQUIPABLE_PLAYER_ITEM) || !item.isEquipable())
			{
				ItemsAutoDestroy.getInstance().addItem(item);
			}
		}
		if (Config.DESTROY_DROPPED_PLAYER_ITEM)
		{
			if (!item.isEquipable() || (item.isEquipable() && Config.DESTROY_EQUIPABLE_PLAYER_ITEM))
			{
				item.setProtected(false);
			}
			else
			{
				item.setProtected(true);
			}
		}
		else
		{
			item.setProtected(true);
		}
		
		// retail drop protection
		if (protectItem)
		{
			item.getDropProtection().protect(this);
		}
		
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
		// TODO: if we remove objects that are not visisble from the L2World, we'll have to remove this check
		if (L2World.getInstance().findObject(objectId) == null)
		{
			_log.finest(getObjectId() + ": player tried to " + action + " item not available in L2World");
			return null;
		}
		
		L2ItemInstance item = getInventory().getItemByObjectId(objectId);
		
		if ((item == null) || (item.getOwnerId() != getObjectId()))
		{
			_log.finest(getObjectId() + ": player tried to " + action + " item he is not owner of");
			return null;
		}
		
		if ((count < 0) || ((count > 1) && !item.isStackable()))
		{
			_log.finest(getObjectId() + ": player tried to " + action + " item with invalid count: " + count);
			return null;
		}
		
		if (count > item.getCount())
		{
			_log.finest(getObjectId() + ": player tried to " + action + " more items than he owns");
			return null;
		}
		
		// Pet is summoned and not the item that summoned the pet AND not the buggle from strider you're mounting
		if ((hasSummon() && (getSummon().getControlObjectId() == objectId)) || (getMountObjectID() == objectId))
		{
			if (Config.DEBUG)
			{
				_log.finest(getObjectId() + ": player tried to " + action + " item controling pet");
			}
			
			return null;
		}
		
		if (getActiveEnchantItemId() == objectId)
		{
			if (Config.DEBUG)
			{
				_log.finest(getObjectId() + ":player tried to " + action + " an enchant scroll he was using");
			}
			return null;
		}
		
		// We cannot put a Weapon with Augmention in WH while casting (Possible Exploit)
		if (item.isAugmented() && (isCastingNow() || isCastingSimultaneouslyNow()))
		{
			return null;
		}
		
		return item;
	}
	
	/**
	 * Set _protectEndTime according settings.
	 * @param protect
	 */
	public void setProtection(boolean protect)
	{
		if (Config.DEVELOPER && (protect || (_protectEndTime > 0)))
		{
			_log.warning(getName() + ": Protection " + (protect ? "ON " + (GameTimeController.getInstance().getGameTicks() + (Config.PLAYER_SPAWN_PROTECTION * GameTimeController.TICKS_PER_SECOND)) : "OFF") + " (currently " + GameTimeController.getInstance().getGameTicks() + ")");
		}
		
		_protectEndTime = protect ? GameTimeController.getInstance().getGameTicks() + (Config.PLAYER_SPAWN_PROTECTION * GameTimeController.TICKS_PER_SECOND) : 0;
	}
	
	public void setTeleportProtection(boolean protect)
	{
		if (Config.DEVELOPER && (protect || (_teleportProtectEndTime > 0)))
		{
			_log.warning(getName() + ": Tele Protection " + (protect ? "ON " + (GameTimeController.getInstance().getGameTicks() + (Config.PLAYER_TELEPORT_PROTECTION * GameTimeController.TICKS_PER_SECOND)) : "OFF") + " (currently " + GameTimeController.getInstance().getGameTicks() + ")");
		}
		
		_teleportProtectEndTime = protect ? GameTimeController.getInstance().getGameTicks() + (Config.PLAYER_TELEPORT_PROTECTION * GameTimeController.TICKS_PER_SECOND) : 0;
	}
	
	/**
	 * Set protection from agro mobs when getting up from fake death, according settings.
	 * @param protect
	 */
	public void setRecentFakeDeath(boolean protect)
	{
		_recentFakeDeathEndTime = protect ? GameTimeController.getInstance().getGameTicks() + (Config.PLAYER_FAKEDEATH_UP_PROTECTION * GameTimeController.TICKS_PER_SECOND) : 0;
	}
	
	public boolean isRecentFakeDeath()
	{
		return _recentFakeDeathEndTime > GameTimeController.getInstance().getGameTicks();
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
		return super.isAlikeDead() || isFakeDeath();
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
	
	public String getIPAddress()
	{
		String ip = "N/A";
		if ((_client != null) && (_client.getConnectionAddress() != null))
		{
			ip = _client.getConnectionAddress().getHostAddress();
		}
		return ip;
	}
	
	/**
	 * Close the active connection with the client.
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
					{
						client.close(LeaveWorld.STATIC_PACKET);
					}
					else
					{
						client.close(ServerClose.STATIC_PACKET);
					}
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
	
	@Override
	public void enableSkill(L2Skill skill)
	{
		super.enableSkill(skill);
		_reuseTimeStampsSkills.remove(skill.getReuseHashCode());
	}
	
	@Override
	protected boolean checkDoCastConditions(L2Skill skill)
	{
		if (!super.checkDoCastConditions(skill))
		{
			return false;
		}
		
		if ((skill.getSkillType() == L2SkillType.SUMMON) && (hasSummon() || isMounted() || inObserverMode()))
		{
			sendPacket(SystemMessageId.SUMMON_ONLY_ONE);
			return false;
		}
		
		// TODO: Should possibly be checked only in L2PcInstance's useMagic
		// Can't use Hero and resurrect skills during Olympiad
		if (isInOlympiadMode() && (skill.isHeroSkill() || (skill.getSkillType() == L2SkillType.RESURRECT)))
		{
			sendPacket(SystemMessageId.THIS_SKILL_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT);
			return false;
		}
		
		// Check if the spell using charges or not in AirShip
		if (((getCharges() < skill.getChargeConsume())) || (isInAirShip() && !skill.hasEffectType(L2EffectType.REFUEL_AIRSHIP)))
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
	 * @return boolean
	 */
	private boolean needCpUpdate()
	{
		double currentCp = getCurrentCp();
		
		if ((currentCp <= 1.0) || (getMaxCp() < MAX_HP_BAR_PX))
		{
			return true;
		}
		
		if ((currentCp <= _cpUpdateDecCheck) || (currentCp >= _cpUpdateIncCheck))
		{
			if (currentCp == getMaxCp())
			{
				_cpUpdateIncCheck = currentCp + 1;
				_cpUpdateDecCheck = currentCp - _cpUpdateInterval;
			}
			else
			{
				double doubleMulti = currentCp / _cpUpdateInterval;
				int intMulti = (int) doubleMulti;
				
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
	private boolean needMpUpdate()
	{
		double currentMp = getCurrentMp();
		
		if ((currentMp <= 1.0) || (getMaxMp() < MAX_HP_BAR_PX))
		{
			return true;
		}
		
		if ((currentMp <= _mpUpdateDecCheck) || (currentMp >= _mpUpdateIncCheck))
		{
			if (currentMp == getMaxMp())
			{
				_mpUpdateIncCheck = currentMp + 1;
				_mpUpdateDecCheck = currentMp - _mpUpdateInterval;
			}
			else
			{
				double doubleMulti = currentMp / _mpUpdateInterval;
				int intMulti = (int) doubleMulti;
				
				_mpUpdateDecCheck = _mpUpdateInterval * (doubleMulti < intMulti ? intMulti-- : intMulti);
				_mpUpdateIncCheck = _mpUpdateDecCheck + _mpUpdateInterval;
			}
			
			return true;
		}
		
		return false;
	}
	
	/**
	 * Send packet StatusUpdate with current HP,MP and CP to the L2PcInstance and only current HP, MP and Level to all other L2PcInstance of the Party. <B><U> Actions</U> :</B> <li>Send the Server->Client packet StatusUpdate with current HP, MP and CP to this L2PcInstance</li><BR>
	 * <li>Send the Server->Client packet PartySmallWindowUpdate with current HP, MP and Level to all other L2PcInstance of the Party</li> <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T SEND current HP and MP to all L2PcInstance of the _statusListener</B></FONT>
	 */
	@Override
	public void broadcastStatusUpdate()
	{
		// TODO We mustn't send these informations to other players
		// Send the Server->Client packet StatusUpdate with current HP and MP to all L2PcInstance that must be informed of HP/MP updates of this L2PcInstance
		// super.broadcastStatusUpdate();
		
		// Send the Server->Client packet StatusUpdate with current HP, MP and CP to this L2PcInstance
		StatusUpdate su = new StatusUpdate(this);
		su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
		su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
		su.addAttribute(StatusUpdate.MAX_MP, getMaxMp());
		su.addAttribute(StatusUpdate.CUR_MP, (int) getCurrentMp());
		su.addAttribute(StatusUpdate.MAX_CP, getMaxCp());
		su.addAttribute(StatusUpdate.CUR_CP, (int) getCurrentCp());
		sendPacket(su);
		
		final boolean needCpUpdate = needCpUpdate();
		final boolean needHpUpdate = needHpUpdate();
		
		// Check if a party is in progress and party window update is usefull
		if (isInParty() && (needCpUpdate || needHpUpdate || needMpUpdate()))
		{
			getParty().broadcastToPartyMembers(this, new PartySmallWindowUpdate(this));
		}
		
		if (isInOlympiadMode() && isOlympiadStart() && (needCpUpdate || needHpUpdate))
		{
			final OlympiadGameTask game = OlympiadGameManager.getInstance().getOlympiadTask(getOlympiadGameId());
			if ((game != null) && game.isBattleStarted())
			{
				game.getZone().broadcastStatusUpdate(this);
			}
		}
		
		// In duel MP updated only with CP or HP
		if (isInDuel() && (needCpUpdate || needHpUpdate))
		{
			DuelManager.getInstance().broadcastToOppositTeam(this, new ExDuelUpdateUserInfo(this));
		}
	}
	
	/**
	 * Send a Server->Client packet UserInfo to this L2PcInstance and CharInfo to all L2PcInstance in its _KnownPlayers. <B><U> Concept</U> :</B> Others L2PcInstance in the detection area of the L2PcInstance are identified in <B>_knownPlayers</B>. In order to inform other players of this
	 * L2PcInstance state modifications, server just need to go through _knownPlayers to send Server->Client Packet <B><U> Actions</U> :</B> <li>Send a Server->Client packet UserInfo to this L2PcInstance (Public and Private Data)</li> <li>Send a Server->Client packet CharInfo to all L2PcInstance in
	 * _KnownPlayers of the L2PcInstance (Public data only)</li> <FONT COLOR=#FF0000><B> <U>Caution</U> : DON'T SEND UserInfo packet to other players instead of CharInfo packet. Indeed, UserInfo packet contains PRIVATE DATA as MaxHP, STR, DEX...</B></FONT>
	 */
	public final void broadcastUserInfo()
	{
		// Send a Server->Client packet UserInfo to this L2PcInstance
		sendPacket(new UserInfo(this));
		
		// Send a Server->Client packet CharInfo to all L2PcInstance in _KnownPlayers of the L2PcInstance
		broadcastPacket(new CharInfo(this));
		broadcastPacket(new ExBrExtraUserInfo(this));
		if (TerritoryWarManager.getInstance().isTWInProgress() && (TerritoryWarManager.getInstance().checkIsRegistered(-1, getObjectId()) || TerritoryWarManager.getInstance().checkIsRegistered(-1, getClan())))
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
		
		broadcastPacket(new NicknameChanged(this));
	}
	
	@Override
	public final void broadcastPacket(L2GameServerPacket mov)
	{
		if (!(mov instanceof CharInfo))
		{
			sendPacket(mov);
		}
		
		mov.setInvisible(getAppearance().getInvisible());
		
		Collection<L2PcInstance> plrs = getKnownList().getKnownPlayers().values();
		for (L2PcInstance player : plrs)
		{
			if (player == null)
			{
				continue;
			}
			player.sendPacket(mov);
			if (mov instanceof CharInfo)
			{
				int relation = getRelation(player);
				Integer oldrelation = getKnownList().getKnownRelations().get(player.getObjectId());
				if ((oldrelation != null) && (oldrelation != relation))
				{
					player.sendPacket(new RelationChanged(this, relation, isAutoAttackable(player)));
					if (hasSummon())
					{
						player.sendPacket(new RelationChanged(getSummon(), relation, isAutoAttackable(player)));
					}
				}
			}
		}
	}
	
	@Override
	public void broadcastPacket(L2GameServerPacket mov, int radiusInKnownlist)
	{
		if (!(mov instanceof CharInfo))
		{
			sendPacket(mov);
		}
		
		mov.setInvisible(getAppearance().getInvisible());
		
		Collection<L2PcInstance> plrs = getKnownList().getKnownPlayers().values();
		for (L2PcInstance player : plrs)
		{
			if (player == null)
			{
				continue;
			}
			if (isInsideRadius(player, radiusInKnownlist, false, false))
			{
				player.sendPacket(mov);
				if (mov instanceof CharInfo)
				{
					int relation = getRelation(player);
					Integer oldrelation = getKnownList().getKnownRelations().get(player.getObjectId());
					if ((oldrelation != null) && (oldrelation != relation))
					{
						player.sendPacket(new RelationChanged(this, relation, isAutoAttackable(player)));
						if (hasSummon())
						{
							player.sendPacket(new RelationChanged(getSummon(), relation, isAutoAttackable(player)));
						}
					}
				}
			}
		}
	}
	
	/**
	 * @return the Alliance Identifier of the L2PcInstance.
	 */
	@Override
	public int getAllyId()
	{
		if (_clan == null)
		{
			return 0;
		}
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
	
	public void queryGameGuard()
	{
		if (getClient() != null)
		{
			getClient().setGameGuardOk(false);
			sendPacket(GameGuardQuery.STATIC_PACKET);
		}
		if (Config.GAMEGUARD_ENFORCE)
		{
			ThreadPoolManager.getInstance().scheduleGeneral(new GameGuardCheckTask(this), 30 * 1000);
		}
	}
	
	/**
	 * Send a Server->Client packet StatusUpdate to the L2PcInstance.
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
	 * Send SystemMessage packet.
	 * @param id SystemMessageId
	 */
	@Override
	public void sendPacket(SystemMessageId id)
	{
		sendPacket(SystemMessage.getSystemMessage(id));
	}
	
	/**
	 * Manage Interact Task with another L2PcInstance. <B><U> Actions</U> :</B> <li>If the private store is a STORE_PRIVATE_SELL, send a Server->Client PrivateBuyListSell packet to the L2PcInstance</li> <li>If the private store is a STORE_PRIVATE_BUY, send a Server->Client PrivateBuyListBuy packet
	 * to the L2PcInstance</li> <li>If the private store is a STORE_PRIVATE_MANUFACTURE, send a Server->Client RecipeShopSellList packet to the L2PcInstance</li>
	 * @param target The L2Character targeted
	 */
	public void doInteract(L2Character target)
	{
		if (target instanceof L2PcInstance)
		{
			L2PcInstance temp = (L2PcInstance) target;
			sendPacket(ActionFailed.STATIC_PACKET);
			
			if ((temp.getPrivateStoreType() == STORE_PRIVATE_SELL) || (temp.getPrivateStoreType() == STORE_PRIVATE_PACKAGE_SELL))
			{
				sendPacket(new PrivateStoreListSell(this, temp));
			}
			else if (temp.getPrivateStoreType() == STORE_PRIVATE_BUY)
			{
				sendPacket(new PrivateStoreListBuy(this, temp));
			}
			else if (temp.getPrivateStoreType() == STORE_PRIVATE_MANUFACTURE)
			{
				sendPacket(new RecipeShopSellList(this, temp));
			}
			
		}
		else
		{
			// _interactTarget=null should never happen but one never knows ^^;
			if (target != null)
			{
				target.onAction(this);
			}
		}
	}
	
	/**
	 * Manage AutoLoot Task. <B><U> Actions</U> :</B> <li>Send a System Message to the L2PcInstance : YOU_PICKED_UP_S1_ADENA or YOU_PICKED_UP_S1_S2</li> <li>Add the Item to the L2PcInstance inventory</li> <li>Send a Server->Client packet InventoryUpdate to this L2PcInstance with NewItem (use a new
	 * slot) or ModifiedItem (increase amount)</li> <li>Send a Server->Client packet StatusUpdate to this L2PcInstance with current weight</li> <FONT COLOR=#FF0000><B> <U>Caution</U> : If a Party is in progress, distribute Items between party members</B></FONT>
	 * @param target The L2ItemInstance dropped
	 * @param item
	 */
	public void doAutoLoot(L2Attackable target, ItemHolder item)
	{
		if (isInParty() && (ItemTable.getInstance().getTemplate(item.getId()).getItemType() != L2EtcItemType.HERB))
		{
			getParty().distributeItem(this, item, false, target);
		}
		else if (item.getId() == PcInventory.ADENA_ID)
		{
			addAdena("Loot", item.getCount(), target, true);
		}
		else
		{
			addItem("Loot", item, target, true);
		}
	}
	
	/**
	 * Manage Pickup Task. <B><U> Actions</U> :</B> <li>Send a Server->Client packet StopMove to this L2PcInstance</li> <li>Remove the L2ItemInstance from the world and send server->client GetItem packets</li> <li>Send a System Message to the L2PcInstance : YOU_PICKED_UP_S1_ADENA or
	 * YOU_PICKED_UP_S1_S2</li> <li>Add the Item to the L2PcInstance inventory</li> <li>Send a Server->Client packet InventoryUpdate to this L2PcInstance with NewItem (use a new slot) or ModifiedItem (increase amount)</li> <li>Send a Server->Client packet StatusUpdate to this L2PcInstance with
	 * current weight</li> <FONT COLOR=#FF0000><B> <U>Caution</U> : If a Party is in progress, distribute Items between party members</B></FONT>
	 * @param object The L2ItemInstance to pick up
	 */
	protected void doPickupItem(L2Object object)
	{
		if (isAlikeDead() || isFakeDeath())
		{
			return;
		}
		
		// Set the AI Intention to AI_INTENTION_IDLE
		getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		
		// Check if the L2Object to pick up is a L2ItemInstance
		if (!(object instanceof L2ItemInstance))
		{
			// dont try to pickup anything that is not an item :)
			_log.warning(this + " trying to pickup wrong target." + getTarget());
			return;
		}
		
		L2ItemInstance target = (L2ItemInstance) object;
		
		// Send a Server->Client packet ActionFailed to this L2PcInstance
		sendPacket(ActionFailed.STATIC_PACKET);
		
		// Send a Server->Client packet StopMove to this L2PcInstance
		StopMove sm = new StopMove(this);
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
			
			if (((isInParty() && (getParty().getLootDistribution() == L2Party.ITEM_LOOTER)) || !isInParty()) && !_inventory.validateCapacity(target))
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(SystemMessageId.SLOTS_FULL);
				return;
			}
			
			if (isInvul() && !canOverrideCond(PcCondOverride.ITEM_CONDITIONS))
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				smsg = SystemMessage.getSystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1);
				smsg.addItemName(target);
				sendPacket(smsg);
				return;
			}
			
			if ((target.getOwnerId() != 0) && (target.getOwnerId() != getObjectId()) && !isInLooterParty(target.getOwnerId()))
			{
				if (target.getId() == PcInventory.ADENA_ID)
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
			if (FortSiegeManager.getInstance().isCombat(target.getId()))
			{
				if (!FortSiegeManager.getInstance().checkIfCanPickup(this))
				{
					return;
				}
			}
			
			if ((target.getItemLootShedule() != null) && ((target.getOwnerId() == getObjectId()) || isInLooterParty(target.getOwnerId())))
			{
				target.resetOwnerTimer();
			}
			
			// Remove the L2ItemInstance from the world and send server->client GetItem packets
			target.pickupMe(this);
			if (Config.SAVE_DROPPED_ITEM)
			{
				ItemsOnGroundManager.getInstance().removeObject(target);
			}
		}
		
		// Auto use herbs - pick up
		if (target.getItemType() == L2EtcItemType.HERB)
		{
			IItemHandler handler = ItemHandler.getInstance().getHandler(target.getEtcItem());
			if (handler == null)
			{
				_log.fine("No item handler registered for item ID " + target.getId() + ".");
			}
			else
			{
				handler.useItem(this, target, false);
			}
			ItemTable.getInstance().destroyItem("Consume", target, this, null);
		}
		// Cursed Weapons are not distributed
		else if (CursedWeaponsManager.getInstance().isCursed(target.getId()))
		{
			addItem("Pickup", target, null, true);
		}
		else if (FortSiegeManager.getInstance().isCombat(target.getId()))
		{
			addItem("Pickup", target, null, true);
		}
		else
		{
			// if item is instance of L2ArmorType or L2WeaponType broadcast an "Attention" system message
			if ((target.getItemType() instanceof L2ArmorType) || (target.getItemType() instanceof L2WeaponType))
			{
				if (target.getEnchantLevel() > 0)
				{
					smsg = SystemMessage.getSystemMessage(SystemMessageId.ANNOUNCEMENT_C1_PICKED_UP_S2_S3);
					smsg.addPcName(this);
					smsg.addNumber(target.getEnchantLevel());
					smsg.addItemName(target.getId());
					broadcastPacket(smsg, 1400);
				}
				else
				{
					smsg = SystemMessage.getSystemMessage(SystemMessageId.ANNOUNCEMENT_C1_PICKED_UP_S2);
					smsg.addPcName(this);
					smsg.addItemName(target.getId());
					broadcastPacket(smsg, 1400);
				}
			}
			
			// Check if a Party is in progress
			if (isInParty())
			{
				getParty().distributeItem(this, target);
			}
			else if ((target.getId() == PcInventory.ADENA_ID) && (getInventory().getAdenaInstance() != null))
			{
				addAdena("Pickup", target.getCount(), null, true);
				ItemTable.getInstance().destroyItem("Pickup", target, this, null);
			}
			else
			{
				addItem("Pickup", target, null, true);
				// Auto-Equip arrows/bolts if player has a bow/crossbow and player picks up arrows/bolts.
				final L2ItemInstance weapon = getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
				if (weapon != null)
				{
					final L2EtcItem etcItem = target.getEtcItem();
					if (etcItem != null)
					{
						final L2EtcItemType itemType = etcItem.getItemType();
						if ((weapon.getItemType() == L2WeaponType.BOW) && (itemType == L2EtcItemType.ARROW))
						{
							checkAndEquipArrows();
						}
						else if ((weapon.getItemType() == L2WeaponType.CROSSBOW) && (itemType == L2EtcItemType.BOLT))
						{
							checkAndEquipBolts();
						}
					}
				}
			}
		}
	}
	
	public boolean canOpenPrivateStore()
	{
		return !isAlikeDead() && !isInOlympiadMode() && !isMounted() && !isInsideZone(ZoneId.NO_STORE) && !isCastingNow();
	}
	
	public void tryOpenPrivateBuyStore()
	{
		// Player shouldn't be able to set stores if he/she is alike dead (dead or fake death)
		if (canOpenPrivateStore())
		{
			if ((getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_BUY) || (getPrivateStoreType() == (L2PcInstance.STORE_PRIVATE_BUY + 1)))
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
			if (isInsideZone(ZoneId.NO_STORE))
			{
				sendPacket(SystemMessageId.NO_PRIVATE_STORE_HERE);
			}
			sendPacket(ActionFailed.STATIC_PACKET);
		}
	}
	
	public void tryOpenPrivateSellStore(boolean isPackageSale)
	{
		// Player shouldn't be able to set stores if he/she is alike dead (dead or fake death)
		if (canOpenPrivateStore())
		{
			if ((getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_SELL) || (getPrivateStoreType() == (L2PcInstance.STORE_PRIVATE_SELL + 1)) || (getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_PACKAGE_SELL))
			{
				setPrivateStoreType(L2PcInstance.STORE_PRIVATE_NONE);
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
			if (isInsideZone(ZoneId.NO_STORE))
			{
				sendPacket(SystemMessageId.NO_PRIVATE_STORE_HERE);
			}
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
		return (_transformation != null) && !_transformation.isStance();
	}
	
	public boolean isInStance()
	{
		return (_transformation != null) && _transformation.isStance();
	}
	
	public void transform(Transform transformation)
	{
		if (_transformation != null)
		{
			// You already polymorphed and cannot polymorph again.
			SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.YOU_ALREADY_POLYMORPHED_AND_CANNOT_POLYMORPH_AGAIN);
			sendPacket(msg);
			return;
		}
		
		if (!fireTransformListeners(transformation, true))
		{
			return;
		}
		
		setQueuedSkill(null, false, false);
		if (isMounted())
		{
			// Get off the strider or something else if character is mounted
			dismount();
		}
		
		_transformation = transformation;
		stopAllToggles();
		transformation.onTransform(this);
		sendSkillList();
		sendPacket(new SkillCoolTime(this));
		broadcastUserInfo();
	}
	
	@Override
	public void untransform()
	{
		if (_transformation != null)
		{
			if (!fireTransformListeners(_transformation, false))
			{
				return;
			}
			setQueuedSkill(null, false, false);
			_transformation.onUntransform(this);
			_transformation = null;
			stopEffects(L2EffectType.TRANSFORMATION);
			sendSkillList();
			sendPacket(new SkillCoolTime(this));
			broadcastUserInfo();
		}
	}
	
	@Override
	public Transform getTransformation()
	{
		return _transformation;
	}
	
	/**
	 * This returns the transformation Id of the current transformation. For example, if a player is transformed as a Buffalo, and then picks up the Zariche, the transform Id returned will be that of the Zariche, and NOT the Buffalo.
	 * @return Transformation Id
	 */
	public int getTransformationId()
	{
		return (isTransformed() ? getTransformation().getId() : 0);
	}
	
	public int getTransformationDisplayId()
	{
		return (isTransformed() ? getTransformation().getDisplayId() : 0);
	}
	
	/**
	 * Set a target. <B><U> Actions</U> :</B>
	 * <ul>
	 * <li>Remove the L2PcInstance from the _statusListener of the old target if it was a L2Character</li>
	 * <li>Add the L2PcInstance to the _statusListener of the new target if it's a L2Character</li>
	 * <li>Target the new L2Object (add the target to the L2PcInstance _target, _knownObject and L2PcInstance to _KnownObject of the L2Object)</li>
	 * </ul>
	 * @param newTarget The L2Object to target
	 */
	@Override
	public void setTarget(L2Object newTarget)
	{
		if (newTarget != null)
		{
			boolean isInParty = (newTarget.isPlayer() && isInParty() && getParty().containsPlayer(newTarget.getActingPlayer()));
			
			// Prevents /target exploiting
			if (!isInParty && (Math.abs(newTarget.getZ() - getZ()) > 1000))
			{
				newTarget = null;
			}
			
			// Check if the new target is visible
			if ((newTarget != null) && !isInParty && !newTarget.isVisible())
			{
				newTarget = null;
			}
			
			// vehicles cant be targeted
			if (!isGM() && (newTarget instanceof L2Vehicle))
			{
				newTarget = null;
			}
		}
		
		// Get the current target
		L2Object oldTarget = getTarget();
		
		if (oldTarget != null)
		{
			if (oldTarget.equals(newTarget)) // no target change?
			{
				// Validate location of the target.
				if ((newTarget != null) && (newTarget.getObjectId() != getObjectId()))
				{
					sendPacket(new ValidateLocation(newTarget));
				}
				return;
			}
			
			// Remove the target from the status listener.
			oldTarget.removeStatusListener(this);
		}
		
		if (newTarget instanceof L2Character)
		{
			final L2Character target = (L2Character) newTarget;
			
			// Validate location of the new target.
			if (newTarget.getObjectId() != getObjectId())
			{
				sendPacket(new ValidateLocation(target));
			}
			
			// Show the client his new target.
			sendPacket(new MyTargetSelected(this, target));
			
			// Register target to listen for hp changes.
			target.addStatusListener(this);
			
			// Send max/current hp.
			final StatusUpdate su = new StatusUpdate(target);
			su.addAttribute(StatusUpdate.MAX_HP, target.getMaxHp());
			su.addAttribute(StatusUpdate.CUR_HP, (int) target.getCurrentHp());
			sendPacket(su);
			
			// To others the new target, and not yourself!
			Broadcast.toKnownPlayers(this, new TargetSelected(getObjectId(), newTarget.getObjectId(), getX(), getY(), getZ()));
		}
		
		// Target was removed?
		if ((newTarget == null) && (getTarget() != null))
		{
			broadcastPacket(new TargetUnselected(this));
		}
		
		// Target the new L2Object (add the target to the L2PcInstance _target, _knownObject and L2PcInstance to _KnownObject of the L2Object)
		super.setTarget(newTarget);
	}
	
	/**
	 * Return the active weapon instance (always equiped in the right hand).
	 */
	@Override
	public L2ItemInstance getActiveWeaponInstance()
	{
		return getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
	}
	
	/**
	 * Return the active weapon item (always equiped in the right hand).
	 */
	@Override
	public L2Weapon getActiveWeaponItem()
	{
		L2ItemInstance weapon = getActiveWeaponInstance();
		
		if (weapon == null)
		{
			return getFistsWeaponItem();
		}
		
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
		{
			return null;
		}
		
		return (L2Armor) armor.getItem();
	}
	
	public L2Armor getActiveLegsArmorItem()
	{
		L2ItemInstance legs = getLegsArmorInstance();
		
		if (legs == null)
		{
			return null;
		}
		
		return (L2Armor) legs.getItem();
	}
	
	public boolean isWearingHeavyArmor()
	{
		L2ItemInstance legs = getLegsArmorInstance();
		L2ItemInstance armor = getChestArmorInstance();
		
		if ((armor != null) && (legs != null))
		{
			if (((L2ArmorType) legs.getItemType() == L2ArmorType.HEAVY) && ((L2ArmorType) armor.getItemType() == L2ArmorType.HEAVY))
			{
				return true;
			}
		}
		if (armor != null)
		{
			if (((getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST).getItem().getBodyPart() == L2Item.SLOT_FULL_ARMOR) && ((L2ArmorType) armor.getItemType() == L2ArmorType.HEAVY)))
			{
				return true;
			}
		}
		return false;
	}
	
	public boolean isWearingLightArmor()
	{
		L2ItemInstance legs = getLegsArmorInstance();
		L2ItemInstance armor = getChestArmorInstance();
		
		if ((armor != null) && (legs != null))
		{
			if (((L2ArmorType) legs.getItemType() == L2ArmorType.LIGHT) && ((L2ArmorType) armor.getItemType() == L2ArmorType.LIGHT))
			{
				return true;
			}
		}
		if (armor != null)
		{
			if (((getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST).getItem().getBodyPart() == L2Item.SLOT_FULL_ARMOR) && ((L2ArmorType) armor.getItemType() == L2ArmorType.LIGHT)))
			{
				return true;
			}
		}
		return false;
	}
	
	public boolean isWearingMagicArmor()
	{
		L2ItemInstance legs = getLegsArmorInstance();
		L2ItemInstance armor = getChestArmorInstance();
		
		if ((armor != null) && (legs != null))
		{
			if (((L2ArmorType) legs.getItemType() == L2ArmorType.MAGIC) && ((L2ArmorType) armor.getItemType() == L2ArmorType.MAGIC))
			{
				return true;
			}
		}
		if (armor != null)
		{
			if (((getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST).getItem().getBodyPart() == L2Item.SLOT_FULL_ARMOR) && ((L2ArmorType) armor.getItemType() == L2ArmorType.MAGIC)))
			{
				return true;
			}
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
	
	public void setEngageRequest(boolean state, int playerid)
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
		if (!_engagerequest)
		{
			return;
		}
		else if (_engageid == 0)
		{
			return;
		}
		else
		{
			L2PcInstance ptarget = L2World.getInstance().getPlayer(_engageid);
			setEngageRequest(false, 0);
			if (ptarget != null)
			{
				if (answer == 1)
				{
					CoupleManager.getInstance().createCouple(ptarget, L2PcInstance.this);
					ptarget.sendMessage("Request to Engage has been >ACCEPTED<");
				}
				else
				{
					ptarget.sendMessage("Request to Engage has been >DENIED<!");
				}
			}
		}
	}
	
	/**
	 * Return the secondary weapon instance (always equiped in the left hand).
	 */
	@Override
	public L2ItemInstance getSecondaryWeaponInstance()
	{
		return getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
	}
	
	/**
	 * Return the secondary L2Item item (always equiped in the left hand).<BR>
	 * Arrows, Shield..<BR>
	 */
	@Override
	public L2Item getSecondaryWeaponItem()
	{
		L2ItemInstance item = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		if (item != null)
		{
			return item.getItem();
		}
		return null;
	}
	
	/**
	 * Kill the L2Character, Apply Death Penalty, Manage gain/loss Karma and Item Drop. <B><U> Actions</U> :</B> <li>Reduce the Experience of the L2PcInstance in function of the calculated Death Penalty</li> <li>If necessary, unsummon the Pet of the killed L2PcInstance</li> <li>Manage Karma gain for
	 * attacker and Karam loss for the killed L2PcInstance</li> <li>If the killed L2PcInstance has Karma, manage Drop Item</li> <li>Kill the L2PcInstance</li>
	 * @param killer
	 */
	@Override
	public boolean doDie(L2Character killer)
	{
		// Kill the L2PcInstance
		if (!super.doDie(killer))
		{
			return false;
		}
		
		if (isMounted())
		{
			stopFeed();
		}
		synchronized (this)
		{
			if (isFakeDeath())
			{
				stopFakeDeath(true);
			}
		}
		
		if (killer != null)
		{
			final L2PcInstance pk = killer.getActingPlayer();
			if (pk != null)
			{
				pk.getEvents().onPvPKill(this);
				
				TvTEvent.onKill(killer, this);
				
				if (L2Event.isParticipant(pk))
				{
					pk.getEventStatus().kills.add(this);
				}
				
				// announce pvp/pk
				if (Config.ANNOUNCE_PK_PVP && !pk.isGM())
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
						{
							Announcements.getInstance().announceToAll(msg);
						}
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
						{
							Announcements.getInstance().announceToAll(msg);
						}
					}
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
				// TODO: Fort siege during TW??
				if (TerritoryWarManager.getInstance().isTWInProgress())
				{
					TerritoryWarManager.getInstance().dropCombatFlag(this, true, false);
				}
				else
				{
					Fort fort = FortManager.getInstance().getFort(this);
					if (fort != null)
					{
						FortSiegeManager.getInstance().dropCombatFlag(this, fort.getFortId());
					}
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
				if ((pk == null) || !pk.isCursedWeaponEquipped())
				{
					onDieDropItem(killer); // Check if any item should be dropped
					
					if (!(isInsideZone(ZoneId.PVP) && !isInsideZone(ZoneId.SIEGE)))
					{
						if ((pk != null) && (pk.getClan() != null) && (getClan() != null) && !isAcademyMember() && !(pk.isAcademyMember()))
						{
							if ((_clan.isAtWarWith(pk.getClanId()) && pk.getClan().isAtWarWith(_clan.getClanId())) || (isInSiege() && pk.isInSiege()))
							{
								if (AntiFeedManager.getInstance().check(killer, this))
								{
									// when your reputation score is 0 or below, the other clan cannot acquire any reputation points
									if (getClan().getReputationScore() > 0)
									{
										pk.getClan().addReputationScore(Config.REPUTATION_SCORE_PER_KILL, false);
									}
									// when the opposing sides reputation score is 0 or below, your clans reputation score does not decrease
									if (pk.getClan().getReputationScore() > 0)
									{
										_clan.takeReputationScore(Config.REPUTATION_SCORE_PER_KILL, false);
									}
								}
							}
						}
					}
					
					if (Config.ALT_GAME_DELEVEL)
					{
						// If player is Lucky shouldn't get penalized.
						if (!isLucky())
						{
							// Reduce the Experience of the L2PcInstance in function of the calculated Death Penalty
							// NOTE: deathPenalty +- Exp will update karma
							// Penalty is lower if the player is at war with the pk (war has to be declared)
							final boolean siegeNpc = (killer instanceof L2DefenderInstance) || (killer instanceof L2FortCommanderInstance);
							final boolean atWar = (pk != null) && (getClan() != null) && (getClan().isAtWarWith(pk.getClanId()));
							deathPenalty(atWar, (pk != null), siegeNpc);
						}
					}
				}
			}
		}
		
		// Unsummon Cubics
		if (!_cubics.isEmpty())
		{
			for (L2CubicInstance cubic : _cubics)
			{
				cubic.stopAction();
				cubic.cancelDisappear();
			}
			_cubics.clear();
		}
		
		if (_fusionSkill != null)
		{
			abortCast();
		}
		
		for (L2Character character : getKnownList().getKnownCharacters())
		{
			if ((character.getFusionSkill() != null) && (character.getFusionSkill().getTarget() == this))
			{
				character.abortCast();
			}
		}
		
		if (isInParty() && getParty().isInDimensionalRift())
		{
			getParty().getDimensionalRift().getDeadMemberList().add(this);
		}
		
		if (getAgathionId() != 0)
		{
			setAgathionId(0);
		}
		
		// calculate death penalty buff
		calculateDeathPenaltyBuffLevel(killer);
		
		stopRentPet();
		stopWaterTask();
		
		AntiFeedManager.getInstance().setLastDeathTime(getObjectId());
		
		if (isPhoenixBlessed() || (isAffected(EffectFlag.CHARM_OF_COURAGE) && isInSiege()))
		{
			reviveRequest(this, null, false);
		}
		return true;
	}
	
	private void onDieDropItem(L2Character killer)
	{
		if (L2Event.isParticipant(this) || (killer == null))
		{
			return;
		}
		
		L2PcInstance pk = killer.getActingPlayer();
		if ((getKarma() <= 0) && (pk != null) && (pk.getClan() != null) && (getClan() != null) && (pk.getClan().isAtWarWith(getClanId())
		// || getClan().isAtWarWith(((L2PcInstance)killer).getClanId())
		))
		{
			return;
		}
		
		if ((!isInsideZone(ZoneId.PVP) || (pk == null)) && (!isGM() || Config.KARMA_DROP_GM))
		{
			boolean isKarmaDrop = false;
			boolean isKillerNpc = (killer instanceof L2Npc);
			int pkLimit = Config.KARMA_PK_LIMIT;
			
			int dropEquip = 0;
			int dropEquipWeapon = 0;
			int dropItem = 0;
			int dropLimit = 0;
			int dropPercent = 0;
			
			if ((getKarma() > 0) && (getPkKills() >= pkLimit))
			{
				isKarmaDrop = true;
				dropPercent = Config.KARMA_RATE_DROP;
				dropEquip = Config.KARMA_RATE_DROP_EQUIP;
				dropEquipWeapon = Config.KARMA_RATE_DROP_EQUIP_WEAPON;
				dropItem = Config.KARMA_RATE_DROP_ITEM;
				dropLimit = Config.KARMA_DROP_LIMIT;
			}
			else if (isKillerNpc && (getLevel() > 4) && !isFestivalParticipant())
			{
				dropPercent = Config.PLAYER_RATE_DROP;
				dropEquip = Config.PLAYER_RATE_DROP_EQUIP;
				dropEquipWeapon = Config.PLAYER_RATE_DROP_EQUIP_WEAPON;
				dropItem = Config.PLAYER_RATE_DROP_ITEM;
				dropLimit = Config.PLAYER_DROP_LIMIT;
			}
			
			if ((dropPercent > 0) && (Rnd.get(100) < dropPercent))
			{
				int dropCount = 0;
				
				int itemDropPercent = 0;
				
				for (L2ItemInstance itemDrop : getInventory().getItems())
				{
					// Don't drop
					if (itemDrop.isShadowItem() || // Dont drop Shadow Items
					itemDrop.isTimeLimitedItem() || // Dont drop Time Limited Items
					!itemDrop.isDropable() || (itemDrop.getId() == PcInventory.ADENA_ID) || // Adena
					(itemDrop.getItem().getType2() == L2Item.TYPE2_QUEST) || // Quest Items
					(hasSummon() && (getSummon().getControlObjectId() == itemDrop.getId())) || // Control Item of active pet
					(Arrays.binarySearch(Config.KARMA_LIST_NONDROPPABLE_ITEMS, itemDrop.getId()) >= 0) || // Item listed in the non droppable item list
					(Arrays.binarySearch(Config.KARMA_LIST_NONDROPPABLE_PET_ITEMS, itemDrop.getId()) >= 0 // Item listed in the non droppable pet item list
					))
					{
						continue;
					}
					
					if (itemDrop.isEquipped())
					{
						// Set proper chance according to Item type of equipped Item
						itemDropPercent = itemDrop.getItem().getType2() == L2Item.TYPE2_WEAPON ? dropEquipWeapon : dropEquip;
						getInventory().unEquipItemInSlot(itemDrop.getLocationSlot());
					}
					else
					{
						itemDropPercent = dropItem; // Item in inventory
					}
					
					// NOTE: Each time an item is dropped, the chance of another item being dropped gets lesser (dropCount * 2)
					if (Rnd.get(100) < itemDropPercent)
					{
						dropItem("DieDrop", itemDrop, killer, true);
						
						if (isKarmaDrop)
						{
							_log.warning(getName() + " has karma and dropped id = " + itemDrop.getId() + ", count = " + itemDrop.getCount());
						}
						else
						{
							_log.warning(getName() + " dropped id = " + itemDrop.getId() + ", count = " + itemDrop.getCount());
						}
						
						if (++dropCount >= dropLimit)
						{
							break;
						}
					}
				}
			}
		}
	}
	
	public void onKillUpdatePvPKarma(L2Character target)
	{
		if ((target == null) || !target.isPlayable())
		{
			return;
		}
		
		L2PcInstance targetPlayer = target.getActingPlayer();
		if ((targetPlayer == null) || (targetPlayer == this))
		{
			return;
		}
		
		if (isCursedWeaponEquipped() && target.isPlayer())
		{
			CursedWeaponsManager.getInstance().increaseKills(_cursedWeaponEquippedId);
			return;
		}
		
		// If in duel and you kill (only can kill l2summon), do nothing
		if (isInDuel() && targetPlayer.isInDuel())
		{
			return;
		}
		
		// If in Arena, do nothing
		if (isInsideZone(ZoneId.PVP) || targetPlayer.isInsideZone(ZoneId.PVP))
		{
			return;
		}
		
		// Check if it's pvp
		if ((checkIfPvP(target) && (targetPlayer.getPvpFlag() != 0)) || (isInsideZone(ZoneId.PVP) && targetPlayer.isInsideZone(ZoneId.PVP)))
		{
			increasePvpKills(target);
		}
		else
		{
			// Target player doesn't have pvp flag set
			// check about wars
			if ((targetPlayer.getClan() != null) && (getClan() != null) && getClan().isAtWarWith(targetPlayer.getClanId()) && targetPlayer.getClan().isAtWarWith(getClanId()) && (targetPlayer.getPledgeType() != L2Clan.SUBUNIT_ACADEMY) && (getPledgeType() != L2Clan.SUBUNIT_ACADEMY))
			{
				// 'Both way war' -> 'PvP Kill'
				increasePvpKills(target);
				return;
			}
			
			// 'No war' or 'One way war' -> 'Normal PK'
			if (targetPlayer.getKarma() > 0) // Target player has karma
			{
				if (Config.KARMA_AWARD_PK_KILL)
				{
					increasePvpKills(target);
				}
			}
			else if (targetPlayer.getPvpFlag() == 0) // Target player doesn't have karma
			{
				increasePkKillsAndKarma(target);
				checkItemRestriction(); // Unequip adventurer items
			}
		}
	}
	
	/**
	 * Increase the pvp kills count and send the info to the player
	 * @param target
	 */
	public void increasePvpKills(L2Character target)
	{
		if ((target instanceof L2PcInstance) && AntiFeedManager.getInstance().check(this, target))
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
		// Only playables can increase karma/pk
		if ((target == null) || !target.isPlayable())
		{
			return;
		}
		
		// PK Points are increased only if you kill a player.
		if (target.isPlayer())
		{
			setPkKills(getPkKills() + 1);
		}
		
		// Calculate new karma.
		setKarma(getKarma() + Formulas.calculateKarmaGain(getPkKills(), target.isSummon()));
		
		// Update player's UI.
		sendPacket(new UserInfo(this));
		sendPacket(new ExBrExtraUserInfo(this));
	}
	
	public void updatePvPStatus()
	{
		if (isInsideZone(ZoneId.PVP))
		{
			return;
		}
		setPvpFlagLasts(System.currentTimeMillis() + Config.PVP_NORMAL_TIME);
		
		if (getPvpFlag() == 0)
		{
			startPvPFlag();
		}
	}
	
	public void updatePvPStatus(L2Character target)
	{
		L2PcInstance player_target = target.getActingPlayer();
		
		if (player_target == null)
		{
			return;
		}
		
		if ((isInDuel() && (player_target.getDuelId() == getDuelId())))
		{
			return;
		}
		if ((!isInsideZone(ZoneId.PVP) || !player_target.isInsideZone(ZoneId.PVP)) && (player_target.getKarma() == 0))
		{
			if (checkIfPvP(player_target))
			{
				setPvpFlagLasts(System.currentTimeMillis() + Config.PVP_PVP_TIME);
			}
			else
			{
				setPvpFlagLasts(System.currentTimeMillis() + Config.PVP_NORMAL_TIME);
			}
			if (getPvpFlag() == 0)
			{
				startPvPFlag();
			}
		}
	}
	
	/**
	 * @return {@code true} if player has Lucky effect and is level 9 or less
	 */
	public boolean isLucky()
	{
		return ((getLevel() <= 9) && (getFirstPassiveEffect(L2EffectType.LUCKY) != null));
	}
	
	/**
	 * Restore the specified % of experience this L2PcInstance has lost and sends a Server->Client StatusUpdate packet.
	 * @param restorePercent
	 */
	public void restoreExp(double restorePercent)
	{
		if (getExpBeforeDeath() > 0)
		{
			// Restore the specified % of lost experience.
			getStat().addExp(Math.round(((getExpBeforeDeath() - getExp()) * restorePercent) / 100));
			setExpBeforeDeath(0);
		}
	}
	
	/**
	 * Reduce the Experience (and level if necessary) of the L2PcInstance in function of the calculated Death Penalty. <B><U> Actions</U> :</B> <li>Calculate the Experience loss</li> <li>Set the value of _expBeforeDeath</li> <li>Set the new Experience value of the L2PcInstance and Decrease its level
	 * if necessary</li> <li>Send a Server->Client StatusUpdate packet with its new Experience</li>
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
		
		// The death steal you some Exp
		double percentLost = Config.PLAYER_XP_PERCENT_LOST[getLevel()] * clan_luck_modificator;
		
		if (getKarma() > 0)
		{
			percentLost *= Config.RATE_KARMA_EXP_LOST;
		}
		
		if (isFestivalParticipant() || atwar)
		{
			percentLost /= 4.0;
		}
		
		// Calculate the Experience loss
		long lostExp = 0;
		if (!L2Event.isParticipant(this))
		{
			if (lvl < ExperienceTable.getInstance().getMaxLevel())
			{
				lostExp = Math.round(((getStat().getExpForLevel(lvl + 1) - getStat().getExpForLevel(lvl)) * percentLost) / 100);
			}
			else
			{
				lostExp = Math.round(((getStat().getExpForLevel(ExperienceTable.getInstance().getMaxLevel()) - getStat().getExpForLevel(ExperienceTable.getInstance().getMaxLevel() - 1)) * percentLost) / 100);
			}
		}
		
		// Get the Experience before applying penalty
		setExpBeforeDeath(getExp());
		
		// No xp loss inside pvp zone unless
		// - it's a siege zone and you're NOT participating
		// - you're killed by a non-pc whose not belong to the siege
		if (isInsideZone(ZoneId.PVP))
		{
			// No xp loss for siege participants inside siege zone
			if (isInsideZone(ZoneId.SIEGE))
			{
				if (isInSiege() && (killed_by_pc || killed_by_siege_npc))
				{
					lostExp = 0;
				}
			}
			else if (killed_by_pc)
			{
				lostExp = 0;
			}
		}
		
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
	 * Stop the HP/MP/CP Regeneration task. <B><U> Actions</U> :</B> <li>Set the RegenActive flag to False</li> <li>Stop the HP/MP/CP Regeneration task</li>
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
		stopSoulTask();
		stopChargeTask();
		stopFameTask();
		stopVitalityTask();
		stopRecoBonusTask();
		stopRecoGiveTask();
	}
	
	@Override
	public L2Summon getSummon()
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
	public L2TrapInstance getTrap()
	{
		return _trap;
	}
	
	/**
	 * Set the L2Summon of the L2PcInstance.
	 * @param summon
	 */
	public void setPet(L2Summon summon)
	{
		_summon = summon;
	}
	
	/**
	 * Set the L2Decoy of the L2PcInstance.
	 * @param decoy
	 */
	public void setDecoy(L2Decoy decoy)
	{
		_decoy = decoy;
	}
	
	/**
	 * Set the L2Trap of this L2PcInstance
	 * @param trap
	 */
	public void setTrap(L2TrapInstance trap)
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
	 * Set the L2Summon of the L2PcInstance.
	 * @param tamedBeast
	 */
	public void addTrainedBeast(L2TamedBeastInstance tamedBeast)
	{
		if (_tamedBeast == null)
		{
			_tamedBeast = new FastList<>();
		}
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
	 * Set the L2PcInstance requester of a transaction (ex : FriendInvite, JoinAlly, JoinParty...).
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
			if (requester.isRequestExpired() && (_activeTradeList == null))
			{
				_activeRequester = null;
			}
		}
		return _activeRequester;
	}
	
	/**
	 * @return True if a transaction is in progress.
	 */
	public boolean isProcessingRequest()
	{
		return (getActiveRequester() != null) || (_requestExpireTime > GameTimeController.getInstance().getGameTicks());
	}
	
	/**
	 * @return True if a transaction is in progress.
	 */
	public boolean isProcessingTransaction()
	{
		return (getActiveRequester() != null) || (_activeTradeList != null) || (_requestExpireTime > GameTimeController.getInstance().getGameTicks());
	}
	
	/**
	 * Select the Warehouse to be used in next activity.
	 * @param partner
	 */
	public void onTransactionRequest(L2PcInstance partner)
	{
		_requestExpireTime = GameTimeController.getInstance().getGameTicks() + (REQUEST_TIMEOUT * GameTimeController.TICKS_PER_SECOND);
		partner.setActiveRequester(this);
	}
	
	/**
	 * Return true if last request is expired.
	 * @return
	 */
	public boolean isRequestExpired()
	{
		return !(_requestExpireTime > GameTimeController.getInstance().getGameTicks());
	}
	
	/**
	 * Select the Warehouse to be used in next activity.
	 */
	public void onTransactionResponse()
	{
		_requestExpireTime = 0;
	}
	
	/**
	 * Select the Warehouse to be used in next activity.
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
	 * Select the TradeList to be used in next activity.
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
		sendPacket(TradeOtherDone.STATIC_PACKET);
	}
	
	public void onTradeCancel(L2PcInstance partner)
	{
		if (_activeTradeList == null)
		{
			return;
		}
		
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
		{
			sendPacket(SystemMessageId.TRADE_SUCCESSFUL);
		}
	}
	
	public void startTrade(L2PcInstance partner)
	{
		onTradeStart(partner);
		partner.onTradeStart(this);
	}
	
	public void cancelActiveTrade()
	{
		if (_activeTradeList == null)
		{
			return;
		}
		
		L2PcInstance partner = _activeTradeList.getPartner();
		if (partner != null)
		{
			partner.onTradeCancel(this);
		}
		onTradeCancel(this);
	}
	
	public boolean hasManufactureShop()
	{
		return (_manufactureItems != null) && !_manufactureItems.isEmpty();
	}
	
	/**
	 * Get the manufacture items map of this player.
	 * @return the the manufacture items map
	 */
	public Map<Integer, L2ManufactureItem> getManufactureItems()
	{
		if (_manufactureItems == null)
		{
			synchronized (this)
			{
				if (_manufactureItems == null)
				{
					_manufactureItems = Collections.synchronizedMap(new LinkedHashMap<Integer, L2ManufactureItem>());
				}
			}
		}
		return _manufactureItems;
	}
	
	/**
	 * Get the store name, if any.
	 * @return the store name
	 */
	public String getStoreName()
	{
		return _storeName;
	}
	
	/**
	 * Set the store name.
	 * @param name the store name to set
	 */
	public void setStoreName(String name)
	{
		_storeName = name == null ? "" : name;
	}
	
	/**
	 * @return the _buyList object of the L2PcInstance.
	 */
	public TradeList getSellList()
	{
		if (_sellList == null)
		{
			_sellList = new TradeList(this);
		}
		return _sellList;
	}
	
	/**
	 * @return the _buyList object of the L2PcInstance.
	 */
	public TradeList getBuyList()
	{
		if (_buyList == null)
		{
			_buyList = new TradeList(this);
		}
		return _buyList;
	}
	
	/**
	 * Set the Private Store type of the L2PcInstance. <B><U> Values </U> :</B> <li>0 : STORE_PRIVATE_NONE</li> <li>1 : STORE_PRIVATE_SELL</li> <li>2 : sellmanage</li><BR>
	 * <li>3 : STORE_PRIVATE_BUY</li><BR>
	 * <li>4 : buymanage</li><BR>
	 * <li>5 : STORE_PRIVATE_MANUFACTURE</li><BR>
	 * @param type
	 */
	public void setPrivateStoreType(int type)
	{
		_privatestore = type;
		
		if (Config.OFFLINE_DISCONNECT_FINISHED && (_privatestore == STORE_PRIVATE_NONE) && ((getClient() == null) || getClient().isDetached()))
		{
			deleteMe();
		}
	}
	
	/**
	 * <B><U> Values </U> :</B> <li>0 : STORE_PRIVATE_NONE</li> <li>1 : STORE_PRIVATE_SELL</li> <li>2 : sellmanage</li><BR>
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
	 * Set the _clan object, _clanId, _clanLeader Flag and title of the L2PcInstance.
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
			{
				CommunityServerThread.getInstance().sendPacket(new WorldInfo(this, null, WorldInfo.TYPE_UPDATE_PLAYER_DATA));
			}
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
		{
			CommunityServerThread.getInstance().sendPacket(new WorldInfo(this, null, WorldInfo.TYPE_UPDATE_PLAYER_DATA));
		}
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
	 * Reduce the number of arrows/bolts owned by the L2PcInstance and send it Server->Client Packet InventoryUpdate or ItemList (to unequip if the last arrow was consummed).
	 */
	@Override
	protected void reduceArrowCount(boolean bolts)
	{
		L2ItemInstance arrows = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		
		if (arrows == null)
		{
			getInventory().unEquipItemInSlot(Inventory.PAPERDOLL_LHAND);
			if (bolts)
			{
				_boltItem = null;
			}
			else
			{
				_arrowItem = null;
			}
			sendPacket(new ItemList(this, false));
			return;
		}
		
		// Adjust item quantity
		if (arrows.getCount() > 1)
		{
			synchronized (arrows)
			{
				arrows.changeCountWithoutTrace(-1, this, null);
				arrows.setLastChange(L2ItemInstance.MODIFIED);
				
				// could do also without saving, but let's save approx 1 of 10
				if ((GameTimeController.getInstance().getGameTicks() % 10) == 0)
				{
					arrows.updateDatabase();
				}
				_inventory.refreshWeight();
			}
		}
		else
		{
			// Destroy entire item and save to database
			_inventory.destroyItem("Consume", arrows, this, null);
			
			getInventory().unEquipItemInSlot(Inventory.PAPERDOLL_LHAND);
			if (bolts)
			{
				_boltItem = null;
			}
			else
			{
				_arrowItem = null;
			}
			
			sendPacket(new ItemList(this, false));
			return;
		}
		
		if (!Config.FORCE_INVENTORY_UPDATE)
		{
			InventoryUpdate iu = new InventoryUpdate();
			iu.addModifiedItem(arrows);
			sendPacket(iu);
		}
		else
		{
			sendPacket(new ItemList(this, false));
		}
	}
	
	/**
	 * Equip arrows needed in left hand and send a Server->Client packet ItemList to the L2PcINstance then return True.
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
	 * Equip bolts needed in left hand and send a Server->Client packet ItemList to the L2PcINstance then return True.
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
	 * @return {@code true} if the player was disarmed or doesn't have a weapon to disarm, {@code false} otherwise.
	 */
	public boolean disarmWeapons()
	{
		// If there is no weapon to disarm then return true.
		final L2ItemInstance wpn = getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
		if (wpn == null)
		{
			return true;
		}
		
		// Don't allow disarming a cursed weapon
		if (isCursedWeaponEquipped())
		{
			return false;
		}
		
		// Don't allow disarming a Combat Flag or Territory Ward.
		if (isCombatFlagEquipped())
		{
			return false;
		}
		
		// Don't allow disarming if the weapon is force equip.
		if (wpn.getWeaponItem().isForceEquip())
		{
			return false;
		}
		
		final L2ItemInstance[] unequiped = getInventory().unEquipItemInBodySlotAndRecord(wpn.getItem().getBodyPart());
		final InventoryUpdate iu = new InventoryUpdate();
		for (L2ItemInstance itm : unequiped)
		{
			iu.addModifiedItem(itm);
		}
		
		sendPacket(iu);
		abortAttack();
		broadcastUserInfo();
		
		// This can be 0 if the user pressed the right mousebutton twice very fast.
		if (unequiped.length > 0)
		{
			final SystemMessage sm;
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
		return true;
	}
	
	/**
	 * Disarm the player's shield.
	 * @return {@code true}.
	 */
	public boolean disarmShield()
	{
		L2ItemInstance sld = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		if (sld != null)
		{
			L2ItemInstance[] unequiped = getInventory().unEquipItemInBodySlotAndRecord(sld.getItem().getBodyPart());
			InventoryUpdate iu = new InventoryUpdate();
			for (L2ItemInstance itm : unequiped)
			{
				iu.addModifiedItem(itm);
			}
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
		if (!disarmWeapons() || !disarmShield() || isTransformed())
		{
			return false;
		}
		
		stopAllToggles();
		setMount(pet.getId(), pet.getLevel());
		setMountObjectID(pet.getControlObjectId());
		clearPetData();
		startFeed(pet.getId());
		broadcastPacket(new Ride(this));
		
		// Notify self and others about speed change
		broadcastUserInfo();
		
		pet.unSummon(this);
		return true;
	}
	
	public boolean mount(int npcId, int controlItemObjId, boolean useFood)
	{
		if (!disarmWeapons() || !disarmShield() || isTransformed())
		{
			return false;
		}
		
		stopAllToggles();
		setMount(npcId, getLevel());
		clearPetData();
		setMountObjectID(controlItemObjId);
		broadcastPacket(new Ride(this));
		
		// Notify self and others about speed change
		broadcastUserInfo();
		if (useFood)
		{
			startFeed(npcId);
		}
		return true;
	}
	
	public boolean mountPlayer(L2Summon pet)
	{
		if ((pet != null) && pet.isMountable() && !isMounted() && !isBetrayed())
		{
			if (isDead())
			{
				// A strider cannot be ridden when dead
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(SystemMessageId.STRIDER_CANT_BE_RIDDEN_WHILE_DEAD);
				return false;
			}
			else if (pet.isDead())
			{
				// A dead strider cannot be ridden.
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(SystemMessageId.DEAD_STRIDER_CANT_BE_RIDDEN);
				return false;
			}
			else if (pet.isInCombat() || pet.isRooted())
			{
				// A strider in battle cannot be ridden
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(SystemMessageId.STRIDER_IN_BATLLE_CANT_BE_RIDDEN);
				return false;
				
			}
			else if (isInCombat())
			{
				// A strider cannot be ridden while in battle
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(SystemMessageId.STRIDER_CANT_BE_RIDDEN_WHILE_IN_BATTLE);
				return false;
			}
			else if (isSitting())
			{
				// A strider can be ridden only when standing
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(SystemMessageId.STRIDER_CAN_BE_RIDDEN_ONLY_WHILE_STANDING);
				return false;
			}
			else if (isFishing())
			{
				// You can't mount, dismount, break and drop items while fishing
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(SystemMessageId.CANNOT_DO_WHILE_FISHING_2);
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
				// FIXME: Wrong Message
				sendMessage("You cannot mount a steed while holding a flag.");
				return false;
			}
			else if (pet.isHungry())
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(SystemMessageId.HUNGRY_STRIDER_NOT_MOUNT);
				return false;
			}
			else if (!Util.checkIfInRange(200, this, pet, true))
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(SystemMessageId.TOO_FAR_AWAY_FROM_FENRIR_TO_MOUNT);
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
			if ((getMountType() == MountType.WYVERN) && isInsideZone(ZoneId.NO_LANDING))
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(SystemMessageId.NO_DISMOUNT_HERE);
				return false;
			}
			else if (isHungry())
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(SystemMessageId.HUNGRY_STRIDER_NOT_MOUNT);
				return false;
			}
			else
			{
				dismount();
			}
		}
		return true;
	}
	
	public boolean dismount()
	{
		boolean wasFlying = isFlying();
		
		sendPacket(new SetupGauge(3, 0, 0));
		int petId = _mountNpcId;
		setMount(0, 0);
		stopFeed();
		clearPetData();
		if (wasFlying)
		{
			removeSkill(SkillTable.FrequentSkill.WYVERN_BREATH.getSkill());
		}
		broadcastPacket(new Ride(this));
		setMountObjectID(0);
		storePetFood(petId);
		// Notify self and others about speed change
		broadcastUserInfo();
		return true;
	}
	
	public void setUptime(long time)
	{
		_uptime = time;
	}
	
	public long getUptime()
	{
		return System.currentTimeMillis() - _uptime;
	}
	
	/**
	 * Return True if the L2PcInstance is invulnerable.
	 */
	@Override
	public boolean isInvul()
	{
		return super.isInvul() || (_teleportProtectEndTime > GameTimeController.getInstance().getGameTicks());
	}
	
	/**
	 * Return True if the L2PcInstance has a Party in progress.
	 */
	@Override
	public boolean isInParty()
	{
		return _party != null;
	}
	
	/**
	 * Set the _party object of the L2PcInstance (without joining it).
	 * @param party
	 */
	public void setParty(L2Party party)
	{
		_party = party;
	}
	
	/**
	 * Set the _party object of the L2PcInstance AND join it.
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
	 * Manage the Leave Party task of the L2PcInstance.
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
	 * Return the _party object of the L2PcInstance.
	 */
	@Override
	public L2Party getParty()
	{
		return _party;
	}
	
	/**
	 * Return True if the L2PcInstance is a GM.
	 */
	@Override
	public boolean isGM()
	{
		return getAccessLevel().isGm();
	}
	
	/**
	 * Set the _accessLevel of the L2PcInstance.
	 * @param level
	 */
	public void setAccessLevel(int level)
	{
		_accessLevel = AdminTable.getInstance().getAccessLevel(level);
		
		getAppearance().setNameColor(_accessLevel.getNameColor());
		getAppearance().setTitleColor(_accessLevel.getTitleColor());
		broadcastUserInfo();
		
		CharNameTable.getInstance().addName(this);
		
		if (!AdminTable.getInstance().hasAccessLevel(level))
		{
			_log.warning("Tryed to set unregistered access level " + level + " for " + toString() + ". Setting access level without privileges!");
		}
		else if (level > 0)
		{
			_log.warning(_accessLevel.getName() + " access level set for character " + getName() + "! Just a warning to be careful ;)");
		}
	}
	
	public void setAccountAccesslevel(int level)
	{
		LoginServerThread.getInstance().sendAccessLevel(getAccountName(), level);
	}
	
	/**
	 * @return the _accessLevel of the L2PcInstance.
	 */
	@Override
	public L2AccessLevel getAccessLevel()
	{
		if (Config.EVERYBODY_HAS_ADMIN_RIGHTS)
		{
			return AdminTable.getInstance().getMasterAccessLevel();
		}
		else if (_accessLevel == null)
		{
			setAccessLevel(0);
		}
		
		return _accessLevel;
	}
	
	/**
	 * Update Stats of the L2PcInstance client side by sending Server->Client packet UserInfo/StatusUpdate to this L2PcInstance and CharInfo/StatusUpdate to all L2PcInstance in its _KnownPlayers (broadcast).
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
		if (broadcastType == 2)
		{
			broadcastUserInfo();
		}
	}
	
	/**
	 * Send a Server->Client StatusUpdate packet with Karma and PvP Flag to the L2PcInstance and all L2PcInstance to inform (broadcast).
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
			if (hasSummon())
			{
				player.sendPacket(new RelationChanged(getSummon(), getRelation(player), isAutoAttackable(player)));
			}
		}
	}
	
	/**
	 * Send a Server->Client StatusUpdate packet with Karma to the L2PcInstance and all L2PcInstance to inform (broadcast).
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
			if (hasSummon())
			{
				player.sendPacket(new RelationChanged(getSummon(), getRelation(player), isAutoAttackable(player)));
			}
		}
	}
	
	/**
	 * Set the online Flag to True or False and update the characters table of the database with online status and lastAccess (called when login and logout).
	 * @param isOnline
	 * @param updateInDb
	 */
	public void setOnlineStatus(boolean isOnline, boolean updateInDb)
	{
		if (_isOnline != isOnline)
		{
			_isOnline = isOnline;
		}
		
		// Update the characters table of the database with online status and lastAccess (called when login and logout)
		if (updateInDb)
		{
			updateOnlineStatus();
		}
	}
	
	public void setIsIn7sDungeon(boolean isIn7sDungeon)
	{
		_isIn7sDungeon = isIn7sDungeon;
	}
	
	/**
	 * Update the characters table of the database with online status and lastAccess of this L2PcInstance (called when login and logout).
	 */
	public void updateOnlineStatus()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE characters SET online=?, lastAccess=? WHERE charId=?"))
		{
			statement.setInt(1, isOnlineInt());
			statement.setLong(2, System.currentTimeMillis());
			statement.setInt(3, getObjectId());
			statement.execute();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Failed updating character online status.", e);
		}
	}
	
	/**
	 * Create a new player in the characters table of the database.
	 * @return
	 */
	private boolean createDb()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(INSERT_CHARACTER))
		{
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
			statement.setInt(14, getAppearance().getSex() ? 1 : 0);
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
			statement.setInt(35, isNoble() ? 1 : 0);
			statement.setLong(36, 0);
			statement.setDate(37, new Date(getCreateDate().getTimeInMillis()));
			statement.executeUpdate();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Could not insert char data: " + e.getMessage(), e);
			return false;
		}
		return true;
	}
	
	/**
	 * Retrieve a L2PcInstance from the characters table of the database and add it in _allObjects of the L2world. <B><U> Actions</U> :</B> <li>Retrieve the L2PcInstance from the characters table of the database</li> <li>Add the L2PcInstance object in _allObjects</li> <li>Set the x,y,z position of
	 * the L2PcInstance and make it invisible</li> <li>Update the overloaded status of the L2PcInstance</li>
	 * @param objectId Identifier of the object to initialized
	 * @return The L2PcInstance loaded from the database
	 */
	private static L2PcInstance restore(int objectId)
	{
		L2PcInstance player = null;
		double currentCp = 0;
		double currentHp = 0;
		double currentMp = 0;
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(RESTORE_CHARACTER))
		{
			// Retrieve the L2PcInstance from the characters table of the database
			statement.setInt(1, objectId);
			try (ResultSet rset = statement.executeQuery())
			{
				if (rset.next())
				{
					final int activeClassId = rset.getInt("classid");
					final boolean female = rset.getInt("sex") != Sex.MALE;
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
					player.setNoble(rset.getInt("nobless") == 1);
					
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
					
					int clanId = rset.getInt("clanid");
					player.setPowerGrade(rset.getInt("power_grade"));
					player.setPledgeType(rset.getInt("subpledge"));
					// player.setApprentice(rset.getInt("apprentice"));
					
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
						player.setPledgeClass(L2ClanMember.calculatePledgeClass(player));
					}
					else
					{
						if (player.isNoble())
						{
							player.setPledgeClass(5);
						}
						
						if (player.isHero())
						{
							player.setPledgeClass(8);
						}
						
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
					try
					{
						player.setBaseClass(rset.getInt("base_class"));
					}
					catch (Exception e)
					{
						// TODO: Should this be logged?
						player.setBaseClass(activeClassId);
					}
					
					// Restore Subclass Data (cannot be done earlier in function)
					if (restoreSubClassData(player))
					{
						if (activeClassId != player.getBaseClass())
						{
							for (SubClass subClass : player.getSubClasses().values())
							{
								if (subClass.getClassId() == activeClassId)
								{
									player._classIndex = subClass.getClassIndex();
								}
							}
						}
					}
					if ((player.getClassIndex() == 0) && (activeClassId != player.getBaseClass()))
					{
						// Subclass in use but doesn't exist in DB -
						// a possible restart-while-modifysubclass cheat has been attempted.
						// Switching to use base class
						player.setClassId(player.getBaseClass());
						_log.warning("Player " + player.getName() + " reverted to base class. Possibly has tried a relogin exploit while subclassing.");
					}
					else
					{
						player._activeClass = activeClassId;
					}
					
					player.setApprentice(rset.getInt("apprentice"));
					player.setSponsor(rset.getInt("sponsor"));
					player.setLvlJoinedAcademy(rset.getInt("lvl_joined_academy"));
					player.setIsIn7sDungeon(rset.getInt("isin7sdungeon") == 1);
					
					CursedWeaponsManager.getInstance().checkPlayer(player);
					
					player.setDeathPenaltyBuffLevel(rset.getInt("death_penalty_level"));
					
					player.setVitalityPoints(rset.getInt("vitality_points"), true);
					
					// Set the x,y,z position of the L2PcInstance and make it invisible
					player.setXYZInvisible(rset.getInt("x"), rset.getInt("y"), rset.getInt("z"));
					
					// Set Teleport Bookmark Slot
					player.setBookMarkSlot(rset.getInt("BookmarkSlot"));
					
					// character creation Time
					player.getCreateDate().setTime(rset.getDate("createDate"));
					
					// Language
					player.setLang(rset.getString("language"));
					
					// Retrieve the name and ID of the other characters assigned to this account.
					try (PreparedStatement stmt = con.prepareStatement("SELECT charId, char_name FROM characters WHERE account_name=? AND charId<>?"))
					{
						stmt.setString(1, player._accountName);
						stmt.setInt(2, objectId);
						try (ResultSet chars = stmt.executeQuery())
						{
							while (chars.next())
							{
								player._chars.put(chars.getInt("charId"), chars.getString("char_name"));
							}
						}
					}
				}
			}
			
			if (player == null)
			{
				return null;
			}
			
			// Set Hero status if it applies
			if (Hero.getInstance().isHero(objectId))
			{
				player.setHero(true);
			}
			
			// Retrieve from the database all items of this L2PcInstance and add them to _inventory
			player.getInventory().restore();
			player.getFreight().restore();
			if (!Config.WAREHOUSE_CACHE)
			{
				player.getWarehouse();
			}
			
			// Retrieve from the database all secondary data of this L2PcInstance
			// Note that Clan, Noblesse and Hero skills are given separately and not here.
			// Retrieve from the database all skills of this L2PcInstance and add them to _skills
			player.restoreCharData();
			
			// Buff and status icons
			if (Config.STORE_SKILL_COOLTIME)
			{
				player.restoreEffects();
			}
			
			// Reward auto-get skills and all available skills if auto-learn skills is true.
			player.rewardSkills();
			
			player.restoreItemReuse();
			
			// Restore current Cp, HP and MP values
			player.setCurrentCp(currentCp);
			player.setCurrentHp(currentHp);
			player.setCurrentMp(currentMp);
			
			if (currentHp < 0.5)
			{
				player.setIsDead(true);
				player.stopHpMpRegeneration();
			}
			
			// Restore pet if exists in the world
			player.setPet(L2World.getInstance().getPet(player.getObjectId()));
			if (player.hasSummon())
			{
				player.getSummon().setOwner(player);
			}
			
			// Update the overloaded status of the L2PcInstance
			player.refreshOverloaded();
			// Update the expertise status of the L2PcInstance
			player.refreshExpertisePenalty();
			
			player.restoreFriendList();
			
			if (Config.STORE_UI_SETTINGS)
			{
				player.restoreUISettings();
			}
			
			player.restoreZoneRestartLimitTime();
			
			if (player.isGM())
			{
				final long masks = player.getVariables().getLong(COND_OVERRIDE_KEY, PcCondOverride.getAllExceptionsMask());
				player.setOverrideCond(masks);
			}
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Failed loading character.", e);
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
				ForumsBBSManager.getInstance().createNewForum(getName(), ForumsBBSManager.getInstance().getForumByName("MailRoot"), Forum.MAIL, Forum.OWNERONLY, getObjectId());
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
	 * Restores sub-class data for the L2PcInstance, used to check the current class index for the character.
	 * @param player
	 * @return
	 */
	private static boolean restoreSubClassData(L2PcInstance player)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(RESTORE_CHAR_SUBCLASSES))
		{
			statement.setInt(1, player.getObjectId());
			try (ResultSet rset = statement.executeQuery())
			{
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
			}
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Could not restore classes for " + player.getName() + ": " + e.getMessage(), e);
		}
		return true;
	}
	
	/**
	 * Restores:
	 * <ul>
	 * <li>Skills</li>
	 * <li>Macros</li>
	 * <li>Short-cuts</li>
	 * <li>Henna</li>
	 * <li>Teleport Bookmark</li>
	 * <li>Recipe Book</li>
	 * <li>Recipe Shop List (If configuration enabled)</li>
	 * <li>Premium Item List</li>
	 * <li>Pet Inventory Items</li>
	 * </ul>
	 */
	private void restoreCharData()
	{
		// Retrieve from the database all skills of this L2PcInstance and add them to _skills.
		restoreSkills();
		
		// Retrieve from the database all macroses of this L2PcInstance and add them to _macros.
		_macros.restore();
		
		// Retrieve from the database all shortCuts of this L2PcInstance and add them to _shortCuts.
		_shortCuts.restore();
		
		// Retrieve from the database all henna of this L2PcInstance and add them to _henna.
		restoreHenna();
		
		// Retrieve from the database all teleport bookmark of this L2PcInstance and add them to _tpbookmark.
		restoreTeleportBookmark();
		
		// Retrieve from the database the recipe book of this L2PcInstance.
		restoreRecipeBook(true);
		
		// Restore Recipe Shop list.
		if (Config.STORE_RECIPE_SHOPLIST)
		{
			restoreRecipeShopList();
		}
		
		// Load Premium Item List.
		loadPremiumItemList();
		
		// Restore items in pet inventory.
		restorePetInventoryItems();
	}
	
	/**
	 * Restore recipe book data for this L2PcInstance.
	 * @param loadCommon
	 */
	private void restoreRecipeBook(boolean loadCommon)
	{
		final String sql = loadCommon ? "SELECT id, type, classIndex FROM character_recipebook WHERE charId=?" : "SELECT id FROM character_recipebook WHERE charId=? AND classIndex=? AND type = 1";
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(sql))
		{
			statement.setInt(1, getObjectId());
			if (!loadCommon)
			{
				statement.setInt(2, _classIndex);
			}
			
			try (ResultSet rset = statement.executeQuery())
			{
				_dwarvenRecipeBook.clear();
				
				L2RecipeList recipe;
				RecipeData rd = RecipeData.getInstance();
				while (rset.next())
				{
					recipe = rd.getRecipeList(rset.getInt("id"));
					if (loadCommon)
					{
						if (rset.getInt(2) == 1)
						{
							if (rset.getInt(3) == _classIndex)
							{
								registerDwarvenRecipeList(recipe, false);
							}
						}
						else
						{
							registerCommonRecipeList(recipe, false);
						}
					}
					else
					{
						registerDwarvenRecipeList(recipe, false);
					}
				}
			}
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Could not restore recipe book data:" + e.getMessage(), e);
		}
	}
	
	public Map<Integer, L2PremiumItem> getPremiumItemList()
	{
		return _premiumItems;
	}
	
	private void loadPremiumItemList()
	{
		final String sql = "SELECT itemNum, itemId, itemCount, itemSender FROM character_premium_items WHERE charId=?";
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(sql))
		{
			statement.setInt(1, getObjectId());
			try (ResultSet rset = statement.executeQuery())
			{
				while (rset.next())
				{
					int itemNum = rset.getInt("itemNum");
					int itemId = rset.getInt("itemId");
					long itemCount = rset.getLong("itemCount");
					String itemSender = rset.getString("itemSender");
					_premiumItems.put(itemNum, new L2PremiumItem(itemId, itemCount, itemSender));
				}
			}
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Could not restore premium items: " + e.getMessage(), e);
		}
	}
	
	public void updatePremiumItem(int itemNum, long newcount)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE character_premium_items SET itemCount=? WHERE charId=? AND itemNum=? "))
		{
			statement.setLong(1, newcount);
			statement.setInt(2, getObjectId());
			statement.setInt(3, itemNum);
			statement.execute();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Could not update premium items: " + e.getMessage(), e);
		}
	}
	
	public void deletePremiumItem(int itemNum)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM character_premium_items WHERE charId=? AND itemNum=? "))
		{
			statement.setInt(1, getObjectId());
			statement.setInt(2, itemNum);
			statement.execute();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Could not delete premium item: " + e);
		}
	}
	
	/**
	 * Update L2PcInstance stats in the characters table of the database.
	 * @param storeActiveEffects
	 */
	public synchronized void store(boolean storeActiveEffects)
	{
		// update client coords, if these look like true
		// if (isInsideRadius(getClientX(), getClientY(), 1000, true))
		// setXYZ(getClientX(), getClientY(), getClientZ());
		
		storeCharBase();
		storeCharSub();
		storeEffect(storeActiveEffects);
		storeItemReuseDelay();
		if (Config.STORE_RECIPE_SHOPLIST)
		{
			storeRecipeShopList();
		}
		if (Config.STORE_UI_SETTINGS)
		{
			storeUISettings();
		}
		SevenSigns.getInstance().saveSevenSignsData(getObjectId());
		
		final PlayerVariables vars = getScript(PlayerVariables.class);
		if (vars != null)
		{
			vars.store();
		}
		
		final AccountVariables aVars = getScript(AccountVariables.class);
		if (aVars != null)
		{
			aVars.store();
		}
	}
	
	@Override
	public void store()
	{
		store(true);
	}
	
	private void storeCharBase()
	{
		// Get the exp, level, and sp of base class to store in base table
		long exp = getStat().getBaseExp();
		int level = getStat().getBaseLevel();
		int sp = getStat().getBaseSp();
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(UPDATE_CHARACTER))
		{
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
			statement.setInt(11, getAppearance().getSex() ? 1 : 0);
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
			{
				totalOnlineTime += (System.currentTimeMillis() - _onlineBeginTime) / 1000;
			}
			
			statement.setLong(35, totalOnlineTime);
			statement.setInt(36, getNewbie());
			statement.setInt(37, isNoble() ? 1 : 0);
			statement.setInt(38, getPowerGrade());
			statement.setInt(39, getPledgeType());
			statement.setInt(40, getLvlJoinedAcademy());
			statement.setLong(41, getApprentice());
			statement.setLong(42, getSponsor());
			statement.setLong(43, getClanJoinExpiryTime());
			statement.setLong(44, getClanCreateExpiryTime());
			statement.setString(45, getName());
			statement.setLong(46, getDeathPenaltyBuffLevel());
			statement.setInt(47, getBookMarkSlot());
			statement.setInt(48, getVitalityPoints());
			statement.setString(49, getLang());
			statement.setInt(50, getObjectId());
			
			statement.execute();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Could not store char base data: " + this + " - " + e.getMessage(), e);
		}
	}
	
	private void storeCharSub()
	{
		if (getTotalSubClasses() <= 0)
		{
			return;
		}
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(UPDATE_CHAR_SUBCLASS))
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
				statement.clearParameters();
			}
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Could not store sub class data for " + getName() + ": " + e.getMessage(), e);
		}
	}
	
	@Override
	public void storeEffect(boolean storeEffects)
	{
		if (!Config.STORE_SKILL_COOLTIME)
		{
			return;
		}
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement delete = con.prepareStatement(DELETE_SKILL_SAVE);
			PreparedStatement statement = con.prepareStatement(ADD_SKILL_SAVE);)
		{
			// Delete all current stored effects for char to avoid dupe
			delete.setInt(1, getObjectId());
			delete.setInt(2, getClassIndex());
			delete.execute();
			
			int buff_index = 0;
			final List<Integer> storedSkills = new ArrayList<>();
			
			// Store all effect data along with calulated remaining
			// reuse delays for matching skills. 'restore_type'= 0.
			if (storeEffects)
			{
				for (L2Effect effect : getAllEffects())
				{
					if (effect == null)
					{
						continue;
					}
					
					switch (effect.getEffectType())
					{
						case HEAL_OVER_TIME:
						case CPHEAL_OVER_TIME:
						case HIDE:
							continue;
					}
					
					L2Skill skill = effect.getSkill();
					if (storedSkills.contains(skill.getReuseHashCode()))
					{
						continue;
					}
					
					// Dances and songs are not kept in retail.
					if (skill.isDance() && !Config.ALT_STORE_DANCES)
					{
						continue;
					}
					
					storedSkills.add(skill.getReuseHashCode());
					
					if (effect.isInUse() && !skill.isToggle())
					{
						statement.setInt(1, getObjectId());
						statement.setInt(2, skill.getId());
						statement.setInt(3, skill.getLevel());
						statement.setInt(4, effect.getTickCount());
						statement.setInt(5, effect.getTime());
						
						if (_reuseTimeStampsSkills.containsKey(skill.getReuseHashCode()))
						{
							TimeStamp t = _reuseTimeStampsSkills.get(skill.getReuseHashCode());
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
			int hash;
			TimeStamp t;
			for (Entry<Integer, TimeStamp> ts : _reuseTimeStampsSkills.entrySet())
			{
				hash = ts.getKey();
				if (storedSkills.contains(hash))
				{
					continue;
				}
				t = ts.getValue();
				if ((t != null) && t.hasNotPassed())
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
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Could not store char effect data: ", e);
		}
	}
	
	private void storeItemReuseDelay()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps1 = con.prepareStatement(DELETE_ITEM_REUSE_SAVE);
			PreparedStatement ps2 = con.prepareStatement(ADD_ITEM_REUSE_SAVE))
		{
			ps1.setInt(1, getObjectId());
			ps1.execute();
			
			for (TimeStamp ts : _reuseTimeStampsItems.values())
			{
				if ((ts != null) && ts.hasNotPassed())
				{
					ps2.setInt(1, getObjectId());
					ps2.setInt(2, ts.getItemId());
					ps2.setInt(3, ts.getItemObjectId());
					ps2.setLong(4, ts.getReuse());
					ps2.setDouble(5, ts.getStamp());
					ps2.execute();
				}
			}
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Could not store char item reuse data: ", e);
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
		if (_isOnline && (getClient() != null))
		{
			return getClient().isDetached() ? 2 : 1;
		}
		return 0;
	}
	
	public boolean isIn7sDungeon()
	{
		return _isIn7sDungeon;
	}
	
	@Override
	public L2Skill addSkill(L2Skill newSkill)
	{
		addCustomSkill(newSkill);
		return super.addSkill(newSkill);
	}
	
	/**
	 * Add a skill to the L2PcInstance _skills and its Func objects to the calculator set of the L2PcInstance and save update in the character_skills table of the database. <B><U> Concept</U> :</B> All skills own by a L2PcInstance are identified in <B>_skills</B> <B><U> Actions</U> :</B> <li>Replace
	 * oldSkill by newSkill or Add the newSkill</li> <li>If an old skill has been replaced, remove all its Func objects of L2Character calculator set</li> <li>Add Func objects of newSkill to the calculator set of the L2Character</li>
	 * @param newSkill The L2Skill to add to the L2Character
	 * @param store
	 * @return The L2Skill replaced or null if just added a new L2Skill
	 */
	public L2Skill addSkill(L2Skill newSkill, boolean store)
	{
		// Add a skill to the L2PcInstance _skills and its Func objects to the calculator set of the L2PcInstance
		final L2Skill oldSkill = addSkill(newSkill);
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
		removeCustomSkill(skill);
		return store ? removeSkill(skill) : super.removeSkill(skill, true);
	}
	
	public L2Skill removeSkill(L2Skill skill, boolean store, boolean cancelEffect)
	{
		removeCustomSkill(skill);
		return store ? removeSkill(skill) : super.removeSkill(skill, cancelEffect);
	}
	
	/**
	 * Remove a skill from the L2Character and its Func objects from calculator set of the L2Character and save update in the character_skills table of the database. <B><U> Concept</U> :</B> All skills own by a L2Character are identified in <B>_skills</B> <B><U> Actions</U> :</B> <li>Remove the
	 * skill from the L2Character _skills</li> <li>Remove all its Func objects from the L2Character calculator set</li> <B><U> Overridden in </U> :</B> <li>L2PcInstance : Save update in the character_skills table of the database</li>
	 * @param skill The L2Skill to remove from the L2Character
	 * @return The L2Skill removed
	 */
	public L2Skill removeSkill(L2Skill skill)
	{
		removeCustomSkill(skill);
		// Remove a skill from the L2Character and its Func objects from calculator set of the L2Character
		final L2Skill oldSkill = super.removeSkill(skill, true);
		if (oldSkill != null)
		{
			try (Connection con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement(DELETE_SKILL_FROM_CHAR))
			{
				// Remove or update a L2PcInstance skill from the character_skills table of the database
				statement.setInt(1, oldSkill.getId());
				statement.setInt(2, getObjectId());
				statement.setInt(3, getClassIndex());
				statement.execute();
			}
			catch (Exception e)
			{
				_log.log(Level.WARNING, "Error could not delete skill: " + e.getMessage(), e);
			}
		}
		
		if ((getTransformationId() > 0) || isCursedWeaponEquipped())
		{
			return oldSkill;
		}
		
		final L2ShortCut[] allShortCuts = getAllShortCuts();
		for (L2ShortCut sc : allShortCuts)
		{
			if ((sc != null) && (skill != null) && (sc.getId() == skill.getId()) && (sc.getType() == L2ShortCut.TYPE_SKILL) && !((skill.getId() >= 3080) && (skill.getId() <= 3259)))
			{
				deleteShortCut(sc.getSlot(), sc.getPage());
			}
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
		{
			classIndex = newClassIndex;
		}
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			if ((oldSkill != null) && (newSkill != null))
			{
				try (PreparedStatement ps = con.prepareStatement(UPDATE_CHARACTER_SKILL_LEVEL))
				{
					ps.setInt(1, newSkill.getLevel());
					ps.setInt(2, oldSkill.getId());
					ps.setInt(3, getObjectId());
					ps.setInt(4, classIndex);
					ps.execute();
				}
			}
			else if (newSkill != null)
			{
				try (PreparedStatement ps = con.prepareStatement(ADD_NEW_SKILL))
				{
					ps.setInt(1, getObjectId());
					ps.setInt(2, newSkill.getId());
					ps.setInt(3, newSkill.getLevel());
					ps.setInt(4, classIndex);
					ps.execute();
				}
			}
			else
			{
				_log.warning("Could not store new skill, it's null!");
			}
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Error could not store char skills: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Retrieve from the database all skills of this L2PcInstance and add them to _skills.
	 */
	private void restoreSkills()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(RESTORE_SKILLS_FOR_CHAR))
		{
			// Retrieve all skills of this L2PcInstance from the database
			statement.setInt(1, getObjectId());
			statement.setInt(2, getClassIndex());
			try (ResultSet rset = statement.executeQuery())
			{
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
					addSkill(skill);
					
					if (Config.SKILL_CHECK_ENABLE && (!canOverrideCond(PcCondOverride.SKILL_CONDITIONS) || Config.SKILL_CHECK_GM))
					{
						if (!SkillTreesData.getInstance().isSkillAllowed(this, skill))
						{
							Util.handleIllegalPlayerAction(this, "Player " + getName() + " has invalid skill " + skill.getName() + " (" + skill.getId() + "/" + skill.getLevel() + "), class:" + ClassListData.getInstance().getClass(getClassId()).getClassName(), 1);
							if (Config.SKILL_CHECK_REMOVE)
							{
								removeSkill(skill);
							}
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Could not restore character " + this + " skills: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Retrieve from the database all skill effects of this L2PcInstance and add them to the player.
	 */
	@Override
	public void restoreEffects()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(RESTORE_SKILL_SAVE))
		{
			statement.setInt(1, getObjectId());
			statement.setInt(2, getClassIndex());
			try (ResultSet rset = statement.executeQuery())
			{
				while (rset.next())
				{
					int effectCount = rset.getInt("effect_count");
					int effectCurTime = rset.getInt("effect_cur_time");
					long reuseDelay = rset.getLong("reuse_delay");
					long systime = rset.getLong("systime");
					int restoreType = rset.getInt("restore_type");
					
					final L2Skill skill = SkillTable.getInstance().getInfo(rset.getInt("skill_id"), rset.getInt("skill_level"));
					if (skill == null)
					{
						continue;
					}
					
					final long remainingTime = systime - System.currentTimeMillis();
					if (remainingTime > 10)
					{
						disableSkill(skill, remainingTime);
						addTimeStamp(skill, reuseDelay, systime);
					}
					
					/**
					 * Restore Type 1 The remaning skills lost effect upon logout but were still under a high reuse delay.
					 */
					if (restoreType > 0)
					{
						continue;
					}
					
					/**
					 * Restore Type 0 These skill were still in effect on the character upon logout.<br>
					 * Some of which were self casted and might still have had a long reuse delay which also is restored.
					 */
					if (skill.hasEffects())
					{
						final Env env = new Env();
						env.setCharacter(this);
						env.setTarget(this);
						env.setSkill(skill);
						final L2Effect[] effects = new L2Effect[skill.getEffectTemplates().size()];
						int index = 0;
						for (EffectTemplate et : skill.getEffectTemplates())
						{
							L2Effect effect = et.getEffect(env);
							if (effect != null)
							{
								effect.setCount(effectCount);
								effect.setFirstTime(effectCurTime);
								effect.scheduleEffect();
								effects[index++] = effect;
							}
						}
						getEffectList().add(effects);
					}
				}
			}
			// Remove previously restored skills
			try (PreparedStatement delete = con.prepareStatement(DELETE_SKILL_SAVE))
			{
				delete.setInt(1, getObjectId());
				delete.setInt(2, getClassIndex());
				delete.executeUpdate();
			}
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Could not restore " + this + " active effect data: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Retrieve from the database all Item Reuse Time of this L2PcInstance and add them to the player.
	 */
	private void restoreItemReuse()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(RESTORE_ITEM_REUSE_SAVE);
			PreparedStatement delete = con.prepareStatement(DELETE_ITEM_REUSE_SAVE);)
		{
			statement.setInt(1, getObjectId());
			try (ResultSet rset = statement.executeQuery())
			{
				int itemId;
				@SuppressWarnings("unused")
				int itemObjId;
				long reuseDelay;
				long systime;
				boolean isInInventory;
				long remainingTime;
				while (rset.next())
				{
					itemId = rset.getInt("itemId");
					itemObjId = rset.getInt("itemObjId");
					reuseDelay = rset.getLong("reuseDelay");
					systime = rset.getLong("systime");
					isInInventory = true;
					
					// Using item Id
					L2ItemInstance item = getInventory().getItemByItemId(itemId);
					if (item == null)
					{
						item = getWarehouse().getItemByItemId(itemId);
						isInInventory = false;
					}
					
					if ((item != null) && (item.getId() == itemId) && (item.getReuseDelay() > 0))
					{
						remainingTime = systime - System.currentTimeMillis();
						// Hardcoded to 10 seconds.
						if (remainingTime > 10)
						{
							addTimeStampItem(item, reuseDelay, systime);
							
							if (isInInventory && item.isEtcItem())
							{
								final int group = item.getSharedReuseGroup();
								if (group > 0)
								{
									sendPacket(new ExUseSharedGroupItem(itemId, group, (int) remainingTime, (int) reuseDelay));
								}
							}
						}
					}
				}
			}
			
			// Delete item reuse.
			delete.setInt(1, getObjectId());
			delete.executeUpdate();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Could not restore " + this + " Item Reuse data: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Retrieve from the database all Henna of this L2PcInstance, add them to _henna and calculate stats of the L2PcInstance.
	 */
	private void restoreHenna()
	{
		for (int i = 0; i < 3; i++)
		{
			_henna[i] = null;
		}
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(RESTORE_CHAR_HENNAS))
		{
			statement.setInt(1, getObjectId());
			statement.setInt(2, getClassIndex());
			try (ResultSet rset = statement.executeQuery())
			{
				int slot;
				int symbolId;
				while (rset.next())
				{
					slot = rset.getInt("slot");
					if ((slot < 1) || (slot > 3))
					{
						continue;
					}
					
					symbolId = rset.getInt("symbol_id");
					if (symbolId == 0)
					{
						continue;
					}
					_henna[slot - 1] = HennaData.getInstance().getHenna(symbolId);
				}
			}
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Failed restoing character " + this + " hennas.", e);
		}
		
		// Calculate henna modifiers of this player.
		recalcHennaStats();
	}
	
	/**
	 * @return the number of Henna empty slot of the L2PcInstance.
	 */
	public int getHennaEmptySlots()
	{
		int totalSlots = 0;
		if (getClassId().level() == 1)
		{
			totalSlots = 2;
		}
		else
		{
			totalSlots = 3;
		}
		
		for (int i = 0; i < 3; i++)
		{
			if (_henna[i] != null)
			{
				totalSlots--;
			}
		}
		
		if (totalSlots <= 0)
		{
			return 0;
		}
		
		return totalSlots;
	}
	
	/**
	 * Remove a Henna of the L2PcInstance, save update in the character_hennas table of the database and send Server->Client HennaInfo/UserInfo packet to this L2PcInstance.
	 * @param slot
	 * @return
	 */
	public boolean removeHenna(int slot)
	{
		if (!fireHennaListeners(getHenna(slot + 1), false))
		{
			return false;
		}
		
		if ((slot < 1) || (slot > 3))
		{
			return false;
		}
		
		slot--;
		
		L2Henna henna = _henna[slot];
		if (henna == null)
		{
			return false;
		}
		
		_henna[slot] = null;
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(DELETE_CHAR_HENNA))
		{
			statement.setInt(1, getObjectId());
			statement.setInt(2, slot + 1);
			statement.setInt(3, getClassIndex());
			statement.execute();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Failed remocing character henna.", e);
		}
		
		// Calculate Henna modifiers of this L2PcInstance
		recalcHennaStats();
		
		// Send Server->Client HennaInfo packet to this L2PcInstance
		sendPacket(new HennaInfo(this));
		
		// Send Server->Client UserInfo packet to this L2PcInstance
		sendPacket(new UserInfo(this));
		sendPacket(new ExBrExtraUserInfo(this));
		// Add the recovered dyes to the player's inventory and notify them.
		getInventory().addItem("Henna", henna.getDyeItemId(), henna.getCancelCount(), this, null);
		reduceAdena("Henna", henna.getCancelFee(), this, false);
		
		final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.EARNED_S2_S1_S);
		sm.addItemName(henna.getDyeItemId());
		sm.addItemNumber(henna.getCancelCount());
		sendPacket(sm);
		sendPacket(SystemMessageId.SYMBOL_DELETED);
		return true;
	}
	
	/**
	 * Add a Henna to the L2PcInstance, save update in the character_hennas table of the database and send Server->Client HennaInfo/UserInfo packet to this L2PcInstance.
	 * @param henna the henna to add to the player.
	 * @return {@code true} if the henna is added to the player, {@code false} otherwise.
	 */
	public boolean addHenna(L2Henna henna)
	{
		if (!fireHennaListeners(henna, true))
		{
			return false;
		}
		for (int i = 0; i < 3; i++)
		{
			if (_henna[i] == null)
			{
				_henna[i] = henna;
				
				// Calculate Henna modifiers of this L2PcInstance
				recalcHennaStats();
				
				try (Connection con = L2DatabaseFactory.getInstance().getConnection();
					PreparedStatement statement = con.prepareStatement(ADD_CHAR_HENNA))
				{
					statement.setInt(1, getObjectId());
					statement.setInt(2, henna.getDyeId());
					statement.setInt(3, i + 1);
					statement.setInt(4, getClassIndex());
					statement.execute();
				}
				catch (Exception e)
				{
					_log.log(Level.SEVERE, "Failed saving character henna.", e);
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
	 * Calculate Henna modifiers of this L2PcInstance.
	 */
	private void recalcHennaStats()
	{
		_hennaINT = 0;
		_hennaSTR = 0;
		_hennaCON = 0;
		_hennaMEN = 0;
		_hennaWIT = 0;
		_hennaDEX = 0;
		
		for (L2Henna h : _henna)
		{
			if (h == null)
			{
				continue;
			}
			
			_hennaINT += ((_hennaINT + h.getStatINT()) > 5) ? 5 - _hennaINT : h.getStatINT();
			_hennaSTR += ((_hennaSTR + h.getStatSTR()) > 5) ? 5 - _hennaSTR : h.getStatSTR();
			_hennaMEN += ((_hennaMEN + h.getStatMEN()) > 5) ? 5 - _hennaMEN : h.getStatMEN();
			_hennaCON += ((_hennaCON + h.getStatCON()) > 5) ? 5 - _hennaCON : h.getStatCON();
			_hennaWIT += ((_hennaWIT + h.getStatWIT()) > 5) ? 5 - _hennaWIT : h.getStatWIT();
			_hennaDEX += ((_hennaDEX + h.getStatDEX()) > 5) ? 5 - _hennaDEX : h.getStatDEX();
		}
	}
	
	/**
	 * @param slot the character inventory henna slot.
	 * @return the Henna of this L2PcInstance corresponding to the selected slot.
	 */
	public L2Henna getHenna(int slot)
	{
		if ((slot < 1) || (slot > 3))
		{
			return null;
		}
		return _henna[slot - 1];
	}
	
	/**
	 * @return {@code true} if player has at least 1 henna symbol, {@code false} otherwise.
	 */
	public boolean hasHennas()
	{
		for (L2Henna henna : _henna)
		{
			if (henna != null)
			{
				return true;
			}
		}
		return false;
	}
	
	/**
	 * @return the henna holder for this player.
	 */
	public L2Henna[] getHennaList()
	{
		return _henna;
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
	 * Return True if the L2PcInstance is autoAttackable.<br>
	 * <B><U>Actions</U>:</B>
	 * <ul>
	 * <li>Check if the attacker isn't the L2PcInstance Pet</li>
	 * <li>Check if the attacker is L2MonsterInstance</li>
	 * <li>If the attacker is a L2PcInstance, check if it is not in the same party</li>
	 * <li>Check if the L2PcInstance has Karma</li>
	 * <li>If the attacker is a L2PcInstance, check if it is not in the same siege clan (Attacker, Defender)</li>
	 * </ul>
	 */
	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		if (attacker == null)
		{
			return false;
		}
		
		// Check if the attacker isn't the L2PcInstance Pet
		if ((attacker == this) || (attacker == getSummon()))
		{
			return false;
		}
		
		// Friendly mobs doesnt attack players
		if (attacker instanceof L2FriendlyMobInstance)
		{
			return false;
		}
		
		// Check if the attacker is a L2MonsterInstance
		if (attacker.isMonster())
		{
			return true;
		}
		
		// is AutoAttackable if both players are in the same duel and the duel is still going on
		if (attacker.isPlayer() && (getDuelState() == Duel.DUELSTATE_DUELLING) && (getDuelId() == ((L2PcInstance) attacker).getDuelId()))
		{
			return true;
		}
		
		// Check if the attacker is not in the same party. NOTE: Party checks goes before oly checks in order to prevent patry member autoattack at oly.
		if (isInParty() && getParty().getMembers().contains(attacker))
		{
			return false;
		}
		
		// Check if the attacker is in olympia and olympia start
		if (attacker.isPlayer() && attacker.getActingPlayer().isInOlympiadMode())
		{
			if (isInOlympiadMode() && isOlympiadStart() && (((L2PcInstance) attacker).getOlympiadGameId() == getOlympiadGameId()))
			{
				return true;
			}
			return false;
		}
		
		// Check if the attacker is in TvT and TvT is started
		if (isOnEvent())
		{
			return true;
		}
		
		// Check if the attacker is a L2Playable
		if (attacker.isPlayable())
		{
			if (isInsideZone(ZoneId.PEACE))
			{
				return false;
			}
			
			// Get L2PcInstance
			L2PcInstance attackerPlayer = attacker.getActingPlayer();
			
			if (getClan() != null)
			{
				Siege siege = SiegeManager.getInstance().getSiege(getX(), getY(), getZ());
				if (siege != null)
				{
					// Check if a siege is in progress and if attacker and the L2PcInstance aren't in the Defender clan
					if (siege.checkIsDefender(attackerPlayer.getClan()) && siege.checkIsDefender(getClan()))
					{
						return false;
					}
					
					// Check if a siege is in progress and if attacker and the L2PcInstance aren't in the Attacker clan
					if (siege.checkIsAttacker(attackerPlayer.getClan()) && siege.checkIsAttacker(getClan()))
					{
						return false;
					}
				}
				
				// Check if clan is at war
				if ((getClan() != null) && (attackerPlayer.getClan() != null) && getClan().isAtWarWith(attackerPlayer.getClanId()) && attackerPlayer.getClan().isAtWarWith(getClanId()) && (getWantsPeace() == 0) && (attackerPlayer.getWantsPeace() == 0) && !isAcademyMember())
				{
					return true;
				}
			}
			
			// Check if the L2PcInstance is in an arena, but NOT siege zone. NOTE: This check comes before clan/ally checks, but after party checks.
			// This is done because in arenas, clan/ally members can autoattack if they arent in party.
			if ((isInsideZone(ZoneId.PVP) && attackerPlayer.isInsideZone(ZoneId.PVP)) && !(isInsideZone(ZoneId.SIEGE) && attackerPlayer.isInsideZone(ZoneId.SIEGE)))
			{
				return true;
			}
			
			// Check if the attacker is not in the same clan
			if ((getClan() != null) && getClan().isMember(attacker.getObjectId()))
			{
				return false;
			}
			
			// Check if the attacker is not in the same ally
			if (attacker.isPlayer() && (getAllyId() != 0) && (getAllyId() == attackerPlayer.getAllyId()))
			{
				return false;
			}
			
			// Now check again if the L2PcInstance is in pvp zone, but this time at siege PvP zone, applying clan/ally checks
			if ((isInsideZone(ZoneId.PVP) && attackerPlayer.isInsideZone(ZoneId.PVP)) && (isInsideZone(ZoneId.SIEGE) && attackerPlayer.isInsideZone(ZoneId.SIEGE)))
			{
				return true;
			}
		}
		else if (attacker instanceof L2DefenderInstance)
		{
			if (getClan() != null)
			{
				Siege siege = SiegeManager.getInstance().getSiege(this);
				return ((siege != null) && siege.checkIsAttacker(getClan()));
			}
		}
		
		// Check if the L2PcInstance has Karma
		if ((getKarma() > 0) || (getPvpFlag() > 0))
		{
			return true;
		}
		
		return false;
	}
	
	/**
	 * Check if the active L2Skill can be casted.<br>
	 * <B><U>Actions</U>:</B>
	 * <ul>
	 * <li>Check if the skill isn't toggle and is offensive</li>
	 * <li>Check if the target is in the skill cast range</li>
	 * <li>Check if the skill is Spoil type and if the target isn't already spoiled</li>
	 * <li>Check if the caster owns enought consummed Item, enough HP and MP to cast the skill</li>
	 * <li>Check if the caster isn't sitting</li>
	 * <li>Check if all skills are enabled and this skill is enabled</li>
	 * <li>Check if the caster own the weapon needed</li>
	 * <li>Check if the skill is active</li>
	 * <li>Check if all casting conditions are completed</li>
	 * <li>Notify the AI with AI_INTENTION_CAST and target</li>
	 * </ul>
	 * @param skill The L2Skill to use
	 * @param forceUse used to force ATTACK on players
	 * @param dontMove used to prevent movement, if not in range
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
		
		// ************************************* Check Casting in Progress *******************************************
		
		// If a skill is currently being used, queue this one if this is not the same
		if (isCastingNow())
		{
			SkillUseHolder currentSkill = getCurrentSkill();
			// Check if new skill different from current skill in progress
			if ((currentSkill != null) && (skill.getId() == currentSkill.getSkillId()))
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
			
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
		
		if (getQueuedSkill() != null)
		{
			setQueuedSkill(null, false, false);
		}
		
		if (!checkUseMagicConditions(skill, forceUse, dontMove))
		{
			setIsCastingNow(false);
			return false;
		}
		
		// Check if the target is correct and Notify the AI with AI_INTENTION_CAST and target
		L2Object target = null;
		switch (skill.getTargetType())
		{
			case AURA: // AURA, SELF should be cast even if no target has been found
			case FRONT_AURA:
			case BEHIND_AURA:
			case GROUND:
			case SELF:
			case AURA_CORPSE_MOB:
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
		
		// ************************************* Check Player State *******************************************
		
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
		
		if (isFishing() && ((sklType != L2SkillType.PUMPING) && (sklType != L2SkillType.REELING) && (sklType != L2SkillType.FISHING)))
		{
			// Only fishing skills are available
			sendPacket(SystemMessageId.ONLY_FISHING_SKILLS_NOW);
			return false;
		}
		
		if (inObserverMode())
		{
			sendPacket(SystemMessageId.OBSERVERS_CANNOT_PARTICIPATE);
			abortCast();
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// Check if the caster is sitting
		if (isSitting())
		{
			// Send a System Message to the caster
			sendPacket(SystemMessageId.CANT_MOVE_SITTING);
			
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
		
		// ************************************* Check Target *******************************************
		// Create and set a L2Object containing the target of the skill
		L2Object target = null;
		L2TargetType sklTargetType = skill.getTargetType();
		Point3D worldPosition = getCurrentSkillWorldPosition();
		
		if ((sklTargetType == L2TargetType.GROUND) && (worldPosition == null))
		{
			_log.info("WorldPosition is null for skill: " + skill.getName() + ", player: " + getName() + ".");
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		switch (sklTargetType)
		{
		// Target the player if skill type is AURA, PARTY, CLAN or SELF
			case AURA:
			case FRONT_AURA:
			case BEHIND_AURA:
			case PARTY:
			case CLAN:
			case PARTY_CLAN:
			case GROUND:
			case SELF:
			case AREA_SUMMON:
			case AURA_CORPSE_MOB:
				target = this;
				break;
			case PET:
			case SERVITOR:
			case SUMMON:
				target = getSummon();
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
		if (target.isDoor())
		{
			if ((((L2DoorInstance) target).getCastle() != null) && (((L2DoorInstance) target).getCastle().getCastleId() > 0)) // If its castle door
			{
				if (!((L2DoorInstance) target).getCastle().getSiege().getIsInProgress())
				{
					return false;
				}
			}
			else if ((((L2DoorInstance) target).getFort() != null) && (((L2DoorInstance) target).getFort().getFortId() > 0) && !((L2DoorInstance) target).getIsShowHp()) // If its fort door
			{
				if (!((L2DoorInstance) target).getFort().getSiege().getIsInProgress())
				{
					return false;
				}
			}
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
		
		// ************************************* Check skill availability *******************************************
		
		// Check if it's ok to summon
		// siege golem (13), Wild Hog Cannon (299), Swoop Cannon (448)
		if (((skill.getId() == 13) || (skill.getId() == 299) || (skill.getId() == 448)) && ((!SiegeManager.getInstance().checkIfOkToSummon(this, false) && !FortSiegeManager.getInstance().checkIfOkToSummon(this, false)) || (SevenSigns.getInstance().checkSummonConditions(this))))
		{
			return false;
		}
		
		// Check if this skill is enabled (ex : reuse time)
		if (isSkillDisabled(skill))
		{
			SystemMessage sm = null;
			if (_reuseTimeStampsSkills.containsKey(skill.getReuseHashCode()))
			{
				int remainingTime = (int) (_reuseTimeStampsSkills.get(skill.getReuseHashCode()).getRemaining() / 1000);
				int hours = remainingTime / 3600;
				int minutes = (remainingTime % 3600) / 60;
				int seconds = (remainingTime % 60);
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
		
		// ************************************* Check casting conditions *******************************************
		
		// Check if all casting conditions are completed
		if (!skill.checkCondition(this, target, false))
		{
			// Send a Server->Client packet ActionFailed to the L2PcInstance
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// ************************************* Check Skill Type *******************************************
		
		// Check if this is bad magic skill
		if (skill.isBad())
		{
			if ((isInsidePeaceZone(this, target)) && !getAccessLevel().allowPeaceAttack())
			{
				// If L2Character or target is in a peace zone, send a system message TARGET_IN_PEACEZONE a Server->Client packet ActionFailed
				sendPacket(SystemMessageId.TARGET_IN_PEACEZONE);
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
			
			if (isInOlympiadMode() && !isOlympiadStart())
			{
				// if L2PcInstance is in Olympia and the match isn't already start, send a Server->Client packet ActionFailed
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
			
			if ((target.getActingPlayer() != null) && (getSiegeState() > 0) && isInsideZone(ZoneId.SIEGE) && (target.getActingPlayer().getSiegeState() == getSiegeState()) && (target.getActingPlayer() != this) && (target.getActingPlayer().getSiegeSide() == getSiegeSide()))
			{
				//
				if (TerritoryWarManager.getInstance().isTWInProgress())
				{
					sendPacket(SystemMessageId.YOU_CANNOT_ATTACK_A_MEMBER_OF_THE_SAME_TERRITORY);
				}
				else
				{
					sendPacket(SystemMessageId.FORCED_ATTACK_IS_IMPOSSIBLE_AGAINST_SIEGE_SIDE_TEMPORARY_ALLIED_MEMBERS);
				}
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
			if ((target instanceof L2EventMonsterInstance) && ((L2EventMonsterInstance) target).eventSkillAttackBlocked())
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
			
			// Check if a Forced ATTACK is in progress on non-attackable target
			if (!target.isAutoAttackable(this) && !forceUse)
			{
				switch (sklTargetType)
				{
					case AURA:
					case FRONT_AURA:
					case BEHIND_AURA:
					case AURA_CORPSE_MOB:
					case CLAN:
					case PARTY:
					case SELF:
					case GROUND:
					case AREA_SUMMON:
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
				if (sklTargetType == L2TargetType.GROUND)
				{
					if (!isInsideRadius(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), skill.getCastRange() + getTemplate().getCollisionRadius(), false, false))
					{
						// Send a System Message to the caster
						sendPacket(SystemMessageId.TARGET_TOO_FAR);
						
						// Send a Server->Client packet ActionFailed to the L2PcInstance
						sendPacket(ActionFailed.STATIC_PACKET);
						return false;
					}
				}
				else if ((skill.getCastRange() > 0) && !isInsideRadius(target, skill.getCastRange() + getTemplate().getCollisionRadius(), false, false))
				{
					// Send a System Message to the caster
					sendPacket(SystemMessageId.TARGET_TOO_FAR);
					
					// Send a Server->Client packet ActionFailed to the L2PcInstance
					sendPacket(ActionFailed.STATIC_PACKET);
					return false;
				}
			}
		}
		
		if (skill.hasEffectType(L2EffectType.TELEPORT_TO_TARGET))
		{
			// You cannot jump while rooted right ;)
			if (isMovementDisabled())
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
			if (isInsideZone(ZoneId.PEACE))
			{
				// Sends a sys msg to client
				sendPacket(SystemMessageId.TARGET_IN_PEACEZONE);
				// Send a Server->Client packet ActionFailed to the L2PcInstance
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
		}
		// Check if the skill is a good magic, target is a monster and if force attack is set, if not then we don't want to cast.
		if ((skill.getEffectPoint() > 0) && target.isMonster() && !forceUse)
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// Check if this is a Pvp skill and target isn't a non-flagged/non-karma player
		switch (sklTargetType)
		{
			case PARTY:
			case CLAN: // For such skills, checkPvpSkill() is called from L2Skill.getTargetList()
			case PARTY_CLAN: // For such skills, checkPvpSkill() is called from L2Skill.getTargetList()
			case AURA:
			case FRONT_AURA:
			case BEHIND_AURA:
			case GROUND:
			case SELF:
				break;
			default:
				if (!checkPvpSkill(target, skill) && !getAccessLevel().allowPeaceAttack())
				{
					// Send a System Message to the L2PcInstance
					sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
					
					// Send a Server->Client packet ActionFailed to the L2PcInstance
					sendPacket(ActionFailed.STATIC_PACKET);
					return false;
				}
		}
		
		// TODO: Unhardcode skillId 844 which is the outpost construct skill
		if (((sklTargetType == L2TargetType.HOLY) && !checkIfOkToCastSealOfRule(CastleManager.getInstance().getCastle(this), false, skill, target)) || ((sklTargetType == L2TargetType.FLAGPOLE) && !checkIfOkToCastFlagDisplay(FortManager.getInstance().getFort(this), false, skill, target)) || ((sklType == L2SkillType.SIEGEFLAG) && !L2SkillSiegeFlag.checkIfOkToPlaceFlag(this, false, skill.getId() == 844)))
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			abortCast();
			return false;
		}
		
		// GeoData Los Check here
		if (skill.getCastRange() > 0)
		{
			if (sklTargetType == L2TargetType.GROUND)
			{
				if (!GeoData.getInstance().canSeeTarget(this, worldPosition))
				{
					sendPacket(SystemMessageId.CANT_SEE_TARGET);
					sendPacket(ActionFailed.STATIC_PACKET);
					return false;
				}
			}
			else if (!GeoData.getInstance().canSeeTarget(this, target))
			{
				sendPacket(SystemMessageId.CANT_SEE_TARGET);
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
		}
		// finally, after passing all conditions
		return true;
	}
	
	public boolean checkIfOkToCastSealOfRule(Castle castle, boolean isCheckOnly, L2Skill skill, L2Object target)
	{
		SystemMessage sm;
		if ((castle == null) || (castle.getCastleId() <= 0))
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
			sm.addSkillName(skill);
		}
		else if (!castle.getArtefacts().contains(target))
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.INCORRECT_TARGET);
		}
		else if (!castle.getSiege().getIsInProgress())
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
			sm.addSkillName(skill);
		}
		else if (!Util.checkIfInRange(200, this, target, true))
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
	
	public boolean checkIfOkToCastFlagDisplay(Fort fort, boolean isCheckOnly, L2Skill skill, L2Object target)
	{
		SystemMessage sm;
		
		if ((fort == null) || (fort.getFortId() <= 0))
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
			sm.addSkillName(skill);
		}
		else if (fort.getFlagPole() != target)
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.INCORRECT_TARGET);
		}
		else if (!fort.getSiege().getIsInProgress())
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
			sm.addSkillName(skill);
		}
		else if (!Util.checkIfInRange(200, this, target, true))
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
			{
				fort.getSiege().announceToPlayer(SystemMessage.getSystemMessage(SystemMessageId.S1_TRYING_RAISE_FLAG), getClan().getName());
			}
			return true;
		}
		
		sendPacket(sm);
		return false;
	}
	
	public boolean isInLooterParty(int LooterId)
	{
		L2PcInstance looter = L2World.getInstance().getPlayer(LooterId);
		
		// if L2PcInstance is in a CommandChannel
		if (isInParty() && getParty().isInCommandChannel() && (looter != null))
		{
			return getParty().getCommandChannel().getMembers().contains(looter);
		}
		
		if (isInParty() && (looter != null))
		{
			return getParty().getMembers().contains(looter);
		}
		
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
	 * @return {@code false} if the skill is a pvpSkill and target is not a valid pvp target, {@code true} otherwise.
	 */
	public boolean checkPvpSkill(L2Object target, L2Skill skill, boolean srcIsSummon)
	{
		final L2PcInstance targetPlayer = target != null ? target.getActingPlayer() : null;
		if ((targetPlayer != null) && (target != this) && !(isInDuel() && (targetPlayer.getDuelId() == getDuelId())) && !isInsideZone(ZoneId.PVP) && !targetPlayer.isInsideZone(ZoneId.PVP))
		{
			SkillUseHolder skilldat = getCurrentSkill();
			SkillUseHolder skilldatpet = getCurrentPetSkill();
			if (((skilldat != null) && !skilldat.isCtrlPressed() && skill.isBad() && !srcIsSummon) || ((skilldatpet != null) && !skilldatpet.isCtrlPressed() && skill.isBad() && srcIsSummon))
			{
				if ((getClan() != null) && (targetPlayer.getClan() != null))
				{
					if (getClan().isAtWarWith(targetPlayer.getClan().getClanId()) && targetPlayer.getClan().isAtWarWith(getClan().getClanId()))
					{
						return true;
					}
				}
				if ((targetPlayer.getPvpFlag() == 0) && (targetPlayer.getKarma() == 0))
				{
					return false;
				}
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
		return _mountType != MountType.NONE;
	}
	
	public boolean checkLandingState()
	{
		// Check if char is in a no landing zone
		if (isInsideZone(ZoneId.NO_LANDING))
		{
			return true;
		}
		else
		// if this is a castle that is currently being sieged, and the rider is NOT a castle owner
		// he cannot land.
		// castle owner is the leader of the clan that owns the castle where the pc is
		if (isInsideZone(ZoneId.SIEGE) && !((getClan() != null) && (CastleManager.getInstance().getCastle(this) == CastleManager.getInstance().getCastleByOwner(getClan())) && (this == getClan().getLeader().getPlayerInstance())))
		{
			return true;
		}
		
		return false;
	}
	
	// returns false if the change of mount type fails.
	public void setMount(int npcId, int npcLevel)
	{
		final MountType type = MountType.findByNpcId(npcId);
		switch (type)
		{
			case NONE: // None
			{
				setIsFlying(false);
				break;
			}
			case STRIDER: // Strider
			{
				if (isNoble())
				{
					addSkill(FrequentSkill.STRIDER_SIEGE_ASSAULT.getSkill(), false);
				}
				break;
			}
			case WYVERN: // Wyvern
			{
				setIsFlying(true);
				break;
			}
		}
		
		_mountType = type;
		_mountNpcId = npcId;
		_mountLevel = npcLevel;
	}
	
	/**
	 * @return the type of Pet mounted (0 : none, 1 : Strider, 2 : Wyvern, 3: Wolf).
	 */
	public MountType getMountType()
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
		for (L2Effect effect : getEffectList().getAllEffects())
		{
			if ((effect != null) && !effect.getSkill().isStayOnSubclassChange())
			{
				effect.exit(true);
			}
		}
		updateAndBroadcastStatus(2);
	}
	
	/**
	 * Stop all toggle-type effects
	 */
	public final void stopAllToggles()
	{
		getEffectList().stopAllToggles();
	}
	
	public final void stopCubics()
	{
		if (!_cubics.isEmpty())
		{
			for (L2CubicInstance cubic : _cubics)
			{
				cubic.stopAction();
				cubic.cancelDisappear();
			}
			_cubics.clear();
			broadcastUserInfo();
		}
	}
	
	public final void stopCubicsByOthers()
	{
		if (!_cubics.isEmpty())
		{
			boolean broadcast = false;
			for (L2CubicInstance cubic : _cubics)
			{
				if (cubic.givenByOther())
				{
					cubic.stopAction();
					cubic.cancelDisappear();
					_cubics.remove(cubic);
					broadcast = true;
				}
			}
			if (broadcast)
			{
				broadcastUserInfo();
			}
		}
	}
	
	/**
	 * Send a Server->Client packet UserInfo to this L2PcInstance and CharInfo to all L2PcInstance in its _KnownPlayers.<br>
	 * <B><U>Concept</U>:</B><br>
	 * Others L2PcInstance in the detection area of the L2PcInstance are identified in <B>_knownPlayers</B>.<br>
	 * In order to inform other players of this L2PcInstance state modifications, server just need to go through _knownPlayers to send Server->Client Packet<br>
	 * <B><U>Actions</U>:</B>
	 * <ul>
	 * <li>Send a Server->Client packet UserInfo to this L2PcInstance (Public and Private Data)</li>
	 * <li>Send a Server->Client packet CharInfo to all L2PcInstance in _KnownPlayers of the L2PcInstance (Public data only)</li>
	 * </ul>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : DON'T SEND UserInfo packet to other players instead of CharInfo packet. Indeed, UserInfo packet contains PRIVATE DATA as MaxHP, STR, DEX...</B></FONT>
	 */
	@Override
	public void updateAbnormalEffect()
	{
		broadcastUserInfo();
	}
	
	/**
	 * Disable the Inventory and create a new task to enable it after 1.5s.
	 * @param val
	 */
	public void setInventoryBlockingStatus(boolean val)
	{
		_inventoryDisable = val;
		if (val)
		{
			ThreadPoolManager.getInstance().scheduleGeneral(new InventoryEnableTask(this), 1500);
		}
	}
	
	/**
	 * @return True if the Inventory is disabled.
	 */
	public boolean isInventoryDisabled()
	{
		return _inventoryDisable;
	}
	
	public List<L2CubicInstance> getCubics()
	{
		return _cubics;
	}
	
	/**
	 * Add a cubic to this player.
	 * @param id
	 * @param level
	 * @param cubicPower
	 * @param cubicDelay
	 * @param cubicSkillChance
	 * @param cubicMaxCount
	 * @param cubicDuration
	 * @param givenByOther
	 */
	public void addCubic(int id, int level, double cubicPower, int cubicDelay, int cubicSkillChance, int cubicMaxCount, int cubicDuration, boolean givenByOther)
	{
		_cubics.add(new L2CubicInstance(this, id, level, (int) cubicPower, cubicDelay, cubicSkillChance, cubicMaxCount, cubicDuration, givenByOther));
	}
	
	/**
	 * Get the player cubic by NPC Id, if any.
	 * @param id the NPC id
	 * @return the cubic with the given NPC id, {@code null} otherwise
	 */
	public L2CubicInstance getCubicById(int id)
	{
		for (L2CubicInstance c : _cubics)
		{
			if (c.getId() == id)
			{
				return c;
			}
		}
		return null;
	}
	
	/**
	 * @return the modifier corresponding to the Enchant Effect of the Active Weapon (Min : 127).
	 */
	public int getEnchantEffect()
	{
		L2ItemInstance wpn = getActiveWeaponInstance();
		
		if (wpn == null)
		{
			return 0;
		}
		
		return Math.min(127, wpn.getEnchantLevel());
	}
	
	/**
	 * Set the _lastFolkNpc of the L2PcInstance corresponding to the last Folk wich one the player talked.
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
	
	@Override
	public void rechargeShots(boolean physical, boolean magic)
	{
		L2ItemInstance item;
		IItemHandler handler;
		
		if ((_activeSoulShots == null) || _activeSoulShots.isEmpty())
		{
			return;
		}
		
		for (int itemId : _activeSoulShots)
		{
			item = getInventory().getItemByItemId(itemId);
			
			if (item != null)
			{
				if (magic)
				{
					if (item.getItem().getDefaultAction() == L2ActionType.spiritshot)
					{
						handler = ItemHandler.getInstance().getHandler(item.getEtcItem());
						if (handler != null)
						{
							handler.useItem(this, item, false);
						}
					}
				}
				
				if (physical)
				{
					if (item.getItem().getDefaultAction() == L2ActionType.soulshot)
					{
						handler = ItemHandler.getInstance().getHandler(item.getEtcItem());
						if (handler != null)
						{
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
	
	/**
	 * Cancel autoshot for all shots matching crystaltype<BR>
	 * {@link L2Item#getCrystalType()}
	 * @param crystalType int type to disable
	 */
	public void disableAutoShotByCrystalType(int crystalType)
	{
		for (int itemId : _activeSoulShots)
		{
			if (ItemTable.getInstance().getTemplate(itemId).getCrystalType() == crystalType)
			{
				disableAutoShot(itemId);
			}
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
			sm.addItemName(itemId);
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
			sm.addItemName(itemId);
			sendPacket(sm);
		}
		_activeSoulShots.clear();
	}
	
	private ScheduledFuture<?> _taskWarnUserTakeBreak;
	
	public int getClanPrivileges()
	{
		return _clanPrivileges;
	}
	
	public void setClanPrivileges(int n)
	{
		_clanPrivileges = n;
	}
	
	public boolean hasClanPrivilege(int privilege)
	{
		return ((_clanPrivileges & privilege) == privilege);
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
		// sendPacket(new GMHide(1));
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
		if (hasSummon())
		{
			getSummon().unSummon(this);
		}
		
		stopEffects(L2EffectType.HIDE);
		
		if (!_cubics.isEmpty())
		{
			for (L2CubicInstance cubic : _cubics)
			{
				cubic.stopAction();
				cubic.cancelDisappear();
			}
			_cubics.clear();
		}
		
		if (getParty() != null)
		{
			getParty().removePartyMember(this, messageType.Expelled);
		}
		
		_olympiadGameId = id;
		if (isSitting())
		{
			standUp();
		}
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
		if (!isGM())
		{
			getAppearance().setVisible();
			setIsInvul(false);
		}
		if (hasAI())
		{
			getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		}
		
		setFalling(); // prevent receive falling damage
		_observerMode = false;
		setLastCords(0, 0, 0);
		sendPacket(new ObservationReturn(this));
		broadcastUserInfo();
	}
	
	public void leaveOlympiadObserverMode()
	{
		if (_olympiadGameId == -1)
		{
			return;
		}
		_olympiadGameId = -1;
		_observerMode = false;
		setTarget(null);
		sendPacket(new ExOlympiadMode(0));
		setInstanceId(0);
		teleToLocation(_lastX, _lastY, _lastZ, true);
		if (!isGM())
		{
			getAppearance().setVisible();
			setIsInvul(false);
		}
		if (hasAI())
		{
			getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		}
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
		if (hero && (_baseClass == _activeClass))
		{
			for (L2Skill skill : SkillTreesData.getInstance().getHeroSkillTree().values())
			{
				addSkill(skill, false); // Don't persist hero skills into database
			}
		}
		else
		{
			for (L2Skill skill : SkillTreesData.getInstance().getHeroSkillTree().values())
			{
				removeSkill(skill, false, true); // Just remove skills from non-hero players
			}
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
	
	public boolean isOlympiadStart()
	{
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
	 * This returns a SystemMessage stating why the player is not available for duelling.
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
	 * Checks if this player might join / start a duel.<br>
	 * To get the reason use getNoDuelReason() after calling this function.
	 * @return true if the player might join/start a duel.
	 */
	public boolean canDuel()
	{
		if (isInCombat() || isJailed())
		{
			_noDuelReason = SystemMessageId.C1_CANNOT_DUEL_BECAUSE_C1_IS_CURRENTLY_ENGAGED_IN_BATTLE;
			return false;
		}
		if (isDead() || isAlikeDead() || ((getCurrentHp() < (getMaxHp() / 2)) || (getCurrentMp() < (getMaxMp() / 2))))
		{
			_noDuelReason = SystemMessageId.C1_CANNOT_DUEL_BECAUSE_C1_HP_OR_MP_IS_BELOW_50_PERCENT;
			return false;
		}
		if (isInDuel())
		{
			_noDuelReason = SystemMessageId.C1_CANNOT_DUEL_BECAUSE_C1_IS_ALREADY_ENGAGED_IN_A_DUEL;
			return false;
		}
		if (isInOlympiadMode())
		{
			_noDuelReason = SystemMessageId.C1_CANNOT_DUEL_BECAUSE_C1_IS_PARTICIPATING_IN_THE_OLYMPIAD;
			return false;
		}
		if (isCursedWeaponEquipped())
		{
			_noDuelReason = SystemMessageId.C1_CANNOT_DUEL_BECAUSE_C1_IS_IN_A_CHAOTIC_STATE;
			return false;
		}
		if (getPrivateStoreType() != STORE_PRIVATE_NONE)
		{
			_noDuelReason = SystemMessageId.C1_CANNOT_DUEL_BECAUSE_C1_IS_CURRENTLY_ENGAGED_IN_A_PRIVATE_STORE_OR_MANUFACTURE;
			return false;
		}
		if (isMounted() || isInBoat())
		{
			_noDuelReason = SystemMessageId.C1_CANNOT_DUEL_BECAUSE_C1_IS_CURRENTLY_RIDING_A_BOAT_STEED_OR_STRIDER;
			return false;
		}
		if (isFishing())
		{
			_noDuelReason = SystemMessageId.C1_CANNOT_DUEL_BECAUSE_C1_IS_CURRENTLY_FISHING;
			return false;
		}
		if (isInsideZone(ZoneId.PVP) || isInsideZone(ZoneId.PEACE) || isInsideZone(ZoneId.SIEGE))
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
		final Collection<L2Skill> nobleSkillTree = SkillTreesData.getInstance().getNobleSkillTree().values();
		if (val)
		{
			for (L2Skill skill : nobleSkillTree)
			{
				addSkill(skill, false);
			}
		}
		else
		{
			for (L2Skill skill : nobleSkillTree)
			{
				removeSkill(skill, false, true);
			}
		}
		
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
	
	@Override
	public void setTeam(int team)
	{
		super.setTeam(team);
		broadcastUserInfo();
		if (hasSummon())
		{
			getSummon().broadcastStatusUpdate();
		}
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
	
	public void sendSkillList()
	{
		boolean isDisabled = false;
		SkillList sl = new SkillList();
		
		for (L2Skill s : getAllSkills())
		{
			if (s == null)
			{
				continue;
			}
			
			if ((_transformation != null) && (!hasTransformSkill(s.getId()) && !s.allowOnTransform()))
			{
				continue;
			}
			if (getClan() != null)
			{
				isDisabled = s.isClanSkill() && (getClan().getReputationScore() < 0);
			}
			
			boolean isEnchantable = SkillTable.getInstance().isEnchantable(s.getId());
			if (isEnchantable)
			{
				L2EnchantSkillLearn esl = EnchantSkillGroupsData.getInstance().getSkillEnchantmentBySkillId(s.getId());
				if (esl != null)
				{
					// if player dont have min level to enchant
					if (s.getLevel() < esl.getBaseLevel())
					{
						isEnchantable = false;
					}
				}
				// if no enchant data
				else
				{
					isEnchantable = false;
				}
			}
			
			sl.addSkill(s.getDisplayId(), s.getDisplayLevel(), s.isPassive(), isDisabled, isEnchantable);
		}
		
		sendPacket(sl);
	}
	
	/**
	 * 1. Add the specified class ID as a subclass (up to the maximum number of <b>three</b>) for this character.<BR>
	 * 2. This method no longer changes the active _classIndex of the player. This is only done by the calling of setActiveClass() method as that should be the only way to do so.
	 * @param classId
	 * @param classIndex
	 * @return boolean subclassAdded
	 */
	public boolean addSubClass(int classId, int classIndex)
	{
		if (!_subclassLock.tryLock())
		{
			return false;
		}
		
		try
		{
			if ((getTotalSubClasses() == Config.MAX_SUBCLASS) || (classIndex == 0))
			{
				return false;
			}
			
			if (getSubClasses().containsKey(classIndex))
			{
				return false;
			}
			
			// Note: Never change _classIndex in any method other than setActiveClass().
			
			SubClass newClass = new SubClass();
			newClass.setClassId(classId);
			newClass.setClassIndex(classIndex);
			
			try (Connection con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement(ADD_CHAR_SUBCLASS))
			{
				// Store the basic info about this new sub-class.
				statement.setInt(1, getObjectId());
				statement.setInt(2, newClass.getClassId());
				statement.setLong(3, newClass.getExp());
				statement.setInt(4, newClass.getSp());
				statement.setInt(5, newClass.getLevel());
				statement.setInt(6, newClass.getClassIndex()); // <-- Added
				statement.execute();
			}
			catch (Exception e)
			{
				_log.log(Level.WARNING, "WARNING: Could not add character sub class for " + getName() + ": " + e.getMessage(), e);
				return false;
			}
			
			// Commit after database INSERT incase exception is thrown.
			getSubClasses().put(newClass.getClassIndex(), newClass);
			
			final ClassId subTemplate = ClassId.getClassId(classId);
			final Map<Integer, L2SkillLearn> skillTree = SkillTreesData.getInstance().getCompleteClassSkillTree(subTemplate);
			final Map<Integer, L2Skill> prevSkillList = new HashMap<>();
			for (L2SkillLearn skillInfo : skillTree.values())
			{
				if (skillInfo.getGetLevel() <= 40)
				{
					L2Skill prevSkill = prevSkillList.get(skillInfo.getSkillId());
					L2Skill newSkill = SkillTable.getInstance().getInfo(skillInfo.getSkillId(), skillInfo.getSkillLevel());
					
					if ((prevSkill != null) && (prevSkill.getLevel() > newSkill.getLevel()))
					{
						continue;
					}
					
					prevSkillList.put(newSkill.getId(), newSkill);
					storeSkill(newSkill, prevSkill, classIndex);
				}
			}
			return true;
		}
		finally
		{
			_subclassLock.unlock();
		}
	}
	
	/**
	 * 1. Completely erase all existance of the subClass linked to the classIndex.<br>
	 * 2. Send over the newClassId to addSubClass() to create a new instance on this classIndex.<br>
	 * 3. Upon Exception, revert the player to their BaseClass to avoid further problems.
	 * @param classIndex the class index to delete
	 * @param newClassId the new class Id
	 * @return {@code true} if the sub-class was modified, {@code false} otherwise
	 */
	public boolean modifySubClass(int classIndex, int newClassId)
	{
		if (!_subclassLock.tryLock())
		{
			return false;
		}
		
		try
		{
			try (Connection con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement deleteHennas = con.prepareStatement(DELETE_CHAR_HENNAS);
				PreparedStatement deleteShortcuts = con.prepareStatement(DELETE_CHAR_SHORTCUTS);
				PreparedStatement deleteSkillReuse = con.prepareStatement(DELETE_SKILL_SAVE);
				PreparedStatement deleteSkills = con.prepareStatement(DELETE_CHAR_SKILLS);
				PreparedStatement deleteSubclass = con.prepareStatement(DELETE_CHAR_SUBCLASS))
			{
				// Remove all henna info stored for this sub-class.
				deleteHennas.setInt(1, getObjectId());
				deleteHennas.setInt(2, classIndex);
				deleteHennas.execute();
				
				// Remove all shortcuts info stored for this sub-class.
				deleteShortcuts.setInt(1, getObjectId());
				deleteShortcuts.setInt(2, classIndex);
				deleteShortcuts.execute();
				
				// Remove all effects info stored for this sub-class.
				deleteSkillReuse.setInt(1, getObjectId());
				deleteSkillReuse.setInt(2, classIndex);
				deleteSkillReuse.execute();
				
				// Remove all skill info stored for this sub-class.
				deleteSkills.setInt(1, getObjectId());
				deleteSkills.setInt(2, classIndex);
				deleteSkills.execute();
				
				// Remove all basic info stored about this sub-class.
				deleteSubclass.setInt(1, getObjectId());
				deleteSubclass.setInt(2, classIndex);
				deleteSubclass.execute();
			}
			catch (Exception e)
			{
				_log.log(Level.WARNING, "Could not modify sub class for " + getName() + " to class index " + classIndex + ": " + e.getMessage(), e);
				
				// This must be done in order to maintain data consistency.
				getSubClasses().remove(classIndex);
				return false;
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
		{
			_subClasses = new FastMap<>();
		}
		
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
		
		final L2PcTemplate pcTemplate = CharTemplateTable.getInstance().getTemplate(classId);
		if (pcTemplate == null)
		{
			_log.severe("Missing template for classId: " + classId);
			throw new Error();
		}
		// Set the template of the L2PcInstance
		setTemplate(pcTemplate);
		fireProfessionChangeListeners(pcTemplate);
	}
	
	/**
	 * Changes the character's class based on the given class index.<br>
	 * An index of zero specifies the character's original (base) class, while indexes 1-3 specifies the character's sub-classes respectively.<br>
	 * <font color="00FF00"/>WARNING: Use only on subclase change</font>
	 * @param classIndex
	 * @return
	 */
	public boolean setActiveClass(int classIndex)
	{
		if (!_subclassLock.tryLock())
		{
			return false;
		}
		
		try
		{
			// Cannot switch or change subclasses while transformed
			if (_transformation != null)
			{
				return false;
			}
			
			// Remove active item skills before saving char to database
			// because next time when choosing this class, weared items can
			// be different
			for (L2ItemInstance item : getInventory().getAugmentedItems())
			{
				if ((item != null) && item.isEquipped())
				{
					item.getAugmentation().removeBonus(this);
				}
			}
			
			// abort any kind of cast.
			abortCast();
			
			// Stop casting for any player that may be casting a force buff on this l2pcinstance.
			for (L2Character character : getKnownList().getKnownCharacters())
			{
				if ((character.getFusionSkill() != null) && (character.getFusionSkill().getTarget() == this))
				{
					character.abortCast();
				}
			}
			
			// 1. Call store() before modifying _classIndex to avoid skill effects rollover.
			// 2. Register the correct _classId against applied 'classIndex'.
			store(Config.SUBCLASS_STORE_SKILL_COOLTIME);
			_reuseTimeStampsSkills.clear();
			
			// clear charges
			_charges.set(0);
			stopChargeTask();
			
			if (hasServitor())
			{
				getSummon().unSummon(this);
			}
			
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
			
			setLearningClass(getClassId());
			
			if (isInParty())
			{
				getParty().recalculatePartyLevel();
			}
			
			// Update the character's change in class status.
			// 1. Remove any active cubics from the player.
			// 2. Renovate the characters table in the database with the new class info, storing also buff/effect data.
			// 3. Remove all existing skills.
			// 4. Restore all the learned skills for the current class from the database.
			// 5. Restore effect/buff data for the new class.
			// 6. Restore henna data for the class, applying the new stat modifiers while removing existing ones.
			// 7. Reset HP/MP/CP stats and send Server->Client character status packet to reflect changes.
			// 8. Restore shortcut data related to this class.
			// 9. Resend a class change animation effect to broadcast to all nearby players.
			for (L2Skill oldSkill : getAllSkills())
			{
				removeSkill(oldSkill, false, true);
			}
			
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
			if ((_disabledSkills != null) && !_disabledSkills.isEmpty())
			{
				_disabledSkills.clear();
			}
			
			restoreEffects();
			updateEffectIcons();
			sendPacket(new EtcStatusUpdate(this));
			
			// if player has quest 422: Repent Your Sins, remove it
			QuestState st = getQuestState("422_RepentYourSins");
			if (st != null)
			{
				st.exitQuest(true);
			}
			
			for (int i = 0; i < 3; i++)
			{
				_henna[i] = null;
			}
			
			restoreHenna();
			sendPacket(new HennaInfo(this));
			
			if (getCurrentHp() > getMaxHp())
			{
				setCurrentHp(getMaxHp());
			}
			if (getCurrentMp() > getMaxMp())
			{
				setCurrentMp(getMaxMp());
			}
			if (getCurrentCp() > getMaxCp())
			{
				setCurrentCp(getMaxCp());
			}
			
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
			_taskWarnUserTakeBreak = null;
		}
	}
	
	public void startWarnUserTakeBreak()
	{
		if (_taskWarnUserTakeBreak == null)
		{
			_taskWarnUserTakeBreak = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new WarnUserTakeBreakTask(this), 7200000, 7200000);
		}
	}
	
	public void stopRentPet()
	{
		if (_taskRentPet != null)
		{
			// if the rent of a wyvern expires while over a flying zone, tp to down before unmounting
			if (checkLandingState() && (getMountType() == MountType.WYVERN))
			{
				teleToLocation(TeleportWhereType.TOWN);
			}
			
			if (dismount()) // this should always be true now, since we teleported already
			{
				_taskRentPet.cancel(true);
				_taskRentPet = null;
			}
		}
	}
	
	public void startRentPet(int seconds)
	{
		if (_taskRentPet == null)
		{
			_taskRentPet = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new RentPetTask(this), seconds * 1000L, seconds * 1000L);
		}
	}
	
	public boolean isRentedPet()
	{
		if (_taskRentPet != null)
		{
			return true;
		}
		
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
		if (!isDead() && (_taskWater == null))
		{
			int timeinwater = (int) calcStat(Stats.BREATH, 60000, this, null);
			
			sendPacket(new SetupGauge(2, timeinwater));
			_taskWater = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new WaterTask(this), timeinwater, 1000);
		}
	}
	
	public boolean isInWater()
	{
		if (_taskWater != null)
		{
			return true;
		}
		
		return false;
	}
	
	public void checkWaterState()
	{
		if (isInsideZone(ZoneId.WATER))
		{
			startWaterTask();
		}
		else
		{
			stopWaterTask();
		}
	}
	
	public void onPlayerEnter()
	{
		startWarnUserTakeBreak();
		
		if (SevenSigns.getInstance().isSealValidationPeriod() || SevenSigns.getInstance().isCompResultsPeriod())
		{
			if (!isGM() && isIn7sDungeon() && (SevenSigns.getInstance().getPlayerCabal(getObjectId()) != SevenSigns.getInstance().getCabalHighestScore()))
			{
				teleToLocation(TeleportWhereType.TOWN);
				setIsIn7sDungeon(false);
				sendMessage("You have been teleported to the nearest town due to the beginning of the Seal Validation period.");
			}
		}
		else
		{
			if (!isGM() && isIn7sDungeon() && (SevenSigns.getInstance().getPlayerCabal(getObjectId()) == SevenSigns.CABAL_NULL))
			{
				teleToLocation(TeleportWhereType.TOWN);
				setIsIn7sDungeon(false);
				sendMessage("You have been teleported to the nearest town because you have not signed for any cabal.");
			}
		}
		
		if (isGM())
		{
			if (isInvul())
			{
				sendMessage("Entering world in Invulnerable mode.");
			}
			if (getAppearance().getInvisible())
			{
				sendMessage("Entering world in Invisible mode.");
			}
			if (isSilenceMode())
			{
				sendMessage("Entering world in Silence mode.");
			}
		}
		
		revalidateZone(true);
		
		notifyFriends();
		if (!canOverrideCond(PcCondOverride.SKILL_CONDITIONS) && Config.DECREASE_SKILL_LEVEL)
		{
			checkPlayerSkills();
		}
		getEvents().onPlayerLogin();
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
		{
			startFeed(_mountNpcId);
		}
		if (isInParty() && getParty().isInDimensionalRift())
		{
			if (!DimensionalRiftManager.getInstance().checkIfInPeaceZone(getX(), getY(), getZ()))
			{
				getParty().getDimensionalRift().memberRessurected(this);
			}
		}
		if (getInstanceId() > 0)
		{
			final Instance instance = InstanceManager.getInstance().getInstance(getInstanceId());
			if (instance != null)
			{
				instance.cancelEjectDeadPlayer(this);
			}
		}
	}
	
	@Override
	public void setName(String value)
	{
		super.setName(value);
		if (Config.CACHE_CHAR_NAMES)
		{
			CharNameTable.getInstance().addName(this);
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
	
	public void reviveRequest(L2PcInstance reviver, L2Skill skill, boolean Pet)
	{
		if (isResurrectionBlocked())
		{
			return;
		}
		
		if (_reviveRequested == 1)
		{
			if (_revivePet == Pet)
			{
				reviver.sendPacket(SystemMessageId.RES_HAS_ALREADY_BEEN_PROPOSED); // Resurrection is already been proposed.
			}
			else
			{
				if (Pet)
				{
					reviver.sendPacket(SystemMessageId.CANNOT_RES_PET2); // A pet cannot be resurrected while it's owner is in the process of resurrecting.
				}
				else
				{
					reviver.sendPacket(SystemMessageId.MASTER_CANNOT_RES); // While a pet is attempting to resurrect, it cannot help in resurrecting its master.
				}
			}
			return;
		}
		if ((Pet && hasSummon() && getSummon().isDead()) || (!Pet && isDead()))
		{
			_reviveRequested = 1;
			int restoreExp = 0;
			if (isPhoenixBlessed())
			{
				_revivePower = 100;
			}
			else if (isAffected(EffectFlag.CHARM_OF_COURAGE))
			{
				_revivePower = 0;
			}
			else
			{
				_revivePower = Formulas.calculateSkillResurrectRestorePercent(skill.getPower(), reviver);
			}
			
			restoreExp = (int) Math.round(((getExpBeforeDeath() - getExp()) * _revivePower) / 100);
			
			_revivePet = Pet;
			
			if (isAffected(EffectFlag.CHARM_OF_COURAGE))
			{
				ConfirmDlg dlg = new ConfirmDlg(SystemMessageId.RESURRECT_USING_CHARM_OF_COURAGE.getId());
				dlg.addTime(60000);
				sendPacket(dlg);
				return;
			}
			ConfirmDlg dlg = new ConfirmDlg(SystemMessageId.RESSURECTION_REQUEST_BY_C1_FOR_S2_XP.getId());
			dlg.addPcName(reviver);
			dlg.addString(Integer.toString(restoreExp));
			sendPacket(dlg);
		}
	}
	
	public void reviveAnswer(int answer)
	{
		if ((_reviveRequested != 1) || (!isDead() && !_revivePet) || (_revivePet && hasSummon() && !getSummon().isDead()))
		{
			return;
		}
		// If character refuses a PhoenixBless autoress, cancel all buffs he had
		if ((answer == 0) && isPhoenixBlessed())
		{
			stopEffects(L2EffectType.PHOENIX_BLESSING);
			stopAllEffectsExceptThoseThatLastThroughDeath();
		}
		if (answer == 1)
		{
			if (!_revivePet)
			{
				if (_revivePower != 0)
				{
					doRevive(_revivePower);
				}
				else
				{
					doRevive();
				}
			}
			else if (hasSummon())
			{
				if (_revivePower != 0)
				{
					getSummon().doRevive(_revivePower);
				}
				else
				{
					getSummon().doRevive();
				}
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
			sendPacket(SystemMessageId.YOU_ARE_NO_LONGER_PROTECTED_FROM_AGGRESSIVE_MONSTERS);
			
			if (Config.RESTORE_SERVITOR_ON_RECONNECT && !hasSummon() && CharSummonTable.getInstance().getServitors().containsKey(getObjectId()))
			{
				CharSummonTable.getInstance().restoreServitor(this);
			}
			if (Config.RESTORE_PET_ON_RECONNECT && !hasSummon() && CharSummonTable.getInstance().getPets().containsKey(getObjectId()))
			{
				CharSummonTable.getInstance().restorePet(this);
			}
		}
		if (isTeleportProtected())
		{
			sendMessage("Teleport spawn protection ended.");
		}
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
		{
			level = 0;
		}
		return level;
	}
	
	@Override
	public void teleToLocation(int x, int y, int z, int heading, boolean allowRandomOffset)
	{
		if ((getVehicle() != null) && !getVehicle().isTeleporting())
		{
			setVehicle(null);
		}
		
		if (isFlyingMounted() && (z < -1005))
		{
			z = -1005;
		}
		
		super.teleToLocation(x, y, z, heading, allowRandomOffset);
	}
	
	@Override
	public final void onTeleported()
	{
		super.onTeleported();
		
		if (isInAirShip())
		{
			getAirShip().sendInfo(this);
		}
		
		// Force a revalidation
		revalidateZone(true);
		
		checkItemRestriction();
		
		if ((Config.PLAYER_TELEPORT_PROTECTION > 0) && !isInOlympiadMode())
		{
			setTeleportProtection(true);
		}
		
		// Trained beast is lost after teleport
		if (getTrainedBeasts() != null)
		{
			for (L2TamedBeastInstance tamedBeast : getTrainedBeasts())
			{
				tamedBeast.deleteMe();
			}
			getTrainedBeasts().clear();
		}
		
		// Modify the position of the pet if necessary
		if (hasSummon())
		{
			getSummon().setFollowStatus(false);
			getSummon().teleToLocation(getPosition().getX(), getPosition().getY(), getPosition().getZ(), false);
			((L2SummonAI) getSummon().getAI()).setStartFollowController(true);
			getSummon().setFollowStatus(true);
			getSummon().updateAndBroadcastStatus(0);
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
		{
			return;
		}
		if (teleport)
		{
			if ((_teleportWatchdog == null) && (Config.TELEPORT_WATCHDOG_TIMEOUT > 0))
			{
				synchronized (this)
				{
					if (_teleportWatchdog == null)
					{
						_teleportWatchdog = ThreadPoolManager.getInstance().scheduleGeneral(new TeleportWatchdogTask(this), Config.TELEPORT_WATCHDOG_TIMEOUT * 1000);
					}
				}
			}
		}
		else
		{
			if (_teleportWatchdog != null)
			{
				_teleportWatchdog.cancel(false);
				_teleportWatchdog = null;
			}
		}
	}
	
	public void setLastServerPosition(int x, int y, int z)
	{
		_lastServerPosition.setXYZ(x, y, z);
	}
	
	public Point3D getLastServerPosition()
	{
		return _lastServerPosition;
	}
	
	public boolean checkLastServerPosition(int x, int y, int z)
	{
		return _lastServerPosition.equals(x, y, z);
	}
	
	public int getLastServerDistance(int x, int y, int z)
	{
		double dx = (x - _lastServerPosition.getX());
		double dy = (y - _lastServerPosition.getY());
		double dz = (z - _lastServerPosition.getZ());
		
		return (int) Math.sqrt((dx * dx) + (dy * dy) + (dz * dz));
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
		{
			getStatus().reduceHp(value, attacker, awake, isDOT, skill.isToggle(), skill.getDmgDirectlyToHP());
		}
		else
		{
			getStatus().reduceHp(value, attacker, awake, isDOT, false, false);
		}
		
		// notify the tamed beast of attacks
		if (getTrainedBeasts() != null)
		{
			for (L2TamedBeastInstance tamedBeast : getTrainedBeasts())
			{
				tamedBeast.onOwnerGotAttacked(attacker);
			}
		}
	}
	
	public void broadcastSnoop(int type, String name, String _text)
	{
		if (!_snoopListener.isEmpty())
		{
			Snoop sn = new Snoop(getObjectId(), getName(), type, name, _text);
			
			for (L2PcInstance pci : _snoopListener)
			{
				if (pci != null)
				{
					pci.sendPacket(sn);
				}
			}
		}
	}
	
	public void addSnooper(L2PcInstance pci)
	{
		if (!_snoopListener.contains(pci))
		{
			_snoopListener.add(pci);
		}
	}
	
	public void removeSnooper(L2PcInstance pci)
	{
		_snoopListener.remove(pci);
	}
	
	public void addSnooped(L2PcInstance pci)
	{
		if (!_snoopedPlayer.contains(pci))
		{
			_snoopedPlayer.add(pci);
		}
	}
	
	public void removeSnooped(L2PcInstance pci)
	{
		_snoopedPlayer.remove(pci);
	}
	
	public void addBypass(String bypass)
	{
		if (bypass == null)
		{
			return;
		}
		
		_validBypass.add(bypass);
	}
	
	public void addBypass2(String bypass)
	{
		if (bypass == null)
		{
			return;
		}
		
		_validBypass2.add(bypass);
	}
	
	public boolean validateBypass(String cmd)
	{
		if (!Config.BYPASS_VALIDATION)
		{
			return true;
		}
		
		for (String bp : _validBypass)
		{
			if (bp == null)
			{
				continue;
			}
			
			if (bp.equals(cmd))
			{
				return true;
			}
		}
		
		for (String bp : _validBypass2)
		{
			if (bp == null)
			{
				continue;
			}
			
			if (cmd.startsWith(bp))
			{
				return true;
			}
		}
		
		_log.warning("[L2PcInstance] player [" + getName() + "] sent invalid bypass '" + cmd + "'.");
		return false;
	}
	
	/**
	 * Performs following tests:
	 * <ul>
	 * <li>Inventory contains item</li>
	 * <li>Item owner id == owner id</li>
	 * <li>It isnt pet control item while mounting pet or pet summoned</li>
	 * <li>It isnt active enchant item</li>
	 * <li>It isnt cursed weapon/item</li>
	 * <li>It isnt wear item</li>
	 * </ul>
	 * @param objectId item object id
	 * @param action just for login porpouse
	 * @return
	 */
	public boolean validateItemManipulation(int objectId, String action)
	{
		L2ItemInstance item = getInventory().getItemByObjectId(objectId);
		
		if ((item == null) || (item.getOwnerId() != getObjectId()))
		{
			_log.finest(getObjectId() + ": player tried to " + action + " item he is not owner of");
			return false;
		}
		
		// Pet is summoned and not the item that summoned the pet AND not the buggle from strider you're mounting
		if ((hasSummon() && (getSummon().getControlObjectId() == objectId)) || (getMountObjectID() == objectId))
		{
			if (Config.DEBUG)
			{
				_log.finest(getObjectId() + ": player tried to " + action + " item controling pet");
			}
			
			return false;
		}
		
		if (getActiveEnchantItemId() == objectId)
		{
			if (Config.DEBUG)
			{
				_log.finest(getObjectId() + ":player tried to " + action + " an enchant scroll he was using");
			}
			return false;
		}
		
		if (CursedWeaponsManager.getInstance().isCursed(item.getId()))
		{
			// can not trade a cursed weapon
			return false;
		}
		
		return true;
	}
	
	public void clearBypass()
	{
		_validBypass.clear();
		_validBypass2.clear();
	}
	
	/**
	 * @return Returns the inBoat.
	 */
	public boolean isInBoat()
	{
		return (_vehicle != null) && _vehicle.isBoat();
	}
	
	/**
	 * @return
	 */
	public L2BoatInstance getBoat()
	{
		return (L2BoatInstance) _vehicle;
	}
	
	/**
	 * @return Returns the inAirShip.
	 */
	public boolean isInAirShip()
	{
		return (_vehicle != null) && _vehicle.isAirShip();
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
		if ((v == null) && (_vehicle != null))
		{
			_vehicle.removePassenger(this);
		}
		
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
	 * Manage the delete task of a L2PcInstance (Leave Party, Unsummon pet, Save its inventory in the database, Remove it from the world...).<br>
	 * <B><U>Actions</U>:</B>
	 * <ul>
	 * <li>If the L2PcInstance is in observer mode, set its position to its position before entering in observer mode</li>
	 * <li>Set the online Flag to True or False and update the characters table of the database with online status and lastAccess</li>
	 * <li>Stop the HP/MP/CP Regeneration task</li>
	 * <li>Cancel Crafting, Attak or Cast</li>
	 * <li>Remove the L2PcInstance from the world</li>
	 * <li>Stop Party and Unsummon Pet</li>
	 * <li>Update database with items in its inventory and remove them from the world</li>
	 * <li>Remove all L2Object from _knownObjects and _knownPlayer of the L2Character then cancel Attak or Cast and notify AI</li>
	 * <li>Close the connection with the client</li>
	 * </ul>
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
		getEvents().onPlayerLogout();
		
		// Set the online Flag to True or False and update the characters table of the database with online status and lastAccess (called when login and logout)
		try
		{
			if (!isOnline())
			{
				_log.log(Level.SEVERE, "deleteMe() called on offline character " + this, new RuntimeException());
			}
			setOnlineStatus(false, true);
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "deleteMe()", e);
		}
		
		try
		{
			if (Config.ENABLE_BLOCK_CHECKER_EVENT && (getBlockCheckerArena() != -1))
			{
				HandysBlockCheckerManager.getInstance().onDisconnect(this);
			}
		}
		catch (Exception e)
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
		
		// remove combat flag
		try
		{
			if (getInventory().getItemByItemId(9819) != null)
			{
				Fort fort = FortManager.getInstance().getFort(this);
				if (fort != null)
				{
					FortSiegeManager.getInstance().dropCombatFlag(this, fort.getFortId());
				}
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
			CommunityServerThread.getInstance().sendPacket(new WorldInfo(this, null, WorldInfo.TYPE_UPDATE_PLAYER_STATUS));
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
			{
				abortCast();
			}
			
			for (L2Character character : getKnownList().getKnownCharacters())
			{
				if ((character.getFusionSkill() != null) && (character.getFusionSkill().getTarget() == this))
				{
					character.abortCast();
				}
			}
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
		{
			oldRegion.removeFromZones(this);
		}
		
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
		
		if (OlympiadManager.getInstance().isRegistered(this) || (getOlympiadGameId() != -1))
		{
			OlympiadManager.getInstance().removeDisconnectedCompetitor(this);
		}
		
		// If the L2PcInstance has Pet, unsummon it
		if (hasSummon())
		{
			try
			{
				getSummon().setRestoreSummon(true);
				
				getSummon().unSummon(this);
				// Dead pet wasn't unsummoned, broadcast npcinfo changes (pet will be without owner name - means owner offline)
				if (hasSummon())
				{
					getSummon().broadcastNpcInfo(0);
				}
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
				{
					clanMember.setPlayerInstance(null);
				}
				
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
				AdminTable.getInstance().deleteGm(this);
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
			{
				setXYZInvisible(_lastX, _lastY, _lastZ);
			}
			
			if (getVehicle() != null)
			{
				getVehicle().oustPlayer(this);
			}
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "deleteMe()", e);
		}
		
		// remove player from instance and set spawn location if any
		try
		{
			final int instanceId = getInstanceId();
			if ((instanceId != 0) && !Config.RESTORE_PLAYER_INSTANCE)
			{
				final Instance inst = InstanceManager.getInstance().getInstance(instanceId);
				if (inst != null)
				{
					inst.removePlayer(getObjectId());
					final Location loc = inst.getSpawnLoc();
					if (loc != null)
					{
						final int x = loc.getX() + Rnd.get(-30, 30);
						final int y = loc.getY() + Rnd.get(-30, 30);
						setXYZInvisible(x, y, loc.getZ());
						if (hasSummon()) // dead pet
						{
							getSummon().teleToLocation(loc, true);
							getSummon().setInstanceId(0);
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
		{
			WarehouseCacheManager.getInstance().remCacheTask(this);
		}
		
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
		{
			getClan().broadcastToOtherOnlineMembers(new PledgeShowMemberListUpdate(this), this);
			// ClanTable.getInstance().getClan(getClanId()).broadcastToOnlineMembers(new PledgeShowMemberListAdd(this));
		}
		
		for (L2PcInstance player : _snoopedPlayer)
		{
			player.removeSnooper(this);
		}
		
		for (L2PcInstance player : _snoopListener)
		{
			player.removeSnooped(this);
		}
		
		// Remove L2Object object from _allObjects of L2World
		L2World.getInstance().removeObject(this);
		L2World.getInstance().removeFromAllPlayers(this); // force remove in case of crash during teleport
		
		// update bbs
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
	
	private L2Fish _fish;
	
	// startFishing() was stripped of any pre-fishing related checks, namely the fishing zone check.
	// Also worthy of note is the fact the code to find the hook landing position was also striped.
	// The stripped code was moved into fishing.java.
	// In my opinion it makes more sense for it to be there since all other skill related checks were also there.
	// Last but not least, moving the zone check there, fixed a bug where baits would always be consumed no matter if fishing actualy took place.
	// startFishing() now takes up 3 arguments, wich are acurately described as being the hook landing coordinates.
	public void startFishing(int _x, int _y, int _z)
	{
		stopMove(null);
		setIsImmobilized(true);
		_fishing = true;
		_fishx = _x;
		_fishy = _y;
		_fishz = _z;
		// broadcastUserInfo();
		// Starts fishing
		int lvl = getRandomFishLvl();
		int grade = getRandomFishGrade();
		int group = getRandomFishGroup(grade);
		List<L2Fish> fishs = FishData.getInstance().getFish(lvl, group, grade);
		if ((fishs == null) || fishs.isEmpty())
		{
			sendMessage("Error - Fishes are not definied");
			endFishing(false);
			return;
		}
		int check = Rnd.get(fishs.size());
		// Use a copy constructor else the fish data may be over-written below
		_fish = fishs.get(check).clone();
		fishs.clear();
		fishs = null;
		sendPacket(SystemMessageId.CAST_LINE_AND_START_FISHING);
		if (!GameTimeController.getInstance().isNight() && _lure.isNightLure())
		{
			_fish.setFishGroup(-1);
		}
		// sendMessage("Hook x,y: " + _x + "," + _y + " - Water Z, Player Z:" + _z + ", " + getZ()); //debug line, uncoment to show coordinates used in fishing.
		broadcastPacket(new ExFishingStart(this, _fish.getFishGroup(), _x, _y, _z, _lure.isNightLure()));
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
		if (!isDead() && (_taskforfish == null))
		{
			int checkDelay = 0;
			boolean isNoob = false;
			boolean isUpperGrade = false;
			
			if (_lure != null)
			{
				int lureid = _lure.getId();
				isNoob = _fish.getFishGrade() == 0;
				isUpperGrade = _fish.getFishGrade() == 2;
				if ((lureid == 6519) || (lureid == 6522) || (lureid == 6525) || (lureid == 8505) || (lureid == 8508) || (lureid == 8511))
				{
					checkDelay = _fish.getGutsCheckTime() * 133;
				}
				else if ((lureid == 6520) || (lureid == 6523) || (lureid == 6526) || ((lureid >= 8505) && (lureid <= 8513)) || ((lureid >= 7610) && (lureid <= 7613)) || ((lureid >= 7807) && (lureid <= 7809)) || ((lureid >= 8484) && (lureid <= 8486)))
				{
					checkDelay = _fish.getGutsCheckTime() * 100;
				}
				else if ((lureid == 6521) || (lureid == 6524) || (lureid == 6527) || (lureid == 8507) || (lureid == 8510) || (lureid == 8513))
				{
					checkDelay = _fish.getGutsCheckTime() * 66;
				}
			}
			_taskforfish = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new LookingForFishTask(this, _fish.getStartCombatTime(), _fish.getFishGuts(), _fish.getFishGroup(), isNoob, isUpperGrade), 10000, checkDelay);
		}
	}
	
	private int getRandomFishGrade()
	{
		switch (_lure.getId())
		{
			case 7807: // green for beginners
			case 7808: // purple for beginners
			case 7809: // yellow for beginners
			case 8486: // prize-winning for beginners
				return 0;
			case 8485: // prize-winning luminous
			case 8506: // green luminous
			case 8509: // purple luminous
			case 8512: // yellow luminous
				return 2;
			default:
				return 1;
		}
	}
	
	private int getRandomFishGroup(int group)
	{
		int check = Rnd.get(100);
		int type = 1;
		switch (group)
		{
			case 0: // fish for novices
				switch (_lure.getId())
				{
					case 7807: // green lure, preferred by fast-moving (nimble) fish (type 5)
						if (check <= 54)
						{
							type = 5;
						}
						else if (check <= 77)
						{
							type = 4;
						}
						else
						{
							type = 6;
						}
						break;
					case 7808: // purple lure, preferred by fat fish (type 4)
						if (check <= 54)
						{
							type = 4;
						}
						else if (check <= 77)
						{
							type = 6;
						}
						else
						{
							type = 5;
						}
						break;
					case 7809: // yellow lure, preferred by ugly fish (type 6)
						if (check <= 54)
						{
							type = 6;
						}
						else if (check <= 77)
						{
							type = 5;
						}
						else
						{
							type = 4;
						}
						break;
					case 8486: // prize-winning fishing lure for beginners
						if (check <= 33)
						{
							type = 4;
						}
						else if (check <= 66)
						{
							type = 5;
						}
						else
						{
							type = 6;
						}
						break;
				}
				break;
			case 1: // normal fish
				switch (_lure.getId())
				{
					case 7610:
					case 7611:
					case 7612:
					case 7613:
						type = 3;
						break;
					case 6519: // all theese lures (green) are prefered by fast-moving (nimble) fish (type 1)
					case 8505:
					case 6520:
					case 6521:
					case 8507:
						if (check <= 54)
						{
							type = 1;
						}
						else if (check <= 74)
						{
							type = 0;
						}
						else if (check <= 94)
						{
							type = 2;
						}
						else
						{
							type = 3;
						}
						break;
					case 6522: // all theese lures (purple) are prefered by fat fish (type 0)
					case 8508:
					case 6523:
					case 6524:
					case 8510:
						if (check <= 54)
						{
							type = 0;
						}
						else if (check <= 74)
						{
							type = 1;
						}
						else if (check <= 94)
						{
							type = 2;
						}
						else
						{
							type = 3;
						}
						break;
					case 6525: // all theese lures (yellow) are prefered by ugly fish (type 2)
					case 8511:
					case 6526:
					case 6527:
					case 8513:
						if (check <= 55)
						{
							type = 2;
						}
						else if (check <= 74)
						{
							type = 1;
						}
						else if (check <= 94)
						{
							type = 0;
						}
						else
						{
							type = 3;
						}
						break;
					case 8484: // prize-winning fishing lure
						if (check <= 33)
						{
							type = 0;
						}
						else if (check <= 66)
						{
							type = 1;
						}
						else
						{
							type = 2;
						}
						break;
				}
				break;
			case 2: // upper grade fish, luminous lure
				switch (_lure.getId())
				{
					case 8506: // green lure, preferred by fast-moving (nimble) fish (type 8)
						if (check <= 54)
						{
							type = 8;
						}
						else if (check <= 77)
						{
							type = 7;
						}
						else
						{
							type = 9;
						}
						break;
					case 8509: // purple lure, preferred by fat fish (type 7)
						if (check <= 54)
						{
							type = 7;
						}
						else if (check <= 77)
						{
							type = 9;
						}
						else
						{
							type = 8;
						}
						break;
					case 8512: // yellow lure, preferred by ugly fish (type 9)
						if (check <= 54)
						{
							type = 9;
						}
						else if (check <= 77)
						{
							type = 8;
						}
						else
						{
							type = 7;
						}
						break;
					case 8485: // prize-winning fishing lure
						if (check <= 33)
						{
							type = 7;
						}
						else if (check <= 66)
						{
							type = 8;
						}
						else
						{
							type = 9;
						}
						break;
				}
		}
		return type;
	}
	
	private int getRandomFishLvl()
	{
		int skilllvl = getSkillLevel(1315);
		final L2Effect e = getFirstEffect(2274);
		if (e != null)
		{
			skilllvl = (int) e.getSkill().getPower();
		}
		if (skilllvl <= 0)
		{
			return 1;
		}
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
			if (randomlvl > 27)
			{
				randomlvl = 27;
			}
		}
		
		return randomlvl;
	}
	
	public void startFishCombat(boolean isNoob, boolean isUpperGrade)
	{
		_fishCombat = new L2Fishing(this, _fish, isNoob, isUpperGrade);
	}
	
	public void endFishing(boolean win)
	{
		_fishing = false;
		_fishx = 0;
		_fishy = 0;
		_fishz = 0;
		// broadcastUserInfo();
		if (_fishCombat == null)
		{
			sendPacket(SystemMessageId.BAIT_LOST_FISH_GOT_AWAY);
		}
		_fishCombat = null;
		_lure = null;
		// Ends fishing
		broadcastPacket(new ExFishingEnd(win, this));
		sendPacket(SystemMessageId.REEL_LINE_AND_STOP_FISHING);
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
	
	public void setLure(L2ItemInstance lure)
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
		{
			ivlim = Config.INVENTORY_MAXIMUM_GM;
		}
		else if (getRace() == Race.Dwarf)
		{
			ivlim = Config.INVENTORY_MAXIMUM_DWARF;
		}
		else
		{
			ivlim = Config.INVENTORY_MAXIMUM_NO_DWARF;
		}
		ivlim += (int) getStat().calcStat(Stats.INV_LIM, 0, null, null);
		
		return ivlim;
	}
	
	public int getWareHouseLimit()
	{
		int whlim;
		if (getRace() == Race.Dwarf)
		{
			whlim = Config.WAREHOUSE_SLOTS_DWARF;
		}
		else
		{
			whlim = Config.WAREHOUSE_SLOTS_NO_DWARF;
		}
		
		whlim += (int) getStat().calcStat(Stats.WH_LIM, 0, null, null);
		
		return whlim;
	}
	
	public int getPrivateSellStoreLimit()
	{
		int pslim;
		
		if (getRace() == Race.Dwarf)
		{
			pslim = Config.MAX_PVTSTORESELL_SLOTS_DWARF;
		}
		else
		{
			pslim = Config.MAX_PVTSTORESELL_SLOTS_OTHER;
		}
		
		pslim += (int) getStat().calcStat(Stats.P_SELL_LIM, 0, null, null);
		
		return pslim;
	}
	
	public int getPrivateBuyStoreLimit()
	{
		int pblim;
		
		if (getRace() == Race.Dwarf)
		{
			pblim = Config.MAX_PVTSTOREBUY_SLOTS_DWARF;
		}
		else
		{
			pblim = Config.MAX_PVTSTOREBUY_SLOTS_OTHER;
		}
		pblim += (int) getStat().calcStat(Stats.P_BUY_LIM, 0, null, null);
		
		return pblim;
	}
	
	public int getDwarfRecipeLimit()
	{
		int recdlim = Config.DWARF_RECIPE_LIMIT;
		recdlim += (int) getStat().calcStat(Stats.REC_D_LIM, 0, null, null);
		return recdlim;
	}
	
	public int getCommonRecipeLimit()
	{
		int recclim = Config.COMMON_RECIPE_LIMIT;
		recclim += (int) getStat().calcStat(Stats.REC_C_LIM, 0, null, null);
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
	private int _shortBuffTaskSkillId = 0;
	
	/**
	 * @return the current skill in use or return null.
	 */
	public SkillUseHolder getCurrentSkill()
	{
		return _currentSkill;
	}
	
	/**
	 * Create a new SkillDat object and set the player _currentSkill.
	 * @param currentSkill
	 * @param ctrlPressed
	 * @param shiftPressed
	 */
	public void setCurrentSkill(L2Skill currentSkill, boolean ctrlPressed, boolean shiftPressed)
	{
		if (currentSkill == null)
		{
			_currentSkill = null;
			return;
		}
		_currentSkill = new SkillUseHolder(currentSkill, ctrlPressed, shiftPressed);
	}
	
	/**
	 * @return the current pet skill in use or return null.
	 */
	public SkillUseHolder getCurrentPetSkill()
	{
		return _currentPetSkill;
	}
	
	/**
	 * Create a new SkillDat object and set the player _currentPetSkill.
	 * @param currentSkill
	 * @param ctrlPressed
	 * @param shiftPressed
	 */
	public void setCurrentPetSkill(L2Skill currentSkill, boolean ctrlPressed, boolean shiftPressed)
	{
		if (currentSkill == null)
		{
			_currentPetSkill = null;
			return;
		}
		_currentPetSkill = new SkillUseHolder(currentSkill, ctrlPressed, shiftPressed);
	}
	
	public SkillUseHolder getQueuedSkill()
	{
		return _queuedSkill;
	}
	
	/**
	 * Create a new SkillDat object and queue it in the player _queuedSkill.
	 * @param queuedSkill
	 * @param ctrlPressed
	 * @param shiftPressed
	 */
	public void setQueuedSkill(L2Skill queuedSkill, boolean ctrlPressed, boolean shiftPressed)
	{
		if (queuedSkill == null)
		{
			_queuedSkill = null;
			return;
		}
		_queuedSkill = new SkillUseHolder(queuedSkill, ctrlPressed, shiftPressed);
	}
	
	/**
	 * @return {@code true} if player is jailed, {@code false} otherwise.
	 */
	public boolean isJailed()
	{
		return PunishmentManager.getInstance().hasPunishment(getObjectId(), PunishmentAffect.CHARACTER, PunishmentType.JAIL) || PunishmentManager.getInstance().hasPunishment(getAccountName(), PunishmentAffect.ACCOUNT, PunishmentType.JAIL) || PunishmentManager.getInstance().hasPunishment(getIPAddress(), PunishmentAffect.IP, PunishmentType.JAIL);
	}
	
	/**
	 * @return {@code true} if player is chat banned, {@code false} otherwise.
	 */
	public boolean isChatBanned()
	{
		return PunishmentManager.getInstance().hasPunishment(getObjectId(), PunishmentAffect.CHARACTER, PunishmentType.CHAT_BAN) || PunishmentManager.getInstance().hasPunishment(getAccountName(), PunishmentAffect.ACCOUNT, PunishmentType.CHAT_BAN) || PunishmentManager.getInstance().hasPunishment(getIPAddress(), PunishmentAffect.IP, PunishmentType.CHAT_BAN);
	}
	
	public void startFameTask(long delay, int fameFixRate)
	{
		if ((getLevel() < 40) || (getClassId().level() < 2))
		{
			return;
		}
		if (_fameTask == null)
		{
			_fameTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new FameTask(this, fameFixRate), delay, delay);
		}
	}
	
	public void stopFameTask()
	{
		if (_fameTask != null)
		{
			_fameTask.cancel(false);
			_fameTask = null;
		}
	}
	
	public void startVitalityTask()
	{
		if (Config.ENABLE_VITALITY && (_vitalityTask == null))
		{
			_vitalityTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new VitalityTask(this), 1000, 60000);
		}
	}
	
	public void stopVitalityTask()
	{
		if (_vitalityTask != null)
		{
			_vitalityTask.cancel(false);
			_vitalityTask = null;
		}
	}
	
	public int getPowerGrade()
	{
		return _powerGrade;
	}
	
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
	
	public boolean isCombatFlagEquipped()
	{
		return _combatFlagEquippedId;
	}
	
	public void setCombatFlagEquipped(boolean value)
	{
		_combatFlagEquippedId = value;
	}
	
	/**
	 * Returns the Number of Souls this L2PcInstance got.
	 * @return
	 */
	public int getChargedSouls()
	{
		return _souls;
	}
	
	/**
	 * Increase Souls
	 * @param count
	 */
	public void increaseSouls(int count)
	{
		_souls += count;
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
		_souls -= count;
		
		if (getChargedSouls() < 0)
		{
			_souls = 0;
		}
		
		if (getChargedSouls() == 0)
		{
			stopSoulTask();
		}
		else
		{
			restartSoulTask();
		}
		
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
		if (_soulTask != null)
		{
			_soulTask.cancel(false);
			_soulTask = null;
		}
		_soulTask = ThreadPoolManager.getInstance().scheduleGeneral(new ResetSoulsTask(this), 600000);
		
	}
	
	/**
	 * Stops the Clearing Task.
	 */
	public void stopSoulTask()
	{
		if (_soulTask != null)
		{
			_soulTask.cancel(false);
			_soulTask = null;
		}
	}
	
	/**
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
		_shortBuffTask = ThreadPoolManager.getInstance().scheduleGeneral(new ShortBuffTask(this), time * 1000);
		setShortBuffTaskSkillId(magicId);
		
		sendPacket(new ShortBuffStatusUpdate(magicId, level, time));
	}
	
	public int getShortBuffTaskSkillId()
	{
		return _shortBuffTaskSkillId;
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
		if (((getKarma() > 0) || (Rnd.get(1, 100) <= Config.DEATH_PENALTY_CHANCE)) && !(killer instanceof L2PcInstance) && !(canOverrideCond(PcCondOverride.DEATH_PENALTY)) && !(isCharmOfLuckAffected() && killer.isRaid()) && !isPhoenixBlessed() && !isLucky() && !isBlockedFromDeathPenalty() && !(isInsideZone(ZoneId.PVP) || isInsideZone(ZoneId.SIEGE)))
		{
			increaseDeathPenaltyBuffLevel();
		}
	}
	
	public void increaseDeathPenaltyBuffLevel()
	{
		if (getDeathPenaltyBuffLevel() >= 15)
		{
			return;
		}
		
		if (getDeathPenaltyBuffLevel() != 0)
		{
			L2Skill skill = SkillTable.getInstance().getInfo(5076, getDeathPenaltyBuffLevel());
			
			if (skill != null)
			{
				removeSkill(skill, true);
			}
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
		if (getDeathPenaltyBuffLevel() <= 0)
		{
			return;
		}
		
		L2Skill skill = SkillTable.getInstance().getInfo(5076, getDeathPenaltyBuffLevel());
		
		if (skill != null)
		{
			removeSkill(skill, true);
		}
		
		_deathPenaltyBuffLevel--;
		
		if (getDeathPenaltyBuffLevel() > 0)
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
			sendPacket(SystemMessageId.DEATH_PENALTY_LIFTED);
		}
	}
	
	public void restoreDeathPenaltyBuffLevel()
	{
		if (getDeathPenaltyBuffLevel() > 0)
		{
			addSkill(SkillTable.getInstance().getInfo(5076, getDeathPenaltyBuffLevel()), false);
		}
	}
	
	private final Map<Integer, TimeStamp> _reuseTimeStampsItems = new FastMap<>();
	
	@Override
	public void addTimeStampItem(L2ItemInstance item, long reuse)
	{
		_reuseTimeStampsItems.put(item.getObjectId(), new TimeStamp(item, reuse));
	}
	
	public void addTimeStampItem(L2ItemInstance item, long reuse, long systime)
	{
		_reuseTimeStampsItems.put(item.getObjectId(), new TimeStamp(item, reuse, systime));
	}
	
	@Override
	public long getItemRemainingReuseTime(int itemObjId)
	{
		if (!_reuseTimeStampsItems.containsKey(itemObjId))
		{
			return -1;
		}
		return _reuseTimeStampsItems.get(itemObjId).getRemaining();
	}
	
	public long getReuseDelayOnGroup(int group)
	{
		if (group > 0)
		{
			for (TimeStamp ts : _reuseTimeStampsItems.values())
			{
				if ((ts.getSharedReuseGroup() == group) && ts.hasNotPassed())
				{
					return ts.getRemaining();
				}
			}
		}
		return 0;
	}
	
	private final Map<Integer, TimeStamp> _reuseTimeStampsSkills = new FastMap<>();
	
	public Map<Integer, TimeStamp> getSkillReuseTimeStamps()
	{
		return _reuseTimeStampsSkills;
	}
	
	@Override
	public long getSkillRemainingReuseTime(int skillReuseHashId)
	{
		if (!_reuseTimeStampsSkills.containsKey(skillReuseHashId))
		{
			return -1;
		}
		return _reuseTimeStampsSkills.get(skillReuseHashId).getRemaining();
	}
	
	public boolean hasSkillReuse(int skillReuseHashId)
	{
		if (!_reuseTimeStampsSkills.containsKey(skillReuseHashId))
		{
			return false;
		}
		return _reuseTimeStampsSkills.get(skillReuseHashId).hasNotPassed();
	}
	
	public TimeStamp getSkillReuseTimeStamp(int skillReuseHashId)
	{
		return _reuseTimeStampsSkills.get(skillReuseHashId);
	}
	
	/**
	 * Index according to skill id the current timestamp of use.
	 * @param skill
	 * @param reuse delay
	 */
	@Override
	public void addTimeStamp(L2Skill skill, long reuse)
	{
		_reuseTimeStampsSkills.put(skill.getReuseHashCode(), new TimeStamp(skill, reuse));
	}
	
	/**
	 * Index according to skill this TimeStamp instance for restoration purposes only.
	 * @param skill
	 * @param reuse
	 * @param systime
	 */
	public void addTimeStamp(L2Skill skill, long reuse, long systime)
	{
		_reuseTimeStampsSkills.put(skill.getReuseHashCode(), new TimeStamp(skill, reuse, systime));
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
			if (target.isPlayer())
			{
				SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_EVADED_C2_ATTACK);
				sm.addPcName(target.getActingPlayer());
				sm.addCharName(this);
				target.sendPacket(sm);
			}
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_ATTACK_WENT_ASTRAY);
			sm.addPcName(this);
			sendPacket(sm);
			return;
		}
		
		// Check if hit is critical
		if (pcrit)
		{
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_HAD_CRITICAL_HIT);
			sm.addPcName(this);
			sendPacket(sm);
		}
		if (mcrit)
		{
			sendPacket(SystemMessageId.CRITICAL_HIT_MAGIC);
		}
		
		if (isInOlympiadMode() && target.isPlayer() && target.getActingPlayer().isInOlympiadMode() && (target.getActingPlayer().getOlympiadGameId() == getOlympiadGameId()))
		{
			OlympiadGameManager.getInstance().notifyCompetitorDamage(this, damage);
		}
		
		final SystemMessage sm;
		
		if (target.isInvul() && !target.isNpc())
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.ATTACK_WAS_BLOCKED);
		}
		else if (target.isDoor() || (target instanceof L2ControlTowerInstance))
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_DID_S1_DMG);
			sm.addNumber(damage);
		}
		else
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.C1_DONE_S3_DAMAGE_TO_C2);
			sm.addPcName(this);
			sm.addCharName(target);
			sm.addNumber(damage);
		}
		sendPacket(sm);
	}
	
	/**
	 * @param npcId
	 */
	public void setAgathionId(int npcId)
	{
		_agathionId = npcId;
	}
	
	/**
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
	
	public void checkItemRestriction()
	{
		for (int i = 0; i < Inventory.PAPERDOLL_TOTALSLOTS; i++)
		{
			L2ItemInstance equippedItem = getInventory().getPaperdollItem(i);
			if ((equippedItem != null) && !equippedItem.getItem().checkCondition(this, this, false))
			{
				getInventory().unEquipItemInSlot(i);
				
				InventoryUpdate iu = new InventoryUpdate();
				iu.addModifiedItem(equippedItem);
				sendPacket(iu);
				
				SystemMessage sm = null;
				if (equippedItem.getItem().getBodyPart() == L2Item.SLOT_BACK)
				{
					sendPacket(SystemMessageId.CLOAK_REMOVED_BECAUSE_ARMOR_SET_REMOVED);
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
	
	public void addTransformSkill(int id)
	{
		if (_transformAllowedSkills == null)
		{
			synchronized (this)
			{
				if (_transformAllowedSkills == null)
				{
					_transformAllowedSkills = new HashSet<>();
				}
			}
		}
		_transformAllowedSkills.add(id);
	}
	
	public boolean hasTransformSkill(int id)
	{
		return (_transformAllowedSkills != null) && _transformAllowedSkills.contains(id);
	}
	
	public synchronized void removeAllTransformSkills()
	{
		_transformAllowedSkills = null;
	}
	
	protected void startFeed(int npcId)
	{
		_canFeed = npcId > 0;
		if (!isMounted())
		{
			return;
		}
		if (hasSummon())
		{
			setCurrentFeed(((L2PetInstance) getSummon()).getCurrentFed());
			_controlItemId = getSummon().getControlObjectId();
			sendPacket(new SetupGauge(3, (getCurrentFeed() * 10000) / getFeedConsume(), (getMaxFeed() * 10000) / getFeedConsume()));
			if (!isDead())
			{
				_mountFeedTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new PetFeedTask(this), 10000, 10000);
			}
		}
		else if (_canFeed)
		{
			setCurrentFeed(getMaxFeed());
			SetupGauge sg = new SetupGauge(3, (getCurrentFeed() * 10000) / getFeedConsume(), (getMaxFeed() * 10000) / getFeedConsume());
			sendPacket(sg);
			if (!isDead())
			{
				_mountFeedTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new PetFeedTask(this), 10000, 10000);
			}
		}
	}
	
	public void stopFeed()
	{
		if (_mountFeedTask != null)
		{
			_mountFeedTask.cancel(false);
			_mountFeedTask = null;
		}
	}
	
	private final void clearPetData()
	{
		_data = null;
	}
	
	public final L2PetData getPetData(int npcId)
	{
		if (_data == null)
		{
			_data = PetDataTable.getInstance().getPetData(npcId);
		}
		return _data;
	}
	
	private final L2PetLevelData getPetLevelData(int npcId)
	{
		if (_leveldata == null)
		{
			_leveldata = PetDataTable.getInstance().getPetData(npcId).getPetLevelData(getMountLevel());
		}
		return _leveldata;
	}
	
	public int getCurrentFeed()
	{
		return _curFeed;
	}
	
	public int getFeedConsume()
	{
		// if pet is attacking
		if (isAttackingNow())
		{
			return getPetLevelData(_mountNpcId).getPetFeedBattle();
		}
		return getPetLevelData(_mountNpcId).getPetFeedNormal();
	}
	
	public void setCurrentFeed(int num)
	{
		boolean lastHungryState = isHungry();
		_curFeed = num > getMaxFeed() ? getMaxFeed() : num;
		SetupGauge sg = new SetupGauge(3, (getCurrentFeed() * 10000) / getFeedConsume(), (getMaxFeed() * 10000) / getFeedConsume());
		sendPacket(sg);
		// broadcast move speed change when strider becomes hungry / full
		if (lastHungryState != isHungry())
		{
			broadcastUserInfo();
		}
	}
	
	private int getMaxFeed()
	{
		return getPetLevelData(_mountNpcId).getPetMaxFeed();
	}
	
	public boolean isHungry()
	{
		return _canFeed ? (getCurrentFeed() < ((getPetData(getMountNpcId()).getHungryLimit() / 100f) * getPetLevelData(getMountNpcId()).getPetMaxFeed())) : false;
	}
	
	public void enteredNoLanding(int delay)
	{
		_dismountTask = ThreadPoolManager.getInstance().scheduleGeneral(new DismountTask(this), delay * 1000);
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
		if ((_controlItemId != 0) && (petId != 0))
		{
			String req;
			req = "UPDATE pets SET fed=? WHERE item_obj_id = ?";
			try (Connection con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement(req))
			{
				statement.setInt(1, getCurrentFeed());
				statement.setInt(2, _controlItemId);
				statement.executeUpdate();
				_controlItemId = 0;
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "Failed to store Pet [NpcId: " + petId + "] data", e);
			}
		}
	}
	
	/** End of section for mounted pets */
	
	/**
	 * @return event effect id
	 */
	public int getEventEffectId()
	{
		return _eventEffectId;
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
	
	/**
	 * @param isInHideoutSiege sets the value of {@link #_isInHideoutSiege}.
	 */
	public void setIsInHideoutSiege(boolean isInHideoutSiege)
	{
		_isInHideoutSiege = isInHideoutSiege;
	}
	
	/**
	 * @return the value of {@link #_isInHideoutSiege}, {@code true} if the player is participing on a Hideout Siege, otherwise {@code false}.
	 */
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
		return (isTransformed() && (getTransformation().isFlying()));
	}
	
	/**
	 * Returns the Number of Charges this L2PcInstance got.
	 * @return
	 */
	public int getCharges()
	{
		return _charges.get();
	}
	
	public void increaseCharges(int count, int max)
	{
		if (_charges.get() >= max)
		{
			sendPacket(SystemMessageId.FORCE_MAXLEVEL_REACHED);
			return;
		}
		
		// Charge clear task should be reset every time a charge is increased.
		restartChargeTask();
		
		if (_charges.addAndGet(count) >= max)
		{
			_charges.set(max);
			sendPacket(SystemMessageId.FORCE_MAXLEVEL_REACHED);
		}
		else
		{
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.FORCE_INCREASED_TO_S1);
			sm.addNumber(_charges.get());
			sendPacket(sm);
		}
		
		sendPacket(new EtcStatusUpdate(this));
	}
	
	public boolean decreaseCharges(int count)
	{
		if (_charges.get() < count)
		{
			return false;
		}
		
		// Charge clear task should be reset every time a charge is decreased and stopped when charges become 0.
		if (_charges.addAndGet(-count) == 0)
		{
			stopChargeTask();
		}
		else
		{
			restartChargeTask();
		}
		
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
		_chargeTask = ThreadPoolManager.getInstance().scheduleGeneral(new ResetChargesTask(this), 600000);
	}
	
	/**
	 * Stops the Charges Clearing Task.
	 */
	public void stopChargeTask()
	{
		if (_chargeTask != null)
		{
			_chargeTask.cancel(false);
			_chargeTask = null;
		}
	}
	
	public void teleportBookmarkModify(int id, int icon, String tag, String name)
	{
		TeleportBookmark bookmark = _tpbookmarks.get(id);
		if (bookmark != null)
		{
			bookmark.setIcon(icon);
			bookmark.setTag(tag);
			bookmark.setName(name);
			
			try (Connection con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement(UPDATE_TP_BOOKMARK))
			{
				statement.setInt(1, icon);
				statement.setString(2, tag);
				statement.setString(3, name);
				statement.setInt(4, getObjectId());
				statement.setInt(5, id);
				statement.execute();
			}
			catch (Exception e)
			{
				_log.log(Level.WARNING, "Could not update character teleport bookmark data: " + e.getMessage(), e);
			}
		}
		
		sendPacket(new ExGetBookMarkInfoPacket(this));
	}
	
	public void teleportBookmarkDelete(int id)
	{
		if (_tpbookmarks.remove(id) != null)
		{
			try (Connection con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement(DELETE_TP_BOOKMARK))
			{
				statement.setInt(1, getObjectId());
				statement.setInt(2, id);
				statement.execute();
			}
			catch (Exception e)
			{
				_log.log(Level.WARNING, "Could not delete character teleport bookmark data: " + e.getMessage(), e);
			}
			
			sendPacket(new ExGetBookMarkInfoPacket(this));
		}
	}
	
	public void teleportBookmarkGo(int id)
	{
		if (!teleportBookmarkCondition(0))
		{
			return;
		}
		if (getInventory().getInventoryItemCount(13016, 0) == 0)
		{
			sendPacket(SystemMessageId.YOU_CANNOT_TELEPORT_BECAUSE_YOU_DO_NOT_HAVE_A_TELEPORT_ITEM);
			return;
		}
		SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_DISAPPEARED);
		sm.addItemName(13016);
		sendPacket(sm);
		
		final TeleportBookmark bookmark = _tpbookmarks.get(id);
		if (bookmark != null)
		{
			destroyItem("Consume", getInventory().getItemByItemId(13016).getObjectId(), 1, null, false);
			teleToLocation(bookmark, false);
		}
		sendPacket(new ExGetBookMarkInfoPacket(this));
	}
	
	public boolean teleportBookmarkCondition(int type)
	{
		if (isInCombat())
		{
			sendPacket(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_DURING_A_BATTLE);
			return false;
		}
		else if (isInSiege() || (getSiegeState() != 0))
		{
			sendPacket(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_WHILE_PARTICIPATING);
			return false;
		}
		else if (isInDuel())
		{
			sendPacket(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_DURING_A_DUEL);
			return false;
		}
		else if (isFlying())
		{
			sendPacket(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_WHILE_FLYING);
			return false;
		}
		else if (isInOlympiadMode())
		{
			sendPacket(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_WHILE_PARTICIPATING_IN_AN_OLYMPIAD_MATCH);
			return false;
		}
		else if (isParalyzed())
		{
			sendPacket(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_WHILE_YOU_ARE_PARALYZED);
			return false;
		}
		else if (isDead())
		{
			sendPacket(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_WHILE_YOU_ARE_DEAD);
			return false;
		}
		else if ((type == 1) && (isIn7sDungeon() || (isInParty() && getParty().isInDimensionalRift())))
		{
			sendPacket(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_TO_REACH_THIS_AREA);
			return false;
		}
		else if (isInWater())
		{
			sendPacket(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_UNDERWATER);
			return false;
		}
		else if ((type == 1) && (isInsideZone(ZoneId.SIEGE) || isInsideZone(ZoneId.CLAN_HALL) || isInsideZone(ZoneId.JAIL) || isInsideZone(ZoneId.CASTLE) || isInsideZone(ZoneId.NO_SUMMON_FRIEND) || isInsideZone(ZoneId.FORT)))
		{
			sendPacket(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_TO_REACH_THIS_AREA);
			return false;
		}
		else if (isInsideZone(ZoneId.NO_BOOKMARK) || isInBoat() || isInAirShip())
		{
			if (type == 0)
			{
				sendPacket(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_IN_THIS_AREA);
			}
			else if (type == 1)
			{
				sendPacket(SystemMessageId.YOU_CANNOT_USE_MY_TELEPORTS_TO_REACH_THIS_AREA);
			}
			return false;
		}
		/*
		 * TODO: Instant Zone still not implemented else if (isInsideZone(ZoneId.INSTANT)) { sendPacket(SystemMessage.getSystemMessage(2357)); return; }
		 */
		else
		{
			return true;
		}
	}
	
	public void teleportBookmarkAdd(int x, int y, int z, int icon, String tag, String name)
	{
		if (!teleportBookmarkCondition(1))
		{
			return;
		}
		
		if (_tpbookmarks.size() >= _bookmarkslot)
		{
			sendPacket(SystemMessageId.YOU_HAVE_NO_SPACE_TO_SAVE_THE_TELEPORT_LOCATION);
			return;
		}
		
		if (getInventory().getInventoryItemCount(20033, 0) == 0)
		{
			sendPacket(SystemMessageId.YOU_CANNOT_BOOKMARK_THIS_LOCATION_BECAUSE_YOU_DO_NOT_HAVE_A_MY_TELEPORT_FLAG);
			return;
		}
		
		int id = _tpbookmarks.size();
		_tpbookmarks.put(id, new TeleportBookmark(id, x, y, z, icon, tag, name));
		
		destroyItem("Consume", getInventory().getItemByItemId(20033).getObjectId(), 1, null, false);
		
		SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_DISAPPEARED);
		sm.addItemName(20033);
		sendPacket(sm);
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(INSERT_TP_BOOKMARK))
		{
			statement.setInt(1, getObjectId());
			statement.setInt(2, id);
			statement.setInt(3, x);
			statement.setInt(4, y);
			statement.setInt(5, z);
			statement.setInt(6, icon);
			statement.setString(7, tag);
			statement.setString(8, name);
			statement.execute();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Could not insert character teleport bookmark data: " + e.getMessage(), e);
		}
		sendPacket(new ExGetBookMarkInfoPacket(this));
	}
	
	public void restoreTeleportBookmark()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(RESTORE_TP_BOOKMARK))
		{
			statement.setInt(1, getObjectId());
			try (ResultSet rset = statement.executeQuery())
			{
				while (rset.next())
				{
					_tpbookmarks.put(rset.getInt("Id"), new TeleportBookmark(rset.getInt("Id"), rset.getInt("x"), rset.getInt("y"), rset.getInt("z"), rset.getInt("icon"), rset.getString("tag"), rset.getString("name")));
				}
			}
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Failed restoing character teleport bookmark.", e);
		}
	}
	
	@Override
	public void sendInfo(L2PcInstance activeChar)
	{
		if (isInBoat())
		{
			getPosition().setWorldPosition(getBoat().getPosition().getWorldPosition());
			
			activeChar.sendPacket(new CharInfo(this));
			activeChar.sendPacket(new ExBrExtraUserInfo(this));
			int relation1 = getRelation(activeChar);
			int relation2 = activeChar.getRelation(this);
			Integer oldrelation = getKnownList().getKnownRelations().get(activeChar.getObjectId());
			if ((oldrelation != null) && (oldrelation != relation1))
			{
				activeChar.sendPacket(new RelationChanged(this, relation1, isAutoAttackable(activeChar)));
				if (hasSummon())
				{
					activeChar.sendPacket(new RelationChanged(getSummon(), relation1, isAutoAttackable(activeChar)));
				}
			}
			oldrelation = activeChar.getKnownList().getKnownRelations().get(getObjectId());
			if ((oldrelation != null) && (oldrelation != relation2))
			{
				sendPacket(new RelationChanged(activeChar, relation2, activeChar.isAutoAttackable(this)));
				if (activeChar.hasSummon())
				{
					sendPacket(new RelationChanged(activeChar.getSummon(), relation2, activeChar.isAutoAttackable(this)));
				}
			}
			activeChar.sendPacket(new GetOnVehicle(getObjectId(), getBoat().getObjectId(), getInVehiclePosition()));
		}
		else if (isInAirShip())
		{
			getPosition().setWorldPosition(getAirShip().getPosition().getWorldPosition());
			
			activeChar.sendPacket(new CharInfo(this));
			activeChar.sendPacket(new ExBrExtraUserInfo(this));
			int relation1 = getRelation(activeChar);
			int relation2 = activeChar.getRelation(this);
			Integer oldrelation = getKnownList().getKnownRelations().get(activeChar.getObjectId());
			if ((oldrelation != null) && (oldrelation != relation1))
			{
				activeChar.sendPacket(new RelationChanged(this, relation1, isAutoAttackable(activeChar)));
				if (hasSummon())
				{
					activeChar.sendPacket(new RelationChanged(getSummon(), relation1, isAutoAttackable(activeChar)));
				}
			}
			oldrelation = activeChar.getKnownList().getKnownRelations().get(getObjectId());
			if ((oldrelation != null) && (oldrelation != relation2))
			{
				sendPacket(new RelationChanged(activeChar, relation2, activeChar.isAutoAttackable(this)));
				if (activeChar.hasSummon())
				{
					sendPacket(new RelationChanged(activeChar.getSummon(), relation2, activeChar.isAutoAttackable(this)));
				}
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
			if ((oldrelation != null) && (oldrelation != relation1))
			{
				activeChar.sendPacket(new RelationChanged(this, relation1, isAutoAttackable(activeChar)));
				if (hasSummon())
				{
					activeChar.sendPacket(new RelationChanged(getSummon(), relation1, isAutoAttackable(activeChar)));
				}
			}
			oldrelation = activeChar.getKnownList().getKnownRelations().get(getObjectId());
			if ((oldrelation != null) && (oldrelation != relation2))
			{
				sendPacket(new RelationChanged(activeChar, relation2, activeChar.isAutoAttackable(this)));
				if (activeChar.hasSummon())
				{
					sendPacket(new RelationChanged(activeChar.getSummon(), relation2, activeChar.isAutoAttackable(this)));
				}
			}
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
		if (_movieId > 0)
		{
			return;
		}
		abortAttack();
		abortCast();
		stopMove(null);
		_movieId = id;
		sendPacket(new ExStartScenePlayer(id));
	}
	
	public boolean isAllowedToEnchantSkills()
	{
		if (isLocked())
		{
			return false;
		}
		if (isTransformed() || isInStance())
		{
			return false;
		}
		if (AttackStanceTaskManager.getInstance().hasAttackStanceTask(this))
		{
			return false;
		}
		if (isCastingNow() || isCastingSimultaneouslyNow())
		{
			return false;
		}
		if (isInBoat() || isInAirShip())
		{
			return false;
		}
		return true;
	}
	
	/**
	 * Set the _createDate of the L2PcInstance.
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
	 * @return number of days to char birthday.
	 */
	public int checkBirthDay()
	{
		Calendar now = Calendar.getInstance();
		
		// "Characters with a February 29 creation date will receive a gift on February 28."
		if ((_createDate.get(Calendar.DAY_OF_MONTH) == 29) && (_createDate.get(Calendar.MONTH) == 1))
		{
			_createDate.add(Calendar.HOUR_OF_DAY, -24);
		}
		
		if ((now.get(Calendar.MONTH) == _createDate.get(Calendar.MONTH)) && (now.get(Calendar.DAY_OF_MONTH) == _createDate.get(Calendar.DAY_OF_MONTH)) && (now.get(Calendar.YEAR) != _createDate.get(Calendar.YEAR)))
		{
			return 0;
		}
		
		int i;
		for (i = 1; i < 6; i++)
		{
			now.add(Calendar.HOUR_OF_DAY, 24);
			if ((now.get(Calendar.MONTH) == _createDate.get(Calendar.MONTH)) && (now.get(Calendar.DAY_OF_MONTH) == _createDate.get(Calendar.DAY_OF_MONTH)) && (now.get(Calendar.YEAR) != _createDate.get(Calendar.YEAR)))
			{
				return i;
			}
		}
		return -1;
	}
	
	/**
	 * list of character friends
	 */
	private final List<Integer> _friendList = new FastList<>();
	
	public List<Integer> getFriendList()
	{
		return _friendList;
	}
	
	public void restoreFriendList()
	{
		_friendList.clear();
		
		final String sqlQuery = "SELECT friendId FROM character_friends WHERE charId=? AND relation=0";
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(sqlQuery))
		{
			statement.setInt(1, getObjectId());
			try (ResultSet rset = statement.executeQuery())
			{
				while (rset.next())
				{
					int friendId = rset.getInt("friendId");
					if (friendId == getObjectId())
					{
						continue;
					}
					_friendList.add(friendId);
				}
			}
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Error found in " + getName() + "'s FriendList: " + e.getMessage(), e);
		}
	}
	
	private void notifyFriends()
	{
		FriendStatusPacket pkt = new FriendStatusPacket(getObjectId());
		for (int id : _friendList)
		{
			L2PcInstance friend = L2World.getInstance().getPlayer(id);
			if (friend != null)
			{
				friend.sendPacket(pkt);
			}
		}
	}
	
	/**
	 * Verify if this player is in silence mode.
	 * @return the {@code true} if this player is in silence mode, {@code false} otherwise
	 */
	public boolean isSilenceMode()
	{
		return _silenceMode;
	}
	
	/**
	 * While at silenceMode, checks if this player blocks PMs for this user
	 * @param playerObjId the player object Id
	 * @return {@code true} if the given Id is not excluded and this player is in silence mode, {@code false} otherwise
	 */
	public boolean isSilenceMode(int playerObjId)
	{
		if (Config.SILENCE_MODE_EXCLUDE && _silenceMode && (_silenceModeExcluded != null))
		{
			return !_silenceModeExcluded.contains(playerObjId);
		}
		return _silenceMode;
	}
	
	/**
	 * Set the silence mode.
	 * @param mode the value
	 */
	public void setSilenceMode(boolean mode)
	{
		_silenceMode = mode;
		if (_silenceModeExcluded != null)
		{
			_silenceModeExcluded.clear(); // Clear the excluded list on each setSilenceMode
		}
		sendPacket(new EtcStatusUpdate(this));
	}
	
	/**
	 * Add a player to the "excluded silence mode" list.
	 * @param playerObjId the player's object Id
	 */
	public void addSilenceModeExcluded(int playerObjId)
	{
		if (_silenceModeExcluded == null)
		{
			_silenceModeExcluded = new ArrayList<>(1);
		}
		_silenceModeExcluded.add(playerObjId);
	}
	
	private void storeRecipeShopList()
	{
		if (hasManufactureShop())
		{
			try (Connection con = L2DatabaseFactory.getInstance().getConnection())
			{
				try (PreparedStatement st = con.prepareStatement("DELETE FROM character_recipeshoplist WHERE charId=?"))
				{
					st.setInt(1, getObjectId());
					st.execute();
				}
				
				try (PreparedStatement st = con.prepareStatement("INSERT INTO character_recipeshoplist (`charId`, `recipeId`, `price`, `index`) VALUES (?, ?, ?, ?)"))
				{
					int i = 1;
					for (L2ManufactureItem item : _manufactureItems.values())
					{
						st.setInt(1, getObjectId());
						st.setInt(2, item.getRecipeId());
						st.setLong(3, item.getCost());
						st.setInt(4, i++);
						st.addBatch();
					}
					st.executeBatch();
				}
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "Could not store recipe shop for playerId " + getObjectId() + ": ", e);
			}
		}
	}
	
	private void restoreRecipeShopList()
	{
		if (_manufactureItems != null)
		{
			_manufactureItems.clear();
		}
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT * FROM character_recipeshoplist WHERE charId=? ORDER BY `index`"))
		{
			statement.setInt(1, getObjectId());
			try (ResultSet rset = statement.executeQuery())
			{
				while (rset.next())
				{
					getManufactureItems().put(rset.getInt("recipeId"), new L2ManufactureItem(rset.getInt("recipeId"), rset.getLong("price")));
				}
			}
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Could not restore recipe shop list data for playerId: " + getObjectId(), e);
		}
	}
	
	public double getCollisionRadius()
	{
		if (isMounted() && (getMountNpcId() > 0))
		{
			return NpcTable.getInstance().getTemplate(getMountNpcId()).getfCollisionRadius();
		}
		else if (isTransformed())
		{
			return getTransformation().getCollisionRadius(this);
		}
		return getAppearance().getSex() ? getBaseTemplate().getFCollisionRadiusFemale() : getBaseTemplate().getfCollisionRadius();
	}
	
	public double getCollisionHeight()
	{
		if (isMounted() && (getMountNpcId() > 0))
		{
			return NpcTable.getInstance().getTemplate(getMountNpcId()).getfCollisionHeight();
		}
		else if (isTransformed())
		{
			return getTransformation().getCollisionHeight(this);
		}
		return getAppearance().getSex() ? getBaseTemplate().getFCollisionHeightFemale() : getBaseTemplate().getfCollisionHeight();
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
		_clientX = val;
	}
	
	public final void setClientY(int val)
	{
		_clientY = val;
	}
	
	public final void setClientZ(int val)
	{
		_clientZ = val;
	}
	
	public final void setClientHeading(int val)
	{
		_clientHeading = val;
	}
	
	/**
	 * @param z
	 * @return true if character falling now on the start of fall return false for correct coord sync!
	 */
	public final boolean isFalling(int z)
	{
		if (isDead() || isFlying() || isFlyingMounted() || isInsideZone(ZoneId.WATER))
		{
			return false;
		}
		
		if (System.currentTimeMillis() < _fallingTimestamp)
		{
			return true;
		}
		
		final int deltaZ = getZ() - z;
		if (deltaZ <= getBaseTemplate().getSafeFallHeight())
		{
			return false;
		}
		
		final int damage = (int) Formulas.calcFallDam(this, deltaZ);
		if (damage > 0)
		{
			reduceCurrentHp(Math.min(damage, getCurrentHp() - 1), null, false, true, null);
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.FALL_DAMAGE_S1);
			sm.addNumber(damage);
			sendPacket(sm);
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
	 *         (last request was in 2 seconds before)
	 */
	public boolean isItemAuctionPolling()
	{
		return (System.currentTimeMillis() - _lastItemAuctionInfoRequest) < 2000;
	}
	
	@Override
	public boolean isMovementDisabled()
	{
		return super.isMovementDisabled() || (_movieId > 0);
	}
	
	private void restoreUISettings()
	{
		_uiKeySettings = new UIKeysSettings(getObjectId());
	}
	
	private void storeUISettings()
	{
		if (_uiKeySettings == null)
		{
			return;
		}
		
		if (!_uiKeySettings.isSaved())
		{
			_uiKeySettings.saveInDB();
		}
	}
	
	public UIKeysSettings getUISettings()
	{
		return _uiKeySettings;
	}
	
	public String getHtmlPrefix()
	{
		if (!Config.L2JMOD_MULTILANG_ENABLE)
		{
			return null;
		}
		
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
			{
				_lang = Config.L2JMOD_MULTILANG_DEFAULT;
			}
			
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
		L2SkillLearn learn;
		for (Entry<Integer, L2Skill> e : getSkills().entrySet())
		{
			learn = SkillTreesData.getInstance().getClassSkill(e.getKey(), e.getValue().getLevel() % 100, getClassId());
			if (learn != null)
			{
				int lvlDiff = e.getKey() == L2Skill.SKILL_EXPERTISE ? 0 : 9;
				if (getLevel() < (learn.getGetLevel() - lvlDiff))
				{
					deacreaseSkillLevel(e.getValue(), lvlDiff);
				}
			}
		}
	}
	
	private void deacreaseSkillLevel(L2Skill skill, int lvlDiff)
	{
		int nextLevel = -1;
		final Map<Integer, L2SkillLearn> skillTree = SkillTreesData.getInstance().getCompleteClassSkillTree(getClassId());
		for (L2SkillLearn sl : skillTree.values())
		{
			if ((sl.getSkillId() == skill.getId()) && (nextLevel < sl.getSkillLevel()) && (getLevel() >= (sl.getGetLevel() - lvlDiff)))
			{
				nextLevel = sl.getSkillLevel(); // next possible skill level
			}
		}
		
		if (nextLevel == -1)
		{
			_log.info("Removing skill " + skill + " from player " + toString());
			removeSkill(skill, true); // there is no lower skill
		}
		else
		{
			_log.info("Decreasing skill " + skill + " to " + nextLevel + " for player " + toString());
			addSkill(SkillTable.getInstance().getInfo(skill.getId(), nextLevel), true); // replace with lower one
		}
	}
	
	public boolean canMakeSocialAction()
	{
		return ((getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_NONE) && (getActiveRequester() == null) && !isAlikeDead() && !isAllSkillsDisabled() && !isInDuel() && !isCastingNow() && !isCastingSimultaneouslyNow() && (getAI().getIntention() == CtrlIntention.AI_INTENTION_IDLE) && !AttackStanceTaskManager.getInstance().hasAttackStanceTask(this) && !isInOlympiadMode());
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
	
	public Collection<TeleportBookmark> getTeleportBookmarks()
	{
		return _tpbookmarks.values();
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
		if (cha instanceof L2Attackable)
		{
			return true;
		}
		else if (cha instanceof L2Playable)
		{
			if (cha.isInsideZone(ZoneId.PVP) && !cha.isInsideZone(ZoneId.SIEGE))
			{
				return true;
			}
			
			L2PcInstance target;
			if (cha instanceof L2Summon)
			{
				target = ((L2Summon) cha).getOwner();
			}
			else
			{
				target = (L2PcInstance) cha;
			}
			
			if (isInDuel() && target.isInDuel() && (target.getDuelId() == getDuelId()))
			{
				return true;
			}
			else if (isInParty() && target.isInParty())
			{
				if (getParty() == target.getParty())
				{
					return false;
				}
				if (((getParty().getCommandChannel() != null) || (target.getParty().getCommandChannel() != null)) && (getParty().getCommandChannel() == target.getParty().getCommandChannel()))
				{
					return false;
				}
			}
			else if ((getClan() != null) && (target.getClan() != null))
			{
				if (getClanId() == target.getClanId())
				{
					return false;
				}
				if (((getAllyId() > 0) || (target.getAllyId() > 0)) && (getAllyId() == target.getAllyId()))
				{
					return false;
				}
				if (getClan().isAtWarWith(target.getClan().getClanId()) && target.getClan().isAtWarWith(getClan().getClanId()))
				{
					return true;
				}
			}
			else if ((getClan() == null) || (target.getClan() == null))
			{
				if ((target.getPvpFlag() == 0) && (target.getKarma() == 0))
				{
					return false;
				}
			}
		}
		return true;
	}
	
	/**
	 * Test if player inventory is under 90% capaity
	 * @param includeQuestInv check also quest inventory
	 * @return
	 */
	public boolean isInventoryUnder90(boolean includeQuestInv)
	{
		return (getInventory().getSize(includeQuestInv) <= (getInventoryLimit() * 0.9));
	}
	
	public boolean havePetInvItems()
	{
		return _petItems;
	}
	
	public void setPetInvItems(boolean haveit)
	{
		_petItems = haveit;
	}
	
	/**
	 * Restore Pet's inventory items from database.
	 */
	private void restorePetInventoryItems()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT object_id FROM `items` WHERE `owner_id`=? AND (`loc`='PET' OR `loc`='PET_EQUIP') LIMIT 1;"))
		{
			statement.setInt(1, getObjectId());
			try (ResultSet rset = statement.executeQuery())
			{
				if (rset.next() && (rset.getInt("object_id") > 0))
				{
					setPetInvItems(true);
				}
				else
				{
					setPetInvItems(false);
				}
			}
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Could not check Items in Pet Inventory for playerId: " + getObjectId(), e);
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
	 * Load L2PcInstance Recommendations data.
	 * @return
	 */
	private long loadRecommendations()
	{
		long _time_left = 0;
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT rec_have,rec_left,time_left FROM character_reco_bonus WHERE charId=? LIMIT 1"))
		{
			statement.setInt(1, getObjectId());
			try (ResultSet rset = statement.executeQuery())
			{
				if (rset.next())
				{
					setRecomHave(rset.getInt("rec_have"));
					setRecomLeft(rset.getInt("rec_left"));
					_time_left = rset.getLong("time_left");
				}
				else
				{
					_time_left = 3600000;
				}
			}
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Could not restore Recommendations for player: " + getObjectId(), e);
		}
		return _time_left;
	}
	
	/**
	 * Update L2PcInstance Recommendations data.
	 */
	public void storeRecommendations()
	{
		long recoTaskEnd = 0;
		if (_recoBonusTask != null)
		{
			recoTaskEnd = Math.max(0, _recoBonusTask.getDelay(TimeUnit.MILLISECONDS));
		}
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("INSERT INTO character_reco_bonus (charId,rec_have,rec_left,time_left) VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE rec_have=?, rec_left=?, time_left=?"))
		{
			statement.setInt(1, getObjectId());
			statement.setInt(2, getRecomHave());
			statement.setInt(3, getRecomLeft());
			statement.setLong(4, recoTaskEnd);
			// Update part
			statement.setInt(5, getRecomHave());
			statement.setInt(6, getRecomLeft());
			statement.setLong(7, recoTaskEnd);
			statement.execute();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Could not update Recommendations for player: " + getObjectId(), e);
		}
	}
	
	public void checkRecoBonusTask()
	{
		// Load data
		long taskTime = loadRecommendations();
		
		if (taskTime > 0)
		{
			// Add 20 recos on first login
			if (taskTime == 3600000)
			{
				setRecomLeft(getRecomLeft() + 20);
			}
			
			// If player have some timeleft, start bonus task
			_recoBonusTask = ThreadPoolManager.getInstance().scheduleGeneral(new RecoBonusTaskEnd(this), taskTime);
		}
		
		// Create task to give new recommendations
		_recoGiveTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new RecoGiveTask(this), 7200000, 3600000);
		
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
	
	public boolean isRecoTwoHoursGiven()
	{
		return _recoTwoHoursGiven;
	}
	
	public void setRecoTwoHoursGiven(boolean val)
	{
		_recoTwoHoursGiven = val;
	}
	
	public int getRecomBonusTime()
	{
		if (_recoBonusTask != null)
		{
			return (int) Math.max(0, _recoBonusTask.getDelay(TimeUnit.SECONDS));
		}
		
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
	
	@Override
	public boolean isPlayer()
	{
		return true;
	}
	
	@Override
	public boolean isChargedShot(ShotType type)
	{
		final L2ItemInstance weapon = getActiveWeaponInstance();
		return (weapon != null) && weapon.isChargedShot(type);
	}
	
	@Override
	public void setChargedShot(ShotType type, boolean charged)
	{
		final L2ItemInstance weapon = getActiveWeaponInstance();
		if (weapon != null)
		{
			weapon.setChargedShot(type, charged);
		}
	}
	
	/**
	 * @param skillId the display skill Id
	 * @return the custom skill
	 */
	public final L2Skill getCustomSkill(int skillId)
	{
		return (_customSkills != null) ? _customSkills.get(skillId) : null;
	}
	
	/**
	 * Add a skill level to the custom skills map.
	 * @param skill the skill to add
	 */
	private final void addCustomSkill(L2Skill skill)
	{
		if ((skill != null) && (skill.getDisplayId() != skill.getId()))
		{
			if (_customSkills == null)
			{
				_customSkills = new FastMap<Integer, L2Skill>().shared();
			}
			_customSkills.put(skill.getDisplayId(), skill);
		}
	}
	
	/**
	 * Remove a skill level from the custom skill map.
	 * @param skill the skill to remove
	 */
	private final void removeCustomSkill(L2Skill skill)
	{
		if ((skill != null) && (_customSkills != null) && (skill.getDisplayId() != skill.getId()))
		{
			_customSkills.remove(skill.getDisplayId());
		}
	}
	
	/**
	 * @return {@code true} if current player can revive and shows 'To Village' button upon death, {@code false} otherwise.
	 */
	@Override
	public boolean canRevive()
	{
		return _canRevive;
	}
	
	/**
	 * This method can prevent from displaying 'To Village' button upon death.
	 * @param val
	 */
	@Override
	public void setCanRevive(boolean val)
	{
		_canRevive = val;
	}
	
	// LISTENERS
	/**
	 * Fires all the equipment listeners, if any.<br>
	 * Action is cancelled if it returns false.
	 * @param isEquiped
	 * @param item
	 * @return
	 */
	private boolean fireEquipmentListeners(boolean isEquiped, L2ItemInstance item)
	{
		if (item != null)
		{
			EquipmentEvent event = new EquipmentEvent();
			event.setEquipped(!isEquiped);
			event.setItem(item);
			for (EquipmentListener listener : _equipmentListeners)
			{
				if (!listener.onEquip(event))
				{
					return false;
				}
			}
			for (EquipmentListener listener : GLOBAL_EQUIPMENT_LISTENERS)
			{
				if (!listener.onEquip(event))
				{
					return false;
				}
			}
		}
		return true;
	}
	
	/**
	 * Fires all the transformation listeners, if any.<br>
	 * If it returns false, the action is cancelled.<br>
	 * @param transformation
	 * @param isTransforming
	 * @return
	 */
	private boolean fireTransformListeners(Transform transformation, boolean isTransforming)
	{
		if ((transformation != null) && !_transformListeners.isEmpty())
		{
			TransformEvent event = new TransformEvent();
			event.setTransformation(transformation);
			event.setTransforming(isTransforming);
			for (TransformListener listener : _transformListeners)
			{
				if (!listener.onTransform(event))
				{
					return false;
				}
			}
		}
		return true;
	}
	
	/**
	 * Fires all the henna listeners, if any.<br>
	 * The action is cancelled if it returns false
	 * @param henna
	 * @param isAdding
	 * @return
	 */
	private boolean fireHennaListeners(L2Henna henna, boolean isAdding)
	{
		if ((henna != null) && !HENNA_LISTENERS.isEmpty())
		{
			HennaEvent event = new HennaEvent();
			event.setAdd(isAdding);
			event.setHenna(henna);
			event.setPlayer(this);
			for (HennaListener listener : HENNA_LISTENERS)
			{
				if (!listener.onRemoveHenna(event))
				{
					return false;
				}
			}
		}
		return true;
	}
	
	/**
	 * Fires all the profession change listeners
	 * @param t
	 */
	private void fireProfessionChangeListeners(L2PcTemplate t)
	{
		if (!_professionChangeListeners.isEmpty() || !GLOBAL_PROFESSION_CHANGE_LISTENERS.isEmpty())
		{
			ProfessionChangeEvent event = null;
			event = new ProfessionChangeEvent();
			event.setPlayer(this);
			event.setSubClass(isSubClassActive());
			event.setTemplate(t);
			for (ProfessionChangeListener listener : _professionChangeListeners)
			{
				listener.professionChanged(event);
			}
			for (ProfessionChangeListener listener : GLOBAL_PROFESSION_CHANGE_LISTENERS)
			{
				listener.professionChanged(event);
			}
		}
	}
	
	/**
	 * @return {@code true} if player is on event, {@code false} otherwise.
	 */
	@Override
	public boolean isOnEvent()
	{
		for (EventListener listener : _eventListeners)
		{
			if (listener.isOnEvent())
			{
				return true;
			}
		}
		return super.isOnEvent();
	}
	
	public boolean isBlockedFromExit()
	{
		for (EventListener listener : _eventListeners)
		{
			if (listener.isOnEvent() && listener.isBlockingExit())
			{
				return true;
			}
		}
		return false;
	}
	
	public boolean isBlockedFromDeathPenalty()
	{
		for (EventListener listener : _eventListeners)
		{
			if (listener.isOnEvent() && listener.isBlockingDeathPenalty())
			{
				return true;
			}
		}
		return false;
	}
	
	@Override
	public void addOverrideCond(PcCondOverride... excs)
	{
		super.addOverrideCond(excs);
		getVariables().set(COND_OVERRIDE_KEY, Long.toString(_exceptions));
	}
	
	@Override
	public void removeOverridedCond(PcCondOverride... excs)
	{
		super.removeOverridedCond(excs);
		getVariables().set(COND_OVERRIDE_KEY, Long.toString(_exceptions));
	}
	
	/**
	 * @return {@code true} if {@link PlayerVariables} instance is attached to current player's scripts, {@code false} otherwise.
	 */
	public boolean hasVariables()
	{
		return getScript(PlayerVariables.class) != null;
	}
	
	/**
	 * @return {@link PlayerVariables} instance containing parameters regarding player.
	 */
	public PlayerVariables getVariables()
	{
		final PlayerVariables vars = getScript(PlayerVariables.class);
		return vars != null ? vars : addScript(new PlayerVariables(getObjectId()));
	}
	
	/**
	 * @return {@code true} if {@link AccountVariables} instance is attached to current player's scripts, {@code false} otherwise.
	 */
	public boolean hasAccountVariables()
	{
		return getScript(AccountVariables.class) != null;
	}
	
	/**
	 * @return {@link AccountVariables} instance containing parameters regarding player.
	 */
	public AccountVariables getAccountVariables()
	{
		final AccountVariables vars = getScript(AccountVariables.class);
		return vars != null ? vars : addScript(new AccountVariables(getAccountName()));
	}
	
	/**
	 * Adds a henna listener
	 * @param listener
	 */
	public static void addHennaListener(HennaListener listener)
	{
		if (!HENNA_LISTENERS.contains(listener))
		{
			HENNA_LISTENERS.add(listener);
		}
	}
	
	/**
	 * Removes a henna listener
	 * @param listener
	 */
	public static void removeHennaListener(HennaListener listener)
	{
		HENNA_LISTENERS.remove(listener);
	}
	
	/**
	 * Adds an equipment listener
	 * @param listener
	 */
	public void addEquipmentListener(EquipmentListener listener)
	{
		if (!_equipmentListeners.contains(listener))
		{
			_equipmentListeners.add(listener);
		}
	}
	
	/**
	 * Removes an equipment listener
	 * @param listener
	 */
	public void removeEquipmentListener(EquipmentListener listener)
	{
		_equipmentListeners.remove(listener);
	}
	
	/**
	 * Adds an equipment listener
	 * @param listener
	 */
	public static void addGlobalEquipmentListener(EquipmentListener listener)
	{
		if (!GLOBAL_EQUIPMENT_LISTENERS.contains(listener))
		{
			GLOBAL_EQUIPMENT_LISTENERS.add(listener);
		}
	}
	
	/**
	 * Removes an equipment listener
	 * @param listener
	 */
	public static void removeGlobalEquipmentListener(EquipmentListener listener)
	{
		GLOBAL_EQUIPMENT_LISTENERS.remove(listener);
	}
	
	/**
	 * Adds a transformation listener
	 * @param listener
	 */
	public void addTransformListener(TransformListener listener)
	{
		if (!_transformListeners.contains(listener))
		{
			_transformListeners.add(listener);
		}
	}
	
	/**
	 * Remove a transformation listener
	 * @param listener
	 */
	public void removeTransformListener(TransformListener listener)
	{
		_transformListeners.remove(listener);
	}
	
	/**
	 * Adds a profession change listener
	 * @param listener
	 */
	public void addProfessionChangeListener(ProfessionChangeListener listener)
	{
		if (!_professionChangeListeners.contains(listener))
		{
			_professionChangeListeners.add(listener);
		}
	}
	
	/**
	 * Removes a profession change listener
	 * @param listener
	 */
	public void removeProfessionChangeListener(ProfessionChangeListener listener)
	{
		_professionChangeListeners.remove(listener);
	}
	
	/**
	 * Adds a global profession change listener
	 * @param listener
	 */
	public static void addGlobalProfessionChangeListener(ProfessionChangeListener listener)
	{
		if (!GLOBAL_PROFESSION_CHANGE_LISTENERS.contains(listener))
		{
			GLOBAL_PROFESSION_CHANGE_LISTENERS.add(listener);
		}
	}
	
	/**
	 * Removes a global profession change listener
	 * @param listener
	 */
	public static void removeGlobalProfessionChangeListener(ProfessionChangeListener listener)
	{
		GLOBAL_PROFESSION_CHANGE_LISTENERS.remove(listener);
	}
	
	/**
	 * Adds a event listener.
	 * @param listener
	 */
	public void addEventListener(EventListener listener)
	{
		_eventListeners.add(listener);
	}
	
	/**
	 * Removes event listener
	 * @param listener
	 */
	public void removeEventListener(EventListener listener)
	{
		_eventListeners.remove(listener);
	}
	
	public void removeEventListener(Class<? extends EventListener> clazz)
	{
		final Iterator<EventListener> it = _eventListeners.iterator();
		EventListener event;
		while (it.hasNext())
		{
			event = it.next();
			if (event.getClass() == clazz)
			{
				it.remove();
			}
		}
	}
	
	public Collection<EventListener> getEventListeners()
	{
		return _eventListeners;
	}
	
	@Override
	public boolean isInCategory(CategoryType type)
	{
		return CategoryData.getInstance().isInCategory(type, getClassId().getId());
	}
	
	@Override
	public int getId()
	{
		return 0;
	}
	
	public boolean isNewbie()
	{
		return getAccountVariables().hasVariable(NEWBIE_KEY) ? getAccountVariables().getInteger(NEWBIE_KEY) == getObjectId() : false;
	}
}