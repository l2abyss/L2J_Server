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

import java.util.logging.Logger;

import javolution.util.FastMap;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.NpcBufferTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Multisell;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.olympiad.Olympiad;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ExHeroList;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;
import net.sf.l2j.gameserver.util.StringUtil;
import net.sf.l2j.util.L2FastList;

/**
 * Olympiad Npc's Instance
 *
 * @author godson
 */

public class L2OlympiadManagerInstance extends L2NpcInstance
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
    	int npcId = getNpcId();
    	
        if (command.startsWith("OlympiadDesc"))
        {
            int val = Integer.parseInt(command.substring(13,14));
            String suffix = command.substring(14);
            showChatWindow(player, val, suffix);
        }
        else if (command.startsWith("OlympiadNoble"))
        {
        	if (!player.isNoble() || player.getClassId().level() < 3)
                return;

            int val = Integer.parseInt(command.substring(14));
            NpcHtmlMessage reply;

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

                    {
                    reply = new NpcHtmlMessage(getObjectId());
                    final String replyMSG = StringUtil.concat(
                            "<html><body>" +
                            "The number of people on the waiting list for " +
                            "Grand Olympiad" +
                            "<center>" +
                            "<img src=\"L2UI.SquareWhite\" width=270 height=1><img src=\"L2UI.SquareBlank\" width=1 height=3>" +
                            "<table width=270 border=0 bgcolor=\"000000\">" +
                            "<tr>" +
                            "<td align=\"left\">General</td>" +
                            "<td align=\"right\">",
                            String.valueOf(classed),
                            "</td>" +
                            "</tr>" +
                            "<tr>" +
                            "<td align=\"left\">Not class-defined</td>" +
                            "<td align=\"right\">",
                            String.valueOf(nonClassed),
                            "</td>" +
                            "</tr>" +
                            "</table><br>" +
                            "<img src=\"L2UI.SquareWhite\" width=270 height=1> <img src=\"L2UI.SquareBlank\" width=1 height=3>" +
                            "<button value=\"Back\" action=\"bypass -h npc_",
                            String.valueOf(getObjectId()),
                            "_OlympiadDesc 2a\" " +
                            "width=40 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></center>" +
                            "</body></html>"
                            );
                    reply.setHtml(replyMSG);
                    player.sendPacket(reply);
                    }
                    break;
                case 3:
                    int points = Olympiad.getInstance().getNoblePoints(player.getObjectId());
                    if (points >= 0) {
                        reply = new NpcHtmlMessage(getObjectId());
                        final String replyMSG = StringUtil.concat(
                                "<html><body>" +
                                "There are ",
                                String.valueOf(points),
                                " Grand Olympiad " +
                                "points granted for this event.<br><br>" +
                                "<a action=\"bypass -h npc_",
                                String.valueOf(getObjectId()),
                                "_OlympiadDesc 2a\">Return</a>" +
                                "</body></html>"
                                );
                        reply.setHtml(replyMSG);
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
                        sm.addItemName(item);
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
        	String[] params = command.split(" ");
        	
        	if (params[1] == null)
        	{
        		_log.warning("Olympiad Buffer Warning: npcId = " + npcId + " has no buffGroup set in the bypass for the buff selected.");
        		return;
        	}
        	int buffGroup = Integer.parseInt(params[1]);

    		int[] npcBuffGroupInfo = NpcBufferTable.getInstance().getSkillInfo(npcId, buffGroup);
    		
    		if (npcBuffGroupInfo == null)
    		{
    			_log.warning("Olympiad Buffer Warning: npcId = " + npcId + " Location: " + getX() + ", " + getY() + ", " + getZ() + " Player: " + player.getName() + " has tried to use skill group (" + buffGroup + ") not assigned to the NPC Buffer!");
    			return;
    		}
    		
    		int skillId = npcBuffGroupInfo[0];
    		int skillLevel = npcBuffGroupInfo[1];

     		L2Skill skill;
        	skill = SkillTable.getInstance().getInfo(skillId,skillLevel);
        	
        	if (player.olyBuff > 0)
           	{
        		if (skill != null)
        		{
        			skill.getEffects(player, player);
            		player.olyBuff--;
        		}
           	}
        	
        	if (player.olyBuff > 0)
           	{
            	html.setFile(Olympiad.OLYMPIAD_HTML_PATH + "olympiad_buffs.htm");
            	html.replace("%objectId%", String.valueOf(getObjectId()));
            	player.sendPacket(html);
            } 
        	else
            {
            	html.setFile(Olympiad.OLYMPIAD_HTML_PATH + "olympiad_nobuffs.htm");
            	html.replace("%objectId%", String.valueOf(getObjectId()));
            	player.sendPacket(html);
            	this.deleteMe();                    	
            }
        } else if (command.startsWith("Olympiad")) {
            int val = Integer.parseInt(command.substring(9,10));

            NpcHtmlMessage reply = new NpcHtmlMessage(getObjectId());
            final StringBuilder replyMSG;

            switch (val) {
                case 1:
                    FastMap<Integer, String> matches = Olympiad.getInstance().getMatchList();
                    replyMSG = StringUtil.startAppend(
                            500 + Olympiad.getStadiumCount() * 100,
                            "<html><body>" +
                            "<br>Grand Olympiad Competition View <br> Warning: " +
                            "If you choose to watch an Olympiad game, any summoning of Servitors " +
                            "or Pets will be canceled. <br><br>"
                            );

                    for (int i = 0; i < Olympiad.getStadiumCount(); i++) {
                    	int arenaID = i + 1;
                    	String title = "";
                        
                    	if (matches.containsKey(i)) {
                        	title = matches.get(i);
                        } else {
                        	title = "Initial State";
                        }

                        StringUtil.append(replyMSG,
                                "<a action=\"bypass -h npc_",
                                String.valueOf(getObjectId()),
                                "_Olympiad 3_",
                                String.valueOf(i),
                                "\">" +
                                "Arena ",
                                String.valueOf(arenaID),
                                "&nbsp;&nbsp;&nbsp;",
                                title,
                                "</a><br>");
                    }

                    StringUtil.append(replyMSG,
                            "<img src=\"L2UI.SquareWhite\" width=270 height=1> <img src=\"L2UI.SquareBlank\" width=1 height=3>" +
                            "<table width=270 border=0 cellpadding=0 cellspacing=0>" +
                            "<tr><td width=90 height=20 align=center>" +
                            "<button value=\"Back\" action=\"bypass -h npc_",
                            String.valueOf(getObjectId()),
                            "_Chat 0\" width=80 height=27 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">" +
                            "</td></tr></table></body></html>"
                            );
                    reply.setHtml(replyMSG.toString());
                    player.sendPacket(reply);
                    break;
                case 2:
                    // for example >> Olympiad 1_88
                    int classId = Integer.parseInt(command.substring(11));
                    if ((classId >= 88 && classId <= 118) || (classId >= 131 && classId <= 134) || classId == 136) {
                        L2FastList<String> names = Olympiad.getInstance().getClassLeaderBoard(classId);
                        replyMSG = StringUtil.startAppend(
                                500 + names.size() * 80,
                                "<html><body>" +
                                "<center>Grand Olympiad Ranking" +
                                "<img src=\"L2UI.SquareWhite\" width=270 height=1><img src=\"L2UI.SquareBlank\" width=1 height=3>"
                                );

                        if (names.size() != 0) {
                            replyMSG.append("<table width=270 border=0 bgcolor=\"000000\">");

                            int index = 1;

                            for (String name : names) {
                                StringUtil.append(replyMSG,
                                        "<tr>" +
                                        "<td align=\"left\">",
                                        String.valueOf(index++),
                                        "</td>" +
                                        "<td align=\"right\">",
                                        name,
                                        "</td>" +
                                        "</tr>");
                            }

                            replyMSG.append("</table>");
                        }

                        StringUtil.append(replyMSG,
                                "<img src=\"L2UI.SquareWhite\" width=270 height=1> <img src=\"L2UI.SquareBlank\" width=1 height=3>" +
                                "<button value=\"Back\" action=\"bypass -h npc_",
                                String.valueOf(getObjectId()),
                                "_OlympiadDesc 3a\" width=80 height=26 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></center>" +
                                "</body></html>");

                        reply.setHtml(replyMSG.toString());
                        player.sendPacket(reply);
                    }
                    break;
                case 3:
                    int id = Integer.parseInt(command.substring(11));
                    Olympiad.addSpectator(id, player, true);
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
        String filename = Olympiad.OLYMPIAD_HTML_PATH;

        filename += "noble_desc" + val;
        filename += (suffix != null)? suffix + ".htm" : ".htm";

        if (filename.equals(Olympiad.OLYMPIAD_HTML_PATH + "noble_desc0.htm"))
            filename = Olympiad.OLYMPIAD_HTML_PATH + "noble_main.htm";

        showChatWindow(player, filename);
    }
}
