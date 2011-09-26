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
package org.netcon;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Forsaiken
 */
public abstract class NetConnectionListener extends Thread
{
	private static final Logger _log = Logger.getLogger(NetConnectionListener.class.getName());
	
	private final NetConnectionConfig _config;
	private final ServerSocket _serverSocket;
	private final Map<String, ForeignConnection> _floodProtection;
	private final List<String> _ipBanns;
	
	protected NetConnectionListener(final NetConnectionConfig config) throws IOException
	{
		_config = config;
		
		if (_config.TCP_IP_BANN_ENABLED)
		{
			_ipBanns = new ArrayList<String>();
			for (final String ip : _config.TCP_IP_BANN_LIST)
			{
				_ipBanns.add(ip);
			}
		}
		else
		{
			_ipBanns = null;
		}
		
		if (_config.TCP_FLOOD_PROTECTION_ENABLED)
		{
			_floodProtection = new HashMap<String, ForeignConnection>();
		}
		else
		{
			_floodProtection = null;
		}
		
		if (_config.TCP_EXTERNAL_HOST_ADDRESS.equals("*"))
		{
			_serverSocket = new ServerSocket(_config.TCP_EXTERNAL_PORT, _config.TCP_CONNECTION_QUEUE);
		}
		else
		{
			_serverSocket = new ServerSocket(_config.TCP_EXTERNAL_PORT, _config.TCP_CONNECTION_QUEUE, InetAddress.getByName(_config.TCP_EXTERNAL_HOST_ADDRESS));
		}
	}
	
	@Override
	public final void run()
	{
		Socket connection = null;
		
		while (true)
		{
			try
			{
				connection = _serverSocket.accept();
				final String connectionAddress = connection.getInetAddress().getHostAddress();
				
				_log.log(Level.INFO, "Received connection: " + connectionAddress);
				
				if (_config.TCP_IP_BANN_ENABLED && _ipBanns.contains(connectionAddress))
				{
					throw new IOException("IP: " + connectionAddress + " is on TCP_IP_BANN_LIST. Closing connection...");
				}
				
				if (_config.TCP_FLOOD_PROTECTION_ENABLED)
				{
					ForeignConnection fConnection = _floodProtection.get(connectionAddress);
					
					if (fConnection != null)
					{
						fConnection.connectionNumber += 1;
						if (((fConnection.connectionNumber > _config.TCP_FAST_CONNECTION_LIMIT) && ((System.currentTimeMillis() - fConnection.lastConnection) < _config.TCP_NORMAL_CONNECTION_TIME)) || ((System.currentTimeMillis() - fConnection.lastConnection) < _config.TCP_FAST_CONNECTION_TIME) || (fConnection.connectionNumber > _config.TCP_MAX_CONNECTION_PER_IP))
						{
							fConnection.lastConnection = System.currentTimeMillis();
							fConnection.connectionNumber -= 1;
							fConnection.isFlooding = true;
							
							throw new IOException("IP: " + connectionAddress + " is marked as Flooding. Closing connection...");
						}
						
						fConnection.lastConnection = System.currentTimeMillis();
						
						if (fConnection.isFlooding)
						{
							fConnection.isFlooding = false;
						}
						
						_log.log(Level.FINE, "IP: " + connectionAddress + " is no longer marked as Flooding.");
					}
					else
					{
						fConnection = new ForeignConnection(System.currentTimeMillis());
						_floodProtection.put(connectionAddress, fConnection);
					}
				}
				
				buildTCPNetConnection(_config, connection);
			}
			catch (IOException e)
			{
				_log.log(Level.WARNING, "", e);
				
				if (connection != null)
				{
					try
					{
						connection.close();
					}
					catch (IOException e1)
					{
						_log.log(Level.WARNING, "Failed closing connection.", e1);
					}
				}
				
				try
				{
					if (isInterrupted())
					{
						close();
					}
				}
				catch (IOException e1)
				{
					_log.log(Level.WARNING, "Failed closing listener.", e1);
				}
			}
		}
	}
	
	public final void close() throws IOException
	{
		_serverSocket.close();
	}
	
	public final void removeTCPNetConnection(final NetConnection connection) throws IOException
	{
		if (!_config.TCP_FLOOD_PROTECTION_ENABLED)
		{
			return;
		}
		
		final String connectionAddress = connection.getConnectionAddress();
		final ForeignConnection fConnection = _floodProtection.get(connectionAddress);
		if (fConnection != null)
		{
			fConnection.connectionNumber -= 1;
			if (fConnection.connectionNumber == 0)
			{
				_floodProtection.remove(connectionAddress);
			}
		}
	}
	
	protected abstract void buildTCPNetConnection(final NetConnectionConfig config, final Socket remoteConnection) throws IOException;
	
	private final class ForeignConnection
	{
		private int connectionNumber;
		private long lastConnection;
		private boolean isFlooding = false;
		
		private ForeignConnection(final long time)
		{
			lastConnection = time;
			connectionNumber = 1;
		}
	}
}
