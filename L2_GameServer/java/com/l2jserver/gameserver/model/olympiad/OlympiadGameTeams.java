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
import java.util.logging.Level;

import com.l2jserver.Config;
import com.l2jserver.gameserver.model.L2World;
import com.l2jserver.gameserver.model.Location;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.zone.type.L2OlympiadStadiumZone;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.ExOlympiadUserInfo;
import com.l2jserver.gameserver.network.serverpackets.L2GameServerPacket;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;
import com.l2jserver.util.Rnd;

/**
 * 
 * @author Pere, DS
 */
class OlympiadGameTeams extends AbstractOlympiadGame
{
	public static final int TEAM_SIZE = 3;

	protected boolean _teamOneDefaulted;
	protected boolean _teamTwoDefaulted;
	
	protected int _damageT1 = 0;
	protected int _damageT2 = 0;

	protected Participant[] _teamOne;
	protected Participant[] _teamTwo;

	private OlympiadGameTeams(int id, Participant[] teamOne, Participant[] teamTwo)
	{
		super(id);

		_teamOne = teamOne;
		_teamTwo = teamTwo;

		Participant par;
		for (int i = 0; i < TEAM_SIZE; i++)
		{
			par = _teamOne[i];
			par.player.setOlympiadGameId(id);

			par = _teamTwo[i];
			par.player.setOlympiadGameId(id);
		}
	}
	
	protected static final OlympiadGameTeams createGame(int id, List<List<Integer>> list)
	{
		if (list == null || list.isEmpty() || list.size() < 2)
			return null;

		List<Integer> teamOne = null;
		List<Integer> teamTwo = null;
		L2PcInstance player;
		List<L2PcInstance> teamOnePlayers = new ArrayList<L2PcInstance>(TEAM_SIZE);
		List<L2PcInstance> teamTwoPlayers = new ArrayList<L2PcInstance>(TEAM_SIZE);

		while (list.size() > 1)
		{
			teamOne = list.remove(Rnd.nextInt(list.size()));

			if ((teamOne == null || teamOne.size() != TEAM_SIZE))
				continue;

			for (int objectId : teamOne)
			{
				player = L2World.getInstance().getPlayer(objectId);
				if (player == null || !player.isOnline())
				{
					teamOnePlayers.clear();
					break;
				}
				teamOnePlayers.add(player);
			}
			if (teamOnePlayers.isEmpty())
				continue;

			teamTwo = list.remove(Rnd.nextInt(list.size()));
			if (teamTwo == null || teamTwo.size() != TEAM_SIZE)
			{
				list.add(teamOne);
				teamOnePlayers.clear();
				continue;
			}

			for (int objectId : teamTwo)
			{
				player = L2World.getInstance().getPlayer(objectId);
				if (player == null || !player.isOnline())
				{
					teamTwoPlayers.clear();
					break;
				}
				teamTwoPlayers.add(player);
			}
			if (teamTwoPlayers.isEmpty())
			{
				list.add(teamOne);
				teamOnePlayers.clear();
				continue;
			}

			Participant[] t1 = new Participant[TEAM_SIZE];
			Participant[] t2 = new Participant[TEAM_SIZE];

			for (int i = 0; i < TEAM_SIZE; i++)
			{
				t1[i] = new Participant(teamOnePlayers.get(i), 1);
				t2[i] = new Participant(teamTwoPlayers.get(i), 2);
			}

			return new OlympiadGameTeams(id, t1, t2);
		}

		return null;
	}

	@Override
	public final CompetitionType getType()
	{
		return CompetitionType.TEAMS;
	}

	@Override
	protected final int getDivider()
	{
		return 5;
	}

	@Override
	protected final int[][] getReward()
	{
		return Config.ALT_OLY_TEAM_REWARD;
	}

	@Override
	public final boolean containsParticipant(int playerId)
	{
		for (int i = 0; i < TEAM_SIZE; i++)
		{
			if (_teamOne[i].objectId == playerId || _teamTwo[i].objectId == playerId)
				return true;
		}
		return false;
	}

