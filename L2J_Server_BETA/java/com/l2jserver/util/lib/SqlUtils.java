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
package com.l2jserver.util.lib;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2jserver.L2DatabaseFactory;

public class SqlUtils
{
	private static Logger _log = Logger.getLogger(SqlUtils.class.getName());
	
	public static SqlUtils getInstance()
	{
		return SingletonHolder._instance;
	}
	
	public static Integer getIntValue(String resultField, String tableName, String whereClause)
	{
		String query = "";
		Integer res = null;
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			query = L2DatabaseFactory.getInstance().prepQuerySelect(new String[]
			{
				resultField
			}, tableName, whereClause, true);
			
			try (PreparedStatement ps = con.prepareStatement(query);
				ResultSet rs = ps.executeQuery())
			{
				if (rs.next())
				{
					res = rs.getInt(1);
				}
			}
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Error in query '" + query + "':", e);
		}
		return res;
	}
	
	public static Integer[] getIntArray(String resultField, String tableName, String whereClause)
	{
		String query = "";
		Integer[] res = null;
		try
		{
			query = L2DatabaseFactory.getInstance().prepQuerySelect(new String[]
			{
				resultField
			}, tableName, whereClause, false);
			
			try (Connection con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement ps = con.prepareStatement(query);
				ResultSet rs = ps.executeQuery())
			{
				int rows = 0;
				while (rs.next())
				{
					rows++;
				}
				
				if (rows == 0)
				{
					return new Integer[0];
				}
				
				res = new Integer[rows - 1];
				
				rs.first();
				
				int row = 0;
				while (rs.next())
				{
					res[row] = rs.getInt(1);
				}
			}
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "mSGI: Error in query '" + query + "':", e);
		}
		return res;
	}
	
	public static Integer[][] get2DIntArray(String[] resultFields, String usedTables, String whereClause)
	{
		long start = System.currentTimeMillis();
		String query = "";
		Integer res[][] = null;
		try
		{
			query = L2DatabaseFactory.getInstance().prepQuerySelect(resultFields, usedTables, whereClause, false);
			try (Connection con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement ps = con.prepareStatement(query);
				ResultSet rs = ps.executeQuery())
			{
				int rows = 0;
				while (rs.next())
				{
					rows++;
				}
				
				res = new Integer[rows - 1][resultFields.length];
				
				rs.first();
				
				int row = 0;
				while (rs.next())
				{
					for (int i = 0; i < resultFields.length; i++)
					{
						res[row][i] = rs.getInt(i + 1);
					}
					row++;
				}
			}
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Error in query '" + query + "':", e);
		}
		_log.fine("Get all rows in query '" + query + "' in " + (System.currentTimeMillis() - start) + "ms");
		return res;
	}
	
	private static class SingletonHolder
	{
		protected static final SqlUtils _instance = new SqlUtils();
	}
}
