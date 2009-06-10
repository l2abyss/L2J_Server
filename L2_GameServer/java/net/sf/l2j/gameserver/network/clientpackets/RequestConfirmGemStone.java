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
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ExPutCommissionResultForVariationMake;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.item.L2Item;
import net.sf.l2j.gameserver.util.Util;

/**
 * Format:(ch) dddd
 * @author  -Wooden-
 */
public final class RequestConfirmGemStone extends L2GameClientPacket
{
	private static final String _C__D0_2B_REQUESTCONFIRMGEMSTONE = "[C] D0:2B RequestConfirmGemStone";
	private int _targetItemObjId;
	private int _refinerItemObjId;
	private int _gemstoneItemObjId;
	private long _gemstoneCount;


	/**
	 * @param buf
	 * @param client
	 */
	@Override
	protected void readImpl()
	{
		_targetItemObjId = readD();
		_refinerItemObjId = readD();
		_gemstoneItemObjId = readD();
		_gemstoneCount= readQ();
	}

	/**
	 * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#runImpl()
	 */
	@Override
	protected
	void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		L2ItemInstance targetItem = (L2ItemInstance)L2World.getInstance().findObject(_targetItemObjId);
		L2ItemInstance refinerItem = (L2ItemInstance)L2World.getInstance().findObject(_refinerItemObjId);
		L2ItemInstance gemstoneItem = (L2ItemInstance)L2World.getInstance().findObject(_gemstoneItemObjId);

		if (targetItem == null || refinerItem == null || gemstoneItem == null) return;
		if (targetItem.getOwnerId() != activeChar.getObjectId()
			|| refinerItem.getOwnerId() != activeChar.getObjectId()
			|| gemstoneItem.getOwnerId() != activeChar.getObjectId())
		{
			Util.handleIllegalPlayerAction(getClient().getActiveChar(),"Warning!! Character "+getClient().getActiveChar().getName()+" of account "+getClient().getActiveChar().getAccountName()+" tryied to use gemstones on augment that doesn't own.",Config.DEFAULT_PUNISH);
			return;
		}
		// Make sure the item is a gemstone
		int gemstoneItemId = gemstoneItem.getItem().getItemId();
		if (gemstoneItemId != 2130 && gemstoneItemId != 2131)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.THIS_IS_NOT_A_SUITABLE_ITEM));
			return;
		}

		// Check if the gemstoneCount is sufficant
		int itemGrade = targetItem.getItem().getItemGradeSPlus();
		switch (itemGrade)
		{
			case L2Item.CRYSTAL_C:
				if (_gemstoneCount != 20 || gemstoneItemId != 2130)
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.GEMSTONE_QUANTITY_IS_INCORRECT));
					return;
				}
				break;
			case L2Item.CRYSTAL_B:
				if (_gemstoneCount != 30 || gemstoneItemId != 2130)
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.GEMSTONE_QUANTITY_IS_INCORRECT));
					return;
				}
				break;
			case L2Item.CRYSTAL_A:
				if (_gemstoneCount != 20 || gemstoneItemId != 2131)
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.GEMSTONE_QUANTITY_IS_INCORRECT));
					return;
				}
				break;
			case L2Item.CRYSTAL_S:
				if (_gemstoneCount != 25 || gemstoneItemId != 2131)
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.GEMSTONE_QUANTITY_IS_INCORRECT));
					return;
				}
				break;
		}

		activeChar.sendPacket(new ExPutCommissionResultForVariationMake(_gemstoneItemObjId, _gemstoneCount, gemstoneItemId));
		activeChar.sendPacket(new SystemMessage(SystemMessageId.PRESS_THE_AUGMENT_BUTTON_TO_BEGIN));
	}

	/**
	 * @see net.sf.l2j.gameserver.BasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _C__D0_2B_REQUESTCONFIRMGEMSTONE;
	}

}
