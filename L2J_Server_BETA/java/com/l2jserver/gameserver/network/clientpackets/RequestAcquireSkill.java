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
import com.l2jserver.gameserver.datatables.SkillTable;
import com.l2jserver.gameserver.datatables.SkillTreesData;
import com.l2jserver.gameserver.instancemanager.QuestManager;
import com.l2jserver.gameserver.model.L2Clan;
import com.l2jserver.gameserver.model.L2ItemInstance;
import com.l2jserver.gameserver.model.L2ShortCut;
import com.l2jserver.gameserver.model.L2Skill;
import com.l2jserver.gameserver.model.L2SkillLearn;
import com.l2jserver.gameserver.model.L2SquadTrainer;
import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.model.actor.instance.L2FishermanInstance;
import com.l2jserver.gameserver.model.actor.instance.L2NpcInstance;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.actor.instance.L2TrainerHealersInstance;
import com.l2jserver.gameserver.model.actor.instance.L2TransformManagerInstance;
import com.l2jserver.gameserver.model.actor.instance.L2VillageMasterInstance;
import com.l2jserver.gameserver.model.quest.Quest;
import com.l2jserver.gameserver.model.quest.QuestState;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.AcquireSkillDone;
import com.l2jserver.gameserver.network.serverpackets.AcquireSkillList.SkillType;
import com.l2jserver.gameserver.network.serverpackets.ExStorageMaxCount;
import com.l2jserver.gameserver.network.serverpackets.PledgeSkillList;
import com.l2jserver.gameserver.network.serverpackets.ShortCutRegister;
import com.l2jserver.gameserver.network.serverpackets.StatusUpdate;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;
import com.l2jserver.gameserver.util.Util;

/**
 * @author Zoey76
 */
public final class RequestAcquireSkill extends L2GameClientPacket
{
	private static final String _C__7C_REQUESTACQUIRESKILL = "[C] 7C RequestAcquireSkill";
	
	private static Logger _log = Logger.getLogger(RequestAcquireSkill.class.getName());
	
	private int _id;
	private int _level;
	private int _skillType;
	private int _subType;
	
	@Override
	protected void readImpl()
	{
		_id = readD();
		_level = readD();
		_skillType = readD();
		if (_skillType == SkillType.SubPledge.ordinal())
		{
			_subType = readD();
		}
	}
	
	@Override
	protected void runImpl()
	{
		final L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		if ((_level < 1) || (_level > 1000) || (_id < 1) || (_id > 32000))
		{
			Util.handleIllegalPlayerAction(activeChar, "Wrong Packet Data in Aquired Skill", Config.DEFAULT_PUNISH);
			_log.warning("Recived Wrong Packet Data in Aquired Skill - id: " + _id + " level: " + _level + " for " + activeChar);
			return;
		}
		
		final L2Npc trainer = activeChar.getLastFolkNPC();
		if (!(trainer instanceof L2NpcInstance))
		{
			return;
		}
		
		if (!trainer.canInteract(activeChar) && !activeChar.isGM())
		{
			return;
		}
		
		final SkillType skillType = SkillType.values()[_skillType];
		if ((activeChar.getSkillLevel(_id) >= _level) && (skillType != SkillType.SubPledge))
		{
			//Already knows the skill with this level
			return;
		}
		
		final L2Skill skill = SkillTable.getInstance().getInfo(_id, _level);
		if (skill == null)
		{
			_log.warning("Player " + activeChar.getName() + " is trying to learn a null skill id: " + _id + " level: " + _level + "!");
			return;
		}
		
		//Hack check. Doesn't apply to all Skill Types
		if (((skillType != SkillType.Transfer) && ((_level > 1) && (activeChar.getKnownSkill(_id) == null))) || ((activeChar.getKnownSkill(_id) != null) && (activeChar.getKnownSkill(_id).getLevel() != (_level - 1))))
		{
			//The previous level skill has not been learned.
			activeChar.sendPacket(SystemMessageId.PREVIOUS_LEVEL_SKILL_NOT_LEARNED);
			Util.handleIllegalPlayerAction(activeChar, "Player " + activeChar.getName() + " is requesting skill Id: " + _id + " level " + _level + " without knowing it's previous level!", 0);
			return;
		}
		
		switch (skillType)
		{
			case ClassTransform:
			{
				//If players is learning transformations:
				if (trainer instanceof L2TransformManagerInstance)
				{
					//Hack check.
					if (!L2TransformManagerInstance.canTransform(activeChar))
					{
						activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_COMPLETED_QUEST_FOR_SKILL_ACQUISITION));
						Util.handleIllegalPlayerAction(activeChar, "Player " + activeChar.getName() + " is requesting skill Id: " + _id + " level " + _level + " without required quests!", 0);
						return;
					}
					
					//Required skills:
					final L2SkillLearn s = SkillTreesData.getInstance().getTransformSkill(_id, _level);
					if ((s != null) && (s.getPreReqSkillIdLvl() != null) && (activeChar.getKnownSkill(s.getPreReqSkillIdLvl()[0]) == null))
					{
						activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_MUST_LEARN_ONYX_BEAST_SKILL));
						return;
					}
					
