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
package net.sf.l2j.gameserver.instancemanager;

import java.util.logging.Logger;

import javolution.util.FastList;
import net.sf.l2j.gameserver.model.zone.type.L2FishingZone;
import net.sf.l2j.gameserver.model.zone.type.L2WaterZone;

public class FishingZoneManager
{
    protected static final Logger _log = Logger.getLogger(FishingZoneManager.class.getName());
	// =========================================================
	private static FishingZoneManager _instance;
	public static final FishingZoneManager getInstance()
	{
		if (_instance == null)
		{
			_log.info("Initializing FishingZoneManager");
			_instance = new FishingZoneManager();
		}
		return _instance;
	}
	// =========================================================


	// =========================================================
	// Data Field
	private FastList<L2FishingZone> _fishingZones;
	private FastList<L2WaterZone> _waterZones;

	// =========================================================
	// Constructor
	public FishingZoneManager()
	{
	}

	// =========================================================
	// Property - Public

	public void addFishingZone(L2FishingZone fishingZone)
	{
		if (_fishingZones == null)
			_fishingZones = new FastList<L2FishingZone>();

		_fishingZones.add(fishingZone);
	}
	public void addWaterZone(L2WaterZone waterZone)
	{
		if (_waterZones == null)
			_waterZones = new FastList<L2WaterZone>();

		_waterZones.add(waterZone);
	}
	/* isInsideFishingZone() - This function was modified to check the coordinates without caring for Z.
	 * This allows for the player to fish off bridges, into the water, or from other similar high places. One
	 * should be able to cast the line from up into the water, not only fishing whith one's feet wet. :)
	 *
	 *  TODO: Consider in the future, limiting the maximum height one can be above water, if we start getting
	 *  "orbital fishing" players... xD
	 */
	public final L2FishingZone isInsideFishingZone(int x, int y, int z)
	{
		for (L2FishingZone temp : _fishingZones)
			if (temp.isInsideZone(x, y, temp.getWaterZ() - 10)) return temp;
		return null;
	}
	public final L2WaterZone isInsideWaterZone(int x, int y, int z)
	{
		for (L2WaterZone temp : _waterZones)
			if (temp.isInsideZone(x, y, temp.getWaterZ())) return temp;
		return null;
	}
}
