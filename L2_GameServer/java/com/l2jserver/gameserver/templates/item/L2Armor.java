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
package com.l2jserver.gameserver.templates.item;

import java.util.ArrayList;

import com.l2jserver.gameserver.model.L2ItemInstance;
import com.l2jserver.gameserver.model.L2Skill;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.skills.Env;
import com.l2jserver.gameserver.skills.SkillHolder;
import com.l2jserver.gameserver.skills.funcs.Func;
import com.l2jserver.gameserver.skills.funcs.FuncTemplate;
import com.l2jserver.gameserver.templates.StatsSet;
import com.l2jserver.util.StringUtil;

/**
 * This class is dedicated to the management of armors.
 *
 * @version $Revision: 1.2.2.1.2.6 $ $Date: 2005/03/27 15:30:10 $
 */
public final class L2Armor extends L2Item
{
	private final int _avoidModifier;
	private SkillHolder _enchant4Skill = null; // skill that activates when armor is enchanted +4
	// private final String[] _skill;
	private SkillHolder[] _skillHolder;
	private L2ArmorType _type;
	
	/**
	 * Constructor for Armor.<BR><BR>
	 * <U><I>Variables filled :</I></U><BR>
	 * <LI>_avoidModifier</LI>
	 * <LI>_pDef & _mDef</LI>
	 * <LI>_mpBonus & _hpBonus</LI>
	 * <LI>enchant4Skill</LI>
	 * @param type : L2ArmorType designating the type of armor
	 * @param set : StatsSet designating the set of couples (key,value) caracterizing the armor
	 * @see L2Item constructor
	 */
	public L2Armor(StatsSet set)
	{
		super(set);
		_type = L2ArmorType.valueOf(set.getString("armor_type", "none").toUpperCase());
		
		int _bodyPart = getBodyPart();
		if (_bodyPart == L2Item.SLOT_NECK || _bodyPart == L2Item.SLOT_HAIR || _bodyPart == L2Item.SLOT_HAIR2
				|| _bodyPart == L2Item.SLOT_HAIRALL || (_bodyPart & L2Item.SLOT_L_EAR) != 0 || (_bodyPart & L2Item.SLOT_L_FINGER) != 0
				|| (_bodyPart & L2Item.SLOT_R_BRACELET) != 0 || (_bodyPart & L2Item.SLOT_L_BRACELET) != 0
				|| (_bodyPart & L2Item.SLOT_BACK) != 0 )
		{
			_type1 = L2Item.TYPE1_WEAPON_RING_EARRING_NECKLACE;
			_type2 = L2Item.TYPE2_ACCESSORY;
		}
		else
		{
			if (_type == L2ArmorType.NONE && getBodyPart() == L2Item.SLOT_L_HAND) // retail define shield as NONE
				_type = L2ArmorType.SHIELD;
			_type1 = L2Item.TYPE1_SHIELD_ARMOR;
			_type2 = L2Item.TYPE2_SHIELD_ARMOR;
		}
		
		_avoidModifier = set.getInteger("avoid_modify", 0);
		
		String skill = set.getString("enchant4_skill", null);
		if (skill != null)
		{
			String[] info = skill.split("-");
			
			if (info != null && info.length == 2)
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
					// Incorrect syntax, dont add new skill
					_log.info(StringUtil.concat("> Couldnt parse ", skill, " in armor enchant skills! item ",this.toString()));
				}
				if (id > 0 && level > 0)
					_enchant4Skill = new SkillHolder(id, level);
			}
		}
		
		skill = set.getString("item_skill", null);
		if (skill != null)
		{
			String[] skills = skill.split(";");
			_skillHolder = new SkillHolder[skills.length];
			byte iterator = 0;
			for (String st : skills)
			{
				String[] info = st.split("-");
				
				if (info == null || info.length != 2)
					continue;
				
				int id = 0;
				int level = 0;
				
				try
				{
					id = Integer.parseInt(info[0]);
					level = Integer.parseInt(info[1]);
				}
				catch (Exception nfe)
				{
					// Incorrect syntax, dont add new skill
					_log.info(StringUtil.concat("> Couldnt parse ", st, " in armor skills! item ",this.toString()));
					continue;
				}
				
				// If skill can exist, add it
				if (id > 0 && level > 0)
				{
					_skillHolder[iterator] = new SkillHolder(id, level);
					iterator++;
				}
			}
		}
	}
	
	/**
	 * Returns the type of the armor.
	 * @return L2ArmorType
	 */
	@Override
	public L2ArmorType getItemType()
	{
		return _type;
	}
	
	/**
	 * Returns the ID of the item after applying the mask.
	 * @return int : ID of the item
	 */
	@Override
	public final int getItemMask()
	{
		return getItemType().mask();
	}
	
	
	/**
	 * Returns avoid modifier given by the armor
	 * @return int : avoid modifier
	 */
	public final int getAvoidModifier()
	{
		return _avoidModifier;
	}
	
	/**
	 * Returns skill that player get when has equiped armor +4  or more
	 * @return
	 */
	public L2Skill getEnchant4Skill()
	{
		if (_enchant4Skill == null)
			return null;
		return _enchant4Skill.getSkill();
	}
	
	/**
	 * Returns passive skill linked to that armor
	 * @return
	 */
	@Override
	public SkillHolder[] getSkills()
	{
		return _skillHolder;
	}
	
	
	/**
	 * Returns array of Func objects containing the list of functions used by the armor
	 * @param instance : L2ItemInstance pointing out the armor
	 * @param player : L2Character pointing out the player
	 * @return Func[] : array of functions
	 */
	@Override
	public Func[] getStatFuncs(L2ItemInstance instance, L2Character player)
	{
		if (_funcTemplates == null || _funcTemplates.length == 0)
			return _emptyFunctionSet;
		
		ArrayList<Func> funcs = new ArrayList<Func>(_funcTemplates.length);
		
		Env env = new Env();
		env.player = player;
		env.item = instance;
		
		Func f;
		
		for (FuncTemplate t : _funcTemplates) {
			
			f = t.getFunc(env, instance);
			if (f != null)
				funcs.add(f);
		}
		
		return funcs.toArray(new Func[funcs.size()]);
	}
}
