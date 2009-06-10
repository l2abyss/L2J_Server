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
package net.sf.l2j.gameserver.datatables;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.L2HennaInstance;
import net.sf.l2j.gameserver.model.base.ClassId;
import net.sf.l2j.gameserver.templates.item.L2Henna;

/**
 * This class ...
 *
 * @version $Revision$ $Date$
 */
public class HennaTreeTable
{
	private static Logger _log = Logger.getLogger(HennaTreeTable.class.getName());
	private Map<ClassId, List<L2HennaInstance>> _hennaTrees;
	private boolean _initialized = true;
	
	public static HennaTreeTable getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private HennaTreeTable()
	{
		_hennaTrees = new FastMap<ClassId, List<L2HennaInstance>>();
		int classId = 0;
		int count = 0;
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT class_name, id, parent_id FROM class_list ORDER BY id");
			ResultSet classlist = statement.executeQuery();
			List<L2HennaInstance> list;
			//int parentClassId;
			//L2Henna henna;
			while (classlist.next())
			{
				list = new FastList<L2HennaInstance>();
				classId = classlist.getInt("id");
				PreparedStatement statement2 = con.prepareStatement("SELECT class_id, symbol_id FROM henna_trees where class_id=? ORDER BY symbol_id");
				statement2.setInt(1, classId);
				ResultSet hennatree = statement2.executeQuery();
				
				while (hennatree.next())
				{
					int id = hennatree.getInt("symbol_id");
					//String name = hennatree.getString("name");
					L2Henna template = HennaTable.getInstance().getTemplate(id);
					if (template == null)
					{
						hennatree.close();
						statement2.close();
						classlist.close();
						statement.close();
						return;
					}
					L2HennaInstance temp = new L2HennaInstance(template);
					temp.setSymbolId(id);
					temp.setItemIdDye(template.getDyeId());
					temp.setAmountDyeRequire(template.getAmountDyeRequire());
					temp.setPrice(template.getPrice());
					temp.setStatINT(template.getStatINT());
					temp.setStatSTR(template.getStatSTR());
					temp.setStatCON(template.getStatCON());
					temp.setStatMEM(template.getStatMEM());
					temp.setStatDEX(template.getStatDEX());
					temp.setStatWIT(template.getStatWIT());
					
					list.add(temp);
				}
				_hennaTrees.put(ClassId.values()[classId], list);
				hennatree.close();
				statement2.close();
				count += list.size();
				_log.fine("Henna Tree for Class: " + classId + " has " + list.size() + " Henna Templates.");
			}
			
			classlist.close();
			statement.close();
			
			_log.config("HennaTreeTable: Loaded " + count + " Henna Tree Templates.");
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Error loading Henna Tree Table.", e);
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
	
	public L2HennaInstance[] getAvailableHenna(ClassId classId)
	{
		List<L2HennaInstance> result = new FastList<L2HennaInstance>();
		List<L2HennaInstance> henna = _hennaTrees.get(classId);
		if (henna == null)
		{
			// the hennatree for this class is undefined, so we give an empty list
			_log.warning("Hennatree for class " + classId + " is not defined !");
			return new L2HennaInstance[0];
		}
		
		for (L2HennaInstance temp : henna)
		{
			result.add(temp);
		}
		
		return result.toArray(new L2HennaInstance[result.size()]);
	}
	
	public boolean isInitialized()
	{
		return _initialized;
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final HennaTreeTable _instance = new HennaTreeTable();
	}
}
