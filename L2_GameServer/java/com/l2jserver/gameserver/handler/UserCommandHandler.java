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

import gnu.trove.TIntObjectHashMap;

import java.util.logging.Logger;

import com.l2jserver.Config;

/**
 * This class ...
 *
 * @version $Revision: 1.1.2.1.2.5 $ $Date: 2005/03/27 15:30:09 $
 */
public class UserCommandHandler
{
	private static Logger _log = Logger.getLogger(UserCommandHandler.class.getName());
	
	private TIntObjectHashMap<IUserCommandHandler> _datatable;
	
	public static UserCommandHandler getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private UserCommandHandler()
	{
		_datatable = new TIntObjectHashMap<IUserCommandHandler>();
	}
	
	public void registerUserCommandHandler(IUserCommandHandler handler)
	{
		int[] ids = handler.getUserCommandList();
		for (int i = 0; i < ids.length; i++)
		{
			if (Config.DEBUG)
				_log.fine("Adding handler for user command " + ids[i]);
			_datatable.put(ids[i], handler);
		}
	}
	
	public IUserCommandHandler getUserCommandHandler(int userCommand)
	{
		if (Config.DEBUG)
			_log.fine("getting handler for user command: " + userCommand);
		return _datatable.get(userCommand);
	}
	
	/**
	 * @return
	 */
	public int size()
	{
		return _datatable.size();
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final UserCommandHandler _instance = new UserCommandHandler();
	}
}
