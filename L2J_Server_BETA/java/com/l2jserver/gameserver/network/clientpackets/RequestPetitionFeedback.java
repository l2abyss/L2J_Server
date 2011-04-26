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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2jserver.L2DatabaseFactory;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author Plim
 */
public class RequestPetitionFeedback extends L2GameClientPacket
{
	private static final String _C__C9_REQUESTPETITIONFEEDBACK = "[C] C9 RequestPetitionFeedback";
	private static final Logger _log = Logger.getLogger(RequestPetitionFeedback.class.getName());
	
	private static final String INSERT_FEEDBACK = "INSERT INTO petition_feedback VALUES (?,?,?,?,?)";
	
	//cdds
	//private int _unknown;
	private int _rate; // 4=VeryGood, 3=Good, 2=Fair, 1=Poor, 0=VeryPoor
	private String _message;
	
	@Override
	protected void readImpl()
	{
		//_unknown = 
		readD(); // unknown
		_rate = readD();
		_message = readS();
	}
	
	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		
		if (player == null || player.getLastPetitionGmName() == null)
			return;
		
		if (_rate > 4 || _rate < 0) // Ilegal vote
			return;
		
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(INSERT_FEEDBACK);
			statement.setString(1, player.getName());
			statement.setString(2, player.getLastPetitionGmName());
			statement.setInt(3, _rate);
			statement.setString(4, _message);
			statement.setLong(5, System.currentTimeMillis());
			
			statement.execute();
			statement.close();
		}
		catch (SQLException e)
		{
			_log.log(Level.SEVERE, "Error while saving petition feedback");
		}
		finally
		{
			L2DatabaseFactory.close(con);
			player.setLastPetitionGmName(null);
		}
	}
	
	/* (non-Javadoc)
	 * @see com.l2jserver.gameserver.network.clientpackets.L2GameClientPacket#getType()
	 */
	@Override
	public String getType()
	{
		return _C__C9_REQUESTPETITIONFEEDBACK;
	}
}
