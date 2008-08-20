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
package net.sf.l2j.gameserver.model.base;

/**
 * This class defines all classes (ex : human fighter, darkFighter...) that a player can chose.<BR><BR>
 *
 * Data :<BR><BR>
 * <li>id : The Identifier of the class</li>
 * <li>isMage : True if the class is a mage class</li>
 * <li>race : The race of this class</li>
 * <li>parent : The parent ClassId or null if this class is the root</li><BR><BR>
 *
 * @version $Revision: 1.4.4.4 $ $Date: 2005/03/27 15:29:33 $
 *
 */
public enum ClassId
{
	fighter(0x00, false, Race.Human, null),
	
	warrior(0x01, false, Race.Human, fighter),
	gladiator(0x02, false, Race.Human, warrior),
	warlord(0x03, false, Race.Human, warrior),
	knight(0x04, false, Race.Human, fighter),
	paladin(0x05, false, Race.Human, knight),
	darkAvenger(0x06, false, Race.Human, knight),
	rogue(0x07, false, Race.Human, fighter),
	treasureHunter(0x08, false, Race.Human, rogue),
	hawkeye(0x09, false, Race.Human, rogue),
	
	mage(0x0a, true, Race.Human, null),
	wizard(0x0b, true, Race.Human, mage),
	sorceror(0x0c, true, Race.Human, wizard),
	necromancer(0x0d, true, Race.Human, wizard),
	warlock(0x0e, true, Race.Human, wizard),
	cleric(0x0f, true, Race.Human, mage),
	bishop(0x10, true, Race.Human, cleric),
	prophet(0x11, true, Race.Human, cleric),
	
	elvenFighter(0x12, false, Race.Elf, null),
	elvenKnight(0x13, false, Race.Elf, elvenFighter),
	templeKnight(0x14, false, Race.Elf, elvenKnight),
	swordSinger(0x15, false, Race.Elf, elvenKnight),
	elvenScout(0x16, false, Race.Elf, elvenFighter),
	plainsWalker(0x17, false, Race.Elf, elvenScout),
	silverRanger(0x18, false, Race.Elf, elvenScout),
	
	elvenMage(0x19, true, Race.Elf, null),
	elvenWizard(0x1a, true, Race.Elf, elvenMage),
	spellsinger(0x1b, true, Race.Elf, elvenWizard),
	elementalSummoner(0x1c, true, Race.Elf, elvenWizard),
	oracle(0x1d, true, Race.Elf, elvenMage),
	elder(0x1e, true, Race.Elf, oracle),
	
	darkFighter(0x1f, false, Race.DarkElf, null),
	palusKnight(0x20, false, Race.DarkElf, darkFighter),
	shillienKnight(0x21, false, Race.DarkElf, palusKnight),
	bladedancer(0x22, false, Race.DarkElf, palusKnight),
	assassin(0x23, false, Race.DarkElf, darkFighter),
	abyssWalker(0x24, false, Race.DarkElf, assassin),
	phantomRanger(0x25, false, Race.DarkElf, assassin),
	
	darkMage(0x26, true, Race.DarkElf, null),
	darkWizard(0x27, true, Race.DarkElf, darkMage),
	spellhowler(0x28, true, Race.DarkElf, darkWizard),
	phantomSummoner(0x29, true, Race.DarkElf, darkWizard),
	shillienOracle(0x2a, true, Race.DarkElf, darkMage),
	shillenElder(0x2b, true, Race.DarkElf, shillienOracle),
	
	orcFighter(0x2c, false, Race.Orc, null),
	orcRaider(0x2d, false, Race.Orc, orcFighter),
	destroyer(0x2e, false, Race.Orc, orcRaider),
	orcMonk(0x2f, false, Race.Orc, orcFighter),
	tyrant(0x30, false, Race.Orc, orcMonk),
	
	orcMage(0x31, false, Race.Orc, null),
	orcShaman(0x32, true, Race.Orc, orcMage),
	overlord(0x33, true, Race.Orc, orcShaman),
	warcryer(0x34, true, Race.Orc, orcShaman),
	
	dwarvenFighter(0x35, false, Race.Dwarf, null),
	scavenger(0x36, false, Race.Dwarf, dwarvenFighter),
	bountyHunter(0x37, false, Race.Dwarf, scavenger),
	artisan(0x38, false, Race.Dwarf, dwarvenFighter),
	warsmith(0x39, false, Race.Dwarf, artisan),
	
	/*
	 * Dummy Entries (id's already in decimal format)
	 * btw FU NCSoft for the amount of work you put me
	 * through to do this!!
	 * <START>
	 */
	dummyEntry1(58, false, null, null),
	dummyEntry2(59, false, null, null),
	dummyEntry3(60, false, null, null),
	dummyEntry4(61, false, null, null),
	dummyEntry5(62, false, null, null),
	dummyEntry6(63, false, null, null),
	dummyEntry7(64, false, null, null),
	dummyEntry8(65, false, null, null),
	dummyEntry9(66, false, null, null),
	dummyEntry10(67, false, null, null),
	dummyEntry11(68, false, null, null),
	dummyEntry12(69, false, null, null),
	dummyEntry13(70, false, null, null),
	dummyEntry14(71, false, null, null),
	dummyEntry15(72, false, null, null),
	dummyEntry16(73, false, null, null),
	dummyEntry17(74, false, null, null),
	dummyEntry18(75, false, null, null),
	dummyEntry19(76, false, null, null),
	dummyEntry20(77, false, null, null),
	dummyEntry21(78, false, null, null),
	dummyEntry22(79, false, null, null),
	dummyEntry23(80, false, null, null),
	dummyEntry24(81, false, null, null),
	dummyEntry25(82, false, null, null),
	dummyEntry26(83, false, null, null),
	dummyEntry27(84, false, null, null),
	dummyEntry28(85, false, null, null),
	dummyEntry29(86, false, null, null),
	dummyEntry30(87, false, null, null),
	/*
	 * <END>
	 * Of Dummy entries
	 */

