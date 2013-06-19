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
package com.l2jserver.gameserver.model.skills;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2jserver.Config;
import com.l2jserver.gameserver.GeoData;
import com.l2jserver.gameserver.datatables.SkillTable;
import com.l2jserver.gameserver.datatables.SkillTreesData;
import com.l2jserver.gameserver.handler.ITargetTypeHandler;
import com.l2jserver.gameserver.handler.TargetHandler;
import com.l2jserver.gameserver.model.ChanceCondition;
import com.l2jserver.gameserver.model.L2ExtractableProductItem;
import com.l2jserver.gameserver.model.L2ExtractableSkill;
import com.l2jserver.gameserver.model.L2Object;
import com.l2jserver.gameserver.model.PcCondOverride;
import com.l2jserver.gameserver.model.StatsSet;
import com.l2jserver.gameserver.model.actor.L2Attackable;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.L2Playable;
import com.l2jserver.gameserver.model.actor.L2Summon;
import com.l2jserver.gameserver.model.actor.instance.L2CubicInstance;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.actor.instance.L2SiegeFlagInstance;
import com.l2jserver.gameserver.model.conditions.Condition;
import com.l2jserver.gameserver.model.effects.EffectTemplate;
import com.l2jserver.gameserver.model.effects.L2Effect;
import com.l2jserver.gameserver.model.effects.L2EffectType;
import com.l2jserver.gameserver.model.entity.TvTEvent;
import com.l2jserver.gameserver.model.holders.ItemHolder;
import com.l2jserver.gameserver.model.interfaces.IChanceSkillTrigger;
import com.l2jserver.gameserver.model.skills.funcs.Func;
import com.l2jserver.gameserver.model.skills.funcs.FuncTemplate;
import com.l2jserver.gameserver.model.skills.targets.L2TargetType;
import com.l2jserver.gameserver.model.stats.BaseStats;
import com.l2jserver.gameserver.model.stats.Env;
import com.l2jserver.gameserver.model.stats.Formulas;
import com.l2jserver.gameserver.model.zone.ZoneId;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;
import com.l2jserver.gameserver.util.Util;
import com.l2jserver.util.Rnd;

public abstract class L2Skill implements IChanceSkillTrigger
{
	protected static final Logger _log = Logger.getLogger(L2Skill.class.getName());
	
	private static final L2Object[] EMPTY_TARGET_LIST = new L2Object[0];
	private static final List<L2Effect> EMPTY_EFFECT_LIST = Collections.<L2Effect> emptyList();
	
	public static final int SKILL_CREATE_DWARVEN = 172;
	public static final int SKILL_EXPERTISE = 239;
	public static final int SKILL_CRYSTALLIZE = 248;
	public static final int SKILL_CLAN_LUCK = 390;
	public static final int SKILL_SOUL_MASTERY = 467;
	public static final int SKILL_ONYX_BEAST_TRANSFORMATION = 617;
	public static final int SKILL_CREATE_COMMON = 1320;
	public static final int SKILL_DIVINE_INSPIRATION = 1405;
	public static final int SKILL_NPC_RACE = 4416;
	
	public static final boolean geoEnabled = Config.GEODATA > 0;
	
	/** Skill Id. */
	private final int _id;
	/** Skill level. */
	private final int _level;
	/** Custom skill Id displayed by the client. */
	private final int _displayId;
	/** Custom skill level displayed by the client. */
	private final int _displayLevel;
	/** Skill client's name. */
	private final String _name;
	/** Operative type: passive, active, toggle. */
	private final L2SkillOpType _operateType;
	private final int _magic;
	private final L2TraitType _traitType;
	private final boolean _staticReuse;
	/** MP consumption. */
	private final int _mpConsume;
	/** Initial MP consumption. */
	private final int _mpInitialConsume;
	/** HP consumption. */
	private final int _hpConsume;
	/** Amount of items consumed by this skill from caster. */
	private final int _itemConsumeCount;
	/** Id of item consumed by this skill from caster. */
	private final int _itemConsumeId;
	/** Cast range: how far can be the target. */
	private final int _castRange;
	/** Effect range: how far the skill affect the target. */
	private final int _effectRange;
	/** Abnormal instant, used for herbs mostly. */
	private final boolean _isAbnormalInstant;
	/** Abnormal level, global effect level. */
	private final int _abnormalLvl;
	/** Abnormal type: global effect "group". */
	private final AbnormalType _abnormalType;
	/** Abnormal time: global effect duration time. */
	private final int _abnormalTime;
	/** Abnormal type set for abnormal types that this effect skill blocks. */
	private final Set<AbnormalType> _blockBuffSlots;
	/** If {@code true} this skill's effect should stay after death. */
	private final boolean _stayAfterDeath;
	/** If {@code true} this skill's effect should stay after class-subclass change. */
	private final boolean _stayOnSubclassChange;
	/** If {@code true} this skill might kill by damage over time. */
	private final boolean _killByDOT;
	
	private final int _refId;
	// all times in milliseconds
	private final int _hitTime;
	private final int[] _hitTimings;
	// private final int _skillInterruptTime;
	private final int _coolTime;
	private final int _reuseHashCode;
	private final int _reuseDelay;
	
	/** Target type of the skill : SELF, PARTY, CLAN, PET... */
	private final L2TargetType _targetType;
	private final int _feed;
	// base success chance
	private final double _power;
	private final double _pvpPower;
	private final double _pvePower;
	private final int _magicLevel;
	private final int _lvlBonusRate;
	private final int _minChance;
	private final int _maxChance;
	private final int _blowChance;
	
	// Effecting area of the skill, in radius.
	// The radius center varies according to the _targetType:
	// "caster" if targetType = AURA/PARTY/CLAN or "target" if targetType = AREA
	private final int _affectRange;
	private final int[] _affectLimit = new int[2];
	
	private final L2SkillType _skillType;
	private final int _effectId;
	private final int _effectLvl; // normal effect level
	
