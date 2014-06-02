/*
 * Copyright (C) 2004-2014 L2J Server
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
package com.l2jserver.gameserver.model.base;

import static com.l2jserver.gameserver.model.base.ClassLevel.First;
import static com.l2jserver.gameserver.model.base.ClassLevel.Fourth;
import static com.l2jserver.gameserver.model.base.ClassLevel.Second;
import static com.l2jserver.gameserver.model.base.ClassLevel.Third;
import static com.l2jserver.gameserver.model.base.ClassType.Fighter;
import static com.l2jserver.gameserver.model.base.ClassType.Mystic;
import static com.l2jserver.gameserver.model.base.ClassType.Priest;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Set;

import com.l2jserver.Config;
import com.l2jserver.gameserver.enums.PcRace;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author luisantonioa
 */
public enum PlayerClass
{
	HumanFighter(PcRace.HUMAN, Fighter, First),
	Warrior(PcRace.HUMAN, Fighter, Second),
	Gladiator(PcRace.HUMAN, Fighter, Third),
	Warlord(PcRace.HUMAN, Fighter, Third),
	HumanKnight(PcRace.HUMAN, Fighter, Second),
	Paladin(PcRace.HUMAN, Fighter, Third),
	DarkAvenger(PcRace.HUMAN, Fighter, Third),
	Rogue(PcRace.HUMAN, Fighter, Second),
	TreasureHunter(PcRace.HUMAN, Fighter, Third),
	Hawkeye(PcRace.HUMAN, Fighter, Third),
	HumanMystic(PcRace.HUMAN, Mystic, First),
	HumanWizard(PcRace.HUMAN, Mystic, Second),
	Sorceror(PcRace.HUMAN, Mystic, Third),
	Necromancer(PcRace.HUMAN, Mystic, Third),
	Warlock(PcRace.HUMAN, Mystic, Third),
	Cleric(PcRace.HUMAN, Priest, Second),
	Bishop(PcRace.HUMAN, Priest, Third),
	Prophet(PcRace.HUMAN, Priest, Third),
	
	ElvenFighter(PcRace.ELF, Fighter, First),
	ElvenKnight(PcRace.ELF, Fighter, Second),
	TempleKnight(PcRace.ELF, Fighter, Third),
	Swordsinger(PcRace.ELF, Fighter, Third),
	ElvenScout(PcRace.ELF, Fighter, Second),
	Plainswalker(PcRace.ELF, Fighter, Third),
	SilverRanger(PcRace.ELF, Fighter, Third),
	ElvenMystic(PcRace.ELF, Mystic, First),
	ElvenWizard(PcRace.ELF, Mystic, Second),
	Spellsinger(PcRace.ELF, Mystic, Third),
	ElementalSummoner(PcRace.ELF, Mystic, Third),
	ElvenOracle(PcRace.ELF, Priest, Second),
	ElvenElder(PcRace.ELF, Priest, Third),
	
	DarkElvenFighter(PcRace.DARK_ELF, Fighter, First),
	PalusKnight(PcRace.DARK_ELF, Fighter, Second),
	ShillienKnight(PcRace.DARK_ELF, Fighter, Third),
	Bladedancer(PcRace.DARK_ELF, Fighter, Third),
	Assassin(PcRace.DARK_ELF, Fighter, Second),
	AbyssWalker(PcRace.DARK_ELF, Fighter, Third),
	PhantomRanger(PcRace.DARK_ELF, Fighter, Third),
	DarkElvenMystic(PcRace.DARK_ELF, Mystic, First),
	DarkElvenWizard(PcRace.DARK_ELF, Mystic, Second),
	Spellhowler(PcRace.DARK_ELF, Mystic, Third),
	PhantomSummoner(PcRace.DARK_ELF, Mystic, Third),
	ShillienOracle(PcRace.DARK_ELF, Priest, Second),
	ShillienElder(PcRace.DARK_ELF, Priest, Third),
	
	OrcFighter(PcRace.ORC, Fighter, First),
	OrcRaider(PcRace.ORC, Fighter, Second),
	Destroyer(PcRace.ORC, Fighter, Third),
	OrcMonk(PcRace.ORC, Fighter, Second),
	Tyrant(PcRace.ORC, Fighter, Third),
	OrcMystic(PcRace.ORC, Mystic, First),
	OrcShaman(PcRace.ORC, Mystic, Second),
	Overlord(PcRace.ORC, Mystic, Third),
	Warcryer(PcRace.ORC, Mystic, Third),
	
	DwarvenFighter(PcRace.DWARF, Fighter, First),
	DwarvenScavenger(PcRace.DWARF, Fighter, Second),
	BountyHunter(PcRace.DWARF, Fighter, Third),
	DwarvenArtisan(PcRace.DWARF, Fighter, Second),
	Warsmith(PcRace.DWARF, Fighter, Third),
	
