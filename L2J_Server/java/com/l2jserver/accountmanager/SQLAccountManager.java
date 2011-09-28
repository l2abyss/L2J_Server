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
package com.l2jserver.accountmanager;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

import javolution.util.FastList;

import com.l2jserver.Config;
import com.l2jserver.L2DatabaseFactory;
import com.l2jserver.Server;
import com.l2jserver.ngl.ConsoleLocalizator;
import com.l2jserver.ngl.LocaleCodes;
import com.l2jserver.util.Base64;

/**
 * This class SQL Account Manager
 *
 * @author netimperia
 * @version $Revision: 2.3.2.1.2.3 $ $Date: 2005/08/08 22:47:12 $
 */
public class SQLAccountManager
{
	private static String _uname = "";
	private static String _pass = "";
	private static String _level = "";
	private static String _mode = "";
	private static ConsoleLocalizator cl;
	
	public static void main(String[] args) throws SQLException, IOException, NoSuchAlgorithmException
	{
		Server.serverMode = Server.MODE_LOGINSERVER;
		Config.load();
		if (args.length > 0)
		{
			if (LocaleCodes.getInstance().getLanguage(args[0]) != null)
				cl = new ConsoleLocalizator("accountmanager", "SQLAccountManager", LocaleCodes.getInstance().getLanguage(args[0]));
			else
				cl = new ConsoleLocalizator("accountmanager", "SQLAccountManager", args[0]);
		}
		else
			cl = new ConsoleLocalizator("accountmanager", "SQLAccountManager", Locale.getDefault());
		
		while (true)
		{
			cl.println("functChooser");
			cl.println();
			cl.println("functCreateAccount");
			cl.println("functAccessLevel");
			cl.println("functDeleteAccount");
			cl.println("functListAccount");
			cl.println("functExit");
			while (!(_mode.equals("1") || _mode.equals("2") || _mode.equals("3") || _mode.equals("4") || _mode.equals("5")))
			{
				_mode = cl.inputString("inputChoice");
			}
			
			if (_mode.equals("1") || _mode.equals("2") || _mode.equals("3"))
			{
				if (_mode.equals("1") || _mode.equals("2"))
				{
					while (_uname.trim().length() == 0)
					{
						_uname = cl.inputString("inputUsername").toLowerCase();
					}
				}
				else if (_mode.equals("3"))
				{
					while (_uname.trim().length() == 0)
					{
						_uname = cl.inputString("inputUsername").toLowerCase();
					}
				}
				if (_mode.equals("1"))
				{
					while (_pass.trim().length() == 0)
					{
						_pass = cl.inputString("inputPassword");
					}
				}
				if (_mode.equals("1") || _mode.equals("2"))
				{
					while (_level.trim().length() == 0)
					{
						_level = cl.inputString("inputAccessLevel");
					}
				}
			}
			
			if (_mode.equals("1"))
			{
				// Add or Update
				addOrUpdateAccount(_uname.trim(), _pass.trim(), _level.trim());
			}
			else if (_mode.equals("2"))
			{
				// Change Level
				changeAccountLevel(_uname.trim(), _level.trim());
			}
			else if (_mode.equals("3"))
			{
				// Delete
				String yesno = cl.inputString("functDeleteAccountConfirm");
				if (yesno != null && yesno.equalsIgnoreCase(cl.getString("yesChar")))
					deleteAccount(_uname.trim());
				else
					cl.println("functDeleteAccountCancel");
			}
			else if (_mode.equals("4"))
			{
				// List
				_mode = "";
				cl.println();
				cl.println("functListAccountChooser");
				cl.println();
				cl.println("functListAccountBanned");
				cl.println("functListAccountPrivileged");
				cl.println("functListAccountRegular");
				cl.println("functListAccountAll");
				while (!(_mode.equals("1") || _mode.equals("2") || _mode.equals("3") || _mode.equals("4")))
				{
					_mode = cl.inputString("inputChoice");
				}
				cl.println();
				printAccInfo(_mode);
			}
			else if (_mode.equals("5"))
			{
				System.exit(0);
			}
			
			_uname = "";
			_pass = "";
			_level = "";
			_mode = "";
			cl.println();
		}
	}
	
	private static void printAccInfo(String m) throws SQLException
	{
		int count = 0;
		Connection con = null;
		con = L2DatabaseFactory.getInstance().getConnection();
		String q = "SELECT login, accessLevel FROM accounts ";
		if (m.equals("1"))
			q = q.concat("WHERE accessLevel < 0");
		else if (m.equals("2"))
			q = q.concat("WHERE accessLevel > 0");
		else if (m.equals("3"))
			q = q.concat("WHERE accessLevel = 0");
		q = q.concat(" ORDER BY login ASC");
		
		PreparedStatement statement = con.prepareStatement(q);
		ResultSet rset = statement.executeQuery();
		while (rset.next())
		{
			System.out.println(rset.getString("login") + " -> " + rset.getInt("accessLevel"));
			count++;
		}
		rset.close();
		statement.close();
		L2DatabaseFactory.close(con);
		cl.println("functListAccountDisplayed", count);
	}
	
