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
package com.l2jserver.gameserver.model.zone.type;

import com.l2jserver.gameserver.instancemanager.CastleManager;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.entity.Castle;
import com.l2jserver.gameserver.model.zone.L2ZoneType;

/**
 * another type of zone where your speed is changed
 *
 * @author  kerberos
 */
public class L2SwampZone extends L2ZoneType
{
	private int _move_bonus;
	
	private int _castleId;
	private Castle _castle;
	
	public L2SwampZone(int id)
	{
		super(id);
		
		// Setup default speed reduce (in %)
		_move_bonus = -50;
		
		// no castle by default
		_castleId = 0;
		_castle = null;
	}
	
	@Override
	public void setParameter(String name, String value)
	{
		if (name.equals("move_bonus"))
		{
			_move_bonus = Integer.parseInt(value);
		}
		else if (name.equals("castleId"))
		{
			_castleId = Integer.parseInt(value);
		}
		else
			super.setParameter(name, value);
	}
	
	private Castle getCastle()
	{
		if (_castleId > 0 && _castle == null)
			_castle = CastleManager.getInstance().getCastleById(_castleId);
		
		return _castle;
	}
	
	@Override
	protected void onEnter(L2Character character)
	{
		if (getCastle() != null)
		{
			// castle zones active only during siege
			if (!getCastle().getSiege().getIsInProgress() || !getCastle().getSiege().isTrapsActive())
				return;
			
			// defenders not affected
			final L2PcInstance player = character.getActingPlayer();
			if (player != null && player.isInSiege() && player.getSiegeState() == 2)
				return;
		}
		
		character.setInsideZone(L2Character.ZONE_SWAMP, true);
		if (character instanceof L2PcInstance)
		{
			((L2PcInstance) character).broadcastUserInfo();
		}
	}
	
	@Override
	protected void onExit(L2Character character)
	{
		// don't broadcast info if not needed
		if (character.isInsideZone(L2Character.ZONE_SWAMP))
		{
			character.setInsideZone(L2Character.ZONE_SWAMP, false);
			if (character instanceof L2PcInstance)
			{
				((L2PcInstance) character).broadcastUserInfo();
			}
		}
	}
	
	public int getMoveBonus()
	{
		return _move_bonus;
	}
	
	@Override
	public void onDieInside(L2Character character)
	{
	}
	
	@Override
	public void onReviveInside(L2Character character)
	{
	}
	
}
