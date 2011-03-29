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
import java.util.List;
import java.util.logging.Logger;

import javolution.util.FastList;

import com.l2jserver.Config;
import com.l2jserver.gameserver.datatables.ItemTable;
import com.l2jserver.gameserver.model.Elementals;
import com.l2jserver.gameserver.model.L2Effect;
import com.l2jserver.gameserver.model.L2ItemInstance;
import com.l2jserver.gameserver.model.L2Object;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.L2Summon;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;
import com.l2jserver.gameserver.skills.Env;
import com.l2jserver.gameserver.skills.SkillHolder;
import com.l2jserver.gameserver.skills.conditions.Condition;
import com.l2jserver.gameserver.skills.conditions.ConditionLogicOr;
import com.l2jserver.gameserver.skills.conditions.ConditionPetType;
import com.l2jserver.gameserver.skills.funcs.Func;
import com.l2jserver.gameserver.skills.funcs.FuncTemplate;
import com.l2jserver.gameserver.templates.StatsSet;
import com.l2jserver.gameserver.templates.effects.EffectTemplate;

/**
 * This class contains all informations concerning the item (weapon, armor, etc).<BR>
 * Mother class of :
 * <LI>L2Armor</LI>
 * <LI>L2EtcItem</LI>
 * <LI>L2Weapon</LI>
 * @version $Revision: 1.7.2.2.2.5 $ $Date: 2005/04/06 18:25:18 $
 */
public abstract class L2Item
{
	public static final int TYPE1_WEAPON_RING_EARRING_NECKLACE = 0;
	public static final int TYPE1_SHIELD_ARMOR = 1;
	public static final int TYPE1_ITEM_QUESTITEM_ADENA = 4;
	
	public static final int TYPE2_WEAPON = 0;
	public static final int TYPE2_SHIELD_ARMOR = 1;
	public static final int TYPE2_ACCESSORY = 2;
	public static final int TYPE2_QUEST = 3;
	public static final int TYPE2_MONEY = 4;
	public static final int TYPE2_OTHER = 5;
	
	public static final int WOLF = 0x1;
	public static final int HATCHLING = 0x2;
	public static final int STRIDER = 0x4;
	public static final int BABY = 0x8;
	public static final int IMPROVED_BABY = 0x10;
	public static final int GROWN_WOLF = 0x20;
	public static final int ALL_WOLF = 0x21;
	public static final int ALL_PET = 0x3F;
	
	public static final int SLOT_NONE = 0x0000;
	public static final int SLOT_UNDERWEAR = 0x0001;
	public static final int SLOT_R_EAR = 0x0002;
	public static final int SLOT_L_EAR = 0x0004;
	public static final int SLOT_LR_EAR = 0x00006;
	public static final int SLOT_NECK = 0x0008;
	public static final int SLOT_R_FINGER = 0x0010;
	public static final int SLOT_L_FINGER = 0x0020;
	public static final int SLOT_LR_FINGER = 0x0030;
	public static final int SLOT_HEAD = 0x0040;
	public static final int SLOT_R_HAND = 0x0080;
	public static final int SLOT_L_HAND = 0x0100;
	public static final int SLOT_GLOVES = 0x0200;
	public static final int SLOT_CHEST = 0x0400;
	public static final int SLOT_LEGS = 0x0800;
	public static final int SLOT_FEET = 0x1000;
	public static final int SLOT_BACK = 0x2000;
	public static final int SLOT_LR_HAND = 0x4000;
	public static final int SLOT_FULL_ARMOR = 0x8000;
	public static final int SLOT_HAIR = 0x010000;
	public static final int SLOT_ALLDRESS = 0x020000;
	public static final int SLOT_HAIR2 = 0x040000;
	public static final int SLOT_HAIRALL = 0x080000;
	public static final int SLOT_R_BRACELET = 0x100000;
	public static final int SLOT_L_BRACELET = 0x200000;
	public static final int SLOT_DECO = 0x400000;
	public static final int SLOT_BELT = 0x10000000;
	public static final int SLOT_WOLF = -100;
	public static final int SLOT_HATCHLING = -101;
	public static final int SLOT_STRIDER = -102;
	public static final int SLOT_BABYPET = -103;
	public static final int SLOT_GREATWOLF = -104;
	
	public static final int SLOT_MULTI_ALLWEAPON = SLOT_LR_HAND | SLOT_R_HAND;
	
