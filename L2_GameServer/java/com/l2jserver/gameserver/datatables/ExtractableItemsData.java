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

/**
 *
 * @author FBIagent
 *
 */

package com.l2jserver.gameserver.datatables;

import gnu.trove.TIntObjectHashMap;

import java.io.File;
import java.util.Scanner;
import java.util.logging.Logger;

import javolution.util.FastList;

import com.l2jserver.Config;
import com.l2jserver.gameserver.model.L2ExtractableItem;
import com.l2jserver.gameserver.model.L2ExtractableProductItem;

public class ExtractableItemsData
{
	protected static final Logger _log = Logger.getLogger(ExtractableItemsData.class.getName());
	//          Map<itemid, L2ExtractableItem>
	private final TIntObjectHashMap<L2ExtractableItem> _items = new TIntObjectHashMap<L2ExtractableItem>();
	
	public static ExtractableItemsData getInstance()
	{
		return SingletonHolder._instance;
	}
	
	public ExtractableItemsData()
	{
		loadExtractableItems();
	}
	
	public L2ExtractableItem getExtractableItem(int itemID)
	{
		return _items.get(itemID);
	}
	
	public void reload()
	{
		loadExtractableItems();
	}
	
	private void loadExtractableItems()
	{
		Scanner s;
		
		try
		{
			s = new Scanner(new File(Config.DATAPACK_ROOT + "/data/extractable_items.csv"));
		}
		catch (Exception e)
		{
			_log.warning("Extractable items data: Can not find '" + Config.DATAPACK_ROOT + "/data/extractable_items.csv'");
			return;
		}
		
		_items.clear();
		int lineCount = 0;
		
		while (s.hasNextLine())
		{
			lineCount++;
			
			String line = s.nextLine();
			
			if (line.startsWith("#"))
				continue;
			else if (line.equals(""))
				continue;
			
			String[] lineSplit = line.split(";");
			boolean ok = true;
			int itemID = 0;
			
			try
			{
				itemID = Integer.parseInt(lineSplit[0]);
			}
			catch (Exception e)
			{
				_log.warning("Extractable items data: Error in line " + lineCount + " -> invalid item id or wrong seperator after item id!");
				_log.warning("		" + line);
				ok = false;
			}
			
			if (!ok)
				continue;
			
			FastList<L2ExtractableProductItem> product_temp = new FastList<L2ExtractableProductItem>();
			
			for (int i = 0; i < lineSplit.length - 1; i++)
			{
				ok = true;
				
				String[] lineSplit2 = lineSplit[i + 1].split(",");
				
				if (lineSplit2.length < 3)
				{
					_log.warning("Extractable items data: Error in line " + lineCount + " -> wrong seperator!");
					_log.warning("		" + line);
					ok = false;
				}
				
				if (!ok)
					continue;
				
				int[] production =null;
				int[] amount = null;
				int chance = 0;
				
				try
				{
					int k =0;
					production = new int[lineSplit2.length-1/2];
					amount = new int[lineSplit2.length-1/2];
					for (int j = 0; j < lineSplit2.length-1 ;j++)
					{
						production[k] = Integer.parseInt(lineSplit2[j]);
						amount[k] = Integer.parseInt(lineSplit2[j+=1]);
						k++;
					}
					
					chance = Integer.parseInt(lineSplit2[lineSplit2.length-1]);
				}
				catch (Exception e)
				{
					_log.warning("Extractable items data: Error in line " + lineCount + " -> incomplete/invalid production data or wrong seperator!");
					_log.warning("		" + line);
					ok = false;
				}
				
				if (!ok)
					continue;
				
				L2ExtractableProductItem product = new L2ExtractableProductItem(production, amount, chance);
				product_temp.add(product);
			}
			
			int fullChances = 0;
			
			for (L2ExtractableProductItem Pi : product_temp)
				fullChances += Pi.getChance();
			
			if (fullChances > 100)
			{
				_log.warning("Extractable items data: Error in line " + lineCount + " -> all chances together are more then 100!");
				_log.warning("		" + line);
				continue;
			}
			L2ExtractableItem product = new L2ExtractableItem(itemID, product_temp);
			_items.put(itemID, product);
		}
		
		s.close();
		_log.info("Extractable items data: Loaded " + _items.size() + " extractable items!");
	}
	
	private static class SingletonHolder
	{
		protected static final ExtractableItemsData _instance = new ExtractableItemsData();
	}
}
