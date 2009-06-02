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

import java.util.Map;

import javolution.util.FastMap;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.templates.item.L2Item;
import net.sf.l2j.gameserver.templates.item.L2WeaponType;

public class AbstractEnchantPacket extends L2GameClientPacket
{

	public static final Map<Integer, EnchantScroll> _scrolls = new FastMap<Integer, EnchantScroll>();
	public static final Map<Integer, EnchantItem> _supports = new FastMap<Integer, EnchantItem>();

	public static class EnchantItem
	{
		protected final boolean _isWeapon;
		protected final int _grade;
		protected final int _maxEnchantLevel;
		protected final int _chanceAdd;

		public EnchantItem(boolean wep, int type, int level, int chance)
		{
			_isWeapon = wep;
			_grade = type;
			_maxEnchantLevel = level;
			_chanceAdd = chance;
		}

		/*
		 * Return true if support item can be used for this item 
		 */
		public final boolean isValid(L2ItemInstance enchantItem)
		{
			if (enchantItem == null)
				return false;
			
			int type2 = enchantItem.getItem().getType2();

			// checking scroll type and configured maximum enchant level
			switch (type2)
			{
				// weapon scrolls can enchant only weapons
				case L2Item.TYPE2_WEAPON:
					if (!_isWeapon
							|| (Config.ENCHANT_MAX_WEAPON > 0 && enchantItem.getEnchantLevel() >= Config.ENCHANT_MAX_WEAPON))
						return false;
					break;
					// armor scrolls can enchant only accessory and armors
				case L2Item.TYPE2_SHIELD_ARMOR:
					if (_isWeapon
							|| (Config.ENCHANT_MAX_ARMOR > 0 && enchantItem.getEnchantLevel() >= Config.ENCHANT_MAX_ARMOR))
						return false;
					break;
				case L2Item.TYPE2_ACCESSORY:
					if (_isWeapon
							|| (Config.ENCHANT_MAX_JEWELRY > 0 && enchantItem.getEnchantLevel() >= Config.ENCHANT_MAX_JEWELRY))
						return false;
					break;
				default:
					return false;
			}

			// check for crystal types
			switch (enchantItem.getItem().getCrystalType())
			{
				case L2Item.CRYSTAL_S:
				case L2Item.CRYSTAL_S80:
				case L2Item.CRYSTAL_S84:
					if (_grade != L2Item.CRYSTAL_S)
						return false;
					break;
				default:
					if (_grade != enchantItem.getItem().getCrystalType())
						return false;
			}

			// check for maximum enchant level
			if (_maxEnchantLevel != 0 && enchantItem.getEnchantLevel() >= _maxEnchantLevel)
				return false;

			return true;
		}

		/*
		 * return chance increase
		 */
		public final int getChanceAdd()
		{
			return _chanceAdd;
		}
	}

	public static final class EnchantScroll extends EnchantItem
	{
		private final boolean _isBlessed;
		private final boolean _isCrystal;
		private final boolean _isSafe;

		public EnchantScroll(boolean wep, boolean bless, boolean crystal, boolean safe, int type, int level, int chance)
		{
			super(wep, type, level, chance);

			_isBlessed = bless;
			_isCrystal = crystal;
			_isSafe = safe;
		}

		/*
		 * Return true for blessed scrolls
		 */
		public final boolean isBlessed()
		{
			return _isBlessed;
		}

		/*
		 * Return true for crystal scrolls
		 */
		public final boolean isCrystal()
		{
			return _isCrystal;
		}

		/*
		 * Return true for safe-enchant scrolls (enchant level will remain on failure)
		 */
		public final boolean isSafe()
		{
			return _isSafe;
		}

		public final boolean isValid(L2ItemInstance enchantItem, EnchantItem supportItem)
		{
			// blessed scrolls can't use support items
			if (supportItem != null && (!supportItem.isValid(enchantItem) || isBlessed()))
					return false;

			return isValid(enchantItem);
		}

		public final int getChance(L2ItemInstance enchantItem, EnchantItem supportItem)
		{
			if (!isValid(enchantItem, supportItem))
				return -1;

			boolean fullBody = enchantItem.getItem().getBodyPart() == L2Item.SLOT_FULL_ARMOR;
			if (enchantItem.getEnchantLevel() < Config.ENCHANT_SAFE_MAX
					|| (fullBody && enchantItem.getEnchantLevel() < Config.ENCHANT_SAFE_MAX_FULL))
				return 100;

			boolean isAccessory = enchantItem.getItem().getType2() == L2Item.TYPE2_ACCESSORY;
			int chance = 0;

			if (_isBlessed)
			{
				// blessed scrolls does not use support items
				if (supportItem != null)
					return -1;

				if (_isWeapon)
					chance = Config.BLESSED_ENCHANT_CHANCE_WEAPON;
				else if (isAccessory)
					chance = Config.BLESSED_ENCHANT_CHANCE_JEWELRY;
				else
					chance = Config.BLESSED_ENCHANT_CHANCE_ARMOR;
			}
			else
			{
				if (_isWeapon)
					chance = Config.ENCHANT_CHANCE_WEAPON;
				else if (isAccessory)
					chance = Config.ENCHANT_CHANCE_JEWELRY;
				else
					chance = Config.ENCHANT_CHANCE_ARMOR;
			}

			chance += _chanceAdd;

			if (supportItem != null)
				chance += supportItem.getChanceAdd();

			return chance;
		}
	}

