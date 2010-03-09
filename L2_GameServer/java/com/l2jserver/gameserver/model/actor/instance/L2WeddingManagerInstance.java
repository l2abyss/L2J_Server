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
import com.l2jserver.gameserver.Announcements;
import com.l2jserver.gameserver.datatables.SkillTable;
import com.l2jserver.gameserver.instancemanager.CoupleManager;
import com.l2jserver.gameserver.model.L2ItemInstance;
import com.l2jserver.gameserver.model.L2Skill;
import com.l2jserver.gameserver.model.L2World;
import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.model.entity.Couple;
import com.l2jserver.gameserver.model.itemcontainer.Inventory;
import com.l2jserver.gameserver.network.serverpackets.MagicSkillUse;
import com.l2jserver.gameserver.network.serverpackets.NpcHtmlMessage;
import com.l2jserver.gameserver.templates.chars.L2NpcTemplate;

public class L2WeddingManagerInstance extends L2Npc
{
	/**
	* @author evill33t & squeezed
	*/
	public L2WeddingManagerInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
		setInstanceType(InstanceType.L2WeddingManagerInstance);
	}

	public void showChatWindow(L2PcInstance player)
    {
        String filename = "data/html/mods/Wedding_start.htm";
        String replace = String.valueOf(Config.L2JMOD_WEDDING_PRICE);

        NpcHtmlMessage html = new NpcHtmlMessage(1);
        html.setFile(player.getHtmlPrefix(), filename);
        html.replace("%objectId%", String.valueOf(getObjectId()));
        html.replace("%replace%", replace);
        html.replace("%npcname%", getName());
        player.sendPacket(html);
    }

    @Override
    public void onBypassFeedback(L2PcInstance player, String command)
    {
        // standard msg
        String filename = "data/html/mods/Wedding_start.htm";
        String replace = "";

        // if player has no partner
        if(player.getPartnerId()==0)
        {
            filename = "data/html/mods/Wedding_nopartner.htm";
            sendHtmlMessage(player, filename, replace);
            return;
        }
        else
        {
            L2PcInstance ptarget = (L2PcInstance)L2World.getInstance().findObject(player.getPartnerId());
            // partner online ?
            if(ptarget==null || ptarget.isOnline()==0)
            {
                filename = "data/html/mods/Wedding_notfound.htm";
                sendHtmlMessage(player, filename, replace);
                return;
            }
            else
            {
                // already married ?
                if(player.isMarried())
                {
                    filename = "data/html/mods/Wedding_already.htm";
                    sendHtmlMessage(player, filename, replace);
                    return;
                }
                else if (player.isMarryAccepted())
                {
                    filename = "data/html/mods/Wedding_waitforpartner.htm";
                    sendHtmlMessage(player, filename, replace);
                    return;
                }
                else if (command.startsWith("AcceptWedding"))
                {
                    // accept the wedding request
                    player.setMarryAccepted(true);
                    Couple couple = CoupleManager.getInstance().getCouple(player.getCoupleId());
                    couple.marry();

                    //messages to the couple
                    player.sendMessage("Congratulations you are married!");
                    player.setMarried(true);
                    player.setMaryRequest(false);
                    ptarget.sendMessage("Congratulations you are married!");
                    ptarget.setMarried(true);
                    ptarget.setMaryRequest(false);

                    //wedding march
                    MagicSkillUse MSU = new MagicSkillUse(player, player, 2230, 1, 1, 0);
                    player.broadcastPacket(MSU);
                    MSU = new MagicSkillUse(ptarget, ptarget, 2230, 1, 1, 0);
                    ptarget.broadcastPacket(MSU);

                    // fireworks
                    L2Skill skill = SkillTable.FrequentSkill.LARGE_FIREWORK.getSkill();
                    if (skill != null)
                    {
                        MSU = new MagicSkillUse(player, player, 2025, 1, 1, 0);
                        player.sendPacket(MSU);
                        player.broadcastPacket(MSU);
                        player.useMagic(skill, false, false);

                        MSU = new MagicSkillUse(ptarget, ptarget, 2025, 1, 1, 0);
                        ptarget.sendPacket(MSU);
                        ptarget.broadcastPacket(MSU);
                        ptarget.useMagic(skill, false, false);

                    }

                    Announcements.getInstance().announceToAll("Congratulations to "+player.getName()+" and "+ptarget.getName()+"! They have been married.");

                    MSU = null;

                    filename = "data/html/mods/Wedding_accepted.htm";
                    replace = ptarget.getName();
                    sendHtmlMessage(ptarget, filename, replace);
                    return;
                }
                else if (command.startsWith("DeclineWedding"))
                {
                    player.setMaryRequest(false);
                    ptarget.setMaryRequest(false);
                    player.setMarryAccepted(false);
                    ptarget.setMarryAccepted(false);
                    player.sendMessage("You declined");
                    ptarget.sendMessage("Your partner declined");
                    replace = ptarget.getName();
                    filename = "data/html/mods/Wedding_declined.htm";
                    sendHtmlMessage(ptarget, filename, replace);
                    return;
                }
                else if (player.isMaryRequest())
                {
                    // check for formalwear
                	if(Config.L2JMOD_WEDDING_FORMALWEAR)
                	{
                		Inventory inv3 = player.getInventory();
                		L2ItemInstance item3 = inv3.getPaperdollItem(Inventory.PAPERDOLL_CHEST);
                		if(null==item3)
                		{
                			player.setIsWearingFormalWear(false);
                		}
                		else
                		{
	                		if(item3.getItemId() == 6408)
	                		{
	                			player.setIsWearingFormalWear(true);
	                		}else{
	                			player.setIsWearingFormalWear(false);
	                		}
                		}
                	}
                    if(Config.L2JMOD_WEDDING_FORMALWEAR && !player.isWearingFormalWear())
                    {
                        filename = "data/html/mods/Wedding_noformal.htm";
                        sendHtmlMessage(player, filename, replace);
                        return;
                    }
                    filename = "data/html/mods/Wedding_ask.htm";
                    player.setMaryRequest(false);
                    ptarget.setMaryRequest(false);
                    replace = ptarget.getName();
                    sendHtmlMessage(player, filename, replace);
                    return;
                }
                else if (command.startsWith("AskWedding"))
                {
                    // check for formalwear
                	if(Config.L2JMOD_WEDDING_FORMALWEAR)
                	{
                		Inventory inv3 = player.getInventory();
                		L2ItemInstance item3 = inv3.getPaperdollItem(Inventory.PAPERDOLL_CHEST);

                		if (null==item3)
                		{
                			player.setIsWearingFormalWear(false);
                		}
                		else
                		{
	                		if(item3.getItemId() == 6408)
	                		{
	                			player.setIsWearingFormalWear(true);
	                		}else{
	                			player.setIsWearingFormalWear(false);
	                		}
                		}
                	}
                    if(Config.L2JMOD_WEDDING_FORMALWEAR && !player.isWearingFormalWear())
                    {
                        filename = "data/html/mods/Wedding_noformal.htm";
                        sendHtmlMessage(player, filename, replace);
                        return;
                    }
                    else if(player.getAdena()<Config.L2JMOD_WEDDING_PRICE)
                    {
                        filename = "data/html/mods/Wedding_adena.htm";
                        replace = String.valueOf(Config.L2JMOD_WEDDING_PRICE);
                        sendHtmlMessage(player, filename, replace);
                        return;
                    }
                    else
                    {
                        player.setMarryAccepted(true);
                        ptarget.setMaryRequest(true);
                        replace = ptarget.getName();
                        filename = "data/html/mods/Wedding_requested.htm";
                        player.getInventory().reduceAdena("Wedding", Config.L2JMOD_WEDDING_PRICE, player, player.getLastFolkNPC());
                        sendHtmlMessage(player, filename, replace);
                        return;
                    }
                }
            }
        }
        sendHtmlMessage(player, filename, replace);
    }

    private void sendHtmlMessage(L2PcInstance player, String filename, String replace)
    {
        NpcHtmlMessage html = new NpcHtmlMessage(1);
        html.setFile(player.getHtmlPrefix(), filename);
        html.replace("%objectId%", String.valueOf(getObjectId()));
        html.replace("%replace%", replace);
        html.replace("%npcname%", getName());
        player.sendPacket(html);
    }
}
