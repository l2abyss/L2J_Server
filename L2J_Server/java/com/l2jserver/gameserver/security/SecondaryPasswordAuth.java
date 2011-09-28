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
package com.l2jserver.gameserver.security;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2jserver.Config;
import com.l2jserver.L2DatabaseFactory;
import com.l2jserver.gameserver.LoginServerThread;
import com.l2jserver.gameserver.network.L2GameClient;
import com.l2jserver.gameserver.network.serverpackets.Ex2ndPasswordAck;
import com.l2jserver.gameserver.network.serverpackets.Ex2ndPasswordCheck;
import com.l2jserver.gameserver.network.serverpackets.Ex2ndPasswordVerify;
import com.l2jserver.gameserver.util.Util;
import com.l2jserver.util.Base64;

/**
 * @author mrTJO
 */
public class SecondaryPasswordAuth
{
	private final Logger _log = Logger.getLogger(SecondaryPasswordAuth.class.getName());
	private final L2GameClient _activeClient;
	
	private String _password;
	private int _wrongAttempts;
	private boolean _authed;
	
	private static final String VAR_PWD = "secauth_pwd";
	private static final String VAR_WTE = "secauth_wte";
	
	private static final String SELECT_PASSWORD = "SELECT var, value FROM account_gsdata WHERE account_name=? AND var LIKE 'secauth_%'";
	private static final String INSERT_PASSWORD = "INSERT INTO account_gsdata VALUES (?, ?, ?)";
	private static final String UPDATE_PASSWORD = "UPDATE account_gsdata SET value=? WHERE account_name=? AND var=?";
	
	private static final String INSERT_ATTEMPT = "INSERT INTO account_gsdata VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE value=?";
	
	/**
	 * @param activeClient 
	 */
	public SecondaryPasswordAuth(L2GameClient activeClient)
	{
		_activeClient = activeClient;
		_password = null;
		_wrongAttempts = 0;
		_authed = false;
		loadPassword();
	}
	
	private void loadPassword()
	{
		String var, value = null;
		
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(SELECT_PASSWORD);
			statement.setString(1, _activeClient.getAccountName());
			ResultSet rs = statement.executeQuery();
			while (rs.next())
			{
				var = rs.getString("var");
				value = rs.getString("value");
				
				if (var.equals(VAR_PWD))
				{
					_password = value;
				}
				else if (var.equals(VAR_WTE))
				{
					_wrongAttempts = Integer.parseInt(value);
				}
			}
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Error while reading password.", e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
	}
	
	public boolean savePassword(String password)
	{
		if (passwordExist())
		{
			_log.warning("[SecondaryPasswordAuth]"+_activeClient.getAccountName()+" forced savePassword");
			_activeClient.closeNow();
			return false;
		}
		
		if (!validatePassword(password))
		{
			_activeClient.sendPacket(new Ex2ndPasswordAck(Ex2ndPasswordAck.WRONG_PATTERN));
			return false;
		}
		
		password = cryptPassword(password);
		
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(INSERT_PASSWORD);
			statement.setString(1, _activeClient.getAccountName());
			statement.setString(2, VAR_PWD);
			statement.setString(3, password);
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Error while writing password.", e);
			return false;
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
		_password = password;
		return true;
	}
	
