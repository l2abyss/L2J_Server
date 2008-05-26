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
package net.sf.l2j.gameserver.handler.admincommandhandlers;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.GMAudit;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.TvTEvent;
import net.sf.l2j.gameserver.model.entity.TvTEventTeleporter;

/**
 * @author FBIagent
 */
public class AdminTvTEvent
implements IAdminCommandHandler {
	private static final String[] ADMIN_COMMANDS = {"admin_tvt_add", "admin_tvt_remove"};

	public boolean useAdminCommand( String command, L2PcInstance adminInstance )
	{
		GMAudit.auditGMAction( adminInstance.getName(), command, ( adminInstance.getTarget() != null ? adminInstance.getTarget().getName() : "no-target" ), "" );

		if ( command.equals( "admin_tvt_add" ) ) {
			L2Object target = adminInstance.getTarget();

			if (!( target instanceof L2PcInstance ) )
			{
				adminInstance.sendMessage( "You should select a player!" );
				return true;
			}

			add( adminInstance, ( L2PcInstance )target );
		}
		else if ( command.equals( "admin_tvt_remove" ) )
		{
			L2Object target = adminInstance.getTarget();

			if (!( target instanceof L2PcInstance ) )
			{
				adminInstance.sendMessage( "You should select a player!" );
				return true;
			}

			remove( adminInstance, ( L2PcInstance )target );
		}

		return true;
	}

	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}

	private void add( L2PcInstance adminInstance, L2PcInstance playerInstance ) {
		if ( TvTEvent.isPlayerParticipant( playerInstance.getObjectId() ) ) {
			adminInstance.sendMessage( "Player already participated in the event!" );
			return;
		}

		if ( !TvTEvent.addParticipant( playerInstance ) ) {
			adminInstance.sendMessage( "Player instance could not be added, it seems to be null!" );
			return;
		}

		if ( TvTEvent.isStarted() ) {
			new TvTEventTeleporter( playerInstance, TvTEvent.getParticipantTeamCoordinates( playerInstance.getObjectId() ), true, false );
		}
	}

	private void remove( L2PcInstance adminInstance, L2PcInstance playerInstance ) {
		if ( !TvTEvent.removeParticipant( playerInstance.getObjectId() ) ) {
			adminInstance.sendMessage( "Player is not part of the event!" );
			return;
		}

		new TvTEventTeleporter( playerInstance, Config.TVT_EVENT_PARTICIPATION_NPC_COORDINATES, true, true );
	}
}
