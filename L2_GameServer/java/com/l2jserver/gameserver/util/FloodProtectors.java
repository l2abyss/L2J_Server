/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package com.l2jserver.gameserver.util;

import com.l2jserver.Config;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;

/**
 * Collection of flood protectors for single player.
 * 
 * @author fordfrog
 */
public final class FloodProtectors
{
	
	/**
	 * Use-item flood protector.
	 */
	private final FloodProtectorAction _useItem;
	/**
	 * Roll-dice flood protector.
	 */
	private final FloodProtectorAction _rollDice;
	/**
	 * Firework flood protector.
	 */
	private final FloodProtectorAction _firework;
	/**
	 * Item-pet-summon flood protector.
	 */
	private final FloodProtectorAction _itemPetSummon;
	/**
	 * Hero-voice flood protector.
	 */
	private final FloodProtectorAction _heroVoice;
	/**
	 * Global-chat flood protector.
	 */
	private final FloodProtectorAction _globalChat;
	/**
	 * Subclass flood protector.
	 */
	private final FloodProtectorAction _subclass;
	/**
	 * Drop-item flood protector.
	 */
	private final FloodProtectorAction _dropItem;
	/**
	 * Server-bypass flood protector.
	 */
	private final FloodProtectorAction _serverBypass;
	/**
	 * Multisell flood protector.
	 */
	private final FloodProtectorAction _multiSell;
	/**
	 * Transaction flood protector.
	 */
	private final FloodProtectorAction _transaction;
	/**
	 * Send mail flood protector.
	 */
	private final FloodProtectorAction _sendMail;

	/**
	 * Creates new instance of FloodProtectors.
	 * 
	 * @param player
	 *            player for which the collection of flood protectors is being created.
	 */
	public FloodProtectors(final L2PcInstance player)
	{
		super();
		_useItem = new FloodProtectorAction(player, Config.FLOOD_PROTECTOR_USE_ITEM);
		_rollDice = new FloodProtectorAction(player, Config.FLOOD_PROTECTOR_ROLL_DICE);
		_firework = new FloodProtectorAction(player, Config.FLOOD_PROTECTOR_FIREWORK);
		_itemPetSummon = new FloodProtectorAction(player, Config.FLOOD_PROTECTOR_ITEM_PET_SUMMON);
		_heroVoice = new FloodProtectorAction(player, Config.FLOOD_PROTECTOR_HERO_VOICE);
		_globalChat = new FloodProtectorAction(player, Config.FLOOD_PROTECTOR_GLOBAL_CHAT);
		_subclass = new FloodProtectorAction(player, Config.FLOOD_PROTECTOR_SUBCLASS);
		_dropItem = new FloodProtectorAction(player, Config.FLOOD_PROTECTOR_DROP_ITEM);
		_serverBypass = new FloodProtectorAction(player, Config.FLOOD_PROTECTOR_SERVER_BYPASS);
		_multiSell = new FloodProtectorAction(player, Config.FLOOD_PROTECTOR_MULTISELL);
		_transaction = new FloodProtectorAction(player, Config.FLOOD_PROTECTOR_TRANSACTION);
		_sendMail = new FloodProtectorAction(player, Config.FLOOD_PROTECTOR_SENDMAIL);
	}
	
	/**
	 * Returns {@link #_useItem}.
	 * 
	 * @return {@link #_useItem}
	 */
	public FloodProtectorAction getUseItem()
	{
		return _useItem;
	}
	
	/**
	 * Returns {@link #_rollDice}.
	 * 
	 * @return {@link #_rollDice}
	 */
	public FloodProtectorAction getRollDice()
	{
		return _rollDice;
	}
	
	/**
	 * Returns {@link #_firework}.
	 * 
	 * @return {@link #_firework}
	 */
	public FloodProtectorAction getFirework()
	{
		return _firework;
	}
	
	/**
	 * Returns {@link #_itemPetSummon}.
	 * 
	 * @return {@link #_itemPetSummon}
	 */
	public FloodProtectorAction getItemPetSummon()
	{
		return _itemPetSummon;
	}
	
	/**
	 * Returns {@link #_heroVoice}.
	 * 
	 * @return {@link #_heroVoice}
	 */
	public FloodProtectorAction getHeroVoice()
	{
		return _heroVoice;
	}
	
	/**
	 * Returns {@link #_globalChat}.
	 * 
	 * @return {@link #_globalChat}
	 */
	public FloodProtectorAction getGlobalChat()
	{
		return _globalChat;
	}
	
	/**
	 * Returns {@link #_subclass}.
	 * 
	 * @return {@link #_subclass}
	 */
	public FloodProtectorAction getSubclass()
	{
		return _subclass;
	}
	
	/**
	 * Returns {@link #_dropItem}.
	 * 
	 * @return {@link #_dropItem}
	 */
	public FloodProtectorAction getDropItem()
	{
		return _dropItem;
	}
	
	/**
	 * Returns {@link #_serverBypass}.
	 * 
	 * @return {@link #_serverBypass}
	 */
	public FloodProtectorAction getServerBypass()
	{
		return _serverBypass;
	}

	/**
	 * Returns {@link #_multisell}.
	 * 
	 * @return {@link #_multisell}
	 */
	public FloodProtectorAction getMultiSell()
	{
		return _multiSell;
	}

	/**
	 * Returns {@link #_transaction}.
	 * 
	 * @return {@link #_transaction}
	 */
	public FloodProtectorAction getTransaction()
	{
		return _transaction;
	}

	/**
	 * Returns {@link #_sendMail}.
	 * 
	 * @return {@link #_sendMail}
	 */
	public FloodProtectorAction getSendMail()
	{
		return _sendMail;
	}
}
