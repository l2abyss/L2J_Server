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
package com.l2jserver.gameserver.datatables;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.l2jserver.Config;
import com.l2jserver.gameserver.engines.DocumentParser;
import com.l2jserver.gameserver.enums.AISkillScope;
import com.l2jserver.gameserver.model.StatsSet;
import com.l2jserver.gameserver.model.actor.templates.L2NpcTemplate;
import com.l2jserver.gameserver.model.base.ClassId;
import com.l2jserver.gameserver.model.drops.DropListScope;
import com.l2jserver.gameserver.model.drops.GeneralDropItem;
import com.l2jserver.gameserver.model.drops.GroupedGeneralDropItem;
import com.l2jserver.gameserver.model.drops.IDropItem;
import com.l2jserver.gameserver.model.effects.L2EffectType;
import com.l2jserver.gameserver.model.holders.MinionHolder;
import com.l2jserver.gameserver.model.holders.SkillHolder;
import com.l2jserver.gameserver.model.skills.Skill;
import com.l2jserver.gameserver.util.Util;

/**
 * NPC data parser.
 * @author Nos
 */
public class NpcData extends DocumentParser
{
	private final Map<Integer, L2NpcTemplate> _npcs = new ConcurrentHashMap<>();
	private final Map<String, Integer> _clans = new ConcurrentHashMap<>();
	private MinionData _minionData;
	
	protected NpcData()
	{
		load();
	}
	
	@Override
	public synchronized void load()
	{
		_minionData = new MinionData();
		
		parseDatapackDirectory("data/stats/npcs", false);
		_log.info(getClass().getSimpleName() + ": Loaded " + _npcs.size() + " NPCs.");
		
		if (Config.CUSTOM_NPC_DATA)
		{
			final int npcCount = _npcs.size();
			parseDatapackDirectory("data/stats/npcs/custom", true);
			_log.info(getClass().getSimpleName() + ": Loaded " + (_npcs.size() - npcCount) + " Custom NPCs.");
		}
		
		_minionData = null;
		loadNpcsSkillLearn();
	}
	
