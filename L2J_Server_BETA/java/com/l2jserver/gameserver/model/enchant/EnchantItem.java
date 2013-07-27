/*
 * Copyright (C) 2004-2013 L2J Server
 * 
 * This file is part of L2J Server.
 * 
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.l2jserver.gameserver.model.enchant;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.l2jserver.gameserver.datatables.ItemTable;
import com.l2jserver.gameserver.model.StatsSet;
import com.l2jserver.gameserver.model.items.L2Item;
import com.l2jserver.gameserver.model.items.instance.L2ItemInstance;

/**
 * @author UnAfraid
 */
public class EnchantItem
{
	protected static final Logger _log = Logger.getLogger(EnchantItem.class.getName());
	
	private final int _id;
	private final boolean _isWeapon;
	private final int _grade;
	private final int _maxEnchantLevel;
	private final double _bonusRate;
	private List<Integer> _itemIds;
	
	public EnchantItem(StatsSet set)
	{
		_id = set.getInteger("id");
		_isWeapon = set.getBool("isWeapon", true);
		_grade = ItemTable._crystalTypes.get(set.getString("targetGrade", "none"));
		_maxEnchantLevel = set.getInteger("maxEnchant", 65535);
		_bonusRate = set.getDouble("bonusRate", 0);
	}
	
	/**
	 * @return id of current item.
	 */
	public final int getId()
	{
		return _id;
	}
	
	/**
	 * @return bonus chance that would be added.
	 */
	public final double getBonusRate()
	{
		return _bonusRate;
	}
	
	public void addItem(int id)
	{
		if (_itemIds == null)
		{
			_itemIds = new ArrayList<>();
		}
		_itemIds.add(id);
	}
	
	public boolean verifyItemId(int itemId)
	{
		return _itemIds != null ? _itemIds.contains(itemId) : true;
	}
	
	/**
	 * @param enchantItem
	 * @return {@code true} if current item is valid to be enchanted, {@code false} otherwise.
	 */
	public final boolean isValid(L2ItemInstance enchantItem)
	{
		if (enchantItem == null)
		{
			return false;
		}
		else if (enchantItem.isEnchantable() == 0)
		{
			return false;
		}
		else if (!isValidItemType(enchantItem.getItem().getType2()))
		{
			return false;
		}
		else if ((_maxEnchantLevel != 0) && (enchantItem.getEnchantLevel() >= _maxEnchantLevel))
		{
			return false;
		}
		else if (_grade != enchantItem.getItem().getItemGradeSPlus())
		{
			return false;
		}
		else if ((enchantItem.isEnchantable() > 1) && !verifyItemId(enchantItem.getItemId()))
		{
			return false;
		}
		
		return true;
	}
	
	/**
	 * @param type2
	 * @return {@code true} if current type2 is valid to be enchanted, {@code false} otherwise.
	 */
	private boolean isValidItemType(int type2)
	{
		if (type2 == L2Item.TYPE2_WEAPON)
		{
			return _isWeapon;
		}
		else if ((type2 == L2Item.TYPE2_SHIELD_ARMOR) || (type2 == L2Item.TYPE2_ACCESSORY))
		{
			return !_isWeapon;
		}
		return false;
	}
}