					if (checkPlayerSkill(activeChar, trainer, s))
					{
						giveSkill(activeChar, trainer, skill);
					}
				}
				else
				{
					final L2SkillLearn s = SkillTreesData.getInstance().getClassSkill(_id, _level, activeChar.getClassId());
					if (checkPlayerSkill(activeChar, trainer, s))
					{
						giveSkill(activeChar, trainer, skill);
					}
				}
				break;
			}
			case Fishing:
			{
				final L2SkillLearn s = SkillTreesData.getInstance().getFishingSkill(_id, _level);
				if (checkPlayerSkill(activeChar, trainer, s))
				{
					giveSkill(activeChar, trainer, skill);
				}
				break;
			}
			case Pledge:
			{
				if (!activeChar.isClanLeader())
				{
					return;
				}
				
				final L2Clan clan = activeChar.getClan();
				
				int itemId = -1;
				int itemCount = -1;
				int repCost = 100000000;
				
				final L2SkillLearn s = SkillTreesData.getInstance().getPledgeSkill(_id, _level);
				if (s != null)
				{
					repCost = s.getLevelUpSp();
					
					if (clan.getReputationScore() >= repCost)
					{
						if (Config.LIFE_CRYSTAL_NEEDED && (s.getItemsIdCount() != null))
						{
							for (int[] itemIdCount : s.getItemsIdCount())
							{
								itemId = itemIdCount[0];
								itemCount = itemIdCount[1];
								
								if ((itemId > 0) && (itemCount > 0))
								{
									if (!activeChar.destroyItemByItemId("Consume", itemId, itemCount, trainer, false))
									{
										//Doesn't have required item.
										activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.ITEM_MISSING_TO_LEARN_SKILL));
										L2VillageMasterInstance.showPledgeSkillList(activeChar);
										return;
									}
									
									final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S2_S1_DISAPPEARED);
									sm.addItemName(itemId);
									sm.addItemNumber(itemCount);
									activeChar.sendPacket(sm);
								}
							}
						}
						
						clan.takeReputationScore(repCost, true);
						
						final SystemMessage cr = SystemMessage.getSystemMessage(SystemMessageId.S1_DEDUCTED_FROM_CLAN_REP);
						cr.addNumber(repCost);
						activeChar.sendPacket(cr);
						
						clan.addNewSkill(skill);
						
						clan.broadcastToOnlineMembers(new PledgeSkillList(clan));
						
						activeChar.sendPacket(new AcquireSkillDone());
						
						L2VillageMasterInstance.showPledgeSkillList(activeChar);
					}
					else
					{
						activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.ACQUIRE_SKILL_FAILED_BAD_CLAN_REP_SCORE));
						L2VillageMasterInstance.showPledgeSkillList(activeChar);
					}
					return;
				}
				break;
			}
			case SubPledge:
			{
				if (!activeChar.isClanLeader())
				{
					return;
				}
				
				final L2Clan clan = activeChar.getClan();
				if ((clan.getHasFort() == 0) && (clan.getHasCastle() == 0))
				{
					return;
				}
				
				if (trainer instanceof L2SquadTrainer)
				{
					int itemId = -1;
					int itemCount = -1;
					int rep = 100000000;
					
					final L2SkillLearn s = SkillTreesData.getInstance().getSubPledgeSkill(_id, _level);
					if (s != null)
					{
						//Hack check. Check if SubPledge can accept the new skill:
						if (!clan.isLearnableSubPledgeSkill(skill, _subType))
						{
							activeChar.sendPacket(SystemMessageId.SQUAD_SKILL_ALREADY_ACQUIRED);
							Util.handleIllegalPlayerAction(activeChar, "Player " + activeChar.getName() + " is requesting skill Id: " + _id + " level " + _level + " without knowing it's previous level!", 0);
							return;
						}
						
						rep = s.getLevelUpSp();
						if (clan.getReputationScore() < rep)
						{
							activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.ACQUIRE_SKILL_FAILED_BAD_CLAN_REP_SCORE));
							return;
						}
						
						for (int[] itemIdCount : s.getItemsIdCount())
						{
							itemId = itemIdCount[0];
							itemCount = itemIdCount[1];
							
							if (!activeChar.destroyItemByItemId("SubSkills", itemId, itemCount, trainer, false))
							{
								activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.ITEM_MISSING_TO_LEARN_SKILL));
								return;
							}
							
							final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S2_S1_DISAPPEARED);
							sm.addItemName(itemId);
							sm.addItemNumber(itemCount);
							activeChar.sendPacket(sm);
						}
						
						if (rep > 0)
						{
							clan.takeReputationScore(rep, true);
							final SystemMessage cr = SystemMessage.getSystemMessage(SystemMessageId.S1_DEDUCTED_FROM_CLAN_REP);
							cr.addNumber(rep);
							activeChar.sendPacket(cr);
						}
						
						clan.addNewSkill(skill, _subType);
						clan.broadcastToOnlineMembers(new PledgeSkillList(clan));
						activeChar.sendPacket(new AcquireSkillDone());
						
						((L2SquadTrainer) trainer).showSubUnitSkillList(activeChar);
						return;
					}
				}
				break;
			}
			case Transfer:
			{
				final L2SkillLearn s = SkillTreesData.getInstance().getTransferSkill(_id, _level, activeChar.getClassId());
				if (checkPlayerSkill(activeChar, trainer, s))
				{
					giveSkill(activeChar, trainer, skill);
				}
				break;
			}
			case SubClass:
			{
				//Hack check.
				if (activeChar.isSubClassActive())
				{
					activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.SKILL_NOT_FOR_SUBCLASS));
					Util.handleIllegalPlayerAction(activeChar, "Player " + activeChar.getName() + " is requesting skill Id: " + _id + " level " + _level + " while Sub-Class is active!", 0);
					return;
				}
				
				final L2SkillLearn s = SkillTreesData.getInstance().getSubClassSkill(_id, _level);
				QuestState st = activeChar.getQuestState("SubClassSkills");
				if (st == null)
				{
					final Quest subClassSkilllsQuest = QuestManager.getInstance().getQuest("SubClassSkills");
					if (subClassSkilllsQuest != null)
					{
						st = subClassSkilllsQuest.newQuestState(activeChar);
					}
					else
					{
						_log.warning("Null SubClassSkills quest, for Sub-Class skill Id: " + _id + " level: " + _level + " for player " + activeChar.getName() + "!");
						return;
					}
				}
				
				for (String varName : L2TransformManagerInstance._questVarNames)
				{
					for (int i = 1; i <= Config.MAX_SUBCLASS; i++)
					{
						final String itemOID = st.getGlobalQuestVar(varName + i);
						if (!itemOID.isEmpty() && !itemOID.endsWith(";") && !itemOID.equals("0"))
						{
							if (Util.isDigit(itemOID))
							{
								final int itemObjId = Integer.parseInt(itemOID);
								final L2ItemInstance item = activeChar.getInventory().getItemByObjectId(itemObjId);
								if (item != null)
								{
									if (Util.contains(s.getItemsIdCount()[0], item.getItemId()))
									{
										if (checkPlayerSkill(activeChar, trainer, s))
										{
											giveSkill(activeChar, trainer, skill);
											//Logging the given skill.
											st.saveGlobalQuestVar(varName + i, skill.getId() + ";");
										}
										return;
									}
								}
								else
								{
									_log.warning("Inexistent item for object Id " + itemObjId + ", for Sub-Class skill Id: " + _id + " level: " + _level + " for player " + activeChar.getName() + "!");
								}
							}
							else
							{
								_log.warning("Invalid item object Id " + itemOID + ", for Sub-Class skill Id: " + _id + " level: " + _level + " for player " + activeChar.getName() + "!");
							}
						}
					}
				}
				
				//Player doesn't have required item.
				activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.ITEM_MISSING_TO_LEARN_SKILL));
				showSkillList(trainer, activeChar);
				break;
			}
			case Collect:
			{
				final L2SkillLearn s = SkillTreesData.getInstance().getCollectSkill(_id, _level);
				if (checkPlayerSkill(activeChar, trainer, s))
				{
					giveSkill(activeChar, trainer, skill);
				}
				break;
			}
			default:
			{
				_log.warning("Recived Wrong Packet Data in Aquired Skill, unknown skill type:" + _skillType);
			}
		}
	}
	
	/**
	 * Perform a simple check for current player and skill.<br>
	 * Takes the needed SP if the skill require it and all requirements are meet.<br>
	 * Consume required items if the skill require it and all requirements are meet.<br>
	 * @param player the skill learning player.
	 * @param trainer the skills teaching Npc.
	 * @param s the skill to be learn.
	 */
	private boolean checkPlayerSkill(L2PcInstance player, L2Npc trainer, L2SkillLearn s)
	{
		if (s != null)
		{
			if ((s.getSkillId() == _id) && (s.getSkillLevel() == _level))
			{
				//Hack check.
				if (s.getGetLevel() > player.getLevel())
				{
					player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_DONT_MEET_SKILL_LEVEL_REQUIREMENTS));
					Util.handleIllegalPlayerAction(player, "Player " + player.getName() + ", level " + player.getLevel() + " is requesting skill Id: " + _id + " level " + _level + " without having minimum required level, " + s.getGetLevel() + "!", 0);
					return false;
				}
				
				//First it checks that the skill require SP and the player has enough SP to learn it.
				final int levelUpSp = s.getLevelUpSp();
				if ((levelUpSp > 0) && (levelUpSp > player.getSp()))
				{
					player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_SP_TO_LEARN_SKILL));
					showSkillList(trainer, player);
					return false;
				}
				
				if (!Config.DIVINE_SP_BOOK_NEEDED && (_id == L2Skill.SKILL_DIVINE_INSPIRATION))
				{
					return true;
				}
				
				if (s.getItemsIdCount() != null)
				{
					//Then checks that the player has all the items
					long reqItemCount = 0;
					for (int[] itemIdCount : s.getItemsIdCount())
					{
						reqItemCount = player.getInventory().getInventoryItemCount(itemIdCount[0], -1);
						if (reqItemCount < itemIdCount[1])
						{
							//Player doesn't have required item.
							player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.ITEM_MISSING_TO_LEARN_SKILL));
							showSkillList(trainer, player);
							return false;
						}
					}
					//If the player has all required items, they are consumed.
					for (int[] itemIdCount : s.getItemsIdCount())
					{
						if (!player.destroyItemByItemId("SkillLearn", itemIdCount[0], itemIdCount[1], trainer, true))
						{
							Util.handleIllegalPlayerAction(player, "Somehow player " + player.getName() + ", level " + player.getLevel() + " lose required item Id: " + itemIdCount[0] + " to learn skill while learning skill Id: " + _id + " level " + _level + "!", 0);
						}
					}
				}
				//If the player has SP and all required items then consume SP.
				if (levelUpSp > 0)
				{
					player.setSp(player.getSp() - levelUpSp);
					final StatusUpdate su = new StatusUpdate(player);
					su.addAttribute(StatusUpdate.SP, player.getSp());
					player.sendPacket(su);
				}
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Add the skill to the player and makes proper updates.
	 * @param player the player acquiring a skill.
	 * @param trainer the Npc teaching a skill.
	 * @param skill the skill to be learn.
	 */
	private void giveSkill(L2PcInstance player, L2Npc trainer, L2Skill skill)
	{
		//Send message.
		final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.LEARNED_SKILL_S1);
		sm.addSkillName(skill);
		player.sendPacket(sm);
		
		player.sendPacket(new AcquireSkillDone());
		
		player.addSkill(skill, true);
		player.sendSkillList();
		
		updateShortCuts(player);
		showSkillList(trainer, player);
		
		//If skill is expand type then sends packet:
		if ((_id >= 1368) && (_id <= 1372))
		{
			player.sendPacket(new ExStorageMaxCount(player));
		}
	}
	
	/**
	 * Updates the shortcut bars with the new acquired skill.
	 * @param player the player that needs a shortcut update.
	 */
	private void updateShortCuts(L2PcInstance player)
	{
		//Update all the shortcuts for this skill
		if (_level > 1)
		{
			final L2ShortCut[] allShortCuts = player.getAllShortCuts();
			
			for (L2ShortCut sc : allShortCuts)
			{
				if ((sc.getId() == _id) && (sc.getType() == L2ShortCut.TYPE_SKILL))
				{
					L2ShortCut newsc = new L2ShortCut(sc.getSlot(), sc.getPage(), sc.getType(), sc.getId(), _level, 1);
					player.sendPacket(new ShortCutRegister(newsc));
					player.registerShortCut(newsc);
				}
			}
		}
	}
	
	/**
	 * Wrapper for returning the skill list to the player after it's done with current skill.
	 * @param trainer the Npc which the {@code player} is interacting.
	 * @param player the active character.
	 */
	private void showSkillList(L2Npc trainer, L2PcInstance player)
	{
		if ((trainer instanceof L2TrainerHealersInstance) && (_skillType == SkillType.Transfer.ordinal()))
		{
			L2TrainerHealersInstance.showTransferSkillList(player);
		}
		else if (trainer instanceof L2FishermanInstance)
		{
			L2FishermanInstance.showFishSkillList(player);
		}
		else if ((trainer instanceof L2TransformManagerInstance) && (_skillType == SkillType.ClassTransform.ordinal()))
		{
			L2TransformManagerInstance.showTransformSkillList(player);
		}
		else if ((trainer instanceof L2TransformManagerInstance) && (_skillType == SkillType.SubClass.ordinal()))
		{
			L2TransformManagerInstance.showSubClassSkillList(player);
		}
		else
		{
			L2NpcInstance.showSkillList(player, trainer, player.getClassId());
		}
	}
	
	@Override
	public String getType()
	{
		return _C__7C_REQUESTACQUIRESKILL;
	}
}
