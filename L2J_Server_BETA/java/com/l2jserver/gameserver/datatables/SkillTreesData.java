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
package com.l2jserver.gameserver.datatables;

import gnu.trove.TIntObjectHashMap;

import java.io.File;
import java.util.Arrays;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.l2jserver.Config;
import com.l2jserver.gameserver.model.L2Clan;
import com.l2jserver.gameserver.model.L2Skill;
import com.l2jserver.gameserver.model.L2SkillLearn;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.base.ClassId;
import com.l2jserver.gameserver.model.base.Race;
import com.l2jserver.gameserver.model.base.SubClass;
import com.l2jserver.gameserver.templates.StatsSet;
import com.l2jserver.gameserver.util.Util;
import com.l2jserver.util.file.filter.XMLFilter;

/**
 * @author Zoey76
 */
public final class SkillTreesData
{
	private static Logger _log = Logger.getLogger(SkillTreesData.class.getSimpleName());
	
	//ClassId, FastMap of Skill Hash Code, L2LearkSkill
	private FastMap<ClassId, FastMap<Integer, L2SkillLearn>> _classSkillTrees;
	private FastMap<ClassId, FastMap<Integer, L2SkillLearn>> _transferSkillTrees;
	//Skill Hash Code, L2LearkSkill
	private FastMap<Integer, L2SkillLearn> _collectSkillTree;
	private FastMap<Integer, L2SkillLearn> _fishingSkillTree;
	private FastMap<Integer, L2SkillLearn> _pledgeSkillTree;
	private FastMap<Integer, L2SkillLearn> _subClassSkillTree;
	private FastMap<Integer, L2SkillLearn> _subPledgeSkillTree;
	private FastMap<Integer, L2SkillLearn> _transformSkillTree;
	
	//TODO: Unhardcode?
	//Checker, sorted arrays of hash codes
	private TIntObjectHashMap<int[]> _skillsByClassIdHashCodes; //Occupation skills
	private TIntObjectHashMap<int[]> _skillsByRaceHashCodes; // race-specific transformations
	private int[] _allSkillsHashCodes; // fishing, collection and all races transformations
	
	private boolean _loading = true;
	
	/**
	 * Parent class IDs are read from XML and stored in this map, to allow easy customization.
	 */
	private final FastMap<ClassId, ClassId> _parentClassMap = new FastMap<ClassId, ClassId>();
	
	private SkillTreesData()
	{
		load();
	}
	
	public void load()
	{
		_loading = true;
		_classSkillTrees = new FastMap<ClassId, FastMap<Integer, L2SkillLearn>>();
		_collectSkillTree = new FastMap<Integer, L2SkillLearn>();
		_fishingSkillTree = new FastMap<Integer, L2SkillLearn>();
		_pledgeSkillTree = new FastMap<Integer, L2SkillLearn>();
		_subClassSkillTree = new FastMap<Integer, L2SkillLearn>();
		_subPledgeSkillTree = new FastMap<Integer, L2SkillLearn>();
		_transferSkillTrees = new FastMap<ClassId, FastMap<Integer, L2SkillLearn>>();
		_transformSkillTree = new FastMap<Integer, L2SkillLearn>();
		
		//Load files.
		_loading = loadFiles();
		
		int classSkillTreeCount = 0;
		for (ClassId classId : _classSkillTrees.keySet())
		{
			classSkillTreeCount += _classSkillTrees.get(classId).size();
		}
		
		int trasferSkillTreeCount = 0;
		for (ClassId classId : _transferSkillTrees.keySet())
		{
			trasferSkillTreeCount += _transferSkillTrees.get(classId).size();
		}
		
		int fishingDwarvenSkillCount = 0;
		for (L2SkillLearn fishSkill : _fishingSkillTree.values())
		{
			if ((fishSkill.getRaces() != null) && Util.contains(fishSkill.getRaces(), 4))
			{
				fishingDwarvenSkillCount++;
			}
		}
		
		int residentialSkillCount = 0;
		for (L2SkillLearn pledgeSkill : _pledgeSkillTree.values())
		{
			if (pledgeSkill.isResidencialSkill())
			{
				residentialSkillCount++;
			}
		}
		
		_log.info(getClass().getSimpleName() + ": Loaded " + classSkillTreeCount + "  Class Skills for " + _classSkillTrees.size() + " Class Skill Trees.");
		_log.info(getClass().getSimpleName() + ": Loaded " + _subClassSkillTree.size() + " Sub-Class Skills.");
		_log.info(getClass().getSimpleName() + ": Loaded " + trasferSkillTreeCount + " Transfer Skills for " + _transferSkillTrees.size() + " Transfer Skill Trees.");
		_log.info(getClass().getSimpleName() + ": Loaded " + _fishingSkillTree.size() + " Fishing Skills, " + fishingDwarvenSkillCount + " Dwarven only Fishing Skills.");
		_log.info(getClass().getSimpleName() + ": Loaded " + _collectSkillTree.size() + " Collect Skills.");
		_log.info(getClass().getSimpleName() + ": Loaded " + _pledgeSkillTree.size() + " Pledge Skills, " + (_pledgeSkillTree.size() - residentialSkillCount) + " for Pledge and " + residentialSkillCount + " Residential.");
		_log.info(getClass().getSimpleName() + ": Loaded " + _subPledgeSkillTree.size() + " Sub-Pledge Skills.");
		_log.info(getClass().getSimpleName() + ": Loaded " + _transformSkillTree.size() + " Transform Skills.");
	}
	
