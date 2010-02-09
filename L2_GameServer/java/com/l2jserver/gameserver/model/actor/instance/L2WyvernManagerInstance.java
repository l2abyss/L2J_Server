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

import java.util.Arrays;

import com.l2jserver.Config;
import com.l2jserver.gameserver.SevenSigns;
import com.l2jserver.gameserver.datatables.SkillTable;
import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.network.serverpackets.ActionFailed;
import com.l2jserver.gameserver.network.serverpackets.NpcHtmlMessage;
import com.l2jserver.gameserver.templates.chars.L2NpcTemplate;

public class L2WyvernManagerInstance extends L2Npc
{
	private static final int[] STRIDERS = { 12526, 12527, 12528, 16038, 16039, 16040 };

	public L2WyvernManagerInstance (int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		if (command.startsWith("RideWyvern"))
		{
			if (!isOwnerClan(player))
				return;
			
			if(!Config.ALLOW_WYVERN_DURING_SIEGE && isInSiege())
			{
				player.sendMessage("You cannot ride wyvern during siege.");
				return;
			}

			if ((SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE) == SevenSigns.CABAL_DUSK) && SevenSigns.getInstance().isSealValidationPeriod())
			{
				player.sendMessage("You cannot ride wyvern while Seal of Strife controlled by Dusk.");
				return;
			}
			if(player.getPet() == null)
			{
				if(player.isMounted())
				{
					player.sendMessage("You already have a pet.");
					return;
				}
				else
				{
					player.sendMessage("Summon your Strider first.");
					return;
				}
			}
			else if (Arrays.binarySearch(STRIDERS, player.getPet().getNpcId()) >= 0 )
			{
				if (player.getInventory().getItemByItemId(1460) != null && player.getInventory().getItemByItemId(1460).getCount() >= 25)
				{
					if (player.getPet().getLevel() < 55)
					{
						player.sendMessage("Your Strider Has not reached the required level.");
						return;
					}
					else
					{
						player.getPet().unSummon(player);
						if (player.mount(12621, 0, true))
						{
							player.getInventory().destroyItemByItemId("Wyvern", 1460, 25, player, player.getTarget());
							player.addSkill(SkillTable.FrequentSkill.WYVERN_BREATH.getSkill());
							player.sendMessage("The Wyvern has been summoned successfully!");
						}
						return;
					}
				}
				else
				{
					player.sendMessage("You need 25 Crystals: B Grade.");
					return;
				}
			}
			else
			{
				player.sendMessage("Unsummon your pet.");
				return;
			}
		}
		else
			super.onBypassFeedback(player, command);
	}

	public void showChatWindow(L2PcInstance player)
	{
		player.sendPacket( ActionFailed.STATIC_PACKET );
		String filename = "data/html/wyvernmanager/wyvernmanager-no.htm";

		if (isOwnerClan(player))
			filename = "data/html/wyvernmanager/wyvernmanager.htm";      // Owner message window

		NpcHtmlMessage html = new NpcHtmlMessage(1);
		html.setFile(filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcname%", getName());
		player.sendPacket(html);
	}

	protected boolean isOwnerClan(L2PcInstance player)
	{
		return true;
	}
	
	protected boolean isInSiege()
	{
		return false;
	}
}
