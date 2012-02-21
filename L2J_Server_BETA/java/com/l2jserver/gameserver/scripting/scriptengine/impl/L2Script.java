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
package com.l2jserver.gameserver.scripting.scriptengine.impl;

import java.util.ArrayList;
import java.util.List;

import javolution.util.FastList;

import com.l2jserver.gameserver.model.L2Augmentation;
import com.l2jserver.gameserver.model.L2Clan;
import com.l2jserver.gameserver.model.L2Object;
import com.l2jserver.gameserver.model.L2Transformation;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.actor.templates.L2PcTemplate;
import com.l2jserver.gameserver.model.entity.FortSiege;
import com.l2jserver.gameserver.model.entity.Siege;
import com.l2jserver.gameserver.model.entity.TvTEventTeam;
import com.l2jserver.gameserver.model.itemcontainer.ItemContainer;
import com.l2jserver.gameserver.model.items.instance.L2HennaInstance;
import com.l2jserver.gameserver.model.items.instance.L2ItemInstance;
import com.l2jserver.gameserver.model.quest.Quest;
import com.l2jserver.gameserver.model.skills.L2Skill;
import com.l2jserver.gameserver.scripting.scriptengine.listeners.character.AttackListener;
import com.l2jserver.gameserver.scripting.scriptengine.listeners.character.DeathListener;
import com.l2jserver.gameserver.scripting.scriptengine.listeners.character.SkillUseListener;
import com.l2jserver.gameserver.scripting.scriptengine.listeners.clan.ClanCreationListener;
import com.l2jserver.gameserver.scripting.scriptengine.listeners.clan.ClanMembershipListener;
import com.l2jserver.gameserver.scripting.scriptengine.listeners.clan.ClanWarListener;
import com.l2jserver.gameserver.scripting.scriptengine.listeners.clan.ClanWarehouseListener;
import com.l2jserver.gameserver.scripting.scriptengine.listeners.events.FortSiegeListener;
import com.l2jserver.gameserver.scripting.scriptengine.listeners.events.SiegeListener;
import com.l2jserver.gameserver.scripting.scriptengine.listeners.events.TvTListener;
import com.l2jserver.gameserver.scripting.scriptengine.listeners.player.AugmentListener;
import com.l2jserver.gameserver.scripting.scriptengine.listeners.player.DropListener;
import com.l2jserver.gameserver.scripting.scriptengine.listeners.player.EquipmentListener;
import com.l2jserver.gameserver.scripting.scriptengine.listeners.player.HennaListener;
import com.l2jserver.gameserver.scripting.scriptengine.listeners.player.ItemTracker;
import com.l2jserver.gameserver.scripting.scriptengine.listeners.player.NewItemListener;
import com.l2jserver.gameserver.scripting.scriptengine.listeners.player.PlayerDespawnListener;
import com.l2jserver.gameserver.scripting.scriptengine.listeners.player.PlayerLevelListener;
import com.l2jserver.gameserver.scripting.scriptengine.listeners.player.PlayerSpawnListener;
import com.l2jserver.gameserver.scripting.scriptengine.listeners.player.ProfessionChangeListener;
import com.l2jserver.gameserver.scripting.scriptengine.listeners.player.TransformListener;
import com.l2jserver.gameserver.scripting.scriptengine.listeners.talk.ChatFilterListener;
import com.l2jserver.gameserver.scripting.scriptengine.listeners.talk.ChatListener;
import com.l2jserver.gameserver.scripting.scriptengine.listeners.talk.ChatListener.ChatTargetType;

/**
 * L2Script is an extension of Quest.java which makes use of the L2J listeners.<br>
 * It is much more in-depth than its predecessor,<br>
 * It is strongly recommended for the more advanced developers.<br>
 * Methods with boolean return values can be used as code blockers. This means that if the return is false, the action for which the listener was fired does not happen.<br>
 * @author TheOne
 */
public abstract class L2Script extends Quest
{
	private FastList<L2JListener> _listeners = new FastList<L2JListener>();
	
	/**
	 * constructor
	 * @param name
	 * @param descr
	 */
	public L2Script(String name, String descr)
	{
		super(-1, name, descr);
		init();
	}
	
	/**
	 * constructor
	 * @param questId
	 * @param name
	 * @param descr
	 */
	public L2Script(int questId, String name, String descr)
	{
		super(questId, name, descr);
		init();
	}
	
	// New in this version: profession change + player level change
	// TODO: pet item use listeners
	// TODO: player subclass listeners ?? (needed?)
	
	/**
	 * Initialize the variables and listeners/notifiers here This method is called automatically! No need to call it again ;)
	 */
	public abstract void init();
	
	/**
	 * Unloads the script
	 */
	@Override
	public boolean unload()
	{
		for (L2JListener listener : _listeners)
		{
			listener.unregister();
		}
		_listeners.clear();
		return super.unload();
	}
	
