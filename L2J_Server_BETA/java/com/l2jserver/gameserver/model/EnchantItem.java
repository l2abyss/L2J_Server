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

import java.util.Arrays;

import com.l2jserver.gameserver.model.item.L2Item;
import com.l2jserver.gameserver.model.item.instance.L2ItemInstance;

/**
 * @author UnAfraid
 */
public class EnchantItem
{
	protected final boolean _isWeapon;
	protected final int _grade;
	protected final int _maxEnchantLevel;
	protected final double _chanceAdd;
	protected final int[] _itemIds;
	
	/**
	 * @param wep
	 * @param type
	 * @param level
	 * @param chance
	 * @param items
	 */
	public EnchantItem(boolean wep, int type, int level, double chance, int[] items)
	{
		_isWeapon = wep;
		_grade = type;
		_maxEnchantLevel = level;
		_chanceAdd = chance;
		_itemIds = items;
	}
	
	/**
	 * @param enchantItem
	 * @return true if support item can be used for this item
	 */
	public final boolean isValid(L2ItemInstance enchantItem)
	{
		if (enchantItem == null)
			return false;
		
		else if (enchantItem.isEnchantable() == 0)
			return false;
		
		else if (!isValidItemType(enchantItem.getItem().getType2()))
			return false;

		else if (_maxEnchantLevel != 0 && enchantItem.getEnchantLevel() >= _maxEnchantLevel)
			return false;
		
		else if (_grade != enchantItem.getItem().getItemGradeSPlus())
			return false;
		
		else if ((enchantItem.isEnchantable() > 1 && (_itemIds == null || Arrays.binarySearch(_itemIds, enchantItem.getItemId()) < 0)) || _itemIds != null && Arrays.binarySearch(_itemIds, enchantItem.getItemId()) < 0)
			return false;
				
		return true;
	}
	
	private boolean isValidItemType(int type2)
	{
		if (type2 == L2Item.TYPE2_WEAPON)
		{
			return _isWeapon;
		}
		else if (type2 == L2Item.TYPE2_SHIELD_ARMOR || type2 == L2Item.TYPE2_ACCESSORY)
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
}
