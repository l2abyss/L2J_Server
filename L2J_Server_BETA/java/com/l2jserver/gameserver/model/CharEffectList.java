/*
 * Copyright (C) 2004-2013 L2J Server
 * 
 * This file is part of L2J Server.
 * 
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.l2jserver.gameserver.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

import com.l2jserver.Config;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.L2Summon;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.effects.EffectFlag;
import com.l2jserver.gameserver.model.effects.L2Effect;
import com.l2jserver.gameserver.model.effects.L2EffectType;
import com.l2jserver.gameserver.model.olympiad.OlympiadGameManager;
import com.l2jserver.gameserver.model.olympiad.OlympiadGameTask;
import com.l2jserver.gameserver.model.skills.AbnormalType;
import com.l2jserver.gameserver.model.skills.L2Skill;
import com.l2jserver.gameserver.model.skills.L2SkillType;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.AbnormalStatusUpdate;
import com.l2jserver.gameserver.network.serverpackets.ExOlympiadSpelledInfo;
import com.l2jserver.gameserver.network.serverpackets.PartySpelled;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;

/**
 * Effect lists.
 * @author Zoey76
 */
public final class CharEffectList
{
	private static final Logger _log = Logger.getLogger(CharEffectList.class.getName());
	/** List containing all effect buffs for this effect list. */
	private List<L2Effect> _buffs;
	/** List containing all effect debuffs for this effect list. */
	private List<L2Effect> _debuffs;
	/** They bypass most of the actions, they are not included in {@link #getAllEffects()}. */
	private List<L2Effect> _passives;
	/** Map containing the all stacked effect in progress for each abnormal type. */
	private Map<AbnormalType, L2Effect[]> _stackedEffects;
	/** Set containing all abnormal types that shouldn't be added to this character effect list. */
	private volatile Set<AbnormalType> _blockedBuffSlots = null;
	/** If {@code true} this effect list has buffs removed on any action. */
	private volatile boolean _hasBuffsRemovedOnAnyAction = false;
	/** If {@code true} this effect list has buffs removed on damage. */
	private volatile boolean _hasBuffsRemovedOnDamage = false;
	/** If {@code true} this effect list has debuffs removed on damage. */
	private volatile boolean _hasDebuffsRemovedOnDamage = false;
	/** Effect flags. */
	private int _effectFlags;
	/** If {@code true} only party icons need to be updated. */
	private boolean _partyOnly = false;
	/** The owner of this effect list. */
	private final L2Character _owner;
	/** Reentrant read write lock. */
	private final ReentrantReadWriteLock _reentrantRWLock = new ReentrantReadWriteLock();
	/** Read lock. */
	private final Lock _rLock = _reentrantRWLock.readLock();
	/** Write lock. */
	private final Lock _wLock = _reentrantRWLock.writeLock();
	
	/**
	 * Constructor for effect list.
	 * @param owner the character that owns this effect list
	 */
	public CharEffectList(L2Character owner)
	{
		_owner = owner;
	}
	
	/**
	 * Get all effects in this effect list.
	 * @return all effects for this effect list
	 */
	public final List<L2Effect> getAllEffects()
	{
		_rLock.lock();
		try
		{
			if (hasBuffs())
			{
				if (hasDebuffs())
				{
					// TODO: Verify if there is an efficient way to do it.
					final List<L2Effect> effects = new CopyOnWriteArrayList<>();
					effects.addAll(_buffs);
					effects.addAll(_debuffs);
					return effects;
				}
				return _buffs;
			}
			else if (hasDebuffs())
			{
				return _debuffs;
			}
			return Collections.<L2Effect> emptyList();
		}
		finally
		{
			_rLock.unlock();
		}
	}
	
	/**
	 * Get passive effects.
	 * @return the passive effects.
	 */
	public final List<L2Effect> getPassiveEffects()
	{
		return hasPassiveEffects() ? _passives : Collections.<L2Effect> emptyList();
	}
	
