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
package net.sf.l2j.gameserver.model;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javolution.util.FastList;
import javolution.util.FastMap;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.effects.EffectCharge;

public class CharEffectList
{
	private static final L2Effect[] EMPTY_EFFECTS = new L2Effect[0];

	private FastList<L2Effect> _buffs;
	private FastList<L2Effect> _debuffs;

	// The table containing the List of all stacked effect in progress for each Stack group Identifier
	protected Map<String, List<L2Effect>> _stackedEffects;

	// Owner of this list
	private L2Character _owner;

	public CharEffectList(L2Character owner)
	{
		_owner = owner;
	}

	/**
	 * Returns all effects affecting stored in this CharEffectList
	 * @return
	 */
	public final L2Effect[] getAllEffects()
	{
		// If no effect is active, return EMPTY_EFFECTS
		if ( (_buffs == null || _buffs.isEmpty()) && (_debuffs == null || _debuffs.isEmpty()) )
		{
			return EMPTY_EFFECTS;
		}

		// Create a copy of the effects
		FastList<L2Effect> temp = new FastList<L2Effect>();

		// Add all buffs and all debuffs
		synchronized (this)
		{
			if (_buffs != null && !_buffs.isEmpty()) 
				temp.addAll(_buffs);
			if (_debuffs != null && !_debuffs.isEmpty()) 
				temp.addAll(_debuffs);
		}

		// Return all effects in an array
		L2Effect[] tempArray = new L2Effect[temp.size()];
		temp.toArray(tempArray);
		return tempArray;
	}

	/**
	 * Returns the first ChargeEffect in this CharEffectList
	 * @return
	 */
	public final EffectCharge getChargeEffect()
	{
		L2Effect[] effects = getAllEffects();
		for (L2Effect e : effects)
		{
			if (e.getSkill().getSkillType() == L2Skill.SkillType.CHARGE)
			{
				return (EffectCharge)e;
			}
		}
		return null;
	}

	/**
	 * Returns the first effect matching the given EffectType
	 * @param tp
	 * @return
	 */
	public final L2Effect getFirstEffect(L2Effect.EffectType tp)
	{
		L2Effect[] effects = getAllEffects();

		L2Effect eventNotInUse = null;
		for (L2Effect e : effects)
		{
			if (e.getEffectType() == tp)
			{
				if (e.getInUse()) return e;
				else eventNotInUse = e;
			}
		}
		return eventNotInUse;
	}

	/**
	 * Returns the first effect matching the given L2Skill
	 * @param skill
	 * @return
	 */
	public final L2Effect getFirstEffect(L2Skill skill)
	{
		L2Effect[] effects = getAllEffects();

		L2Effect eventNotInUse = null;
		
		if (effects == null || effects.length < 1)
			return eventNotInUse;
		
		for (L2Effect e : effects)
		{
			if (e != null && e.getSkill() == skill)
			{
				if (e.getInUse()) return e;
				else eventNotInUse = e;
			}
		}
		return eventNotInUse;
	}

	/**
	 * Returns the first effect matching the given skillId
	 * @param index
	 * @return
	 */
	public final L2Effect getFirstEffect(int skillId)
	{
		L2Effect[] effects = getAllEffects();

		L2Effect eventNotInUse = null;
		
		if (effects == null || effects.length < 1)
			return eventNotInUse;
		
		for (L2Effect e : effects)
		{
			if (e!= null && e.getSkill().getId() == skillId)
			{
				if (e.getInUse()) return e;
				else eventNotInUse = e;
			}
		}
		return eventNotInUse;
	}

