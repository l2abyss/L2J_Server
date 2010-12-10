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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.l2jserver.Config;
import com.l2jserver.gameserver.model.L2PetData;
import com.l2jserver.gameserver.model.L2PetLevelData;
import com.l2jserver.gameserver.model.actor.instance.L2PetInstance;
import com.l2jserver.gameserver.templates.item.L2EtcItemType;
import com.l2jserver.gameserver.templates.item.L2Item;

public class PetDataTable
{
	private static Logger _log = Logger.getLogger(L2PetInstance.class.getName());
	
	private static TIntObjectHashMap<L2PetData> _petTable;

	public static PetDataTable getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private PetDataTable()
	{
		_petTable = new TIntObjectHashMap<L2PetData>();
		load();
	}
	
	public void load()
	{
		_petTable.clear();
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setIgnoringComments(true);
		File file = new File(Config.DATAPACK_ROOT, "data/PetData.xml");
		Document doc = null;
		if (file.exists())
		{
			try
			{
				doc = factory.newDocumentBuilder().parse(file);
			}
			catch (Exception e)
			{
				_log.log(Level.WARNING, "Could not parse PetData.xml file: " + e.getMessage(), e);
			}
			
			Node n = doc.getFirstChild();
			for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
			{
				if (d.getNodeName().equals("pet"))
				{
					int npcId = Integer.parseInt(d.getAttributes().getNamedItem("id").getNodeValue());
					//index ignored for now
					L2PetData data = new L2PetData();
					for (Node p = d.getFirstChild(); p != null; p = p.getNextSibling())
					{
						if (p.getNodeName().equals("set"))
						{
							NamedNodeMap attrs = p.getAttributes();
							String type = attrs.getNamedItem("name").getNodeValue();
							if ("food".equals(type))
							{
								String[] values = attrs.getNamedItem("val").getNodeValue().split(";");
								int[] food = new int[values.length];
								for (int i = 0; i < values.length; i++)
								{
									food[i] = Integer.parseInt(values[i]);
								}
								data.set_food(food);
							}
							else if ("load".equals(type))
							{
								data.set_load(Integer.parseInt(attrs.getNamedItem("val").getNodeValue()));
							}
							else if ("hungry_limit".equals(type))
							{
								data.set_hungry_limit(Integer.parseInt(attrs.getNamedItem("val").getNodeValue()));
							}
							//sync_level and evolve ignored 
						}
						else if (p.getNodeName().equals("skills"))
						{
							for (Node s = p.getFirstChild(); s != null; s = s.getNextSibling())
							{
								if (s.getNodeName().equals("skill"))
								{
									NamedNodeMap attrs = s.getAttributes();
									int skillId = Integer.parseInt(attrs.getNamedItem("skillId").getNodeValue());
									int skillLvl = Integer.parseInt(attrs.getNamedItem("skillLvl").getNodeValue());
									int minLvl = Integer.parseInt(attrs.getNamedItem("minLvl").getNodeValue());
									data.addNewSkill(skillId, skillLvl, minLvl);
								}
							}
						}
						else if (p.getNodeName().equals("stats"))
						{
							for (Node s = p.getFirstChild(); s != null; s = s.getNextSibling())
							{
								if (s.getNodeName().equals("stat"))
								{
									int level = Integer.parseInt(s.getAttributes().getNamedItem("level").getNodeValue());
									L2PetLevelData stat = new L2PetLevelData();
									for (Node bean = s.getFirstChild(); bean != null; bean = bean.getNextSibling())
									{
										if (bean.getNodeName().equals("set"))
										{
											NamedNodeMap attrs = bean.getAttributes();
											String type = attrs.getNamedItem("name").getNodeValue();
											String value = attrs.getNamedItem("val").getNodeValue();
											if ("exp".equals(type))
											{
												stat.setPetMaxExp(Long.parseLong(value));
											}
											else if ("get_exp_type".equals(type))
											{
												stat.setOwnerExpTaken(Integer.parseInt(value));
											}
											else if ("consume_meal_in_battle".equals(type))
											{
												stat.setPetFeedBattle(Integer.parseInt(value));
											}
											else if ("consume_meal_in_normal".equals(type))
											{
												stat.setPetFeedNormal(Integer.parseInt(value));
											}
											else if ("max_meal".equals(type))
											{
												stat.setPetMaxFeed(Integer.parseInt(value));
											}
											else if ("soulshot_count".equals(type))
											{
												stat.setPetSoulShot((short) Integer.parseInt(value));
											}
											else if ("spiritshot_count".equals(type))
											{
												stat.setPetSpiritShot((short) Integer.parseInt(value));
											}
											else if ("hp".equals(type))
											{
												stat.setPetMaxHP(Integer.parseInt(value));
											}
											else if ("mp".equals(type))
											{
												stat.setPetMaxMP(Integer.parseInt(value));
											}
											else if ("pdef".equals(type))
											{
												stat.setPetPDef(Integer.parseInt(value));
											}
											else if ("mdef".equals(type))
											{
												stat.setPetMDef(Integer.parseInt(value));
											}
											else if ("patk".equals(type))
											{
												stat.setPetPAtk(Integer.parseInt(value));
											}
											else if ("matk".equals(type))
											{
												stat.setPetMAtk(Integer.parseInt(value));
											}
											else if ("hpreg".equals(type))
											{
												stat.setPetRegenHP(Integer.parseInt(value));
											}
											else if ("mpreg".equals(type))
											{
												stat.setPetRegenMP(Integer.parseInt(value));
											}
										}
									}
									data.addNewStat(stat, level);
								}
							}
						}
					}
					_petTable.put(npcId, data);
				}
			}
		}
		else
			_log.warning("Not found PetData.xml");
		
		_log.info(getClass().getSimpleName()+": Loaded " + _petTable.size() + " Pets.");
		
	}
	
