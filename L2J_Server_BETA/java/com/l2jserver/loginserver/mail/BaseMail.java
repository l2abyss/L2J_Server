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
package com.l2jserver.loginserver.mail;

import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;
import java.util.logging.Logger;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.l2jserver.Config;
import com.l2jserver.L2DatabaseFactory;
import com.l2jserver.loginserver.mail.MailSystem.MailContent;

/**
 * 
 * @author mrTJO
 */
public class BaseMail implements Runnable
{
	private final static Logger _log = Logger.getLogger(BaseMail.class.getName());
	
	private static final String QUERY_SEL_MAIL = "SELECT value FROM account_data " +
	"WHERE account_name=? AND var='email_addr'";
	
	private final Properties _mailProp = new Properties();
	private final SmtpAuthenticator _authenticator;
	private MimeMessage _messageMime = null;
	
	private class SmtpAuthenticator extends Authenticator
	{
		private final PasswordAuthentication _auth;
		
		public SmtpAuthenticator()
		{
			_auth = new PasswordAuthentication(Config.EMAIL_SYS_USERNAME, Config.EMAIL_SYS_PASSWORD);
		}
		
		@Override
		public PasswordAuthentication getPasswordAuthentication()
		{
			return _auth;
		}
	}
	
	public BaseMail(String account, String mailId, String... args)
	{
		_mailProp.put("mail.smtp.host", Config.EMAIL_SYS_HOST);
		_mailProp.put("mail.smtp.auth", Config.EMAIL_SYS_SMTP_AUTH);
		_mailProp.put("mail.smtp.port", Config.EMAIL_SYS_PORT);
		_mailProp.put("mail.smtp.socketFactory.port", Config.EMAIL_SYS_PORT);
		_mailProp.put("mail.smtp.socketFactory.class", Config.EMAIL_SYS_FACTORY);
		_mailProp.put("mail.smtp.socketFactory.fallback", Config.EMAIL_SYS_FACTORY_CALLBACK);
		
		_authenticator = Config.EMAIL_SYS_SMTP_AUTH ? new SmtpAuthenticator() : null;
		
		String mailAddr = getUserMail(account);
		
		if (mailAddr == null)
			return;
		
		MailContent content = MailSystem.getInstance().getMailContent(mailId);
		if (content == null)
			return;
		
		String message = compileHtml(account, content.getText(), args);
		
		Session mailSession = Session.getDefaultInstance(_mailProp, _authenticator);
		
		try
		{
			_messageMime = new MimeMessage(mailSession);
			_messageMime.setSubject(content.getSubject());
			try
			{
				_messageMime.setFrom(new InternetAddress(Config.EMAIL_SYS_ADDRESS, Config.EMAIL_SERVERINFO_NAME));
			}
			catch (UnsupportedEncodingException e)
			{
				_log.warning("Sender Address not Valid!");
			}
			_messageMime.setContent(message, "text/html");
			_messageMime.setRecipient(Message.RecipientType.TO, new InternetAddress(mailAddr));
		}
		catch (MessagingException e)
		{
			e.printStackTrace();
		}
	}
	
	
	
	private String compileHtml(String account, String html, String[] args)
	{
		if (args != null)
		{
			for (int i = 0; i < args.length; i++)
			{
				html = html.replace("%var"+i+"%", args[i]);
			}
		}
		html = html.replace("%accountname%", account);
		return html;
	}
	
	private String getUserMail(String username)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(QUERY_SEL_MAIL);
			statement.setString(1, username);
			ResultSet rset = statement.executeQuery();
			if (rset.next())
			{
				String mail = rset.getString("value");
				return mail;
			}
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("Cannot select user mail: Exception");
		}
		finally
		{
			try
			{
				L2DatabaseFactory.close(con);
			}
			catch (Exception e)
			{
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run()
	{
		try
		{
			if (_messageMime != null)
				Transport.send(_messageMime);
		}
		catch (MessagingException e)
		{
			_log.warning("Error encounterd while sending email");
		}
	}
}