	public static final int MATERIAL_STEEL = 0x00; // ??
	public static final int MATERIAL_FINE_STEEL = 0x01; // ??
	public static final int MATERIAL_BLOOD_STEEL = 0x02; // ??
	public static final int MATERIAL_BRONZE = 0x03; // ??
	public static final int MATERIAL_SILVER = 0x04; // ??
	public static final int MATERIAL_GOLD = 0x05; // ??
	public static final int MATERIAL_MITHRIL = 0x06; // ??
	public static final int MATERIAL_ORIHARUKON = 0x07; // ??
	public static final int MATERIAL_PAPER = 0x08; // ??
	public static final int MATERIAL_WOOD = 0x09; // ??
	public static final int MATERIAL_CLOTH = 0x0a; // ??
	public static final int MATERIAL_LEATHER = 0x0b; // ??
	public static final int MATERIAL_BONE = 0x0c; // ??
	public static final int MATERIAL_HORN = 0x0d; // ??
	public static final int MATERIAL_DAMASCUS = 0x0e; // ??
	public static final int MATERIAL_ADAMANTAITE = 0x0f; // ??
	public static final int MATERIAL_CHRYSOLITE = 0x10; // ??
	public static final int MATERIAL_CRYSTAL = 0x11; // ??
	public static final int MATERIAL_LIQUID = 0x12; // ??
	public static final int MATERIAL_SCALE_OF_DRAGON = 0x13; // ??
	public static final int MATERIAL_DYESTUFF = 0x14; // ??
	public static final int MATERIAL_COBWEB = 0x15; // ??
	public static final int MATERIAL_SEED = 0x16; // ??
	public static final int MATERIAL_FISH = 0x17; // ??
	public static final int MATERIAL_RUNE_XP = 0x18; // ??
	public static final int MATERIAL_RUNE_SP = 0x19; // ??
	public static final int MATERIAL_RUNE_PENALTY = 0x20; // ??
	
	public static final int CRYSTAL_NONE = 0x00; // ??
	public static final int CRYSTAL_D = 0x01; // ??
	public static final int CRYSTAL_C = 0x02; // ??
	public static final int CRYSTAL_B = 0x03; // ??
	public static final int CRYSTAL_A = 0x04; // ??
	public static final int CRYSTAL_S = 0x05; // ??
	public static final int CRYSTAL_S80 = 0x06; // ??
	public static final int CRYSTAL_S84 = 0x07; // ??
	
	private static final int[] crystalItemId =
	{
		0, 1458, 1459, 1460, 1461, 1462, 1462, 1462
	};
	private static final int[] crystalEnchantBonusArmor =
	{
		0, 11, 6, 11, 19, 25, 25, 25
	};
	private static final int[] crystalEnchantBonusWeapon =
	{
		0, 90, 45, 67, 144, 250, 250, 250
	};
	
	private final int _itemId;
	private final String _name;
	private final String _icon;
	private final int _weight;
	private final boolean _stackable;
	private final int _materialType;
	private final int _crystalType; // default to none-grade
	private final int _duration;
	private final int _time;
	private final int _bodyPart;
	private final int _referencePrice;
	private final int _crystalCount;
	private final boolean _sellable;
	private final boolean _dropable;
	private final boolean _destroyable;
	private final boolean _tradeable;
	private final boolean _depositable;
	private final boolean _questItem;
	private final boolean _common;
	private final boolean _heroItem;
	private final boolean _pvpItem;
	private final boolean _ex_immediate_effect;
	private final L2ActionType _defaultAction;
	
	protected int _type1; // needed for item list (inventory)
	protected int _type2; // different lists for armor, weapon, etc	
	protected Elementals[] _elementals = null;
	protected FuncTemplate[] _funcTemplates;
	protected EffectTemplate[] _effectTemplates;
	protected List <Condition> _preConditions;
	
	protected static final Func[] _emptyFunctionSet = new Func[0];
	protected static final L2Effect[] _emptyEffectSet = new L2Effect[0];
	
	protected static final Logger _log = Logger.getLogger(L2Item.class.getName());
	