	/**
	 * Checks if the given skill stacks with an existing one.
	 *
	 * @param checkSkill the skill to be checked
	 *
	 * @return Returns whether or not this skill will stack
	 */
	private boolean doesStack(L2Skill checkSkill)
	{
		if ( (_buffs == null || _buffs.isEmpty()) && (_debuffs == null || _debuffs.isEmpty()) ||
				checkSkill._effectTemplates == null ||
				checkSkill._effectTemplates.length < 1 ||
				checkSkill._effectTemplates[0].stackType == null ||
				checkSkill._effectTemplates[0].stackType.equals("none") )
		{
			return false;
		}

		String stackType = checkSkill._effectTemplates[0].stackType;

		L2Effect[] effects = getAllEffects();
		for (L2Effect e : effects)
		{
			if (e.getStackType() != null && e.getStackType().equals(stackType))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Return the number of buffs in this CharEffectList
	 * @return
	 */
	public int getBuffCount()
	{
		if (_buffs == null) return 0;
		int buffCount=0;
		
		for (L2Effect e : _buffs)
		{
			if (e != null && e.getShowIcon() &&
					(e.getSkill().getSkillType() == L2Skill.SkillType.BUFF ||
					e.getSkill().getSkillType() == L2Skill.SkillType.REFLECT ||
					e.getSkill().getSkillType() == L2Skill.SkillType.HEAL_PERCENT ||
					e.getSkill().getSkillType() == L2Skill.SkillType.MANAHEAL_PERCENT) &&
					!(e.getSkill().getId() > 4360  && e.getSkill().getId() < 4367)) // Seven Signs buffs
			{
				buffCount++;
			}
		}
		return buffCount;
	}

	/**
	 * Return the number of dances in this CharEffectList
	 * @return
	 */
	public int getDanceCount()
	{
		if (_buffs == null) return 0;
		int danceCount = 0;

		for (L2Effect e : _buffs)
		{
			if (e != null && e.getSkill().isDance() && e.getInUse())
				danceCount++;
		}
		return danceCount;
	}

	/**
	 * Exits all effects in this CharEffectList
	 */
	public final void stopAllEffects()
	{
		// Get all active skills effects from this list
		L2Effect[] effects = getAllEffects();

		// Exit them
		for (L2Effect e : effects)
		{
			if (e != null)
			{
				e.exit(true);
			}
		}
 	}

	/**
	 * Exit all effects having a specified type
	 * @param type
	 */
	public final void stopEffects(L2Effect.EffectType type)
	{
		// Get all active skills effects from this list
		L2Effect[] effects = getAllEffects();

		// Go through all active skills effects
		for(L2Effect e : effects)
		{
			// Stop active skills effects of the selected type
			if (e.getEffectType() == type) e.exit();
		}
	}

	/**
	 * Exits all effects created by a specific skillId
	 * @param skillId
	 */
	public final void stopSkillEffects(int skillId)
	{
		// Get all skills effects on the L2Character
		L2Effect[] effects = getAllEffects();

		for(L2Effect e : effects)
		{
			if (e.getSkill().getId() == skillId) e.exit();
		}
	}



	/**
	 * Removes the first buff of this list.
	 *
	 * @param preferSkill If != 0 the given skill Id will be removed instead of the first
	 */
	private void removeFirstBuff(int preferSkill)
	{
		L2Effect[] effects = getAllEffects();
		L2Effect removeMe = null;

		for (L2Effect e : effects)
		{
			if ( e != null &&
					(e.getSkill().getSkillType() == L2Skill.SkillType.BUFF ||
					 e.getSkill().getSkillType() == L2Skill.SkillType.DEBUFF ||
					 e.getSkill().getSkillType() == L2Skill.SkillType.REFLECT ||
					 e.getSkill().getSkillType() == L2Skill.SkillType.HEAL_PERCENT ||
					 e.getSkill().getSkillType() == L2Skill.SkillType.MANAHEAL_PERCENT) &&
					!(e.getSkill().getId() > 4360  && e.getSkill().getId() < 4367)) // Seven Signs buff
			{
				if (preferSkill == 0) { removeMe = e; break; }
				else if (e.getSkill().getId() == preferSkill) { removeMe = e; break; }
				else if (removeMe == null) removeMe = e;
			}
		}
		if (removeMe != null) removeMe.exit();
	}



	public final void removeEffect(L2Effect effect)
	{
		if (effect == null || (_buffs == null && _debuffs == null) ) return;

		FastList<L2Effect> effectList = effect.getSkill().isDebuff() ? _debuffs : _buffs;

		synchronized(effectList)
		{

			if (effect.getStackType() == "none")
			{
				// Remove Func added by this effect from the L2Character Calculator
				_owner.removeStatsOwner(effect);
			}
			else
			{
				if(_stackedEffects == null) return;

				// Get the list of all stacked effects corresponding to the stack type of the L2Effect to add
				List<L2Effect> stackQueue = _stackedEffects.get(effect.getStackType());

				if (stackQueue == null || stackQueue.size() < 1) return;

				// Get the identifier of the first stacked effect of the stack group selected
				L2Effect frontEffect = stackQueue.get(0);

				// Remove the effect from the stack group
				boolean removed = stackQueue.remove(effect);

				if (removed)
				{
					// Check if the first stacked effect was the effect to remove
					if (frontEffect == effect)
					{
						// Remove all its Func objects from the L2Character calculator set
						_owner.removeStatsOwner(effect);

						// Check if there's another effect in the Stack Group
						if (stackQueue.size() > 0)
						{
							// Add its list of Funcs to the Calculator set of the L2Character
							for (L2Effect e : effectList)
							{
								if (e == stackQueue.get(0))
								{
									// Add its list of Funcs to the Calculator set of the L2Character
									_owner.addStatFuncs(e.getStatFuncs());
									// Set the effect to In Use
									e.setInUse(true);
									break;
								}
							}
						}
					}
					if (stackQueue.isEmpty())
						_stackedEffects.remove(effect.getStackType());
					else
						// Update the Stack Group table _stackedEffects of the L2Character
						_stackedEffects.put(effect.getStackType(), stackQueue);
				}
			}


			// Remove the active skill L2effect from _effects of the L2Character
			// The Integer key of _effects is the L2Skill Identifier that has created the effect
			for (L2Effect e : effectList)
			{
				if (e == effect)
				{
					effectList.remove(e);
					if (_owner instanceof L2PcInstance)
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.EFFECT_S1_DISAPPEARED);
						sm.addSkillName(effect);
						_owner.sendPacket(sm);
					}
					break;
				}
			}

		}
	}

