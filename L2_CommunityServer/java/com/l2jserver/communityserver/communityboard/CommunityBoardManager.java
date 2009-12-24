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
package com.l2jserver.communityserver.communityboard;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.logging.Logger;

import javolution.util.FastMap;
import com.l2jserver.communityserver.Config;
import com.l2jserver.communityserver.L2DatabaseFactory;
import com.l2jserver.communityserver.communityboard.boards.ClanBoard;
import com.l2jserver.communityserver.communityboard.boards.ErrorBoard;
import com.l2jserver.communityserver.communityboard.boards.FriendBoard;
import com.l2jserver.communityserver.communityboard.boards.MailBoard;
import com.l2jserver.communityserver.communityboard.boards.MemoBoard;
import com.l2jserver.communityserver.communityboard.boards.RegionBoard;
import com.l2jserver.communityserver.communityboard.boards.TopBoard;
import com.l2jserver.communityserver.communityboard.boards.ClanPostBoard;
import com.l2jserver.communityserver.model.Forum;
import com.l2jserver.communityserver.model.L2Castle;
import com.l2jserver.communityserver.model.L2Player;
import com.l2jserver.communityserver.model.L2Clan;
import com.l2jserver.communityserver.network.GameServerThread;
import com.l2jserver.communityserver.network.netcon.BaseWritePacket;
import com.l2jserver.communityserver.network.writepackets.RequestWorldInfo;

public final class CommunityBoardManager
{
	private static Logger _log = Logger.getLogger(CommunityBoardManager.class.getName());
	private static FastMap<Integer, CommunityBoardManager> _instances;
	
	public static CommunityBoardManager getInstance(final int sqlDPId)
	{
		if (_instances == null)
			_instances = new FastMap<Integer, CommunityBoardManager>();
		
		CommunityBoardManager mgr = _instances.get(sqlDPId);
		
		if (mgr == null)
		{
			mgr = new CommunityBoardManager(sqlDPId);
			_instances.put(sqlDPId, mgr);
		}
		
		return mgr;
	}
	
	private FastMap<Integer, Forum> _forumRoot;
	private FastMap<Integer, L2Player> _players;
	private FastMap<Integer, L2Clan> _clans;
	private FastMap<Integer, L2Castle> _castles;
	private final FastMap<String, CommunityBoard> _boards;
	private final int _sqlDPId;
	private GameServerThread _gst;
	private int _lastForumId = 1;
	private boolean _isLoaded = false;
	
	private CommunityBoardManager(final int sqlDPId)
	{
		_sqlDPId = sqlDPId;
		
		_boards = new FastMap<String, CommunityBoard>();
		_boards.put("_bbsloc", new RegionBoard(this));
		_boards.put("_bbsfriend", new FriendBoard(this));
		_boards.put("_bbsclan", new ClanBoard(this));
		_boards.put("_bbscpost", new ClanPostBoard(this));
		_boards.put("_bbsmail", new MailBoard(this));
		_boards.put("_bbsmemo", new MemoBoard(this));
		_boards.put("_bbshome", new TopBoard(this));
		_boards.put("_bbserror", new ErrorBoard(this));
		_forumRoot = new FastMap<Integer, Forum>();
		_players = new FastMap<Integer, L2Player>();
		_clans = new FastMap<Integer, L2Clan>();
		_castles = new FastMap<Integer, L2Castle>();
	}
	