	private final boolean _nextActionIsAttack;
	
	private final boolean _removedOnAnyActionExceptMove;
	private final boolean _removedOnDamage;
	
	private final byte _element;
	private final int _elementPower;
	
	private final BaseStats _saveVs;
	
	private final boolean _overhit;
	
	private final int _minPledgeClass;
	private final boolean _isOffensive;
	private final boolean _isPVP;
	private final int _chargeConsume;
	private final int _triggeredId;
	private final int _triggeredLevel;
	private final String _chanceType;
	private final int _soulMaxConsume;
	private final int _numSouls;
	private final int _expNeeded;
	private final boolean _dependOnTargetBuff;
	
	private final int _afterEffectId;
	private final int _afterEffectLvl;
	private final boolean _isHeroSkill; // If true the skill is a Hero Skill
	private final boolean _isGMSkill; // True if skill is GM skill
	private final boolean _isSevenSigns;
	
	private final int _baseCritRate; // percent of success for skill critical hit (especially for PhysicalAttack & Blow - they're not affected by rCrit values or buffs).
	private final int _halfKillRate;
	private final int _lethalStrikeRate;
	private final boolean _directHpDmg; // If true then damage is being make directly
	private final boolean _isTriggeredSkill; // If true the skill will take activation buff slot instead of a normal buff slot
	private final int _effectPoint;
	// Condition lists
	private List<Condition> _preCondition;
	private List<Condition> _itemPreCondition;
	// Function lists
	private List<FuncTemplate> _funcTemplates;
	// Effect lists
	private List<EffectTemplate> _effectTemplates;
	private List<EffectTemplate> _effectTemplatesSelf;
	private List<EffectTemplate> _effectTemplatesPassive;
	
	protected ChanceCondition _chanceCondition = null;
	
	// Flying support
	private final String _flyType;
	private final int _flyRadius;
	private final float _flyCourse;
	
	private final boolean _isDebuff;
	
	private final String _attribute;
	
	private final boolean _ignoreShield;
	
	private final boolean _isSuicideAttack;
	private final boolean _canBeDispeled;
	
	private final boolean _isClanSkill;
	private final boolean _excludedFromCheck;
	private final boolean _simultaneousCast;
	
	private L2ExtractableSkill _extractableItems = null;
	
	private int _npcId = 0;
	
	private final String _icon;
	
	private byte[] _effectTypes;
	
