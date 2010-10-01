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

import java.util.logging.Logger;

import javolution.util.FastMap;

import com.l2jserver.Config;
import com.l2jserver.gameserver.datatables.MultiSell;
import com.l2jserver.gameserver.datatables.NpcBufferTable;
import com.l2jserver.gameserver.datatables.SkillTable;
import com.l2jserver.gameserver.model.L2ItemInstance;
import com.l2jserver.gameserver.model.L2Skill;
import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.model.actor.L2Summon;
import com.l2jserver.gameserver.model.olympiad.Olympiad;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.ExHeroList;
import com.l2jserver.gameserver.network.serverpackets.InventoryUpdate;
import com.l2jserver.gameserver.network.serverpackets.MagicSkillUse;
import com.l2jserver.gameserver.network.serverpackets.NpcHtmlMessage;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;
import com.l2jserver.gameserver.templates.chars.L2NpcTemplate;
import com.l2jserver.util.L2FastList;

/**
 * Olympiad Npc's Instance
 *
 * @author godson
 */
public class L2OlympiadManagerInstance extends L2Npc
{
	private static Logger _logOlymp = Logger.getLogger(L2OlympiadManagerInstance.class.getName());
	
	private static final int GATE_PASS = Config.ALT_OLY_COMP_RITEM;
	private static final String FEWER_THAN = "Fewer than " + String.valueOf(Config.ALT_OLY_REG_DISPLAY);
	private static final String MORE_THAN = "More than " + String.valueOf(Config.ALT_OLY_REG_DISPLAY);
	
	public L2OlympiadManagerInstance (int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
		setInstanceType(InstanceType.L2OlympiadManagerInstance);
	}
	
