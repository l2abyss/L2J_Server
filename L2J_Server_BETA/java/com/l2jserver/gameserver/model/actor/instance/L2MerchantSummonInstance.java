/*
 * Copyright (C) 2004-2013 L2J Server
 * 
 * This file is part of L2J Server.
 * 
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.l2jserver.gameserver.model.actor.instance;

import java.util.StringTokenizer;

import com.l2jserver.Config;
import com.l2jserver.gameserver.ai.CtrlIntention;
import com.l2jserver.gameserver.ai.L2CharacterAI;
import com.l2jserver.gameserver.datatables.BuyListData;
import com.l2jserver.gameserver.model.L2Party;
import com.l2jserver.gameserver.model.L2WorldRegion;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.templates.L2NpcTemplate;
import com.l2jserver.gameserver.model.buylist.L2BuyList;
import com.l2jserver.gameserver.model.skills.L2Skill;
import com.l2jserver.gameserver.network.serverpackets.ActionFailed;
import com.l2jserver.gameserver.network.serverpackets.BuyList;
import com.l2jserver.gameserver.network.serverpackets.ExBuySellList;
import com.l2jserver.gameserver.network.serverpackets.NpcHtmlMessage;
import com.l2jserver.gameserver.network.serverpackets.SellList;

/**
 * @author Kerberos
 */
public class L2MerchantSummonInstance extends L2ServitorInstance
{
	public L2MerchantSummonInstance(int objectId, L2NpcTemplate template, L2PcInstance owner, L2Skill skill)
	{
		super(objectId, template, owner, skill);
		setInstanceType(InstanceType.L2MerchantSummonInstance);
	}
	
	@Override
	public boolean hasAI()
	{
		return false;
	}
	
	@Override
	public L2CharacterAI getAI()
	{
		return null;
	}
	
	@Override
	public void deleteMe(L2PcInstance owner)
	{
		
	}
	
	@Override
	public void unSummon(L2PcInstance owner)
	{
		if (isVisible())
		{
			stopAllEffects();
			L2WorldRegion oldRegion = getWorldRegion();
			decayMe();
			if (oldRegion != null)
			{
				oldRegion.removeFromZones(this);
			}
			getKnownList().removeAllKnownObjects();
			setTarget(null);
			
			if (_summonLifeTask != null)
			{
				_summonLifeTask.cancel(false);
				_summonLifeTask = null;
			}
		}
	}
	
	@Override
	public void setFollowStatus(boolean state)
	{
		
	}
	
	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		return false;
	}
	
	@Override
	public boolean isInvul()
	{
		return true;
	}
	
	@Override
	public L2Party getParty()
	{
		return null;
	}
	
	@Override
	public boolean isInParty()
	{
		return false;
	}
	
	@Override
	public boolean useMagic(L2Skill skill, boolean forceUse, boolean dontMove)
	{
		return false;
	}
	
	@Override
	public void doCast(L2Skill skill)
	{
		
	}
	
	@Override
	public boolean isInCombat()
	{
		return false;
	}
	
	@Override
	public final void sendDamageMessage(L2Character target, int damage, boolean mcrit, boolean pcrit, boolean miss)
	{
		
	}
	
	@Override
	public void reduceCurrentHp(double i, L2Character attacker, boolean awake, boolean isDOT, L2Skill skill)
	{
		
	}
	
	@Override
	public void updateAndBroadcastStatus(int val)
	{
		
	}
	
	@Override
	public void onAction(L2PcInstance player, boolean interact)
	{
		if (player.isOutOfControl())
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Check if the L2PcInstance already target the L2NpcInstance
		if (this != player.getTarget())
		{
			// Set the target of the L2PcInstance player
			player.setTarget(this);
		}
		else if (interact)
		{
			// Calculate the distance between the L2PcInstance and the L2NpcInstance
			if (!isInsideRadius(player, 150, false, false))
			{
				// Notify the L2PcInstance AI with AI_INTENTION_INTERACT
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
			}
			else
			{
				showMessageWindow(player);
			}
		}
		// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		final StringTokenizer st = new StringTokenizer(command, " ");
		final String actualCommand = st.nextToken(); // Get actual command
		
		if (actualCommand.equalsIgnoreCase("Buy"))
		{
			if (st.countTokens() < 1)
			{
				return;
			}
			
			final int val = Integer.parseInt(st.nextToken());
			showBuyWindow(player, val);
		}
		else if (actualCommand.equalsIgnoreCase("Sell"))
		{
			showSellWindow(player);
		}
	}
	
	protected final void showBuyWindow(L2PcInstance player, int val)
	{
		final L2BuyList buyList = BuyListData.getInstance().getBuyList(val);
		if (buyList == null)
		{
			_log.warning("BuyList not found! BuyListId:" + val);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (!buyList.isNpcAllowed(getId()))
		{
			_log.warning("Npc not allowed in BuyList! BuyListId:" + val + " NpcId:" + getId());
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		double taxRate = 0.50;
		
		player.setInventoryBlockingStatus(true);
		
		player.sendPacket(new BuyList(buyList, player.getAdena(), taxRate));
		player.sendPacket(new ExBuySellList(player, false));
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	protected final void showSellWindow(L2PcInstance player)
	{
		if (Config.DEBUG)
		{
			_log.fine("Showing selllist");
		}
		
		player.sendPacket(new SellList(player));
		
		if (Config.DEBUG)
		{
			_log.fine("Showing sell window");
		}
		
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	private void showMessageWindow(L2PcInstance player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		final String filename = "data/html/merchant/" + getId() + ".htm";
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(player.getHtmlPrefix(), filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);
	}
}