	protected L2Skill(StatsSet set)
	{
		_isAbnormalInstant = set.getBool("abnormalInstant", false);
		_id = set.getInteger("skill_id");
		_level = set.getInteger("level");
		_refId = set.getInteger("referenceId", 0);
		_displayId = set.getInteger("displayId", _id);
		_displayLevel = set.getInteger("displayLevel", _level);
		_name = set.getString("name", "");
		_operateType = set.getEnum("operateType", L2SkillOpType.class);
		_magic = set.getInteger("isMagic", 0);
		_traitType = set.getEnum("trait", L2TraitType.class, L2TraitType.NONE);
		_staticReuse = set.getBool("staticReuse", false);
		_mpConsume = set.getInteger("mpConsume", 0);
		_mpInitialConsume = set.getInteger("mpInitialConsume", 0);
		_hpConsume = set.getInteger("hpConsume", 0);
		_itemConsumeCount = set.getInteger("itemConsumeCount", 0);
		_itemConsumeId = set.getInteger("itemConsumeId", 0);
		_afterEffectId = set.getInteger("afterEffectId", 0);
		_afterEffectLvl = set.getInteger("afterEffectLvl", 1);
		
		_castRange = set.getInteger("castRange", -1);
		_effectRange = set.getInteger("effectRange", -1);
		_abnormalLvl = set.getInteger("abnormalLvl", 0);
		_abnormalType = set.getEnum("abnormalType", AbnormalType.class, AbnormalType.NONE);
		
		int abnormalTime = set.getInteger("abnormalTime", 1); // TODO: Should be 0, but instant effects need it until implementation is done.
		if (Config.ENABLE_MODIFY_SKILL_DURATION && Config.SKILL_DURATION_LIST.containsKey(getId()))
		{
			if ((getLevel() < 100) || (getLevel() > 140))
			{
				abnormalTime = Config.SKILL_DURATION_LIST.get(getId());
			}
			else if ((getLevel() >= 100) && (getLevel() < 140))
			{
				abnormalTime += Config.SKILL_DURATION_LIST.get(getId());
			}
		}
		
		_abnormalTime = abnormalTime;
		_attribute = set.getString("attribute", "");
		
		String blockBuffSlots = set.getString("blockBuffSlot", null);
		if ((blockBuffSlots != null) && !blockBuffSlots.isEmpty())
		{
			_blockBuffSlots = new HashSet<>();
			for (String slot : blockBuffSlots.split(";"))
			{
				_blockBuffSlots.add(AbnormalType.getAbnormalType(slot));
			}
		}
		else
		{
			_blockBuffSlots = Collections.<AbnormalType> emptySet();
		}
		
		_stayAfterDeath = set.getBool("stayAfterDeath", false);
		_stayOnSubclassChange = set.getBool("stayOnSubclassChange", true);
		
		_killByDOT = set.getBool("killByDOT", false);
		_hitTime = set.getInteger("hitTime", 0);
		String hitTimings = set.getString("hitTimings", null);
		if (hitTimings != null)
		{
			try
			{
				String[] valuesSplit = hitTimings.split(",");
				_hitTimings = new int[valuesSplit.length];
				for (int i = 0; i < valuesSplit.length; i++)
				{
					_hitTimings[i] = Integer.parseInt(valuesSplit[i]);
				}
			}
			catch (Exception e)
			{
				throw new IllegalArgumentException("SkillId: " + _id + " invalid hitTimings value: " + hitTimings + ", \"percent,percent,...percent\" required");
			}
		}
		else
		{
			_hitTimings = new int[0];
		}
		
		_coolTime = set.getInteger("coolTime", 0);
		_isDebuff = set.getBool("isDebuff", false);
		_feed = set.getInteger("feed", 0);
		_reuseHashCode = SkillTable.getSkillHashCode(_id, _level);
		
		if (Config.ENABLE_MODIFY_SKILL_REUSE && Config.SKILL_REUSE_LIST.containsKey(_id))
		{
			if (Config.DEBUG)
			{
				_log.info("*** Skill " + _name + " (" + _level + ") changed reuse from " + set.getInteger("reuseDelay", 0) + " to " + Config.SKILL_REUSE_LIST.get(_id) + " seconds.");
			}
			_reuseDelay = Config.SKILL_REUSE_LIST.get(_id);
		}
		else
		{
			_reuseDelay = set.getInteger("reuseDelay", 0);
		}
		
		_affectRange = set.getInteger("affectRange", 0);
		
		final String affectLimit = set.getString("affectLimit", null);
		if (affectLimit != null)
		{
			try
			{
				String[] valuesSplit = affectLimit.split("-");
				_affectLimit[0] = Integer.parseInt(valuesSplit[0]);
				_affectLimit[1] = Integer.parseInt(valuesSplit[1]);
			}
			catch (Exception e)
			{
				throw new IllegalArgumentException("SkillId: " + _id + " invalid affectLimit value: " + affectLimit + ", \"percent-percent\" required");
			}
		}
		
		_targetType = set.getEnum("targetType", L2TargetType.class);
		_power = set.getFloat("power", 0.f);
		_pvpPower = set.getFloat("pvpPower", (float) getPower());
		_pvePower = set.getFloat("pvePower", (float) getPower());
		_magicLevel = set.getInteger("magicLvl", 0);
		_lvlBonusRate = set.getInteger("lvlBonusRate", 0);
		_minChance = set.getInteger("minChance", Config.MIN_ABNORMAL_STATE_SUCCESS_RATE);
		_maxChance = set.getInteger("maxChance", Config.MAX_ABNORMAL_STATE_SUCCESS_RATE);
		_ignoreShield = set.getBool("ignoreShld", false);
		_skillType = set.getEnum("skillType", L2SkillType.class, L2SkillType.DUMMY);
		_effectId = set.getInteger("effectId", 0);
		_effectLvl = set.getInteger("effectLevel", 0);
		
		_nextActionIsAttack = set.getBool("nextActionAttack", false);
		
		_removedOnAnyActionExceptMove = set.getBool("removedOnAnyActionExceptMove", false);
		_removedOnDamage = set.getBool("removedOnDamage", false);
		
		_element = set.getByte("element", (byte) -1);
		_elementPower = set.getInteger("elementPower", 0);
		
		_saveVs = set.getEnum("saveVs", BaseStats.class, BaseStats.NULL);
		
		_overhit = set.getBool("overHit", false);
		_isSuicideAttack = set.getBool("isSuicideAttack", false);
		
		_minPledgeClass = set.getInteger("minPledgeClass", 0);
		_isOffensive = set.getBool("offensive", false);
		_isPVP = set.getBool("pvp", false);
		_chargeConsume = set.getInteger("chargeConsume", 0);
		_triggeredId = set.getInteger("triggeredId", 0);
		_triggeredLevel = set.getInteger("triggeredLevel", 1);
		_chanceType = set.getString("chanceType", "");
		if (!_chanceType.isEmpty())
		{
			_chanceCondition = ChanceCondition.parse(set);
		}
		
		_numSouls = set.getInteger("num_souls", 0);
		_soulMaxConsume = set.getInteger("soulMaxConsumeCount", 0);
		_blowChance = set.getInteger("blowChance", 0);
		_expNeeded = set.getInteger("expNeeded", 0);
		
		_isHeroSkill = SkillTreesData.getInstance().isHeroSkill(_id, _level);
		_isGMSkill = SkillTreesData.getInstance().isGMSkill(_id, _level);
		_isSevenSigns = (_id > 4360) && (_id < 4367);
		_isClanSkill = SkillTreesData.getInstance().isClanSkill(_id, _level);
		
		_baseCritRate = set.getInteger("baseCritRate", 0);
		_halfKillRate = set.getInteger("halfKillRate", 0);
		_lethalStrikeRate = set.getInteger("lethalStrikeRate", 0);
		
		_directHpDmg = set.getBool("dmgDirectlyToHp", false);
		_isTriggeredSkill = set.getBool("isTriggeredSkill", false);
		_effectPoint = set.getInteger("effectPoint", 0);
		
		_flyType = set.getString("flyType", null);
		_flyRadius = set.getInteger("flyRadius", 0);
		_flyCourse = set.getFloat("flyCourse", 0);
		
		_canBeDispeled = set.getBool("canBeDispeled", true);
		
		_excludedFromCheck = set.getBool("excludedFromCheck", false);
		_dependOnTargetBuff = set.getBool("dependOnTargetBuff", false);
		_simultaneousCast = set.getBool("simultaneousCast", false);
		
		String capsuled_items = set.getString("capsuled_items_skill", null);
		if (capsuled_items != null)
		{
			if (capsuled_items.isEmpty())
			{
				_log.warning("Empty Extractable Item Skill data in Skill Id: " + _id);
			}
			
			_extractableItems = parseExtractableSkill(_id, _level, capsuled_items);
		}
		_npcId = set.getInteger("npcId", 0);
		_icon = set.getString("icon", "icon.skill0000");
	}
	
	public abstract void useSkill(L2Character caster, L2Object[] targets);
	
	public final L2SkillType getSkillType()
	{
		return _skillType;
	}
	
	public final L2TraitType getTraitType()
	{
		return _traitType;
	}
	
	public final byte getElement()
	{
		return _element;
	}
	
	public final int getElementPower()
	{
		return _elementPower;
	}
	
