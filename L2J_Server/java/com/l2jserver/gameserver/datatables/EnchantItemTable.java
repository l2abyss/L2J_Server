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

import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.l2jserver.Config;
import com.l2jserver.gameserver.model.EnchantItem;
import com.l2jserver.gameserver.model.EnchantScroll;
import com.l2jserver.gameserver.model.item.L2Item;
import com.l2jserver.gameserver.model.item.instance.L2ItemInstance;


/**
 * @author UnAfraid
 *
 */
public class EnchantItemTable
{
	private static final Logger _log = Logger.getLogger(EnchantItemTable.class.getName());
	
	public final TIntObjectHashMap<EnchantScroll> _scrolls;
	public final TIntObjectHashMap<EnchantItem> _supports;
	
	public EnchantItemTable()
	{
		_scrolls = new TIntObjectHashMap<EnchantScroll>();
		_supports = new TIntObjectHashMap<EnchantItem>();
		
		load();
	}
	
	public void load()
	{
		try
		{
			_scrolls.clear();
			_supports.clear();
			
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			
			File file = new File(Config.DATAPACK_ROOT + "/data/enchantData.xml");
			if (!file.exists())
			{
				_log.log(Level.WARNING, "[" + getClass().getSimpleName() + "] File is missing " + Config.DATAPACK_ROOT + "/data/enchantData.xml !");
				return;
			}
			
			Document doc = factory.newDocumentBuilder().parse(file);
			Node first = doc.getFirstChild();
			if (first != null && "list".equalsIgnoreCase(first.getNodeName()))
			{
				for (Node n = first.getFirstChild(); n != null; n = n.getNextSibling())
				{
					if ("enchant".equalsIgnoreCase(n.getNodeName()))
					{
						int scrollId = 0;
						boolean isWeapon = true;
						boolean isBlessed = false;
						boolean isCrystal = false;
						boolean isSafe = false;
						int type = L2Item.CRYSTAL_NONE;
						int maxEnchant = 0;
						double chance = Config.ENCHANT_CHANCE;
						int[] items = null;
						
						NamedNodeMap attrs = n.getAttributes();
						Node att = attrs.getNamedItem("id");
						
						if (att == null)
						{
							_log.log(Level.WARNING, "[" + getClass().getSimpleName() + "] Missing Enchant id, skipping");
							continue;
						}
						scrollId = Integer.parseInt(att.getNodeValue());
						
						att = attrs.getNamedItem("isWeapon");
						if (att != null)
						{
							isWeapon = Boolean.parseBoolean(att.getNodeValue());
						}
						
						att = attrs.getNamedItem("isBlessed");
						if (att != null)
						{
							isBlessed = Boolean.parseBoolean(att.getNodeValue());
						}
						
						att = attrs.getNamedItem("isCrystal");
						if (att != null)
						{
							isCrystal = Boolean.parseBoolean(att.getNodeValue());
						}
						
						att = attrs.getNamedItem("isSafe");
						if (att != null)
						{
							isSafe = Boolean.parseBoolean(att.getNodeValue());
						}
						
						att = attrs.getNamedItem("targetGrade");
						if (att != null)
						{
							type = ItemTable._crystalTypes.get(att.getNodeValue());
						}
						
						att = attrs.getNamedItem("maxEnchant");
						if (att != null)
						{
							maxEnchant = Integer.parseInt(att.getNodeValue());
						}
						
						att = attrs.getNamedItem("successRate");
						if (att != null)
						{
							chance = Double.parseDouble(att.getNodeValue());
							chance = Math.max(chance, 1.0); // Enchant bonus cannot be below 1.0
						}
						
						List<Integer> itemz = new ArrayList<Integer>();
						
						for (Node cd = n.getFirstChild(); cd != null; cd = cd.getNextSibling())
						{
							if ("item".equalsIgnoreCase(cd.getNodeName()))
							{
								att = cd.getAttributes().getNamedItem("id");
								if (itemz != null)
									itemz.add(Integer.parseInt(att.getNodeValue()));
								else
								{
									_log.log(Level.WARNING, "[" + getClass().getSimpleName() + "] Missing Item id, skipping");
									continue;
								}
							}
						}
						
						if (itemz.size() > 0)
						{
							items = new int[itemz.size()];
							int i = 0;
							for (Integer id : itemz)
							{
								items[i++] = id;
							}
							Arrays.sort(items);
						}
						
						_scrolls.put(scrollId, new EnchantScroll(isWeapon, isBlessed, isCrystal, isSafe, type, maxEnchant, chance, items));
					}
					else if ("support".equalsIgnoreCase(n.getNodeName()))
					{
						int scrollId = 0;
						boolean isWeapon = true;
						int type = L2Item.CRYSTAL_NONE;
						int maxEnchant = 0;
						double chance = 1.0;
						int[] items = null;
						NamedNodeMap attrs = n.getAttributes();
						Node att = attrs.getNamedItem("id");
						
						if (att == null)
						{
							_log.log(Level.WARNING, "[" + getClass().getSimpleName() + "] Missing Support id, skipping");
							continue;
						}
						scrollId = Integer.parseInt(att.getNodeValue());
						
						att = attrs.getNamedItem("isWeapon");
						if (att != null)
						{
							isWeapon = Boolean.parseBoolean(att.getNodeValue());
						}
						
						att = attrs.getNamedItem("targetGrade");
						if (att != null)
						{
							type = ItemTable._crystalTypes.get(att.getNodeValue());
						}
						
						att = attrs.getNamedItem("maxEnchant");
						if (att != null)
						{
							maxEnchant = Integer.parseInt(att.getNodeValue());
						}
						
						att = attrs.getNamedItem("successBonus");
						if (att != null)
						{
							chance = Double.parseDouble(att.getNodeValue());
							chance = Math.max(chance, 1.0); // Enchant bonus cannot be below 1.0
						}
						
						List<Integer> itemz = new ArrayList<Integer>();
						
						for (Node cd = n.getFirstChild(); cd != null; cd = cd.getNextSibling())
						{
							if ("item".equalsIgnoreCase(cd.getNodeName()))
							{
								att = cd.getAttributes().getNamedItem("id");
								if (itemz != null)
									itemz.add(Integer.parseInt(att.getNodeValue()));
								else
								{
									_log.log(Level.WARNING, "[" + getClass().getSimpleName() + "] Missing Item id, skipping");
									continue;
								}
							}
						}
						
						if (itemz.size() > 0)
						{
							items = new int[itemz.size()];
							int i = 0;
							for (Integer id : itemz)
							{
								items[i++] = id;
							}
							Arrays.sort(items);
						}
						_supports.put(scrollId, new EnchantItem(isWeapon, type, maxEnchant, chance, items));
					}
				}
			}
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "[" + getClass().getSimpleName() + "] Failed to parse xml: " + e.getMessage(), e);
		}
		
		_log.info(getClass().getSimpleName() + ": Loaded " + _scrolls.size() + " Enchant Scrolls");
		_log.info(getClass().getSimpleName() + ": Loaded " + _supports.size() + " Support Items");
	}
	
	/**
	 * @param scroll 
	 * @return enchant template for scroll
	 */
	public final EnchantScroll getEnchantScroll(L2ItemInstance scroll)
	{
		return _scrolls.get(scroll.getItemId());
	}
	
	/**
	 * @param item 
	 * @return enchant template for support item
	 */
	public final EnchantItem getSupportItem(L2ItemInstance item)
	{
		return _supports.get(item.getItemId());
	}
	
	public static final EnchantItemTable getInstance()
	{
		return SingletonHolder._instance;
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final EnchantItemTable _instance = new EnchantItemTable();
	}
}
