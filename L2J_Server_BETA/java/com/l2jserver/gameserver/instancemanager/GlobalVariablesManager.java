/*
 * Copyright (C) 2004-2013 L2J Server
 * 
 * This file is part of L2J Server.
 * 
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.l2jserver.gameserver.instancemanager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;
import java.util.logging.Logger;

import com.l2jserver.L2DatabaseFactory;
import com.l2jserver.util.L2FastMap;

/**
 * @author Gigiikun
 */
public class GlobalVariablesManager
{
	private static final Logger _log = Logger.getLogger(GlobalVariablesManager.class.getName());
	
	private static final String LOAD_VAR = "SELECT var,value FROM global_variables";
	private static final String SAVE_VAR = "INSERT INTO global_variables (var,value) VALUES (?,?) ON DUPLICATE KEY UPDATE value=?";
	
	private final Map<String, String> _variablesMap = new L2FastMap<>(true);
	
	protected GlobalVariablesManager()
	{
		loadVars();
	}
	
	private final void loadVars()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			Statement statement = con.createStatement();
			ResultSet rset = statement.executeQuery(LOAD_VAR))
		{
			String var, value;
			while (rset.next())
			{
				var = rset.getString(1);
				value = rset.getString(2);
				
				_variablesMap.put(var, value);
			}
		}
		catch (Exception e)
		{
			_log.warning(getClass().getSimpleName() + ": problem while loading variables: " + e);
		}
	}
	
	public final void saveVars()
	{
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(SAVE_VAR))
		{
			for (String var : _variablesMap.keySet())
			{
				statement.setString(1, var);
				statement.setString(2, _variablesMap.get(var));
				statement.setString(3, _variablesMap.get(var));
				statement.execute();
				statement.clearParameters();
			}
		}
		catch (Exception e)
		{
			_log.warning(getClass().getSimpleName() + ": problem while saving variables: " + e);
		}
	}
	
	public void storeVariable(String var, String value)
	{
		_variablesMap.put(var, value);
	}
	
	public boolean isVariableStored(String var)
	{
		return _variablesMap.containsKey(var);
	}
	
	public String getStoredVariable(String var)
	{
		return _variablesMap.get(var);
	}
	
	public static final GlobalVariablesManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final GlobalVariablesManager _instance = new GlobalVariablesManager();
	}
}