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
import net.sf.l2j.gameserver.network.serverpackets.ExPutItemResultForVariationMake;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 * Format:(ch) d
 * @author  -Wooden-
 */
public final class RequestConfirmTargetItem extends AbstractRefinePacket
{
	private static final String _C__D0_29_REQUESTCONFIRMTARGETITEM = "[C] D0:29 RequestConfirmTargetItem";
	private int _itemObjId;

	@Override
	protected void readImpl()
	{
		_itemObjId = readD();
	}

	@Override
	protected void runImpl()
	{
		final L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;

		final L2ItemInstance item = (L2ItemInstance)L2World.getInstance().findObject(_itemObjId);
		if (item == null)
			return;

		if (!isValid(activeChar, item))
		{
			// Different system message here
			if (item.isAugmented())
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.ONCE_AN_ITEM_IS_AUGMENTED_IT_CANNOT_BE_AUGMENTED_AGAIN));
				return;
			}

			activeChar.sendPacket(new SystemMessage(SystemMessageId.THIS_IS_NOT_A_SUITABLE_ITEM));
			return;
		}

		activeChar.sendPacket(new ExPutItemResultForVariationMake(_itemObjId));
		activeChar.sendPacket(new SystemMessage(SystemMessageId.SELECT_THE_CATALYST_FOR_AUGMENTATION));
	}

	/**
	 * @see net.sf.l2j.gameserver.BasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _C__D0_29_REQUESTCONFIRMTARGETITEM;
	}
}
