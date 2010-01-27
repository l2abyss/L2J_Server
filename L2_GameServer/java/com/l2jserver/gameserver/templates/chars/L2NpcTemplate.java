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
package com.l2jserver.gameserver.templates.chars;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.l2jserver.gameserver.model.L2DropCategory;
import com.l2jserver.gameserver.model.L2DropData;
import com.l2jserver.gameserver.model.L2MinionData;
import com.l2jserver.gameserver.model.L2Skill;
import com.l2jserver.gameserver.model.actor.instance.L2XmassTreeInstance;
import com.l2jserver.gameserver.model.base.ClassId;
import com.l2jserver.gameserver.model.quest.Quest;
import com.l2jserver.gameserver.model.L2NpcAIData;
import com.l2jserver.gameserver.skills.Stats;
import com.l2jserver.gameserver.templates.StatsSet;

import javolution.util.FastList;
import javolution.util.FastMap;

/**
 * This cl contains all generic data of a L2Spawn object.<BR><BR>
 *
 * <B><U> Data</U> :</B><BR><BR>
 * <li>npcId, type, name, sex</li>
 * <li>rewardExp, rewardSp</li>
 * <li>aggroRange, factionId, factionRange</li>
 * <li>rhand, lhand, armor</li>
 * <li>isUndead</li>
 * <li>_drops</li>
 * <li>_minions</li>
 * <li>_teachInfo</li>
 * <li>_skills</li>
 * <li>_questsStart</li><BR><BR>
 *
 * @version $Revision: 1.1.2.4 $ $Date: 2005/04/02 15:57:51 $
 */
public final class L2NpcTemplate extends L2CharTemplate
{
	protected static final Logger _log = Logger.getLogger(Quest.class.getName());
	
	public final int npcId;
	public final int idTemplate;
	public final String type;
	public final String name;
	public final boolean serverSideName;
	public final String title;
	public final boolean serverSideTitle;
	public final String sex;
	public final byte level;
	public final int rewardExp;
	public final int rewardSp;
	public final int aggroRange;
	public final int rhand;
	public final int lhand;
	public final int armor;
	public final int enchantEffect;
	public final int absorbLevel;
	public final AbsorbCrystalType absorbType;
	public Race race;
	public final String jClass;
	public final boolean dropherb;
	public boolean isQuestMonster; // doesn't include all mobs that are involved in 
	// quests, just plain quest monsters for preventing champion spawn
	public final float baseVitalityDivider;
	
	//Skill AI
	public FastList<L2Skill> _buffskills;
	public FastList<L2Skill> _negativeskills;
	public FastList<L2Skill> _debuffskills;
	public FastList<L2Skill> _atkskills;
	public FastList<L2Skill> _rootskills;
	public FastList<L2Skill> _stunskills;
	public FastList<L2Skill> _sleepskills;
	public FastList<L2Skill> _paralyzeskills;
	public FastList<L2Skill> _fossilskills;
	public FastList<L2Skill> _floatskills;
	public FastList<L2Skill> _immobiliseskills;
	public FastList<L2Skill> _healskills;
	public FastList<L2Skill> _resskills;
	public FastList<L2Skill> _dotskills;
	public FastList<L2Skill> _cotskills;
	public FastList<L2Skill> _universalskills;
	public FastList<L2Skill> _manaskills;
	public FastList<L2Skill> _Lrangeskills;
	public FastList<L2Skill> _Srangeskills;
	public FastList<L2Skill> _generalskills;
	
	private boolean _hasbuffskills;
	private boolean _hasnegativeskills;
	private boolean _hasdebuffskills;
	private boolean _hasatkskills;
	private boolean _hasrootskills;
	private boolean _hasstunskills;
	private boolean _hassleepskills;
	private boolean _hasparalyzeskills;
	private boolean _hasfossilskills;
	private boolean _hasfloatskills;
	private boolean _hasimmobiliseskills;
	private boolean _hashealskills;
	private boolean _hasresskills;
	private boolean _hasdotskills;
	private boolean _hascotskills;
	private boolean _hasuniversalskills;
	private boolean _hasmanaskills;
	private boolean _hasLrangeskills;
	private boolean _hasSrangeskills;
	private boolean _hasgeneralskills;
	
	private L2NpcAIData _AIdataStatic = new L2NpcAIData();
	
	public static enum AbsorbCrystalType
	{
		LAST_HIT,
		FULL_PARTY,
		PARTY_ONE_RANDOM
	}
	
