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
package com.l2jserver.gameserver.instancemanager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastList;

import com.l2jserver.Config;
import com.l2jserver.L2DatabaseFactory;
import com.l2jserver.gameserver.ItemsAutoDestroy;
import com.l2jserver.gameserver.ThreadPoolManager;
import com.l2jserver.gameserver.model.L2ItemInstance;
import com.l2jserver.gameserver.model.L2World;
import com.l2jserver.gameserver.templates.item.L2EtcItemType;


/**
 * This class manage all items on ground
 *
 * @version $Revision: $ $Date: $
 * @author  DiezelMax - original idea
 * @author  Enforcer  - actual build
 */
public class ItemsOnGroundManager
{
	static final Logger _log = Logger.getLogger(ItemsOnGroundManager.class.getName());
	
	protected List<L2ItemInstance> _items = null;
	private final StoreInDb _task = new StoreInDb();
	
	private ItemsOnGroundManager()
	{
		if (Config.SAVE_DROPPED_ITEM)
			_items = new FastList<L2ItemInstance>();
		if (Config.SAVE_DROPPED_ITEM_INTERVAL > 0)
			ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(_task, Config.SAVE_DROPPED_ITEM_INTERVAL, Config.SAVE_DROPPED_ITEM_INTERVAL);
		load();
	}
	
	public static final ItemsOnGroundManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private void load()
	{
		// If SaveDroppedItem is false, may want to delete all items previously stored to avoid add old items on reactivate
		if (!Config.SAVE_DROPPED_ITEM && Config.CLEAR_DROPPED_ITEM_TABLE)
			emptyTable();
		
		if (!Config.SAVE_DROPPED_ITEM)
			return;
		
		// if DestroyPlayerDroppedItem was previously  false, items curently protected will be added to ItemsAutoDestroy
		if (Config.DESTROY_DROPPED_PLAYER_ITEM)
		{
			Connection con = null;
			try
			{
				String str = null;
				if (!Config.DESTROY_EQUIPABLE_PLAYER_ITEM) //Recycle misc. items only
					str = "update itemsonground set drop_time=? where drop_time=-1 and equipable=0";
				else if (Config.DESTROY_EQUIPABLE_PLAYER_ITEM) //Recycle all items including equip-able
					str = "update itemsonground set drop_time=? where drop_time=-1";
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement(str);
				statement.setLong(1, System.currentTimeMillis());
				statement.execute();
				statement.close();
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "Error while updating table ItemsOnGround " + e.getMessage(), e);
			}
			finally
			{
				L2DatabaseFactory.close(con);
			}
		}
		
		//Add items to world
		Connection con = null;
		L2ItemInstance item;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT object_id,item_id,count,enchant_level,x,y,z,drop_time,equipable FROM itemsonground");
			ResultSet rset;
			int count = 0;
			rset = statement.executeQuery();
			while (rset.next())
			{
				item = new L2ItemInstance(rset.getInt(1), rset.getInt(2));
				L2World.getInstance().storeObject(item);
				if (item.isStackable() && rset.getInt(3) > 1) //this check and..
					item.setCount(rset.getInt(3));
				if (rset.getInt(4) > 0) // this, are really necessary?
					item.setEnchantLevel(rset.getInt(4));
				item.getPosition().setWorldPosition(rset.getInt(5), rset.getInt(6), rset.getInt(7));
				item.getPosition().setWorldRegion(L2World.getInstance().getRegion(item.getPosition().getWorldPosition()));
				item.getPosition().getWorldRegion().addVisibleObject(item);
				item.setDropTime(rset.getLong(8));
				item.setProtected(rset.getLong(8) == -1);
				item.setIsVisible(true);
				L2World.getInstance().addVisibleObject(item, item.getPosition().getWorldRegion());
				_items.add(item);
				count++;
				// add to ItemsAutoDestroy only items not protected
				if (!Config.LIST_PROTECTED_ITEMS.contains(item.getItemId()))
				{
					if (rset.getLong(8) > -1)
					{
						if ((Config.AUTODESTROY_ITEM_AFTER > 0 && item.getItemType() != L2EtcItemType.HERB)
								|| (Config.HERB_AUTO_DESTROY_TIME > 0 && item.getItemType() == L2EtcItemType.HERB))
							ItemsAutoDestroy.getInstance().addItem(item);
					}
				}
			}
			rset.close();
			statement.close();
			if (count > 0)
				_log.info("ItemsOnGroundManager: restored " + count + " items.");
			else
				_log.info("Initializing ItemsOnGroundManager.");
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "Error while loading ItemsOnGround " + e.getMessage(), e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
		if (Config.EMPTY_DROPPED_ITEM_TABLE_AFTER_LOAD)
			emptyTable();
	}
	
	public void save(L2ItemInstance item)
	{
		if (!Config.SAVE_DROPPED_ITEM)
			return;
		_items.add(item);
	}
	
	public void removeObject(L2ItemInstance item)
	{
		if (Config.SAVE_DROPPED_ITEM && _items != null)
		{
			_items.remove(item);
		}
	}
	
	public void saveInDb()
	{
		_task.run();
	}
	
	public void cleanUp()
	{
		_items.clear();
	}
	
	public void emptyTable()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM itemsonground");
			statement.execute();
			statement.close();
		}
		catch (Exception e1)
		{
			_log.log(Level.SEVERE, "Error while cleaning table ItemsOnGround " + e1.getMessage(), e1);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
	}
	
	protected class StoreInDb extends Thread
	{
		@Override
		public synchronized void run()
		{
			if (!Config.SAVE_DROPPED_ITEM)
				return;
			
			emptyTable();
			
			if (_items.isEmpty())
			{
				if (Config.DEBUG)
					_log.warning("ItemsOnGroundManager: nothing to save...");
				return;
			}
			
			Connection con = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement("INSERT INTO itemsonground(object_id,item_id,count,enchant_level,x,y,z,drop_time,equipable) VALUES(?,?,?,?,?,?,?,?,?)");
				
				for (L2ItemInstance item : _items)
				{
					if (item == null)
						continue;
					
					if (CursedWeaponsManager.getInstance().isCursed(item.getItemId()))
						continue; // Cursed Items not saved to ground, prevent double save
					
					try
					{
						statement.setInt(1, item.getObjectId());
						statement.setInt(2, item.getItemId());
						statement.setLong(3, item.getCount());
						statement.setInt(4, item.getEnchantLevel());
						statement.setInt(5, item.getX());
						statement.setInt(6, item.getY());
						statement.setInt(7, item.getZ());
						statement.setLong(8, (item.isProtected() ? -1 : item.getDropTime())); //item is protected or AutoDestroyed
						statement.setLong(9, (item.isEquipable() ? 1 : 0)); //set equip-able
						statement.execute();
						statement.clearParameters();
					}
					catch (Exception e)
					{
						_log.log(Level.SEVERE, "Error while inserting into table ItemsOnGround: " + e.getMessage(), e);
					}
				}
				statement.close();
			}
			catch (SQLException e)
			{
				_log.log(Level.SEVERE, "SQL error while storing items on ground: " + e.getMessage(), e);
			}
			finally
			{
				L2DatabaseFactory.close(con);
			}
			
			if (Config.DEBUG)
				_log.warning("ItemsOnGroundManager: " + _items.size() + " items on ground saved");
		}
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final ItemsOnGroundManager _instance = new ItemsOnGroundManager();
	}
}
