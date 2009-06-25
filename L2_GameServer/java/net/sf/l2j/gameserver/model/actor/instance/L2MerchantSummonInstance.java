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

import java.util.StringTokenizer;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.TradeController;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2TradeList;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.BuyList;
import net.sf.l2j.gameserver.network.serverpackets.MyTargetSelected;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.SellList;
import net.sf.l2j.gameserver.network.serverpackets.ValidateLocation;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;

/**
 * @author Kerberos
 */
public class L2MerchantSummonInstance extends L2SummonInstance
{
    public L2MerchantSummonInstance(int objectId, L2NpcTemplate template, L2PcInstance owner, L2Skill skill)
    {
    	super(objectId, template, owner, skill);
    }

    @Override
    public void onAction(L2PcInstance player)
    {
    	if (player.isOutOfControl())
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
        
        // Check if the L2PcInstance already target the L2NpcInstance
        if (this != player.getTarget())
        {
            // Set the target of the L2PcInstance player
            player.setTarget(this);

            // Send a Server->Client packet MyTargetSelected to the L2PcInstance player
            MyTargetSelected my = new MyTargetSelected(getObjectId(), 0);
            player.sendPacket(my);

            // Send a Server->Client packet ValidateLocation to correct the L2NpcInstance position and heading on the client
            player.sendPacket(new ValidateLocation(this));
        }
        else
        {
            // Calculate the distance between the L2PcInstance and the L2NpcInstance
        	if (!isInsideRadius(player, 150, false, false))
            {
                // Notify the L2PcInstance AI with AI_INTENTION_INTERACT
                player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
            }
            else
            {
                showMessageWindow(player);
            }
        }
        // Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
        player.sendPacket(ActionFailed.STATIC_PACKET);
    }

    public void onBypassFeedback(L2PcInstance player, String command)
    {
    	StringTokenizer st = new StringTokenizer(command, " ");
        String actualCommand = st.nextToken(); // Get actual command

        if (actualCommand.equalsIgnoreCase("Buy"))
        {
            if (st.countTokens() < 1) return;

            int val = Integer.parseInt(st.nextToken());
            showBuyWindow(player, val);
        }
        else if (actualCommand.equalsIgnoreCase("Sell"))
        {
            showSellWindow(player);
        }
    }
    protected final void showBuyWindow(L2PcInstance player, int val)
    {
        double taxRate = 0;

        taxRate = 50;
        
        player.tempInventoryDisable();

        if (Config.DEBUG)
        {
            _log.fine("Showing buylist");
        }

        L2TradeList list = TradeController.getInstance().getBuyList(val);

        if (list != null && list.getNpcId().equals(String.valueOf(getNpcId())))
        {
            BuyList bl = new BuyList(list, player.getAdena(), taxRate);
            player.sendPacket(bl);
        }
        else
        {
            _log.warning("possible client hacker: "+player.getName()+" attempting to buy from GM shop! < Ban him!");
            _log.warning("buylist id:" + val);
        }

        player.sendPacket(ActionFailed.STATIC_PACKET);
    }
    protected final void showSellWindow(L2PcInstance player)
    {
        if (Config.DEBUG) _log.fine("Showing selllist");

        player.sendPacket(new SellList(player));

        if (Config.DEBUG) _log.fine("Showing sell window");

        player.sendPacket(ActionFailed.STATIC_PACKET);
    }

    private void showMessageWindow(L2PcInstance player)
    {
        player.sendPacket(ActionFailed.STATIC_PACKET);
        String filename = "data/html/merchant/"+getNpcId()+".htm";
        NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
        html.setFile(filename);
        html.replace("%objectId%", String.valueOf(getObjectId()));       
        player.sendPacket(html);
    }
}
