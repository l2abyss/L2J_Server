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

package net.sf.l2j.gameserver.datatables;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;

import javolution.util.FastMap;
import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.L2ArmorSet;

/**
 *
 *
 * @author  Luno
 */
public class ArmorSetsTable
{
	private static Logger _log = Logger.getLogger(ArmorSetsTable.class.getName());
	private static ArmorSetsTable _instance;
	
	private FastMap<Integer, L2ArmorSet> _armorSets;
	
	public static ArmorSetsTable getInstance()
	{
		if (_instance == null)
			_instance = new ArmorSetsTable();
		return _instance;
	}
	
	private ArmorSetsTable()
	{
		_armorSets = new FastMap<Integer, L2ArmorSet>();
		loadData();
	}
	
	private void loadData()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT chest, legs, head, gloves, feet, skill_id, skill_lvl, shield, shield_skill_id, enchant6skill, mw_legs, mw_head, mw_gloves, mw_feet, mw_shield FROM armorsets");
			ResultSet rset = statement.executeQuery();
			
			while (rset.next())
			{
				int chest = rset.getInt("chest");
				int legs = rset.getInt("legs");
				int head = rset.getInt("head");
				int gloves = rset.getInt("gloves");
				int feet = rset.getInt("feet");
				int skill_id = rset.getInt("skill_id");
				int skill_lvl = rset.getInt("skill_lvl");
				int shield = rset.getInt("shield");
				int shield_skill_id = rset.getInt("shield_skill_id");
				int enchant6skill = rset.getInt("enchant6skill");
				int mw_legs = rset.getInt("mw_legs"); 
				int mw_head = rset.getInt("mw_head"); 
				int mw_gloves = rset.getInt("mw_gloves"); 
				int mw_feet = rset.getInt("mw_feet"); 
				int mw_shield = rset.getInt("mw_shield");
				_armorSets.put(chest, new L2ArmorSet(chest, legs, head, gloves, feet, skill_id, skill_lvl, shield, shield_skill_id, enchant6skill, mw_legs, mw_head, mw_gloves, mw_feet, mw_shield));
			}
			
			_log.config("ArmorSetsTable: Loaded " + _armorSets.size() + " armor sets.");
			
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.severe("ArmorSetsTable: Error reading ArmorSets table: " + e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
			}
		}
		if (Config.CUSTOM_ARMORSETS_TABLE)
		{
			try
			{
				int cSets = _armorSets.size();
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement("SELECT chest, legs, head, gloves, feet, skill_id, skill_lvl, shield, shield_skill_id, enchant6skill, mw_legs, mw_head, mw_gloves, mw_feet, mw_shield FROM custom_armorsets");
				ResultSet rset = statement.executeQuery();
				while (rset.next())
				{
					int chest = rset.getInt("chest");
					int legs = rset.getInt("legs");
					int head = rset.getInt("head");
					int gloves = rset.getInt("gloves");
					int feet = rset.getInt("feet");
					int skill_id = rset.getInt("skill_id");
					int skill_lvl = rset.getInt("skill_lvl");
					int shield = rset.getInt("shield");
					int shield_skill_id = rset.getInt("shield_skill_id");
					int enchant6skill = rset.getInt("enchant6skill");
					int mw_legs = rset.getInt("mw_legs"); 
					int mw_head = rset.getInt("mw_head"); 
					int mw_gloves = rset.getInt("mw_gloves"); 
					int mw_feet = rset.getInt("mw_feet"); 
					int mw_shield = rset.getInt("mw_shield");
					_armorSets.put(chest, new L2ArmorSet(chest, legs, head, gloves, feet, skill_id, skill_lvl, shield, shield_skill_id, enchant6skill, mw_legs, mw_head, mw_gloves, mw_feet, mw_shield));
				}
				_log.config("ArmorSetsTable: Loaded " + (_armorSets.size() - cSets) + " Custom armor sets.");
				rset.close();
				statement.close();
			}
			catch (Exception e)
			{
				_log.severe("ArmorSetsTable: Error reading Custom ArmorSets table: " + e);
			}
			finally
			{
				try
				{
					con.close();
				}
				catch (Exception e)
				{
				}
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
}
