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

import com.l2jserver.gameserver.instancemanager.ClanHallManager;
import com.l2jserver.gameserver.instancemanager.MapRegionManager;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.entity.ClanHall;
import com.l2jserver.gameserver.model.zone.L2ZoneRespawn;
import com.l2jserver.gameserver.network.serverpackets.AgitDecoInfo;

/**
 * A clan hall zone
 *
 * @author  durgus
 */
public class L2ClanHallZone extends L2ZoneRespawn
{
	private int _clanHallId;
	
	public L2ClanHallZone(int id)
	{
		super(id);
	}
	
	@Override
	public void setParameter(String name, String value)
	{
		if (name.equals("clanHallId"))
		{
			_clanHallId = Integer.parseInt(value);
			// Register self to the correct clan hall
			ClanHallManager.getInstance().getClanHallById(_clanHallId).setZone(this);
		}
		else
			super.setParameter(name, value);
	}
	
	@Override
	protected void onEnter(L2Character character)
	{
		if (character instanceof L2PcInstance)
		{
			// Set as in clan hall
			character.setInsideZone(L2Character.ZONE_CLANHALL, true);
			
			ClanHall clanHall = ClanHallManager.getInstance().getClanHallById(_clanHallId);
			if (clanHall == null)
				return;
			
			// Send decoration packet
			AgitDecoInfo deco = new AgitDecoInfo(clanHall);
			((L2PcInstance) character).sendPacket(deco);
			
		}
	}
	
	@Override
	protected void onExit(L2Character character)
	{
		if (character instanceof L2PcInstance)
		{
			// Unset clanhall zone
			character.setInsideZone(L2Character.ZONE_CLANHALL, false);
			
		}
	}
	
	@Override
	public void onDieInside(L2Character character)
	{
	}
	
	@Override
	public void onReviveInside(L2Character character)
	{
	}
	
	/**
	 * Removes all foreigners from the clan hall
	 * @param owningClanId
	 */
	public void banishForeigners(int owningClanId)
	{
		for (L2Character temp : getCharactersInsideArray())
		{
			if (!(temp instanceof L2PcInstance))
				continue;
			if (((L2PcInstance) temp).getClanId() == owningClanId)
				continue;
			
			((L2PcInstance) temp).teleToLocation(MapRegionManager.TeleportWhereType.ClanHall_banish);
		}
	}
	
	/**
	 * @return the clanHallId
	 */
	public int getClanHallId()
	{
		return _clanHallId;
	}
}