	/**
	 * Unregisters the listeners and removes them from the listeners list
	 * @param removeList
	 */
	private void removeListeners(List<L2JListener> removeList)
	{
		for (L2JListener listener : removeList)
		{
			listener.unregister();
			_listeners.remove(listener);
		}
	}
	
	/**
	 * Used locally to call onDeath()
	 * @param killer
	 * @param victim
	 * @return
	 */
	private boolean notifyDeath(L2Character killer, L2Character victim)
	{
		return onDeath(killer, victim);
	}
	
	/**
	 * Used locally to call onAttack(L2Character,L2Character)
	 * @param target
	 * @param attacker
	 * @return
	 */
	private boolean notifyAttack(L2Character target, L2Character attacker)
	{
		return onAttack(target, attacker);
	}
	
	// Register for event notification
	/**
	 * Will notify the script when this L2Character is killed<br>
	 * Can be used for Npc or Player<br>
	 * When the L2Character is killed, the onDeath(L2Character,L2Character) method will be fired<br>
	 * To set a global notifier (for all L2Character) set character to null!
	 * @param character
	 */
	public void addDeathNodify(final L2Character character)
	{
		DeathListener listener = new DeathListener(character)
		{
			@Override
			public boolean onKill(L2Character target, L2Character killer)
			{
				return notifyDeath(killer, target);
			}
			
			@Override
			public boolean onDeath(L2Character target, L2Character killer)
			{
				return notifyDeath(killer, target);
			}
		};
		_listeners.add(listener);
	}
	
	/**
	 * Removes the death listener from this L2Character
	 * @param character
	 */
	public void removeDeathNotify(L2Character character)
	{
		List<L2JListener> removeList = new ArrayList<L2JListener>();
		for (L2JListener listener : _listeners)
		{
			if (listener instanceof DeathListener)
			{
				if (((DeathListener) listener).getCharacter() == character)
				{
					removeList.add(listener);
				}
			}
		}
		removeListeners(removeList);
	}
	
	/**
	 * When a player logs in it will call the onPlayerLogin(L2PcInstance player) method<br>
	 * When a player logs out it will call the onPlayerLogout(L2PcInstance player) method<br>
	 */
	public void addLoginLogoutNotify()
	{
		PlayerSpawnListener spawn = new PlayerSpawnListener()
		{
			@Override
			public void onSpawn(L2PcInstance player)
			{
				onPlayerLogin(player);
			}
		};
		PlayerDespawnListener despawn = new PlayerDespawnListener()
		{
			@Override
			public void onDespawn(L2PcInstance player)
			{
				onPlayerLogout(player);
			}
		};
		_listeners.add(spawn);
		_listeners.add(despawn);
	}
	
	/**
	 * Removes the login and logout notifications
	 */
	public void removeLoginLogoutNotify()
	{
		List<L2JListener> removeList = new ArrayList<L2JListener>();
		for (L2JListener listener : _listeners)
		{
			if (listener instanceof PlayerSpawnListener || listener instanceof PlayerDespawnListener)
			{
				removeList.add(listener);
			}
		}
		removeListeners(removeList);
	}
	
	/**
	 * Adds an L2Character-specific attack listener Fires onAttack(L2Character target, L2Character attacker) when this character is attacked AND when it gets attacked
	 * @param character
	 */
	public void addAttackNotify(final L2Character character)
	{
		if (character != null)
		{
			AttackListener listener = new AttackListener(character)
			{
				@Override
				public boolean onAttack(L2Character target)
				{
					return notifyAttack(target, character);
				}
				
				@Override
				public boolean isAttacked(L2Character attacker)
				{
					return notifyAttack(character, attacker);
				}
			};
			_listeners.add(listener);
		}
	}
	
	/**
	 * Removes the notifications for attacks from/on this L2Character
	 * @param character
	 */
	public void removeAttackNotify(L2Character character)
	{
		if (character != null)
		{
			List<L2JListener> removeList = new ArrayList<L2JListener>();
			for (L2JListener listener : _listeners)
			{
				if (listener instanceof AttackListener && ((AttackListener) listener).getCharacter() == character)
				{
					removeList.add(listener);
				}
			}
			removeListeners(removeList);
		}
	}
	
	/**
	 * NPC specific, will only be triggered when npc with the given ID uses the correct skill Use skillId = -1 to be notified of all skills used Use npcId = -1 to be notified for all NPCs use npcId = -2 to be notified for all players use npcId = -3 to be notified for all L2Characters
	 * @param npcId
	 * @param skillId
	 */
	public void addSkillUseNotify(int npcId, int skillId)
	{
		SkillUseListener listener = new SkillUseListener(npcId, skillId)
		{
			@Override
			public boolean onSkillUse(L2Skill skill, L2Character character, L2Object[] targets)
			{
				return onUseSkill(skill, character, targets);
			}
		};
		_listeners.add(listener);
	}
	