	private static void addOrUpdateAccount(String account, String password, String level) throws IOException, SQLException, NoSuchAlgorithmException
	{
		// Encode Password
		MessageDigest md = MessageDigest.getInstance("SHA");
		byte[] newpass;
		newpass = password.getBytes("UTF-8");
		newpass = md.digest(newpass);
		
		// Add to Base
		Connection con = null;
		con = L2DatabaseFactory.getInstance().getConnection();
		PreparedStatement statement = con.prepareStatement("REPLACE accounts (login, password, accessLevel) VALUES (?,?,?)");
		statement.setString(1, account);
		statement.setString(2, Base64.encodeBytes(newpass));
		statement.setString(3, level);
		statement.executeUpdate();
		statement.close();
		L2DatabaseFactory.close(con);
	}
	
	private static void changeAccountLevel(String account, String level) throws SQLException
	{
		Connection con = null;
		con = L2DatabaseFactory.getInstance().getConnection();
		
		// Check Account Exist
		PreparedStatement statement = con.prepareStatement("SELECT COUNT(*) FROM accounts WHERE login=?;");
		statement.setString(1, account);
		ResultSet rset = statement.executeQuery();
		if (!rset.next())
		{
			cl.println("falseString");
		}
		else if (rset.getInt(1) > 0)
		{
			// Exist
			// Update
			statement = con.prepareStatement("UPDATE accounts SET accessLevel=? WHERE login=?;");
			statement.setEscapeProcessing(true);
			statement.setString(1, level);
			statement.setString(2, account);
			statement.executeUpdate();
			
			cl.println("functAccessLevelUpdated", account);
		}
		else
		{
			// Not Exist
			cl.println("functAccessLevelNotExist", account);
		}
		rset.close();
		statement.close();
		L2DatabaseFactory.close(con);
	}
	
