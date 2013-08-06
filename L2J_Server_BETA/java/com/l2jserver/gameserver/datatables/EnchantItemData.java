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
import com.l2jserver.gameserver.model.StatsSet;
import com.l2jserver.gameserver.model.enchant.EnchantItem;
import com.l2jserver.gameserver.model.enchant.EnchantScroll;
import com.l2jserver.gameserver.model.items.instance.L2ItemInstance;

/**
 * This class holds the Enchant Item information.
 * @author UnAfraid
 */
public class EnchantItemData extends DocumentParser
{
	public static final Map<Integer, EnchantScroll> _scrolls = new HashMap<>();
	public static final Map<Integer, EnchantItem> _supports = new HashMap<>();
	
	/**
	 * Instantiates a new enchant item data.
	 */
	public EnchantItemData()
	{
		load();
	}
	
	@Override
	public synchronized void load()
	{
		_scrolls.clear();
		_supports.clear();
		parseDatapackFile("data/enchantData.xml");
		_log.info(getClass().getSimpleName() + ": Loaded " + _scrolls.size() + " Enchant Scrolls.");
		_log.info(getClass().getSimpleName() + ": Loaded " + _supports.size() + " Support Items.");
	}
	
	@Override
	protected void parseDocument()
	{
		StatsSet set;
		Node att;
		for (Node n = getCurrentDocument().getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("enchant".equalsIgnoreCase(d.getNodeName()))
					{
						NamedNodeMap attrs = d.getAttributes();
						set = new StatsSet();
						for (int i = 0; i < attrs.getLength(); i++)
						{
							att = attrs.item(i);
							set.set(att.getNodeName(), att.getNodeValue());
						}
						
						final EnchantScroll item = new EnchantScroll(set);
						for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
						{
							if ("item".equalsIgnoreCase(cd.getNodeName()))
							{
								item.addItem(parseInteger(cd.getAttributes(), "id"));
							}
						}
						_scrolls.put(item.getId(), item);
					}
					else if ("support".equalsIgnoreCase(d.getNodeName()))
					{
						NamedNodeMap attrs = d.getAttributes();
						
						set = new StatsSet();
						for (int i = 0; i < attrs.getLength(); i++)
						{
							att = attrs.item(i);
							set.set(att.getNodeName(), att.getNodeValue());
						}
						
						final EnchantItem item = new EnchantItem(set);
						for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
						{
							if ("item".equalsIgnoreCase(cd.getNodeName()))
							{
								item.addItem(parseInteger(cd.getAttributes(), "id"));
							}
						}
						_supports.put(item.getId(), item);
					}
				}
			}
		}
	}
	
	/**
	 * Gets the enchant scroll.
	 * @param scroll the scroll
	 * @return enchant template for scroll
	 */
	public final EnchantScroll getEnchantScroll(L2ItemInstance scroll)
	{
		return _scrolls.get(scroll.getId());
	}
	
	/**
	 * Gets the support item.
	 * @param item the item
	 * @return enchant template for support item
	 */
	public final EnchantItem getSupportItem(L2ItemInstance item)
	{
		return _supports.get(item.getId());
	}
	
	/**
	 * Gets the single instance of EnchantItemData.
	 * @return single instance of EnchantItemData
	 */
	public static final EnchantItemData getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final EnchantItemData _instance = new EnchantItemData();
	}
}
