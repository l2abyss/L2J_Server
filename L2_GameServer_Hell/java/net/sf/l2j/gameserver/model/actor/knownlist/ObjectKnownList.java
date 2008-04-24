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
package net.sf.l2j.gameserver.model.actor.knownlist;

import java.util.Map;

import javolution.util.FastMap;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2WorldRegion;
import net.sf.l2j.gameserver.model.actor.instance.L2BoatInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PlayableInstance;
import net.sf.l2j.gameserver.util.Util;

public class ObjectKnownList
{
    // =========================================================
    // Data Field
    private L2Object _activeObject;
    private Map<Integer, L2Object> _knownObjects;

    // =========================================================
    // Constructor
    public ObjectKnownList(L2Object activeObject)
    {
        _activeObject = activeObject;
    }

    // =========================================================
    // Method - Public
    public boolean addKnownObject(L2Object object) { return addKnownObject(object, null); }
    public boolean addKnownObject(L2Object object, L2Character dropper)
    {
        if (object == null) return false;

        // Check if already know object
        if (knowsObject(object))
            return false;

        // Check if object is not inside distance to watch object
        if (!Util.checkIfInShortRadius(getDistanceToWatchObject(object), getActiveObject(), object, true)) return false;

        return (getKnownObjects().put(object.getObjectId(), object) == null);
    }

    public final boolean knowsObject(L2Object object) { return getActiveObject() == object || getKnownObjects().containsKey(object.getObjectId()); }

    /** Remove all L2Object from _knownObjects */
    public void removeAllKnownObjects() { getKnownObjects().clear(); }

    public boolean removeKnownObject(L2Object object)
    {
    	if (object == null) return false;
    	return (getKnownObjects().remove(object.getObjectId()) != null);
    }

    // used only in Config.MOVE_BASED_KNOWNLIST and does not support guards seeing
    // moving monsters
    public final void findObjects()
    {
    	L2WorldRegion region = getActiveObject().getWorldRegion();
    	if (region == null) return;
    	
    	if (getActiveObject() instanceof L2PlayableInstance)
    	{
    		for (L2WorldRegion regi : region.getSurroundingRegions()) // offer members of this and surrounding regions
    		{
    			for (L2Object _object : regi.getVisibleObjects()) 
    			{
    				if (_object != getActiveObject())
    				{
    					addKnownObject(_object);
    					if (_object instanceof L2Character) _object.getKnownList().addKnownObject(getActiveObject());
    				}
    			}
    		}
    	}
    	else if (getActiveObject() instanceof L2Character)
    	{
    		for (L2WorldRegion regi : region.getSurroundingRegions()) // offer members of this and surrounding regions
    		{
    			if (regi.isActive()) for (L2Object _object : regi.getVisiblePlayable()) 
    			{
    				if (_object != getActiveObject())
    				{
    					addKnownObject(_object);
    				}
    			}
    		}
    	}
    }
    
    // Remove invisible and too far L2Object from _knowObject and if necessary from _knownPlayers of the L2Character
    public final void forgetObjects(boolean fullCheck)
    {
    	// Go through knownObjects
    	for (L2Object object: getKnownObjects().values())
    	{
    		if (!fullCheck && !(object instanceof L2PlayableInstance))
    			continue;

    		// Remove all invisible object
    		// Remove all too far object
    		if (
    				!object.isVisible() ||
    				!Util.checkIfInShortRadius(getDistanceToForgetObject(object), getActiveObject(), object, true)
    		)
    			if (object instanceof L2BoatInstance && getActiveObject() instanceof L2PcInstance)
    			{
    				if(((L2BoatInstance)(object)).getVehicleDeparture() == null )
    				{
    					//
    				}
    				else if(((L2PcInstance)getActiveObject()).isInBoat())
    				{
    					if(((L2PcInstance)getActiveObject()).getBoat() == object)
    					{
    						//
    					}
    					else
    					{
    						removeKnownObject(object);
    					}
    				}
    				else
    				{
    					removeKnownObject(object);
    				}
    			}
    			else
    			{
    				removeKnownObject(object);
    			}
    	}
    }

    // =========================================================
    // Property - Public
    public L2Object getActiveObject()
    {
        return _activeObject;
    }

    public int getDistanceToForgetObject(L2Object object) { return 0; }

    public int getDistanceToWatchObject(L2Object object) { return 0; }

    /** Return the _knownObjects containing all L2Object known by the L2Character. */
    public final Map<Integer, L2Object> getKnownObjects()
    {
        if (_knownObjects == null) _knownObjects = new FastMap<Integer, L2Object>().setShared(true);
        return _knownObjects;
    }

}
