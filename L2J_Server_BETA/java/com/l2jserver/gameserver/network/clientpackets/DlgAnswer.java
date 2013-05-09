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
package com.l2jserver.gameserver.network.clientpackets;

import java.util.List;

import javolution.util.FastList;

import com.l2jserver.Config;
import com.l2jserver.gameserver.datatables.AdminTable;
import com.l2jserver.gameserver.handler.AdminCommandHandler;
import com.l2jserver.gameserver.handler.IAdminCommandHandler;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.holders.DoorRequestHolder;
import com.l2jserver.gameserver.model.holders.SummonRequestHolder;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.scripting.scriptengine.events.DlgAnswerEvent;
import com.l2jserver.gameserver.scripting.scriptengine.listeners.talk.DlgAnswerListener;
import com.l2jserver.gameserver.util.GMAudit;

/**
 * @author Dezmond_snz Format: cddd
 */
public final class DlgAnswer extends L2GameClientPacket
{
	private static final String _C__C6_DLGANSWER = "[C] C6 DlgAnswer";
	private static final List<DlgAnswerListener> _listeners = new FastList<DlgAnswerListener>().shared();
	private int _messageId;
	private int _answer;
	private int _requesterId;
	
	@Override
	protected void readImpl()
	{
		_messageId = readD();
		_answer = readD();
		_requesterId = readD();
	}
	
	@Override
	public void runImpl()
	{
		final L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		if (_messageId == SystemMessageId.S1.getId())
		{
			String _command = activeChar.getAdminConfirmCmd();
			if (_command == null)
			{
				if (Config.L2JMOD_ALLOW_WEDDING)
				{
					activeChar.engageAnswer(_answer);
				}
			}
			else
			{
				activeChar.setAdminConfirmCmd(null);
				if (_answer == 0)
				{
					return;
				}
				String command = _command.split(" ")[0];
				IAdminCommandHandler ach = AdminCommandHandler.getInstance().getHandler(command);
				if (AdminTable.getInstance().hasAccess(command, activeChar.getAccessLevel()))
				{
					if (Config.GMAUDIT)
					{
						GMAudit.auditGMAction(activeChar.getName() + " [" + activeChar.getObjectId() + "]", _command, (activeChar.getTarget() != null ? activeChar.getTarget().getName() : "no-target"));
					}
					ach.useAdminCommand(_command, activeChar);
				}
			}
		}
		else if ((_messageId == SystemMessageId.RESSURECTION_REQUEST_BY_C1_FOR_S2_XP.getId()) || (_messageId == SystemMessageId.RESURRECT_USING_CHARM_OF_COURAGE.getId()))
		{
			activeChar.reviveAnswer(_answer);
		}
		else if (_messageId == SystemMessageId.C1_WISHES_TO_SUMMON_YOU_FROM_S2_DO_YOU_ACCEPT.getId())
		{
			final SummonRequestHolder holder = activeChar.getScript(SummonRequestHolder.class);
			if ((_answer == 1) && (holder != null) && (holder.getTarget().getObjectId() == _requesterId))
			{
				L2PcInstance.teleToTarget(activeChar, holder.getTarget(), holder.getSkill());
			}
			activeChar.removeScript(SummonRequestHolder.class);
		}
		else if (_messageId == SystemMessageId.WOULD_YOU_LIKE_TO_OPEN_THE_GATE.getId())
		{
			final DoorRequestHolder holder = activeChar.getScript(DoorRequestHolder.class);
			if ((holder != null) && (holder.getDoor() == activeChar.getTarget()) && (_answer == 1))
			{
				holder.getDoor().openMe();
			}
			activeChar.removeScript(DoorRequestHolder.class);
		}
		else if (_messageId == SystemMessageId.WOULD_YOU_LIKE_TO_CLOSE_THE_GATE.getId())
		{
			final DoorRequestHolder holder = activeChar.getScript(DoorRequestHolder.class);
			if ((holder != null) && (holder.getDoor() == activeChar.getTarget()) && (_answer == 1))
			{
				holder.getDoor().closeMe();
			}
			activeChar.removeScript(DoorRequestHolder.class);
		}
		
		fireDlgAnswerListener();
	}
	
	/**
	 * Fires the event when packet arrived.
	 */
	private void fireDlgAnswerListener()
	{
		DlgAnswerEvent event = new DlgAnswerEvent();
		event.setActiveChar(getActiveChar());
		event.setMessageId(_messageId);
		event.setAnswer(_answer);
		event.setRequesterId(_requesterId);
		
		for (DlgAnswerListener listener : _listeners)
		{
			if ((listener.getMessageId() == -1) || (_messageId == listener.getMessageId()))
			{
				listener.onDlgAnswer(event);
			}
		}
	}
	
	/**
	 * @param listener
	 */
	public static void addDlgAnswerListener(DlgAnswerListener listener)
	{
		if (!_listeners.contains(listener))
		{
			_listeners.add(listener);
		}
	}
	
	/**
	 * @param listener
	 */
	public static void removeDlgAnswerListener(DlgAnswerListener listener)
	{
		if (_listeners.contains(listener))
		{
			_listeners.remove(listener);
		}
	}
	
	@Override
	public String getType()
	{
		return _C__C6_DLGANSWER;
	}
}
