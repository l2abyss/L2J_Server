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
package net.sf.l2j.gameserver.datatables;

import java.io.File;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;

import javolution.util.FastList;
import javolution.util.FastMap;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.L2Augmentation;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.util.Rnd;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * This class manages the augmentation data and can also create new augmentations.
 *
 * @author  durgus 
 * edited by Gigiikun
 */
public class AugmentationData
{
	private static final Logger _log = Logger.getLogger(AugmentationData.class.getName());
	
	public static final AugmentationData getInstance()
	{
		return SingletonHolder._instance;
	}
	
	// =========================================================
	// Data Field
	
	// stats
	private static final int STAT_START = 1;
	private static final int STAT_END = 14560;
	private static final int STAT_BLOCKSIZE = 3640;
	//private static final int STAT_NUMBEROF_BLOCKS = 4;
	private static final int STAT_SUBBLOCKSIZE = 91;
	//private static final int STAT_NUMBEROF_SUBBLOCKS = 40;
	private static final int STAT_NUM = 13;
	
	private static final byte[] STATS1_MAP = new byte[STAT_SUBBLOCKSIZE];
	private static final byte[] STATS2_MAP = new byte[STAT_SUBBLOCKSIZE];

	// skills
	private static final int BLUE_START = 14561;
	// private static final int PURPLE_START = 14578;
	// private static final int RED_START = 14685;
	private static final int SKILLS_BLOCKSIZE = 178;
	
	// basestats
	private static final int BASESTAT_STR = 16341;
	private static final int BASESTAT_CON = 16342;
	private static final int BASESTAT_INT = 16343;
	private static final int BASESTAT_MEN = 16344;
	
	// accessory
	private static final int ACC_START = 16669;
	private static final int ACC_BLOCKS_NUM = 10;
	private static final int ACC_STAT_SUBBLOCKSIZE = 21;
	private static final int ACC_STAT_NUM = 6; 

	private static final int ACC_RING_START = ACC_START;
	private static final int ACC_RING_SKILLS = 18;
	private static final int ACC_RING_BLOCKSIZE = ACC_RING_SKILLS + 4 * ACC_STAT_SUBBLOCKSIZE;
	private static final int ACC_RING_END = ACC_RING_START + ACC_BLOCKS_NUM * ACC_RING_BLOCKSIZE - 1;

	private static final int ACC_EAR_START = ACC_RING_END + 1;
	private static final int ACC_EAR_SKILLS = 18;
	private static final int ACC_EAR_BLOCKSIZE = ACC_EAR_SKILLS + 4 * ACC_STAT_SUBBLOCKSIZE;
	private static final int ACC_EAR_END = ACC_EAR_START + ACC_BLOCKS_NUM * ACC_EAR_BLOCKSIZE - 1;

	private static final int ACC_NECK_START = ACC_EAR_END + 1;
	private static final int ACC_NECK_SKILLS = 24;
	private static final int ACC_NECK_BLOCKSIZE = ACC_NECK_SKILLS + 4 * ACC_STAT_SUBBLOCKSIZE;

	private static final int ACC_END = ACC_NECK_START + ACC_BLOCKS_NUM * ACC_NECK_BLOCKSIZE;

	private static final byte[] ACC_STATS1_MAP = new byte[ACC_STAT_SUBBLOCKSIZE];
	private static final byte[] ACC_STATS2_MAP = new byte[ACC_STAT_SUBBLOCKSIZE];
	
	private FastList<?> _augmentationStats[];
	private Map<Integer, FastList<augmentationSkill>> _blueSkills;
	private Map<Integer, FastList<augmentationSkill>> _purpleSkills;
	private Map<Integer, FastList<augmentationSkill>> _redSkills;

	private FastList<?> _augmentationAccStats[];

