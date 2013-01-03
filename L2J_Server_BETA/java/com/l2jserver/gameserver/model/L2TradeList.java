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
package com.l2jserver.gameserver.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastMap;

import com.l2jserver.L2DatabaseFactory;
import com.l2jserver.gameserver.ThreadPoolManager;
import com.l2jserver.gameserver.datatables.ItemTable;
import com.l2jserver.gameserver.model.items.L2Item;

/**
 * This class ...
 * @version $Revision: 1.4.2.1.2.5 $ $Date: 2005/03/27 15:29:33 $
 */
public class L2TradeList
{
	private final Map<Integer, L2TradeItem> _items;
	private final int _listId;
	private String _buystorename, _sellstorename;
	private boolean _hasLimitedStockItem;
	private String _npcId;
	
	public L2TradeList(int listId)
	{
		_items = new FastMap<Integer, L2TradeItem>().shared();
		_listId = listId;
	}
	
	public void setNpcId(String id)
	{
		_npcId = id;
	}
	
	public String getNpcId()
	{
		return _npcId;
	}
	
	public void addItem(L2TradeItem item)
	{
		_items.put(item.getItemId(), item);
		if (item.hasLimitedStock())
		{
			setHasLimitedStockItem(true);
		}
	}
	
	public void replaceItem(int itemID, long price)
	{
		L2TradeItem item = _items.get(itemID);
		if (item != null)
		{
			item.setPrice(price);
		}
	}
	
	public void removeItem(int itemID)
	{
		_items.remove(itemID);
	}
	
	/**
	 * @return Returns the listId.
	 */
	public int getListId()
	{
		return _listId;
	}
	
	/**
	 * @param hasLimitedStockItem The hasLimitedStockItem to set.
	 */
	public void setHasLimitedStockItem(boolean hasLimitedStockItem)
	{
		_hasLimitedStockItem = hasLimitedStockItem;
	}
	
	/**
	 * @return Returns the hasLimitedStockItem.
	 */
	public boolean hasLimitedStockItem()
	{
		return _hasLimitedStockItem;
	}
	
	public void setSellStoreName(String name)
	{
		_sellstorename = name;
	}
	
	public String getSellStoreName()
	{
		return _sellstorename;
	}
	
	public void setBuyStoreName(String name)
	{
		_buystorename = name;
	}
	
	public String getBuyStoreName()
	{
		return _buystorename;
	}
	
	/**
	 * @return Returns the items.
	 */
	public Collection<L2TradeItem> getItems()
	{
		return _items.values();
	}
	
	public List<L2TradeItem> getItems(int start, int end)
	{
		List<L2TradeItem> list = new LinkedList<>();
		list.addAll(_items.values());
		return list.subList(start, end);
	}
	
	public long getPriceForItemId(int itemId)
	{
		L2TradeItem item = _items.get(itemId);
		if (item != null)
		{
			return item.getPrice();
		}
		return -1;
	}
	
	public L2TradeItem getItemById(int itemId)
	{
		return _items.get(itemId);
	}
	
	/*
	 * public boolean countDecrease(int itemId) { L2TradeItem item = _items.get(itemId); if (item != null) { return item.hasLimitedStock(); } return false; }
	 */
	
	public boolean containsItemId(int itemId)
	{
		return _items.containsKey(itemId);
	}
	
	/**
	 * Itens representation for trade lists
	 * @author KenM
	 */
	public static class L2TradeItem
	{
		private static final Logger _log = Logger.getLogger(L2TradeItem.class.getName());
		
		private final int _listId;
		private final int _itemId;
		private final L2Item _template;
		private long _price;
		
		// count related
		private final AtomicLong _currentCount = new AtomicLong();
		private long _maxCount = -1;
		private long _restoreDelay;
		private long _nextRestoreTime;
		
		public L2TradeItem(int listId, int itemId)
		{
			_listId = listId;
			_itemId = itemId;
			_template = ItemTable.getInstance().getTemplate(itemId);
		}
		
		/**
		 * @return Returns the itemId.
		 */
		public int getItemId()
		{
			return _itemId;
		}
		
		/**
		 * @param price The price to set.
		 */
		public void setPrice(long price)
		{
			_price = price;
		}
		
		/**
		 * @return Returns the price.
		 */
		public long getPrice()
		{
			return _price;
		}
		
		public L2Item getTemplate()
		{
			return _template;
		}
		
		/**
		 * @param currentCount The currentCount to set.
		 */
		public void setCurrentCount(long currentCount)
		{
			_currentCount.set(currentCount);
		}
		
		public boolean decreaseCount(long val)
		{
			return _currentCount.addAndGet(-val) >= 0;
		}
		
		/**
		 * @return Returns the currentCount.
		 */
		public long getCurrentCount()
		{
			if (hasLimitedStock() && isPendingStockUpdate())
			{
				restoreInitialCount();
			}
			long ret = _currentCount.get();
			return ret > 0 ? ret : 0;
		}
		
		public boolean isPendingStockUpdate()
		{
			return System.currentTimeMillis() >= _nextRestoreTime;
		}
		
		public void restoreInitialCount()
		{
			setCurrentCount(getMaxCount());
			_nextRestoreTime = _nextRestoreTime + getRestoreDelay();
			
			// consume until next update is on future
			if (isPendingStockUpdate() && (getRestoreDelay() > 0))
			{
				_nextRestoreTime = System.currentTimeMillis() + getRestoreDelay();
			}
			
			// exec asynchronously
			try
			{
				ThreadPoolManager.getInstance().executeTask(new TimerSave());
			}
			catch (RejectedExecutionException e)
			{
				// during shutdown executeTask() failed
				saveDataTimer();
			}
		}
		
		/**
		 * @param maxCount The maxCount to set.
		 */
		public void setMaxCount(long maxCount)
		{
			_maxCount = maxCount;
		}
		
		/**
		 * @return Returns the maxCount.
		 */
		public long getMaxCount()
		{
			return _maxCount;
		}
		
		public boolean hasLimitedStock()
		{
			return getMaxCount() > -1;
		}
		
		/**
		 * @param restoreDelay The restoreDelay to set (in hours)
		 */
		public void setRestoreDelay(long restoreDelay)
		{
			_restoreDelay = restoreDelay * 60 * 60 * 1000;
		}
		
		/**
		 * @return Returns the restoreDelay (in milis)
		 */
		public long getRestoreDelay()
		{
			return _restoreDelay;
		}
		
		/**
		 * For resuming when server loads
		 * @param nextRestoreTime The nextRestoreTime to set.
		 */
		public void setNextRestoreTime(long nextRestoreTime)
		{
			_nextRestoreTime = nextRestoreTime;
		}
		
		/**
		 * @return Returns the nextRestoreTime.
		 */
		public long getNextRestoreTime()
		{
			return _nextRestoreTime;
		}
		
		class TimerSave implements Runnable
		{
			@Override
			public void run()
			{
				saveDataTimer();
			}
		}
		
		protected void saveDataTimer()
		{
			try (Connection con = L2DatabaseFactory.getInstance().getConnection())
			{
				PreparedStatement statement = con.prepareStatement("UPDATE merchant_buylists SET savetimer =? WHERE shop_id =? and item_id =?");
				statement.setLong(1, _nextRestoreTime);
				statement.setInt(2, _listId);
				statement.setInt(3, _itemId);
				statement.executeUpdate();
				statement.close();
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "L2TradeItem: Could not update Timer save in Buylist");
			}
		}
	}
}
