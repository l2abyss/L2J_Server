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
package com.l2jserver.gameserver.network.clientpackets;

import java.util.logging.Logger;

import com.l2jserver.Config;
import com.l2jserver.gameserver.GameTimeController;
import com.l2jserver.gameserver.ThreadPoolManager;
import com.l2jserver.gameserver.handler.IItemHandler;
import com.l2jserver.gameserver.handler.ItemHandler;
import com.l2jserver.gameserver.instancemanager.FortSiegeManager;
import com.l2jserver.gameserver.model.L2ItemInstance;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.base.Race;
import com.l2jserver.gameserver.model.itemcontainer.Inventory;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.ActionFailed;
import com.l2jserver.gameserver.network.serverpackets.ItemList;
import com.l2jserver.gameserver.network.serverpackets.ShowCalculator;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;
import com.l2jserver.gameserver.templates.item.L2ArmorType;
import com.l2jserver.gameserver.templates.item.L2Item;
import com.l2jserver.gameserver.templates.item.L2Weapon;
import com.l2jserver.gameserver.templates.item.L2WeaponType;


/**
 * This class ...
 *
 * @version $Revision: 1.18.2.7.2.9 $ $Date: 2005/03/27 15:29:30 $
 */
public final class UseItem extends L2GameClientPacket
{
	private static Logger _log = Logger.getLogger(UseItem.class.getName());
	private static final String _C__14_USEITEM = "[C] 14 UseItem";
	
	private int _objectId;
	private boolean _ctrlPressed;
	private int _itemId;
	
	/** Weapon Equip Task */
	public static class WeaponEquipTask implements Runnable
	{
		L2ItemInstance item;
		L2PcInstance activeChar;
		public WeaponEquipTask(L2ItemInstance it, L2PcInstance character){
			item = it;
			activeChar = character;
		}
		public void run()
		{
			//If character is still engaged in strike we should not change weapon
			if (activeChar.isAttackingNow())
				return;
			// Equip or unEquip
			activeChar.useEquippableItem(item, false);
		}
	}
	
	@Override
	protected void readImpl()
	{
		_objectId = readD();
		_ctrlPressed = readD() != 0;
	}
	
	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;
		
		// Flood protect UseItem
		if (!getClient().getFloodProtectors().getUseItem().tryPerformAction("use item"))
			return;
		
