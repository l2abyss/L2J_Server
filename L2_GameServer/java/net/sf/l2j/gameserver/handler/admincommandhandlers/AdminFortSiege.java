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
package net.sf.l2j.gameserver.handler.admincommandhandlers;

import java.util.StringTokenizer;

import javolution.text.TextBuilder;
import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.instancemanager.FortManager;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.Fort;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;

/**
 * This class handles all siege commands:
 * Todo: change the class name, and neaten it up
 *
 *
 */
public class AdminFortSiege implements IAdminCommandHandler
{
    //private static Logger _log = Logger.getLogger(AdminFortSiege.class.getName());

    private static final String[] ADMIN_COMMANDS = {"admin_fortsiege",
        "admin_add_fortattacker", "admin_add_fortdefender", "admin_add_fortguard",
        "admin_list_fortsiege_clans", "admin_clear_fortsiege_list",
        "admin_move_fortdefenders", "admin_spawn_fortdoors",
        "admin_endfortsiege", "admin_startfortsiege",
        "admin_setfort", "admin_removefort"
    };

    public boolean useAdminCommand(String command, L2PcInstance activeChar)
    {
        StringTokenizer st = new StringTokenizer(command, " ");
        command = st.nextToken(); // Get actual command

        // Get fort
        Fort fort = null;
        if (st.hasMoreTokens())
           fort = FortManager.getInstance().getFort(st.nextToken());
        // Get fort
        @SuppressWarnings("unused")
        String val = "";
        if (st.hasMoreTokens())
            val = st.nextToken();
        if ((fort == null  || fort.getFortId() < 0))
            // No fort specified
            showFortSelectPage(activeChar);
        else
        {
            L2Object target = activeChar.getTarget();
            L2PcInstance player = null;
            if (target instanceof L2PcInstance)
                player = (L2PcInstance)target;

            if (command.equalsIgnoreCase("admin_add_fortattacker"))
            {
                if (player == null)
                    activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
                else
                    fort.getSiege().registerAttacker(player,true);
            }
            else if (command.equalsIgnoreCase("admin_add_fortdefender"))
            {
                if (player == null)
                    activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
                else
                    fort.getSiege().registerDefender(player,true);
            }
//            else if (command.equalsIgnoreCase("admin_add_guard"))
//            {
//                try
//                {
//                    int npcId = Integer.parseInt(val);
//                    fort.getSiege().getFortSiegeGuardManager().addFortSiegeGuard(activeChar, npcId);
//                }
//                catch (Exception e)
//                {
//                    activeChar.sendMessage("Usage: //add_guard npcId");
//                }
//            }
            else if (command.equalsIgnoreCase("admin_clear_fortsiege_list"))
            {
                fort.getSiege().clearSiegeClan();
            }
            else if (command.equalsIgnoreCase("admin_endfortsiege"))
            {
                fort.getSiege().endSiege();
            }
            else if (command.equalsIgnoreCase("admin_list_fortsiege_clans"))
            {
                fort.getSiege().listRegisterClan(activeChar);
                return true;
            }
            else if (command.equalsIgnoreCase("admin_move_fortdefenders"))
            {
                activeChar.sendMessage("Not implemented yet.");
            }
            else if (command.equalsIgnoreCase("admin_setfort"))
            {
                if (player == null || player.getClan() == null)
                    activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
                else
                    fort.setOwner(player.getClan());
            }
            else if (command.equalsIgnoreCase("admin_removefort"))
            {
                L2Clan clan = ClanTable.getInstance().getClan(fort.getOwnerId());
                if (clan != null)
                    fort.removeOwner(clan);
                else
                    activeChar.sendMessage("Unable to remove fort");
            }
            else if (command.equalsIgnoreCase("admin_spawn_fortdoors"))
            {
                fort.spawnDoor();
            }
            else if (command.equalsIgnoreCase("admin_startfortsiege"))
            {
                fort.getSiege().startSiege();
            }

            showFortSiegePage(activeChar, fort.getName());
        }
        return true;
    }

    private void showFortSelectPage(L2PcInstance activeChar)
    {
        int i=0;
        NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
        adminReply.setFile("data/html/admin/forts.htm");
        TextBuilder cList = new TextBuilder();
        for (Fort fort: FortManager.getInstance().getForts())
        {
            if (fort != null)
            {
                String name=fort.getName();
                cList.append("<td fixwidth=90><a action=\"bypass -h admin_fortsiege "+name+"\">"+name+"</a></td>");
                i++;
            }
            if (i>2)
            {
                cList.append("</tr><tr>");
                i=0;
            }
        }
        adminReply.replace("%forts%", cList.toString());
        activeChar.sendPacket(adminReply);
    }

    private void showFortSiegePage(L2PcInstance activeChar, String fortName)
    {
        NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
        adminReply.setFile("data/html/admin/fort.htm");
        adminReply.replace("%fortName%", fortName);
        activeChar.sendPacket(adminReply);
    }

    public String[] getAdminCommandList() {
        return ADMIN_COMMANDS;
    }

}
