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

import gnu.trove.map.hash.TIntObjectHashMap;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2jserver.L2DatabaseFactory;
import com.l2jserver.gameserver.model.L2LvlupData;
import com.l2jserver.gameserver.model.base.ClassId;

/**
 * This class ...
 *
 * @author NightMarez
 * @version $Revision: 1.3.2.4.2.3 $ $Date: 2005/03/27 15:29:18 $
 */
public class LevelUpData
{
	private static final String SELECT_ALL = "SELECT classid, defaulthpbase, defaulthpadd, defaulthpmod, defaultcpbase, defaultcpadd, defaultcpmod, defaultmpbase, defaultmpadd, defaultmpmod, class_lvl FROM lvlupgain";
	private static final String CLASS_LVL = "class_lvl";
	private static final String MP_MOD = "defaultmpmod";
	private static final String MP_ADD = "defaultmpadd";
	private static final String MP_BASE = "defaultmpbase";
	private static final String HP_MOD = "defaulthpmod";
	private static final String HP_ADD = "defaulthpadd";
	private static final String HP_BASE = "defaulthpbase";
	private static final String CP_MOD = "defaultcpmod";
	private static final String CP_ADD = "defaultcpadd";
	private static final String CP_BASE = "defaultcpbase";
	private static final String CLASS_ID = "classid";
	
	private static Logger _log = Logger.getLogger(LevelUpData.class.getName());
	
	private TIntObjectHashMap<L2LvlupData> _lvlTable;
	
	public static LevelUpData getInstance()
	{
		return SingletonHolder._instance;
	}
	
	protected LevelUpData()
	{
		_lvlTable = new TIntObjectHashMap<>();
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			Statement s = con.createStatement();
			ResultSet rs = s.executeQuery(SELECT_ALL))
		{
			L2LvlupData lvlDat;
			while (rs.next())
			{
				lvlDat = new L2LvlupData();
				lvlDat.setClassid(rs.getInt(CLASS_ID));
				lvlDat.setClassLvl(rs.getInt(CLASS_LVL));
				lvlDat.setClassHpBase(rs.getFloat(HP_BASE));
				lvlDat.setClassHpAdd(rs.getFloat(HP_ADD));
				lvlDat.setClassHpModifier(rs.getFloat(HP_MOD));
				lvlDat.setClassCpBase(rs.getFloat(CP_BASE));
				lvlDat.setClassCpAdd(rs.getFloat(CP_ADD));
				lvlDat.setClassCpModifier(rs.getFloat(CP_MOD));
				lvlDat.setClassMpBase(rs.getFloat(MP_BASE));
				lvlDat.setClassMpAdd(rs.getFloat(MP_ADD));
				lvlDat.setClassMpModifier(rs.getFloat(MP_MOD));
				
				_lvlTable.put(lvlDat.getClassid(), lvlDat);
			}
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Error loading Level Up data.", e);
		}
		_log.info("LevelUpData: Loaded " + _lvlTable.size() + " Character Level Up Templates.");
	}
	
	/**
	 * @param classId
	 * @return
	 */
	public L2LvlupData getTemplate(int classId)
	{
		return _lvlTable.get(classId);
	}
	
	public L2LvlupData getTemplate(ClassId classId)
	{
		return _lvlTable.get(classId.getId());
	}
	
	private static class SingletonHolder
	{
		protected static final LevelUpData _instance = new LevelUpData();
	}
}
