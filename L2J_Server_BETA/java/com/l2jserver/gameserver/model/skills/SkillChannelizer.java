/*
 * Copyright (C) 2004-2014 L2J Server
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
package com.l2jserver.gameserver.model.skills;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2jserver.gameserver.GeoData;
import com.l2jserver.gameserver.ThreadPoolManager;
import com.l2jserver.gameserver.datatables.SkillTable;
import com.l2jserver.gameserver.enums.ShotType;
import com.l2jserver.gameserver.model.Location;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.MagicSkillLaunched;
import com.l2jserver.gameserver.util.Util;

/**
 * Skill Channelizer implementation.
 * @author UnAfraid
 */
public class SkillChannelizer implements Runnable
{
	private static final Logger _log = Logger.getLogger(SkillChannelizer.class.getName());
	
	private final L2Character _channelizer;
	private L2Character _channelized;
	
	private L2Skill _skill;
	private volatile ScheduledFuture<?> _task = null;
	
	public SkillChannelizer(L2Character channelizer)
	{
		_channelizer = channelizer;
	}
	
	public L2Character getChannelizer()
	{
		return _channelizer;
	}
	
	public L2Character getChannelized()
	{
		return _channelized;
	}
	
	public boolean hasChannelized()
	{
		return _channelized != null;
	}
	
	public void startChanneling(L2Skill skill)
	{
		// Verify for same status.
		if (isChanneling())
		{
			_log.log(Level.WARNING, "Character: " + toString() + " is attempting to channel skill but he already does!");
			return;
		}
		
		// Start channeling.
		_skill = skill;
		_task = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(this, skill.getChannelingTickInitialDelay(), skill.getChannelingTickInterval());
	}
	
	public void stopChanneling()
	{
		// Verify for same status.
		if (!isChanneling())
		{
			_log.log(Level.WARNING, "Character: " + toString() + " is attempting to stop channel skill but he does not!");
			return;
		}
		
		// Cancel the task and unset it.
		_task.cancel(false);
		_task = null;
		
		// Cancel target channelization and unset it.
		if (_channelized != null)
		{
			_channelized.getSkillChannelized().removeChannelizer(_skill.getChannelingSkillId(), getChannelizer());
			_channelized = null;
		}
		
		// unset skill.
		_skill = null;
	}
	
	public L2Skill getSkill()
	{
		return _skill;
	}
	
	public boolean isChanneling()
	{
		return _task != null;
	}
	