	public boolean insertWrongAttempt(int attempts)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(INSERT_ATTEMPT);
			statement.setString(1, _activeClient.getAccountName());
			statement.setString(2, VAR_WTE);
			statement.setString(3, Integer.toString(attempts));
			statement.setString(4, Integer.toString(attempts));
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Error while writing wrong attempts.", e);
			return false;
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
		return true;
	}
	
	public boolean changePassword(String oldPassword, String newPassword)
	{
		if (!passwordExist())
		{
			_log.warning("[SecondaryPasswordAuth]"+_activeClient.getAccountName()+" forced changePassword");
			_activeClient.closeNow();
			return false;
		}
		
		if (!checkPassword(oldPassword, true))
			return false;
		
		if (!validatePassword(newPassword))
		{
			_activeClient.sendPacket(new Ex2ndPasswordAck(Ex2ndPasswordAck.WRONG_PATTERN));
			return false;
		}
		
		newPassword = cryptPassword(newPassword);
		
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(UPDATE_PASSWORD);
			statement.setString(1, newPassword);
			statement.setString(2, _activeClient.getAccountName());
			statement.setString(3, VAR_PWD);
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Error while reading password.", e);
			return false;
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
		_password = newPassword;
		_authed = false;
		return true;
	}
	
	public boolean checkPassword(String password, boolean skipAuth)
	{
		password = cryptPassword(password);
		
		if (!password.equals(_password))
		{
			_wrongAttempts++;
			if (_wrongAttempts < Config.SECOND_AUTH_MAX_ATTEMPTS)
			{
				_activeClient.sendPacket(new Ex2ndPasswordVerify(Ex2ndPasswordVerify.PASSWORD_WRONG, _wrongAttempts));
				insertWrongAttempt(_wrongAttempts);
			}
			else
			{
				LoginServerThread.getInstance().sendTempBan(_activeClient.getAccountName(), _activeClient.getConnectionAddress().getHostAddress(), Config.SECOND_AUTH_BAN_TIME);
				LoginServerThread.getInstance().sendMail(_activeClient.getAccountName(), "SATempBan", 
						_activeClient.getConnectionAddress().getHostAddress(), Integer.toString(Config.SECOND_AUTH_MAX_ATTEMPTS),
						Long.toString(Config.SECOND_AUTH_BAN_TIME), Config.SECOND_AUTH_REC_LINK);
				_log.warning(_activeClient.getAccountName()+" - ("+
						_activeClient.getConnectionAddress().getHostAddress()+") has inputted the wrong password "+
						_wrongAttempts+" times in row.");
				insertWrongAttempt(0);
				_activeClient.close(new Ex2ndPasswordVerify(Ex2ndPasswordVerify.PASSWORD_BAN, Config.SECOND_AUTH_MAX_ATTEMPTS));
			}
			return false;
		}
		if (!skipAuth)
		{
			_authed = true;
			_activeClient.sendPacket(new Ex2ndPasswordVerify(Ex2ndPasswordVerify.PASSWORD_OK, _wrongAttempts));
		}
		insertWrongAttempt(0);
		return true;
	}
	
	public boolean passwordExist()
	{
		return _password == null ? false : true;
	}
	
	public void openDialog()
	{
		if (passwordExist())
			_activeClient.sendPacket(new Ex2ndPasswordCheck(Ex2ndPasswordCheck.PASSWORD_PROMPT));
		else
			_activeClient.sendPacket(new Ex2ndPasswordCheck(Ex2ndPasswordCheck.PASSWORD_NEW));
	}
	
	public boolean isAuthed()
	{
		return _authed;
	}
	
	private String cryptPassword(String password)
	{
		try
		{
			MessageDigest md = MessageDigest.getInstance("SHA");
			byte[] raw = password.getBytes("UTF-8");
			byte[] hash = md.digest(raw);
			return Base64.encodeBytes(hash);
		}
		catch (NoSuchAlgorithmException e)
		{
			_log.severe("[SecondaryPasswordAuth]Unsupported Algorythm");
		}
		catch (UnsupportedEncodingException e)
		{
			_log.severe("[SecondaryPasswordAuth]Unsupported Encoding");
		}
		return null;
	}
	
	private boolean validatePassword(String password)
	{
		if (!Util.isDigit(password))
			return false;
		
		if (password.length() < 6 || password.length() > 8)
			return false;
		
		for (int i = 0; i < password.length()-1; i++)
		{
			char curCh = password.charAt(i);
			char nxtCh = password.charAt(i+1);
			
			if (curCh+1 == nxtCh)
				return false;
			else if (curCh-1 == nxtCh)
				return false;
			else if (curCh == nxtCh)
				return false;
		}
		
		for (int i = 0; i < password.length()-2; i++)
		{
			String toChk = password.substring(i+1);
			StringBuffer chkEr = new StringBuffer(password.substring(i, i+2));
			
			if (toChk.contains(chkEr))
				return false;
			else if (toChk.contains(chkEr.reverse()))
				return false;
		}
		_wrongAttempts = 0;
		return true;
	}
}