	public static enum AIType
	{
		FIGHTER,
		ARCHER,
		BALANCED,
		MAGE,
		HEALER,
		CORPSE
	}
	
	public static enum Race
	{
		UNDEAD,
		MAGICCREATURE,
		BEAST,
		ANIMAL,
		PLANT,
		HUMANOID,
		SPIRIT,
		ANGEL,
		DEMON,
		DRAGON,
		GIANT,
		BUG,
		FAIRIE,
		HUMAN,
		ELVE,
		DARKELVE,
		ORC,
		DWARVE,
		OTHER,
		NONLIVING,
		SIEGEWEAPON,
		DEFENDINGARMY,
		MERCENARIE,
		UNKNOWN,
		KAMAEL,
		NONE
	}
	
	//private final StatsSet _npcStatsSet;
	
	/** The table containing all Item that can be dropped by L2NpcInstance using this L2NpcTemplate*/
	private FastList<L2DropCategory> _categories = null;
	
	/** The table containing all Minions that must be spawn with the L2NpcInstance using this L2NpcTemplate*/
	private List<L2MinionData> _minions = null;
	
	private List<ClassId> _teachInfo;
	private Map<Integer, L2Skill> _skills;
	private Map<Stats, Double> _vulnerabilities;
	// contains a list of quests for each event type (questStart, questAttack, questKill, etc)
	private Map<Quest.QuestEventType, Quest[]> _questEvents;
	
	/**
	 * Constructor of L2Character.<BR><BR>
	 * 
	 * @param set The StatsSet object to transfer data to the method
	 * 
	 */
	public L2NpcTemplate(StatsSet set)
	{
		super(set);
		npcId = set.getInteger("npcId");
		idTemplate = set.getInteger("idTemplate");
		type = set.getString("type");
		name = set.getString("name");
		serverSideName = set.getBool("serverSideName");
		title = set.getString("title");
		if (title.equalsIgnoreCase("Quest Monster"))
			isQuestMonster = true;
		else
			isQuestMonster = false;
		serverSideTitle = set.getBool("serverSideTitle");
		sex = set.getString("sex");
		level = set.getByte("level");
		rewardExp = set.getInteger("rewardExp");
		rewardSp = set.getInteger("rewardSp");
		aggroRange = set.getInteger("aggroRange");
		rhand = set.getInteger("rhand");
		lhand = set.getInteger("lhand");
		armor = set.getInteger("armor");
		enchantEffect = set.getInteger("enchant"); 
		absorbLevel = set.getInteger("absorb_level", 0);
		absorbType = AbsorbCrystalType.valueOf(set.getString("absorb_type"));
		race = null;
		dropherb = set.getBool("drop_herbs", false);
		//_npcStatsSet = set;
		_teachInfo = null;
		jClass = set.getString("jClass");

		// all NPCs has 20 resistance to all attributes
		baseFireRes += 20;
		baseWindRes += 20;
		baseWaterRes += 20;
		baseEarthRes += 20;
		baseHolyRes += 20;
		baseDarkRes += 20;

		// can be loaded from db
		baseVitalityDivider = level > 0 && rewardExp > 0 ? baseHpMax * 9 * level * level /(100 * rewardExp) : 0;
	}
	
	public void addTeachInfo(ClassId classId)
	{
		if (_teachInfo == null)
			_teachInfo = new FastList<ClassId>();
		_teachInfo.add(classId);
	}
	
	public ClassId[] getTeachInfo()
	{
		if (_teachInfo == null)
			return null;
		return _teachInfo.toArray(new ClassId[_teachInfo.size()]);
	}
	
	public boolean canTeach(ClassId classId)
	{
		if (_teachInfo == null)
			return false;
		
		// If the player is on a third class, fetch the class teacher
		// information for its parent class.
		if (classId.level() == 3)
			return _teachInfo.contains(classId.getParent());
		
		return _teachInfo.contains(classId);
	}
	
	// add a drop to a given category.  If the category does not exist, create it.
	public void addDropData(L2DropData drop, int categoryType)
	{
		if (drop.isQuestDrop())
		{
			//			if (_questDrops == null)
			//				_questDrops = new FastList<L2DropData>(0);
			//			_questDrops.add(drop);
		}
		else
		{
			if (_categories == null)
				_categories = new FastList<L2DropCategory>();
			// if the category doesn't already exist, create it first
			synchronized (_categories)
			{
				boolean catExists = false;
				for (L2DropCategory cat : _categories)
					// if the category exists, add the drop to this category.
					if (cat.getCategoryType() == categoryType)
					{
						cat.addDropData(drop, type.equalsIgnoreCase("L2RaidBoss") || type.equalsIgnoreCase("L2GrandBoss"));
						catExists = true;
						break;
					}
				// if the category doesn't exit, create it and add the drop
				if (!catExists)
				{
					L2DropCategory cat = new L2DropCategory(categoryType);
					cat.addDropData(drop, type.equalsIgnoreCase("L2RaidBoss") || type.equalsIgnoreCase("L2GrandBoss"));
					_categories.add(cat);
				}
			}
		}
	}
	
