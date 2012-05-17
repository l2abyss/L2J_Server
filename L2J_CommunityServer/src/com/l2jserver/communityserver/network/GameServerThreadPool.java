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

import java.util.HashMap;
import java.util.Map;

public final class GameServerThreadPool
{
	private final Map<Integer, GameServerThread> _gameServerThreads = new HashMap<>();
	
	/**
	 * Using a version of the double-locking check.
	 * @param serverId
	 * @param gst
	 */
	public final void addGameServerThread(final int serverId, final GameServerThread gst)
	{
		if (!_gameServerThreads.containsKey(serverId))
		{
			synchronized (_gameServerThreads)
			{
				if (!_gameServerThreads.containsKey(serverId))
				{
					_gameServerThreads.put(serverId, gst);
				}
			}
		}
	}
	
	public final GameServerThread getGameServerThread(final int serverId)
	{
		return _gameServerThreads.get(serverId);
	}
	

	public static GameServerThreadPool getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final GameServerThreadPool _instance = new GameServerThreadPool();
	}
}
