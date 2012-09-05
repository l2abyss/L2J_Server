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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2jserver.L2DatabaseFactory;
import com.l2jserver.gameserver.model.StatsSet;
import com.l2jserver.gameserver.model.actor.templates.L2PcTemplate;
import com.l2jserver.gameserver.model.base.ClassId;

/**
 * This will be reworked Soon(tm).
 * @author Unknown, Forsaiken, Zoey76
 */
public final class CharTemplateTable
{
	private static final Logger _log = Logger.getLogger(CharTemplateTable.class.getName());
	
	private static final Map<ClassId, L2PcTemplate> _charTemplates = new HashMap<>();
	
	protected CharTemplateTable()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			Statement s = con.createStatement();
			ResultSet rset = s.executeQuery("SELECT * FROM char_templates, lvlupgain WHERE char_templates.classId = lvlupgain.classId ORDER BY char_templates.ClassId"))
		{
			StatsSet set;
			int cId;
			while (rset.next())
			{
				set = new StatsSet();
				cId = rset.getInt("ClassId");
				set.set("classId", cId);
				set.set("className", rset.getString("className"));
				set.set("raceId", rset.getInt("raceId"));
				set.set("baseSTR", rset.getInt("STR"));
				set.set("baseCON", rset.getInt("CON"));
				set.set("baseDEX", rset.getInt("DEX"));
				set.set("baseINT", rset.getInt("_INT"));
				set.set("baseWIT", rset.getInt("WIT"));
				set.set("baseMEN", rset.getInt("MEN"));
				set.set("baseHpMax", rset.getFloat("defaultHpBase"));
				set.set("lvlHpAdd", rset.getFloat("defaultHpAdd"));
				set.set("lvlHpMod", rset.getFloat("defaultHpMod"));
				set.set("baseMpMax", rset.getFloat("defaultMpBase"));
				set.set("baseCpMax", rset.getFloat("defaultCpBase"));
				set.set("lvlCpAdd", rset.getFloat("defaultCpAdd"));
				set.set("lvlCpMod", rset.getFloat("defaultCpMod"));
				set.set("lvlMpAdd", rset.getFloat("defaultMpAdd"));
				set.set("lvlMpMod", rset.getFloat("defaultMpMod"));
				set.set("baseHpReg", 2);
				set.set("baseMpReg", 0.9);
				set.set("basePAtk", rset.getInt("p_atk"));
				set.set("basePDef", rset.getInt("p_def"));
				set.set("baseMAtk", rset.getInt("m_atk"));
				set.set("baseMDef", rset.getInt("m_def"));
				set.set("classBaseLevel", rset.getInt("class_lvl"));
				set.set("baseRunSpd", rset.getInt("move_spd"));
				set.set("baseWalkSpd", 0);
				set.set("baseShldDef", 0);
				set.set("baseShldRate", 0);
				set.set("baseAtkRange", 40);
				
				set.set("spawnX", rset.getInt("x"));
				set.set("spawnY", rset.getInt("y"));
				set.set("spawnZ", rset.getInt("z"));
				
				set.set("collision_radius", rset.getDouble("m_col_r"));
				set.set("collision_height", rset.getDouble("m_col_h"));
				set.set("collision_radius_female", rset.getDouble("f_col_r"));
				set.set("collision_height_female", rset.getDouble("f_col_h"));
				
				final L2PcTemplate ct = new L2PcTemplate(set);
				_charTemplates.put(ClassId.getClassId(cId), ct);
			}
			_log.info(getClass().getSimpleName() + ": Loaded " + _charTemplates.size() + " Character Templates.");
		}
		catch (SQLException e)
		{
			_log.log(Level.SEVERE, getClass().getSimpleName() + ": Failed loading char templates", e);
		}
	}
	
	public L2PcTemplate getTemplate(final ClassId classId)
	{
		return _charTemplates.get(classId);
	}
	
	public L2PcTemplate getTemplate(final int classId)
	{
		return _charTemplates.get(ClassId.getClassId(classId));
	}
	
	public static final CharTemplateTable getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final CharTemplateTable _instance = new CharTemplateTable();
	}
}