	/**
	 * Return the target type of the skill : SELF, PARTY, CLAN, PET...
	 * @return
	 */
	public final L2TargetType getTargetType()
	{
		return _targetType;
	}
	
	public final boolean isOverhit()
	{
		return _overhit;
	}
	
	public final boolean killByDOT()
	{
		return _killByDOT;
	}
	
	public final boolean isSuicideAttack()
	{
		return _isSuicideAttack;
	}
	
	public final boolean allowOnTransform()
	{
		return isPassive();
	}
	
	/**
	 * Return the power of the skill.
	 * @param activeChar
	 * @param target
	 * @param isPvP
	 * @param isPvE
	 * @return
	 */
	public final double getPower(L2Character activeChar, L2Character target, boolean isPvP, boolean isPvE)
	{
		if (activeChar == null)
		{
			return getPower(isPvP, isPvE);
		}
		
		if (hasEffectType(L2EffectType.DEATH_LINK))
		{
			return getPower(isPvP, isPvE) * (-((activeChar.getCurrentHp() * 2) / activeChar.getMaxHp()) + 2);
		}
		
		if (hasEffectType(L2EffectType.PHYSICAL_ATTACK_HP_LINK))
		{
			return getPower(isPvP, isPvE) * (-((target.getCurrentHp() * 2) / target.getMaxHp()) + 2);
		}
		return getPower(isPvP, isPvE);
	}
	
	public final double getPower()
	{
		return _power;
	}
	
	public final double getPower(boolean isPvP, boolean isPvE)
	{
		return isPvE ? _pvePower : isPvP ? _pvpPower : _power;
	}
	
	public final Set<AbnormalType> getBlockBuffSlots()
	{
		return _blockBuffSlots;
	}
	
	/**
	 * @return {@code true} if the skill is abnormal instant, {@code false} otherwise
	 */
	public final boolean isAbnormalInstant()
	{
		return _isAbnormalInstant;
	}
	
	public final int getAbnormalLvl()
	{
		return _abnormalLvl;
	}
	
	public final AbnormalType getAbnormalType()
	{
		return _abnormalType;
	}
	
	public final int getAbnormalTime()
	{
		return _abnormalTime;
	}
	
	public final int getMagicLevel()
	{
		return _magicLevel;
	}
	
	public final int getLvlBonusRate()
	{
		return _lvlBonusRate;
	}
	
	/**
	 * Return custom minimum skill/effect chance.
	 * @return
	 */
	public final int getMinChance()
	{
		return _minChance;
	}
	
	/**
	 * Return custom maximum skill/effect chance.
	 * @return
	 */
	public final int getMaxChance()
	{
		return _maxChance;
	}
	
	/**
	 * Return true if skill effects should be removed on any action except movement
	 * @return
	 */
	public final boolean isRemovedOnAnyActionExceptMove()
	{
		return _removedOnAnyActionExceptMove;
	}
	
	/**
	 * @return {@code true} if skill effects should be removed on damage
	 */
	public final boolean isRemovedOnDamage()
	{
		return _removedOnDamage;
	}
	
	/**
	 * Return the additional effect Id.
	 * @return
	 */
	public final int getEffectId()
	{
		return _effectId;
	}
	
	/**
	 * Return the additional effect level.
	 * @return
	 */
	public final int getEffectLvl()
	{
		return _effectLvl;
	}
	
	/**
	 * Return true if character should attack target after skill
	 * @return
	 */
	public final boolean nextActionIsAttack()
	{
		return _nextActionIsAttack;
	}
	
	/**
	 * @return Returns the castRange.
	 */
	public final int getCastRange()
	{
		return _castRange;
	}
	
	/**
	 * @return Returns the effectRange.
	 */
	public final int getEffectRange()
	{
		return _effectRange;
	}
	
	/**
	 * @return Returns the hpConsume.
	 */
	public final int getHpConsume()
	{
		return _hpConsume;
	}
	
	/**
	 * @return Returns the id.
	 */
	public final int getId()
	{
		return _id;
	}
	
	/**
	 * @return Returns the boolean _isDebuff.
	 */
	public final boolean isDebuff()
	{
		return _isDebuff;
	}
	
	public int getDisplayId()
	{
		return _displayId;
	}
	
	public int getDisplayLevel()
	{
		return _displayLevel;
	}
	
	public int getTriggeredId()
	{
		return _triggeredId;
	}
	
	public int getTriggeredLevel()
	{
		return _triggeredLevel;
	}
	
	public boolean triggerAnotherSkill()
	{
		return _triggeredId > 1;
	}
	
	/**
	 * Return skill saveVs base stat (STR, INT ...).
	 * @return
	 */
	public final BaseStats getSaveVs()
	{
		return _saveVs;
	}
	
	/**
	 * @return Returns the itemConsume.
	 */
	public final int getItemConsume()
	{
		return _itemConsumeCount;
	}
	
	/**
	 * @return Returns the itemConsumeId.
	 */
	public final int getItemConsumeId()
	{
		return _itemConsumeId;
	}
	
	/**
	 * @return Returns the level.
	 */
	public final int getLevel()
	{
		return _level;
	}
	
	/**
	 * @return Returns true to set physical skills.
	 */
	public final boolean isPhysical()
	{
		return _magic == 0;
	}
	
	/**
	 * @return Returns true to set magic skills.
	 */
	public final boolean isMagic()
	{
		return _magic == 1;
	}
	
	/**
	 * @return Returns true to set static skills.
	 */
	public final boolean isStatic()
	{
		return _magic == 2;
	}
	
	/**
	 * @return Returns true to set dance skills.
	 */
	public final boolean isDance()
	{
		return _magic == 3;
	}
	
	/**
	 * @return Returns true to set static reuse.
	 */
	public final boolean isStaticReuse()
	{
		return _staticReuse;
	}
	
	/**
	 * @return Returns the mpConsume.
	 */
	public final int getMpConsume()
	{
		return _mpConsume;
	}
	
