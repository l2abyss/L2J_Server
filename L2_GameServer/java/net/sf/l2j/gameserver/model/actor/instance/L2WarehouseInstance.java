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

import java.util.Map;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.PcFreight;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.PackageToList;
import net.sf.l2j.gameserver.network.serverpackets.SortedWareHouseWithdrawalList;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.network.serverpackets.WareHouseDepositList;
import net.sf.l2j.gameserver.network.serverpackets.WareHouseWithdrawalList;
import net.sf.l2j.gameserver.network.serverpackets.SortedWareHouseWithdrawalList.WarehouseListType;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;
import net.sf.l2j.gameserver.util.IllegalPlayerAction;
import net.sf.l2j.gameserver.util.Util;

/**
 * This class ...
 *
 * @version $Revision: 1.3.4.10 $ $Date: 2005/04/06 16:13:41 $
 */
public final class L2WarehouseInstance extends L2FolkInstance
{
    //private static Logger _log = Logger.getLogger(L2WarehouseInstance.class.getName());

    /**
     * @param template
     */
    public L2WarehouseInstance(int objectId, L2NpcTemplate template)
    {
        super(objectId, template);
    }

    @Override
    public String getHtmlPath(int npcId, int val)
    {
        String pom = "";
        if (val == 0)
        {
            pom = "" + npcId;
        }
        else
        {
            pom = npcId + "-" + val;
        }
        return "data/html/warehouse/" + pom + ".htm";
    }
    
    private void showRetrieveWindow(L2PcInstance player, WarehouseListType itemtype, byte sortorder)
	{
    	player.sendPacket(ActionFailed.STATIC_PACKET);
    	player.setActiveWarehouse(player.getWarehouse());

    	if (player.getActiveWarehouse().getSize() == 0)
    	{
    		player.sendPacket(new SystemMessage(SystemMessageId.NO_ITEM_DEPOSITED_IN_WH));
    		return;
    	}

		if (Config.DEBUG)
			_log.fine("Showing stored items");
		
		player.sendPacket(new SortedWareHouseWithdrawalList(player, WareHouseWithdrawalList.PRIVATE, itemtype, sortorder));
	}

    private void showRetrieveWindow(L2PcInstance player)
    {
        player.sendPacket(ActionFailed.STATIC_PACKET);
        player.setActiveWarehouse(player.getWarehouse());

        if (player.getActiveWarehouse().getSize() == 0)
        {
        	player.sendPacket(new SystemMessage(SystemMessageId.NO_ITEM_DEPOSITED_IN_WH));
        	return;
        }

        if (Config.DEBUG) _log.fine("Showing stored items");
        player.sendPacket(new WareHouseWithdrawalList(player, WareHouseWithdrawalList.PRIVATE));
    }

    private void showDepositWindow(L2PcInstance player)
    {
        player.sendPacket(ActionFailed.STATIC_PACKET);
        player.setActiveWarehouse(player.getWarehouse());
        player.tempInvetoryDisable();
        if (Config.DEBUG) _log.fine("Showing items to deposit");

        player.sendPacket(new WareHouseDepositList(player, WareHouseDepositList.PRIVATE));
    }

    private void showDepositWindowClan(L2PcInstance player)
    {
        player.sendPacket(ActionFailed.STATIC_PACKET);
    	if (player.getClan() != null)
    	{
            if (player.getClan().getLevel() == 0)
                player.sendPacket(new SystemMessage(SystemMessageId.ONLY_LEVEL_1_CLAN_OR_HIGHER_CAN_USE_WAREHOUSE));
            else
            {
                player.setActiveWarehouse(player.getClan().getWarehouse());
                player.tempInvetoryDisable();
                if (Config.DEBUG) _log.fine("Showing items to deposit - clan");

                WareHouseDepositList dl = new WareHouseDepositList(player, WareHouseDepositList.CLAN);
                player.sendPacket(dl);
            }
    	}
    }
    
    private void showWithdrawWindowClan(L2PcInstance player, WarehouseListType itemtype, byte sortorder)
    {
    	player.sendPacket(ActionFailed.STATIC_PACKET);
    	if ((player.getClanPrivileges() & L2Clan.CP_CL_VIEW_WAREHOUSE) != L2Clan.CP_CL_VIEW_WAREHOUSE)
    	{
    		player.sendPacket(new SystemMessage(SystemMessageId.YOU_DO_NOT_HAVE_THE_RIGHT_TO_USE_CLAN_WAREHOUSE));
    		return;
    	}
    	else
    	{
    		if (player.getClan().getLevel() == 0)
    			player.sendPacket(new SystemMessage(SystemMessageId.ONLY_LEVEL_1_CLAN_OR_HIGHER_CAN_USE_WAREHOUSE));
    		else
    		{
    			player.setActiveWarehouse(player.getClan().getWarehouse());
    			if (Config.DEBUG) _log.fine("Showing items to deposit - clan");
    			player.sendPacket(new SortedWareHouseWithdrawalList(player, WareHouseWithdrawalList.CLAN, itemtype, sortorder));
    		}
    	}
	}