	/**
	 * Constructor of the L2Item that fill class variables.<BR><BR>
	 * @param set : StatsSet corresponding to a set of couples (key,value) for description of the item
	 */
	protected L2Item(StatsSet set)
	{
		_itemId = set.getInteger("item_id");
		_name = set.getString("name");
		_icon = set.getString("icon", null);
		_weight = set.getInteger("weight", 0);
		_materialType = ItemTable._materials.get(set.getString("material", "steel")); // default is steel, yeah and what?
		_duration = set.getInteger("duration", -1);
		_time = set.getInteger("time", -1);
		_bodyPart = ItemTable._slots.get(set.getString("bodypart", "none"));
		_referencePrice = set.getInteger("price", 0);
		_crystalType = ItemTable._crystalTypes.get(set.getString("crystal_type", "none")); // default to none-grade
		_crystalCount = set.getInteger("crystal_count", 0);
		
		_stackable = set.getBool("is_stackable", false);
		_sellable = set.getBool("is_sellable", true);
		_dropable = set.getBool("is_dropable", true);
		_destroyable = set.getBool("is_destroyable", true);
		_tradeable = set.getBool("is_tradable", true);
		_depositable = set.getBool("is_depositable", true);
		_questItem = set.getBool("is_questitem", false);
		
		//_immediate_effect - herb
		_ex_immediate_effect = set.getInteger("ex_immediate_effect", 0) > 0;
		//used for custom type select
		_defaultAction = set.getEnum("default_action", L2ActionType.class, L2ActionType.none);
		
		//TODO cleanup + finish
		String equip_condition = set.getString("equip_condition", null);
		if (equip_condition != null)
		{
			//pet conditions
			ConditionLogicOr cond = new ConditionLogicOr();
			if (equip_condition.contains("all_wolf_group"))
				cond.add(new ConditionPetType(ALL_WOLF));
			if (equip_condition.contains("hatchling_group"))
				cond.add(new ConditionPetType(HATCHLING));
			if (equip_condition.contains("strider"))
				cond.add(new ConditionPetType(STRIDER));
			if (equip_condition.contains("baby_pet_group"))
				cond.add(new ConditionPetType(BABY));
			if (equip_condition.contains("upgrade_baby_pet_group"))
				cond.add(new ConditionPetType(IMPROVED_BABY));
			if (equip_condition.contains("grown_up_wolf_group"))
				cond.add(new ConditionPetType(GROWN_WOLF));
			if (equip_condition.contains("item_equip_pet_group"))
				cond.add(new ConditionPetType(ALL_PET));
			
			if (cond.conditions.length > 0)
				attach(cond);
		}

		
		_common = (_itemId >= 11605 && _itemId <= 12361);
		_heroItem = (_itemId >= 6611 && _itemId <= 6621) || (_itemId >= 9388 && _itemId <= 9390) || _itemId == 6842;
		_pvpItem = (_itemId >= 10667 && _itemId <= 10835) || (_itemId >= 12852 && _itemId <= 12977) || (_itemId >= 14363 && _itemId <= 14525) || _itemId == 14528 || _itemId == 14529 || _itemId == 14558 || (_itemId >=15913 && _itemId <= 16024) || (_itemId >=16134 && _itemId <= 16147) || _itemId == 16149 || _itemId == 16151 || _itemId == 16153 || _itemId == 16155 || _itemId == 16157 || _itemId == 16159 || (_itemId >=16168 && _itemId <= 16176) || (_itemId >=16179 && _itemId <= 16220);
	}
	
	/**
	 * Returns the itemType.
	 * @return Enum
	 */
	public abstract L2ItemType getItemType();
	
	/**
	 * Returns the duration of the item
	 * @return int
	 */
	public final int getDuration()
	{
		return _duration;
	}
	
	/**
	 * Returns the time of the item
	 * @return int
	 */
	public final int getTime()
	{
		return _time;
	}
	/**
	 * Returns the ID of the iden
	 * @return int
	 */
	public final int getItemId()
	{
		return _itemId;
	}
	
	public abstract int getItemMask();
	
	/**
	 * Return the type of material of the item
	 * @return int
	 */
	public final int getMaterialType()
	{
		return _materialType;
	}
	
	/**
	 * Returns the type 2 of the item
	 * @return int
	 */
	public final int getType2()
	{
		return _type2;
	}
	
	/**
	 * Returns the weight of the item
	 * @return int
	 */
	public final int getWeight()
	{
		return _weight;
	}
	
	/**
	 * Returns if the item is crystallizable
	 * @return boolean
	 */
	public final boolean isCrystallizable()
	{
		return _crystalType != L2Item.CRYSTAL_NONE && _crystalCount > 0;
	}
	
	/**
	 * Return the type of crystal if item is crystallizable
	 * @return int
	 */
	public final int getCrystalType()
	{
		return _crystalType;
	}
	
