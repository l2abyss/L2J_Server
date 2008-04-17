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
package net.sf.l2j.gameserver.model.actor.instance;

import java.util.HashMap;
import java.util.logging.Logger;

import javolution.text.TextBuilder;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.Olympiad;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Multisell;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.serverpackets.ExHeroList;
import net.sf.l2j.gameserver.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;
import net.sf.l2j.util.L2FastList;

/**
 * Olympiad Npc's Instance
 *
 * @author godson
 */

public class L2OlympiadManagerInstance extends L2FolkInstance
{
    private static Logger _logOlymp = Logger.getLogger(L2OlympiadManagerInstance.class.getName());

    private static final int GATE_PASS = Config.ALT_OLY_COMP_RITEM;

    public L2OlympiadManagerInstance (int objectId, L2NpcTemplate template)
    {
        super(objectId, template);
    }

    @Override
	public void onBypassFeedback (L2PcInstance player, String command)
    {
        if (command.startsWith("OlympiadDesc"))
        {
            int val = Integer.parseInt(command.substring(13,14));
            String suffix = command.substring(14);
            showChatWindow(player, val, suffix);
        }
        else if (command.startsWith("OlympiadNoble"))
        {
        	if (!player.isNoble() || player.getClassId().getId()<88)
                return;

            int val = Integer.parseInt(command.substring(14));
            NpcHtmlMessage reply;
            TextBuilder replyMSG;

            switch(val)
            {
                case 1:
                    Olympiad.getInstance().unRegisterNoble(player);
                    break;
                case 2:
                    int classed = 0;
                    int nonClassed = 0;
                    int[] array = Olympiad.getInstance().getWaitingList();

                    if (array != null)
                    {
                        classed = array[0];
                        nonClassed = array[1];

                    }

                    reply = new NpcHtmlMessage(getObjectId());
                    replyMSG = new TextBuilder("<html><body>");
                    replyMSG.append("The number of people on the waiting list for " +
                            "Grand Olympiad" +
                            "<center>" +
                            "<img src=\"L2UI.SquareWhite\" width=270 height=1><img src=\"L2UI.SquareBlank\" width=1 height=3>" +
                            "<table width=270 border=0 bgcolor=\"000000\">" +
                            "<tr>" +
                            "<td align=\"left\">General</td>" +
                            "<td align=\"right\">"+ classed + "</td>" +
                            "</tr>" +
                            "<tr>" +
                            "<td align=\"left\">Not class-defined</td>" +
                            "<td align=\"right\">" + nonClassed + "</td>" +
                            "</tr>" +
                            "</table><br>" +
                            "<img src=\"L2UI.SquareWhite\" width=270 height=1> <img src=\"L2UI.SquareBlank\" width=1 height=3>" +
                            "<button value=\"Back\" action=\"bypass -h npc_"+getObjectId()+"_OlympiadDesc 2a\" " +
                            "width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></center>");

                    replyMSG.append("</body></html>");

                    reply.setHtml(replyMSG.toString());
                    player.sendPacket(reply);
                    break;
                case 3:
                    int points = Olympiad.getInstance().getNoblePoints(player.getObjectId());
                    if (points >= 0)
                    {
                        reply = new NpcHtmlMessage(getObjectId());
                        replyMSG = new TextBuilder("<html><body>");
                        replyMSG.append("There are " + points + " Grand Olympiad " +
                                "points granted for this event.<br><br>" +
                                "<a action=\"bypass -h npc_"+getObjectId()+"_OlympiadDesc 2a\">Return</a>");
                        replyMSG.append("</body></html>");

                        reply.setHtml(replyMSG.toString());
                        player.sendPacket(reply);
                    }
                    break;
                case 4:
                    Olympiad.getInstance().registerNoble(player, false);
                    break;
                case 5:
                    Olympiad.getInstance().registerNoble(player, true);
                    break;
                case 6:
                    int passes = Olympiad.getInstance().getNoblessePasses(player.getObjectId());
                    if (passes > 0)
                    {
                        L2ItemInstance item = player.getInventory().addItem("Olympiad", GATE_PASS, passes, player, this);

                        InventoryUpdate iu = new InventoryUpdate();
                        iu.addModifiedItem(item);
                        player.sendPacket(iu);

                        SystemMessage sm = new SystemMessage(SystemMessageId.EARNED_ITEM);
                        sm.addNumber(passes);
                        sm.addItemName(item.getItemId());
                        player.sendPacket(sm);
                    }
                    else
                    {
                        player.sendMessage("Not enough points, or not currently in Valdation Period");
                        //TODO Send HTML packet "Saying not enough olympiad points.
                    }
                    break;
                case 7:
                	L2Multisell.getInstance().separateAndSend(102, player, false, getCastle().getTaxRate());
                    break;
                    default:
                        _logOlymp.warning("Olympiad System: Couldnt send packet for request " + val);
                    break;

            }
        }
        else if (command.startsWith("OlyBuff"))
        {
        	NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
        	String[] params = command.split( " " );
        	int skillId = Integer.parseInt(params[1]);
        	int skillLvl;
        	
        	// Olympiad buff whitelist prevents bypass exploiters
			HashMap<Integer,Integer> buffList = new HashMap<Integer,Integer>();
			buffList.put(1086,2); //Haste Lv2
			buffList.put(1204,2); //Wind Walk Lv2
			buffList.put(1059,3); //Empower Lv3
			buffList.put(1085,3); //Acumen Lv3
			buffList.put(1078,6); //Concentration Lv6
			buffList.put(1068,3); //Might Lv3
			buffList.put(1240,3); //Guidance Lv3
			buffList.put(1077,3); //Focus Lv3 
			buffList.put(1242,3); //Death Whisper Lv3
			buffList.put(1062,2); //Berserk Spirit Lv2
			
			if(!buffList.containsKey(skillId))
				return;
			
			// get skill level from the hashmap
			skillLvl = buffList.get(skillId);
        	
        	L2Skill skill;
        	skill = SkillTable.getInstance().getInfo(skillId,skillLvl);
        	
        	if (player.olyBuff > 0)
           	{
        		skill.getEffects(player, player);
            	player.olyBuff--;
           	}
        	
        	if (player.olyBuff > 0)
           	{
            	html.setFile(Olympiad.OLYMPIAD_HTML_FILE + "olympiad_buffs.htm");
            	html.replace("%objectId%", String.valueOf(getObjectId()));
            	player.sendPacket(html);
            } else
            {
            	html.setFile(Olympiad.OLYMPIAD_HTML_FILE + "olympiad_nobuffs.htm");
            	html.replace("%objectId%", String.valueOf(getObjectId()));
            	player.sendPacket(html);
            	this.deleteMe();                    	
            }
        }
        else if (command.startsWith("Olympiad"))
        {
            int val = Integer.parseInt(command.substring(9,10));

            NpcHtmlMessage reply = new NpcHtmlMessage(getObjectId());
            TextBuilder replyMSG = new TextBuilder("<html><body>");

            switch (val)
            {
                case 1:
                    String[] matches = Olympiad.getInstance().getMatchList();
                    int stad;
                    int showbattle;
                    replyMSG.append("Grand Olympiad Competition View<br>" +
                            "Warning: If you watch an Olympiad game, the " +
                            "summoning of your Servitors or Pets will be " +
                            "cancelled.<br><br>");

                    if (matches == null)
                        replyMSG.append("<br>There are no matches at the moment");
                    else
                    {
                        for (int i = 0; i < matches.length; i++)
                        {
                        	showbattle = Integer.parseInt(matches[i].substring(1,2));
                        	stad = Integer.parseInt(matches[i].substring(4,5));
                        	if (showbattle == 1) {
                        		replyMSG.append("<br><a action=\"bypass -h npc_"+getObjectId()+"_Olympiad 3_" + stad + "\">" +
                                    matches[i] + "</a>");
                        	}

                        }
                    }
                    replyMSG.append("</body></html>");

                    reply.setHtml(replyMSG.toString());
                    player.sendPacket(reply);
                    break;
                case 2:
                    // for example >> Olympiad 1_88
                    int classId = Integer.parseInt(command.substring(11));
                    if ((classId >= 88 && classId <= 118) || (classId >= 131 && classId <= 134) || classId == 136)
                    {
                        replyMSG.append("<center>Grand Olympiad Ranking");
                        replyMSG.append("<img src=\"L2UI.SquareWhite\" width=270 height=1><img src=\"L2UI.SquareBlank\" width=1 height=3>");

                        L2FastList<String> names = Olympiad.getInstance().getClassLeaderBoard(classId);
                        if (names.size() != 0)
                        {
                            replyMSG.append("<table width=270 border=0 bgcolor=\"000000\">");

                            int index = 1;

                            for (String name : names)
                            {
                                replyMSG.append("<tr>");
                                replyMSG.append("<td align=\"left\">" + index + "</td>");
                                replyMSG.append("<td align=\"right\">" + name + "</td>");
                                replyMSG.append("</tr>");
                                index++;
                            }

                            replyMSG.append("</table>");
                        }

                        replyMSG.append("<img src=\"L2UI.SquareWhite\" width=270 height=1> <img src=\"L2UI.SquareBlank\" width=1 height=3>");
                        replyMSG.append("</center>");
                        replyMSG.append("</body></html>");

                        reply.setHtml(replyMSG.toString());
                        player.sendPacket(reply);
                    }
                    break;
                case 3:
                    int id = Integer.parseInt(command.substring(11));
                    Olympiad.getInstance().addSpectator(id, player);
                    break;
                case 4:
                    player.sendPacket(new ExHeroList());
                    break;
                    default:
                        _logOlymp.warning("Olympiad System: Couldnt send packet for request " + val);
                    break;
            }
        }
        else
            super.onBypassFeedback(player, command);
    }

    private void showChatWindow(L2PcInstance player, int val, String suffix)
    {
        String filename = Olympiad.OLYMPIAD_HTML_FILE;

        filename += "noble_desc" + val;
        filename += (suffix != null)? suffix + ".htm" : ".htm";

        if (filename.equals(Olympiad.OLYMPIAD_HTML_FILE + "noble_desc0.htm"))
            filename = Olympiad.OLYMPIAD_HTML_FILE + "noble_main.htm";

        showChatWindow(player, filename);
    }
}