	// =========================================================
	// Constructor
	private AugmentationData()
	{
		_log.info("Initializing AugmentationData.");
		
		_augmentationStats = new FastList[4];
		_augmentationStats[0] = new FastList<augmentationStat>();
		_augmentationStats[1] = new FastList<augmentationStat>();
		_augmentationStats[2] = new FastList<augmentationStat>();
		_augmentationStats[3] = new FastList<augmentationStat>();
		
		_augmentationAccStats = new FastList[4];
		_augmentationAccStats[0] = new FastList<augmentationStat>();
		_augmentationAccStats[1] = new FastList<augmentationStat>();
		_augmentationAccStats[2] = new FastList<augmentationStat>();
		_augmentationAccStats[3] = new FastList<augmentationStat>();

		// Lookup tables structure: STAT1 represent first stat, STAT2 - second.
		// If both values are the same - use solo stat, if different - combined.
		byte idx;
		// weapon augmentation block: solo values first
		// 00-00, 01-01 ... 11-11,12-12
		for (idx = 0; idx < STAT_NUM; idx++)
		{
			// solo stats
			STATS1_MAP[idx] = idx;
			STATS2_MAP[idx] = idx;
		}
		// combined values next.
		// 00-01,00-02,00-03 ... 00-11,00-12;
		// 01-02,01-03 ... 01-11,01-12;
		// ...
		// 09-10,09-11,09-12;
		// 10-11,10-12;
		// 11-12
		for (int i = 0; i < STAT_NUM; i++)
		{
			for (int j = i + 1; j < STAT_NUM; idx++, j++)
			{
				// combined stats
				STATS1_MAP[idx] = (byte)i;
				STATS2_MAP[idx] = (byte)j;
			}
		}
		idx = 0;
		// accessory augmentation block, structure is different:
		// 00-00,00-01,00-02,00-03,00-04,00-05
		// 01-01,01-02,01-03,01-04,01-05
		// 02-02,02-03,02-04,02-05
		// 03-03,03-04,03-05
		// 04-04,04-05
		// 05-05
		// First values always solo, next are combined
		for (int i = 0; i < ACC_STAT_NUM; i++)
		{
			for (int j = i; j < ACC_STAT_NUM; idx++, j++)
			{
				ACC_STATS1_MAP[idx] = (byte)i;
				ACC_STATS2_MAP[idx] = (byte)j;				
			}
		}

		_blueSkills = new FastMap<Integer, FastList<augmentationSkill>>();
		_purpleSkills = new FastMap<Integer, FastList<augmentationSkill>>();
		_redSkills = new FastMap<Integer, FastList<augmentationSkill>>();
		for (int i = 1; i <= 10; i++)
		{
			_blueSkills.put(i, new FastList<augmentationSkill>());
			_purpleSkills.put(i, new FastList<augmentationSkill>());
			_redSkills.put(i, new FastList<augmentationSkill>());
		}
		
		load();
		
		// Use size*4: since theres 4 blocks of stat-data with equivalent size
		_log.info("AugmentationData: Loaded: " + (_augmentationStats[0].size() * 4) + " augmentation stats.");
		_log.info("AugmentationData: Loaded: " + (_augmentationAccStats[0].size() * 4) + " accessory augmentation stats.");
		for (int i = 1; i <= 10; i++)
		{
			_log.info("AugmentationData: Loaded: " + _blueSkills.get(i).size() + " blue, " + _purpleSkills.get(i).size() + " purple and "
					+ _redSkills.get(i).size() + " red skills for lifeStoneLevel " + i);
		}
	}
	
	// =========================================================
	// Nested Class
	
	public class augmentationSkill
	{
		private int _skillId;
		private int _skillLevel;
		private int _augmentationSkillId;
		
		public augmentationSkill(int skillId, int skillLevel, int augmentationSkillId)
		{
			_skillId = skillId;
			_skillLevel = skillLevel;
			_augmentationSkillId = augmentationSkillId;
		}
		
		public L2Skill getSkill()
		{
			return SkillTable.getInstance().getInfo(_skillId, _skillLevel);
		}
		
		public int getAugmentationSkillId()
		{
			return _augmentationSkillId;
		}
	}
	
	public class augmentationStat
	{
		private final Stats _stat;
		private final int _singleSize;
		private final int _combinedSize;
		private final float _singleValues[];
		private final float _combinedValues[];
		
		public augmentationStat(Stats stat, float sValues[], float cValues[])
		{
			_stat = stat;
			_singleSize = sValues.length;
			_singleValues = sValues;
			_combinedSize = cValues.length;
			_combinedValues = cValues;
		}
		
