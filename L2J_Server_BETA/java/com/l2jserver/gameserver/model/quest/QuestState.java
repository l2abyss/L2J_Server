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
import java.util.Calendar;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastMap;

import com.l2jserver.L2DatabaseFactory;
import com.l2jserver.gameserver.cache.HtmCache;
import com.l2jserver.gameserver.instancemanager.QuestManager;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.model.actor.instance.L2MonsterInstance;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.itemcontainer.PcInventory;
import com.l2jserver.gameserver.network.serverpackets.ExShowQuestMark;
import com.l2jserver.gameserver.network.serverpackets.PlaySound;
import com.l2jserver.gameserver.network.serverpackets.QuestList;
import com.l2jserver.gameserver.network.serverpackets.TutorialCloseHtml;
import com.l2jserver.gameserver.network.serverpackets.TutorialEnableClientEvent;
import com.l2jserver.gameserver.network.serverpackets.TutorialShowHtml;
import com.l2jserver.gameserver.network.serverpackets.TutorialShowQuestionMark;
import com.l2jserver.gameserver.util.Util;

/**
 * @author Luis Arias
 */
public final class QuestState
{
	protected static final Logger _log = Logger.getLogger(QuestState.class.getName());
	
	/**
	 * Quest associated to the QuestState
	 */
	private final String _questName;
	
	/**
	 * Player who engaged the quest
	 */
	private final L2PcInstance _player;
	
	/**
	 * State of the quest
	 */
	private byte _state;
	
	/**
	 * List of couples (variable for quest,value of the variable for quest)
	 */
	private Map<String, String> _vars;
	
	/**
	 * boolean flag letting QuestStateManager know to exit quest when cleaning up
	 */
	private boolean _isExitQuestOnCleanUp = false;
	
	/**
	 * This enumerate represent the different quest types.
	 */
	public static enum QuestType
	{
		REPEATABLE,
		ONE_TIME,
		DAILY
	}
	
	/**
	 * Constructor of the QuestState : save the quest in the list of quests of the player.<br>
	 * Actions:<br>
	 * <ul>
	 * <li>Save informations in the object QuestState created (Quest, Player, Completion, State)</li>
	 * <li>Add the QuestState in the player's list of quests by using setQuestState()</li>
	 * <li>Add drops gotten by the quest</li>
	 * </ul>
	 * @param quest the quest associated with this QuestState.
	 * @param player the player reference to the owner of the state.
	 * @param state the state of the quest.
	 */
	public QuestState(Quest quest, L2PcInstance player, byte state)
	{
		_questName = quest.getName();
		_player = player;
		
		// Save the state of the quest for the player in the player's list of quest owned.
		getPlayer().setQuestState(this);
		
		// Set the state of the quest.
		_state = state;
	}
	
	public String getQuestName()
	{
		return _questName;
	}
	
	/**
	 * Return the quest
	 * @return Quest
	 */
	public Quest getQuest()
	{
		return QuestManager.getInstance().getQuest(_questName);
	}
	
	/**
	 * Return the L2PcInstance
	 * @return L2PcInstance
	 */
	public L2PcInstance getPlayer()
	{
		return _player;
	}
	
	/**
	 * Return the state of the quest
	 * @return State
	 */
	public byte getState()
	{
		return _state;
	}
	
	/**
	 * Return true if quest just created, false otherwise
	 * @return
	 */
	public boolean isCreated()
	{
		return (getState() == State.CREATED);
	}
	
	/**
	 * Return true if quest completed, false otherwise
	 * @return boolean
	 */
	public boolean isCompleted()
	{
		return (getState() == State.COMPLETED);
	}
	
	/**
	 * Return true if quest started, false otherwise
	 * @return boolean
	 */
	public boolean isStarted()
	{
		return (getState() == State.STARTED);
	}
	
