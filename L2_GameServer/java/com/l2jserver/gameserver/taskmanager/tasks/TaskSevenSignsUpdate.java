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

import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2jserver.gameserver.SevenSigns;
import com.l2jserver.gameserver.SevenSignsFestival;
import com.l2jserver.gameserver.taskmanager.Task;
import com.l2jserver.gameserver.taskmanager.TaskManager;
import com.l2jserver.gameserver.taskmanager.TaskManager.ExecutedTask;
import com.l2jserver.gameserver.taskmanager.TaskTypes;


/**
 * Updates all data for the Seven Signs and Festival of Darkness engines, when
 * time is elapsed.
 * 
 * @author Tempy
 */
public class TaskSevenSignsUpdate extends Task
{
	private static final Logger _log = Logger.getLogger(TaskSevenSignsUpdate.class.getName());
	public static final String NAME = "seven_signs_update";
	
	/**
	 * 
	 * @see com.l2jserver.gameserver.taskmanager.Task#getName()
	 */
	@Override
	public String getName()
	{
		return NAME;
	}
	
	/**
	 * 
	 * @see com.l2jserver.gameserver.taskmanager.Task#onTimeElapsed(com.l2jserver.gameserver.taskmanager.TaskManager.ExecutedTask)
	 */
	@Override
	public void onTimeElapsed(ExecutedTask task)
	{
		try
		{
			SevenSigns.getInstance().saveSevenSignsStatus();
			
			if (!SevenSigns.getInstance().isSealValidationPeriod())
				SevenSignsFestival.getInstance().saveFestivalData(false);
			
			_log.info("SevenSigns: Data updated successfully.");
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "SevenSigns: Failed to save Seven Signs configuration: " + e.getMessage(), e);
		}
	}
	
	/**
	 * 
	 * @see com.l2jserver.gameserver.taskmanager.Task#initializate()
	 */
	@Override
	public void initializate()
	{
		super.initializate();
		TaskManager.addUniqueTask(NAME, TaskTypes.TYPE_FIXED_SHEDULED, "1800000", "1800000", "");
	}
}