	public void addRaidData(L2MinionData minion)
	{
		if (_minions == null)
			_minions = new FastList<L2MinionData>();
		_minions.add(minion);
	}
	
	public void addVulnerability(Stats id, double vuln)
	{
		if (_vulnerabilities == null)
			_vulnerabilities = new FastMap<Stats, Double>();
		_vulnerabilities.put(id, new Double(vuln));
	}
	
	public double getVulnerability(Stats id)
	{
		if (_vulnerabilities == null || _vulnerabilities.get(id) == null)
			return 1;
		return _vulnerabilities.get(id);
	}
	
	
	public void addSkill(L2Skill skill)
	{

		if (_skills == null)
			_skills = new FastMap<Integer, L2Skill>();
		
		if(!skill.isPassive())
		{
			addGeneralSkill(skill);
			switch(skill.getSkillType())
			{
				case BUFF:
				case REFLECT:
					addBuffSkill(skill);
					break;
				case HEAL:
				case HOT:
				case HEAL_PERCENT:
				case HEAL_STATIC:
				case BALANCE_LIFE:
					addHealSkill(skill);
					break;
				case RESURRECT:
					addResSkill(skill);
					break;
				case DEBUFF:
				case WEAKNESS:
					addDebuffSkill(skill);
					addCOTSkill(skill);
					addRangeSkill(skill);
					break;
				case ROOT:
					addRootSkill(skill);
					addImmobiliseSkill(skill);
					addRangeSkill(skill);
					break;
				case SLEEP:
					addSleepSkill(skill);
					addImmobiliseSkill(skill);
					break;
				case STUN:
					addRootSkill(skill);
					addImmobiliseSkill(skill);
					addRangeSkill(skill);
					break;
				case PARALYZE:
					addParalyzeSkill(skill);
					addImmobiliseSkill(skill);
					addRangeSkill(skill);
					break;
				case PDAM:
				case MDAM:
				case BLOW:
				case DRAIN:
				case CHARGEDAM:
				case DEATHLINK:
				case CPDAM:
				case MANADAM:
					addAtkSkill(skill);
					addUniversalSkill(skill);
					addRangeSkill(skill);
					break;
				case POISON:
				case DOT:
				case MDOT:
				case BLEED:
					addDOTSkill(skill);
					addRangeSkill(skill);
					break;
				case MUTE:
				case FEAR:
					addCOTSkill(skill);
					addRangeSkill(skill);
					break;
				case CANCEL:
				case NEGATE:
					addNegativeSkill(skill);
					addRangeSkill(skill);
					break;
				default :
					addUniversalSkill(skill);
					break;
		
			}
		}
		
		_skills.put(skill.getId(), skill);
	}
	
	
	public double removeVulnerability(Stats id)
	{
		return _vulnerabilities.remove(id);
	}
	
	/**
	 * Return the list of all possible UNCATEGORIZED drops of this L2NpcTemplate.<BR><BR>
	 */
	public FastList<L2DropCategory> getDropData()
	{
		return _categories;
	}
	
	/**
	 * Return the list of all possible item drops of this L2NpcTemplate.<BR>
	 * (ie full drops and part drops, mats, miscellaneous & UNCATEGORIZED)<BR><BR>
	 */
	public List<L2DropData> getAllDropData()
	{
		if (_categories == null)
			return null;
		List<L2DropData> lst = new FastList<L2DropData>();
		for (L2DropCategory tmp : _categories)
		{
			lst.addAll(tmp.getAllDrops());
		}
		return lst;
	}
	
	/**
	 * Empty all possible drops of this L2NpcTemplate.<BR><BR>
	 */
	public synchronized void clearAllDropData()
	{
		if (_categories == null)
			return;
		while (!_categories.isEmpty())
		{
			_categories.getFirst().clearAllDrops();
			_categories.removeFirst();
		}
		_categories.clear();
	}
	
