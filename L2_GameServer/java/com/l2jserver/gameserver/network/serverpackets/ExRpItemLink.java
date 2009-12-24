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
package com.l2jserver.gameserver.network.serverpackets;

import com.l2jserver.gameserver.model.L2ItemInstance;

/**
 *
 * @author  KenM
 */
public final class ExRpItemLink extends L2GameServerPacket
{
    private final L2ItemInstance _item;
    
    public ExRpItemLink(L2ItemInstance item)
    {
        _item = item;
    }
    
    /**
     * @see com.l2jserver.gameserver.network.serverpackets.L2GameServerPacket#getType()
     */
    @Override
    public String getType()
    {
        return "[S] FE:6C ExRpItemLink";
    }

    /**
     * @see com.l2jserver.gameserver.network.serverpackets.L2GameServerPacket#writeImpl()
     */
    @Override
    protected void writeImpl()
    {
        writeC(0xfe);
        writeH(0x6c);
        // guessing xD
        writeD(_item.getObjectId());
        writeD(_item.getItemId());
        writeQ(_item.getCount());
        writeH(_item.getItem().getType2());
        writeD(_item.getItem().getBodyPart());
        writeH(_item.getEnchantLevel());
        writeH(_item.getCustomType2());  // item type3
        writeH(0x00); // ??
        writeD(_item.isAugmented() ? _item.getAugmentation().getAugmentationId() : 0x00);
        writeD(_item.getMana());
        // T1
        writeH(_item.getAttackElementType());
        writeH(_item.getAttackElementPower());
        for (byte i = 0; i < 6; i++)
            writeH(_item.getElementDefAttr(i));

        writeH(0x00); // Enchant effect 1
		writeH(0x00); // Enchant effect 2
		writeH(0x00); // Enchant effect 3 
    }
}