	private void loadDataBase()
	{
		java.sql.Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT forum_id, forum_type, forum_owner_id FROM forums WHERE serverId=?");
			statement.setInt(1, _sqlDPId);
			ResultSet result = statement.executeQuery();
			while (result.next())
			{
				Forum f = new Forum(_sqlDPId, Integer.parseInt(result.getString("forum_id")));
				int type = result.getInt("forum_type");
				if (type == Forum.CLAN)
				{
					if (getClan(result.getInt("forum_owner_id")) == null)
					{
						// delete this forum
					}
					else
					{
						getClan(result.getInt("forum_owner_id")).setForum(f);
						_forumRoot.put(Integer.parseInt(result.getString("forum_id")), f);
					}
				}
				else if (type == Forum.PLAYER)
				{
					if (getPlayer(result.getInt("forum_owner_id")) == null)
					{
						// delete this forum
					}
					else
					{
						getPlayer(result.getInt("forum_owner_id")).setForum(f);
						_forumRoot.put(Integer.parseInt(result.getString("forum_id")), f);
					}
				}
				if (f.getID() > _lastForumId)
					_lastForumId = f.getID();
			}
			result.close();
			statement.close();
		}
		catch (Exception e)
		{
			// _log.warning("data error on Forum (root): " + e);
			e.printStackTrace();
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
			}
		}
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT introduction,clanId FROM clan_introductions WHERE serverId=?");
			statement.setInt(1, _sqlDPId);
			ResultSet result = statement.executeQuery();
			while (result.next())
				getClan(result.getInt("clanId")).setIntroduction(result.getString("introduction"));
			result.close();
			statement.close();
		}
		catch (Exception e)
		{
			// _log.warning("data error on Forum (root): " + e);
			e.printStackTrace();
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
			}
		}
		int requestedClanNotices = 0;
		try
		{
			for(L2Clan c : _clans.values())
			{
				if (c == null)
					continue;
				if (_players.containsKey(c.getLordObjId()) && _players.get(c.getLordObjId()).isOnline())
				{
					getGST().sendPacket(new RequestWorldInfo(RequestWorldInfo.CLAN_NOTICE_DATA,c.getClanId(), "", false));
					requestedClanNotices++;	
				}
			}
			_log.info("Requesting " + requestedClanNotices + " clan notices from GS.");
		}
		catch (Exception e)
		{
			_log.warning("Data error on Notice Load: " + e);
			// e.printStackTrace();
		}

	}
	
	private int getNewForumId()
	{
		return ++_lastForumId;
	}
	
	public void clean()
	{
		_forumRoot.clear();
		_players.clear();
		_clans.clear();
		_lastForumId = 0;
	}
	
	public void addPlayer(L2Player player)
	{
		if (!_players.containsKey(player.getObjId()))
			_players.put(player.getObjId(), player);
	}
	
	public void updatePlayer(int playerObjId, String name, String accountName, int playerLevel, int accessLevel, int playerClanId, boolean isOnline, int[] friendIDs)
	{
		if (_players.containsKey(playerObjId))
		{
			L2Player player = _players.get(playerObjId);
			if (player.getName() != name)
				player.setName(name);
			if (player.getLevel() != playerLevel)
				player.setLevel(playerLevel);
			if (player.getAccessLevel() != accessLevel)
				player.setAccessLevel(accessLevel);
			if (player.getClanId() != playerClanId)
				player.setClanId(playerClanId);
			if (player.isOnline() != isOnline)
				player.setIsOnline(isOnline);
			player.removeAllFriends();
			for(int i : friendIDs)
			{
				player.addFriend(i);
			}			
		}
		else
		{
			L2Player player = new L2Player(playerObjId, name, accountName, playerLevel, accessLevel, playerClanId, isOnline);
			for(int i : friendIDs)
			{
				player.addFriend(i);
			}
			_players.put(playerObjId, player);
			_log.info("New player is successfully created with " + player.getName() + " name.");
		}
	}
	
	public L2Player getPlayer(int playerObjId)
	{
		if (!_players.containsKey(playerObjId))
			return null;
		return _players.get(playerObjId);
	}
	
	public L2Player getPlayerByName(String playerName)
	{
		for (L2Player p : _players.values())
			if (p.getName().equalsIgnoreCase(playerName.toLowerCase()))
				return p;
		return null;
	}

	public Collection<L2Player> getPlayerList()
	{
		return _players.values();
	}
	
	public Forum getPlayerForum(int playerObjId)
	{
		if (!_players.containsKey(playerObjId))
			return null;
		L2Player p = _players.get(playerObjId);
		Forum ret = p.getForum();
		if (ret == null && p.getLevel() >= Config.MIN_PLAYER_LVL_FOR_FORUM)
		{
			ret = new Forum(_sqlDPId, getNewForumId(), p.getName(), Forum.PLAYER, p.getObjId());
			p.setForum(ret);
		}
		return ret;
	}
	
	public void addClan(L2Clan clan)
	{
		if (!_clans.containsKey(clan.getClanId()))
			_clans.put(clan.getClanId(), clan);
	}
	
	public void updateClan(int clanId, String clanName, int level, int lordObjId, String lordName, int members, String allyName, int[] alliance, boolean isNoticeEnabled)
	{
		if (_clans.containsKey(clanId))
		{
			L2Clan clan = _clans.get(clanId);
			if (clan.getName() != clanName)
				clan.setName(clanName);
			if (clan.getClanLevel() != level)
				clan.setLevel(level);
			if (clan.getLordObjId() != lordObjId)
			{
				clan.setLordObjId(lordObjId);
				clan.setLordName(lordName);
			}
			if (clan.getLordName() != lordName)
				clan.setLordName(lordName);
			if (clan.getMembersCount() != members)
				clan.setMembersCount(members);
			if (clan.getAllianceName() != allyName)
				clan.setAllianceName(allyName);
			clan.setAllianceClanIdList(alliance);
			if (clan.isNoticeEnabled() != isNoticeEnabled)
				clan.setNoticeEnabled(isNoticeEnabled);
		}
		else
		{
			L2Clan clan = new L2Clan(clanId, clanName, level, lordObjId, lordName, members, allyName, alliance, isNoticeEnabled);
			_clans.put(clan.getClanId(), clan);
			_log.info("New clan is successfully created with " + clan.getName() + " name.");
		}
	}
	
	public L2Clan getClan(int clanId)
	{
		if (!_clans.containsKey(clanId))
			return null;
		return _clans.get(clanId);
	}
	
	public L2Clan getPlayersClan(int playerObjId)
	{
		if (!_players.containsKey(playerObjId))
			return null;
		int clanId = _players.get(playerObjId).getClanId();
		if (!_clans.containsKey(clanId))
			return null;
		return _clans.get(clanId);
	}

	public Collection<L2Clan> getClanList()
	{
		return _clans.values();
	}
	
	public Forum getClanForum(int clanId)
	{
		if (!_clans.containsKey(clanId))
			return null;
		L2Clan c = _clans.get(clanId);
		Forum ret = c.getForum();
		if (ret == null && c.getClanLevel() >= Config.MIN_CLAN_LVL_FOR_FORUM)
		{
			ret = new Forum(_sqlDPId, getNewForumId(), c.getName(), Forum.CLAN, c.getClanId());
			c.setForum(ret);
		}
		return ret;
	}
	
	public void addCastle(L2Castle castle)
	{
		_castles.put(castle.getId(), castle);
	}
	
	public L2Castle getCastle(int castleId)
	{
		return _castles.get(castleId);
	}
	
	public Collection<L2Castle> getCastleList()
	{
		return _castles.values();
	}

	public boolean isLoaded()
	{
		return _isLoaded;
	}
	
	public void setLoaded()
	{
		loadDataBase();
		_isLoaded = true;
	}
		
	public void storeClanIntro(int clanId, String intro)
	{
		java.sql.Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("INSERT INTO clan_introductions (serverId,clanId,introduction) values (?,?,?) ON DUPLICATE KEY UPDATE introduction = ?");
			statement.setInt(1, _sqlDPId);
			statement.setInt(2, clanId);
			statement.setString(3, intro);
			statement.setString(4, intro);
			statement.execute();
			statement.close();
			
		}
		catch (Exception e)
		{
			_log.warning("error while saving new Topic to db " + e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
			}
		}
	}
	
	public final void parseCmd(final int playerObjId, final String cmd)
	{
		String board = cmd.split(";")[0];
		try
		{
			if (_boards.containsKey(board))
				_boards.get(board).parseCmd(playerObjId, cmd);
			else
				_boards.get("_bbserror").parseCmd(playerObjId, "noBoard;" + cmd);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public final void parseWrite(final int playerObjId, final String url, final String arg1, final String arg2, final String arg3, final String arg4, final String arg5)
	{
		try
		{
			if (_boards.containsKey(url))
				_boards.get(url).parseWrite(playerObjId, arg1, arg2, arg3, arg4, arg5);
			else
				_boards.get("_bbserror").parseCmd(playerObjId, "noBoard;" + url);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public final int getSQLDPId()
	{
		return _sqlDPId;
	}
	
	protected final void sendPacket(final BaseWritePacket packet)
	{
		_gst.sendPacket(packet);
	}
	
	public final void setGST(final GameServerThread gst)
	{
		_gst = gst;
	}
	
	public final GameServerThread getGST()
	{
		return _gst;
	}
}