	public L2PetLevelData getPetLevelData(int petID, int petLevel)
	{
		return _petTable.get(petID).getPetLevelData(petLevel);
	}
	
	public L2PetData getPetData(int petID)
	{
		if (!_petTable.contains(petID))
			_log.info("Missing pet data for npcid: "+petID);
		return _petTable.get(petID);
	}
	

	public int getPetMinLevel(int petID)
	{
		return _petTable.get(petID).getMinLevel();
	}

	/*
	 * Pets stuffs
	 */
	public static boolean isWolf(int npcId)
	{
		return npcId == 12077;
	}
	
	public static boolean isEvolvedWolf(int npcId)
	{
		return npcId == 16030 || npcId == 16037 || npcId == 16025 || npcId == 16041 || npcId == 16042;
	}
	
	public static boolean isSinEater(int npcId)
	{
		return npcId == 12564;
	}
	
	public static boolean isHatchling(int npcId)
	{
		return npcId > 12310 && npcId < 12314;
	}
	
	public static boolean isStrider(int npcId)
	{
		return (npcId > 12525 && npcId < 12529) || (npcId > 16037 && npcId < 16041) || npcId == 16068;
	}
	
	public static boolean isWyvern(int npcId)
	{
		return npcId == 12621;
	}
	
	public static boolean isBaby(int npcId)
	{
		return npcId > 12779 && npcId < 12783;
	}
	
	public static boolean isImprovedBaby(int npcId)
	{
		return npcId > 16033 && npcId < 16037;
	}
	
	public static boolean isPetFood(int itemId)
	{
		switch (itemId)
		{
			case 2515:
			case 4038:
			case 5168:
			case 5169:
			case 6316:
			case 7582:
			case 9668:
			case 10425:
				return true;
			default:
				return false;
		}
	}
	
	/**
	 * @see L2PetData#getFood()
	 * 
	 */
	@Deprecated
	public static int[] getFoodItemId(int npcId)
	{
		switch (npcId)
		{
			case 12077:// Wolf
			case 12564://Sin Eater
				return new int[] { 2515 };
				
			case 16030:// Great Wolf
			case 16025:// Black Wolf
			case 16037:// White Great Wolf
			case 16041:// Fenrir
			case 16042:// White Fenrir
				return new int[] { 9668 };
				
			case 12311:// hatchling of wind
			case 12312:// hatchling of star
			case 12313:// hatchling of twilight
				return new int[] { 4038 };
				
			case 12526:// wind strider
			case 12527:// Star strider
			case 12528:// Twilight strider
			case 16038:// red wind strider
			case 16039:// red Star strider
			case 16040:// red Twilight strider
			case 16068:// Guardian Strider
				return new int[] { 5168, 5169 };
				
			case 12621: // wyvern
				return new int[] { 6316 };
				
			case 12780:// Baby Buffalo
			case 12782:// Baby Cougar
			case 12781:// Baby Kookaburra
				return new int[] { 7582 };
				
			case 16034:// Improved Baby Buffalo
			case 16036:// Improved Baby Cougar
			case 16035:// Improved Baby Kookaburra
				return new int[] { 10425 };
				
			default:
				return new int[] { 0 };
		}
	}
	