	/**
	 * @return Returns the mpInitialConsume.
	 */
	public final int getMpInitialConsume()
	{
		return _mpInitialConsume;
	}
	
	/**
	 * @return the skill name
	 */
	public final String getName()
	{
		return _name;
	}
	
	/**
	 * @return the reuse delay
	 */
	public final int getReuseDelay()
	{
		return _reuseDelay;
	}
	
	public final int getReuseHashCode()
	{
		return _reuseHashCode;
	}
	
	public final int getHitTime()
	{
		return _hitTime;
	}
	
	public final int getHitCounts()
	{
		return _hitTimings.length;
	}
	
	public final int[] getHitTimings()
	{
		return _hitTimings;
	}
	
	/**
	 * @return the cool time
	 */
	public final int getCoolTime()
	{
		return _coolTime;
	}
	
	public final int getAffectRange()
	{
		return _affectRange;
	}
	
	public final int getAffectLimit()
	{
		return (_affectLimit[0] + Rnd.get(_affectLimit[1]));
	}
	
	public final boolean isActive()
	{
		return (_operateType != null) && _operateType.isActive();
	}
	
	public final boolean isPassive()
	{
		return (_operateType != null) && _operateType.isPassive();
	}
	
	public final boolean isToggle()
	{
		return (_operateType != null) && _operateType.isToggle();
	}
	
	public final boolean isChance()
	{
		return (_chanceCondition != null) && isPassive();
	}
	
	public final boolean isTriggeredSkill()
	{
		return _isTriggeredSkill;
	}
	
	public final int getEffectPoint()
	{
		return _effectPoint;
	}
	
	public final boolean useSoulShot()
	{
		return (hasEffectType(L2EffectType.PHYSICAL_ATTACK, L2EffectType.PHYSICAL_ATTACK_HP_LINK, L2EffectType.FATAL_BLOW, L2EffectType.ENERGY_ATTACK));
	}
	
	public final boolean useSpiritShot()
	{
		return _magic == 1;
	}
	
	public final boolean useFishShot()
	{
		return ((getSkillType() == L2SkillType.PUMPING) || (getSkillType() == L2SkillType.REELING));
	}
	
	public int getMinPledgeClass()
	{
		return _minPledgeClass;
	}
	
	public final boolean isOffensive()
	{
		return _isOffensive || isPVP();
	}
	
	public final boolean isPVP()
	{
		return _isPVP;
	}
	
	public final boolean isHeroSkill()
	{
		return _isHeroSkill;
	}
	
	public final boolean isGMSkill()
	{
		return _isGMSkill;
	}
	
	public final boolean is7Signs()
	{
		return _isSevenSigns;
	}
	
	public final int getChargeConsume()
	{
		return _chargeConsume;
	}
	
	public final int getNumSouls()
	{
		return _numSouls;
	}
	
	public final int getMaxSoulConsumeCount()
	{
		return _soulMaxConsume;
	}
	
	public final int getExpNeeded()
	{
		return _expNeeded;
	}
	
	public final int getBaseCritRate()
	{
		return _baseCritRate;
	}
	
	public final int getHalfKillRate()
	{
		return _halfKillRate;
	}
	
	public final int getLethalStrikeRate()
	{
		return _lethalStrikeRate;
	}
	
	public final boolean getDmgDirectlyToHP()
	{
		return _directHpDmg;
	}
	
	public final String getFlyType()
	{
		return _flyType;
	}
	
	public final int getFlyRadius()
	{
		return _flyRadius;
	}
	
	public final float getFlyCourse()
	{
		return _flyCourse;
	}
	
	public final boolean isStayAfterDeath()
	{
		return _stayAfterDeath;
	}
	
	public final boolean isStayOnSubclassChange()
	{
		return _stayOnSubclassChange;
	}
	
	public final boolean isBad()
	{
		return _effectPoint < 0;
	}
	
	public boolean checkCondition(L2Character activeChar, L2Object target, boolean itemOrWeapon)
	{
		if (activeChar.canOverrideCond(PcCondOverride.SKILL_CONDITIONS) && !Config.GM_SKILL_RESTRICTION)
		{
			return true;
		}
		
		final List<Condition> preCondition = itemOrWeapon ? _itemPreCondition : _preCondition;
		if ((preCondition == null) || preCondition.isEmpty())
		{
			return true;
		}
		
		final Env env = new Env();
		env.setCharacter(activeChar);
		if (target instanceof L2Character)
		{
			env.setTarget((L2Character) target);
		}
		env.setSkill(this);
		
		for (Condition cond : preCondition)
		{
			if (!cond.test(env))
			{
				final String msg = cond.getMessage();
				final int msgId = cond.getMessageId();
				if (msgId != 0)
				{
					final SystemMessage sm = SystemMessage.getSystemMessage(msgId);
					if (cond.isAddName())
					{
						sm.addSkillName(_id);
					}
					activeChar.sendPacket(sm);
				}
				else if (msg != null)
				{
					activeChar.sendMessage(msg);
				}
				return false;
			}
		}
		return true;
	}
	
	public final L2Object[] getTargetList(L2Character activeChar, boolean onlyFirst)
	{
		// Init to null the target of the skill
		L2Character target = null;
		
		// Get the L2Objcet targeted by the user of the skill at this moment
		L2Object objTarget = activeChar.getTarget();
		// If the L2Object targeted is a L2Character, it becomes the L2Character target
		if (objTarget instanceof L2Character)
		{
			target = (L2Character) objTarget;
		}
		
		return getTargetList(activeChar, onlyFirst, target);
	}
	