	/**
	 * L2Character specific, will only be fired when this L2Character uses the specified skill Use skillId = -1 to be notified of all skills used
	 * @param character
	 * @param skillId
	 */
	public void addSkillUseNotify(L2Character character, int skillId)
	{
		if (character != null)
		{
			SkillUseListener listener = new SkillUseListener(character, skillId)
			{
				@Override
				public boolean onSkillUse(L2Skill skill, L2Character character, L2Object[] targets)
				{
					return onUseSkill(skill, character, targets);
				}
			};
			_listeners.add(listener);
		}
	}
	
	/**
	 * Removes a skill use listener
	 * @param character
	 */
	public void removeSkillUseNotify(L2Character character)
	{
		if (character != null)
		{
			List<L2JListener> removeList = new ArrayList<L2JListener>();
			for (L2JListener listener : _listeners)
			{
				if (listener instanceof SkillUseListener && ((SkillUseListener) listener).getCharacter() == character)
				{
					removeList.add(listener);
				}
			}
			removeListeners(removeList);
		}
	}
	
	/**
	 * Removes a skill use listener
	 * @param npcId
	 */
	public void removeSkillUseNotify(int npcId)
	{
		List<L2JListener> removeList = new ArrayList<L2JListener>();
		for (L2JListener listener : _listeners)
		{
			if (listener instanceof SkillUseListener && ((SkillUseListener) listener).getNpcId() == npcId)
			{
				removeList.add(listener);
			}
		}
		removeListeners(removeList);
	}
	
	/**
	 * Adds a notification for when a clan is created or levels up
	 */
	public void addClanCreationLevelUpNotify()
	{
		ClanCreationListener listener = new ClanCreationListener()
		{
			@Override
			public void onClanCreate(L2Clan clan)
			{
				onClanCreated(clan);
			}
			
			@Override
			public boolean onClanLevelUp(L2Clan clan, int oldLevel)
			{
				return onClanLeveledUp(clan, oldLevel);
			}
		};
		_listeners.add(listener);
	}
	
	/**
	 * Removes the clan creation and level up notifications
	 */
	public void removeClanCreationLevelUpNotify()
	{
		List<L2JListener> removeList = new ArrayList<L2JListener>();
		for (L2JListener listener : _listeners)
		{
			if (listener instanceof ClanCreationListener)
			{
				removeList.add(listener);
			}
		}
		removeListeners(removeList);
	}
	
	/**
	 * Adds a notification for players joining and leaving a clan as well as clan leadership changes
	 */
	public void addClanJoinLeaveNotify()
	{
		ClanMembershipListener listener = new ClanMembershipListener()
		{
			@Override
			public boolean onJoin(L2PcInstance player, L2Clan clan)
			{
				return onClanJoin(player, clan);
			}
			
			@Override
			public boolean onLeaderChange(L2Clan clan, L2PcInstance newLeader, L2PcInstance oldLeader)
			{
				return onClanLeaderChange(player, clan);
			}
			
			@Override
			public boolean onLeave(int playerObjId, L2Clan clan)
			{
				return onClanLeave(playerObjId, clan);
			}
		};
		_listeners.add(listener);
	}
	
	/**
	 * Removes the notification for players joining and leaving a clan as well as clan leadership changes
	 */
	public void removeClanJoinLeaveNotify()
	{
		List<L2JListener> removeList = new ArrayList<L2JListener>();
		for (L2JListener listener : _listeners)
		{
			if (listener instanceof ClanMembershipListener)
			{
				removeList.add(listener);
			}
		}
		removeListeners(removeList);
	}
	
	/**
	 * Adds a notification for when an item from a clanwarehouse is added, deleted or transfered
	 * @param clan
	 */
	public void addClanWarehouseNotify(L2Clan clan)
	{
		if (clan != null)
		{
			ClanWarehouseListener listener = new ClanWarehouseListener(clan)
			{
				@Override
				public boolean onAddItem(String process, L2ItemInstance item, L2PcInstance actor)
				{
					return onClanWarehouseAddItem(process, item, actor);
				}
				
				@Override
				public boolean onDeleteItem(String process, L2ItemInstance item, long count, L2PcInstance actor)
				{
					return onClanWarehouseDeleteItem(process, item, count, actor);
				}
				
				@Override
				public boolean onTransferItem(String process, L2ItemInstance item, long count, ItemContainer target, L2PcInstance actor)
				{
					return onClanWarehouseTransferItem(process, item, count, target, actor);
				}
			};
			_listeners.add(listener);
		}
	}
	