	/**
	 * Loads all files type xml from data/skillTrees/ and call the parser for each one of them.
	 * @return {@code false} when the files are loaded.
	 */
	private boolean loadFiles()
	{
		File folder = new File(Config.DATAPACK_ROOT, "data/skillTrees/");
		File[] listOfFiles = folder.listFiles(new XMLFilter());
		for (File f : listOfFiles)
		{
			loadSkillTree(f);
		}
		return false;
	}
	
	/**
	 * Parse a skill tree file and store it into the correct skill tree.
	 * @param file the xml file to be parsed.
	 */
	private void loadSkillTree(File file)
	{
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setValidating(false);
		dbf.setIgnoringComments(true);
		Document doc = null;
		
		if (file.exists())
		{
			try
			{
				doc = dbf.newDocumentBuilder().parse(file);
			}
			catch (Exception e)
			{
				_log.warning(getClass().getSimpleName() + ": Could not parse " + file.getName() + " file: " + e.getMessage());
			}
			
			NamedNodeMap attributes;
			Node attribute;
			String type = null;
			int cId = -1;
			int parentClassId = -1;
			ClassId classId = null;
			
			for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
			{
				if ("list".equalsIgnoreCase(n.getNodeName()))
				{
					for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					{
						FastMap<Integer, L2SkillLearn> classSkillTree = new FastMap<Integer, L2SkillLearn>();
						FastMap<Integer, L2SkillLearn> trasferSkillTree = new FastMap<Integer, L2SkillLearn>();
						if ("skillTree".equalsIgnoreCase(d.getNodeName()))
						{
							attribute = d.getAttributes().getNamedItem("type");
							if (attribute == null)
							{
								_log.warning(getClass().getSimpleName() + ": Skill Tree without type!");
								continue;
							}
							type = attribute.getNodeValue();
							
							attribute = d.getAttributes().getNamedItem("classId");
							if (attribute != null)
							{
								cId = Integer.parseInt(attribute.getNodeValue());
								classId = ClassId.values()[cId];
							}
							
							attribute = d.getAttributes().getNamedItem("parentClassId");
							if (attribute != null)
							{
								parentClassId = Integer.parseInt(attribute.getNodeValue());
								
								if ((cId != parentClassId) && (parentClassId > -1))
								{
									_parentClassMap.putIfAbsent(classId, ClassId.values()[parentClassId]);
								}
							}
							
							for (Node c = d.getFirstChild(); c != null; c = c.getNextSibling())
							{
								if ("skill".equalsIgnoreCase(c.getNodeName()))
								{
									final StatsSet learnSkillSet = new StatsSet();
									
									int skillId;
									int skillLvl;
									
									attributes = c.getAttributes();
									
									attribute = attributes.getNamedItem("skillName");
									if (attribute == null)
									{
										_log.severe(getClass().getSimpleName() + ": Missing skillName, skipping!");
										continue;
									}
									learnSkillSet.set("skillName", attribute.getNodeValue());
									
									attribute = attributes.getNamedItem("skillIdLvl");
									if (attribute == null)
									{
										_log.severe(getClass().getSimpleName() + ": Missing skillIdLvl, skipping!");
										continue;
									}
									
									try
									{
										skillId = Integer.parseInt(attribute.getNodeValue().split(",")[0]);
										skillLvl = Integer.parseInt(attribute.getNodeValue().split(",")[1]);
										learnSkillSet.set("skillId", skillId);
										learnSkillSet.set("skillLvl", skillLvl);
									}
									catch (Exception e)
									{
										_log.severe(getClass().getSimpleName() + ": Malformed skillIdLvl, skipping!");
										continue;
									}
									
									attribute = attributes.getNamedItem("getLevel");
									if (attribute != null)
									{
										learnSkillSet.set("getLevel", attribute.getNodeValue());
									}
									
									attribute = attributes.getNamedItem("autoGet");
									if (attribute != null)
									{
										learnSkillSet.set("autoGet", attribute.getNodeValue());
									}
									
									attribute = attributes.getNamedItem("levelUpSp");
									if (attribute != null)
									{
										learnSkillSet.set("levelUpSp", attribute.getNodeValue());
									}
									
									attribute = attributes.getNamedItem("itemsIdCount");
									if (attribute != null)
									{
										learnSkillSet.set("itemsIdCount", attribute.getNodeValue());
									}
									
									attribute = attributes.getNamedItem("race");
									if (attribute != null)
									{
										learnSkillSet.set("race", attribute.getNodeValue());
									}
									
									attribute = attributes.getNamedItem("preReqSkillIdLvl");
									if (attribute != null)
									{
										learnSkillSet.set("preReqSkillIdLvl", attribute.getNodeValue());
									}
									
									attribute = attributes.getNamedItem("socialClass");
									if (attribute != null)
									{
										learnSkillSet.set("socialClass", attribute.getNodeValue());
									}
									
									attribute = attributes.getNamedItem("subClassLvlNumber");
									if (attribute != null)
									{
										learnSkillSet.set("subClassLvlNumber", attribute.getNodeValue());
									}
									
									attribute = attributes.getNamedItem("residenceSkill");
									if (attribute != null)
									{
										learnSkillSet.set("residenceSkill", attribute.getNodeValue());
									}
									
									attribute = attributes.getNamedItem("residenceIds");
									if (attribute != null)
									{
										learnSkillSet.set("residenceIds", attribute.getNodeValue());
									}
									
									attribute = attributes.getNamedItem("learnedByNpc");
									if (attribute != null)
									{
										learnSkillSet.set("learnedByNpc", attribute.getNodeValue());
									}
									
									attribute = attributes.getNamedItem("learnedByFS");
									if (attribute != null)
									{
										learnSkillSet.set("learnedByFS", attribute.getNodeValue());
									}
									
									final L2SkillLearn skillLearn = new L2SkillLearn(learnSkillSet);
									if (type.equals("classSkillTree"))
									{
										classSkillTree.put(SkillTable.getSkillHashCode(skillId, skillLvl), skillLearn);
									}
									else if (type.equals("transferSkillTree"))
									{
										trasferSkillTree.put(SkillTable.getSkillHashCode(skillId, skillLvl), skillLearn);
									}
									else
									{
										if (type.equals("collectSkillTree"))
										{
											_collectSkillTree.put(SkillTable.getSkillHashCode(skillId, skillLvl), skillLearn);
										}
										else if (type.equals("fishingSkillTree"))
										{
											_fishingSkillTree.put(SkillTable.getSkillHashCode(skillId, skillLvl), skillLearn);
										}
										else if (type.equals("pledgeSkillTree"))
										{
											_pledgeSkillTree.put(SkillTable.getSkillHashCode(skillId, skillLvl), skillLearn);
										}
										else if (type.equals("subClassSkillTree"))
										{
											_subClassSkillTree.put(SkillTable.getSkillHashCode(skillId, skillLvl), skillLearn);
										}
										else if (type.equals("subPledgeSkillTree"))
										{
											_subPledgeSkillTree.put(SkillTable.getSkillHashCode(skillId, skillLvl), skillLearn);
										}
										else if (type.equals("transformSkillTree"))
										{
											_transformSkillTree.put(SkillTable.getSkillHashCode(skillId, skillLvl), skillLearn);
										}
									}
								}
							}
							
							if (type.equals("classSkillTree"))
							{
								if (_classSkillTrees.get(classId) == null)
								{
									_classSkillTrees.put(classId, classSkillTree);
								}
								else
								{
									_classSkillTrees.get(classId).putAll(classSkillTree);
								}
							}
							else if (type.equals("transferSkillTree"))
							{
								_transferSkillTrees.put(classId, trasferSkillTree);
							}
						}
					}
				}
			}
		}
		generateCheckArrays();
	}
	
