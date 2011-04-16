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

import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2jserver.Config;
import com.l2jserver.gameserver.GameTimeController;
import com.l2jserver.gameserver.ThreadPoolManager;
import com.l2jserver.gameserver.handler.IItemHandler;
import com.l2jserver.gameserver.handler.ItemHandler;
import com.l2jserver.gameserver.instancemanager.FortSiegeManager;
import com.l2jserver.gameserver.model.L2ItemInstance;
import com.l2jserver.gameserver.model.L2Skill;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.base.Race;
import com.l2jserver.gameserver.model.itemcontainer.Inventory;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.ActionFailed;
import com.l2jserver.gameserver.network.serverpackets.ItemList;
import com.l2jserver.gameserver.network.serverpackets.ShowCalculator;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;
import com.l2jserver.gameserver.skills.SkillHolder;
import com.l2jserver.gameserver.templates.item.L2ArmorType;
import com.l2jserver.gameserver.templates.item.L2Item;
import com.l2jserver.gameserver.templates.item.L2Weapon;
import com.l2jserver.gameserver.templates.item.L2WeaponType;
import com.l2jserver.gameserver.templates.skills.L2SkillType;


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
		
		public WeaponEquipTask(L2ItemInstance it, L2PcInstance character)
		{
			item = it;
			activeChar = character;
		}
		
		@Override
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
		
		if (activeChar.getActiveTradeList() != null)
			activeChar.cancelActiveTrade();
		
		if (activeChar.getPrivateStoreType() != 0)
		{
			activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CANNOT_TRADE_DISCARD_DROP_ITEM_WHILE_IN_SHOPMODE));
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
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
		
		// No UseItem is allowed while the player is in special conditions
		if (activeChar.isStunned()
				|| activeChar.isParalyzed()
				|| activeChar.isSleeping()
				|| activeChar.isAfraid()
				|| activeChar.isAlikeDead())
		{
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
		
		if (!item.isEquipped() && !item.getItem().checkCondition(activeChar, activeChar, true))
			return;
		
		if (!activeChar.getInventory().canManipulateWithItemId(item.getItemId()))
		{
			activeChar.sendMessage("Cannot use this item.");
			return;
		}
		
		_itemId = item.getItemId();
		
		if (activeChar.isFishing() && (_itemId < 6535 || _itemId > 6540))
		{
			// You cannot do anything else while fishing
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.CANNOT_DO_WHILE_FISHING_3);
			getClient().getActiveChar().sendPacket(sm);
			sm = null;
			return;
		}

		if (!Config.ALT_GAME_KARMA_PLAYER_CAN_TELEPORT && activeChar.getKarma() > 0)
		{
			SkillHolder[] sHolders = item.getItem().getSkills();
			if (sHolders != null)
			{
				for (SkillHolder sHolder : sHolders)
				{
					L2Skill skill = sHolder.getSkill();
					if (skill != null && (skill.getSkillType() == L2SkillType.TELEPORT || skill.getSkillType() == L2SkillType.RECALL))
						return;
				}
			}

		}
		
		if (Config.DEBUG)
			_log.log(Level.INFO, activeChar.getObjectId() + ": use item " + _objectId);
		
		if (item.isEquipable())
		{
			// Don't allow hero equipment and restricted items during Olympiad
			if (activeChar.isInOlympiadMode() && (item.isHeroItem() || item.isOlyRestrictedItem()))
			{
				activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.THIS_ITEM_CANT_BE_EQUIPPED_FOR_THE_OLYMPIAD_EVENT));
				return;
			}
			
			if (activeChar.isCursedWeaponEquipped() && _itemId == 6408) // Don't allow to put formal wear
				return;
				
			// Equip or unEquip
			if (FortSiegeManager.getInstance().isCombat(item.getItemId()))
				return;	//no message
			else if (activeChar.isCombatFlagEquipped())
				return;
			
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
			
			if (activeChar.isAttackingNow())
			{
				ThreadPoolManager.getInstance().scheduleGeneral( new WeaponEquipTask(item,activeChar), (activeChar.getAttackEndTime()-GameTimeController.getGameTicks())*GameTimeController.MILLIS_IN_TICK);
				return;
			}
			
			activeChar.useEquippableItem(item, true);
		}
		else
		{
			if(activeChar.isCastingNow() && !(item.isPotion() || item.isElixir()))
				return;
			
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
				sendPacket(new ItemList(activeChar, false));
				return;
			}
			else
			{
				IItemHandler handler = ItemHandler.getInstance().getItemHandler(item.getEtcItem());
				if (handler != null)
					handler.useItem(activeChar, item, _ctrlPressed);
				else if (Config.DEBUG)
					_log.log(Level.WARNING, "No item handler registered for item ID " + item.getItemId() + ".");
			}
		}
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
