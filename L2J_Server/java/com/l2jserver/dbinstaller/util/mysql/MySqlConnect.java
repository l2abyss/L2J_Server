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
package com.l2jserver.dbinstaller.util.mysql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Formatter;

import javax.swing.JOptionPane;

/**
 * 
 * @author mrTJO
 */
public class MySqlConnect
{
	Connection con = null;
	
	public MySqlConnect(String host, String port, String user, String password, String db, boolean console)
	{
		try
		{
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			con = DriverManager.getConnection(
				new Formatter().format("jdbc:mysql://%1$s:%2$s", host, port).toString(),
				user, password);
			
			Statement st = con.createStatement();
			st.execute("CREATE DATABASE IF NOT EXISTS `"+db+"`");
			st.execute("USE `"+db+"`");
			st.close();
		}
		catch (SQLException e)
		{
			if (console)
				e.printStackTrace();
			else
				JOptionPane.showMessageDialog(null, "MySQL Error: "+e.getMessage(),
					"Connection Error", JOptionPane.ERROR_MESSAGE);
		}
		catch (InstantiationException e)
		{
			if (console)
				e.printStackTrace();
			else
				JOptionPane.showMessageDialog(null, "Instantiation Exception: "+e.getMessage(),
					"Connection Error", JOptionPane.ERROR_MESSAGE);
		}
		catch (IllegalAccessException e)
		{
			if (console)
				e.printStackTrace();
			else
				JOptionPane.showMessageDialog(null, "Illegal Access: "+e.getMessage(), "Connection Error",
					JOptionPane.ERROR_MESSAGE);
		}
		catch (ClassNotFoundException e)
		{
			if (console)
				e.printStackTrace();
			else
				JOptionPane.showMessageDialog(null, "Cannot find MySQL Connector: "+e.getMessage(),
					"Connection Error", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	public Connection getConnection()
	{
		return con;
	}
	
	public Statement getStatement()
	{
		try
		{
			return con.createStatement();
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			System.out.println("Statement Null");
			return null;
		}
	}
}