	/**
	 * Return the list of all Minions that must be spawn with the L2NpcInstance using this L2NpcTemplate.<BR><BR>
	 */
	public List<L2MinionData> getMinionData()
	{
		return _minions;
	}
	
	public Map<Integer, L2Skill> getSkills()
	{
		return _skills;
	}
	
	public void addQuestEvent(Quest.QuestEventType EventType, Quest q)
	{
		if (_questEvents == null)
			_questEvents = new FastMap<Quest.QuestEventType, Quest[]>();
		
		if (_questEvents.get(EventType) == null)
		{
			_questEvents.put(EventType, new Quest[]
			{
				q
			});
		}
		else
		{
			Quest[] _quests = _questEvents.get(EventType);
			int len = _quests.length;
			
			// if only one registration per npc is allowed for this event type
			// then only register this NPC if not already registered for the specified event.
			// if a quest allows multiple registrations, then register regardless of count
			// In all cases, check if this new registration is replacing an older copy of the SAME quest
			// Finally, check quest class hierarchy: a parent class should never replace a child class.
			// a child class should always replace a parent class.
			if (!EventType.isMultipleRegistrationAllowed())
			{
				// if it is the same quest (i.e. reload) or the existing is a superclass of the new one, replace the existing.
				if (_quests[0].getName().equals(q.getName()) || L2NpcTemplate.isAssignableTo(q, _quests[0].getClass()))
				{
					_quests[0] = q;
				}
				else
				{
					_log.warning("Quest event not allowed in multiple quests.  Skipped addition of Event Type \"" + EventType + "\" for NPC \"" + name + "\" and quest \"" + q.getName() + "\".");
				}
			}
			else
			{
				// be ready to add a new quest to a new copy of the list, with larger size than previously.
				Quest[] tmp = new Quest[len + 1];
				
				// loop through the existing quests and copy them to the new list.  While doing so, also 
				// check if this new quest happens to be just a replacement for a previously loaded quest.  
				// Replace existing if the new quest is the same (reload) or a child of the existing quest.
				// Do nothing if the new quest is a superclass of an existing quest.
				// Add the new quest in the end of the list otherwise.
				for (int i = 0; i < len; i++)
				{
					if (_quests[i].getName().equals(q.getName()) || L2NpcTemplate.isAssignableTo(q, _quests[i].getClass()))
					{
						_quests[i] = q;
						return;
					}
					else if (L2NpcTemplate.isAssignableTo(_quests[i], q.getClass()))
					{
						return;
					}
					tmp[i] = _quests[i];
				}
				tmp[len] = q;
				_questEvents.put(EventType, tmp);
			}
		}
	}
	
	/**
	 * Checks if obj can be assigned to the Class represented by clazz.<br>
	 * This is true if, and only if, obj is the same class represented by clazz, 
	 * or a subclass of it or obj implements the interface represented by clazz. 
	 * 
	 * 
	 * @param obj
	 * @param clazz
	 * @return
	 */
	public static boolean isAssignableTo(Object obj, Class<?> clazz)
	{
		return L2NpcTemplate.isAssignableTo(obj.getClass(), clazz);
	}
	
	public static boolean isAssignableTo(Class<?> sub, Class<?> clazz)
	{
		// if clazz represents an interface
		if (clazz.isInterface())
		{
			// check if obj implements the clazz interface
			Class<?>[] interfaces = sub.getInterfaces();
			for (int i = 0; i < interfaces.length; i++)
			{
				if (clazz.getName().equals(interfaces[i].getName()))
				{
					return true;
				}
			}
		}
		else
		{
			do
			{
				if (sub.getName().equals(clazz.getName()))
				{
					return true;
				}
				
				sub = sub.getSuperclass();
			}
			while (sub != null);
		}
		
		return false;
	}
	
	public Quest[] getEventQuests(Quest.QuestEventType EventType)
	{
		if (_questEvents == null)
		{
			return null;
		}
		return _questEvents.get(EventType);
	}
	