	private static void deleteAccount(String account) throws SQLException
	{
		Connection con = null;
		con = L2DatabaseFactory.getInstance().getConnection();
		
		// Check Account Exist
		PreparedStatement statement = con.prepareStatement("SELECT COUNT(*) FROM accounts WHERE login=?;");
		statement.setString(1, account);
		ResultSet rset = statement.executeQuery();
		if (!rset.next())
		{
			cl.println("falseString");
			rset.close();
		}
		else if (rset.getInt(1) > 0)
		{
			rset.close();
			// Account exist
			// Get Accounts ID
			ResultSet rcln;
			statement = con.prepareStatement("SELECT charId, char_name, clanid FROM characters WHERE account_name=?;");
			statement.setEscapeProcessing(true);
			statement.setString(1, account);
			rset = statement.executeQuery();
			
			FastList<String> objIds = new FastList<String>();
			FastList<String> charNames = new FastList<String>();
			FastList<String> clanIds = new FastList<String>();
			
			while (rset.next())
			{
				objIds.add(rset.getString("charId"));
				charNames.add(rset.getString("char_name"));
				clanIds.add(rset.getString("clanid"));
			}
			rset.close();
			
			for (int index = 0; index < objIds.size(); index++)
			{
				cl.println("functDeleteAccountChar", charNames.get(index));
				
				// Check If clan leader Remove Clan and remove all from it
				statement.close();
				statement = con.prepareStatement("SELECT COUNT(*) FROM clan_data WHERE leader_id=?;");
				statement.setString(1, clanIds.get(index));
				rcln = statement.executeQuery();
				rcln.next();
				if (rcln.getInt(1) > 0)
				{
					rcln.close();
					// Clan Leader
					
					// Get Clan Name
					statement.close();
					statement = con.prepareStatement("SELECT clan_name FROM clan_data WHERE leader_id=?;");
					statement.setString(1, clanIds.get(index));
					rcln = statement.executeQuery();
					rcln.next();
					
					String clanName = rcln.getString("clan_name");
					
					cl.println("functDeleteAccountClan", clanName);
					
					// Delete Clan Wars
					statement.close();
					statement = con.prepareStatement("DELETE FROM clan_wars WHERE clan1=? OR clan2=?;");
					statement.setEscapeProcessing(true);
					statement.setString(1, clanName);
					statement.setString(2, clanName);
					statement.executeUpdate();
					
					rcln.close();
					
					// Remove All From clan
					statement.close();
					statement = con.prepareStatement("UPDATE characters SET clanid=0 WHERE clanid=?;");
					statement.setString(1, clanIds.get(index));
					statement.executeUpdate();
					// Free Clan Halls
					statement.close();
					statement = con.prepareStatement("UPDATE clanhall SET ownerId=0, paidUntil=0, paid=0 WHERE ownerId=?;");
					statement.setString(1, clanIds.get(index));
					statement.executeUpdate();
					// Delete Clan
					statement.close();
					statement = con.prepareStatement("DELETE FROM clan_data WHERE clan_id=?;");
					statement.setString(1, clanIds.get(index));
					statement.executeUpdate();
					//Clan privileges
					statement.close();
					statement = con.prepareStatement("DELETE FROM clan_privs WHERE clan_id=?;");
					statement.setString(1, clanIds.get(index));
					statement.executeUpdate();
					//Clan subpledges
					statement.close();
					statement = con.prepareStatement("DELETE FROM clan_subpledges WHERE clan_id=?;");
					statement.setString(1, clanIds.get(index));
					statement.executeUpdate();
					//Clan skills
					statement.close();
					statement = con.prepareStatement("DELETE FROM clan_skills WHERE clan_id=?;");
					statement.setString(1, clanIds.get(index));
					statement.executeUpdate();
					
				}
				else
					rcln.close();
				
				// skills
				statement.close();
				statement = con.prepareStatement("DELETE FROM character_skills WHERE charId=?;");
				statement.setString(1, objIds.get(index));
				statement.executeUpdate();
				
				// skills save
				statement.close();
				statement = con.prepareStatement("DELETE FROM character_skills_save WHERE charId=?;");
				statement.setString(1, objIds.get(index));
				statement.executeUpdate();
				
				// subclasses
				statement.close();
				statement = con.prepareStatement("DELETE FROM character_subclasses WHERE charId=?;");
				statement.setString(1, objIds.get(index));
				statement.executeUpdate();
				
				// shortcuts
				statement.close();
				statement = con.prepareStatement("DELETE FROM character_shortcuts WHERE charId=?;");
				statement.setString(1, objIds.get(index));
				statement.executeUpdate();
				
				// items
				statement.close();
				statement = con.prepareStatement("DELETE FROM items WHERE owner_id=?;");
				statement.setString(1, objIds.get(index));
				statement.executeUpdate();
				
				// recipebook
				statement.close();
				statement = con.prepareStatement("DELETE FROM character_recipebook WHERE charId=?;");
				statement.setString(1, objIds.get(index));
				statement.executeUpdate();
				
				// quests
				statement.close();
				statement = con.prepareStatement("DELETE FROM character_quests WHERE charId=?;");
				statement.setString(1, objIds.get(index));
				statement.executeUpdate();
				
				// macroses
				statement.close();
				statement = con.prepareStatement("DELETE FROM character_macroses WHERE charId=?;");
				statement.setString(1, objIds.get(index));
				statement.executeUpdate();
				
				// contacts
				statement.close();
				statement = con.prepareStatement("DELETE FROM character_contacts WHERE charId=? OR contactId=?;");
				statement.setString(1, objIds.get(index));
				statement.setString(2, objIds.get(index));
				statement.executeUpdate();
				
				// friends
				statement.close();
				statement = con.prepareStatement("DELETE FROM character_friends WHERE charId=? OR friendId=?;");
				statement.setString(1, objIds.get(index));
				statement.setString(2, objIds.get(index));
				statement.executeUpdate();
				
				// merchant_lease
				statement.close();
				statement = con.prepareStatement("DELETE FROM merchant_lease WHERE player_id=?;");
				statement.setString(1, objIds.get(index));
				statement.executeUpdate();
				
				// boxaccess
				statement.close();
				statement = con.prepareStatement("DELETE FROM boxaccess WHERE charname=?;");
				statement.setString(1, charNames.get(index));
				statement.executeUpdate();
				
				// hennas
				statement.close();
				statement = con.prepareStatement("DELETE FROM character_hennas WHERE charId=?;");
				statement.setString(1, objIds.get(index));
				statement.executeUpdate();
				
				// recommends
				statement.close();
				statement = con.prepareStatement("DELETE FROM character_reco_bonus WHERE charId=?;");
				statement.setString(1, objIds.get(index));
				statement.executeUpdate();
				
				// ui categories
				statement.close();
				statement = con.prepareStatement("DELETE FROM character_ui_categories WHERE charId=?;");
				statement.setString(1, objIds.get(index));
				statement.executeUpdate();
				
				// ui keys
				statement.close();
				statement = con.prepareStatement("DELETE FROM character_ui_keys WHERE charId=?;");
				statement.setString(1, objIds.get(index));
				statement.executeUpdate();
				
				// characters
				statement.close();
				statement = con.prepareStatement("DELETE FROM characters WHERE charId=?;");
				statement.setString(1, objIds.get(index));
				statement.executeUpdate();
				
				// TODO: delete pets, olympiad/noble/hero stuff
			}
			
			// characters
			statement.close();
			statement = con.prepareStatement("DELETE FROM account_gsdata WHERE account_name=?;");
			statement.setString(1, account);
			statement.executeUpdate();
			
			// Delete Account
			statement.close();
			statement = con.prepareStatement("DELETE FROM accounts WHERE login=?;");
			statement.setEscapeProcessing(true);
			statement.setString(1, account);
			statement.executeUpdate();
			
			cl.println("functDeleteAccountComplete", account);
		}
		else
		{
			// Not Exist
			cl.println("functDeleteAccountNotExist", account);
		}
		
		// Close Connection
		statement.close();
		L2DatabaseFactory.close(con);
	}
	
}
