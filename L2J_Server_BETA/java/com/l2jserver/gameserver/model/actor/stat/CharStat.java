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
package com.l2jserver.gameserver.model.actor.stat;

import com.l2jserver.Config;
import com.l2jserver.gameserver.model.Elementals;
import com.l2jserver.gameserver.model.PcCondOverride;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.items.L2Weapon;
import com.l2jserver.gameserver.model.items.instance.L2ItemInstance;
import com.l2jserver.gameserver.model.items.type.L2WeaponType;
import com.l2jserver.gameserver.model.skills.L2Skill;
import com.l2jserver.gameserver.model.stats.Calculator;
import com.l2jserver.gameserver.model.stats.Env;
import com.l2jserver.gameserver.model.stats.Stats;

public class CharStat
{
	private final L2Character _activeChar;
	private long _exp = 0;
	private int _sp = 0;
	private byte _level = 1;
	
	public CharStat(L2Character activeChar)
	{
		_activeChar = activeChar;
	}
	
	public final double calcStat(Stats stat, double init)
	{
		return calcStat(stat, init, null, null);
	}
	
	/**
	 * Calculate the new value of the state with modifiers that will be applied on the targeted L2Character.<BR>
	 * <B><U> Concept</U> :</B><BR
	 * A L2Character owns a table of Calculators called <B>_calculators</B>. Each Calculator (a calculator per state) own a table of Func object. A Func object is a mathematic function that permit to calculate the modifier of a state (ex : REGENERATE_HP_RATE...) : <BR>
	 * FuncAtkAccuracy -> Math.sqrt(_player.getDEX())*6+_player.getLevel()<BR>
	 * When the calc method of a calculator is launched, each mathematical function is called according to its priority <B>_order</B>.<br>
	 * Indeed, Func with lowest priority order is executed firsta and Funcs with the same order are executed in unspecified order.<br>
	 * The result of the calculation is stored in the value property of an Env class instance.<br>
	 * @param stat The stat to calculate the new value with modifiers
	 * @param init The initial value of the stat before applying modifiers
	 * @param target The L2Charcater whose properties will be used in the calculation (ex : CON, INT...)
	 * @param skill The L2Skill whose properties will be used in the calculation (ex : Level...)
	 * @return
	 */
	public final double calcStat(Stats stat, double init, L2Character target, L2Skill skill)
	{
		if ((_activeChar == null) || (stat == null))
		{
			return init;
		}
		
		int id = stat.ordinal();
		
		Calculator c = _activeChar.getCalculators()[id];
		
		// If no Func object found, no modifier is applied
		if ((c == null) || (c.size() == 0))
		{
			return init;
		}
		
		// Create and init an Env object to pass parameters to the Calculator
		Env env = new Env();
		env.setCharacter(_activeChar);
		env.setTarget(target);
		env.setSkill(skill);
		env.setValue(init);
		
		// Launch the calculation
		c.calc(env);
		// avoid some troubles with negative stats (some stats should never be negative)
		if (env.getValue() <= 0)
		{
			switch (stat)
			{
				case MAX_HP:
				case MAX_MP:
				case MAX_CP:
				case MAGIC_DEFENCE:
				case POWER_DEFENCE:
				case POWER_ATTACK:
				case MAGIC_ATTACK:
				case POWER_ATTACK_SPEED:
				case MAGIC_ATTACK_SPEED:
				case SHIELD_DEFENCE:
				case STAT_CON:
				case STAT_DEX:
				case STAT_INT:
				case STAT_MEN:
				case STAT_STR:
				case STAT_WIT:
					env.setValue(1);
			}
		}
		return env.getValue();
	}
	
	/**
	 * @return the Accuracy (base+modifier) of the L2Character in function of the Weapon Expertise Penalty.
	 */
	public int getAccuracy()
	{
		if (_activeChar == null)
		{
			return 0;
		}
		return (int) Math.round(calcStat(Stats.ACCURACY_COMBAT, 0, null, null));
	}
	
	public L2Character getActiveChar()
	{
		return _activeChar;
	}
	
	/**
	 * @return the Attack Speed multiplier (base+modifier) of the L2Character to get proper animations.
	 */
	public final float getAttackSpeedMultiplier()
	{
		if (_activeChar == null)
		{
			return 1;
		}
		
		return (float) (((1.1) * getPAtkSpd()) / _activeChar.getTemplate().getBasePAtkSpd());
	}
	
