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
package com.l2jserver.gameserver.network.serverpackets;

import java.util.logging.Level;

import com.l2jserver.gameserver.cache.HtmCache;

/**
 * @author FBIagent
 */
public abstract class AbstractHtmlPacket extends L2GameServerPacket
{
	public static final char VAR_PARAM_START_CHAR = '$';
	
	private String _html = null;
	private boolean _disabledValidation = false;
	
	protected AbstractHtmlPacket()
	{
	}
	
	protected AbstractHtmlPacket(String html)
	{
		setHtml(html);
	}
	
	public final void disableValidation()
	{
		_disabledValidation = true;
	}
	
	public final void setHtml(String html)
	{
		if (html.length() > 17200)
		{
			_log.log(Level.WARNING, "Html is too long! this will crash the client!", new Throwable());
			_html = html.substring(0, 17200);
		}
		
		if (!html.contains("<html>"))
		{
			html = "<html><body>" + html + "</body></html>";
		}
		
		_html = html;
	}
	
	public final boolean setFile(String prefix, String path)
	{
		String content = HtmCache.getInstance().getHtm(prefix, path);
		
		if (content == null)
		{
			setHtml("<html><body>My Text is missing:<br>" + path + "</body></html>");
			_log.warning("missing html page " + path);
			return false;
		}
		
		setHtml(content);
		return true;
	}
	
	public final void replace(String pattern, String value)
	{
		_html = _html.replaceAll(pattern, value.replaceAll("\\$", "\\\\\\$"));
	}
	
	public final void replace(String pattern, boolean val)
	{
		replace(pattern, String.valueOf(val));
	}
	
	public final void replace(String pattern, int val)
	{
		replace(pattern, String.valueOf(val));
	}
	
	public final void replace(String pattern, long val)
	{
		replace(pattern, String.valueOf(val));
	}
	
	public final void replace(String pattern, double val)
	{
		replace(pattern, String.valueOf(val));
	}
	
	private final void _buildHtmlBypassCache()
	{
		int bypassEnd = 0;
		int bypassStart = _html.indexOf("=\"bypass ", bypassEnd);
		int bypassStartEnd;
		while (bypassStart != -1)
		{
			bypassStartEnd = bypassStart + 9;
			bypassEnd = _html.indexOf("\"", bypassStartEnd);
			if (bypassEnd == -1)
			{
				break;
			}
			
			int hParamPos = _html.indexOf("-h ", bypassStartEnd);
			String bypass;
			if ((hParamPos != -1) && (hParamPos < bypassEnd))
			{
				bypass = _html.substring(hParamPos + 3, bypassEnd).trim();
			}
			else
			{
				bypass = _html.substring(bypassStartEnd, bypassEnd).trim();
			}
			
			int firstParameterStart = bypass.indexOf(VAR_PARAM_START_CHAR);
			if (firstParameterStart != -1)
			{
				bypass = bypass.substring(0, firstParameterStart + 1);
			}
			
			addHtmlAction(bypass);
			bypassStart = _html.indexOf("=\"bypass ", bypassEnd);
		}
	}
	
	private final void _buildHtmlLinkCache()
	{
		int linkEnd = 0;
		int linkStart = _html.indexOf("=\"link ", linkEnd);
		int linkStartEnd;
		while (linkStart != -1)
		{
			// we include the char sequence "link " in the cached html action
			linkStartEnd = linkStart + 2;
			linkEnd = _html.indexOf("\"", linkStartEnd);
			if (linkEnd == -1)
			{
				break;
			}
			
			String htmlLink = _html.substring(linkStartEnd, linkEnd).trim();
			if (htmlLink.isEmpty())
			{
				_log.warning("Html link path is empty!");
				continue;
			}
			
			if (htmlLink.contains(".."))
			{
				_log.warning("Html link path is invalid: " + htmlLink);
				continue;
			}
			
			addHtmlAction(htmlLink);
			linkStart = _html.indexOf("=\"link ", linkEnd);
		}
	}
	
	@Override
	public final void runImpl()
	{
		clearHtmlActionCache();
		
		if (_disabledValidation)
		{
			return;
		}
		
		_buildHtmlBypassCache();
		_buildHtmlLinkCache();
	}
	
	public final String getHtml()
	{
		return _html;
	}
	
	public abstract void clearHtmlActionCache();
	
	public abstract void addHtmlAction(String action);
}
