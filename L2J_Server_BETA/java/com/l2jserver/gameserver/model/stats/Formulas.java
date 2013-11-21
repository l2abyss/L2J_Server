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
package com.l2jserver.gameserver.model.stats;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.l2jserver.Config;
import com.l2jserver.gameserver.SevenSigns;
import com.l2jserver.gameserver.SevenSignsFestival;
import com.l2jserver.gameserver.datatables.HitConditionBonus;
import com.l2jserver.gameserver.datatables.KarmaData;
import com.l2jserver.gameserver.instancemanager.CastleManager;
import com.l2jserver.gameserver.instancemanager.ClanHallManager;
import com.l2jserver.gameserver.instancemanager.FortManager;
import com.l2jserver.gameserver.instancemanager.SiegeManager;
import com.l2jserver.gameserver.instancemanager.ZoneManager;
import com.l2jserver.gameserver.model.L2SiegeClan;
import com.l2jserver.gameserver.model.StatsSet;
import com.l2jserver.gameserver.model.actor.L2Attackable;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.instance.L2CubicInstance;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.actor.instance.L2PetInstance;
import com.l2jserver.gameserver.model.effects.L2EffectType;
import com.l2jserver.gameserver.model.entity.Castle;
import com.l2jserver.gameserver.model.entity.ClanHall;
import com.l2jserver.gameserver.model.entity.Fort;
import com.l2jserver.gameserver.model.entity.Siege;
import com.l2jserver.gameserver.model.items.L2Armor;
import com.l2jserver.gameserver.model.items.L2Item;
import com.l2jserver.gameserver.model.items.L2Weapon;
import com.l2jserver.gameserver.model.items.type.L2ArmorType;
import com.l2jserver.gameserver.model.items.type.L2WeaponType;
import com.l2jserver.gameserver.model.skills.BuffInfo;
import com.l2jserver.gameserver.model.skills.L2Skill;
import com.l2jserver.gameserver.model.skills.funcs.formulas.FuncArmorSet;
import com.l2jserver.gameserver.model.skills.funcs.formulas.FuncAtkAccuracy;
import com.l2jserver.gameserver.model.skills.funcs.formulas.FuncAtkCritical;
import com.l2jserver.gameserver.model.skills.funcs.formulas.FuncAtkEvasion;
import com.l2jserver.gameserver.model.skills.funcs.formulas.FuncBowAtkRange;
import com.l2jserver.gameserver.model.skills.funcs.formulas.FuncCrossBowAtkRange;
import com.l2jserver.gameserver.model.skills.funcs.formulas.FuncGatesMDefMod;
import com.l2jserver.gameserver.model.skills.funcs.formulas.FuncGatesPDefMod;
import com.l2jserver.gameserver.model.skills.funcs.formulas.FuncHenna;
import com.l2jserver.gameserver.model.skills.funcs.formulas.FuncMAtkCritical;
import com.l2jserver.gameserver.model.skills.funcs.formulas.FuncMAtkMod;
import com.l2jserver.gameserver.model.skills.funcs.formulas.FuncMAtkSpeed;
import com.l2jserver.gameserver.model.skills.funcs.formulas.FuncMDefMod;
import com.l2jserver.gameserver.model.skills.funcs.formulas.FuncMaxCpMul;
import com.l2jserver.gameserver.model.skills.funcs.formulas.FuncMaxHpMul;
import com.l2jserver.gameserver.model.skills.funcs.formulas.FuncMaxMpMul;
import com.l2jserver.gameserver.model.skills.funcs.formulas.FuncMoveSpeed;
import com.l2jserver.gameserver.model.skills.funcs.formulas.FuncPAtkMod;
import com.l2jserver.gameserver.model.skills.funcs.formulas.FuncPAtkSpeed;
import com.l2jserver.gameserver.model.skills.funcs.formulas.FuncPDefMod;
import com.l2jserver.gameserver.model.zone.ZoneId;
import com.l2jserver.gameserver.model.zone.type.L2CastleZone;
import com.l2jserver.gameserver.model.zone.type.L2ClanHallZone;
import com.l2jserver.gameserver.model.zone.type.L2FortZone;
import com.l2jserver.gameserver.model.zone.type.L2MotherTreeZone;
import com.l2jserver.gameserver.network.Debug;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;
import com.l2jserver.gameserver.util.Util;
import com.l2jserver.util.Rnd;

/**
 * Global calculations.
 */
public final class Formulas
{
	private static final Logger _log = Logger.getLogger(Formulas.class.getName());
	
	/** Regeneration Task period. */
	private static final int HP_REGENERATE_PERIOD = 3000; // 3 secs
	
	public static final byte SHIELD_DEFENSE_FAILED = 0; // no shield defense
	public static final byte SHIELD_DEFENSE_SUCCEED = 1; // normal shield defense
	public static final byte SHIELD_DEFENSE_PERFECT_BLOCK = 2; // perfect block
	
	private static final byte MELEE_ATTACK_RANGE = 40;
	
	/**
	 * Return the period between 2 regeneration task (3s for L2Character, 5 min for L2DoorInstance).
	 * @param cha
	 * @return
	 */
	public static int getRegeneratePeriod(L2Character cha)
	{
		return cha.isDoor() ? HP_REGENERATE_PERIOD * 100 : HP_REGENERATE_PERIOD;
	}
	
	/**
	 * Return the standard NPC Calculator set containing ACCURACY_COMBAT and EVASION_RATE.<br>
	 * <B><U>Concept</U>:</B><br>
	 * A calculator is created to manage and dynamically calculate the effect of a character property (ex : MAX_HP, REGENERATE_HP_RATE...). In fact, each calculator is a table of Func object in which each Func represents a mathematic function : <br>
	 * FuncAtkAccuracy -> Math.sqrt(_player.getDEX())*6+_player.getLevel()<br>
	 * To reduce cache memory use, L2NPCInstances who don't have skills share the same Calculator set called <B>NPC_STD_CALCULATOR</B>.<br>
	 * @return
	 */
	public static Calculator[] getStdNPCCalculators()
	{
		Calculator[] std = new Calculator[Stats.NUM_STATS];
		
		std[Stats.MAX_HP.ordinal()] = new Calculator();
		std[Stats.MAX_HP.ordinal()].addFunc(FuncMaxHpMul.getInstance());
		
		std[Stats.MAX_MP.ordinal()] = new Calculator();
		std[Stats.MAX_MP.ordinal()].addFunc(FuncMaxMpMul.getInstance());
		
		std[Stats.POWER_ATTACK.ordinal()] = new Calculator();
		std[Stats.POWER_ATTACK.ordinal()].addFunc(FuncPAtkMod.getInstance());
		
		std[Stats.MAGIC_ATTACK.ordinal()] = new Calculator();
		std[Stats.MAGIC_ATTACK.ordinal()].addFunc(FuncMAtkMod.getInstance());
		
		std[Stats.POWER_DEFENCE.ordinal()] = new Calculator();
		std[Stats.POWER_DEFENCE.ordinal()].addFunc(FuncPDefMod.getInstance());
		
		std[Stats.MAGIC_DEFENCE.ordinal()] = new Calculator();
		std[Stats.MAGIC_DEFENCE.ordinal()].addFunc(FuncMDefMod.getInstance());
		
		std[Stats.CRITICAL_RATE.ordinal()] = new Calculator();
		std[Stats.CRITICAL_RATE.ordinal()].addFunc(FuncAtkCritical.getInstance());
		
		std[Stats.MCRITICAL_RATE.ordinal()] = new Calculator();
		std[Stats.MCRITICAL_RATE.ordinal()].addFunc(FuncMAtkCritical.getInstance());
		
		std[Stats.ACCURACY_COMBAT.ordinal()] = new Calculator();
		std[Stats.ACCURACY_COMBAT.ordinal()].addFunc(FuncAtkAccuracy.getInstance());
		
		std[Stats.EVASION_RATE.ordinal()] = new Calculator();
		std[Stats.EVASION_RATE.ordinal()].addFunc(FuncAtkEvasion.getInstance());
		
		std[Stats.POWER_ATTACK_SPEED.ordinal()] = new Calculator();
		std[Stats.POWER_ATTACK_SPEED.ordinal()].addFunc(FuncPAtkSpeed.getInstance());
		
		std[Stats.MAGIC_ATTACK_SPEED.ordinal()] = new Calculator();
		std[Stats.MAGIC_ATTACK_SPEED.ordinal()].addFunc(FuncMAtkSpeed.getInstance());
		
		std[Stats.MOVE_SPEED.ordinal()] = new Calculator();
		std[Stats.MOVE_SPEED.ordinal()].addFunc(FuncMoveSpeed.getInstance());
		
		return std;
	}
	
	public static Calculator[] getStdDoorCalculators()
	{
		Calculator[] std = new Calculator[Stats.NUM_STATS];
		
		// Add the FuncAtkAccuracy to the Standard Calculator of ACCURACY_COMBAT
		std[Stats.ACCURACY_COMBAT.ordinal()] = new Calculator();
		std[Stats.ACCURACY_COMBAT.ordinal()].addFunc(FuncAtkAccuracy.getInstance());
		
		// Add the FuncAtkEvasion to the Standard Calculator of EVASION_RATE
		std[Stats.EVASION_RATE.ordinal()] = new Calculator();
		std[Stats.EVASION_RATE.ordinal()].addFunc(FuncAtkEvasion.getInstance());
		
		// SevenSigns PDEF Modifier
		std[Stats.POWER_DEFENCE.ordinal()] = new Calculator();
		std[Stats.POWER_DEFENCE.ordinal()].addFunc(FuncGatesPDefMod.getInstance());
		
		// SevenSigns MDEF Modifier
		std[Stats.MAGIC_DEFENCE.ordinal()] = new Calculator();
		std[Stats.MAGIC_DEFENCE.ordinal()].addFunc(FuncGatesMDefMod.getInstance());
		
		return std;
	}
	
