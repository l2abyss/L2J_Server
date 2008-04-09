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
package net.sf.l2j.gameserver.model;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.lib.Log;

public class GMAudit
{
	
	private static final Logger _log = Logger.getLogger(Log.class.getName());
	
	public static void auditGMAction(String gmName, String action, String target, String params)
	{
		if (Config.GMAUDIT)
		{
			new File("log/GMAudit").mkdirs();
			
			SimpleDateFormat formatter;
			formatter = new SimpleDateFormat("dd/MM/yyyy H:mm:ss");
			String today = formatter.format(new Date());
			
			try
			{
				File file = new File("log/GMAudit/" + gmName + ".txt");
				FileWriter save = new FileWriter(file, true);
				
				String out = (today + ">" + gmName + ">" + action + ">" + target + ">" + params + "\n");
				
				save.write(out);
				save.flush();
				save.close();
				
				save = null;
				file = null;
			}
			catch (IOException e)
			{
				_log.log(Level.SEVERE, "GMAudit for GM " + gmName +" could not be saved: ", e);
			}
		}
	}
}