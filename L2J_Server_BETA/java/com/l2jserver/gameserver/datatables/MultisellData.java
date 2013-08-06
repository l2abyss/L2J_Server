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
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;

import org.w3c.dom.DOMException;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.l2jserver.Config;
import com.l2jserver.gameserver.engines.DocumentParser;
import com.l2jserver.gameserver.model.StatsSet;
import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.multisell.Entry;
import com.l2jserver.gameserver.model.multisell.Ingredient;
import com.l2jserver.gameserver.model.multisell.ListContainer;
import com.l2jserver.gameserver.model.multisell.PreparedListContainer;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.ExBrExtraUserInfo;
import com.l2jserver.gameserver.network.serverpackets.MultiSellList;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;
import com.l2jserver.gameserver.network.serverpackets.UserInfo;
import com.l2jserver.gameserver.util.Util;
import com.l2jserver.util.file.filter.NumericNameFilter;

public class MultisellData extends DocumentParser
{
	public static final int PAGE_SIZE = 40;
	
	public static final int PC_BANG_POINTS = -100;
	public static final int CLAN_REPUTATION = -200;
	public static final int FAME = -300;
	
	private final Map<Integer, ListContainer> _entries = new HashMap<>();
	
	protected MultisellData()
	{
		setCurrentFileFilter(new NumericNameFilter());
		load();
	}
	
	@Override
	public final void load()
	{
		_entries.clear();
		parseDirectory("data/multisell");
		if (Config.CUSTOM_MULTISELL_LOAD)
		{
			parseDirectory("data/multisell/custom");
		}
		
		verify();
		_log.log(Level.INFO, getClass().getSimpleName() + ": Loaded " + _entries.size() + " lists.");
	}
	
