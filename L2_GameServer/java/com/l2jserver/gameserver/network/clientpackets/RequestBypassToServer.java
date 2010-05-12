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

import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2jserver.Config;
import com.l2jserver.gameserver.ai.CtrlIntention;
import com.l2jserver.gameserver.communitybbs.CommunityBoard;
import com.l2jserver.gameserver.datatables.AdminCommandAccessRights;
import com.l2jserver.gameserver.handler.AdminCommandHandler;
import com.l2jserver.gameserver.handler.IAdminCommandHandler;
import com.l2jserver.gameserver.handler.IVoicedCommandHandler;
import com.l2jserver.gameserver.handler.VoicedCommandHandler;
import com.l2jserver.gameserver.model.L2CharPosition;
import com.l2jserver.gameserver.model.L2Object;
import com.l2jserver.gameserver.model.L2World;
import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.model.actor.instance.L2CastleChamberlainInstance;
import com.l2jserver.gameserver.model.actor.instance.L2ManorManagerInstance;
import com.l2jserver.gameserver.model.actor.instance.L2MerchantSummonInstance;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.entity.L2Event;
import com.l2jserver.gameserver.model.olympiad.Olympiad;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.communityserver.CommunityServerThread;
import com.l2jserver.gameserver.network.communityserver.writepackets.RequestShowCommunityBoard;
import com.l2jserver.gameserver.network.serverpackets.ActionFailed;
import com.l2jserver.gameserver.network.serverpackets.NpcHtmlMessage;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;
import com.l2jserver.gameserver.util.GMAudit;


/**
 * This class ...
 *
 * @version $Revision: 1.12.4.5 $ $Date: 2005/04/11 10:06:11 $
 */
public final class RequestBypassToServer extends L2GameClientPacket
{
	private static final String _C__21_REQUESTBYPASSTOSERVER = "[C] 21 RequestBypassToServer";
	private static Logger _log = Logger.getLogger(RequestBypassToServer.class.getName());

	// S
	private String _command;

