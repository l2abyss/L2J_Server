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
package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.HennaTable;
import net.sf.l2j.gameserver.datatables.HennaTreeTable;
import net.sf.l2j.gameserver.model.L2HennaInstance;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.item.L2Henna;
import net.sf.l2j.gameserver.util.Util;

/**
 * This class ...
 *
 * @version $Revision$ $Date$
 */
public final class RequestHennaEquip extends L2GameClientPacket
{
	private static final String _C__BC_RequestHennaEquip = "[C] bc RequestHennaEquip";
	//private static Logger _log = Logger.getLogger(RequestHennaEquip.class.getName());
	private int _symbolId;
	// format  cd

	/**
	 * packet type id 0xbb
	 * format:		cd
	 * @param decrypt
	 */
	@Override
	protected void readImpl()
	{
		_symbolId  = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();

		if (activeChar == null)
		    return;

		L2Henna template = HennaTable.getInstance().getTemplate(_symbolId);

        if (template == null)
            return;

    	L2HennaInstance temp = new L2HennaInstance(template);
    	int _count = 0;
    	
    	/* Prevents henna drawing exploit: 
    	   1) talk to L2SymbolMakerInstance 
    	   2) RequestHennaList
    	   3) Don't close the window and go to a GrandMaster and change your subclass
    	   4) Get SymbolMaker range again and press draw
    	   You could draw any kind of henna just having the required subclass...
    	 */
    	boolean cheater = true;
    	for (L2HennaInstance h : HennaTreeTable.getInstance().getAvailableHenna(activeChar.getClassId()))
    	{
    	    if (h.getSymbolId() == temp.getSymbolId()) 
    	    {
    	        cheater = false;
    	        break;
    	    }
    	}    	
		try{
			_count = activeChar.getInventory().getItemByItemId(temp.getItemIdDye()).getCount();
		}
		catch(Exception e){}

		if (!cheater && (_count >= temp.getAmountDyeRequire())&& (activeChar.getAdena()>= temp.getPrice()) && activeChar.addHenna(temp))
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_DISAPPEARED);
			sm.addNumber(temp.getItemIdDye());
			activeChar.sendPacket(sm);
			sm = null;
			activeChar.sendPacket(new SystemMessage(SystemMessageId.SYMBOL_ADDED));

			//HennaInfo hi = new HennaInfo(temp,activeChar);
			//activeChar.sendPacket(hi);

			activeChar.getInventory().reduceAdena("Henna", temp.getPrice(), activeChar, activeChar.getLastFolkNPC());
			L2ItemInstance dyeToUpdate = activeChar.getInventory().destroyItemByItemId("Henna", temp.getItemIdDye(),temp.getAmountDyeRequire(), activeChar, activeChar.getLastFolkNPC());

			//update inventory
			InventoryUpdate iu = new InventoryUpdate();
            iu.addModifiedItem(activeChar.getInventory().getAdenaInstance());
			iu.addModifiedItem(dyeToUpdate);
			activeChar.sendPacket(iu);
		}
		else
        {
			activeChar.sendPacket(new SystemMessage(SystemMessageId.CANT_DRAW_SYMBOL));
			if ((!activeChar.isGM()) && (cheater)) Util.handleIllegalPlayerAction(activeChar,"Exploit attempt: Character "+activeChar.getName()+" of account "+activeChar.getAccountName()+" tryed to add a forbidden henna.",Config.DEFAULT_PUNISH);
		}
	}

	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _C__BC_RequestHennaEquip;
	}
}
