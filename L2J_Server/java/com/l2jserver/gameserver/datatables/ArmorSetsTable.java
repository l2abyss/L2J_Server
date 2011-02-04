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
 * @author godson
 */

package com.l2jserver.gameserver.datatables;

import gnu.trove.TIntObjectHashMap;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2jserver.Config;
import com.l2jserver.L2DatabaseFactory;
import com.l2jserver.gameserver.model.L2ArmorSet;


/**
 *
 *
 * @author  Luno
 */
public class ArmorSetsTable
{
	private static Logger _log = Logger.getLogger(ArmorSetsTable.class.getName());
	
	private TIntObjectHashMap<L2ArmorSet> _armorSets;
	
	public static ArmorSetsTable getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private ArmorSetsTable()
	{
		_armorSets = new TIntObjectHashMap<L2ArmorSet>();
		loadData();
	}
	
	private void loadData()
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT chest, legs, head, gloves, feet, skill, shield, shield_skill_id, enchant6skill, mw_legs, mw_head, mw_gloves, mw_feet, mw_shield FROM armorsets");
			ResultSet rset = statement.executeQuery();
			
			while (rset.next())
			{
				int chest = rset.getInt("chest");
				int legs = rset.getInt("legs");
				int head = rset.getInt("head");
				int gloves = rset.getInt("gloves");
				int feet = rset.getInt("feet");
				String[] skills = rset.getString("skill").split(";");
				int shield = rset.getInt("shield");
				int shield_skill_id = rset.getInt("shield_skill_id");
				int enchant6skill = rset.getInt("enchant6skill");
				int mw_legs = rset.getInt("mw_legs");
				int mw_head = rset.getInt("mw_head");
				int mw_gloves = rset.getInt("mw_gloves");
				int mw_feet = rset.getInt("mw_feet");
				int mw_shield = rset.getInt("mw_shield");
				_armorSets.put(chest, new L2ArmorSet(chest, legs, head, gloves, feet, skills, shield, shield_skill_id, enchant6skill, mw_legs, mw_head, mw_gloves, mw_feet, mw_shield));
			}
			_log.info("ArmorSetsTable: Loaded " + _armorSets.size() + " armor sets.");
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "ArmorSetsTable: Error reading ArmorSets table: " + e.getMessage(), e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
		if (Config.CUSTOM_ARMORSETS_TABLE)
		{
			try
			{
				int cSets = _armorSets.size();
				con = L2DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement("SELECT chest, legs, head, gloves, feet, skill, shield, shield_skill_id, enchant6skill, mw_legs, mw_head, mw_gloves, mw_feet, mw_shield FROM custom_armorsets");
				ResultSet rset = statement.executeQuery();
				while (rset.next())
				{
					int chest = rset.getInt("chest");
					int legs = rset.getInt("legs");
					int head = rset.getInt("head");
					int gloves = rset.getInt("gloves");
					int feet = rset.getInt("feet");
					String[] skills = rset.getString("skill").split(";");
					int shield = rset.getInt("shield");
					int shield_skill_id = rset.getInt("shield_skill_id");
					int enchant6skill = rset.getInt("enchant6skill");
					int mw_legs = rset.getInt("mw_legs");
					int mw_head = rset.getInt("mw_head");
					int mw_gloves = rset.getInt("mw_gloves");
					int mw_feet = rset.getInt("mw_feet");
					int mw_shield = rset.getInt("mw_shield");
					_armorSets.put(chest, new L2ArmorSet(chest, legs, head, gloves, feet, skills, shield, shield_skill_id, enchant6skill, mw_legs, mw_head, mw_gloves, mw_feet, mw_shield));
				}
				_log.info("ArmorSetsTable: Loaded " + (_armorSets.size() - cSets) + " Custom armor sets.");
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "ArmorSetsTable: Error reading Custom ArmorSets table: " + e.getMessage(), e);
			}
			finally
			{
				L2DatabaseFactory.close(con);
			}
		}
	}
	
	public boolean setExists(int chestId)
	{
		return _armorSets.containsKey(chestId);
	}
	
	public L2ArmorSet getSet(int chestId)
	{
		return _armorSets.get(chestId);
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final ArmorSetsTable _instance = new ArmorSetsTable();
	}
}