	public final void addEffect(L2Effect newEffect)
	{
		if (newEffect == null) return;

		synchronized (this)
		{
			if (_buffs == null) _buffs = new FastList<L2Effect>();
			if (_debuffs == null) _debuffs = new FastList<L2Effect>();
			if (_stackedEffects == null) _stackedEffects = new FastMap<String, List<L2Effect>>();
		}

		FastList<L2Effect> effectList = newEffect.getSkill().isDebuff() ? _debuffs : _buffs;
		synchronized(effectList)
		{
			L2Effect tempEffect, tempEffect2;

			// Check for same effects
			for (L2Effect e : effectList)
			{
				if (e != null
						&& e.getSkill().getId() == newEffect.getSkill().getId()
						&& e.getEffectType() == newEffect.getEffectType()
						&& e.getStackOrder() == newEffect.getStackOrder())
				{
					if (!newEffect.getSkill().isDebuff())
						e.exit();
					else
					{
						// Started scheduled timer needs to be canceled.
						newEffect.stopEffectTask();
						return;
					}
				}
			}

			// Remove first Buff if number of buffs > getMaxBuffCount()
			L2Skill tempSkill = newEffect.getSkill();
			if (getBuffCount() >= _owner.getMaxBuffCount() && !doesStack(tempSkill) && ((
				tempSkill.getSkillType() == L2Skill.SkillType.BUFF ||
                tempSkill.getSkillType() == L2Skill.SkillType.REFLECT ||
                tempSkill.getSkillType() == L2Skill.SkillType.HEAL_PERCENT ||
                tempSkill.getSkillType() == L2Skill.SkillType.MANAHEAL_PERCENT) &&
                !tempSkill.isDebuff() &&  !(tempSkill.getId() > 4360 && tempSkill.getId() < 4367))
        	)
			{
				// if max buffs, no herb effects are used, even if they would replace one old
				if (newEffect.isHerbEffect()) 
				{ 
					newEffect.stopEffectTask(); 
					return; 
				}
				removeFirstBuff(tempSkill.getId());
			}

			// Add the L2Effect to all effect in progress on the L2Character
			if (!newEffect.getSkill().isToggle() && !newEffect.getSkill().isDebuff())
			{
				int pos=0;
				for (L2Effect e : effectList)
            	{
            		if (e != null)
            		{
            			int skillid = e.getSkill().getId();
            			if (!e.getSkill().isToggle() && (!(skillid > 4360  && skillid < 4367))) pos++;
            		}
            		else break;
            	}
				effectList.add(pos, newEffect);
			}
			else effectList.addLast(newEffect);

			// Check if a stack group is defined for this effect
			if (newEffect.getStackType().equals("none"))
			{
				// Set this L2Effect to In Use
				newEffect.setInUse(true);

				// Add Funcs of this effect to the Calculator set of the L2Character
				_owner.addStatFuncs(newEffect.getStatFuncs());

				// Update active skills in progress icons on player client
				_owner.updateEffectIcons();
				return;
			}

			// Get the list of all stacked effects corresponding to the stack type of the L2Effect to add
			List<L2Effect> stackQueue = _stackedEffects.get(newEffect.getStackType());
			L2Effect[] allEffects = getAllEffects();

			if (stackQueue == null)
				stackQueue = new FastList<L2Effect>();

			tempEffect = null;
			if (stackQueue.size() > 0)
			{
				// Get the first stacked effect of the Stack group selected
				for (L2Effect e : allEffects)
				{
					if (e == stackQueue.get(0))
					{
						tempEffect = e;
						break;
					}
				}
			}

			// Add the new effect to the stack group selected at its position
			stackQueue = effectQueueInsert(newEffect, stackQueue);

			if (stackQueue == null) return;

			// Update the Stack Group table _stackedEffects of the L2Character
			_stackedEffects.put(newEffect.getStackType(), stackQueue);

			// Get the first stacked effect of the Stack group selected
			tempEffect2 = null;
			for (L2Effect e : allEffects)
			{
				if (e == stackQueue.get(0))
				{
					tempEffect2 = e;
					break;
				}
			}

			if (tempEffect != tempEffect2)
			{
				if (tempEffect != null)
				{
					// Remove all Func objects corresponding to this stacked effect from the Calculator set of the L2Character
					_owner.removeStatsOwner(tempEffect);

					// Set the L2Effect to Not In Use
					tempEffect.setInUse(false);
				}
				if (tempEffect2 != null)
				{
					// Set this L2Effect to In Use
					tempEffect2.setInUse(true);

					// Add all Func objects corresponding to this stacked effect to the Calculator set of the L2Character
					_owner.addStatFuncs(tempEffect2.getStatFuncs());
				}
			}
		}
	}

