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

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.w3c.dom.Node;

import com.l2jserver.gameserver.engines.DocumentParser;
import com.l2jserver.gameserver.model.EnchantOptions;
import com.l2jserver.gameserver.model.items.instance.L2ItemInstance;
import com.l2jserver.gameserver.util.Util;

/**
 * @author UnAfraid
 */
public class EnchantOptionsData extends DocumentParser
{
	private final Map<Integer, Map<Integer, EnchantOptions>> _data = new HashMap<>();
	
	protected EnchantOptionsData()
	{
		load();
	}
	
	@Override
	public synchronized void load()
	{
		parseDatapackFile("data/enchantOptions.xml");
	}
	
	@Override
	protected void parseDocument()
	{
		Node att = null;
		int counter = 0;
		EnchantOptions op = null;
		for (Node n = getCurrentDocument().getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("item".equalsIgnoreCase(d.getNodeName()))
					{
						int itemId = parseInt(d.getAttributes(), "id");
						if (!_data.containsKey(itemId))
						{
							_data.put(itemId, new HashMap<Integer, EnchantOptions>());
						}
						for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
						{
							if ("options".equalsIgnoreCase(cd.getNodeName()))
							{
								op = new EnchantOptions(parseInt(cd.getAttributes(), "level"));
								_data.get(itemId).put(op.getLevel(), op);
								
								for (byte i = 0; i < 3; i++)
								{
									att = cd.getAttributes().getNamedItem("option" + (i + 1));
									if ((att != null) && Util.isDigit(att.getNodeValue()))
									{
										op.setOption(i, parseInt(att));
									}
								}
								counter++;
							}
						}
					}
				}
			}
		}
		_log.log(Level.INFO, getClass().getSimpleName() + ": Loaded: " + _data.size() + " Items and " + counter + " Options.");
	}
	
	/**
	 * @param itemId
	 * @param enchantLevel
	 * @return enchant effects information.
	 */
	public EnchantOptions getOptions(int itemId, int enchantLevel)
	{
		if (!_data.containsKey(itemId) || !_data.get(itemId).containsKey(enchantLevel))
		{
			return null;
		}
		return _data.get(itemId).get(enchantLevel);
	}
	
	/**
	 * @param item
	 * @return enchant effects information.
	 */
	public EnchantOptions getOptions(L2ItemInstance item)
	{
		return item != null ? getOptions(item.getItemId(), item.getEnchantLevel()) : null;
	}
	
	/**
	 * Gets the single instance of EnchantOptionsData.
	 * @return single instance of EnchantOptionsData
	 */
	public static final EnchantOptionsData getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final EnchantOptionsData _instance = new EnchantOptionsData();
	}
}
