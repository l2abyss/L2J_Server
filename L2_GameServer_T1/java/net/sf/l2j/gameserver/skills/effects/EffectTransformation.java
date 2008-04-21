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

import net.sf.l2j.gameserver.instancemanager.TransformationManager;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.skills.Env;

/**
*
* @author nBd
*/
public class EffectTransformation extends L2Effect
{
    public EffectTransformation(Env env, EffectTemplate template)
    {
        super(env, template);
    }
    
    @Override
    public EffectType getEffectType()
    {
        return L2Effect.EffectType.TRANSFORMATION;
    }
    
    @Override
    public void onStart()
    {
        if (getEffected().isAlikeDead())
            return;
        
        if (!(getEffected() instanceof L2PcInstance))
            return;
        
        L2PcInstance trg = (L2PcInstance) getEffected();
        if (trg == null)
            return;
        
        if (trg.isAlikeDead() || trg.isCursedWeaponEquipped())
            return;
        
        int transformId = getSkill().getTransformId();
        
        if (!trg.isTransformed())
        {
            TransformationManager.getInstance().transformPlayer(transformId, trg);
        }
    }
    
    @Override
    public boolean onActionTime()
    {
       return true;
    }
}