	/**
	 * Removes a clan warehouse notifier
	 * @param clan
	 */
	public void removeClanWarehouseNotify(L2Clan clan)
	{
		if (clan != null)
		{
			List<L2JListener> removeList = new ArrayList<L2JListener>();
			for (L2JListener listener : _listeners)
			{
				if (listener instanceof ClanWarehouseListener && ((ClanWarehouseListener) listener).getWarehouse() == clan.getWarehouse())
				{
					removeList.add(listener);
				}
			}
			removeListeners(removeList);
		}
	}
	
	/**
	 * Adds a notifier for when clan wars start and end
	 */
	public void addClanWarNotify()
	{
		ClanWarListener listener = new ClanWarListener()
		{
			@Override
			public boolean onWarStart(L2Clan clan1, L2Clan clan2)
			{
				return onClanWarEvent(clan1, clan2, EventStage.start);
			}
			
			@Override
			public boolean onWarEnd(L2Clan clan1, L2Clan clan2)
			{
				return onClanWarEvent(clan1, clan2, EventStage.end);
			}
		};
		_listeners.add(listener);
	}
	
	/**
	 * Removes the notification for start/end of clan wars
	 */
	public void removeClanWarNotify()
	{
		List<L2JListener> removeList = new ArrayList<L2JListener>();
		for (L2JListener listener : _listeners)
		{
			if (listener instanceof ClanWarListener)
			{
				removeList.add(listener);
			}
		}
		removeListeners(removeList);
	}
	
	/**
	 * Notifies when fort sieges start and end
	 */
	public void addFortSiegeNotify()
	{
		FortSiegeListener listener = new FortSiegeListener()
		{
			@Override
			public boolean onStart(FortSiege fortSiege)
			{
				return onFortSiegeEvent(fortSiege, EventStage.start);
			}
			
			@Override
			public void onEnd(FortSiege fortSiege)
			{
				onFortSiegeEvent(fortSiege, EventStage.end);
			}
		};
		_listeners.add(listener);
	}
	
	/**
	 * Removes the notification for fort sieges
	 */
	public void removeFortSiegeNotify()
	{
		List<L2JListener> removeList = new ArrayList<L2JListener>();
		for (L2JListener listener : _listeners)
		{
			if (listener instanceof FortSiegeListener)
			{
				removeList.add(listener);
			}
		}
		removeListeners(removeList);
	}
	
	/**
	 * Adds a notifier for when castle sieges start and end
	 */
	public void addSiegeNotify()
	{
		SiegeListener listener = new SiegeListener()
		{
			@Override
			public boolean onStart(Siege siege)
			{
				return onSiegeEvent(siege, EventStage.start);
			}
			
			@Override
			public void onEnd(Siege siege)
			{
				onSiegeEvent(siege, EventStage.end);
			}
			
			@Override
			public void onControlChange(Siege siege)
			{
				onCastleControlChange(siege);
			}
		};
		_listeners.add(listener);
	}
	
	/**
	 * Removes notification for castle sieges
	 */
	public void removeSiegeNotify()
	{
		List<L2JListener> removeList = new ArrayList<L2JListener>();
		for (L2JListener listener : _listeners)
		{
			if (listener instanceof SiegeListener)
			{
				removeList.add(listener);
			}
		}
		removeListeners(removeList);
	}
	
	/**
	 * Notifies of events on TvT:<br>
	 * start<br>
	 * end<br>
	 * registion begin<br>
	 * event stopped<br>
	 * player killed<br>
	 */
	public void addTvTNotify()
	{
		TvTListener listener = new TvTListener()
		{
			@Override
			public void onBegin()
			{
				onTvtEvent(EventStage.start);
			}
			
			@Override
			public void onKill(L2PcInstance killed, L2PcInstance killer, TvTEventTeam killerTeam)
			{
				onTvtKill(killed, killer, killerTeam);
			}
			
			@Override
			public void onEnd()
			{
				onTvtEvent(EventStage.end);
			}
			
			@Override
			public void onRegistrationStart()
			{
				onTvtEvent(EventStage.registration_begin);
			}
		};
		_listeners.add(listener);
	}
	
	/**
	 * Removes the TvT notifications
	 */
	public void removeTvtNotify()
	{
		List<L2JListener> removeList = new ArrayList<L2JListener>();
		for (L2JListener listener : _listeners)
		{
			if (listener instanceof TvTListener)
			{
				removeList.add(listener);
			}
		}
		removeListeners(removeList);
	}
	
	/**
	 * Adds a notifier for when items get augmented
	 */
	public void addItemAugmentNotify()
	{
		AugmentListener listener = new AugmentListener()
		{
			@Override
			public boolean onAugment(L2ItemInstance item, L2Augmentation augmentation)
			{
				return onItemAugment(item, augmentation, true);
			}
			
			@Override
			public boolean onRemoveAugment(L2ItemInstance item, L2Augmentation augmentation)
			{
				return onItemAugment(item, augmentation, false);
			}
		};
		_listeners.add(listener);
	}
	
