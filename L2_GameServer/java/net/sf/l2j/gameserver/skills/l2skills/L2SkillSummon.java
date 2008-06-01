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

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2CubicInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SiegeSummonInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SummonInstance;
import net.sf.l2j.gameserver.model.base.Experience;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;
import net.sf.l2j.gameserver.templates.StatsSet;

public class L2SkillSummon extends L2Skill
{

	private int     _npcId;
	private float   _expPenalty;

	public L2SkillSummon(StatsSet set)
	{
		super(set);

		_npcId      = set.getInteger("npcId", 0); // default for undescribed skills
		_expPenalty = set.getFloat ("expPenalty", 0.f);
	}

	public boolean checkCondition(L2Character activeChar)
	{
		if (activeChar instanceof L2PcInstance)
		{
			L2PcInstance player = (L2PcInstance)activeChar;
			if (isCubic())
			{
				if (getTargetType() != L2Skill.SkillTargetType.TARGET_SELF)
				{
					return true; //Player is always able to cast mass cubic skill
				}
				int mastery = player.getSkillLevel(L2Skill.SKILL_CUBIC_MASTERY);
				if (mastery < 0)
					mastery = 0;
				int count = player.getCubics().size();
				if (count > mastery)
				{
					activeChar.sendMessage("You already have "+count+" cubic(s).");
					return false;
				}
			}
			else
			{
				if (player.inObserverMode())
					return false;
				if (player.getPet() != null)
				{
					activeChar.sendMessage("You already have a pet.");
					return false;
				}
			}
		}
		return super.checkCondition(activeChar, null, false);
	}

	@Override
	public void useSkill(L2Character caster, L2Object[] targets) {
		if (caster.isAlikeDead() || !(caster instanceof L2PcInstance))
			return;

		L2PcInstance activeChar = (L2PcInstance) caster;

		if (_npcId == 0)
		{
			activeChar.sendMessage("Summon skill "+getId()+" not described yet");
			return;
		}

		if (isCubic())
		{
			if (targets.length > 1) //Mass cubic skill
			{
				for (L2Object obj: targets)
				{
					if (!(obj instanceof L2PcInstance)) continue;
					L2PcInstance player = ((L2PcInstance)obj);
					int mastery = player.getSkillLevel(L2Skill.SKILL_CUBIC_MASTERY);
					if (mastery < 0)
						mastery = 0;
                    if (mastery == 0 && player.getCubics().size() > 0)
					{
						//Player can have only 1 cubic - we shuld replace old cubic with new one
                        for (L2CubicInstance c: player.getCubics().values())
                        {
                            c.stopAction();
                            c = null;
                        }
						player.getCubics().clear();
					}
					if (player.getCubics().size() > mastery) continue;
                    if (player.getCubics().containsKey(_npcId))
                    {
                        player.sendMessage("You already have such cubic");
                    }
                    else
                    {

						player.addCubic(_npcId, getLevel(), getPower(), getActivationTime(), getActivationChance());
						player.broadcastUserInfo();
                    }
				}
				return;
			}
			else //normal cubic skill
			{
				int mastery = activeChar.getSkillLevel(L2Skill.SKILL_CUBIC_MASTERY);
				if (mastery < 0)
					mastery = 0;
				if (activeChar.getCubics().size() > mastery) {
					if (Config.DEBUG)
						_log.fine("player can't summon any more cubics. ignore summon skill");
					activeChar.sendPacket(new SystemMessage(SystemMessageId.CUBIC_SUMMONING_FAILED));
					return;
				}
                if (activeChar.getCubics().containsKey(_npcId))
                {
                    activeChar.sendMessage("You already have such cubic");
                    return;
                }
                activeChar.addCubic(_npcId, getLevel(), getPower(), getActivationTime(), getActivationChance());
				activeChar.broadcastUserInfo();
				return;
			}
		}

		if (activeChar.getPet() != null || activeChar.isMounted()) {
			if (Config.DEBUG)
				_log.fine("player has a pet already. ignore summon skill");
			return;
		}

		L2SummonInstance summon;
		L2NpcTemplate summonTemplate = NpcTable.getInstance().getTemplate(_npcId);
		if (summonTemplate == null) 
		{
			_log.warning("Summon attempt for nonexisting NPC ID:"+_npcId+", skill ID:"+this.getId());
			return; // npcID doesn't exist
		}
		if (summonTemplate.type.equalsIgnoreCase("L2SiegeSummon"))
			summon = new L2SiegeSummonInstance(IdFactory.getInstance().getNextId(), summonTemplate, activeChar, this);
		else
			summon = new L2SummonInstance(IdFactory.getInstance().getNextId(), summonTemplate, activeChar, this);

        summon.setName(summonTemplate.name);
        summon.setTitle(activeChar.getName());
        summon.setExpPenalty(_expPenalty);
        if (summon.getLevel() >= Experience.LEVEL.length)
        {
            summon.getStat().setExp(Experience.LEVEL[Experience.LEVEL.length - 1]);
            _log.warning("Summon ("+summon.getName()+") NpcID: "+summon.getNpcId()+" has a level above 75. Please rectify.");
        }
        else
        {
            summon.getStat().setExp(Experience.LEVEL[(summon.getLevel() % Experience.LEVEL.length)]);
        }
		summon.setCurrentHp(summon.getMaxHp());
		summon.setCurrentMp(summon.getMaxMp());
		summon.setHeading(activeChar.getHeading());
    	summon.setRunning();
		activeChar.setPet(summon);

    	L2World.getInstance().storeObject(summon);
        summon.spawnMe(activeChar.getX()+50, activeChar.getY()+100, activeChar.getZ());

	}

}