		if (activeChar.getPrivateStoreType() != 0)
		{
			activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CANNOT_TRADE_DISCARD_DROP_ITEM_WHILE_IN_SHOPMODE));
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (activeChar.getActiveTradeList() != null)
			activeChar.cancelActiveTrade();
		
		// cannot use items during Fear (possible more abnormal states?)
		if (activeChar.isAfraid())
		{
			// no sysmsg
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// NOTE: disabled due to deadlocks
		// synchronized (activeChar.getInventory())
		// 	{
		L2ItemInstance item = activeChar.getInventory().getItemByObjectId(_objectId);
		if (item == null)
			return;
		
		if (item.getItem().getType2() == L2Item.TYPE2_QUEST)
		{
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.CANNOT_USE_QUEST_ITEMS);
			activeChar.sendPacket(sm);
			sm = null;
			return;
		}
		
		_itemId = item.getItemId();
		/*
		 * Alt game - Karma punishment // SOE
		 * 736  	Scroll of Escape
		 * 1538  	Blessed Scroll of Escape
		 * 1829  	Scroll of Escape: Clan Hall
		 * 1830  	Scroll of Escape: Castle
		 * 3958  	L2Day - Blessed Scroll of Escape
		 * 5858  	Blessed Scroll of Escape: Clan Hall
		 * 5859  	Blessed Scroll of Escape: Castle
		 * 6663  	Scroll of Escape: Orc Village
		 * 6664  	Scroll of Escape: Silenos Village
		 * 7117  	Scroll of Escape to Talking Island
		 * 7118  	Scroll of Escape to Elven Village
		 * 7119  	Scroll of Escape to Dark Elf Village
		 * 7120  	Scroll of Escape to Orc Village
		 * 7121  	Scroll of Escape to Dwarven Village
		 * 7122  	Scroll of Escape to Gludin Village
		 * 7123  	Scroll of Escape to the Town of Gludio
		 * 7124  	Scroll of Escape to the Town of Dion
		 * 7125  	Scroll of Escape to Floran
		 * 7126  	Scroll of Escape to Giran Castle Town
		 * 7127  	Scroll of Escape to Hardin's Private Academy
		 * 7128  	Scroll of Escape to Heine
		 * 7129  	Scroll of Escape to the Town of Oren
		 * 7130  	Scroll of Escape to Ivory Tower
		 * 7131  	Scroll of Escape to Hunters Village
		 * 7132  	Scroll of Escape to Aden Castle Town
		 * 7133  	Scroll of Escape to the Town of Goddard
		 * 7134  	Scroll of Escape to the Rune Township
		 * 7135  	Scroll of Escape to the Town of Schuttgart.
		 * 7554  	Scroll of Escape to Talking Island
		 * 7555  	Scroll of Escape to Elven Village
		 * 7556  	Scroll of Escape to Dark Elf Village
		 * 7557  	Scroll of Escape to Orc Village
		 * 7558  	Scroll of Escape to Dwarven Village
		 * 7559  	Scroll of Escape to Giran Castle Town
		 * 7618  	Scroll of Escape - Ketra Orc Village
		 * 7619  	Scroll of Escape - Varka Silenos Village
		 * 10129    Scroll of Escape : Fortress
		 * 10130    Blessed Scroll of Escape : Fortress
		 */
		if (!Config.ALT_GAME_KARMA_PLAYER_CAN_TELEPORT && activeChar.getKarma() > 0)
		{
			switch (_itemId)
			{
				case 736: case 1538: case 1829: case 1830: case 3958: case 5858:
				case 5859: case 6663: case 6664: case 7554: case 7555: case 7556:
				case 7557: case 7558: case 7559: case 7618: case 7619: case 10129:
				case 10130:
					return;
			}
			
			if (_itemId >= 7117 && _itemId <= 7135)
				return;
		}
		
		if (activeChar.isFishing() && (_itemId < 6535 || _itemId > 6540))
		{
			// You cannot do anything else while fishing
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.CANNOT_DO_WHILE_FISHING_3);
			getClient().getActiveChar().sendPacket(sm);
			sm = null;
			return;
		}
		
		// Char cannot use item when dead
		if (activeChar.isDead())
		{
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
			sm.addItemName(item);
			getClient().getActiveChar().sendPacket(sm);
			sm = null;
			return;
		}
		
		// No UseItem is allowed while the player is in special conditions
		if (activeChar.isStunned()
				|| activeChar.isSleeping()
				|| activeChar.isParalyzed()
				|| activeChar.isAlikeDead()
				|| activeChar.isAfraid()
				|| activeChar.isCastingNow())
		{
			return;
		}
		
		// Char cannot use pet items
		/*if ((item.getItem() instanceof L2Armor && item.getItem().getItemType() == L2ArmorType.PET)
				|| (item.getItem() instanceof L2Weapon && item.getItem().getItemType() == L2WeaponType.PET) )
		{
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.CANNOT_EQUIP_PET_ITEM); // You cannot equip a pet item.
			sm.addItemName(item);
			getClient().getActiveChar().sendPacket(sm);
			sm = null;
			return;
		}*/
		
		if (!activeChar.getInventory().canManipulateWithItemId(item.getItemId()))
		{
			activeChar.sendMessage("Cannot use this item.");
			return;
		}
		
		if (Config.DEBUG)
			_log.finest(activeChar.getObjectId() + ": use item " + _objectId);
		
		if (!item.isEquipped())
		{
			if (!item.getItem().checkCondition(activeChar, activeChar, true))
			{
				return;
			}
		}
		
		if (item.isEquipable())
		{
			// Don't allow hero equipment and restricted items during Olympiad
			if (activeChar.isInOlympiadMode() && (item.isHeroItem() || item.isOlyRestrictedItem()))
			{
				activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.THIS_ITEM_CANT_BE_EQUIPPED_FOR_THE_OLYMPIAD_EVENT));
				return;
			}
			
			switch (item.getItem().getBodyPart())
			{
				case L2Item.SLOT_LR_HAND:
				case L2Item.SLOT_L_HAND:
				case L2Item.SLOT_R_HAND:
				{
					// prevent players to equip weapon while wearing combat flag
					if (activeChar.getActiveWeaponItem() != null && activeChar.getActiveWeaponItem().getItemId() == 9819)
					{
						activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CANNOT_EQUIP_ITEM_DUE_TO_BAD_CONDITION));
						return;
					}
					// Prevent player to remove the weapon on special conditions
					if (activeChar.isCastingNow() || activeChar.isCastingSimultaneouslyNow())
					{
						activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CANNOT_USE_ITEM_WHILE_USING_MAGIC));
						return;
					}
					if (activeChar.isMounted())
					{
						activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CANNOT_EQUIP_ITEM_DUE_TO_BAD_CONDITION));
						return;
					}
					if (activeChar.isDisarmed())
					{
						activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CANNOT_EQUIP_ITEM_DUE_TO_BAD_CONDITION));
						return;
					}
					
					// Don't allow weapon/shield equipment if a cursed weapon is equiped
					if (activeChar.isCursedWeaponEquipped())
						return;
					
					// Don't allow other Race to Wear Kamael exclusive Weapons.
					if (!item.isEquipped() && item.getItem() instanceof L2Weapon && !activeChar.isGM())
					{
						L2Weapon wpn = (L2Weapon)item.getItem();
						
						switch (activeChar.getRace())
						{
							case Kamael:
							{
								switch (wpn.getItemType())
								{
									case NONE:
										activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CANNOT_EQUIP_ITEM_DUE_TO_BAD_CONDITION));
										return;
								}
								break;
							}
							case Human:
							case Dwarf:
							case Elf:
							case DarkElf:
							case Orc:
							{
								switch (wpn.getItemType())
								{
									case RAPIER:
									case CROSSBOW:
									case ANCIENTSWORD:
										activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CANNOT_EQUIP_ITEM_DUE_TO_BAD_CONDITION));
										return;
								}
								break;
							}
						}
					}
					break;
				}
				case L2Item.SLOT_CHEST:
				case L2Item.SLOT_BACK:
				case L2Item.SLOT_GLOVES:
				case L2Item.SLOT_FEET:
				case L2Item.SLOT_HEAD:
				case L2Item.SLOT_FULL_ARMOR:
				case L2Item.SLOT_LEGS:
				{
					if (activeChar.getRace() == Race.Kamael &&
							(item.getItem().getItemType() == L2ArmorType.HEAVY
									||item.getItem().getItemType() == L2ArmorType.MAGIC))
					{
						activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CANNOT_EQUIP_ITEM_DUE_TO_BAD_CONDITION));
						return;
					}
					break;
				}
				case L2Item.SLOT_DECO:
				{
					if (!item.isEquipped() && activeChar.getInventory().getMaxTalismanCount() == 0)
					{
						activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CANNOT_EQUIP_ITEM_DUE_TO_BAD_CONDITION));
						return;
					}
				}
			}
			
			if (activeChar.isCursedWeaponEquipped() && _itemId == 6408) // Don't allow to put formal wear
				return;
			
			if (activeChar.isAttackingNow())
			{
				ThreadPoolManager.getInstance().scheduleGeneral( new WeaponEquipTask(item,activeChar), (activeChar.getAttackEndTime()-GameTimeController.getGameTicks())*GameTimeController.MILLIS_IN_TICK);
				return;
			}
			// Equip or unEquip
			if (FortSiegeManager.getInstance().isCombat(item.getItemId()))
				return;	//no message
			else if (activeChar.isCombatFlagEquipped())
				return;
			
			activeChar.useEquippableItem(item, true);
		}
		else
		{
			L2Weapon weaponItem = activeChar.getActiveWeaponItem();
			int itemid = item.getItemId();
			if (itemid == 4393)
			{
				activeChar.sendPacket(new ShowCalculator(4393));
			}
			else if ((weaponItem != null && weaponItem.getItemType() == L2WeaponType.FISHINGROD)
					&& ((itemid >= 6519 && itemid <= 6527) || (itemid >= 7610 && itemid <= 7613) || (itemid >= 7807 && itemid <= 7809) || (itemid >= 8484 && itemid <= 8486) || (itemid >= 8505 && itemid <= 8513)))
			{
				activeChar.getInventory().setPaperdollItem(Inventory.PAPERDOLL_LHAND, item);
				activeChar.broadcastUserInfo();
				// Send a Server->Client packet ItemList to this L2PcINstance to update left hand equipement
				ItemList il = new ItemList(activeChar, false);
				sendPacket(il);
				return;
			}
			else
			{
				IItemHandler handler = ItemHandler.getInstance().getItemHandler(item.getEtcItem());
				if (handler == null)
				{
					if (Config.DEBUG)
						_log.warning("No item handler registered for item ID " + item.getItemId() + ".");
				}
				else
					handler.useItem(activeChar, item, _ctrlPressed);
			}
		}
		//		}
	}
	
	@Override
	public String getType()
	{
		return _C__14_USEITEM;
	}
	
	@Override
	protected boolean triggersOnActionRequest()
	{
		return !Config.SPAWN_PROTECTION_ALLOWED_ITEMS.contains(_itemId);
	}
}