	/**
	 * Removes the item augmentation listener
	 */
	public void removeItemAugmentNotify()
	{
		List<L2JListener> removeList = new ArrayList<L2JListener>();
		for (L2JListener listener : _listeners)
		{
			if (listener instanceof AugmentListener)
			{
				removeList.add(listener);
			}
		}
		removeListeners(removeList);
	}
	
	/**
	 * Adds a listener for items dropped and picked up by players
	 */
	public void addItemDropPickupNotify()
	{
		DropListener listener = new DropListener()
		{
			
			@Override
			public boolean onDrop(L2ItemInstance item, L2PcInstance dropper, int x, int y, int z)
			{
				return onItemDrop(item, dropper, x, y, z);
			}
			
			@Override
			public boolean onPickup(L2ItemInstance item, L2PcInstance picker, int x, int y, int z)
			{
				return onItemPickup(item, picker, x, y, z);
			}
		};
		_listeners.add(listener);
	}
	
	/**
	 * Removes the item drop and pickup listeners
	 */
	public void removeItemDropPickupNotify()
	{
		List<L2JListener> removeList = new ArrayList<L2JListener>();
		for (L2JListener listener : _listeners)
		{
			if (listener instanceof DropListener)
			{
				removeList.add(listener);
			}
		}
		removeListeners(removeList);
	}
	
	/**
	 * Adds a player level change listener<br>
	 * Set player to null if you wish to be notified for all the players on the server.
	 * @param player
	 */
	public void addPlayerLevelNotify(L2PcInstance player)
	{
		PlayerLevelListener listener = new PlayerLevelListener(player)
		{
			@Override
			public void levelChanged(L2PcInstance player, int oldLevel, int newLevel)
			{
				onPlayerLevelChange(player, oldLevel, newLevel);
			}
		};
		_listeners.add(listener);
	}
	
	/**
	 * Removes the player level notification for the given player<br>
	 * Removes all global notifications if player = null
	 * @param player
	 */
	public void removePlayerLevelNotify(L2PcInstance player)
	{
		List<L2JListener> removeList = new ArrayList<L2JListener>();
		for (L2JListener listener : _listeners)
		{
			if (listener instanceof PlayerLevelListener && listener.getPlayer() == player)
			{
				removeList.add(listener);
			}
		}
		removeListeners(removeList);
	}
	
	/**
	 * Adds a player profession change listener.<br>
	 * Set player to null if you wish to be notified for all the players on the server.
	 * @param player
	 */
	public void addProfessionChangeNotify(L2PcInstance player)
	{
		ProfessionChangeListener listener = new ProfessionChangeListener(player)
		{
			@Override
			public void professionChanged(L2PcInstance player, boolean isSubClass, L2PcTemplate template)
			{
				onProfessionChange(player, isSubClass, template);
			}
		};
		_listeners.add(listener);
	}
	
	/**
	 * Removes the profession change notification for the given player<br>
	 * Removes all global notifications if player = null
	 * @param player
	 */
	public void removeProfessionChangeNotify(L2PcInstance player)
	{
		List<L2JListener> removeList = new ArrayList<L2JListener>();
		for (L2JListener listener : _listeners)
		{
			if (listener instanceof ProfessionChangeListener && listener.getPlayer() == player)
			{
				removeList.add(listener);
			}
		}
		removeListeners(removeList);
	}
	
	/**
	 * Adds an item equip/unequip listener.<br>
	 * Set player to null if you wish to be notified for all the players on server
	 * @param player
	 */
	public void addEquipmentNotify(L2PcInstance player)
	{
		EquipmentListener listener = new EquipmentListener(player)
		{
			@Override
			public boolean onEquip(L2ItemInstance item, boolean isEquipped)
			{
				return onItemEquip(player, item, isEquipped);
			}
		};
		_listeners.add(listener);
	}
	
	/**
	 * Removes an equip/unequip listener<br>
	 * Set player to null if you wish to remove a global listener
	 * @param player
	 */
	public void removeEquipmentNotify(L2PcInstance player)
	{
		List<L2JListener> removeList = new ArrayList<L2JListener>();
		for (L2JListener listener : _listeners)
		{
			if (listener instanceof EquipmentListener && ((EquipmentListener) listener).getPlayer() == player)
			{
				removeList.add(listener);
			}
		}
		removeListeners(removeList);
	}
	
	/**
	 * Adds a Henna add/remove notifier
	 */
	public void addHennaNotify()
	{
		HennaListener listener = new HennaListener()
		{
			@Override
			public boolean onAddHenna(L2PcInstance player, L2HennaInstance henna)
			{
				return onHennaModify(player, henna, true);
			}
			
			@Override
			public boolean onRemoveHenna(L2PcInstance player, L2HennaInstance henna)
			{
				return onHennaModify(player, henna, false);
			}
		};
		_listeners.add(listener);
	}
	
