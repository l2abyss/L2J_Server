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

import static com.l2jserver.gameserver.ai.CtrlIntention.AI_INTENTION_ATTACK;
import static com.l2jserver.gameserver.ai.CtrlIntention.AI_INTENTION_FOLLOW;
import static com.l2jserver.gameserver.ai.CtrlIntention.AI_INTENTION_IDLE;

import com.l2jserver.gameserver.model.actor.L2Summon;
import com.l2jserver.gameserver.model.actor.L2Character.AIAccessor;
import com.l2jserver.gameserver.model.actor.instance.L2MerchantSummonInstance;

public class L2SummonAI extends L2PlayableAI
{
	
	private boolean _thinking; // to prevent recursive thinking
	private boolean _startFollow = ((L2Summon) _actor).getFollowStatus();
	
	public L2SummonAI(AIAccessor accessor)
	{
		super(accessor);
	}
	
	@Override
	protected void onIntentionIdle()
	{
		if (_actor instanceof L2MerchantSummonInstance)
			return;
		stopFollow();
		_startFollow = false;
		onIntentionActive();
	}
	
	@Override
	protected void onIntentionActive()
	{
		if (_actor instanceof L2MerchantSummonInstance)
			return;
		L2Summon summon = (L2Summon) _actor;
		if (_startFollow)
			setIntention(AI_INTENTION_FOLLOW, summon.getOwner());
		else
			super.onIntentionActive();
	}
	
	private void thinkAttack()
	{
		if (_actor instanceof L2MerchantSummonInstance)
			return;
		if (checkTargetLostOrDead(getAttackTarget()))
		{
			setAttackTarget(null);
			return;
		}
		if (maybeMoveToPawn(getAttackTarget(), _actor.getPhysicalAttackRange()))
			return;
		clientStopMoving(null);
		_accessor.doAttack(getAttackTarget());
	}
	
	private void thinkCast()
	{
		if (_actor instanceof L2MerchantSummonInstance)
			return;
		L2Summon summon = (L2Summon) _actor;
		if (checkTargetLost(getCastTarget()))
		{
			setCastTarget(null);
			return;
		}
		boolean val = _startFollow;
		if (maybeMoveToPawn(getCastTarget(), _actor.getMagicalAttackRange(_skill)))
			return;
		clientStopMoving(null);
		summon.setFollowStatus(false);
		setIntention(AI_INTENTION_IDLE);
		_startFollow = val;
		_accessor.doCast(_skill);
	}
	
	private void thinkPickUp()
	{
		if (_actor instanceof L2MerchantSummonInstance)
			return;
		if (checkTargetLost(getTarget()))
			return;
		if (maybeMoveToPawn(getTarget(), 36))
			return;
		setIntention(AI_INTENTION_IDLE);
		((L2Summon.AIAccessor) _accessor).doPickupItem(getTarget());
	}
	
	private void thinkInteract()
	{
		if (_actor instanceof L2MerchantSummonInstance)
			return;
		if (checkTargetLost(getTarget()))
			return;
		if (maybeMoveToPawn(getTarget(), 36))
			return;
		setIntention(AI_INTENTION_IDLE);
	}
	
	@Override
	protected void onEvtThink()
	{
		if (_actor instanceof L2MerchantSummonInstance)
			return;
		if (_thinking || _actor.isCastingNow() || _actor.isAllSkillsDisabled())
			return;
		_thinking = true;
		try
		{
			switch (getIntention())
			{
				case AI_INTENTION_ATTACK:
					thinkAttack();
					break;
				case AI_INTENTION_CAST:
					thinkCast();
					break;
				case AI_INTENTION_PICK_UP:
					thinkPickUp();
					break;
				case AI_INTENTION_INTERACT:
					thinkInteract();
					break;
			}
		}
		finally
		{
			_thinking = false;
		}
	}
	
	@Override
	protected void onEvtFinishCasting()
	{
		if (_actor instanceof L2MerchantSummonInstance)
			return;
		if (_actor.getAI().getIntention() != AI_INTENTION_ATTACK)
			((L2Summon) _actor).setFollowStatus(_startFollow);
	}
	
	public void notifyFollowStatusChange()
	{
		if (_actor instanceof L2MerchantSummonInstance)
			return;
		_startFollow = !_startFollow;
		switch (getIntention())
		{
			case AI_INTENTION_ACTIVE:
			case AI_INTENTION_FOLLOW:
			case AI_INTENTION_IDLE:
			case AI_INTENTION_MOVE_TO: 
			case AI_INTENTION_PICK_UP: 
				((L2Summon) _actor).setFollowStatus(_startFollow);
		}
	}
	
	public void setStartFollowController(boolean val)
	{
		if (_actor instanceof L2MerchantSummonInstance)
			return;
		_startFollow = val;
	}
}