	static
	{
		// itemId, (isWeapon, isBlessed, isCrystal, isSafe, grade, max enchant level, chance increase)
		_scrolls.put(729, new EnchantScroll(true, false, false, false, L2Item.CRYSTAL_A, 0, 0));
		_scrolls.put(730, new EnchantScroll(false, false, false, false, L2Item.CRYSTAL_A, 0, 0));
		_scrolls.put(731, new EnchantScroll(true, false, true, false, L2Item.CRYSTAL_A, 0, 0));
		_scrolls.put(732, new EnchantScroll(false, false, true, false, L2Item.CRYSTAL_A, 0, 0));
		_scrolls.put(947, new EnchantScroll(true, false, false, false, L2Item.CRYSTAL_B, 0, 0));
		_scrolls.put(948, new EnchantScroll(false, false, false, false, L2Item.CRYSTAL_B, 0, 0));
		_scrolls.put(949, new EnchantScroll(true, false, true, false, L2Item.CRYSTAL_B, 0, 0));
		_scrolls.put(950, new EnchantScroll(false, false, true, false, L2Item.CRYSTAL_B, 0, 0));
		_scrolls.put(951, new EnchantScroll(true, false, false, false, L2Item.CRYSTAL_C, 0, 0));
		_scrolls.put(952, new EnchantScroll(false, false, false, false, L2Item.CRYSTAL_C, 0, 0));
		_scrolls.put(953, new EnchantScroll(true, false, true, false, L2Item.CRYSTAL_C, 0, 0));
		_scrolls.put(954, new EnchantScroll(false, false, true, false, L2Item.CRYSTAL_C, 0, 0));
		_scrolls.put(955, new EnchantScroll(true, false, false, false, L2Item.CRYSTAL_D, 0, 0));
		_scrolls.put(956, new EnchantScroll(false, false, false, false, L2Item.CRYSTAL_D, 0, 0));
		_scrolls.put(957, new EnchantScroll(true, false, true, false, L2Item.CRYSTAL_D, 0, 0));
		_scrolls.put(958, new EnchantScroll(false, false, true, false, L2Item.CRYSTAL_D, 0, 0));
		_scrolls.put(959, new EnchantScroll(true, false, false, false, L2Item.CRYSTAL_S, 0, 0));
		_scrolls.put(960, new EnchantScroll(false, false, false, false, L2Item.CRYSTAL_S, 0, 0));
		_scrolls.put(961, new EnchantScroll(true, false, true, false, L2Item.CRYSTAL_S, 0, 0));
		_scrolls.put(962, new EnchantScroll(false, false, true, false, L2Item.CRYSTAL_S, 0, 0));
		_scrolls.put(6569, new EnchantScroll(true, true, false, false, L2Item.CRYSTAL_A, 0, 0));
		_scrolls.put(6570, new EnchantScroll(false, true, false, false, L2Item.CRYSTAL_A, 0, 0));
		_scrolls.put(6571, new EnchantScroll(true, true, false, false, L2Item.CRYSTAL_B, 0, 0));
		_scrolls.put(6572, new EnchantScroll(false, true, false, false, L2Item.CRYSTAL_B, 0, 0));
		_scrolls.put(6573, new EnchantScroll(true, true, false, false, L2Item.CRYSTAL_C, 0, 0));
		_scrolls.put(6574, new EnchantScroll(false, true, false, false, L2Item.CRYSTAL_C, 0, 0));
		_scrolls.put(6575, new EnchantScroll(true, true, false, false, L2Item.CRYSTAL_D, 0, 0));
		_scrolls.put(6576, new EnchantScroll(false, true, false, false, L2Item.CRYSTAL_D, 0, 0));
		_scrolls.put(6577, new EnchantScroll(true, true, false, false, L2Item.CRYSTAL_S, 0, 0));
		_scrolls.put(6578, new EnchantScroll(false, true, false, false, L2Item.CRYSTAL_S, 0, 0));
		_scrolls.put(22006, new EnchantScroll(true, false, false, false, L2Item.CRYSTAL_D, 0, 10));
		_scrolls.put(22007, new EnchantScroll(true, false, false, false, L2Item.CRYSTAL_C, 0, 10));
		_scrolls.put(22008, new EnchantScroll(true, false, false, false, L2Item.CRYSTAL_B, 0, 10));
		_scrolls.put(22009, new EnchantScroll(true, false, false, false, L2Item.CRYSTAL_A, 0, 10));
		_scrolls.put(22010, new EnchantScroll(false, false, false, false, L2Item.CRYSTAL_D, 0, 10));
		_scrolls.put(22011, new EnchantScroll(false, false, false, false, L2Item.CRYSTAL_C, 0, 10));
		_scrolls.put(22012, new EnchantScroll(false, false, false, false, L2Item.CRYSTAL_B, 0, 10));
		_scrolls.put(22013, new EnchantScroll(false, false, false, false, L2Item.CRYSTAL_A, 0, 10));
		_scrolls.put(22014, new EnchantScroll(true, false, false, true, L2Item.CRYSTAL_B, 16, 10));
		_scrolls.put(22015, new EnchantScroll(true, false, false, true, L2Item.CRYSTAL_A, 16, 10));
		_scrolls.put(22016, new EnchantScroll(false, false, false, true, L2Item.CRYSTAL_B, 16, 10));
		_scrolls.put(22017, new EnchantScroll(false, false, false, true, L2Item.CRYSTAL_A, 16, 10));
		_scrolls.put(22018, new EnchantScroll(true, false, false, false, L2Item.CRYSTAL_B, 0, 100));
		_scrolls.put(22019, new EnchantScroll(true, false, false, false, L2Item.CRYSTAL_A, 0, 100));
		_scrolls.put(22020, new EnchantScroll(false, false, false, false, L2Item.CRYSTAL_B, 0, 100));
		_scrolls.put(22021, new EnchantScroll(false, false, false, false, L2Item.CRYSTAL_A, 0, 100));

		// itemId, (isWeapon, grade, max enchant level, chance increase)
		_supports.put(12362, new EnchantItem(true, L2Item.CRYSTAL_D, 9, 20));
		_supports.put(12363, new EnchantItem(true, L2Item.CRYSTAL_C, 9, 18));
		_supports.put(12364, new EnchantItem(true, L2Item.CRYSTAL_B, 9, 15));
		_supports.put(12365, new EnchantItem(true, L2Item.CRYSTAL_A, 9, 12));
		_supports.put(12366, new EnchantItem(true, L2Item.CRYSTAL_S, 9, 10));
		_supports.put(12367, new EnchantItem(false, L2Item.CRYSTAL_D, 9, 35));
		_supports.put(12368, new EnchantItem(false, L2Item.CRYSTAL_C, 9, 27));
		_supports.put(12369, new EnchantItem(false, L2Item.CRYSTAL_B, 9, 23));
		_supports.put(12370, new EnchantItem(false, L2Item.CRYSTAL_A, 9, 18));
		_supports.put(12371, new EnchantItem(false, L2Item.CRYSTAL_S, 9, 15));
		_supports.put(14702, new EnchantItem(true, L2Item.CRYSTAL_D, 9, 20));
		_supports.put(14703, new EnchantItem(true, L2Item.CRYSTAL_C, 9, 18));
		_supports.put(14704, new EnchantItem(true, L2Item.CRYSTAL_B, 9, 15));
		_supports.put(14705, new EnchantItem(true, L2Item.CRYSTAL_A, 9, 12));
		_supports.put(14706, new EnchantItem(true, L2Item.CRYSTAL_S, 9, 10));
		_supports.put(14707, new EnchantItem(false, L2Item.CRYSTAL_D, 9, 35));
		_supports.put(14708, new EnchantItem(false, L2Item.CRYSTAL_C, 9, 27));
		_supports.put(14709, new EnchantItem(false, L2Item.CRYSTAL_B, 9, 23));
		_supports.put(14710, new EnchantItem(false, L2Item.CRYSTAL_A, 9, 18));
		_supports.put(14711, new EnchantItem(false, L2Item.CRYSTAL_S, 9, 15));
	}

