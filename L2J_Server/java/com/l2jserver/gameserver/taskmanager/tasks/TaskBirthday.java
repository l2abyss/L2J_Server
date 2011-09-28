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
package com.l2jserver.gameserver.taskmanager.tasks;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2jserver.Config;
import com.l2jserver.L2DatabaseFactory;
import com.l2jserver.gameserver.datatables.CharNameTable;
import com.l2jserver.gameserver.instancemanager.MailManager;
import com.l2jserver.gameserver.model.entity.Message;
import com.l2jserver.gameserver.model.itemcontainer.Mail;
import com.l2jserver.gameserver.taskmanager.Task;
import com.l2jserver.gameserver.taskmanager.TaskManager;
import com.l2jserver.gameserver.taskmanager.TaskManager.ExecutedTask;
import com.l2jserver.gameserver.taskmanager.TaskTypes;
import com.l2jserver.gameserver.util.Util;

/**
 * @author Nyaran
 */
public class TaskBirthday extends Task
{
	private static final Logger _log = Logger.getLogger(TaskBirthday.class.getName());
	
	private static final String NAME = "birthday";
	
	private static final String QUERY = "SELECT charId, createDate FROM characters WHERE createDate LIKE ?";
	private static final Calendar _today = Calendar.getInstance();
	
	private int _count = 0;

	/* (non-Javadoc)
	 * @see com.l2jserver.gameserver.taskmanager.Task#getName()
	 */
	@Override
	public String getName()
	{
		return NAME;
	}

	/* (non-Javadoc)
	 * @see com.l2jserver.gameserver.taskmanager.Task#onTimeElapsed(com.l2jserver.gameserver.taskmanager.TaskManager.ExecutedTask)
	 */
	@Override
	public void onTimeElapsed(ExecutedTask task)
	{
		Calendar lastExecDate = Calendar.getInstance();
		long lastActivation = task.getLastActivation();
		
		if (lastActivation > 0)
			lastExecDate.setTimeInMillis(lastActivation);
		
		String rangeDate = "[" + Util.getDateString(lastExecDate.getTime()) + "] - [" + Util.getDateString(_today.getTime()) + "]";
		
		for(;!_today.before(lastExecDate);lastExecDate.add(Calendar.DATE, 1))
		{
			checkBirthday(lastExecDate.get(Calendar.YEAR), lastExecDate.get(Calendar.MONTH), lastExecDate.get(Calendar.DATE));
		}
		
		_log.info("BirthdayManager: " + _count + " gifts sent. " + rangeDate);
	}
	
	private void checkBirthday(int year, int month, int day)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(QUERY);
			statement.setString(1, "%-" + getNum(month + 1) + "-" + getNum(day));
			
			ResultSet rset = statement.executeQuery();
			
			while (rset.next())
			{
				int playerId = rset.getInt("charId");
				Calendar createDate = Calendar.getInstance();
				createDate.setTime(rset.getDate("createDate"));
				
				int age = year - createDate.get(Calendar.YEAR);
				
				if (age <= 0) // Player births this year
					continue;
				
				String text = Config.ALT_BIRTHDAY_MAIL_TEXT;
				
				if (text.contains("$c1"))
					text = text.replace("$c1", CharNameTable.getInstance().getNameById(playerId));
				if (text.contains("$s1"))
					text = text.replace("$s1", String.valueOf(age));
				
				Message msg = new Message(playerId, Config.ALT_BIRTHDAY_MAIL_SUBJECT, text, Message.SendBySystem.ALEGRIA);
				
				Mail attachments = msg.createAttachments();
				attachments.addItem("Birthday", Config.ALT_BIRTHDAY_GIFT, 1, null, null);
				
				MailManager.getInstance().sendMessage(msg);
				_count++;
			}
		}
		catch (SQLException e)
		{
			_log.log(Level.WARNING, "Error checking birthdays. ", e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
		
		// If character birthday is 29-Feb and year isn't leap, send gift on 28-feb
		GregorianCalendar calendar = new GregorianCalendar();
		if (month == Calendar.FEBRUARY && day == 28 && !calendar.isLeapYear(_today.get(Calendar.YEAR)))
			checkBirthday(year, Calendar.FEBRUARY, 29);
	}
	
	private String getNum(int num)
	{
		if (num <= 9)
			return "0" + num;
		
		return String.valueOf(num);
	}
	
	/**
	 * @see com.l2jserver.gameserver.taskmanager.Task#initializate()
	 */
	@Override
	public void initializate()
	{
		super.initializate();
		TaskManager.addUniqueTask(NAME, TaskTypes.TYPE_GLOBAL_TASK, "1", "06:30:00", "");
	}
}

