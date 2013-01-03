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
package com.l2jserver.gameserver.datatables;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.l2jserver.gameserver.engines.DocumentParser;
import com.l2jserver.gameserver.model.L2PetData;
import com.l2jserver.gameserver.model.L2PetLevelData;
import com.l2jserver.gameserver.model.StatsSet;

/**
 * This class parse and hold all pet parameters.<br>
 * TODO: Unhardcode where is possible boolean methods and load and use all pet parameters.
 * @author Zoey76 (rework)
 */
public final class PetDataTable extends DocumentParser
{
	private static final Map<Integer, L2PetData> _pets = new HashMap<>();
	
	/**
	 * Instantiates a new pet data table.
	 */
	protected PetDataTable()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_pets.clear();
		parseDatapackFile("data/stats/npc/PetData.xml");
		_log.info(getClass().getSimpleName() + ": Loaded " + _pets.size() + " Pets.");
	}
	
	@Override
	protected void parseDocument()
	{
		NamedNodeMap attrs;
		Node n = getCurrentDocument().getFirstChild();
		for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
		{
			if (d.getNodeName().equals("pet"))
			{
				int npcId = parseInt(d.getAttributes(), "id");
				// index ignored for now
				L2PetData data = new L2PetData();
				for (Node p = d.getFirstChild(); p != null; p = p.getNextSibling())
				{
					if (p.getNodeName().equals("set"))
					{
						attrs = p.getAttributes();
						String type = attrs.getNamedItem("name").getNodeValue();
						if ("food".equals(type))
						{
							for (String foodId : attrs.getNamedItem("val").getNodeValue().split(";"))
							{
								data.addFood(Integer.valueOf(foodId));
							}
						}
						else if ("load".equals(type))
						{
							data.setLoad(parseInt(attrs, "val"));
						}
						else if ("hungry_limit".equals(type))
						{
							data.setHungryLimit(parseInt(attrs, "val"));
						}
						// sync_level and evolve ignored
					}
					else if (p.getNodeName().equals("skills"))
					{
						for (Node s = p.getFirstChild(); s != null; s = s.getNextSibling())
						{
							if (s.getNodeName().equals("skill"))
							{
								attrs = s.getAttributes();
								data.addNewSkill(parseInt(attrs, "skillId"), parseInt(attrs, "skillLvl"), parseInt(attrs, "minLvl"));
							}
						}
					}
					else if (p.getNodeName().equals("stats"))
					{
						for (Node s = p.getFirstChild(); s != null; s = s.getNextSibling())
						{
							if (s.getNodeName().equals("stat"))
							{
								final int level = Integer.parseInt(s.getAttributes().getNamedItem("level").getNodeValue());
								final StatsSet set = new StatsSet();
								for (Node bean = s.getFirstChild(); bean != null; bean = bean.getNextSibling())
								{
									if (bean.getNodeName().equals("set"))
									{
										attrs = bean.getAttributes();
										set.set(attrs.getNamedItem("name").getNodeValue(), attrs.getNamedItem("val").getNodeValue());
									}
								}
								data.addNewStat(level, new L2PetLevelData(set));
							}
						}
					}
				}
				_pets.put(npcId, data);
			}
		}
	}
	
	/**
	 * Gets the pet level data.
	 * @param petId the pet Id.
	 * @param petLevel the pet level.
	 * @return the pet's parameters for the given Id and level.
	 */
	public L2PetLevelData getPetLevelData(int petId, int petLevel)
	{
		final L2PetData pd = getPetData(petId);
		if (pd != null)
		{
			return pd.getPetLevelData(petLevel);
		}
		return null;
	}
	
	/**
	 * Gets the pet data.
	 * @param petId the pet Id.
	 * @return the pet data
	 */
	public L2PetData getPetData(int petId)
	{
		if (!_pets.containsKey(petId))
		{
			_log.info(getClass().getSimpleName() + ": Missing pet data for npcid: " + petId);
		}
		return _pets.get(petId);
	}
	
	/**
	 * Gets the pet min level.
	 * @param petId the pet Id.
	 * @return the pet min level
	 */
	public int getPetMinLevel(int petId)
	{
		return _pets.get(petId).getMinLevel();
	}
	
	/**
	 * Checks if is wolf.
	 * @param npcId the NPC Id to verify.
	 * @return {@code true} if the given Id is from a wolf, {@code false} otherwise.
	 */
	public static boolean isWolf(int npcId)
	{
		return npcId == 12077;
	}
	
	/**
	 * Checks if is evolved wolf.
	 * @param npcId the NPC Id to verify.
	 * @return {@code true} if the given Id is from an evolved wolf, {@code false} otherwise.
	 */
	public static boolean isEvolvedWolf(int npcId)
	{
		return (npcId == 16030) || (npcId == 16037) || (npcId == 16025) || (npcId == 16041) || (npcId == 16042);
	}
	
	/**
	 * Checks if is sin eater.
	 * @param npcId the NPC Id to verify.
	 * @return {@code true} if the given Id is from a Sin Eater, {@code false} otherwise.
	 */
	public static boolean isSinEater(int npcId)
	{
		return npcId == 12564;
	}
	
	/**
	 * Checks if is hatchling.
	 * @param npcId the NPC Id to verify.
	 * @return {@code true} if the given Id is from a hatchling, {@code false} otherwise.
	 */
	public static boolean isHatchling(int npcId)
	{
		return (npcId > 12310) && (npcId < 12314);
	}
	
	/**
	 * Checks if is strider.
	 * @param npcId the NPC Id to verify.
	 * @return {@code true} if the given Id is from a strider, {@code false} otherwise.
	 */
	public static boolean isStrider(int npcId)
	{
		return ((npcId > 12525) && (npcId < 12529)) || ((npcId > 16037) && (npcId < 16041)) || (npcId == 16068);
	}
	
	/**
	 * Checks if is wyvern.
	 * @param npcId the NPC Id to verify.
	 * @return {@code true} if the given Id is from a wyvern, {@code false} otherwise.
	 */
	public static boolean isWyvern(int npcId)
	{
		return npcId == 12621;
	}
	
	/**
	 * Checks if is baby.
	 * @param npcId the NPC Id to verify.
	 * @return {@code true} if the given Id is from a baby pet, {@code false} otherwise.
	 */
	public static boolean isBaby(int npcId)
	{
		return (npcId > 12779) && (npcId < 12783);
	}
	
	/**
	 * Checks if is improved baby.
	 * @param npcId the NPC Id to verify.
	 * @return {@code true} if the given Id is from an improved baby pet, {@code false} otherwise.
	 */
	public static boolean isImprovedBaby(int npcId)
	{
		return (npcId > 16033) && (npcId < 16037);
	}
	
	/**
	 * Gets the pet items by npc.
	 * @param npcId the NPC Id to get its summoning item.
	 * @return an array containing the list of summoning items for the given NPC Id.
	 */
	public static int[] getPetItemsByNpc(int npcId)
	{
		switch (npcId)
		{
			case 12077:// Wolf
				return new int[]
				{
					2375
				};
			case 16025:// Great Wolf
				return new int[]
				{
					9882
				};
			case 16030:// Black Wolf
				return new int[]
				{
					10163
				};
			case 16037:// White Great Wolf
				return new int[]
				{
					10307
				};
			case 16041:// Fenrir
				return new int[]
				{
					10426
				};
			case 16042:// White Fenrir
				return new int[]
				{
					10611
				};
			case 12564:// Sin Eater
				return new int[]
				{
					4425
				};
			case 12311:// hatchling of wind
			case 12312:// hatchling of star
			case 12313:// hatchling of twilight
				return new int[]
				{
					3500,
					3501,
					3502
				};
			case 12526:// wind strider
			case 12527:// Star strider
			case 12528:// Twilight strider
			case 16038: // red strider of wind
			case 16039: // red strider of star
			case 16040: // red strider of dusk
			case 16068: // Guardian Strider
				return new int[]
				{
					4422,
					4423,
					4424,
					10308,
					10309,
					10310,
					14819
				};
			case 12621:// Wyvern
				return new int[]
				{
					8663
				};
			case 12780:// Baby Buffalo
			case 12782:// Baby Cougar
			case 12781:// Baby Kookaburra
				return new int[]
				{
					6648,
					6649,
					6650
				};
			case 16034:// Improved Baby Buffalo
			case 16036:// Improved Baby Cougar
			case 16035:// Improved Baby Kookaburra
				return new int[]
				{
					10311,
					10312,
					10313
				};
				// unknown item id.. should never happen
			default:
				return new int[]
				{
					0
				};
		}
	}
	
	/**
	 * Checks if is mountable.
	 * @param npcId the NPC Id to verify.
	 * @return {@code true} if the given Id is from a mountable pet, {@code false} otherwise.
	 */
	public static boolean isMountable(int npcId)
	{
		return (npcId == 12526 // wind strider
		) || (npcId == 12527 // star strider
		) || (npcId == 12528 // twilight strider
		) || (npcId == 12621 // wyvern
		) || (npcId == 16037 // Great Snow Wolf
		) || (npcId == 16041 // Fenrir Wolf
		) || (npcId == 16042 // White Fenrir Wolf
		) || (npcId == 16038 // Red Wind Strider
		) || (npcId == 16039 // Red Star Strider
		) || (npcId == 16040 // Red Twilight Strider
		) || (npcId == 16068); // Guardian Strider
	}
	
	/**
	 * Gets the single instance of PetDataTable.
	 * @return this class unique instance.
	 */
	public static PetDataTable getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final PetDataTable _instance = new PetDataTable();
	}
}