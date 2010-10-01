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
package com.l2jserver.gameserver.model.actor;

import com.l2jserver.gameserver.ai.CtrlEvent;
import com.l2jserver.gameserver.model.L2Effect;
import com.l2jserver.gameserver.model.L2Skill;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.actor.knownlist.PlayableKnownList;
import com.l2jserver.gameserver.model.actor.stat.PlayableStat;
import com.l2jserver.gameserver.model.actor.status.PlayableStatus;
import com.l2jserver.gameserver.model.quest.QuestState;
import com.l2jserver.gameserver.templates.chars.L2CharTemplate;
import com.l2jserver.gameserver.templates.skills.L2EffectType;

/**
 * This class represents all Playable characters in the world.<BR><BR>
 *
 * L2PlayableInstance :<BR><BR>
 * <li>L2PcInstance</li>
 * <li>L2Summon</li><BR><BR>
 *
 */

public abstract class L2Playable extends L2Character
{
	
	private boolean _isNoblesseBlessed = false; 	// for Noblesse Blessing skill, restores buffs after death
	private boolean _getCharmOfLuck = false; 		// Charm of Luck - During a Raid/Boss war, decreased chance for death penalty
	private boolean _isPhoenixBlessed = false; 		// for Soul of The Phoenix or Salvation buffs
	private boolean _isSilentMoving = false;		// Silent Move
	private boolean _ProtectionBlessing = false;
	
	private L2Character _lockedTarget = null;
	
	/**
	 * Constructor of L2PlayableInstance (use L2Character constructor).<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Call the L2Character constructor to create an empty _skills slot and link copy basic Calculator set to this L2PlayableInstance </li><BR><BR>
	 *
	 * @param objectId Identifier of the object to initialized
	 * @param template The L2CharTemplate to apply to the L2PlayableInstance
	 *
	 */
	public L2Playable(int objectId, L2CharTemplate template)
	{
		super(objectId, template);
		setInstanceType(InstanceType.L2Playable);
		setIsInvul(false);
	}
	
	@Override
	public PlayableKnownList getKnownList()
	{
		return (PlayableKnownList)super.getKnownList();
	}
	
	@Override
	public void initKnownList()
	{
		setKnownList(new PlayableKnownList(this));
	}
	
	@Override
	public PlayableStat getStat()
	{
		return (PlayableStat)super.getStat();
	}
	
	@Override
	public void initCharStat()
	{
		setStat(new PlayableStat(this));
	}
	
	@Override
	public PlayableStatus getStatus()
	{
		return (PlayableStatus)super.getStatus();
	}
	
	@Override
	public void initCharStatus()
	{
		setStatus(new PlayableStatus(this));
	}
	
	@Override
	public boolean doDie(L2Character killer)
	{
		// killing is only possible one time
		synchronized (this)
		{
			if (isDead())
				return false;
			// now reset currentHp to zero
			setCurrentHp(0);
			setIsDead(true);
		}
		
		// Set target to null and cancel Attack or Cast
		setTarget(null);
		
		// Stop movement
		stopMove(null);
		
		// Stop HP/MP/CP Regeneration task
		getStatus().stopHpMpRegeneration();
		
		// Stop all active skills effects in progress on the L2Character,
		// if the Character isn't affected by Soul of The Phoenix or Salvation
		if (isPhoenixBlessed())
		{
			if (getCharmOfLuck()) //remove Lucky Charm if player has SoulOfThePhoenix/Salvation buff
				stopCharmOfLuck(null);
			if (isNoblesseBlessed())
				stopNoblesseBlessing(null);
		}
		// Same thing if the Character isn't a Noblesse Blessed L2PlayableInstance
		else if (isNoblesseBlessed())
		{
			stopNoblesseBlessing(null);
			
			if (getCharmOfLuck()) //remove Lucky Charm if player have Nobless blessing buff
				stopCharmOfLuck(null);
		}
		else
			stopAllEffectsExceptThoseThatLastThroughDeath();
		
		// Send the Server->Client packet StatusUpdate with current HP and MP to all other L2PcInstance to inform
		broadcastStatusUpdate();
		
		if (getWorldRegion() != null)
			getWorldRegion().onDeath(this);
		
		// Notify Quest of L2Playable's death
		L2PcInstance actingPlayer = getActingPlayer();
		for (QuestState qs : actingPlayer.getNotifyQuestOfDeath())
		{
			qs.getQuest().notifyDeath((killer == null ? this : killer), this, qs);
		}
		
		if (killer != null)
		{
			L2PcInstance player = killer.getActingPlayer();
			
			if (player != null)
				player.onKillUpdatePvPKarma(this);
		}
		
		// Notify L2Character AI
		getAI().notifyEvent(CtrlEvent.EVT_DEAD);
		
		return true;
	}
	
