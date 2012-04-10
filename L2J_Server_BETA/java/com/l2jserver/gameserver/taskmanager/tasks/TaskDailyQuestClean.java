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

import com.l2jserver.L2DatabaseFactory;
import com.l2jserver.gameserver.taskmanager.Task;
import com.l2jserver.gameserver.taskmanager.TaskManager;
import com.l2jserver.gameserver.taskmanager.TaskManager.ExecutedTask;
import com.l2jserver.gameserver.taskmanager.TaskTypes;

/**
 * @author Gnacik
 */
public class TaskDailyQuestClean extends Task
{
	private static final String NAME = "daily_quest_clean";
	
	private static final String[] _daily_names =
	{
		"463_IMustBeaGenius",
		"464_Oath",
		"458_PerfectForm",
		"461_RumbleInTheBase",
		"551_OlympiadStarter",
		"552_OlympiadVeteran",
		"553_OlympiadUndefeated"
	};
	
	@Override
	public String getName()
	{
		return NAME;
	}
	
	@Override
	public void onTimeElapsed(ExecutedTask task)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			for (String name : _daily_names)
			{
				PreparedStatement statement = con.prepareStatement("DELETE FROM character_quests WHERE name=? AND var='<state>' AND value='Completed';");
				statement.setString(1, name);
				statement.execute();
				statement.close();
			}
		}
		catch (Exception e)
		{
			_log.severe(getClass().getSimpleName() + ": Could not reset daily quests: " + e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
		_log.info("Daily quests cleared");
	}
	
	@Override
	public void initializate()
	{
		super.initializate();
		TaskManager.addUniqueTask(NAME, TaskTypes.TYPE_GLOBAL_TASK, "1", "06:30:00", "");
	}
}
