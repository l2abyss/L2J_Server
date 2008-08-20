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
import net.sf.l2j.gameserver.network.serverpackets.ExPutIntensiveResultForVariationMake;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.L2Item;

/**
 * Fromat(ch) dd
 * @author  -Wooden-
 */
public class RequestConfirmRefinerItem extends L2GameClientPacket
{
	private static final String _C__D0_2A_REQUESTCONFIRMREFINERITEM = "[C] D0:2A RequestConfirmRefinerItem";

	private static final int GEMSTONE_D = 2130;
	private static final int GEMSTONE_C = 2131;

	private int _targetItemObjId;
	private int _refinerItemObjId;

	/**
	 * @param buf
	 * @param client
	 */
	@Override
	protected void readImpl()
	{
		_targetItemObjId = readD();
		_refinerItemObjId = readD();
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

		if (targetItem == null || refinerItem == null) return;

		int itemGrade = targetItem.getItem().getItemGrade();
		int refinerItemId = refinerItem.getItem().getItemId();

		// is the item a life stone?
		if (refinerItemId < 8723 || (refinerItemId > 8762 && refinerItemId < 9573) || refinerItemId > 9576)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.THIS_IS_NOT_A_SUITABLE_ITEM));
			return;
		}
		// check if target is common item
		if (targetItem.isCommonItem())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.THIS_IS_NOT_A_SUITABLE_ITEM));
			return;
		}

		int gemstoneCount=0;
		int gemstoneItemId=0;
		int lifeStoneLevel = getLifeStoneLevel(refinerItemId);
		SystemMessage sm = new SystemMessage(SystemMessageId.REQUIRES_S1_S2);
		switch (itemGrade)
		{
			case L2Item.CRYSTAL_C:
				gemstoneCount = 20;
				gemstoneItemId = GEMSTONE_D;
				sm.addNumber(gemstoneCount);
				sm.addString("Gemstone D");
				break;
			case L2Item.CRYSTAL_B:
				if (lifeStoneLevel < 3)
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.THIS_IS_NOT_A_SUITABLE_ITEM));
					return;
				}
				gemstoneCount = 30;
				gemstoneItemId = GEMSTONE_D;
				sm.addNumber(gemstoneCount);
				sm.addString("Gemstone D");
				break;
			case L2Item.CRYSTAL_A:
				if (lifeStoneLevel < 6)
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.THIS_IS_NOT_A_SUITABLE_ITEM));
					return;
				}
				gemstoneCount = 20;
				gemstoneItemId = GEMSTONE_C;
				sm.addNumber(gemstoneCount);
				sm.addString("Gemstone C");
				break;
			case L2Item.CRYSTAL_S:
			case L2Item.CRYSTAL_S80:
				if (lifeStoneLevel != 10)
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.THIS_IS_NOT_A_SUITABLE_ITEM));
					return;
				}
				gemstoneCount = 25;
				gemstoneItemId = GEMSTONE_C;
				sm.addNumber(gemstoneCount);
				sm.addString("Gemstone C");
				break;
		}

		activeChar.sendPacket(new ExPutIntensiveResultForVariationMake(_refinerItemObjId, refinerItemId, gemstoneItemId, gemstoneCount));

		activeChar.sendPacket(sm);
	}

	private int getLifeStoneGrade(int itemId)
	{
		itemId -= 8723;
        if (itemId < 10 || itemId == 850) return 0; // normal grade
        if (itemId < 20 || itemId == 851) return 1; // mid grade
        if (itemId < 30 || itemId == 852) return 2; // high grade
		return 3; // top grade
	}

	private int getLifeStoneLevel(int itemId)
	{
		itemId -= 10 * getLifeStoneGrade(itemId);
		itemId -= 8722;
		if (itemId > 823) return 10;
		return itemId;
	}

	/**
	 * @see net.sf.l2j.gameserver.BasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _C__D0_2A_REQUESTCONFIRMREFINERITEM;
	}

}