	/**
	 * Get the first effect for the given effect type.
	 * @param type the effect type
	 * @return the first effect matching the given effect type
	 */
	public final L2Effect getFirstEffect(L2EffectType type)
	{
		L2Effect effectNotInUse = null;
		if (hasBuffs())
		{
			for (L2Effect e : _buffs)
			{
				if (e == null)
				{
					continue;
				}
				
				if (e.getEffectType() == type)
				{
					if (e.isInUse())
					{
						return e;
					}
					
					effectNotInUse = e;
				}
			}
		}
		if ((effectNotInUse == null) && hasDebuffs())
		{
			for (L2Effect e : _debuffs)
			{
				if (e == null)
				{
					continue;
				}
				
				if (e.getEffectType() == type)
				{
					if (e.isInUse())
					{
						return e;
					}
					
					effectNotInUse = e;
				}
			}
		}
		return effectNotInUse;
	}
	
	/**
	 * Get the first effect for the given skill.
	 * @param skill the skill
	 * @return the first effect matching the given skill
	 */
	public final L2Effect getFirstEffect(L2Skill skill)
	{
		L2Effect effectNotInUse = null;
		if (skill.isDebuff())
		{
			if (hasDebuffs())
			{
				for (L2Effect e : _debuffs)
				{
					if ((e != null) && (e.getSkill() == skill))
					{
						if (e.isInUse())
						{
							return e;
						}
						effectNotInUse = e;
					}
				}
			}
		}
		else
		{
			if (hasBuffs())
			{
				for (L2Effect e : _buffs)
				{
					if ((e != null) && (e.getSkill() == skill))
					{
						if (e.isInUse())
						{
							return e;
						}
						effectNotInUse = e;
					}
				}
			}
		}
		return effectNotInUse;
	}
	
	/**
	 * @param skillId the skill Id
	 * @return the first effect matching the given skill Id
	 */
	public final L2Effect getFirstEffect(int skillId)
	{
		L2Effect effectNotInUse = null;
		if (hasBuffs())
		{
			for (L2Effect e : _buffs)
			{
				if ((e != null) && (e.getSkill().getId() == skillId))
				{
					if (e.isInUse())
					{
						return e;
					}
					effectNotInUse = e;
				}
			}
		}
		
		if ((effectNotInUse == null) && hasDebuffs())
		{
			for (L2Effect e : _debuffs)
			{
				if ((e != null) && (e.getSkill().getId() == skillId))
				{
					if (e.isInUse())
					{
						return e;
					}
					effectNotInUse = e;
				}
			}
		}
		return effectNotInUse;
	}
	
	/**
	 * Get the first passive effect for the given effect type.
	 * @param type the effect type
	 * @return the first passive effect for the given effect type
	 */
	public final L2Effect getFirstPassiveEffect(L2EffectType type)
	{
		if (hasPassiveEffects())
		{
			for (L2Effect e : _passives)
			{
				if ((e != null) && (e.getEffectType() == type) && e.isInUse())
				{
					return e;
				}
			}
		}
		return null;
	}
	
	/**
	 * Add abnormal types to the blocked buff slot set.
	 * @param blockedBuffSlots the blocked buff slot set to add
	 */
	public final void addBlockedBuffSlots(Set<AbnormalType> blockedBuffSlots)
	{
		// Using Double-Checked Locking to avoid synchronization overhead.
		if (_blockedBuffSlots == null)
		{
			synchronized (this)
			{
				if (_blockedBuffSlots == null)
				{
					_blockedBuffSlots = new CopyOnWriteArraySet<>();
				}
			}
		}
		_blockedBuffSlots.addAll(blockedBuffSlots);
	}
	
	/**
	 * Remove abnormal types from the blocked buff slot set.
	 * @param blockedBuffSlots the blocked buff slot set to remove
	 */
	public final void removeBlockedBuffSlots(Set<AbnormalType> blockedBuffSlots)
	{
		if (_blockedBuffSlots != null)
		{
			_blockedBuffSlots.removeAll(blockedBuffSlots);
		}
	}
	
