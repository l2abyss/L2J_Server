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
package net.sf.l2j.gameserver.network.serverpackets;

import net.sf.l2j.gameserver.model.entity.Fort;

/**
 *
 * @author  KenM
 */
public class ExShowFortressMapInfo extends L2GameServerPacket
{
    private final Fort _fortress;
    
    public ExShowFortressMapInfo(Fort fortress)
    {
        _fortress = fortress;
    }
    
    /**
     * @see net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket#getType()
     */
    @Override
    public String getType()
    {
        return "[S] FE:7D ExShowFortressMapInfo";
    }

    /**
     * @see net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket#writeImpl()
     */
    @Override
    protected void writeImpl()
    {
        writeC(0xfe);
        writeH(0x7d);
        
        writeD(_fortress.getFortId());
        writeD(0); // fortress siege status
        writeD(0); // barracks count
        
        // foreach barrack do:
        // writeD() barrack status
    }
    
}
