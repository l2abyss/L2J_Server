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
package com.l2jserver.gameserver.model.quest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2jserver.Config;
import com.l2jserver.L2DatabaseFactory;
import com.l2jserver.gameserver.GameTimeController;
import com.l2jserver.gameserver.ThreadPoolManager;
import com.l2jserver.gameserver.cache.HtmCache;
import com.l2jserver.gameserver.datatables.DoorTable;
import com.l2jserver.gameserver.datatables.ItemTable;
import com.l2jserver.gameserver.datatables.NpcTable;
import com.l2jserver.gameserver.idfactory.IdFactory;
import com.l2jserver.gameserver.instancemanager.InstanceManager;
import com.l2jserver.gameserver.instancemanager.QuestManager;
import com.l2jserver.gameserver.instancemanager.ZoneManager;
import com.l2jserver.gameserver.model.L2DropData;
import com.l2jserver.gameserver.model.L2Object;
import com.l2jserver.gameserver.model.L2Party;
import com.l2jserver.gameserver.model.L2Spawn;
import com.l2jserver.gameserver.model.Location;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.model.actor.L2Trap;
import com.l2jserver.gameserver.model.actor.instance.L2DoorInstance;
import com.l2jserver.gameserver.model.actor.instance.L2MonsterInstance;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.actor.instance.L2TrapInstance;
import com.l2jserver.gameserver.model.actor.templates.L2NpcTemplate;
import com.l2jserver.gameserver.model.entity.Instance;
import com.l2jserver.gameserver.model.holders.ItemHolder;
import com.l2jserver.gameserver.model.interfaces.IL2Procedure;
import com.l2jserver.gameserver.model.itemcontainer.PcInventory;
import com.l2jserver.gameserver.model.items.L2Item;
import com.l2jserver.gameserver.model.items.instance.L2ItemInstance;
import com.l2jserver.gameserver.model.olympiad.CompetitionType;
import com.l2jserver.gameserver.model.quest.AITasks.AggroRangeEnter;
import com.l2jserver.gameserver.model.quest.AITasks.Attack;
import com.l2jserver.gameserver.model.quest.AITasks.SeeCreature;
import com.l2jserver.gameserver.model.quest.AITasks.SkillSee;
import com.l2jserver.gameserver.model.skills.L2Skill;
import com.l2jserver.gameserver.model.stats.Stats;
import com.l2jserver.gameserver.model.zone.L2ZoneType;
import com.l2jserver.gameserver.network.NpcStringId;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.ActionFailed;
import com.l2jserver.gameserver.network.serverpackets.ExShowScreenMessage;
import com.l2jserver.gameserver.network.serverpackets.InventoryUpdate;
import com.l2jserver.gameserver.network.serverpackets.NpcHtmlMessage;
import com.l2jserver.gameserver.network.serverpackets.NpcQuestHtmlMessage;
import com.l2jserver.gameserver.network.serverpackets.PlaySound;
import com.l2jserver.gameserver.network.serverpackets.StatusUpdate;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;
import com.l2jserver.gameserver.scripting.ManagedScript;
import com.l2jserver.gameserver.scripting.ScriptManager;
import com.l2jserver.gameserver.util.MinionList;
import com.l2jserver.util.L2FastMap;
import com.l2jserver.util.Rnd;
import com.l2jserver.util.Util;

/**
 * Quest main class.
 * @author Luis Arias
 */
public class Quest extends ManagedScript
{
	public static final Logger _log = Logger.getLogger(Quest.class.getName());
	
	/** Map containing events from String value of the event. */
	private static Map<String, Quest> _allEventsS = new HashMap<>();
	
	/** Map containing lists of timers from the name of the timer. */
	private final Map<String, List<QuestTimer>> _allEventTimers = new L2FastMap<>(true);
	private final Set<Integer> _questInvolvedNpcs = new HashSet<>();
	
	private final ReentrantReadWriteLock _rwLock = new ReentrantReadWriteLock();
	private final WriteLock _writeLock = _rwLock.writeLock();
	private final ReadLock _readLock = _rwLock.readLock();
	
	private final int _questId;
	private final String _name;
	private final String _descr;
	private final byte _initialState = State.CREATED;
	protected boolean _onEnterWorld = false;
	private boolean _isCustom = false;
	private boolean _isOlympiadUse = false;
	
	public int[] questItemIds = null;
	
	private static final String DEFAULT_NO_QUEST_MSG = "<html><body>You are either not on a quest that involves this NPC, or you don't meet this NPC's minimum quest requirements.</body></html>";
	private static final String DEFAULT_ALREADY_COMPLETED_MSG = "<html><body>This quest has already been completed.</body></html>";
	
	private static final String QUEST_DELETE_FROM_CHAR_QUERY = "DELETE FROM character_quests WHERE charId=? AND name=?";
	private static final String QUEST_DELETE_FROM_CHAR_QUERY_NON_REPEATABLE_QUERY = "DELETE FROM character_quests WHERE charId=? AND name=? AND var!=?";
	
	private static final int RESET_HOUR = 6;
	private static final int RESET_MINUTES = 30;
	
	/**
	 * This enum contains known sound effects used by quests.<br>
	 * The idea is to have only a single object of each quest sound instead of constructing a new one every time a script calls the playSound method.<br>
	 * This is pretty much just a memory and CPU cycle optimization; avoids constructing/deconstructing objects all the time if they're all the same.<br>
	 * For datapack scripts written in Java and extending the Quest class, this does not need an extra import.
	 * @author jurchiks
	 */
	public static enum QuestSound
	{
		ITEMSOUND_QUEST_ACCEPT(new PlaySound("ItemSound.quest_accept")),
		ITEMSOUND_QUEST_MIDDLE(new PlaySound("ItemSound.quest_middle")),
		ITEMSOUND_QUEST_FINISH(new PlaySound("ItemSound.quest_finish")),
		ITEMSOUND_QUEST_ITEMGET(new PlaySound("ItemSound.quest_itemget")),
		// Newbie Guide tutorial (incl. some quests), Mutated Kaneus quests, Quest 192
		ITEMSOUND_QUEST_TUTORIAL(new PlaySound("ItemSound.quest_tutorial")),
		// Quests 107, 363, 364
		ITEMSOUND_QUEST_GIVEUP(new PlaySound("ItemSound.quest_giveup")),
		// Quests 212, 217, 224, 226, 416
		ITEMSOUND_QUEST_BEFORE_BATTLE(new PlaySound("ItemSound.quest_before_battle")),
		// Quests 211, 258, 266, 330
		ITEMSOUND_QUEST_JACKPOT(new PlaySound("ItemSound.quest_jackpot")),
		// Quests 508, 509 and 510
		ITEMSOUND_QUEST_FANFARE_1(new PlaySound("ItemSound.quest_fanfare_1")),
		// Played only after class transfer via Test Server Helpers (Id 31756 and 31757)
		ITEMSOUND_QUEST_FANFARE_2(new PlaySound("ItemSound.quest_fanfare_2")),
		// Quests 336
		ITEMSOUND_QUEST_FANFARE_MIDDLE(new PlaySound("ItemSound.quest_fanfare_middle")),
		// Quest 114
		ITEMSOUND_ARMOR_WOOD(new PlaySound("ItemSound.armor_wood_3")),
		// Quest 21
		ITEMSOUND_ARMOR_CLOTH(new PlaySound("ItemSound.item_drop_equip_armor_cloth")),
		ITEMSOUND_ED_CHIMES(new PlaySound("AmdSound.ed_chimes_05")),
		// Quest 22
		ITEMSOUND_D_HORROR_03(new PlaySound("AmbSound.d_horror_03")),
		ITEMSOUND_D_HORROR_15(new PlaySound("AmbSound.d_horror_15")),
		ITEMSOUND_DD_HORROR_01(new PlaySound("AmbSound.dd_horror_01")),
		// Quest 120
		ITEMSOUND_ED_DRONE_02(new PlaySound("AmbSound.ed_drone_02")),
		// Quest 23
		ITEMSOUND_ARMOR_LEATHER(new PlaySound("ItemSound.itemdrop_armor_leather")),
		ITEMSOUND_WEAPON_SPEAR(new PlaySound("ItemSound.itemdrop_weapon_spear")),
		// Quest 24
		AMDSOUND_D_WIND_LOOT_02(new PlaySound("AmdSound.d_wind_loot_02")),
		INTERFACESOUND_CHARSTAT_OPEN_01(new PlaySound("InterfaceSound.charstat_open_01")),
		// Quest 648 and treasure chests
		ITEMSOUND_BROKEN_KEY(new PlaySound("ItemSound2.broken_key")),
		// Quest 184
		ITEMSOUND_SIREN(new PlaySound("ItemSound3.sys_siren")),
		// Quest 648
		ITEMSOUND_ENCHANT_SUCCESS(new PlaySound("ItemSound3.sys_enchant_success")),
		ITEMSOUND_ENCHANT_FAILED(new PlaySound("ItemSound3.sys_enchant_failed")),
		// Best farm mobs
		ITEMSOUND_SOW_SUCCESS(new PlaySound("ItemSound3.sys_sow_success")),
		// Quest 25
		SKILLSOUND_HORROR_1(new PlaySound("SkillSound5.horror_01")),
		// Quests 21 and 23
		SKILLSOUND_HORROR_2(new PlaySound("SkillSound5.horror_02")),
		// Quest 22
		SKILLSOUND_ANTARAS_FEAR(new PlaySound("SkillSound3.antaras_fear")),
		// Quest 505
		SKILLSOUND_JEWEL_CELEBRATE(new PlaySound("SkillSound2.jewel.celebrate")),
		// Quest 373
		SKILLSOUND_LIQUID_MIX(new PlaySound("SkillSound5.liquid_mix_01")),
		SKILLSOUND_LIQUID_SUCCESS(new PlaySound("SkillSound5.liquid_success_01")),
		SKILLSOUND_LIQUID_FAIL(new PlaySound("SkillSound5.liquid_fail_01")),
		// Elroki sounds - Quest 111
		ETCSOUND_ELROKI_SOUND_FULL(new PlaySound("EtcSound.elcroki_song_full")),
		ETCSOUND_ELROKI_SOUND_1ST(new PlaySound("EtcSound.elcroki_song_1st")),
		ETCSOUND_ELROKI_SOUND_2ND(new PlaySound("EtcSound.elcroki_song_2nd")),
		ETCSOUND_ELROKI_SOUND_3RD(new PlaySound("EtcSound.elcroki_song_3rd")),
		// PailakaInjuredDragon
		BS08_A(new PlaySound("BS08_A")),
		// Quest 115
		AMBSOUND_T_WINGFLAP_04(new PlaySound("AmbSound.t_wingflap_04")),
		AMBSOUND_THUNDER_02(new PlaySound("AmbSound.thunder_02"));
		
		private final PlaySound _playSound;
		
		private static Map<String, PlaySound> soundPackets = new HashMap<>();
		
		private QuestSound(PlaySound playSound)
		{
			_playSound = playSound;
		}
		
		/**
		 * Get a {@link PlaySound} packet by its name.
		 * @param soundName : the name of the sound to look for
		 * @return the {@link PlaySound} packet with the specified sound or {@code null} if one was not found
		 */
		public static PlaySound getSound(String soundName)
		{
			if (soundPackets.containsKey(soundName))
			{
				return soundPackets.get(soundName);
			}
			
			for (QuestSound qs : QuestSound.values())
			{
				if (qs._playSound.getSoundName().equals(soundName))
				{
					soundPackets.put(soundName, qs._playSound); // cache in map to avoid looping repeatedly
					return qs._playSound;
				}
			}
			
			_log.info("Missing QuestSound enum for sound: " + soundName);
			soundPackets.put(soundName, new PlaySound(soundName));
			return soundPackets.get(soundName);
		}
		
		/**
		 * @return the name of the sound of this QuestSound object
		 */
		public String getSoundName()
		{
			return _playSound.getSoundName();
		}
		
		/**
		 * @return the {@link PlaySound} packet of this QuestSound object
		 */
		public PlaySound getPacket()
		{
			return _playSound;
		}
	}
	
	/**
	 * @return the reset hour for a daily quest, could be overridden on a script.
	 */
	public int getResetHour()
	{
		return RESET_HOUR;
	}
	
	/**
	 * @return the reset minutes for a daily quest, could be overridden on a script.
	 */
	public int getResetMinutes()
	{
		return RESET_MINUTES;
	}
	
	/**
	 * @return a collection of the values contained in the _allEventsS map.
	 */
	public static Collection<Quest> findAllEvents()
	{
		return _allEventsS.values();
	}
	
	/**
	 * The Quest object constructor.<br>
	 * Constructing a quest also calls the {@code init_LoadGlobalData} convenience method.
	 * @param questId int pointing out the Id of the quest.
	 * @param name String corresponding to the name of the quest.
	 * @param descr String for the description of the quest.
	 */
	public Quest(int questId, String name, String descr)
	{
		_questId = questId;
		_name = name;
		_descr = descr;
		if (questId != 0)
		{
			QuestManager.getInstance().addQuest(this);
		}
		else
		{
			_allEventsS.put(name, this);
		}
		init_LoadGlobalData();
	}
	
	/**
	 * The function init_LoadGlobalData is, by default, called by the constructor of all quests.<br>
	 * Children of this class can implement this function in order to define what variables to load and what structures to save them in.<br>
	 * By default, nothing is loaded.
	 */
	protected void init_LoadGlobalData()
	{
		
	}
	
	/**
	 * The function saveGlobalData is, by default, called at shutdown, for all quests, by the QuestManager.<br>
	 * Children of this class can implement this function in order to convert their structures<br>
	 * into <var, value> tuples and make calls to save them to the database, if needed.<br>
	 * By default, nothing is saved.
	 */
	public void saveGlobalData()
	{
		
	}
	
	/**
	 * Trap actions:<br>
	 * <ul>
	 * <li>Triggered</li>
	 * <li>Detected</li>
	 * <li>Disarmed</li>
	 * </ul>
	 */
	public static enum TrapAction
	{
		TRAP_TRIGGERED,
		TRAP_DETECTED,
		TRAP_DISARMED
	}
	
