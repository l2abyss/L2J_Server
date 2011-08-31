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
package com.l2jserver.gameserver.skills.l2skills;

import com.l2jserver.gameserver.datatables.NpcTable;
import com.l2jserver.gameserver.idfactory.IdFactory;
import com.l2jserver.gameserver.model.L2Object;
import com.l2jserver.gameserver.model.L2Skill;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.model.actor.instance.L2BirthdayCakeInstance;
import com.l2jserver.gameserver.model.actor.instance.L2NpcInstance;
import com.l2jserver.gameserver.model.actor.instance.L2XmassTreeInstance;
import com.l2jserver.gameserver.templates.StatsSet;
import com.l2jserver.gameserver.templates.chars.L2NpcTemplate;
import com.l2jserver.util.Rnd;

public class L2SkillSpawn extends L2Skill
{
	private final int _npcId;
	private final int _despawnDelay;
	private final boolean _summonSpawn;
	private final boolean _randomOffset;
	
	public L2SkillSpawn(StatsSet set)
	{
		super(set);
		_npcId = set.getInteger("npcId", 0);
		_despawnDelay = set.getInteger("despawnDelay", 0);
		_summonSpawn = set.getBool("isSummonSpawn", false);
		_randomOffset = set.getBool("randomOffset", true);
	}
	
	@Override
	public void useSkill(L2Character caster, L2Object[] targets)
	{
		int x, y;
		
		if (caster.isAlikeDead())
			return;
		
		if (_npcId == 0)
		{
			_log.warning("NPC ID not defined for skill ID:"+this.getId());
			return;
		}
		
		L2Npc npc;
		final L2NpcTemplate template = NpcTable.getInstance().getTemplate(_npcId);
		if (template == null)
		{
			_log.warning("Spawn of the nonexisting NPC ID:"+_npcId+", skill ID:"+this.getId());
			return;
		}
		
		if (template.type.equalsIgnoreCase("L2XmassTree"))
			npc = new L2XmassTreeInstance(IdFactory.getInstance().getNextId(), template);
		else if (template.type.equalsIgnoreCase("L2BirthdayCake"))
			npc = new L2BirthdayCakeInstance(IdFactory.getInstance().getNextId(), template, caster.getObjectId());
		/* TODO
		else if (template.type.equalsIgnoreCase("L2WeddingCake"))
			npc = new L2WeddingCakeInstance(IdFactory.getInstance().getNextId(), template);*/
		else
			npc = new L2NpcInstance(IdFactory.getInstance().getNextId(), template);
		
		npc.setName(template.name);
		npc.setTitle(caster.getName());
		npc.setHeading(-1);
		npc.setShowSummonAnimation(_summonSpawn);
		
		if (_randomOffset)
		{
			x = caster.getX() + (Rnd.nextBoolean() ? Rnd.get(20, 50) : Rnd.get(-50, -20));
			y = caster.getY() + (Rnd.nextBoolean() ? Rnd.get(20, 50) : Rnd.get(-50, -20));
		}
		else
		{
			x = caster.getX();
			y = caster.getY();
		}

		npc.spawnMe(x, y, caster.getZ() + 20);
		if (_despawnDelay > 0)
			npc.scheduleDespawn(_despawnDelay);
	}
}