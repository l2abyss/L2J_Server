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
package com.l2jserver.gameserver.model.items;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javolution.util.FastList;

import com.l2jserver.gameserver.handler.ISkillHandler;
import com.l2jserver.gameserver.handler.SkillHandler;
import com.l2jserver.gameserver.model.L2Object;
import com.l2jserver.gameserver.model.StatsSet;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.conditions.Condition;
import com.l2jserver.gameserver.model.conditions.ConditionGameChance;
import com.l2jserver.gameserver.model.effects.L2Effect;
import com.l2jserver.gameserver.model.holders.SkillHolder;
import com.l2jserver.gameserver.model.items.instance.L2ItemInstance;
import com.l2jserver.gameserver.model.items.type.L2WeaponType;
import com.l2jserver.gameserver.model.quest.Quest;
import com.l2jserver.gameserver.model.quest.Quest.QuestEventType;
import com.l2jserver.gameserver.model.skills.L2Skill;
import com.l2jserver.gameserver.model.skills.funcs.Func;
import com.l2jserver.gameserver.model.skills.funcs.FuncTemplate;
import com.l2jserver.gameserver.model.stats.Env;
import com.l2jserver.gameserver.model.stats.Formulas;
import com.l2jserver.util.StringUtil;

/**
 * This class is dedicated to the management of weapons.
 */
public final class L2Weapon extends L2Item
{
	private final L2WeaponType _type;
	private final boolean _isMagicWeapon;
	private final int _rndDam;
	private final int _soulShotCount;
	private final int _spiritShotCount;
	private final int _mpConsume;
	/**
	 * Skill that activates when item is enchanted +4 (for duals).
	 */
	private SkillHolder _enchant4Skill = null;
	private final int _changeWeaponId;
	
	// Attached skills for Special Abilities
	private SkillHolder _skillsOnMagic;
	private Condition _skillsOnMagicCondition = null;
	private SkillHolder _skillsOnCrit;
	private Condition _skillsOnCritCondition = null;
	
	private final int _reducedSoulshot;
	private final int _reducedSoulshotChance;
	
	private final int _reducedMpConsume;
	private final int _reducedMpConsumeChance;
	
	private final boolean _isForceEquip;
	private final boolean _isAttackWeapon;
	private final boolean _useWeaponSkillsOnly;
	