	public static enum QuestEventType
	{
		ON_FIRST_TALK(false), // control the first dialog shown by NPCs when they are clicked (some quests must override the default npc action)
		QUEST_START(true), // onTalk action from start npcs
		ON_TALK(true), // onTalk action from npcs participating in a quest
		ON_ATTACK(true), // onAttack action triggered when a mob gets attacked by someone
		ON_KILL(true), // onKill action triggered when a mob gets killed.
		ON_SPAWN(true), // onSpawn action triggered when an NPC is spawned or respawned.
		ON_SKILL_SEE(true), // NPC or Mob saw a person casting a skill (regardless what the target is).
		ON_FACTION_CALL(true), // NPC or Mob saw a person casting a skill (regardless what the target is).
		ON_AGGRO_RANGE_ENTER(true), // a person came within the Npc/Mob's range
		ON_SPELL_FINISHED(true), // on spell finished action when npc finish casting skill
		ON_SKILL_LEARN(false), // control the AcquireSkill dialog from quest script
		ON_ENTER_ZONE(true), // on zone enter
		ON_EXIT_ZONE(true), // on zone exit
		ON_TRAP_ACTION(true), // on zone exit
		ON_ITEM_USE(true),
		ON_EVENT_RECEIVED(true), // onEventReceived action, triggered when NPC receiving an event, sent by other NPC
		ON_MOVE_FINISHED(true), // onMoveFinished action, triggered when NPC stops after moving
		ON_NODE_ARRIVED(true), // onNodeArrived action, triggered when NPC, controlled by Walking Manager, arrives to next node
		ON_SEE_CREATURE(true); // onSeeCreature action, triggered when NPC's known list include the character
		
		// control whether this event type is allowed for the same npc template in multiple quests
		// or if the npc must be registered in at most one quest for the specified event
		private boolean _allowMultipleRegistration;
		
		private QuestEventType(boolean allowMultipleRegistration)
		{
			_allowMultipleRegistration = allowMultipleRegistration;
		}
		
		public boolean isMultipleRegistrationAllowed()
		{
			return _allowMultipleRegistration;
		}
	}
	
	/**
	 * @return the Id of the quest
	 */
	public int getQuestIntId()
	{
		return _questId;
	}
	
	/**
	 * Add a new quest state of this quest to the database.
	 * @param player the owner of the newly created quest state
	 * @return the newly created {@link QuestState} object
	 */
	public QuestState newQuestState(L2PcInstance player)
	{
		return new QuestState(this, player, getInitialState());
	}
	
	/**
	 * @return the initial state of the quest
	 */
	public byte getInitialState()
	{
		return _initialState;
	}
	
	/**
	 * @return the name of the quest
	 */
	public String getName()
	{
		return _name;
	}
	
	/**
	 * @return the description of the quest
	 */
	public String getDescr()
	{
		return _descr;
	}
	
	/**
	 * Add a timer to the quest (if it doesn't exist already) and start it.
	 * @param name the name of the timer (also passed back as "event" in {@link #onAdvEvent(String, L2Npc, L2PcInstance)})
	 * @param time time in ms for when to fire the timer
	 * @param npc the npc associated with this timer (can be null)
	 * @param player the player associated with this timer (can be null)
	 * @see #startQuestTimer(String, long, L2Npc, L2PcInstance, boolean)
	 */
	public void startQuestTimer(String name, long time, L2Npc npc, L2PcInstance player)
	{
		startQuestTimer(name, time, npc, player, false);
	}
	
	/**
	 * Add a timer to the quest (if it doesn't exist already) and start it.
	 * @param name the name of the timer (also passed back as "event" in {@link #onAdvEvent(String, L2Npc, L2PcInstance)})
	 * @param time time in ms for when to fire the timer
	 * @param npc the npc associated with this timer (can be null)
	 * @param player the player associated with this timer (can be null)
	 * @param repeating indicates whether the timer is repeatable or one-time.<br>
	 *            If {@code true}, the task is repeated every {@code time} milliseconds until explicitly stopped.
	 */
	public void startQuestTimer(String name, long time, L2Npc npc, L2PcInstance player, boolean repeating)
	{
		List<QuestTimer> timers = _allEventTimers.get(name);
		// Add quest timer if timer doesn't already exist
		if (timers == null)
		{
			timers = new ArrayList<>();
			timers.add(new QuestTimer(this, name, time, npc, player, repeating));
			_allEventTimers.put(name, timers);
		}
		// a timer with this name exists, but may not be for the same set of npc and player
		else
		{
			// if there exists a timer with this name, allow the timer only if the [npc, player] set is unique
			// nulls act as wildcards
			if (getQuestTimer(name, npc, player) == null)
			{
				_writeLock.lock();
				try
				{
					timers.add(new QuestTimer(this, name, time, npc, player, repeating));
				}
				finally
				{
					_writeLock.unlock();
				}
			}
		}
	}
	
	/**
	 * Get a quest timer that matches the provided name and parameters.
	 * @param name the name of the quest timer to get
	 * @param npc the NPC associated with the quest timer to get
	 * @param player the player associated with the quest timer to get
	 * @return the quest timer that matches the parameters of this function or {@code null} if nothing was found
	 */
	public QuestTimer getQuestTimer(String name, L2Npc npc, L2PcInstance player)
	{
		final List<QuestTimer> timers = _allEventTimers.get(name);
		if (timers != null)
		{
			_readLock.lock();
			try
			{
				for (QuestTimer timer : timers)
				{
					if (timer != null)
					{
						if (timer.isMatch(this, name, npc, player))
						{
							return timer;
						}
					}
				}
			}
			finally
			{
				_readLock.unlock();
			}
		}
		return null;
	}
	
	/**
	 * Cancel all quest timers with the specified name.
	 * @param name the name of the quest timers to cancel
	 */
	public void cancelQuestTimers(String name)
	{
		final List<QuestTimer> timers = _allEventTimers.get(name);
		if (timers != null)
		{
			_writeLock.lock();
			try
			{
				for (QuestTimer timer : timers)
				{
					if (timer != null)
					{
						timer.cancel();
					}
				}
				timers.clear();
			}
			finally
			{
				_writeLock.unlock();
			}
		}
	}
	
	/**
	 * Cancel the quest timer that matches the specified name and parameters.
	 * @param name the name of the quest timer to cancel
	 * @param npc the NPC associated with the quest timer to cancel
	 * @param player the player associated with the quest timer to cancel
	 */
	public void cancelQuestTimer(String name, L2Npc npc, L2PcInstance player)
	{
		final QuestTimer timer = getQuestTimer(name, npc, player);
		if (timer != null)
		{
			timer.cancelAndRemove();
		}
	}
	
	/**
	 * Remove a quest timer from the list of all timers.<br>
	 * Note: does not stop the timer itself!
	 * @param timer the {@link QuestState} object to remove
	 */
	public void removeQuestTimer(QuestTimer timer)
	{
		if (timer != null)
		{
			final List<QuestTimer> timers = _allEventTimers.get(timer.getName());
			if (timers != null)
			{
				_writeLock.lock();
				try
				{
					timers.remove(timer);
				}
				finally
				{
					_writeLock.unlock();
				}
			}
		}
	}
	
	public Map<String, List<QuestTimer>> getQuestTimers()
	{
		return _allEventTimers;
	}
	
	// These are methods to call within the core to call the quest events.
	
	/**
	 * @param npc the NPC that was attacked
	 * @param attacker the attacking player
	 * @param damage the damage dealt to the NPC by the player
	 * @param isSummon if {@code true}, the attack was actually made by the player's summon
	 * @param skill the skill used to attack the NPC (can be null)
	 */
	public final void notifyAttack(L2Npc npc, L2PcInstance attacker, int damage, boolean isSummon, L2Skill skill)
	{
		ThreadPoolManager.getInstance().executeAi(new Attack(this, npc, attacker, damage, isSummon, skill));
	}
	
	/**
	 * @param killer the character that killed the {@code victim}
	 * @param victim the character that was killed by the {@code killer}
	 * @param qs the quest state object of the player to be notified of this event
	 */
	public final void notifyDeath(L2Character killer, L2Character victim, QuestState qs)
	{
		String res = null;
		try
		{
			res = onDeath(killer, victim, qs);
		}
		catch (Exception e)
		{
			showError(qs.getPlayer(), e);
		}
		showResult(qs.getPlayer(), res);
	}
	
	/**
	 * @param item
	 * @param player
	 */
	public final void notifyItemUse(L2Item item, L2PcInstance player)
	{
		String res = null;
		try
		{
			res = onItemUse(item, player);
		}
		catch (Exception e)
		{
			showError(player, e);
		}
		showResult(player, res);
	}
	
	/**
	 * @param instance
	 * @param player
	 * @param skill
	 */
	public final void notifySpellFinished(L2Npc instance, L2PcInstance player, L2Skill skill)
	{
		String res = null;
		try
		{
			res = onSpellFinished(instance, player, skill);
		}
		catch (Exception e)
		{
			showError(player, e);
		}
		showResult(player, res);
	}
	
	/**
	 * Notify quest script when something happens with a trap.
	 * @param trap the trap instance which triggers the notification
	 * @param trigger the character which makes effect on the trap
	 * @param action 0: trap casting its skill. 1: trigger detects the trap. 2: trigger removes the trap
	 */
	public final void notifyTrapAction(L2Trap trap, L2Character trigger, TrapAction action)
	{
		String res = null;
		try
		{
			res = onTrapAction(trap, trigger, action);
		}
		catch (Exception e)
		{
			if (trigger.getActingPlayer() != null)
			{
				showError(trigger.getActingPlayer(), e);
			}
			_log.log(Level.WARNING, "Exception on onTrapAction() in notifyTrapAction(): " + e.getMessage(), e);
			return;
		}
		if (trigger.getActingPlayer() != null)
		{
			showResult(trigger.getActingPlayer(), res);
		}
	}
	
