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

import javolution.util.FastList;

/**
 * This class describes a player instance
 *
 */
public class L2Player
{
	private int _objId;
	private String _name;
	private String _accountName;
	private int _level;
	private int _accessLevel;
	private int _clanId;
	private boolean _isOnline;
	private FastList<Integer> _friends;
	private FastList<Integer> _selectedFriends;
	private Forum _forum;
	
	public L2Player(int objId, String name, String accountName, int level, int accessLevel, int clanId, boolean isOnline)
	{
		_objId = objId;
		_name = name;
		_accountName = accountName;
		_level = level;
		_accessLevel = accessLevel;
		_clanId = clanId;
		_isOnline = isOnline;
		_forum = null;
		_friends = new FastList<Integer>();
		_selectedFriends = new FastList<Integer>();
	}
	
	public int getObjId()
	{
		return _objId;
	}
	
	public void setName(String val)
	{
		_name = val;
	}

	public String getName()
	{
		return _name;
	}
	
	public String getAccountName()
	{
		return _accountName;
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

	public int getLevel()
	{
		return _level;
	}
	
	public void setAccessLevel(int val)
	{
		_accessLevel = val;
	}

	public int getAccessLevel()
	{
		return _accessLevel;
	}

	public void setClanId(int val)
	{
		_clanId = val;
	}

	public int getClanId()
	{
		return _clanId;
	}

	public boolean isOnline()
	{
		return _isOnline;
	}
	
	public void setIsOnline(boolean val)
	{
		_isOnline = val;
	}
	
	public void setForum(Forum f)
	{
		_forum = f;
	}
	
	public void addFriend(Integer friendId)
	{
		_friends.add(friendId);
	}
	
	public void removeFriend(Integer friendId)
	{
		_friends.remove(friendId);
	}
	
	public void removeAllFriends()
	{
		_friends.clear();
	}

	public FastList<Integer> getFriendList()
	{
		return _friends;
	}

	public void selectFriend(Integer friendId)
	{
		_selectedFriends.add(friendId);
	}
	
	public void deSelectFriend(Integer friendId)
	{
		_selectedFriends.remove(friendId);
	}
	
	public FastList<Integer> getSelectedFriendsList()
	{
		return _selectedFriends;
	}
}

