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
package com.l2jserver.gameserver.network.communityserver.readpackets;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.netcon.BaseReadPacket;

import com.l2jserver.L2DatabaseFactory;
import com.l2jserver.gameserver.datatables.ClanTable;
import com.l2jserver.gameserver.model.L2Clan;
import com.l2jserver.gameserver.model.L2World;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.network.communityserver.CommunityServerThread;
import com.l2jserver.gameserver.network.communityserver.writepackets.InitWorldInfo;
import com.l2jserver.gameserver.network.communityserver.writepackets.WorldInfo;
import com.l2jserver.gameserver.templates.StatsSet;

/**
 * @authors  Forsaiken, Gigiikun
 */
public final class RequestWorldInfo extends BaseReadPacket
{
	public static final byte SERVER_LOAD						= 0;
	public static final byte PLAYER_DATA_UPDATE					= 1;
	public static final byte CLAN_DATA_UPDATE					= 2;
	private static Logger _log = Logger.getLogger(WorldInfo.class.getName());
	private final CommunityServerThread _cst;
	private static final int MAX_ARRAY = 10; // set this with caution, 8192 is the max packet size!!!
	private final int _type;
	
	public RequestWorldInfo(final byte[] data, final CommunityServerThread cst, final int type)
	{
		super(data);
		_cst = cst;
		_type = type;
	}
	
	@Override
	public final void run()
	{
		switch(_type)
		{
			case SERVER_LOAD:
				// clans data
				L2Clan[] clans = new L2Clan[MAX_ARRAY];
				int i = 0;
				int j = 0;
				for (L2Clan c : ClanTable.getInstance().getClans())
				{
					clans[i++] = c;
					if (i >= MAX_ARRAY)
					{
						i = 0;
						j++;
						_cst.sendPacket(new InitWorldInfo(null, clans, InitWorldInfo.TYPE_CLAN, -1), false);
					}
				}
				if (i != 0)
				{
					j++;
					_cst.sendPacket(new InitWorldInfo(null, clans, InitWorldInfo.TYPE_CLAN, i), false);
				}
				_log.info("Transfering " + ClanTable.getInstance().getClans().length + " Clan data to CB server.");
				
				// players data
				Connection con = null;
				StatsSet[] charDatList = new StatsSet[MAX_ARRAY];
				try
				{
					con = L2DatabaseFactory.getInstance().getConnection();
					PreparedStatement statement = con.prepareStatement("SELECT account_name, charId, char_name, level, clanid, accesslevel, online FROM characters");
					ResultSet charList = statement.executeQuery();
					i = 0;
					int charNumber = 0;
					while (charList.next())
					{
						charNumber++;
						StatsSet charDat = new StatsSet();
						charDat.set("account_name", charList.getString("account_name"));
						charDat.set("charId", charList.getInt("charId"));
						charDat.set("char_name", charList.getString("char_name"));
						charDat.set("level", charList.getInt("level"));
						charDat.set("clanid", charList.getInt("clanid"));
						charDat.set("accesslevel", charList.getInt("accesslevel"));
						charDat.set("online", charList.getInt("online"));
						charDatList[i++] = charDat;
						if (i >= MAX_ARRAY)
						{
							i = 0;
							j++;
							_cst.sendPacket(new InitWorldInfo(charDatList, null, InitWorldInfo.TYPE_PLAYER, -1), false);
						}
					}
					if (i != 0)
					{
						j++;
						_cst.sendPacket(new InitWorldInfo(charDatList, null, InitWorldInfo.TYPE_PLAYER, i), false);
					}
					_log.info("Transfering " + charNumber + " character data to CB server.");
					charList.close();
					statement.close();
					
				}
				catch (Exception e)
				{
					_log.log(Level.WARNING, "Could not restore char info: " + e.getMessage(), e);
				}
				finally
				{
					try { con.close(); } catch (Exception e) {}
				}

				// Castles data
				j++;
				_cst.sendPacket(new InitWorldInfo(null, null, InitWorldInfo.TYPE_CASTLE, j), false);
				
				j++;
				_cst.sendPacket(new InitWorldInfo(null, null, InitWorldInfo.TYPE_INFO, j), false);
				break;
			case PLAYER_DATA_UPDATE:
				int playerObjId = super.readD();
				L2PcInstance player = L2World.getInstance().getPlayer(playerObjId);
				if (player != null)
					_cst.sendPacket(new WorldInfo(player, null, WorldInfo.TYPE_UPDATE_PLAYER_DATA));
				break;
			case CLAN_DATA_UPDATE:
				int clanObjId = super.readD();
				L2Clan clan = ClanTable.getInstance().getClan(clanObjId);
				if (clan != null)
					_cst.sendPacket(new WorldInfo(null, clan, WorldInfo.TYPE_UPDATE_CLAN_DATA));
				break;
		}
	}
}
