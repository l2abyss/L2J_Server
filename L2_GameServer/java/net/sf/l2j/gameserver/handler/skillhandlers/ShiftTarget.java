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
package net.sf.l2j.gameserver.handler.skillhandlers;

import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.model.L2Attackable;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Skill.SkillType;

public class ShiftTarget implements ISkillHandler
{
	private static final SkillType[] SKILL_IDS = { SkillType.SHIFT_TARGET };
	
	public void useSkill(L2Character activeChar, L2Skill skill,
	        L2Object[] targets)
	{
		if (targets == null)
			return;
		L2Character target = (L2Character) targets[0];
		
		if (activeChar.isAlikeDead() || target == null)
			return;
		
		for (L2Character obj : activeChar.getKnownList().getKnownCharactersInRadius(skill.getSkillRadius()))
		{
			if (obj == null || !(obj instanceof L2Attackable) || obj.isDead())
				continue;
			L2Attackable hater = ((L2Attackable) obj);
			if (hater.getHating(activeChar) == 0)
				continue;
			hater.addDamageHate(target, hater.getHating(activeChar), hater.getHating(activeChar));
			
		}
	}
	
	public SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}
