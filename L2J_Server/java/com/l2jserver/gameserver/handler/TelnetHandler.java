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
package com.l2jserver.gameserver.handler;

import gnu.trove.map.hash.TIntObjectHashMap;

/**
 * @author UnAfraid
 */
public class TelnetHandler
{
	private final TIntObjectHashMap<ITelnetHandler> _telnetHandlers;
	
	public static TelnetHandler getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private TelnetHandler()
	{
		_telnetHandlers = new TIntObjectHashMap<ITelnetHandler>();
	}
	
	public void registerHandler(ITelnetHandler handler)
	{
		for (String element : handler.getCommandList())
		{
			_telnetHandlers.put(element.toLowerCase().hashCode(), handler);
		}
	}
	
	public ITelnetHandler getHandler(String BypassCommand)
	{
		String command = BypassCommand;
		
		if (BypassCommand.indexOf(" ") != -1)
		{
			command = BypassCommand.substring(0, BypassCommand.indexOf(" "));
		}
		
		return _telnetHandlers.get(command.toLowerCase().hashCode());
	}
	
	public int size()
	{
		return _telnetHandlers.size();
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final TelnetHandler _instance = new TelnetHandler();
	}
}