	/**
	 * @return the CON of the L2Character (base+modifier).
	 */
	public final int getCON()
	{
		if (_activeChar == null)
		{
			return 1;
		}
		
		return (int) calcStat(Stats.STAT_CON, _activeChar.getTemplate().getBaseCON());
	}
	
	/**
	 * @param target
	 * @param init
	 * @return the Critical Damage rate (base+modifier) of the L2Character.
	 */
	public final double getCriticalDmg(L2Character target, double init)
	{
		return calcStat(Stats.CRITICAL_DAMAGE, init, target, null);
	}
	
	/**
	 * @param target
	 * @param skill
	 * @return the Critical Hit rate (base+modifier) of the L2Character.
	 */
	public int getCriticalHit(L2Character target, L2Skill skill)
	{
		if (_activeChar == null)
		{
			return 1;
		}
		
		int criticalHit = (int) calcStat(Stats.CRITICAL_RATE, _activeChar.getTemplate().getBaseCritRate(), target, skill);
		// Set a cap of Critical Hit at 500
		return Math.min(criticalHit, Config.MAX_PCRIT_RATE);
	}
	
	/**
	 * @return the DEX of the L2Character (base+modifier).
	 */
	public final int getDEX()
	{
		if (_activeChar == null)
		{
			return 1;
		}
		return (int) calcStat(Stats.STAT_DEX, _activeChar.getTemplate().getBaseDEX());
	}
	
	/**
	 * @param target
	 * @return the Attack Evasion rate (base+modifier) of the L2Character.
	 */
	public int getEvasionRate(L2Character target)
	{
		if (_activeChar == null)
		{
			return 1;
		}
		
		int val = (int) Math.round(calcStat(Stats.EVASION_RATE, 0, target, null));
		if ((val > Config.MAX_EVASION) && !_activeChar.canOverrideCond(PcCondOverride.MAX_STATS_VALUE))
		{
			val = Config.MAX_EVASION;
		}
		return val;
	}
	
	public long getExp()
	{
		return _exp;
	}
	
	public void setExp(long value)
	{
		_exp = value;
	}
	
	/**
	 * @return the INT of the L2Character (base+modifier).
	 */
	public int getINT()
	{
		if (_activeChar == null)
		{
			return 1;
		}
		
		return (int) calcStat(Stats.STAT_INT, _activeChar.getTemplate().getBaseINT());
	}
	
	public byte getLevel()
	{
		return _level;
	}
	
	public void setLevel(byte value)
	{
		_level = value;
	}
	
	/**
	 * @param skill
	 * @return the Magical Attack range (base+modifier) of the L2Character.
	 */
	public final int getMagicalAttackRange(L2Skill skill)
	{
		if (_activeChar == null)
		{
			return 1;
		}
		
		if (skill != null)
		{
			return (int) calcStat(Stats.MAGIC_ATTACK_RANGE, skill.getCastRange(), null, skill);
		}
		
		return _activeChar.getTemplate().getBaseAtkRange();
	}
	
	public int getMaxCp()
	{
		if (_activeChar == null)
		{
			return 1;
		}
		
		return (int) calcStat(Stats.MAX_CP, _activeChar.getTemplate().getBaseCpMax());
	}
	
	public int getMaxRecoverableCp()
	{
		if (_activeChar == null)
		{
			return 1;
		}
		
		return (int) calcStat(Stats.MAX_RECOVERABLE_CP, getMaxCp());
	}
	
	public int getMaxHp()
	{
		if (_activeChar == null)
		{
			return 1;
		}
		
		return (int) calcStat(Stats.MAX_HP, _activeChar.getTemplate().getBaseHpMax());
	}
	
	public int getMaxRecoverableHp()
	{
		if (_activeChar == null)
		{
			return 1;
		}
		
		return (int) calcStat(Stats.MAX_RECOVERABLE_HP, getMaxHp());
	}
	
	public int getMaxMp()
	{
		if (_activeChar == null)
		{
			return 1;
		}
		
		return (int) calcStat(Stats.MAX_MP, _activeChar.getTemplate().getBaseMpMax());
	}
	
	public int getMaxRecoverableMp()
	{
		if (_activeChar == null)
		{
			return 1;
		}
		
		return (int) calcStat(Stats.MAX_RECOVERABLE_MP, getMaxMp());
	}
	