	/**
	 * Wrapper for class skill trees.
	 * @return the {@code _classSkillTrees}, if it's null allocate a new map and returns it.
	 */
	private FastMap<ClassId, FastMap<Integer, L2SkillLearn>> getClassSkillTrees()
	{
		if (_classSkillTrees == null)
		{
			_classSkillTrees = new FastMap<ClassId, FastMap<Integer, L2SkillLearn>>();
		}
		return _classSkillTrees;
	}
	
	/**
	 * Method to get the complete skill tree for a given class id.<br>
	 * Includes all parent skill trees.
	 * @param classId the class skill tree ID.
	 * @return the complete Class Skill Tree including skill trees from parent class for a given {@code classId}.
	 */
	public FastMap<Integer, L2SkillLearn> getCompleteClassSkillTree(ClassId classId)
	{
		final FastMap<Integer, L2SkillLearn> skillTree = new FastMap<Integer, L2SkillLearn>();
		
		while ((classId != null) && (getClassSkillTrees().get(classId) != null))
		{
			skillTree.putAll(getClassSkillTrees().get(classId));
			classId = _parentClassMap.get(classId);
		}
		return skillTree;
	}
	
	/**
	 * @param classId the transfer skill tree ID.
	 * @return the complete Transfer Skill Tree for a given {@code classId}.
	 */
	public FastMap<Integer, L2SkillLearn> getTransferSkillTree(ClassId classId)
	{
		//If new classes are implemented over 3rd class, we use a recursive call.
		if (classId.level() >= 3)
		{
			classId = classId.getParent();
			return getTransferSkillTree(classId);
		}
		return _transferSkillTrees.get(classId);
	}
	