	/**
	 * Return all targets of the skill in a table in function a the skill type.<br>
	 * <B><U>Values of skill type</U>:</B>
	 * <ul>
	 * <li>ONE : The skill can only be used on the L2PcInstance targeted, or on the caster if it's a L2PcInstance and no L2PcInstance targeted</li>
	 * <li>SELF</li>
	 * <li>HOLY, UNDEAD</li>
	 * <li>PET</li>
	 * <li>AURA, AURA_CLOSE</li>
	 * <li>AREA</li>
	 * <li>MULTIFACE</li>
	 * <li>PARTY, CLAN</li>
	 * <li>CORPSE_PLAYER, CORPSE_MOB, CORPSE_CLAN</li>
	 * <li>UNLOCKABLE</li>
	 * <li>ITEM</li>
	 * <ul>
	 * @param activeChar The L2Character who use the skill
	 * @param onlyFirst
	 * @param target
	 * @return
	 */
	public final L2Object[] getTargetList(L2Character activeChar, boolean onlyFirst, L2Character target)
	{
		final ITargetTypeHandler handler = TargetHandler.getInstance().getHandler(getTargetType());
		if (handler != null)
		{
			try
			{
				return handler.getTargetList(this, activeChar, onlyFirst, target);
			}
			catch (Exception e)
			{
				_log.log(Level.WARNING, "Exception in L2Skill.getTargetList(): " + e.getMessage(), e);
			}
		}
		activeChar.sendMessage("Target type of skill is not currently handled.");
		return EMPTY_TARGET_LIST;
	}
	
	public final L2Object[] getTargetList(L2Character activeChar)
	{
		return getTargetList(activeChar, false);
	}
	
	public final L2Object getFirstOfTargetList(L2Character activeChar)
	{
		L2Object[] targets = getTargetList(activeChar, true);
		if (targets.length == 0)
		{
			return null;
		}
		return targets[0];
	}
	
	/**
	 * Check if should be target added to the target list false if target is dead, target same as caster,<br>
	 * target inside peace zone, target in the same party with caster, caster can see target Additional checks if not in PvP zones (arena, siege):<br>
	 * target in not the same clan and alliance with caster, and usual skill PvP check. If TvT event is active - performing additional checks. Caution: distance is not checked.
	 * @param caster
	 * @param target
	 * @param skill
	 * @param sourceInArena
	 * @return
	 */
	public static final boolean checkForAreaOffensiveSkills(L2Character caster, L2Character target, L2Skill skill, boolean sourceInArena)
	{
		if ((target == null) || target.isDead() || (target == caster))
		{
			return false;
		}
		
		final L2PcInstance player = caster.getActingPlayer();
		final L2PcInstance targetPlayer = target.getActingPlayer();
		if (player != null)
		{
			if (targetPlayer != null)
			{
				if ((targetPlayer == caster) || (targetPlayer == player))
				{
					return false;
				}
				
				if (targetPlayer.inObserverMode())
				{
					return false;
				}
				
				if (skill.isOffensive() && (player.getSiegeState() > 0) && player.isInsideZone(ZoneId.SIEGE) && (player.getSiegeState() == targetPlayer.getSiegeState()) && (player.getSiegeSide() == targetPlayer.getSiegeSide()))
				{
					return false;
				}
				
				if (skill.isOffensive() && target.isInsideZone(ZoneId.PEACE))
				{
					return false;
				}
				
				if (player.isInParty() && targetPlayer.isInParty())
				{
					// Same party
					if (player.getParty().getLeaderObjectId() == targetPlayer.getParty().getLeaderObjectId())
					{
						return false;
					}
					
					// Same command channel
					if (player.getParty().isInCommandChannel() && (player.getParty().getCommandChannel() == targetPlayer.getParty().getCommandChannel()))
					{
						return false;
					}
				}
				
				if (!TvTEvent.checkForTvTSkill(player, targetPlayer, skill))
				{
					return false;
				}
				
				if (!sourceInArena && !(targetPlayer.isInsideZone(ZoneId.PVP) && !targetPlayer.isInsideZone(ZoneId.SIEGE)))
				{
					if ((player.getAllyId() != 0) && (player.getAllyId() == targetPlayer.getAllyId()))
					{
						return false;
					}
					
					if ((player.getClanId() != 0) && (player.getClanId() == targetPlayer.getClanId()))
					{
						return false;
					}
					
					if (!player.checkPvpSkill(targetPlayer, skill, (caster instanceof L2Summon)))
					{
						return false;
					}
				}
			}
		}
		else
		{
			// target is mob
			if ((targetPlayer == null) && (target instanceof L2Attackable) && (caster instanceof L2Attackable))
			{
				String casterEnemyClan = ((L2Attackable) caster).getEnemyClan();
				if ((casterEnemyClan == null) || casterEnemyClan.isEmpty())
				{
					return false;
				}
				
				String targetClan = ((L2Attackable) target).getClan();
				if ((targetClan == null) || targetClan.isEmpty())
				{
					return false;
				}
				
				if (!casterEnemyClan.equals(targetClan))
				{
					return false;
				}
			}
		}
		
		if (geoEnabled && !GeoData.getInstance().canSeeTarget(caster, target))
		{
			return false;
		}
		return true;
	}
	
	public static final boolean addSummon(L2Character caster, L2PcInstance owner, int radius, boolean isDead)
	{
		if (!owner.hasSummon())
		{
			return false;
		}
		return addCharacter(caster, owner.getSummon(), radius, isDead);
	}
	
	public static final boolean addCharacter(L2Character caster, L2Character target, int radius, boolean isDead)
	{
		if (isDead != target.isDead())
		{
			return false;
		}
		
		if ((radius > 0) && !Util.checkIfInRange(radius, caster, target, true))
		{
			return false;
		}
		return true;
	}
	
	public final List<Func> getStatFuncs(L2Effect effect, L2Character player)
	{
		if (_funcTemplates == null)
		{
			return Collections.<Func> emptyList();
		}
		
		if (!(player instanceof L2Playable) && !(player instanceof L2Attackable))
		{
			return Collections.<Func> emptyList();
		}
		
		final List<Func> funcs = new ArrayList<>(_funcTemplates.size());
		final Env env = new Env();
		env.setCharacter(player);
		env.setSkill(this);
		for (FuncTemplate t : _funcTemplates)
		{
			Func f = t.getFunc(env, this); // skill is owner
			if (f != null)
			{
				funcs.add(f);
			}
		}
		return funcs;
	}
	