	/**
	 * Add basics Func objects to L2PcInstance and L2Summon.<br>
	 * <B><U>Concept</U>:</B><br>
	 * A calculator is created to manage and dynamically calculate the effect of a character property (ex : MAX_HP, REGENERATE_HP_RATE...). In fact, each calculator is a table of Func object in which each Func represents a mathematic function : <br>
	 * FuncAtkAccuracy -> Math.sqrt(_player.getDEX())*6+_player.getLevel()<br>
	 * @param cha L2PcInstance or L2Summon that must obtain basic Func objects
	 */
	public static void addFuncsToNewCharacter(L2Character cha)
	{
		if (cha.isPlayer())
		{
			cha.addStatFunc(FuncMaxHpMul.getInstance());
			cha.addStatFunc(FuncMaxCpMul.getInstance());
			cha.addStatFunc(FuncMaxMpMul.getInstance());
			// cha.addStatFunc(FuncMultRegenResting.getInstance(Stats.REGENERATE_HP_RATE));
			// cha.addStatFunc(FuncMultRegenResting.getInstance(Stats.REGENERATE_CP_RATE));
			// cha.addStatFunc(FuncMultRegenResting.getInstance(Stats.REGENERATE_MP_RATE));
			cha.addStatFunc(FuncBowAtkRange.getInstance());
			cha.addStatFunc(FuncCrossBowAtkRange.getInstance());
			// cha.addStatFunc(FuncMultLevelMod.getInstance(Stats.POWER_ATTACK));
			// cha.addStatFunc(FuncMultLevelMod.getInstance(Stats.POWER_DEFENCE));
			// cha.addStatFunc(FuncMultLevelMod.getInstance(Stats.MAGIC_DEFENCE));
			cha.addStatFunc(FuncPAtkMod.getInstance());
			cha.addStatFunc(FuncMAtkMod.getInstance());
			cha.addStatFunc(FuncPDefMod.getInstance());
			cha.addStatFunc(FuncMDefMod.getInstance());
			cha.addStatFunc(FuncAtkCritical.getInstance());
			cha.addStatFunc(FuncMAtkCritical.getInstance());
			cha.addStatFunc(FuncAtkAccuracy.getInstance());
			cha.addStatFunc(FuncAtkEvasion.getInstance());
			cha.addStatFunc(FuncPAtkSpeed.getInstance());
			cha.addStatFunc(FuncMAtkSpeed.getInstance());
			cha.addStatFunc(FuncMoveSpeed.getInstance());
			
			cha.addStatFunc(FuncHenna.getInstance(Stats.STAT_STR));
			cha.addStatFunc(FuncHenna.getInstance(Stats.STAT_DEX));
			cha.addStatFunc(FuncHenna.getInstance(Stats.STAT_INT));
			cha.addStatFunc(FuncHenna.getInstance(Stats.STAT_MEN));
			cha.addStatFunc(FuncHenna.getInstance(Stats.STAT_CON));
			cha.addStatFunc(FuncHenna.getInstance(Stats.STAT_WIT));
			
			cha.addStatFunc(FuncArmorSet.getInstance(Stats.STAT_STR));
			cha.addStatFunc(FuncArmorSet.getInstance(Stats.STAT_DEX));
			cha.addStatFunc(FuncArmorSet.getInstance(Stats.STAT_INT));
			cha.addStatFunc(FuncArmorSet.getInstance(Stats.STAT_MEN));
			cha.addStatFunc(FuncArmorSet.getInstance(Stats.STAT_CON));
			cha.addStatFunc(FuncArmorSet.getInstance(Stats.STAT_WIT));
		}
		else if (cha.isSummon())
		{
			cha.addStatFunc(FuncMaxHpMul.getInstance());
			cha.addStatFunc(FuncMaxMpMul.getInstance());
			cha.addStatFunc(FuncPAtkMod.getInstance());
			cha.addStatFunc(FuncMAtkMod.getInstance());
			cha.addStatFunc(FuncPDefMod.getInstance());
			cha.addStatFunc(FuncMDefMod.getInstance());
			cha.addStatFunc(FuncAtkCritical.getInstance());
			cha.addStatFunc(FuncMAtkCritical.getInstance());
			cha.addStatFunc(FuncAtkAccuracy.getInstance());
			cha.addStatFunc(FuncAtkEvasion.getInstance());
			cha.addStatFunc(FuncMoveSpeed.getInstance());
			cha.addStatFunc(FuncPAtkSpeed.getInstance());
			cha.addStatFunc(FuncMAtkSpeed.getInstance());
		}
	}
	
	/**
	 * Calculate the HP regen rate (base + modifiers).
	 * @param cha
	 * @return
	 */
	public static final double calcHpRegen(L2Character cha)
	{
		double init = cha.isPlayer() ? cha.getActingPlayer().getTemplate().getBaseHpRegen(cha.getLevel()) : cha.getTemplate().getBaseHpReg();
		double hpRegenMultiplier = cha.isRaid() ? Config.RAID_HP_REGEN_MULTIPLIER : Config.HP_REGEN_MULTIPLIER;
		double hpRegenBonus = 0;
		
		if (Config.L2JMOD_CHAMPION_ENABLE && cha.isChampion())
		{
			hpRegenMultiplier *= Config.L2JMOD_CHAMPION_HP_REGEN;
		}
		
		if (cha.isPlayer())
		{
			L2PcInstance player = cha.getActingPlayer();
			
			// SevenSigns Festival modifier
			if (SevenSignsFestival.getInstance().isFestivalInProgress() && player.isFestivalParticipant())
			{
				hpRegenMultiplier *= calcFestivalRegenModifier(player);
			}
			else
			{
				double siegeModifier = calcSiegeRegenModifier(player);
				if (siegeModifier > 0)
				{
					hpRegenMultiplier *= siegeModifier;
				}
			}
			
			if (player.isInsideZone(ZoneId.CLAN_HALL) && (player.getClan() != null) && (player.getClan().getHideoutId() > 0))
			{
				L2ClanHallZone zone = ZoneManager.getInstance().getZone(player, L2ClanHallZone.class);
				int posChIndex = zone == null ? -1 : zone.getResidenceId();
				int clanHallIndex = player.getClan().getHideoutId();
				if ((clanHallIndex > 0) && (clanHallIndex == posChIndex))
				{
					ClanHall clansHall = ClanHallManager.getInstance().getClanHallById(clanHallIndex);
					if (clansHall != null)
					{
						if (clansHall.getFunction(ClanHall.FUNC_RESTORE_HP) != null)
						{
							hpRegenMultiplier *= 1 + ((double) clansHall.getFunction(ClanHall.FUNC_RESTORE_HP).getLvl() / 100);
						}
					}
				}
			}
			
			if (player.isInsideZone(ZoneId.CASTLE) && (player.getClan() != null) && (player.getClan().getCastleId() > 0))
			{
				L2CastleZone zone = ZoneManager.getInstance().getZone(player, L2CastleZone.class);
				int posCastleIndex = zone == null ? -1 : zone.getResidenceId();
				int castleIndex = player.getClan().getCastleId();
				if ((castleIndex > 0) && (castleIndex == posCastleIndex))
				{
					Castle castle = CastleManager.getInstance().getCastleById(castleIndex);
					if (castle != null)
					{
						if (castle.getFunction(Castle.FUNC_RESTORE_HP) != null)
						{
							hpRegenMultiplier *= 1 + ((double) castle.getFunction(Castle.FUNC_RESTORE_HP).getLvl() / 100);
						}
					}
				}
			}
			
			if (player.isInsideZone(ZoneId.FORT) && (player.getClan() != null) && (player.getClan().getFortId() > 0))
			{
				L2FortZone zone = ZoneManager.getInstance().getZone(player, L2FortZone.class);
				int posFortIndex = zone == null ? -1 : zone.getResidenceId();
				int fortIndex = player.getClan().getFortId();
				if ((fortIndex > 0) && (fortIndex == posFortIndex))
				{
					Fort fort = FortManager.getInstance().getFortById(fortIndex);
					if (fort != null)
					{
						if (fort.getFunction(Fort.FUNC_RESTORE_HP) != null)
						{
							hpRegenMultiplier *= 1 + ((double) fort.getFunction(Fort.FUNC_RESTORE_HP).getLvl() / 100);
						}
					}
				}
			}
			
			// Mother Tree effect is calculated at last
			if (player.isInsideZone(ZoneId.MOTHER_TREE))
			{
				L2MotherTreeZone zone = ZoneManager.getInstance().getZone(player, L2MotherTreeZone.class);
				int hpBonus = zone == null ? 0 : zone.getHpRegenBonus();
				hpRegenBonus += hpBonus;
			}
			
			// Calculate Movement bonus
			if (player.isSitting())
			{
				hpRegenMultiplier *= 1.5; // Sitting
			}
			else if (!player.isMoving())
			{
				hpRegenMultiplier *= 1.1; // Staying
			}
			else if (player.isRunning())
			{
				hpRegenMultiplier *= 0.7; // Running
			}
			
			// Add CON bonus
			init *= cha.getLevelMod() * BaseStats.CON.calcBonus(cha);
		}
		else if (cha.isPet())
		{
			init = ((L2PetInstance) cha).getPetLevelData().getPetRegenHP() * Config.PET_HP_REGEN_MULTIPLIER;
		}
		
		return (cha.calcStat(Stats.REGENERATE_HP_RATE, Math.max(1, init), null, null) * hpRegenMultiplier) + hpRegenBonus;
	}
	
