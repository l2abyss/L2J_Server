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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public final class NetConnectionConfig
{
	public NetConnectionConfig(final String configFilePath) throws Exception
	{
		final Properties settings = new Properties();
		final InputStream is = new FileInputStream(new File(configFilePath));
		settings.load(is);
		is.close();
		
		INITIAL_CRYPT = settings.getProperty("InitialCrypt", "_;v.]05-31!|+-%xT!^[$\00");
		
		TCP_EXTERNAL_HOST_ADDRESS = settings.getProperty("ExternalHostAddress", "*");
		TCP_EXTERNAL_PORT = Integer.parseInt(settings.getProperty("ExternalPort", "0"));
		TCP_CONNECTION_QUEUE = Integer.parseInt(settings.getProperty("ConnectionQueue", "50"));
		
		TCP_FLOOD_PROTECTION_ENABLED = Boolean.parseBoolean(settings.getProperty("FloodProtectionEnabled", "False"));
		TCP_FAST_CONNECTION_LIMIT = Integer.parseInt(settings.getProperty("FastConnectionLimit", "15"));
		TCP_FAST_CONNECTION_TIME = Integer.parseInt(settings.getProperty("FastConnectionTime", "350"));
		TCP_NORMAL_CONNECTION_TIME = Integer.parseInt(settings.getProperty("NormalConnectionTime", "700"));
		TCP_MAX_CONNECTION_PER_IP = Integer.parseInt(settings.getProperty("MaxConnectionperIP", "50"));
		TCP_IP_BANN_ENABLED = Boolean.parseBoolean(settings.getProperty("IPBannEnabled", "False"));
		TCP_IP_BANN_LIST = settings.getProperty("IPBannList", "").split(":");
		
		TCP_SEND_BUFFER_SIZE = Integer.parseInt(settings.getProperty("SendBufferSize", "8192"));
		TCP_RECEIVE_BUFFER_SIZE = Integer.parseInt(settings.getProperty("ReceiveBufferSize", "8192"));
		
		if (TCP_SEND_BUFFER_SIZE < 1024)
		{
			throw new IllegalArgumentException("Init: TCP_SEND_BUFFER_SIZE < 1024");
		}
		
		if (TCP_RECEIVE_BUFFER_SIZE < 1024)
		{
			throw new IllegalArgumentException("Init: TCP_RECEIVE_BUFFER_SIZE < 2048");
		}
	}
	
	/**
	 * ##################################################<br>
	 * Crypt<br>
	 * ##################################################
	 */
	
	public final String INITIAL_CRYPT;
	
	/**
	 * ##################################################<br>
	 * TCP Server start<br>
	 * ##################################################
	 */
	
	/** TCP external host address default '*' */
	public final String TCP_EXTERNAL_HOST_ADDRESS;
	/** TCP external port default 0 */
	public final int TCP_EXTERNAL_PORT;
	/** TCP external port default 50 */
	public final int TCP_CONNECTION_QUEUE;
	
	/** TCP FloodProtection default false */
	public final boolean TCP_FLOOD_PROTECTION_ENABLED;
	/** TCP Fast connection limit default 15 */
	public final int TCP_FAST_CONNECTION_LIMIT;
	/** TCP Fast connection time default 350 */
	public final int TCP_FAST_CONNECTION_TIME;
	/** TCP Normal connection time default 700 */
	public final int TCP_NORMAL_CONNECTION_TIME;
	/** TCP Max connection per IP 50 */
	public final int TCP_MAX_CONNECTION_PER_IP;
	
	/** TCP IP ban default false */
	public final boolean TCP_IP_BANN_ENABLED;
	/** TCP IP ban list default empty */
	public final String[] TCP_IP_BANN_LIST;
	
	/**
	 * ##################################################<br>
	 * TCP Client start<br>
	 * ##################################################
	 */
	
	/** TCP send buffer size default 8192 */
	public final int TCP_SEND_BUFFER_SIZE;
	/** TCP receive buffer size default 8192 */
	public final int TCP_RECEIVE_BUFFER_SIZE;
}
