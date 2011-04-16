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
package com.l2jserver;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jolbox.bonecp.BoneCPDataSource;
import com.l2jserver.gameserver.ThreadPoolManager;

public class L2DatabaseFactory
{
	static Logger _log = Logger.getLogger(L2DatabaseFactory.class.getName());
	
	public static enum ProviderType
	{
		MySql, MsSql
	}
	
	// =========================================================
	// Data Field
	private static ScheduledExecutorService _executor;
	private ProviderType _providerType;
	private BoneCPDataSource _source;
	
	// =========================================================
	// Constructor
	public L2DatabaseFactory()
	{
		_log.info("Initializing BoneCP [ version: databaseDriver -> " + Config.DATABASE_DRIVER + ", jdbcUrl -> " + Config.DATABASE_URL + ", maxConnectionsPerPartition -> " + Config.DATABASE_MAX_CONNECTIONS + ", username -> " + Config.DATABASE_LOGIN + ", password -> " + Config.DATABASE_PASSWORD + " ]");
		try
		{
			_source = new BoneCPDataSource();
			_source.getConfig().setDefaultAutoCommit(true);
			
			_source.getConfig().setPoolAvailabilityThreshold(10);
			_source.getConfig().setMinConnectionsPerPartition(10);
			_source.getConfig().setMaxConnectionsPerPartition(Config.DATABASE_MAX_CONNECTIONS);
			
			_source.setPartitionCount(3);
			
			_source.setAcquireRetryAttempts(0); // try to obtain connections indefinitely (0 = never quit)
			_source.setAcquireRetryDelayInMs(500); // 500 miliseconds wait before try to acquire connection again
			
			// if pool is exhausted
			_source.setAcquireIncrement(5); // if pool is exhausted, get 5 more connections at a time
			// cause there is a "long" delay on acquire connection
			// so taking more than one connection at once will make connection pooling
			// more effective.
			
			_source.setConnectionTimeoutInMs(0);
			
			// testing OnCheckin used with IdleConnectionTestPeriod is faster than testing on checkout
			
			_source.setIdleConnectionTestPeriodInMinutes(1); // test idle connection every 60 sec
			_source.setIdleMaxAgeInSeconds(1800);
			
			_source.setTransactionRecoveryEnabled(true);
			
			_source.setDriverClass(Config.DATABASE_DRIVER);
			_source.setJdbcUrl(Config.DATABASE_URL);
			_source.setUsername(Config.DATABASE_LOGIN);
			_source.setPassword(Config.DATABASE_PASSWORD);
			
			/* Test the connection */
			_source.getConnection().close();
			
			if (Config.DEBUG)
				_log.fine("Database Connection Working");
			
			if (Config.DATABASE_DRIVER.toLowerCase().contains("microsoft"))
				_providerType = ProviderType.MsSql;
			else
				_providerType = ProviderType.MySql;
		}
		catch (Exception e)
		{
			if (Config.DEBUG)
				_log.fine("Database Connection FAILED");
			throw new Error("L2DatabaseFactory: Failed to init database connections: " + e.getMessage(), e);
		}
	}
	
	// =========================================================
	// Method - Public
	public final String prepQuerySelect(String[] fields, String tableName, String whereClause, boolean returnOnlyTopRecord)
	{
		String msSqlTop1 = "";
		String mySqlTop1 = "";
		if (returnOnlyTopRecord)
		{
			if (getProviderType() == ProviderType.MsSql)
				msSqlTop1 = " Top 1 ";
			if (getProviderType() == ProviderType.MySql)
				mySqlTop1 = " Limit 1 ";
		}
		String query = "SELECT " + msSqlTop1 + safetyString(fields) + " FROM " + tableName + " WHERE " + whereClause + mySqlTop1;
		return query;
	}
	
	public void shutdown()
	{
		try
		{
			_source.close();
			_source = null;
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, e.getMessage(), e);
		}
	}
	
	public final String safetyString(String... whatToCheck)
	{
		// NOTE: Use brace as a safty precaution just incase name is a reserved word
		final char braceLeft;
		final char braceRight;
		
		if (getProviderType() == ProviderType.MsSql)
		{
			braceLeft = '[';
			braceRight = ']';
		}
		else
		{
			braceLeft = '`';
			braceRight = '`';
		}
		
		int length = 0;
		
		for (String word : whatToCheck)
		{
			length += word.length() + 4;
		}
		
		final StringBuilder sbResult = new StringBuilder(length);
		
		for (String word : whatToCheck)
		{
			if (sbResult.length() > 0)
			{
				sbResult.append(", ");
			}
			
			sbResult.append(braceLeft);
			sbResult.append(word);
			sbResult.append(braceRight);
		}
		
		return sbResult.toString();
	}
	
	public Connection getConnection()
	{
		Connection con = null;
		
		while (con == null)
		{
			try
			{
				con = _source.getConnection();
				if (Server.serverMode == Server.MODE_GAMESERVER)
					ThreadPoolManager.getInstance().scheduleGeneral(new ConnectionCloser(con, new RuntimeException()), 60000);
				else
					getExecutor().schedule(new ConnectionCloser(con, new RuntimeException()), 60, TimeUnit.SECONDS);
			}
			catch (SQLException e)
			{
				_log.log(Level.WARNING, "L2DatabaseFactory: getConnection() failed, trying again " + e.getMessage(), e);
			}
		}
		return con;
	}
	
	private static class ConnectionCloser implements Runnable
	{
		private Connection c;
		private RuntimeException exp;
		
		public ConnectionCloser(Connection con, RuntimeException e)
		{
			c = con;
			exp = e;
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run()
		{
			try
			{
				if (!c.isClosed())
				{
					_log.log(Level.WARNING, "Unclosed connection! Trace: " + exp.getStackTrace()[1], exp);
				}
			}
			catch (SQLException e)
			{
				_log.log(Level.WARNING, "", e);
			}
			
		}
	}
	
	public static void close(Connection con)
	{
		if (con == null)
			return;
		
		try
		{
			con.close();
		}
		catch (SQLException e)
		{
			_log.log(Level.WARNING, "Failed to close database connection!", e);
		}
	}
	
	private static ScheduledExecutorService getExecutor()
	{
		if (_executor == null)
		{
			synchronized (L2DatabaseFactory.class)
			{
				if (_executor == null)
					_executor = Executors.newSingleThreadScheduledExecutor();
			}
		}
		return _executor;
	}
	
	public int getBusyConnectionCount()
	{
		return _source.getTotalLeased();
	}
	
	public final ProviderType getProviderType()
	{
		return _providerType;
	}
	
	private static final class SingletonHolder
	{
		private static final L2DatabaseFactory INSTANCE = new L2DatabaseFactory();
	}
	
	public static L2DatabaseFactory getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
}
