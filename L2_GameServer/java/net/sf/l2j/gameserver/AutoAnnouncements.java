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
package net.sf.l2j.gameserver;


import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.sql.PreparedStatement;
import java.sql.ResultSet;


import javolution.util.FastList;
import net.sf.l2j.L2DatabaseFactory;

import net.sf.l2j.gameserver.util.Broadcast;



public class AutoAnnouncements
{
	static Logger _log = Logger.getLogger(Announcements.class.getName());

	private static AutoAnnouncements _instance;

	private List<AutoAnnounceThread> autothread = new FastList<AutoAnnounceThread>();

	public AutoAnnouncements()
	{
		restore();
		if (!autothread.isEmpty())
			autoTask();

	}

	@SuppressWarnings("deprecation")
    public void reload()
	{
		int count = 0;
		for (AutoAnnounceThread exec : autothread)
		{
			autothread.get(count).stop();
			autothread.remove(exec);
			count++;
		}

		autothread =  new FastList<AutoAnnounceThread>();

		restore();
		if (!autothread.isEmpty())
			autoTask();

	}

	public void autoTask()
	{
		int count = 0;
		for (@SuppressWarnings("unused") AutoAnnounceThread exec : autothread)
		{
			autothread.get(count).start();
			count++;
		}
	}

	public void announce(String text)
	{
		Broadcast.announceToOnlinePlayers(text);
		_log.warning("AutoAnnounce: "+text);
	}

	public static AutoAnnouncements getInstance()
	{
		if (_instance == null)
		{
			_instance = new AutoAnnouncements();
		}

		return _instance;
	}

    private void restore()
	{

		java.sql.Connection conn = null;
		int count = 0;
		try
		{
			conn = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = conn.prepareStatement("SELECT id, initial, delay, cycle, memo FROM auto_announcements");
			ResultSet data = statement.executeQuery();
			while(data.next())
			{
				int id = data.getInt("id");
				long initial = data.getLong("initial");
				long delay = data.getLong("delay");
				int repeat = data.getInt("cycle");
				String memo = data.getString("memo");
				String[] text = memo.split("/n");
				AutoAnnounceThread announces = new AutoAnnounceThread();
				announces.id = id;
				announces.initial = initial;
				announces.delay = delay;
		    	if (repeat > 0)
				announces.repeat = repeat;
				announces.memo = text;

				autothread.add(announces);
				count++;
			}

		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "AutoAnnoucements: Fail to load announcements data.", e);
		}
		_log.log(Level.SEVERE, "AutoAnnoucements: Load "+count+" Auto Annoucement Data.");
	}

	public class AutoAnnounceThread extends Thread
	{
		int id;
		long initial;
		long delay;
		int repeat = -1;
		int stop = 0;
		String[] memo;

	    public void run ()
		{

	    	try
            {

	            Thread.sleep(initial*1000);
            }
            catch (InterruptedException e1)
            {
	            e1.printStackTrace();
            }

		    while (true)
		    {
		    	if(repeat > 0)
		    		repeat--;
		    	for (String text: memo)
		    	{
		    		announce(text);

		    	}
	            if(repeat == 0)
	            {
	            	break;
	            }

		        try
		        {
		            Thread.sleep(delay*1000);

		        }
		        catch
		        (InterruptedException e)
		        {
		    	    _log.log(Level.SEVERE, "AutoAnnoucements: ID ["+id+"] Auto Announcement thread stop", e);
		        }

		    }


		 }
	}



}
