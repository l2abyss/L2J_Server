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
package net.sf.l2j.gameserver.model.actor.status;

import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Playable;

public class PlayableStatus extends CharStatus
{
    // =========================================================
    // Data Field

    // =========================================================
    // Constructor
    public PlayableStatus(L2Playable activeChar)
    {
        super(activeChar);
    }

    // =========================================================
    // Method - Public
    @Override
	public void reduceHp(double value, L2Character attacker) { reduceHp(value, attacker, true, false); }
    @Override
	public void reduceHp(double value, L2Character attacker, boolean awake, boolean isDOT)
    {
        if (getActiveChar().isDead()) return;

        super.reduceHp(value, attacker, awake, isDOT);
        /*
        if (attacker != null && attacker != getActiveChar())
        {
            // Flag the attacker if it's a L2PcInstance outside a PvP area
            L2PcInstance player = null;
            if (attacker instanceof L2PcInstance)
                player = (L2PcInstance)attacker;
            else if (attacker instanceof L2Summon)
                player = ((L2Summon)attacker).getOwner();

            if (player != null) player.updatePvPStatus(getActiveChar());
        }
        */
    }

    // =========================================================
    // Method - Private

    // =========================================================
    // Property - Public
    @Override
	public L2Playable getActiveChar() { return (L2Playable)super.getActiveChar(); }
}
