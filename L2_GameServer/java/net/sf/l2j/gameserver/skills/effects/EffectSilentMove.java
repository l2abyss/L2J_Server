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
package net.sf.l2j.gameserver.skills.effects;

import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2Skill.SkillType;
import net.sf.l2j.gameserver.model.actor.instance.L2PlayableInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Env;

final class EffectSilentMove extends L2Effect
{
	public EffectSilentMove(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	/** Notify started */
	@Override
	public void onStart()
	{
		super.onStart();

		L2Character effected = getEffected();
		if (effected instanceof L2PlayableInstance)
			((L2PlayableInstance)effected).setSilentMoving(true);
	}

	/** Notify exited */
	@Override
	public void onExit()
	{
		super.onExit();

		L2Character effected = getEffected();
		if (effected instanceof L2PlayableInstance)
			((L2PlayableInstance)effected).setSilentMoving(false);
	}

	@Override
	public EffectType getEffectType()
	{
		return EffectType.SILENT_MOVE;
	}

	@Override
	public boolean onActionTime()
	{
		 // Only cont skills shouldn't end
		if(getSkill().getSkillType() != SkillType.CONT)
			return false;

		if(getEffected().isDead())
			return false;

		double manaDam = calc();

		if(manaDam > getEffected().getCurrentMp())
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.SKILL_REMOVED_DUE_LACK_MP);
			getEffected().sendPacket(sm);
			return false;
		}

		getEffected().reduceCurrentMp(manaDam);
		return true;
	}

}