	/**
	 * Return the MAtk (base+modifier) of the L2Character.<br>
	 * <B><U>Example of use</U>: Calculate Magic damage
	 * @param target The L2Character targeted by the skill
	 * @param skill The L2Skill used against the target
	 * @return
	 */
	public int getMAtk(L2Character target, L2Skill skill)
	{
		if (_activeChar == null)
		{
			return 1;
		}
		
		float bonusAtk = 1;
		if (Config.L2JMOD_CHAMPION_ENABLE && _activeChar.isChampion())
		{
			bonusAtk = Config.L2JMOD_CHAMPION_ATK;
		}
		if (_activeChar.isRaid())
		{
			bonusAtk *= Config.RAID_MATTACK_MULTIPLIER;
		}
		
		// Calculate modifiers Magic Attack
		return (int) calcStat(Stats.MAGIC_ATTACK, _activeChar.getTemplate().getBaseMAtk() * bonusAtk, target, skill);
	}
	
	/**
	 * @return the MAtk Speed (base+modifier) of the L2Character in function of the Armour Expertise Penalty.
	 */
	public int getMAtkSpd()
	{
		if (_activeChar == null)
		{
			return 1;
		}
		float bonusSpdAtk = 1;
		if (Config.L2JMOD_CHAMPION_ENABLE && _activeChar.isChampion())
		{
			bonusSpdAtk = Config.L2JMOD_CHAMPION_SPD_ATK;
		}
		double val = calcStat(Stats.MAGIC_ATTACK_SPEED, _activeChar.getTemplate().getBaseMAtkSpd() * bonusSpdAtk);
		if ((val > Config.MAX_MATK_SPEED) && !_activeChar.canOverrideCond(PcCondOverride.MAX_STATS_VALUE))
		{
			val = Config.MAX_MATK_SPEED;
		}
		return (int) val;
	}
	
	/**
	 * @param target
	 * @param skill
	 * @return the Magic Critical Hit rate (base+modifier) of the L2Character.
	 */
	public final int getMCriticalHit(L2Character target, L2Skill skill)
	{
		if (_activeChar == null)
		{
			return 1;
		}
		
		double mrate = calcStat(Stats.MCRITICAL_RATE, 1, target, skill) * 10;
		// Set a cap of Magical Critical Hit at 200
		return (int) Math.min(mrate, Config.MAX_MCRIT_RATE);
	}
	
	/**
	 * <B><U>Example of use </U>: Calculate Magic damage.
	 * @param target The L2Character targeted by the skill
	 * @param skill The L2Skill used against the target
	 * @return the MDef (base+modifier) of the L2Character against a skill in function of abnormal effects in progress.
	 */
	public int getMDef(L2Character target, L2Skill skill)
	{
		if (_activeChar == null)
		{
			return 1;
		}
		
		// Get the base MDef of the L2Character
		double defence = _activeChar.getTemplate().getBaseMDef();
		
		// Calculate modifier for Raid Bosses
		if (_activeChar.isRaid())
		{
			defence *= Config.RAID_MDEFENCE_MULTIPLIER;
		}
		
		// Calculate modifiers Magic Attack
		return (int) calcStat(Stats.MAGIC_DEFENCE, defence, target, skill);
	}
	
	/**
	 * @return the MEN of the L2Character (base+modifier).
	 */
	public final int getMEN()
	{
		if (_activeChar == null)
		{
			return 1;
		}
		
		return (int) calcStat(Stats.STAT_MEN, _activeChar.getTemplate().getBaseMEN());
	}
	
	public float getMovementSpeedMultiplier()
	{
		if (_activeChar == null)
		{
			return 1;
		}
		
		return getRunSpeed() / (float) _activeChar.getTemplate().getBaseRunSpd();
	}
	
	/**
	 * @return the RunSpeed (base+modifier) or WalkSpeed (base+modifier) of the L2Character in function of the movement type.
	 */
	public float getMoveSpeed()
	{
		if (_activeChar == null)
		{
			return 1;
		}
		
		if (_activeChar.isRunning())
		{
			return getRunSpeed();
		}
		return getWalkSpeed();
	}
	
	/**
	 * @param skill
	 * @return the MReuse rate (base+modifier) of the L2Character.
	 */
	public final double getMReuseRate(L2Skill skill)
	{
		if (_activeChar == null)
		{
			return 1;
		}
		
		return calcStat(Stats.MAGIC_REUSE_RATE, _activeChar.getTemplate().getBaseMReuseRate(), null, skill);
	}
	
