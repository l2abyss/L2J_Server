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

import javolution.util.FastList;

import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.holders.SkillHolder;
import com.l2jserver.gameserver.model.itemcontainer.Inventory;
import com.l2jserver.gameserver.model.items.instance.L2ItemInstance;

/**
 * @author Luno
 */
public final class L2ArmorSet
{
	private final FastList<Integer> _chest;
	private final FastList<Integer> _legs;
	private final FastList<Integer> _head;
	private final FastList<Integer> _gloves;
	private final FastList<Integer> _feet;	
	private final FastList<Integer> _shield;
	
	private final FastList<SkillHolder> _skills;
	private final FastList<SkillHolder> _shieldSkills;
	private final FastList<SkillHolder> _enchant6Skill;
	
	public L2ArmorSet()
	{
		_chest = new FastList<>();
		_legs = new FastList<>();
		_head = new FastList<>();
		_gloves = new FastList<>();
		_feet = new FastList<>();
		_shield = new FastList<>();
		
		_skills = new FastList<>();
		_shieldSkills = new FastList<>();
		_enchant6Skill = new FastList<>();
	}
	
	public void addChest(int id)
	{
		_chest.add(id);
	}
	
	public void addLegs(int id)
	{
		_legs.add(id);
	}
	
	public void addHead(int id)
	{
		_head.add(id);
	}
	
	public void addGloves(int id)
	{
		_gloves.add(id);
	}
	
	public void addFeet(int id)
	{
		_feet.add(id);
	}
	
	public void addShield(int id)
	{
		_shield.add(id);
	}
	
	public void addSkill(SkillHolder holder)
	{
		_skills.add(holder);
	}
	
	public void addShieldSkill(SkillHolder holder)
	{
		_shieldSkills.add(holder);
	}
	
	public void addEnchant6Skill(SkillHolder holder)
	{
		_enchant6Skill.add(holder);
	}
	
	/**
	 * Checks if player have equipped all items from set (not checking shield)
	 * 
	 * @param player
	 *            whose inventory is being checked
	 * @return True if player equips whole set
	 */
	public boolean containAll(L2PcInstance player)
	{
		Inventory inv = player.getInventory();
		
		L2ItemInstance legsItem = inv.getPaperdollItem(Inventory.PAPERDOLL_LEGS);
		L2ItemInstance headItem = inv.getPaperdollItem(Inventory.PAPERDOLL_HEAD);
		L2ItemInstance glovesItem = inv.getPaperdollItem(Inventory.PAPERDOLL_GLOVES);
		L2ItemInstance feetItem = inv.getPaperdollItem(Inventory.PAPERDOLL_FEET);
		
		int legs = 0;
		int head = 0;
		int gloves = 0;
		int feet = 0;
		
		if (legsItem != null)
			legs = legsItem.getItemId();
		if (headItem != null)
			head = headItem.getItemId();
		if (glovesItem != null)
			gloves = glovesItem.getItemId();
		if (feetItem != null)
			feet = feetItem.getItemId();
		
		if (!_chest.isEmpty())
		{
			for (Integer chest : _chest)
			{
				if (containAll(chest, legs, head, gloves, feet))
					return true;
			}
		}
		return containAll(0, legs, head, gloves, feet);
	}
	
	public boolean containAll(int chest, int legs, int head, int gloves, int feet)
	{
		if (!_chest.isEmpty() && !_chest.contains(Integer.valueOf(chest)))
			return false;
		if (!_legs.isEmpty() && !_legs.contains(Integer.valueOf(legs)))
			return false;
		if (!_head.isEmpty() && !_head.contains(Integer.valueOf(head)))
			return false;
		if (!_gloves.isEmpty() && !_gloves.contains(Integer.valueOf(gloves)))
			return false;
		if (!_feet.isEmpty() && !_feet.contains(Integer.valueOf(feet)))
			return false;
		
		return true;
	}
	
	public boolean containItem(int slot, Integer itemId)
	{
		switch (slot)
		{
			case Inventory.PAPERDOLL_CHEST:
				return _chest.contains(Integer.valueOf(itemId));
			case Inventory.PAPERDOLL_LEGS:
				return _legs.contains(Integer.valueOf(itemId));
			case Inventory.PAPERDOLL_HEAD:
				return _head.contains(Integer.valueOf(itemId));
			case Inventory.PAPERDOLL_GLOVES:
				return _gloves.contains(Integer.valueOf(itemId));
			case Inventory.PAPERDOLL_FEET:
				return _feet.contains(Integer.valueOf(itemId));
			default:
				return false;
		}
	}
	
	public FastList<SkillHolder> getSkills()
	{
		return _skills;
	}
	
	public boolean containShield(L2PcInstance player)
	{
		Inventory inv = player.getInventory();
		
		L2ItemInstance shieldItem = inv.getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		return (shieldItem != null && _shield.contains(Integer.valueOf(shieldItem.getItemId())));
	}
	
	public boolean containShield(int shield_id)
	{
		if (_shield.isEmpty())
			return false;
		
		return _shield.contains(Integer.valueOf(shield_id));
	}
	
	public FastList<SkillHolder> getShieldSkillId()
	{
		return _shieldSkills;
	}
	
	public FastList<SkillHolder> getEnchant6skillId()
	{
		return _enchant6Skill;
	}
	
	/**
	 * @param player
	 * @return true if all parts of set are enchanted to +6 or more
	 */
	public boolean isEnchanted6(L2PcInstance player)
	{
		// Player don't have full set
		if (!containAll(player))
			return false;
		
		Inventory inv = player.getInventory();
		
		L2ItemInstance chestItem = inv.getPaperdollItem(Inventory.PAPERDOLL_CHEST);
		L2ItemInstance legsItem = inv.getPaperdollItem(Inventory.PAPERDOLL_LEGS);
		L2ItemInstance headItem = inv.getPaperdollItem(Inventory.PAPERDOLL_HEAD);
		L2ItemInstance glovesItem = inv.getPaperdollItem(Inventory.PAPERDOLL_GLOVES);
		L2ItemInstance feetItem = inv.getPaperdollItem(Inventory.PAPERDOLL_FEET);
		
		if (chestItem == null || chestItem.getEnchantLevel() < 6)
			return false;
		if (!_legs.isEmpty() && (legsItem == null || legsItem.getEnchantLevel() < 6))
			return false;
		if (!_gloves.isEmpty() && (glovesItem == null || glovesItem.getEnchantLevel() < 6))
			return false;
		if (!_head.isEmpty() && (headItem == null || headItem.getEnchantLevel() < 6))
			return false;
		if (!_feet.isEmpty() && (feetItem == null || feetItem.getEnchantLevel() < 6))
			return false;
		
		return true;
	}

	public boolean containsChest(int chestId)
	{
		return _chest.contains(Integer.valueOf(chestId));
	}
}
