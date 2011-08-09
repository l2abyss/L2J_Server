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
package com.l2jserver.gameserver.model;

/**
 * 
 * @author Rayan RPG, JIV
 * @since 927
 *
 */
public class L2NpcWalkerNode
{
	private int _routeId;
	private String _chatString;
	private int _npcString;
	private int _moveX;
	private int _moveY;
	private int _moveZ;
	private int _delay;
	private boolean _running;
	
	public L2NpcWalkerNode(int routeId, int npcString, String chatText, int moveX, int moveY, int moveZ, int delay, boolean running)
	{
		super();
		this._routeId = routeId;
		this._chatString = chatText;
		this._npcString = npcString;
		this._moveX = moveX;
		this._moveY = moveY;
		this._moveZ = moveZ;
		this._delay = delay;
		this._running = running;
	}
	
	public int getRouteId()
	{
		return _routeId;
	}
	
	public String getChatText()
	{
		if (_npcString != -1)
			throw new IllegalStateException("npcString is defined for walker route!");
		return _chatString;
	}
	
	public int getMoveX()
	{
		return _moveX;
	}
	
	public int getMoveY()
	{
		return _moveY;
	}
	
	public int getMoveZ()
	{
		return _moveZ;
	}
	
	public int getDelay()
	{
		return _delay;
	}
	
	public boolean getRunning()
	{
		return _running;
	}

	public int getNpcString()
	{
		return _npcString;
	}
}
