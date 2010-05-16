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
package com.l2jserver.gameserver.network.communityserver;

import java.io.IOException;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2jserver.Config;
import com.l2jserver.gameserver.ThreadPoolManager;
import com.l2jserver.gameserver.network.communityserver.readpackets.AuthResponse;
import com.l2jserver.gameserver.network.communityserver.readpackets.ClanNoticeInfo;
import com.l2jserver.gameserver.network.communityserver.readpackets.ConnectionError;
import com.l2jserver.gameserver.network.communityserver.readpackets.InitCS;
import com.l2jserver.gameserver.network.communityserver.readpackets.RequestPlayerShowBoard;
import com.l2jserver.gameserver.network.communityserver.readpackets.RequestPlayerShowMessage;
import com.l2jserver.gameserver.network.communityserver.readpackets.RequestWorldInfo;

import org.netcon.BaseReadPacket;
import org.netcon.BaseWritePacket;
import org.netcon.NetConnection;
import org.netcon.NetConnectionConfig;

/**
 * @authors  Forsaiken, Gigiikun
 */
public final class CommunityServerThread extends NetConnection
{
	private static final Logger _log = Logger.getLogger(CommunityServerThread.class.getName());
	
	private static CommunityServerThread _instance;
	
	public static final void initialize()
	{
		if (_instance == null)
		{
			if (Config.ENABLE_COMMUNITY_BOARD)
			{
				try
				{
					_instance = new CommunityServerThread(new NetConnectionConfig(Config.COMMUNITY_CONFIGURATION_FILE));
					_instance.start();
				}
				catch (Exception e)
				{
					_log.log(Level.WARNING, "CommunityServerThread: Failed loading config file! " + e.getMessage(), e);
				}
			}
			else
			{
				_log.log(Level.INFO, "CommunityServerThread: Deactivated by config.");
				_instance = new CommunityServerThread(null);
			}
		}
	}
	
	public static final CommunityServerThread getInstance()
	{
		return _instance;
	}
	
	private boolean _authed;
	
	private CommunityServerThread(final NetConnectionConfig config)
	{
		super(config);
	}
	
	public final boolean isAuthed()
	{
		return _authed;
	}
	
	public final void setAuthed(final boolean authed)
	{
		_authed = authed;
	}
	
	public final void forceClose(final BaseWritePacket packet)
	{
		_authed = false;
		
		try
		{
			super.close(packet);
		}
		catch (IOException e)
		{
			_log.log(Level.INFO, "CommunityServerThread: Failed disconnecting server, server already disconnected: " + e.getMessage(), e);
		}
	}
	
	public boolean sendPacket(final BaseWritePacket packet)
	{
		return sendPacket(packet, true);
	}
	
	public boolean sendPacket(final BaseWritePacket packet, final boolean needAuth)
	{
		if (needAuth && !_authed)
			return false;
		
		try
		{
			super.write(packet);
		}
		catch (IOException e)
		{
			_log.log(Level.INFO, "CommunityServerThread: Failed sending TCP packet: " + e.getMessage(), e);
			return false;
		}
		return true;
	}
	
	@Override
	public void run()
	{
		_log.log(Level.INFO, "CommunityServerThread: Activated by config.");
		
		int packetType1 = 0xFF;
		int packetType2 = 0xFF;
		
		BaseReadPacket packet = null;
		byte[] data = null;
		
		while (!isInterrupted())
		{
			try
			{
				super.sleep(10000L);
			}
			catch (InterruptedException e)
			{
				return;
			}
			
			_log.log(Level.INFO, "CommunityServerThread: Trying to connect to " + Config.COMMUNITY_SERVER_ADDRESS + " on port " + Config.COMMUNITY_SERVER_PORT + ".");
			
			try
			{
				_instance.connect(Config.COMMUNITY_SERVER_ADDRESS, Config.COMMUNITY_SERVER_PORT);
			}
			catch (SocketException se)
			{
				_log.log(Level.INFO, "CommunityServerThread: Connecting to " + Config.COMMUNITY_SERVER_ADDRESS + " on port " + Config.COMMUNITY_SERVER_PORT + " failed.");
				continue;
			}
			catch (IOException e)
			{
				_log.log(Level.INFO, "CommunityServerThread: Connection failed: " + e.getMessage(), e);
				continue;
			}

			
			try
			{
				long gameServerConnectStart = System.currentTimeMillis();
				while (!isInterrupted())
				{
					data = super.read();
					packetType1 = data[0] & 0xFF;
					packetType2 = data[1] & 0xFF;
					
					if (Config.PACKET_HANDLER_DEBUG)
						_log.log(Level.INFO, "Received packet: 0x" + Integer.toHexString(packetType1) + "-0x" + Integer.toHexString(packetType2));
					
					switch (packetType1)
					{
						case 0x00:
						{
							switch (packetType2)
							{
								case 0x00:
									packet = new InitCS(data, this);
									break;
									
								case 0x01:
									_log.info("Server connected in " + ((System.currentTimeMillis() - gameServerConnectStart) / 1000) + " seconds");
									packet = new AuthResponse(data, this);
									break;
									
								case 0x02:
									packet = new ConnectionError(data);
									break;
							}
							break;
						}
						
						case 0x01:
						{
							switch (packetType2)
							{
								case 0x00:
									packet = new RequestWorldInfo(data, this, RequestWorldInfo.SERVER_LOAD);
									break;
								case 0x01:
									packet = new RequestWorldInfo(data, this, RequestWorldInfo.PLAYER_DATA_UPDATE);
									break;
								case 0x02:
									packet = new RequestWorldInfo(data, this, RequestWorldInfo.CLAN_DATA_UPDATE);
									break;
								case 0x03:
									packet = new ClanNoticeInfo(data, 0);
									break;
								case 0x04:
									packet = new ClanNoticeInfo(data, 1);
									break;
								case 0x05:
									packet = new ClanNoticeInfo(data, this, 2);
									break;
							}
							break;
						}
						
						case 0x02:
						{
							switch (packetType2)
							{
								case 0x00:
									packet = new RequestPlayerShowBoard(data);
									break;
								case 0x01:
									packet = new RequestPlayerShowMessage(data);
									break;
							}
							break;
						}
					}
					
					if (packet != null)
						//new Thread(packet).start();
						ThreadPoolManager.getInstance().executeCommunityPacket(packet);
					else
						throw new IOException("Invalid packet!");
				}
				if (isInterrupted())
					forceClose(null);
			}
			catch (IOException e)
			{
				_log.log(Level.WARNING, "CommunityServerThread: TCP Connection lost: " + e.getMessage(), e);
				
				forceClose(null);
			}
		}
	}
}