	/**
	 * @param npc the spawned NPC
	 */
	public final void notifySpawn(L2Npc npc)
	{
		try
		{
			onSpawn(npc);
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Exception on onSpawn() in notifySpawn(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * @param event
	 * @param npc
	 * @param player
	 * @return {@code false} if there was an error or the message was sent, {@code true} otherwise
	 */
	public final boolean notifyEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String res = null;
		try
		{
			res = onAdvEvent(event, npc, player);
		}
		catch (Exception e)
		{
			return showError(player, e);
		}
		return showResult(player, res);
	}
	
	/**
	 * @param player the player entering the world
	 */
	public final void notifyEnterWorld(L2PcInstance player)
	{
		String res = null;
		try
		{
			res = onEnterWorld(player);
		}
		catch (Exception e)
		{
			showError(player, e);
		}
		showResult(player, res);
	}
	
	/**
	 * @param npc
	 * @param killer
	 * @param isSummon
	 */
	public final void notifyKill(L2Npc npc, L2PcInstance killer, boolean isSummon)
	{
		String res = null;
		try
		{
			res = onKill(npc, killer, isSummon);
		}
		catch (Exception e)
		{
			showError(killer, e);
		}
		showResult(killer, res);
	}
	
	/**
	 * @param npc
	 * @param qs
	 * @return {@code false} if there was an error or the message was sent, {@code true} otherwise
	 */
	public final boolean notifyTalk(L2Npc npc, QuestState qs)
	{
		String res = null;
		try
		{
			res = onTalk(npc, qs.getPlayer());
		}
		catch (Exception e)
		{
			return showError(qs.getPlayer(), e);
		}
		qs.getPlayer().setLastQuestNpcObject(npc.getObjectId());
		return showResult(qs.getPlayer(), res);
	}
	
	/**
	 * Override the default NPC dialogs when a quest defines this for the given NPC.<br>
	 * Note: If the default html for this npc needs to be shown, onFirstTalk should call npc.showChatWindow(player) and then return null.
	 * @param npc the NPC whose dialogs to override
	 * @param player the player talking to the NPC
	 */
	public final void notifyFirstTalk(L2Npc npc, L2PcInstance player)
	{
		String res = null;
		try
		{
			res = onFirstTalk(npc, player);
		}
		catch (Exception e)
		{
			showError(player, e);
		}
		showResult(player, res);
	}
	
	/**
	 * @param npc
	 * @param player
	 */
	public final void notifyAcquireSkillList(L2Npc npc, L2PcInstance player)
	{
		String res = null;
		try
		{
			res = onAcquireSkillList(npc, player);
		}
		catch (Exception e)
		{
			showError(player, e);
		}
		showResult(player, res);
	}
	
	/**
	 * @param npc
	 * @param player
	 * @param skill
	 */
	public final void notifyAcquireSkillInfo(L2Npc npc, L2PcInstance player, L2Skill skill)
	{
		String res = null;
		try
		{
			res = onAcquireSkillInfo(npc, player, skill);
		}
		catch (Exception e)
		{
			showError(player, e);
		}
		showResult(player, res);
	}
	
	/**
	 * @param npc
	 * @param player
	 * @param skill
	 */
	public final void notifyAcquireSkill(L2Npc npc, L2PcInstance player, L2Skill skill)
	{
		String res = null;
		try
		{
			res = onAcquireSkill(npc, player, skill);
		}
		catch (Exception e)
		{
			showError(player, e);
		}
		showResult(player, res);
	}
	
	/**
	 * @param item
	 * @param player
	 * @return
	 */
	public final boolean notifyItemTalk(L2ItemInstance item, L2PcInstance player)
	{
		String res = null;
		try
		{
			res = onItemTalk(item, player);
			if (res != null)
			{
				if (res.equalsIgnoreCase("true"))
				{
					return true;
				}
				else if (res.equalsIgnoreCase("false"))
				{
					return false;
				}
			}
		}
		catch (Exception e)
		{
			return showError(player, e);
		}
		return showResult(player, res);
	}
	
	/**
	 * @param item
	 * @param player
	 * @return
	 */
	public String onItemTalk(L2ItemInstance item, L2PcInstance player)
	{
		return null;
	}
	
	/**
	 * @param item
	 * @param player
	 * @param event
	 * @return
	 */
	public final boolean notifyItemEvent(L2ItemInstance item, L2PcInstance player, String event)
	{
		String res = null;
		try
		{
			res = onItemEvent(item, player, event);
			if (res != null)
			{
				if (res.equalsIgnoreCase("true"))
				{
					return true;
				}
				else if (res.equalsIgnoreCase("false"))
				{
					return false;
				}
			}
		}
		catch (Exception e)
		{
			return showError(player, e);
		}
		return showResult(player, res);
	}
	
	/**
	 * @param npc
	 * @param caster
	 * @param skill
	 * @param targets
	 * @param isSummon
	 */
	public final void notifySkillSee(L2Npc npc, L2PcInstance caster, L2Skill skill, L2Object[] targets, boolean isSummon)
	{
		ThreadPoolManager.getInstance().executeAi(new SkillSee(this, npc, caster, skill, targets, isSummon));
	}
	
	/**
	 * @param npc
	 * @param caller
	 * @param attacker
	 * @param isSummon
	 */
	public final void notifyFactionCall(L2Npc npc, L2Npc caller, L2PcInstance attacker, boolean isSummon)
	{
		String res = null;
		try
		{
			res = onFactionCall(npc, caller, attacker, isSummon);
		}
		catch (Exception e)
		{
			showError(attacker, e);
		}
		showResult(attacker, res);
	}
	
	/**
	 * @param npc
	 * @param player
	 * @param isSummon
	 */
	public final void notifyAggroRangeEnter(L2Npc npc, L2PcInstance player, boolean isSummon)
	{
		ThreadPoolManager.getInstance().executeAi(new AggroRangeEnter(this, npc, player, isSummon));
	}
	
	/**
	 * @param npc the NPC that sees the creature
	 * @param creature the creature seen by the NPC
	 * @param isSummon
	 */
	public final void notifySeeCreature(L2Npc npc, L2Character creature, boolean isSummon)
	{
		ThreadPoolManager.getInstance().executeAi(new SeeCreature(this, npc, creature, isSummon));
	}
	
	/**
	 * @param eventName - name of event
	 * @param sender - NPC, who sent event
	 * @param receiver - NPC, who received event
	 * @param reference - L2Object to pass, if needed
	 */
	public final void notifyEventReceived(String eventName, L2Npc sender, L2Npc receiver, L2Object reference)
	{
		try
		{
			onEventReceived(eventName, sender, receiver, reference);
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Exception on onEventReceived() in notifyEventReceived(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * @param character
	 * @param zone
	 */
	public final void notifyEnterZone(L2Character character, L2ZoneType zone)
	{
		L2PcInstance player = character.getActingPlayer();
		String res = null;
		try
		{
			res = onEnterZone(character, zone);
		}
		catch (Exception e)
		{
			if (player != null)
			{
				showError(player, e);
			}
		}
		if (player != null)
		{
			showResult(player, res);
		}
	}
	
	/**
	 * @param character
	 * @param zone
	 */
	public final void notifyExitZone(L2Character character, L2ZoneType zone)
	{
		L2PcInstance player = character.getActingPlayer();
		String res = null;
		try
		{
			res = onExitZone(character, zone);
		}
		catch (Exception e)
		{
			if (player != null)
			{
				showError(player, e);
			}
		}
		if (player != null)
		{
			showResult(player, res);
		}
	}
	
	/**
	 * @param winner
	 * @param type {@code false} if there was an error, {@code true} otherwise
	 */
	public final void notifyOlympiadWin(L2PcInstance winner, CompetitionType type)
	{
		try
		{
			onOlympiadWin(winner, type);
		}
		catch (Exception e)
		{
			showError(winner, e);
		}
	}
	
	/**
	 * @param loser
	 * @param type {@code false} if there was an error, {@code true} otherwise
	 */
	public final void notifyOlympiadLose(L2PcInstance loser, CompetitionType type)
	{
		try
		{
			onOlympiadLose(loser, type);
		}
		catch (Exception e)
		{
			showError(loser, e);
		}
	}
	
	/**
	 * @param npc
	 */
	public final void notifyMoveFinished(L2Npc npc)
	{
		try
		{
			onMoveFinished(npc);
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Exception on onMoveFinished() in notifyMoveFinished(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * @param npc
	 */
	public final void notifyNodeArrived(L2Npc npc)
	{
		try
		{
			onNodeArrived(npc);
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Exception on onNodeArrived() in notifyNodeArrived(): " + e.getMessage(), e);
		}
	}
	
	// These are methods that java calls to invoke scripts.
	
	/**
	 * This function is called in place of {@link #onAttack(L2Npc, L2PcInstance, int, boolean, L2Skill)} if the former is not implemented.<br>
	 * If a script contains both onAttack(..) implementations, then this method will never be called unless the script's {@link #onAttack(L2Npc, L2PcInstance, int, boolean, L2Skill)} explicitly calls this method.
	 * @param npc this parameter contains a reference to the exact instance of the NPC that got attacked the NPC.
	 * @param attacker this parameter contains a reference to the exact instance of the player who attacked.
	 * @param damage this parameter represents the total damage that this attack has inflicted to the NPC.
	 * @param isSummon this parameter if it's {@code false} it denotes that the attacker was indeed the player, else it specifies that the damage was actually dealt by the player's pet.
	 * @return
	 */
	public String onAttack(L2Npc npc, L2PcInstance attacker, int damage, boolean isSummon)
	{
		return null;
	}
	
	/**
	 * This function is called whenever a player attacks an NPC that is registered for the quest.<br>
	 * If is not overridden by a subclass, then default to the returned value of the simpler (and older) {@link #onAttack(L2Npc, L2PcInstance, int, boolean)} override.<br>
	 * @param npc this parameter contains a reference to the exact instance of the NPC that got attacked.
	 * @param attacker this parameter contains a reference to the exact instance of the player who attacked the NPC.
	 * @param damage this parameter represents the total damage that this attack has inflicted to the NPC.
	 * @param isSummon this parameter if it's {@code false} it denotes that the attacker was indeed the player, else it specifies that the damage was actually dealt by the player's summon
	 * @param skill parameter is the skill that player used to attack NPC.
	 * @return
	 */
	public String onAttack(L2Npc npc, L2PcInstance attacker, int damage, boolean isSummon, L2Skill skill)
	{
		return onAttack(npc, attacker, damage, isSummon);
	}
	
	/**
	 * This function is called whenever an <b>exact instance</b> of a character who was previously registered for this event dies.<br>
	 * The registration for {@link #onDeath(L2Character, L2Character, QuestState)} events <b>is not</b> done via the quest itself, but it is instead handled by the QuestState of a particular player.
	 * @param killer this parameter contains a reference to the exact instance of the NPC that <b>killed</b> the character.
	 * @param victim this parameter contains a reference to the exact instance of the character that got killed.
	 * @param qs this parameter contains a reference to the QuestState of whomever was interested (waiting) for this kill.
	 * @return
	 */
	public String onDeath(L2Character killer, L2Character victim, QuestState qs)
	{
		return onAdvEvent("", ((killer instanceof L2Npc) ? ((L2Npc) killer) : null), qs.getPlayer());
	}
	
	/**
	 * This function is called whenever a player clicks on a link in a quest dialog and whenever a timer fires.<br>
	 * If is not overridden by a subclass, then default to the returned value of the simpler (and older) {@link #onEvent(String, QuestState)} override.<br>
	 * If the player has a quest state, use it as parameter in the next call, otherwise return null.
	 * @param event this parameter contains a string identifier for the event.<br>
	 *            Generally, this string is passed directly via the link.<br>
	 *            For example:<br>
	 *            <code>
	 *            &lt;a action="bypass -h Quest 626_ADarkTwilight 31517-01.htm"&gt;hello&lt;/a&gt;
	 *            </code><br>
	 *            The above link sets the event variable to "31517-01.htm" for the quest 626_ADarkTwilight.<br>
	 *            In the case of timers, this will be the name of the timer.<br>
	 *            This parameter serves as a sort of identifier.
	 * @param npc this parameter contains a reference to the instance of NPC associated with this event.<br>
	 *            This may be the NPC registered in a timer, or the NPC with whom a player is speaking, etc.<br>
	 *            This parameter may be {@code null} in certain circumstances.
	 * @param player this parameter contains a reference to the player participating in this function.<br>
	 *            It may be the player speaking to the NPC, or the player who caused a timer to start (and owns that timer).<br>
	 *            This parameter may be {@code null} in certain circumstances.
	 * @return the text returned by the event (may be {@code null}, a filename or just text)
	 */
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		if (player != null)
		{
			final QuestState qs = player.getQuestState(getName());
			if (qs != null)
			{
				return onEvent(event, qs);
			}
		}
		return null;
	}
	
	/**
	 * This function is called in place of {@link #onAdvEvent(String, L2Npc, L2PcInstance)} if the former is not implemented.<br>
	 * If a script contains both {@link #onAdvEvent(String, L2Npc, L2PcInstance)} and this implementation, then this method will never be called unless the script's {@link #onAdvEvent(String, L2Npc, L2PcInstance)} explicitly calls this method.
	 * @param event this parameter contains a string identifier for the event.<br>
	 *            Generally, this string is passed directly via the link.<br>
	 *            For example:<br>
	 *            <code>
	 *            &lt;a action="bypass -h Quest 626_ADarkTwilight 31517-01.htm"&gt;hello&lt;/a&gt;
	 *            </code><br>
	 *            The above link sets the event variable to "31517-01.htm" for the quest 626_ADarkTwilight.<br>
	 *            In the case of timers, this will be the name of the timer.<br>
	 *            This parameter serves as a sort of identifier.
	 * @param qs this parameter contains a reference to the quest state of the player who used the link or started the timer.
	 * @return the text returned by the event (may be {@code null}, a filename or just text)
	 */
	public String onEvent(String event, QuestState qs)
	{
		return null;
	}
	
	/**
	 * This function is called whenever a player kills a NPC that is registered for the quest.
	 * @param npc this parameter contains a reference to the exact instance of the NPC that got killed.
	 * @param killer this parameter contains a reference to the exact instance of the player who killed the NPC.
	 * @param isSummon this parameter if it's {@code false} it denotes that the attacker was indeed the player, else it specifies that the killer was the player's pet.
	 * @return the text returned by the event (may be {@code null}, a filename or just text)
	 */
	public String onKill(L2Npc npc, L2PcInstance killer, boolean isSummon)
	{
		return null;
	}
	
	/**
	 * This function is called whenever a player clicks to the "Quest" link of an NPC that is registered for the quest.
	 * @param npc this parameter contains a reference to the exact instance of the NPC that the player is talking with.
	 * @param talker this parameter contains a reference to the exact instance of the player who is talking to the NPC.
	 * @return the text returned by the event (may be {@code null}, a filename or just text)
	 */
	public String onTalk(L2Npc npc, L2PcInstance talker)
	{
		return null;
	}
	
	/**
	 * This function is called whenever a player talks to an NPC that is registered for the quest.<br>
	 * That is, it is triggered from the very first click on the NPC, not via another dialog.<br>
	 * <b>Note 1:</b><br>
	 * Each NPC can be registered to at most one quest for triggering this function.<br>
	 * In other words, the same one NPC cannot respond to an "onFirstTalk" request from two different quests.<br>
	 * Attempting to register an NPC in two different quests for this function will result in one of the two registration being ignored.<br>
	 * <b>Note 2:</b><br>
	 * Since a Quest link isn't clicked in order to reach this, a quest state can be invalid within this function.<br>
	 * The coder of the script may need to create a new quest state (if necessary).<br>
	 * <b>Note 3:</b><br>
	 * The returned value of onFirstTalk replaces the default HTML that would have otherwise been loaded from a sub-folder of DatapackRoot/game/data/html/.<br>
	 * If you wish to show the default HTML, within onFirstTalk do npc.showChatWindow(player) and then return ""<br>
	 * @param npc this parameter contains a reference to the exact instance of the NPC that the player is talking with.
	 * @param player this parameter contains a reference to the exact instance of the player who is talking to the NPC.
	 * @return the text returned by the event (may be {@code null}, a filename or just text)
	 * @since <a href="http://trac.l2jserver.com/changeset/771">Jython AI support for "onFirstTalk"</a>
	 */
	public String onFirstTalk(L2Npc npc, L2PcInstance player)
	{
		return null;
	}
	
	/**
	 * @param item
	 * @param player
	 * @param event
	 * @return
	 */
	public String onItemEvent(L2ItemInstance item, L2PcInstance player, String event)
	{
		return null;
	}
	
	/**
	 * This function is called whenever a player request a skill list.<br>
	 * TODO: Re-implement, since Skill Trees rework it's support was removed.
	 * @param npc this parameter contains a reference to the exact instance of the NPC that the player requested the skill list.
	 * @param player this parameter contains a reference to the exact instance of the player who requested the skill list.
	 * @return
	 */
	public String onAcquireSkillList(L2Npc npc, L2PcInstance player)
	{
		return null;
	}
	
	/**
	 * This function is called whenever a player request a skill info.<br>
	 * TODO: Re-implement, since Skill Trees rework it's support was removed.
	 * @param npc this parameter contains a reference to the exact instance of the NPC that the player requested the skill info.
	 * @param player this parameter contains a reference to the exact instance of the player who requested the skill info.
	 * @param skill this parameter contains a reference to the skill that the player requested its info.
	 * @return
	 */
	public String onAcquireSkillInfo(L2Npc npc, L2PcInstance player, L2Skill skill)
	{
		return null;
	}
	
	/**
	 * This function is called whenever a player acquire a skill.<br>
	 * TODO: Re-implement, since Skill Trees rework it's support was removed.
	 * @param npc this parameter contains a reference to the exact instance of the NPC that the player requested the skill.
	 * @param player this parameter contains a reference to the exact instance of the player who requested the skill.
	 * @param skill this parameter contains a reference to the skill that the player requested.
	 * @return
	 */
	public String onAcquireSkill(L2Npc npc, L2PcInstance player, L2Skill skill)
	{
		return null;
	}
	
	/**
	 * This function is called whenever a player uses a quest item that has a quest events list.<br>
	 * TODO: complete this documentation and unhardcode it to work with all item uses not with those listed.
	 * @param item the quest item that the player used
	 * @param player the player who used the item
	 * @return
	 */
	public String onItemUse(L2Item item, L2PcInstance player)
	{
		return null;
	}
	
	/**
	 * This function is called whenever a player casts a skill near a registered NPC (1000 distance).<br>
	 * <b>Note:</b><br>
	 * If a skill does damage, both onSkillSee(..) and onAttack(..) will be triggered for the damaged NPC!<br>
	 * However, only onSkillSee(..) will be triggered if the skill does no damage,<br>
	 * or if it damages an NPC who has no onAttack(..) registration while near another NPC who has an onSkillSee registration.<br>
	 * TODO: confirm if the distance is 1000 and unhardcode.
	 * @param npc the NPC that saw the skill
	 * @param caster the player who cast the skill
	 * @param skill the actual skill that was used
	 * @param targets an array of all objects (can be any type of object, including mobs and players) that were affected by the skill
	 * @param isSummon if {@code true}, the skill was actually cast by the player's summon, not the player himself
	 * @return
	 */
	public String onSkillSee(L2Npc npc, L2PcInstance caster, L2Skill skill, L2Object[] targets, boolean isSummon)
	{
		return null;
	}
	
	/**
	 * This function is called whenever an NPC finishes casting a skill.
	 * @param npc the NPC that casted the skill.
	 * @param player the player who is the target of the skill. Can be {@code null}.
	 * @param skill the actual skill that was used by the NPC.
	 * @return
	 */
	public String onSpellFinished(L2Npc npc, L2PcInstance player, L2Skill skill)
	{
		return null;
	}
	
	/**
	 * This function is called whenever a trap action is performed.
	 * @param trap this parameter contains a reference to the exact instance of the trap that was activated.
	 * @param trigger this parameter contains a reference to the exact instance of the character that triggered the action.
	 * @param action this parameter contains a reference to the action that was triggered.
	 * @return
	 */
	public String onTrapAction(L2Trap trap, L2Character trigger, TrapAction action)
	{
		return null;
	}
	
	/**
	 * This function is called whenever an NPC spawns or re-spawns and passes a reference to the newly (re)spawned NPC.<br>
	 * Currently the only function that has no reference to a player.<br>
	 * It is useful for initializations, starting quest timers, displaying chat (NpcSay), and more.
	 * @param npc this parameter contains a reference to the exact instance of the NPC who just (re)spawned.
	 * @return
	 */
	public String onSpawn(L2Npc npc)
	{
		return null;
	}
	
	/**
	 * This function is called whenever an NPC is called by another NPC in the same faction.
	 * @param npc this parameter contains a reference to the exact instance of the NPC who is being asked for help.
	 * @param caller this parameter contains a reference to the exact instance of the NPC who is asking for help.<br>
	 * @param attacker this parameter contains a reference to the exact instance of the player who attacked.
	 * @param isSummon this parameter if it's {@code false} it denotes that the attacker was indeed the player, else it specifies that the attacker was the player's summon.
	 * @return
	 */
	public String onFactionCall(L2Npc npc, L2Npc caller, L2PcInstance attacker, boolean isSummon)
	{
		return null;
	}
	
	/**
	 * This function is called whenever a player enters an NPC aggression range.
	 * @param npc this parameter contains a reference to the exact instance of the NPC whose aggression range is being transgressed.
	 * @param player this parameter contains a reference to the exact instance of the player who is entering the NPC's aggression range.
	 * @param isSummon this parameter if it's {@code false} it denotes that the character that entered the aggression range was indeed the player, else it specifies that the character was the player's summon.
	 * @return
	 */
	public String onAggroRangeEnter(L2Npc npc, L2PcInstance player, boolean isSummon)
	{
		return null;
	}
	
	/**
	 * This function is called whenever a NPC "sees" a creature.
	 * @param npc the NPC who sees the creature
	 * @param creature the creature seen by the NPC
	 * @param isSummon this parameter if it's {@code false} it denotes that the character seen by the NPC was indeed the player, else it specifies that the character was the player's summon
	 * @return
	 */
	public String onSeeCreature(L2Npc npc, L2Character creature, boolean isSummon)
	{
		return null;
	}
	
	/**
	 * This function is called whenever a player enters the game.
	 * @param player this parameter contains a reference to the exact instance of the player who is entering to the world.
	 * @return
	 */
	public String onEnterWorld(L2PcInstance player)
	{
		return null;
	}
	
	/**
	 * This function is called whenever a character enters a registered zone.
	 * @param character this parameter contains a reference to the exact instance of the character who is entering the zone.
	 * @param zone this parameter contains a reference to the zone.
	 * @return
	 */
	public String onEnterZone(L2Character character, L2ZoneType zone)
	{
		return null;
	}
	
	/**
	 * This function is called whenever a character exits a registered zone.
	 * @param character this parameter contains a reference to the exact instance of the character who is exiting the zone.
	 * @param zone this parameter contains a reference to the zone.
	 * @return
	 */
	public String onExitZone(L2Character character, L2ZoneType zone)
	{
		return null;
	}
	
	/**
	 * @param eventName - name of event
	 * @param sender - NPC, who sent event
	 * @param receiver - NPC, who received event
	 * @param reference - L2Object to pass, if needed
	 * @return
	 */
	public String onEventReceived(String eventName, L2Npc sender, L2Npc receiver, L2Object reference)
	{
		return null;
	}
	
	/**
	 * This function is called whenever a player wins an Olympiad Game.
	 * @param winner this parameter contains a reference to the exact instance of the player who won the competition.
	 * @param type this parameter contains a reference to the competition type.
	 */
	public void onOlympiadWin(L2PcInstance winner, CompetitionType type)
	{
		
	}
	
	/**
	 * This function is called whenever a player looses an Olympiad Game.
	 * @param loser this parameter contains a reference to the exact instance of the player who lose the competition.
	 * @param type this parameter contains a reference to the competition type.
	 */
	public void onOlympiadLose(L2PcInstance loser, CompetitionType type)
	{
		
	}
	
	/**
	 * This function is called whenever a NPC finishes moving
	 * @param npc registered NPC
	 * @return
	 */
	public String onMoveFinished(L2Npc npc)
	{
		return null;
	}
	
	/**
	 * This function is called whenever a walker NPC (controlled by WalkingManager) arrive a walking node
	 * @param npc registered NPC
	 * @return
	 */
	public String onNodeArrived(L2Npc npc)
	{
		return null;
	}
	
	/**
	 * Show an error message to the specified player.
	 * @param player the player to whom to send the error (must be a GM)
	 * @param t the {@link Throwable} to get the message/stacktrace from
	 * @return {@code false}
	 */
	public boolean showError(L2PcInstance player, Throwable t)
	{
		_log.log(Level.WARNING, getScriptFile().getAbsolutePath(), t);
		if (t.getMessage() == null)
		{
			_log.warning(getClass().getSimpleName() + ": " + t.getMessage());
		}
		if ((player != null) && player.getAccessLevel().isGm())
		{
			String res = "<html><body><title>Script error</title>" + Util.getStackTrace(t) + "</body></html>";
			return showResult(player, res);
		}
		return false;
	}
	
	/**
	 * Show a message to the specified player.<br>
	 * <u><i>Concept:</i></u><br>
	 * Three cases are managed according to the value of the {@code res} parameter:<br>
	 * <ul>
	 * <li><u>{@code res} ends with ".htm" or ".html":</u> the contents of the specified HTML file are shown in a dialog window</li>
	 * <li><u>{@code res} starts with "&lt;html&gt;":</u> the contents of the parameter are shown in a dialog window</li>
	 * <li><u>all other cases :</u> the text contained in the parameter is shown in chat</li>
	 * </ul>
	 * @param player the player to whom to show the result
	 * @param res the message to show to the player
	 * @return {@code false} if the message was sent, {@code true} otherwise
	 */
	public boolean showResult(L2PcInstance player, String res)
	{
		if ((res == null) || res.isEmpty() || (player == null))
		{
			return true;
		}
		
		if (res.endsWith(".htm") || res.endsWith(".html"))
		{
			showHtmlFile(player, res);
		}
		else if (res.startsWith("<html>"))
		{
			NpcHtmlMessage npcReply = new NpcHtmlMessage(5);
			npcReply.setHtml(res);
			npcReply.replace("%playername%", player.getName());
			player.sendPacket(npcReply);
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
		else
		{
			player.sendMessage(res);
		}
		return false;
	}
	
	/**
	 * Loads all quest states and variables for the specified player.
	 * @param player the player who is entering the world
	 */
	public static final void playerEnter(L2PcInstance player)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement invalidQuestData = con.prepareStatement("DELETE FROM character_quests WHERE charId = ? AND name = ?");
			PreparedStatement invalidQuestDataVar = con.prepareStatement("DELETE FROM character_quests WHERE charId = ? AND name = ? AND var = ?");
			PreparedStatement ps1 = con.prepareStatement("SELECT name, value FROM character_quests WHERE charId = ? AND var = ?"))
		{
			// Get list of quests owned by the player from database
			
			ps1.setInt(1, player.getObjectId());
			ps1.setString(2, "<state>");
			try (ResultSet rs = ps1.executeQuery())
			{
				while (rs.next())
				{
					// Get Id of the quest and Id of its state
					String questId = rs.getString("name");
					String statename = rs.getString("value");
					
					// Search quest associated with the Id
					Quest q = QuestManager.getInstance().getQuest(questId);
					if (q == null)
					{
						_log.finer("Unknown quest " + questId + " for player " + player.getName());
						if (Config.AUTODELETE_INVALID_QUEST_DATA)
						{
							invalidQuestData.setInt(1, player.getObjectId());
							invalidQuestData.setString(2, questId);
							invalidQuestData.executeUpdate();
						}
						continue;
					}
					
					// Create a new QuestState for the player that will be added to the player's list of quests
					new QuestState(q, player, State.getStateId(statename));
				}
			}
			
			// Get list of quests owned by the player from the DB in order to add variables used in the quest.
			try (PreparedStatement ps2 = con.prepareStatement("SELECT name, var, value FROM character_quests WHERE charId = ? AND var <> ?"))
			{
				ps2.setInt(1, player.getObjectId());
				ps2.setString(2, "<state>");
				try (ResultSet rs = ps2.executeQuery())
				{
					while (rs.next())
					{
						String questId = rs.getString("name");
						String var = rs.getString("var");
						String value = rs.getString("value");
						// Get the QuestState saved in the loop before
						QuestState qs = player.getQuestState(questId);
						if (qs == null)
						{
							_log.finer("Lost variable " + var + " in quest " + questId + " for player " + player.getName());
							if (Config.AUTODELETE_INVALID_QUEST_DATA)
							{
								invalidQuestDataVar.setInt(1, player.getObjectId());
								invalidQuestDataVar.setString(2, questId);
								invalidQuestDataVar.setString(3, var);
								invalidQuestDataVar.executeUpdate();
							}
							continue;
						}
						// Add parameter to the quest
						qs.setInternal(var, value);
					}
				}
			}
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "could not insert char quest:", e);
		}
		
		// events
		for (String name : _allEventsS.keySet())
		{
			player.processQuestEvent(name, "enter");
		}
	}
	
	/**
	 * Insert (or update) in the database variables that need to stay persistent for this quest after a reboot.<br>
	 * This function is for storage of values that do not related to a specific player but are global for all characters.<br>
	 * For example, if we need to disable a quest-gatekeeper until a certain time (as is done with some grand-boss gatekeepers), we can save that time in the DB.
	 * @param var the name of the variable to save
	 * @param value the value of the variable
	 */
	public final void saveGlobalQuestVar(String var, String value)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("REPLACE INTO quest_global_data (quest_name,var,value) VALUES (?,?,?)"))
		{
			statement.setString(1, getName());
			statement.setString(2, var);
			statement.setString(3, value);
			statement.executeUpdate();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "could not insert global quest variable:", e);
		}
	}
	
	/**
	 * Read from the database a previously saved variable for this quest.<br>
	 * Due to performance considerations, this function should best be used only when the quest is first loaded.<br>
	 * Subclasses of this class can define structures into which these loaded values can be saved.<br>
	 * However, on-demand usage of this function throughout the script is not prohibited, only not recommended.<br>
	 * Values read from this function were entered by calls to "saveGlobalQuestVar".
	 * @param var the name of the variable to load
	 * @return the current value of the specified variable, or an empty string if the variable does not exist
	 */
	public final String loadGlobalQuestVar(String var)
	{
		String result = "";
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT value FROM quest_global_data WHERE quest_name = ? AND var = ?"))
		{
			statement.setString(1, getName());
			statement.setString(2, var);
			try (ResultSet rs = statement.executeQuery())
			{
				if (rs.first())
				{
					result = rs.getString(1);
				}
			}
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "could not load global quest variable:", e);
		}
		return result;
	}
	
	/**
	 * Permanently delete from the database a global quest variable that was previously saved for this quest.
	 * @param var the name of the variable to delete
	 */
	public final void deleteGlobalQuestVar(String var)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM quest_global_data WHERE quest_name = ? AND var = ?"))
		{
			statement.setString(1, getName());
			statement.setString(2, var);
			statement.executeUpdate();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "could not delete global quest variable:", e);
		}
	}
	
	/**
	 * Permanently delete from the database all global quest variables that were previously saved for this quest.
	 */
	public final void deleteAllGlobalQuestVars()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM quest_global_data WHERE quest_name = ?"))
		{
			statement.setString(1, getName());
			statement.executeUpdate();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "could not delete global quest variables:", e);
		}
	}
	
	/**
	 * Insert in the database the quest for the player.
	 * @param qs the {@link QuestState} object whose variable to insert
	 * @param var the name of the variable
	 * @param value the value of the variable
	 */
	public static void createQuestVarInDb(QuestState qs, String var, String value)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("INSERT INTO character_quests (charId,name,var,value) VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE value=?"))
		{
			statement.setInt(1, qs.getPlayer().getObjectId());
			statement.setString(2, qs.getQuestName());
			statement.setString(3, var);
			statement.setString(4, value);
			statement.setString(5, value);
			statement.executeUpdate();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "could not insert char quest:", e);
		}
	}
	
	/**
	 * Update the value of the variable "var" for the specified quest in database
	 * @param qs the {@link QuestState} object whose variable to update
	 * @param var the name of the variable
	 * @param value the value of the variable
	 */
	public static void updateQuestVarInDb(QuestState qs, String var, String value)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE character_quests SET value=? WHERE charId=? AND name=? AND var = ?"))
		{
			statement.setString(1, value);
			statement.setInt(2, qs.getPlayer().getObjectId());
			statement.setString(3, qs.getQuestName());
			statement.setString(4, var);
			statement.executeUpdate();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "could not update char quest:", e);
		}
	}
	
	/**
	 * Delete a variable of player's quest from the database.
	 * @param qs the {@link QuestState} object whose variable to delete
	 * @param var the name of the variable to delete
	 */
	public static void deleteQuestVarInDb(QuestState qs, String var)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM character_quests WHERE charId=? AND name=? AND var=?"))
		{
			statement.setInt(1, qs.getPlayer().getObjectId());
			statement.setString(2, qs.getQuestName());
			statement.setString(3, var);
			statement.executeUpdate();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "could not delete char quest:", e);
		}
	}
	
	/**
	 * Delete from the database all variables and states of the specified quest state.
	 * @param qs the {@link QuestState} object whose variables to delete
	 * @param repeatable if {@code false}, the state variable will be preserved, otherwise it will be deleted as well
	 */
	public static void deleteQuestInDb(QuestState qs, boolean repeatable)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(repeatable ? QUEST_DELETE_FROM_CHAR_QUERY : QUEST_DELETE_FROM_CHAR_QUERY_NON_REPEATABLE_QUERY))
		{
			ps.setInt(1, qs.getPlayer().getObjectId());
			ps.setString(2, qs.getQuestName());
			if (!repeatable)
			{
				ps.setString(3, "<state>");
			}
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "could not delete char quest:", e);
		}
	}
	
	/**
	 * Create a database record for the specified quest state.
	 * @param qs the {@link QuestState} object whose data to write in the database
	 */
	public static void createQuestInDb(QuestState qs)
	{
		createQuestVarInDb(qs, "<state>", State.getStateName(qs.getState()));
	}
	
	/**
	 * Update informations regarding quest in database.<br>
	 * Actions:<br>
	 * <ul>
	 * <li>Get Id state of the quest recorded in object qs</li>
	 * <li>Test if quest is completed. If true, add a star (*) before the Id state</li>
	 * <li>Save in database the Id state (with or without the star) for the variable called "&lt;state&gt;" of the quest</li>
	 * </ul>
	 * @param qs the quest state
	 */
	public static void updateQuestInDb(QuestState qs)
	{
		updateQuestVarInDb(qs, "<state>", State.getStateName(qs.getState()));
	}
	
	/**
	 * @param player the player whose language settings to use in finding the html of the right language
	 * @return the default html for when no quest is available: "You are either not on a quest that involves this NPC.."
	 */
	public static String getNoQuestMsg(L2PcInstance player)
	{
		final String result = HtmCache.getInstance().getHtm(player.getHtmlPrefix(), "data/html/noquest.htm");
		if ((result != null) && (result.length() > 0))
		{
			return result;
		}
		return DEFAULT_NO_QUEST_MSG;
	}
	
	/**
	 * @param player the player whose language settings to use in finding the html of the right language
	 * @return the default html for when no quest is already completed: "This quest has already been completed."
	 */
	public static String getAlreadyCompletedMsg(L2PcInstance player)
	{
		final String result = HtmCache.getInstance().getHtm(player.getHtmlPrefix(), "data/html/alreadycompleted.htm");
		if ((result != null) && (result.length() > 0))
		{
			return result;
		}
		return DEFAULT_ALREADY_COMPLETED_MSG;
	}
	
	/**
	 * Add this quest to the list of quests that the passed mob will respond to for the specified Event type.
	 * @param npcId id of the NPC to register
	 * @param eventType type of event being registered
	 * @return L2NpcTemplate Npc Template corresponding to the npcId, or null if the id is invalid
	 */
	public L2NpcTemplate addEventId(int npcId, QuestEventType eventType)
	{
		try
		{
			L2NpcTemplate t = NpcTable.getInstance().getTemplate(npcId);
			if (t != null)
			{
				t.addQuestEvent(eventType, this);
			}
			
			_questInvolvedNpcs.add(npcId);
			return t;
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Exception on addEventId(): " + e.getMessage(), e);
			return null;
		}
	}
	
	/**
	 * Add the quest to the NPC's startQuest
	 * @param npcIds
	 * @return L2NpcTemplate Start NPC
	 */
	public L2NpcTemplate[] addStartNpc(int... npcIds)
	{
		L2NpcTemplate[] value = new L2NpcTemplate[npcIds.length];
		int i = 0;
		for (int npcId : npcIds)
		{
			value[i++] = addEventId(npcId, QuestEventType.QUEST_START);
		}
		return value;
	}
	
	public L2NpcTemplate addStartNpc(int npcId)
	{
		return addEventId(npcId, QuestEventType.QUEST_START);
	}
	
	/**
	 * Add the quest to the NPC's first-talk (default action dialog)
	 * @param npcIds
	 * @return L2NpcTemplate Start NPC
	 */
	public L2NpcTemplate[] addFirstTalkId(int... npcIds)
	{
		L2NpcTemplate[] value = new L2NpcTemplate[npcIds.length];
		int i = 0;
		for (int npcId : npcIds)
		{
			value[i++] = addEventId(npcId, QuestEventType.ON_FIRST_TALK);
		}
		return value;
	}
	
	/**
	 * @param npcId
	 * @return
	 */
	public L2NpcTemplate addFirstTalkId(int npcId)
	{
		return addEventId(npcId, QuestEventType.ON_FIRST_TALK);
	}
	
	/**
	 * Add the NPC to the AcquireSkill dialog
	 * @param npcIds
	 * @return L2NpcTemplate NPC
	 */
	public L2NpcTemplate[] addAcquireSkillId(int... npcIds)
	{
		L2NpcTemplate[] value = new L2NpcTemplate[npcIds.length];
		int i = 0;
		for (int npcId : npcIds)
		{
			value[i++] = addEventId(npcId, QuestEventType.ON_SKILL_LEARN);
		}
		return value;
	}
	
	/**
	 * @param npcId
	 * @return
	 */
	public L2NpcTemplate addAcquireSkillId(int npcId)
	{
		return addEventId(npcId, QuestEventType.ON_SKILL_LEARN);
	}
	
	/**
	 * Add this quest to the list of quests that the passed mob will respond to for Attack Events.
	 * @param npcIds
	 * @return int attackId
	 */
	public L2NpcTemplate[] addAttackId(int... npcIds)
	{
		L2NpcTemplate[] value = new L2NpcTemplate[npcIds.length];
		int i = 0;
		for (int npcId : npcIds)
		{
			value[i++] = addEventId(npcId, QuestEventType.ON_ATTACK);
		}
		return value;
	}
	
	/**
	 * @param npcId
	 * @return
	 */
	public L2NpcTemplate addAttackId(int npcId)
	{
		return addEventId(npcId, QuestEventType.ON_ATTACK);
	}
	
	/**
	 * Add this quest to the list of quests that the passed mob will respond to for Kill Events.
	 * @param killIds
	 * @return int killId
	 */
	public L2NpcTemplate[] addKillId(int... killIds)
	{
		L2NpcTemplate[] value = new L2NpcTemplate[killIds.length];
		int i = 0;
		for (int killId : killIds)
		{
			value[i++] = addEventId(killId, QuestEventType.ON_KILL);
		}
		return value;
	}
	
	/**
	 * Add this quest event to the collection of NPC Ids that will respond to for on kill events.
	 * @param killIds the collection of NPC Ids
	 * @return the list of NPC templates that has been associated with this event
	 */
	public List<L2NpcTemplate> addKillId(Collection<Integer> killIds)
	{
		final List<L2NpcTemplate> list = new ArrayList<>(killIds.size());
		for (int killId : killIds)
		{
			list.add(addEventId(killId, QuestEventType.ON_KILL));
		}
		return list;
	}
	
	/**
	 * @param npcId
	 * @return
	 */
	public L2NpcTemplate addKillId(int npcId)
	{
		return addEventId(npcId, QuestEventType.ON_KILL);
	}
	
	/**
	 * Add this quest to the list of quests that the passed npc will respond to for Talk Events.
	 * @param talkIds Id of the NPC
	 * @return int Id of the NPC
	 */
	public L2NpcTemplate[] addTalkId(int... talkIds)
	{
		L2NpcTemplate[] value = new L2NpcTemplate[talkIds.length];
		int i = 0;
		for (int talkId : talkIds)
		{
			value[i++] = addEventId(talkId, QuestEventType.ON_TALK);
		}
		return value;
	}
	
	/**
	 * @param npcId
	 * @return
	 */
	public L2NpcTemplate addTalkId(int npcId)
	{
		return addEventId(npcId, QuestEventType.ON_TALK);
	}
	
	/**
	 * Add this quest to the list of quests that the passed npc will respond to for Spawn Events.
	 * @param npcIds Id of the NPC
	 * @return int Id of the NPC
	 */
	public L2NpcTemplate[] addSpawnId(int... npcIds)
	{
		L2NpcTemplate[] value = new L2NpcTemplate[npcIds.length];
		int i = 0;
		for (int npcId : npcIds)
		{
			value[i++] = addEventId(npcId, QuestEventType.ON_SPAWN);
		}
		return value;
	}
	
	/**
	 * @param npcId
	 * @return
	 */
	public L2NpcTemplate addSpawnId(int npcId)
	{
		return addEventId(npcId, QuestEventType.ON_SPAWN);
	}
	
	/**
	 * Add this quest to the list of quests that the passed npc will respond to for Skill-See Events.
	 * @param npcIds Id of the NPC
	 * @return int Id of the NPC
	 */
	public L2NpcTemplate[] addSkillSeeId(int... npcIds)
	{
		L2NpcTemplate[] value = new L2NpcTemplate[npcIds.length];
		int i = 0;
		for (int npcId : npcIds)
		{
			value[i++] = addEventId(npcId, QuestEventType.ON_SKILL_SEE);
		}
		return value;
	}
	
	/**
	 * @param npcId
	 * @return
	 */
	public L2NpcTemplate addSkillSeeId(int npcId)
	{
		return addEventId(npcId, QuestEventType.ON_SKILL_SEE);
	}
	
	/**
	 * @param npcIds
	 * @return
	 */
	public L2NpcTemplate[] addSpellFinishedId(int... npcIds)
	{
		L2NpcTemplate[] value = new L2NpcTemplate[npcIds.length];
		int i = 0;
		for (int npcId : npcIds)
		{
			value[i++] = addEventId(npcId, QuestEventType.ON_SPELL_FINISHED);
		}
		return value;
	}
	
	/**
	 * @param npcId
	 * @return
	 */
	public L2NpcTemplate addSpellFinishedId(int npcId)
	{
		return addEventId(npcId, QuestEventType.ON_SPELL_FINISHED);
	}
	
	/**
	 * @param npcIds
	 * @return
	 */
	public L2NpcTemplate[] addTrapActionId(int... npcIds)
	{
		L2NpcTemplate[] value = new L2NpcTemplate[npcIds.length];
		int i = 0;
		for (int npcId : npcIds)
		{
			value[i++] = addEventId(npcId, QuestEventType.ON_TRAP_ACTION);
		}
		return value;
	}
	
	/**
	 * @param npcId
	 * @return
	 */
	public L2NpcTemplate addTrapActionId(int npcId)
	{
		return addEventId(npcId, QuestEventType.ON_TRAP_ACTION);
	}
	
	/**
	 * Add this quest to the list of quests that the passed npc will respond to for Faction Call Events.
	 * @param npcIds Id of the NPC
	 * @return int Id of the NPC
	 */
	public L2NpcTemplate[] addFactionCallId(int... npcIds)
	{
		L2NpcTemplate[] value = new L2NpcTemplate[npcIds.length];
		int i = 0;
		for (int npcId : npcIds)
		{
			value[i++] = addEventId(npcId, QuestEventType.ON_FACTION_CALL);
		}
		return value;
	}
	
	/**
	 * @param npcId
	 * @return
	 */
	public L2NpcTemplate addFactionCallId(int npcId)
	{
		return addEventId(npcId, QuestEventType.ON_FACTION_CALL);
	}
	
	/**
	 * Add this quest to the list of quests that the passed npc will respond to for Character See Events.
	 * @param npcIds Id of the NPC
	 * @return int Id of the NPC
	 */
	public L2NpcTemplate[] addAggroRangeEnterId(int... npcIds)
	{
		L2NpcTemplate[] value = new L2NpcTemplate[npcIds.length];
		int i = 0;
		for (int npcId : npcIds)
		{
			value[i++] = addEventId(npcId, QuestEventType.ON_AGGRO_RANGE_ENTER);
		}
		return value;
	}
	
	/**
	 * @param npcId
	 * @return
	 */
	public L2NpcTemplate addAggroRangeEnterId(int npcId)
	{
		return addEventId(npcId, QuestEventType.ON_AGGRO_RANGE_ENTER);
	}
	
	/**
	 * @param npcIds NPCs to register to on see creature event
	 * @return the templates of the registered NPCs
	 */
	public L2NpcTemplate[] addSeeCreatureId(int... npcIds)
	{
		final L2NpcTemplate[] value = new L2NpcTemplate[npcIds.length];
		for (int i = 0; i < npcIds.length; i++)
		{
			value[i] = addEventId(npcIds[i], QuestEventType.ON_SEE_CREATURE);
		}
		return value;
	}
	
	/**
	 * @param zoneIds
	 * @return
	 */
	public L2ZoneType[] addEnterZoneId(int... zoneIds)
	{
		L2ZoneType[] value = new L2ZoneType[zoneIds.length];
		int i = 0;
		for (int zoneId : zoneIds)
		{
			try
			{
				L2ZoneType zone = ZoneManager.getInstance().getZoneById(zoneId);
				if (zone != null)
				{
					zone.addQuestEvent(QuestEventType.ON_ENTER_ZONE, this);
				}
				value[i++] = zone;
			}
			catch (Exception e)
			{
				_log.log(Level.WARNING, "Exception on addEnterZoneId(): " + e.getMessage(), e);
				continue;
			}
		}
		
		return value;
	}
	
	/**
	 * @param zoneId
	 * @return
	 */
	public L2ZoneType addEnterZoneId(int zoneId)
	{
		try
		{
			L2ZoneType zone = ZoneManager.getInstance().getZoneById(zoneId);
			if (zone != null)
			{
				zone.addQuestEvent(QuestEventType.ON_ENTER_ZONE, this);
			}
			return zone;
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Exception on addEnterZoneId(): " + e.getMessage(), e);
			return null;
		}
	}
	
	/**
	 * @param zoneIds
	 * @return
	 */
	public L2ZoneType[] addExitZoneId(int... zoneIds)
	{
		L2ZoneType[] value = new L2ZoneType[zoneIds.length];
		int i = 0;
		for (int zoneId : zoneIds)
		{
			try
			{
				L2ZoneType zone = ZoneManager.getInstance().getZoneById(zoneId);
				if (zone != null)
				{
					zone.addQuestEvent(QuestEventType.ON_EXIT_ZONE, this);
				}
				value[i++] = zone;
			}
			catch (Exception e)
			{
				_log.log(Level.WARNING, "Exception on addEnterZoneId(): " + e.getMessage(), e);
				continue;
			}
		}
		
		return value;
	}
	
	/**
	 * @param zoneId
	 * @return
	 */
	public L2ZoneType addExitZoneId(int zoneId)
	{
		try
		{
			L2ZoneType zone = ZoneManager.getInstance().getZoneById(zoneId);
			if (zone != null)
			{
				zone.addQuestEvent(QuestEventType.ON_EXIT_ZONE, this);
			}
			return zone;
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Exception on addExitZoneId(): " + e.getMessage(), e);
			return null;
		}
	}
	
	/**
	 * Register onEventReceived trigger for NPC
	 * @param npcId id of NPC to register
	 * @return
	 */
	public L2NpcTemplate addEventReceivedId(int npcId)
	{
		return addEventId(npcId, QuestEventType.ON_EVENT_RECEIVED);
	}
	
	/**
	 * Register onEventReceived trigger for NPC
	 * @param npcIds
	 * @return
	 */
	public L2NpcTemplate[] addEventReceivedId(int... npcIds)
	{
		L2NpcTemplate[] value = new L2NpcTemplate[npcIds.length];
		int i = 0;
		for (int npcId : npcIds)
		{
			value[i++] = addEventId(npcId, QuestEventType.ON_EVENT_RECEIVED);
		}
		return value;
	}
	
	/**
	 * Register onMoveFinished trigger for NPC
	 * @param npcId id of NPC to register
	 * @return
	 */
	public L2NpcTemplate addMoveFinishedId(int npcId)
	{
		return addEventId(npcId, QuestEventType.ON_MOVE_FINISHED);
	}
	
	/**
	 * Register onMoveFinished trigger for NPC
	 * @param npcIds
	 * @return
	 */
	public L2NpcTemplate[] addMoveFinishedId(int... npcIds)
	{
		L2NpcTemplate[] value = new L2NpcTemplate[npcIds.length];
		int i = 0;
		for (int npcId : npcIds)
		{
			value[i++] = addEventId(npcId, QuestEventType.ON_MOVE_FINISHED);
		}
		return value;
	}
	
	/**
	 * Register addNodeArrived trigger for NPC
	 * @param npcId id of NPC to register
	 * @return
	 */
	public L2NpcTemplate addNodeArrivedId(int npcId)
	{
		return addEventId(npcId, QuestEventType.ON_NODE_ARRIVED);
	}
	
	/**
	 * Register addNodeArrived trigger for NPC
	 * @param npcIds id of NPC to register
	 * @return
	 */
	public L2NpcTemplate[] addNodeArrivedId(int... npcIds)
	{
		L2NpcTemplate[] value = new L2NpcTemplate[npcIds.length];
		int i = 0;
		for (int npcId : npcIds)
		{
			value[i++] = addEventId(npcId, QuestEventType.ON_NODE_ARRIVED);
		}
		return value;
	}
	
	/**
	 * Use this method to get a random party member from a player's party.<br>
	 * Useful when distributing rewards after killing an NPC.
	 * @param player this parameter represents the player whom the party will taken.
	 * @return {@code null} if {@code player} is {@code null}, {@code player} itself if the player does not have a party, and a random party member in all other cases
	 */
	public L2PcInstance getRandomPartyMember(L2PcInstance player)
	{
		if (player == null)
		{
			return null;
		}
		final L2Party party = player.getParty();
		if ((party == null) || (party.getMembers().isEmpty()))
		{
			return player;
		}
		return party.getMembers().get(Rnd.get(party.getMembers().size()));
	}
	
	/**
	 * Get a random party member with required cond value.
	 * @param player the instance of a player whose party is to be searched
	 * @param cond the value of the "cond" variable that must be matched
	 * @return a random party member that matches the specified condition, or {@code null} if no match was found
	 */
	public L2PcInstance getRandomPartyMember(L2PcInstance player, int cond)
	{
		return getRandomPartyMember(player, "cond", String.valueOf(cond));
	}
	
	/**
	 * Auxiliary function for party quests.<br>
	 * Note: This function is only here because of how commonly it may be used by quest developers.<br>
	 * For any variations on this function, the quest script can always handle things on its own.
	 * @param player the instance of a player whose party is to be searched
	 * @param var the quest variable to look for in party members. If {@code null}, it simply unconditionally returns a random party member
	 * @param value the value of the specified quest variable the random party member must have
	 * @return a random party member that matches the specified conditions or {@code null} if no match was found.<br>
	 *         If the {@code var} parameter is {@code null}, a random party member is selected without any conditions.<br>
	 *         The party member must be within a range of 1500 ingame units of the target of the reference player, or, if no target exists, within the same range of the player itself
	 */
	public L2PcInstance getRandomPartyMember(L2PcInstance player, String var, String value)
	{
		// if no valid player instance is passed, there is nothing to check...
		if (player == null)
		{
			return null;
		}
		
		// for null var condition, return any random party member.
		if (var == null)
		{
			return getRandomPartyMember(player);
		}
		
		// normal cases...if the player is not in a party, check the player's state
		QuestState temp = null;
		L2Party party = player.getParty();
		// if this player is not in a party, just check if this player instance matches the conditions itself
		if ((party == null) || (party.getMembers().isEmpty()))
		{
			temp = player.getQuestState(getName());
			if ((temp != null) && (temp.get(var) != null) && (temp.get(var)).equalsIgnoreCase(value))
			{
				return player; // match
			}
			
			return null; // no match
		}
		
		// if the player is in a party, gather a list of all matching party members (possibly
		// including this player)
		List<L2PcInstance> candidates = new ArrayList<>();
		// get the target for enforcing distance limitations.
		L2Object target = player.getTarget();
		if (target == null)
		{
			target = player;
		}
		
		for (L2PcInstance partyMember : party.getMembers())
		{
			if (partyMember == null)
			{
				continue;
			}
			temp = partyMember.getQuestState(getName());
			if ((temp != null) && (temp.get(var) != null) && (temp.get(var)).equalsIgnoreCase(value) && partyMember.isInsideRadius(target, 1500, true, false))
			{
				candidates.add(partyMember);
			}
		}
		// if there was no match, return null...
		if (candidates.isEmpty())
		{
			return null;
		}
		// TODO where's the range check?
		// if a match was found from the party, return one of them at random.
		return candidates.get(Rnd.get(candidates.size()));
	}
	
	/**
	 * Auxiliary function for party quests.<br>
	 * Note: This function is only here because of how commonly it may be used by quest developers.<br>
	 * For any variations on this function, the quest script can always handle things on its own.
	 * @param player the player whose random party member is to be selected
	 * @param state the quest state required of the random party member
	 * @return {@code null} if nothing was selected or a random party member that has the specified quest state
	 */
	public L2PcInstance getRandomPartyMemberState(L2PcInstance player, byte state)
	{
		// if no valid player instance is passed, there is nothing to check...
		if (player == null)
		{
			return null;
		}
		
		// normal cases...if the player is not in a partym check the player's state
		QuestState temp = null;
		L2Party party = player.getParty();
		// if this player is not in a party, just check if this player instance matches the conditions itself
		if ((party == null) || (party.getMembers().isEmpty()))
		{
			temp = player.getQuestState(getName());
			if ((temp != null) && (temp.getState() == state))
			{
				return player; // match
			}
			
			return null; // no match
		}
		
		// if the player is in a party, gather a list of all matching party members (possibly
		// including this player)
		List<L2PcInstance> candidates = new ArrayList<>();
		
		// get the target for enforcing distance limitations.
		L2Object target = player.getTarget();
		if (target == null)
		{
			target = player;
		}
		
		for (L2PcInstance partyMember : party.getMembers())
		{
			if (partyMember == null)
			{
				continue;
			}
			temp = partyMember.getQuestState(getName());
			if ((temp != null) && (temp.getState() == state) && partyMember.isInsideRadius(target, 1500, true, false))
			{
				candidates.add(partyMember);
			}
		}
		// if there was no match, return null...
		if (candidates.isEmpty())
		{
			return null;
		}
		
		// if a match was found from the party, return one of them at random.
		return candidates.get(Rnd.get(candidates.size()));
	}
	
	/**
	 * Show an on screen message to the player.
	 * @param player the player to display the message
	 * @param text the message
	 * @param time the display time
	 */
	public static void showOnScreenMsg(L2PcInstance player, String text, int time)
	{
		player.sendPacket(new ExShowScreenMessage(text, time));
	}
	
	/**
	 * Show an on screen message to the player.
	 * @param player the player to display the message
	 * @param npcString the NPC String to display
	 * @param position the position in the screen
	 * @param time the display time
	 * @param params parameters values to replace in the NPC String
	 */
	public static void showOnScreenMsg(L2PcInstance player, NpcStringId npcString, int position, int time, String... params)
	{
		player.sendPacket(new ExShowScreenMessage(npcString, position, time, params));
	}
	
	/**
	 * Show an on screen message to the player.
	 * @param player the player to display the message
	 * @param systemMsg the System Message to display
	 * @param position the position in the screen
	 * @param time the display time
	 * @param params parameters values to replace in the System Message
	 */
	public static void showOnScreenMsg(L2PcInstance player, SystemMessageId systemMsg, int position, int time, String... params)
	{
		player.sendPacket(new ExShowScreenMessage(systemMsg, position, time, params));
	}
	
	/**
	 * Show HTML file to client
	 * @param player
	 * @param fileName
	 * @return String message sent to client
	 */
	public String showHtmlFile(L2PcInstance player, String fileName)
	{
		boolean questwindow = !fileName.endsWith(".html");
		int questId = getQuestIntId();
		
		// Create handler to file linked to the quest
		String content = getHtm(player.getHtmlPrefix(), fileName);
		
		// Send message to client if message not empty
		if (content != null)
		{
			if (player.getTarget() != null)
			{
				content = content.replaceAll("%objectId%", Integer.toString(player.getTargetId()));
			}
			
			if (questwindow && (questId > 0) && (questId < 20000) && (questId != 999))
			{
				NpcQuestHtmlMessage npcReply = new NpcQuestHtmlMessage(player.getTargetId(), questId);
				npcReply.setHtml(content);
				npcReply.replace("%playername%", player.getName());
				player.sendPacket(npcReply);
			}
			else
			{
				NpcHtmlMessage npcReply = new NpcHtmlMessage(player.getTargetId());
				npcReply.setHtml(content);
				npcReply.replace("%playername%", player.getName());
				player.sendPacket(npcReply);
			}
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
		
		return content;
	}
	
	/**
	 * @param prefix player's language prefix.
	 * @param fileName the html file to be get.
	 * @return the HTML file contents
	 */
	public String getHtm(String prefix, String fileName)
	{
		final HtmCache hc = HtmCache.getInstance();
		String content = hc.getHtm(prefix, fileName.startsWith("data/") ? fileName : "data/scripts/" + getDescr().toLowerCase() + "/" + getName() + "/" + fileName);
		if (content == null)
		{
			content = hc.getHtm(prefix, "data/scripts/" + getDescr() + "/" + getName() + "/" + fileName);
			if (content == null)
			{
				content = hc.getHtm(prefix, "data/scripts/quests/Q" + getName() + "/" + fileName);
				if (content == null)
				{
					content = hc.getHtmForce(prefix, "data/scripts/quests/" + getName() + "/" + fileName);
				}
			}
		}
		return content;
	}
	
	/**
	 * Add a temporary (quest) spawn
	 * @param npcId
	 * @param cha
	 * @return instance of newly spawned npc
	 */
	public L2Npc addSpawn(int npcId, L2Character cha)
	{
		return addSpawn(npcId, cha.getX(), cha.getY(), cha.getZ(), cha.getHeading(), false, 0, false, 0);
	}
	
	/**
	 * Add a temporary (quest) spawn
	 * @param npcId
	 * @param cha
	 * @param isSummonSpawn
	 * @return instance of newly spawned npc with summon animation
	 */
	public L2Npc addSpawn(int npcId, L2Character cha, boolean isSummonSpawn)
	{
		return addSpawn(npcId, cha.getX(), cha.getY(), cha.getZ(), cha.getHeading(), false, 0, isSummonSpawn, 0);
	}
	
	/**
	 * @param npcId
	 * @param x
	 * @param y
	 * @param z
	 * @param heading
	 * @param randomOffSet
	 * @param despawnDelay
	 * @return
	 */
	public L2Npc addSpawn(int npcId, int x, int y, int z, int heading, boolean randomOffSet, long despawnDelay)
	{
		return addSpawn(npcId, x, y, z, heading, randomOffSet, despawnDelay, false, 0);
	}
	
	/**
	 * @param npcId
	 * @param loc
	 * @param randomOffSet
	 * @param despawnDelay
	 * @return
	 */
	public L2Npc addSpawn(int npcId, Location loc, boolean randomOffSet, long despawnDelay)
	{
		return addSpawn(npcId, loc.getX(), loc.getY(), loc.getZ(), loc.getHeading(), randomOffSet, despawnDelay, false, 0);
	}
	
	/**
	 * @param npcId
	 * @param x
	 * @param y
	 * @param z
	 * @param heading
	 * @param randomOffset
	 * @param despawnDelay
	 * @param isSummonSpawn
	 * @return
	 */
	public L2Npc addSpawn(int npcId, int x, int y, int z, int heading, boolean randomOffset, long despawnDelay, boolean isSummonSpawn)
	{
		return addSpawn(npcId, x, y, z, heading, randomOffset, despawnDelay, isSummonSpawn, 0);
	}
	
	/**
	 * @param npcId
	 * @param loc
	 * @param randomOffset
	 * @param despawnDelay
	 * @param isSummonSpawn
	 * @return
	 */
	public L2Npc addSpawn(int npcId, Location loc, boolean randomOffset, long despawnDelay, boolean isSummonSpawn)
	{
		return addSpawn(npcId, loc.getX(), loc.getY(), loc.getZ(), loc.getHeading(), randomOffset, despawnDelay, isSummonSpawn, 0);
	}
	
	/**
	 * @param npcId
	 * @param loc
	 * @param randomOffset
	 * @param despawnDelay
	 * @param isSummonSpawn
	 * @param instanceId
	 * @return
	 */
	public L2Npc addSpawn(int npcId, Location loc, boolean randomOffset, long despawnDelay, boolean isSummonSpawn, int instanceId)
	{
		return addSpawn(npcId, loc.getX(), loc.getY(), loc.getZ(), loc.getHeading(), randomOffset, despawnDelay, isSummonSpawn, instanceId);
	}
	
	/**
	 * @param npcId
	 * @param x
	 * @param y
	 * @param z
	 * @param heading
	 * @param randomOffset
	 * @param despawnDelay
	 * @param isSummonSpawn
	 * @param instanceId
	 * @return
	 */
	public L2Npc addSpawn(int npcId, int x, int y, int z, int heading, boolean randomOffset, long despawnDelay, boolean isSummonSpawn, int instanceId)
	{
		L2Npc result = null;
		try
		{
			L2NpcTemplate template = NpcTable.getInstance().getTemplate(npcId);
			if (template != null)
			{
				// Sometimes, even if the quest script specifies some xyz (for example npc.getX() etc) by the time the code
				// reaches here, xyz have become 0! Also, a questdev might have purposely set xy to 0,0...however,
				// the spawn code is coded such that if x=y=0, it looks into location for the spawn loc! This will NOT work
				// with quest spawns! For both of the above cases, we need a fail-safe spawn. For this, we use the
				// default spawn location, which is at the player's loc.
				if ((x == 0) && (y == 0))
				{
					_log.log(Level.SEVERE, "Failed to adjust bad locks for quest spawn!  Spawn aborted!");
					return null;
				}
				if (randomOffset)
				{
					int offset;
					
					offset = Rnd.get(2); // Get the direction of the offset
					if (offset == 0)
					{
						// make offset negative
						offset = -1;
					}
					offset *= Rnd.get(50, 100);
					x += offset;
					
					offset = Rnd.get(2); // Get the direction of the offset
					if (offset == 0)
					{
						// make offset negative
						offset = -1;
					}
					offset *= Rnd.get(50, 100);
					y += offset;
				}
				L2Spawn spawn = new L2Spawn(template);
				spawn.setInstanceId(instanceId);
				spawn.setHeading(heading);
				spawn.setLocx(x);
				spawn.setLocy(y);
				spawn.setLocz(z);
				spawn.stopRespawn();
				result = spawn.spawnOne(isSummonSpawn);
				
				if (despawnDelay > 0)
				{
					result.scheduleDespawn(despawnDelay);
				}
				
				return result;
			}
		}
		catch (Exception e1)
		{
			_log.warning("Could not spawn Npc " + npcId + " Error: " + e1.getMessage());
		}
		
		return null;
	}
	
	/**
	 * @param trapId
	 * @param x
	 * @param y
	 * @param z
	 * @param heading
	 * @param skill
	 * @param instanceId
	 * @return
	 */
	public L2Trap addTrap(int trapId, int x, int y, int z, int heading, L2Skill skill, int instanceId)
	{
		L2NpcTemplate TrapTemplate = NpcTable.getInstance().getTemplate(trapId);
		L2Trap trap = new L2TrapInstance(IdFactory.getInstance().getNextId(), TrapTemplate, instanceId, -1, skill);
		trap.setCurrentHp(trap.getMaxHp());
		trap.setCurrentMp(trap.getMaxMp());
		trap.setIsInvul(true);
		trap.setHeading(heading);
		// L2World.getInstance().storeObject(trap);
		trap.spawnMe(x, y, z);
		
		return trap;
	}
	
	/**
	 * @param master
	 * @param minionId
	 * @return
	 */
	public L2Npc addMinion(L2MonsterInstance master, int minionId)
	{
		return MinionList.spawnMinion(master, minionId);
	}
	
	/**
	 * @return the registered quest items Ids.
	 */
	public int[] getRegisteredItemIds()
	{
		return questItemIds;
	}
	
	/**
	 * Registers all items that have to be destroyed in case player abort the quest or finish it.
	 * @param items
	 */
	public void registerQuestItems(int... items)
	{
		questItemIds = items;
	}
	
	@Override
	public String getScriptName()
	{
		return getName();
	}
	
	@Override
	public void setActive(boolean status)
	{
		// TODO implement me
	}
	
	@Override
	public boolean reload()
	{
		unload();
		return super.reload();
	}
	
	@Override
	public boolean unload()
	{
		return unload(true);
	}
	
	/**
	 * @param removeFromList
	 * @return
	 */
	public boolean unload(boolean removeFromList)
	{
		saveGlobalData();
		// cancel all pending timers before reloading.
		// if timers ought to be restarted, the quest can take care of it
		// with its code (example: save global data indicating what timer must be restarted).
		for (List<QuestTimer> timers : _allEventTimers.values())
		{
			_readLock.lock();
			try
			{
				for (QuestTimer timer : timers)
				{
					timer.cancel();
				}
			}
			finally
			{
				_readLock.unlock();
			}
			timers.clear();
		}
		_allEventTimers.clear();
		
		for (Integer npcId : _questInvolvedNpcs)
		{
			L2NpcTemplate template = NpcTable.getInstance().getTemplate(npcId.intValue());
			if (template != null)
			{
				template.removeQuest(this);
			}
		}
		_questInvolvedNpcs.clear();
		
		if (removeFromList)
		{
			return QuestManager.getInstance().removeQuest(this);
		}
		return true;
	}
	
	public Set<Integer> getQuestInvolvedNpcs()
	{
		return _questInvolvedNpcs;
	}
	
	@Override
	public ScriptManager<?> getScriptManager()
	{
		return QuestManager.getInstance();
	}
	
	/**
	 * @param val
	 */
	public void setOnEnterWorld(boolean val)
	{
		_onEnterWorld = val;
	}
	
	/**
	 * @return
	 */
	public boolean getOnEnterWorld()
	{
		return _onEnterWorld;
	}
	
	/**
	 * If a quest is set as custom, it will display it's name in the NPC Quest List.<br>
	 * Retail quests are unhardcoded to display the name using a client string.
	 * @param val if {@code true} the quest script will be set as custom quest.
	 */
	public void setIsCustom(boolean val)
	{
		_isCustom = val;
	}
	
	/**
	 * @return {@code true} if the quest script is a custom quest, {@code false} otherwise.
	 */
	public boolean isCustomQuest()
	{
		return _isCustom;
	}
	
	/**
	 * @param val
	 */
	public void setOlympiadUse(boolean val)
	{
		_isOlympiadUse = val;
	}
	
	/**
	 * @return {@code true} if the quest script is used for Olympiad quests, {@code false} otherwise.
	 */
	public boolean isOlympiadUse()
	{
		return _isOlympiadUse;
	}
	
	/**
	 * Get the amount of an item in player's inventory.
	 * @param player the player whose inventory to check
	 * @param itemId the Id of the item whose amount to get
	 * @return the amount of the specified item in player's inventory
	 */
	public long getQuestItemsCount(L2PcInstance player, int itemId)
	{
		return player.getInventory().getInventoryItemCount(itemId, -1);
	}
	
	/**
	 * Get the total amount of all specified items in player's inventory.
	 * @param player the player whose inventory to check
	 * @param itemIds a list of Ids of items whose amount to get
	 * @return the summary amount of all listed items in player's inventory
	 */
	public long getQuestItemsCount(L2PcInstance player, int... itemIds)
	{
		long count = 0;
		for (L2ItemInstance item : player.getInventory().getItems())
		{
			if (item == null)
			{
				continue;
			}
			
			for (int itemId : itemIds)
			{
				if (item.getItemId() == itemId)
				{
					if ((count + item.getCount()) > Long.MAX_VALUE)
					{
						return Long.MAX_VALUE;
					}
					count += item.getCount();
				}
			}
		}
		return count;
	}
	
	/**
	 * Check for an item in player's inventory.
	 * @param player the player whose inventory to check for quest items
	 * @param itemId the Id of the item to check for
	 * @return {@code true} if the item exists in player's inventory, {@code false} otherwise
	 */
	public boolean hasQuestItems(L2PcInstance player, int itemId)
	{
		return player.getInventory().getItemByItemId(itemId) != null;
	}
	
	/**
	 * Check for multiple items in player's inventory.
	 * @param player the player whose inventory to check for quest items
	 * @param itemIds a list of item Ids to check for
	 * @return {@code true} if all items exist in player's inventory, {@code false} otherwise
	 */
	public boolean hasQuestItems(L2PcInstance player, int... itemIds)
	{
		final PcInventory inv = player.getInventory();
		for (int itemId : itemIds)
		{
			if (inv.getItemByItemId(itemId) == null)
			{
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Check for multiple items in player's inventory.
	 * @param player the player whose inventory to check for quest items
	 * @param itemIds a list of item Ids to check for
	 * @return {@code true} if at least one items exist in player's inventory, {@code false} otherwise
	 */
	public boolean hasAtLeastOneQuestItem(L2PcInstance player, int... itemIds)
	{
		final PcInventory inv = player.getInventory();
		for (int itemId : itemIds)
		{
			if (inv.getItemByItemId(itemId) != null)
			{
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Get the enchantment level of an item in player's inventory.
	 * @param player the player whose item to check
	 * @param itemId the Id of the item whose enchantment level to get
	 * @return the enchantment level of the item or 0 if the item was not found
	 */
	public int getEnchantLevel(L2PcInstance player, int itemId)
	{
		L2ItemInstance enchantedItem = player.getInventory().getItemByItemId(itemId);
		if (enchantedItem == null)
		{
			return 0;
		}
		return enchantedItem.getEnchantLevel();
	}
	
	/**
	 * Give Adena to the player.
	 * @param player the player to whom to give the Adena
	 * @param count the amount of Adena to give
	 * @param applyRates if {@code true} quest rates will be applied to the amount
	 */
	public void giveAdena(L2PcInstance player, long count, boolean applyRates)
	{
		if (applyRates)
		{
			rewardItems(player, PcInventory.ADENA_ID, count);
		}
		else
		{
			giveItems(player, PcInventory.ADENA_ID, count);
		}
	}
	
	/**
	 * Give a reward to player using multipliers.
	 * @param player the player to whom to give the item
	 * @param itemId the Id of the item to give
	 * @param count the amount of items to give
	 */
	public void rewardItems(L2PcInstance player, int itemId, long count)
	{
		if (count <= 0)
		{
			return;
		}
		
		L2ItemInstance _tmpItem = ItemTable.getInstance().createDummyItem(itemId);
		
		if (_tmpItem == null)
		{
			return;
		}
		
		try
		{
			if (itemId == PcInventory.ADENA_ID)
			{
				count *= Config.RATE_QUEST_REWARD_ADENA;
			}
			else if (Config.RATE_QUEST_REWARD_USE_MULTIPLIERS)
			{
				if (_tmpItem.isEtcItem())
				{
					switch (_tmpItem.getEtcItem().getItemType())
					{
						case POTION:
							count *= Config.RATE_QUEST_REWARD_POTION;
							break;
						case SCRL_ENCHANT_WP:
						case SCRL_ENCHANT_AM:
						case SCROLL:
							count *= Config.RATE_QUEST_REWARD_SCROLL;
							break;
						case RECIPE:
							count *= Config.RATE_QUEST_REWARD_RECIPE;
							break;
						case MATERIAL:
							count *= Config.RATE_QUEST_REWARD_MATERIAL;
							break;
						default:
							count *= Config.RATE_QUEST_REWARD;
					}
				}
			}
			else
			{
				count *= Config.RATE_QUEST_REWARD;
			}
		}
		catch (Exception e)
		{
			count = Long.MAX_VALUE;
		}
		
		// Add items to player's inventory
		L2ItemInstance item = player.getInventory().addItem("Quest", itemId, count, player, player.getTarget());
		if (item == null)
		{
			return;
		}
		
		sendItemGetMessage(player, item, count);
	}
	
	/**
	 * Send the system message and the status update packets to the player.
	 * @param player the player that has got the item
	 * @param item the item obtain by the player
	 * @param count the item count
	 */
	private void sendItemGetMessage(L2PcInstance player, L2ItemInstance item, long count)
	{
		// If item for reward is gold, send message of gold reward to client
		if (item.getItemId() == PcInventory.ADENA_ID)
		{
			SystemMessage smsg = SystemMessage.getSystemMessage(SystemMessageId.EARNED_S1_ADENA);
			smsg.addItemNumber(count);
			player.sendPacket(smsg);
		}
		// Otherwise, send message of object reward to client
		else
		{
			if (count > 1)
			{
				SystemMessage smsg = SystemMessage.getSystemMessage(SystemMessageId.EARNED_S2_S1_S);
				smsg.addItemName(item);
				smsg.addItemNumber(count);
				player.sendPacket(smsg);
			}
			else
			{
				SystemMessage smsg = SystemMessage.getSystemMessage(SystemMessageId.EARNED_ITEM_S1);
				smsg.addItemName(item);
				player.sendPacket(smsg);
			}
		}
		// send packets
		StatusUpdate su = new StatusUpdate(player);
		su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
		player.sendPacket(su);
	}
	
	/**
	 * Give item/reward to the player
	 * @param player
	 * @param itemId
	 * @param count
	 */
	public void giveItems(L2PcInstance player, int itemId, long count)
	{
		giveItems(player, itemId, count, 0);
	}
	
	/**
	 * Give item/reward to the player
	 * @param player
	 * @param holder
	 */
	protected void giveItems(L2PcInstance player, ItemHolder holder)
	{
		giveItems(player, holder.getId(), holder.getCount());
	}
	
	/**
	 * @param player
	 * @param itemId
	 * @param count
	 * @param enchantlevel
	 */
	public void giveItems(L2PcInstance player, int itemId, long count, int enchantlevel)
	{
		if (count <= 0)
		{
			return;
		}
		
		// If item for reward is adena (Id=57), modify count with rate for quest reward if rates available
		if ((itemId == PcInventory.ADENA_ID) && (enchantlevel == 0))
		{
			count = (long) (count * Config.RATE_QUEST_REWARD_ADENA);
		}
		
		// Add items to player's inventory
		L2ItemInstance item = player.getInventory().addItem("Quest", itemId, count, player, player.getTarget());
		if (item == null)
		{
			return;
		}
		
		// set enchant level for item if that item is not adena
		if ((enchantlevel > 0) && (itemId != PcInventory.ADENA_ID))
		{
			item.setEnchantLevel(enchantlevel);
		}
		
		sendItemGetMessage(player, item, count);
	}
	
	/**
	 * @param player
	 * @param itemId
	 * @param count
	 * @param attributeId
	 * @param attributeLevel
	 */
	public void giveItems(L2PcInstance player, int itemId, long count, byte attributeId, int attributeLevel)
	{
		if (count <= 0)
		{
			return;
		}
		
		// Add items to player's inventory
		L2ItemInstance item = player.getInventory().addItem("Quest", itemId, count, player, player.getTarget());
		
		if (item == null)
		{
			return;
		}
		
		// set enchant level for item if that item is not adena
		if ((attributeId >= 0) && (attributeLevel > 0))
		{
			item.setElementAttr(attributeId, attributeLevel);
			if (item.isEquipped())
			{
				item.updateElementAttrBonus(player);
			}
			
			InventoryUpdate iu = new InventoryUpdate();
			iu.addModifiedItem(item);
			player.sendPacket(iu);
		}
		
		sendItemGetMessage(player, item, count);
	}
	
	/**
	 * Drop Quest item using Config.RATE_QUEST_DROP
	 * @param player
	 * @param itemId int Item Identifier of the item to be dropped
	 * @param count (minCount, maxCount) long Quantity of items to be dropped
	 * @param neededCount Quantity of items needed for quest
	 * @param dropChance int Base chance of drop, same as in droplist
	 * @param sound boolean indicating whether to play sound
	 * @return boolean indicating whether player has requested number of items
	 */
	public boolean dropQuestItems(L2PcInstance player, int itemId, int count, long neededCount, int dropChance, boolean sound)
	{
		return dropQuestItems(player, itemId, count, count, neededCount, dropChance, sound);
	}
	
	/**
	 * @param player
	 * @param itemId
	 * @param minCount
	 * @param maxCount
	 * @param neededCount
	 * @param dropChance
	 * @param sound
	 * @return
	 */
	public boolean dropQuestItems(L2PcInstance player, int itemId, int minCount, int maxCount, long neededCount, int dropChance, boolean sound)
	{
		dropChance *= Config.RATE_QUEST_DROP / ((player.getParty() != null) ? player.getParty().getMemberCount() : 1);
		long currentCount = getQuestItemsCount(player, itemId);
		
		if ((neededCount > 0) && (currentCount >= neededCount))
		{
			return true;
		}
		
		if (currentCount >= neededCount)
		{
			return true;
		}
		
		long itemCount = 0;
		int random = Rnd.get(L2DropData.MAX_CHANCE);
		
		while (random < dropChance)
		{
			// Get the item quantity dropped
			if (minCount < maxCount)
			{
				itemCount += Rnd.get(minCount, maxCount);
			}
			else if (minCount == maxCount)
			{
				itemCount += minCount;
			}
			else
			{
				itemCount++;
			}
			
			// Prepare for next iteration if dropChance > L2DropData.MAX_CHANCE
			dropChance -= L2DropData.MAX_CHANCE;
		}
		
		if (itemCount > 0)
		{
			// if over neededCount, just fill the gap
			if ((neededCount > 0) && ((currentCount + itemCount) > neededCount))
			{
				itemCount = neededCount - currentCount;
			}
			
			// Inventory slot check
			if (!player.getInventory().validateCapacityByItemId(itemId))
			{
				return false;
			}
			
			// Give the item to Player
			player.addItem("Quest", itemId, itemCount, player.getTarget(), true);
			
			if (sound)
			{
				playSound(player, ((currentCount + itemCount) < neededCount) ? QuestSound.ITEMSOUND_QUEST_ITEMGET : QuestSound.ITEMSOUND_QUEST_MIDDLE);
			}
		}
		
		return ((neededCount > 0) && ((currentCount + itemCount) >= neededCount));
	}
	
	/**
	 * Take an amount of a specified item from player's inventory.
	 * @param player the player whose item to take
	 * @param itemId the Id of the item to take
	 * @param amount the amount to take
	 * @return {@code true} if any items were taken, {@code false} otherwise
	 */
	public boolean takeItems(L2PcInstance player, int itemId, long amount)
	{
		// Get object item from player's inventory list
		L2ItemInstance item = player.getInventory().getItemByItemId(itemId);
		if (item == null)
		{
			return false;
		}
		
		// Tests on count value in order not to have negative value
		if ((amount < 0) || (amount > item.getCount()))
		{
			amount = item.getCount();
		}
		
		// Destroy the quantity of items wanted
		if (item.isEquipped())
		{
			L2ItemInstance[] unequiped = player.getInventory().unEquipItemInBodySlotAndRecord(item.getItem().getBodyPart());
			InventoryUpdate iu = new InventoryUpdate();
			for (L2ItemInstance itm : unequiped)
			{
				iu.addModifiedItem(itm);
			}
			player.sendPacket(iu);
			player.broadcastUserInfo();
		}
		return player.destroyItemByItemId("Quest", itemId, amount, player, true);
	}
	
	/**
	 * Take an amount of a specified item from player's inventory.
	 * @param player
	 * @param holder
	 * @return {@code true} if any items were taken, {@code false} otherwise
	 */
	protected boolean takeItems(L2PcInstance player, ItemHolder holder)
	{
		return takeItems(player, holder.getId(), holder.getCount());
	}
	
	/**
	 * Take an amount of all specified items from player's inventory.
	 * @param player the player whose items to take
	 * @param amount the amount to take of each item
	 * @param itemIds a list or an array of Ids of the items to take
	 * @return {@code true} if all items were taken, {@code false} otherwise
	 */
	public boolean takeItems(L2PcInstance player, int amount, int... itemIds)
	{
		boolean check = true;
		if (itemIds != null)
		{
			for (int item : itemIds)
			{
				check &= takeItems(player, item, amount);
			}
		}
		return check;
	}
	
	/**
	 * Remove all quest items associated with this quest from the specified player's inventory.
	 * @param player the player whose quest items to remove
	 */
	public void removeRegisteredQuestItems(L2PcInstance player)
	{
		takeItems(player, -1, questItemIds);
	}
	
	/**
	 * Send a packet in order to play a sound to the player.
	 * @param player the player whom to send the packet
	 * @param sound the name of the sound to play
	 */
	public void playSound(L2PcInstance player, String sound)
	{
		player.sendPacket(QuestSound.getSound(sound));
	}
	
	/**
	 * Send a packet in order to play a sound to the player.
	 * @param player the player whom to send the packet
	 * @param sound the {@link QuestSound} object of the sound to play
	 */
	public void playSound(L2PcInstance player, QuestSound sound)
	{
		player.sendPacket(sound.getPacket());
	}
	
	/**
	 * Add EXP and SP as quest reward.
	 * @param player the player whom to reward with the EXP/SP
	 * @param exp the amount of EXP to give to the player
	 * @param sp the amount of SP to give to the player
	 */
	public void addExpAndSp(L2PcInstance player, long exp, int sp)
	{
		player.addExpAndSp((long) player.calcStat(Stats.EXPSP_RATE, exp * Config.RATE_QUEST_REWARD_XP, null, null), (int) player.calcStat(Stats.EXPSP_RATE, sp * Config.RATE_QUEST_REWARD_SP, null, null));
	}
	
	/**
	 * Get a random integer from 0 (inclusive) to {@code max} (exclusive).<br>
	 * Use this method instead of importing {@link com.l2jserver.util.Rnd} utility.
	 * @param max the maximum value for randomization
	 * @return a random integer number from 0 to {@code max - 1}
	 */
	public static int getRandom(int max)
	{
		return Rnd.get(max);
	}
	
	/**
	 * Get a random integer from {@code min} (inclusive) to {@code max} (inclusive).<br>
	 * Use this method instead of importing {@link com.l2jserver.util.Rnd} utility.
	 * @param min the minimum value for randomization
	 * @param max the maximum value for randomization
	 * @return a random integer number from {@code min} to {@code max}
	 */
	public static int getRandom(int min, int max)
	{
		return Rnd.get(min, max);
	}
	
	/**
	 * Get the Id of the item equipped in the specified inventory slot of the player.
	 * @param player the player whose inventory to check
	 * @param slot the location in the player's inventory to check
	 * @return the Id of the item equipped in the specified inventory slot or 0 if the slot is empty or item is {@code null}.
	 */
	public int getItemEquipped(L2PcInstance player, int slot)
	{
		return player.getInventory().getPaperdollItemId(slot);
	}
	
	/**
	 * @return the number of ticks from the {@link com.l2jserver.gameserver.GameTimeController}.
	 */
	public int getGameTicks()
	{
		return GameTimeController.getGameTicks();
	}
	
	/**
	 * Executes a procedure for each player, depending on the parameters.
	 * @param player the player were the procedure will be executed
	 * @param npc the related Npc
	 * @param isSummon {@code true} if the event that call this method was originated by the player's summon
	 * @param includeParty if {@code true} #actionForEachPlayer(L2PcInstance, L2Npc, boolean) will be called with the player's party members
	 * @param includeCommandChannel if {@code true} {@link #actionForEachPlayer(L2PcInstance, L2Npc, boolean)} will be called with the player's command channel members
	 * @see #actionForEachPlayer(L2PcInstance, L2Npc, boolean)
	 */
	public final void executeForEachPlayer(L2PcInstance player, final L2Npc npc, final boolean isSummon, boolean includeParty, boolean includeCommandChannel)
	{
		if ((includeParty || includeCommandChannel) && player.isInParty())
		{
			if (includeCommandChannel && player.getParty().isInCommandChannel())
			{
				player.getParty().getCommandChannel().forEachMember(new IL2Procedure<L2PcInstance>()
				{
					@Override
					public boolean execute(L2PcInstance member)
					{
						actionForEachPlayer(member, npc, isSummon);
						return true;
					}
				});
			}
			else if (includeParty)
			{
				player.getParty().forEachMember(new IL2Procedure<L2PcInstance>()
				{
					@Override
					public boolean execute(L2PcInstance member)
					{
						actionForEachPlayer(member, npc, isSummon);
						return true;
					}
				});
			}
		}
		else
		{
			actionForEachPlayer(player, npc, isSummon);
		}
	}
	
	/**
	 * Overridable method called from {@link #executeForEachPlayer(L2PcInstance, L2Npc, boolean, boolean, boolean)}
	 * @param player the player where the action will be run
	 * @param npc the Npc related to this action
	 * @param isSummon {@code true} if the event that call this method was originated by the player's summon
	 */
	public void actionForEachPlayer(L2PcInstance player, L2Npc npc, boolean isSummon)
	{
		// To be overridden in quest scripts.
	}
	
	/**
	 * Opens the door if presents on the instance and its not open.
	 * @param doorId
	 * @param instanceId
	 */
	public void openDoor(int doorId, int instanceId)
	{
		final L2DoorInstance door = getDoor(doorId, instanceId);
		if (door == null)
		{
			_log.log(Level.WARNING, getClass().getSimpleName() + ": called openDoor(" + doorId + ", " + instanceId + "); but door wasnt found!", new NullPointerException());
		}
		else if (!door.getOpen())
		{
			door.openMe();
		}
	}
	
	/**
	 * Closes the door if presents on the instance and its open
	 * @param doorId
	 * @param instanceId
	 */
	public void closeDoor(int doorId, int instanceId)
	{
		final L2DoorInstance door = getDoor(doorId, instanceId);
		if (door == null)
		{
			_log.log(Level.WARNING, getClass().getSimpleName() + ": called closeDoor(" + doorId + ", " + instanceId + "); but door wasnt found!", new NullPointerException());
		}
		else if (door.getOpen())
		{
			door.closeMe();
		}
	}
	
	/**
	 * Retriving Door from instances or from the real world.
	 * @param doorId
	 * @param instanceId
	 * @return {@link L2DoorInstance}
	 */
	public L2DoorInstance getDoor(int doorId, int instanceId)
	{
		L2DoorInstance door = null;
		if (instanceId <= 0)
		{
			door = DoorTable.getInstance().getDoor(doorId);
		}
		else
		{
			final Instance inst = InstanceManager.getInstance().getInstance(instanceId);
			if (inst != null)
			{
				door = inst.getDoor(doorId);
			}
		}
		return door;
	}
	
	/**
	 * Teleport player to/from instance
	 * @param player
	 * @param loc
	 * @param instanceId
	 */
	public void teleportPlayer(L2PcInstance player, Location loc, int instanceId)
	{
		teleportPlayer(player, loc, instanceId, true);
	}
	
	/**
	 * Teleport player to/from instance
	 * @param player
	 * @param loc
	 * @param instanceId
	 * @param allowRandomOffset
	 */
	public void teleportPlayer(L2PcInstance player, Location loc, int instanceId, boolean allowRandomOffset)
	{
		loc.setInstanceId(instanceId);
		player.teleToLocation(loc, allowRandomOffset);
	}
}
