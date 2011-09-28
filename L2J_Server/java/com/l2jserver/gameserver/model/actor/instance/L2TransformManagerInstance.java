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

import javolution.util.FastList;

import com.l2jserver.Config;
import com.l2jserver.gameserver.datatables.MultiSell;
import com.l2jserver.gameserver.datatables.SkillTable;
import com.l2jserver.gameserver.datatables.SkillTreesData;
import com.l2jserver.gameserver.instancemanager.QuestManager;
import com.l2jserver.gameserver.model.L2ItemInstance;
import com.l2jserver.gameserver.model.L2Skill;
import com.l2jserver.gameserver.model.L2SkillLearn;
import com.l2jserver.gameserver.model.quest.QuestState;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.AcquireSkillList;
import com.l2jserver.gameserver.network.serverpackets.AcquireSkillList.SkillType;
import com.l2jserver.gameserver.network.serverpackets.NpcHtmlMessage;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;
import com.l2jserver.gameserver.templates.chars.L2NpcTemplate;
import com.l2jserver.gameserver.util.Util;

/**
 * This class manage the Transformation Master:<br>
 * Sub-Class Certification system, skill learning and certification cancelling.<br>
 * Transformation skill learning and transformation buying.
 * @author Zoey76
 */
public final class L2TransformManagerInstance extends L2MerchantInstance
{
	private static final String htmlFolder = "data/html/masterTransformation/";
	
	public static final String[] _questVarNames =
	{
		"EmergentAbility65-",
		"EmergentAbility70-",
		"ClassAbility75-",
		"ClassAbility80-"
	};
	
	private static final int[] _itemsIds = { 10280, 10281, 10282, 10283, 10284, 10285, 10286, 10287, 10288, 10289, 10290, 10291, 10292, 10293, 10294, 10612 };
	
