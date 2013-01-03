/*
 * Copyright (C) 2004-2013 L2J Server
 * 
 * This file is part of L2J Server.
 * 
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.l2jserver.tools.dbinstaller.console;

import java.sql.Connection;
import java.util.Scanner;
import java.util.prefs.Preferences;

import com.l2jserver.tools.dbinstaller.DBOutputInterface;
import com.l2jserver.tools.dbinstaller.RunTasks;
import com.l2jserver.tools.dbinstaller.util.mysql.MySqlConnect;
import com.l2jserver.util.CloseShieldedInputStream;

/**
 * @author mrTJO
 */
public class DBInstallerConsole implements DBOutputInterface
{
	Connection _con;
	
	public DBInstallerConsole(String db, String dir, String cleanUp)
	{
		System.out.println("Welcome to L2J DataBase installer");
		Preferences prop = Preferences.userRoot();
		RunTasks rt = null;
		try (Scanner scn = new Scanner(new CloseShieldedInputStream(System.in)))
		{
			while (_con == null)
			{
				System.out.printf("%s (%s): ", "Host", prop.get("dbHost_" + db, "localhost"));
				String dbHost = scn.nextLine();
				System.out.printf("%s (%s): ", "Port", prop.get("dbPort_" + db, "3306"));
				String dbPort = scn.nextLine();
				System.out.printf("%s (%s): ", "Username", prop.get("dbUser_" + db, "root"));
				String dbUser = scn.nextLine();
				System.out.printf("%s (%s): ", "Password", "");
				String dbPass = scn.nextLine();
				System.out.printf("%s (%s): ", "Database", prop.get("dbDbse_" + db, db));
				String dbDbse = scn.nextLine();
				
				dbHost = dbHost.isEmpty() ? prop.get("dbHost_" + db, "localhost") : dbHost;
				dbPort = dbPort.isEmpty() ? prop.get("dbPort_" + db, "3306") : dbPort;
				dbUser = dbUser.isEmpty() ? prop.get("dbUser_" + db, "root") : dbUser;
				dbDbse = dbDbse.isEmpty() ? prop.get("dbDbse_" + db, db) : dbDbse;
				
				MySqlConnect connector = new MySqlConnect(dbHost, dbPort, dbUser, dbPass, dbDbse, true);
				
				_con = connector.getConnection();
			}
			
			System.out.print("(C)lean install, (U)pdate or (E)xit? ");
			String resp = scn.next();
			if (resp.equalsIgnoreCase("c"))
			{
				System.out.print("Do you really want to destroy your db (Y/N)?");
				if (scn.next().equalsIgnoreCase("y"))
				{
					rt = new RunTasks(this, db, dir, cleanUp, true);
				}
			}
			else if (resp.equalsIgnoreCase("u"))
			{
				rt = new RunTasks(this, db, dir, cleanUp, false);
			}
		}
		
		if (rt != null)
		{
			rt.run();
		}
		else
		{
			System.exit(0);
		}
	}
	
	@Override
	public void appendToProgressArea(String text)
	{
		System.out.println(text);
	}
	
	@Override
	public Connection getConnection()
	{
		return _con;
	}
	
	@Override
	public void setProgressIndeterminate(boolean value)
	{
	}
	
	@Override
	public void setProgressMaximum(int maxValue)
	{
	}
	
	@Override
	public void setProgressValue(int value)
	{
	}
	
	@Override
	public void setFrameVisible(boolean value)
	{
	}
	
	@Override
	public int requestConfirm(String title, String message, int type)
	{
		System.out.print(message);
		String res = "";
		try (Scanner scn = new Scanner(new CloseShieldedInputStream(System.in)))
		{
			res = scn.next();
		}
		return res.equalsIgnoreCase("y") ? 0 : 1;
	}
	
	@Override
	public void showMessage(String title, String message, int type)
	{
		System.out.println(message);
	}
}
