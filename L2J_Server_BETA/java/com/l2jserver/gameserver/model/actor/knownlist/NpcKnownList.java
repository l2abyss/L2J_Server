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
package com.l2jserver.gameserver.model.actor.knownlist;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import com.l2jserver.gameserver.ThreadPoolManager;
import com.l2jserver.gameserver.ai.CtrlIntention;
import com.l2jserver.gameserver.instancemanager.WalkingManager;
import com.l2jserver.gameserver.model.L2Object;
import com.l2jserver.gameserver.model.actor.L2Attackable;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.model.actor.instance.L2FestivalGuideInstance;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.quest.Quest;

public class NpcKnownList extends CharKnownList
{
	private ScheduledFuture<?> _trackingTask = null;
	
	public NpcKnownList(L2Npc activeChar)
	{
		super(activeChar);
	}
	
	@Override
	public boolean addKnownObject(L2Object object)
	{
		if (!super.addKnownObject(object))
		{
			return false;
		}
		
		if (getActiveObject().isNpc() && (object instanceof L2Character))
		{
			final L2Npc npc = (L2Npc) getActiveObject();
			final List<Quest> quests = npc.getTemplate().getEventQuests(Quest.QuestEventType.ON_SEE_CREATURE);
			if (quests != null)
			{
				for (Quest quest : quests)
				{
					quest.notifySeeCreature(npc, (L2Character) object, object.isSummon());
				}
			}
		}
		return true;
	}
	
	@Override
	public L2Npc getActiveChar()
	{
		return (L2Npc) super.getActiveChar();
	}
	
	@Override
	public int getDistanceToForgetObject(L2Object object)
	{
		return 2 * getDistanceToWatchObject(object);
	}
	
	@Override
	public int getDistanceToWatchObject(L2Object object)
	{
		if (!(object instanceof L2Character))
		{
			return 0;
		}
		
		if (object instanceof L2FestivalGuideInstance)
		{
			return 4000;
		}
		
		if (object.isPlayable())
		{
			return 1500;
		}
		
		return 500;
	}
	
	// Support for Walking monsters aggro
	public void startTrackingTask()
	{
		if ((_trackingTask == null) && (getActiveChar().getAggroRange() > 0))
		{
			_trackingTask = ThreadPoolManager.getInstance().scheduleAiAtFixedRate(new TrackingTask(), 2000, 2000);
		}
	}
	
	// Support for Walking monsters aggro
	public void stopTrackingTask()
	{
		if (_trackingTask != null)
		{
			_trackingTask.cancel(true);
			_trackingTask = null;
		}
	}
	
	// Support for Walking monsters aggro
	protected class TrackingTask implements Runnable
	{
		@Override
		public void run()
		{
			if (getActiveChar() instanceof L2Attackable)
			{
				final L2Attackable monster = (L2Attackable) getActiveChar();
				if (monster.getAI().getIntention() == CtrlIntention.AI_INTENTION_MOVE_TO)
				{
					final Collection<L2PcInstance> players = getKnownPlayers().values();
					if (players != null)
					{
						for (L2PcInstance pl : players)
						{
							if (!pl.isDead() && !pl.isInvul() && pl.isInsideRadius(monster, monster.getAggroRange(), true, false))
							{
								// Send aggroRangeEnter
								if (monster.getHating(pl) == 0)
								{
									monster.addDamageHate(pl, 0, 0);
								}
								
								// Skip attack for other targets, if one is already chosen for attack
								if ((monster.getAI().getIntention() != CtrlIntention.AI_INTENTION_ATTACK) && !monster.isCoreAIDisabled())
								{
									WalkingManager.getInstance().stopMoving(getActiveChar(), false, true);
									monster.addDamageHate(pl, 0, 100);
									monster.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, pl, null);
								}
							}
						}
					}
				}
			}
		}
	}
}