	/**
	 * @return the complete Collect Skill Tree. 
	 */
	public FastMap<Integer, L2SkillLearn> getCollectSkillTree()
	{
		return _collectSkillTree;
	}
	
	/**
	 * @return the complete Fishing Skill Tree. 
	 */
	public FastMap<Integer, L2SkillLearn> getFishingSkillTree()
	{
		return _fishingSkillTree;
	}
	
	/**
	 * @return the complete Pledge Skill Tree. 
	 */
	public FastMap<Integer, L2SkillLearn> getPledgeSkillTree()
	{
		return _pledgeSkillTree;
	}
	
	/**
	 * @return the complete Sub-Class Skill Tree. 
	 */
	public FastMap<Integer, L2SkillLearn> getSubClassSkillTree()
	{
		return _subClassSkillTree;
	}
	
	/**
	 * @return the complete Sub-Pledge Skill Tree. 
	 */
	public FastMap<Integer, L2SkillLearn> getSubPledgeSkillTree()
	{
		return _subPledgeSkillTree;
	}
	
	/**
	 * @return the complete Transform Skill Tree. 
	 */
	public FastMap<Integer, L2SkillLearn> getTransformSkillTree()
	{
		return _transformSkillTree;
	}
	
	/**
	 * @param player the learning skill player.
	 * @param classId the learning skill class ID.
	 * @param includeByFs if {@code true} skills from Forgotten Scroll will be included.
	 * @param includeAutoGet if {@code true} Auto-Get skills will be included.
	 * @return all available skills for a given {@code player}, {@code classId}, {@code includeByFs} and {@code includeAutoGet}.
	 */
	public FastList<L2SkillLearn> getAvailableSkills(L2PcInstance player, ClassId classId, boolean includeByFs, boolean includeAutoGet)
	{
		final FastList<L2SkillLearn> result = new FastList<L2SkillLearn>();
		final FastMap<Integer, L2SkillLearn> skills = getCompleteClassSkillTree(classId);
		
		if (skills.isEmpty())
		{
			//The Skill Tree for this class is undefined.
			_log.warning(getClass().getSimpleName() + ": Skilltree for class " + classId + " is not defined!");
			return result;
		}
		
		final L2Skill[] oldSkills = player.getAllSkills();
		
		for (L2SkillLearn temp : skills.values())
		{
			if (((includeAutoGet && temp.isAutoGet()) || temp.isLearnedByNpc() || (includeByFs && temp.isLearnedByFS())) && (player.getLevel() >= temp.getGetLevel()))
			{
				boolean knownSkill = false;
				
				for (int j = 0; (j < oldSkills.length) && !knownSkill; j++)
				{
					if (oldSkills[j].getId() == temp.getSkillId())
					{
						if (oldSkills[j].getLevel() == (temp.getSkillLevel() - 1))
						{
							//This is the next level of a known skill:
							result.add(temp);
						}
						knownSkill = true;
					}
				}
				
				if (!knownSkill && (temp.getSkillLevel() == 1))
				{
					//This is a new skill:
					result.add(temp);
				}
			}
		}
		return result;
	}
	
	/**
	 * @param player the player requesting the Auto-Get skills.
	 * @return all the available Auto-Get skills for a given {@code player}.
	 */
	public FastList<L2SkillLearn> getAvailableAutoGetSkills(L2PcInstance player)
	{
		final FastList<L2SkillLearn> result = new FastList<L2SkillLearn>();
		final FastMap<Integer, L2SkillLearn> skills = getCompleteClassSkillTree(player.getClassId());
		
		if (skills.size() < 1)
		{
			//The Skill Tree for this class is undefined, so we return an empty list.
			_log.warning(getClass().getSimpleName() + ": Skill Tree for this classId(" + player.getClassId() + ") is not defined!");
			return new FastList<L2SkillLearn>();
		}
		
		final L2Skill[] oldSkills = player.getAllSkills();
		
		for (L2SkillLearn temp : skills.values())
		{
			if ((temp.getRaces() != null) && Util.contains(temp.getRaces(), 4) && !player.hasDwarvenCraft())
			{
				continue;
			}
			
			if (temp.isAutoGet() && (player.getLevel() >= temp.getGetLevel()))
			{
				boolean knownSkill = false;
				
				for (int j = 0; (j < oldSkills.length) && !knownSkill; j++)
				{
					if (oldSkills[j].getId() == temp.getSkillId())
					{
						if (oldSkills[j].getLevel() < temp.getSkillLevel())
						{
							result.add(temp);
						}
						knownSkill = true;
					}
				}
				
				if (!knownSkill)
				{
					result.add(temp);
				}
			}
		}
		return result;
	}
	
