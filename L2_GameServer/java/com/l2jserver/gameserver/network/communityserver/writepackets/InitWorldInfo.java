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
package com.l2jserver.gameserver.network.communityserver.writepackets;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastList;

import com.l2jserver.L2DatabaseFactory;
import com.l2jserver.gameserver.datatables.ClanTable;
import com.l2jserver.gameserver.instancemanager.CastleManager;
import com.l2jserver.gameserver.model.L2Clan;
import com.l2jserver.gameserver.model.entity.Castle;
import com.l2jserver.gameserver.templates.StatsSet;

import org.netcon.BaseWritePacket;

/**
 * @authors  Forsaiken, Gigiikun
 */
public final class InitWorldInfo extends BaseWritePacket
{
	public static final byte TYPE_INFO			= 0;
	public static final byte TYPE_PLAYER		= 1;
	public static final byte TYPE_CLAN			= 2;
	public static final byte TYPE_CASTLE		= 3;
	private static Logger _log = Logger.getLogger(InitWorldInfo.class.getName());
	
	public InitWorldInfo(StatsSet[] players, L2Clan[] clans, final byte type, int info)
	{
		super.writeC(0x01);
		super.writeC(0x00);
		super.writeD(type);
		
		int i;
		switch (type)
		{
			case TYPE_INFO:
				super.writeD(info);
				break;
			case TYPE_CLAN:
				super.writeD(info != -1 ? info : clans.length);
				i = 0;
				for (L2Clan c : clans)
				{
					if (i++ == info)
						break;
					super.writeD(c.getClanId());
					super.writeS(c.getName());
					super.writeD(c.getLevel());
					super.writeD(c.getLeader().getObjectId());
					super.writeS(c.getLeader().getName());
					super.writeD(c.getMembersCount());
					super.writeC((c.isNoticeEnabled() ? 1:0));
					super.writeS(c.getAllyName());
					FastList<Integer> allyClanIdList = FastList.newInstance();
					if (c.getAllyId() != 0)
						for (L2Clan clan : ClanTable.getInstance().getClans())
							if (clan.getAllyId() == c.getAllyId() && c != clan)
								allyClanIdList.add(clan.getClanId());
					super.writeD(allyClanIdList.size());
					for (int k : allyClanIdList)
						super.writeD(k);
					FastList.recycle(allyClanIdList);
				}
				break;
			case TYPE_PLAYER:
				super.writeD(info != -1 ? info : players.length);
				i = 0;
				for (StatsSet p : players)
				{
					if (i++ == info)
						break;
					super.writeD(p.getInteger("charId"));
					super.writeS(p.getString("char_name"));
					super.writeS(p.getString("account_name"));
					super.writeD(p.getInteger("level"));
					super.writeD(p.getInteger("clanid"));
					super.writeD(p.getInteger("accesslevel"));
					super.writeC(p.getInteger("online"));
					java.sql.Connection con = null;
					FastList<Integer> list = FastList.newInstance();
					
					try
					{
						con = L2DatabaseFactory.getInstance().getConnection();
						PreparedStatement statement;
						statement = con.prepareStatement("SELECT friendId FROM character_friends WHERE charId=?");
						statement.setInt(1, p.getInteger("charId"));
						ResultSet rset = statement.executeQuery();
						
						while (rset.next())
							list.add(rset.getInt("friendId"));

						rset.close();
						statement.close();
					}
					catch (Exception e)
					{
						_log.log(Level.SEVERE, "Error restoring friend data for Community Board transfer.", e);
					}
					finally
					{
						try {con.close();} catch (Exception e){}
					}
					super.writeD(list.size());
					for (int j : list)
						super.writeD(j);
					FastList.recycle(list);
				}
				break;
			case TYPE_CASTLE:
				List<Castle> castles = CastleManager.getInstance().getCastles();
				writeD(castles.size());
				_log.info("Transfering " + castles.size() + " castles data to CB server.");
				for (Castle castle : castles)
				{
					writeD(castle.getCastleId());
					writeS(castle.getName());
					writeD(castle.getOwnerId());
					writeD(castle.getTaxPercent());
					writeD((int)(castle.getSiege().getSiegeDate().getTimeInMillis()/1000));
				}
				break;
		}
	}
}