	public static boolean isPetItem(int itemId)
	{
		L2Item item = ItemTable.getInstance().getTemplate(itemId);
		if (item != null && item.getItemType() == L2EtcItemType.PET_COLLAR)
			return true;
		return false;
		
		/*switch (itemId)
		{
			case 2375: // Wolf
			case 3500: // hatchling of wind
			case 3501: // hatchling of star
			case 3502: // hatchling of twilight
			case 4422: // strider of wind
			case 4423: // strider of star
			case 4424: // strider of dusk
			case 4425: // Sin Eater
			case 6648: // baby buffalo
			case 6649: // baby cougar
			case 6650: // baby kookaburra
			case 8663: // Wyvern
			case 9882: // Great Wolf
			case 10163: // Black Wolf
			case 10307: // Great Snow Wolf
			case 10308: // red strider of wind
			case 10309: // red strider of star
			case 10310: // red strider of dusk
			case 10311: // improved buffalo
			case 10312: // improved cougar
			case 10313: // improved kookaburra
			case 10426: // Fenrir
			case 10611: // White Fenrir
			case 14819: // Guardian Strider
				return true;
			default:
				return false;
		}*/
	}
	
	public static int[] getPetItemsByNpc(int npcId)
	{
		switch (npcId)
		{
			case 12077:// Wolf
				return new int[] { 2375 };
			case 16025:// Great Wolf
				return new int[] { 9882 };
			case 16030:// Black Wolf
				return new int[] { 10163 };
			case 16037:// White Great Wolf
				return new int[] { 10307 };
			case 16041:// Fenrir
				return new int[] { 10426 };
			case 16042:// White Fenrir
				return new int[] { 10611 };
			case 12564://Sin Eater
				return new int[] { 4425 };
				
			case 12311:// hatchling of wind
			case 12312:// hatchling of star
			case 12313:// hatchling of twilight
				return new int[] { 3500, 3501, 3502 };
				
			case 12526:// wind strider
			case 12527:// Star strider
			case 12528:// Twilight strider
			case 16038: // red strider of wind
			case 16039: // red strider of star
			case 16040: // red strider of dusk
			case 16068: // Guardian Strider
				return new int[] { 4422, 4423, 4424, 10308, 10309, 10310 , 14819};
				
			case 12621:// Wyvern
				return new int[] { 8663 };
				
			case 12780:// Baby Buffalo
			case 12782:// Baby Cougar
			case 12781:// Baby Kookaburra
				return new int[] { 6648, 6649, 6650 };
				
			case 16034:// Improved Baby Buffalo
			case 16036:// Improved Baby Cougar
			case 16035:// Improved Baby Kookaburra
				return new int[] { 10311, 10312, 10313 };
				
				// unknown item id.. should never happen
			default:
				return new int[] { 0 };
		}
	}
	
	public static boolean isMountable(int npcId)
	{
		return npcId == 12526 // wind strider
		|| npcId == 12527 // star strider
		|| npcId == 12528 // twilight strider
		|| npcId == 12621 // wyvern
		|| npcId == 16037 // Great Snow Wolf
		|| npcId == 16041 // Fenrir Wolf
		|| npcId == 16042 // White Fenrir Wolf
		|| npcId == 16038 // Red Wind Strider
		|| npcId == 16039 // Red Star Strider
		|| npcId == 16040 // Red Twilight Strider
		|| npcId == 16068; // Guardian Strider
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final PetDataTable _instance = new PetDataTable();
	}
	
	public static void main(String... s)
	{
		getInstance();
	}
}