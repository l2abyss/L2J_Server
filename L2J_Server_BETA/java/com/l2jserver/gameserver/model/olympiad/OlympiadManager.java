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
package com.l2jserver.gameserver.model.olympiad;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javolution.util.FastList;
import javolution.util.FastMap;

import com.l2jserver.Config;
import com.l2jserver.gameserver.ThreadPoolManager;
import com.l2jserver.gameserver.instancemanager.AntiFeedManager;
import com.l2jserver.gameserver.model.L2Party;
import com.l2jserver.gameserver.model.L2World;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.entity.TvTEvent;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.NpcHtmlMessage;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;
import com.l2jserver.gameserver.templates.StatsSet;

/**
 * 
 * @author DS
 *
 */
public class OlympiadManager
{
	private List<Integer> _nonClassBasedRegisters;
	private Map<Integer, List<Integer>> _classBasedRegisters;
	private List<List<Integer>> _teamsBasedRegisters;

	private OlympiadManager()
	{
		_nonClassBasedRegisters = new FastList<Integer>().shared();
		_classBasedRegisters = new FastMap<Integer, List<Integer>>().shared();
		_teamsBasedRegisters = new FastList<List<Integer>>().shared();
	}

	public static final OlympiadManager getInstance()
	{
		return SingletonHolder._instance;
	}

	public final List<Integer> getRegisteredNonClassBased()
	{
		return _nonClassBasedRegisters;
	}
	
	public final Map<Integer, List<Integer>> getRegisteredClassBased()
	{
		return _classBasedRegisters;
	}
	
	public final List<List<Integer>> getRegisteredTeamsBased()
	{
		return _teamsBasedRegisters;
	}
	
	protected final List<List<Integer>> hasEnoughRegisteredClassed()
	{
		List<List<Integer>> result = null;
		for (Map.Entry<Integer, List<Integer>> classList : _classBasedRegisters.entrySet())
		{
			if (classList.getValue() != null && classList.getValue().size() >= Config.ALT_OLY_CLASSED)
			{
				if (result == null)
					result = new FastList<List<Integer>>();

				result.add(classList.getValue());
			}
		}
		return result;
	}
	
	protected final boolean hasEnoughRegisteredNonClassed()
	{
		return _nonClassBasedRegisters.size() >= Config.ALT_OLY_NONCLASSED;
	}
	
	protected final boolean hasEnoughRegisteredTeams()
	{
		return _teamsBasedRegisters.size() >= Config.ALT_OLY_TEAMS;
	}
	
	protected final void clearRegistered()
	{
		_nonClassBasedRegisters.clear();
		_classBasedRegisters.clear();
		_teamsBasedRegisters.clear();
		AntiFeedManager.getInstance().clear(AntiFeedManager.OLYMPIAD_ID);
	}
	
	public final boolean isRegistered(L2PcInstance noble)
	{
		return isRegistered(noble, noble, false);
	}

	private final boolean isRegistered(L2PcInstance noble, L2PcInstance player, boolean showMessage)
	{
		final Integer objId = Integer.valueOf(noble.getObjectId());
		// party may be already dispersed
		for (List<Integer> team : _teamsBasedRegisters)
		{
			if (team != null && team.contains(objId))
			{
				if (showMessage)
				{
					SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_IS_ALREADY_REGISTERED_NON_CLASS_LIMITED_EVENT_TEAMS);
					sm.addPcName(noble);
					player.sendPacket(sm);
				}
				return true;
			}
		}

