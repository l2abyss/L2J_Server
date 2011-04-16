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
package com.l2jserver.gameserver.network.clientpackets;

import java.util.logging.Logger;

import com.l2jserver.Config;
import com.l2jserver.gameserver.datatables.SkillSpellbookTable;
import com.l2jserver.gameserver.datatables.SkillTable;
import com.l2jserver.gameserver.datatables.SkillTreeTable;
import com.l2jserver.gameserver.datatables.SubPledgeSkillTree;
import com.l2jserver.gameserver.datatables.SubPledgeSkillTree.SubUnitSkill;
import com.l2jserver.gameserver.model.L2PledgeSkillLearn;
import com.l2jserver.gameserver.model.L2ShortCut;
import com.l2jserver.gameserver.model.L2Skill;
import com.l2jserver.gameserver.model.L2SkillLearn;
import com.l2jserver.gameserver.model.L2SquadTrainer;
import com.l2jserver.gameserver.model.L2TransformSkillLearn;
import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.model.actor.instance.L2FishermanInstance;
import com.l2jserver.gameserver.model.actor.instance.L2NpcInstance;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.actor.instance.L2TransformManagerInstance;
import com.l2jserver.gameserver.model.actor.instance.L2VillageMasterInstance;
import com.l2jserver.gameserver.model.quest.Quest;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.AcquireSkillDone;
import com.l2jserver.gameserver.network.serverpackets.ExStorageMaxCount;
import com.l2jserver.gameserver.network.serverpackets.PledgeSkillList;
import com.l2jserver.gameserver.network.serverpackets.ShortCutRegister;
import com.l2jserver.gameserver.network.serverpackets.StatusUpdate;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;
import com.l2jserver.gameserver.util.Util;

/**
 * This class ...
 *
 * @version $Revision: 1.7.2.1.2.4 $ $Date: 2005/03/27 15:29:30 $
 */
public class RequestAcquireSkill extends L2GameClientPacket
{
	private static final String _C__6C_REQUESTAQUIRESKILL = "[C] 7C RequestAcquireSkill";
	
	private static Logger _log = Logger.getLogger(RequestAcquireSkill.class.getName());
	
	private int _id;
	private int _level;
	private int _skillType;
	private int subType;
	
