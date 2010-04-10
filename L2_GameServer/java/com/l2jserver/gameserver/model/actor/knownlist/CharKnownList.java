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
package com.l2jserver.gameserver.model.actor.knownlist;

import java.util.Collection;
import java.util.Map;

import com.l2jserver.gameserver.model.L2Object;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.L2Summon;
import com.l2jserver.gameserver.model.actor.instance.L2AirShipInstance;
import com.l2jserver.gameserver.model.actor.instance.L2BoatInstance;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.util.Util;

import javolution.util.FastList;
import javolution.util.FastMap;

public class CharKnownList extends ObjectKnownList
{
    // =========================================================
    // Data Field
    private Map<Integer, L2PcInstance> _knownPlayers;
    private Map<Integer, L2Summon> _knownSummons;
    private Map<Integer, Integer> _knownRelations;

    // =========================================================
    // Constructor
    public CharKnownList(L2Character activeChar)
    {
        super(activeChar);
    }

    // =========================================================
    // Method - Public
    @Override
	public boolean addKnownObject(L2Object object)
    {
        if (!super.addKnownObject(object)) return false;
        if (object instanceof L2PcInstance) {
        	getKnownPlayers().put(object.getObjectId(), (L2PcInstance)object);
        	getKnownRelations().put(object.getObjectId(), -1);
        }
        else if (object instanceof L2Summon)
        	getKnownSummons().put(object.getObjectId(), (L2Summon)object);
        	
        return true;
    }

    /**
     * Return True if the L2PcInstance is in _knownPlayer of the L2Character.<BR><BR>
     * @param player The L2PcInstance to search in _knownPlayer
     */
    public final boolean knowsThePlayer(L2PcInstance player) { return getActiveChar() == player || getKnownPlayers().containsKey(player.getObjectId()); }

    /** Remove all L2Object from _knownObjects and _knownPlayer of the L2Character then cancel Attak or Cast and notify AI. */
    @Override
	public final void removeAllKnownObjects()
    {
        super.removeAllKnownObjects();
        getKnownPlayers().clear();
        getKnownRelations().clear();
        getKnownSummons().clear();

        // Set _target of the L2Character to null
        // Cancel Attack or Cast
        getActiveChar().setTarget(null);

        // Cancel AI Task
        if (getActiveChar().hasAI()) getActiveChar().setAI(null);
    }

    @Override
	public boolean removeKnownObject(L2Object object)
    {
        if (!super.removeKnownObject(object)) return false;
        if (object instanceof L2PcInstance) {
        	getKnownPlayers().remove(object.getObjectId());
        	getKnownRelations().remove(object.getObjectId());
        }
        else if (object instanceof L2Summon)
        {
        	getKnownSummons().remove(object.getObjectId());
        }
        // If object is targeted by the L2Character, cancel Attack or Cast
        if (object == getActiveChar().getTarget()) getActiveChar().setTarget(null);

        return true;
    }
    
    @Override
    public void forgetObjects(boolean fullCheck)
    {
    	if (!fullCheck)
    	{
    		Collection<L2PcInstance> plrs = getKnownPlayers().values();
    		//synchronized (getKnownPlayers())
			{
				for (L2PcInstance player : plrs)
				{
					// Remove all objects invisible or too far
					if (!player.isVisible()
					        || !Util.checkIfInShortRadius(getDistanceToForgetObject(player), getActiveObject(), player, true))
						removeKnownObject(player);
				}
			}
    		Collection<L2Summon> sums = getKnownSummons().values();
    		//synchronized (sums)
			{
				for (L2Summon summon : sums)
				{
					// Remove all objects invisible or too far
					if (!summon.isVisible()
					        || !Util.checkIfInShortRadius(getDistanceToForgetObject(summon), getActiveObject(), summon, true))
						removeKnownObject(summon);
				}
			}
        	return;
    	}
    	// Go through knownObjects
    	Collection<L2Object> objs = getKnownObjects().values();
    	//synchronized (getKnownObjects())
		{
			for (L2Object object : objs)
			{
				// Remove all objects invisible or too far
				if (!object.isVisible() || !Util.checkIfInShortRadius(getDistanceToForgetObject(object), getActiveObject(), object, true))
				{
					if (object instanceof L2BoatInstance && getActiveObject() instanceof L2PcInstance)
					{
						if (((L2BoatInstance) (object)).getVehicleDeparture() == null)
						{
							//
						}
						else if (((L2PcInstance) getActiveObject()).isInBoat())
						{
							if (((L2PcInstance) getActiveObject()).getBoat() != object)
							{
								removeKnownObject(object);
							}
						}
						else
						{
							removeKnownObject(object);
						}
					}
					else if (object instanceof L2AirShipInstance && getActiveObject() instanceof L2PcInstance)
					{
						if (((L2AirShipInstance) (object)).getAirShipInfo() == null)
						{
							//
						}
						else if (((L2PcInstance) getActiveObject()).isInAirShip())
						{
							if (((L2PcInstance) getActiveObject()).getAirShip() != object)
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
		}
    }

    // =========================================================
    // Method - Private

    // =========================================================
    // Property - Public
    public L2Character getActiveChar() { return (L2Character)super.getActiveObject(); }

    @Override
	public int getDistanceToForgetObject(L2Object object) { return 0; }

    @Override
	public int getDistanceToWatchObject(L2Object object) { return 0; }

    public Collection<L2Character> getKnownCharacters()
    {
        FastList<L2Character> result = new FastList<L2Character>();

        Collection<L2Object> objs = getKnownObjects().values();
        //synchronized (getKnownObjects())
		{
			for (L2Object obj : objs)
			{
				if (obj instanceof L2Character)
					result.add((L2Character) obj);
			}
		}
        return result;
    }

    public Collection<L2Character> getKnownCharactersInRadius(long radius)
    {
       FastList<L2Character> result = new FastList<L2Character>();

       Collection<L2Object> objs = getKnownObjects().values();
       //synchronized (getKnownObjects())
       if (objs != null && !objs.isEmpty())
       {
			for (L2Object obj : objs)
			{
				if (obj instanceof L2Character)
				{
					if (Util.checkIfInRange((int) radius, getActiveChar(), obj, true))
						result.add((L2Character) obj);
				}
			}
		}

       return result;
    }

    public final Map<Integer, L2PcInstance> getKnownPlayers()
    {
        if (_knownPlayers == null) _knownPlayers = new FastMap<Integer, L2PcInstance>().shared();
        return _knownPlayers;
    }

    public final Map<Integer, Integer> getKnownRelations()
    {
        if (_knownRelations == null) _knownRelations = new FastMap<Integer, Integer>().shared();
        return _knownRelations;
    }

    public final Map<Integer, L2Summon> getKnownSummons()
    {
        if (_knownSummons == null) _knownSummons = new FastMap<Integer, L2Summon>().shared();
        return _knownSummons;
    }
    
    public final Collection<L2PcInstance> getKnownPlayersInRadius(long radius)
    {
        FastList<L2PcInstance> result = new FastList<L2PcInstance>();

        Collection<L2PcInstance> plrs = getKnownPlayers().values();
        //synchronized (getKnownPlayers())
		{
			for (L2PcInstance player : plrs)
				if (Util.checkIfInRange((int) radius, getActiveChar(), player, true))
					result.add(player);
		}
        return result;
    }
}
