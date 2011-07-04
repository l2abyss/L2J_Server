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
import com.l2jserver.gameserver.model.L2Skill;
import com.l2jserver.gameserver.model.L2SkillLearn;
import com.l2jserver.gameserver.model.L2SquadTrainer;
import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.model.actor.instance.L2NpcInstance;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.actor.instance.L2TransformManagerInstance;
import com.l2jserver.gameserver.network.serverpackets.AcquireSkillInfo;
import com.l2jserver.gameserver.network.serverpackets.AcquireSkillList.SkillType;

/**
 * @author Zoey76
 */
public final class RequestAcquireSkillInfo extends L2GameClientPacket
{
	private static final String _C__6B_REQUESTACQUIRESKILLINFO = "[C] 6B RequestAcquireSkillInfo";
	private static final Logger _log = Logger.getLogger(RequestAcquireSkillInfo.class.getName());
	
	private int _id;
	private int _level;
	private int _skillType;
	
	@Override
	protected void readImpl()
	{
		_id = readD();
		_level = readD();
		_skillType = readD();
	}
	
	@Override
	protected void runImpl()
	{
		if ((_id <= 0) || (_level <= 0))
		{
			return;
		}
		
		final L2PcInstance activeChar = getClient().getActiveChar();
		
		if (activeChar == null)
		{
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
		
		final L2Skill skill = SkillTable.getInstance().getInfo(_id, _level);
		
		if (skill == null)
		{
			_log.warning(RequestAcquireSkillInfo.class.getSimpleName() + ": Skill Id: " + _id + " level: " + _level + " is undefined. " + RequestAcquireSkillInfo.class.getName() + " failed.");
			return;
		}
		
		final SkillType skillType = SkillType.values()[_skillType];
		
		//Doesn't apply to all Skill Types
		if (((skillType != SkillType.Transfer) && ((_level > 1) && (activeChar.getKnownSkill(_id) == null))) || ((activeChar.getKnownSkill(_id) != null) && (activeChar.getKnownSkill(_id).getLevel() != (_level - 1))))
		{
			_log.warning(RequestAcquireSkillInfo.class.getSimpleName() + ": Player " + activeChar.getName() + " is requesting info for skill Id: " + _id + " level " + _level + " without knowing it's previous level!");
		}
		
		switch (skillType)
		{
			case ClassTransform:
			{
				if (trainer instanceof L2TransformManagerInstance)
				{
					final L2SkillLearn s = SkillTreesData.getInstance().getTransformSkill(_id, _level);
					
					if (s != null)
					{
						int itemId = -1;
						int itemCount = -1;
						final int levelUpSp = s.getLevelUpSp();
						
						final AcquireSkillInfo asi = new AcquireSkillInfo(_id, _level, levelUpSp, SkillType.ClassTransform);
						if (s.getItemsIdCount() != null)
						{
							for (int[] itemIdCount : s.getItemsIdCount())
							{
								itemId = itemIdCount[0];
								itemCount = itemIdCount[1];
								
								if ((itemId > 0) && (itemCount > 0))
								{
									asi.addRequirement(99, itemId, itemCount, 50);
								}
							}
						}
						sendPacket(asi);
					}
					return;
				}
				else if (trainer.getTemplate().canTeach(activeChar.getClassId()))
				{
					final L2SkillLearn s = SkillTreesData.getInstance().getClassSkill(_id, _level, activeChar.getClassId());
					if (s != null)
					{
						int itemId = -1;
						int itemCount = -1;
						final int levelUpSp = s.getLevelUpSp();
						
						final AcquireSkillInfo asi = new AcquireSkillInfo(_id, _level, levelUpSp, SkillType.ClassTransform);
						
						if (s.getItemsIdCount() != null)
						{
							for (int[] itemIdCount : s.getItemsIdCount())
							{
								if (!Config.DIVINE_SP_BOOK_NEEDED && (_id == L2Skill.SKILL_DIVINE_INSPIRATION))
								{
									continue;
								}
								
								itemId = itemIdCount[0];
								itemCount = itemIdCount[1];
								
								if ((itemId > 0) && (itemCount > 0))
								{
									asi.addRequirement(99, itemId, itemCount, 50);
								}
							}
						}
						sendPacket(asi);
					}
				}
				break;
			}
			case Fishing:
			{
				final L2SkillLearn s = SkillTreesData.getInstance().getFishingSkill(_id, _level);
				if (s != null)
				{
					int itemId = -1;
					int itemCount = -1;
					final int levelUpSp = s.getLevelUpSp();
					
					final AcquireSkillInfo asi = new AcquireSkillInfo(_id, _level, levelUpSp, SkillType.Fishing);
					for (int[] itemIdCount : s.getItemsIdCount())
					{
						itemId = itemIdCount[0];
						itemCount = itemIdCount[1];
						
						if ((itemId > 0) && (itemCount > 0))
						{
							asi.addRequirement(4, itemId, itemCount, 0);
						}
					}
					sendPacket(asi);
				}
				break;
			}
			case Pledge:
			{
				if (!activeChar.isClanLeader())
				{
					return;
				}
				
				final L2SkillLearn s = SkillTreesData.getInstance().getPledgeSkill(_id, _level);
				if (s != null)
				{
					int itemId = -1;
					int itemCount = -1;
					final int requiredRep = s.getLevelUpSp();
					
					final AcquireSkillInfo asi = new AcquireSkillInfo(_id, _level, requiredRep, SkillType.Pledge);
					
					if (Config.LIFE_CRYSTAL_NEEDED)
					{
						for (int[] itemIdCount : s.getItemsIdCount())
						{
							itemId = itemIdCount[0];
							itemCount = itemIdCount[1];
							
							if ((itemId > 0) && (itemCount > 0))
							{
								asi.addRequirement(1, itemId, itemCount, 0);
							}
						}
					}
					sendPacket(asi);
				}
				break;
			}
			case SubPledge:
			{
				if (!activeChar.isClanLeader())
				{
					return;
				}
				
				if (trainer instanceof L2SquadTrainer)
				{
					final L2SkillLearn s = SkillTreesData.getInstance().getSubPledgeSkill(_id, _level);
					if (s != null)
					{
						int itemId = -1;
						int itemCount = -1;
						final int levelUpSp = s.getLevelUpSp();
						
						final AcquireSkillInfo asi = new AcquireSkillInfo(_id, _level, levelUpSp, SkillType.SubPledge);
						for (int[] itemIdCount : s.getItemsIdCount())
						{
							itemId = itemIdCount[0];
							itemCount = itemIdCount[1];
							
							if ((itemId > 0) && (itemCount > 0))
							{
								asi.addRequirement(0, itemId, itemCount, 0);
							}
						}
						sendPacket(asi);
					}
				}
				break;
			}
			case SubClass:
			{
				final L2SkillLearn s = SkillTreesData.getInstance().getSubClassSkill(_id, _level);
				
				if (s != null)
				{
					int itemId = -1;
					int itemCount = -1;
					final int levelUpSp = s.getLevelUpSp();
					
					final AcquireSkillInfo asi = new AcquireSkillInfo(_id, _level, levelUpSp, SkillType.SubClass);
					for (int[] itemIdCount : s.getItemsIdCount())
					{
						itemId = itemIdCount[0];
						itemCount = itemIdCount[1];
						
						if ((itemId > 0) && (itemCount > 0))
						{
							asi.addRequirement(99, itemId, itemCount, 50);
						}
					}
					sendPacket(asi);
				}
				break;
			}
			case Collect:
			{
				final L2SkillLearn s = SkillTreesData.getInstance().getCollectSkill(_id, _level);
				if (s != null)
				{
					int itemId = -1;
					int itemCount = -1;
					final int levelUpSp = s.getLevelUpSp();
					
					final AcquireSkillInfo asi = new AcquireSkillInfo(_id, _level, levelUpSp, SkillType.Collect);
					for (int[] itemIdCount : s.getItemsIdCount())
					{
						itemId = itemIdCount[0];
						itemCount = itemIdCount[1];
						
						if ((itemId > 0) && (itemCount > 0))
						{
							asi.addRequirement(6, itemId, itemCount, 0);
						}
					}
					sendPacket(asi);
				}
				break;
			}
			case Transfer:
			{
				final L2SkillLearn s = SkillTreesData.getInstance().getTransferSkill(_id, _level, activeChar.getClassId());
				if (s != null)
				{
					int itemId = -1;
					int itemCount = -1;
					final int levelUpSp = s.getLevelUpSp();
					
					final AcquireSkillInfo asi = new AcquireSkillInfo(_id, _level, levelUpSp, SkillType.Transfer);
					for (int[] itemIdCount : s.getItemsIdCount())
					{
						itemId = itemIdCount[0];
						itemCount = itemIdCount[1];
						
						if ((itemId > 0) && (itemCount > 0))
						{
							asi.addRequirement(4, itemId, itemCount, 0);
						}
					}
					sendPacket(asi);
				}
				else
				{
					_log.warning(RequestAcquireSkillInfo.class.getSimpleName() + ": Null L2SkillLearn for id: " + _id + " and level " + _level + " in Transfer Skill Tree for skill learning class " + activeChar.getClassId() + "!");
				}
				break;
			}
		}
	}
	
	@Override
	public String getType()
	{
		return _C__6B_REQUESTACQUIRESKILLINFO;
	}
}
