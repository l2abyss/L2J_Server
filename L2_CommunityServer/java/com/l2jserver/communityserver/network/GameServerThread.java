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
package com.l2jserver.communityserver.network;

import java.io.IOException;
import java.net.SocketException;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2jserver.communityserver.GameServerRegistrationTable;
import com.l2jserver.communityserver.communityboard.CommunityBoardManager;
import com.l2jserver.communityserver.network.netcon.BaseReadPacket;
import com.l2jserver.communityserver.network.netcon.BaseWritePacket;
import com.l2jserver.communityserver.network.netcon.NetConnection;
import com.l2jserver.communityserver.network.netcon.NetConnectionConfig;
import com.l2jserver.communityserver.network.readpackets.BlowFishKey;
import com.l2jserver.communityserver.network.readpackets.GameServerAuth;
import com.l2jserver.communityserver.network.readpackets.WorldInfo;
import com.l2jserver.communityserver.network.readpackets.RequestShowCommunityBoard;
import com.l2jserver.communityserver.network.readpackets.RequestWriteCommunityBoard;
import com.l2jserver.communityserver.network.writepackets.AuthResponse;
import com.l2jserver.communityserver.network.writepackets.CommunityServerFail;
import com.l2jserver.communityserver.network.writepackets.InitCS;
import com.l2jserver.communityserver.network.writepackets.RequestWorldInfo;

/**
 * @author Forsaiken
 */

public class GameServerThread extends NetConnection
{
	protected static final Logger _log = Logger.getLogger(GameServerThread.class.getName());
	
	private final RSAPublicKey _publicKey;
	private final RSAPrivateKey _privateKey;
	
	private CommunityBoardManager _communityBoardManager;
	private boolean _isAuthed;
	private loadingData _loadingTask;
	
	public GameServerThread(final NetConnectionConfig config) throws IOException
	{
		super(config);
		final KeyPair pair = GameServerRegistrationTable.getInstance().getRandomKeyPair();
		_privateKey = (RSAPrivateKey)pair.getPrivate();
		_publicKey = (RSAPublicKey)pair.getPublic();
	}
	
	@Override
	public final void run()
	{
		int packetType1 = 0xFF;
		int packetType2 = 0xFF;
		
		byte[] data = null;
		BaseReadPacket packet = null;
		
		try
		{
			sendPacket(new InitCS(_publicKey.getModulus().toByteArray()));
			
			while (true)
			{
				data = super.read();
				packetType1 = data[0] & 0xFF;
				packetType2 = data[1] & 0xFF;
				
				// _log.log(Level.INFO, "Received packet: 0x" + Integer.toHexString(packetType1) + "-0x" + Integer.toHexString(packetType2));
				
				switch (packetType1)
				{
					case 0x00:
					{
						switch (packetType2)
						{
							case 0x00:
								packet = new BlowFishKey(data, _privateKey, this);
								break;
							
							case 0x01:
								packet = new GameServerAuth(data, this);
								break;
						}
						break;
					}
					
					case 0x01:
					{
						if (!_isAuthed)
						{
							forceClose(new CommunityServerFail(CommunityServerFail.NOT_AUTHED));
							return;
						}
						
						if (_loadingTask != null)
							_loadingTask.addPacket();
						packet = new WorldInfo(data, this, packetType2);
						/*
						switch (packetType2)
						{
							case 0x00:
								packet = new WorldInfo(data, this, packetType2);
								break;

							case 0x01:
								packet = new WorldInfo(data, this, 1);
								break;
								
							case 0x02:
								packet = new WorldInfo(data, this, 2);
								break;
								
							case 0x03:
								packet = new PlayerUpdate(data, this);
								break;
								
							case 0x04:
								// clan update
								break;
								
							case 0x05:
								// clan created
								break;
								
							case 0x06:
								// clan deleted
								break;
						}*/
						break;
					}
					
					case 0x02:
					{
						if (!_isAuthed)
						{
							forceClose(new CommunityServerFail(CommunityServerFail.NOT_AUTHED));
							return;
						}
						
						switch (packetType2)
						{
							case 0x00:
								packet = new RequestShowCommunityBoard(data, this);
								break;
							case 0x01:
								packet = new RequestWriteCommunityBoard(data, this);
								break;
						}
						
						break;
					}
				}
				
				if (packet != null)
					new Thread(packet).start();
				else
					throw new IOException("Invalid packet!");
			}
		}
		catch (SocketException se)
		{
			forceClose(null);
		}
		catch (IOException e)
		{
			_log.log(Level.INFO, "Connection lost!");
			
			e.printStackTrace();
			
			forceClose(new CommunityServerFail(CommunityServerFail.NOT_AUTHED));
		}
	}
	