	@Override
	public final void sendOlympiadInfo(L2Character player)
	{
		for (int i = 0; i < TEAM_SIZE; i++)
		{
			player.sendPacket(new ExOlympiadUserInfo(_teamOne[i]));
		}
		for (int i = 0; i < TEAM_SIZE; i++)
		{
			player.sendPacket(new ExOlympiadUserInfo(_teamTwo[i]));
		}
	}

	@Override
	public final void broadcastOlympiadInfo(L2OlympiadStadiumZone stadium)
	{
		for (int i = 0; i < TEAM_SIZE; i++)
		{
			stadium.broadcastPacket(new ExOlympiadUserInfo(_teamOne[i]));
		}
		for (int i = 0; i < TEAM_SIZE; i++)
		{
			stadium.broadcastPacket(new ExOlympiadUserInfo(_teamTwo[i]));
		}
	}

	@Override
	protected final void broadcastPacket(L2GameServerPacket packet)
	{
		Participant par;
		for (int i = 0; i < TEAM_SIZE; i++)
		{
			par = _teamOne[i];
			par.updatePlayer();
			if (par.player != null)
				par.player.sendPacket(packet);

			par = _teamTwo[i];
			par.updatePlayer();
			if (par.player != null)
				par.player.sendPacket(packet);
		}
	}

	@Override
	protected boolean needBuffers()
	{
		return false;
	}