	public boolean checkIfPvP(L2Character target)
	{
		if (target == null) return false;                                               // Target is null
		if (target == this) return false;                                               // Target is self
		if (!(target instanceof L2Playable)) return false;                      // Target is not a L2PlayableInstance
		
		L2PcInstance player = null;
		if (this instanceof L2PcInstance)
			player = (L2PcInstance)this;
		else if (this instanceof L2Summon)
			player = ((L2Summon)this).getOwner();
		
		if (player == null) return false;                                               // Active player is null
		if (player.getKarma() != 0) return false;                                       // Active player has karma
		
		L2PcInstance targetPlayer = null;
		if (target instanceof L2PcInstance)
			targetPlayer = (L2PcInstance)target;
		else if (target instanceof L2Summon)
			targetPlayer = ((L2Summon)target).getOwner();
		
		if (targetPlayer == null) return false;                                         // Target player is null
		if (targetPlayer == this) return false;                                         // Target player is self
		if (targetPlayer.getKarma() != 0) return false;                                 // Target player has karma
		if (targetPlayer.getPvpFlag() == 0) return false;
		
		return true;
		/*  Even at war, there should be PvP flag
        if(
                player.getClan() == null ||
                targetPlayer.getClan() == null ||
                (
                        !targetPlayer.getClan().isAtWarWith(player.getClanId()) &&
                        targetPlayer.getWantsPeace() == 0 &&
                        player.getWantsPeace() == 0
                )
            )
        {
            return true;
        }

        return false;
		 */
	}
	
	/**
	 * Return True.<BR><BR>
	 */
	@Override
	public boolean isAttackable()
	{
		return true;
	}
	
	// Support for Noblesse Blessing skill, where buffs are retained
	// after resurrect
	public final boolean isNoblesseBlessed() { return _isNoblesseBlessed; }
	public final void setIsNoblesseBlessed(boolean value) { _isNoblesseBlessed = value; }
	
	public final void startNoblesseBlessing()
	{
		setIsNoblesseBlessed(true);
		updateAbnormalEffect();
	}
	
	public final void stopNoblesseBlessing(L2Effect effect)
	{
		if (effect == null)
			stopEffects(L2EffectType.NOBLESSE_BLESSING);
		else
			removeEffect(effect);
		
		setIsNoblesseBlessed(false);
		updateAbnormalEffect();
	}
	
	// Support for Soul of the Phoenix and Salvation skills
	public final boolean isPhoenixBlessed() { return _isPhoenixBlessed; }
	public final void setIsPhoenixBlessed(boolean value) { _isPhoenixBlessed = value; }
	
	public final void startPhoenixBlessing()
	{
		setIsPhoenixBlessed(true);
		updateAbnormalEffect();
	}
	
	public final void stopPhoenixBlessing(L2Effect effect)
	{
		if (effect == null)
			stopEffects(L2EffectType.PHOENIX_BLESSING);
		else
			removeEffect(effect);
		
		setIsPhoenixBlessed(false);
		updateAbnormalEffect();
	}
	
	/**
	 * Set the Silent Moving mode Flag.<BR><BR>
	 */
	public void setSilentMoving(boolean flag)
	{
		_isSilentMoving = flag;
	}
	
	/**
	 * Return True if the Silent Moving mode is active.<BR><BR>
	 */
	public boolean isSilentMoving()
	{
		return _isSilentMoving;
	}
	
	// for Newbie Protection Blessing skill, keeps you safe from an attack by a chaotic character >= 10 levels apart from you
	public final boolean getProtectionBlessing() { return _ProtectionBlessing; }
	public final void setProtectionBlessing(boolean value) { _ProtectionBlessing = value; }
	public void startProtectionBlessing()
	{
		setProtectionBlessing(true);
		updateAbnormalEffect();
	}
	/**
	 * @param blessing
	 */
	public void stopProtectionBlessing(L2Effect effect)
	{
		if (effect == null)
			stopEffects(L2EffectType.PROTECTION_BLESSING);
		else
			removeEffect(effect);
		
		setProtectionBlessing(false);
		updateAbnormalEffect();
	}
	
	//Charm of Luck - During a Raid/Boss war, decreased chance for death penalty
	public final boolean getCharmOfLuck() { return _getCharmOfLuck; }
	public final void setCharmOfLuck(boolean value) { _getCharmOfLuck = value; }
	
	public final void startCharmOfLuck()
	{
		setCharmOfLuck(true);
		updateAbnormalEffect();
	}
	
	public final void stopCharmOfLuck(L2Effect effect)
	{
		if (effect == null)
			stopEffects(L2EffectType.CHARM_OF_LUCK);
		else
			removeEffect(effect);
		
		setCharmOfLuck(false);
		updateAbnormalEffect();
	}
	
	@Override
	public void updateEffectIcons(boolean partyOnly)
	{
		_effects.updateEffectIcons(partyOnly);
	}
	
	public boolean isLockedTarget()
	{
		return _lockedTarget != null;
	}
	
	public L2Character getLockedTarget()
	{
		return _lockedTarget;
	}
	
	public void setLockedTarget(L2Character cha)
	{
		_lockedTarget = cha;
	}
	
	public abstract int getKarma();
	
	public abstract byte getPvpFlag();
	
	public abstract void useMagic(L2Skill skill, boolean forceUse, boolean dontMove);
}