	@Override
	protected void parseDocument()
	{
		for (Node node = getCurrentDocument().getFirstChild(); node != null; node = node.getNextSibling())
		{
			if ("list".equalsIgnoreCase(node.getNodeName()))
			{
				for (Node list_node = node.getFirstChild(); list_node != null; list_node = list_node.getNextSibling())
				{
					if ("npc".equalsIgnoreCase(list_node.getNodeName()))
					{
						NamedNodeMap attrs = list_node.getAttributes();
						final StatsSet set = new StatsSet();
						final int npcId = parseInteger(attrs, "id");
						Map<String, Object> parameters = null;
						Map<Integer, Skill> skills = null;
						Set<Integer> clans = null;
						Set<Integer> ignoreClanNpcIds = null;
						Map<DropListScope, List<IDropItem>> dropLists = null;
						set.set("id", npcId);
						set.set("displayId", parseInteger(attrs, "displayId"));
						set.set("level", parseByte(attrs, "level"));
						set.set("type", parseString(attrs, "type"));
						set.set("name", parseString(attrs, "name"));
						set.set("usingServerSideName", parseBoolean(attrs, "usingServerSideName"));
						set.set("title", parseString(attrs, "title"));
						set.set("usingServerSideTitle", parseBoolean(attrs, "usingServerSideTitle"));
						for (Node npc_node = list_node.getFirstChild(); npc_node != null; npc_node = npc_node.getNextSibling())
						{
							attrs = npc_node.getAttributes();
							switch (npc_node.getNodeName().toLowerCase())
							{
								case "parameters":
								{
									if (parameters == null)
									{
										parameters = new HashMap<>();
									}
									
									for (Node parameters_node = npc_node.getFirstChild(); parameters_node != null; parameters_node = parameters_node.getNextSibling())
									{
										attrs = parameters_node.getAttributes();
										switch (parameters_node.getNodeName().toLowerCase())
										{
											case "param":
											{
												parameters.put(parseString(attrs, "name"), parseString(attrs, "value"));
												break;
											}
											case "skill":
											{
												parameters.put(parseString(attrs, "name"), new SkillHolder(parseInteger(attrs, "id"), parseInteger(attrs, "level")));
												break;
											}
											case "minions":
											{
												final List<MinionHolder> minions = new ArrayList<>(1);
												for (Node minions_node = parameters_node.getFirstChild(); minions_node != null; minions_node = minions_node.getNextSibling())
												{
													if (minions_node.getNodeName().equalsIgnoreCase("npc"))
													{
														attrs = minions_node.getAttributes();
														minions.add(new MinionHolder(parseInteger(attrs, "id"), parseInteger(attrs, "count"), parseInteger(attrs, "respawnTime"), parseInteger(attrs, "weightPoint")));
													}
												}
												
												if (!minions.isEmpty())
												{
													parameters.put(parseString(parameters_node.getAttributes(), "name"), minions);
												}
												
												break;
											}
										}
									}
									break;
								}
								case "race":
								case "sex":
									set.set(npc_node.getNodeName(), npc_node.getTextContent().toUpperCase());
									break;
								case "equipment":
								{
									set.set("chestId", parseInteger(attrs, "chest"));
									set.set("rhandId", parseInteger(attrs, "rhand"));
									set.set("lhandId", parseInteger(attrs, "lhand"));
									set.set("weaponEnchant", parseInteger(attrs, "weaponEnchant"));
									break;
								}
								case "acquire":
								{
									set.set("expRate", parseDouble(attrs, "expRate"));
									set.set("sp", parseDouble(attrs, "sp"));
									set.set("raidPoints", parseDouble(attrs, "raidPoints"));
									break;
								}
								case "stats":
								{
									set.set("baseSTR", parseInteger(attrs, "str"));
									set.set("baseINT", parseInteger(attrs, "int"));
									set.set("baseDEX", parseInteger(attrs, "dex"));
									set.set("baseWIT", parseInteger(attrs, "wit"));
									set.set("baseCON", parseInteger(attrs, "con"));
									set.set("baseMEN", parseInteger(attrs, "men"));
									for (Node stats_node = npc_node.getFirstChild(); stats_node != null; stats_node = stats_node.getNextSibling())
									{
										attrs = stats_node.getAttributes();
										switch (stats_node.getNodeName().toLowerCase())
										{
											case "vitals":
											{
												set.set("baseHpMax", parseDouble(attrs, "hp"));
												set.set("baseHpReg", parseDouble(attrs, "hpRegen"));
												set.set("baseMpMax", parseDouble(attrs, "mp"));
												set.set("baseMpReg", parseDouble(attrs, "mpRegen"));
												break;
											}
											case "attack":
											{
												set.set("basePAtk", parseDouble(attrs, "physical"));
												set.set("baseMAtk", parseDouble(attrs, "magical"));
												set.set("baseRndDam", parseInteger(attrs, "random"));
												set.set("baseCritRate", parseInteger(attrs, "critical"));
												set.set("accuracy", parseDouble(attrs, "accuracy"));// TODO: Implement me
												set.set("basePAtkSpd", parseInteger(attrs, "attackSpeed"));
												set.set("reuseDelay", parseInteger(attrs, "reuseDelay"));// TODO: Implement me
												set.set("baseAtkType", parseString(attrs, "type"));
												set.set("baseAtkRange", parseInteger(attrs, "range"));
												set.set("distance", parseInteger(attrs, "distance"));// TODO: Implement me
												set.set("width", parseInteger(attrs, "width"));// TODO: Implement me
												break;
											}
											case "defence":
											{
												set.set("basePDef", parseDouble(attrs, "physical"));
												set.set("baseMDef", parseDouble(attrs, "magical"));
												set.set("evasion", parseInteger(attrs, "evasion"));// TODO: Implement me
												set.set("baseShldDef", parseInteger(attrs, "shield"));
												set.set("baseShldRate", parseInteger(attrs, "shieldRate"));
												break;
											}
											case "attribute":
											{
												for (Node attribute_node = stats_node.getFirstChild(); attribute_node != null; attribute_node = attribute_node.getNextSibling())
												{
													attrs = attribute_node.getAttributes();
													switch (attribute_node.getNodeName().toLowerCase())
													{
														case "attack":
														{
															String attackAttributeType = parseString(attrs, "type");
															switch (attackAttributeType.toUpperCase())
															{
																case "FIRE":
																	set.set("baseFire", parseInteger(attrs, "value"));
																	break;
																case "WATER":
																	set.set("baseWater", parseInteger(attrs, "value"));
																	break;
																case "WIND":
																	set.set("baseWind", parseInteger(attrs, "value"));
																	break;
																case "EARTH":
																	set.set("baseEarth", parseInteger(attrs, "value"));
																	break;
																case "DARK":
																	set.set("baseDark", parseInteger(attrs, "value"));
																	break;
																case "HOLY":
																	set.set("baseHoly", parseInteger(attrs, "value"));
																	break;
															}
															break;
														}
														case "defence":
														{
															set.set("baseFireRes", parseInteger(attrs, "fire"));
															set.set("baseWaterRes", parseInteger(attrs, "water"));
															set.set("baseWindRes", parseInteger(attrs, "wind"));
															set.set("baseEarthRes", parseInteger(attrs, "earth"));
															set.set("baseHolyRes", parseInteger(attrs, "holy"));
															set.set("baseDarkRes", parseInteger(attrs, "dark"));
															set.set("baseElementRes", parseInteger(attrs, "default"));
															break;
														}
													}
												}
												break;
											}
											case "speed":
											{
												for (Node speed_node = stats_node.getFirstChild(); speed_node != null; speed_node = speed_node.getNextSibling())
												{
													attrs = speed_node.getAttributes();
													switch (speed_node.getNodeName().toLowerCase())
													{
														case "walk":
														{
															set.set("baseWalkSpd", parseDouble(attrs, "ground"));
															set.set("baseSwimWalkSpd", parseDouble(attrs, "swim"));
															set.set("baseFlyWalkSpd", parseDouble(attrs, "fly"));
															break;
														}
														case "run":
														{
															set.set("baseRunSpd", parseDouble(attrs, "ground"));
															set.set("baseSwimRunSpd", parseDouble(attrs, "swim"));
															set.set("baseFlyRunSpd", parseDouble(attrs, "fly"));
															break;
														}
													}
												}
												break;
											}
											case "hit_time":
												set.set("hit_time", npc_node.getTextContent());// TODO: Implement me default 600 (value in ms)
												break;
										}
									}
									break;
								}
								case "status":
								{
									set.set("unique", parseBoolean(attrs, "unique"));
									set.set("attackable", parseBoolean(attrs, "attackable"));
									set.set("targetable", parseBoolean(attrs, "targetable"));
									set.set("undying", parseBoolean(attrs, "undying"));
									set.set("showName", parseBoolean(attrs, "showName"));
									set.set("flying", parseBoolean(attrs, "flying"));
									set.set("canMove", parseBoolean(attrs, "canMove"));
									set.set("noSleepMode", parseBoolean(attrs, "noSleepMode"));
									set.set("passableDoor", parseBoolean(attrs, "passableDoor"));
									set.set("hasSummoner", parseBoolean(attrs, "hasSummoner"));
									set.set("canBeSown", parseBoolean(attrs, "canBeSown"));
									break;
								}
								case "skill_list":
								{
									skills = new HashMap<>();
									for (Node skill_list_node = npc_node.getFirstChild(); skill_list_node != null; skill_list_node = skill_list_node.getNextSibling())
									{
										if ("skill".equalsIgnoreCase(skill_list_node.getNodeName()))
										{
											attrs = skill_list_node.getAttributes();
											final int skillId = parseInteger(attrs, "id");
											final int skillLevel = parseInteger(attrs, "level");
											final Skill skill = SkillData.getInstance().getSkill(skillId, skillLevel);
											if (skill != null)
											{
												skills.put(skill.getId(), skill);
											}
											else
											{
												_log.warning("[" + getCurrentFile().getName() + "] skill not found. NPC ID: " + npcId + " Skill ID:" + skillId + " Skill Level: " + skillLevel);
											}
										}
									}
									break;
								}
								case "shots":
								{
									set.set("soulShot", parseInteger(attrs, "soul"));
									set.set("spiritShot", parseInteger(attrs, "spirit"));
									set.set("shotShotChance", parseInteger(attrs, "shotChance"));
									set.set("spiritShotChance", parseInteger(attrs, "spiritChance"));
									break;
								}
								case "corpse_time":
									set.set("corpseTime", npc_node.getTextContent());
									break;
								case "ex_crt_effect":
									set.set("ex_crt_effect", npc_node.getTextContent()); // TODO: Implement me default ? type boolean
									break;
								case "s_npc_prop_hp_rate":
									set.set("s_npc_prop_hp_rate", npc_node.getTextContent()); // TODO: Implement me default 1 type double
									break;
								case "ai":
								{
									set.set("aiType", parseString(attrs, "type"));
									set.set("aggroRange", parseInteger(attrs, "aggroRange"));
									set.set("clanHelpRange", parseInteger(attrs, "clanHelpRange"));
									set.set("dodge", parseInteger(attrs, "dodge"));
									set.set("isChaos", parseBoolean(attrs, "isChaos"));
									set.set("isAggressive", parseBoolean(attrs, "isAggressive"));
									for (Node ai_node = npc_node.getFirstChild(); ai_node != null; ai_node = ai_node.getNextSibling())
									{
										attrs = ai_node.getAttributes();
										switch (ai_node.getNodeName().toLowerCase())
										{
											case "skill":
											{
												set.set("minSkillChance", parseInteger(attrs, "minChance"));
												set.set("maxSkillChance", parseInteger(attrs, "maxChance"));
												set.set("primarySkillId", parseInteger(attrs, "primaryId"));
												set.set("shortRangeSkillId", parseInteger(attrs, "shortRangeId"));
												set.set("shortRangeSkillChance", parseInteger(attrs, "shortRangeChance"));
												set.set("longRangeSkillId", parseInteger(attrs, "longRangeId"));
												set.set("longRangeSkillChance", parseInteger(attrs, "longRangeChance"));
												break;
											}
											case "clan_list":
											{
												for (Node clan_list_node = ai_node.getFirstChild(); clan_list_node != null; clan_list_node = clan_list_node.getNextSibling())
												{
													attrs = clan_list_node.getAttributes();
													switch (clan_list_node.getNodeName().toLowerCase())
													{
														case "clan":
														{
															if (clans == null)
															{
																clans = new HashSet<>(1);
															}
															clans.add(getOrCreateClanId(clan_list_node.getTextContent()));
															break;
														}
														case "ignore_npc_id":
														{
															if (ignoreClanNpcIds == null)
															{
																ignoreClanNpcIds = new HashSet<>(1);
															}
															ignoreClanNpcIds.add(Integer.parseInt(clan_list_node.getTextContent()));
															break;
														}
													}
												}
												break;
											}
										}
									}
									break;
								}
								case "drop_lists":
								{
									for (Node drop_lists_node = npc_node.getFirstChild(); drop_lists_node != null; drop_lists_node = drop_lists_node.getNextSibling())
									{
										DropListScope dropListScope = null;
										
										try
										{
											dropListScope = Enum.valueOf(DropListScope.class, drop_lists_node.getNodeName().toUpperCase());
										}
										catch (Exception e)
										{
										}
										
										if (dropListScope != null)
										{
											if (dropLists == null)
											{
												dropLists = new EnumMap<>(DropListScope.class);
											}
											
											List<IDropItem> dropList = new ArrayList<>();
											parseDropList(drop_lists_node, dropListScope, dropList);
											dropLists.put(dropListScope, Collections.unmodifiableList(dropList));
										}
									}
									break;
								}
								case "collision":
								{
									for (Node collision_node = npc_node.getFirstChild(); collision_node != null; collision_node = collision_node.getNextSibling())
									{
										attrs = collision_node.getAttributes();
										switch (collision_node.getNodeName().toLowerCase())
										{
											case "radius":
											{
												set.set("collision_radius", parseDouble(attrs, "normal"));
												set.set("collisionRadiusGrown", parseDouble(attrs, "grown"));
												break;
											}
											case "height":
											{
												set.set("collision_height", parseDouble(attrs, "normal"));
												set.set("collisionHeightGrown", parseDouble(attrs, "grown"));
												break;
											}
										}
									}
									break;
								}
							}
						}
						
						L2NpcTemplate template = _npcs.get(npcId);
						if (template == null)
						{
							template = new L2NpcTemplate(set);
							_npcs.put(template.getId(), template);
						}
						else
						{
							template.set(set);
						}
						
						if (_minionData._tempMinions.containsKey(npcId))
						{
							if (parameters == null)
							{
								parameters = new HashMap<>();
							}
							parameters.putIfAbsent("Privates", _minionData._tempMinions.get(npcId));
						}
						
						if (parameters != null)
						{
							// Using unmodifiable map parameters of template are not meant to be changed at runtime.
							template.setParameters(new StatsSet(Collections.unmodifiableMap(parameters)));
						}
						else
						{
							template.setParameters(StatsSet.EMPTY_STATSET);
						}
						
						if (skills != null)
						{
							Map<AISkillScope, List<Skill>> aiSkillLists = null;
							for (Skill skill : skills.values())
							{
								if (!skill.isPassive())
								{
									if (aiSkillLists == null)
									{
										aiSkillLists = new EnumMap<>(AISkillScope.class);
									}
									
									List<AISkillScope> aiSkillScopes = new ArrayList<>();
									final AISkillScope shortOrLongRangeScope = skill.getCastRange() <= 150 ? AISkillScope.SHORT_RANGE : AISkillScope.SHORT_RANGE;
									if (skill.isSuicideAttack())
									{
										aiSkillScopes.add(AISkillScope.SUICIDE);
									}
									else
									{
										aiSkillScopes.add(AISkillScope.GENERAL);
										
										if (skill.isContinuous())
										{
											if (!skill.isDebuff())
											{
												aiSkillScopes.add(AISkillScope.BUFF);
											}
											else
											{
												aiSkillScopes.add(AISkillScope.DEBUFF);
												aiSkillScopes.add(AISkillScope.COT);
												aiSkillScopes.add(shortOrLongRangeScope);
											}
										}
										else
										{
											if (skill.hasEffectType(L2EffectType.DISPEL, L2EffectType.DISPEL_BY_SLOT))
											{
												aiSkillScopes.add(AISkillScope.NEGATIVE);
												aiSkillScopes.add(shortOrLongRangeScope);
											}
											else if (skill.hasEffectType(L2EffectType.HEAL))
											{
												aiSkillScopes.add(AISkillScope.HEAL);
											}
											else if (skill.hasEffectType(L2EffectType.PHYSICAL_ATTACK, L2EffectType.PHYSICAL_ATTACK_HP_LINK, L2EffectType.MAGICAL_ATTACK, L2EffectType.DEATH_LINK, L2EffectType.HP_DRAIN))
											{
												aiSkillScopes.add(AISkillScope.ATTACK);
												aiSkillScopes.add(AISkillScope.UNIVERSAL);
												aiSkillScopes.add(shortOrLongRangeScope);
											}
											else if (skill.hasEffectType(L2EffectType.SLEEP))
											{
												aiSkillScopes.add(AISkillScope.IMMOBILIZE);
											}
											else if (skill.hasEffectType(L2EffectType.STUN, L2EffectType.ROOT))
											{
												aiSkillScopes.add(AISkillScope.IMMOBILIZE);
												aiSkillScopes.add(shortOrLongRangeScope);
											}
											else if (skill.hasEffectType(L2EffectType.MUTE, L2EffectType.FEAR))
											{
												aiSkillScopes.add(AISkillScope.COT);
												aiSkillScopes.add(shortOrLongRangeScope);
											}
											else if (skill.hasEffectType(L2EffectType.PARALYZE))
											{
												aiSkillScopes.add(AISkillScope.IMMOBILIZE);
												aiSkillScopes.add(shortOrLongRangeScope);
											}
											else if (skill.hasEffectType(L2EffectType.DMG_OVER_TIME, L2EffectType.DMG_OVER_TIME_PERCENT))
											{
												aiSkillScopes.add(shortOrLongRangeScope);
											}
											else if (skill.hasEffectType(L2EffectType.RESURRECTION))
											{
												aiSkillScopes.add(AISkillScope.RES);
											}
											else
											{
												aiSkillScopes.add(AISkillScope.UNIVERSAL);
											}
										}
									}
									
									for (AISkillScope aiSkillScope : aiSkillScopes)
									{
										List<Skill> aiSkills = aiSkillLists.get(aiSkillScope);
										if (aiSkills == null)
										{
											aiSkills = new ArrayList<>();
											aiSkillLists.put(aiSkillScope, aiSkills);
										}
										
										aiSkills.add(skill);
									}
								}
							}
							
							template.setSkills(skills);
							template.setAISkillLists(aiSkillLists);
						}
						else
						{
							template.setSkills(null);
							template.setAISkillLists(null);
						}
						
						template.setClans(clans);
						template.setIgnoreClanNpcIds(ignoreClanNpcIds);
						
						template.setDropLists(dropLists);
					}
				}
			}
		}
	}
	
