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

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.l2jserver.dbinstaller.DBOutputInterface;
import com.l2jserver.dbinstaller.util.FileWriterStdout;

/**
 * 
 * @author mrTJO
 */
public class DBDumper
{
	DBOutputInterface _frame;
	String _db;
	
	public DBDumper(DBOutputInterface frame, String db)
	{
		_frame = frame;
		_db = db;
		createDump();
	}
	
	public void createDump()
	{
		try
		{
			Connection con = _frame.getConnection();
			Formatter form = new Formatter();
			PreparedStatement stmt = con.prepareStatement("SHOW TABLES");
			ResultSet rset = stmt.executeQuery();
			File dump = new File("dumps", form.format("%1$s_dump_%2$tY%2$tm%2$td-%2$tH%2$tM%2$tS.sql",
					_db, new GregorianCalendar().getTime()).toString());
			new File("dumps").mkdir();
			dump.createNewFile();
			
			_frame.appendToProgressArea("Writing dump "+dump.getName());
			if (rset.last())
			{
				int rows = rset.getRow();
				rset.beforeFirst();
				if (rows > 0)
				{
					_frame.setProgressIndeterminate(false);
					_frame.setProgressMaximum(rows);
				}
			}
			
			FileWriterStdout ps = new FileWriterStdout(dump);
			while (rset.next())
			{
				_frame.setProgressValue(rset.getRow());
				_frame.appendToProgressArea("Dumping Table "+rset.getString(1));
				ps.println("CREATE TABLE `"+rset.getString(1)+"`");
				ps.println("(");
				PreparedStatement desc = con.prepareStatement("DESC "+rset.getString(1));
				ResultSet dset = desc.executeQuery();
				Map<String, List<String>> keys = new HashMap<String, List<String>>();
				boolean isFirst = true;
				while (dset.next())
				{
					if (!isFirst) ps.println(",");
					ps.print("\t`"+dset.getString(1)+"`");
					ps.print(" "+dset.getString(2));
					if (dset.getString(3).equals("NO"))
						ps.print(" NOT NULL");
					if (!dset.getString(4).isEmpty())
					{
						if (!keys.containsKey(dset.getString(4)))
							keys.put(dset.getString(4), new ArrayList<String>());
						keys.get(dset.getString(4)).add(dset.getString(1));
					}
					if (dset.getString(5) != null)
						ps.print(" DEFAULT '"+dset.getString(5)+"'");
					if (!dset.getString(6).isEmpty())
						ps.print(" "+dset.getString(6));
					isFirst = false;
				}
				if (keys.containsKey("PRI"))
				{
					ps.println(",");
					ps.print("\tPRIMARY KEY (");
					isFirst = true;
					for (String key : keys.get("PRI"))
					{
						if (!isFirst) ps.print(", ");
						ps.print("`"+key+"`");
						isFirst = false;
					}
					ps.print(")");
				}
				if (keys.containsKey("MUL"))
				{
					ps.println(",");
					isFirst = true;
					for (String key : keys.get("MUL"))
					{
						if (!isFirst) ps.println(", ");
						ps.print("\tKEY `key_"+key+"` (`"+key+"`)");
						isFirst = false;
					}
				}
				ps.println();
				ps.println(");");
				ps.flush();
				dset.close();
				desc.close();
				
				desc = con.prepareStatement("SELECT * FROM "+rset.getString(1));
				dset = desc.executeQuery();
				isFirst = true;
				int cnt = 0;
				while (dset.next())
				{
					if ((cnt%100) == 0)
						ps.println("INSERT INTO `"+rset.getString(1)+"` VALUES ");
					else
						ps.println(",");
					
					ps.print("\t(");
					boolean isInFirst = true;
					for (int i = 1; i <= dset.getMetaData().getColumnCount(); i++)
					{
						if (!isInFirst)
							ps.print(", ");
						
						if (dset.getString(i) == null)
							ps.print("NULL");
						else
							ps.print("'"+dset.getString(i).replace("\'", "\\\'")+"'");
						isInFirst = false;
					}
					ps.print(")");
					isFirst = false;
					
					if ((cnt%100) == 99)
						ps.println(";");
					cnt++;
				}
				if (!isFirst && (cnt%100) != 0)
					ps.println(";");
				ps.println();
				ps.flush();
				dset.close();
				desc.close();
			}
			rset.close();
			stmt.close();
			ps.flush();
			ps.close();
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		_frame.appendToProgressArea("Dump Complete!");
	}
}