    private void showWithdrawWindowClan(L2PcInstance player)
    {
        player.sendPacket(ActionFailed.STATIC_PACKET);
    	if ((player.getClanPrivileges() & L2Clan.CP_CL_VIEW_WAREHOUSE) != L2Clan.CP_CL_VIEW_WAREHOUSE)
    	{
    		player.sendPacket(new SystemMessage(SystemMessageId.YOU_DO_NOT_HAVE_THE_RIGHT_TO_USE_CLAN_WAREHOUSE));
    		return;
    	}
    	else
    	{
            if (player.getClan().getLevel() == 0)
                player.sendPacket(new SystemMessage(SystemMessageId.ONLY_LEVEL_1_CLAN_OR_HIGHER_CAN_USE_WAREHOUSE));
            else
            {
            	player.setActiveWarehouse(player.getClan().getWarehouse());
                if (Config.DEBUG) _log.fine("Showing items to deposit - clan");
                player.sendPacket(new WareHouseWithdrawalList(player, WareHouseWithdrawalList.CLAN));
            }
    	}
    }
    
    private void showWithdrawWindowFreight(L2PcInstance player, WarehouseListType itemtype, byte sortorder)
	{
    	player.sendPacket(ActionFailed.STATIC_PACKET);
    	if (Config.DEBUG) _log.fine("Showing freightened items");

    	PcFreight freight = player.getFreight();

    	if (freight != null)
    	{
    		if (freight.getSize() > 0)
    		{
    			if (Config.ALT_GAME_FREIGHTS)
    				freight.setActiveLocation(0);
    			else
    				freight.setActiveLocation(getWorldRegion().hashCode());
    			
    			player.setActiveWarehouse(freight);
    			player.sendPacket(new SortedWareHouseWithdrawalList(player, WareHouseWithdrawalList.FREIGHT, itemtype, sortorder));
    		}
    		else
    			player.sendPacket(new SystemMessage(SystemMessageId.NO_ITEM_DEPOSITED_IN_WH));
    	}
    	else
    		if (Config.DEBUG) _log.fine("no items freightened");
	}

    private void showWithdrawWindowFreight(L2PcInstance player)
    {
        player.sendPacket(ActionFailed.STATIC_PACKET);
        if (Config.DEBUG) _log.fine("Showing freightened items");

        PcFreight freight = player.getFreight();

        if (freight != null)
        {
        	if (freight.getSize() > 0)
        	{
	        	if (Config.ALT_GAME_FREIGHTS)
	        	{
	                freight.setActiveLocation(0);
	        	} else
	        	{
	        		freight.setActiveLocation(getWorldRegion().hashCode());
	        	}
	            player.setActiveWarehouse(freight);
	            player.sendPacket(new WareHouseWithdrawalList(player, WareHouseWithdrawalList.FREIGHT));
        	}
        	else
        	{
            	player.sendPacket(new SystemMessage(SystemMessageId.NO_ITEM_DEPOSITED_IN_WH));
        	}
        }
        else
        {
            if (Config.DEBUG) _log.fine("no items freightened");
        }
    }

    private void showDepositWindowFreight(L2PcInstance player)
    {
        // No other chars in the account of this player
        if (player.getAccountChars().size() == 0)
        {
            player.sendPacket(new SystemMessage(SystemMessageId.CHARACTER_DOES_NOT_EXIST));
        }
        // One or more chars other than this player for this account
        else
        {

            Map<Integer, String> chars = player.getAccountChars();

            if (chars.size() < 1)
            {
                player.sendPacket(ActionFailed.STATIC_PACKET);
                return;
            }

            player.sendPacket(new PackageToList(chars));

            if (Config.DEBUG)
                _log.fine("Showing destination chars to freight - char src: " + player.getName());
        }
    }

    private void showDepositWindowFreight(L2PcInstance player, int obj_Id)
    {
        player.sendPacket(ActionFailed.STATIC_PACKET);
        L2PcInstance destChar = L2PcInstance.load(obj_Id);
        if (destChar == null)
        {
            // Something went wrong!
            if (Config.DEBUG)
                _log.warning("Error retrieving a target object for char " + player.getName()
                    + " - using freight.");
            return;
        }

        PcFreight freight = destChar.getFreight();
    	if (Config.ALT_GAME_FREIGHTS)
    	{
            freight.setActiveLocation(0);
    	} else
    	{
    		freight.setActiveLocation(getWorldRegion().hashCode());
    	}
        player.setActiveWarehouse(freight);
        player.tempInvetoryDisable();
        destChar.deleteMe();

        if (Config.DEBUG) _log.fine("Showing items to freight");
        player.sendPacket(new WareHouseDepositList(player, WareHouseDepositList.FREIGHT));
    }