	@Override
	protected final void parseDocument()
	{
		try
		{
			int id = Integer.parseInt(getCurrentFile().getName().replaceAll(".xml", ""));
			int entryId = 1;
			Node att;
			final ListContainer list = new ListContainer(id);
			
			for (Node n = getCurrentDocument().getFirstChild(); n != null; n = n.getNextSibling())
			{
				if ("list".equalsIgnoreCase(n.getNodeName()))
				{
					att = n.getAttributes().getNamedItem("applyTaxes");
					list.setApplyTaxes((att != null) && Boolean.parseBoolean(att.getNodeValue()));
					
					att = n.getAttributes().getNamedItem("useRate");
					if (att != null)
					{
						try
						{
							
							list.setUseRate(Double.valueOf(att.getNodeValue()));
							if (list.getUseRate() <= 1e-6)
							{
								throw new NumberFormatException("The value cannot be 0"); // threat 0 as invalid value
							}
						}
						catch (NumberFormatException e)
						{
							
							try
							{
								list.setUseRate(Config.class.getField(att.getNodeValue()).getDouble(Config.class));
							}
							catch (Exception e1)
							{
								_log.warning(e1.getMessage() + getCurrentDocument().getLocalName());
								list.setUseRate(1.0);
							}
							
						}
						catch (DOMException e)
						{
							_log.warning(e.getMessage() + getCurrentDocument().getLocalName());
						}
					}
					
					att = n.getAttributes().getNamedItem("maintainEnchantment");
					list.setMaintainEnchantment((att != null) && Boolean.parseBoolean(att.getNodeValue()));
					
					for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					{
						if ("item".equalsIgnoreCase(d.getNodeName()))
						{
							Entry e = parseEntry(d, entryId++, list);
							list.getEntries().add(e);
						}
						else if ("npcs".equalsIgnoreCase(d.getNodeName()))
						{
							for (Node b = d.getFirstChild(); b != null; b = b.getNextSibling())
							{
								if ("npc".equalsIgnoreCase(b.getNodeName()))
								{
									if (Util.isDigit(b.getTextContent()))
									{
										list.allowNpc(Integer.parseInt(b.getTextContent()));
									}
								}
							}
						}
					}
				}
			}
			_entries.put(id, list);
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, getClass().getSimpleName() + ": Error in file " + getCurrentFile(), e);
		}
	}
	
	private final Entry parseEntry(Node n, int entryId, ListContainer list)
	{
		Node first = n.getFirstChild();
		final Entry entry = new Entry(entryId);
		
		NamedNodeMap attrs;
		Node att;
		StatsSet set;
		
		for (n = first; n != null; n = n.getNextSibling())
		{
			if ("ingredient".equalsIgnoreCase(n.getNodeName()))
			{
				attrs = n.getAttributes();
				set = new StatsSet();
				for (int i = 0; i < attrs.getLength(); i++)
				{
					att = attrs.item(i);
					set.set(att.getNodeName(), att.getNodeValue());
				}
				entry.addIngredient(new Ingredient(set));
			}
			else if ("production".equalsIgnoreCase(n.getNodeName()))
			{
				attrs = n.getAttributes();
				set = new StatsSet();
				for (int i = 0; i < attrs.getLength(); i++)
				{
					att = attrs.item(i);
					set.set(att.getNodeName(), att.getNodeValue());
				}
				entry.addProduct(new Ingredient(set));
			}
		}
		
		return entry;
	}
	
	/**
	 * This will generate the multisell list for the items.<br>
	 * There exist various parameters in multisells that affect the way they will appear:
	 * <ol>
	 * <li>Inventory only:
	 * <ul>
	 * <li>If true, only show items of the multisell for which the "primary" ingredients are already in the player's inventory. By "primary" ingredients we mean weapon and armor.</li>
	 * <li>If false, show the entire list.</li>
	 * </ul>
	 * </li>
	 * <li>Maintain enchantment: presumably, only lists with "inventory only" set to true should sometimes have this as true. This makes no sense otherwise...
	 * <ul>
	 * <li>If true, then the product will match the enchantment level of the ingredient.<br>
	 * If the player has multiple items that match the ingredient list but the enchantment levels differ, then the entries need to be duplicated to show the products and ingredients for each enchantment level.<br>
	 * For example: If the player has a crystal staff +1 and a crystal staff +3 and goes to exchange it at the mammon, the list should have all exchange possibilities for the +1 staff, followed by all possibilities for the +3 staff.</li>
	 * <li>If false, then any level ingredient will be considered equal and product will always be at +0</li>
	 * </ul>
	 * </li>
	 * <li>Apply taxes: Uses the "taxIngredient" entry in order to add a certain amount of adena to the ingredients.
	 * <li>
	 * <li>Additional product and ingredient multipliers.</li>
	 * </ol>
	 * @param listId
	 * @param player
	 * @param npc
	 * @param inventoryOnly
	 * @param productMultiplier
	 * @param ingredientMultiplier
	 */
	public final void separateAndSend(int listId, L2PcInstance player, L2Npc npc, boolean inventoryOnly, double productMultiplier, double ingredientMultiplier)
	{
		ListContainer template = _entries.get(listId);
		if (template == null)
		{
			_log.warning(getClass().getSimpleName() + ": can't find list id: " + listId + " requested by player: " + player.getName() + ", npcId:" + (npc != null ? npc.getId() : 0));
			return;
		}
		
		if (((npc != null) && !template.isNpcAllowed(npc.getId())) || ((npc == null) && template.isNpcOnly()))
		{
			_log.warning(getClass().getSimpleName() + ": player " + player + " attempted to open multisell " + listId + " from npc " + npc + " which is not allowed!");
			return;
		}
		
		final PreparedListContainer list = new PreparedListContainer(template, inventoryOnly, player, npc);
		
		// Pass through this only when multipliers are different from 1
		if ((productMultiplier != 1) || (ingredientMultiplier != 1))
		{
			for (Entry entry : list.getEntries())
			{
				for (Ingredient product : entry.getProducts())
				{
					// Math.max used here to avoid dropping count to 0
					product.setItemCount((long) Math.max(product.getItemCount() * productMultiplier, 1));
				}
				for (Ingredient ingredient : entry.getIngredients())
				{
					// Math.max used here to avoid dropping count to 0
					ingredient.setItemCount((long) Math.max(ingredient.getItemCount() * ingredientMultiplier, 1));
				}
			}
		}
		int index = 0;
		do
		{
			// send list at least once even if size = 0
			player.sendPacket(new MultiSellList(list, index));
			index += PAGE_SIZE;
		}
		while (index < list.getEntries().size());
		
		player.setMultiSell(list);
	}
	
	public final void separateAndSend(int listId, L2PcInstance player, L2Npc npc, boolean inventoryOnly)
	{
		separateAndSend(listId, player, npc, inventoryOnly, 1, 1);
	}
	
	public static final boolean checkSpecialIngredient(int id, long amount, L2PcInstance player)
	{
		switch (id)
		{
			case CLAN_REPUTATION:
				if (player.getClan() == null)
				{
					player.sendPacket(SystemMessageId.YOU_ARE_NOT_A_CLAN_MEMBER);
					break;
				}
				if (!player.isClanLeader())
				{
					player.sendPacket(SystemMessageId.ONLY_THE_CLAN_LEADER_IS_ENABLED);
					break;
				}
				if (player.getClan().getReputationScore() < amount)
				{
					player.sendPacket(SystemMessageId.THE_CLAN_REPUTATION_SCORE_IS_TOO_LOW);
					break;
				}
				return true;
			case FAME:
				if (player.getFame() < amount)
				{
					player.sendPacket(SystemMessageId.NOT_ENOUGH_FAME_POINTS);
					break;
				}
				return true;
		}
		return false;
	}
	
	public static final boolean getSpecialIngredient(int id, long amount, L2PcInstance player)
	{
		switch (id)
		{
			case CLAN_REPUTATION:
				player.getClan().takeReputationScore((int) amount, true);
				SystemMessage smsg = SystemMessage.getSystemMessage(SystemMessageId.S1_DEDUCTED_FROM_CLAN_REP);
				smsg.addItemNumber(amount);
				player.sendPacket(smsg);
				return true;
			case FAME:
				player.setFame(player.getFame() - (int) amount);
				player.sendPacket(new UserInfo(player));
				player.sendPacket(new ExBrExtraUserInfo(player));
				return true;
		}
		return false;
	}
	
	public static final void addSpecialProduct(int id, long amount, L2PcInstance player)
	{
		switch (id)
		{
			case CLAN_REPUTATION:
				player.getClan().addReputationScore((int) amount, true);
				break;
			case FAME:
				player.setFame((int) (player.getFame() + amount));
				player.sendPacket(new UserInfo(player));
				player.sendPacket(new ExBrExtraUserInfo(player));
				break;
		}
	}
	
	private final void verify()
	{
		ListContainer list;
		final Iterator<ListContainer> iter = _entries.values().iterator();
		while (iter.hasNext())
		{
			list = iter.next();
			
			for (Entry ent : list.getEntries())
			{
				for (Ingredient ing : ent.getIngredients())
				{
					if (!verifyIngredient(ing))
					{
						_log.warning(getClass().getSimpleName() + ": can't find ingredient with itemId: " + ing.getItemId() + " in list: " + list.getListId());
					}
				}
				for (Ingredient ing : ent.getProducts())
				{
					if (!verifyIngredient(ing))
					{
						_log.warning(getClass().getSimpleName() + ": can't find product with itemId: " + ing.getItemId() + " in list: " + list.getListId());
					}
				}
			}
		}
	}
	
	private final boolean verifyIngredient(Ingredient ing)
	{
		switch (ing.getItemId())
		{
			case CLAN_REPUTATION:
			case FAME:
				return true;
			default:
				return ing.getTemplate() != null;
		}
	}
	
	public static MultisellData getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final MultisellData _instance = new MultisellData();
	}
}
