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
package com.l2jserver.loginserver.network.clientpackets;

import java.net.InetAddress;
import java.security.GeneralSecurityException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.Cipher;

import com.l2jserver.Config;
import com.l2jserver.loginserver.GameServerTable.GameServerInfo;
import com.l2jserver.loginserver.HackingException;
import com.l2jserver.loginserver.LoginController;
import com.l2jserver.loginserver.LoginController.AuthLoginResult;
import com.l2jserver.loginserver.network.L2LoginClient;
import com.l2jserver.loginserver.network.L2LoginClient.LoginClientState;
import com.l2jserver.loginserver.network.serverpackets.AccountKicked;
import com.l2jserver.loginserver.network.serverpackets.AccountKicked.AccountKickedReason;
import com.l2jserver.loginserver.network.serverpackets.LoginFail.LoginFailReason;
import com.l2jserver.loginserver.network.serverpackets.LoginOk;
import com.l2jserver.loginserver.network.serverpackets.ServerList;

/**
 * Format: x
 * 0 (a leading null)
 * x: the rsa encrypted block with the login an password
 */
public class RequestAuthLogin extends L2LoginClientPacket
{
	private static Logger _log = Logger.getLogger(RequestAuthLogin.class.getName());
	
	private final byte[] _raw = new byte[128];
	
	private String _user;
	private String _password;
	private int _ncotp;
	
	/**
	 * @return
	 */
	public String getPassword()
	{
		return _password;
	}
	
	/**
	 * @return
	 */
	public String getUser()
	{
		return _user;
	}
	
	public int getOneTimePassword()
	{
		return _ncotp;
	}
	
	@Override
	public boolean readImpl()
	{
		if (super._buf.remaining() >= 128)
		{
			readB(_raw);
			return true;
		}
		else
		{
			return false;
		}
	}
	
	@Override
	public void run()
	{
		byte[] decrypted = null;
		final L2LoginClient client = getClient();
		try
		{
			final Cipher rsaCipher = Cipher.getInstance("RSA/ECB/nopadding");
			rsaCipher.init(Cipher.DECRYPT_MODE, client.getRSAPrivateKey());
			decrypted = rsaCipher.doFinal(_raw, 0x00, 0x80);
		}
		catch (GeneralSecurityException e)
		{
			_log.log(Level.INFO, "", e);
			return;
		}
		
		try
		{
			_user = new String(decrypted, 0x5E, 14).trim().toLowerCase();
			_password = new String(decrypted, 0x6C, 16).trim();
			_ncotp = decrypted[0x7c];
			_ncotp |= decrypted[0x7d] << 8;
			_ncotp |= decrypted[0x7e] << 16;
			_ncotp |= decrypted[0x7f] << 24;
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "", e);
			return;
		}
		
		final LoginController lc = LoginController.getInstance();
		try
		{
			AuthLoginResult result = lc.tryAuthLogin(_user, _password, client);
			switch (result)
			{
				case AUTH_SUCCESS:
					client.setAccount(_user);
					client.setState(LoginClientState.AUTHED_LOGIN);
					client.setSessionKey(lc.assignSessionKeyToClient(_user, client));
					lc.getCharactersOnAccount(_user);
					if (Config.SHOW_LICENCE)
					{
						client.sendPacket(new LoginOk(getClient().getSessionKey()));
					}
					else
					{
						getClient().sendPacket(new ServerList(getClient()));
					}
					break;
				case INVALID_PASSWORD:
					client.close(LoginFailReason.REASON_USER_OR_PASS_WRONG);
					break;
				case ACCOUNT_BANNED:
					client.close(new AccountKicked(AccountKickedReason.REASON_PERMANENTLY_BANNED));
					break;
				case ALREADY_ON_LS:
					L2LoginClient oldClient;
					if ((oldClient = lc.getAuthedClient(_user)) != null)
					{
						// kick the other client
						oldClient.close(LoginFailReason.REASON_ACCOUNT_IN_USE);
						lc.removeAuthedLoginClient(_user);
					}
					// kick also current client
					client.close(LoginFailReason.REASON_ACCOUNT_IN_USE);
					break;
				case ALREADY_ON_GS:
					GameServerInfo gsi;
					if ((gsi = lc.getAccountOnGameServer(_user)) != null)
					{
						client.close(LoginFailReason.REASON_ACCOUNT_IN_USE);
						
						// kick from there
						if (gsi.isAuthed())
						{
							gsi.getGameServerThread().kickPlayer(_user);
						}
					}
					break;
			}
		}
		catch (HackingException e)
		{
			InetAddress address = getClient().getConnection().getInetAddress();
			lc.addBanForAddress(address, Config.LOGIN_BLOCK_AFTER_BAN * 1000);
			_log.info("Banned (" + address + ") for " + Config.LOGIN_BLOCK_AFTER_BAN + " seconds, due to " + e.getConnects() + " incorrect login attempts.");
		}
	}
}
