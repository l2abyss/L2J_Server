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
package com.l2jserver.communityserver;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.mchange.v2.c3p0.ComboPooledDataSource;

public class L2DatabaseFactory
{
    static Logger _log = Logger.getLogger(L2DatabaseFactory.class.getName());

    public static enum ProviderType
    {
        MySql,
        MsSql
    }
    
    private static L2DatabaseFactory _instance;
    private final ProviderType _providerType;
	private final ComboPooledDataSource _source;
	
	public L2DatabaseFactory() throws SQLException
	{
		try
		{
			if (Config.DATABASE_MAX_CONNECTIONS < 2)
            {
                Config.DATABASE_MAX_CONNECTIONS = 2;
                _log.warning("at least " + Config.DATABASE_MAX_CONNECTIONS + " db connections are required.");
            }
			
			_source = new ComboPooledDataSource();
			_source.setAutoCommitOnClose(true);
			_source.setInitialPoolSize(10);
			_source.setMinPoolSize(10);
			_source.setMaxPoolSize(Config.DATABASE_MAX_CONNECTIONS);
			_source.setAcquireRetryAttempts(0);
			_source.setAcquireRetryDelay(500);
			_source.setCheckoutTimeout(0);
			_source.setAcquireIncrement(5);
			_source.setAutomaticTestTable("connection_test_table");
			_source.setTestConnectionOnCheckin(false);
			_source.setIdleConnectionTestPeriod(3600);
			_source.setMaxIdleTime(0);
			_source.setMaxStatementsPerConnection(100);
			_source.setBreakAfterAcquireFailure(false);
			_source.setDriverClass(Config.DATABASE_DRIVER);
			_source.setJdbcUrl(Config.DATABASE_URL);
			_source.setUser(Config.DATABASE_LOGIN);
			_source.setPassword(Config.DATABASE_PASSWORD);
			_source.getConnection().close();
			
			if (Config.DATABASE_DRIVER.toLowerCase().contains("microsoft"))
                _providerType = ProviderType.MsSql;
            else
                _providerType = ProviderType.MySql;
		}
		catch (SQLException x)
		{
			_log.fine("Database Connection FAILED");
			throw x;
		}
		catch (Exception e)
		{
			_log.fine("Database Connection FAILED");
			throw new SQLException("could not init DB connection: " + e);
		}
	}
	
    public final String prepQuerySelect(String[] fields, String tableName, String whereClause, boolean returnOnlyTopRecord)
    {
        String msSqlTop1 = "";
        String mySqlTop1 = "";
        if (returnOnlyTopRecord)
        {
            if (getProviderType() == ProviderType.MsSql) msSqlTop1 = " Top 1 ";
            if (getProviderType() == ProviderType.MySql) mySqlTop1 = " Limit 1 ";
        }
        String query = "SELECT " + msSqlTop1 + safetyString(fields) + " FROM " + tableName + " WHERE " + whereClause + mySqlTop1;
        return query;
    }

    public final void shutdown()
    {
        try
        {
            _source.close();
        }
        catch (Exception e)
        {
        	_log.log(Level.INFO, "", e);
        }
    }

    public final String safetyString(String[] whatToCheck)
    {
        String braceLeft = "`";
        String braceRight = "`";
        if (getProviderType() == ProviderType.MsSql)
        {
            braceLeft = "[";
            braceRight = "]";
        }

        String result = "";
        for (String word : whatToCheck)
        {
            if (result != "")
            	result += ", ";
            result += braceLeft + word + braceRight;
        }
        return result;
    }
    
	public static final L2DatabaseFactory getInstance() throws SQLException
	{
		if (_instance == null)
			_instance = new L2DatabaseFactory();
		
		return _instance;
	}

	public final Connection getConnection()
	{
		Connection con = null;
		while (con == null)
		{
			try
			{
				con = _source.getConnection();
			}
			catch (SQLException e)
			{
				_log.warning("L2DatabaseFactory: getConnection() failed, trying again "+e);
			}
		}
		return con;
	}

	public final int getBusyConnectionCount() throws SQLException
	{
	    return _source.getNumBusyConnectionsDefaultUser();
	}

	public final int getIdleConnectionCount() throws SQLException
	{
	    return _source.getNumIdleConnectionsDefaultUser();
	}

    public final ProviderType getProviderType()
    {
    	return _providerType;
    }
}