	@Override
	protected void readImpl()
	{
		_id = readD();
		_level = readD();
		_skillType = readD();
		if (_skillType == 3)
			subType = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final L2PcInstance player = getClient().getActiveChar();
		if (player == null)
			return;
		
		if (_level < 1 || _level > 1000 || _id < 1 || _id > 32000)
		{
			Util.handleIllegalPlayerAction(player, "Wrong Packet Data in Aquired Skill", Config.DEFAULT_PUNISH);
			_log.warning("Recived Wrong Packet Data in Aquired Skill - id: " + _id + " level: " + _level + " for "+player);
			return;
		}
		
		final L2Npc trainer = player.getLastFolkNPC();
		if (!(trainer instanceof L2NpcInstance))
			return;
		
		if (!trainer.canInteract(player) && !player.isGM())
			return;
		
		if (!Config.ALT_GAME_SKILL_LEARN)
			player.setSkillLearningClassId(player.getClassId());
		
		// If current skill lvl + 1 is not equal to the skill lvl you wanna learn (eg: You have Aggression lvl 3 and the packet sends info that
		// you want to learn Aggression lvl 5, thus skipping lvl 4.) or the packet sends the same level or lower (eg: Aggression lvl 3 and the
		// packet sends info that you want to learn Aggression level 3).
		if (Math.max(player.getSkillLevel(_id), 0) + 1 != _level && _skillType != 3)
			return;
		
		final L2Skill skill = SkillTable.getInstance().getInfo(_id, _level);
		
		int counts = 0;
		int requiredSp = 10000000;
		
		switch (_skillType)
		{
			case 0:
			{
				if (trainer instanceof L2TransformManagerInstance) // transform skills
				{
					int costId = 0;
					
					// Skill Learn bug Fix
					L2TransformSkillLearn[] skillst = SkillTreeTable.getInstance().getAvailableTransformSkills(player);
					for (L2TransformSkillLearn s : skillst)
					{
						L2Skill sk = SkillTable.getInstance().getInfo(s.getId(), s.getLevel());
						if (sk == null || sk != skill)
							continue;
						
						counts++;
						costId = s.getItemId();
						requiredSp = s.getSpCost();
					}
					
					if (counts == 0)
					{
						player.sendMessage("You are trying to learn skill that u can't..");
						Util.handleIllegalPlayerAction(player, "Player " + player.getName() + " tried to learn skill that he can't!!!", Config.DEFAULT_PUNISH);
						return;
					}
					
					if (player.getSp() >= requiredSp)
					{
						if (!player.destroyItemByItemId("Consume", costId, 1, trainer, false))
						{
							// Haven't spellbook
							player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.ITEM_MISSING_TO_LEARN_SKILL));
							showSkillList(trainer, player);
							return;
						}
						
						SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_DISAPPEARED);
						sm.addItemName(costId);
						player.sendPacket(sm);
					}
					else
					{
						player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_SP_TO_LEARN_SKILL));
						showSkillList(trainer, player);
						return;
					}
					break;
				}
				
				// normal skills
				L2SkillLearn[] skills = SkillTreeTable.getInstance().getAvailableSkills(player, player.getSkillLearningClassId());
				for (L2SkillLearn s : skills)
				{
					if (!s.isLearnedByNPC())
						continue;
					
					L2Skill sk = SkillTable.getInstance().getInfo(s.getId(), s.getLevel());
					if (sk == null || sk != skill)
						continue;
					
					counts++;
					requiredSp = SkillTreeTable.getInstance().getSkillCost(player,skill);
				}
				
				if (counts == 0 && !Config.ALT_GAME_SKILL_LEARN)
				{
					player.sendMessage("You are trying to learn skill that u can't..");
					Util.handleIllegalPlayerAction(player, "Player " + player.getName() + " tried to learn skill that he can't!!!", Config.DEFAULT_PUNISH);
					return;
				}
				
				if (player.getSp() >= requiredSp)
				{
					int spbId = -1;
					
					// divine inspiration require book for each level
					if (Config.DIVINE_SP_BOOK_NEEDED && skill.getId() == L2Skill.SKILL_DIVINE_INSPIRATION)
						spbId = SkillSpellbookTable.getInstance().getBookForSkill(skill, _level);
					else if (Config.SP_BOOK_NEEDED && skill.getLevel() == 1)
						spbId = SkillSpellbookTable.getInstance().getBookForSkill(skill);
					
					// spellbook required
					if (spbId > -1)
					{
						if (!player.destroyItemByItemId("Consume", spbId, 1, trainer, false))
						{
							// Haven't spellbook
							player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.ITEM_MISSING_TO_LEARN_SKILL));
							showSkillList(trainer, player);
							return;
						}
						
						SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_DISAPPEARED);
						sm.addItemName(spbId);
						player.sendPacket(sm);
					}
				}
				else
				{
					player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_SP_TO_LEARN_SKILL));
					showSkillList(trainer, player);
					return;
				}
				break;
			}
			case 1:
			{
				int costId = 0;
				int costCount = 0;
				
				// Skill Learn bug Fix
				L2SkillLearn[] skillsc = SkillTreeTable.getInstance().getAvailableSkills(player);
				for (L2SkillLearn s : skillsc)
				{
					if (!s.isLearnedByNPC())
						continue;
					
					L2Skill sk = SkillTable.getInstance().getInfo(s.getId(), s.getLevel());
					if (sk == null || sk != skill)
						continue;
					
					counts++;
					costId = s.getIdCost();
					costCount = s.getCostCount();
					requiredSp = s.getSpCost();
				}
				
				if (counts == 0)
				{
					//player.sendMessage("You are trying to learn skill that u can't..");
					Util.handleIllegalPlayerAction(player, "Player " + player.getName() + " tried to learn skill that he can't!!!", Config.DEFAULT_PUNISH);
					return;
				}
				
				if (player.getSp() >= requiredSp)
				{
					if (!player.destroyItemByItemId("Consume", costId, costCount, trainer, false))
					{
						// Haven't spellbook
						player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.ITEM_MISSING_TO_LEARN_SKILL));
						showSkillList(trainer, player);
						return;
					}
					
					SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S2_S1_DISAPPEARED);
					sm.addItemName(costId);
					sm.addItemNumber(costCount);
					player.sendPacket(sm);
				}
				else
				{
					player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_SP_TO_LEARN_SKILL));
					showSkillList(trainer, player);
					return;
				}
				break;
			}
			case 2:
			{
				int itemId = 0;
				int itemCount = 0;
				int repCost = 100000000;
				
				// Skill Learn bug Fix
				L2PledgeSkillLearn[] skills = SkillTreeTable.getInstance().getAvailablePledgeSkills(player);
				for (L2PledgeSkillLearn s : skills)
				{
					L2Skill sk = SkillTable.getInstance().getInfo(s.getId(), s.getLevel());
					if (sk == null || sk != skill)
						continue;
					
					counts++;
					itemId = s.getItemId();
					itemCount = s.getItemCount();
					repCost = s.getRepCost();
					break;
				}
				
				if (counts == 0)
				{
					//player.sendMessage("You are trying to learn skill that u can't..");
					Util.handleIllegalPlayerAction(player, "Player " + player + " tried to learn clan skill that he can't!!!", Config.DEFAULT_PUNISH);
					return;
				}
				
				if (player.getClan().getReputationScore() >= repCost)
				{
					if (Config.LIFE_CRYSTAL_NEEDED)
					{
						if (!player.destroyItemByItemId("Consume", itemId, itemCount, trainer, false))
						{
							// Haven't spellbook
							player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.ITEM_MISSING_TO_LEARN_SKILL));
							L2VillageMasterInstance.showPledgeSkillList(player);
							return;
						}
						
						SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S2_S1_DISAPPEARED);
						sm.addItemName(itemId);
						sm.addItemNumber(itemCount);
						player.sendPacket(sm);
					}
				}
				else
				{
					player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.ACQUIRE_SKILL_FAILED_BAD_CLAN_REP_SCORE));
					L2VillageMasterInstance.showPledgeSkillList(player);
					return;
				}
				
				player.getClan().takeReputationScore(repCost, true);
				player.getClan().addNewSkill(skill);
				
				if (Config.DEBUG)
					_log.fine("Learned pledge skill " + _id + " for " + requiredSp + " SP.");
				
				SystemMessage cr = SystemMessage.getSystemMessage(SystemMessageId.S1_DEDUCTED_FROM_CLAN_REP);
				cr.addNumber(repCost);
				player.sendPacket(cr);
				
				player.sendPacket(new AcquireSkillDone());
				
				player.getClan().broadcastToOnlineMembers(new PledgeSkillList(player.getClan()));
				
				L2VillageMasterInstance.showPledgeSkillList(player); //Maybe we should add a check here...
				return;
			}
			case 3:
			{
				if (!player.isClanLeader())
					return;
				if (player.getClan().getHasFort() == 0 && player.getClan().getHasCastle() == 0)
					return;
				if (trainer instanceof L2SquadTrainer)
				{
					int id = 0;
					int count = 0;
					int rep = 100000000;
					boolean found = false;
					for (SubUnitSkill sus: SubPledgeSkillTree.getInstance().getAvailableSkills(player.getClan()))
					{
						if (sus.getSkill() == skill)
						{
							id = sus.getItemId();
							count = sus.getCount();
							rep = sus.getReputation();
							found = true;
							break;
						}
					}
					
					// skill not available for clan !?
					if (!found)
						return;
					
					// check if subunit can accept skill
					if (!player.getClan().isLearnableSubSkill(skill, subType))
						return;
					
					if (player.getClan().getReputationScore() < rep)
					{
						player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.ACQUIRE_SKILL_FAILED_BAD_CLAN_REP_SCORE));
						return;
					}
					
					if (!player.destroyItemByItemId("SubSkills", id, count, trainer, false))
					{
						player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.ITEM_MISSING_TO_LEARN_SKILL));
						return;
					}
					else
					{
						SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S2_S1_DISAPPEARED);
						sm.addItemName(id);
						sm.addItemNumber(count);
						player.sendPacket(sm);
					}
					
					player.getClan().takeReputationScore(rep, true);
					SystemMessage cr = SystemMessage.getSystemMessage(SystemMessageId.S1_DEDUCTED_FROM_CLAN_REP);
					cr.addNumber(rep);
					player.sendPacket(cr);
					
					if (subType > -2)
						player.getClan().addNewSkill(skill, subType);
					
					player.getClan().broadcastToOnlineMembers(new PledgeSkillList(player.getClan()));
					
					((L2SquadTrainer) trainer).showSubUnitSkillList(player);
				}
				break;
			}
			case 4:
			{
				requiredSp = 0;
				Quest[] qlst = trainer.getTemplate().getEventQuests(Quest.QuestEventType.ON_SKILL_LEARN);
				if ((qlst != null) && qlst.length == 1)
				{
					if (!qlst[0].notifyAcquireSkill(trainer, player, skill))
					{
						qlst[0].notifyAcquireSkillList(trainer, player);
						return;
					}
				}
				else
					return;
				break;
			}
			case 6:
			{
				int costId = 0;
				int costCount = 0;
				
				// Skill Learn bug Fix
				L2SkillLearn[] skillsc = SkillTreeTable.getInstance().getAvailableSpecialSkills(player);
				for (L2SkillLearn s : skillsc)
				{
					L2Skill sk = SkillTable.getInstance().getInfo(s.getId(), s.getLevel());
					if (sk == null || sk != skill)
						continue;
					
					counts++;
					costId = s.getIdCost();
					costCount = s.getCostCount();
					requiredSp = s.getSpCost();
				}
				
				if (counts == 0)
				{
					player.sendMessage("You are trying to learn skill that u can't..");
					Util.handleIllegalPlayerAction(player, "Player " + player.getName() + " tried to learn skill that he can't!!!", Config.DEFAULT_PUNISH);
					return;
				}
				
				if (player.getSp() >= requiredSp)
				{
					if (!player.destroyItemByItemId("Consume", costId, costCount, trainer, false))
					{
						// Haven't spellbook
						player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.ITEM_MISSING_TO_LEARN_SKILL));
						showSkillList(trainer, player);
						return;
					}
					
					SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S2_S1_DISAPPEARED);
					sm.addItemName(costId);
					sm.addItemNumber(costCount);
					player.sendPacket(sm);
				}
				else
				{
					player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_SP_TO_LEARN_SKILL));
					showSkillList(trainer, player);
					return;
				}
				break;
			}
			default:
			{
				_log.warning("Recived Wrong Packet Data in Aquired Skill - unk1:" + _skillType);
				return;
			}
		}
		
		if (Config.DEBUG)
			_log.fine("Learned skill " + _id + " for " + requiredSp + " SP.");
		
		if (_skillType != 3 && _skillType != 2)
		{
			player.setSp(player.getSp() - requiredSp);
			
			StatusUpdate su = new StatusUpdate(player);
			su.addAttribute(StatusUpdate.SP, player.getSp());
			player.sendPacket(su);
			
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.LEARNED_SKILL_S1);
			sm.addSkillName(skill);
			player.sendPacket(sm);
			
			player.sendPacket(new AcquireSkillDone());
			
			player.addSkill(skill, true);
			player.sendSkillList();
			
			updateShortCuts(player);
			showSkillList(trainer, player);
		}
	}
	
	private void updateShortCuts(L2PcInstance player)
	{
		// update all the shortcuts to this skill
		if (_level > 1)
		{
			L2ShortCut[] allShortCuts = player.getAllShortCuts();
			
			for (L2ShortCut sc : allShortCuts)
			{
				if (sc.getId() == _id && sc.getType() == L2ShortCut.TYPE_SKILL)
				{
					L2ShortCut newsc = new L2ShortCut(sc.getSlot(), sc.getPage(), sc.getType(), sc.getId(), _level, 1);
					player.sendPacket(new ShortCutRegister(newsc));
					player.registerShortCut(newsc);
				}
			}
		}
	}
	
	private void showSkillList(L2Npc trainer, L2PcInstance player)
	{
		if (_skillType == 4)
		{
			Quest[] qlst = trainer.getTemplate().getEventQuests(Quest.QuestEventType.ON_SKILL_LEARN);
			qlst[0].notifyAcquireSkillList(trainer, player);
		}
		else if (trainer instanceof L2FishermanInstance)
			L2FishermanInstance.showFishSkillList(player);
		else if (trainer instanceof L2TransformManagerInstance)
			L2TransformManagerInstance.showTransformSkillList(player);
		else
			L2NpcInstance.showSkillList(player, trainer, player.getSkillLearningClassId());
		
		// if skill is expand sendpacket :)
		if (_id >= 1368 && _id <= 1372)
			player.sendPacket(new ExStorageMaxCount(player));
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.l2jserver.gameserver.clientpackets.ClientBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _C__6C_REQUESTAQUIRESKILL;
	}
}