	/**
	 * @param target
	 * @return the PAtk (base+modifier) of the L2Character.
	 */
	public int getPAtk(L2Character target)
	{
		if (_activeChar == null)
		{
			return 1;
		}
		float bonusAtk = 1;
		if (Config.L2JMOD_CHAMPION_ENABLE && _activeChar.isChampion())
		{
			bonusAtk = Config.L2JMOD_CHAMPION_ATK;
		}
		if (_activeChar.isRaid())
		{
			bonusAtk *= Config.RAID_PATTACK_MULTIPLIER;
		}
		return (int) calcStat(Stats.POWER_ATTACK, _activeChar.getTemplate().getBasePAtk() * bonusAtk, target, null);
	}
	
	/**
	 * @param target
	 * @return the PAtk Modifier against animals.
	 */
	public final double getPAtkAnimals(L2Character target)
	{
		return calcStat(Stats.PATK_ANIMALS, 1, target, null);
	}
	
	/**
	 * @param target
	 * @return the PAtk Modifier against dragons.
	 */
	public final double getPAtkDragons(L2Character target)
	{
		return calcStat(Stats.PATK_DRAGONS, 1, target, null);
	}
	
	/**
	 * @param target
	 * @return the PAtk Modifier against insects.
	 */
	public final double getPAtkInsects(L2Character target)
	{
		return calcStat(Stats.PATK_INSECTS, 1, target, null);
	}
	
	/**
	 * @param target
	 * @return the PAtk Modifier against monsters.
	 */
	public final double getPAtkMonsters(L2Character target)
	{
		return calcStat(Stats.PATK_MONSTERS, 1, target, null);
	}
	
	/**
	 * @param target
	 * @return the PAtk Modifier against plants.
	 */
	public final double getPAtkPlants(L2Character target)
	{
		return calcStat(Stats.PATK_PLANTS, 1, target, null);
	}
	
	/**
	 * @param target
	 * @return the PAtk Modifier against giants.
	 */
	public final double getPAtkGiants(L2Character target)
	{
		return calcStat(Stats.PATK_GIANTS, 1, target, null);
	}
	
	/**
	 * @param target
	 * @return the PAtk Modifier against magic creatures.
	 */
	public final double getPAtkMagicCreatures(L2Character target)
	{
		return calcStat(Stats.PATK_MCREATURES, 1, target, null);
	}
	
	/**
	 * @return the PAtk Speed (base+modifier) of the L2Character in function of the Armour Expertise Penalty.
	 */
	public int getPAtkSpd()
	{
		if (_activeChar == null)
		{
			return 1;
		}
		float bonusAtk = 1;
		if (Config.L2JMOD_CHAMPION_ENABLE && _activeChar.isChampion())
		{
			bonusAtk = Config.L2JMOD_CHAMPION_SPD_ATK;
		}
		int val = (int) Math.round(calcStat(Stats.POWER_ATTACK_SPEED, _activeChar.getTemplate().getBasePAtkSpd() * bonusAtk, null, null));
		return val;
	}
	
	/**
	 * @param target
	 * @return the PDef Modifier against animals.
	 */
	public final double getPDefAnimals(L2Character target)
	{
		return calcStat(Stats.PDEF_ANIMALS, 1, target, null);
	}
	
	/**
	 * @param target
	 * @return the PDef Modifier against dragons.
	 */
	public final double getPDefDragons(L2Character target)
	{
		return calcStat(Stats.PDEF_DRAGONS, 1, target, null);
	}
	
	/**
	 * @param target
	 * @return the PDef Modifier against insects.
	 */
	public final double getPDefInsects(L2Character target)
	{
		return calcStat(Stats.PDEF_INSECTS, 1, target, null);
	}
	
	/**
	 * @param target
	 * @return the PDef Modifier against monsters.
	 */
	public final double getPDefMonsters(L2Character target)
	{
		return calcStat(Stats.PDEF_MONSTERS, 1, target, null);
	}
	
	/**
	 * @param target
	 * @return the PDef Modifier against plants.
	 */
	public final double getPDefPlants(L2Character target)
	{
		return calcStat(Stats.PDEF_PLANTS, 1, target, null);
	}
	
	/**
	 * @param target
	 * @return the PDef Modifier against giants.
	 */
	public final double getPDefGiants(L2Character target)
	{
		return calcStat(Stats.PDEF_GIANTS, 1, target, null);
	}
	
	/**
	 * @param target
	 * @return the PDef Modifier against giants.
	 */
	public final double getPDefMagicCreatures(L2Character target)
	{
		return calcStat(Stats.PDEF_MCREATURES, 1, target, null);
	}
	
