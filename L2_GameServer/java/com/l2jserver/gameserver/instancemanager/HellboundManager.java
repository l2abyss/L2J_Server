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
package com.l2jserver.gameserver.instancemanager;

import java.util.logging.Logger;

public class HellboundManager
{
	private static final Logger _log = Logger.getLogger(HellboundManager.class.getName());

	private HellboundManager()
	{
		_log.info(getClass().getSimpleName()+": Initializing");
		init();
	}

	private void init()
	{
		_log.info(getClass().getSimpleName()+": Mode: dummy");
		if (isLocked())
			_log.info(getClass().getSimpleName()+": State: locked");
		else
			_log.info(getClass().getSimpleName()+": State: unlocked");
	}

	/**
	 * Returns true if Hellbound is locked
	 */
	public boolean isLocked()
	{
		return true;
	}

	public static final HellboundManager getInstance()
	{
		return SingletonHolder._instance;
	}

	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final HellboundManager _instance = new HellboundManager();
	}
}