	/**
	 * Dwarvens will get additional dwarven only fishing skills.
	 * @param player
	 * @return all the available Fishing skills for a given {@code player}.
	 */
	public FastList<L2SkillLearn> getAvailableFishingSkills(L2PcInstance player)
	{
		final FastList<L2SkillLearn> result = new FastList<L2SkillLearn>();
		final FastMap<Integer, L2SkillLearn> skills = new FastMap<Integer, L2SkillLearn>();
		
		skills.putAll(_fishingSkillTree);
		
		if (skills.size() < 1)
		{
			//The Skill Tree for fishing skills is undefined.
			_log.warning(getClass().getSimpleName() + ": Skilltree for fishing is not defined !");
			return new FastList<L2SkillLearn>();
		}
		
		final L2Skill[] oldSkills = player.getAllSkills();
		
		for (L2SkillLearn temp : skills.values())
		{
			//If skill is Dwarven only and player is not Dwarven.
			if ((temp.getRaces() != null) && Util.contains(temp.getRaces(), 4) && !player.hasDwarvenCraft())
			{
				continue;
			}
			
			if (temp.isLearnedByNpc() && (player.getLevel() >= temp.getGetLevel()))
			{
				boolean knownSkill = false;
				
				for (int j = 0; (j < oldSkills.length) && !knownSkill; j++)
				{
					if (oldSkills[j].getId() == temp.getSkillId())
					{
						if (oldSkills[j].getLevel() == (temp.getSkillLevel() - 1))
						{
							//This is the next level of a known skill:
							result.add(temp);
						}
						knownSkill = true;
					}
				}
				
				if (!knownSkill && (temp.getSkillLevel() == 1))
				{
					//This is a new skill:
					result.add(temp);
				}
			}
		}
		
		return result;
	}
	
	/**
	 * Used in Gracia continent.
	 * @param player the collecting skill learning player.
	 * @return all the available Collecting skills for a given {@code player}.
	 */
	public FastList<L2SkillLearn> getAvailableCollectSkills(L2PcInstance player)
	{
		final FastList<L2SkillLearn> result = new FastList<L2SkillLearn>();
		final FastMap<Integer, L2SkillLearn> skills = new FastMap<Integer, L2SkillLearn>();
		
		skills.putAll(_collectSkillTree);
		
		if (skills.size() < 1)
		{
			//The Skill Tree for Collecting skills is undefined.
			_log.warning(getClass().getSimpleName() + ": Skilltree for collecting skills is not defined !");
			return new FastList<L2SkillLearn>();
		}
		
		final L2Skill[] oldSkills = player.getAllSkills();
		
		for (L2SkillLearn temp : skills.values())
		{
			boolean knownSkill = false;
			
			for (int j = 0; (j < oldSkills.length) && !knownSkill; j++)
			{
				if (oldSkills[j].getId() == temp.getSkillId())
				{
					if (oldSkills[j].getLevel() == (temp.getSkillLevel() - 1))
					{
						//This is the next level of a known skill:
						result.add(temp);
					}
					knownSkill = true;
				}
			}
			
			if (!knownSkill && (temp.getSkillLevel() == 1))
			{
				//This is a new skill:
				result.add(temp);
			}
		}
		return result;
	}
	
	/**
	 * @param player the transfer skill learning player.
	 * @return all the available Transfer skills for a given {@code player}.
	 */
	public FastList<L2SkillLearn> getAvailableTransferSkills(L2PcInstance player)
	{
		final FastList<L2SkillLearn> result = new FastList<L2SkillLearn>();
		
		ClassId classId = player.getClassId();
		
		//If new classes are implemented over 3rd class, a different way should be implemented.
		if (classId.level() == 3)
		{
			classId = classId.getParent();
		}
		
		if (_transferSkillTrees.get(classId) == null)
		{
			return result;
		}
		
		for (L2SkillLearn temp : _transferSkillTrees.get(classId).values())
		{
			//If player doesn't know this transfer skill:
			if (player.getKnownSkill(temp.getSkillId()) == null)
			{
				result.add(temp);
			}
		}
		return result;
	}
	
	/**
	 * Some transformations are not available for some races.
	 * @param playerthe transformation skill learning player.
	 * @return all the available Transformation skills for a given {@code player}.
	 */
	public FastList<L2SkillLearn> getAvailableTransformSkills(L2PcInstance player)
	{
		final FastList<L2SkillLearn> result = new FastList<L2SkillLearn>();
		final FastMap<Integer, L2SkillLearn> skills = _transformSkillTree;
		
		if (skills == null)
		{
			//The Skill Tree for Transformation skills is undefined.
			_log.warning(getClass().getSimpleName() + ": No Transform skills defined!");
			return new FastList<L2SkillLearn>();
		}
		
		final L2Skill[] oldSkills = player.getAllSkills();
		
		for (L2SkillLearn temp : skills.values())
		{
			if ((player.getLevel() >= temp.getGetLevel()) && ((temp.getRaces() == null) || Util.contains(temp.getRaces(), player.getRace().ordinal())))
			{
				boolean knownSkill = false;
				
				for (int j = 0; (j < oldSkills.length) && !knownSkill; j++)
				{
					if (oldSkills[j].getId() == temp.getSkillId())
					{
						if (oldSkills[j].getLevel() == (temp.getSkillLevel() - 1))
						{
							//This is the next level of a known skill:
							result.add(temp);
						}
						knownSkill = true;
					}
				}
				
				if (!knownSkill && (temp.getSkillLevel() == 1))
				{
					//This is a new skill:
					result.add(temp);
				}
			}
		}
		return result;
	}
	