	/**
	 * Removes the henna add/remove notifier
	 */
	public void removeHennaNotify()
	{
		List<L2JListener> removeList = new ArrayList<L2JListener>();
		for (L2JListener listener : _listeners)
		{
			if (listener instanceof HennaListener)
			{
				removeList.add(listener);
			}
		}
		removeListeners(removeList);
	}
	
	/**
	 * Adds an item tracker notifier.<br>
	 * It will keep track of all movements for the items with the given IDs
	 * @param itemIds
	 */
	public void addItemTracker(final List<Integer> itemIds)
	{
		if (itemIds != null)
		{
			ItemTracker listener = new ItemTracker(itemIds)
			{
				@Override
				public void onDrop(L2ItemInstance item, L2PcInstance player)
				{
					onItemTrackerEvent(item, player, null, ItemTrackerEvent.drop);
				}
				
				@Override
				public void onAddToInventory(L2ItemInstance item, L2PcInstance player)
				{
					onItemTrackerEvent(item, player, null, ItemTrackerEvent.add_to_inventory);
				}
				
				@Override
				public void onDestroy(L2ItemInstance item, L2PcInstance player)
				{
					onItemTrackerEvent(item, player, null, ItemTrackerEvent.destroy);
				}
				
				@Override
				public void onTransfer(L2ItemInstance item, L2PcInstance player, ItemContainer target)
				{
					onItemTrackerEvent(item, player, target, ItemTrackerEvent.transfer);
				}
			};
			_listeners.add(listener);
		}
	}
	
	/**
	 * Removes all the item trackers
	 */
	public void removeItemTrackers()
	{
		List<L2JListener> removeList = new ArrayList<L2JListener>();
		for (L2JListener listener : _listeners)
		{
			if (listener instanceof ItemTracker)
			{
				removeList.add(listener);
			}
		}
		removeListeners(removeList);
	}
	
	/**
	 * Adds an item creation notifier
	 * @param itemIds
	 */
	public void addNewItemNotify(List<Integer> itemIds)
	{
		if (itemIds != null)
		{
			NewItemListener listener = new NewItemListener(itemIds)
			{
				
				@Override
				public boolean onCreate(int itemId, L2PcInstance player)
				{
					return onItemCreate(itemId, player);
				}
			};
			_listeners.add(listener);
		}
	}
	
	/**
	 * Removes all new items notifiers
	 */
	public void removeNewItemNotify()
	{
		List<L2JListener> removeList = new ArrayList<L2JListener>();
		for (L2JListener listener : _listeners)
		{
			if (listener instanceof NewItemListener)
			{
				removeList.add(listener);
			}
		}
		removeListeners(removeList);
	}
	
	/**
	 * Adds a player transformation notifier
	 * @param player
	 */
	public void addTransformNotify(final L2PcInstance player)
	{
		if (player != null)
		{
			TransformListener listener = new TransformListener(player)
			{
				@Override
				public boolean onTransform(L2Transformation transformation)
				{
					return onPlayerTransform(player, transformation, true);
				}
				
				@Override
				public boolean onUntransform(L2Transformation transformation)
				{
					return onPlayerTransform(player, transformation, false);
				}
			};
			_listeners.add(listener);
		}
	}
	
	/**
	 * Removes a player transform notifier
	 * @param player
	 */
	public void removeTransformNotify(L2PcInstance player)
	{
		if (player != null)
		{
			List<L2JListener> removeList = new ArrayList<L2JListener>();
			for (L2JListener listener : _listeners)
			{
				if (listener instanceof TransformListener && listener.getPlayer() == player)
				{
					removeList.add(listener);
				}
			}
			removeListeners(removeList);
		}
	}
	
	/**
	 * Adds a chat filter
	 */
	public void addPlayerChatFilter()
	{
		ChatFilterListener listener = new ChatFilterListener()
		{
			@Override
			public String onTalk(String text, L2PcInstance origin, ChatTargetType targetType)
			{
				return filterChat(text, origin, targetType);
			}
		};
		_listeners.add(listener);
	}
	
	/**
	 * Removes a chat filter
	 */
	public void removePlayerChatFilter()
	{
		List<L2JListener> removeList = new ArrayList<L2JListener>();
		for (L2JListener listener : _listeners)
		{
			if (listener instanceof ChatFilterListener)
			{
				removeList.add(listener);
			}
		}
		removeListeners(removeList);
	}
	
	/**
	 * Adds a player chat notifier
	 */
	public void addPlayerTalkNotify()
	{
		ChatListener listener = new ChatListener()
		{
			@Override
			public void onTalk(String text, L2PcInstance origin, String target, ChatTargetType targetType)
			{
				onPlayerTalk(text, origin, target, targetType);
			}
		};
		_listeners.add(listener);
	}
	
