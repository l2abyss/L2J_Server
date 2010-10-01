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

import gnu.trove.TIntObjectHashMap;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastList;

import com.l2jserver.L2DatabaseFactory;
import com.l2jserver.gameserver.model.L2DropCategory;
import com.l2jserver.gameserver.model.L2DropData;

/**
 * This class ...
 *
 * @version $Revision$ $Date$
 */
public class HerbDropTable
{
	private static Logger _log = Logger.getLogger(HerbDropTable.class.getName());
	
	private TIntObjectHashMap<FastList<L2DropCategory>> _herbGroups;
	
	public static HerbDropTable getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private HerbDropTable()
	{
		_herbGroups = new TIntObjectHashMap<FastList<L2DropCategory>>();
		restoreData();
	}
	
	/**
	 *
	 */
	private void restoreData()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement2 = con.prepareStatement("SELECT "
					+ L2DatabaseFactory.getInstance().safetyString(new String[] { "groupId", "itemId", "min", "max", "category", "chance" })
					+ " FROM herb_droplist_groups ORDER BY groupId, chance DESC");
			ResultSet dropData = statement2.executeQuery();
			L2DropData dropDat = null;
			
			while (dropData.next())
			{
				int groupId = dropData.getInt("groupId");
				FastList<L2DropCategory> category;
				if (_herbGroups.contains(groupId))
					category = _herbGroups.get(groupId);
				else
				{
					category = new FastList<L2DropCategory>();
					_herbGroups.put(groupId, category);
				}
				
				dropDat = new L2DropData();
				
				dropDat.setItemId(dropData.getInt("itemId"));
				dropDat.setMinDrop(dropData.getInt("min"));
				dropDat.setMaxDrop(dropData.getInt("max"));
				dropDat.setChance(dropData.getInt("chance"));
				
				int categoryType = dropData.getInt("category");
				
				if (ItemTable.getInstance().getTemplate(dropDat.getItemId()) == null)
				{
					_log.warning("Herb Drop data for undefined item template! GroupId: " + groupId+" itemId: "+dropDat.getItemId());
					continue;
				}
				
				boolean catExists = false;
				for (L2DropCategory cat : category)
					// if the category exists, add the drop to this category.
					if (cat.getCategoryType() == categoryType)
					{
						cat.addDropData(dropDat, false);
						catExists = true;
						break;
					}
				// if the category doesn't exit, create it and add the drop
				if (!catExists)
				{
					L2DropCategory cat = new L2DropCategory(categoryType);
					cat.addDropData(dropDat, false);
					category.add(cat);
				}
			}
			
			dropData.close();
			statement2.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "HerbDroplistGroupsTable: Error reading Herb dropdata. ", e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
	}
	
	public FastList<L2DropCategory> getHerbDroplist(int groupId)
	{
		return _herbGroups.get(groupId);
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final HerbDropTable _instance = new HerbDropTable();
	}
}