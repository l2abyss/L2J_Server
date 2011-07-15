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

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import com.l2jserver.Config;
import com.l2jserver.gameserver.datatables.EnchantGroupsTable;
import com.l2jserver.gameserver.datatables.SkillTable;
import com.l2jserver.gameserver.model.L2EnchantSkillGroup.EnchantSkillDetail;
import com.l2jserver.gameserver.model.L2EnchantSkillLearn;
import com.l2jserver.gameserver.model.L2ItemInstance;
import com.l2jserver.gameserver.model.L2ShortCut;
import com.l2jserver.gameserver.model.L2Skill;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.ExBrExtraUserInfo;
import com.l2jserver.gameserver.network.serverpackets.ExEnchantSkillInfo;
import com.l2jserver.gameserver.network.serverpackets.ExEnchantSkillInfoDetail;
import com.l2jserver.gameserver.network.serverpackets.ExEnchantSkillResult;
import com.l2jserver.gameserver.network.serverpackets.ShortCutRegister;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;
import com.l2jserver.gameserver.network.serverpackets.UserInfo;
import com.l2jserver.util.Rnd;

/**
 * Format (ch) dd
 * c: (id) 0xD0
 * h: (subid) 0x06
 * d: skill id
 * d: skill lvl
 * @author -Wooden-
 */
public final class RequestExEnchantSkill extends L2GameClientPacket
{
	private static final String _C__D0_0F_REQUESTEXENCHANTSKILL = "[C] D0:0F RequestExEnchantSkill";
	private static final Logger _log = Logger.getLogger(RequestExEnchantSkill.class.getName());
	private static final Logger _logEnchant = Logger.getLogger("enchant");
	
	private int _skillId;
	private int _skillLvl;
	
	@Override
	protected void readImpl()
	{
		_skillId = readD();
		_skillLvl = readD();
	}
	
	@Override
	protected void runImpl()
	{
		if (_skillId <= 0 || _skillLvl <= 0) // minimal sanity check
			return;

		final L2PcInstance player = getClient().getActiveChar();
		if (player == null)
			return;
		
		if (player.getClassId().level() < 3) // requires to have 3rd class quest completed
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_USE_SKILL_ENCHANT_IN_THIS_CLASS);
			return;
		}
		
		if (player.getLevel() < 76)
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_USE_SKILL_ENCHANT_ON_THIS_LEVEL);
			return;
		}
		
		if (!player.isAllowedToEnchantSkills())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_USE_SKILL_ENCHANT_ATTACKING_TRANSFORMED_BOAT);
			return;
		}
		
		final L2Skill skill = SkillTable.getInstance().getInfo(_skillId, _skillLvl);
		if (skill == null)
		{
			return;
		}
		
		final L2EnchantSkillLearn s = EnchantGroupsTable.getInstance().getSkillEnchantmentBySkillId(_skillId);
		if (s == null)
		{
			return;
		}
		final EnchantSkillDetail esd = s.getEnchantSkillDetail(_skillLvl);
		if (player.getSkillLevel(_skillId) != s.getMinSkillLevel(_skillLvl))
		{
			return;
		}
		
		final int costMultiplier = EnchantGroupsTable.NORMAL_ENCHANT_COST_MULTIPLIER;
		final int requiredSp = esd.getSpCost() * costMultiplier;
		if (player.getSp() >= requiredSp)
		{
			// only first lvl requires book
			final boolean usesBook = _skillLvl % 100 == 1; // 101, 201, 301 ...
			final int reqItemId = EnchantGroupsTable.NORMAL_ENCHANT_BOOK;
			final L2ItemInstance spb = player.getInventory().getItemByItemId(reqItemId);
			
			if (Config.ES_SP_BOOK_NEEDED && usesBook && (spb == null)) // Haven't spellbook
			{
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_DONT_HAVE_ALL_OF_THE_ITEMS_NEEDED_TO_ENCHANT_THAT_SKILL));
				return;
			}
			
			final int requiredAdena = (esd.getAdenaCost() * costMultiplier);
			if (player.getInventory().getAdena() < requiredAdena)
			{
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_DONT_HAVE_ALL_OF_THE_ITEMS_NEEDED_TO_ENCHANT_THAT_SKILL));
				return;
			}
			
			boolean check = player.getStat().removeExpAndSp(0, requiredSp, false);
			if (Config.ES_SP_BOOK_NEEDED && usesBook)
			{
				check &= player.destroyItem("Consume", spb.getObjectId(), 1, player, true);
			}
			
			check &= player.destroyItemByItemId("Consume", 57, requiredAdena, player, true);
			if (!check)
			{
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_DONT_HAVE_ALL_OF_THE_ITEMS_NEEDED_TO_ENCHANT_THAT_SKILL));
				return;
			}
			
			// ok. Destroy ONE copy of the book
			final int rate = esd.getRate(player);
			if (Rnd.get(100) <= rate)
			{
				if (Config.LOG_SKILL_ENCHANTS)
				{
					final LogRecord record = new LogRecord(Level.INFO, "Success");
					record.setParameters(new Object[] { player, skill, spb, rate });
					record.setLoggerName("skill");
					_logEnchant.log(record);
				}
				
				player.addSkill(skill, true);
				player.sendPacket(ExEnchantSkillResult.valueOf(true));
				
				final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_HAVE_SUCCEEDED_IN_ENCHANTING_THE_SKILL_S1);
				sm.addSkillName(_skillId);
				player.sendPacket(sm);
				
				if (Config.DEBUG)
				{
					_log.fine("Learned skill ID: " + _skillId + " Level: " + _skillLvl + " for " + requiredSp + " SP, " + requiredAdena + " Adena.");
				}
			}
			else
			{
				player.addSkill(SkillTable.getInstance().getInfo(_skillId, s.getBaseLevel()), true);
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_HAVE_FAILED_TO_ENCHANT_THE_SKILL_S1));
				player.sendPacket(ExEnchantSkillResult.valueOf(false));
				
				if (Config.LOG_SKILL_ENCHANTS)
				{
					final LogRecord record = new LogRecord(Level.INFO, "Fail");
					record.setParameters(new Object[] { player, skill, spb, rate });
					record.setLoggerName("skill");
					_logEnchant.log(record);
				}
			}
			
			player.sendPacket(new UserInfo(player));
			player.sendPacket(new ExBrExtraUserInfo(player));
			player.sendSkillList();
			player.sendPacket(new ExEnchantSkillInfo(_skillId, player.getSkillLevel(_skillId)));
			player.sendPacket(new ExEnchantSkillInfoDetail(0, _skillId, player.getSkillLevel(_skillId)+1, player));
			updateSkillShortcuts(player);
		}
		else
		{
			player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_DONT_HAVE_ENOUGH_SP_TO_ENCHANT_THAT_SKILL));
		}
	}
	
	private void updateSkillShortcuts(L2PcInstance player)
	{
		// update all the shortcuts to this skill
		L2ShortCut[] allShortCuts = player.getAllShortCuts();
		
		for (L2ShortCut sc : allShortCuts)
		{
			if (sc.getId() == _skillId && sc.getType() == L2ShortCut.TYPE_SKILL)
			{
				L2ShortCut newsc = new L2ShortCut(sc.getSlot(), sc.getPage(), sc.getType(), sc.getId(), player.getSkillLevel(_skillId), 1);
				player.sendPacket(new ShortCutRegister(newsc));
				player.registerShortCut(newsc);
			}
		}
	}
	
	@Override
	public String getType()
	{
		return _C__D0_0F_REQUESTEXENCHANTSKILL;
	}
}
