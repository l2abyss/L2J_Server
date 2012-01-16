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
package com.l2jserver.gameserver.model.item;

import java.util.ArrayList;

import com.l2jserver.gameserver.model.L2Skill;
import com.l2jserver.gameserver.model.StatsSet;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.item.instance.L2ItemInstance;
import com.l2jserver.gameserver.model.item.type.L2ArmorType;
import com.l2jserver.gameserver.skills.Env;
import com.l2jserver.gameserver.skills.SkillHolder;
import com.l2jserver.gameserver.skills.funcs.Func;
import com.l2jserver.gameserver.skills.funcs.FuncTemplate;
import com.l2jserver.util.StringUtil;

/**
 * This class is dedicated to the management of armors.
 */
public final class L2Armor extends L2Item
{
	/**
	 * Skill that activates when armor is enchanted +4.
	 */
	private SkillHolder _enchant4Skill = null;
	private L2ArmorType _type;
	
	/**
	 * Constructor for Armor.
	 * @param set the StatsSet designating the set of couples (key,value) characterizing the armor.
	 * @see L2Item constructor
	 */
	public L2Armor(StatsSet set)
	{
		super(set);
		_type = L2ArmorType.valueOf(set.getString("armor_type", "none").toUpperCase());
		
		int _bodyPart = getBodyPart();
		if ((_bodyPart == L2Item.SLOT_NECK) || (_bodyPart == L2Item.SLOT_HAIR) || (_bodyPart == L2Item.SLOT_HAIR2) || (_bodyPart == L2Item.SLOT_HAIRALL) || ((_bodyPart & L2Item.SLOT_L_EAR) != 0) || ((_bodyPart & L2Item.SLOT_L_FINGER) != 0) || ((_bodyPart & L2Item.SLOT_R_BRACELET) != 0) || ((_bodyPart & L2Item.SLOT_L_BRACELET) != 0) || ((_bodyPart & L2Item.SLOT_BACK) != 0))
		{
			_type1 = L2Item.TYPE1_WEAPON_RING_EARRING_NECKLACE;
			_type2 = L2Item.TYPE2_ACCESSORY;
		}
		else
		{
			if ((_type == L2ArmorType.NONE) && (getBodyPart() == L2Item.SLOT_L_HAND))
			{
				_type = L2ArmorType.SHIELD;
			}
			_type1 = L2Item.TYPE1_SHIELD_ARMOR;
			_type2 = L2Item.TYPE2_SHIELD_ARMOR;
		}
		
		String skill = set.getString("enchant4_skill", null);
		if (skill != null)
		{
			String[] info = skill.split("-");
			
			if ((info != null) && (info.length == 2))
			{
				int id = 0;
				int level = 0;
				try
				{
					id = Integer.parseInt(info[0]);
					level = Integer.parseInt(info[1]);
				}
				catch (Exception nfe)
				{
					// Incorrect syntax, don't add new skill
					_log.info(StringUtil.concat("> Couldnt parse ", skill, " in armor enchant skills! item ", toString()));
				}
				if ((id > 0) && (level > 0))
				{
					_enchant4Skill = new SkillHolder(id, level);
				}
			}
		}
	}
	
	/**
	 * @return the type of the armor.
	 */
	@Override
	public L2ArmorType getItemType()
	{
		return _type;
	}
	
	/**
	 * @return the ID of the item after applying the mask.
	 */
	@Override
	public final int getItemMask()
	{
		return getItemType().mask();
	}
	
	/**
	 * @return skill that player get when has equipped armor +4 or more
	 */
	public L2Skill getEnchant4Skill()
	{
		if (_enchant4Skill == null)
		{
			return null;
		}
		return _enchant4Skill.getSkill();
	}
	
	/**
	 * @param instance : L2ItemInstance pointing out the armor
	 * @param player : L2Character pointing out the player
	 * @return array of Func objects containing the list of functions used by the armor
	 */
	@Override
	public Func[] getStatFuncs(L2ItemInstance instance, L2Character player)
	{
		if ((_funcTemplates == null) || (_funcTemplates.length == 0))
		{
			return _emptyFunctionSet;
		}
		
		ArrayList<Func> funcs = new ArrayList<Func>(_funcTemplates.length);
		
		Env env = new Env();
		env.player = player;
		env.item = instance;
		
		Func f;
		
		for (FuncTemplate t : _funcTemplates)
		{
			
			f = t.getFunc(env, instance);
			if (f != null)
			{
				funcs.add(f);
			}
		}
		
		return funcs.toArray(new Func[funcs.size()]);
	}
}
