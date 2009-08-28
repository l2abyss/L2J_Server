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

import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.DeleteObject;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.templates.effects.EffectTemplate;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;
import net.sf.l2j.gameserver.util.Broadcast;


/**
 * 
 *
 * @author ZaKaX - nBd
 */
public class EffectHide extends L2Effect
{
	
	public EffectHide(Env env, EffectTemplate template)
	{
		super(env, template);
	}
	
	public EffectHide(Env env, L2Effect effect)
	{
		super(env, effect);
	}
	
	/**
	 * 
	 * @see net.sf.l2j.gameserver.model.L2Effect#getEffectType()
	 */
	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.HIDE;
	}
	
	/**
	 * 
	 * @see net.sf.l2j.gameserver.model.L2Effect#onStart()
	 */
	@Override
	public boolean onStart()
	{
		if (getEffected() instanceof L2PcInstance)
		{
			L2PcInstance activeChar = ((L2PcInstance) getEffected());
			activeChar.getAppearance().setInvisible();
			activeChar.broadcastUserInfo();
			activeChar.startAbnormalEffect(L2Character.ABNORMAL_EFFECT_STEALTH);
			Broadcast.toKnownPlayers(activeChar, new DeleteObject(activeChar));
		}
		return true;
	}
  
	/**
	 * 
	 * @see net.sf.l2j.gameserver.model.L2Effect#onExit()
	 */
	@Override
	public void onExit()
	{
		if (getEffected() instanceof L2PcInstance)
		{
			L2PcInstance activeChar = ((L2PcInstance) getEffected());
			activeChar.getAppearance().setVisible();
			activeChar.broadcastUserInfo();
			activeChar.stopAbnormalEffect(L2Character.ABNORMAL_EFFECT_STEALTH);
		}
	}
	
	/**
	 * 
	 * @see net.sf.l2j.gameserver.model.L2Effect#onActionTime()
	 */
	@Override
	public boolean onActionTime()
	{
		return false;
	}
}