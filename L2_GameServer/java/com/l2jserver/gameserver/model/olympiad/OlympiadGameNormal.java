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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import com.l2jserver.Config;
import com.l2jserver.L2DatabaseFactory;
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
 * @author GodKratos, Pere, DS
 */
abstract public class OlympiadGameNormal extends AbstractOlympiadGame
{
	protected int _damageP1 = 0;
	protected int _damageP2 = 0;
	
	protected Participant _playerOne;
	protected Participant _playerTwo;
	
	protected OlympiadGameNormal(int id, Participant[] opponents)
	{
		super(id);

		_playerOne = opponents[0];
		_playerTwo = opponents[1];

		_playerOne.player.setOlympiadGameId(id);
		_playerTwo.player.setOlympiadGameId(id);
	}

	protected static final Participant[] createListOfParticipants(List<Integer> list)
	{
		if (list == null || list.isEmpty() || list.size() < 2)
			return null;

		int playerOneObjectId = 0;
		L2PcInstance playerOne = null;
		L2PcInstance playerTwo = null;

		while (list.size() > 1)
		{
			playerOneObjectId = list.remove(Rnd.nextInt(list.size()));
			playerOne = L2World.getInstance().getPlayer(playerOneObjectId);
			if (playerOne == null || !playerOne.isOnline())
				continue;

			playerTwo = L2World.getInstance().getPlayer(list.remove(Rnd.nextInt(list.size())));
			if (playerTwo == null || !playerTwo.isOnline())
			{
				list.add(playerOneObjectId);
				continue;
			}

			Participant[] result = new Participant[2];
			result[0] = new Participant(playerOne, 1);
			result[1] = new Participant(playerTwo, 2);

			return result;
		}
		return null;
	}

	@Override
	public final boolean containsParticipant(int playerId)
	{
		return _playerOne.objectId == playerId || _playerTwo.objectId == playerId;
	}

	@Override
	public final void sendOlympiadInfo(L2Character player)
	{
		player.sendPacket(new ExOlympiadUserInfo(_playerOne));
		player.sendPacket(new ExOlympiadUserInfo(_playerTwo));
	}

	@Override
	public final void broadcastOlympiadInfo(L2OlympiadStadiumZone stadium)
	{
		stadium.broadcastPacket(new ExOlympiadUserInfo(_playerOne));
		stadium.broadcastPacket(new ExOlympiadUserInfo(_playerTwo));
	}

	@Override
	protected final void broadcastPacket(L2GameServerPacket packet)
	{
		_playerOne.updatePlayer();
		if (_playerOne.player != null)
			_playerOne.player.sendPacket(packet);

		_playerTwo.updatePlayer();
		if (_playerTwo.player != null)
			_playerTwo.player.sendPacket(packet);
	}

