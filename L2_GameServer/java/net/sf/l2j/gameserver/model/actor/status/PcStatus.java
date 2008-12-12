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

import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PlayableInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SummonInstance;
import net.sf.l2j.gameserver.model.entity.Duel;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.util.Util;

public class PcStatus extends PlayableStatus
{
    // =========================================================
    // Data Field

    // =========================================================
    // Constructor
    public PcStatus(L2PcInstance activeChar)
    {
        super(activeChar);
    }

    // =========================================================
    // Method - Public
    @Override
	public final void reduceHp(double value, L2Character attacker) { reduceHp(value, attacker, true, false); }
    @Override
	public final void reduceHp(double value, L2Character attacker, boolean awake, boolean isDOT)
    {
        if (getActiveChar().isInvul() && getActiveChar() != attacker)
        	return;
		if (getActiveChar().isDead())
			return;
		
		if (attacker instanceof L2PcInstance)
		{
			L2PcInstance pcInst = (L2PcInstance)attacker;
			if (pcInst.isGM() && !pcInst.getAccessLevel().canGiveDamage())
				return;
		}
		
		if (getActiveChar().isInDuel())
		{
			if ( attacker instanceof L2PcInstance)
			{
				if (getActiveChar().getDuelState() == Duel.DUELSTATE_DEAD)
					return;
				else if (getActiveChar().getDuelState() == Duel.DUELSTATE_WINNER)
					return;

				// cancel duel if player got hit by another player, that is not part of the duel
				if (((L2PcInstance)attacker).getDuelId() != getActiveChar().getDuelId())
					getActiveChar().setDuelState(Duel.DUELSTATE_INTERRUPTED);
			
			}
			else if (!(attacker instanceof L2SummonInstance))
					getActiveChar().setDuelState(Duel.DUELSTATE_INTERRUPTED);
		}
		
		int fullValue = (int) value;

        if (attacker != null && attacker != getActiveChar())
        {
            // Check and calculate transfered damage
            L2Summon summon = getActiveChar().getPet();
            //TODO correct range
            if (summon != null && summon instanceof L2SummonInstance && Util.checkIfInRange(900, getActiveChar(), summon, true))
            {
                int tDmg = (int)value * (int)getActiveChar().getStat().calcStat(Stats.TRANSFER_DAMAGE_PERCENT, 0, null, null) /100;

                // Only transfer dmg up to current HP, it should not be killed
                if (summon.getCurrentHp() < tDmg) tDmg = (int)summon.getCurrentHp() - 1;
                if (tDmg > 0)
                {
                    summon.reduceCurrentHp(tDmg, attacker);
                    value -= tDmg;
                    fullValue = (int) value; // reduce the annouced value here as player will get a message about summon dammage
                }
            }

            if (attacker instanceof L2PlayableInstance)
            {
                if (getCurrentCp() >= value)
                {
                    setCurrentCp(getCurrentCp() - value);   // Set Cp to diff of Cp vs value
                    value = 0;                              // No need to subtract anything from Hp
                }
                else
                {
                    value -= getCurrentCp();                // Get diff from value vs Cp; will apply diff to Hp
                    setCurrentCp(0);                        // Set Cp to 0
                }
            }
        }

        super.reduceHp(value, attacker, awake, isDOT);

        if (!getActiveChar().isDead() && getActiveChar().isSitting() && !isDOT)
            getActiveChar().standUp();

        if (getActiveChar().isFakeDeath() && !isDOT)
            getActiveChar().stopFakeDeath(null);

        if (attacker != null && attacker != getActiveChar() && fullValue > 0 && !isDOT)
        {
            // Send a System Message to the L2PcInstance
            SystemMessage smsg = new SystemMessage(SystemMessageId.S1_RECEIVED_DAMAGE_OF_S3_FROM_S2);
            smsg.addString(getActiveChar().getName());
            smsg.addCharName(attacker);
            smsg.addNumber(fullValue);
            getActiveChar().sendPacket(smsg);
        }
    }

    // =========================================================
    // Method - Private

    // =========================================================
    // Property - Public
    @Override
	public L2PcInstance getActiveChar() { return (L2PcInstance)super.getActiveChar(); }
}