		public int getSingleStatSize()
		{
			return _singleSize;
		}
		
		public int getCombinedStatSize()
		{
			return _combinedSize;
		}
		
		public float getSingleStatValue(int i)
		{
			if (i >= _singleSize || i < 0)
				return _singleValues[_singleSize - 1];
			return _singleValues[i];
		}
		
		public float getCombinedStatValue(int i)
		{
			if (i >= _combinedSize || i < 0)
				return _combinedValues[_combinedSize - 1];
			return _combinedValues[i];
		}
		
		public Stats getStat()
		{
			return _stat;
		}
	}
	
	// =========================================================
	// Method - Private
	
	@SuppressWarnings("unchecked")
	private final void load()
	{
		// Load the skillmap
		// Note: the skillmap data is only used when generating new augmentations
		// the client expects a different id in order to display the skill in the
		// items description...
		try
		{
			int badAugmantData = 0;
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			
			File file = new File(Config.DATAPACK_ROOT + "/data/stats/augmentation/augmentation_skillmap.xml");
			if (!file.exists())
			{
				if (Config.DEBUG)
					_log.info("The augmentation skillmap file is missing.");
				return;
			}
			
			Document doc = factory.newDocumentBuilder().parse(file);
			
			for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
			{
				if ("list".equalsIgnoreCase(n.getNodeName()))
				{
					for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					{
						if ("augmentation".equalsIgnoreCase(d.getNodeName()))
						{
							NamedNodeMap attrs = d.getAttributes();
							int skillId = 0, augmentationId = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
							int skillLvL = 0;
							String type = "blue";
							
							for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
							{
								if ("skillId".equalsIgnoreCase(cd.getNodeName()))
								{
									attrs = cd.getAttributes();
									skillId = Integer.parseInt(attrs.getNamedItem("val").getNodeValue());
								}
								else if ("skillLevel".equalsIgnoreCase(cd.getNodeName()))
								{
									attrs = cd.getAttributes();
									skillLvL = Integer.parseInt(attrs.getNamedItem("val").getNodeValue());
								}
								else if ("type".equalsIgnoreCase(cd.getNodeName()))
								{
									attrs = cd.getAttributes();
									type = attrs.getNamedItem("val").getNodeValue();
								}
							}
							if (skillId == 0)
							{
								if (Config.DEBUG)
									_log.log(Level.SEVERE, "Bad skillId in augmentation_skillmap.xml in the augmentationId:"
											+ augmentationId);
								badAugmantData++;
								continue;
							}
							else if (skillLvL == 0)
							{
								if (Config.DEBUG)
									_log.log(Level.SEVERE, "Bad skillLevel in augmentation_skillmap.xml in the augmentationId:"
											+ augmentationId);
								badAugmantData++;
								continue;
							}
							int k = 1;
							while ((augmentationId - k * SKILLS_BLOCKSIZE) >= BLUE_START)
								k++;
							
							if (type.equalsIgnoreCase("blue"))
								_blueSkills.get(k).add(new augmentationSkill(skillId, skillLvL, augmentationId));
							else if (type.equalsIgnoreCase("purple"))
								_purpleSkills.get(k).add(new augmentationSkill(skillId, skillLvL, augmentationId));
							else
								_redSkills.get(k).add(new augmentationSkill(skillId, skillLvL, augmentationId));
						}
					}
				}
			}
			if (badAugmantData != 0)
				_log.info("AugmentationData: " + badAugmantData + " bad skill(s) were skipped.");
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Error parsing augmentation_skillmap.xml.", e);
			return;
		}
		
		// Load the stats from xml
		for (int i = 1; i < 5; i++)
		{
			try
			{
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				factory.setValidating(false);
				factory.setIgnoringComments(true);
				
				File file = new File(Config.DATAPACK_ROOT + "/data/stats/augmentation/augmentation_stats" + i + ".xml");
				if (!file.exists())
				{
					if (Config.DEBUG)
						_log.info("The augmentation stat data file " + i + " is missing.");
					return;
				}
				
				Document doc = factory.newDocumentBuilder().parse(file);
				
				for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
				{
					if ("list".equalsIgnoreCase(n.getNodeName()))
					{
						for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
						{
							if ("stat".equalsIgnoreCase(d.getNodeName()))
							{
								NamedNodeMap attrs = d.getAttributes();
								String statName = attrs.getNamedItem("name").getNodeValue();
								float soloValues[] = null, combinedValues[] = null;
								
								for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
								{
									if ("table".equalsIgnoreCase(cd.getNodeName()))
									{
										attrs = cd.getAttributes();
										String tableName = attrs.getNamedItem("name").getNodeValue();
										
										StringTokenizer data = new StringTokenizer(cd.getFirstChild().getNodeValue());
										FastList<Float> array = new FastList<Float>();
										while (data.hasMoreTokens())
											array.add(Float.parseFloat(data.nextToken()));
										
										if (tableName.equalsIgnoreCase("#soloValues"))
										{
											soloValues = new float[array.size()];
											int x = 0;
											for (float value : array)
												soloValues[x++] = value;
										}
										else
										{
											combinedValues = new float[array.size()];
											int x = 0;
											for (float value : array)
												combinedValues[x++] = value;
										}
									}
								}
								// store this stat
								((FastList<augmentationStat>) _augmentationStats[(i - 1)]).add(new augmentationStat(Stats.valueOfXml(statName), soloValues, combinedValues));
							}
						}
					}
				}
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "Error parsing augmentation_stats" + i + ".xml.", e);
				return;
			}

			try
			{
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				factory.setValidating(false);
				factory.setIgnoringComments(true);
				
				File file = new File(Config.DATAPACK_ROOT + "/data/stats/augmentation/augmentation_jewel_stats" + i + ".xml");
				
				if (!file.exists())
				{
					if (Config.DEBUG)
						_log.info("The jewel augmentation stat data file " + i + " is missing.");
					return;
				}

				Document doc = factory.newDocumentBuilder().parse(file);

				for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
				{
					if ("list".equalsIgnoreCase(n.getNodeName()))
					{
						for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
						{
							if ("stat".equalsIgnoreCase(d.getNodeName()))
							{
								NamedNodeMap attrs = d.getAttributes();
								String statName = attrs.getNamedItem("name").getNodeValue();
								float soloValues[] = null, combinedValues[] = null;
								
								for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
								{
									if ("table".equalsIgnoreCase(cd.getNodeName()))
									{
										attrs = cd.getAttributes();
										String tableName = attrs.getNamedItem("name").getNodeValue();
										
										StringTokenizer data = new StringTokenizer(cd.getFirstChild().getNodeValue());
										FastList<Float> array = new FastList<Float>();
										while (data.hasMoreTokens())
											array.add(Float.parseFloat(data.nextToken()));
										
										if (tableName.equalsIgnoreCase("#soloValues"))
										{
											soloValues = new float[array.size()];
											int x = 0;
											for (float value : array)
												soloValues[x++] = value;
										}
										else
										{
											combinedValues = new float[array.size()];
											int x = 0;
											for (float value : array)
												combinedValues[x++] = value;
										}
									}
								}
								// store this stat
								((FastList<augmentationStat>) _augmentationAccStats[(i - 1)]).add(new augmentationStat(Stats.valueOfXml(statName), soloValues, combinedValues));
							}
						}
					}
				}
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "Error parsing jewel augmentation_stats" + i + ".xml.", e);
				return;
			}
		}
	}
	
	// =========================================================
	// Properties - Public

	/**
	 * Generate a new random augmentation
	 * @param item
	 * @param lifeStoneLevel
	 * @param lifeSoneGrade
	 * @return L2Augmentation
	 */
	public L2Augmentation generateRandomAugmentation(int lifeStoneLevel, int lifeStoneGrade)
	{
		// Note that stat12 stands for stat 1 AND 2 (same for stat34 ;p )
		// this is because a value can contain up to 2 stat modifications
		// (there are two short values packed in one integer value, meaning 4 stat modifications at max)
		// for more info take a look at getAugStatsById(...)
		
		// Note: lifeStoneGrade: (0 means low grade, 3 top grade)
		// First: determine whether we will add a skill/baseStatModifier or not
		// because this determine which color could be the result 
		int skill_Chance = 0;
		int stat34 = 0;
		boolean generateSkill = false;
		boolean generateGlow = false;
		int resultColor = 0;
		//lifestonelevel is used for stat Id and skill level, but here the max level is 10
		if (lifeStoneLevel > 10)
			lifeStoneLevel = 10;
		switch (lifeStoneGrade)
		{
			case 0:
				skill_Chance = Config.AUGMENTATION_NG_SKILL_CHANCE;
				if (Rnd.get(1, 100) <= Config.AUGMENTATION_NG_GLOW_CHANCE)
					generateGlow = true;
				break;
			case 1:
				skill_Chance = Config.AUGMENTATION_MID_SKILL_CHANCE;
				if (Rnd.get(1, 100) <= Config.AUGMENTATION_MID_GLOW_CHANCE)
					generateGlow = true;
				break;
			case 2:
				skill_Chance = Config.AUGMENTATION_HIGH_SKILL_CHANCE;
				if (Rnd.get(1, 100) <= Config.AUGMENTATION_HIGH_GLOW_CHANCE)
					generateGlow = true;
				break;
			case 3:
				skill_Chance = Config.AUGMENTATION_TOP_SKILL_CHANCE;
				if (Rnd.get(1, 100) <= Config.AUGMENTATION_TOP_GLOW_CHANCE)
					generateGlow = true;
		}
		
		if (Rnd.get(1, 100) <= skill_Chance)
			generateSkill = true;
		else if (Rnd.get(1, 100) <= Config.AUGMENTATION_BASESTAT_CHANCE)
			stat34 = Rnd.get(BASESTAT_STR, BASESTAT_MEN);
		
		// Second: decide which grade the augmentation result is going to have:
		// 0:yellow, 1:blue, 2:purple, 3:red
		// The chances used here are most likely custom,
		// whats known is: you cant have yellow with skill(or baseStatModifier)
		// noGrade stone can not have glow, mid only with skill, high has a chance(custom), top allways glow
		if (stat34 == 0 && !generateSkill)
		{
			resultColor = Rnd.get(0, 100);
			if (resultColor <= (15 * lifeStoneGrade) + 40)
				resultColor = 1;
			else
				resultColor = 0;
		}
		else
		{
			resultColor = Rnd.get(0, 100);
			if (resultColor <= (10 * lifeStoneGrade) + 5 || stat34 != 0)
				resultColor = 3;
			else if (resultColor <= (10 * lifeStoneGrade) + 10)
				resultColor = 1;
			else
				resultColor = 2;
		}
		
		// Third: Calculate the subblock offset for the choosen color,
		// and the level of the lifeStone
		// from large number of retail augmentations:
		// no skill part
		// Id for stat12:
		// A:1-910 B:911-1820 C:1821-2730 D:2731-3640 E:3641-4550 F:4551-5460 G:5461-6370 H:6371-7280
		// Id for stat34(this defines the color):
		// I:7281-8190(yellow) K:8191-9100(blue) L:10921-11830(yellow) M:11831-12740(blue)
		// you can combine I-K with A-D and L-M with E-H
		// using C-D or G-H Id you will get a glow effect
		// there seems no correlation in which grade use which Id except for the glowing restriction
		// skill part
		// Id for stat12:
		// same for no skill part
		// A same as E, B same as F, C same as G, D same as H
		// A - no glow, no grade LS
		// B - weak glow, mid grade LS?
		// C - glow, high grade LS?
		// D - strong glow, top grade LS?
		
		// is neither a skill nor basestat used for stat34? then generate a normal stat
		int stat12 = 0;
		if (stat34 == 0 && !generateSkill)
		{
			int temp = Rnd.get(2, 3);
			int colorOffset = resultColor * (10 * STAT_SUBBLOCKSIZE) + temp * STAT_BLOCKSIZE + 1;
			int offset = ((lifeStoneLevel - 1) * STAT_SUBBLOCKSIZE) + colorOffset;
			
			stat34 = Rnd.get(offset, offset + STAT_SUBBLOCKSIZE - 1);
			if (generateGlow && lifeStoneGrade >= 2)
				offset = ((lifeStoneLevel - 1) * STAT_SUBBLOCKSIZE) + (temp - 2) * STAT_BLOCKSIZE + lifeStoneGrade
						* (10 * STAT_SUBBLOCKSIZE) + 1;
			else
				offset = ((lifeStoneLevel - 1) * STAT_SUBBLOCKSIZE) + (temp - 2) * STAT_BLOCKSIZE + Rnd.get(0, 1)
						* (10 * STAT_SUBBLOCKSIZE) + 1;
			stat12 = Rnd.get(offset, offset + STAT_SUBBLOCKSIZE - 1);
		}
		else
		{
			int offset;
			if (!generateGlow)
				offset = ((lifeStoneLevel - 1) * STAT_SUBBLOCKSIZE) + Rnd.get(0, 1) * STAT_BLOCKSIZE + 1;
			else
				offset = ((lifeStoneLevel - 1) * STAT_SUBBLOCKSIZE) + Rnd.get(0, 1) * STAT_BLOCKSIZE + (lifeStoneGrade + resultColor) / 2
						* (10 * STAT_SUBBLOCKSIZE) + 1;
			stat12 = Rnd.get(offset, offset + STAT_SUBBLOCKSIZE - 1);
		}
		
		// generate a skill if neccessary
		L2Skill skill = null;
		if (generateSkill)
		{
			augmentationSkill temp = null;
			switch (resultColor)
			{
				case 1: // blue skill
					temp = _blueSkills.get(lifeStoneLevel).get(Rnd.get(0, _blueSkills.get(lifeStoneLevel).size() - 1));
					skill = temp.getSkill();
					stat34 = temp.getAugmentationSkillId();
					break;
				case 2: // purple skill
					temp = _purpleSkills.get(lifeStoneLevel).get(Rnd.get(0, _purpleSkills.get(lifeStoneLevel).size() - 1));
					skill = temp.getSkill();
					stat34 = temp.getAugmentationSkillId();
					break;
				case 3: // red skill
					temp = _redSkills.get(lifeStoneLevel).get(Rnd.get(0, _redSkills.get(lifeStoneLevel).size() - 1));
					skill = temp.getSkill();
					stat34 = temp.getAugmentationSkillId();
					break;
			}
		}
		
		if (Config.DEBUG)
			_log.info("Augmentation success: stat12=" + stat12 + "; stat34=" + stat34 + "; resultColor=" + resultColor + "; level="
					+ lifeStoneLevel + "; grade=" + lifeStoneGrade);
		return new L2Augmentation(((stat34 << 16) + stat12), skill);
	}
	
	public class AugStat
	{
		private final Stats _stat;
		private final float _value;
		
		public AugStat(Stats stat, float value)
		{
			_stat = stat;
			_value = value;
		}
		
		public Stats getStat()
		{
			return _stat;
		}
		
		public float getValue()
		{
			return _value;
		}
	}
	
	/**
	 * Returns the stat and basestat boni for a given augmentation id
	 * @param augmentationId
	 * @return
	 */
	public FastList<AugStat> getAugStatsById(int augmentationId)
	{
		FastList<AugStat> temp = new FastList<AugStat>();
		// An augmentation id contains 2 short vaues so we gotta seperate them here
		// both values contain a number from 1-16380, the first 14560 values are stats
		// the 14560 stats are divided into 4 blocks each holding 3640 values
		// each block contains 40 subblocks holding 91 stat values
		// the first 13 values are so called Solo-stats and they have the highest stat increase possible
		// after the 13 Solo-stats come 78 combined stats (thats every possible combination of the 13 solo stats)
		// the first 12 combined stats (14-26) is the stat 1 combined with stat 2-13
		// the next 11 combined stats then are stat 2 combined with stat 3-13 and so on...
		// to get the idea have a look @ optiondata_client-e.dat - thats where the data came from :)
		int stats[] = new int[2];
		stats[0] = 0x0000FFFF & augmentationId;
		stats[1] = (augmentationId >> 16);
		
		for (int i = 0; i < 2; i++)
		{
			// weapon augmentation - stats
			if (stats[i] >= STAT_START && stats[i] <= STAT_END)
			{
				int color = stats[i] / STAT_BLOCKSIZE; // 4 color blocks
				int subblock = stats[i] % STAT_BLOCKSIZE; // offset in color block
				int level = subblock / STAT_SUBBLOCKSIZE; // stat level (sub-block number)
				int stat = subblock % STAT_SUBBLOCKSIZE; // offset in sub-block - stat

				byte stat1 = STATS1_MAP[stat];
				byte stat2 = STATS2_MAP[stat];
				if (stat1 == stat2) // solo stat
				{
					augmentationStat as = ((augmentationStat) _augmentationStats[color].get(stat1));
					temp.add(new AugStat(as.getStat(), as.getSingleStatValue(level)));
				}
				else // combined stat
				{
					augmentationStat as = ((augmentationStat) _augmentationStats[color].get(stat1));					
					temp.add(new AugStat(as.getStat(), as.getCombinedStatValue(level)));
					as = ((augmentationStat) _augmentationStats[color].get(stat2));
					temp.add(new AugStat(as.getStat(), as.getCombinedStatValue(level)));
				}
			}
			// its a base stat
			else if (stats[i] >= BASESTAT_STR && stats[i] <= BASESTAT_MEN)
			{
				switch (stats[i])
				{
					case BASESTAT_STR:
						temp.add(new AugStat(Stats.STAT_STR, 1.0f));
						break;
					case BASESTAT_CON:
						temp.add(new AugStat(Stats.STAT_CON, 1.0f));
						break;
					case BASESTAT_INT:
						temp.add(new AugStat(Stats.STAT_INT, 1.0f));
						break;
					case BASESTAT_MEN:
						temp.add(new AugStat(Stats.STAT_MEN, 1.0f));
						break;
				}
			}
			// accessory augmentation
			// 3 areas for rings, earrings and necklaces
			// each area consist of 10 blocks (level)
			// each block has skills first (18 or 24 for necklaces)
			// and sub-block for stats next 
			else if (stats[i] >= ACC_START && stats[i] <= ACC_END)
			{
				int base, level, subblock;

				if (stats[i] <= ACC_RING_END) // rings area
				{
					base = stats[i] - ACC_RING_START; // calculate base offset
					level = base / ACC_RING_BLOCKSIZE; // stat level (block number)
					subblock = (base % ACC_RING_BLOCKSIZE) - ACC_RING_SKILLS; // skills first
				}
				else if (stats[i] <= ACC_EAR_END) //earrings area
				{
					base = stats[i] - ACC_EAR_START;
					level = base / ACC_EAR_BLOCKSIZE;
					subblock = (base % ACC_EAR_BLOCKSIZE) - ACC_EAR_SKILLS;
				}
				else // necklaces
				{
					base = stats[i] - ACC_NECK_START;
					level = base / ACC_NECK_BLOCKSIZE;
					subblock = (base % ACC_NECK_BLOCKSIZE) - ACC_NECK_SKILLS;
				}

				if (subblock >= 0) // stat, not skill
				{
					int color = subblock / ACC_STAT_SUBBLOCKSIZE;
					int stat = subblock % ACC_STAT_SUBBLOCKSIZE;
					byte stat1 = ACC_STATS1_MAP[stat];
					byte stat2 = ACC_STATS2_MAP[stat];
					if (stat1 == stat2) // solo
					{
						augmentationStat as = ((augmentationStat) _augmentationAccStats[color].get(stat1));					
						temp.add(new AugStat(as.getStat(), as.getSingleStatValue(level)));
					}
					else // combined
					{
						augmentationStat as = ((augmentationStat) _augmentationAccStats[color].get(stat1));					
						temp.add(new AugStat(as.getStat(), as.getCombinedStatValue(level)));
						as = ((augmentationStat) _augmentationAccStats[color].get(stat2));
						temp.add(new AugStat(as.getStat(), as.getCombinedStatValue(level)));
					}
				}
			}
		}
		
		return temp;
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final AugmentationData _instance = new AugmentationData();
	}
}
