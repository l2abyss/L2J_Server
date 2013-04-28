/*
 * Copyright (C) 2004-2013 L2J DataPack
 * 
 * This file is part of L2J DataPack.
 * 
 * L2J DataPack is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J DataPack is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.l2jserver.gameserver.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2jserver.L2DatabaseFactory;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author UnAfraid
 */
public class PlayerVariables extends StatsSet
{
	private static final Logger _log = Logger.getLogger(PlayerVariables.class.getName());
	
	// SQL Queries.
	private static final String SELECT_QUERY = "SELECT * FROM character_variables WHERE charId = ?";
	private static final String DELETE_QUERY = "DELETE FROM character_variables WHERE charId = ?";
	private static final String INSERT_QUERY = "INSERT INTO character_variables (charId, var, val) VALUES (?, ?, ?)";
	
	private final int _objectId;
	private final AtomicBoolean _hasChanges = new AtomicBoolean(false);
	
	public PlayerVariables(int objectId)
	{
		_objectId = objectId;
		load();
	}
	
	private void load()
	{
		// Restore previous variables.
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement st = con.prepareStatement(SELECT_QUERY))
		{
			st.setInt(1, _objectId);
			try (ResultSet rset = st.executeQuery())
			{
				while (rset.next())
				{
					super.set(rset.getString("var"), rset.getString("val"));
				}
			}
		}
		catch (SQLException e)
		{
			_log.log(Level.WARNING, getClass().getSimpleName() + ": Couldn't restore variables for: " + getPlayer(), e);
		}
	}
	
	public void store()
	{
		// No changes, nothing to store.
		if (!_hasChanges.get())
		{
			return;
		}
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			// Clear previous entries.
			try (PreparedStatement st = con.prepareStatement(DELETE_QUERY))
			{
				st.setInt(1, _objectId);
				st.execute();
			}
			
			// Insert all variables.
			try (PreparedStatement st = con.prepareStatement(INSERT_QUERY))
			{
				st.setInt(1, _objectId);
				for (Entry<String, Object> entry : getSet().entrySet())
				{
					st.setString(2, entry.getKey());
					st.setString(3, String.valueOf(entry.getValue()));
					st.addBatch();
				}
				st.executeBatch();
			}
		}
		catch (SQLException e)
		{
			_log.log(Level.WARNING, getClass().getSimpleName() + ": Couldn't update variables for: " + getPlayer(), e);
		}
		finally
		{
			_hasChanges.compareAndSet(true, false);
		}
	}
	
	/**
	 * Overriding following methods to prevent from doing useless database operations if there is no changes since player's login.
	 */
	
	@Override
	public void set(String name, boolean value)
	{
		_hasChanges.compareAndSet(false, true);
		super.set(name, value);
	}
	
	@Override
	public void set(String name, double value)
	{
		_hasChanges.compareAndSet(false, true);
		super.set(name, value);
	}
	
	@Override
	public void set(String name, Enum<?> value)
	{
		_hasChanges.compareAndSet(false, true);
		super.set(name, value);
	}
	
	@Override
	public void set(String name, int value)
	{
		_hasChanges.compareAndSet(false, true);
		super.set(name, value);
	}
	
	@Override
	public void set(String name, long value)
	{
		_hasChanges.compareAndSet(false, true);
		super.set(name, value);
	}
	
	@Override
	public void set(String name, String value)
	{
		_hasChanges.compareAndSet(false, true);
		super.set(name, value);
	}
	
	public L2PcInstance getPlayer()
	{
		return L2World.getInstance().getPlayer(_objectId);
	}
}
