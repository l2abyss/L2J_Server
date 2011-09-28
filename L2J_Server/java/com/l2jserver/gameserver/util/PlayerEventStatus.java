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
package com.l2jserver.gameserver.util;

import java.util.List;

import javolution.util.FastList;

import com.l2jserver.gameserver.instancemanager.InstanceManager;
import com.l2jserver.gameserver.model.Location;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author Nik
 */

public class PlayerEventStatus
{
	public L2PcInstance player = null;
	public Location initLoc = new Location(0,0,0);
	public int initInstanceId = 0;
	public int initKarma = 0;
	public int initPvpKills = 0;
	public int initPkKills = 0;
	public String initTitle = "";
	public List<L2PcInstance> kills = new FastList<L2PcInstance>();
	public boolean eventSitForced = false;
	
	public PlayerEventStatus(L2PcInstance player)
	{
		this.player = player;
		initLoc = new Location(player.getX(), player.getY(), player.getZ(), player.getHeading());
		initInstanceId = player.getInstanceId();
		initKarma = player.getKarma();
		initPvpKills = player.getPvpKills();
		initPkKills = player.getPkKills();
		initTitle = player.getTitle();
		
	}
	
	public void restoreInits()
	{
		player.teleToLocation(initLoc, true);
		if (initInstanceId > 0 && InstanceManager.getInstance().getInstance(initInstanceId) != null)
			player.setInstanceId(initInstanceId);
		player.setKarma(initKarma);
		player.setPvpKills(initPvpKills);
		player.setPkKills(initPkKills);
		player.setTitle(initTitle);
	}
}