	/**
	 * Removes all player chat notifiers
	 */
	public void removePlayerTalkNotify()
	{
		List<L2JListener> removeList = new ArrayList<L2JListener>();
		for (L2JListener listener : _listeners)
		{
			if (listener instanceof ChatListener)
			{
				removeList.add(listener);
			}
		}
		removeListeners(removeList);
	}
	
	// Script notifications
	/**
	 * Fired when a player logs in
	 * @param player
	 */
	public void onPlayerLogin(L2PcInstance player)
	{
		
	}
	
	/**
	 * Fired when a player logs out
	 * @param player
	 */
	public void onPlayerLogout(L2PcInstance player)
	{
		
	}
	
	/**
	 * Fired when a L2Character registered with addAttackNotify is either attacked or attacks another L2Character
	 * @param target
	 * @param attacker
	 * @return
	 */
	public boolean onAttack(L2Character target, L2Character attacker)
	{
		return true;
	}
	
	/**
	 * Fired when a L2Character registered with addNotifyDeath is either killed or kills another L2Character
	 * @param killer
	 * @param victim
	 * @return
	 */
	public boolean onDeath(L2Character killer, L2Character victim)
	{
		return true;
	}
	
	/**
	 * Fired when a SKillUseListener gets triggered.<br>
	 * Register using addSkillUseNotify()
	 * @param skill
	 * @param caster
	 * @param targets
	 * @return
	 */
	public boolean onUseSkill(L2Skill skill, L2Character caster, L2Object[] targets)
	{
		return true;
	}
	
	/**
	 * Fired when a clan is created Register the listener using addClanCreationLevelUpNotify()
	 * @param clan
	 */
	public void onClanCreated(L2Clan clan)
	{
		
	}
	
	/**
	 * Fired when a clan levels up<br>
	 * Register the listener using addClanCreationLevelUpListener()
	 * @param clan
	 * @param oldLevel
	 * @return
	 */
	public boolean onClanLeveledUp(L2Clan clan, int oldLevel)
	{
		return true;
	}
	
	/**
	 * Fired when a player joins a clan<br>
	 * Register the listener with addClanJoinLeaveNotify()<br>
	 * @param player
	 * @param clan
	 * @return
	 */
	public boolean onClanJoin(L2PcInstance player, L2Clan clan)
	{
		return true;
	}
	
	/**
	 * Fired when a player leaves a clan<br>
	 * Register the listener with addClanJoinLeaveNotify()<br>
	 * @param playerObjId
	 * @param clan
	 * @return
	 */
	public boolean onClanLeave(int playerObjId, L2Clan clan)
	{
		return true;
	}
	
	/**
	 * Fired when a clan leader is changed for another<br>
	 * Register the listener with addClanJoinLeaveNotify()<br>
	 * @param player
	 * @param clan
	 * @return
	 */
	public boolean onClanLeaderChange(L2PcInstance player, L2Clan clan)
	{
		return true;
	}
	
	/**
	 * Fired when an item is added to a clan warehouse<br>
	 * Register the listener with addClanWarehouseNotify(L2Clan)
	 * @param process
	 * @param item
	 * @param actor
	 * @return
	 */
	public boolean onClanWarehouseAddItem(String process, L2ItemInstance item, L2PcInstance actor)
	{
		return true;
	}
	
	/**
	 * Fired when an item is deleted from a clan warehouse<br>
	 * Register the listener with addClanWarehouseNotify(L2Clan)
	 * @param process
	 * @param item
	 * @param count
	 * @param actor
	 * @return
	 */
	public boolean onClanWarehouseDeleteItem(String process, L2ItemInstance item, long count, L2PcInstance actor)
	{
		return true;
	}
	
	/**
	 * Fired when an item is transfered from/to a clan warehouse<br>
	 * Register the listener with addClanWarehouseNotify(L2Clan)
	 * @param process
	 * @param item
	 * @param count
	 * @param target
	 * @param actor
	 * @return
	 */
	public boolean onClanWarehouseTransferItem(String process, L2ItemInstance item, long count, ItemContainer target, L2PcInstance actor)
	{
		return true;
	}
	
	/**
	 * Fired when a clan war starts or ends<br>
	 * Register the listener witn addClanWarNotify()
	 * @param clan1
	 * @param clan2
	 * @param stage
	 * @return
	 */
	public boolean onClanWarEvent(L2Clan clan1, L2Clan clan2, EventStage stage)
	{
		return true;
	}
	
	/**
	 * Fired when a fort siege starts or ends<br>
	 * Register using addFortSiegeNotify()
	 * @param fortSiege
	 * @param stage
	 * @return
	 */
	public boolean onFortSiegeEvent(FortSiege fortSiege, EventStage stage)
	{
		return true;
	}
	
	/**
	 * Fired when a castle siege starts or ends<br>
	 * Register using addSiegeNotify()
	 * @param siege
	 * @param stage
	 * @return
	 */
	public boolean onSiegeEvent(Siege siege, EventStage stage)
	{
		return true;
	}
	
