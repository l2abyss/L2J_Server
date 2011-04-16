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
import com.l2jserver.gameserver.SevenSigns;
import com.l2jserver.gameserver.datatables.SubPledgeSkillTree;
import com.l2jserver.gameserver.datatables.SubPledgeSkillTree.SubUnitSkill;
import com.l2jserver.gameserver.instancemanager.InstanceManager;
import com.l2jserver.gameserver.model.L2SquadTrainer;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.entity.TvTEvent;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.AcquireSkillList;
import com.l2jserver.gameserver.network.serverpackets.AcquireSkillList.SkillType;
import com.l2jserver.gameserver.network.serverpackets.ActionFailed;
import com.l2jserver.gameserver.network.serverpackets.NpcHtmlMessage;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;
import com.l2jserver.gameserver.templates.chars.L2NpcTemplate;
import com.l2jserver.gameserver.templates.skills.L2EffectType;
import com.l2jserver.util.Rnd;

/**
 * @author Kerberos | ZaKaX
 */
public class L2CastleMagicianInstance extends L2NpcInstance implements L2SquadTrainer
{
	protected static final int COND_ALL_FALSE = 0;
	protected static final int COND_BUSY_BECAUSE_OF_SIEGE = 1;
	protected static final int COND_OWNER = 2;
	
