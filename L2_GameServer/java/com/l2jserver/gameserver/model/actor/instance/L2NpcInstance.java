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

import com.l2jserver.Config;
import com.l2jserver.gameserver.datatables.SkillTable;
import com.l2jserver.gameserver.datatables.SkillTreeTable;
import com.l2jserver.gameserver.model.L2Effect;
import com.l2jserver.gameserver.model.L2Skill;
import com.l2jserver.gameserver.model.L2SkillLearn;
import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.model.actor.status.FolkStatus;
import com.l2jserver.gameserver.model.base.ClassId;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.AcquireSkillList;
import com.l2jserver.gameserver.network.serverpackets.ActionFailed;
import com.l2jserver.gameserver.network.serverpackets.NpcHtmlMessage;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;
import com.l2jserver.gameserver.skills.effects.EffectBuff;
import com.l2jserver.gameserver.skills.effects.EffectDebuff;
import com.l2jserver.gameserver.templates.chars.L2NpcTemplate;
import com.l2jserver.util.StringUtil;

public class L2NpcInstance extends L2Npc
{
	private final ClassId[] _classesToTeach;

	public L2NpcInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
		setInstanceType(InstanceType.L2NpcInstance);
		setIsInvul(false);
		_classesToTeach = template.getTeachInfo();
	}

	@Override
	public FolkStatus getStatus()
	{
		return (FolkStatus)super.getStatus();
	}

	@Override
	public void initCharStatus()
	{
		setStatus(new FolkStatus(this));
	}

	@Override
	public void addEffect(L2Effect newEffect)
	{
		if (newEffect instanceof EffectDebuff || newEffect instanceof EffectBuff)
			super.addEffect(newEffect);
		else if (newEffect != null)
			newEffect.stopEffectTask();
	}

	public ClassId[] getClassesToTeach()
	{
		return _classesToTeach;
	}

	/**
	 * this displays SkillList to the player.
	 * @param player
	 */
	public static void showSkillList(L2PcInstance player, L2Npc npc, ClassId classId)
	{
		if (Config.DEBUG)
			_log.fine("SkillList activated on: "+npc.getObjectId());

		int npcId = npc.getTemplate().npcId;

		if (npcId == 32611)
		{
			L2SkillLearn[] skills = SkillTreeTable.getInstance().getAvailableSpecialSkills(player);
			AcquireSkillList asl = new AcquireSkillList(AcquireSkillList.SkillType.Special);

			int counts = 0;

			for (L2SkillLearn s : skills)
			{
				L2Skill sk = SkillTable.getInstance().getInfo(s.getId(), s.getLevel());

				if (sk == null)
					continue;

				counts++;
				asl.addSkill(s.getId(), s.getLevel(), s.getLevel(), 0, 1);
			}

			if (counts == 0) // No more skills to learn, come back when you level.
				player.sendPacket(new SystemMessage(SystemMessageId.NO_MORE_SKILLS_TO_LEARN));
			else
				player.sendPacket(asl);

			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if (!npc.getTemplate().canTeach(classId))
		{
			npc.showNoTeachHtml(player);
			return;
		}

		if (((L2NpcInstance)npc).getClassesToTeach() == null)
		{
			NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
			final String sb = StringUtil.concat(
					"<html><body>" +
					"I cannot teach you. My class list is empty.<br> Ask admin to fix it. Need add my npcid and classes to skill_learn.sql.<br>NpcId:",
					String.valueOf(npcId),
					", Your classId:",
					String.valueOf(player.getClassId().getId()),
					"<br>" +
					"</body></html>"
			);
			html.setHtml(sb);
			player.sendPacket(html);
			return;
		}

		L2SkillLearn[] skills = SkillTreeTable.getInstance().getAvailableSkills(player, classId);
		AcquireSkillList asl = new AcquireSkillList(AcquireSkillList.SkillType.Usual);
		int counts = 0;

		for (L2SkillLearn s: skills)
		{
			L2Skill sk = SkillTable.getInstance().getInfo(s.getId(), s.getLevel());
			if (sk == null)
				continue;

			int cost = SkillTreeTable.getInstance().getSkillCost(player, sk);
			counts++;

			asl.addSkill(s.getId(), s.getLevel(), s.getLevel(), cost, 0);
		}

		if (counts == 0)
		{
			int minlevel = SkillTreeTable.getInstance().getMinLevelForNewSkill(player, classId);
			if (minlevel > 0)
			{
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
