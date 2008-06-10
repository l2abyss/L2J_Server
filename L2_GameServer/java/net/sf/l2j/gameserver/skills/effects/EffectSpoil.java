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

import net.sf.l2j.gameserver.ai.CtrlEvent;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.actor.instance.L2MonsterInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.Formulas;

/**
 * 
 * @author Ahmed
 * 
 * This is the Effect support for spoil.
 * 
 * This was originally done by _drunk_
 */
public class EffectSpoil extends L2Effect
{
	public EffectSpoil(Env env, EffectTemplate template)
	{
		super(env, template);
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.SPOIL;
	}
	
	@Override
	public void onStart()
	{
		
		if (!(getEffector() instanceof L2PcInstance))
			return;
		
		if (!(getEffected() instanceof L2MonsterInstance))
			return;
		
		L2MonsterInstance target = (L2MonsterInstance) getEffected();
		
		if (target == null)
			return;
		
		if (target.isSpoil())
		{
			getEffector().sendPacket(new SystemMessage(SystemMessageId.ALREADY_SPOILED));
			return;
		}
		
		// SPOIL SYSTEM by Lbaldi
		boolean spoil = false;
		if (target.isDead() == false)
		{
			spoil = Formulas.getInstance().calcMagicSuccess(getEffector(), target, getSkill());
			
			if (spoil)
			{
				target.setSpoil(true);
				target.setIsSpoiledBy(getEffector().getObjectId());
				getEffector().sendPacket(new SystemMessage(SystemMessageId.SPOIL_SUCCESS));
			}
			else
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.S1_WAS_UNAFFECTED_BY_S2);
				sm.addCharName(target);
				sm.addSkillName(getSkill().getDisplayId());
				getEffector().sendPacket(sm);
			}
			target.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, getEffector());
		}
		
	}
	
	@Override
	public boolean onActionTime()
	{
		return false;
	}
}
