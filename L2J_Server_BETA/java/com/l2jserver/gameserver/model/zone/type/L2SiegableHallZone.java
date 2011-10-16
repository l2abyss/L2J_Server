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

import java.util.List;

import com.l2jserver.gameserver.instancemanager.MapRegionManager.TeleportWhereType;
import com.l2jserver.gameserver.model.Location;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author BiggBoss
 */
public final class L2SiegableHallZone extends L2ClanHallZone
{
	private List<Location> _challengerLocations;
	
	public L2SiegableHallZone(int id)
	{
		super(id);
	}
	
	@Override
	public void parseLoc(int x, int y, int z, String type)
	{
		if(type != null && type.equals("challenger"))
		{
			if(_challengerLocations == null)
				_challengerLocations = new java.util.ArrayList<Location>();
			_challengerLocations.add(new Location(x,y,z));
		}
		else
			super.parseLoc(x, y, z, type);
	}
	
	public List<Location> getChallengerSpawns()
	{
		return _challengerLocations;
	}
	
	public void banishNonSiegeParticipants()
	{
		final TeleportWhereType banish = TeleportWhereType.ClanHall_banish;
		for(L2Character character : getCharactersInsideArray())
		{
			if(character instanceof L2PcInstance)
			{
				if(!((L2PcInstance)character).isInHideoutSiege())
					character.teleToLocation(banish);
			}
		}
	}
}