		if (_nonClassBasedRegisters.contains(objId))
		{
			if (showMessage)
			{
				final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_IS_ALREADY_REGISTERED_ON_THE_NON_CLASS_LIMITED_MATCH_WAITING_LIST);
				sm.addPcName(noble);
				player.sendPacket(sm);
			}
			return true;
		}
		
		final List<Integer> classed = _classBasedRegisters.get(noble.getBaseClass());
		if (classed != null && classed.contains(objId))
		{
			if (showMessage)
			{
				final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_IS_ALREADY_REGISTERED_ON_THE_CLASS_MATCH_WAITING_LIST);
				sm.addPcName(noble);
				player.sendPacket(sm);
			}
			return true;
		}
		
		return false;
	}

	public final boolean isRegisteredInComp(L2PcInstance noble)
	{
		return isRegistered(noble, noble, false) || isInCompetition(noble, noble, false);
	}

	private final boolean isInCompetition(L2PcInstance noble, L2PcInstance player, boolean showMessage)
	{
		if (!Olympiad._inCompPeriod)
			return false;

		AbstractOlympiadGame game;
		for (int i = OlympiadGameManager.getInstance().getNumberOfStadiums(); --i >=0;)
		{
			game = OlympiadGameManager.getInstance().getOlympiadTask(i).getGame();
			if (game == null)
				continue;
			
			if (game.containsParticipant(noble.getObjectId()))
			{
				if (!showMessage)
					return true;

				switch (game.getType())
				{
					case CLASSED:
					{
						final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_IS_ALREADY_REGISTERED_ON_THE_CLASS_MATCH_WAITING_LIST);
						sm.addPcName(noble);
						player.sendPacket(sm);
						break;
					}
					case NON_CLASSED:
					{
						final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_IS_ALREADY_REGISTERED_ON_THE_NON_CLASS_LIMITED_MATCH_WAITING_LIST);
						sm.addPcName(noble);
						player.sendPacket(sm);
						break;
					}
					case TEAMS:
					{
						final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.C1_IS_ALREADY_REGISTERED_NON_CLASS_LIMITED_EVENT_TEAMS);
						sm.addPcName(noble);
						player.sendPacket(sm);
						break;
					}
				}
				return true;
			}
		}
		return false;
	}

	public final boolean registerNoble(L2PcInstance player, CompetitionType type)
	{
		SystemMessage sm;
		if (!Olympiad._inCompPeriod)
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.THE_OLYMPIAD_GAME_IS_NOT_CURRENTLY_IN_PROGRESS);
			player.sendPacket(sm);
			return false;
		}
		
		if (Olympiad.getInstance().getMillisToCompEnd() < 600000)
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.GAME_REQUEST_CANNOT_BE_MADE);
			player.sendPacket(sm);
			return false;
		}
		
		switch (type)
		{
			case CLASSED:
			{
				if (!checkNoble(player, player))
					return false;

				List<Integer> classed = _classBasedRegisters.get(player.getBaseClass());
				if (classed != null)
					classed.add(player.getObjectId());
				else
				{
					classed = new FastList<Integer>().shared();
					classed.add(player.getObjectId());
					_classBasedRegisters.put(player.getBaseClass(), classed);
				}

				sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_HAVE_BEEN_REGISTERED_IN_A_WAITING_LIST_OF_CLASSIFIED_GAMES);
				player.sendPacket(sm);
				break;
			}
			case NON_CLASSED:
			{
				if (!checkNoble(player, player))
					return false;

				_nonClassBasedRegisters.add(player.getObjectId());
				sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_HAVE_BEEN_REGISTERED_IN_A_WAITING_LIST_OF_NO_CLASS_GAMES);
				player.sendPacket(sm);
				break;
			}
			case TEAMS:
			{
				final L2Party party = player.getParty();
				if (party == null || party.getMemberCount() != 3)
				{
					sm = SystemMessage.getSystemMessage(SystemMessageId.PARTY_REQUIREMENTS_NOT_MET);
					player.sendPacket(sm);
					return false;
				}
				if (!party.isLeader(player))
				{
					sm = SystemMessage.getSystemMessage(SystemMessageId.ONLY_PARTY_LEADER_CAN_REQUEST_TEAM_MATCH);
					player.sendPacket(sm);
					return false;
				}

				int teamPoints = 0;
				ArrayList<Integer> team = new ArrayList<Integer>(party.getMemberCount());
				for (L2PcInstance noble : party.getPartyMembers())
				{
					if (!checkNoble(noble, player))
					{
						// remove previously registered party members
						if (Config.L2JMOD_DUALBOX_CHECK_MAX_OLYMPIAD_PARTICIPANTS_PER_IP > 0)
						{
							for (L2PcInstance unreg : party.getPartyMembers())
							{
								if (unreg == noble)
									break;

								AntiFeedManager.getInstance().removePlayer(AntiFeedManager.OLYMPIAD_ID, unreg);		
							}
						}						
						return false;
					}
					team.add(noble.getObjectId());
					teamPoints += Olympiad.getInstance().getNoblePoints(noble.getObjectId());
				}
				if (teamPoints < 10)
				{
					// TODO: replace with retail message
					player.sendMessage("Your team must have at least 10 points in total.");
					// remove previously registered party members
					if (Config.L2JMOD_DUALBOX_CHECK_MAX_OLYMPIAD_PARTICIPANTS_PER_IP > 0)
					{
						for (L2PcInstance unreg : party.getPartyMembers())
							AntiFeedManager.getInstance().removePlayer(AntiFeedManager.OLYMPIAD_ID, unreg);		
					}						
					return false;
				}

				party.broadcastToPartyMembers(SystemMessage.getSystemMessage(SystemMessageId.YOU_HAVE_REGISTERED_IN_A_WAITING_LIST_OF_TEAM_GAMES));
				_teamsBasedRegisters.add(team);
				break;
			}
		}
		return true;
	}
	
	public final boolean unRegisterNoble(L2PcInstance noble)
	{
		SystemMessage sm;
		if (!Olympiad._inCompPeriod)
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.THE_OLYMPIAD_GAME_IS_NOT_CURRENTLY_IN_PROGRESS);
			noble.sendPacket(sm);
			return false;
		}
		
		if (!noble.isNoble())
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.C1_DOES_NOT_MEET_REQUIREMENTS_ONLY_NOBLESS_CAN_PARTICIPATE_IN_THE_OLYMPIAD);
			sm.addString(noble.getName());
			noble.sendPacket(sm);
			return false;
		}
		
		if (!isRegistered(noble, noble, false))
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_HAVE_NOT_BEEN_REGISTERED_IN_A_WAITING_LIST_OF_A_GAME);
			noble.sendPacket(sm);
			return false;
		}

		if (isInCompetition(noble, noble, false))
			return false;

		sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_HAVE_BEEN_DELETED_FROM_THE_WAITING_LIST_OF_A_GAME);
		Integer objId = Integer.valueOf(noble.getObjectId());
		if (_nonClassBasedRegisters.remove(objId))
		{
			if (Config.L2JMOD_DUALBOX_CHECK_MAX_OLYMPIAD_PARTICIPANTS_PER_IP > 0)
				AntiFeedManager.getInstance().removePlayer(AntiFeedManager.OLYMPIAD_ID, noble);		

			noble.sendPacket(sm);
			return true;
		}

		final List<Integer> classed = _classBasedRegisters.get(noble.getBaseClass());
		if (classed != null && classed.remove(objId))
		{
			_classBasedRegisters.remove(noble.getBaseClass());
			_classBasedRegisters.put(noble.getBaseClass(), classed);

			if (Config.L2JMOD_DUALBOX_CHECK_MAX_OLYMPIAD_PARTICIPANTS_PER_IP > 0)
				AntiFeedManager.getInstance().removePlayer(AntiFeedManager.OLYMPIAD_ID, noble);		

			noble.sendPacket(sm);
			return true;
		}

		for (List<Integer> team : _teamsBasedRegisters)
		{
			if (team != null && team.contains(objId))
			{
				_teamsBasedRegisters.remove(team);
				ThreadPoolManager.getInstance().executeTask(new AnnounceUnregToTeam(team));
				return true;
			}
		}
		return false;
	}
	
	public final void removeDisconnectedCompetitor(L2PcInstance player)
	{
		final OlympiadGameTask task = OlympiadGameManager.getInstance().getOlympiadTask(player.getOlympiadGameId());
		if (task != null && task.isGameStarted())
			task.getGame().handleDisconnect(player);

		final Integer objId = Integer.valueOf(player.getObjectId());
		if (_nonClassBasedRegisters.remove(objId))
			return;

		final List<Integer> classed = _classBasedRegisters.get(player.getBaseClass());
		if (classed != null && classed.remove(objId))
			return;

		for (List<Integer> team : _teamsBasedRegisters)
		{
			if (team != null && team.contains(objId))
			{
				_teamsBasedRegisters.remove(team);
				ThreadPoolManager.getInstance().executeTask(new AnnounceUnregToTeam(team));
				return;
			}
		}
	}

	/**
	 * @param noble - checked noble
	 * @param player - messages will be sent to this L2PcInstance
	 * @return true if all requirements are met
	 */
	// TODO: move to the bypass handler after reworking points system
	private final boolean checkNoble(L2PcInstance noble, L2PcInstance player)
	{
		SystemMessage sm;
		if (!noble.isNoble())
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.C1_DOES_NOT_MEET_REQUIREMENTS_ONLY_NOBLESS_CAN_PARTICIPATE_IN_THE_OLYMPIAD);
			sm.addPcName(noble);
			player.sendPacket(sm);
			return false;
		}
		
		if (noble.isSubClassActive())
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.C1_CANT_JOIN_THE_OLYMPIAD_WITH_A_SUB_CLASS_CHARACTER);
			sm.addPcName(noble);
			player.sendPacket(sm);
			return false;
		}

		if (noble.isCursedWeaponEquipped())
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.C1_CANNOT_JOIN_OLYMPIAD_POSSESSING_S2);
			sm.addPcName(noble);
			sm.addItemName(noble.getCursedWeaponEquippedId());
			player.sendPacket(sm);
			return false;
		}

		if (!noble.isInventoryUnder80(true))
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.C1_CANNOT_PARTICIPATE_IN_OLYMPIAD_INVENTORY_SLOT_EXCEEDS_80_PERCENT);
			sm.addPcName(noble);
			player.sendPacket(sm);
			return false;
		}

		if (TvTEvent.isPlayerParticipant(noble.getObjectId()))
		{
			player.sendMessage("You can't join olympiad while participating on TvT Event.");
			return false;
		}

		if (isRegistered(noble, player, true))
			return false;

		if (isInCompetition(noble, player, true))
			return false;

		StatsSet statDat = Olympiad.getNobleStats(noble.getObjectId());
		if (statDat == null)
		{
			statDat = new StatsSet();
			statDat.set(Olympiad.CLASS_ID, noble.getBaseClass());
			statDat.set(Olympiad.CHAR_NAME, noble.getName());
			statDat.set(Olympiad.POINTS, Olympiad.DEFAULT_POINTS);
			statDat.set(Olympiad.COMP_DONE, 0);
			statDat.set(Olympiad.COMP_WON, 0);
			statDat.set(Olympiad.COMP_LOST, 0);
			statDat.set(Olympiad.COMP_DRAWN, 0);
			statDat.set("to_save", true);
			Olympiad.updateNobleStats(noble.getObjectId(), statDat);
		}

		final int points = Olympiad.getInstance().getNoblePoints(noble.getObjectId());
		if (points <= 0)
		{
			NpcHtmlMessage message = new NpcHtmlMessage(0);
			message.setFile(player.getHtmlPrefix(), "data/html/olympiad/noble_nopoints1.htm");
			message.replace("%objectId%", String.valueOf(noble.getTargetId()));
			player.sendPacket(message);
			return false;
		}

		if (Config.L2JMOD_DUALBOX_CHECK_MAX_OLYMPIAD_PARTICIPANTS_PER_IP > 0
				&& !AntiFeedManager.getInstance().tryAddPlayer(AntiFeedManager.OLYMPIAD_ID, noble, Config.L2JMOD_DUALBOX_CHECK_MAX_OLYMPIAD_PARTICIPANTS_PER_IP))
		{
			NpcHtmlMessage message = new NpcHtmlMessage(0);
			message.setFile(player.getHtmlPrefix(), "data/html/mods/OlympiadIPRestriction.htm");
			message.replace("%max%", String.valueOf(AntiFeedManager.getInstance().getLimit(player, Config.L2JMOD_DUALBOX_CHECK_MAX_OLYMPIAD_PARTICIPANTS_PER_IP)));
			player.sendPacket(message);
			return false;
		}

		return true;
	}
	
	private static final class AnnounceUnregToTeam implements Runnable
	{
		private final List<Integer> _team;
		
		public AnnounceUnregToTeam(List<Integer> t)
		{
			_team = t;
		}
		
		public final void run()
		{
			L2PcInstance teamMember;
			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_HAVE_BEEN_DELETED_FROM_THE_WAITING_LIST_OF_A_GAME);
			for (int objectId : _team)
			{
				teamMember = L2World.getInstance().getPlayer(objectId);
				if (teamMember != null)
				{
					teamMember.sendPacket(sm);
					if (Config.L2JMOD_DUALBOX_CHECK_MAX_OLYMPIAD_PARTICIPANTS_PER_IP > 0)
						AntiFeedManager.getInstance().removePlayer(AntiFeedManager.OLYMPIAD_ID, teamMember);		
				}
			}
			teamMember = null;
		}
	}

	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final OlympiadManager _instance = new OlympiadManager();
	}
}