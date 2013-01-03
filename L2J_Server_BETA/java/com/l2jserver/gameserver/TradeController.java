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
package com.l2jserver.gameserver;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;

import com.l2jserver.Config;
import com.l2jserver.L2DatabaseFactory;
import com.l2jserver.gameserver.datatables.ItemTable;
import com.l2jserver.gameserver.model.L2TradeList;
import com.l2jserver.gameserver.model.L2TradeList.L2TradeItem;

public class TradeController
{
	private static final Logger _log = Logger.getLogger(TradeController.class.getName());
	
	private int _nextListId;
	private final Map<Integer, L2TradeList> _lists = new FastMap<>();
	
	protected TradeController()
	{
		_lists.clear();
		// Initialize Shop buy list
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			Statement s = con.createStatement();
			ResultSet rs1 = s.executeQuery("SELECT  shop_id, npc_id FROM merchant_shopids"))
		{
			int itemId, price, maxCount, currentCount, time;
			long saveTimer;
			try (PreparedStatement ps = con.prepareStatement("SELECT item_id, price, shop_id, " + L2DatabaseFactory.getInstance().safetyString("order") + ", count, currentCount, time, savetimer FROM merchant_buylists WHERE shop_id=? ORDER BY " + L2DatabaseFactory.getInstance().safetyString("order") + " ASC"))
			{
				while (rs1.next())
				{
					ps.setString(1, String.valueOf(rs1.getInt("shop_id")));
					try (ResultSet rs2 = ps.executeQuery())
					{
						ps.clearParameters();
						
						int shopId = rs1.getInt("shop_id");
						L2TradeList buy1 = new L2TradeList(shopId);
						
						while (rs2.next())
						{
							itemId = rs2.getInt("item_id");
							price = rs2.getInt("price");
							maxCount = rs2.getInt("count");
							currentCount = rs2.getInt("currentCount");
							time = rs2.getInt("time");
							saveTimer = rs2.getLong("saveTimer");
							
							L2TradeItem item = new L2TradeItem(shopId, itemId);
							if (ItemTable.getInstance().getTemplate(itemId) == null)
							{
								_log.warning("Skipping itemId: " + itemId + " on buylistId: " + buy1.getListId() + ", missing data for that item.");
								continue;
							}
							
							if (price <= -1)
							{
								price = ItemTable.getInstance().getTemplate(itemId).getReferencePrice();
							}
							
							item.setPrice(price);
							
							item.setRestoreDelay(time);
							item.setNextRestoreTime(saveTimer);
							item.setMaxCount(maxCount);
							
							if (currentCount > -1)
							{
								item.setCurrentCount(currentCount);
							}
							else
							{
								item.setCurrentCount(maxCount);
							}
							
							buy1.addItem(item);
						}
						
						buy1.setNpcId(rs1.getString("npc_id"));
						_lists.put(buy1.getListId(), buy1);
						_nextListId = Math.max(_nextListId, buy1.getListId() + 1);
					}
				}
			}
			_log.info("TradeController: Loaded " + _lists.size() + " Buylists.");
		}
		catch (Exception e)
		{
			// problem with initializing spawn, go to next one
			_log.log(Level.WARNING, "TradeController: Buylists could not be initialized: " + e.getMessage(), e);
		}
		
		// If enabled, initialize the custom buy list
		if (Config.CUSTOM_MERCHANT_TABLES)
		{
			try (Connection con = L2DatabaseFactory.getInstance().getConnection();
				Statement s = con.createStatement();
				ResultSet rset1 = s.executeQuery("SELECT  shop_id, npc_id FROM custom_merchant_shopids"))
			{
				int initialSize = _lists.size();
				int itemId, price, maxCount, currentCount, time;
				long saveTimer;
				try (PreparedStatement ps = con.prepareStatement("SELECT item_id, price, shop_id, " + L2DatabaseFactory.getInstance().safetyString("order") + ", count, currentCount, time, savetimer FROM custom_merchant_buylists WHERE shop_id=? ORDER BY " + L2DatabaseFactory.getInstance().safetyString("order") + " ASC"))
				{
					while (rset1.next())
					{
						ps.setString(1, String.valueOf(rset1.getInt("shop_id")));
						try (ResultSet rset = ps.executeQuery())
						{
							ps.clearParameters();
							
							int shopId = rset1.getInt("shop_id");
							L2TradeList buy1 = new L2TradeList(shopId);
							
							while (rset.next())
							{
								itemId = rset.getInt("item_id");
								price = rset.getInt("price");
								maxCount = rset.getInt("count");
								currentCount = rset.getInt("currentCount");
								time = rset.getInt("time");
								saveTimer = rset.getLong("saveTimer");
								
								L2TradeItem item = new L2TradeItem(shopId, itemId);
								if (ItemTable.getInstance().getTemplate(itemId) == null)
								{
									_log.warning("Skipping itemId: " + itemId + " on buylistId: " + buy1.getListId() + ", missing data for that item.");
									continue;
								}
								
								if (price <= -1)
								{
									price = ItemTable.getInstance().getTemplate(itemId).getReferencePrice();
								}
								
								item.setPrice(price);
								
								item.setRestoreDelay(time);
								item.setNextRestoreTime(saveTimer);
								item.setMaxCount(maxCount);
								
								if (currentCount > -1)
								{
									item.setCurrentCount(currentCount);
								}
								else
								{
									item.setCurrentCount(maxCount);
								}
								
								buy1.addItem(item);
							}
							
							buy1.setNpcId(rset1.getString("npc_id"));
							_lists.put(buy1.getListId(), buy1);
							_nextListId = Math.max(_nextListId, buy1.getListId() + 1);
						}
					}
				}
				_log.info("TradeController: Loaded " + (_lists.size() - initialSize) + " Custom Buylists.");
				
			}
			catch (Exception e)
			{
				// problem with initializing spawn, go to next one
				_log.log(Level.WARNING, "TradeController: Buylists could not be initialized: " + e.getMessage(), e);
			}
		}
	}
	
	public L2TradeList getBuyList(int listId)
	{
		return _lists.get(listId);
	}
	
	public List<L2TradeList> getBuyListByNpcId(int npcId)
	{
		List<L2TradeList> lists = new FastList<>();
		Collection<L2TradeList> values = _lists.values();
		
		for (L2TradeList list : values)
		{
			String tradeNpcId = list.getNpcId();
			if (tradeNpcId.startsWith("gm"))
			{
				continue;
			}
			if (npcId == Integer.parseInt(tradeNpcId))
			{
				lists.add(list);
			}
		}
		return lists;
	}
	
	public void dataCountStore()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE merchant_buylists SET currentCount = ? WHERE item_id = ? AND shop_id = ?"))
		{
			for (L2TradeList list : _lists.values())
			{
				if (list.hasLimitedStockItem())
				{
					for (L2TradeItem item : list.getItems())
					{
						long currentCount;
						if (item.hasLimitedStock() && ((currentCount = item.getCurrentCount()) < item.getMaxCount()))
						{
							statement.setLong(1, currentCount);
							statement.setInt(2, item.getItemId());
							statement.setInt(3, list.getListId());
							statement.executeUpdate();
							statement.clearParameters();
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "TradeController: Could not store Count Item: " + e.getMessage(), e);
		}
	}
	
	/**
	 * @return
	 */
	public synchronized int getNextId()
	{
		return _nextListId++;
	}
	
	public static TradeController getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final TradeController _instance = new TradeController();
	}
}