	public boolean hasEffects()
	{
		return (_effectTemplates != null) && !_effectTemplates.isEmpty();
	}
	
	public List<EffectTemplate> getEffectTemplates()
	{
		return _effectTemplates;
	}
	
	public List<EffectTemplate> getEffectTemplatesPassive()
	{
		return _effectTemplatesPassive;
	}
	
	public boolean hasSelfEffects()
	{
		return (_effectTemplatesSelf != null) && !_effectTemplatesSelf.isEmpty();
	}
	
	public boolean hasPassiveEffects()
	{
		return (_effectTemplatesPassive != null) && !_effectTemplatesPassive.isEmpty();
	}
	
	/**
	 * Env is used to pass parameters for secondary effects (shield and ss/bss/bsss)
	 * @param effector
	 * @param effected
	 * @param env
	 * @return an array with the effects that have been added to effector
	 */
	public final List<L2Effect> getEffects(L2Character effector, L2Character effected, Env env)
	{
		if ((effected == null) || !hasEffects() || isPassive())
		{
			return EMPTY_EFFECT_LIST;
		}
		
		// doors and siege flags cannot receive any effects
		if (effected.isDoor() || (effected instanceof L2SiegeFlagInstance))
		{
			return EMPTY_EFFECT_LIST;
		}
		
		if (effector != effected)
		{
			if (isOffensive() || isDebuff())
			{
				if (effected.isInvul())
				{
					return EMPTY_EFFECT_LIST;
				}
				
				if (effector.isPlayer() && effector.isGM())
				{
					if (!effector.getAccessLevel().canGiveDamage())
					{
						return EMPTY_EFFECT_LIST;
					}
				}
			}
		}
		
		if (env == null)
		{
			env = new Env();
		}
		env.setSkillMastery(Formulas.calcSkillMastery(effector, this));
		env.setCharacter(effector);
		env.setTarget(effected);
		env.setSkill(this);
		
		final List<L2Effect> effects = new ArrayList<>(_effectTemplates.size());
		for (EffectTemplate et : _effectTemplates)
		{
			final L2Effect e = et.getEffect(env);
			if (e != null)
			{
				if (e.calcSuccess())
				{
					e.scheduleEffect();
					effects.add(e);
				}
			}
		}
		effected.getEffectList().add(effects);
		return effects;
	}
	
	/**
	 * Warning: this method doesn't consider modifier (shield, ss, sps, bss) for secondary effects
	 * @param effector
	 * @param effected
	 * @return
	 */
	public final List<L2Effect> getEffects(L2Character effector, L2Character effected)
	{
		return getEffects(effector, effected, null);
	}
	
	public final List<L2Effect> getEffects(L2CubicInstance effector, L2Character effected, Env env)
	{
		if ((effector == null) || (effected == null) || !hasEffects() || isPassive())
		{
			return EMPTY_EFFECT_LIST;
		}
		
		if (effector.getOwner() != effected)
		{
			if (isDebuff() || isOffensive())
			{
				if (effected.isInvul())
				{
					return EMPTY_EFFECT_LIST;
				}
				
				if (effector.getOwner().isGM() && !effector.getOwner().getAccessLevel().canGiveDamage())
				{
					return EMPTY_EFFECT_LIST;
				}
			}
		}
		
		if (env == null)
		{
			env = new Env();
		}
		env.setCharacter(effector.getOwner());
		env.setCubic(effector);
		env.setTarget(effected);
		env.setSkill(this);
		
		final List<L2Effect> effects = new ArrayList<>(_effectTemplates.size());
		for (EffectTemplate et : _effectTemplates)
		{
			final L2Effect e = et.getEffect(env);
			if (e != null)
			{
				if (e.calcSuccess())
				{
					e.scheduleEffect();
					effects.add(e);
				}
			}
		}
		return effects;
	}
	
	public final List<L2Effect> getEffectsSelf(L2Character effector)
	{
		if ((effector == null) || !hasSelfEffects() || isPassive())
		{
			return EMPTY_EFFECT_LIST;
		}
		
		final Env env = new Env();
		env.setCharacter(effector);
		env.setTarget(effector);
		env.setSkill(this);
		
		final List<L2Effect> effects = new ArrayList<>(_effectTemplatesSelf.size());
		for (EffectTemplate et : _effectTemplatesSelf)
		{
			final L2Effect e = et.getEffect(env);
			if (e != null)
			{
				if (e.calcSuccess())
				{
					e.setSelfEffect();
					e.scheduleEffect();
					effects.add(e);
				}
			}
		}
		effector.getEffectList().add(effects);
		return effects;
	}
	
	public final List<L2Effect> getPassiveEffects(L2Character effector)
	{
		if ((effector == null) || !hasPassiveEffects())
		{
			return EMPTY_EFFECT_LIST;
		}
		
		final Env env = new Env();
		env.setCharacter(effector);
		env.setTarget(effector);
		env.setSkill(this);
		final List<L2Effect> effects = new ArrayList<>(_effectTemplatesPassive.size());
		for (EffectTemplate et : _effectTemplatesPassive)
		{
			final L2Effect e = et.getEffect(env);
			if (e != null)
			{
				if (e.calcSuccess())
				{
					e.scheduleEffect();
					effects.add(e);
				}
			}
		}
		effector.getEffectList().add(effects);
		return effects;
	}
	
	public final void attach(FuncTemplate f)
	{
		if (_funcTemplates == null)
		{
			_funcTemplates = new ArrayList<>(1);
		}
		_funcTemplates.add(f);
	}
	
	public final void attach(EffectTemplate effect)
	{
		if (_effectTemplates == null)
		{
			_effectTemplates = new ArrayList<>(1);
		}
		_effectTemplates.add(effect);
	}
	
