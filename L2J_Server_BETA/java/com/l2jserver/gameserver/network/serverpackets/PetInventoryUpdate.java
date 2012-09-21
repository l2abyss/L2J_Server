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
package com.l2jserver.gameserver.network.serverpackets;

import java.util.ArrayList;
import java.util.List;

import com.l2jserver.gameserver.model.ItemInfo;
import com.l2jserver.gameserver.model.items.instance.L2ItemInstance;

/**
 * @author Yme, Advi
 */
public class PetInventoryUpdate extends L2GameServerPacket
{
	private final List<ItemInfo> _items;
	
	/**
	 * @param items
	 */
	public PetInventoryUpdate(List<ItemInfo> items)
	{
		_items = items;
	}
	
	public PetInventoryUpdate()
	{
		this(new ArrayList<ItemInfo>());
	}
	
	public void addItem(L2ItemInstance item)
	{
		_items.add(new ItemInfo(item));
	}
	
	public void addNewItem(L2ItemInstance item)
	{
		_items.add(new ItemInfo(item, 1));
	}
	
	public void addModifiedItem(L2ItemInstance item)
	{
		_items.add(new ItemInfo(item, 2));
	}
	
	public void addRemovedItem(L2ItemInstance item)
	{
		_items.add(new ItemInfo(item, 3));
	}
	
	public void addItems(List<L2ItemInstance> items)
	{
		for (L2ItemInstance item : items)
		{
			_items.add(new ItemInfo(item));
		}
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0xB4);
		int count = _items.size();
		writeH(count);
		for (ItemInfo item : _items)
		{
			writeH(item.getChange()); // Update type : 01-add, 02-modify, 03-remove
			writeD(item.getObjectId());
			writeD(item.getItem().getDisplayId());
			writeD(item.getLocation());
			writeQ(item.getCount());
			writeH(item.getItem().getType2());
			writeH(item.getCustomType1());
			writeH(item.getEquipped());
			writeD(item.getItem().getBodyPart());
			writeH(item.getEnchant());
			writeH(item.getCustomType2());
			writeD(item.getAugmentationBonus());
			writeD(item.getMana());
			writeD(item.getTime());
			writeH(item.getAttackElementType());
			writeH(item.getAttackElementPower());
			for (byte i = 0; i < 6; i++)
			{
				writeH(item.getElementDefAttr(i));
			}
			// Enchant Effects
			for (int op : item.getEnchantOptions())
			{
				writeH(op);
			}
		}
	}
}