	/**
	 * @param target
	 * @return the PDef (base+modifier) of the L2Character.
	 */
	public int getPDef(L2Character target)
	{
		if (_activeChar == null)
		{
			return 1;
		}
		
		return (int) calcStat(Stats.POWER_DEFENCE, (_activeChar.isRaid()) ? _activeChar.getTemplate().getBasePDef() * Config.RAID_PDEFENCE_MULTIPLIER : _activeChar.getTemplate().getBasePDef(), target, null);
	}
	
	/**
	 * @return the Physical Attack range (base+modifier) of the L2Character.
	 */
	public final int getPhysicalAttackRange()
	{
		if (_activeChar == null)
		{
			return 1;
		}
		
		if (_activeChar.isTransformed())
		{
			return _activeChar.getTemplate().getBaseAtkRange();
		}
		// Polearm handled here for now. Basically L2PcInstance could have a function
		// similar to FuncBowAtkRange and NPC are defined in DP.
		L2Weapon weaponItem = _activeChar.getActiveWeaponItem();
		if ((weaponItem != null) && (weaponItem.getItemType() == L2WeaponType.POLE))
		{
			return (int) calcStat(Stats.POWER_ATTACK_RANGE, 66);
		}
		
		return (int) calcStat(Stats.POWER_ATTACK_RANGE, _activeChar.getTemplate().getBaseAtkRange());
	}
	
	/**
	 * @param target
	 * @return the weapon reuse modifier.
	 */
	public final double getWeaponReuseModifier(L2Character target)
	{
		return calcStat(Stats.ATK_REUSE, 1, target, null);
	}
	
	/**
	 * @return the RunSpeed (base+modifier) of the L2Character in function of the Armour Expertise Penalty.
	 */
	public int getRunSpeed()
	{
		if (_activeChar == null)
		{
			return 1;
		}
		
		// err we should be adding TO the persons run speed
		// not making it a constant
		double baseRunSpd = _activeChar.getTemplate().getBaseRunSpd();
		
		if (baseRunSpd == 0)
		{
			return 0;
		}
		
		return (int) Math.round(calcStat(Stats.RUN_SPEED, baseRunSpd, null, null));
	}
	
	/**
	 * @return the ShieldDef rate (base+modifier) of the L2Character.
	 */
	public final int getShldDef()
	{
		return (int) calcStat(Stats.SHIELD_DEFENCE, 0);
	}
	
	public int getSp()
	{
		return _sp;
	}
	
	public void setSp(int value)
	{
		_sp = value;
	}
	
	/**
	 * @return the STR of the L2Character (base+modifier).
	 */
	public final int getSTR()
	{
		if (_activeChar == null)
		{
			return 1;
		}
		
		return (int) calcStat(Stats.STAT_STR, _activeChar.getTemplate().getBaseSTR());
	}
	
	/**
	 * @return the WalkSpeed (base+modifier) of the L2Character.
	 */
	public int getWalkSpeed()
	{
		if (_activeChar == null)
		{
			return 1;
		}
		
		double baseWalkSpd = _activeChar.getTemplate().getBaseWalkSpd();
		
		if (baseWalkSpd == 0)
		{
			return 0;
		}
		
		return (int) calcStat(Stats.WALK_SPEED, baseWalkSpd);
	}
	
	/**
	 * @return the WIT of the L2Character (base+modifier).
	 */
	public final int getWIT()
	{
		if (_activeChar == null)
		{
			return 1;
		}
		
		return (int) calcStat(Stats.STAT_WIT, _activeChar.getTemplate().getBaseWIT());
	}
	
	/**
	 * @param skill
	 * @return the mpConsume.
	 */
	public final int getMpConsume(L2Skill skill)
	{
		if (skill == null)
		{
			return 1;
		}
		double mpConsume = skill.getMpConsume();
		double nextDanceMpCost = Math.ceil(skill.getMpConsume() / 2.);
		if (skill.isDance())
		{
			if (Config.DANCE_CONSUME_ADDITIONAL_MP && (_activeChar != null) && (_activeChar.getDanceCount() > 0))
			{
				mpConsume += _activeChar.getDanceCount() * nextDanceMpCost;
			}
		}
		
		mpConsume = calcStat(Stats.MP_CONSUME, mpConsume, null, skill);
		
		if (skill.isDance())
		{
			return (int) calcStat(Stats.DANCE_MP_CONSUME_RATE, mpConsume);
		}
		else if (skill.isMagic())
		{
			return (int) calcStat(Stats.MAGICAL_MP_CONSUME_RATE, mpConsume);
		}
		else
		{
			return (int) calcStat(Stats.PHYSICAL_MP_CONSUME_RATE, mpConsume);
		}
	}
	