	/**
	 * @param clan the pledge skill learning clan.
	 * @return all the available Pledge skills for a given {@code clan}.
	 */
	public FastList<L2SkillLearn> getAvailablePledgeSkills(L2Clan clan)
	{
		final FastList<L2SkillLearn> result = new FastList<L2SkillLearn>();
		final FastMap<Integer, L2SkillLearn> skills = _pledgeSkillTree;
		
		if (skills == null)
		{
			//The Skill Tree for Pledge skills is undefined.
			_log.warning(getClass().getSimpleName() + ": No clan skills defined!");
			return new FastList<L2SkillLearn>();
		}
		
		L2Skill[] oldSkills = clan.getAllSkills();
		
		for (L2SkillLearn temp : skills.values())
		{
			if (!temp.isResidencialSkill() && (clan.getLevel() >= temp.getGetLevel()))
			{
				boolean knownSkill = false;
				
				for (int j = 0; (j < oldSkills.length) && !knownSkill; j++)
				{
					if (oldSkills[j].getId() == temp.getSkillId())
					{
						if (oldSkills[j].getLevel() == (temp.getSkillLevel() - 1))
						{
							//This is the next level of a known skill:
							result.add(temp);
						}
						knownSkill = true;
					}
				}
				
				if (!knownSkill && (temp.getSkillLevel() == 1))
				{
					//This is a new skill:
					result.add(temp);
				}
			}
		}
		return result;
	}
	
	/**
	 * @param clan the sub-pledge skill learning clan.
	 * @return all the available Sub-Pledge skills for a given {@code clan}.
	 */
	public FastList<L2SkillLearn> getAvailableSubPledgeSkills(L2Clan clan)
	{
		final FastList<L2SkillLearn> result = new FastList<L2SkillLearn>();
		final FastMap<Integer, L2SkillLearn> skills = _subPledgeSkillTree;
		
		if (skills == null)
		{
			//The Skill Tree for Sub-Pledge skills is undefined.
			_log.warning(getClass().getSimpleName() + ": No sub-clan skills defined!");
			return new FastList<L2SkillLearn>();
		}
		
		for (L2SkillLearn temp : skills.values())
		{
			if ((clan.getLevel() >= temp.getGetLevel()) && clan.isLearnableSubSkill(temp.getSkillId(), temp.getSkillLevel()))
			{
				result.add(temp);
			}
		}
		return result;
	}
	
	/**
	 * @param player the sub-class skill learning player.
	 * @return all the available Sub-Class skills for a given {@code player}.
	 */
	public FastList<L2SkillLearn> getAvailableSubClassSkills(L2PcInstance player)
	{
		final FastList<L2SkillLearn> result = new FastList<L2SkillLearn>();
		final FastMap<Integer, L2SkillLearn> skills = _subClassSkillTree;
		
		if (skills == null)
		{
			//The Skill Tree for Sub-Class skills is undefined.
			_log.warning(getClass().getSimpleName() + ": No Sub-Class skills defined!");
			return new FastList<L2SkillLearn>();
		}
		
		final L2Skill[] oldSkills = player.getAllSkills();
		
		for (L2SkillLearn temp : skills.values())
		{
			if (player.getLevel() >= temp.getGetLevel())
			{
				int[][] subClassConds = null;
				for (SubClass subClass : player.getSubClasses().values())
				{
					subClassConds = temp.getSubClassConditions();
					if ((subClassConds != null) && (subClass.getClassIndex() <= subClassConds.length) && (subClass.getClassIndex() == subClassConds[subClass.getClassIndex() - 1][1]) && (subClassConds[subClass.getClassIndex() - 1][0] <= subClass.getLevel()))
					{
						boolean knownSkill = false;
						
						for (int j = 0; (j < oldSkills.length) && !knownSkill; j++)
						{
							if (oldSkills[j].getId() == temp.getSkillId())
							{
								if (oldSkills[j].getLevel() == (temp.getSkillLevel() - 1))
								{
									//This is the next level of a known skill:
									result.add(temp);
								}
								knownSkill = true;
							}
						}
						
						if (!knownSkill && (temp.getSkillLevel() == 1))
						{
							//This is a new skill:
							result.add(temp);
						}
					}
				}
			}
		}
		return result;
	}
	
