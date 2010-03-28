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
package com.l2jserver.gameserver.model;

import com.l2jserver.Config;
import com.l2jserver.gameserver.datatables.ItemTable;
import com.l2jserver.gameserver.instancemanager.TerritoryWarManager;
import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.InventoryUpdate;
import com.l2jserver.gameserver.network.serverpackets.ItemList;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;

public class TerritoryWard
{
	//private static final Logger _log = Logger.getLogger(CombatFlag.class.getName());

	protected L2PcInstance _player = null;
	public int playerId = 0;
	private L2ItemInstance _item = null;
	private L2Npc _npc = null;

	private Location _location;
	public L2ItemInstance itemInstance;
	
	private int _itemId;
	private int _ownerCastleId;
	
	@SuppressWarnings("unused")
	private int _heading;
	private int _territoryId;
	
	// =========================================================
	// Constructor
	public TerritoryWard(int territory_id, int x, int y, int z, int heading, int item_id, int castleId, L2Npc npc)
	{
		_territoryId = territory_id;
		_location = new Location(x,y,z,heading);
		_heading = heading;
		_itemId = item_id;
		_ownerCastleId = castleId;
		_npc = npc;
	}

	public int getTerritoryId()
	{
		return _territoryId;
	}
	
	public int getOwnerCastleId()
	{
		return _ownerCastleId;
	}
	
	public void setOwnerCastleId(int newOwner)
	{
		_ownerCastleId = newOwner;
	}
	
	public L2Npc getNpc()
	{
		return _npc;
	}
	
	public void setNpc(L2Npc npc)
	{
		_npc = npc;
	}
	
	public L2PcInstance getPlayer()
	{
		return _player;
	}
	
	public L2ItemInstance getItemInstance()
	{
		return itemInstance;
	}
	
	public synchronized void spawnMe()
	{
		if ( _player != null )
			dropIt();
		L2ItemInstance i;
		
		// Init the dropped L2ItemInstance and add it in the world as a visible object at the position where mob was last
		i = ItemTable.getInstance().createItem("Combat", _itemId, 1, null, null);
		i.spawnMe(_location.getX(), _location.getY(), _location.getZ());
		itemInstance = i;
		_npc = null;
	}

	public synchronized void unSpawnMe()
	{
		if ( _player != null )
			dropIt();
		
		if ( itemInstance != null )
		{
			itemInstance.decayMe();
		}
	}
	
	public boolean activate(L2PcInstance player, L2ItemInstance item)
	{
		if (player.isMounted())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.CANNOT_EQUIP_ITEM_DUE_TO_BAD_CONDITION));
			player.destroyItem("CombatFlag", item, null, true);
			spawnMe();
			return false;
		}
		else if (TerritoryWarManager.getInstance().getRegisteredTerritoryId(player) == 0)
		{
			player.sendMessage("Non participants can't pickup Territory Wards!");
			player.destroyItem("CombatFlag", item, null, true);
			spawnMe();
			return false;
		}

		// Player holding it data
		_player = player;
		playerId = _player.getObjectId();
		itemInstance = null;
		_npc = null;

		// Equip with the weapon
		if (item == null)
			_item = ItemTable.getInstance().createItem("Combat", _itemId, 1, null, null);
		else
			_item = item;
		_player.getInventory().equipItemAndRecord(_item);
		SystemMessage sm = new SystemMessage(SystemMessageId.S1_EQUIPPED);
		sm.addItemName(_item);
		_player.sendPacket(sm);

		// Refresh inventory
		if (!Config.FORCE_INVENTORY_UPDATE)
		{
			InventoryUpdate iu = new InventoryUpdate();
			iu.addItem(_item);
			_player.sendPacket(iu);
		}
		else _player.sendPacket(new ItemList(_player, false));
		
		// Refresh player stats
		_player.broadcastUserInfo();
		_player.setCombatFlagEquipped(true);
		_player.sendPacket(new SystemMessage(SystemMessageId.YOU_VE_ACQUIRED_THE_WARD));
		TerritoryWarManager.getInstance().giveTWPoint(player, _territoryId, 5);
		return true;
	}

	public void dropIt()
	{
		// Reset player stats
		_player.setCombatFlagEquipped(false);
		int slot = _player.getInventory().getSlotFromItem(_item);
		_player.getInventory().unEquipItemInBodySlotAndRecord(slot);
		_player.destroyItem("CombatFlag", _item, null, true);
		_item = null;
		_player.broadcastUserInfo();
		_location = new Location(_player.getX(),_player.getY(),_player.getZ(),_player.getHeading());
		_player = null;
		playerId = 0;
	}
}