	/**
	 * Insert an effect at the specified position in a Stack Group.<BR><BR>
	 *
	 * <B><U> Concept</U> :</B><BR><BR>
	 * Several same effect can't be used on a L2Character at the same time.
	 * Indeed, effects are not stackable and the last cast will replace the previous in progress.
	 * More, some effects belong to the same Stack Group (ex WindWalk and Haste Potion).
	 * If 2 effects of a same group are used at the same time on a L2Character, only the more efficient (identified by its priority order) will be preserve.<BR><BR>
	 *
	 * @param id The identifier of the stacked effect to add to the Stack Group
	 * @param stackOrder The position of the effect in the Stack Group
	 * @param stackQueue The Stack Group in which the effect must be added
	 *
	 */
	private List<L2Effect> effectQueueInsert(L2Effect newStackedEffect, List<L2Effect> stackQueue)
	{
		FastList<L2Effect> effectList = newStackedEffect.getSkill().isDebuff() ? _debuffs : _buffs;

		// Get the L2Effect corresponding to the effect identifier from the L2Character _effects
		if (_buffs == null && _debuffs == null) return null;

		// Create an Iterator to go through the list of stacked effects in progress on the L2Character
		Iterator<L2Effect> queueIterator = stackQueue.iterator();

		int i = 0;
		while (queueIterator.hasNext())
		{
            L2Effect cur = queueIterator.next();
            if (newStackedEffect.getStackOrder() < cur.getStackOrder())
            	i++;
            else break;
        }

		// Add the new effect to the Stack list in function of its position in the Stack group
		stackQueue.add(i, newStackedEffect);

		// skill.exit() could be used, if the users don't wish to see "effect
		// removed" always when a timer goes off, even if the buff isn't active
		// any more (has been replaced). but then check e.g. npc hold and raid petrification.
		if (Config.EFFECT_CANCELING && !newStackedEffect.isHerbEffect() && stackQueue.size() > 1)
		{
			// only keep the current effect, cancel other effects
			for (L2Effect e : effectList)
			{
				if (e == stackQueue.get(1))
				{
					effectList.remove(e);
					break;
				}
			}
			stackQueue.remove(1);
		}

		return stackQueue;
	}
}
