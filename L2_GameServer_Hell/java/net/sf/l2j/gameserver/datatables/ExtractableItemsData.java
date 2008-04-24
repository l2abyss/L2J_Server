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

package net.sf.l2j.gameserver.datatables;

import java.io.File;
import java.util.Scanner;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;
import net.sf.l2j.gameserver.model.L2ExtractableItem;
import net.sf.l2j.gameserver.model.L2ExtractableProductItem;

public class ExtractableItemsData
{
    protected static final Logger _log = Logger.getLogger(ExtractableItemsData.class.getName());
	//          Map<itemid, L2ExtractableItem>
	private FastMap<Integer, L2ExtractableItem> _items;

	private static ExtractableItemsData _instance = null;

	public static ExtractableItemsData getInstance()
	{
		if (_instance == null)
			_instance = new ExtractableItemsData();

		return _instance;
	}

	public ExtractableItemsData()
	{
		_items = new FastMap<Integer,L2ExtractableItem>();

		Scanner s;

		try
		{
			s = new Scanner(new File("./data/extractable_items.csv"));
		}
		catch (Exception e)
		{
			_log.warning("Extractable items data: Can not find './data/extractable_items.csv'");
			return;
		}

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

            for (int i=0;i<lineSplit.length-1;i++)
            {
            	ok = true;

            	String[] lineSplit2 = lineSplit[i+1].split(",");

            	if (lineSplit2.length != 3)
            	{
               		_log.warning("Extractable items data: Error in line " + lineCount + " -> wrong seperator!");
               		_log.warning("		" + line);
            		ok = false;
            	}
            	
            	if (!ok)
            		continue;

            	int production = 0,
    				amount = 0,
    				chance = 0;

            	try
            	{
            		production = Integer.parseInt(lineSplit2[0]);
            		amount = Integer.parseInt(lineSplit2[1]);
            		chance = Integer.parseInt(lineSplit2[2]);
            	}
            	catch (Exception e)
            	{
               		_log.warning("Extractable items data: Error in line " + lineCount + " -> incomplete/invalid production data or wrong seperator!");
               		_log.warning("		" + line);
            		ok = false;
            	}
            	
            	if (!ok)
            		continue;
            	
            	L2ExtractableProductItem product = new L2ExtractableProductItem(production,amount,chance);
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
	
	public L2ExtractableItem getExtractableItem(int itemID)
	{
		return _items.get(itemID);
	}	
	public int[] itemIDs()
	{
		int size = _items.size();
		int[] result = new int[size];
		int i = 0;
		for (L2ExtractableItem ei : _items.values())
        {
			result[i] = ei.getItemId();
			i++;
        }
		return result;
	}
}