	@Override
	protected final boolean portPlayersToArena(List<Location> spawns)
	{
		boolean result = true;
		try
		{
			final int offset = spawns.size() / 2;
			for (int i = 0; i < TEAM_SIZE; i++)
			{
				result &= portPlayerToArena(_teamOne[i], spawns.get(i), _stadiumID);
				result &= portPlayerToArena(_teamTwo[i], spawns.get(i + offset), _stadiumID);
			}
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "", e);
			return false;
		}
		return result;
	}

	@Override
	protected final void removals()
	{
		for (int i = 0; i < TEAM_SIZE; i++)
		{
			removals(_teamOne[i].player, false);
			removals(_teamTwo[i].player, false);
		}
	}

	@Override
	protected final boolean makeCompetitionStart()
	{
		if (!super.makeCompetitionStart())
			return false;

		Participant par;
		for (int i = 0; i < TEAM_SIZE; i++)
		{
			par = _teamOne[i];
			if (par.player == null)
				return false;

			par.player.setIsOlympiadStart(true);
			par.player.updateEffectIcons();

			par = _teamTwo[i];
			if (par.player == null)
				return false;

			par.player.setIsOlympiadStart(true);
			par.player.updateEffectIcons();
		}
		return true;
	}

	@Override
	protected final void cleanEffects()
	{
		Participant par;
		for (int i = 0; i < TEAM_SIZE; i++)
		{
			par = _teamOne[i];
			if (par.player != null
					&& !par.defaulted
					&& !par.disconnected
					&& par.player.getOlympiadGameId() == _stadiumID)
				cleanEffects(par.player);

			par = _teamTwo[i];
			if (par.player != null
					&& !par.defaulted
					&& !par.disconnected
					&& par.player.getOlympiadGameId() == _stadiumID)
				cleanEffects(par.player);
		}
	}

	@Override
	protected final void portPlayersBack()
	{
		Participant par;
		for (int i = 0; i < TEAM_SIZE; i++)
		{
			par = _teamOne[i];
			if (par.player != null
					&& !par.defaulted
					&& !par.disconnected)
				portPlayerBack(par.player);

			par = _teamTwo[i];
			if (par.player != null
					&& !par.defaulted
					&& !par.disconnected)
				portPlayerBack(par.player);
		}
	}

	@Override
	protected final void playersStatusBack()
	{
		Participant par;
		for (int i = 0; i < TEAM_SIZE; i++)
		{
			par = _teamOne[i];
			if (par.player != null
					&& !par.defaulted
					&& !par.disconnected
					&& par.player.getOlympiadGameId() == _stadiumID)
				playerStatusBack(par.player);

			par = _teamTwo[i];
			if (par.player != null
					&& !par.defaulted
					&& !par.disconnected
					&& par.player.getOlympiadGameId() == _stadiumID)
				playerStatusBack(par.player);
		}
	}

	@Override
	protected final void clearPlayers()
	{
		for (int i = 0; i < TEAM_SIZE; i++)
		{
			_teamOne[i].player = null;
			_teamOne[i] = null;
			_teamTwo[i].player = null;
			_teamTwo[i] = null;
		}
	}
	
	@Override
	protected final void handleDisconnect(L2PcInstance player)
	{
		Participant par;
		for (int i = 0; i < TEAM_SIZE; i++)
		{
			par = _teamOne[i];
			if (par.objectId == player.getObjectId())
			{
				par.disconnected = true;
				return;
			}

			par = _teamTwo[i];
			if (par.objectId == player.getObjectId())
			{
				par.disconnected = true;
				return;
			}
		}
	}
	
	@Override
	protected final boolean haveWinner()
	{
		if (!checkBattleStatus())
			return true;
		
		boolean teamOneLost = true;
		boolean teamTwoLost = true;
		Participant par;
		for (int i = 0; i < TEAM_SIZE; i++)
		{
			par = _teamOne[i];
			if (!par.disconnected)
			{
				if (par.player != null && par.player.getOlympiadGameId() == _stadiumID)
					teamOneLost &= par.player.isDead();
			}

			par = _teamTwo[i];
			if (!par.disconnected)
			{
				if (par.player != null && par.player.getOlympiadGameId() == _stadiumID)
					teamTwoLost &= par.player.isDead();
			}
		}
		
		return teamOneLost || teamTwoLost;
	}
	
	@Override
	protected final boolean checkBattleStatus()
	{
		if (_aborted)
			return false;		

		if (teamOneAllDisconnected())
			return false;

		if (teamTwoAllDisconnected())
			return false;

		return true;
	}

	@Override
	protected void validateWinner(L2OlympiadStadiumZone stadium)
	{
		if (_aborted)
			return;
		
		final boolean tOneCrash = teamOneAllDisconnected();
		final boolean tTwoCrash = teamTwoAllDisconnected();
		
		Participant par;
		SystemMessage sm;
		int points;

		// Check for if a team defaulted before battle started
		if (_teamOneDefaulted || _teamTwoDefaulted)
		{
			try
			{
				if (_teamOneDefaulted)
				{
					for (int i = 0 ; i < TEAM_SIZE; i++)
					{
						par = _teamOne[i];
						removePointsFromParticipant(par, Math.min(par.stats.getInteger(POINTS) / 3, Config.ALT_OLY_MAX_POINTS));
						par.updateNobleStats();
					}
				}
				if (_teamTwoDefaulted)
				{
					for (int i = 0 ; i < TEAM_SIZE; i++)
					{
						par = _teamTwo[i];
						removePointsFromParticipant(par, Math.min(par.stats.getInteger(POINTS) / 3, Config.ALT_OLY_MAX_POINTS));
						par.updateNobleStats();
					}
				}
			}
			catch (Exception e)
			{
				_log.log(Level.WARNING, "Exception on validateWinner(): " + e.getMessage(), e);
			}
			return;
		}
		
		 // points to be dedicted in case of losing
		final int[] pointsTeamOne = new int[TEAM_SIZE];
		final int[] pointsTeamTwo = new int[TEAM_SIZE];
		final int[] maxPointsTeamOne = new int[TEAM_SIZE];
		final int[] maxPointsTeamTwo = new int[TEAM_SIZE];
		int totalPointsTeamOne = 0;
		int totalPointsTeamTwo = 0;
		for (int i = 0; i < TEAM_SIZE; i++)
		{
			points = _teamOne[i].stats.getInteger(POINTS) / getDivider();
			if (points <= 0)
				points = 1;
			else if (points > Config.ALT_OLY_MAX_POINTS)
				points = Config.ALT_OLY_MAX_POINTS;

			totalPointsTeamOne += points;
			pointsTeamOne[i] = points;
			maxPointsTeamOne[i] = points;

			points = _teamTwo[i].stats.getInteger(POINTS) / getDivider();
			if (points <= 0)
				points = 1;
			else if (points > Config.ALT_OLY_MAX_POINTS)
				points = Config.ALT_OLY_MAX_POINTS;

			totalPointsTeamTwo += points;
			pointsTeamTwo[i] = points;
			maxPointsTeamTwo[i] = points;
		}

		// Choose minimum sum
		int min = Math.min(totalPointsTeamOne, totalPointsTeamTwo);

		// make sure all team members got same number of the points: round down to 3x
		min = (min / TEAM_SIZE) * TEAM_SIZE;

		// calculating coefficients and trying to correct total number of points for each team
		// due to rounding errors total points after correction will always be lower or equal
		// than needed minimal sum
		final double dividerOne = (double)totalPointsTeamOne / min;
		final double dividerTwo = (double)totalPointsTeamTwo / min;
		totalPointsTeamOne = min;
		totalPointsTeamTwo = min;
		for (int i = 0; i < TEAM_SIZE; i++)
		{
			points = Math.max((int)(pointsTeamOne[i] / dividerOne), 1);
			pointsTeamOne[i] = points;
			totalPointsTeamOne -= points;
			points = Math.max((int)(pointsTeamTwo[i] / dividerTwo), 1);
			pointsTeamTwo[i] = points;
			totalPointsTeamTwo -= points;
		}

		// compensating remaining points, first team from begin to end, second from end to begin
		for (int i = 0; totalPointsTeamOne > 0 && i < TEAM_SIZE; i++)
		{
			if (pointsTeamOne[i] < maxPointsTeamOne[i])
			{
				pointsTeamOne[i]++;
				totalPointsTeamOne--;
			}
		}

		for (int i = TEAM_SIZE; totalPointsTeamTwo > 0 && --i >= 0;)
		{
			if (pointsTeamTwo[i] < maxPointsTeamTwo[i])
			{
				pointsTeamTwo[i]++;
				totalPointsTeamTwo--;
			}
		}

		// Create results for players if a team crashed
		if (tOneCrash || tTwoCrash)
		{
			try
			{
				if (tTwoCrash && !tOneCrash)
				{
					sm = SystemMessage.getSystemMessage(SystemMessageId.C1_HAS_WON_THE_GAME);
					sm.addString(_teamOne[0].name);
					stadium.broadcastPacket(sm);

					for (int i = 0 ; i < TEAM_SIZE; i++)
					{
						par = _teamTwo[i];
						par.updateStat(COMP_LOST, 1);
						points = pointsTeamTwo[i];
						removePointsFromParticipant(par, points);
					}

					points = min / TEAM_SIZE;
					for (int i = 0 ; i < TEAM_SIZE; i++)
					{
						par = _teamOne[i];
						par.updateStat(COMP_WON, 1);
						addPointsToParticipant(par, points);
					}

					for (int i = 0 ; i < TEAM_SIZE; i++)
						rewardParticipant(_teamOne[i].player, getReward());
				}
				else if (tOneCrash && !tTwoCrash)
				{
					sm = SystemMessage.getSystemMessage(SystemMessageId.C1_HAS_WON_THE_GAME);
					sm.addString(_teamTwo[0].name);
					stadium.broadcastPacket(sm);

					for (int i = 0 ; i < TEAM_SIZE; i++)
					{
						par = _teamOne[i];
						par.updateStat(COMP_LOST, 1);
						points = pointsTeamOne[i];
						removePointsFromParticipant(par, points);
					}
					
					points = min / TEAM_SIZE;
					for (int i = 0 ; i < TEAM_SIZE; i++)
					{
						par = _teamTwo[i];
						par.updateStat(COMP_WON, 1);
						addPointsToParticipant(par, points);
					}

					for (int i = 0 ; i < TEAM_SIZE; i++)
						rewardParticipant(_teamTwo[i].player, getReward());
				}
				else if (tOneCrash && tTwoCrash)
				{
					stadium.broadcastPacket(SystemMessage.getSystemMessage(SystemMessageId.THE_GAME_ENDED_IN_A_TIE));

					for (int i = 0 ; i < TEAM_SIZE; i++)
					{
						par = _teamOne[i];
						par.updateStat(COMP_LOST, 1);
						removePointsFromParticipant(par, pointsTeamOne[i]);
					}

					for (int i = 0 ; i < TEAM_SIZE; i++)
					{
						par = _teamTwo[i];
						par.updateStat(COMP_LOST, 1);
						removePointsFromParticipant(par, pointsTeamTwo[i]);
					}
				}

				for (int i = 0 ; i < TEAM_SIZE; i++)
				{
					par = _teamOne[i];
					par.updateStat(COMP_DONE, 1);
					par.updateNobleStats();

					par = _teamTwo[i];
					par.updateStat(COMP_DONE, 1);
					par.updateNobleStats();
				}
			}
			catch (Exception e)
			{
				_log.log(Level.WARNING, "Exception on validateWinner(): " + e.getMessage(), e);
			}
			return;
		}
		
		try
		{
			double hp;
			double teamOneHp = 0;
			double teamTwoHp = 0;
			for (int i = 0 ; i < TEAM_SIZE; i++)
			{
				par = _teamOne[i];
				if (!par.disconnected
						&& par.player != null
						&& !par.player.isDead())
				{
					hp = par.player.getCurrentHp() + par.player.getCurrentCp();
					if (hp >= 0.5)
						teamOneHp += hp;
				}
				par.updatePlayer();
				
				par = _teamTwo[i];
				if (!par.disconnected
						&& par.player != null
						&& !par.player.isDead())
				{
					hp = par.player.getCurrentHp() + par.player.getCurrentCp();
					if (hp >= 0.5)
						teamTwoHp += hp;
				}
				par.updatePlayer();
			}
			
			if ((teamTwoHp == 0 && teamOneHp != 0)
					|| (_damageT1 > _damageT2 && teamTwoHp != 0 && teamOneHp != 0))
			{
				sm = SystemMessage.getSystemMessage(SystemMessageId.C1_HAS_WON_THE_GAME);
				sm.addString(_teamOne[0].name);
				stadium.broadcastPacket(sm);

				for (int i = 0 ; i < TEAM_SIZE; i++)
				{
					par = _teamTwo[i];
					par.updateStat(COMP_LOST, 1);
					points = pointsTeamTwo[i];
					removePointsFromParticipant(par, points);
				}

				points = min / TEAM_SIZE;
				for (int i = 0 ; i < TEAM_SIZE; i++)
				{
					par = _teamOne[i];
					par.updateStat(COMP_WON, 1);
					addPointsToParticipant(par, points);
				}

				for (int i = 0 ; i < TEAM_SIZE; i++)
					rewardParticipant(_teamOne[i].player, getReward());
			}
			else if ((teamOneHp == 0 && teamTwoHp != 0)
					|| (_damageT2 > _damageT1 && teamOneHp != 0 && teamTwoHp != 0))
			{
				sm = SystemMessage.getSystemMessage(SystemMessageId.C1_HAS_WON_THE_GAME);
				sm.addString(_teamTwo[0].name);
				stadium.broadcastPacket(sm);

				for (int i = 0 ; i < TEAM_SIZE; i++)
				{
					par = _teamOne[i];
					par.updateStat(COMP_LOST, 1);
					points = pointsTeamOne[i];
					removePointsFromParticipant(par, points);
				}
				
				points = min / TEAM_SIZE;
				for (int i = 0 ; i < TEAM_SIZE; i++)
				{
					par = _teamTwo[i];
					par.updateStat(COMP_WON, 1);
					addPointsToParticipant(par, points);
				}

				for (int i = 0 ; i < TEAM_SIZE; i++)
					rewardParticipant(_teamTwo[i].player, getReward());
			}
			else
			{
				stadium.broadcastPacket(SystemMessage.getSystemMessage(SystemMessageId.THE_GAME_ENDED_IN_A_TIE));

				for (int i = 0 ; i < TEAM_SIZE; i++)
				{
					par = _teamOne[i];
					par.updateStat(COMP_DRAWN, 1);
					points = Math.min(par.stats.getInteger(POINTS) / getDivider(), Config.ALT_OLY_MAX_POINTS);
					removePointsFromParticipant(par, points);
				}

				for (int i = 0 ; i < TEAM_SIZE; i++)
				{
					par = _teamTwo[i];
					par.updateStat(COMP_DRAWN, 1);
					points = Math.min(par.stats.getInteger(POINTS) / getDivider(), Config.ALT_OLY_MAX_POINTS);
					removePointsFromParticipant(par, points);
				}
			}
			
			for (int i = 0 ; i < TEAM_SIZE; i++)
			{
				par = _teamOne[i];
				par.updateStat(COMP_DONE, 1);
				par.updateNobleStats();

				par = _teamTwo[i];
				par.updateStat(COMP_DONE, 1);
				par.updateNobleStats();
			}
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Exception on validateWinner(): " + e.getMessage(), e);
		}
	}
	
	@Override
	protected final void addDamage(L2PcInstance player, int damage)
	{
		Participant par;
		for (int i = 0; i < TEAM_SIZE; i++)
		{
			par = _teamOne[i];
			if (par.objectId == player.getObjectId())
			{
				if (!par.disconnected)
					_damageT1 += damage;
				return;
			}
		}

		for (int i = 0; i < TEAM_SIZE; i++)
		{
			par = _teamTwo[i];
			if (par.objectId == player.getObjectId())
			{
				if (!par.disconnected)
					_damageT2 += damage;
				return;
			}
		}
	}
	
	@Override
	public final String[] getPlayerNames()
	{
		return new String[] {_teamOne[0].name, _teamTwo[0].name};
	}

	@Override
	public final boolean checkDefaulted()
	{
		try
		{
			SystemMessage reason = null;
			Participant par;
			for (int i = 0; i < TEAM_SIZE; i++)
			{
				par = _teamOne[i];
				par.updatePlayer();
				reason = AbstractOlympiadGame.checkDefaulted(par.player);
				if (reason != null)
				{
					par.defaulted = true;
					if (!_teamOneDefaulted)
					{
						_teamOneDefaulted = true;
						for (Participant t : _teamTwo)
						{
							if (t.player != null)
								t.player.sendPacket(reason);
						}
					}
				}
			}

			reason = null;
			for (int i = 0; i < TEAM_SIZE; i++)
			{
				par = _teamTwo[i];
				par.updatePlayer();
				reason = AbstractOlympiadGame.checkDefaulted(par.player);
				if (reason != null)
				{
					par.defaulted = true;
					if (!_teamTwoDefaulted)
					{
						_teamTwoDefaulted = true;
						for (Participant t : _teamOne)
						{
							if (t.player != null)
								t.player.sendPacket(reason);
						}
					}
				}
			}
			
			return _teamOneDefaulted || _teamTwoDefaulted;
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Exception on checkDefaulted(): " + e.getMessage(), e);
			return true;
		}
	}

	@Override
	public final void resetDamage()
	{
		_damageT1 = 0;
		_damageT2 = 0;
	}

	private final boolean teamOneAllDisconnected()
	{
		for (int i = 0; i < TEAM_SIZE; i++)
			if (!_teamOne[i].disconnected)
				return false;

		return true;
	}

	private final boolean teamTwoAllDisconnected()
	{
		for (int i = 0; i < TEAM_SIZE; i++)
			if (!_teamTwo[i].disconnected)
				return false;

		return true;
	}
}