	/**
	 * @param residenceId the id of the Castle, Fort, Territory.
	 * @return all the available Residential skills for a given {@code residenceId}.
	 */
	public FastList<L2SkillLearn> getAvailableResidentialSkills(int residenceId)
	{
		final FastList<L2SkillLearn> result = new FastList<L2SkillLearn>();
		final FastMap<Integer, L2SkillLearn> skills = _pledgeSkillTree;
		
		if (skills == null)
		{
			//The Skill Tree for Residential skills is undefined?
			_log.warning(getClass().getSimpleName() + ": No residential skills defined!");
			return new FastList<L2SkillLearn>();
		}
		
		for (L2SkillLearn temp : skills.values())
		{
			if (temp.isResidencialSkill() && (temp.getRecidenceIds() != null) && Util.contains(temp.getRecidenceIds(), residenceId))
			{
				result.add(temp);
			}
		}
		return result;
	}
	
	/**
	 * @param id the transformation skill ID.
	 * @param lvl the transformation skill level.
	 * @return the transform skill from the Transform Skill Tree for a given {@code id} and {@code lvl}.
	 */
	public L2SkillLearn getTransformSkill(int id, int lvl)
	{
		return _transformSkillTree.get(SkillTable.getSkillHashCode(id, lvl));
	}
	
	/**
	 * @param id the class skill ID.
	 * @param lvl the class skill level.
	 * @param classId the class skill tree ID.
	 * @return the class skill from the Class Skill Trees for a given {@code classId}, {@code id} and {@code lvl}.
	 */
	public L2SkillLearn getClassSkill(int id, int lvl, ClassId classId)
	{
		final FastMap<Integer, L2SkillLearn> skills = getCompleteClassSkillTree(classId);
		
		return skills.get(SkillTable.getSkillHashCode(id, lvl));
	}
	
	/**
	 * @param id the fishing skill ID.
	 * @param lvl the fishing skill level.
	 * @return Fishing skill from the Fishing Skill Tree for a given {@code id} and {@code lvl}.
	 */
	public L2SkillLearn getFishingSkill(int id, int lvl)
	{
		return _fishingSkillTree.get(SkillTable.getSkillHashCode(id, lvl));
	}
	
	/**
	 * @param id the pledge skill ID.
	 * @param lvl the pledge skill level.
	 * @return the pledge skill from the Pledge Skill Tree for a given {@code id} and {@code lvl}.
	 */
	public L2SkillLearn getPledgeSkill(int id, int lvl)
	{
		return _pledgeSkillTree.get(SkillTable.getSkillHashCode(id, lvl));
	}
	
	/**
	 * @param id the sub-pledge skill ID.
	 * @param lvl the sub-pledge skill level.
	 * @return the sub-pledge skill from the Sub-Pledge Skill Tree for a given {@code id} and {@code lvl}.
	 */
	public L2SkillLearn getSubPledgeSkill(int id, int lvl)
	{
		return _subPledgeSkillTree.get(SkillTable.getSkillHashCode(id, lvl));
	}
	
	/**
	 * @param id the transfer skill ID.
	 * @param lvl the transfer skill level.
	 * @param classId the transfer skill tree ID.
	 * @return the transfer skill from the Transfer Skill Trees for a given {@code classId}, {@code id} and {@code lvl}.
	 */
	public L2SkillLearn getTransferSkill(int id, int lvl, ClassId classId)
	{
		if (classId.getParent() != null)
		{
			final ClassId parentId = classId.getParent();
			if (_transferSkillTrees.get(parentId) != null)
			{
				return _transferSkillTrees.get(parentId).get(SkillTable.getSkillHashCode(id, lvl));
			}
		}
		return null;
	}
	
	/**
	 * @param id the sub-class skill ID.
	 * @param lvl the sub-class skill level.
	 * @return the sub-class skill from the Sub-Class Skill Tree for a given {@code id} and {@code lvl}.
	 */
	public L2SkillLearn getSubClassSkill(int id, int lvl)
	{
		return _subClassSkillTree.get(SkillTable.getSkillHashCode(id, lvl));
	}
	
	/**
	 * @param id the collect skill ID.
	 * @param lvl the collect skill level.
	 * @return the collect skill from the Collect Skill Tree for a given {@code id} and {@code lvl}.
	 */
	public L2SkillLearn getCollectSkill(int id, int lvl)
	{
		return _collectSkillTree.get(SkillTable.getSkillHashCode(id, lvl));
	}
	
	/**
	 * @param player the player that requires the minimum level.
	 * @param skillTree the skill tree to search the minimum get level.
	 * @return the minimum level for a new skill for a given {@code player} and {@code skillTree}.
	 */
	public int getMinLevelForNewSkill(L2PcInstance player, FastMap<Integer, L2SkillLearn> skillTree)
	{
		int minLevel = 0;
		if (skillTree.isEmpty())
		{
			_log.warning(getClass().getSimpleName() + ": SkillTree is not defined for getMinLevelForNewSkill!");
		}
		else
		{
			for (L2SkillLearn s : skillTree.values())
			{
				if (s.isLearnedByNpc() && (player.getLevel() < s.getGetLevel()))
				{
					if ((minLevel == 0) || (minLevel > s.getGetLevel()))
					{
						minLevel = s.getGetLevel();
					}
				}
			}
		}
		return minLevel;
	}
	