	/**
	 * @param template
	 */
	public L2CastleMagicianInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
		setInstanceType(InstanceType.L2CastleMagicianInstance);
	}
	
	@Override
	public void showChatWindow(L2PcInstance player, int val)
	{
		player.sendPacket( ActionFailed.STATIC_PACKET );
		String filename = "data/html/castlemagician/magician-no.htm";
		
		int condition = validateCondition(player);
		if (condition > COND_ALL_FALSE)
		{
			if (condition == COND_BUSY_BECAUSE_OF_SIEGE)
				filename = "data/html/castlemagician/magician-busy.htm"; // Busy because of siege
			else if (condition == COND_OWNER)                                    // Clan owns castle
			{
				if (val == 0)
					filename = "data/html/castlemagician/magician.htm";
				else
					filename = "data/html/castlemagician/magician-" + val + ".htm";
			}
		}
		
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(player.getHtmlPrefix(), filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);
	}
	
	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		if (command.startsWith("Chat"))
		{
			int val = 0;
			try
			{
				val = Integer.parseInt(command.substring(5));
			}
			catch (IndexOutOfBoundsException ioobe){}
			catch (NumberFormatException nfe){}
			showChatWindow(player, val);
			return;
		}
		else if (command.startsWith("ExchangeKE"))
		{
			String filename = null;
			int i1;
			int item;
			int i0 = Rnd.get(100);
			if (i0 < 5)
			{
				i1 = Rnd.get(25);
				if (i0 < 5)
				{
					item = 9931;
				}
				else if (i1 <= 50)
				{
					item = 9932;
				}
				else if( i1 <= 75 )
				{
					item = 10416;
				}
				else
				{
					item = 10417;
				}
			}
			else if (i0 <= 15)
			{
				i1 = Rnd.get(100);
				if( i1 <= 20 )
				{
					item = 9917;
				}
				else if( i1 <= 40 )
				{
					item = 9918;
				}
				else if( i1 <= 60 )
				{
					item = 9928;
				}
				else if( i1 <= 80 )
				{
					item = 9929;
				}
				else
				{
					item = 9920;
				}
			}
			else if( i0 <= 30 )
			{
				i1 = Rnd.get(100);
				if( i1 <= 12 )
				{
					item = 9916;
				}
				else if( i1 <= 25 )
				{
					item = 9916;
				}
				else if( i1 <= 37 )
				{
					item = 9924;
				}
				else if( i1 <= 50 )
				{
					item = 9925;
				}
				else if( i1 <= 62 )
				{
					item = 9926;
				}
				else if( i1 <= 75 )
				{
					item = 9927;
				}
				else if( i1 <= 87 )
				{
					item = 10518;
				}
				else
				{
					item = 10424;
				}
			}
			else
			{
				i1 = Rnd.get(46);
				if( i1 == 0 )
				{
					item = 9914;
				}
				else if( i1 == 1 )
				{
					item = 9915;
				}
				else if( i1 == 2 )
				{
					item = 9920;
				}
				else if( i1 == 3 )
				{
					item = 9920;
				}
				else if( i1 == 4 )
				{
					item = 9921;
				}
				else if( i1 == 5 )
				{
					item = 9922;
				}
				else if( i1 == 6 )
				{
					item = 9933;
				}
				else if( i1 == 7 )
				{
					item = 9934;
				}
				else if( i1 == 8 )
				{
					item = 9935;
				}
				else if( i1 == 9 )
				{
					item = 9936;
				}
				else if( i1 == 10 )
				{
					item = 9937;
				}
				else if( i1 == 11 )
				{
					item = 9938;
				}
				else if( i1 == 12 )
				{
					item = 9939;
				}
				else if( i1 == 13 )
				{
					item = 9940;
				}
				else if( i1 == 14 )
				{
					item = 9941;
				}
				else if( i1 == 15 )
				{
					item = 9942;
				}
				else if( i1 == 16 )
				{
					item = 9943;
				}
				else if( i1 == 17 )
				{
					item = 9944;
				}
				else if( i1 == 18 )
				{
					item = 9945;
				}
				else if( i1 == 19 )
				{
					item = 9946;
				}
				else if( i1 == 20 )
				{
					item = 9947;
				}
				else if( i1 == 21 )
				{
					item = 9948;
				}
				else if( i1 == 22 )
				{
					item = 9949;
				}
				else if( i1 == 23 )
				{
					item = 9950;
				}
				else if( i1 == 25 )
				{
					item = 9952;
				}
				else if( i1 == 26 )
				{
					item = 9953;
				}
				else if( i1 == 27 )
				{
					item = 9954;
				}
				else if( i1 == 28 )
				{
					item = 9955;
				}
				else if( i1 == 29 )
				{
					item = 9956;
				}
				else if( i1 == 30 )
				{
					item = 9957;
				}
				else if( i1 == 31 )
				{
					item = 9958;
				}
				else if( i1 == 32 )
				{
					item = 9959;
				}
				else if( i1 == 33 )
				{
					item = 9960;
				}
				else if( i1 == 34 )
				{
					item = 9961;
				}
				else if( i1 == 35 )
				{
					item = 9962;
				}
				else if( i1 == 36 )
				{
					item = 9963;
				}
				else if( i1 == 37 )
				{
					item = 9964;
				}
				else if( i1 == 24 )
				{
					item = 9965;
				}
				else if( i1 == 38 )
				{
					item = 10418;
				}
				else if( i1 == 39 )
				{
					item = 10420;
				}
				else if( i1 == 40 )
				{
					item = 10519;
				}
				else if( i1 == 41 )
				{
					item = 10422;
				}
				else if( i1 == 42 )
				{
					item = 10423;
				}
				else if( i1 == 43 )
				{
					item = 10419;
				}
				else
				{
					item = 10421;
				}
			}
			
			if (player.destroyItemByItemId("ExchangeKE", 9912, 10, this, false))
			{
				SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.S2_S1_DISAPPEARED);
				msg.addItemName(9912);
				msg.addNumber(10);
				player.sendPacket(msg);
				
				player.addItem("ExchangeKE", item, 1, player, true);
				
				filename = "data/html/castlemagician/magician-KE-Exchange.htm";
			}
			else
				filename = "data/html/castlemagician/magician-no-KE.htm";
			
			showChatWindow(player, filename);
			return;
		}
		else if (command.equals("gotoleader"))
		{
			if (player.getClan() != null)
			{
				L2PcInstance clanLeader = player.getClan().getLeader().getPlayerInstance();
				if (clanLeader == null)
					return;
				
				if (clanLeader.getFirstEffect(L2EffectType.CLAN_GATE) != null)
				{
					if (!validateGateCondition(clanLeader, player))
						return;
					
					player.teleToLocation(clanLeader.getX(), clanLeader.getY(), clanLeader.getZ(), false);
					return;
				}
				String filename = "data/html/castlemagician/magician-nogate.htm";
				showChatWindow(player, filename);
			}
			return;
		}
		else if (command.equals("subskills"))
		{
			if (player.getClan() != null)
			{
				if (player.isClanLeader())
				{
					AcquireSkillList skilllist = new AcquireSkillList(SkillType.SubUnit);
					SubUnitSkill[] array = SubPledgeSkillTree.getInstance().getAvailableSkills(player.getClan());
					if (array.length == 0)
					{
						player.sendPacket(SystemMessageId.NO_MORE_SKILLS_TO_LEARN);
						return;
					}
					for (SubUnitSkill sus : array)
					{
						skilllist.addSkill(sus.getSkill().getId(), sus.getSkill().getLevel(), sus.getSkill().getLevel(), sus.getReputation(), 0);
					}
					player.sendPacket(skilllist);
				}
				else
				{
					String filename = "data/html/castlemagician/magician-nosquad.htm";
					showChatWindow(player, filename);
				}
			}
		}
		else
			super.onBypassFeedback(player, command);
	}
	
	protected int validateCondition(L2PcInstance player)
	{
		if (player.isGM())
			return COND_OWNER;
		if (getCastle() != null && getCastle().getCastleId() > 0)
		{
			if (player.getClan() != null)
			{
				if (getCastle().getZone().isActive())
					return COND_BUSY_BECAUSE_OF_SIEGE;                   // Busy because of siege
				else if (getCastle().getOwnerId() == player.getClanId()) // Clan owns castle
					return COND_OWNER;
			}
		}
		return COND_ALL_FALSE;
	}
	
	private static final boolean validateGateCondition(L2PcInstance clanLeader, L2PcInstance player)
	{
		if (clanLeader.isAlikeDead())
		{
			// Need retail message if there's one.
			player.sendMessage("Couldn't teleport to clan leader. The requirements was not meet.");
			return false;
		}
		
		if (clanLeader.isInStoreMode())
		{
			// Need retail message if there's one.
			player.sendMessage("Couldn't teleport to clan leader. The requirements was not meet.");
			return false;
		}
		
		if (clanLeader.isRooted() || clanLeader.isInCombat())
		{
			// Need retail message if there's one.
			player.sendMessage("Couldn't teleport to clan leader. The requirements was not meet.");
			return false;
		}
		
		if (clanLeader.isInOlympiadMode())
		{
			// Need retail message if there's one.
			player.sendMessage("Couldn't teleport to clan leader. The requirements was not meet.");
			return false;
		}
		
		if (clanLeader.isFestivalParticipant())
		{
			// Need retail message if there's one.
			player.sendMessage("Couldn't teleport to clan leader. The requirements was not meet.");
			return false;
		}
		
		if (clanLeader.inObserverMode())
		{
			// Need retail message if there's one.
			player.sendMessage("Couldn't teleport to clan leader. The requirements was not meet.");
			return false;
		}
		
		if (clanLeader.isInsideZone(L2Character.ZONE_NOSUMMONFRIEND))
		{
			// Need retail message if there's one.
			player.sendMessage("Couldn't teleport to clan leader. The requirements was not meet.");
			return false;
		}
		
		if (clanLeader.getInstanceId() > 0)
		{
			if (!Config.ALLOW_SUMMON_TO_INSTANCE
					|| InstanceManager.getInstance().getInstance(player.getInstanceId()).isSummonAllowed())
			{
				// Need retail message if there's one.
				player.sendMessage("Couldn't teleport to clan leader. The requirements was not meet.");
				return false;
			}
		}
		
		if (player.isIn7sDungeon())
		{
			final int targetCabal = SevenSigns.getInstance().getPlayerCabal(clanLeader.getObjectId());
			if (SevenSigns.getInstance().isSealValidationPeriod())
			{
				if (targetCabal != SevenSigns.getInstance().getCabalHighestScore())
				{
					// Need retail message if there's one.
					player.sendMessage("Couldn't teleport to clan leader. The requirements was not meet.");
					return false;
				}
			}
			else
			{
				if (targetCabal == SevenSigns.CABAL_NULL)
				{
					// Need retail message if there's one.
					player.sendMessage("Couldn't teleport to clan leader. The requirements was not meet.");
					return false;
				}
			}
		}
		
		if (!TvTEvent.onEscapeUse(player.getObjectId()))
		{
			player.sendMessage("You on TvT Event, teleporting disabled.");
			return false;
		}
		
		if (!TvTEvent.onEscapeUse(clanLeader.getObjectId()))
		{
			// Need retail message if there's one.
			player.sendMessage("Couldn't teleport to clan leader. The requirements was not meet.");
			return false;
		}
		
		return true;
	}
	
	/* (non-Javadoc)
	 * @see com.l2jserver.gameserver.model.actor.L2SquadTrainer#showSubUnitSkillList(com.l2jserver.gameserver.model.actor.instance.L2PcInstance)
	 */
	@Override
	public void showSubUnitSkillList(L2PcInstance player)
	{
		onBypassFeedback(player, "subskills");
	}
}