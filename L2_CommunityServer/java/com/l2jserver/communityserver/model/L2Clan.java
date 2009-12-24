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
package com.l2jserver.communityserver.model;

/**
 * This class describes a clan instance
 *
 */
public class L2Clan
{
	private int _clanId;
	private String _name;
	private int _level;
	private int _lordObjId;
	private String _lordName;
	private int _members;
	private String _allianceName;
	private int[] _allianceClanIdList;
	private boolean _noticeEnabled;
	private Forum _forum;
	private String _introduction;
	private String _notice;
	private boolean _isNoticeLoaded = false;
	
	public L2Clan(int clanId, String name, int level, int lordObjId, String lordName, int members, String allianceName, int[] allianceClanIdList, boolean noticeEnabled)
	{
		_clanId = clanId;
		_name = name;
		_lordName = lordName;
		_level = level;
		_lordObjId = lordObjId;
		_members = members;
		_allianceName = allianceName;
		_allianceClanIdList = allianceClanIdList;
		_noticeEnabled = noticeEnabled;
		_forum = null;
		_introduction = "";
	}
	
	public int getClanId()
	{
		return _clanId;
	}
	
	public void setName(String val)
	{
		_name = val;
	}

	public String getName()
	{
		return _name;
	}
	
	public void setLordName(String val)
	{
		_lordName = val;
	}

	public String getLordName()
	{
		return _lordName;
	}
	
	// do not use this from here!!!
	public Forum getForum()
	{
		return _forum;
	}
	
	public void setLevel(int val)
	{
		_level = val;
	}

	public int getClanLevel()
	{
		return _level;
	}
	
	public void setMembersCount(int val)
	{
		_members = val;
	}

	public int getMembersCount()
	{
		return _members;
	}
	
	public void setLordObjId(int val)
	{
		_lordObjId = val;
	}

	public int getLordObjId()
	{
		return _lordObjId;
	}

	public void setAllianceName(String val)
	{
		_allianceName = val;
	}

	public String getAllianceName()
	{
		return _allianceName;
	}

	public void setAllianceClanIdList(int[] val)
	{
		_allianceClanIdList = val;
	}

	public int[] getAllianceClanIdList()
	{
		return _allianceClanIdList;
	}
	
	public void setForum(Forum f)
	{
		_forum = f;
	}
	
	public String getIndtroduction()
	{
		return _introduction;
	}
	
	public void setIntroduction(String val)
	{
		_introduction = val;
	}
	
	public boolean isNoticeEnabled()
	{
		return _noticeEnabled;
	}
	
	public void setNoticeEnabled(boolean val)
	{
		_noticeEnabled = val;
	}
	
	public boolean isNoticeLoaded()
	{
		return _isNoticeLoaded;
	}
	
	public void setNotice(String val)
	{
		_isNoticeLoaded = true;
		_notice = val;
	}
	
	public String getNotice()
	{
		return _notice;
	}
}