	/**
	 * Return the type of crystal if item is crystallizable
	 * @return int
	 */
	public final int getCrystalItemId()
	{
		return crystalItemId[_crystalType];
	}
	
	/**
	 * Returns the grade of the item.<BR><BR>
	 * <U><I>Concept :</I></U><BR>
	 * In fact, this fucntion returns the type of crystal of the item.
	 * @return int
	 */
	public final int getItemGrade()
	{
		return getCrystalType();
	}
	
	/**
	 * Returns the grade of the item.<BR><BR>
	 * For grades S80 and S84 return S
	 * @return int
	 */
	public final int getItemGradeSPlus()
	{
		switch (getItemGrade())
		{
			case CRYSTAL_S80:
			case CRYSTAL_S84:
				return CRYSTAL_S;
			default:
				return getItemGrade();
		}
	}
	
	/**
	 * Returns the quantity of crystals for crystallization
	 * @return int
	 */
	public final int getCrystalCount()
	{
		return _crystalCount;
	}
	
	/**
	 * Returns the quantity of crystals for crystallization on specific enchant level
	 * @return int
	 */
	public final int getCrystalCount(int enchantLevel)
	{
		if (enchantLevel > 3)
			switch (_type2)
			{
				case TYPE2_SHIELD_ARMOR:
				case TYPE2_ACCESSORY:
					return _crystalCount + crystalEnchantBonusArmor[getCrystalType()] * (3 * enchantLevel - 6);
				case TYPE2_WEAPON:
					return _crystalCount + crystalEnchantBonusWeapon[getCrystalType()] * (2 * enchantLevel - 3);
				default:
					return _crystalCount;
			}
		else if (enchantLevel > 0)
			switch (_type2)
			{
				case TYPE2_SHIELD_ARMOR:
				case TYPE2_ACCESSORY:
					return _crystalCount + crystalEnchantBonusArmor[getCrystalType()] * enchantLevel;
				case TYPE2_WEAPON:
					return _crystalCount + crystalEnchantBonusWeapon[getCrystalType()] * enchantLevel;
				default:
					return _crystalCount;
			}
		else
			return _crystalCount;
	}
	
	/**
	 * Returns the name of the item
	 * @return String
	 */
	public final String getName()
	{
		return _name;
	}
	
	/**
	 * Returns the base elemental of the item
	 * @return Elementals
	 */
	public final Elementals[] getElementals()
	{
		return _elementals;
	}
	
	public Elementals getElemental(byte attribute)
	{
		for (Elementals elm : _elementals)
		{
			if (elm.getElement() == attribute)
				return elm;
		}
		return null;
	}
	
	/**
	 * Sets the base elemental of the item
	 */
	public void setElementals(Elementals element)
	{
		if (_elementals == null)
		{
			_elementals = new Elementals[1];
			_elementals[0] = element;
		}
		else
		{
			Elementals elm = getElemental(element.getElement());
			if (elm != null)
			{
				elm.setValue(element.getValue());
			}
			else
			{
				elm = element;
				Elementals[] array = new Elementals[_elementals.length + 1];
				System.arraycopy(_elementals, 0, array, 0, _elementals.length);
				array[_elementals.length] = elm;
				_elementals = array;
			}
		}
	}
	
	/**
	 * Return the part of the body used with the item.
	 * @return int
	 */
	public final int getBodyPart()
	{
		return _bodyPart;
	}	
	/**
	 * Returns the type 1 of the item
	 * @return int
	 */
	public final int getType1()
	{
		return _type1;
	}
	
	/**
	 * Returns if the item is stackable
	 * @return boolean
	 */
	public final boolean isStackable()
	{
		return _stackable;
	}
	
	/**
	 * Returns if the item is consumable
	 * @return boolean
	 */
	public boolean isConsumable()
	{
		return false;
	}
	
	public boolean isEquipable()
	{
		return this.getBodyPart() != 0 && !(this.getItemType() instanceof L2EtcItemType);
	}
	
	/**
	 * Returns the price of reference of the item
	 * @return int
	 */
	public final int getReferencePrice()
	{
		return (isConsumable() ? (int)(_referencePrice * Config.RATE_CONSUMABLE_COST) : _referencePrice);
	}
	
	/**
	 * Returns if the item can be sold
	 * @return boolean
	 */
	public final boolean isSellable()
	{
		return _sellable;
	}
	
	/**
	 * Returns if the item can dropped
	 * @return boolean
	 */
	public final boolean isDropable()
	{
		return _dropable;
	}
	