	public final CommunityBoardManager getCommunityBoardManager()
	{
		return _communityBoardManager;
	}
	
	private final void setCommunityBoardManager(final CommunityBoardManager communityBoardManager)
	{
		if (_communityBoardManager != null)
			_communityBoardManager.setGST(null);
		
		_communityBoardManager = communityBoardManager;
		
		if (_communityBoardManager != null)
			_communityBoardManager.setGST(this);
	}
	
	public final void sendPacket(final BaseWritePacket packet)
	{
		try
		{
			super.write(packet);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public final void forceClose(final BaseWritePacket packet)
	{
		try
		{
			super.close(packet);
		}
		catch (IOException e)
		{
			_log.finer("GameServerThread: Failed disconnecting server, server already disconnected.");
		}
		finally
		{
			setCommunityBoardManager(null);
		}
	}
	
	public final void onGameServerAuth(final byte[] hexId, final int sqlDPId)
	{
		if (!GameServerRegistrationTable.getInstance().isHexIdOk(hexId))
		{
			_log.log(Level.INFO, "Invalid HexId. Closing connection...");
			forceClose(null);
			return;
		}
		else if (GameServerRegistrationTable.getInstance().isHexIdInUse(hexId))
		{
			_log.log(Level.INFO, "HexId allready in use. Closing connection...");
			forceClose(null);
			return;
		}
		
		final CommunityBoardManager communityBoardManager = CommunityBoardManager.getInstance(sqlDPId);
		if (communityBoardManager.getGST() != null)
		{
			try
			{
				if (communityBoardManager.getGST().isConnected())
				{
					_log.log(Level.INFO, "SQLDPId allready in use. Closing connection...");
					forceClose(null);
					return;
				}
			}
			catch (IOException e)
			{
				_log.log(Level.INFO, "Exception. Closing connection...", e);
				e.printStackTrace();
				forceClose(null);
				return;
			}
		}
		
		GameServerRegistrationTable.getInstance().setHexIdInUse(hexId);
		setCommunityBoardManager(communityBoardManager);
		_isAuthed = true;
		
		_log.log(Level.INFO, "GameServer connected!");
		if (communityBoardManager.isLoaded())
		{
			sendPacket(new AuthResponse(AuthResponse.AUTHED));
		}
		else
		{
			communityBoardManager.clean();
			_log.log(Level.INFO, "Transfering needed data for CommunityServer!");
			_loadingTask = new loadingData(this);
			new Thread(_loadingTask).start();
			sendPacket(new RequestWorldInfo(RequestWorldInfo.SERVER_LOAD, 0, null, false));
		}
	}
	
	public void addNeededPacket(int val)
	{
		if (_loadingTask != null)
		{
			_loadingTask.sendedPacket(val);
		}
	}

	private final class loadingData implements Runnable
	{
		private  GameServerThread _gst;
		private int _incomePacket;
		private int _neededPacket;
		
		private loadingData(GameServerThread gst)
		{
			_gst = gst;
			_neededPacket = -1;
			_incomePacket = 0;
		}

		public final void run()
		{
			try
			{
				int i = 0;
				while ((_neededPacket == -1 || _incomePacket < _neededPacket) && i++ < 15)
				{
					currentThread();
					Thread.sleep(1000);
				}
			}
			catch (InterruptedException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (_neededPacket == -1)
				_gst.forceClose(null);
			else if (_incomePacket != _neededPacket)
				_gst.forceClose(null);
			else
			{
				_log.info("CB successfully get " + _gst.getCommunityBoardManager().getPlayerList().size() + " player(s) data.");
				_log.info("CB successfully get " + _gst.getCommunityBoardManager().getClanList().size() + " clan(s) data.");
				_log.info("CB successfully get " + _gst.getCommunityBoardManager().getCastleList().size() + " castle(s) data.");
				_gst.getCommunityBoardManager().setLoaded();
				_gst.sendPacket(new AuthResponse(AuthResponse.AUTHED));
			}
			_loadingTask = null;
		}
		
		public void addPacket()
		{
			_incomePacket++;
		}
		
		public void sendedPacket(int val)
		{
			_neededPacket = val;
		}
	}
}