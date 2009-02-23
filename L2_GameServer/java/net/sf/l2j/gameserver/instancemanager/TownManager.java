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

import net.sf.l2j.gameserver.datatables.MapRegionTable;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.entity.Castle;
import net.sf.l2j.gameserver.model.zone.L2ZoneType;
import net.sf.l2j.gameserver.model.zone.type.L2TownZone;

public class TownManager
{
    //private static final Logger _log = Logger.getLogger(TownManager.class.getName());

    // =========================================================
    // Property - Public

    public final static L2TownZone getClosestTown(L2Object activeObject)
    {
        switch (MapRegionTable.getInstance().getMapRegion(activeObject.getPosition().getX(), activeObject.getPosition().getY()))
		{
			case 0:
				return getTown(2); // TI
			case 1:
				return getTown(3); // Elven
			case 2:
				return getTown(1); // DE
			case 3:
				return getTown(4); // Orc
			case 4:
				return getTown(6); // Dwarven
			case 5:
				return getTown(7); // Gludio
			case 6:
				return getTown(5); // Gludin
			case 7:
				return getTown(8); // Dion
			case 8:
				return getTown(9); // Giran
			case 9:
				return getTown(10); // Oren
			case 10:
				return getTown(12); // Aden
			case 11:
				return getTown(11); // HV
			case 12:
				return getTown(9); // Giran Harbour
			case 13:
				return getTown(15); // Heine
			case 14:
				return getTown(14); // Rune
			case 15:
				return getTown(13); // Goddard
			case 16:
				return getTown(17); // Schuttgart
			case 17:
				return getTown(16); // Floran
			case 18:
				return getTown(19); //Primeval Isle
            case 19:
                return getTown(20); //Kamael Village
            case 20:
                return getTown(21); //South of Wastelands Camp
            case 21:
                return getTown(22); //Fantasy Island
		}

        return getTown(16); // Default to floran
    }

    public final static L2TownZone getSecondClosestTown(L2Object activeObject)
    {
        switch (MapRegionTable.getInstance().getMapRegion(activeObject.getPosition().getX(), activeObject.getPosition().getY()))
		{
			case 0:
				return getTown(5); // TI
			case 1:
				return getTown(5); // Elven
			case 2:
				return getTown(5); // DE
			case 3:
				return getTown(4); // Orc
			case 4:
				return getTown(6); // Dwarven
			case 5:
				return getTown(5); // Gludio
			case 6:
				return getTown(5); // Gludin
			case 7:
				return getTown(7); // Dion
			case 8:
				return getTown(11); // Giran
			case 9:
				return getTown(11); // Oren
			case 10:
				return getTown(11); // Aden
			case 11:
				return getTown(11); // HV
			case 12:
				return getTown(16); // Giran Harbour
			case 13:
				return getTown(16); // Heine
			case 14:
				return getTown(13); // Rune
			case 15:
				return getTown(12); // Goddard
			case 16:
				return getTown(6); // Schuttgart
			case 17:
				return getTown(16); // Floran
			case 18:
				return getTown(19); //Primeval Isle
            case 19:
                return getTown(20); //Kamael Village
            case 20:
                return getTown(21); //South of Wastelands Camp
            case 21:
                return getTown(22); //Fantasy Island
		}

        return getTown(16); // Default to floran
    }
    
    
    public final static boolean townHasCastleInSiege(int townId)
    {
    	//int[] castleidarray = {0,0,0,0,0,0,0,1,2,3,4,0,5,0,0,6,0};
    	int[] castleidarray = {0,0,0,0,0,0,0,1,2,3,4,0,5,7,8,6,0,9,0,0,0,0,0};
    	int castleIndex= castleidarray[townId] ;

    	if ( castleIndex > 0 )
        {
           	Castle castle = CastleManager.getInstance().getCastles().get(CastleManager.getInstance().getCastleIndex(castleIndex));
           	if (castle != null)
           		return castle.getSiege().getIsInProgress();
        }
        return false;
    }

    public final static boolean townHasCastleInSiege(int x, int y)
    {
        int curtown= (MapRegionTable.getInstance().getMapRegion(x, y));
        //int[] castleidarray = {0,0,0,0,0,1,0,2,3,4,5,0,0,6,0,0,0,0};
        int[] castleidarray = {0,0,0,0,0,1,0,2,3,4,5,0,0,6,8,7,9,0,0,0,0,0};
        //find an instance of the castle for this town.
        int castleIndex = castleidarray[curtown];
        if ( castleIndex > 0 )
        {
        	Castle castle = CastleManager.getInstance().getCastles().get(CastleManager.getInstance().getCastleIndex(castleIndex));
        	if (castle != null)
        		return castle.getSiege().getIsInProgress();
        }
        return false;
    }

    public final static L2TownZone getTown(int townId)
    {
    	for (L2ZoneType temp : ZoneManager.getInstance().getAllZones())
    	{
        	if (temp instanceof L2TownZone && ((L2TownZone) temp).getTownId() == townId)
        		return (L2TownZone) temp;
    	}
        return null;
    }

    /**
     * Returns the town at that position (if any)
     * @param x
     * @param y
     * @param z
     * @return
     */
    public final static L2TownZone getTown(int x, int y, int z)
    {
        for (L2ZoneType temp : ZoneManager.getInstance().getZones(x, y, z))
        {
        	if (temp instanceof L2TownZone)
        		return (L2TownZone) temp;
        }
        return null;
    }
}
