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
package com.l2jserver.gameserver.model.quest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;

import com.l2jserver.Config;
import com.l2jserver.L2DatabaseFactory;
import com.l2jserver.gameserver.GameTimeController;
import com.l2jserver.gameserver.ThreadPoolManager;
import com.l2jserver.gameserver.cache.HtmCache;
import com.l2jserver.gameserver.datatables.ItemTable;
import com.l2jserver.gameserver.datatables.NpcTable;
import com.l2jserver.gameserver.idfactory.IdFactory;
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
import com.l2jserver.gameserver.model.actor.instance.L2MonsterInstance;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.actor.instance.L2TrapInstance;
import com.l2jserver.gameserver.model.actor.templates.L2NpcTemplate;
import com.l2jserver.gameserver.model.itemcontainer.PcInventory;
import com.l2jserver.gameserver.model.items.L2Item;
import com.l2jserver.gameserver.model.items.instance.L2ItemInstance;
import com.l2jserver.gameserver.model.olympiad.CompetitionType;
import com.l2jserver.gameserver.model.skills.L2Skill;
import com.l2jserver.gameserver.model.stats.Stats;
import com.l2jserver.gameserver.model.zone.L2ZoneType;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.ActionFailed;
import com.l2jserver.gameserver.network.serverpackets.InventoryUpdate;
import com.l2jserver.gameserver.network.serverpackets.NpcHtmlMessage;
import com.l2jserver.gameserver.network.serverpackets.NpcQuestHtmlMessage;
import com.l2jserver.gameserver.network.serverpackets.PlaySound;
import com.l2jserver.gameserver.network.serverpackets.StatusUpdate;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;
import com.l2jserver.gameserver.scripting.ManagedScript;
import com.l2jserver.gameserver.scripting.ScriptManager;
import com.l2jserver.gameserver.util.MinionList;
import com.l2jserver.util.Rnd;
import com.l2jserver.util.Util;

/**
 * @author Luis Arias
 */
public class Quest extends ManagedScript
{
	protected static final Logger _log = Logger.getLogger(Quest.class.getName());
	
	/**
	 * HashMap containing events from String value of the event.
	 */
	private static Map<String, Quest> _allEventsS = new FastMap<String, Quest>();
	/**
	 * HashMap containing lists of timers from the name of the timer.
	 */
	private final Map<String, FastList<QuestTimer>> _allEventTimers = new FastMap<String, FastList<QuestTimer>>().shared();
	
	private final ReentrantReadWriteLock _rwLock = new ReentrantReadWriteLock();
	
	private final int _questId;
	private final String _name;
	private final String _descr;
	private final byte _initialState = State.CREATED;
	protected boolean _onEnterWorld = false;
	private boolean _isCustom = false;
	private boolean _isOlympiadUse = false;
	
	/**
	 * <b>Note: questItemIds will be overridden by child classes.</b><br>
	 * Ideally, it should be protected instead of public.<br>
	 * However, quest scripts written in Jython will have trouble with protected, as Jython only knows private and public...<br>
	 * In fact, protected will typically be considered private thus breaking the scripts.<br>
	 * Leave this as public as a workaround.
	 */
	public int[] questItemIds = null;
	
	private static final String DEFAULT_NO_QUEST_MSG = "<html><body>You are either not on a quest that involves this NPC, or you don't meet this NPC's minimum quest requirements.</body></html>";
	private static final String DEFAULT_ALREADY_COMPLETED_MSG = "<html><body>This quest has already been completed.</body></html>";
	
	private static final int RESET_HOUR = 6;
	private static final int RESET_MINUTES = 30;
	
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
	 * @return a collection of the values contained in the {@link #_allEventsS}.
	 */
	public static Collection<Quest> findAllEvents()
	{
		return _allEventsS.values();
	}
	
	/**
	 * The Quest object constructor.<br>
	 * Constructing a quest also calls the {@code init_LoadGlobalData} convenience method.
	 * @param questId int pointing out the ID of the quest.
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
	 * By default, nothing is loaded.<br>
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
		ON_ITEM_USE(true);
		
		// control whether this event type is allowed for the same npc template in multiple quests
		// or if the npc must be registered in at most one quest for the specified event
		private boolean _allowMultipleRegistration;
		
		QuestEventType(boolean allowMultipleRegistration)
		{
			_allowMultipleRegistration = allowMultipleRegistration;
		}
		
		public boolean isMultipleRegistrationAllowed()
		{
			return _allowMultipleRegistration;
		}
	}
	
	/**
	 * @return the Id of the quest.
	 */
	public int getQuestIntId()
	{
		return _questId;
	}
	
	/**
	 * Add a new quest state of this quest to the database.
	 * @param player the owner of the newly created quest state.
	 * @return the newly created quest state object.
	 */
	public QuestState newQuestState(L2PcInstance player)
	{
		return new QuestState(this, player, getInitialState());
	}
	
	/**
	 * @return the initial state of the quest.
	 */
	public byte getInitialState()
	{
		return _initialState;
	}
	
	/**
	 * @return the name of the quest.
	 */
	public String getName()
	{
		return _name;
	}
	
	/**
	 * @return the description of the quest.
	 */
	public String getDescr()
	{
		return _descr;
	}
	
	/**
	 * Add a timer to the quest (if it doesn't exist already) and start it.
	 * @param name the name of the timer (also passed back as "event" in {@link #onAdvEvent(String, L2Npc, L2PcInstance)}).
	 * @param time the time in ms for when to fire the timer.
	 * @param npc the npc associated with this timer (can be null).
	 * @param player player associated with this timer (can be null).
	 * @see #startQuestTimer(String, long, L2Npc, L2PcInstance, boolean)
	 */
	public void startQuestTimer(String name, long time, L2Npc npc, L2PcInstance player)
	{
		startQuestTimer(name, time, npc, player, false);
	}
	
	/**
	 * Add a timer to the quest (if it doesn't exist already) and start it.
	 * @param name the name of the timer (also passed back as "event" in onAdvEvent)
	 * @param time the time in ms for when to fire the timer
	 * @param npc the npc associated with this timer (can be null)
	 * @param player the player associated with this timer (can be null)
	 * @param repeating indicates if the timer is repeatable or one-time.<br>
	 *            If {@code true}, it will auto-fire automatically, at a fixed rate, until explicitly canceled.
	 */
	public void startQuestTimer(String name, long time, L2Npc npc, L2PcInstance player, boolean repeating)
	{
		FastList<QuestTimer> timers = getQuestTimers(name);
		// Add quest timer if timer doesn't already exist
		if (timers == null)
		{
			timers = new FastList<QuestTimer>();
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
				try
				{
					_rwLock.writeLock().lock();
					timers.add(new QuestTimer(this, name, time, npc, player, repeating));
				}
				finally
				{
					_rwLock.writeLock().unlock();
				}
			}
		}
	}
	