	/**
	 * Returns if the item can destroy
	 * @return boolean
	 */
	public final boolean isDestroyable()
	{
		return _destroyable;
	}
	
	/**
	 * Returns if the item can add to trade
	 * @return boolean
	 */
	public final boolean isTradeable()
	{
		return _tradeable;
	}
	
	/**
	 * Returns if the item can be put into warehouse
	 * @return boolean
	 */
	public final boolean isDepositable()
	{
		return _depositable;
	}
	
	/**
	 * Returns if item is common
	 * @return boolean
	 */
	public final boolean isCommon()
	{
		return _common;
	}
	
	/**
	 * Returns if item is hero-only
	 * @return
	 */
	public final boolean isHeroItem()
	{
		return _heroItem;
	}
	
	/**
	 * Returns if item is pvp
	 * @return
	 */
	public final boolean isPvpItem()
	{
		return _pvpItem;
	}
	
	public boolean isPotion() 
	{ 
		return (getItemType() == L2EtcItemType.POTION);
	}

	public boolean isElixir() 
	{ 
		return (getItemType() == L2EtcItemType.ELIXIR);
	}

	/**
	 * Returns array of Func objects containing the list of functions used by the item
	 * @param instance : L2ItemInstance pointing out the item
	 * @param player : L2Character pointing out the player
	 * @return Func[] : array of functions
	 */
	public Func[] getStatFuncs(L2ItemInstance instance, L2Character player)
	{
		if (_funcTemplates == null || _funcTemplates.length == 0)
			return _emptyFunctionSet;
		
		ArrayList<Func> funcs = new ArrayList<Func>(_funcTemplates.length);
		
		Env env = new Env();
		env.player = player;
		env.target = player;
		env.item = instance;
		
		Func f;
		
		for (FuncTemplate t : _funcTemplates)
		{
			f = t.getFunc(env, this); // skill is owner
			if (f != null)
				funcs.add(f);
		}
		
		if (funcs.isEmpty())
			return _emptyFunctionSet;
		
		return funcs.toArray(new Func[funcs.size()]);
	}
	
	/**
	 * Returns the effects associated with the item.
	 * @param instance : L2ItemInstance pointing out the item
	 * @param player : L2Character pointing out the player
	 * @return L2Effect[] : array of effects generated by the item
	 */
	public L2Effect[] getEffects(L2ItemInstance instance, L2Character player)
	{
		if (_effectTemplates == null || _effectTemplates.length == 0)
			return _emptyEffectSet;
		
		FastList<L2Effect> effects = FastList.newInstance();
		
		Env env = new Env();
		env.player = player;
		env.target = player;
		env.item = instance;
		
		L2Effect e;
		
		for (EffectTemplate et : _effectTemplates)
		{
			
			e = et.getEffect(env);
			if (e != null)
			{
				e.scheduleEffect();
				effects.add(e);
			}
		}
		
		if (effects.isEmpty())
			return _emptyEffectSet;
		
		L2Effect[] result = effects.toArray(new L2Effect[effects.size()]);
		FastList.recycle(effects);
		return result;
	}
	
