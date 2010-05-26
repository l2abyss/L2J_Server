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
package com.l2jserver.gameserver.instancemanager;

import java.util.ArrayList;

import javolution.util.FastMap;

import com.l2jserver.gameserver.idfactory.IdFactory;
import com.l2jserver.gameserver.model.actor.instance.L2AirShipControllerInstance;
import com.l2jserver.gameserver.model.actor.instance.L2AirShipInstance;
import com.l2jserver.gameserver.templates.StatsSet;
import com.l2jserver.gameserver.templates.chars.L2CharTemplate;


public class AirShipManager
{
	private L2CharTemplate _airShipTemplate = null;
	private FastMap<Integer, L2AirShipInstance> _airShips = new FastMap<Integer, L2AirShipInstance>();
	private ArrayList<L2AirShipControllerInstance> _atcs = new ArrayList<L2AirShipControllerInstance>(2);
	
	public static final AirShipManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private AirShipManager()
	{
		StatsSet npcDat = new StatsSet();
		npcDat.set("npcId", 9);
		npcDat.set("level", 0);
		npcDat.set("jClass", "boat");

		npcDat.set("baseSTR", 0);
		npcDat.set("baseCON", 0);
		npcDat.set("baseDEX", 0);
		npcDat.set("baseINT", 0);
		npcDat.set("baseWIT", 0);
		npcDat.set("baseMEN", 0);

		npcDat.set("baseShldDef", 0);
		npcDat.set("baseShldRate", 0);
		npcDat.set("baseAccCombat", 38);
		npcDat.set("baseEvasRate", 38);
		npcDat.set("baseCritRate", 38);

		npcDat.set("collision_radius", 0);
		npcDat.set("collision_height", 0);
		npcDat.set("sex", "male");
		npcDat.set("type", "");
		npcDat.set("baseAtkRange", 0);
		npcDat.set("baseMpMax", 0);
		npcDat.set("baseCpMax", 0);
		npcDat.set("rewardExp", 0);
		npcDat.set("rewardSp", 0);
		npcDat.set("basePAtk", 0);
		npcDat.set("baseMAtk", 0);
		npcDat.set("basePAtkSpd", 0);
		npcDat.set("aggroRange", 0);
		npcDat.set("baseMAtkSpd", 0);
		npcDat.set("rhand", 0);
		npcDat.set("lhand", 0);
		npcDat.set("armor", 0);
		npcDat.set("baseWalkSpd", 0);
		npcDat.set("baseRunSpd", 0);
		npcDat.set("name", "AirShip");
		npcDat.set("baseHpMax", 50000);
		npcDat.set("baseHpReg", 3.e-3f);
		npcDat.set("baseMpReg", 3.e-3f);
		npcDat.set("basePDef", 100);
		npcDat.set("baseMDef", 100);
		_airShipTemplate = new L2CharTemplate(npcDat);
	}
	
	public L2AirShipInstance getAirShip(int objectId)
	{
		return _airShips.get(objectId);
	}

	public L2AirShipInstance getNewAirShip(int x, int y, int z, int heading)
	{
		L2AirShipInstance airShip = new L2AirShipInstance(IdFactory.getInstance().getNextId(), _airShipTemplate);
		_airShips.put(airShip.getObjectId(), airShip);
		airShip.setHeading(heading);
		airShip.setXYZInvisible(x, y, z);
		airShip.spawnMe();
		return airShip;
	}

	public void registerATC(L2AirShipControllerInstance atc)
	{
		_atcs.add(atc);
	}

	public ArrayList<L2AirShipControllerInstance> getATCs()
	{
		return _atcs;
	}

	public L2AirShipControllerInstance getNearestATC(L2AirShipInstance ship, int radius)
	{
		if (_atcs == null || _atcs.isEmpty())
			return null;

		for (L2AirShipControllerInstance atc : _atcs)
		{
			if (atc != null && atc.isInsideRadius(ship, radius, true, false))
				return atc;
		}

		return null;
	}

	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final AirShipManager _instance = new AirShipManager();
	}
}