	private void parseDropList(Node drop_list_node, DropListScope dropListScope, List<IDropItem> drops)
	{
		for (Node drop_node = drop_list_node.getFirstChild(); drop_node != null; drop_node = drop_node.getNextSibling())
		{
			NamedNodeMap attrs = drop_node.getAttributes();
			switch (drop_node.getNodeName().toLowerCase())
			{
				case "group":
				{
					GroupedGeneralDropItem dropItem = dropListScope.newGroupedDropItem(parseDouble(attrs, "chance"));
					List<IDropItem> groupedDropList = new ArrayList<>(2);
					for (Node group_node = drop_node.getFirstChild(); group_node != null; group_node = group_node.getNextSibling())
					{
						parseDropListItem(group_node, dropListScope, groupedDropList);
					}
					
					List<GeneralDropItem> items = new ArrayList<>(groupedDropList.size());
					for (IDropItem item : groupedDropList)
					{
						if (item instanceof GeneralDropItem)
						{
							items.add((GeneralDropItem) item);
						}
						else
						{
							_log.warning("[" + getCurrentFile() + "] grouped general drop item supports only general drop item.");
						}
					}
					dropItem.setItems(items);
					
					drops.add(dropItem);
					break;
				}
				default:
				{
					parseDropListItem(drop_node, dropListScope, drops);
					break;
				}
			}
		}
	}
	
