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

import java.util.logging.Level;

import com.l2jserver.gameserver.datatables.EnchantItemGroupsData;
import com.l2jserver.gameserver.model.StatsSet;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.items.instance.L2ItemInstance;
import com.l2jserver.gameserver.model.items.type.L2EtcItemType;
import com.l2jserver.gameserver.model.items.type.L2ItemType;
import com.l2jserver.gameserver.network.Debug;
import com.l2jserver.gameserver.util.Util;
import com.l2jserver.util.Rnd;

/**
 * @author UnAfraid
 */
public final class EnchantScroll extends EnchantItem
{
	private final boolean _isBlessed;
	private final boolean _isSafe;
	private final int _scrollGroupId;
	
	public EnchantScroll(StatsSet set)
	{
		super(set);
		_scrollGroupId = set.getInteger("scrollGroupId", 0);
		
		final L2ItemType type = getItem().getItemType();
		_isWeapon = (type == L2EtcItemType.ANCIENT_CRYSTAL_ENCHANT_WP) || (type == L2EtcItemType.BLESS_SCRL_ENCHANT_WP) || (type == L2EtcItemType.SCRL_ENCHANT_WP);
		_isBlessed = (type == L2EtcItemType.BLESS_SCRL_ENCHANT_AM) || (type == L2EtcItemType.BLESS_SCRL_ENCHANT_WP);
		_isSafe = (type == L2EtcItemType.ANCIENT_CRYSTAL_ENCHANT_AM) || (type == L2EtcItemType.ANCIENT_CRYSTAL_ENCHANT_WP);
	}
	
	/**
	 * @return {@code true} for blessed scrolls (enchanted item will remain on failure), {@code false} otherwise
	 */
	public boolean isBlessed()
	{
		return _isBlessed;
	}
	
	/**
	 * @return {@code true} for safe-enchant scrolls (enchant level will remain on failure), {@code false} otherwise
	 */
	public boolean isSafe()
	{
		return _isSafe;
	}
	
	/**
	 * @return id of scroll group that should be used
	 */
	public int getScrollGroupId()
	{
		return _scrollGroupId;
	}
	
	/**
	 * @param enchantItem
	 * @param supportItem
	 * @return {@code true} if current scroll is valid to be used with support item, {@code false} otherwise
	 */
	public boolean isValid(L2ItemInstance enchantItem, EnchantItem supportItem)
	{
		if ((supportItem != null))
		{
			// blessed scrolls can't use support items
			if (isBlessed())
			{
				return false;
			}
			if (!supportItem.isValid(enchantItem))
			{
				return false;
			}
			else if (supportItem.isWeapon() != _isWeapon)
			{
				return false;
			}
		}
		
		return super.isValid(enchantItem);
	}
	
	/**
	 * @param player
	 * @param enchantItem
	 * @return the chance of current scroll's group.
	 */
	public double getChance(L2PcInstance player, L2ItemInstance enchantItem)
	{
		if (EnchantItemGroupsData.getInstance().getScrollGroup(_scrollGroupId) == null)
		{
			_log.log(Level.WARNING, getClass().getSimpleName() + ": Unexistent enchant scroll group specified for enchant scroll: " + getId());
			return -1;
		}
		
		final EnchantItemGroup group = EnchantItemGroupsData.getInstance().getItemGroup(enchantItem.getItem(), _scrollGroupId);
		if (group == null)
		{
			_log.log(Level.WARNING, getClass().getSimpleName() + ": Couldn't find enchant item group for scroll: " + getId() + " requested by: " + player);
			return -1;
		}
		return group.getChance(enchantItem.getEnchantLevel());
	}
	
	/**
	 * @param player
	 * @param enchantItem
	 * @param supportItem
	 * @return the total chance for success rate of this scroll
	 */
	public EnchantResultType calculateSuccess(L2PcInstance player, L2ItemInstance enchantItem, EnchantItem supportItem)
	{
		if (!isValid(enchantItem, supportItem))
		{
			return EnchantResultType.ERROR;
		}
		
		final double chance = getChance(player, enchantItem);
		if (chance == -1)
		{
			return EnchantResultType.ERROR;
		}
		
		final double bonusRate = getBonusRate();
		final double supportBonusRate = (supportItem != null) ? supportItem.getBonusRate() : 0;
		final double finalChance = Math.min(chance + bonusRate + supportBonusRate, 100);
		
		final double random = 100 * Rnd.nextDouble();
		final boolean success = (random < finalChance);
		
		if (player.isDebug())
		{
			final EnchantItemGroup group = EnchantItemGroupsData.getInstance().getItemGroup(enchantItem.getItem(), _scrollGroupId);
			final StatsSet set = new StatsSet();
			if (isBlessed())
			{
				set.set("isBlessed", isBlessed());
			}
			if (isSafe())
			{
				set.set("isSafe", isSafe());
			}
			set.set("chance", Util.formatDouble(chance, "#.##"));
			if (bonusRate > 0)
			{
				set.set("bonusRate", Util.formatDouble(bonusRate, "#.##"));
			}
			if (supportBonusRate > 0)
			{
				set.set("supportBonusRate", Util.formatDouble(supportBonusRate, "#.##"));
			}
			set.set("finalChance", Util.formatDouble(finalChance, "#.##"));
			set.set("random", Util.formatDouble(random, "#.##"));
			set.set("success", success);
			set.set("item group", group.getName());
			set.set("scroll group", _scrollGroupId);
			Debug.sendItemDebug(player, enchantItem, set);
		}
		return success ? EnchantResultType.SUCCESS : EnchantResultType.FAILURE;
	}
}
