package net.sf.l2j.gameserver.skills.conditions;

import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.skills.Env;

/**
 * 
 * @author  DrHouse
 */
public class ConditionPlayerActiveSkillId extends Condition
{
	private final int _skillId;
    
    public ConditionPlayerActiveSkillId(int skillId)
    {
        _skillId = skillId;
    }
    
    @Override
    public boolean testImpl(Env env)
    {
        for (L2Skill sk : env.player.getAllSkills())
        {
            if (sk != null)
            {
                if (sk.getId() == _skillId)
                    return true;
            }
        }
        return false;
    }
}