	/**
	 * Return state of the quest after its initialization.<br>
	 * Actions:<br>
	 * <ul>
	 * <li>Remove drops from previous state.</li>
	 * <li>Set new state of the quest.</li>
	 * <li>Add drop for new state.</li>
	 * <li>Update information in database.</li>
	 * <li>Send packet QuestList to client.</li>
	 * </ul>
	 * @param state
	 * @return object
	 */
	public Object setState(byte state)
	{
		// set new state if it is not already in that state
		if (_state != state)
		{
			final boolean newQuest = isCreated();
			_state = state;
			
			if (newQuest)
			{
				Quest.createQuestInDb(this);
			}
			else
			{
				Quest.updateQuestInDb(this);
			}
			
			getPlayer().sendPacket(new QuestList());
		}
		return state;
	}
	
	/**
	 * @param state
	 * @return
	 */
	public Object setStateAndNotSave(byte state)
	{
		// set new state if it is not already in that state
		if (_state != state)
		{
			_state = state;
			getPlayer().sendPacket(new QuestList());
		}
		return state;
	}
	
	/**
	 * Add parameter used in quests.
	 * @param var : String pointing out the name of the variable for quest
	 * @param val : String pointing out the value of the variable for quest
	 * @return String (equal to parameter "val")
	 */
	public String setInternal(String var, String val)
	{
		if (_vars == null)
		{
			_vars = new FastMap<String, String>();
		}
		
		if (val == null)
		{
			val = "";
		}
		
		_vars.put(var, val);
		return val;
	}
	
	/**
	 * Return value of parameter "val" after adding the couple (var,val) in class variable "vars".<br>
	 * Actions:<br>
	 * <ul>
	 * <li>Initialize class variable "vars" if is null.</li>
	 * <li>Initialize parameter "val" if is null</li>
	 * <li>Add/Update couple (var,val) in class variable FastMap "vars"</li>
	 * <li>If the key represented by "var" exists in FastMap "vars", the couple (var,val) is updated in the database.<br>
	 * The key is known as existing if the preceding value of the key (given as result of function put()) is not null.<br>
	 * If the key doesn't exist, the couple is added/created in the database</li>
	 * <ul>
	 * @param var : String indicating the name of the variable for quest
	 * @param val : String indicating the value of the variable for quest
	 * @return String (equal to parameter "val")
	 */
	public String set(String var, String val)
	{
		if (_vars == null)
		{
			_vars = new FastMap<String, String>();
		}
		
		if (val == null)
		{
			val = "";
		}
		
		// FastMap.put() returns previous value associated with specified key, or null if there was no mapping for key.
		String old = _vars.put(var, val);
		
		if (old != null)
		{
			Quest.updateQuestVarInDb(this, var, val);
		}
		else
		{
			Quest.createQuestVarInDb(this, var, val);
		}
		
		if ("cond".equals(var))
		{
			try
			{
				int previousVal = 0;
				try
				{
					previousVal = Integer.parseInt(old);
				}
				catch (Exception ex)
				{
					previousVal = 0;
				}
				setCond(Integer.parseInt(val), previousVal);
			}
			catch (Exception e)
			{
				_log.log(Level.WARNING, getPlayer().getName() + ", " + getQuestName() + " cond [" + val + "] is not an integer.  Value stored, but no packet was sent: " + e.getMessage(), e);
			}
		}
		
		return val;
	}
	