    @Override
	public void onBypassFeedback(L2PcInstance player, String command)
    {
        // lil check to prevent enchant exploit
        if (player.getActiveEnchantItem() != null)
        {
        	Util.handleIllegalPlayerAction(player, "Player " + player.getName() + " trying to use enchant exploit, ban this player!", IllegalPlayerAction.PUNISH_KICK);
            return;
        }
        
        String param[] = command.split("_");

        if (command.startsWith("WithdrawP"))
        {
        	if (Config.L2JMOD_ENABLE_WAREHOUSESORTING_PRIVATE)
        	{
        		String htmFile = "data/html/mods/WhSortedP.htm";
        		String htmContent = HtmCache.getInstance().getHtm(htmFile);
        		if (htmContent != null)
        		{
        			NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(getObjectId());
        			npcHtmlMessage.setHtml(htmContent);
        			npcHtmlMessage.replace("%objectId%", String.valueOf(getObjectId()));
        			player.sendPacket(npcHtmlMessage);
        		}
        		else
        		{
        			_log.warning("Missing htm: " + htmFile + " !");
        		}
        	}
        	else
        		showRetrieveWindow(player);
        }
        else if (command.startsWith("WithdrawSortedP"))
        {
        	if (param.length > 2)
        		showRetrieveWindow(player, WarehouseListType.valueOf(param[1]), SortedWareHouseWithdrawalList.getOrder(param[2]));
        	else if (param.length > 1)
        		showRetrieveWindow(player, WarehouseListType.valueOf(param[1]), SortedWareHouseWithdrawalList.A2Z);
        	else
        		showRetrieveWindow(player, WarehouseListType.ALL, SortedWareHouseWithdrawalList.A2Z);
        }
        else if (command.equals("DepositP"))
        {
            showDepositWindow(player);
        }
        else if (command.startsWith("WithdrawC"))
        {
        	if (Config.L2JMOD_ENABLE_WAREHOUSESORTING_CLAN)
        	{
        		String htmFile = "data/html/mods/WhSortedC.htm";
        		String htmContent = HtmCache.getInstance().getHtm(htmFile);
        		if (htmContent != null)
        		{
        			NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(getObjectId());
        			npcHtmlMessage.setHtml(htmContent);
        			npcHtmlMessage.replace("%objectId%", String.valueOf(getObjectId()));
        			player.sendPacket(npcHtmlMessage);
        		}
        		else
        		{
        			_log.warning("Missing htm: " + htmFile + " !");
        		}
        	}
        	else
        		showWithdrawWindowClan(player);
        }
        else if (command.startsWith("WithdrawSortedC"))
        {
        	if (param.length > 2)
        		showWithdrawWindowClan(player, WarehouseListType.valueOf(param[1]), SortedWareHouseWithdrawalList.getOrder(param[2]));
        	else if (param.length > 1)
        		showWithdrawWindowClan(player, WarehouseListType.valueOf(param[1]), SortedWareHouseWithdrawalList.A2Z);
        	else
        		showWithdrawWindowClan(player, WarehouseListType.ALL, SortedWareHouseWithdrawalList.A2Z);
        }
        else if (command.equals("DepositC"))
        {
            showDepositWindowClan(player);
        }
        else if (command.startsWith("WithdrawF"))
        {
            if (Config.ALLOW_FREIGHT)
            {
            	if (Config.L2JMOD_ENABLE_WAREHOUSESORTING_FREIGHT)
            	{
            		String htmFile = "data/html/mods/WhSortedF.htm";
            		String htmContent = HtmCache.getInstance().getHtm(htmFile);
            		if (htmContent != null)
            		{
            			NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(getObjectId());
            			npcHtmlMessage.setHtml(htmContent);
            			npcHtmlMessage.replace("%objectId%", String.valueOf(getObjectId()));
            			player.sendPacket(npcHtmlMessage);
            		}
            		else
            		{
            			_log.warning("Missing htm: " + htmFile + " !");
            		}
            	}
            	else
            		showWithdrawWindowFreight(player);
            }
        }
        else if (command.startsWith("WithdrawSortedF"))
        {
        	if (Config.ALLOW_FREIGHT)
        	{
        		if (param.length > 2) 
        			showWithdrawWindowFreight(player, WarehouseListType.valueOf(param[1]), SortedWareHouseWithdrawalList.getOrder(param[2]));
        		else if (param.length > 1)
        			showWithdrawWindowFreight(player, WarehouseListType.valueOf(param[1]), SortedWareHouseWithdrawalList.A2Z);
        		else
        			showWithdrawWindowFreight(player, WarehouseListType.ALL, SortedWareHouseWithdrawalList.A2Z);
        	}
        }
        else if (command.startsWith("DepositF"))
        {
            if (Config.ALLOW_FREIGHT)
            {
                showDepositWindowFreight(player);
            }
        }
        else if (command.startsWith("FreightChar"))
        {
            if (Config.ALLOW_FREIGHT)
            {
                int startOfId = command.lastIndexOf("_") + 1;
                String id = command.substring(startOfId);
                showDepositWindowFreight(player, Integer.parseInt(id));
            }
        }
        else
        {
            // this class dont know any other commands, let forward
            // the command to the parent class

            super.onBypassFeedback(player, command);
        }
    }
}