	@Override
	protected void readImpl()
	{
		_command = readS();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();

		if (activeChar == null)
			return;
		
		if (!activeChar.getFloodProtectors().getServerBypass().tryPerformAction(_command))
			return;

		try
		{
			if (_command.startsWith("admin_")) //&& activeChar.getAccessLevel() >= Config.GM_ACCESSLEVEL)
			{
				String command = _command.split(" ")[0];

				IAdminCommandHandler ach = AdminCommandHandler.getInstance().getAdminCommandHandler(command);
				
				if (ach == null)
				{
					if ( activeChar.isGM() )
						activeChar.sendMessage("The command " + command.substring(6) + " does not exist!");

					_log.warning("No handler registered for admin command '" + command + "'");
					return;
				}

				if (!AdminCommandAccessRights.getInstance().hasAccess(command, activeChar.getAccessLevel()))
				{
					activeChar.sendMessage("You don't have the access rights to use this command!");
					_log.warning("Character " + activeChar.getName() + " tried to use admin command " + command + ", without proper access level!");
					return;
				}
				if (Config.GMAUDIT)
					GMAudit.auditGMAction(activeChar.getName()+" ["+activeChar.getObjectId()+"]", _command, (activeChar.getTarget() != null?activeChar.getTarget().getName():"no-target"));

				ach.useAdminCommand(_command, activeChar);
			}
			else if (_command.equals("come_here") && ( activeChar.isGM()))
			{
				comeHere(activeChar);
			}
			else if (_command.startsWith("player_help "))
			{
				playerHelp(activeChar, _command.substring(12));
			}
			else if (_command.startsWith("npc_"))
			{
				if(!activeChar.validateBypass(_command))
					return;

				int endOfId = _command.indexOf('_', 5);
				String id;
				if (endOfId > 0)
					id = _command.substring(4, endOfId);
				else
					id = _command.substring(4);
				try
				{
					L2Object object = L2World.getInstance().findObject(Integer.parseInt(id));

					if (_command.substring(endOfId+1).startsWith("event_participate"))
						L2Event.inscribePlayer(activeChar);
					else if (object instanceof L2Npc && endOfId > 0 && activeChar.isInsideRadius(object, L2Npc.INTERACTION_DISTANCE, false, false))
						((L2Npc)object).onBypassFeedback(activeChar, _command.substring(endOfId+1));

					activeChar.sendPacket(ActionFailed.STATIC_PACKET);
				}
				catch (NumberFormatException nfe) {}
			}
			else if (_command.startsWith("summon_"))
			{
				if(!activeChar.validateBypass(_command))
					return;

				int endOfId = _command.indexOf('_', 8);
				String id;
				if (endOfId > 0)
					id = _command.substring(7, endOfId);
				else
					id = _command.substring(7);
				try
				{
					L2Object object = L2World.getInstance().findObject(Integer.parseInt(id));

					if (object instanceof L2MerchantSummonInstance && endOfId > 0 && activeChar.isInsideRadius(object, L2Npc.INTERACTION_DISTANCE, false, false))
						((L2MerchantSummonInstance)object).onBypassFeedback(activeChar, _command.substring(endOfId+1));

					activeChar.sendPacket(ActionFailed.STATIC_PACKET);
				}
				catch (NumberFormatException nfe) {}
			}
			// Navigate through Manor windows
			else if (_command.startsWith("manor_menu_select?"))
			{
				/*if(!activeChar.validateBypass(_command))
					return;*/
				
				L2Object object = activeChar.getLastFolkNPC();
				if ((object instanceof L2ManorManagerInstance || object instanceof L2CastleChamberlainInstance) 
						&& activeChar.isInsideRadius(object, L2Npc.INTERACTION_DISTANCE, false, false))
					((L2Npc) object).onBypassFeedback(activeChar, _command);
			}
			else if (_command.startsWith("bbs_"))
			{
				if (Config.ENABLE_COMMUNITY_BOARD)
				{
					if (!CommunityServerThread.getInstance().sendPacket(new RequestShowCommunityBoard(activeChar.getObjectId(), _command)))
						activeChar.sendPacket(new SystemMessage(SystemMessageId.CB_OFFLINE));
				}
				else
					CommunityBoard.getInstance().handleCommands(getClient(), _command);
			}
			else if (_command.startsWith("_bbsloc"))
			{
				CommunityBoard.getInstance().handleCommands(getClient(), _command);
			}
			else if (_command.startsWith("_bbs"))
			{
				if (Config.ENABLE_COMMUNITY_BOARD)
				{
					if (!CommunityServerThread.getInstance().sendPacket(new RequestShowCommunityBoard(activeChar.getObjectId(), _command)))
						activeChar.sendPacket(new SystemMessage(SystemMessageId.CB_OFFLINE));
				}
				else
					CommunityBoard.getInstance().handleCommands(getClient(), _command);
			}
			else if (_command.startsWith("_mail"))
			{
				if (!CommunityServerThread.getInstance().sendPacket(new RequestShowCommunityBoard(activeChar.getObjectId(), "_bbsmail")))
					activeChar.sendPacket(new SystemMessage(SystemMessageId.CB_OFFLINE));
			}
			else if (_command.startsWith("_friend"))
			{
				if (!CommunityServerThread.getInstance().sendPacket(new RequestShowCommunityBoard(activeChar.getObjectId(), "_bbsfriend")))
					activeChar.sendPacket(new SystemMessage(SystemMessageId.CB_OFFLINE));
			}
			else if (_command.startsWith("Quest "))
			{
				if(!activeChar.validateBypass(_command))
					return;

				L2PcInstance player = getClient().getActiveChar();
				if (player == null) return;

				String p = _command.substring(6).trim();
				int idx = p.indexOf(' ');
				if (idx < 0)
					player.processQuestEvent(p, "");
				else
					player.processQuestEvent(p.substring(0, idx), p.substring(idx).trim());
			}
			else if (_command.startsWith("OlympiadArenaChange"))
			{
				Olympiad.bypassChangeArena(_command, activeChar);
			}
			else if (_command.startsWith("voice "))
			{
				// only voice commands allowed in bypass
				if (_command.length() > 7
						&& _command.charAt(6) == '.')
				{
					final String vc, vparams;
					int endOfCommand = _command.indexOf(" ", 7);
					if (endOfCommand > 0)
					{
						vc = _command.substring(7, endOfCommand).trim();
						vparams = _command.substring(endOfCommand).trim();
					}
					else
					{
						vc = _command.substring(7).trim();
						vparams = null;
					}

					if (vc.length() > 0)
					{
						IVoicedCommandHandler vch = VoicedCommandHandler.getInstance().getVoicedCommandHandler(vc);
						if (vch != null)
							vch.useVoicedCommand(vc, activeChar, vparams);
					}
				}
			}
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, getClient()+" sent bad RequestBypassToServer: \""+_command+"\"", e);
			if (activeChar.isGM())
			{
				StringBuilder sb = new StringBuilder(200);
				sb.append("<html><body>");
				sb.append("Bypass error: "+e+"<br1>");
				sb.append("Bypass command: "+_command+"<br1>");
				sb.append("StackTrace:<br1>");
				for (StackTraceElement ste : e.getStackTrace())
					sb.append(ste.toString()+"<br1>");
				sb.append("</body></html>");
				// item html
				NpcHtmlMessage msg = new NpcHtmlMessage(0,12807);
				msg.setHtml(sb.toString());
				msg.disableValidation();
				activeChar.sendPacket(msg);
			}
		}
	}

	/**
	 * @param client
	 */
	private void comeHere(L2PcInstance activeChar)
	{
		L2Object obj = activeChar.getTarget();
		if (obj == null) return;
		if (obj instanceof L2Npc)
		{
			L2Npc temp = (L2Npc) obj;
			temp.setTarget(activeChar);
			temp.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition(activeChar.getX(),activeChar.getY(), activeChar.getZ(), 0 ));
		}
	}

	private void playerHelp(L2PcInstance activeChar, String path)
	{
		if (path.indexOf("..") != -1)
			return;
		
		StringTokenizer st = new StringTokenizer(path);
		String[] cmd = st.nextToken().split("#");
		
		if (cmd.length > 1)
		{
			int itemId = 0;
			itemId = Integer.parseInt(cmd[1]);
			String filename = "data/html/help/"+cmd[0];
			NpcHtmlMessage html = new NpcHtmlMessage(1,itemId);
			html.setFile(activeChar.getHtmlPrefix(), filename);
			html.disableValidation();
			activeChar.sendPacket(html);
		}
		else
		{
			String filename = "data/html/help/"+path;
			NpcHtmlMessage html = new NpcHtmlMessage(1);
			html.setFile(activeChar.getHtmlPrefix(), filename);
			html.disableValidation();
			activeChar.sendPacket(html);
		}
	}

	/* (non-Javadoc)
	 * @see com.l2jserver.gameserver.clientpackets.ClientBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _C__21_REQUESTBYPASSTOSERVER;
	}
}
