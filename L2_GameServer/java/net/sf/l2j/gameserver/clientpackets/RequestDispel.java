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
package net.sf.l2j.gameserver.clientpackets;

import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 *
 * @author  KenM
 */
public class RequestDispel extends L2GameClientPacket
{

	private int _skillId;
	private int _skillLevel;

	/**
     * @see net.sf.l2j.gameserver.clientpackets.L2GameClientPacket#getType()
     */
    @Override
    public String getType()
    {
	    return "[C] D0:4E RequestDispel";
    }

	/**
     * @see net.sf.l2j.gameserver.clientpackets.L2GameClientPacket#readImpl()
     */
    @Override
    protected void readImpl()
    {
	    _skillId = readD();
	    _skillLevel = readD();
    }

	/**
     * @see net.sf.l2j.gameserver.clientpackets.L2GameClientPacket#runImpl()
     */
    @Override
    protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar != null)
		{
			L2Skill skill = SkillTable.getInstance().getInfo(_skillId, _skillLevel);
			if (skill != null && !skill.isDance() && !skill.isDebuff())
			{
				for (L2Effect e : activeChar.getAllEffects())
				{
					if (e != null && e.getSkill() == skill)
					{
						e.exit();
						break;
					}
				}
			}
		}
	}
}
