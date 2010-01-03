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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import com.l2jserver.communityserver.cache.HtmCache;
import com.l2jserver.communityserver.network.GameServerListener;
import com.l2jserver.communityserver.network.netcon.NetConnectionConfig;
import com.l2jserver.communityserver.threading.ThreadPoolManager;
import com.l2jserver.communityserver.Shutdown;

public final class L2CommunityServer
{
	private static final Logger _log = Logger.getLogger(L2CommunityServer.class.getName());
	
	private static L2CommunityServer _instance;
	
	public static final L2CommunityServer getInstance()
	{
		return _instance;
	}
	
	public static final void main(final String[] args)
	{
		_instance = new L2CommunityServer();
	}
	
	private GameServerListener _listener;
	private final Shutdown _shutdownHandler;
	
	public L2CommunityServer()
	{
		final String LOG_FOLDER = "log";
		final String LOG_NAME   = "./log.cfg";
		
		File logFolder = new File(Config.DATAPACK_ROOT, LOG_FOLDER);
		logFolder.mkdir();
		
		InputStream is = null;
		try
		{
			is = new FileInputStream(new File(LOG_NAME));
			LogManager.getLogManager().readConfiguration(is);
			is.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if (is != null)
				{
					is.close();
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		
		Config.load();
		
		_shutdownHandler = Shutdown.getInstance();
		Runtime.getRuntime().addShutdownHook(_shutdownHandler);
		ThreadPoolManager.init();
		
		GameServerRegistrationTable.getInstance();
		
		try
		{
			L2DatabaseFactory.getInstance();
		}
		catch (SQLException e)
		{
			_log.severe("FATAL: Failed initializing database. Reason: "+e.getMessage());
			System.exit(1);
		}
		
		// load htm cache
		HtmCache.getInstance();
		try
		{
			_listener = new GameServerListener(new NetConnectionConfig(Config.CONFIGURATION_FILE));
			_listener.start();
			_log.info("Listening for GameServers on " + Config.GAME_SERVER_LOGIN_HOST + ":" + Config.GAME_SERVER_LOGIN_PORT);
		}
		catch (Exception e)
		{
			_log.severe("FATAL: Failed to start the Game Server Listener. Reason: " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
	}
}