	/**
	 * Get a quest timer that matches the provided name and parameters.
	 * @param name the name of the quest timer to get.
	 * @param npc the NPC associated with the quest timer to get.
	 * @param player the player associated with the quest timer to get.
	 * @return the quest timer that matches the parameters of this function or {@code null} if nothing was found.
	 */
	public QuestTimer getQuestTimer(String name, L2Npc npc, L2PcInstance player)
	{
		final FastList<QuestTimer> qt = getQuestTimers(name);
		if ((qt == null) || qt.isEmpty())
		{
			return null;
		}
		try
		{
			_rwLock.readLock().lock();
			for (QuestTimer timer : qt)
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
			_rwLock.readLock().unlock();
		}
		return null;
	}
	
	/**
	 * Get all quest timers with the specified name.
	 * @param name the name of the quest timers to get.
	 * @return a list of all quest timers matching the given name or {@code null} if none was found.
	 */
	private FastList<QuestTimer> getQuestTimers(String name)
	{
		return _allEventTimers.get(name);
	}
	
	/**
	 * Cancel all quest timers with the specified name.
	 * @param name the name of the quest timers to cancel.
	 */
	public void cancelQuestTimers(String name)
	{
		FastList<QuestTimer> timers = getQuestTimers(name);
		if (timers == null)
		{
			return;
		}
		try
		{
			_rwLock.writeLock().lock();
			for (QuestTimer timer : timers)
			{
				if (timer != null)
				{
					timer.cancel();
				}
			}
		}
		finally
		{
			_rwLock.writeLock().unlock();
		}
	}
	
	/**
	 * Cancel the quest timer that matches the specified name and parameters.
	 * @param name the name of the quest timer to cancel.
	 * @param npc the NPC associated with the quest timer to cancel.
	 * @param player the player associated with the quest timer to cancel.
	 */
	public void cancelQuestTimer(String name, L2Npc npc, L2PcInstance player)
	{
		QuestTimer timer = getQuestTimer(name, npc, player);
		if (timer != null)
		{
			timer.cancel();
		}
	}
	
	/**
	 * Remove a quest timer from the list of all timers.<br>
	 * Note: does not stop the timer itself!
	 * @param timer the quest timer object to remove.
	 */
	public void removeQuestTimer(QuestTimer timer)
	{
		if (timer == null)
		{
			return;
		}
		final FastList<QuestTimer> timers = getQuestTimers(timer.getName());
		if (timers == null)
		{
			return;
		}
		try
		{
			_rwLock.writeLock().lock();
			timers.remove(timer);
		}
		finally
		{
			_rwLock.writeLock().unlock();
		}
	}
	
	// These are methods to call within the core to call the quest events.
	
	/**
	 * @param npc the NPC that was attacked.
	 * @param attacker the attacking player.
	 * @param damage the damage dealt to the NPC by the player.
	 * @param isPet if {@code true}, the attack was actually made by the player's pet.
	 * @param skill the skill used to attack the NPC (can be null).
	 * @return
	 */
	public final boolean notifyAttack(L2Npc npc, L2PcInstance attacker, int damage, boolean isPet, L2Skill skill)
	{
		String res = null;
		try
		{
			res = onAttack(npc, attacker, damage, isPet, skill);
		}
		catch (Exception e)
		{
			return showError(attacker, e);
		}
		return showResult(attacker, res);
	}
	
	/**
	 * @param killer the character that killed the victim.
	 * @param victim the character that was killed by the killer.
	 * @param qs the quest state object of the player to be notified of this event.
	 * @return {@code false} if there is an error or a message sent, {@code true} otherwise.
	 */
	public final boolean notifyDeath(L2Character killer, L2Character victim, QuestState qs)
	{
		String res = null;
		try
		{
			res = onDeath(killer, victim, qs);
		}
		catch (Exception e)
		{
			return showError(qs.getPlayer(), e);
		}
		return showResult(qs.getPlayer(), res);
	}
	
	/**
	 * @param item
	 * @param player
	 * @return
	 */
	public final boolean notifyItemUse(L2Item item, L2PcInstance player)
	{
		String res = null;
		try
		{
			res = onItemUse(item, player);
		}
		catch (Exception e)
		{
			return showError(player, e);
		}
		return showResult(player, res);
	}
	
	/**
	 * @param instance
	 * @param player
	 * @param skill
	 * @return
	 */
	public final boolean notifySpellFinished(L2Npc instance, L2PcInstance player, L2Skill skill)
	{
		String res = null;
		try
		{
			res = onSpellFinished(instance, player, skill);
		}
		catch (Exception e)
		{
			return showError(player, e);
		}
		return showResult(player, res);
	}
	