	/**
	 * Returns effects of skills associated with the item.
	 * @param caster : L2Character pointing out the caster
	 * @param target : L2Character pointing out the target
	 * @return L2Effect[] : array of effects generated by the skill
	 
	public L2Effect[] getSkillEffects(L2Character caster, L2Character target)
	{
		if (_skills == null)
			return _emptyEffectSet;
		List<L2Effect> effects = new FastList<L2Effect>();
		
		for (L2Skill skill : _skills)
		{
			if (!skill.checkCondition(caster, target, true))
				continue; // Skill condition not met
				
			if (target.getFirstEffect(skill.getId()) != null)
				target.removeEffect(target.getFirstEffect(skill.getId()));
			for (L2Effect e : skill.getEffects(caster, target))
				effects.add(e);
		}
		if (effects.isEmpty())
			return _emptyEffectSet;
		return effects.toArray(new L2Effect[effects.size()]);
	}
	 */
	/**
	 * Add the FuncTemplate f to the list of functions used with the item
	 * @param f : FuncTemplate to add
	 */
	public void attach(FuncTemplate f)
	{
		switch(f.stat)
		{
			case FIRE_RES:
			case FIRE_POWER:
				setElementals(new Elementals(Elementals.FIRE, (int) f.lambda.calc(null)));
				break;
			case WATER_RES:
			case WATER_POWER:
				setElementals(new Elementals(Elementals.WATER, (int) f.lambda.calc(null)));
				break;
			case WIND_RES:
			case WIND_POWER:
				setElementals(new Elementals(Elementals.WIND, (int) f.lambda.calc(null)));
				break;
			case EARTH_RES:
			case EARTH_POWER:
				setElementals(new Elementals(Elementals.EARTH, (int) f.lambda.calc(null)));
				break;
			case HOLY_RES:
			case HOLY_POWER:
				setElementals(new Elementals(Elementals.HOLY, (int) f.lambda.calc(null)));
				break;
			case DARK_RES:
			case DARK_POWER:
				setElementals(new Elementals(Elementals.DARK, (int) f.lambda.calc(null)));
				break;
		}
		// If _functTemplates is empty, create it and add the FuncTemplate f in it
		if (_funcTemplates == null)
		{
			_funcTemplates = new FuncTemplate[]
			                                  {
					f
			                                  };
		}
		else
		{
			int len = _funcTemplates.length;
			FuncTemplate[] tmp = new FuncTemplate[len + 1];
			// Definition : arraycopy(array source, begins copy at this position of source, array destination, begins copy at this position in dest,
			//						  number of components to be copied)
			System.arraycopy(_funcTemplates, 0, tmp, 0, len);
			tmp[len] = f;
			_funcTemplates = tmp;
		}
	}
	
	/**
	 * Add the EffectTemplate effect to the list of effects generated by the item
	 * @param effect : EffectTemplate
	 */
	public void attach(EffectTemplate effect)
	{
		if (_effectTemplates == null)
		{
			_effectTemplates = new EffectTemplate[]
			                                      {
					effect
			                                      };
		}
		else
		{
			int len = _effectTemplates.length;
			EffectTemplate[] tmp = new EffectTemplate[len + 1];
			// Definition : arraycopy(array source, begins copy at this position of source, array destination, begins copy at this position in dest,
			//						  number of components to be copied)
			System.arraycopy(_effectTemplates, 0, tmp, 0, len);
			tmp[len] = effect;
			_effectTemplates = tmp;
		}
	}
	
	public final void attach(Condition c)
	{
		if (_preConditions == null)
			_preConditions = new FastList<Condition>();
		if (!_preConditions.contains(c))
			_preConditions.add(c);
	}
	
	public abstract SkillHolder[] getSkills();
	
	public boolean checkCondition(L2Character activeChar, L2Object target, boolean sendMessage)
	{
		if (activeChar.isGM() && !Config.GM_ITEM_RESTRICTION)
			return true;
		
		if (_preConditions == null)
			return true;
		
		Env env = new Env();
		env.player = activeChar;
		if (target instanceof L2Character)
			env.target = (L2Character)target;
		
		for (Condition preCondition : _preConditions)
		{
			if (preCondition == null)
				continue;
			
			if (!preCondition.test(env))
			{
				if (activeChar instanceof L2Summon)
				{
					activeChar.getActingPlayer().sendPacket(SystemMessage.getSystemMessage(SystemMessageId.PET_CANNOT_USE_ITEM));
					return false;
				}
				
				if (sendMessage)
				{
					String msg = preCondition.getMessage();
					int msgId = preCondition.getMessageId();
					if (msg != null)
					{
						activeChar.sendMessage(msg);
					}
					else if (msgId !=0)
					{
						SystemMessage sm = SystemMessage.getSystemMessage(msgId);
						if (preCondition.isAddName())
							sm.addItemName(_itemId);
						activeChar.getActingPlayer().sendPacket(sm);
					}
				}
				return false;
			}
		}
		return true;
	}
	
	public boolean isConditionAttached()
	{
		return _preConditions != null && !_preConditions.isEmpty();
	}
	
	public boolean isQuestItem()
	{
		return _questItem;
	}

	/**
	 * Returns the name of the item
	 * @return String
	 */
	@Override
	public String toString()
	{
		return _name+"("+_itemId+")";
	}

	/**
	 * @return the _ex_immediate_effect
	 */
	public boolean is_ex_immediate_effect()
	{
		return _ex_immediate_effect;
	}

	/**
	 * @return the _default_action
	 */
	public L2ActionType getDefaultAction()
	{
		return _defaultAction;
	}

	/**
	 * Get the icon link in client files.<BR> Usable in HTML windows.
	 * @return the _icon
	 */
	public String getIcon()
	{
		return _icon;
	}
}
