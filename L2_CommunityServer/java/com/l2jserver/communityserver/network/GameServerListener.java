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
import java.net.Socket;
import java.util.List;

import javolution.util.FastList;
import com.l2jserver.communityserver.network.netcon.NetConnectionConfig;
import com.l2jserver.communityserver.network.netcon.NetConnectionListener;

public final class GameServerListener extends NetConnectionListener
{
	private static List<GameServerThread> _gameServers = new FastList<GameServerThread>();

	public GameServerListener(final NetConnectionConfig config) throws IOException
	{
		super(config);
	}
	
	@Override
	protected final void buildTCPNetConnection(final NetConnectionConfig config, final Socket remoteConnection) throws IOException
	{
		final GameServerThread gst = new GameServerThread(config);
		
		gst.connect(remoteConnection);
		gst.start();
		
		_gameServers.add(gst);
	}
}