	/**
	 * Internally handles the progression of the quest so that it is ready for sending appropriate packets to the client.<br>
	 * Actions:<br>
	 * <ul>
	 * <li>Check if the new progress number resets the quest to a previous (smaller) step.</li>
	 * <li>If not, check if quest progress steps have been skipped.</li>
	 * <li>If skipped, prepare the variable completedStateFlags appropriately to be ready for sending to clients.</li>
	 * <li>If no steps were skipped, flags do not need to be prepared...</li>
	 * <li>If the passed step resets the quest to a previous step, reset such that steps after the parameter are not considered, while skipped steps before the parameter, if any, maintain their info.</li>
	 * </ul>
	 * For more info on the variable communicating the progress steps to the client, please see ?
	 * @param cond : int indicating the step number for the current quest progress (as will be shown to the client)
	 * @param old : int indicating the previously noted step
	 */
	private void setCond(int cond, int old)
	{
		int completedStateFlags = 0; // initializing...
		
		// if there is no change since last setting, there is nothing to do here
		if (cond == old)
		{
			return;
		}
		
		// cond 0 and 1 do not need completedStateFlags. Also, if cond > 1, the 1st step must
		// always exist (i.e. it can never be skipped). So if cond is 2, we can still safely
		// assume no steps have been skipped.
		// Finally, more than 31 steps CANNOT be supported in any way with skipping.
		if ((cond < 3) || (cond > 31))
		{
			unset("__compltdStateFlags");
		}
		else
		{
			completedStateFlags = getInt("__compltdStateFlags");
		}
		
		// case 1: No steps have been skipped so far...
		if (completedStateFlags == 0)
		{
			// check if this step also doesn't skip anything. If so, no further work is needed
			// also, in this case, no work is needed if the state is being reset to a smaller value
			// in those cases, skip forward to informing the client about the change...
			
			// ELSE, if we just now skipped for the first time...prepare the flags!!!
			if (cond > (old + 1))
			{
				// set the most significant bit to 1 (indicates that there exist skipped states)
				// also, ensure that the least significant bit is an 1 (the first step is never skipped, no matter
				// what the cond says)
				completedStateFlags = 0x80000001;
				
				// since no flag had been skipped until now, the least significant bits must all
				// be set to 1, up until "old" number of bits.
				completedStateFlags |= ((1 << old) - 1);
				
				// now, just set the bit corresponding to the passed cond to 1 (current step)
				completedStateFlags |= (1 << (cond - 1));
				set("__compltdStateFlags", String.valueOf(completedStateFlags));
			}
		}
		// case 2: There were exist previously skipped steps
		else
		{
			// if this is a push back to a previous step, clear all completion flags ahead
			if (cond < old)
			{
				completedStateFlags &= ((1 << cond) - 1); // note, this also unsets the flag indicating that there exist skips
				
				// now, check if this resulted in no steps being skipped any more
				if (completedStateFlags == ((1 << cond) - 1))
				{
					unset("__compltdStateFlags");
				}
				else
				{
					// set the most significant bit back to 1 again, to correctly indicate that this skips states.
					// also, ensure that the least significant bit is an 1 (the first step is never skipped, no matter
					// what the cond says)
					completedStateFlags |= 0x80000001;
					set("__compltdStateFlags", String.valueOf(completedStateFlags));
				}
			}
			// if this moves forward, it changes nothing on previously skipped steps...so just mark this
			// state and we are done
			else
			{
				completedStateFlags |= (1 << (cond - 1));
				set("__compltdStateFlags", String.valueOf(completedStateFlags));
			}
		}
		
		// send a packet to the client to inform it of the quest progress (step change)
		QuestList ql = new QuestList();
		getPlayer().sendPacket(ql);
		
		int questId = getQuest().getQuestIntId();
		if ((questId > 0) && (questId < 19999) && (cond > 0))
		{
			getPlayer().sendPacket(new ExShowQuestMark(questId));
		}
	}
	
	/**
	 * Remove the variable of quest from the list of variables for the quest.<br>
	 * Concept:<br>
	 * Remove the variable of quest represented by "var" from the class variable FastMap "vars" and from the database.
	 * @param var : String designating the variable for the quest to be deleted
	 * @return String pointing out the previous value associated with the variable "var"
	 */
	public String unset(String var)
	{
		if (_vars == null)
		{
			return null;
		}
		
		String old = _vars.remove(var);
		
		if (old != null)
		{
			Quest.deleteQuestVarInDb(this, var);
		}
		
		return old;
	}
	