	/**
	 * Calculate the MP regen rate (base + modifiers).
	 * @param cha
	 * @return
	 */
	public static final double calcMpRegen(L2Character cha)
	{
		double init = cha.isPlayer() ? cha.getActingPlayer().getTemplate().getBaseMpRegen(cha.getLevel()) : cha.getTemplate().getBaseMpReg();
		double mpRegenMultiplier = cha.isRaid() ? Config.RAID_MP_REGEN_MULTIPLIER : Config.MP_REGEN_MULTIPLIER;
		double mpRegenBonus = 0;
		
		if (cha.isPlayer())
		{
			L2PcInstance player = cha.getActingPlayer();
			
			// SevenSigns Festival modifier
			if (SevenSignsFestival.getInstance().isFestivalInProgress() && player.isFestivalParticipant())
			{
				mpRegenMultiplier *= calcFestivalRegenModifier(player);
			}
			
			// Mother Tree effect is calculated at last'
			if (player.isInsideZone(ZoneId.MOTHER_TREE))
			{
				L2MotherTreeZone zone = ZoneManager.getInstance().getZone(player, L2MotherTreeZone.class);
				int mpBonus = zone == null ? 0 : zone.getMpRegenBonus();
				mpRegenBonus += mpBonus;
			}
			
			if (player.isInsideZone(ZoneId.CLAN_HALL) && (player.getClan() != null) && (player.getClan().getHideoutId() > 0))
			{
				L2ClanHallZone zone = ZoneManager.getInstance().getZone(player, L2ClanHallZone.class);
				int posChIndex = zone == null ? -1 : zone.getResidenceId();
				int clanHallIndex = player.getClan().getHideoutId();
				if ((clanHallIndex > 0) && (clanHallIndex == posChIndex))
				{
					ClanHall clansHall = ClanHallManager.getInstance().getClanHallById(clanHallIndex);
					if (clansHall != null)
					{
						if (clansHall.getFunction(ClanHall.FUNC_RESTORE_MP) != null)
						{
							mpRegenMultiplier *= 1 + ((double) clansHall.getFunction(ClanHall.FUNC_RESTORE_MP).getLvl() / 100);
						}
					}
				}
			}
			
			if (player.isInsideZone(ZoneId.CASTLE) && (player.getClan() != null) && (player.getClan().getCastleId() > 0))
			{
				L2CastleZone zone = ZoneManager.getInstance().getZone(player, L2CastleZone.class);
				int posCastleIndex = zone == null ? -1 : zone.getResidenceId();
				int castleIndex = player.getClan().getCastleId();
				if ((castleIndex > 0) && (castleIndex == posCastleIndex))
				{
					Castle castle = CastleManager.getInstance().getCastleById(castleIndex);
					if (castle != null)
					{
						if (castle.getFunction(Castle.FUNC_RESTORE_MP) != null)
						{
							mpRegenMultiplier *= 1 + ((double) castle.getFunction(Castle.FUNC_RESTORE_MP).getLvl() / 100);
						}
					}
				}
			}
			
			if (player.isInsideZone(ZoneId.FORT) && (player.getClan() != null) && (player.getClan().getFortId() > 0))
			{
				L2FortZone zone = ZoneManager.getInstance().getZone(player, L2FortZone.class);
				int posFortIndex = zone == null ? -1 : zone.getResidenceId();
				int fortIndex = player.getClan().getFortId();
				if ((fortIndex > 0) && (fortIndex == posFortIndex))
				{
					Fort fort = FortManager.getInstance().getFortById(fortIndex);
					if (fort != null)
					{
						if (fort.getFunction(Fort.FUNC_RESTORE_MP) != null)
						{
							mpRegenMultiplier *= 1 + ((double) fort.getFunction(Fort.FUNC_RESTORE_MP).getLvl() / 100);
						}
					}
				}
			}
			
			// Calculate Movement bonus
			if (player.isSitting())
			{
				mpRegenMultiplier *= 1.5; // Sitting
			}
			else if (!player.isMoving())
			{
				mpRegenMultiplier *= 1.1; // Staying
			}
			else if (player.isRunning())
			{
				mpRegenMultiplier *= 0.7; // Running
			}
			
			// Add MEN bonus
			init *= cha.getLevelMod() * BaseStats.MEN.calcBonus(cha);
		}
		else if (cha.isPet())
		{
			init = ((L2PetInstance) cha).getPetLevelData().getPetRegenMP() * Config.PET_MP_REGEN_MULTIPLIER;
		}
		
		return (cha.calcStat(Stats.REGENERATE_MP_RATE, Math.max(1, init), null, null) * mpRegenMultiplier) + mpRegenBonus;
	}
	
	/**
	 * Calculate the CP regen rate (base + modifiers).
	 * @param cha
	 * @return
	 */
	public static final double calcCpRegen(L2Character cha)
	{
		double init = cha.isPlayer() ? cha.getActingPlayer().getTemplate().getBaseCpRegen(cha.getLevel()) : cha.getTemplate().getBaseHpReg();
		double cpRegenMultiplier = Config.CP_REGEN_MULTIPLIER;
		double cpRegenBonus = 0;
		
		if (cha.isPlayer())
		{
			L2PcInstance player = cha.getActingPlayer();
			
			// Calculate Movement bonus
			if (player.isSitting())
			{
				cpRegenMultiplier *= 1.5; // Sitting
			}
			else if (!player.isMoving())
			{
				cpRegenMultiplier *= 1.1; // Staying
			}
			else if (player.isRunning())
			{
				cpRegenMultiplier *= 0.7; // Running
			}
		}
		else
		{
			// Calculate Movement bonus
			if (!cha.isMoving())
			{
				cpRegenMultiplier *= 1.1; // Staying
			}
			else if (cha.isRunning())
			{
				cpRegenMultiplier *= 0.7; // Running
			}
		}
		
		// Apply CON bonus
		init *= cha.getLevelMod() * BaseStats.CON.calcBonus(cha);
		return (cha.calcStat(Stats.REGENERATE_CP_RATE, Math.max(1, init), null, null) * cpRegenMultiplier) + cpRegenBonus;
	}
	
	public static final double calcFestivalRegenModifier(L2PcInstance activeChar)
	{
		final int[] festivalInfo = SevenSignsFestival.getInstance().getFestivalForPlayer(activeChar);
		final int oracle = festivalInfo[0];
		final int festivalId = festivalInfo[1];
		int[] festivalCenter;
		
		// If the player isn't found in the festival, leave the regen rate as it is.
		if (festivalId < 0)
		{
			return 0;
		}
		
		// Retrieve the X and Y coords for the center of the festival arena the player is in.
		if (oracle == SevenSigns.CABAL_DAWN)
		{
			festivalCenter = SevenSignsFestival.FESTIVAL_DAWN_PLAYER_SPAWNS[festivalId];
		}
		else
		{
			festivalCenter = SevenSignsFestival.FESTIVAL_DUSK_PLAYER_SPAWNS[festivalId];
		}
		
		// Check the distance between the player and the player spawn point, in the center of the arena.
		double distToCenter = activeChar.calculateDistance(festivalCenter[0], festivalCenter[1], 0, false, false);
		
		if (Config.DEBUG)
		{
			_log.info("Distance: " + distToCenter + ", RegenMulti: " + ((distToCenter * 2.5) / 50));
		}
		
		return 1.0 - (distToCenter * 0.0005); // Maximum Decreased Regen of ~ -65%;
	}
	
	public static final double calcSiegeRegenModifier(L2PcInstance activeChar)
	{
		if ((activeChar == null) || (activeChar.getClan() == null))
		{
			return 0;
		}
		
		Siege siege = SiegeManager.getInstance().getSiege(activeChar.getX(), activeChar.getY(), activeChar.getZ());
		if ((siege == null) || !siege.getIsInProgress())
		{
			return 0;
		}
		
		L2SiegeClan siegeClan = siege.getAttackerClan(activeChar.getClan().getId());
		if ((siegeClan == null) || siegeClan.getFlag().isEmpty() || !Util.checkIfInRange(200, activeChar, siegeClan.getFlag().get(0), true))
		{
			return 0;
		}
		
		return 1.5; // If all is true, then modifier will be 50% more
	}
	
	public static double calcBlowDamage(L2Character attacker, L2Character target, L2Skill skill, byte shld, boolean ss)
	{
		double defence = target.getPDef(attacker);
		
		switch (shld)
		{
			case Formulas.SHIELD_DEFENSE_SUCCEED:
			{
				defence += target.getShldDef();
				break;
			}
			case Formulas.SHIELD_DEFENSE_PERFECT_BLOCK: // perfect block
			{
				return 1;
			}
		}
		
		final boolean isPvP = attacker.isPlayable() && target.isPlayable();
		final boolean isPvE = attacker.isPlayable() && target.isL2Attackable();
		double power = skill.getPower(isPvP, isPvE);
		double damage = 0;
		double proximityBonus = 1;
		double graciaPhysSkillBonus = skill.isMagic() ? 1 : 1.10113; // Gracia final physical skill bonus 10.113%
		double ssboost = ss ? 2.04 : 1; // 104% bonus with SS
		double pvpBonus = 1;
		
		if (isPvP)
		{
			// Damage bonuses in PvP fight
			pvpBonus = attacker.calcStat(Stats.PVP_PHYS_SKILL_DMG, 1, null, null);
			// Defense bonuses in PvP fight
			defence *= target.calcStat(Stats.PVP_PHYS_SKILL_DEF, 1, null, null);
		}
		
		if (attacker.isBehind(target))
		{
			proximityBonus = 1.2; // +20% crit dmg when back stabbed
		}
		else if (attacker.isInFrontOf(target))
		{
			proximityBonus = 1.1; // +10% crit dmg when side stabbed
		}
		
		// Initial damage.
		damage = (((70. * graciaPhysSkillBonus * (power + (attacker.getPAtk(target) * ssboost))) / defence) * (attacker.calcStat(Stats.CRITICAL_DAMAGE, 1, target, skill)) * (target.calcStat(Stats.CRIT_VULN, 1, target, skill)) * proximityBonus * pvpBonus) + (((attacker.calcStat(Stats.CRITICAL_DAMAGE_ADD, 0, target, skill) * 6.1 * 70) / defence) * graciaPhysSkillBonus);
		damage += target.calcStat(Stats.CRIT_ADD_VULN, 0, target, skill);
		// Traits, elements
		damage *= calcWeaponTraitBonus(attacker, target) * calcGeneralTraitBonus(attacker, target, skill.getTraitType(), false) * calcAttributeBonus(attacker, target, skill);
		
		// Random weapon damage
		damage *= attacker.getRandomDamageMultiplier();
		
		if ((target instanceof L2Attackable) && !target.isRaid() && !target.isRaidMinion() && (target.getLevel() >= Config.MIN_NPC_LVL_DMG_PENALTY) && (attacker.getActingPlayer() != null) && ((target.getLevel() - attacker.getActingPlayer().getLevel()) >= 2))
		{
			int lvlDiff = target.getLevel() - attacker.getActingPlayer().getLevel() - 1;
			if (lvlDiff >= Config.NPC_SKILL_DMG_PENALTY.size())
			{
				damage *= Config.NPC_SKILL_DMG_PENALTY.get(Config.NPC_SKILL_DMG_PENALTY.size() - 1);
			}
			else
			{
				damage *= Config.NPC_SKILL_DMG_PENALTY.get(lvlDiff);
			}
		}
		
		return Math.max(damage, 1);
	}
	