	@Override
	public void onBypassFeedback (L2PcInstance player, String command)
	{
		int npcId = getNpcId();
		
		if (command.startsWith("OlympiadDesc"))
		{
			int val = Integer.parseInt(command.substring(13,14));
			String suffix = command.substring(14);
			showChatWindow(player, val, suffix);
		}
		else if (command.startsWith("OlympiadNoble"))
		{
			if (!player.isNoble() || player.getClassId().level() < 3)
				return;
			
			int passes;
			int val = Integer.parseInt(command.substring(14));
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			
			switch(val)
			{
				case 1:
					Olympiad.getInstance().unRegisterNoble(player);
					break;
				case 2:
					int classed = 0;
					int nonClassed = 0;
					int[] array = Olympiad.getInstance().getWaitingList();
					
					if (array != null)
					{
						classed = array[0];
						nonClassed = array[1];
					}
					html.setFile(player.getHtmlPrefix(), Olympiad.OLYMPIAD_HTML_PATH + "noble_registered.htm");
					if (Config.ALT_OLY_REG_DISPLAY > 0)
					{
						html.replace("%listClassed%", classed < Config.ALT_OLY_REG_DISPLAY ? FEWER_THAN : MORE_THAN);
						html.replace("%listNonClassedTeam%", FEWER_THAN);
						html.replace("%listNonClassed%", nonClassed < Config.ALT_OLY_REG_DISPLAY ? FEWER_THAN : MORE_THAN);
					}
					else
					{
						html.replace("%listClassed%", String.valueOf(classed));
						html.replace("%listNonClassedTeam%", "0");
						html.replace("%listNonClassed%", String.valueOf(nonClassed));
					}
					html.replace("%objectId%", String.valueOf(getObjectId()));
					player.sendPacket(html);
					break;
				case 3:
					int points = Olympiad.getInstance().getNoblePoints(player.getObjectId());
					html.setFile(player.getHtmlPrefix(), Olympiad.OLYMPIAD_HTML_PATH + "noble_points1.htm");
					html.replace("%points%", String.valueOf(points));
					html.replace("%objectId%", String.valueOf(getObjectId()));
					player.sendPacket(html);
					break;
				case 4:
					Olympiad.getInstance().registerNoble(player, false);
					break;
				case 5:
					Olympiad.getInstance().registerNoble(player, true);
					break;
				case 6:
					passes = Olympiad.getInstance().getNoblessePasses(player, false);
					if (passes > 0)
					{
						html.setFile(player.getHtmlPrefix(), Olympiad.OLYMPIAD_HTML_PATH + "noble_settle.htm");
						html.replace("%objectId%", String.valueOf(getObjectId()));
						player.sendPacket(html);
					}
					else
					{
						html.setFile(player.getHtmlPrefix(), Olympiad.OLYMPIAD_HTML_PATH + "noble_nopoints.htm");
						html.replace("%objectId%", String.valueOf(getObjectId()));
						player.sendPacket(html);
					}
					break;
				case 7:
					MultiSell.getInstance().separateAndSend(102, player, this, false);
					break;
				case 8:
					MultiSell.getInstance().separateAndSend(103, player, this, false);
					break;
				case 9:
					int point = Olympiad.getInstance().getLastNobleOlympiadPoints(player.getObjectId());
					html.setFile(player.getHtmlPrefix(), Olympiad.OLYMPIAD_HTML_PATH + "noble_points2.htm");
					html.replace("%points%", String.valueOf(point));
					html.replace("%objectId%", String.valueOf(getObjectId()));
					player.sendPacket(html);
					break;
				case 10:
					passes = Olympiad.getInstance().getNoblessePasses(player, true);
					if (passes > 0)
					{
						L2ItemInstance item = player.getInventory().addItem("Olympiad", GATE_PASS, passes, player, this);
						
						InventoryUpdate iu = new InventoryUpdate();
						iu.addModifiedItem(item);
						player.sendPacket(iu);
						
						SystemMessage sm = new SystemMessage(SystemMessageId.EARNED_ITEM);
						sm.addItemNumber(passes);
						sm.addItemName(item);
						player.sendPacket(sm);
					}
					break;
				default:
					_logOlymp.warning("Olympiad System: Couldnt send packet for request " + val);
					break;
			}
		}
		else if (command.startsWith("OlyBuff"))
		{
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			String[] params = command.split(" ");
			
			if (params[1] == null)
			{
				_log.warning("Olympiad Buffer Warning: npcId = " + npcId + " has no buffGroup set in the bypass for the buff selected.");
				return;
			}
			int buffGroup = Integer.parseInt(params[1]);
			
			int[] npcBuffGroupInfo = NpcBufferTable.getInstance().getSkillInfo(npcId, buffGroup);
			
			if (npcBuffGroupInfo == null)
			{
				_log.warning("Olympiad Buffer Warning: npcId = " + npcId + " Location: " + getX() + ", " + getY() + ", " + getZ() + " Player: " + player.getName() + " has tried to use skill group (" + buffGroup + ") not assigned to the NPC Buffer!");
				return;
			}
			
			int skillId = npcBuffGroupInfo[0];
			int skillLevel = npcBuffGroupInfo[1];
			
			L2Skill skill = SkillTable.getInstance().getInfo(skillId,skillLevel);
			
			setTarget(player);
			
			if (player.olyBuff > 0)
			{
				if (skill != null)
				{
					player.olyBuff--;
					broadcastPacket(new MagicSkillUse(this, player, skill.getId(), skill.getLevel(), 0, 0));
					skill.getEffects(player, player);
					L2Summon summon = player.getPet();
					if (summon != null)
					{
						broadcastPacket(new MagicSkillUse(this, summon, skill.getId(), skill.getLevel(), 0, 0));
						skill.getEffects(summon, summon);
					}
				}
			}
			
			if (player.olyBuff > 0)
			{
				html.setFile(player.getHtmlPrefix(), player.olyBuff == 5 ? Olympiad.OLYMPIAD_HTML_PATH + "olympiad_buffs.htm" : Olympiad.OLYMPIAD_HTML_PATH + "olympiad_5buffs.htm");
				html.replace("%objectId%", String.valueOf(getObjectId()));
				player.sendPacket(html);
			}
			else
			{
				html.setFile(player.getHtmlPrefix(), Olympiad.OLYMPIAD_HTML_PATH + "olympiad_nobuffs.htm");
				html.replace("%objectId%", String.valueOf(getObjectId()));
				player.sendPacket(html);
				deleteMe();
			}
		}
		else if (command.startsWith("Olympiad"))
		{
			int val = Integer.parseInt(command.substring(9,10));
			
			NpcHtmlMessage reply = new NpcHtmlMessage(getObjectId());
			
			switch (val)
			{
				case 1:
					FastMap<Integer, String> matches = Olympiad.getInstance().getMatchList();
					reply.setFile(player.getHtmlPrefix(), Olympiad.OLYMPIAD_HTML_PATH + "olympiad_observe1.htm");
					
					for (int i = 0; i < Olympiad.getStadiumCount(); i++)
					{
						int arenaID = i + 1;
						
						// &$906; -> \\&\\$906;
						reply.replace("%title"+arenaID+"%", matches.containsKey(i) ? matches.get(i) : "\\&$906;");
					}
					reply.replace("%objectId%", String.valueOf(getObjectId()));
					player.sendPacket(reply);
					break;
				case 2:
					// for example >> Olympiad 1_88
					int classId = Integer.parseInt(command.substring(11));
					if ((classId >= 88 && classId <= 118) || (classId >= 131 && classId <= 134) || classId == 136)
					{
						L2FastList<String> names = Olympiad.getInstance().getClassLeaderBoard(classId);
						reply.setFile(player.getHtmlPrefix(), Olympiad.OLYMPIAD_HTML_PATH + "olympiad_ranking.htm");
						
						int index = 1;
						for (String name : names)
						{
							reply.replace("%place"+index+"%", String.valueOf(index));
							reply.replace("%rank"+index+"%", name);
							index++;
							if (index > 10)
								break;
						}
						for (; index <= 10; index++)
						{
							reply.replace("%place"+index+"%", "");
							reply.replace("%rank"+index+"%", "");
						}
						
						reply.replace("%objectId%", String.valueOf(getObjectId()));
						player.sendPacket(reply);
					}
					break;
				case 3:
					int id = Integer.parseInt(command.substring(11));
					Olympiad.addSpectator(id, player, true);
					break;
				case 4:
					player.sendPacket(new ExHeroList());
					break;
				default:
					_logOlymp.warning("Olympiad System: Couldnt send packet for request " + val);
					break;
			}
		}
		else
			super.onBypassFeedback(player, command);
	}
	
	private void showChatWindow(L2PcInstance player, int val, String suffix)
	{
		String filename = Olympiad.OLYMPIAD_HTML_PATH;
		
		filename += "noble_desc" + val;
		filename += (suffix != null)? suffix + ".htm" : ".htm";
		
		if (filename.equals(Olympiad.OLYMPIAD_HTML_PATH + "noble_desc0.htm"))
			filename = Olympiad.OLYMPIAD_HTML_PATH + "noble_main.htm";
		
		showChatWindow(player, filename);
	}
}
