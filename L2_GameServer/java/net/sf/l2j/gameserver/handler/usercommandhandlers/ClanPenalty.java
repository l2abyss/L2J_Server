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

package net.sf.l2j.gameserver.handler.usercommandhandlers;

import javolution.text.TextBuilder;
import net.sf.l2j.gameserver.handler.IUserCommandHandler;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * Support for clan penalty user command.
 * @author Tempy
 */
public class ClanPenalty implements IUserCommandHandler
{
    private static final int[] COMMAND_IDS = { 100 };

    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.handler.IUserCommandHandler#useUserCommand(int, net.sf.l2j.gameserver.model.L2PcInstance)
     */
    public boolean useUserCommand(int id, L2PcInstance activeChar)
    {
        if (id != COMMAND_IDS[0]) return false;

        String penaltyStr = "No current penalties in effect.";

        TextBuilder htmlContent = new TextBuilder("<html><body>");
        htmlContent.append("<center><table width=\"270\" border=\"0\" bgcolor=\"111111\">");
        htmlContent.append("<tr><td width=\"170\">Penalty</td>");
        htmlContent.append("<td width=\"100\" align=\"center\">Expiration Date</td></tr>");
        htmlContent.append("</table><table width=\"270\" border=\"0\">");
        htmlContent.append("<tr><td>" + penaltyStr + "</td></tr>");
        htmlContent.append("</table></center>");
        htmlContent.append("</body></html>");

        NpcHtmlMessage penaltyHtml = new NpcHtmlMessage(0);
        penaltyHtml.setHtml(htmlContent.toString());
        activeChar.sendPacket(penaltyHtml);

        return true;
    }

    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.handler.IUserCommandHandler#getUserCommandList()
     */
    public int[] getUserCommandList()
    {
        return COMMAND_IDS;
    }
}
