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

import java.util.List;

import com.l2jserver.Config;
import com.l2jserver.gameserver.datatables.ItemTable;
import com.l2jserver.gameserver.model.items.L2Item;
import com.l2jserver.gameserver.model.items.instance.L2ItemInstance;

/**
 * @author UnAfraid
 */
public class EnchantItem
{
	protected final int _id;
	protected final boolean _isWeapon;
	protected final int _grade;
	protected final int _maxEnchantLevel;
	protected final double _chanceAdd;
	protected final List<Integer> _itemIds;
	
	/**
	 * @param set
	 * @param items
	 */
	public EnchantItem(StatsSet set, List<Integer> items)
	{
		_id = set.getInteger("id");
		_isWeapon = set.getBool("isWeapon", true);
		_grade = ItemTable._crystalTypes.get(set.getString("targetGrade", "none"));
		_maxEnchantLevel = set.getInteger("maxEnchant", Config.MAX_ENCHANT_LEVEL);
		_chanceAdd = set.getDouble("successRate", Config.ENCHANT_CHANCE);
		_itemIds = items;
	}
	
	/**
	 * @param enchantItem
	 * @return true if support item can be used for this item
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
		else if (((enchantItem.isEnchantable() > 1) && (_itemIds.isEmpty() || !_itemIds.contains(enchantItem.getItemId()))) || (!_itemIds.isEmpty() && !_itemIds.contains(enchantItem.getItemId())))
		{
			return false;
		}
		
		return true;
	}
	
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
	
	/**
	 * @return chance increase
	 */
	public final double getChanceAdd()
	{
		return _chanceAdd;
	}
	
	public int getScrollId()
	{
		return _id;
	}
}