	public static double calcBackstabDamage(L2Character attacker, L2Character target, L2Skill skill, byte shld, boolean ss)
	{
		double defence = target.getPDef(attacker);
		
		switch (shld)
		{
			case Formulas.SHIELD_DEFENSE_SUCCEED:
			{
				defence += target.getShldDef();
				break;
			}
			case Formulas.SHIELD_DEFENSE_PERFECT_BLOCK: // perfect block
			{
				return 1;
			}
		}
		
		boolean isPvP = attacker.isPlayable() && target.isPlayer();
		boolean isPvE = attacker.isPlayable() && target.isL2Attackable();
		double damage = 0;
		double proximityBonus = attacker.isBehindTarget() ? 1.2 : attacker.isInFrontOfTarget() ? 1 : 1.1; // Behind: +20% - Side: +10% (TODO: values are unconfirmed, possibly custom, remove or update when confirmed)
		double graciaPhysSkillBonus = skill.isMagic() ? 1 : 1.10113; // Gracia final physical skill bonus 10.113%
		double ssboost = ss ? 1.5 : 1; // 50% bonus with SS
		double pvpBonus = 1;
		
		if (attacker.isPlayable() && target.isPlayable())
		{
			// Damage bonuses in PvP fight
			pvpBonus = attacker.calcStat(Stats.PVP_PHYS_SKILL_DMG, 1, null, null);
			// Defense bonuses in PvP fight
			defence *= target.calcStat(Stats.PVP_PHYS_SKILL_DEF, 1, null, null);
		}
		
		damage = (((70. * graciaPhysSkillBonus * (skill.getPower(isPvP, isPvE) + attacker.getPAtk(target))) / defence) * ssboost * (attacker.calcStat(Stats.CRITICAL_DAMAGE, 1, target, skill)) * (target.calcStat(Stats.CRIT_VULN, 1, target, skill)) * proximityBonus * pvpBonus) + (((attacker.calcStat(Stats.CRITICAL_DAMAGE_ADD, 0, target, skill) * 6.1 * 70) / defence) * graciaPhysSkillBonus);
		damage += target.calcStat(Stats.CRIT_ADD_VULN, 0, target, skill);
		// get the vulnerability for the instance due to skills (buffs, passives, toggles, etc)
		
		damage *= calcGeneralTraitBonus(attacker, target, skill.getTraitType(), false) * calcAttributeBonus(attacker, target, skill);
		damage *= attacker.getRandomDamageMultiplier();
		
		if (target.isL2Attackable() && !target.isRaid() && !target.isRaidMinion() && (target.getLevel() >= Config.MIN_NPC_LVL_DMG_PENALTY) && (attacker.getActingPlayer() != null) && ((target.getLevel() - attacker.getActingPlayer().getLevel()) >= 2))
		{
			int lvlDiff = target.getLevel() - attacker.getActingPlayer().getLevel() - 1;
			if (lvlDiff >= Config.NPC_SKILL_DMG_PENALTY.size())
			{
				damage *= Config.NPC_SKILL_DMG_PENALTY.get(Config.NPC_SKILL_DMG_PENALTY.size() - 1);
			}
			else
			{
				damage *= Config.NPC_SKILL_DMG_PENALTY.get(lvlDiff);
			}
			
		}
		return Math.max(damage, 1);
	}
	
	/**
	 * Calculated damage caused by ATTACK of attacker on target.
	 * @param attacker player or NPC that makes ATTACK
	 * @param target player or NPC, target of ATTACK
	 * @param skill
	 * @param shld
	 * @param crit if the ATTACK have critical success
	 * @param ss if weapon item was charged by soulshot
	 * @return
	 */
	public static final double calcPhysDam(L2Character attacker, L2Character target, L2Skill skill, byte shld, boolean crit, boolean ss)
	{
		final boolean isPvP = attacker.isPlayable() && target.isPlayable();
		final boolean isPvE = attacker.isPlayable() && target.isL2Attackable();
		double damage = attacker.getPAtk(target);
		double defence = target.getPDef(attacker);
		
		// Defense bonuses in PvP fight
		if (isPvP)
		{
			defence *= (skill == null) ? target.calcStat(Stats.PVP_PHYSICAL_DEF, 1, null, null) : target.calcStat(Stats.PVP_PHYS_SKILL_DEF, 1, null, null);
		}
		
		switch (shld)
		{
			case SHIELD_DEFENSE_SUCCEED:
			{
				if (!Config.ALT_GAME_SHIELD_BLOCKS)
				{
					defence += target.getShldDef();
				}
				break;
			}
			case SHIELD_DEFENSE_PERFECT_BLOCK: // perfect block
			{
				return 1.;
			}
		}
		
		// Add soulshot boost.
		int ssBoost = ss ? 2 : 1;
		damage = (skill != null) ? ((damage * ssBoost) + skill.getPower(attacker, target, isPvP, isPvE)) : (damage * ssBoost);
		
		if (crit)
		{
			// Finally retail like formula
			damage = 2 * attacker.calcStat(Stats.CRITICAL_DAMAGE, 1, target, skill) * target.calcStat(Stats.CRIT_VULN, 1, target, null) * ((70 * damage) / defence);
			// Crit dmg add is almost useless in normal hits...
			damage += ((attacker.calcStat(Stats.CRITICAL_DAMAGE_ADD, 0, target, skill) * 70) / defence);
			damage += target.calcStat(Stats.CRIT_ADD_VULN, 0, target, skill);
		}
		else
		{
			damage = (70 * damage) / defence;
		}
		
		damage *= calcAttackTraitBonus(attacker, target);
		
		// Weapon random damage
		damage *= attacker.getRandomDamageMultiplier();
		if ((shld > 0) && Config.ALT_GAME_SHIELD_BLOCKS)
		{
			damage -= target.getShldDef();
			if (damage < 0)
			{
				damage = 0;
			}
		}
		
		if ((damage > 0) && (damage < 1))
		{
			damage = 1;
		}
		else if (damage < 0)
		{
			damage = 0;
		}
		
		// Dmg bonuses in PvP fight
		if (isPvP)
		{
			if (skill == null)
			{
				damage *= attacker.calcStat(Stats.PVP_PHYSICAL_DMG, 1, null, null);
			}
			else
			{
				damage *= attacker.calcStat(Stats.PVP_PHYS_SKILL_DMG, 1, null, null);
			}
		}
		
		// Physical skill dmg boost
		if (skill != null)
		{
			damage = attacker.calcStat(Stats.PHYSICAL_SKILL_POWER, damage, null, null);
		}
		
		damage *= calcAttributeBonus(attacker, target, skill);
		if (target.isL2Attackable())
		{
			final L2Weapon weapon = attacker.getActiveWeaponItem();
			if ((weapon != null) && ((weapon.getItemType() == L2WeaponType.BOW) || (weapon.getItemType() == L2WeaponType.CROSSBOW)))
			{
				if (skill != null)
				{
					damage *= attacker.calcStat(Stats.PVE_BOW_SKILL_DMG, 1, null, null);
				}
				else
				{
					damage *= attacker.calcStat(Stats.PVE_BOW_DMG, 1, null, null);
				}
			}
			else
			{
				damage *= attacker.calcStat(Stats.PVE_PHYSICAL_DMG, 1, null, null);
			}
			if (!target.isRaid() && !target.isRaidMinion() && (target.getLevel() >= Config.MIN_NPC_LVL_DMG_PENALTY) && (attacker.getActingPlayer() != null) && ((target.getLevel() - attacker.getActingPlayer().getLevel()) >= 2))
			{
				int lvlDiff = target.getLevel() - attacker.getActingPlayer().getLevel() - 1;
				if (skill != null)
				{
					if (lvlDiff >= Config.NPC_SKILL_DMG_PENALTY.size())
					{
						damage *= Config.NPC_SKILL_DMG_PENALTY.get(Config.NPC_SKILL_DMG_PENALTY.size() - 1);
					}
					else
					{
						damage *= Config.NPC_SKILL_DMG_PENALTY.get(lvlDiff);
					}
				}
				else if (crit)
				{
					if (lvlDiff >= Config.NPC_CRIT_DMG_PENALTY.size())
					{
						damage *= Config.NPC_CRIT_DMG_PENALTY.get(Config.NPC_CRIT_DMG_PENALTY.size() - 1);
					}
					else
					{
						damage *= Config.NPC_CRIT_DMG_PENALTY.get(lvlDiff);
					}
				}
				else
				{
					if (lvlDiff >= Config.NPC_DMG_PENALTY.size())
					{
						damage *= Config.NPC_DMG_PENALTY.get(Config.NPC_DMG_PENALTY.size() - 1);
					}
					else
					{
						damage *= Config.NPC_DMG_PENALTY.get(lvlDiff);
					}
				}
			}
		}
		return damage;
	}
	
	public static final double calcMagicDam(L2Character attacker, L2Character target, L2Skill skill, byte shld, boolean sps, boolean bss, boolean mcrit)
	{
		int mDef = target.getMDef(attacker, skill);
		switch (shld)
		{
			case SHIELD_DEFENSE_SUCCEED:
			{
				mDef += target.getShldDef();
				break;
			}
			case SHIELD_DEFENSE_PERFECT_BLOCK:
			{
				return 1;
			}
		}
		
		int mAtk = attacker.getMAtk(target, skill);
		final boolean isPvP = attacker.isPlayable() && target.isPlayable();
		final boolean isPvE = attacker.isPlayable() && target.isL2Attackable();
		
		// PvP bonuses for defense
		if (isPvP)
		{
			if (skill.isMagic())
			{
				mDef *= target.calcStat(Stats.PVP_MAGICAL_DEF, 1, null, null);
			}
			else
			{
				mDef *= target.calcStat(Stats.PVP_PHYS_SKILL_DEF, 1, null, null);
			}
		}
		
		// Bonus Spirit shot
		mAtk *= bss ? 4 : sps ? 2 : 1;
		// MDAM Formula.
		double damage = ((91 * Math.sqrt(mAtk)) / mDef) * skill.getPower(attacker, target, isPvP, isPvE);
		
		// Failure calculation
		if (Config.ALT_GAME_MAGICFAILURES && !calcMagicSuccess(attacker, target, skill))
		{
			if (attacker.isPlayer())
			{
				if (calcMagicSuccess(attacker, target, skill) && ((target.getLevel() - attacker.getLevel()) <= 9))
				{
					if (skill.hasEffectType(L2EffectType.HP_DRAIN))
					{
						attacker.sendPacket(SystemMessageId.DRAIN_HALF_SUCCESFUL);
					}
					else
					{
						attacker.sendPacket(SystemMessageId.ATTACK_FAILED);
					}
					damage /= 2;
				}
				else
				{
					SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_RESISTED_YOUR_S2);
					sm.addCharName(target);
					sm.addSkillName(skill);
					attacker.sendPacket(sm);
					damage = 1;
				}
			}
			
			if (target.isPlayer())
			{
				final SystemMessage sm = (skill.hasEffectType(L2EffectType.HP_DRAIN)) ? SystemMessage.getSystemMessage(SystemMessageId.RESISTED_C1_DRAIN) : SystemMessage.getSystemMessage(SystemMessageId.RESISTED_C1_MAGIC);
				sm.addCharName(attacker);
				target.sendPacket(sm);
			}
		}
		else if (mcrit)
		{
			damage *= attacker.isPlayer() && target.isPlayer() ? 2.5 : 3;
			damage *= attacker.calcStat(Stats.MAGIC_CRIT_DMG, 1, null, null);
		}
		
		// Weapon random damage
		damage *= attacker.getRandomDamageMultiplier();
		
		// PvP bonuses for damage
		if (isPvP)
		{
			Stats stat = skill.isMagic() ? Stats.PVP_MAGICAL_DMG : Stats.PVP_PHYS_SKILL_DMG;
			damage *= attacker.calcStat(stat, 1, null, null);
		}
		
