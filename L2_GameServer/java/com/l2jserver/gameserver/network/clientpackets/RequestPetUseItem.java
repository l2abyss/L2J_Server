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
import com.l2jserver.gameserver.datatables.PetDataTable;
import com.l2jserver.gameserver.handler.IItemHandler;
import com.l2jserver.gameserver.handler.ItemHandler;
import com.l2jserver.gameserver.model.L2ItemInstance;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.actor.instance.L2PetInstance;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.PetItemList;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;

public final class RequestPetUseItem extends L2GameClientPacket
{
	private static Logger _log = Logger.getLogger(RequestPetUseItem.class.getName());
	private static final String _C__8A_REQUESTPETUSEITEM = "[C] 8a RequestPetUseItem";
	
	private int _objectId;
	
	@Override
	protected void readImpl()
	{
		_objectId = readD();
		// todo implement me properly
		//readQ();
		//readD();
	}
	
	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;
		
		L2PetInstance pet = (L2PetInstance)activeChar.getPet();
		if (pet == null)
			return;
		
		if (!getClient().getFloodProtectors().getUseItem().tryPerformAction("pet use item"))
			return;
		
		L2ItemInstance item = pet.getInventory().getItemByObjectId(_objectId);
		if (item == null)
			return;
		
		int itemId = item.getItemId();
		
		if (activeChar.isAlikeDead() || pet.isDead())
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
			sm.addItemName(item);
			activeChar.sendPacket(sm);
			sm = null;
			return;
		}
		
		if (Config.DEBUG)
			_log.finest(activeChar.getObjectId()+": pet use item " + _objectId);
		
		if (!item.isEquipped())
		{
			if (!item.getItem().checkCondition(pet, pet, true))
			{
				return;
			}
		}
		
		//check if the item matches the pet
		if (item.isEquipable())
		{
			// all pet items have condition
			if (!item.getItem().isConditionAttached())
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.PET_CANNOT_USE_ITEM));
				return;
			}
			useItem(pet, item, activeChar);
			return;
		}
		else if (PetDataTable.isPetFood(itemId))
		{
			if (pet.canEatFoodId(itemId))
				useItem(pet, item, activeChar);
			else
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.PET_CANNOT_USE_ITEM));
				return;
			}
		}
		
		IItemHandler handler = ItemHandler.getInstance().getItemHandler(item.getEtcItem());
		if (handler != null)
			useItem(pet, item, activeChar);
		else
			activeChar.sendPacket(new SystemMessage(SystemMessageId.PET_CANNOT_USE_ITEM));
		
		return;
	}
	
	private void useItem(L2PetInstance pet, L2ItemInstance item, L2PcInstance activeChar)
	{
		if (item.isEquipable())
		{
			if (item.isEquipped())
			{
				pet.getInventory().unEquipItemInSlot(item.getLocationSlot());
			}
			else
			{
				pet.getInventory().equipItem(item);
			}
			
			activeChar.sendPacket(new PetItemList(pet));
			pet.updateAndBroadcastStatus(1);
		}
		else
		{
			IItemHandler handler = ItemHandler.getInstance().getItemHandler(item.getEtcItem());
			if (handler != null)
			{
				handler.useItem(pet, item, false);
				pet.updateAndBroadcastStatus(1);
			}
			else
				_log.warning("no itemhandler registered for itemId:" + item.getItemId());
		}
	}
	
	/* (non-Javadoc)
	 * @see com.l2jserver.gameserver.clientpackets.ClientBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _C__8A_REQUESTPETUSEITEM;
	}
}
