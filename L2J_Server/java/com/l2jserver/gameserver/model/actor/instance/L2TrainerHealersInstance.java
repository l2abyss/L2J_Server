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

import java.util.Collection;

import javolution.util.FastList;

import com.l2jserver.Config;
import com.l2jserver.gameserver.datatables.SkillTable;
import com.l2jserver.gameserver.datatables.SkillTreesData;
import com.l2jserver.gameserver.model.L2Skill;
import com.l2jserver.gameserver.model.L2SkillLearn;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.AcquireSkillList;
import com.l2jserver.gameserver.network.serverpackets.AcquireSkillList.SkillType;
import com.l2jserver.gameserver.network.serverpackets.NpcHtmlMessage;
import com.l2jserver.gameserver.templates.chars.L2NpcTemplate;

/**
 * @author Zoey76
 */
public final class L2TrainerHealersInstance extends L2TrainerInstance
{
	public L2TrainerHealersInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
		setInstanceType(InstanceType.L2TrainerHealersInstance);
	}
	
	@Override
	public String getHtmlPath(int npcId, int val)
	{
		String pom = "";
		if (val == 0)
		{
			pom = "" + npcId;
		}
		else
		{
			pom = npcId + "-" + val;
		}
		
		return "data/html/trainer/skilltransfer/" + pom + ".htm";
	}
	
	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		if (command.equals("SkillTransfer_Learn"))
		{
			if (!getTemplate().canTeach(player.getClassId()))
			{
				showNoTeachHtml(player);
				return;
			}
			if ((player.getLevel() < 76) || (player.getClassId().level() < 3))
			{
				html.setFile(player.getHtmlPrefix(), "data/html/trainer/skilltransfer/learn-lowlevel.htm");
				player.sendPacket(html);
				return;
			}
			showTransferSkillList(player);
		}
		else if (command.equals("SkillTransfer_Cleanse"))
		{
			if (!getTemplate().canTeach(player.getClassId()))
			{
				showNoTeachHtml(player);
				return;
			}
			if ((player.getLevel() < 76) || (player.getClassId().level() < 3))
			{
				html.setFile(player.getHtmlPrefix(), "data/html/trainer/skilltransfer/cleanse-no.htm");
				player.sendPacket(html);
				return;
			}
			if (player.getAdena() < Config.FEE_DELETE_TRANSFER_SKILLS)
			{
				player.sendPacket(SystemMessageId.CANNOT_RESET_SKILL_LINK_BECAUSE_NOT_ENOUGH_ADENA);
				return;
			}
			
			boolean hasSkills = false;
			if (!hasTransferSkillItems(player))
			{
				final Collection<L2SkillLearn> skills = SkillTreesData.getInstance().getTransferSkillTree(player.getClassId()).values();
				
				for (L2SkillLearn s : skills)
				{
					final L2Skill sk = player.getKnownSkill(s.getSkillId());
					if (sk != null)
					{
						player.removeSkill(sk);
						if (s.getItemsIdCount() != null)
						{
							player.addItem("Cleanse", s.getItemsIdCount()[0][0], s.getItemsIdCount()[0][1], this, true);
						}
						else
						{
							_log.warning(getClass().getSimpleName() + ": Transfer skill Id: " + s.getSkillId() + " doesn't have required items defined!");
						}
						hasSkills = true;
					}
				}
				
				//Adena gets reduced once.
				if (hasSkills)
				{
					player.reduceAdena("Cleanse", Config.FEE_DELETE_TRANSFER_SKILLS, this, true);
				}
			}
			else
			{
				//Come back when you have used all transfer skill items for this class.
				html.setFile(player.getHtmlPrefix(), "data/html/trainer/skilltransfer/cleanse-no_skills.htm");
				player.sendPacket(html);
			}
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}
	
	/**
	 * This displays Transfer Skill List to the player.
	 * @param player the active character.
	 */
	public static void showTransferSkillList(L2PcInstance player)
	{
		final FastList<L2SkillLearn> skills = SkillTreesData.getInstance().getAvailableTransferSkills(player);
		final AcquireSkillList asl = new AcquireSkillList(SkillType.Transfer);
		int count = 0;
		
		for (L2SkillLearn s : skills)
		{
			if (SkillTable.getInstance().getInfo(s.getSkillId(), s.getSkillLevel()) != null)
			{
				count++;
				asl.addSkill(s.getSkillId(), s.getSkillLevel(), s.getSkillLevel(), s.getLevelUpSp(), 0);
			}
		}
		
		if (count > 0)
		{
			player.sendPacket(asl);
		}
		else
		{
			player.sendPacket(SystemMessageId.NO_MORE_SKILLS_TO_LEARN);
		}
	}
	
	private boolean hasTransferSkillItems(L2PcInstance player)
	{
		int itemId;
		switch (player.getClassId())
		{
			case cardinal:
				itemId = 15307;
				break;
			case evaSaint:
				itemId = 15308;
				break;
			case shillienSaint:
				itemId = 15309;
				break;
			default:
				itemId = -1;
		}
		return (player.getInventory().getInventoryItemCount(itemId, -1) > 0);
	}
}