	/**
	 * Insert (or Update) in the database variables that need to stay persistent for this player after a reboot.<br>
	 * This function is for storage of values that do not related to a specific quest but are global for all quests.<br>
	 * For example, player's can get only once the Adena and XP reward for the first class quests, but they can make more than one first class quest.
	 * @param var : String designating the name of the variable for the quest
	 * @param value : String designating the value of the variable for the quest
	 */
	public final void saveGlobalQuestVar(String var, String value)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			statement = con.prepareStatement("REPLACE INTO character_quest_global_data (charId,var,value) VALUES (?,?,?)");
			statement.setInt(1, _player.getObjectId());
			statement.setString(2, var);
			statement.setString(3, value);
			statement.executeUpdate();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Could not insert player's global quest variable: " + e.getMessage(), e);
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
	 * @param var : String designating the name of the variable for the quest
	 * @return String : String representing the loaded value for the passed var, or an empty string if the var was invalid
	 */
	public final String getGlobalQuestVar(String var)
	{
		String result = "";
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			statement = con.prepareStatement("SELECT value FROM character_quest_global_data WHERE charId = ? AND var = ?");
			statement.setInt(1, _player.getObjectId());
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
			_log.log(Level.WARNING, "Could not load player's global quest variable: " + e.getMessage(), e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
		return result;
	}
	
	/**
	 * Permanently delete from the database one of the player's global quest variable that was previously saved.
	 * @param var : String designating the name of the variable
	 */
	public final void deleteGlobalQuestVar(String var)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			statement = con.prepareStatement("DELETE FROM character_quest_global_data WHERE charId = ? AND var = ?");
			statement.setInt(1, _player.getObjectId());
			statement.setString(2, var);
			statement.executeUpdate();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "could not delete player's global quest variable: " + e.getMessage(), e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
	}
	
	/**
	 * Return the value of the variable of quest represented by "var"
	 * @param var : name of the variable of quest
	 * @return String
	 */
	public String get(String var)
	{
		if (_vars == null)
		{
			return null;
		}
		
		return _vars.get(var);
	}
	
	/**
	 * Return the value of the variable of quest represented by "var"
	 * @param var : String designating the variable for the quest
	 * @return int
	 */
	public int getInt(String var)
	{
		if (_vars == null)
		{
			return 0;
		}
		
		final String variable = _vars.get(var);
		if ((variable == null) || (variable.length() == 0))
		{
			return 0;
		}
		
		int varint = 0;
		try
		{
			varint = Integer.parseInt(variable);
		}
		catch (Exception e)
		{
			_log.log(Level.FINER, getPlayer().getName() + ": variable " + var + " isn't an integer: " + varint + " ! " + e.getMessage(), e);
			// if (Config.AUTODELETE_INVALID_QUEST_DATA)
			// exitQuest(true);
		}
		
		return varint;
	}
	
	/**
	 * Add player to get notification of characters death
	 * @param character : L2Character of the character to get notification of death
	 */
	public void addNotifyOfDeath(L2Character character)
	{
		if ((character == null) || !(character instanceof L2PcInstance))
		{
			return;
		}
		
		((L2PcInstance) character).addNotifyQuestOfDeath(this);
	}
	
	// TODO: This all remains because of backward compatibility should be cleared when all scripts been re-writen in java!
	
	/**
	 * Return the quantity of one sort of item hold by the player
	 * @param itemId : ID of the item wanted to be count
	 * @return long
	 */
	public long getQuestItemsCount(int itemId)
	{
		return getQuest().getQuestItemsCount(getPlayer(), itemId);
	}
	
	/**
	 * @param itemId the item Id of the item you're looking for
	 * @return true if item exists in player's inventory, false - if not
	 */
	public boolean hasQuestItems(int itemId)
	{
		return getQuest().hasQuestItems(getPlayer(), itemId);
	}
	
	/**
	 * Return the level of enchantment on the weapon of the player(Done specifically for weapon SA's)
	 * @param itemId : ID of the item to check enchantment
	 * @return int
	 */
	public int getEnchantLevel(int itemId)
	{
		return getQuest().getEnchantLevel(getPlayer(), itemId);
	}
	
	/**
	 * Give adena to the player
	 * @param count
	 * @param applyRates
	 */
	public void giveAdena(long count, boolean applyRates)
	{
		giveItems(PcInventory.ADENA_ID, count, applyRates ? 0 : 1);
	}
	
	/**
	 * Give reward to player using multiplier's
	 * @param itemId
	 * @param count
	 */
	public void rewardItems(int itemId, long count)
	{
		getQuest().rewardItems(getPlayer(), itemId, count);
	}
	
	/**
	 * Give item/reward to the player
	 * @param itemId
	 * @param count
	 */
	public void giveItems(int itemId, long count)
	{
		giveItems(itemId, count, 0);
	}
	