	public final void attachSelf(EffectTemplate effect)
	{
		if (_effectTemplatesSelf == null)
		{
			_effectTemplatesSelf = new ArrayList<>(1);
		}
		_effectTemplatesSelf.add(effect);
	}
	
	public final void attachPassive(EffectTemplate effect)
	{
		if (_effectTemplatesPassive == null)
		{
			_effectTemplatesPassive = new ArrayList<>(1);
		}
		_effectTemplatesPassive.add(effect);
	}
	
	public final void attach(Condition c, boolean itemOrWeapon)
	{
		if (itemOrWeapon)
		{
			if (_itemPreCondition == null)
			{
				_itemPreCondition = new ArrayList<>();
			}
			_itemPreCondition.add(c);
		}
		else
		{
			if (_preCondition == null)
			{
				_preCondition = new ArrayList<>();
			}
			_preCondition.add(c);
		}
	}
	
	@Override
	public String toString()
	{
		return "Skill " + _name + "(" + _id + "," + _level + ")";
	}
	
	/**
	 * @return pet food
	 */
	public int getFeed()
	{
		return _feed;
	}
	
	/**
	 * used for tracking item id in case that item consume cannot be used
	 * @return reference item id
	 */
	public int getReferenceItemId()
	{
		return _refId;
	}
	
	public int getAfterEffectId()
	{
		return _afterEffectId;
	}
	
	public int getAfterEffectLvl()
	{
		return _afterEffectLvl;
	}
	
	@Override
	public boolean triggersChanceSkill()
	{
		return (_triggeredId > 0) && isChance();
	}
	
	@Override
	public int getTriggeredChanceId()
	{
		return _triggeredId;
	}
	
	@Override
	public int getTriggeredChanceLevel()
	{
		return _triggeredLevel;
	}
	
	@Override
	public ChanceCondition getTriggeredChanceCondition()
	{
		return _chanceCondition;
	}
	
	public String getAttributeName()
	{
		return _attribute;
	}
	
	/**
	 * @return the _blowChance
	 */
	public int getBlowChance()
	{
		return _blowChance;
	}
	
	public boolean ignoreShield()
	{
		return _ignoreShield;
	}
	
	public boolean canBeDispeled()
	{
		return _canBeDispeled;
	}
	
	public boolean isClanSkill()
	{
		return _isClanSkill;
	}
	
	public boolean isExcludedFromCheck()
	{
		return _excludedFromCheck;
	}
	
	public boolean getDependOnTargetBuff()
	{
		return _dependOnTargetBuff;
	}
	
	public boolean isSimultaneousCast()
	{
		return _simultaneousCast;
	}
	
	/**
	 * Parse an extractable skill.
	 * @param skillId the skill Id
	 * @param skillLvl the skill level
	 * @param values the values to parse
	 * @return the parsed extractable skill
	 * @author Zoey76
	 */
	private L2ExtractableSkill parseExtractableSkill(int skillId, int skillLvl, String values)
	{
		final String[] prodLists = values.split(";");
		final List<L2ExtractableProductItem> products = new ArrayList<>();
		String[] prodData;
		for (String prodList : prodLists)
		{
			prodData = prodList.split(",");
			if (prodData.length < 3)
			{
				_log.warning("Extractable skills data: Error in Skill Id: " + skillId + " Level: " + skillLvl + " -> wrong seperator!");
			}
			List<ItemHolder> items = null;
			double chance = 0;
			int prodId = 0;
			int quantity = 0;
			final int lenght = prodData.length - 1;
			try
			{
				items = new ArrayList<>(lenght / 2);
				for (int j = 0; j < lenght; j++)
				{
					prodId = Integer.parseInt(prodData[j]);
					quantity = Integer.parseInt(prodData[j += 1]);
					if ((prodId <= 0) || (quantity <= 0))
					{
						_log.warning("Extractable skills data: Error in Skill Id: " + skillId + " Level: " + skillLvl + " wrong production Id: " + prodId + " or wrond quantity: " + quantity + "!");
					}
					items.add(new ItemHolder(prodId, quantity));
				}
				chance = Double.parseDouble(prodData[lenght]);
			}
			catch (Exception e)
			{
				_log.warning("Extractable skills data: Error in Skill Id: " + skillId + " Level: " + skillLvl + " -> incomplete/invalid production data or wrong seperator!");
			}
			products.add(new L2ExtractableProductItem(items, chance));
		}
		
		if (products.isEmpty())
		{
			_log.warning("Extractable skills data: Error in Skill Id: " + skillId + " Level: " + skillLvl + " -> There are no production items!");
		}
		return new L2ExtractableSkill(SkillTable.getSkillHashCode(skillId, skillLvl), products);
	}
	
	public L2ExtractableSkill getExtractableSkill()
	{
		return _extractableItems;
	}
	
	/**
	 * @return the _npcId
	 */
	public int getNpcId()
	{
		return _npcId;
	}
	
	/**
	 * @param types
	 * @return {@code true} if at least one of specified {@link L2EffectType} types present on the current skill's effects, {@code false} otherwise.
	 */
	public boolean hasEffectType(L2EffectType... types)
	{
		if (hasEffects() && (types != null) && (types.length > 0))
		{
			if (_effectTypes == null)
			{
				_effectTypes = new byte[_effectTemplates.size()];
				
				final Env env = new Env();
				env.setSkill(this);
				
				int i = 0;
				for (EffectTemplate et : _effectTemplates)
				{
					final L2Effect e = et.getEffect(env, true);
					if (e == null)
					{
						continue;
					}
					_effectTypes[i++] = (byte) e.getEffectType().ordinal();
				}
				Arrays.sort(_effectTypes);
			}
			
			for (L2EffectType type : types)
			{
				if (Arrays.binarySearch(_effectTypes, (byte) type.ordinal()) >= 0)
				{
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * @return icon of the current skill.
	 */
	public String getIcon()
	{
		return _icon;
	}
}