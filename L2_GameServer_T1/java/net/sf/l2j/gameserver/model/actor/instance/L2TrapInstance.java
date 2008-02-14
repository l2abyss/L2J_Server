/* This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package net.sf.l2j.gameserver.model.actor.instance;

import java.util.concurrent.Future;

import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.model.L2Attackable;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Trap;
import net.sf.l2j.gameserver.model.actor.knownlist.TrapKnownList;
import net.sf.l2j.gameserver.serverpackets.SocialAction;
import net.sf.l2j.gameserver.taskmanager.DecayTaskManager;
import net.sf.l2j.gameserver.templates.L2CharTemplate;

public class L2TrapInstance extends L2Trap
{
	private int _totalLifeTime;
	private int _timeRemaining;
	private Future<?> _trapTask;
	protected L2Skill _skill;
	protected boolean _isDetected;
	protected L2Character target;
	
	/**
	 * @param objectId
	 * @param template
	 * @param owner
	 */
	public L2TrapInstance(int objectId, L2CharTemplate template,
	        L2PcInstance owner, int lifeTime, L2Skill skill)
	{
		super(objectId, template, owner);
		if (lifeTime != 0)
		{
			_totalLifeTime = lifeTime;
		}
		else
		{
			_totalLifeTime = 30000;
		}
		_timeRemaining = _totalLifeTime;
		_skill = skill;
		int delay = 1000;
		_trapTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new TrapTask(getOwner(), this, _skill), delay, delay);
	}
	
	/**
	 * 
	 * @see net.sf.l2j.gameserver.model.L2Character#doDie(net.sf.l2j.gameserver.model.L2Character)
	 */
	@Override
	public boolean doDie(L2Character killer)
	{
		if (!super.doDie(killer))
			return false;
		
		_totalLifeTime = 0;
		DecayTaskManager.getInstance().addDecayTask(this);
		return true;
	}
	
	/**
	 * 
	 * @see net.sf.l2j.gameserver.model.L2Character#getKnownList()
	 */
	@Override
	public TrapKnownList getKnownList()
	{
		if (super.getKnownList() == null || !(super.getKnownList() instanceof TrapKnownList))
			setKnownList(new TrapKnownList(this));
		return (TrapKnownList) super.getKnownList();
	}	
	
	static class TrapTask implements Runnable
	{
		private L2PcInstance _activeChar;
		private L2TrapInstance _trap;
		private L2Skill _tskill;
		
		TrapTask(L2PcInstance activeChar, L2TrapInstance trap, L2Skill skill)
		{
			_activeChar = activeChar;
			_trap = trap;
			_tskill = skill;
		}
		
		public void run()
		{
			try
			{
				double newTimeRemaining;
				_trap.decTimeRemaining(1000);
				newTimeRemaining = _trap.getTimeRemaining();
				if (newTimeRemaining < _trap.getTotalLifeTime() - 15000)
				{
					SocialAction sa = new SocialAction(_trap.getObjectId(), 2);
					_trap.broadcastPacket(sa);
				}
				if (newTimeRemaining < 0)
				{
					L2Character trg = _trap.setTarget();
					switch (_tskill.getTargetType())
					{
						case TARGET_AURA:
						case TARGET_FRONT_AURA:
						case TARGET_BEHIND_AURA:
							trg = _trap;
							break;
					}
					
					if (trg != null)
					{
						_trap.activate(trg);
					}
					else
					{
						_trap.unSummon(_activeChar);
					}
				}
			}
			catch (Throwable e)
			{
				
			}
		}
	}
	
	/**
	 * Returns the Target of this Trap
	 * @return
	 */
	protected L2Character setTarget()
	{
		for (L2Character trg : this.getKnownList().getKnownCharactersInRadius(_skill.getSkillRadius()))
		{
			if (trg == getOwner())
				continue;
			
			if ((getOwner().getParty() != null && trg.getParty() != null)
			        && getOwner().getParty().getPartyLeaderOID() == trg.getParty().getPartyLeaderOID())
				continue;
			
			if (trg.isInsideZone(L2Character.ZONE_PEACE))
				continue;
			
			return trg;
		}
		return null;
	}
	
	/**
	 * 
	 * @param trg
	 */
	public void activate(L2Character trg)
	{
		setTarget(trg);
		doCast(_skill);
		try
		{
			wait(_skill.getHitTime() + 2000);
		}
		catch (Exception e)
		{
			
		}
		unSummon(getOwner());
		
		for (L2Object atked : _skill.getTargetList(this))
		{
			if (atked == getOwner())
				continue;
			
			if (atked instanceof L2PcInstance)
				continue;
			
			else if (atked != null)
			{
				if (atked instanceof L2Attackable)
				{
					((L2Attackable)atked).addDamage(getOwner(), 1);
				}
			}
		}
	}
	
	/**
	 * 
	 * @see net.sf.l2j.gameserver.model.L2Trap#unSummon(net.sf.l2j.gameserver.model.actor.instance.L2PcInstance)
	 */
	@Override
	public void unSummon(L2PcInstance owner)
	{
		if (_trapTask != null)
		{
			_trapTask.cancel(true);
			_trapTask = null;
		}
		super.unSummon(owner);
	}
	
	/**
	 * 
	 * @param value
	 */
	public void decTimeRemaining(int value)
	{
		_timeRemaining -= value;
	}
	
	/**
	 * 
	 * @return
	 */
	public int getTimeRemaining()
	{
		return _timeRemaining;
	}
	
	/**
	 * 
	 * @return
	 */
	public int getTotalLifeTime()
	{
		return _totalLifeTime;
	}
	
	/**
	 * 
	 * @see net.sf.l2j.gameserver.model.L2Trap#setDetected()
	 */
	@Override
	public void setDetected()
	{
		_isDetected = true;
	}
	
	/**
	 * 
	 * @see net.sf.l2j.gameserver.model.L2Trap#isDetected()
	 */
	@Override
	public boolean isDetected()
	{
		return _isDetected;
	}
}