		// CT2.3 general magic vuln
		damage *= target.calcStat(Stats.MAGIC_DAMAGE_VULN, 1, null, null);
		damage *= calcAttributeBonus(attacker, target, skill);
		
		if (target.isL2Attackable())
		{
			damage *= attacker.calcStat(Stats.PVE_MAGICAL_DMG, 1, null, null);
			if (!target.isRaid() && !target.isRaidMinion() && (target.getLevel() >= Config.MIN_NPC_LVL_DMG_PENALTY) && (attacker.getActingPlayer() != null) && ((target.getLevel() - attacker.getActingPlayer().getLevel()) >= 2))
			{
				int lvlDiff = target.getLevel() - attacker.getActingPlayer().getLevel() - 1;
				if (lvlDiff >= Config.NPC_SKILL_DMG_PENALTY.size())
				{
					damage *= Config.NPC_SKILL_DMG_PENALTY.get(Config.NPC_SKILL_DMG_PENALTY.size() - 1);
				}
				else
				{
					damage *= Config.NPC_SKILL_DMG_PENALTY.get(lvlDiff);
				}
			}
		}
		return damage;
	}
	
	public static final double calcMagicDam(L2CubicInstance attacker, L2Character target, L2Skill skill, boolean mcrit, byte shld)
	{
		int mDef = target.getMDef(attacker.getOwner(), skill);
		switch (shld)
		{
			case SHIELD_DEFENSE_SUCCEED:
				mDef += target.getShldDef(); // kamael
				break;
			case SHIELD_DEFENSE_PERFECT_BLOCK: // perfect block
				return 1;
		}
		
		int mAtk = attacker.getCubicPower();
		final boolean isPvP = target.isPlayable();
		final boolean isPvE = target.isL2Attackable();
		
		// Cubics MDAM Formula (similar to PDAM formula, but using 91 instead of 70, also resisted by mDef).
		double damage = 91 * ((mAtk + skill.getPower(isPvP, isPvE)) / mDef);
		
		// Failure calculation
		L2PcInstance owner = attacker.getOwner();
		if (Config.ALT_GAME_MAGICFAILURES && !calcMagicSuccess(owner, target, skill))
		{
			if (calcMagicSuccess(owner, target, skill) && ((target.getLevel() - skill.getMagicLevel()) <= 9))
			{
				if (skill.hasEffectType(L2EffectType.HP_DRAIN))
				{
					owner.sendPacket(SystemMessageId.DRAIN_HALF_SUCCESFUL);
				}
				else
				{
					owner.sendPacket(SystemMessageId.ATTACK_FAILED);
				}
				damage /= 2;
			}
			else
			{
				SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_RESISTED_YOUR_S2);
				sm.addCharName(target);
				sm.addSkillName(skill);
				owner.sendPacket(sm);
				damage = 1;
			}
			
			if (target.isPlayer())
			{
				if (skill.hasEffectType(L2EffectType.HP_DRAIN))
				{
					SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.RESISTED_C1_DRAIN);
					sm.addCharName(owner);
					target.sendPacket(sm);
				}
				else
				{
					SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.RESISTED_C1_MAGIC);
					sm.addCharName(owner);
					target.sendPacket(sm);
				}
			}
		}
		else if (mcrit)
		{
			damage *= 3;
		}
		
		// CT2.3 general magic vuln
		damage *= target.calcStat(Stats.MAGIC_DAMAGE_VULN, 1, null, null);
		damage *= calcAttributeBonus(owner, target, skill);
		
		if (target.isL2Attackable())
		{
			damage *= attacker.getOwner().calcStat(Stats.PVE_MAGICAL_DMG, 1, null, null);
			if (!target.isRaid() && !target.isRaidMinion() && (target.getLevel() >= Config.MIN_NPC_LVL_DMG_PENALTY) && (attacker.getOwner() != null) && ((target.getLevel() - attacker.getOwner().getLevel()) >= 2))
			{
				int lvlDiff = target.getLevel() - attacker.getOwner().getLevel() - 1;
				if (lvlDiff >= Config.NPC_SKILL_DMG_PENALTY.size())
				{
					damage *= Config.NPC_SKILL_DMG_PENALTY.get(Config.NPC_SKILL_DMG_PENALTY.size() - 1);
				}
				else
				{
					damage *= Config.NPC_SKILL_DMG_PENALTY.get(lvlDiff);
				}
			}
		}
		return damage;
	}
	
	/**
	 * Returns true in case of critical hit
	 * @param rate
	 * @param skill
	 * @param target
	 * @return
	 */
	public static final boolean calcCrit(double rate, boolean skill, L2Character target)
	{
		// support for critical damage evade
		if (rate > Rnd.get(1000))
		{
			if ((target == null) || skill)
			{
				return true; // no effect
			}
			
			// little weird, but remember what CRIT_DAMAGE_EVASION > 1 increase chances to _evade_ crit hits
			return Rnd.get((int) target.getStat().calcStat(Stats.CRIT_DAMAGE_EVASION, 100, null, null)) < 100;
		}
		return false;
	}
	
	public static final boolean calcMCrit(double mRate)
	{
		return mRate > Rnd.get(1000);
	}
	
	/**
	 * @param target
	 * @param dmg
	 * @return true in case when ATTACK is canceled due to hit
	 */
	public static final boolean calcAtkBreak(L2Character target, double dmg)
	{
		if (target.isChanneling())
		{
			return false;
		}
		
		double init = 0;
		
		if (Config.ALT_GAME_CANCEL_CAST && target.isCastingNow())
		{
			init = 15;
		}
		if (Config.ALT_GAME_CANCEL_BOW && target.isAttackingNow())
		{
			L2Weapon wpn = target.getActiveWeaponItem();
			if ((wpn != null) && (wpn.getItemType() == L2WeaponType.BOW))
			{
				init = 15;
			}
		}
		
		if (target.isRaid() || target.isInvul() || (init <= 0))
		{
			return false; // No attack break
		}
		
		// Chance of break is higher with higher dmg
		init += Math.sqrt(13 * dmg);
		
		// Chance is affected by target MEN
		init -= ((BaseStats.MEN.calcBonus(target) * 100) - 100);
		
		// Calculate all modifiers for ATTACK_CANCEL
		double rate = target.calcStat(Stats.ATTACK_CANCEL, init, null, null);
		
		// Adjust the rate to be between 1 and 99
		rate = Math.max(Math.min(rate, 99), 1);
		
		return Rnd.get(100) < rate;
	}
	
	/**
	 * Calculate delay (in milliseconds) before next ATTACK
	 * @param attacker
	 * @param target
	 * @param rate
	 * @return
	 */
	public static final int calcPAtkSpd(L2Character attacker, L2Character target, double rate)
	{
		// measured Oct 2006 by Tank6585, formula by Sami
		// attack speed 312 equals 1500 ms delay... (or 300 + 40 ms delay?)
		if (rate < 2)
		{
			return 2700;
		}
		return (int) (470000 / rate);
	}
	
	/**
	 * Calculate delay (in milliseconds) for skills cast
	 * @param attacker
	 * @param skill
	 * @param skillTime
	 * @return
	 */
	public static final int calcAtkSpd(L2Character attacker, L2Skill skill, double skillTime)
	{
		if (skill.isMagic())
		{
			return (int) ((skillTime / attacker.getMAtkSpd()) * 333);
		}
		return (int) ((skillTime / attacker.getPAtkSpd()) * 300);
	}
	
	/**
	 * Formula based on http://l2p.l2wh.com/nonskillattacks.html
	 * @param attacker
	 * @param target
	 * @return {@code true} if hit missed (target evaded), {@code false} otherwise.
	 */
	public static boolean calcHitMiss(L2Character attacker, L2Character target)
	{
		int chance = (80 + (2 * (attacker.getAccuracy() - target.getEvasionRate(attacker)))) * 10;
		
		// Get additional bonus from the conditions when you are attacking
		chance *= HitConditionBonus.getInstance().getConditionBonus(attacker, target);
		
		chance = Math.max(chance, 200);
		chance = Math.min(chance, 980);
		
		return chance < Rnd.get(1000);
	}
	
	/**
	 * Returns:<br>
	 * 0 = shield defense doesn't succeed<br>
	 * 1 = shield defense succeed<br>
	 * 2 = perfect block<br>
	 * @param attacker
	 * @param target
	 * @param skill
	 * @param sendSysMsg
	 * @return
	 */
	public static byte calcShldUse(L2Character attacker, L2Character target, L2Skill skill, boolean sendSysMsg)
	{
		if ((skill != null) && skill.ignoreShield())
		{
			return 0;
		}
		
		L2Item item = target.getSecondaryWeaponItem();
		if ((item == null) || !(item instanceof L2Armor) || (((L2Armor) item).getItemType() == L2ArmorType.SIGIL))
		{
			return 0;
		}
		
		double shldRate = target.calcStat(Stats.SHIELD_RATE, 0, attacker, null) * BaseStats.DEX.calcBonus(target);
		if (shldRate <= 1e-6)
		{
			return 0;
		}
		
		int degreeside = (int) target.calcStat(Stats.SHIELD_DEFENCE_ANGLE, 0, null, null) + 120;
		if ((degreeside < 360) && (!target.isFacing(attacker, degreeside)))
		{
			return 0;
		}
		
		byte shldSuccess = SHIELD_DEFENSE_FAILED;
		// if attacker
		// if attacker use bow and target wear shield, shield block rate is multiplied by 1.3 (30%)
		L2Weapon at_weapon = attacker.getActiveWeaponItem();
		if ((at_weapon != null) && (at_weapon.getItemType() == L2WeaponType.BOW))
		{
			shldRate *= 1.3;
		}
		
		if ((shldRate > 0) && ((100 - Config.ALT_PERFECT_SHLD_BLOCK) < Rnd.get(100)))
		{
			shldSuccess = SHIELD_DEFENSE_PERFECT_BLOCK;
		}
		else if (shldRate > Rnd.get(100))
		{
			shldSuccess = SHIELD_DEFENSE_SUCCEED;
		}
		
		if (sendSysMsg && target.isPlayer())
		{
			L2PcInstance enemy = target.getActingPlayer();
			
			switch (shldSuccess)
			{
				case SHIELD_DEFENSE_SUCCEED:
					enemy.sendPacket(SystemMessageId.SHIELD_DEFENCE_SUCCESSFULL);
					break;
				case SHIELD_DEFENSE_PERFECT_BLOCK:
					enemy.sendPacket(SystemMessageId.YOUR_EXCELLENT_SHIELD_DEFENSE_WAS_A_SUCCESS);
					break;
			}
		}
		
		return shldSuccess;
	}
	
	public static byte calcShldUse(L2Character attacker, L2Character target, L2Skill skill)
	{
		return calcShldUse(attacker, target, skill, true);
	}
	
	public static byte calcShldUse(L2Character attacker, L2Character target)
	{
		return calcShldUse(attacker, target, null, true);
	}
	
	public static boolean calcMagicAffected(L2Character actor, L2Character target, L2Skill skill)
	{
		// TODO: CHECK/FIX THIS FORMULA UP!!
		double defence = 0;
		if (skill.isActive() && skill.isBad())
		{
			defence = target.getMDef(actor, skill);
		}
		
		double attack = 2 * actor.getMAtk(target, skill) * calcGeneralTraitBonus(actor, target, skill.getTraitType(), false);
		double d = (attack - defence) / (attack + defence);
		
		if (skill.isDebuff())
		{
			if (target.calcStat(Stats.DEBUFF_IMMUNITY, 0, null, skill) > 0)
			{
				return false;
			}
		}
		
		d += 0.5 * Rnd.nextGaussian();
		return d > 0;
	}
	
	public static double calcLvlBonusMod(L2Character attacker, L2Character target, L2Skill skill)
	{
		int attackerLvl = skill.getMagicLevel() > 0 ? skill.getMagicLevel() : attacker.getLevel();
		double skillLvlBonusRateMod = 1 + (skill.getLvlBonusRate() / 100.);
		double lvlMod = 1 + ((attackerLvl - target.getLevel()) / 100.);
		return skillLvlBonusRateMod * lvlMod;
	}
	
	/**
	 * Calculates the effect landing success.<br>
	 * @param env the data transfer object
	 * @return {@code true} if the effect lands
	 */
	public static boolean calcEffectSuccess(Env env)
	{
		final L2Character attacker = env.getCharacter();
		final L2Character target = env.getTarget();
		final L2Skill skill = env.getSkill();
		final int activateRate = skill.getActivateRate();
		if ((activateRate == -1) || (skill.getBasicProperty() == BaseStats.NONE))
		{
			return true;
		}
		
		int magicLevel = skill.getMagicLevel();
		if (magicLevel <= -1)
		{
			magicLevel = target.getLevel() + 3;
		}
		
		int targetBaseStat = 0;
		switch (skill.getBasicProperty())
		{
			case STR:
				targetBaseStat = target.getSTR();
				break;
			case DEX:
				targetBaseStat = target.getDEX();
				break;
			case CON:
				targetBaseStat = target.getCON();
				break;
			case INT:
				targetBaseStat = target.getINT();
				break;
			case MEN:
				targetBaseStat = target.getMEN();
				break;
			case WIT:
				targetBaseStat = target.getWIT();
				break;
		}
		
		final double baseMod = ((((((magicLevel - target.getLevel()) + 3) * skill.getLvlBonusRate()) + activateRate) + 30.0) - targetBaseStat);
		final double elementMod = calcAttributeBonus(attacker, target, skill);
		final double traitMod = calcGeneralTraitBonus(attacker, target, skill.getTraitType(), false);
		final double buffDebuffMod = 1 + (target.calcStat(skill.isDebuff() ? Stats.DEBUFF_VULN : Stats.BUFF_VULN, 1, null, null) / 100);
		double mAtkMod = 1;
		
		if (skill.isMagic())
		{
			double mAtk = attacker.getMAtk(null, null);
			double val = 0;
			if (env.isBlessedSpiritShot())// only blessed spiritshot!
			{
				val = mAtk * 3.0;// 3.0 is the blessed spiritshot multiplier
			}
			val += mAtk;
			val = (Math.sqrt(val) / target.getMDef(null, null)) * 11.0;
			mAtkMod = val;
		}
		
		final double rate = baseMod * elementMod * traitMod * mAtkMod * buffDebuffMod;
		final double finalRate = Math.min(Math.max(rate, skill.getMinChance()), skill.getMaxChance());
		
		if (attacker.isDebug())
		{
			final StatsSet set = new StatsSet();
			set.set("baseMod", baseMod);
			set.set("elementMod", elementMod);
			set.set("traitMod", traitMod);
			set.set("mAtkMod", mAtkMod);
			set.set("buffDebuffMod", buffDebuffMod);
			set.set("rate", rate);
			set.set("finalRate", finalRate);
			Debug.sendSkillDebug(attacker, target, skill, set);
		}
		
		if (finalRate < Rnd.get(100))
		{
			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_RESISTED_YOUR_S2);
			sm.addCharName(env.getTarget());
			sm.addSkillName(env.getSkill());
			attacker.sendPacket(sm);
			return false;
		}
		return true;
	}
	
	public static boolean calcSkillSuccess(L2Character attacker, L2Character target, L2Skill skill, byte shld, boolean ss, boolean sps, boolean bss)
	{
		if (skill.isDebuff())
		{
			if (skill.getPower() == -1)
			{
				if (attacker.isDebug())
				{
					attacker.sendDebugMessage(skill.getName() + " ignoring resists");
				}
				return true;
			}
			else if (target.calcStat(Stats.DEBUFF_IMMUNITY, 0, null, skill) > 0)
			{
				return false;
			}
		}
		
		// Perfect Shield Block.
		if (shld == SHIELD_DEFENSE_PERFECT_BLOCK)
		{
			if (attacker.isDebug())
			{
				attacker.sendDebugMessage(skill.getName() + " blocked by shield");
			}
			
			return false;
		}
		
		// Calculate BaseRate.
		double baseRate = skill.getPower();
		double statMod = skill.getBasicProperty().calcBonus(target);
		double rate = (baseRate / statMod);
		
		// Lvl Bonus Modifier.
		double lvlBonusMod = calcLvlBonusMod(attacker, target, skill);
		rate *= lvlBonusMod;
		
		// Element Modifier.
		double elementMod = calcAttributeBonus(attacker, target, skill);
		rate *= elementMod;
		
		// Add Matk/Mdef Bonus (TODO: Pending)
		double finalRate = Math.min(Math.max(rate, skill.getMinChance()), skill.getMaxChance());
		if (attacker.isDebug())
		{
			final StatsSet set = new StatsSet();
			set.set("statMod", statMod);
			set.set("elementMod", elementMod);
			set.set("lvlBonusMod", lvlBonusMod);
			set.set("rate", rate);
			set.set("finalRate", finalRate);
			Debug.sendSkillDebug(attacker, target, skill, set);
		}
		
		// Check the Rate Limits.
		
		return (Rnd.get(100) < finalRate);
	}
	
	public static boolean calcCubicSkillSuccess(L2CubicInstance attacker, L2Character target, L2Skill skill, byte shld)
	{
		if (skill.isDebuff())
		{
			if (skill.getPower() == -1)
			{
				return true;
			}
			else if (target.calcStat(Stats.DEBUFF_IMMUNITY, 0, null, skill) > 0)
			{
				return false;
			}
		}
		
		// Perfect Shield Block.
		if (shld == SHIELD_DEFENSE_PERFECT_BLOCK)
		{
			return false;
		}
		
		// if target reflect this skill then the effect will fail
		if (!calcBuffDebuffReflection(target, skill))
		{
			return false;
		}
		
		// Calculate BaseRate.
		double baseRate = skill.getPower();
		double statMod = skill.getBasicProperty().calcBonus(target);
		double rate = (baseRate / statMod);
		
		// Resist Modifier.
		double resMod = calcGeneralTraitBonus(attacker.getOwner(), target, skill.getTraitType(), false);
		rate *= resMod;
		
		// Lvl Bonus Modifier.
		double lvlBonusMod = calcLvlBonusMod(attacker.getOwner(), target, skill);
		rate *= lvlBonusMod;
		
		// Element Modifier.
		double elementMod = calcAttributeBonus(attacker.getOwner(), target, skill);
		rate *= elementMod;
		
		// Add Matk/Mdef Bonus (TODO: Pending)
		
		// Check the Rate Limits.
		double finalRate = Math.min(Math.max(rate, skill.getMinChance()), skill.getMaxChance());
		
		if (attacker.getOwner().isDebug())
		{
			final StatsSet set = new StatsSet();
			set.set("baseMod", baseRate);
			set.set("resMod", resMod);
			set.set("statMod", statMod);
			set.set("elementMod", elementMod);
			set.set("lvlBonusMod", lvlBonusMod);
			set.set("rate", rate);
			set.set("finalRate", finalRate);
			Debug.sendSkillDebug(attacker.getOwner(), target, skill, set);
		}
		
		return (Rnd.get(100) < finalRate);
	}
	
	public static boolean calcMagicSuccess(L2Character attacker, L2Character target, L2Skill skill)
	{
		if (skill.getPower() == -1)
		{
			return true;
		}
		
		// FIXME: Fix this LevelMod Formula.
		int lvlDifference = (target.getLevel() - (skill.getMagicLevel() > 0 ? skill.getMagicLevel() : attacker.getLevel()));
		double lvlModifier = Math.pow(1.3, lvlDifference);
		float targetModifier = 1;
		if (target.isL2Attackable() && !target.isRaid() && !target.isRaidMinion() && (target.getLevel() >= Config.MIN_NPC_LVL_MAGIC_PENALTY) && (attacker.getActingPlayer() != null) && ((target.getLevel() - attacker.getActingPlayer().getLevel()) >= 3))
		{
			int lvlDiff = target.getLevel() - attacker.getActingPlayer().getLevel() - 2;
			if (lvlDiff >= Config.NPC_SKILL_CHANCE_PENALTY.size())
			{
				targetModifier = Config.NPC_SKILL_CHANCE_PENALTY.get(Config.NPC_SKILL_CHANCE_PENALTY.size() - 1);
			}
			else
			{
				targetModifier = Config.NPC_SKILL_CHANCE_PENALTY.get(lvlDiff);
			}
		}
		// general magic resist
		final double resModifier = target.calcStat(Stats.MAGIC_SUCCESS_RES, 1, null, skill);
		final double failureModifier = attacker.calcStat(Stats.MAGIC_FAILURE_RATE, 1, target, skill);
		int rate = 100 - Math.round((float) (lvlModifier * targetModifier * resModifier * failureModifier));
		
		if (attacker.isDebug())
		{
			final StatsSet set = new StatsSet();
			set.set("lvlDifference", lvlDifference);
			set.set("lvlModifier", lvlModifier);
			set.set("resModifier", resModifier);
			set.set("failureModifier", failureModifier);
			set.set("targetModifier", targetModifier);
			set.set("rate", rate);
			Debug.sendSkillDebug(attacker, target, skill, set);
		}
		
		return (Rnd.get(100) < rate);
	}
	
	public static double calcManaDam(L2Character attacker, L2Character target, L2Skill skill, byte shld, boolean sps, boolean bss, boolean mcrit)
	{
		// Formula: (SQR(M.Atk)*Power*(Target Max MP/97))/M.Def
		double mAtk = attacker.getMAtk(target, skill);
		double mDef = target.getMDef(attacker, skill);
		final boolean isPvP = attacker.isPlayable() && target.isPlayable();
		final boolean isPvE = attacker.isPlayable() && target.isL2Attackable();
		double mp = target.getMaxMp();
		
		switch (shld)
		{
			case SHIELD_DEFENSE_SUCCEED:
				mDef += target.getShldDef();
				break;
			case SHIELD_DEFENSE_PERFECT_BLOCK: // perfect block
				return 1;
		}
		
		// Bonus Spiritshot
		mAtk *= bss ? 4 : sps ? 2 : 1;
		
		double damage = (Math.sqrt(mAtk) * skill.getPower(attacker, target, isPvP, isPvE) * (mp / 97)) / mDef;
		damage *= calcGeneralTraitBonus(attacker, target, skill.getTraitType(), false);
		
		if (target.isL2Attackable())
		{
			damage *= attacker.calcStat(Stats.PVE_MAGICAL_DMG, 1, null, null);
			if (!target.isRaid() && !target.isRaidMinion() && (target.getLevel() >= Config.MIN_NPC_LVL_DMG_PENALTY) && (attacker.getActingPlayer() != null) && ((target.getLevel() - attacker.getActingPlayer().getLevel()) >= 2))
			{
				int lvlDiff = target.getLevel() - attacker.getActingPlayer().getLevel() - 1;
				if (lvlDiff >= Config.NPC_SKILL_DMG_PENALTY.size())
				{
					damage *= Config.NPC_SKILL_DMG_PENALTY.get(Config.NPC_SKILL_DMG_PENALTY.size() - 1);
				}
				else
				{
					damage *= Config.NPC_SKILL_DMG_PENALTY.get(lvlDiff);
				}
			}
		}
		
		// Failure calculation
		if (Config.ALT_GAME_MAGICFAILURES && !calcMagicSuccess(attacker, target, skill))
		{
			if (attacker.isPlayer())
			{
				SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_RESISTED_YOUR_S2);
				sm.addCharName(target);
				sm.addSkillName(skill);
				attacker.sendPacket(sm);
				damage /= 2;
			}
			
			if (target.isPlayer())
			{
				SystemMessage sm2 = SystemMessage.getSystemMessage(SystemMessageId.RESISTED_C1_MAGIC);
				sm2.addCharName(attacker);
				target.sendPacket(sm2);
			}
		}
		
		if (mcrit)
		{
			damage *= 3;
			attacker.sendPacket(SystemMessageId.CRITICAL_HIT_MAGIC);
		}
		return damage;
	}
	
	public static double calculateSkillResurrectRestorePercent(double baseRestorePercent, L2Character caster)
	{
		if ((baseRestorePercent == 0) || (baseRestorePercent == 100))
		{
			return baseRestorePercent;
		}
		
		double restorePercent = baseRestorePercent * BaseStats.WIT.calcBonus(caster);
		if ((restorePercent - baseRestorePercent) > 20.0)
		{
			restorePercent += 20.0;
		}
		
		restorePercent = Math.max(restorePercent, baseRestorePercent);
		restorePercent = Math.min(restorePercent, 90.0);
		
		return restorePercent;
	}
	
	public static boolean calcPhysicalSkillEvasion(L2Character activeChar, L2Character target, L2Skill skill)
	{
		if (skill.isMagic() || skill.isDebuff())
		{
			return false;
		}
		if (Rnd.get(100) < target.calcStat(Stats.P_SKILL_EVASION, 0, null, skill))
		{
			if (activeChar.isPlayer())
			{
				SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_DODGES_ATTACK);
				sm.addString(target.getName());
				activeChar.getActingPlayer().sendPacket(sm);
			}
			if (target.isPlayer())
			{
				SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.AVOIDED_C1_ATTACK2);
				sm.addString(activeChar.getName());
				target.getActingPlayer().sendPacket(sm);
			}
			return true;
		}
		return false;
	}
	
	public static boolean calcSkillMastery(L2Character actor, L2Skill sk)
	{
		// Static Skills are not affected by Skill Mastery.
		if (sk.isStatic())
		{
			return false;
		}
		
		double val = actor.getStat().calcStat(Stats.SKILL_MASTERY, 0, null, null);
		if (actor.isPlayer())
		{
			val *= (actor.getActingPlayer().isMageClass() ? BaseStats.INT : BaseStats.STR).calcBonus(actor);
		}
		
		return Rnd.get(100) < val;
	}
	
	/**
	 * Calculates the Attribute Bonus
	 * @param attacker
	 * @param target
	 * @param skill Can be {@code null} if there is no skill used for the attack.
	 * @return The attribute bonus
	 */
	public static double calcAttributeBonus(L2Character attacker, L2Character target, L2Skill skill)
	{
		int attack_attribute;
		int defence_attribute;
		
		if (skill != null)
		{
			if (skill.getElement() == -1)
			{
				attack_attribute = 0;
				defence_attribute = target.getDefenseElementValue((byte) -1);
			}
			else
			{
				if (attacker.getAttackElement() == skill.getElement())
				{
					attack_attribute = attacker.getAttackElementValue(attacker.getAttackElement()) + skill.getElementPower();
					defence_attribute = target.getDefenseElementValue(attacker.getAttackElement());
				}
				else
				{
					attack_attribute = skill.getElementPower();
					defence_attribute = target.getDefenseElementValue(skill.getElement());
				}
			}
		}
		else
		{
			attack_attribute = attacker.getAttackElementValue(attacker.getAttackElement());
			defence_attribute = target.getDefenseElementValue(attacker.getAttackElement());
		}
		
		double attack_attribute_mod = 0;
		double defence_attribute_mod = 0;
		
		if (attack_attribute >= 450)
		{
			if (defence_attribute >= 450)
			{
				attack_attribute_mod = 0.06909;
				defence_attribute_mod = 0.078;
			}
			else if (attack_attribute >= 350)
			{
				attack_attribute_mod = 0.0887;
				defence_attribute_mod = 0.1007;
			}
			else
			{
				attack_attribute_mod = 0.129;
				defence_attribute_mod = 0.1473;
			}
		}
		else if (attack_attribute >= 300)
		{
			if (defence_attribute >= 300)
			{
				attack_attribute_mod = 0.0887;
				defence_attribute_mod = 0.1007;
			}
			else if (defence_attribute >= 150)
			{
				attack_attribute_mod = 0.129;
				defence_attribute_mod = 0.1473;
			}
			else
			{
				attack_attribute_mod = 0.25;
				defence_attribute_mod = 0.2894;
			}
		}
		else if (attack_attribute >= 150)
		{
			if (defence_attribute >= 150)
			{
				attack_attribute_mod = 0.129;
				defence_attribute_mod = 0.1473;
			}
			else if (defence_attribute >= 0)
			{
				attack_attribute_mod = 0.25;
				defence_attribute_mod = 0.2894;
			}
			else
			{
				attack_attribute_mod = 0.4;
				defence_attribute_mod = 0.55;
			}
		}
		else if (attack_attribute >= -99)
		{
			if (defence_attribute >= 0)
			{
				attack_attribute_mod = 0.25;
				defence_attribute_mod = 0.2894;
			}
			else
			{
				attack_attribute_mod = 0.4;
				defence_attribute_mod = 0.55;
			}
		}
		else
		{
			if (defence_attribute >= 450)
			{
				attack_attribute_mod = 0.06909;
				defence_attribute_mod = 0.078;
			}
			else if (defence_attribute >= 350)
			{
				attack_attribute_mod = 0.0887;
				defence_attribute_mod = 0.1007;
			}
			else
			{
				attack_attribute_mod = 0.129;
				defence_attribute_mod = 0.1473;
			}
		}
		
		int attribute_diff = attack_attribute - defence_attribute;
		double min;
		double max;
		if (attribute_diff >= 300)
		{
			max = 100.0;
			min = -50;
		}
		else if (attribute_diff >= 150)
		{
			max = 70.0;
			min = -50;
		}
		else if (attribute_diff >= -150)
		{
			max = 40.0;
			min = -50;
		}
		else if (attribute_diff >= -300)
		{
			max = 40.0;
			min = -60;
		}
		else
		{
			max = 40.0;
			min = -80;
		}
		
		attack_attribute += 100;
		attack_attribute *= attack_attribute;
		
		attack_attribute_mod = (attack_attribute / 144.0) * attack_attribute_mod;
		
		defence_attribute += 100;
		defence_attribute *= defence_attribute;
		
		defence_attribute_mod = (defence_attribute / 169.0) * defence_attribute_mod;
		
		double attribute_mod_diff = attack_attribute_mod - defence_attribute_mod;
		
		attribute_mod_diff = Math.min(Math.max(attribute_mod_diff, min), max);
		
		double result = (attribute_mod_diff / 100.0) + 1;
		
		if (attacker.isPlayer() && target.isPlayer() && (result < 1.0))
		{
			result = 1.0;
		}
		
		return result;
	}
	
	public static void calcDamageReflected(L2Character attacker, L2Character target, L2Skill skill, boolean crit)
	{
		// Only melee skills can be reflected
		if (skill.isMagic() || (skill.getCastRange() > MELEE_ATTACK_RANGE))
		{
			return;
		}
		
		final double chance = target.calcStat(Stats.VENGEANCE_SKILL_PHYSICAL_DAMAGE, 0, target, skill);
		if (Rnd.get(100) < chance)
		{
			if (target.isPlayer())
			{
				final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.COUNTERED_C1_ATTACK);
				sm.addCharName(attacker);
				target.sendPacket(sm);
			}
			if (attacker.isPlayer())
			{
				final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_PERFORMING_COUNTERATTACK);
				sm.addCharName(target);
				attacker.sendPacket(sm);
			}
			
			double counterdmg = (((target.getPAtk(attacker) * 10.0) * 70.0) / attacker.getPDef(target));
			counterdmg *= calcWeaponTraitBonus(attacker, target);
			counterdmg *= calcGeneralTraitBonus(attacker, target, skill.getTraitType(), false);
			counterdmg *= calcAttributeBonus(attacker, target, skill);
			
			attacker.reduceCurrentHp(counterdmg, target, skill);
			if (crit) // XXX: It counters multiple times depending on how much effects skill has not on critical, but gotta be verified first!
			{
				attacker.reduceCurrentHp(counterdmg, target, skill);
			}
		}
	}
	
	/**
	 * Calculate buff/debuff reflection.
	 * @param target
	 * @param skill
	 * @return {@code true} if reflect, {@code false} otherwise.
	 */
	public static boolean calcBuffDebuffReflection(L2Character target, L2Skill skill)
	{
		boolean reflect = false;
		// Neither some special skills (like hero debuffs...) or those skills ignoring resistances can't be reflected
		if ((skill.getPower() == -1) || ((skill.isHeroSkill() && skill.isDebuff()) || (!skill.isDebuff() && skill.isBad())))
		{
			return reflect;
		}
		
		final double reflectChance = target.calcStat(skill.isMagic() ? Stats.REFLECT_SKILL_MAGIC : Stats.REFLECT_SKILL_PHYSIC, 0, null, skill);
		if (Rnd.get(100) < reflectChance)
		{
			reflect = true;
		}
		return reflect;
	}
	
	/**
	 * Calculate damage caused by falling
	 * @param cha
	 * @param fallHeight
	 * @return damage
	 */
	public static double calcFallDam(L2Character cha, int fallHeight)
	{
		if (!Config.ENABLE_FALLING_DAMAGE || (fallHeight < 0))
		{
			return 0;
		}
		final double damage = cha.calcStat(Stats.FALL, (fallHeight * cha.getMaxHp()) / 1000.0, null, null);
		return damage;
	}
	
	public static boolean calcBlowSuccess(L2Character activeChar, L2Character target, L2Skill skill)
	{
		double dexMod = BaseStats.DEX.calcBonus(activeChar);
		// Apply DEX Mod.
		double blowChance = skill.getBlowChance();
		// Apply Position Bonus (TODO: values are unconfirmed, possibly custom, remove or update when confirmed).
		double sideMod = (activeChar.isInFrontOfTarget()) ? 1 : (activeChar.isBehindTarget()) ? 2 : 1.5;
		// Apply all mods.
		double baseRate = blowChance * dexMod * sideMod;
		// Apply blow rates
		double rate = activeChar.calcStat(Stats.BLOW_RATE, baseRate, target, null);
		// Debug
		if (activeChar.isDebug())
		{
			final StatsSet set = new StatsSet();
			set.set("dexMod", dexMod);
			set.set("blowChance", blowChance);
			set.set("sideMod", sideMod);
			set.set("baseRate", baseRate);
			set.set("rate", rate);
			Debug.sendSkillDebug(activeChar, target, skill, set);
		}
		return Rnd.get(100) < rate;
	}
	
	public static List<BuffInfo> calcCancelStealEffects(L2Character activeChar, L2Character target, L2Skill skill, String slot, int rate, int max)
	{
		final List<BuffInfo> canceled = new ArrayList<>(max);
		switch (slot)
		{
			case "buff":
			{
				// Resist Modifier.
				int cancelMagicLvl = skill.getMagicLevel();
				final double vuln = target.calcStat(Stats.CANCEL_VULN, 0, target, null);
				final double prof = activeChar.calcStat(Stats.CANCEL_PROF, 0, target, null);
				double resMod = 1 + (((vuln + prof) * -1) / 100);
				double finalRate = rate / resMod;
				
				if (activeChar.isDebug())
				{
					final StatsSet set = new StatsSet();
					set.set("baseMod", rate);
					set.set("magicLevel", cancelMagicLvl);
					set.set("resMod", resMod);
					set.set("rate", finalRate);
					Debug.sendSkillDebug(activeChar, target, skill, set);
				}
				
				// Prevent initialization.
				final List<BuffInfo> buffs = target.getEffectList().hasBuffs() ? new ArrayList<>(target.getEffectList().getBuffs().values()) : new ArrayList<BuffInfo>(1);
				if (target.getEffectList().hasTriggered())
				{
					buffs.addAll(target.getEffectList().getTriggered().values());
				}
				if (target.getEffectList().hasDances())
				{
					buffs.addAll(target.getEffectList().getDances().values());
				}
				for (int i = buffs.size() - 1; i >= 0; i--) // reverse order
				{
					BuffInfo info = buffs.get(i);
					if (!info.getSkill().canBeStolen() || (!calcCancelSuccess(info, cancelMagicLvl, (int) finalRate, skill)))
					{
						continue;
					}
					canceled.add(info);
					if (canceled.size() >= max)
					{
						break;
					}
				}
				break;
			}
			case "debuff":
			{
				final List<BuffInfo> debuffs = new ArrayList<>(target.getEffectList().getDebuffs().values());
				for (int i = debuffs.size() - 1; i >= 0; i--)
				{
					BuffInfo info = debuffs.get(i);
					if (info.getSkill().isDebuff() && info.getSkill().canBeDispeled() && (Rnd.get(100) <= rate))
					{
						canceled.add(info);
						if (canceled.size() >= max)
						{
							break;
						}
					}
				}
				break;
			}
		}
		return canceled;
	}
	
	public static boolean calcCancelSuccess(BuffInfo info, int cancelMagicLvl, int rate, L2Skill skill)
	{
		// Lvl Bonus Modifier.
		rate *= info.getSkill().getMagicLevel() > 0 ? 1 + ((cancelMagicLvl - info.getSkill().getMagicLevel()) / 100.) : 1;
		return Rnd.get(100) < Math.min(Math.max(rate, skill.getMinChance()), skill.getMaxChance());
	}
	
	/**
	 * Calculates the abnormal time for an effect.<br>
	 * The abnormal time is taken from the skill definition, and it's global for all effects present in the skills.
	 * @param env the data transfer object with required information
	 * @return the time that the effect will last
	 */
	public static int calcEffectAbnormalTime(Env env)
	{
		final L2Character caster = env.getCharacter();
		final L2Character target = env.getTarget();
		final L2Skill skill = env.getSkill();
		int time = skill.isPassive() || skill.isToggle() ? -1 : skill.getAbnormalTime();
		
		// An herb buff will affect both master and servitor, but the buff duration will be half of the normal duration.
		// If a servitor is not summoned, the master will receive the full buff duration.
		if ((target != null) && target.isServitor() && skill.isAbnormalInstant())
		{
			time /= 2;
		}
		
		// If the skill is a mastery skill, the effect will last twice the default time.
		if (env.isSkillMastery())
		{
			time *= 2;
		}
		
		// Debuffs Duration Affected by Resistances.
		if ((caster != null) && (target != null) && skill.isDebuff())
		{
			double statMod = skill.getBasicProperty().calcBonus(target);
			double resMod = calcGeneralTraitBonus(caster, target, skill.getTraitType(), false);
			double lvlBonusMod = calcLvlBonusMod(caster, target, skill);
			double elementMod = calcAttributeBonus(caster, target, skill);
			time = (int) Math.ceil(Math.min(Math.max((time * resMod * lvlBonusMod * elementMod) / statMod, time * 0.5), time));
		}
		return time;
	}
	
	/**
	 * Calculate Probability in following effects:<br>
	 * TargetCancel,<br>
	 * TargetMeProbability,<br>
	 * SkillTurning,<br>
	 * Betray,<br>
	 * Bluff,<br>
	 * DeleteHate,<br>
	 * RandomizeHate,<br>
	 * DeleteHateOfMe,<br>
	 * TransferHate,<br>
	 * Confuse<br>
	 * @param baseChance chance from effect parameter
	 * @param attacker
	 * @param target
	 * @param skill
	 * @return chance for effect to succeed
	 */
	public static boolean calcProbability(double baseChance, L2Character attacker, L2Character target, L2Skill skill)
	{
		return Rnd.get(100) < ((((((skill.getMagicLevel() + baseChance) - target.getLevel()) + 30) - target.getINT()) * calcAttributeBonus(attacker, target, skill)) * calcGeneralTraitBonus(attacker, target, skill.getTraitType(), false));
	}
	
	/**
	 * Calculates karma lost upon death.
	 * @param player
	 * @param exp
	 * @return the amount of karma player has loosed.
	 */
	public static int calculateKarmaLost(L2PcInstance player, long exp)
	{
		double karmaLooseMul = KarmaData.getInstance().getMultiplier(player.getLevel());
		return (int) ((Math.abs(exp) / karmaLooseMul) / 15);
	}
	
	/**
	 * Calculates karma gain upon player kill.
	 * @param pkCount
	 * @param isSummon
	 * @return karma points that will be added to the player.
	 */
	public static int calculateKarmaGain(int pkCount, boolean isSummon)
	{
		int result = 14400;
		if (pkCount < 100)
		{
			result = (int) (((((pkCount - 1) * 0.5) + 1) * 60) * 4);
		}
		else if (pkCount < 180)
		{
			result = (int) (((((pkCount + 1) * 0.125) + 37.5) * 60) * 4);
		}
		
		if (isSummon)
		{
			result = ((pkCount & 3) + result) >> 2;
		}
		
		return result;
	}
	
	public static double calcGeneralTraitBonus(L2Character attacker, L2Character target, TraitType traitType, boolean ignoreResistance)
	{
		if (traitType == TraitType.NONE)
		{
			return 1.0;
		}
		
		if (target.getStat().isTraitInvul(traitType))
		{
			return 0;
		}
		
		switch (traitType.getType())
		{
			case 2:
			{
				if (!attacker.getStat().hasAttackTrait(traitType) || !target.getStat().hasDefenceTrait(traitType))
				{
					return 1.0;
				}
				break;
			}
			case 3:
			{
				if (ignoreResistance)
				{
					return 1.0;
				}
				break;
			}
			default:
			{
				return 1.0;
			}
		}
		
		final double result = (attacker.getStat().getAttackTrait(traitType) - target.getStat().getDefenceTrait(traitType)) + 1.0;
		return Math.min(Math.max(result, 0.05), 2.0);
	}
	
	public static double calcWeaponTraitBonus(L2Character attacker, L2Character target)
	{
		final TraitType type = attacker.getAttackType().getTraitType();
		double result = target.getStat().getDefenceTraits()[type.getId()] - 1.0;
		return 1.0 - result;
	}
	
	public static double calcAttackTraitBonus(L2Character attacker, L2Character target)
	{
		final double weaponTraitBonus = calcWeaponTraitBonus(attacker, target);
		if (weaponTraitBonus == 0)
		{
			return 0;
		}
		
		double weaknessBonus = 1.0;
		for (TraitType traitType : TraitType.values())
		{
			if (traitType.getType() == 2)
			{
				weaknessBonus *= calcGeneralTraitBonus(attacker, target, traitType, true);
				if (weaknessBonus == 0)
				{
					return 0;
				}
			}
		}
		
		return Math.min(Math.max(weaponTraitBonus * weaknessBonus, 0.05), 2.0);
	}
}
