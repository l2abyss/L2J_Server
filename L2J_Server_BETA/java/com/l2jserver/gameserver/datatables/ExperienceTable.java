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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.l2jserver.Config;

/**
 * @author mrTJO
 *
 */
public class ExperienceTable
{
	private static Logger _log = Logger.getLogger(ExperienceTable.class.getName());
	private byte MAX_LEVEL;
	private byte MAX_PET_LEVEL;
	
	private Map<Integer, Long> _expTable;
	
	public static ExperienceTable getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private ExperienceTable()
	{
		loadTable();
	}
	
	private void loadTable()
	{
		File xml = new File(Config.DATAPACK_ROOT, "data/experience.xml");
		Document doc = null;
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setIgnoringComments(true);
		if (xml.exists())
		{
			try
			{
				doc = factory.newDocumentBuilder().parse(xml);
			}
			catch (IOException e)
			{
				_log.log(Level.WARNING, "Could not read experience.xml table: " + e.getMessage(), e);
			}
			catch (Exception e)
			{
				_log.log(Level.WARNING, "Could not parse experience.xml table: " + e.getMessage(), e);
			}
			
			Node table = doc.getFirstChild();
			NamedNodeMap tableAttr = table.getAttributes();
			
			MAX_LEVEL = (byte)(Byte.parseByte(tableAttr.getNamedItem("maxLevel").getNodeValue())+1);
			MAX_PET_LEVEL = (byte)(Byte.parseByte(tableAttr.getNamedItem("maxPetLevel").getNodeValue())+1);

			_expTable = new HashMap<Integer, Long>(MAX_LEVEL+1);
			
			for (Node experience = table.getFirstChild(); experience != null; experience = experience.getNextSibling())
			{
				if (experience.getNodeName().equals("experience"))
				{
					NamedNodeMap attrs = experience.getAttributes();
					int level = Integer.parseInt(attrs.getNamedItem("level").getNodeValue());
					long exp = Long.parseLong(attrs.getNamedItem("tolevel").getNodeValue());
					
					_expTable.put(level, exp);
				}
			}
			
			_log.info("ExperienceTable: Loaded "+_expTable.size()+" levels");
			_log.info("ExperienceTable: Max Player Level is: "+(MAX_LEVEL-1));
			_log.info("ExperienceTable: Max Pet Level is: "+(MAX_PET_LEVEL-1));
		}
		else
			_log.warning("ExperienceTable: experience.xml not found!");
	}
	
	public long getExpForLevel(int level)
	{
		return _expTable.get(level);
	}
	
	public byte getMaxLevel()
	{
		return MAX_LEVEL;
	}
	
	public byte getMaxPetLevel()
	{
		return MAX_PET_LEVEL;
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final ExperienceTable _instance = new ExperienceTable();
	}
}
