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
package com.l2jserver.gameserver.instancemanager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;
import com.l2jserver.L2DatabaseFactory;
import com.l2jserver.gameserver.ThreadPoolManager;
import com.l2jserver.gameserver.idfactory.IdFactory;
import com.l2jserver.gameserver.model.L2World;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.entity.Message;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.ExNoticePostArrived;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;

/**
 * @author Migi, DS<br>
 */
public class MailManager
{
	private static Logger _log = Logger.getLogger(MailManager.class.getName());

	private Map<Integer, Message> _messages = new FastMap<Integer, Message>();

	public static MailManager getInstance()
	{
		return SingletonHolder._instance;
	}

	private MailManager()
	{
		load();
	}

	private void load()
	{
		int readed = 0;
		Connection con = null;
		PreparedStatement stmt1 = null;
		PreparedStatement stmt2 = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();

			stmt1 = con.prepareStatement("SELECT * FROM messages ORDER BY expiration");
			stmt2 = con.prepareStatement("SELECT * FROM attachments WHERE messageId = ?");

			ResultSet rset1 = stmt1.executeQuery();
			while (rset1.next())
			{

				Message msg = new Message(rset1);

				int msgId = msg.getId();
				_messages.put(msgId, msg);

				readed++;

				long expiration = msg.getExpiration();

				if (expiration < System.currentTimeMillis())
					ThreadPoolManager.getInstance().scheduleGeneral(new MessageDeletionTask(msgId), 10000);
				else
					ThreadPoolManager.getInstance().scheduleGeneral(new MessageDeletionTask(msgId), expiration - System.currentTimeMillis());
			}
		}
		catch (SQLException e)
		{
			_log.warning("Mail Manager: Error loading from database:" + e.toString());
		}
		finally
		{
			try
			{
				stmt1.close();
			}
			catch (Exception e) {}
			try
			{
				stmt2.close();
			}
			catch (Exception e) {}
			try
			{
				con.close();
			}
			catch (Exception e) {}
		}
		_log.info("Mail Manager: Successfully loaded " + readed + " messages.");
	}

	public final Message getMessage(int msgId)
	{
		return _messages.get(msgId);
	}

	public final boolean hasUnreadPost(L2PcInstance player)
	{
		final int objectId = player.getObjectId();
		for (Message msg : _messages.values())
		{
			if (msg != null
					&& msg.getReceiverId() == objectId
					&& msg.isUnread())
				return true;
		}
		return false;
	}

	public final int getInboxSize(int objectId)
	{
		int size = 0;
		for (Message msg : _messages.values())
		{
			if (msg != null
					&& msg.getReceiverId() == objectId
					&& !msg.isDeletedByReceiver())
				size++;
		}
		return size;
	}

	public final int getOutboxSize(int objectId)
	{
		int size = 0;
		for (Message msg : _messages.values())
		{
			if (msg != null
					&& msg.getSenderId() == objectId
					&& !msg.isDeletedBySender())
				size++;
		}
		return size;
	}

	public final List<Message> getInbox(int objectId)
	{
		List<Message> inbox = new FastList<Message>();
		for (Message msg : _messages.values())
		{
			if (msg != null
					&& msg.getReceiverId() == objectId
					&& !msg.isDeletedByReceiver())
				inbox.add(msg);
		}
		return inbox;
	}

	public final List<Message> getOutbox(int objectId)
	{
		List<Message> outbox = new FastList<Message>();
		for (Message msg : _messages.values())
		{
			if (msg != null
					&& msg.getSenderId() == objectId
					&& !msg.isDeletedBySender())
				outbox.add(msg);
		}
		return outbox;
	}

	public void sendMessage(Message msg)
	{
		_messages.put(msg.getId(), msg);

		Connection con = null;
		PreparedStatement stmt = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			stmt = Message.getStatement(msg, con);
			stmt.execute();
			stmt.close();
		}
		catch (SQLException e)
		{
			_log.warning("Mail Manager: Error saving message:" + e.toString());
		}
		finally
		{
			try
			{
				stmt.close();
			}
			catch (Exception e) {}
			try
			{
				con.close();
			}
			catch (Exception e) {}
		}

		final L2PcInstance receiver = L2World.getInstance().getPlayer(msg.getReceiverId());
		if (receiver != null)
			receiver.sendPacket(ExNoticePostArrived.valueOf(true));

		ThreadPoolManager.getInstance().scheduleGeneral(new MessageDeletionTask(msg.getId()), msg.getExpiration() - System.currentTimeMillis());
	}

	class MessageDeletionTask implements Runnable
	{
		final int _msgId;

		public MessageDeletionTask(int msgId)
		{
			_msgId = msgId;
		}

		public void run()
		{
			final Message msg = getMessage(_msgId);
			if (msg == null)
				return;

			if (msg.hasAttachments())
			{
				try
				{
					final L2PcInstance sender = L2World.getInstance().getPlayer(msg.getSenderId());
					if (sender != null)
					{
						msg.getAttachments().returnToWh(sender.getWarehouse());
						sender.sendPacket(new SystemMessage(SystemMessageId.MAIL_RETURNED));
					}
					else
						msg.getAttachments().returnToWh(null);

					msg.getAttachments().deleteMe();
					msg.removeAttachments();

					final L2PcInstance receiver = L2World.getInstance().getPlayer(msg.getReceiverId());
					if (receiver != null)
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.MAIL_RETURNED);
						sm.addString(msg.getReceiverName());
						receiver.sendPacket(sm);
					}
				}
				catch (Exception e)
				{
					_log.warning("Mail Manager: Error returning items:" + e.toString());
				}
			}
			deleteMessageInDb(msg.getId());
		}
	}

	public final void markAsReadInDb(int msgId)
	{
		Connection con = null;
		PreparedStatement stmt = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			stmt = con.prepareStatement("UPDATE messages SET isUnread = 'false' WHERE messageId = ?");
			stmt.setInt(1, msgId);
			stmt.execute();			
		}
		catch (SQLException e)
		{
			_log.warning("Mail Manager: Error marking as read message:" + e.toString());
		}
		finally
		{
			try
			{
				stmt.close();
			}
			catch (Exception e) {}
			try
			{
				con.close();
			}
			catch (Exception e) {}
		}
	}

	public final void markAsDeletedBySenderInDb(int msgId)
	{
		Connection con = null;
		PreparedStatement stmt = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();

			stmt = con.prepareStatement("UPDATE messages SET isDeletedBySender = 'true' WHERE messageId = ?");

			stmt.setInt(1, msgId);

			stmt.execute();
			stmt.close();
		}
		catch (SQLException e)
		{
			_log.warning("Mail Manager: Error marking as deleted by sender message:" + e.toString());
		}
		finally
		{
			try
			{
				stmt.close();
			}
			catch (Exception e) {}
			try
			{
				con.close();
			}
			catch (Exception e) {}
		}
	}

	public final void markAsDeletedByReceiverInDb(int msgId)
	{
		Connection con = null;
		PreparedStatement stmt = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();

			stmt = con.prepareStatement("UPDATE messages SET isDeletedByReceiver = 'true' WHERE messageId = ?");

			stmt.setInt(1, msgId);

			stmt.execute();
			stmt.close();
		}
		catch (SQLException e)
		{
			_log.warning("Mail Manager: Error marking as deleted by receiver message:" + e.toString());
		}
		finally
		{
			try
			{
				stmt.close();
			}
			catch (Exception e) {}
			try
			{
				con.close();
			}
			catch (Exception e) {}
		}
	}

	public final void removeAttachmentsInDb(int msgId)
	{
		Connection con = null;
		PreparedStatement stmt = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();

			stmt = con.prepareStatement("UPDATE messages SET hasAttachments = 'false' WHERE messageId = ?");

			stmt.setInt(1, msgId);

			stmt.execute();
			stmt.close();
		}
		catch (SQLException e)
		{
			_log.warning("Mail Manager: Error removing attachments in message:" + e.toString());
		}
		finally
		{
			try
			{
				stmt.close();
			}
			catch (Exception e) {}
			try
			{
				con.close();
			}
			catch (Exception e) {}
		}
	}

	public final void deleteMessageInDb(int msgId)
	{
		Connection con = null;
		PreparedStatement stmt = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();

			stmt = con.prepareStatement("DELETE FROM messages WHERE messageId = ?");

			stmt.setInt(1, msgId);

			stmt.execute();
			stmt.close();
		}
		catch (SQLException e)
		{
			_log.warning("Mail Manager: Error deleting message:" + e.toString());
		}
		finally
		{
			try
			{
				stmt.close();
			}
			catch (Exception e) {}
			try
			{
				con.close();
			}
			catch (Exception e) {}
		}

		_messages.remove(msgId);
		IdFactory.getInstance().releaseId(msgId);
	}

	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final MailManager _instance = new MailManager();
	}
}