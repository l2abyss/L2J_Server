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


import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Logger;

import javolution.util.FastMap;

import com.l2jserver.communityserver.L2DatabaseFactory;
import com.l2jserver.communityserver.model.Topic.ConstructorType;

public class Post
{
	private static Logger _log = Logger.getLogger(Post.class.getName());
	// type
	public static final int ADVERTISE = 0;
	public static final int MISCELLANEOUS = 1;
	public static final int INFORMATION = 2;
	
	private final int _sqlDPId;
	private int _postId;
	private int _postOwnerId;
	private String _postRecipientList;
	private int _postParentId;
	private long _postDate;
	private int _postTopicId;
	private int _postForumId;
	private String _postTitle;
	private String _postTxt;
	private int _postType;
	private int _lastCommentId;
	private Map<Integer, Comment> _comments;
	private int _readCount;
	
	/**
	 * @param restore
	 * @param t
	 */
	//public enum ConstructorType {REPLY, CREATE };
	public Post(ConstructorType ct, final int sqlDPId, int postId, int postOwnerID,String recipentList,long date,int tid,int postForumID, String title, String txt, int type, int readCount)
	{
		_sqlDPId = sqlDPId;
		_postId = postId;
		_postOwnerId = postOwnerID;
		_postRecipientList = recipentList;
		_postDate = date;
		_postTopicId = tid;
		_postForumId = postForumID;
		_postTitle = title;
		_postTxt = txt;
		_postType = type;
		_postParentId = -1;
		_comments = new FastMap<Integer, Comment>();
		_readCount = readCount;
		if (ct == ConstructorType.CREATE)
		{
			insertindb();
		}
		else
		{
			loadComments();
		}
	}

	public Post(ConstructorType ct, final int sqlDPId, int postId, int postOwnerID, String recipentList,int postParentId, long date, int tid, int postForumID, String title, String txt, int type, int readCount)
	{
		_sqlDPId = sqlDPId;
		_postId = postId;
		_postOwnerId = postOwnerID;
		_postRecipientList = recipentList;
		_postDate = date;
		_postTopicId = tid;
		_postForumId = postForumID;
		_postTitle = title;
		_postTxt = txt;
		_postType = type;
		_postParentId = postParentId;
		_comments = new FastMap<Integer, Comment>();
		_readCount = readCount;
		if (ct == ConstructorType.CREATE)
		{
			insertindb();
		}
		else
		{
			loadComments();
		}
	}

	private void loadComments()
	{
		java.sql.Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT * FROM comments WHERE serverId=? AND comment_forum_id=? AND comment_topic_id=? AND comment_post_id=?");
			statement.setInt(1, _sqlDPId);
			statement.setInt(2, _postForumId);
			statement.setInt(3, _postTopicId);
			statement.setInt(4, _postId);
			ResultSet result = statement.executeQuery();
			
			while (result.next())
			{
				int commentId = Integer.parseInt(result.getString("comment_id"));
				int commentOwner = Integer.parseInt(result.getString("comment_ownerid"));
				long date = Long.parseLong(result.getString("comment_date"));
				String text = result.getString("comment_txt");
				Comment c = new Comment(ConstructorType.RESTORE, _sqlDPId, commentId, commentOwner, date, _postId, _postTopicId, _postForumId, text);
				_comments.put(commentId, c);
				if (commentId > _lastCommentId)
					_lastCommentId = commentId;
			}
			result.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("data error on Forum " + _postForumId + " : " + e);
			e.printStackTrace();
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
			}
		}
	}
	
	public int getNewCommentId()
	{
		return ++_lastCommentId;
	}

	public void insertindb()
	{
		java.sql.Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("INSERT INTO posts (serverId, post_id,post_ownerid,post_recipient_list,post_date,post_topic_id,post_forum_id,post_txt,post_title,post_type,post_parent_id,post_read_count) values (?,?,?,?,?,?,?,?,?,?,?,?)");
			statement.setInt(1, _sqlDPId);
			statement.setInt(2, _postId);
			statement.setInt(3, _postOwnerId);
			statement.setString(4, _postRecipientList);
			statement.setLong(5, _postDate);
			statement.setInt(6, _postTopicId);
			statement.setInt(7, _postForumId);
			statement.setString(8, _postTxt);
			statement.setString(9, _postTitle);
			statement.setInt(10, _postType);
			statement.setInt(11, _postParentId);
			statement.setInt(12, _readCount);
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("error while saving new Post to db " + e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
			}
		}

	}

	public void deleteme()
	{
		for (Comment c: _comments.values())
			c.deleteme();
		_comments.clear();
		java.sql.Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM posts WHERE serverId=? AND post_forum_id=? AND post_topic_id=? AND post_id=?");
			statement.setInt(1, _sqlDPId);
			statement.setInt(2, _postForumId);
			statement.setInt(3, _postTopicId);
			statement.setInt(4, _postId);
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
			}
		}
	}

	/**
	 * @param i
	 */
	private void updatePost()
	{
		java.sql.Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE posts SET post_txt=?,post_title=?,post_recipient_list=?,post_read_count=? WHERE serverId=? AND post_id=? AND post_topic_id=? AND post_forum_id=?");
			statement.setString(1, _postTxt);
			statement.setString(2, _postTitle);
			statement.setString(3, _postRecipientList);
			statement.setInt(4, _readCount);
			statement.setInt(5, _sqlDPId);
			statement.setInt(6, _postId);
			statement.setInt(7, _postTopicId);
			statement.setInt(8, _postForumId);
			
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("error while saving new Post to db " + e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
			}
		}

	}

	public void clearComments()
	{
		_comments.clear();
	}
	
	public int getCommentsSize()
	{
		return _comments.size();
	}
	
	public Comment getComment(int j)
	{
		return _comments.get(j);
	}
	
	public void addComment(Comment c)
	{
		_comments.put(c.getID(), c);
	}
	
	public void rmCommentByID(int id)
	{
		_comments.get(id).deleteme();
		_comments.remove(id);
	}
	
	public Collection<Comment> getAllComments()
	{
		return _comments.values();
	}

	/**
	 *
	 */
	/**
	 * @return
	 */
	public int getID()
	{
		return _postId;
	}

	public String getText()
	{
		return _postTxt;
	}
	
	public int getOwnerId()
	{
		return _postOwnerId;
	}
	
	public int getParentId()
	{
		return _postParentId;
	}
	
	public void updatePost(String newTitle, String newTxt)
	{
		_postTitle = newTitle;
		_postTxt = newTxt;
		updatePost();
	}

	public void updatePost(String newTitle, String newTxt, int type)
	{
		_postTitle = newTitle;
		_postTxt = newTxt;
		_postType = type;
		updatePost();
	}
	
	public void setTopic(int newTopicId, int newPostId)
	{
		_postTopicId = newTopicId;
		_postId = newPostId;
		insertindb();
	}
	
	public String getRecipientList()
	{
		return _postRecipientList;
	}

	public String getTitle()
	{
		return _postTitle;
	}

	public Long getDate()
	{
		return _postDate;
	}

	public int getType()
	{
		return _postType;
	}

	public String getTypeName()
	{
		switch(_postType)
		{
			case ADVERTISE:
				return "[Advertise]";
			case MISCELLANEOUS:
				return "[Miscellaneous]";
			case INFORMATION:
				return "[Information]";
		}
		return "";
	}
	
	public int getReadCount()
	{
		return _readCount;
	}
	
	public void increaseReadCount()
	{
		_readCount++;
		updatePost();
	}

}