	private void parseDropListItem(Node drop_list_item, DropListScope dropListScope, List<IDropItem> drops)
	{
		NamedNodeMap attrs = drop_list_item.getAttributes();
		switch (drop_list_item.getNodeName().toLowerCase())
		{
			case "item":
			{
				final IDropItem dropItem = dropListScope.newDropItem(parseInteger(attrs, "id"), parseLong(attrs, "min"), parseLong(attrs, "max"), parseDouble(attrs, "chance"));
				if (dropItem != null)
				{
					drops.add(dropItem);
				}
				break;
			}
		}
	}
	
	/**
	 * Gets or creates a clan id if it doesnt exists.
	 * @param clanName the clan name to get or create its id
	 * @return the clan id for the given clan name
	 */
	private int getOrCreateClanId(String clanName)
	{
		Integer id = _clans.get(clanName.toUpperCase());
		if (id == null)
		{
			id = _clans.size();
			_clans.put(clanName.toUpperCase(), id);
		}
		return id;
	}
	
	/**
	 * Gets the clan id
	 * @param clanName the clan name to get its id
	 * @return the clan id for the given clan name if it exists, -1 otherwise
	 */
	public int getClanId(String clanName)
	{
		Integer id = _clans.get(clanName.toUpperCase());
		return id != null ? id : -1;
	}
	