	public void giveItems(int itemId, long count, int enchantlevel)
	{
		getQuest().giveItems(getPlayer(), itemId, count, enchantlevel);
	}
	
	public void giveItems(int itemId, long count, byte attributeId, int attributeLevel)
	{
		getQuest().giveItems(getPlayer(), itemId, count, attributeId, attributeLevel);
	}
	
	/**
	 * Drop Quest item using Config.RATE_QUEST_DROP
	 * @param itemId int Item Identifier of the item to be dropped
	 * @param count (minCount, maxCount) long Quantity of items to be dropped
	 * @param neededCount Quantity of items needed for quest
	 * @param dropChance int Base chance of drop, same as in droplist
	 * @param sound boolean indicating whether to play sound
	 * @return boolean indicating whether player has requested number of items
	 */
	public boolean dropQuestItems(int itemId, int count, long neededCount, int dropChance, boolean sound)
	{
		return dropQuestItems(itemId, count, count, neededCount, dropChance, sound);
	}
	
	public boolean dropQuestItems(int itemId, int minCount, int maxCount, long neededCount, int dropChance, boolean sound)
	{
		return getQuest().dropQuestItems(getPlayer(), itemId, minCount, maxCount, neededCount, dropChance, sound);
	}
	
	// TODO: More radar functions need to be added when the radar class is complete.
	// BEGIN STUFF THAT WILL PROBABLY BE CHANGED
	public void addRadar(int x, int y, int z)
	{
		getPlayer().getRadar().addMarker(x, y, z);
	}
	
	public void removeRadar(int x, int y, int z)
	{
		getPlayer().getRadar().removeMarker(x, y, z);
	}
	
	public void clearRadar()
	{
		getPlayer().getRadar().removeAllMarkers();
	}
	
	// END STUFF THAT WILL PROBABLY BE CHANGED
	
	/**
	 * Remove items from player's inventory when talking to NPC in order to have rewards.<br>
	 * Actions:<br>
	 * <ul>
	 * <li>Destroy quantity of items wanted</li>
	 * <li>Send new inventory list to player</li>
	 * </ul>
	 * @param itemId : Identifier of the item
	 * @param count : Quantity of items to destroy
	 */
	public void takeItems(int itemId, long count)
	{
		getQuest().takeItems(getPlayer(), itemId, count);
	}
	
	/**
	 * Send a packet in order to play sound at client terminal
	 * @param sound
	 */
	public void playSound(String sound)
	{
		getQuest().playSound(getPlayer(), sound);
	}
	
	/**
	 * Add XP and SP as quest reward
	 * @param exp
	 * @param sp
	 */
	public void addExpAndSp(int exp, int sp)
	{
		getQuest().addExpAndSp(getPlayer(), exp, sp);
	}
	
	/**
	 * @param loc
	 * @return number of ticks from GameTimeController
	 */
	public int getItemEquipped(int loc)
	{
		return getQuest().getItemEquipped(getPlayer(), loc);
	}
	
	/**
	 * Return the number of ticks from the GameTimeController
	 * @return int
	 */
	public int getGameTicks()
	{
		return getQuest().getGameTicks();
	}
	
	/**
	 * Return true if quest is to exited on clean up by QuestStateManager
	 * @return boolean
	 */
	public final boolean isExitQuestOnCleanUp()
	{
		return _isExitQuestOnCleanUp;
	}
	
	public void setIsExitQuestOnCleanUp(boolean isExitQuestOnCleanUp)
	{
		_isExitQuestOnCleanUp = isExitQuestOnCleanUp;
	}
	
	/**
	 * Start a timer for quest.
	 * @param name The name of the timer. Will also be the value for event of onEvent
	 * @param time The millisecond value the timer will elapse
	 */
	public void startQuestTimer(String name, long time)
	{
		getQuest().startQuestTimer(name, time, null, getPlayer(), false);
	}
	
	public void startQuestTimer(String name, long time, L2Npc npc)
	{
		getQuest().startQuestTimer(name, time, npc, getPlayer(), false);
	}
	
	public void startRepeatingQuestTimer(String name, long time)
	{
		getQuest().startQuestTimer(name, time, null, getPlayer(), true);
	}
	
