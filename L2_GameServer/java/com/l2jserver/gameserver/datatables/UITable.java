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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;

import com.l2jserver.Config;
import com.l2jserver.gameserver.model.entity.ActionKey;

/**
 *
 * @author  mrTJO
 */
public class UITable
{
	private static Logger _log = Logger.getLogger(StaticObjects.class.getName());
	
	private Map<Integer, List<ActionKey>> _storedKeys;
	private Map<Integer, List<Integer>> _storedCategories;
	
	public static UITable getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private UITable()
	{
		_storedKeys = new FastMap<Integer, List<ActionKey>>();
		_storedCategories = new FastMap<Integer, List<Integer>>();
		
		parseCatData();
		parseKeyData();
		_log.config("UITable: Loaded " + _storedCategories.size() + " Categories.");
		_log.config("UITable: Loaded " + _storedKeys.size() + " Keys.");
	}
	
	private void parseCatData()
	{
		LineNumberReader lnr = null;
		try
		{
			File uiData = new File(Config.DATAPACK_ROOT, "data/uicats_en.csv");
			lnr = new LineNumberReader(new BufferedReader(new FileReader(uiData)));
			
			String line = null;
			while ((line = lnr.readLine()) != null)
			{
				if (line.trim().length() == 0 || line.startsWith("#"))
					continue;
				
				StringTokenizer st = new StringTokenizer(line, ";");
				
				int cat = Integer.parseInt(st.nextToken());
				int cmd = Integer.parseInt(st.nextToken());
				
				insertCategory(cat, cmd);
			}
		}
		catch (FileNotFoundException e)
		{
			_log.warning("uicats_en.csv is missing in data folder");
		}
		catch (Exception e)
		{
			_log.warning("error while creating UI Default Categories table " + e);
		}
		finally
		{
			try
			{
				lnr.close();
			}
			catch (Exception e)
			{
			}
		}
	}
	
	private void parseKeyData()
	{
		LineNumberReader lnr = null;
		try
		{
			File uiData = new File(Config.DATAPACK_ROOT, "data/uikeys_en.csv");
			lnr = new LineNumberReader(new BufferedReader(new FileReader(uiData)));
			
			String line = null;
			while ((line = lnr.readLine()) != null)
			{
				if (line.trim().length() == 0 || line.startsWith("#"))
					continue;
				
				StringTokenizer st = new StringTokenizer(line, ";");
				
				int cat = Integer.parseInt(st.nextToken());
				int cmd = Integer.parseInt(st.nextToken());
				int key = Integer.parseInt(st.nextToken());
				int tk1 = Integer.parseInt(st.nextToken());
				int tk2 = Integer.parseInt(st.nextToken());
				int shw = Integer.parseInt(st.nextToken());
				
				insertKey(cat, cmd, key, tk1, tk2, shw);
			}
		}
		catch (FileNotFoundException e)
		{
			_log.warning("uikeys_en.csv is missing in data folder");
		}
		catch (Exception e)
		{
			_log.warning("error while creating UI Default Keys table " + e);
		}
		finally
		{
			try
			{
				lnr.close();
			}
			catch (Exception e)
			{
			}
		}
	}
	
	private void insertCategory(int cat, int cmd)
	{
		if (_storedCategories.containsKey(cat))
			_storedCategories.get(cat).add(cmd);
		else
		{
			List<Integer> tmp = new FastList<Integer>();
			tmp.add(cmd);
			_storedCategories.put(cat, tmp);
		}
	}
	
	private void insertKey(int cat, int cmdId, int key, int tgKey1, int tgKey2, int show)
	{
		ActionKey tmk = new ActionKey(cat, cmdId, key, tgKey1, tgKey2, show);
		if (_storedKeys.containsKey(cat))
			_storedKeys.get(cat).add(tmk);
		else
		{
			List<ActionKey> tmp = new FastList<ActionKey>();
			tmp.add(tmk);
			_storedKeys.put(cat, tmp);
		}
	}
	
	public Map<Integer, List<Integer>> getCategories()
	{
		return _storedCategories;
	}
	
	public Map<Integer, List<ActionKey>> getKeys()
	{
		return _storedKeys;
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final UITable _instance = new UITable();
	}
}