	@Override
	public void run()
	{
		if (!isChanneling())
		{
			return;
		}
		
		try
		{
			
			if (_skill.getMpPerChanneling() > 0)
			{
				// Validate mana per tick.
				if (_channelizer.getCurrentMp() < _skill.getMpPerChanneling())
				{
					if (_channelizer.isPlayer())
					{
						_channelizer.sendPacket(SystemMessageId.SKILL_REMOVED_DUE_LACK_MP);
					}
					_channelizer.abortCast();
					return;
				}
				
				// Reduce mana per tick
				_channelizer.reduceCurrentMp(_skill.getMpPerChanneling());
			}
			
			// Apply channeling skills on the targets.
			if (_skill.getChannelingSkillId() > 0)
			{
				final L2Skill baseSkill = SkillTable.getInstance().getInfo(_skill.getChannelingSkillId(), 1);
				if (baseSkill == null)
				{
					_log.log(Level.WARNING, getClass().getSimpleName() + ": skill " + _skill + " couldn't find effect id skill: " + _skill.getChannelingSkillId() + " !");
					_channelizer.abortCast();
					return;
				}
				
				if (_channelized == null)
				{
					final List<L2Character> targets = getTargetList();
					if (targets.isEmpty())
					{
						_log.log(Level.WARNING, getClass().getSimpleName() + ": skill " + _skill + " couldn't find proper target!");
						_channelizer.abortCast();
						return;
					}
					
					_channelized = targets.get(0);
					_channelized.getSkillChannelized().addChannelizer(_skill.getChannelingSkillId(), getChannelizer());
				}
				
				if (!Util.checkIfInRange(_skill.getEffectRange(), _channelizer, _channelized, true))
				{
					_channelizer.abortCast();
					_channelizer.sendPacket(SystemMessageId.CANT_SEE_TARGET);
				}
				else if (!GeoData.getInstance().canSeeTarget(_channelizer, _channelized))
				{
					_channelizer.abortCast();
					_channelizer.sendPacket(SystemMessageId.CANT_SEE_TARGET);
				}
				else
				{
					final int maxSkillLevel = SkillTable.getInstance().getMaxLevel(_skill.getChannelingSkillId());
					final int skillLevel = Math.min(_channelized.getSkillChannelized().getChannerlizersSize(_skill.getChannelingSkillId()), maxSkillLevel);
					
					final BuffInfo info = _channelized.getEffectList().getBuffInfoBySkillId(_skill.getChannelingSkillId());
					if ((info == null) || (info.getSkill().getLevel() < skillLevel))
					{
						final L2Skill skill = SkillTable.getInstance().getInfo(_skill.getChannelingSkillId(), skillLevel);
						skill.applyEffects(getChannelizer(), _channelized);
					}
					_channelizer.broadcastPacket(new MagicSkillLaunched(_channelizer, _skill.getId(), _skill.getLevel(), _channelized));
				}
			}
			else
			{
				final List<L2Character> targets = getTargetList();
				final Iterator<L2Character> it = targets.iterator();
				while (it.hasNext())
				{
					final L2Character target = it.next();
					if (!GeoData.getInstance().canSeeTarget(_channelizer, target))
					{
						it.remove();
						continue;
					}
					
					if (_channelizer.isPlayable() && target.isPlayable() && _skill.isBad())
					{
						// Validate pvp conditions.
						if (_channelizer.isPlayable() && _channelizer.getActingPlayer().canAttackCharacter(target))
						{
							// Apply channeling skill effects on the target.
							_skill.applyEffects(_channelizer, target);
							// Update the pvp flag of the caster.
							_channelizer.getActingPlayer().updatePvPStatus(target);
						}
						else
						{
							it.remove();
						}
					}
					else
					{
						// Apply channeling skill effects on the target.
						_skill.applyEffects(_channelizer, target);
					}
				}
				
				// Broadcast MagicSkillLaunched on every cast.
				_channelizer.broadcastPacket(new MagicSkillLaunched(_channelizer, _skill.getId(), _skill.getLevel(), targets.toArray(new L2Character[0])));
				
				// Reduce shots.
				if (_skill.useSpiritShot())
				{
					_channelizer.setChargedShot(_channelizer.isChargedShot(ShotType.BLESSED_SPIRITSHOTS) ? ShotType.BLESSED_SPIRITSHOTS : ShotType.SPIRITSHOTS, false);
				}
				else
				{
					_channelizer.setChargedShot(ShotType.SOULSHOTS, false);
				}
				
				// Shots are re-charged every cast.
				_channelizer.rechargeShots(_skill.useSoulShot(), _skill.useSpiritShot());
			}
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Error while channelizing skill: " + _skill + " channelizer: " + _channelizer + " channelized: " + _channelized, e);
		}
	}
	
	public List<L2Character> getTargetList()
	{
		// Get possible targets
		final List<L2Character> targets = new ArrayList<>();
		switch (_skill.getTargetType())
		{
			case GROUND:
			{
				int x = _channelizer.getX();
				int y = _channelizer.getY();
				int z = _channelizer.getZ();
				
				if (_channelizer.isPlayer())
				{
					final Location wordPosition = _channelizer.getActingPlayer().getCurrentSkillWorldPosition();
					if (wordPosition != null)
					{
						x = wordPosition.getX();
						y = wordPosition.getY();
						z = wordPosition.getZ();
					}
				}
				
				for (L2Character cha : _channelizer.getKnownList().getKnownCharacters())
				{
					// Null target or caster himself is not valid target.
					if ((cha == null) || (cha == _channelizer))
					{
						continue;
					}
					
					// Target is too far.
					if (cha.calculateDistance(x, y, z, true, false) > _skill.getAffectRange())
					{
						continue;
					}
					
					// Only attackable creatures can be attacked.
					if (cha.isAttackable() || cha.isPlayable())
					{
						if (cha.isAlikeDead())
						{
							continue;
						}
						
						// Valid target, registering it.
						targets.add(cha);
					}
				}
				break;
			}
			default:
			{
				// Null target, not L2Character or caster himself is not valid target.
				if ((_channelizer.getTarget() != null) && _channelizer.getTarget().isCharacter() && (_channelizer.getTarget() != _channelizer))
				{
					targets.add((L2Character) _channelizer.getTarget());
				}
				break;
			}
		}
		return targets;
	}
}
