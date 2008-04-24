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
package net.sf.l2j.gameserver.model.zone;

import javolution.util.FastList;
import net.sf.l2j.gameserver.model.L2Character;

/**
 * This class manages all zones for a given world region
 *
 * @author  durgus
 */
public class L2ZoneManager
{
	private final FastList<L2ZoneType> _zones;

	/**
	 * The Constructor creates an initial zone list
	 * use registerNewZone() / unregisterZone() to
	 * change the zone list
	 *
	 */
	public L2ZoneManager()
	{
		_zones = new FastList<L2ZoneType>();
	}

	/**
	 * Register a new zone object into the manager
	 * @param zone
	 */
	public void registerNewZone(L2ZoneType zone)
	{
		_zones.add(zone);
	}
    
    public FastList<L2ZoneType> getZones()
    {
        return _zones;
    }

	/**
	 * Unregister a given zone from the manager (e.g. dynamic zones)
	 * @param zone
	 */
	public void unregisterZone(L2ZoneType zone)
	{
		_zones.remove(zone);
	}

	public void revalidateZones(L2Character character)
	{
		for (L2ZoneType e : _zones)
		{
			if(e != null) e.revalidateInZone(character);
		}
	}

	public void removeCharacter(L2Character character)
	{
		for (L2ZoneType e : _zones)
		{
			if(e != null) e.removeCharacter(character);
		}
	}

	public void onDeath(L2Character character)
	{
		for (L2ZoneType e : _zones)
		{
			if(e != null) e.onDieInside(character);
		}
	}

	public void onRevive(L2Character character)
	{
		for (L2ZoneType e : _zones)
		{
			if(e != null) e.onReviveInside(character);
		}
	}

}