	/**
	 * Gets the template.
	 * @param id the template Id to get.
	 * @return the template for the given id.
	 */
	public L2NpcTemplate getTemplate(int id)
	{
		return _npcs.get(id);
	}
	
	/**
	 * Gets the template by name.
	 * @param name of the template to get.
	 * @return the template for the given name.
	 */
	public L2NpcTemplate getTemplateByName(String name)
	{
		for (L2NpcTemplate npcTemplate : _npcs.values())
		{
			if (npcTemplate.getName().equalsIgnoreCase(name))
			{
				return npcTemplate;
			}
		}
		return null;
	}
	
	/**
	 * Gets all templates matching the filter.
	 * @param filter
	 * @return the template list for the given filter
	 */
	public List<L2NpcTemplate> getTemplates(Predicate<L2NpcTemplate> filter)
	{
		//@formatter:off
			return _npcs.values().stream()
			.filter(filter)
			.collect(Collectors.toList());
		//@formatter:on
	}
	
	/**
	 * Gets the all of level.
	 * @param lvls of all the templates to get.
	 * @return the template list for the given level.
	 */
	public List<L2NpcTemplate> getAllOfLevel(int... lvls)
	{
		return getTemplates(template -> Util.contains(lvls, template.getLevel()));
	}
	
	/**
	 * Gets the all monsters of level.
	 * @param lvls of all the monster templates to get.
	 * @return the template list for the given level.
	 */
	public List<L2NpcTemplate> getAllMonstersOfLevel(int... lvls)
	{
		return getTemplates(template -> Util.contains(lvls, template.getLevel()) && template.isType("L2Monster"));
	}
	
