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
package com.l2jserver.gameserver.network.clientpackets;

import com.l2jserver.gameserver.model.L2ItemInstance;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.ExPutEnchantTargetItemResult;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;

/**
 *
 * @author  KenM
 */
public class RequestExTryToPutEnchantTargetItem extends AbstractEnchantPacket
{
	
	private int _objectId = 0;
	
	/**
	 * @see com.l2jserver.gameserver.network.clientpackets.L2GameClientPacket#getType()
	 */
	@Override
	public String getType()
	{
		return "[C] D0:4F RequestExTryToPutEnchantTargetItem".intern();
	}
	
	/**
	 * @see com.l2jserver.gameserver.network.clientpackets.L2GameClientPacket#readImpl()
	 */
	@Override
	protected void readImpl()
	{
		_objectId = readD();
	}
	
	/**
	 * @see com.l2jserver.gameserver.network.clientpackets.L2GameClientPacket#runImpl()
	 */
	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		
		if (_objectId == 0)
			return;
		
		if (activeChar != null)
		{
			if (activeChar.isEnchanting())
				return;
			
			L2ItemInstance item = activeChar.getInventory().getItemByObjectId(_objectId);
			L2ItemInstance scroll = activeChar.getActiveEnchantItem();
			
			if (item == null || scroll == null)
				return;
			
			// template for scroll
			EnchantScroll scrollTemplate = getEnchantScroll(scroll);
			
			if (!scrollTemplate.isValid(item) || !isEnchantable(item))
			{
				activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.DOES_NOT_FIT_SCROLL_CONDITIONS));
				activeChar.setActiveEnchantItem(null);
				activeChar.sendPacket(new ExPutEnchantTargetItemResult(0));
				return;
			}
			activeChar.setIsEnchanting(true);
			activeChar.setActiveEnchantTimestamp(System.currentTimeMillis());
			activeChar.sendPacket(new ExPutEnchantTargetItemResult(_objectId));
		}
	}
}
