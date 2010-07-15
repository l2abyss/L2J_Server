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

import gnu.trove.TIntIntHashMap;

import java.util.logging.Logger;

import com.l2jserver.gameserver.cache.HtmCache;
import com.l2jserver.gameserver.datatables.NpcBufferTable;
import com.l2jserver.gameserver.datatables.SkillTable;
import com.l2jserver.gameserver.model.L2ItemInstance;
import com.l2jserver.gameserver.model.L2Skill;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.model.actor.L2Summon;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.ActionFailed;
import com.l2jserver.gameserver.network.serverpackets.NpcHtmlMessage;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;
import com.l2jserver.gameserver.taskmanager.AttackStanceTaskManager;
import com.l2jserver.gameserver.templates.chars.L2NpcTemplate;

/**
 * The Class L2NpcBufferInstance.
 */
public class L2NpcBufferInstance extends L2Npc
{
	static final Logger _log = Logger.getLogger(L2NpcBufferInstance.class.getName());
	
	private static TIntIntHashMap pageVal = new TIntIntHashMap();
	
	/**
	 * Instantiates a new l2 npc buffer instance.
	 *
	 * @param objectId the object id
	 * @param template the template
	 */
	public L2NpcBufferInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
		setInstanceType(InstanceType.L2NpcBufferInstance);
	}
	
	/* (non-Javadoc)
	 * @see com.l2jserver.gameserver.model.actor.L2Npc#showChatWindow(com.l2jserver.gameserver.model.actor.instance.L2PcInstance, int)
	 */
	@Override
	public void showChatWindow(L2PcInstance playerInstance, int val)
	{
		if (playerInstance == null)
			return;
		
		String htmContent = HtmCache.getInstance().getHtm(playerInstance.getHtmlPrefix(), "data/html/mods/NpcBuffer.htm");
		
		if (val > 0)
			htmContent = HtmCache.getInstance().getHtm(playerInstance.getHtmlPrefix(), "data/html/mods/NpcBuffer-" + val + ".htm");
		
		if (htmContent != null)
		{
			NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(getObjectId());
			
			npcHtmlMessage.setHtml(htmContent);
			npcHtmlMessage.replace("%objectId%", String.valueOf(getObjectId()));
			playerInstance.sendPacket(npcHtmlMessage);
		}
		
		playerInstance.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	/* (non-Javadoc)
	 * @see com.l2jserver.gameserver.model.actor.L2Npc#onBypassFeedback(com.l2jserver.gameserver.model.actor.instance.L2PcInstance, java.lang.String)
	 */
	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		// BypassValidation Exploit plug.
		if (player == null || player.getLastFolkNPC() == null || player.getLastFolkNPC().getObjectId() != this.getObjectId())
			return;
		
		L2Character target = player;
		
		if (command.startsWith("Pet"))
		{
			L2Summon pet = player.getPet();
			if (pet == null)
			{
				player.sendMessage("You do not have your pet summoned.");
				showChatWindow(player, 0); // 0 = main window
				return;
			}
			target = pet;
		}
		
		int npcId = getNpcId();
		
		if (command.startsWith("Chat"))
		{
			int val = Integer.parseInt(command.substring(5));
			
			pageVal.put(player.getObjectId(), val);
			
			showChatWindow(player, val);
		}
		else if (command.startsWith("Buff") || command.startsWith("PetBuff"))
		{
			String[] buffGroupArray = command.substring(command.indexOf("Buff") + 5).split(" ");
			
			for (String buffGroupList : buffGroupArray)
			{
				if (buffGroupList == null)
				{
					_log.warning("NPC Buffer Warning: npcId = " + npcId + " has no buffGroup set in the bypass for the buff selected.");
					return;
				}
				
				int buffGroup = Integer.parseInt(buffGroupList);
				
				int[] npcBuffGroupInfo = NpcBufferTable.getInstance().getSkillInfo(npcId, buffGroup);
				
				if (npcBuffGroupInfo == null)
				{
					_log.warning("NPC Buffer Warning: npcId = " + npcId + " Location: " + getX() + ", " + getY() + ", " + getZ() + " Player: " + player.getName() + " has tried to use skill group (" + buffGroup + ") not assigned to the NPC Buffer!");
					return;
				}
				
				int skillId = npcBuffGroupInfo[0];
				int skillLevel = npcBuffGroupInfo[1];
				int skillFeeId = npcBuffGroupInfo[2];
				int skillFeeAmount = npcBuffGroupInfo[3];
				
				if (skillFeeId != 0)
				{
					L2ItemInstance itemInstance = player.getInventory().getItemByItemId(skillFeeId);
					
					if (itemInstance == null || (!itemInstance.isStackable() && player.getInventory().getInventoryItemCount(skillFeeId, -1) < skillFeeAmount))
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.THERE_ARE_NOT_ENOUGH_NECESSARY_ITEMS_TO_USE_THE_SKILL);
						player.sendPacket(sm);
						continue;
					}
					
					if (itemInstance.isStackable())
					{
						if (!player.destroyItemByItemId("Npc Buffer", skillFeeId, skillFeeAmount, player.getTarget(), true))
						{
							SystemMessage sm = new SystemMessage(SystemMessageId.THERE_ARE_NOT_ENOUGH_NECESSARY_ITEMS_TO_USE_THE_SKILL);
							player.sendPacket(sm);
							continue;
						}
					}
					else
					{
						for (int i = 0; i < skillFeeAmount; ++i)
						{
							player.destroyItemByItemId("Npc Buffer", skillFeeId, 1, player.getTarget(), true);
						}
					}
				}
				
				L2Skill skill;
				skill = SkillTable.getInstance().getInfo(skillId, skillLevel);
				
				if (skill != null)
					skill.getEffects(player, target);
			}
			
			showChatWindow(player, pageVal.get(player.getObjectId()));
		}
		else if (command.startsWith("Heal") || command.startsWith("PetHeal"))
		{
			if (!target.isInCombat() && !AttackStanceTaskManager.getInstance().getAttackStanceTask(target))
			{
				String[] healArray = command.substring(command.indexOf("Heal") + 5).split(" ");
				
				for (String healType : healArray)
				{
					if (healType.equalsIgnoreCase("HP"))
					{
						target.setCurrentHp(target.getMaxHp());
					}
					else if (healType.equalsIgnoreCase("MP"))
					{
						target.setCurrentMp(target.getMaxMp());
					}
					else if (healType.equalsIgnoreCase("CP"))
					{
						target.setCurrentCp(target.getMaxCp());
					}
				}
			}
			showChatWindow(player, pageVal.get(player.getObjectId()));
		}
		else if (command.startsWith("RemoveBuffs") || command.startsWith("PetRemoveBuffs"))
		{
			target.stopAllEffectsExceptThoseThatLastThroughDeath();
			showChatWindow(player, pageVal.get(player.getObjectId()));
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}
}