	/**
	 * Return enchant template for scroll
	 */
	protected static final EnchantScroll getEnchantScroll(L2ItemInstance scroll)
	{
		return _scrolls.get(scroll.getItemId());
	}

	/**
	 * Return enchant template for support item 
	 */
	protected static final EnchantItem getSupportItem(L2ItemInstance item)
	{
		return _supports.get(item.getItemId());
	}

	/**
	 * Return true if item can be enchanted 
	 */
	protected static final boolean isEnchantable(L2ItemInstance item)
	{
		if (item.isHeroItem())
			return false;
		if (item.isShadowItem())
			return false;
		if (item.isCommonItem())
			return false;
		if (item.isEtcItem())
			return false;
		if (item.isTimeLimitedItem())
			return false;
		if (item.isWear())
			return false;
		// rods
		if (item.getItem().getItemType() == L2WeaponType.ROD)
			return false;
		// apprentice and travelers weapons
		if (item.getItemId() >= 7816 && item.getItemId() <= 7831)
			return false;
		// bracelets
		if (item.getItem().getBodyPart() == L2Item.SLOT_L_BRACELET)
			return false;
		if (item.getItem().getBodyPart() == L2Item.SLOT_R_BRACELET)
			return false;
		// only items in inventory and equipped can be enchanted
		if (item.getLocation() != L2ItemInstance.ItemLocation.INVENTORY
				&& item.getLocation() != L2ItemInstance.ItemLocation.PAPERDOLL)
			return false;
		
		return true;
	}

	@Override
	protected void readImpl()
	{

	}

	@Override
	protected void runImpl()
	{

	}

	@Override
	public String getType()
	{
		return "Never Used";
	}
}