	dummyEntry1(null, null, null),
	dummyEntry2(null, null, null),
	dummyEntry3(null, null, null),
	dummyEntry4(null, null, null),
	dummyEntry5(null, null, null),
	dummyEntry6(null, null, null),
	dummyEntry7(null, null, null),
	dummyEntry8(null, null, null),
	dummyEntry9(null, null, null),
	dummyEntry10(null, null, null),
	dummyEntry11(null, null, null),
	dummyEntry12(null, null, null),
	dummyEntry13(null, null, null),
	dummyEntry14(null, null, null),
	dummyEntry15(null, null, null),
	dummyEntry16(null, null, null),
	dummyEntry17(null, null, null),
	dummyEntry18(null, null, null),
	dummyEntry19(null, null, null),
	dummyEntry20(null, null, null),
	dummyEntry21(null, null, null),
	dummyEntry22(null, null, null),
	dummyEntry23(null, null, null),
	dummyEntry24(null, null, null),
	dummyEntry25(null, null, null),
	dummyEntry26(null, null, null),
	dummyEntry27(null, null, null),
	dummyEntry28(null, null, null),
	dummyEntry29(null, null, null),
	dummyEntry30(null, null, null),
	/*
	 * (3rd classes)
	 */
	duelist(PcRace.HUMAN, Fighter, Fourth),
	dreadnought(PcRace.HUMAN, Fighter, Fourth),
	phoenixKnight(PcRace.HUMAN, Fighter, Fourth),
	hellKnight(PcRace.HUMAN, Fighter, Fourth),
	sagittarius(PcRace.HUMAN, Fighter, Fourth),
	adventurer(PcRace.HUMAN, Fighter, Fourth),
	archmage(PcRace.HUMAN, Mystic, Fourth),
	soultaker(PcRace.HUMAN, Mystic, Fourth),
	arcanaLord(PcRace.HUMAN, Mystic, Fourth),
	cardinal(PcRace.HUMAN, Priest, Fourth),
	hierophant(PcRace.HUMAN, Priest, Fourth),
	
	evaTemplar(PcRace.ELF, Fighter, Fourth),
	swordMuse(PcRace.ELF, Fighter, Fourth),
	windRider(PcRace.ELF, Fighter, Fourth),
	moonlightSentinel(PcRace.ELF, Fighter, Fourth),
	mysticMuse(PcRace.ELF, Mystic, Fourth),
	elementalMaster(PcRace.ELF, Mystic, Fourth),
	evaSaint(PcRace.ELF, Priest, Fourth),
	
	shillienTemplar(PcRace.DARK_ELF, Fighter, Fourth),
	spectralDancer(PcRace.DARK_ELF, Fighter, Fourth),
	ghostHunter(PcRace.DARK_ELF, Fighter, Fourth),
	ghostSentinel(PcRace.DARK_ELF, Fighter, Fourth),
	stormScreamer(PcRace.DARK_ELF, Mystic, Fourth),
	spectralMaster(PcRace.DARK_ELF, Mystic, Fourth),
	shillienSaint(PcRace.DARK_ELF, Priest, Fourth),
	
	titan(PcRace.ORC, Fighter, Fourth),
	grandKhavatari(PcRace.ORC, Fighter, Fourth),
	dominator(PcRace.ORC, Mystic, Fourth),
	doomcryer(PcRace.ORC, Mystic, Fourth),
	
	fortuneSeeker(PcRace.DWARF, Fighter, Fourth),
	maestro(PcRace.DWARF, Fighter, Fourth),
	
	dummyEntry31(null, null, null),
	dummyEntry32(null, null, null),
	dummyEntry33(null, null, null),
	dummyEntry34(null, null, null),
	
	maleSoldier(PcRace.KAMAEL, Fighter, First),
	femaleSoldier(PcRace.KAMAEL, Fighter, First),
	trooper(PcRace.KAMAEL, Fighter, Second),
	warder(PcRace.KAMAEL, Fighter, Second),
	berserker(PcRace.KAMAEL, Fighter, Third),
	maleSoulbreaker(PcRace.KAMAEL, Fighter, Third),
	femaleSoulbreaker(PcRace.KAMAEL, Fighter, Third),
	arbalester(PcRace.KAMAEL, Fighter, Third),
	doombringer(PcRace.KAMAEL, Fighter, Fourth),
	maleSoulhound(PcRace.KAMAEL, Fighter, Fourth),
	femaleSoulhound(PcRace.KAMAEL, Fighter, Fourth),
	trickster(PcRace.KAMAEL, Fighter, Fourth),
	inspector(PcRace.KAMAEL, Fighter, Third),
	judicator(PcRace.KAMAEL, Fighter, Fourth);
	
	private PcRace _race;
	private ClassLevel _level;
	private ClassType _type;
	
	private static final Set<PlayerClass> mainSubclassSet;
	private static final Set<PlayerClass> neverSubclassed = EnumSet.of(Overlord, Warsmith);
	
