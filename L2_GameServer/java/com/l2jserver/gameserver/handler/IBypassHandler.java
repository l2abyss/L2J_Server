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

import java.util.logging.Logger;

import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
/**
 * 
 * @author nBd
 *
 */
public interface IBypassHandler
{
	public static Logger _log = Logger.getLogger(IBypassHandler.class.getName());

	/**
	 * this is the worker method that is called when someone uses an bypass command
	 * @param command
	 * @param activeChar
	 * @param target
	 * @return success
	 */
	public boolean useBypass(String command, L2PcInstance activeChar, L2Character target);

	/**
	 * this method is called at initialization to register all bypasses automatically
	 * @return all known bypasses
	 */
	public String[] getBypassList();
}