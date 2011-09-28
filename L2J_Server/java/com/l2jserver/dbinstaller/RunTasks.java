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
package com.l2jserver.dbinstaller;

import java.io.File;
import java.sql.SQLException;
import java.util.prefs.Preferences;

import javax.swing.JOptionPane;

import com.l2jserver.dbinstaller.util.mysql.DBDumper;
import com.l2jserver.dbinstaller.util.mysql.ScriptExecutor;
import com.l2jserver.dbinstaller.util.mysql.ScriptExecutor.SqlFileFilter;

/**
 * 
 * @author mrTJO
 */
public class RunTasks extends Thread
{
	DBOutputInterface _frame;
	boolean _cleanInstall;
	String _db;
	String _sqlDir;
	String _cleanUpFile;
	
	public RunTasks(DBOutputInterface frame, String db, String sqlDir, String cleanUpFile,
			boolean cleanInstall)
	{
		_frame = frame;
		_db = db;
		_cleanInstall = cleanInstall;
		_sqlDir = sqlDir;
		_cleanUpFile = cleanUpFile;
	}
	
	@Override
	public void run()
	{
		new DBDumper(_frame, _db);
		ScriptExecutor exec = new ScriptExecutor(_frame);
		
		File clnFile = new File(_cleanUpFile);
		File updDir = new File(_sqlDir, "updates");
		File[] files = updDir.listFiles(new SqlFileFilter());
		
		Preferences prefs = Preferences.userRoot();
		
		if (_cleanInstall)
		{
			if (clnFile.exists())
			{
				_frame.appendToProgressArea("Cleaning Database...");
				exec.execSqlFile(clnFile);
				_frame.appendToProgressArea("Database Cleaned!");
			}
			else
				_frame.appendToProgressArea("Database Cleaning Script Not Found!");
			
			if (updDir.exists())
			{
				StringBuilder sb = new StringBuilder();
				for (File cf : files)
					sb.append(cf.getName()+';');
				prefs.put(_db+"_upd", sb.toString());
			}
		}
		else
		{
			if (!_cleanInstall && updDir.exists())
			{
				_frame.appendToProgressArea("Installing Updates...");
				
				for (File cf : files)
				{
					if (!prefs.get(_db+"_upd", "").contains(cf.getName()))
					{
						exec.execSqlFile(cf, true);
						prefs.put(_db+"_upd", prefs.get(_db+"_upd", "")+cf.getName()+";");
					}
				}
				_frame.appendToProgressArea("Database Updates Installed!");
			}
		}
		
		_frame.appendToProgressArea("Installing Database Content...");
		exec.execSqlBatch(new File(_sqlDir));
		_frame.appendToProgressArea("Database Installation Complete!");
		
		File cusDir = new File(_sqlDir, "custom");
		if (cusDir.exists())
		{
			int ch = _frame.requestConfirm("Install Custom",
					"Do you want to install custom tables?", JOptionPane.YES_NO_OPTION);
			if (ch == 0)
			{
				_frame.appendToProgressArea("Installing Custom Tables...");
				exec.execSqlBatch(cusDir);
				_frame.appendToProgressArea("Custom Tables Installed!");
			}
		}
		
		File modDir = new File(_sqlDir, "mods");
		if (modDir.exists())
		{
			int ch = _frame.requestConfirm("Install Mods",
					"Do you want to install mod tables?", JOptionPane.YES_NO_OPTION);
			if (ch == 0)
			{
				_frame.appendToProgressArea("Installing Mods Tables...");
				exec.execSqlBatch(modDir);
				_frame.appendToProgressArea("Mods Tables Installed!");
			}
		}
		
		try
		{
			_frame.getConnection().close();
		}
		catch (SQLException e)
		{
			JOptionPane.showMessageDialog(null, "Cannot close MySQL Connection: "+e.getMessage(),
					"Connection Error", JOptionPane.ERROR_MESSAGE);
		}
		
		_frame.setFrameVisible(false);
		_frame.showMessage("Done!", "Database Installation Complete!",
				JOptionPane.INFORMATION_MESSAGE);
		System.exit(0);
		
	}
	
}