	public void setRace(int raceId)
	{
		switch (raceId)
		{
			case 1:
				race = L2NpcTemplate.Race.UNDEAD;
				break;
			case 2:
				race = L2NpcTemplate.Race.MAGICCREATURE;
				break;
			case 3:
				race = L2NpcTemplate.Race.BEAST;
				break;
			case 4:
				race = L2NpcTemplate.Race.ANIMAL;
				break;
			case 5:
				race = L2NpcTemplate.Race.PLANT;
				break;
			case 6:
				race = L2NpcTemplate.Race.HUMANOID;
				break;
			case 7:
				race = L2NpcTemplate.Race.SPIRIT;
				break;
			case 8:
				race = L2NpcTemplate.Race.ANGEL;
				break;
			case 9:
				race = L2NpcTemplate.Race.DEMON;
				break;
			case 10:
				race = L2NpcTemplate.Race.DRAGON;
				break;
			case 11:
				race = L2NpcTemplate.Race.GIANT;
				break;
			case 12:
				race = L2NpcTemplate.Race.BUG;
				break;
			case 13:
				race = L2NpcTemplate.Race.FAIRIE;
				break;
			case 14:
				race = L2NpcTemplate.Race.HUMAN;
				break;
			case 15:
				race = L2NpcTemplate.Race.ELVE;
				break;
			case 16:
				race = L2NpcTemplate.Race.DARKELVE;
				break;
			case 17:
				race = L2NpcTemplate.Race.ORC;
				break;
			case 18:
				race = L2NpcTemplate.Race.DWARVE;
				break;
			case 19:
				race = L2NpcTemplate.Race.OTHER;
				break;
			case 20:
				race = L2NpcTemplate.Race.NONLIVING;
				break;
			case 21:
				race = L2NpcTemplate.Race.SIEGEWEAPON;
				break;
			case 22:
				race = L2NpcTemplate.Race.DEFENDINGARMY;
				break;
			case 23:
				race = L2NpcTemplate.Race.MERCENARIE;
				break;
			case 24:
				race = L2NpcTemplate.Race.UNKNOWN;
				break;
			case 25:
				race = L2NpcTemplate.Race.KAMAEL;
				break;
			default:
				race = L2NpcTemplate.Race.NONE;
				break;
		}
	}
	
	//-----------------------------------------------------------------------
	// Npc AI Data
	// By ShanSoft
	public void setAIData(L2NpcAIData aidata)
	{
		//_AIdataStatic = new L2NpcAIData(); // not needed to init object and in next line override with other reference. maybe other intention?
		_AIdataStatic = aidata;
	}
	//-----------------------------------------------------------------------

	public L2NpcAIData getAIDataStatic()
	{
		return _AIdataStatic;
	}
	
	public void addBuffSkill(L2Skill skill)
	{
		if (_buffskills == null)
			_buffskills = new FastList<L2Skill>();
		_buffskills.add(skill);
		_hasbuffskills=true;
	}
	
	public void addHealSkill(L2Skill skill)
	{
		if (_healskills == null)
			_healskills = new FastList<L2Skill>();
		_healskills.add(skill);
		_hashealskills=true;
	}
	
	public void addResSkill(L2Skill skill)
	{
		if (_resskills == null)
			_resskills = new FastList<L2Skill>();
		_resskills.add(skill);
		_hasresskills=true;
	}
	
	public void addAtkSkill(L2Skill skill)
	{
		if (_atkskills == null)
			_atkskills = new FastList<L2Skill>();
		_atkskills.add(skill);
		_hasatkskills=true;
	}

	public void addDebuffSkill(L2Skill skill)
	{
		if (_debuffskills == null)
			_debuffskills = new FastList<L2Skill>();
		_debuffskills.add(skill);
		_hasdebuffskills=true;
	}
	
	public void addRootSkill(L2Skill skill)
	{
		if (_rootskills == null)
			_rootskills = new FastList<L2Skill>();
		_rootskills.add(skill);
		_hasrootskills=true;
	}
	
	public void addSleepSkill(L2Skill skill)
	{
		if (_sleepskills == null)
			_sleepskills = new FastList<L2Skill>();
		_sleepskills.add(skill);
		_hassleepskills=true;
	}
	
	public void addStunSkill(L2Skill skill)
	{
		if (_stunskills == null)
			_stunskills = new FastList<L2Skill>();
		_stunskills.add(skill);
		_hasstunskills=true;
	}
	
	public void addParalyzeSkill(L2Skill skill)
	{
		if (_paralyzeskills == null)
			_paralyzeskills = new FastList<L2Skill>();
		_paralyzeskills.add(skill);
		_hasparalyzeskills=true;
	}
	
	public void addFloatSkill(L2Skill skill)
	{
		if (_floatskills == null)
			_floatskills = new FastList<L2Skill>();
		_floatskills.add(skill);
		_hasfloatskills=true;
	}
	
	public void addFossilSkill(L2Skill skill)
	{
		if (_fossilskills == null)
			_fossilskills = new FastList<L2Skill>();
		_fossilskills.add(skill);
		_hasfossilskills=true;
	}
	