	@Override
	protected final boolean portPlayersToArena(List<Location> spawns)
	{
		boolean result = true;
		try
		{
			result &= portPlayerToArena(_playerOne, spawns.get(0), _stadiumID);
			result &= portPlayerToArena(_playerTwo, spawns.get(spawns.size() / 2), _stadiumID);
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "", e);
			return false;
		}
		return result;
	}

	@Override
	protected boolean needBuffers()
	{
		return true;
	}

	@Override
	protected final void removals()
	{
		if (_aborted)
			return;

		removals(_playerOne.player, true);
		removals(_playerTwo.player, true);
	}

	@Override
	protected final boolean makeCompetitionStart()
	{
		if (!super.makeCompetitionStart())
			return false;

		if (_playerOne.player == null || _playerTwo.player == null)
			return false;

		_playerOne.player.setIsOlympiadStart(true);
		_playerOne.player.updateEffectIcons();
		_playerTwo.player.setIsOlympiadStart(true);
		_playerTwo.player.updateEffectIcons();
		return true;
	}

	@Override
	protected final void cleanEffects()
	{
		if (_playerOne.player != null
				&& !_playerOne.defaulted
				&& !_playerOne.disconnected
				&& _playerOne.player.getOlympiadGameId() == _stadiumID)
			cleanEffects(_playerOne.player);

		if (_playerTwo.player != null
				&& !_playerTwo.defaulted
				&& !_playerTwo.disconnected
				&& _playerTwo.player.getOlympiadGameId() == _stadiumID)
			cleanEffects(_playerTwo.player);
	}

	@Override
	protected final void portPlayersBack()
	{
		if (_playerOne.player != null
				&& !_playerOne.defaulted
				&& !_playerOne.disconnected)
			portPlayerBack(_playerOne.player);
		if (_playerTwo.player != null 
				&& !_playerTwo.defaulted
				&& !_playerTwo.disconnected)
			portPlayerBack(_playerTwo.player);
	}

	@Override
	protected final void playersStatusBack()
	{
		if (_playerOne.player != null
				&& !_playerOne.defaulted
				&& !_playerOne.disconnected
				&& _playerOne.player.getOlympiadGameId() == _stadiumID)
			playerStatusBack(_playerOne.player);

		if (_playerTwo.player != null
				&& !_playerTwo.defaulted
				&& !_playerTwo.disconnected
				&& _playerTwo.player.getOlympiadGameId() == _stadiumID)
			playerStatusBack(_playerTwo.player);
	}

	@Override
	protected final void clearPlayers()
	{
		_playerOne.player = null;
		_playerOne = null;
		_playerTwo.player = null;
		_playerTwo = null;
	}
	
	@Override
	protected final void handleDisconnect(L2PcInstance player)
	{
		if (player.getObjectId() == _playerOne.objectId)
			_playerOne.disconnected = true;
		else if (player.getObjectId() == _playerTwo.objectId)
			_playerTwo.disconnected = true;
	}

	@Override
	protected final boolean checkBattleStatus()
	{
		if (_aborted)
			return false;		

		if (_playerOne.player == null || _playerOne.disconnected)
			return false;

		if (_playerTwo.player == null || _playerTwo.disconnected)
			return false;

		return true;
	}
	
	@Override
	protected final boolean haveWinner()
	{
		if (!checkBattleStatus())
			return true;
		
		boolean playerOneLost = true;
		try
		{
			if (_playerOne.player.getOlympiadGameId() == _stadiumID)
				playerOneLost = _playerOne.player.isDead();
		}
		catch (Exception e)
		{
			playerOneLost = true;
		}
		
		boolean playerTwoLost = true;
		try
		{
			if (_playerTwo.player.getOlympiadGameId() == _stadiumID)
				playerTwoLost = _playerTwo.player.isDead();
		}
		catch (Exception e)
		{
			playerTwoLost = true;
		}
		
		return playerOneLost || playerTwoLost;
	}
	
	@Override
	protected void validateWinner(L2OlympiadStadiumZone stadium)
	{
		if (_aborted)
			return;
		
		final boolean _pOneCrash = (_playerOne.player == null || _playerOne.disconnected);
		final boolean _pTwoCrash = (_playerTwo.player == null || _playerTwo.disconnected);
		
		final int playerOnePoints = _playerOne.stats.getInteger(POINTS);
		final int playerTwoPoints = _playerTwo.stats.getInteger(POINTS);
		int pointDiff = Math.min(playerOnePoints, playerTwoPoints) / getDivider();
		if (pointDiff <= 0)
			pointDiff = 1;
		else if (pointDiff > Config.ALT_OLY_MAX_POINTS)
			pointDiff = Config.ALT_OLY_MAX_POINTS;

		int points;
		SystemMessage sm;
		
		// Check for if a player defaulted before battle started
		if (_playerOne.defaulted || _playerTwo.defaulted)
		{
			try
			{
				if (_playerOne.defaulted)
				{
					try
					{
						points = Math.min(playerOnePoints / 3, Config.ALT_OLY_MAX_POINTS);
						removePointsFromParticipant(_playerOne, points);
						_playerOne.updateNobleStats();
						
						if (Config.ALT_OLY_LOG_FIGHTS)
						{
							LogRecord record = new LogRecord(Level.INFO, _playerOne.name+" default");
							record.setParameters(new Object[]{_playerOne.name, _playerTwo.name, 0, 0, 0, 0, points, getType().toString()});
							_logResults.log(record);
						}
					}
					catch (Exception e)
					{
						_log.log(Level.WARNING, "Exception on validateWinner(): " + e.getMessage(), e);
					}
				}
				if (_playerTwo.defaulted)
				{
					try
					{
						points = Math.min(playerTwoPoints / 3, Config.ALT_OLY_MAX_POINTS);
						removePointsFromParticipant(_playerTwo, points);
						_playerTwo.updateNobleStats();
						
						if (Config.ALT_OLY_LOG_FIGHTS)
						{
							LogRecord record = new LogRecord(Level.INFO, _playerTwo.name+" default");
							record.setParameters(new Object[]{_playerOne.name, _playerTwo.name, 0, 0, 0, 0, points, getType().toString()});
							_logResults.log(record);
						}
					}
					catch (Exception e)
					{
						_log.log(Level.WARNING, "Exception on validateWinner(): " + e.getMessage(), e);
					}
				}
				return;
			}
			catch (Exception e)
			{
				_log.log(Level.WARNING, "Exception on validateWinner(): " + e.getMessage(), e);
				return;
			}				
		}
		
		// Create results for players if a player crashed
		if (_pOneCrash || _pTwoCrash)
		{
			try
			{
				if (_pTwoCrash && !_pOneCrash)
				{
					sm = new SystemMessage(SystemMessageId.C1_HAS_WON_THE_GAME);
					sm.addString(_playerOne.name);
					stadium.broadcastPacket(sm);

					_playerOne.updateStat(COMP_WON, 1);
					addPointsToParticipant(_playerOne, pointDiff);

					_playerTwo.updateStat(COMP_LOST, 1);
					removePointsFromParticipant(_playerTwo, pointDiff);
					
					rewardParticipant(_playerOne.player, getReward());

					if (Config.ALT_OLY_LOG_FIGHTS)
					{
						LogRecord record = new LogRecord(Level.INFO, _playerTwo.name+" crash");
						record.setParameters(new Object[]{_playerOne.name, _playerTwo.name, 0, 0, 0, 0, pointDiff, getType().toString()});
						_logResults.log(record);
					}
				}
				else if (_pOneCrash && !_pTwoCrash)
				{
					sm = new SystemMessage(SystemMessageId.C1_HAS_WON_THE_GAME);
					sm.addString(_playerTwo.name);
					stadium.broadcastPacket(sm);

					_playerTwo.updateStat(COMP_WON, 1);
					addPointsToParticipant(_playerTwo, pointDiff);

					_playerOne.updateStat(COMP_LOST, 1);
					removePointsFromParticipant(_playerOne, pointDiff);
					
					rewardParticipant(_playerTwo.player, getReward());

					if (Config.ALT_OLY_LOG_FIGHTS)
					{
						LogRecord record = new LogRecord(Level.INFO, _playerOne.name+" crash");
						record.setParameters(new Object[]{_playerOne.name, _playerTwo.name, 0, 0, 0, 0, pointDiff, getType().toString()});
						_logResults.log(record);
					}
				}
				else if (_pOneCrash && _pTwoCrash)
				{
					stadium.broadcastPacket(new SystemMessage(SystemMessageId.THE_GAME_ENDED_IN_A_TIE));

					_playerOne.updateStat(COMP_LOST, 1);
					removePointsFromParticipant(_playerOne, pointDiff);

					_playerTwo.updateStat(COMP_LOST, 1);
					removePointsFromParticipant(_playerTwo, pointDiff);
					
					if (Config.ALT_OLY_LOG_FIGHTS)
					{
						LogRecord record = new LogRecord(Level.INFO, "both crash");
						record.setParameters(new Object[]{_playerOne.name, _playerTwo.name, 0, 0, 0, 0, pointDiff, getType().toString()});
						_logResults.log(record);
					}
				}

				_playerOne.updateStat(COMP_DONE, 1);
				_playerTwo.updateStat(COMP_DONE, 1);
				
				_playerOne.updateNobleStats();
				_playerTwo.updateNobleStats();
				return;
			}
			catch (Exception e)
			{
				_log.log(Level.WARNING, "Exception on validateWinner(): " + e.getMessage(), e);
				return;
			}
		}
		
		try
		{
			String winner = "draw";
			
			// Calculate Fight time
			long _fightTime = (System.currentTimeMillis() - _startTime);
			
			double playerOneHp = 0;
			if (_playerOne.player != null && !_playerOne.player.isDead())
			{
				playerOneHp = _playerOne.player.getCurrentHp() + _playerOne.player.getCurrentCp();
				if (playerOneHp < 0.5)
					playerOneHp = 0;
			}
			
			double playerTwoHp = 0;
			if (_playerTwo.player != null && !_playerTwo.player.isDead())
			{
				playerTwoHp = _playerTwo.player.getCurrentHp() + _playerTwo.player.getCurrentCp();
				if (playerTwoHp < 0.5)
					playerTwoHp = 0;
			}
					
			// if players crashed, search if they've relogged
			_playerOne.updatePlayer();
			_playerTwo.updatePlayer();
			
			if ((_playerOne.player == null || !_playerOne.player.isOnline())
					&& (_playerTwo.player == null || !_playerTwo.player.isOnline()))
			{
				_playerOne.updateStat(COMP_DRAWN, 1);
				_playerTwo.updateStat(COMP_DRAWN, 1);
				sm = new SystemMessage(SystemMessageId.THE_GAME_ENDED_IN_A_TIE);
				stadium.broadcastPacket(sm);
			}
			else if (_playerTwo.player == null
					|| !_playerTwo.player.isOnline()
					|| (playerTwoHp == 0 && playerOneHp != 0)
					|| (_damageP1 > _damageP2 && playerTwoHp != 0 && playerOneHp != 0))
			{
				sm = new SystemMessage(SystemMessageId.C1_HAS_WON_THE_GAME);
				sm.addString(_playerOne.name);
				stadium.broadcastPacket(sm);

				_playerOne.updateStat(COMP_WON, 1);
				_playerTwo.updateStat(COMP_LOST, 1);

				addPointsToParticipant(_playerOne, pointDiff);
				removePointsFromParticipant(_playerTwo, pointDiff);

				winner = _playerOne.name + " won";
				
				// Save Fight Result
				saveResults(_playerOne,_playerTwo,1,_startTime,_fightTime, getType());				
				rewardParticipant(_playerOne.player, getReward());
			}
			else if (_playerOne.player == null
					|| !_playerOne.player.isOnline()
					|| (playerOneHp == 0 && playerTwoHp != 0)
					|| (_damageP2 > _damageP1 && playerOneHp != 0 && playerTwoHp != 0))
			{
				sm = new SystemMessage(SystemMessageId.C1_HAS_WON_THE_GAME);
				sm.addString(_playerTwo.name);
				stadium.broadcastPacket(sm);

				_playerTwo.updateStat(COMP_WON, 1);
				_playerOne.updateStat(COMP_LOST, 1);

				addPointsToParticipant(_playerTwo, pointDiff);
				removePointsFromParticipant(_playerOne, pointDiff);

				winner = _playerTwo.name + " won";
				
				// Save Fight Result
				saveResults(_playerOne,_playerTwo,2,_startTime,_fightTime,getType());				
				rewardParticipant(_playerTwo.player, getReward());
			}
			else
			{
				// Save Fight Result
				saveResults(_playerOne,_playerTwo,0,_startTime,_fightTime, getType());
				
				sm = new SystemMessage(SystemMessageId.THE_GAME_ENDED_IN_A_TIE);
				stadium.broadcastPacket(sm);

				removePointsFromParticipant(_playerOne, Math.min(playerOnePoints / getDivider(), Config.ALT_OLY_MAX_POINTS));
				removePointsFromParticipant(_playerTwo, Math.min(playerTwoPoints / getDivider(), Config.ALT_OLY_MAX_POINTS));
			}

			_playerOne.updateStat(COMP_DONE, 1);
			_playerTwo.updateStat(COMP_DONE, 1);
			
			_playerOne.updateNobleStats();
			_playerTwo.updateNobleStats();
			
			if (Config.ALT_OLY_LOG_FIGHTS)
			{
				LogRecord record = new LogRecord(Level.INFO, winner);
				record.setParameters(new Object[]{_playerOne.name, _playerTwo.name, playerOneHp, playerTwoHp, _damageP1, _damageP2, pointDiff, getType().toString()});
				_logResults.log(record);
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
		if (_playerOne.player == null || _playerTwo.player == null)
			return;
		if (player == _playerOne.player)
			_damageP1 += damage;
		else if (player == _playerTwo.player)
			_damageP2 += damage;
	}
	
	@Override
	public final String[] getPlayerNames()
	{
		return new String[] {_playerOne.name, _playerTwo.name};
	}

	@Override
	public final boolean checkDefaulted()
	{
		SystemMessage reason;
		_playerOne.updatePlayer();
		_playerTwo.updatePlayer();
		
		reason = AbstractOlympiadGame.checkDefaulted(_playerOne.player);
		if (reason != null)
		{
			_playerOne.defaulted = true;
			if (_playerTwo.player != null)
				_playerTwo.player.sendPacket(reason);
		}

		reason = AbstractOlympiadGame.checkDefaulted(_playerTwo.player);
		if (reason != null)
		{
			_playerTwo.defaulted = true;
			if (_playerOne.player != null)
				_playerOne.player.sendPacket(reason);
		}

		return _playerOne.defaulted || _playerTwo.defaulted;
	}

	@Override
	public final void resetDamage()
	{
		_damageP1 = 0;
		_damageP2 = 0;
	}

	protected static final void saveResults(Participant one, Participant two, int _winner, long _startTime, long _fightTime, CompetitionType type)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("INSERT INTO olympiad_fights (charOneId, charTwoId, charOneClass, charTwoClass, winner, start, time, classed) values(?,?,?,?,?,?,?,?)");
			statement.setInt(1, one.objectId);
			statement.setInt(2, two.objectId);
			statement.setInt(3, one.baseClass);
			statement.setInt(4, two.baseClass);
			statement.setInt(5, _winner);
			statement.setLong(6, _startTime);
			statement.setLong(7, _fightTime);
			statement.setInt(8, (type == CompetitionType.CLASSED ? 1 : 0));
			statement.execute();
			statement.close();
		}
		catch (SQLException e)
		{
			if(_log.isLoggable(Level.SEVERE))
				_log.log(Level.SEVERE, "SQL exception while saving olympiad fight.", e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
	}
}
