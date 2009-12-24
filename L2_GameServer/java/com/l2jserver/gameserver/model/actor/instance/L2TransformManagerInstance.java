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
package com.l2jserver.gameserver.model.actor.instance;

import com.l2jserver.gameserver.datatables.SkillTable;
import com.l2jserver.gameserver.datatables.SkillTreeTable;
import com.l2jserver.gameserver.model.L2Multisell;
import com.l2jserver.gameserver.model.L2Skill;
import com.l2jserver.gameserver.model.L2TransformSkillLearn;
import com.l2jserver.gameserver.model.quest.QuestState;
import com.l2jserver.gameserver.model.quest.State;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.AcquireSkillList;
import com.l2jserver.gameserver.network.serverpackets.ActionFailed;
import com.l2jserver.gameserver.network.serverpackets.NpcHtmlMessage;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;
import com.l2jserver.gameserver.templates.chars.L2NpcTemplate;

public class L2TransformManagerInstance extends L2MerchantInstance
{
    /**
	 * @param objectId
	 * @param template
	 */
	public L2TransformManagerInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public String getHtmlPath(int npcId, int val)
	{
		String pom = "";

		if (val == 0)
			pom = "" + npcId;
		else
			pom = npcId + "-" + val;

		return "data/html/default/" + pom + ".htm";
	}

	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		if (command.startsWith("TransformSkillList"))
		{
			if (canTransform(player))
			{
				player.setSkillLearningClassId(player.getClassId());
				showTransformSkillList(player);
			}
			else
			{
				NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile("data/html/default/" + getNpcId() + "-cantlearn.htm");
				player.sendPacket(html);
			}
		}
		else if (command.startsWith("BuyTransform"))
		{
			if (canTransform(player))
			{				
				L2Multisell.getInstance().separateAndSend(32323001, player, getNpcId(), false, getCastle().getTaxRate());
			}
			else
			{
				NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile("data/html/default/" + getNpcId() + "-cantbuy.htm");
				player.sendPacket(html);
			}
		}
		else
			super.onBypassFeedback(player, command);
	}

	public static boolean canTransform(L2PcInstance player)
	{
		QuestState st = player.getQuestState("136_MoreThanMeetsTheEye");
		if (st != null && st.getState() == State.COMPLETED)
			return true;

		return false;
	}

	/**
	 * this displays TransformationSkillList to the player.
	 * @param player
	 */
	public void showTransformSkillList(L2PcInstance player)
	{        
		L2TransformSkillLearn[] skills = SkillTreeTable.getInstance().getAvailableTransformSkills(player);
		AcquireSkillList asl = new AcquireSkillList(AcquireSkillList.SkillType.Usual);
		int counts = 0;

		for (L2TransformSkillLearn s: skills)
		{
			L2Skill sk = SkillTable.getInstance().getInfo(s.getId(), s.getLevel());
			if (sk == null)
				continue;

			counts++;

			asl.addSkill(s.getId(), s.getLevel(), s.getLevel(), s.getSpCost(), 0);
		}

		if (counts == 0)
		{
			int minlevel = SkillTreeTable.getInstance().getMinLevelForNewTransformSkill(player);
			if (minlevel > 0)
			{
				// No more skills to learn, come back when you level.
				SystemMessage sm = new SystemMessage(SystemMessageId.DO_NOT_HAVE_FURTHER_SKILLS_TO_LEARN);
				sm.addNumber(minlevel);
				player.sendPacket(sm);
			}
			else
				player.sendPacket(new SystemMessage(SystemMessageId.NO_MORE_SKILLS_TO_LEARN));
		}
		else
			player.sendPacket(asl);

		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
}