	private static final Set<PlayerClass> subclasseSet1 = EnumSet.of(DarkAvenger, Paladin, TempleKnight, ShillienKnight);
	private static final Set<PlayerClass> subclasseSet2 = EnumSet.of(TreasureHunter, AbyssWalker, Plainswalker);
	private static final Set<PlayerClass> subclasseSet3 = EnumSet.of(Hawkeye, SilverRanger, PhantomRanger);
	private static final Set<PlayerClass> subclasseSet4 = EnumSet.of(Warlock, ElementalSummoner, PhantomSummoner);
	private static final Set<PlayerClass> subclasseSet5 = EnumSet.of(Sorceror, Spellsinger, Spellhowler);
	
	private static final EnumMap<PlayerClass, Set<PlayerClass>> subclassSetMap = new EnumMap<>(PlayerClass.class);
	
	static
	{
		Set<PlayerClass> subclasses = getSet(null, Third);
		subclasses.removeAll(neverSubclassed);
		
		mainSubclassSet = subclasses;
		
		subclassSetMap.put(DarkAvenger, subclasseSet1);
		subclassSetMap.put(Paladin, subclasseSet1);
		subclassSetMap.put(TempleKnight, subclasseSet1);
		subclassSetMap.put(ShillienKnight, subclasseSet1);
		
		subclassSetMap.put(TreasureHunter, subclasseSet2);
		subclassSetMap.put(AbyssWalker, subclasseSet2);
		subclassSetMap.put(Plainswalker, subclasseSet2);
		
		subclassSetMap.put(Hawkeye, subclasseSet3);
		subclassSetMap.put(SilverRanger, subclasseSet3);
		subclassSetMap.put(PhantomRanger, subclasseSet3);
		
		subclassSetMap.put(Warlock, subclasseSet4);
		subclassSetMap.put(ElementalSummoner, subclasseSet4);
		subclassSetMap.put(PhantomSummoner, subclasseSet4);
		
		subclassSetMap.put(Sorceror, subclasseSet5);
		subclassSetMap.put(Spellsinger, subclasseSet5);
		subclassSetMap.put(Spellhowler, subclasseSet5);
	}
	
	PlayerClass(PcRace pRace, ClassType pType, ClassLevel pLevel)
	{
		_race = pRace;
		_level = pLevel;
		_type = pType;
	}
	
	public final Set<PlayerClass> getAvailableSubclasses(L2PcInstance player)
	{
		Set<PlayerClass> subclasses = null;
		
		if (_level == Third)
		{
			if (player.getRace() != PcRace.KAMAEL)
			{
				subclasses = EnumSet.copyOf(mainSubclassSet);
				
				subclasses.remove(this);
				
				switch (player.getRace())
				{
					case ELF:
						subclasses.removeAll(getSet(PcRace.DARK_ELF, Third));
						break;
					case DARK_ELF:
						subclasses.removeAll(getSet(PcRace.ELF, Third));
						break;
				}
				
				subclasses.removeAll(getSet(PcRace.KAMAEL, Third));
				
				Set<PlayerClass> unavailableClasses = subclassSetMap.get(this);
				
				if (unavailableClasses != null)
				{
					subclasses.removeAll(unavailableClasses);
				}
				
			}
			else
			{
				subclasses = getSet(PcRace.KAMAEL, Third);
				subclasses.remove(this);
				// Check sex, male subclasses female and vice versa
				// If server owner set MaxSubclass > 3 some kamael's cannot take 4 sub
				// So, in that situation we must skip sex check
				if (Config.MAX_SUBCLASS <= 3)
				{
					if (player.getAppearance().getSex())
					{
						subclasses.removeAll(EnumSet.of(femaleSoulbreaker));
					}
					else
					{
						subclasses.removeAll(EnumSet.of(maleSoulbreaker));
					}
				}
				if (!player.getSubClasses().containsKey(2) || (player.getSubClasses().get(2).getLevel() < 75))
				{
					subclasses.removeAll(EnumSet.of(inspector));
				}
			}
		}
		return subclasses;
	}
	
	public static final EnumSet<PlayerClass> getSet(PcRace race, ClassLevel level)
	{
		EnumSet<PlayerClass> allOf = EnumSet.noneOf(PlayerClass.class);
		
		for (PlayerClass playerClass : EnumSet.allOf(PlayerClass.class))
		{
			if ((race == null) || playerClass.isOfRace(race))
			{
				if ((level == null) || playerClass.isOfLevel(level))
				{
					allOf.add(playerClass);
				}
			}
		}
		return allOf;
	}
	
	public final boolean isOfRace(PcRace pRace)
	{
		return _race == pRace;
	}
	
	public final boolean isOfType(ClassType pType)
	{
		return _type == pType;
	}
	
	public final boolean isOfLevel(ClassLevel pLevel)
	{
		return _level == pLevel;
	}
	
	public final ClassLevel getLevel()
	{
		return _level;
	}
}
