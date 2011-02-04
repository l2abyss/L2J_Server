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

import com.l2jserver.Config;
import com.l2jserver.gameserver.cache.HtmCache;
import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.model.entity.TvTEvent;
import com.l2jserver.gameserver.network.serverpackets.ActionFailed;
import com.l2jserver.gameserver.network.serverpackets.NpcHtmlMessage;
import com.l2jserver.gameserver.templates.chars.L2NpcTemplate;

public class L2TvTEventNpcInstance extends L2Npc
{
	private static final String htmlPath="data/html/mods/TvTEvent/";
	
	public L2TvTEventNpcInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
		setInstanceType(InstanceType.L2TvTEventNpcInstance);
	}
	
	@Override
	public void onBypassFeedback(L2PcInstance playerInstance, String command)
	{
		TvTEvent.onBypass(command, playerInstance);
	}
	
	@Override
	public void showChatWindow(L2PcInstance playerInstance, int val)
	{
		if (playerInstance == null)
			return;
		
		if (TvTEvent.isParticipating())
		{
			final boolean isParticipant = TvTEvent.isPlayerParticipant(playerInstance.getObjectId());
			final String htmContent;
			
			if (!isParticipant)
				htmContent = HtmCache.getInstance().getHtm(playerInstance.getHtmlPrefix(), htmlPath + "Participation.htm");
			else
				htmContent = HtmCache.getInstance().getHtm(playerInstance.getHtmlPrefix(), htmlPath + "RemoveParticipation.htm");
			
			if (htmContent != null)
			{
				int[] teamsPlayerCounts = TvTEvent.getTeamsPlayerCounts();
				NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(getObjectId());
				
				npcHtmlMessage.setHtml(htmContent);
				npcHtmlMessage.replace("%objectId%", String.valueOf(getObjectId()));
				npcHtmlMessage.replace("%team1name%", Config.TVT_EVENT_TEAM_1_NAME);
				npcHtmlMessage.replace("%team1playercount%", String.valueOf(teamsPlayerCounts[0]));
				npcHtmlMessage.replace("%team2name%", Config.TVT_EVENT_TEAM_2_NAME);
				npcHtmlMessage.replace("%team2playercount%", String.valueOf(teamsPlayerCounts[1]));
				npcHtmlMessage.replace("%playercount%", String.valueOf(teamsPlayerCounts[0]+teamsPlayerCounts[1]));
				if (!isParticipant)
					npcHtmlMessage.replace("%fee%", TvTEvent.getParticipationFee());
				
				playerInstance.sendPacket(npcHtmlMessage);
			}
		}
		else if (TvTEvent.isStarting() || TvTEvent.isStarted())
		{
			final String htmContent = HtmCache.getInstance().getHtm(playerInstance.getHtmlPrefix(), htmlPath + "Status.htm");
			
			if (htmContent != null)
			{
				int[] teamsPlayerCounts = TvTEvent.getTeamsPlayerCounts();
				int[] teamsPointsCounts = TvTEvent.getTeamsPoints();
				NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(getObjectId());
				
				npcHtmlMessage.setHtml(htmContent);
				//npcHtmlMessage.replace("%objectId%", String.valueOf(getObjectId()));
				npcHtmlMessage.replace("%team1name%", Config.TVT_EVENT_TEAM_1_NAME);
				npcHtmlMessage.replace("%team1playercount%", String.valueOf(teamsPlayerCounts[0]));
				npcHtmlMessage.replace("%team1points%", String.valueOf(teamsPointsCounts[0]));
				npcHtmlMessage.replace("%team2name%", Config.TVT_EVENT_TEAM_2_NAME);
				npcHtmlMessage.replace("%team2playercount%", String.valueOf(teamsPlayerCounts[1]));
				npcHtmlMessage.replace("%team2points%", String.valueOf(teamsPointsCounts[1])); // <---- array index from 0 to 1 thx DaRkRaGe
				playerInstance.sendPacket(npcHtmlMessage);
			}
		}
		
		playerInstance.sendPacket(ActionFailed.STATIC_PACKET);
	}
}