	/**
	 * Gets the all npc starting with.
	 * @param text of all the NPC templates which its name start with.
	 * @return the template list for the given letter.
	 */
	public List<L2NpcTemplate> getAllNpcStartingWith(String text)
	{
		return getTemplates(template -> template.isType("L2Npc") && template.getName().startsWith(text));
	}
	
	/**
	 * Gets the all npc of class type.
	 * @param classTypes of all the templates to get.
	 * @return the template list for the given class type.
	 */
	public List<L2NpcTemplate> getAllNpcOfClassType(String... classTypes)
	{
		return getTemplates(template -> Util.contains(classTypes, template.getType(), true));
	}
	
	public void loadNpcsSkillLearn()
	{
		_npcs.values().forEach(template ->
		{
			final List<ClassId> teachInfo = SkillLearnData.getInstance().getSkillLearnData(template.getId());
			if (teachInfo != null)
			{
				template.addTeachInfo(teachInfo);
			}
		});
	}
	
	/**
	 * This class handles minions from Spawn System<br>
	 * Once Spawn System gets reworked delete this class<br>
	 * @author Zealar
	 */
	private final class MinionData extends DocumentParser
	{
		public final Map<Integer, List<MinionHolder>> _tempMinions = new HashMap<>();
		
		protected MinionData()
		{
			load();
		}
		