	/**
	 * Constructor for Weapon.
	 * @param set the StatsSet designating the set of couples (key,value) characterizing the weapon.
	 */
	public L2Weapon(StatsSet set)
	{
		super(set);
		_type = L2WeaponType.valueOf(set.getString("weapon_type", "none").toUpperCase());
		_type1 = L2Item.TYPE1_WEAPON_RING_EARRING_NECKLACE;
		_type2 = L2Item.TYPE2_WEAPON;
		_isMagicWeapon = set.getBool("is_magic_weapon", false);
		_soulShotCount = set.getInteger("soulshots", 0);
		_spiritShotCount = set.getInteger("spiritshots", 0);
		_rndDam = set.getInteger("random_damage", 0);
		_mpConsume = set.getInteger("mp_consume", 0);
		
		String[] reduced_soulshots = set.getString("reduced_soulshot", "").split(",");
		_reducedSoulshotChance = (reduced_soulshots.length == 2) ? Integer.parseInt(reduced_soulshots[0]) : 0;
		_reducedSoulshot = (reduced_soulshots.length == 2) ? Integer.parseInt(reduced_soulshots[1]) : 0;
		
		String[] reduced_mpconsume = set.getString("reduced_mp_consume", "").split(",");
		_reducedMpConsumeChance = (reduced_mpconsume.length == 2) ? Integer.parseInt(reduced_mpconsume[0]) : 0;
		_reducedMpConsume = (reduced_mpconsume.length == 2) ? Integer.parseInt(reduced_mpconsume[1]) : 0;
		
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
					// Incorrect syntax, dont add new skill
					_log.info(StringUtil.concat("> Couldnt parse ", skill, " in weapon enchant skills! item ", toString()));
				}
				if ((id > 0) && (level > 0))
				{
					_enchant4Skill = new SkillHolder(id, level);
				}
			}
		}
		
		skill = set.getString("onmagic_skill", null);
		if (skill != null)
		{
			String[] info = skill.split("-");
			final int chance = set.getInteger("onmagic_chance", 100);
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
					_log.info(StringUtil.concat("> Couldnt parse ", skill, " in weapon onmagic skills! item ", toString()));
				}
				if ((id > 0) && (level > 0) && (chance > 0))
				{
					_skillsOnMagic = new SkillHolder(id, level);
					_skillsOnMagicCondition = new ConditionGameChance(chance);
				}
			}
		}
		
		skill = set.getString("oncrit_skill", null);
		if (skill != null)
		{
			String[] info = skill.split("-");
			final int chance = set.getInteger("oncrit_chance", 100);
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
					_log.info(StringUtil.concat("> Couldnt parse ", skill, " in weapon oncrit skills! item ", toString()));
				}
				if ((id > 0) && (level > 0) && (chance > 0))
				{
					_skillsOnCrit = new SkillHolder(id, level);
					_skillsOnCritCondition = new ConditionGameChance(chance);
				}
			}
		}
		
		_changeWeaponId = set.getInteger("change_weaponId", 0);
		_isForceEquip = set.getBool("isForceEquip", false);
		_isAttackWeapon = set.getBool("isAttackWeapon", true);
		_useWeaponSkillsOnly = set.getBool("useWeaponSkillsOnly", false);
	}
	
	/**
	 * @return the type of Weapon
	 */
	@Override
	public L2WeaponType getItemType()
	{
		return _type;
	}
	
	/**
	 * @return the ID of the Etc item after applying the mask.
	 */
	@Override
	public int getItemMask()
	{
		return getItemType().mask();
	}
	
	/**
	 * @return {@code true} if the weapon is magic, {@code false} otherwise.
	 */
	public boolean isMagicWeapon()
	{
		return _isMagicWeapon;
	}
	
	/**
	 * @return the quantity of SoulShot used.
	 */
	public int getSoulShotCount()
	{
		return _soulShotCount;
	}
	
	/**
	 * @return the quantity of SpiritShot used.
	 */
	public int getSpiritShotCount()
	{
		return _spiritShotCount;
	}
	
	/**
	 * @return the reduced quantity of SoultShot used.
	 */
	public int getReducedSoulShot()
	{
		return _reducedSoulshot;
	}
	
	/**
	 * @return the chance to use Reduced SoultShot.
	 */
	public int getReducedSoulShotChance()
	{
		return _reducedSoulshotChance;
	}
	
	/**
	 * @return the random damage inflicted by the weapon.
	 */
	public int getRandomDamage()
	{
		return _rndDam;
	}
	
	/**
	 * @return the MP consumption with the weapon.
	 */
	public int getMpConsume()
	{
		return _mpConsume;
	}
	
	/**
	 * @return the reduced MP consumption with the weapon.
	 */
	public int getReducedMpConsume()
	{
		return _reducedMpConsume;
	}
	
	/**
	 * @return the chance to use getReducedMpConsume()
	 */
	public int getReducedMpConsumeChance()
	{
		return _reducedMpConsumeChance;
	}
	
	/**
	 * @return the skill that player get when has equipped weapon +4 or more (for duals SA).
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
	 * @return the Id in which weapon this weapon can be changed.
	 */
	public int getChangeWeaponId()
	{
		return _changeWeaponId;
	}
	
	/**
	 * @return {@code true} if the weapon is force equip, {@code false} otherwise.
	 */
	public boolean isForceEquip()
	{
		return _isForceEquip;
	}
	
	/**
	 * @return {@code true} if the weapon is attack weapon, {@code false} otherwise.
	 */
	public boolean isAttackWeapon()
	{
		return _isAttackWeapon;
	}
	
	/**
	 * @return {@code true} if the weapon is skills only, {@code false} otherwise.
	 */
	public boolean useWeaponSkillsOnly()
	{
		return _useWeaponSkillsOnly;
	}
	
	/**
	 * @param item the L2ItemInstance pointing out the weapon.
	 * @param player the L2Character pointing out the player.
	 * @return an array of Func objects containing the list of functions used by the weapon.
	 */
	@Override
	public Func[] getStatFuncs(L2ItemInstance item, L2Character player)
	{
		if ((_funcTemplates == null) || (_funcTemplates.length == 0))
		{
			return _emptyFunctionSet;
		}
		
		ArrayList<Func> funcs = new ArrayList<>(_funcTemplates.length);
		
		Env env = new Env();
		env.setCharacter(player);
		env.setItem(item);
		
		Func f;
		for (FuncTemplate t : _funcTemplates)
		{
			f = t.getFunc(env, item);
			if (f != null)
			{
				funcs.add(f);
			}
		}
		return funcs.toArray(new Func[funcs.size()]);
	}
	
	/**
	 * @param caster the L2Character pointing out the caster
	 * @param target the L2Character pointing out the target
	 * @param crit the boolean tells whether the hit was critical
	 * @return the effects of skills associated with the item to be triggered onHit.
	 */
	public L2Effect[] getSkillEffects(L2Character caster, L2Character target, boolean crit)
	{
		if ((_skillsOnCrit == null) || !crit)
		{
			return _emptyEffectSet;
		}
		
		final List<L2Effect> effects = new FastList<>();
		final L2Skill onCritSkill = _skillsOnCrit.getSkill();
		if (_skillsOnCritCondition != null)
		{
			Env env = new Env();
			env.setCharacter(caster);
			env.setTarget(target);
			env.setSkill(onCritSkill);
			if (!_skillsOnCritCondition.test(env))
			{
				// Chance not met
				return _emptyEffectSet;
			}
		}
		
		if (!onCritSkill.checkCondition(caster, target, false))
		{
			// Skill condition not met
			return _emptyEffectSet;
		}
		
		final byte shld = Formulas.calcShldUse(caster, target, onCritSkill);
		if (!Formulas.calcSkillSuccess(caster, target, onCritSkill, shld, false, false, false))
		{
			// These skills should not work on RaidBoss
			return _emptyEffectSet;
		}
		if (target.getFirstEffect(onCritSkill.getId()) != null)
		{
			target.getFirstEffect(onCritSkill.getId()).exit();
		}
		for (L2Effect e : onCritSkill.getEffects(caster, target, new Env(shld, false, false, false)))
		{
			effects.add(e);
		}
		if (effects.isEmpty())
		{
			return _emptyEffectSet;
		}
		return effects.toArray(new L2Effect[effects.size()]);
	}
	
	/**
	 * @param caster the L2Character pointing out the caster
	 * @param target the L2Character pointing out the target
	 * @param trigger the L2Skill pointing out the skill triggering this action
	 * @return the effects of skills associated with the item to be triggered onMagic.
	 */
	public L2Effect[] getSkillEffects(L2Character caster, L2Character target, L2Skill trigger)
	{
		if (_skillsOnMagic == null)
		{
			return _emptyEffectSet;
		}
		
		final L2Skill onMagicSkill = _skillsOnMagic.getSkill();
		// No Trigger if Offensive Skill
		if (trigger.isOffensive() && onMagicSkill.isOffensive())
		{
			return _emptyEffectSet;
		}
		// No Trigger if not Magic Skill
		if (!trigger.isMagic() && !onMagicSkill.isMagic())
		{
			return _emptyEffectSet;
		}
		
		if (_skillsOnMagicCondition != null)
		{
			Env env = new Env();
			env.setCharacter(caster);
			env.setTarget(target);
			env.setSkill(onMagicSkill);
			if (!_skillsOnMagicCondition.test(env))
			{
				// Chance not met
				return _emptyEffectSet;
			}
		}
		
		if (!onMagicSkill.checkCondition(caster, target, false))
		{
			// Skill condition not met
			return _emptyEffectSet;
		}
		
		final byte shld = Formulas.calcShldUse(caster, target, onMagicSkill);
		if (onMagicSkill.isOffensive() && !Formulas.calcSkillSuccess(caster, target, onMagicSkill, shld, false, false, false))
		{
			return _emptyEffectSet;
		}
		
		L2Character[] targets =
		{
			target
		};
		
		// Launch the magic skill and calculate its effects
		// Get the skill handler corresponding to the skill type
		final ISkillHandler handler = SkillHandler.getInstance().getHandler(onMagicSkill.getSkillType());
		if (handler != null)
		{
			handler.useSkill(caster, onMagicSkill, targets);
		}
		else
		{
			onMagicSkill.useSkill(caster, targets);
		}
		
		// notify quests of a skill use
		if (caster instanceof L2PcInstance)
		{
			// Mobs in range 1000 see spell
			Collection<L2Object> objs = caster.getKnownList().getKnownObjects().values();
			for (L2Object spMob : objs)
			{
				if (spMob instanceof L2Npc)
				{
					L2Npc npcMob = (L2Npc) spMob;
					if (npcMob.getTemplate().getEventQuests(QuestEventType.ON_SKILL_SEE) != null)
					{
						for (Quest quest : npcMob.getTemplate().getEventQuests(QuestEventType.ON_SKILL_SEE))
						{
							quest.notifySkillSee(npcMob, caster.getActingPlayer(), onMagicSkill, targets, false);
						}
					}
				}
			}
		}
		return _emptyEffectSet;
	}
}
