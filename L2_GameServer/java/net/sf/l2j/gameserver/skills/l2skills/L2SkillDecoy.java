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
package net.sf.l2j.gameserver.skills.l2skills;

import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2DecoyInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;
import net.sf.l2j.gameserver.templates.StatsSet;

public class L2SkillDecoy extends L2Skill
{
    
    private int _npcId;
    
    public L2SkillDecoy(StatsSet set)
    {
        super(set);
        _npcId = set.getInteger("npcId", 0);
    }
    
    @Override
    public void useSkill(L2Character caster, L2Object[] targets)
    {
        if (caster.isAlikeDead() || !(caster instanceof L2PcInstance))
            return;
        
        if (_npcId == 0)
            return;
        
        L2PcInstance activeChar = (L2PcInstance) caster;
        
        if (activeChar.inObserverMode())
            return;
        
        if (activeChar.getPet() != null || activeChar.isMounted())
            return;
        
        L2DecoyInstance Decoy;
        L2NpcTemplate DecoyTemplate = NpcTable.getInstance().getTemplate(_npcId);
        Decoy = new L2DecoyInstance(IdFactory.getInstance().getNextId(), DecoyTemplate, activeChar, this);
        Decoy.setCurrentHp(Decoy.getMaxHp());
        Decoy.setCurrentMp(Decoy.getMaxMp());
        Decoy.setHeading(activeChar.getHeading());
        activeChar.setDecoy(Decoy);
        L2World.getInstance().storeObject(Decoy);
        Decoy.spawnMe(activeChar.getX(), activeChar.getY(), activeChar.getZ());
    }
}