	/*
	 * Now the bad boys! new class ids :)) (3rd classes)
	 */
	duelist(0x58, false, Race.Human, gladiator),
	dreadnought(0x59, false, Race.Human, warlord),
	phoenixKnight(0x5a, false, Race.Human, paladin),
	hellKnight(0x5b, false, Race.Human, darkAvenger),
	sagittarius(0x5c, false, Race.Human, hawkeye),
	adventurer(0x5d, false, Race.Human, treasureHunter),
	archmage(0x5e, true, Race.Human, sorceror),
	soultaker(0x5f, true, Race.Human, necromancer),
	arcanaLord(0x60, true, Race.Human, warlock),
	cardinal(0x61, true, Race.Human, bishop),
	hierophant(0x62, true, Race.Human, prophet),
	
	evaTemplar(0x63, false, Race.Elf, templeKnight),
	swordMuse(0x64, false, Race.Elf, swordSinger),
	windRider(0x65, false, Race.Elf, plainsWalker),
	moonlightSentinel(0x66, false, Race.Elf, silverRanger),
	mysticMuse(0x67, true, Race.Elf, spellsinger),
	elementalMaster(0x68, true, Race.Elf, elementalSummoner),
	evaSaint(0x69, true, Race.Elf, elder),
	
	shillienTemplar(0x6a, false, Race.DarkElf, shillienKnight),
	spectralDancer(0x6b, false, Race.DarkElf, bladedancer),
	ghostHunter(0x6c, false, Race.DarkElf, abyssWalker),
	ghostSentinel(0x6d, false, Race.DarkElf, phantomRanger),
	stormScreamer(0x6e, true, Race.DarkElf, spellhowler),
	spectralMaster(0x6f, true, Race.DarkElf, phantomSummoner),
	shillienSaint(0x70, true, Race.DarkElf, shillenElder),
	
	titan(0x71, false, Race.Orc, destroyer),
	grandKhauatari(0x72, false, Race.Orc, tyrant),
	dominator(0x73, true, Race.Orc, overlord),
	doomcryer(0x74, true, Race.Orc, warcryer),
	
	fortuneSeeker(0x75, false, Race.Dwarf, bountyHunter),
	maestro(0x76, false, Race.Dwarf, warsmith),
	
	dummyEntry31(0x77, false, null, null),
	dummyEntry32(0x78, false, null, null),
	dummyEntry33(0x79, false, null, null),
	dummyEntry34(0x7a, false, null, null),
	
	maleSoldier(0x7b, false, Race.Kamael, null),
	femaleSoldier(0x7C, false, Race.Kamael, null),
	trooper(0x7D, false, Race.Kamael, maleSoldier),
	warder(0x7E, false, Race.Kamael, femaleSoldier),
	berserker(0x7F, false, Race.Kamael, trooper),
	maleSoulbreaker(0x80, false, Race.Kamael, trooper),
	femaleSoulbreaker(0x81, false, Race.Kamael, warder),
	arbalester(0x82, false, Race.Kamael, warder),
	doombringer(0x83, false, Race.Kamael, berserker),
	maleSoulhound(0x84, false, Race.Kamael, maleSoulbreaker),
	femaleSoulhound(0x85, false, Race.Kamael, femaleSoulbreaker),
	trickster(0x86, false, Race.Kamael, arbalester),
	inspector(0x87, false, Race.Kamael, null), //Hidden subclass, no first class which it comes from
	judicator(0x88, false, Race.Kamael, inspector);
	
	/** The Identifier of the Class */
	private final int _id;
	
	/** True if the class is a mage class */
	private final boolean _isMage;
	
	/** The Race object of the class */
	private final Race _race;
	
	/** The parent ClassId or null if this class is a root */
	private final ClassId _parent;
	
	/**
	 * Constructor of ClassId.<BR><BR>
	 */
	private ClassId(int pId, boolean pIsMage, Race pRace, ClassId pParent)
	{
		this._id = pId;
		this._isMage = pIsMage;
		this._race = pRace;
		this._parent = pParent;
	}
	
	/**
	 * Return the Identifier of the Class.<BR><BR>
	 */
	public final int getId()
	{
		return _id;
	}
	
	/**
	 * Return True if the class is a mage class.<BR><BR>
	 */
	public final boolean isMage()
	{
		return _isMage;
	}
	
	/**
	 * Return the Race object of the class.<BR><BR>
	 */
	public final Race getRace()
	{
		return _race;
	}
	
	/**
	 * Return True if this Class is a child of the selected ClassId.<BR><BR>
	 *
	 * @param cid The parent ClassId to check
	 *
	 */
	public final boolean childOf(ClassId cid)
	{
		if (_parent == null)
			return false;
		
		if (_parent == cid)
			return true;
		
		return _parent.childOf(cid);
		
	}
	
	/**
	 * Return True if this Class is equal to the selected ClassId or a child of the selected ClassId.<BR><BR>
	 *
	 * @param cid The parent ClassId to check
	 *
	 */
	public final boolean equalsOrChildOf(ClassId cid)
	{
		return this == cid || childOf(cid);
	}
	
	/**
	 * Return the child level of this Class (0=root, 1=child leve 1...).<BR><BR>
	 *
	 * @param cid The parent ClassId to check
	 *
	 */
	public final int level()
	{
		if (_parent == null)
			return 0;
		
		return 1 + _parent.level();
	}
	
	/**
	 * Return its parent ClassId<BR><BR>
	 *
	 */
	public final ClassId getParent()
	{
		return _parent;
	}
	
}
