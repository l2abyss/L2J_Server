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
package com.l2jserver.gameserver.model.actor.instance;

import java.util.concurrent.ScheduledFuture;

import com.l2jserver.gameserver.ThreadPoolManager;
import com.l2jserver.gameserver.datatables.SkillTable;
import com.l2jserver.gameserver.model.L2Party;
import com.l2jserver.gameserver.model.L2Skill;
import com.l2jserver.gameserver.model.L2World;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.templates.chars.L2NpcTemplate;

/**
 * @author Nyaran
 */
public class L2BirthdayCakeInstance extends L2Npc
{
	private static final int BIRTHDAY_CAKE_24 = 106;
	private static final int BIRTHDAY_CAKE = 139;
	private static L2Skill _skill;
	private ScheduledFuture<?> _aiTask;
	private int _masterId;
	
	public L2BirthdayCakeInstance(int objectId, L2NpcTemplate template, int masterId)
	{
		super(objectId, template);
		
		if (template.npcId == BIRTHDAY_CAKE_24)
			_skill = SkillTable.getInstance().getInfo(22035, 1);
		else if (template.npcId == BIRTHDAY_CAKE)
			_skill = SkillTable.getInstance().getInfo(22250, 1);
		
		_masterId = masterId;
		
		_aiTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new BuffTask(this), 1000, 1000);
	}
	
	class BuffTask implements Runnable
	{
		L2BirthdayCakeInstance _cake;
		
		protected BuffTask(L2BirthdayCakeInstance cake)
		{
			_cake = cake;
		}
		
		@Override
		public void run()
		{
			if (!isInsideZone(ZONE_PEACE))
			{
				if (_cake.getNpcId() == BIRTHDAY_CAKE_24)
				{
					for(L2PcInstance player : _cake.getKnownList().getKnownPlayersInRadius(_skill.getSkillRadius()))
					{
						_skill.getEffects(_cake, player);
					}
				}
				else if (_cake.getNpcId() == BIRTHDAY_CAKE)
				{
					L2PcInstance player = L2World.getInstance().getPlayer(_masterId);
					if (player == null)
						return;
					
					L2Party party = player.getParty();
					if (party == null)
					{
						if (player.isInsideRadius(_cake, _skill.getSkillRadius(), true, true))
							_skill.getEffects(_cake, player);
					}
					else
					{
						for (L2PcInstance member : party.getPartyMembers())
						{
							if (member != null && member.isInsideRadius(_cake, _skill.getSkillRadius(), true, true))
								_skill.getEffects(_cake, member);
						}
					}
				}
			}
		}
	}
	
	@Override
	public void deleteMe()
	{
		if (_aiTask != null)
			_aiTask.cancel(true);
		super.deleteMe();
	}
	
	/* (non-Javadoc)
	 * @see com.l2jserver.gameserver.model.L2Object#isAttackable()
	 */
	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		return false;
	}
}
