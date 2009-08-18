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
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SummonInstance;
import net.sf.l2j.gameserver.model.entity.Duel;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.util.Util;

public class PcStatus extends PlayableStatus
{
	public PcStatus(L2PcInstance activeChar)
	{
		super(activeChar);
	}

	@Override
	public final void reduceHp(double value, L2Character attacker) { reduceHp(value, attacker, true, false, false); }

	@Override
	public final void reduceHp(double value, L2Character attacker, boolean awake, boolean isDOT, boolean isHpConsumption)
	{
		if (getActiveChar().isInvul())
		{
			if (attacker == getActiveChar())
			{
				if (!isDOT && !isHpConsumption)
					return;
			}
			else
				return;
		}

		if (getActiveChar().isDead())
			return;

		int fullValue = (int) value;
		int tDmg = 0;

		if (attacker != null && attacker != getActiveChar())
		{
			final L2PcInstance attackerPlayer = attacker.getActingPlayer();

			if (attackerPlayer != null)
			{
				if (attackerPlayer.isGM() && !attackerPlayer.getAccessLevel().canGiveDamage())
					return;

				if (getActiveChar().isInDuel())
				{
					if (getActiveChar().getDuelState() == Duel.DUELSTATE_DEAD)
						return;
					else if (getActiveChar().getDuelState() == Duel.DUELSTATE_WINNER)
						return;

					// cancel duel if player got hit by another player, that is not part of the duel
					if (attackerPlayer.getDuelId() != getActiveChar().getDuelId())
						getActiveChar().setDuelState(Duel.DUELSTATE_INTERRUPTED);
				}
			}

			// Check and calculate transfered damage
			final L2Summon summon = getActiveChar().getPet();
			//TODO correct range
			if (summon != null && summon instanceof L2SummonInstance && Util.checkIfInRange(900, getActiveChar(), summon, true))
			{
				tDmg = (int)value * (int)getActiveChar().getStat().calcStat(Stats.TRANSFER_DAMAGE_PERCENT, 0, null, null) /100;

				// Only transfer dmg up to current HP, it should not be killed
				tDmg = Math.min((int)summon.getCurrentHp() - 1, tDmg);
				if (tDmg > 0)
				{
					summon.reduceCurrentHp(tDmg, attacker, null);
					value -= tDmg;
					fullValue = (int) value; // reduce the announced value here as player will get a message about summon damage
				}
			}

			if (attacker instanceof L2Playable)
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

			if (fullValue > 0 && !isDOT)
			{
				SystemMessage smsg;
				// Send a System Message to the L2PcInstance
				smsg = new SystemMessage(SystemMessageId.C1_RECEIVED_DAMAGE_OF_S3_FROM_C2);
				smsg.addString(getActiveChar().getName());
				smsg.addCharName(attacker);
				smsg.addNumber(fullValue);
				getActiveChar().sendPacket(smsg);

				if (tDmg > 0)
				{
					smsg = new SystemMessage(SystemMessageId.C1_RECEIVED_DAMAGE_OF_S3_FROM_C2);
					smsg.addString(getActiveChar().getPet().getName());
					smsg.addCharName(attacker);
					smsg.addNumber(tDmg);
					getActiveChar().sendPacket(smsg);

					if (attackerPlayer != null)
					{
						smsg = new SystemMessage(SystemMessageId.GIVEN_S1_DAMAGE_TO_YOUR_TARGET_AND_S2_DAMAGE_TO_SERVITOR);
						smsg.addNumber(fullValue);
						smsg.addNumber(tDmg);
						attackerPlayer.sendPacket(smsg);
					}
				}
			}
		}

		if (!getActiveChar().isDead() && getActiveChar().isSitting() && !isDOT)
			getActiveChar().standUp();

		if (getActiveChar().isFakeDeath() && !isDOT)
			getActiveChar().stopFakeDeath(null);

		super.reduceHp(value, attacker, awake, isDOT, isHpConsumption);
	}

	@Override
	public L2PcInstance getActiveChar()
	{
		return (L2PcInstance)super.getActiveChar();
	}
}