	/**
	 * Fired when the control of a castle changes during a siege<br>
	 * Register using addSiegeNotify()
	 * @param siege
	 */
	public void onCastleControlChange(Siege siege)
	{
		
	}
	
	/**
	 * Notifies of TvT events<br>
	 * Register using addTvtNotify()
	 * @param stage
	 */
	public void onTvtEvent(EventStage stage)
	{
		
	}
	
	/**
	 * Notifies that a player was killed during TvT<br>
	 * Register using addTvtNotify()
	 * @param killed
	 * @param killer
	 * @param killerTeam
	 */
	public void onTvtKill(L2PcInstance killed, L2PcInstance killer, TvTEventTeam killerTeam)
	{
		
	}
	
	/**
	 * triggered when an item is augmented or when the augmentation is removed<br>
	 * Register using addItemAugmentNotify()
	 * @param item
	 * @param augmentation
	 * @param augment -> false = remove augment
	 * @return
	 */
	public boolean onItemAugment(L2ItemInstance item, L2Augmentation augmentation, boolean augment)
	{
		return true;
	}
	
	/**
	 * Fired when an item is dropped by a player<br>
	 * Register using addItemDropPickupNotify()
	 * @param item
	 * @param dropper
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	public boolean onItemDrop(L2ItemInstance item, L2PcInstance dropper, int x, int y, int z)
	{
		return true;
	}
	
	/**
	 * Fired when an item is picked up by a player<br>
	 * Register using addItemDropPickupNotify()
	 * @param item
	 * @param dropper
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	public boolean onItemPickup(L2ItemInstance item, L2PcInstance dropper, int x, int y, int z)
	{
		return true;
	}
	
	/**
	 * Fired when an item is equipped or unequipped<br>
	 * Register using addEquipmentNotify()
	 * @param player
	 * @param item
	 * @param isEquipped
	 * @return
	 */
	public boolean onItemEquip(L2PcInstance player, L2ItemInstance item, boolean isEquipped)
	{
		return true;
	}
	
	/**
	 * Fired when a player's level changes<br>
	 * Register using addPlayerLevelNotify(player)
	 * @param player
	 * @param oldLevel
	 * @param newLevel
	 */
	public void onPlayerLevelChange(L2PcInstance player, int oldLevel, int newLevel)
	{
		
	}
	
	/**
	 * Fired when a player changes profession<br>
	 * Register using addProfessionChangeNotify(player)
	 * @param player
	 * @param isSubClass
	 * @param template
	 */
	public void onProfessionChange(L2PcInstance player, boolean isSubClass, L2PcTemplate template)
	{
		
	}
	
	/**
	 * Fired when a player's henna changes (add/remove)<br>
	 * Register using addHennaNotify()
	 * @param player
	 * @param henna
	 * @param add -> false = remove
	 * @return
	 */
	public boolean onHennaModify(L2PcInstance player, L2HennaInstance henna, boolean add)
	{
		return true;
	}
	
	/**
	 * Fired when an item on the item tracker list has an event<br>
	 * Register using addItemTracker(itemIds)
	 * @param item
	 * @param player
	 * @param target
	 * @param event
	 */
	public void onItemTrackerEvent(L2ItemInstance item, L2PcInstance player, ItemContainer target, ItemTrackerEvent event)
	{
		
	}
	
	/**
	 * Fired when an item is created<br>
	 * Register using addNewItemNotify(itemIds)
	 * @param itemId
	 * @param player
	 * @return
	 */
	public boolean onItemCreate(int itemId, L2PcInstance player)
	{
		return true;
	}
	
	/**
	 * Fired when a player transforms/untransforms<br>
	 * Register using addTransformNotify(player)
	 * @param player
	 * @param transformation
	 * @param isTransforming -> false = untransform
	 * @return
	 */
	public boolean onPlayerTransform(L2PcInstance player, L2Transformation transformation, boolean isTransforming)
	{
		return true;
	}
	
	/**
	 * Allows for custom chat filtering<br>
	 * Fired each time a player writes something in any form of chat<br>
	 * Register using addPlayerChatFilter()
	 * @param text
	 * @param origin
	 * @param targetType
	 * @return
	 */
	public String filterChat(String text, L2PcInstance origin, ChatTargetType targetType)
	{
		return "";
	}
	
	/**
	 * Fired when a player writes some text in chat<br>
	 * Register using addPlayerTalkNotify()
	 * @param text
	 * @param origin
	 * @param target
	 * @param targetType
	 */
	public void onPlayerTalk(String text, L2PcInstance origin, String target, ChatTargetType targetType)
	{
		
	}
	
	// Enums
	
	public enum EventStage
	{
		start,
		end,
		event_stopped,
		registration_begin
	}
	
	public enum ItemTrackerEvent
	{
		drop,
		add_to_inventory,
		destroy,
		transfer
	}
}
