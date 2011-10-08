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
package com.l2jserver.gameserver;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastList;

import com.l2jserver.Config;
import com.l2jserver.gameserver.cache.HtmCache;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.clientpackets.Say2;
import com.l2jserver.gameserver.network.serverpackets.CreatureSay;
import com.l2jserver.gameserver.network.serverpackets.NpcHtmlMessage;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;
import com.l2jserver.gameserver.script.DateRange;
import com.l2jserver.gameserver.util.Broadcast;
import com.l2jserver.util.StringUtil;

/**
 * This class ...
 * 
 * @version $Revision: 1.5.2.1.2.7 $ $Date: 2005/03/29 23:15:14 $
 */
public class Announcements
{
	private static Logger _log = Logger.getLogger(Announcements.class.getName());
	
	private List<String> _announcements = new FastList<String>();
	private List<String> _critAnnouncements = new FastList<String>();
	private List<List<Object>> _eventAnnouncements = new FastList<List<Object>>();
	
	private Announcements()
	{
		loadAnnouncements();
	}
	
	public static Announcements getInstance()
	{
		return SingletonHolder._instance;
	}
	
	public void loadAnnouncements()
	{
		_announcements.clear();
		_critAnnouncements.clear();
		readFromDisk("data/announcements.txt", _announcements);
		readFromDisk("data/critannouncements.txt", _critAnnouncements);
		
		if (Config.DEBUG)
			_log.info("Announcements: Loaded " + (_announcements.size() + _critAnnouncements.size())  + " announcements.");
	}
	
	public void showAnnouncements(L2PcInstance activeChar)
	{
		for (String announce : _announcements)
		{
			CreatureSay cs = new CreatureSay(0, Say2.ANNOUNCEMENT, activeChar.getName(), announce);
			activeChar.sendPacket(cs);
		}
		
		for (String critAnnounce : _critAnnouncements)
		{
			CreatureSay cs = new CreatureSay(0, Say2.CRITICAL_ANNOUNCE, activeChar.getName(), critAnnounce);
			activeChar.sendPacket(cs);
		}
		
		for (List<Object> eventAnnounce : _eventAnnouncements)
		{
			List<Object> entry = eventAnnounce;
			
			DateRange validDateRange = (DateRange) entry.get(0);
			String[] msg = (String[]) entry.get(1);
			Date currentDate = new Date();
			
			if (!validDateRange.isValid() || validDateRange.isWithinRange(currentDate))
			{
				SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1);
				for (int j = 0; j < msg.length; j++)
				{
					sm.addString(msg[j]);
				}
				activeChar.sendPacket(sm);
			}
			
		}
	}
	
	public void addEventAnnouncement(DateRange validDateRange, String[] msg)
	{
		List<Object> entry = new FastList<Object>();
		entry.add(validDateRange);
		entry.add(msg);
		_eventAnnouncements.add(entry);
	}
	
	public void listAnnouncements(L2PcInstance activeChar)
	{
		String content = HtmCache.getInstance().getHtmForce(activeChar.getHtmlPrefix(), "data/html/admin/announce.htm");
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setHtml(content);
		final StringBuilder replyMSG = StringUtil.startAppend(500, "<br>");
		for (int i = 0; i < _announcements.size(); i++)
		{
			StringUtil.append(replyMSG, "<table width=260><tr><td width=220>", _announcements.get(i), "</td><td width=40>"
					+ "<button value=\"Delete\" action=\"bypass -h admin_del_announcement ", String.valueOf(i), "\" width=60 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr></table>");
		}
		adminReply.replace("%announces%", replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}
	
	public void listCritAnnouncements(L2PcInstance activeChar)
	{
		String content = HtmCache.getInstance().getHtmForce(activeChar.getHtmlPrefix(), "data/html/admin/critannounce.htm");
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setHtml(content);
		final StringBuilder replyMSG = StringUtil.startAppend(500, "<br>");
		for (int i = 0; i < _critAnnouncements.size(); i++)
		{
			StringUtil.append(replyMSG, "<table width=260><tr><td width=220>", _critAnnouncements.get(i), "</td><td width=40>"
					+ "<button value=\"Delete\" action=\"bypass -h admin_del_critannouncement ", String.valueOf(i), "\" width=60 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr></table>");
		}
		adminReply.replace("%critannounces%", replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}
	
	public void addAnnouncement(String text)
	{
		_announcements.add(text);
		saveToDisk(false);
	}
	
	public void delAnnouncement(int line)
	{
		_announcements.remove(line);
		saveToDisk(false);
	}
	
	public void addCritAnnouncement(String text)
	{
		_critAnnouncements.add(text);
		saveToDisk(true);
	}
	
	public void delCritAnnouncement(int line)
	{
		_critAnnouncements.remove(line);
		saveToDisk(true);
	}
	
	private void readFromDisk(String path, List<String> list)
	{
		File file = new File(Config.DATAPACK_ROOT, path);
		
		if (file.exists())
		{
			LineNumberReader lnr = null;
			try
			{
				String line = null;
				lnr = new LineNumberReader(new FileReader(file));
				while ((line = lnr.readLine()) != null)
				{
					StringTokenizer st = new StringTokenizer(line, "\n\r");
					if (st.hasMoreTokens())
					{
						String announcement = st.nextToken();
						list.add(announcement);
					}
				}
			}
			catch (IOException e1)
			{
				_log.log(Level.SEVERE, "Error reading announcements: ", e1);
			}
			finally
			{
				try
				{
					lnr.close();
				}
				catch (Exception e2)
				{
					// nothing
				}
			}
		}
		else
			_log.warning(file.getAbsolutePath() + " doesn't exist");
	}
	
	private void saveToDisk(boolean isCritical)
	{
		String path;
		List<String> list;
		
		if (isCritical)
		{
			path = "data/critannouncements.txt";
			list = _critAnnouncements;
		}
		else
		{
			path = "data/announcements.txt";
			list = _announcements;
		}
		
		File file = new File(path);
		FileWriter save = null;
		
		try
		{
			save = new FileWriter(file);
			for (String announce : list)
			{
				save.write(announce);
				save.write("\r\n");
			}
		}
		catch (IOException e)
		{
			_log.log(Level.SEVERE, "Saving to the announcements file has failed: ", e);
		}
		finally
		{
			try
			{
				save.close();
			}
			catch (Exception e)
			{
			}
		}
	}
	
	public void announceToAll(String text)
	{
		announceToAll(text, false);
	}
	
	public void announceToAll(String text, boolean isCritical)
	{
		Broadcast.announceToOnlinePlayers(text, isCritical);
	}
	
	public void announceToAll(SystemMessage sm)
	{
		Broadcast.toAllOnlinePlayers(sm);
	}
	
	public void announceToInstance(SystemMessage sm, int instanceId)
	{
		Broadcast.toPlayersInInstance(sm, instanceId);
	}
	
	// Method for handling announcements from admin
	public void handleAnnounce(String command, int lengthToTrim, boolean isCritical)
	{
		try
		{
			// Announce string to everyone on server
			String text = command.substring(lengthToTrim);
			SingletonHolder._instance.announceToAll(text, isCritical);
		}
		
		// No body cares!
		catch (StringIndexOutOfBoundsException e)
		{
			// empty message.. ignore
		}
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final Announcements _instance = new Announcements();
	}
}