		@Override
		public void load()
		{
			_tempMinions.clear();
			parseDatapackFile("data/minionData.xml");
			_log.info(getClass().getSimpleName() + ": Loaded " + _tempMinions.size() + " minions data.");
		}
		
		@Override
		protected void parseDocument()
		{
			for (Node node = getCurrentDocument().getFirstChild(); node != null; node = node.getNextSibling())
			{
				if ("list".equals(node.getNodeName()))
				{
					for (Node list_node = node.getFirstChild(); list_node != null; list_node = list_node.getNextSibling())
					{
						if ("npc".equals(list_node.getNodeName()))
						{
							final List<MinionHolder> minions = new ArrayList<>(1);
							NamedNodeMap attrs = list_node.getAttributes();
							int id = parseInteger(attrs, "id");
							for (Node npc_node = list_node.getFirstChild(); npc_node != null; npc_node = npc_node.getNextSibling())
							{
								if ("minion".equals(npc_node.getNodeName()))
								{
									attrs = npc_node.getAttributes();
									minions.add(new MinionHolder(parseInteger(attrs, "id"), parseInteger(attrs, "count"), parseInteger(attrs, "respawnTime"), 0));
								}
							}
							_tempMinions.put(id, minions);
						}
					}
				}
			}
		}
	}
	
	/**
	 * Gets the single instance of NpcData.
	 * @return single instance of NpcData
	 */
	public static NpcData getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final NpcData _instance = new NpcData();
	}
}
