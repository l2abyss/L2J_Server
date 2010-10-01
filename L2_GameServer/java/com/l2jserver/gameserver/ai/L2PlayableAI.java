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
package com.l2jserver.gameserver.ai;

import com.l2jserver.gameserver.model.L2Object;
import com.l2jserver.gameserver.model.L2Skill;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.L2Character.AIAccessor;
import com.l2jserver.gameserver.model.actor.L2Playable;
import com.l2jserver.gameserver.network.SystemMessageId;

/**
 * 
 * This class manages AI of L2Playable.<BR><BR>
 *
 * L2PlayableAI :<BR><BR>
 * <li>L2SummonAI</li>
 * <li>L2PlayerAI</li>
 * <BR> <BR>
 * 
 * @author  JIV
 */
public abstract class L2PlayableAI extends L2CharacterAI
{
	
	/**
	 * @param accessor
	 */
	public L2PlayableAI(AIAccessor accessor)
	{
		super(accessor);
	}
	
	
	/**
	 * @see com.l2jserver.gameserver.ai.L2CharacterAI#onIntentionAttack(com.l2jserver.gameserver.model.actor.L2Character)
	 */
	@Override
	protected void onIntentionAttack(L2Character target)
	{
		if (target instanceof L2Playable)
		{
			if (target.getActingPlayer().getProtectionBlessing()
					&& (_actor.getActingPlayer().getLevel() - target.getActingPlayer().getLevel()) >= 10
					&& _actor.getActingPlayer().getKarma() > 0
					&& !(target.isInsideZone(L2Character.ZONE_PVP)))
			{
				// If attacker have karma and have level >= 10 than his target and target have
				// Newbie Protection Buff,
				_actor.getActingPlayer().sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
				clientActionFailed();
				return;
			}
			
			if (_actor.getActingPlayer().getProtectionBlessing()
					&& (target.getActingPlayer().getLevel() - _actor.getActingPlayer().getLevel()) >= 10
					&& target.getActingPlayer().getKarma() > 0
					&& !(target.isInsideZone(L2Character.ZONE_PVP)))
			{
				// If target have karma and have level >= 10 than his target and actor have
				// Newbie Protection Buff,
				_actor.getActingPlayer().sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
				clientActionFailed();
				return;
			}
			
			if (target.getActingPlayer().isCursedWeaponEquipped()
					&& _actor.getActingPlayer().getLevel() <= 20)
			{
				_actor.getActingPlayer().sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
				clientActionFailed();
				return;
			}
			
			if (_actor.getActingPlayer().isCursedWeaponEquipped()
					&& target.getActingPlayer().getLevel() <= 20)
			{
				_actor.getActingPlayer().sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
				clientActionFailed();
				return;
			}
		}
		
		super.onIntentionAttack(target);
	}
	
	/**
	 * @see com.l2jserver.gameserver.ai.L2CharacterAI#onIntentionCast(com.l2jserver.gameserver.model.L2Skill, com.l2jserver.gameserver.model.L2Object)
	 */
	@Override
	protected void onIntentionCast(L2Skill skill, L2Object target)
	{
		if (target instanceof L2Playable && skill.isOffensive())
		{
			if (target.getActingPlayer().getProtectionBlessing()
					&& (_actor.getActingPlayer().getLevel() - target.getActingPlayer().getLevel()) >= 10
					&& _actor.getActingPlayer().getKarma() > 0
					&& !(((L2Playable) target).isInsideZone(L2Character.ZONE_PVP)))
			{
				// If attacker have karma and have level >= 10 than his target and target have
				// Newbie Protection Buff,
				_actor.getActingPlayer().sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
				clientActionFailed();
				_actor.setIsCastingNow(false);
				return;
			}
			
			if (_actor.getActingPlayer().getProtectionBlessing()
					&& (target.getActingPlayer().getLevel() - _actor.getActingPlayer().getLevel()) >= 10
					&& target.getActingPlayer().getKarma() > 0
					&& !(((L2Playable) target).isInsideZone(L2Character.ZONE_PVP)))
			{
				// If target have karma and have level >= 10 than his target and actor have
				// Newbie Protection Buff,
				_actor.getActingPlayer().sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
				clientActionFailed();
				_actor.setIsCastingNow(false);
				return;
			}
			
			if (target.getActingPlayer().isCursedWeaponEquipped()
					&& _actor.getActingPlayer().getLevel() <= 20)
			{
				_actor.getActingPlayer().sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
				clientActionFailed();
				_actor.setIsCastingNow(false);
				return;
			}
			
			if (_actor.getActingPlayer().isCursedWeaponEquipped()
					&& target.getActingPlayer().getLevel() <= 20)
			{
				_actor.getActingPlayer().sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
				clientActionFailed();
				_actor.setIsCastingNow(false);
				return;
			}
		}
		
		super.onIntentionCast(skill, target);
	}
	
}