	/**
	 * Create and store hash values for skills for easy and fast checks.
	 */
	private void generateCheckArrays()
	{
		int i;
		int[] array;
		
		//Class specific skills:
		FastMap<Integer, L2SkillLearn> tempMap;
		TIntObjectHashMap<int[]> result = new TIntObjectHashMap<int[]>(getClassSkillTrees().keySet().size());
		for (ClassId cls : getClassSkillTrees().keySet())
		{
			i = 0;
			tempMap = getCompleteClassSkillTree(cls);
			array = new int[tempMap.size()];
			for (int h : tempMap.keySet())
			{
				array[i++] = h;
			}
			Arrays.sort(array);
			result.put(cls.ordinal(), array);
		}
		_skillsByClassIdHashCodes = result;
		
		//Race specific skills from Fishing and Transformation skill trees.
		final FastList<Integer> list = FastList.newInstance();
		result = new TIntObjectHashMap<int[]>(Race.values().length);
		for (Race r : Race.values())
		{
			for (L2SkillLearn s : _fishingSkillTree.values())
			{
				if ((s.getRaces() != null) && Util.contains(s.getRaces(), r.ordinal()))
				{
					list.add(SkillTable.getSkillHashCode(s.getSkillId(), s.getSkillLevel()));
				}
			}
			
			for (L2SkillLearn s : _transformSkillTree.values())
			{
				if ((s.getRaces() != null) && Util.contains(s.getRaces(), r.ordinal()))
				{
					list.add(SkillTable.getSkillHashCode(s.getSkillId(), s.getSkillLevel()));
				}
			}
			
			i = 0;
			array = new int[list.size()];
			for (int s : list)
			{
				array[i++] = s;
			}
			Arrays.sort(array);
			result.put(r.ordinal(), array);
			list.clear();
		}
		_skillsByRaceHashCodes = result;
		
		//Skills available for all classes and races
		for (L2SkillLearn s : _fishingSkillTree.values())
		{
			if (s.getRaces() == null)
			{
				list.add(SkillTable.getSkillHashCode(s.getSkillId(), s.getSkillLevel()));
			}
		}
		
		for (L2SkillLearn s : _transformSkillTree.values())
		{
			if (s.getRaces() == null)
			{
				list.add(SkillTable.getSkillHashCode(s.getSkillId(), s.getSkillLevel()));
			}
		}
		
		for (L2SkillLearn s : _collectSkillTree.values())
		{
			list.add(SkillTable.getSkillHashCode(s.getSkillId(), s.getSkillLevel()));
		}
		
		i = 0;
		array = new int[list.size()];
		for (int s : list)
		{
			array[i++] = s;
		}
		Arrays.sort(array);
		_allSkillsHashCodes = array;
		
		FastList.recycle(list);
	}
	
	/**
	 * Verify if the give skill is valid for the givem player.
	 * GM's skills are excluded for GM players.
	 * @param player the player to verify the skill.
	 * @param skill the skill to be verified.
	 * @return {@code true} if the skill is allowed to the given player. 
	 */
	public boolean isSkillAllowed(L2PcInstance player, L2Skill skill)
	{
		if (skill.isExcludedFromCheck())
		{
			return true;
		}
		
		if (player.isGM() && skill.isGMSkill())
		{
			return true;
		}
		
		//Prevent accidental skill remove during reload
		if (_loading)
		{
			return true;
		}
		
		final int maxLvl = SkillTable.getInstance().getMaxLevel(skill.getId());
		final int hashCode = SkillTable.getSkillHashCode(skill.getId(), Math.min(skill.getLevel(), maxLvl));
		
		if (Arrays.binarySearch(_skillsByClassIdHashCodes.get(player.getClassId().ordinal()), hashCode) >= 0)
		{
			return true;
		}
		
		if (Arrays.binarySearch(_skillsByRaceHashCodes.get(player.getRace().ordinal()), hashCode) >= 0)
		{
			return true;
		}
		
		if (Arrays.binarySearch(_allSkillsHashCodes, hashCode) >= 0)
		{
			return true;
		}
		
		//Exclude Transfer Skills from this check.
		if (getTransferSkill(skill.getId(), skill.getLevel(), player.getClassId()) != null)
		{
			return true;
		}
		
		return false;
	}
	
	public static SkillTreesData getInstance()
	{
		return SingletonHolder._instance;
	}
	
	/**
	 * Singleton holder for the SkillTreesData class.
	 */
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final SkillTreesData _instance = new SkillTreesData();
	}
}