	/**
	 * Get all the blocked abnormal types for this character effect list.
	 * @return the current blocked buff slots set
	 */
	public final Set<AbnormalType> getAllBlockedBuffSlots()
	{
		return _blockedBuffSlots;
	}
	
	/**
	 * Checks if the given skill stacks with an existing one.
	 * @param type the abnormal type to be checked
	 * @return {@code true} if this effect stacks with the given abnormal type, {@code false} otherwise
	 */
	private boolean doesStack(AbnormalType type)
	{
		if (type.isNone())
		{
			return false;
		}
		
		for (L2Effect e : _buffs)
		{
			if (e.getSkill().getAbnormalType() == type)
			{
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Get the buffs count.
	 * @return the number of buffs in this character effect list not counting Songs/Dances
	 */
	public int getBuffCount()
	{
		int buffCount = 0;
		if (hasBuffs())
		{
			for (L2Effect e : _buffs)
			{
				if ((e != null) && e.isIconDisplay() && !e.getSkill().isDance() && !e.getSkill().isTriggeredSkill() && !e.getSkill().is7Signs())
				{
					if (e.getSkill().getSkillType() == L2SkillType.BUFF)
					{
						buffCount++;
					}
				}
			}
		}
		return buffCount;
	}
	
	/**
	 * Get the Songs/Dances count.
	 * @return the number of Songs/Dances in this character effect list
	 */
	public int getDanceCount()
	{
		int danceCount = 0;
		if (hasBuffs())
		{
			for (L2Effect e : _buffs)
			{
				if ((e != null) && e.getSkill().isDance() && e.isInUse())
				{
					danceCount++;
				}
			}
		}
		return danceCount;
	}
	
	/**
	 * Get the triggered buffs count.
	 * @return the number of Activation Buffs in this character effect list
	 */
	public int getTriggeredBuffCount()
	{
		int activationBuffCount = 0;
		if (hasBuffs())
		{
			for (L2Effect e : _buffs)
			{
				if ((e != null) && e.getSkill().isTriggeredSkill() && e.isInUse())
				{
					activationBuffCount++;
				}
			}
		}
		return activationBuffCount;
	}
	
	/**
	 * Exits all effects in this CharEffectList
	 */
	public final void stopAllEffects()
	{
		for (L2Effect e : getAllEffects())
		{
			if (e != null)
			{
				e.exit(true); // Exit them
			}
		}
	}
	
	/**
	 * Exits all effects in this CharEffectList
	 */
	public final void stopAllEffectsExceptThoseThatLastThroughDeath()
	{
		for (L2Effect e : getAllEffects())
		{
			if ((e != null) && !e.getSkill().isStayAfterDeath())
			{
				e.exit(true); // Exit them
			}
		}
	}
	
	/**
	 * Exit all toggle-type effects
	 */
	public void stopAllToggles()
	{
		if (hasBuffs())
		{
			for (L2Effect e : _buffs)
			{
				if ((e != null) && e.getSkill().isToggle())
				{
					e.exit();
				}
			}
		}
	}
	
	/**
	 * Exit all effects having a specified type
	 * @param type the type of the effect to stop
	 */
	public final void stopEffects(L2EffectType type)
	{
		if (hasBuffs())
		{
			for (L2Effect e : _buffs)
			{
				// Get active skills effects of the selected type
				if ((e != null) && (e.getEffectType() == type))
				{
					e.exit();
				}
			}
		}
		
		if (hasDebuffs())
		{
			for (L2Effect e : _debuffs)
			{
				// Get active skills effects of the selected type
				if ((e != null) && (e.getEffectType() == type))
				{
					e.exit();
				}
			}
		}
	}
	
	/**
	 * Exits all effects created by a specific skill Id.
	 * @param skillId the skill Id
	 */
	public final void stopSkillEffects(int skillId)
	{
		if (hasBuffs())
		{
			for (L2Effect e : _buffs)
			{
				if ((e != null) && (e.getSkill().getId() == skillId))
				{
					e.exit();
				}
			}
		}
		if (hasDebuffs())
		{
			for (L2Effect e : _debuffs)
			{
				if ((e != null) && (e.getSkill().getId() == skillId))
				{
					e.exit();
				}
			}
		}
	}
	
	/**
	 * Exits all buffs effects of the skills with "removedOnAnyAction" set.<br>
	 * Called on any action except movement (attack, cast).
	 */
	public void stopEffectsOnAction()
	{
		if (_hasBuffsRemovedOnAnyAction && hasBuffs())
		{
			for (L2Effect e : _buffs)
			{
				if ((e != null) && e.getSkill().isRemovedOnAnyActionExceptMove())
				{
					e.exit(true);
				}
			}
		}
	}
	
	/**
	 * @param awake
	 */
	public void stopEffectsOnDamage(boolean awake)
	{
		if (_hasBuffsRemovedOnDamage && hasBuffs())
		{
			for (L2Effect e : _buffs)
			{
				if ((e != null) && e.getSkill().isRemovedOnDamage() && (awake || (e.getSkill().getSkillType() != L2SkillType.SLEEP)))
				{
					e.exit(true);
				}
			}
		}
		if (_hasDebuffsRemovedOnDamage && hasDebuffs())
		{
			for (L2Effect e : _debuffs)
			{
				if ((e != null) && e.getSkill().isRemovedOnDamage() && (awake || (e.getSkill().getSkillType() != L2SkillType.SLEEP)))
				{
					e.exit(true);
				}
			}
		}
	}
	
	/**
	 * @param partyOnly
	 */
	public void updateEffectIcons(boolean partyOnly)
	{
		if (!isEmpty())
		{
			if (partyOnly)
			{
				_partyOnly = true;
			}
			
			updateEffectIcons();
			computeEffectFlags();
		}
	}
	
	/**
	 * Verify if this effect list is empty.
	 * @return {@code true} if both {@link #_buffs} and {@link #_debuffs} are either {@code null} or empty
	 */
	public boolean isEmpty()
	{
		return ((_buffs == null) || _buffs.isEmpty()) && ((_debuffs == null) || _debuffs.isEmpty());
	}
	
	/**
	 * Verify if this effect list has buffs effects.
	 * @return {@code true} if {@link #_buffs} is not {@code null} and is not empty
	 */
	public boolean hasBuffs()
	{
		return (_buffs != null) && !_buffs.isEmpty();
	}
	
	/**
	 * Verify if this effect list has debuffs effects.
	 * @return {@code true} if {@link #_debuffs} is not {@code null} and is not empty
	 */
	public boolean hasDebuffs()
	{
		return (_debuffs != null) && !_debuffs.isEmpty();
	}
	
	/**
	 * Verify if this effect list has passive effects.
	 * @return {@code true} if {@link #_passives} is not {@code null} and is not empty
	 */
	public boolean hasPassiveEffects()
	{
		return (_passives != null) && !_passives.isEmpty();
	}
	
	/**
	 * Remove a set of effects from this effect list.
	 * @param effects the effect list to remove
	 */
	public void remove(L2Effect... effects)
	{
		if ((effects == null) || (effects.length <= 0))
		{
			return;
		}
		
		final L2Skill skill = effects[0].getSkill(); // Get skill from first effect
		if (skill.isPassive())
		{
			for (L2Effect effect : effects)
			{
				if (effect.setInUse(false))
				{
					_owner.removeStatsOwner(effect);
					if (_passives != null)
					{
						_passives.remove(effect);
					}
				}
			}
			return;
		}
		
		final List<L2Effect> effectList = skill.isDebuff() ? _debuffs : _buffs;
		if (effectList == null)
		{
			return;
		}
		
		// Remove the effect from character effects.
		for (L2Effect effect : effects)
		{
			if (effect.setInUse(false))
			{
				_owner.removeStatsOwner(effect);
			}
			
			effectList.remove(effect);
			
			if (_owner.isPlayer() && effect.isIconDisplay())
			{
				final SystemMessage sm = SystemMessage.getSystemMessage(skill.isToggle() ? SystemMessageId.S1_HAS_BEEN_ABORTED : SystemMessageId.EFFECT_S1_DISAPPEARED);
				sm.addSkillName(effect);
				_owner.sendPacket(sm);
			}
		}
		// Update effect flags.
		computeEffectFlags();
		updateEffectIcons();
	}
	
	/**
	 * Add a set of effects to this effect list.
	 * @param effects the effect list to add
	 */
	public void add(L2Effect... effects)
	{
		if ((effects == null) || (effects.length <= 0))
		{
			return;
		}
		
		// Support for blocked buff slots.
		for (L2Effect effect : effects)
		{
			if ((_blockedBuffSlots != null) && _blockedBuffSlots.contains(effect.getSkill().getAbnormalType()))
			{
				return;
			}
		}
		
		final L2Skill skill = effects[0].getSkill(); // Get skill from first effect
		// Passive effects are treated specially
		if (skill.isPassive())
		{
			// Passive effects don't need stack type
			if (!skill.getAbnormalType().isNone())
			{
				_log.warning("Passive " + this + " with abnormal type: " + skill.getAbnormalType() + "!");
			}
			
			_wLock.lock();
			try
			{
				if (_passives == null)
				{
					_passives = new CopyOnWriteArrayList<>();
				}
				
				for (L2Effect effect : effects)
				{
					// Set this effect to In Use
					if (effect.setInUse(true))
					{
						for (L2Effect eff : _passives)
						{
							if (eff == null)
							{
								continue;
							}
							
							// Check and remove if there is already such effect in order to prevent passive effects overstack.
							if (eff.getEffectTemplate().equals(effect.getEffectTemplate()))
							{
								eff.exit();
							}
						}
						
						// Add Funcs of this effect to the Calculator set of the L2Character
						_owner.addStatFuncs(effect.getStatFuncs());
						_passives.add(effect);
					}
				}
			}
			finally
			{
				_wLock.unlock();
			}
			return;
		}
		
		_wLock.lock();
		try
		{
			if (skill.isDebuff())
			{
				if (_debuffs == null)
				{
					_debuffs = new CopyOnWriteArrayList<>();
				}
				
				for (L2Effect effect : effects)
				{
					for (L2Effect e : _debuffs)
					{
						if ((e != null) && (e.getSkill().getId() == skill.getId()) && (e.getEffectType() == effect.getEffectType()) && (e.getSkill().getAbnormalLvl() == skill.getAbnormalLvl()) && (e.getSkill().getAbnormalType() == skill.getAbnormalType()))
						{
							// Started scheduled timer needs to be canceled.
							effect.stopEffectTask();
						}
					}
					_debuffs.add(effect);
				}
			}
			else
			{
				if (_buffs == null)
				{
					_buffs = new CopyOnWriteArrayList<>();
				}
				
				for (L2Effect effect : effects)
				{
					for (L2Effect e : _buffs)
					{
						if ((e != null) && (e.getSkill().getId() == skill.getId()) && (e.getEffectType() == effect.getEffectType()) && (e.getSkill().getAbnormalLvl() <= skill.getAbnormalLvl()) && (e.getSkill().getAbnormalType() == skill.getAbnormalType()))
						{
							e.exit(); // exit this
						}
					}
				}
				// Remove first buff when buff list is full
				if (!doesStack(skill.getAbnormalType()) && !skill.is7Signs())
				{
					int effectsToRemove;
					if (skill.isDance())
					{
						effectsToRemove = getDanceCount() - Config.DANCES_MAX_AMOUNT;
						if (effectsToRemove >= 0)
						{
							for (L2Effect e : _buffs)
							{
								if ((e == null) || !e.getSkill().isDance())
								{
									continue;
								}
								
								// get first dance
								e.exit();
								effectsToRemove--;
								if (effectsToRemove < 0)
								{
									break;
								}
							}
						}
					}
					else if (skill.isTriggeredSkill())
					{
						effectsToRemove = getTriggeredBuffCount() - Config.TRIGGERED_BUFFS_MAX_AMOUNT;
						if (effectsToRemove >= 0)
						{
							for (L2Effect e : _buffs)
							{
								if ((e == null) || !e.getSkill().isTriggeredSkill())
								{
									continue;
								}
								
								// get first dance
								e.exit();
								effectsToRemove--;
								if (effectsToRemove < 0)
								{
									break;
								}
							}
						}
					}
					else
					{
						effectsToRemove = getBuffCount() - _owner.getMaxBuffCount();
						if ((effectsToRemove >= 0) && (skill.getSkillType() == L2SkillType.BUFF))
						{
							for (L2Effect e : _buffs)
							{
								if ((e == null) || e.getSkill().isDance() || e.getSkill().isTriggeredSkill() || (e.getSkill().getSkillType() != L2SkillType.BUFF))
								{
									continue;
								}
								
								e.exit();
								effectsToRemove--;
								
								if (effectsToRemove < 0)
								{
									break;
								}
							}
						}
					}
				}
				
				for (L2Effect effect : effects)
				{
					// Icons order: buffs, 7s, toggles, dances, activation buffs
					if (skill.isTriggeredSkill())
					{
						_buffs.add(effect);
					}
					else
					{
						int pos = 0;
						if (skill.isToggle())
						{
							// toggle skill - before all dances
							for (L2Effect e : _buffs)
							{
								if (e == null)
								{
									continue;
								}
								if (e.getSkill().isDance())
								{
									break;
								}
								pos++;
							}
						}
						else if (skill.isDance())
						{
							// dance skill - before all activation buffs
							for (L2Effect e : _buffs)
							{
								if (e == null)
								{
									continue;
								}
								if (e.getSkill().isTriggeredSkill())
								{
									break;
								}
								pos++;
							}
						}
						else
						{
							// normal buff - before toggles and 7s and dances
							for (L2Effect e : _buffs)
							{
								if (e == null)
								{
									continue;
								}
								if (e.getSkill().isToggle() || e.getSkill().is7Signs() || e.getSkill().isDance() || e.getSkill().isTriggeredSkill())
								{
									break;
								}
								pos++;
							}
						}
						_buffs.add(pos, effect);
					}
					
					// Check if a stack group is defined for this effect
					if (skill.getAbnormalType().isNone())
					{
						// Set this L2Effect to In Use
						if (effect.setInUse(true))
						{
							// Add Funcs of this effect to the Calculator set of the L2Character
							_owner.addStatFuncs(effect.getStatFuncs());
						}
					}
				}
			}
			
			// Effects without abnormal shouldn't stack.
			if (skill.getAbnormalType().isNone())
			{
				return;
			}
			
			if (_stackedEffects == null)
			{
				_stackedEffects = new ConcurrentHashMap<>();
			}
			
			// Get the list of all stacked effects corresponding to the abnormal type of the skill to add.
			final L2Effect[] effectsToRemove = _stackedEffects.put(skill.getAbnormalType(), effects);
			if ((effectsToRemove != null) && (effectsToRemove.length > 0))
			{
				// Set the effects to not in use and remove stats.
				for (L2Effect effectToRemove : effectsToRemove)
				{
					_owner.removeStatsOwner(effectToRemove);
					
					if (Config.EFFECT_CANCELING && !skill.isStatic())
					{
						if (skill.isDebuff())
						{
							_debuffs.remove(effectToRemove);
						}
						else
						{
							_buffs.remove(effectToRemove);
						}
					}
					effectToRemove.setInUse(false);
				}
			}
			
			// Add stats and set in use.
			for (L2Effect effectToAdd : effects)
			{
				if (effectToAdd.setInUse(true))
				{
					_owner.addStatFuncs(effectToAdd.getStatFuncs());
				}
			}
		}
		finally
		{
			_wLock.unlock();
		}
		// Update effect flags and icons.
		computeEffectFlags();
		updateEffectIcons();
	}
	
	/**
	 * Remove all passive effects held by this <b>skillId</b>.
	 * @param skillId the skill Id
	 */
	public void removePassiveEffects(int skillId)
	{
		if (hasPassiveEffects())
		{
			for (L2Effect eff : _passives)
			{
				if ((eff != null) && (eff.getSkill().getId() == skillId))
				{
					eff.exit();
				}
			}
		}
	}
	
	/**
	 * Update effect icons.
	 */
	private void updateEffectIcons()
	{
		if (_owner == null)
		{
			return;
		}
		
		if (!_owner.isPlayable())
		{
			updateEffectFlags();
			return;
		}
		
		AbnormalStatusUpdate mi = null;
		PartySpelled ps = null;
		PartySpelled psSummon = null;
		ExOlympiadSpelledInfo os = null;
		boolean isSummon = false;
		
		if (_owner.isPlayer())
		{
			if (_partyOnly)
			{
				_partyOnly = false;
			}
			else
			{
				mi = new AbnormalStatusUpdate();
			}
			
			if (_owner.isInParty())
			{
				ps = new PartySpelled(_owner);
			}
			
			if (_owner.getActingPlayer().isInOlympiadMode() && _owner.getActingPlayer().isOlympiadStart())
			{
				os = new ExOlympiadSpelledInfo(_owner.getActingPlayer());
			}
		}
		else if (_owner.isSummon())
		{
			isSummon = true;
			ps = new PartySpelled(_owner);
			psSummon = new PartySpelled(_owner);
		}
		
		boolean foundRemovedOnAction = false;
		boolean foundRemovedOnDamage = false;
		
		if (hasBuffs())
		{
			for (L2Effect e : _buffs)
			{
				if (e == null)
				{
					continue;
				}
				
				if (e.getSkill().isRemovedOnAnyActionExceptMove())
				{
					foundRemovedOnAction = true;
				}
				if (e.getSkill().isRemovedOnDamage())
				{
					foundRemovedOnDamage = true;
				}
				
				if (!e.isIconDisplay())
				{
					continue;
				}
				
				if (e.getEffectType() == L2EffectType.SIGNET_GROUND)
				{
					continue;
				}
				
				if (e.isInUse())
				{
					if (mi != null)
					{
						e.addIcon(mi);
					}
					
					if (ps != null)
					{
						if (isSummon || (!e.getSkill().isToggle() && !(e.getSkill().isStatic() && ((e.getEffectType() == L2EffectType.HEAL_OVER_TIME) || (e.getEffectType() == L2EffectType.CPHEAL_OVER_TIME) || (e.getEffectType() == L2EffectType.MANA_HEAL_OVER_TIME)))))
						{
							e.addPartySpelledIcon(ps);
						}
					}
					
					if (psSummon != null)
					{
						if (!e.getSkill().isToggle() && !(e.getSkill().isStatic() && ((e.getEffectType() == L2EffectType.HEAL_OVER_TIME) || (e.getEffectType() == L2EffectType.CPHEAL_OVER_TIME) || (e.getEffectType() == L2EffectType.MANA_HEAL_OVER_TIME))))
						{
							e.addPartySpelledIcon(psSummon);
						}
					}
					
					if (os != null)
					{
						e.addOlympiadSpelledIcon(os);
					}
				}
			}
			
		}
		
		_hasBuffsRemovedOnAnyAction = foundRemovedOnAction;
		_hasBuffsRemovedOnDamage = foundRemovedOnDamage;
		foundRemovedOnDamage = false;
		
		if (hasDebuffs())
		{
			for (L2Effect e : _debuffs)
			{
				if (e == null)
				{
					continue;
				}
				
				if (e.getSkill().isRemovedOnAnyActionExceptMove())
				{
					foundRemovedOnAction = true;
				}
				if (e.getSkill().isRemovedOnDamage())
				{
					foundRemovedOnDamage = true;
				}
				
				if (!e.isIconDisplay())
				{
					continue;
				}
				
				switch (e.getEffectType())
				{
					case SIGNET_GROUND:
						continue;
				}
				
				if (e.isInUse())
				{
					if (mi != null)
					{
						e.addIcon(mi);
					}
					
					if (ps != null)
					{
						e.addPartySpelledIcon(ps);
					}
					
					if (psSummon != null)
					{
						e.addPartySpelledIcon(psSummon);
					}
					
					if (os != null)
					{
						e.addOlympiadSpelledIcon(os);
					}
				}
			}
			
		}
		
		_hasDebuffsRemovedOnDamage = foundRemovedOnDamage;
		
		if (mi != null)
		{
			_owner.sendPacket(mi);
		}
		
		if (ps != null)
		{
			if (_owner.isSummon())
			{
				L2PcInstance summonOwner = ((L2Summon) _owner).getOwner();
				
				if (summonOwner != null)
				{
					if (summonOwner.isInParty())
					{
						summonOwner.getParty().broadcastToPartyMembers(summonOwner, psSummon); // send to all member except summonOwner
						summonOwner.sendPacket(ps); // now send to summonOwner
					}
					else
					{
						summonOwner.sendPacket(ps);
					}
				}
			}
			else if (_owner.isPlayer() && _owner.isInParty())
			{
				_owner.getParty().broadcastPacket(ps);
			}
		}
		
		if (os != null)
		{
			final OlympiadGameTask game = OlympiadGameManager.getInstance().getOlympiadTask(_owner.getActingPlayer().getOlympiadGameId());
			if ((game != null) && game.isBattleStarted())
			{
				game.getZone().broadcastPacketToObservers(os);
			}
		}
	}
	
	/**
	 * Update effect flags.
	 */
	private void updateEffectFlags()
	{
		boolean foundRemovedOnAction = false;
		boolean foundRemovedOnDamage = false;
		
		if (hasBuffs())
		{
			for (L2Effect e : _buffs)
			{
				if (e == null)
				{
					continue;
				}
				
				if (e.getSkill().isRemovedOnAnyActionExceptMove())
				{
					foundRemovedOnAction = true;
				}
				if (e.getSkill().isRemovedOnDamage())
				{
					foundRemovedOnDamage = true;
				}
			}
		}
		_hasBuffsRemovedOnAnyAction = foundRemovedOnAction;
		_hasBuffsRemovedOnDamage = foundRemovedOnDamage;
		foundRemovedOnDamage = false;
		
		if (hasDebuffs())
		{
			for (L2Effect e : _debuffs)
			{
				if (e == null)
				{
					continue;
				}
				
				if (e.getSkill().isRemovedOnDamage())
				{
					foundRemovedOnDamage = true;
				}
			}
		}
		_hasDebuffsRemovedOnDamage = foundRemovedOnDamage;
	}
	
	/**
	 * Recalculate effect bits flag.<br>
	 * Please no concurrency access.
	 */
	private final void computeEffectFlags()
	{
		_rLock.lock();
		try
		{
			int flags = 0;
			if (hasBuffs())
			{
				for (L2Effect e : _buffs)
				{
					if (e == null)
					{
						continue;
					}
					flags |= e.getEffectFlags();
				}
			}
			
			if (hasDebuffs())
			{
				for (L2Effect e : _debuffs)
				{
					if (e == null)
					{
						continue;
					}
					flags |= e.getEffectFlags();
				}
			}
			_effectFlags = flags;
		}
		finally
		{
			_rLock.unlock();
		}
	}
	
	/**
	 * Check if target is affected with special buff
	 * @param flag of special buff
	 * @return boolean true if affected
	 */
	public boolean isAffected(EffectFlag flag)
	{
		return (_effectFlags & flag.getMask()) != 0;
	}
}
