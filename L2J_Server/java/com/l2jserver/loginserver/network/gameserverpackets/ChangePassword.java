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
package com.l2jserver.loginserver.network.gameserverpackets;

import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2jserver.L2DatabaseFactory;
import com.l2jserver.loginserver.GameServerTable;
import com.l2jserver.loginserver.GameServerTable.GameServerInfo;
import com.l2jserver.loginserver.GameServerThread;
import com.l2jserver.util.Base64;
import com.l2jserver.util.network.BaseRecievePacket;


/**
 * @author Nik
 */
public class ChangePassword extends BaseRecievePacket
{
	protected static Logger _log = Logger.getLogger(ChangePassword.class.getName());
	private static GameServerThread gst = null;
	
	public ChangePassword(byte[] decrypt)
	{
		super(decrypt);
		
		String accountName = readS();
		String characterName = readS();
		String curpass = readS();
		String newpass = readS();
		
		//get the GameServerThread
		Collection<GameServerInfo> serverList = GameServerTable.getInstance().getRegisteredGameServers().values();
		for (GameServerInfo gsi : serverList)
			if (gsi.getGameServerThread() != null && gsi.getGameServerThread().hasAccountOnGameServer(accountName))
				gst = gsi.getGameServerThread();
		
		if (gst == null)
			return;
		
		if (curpass == null || newpass == null)
			gst.ChangePasswordResponse((byte) 0, characterName, "Invalid password data! Try again.");
		else
		{
			Connection con = null;	
			try
			{
				MessageDigest md = MessageDigest.getInstance("SHA");
				
				byte[] raw = curpass.getBytes("UTF-8");
				raw = md.digest(raw);
				String curpassEnc = Base64.encodeBytes(raw);
				String pass = null;
				int passUpdated = 0;
				
				// SQL connection
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement("SELECT password FROM accounts WHERE login=?");
				statement.setString(1, accountName);
				ResultSet rset = statement.executeQuery();
				if (rset.next())
					pass = rset.getString("password");
				rset.close();
				statement.close();
				
				if (curpassEnc.equals(pass))
				{
					byte[] password = newpass.getBytes("UTF-8");
					password = md.digest(password);
					
					// SQL connection
					PreparedStatement ps = con.prepareStatement("UPDATE accounts SET password=? WHERE login=?");
					ps.setString(1, Base64.encodeBytes(password));
					ps.setString(2, accountName);
					passUpdated = ps.executeUpdate();
					ps.close();
					
					_log.log(Level.INFO, "The password for account " + accountName + " has been changed from " + curpassEnc + " to " + Base64.encodeBytes(password));
					if (passUpdated > 0)
						gst.ChangePasswordResponse((byte) 1, characterName, "You have successfully changed your password!");
					else
					{
						gst.ChangePasswordResponse((byte) 0, characterName, "The password change was unsuccessful!");
						L2DatabaseFactory.close(con);
					}
				}
				else
					gst.ChangePasswordResponse((byte) 0, characterName, "The typed current password doesn't match with your current one.");
			}
			catch (Exception e)
			{
				_log.warning("Error while changing password for account " + accountName + " requested by player " + characterName + "! " + e);
			}
			finally
			{
				// close the database connection at the end
				L2DatabaseFactory.close(con);
			}
		}
	}
}