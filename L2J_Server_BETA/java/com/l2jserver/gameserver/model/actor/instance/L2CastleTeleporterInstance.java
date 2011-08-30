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
package com.l2jserver.gameserver.model.actor.instance;

import gnu.trove.TObjectProcedure;

import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2jserver.gameserver.ThreadPoolManager;
import com.l2jserver.gameserver.instancemanager.MapRegionManager;
import com.l2jserver.gameserver.model.L2World;
import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.network.NpcStringId;
import com.l2jserver.gameserver.network.serverpackets.NpcHtmlMessage;
import com.l2jserver.gameserver.network.serverpackets.NpcSay;
import com.l2jserver.gameserver.templates.chars.L2NpcTemplate;

/**
 * @author Kerberos
 */
public final class L2CastleTeleporterInstance extends L2Npc
{
	public static final Logger _log = Logger.getLogger(L2CastleTeleporterInstance.class.getName());
	
	private boolean _currentTask = false;
	
	/**
	 * @param objectId
	 * @param template
	 */
	public L2CastleTeleporterInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
		setInstanceType(InstanceType.L2CastleTeleporterInstance);
	}
	
	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		StringTokenizer st = new StringTokenizer(command, " ");
		String actualCommand = st.nextToken(); // Get actual command
		
		if (actualCommand.equalsIgnoreCase("tele"))
		{
			int delay;
			if (!getTask())
			{
				if (getCastle().getSiege().getIsInProgress() && getCastle().getSiege().getControlTowerCount() == 0)
					delay = 480000;
				else
					delay = 30000;
				
				setTask(true);
				ThreadPoolManager.getInstance().scheduleGeneral(new oustAllPlayers(), delay );
			}
			
			String filename = "data/html/castleteleporter/MassGK-1.htm";
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile(player.getHtmlPrefix(), filename);
			player.sendPacket(html);
			return;
		}
		super.onBypassFeedback(player, command);
	}
	
	@Override
	public void showChatWindow(L2PcInstance player)
	{
		String filename;
		if (!getTask())
		{
			if (getCastle().getSiege().getIsInProgress() && getCastle().getSiege().getControlTowerCount() == 0)
				filename = "data/html/castleteleporter/MassGK-2.htm";
			else
				filename = "data/html/castleteleporter/MassGK.htm";
		}
		else
			filename = "data/html/castleteleporter/MassGK-1.htm";
		
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(player.getHtmlPrefix(), filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);
	}
	
	void oustAllPlayers()
	{
		getCastle().oustAllPlayers();
	}
	
	class oustAllPlayers implements Runnable
	{
		public void run()
		{
			try
			{
				NpcSay cs = new NpcSay(getObjectId(), 1, getNpcId(), NpcStringId.THE_DEFENDERS_OF_S1_CASTLE_WILL_BE_TELEPORTED_TO_THE_INNER_CASTLE);
				cs.addStringParameter(getCastle().getName());
				int region = MapRegionManager.getInstance().getMapRegionLocId(getX(), getY());
				L2World.getInstance().forEachPlayer(new ForEachPlayerInRegionSendPacket(region, cs));
				oustAllPlayers();
				setTask(false);
			}
			catch (NullPointerException e)
			{
				_log.log(Level.WARNING, "" + e.getMessage(), e);
			}
		}
	}
	
	private final class ForEachPlayerInRegionSendPacket implements TObjectProcedure<L2PcInstance>
	{
		int _region;
		NpcSay _cs;
		
		private ForEachPlayerInRegionSendPacket(int region, NpcSay cs)
		{
			_region = region;
			_cs = cs;
		}
		
		@Override
		public final boolean execute(final L2PcInstance player)
		{
			if (_region == MapRegionManager.getInstance().getMapRegionLocId(player.getX(), player.getY()))
				player.sendPacket(_cs);
			return true;
		}
	}
	
	public boolean getTask()
	{
		return _currentTask;
	}
	
	public void setTask(boolean state)
	{
		_currentTask = state;
	}
}