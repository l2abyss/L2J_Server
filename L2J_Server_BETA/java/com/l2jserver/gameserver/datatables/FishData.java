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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.l2jserver.gameserver.engines.DocumentParser;
import com.l2jserver.gameserver.model.L2Fish;
import com.l2jserver.gameserver.model.StatsSet;

/**
 * This class holds the Fish information.
 * @author nonom
 */
public final class FishData extends DocumentParser
{
	private static final Map<Integer, L2Fish> _fishsNormal = new HashMap<>();
	private static final Map<Integer, L2Fish> _fishsEasy = new HashMap<>();
	private static final Map<Integer, L2Fish> _fishsHard = new HashMap<>();
	
	private FishData()
	{
		_fishsEasy.clear();
		_fishsNormal.clear();
		_fishsHard.clear();
		
		parseDatapackFile("data/stats/items/fishing/fishes.xml");
		_log.info(getClass().getSimpleName() + ": Loaded " + (_fishsEasy.size() + _fishsNormal.size() + _fishsHard.size()) + " Fishes.");
	}
	
	@Override
	protected void parseDocument(Document doc)
	{
		NamedNodeMap attrs;
		Node att;
		L2Fish fish;
		StatsSet set;
		for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("fish".equalsIgnoreCase(d.getNodeName()))
					{
						
						attrs = d.getAttributes();
						
						set = new StatsSet();
						for (int i = 0; i < attrs.getLength(); i++)
						{
							att = attrs.item(i);
							set.set(att.getNodeName(), att.getNodeValue());
						}
						fish = new L2Fish(set);
						switch (fish.getFishGrade())
						{
							case 0:
							{
								_fishsEasy.put(fish.getFishId(), fish);
								break;
							}
							case 1:
							{
								_fishsNormal.put(fish.getFishId(), fish);
								break;
							}
							case 2:
							{
								_fishsHard.put(fish.getFishId(), fish);
								break;
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * @param level the fish Level
	 * @param group the fish Group
	 * @param grade the fish Grade
	 * @return List of Fish that can be fished
	 */
	public List<L2Fish> getFish(int level, int group, int grade)
	{
		ArrayList<L2Fish> result = new ArrayList<L2Fish>();
		Map<Integer, L2Fish> _Fishs = null;
		switch (grade)
		{
			case 0:
			{
				_Fishs = _fishsEasy;
				break;
			}
			case 1:
			{
				_Fishs = _fishsNormal;
				break;
			}
			case 2:
			{
				_Fishs = _fishsHard;
				break;
			}
		}
		if (_Fishs == null)
		{
			// the fish list is empty
			_log.warning(getClass().getSimpleName() + ": Fish are not defined !");
			return null;
		}
		for (L2Fish f : _Fishs.values())
		{
			if (f.getFishLevel() != level)
			{
				continue;
			}
			if (f.getFishGroup() != group)
			{
				continue;
			}
			
			result.add(f);
		}
		if (result.isEmpty())
		{
			_log.warning(getClass().getSimpleName() + ": Cant Find Any Fish!? - Lvl: " + level + " Group: " + group + " Grade: " + grade);
		}
		return result;
	}
	
	public static FishData getInstance()
	{
		return SingletonHolder._instance;
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final FishData _instance = new FishData();
	}
}