	/**
	 * Notify quest script when something happens with a trap.
	 * @param trap the trap instance which triggers the notification.
	 * @param trigger the character which makes effect on the trap.
	 * @param action 0: trap casting its skill. 1: trigger detects the trap. 2: trigger removes the trap.
	 * @return {@code false} if the event was triggered successfully, {@code true} otherwise.
	 */
	public final boolean notifyTrapAction(L2Trap trap, L2Character trigger, TrapAction action)
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
				return showError(trigger.getActingPlayer(), e);
			}
			_log.log(Level.WARNING, "Exception on onTrapAction() in notifyTrapAction(): " + e.getMessage(), e);
			return true;
		}
		if (trigger.getActingPlayer() != null)
		{
			return showResult(trigger.getActingPlayer(), res);
		}
		return false;
	}
	
	/**
	 * @param npc
	 * @return
	 */
	public final boolean notifySpawn(L2Npc npc)
	{
		try
		{
			onSpawn(npc);
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Exception on onSpawn() in notifySpawn(): " + e.getMessage(), e);
			return true;
		}
		return false;
	}
	
	/**
	 * @param event
	 * @param npc
	 * @param player
	 * @return
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
	 * @param player
	 * @return
	 */
	public final boolean notifyEnterWorld(L2PcInstance player)
	{
		String res = null;
		try
		{
			res = onEnterWorld(player);
		}
		catch (Exception e)
		{
			return showError(player, e);
		}
		return showResult(player, res);
	}
	
	/**
	 * @param npc
	 * @param killer
	 * @param isPet
	 * @return
	 */
	public final boolean notifyKill(L2Npc npc, L2PcInstance killer, boolean isPet)
	{
		String res = null;
		try
		{
			res = onKill(npc, killer, isPet);
		}
		catch (Exception e)
		{
			return showError(killer, e);
		}
		return showResult(killer, res);
	}
	
	/**
	 * @param npc
	 * @param qs
	 * @return
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
	 * Override the default NPC dialogs when a quest defines this for the given NPC.
	 * @param npc the NPC whose dialogs to override.
	 * @param player the player talking to the NPC.
	 * @return {@code true} if the event was triggered successfully, {@code false} otherwise.
	 */
	public final boolean notifyFirstTalk(L2Npc npc, L2PcInstance player)
	{
		String res = null;
		try
		{
			res = onFirstTalk(npc, player);
		}
		catch (Exception e)
		{
			return showError(player, e);
		}
		// if the quest returns text to display, display it.
		if ((res != null) && (res.length() > 0))
		{
			return showResult(player, res);
		}
		// else tell the player that
		player.sendPacket(ActionFailed.STATIC_PACKET);
		// note: if the default html for this npc needs to be shown, onFirstTalk should
		// call npc.showChatWindow(player) and then return null.
		return true;
	}
	
	/**
	 * @param npc
	 * @param player
	 * @return
	 */
	public final boolean notifyAcquireSkillList(L2Npc npc, L2PcInstance player)
	{
		String res = null;
		try
		{
			res = onAcquireSkillList(npc, player);
		}
		catch (Exception e)
		{
			return showError(player, e);
		}
		return showResult(player, res);
	}
	
	/**
	 * @param npc
	 * @param player
	 * @param skill
	 * @return
	 */
	public final boolean notifyAcquireSkillInfo(L2Npc npc, L2PcInstance player, L2Skill skill)
	{
		String res = null;
		try
		{
			res = onAcquireSkillInfo(npc, player, skill);
		}
		catch (Exception e)
		{
			return showError(player, e);
		}
		return showResult(player, res);
	}
	
	/**
	 * @param npc
	 * @param player
	 * @param skill
	 * @return
	 */
	public final boolean notifyAcquireSkill(L2Npc npc, L2PcInstance player, L2Skill skill)
	{
		String res = null;
		try
		{
			res = onAcquireSkill(npc, player, skill);
			if (res == "true")
			{
				return true;
			}
			else if (res == "false")
			{
				return false;
			}
		}
		catch (Exception e)
		{
			return showError(player, e);
		}
		return showResult(player, res);
	}
	
	public class TmpOnSkillSee implements Runnable
	{
		private final L2Npc _npc;
		private final L2PcInstance _caster;
		private final L2Skill _skill;
		private final L2Object[] _targets;
		private final boolean _isPet;
		
		public TmpOnSkillSee(L2Npc npc, L2PcInstance caster, L2Skill skill, L2Object[] targets, boolean isPet)
		{
			_npc = npc;
			_caster = caster;
			_skill = skill;
			_targets = targets;
			_isPet = isPet;
		}
		
		@Override
		public void run()
		{
			String res = null;
			try
			{
				res = onSkillSee(_npc, _caster, _skill, _targets, _isPet);
			}
			catch (Exception e)
			{
				showError(_caster, e);
			}
			showResult(_caster, res);
			
		}
	}
	
	/**
	 * @param npc
	 * @param caster
	 * @param skill
	 * @param targets
	 * @param isPet
	 * @return
	 */
	public final boolean notifySkillSee(L2Npc npc, L2PcInstance caster, L2Skill skill, L2Object[] targets, boolean isPet)
	{
		ThreadPoolManager.getInstance().executeAi(new TmpOnSkillSee(npc, caster, skill, targets, isPet));
		return true;
	}
	
	/**
	 * @param npc
	 * @param caller
	 * @param attacker
	 * @param isPet
	 * @return
	 */
	public final boolean notifyFactionCall(L2Npc npc, L2Npc caller, L2PcInstance attacker, boolean isPet)
	{
		String res = null;
		try
		{
			res = onFactionCall(npc, caller, attacker, isPet);
		}
		catch (Exception e)
		{
			return showError(attacker, e);
		}
		return showResult(attacker, res);
	}
	
	public class TmpOnAggroEnter implements Runnable
	{
		private final L2Npc _npc;
		private final L2PcInstance _pc;
		private final boolean _isPet;
		
		public TmpOnAggroEnter(L2Npc npc, L2PcInstance pc, boolean isPet)
		{
			_npc = npc;
			_pc = pc;
			_isPet = isPet;
		}
		
		@Override
		public void run()
		{
			String res = null;
			try
			{
				res = onAggroRangeEnter(_npc, _pc, _isPet);
			}
			catch (Exception e)
			{
				showError(_pc, e);
			}
			showResult(_pc, res);
			
		}
	}
	
	/**
	 * @param npc
	 * @param player
	 * @param isPet
	 * @return
	 */
	public final boolean notifyAggroRangeEnter(L2Npc npc, L2PcInstance player, boolean isPet)
	{
		ThreadPoolManager.getInstance().executeAi(new TmpOnAggroEnter(npc, player, isPet));
		return true;
	}
	
	/**
	 * @param character
	 * @param zone
	 * @return
	 */
	public final boolean notifyEnterZone(L2Character character, L2ZoneType zone)
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
				return showError(player, e);
			}
		}
		if (player != null)
		{
			return showResult(player, res);
		}
		return true;
	}
	
	/**
	 * @param character
	 * @param zone
	 * @return
	 */
	public final boolean notifyExitZone(L2Character character, L2ZoneType zone)
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
				return showError(player, e);
			}
		}
		if (player != null)
		{
			return showResult(player, res);
		}
		return true;
	}
	
	/**
	 * @param winner
	 * @param type
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
	 * @param type
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
	
	// These are methods that java calls to invoke scripts.
	
	/**
	 * This function is called in place of {@link #onAttack(L2Npc, L2PcInstance, int, boolean, L2Skill)} if the former is not implemented.<br>
	 * If a script contains both onAttack(..) implementations, then this method will never be called unless the script's {@link #onAttack(L2Npc, L2PcInstance, int, boolean, L2Skill)} explicitly calls this method.
	 * @param npc this parameter contains a reference to the exact instance of the NPC that got attacked the NPC.
	 * @param attacker this parameter contains a reference to the exact instance of the player who attacked.
	 * @param damage this parameter represents the total damage that this attack has inflicted to the NPC.
	 * @param isPet this parameter if it's {@code false} it denotes that the attacker was indeed the player, else it specifies that the damage was actually dealt by the player's pet.
	 * @return
	 */
	public String onAttack(L2Npc npc, L2PcInstance attacker, int damage, boolean isPet)
	{
		return null;
	}
	
	/**
	 * This function is called whenever a player attacks an NPC that is registered for the quest.<br>
	 * If is not overridden by a subclass, then default to the returned value of the simpler (and older) {@link #onAttack(L2Npc, L2PcInstance, int, boolean)} override.<br>
	 * @param npc this parameter contains a reference to the exact instance of the NPC that got attacked.
	 * @param attacker this parameter contains a reference to the exact instance of the player who attacked the NPC.
	 * @param damage this parameter represents the total damage that this attack has inflicted to the NPC.
	 * @param isPet this parameter if it's {@code false} it denotes that the attacker was indeed the player, else it specifies that the damage was actually dealt by the player's pet.
	 * @param skill parameter is the skill that player used to attack NPC.
	 * @return
	 */
	public String onAttack(L2Npc npc, L2PcInstance attacker, int damage, boolean isPet, L2Skill skill)
	{
		return onAttack(npc, attacker, damage, isPet);
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
		if (killer instanceof L2Npc)
		{
			return onAdvEvent("", (L2Npc) killer, qs.getPlayer());
		}
		return onAdvEvent("", null, qs.getPlayer());
	}
	
	/**
	 * This function is called whenever a player clicks on a link in a quest dialog and whenever a timer fires.<br>
	 * If is not overridden by a subclass, then default to the returned value of the simpler (and older) {@link #onEvent(String, QuestState)} override.<br>
	 * If the player has a quest state, use it as parameter in the next call, otherwise return null.
	 * @param event this parameter contains a string identifier for the event.<br>
	 *            Generally, this string is passed directly via the link.<br>
	 *            For example:<br>
	 *            <code>
	 *               <a action="bypass -h Quest 626_ADarkTwilight 31517-01.htm">hello</a>
	 *            </code><br>
	 *            The above link sets the event variable to "31517-01.htm" for the quest 626_ADarkTwilight.<br>
	 *            In the case of timers, this will be the name of the timer.<br>
	 *            This parameter serves as a sort of identifier.
	 * @param npc this parameter contains a reference to the instance of NPC associated with this event.<br>
	 *            This may be the NPC registered in a timer, or the NPC with whom a player is speaking, etc.<br>
	 *            This parameter may be <b>null</b> in certain circumstances.
	 * @param player this parameter contains a reference to the player participating in this function.<br>
	 *            It may be the player speaking to the NPC, or the player who caused a timer to start (and owns that timer).<br>
	 *            This parameter may be <b>null</b> in certain circumstances.
	 * @return the text returned by the event (may be {@code null}, a filename or just text).
	 */
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		final QuestState qs = player.getQuestState(getName());
		if (qs != null)
		{
			return onEvent(event, qs);
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
	 *               <a action="bypass -h Quest 626_ADarkTwilight 31517-01.htm">hello</a>
	 *            </code><br>
	 *            The above link sets the event variable to "31517-01.htm" for the quest 626_ADarkTwilight.<br>
	 *            In the case of timers, this will be the name of the timer.<br>
	 *            This parameter serves as a sort of identifier.
	 * @param qs this parameter contains a reference to the quest state of the player who used the link or started the timer.
	 * @return the text returned by the event (may be {@code null}, a filename or just text).
	 */
	public String onEvent(String event, QuestState qs)
	{
		return null;
	}
	
	/**
	 * This function is called whenever a player kills a NPC that is registered for the quest.
	 * @param npc this parameter contains a reference to the exact instance of the NPC that got killed.
	 * @param killer this parameter contains a reference to the exact instance of the player who killed the NPC.
	 * @param isPet this parameter if it's {@code false} it denotes that the attacker was indeed the player, else it specifies that the killer was the player's pet.
	 * @return the text returned by the event (may be {@code null}, a filename or just text).
	 */
	public String onKill(L2Npc npc, L2PcInstance killer, boolean isPet)
	{
		return null;
	}
	
	/**
	 * This function is called whenever a player clicks to the "Quest" link of an NPC that is registered for the quest.
	 * @param npc this parameter contains a reference to the exact instance of the NPC that the player is talking with.
	 * @param talker this parameter contains a reference to the exact instance of the player who is talking to the NPC.
	 * @return the text returned by the event (may be {@code null}, a filename or just text).
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
	 * @return the text returned by the event (may be {@code null}, a filename or just text).
	 * @since <a href="http://trac.l2jserver.com/changeset/771">Jython AI support for "onFirstTalk"</a>
	 */
	public String onFirstTalk(L2Npc npc, L2PcInstance player)
	{
		return null;
	}
	
	/**
	 * This function is called whenever a player request a skill list.<br>
	 * TODO: Cleanup or re-implement, since Skill Trees rework it's support was removed.
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
	 * TODO: Cleanup or re-implement, since Skill Trees rework it's support was removed.
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
	 * TODO: Cleanup or re-implement, since Skill Trees rework it's support was removed.
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
	 * This function is called whenever a player uses a quest item that has a quest events list<br>
	 * TODO: complete this documentation and unhardcode it to work with all item uses not with those listed.
	 * @param item this parameter contains a reference to the instance of the quest item that the player used.
	 * @param player this parameter contains a reference to the exact instance of the player who used the quest item.
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
	 * @param npc this parameter contains a reference to the exact instance of the NPC that saw the skill.
	 * @param caster this parameter references the actual instance of the player who cast the skill.
	 * @param skill this parameter is a reference to the actual skill that was used (from which info about the id and level of the skill can be obtained).
	 * @param targets this parameter is an array of all objects (can be any type of object, including mobs and players) that are affected by the skill.
	 * @param isPet this parameter if it's {@code false} it denotes that the caster was indeed the player, else it specifies that the caster was the player's pet.
	 * @return
	 */
	public String onSkillSee(L2Npc npc, L2PcInstance caster, L2Skill skill, L2Object[] targets, boolean isPet)
	{
		return null;
	}
	
	/**
	 * This function is called whenever an NPC finishes casting a skill.
	 * @param npc this parameter contains a reference to the exact instance of the NPC that casted the skill.
	 * @param player this parameter references the actual instance of the player who is target of the skill. This parameter may be <b>null</b> in certain circumstances.
	 * @param skill this parameter is a reference to the actual skill that was used by the NPC.
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
	 * @param isPet this parameter if it's {@code false} it denotes that the attacker was indeed the player, else it specifies that the attacker was the player's pet.
	 * @return
	 */
	public String onFactionCall(L2Npc npc, L2Npc caller, L2PcInstance attacker, boolean isPet)
	{
		return null;
	}
	
	/**
	 * This function is called whenever a player enters an NPC aggression range.
	 * @param npc this parameter contains a reference to the exact instance of the NPC whose aggression range is being transgressed.
	 * @param player this parameter contains a reference to the exact instance of the player who is entering the NPC's aggression range.
	 * @param isPet this parameter if it's {@code false} it denotes that the character that entered the aggression range was indeed the player, else it specifies that the character was the player's pet.
	 * @return
	 */
	public String onAggroRangeEnter(L2Npc npc, L2PcInstance player, boolean isPet)
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
	 * Show message error to player who has an access level greater than 0.
	 * @param player the player to whom to send the error (must be a GM).
	 * @param t the throwable to get the message/stacktrace from.
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
	 * Three cases are managed according to the value of the parameter "res":<br>
	 * <ul>
	 * <li>"res" ends with string ".html": an HTML is opened in order to be shown in a dialog box.</li>
	 * <li>"res" starts with "<html>": the message hold in "res" is shown in a dialog box.</li>
	 * <li>otherwise: the message held in "res" is shown in chat box.</li>
	 * </ul>
	 * @param player the player to whom to show the result.
	 * @param res the message to show to the player.
	 * @return {@code false} if the message was sent, {@code true} otherwise.
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
	 * @param player the player who is entering the world.
	 */
	public final static void playerEnter(L2PcInstance player)
	{
		Connection con = null;
		try
		{
			// Get list of quests owned by the player from database
			con = L2DatabaseFactory.getInstance().getConnection();
			
			PreparedStatement invalidQuestData = con.prepareStatement("DELETE FROM character_quests WHERE charId=? and name=?");
			PreparedStatement invalidQuestDataVar = con.prepareStatement("delete FROM character_quests WHERE charId=? and name=? and var=?");
			
			PreparedStatement statement = con.prepareStatement("SELECT name,value FROM character_quests WHERE charId=? AND var=?");
			statement.setInt(1, player.getObjectId());
			statement.setString(2, "<state>");
			ResultSet rs = statement.executeQuery();
			while (rs.next())
			{
				// Get ID of the quest and ID of its state
				String questId = rs.getString("name");
				String statename = rs.getString("value");
				
				// Search quest associated with the ID
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
			rs.close();
			invalidQuestData.close();
			statement.close();
			
			// Get list of quests owned by the player from the DB in order to add variables used in the quest.
			statement = con.prepareStatement("SELECT name,var,value FROM character_quests WHERE charId=? AND var<>?");
			statement.setInt(1, player.getObjectId());
			statement.setString(2, "<state>");
			rs = statement.executeQuery();
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
			rs.close();
			invalidQuestDataVar.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "could not insert char quest:", e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
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
	 * @param var the name of the variable to save.
	 * @param value the value of the variable.
	 */
	public final void saveGlobalQuestVar(String var, String value)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("REPLACE INTO quest_global_data (quest_name,var,value) VALUES (?,?,?)");
			statement.setString(1, getName());
			statement.setString(2, var);
			statement.setString(3, value);
			statement.executeUpdate();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "could not insert global quest variable:", e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
	}
	
	/**
	 * Read from the database a previously saved variable for this quest.<br>
	 * Due to performance considerations, this function should best be used only when the quest is first loaded.<br>
	 * Subclasses of this class can define structures into which these loaded values can be saved.<br>
	 * However, on-demand usage of this function throughout the script is not prohibited, only not recommended.<br>
	 * Values read from this function were entered by calls to "saveGlobalQuestVar".
	 * @param var the name of the variable to load.
	 * @return the current value of the specified variable, or an empty string if the variable does not exist.
	 */
	public final String loadGlobalQuestVar(String var)
	{
		String result = "";
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			statement = con.prepareStatement("SELECT value FROM quest_global_data WHERE quest_name = ? AND var = ?");
			statement.setString(1, getName());
			statement.setString(2, var);
			ResultSet rs = statement.executeQuery();
			if (rs.first())
			{
				result = rs.getString(1);
			}
			rs.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "could not load global quest variable:", e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
		return result;
	}
	
	/**
	 * Permanently delete from the database a global quest variable that was previously saved for this quest.
	 * @param var the name of the variable to delete.
	 */
	public final void deleteGlobalQuestVar(String var)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM quest_global_data WHERE quest_name = ? AND var = ?");
			statement.setString(1, getName());
			statement.setString(2, var);
			statement.executeUpdate();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "could not delete global quest variable:", e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
	}
	
	/**
	 * Permanently delete from the database all global quest variables that were previously saved for this quest.
	 */
	public final void deleteAllGlobalQuestVars()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			statement = con.prepareStatement("DELETE FROM quest_global_data WHERE quest_name = ?");
			statement.setString(1, getName());
			statement.executeUpdate();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "could not delete global quest variables:", e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
	}
	
	/**
	 * Insert in the database the quest for the player.
	 * @param qs the quest state whose variable to insert.
	 * @param var the name of the variable.
	 * @param value the value of the variable.
	 */
	public static void createQuestVarInDb(QuestState qs, String var, String value)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("INSERT INTO character_quests (charId,name,var,value) VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE value=?");
			statement.setInt(1, qs.getPlayer().getObjectId());
			statement.setString(2, qs.getQuestName());
			statement.setString(3, var);
			statement.setString(4, value);
			statement.setString(5, value);
			statement.executeUpdate();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "could not insert char quest:", e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
	}
	
	/**
	 * Update the value of the variable "var" for the specified quest in database.
	 * @param qs the quest state of the quest whose variable to update.
	 * @param var the name of the variable.
	 * @param value the value of the variable.
	 */
	public static void updateQuestVarInDb(QuestState qs, String var, String value)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE character_quests SET value=? WHERE charId=? AND name=? AND var = ?");
			statement.setString(1, value);
			statement.setInt(2, qs.getPlayer().getObjectId());
			statement.setString(3, qs.getQuestName());
			statement.setString(4, var);
			statement.executeUpdate();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "could not update char quest:", e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
	}
	
	/**
	 * Delete from the database all variables and states of the specified quest state.
	 * @param qs : object QuestState pointing out the player's quest
	 * @param var : String designating the variable characterizing the quest
	 */
	public static void deleteQuestVarInDb(QuestState qs, String var)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			statement = con.prepareStatement("DELETE FROM character_quests WHERE charId=? AND name=? AND var=?");
			statement.setInt(1, qs.getPlayer().getObjectId());
			statement.setString(2, qs.getQuestName());
			statement.setString(3, var);
			statement.executeUpdate();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "could not delete char quest:", e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
	}
	
	/**
	 * Delete the player's quest from database.
	 * @param qs : QuestState pointing out the player's quest
	 */
	public static void deleteQuestInDb(QuestState qs)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM character_quests WHERE charId=? AND name=?");
			statement.setInt(1, qs.getPlayer().getObjectId());
			statement.setString(2, qs.getQuestName());
			statement.executeUpdate();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "could not delete char quest:", e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
	}
	
	/**
	 * Create a record in database for quest.<br>
	 * Actions:<br>
	 * Use function createQuestVarInDb() with following parameters:<br>
	 * <ul>
	 * <li>QuestState : parameter qs that puts in fields of database:
	 * <ul type="square">
	 * <li>charId : ID of the player</li>
	 * <li>name : name of the quest</li>
	 * </ul>
	 * </li>
	 * <li>var : string "&lt;state&gt;" as the name of the variable for the quest</li>
	 * <li>val : string corresponding at the ID of the state (in fact, initial state)</li>
	 * </ul>
	 * @param qs : QuestState
	 */
	public static void createQuestInDb(QuestState qs)
	{
		createQuestVarInDb(qs, "<state>", State.getStateName(qs.getState()));
	}
	
	/**
	 * Update informations regarding quest in database.<br>
	 * Actions:<br>
	 * <ul>
	 * <li>Get ID state of the quest recorded in object qs</li>
	 * <li>Test if quest is completed. If true, add a star (*) before the ID state</li>
	 * <li>Save in database the ID state (with or without the star) for the variable called "&lt;state&gt;" of the quest</li>
	 * </ul>
	 * @param qs : QuestState
	 */
	public static void updateQuestInDb(QuestState qs)
	{
		String val = State.getStateName(qs.getState());
		updateQuestVarInDb(qs, "<state>", val);
	}
	
	/**
	 * @param player the player whose language settings to use in finding the html of the right language.
	 * @return the default html for when no quest is available: "You are either not on a quest that involves this NPC..".
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
	 * @param player the player whose language settings to use in finding the html of the right language.
	 * @return the default html for when no quest is already completed: "This quest has already been completed.".
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
	 * @param npcId : id of the NPC to register
	 * @param eventType : type of event being registered
	 * @return L2NpcTemplate : Npc Template corresponding to the npcId, or null if the id is invalid
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
	 * @return L2NpcTemplate : Start NPC
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
	 * @return L2NpcTemplate : Start NPC
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
	 * @return L2NpcTemplate : NPC
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
	 * @return int : attackId
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
	 * @return int : killId
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
	 * @param npcId
	 * @return
	 */
	public L2NpcTemplate addKillId(int npcId)
	{
		return addEventId(npcId, QuestEventType.ON_KILL);
	}
	
	/**
	 * Add this quest to the list of quests that the passed npc will respond to for Talk Events.
	 * @param talkIds : ID of the NPC
	 * @return int : ID of the NPC
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
	 * @param npcIds : ID of the NPC
	 * @return int : ID of the NPC
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
	 * @param npcIds : ID of the NPC
	 * @return int : ID of the NPC
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
	 * @param npcIds : ID of the NPC
	 * @return int : ID of the NPC
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
	 * @param npcIds : ID of the NPC
	 * @return int : ID of the NPC
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
	 * Use this method to get a random party member from a player's party.<br>
	 * Useful when distributing rewards after killing an NPC.
	 * @param player this parameter represents the player whom the party will taken.
	 * @return if {@code player} is null returns null, if the {@code player} does not have a party returns {@code player}, other wise select a random party member.
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
	 * Auxiliary function for party quests.<br>
	 * Note: This function is only here because of how commonly it may be used by quest developers.<br>
	 * For any variations on this function, the quest script can always handle things on its own.
	 * @param player the instance of a player whose party is to be searched
	 * @param value the value of the "cond" variable that must be matched
	 * @return L2PcInstance: L2PcInstance for a random party member that matches the specified condition, or null if no match.
	 */
	public L2PcInstance getRandomPartyMember(L2PcInstance player, String value)
	{
		return getRandomPartyMember(player, "cond", value);
	}
	
	/**
	 * Auxiliary function for party quests.<br>
	 * Note: This function is only here because of how commonly it may be used by quest developers.<br>
	 * For any variations on this function, the quest script can always handle things on its own.
	 * @param player the instance of a player whose party is to be searched
	 * @param var
	 * @param value a tuple specifying a quest condition that must be satisfied for a party member to be considered.
	 * @return L2PcInstance: L2PcInstance for a random party member that matches the specified condition, or null if no match. If the var is null, any random party member is returned (i.e. no condition is applied). The party member must be within 1500 distance from the target of the reference
	 *         player, or if no target exists, 1500 distance from the player itself.
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
		FastList<L2PcInstance> candidates = new FastList<L2PcInstance>();
		
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
		
		// if a match was found from the party, return one of them at random.
		return candidates.get(Rnd.get(candidates.size()));
	}
	
	/**
	 * Auxiliary function for party quests.<br>
	 * Note: This function is only here because of how commonly it may be used by quest developers.<br>
	 * For any variations on this function, the quest script can always handle things on its own.
	 * @param player the instance of a player whose party is to be searched
	 * @param state the state in which the party member's queststate must be in order to be considered.
	 * @return L2PcInstance: L2PcInstance for a random party member that matches the specified condition, or null if no match. If the var is null, any random party member is returned (i.e. no condition is applied).
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
		FastList<L2PcInstance> candidates = new FastList<L2PcInstance>();
		
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
	 * Show HTML file to client
	 * @param player
	 * @param fileName
	 * @return String : message sent to client
	 */
	public String showHtmlFile(L2PcInstance player, String fileName)
	{
		boolean questwindow = true;
		if (fileName.endsWith(".html"))
		{
			questwindow = false;
		}
		int questId = getQuestIntId();
		// Create handler to file linked to the quest
		String content = getHtm(player.getHtmlPrefix(), fileName);
		
		if (player.getTarget() != null)
		{
			content = content.replaceAll("%objectId%", String.valueOf(player.getTarget().getObjectId()));
		}
		
		// Send message to client if message not empty
		if (content != null)
		{
			if (questwindow && (questId > 0) && (questId < 20000) && (questId != 999))
			{
				NpcQuestHtmlMessage npcReply = new NpcQuestHtmlMessage(5, questId);
				npcReply.setHtml(content);
				npcReply.replace("%playername%", player.getName());
				player.sendPacket(npcReply);
			}
			else
			{
				NpcHtmlMessage npcReply = new NpcHtmlMessage(5);
				npcReply.setHtml(content);
				npcReply.replace("%playername%", player.getName());
				player.sendPacket(npcReply);
			}
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
		
		return content;
	}
	
	/**
	 * Return HTML file contents
	 * @param prefix player's language prefix.
	 * @param fileName the html file to be get.
	 * @return
	 */
	public String getHtm(String prefix, String fileName)
	{
		String content = HtmCache.getInstance().getHtm(prefix, "data/scripts/" + getDescr().toLowerCase() + "/" + getName() + "/" + fileName);
		
		if (content == null)
		{
			content = HtmCache.getInstance().getHtm(prefix, "data/scripts/quests/Q" + getName() + "/" + fileName);
			if (content == null)
			{
				content = HtmCache.getInstance().getHtmForce(prefix, "data/scripts/quests/" + getName() + "/" + fileName);
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
		return addSpawn(npcId, cha.getX(), cha.getY(), cha.getZ(), cha.getHeading(), false, 0, false);
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
		return addSpawn(npcId, cha.getX(), cha.getY(), cha.getZ(), cha.getHeading(), false, 0, isSummonSpawn);
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
		return addSpawn(npcId, x, y, z, heading, randomOffSet, despawnDelay, false);
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
		return addSpawn(npcId, loc.getX(), loc.getY(), loc.getZ(), loc.getHeading(), randomOffSet, despawnDelay, false);
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
						offset = -1;
					} // make offset negative
					offset *= Rnd.get(50, 100);
					x += offset;
					
					offset = Rnd.get(2); // Get the direction of the offset
					if (offset == 0)
					{
						offset = -1;
					} // make offset negative
					offset *= Rnd.get(50, 100);
					y += offset;
				}
				L2Spawn spawn = new L2Spawn(template);
				spawn.setInstanceId(instanceId);
				spawn.setHeading(heading);
				spawn.setLocx(x);
				spawn.setLocy(y);
				spawn.setLocz(z + 20);
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
			_log.warning("Could not spawn Npc " + npcId);
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
		// with its code (example: save global data indicating what timer must
		// be restarted).
		for (FastList<QuestTimer> timers : _allEventTimers.values())
		{
			for (QuestTimer timer : timers)
			{
				timer.cancel();
			}
		}
		_allEventTimers.clear();
		if (removeFromList)
		{
			return QuestManager.getInstance().removeQuest(this);
		}
		return true;
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
	 * @param player this parameter contains a reference to the player to check.
	 * @param itemId the item wanted to be count.
	 * @return the quantity of one sort of item hold by the player.
	 */
	public long getQuestItemsCount(L2PcInstance player, int itemId)
	{
		long count = 0;
		for (L2ItemInstance item : player.getInventory().getItems())
		{
			if ((item != null) && (item.getItemId() == itemId))
			{
				count += item.getCount();
			}
		}
		return count;
	}
	
	/**
	 * @param player this parameter contains a reference to the player to check.
	 * @param itemId the item Id of the item to verify.
	 * @return {code true} if the item exists in player's inventory, otherwise {@code false}.
	 */
	public boolean hasQuestItems(L2PcInstance player, int itemId)
	{
		return player.getInventory().getItemByItemId(itemId) != null;
	}
	
	/**
	 * @param player this parameter contains a reference to the player to check.
	 * @param itemIds the item Ids of the items to verify.
	 * @return {code true} if all the items exists in player's inventory, otherwise {@code false}.
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
	 * @param player this parameter contains a reference to the player to check.
	 * @param itemId : ID of the item to check enchantment
	 * @return the level of enchantment on the weapon of the player(Done specifically for weapon SA's)
	 */
	public int getEnchantLevel(L2PcInstance player, int itemId)
	{
		L2ItemInstance enchanteditem = player.getInventory().getItemByItemId(itemId);
		
		if (enchanteditem == null)
		{
			return 0;
		}
		
		return enchanteditem.getEnchantLevel();
	}
	
	/**
	 * Give Adena to the player
	 * @param player this parameter contains a reference to the player that receives the Adena.
	 * @param count this parameter represent the Adena count to give to the player.
	 * @param applyRates if {@code true} quest rates will be applied.
	 */
	public void giveAdena(L2PcInstance player, long count, boolean applyRates)
	{
		giveItems(player, PcInventory.ADENA_ID, count, applyRates ? 0 : 1);
	}
	
	/**
	 * Give reward to player using multipliers.
	 * @param player
	 * @param itemId
	 * @param count
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
		
		if (itemId == PcInventory.ADENA_ID)
		{
			count = (long) (count * Config.RATE_QUEST_REWARD_ADENA);
		}
		else if (Config.RATE_QUEST_REWARD_USE_MULTIPLIERS)
		{
			if (_tmpItem.isEtcItem())
			{
				switch (_tmpItem.getEtcItem().getItemType())
				{
					case POTION:
						count = (long) (count * Config.RATE_QUEST_REWARD_POTION);
						break;
					case SCRL_ENCHANT_WP:
					case SCRL_ENCHANT_AM:
					case SCROLL:
						count = (long) (count * Config.RATE_QUEST_REWARD_SCROLL);
						break;
					case RECIPE:
						count = (long) (count * Config.RATE_QUEST_REWARD_RECIPE);
						break;
					case MATERIAL:
						count = (long) (count * Config.RATE_QUEST_REWARD_MATERIAL);
						break;
					default:
						count = (long) (count * Config.RATE_QUEST_REWARD);
				}
			}
		}
		else
		{
			count = (long) (count * Config.RATE_QUEST_REWARD);
		}
		
		// Add items to player's inventory
		L2ItemInstance item = player.getInventory().addItem("Quest", itemId, count, player, player.getTarget());
		
		if (item == null)
		{
			return;
		}
		
		// If item for reward is gold, send message of gold reward to client
		if (itemId == PcInventory.ADENA_ID)
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
		
		// If item for reward is adena (ID=57), modify count with rate for quest reward if rates available
		if ((itemId == PcInventory.ADENA_ID) && !(enchantlevel > 0))
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
		
		// If item for reward is gold, send message of gold reward to client
		if (itemId == PcInventory.ADENA_ID)
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
		
		// If item for reward is gold, send message of gold reward to client
		if (itemId == PcInventory.ADENA_ID)
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
				playSound(player, ((currentCount + itemCount) < neededCount) ? "Itemsound.quest_itemget" : "Itemsound.quest_middle");
			}
		}
		
		return ((neededCount > 0) && ((currentCount + itemCount) >= neededCount));
	}
	
	/**
	 * Remove items from player's inventory when talking to NPC in order to have rewards.<br>
	 * Actions:<br>
	 * <ul>
	 * <li>Destroy quantity of items wanted</li>
	 * <li>Send new inventory list to player</li>
	 * </ul>
	 * @param player
	 * @param itemId : Identifier of the item
	 * @param count : Quantity of items to destroy
	 */
	public void takeItems(L2PcInstance player, int itemId, long count)
	{
		// Get object item from player's inventory list
		L2ItemInstance item = player.getInventory().getItemByItemId(itemId);
		if (item == null)
		{
			return;
		}
		
		// Tests on count value in order not to have negative value
		if ((count < 0) || (count > item.getCount()))
		{
			count = item.getCount();
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
		player.destroyItemByItemId("Quest", itemId, count, player, true);
	}
	
	/**
	 * Send a packet in order to play sound at client terminal
	 * @param player
	 * @param sound
	 */
	public void playSound(L2PcInstance player, String sound)
	{
		player.sendPacket(new PlaySound(sound));
	}
	
	/**
	 * Add XP and SP as quest reward
	 * @param player
	 * @param exp
	 * @param sp
	 */
	public void addExpAndSp(L2PcInstance player, int exp, int sp)
	{
		player.addExpAndSp((int) player.calcStat(Stats.EXPSP_RATE, exp * Config.RATE_QUEST_REWARD_XP, null, null), (int) player.calcStat(Stats.EXPSP_RATE, sp * Config.RATE_QUEST_REWARD_SP, null, null));
	}
	
	/**
	 * Gets a random integer number from 0 (inclusive) to {@code max} (exclusive).<br>
	 * Use this method instead importing {@link com.l2jserver.util.Rnd} utility.
	 * @param max this parameter represents the maximum value for randomization.
	 * @return a random integer number from 0 to {@code max} - 1.
	 */
	public static int getRandom(int max)
	{
		return Rnd.get(max);
	}
	
	/**
	 * Gets a random integer number from {@code min} (inclusive) to {@code max} (inclusive).<br>
	 * Use this method instead importing {@link com.l2jserver.util.Rnd} utility.
	 * @param min this parameter represents the minimum value for randomization.
	 * @param max this parameter represents the maximum value for randomization.
	 * @return a random integer number from {@code min} to {@code max} .
	 */
	public static int getRandom(int min, int max)
	{
		return Rnd.get(min, max);
	}
	
	/**
	 * @param player this parameter is a reference to the player.
	 * @param slot this parameter represents the location in the player's inventory.
	 * @return the item Id of the item present in the inventory slot {@code slot} if it's not null, 0 otherwise.
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
}