	public void addNegativeSkill(L2Skill skill)
	{
		if (_negativeskills == null)
			_negativeskills = new FastList<L2Skill>();
		_negativeskills.add(skill);
		_hasnegativeskills=true;
	}
	
	public void addImmobiliseSkill(L2Skill skill)
	{
		if (_immobiliseskills == null)
			_immobiliseskills = new FastList<L2Skill>();
		_immobiliseskills.add(skill);
		_hasimmobiliseskills=true;
	}
	
	public void addDOTSkill(L2Skill skill)
	{
		if (_dotskills == null)
			_dotskills = new FastList<L2Skill>();
		_dotskills.add(skill);
		_hasdotskills=true;
	}
	
	public void addUniversalSkill(L2Skill skill)
	{
		if (_universalskills == null)
			_universalskills = new FastList<L2Skill>();
		_universalskills.add(skill);
		_hasuniversalskills=true;
	}
	
	public void addCOTSkill(L2Skill skill)
	{
		if (_cotskills == null)
			_cotskills = new FastList<L2Skill>();
		_cotskills.add(skill);
		_hascotskills=true;
	}
	
	public void addManaHealSkill(L2Skill skill)
	{
		if (_manaskills == null)
			_manaskills = new FastList<L2Skill>();
		_manaskills.add(skill);
		_hasmanaskills=true;
	}
	public void addGeneralSkill(L2Skill skill)
	{
		if (_generalskills == null)
			_generalskills = new FastList<L2Skill>();
		_generalskills.add(skill);
		_hasgeneralskills=true;
	}
	
	public void addRangeSkill(L2Skill skill)
	{
		if (skill.getCastRange() <= 150 && skill.getCastRange() > 0)
		{
			if (_Srangeskills == null)
				_Srangeskills = new FastList<L2Skill>();
			_Srangeskills.add(skill);
			_hasSrangeskills=true;
		}
		else if (skill.getCastRange() > 150)
		{
			if (_Lrangeskills == null)
				_Lrangeskills = new FastList<L2Skill>();
			_Lrangeskills.add(skill);
			_hasLrangeskills=true;
		}
	}
	
	//--------------------------------------------------------------------
	public boolean hasBuffSkill()
	{
		return _hasbuffskills;
	}
	public boolean hasHealSkill()
	{
		return _hashealskills;
	}
	
	public boolean hasResSkill()
	{
		return _hasresskills;
	}
	
	public boolean hasAtkSkill()
	{
		return _hasatkskills;
	}

	public boolean hasDebuffSkill()
	{
		return _hasdebuffskills;
	}
	
	public boolean hasRootSkill()
	{
		return _hasrootskills;
	}
	
	public boolean hasSleepSkill()
	{
		return _hassleepskills;
	}
	
	public boolean hasStunSkill()
	{
		return _hasstunskills;
	}
	
	public boolean hasParalyzeSkill()
	{
		return _hasparalyzeskills;
	}
	
	public boolean hasFloatSkill()
	{
		return _hasfloatskills;
	}
	
	public boolean hasFossilSkill()
	{
		return _hasfossilskills;
	}
	
	public boolean hasNegativeSkill()
	{
		return _hasnegativeskills;
	}
	
	public boolean hasImmobiliseSkill()
	{
		return _hasimmobiliseskills;
	}
	
	public boolean hasDOTSkill()
	{
		return _hasdotskills;
	}
	
	public boolean hasUniversalSkill()
	{
		return _hasuniversalskills;
	}
	
	public boolean hasCOTSkill()
	{
		return _hascotskills;
	}
	
	public boolean hasManaHealSkill()
	{
		return _hasmanaskills;
	}
	public boolean hasAutoLrangeSkill()
	{
		return _hasLrangeskills;
	}
	public boolean hasAutoSrangeSkill()
	{
		return _hasSrangeskills;
	}
	public boolean hasSkill()
	{
		return _hasgeneralskills;
	}
	
	public L2NpcTemplate.Race getRace()
	{
		if (race == null)
			race = L2NpcTemplate.Race.NONE;
		
		return race;
	}
	
	public boolean isCustom()
	{
		return npcId != idTemplate;
	}
	
	/**
	 * @return name
	 */
	public String getName()
	{
		return name;
	}
	
	public boolean isSpecialTree()
	{
		return npcId == L2XmassTreeInstance.SPECIAL_TREE_ID;
	}
}
