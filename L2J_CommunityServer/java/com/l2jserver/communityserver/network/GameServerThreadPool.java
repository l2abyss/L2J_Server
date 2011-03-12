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

import javolution.util.FastMap;

public final class GameServerThreadPool
{
	private static GameServerThreadPool _instance;
	
	public static final GameServerThreadPool getInstance()
	{
		if (_instance == null)
			_instance = new GameServerThreadPool();
		return _instance;
	}
	
	private final FastMap<Integer, GameServerThread> _gameServerThreads;
	
	private GameServerThreadPool()
	{
		_gameServerThreads = new FastMap<Integer, GameServerThread>();
	}
	
	public final void addGameServerThread(final int serverId, final GameServerThread gst)
	{
		synchronized (_gameServerThreads)
		{
			_gameServerThreads.put(serverId, gst);
		}
	}
	
	public final GameServerThread getGameServerThread(final int serverId)
	{
		return _gameServerThreads.get(serverId);
	}
}