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
package net.sf.l2j.gameserver.network.clientpackets;

import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.GmListTable;
import net.sf.l2j.gameserver.instancemanager.CursedWeaponsManager;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.item.L2EtcItemType;
import net.sf.l2j.gameserver.templates.item.L2Item;
import net.sf.l2j.gameserver.util.GMAudit;
import net.sf.l2j.gameserver.util.Util;

/**
 * This class ...
 *
 * @version $Revision: 1.11.2.1.2.7 $ $Date: 2005/04/02 21:25:21 $
 */
public final class RequestDropItem extends L2GameClientPacket
{
	private static final String _C__12_REQUESTDROPITEM = "[C] 12 RequestDropItem";
	private static Logger _log = Logger.getLogger(RequestDropItem.class.getName());
	
	private int _objectId;
	private long _count;
	private int _x;
	private int _y;
	private int _z;
	
	@Override
	protected void readImpl()
	{
		_objectId = readD();
		_count = readQ();
		_x = readD();
		_y = readD();
		_z = readD();
	}
	
	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;
		// Flood protect drop to avoid packet lag
		if (!activeChar.getFloodProtectors().getDropItem().tryPerformAction("drop item"))
			return;
		
		L2ItemInstance item = activeChar.getInventory().getItemByObjectId(_objectId);
		
		if (item == null
				|| _count == 0
				|| !activeChar.validateItemManipulation(_objectId, "drop")
				|| (!Config.ALLOW_DISCARDITEM && !activeChar.isGM())
				|| !item.isDropable())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_DISCARD_THIS_ITEM));
			return;
		}
		if (item.getItemType() == L2EtcItemType.QUEST)
		{
			return;
		}
		int itemId = item.getItemId();
		
		// Cursed Weapons cannot be dropped
		if (CursedWeaponsManager.getInstance().isCursed(itemId))
			return;
		
		if (_count > item.getCount())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_DISCARD_THIS_ITEM));
			return;
		}
		
		if (Config.PLAYER_SPAWN_PROTECTION > 0 && activeChar.isInvul() && !activeChar.isGM())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_DISCARD_THIS_ITEM));
			return;
		}
		
		if (_count < 0)
		{
			Util.handleIllegalPlayerAction(activeChar, "[RequestDropItem] Character " + activeChar.getName() + " of account " + activeChar.getAccountName() + " tried to drop item with oid " + _objectId + " but has count < 0!", Config.DEFAULT_PUNISH);
			return;
		}
		
		if (!item.isStackable() && _count > 1)
		{
			Util.handleIllegalPlayerAction(activeChar, "[RequestDropItem] Character " + activeChar.getName() + " of account " + activeChar.getAccountName() + " tried to drop non-stackable item with oid " + _objectId + " but has count > 1!", Config.DEFAULT_PUNISH);
			return;
		}
		
		if (!activeChar.getAccessLevel().allowTransaction())
		{
			activeChar.sendMessage("Transactions are disable for your Access Level");
			activeChar.sendPacket(new SystemMessage(SystemMessageId.NOTHING_HAPPENED));
			return;
		}
		
		if (activeChar.isProcessingTransaction() || activeChar.getPrivateStoreType() != 0)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_TRADE_DISCARD_DROP_ITEM_WHILE_IN_SHOPMODE));
			return;
		}
		if (activeChar.isFishing())
		{
			//You can't mount, dismount, break and drop items while fishing
			activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_DO_WHILE_FISHING_2));
			return;
		}
		if (activeChar.isFlying())
		{
			return;
		}
		
		// Cannot discard item that the skill is consuming
		if (activeChar.isCastingNow())
		{
			if (activeChar.getCurrentSkill() != null && activeChar.getCurrentSkill().getSkill().getItemConsumeId() == item.getItemId())
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_DISCARD_THIS_ITEM));
				return;
			}
		}
		
		// Cannot discard item that the skill is consuming
		if (activeChar.isCastingSimultaneouslyNow())
		{
			if (activeChar.getLastSimultaneousSkillCast() != null && activeChar.getLastSimultaneousSkillCast().getItemConsumeId() == item.getItemId())
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_DISCARD_THIS_ITEM));
				return;
			}
		}
		
		if (L2Item.TYPE2_QUEST == item.getItem().getType2() && !activeChar.isGM())
		{
			if (Config.DEBUG)
				_log.finest(activeChar.getObjectId() + ":player tried to drop quest item");
			activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_DISCARD_EXCHANGE_ITEM));
			return;
		}
		
		if (!activeChar.isInsideRadius(_x, _y, 150, false) || Math.abs(_z - activeChar.getZ()) > 50)
		{
			if (Config.DEBUG)
				_log.finest(activeChar.getObjectId() + ": trying to drop too far away");
			activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_DISCARD_DISTANCE_TOO_FAR));
			return;
		}
		
		if (Config.DEBUG)
			_log.fine("requested drop item " + _objectId + "(" + item.getCount() + ") at " + _x + "/" + _y + "/" + _z);
		
		if (item.isEquipped())
		{
			L2ItemInstance[] unequiped = activeChar.getInventory().unEquipItemInBodySlotAndRecord(item.getItem().getBodyPart());
			InventoryUpdate iu = new InventoryUpdate();
			for (L2ItemInstance itm : unequiped)
			{
				activeChar.checkSSMatch(null, itm);
				
				iu.addModifiedItem(itm);
			}
			activeChar.sendPacket(iu);
			activeChar.broadcastUserInfo();
			
			ItemList il = new ItemList(activeChar, true);
			activeChar.sendPacket(il);
		}
		
		L2ItemInstance dropedItem = activeChar.dropItem("Drop", _objectId, _count, _x, _y, _z, null, false);
		
		if (Config.DEBUG)
			_log.fine("dropping " + _objectId + " item(" + _count + ") at: " + _x + " " + _y + " " + _z);
		
		// activeChar.broadcastUserInfo();
		
		if (activeChar.isGM())
		{
			String target = (activeChar.getTarget() != null ? activeChar.getTarget().getName() : "no-target");
			GMAudit.auditGMAction(activeChar.getName(), "Drop", target, "(id: " + dropedItem.getItemId() + " name: " + dropedItem.getItemName() + " objId: " + dropedItem.getObjectId() 
					+ " x: " + activeChar.getX() + " y: " + activeChar.getY() + " z: " + activeChar.getZ() + ")");
		}
		
		if (dropedItem != null && dropedItem.getItemId() == 57 && dropedItem.getCount() >= 1000000)
		{
			String msg = "Character (" + activeChar.getName() + ") has dropped (" + dropedItem.getCount() + ")adena at (" + _x + "," + _y + "," + _z + ")";
			_log.warning(msg);
			GmListTable.broadcastMessageToGMs(msg);
		}
	}
	
	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _C__12_REQUESTDROPITEM;
	}
}
