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

import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ExPutEnchantSupportItemResult;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.item.L2Item;

/**
 *
 * @author  KenM
 */
public class RequestExTryToPutEnchantSupportItem extends L2GameClientPacket
{

	private int _supportObjectId;
	private int _enchantObjectId;

	/**
     * @see net.sf.l2j.gameserver.network.clientpackets.L2GameClientPacket#getType()
     */
    @Override
    public String getType()
    {
	    return "[C] D0:50 RequestExTryToPutEnchantSupportItem";
    }

	/**
     * @see net.sf.l2j.gameserver.network.clientpackets.L2GameClientPacket#readImpl()
     */
    @Override
    protected void readImpl()
    {
	    _supportObjectId = readD();
	    _enchantObjectId = readD();
    }

	/**
     * @see net.sf.l2j.gameserver.network.clientpackets.L2GameClientPacket#runImpl()
     */
    @Override
    protected void runImpl()
    {
    	L2PcInstance activeChar = this.getClient().getActiveChar();
	    if (activeChar != null)
	    {
	    	if (activeChar.isEnchanting())
	    	{
	    		L2ItemInstance supportItem = (L2ItemInstance) L2World.getInstance().findObject(_supportObjectId);
	    		L2ItemInstance enchantItem = (L2ItemInstance) L2World.getInstance().findObject(_enchantObjectId);

	    		if (supportItem == null || enchantItem == null)
	    			return;

				int itemType2 = enchantItem.getItem().getType2();
	    		boolean ok = false;

	    		switch (enchantItem.getItem().getCrystalType())
	    		{
	    			case L2Item.CRYSTAL_A:
	    				if (itemType2 == L2Item.TYPE2_WEAPON && supportItem.getItemId() == 12365)
	    					ok = true;
	    				if (itemType2 == L2Item.TYPE2_SHIELD_ARMOR || itemType2 == L2Item.TYPE2_ACCESSORY)
	    					if (supportItem.getItemId() == 12370)
	    						ok = true;
	    				break;
	    			case L2Item.CRYSTAL_B:
	    				if (itemType2 == L2Item.TYPE2_WEAPON && supportItem.getItemId() == 12364)
	    					ok = true;
	    				if (itemType2 == L2Item.TYPE2_SHIELD_ARMOR || itemType2 == L2Item.TYPE2_ACCESSORY)
	    					if (supportItem.getItemId() == 12369)
	    						ok = true;
	    				break;
	    			case L2Item.CRYSTAL_C:
	    				if (itemType2 == L2Item.TYPE2_WEAPON && supportItem.getItemId() == 12363)
	    					ok = true;
	    				if (itemType2 == L2Item.TYPE2_SHIELD_ARMOR || itemType2 == L2Item.TYPE2_ACCESSORY)
	    					if (supportItem.getItemId() == 12368)
	    						ok = true;
	    				break;
	    			case L2Item.CRYSTAL_D:
	    				if (itemType2 == L2Item.TYPE2_WEAPON && supportItem.getItemId() == 12362)
	    					ok = true;
	    				if (itemType2 == L2Item.TYPE2_SHIELD_ARMOR || itemType2 == L2Item.TYPE2_ACCESSORY)
	    					if (supportItem.getItemId() == 12367)
	    						ok = true;
	    				break;
	    			case L2Item.CRYSTAL_S:
	    			case L2Item.CRYSTAL_S80:
	    			case L2Item.CRYSTAL_S84:
	    				if (itemType2 == L2Item.TYPE2_WEAPON && supportItem.getItemId() == 12366)
	    					ok = true;
	    				if (itemType2 == L2Item.TYPE2_SHIELD_ARMOR || itemType2 == L2Item.TYPE2_ACCESSORY)
	    					if (supportItem.getItemId() == 12371)
	    						ok = true;
	    				break;
	    		}
	    		
	    		if (enchantItem.getEnchantLevel() > 9)
	    			ok = false;
	    		
	    		if (!ok)
	    		{
	    			// message may be custom
	    			activeChar.sendPacket(new SystemMessage(SystemMessageId.INAPPROPRIATE_ENCHANT_CONDITION));
		    		activeChar.setActiveEnchantSupportItem(null);
	    			activeChar.sendPacket(new ExPutEnchantSupportItemResult(0));
	    			return;
	    		}
	    		activeChar.setActiveEnchantSupportItem(supportItem);
				activeChar.sendPacket(new ExPutEnchantSupportItemResult(_supportObjectId));
	    	}
	    }
    }
	
}
