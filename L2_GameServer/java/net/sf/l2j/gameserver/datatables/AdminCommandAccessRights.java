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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.logging.Logger;

import javolution.util.FastMap;
import net.sf.l2j.L2DatabaseFactory;

/**
 * @author FBIagent<br>
 */
public class AdminCommandAccessRights
{
	/** The logger<br> */
	private static Logger _log = Logger.getLogger(AdminCommandAccessRights.class.getName());
	/** The one and only instance of this class, retriveable by getInstance()<br> */
	private static AdminCommandAccessRights _instance = null;

	/** The access rights<br> */
	private Map< String, AdminCommandAccessRight > _adminCommandAccessRights = new FastMap< String, AdminCommandAccessRight >();

	/**
	 * Loads admin command access rights from database<br>
	 */
	private AdminCommandAccessRights()
	{
		java.sql.Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			
			PreparedStatement stmt = con.prepareStatement("SELECT * FROM admin_command_access_rights");
			ResultSet rset = stmt.executeQuery();
			String adminCommand = null;
			String accessLevels = null;
			
			while (rset.next())
			{
				adminCommand = rset.getString("adminCommand");
				accessLevels = rset.getString("accessLevels");
				_adminCommandAccessRights.put(adminCommand, new AdminCommandAccessRight(adminCommand, accessLevels));
			}
			rset.close();
			stmt.close();
		}
		catch (SQLException e)
		{
			_log.warning("AdminCommandAccessRights: Error loading from database:" + e);
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
		
		_log.info("AdminCommandAccessRights: Loaded " + _adminCommandAccessRights.size() + " from database.");
	}

	/**
	 * Returns the one and only instance of this class<br><br>
	 * 
	 * @return AdminCommandAccessRights: the one and only instance of this class<br>
	 */
	public static AdminCommandAccessRights getInstance()
	{
		return _instance == null ? (_instance = new AdminCommandAccessRights()) : _instance;
	}

	public boolean hasAccess(String adminCommand, AccessLevel accessLevel)
	{
		if (!accessLevel.isGm())
			return false;
		
		if (accessLevel.getLevel() == AccessLevels._masterAccessLevelNum)
			return true;
		
		AdminCommandAccessRight acar = _adminCommandAccessRights.get(adminCommand);
		
		if (acar == null)
		{
			_log.info("AdminCommandAccessRights: No rights defined for admin command " + adminCommand + ".");
			return false;
		}
		
		return acar.hasAccess(accessLevel);
	}
}