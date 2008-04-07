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
package net.sf.l2j.gameserver.serverpackets;

import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.instancemanager.FortManager;
import net.sf.l2j.gameserver.model.L2Attackable;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2SiegeClan;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.Castle;
import net.sf.l2j.gameserver.model.entity.Fort;

/**
 * sample
 * 0b
 * 952a1048     objectId
 * 00000000 00000000 00000000 00000000 00000000 00000000

 * format  dddddd   rev 377
 * format  ddddddd   rev 417
 *
 * @version $Revision: 1.3.2.1.2.5 $ $Date: 2005/03/27 18:46:18 $
 */
public class Die extends L2GameServerPacket
{
    private static final String _S__0B_DIE = "[S] 00 Die";
    private int _charObjId;
    private boolean _fallDown;
    private boolean _sweepable;
    private int _access;
    private net.sf.l2j.gameserver.model.L2Clan _clan;
    private static final int REQUIRED_LEVEL = net.sf.l2j.Config.GM_FIXED;
    L2Character _activeChar;

    /**
     * @param _characters
     */
    public Die(L2Character cha)
    {
    	_activeChar = cha;
        if (cha instanceof L2PcInstance) {
            L2PcInstance player = (L2PcInstance)cha;
            _access = player.getAccessLevel();
            _clan=player.getClan();

        }
        _charObjId = cha.getObjectId();
        _fallDown = cha instanceof L2PcInstance ? ((L2PcInstance)cha).mustFallDownOnDeath() : cha.mustFallDownOnDeath();
        if (cha instanceof L2Attackable)
            _sweepable = ((L2Attackable)cha).isSweepActive();

    }

    @Override
	protected final void writeImpl()
    {
        if (!_fallDown)
            return;

        writeC(0x00);

        writeD(_charObjId);
        // NOTE:
        // 6d 00 00 00 00 - to nearest village
        // 6d 01 00 00 00 - to hide away
        // 6d 02 00 00 00 - to castle
        // 6d 03 00 00 00 - to siege HQ
        // sweepable
        // 6d 04 00 00 00 - FIXED

        writeD(0x01);                                                   // 6d 00 00 00 00 - to nearest village
        if (_clan != null)
        {
            L2SiegeClan siegeClan = null;
            Boolean isInDefense = false;
            Castle castle = CastleManager.getInstance().getCastle(_activeChar);
			Fort fort = FortManager.getInstance().getFort(_activeChar);
			if (castle != null && castle.getSiege().getIsInProgress())
            {
            	//siege in progress
                siegeClan = castle.getSiege().getAttackerClan(_clan);
                if (siegeClan == null && castle.getSiege().checkIsDefender(_clan)){
                	isInDefense = true;
                }
            }
            else if (fort != null && fort.getSiege().getIsInProgress())
            {
                //siege in progress
                siegeClan = fort.getSiege().getAttackerClan(_clan);
                if (siegeClan == null && fort.getSiege().checkIsDefender(_clan)){
                    isInDefense = true;
                }
            }

            writeD(_clan.getHasHideout() > 0 ? 0x01 : 0x00);            // 6d 01 00 00 00 - to hide away
            writeD((_clan.getHasCastle() > 0 || _clan.getHasFort() > 0)  ||
            	   isInDefense? 0x01 : 0x00);             				// 6d 02 00 00 00 - to castle
            writeD(siegeClan != null &&
            	   !isInDefense &&
                   siegeClan.getFlag().size() > 0 ? 0x01 : 0x00);       // 6d 03 00 00 00 - to siege HQ
        }
        else
        {
            writeD(0x00);                                               // 6d 01 00 00 00 - to hide away
            writeD(0x00);                                               // 6d 02 00 00 00 - to castle
            writeD(0x00);                                               // 6d 03 00 00 00 - to siege HQ
        }

        writeD(_sweepable ? 0x01 : 0x00);                               // sweepable  (blue glow)
        writeD(_access >= REQUIRED_LEVEL? 0x01: 0x00);                  // 6d 04 00 00 00 - to FIXED
    }

    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.serverpackets.ServerBasePacket#getType()
     */
    @Override
	public String getType()
    {
        return _S__0B_DIE;
    }
}
