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
import net.sf.l2j.gameserver.instancemanager.FortManager;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Skill.SkillType;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.Fort;
import net.sf.l2j.gameserver.util.Util;

/**
 * @author _drunk_
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class TakeFort implements ISkillHandler
{
    //private static Logger _log = Logger.getLogger(TakeFort.class.getName());
    private static final SkillType[] SKILL_IDS = {SkillType.TAKEFORT};

    public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets)
    {
        if (!(activeChar instanceof L2PcInstance)) return;

        L2PcInstance player = (L2PcInstance)activeChar;

        if (player.getClan() == null ) return;

        Fort fort = FortManager.getInstance().getFort(player);
        if (fort == null || !checkIfOkToCastFlagDisplay(player, fort, true)) return;

        try
        {
        	fort.EndOfSiege(player.getClan());
        }
        catch(Exception e)
        {}
    }

    public SkillType[] getSkillIds()
    {
        return SKILL_IDS;
    }

    /**
     * Return true if character clan place a flag<BR><BR>
     *
     * @param activeChar The L2Character of the character placing the flag
     *
     */
    public static boolean checkIfOkToCastFlagDisplay(L2Character activeChar, boolean isCheckOnly)
    {
        return checkIfOkToCastFlagDisplay(activeChar, FortManager.getInstance().getFort(activeChar), isCheckOnly);
    }

    public static boolean checkIfOkToCastFlagDisplay(L2Character activeChar, Fort fort, boolean isCheckOnly)
    {
        if (!(activeChar instanceof L2PcInstance))
            return false;

        String text = "";
        L2PcInstance player = (L2PcInstance)activeChar;

        if (fort == null || fort.getFortId() <= 0)
            text = "You must be on fort ground to use this skill";
        else if (!fort.getSiege().getIsInProgress())
            text = "You can only use this skill during a siege.";
        else if (!Util.checkIfInRange(200, player, player.getTarget(), true))
            text = "You are not in range of the flagpole.";
        else if (fort.getSiege().getAttackerClan(player.getClan()) == null)
            text = "You must be an attacker to use this skill";
        else
        {
            if (!isCheckOnly) fort.getSiege().announceToPlayer("Clan " + player.getClan().getName() + " has begun to raise flag.", true);
            return true;
        }

        if (!isCheckOnly)
            player.sendMessage(text);
        return false;
    }
}