	public void startRepeatingQuestTimer(String name, long time, L2Npc npc)
	{
		getQuest().startQuestTimer(name, time, npc, getPlayer(), true);
	}
	
	/**
	 * @param name the quest timer name.
	 * @return the QuestTimer object with the specified name, null if the name doesn't exist.
	 */
	public final QuestTimer getQuestTimer(String name)
	{
		return getQuest().getQuestTimer(name, null, getPlayer());
	}
	
	/**
	 * Add spawn for player instance
	 * @param npcId
	 * @return object id of newly spawned npc
	 */
	public L2Npc addSpawn(int npcId)
	{
		return addSpawn(npcId, getPlayer().getX(), getPlayer().getY(), getPlayer().getZ(), 0, false, 0);
	}
	
	public L2Npc addSpawn(int npcId, int despawnDelay)
	{
		return addSpawn(npcId, getPlayer().getX(), getPlayer().getY(), getPlayer().getZ(), 0, false, despawnDelay);
	}
	
	public L2Npc addSpawn(int npcId, int x, int y, int z)
	{
		return addSpawn(npcId, x, y, z, 0, false, 0);
	}
	
	/**
	 * Add spawn for player instance.<br>
	 * Will despawn after the spawn length expires.<br>
	 * Uses player's coords and heading.<br>
	 * Adds a little randomization in the x y coords.<br>
	 * @param npcId
	 * @param cha
	 * @return object id of newly spawned npc
	 */
	public L2Npc addSpawn(int npcId, L2Character cha)
	{
		return addSpawn(npcId, cha, true, 0);
	}
	
	public L2Npc addSpawn(int npcId, L2Character cha, int despawnDelay)
	{
		return addSpawn(npcId, cha.getX(), cha.getY(), cha.getZ(), cha.getHeading(), true, despawnDelay);
	}
	
	/**
	 * Add spawn for player instance.<br>
	 * Will despawn after the spawn length expires.
	 * @param npcId
	 * @param x
	 * @param y
	 * @param z
	 * @param despawnDelay
	 * @return object id of newly spawned npc
	 */
	public L2Npc addSpawn(int npcId, int x, int y, int z, int despawnDelay)
	{
		return addSpawn(npcId, x, y, z, 0, false, despawnDelay);
	}
	
	/**
	 * Add spawn for player instance.<br>
	 * Inherits coords and heading from specified L2Character instance.<br>
	 * It could be either the player, or any killed/attacked mob
	 * @param npcId
	 * @param cha
	 * @param randomOffset
	 * @param despawnDelay
	 * @return object id of newly spawned npc
	 */
	public L2Npc addSpawn(int npcId, L2Character cha, boolean randomOffset, int despawnDelay)
	{
		return addSpawn(npcId, cha.getX(), cha.getY(), cha.getZ(), cha.getHeading(), randomOffset, despawnDelay);
	}
	
	/**
	 * Add spawn for player instance
	 * @param npcId
	 * @param x
	 * @param y
	 * @param z
	 * @param heading
	 * @param randomOffset
	 * @param despawnDelay
	 * @return object id of newly spawned npc
	 */
	public L2Npc addSpawn(int npcId, int x, int y, int z, int heading, boolean randomOffset, int despawnDelay)
	{
		return getQuest().addSpawn(npcId, x, y, z, heading, randomOffset, despawnDelay, false);
	}
	
	/**
	 * Add spawn for player instance
	 * @param npcId
	 * @param x
	 * @param y
	 * @param z
	 * @param heading
	 * @param randomOffset
	 * @param despawnDelay
	 * @param isSummonSpawn
	 * @return object id of newly spawned npc
	 */
	public L2Npc addSpawn(int npcId, int x, int y, int z, int heading, boolean randomOffset, int despawnDelay, boolean isSummonSpawn)
	{
		return getQuest().addSpawn(npcId, x, y, z, heading, randomOffset, despawnDelay, isSummonSpawn);
	}
	
	public String showHtmlFile(String fileName)
	{
		return getQuest().showHtmlFile(getPlayer(), fileName);
	}
	