	public L2TransformManagerInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
		setInstanceType(InstanceType.L2TransformManagerInstance);
	}
	
	@Override
	public String getHtmlPath(int npcId, int val)
	{
		return htmlFolder + "master_transformation001.htm";
	}
	
	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		if (command.startsWith("LearnTransformationSkill"))
		{
			if (canTransform(player))
			{
				L2TransformManagerInstance.showTransformSkillList(player);
			}
			else
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(player.getHtmlPrefix(), htmlFolder + "master_transformation003.htm");
				player.sendPacket(html);
			}
			return;
		}
		else if (command.startsWith("BuyTransformationItems"))
		{
			if (canTransform(player))
			{
				MultiSell.getInstance().separateAndSend(32323001, player, this, false);
			}
			else
			{
				NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(player.getHtmlPrefix(), htmlFolder + "master_transformation004.htm");
				player.sendPacket(html);
			}
			return;
		}
		else if (command.startsWith("LearnSubClassSkill"))
		{
			if (player.isSubClassActive())
			{
				NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(player.getHtmlPrefix(), htmlFolder + "master_transformation008.htm");
				player.sendPacket(html);
			}
			else
			{
				boolean hasItems = false;
				for (int i : _itemsIds)
				{
					if (player.getInventory().getItemByItemId(i) != null)
					{
						hasItems = true;
						break;
					}
				}
				if (hasItems)
				{
					showSubClassSkillList(player);
				}
				else
				{
					NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile(player.getHtmlPrefix(), htmlFolder + "master_transformation008.htm");
					player.sendPacket(html);
				}
			}
			return;
		}
		else if (command.startsWith("CancelCertification"))
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			if (player.getSubClasses().size() == 0)
			{
				html.setFile(player.getHtmlPrefix(), htmlFolder + "master_transformation007.htm");
			}
			else if (player.isSubClassActive())
			{
				html.setFile(player.getHtmlPrefix(), htmlFolder + "master_transformation008.htm");
			}
			else if (player.getAdena() < Config.FEE_DELETE_SUBCLASS_SKILLS)
			{
				html.setFile(player.getHtmlPrefix(), htmlFolder + "master_transformation008no.htm");
			}
			else
			{
				QuestState st = player.getQuestState("SubClassSkills");
				if (st == null)
				{
					st = QuestManager.getInstance().getQuest("SubClassSkills").newQuestState(player);
				}
				
				int activeCertifications = 0;
				for (String varName : _questVarNames)
				{
					for (int i = 1; i <= Config.MAX_SUBCLASS; i++)
					{
						String qvar = st.getGlobalQuestVar(varName + i);
						if (!qvar.isEmpty() && (qvar.endsWith(";") || !qvar.equals("0")))
						{
							activeCertifications++;
						}
					}
				}
				if (activeCertifications == 0)
				{
					html.setFile(player.getHtmlPrefix(), htmlFolder + "master_transformation010no.htm");
				}
				else
				{
					for (String varName : _questVarNames)
					{
						for (int i = 1; i <= Config.MAX_SUBCLASS; i++)
						{
							final String qvarName = varName + i;
							final String qvar = st.getGlobalQuestVar(qvarName);
							if (qvar.endsWith(";"))
							{
								final String skillIdVar = qvar.replace(";", "");
								if (Util.isDigit(skillIdVar))
								{
									int skillId = Integer.parseInt(skillIdVar);
									final L2Skill sk = SkillTable.getInstance().getInfo(skillId, 1);
									if (sk != null)
									{
										player.removeSkill(sk);
										st.saveGlobalQuestVar(qvarName, "0");
									}
								}
								else
								{
									_log.warning("Invalid Sub-Class Skill Id: " + skillIdVar + " for player " + player.getName() + "!");
								}
							}
							else if (!qvar.isEmpty() && !qvar.equals("0"))
							{
								if (Util.isDigit(qvar))
								{
									final int itemObjId = Integer.parseInt(qvar);
									L2ItemInstance itemInstance = player.getInventory().getItemByObjectId(itemObjId);
									if (itemInstance != null)
									{
										player.destroyItem("CancelCertification", itemObjId, 1, player, false);
									}
									else
									{
										itemInstance = player.getWarehouse().getItemByObjectId(itemObjId);
										if (itemInstance != null)
										{
											_log.warning("Somehow " + player.getName() + " put a certification book into warehouse!");
											player.getWarehouse().destroyItem("CancelCertification", itemInstance, 1, player, false);
										}
										else
										{
											_log.warning("Somehow " + player.getName() + " deleted a certification book!");
										}
									}
									st.saveGlobalQuestVar(qvarName, "0");
								}
								else
								{
									_log.warning("Invalid item object Id: " + qvar + " for player " + player.getName() + "!");
								}
							}
						}
					}
					
					player.reduceAdena("Cleanse", Config.FEE_DELETE_SUBCLASS_SKILLS, this, true);
					html.setFile(player.getHtmlPrefix(), htmlFolder + "master_transformation009no.htm");
					player.sendSkillList();
				}
				
				//Let's consume all certification books, even those not present in database.
				L2ItemInstance itemInstance = null;
				for (int itemId : _itemsIds)
				{
					itemInstance = player.getInventory().getItemByItemId(itemId);
					if (itemInstance != null)
					{
						_log.warning(getClass().getName() + ": player " + player + " had 'extra' certification skill books while cancelling sub-class certifications!");
						player.destroyItem("CancelCertificationExtraBooks", itemInstance, this, false);
					}
				}
			}
			player.sendPacket(html);
			return;
		}
		super.onBypassFeedback(player, command);
	}
	
	//Transformations:
	/**
	 * @param player the player to verify.
	 * @return {code true} if {code player} meets the required conditions to learn a transformation.
	 */
	public static boolean canTransform(L2PcInstance player)
	{
		if (Config.ALLOW_TRANSFORM_WITHOUT_QUEST)
		{
			return true;
		}
		final QuestState st = player.getQuestState("136_MoreThanMeetsTheEye");
		if ((st != null) && st.isCompleted())
		{
			return true;
		}
		return false;
	}
	
	/**
	 * This displays Transformation Skill List to the player.
	 * @param player the active character.
	 */
	public static void showTransformSkillList(L2PcInstance player)
	{
		final FastList<L2SkillLearn> skills = SkillTreesData.getInstance().getAvailableTransformSkills(player);
		final AcquireSkillList asl = new AcquireSkillList(SkillType.ClassTransform);
		int counts = 0;
		
		for (L2SkillLearn s : skills)
		{
			if (SkillTable.getInstance().getInfo(s.getSkillId(), s.getSkillLevel()) != null)
			{
				counts++;
				asl.addSkill(s.getSkillId(), s.getSkillLevel(), s.getSkillLevel(), s.getLevelUpSp(), 0);
			}
		}
		
		if (counts == 0)
		{
			final int minlevel = SkillTreesData.getInstance().getMinLevelForNewSkill(player, SkillTreesData.getInstance().getTransformSkillTree());
			if (minlevel > 0)
			{
				//No more skills to learn, come back when you level.
				final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.DO_NOT_HAVE_FURTHER_SKILLS_TO_LEARN_S1);
				sm.addNumber(minlevel);
				player.sendPacket(sm);
			}
			else
			{
				player.sendPacket(SystemMessageId.NO_MORE_SKILLS_TO_LEARN);
			}
		}
		else
		{
			player.sendPacket(asl);
		}
	}
	
	//SubClass:
	/**
	 * This displays Sub-Class Skill List to the player.
	 * @param player the active character.
	 */
	public static void showSubClassSkillList(L2PcInstance player)
	{
		final FastList<L2SkillLearn> subClassSkills = SkillTreesData.getInstance().getAvailableSubClassSkills(player);
		final AcquireSkillList asl = new AcquireSkillList(AcquireSkillList.SkillType.SubClass);
		int count = 0;
		
		for (L2SkillLearn s : subClassSkills)
		{
			if (SkillTable.getInstance().getInfo(s.getSkillId(), s.getSkillLevel()) != null)
			{
				count++;
				asl.addSkill(s.getSkillId(), s.getSkillLevel(), s.getSkillLevel(), 0, 0);
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
}