	/**
	 * @param skill
	 * @return the mpInitialConsume.
	 */
	public final int getMpInitialConsume(L2Skill skill)
	{
		if (skill == null)
		{
			return 1;
		}
		
		double mpConsume = calcStat(Stats.MP_CONSUME, skill.getMpInitialConsume(), null, skill);
		
		if (skill.isDance())
		{
			return (int) calcStat(Stats.DANCE_MP_CONSUME_RATE, mpConsume);
		}
		else if (skill.isMagic())
		{
			return (int) calcStat(Stats.MAGICAL_MP_CONSUME_RATE, mpConsume);
		}
		else
		{
			return (int) calcStat(Stats.PHYSICAL_MP_CONSUME_RATE, mpConsume);
		}
	}
	
	public byte getAttackElement()
	{
		L2ItemInstance weaponInstance = _activeChar.getActiveWeaponInstance();
		// 1st order - weapon element
		if ((weaponInstance != null) && (weaponInstance.getAttackElementType() >= 0))
		{
			return weaponInstance.getAttackElementType();
		}
		
		// temp fix starts
		int tempVal = 0, stats[] =
		{
			0,
			0,
			0,
			0,
			0,
			0
		};
		
		byte returnVal = -2;
		stats[0] = (int) calcStat(Stats.FIRE_POWER, _activeChar.getTemplate().getBaseFire());
		stats[1] = (int) calcStat(Stats.WATER_POWER, _activeChar.getTemplate().getBaseWater());
		stats[2] = (int) calcStat(Stats.WIND_POWER, _activeChar.getTemplate().getBaseWind());
		stats[3] = (int) calcStat(Stats.EARTH_POWER, _activeChar.getTemplate().getBaseEarth());
		stats[4] = (int) calcStat(Stats.HOLY_POWER, _activeChar.getTemplate().getBaseHoly());
		stats[5] = (int) calcStat(Stats.DARK_POWER, _activeChar.getTemplate().getBaseDark());
		
		for (byte x = 0; x < 6; x++)
		{
			if (stats[x] > tempVal)
			{
				returnVal = x;
				tempVal = stats[x];
			}
		}
		
		return returnVal;
		// temp fix ends
		
		/*
		 * uncomment me once deadlocks in getAllEffects() fixed return _activeChar.getElementIdFromEffects();
		 */
	}
	
	public int getAttackElementValue(byte attackAttribute)
	{
		switch (attackAttribute)
		{
			case Elementals.FIRE:
				return (int) calcStat(Stats.FIRE_POWER, _activeChar.getTemplate().getBaseFire());
			case Elementals.WATER:
				return (int) calcStat(Stats.WATER_POWER, _activeChar.getTemplate().getBaseWater());
			case Elementals.WIND:
				return (int) calcStat(Stats.WIND_POWER, _activeChar.getTemplate().getBaseWind());
			case Elementals.EARTH:
				return (int) calcStat(Stats.EARTH_POWER, _activeChar.getTemplate().getBaseEarth());
			case Elementals.HOLY:
				return (int) calcStat(Stats.HOLY_POWER, _activeChar.getTemplate().getBaseHoly());
			case Elementals.DARK:
				return (int) calcStat(Stats.DARK_POWER, _activeChar.getTemplate().getBaseDark());
			default:
				return 0;
		}
	}
	
	public int getDefenseElementValue(byte defenseAttribute)
	{
		switch (defenseAttribute)
		{
			case Elementals.FIRE:
				return (int) calcStat(Stats.FIRE_RES, _activeChar.getTemplate().getBaseFireRes());
			case Elementals.WATER:
				return (int) calcStat(Stats.WATER_RES, _activeChar.getTemplate().getBaseWaterRes());
			case Elementals.WIND:
				return (int) calcStat(Stats.WIND_RES, _activeChar.getTemplate().getBaseWindRes());
			case Elementals.EARTH:
				return (int) calcStat(Stats.EARTH_RES, _activeChar.getTemplate().getBaseEarthRes());
			case Elementals.HOLY:
				return (int) calcStat(Stats.HOLY_RES, _activeChar.getTemplate().getBaseHolyRes());
			case Elementals.DARK:
				return (int) calcStat(Stats.DARK_RES, _activeChar.getTemplate().getBaseDarkRes());
			default:
				return 0;
		}
	}
}