	/**
	 * @param type the type of the quest, {@link com.l2jserver.gameserver.model.quest.QuestState.QuestType}.
	 * @return this quest state.
	 */
	public QuestState exitQuest(QuestType type)
	{
		switch (type)
		{
			case REPEATABLE:
			case ONE_TIME:
			{
				exitQuest(type == QuestType.REPEATABLE);
				break;
			}
			case DAILY:
			{
				exitQuest(false);
				setRestartTime();
				break;
			}
		}
		return this;
	}
	
	/**
	 * Destroy element used by quest when quest is exited
	 * @param repeatable
	 * @return QuestState
	 */
	public QuestState exitQuest(boolean repeatable)
	{
		// remove this quest from the notifyDeath list of this character if its on this list
		_player.removeNotifyQuestOfDeath(this);
		
		if (isCompleted() || isCreated())
		{
			return this;
		}
		
		// Say quest is completed
		setState(State.COMPLETED);
		
		// Clean registered quest items
		int[] itemIdList = getQuest().getRegisteredItemIds();
		if (itemIdList != null)
		{
			for (int element : itemIdList)
			{
				takeItems(element, -1);
			}
		}
		
		// If quest is repeatable, delete quest from list of quest of the player and from database (quest CAN be created again => repeatable)
		if (repeatable)
		{
			getPlayer().delQuestState(getQuestName());
			Quest.deleteQuestInDb(this);
			
			_vars = null;
		}
		else
		{
			// Otherwise, delete variables for quest and update database (quest CANNOT be created again => not repeatable)
			if (_vars != null)
			{
				for (String var : _vars.keySet())
				{
					unset(var);
				}
			}
			
			Quest.updateQuestInDb(this);
		}
		
		return this;
	}
	
	public void showQuestionMark(int number)
	{
		getPlayer().sendPacket(new TutorialShowQuestionMark(number));
	}
	
	public void playTutorialVoice(String voice)
	{
		getPlayer().sendPacket(new PlaySound(2, voice, 0, 0, getPlayer().getX(), getPlayer().getY(), getPlayer().getZ()));
	}
	
	public void showTutorialHTML(String html)
	{
		String text = HtmCache.getInstance().getHtm(getPlayer().getHtmlPrefix(), "data/scripts/quests/255_Tutorial/" + html);
		if (text == null)
		{
			_log.warning("missing html page data/scripts/quests/255_Tutorial/" + html);
			text = "<html><body>File data/scripts/quests/255_Tutorial/" + html + " not found or file is empty.</body></html>";
		}
		getPlayer().sendPacket(new TutorialShowHtml(text));
	}
	
	public void closeTutorialHtml()
	{
		getPlayer().sendPacket(new TutorialCloseHtml());
	}
	
	public void onTutorialClientEvent(int number)
	{
		getPlayer().sendPacket(new TutorialEnableClientEvent(number));
	}
	
	public void dropItem(L2MonsterInstance npc, L2PcInstance player, int itemId, int count)
	{
		npc.dropItem(player, itemId, count);
	}
	
	/**
	 * Set the restart time for the daily quests.<br>
	 * The time is hardcoded at {@link Quest#getResetHour()} hours, {@link Quest#getResetMinutes()} minutes of the following day.<br>
	 * It can be overridden in scripts (quests).
	 */
	public void setRestartTime()
	{
		final Calendar reDo = Calendar.getInstance();
		if (reDo.get(Calendar.HOUR_OF_DAY) >= getQuest().getResetHour())
		{
			reDo.add(Calendar.DATE, 1);
		}
		reDo.set(Calendar.HOUR_OF_DAY, getQuest().getResetHour());
		reDo.set(Calendar.MINUTE, getQuest().getResetMinutes());
		set("restartTime", String.valueOf(reDo.getTimeInMillis()));
	}
	
	/**
	 * @return {@code true} if the quest is available, for example daily quests, {@code false} otherwise.
	 */
	public boolean isNowAvailable()
	{
		final String val = get("restartTime");
		return ((val == null) || !Util.isDigit(val)) || (Long.parseLong(val) <= System.currentTimeMillis());
	}
}
