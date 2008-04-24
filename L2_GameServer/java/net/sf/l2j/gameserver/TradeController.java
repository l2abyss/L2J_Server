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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;
import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.model.L2TradeList;
import net.sf.l2j.gameserver.model.L2TradeList.L2TradeItem;

/**
 * This class ...
 *
 * @version $Revision: 1.5.4.13 $ $Date: 2005/04/06 16:13:38 $
 */
public class TradeController
{
    private static Logger _log = Logger.getLogger(TradeController.class.getName());
    private static TradeController _instance;
    
    private int _nextListId;
    private Map<Integer, L2TradeList> _lists;
    
    /** Task launching the function for restore count of Item (Clan Hall) */
    /*public class RestoreCount implements Runnable
    {
        private int _timer;
        
        public RestoreCount(int time)
        {
            _timer = time;
        }
        
        public void run()
        {
            restoreCount(_timer);
            dataTimerSave(_timer);
            ThreadPoolManager.getInstance().scheduleGeneral(new RestoreCount(_timer), (long)_timer*60*60*1000);
        }
    }*/
    
    public static TradeController getInstance()
    {
        if (_instance == null)
        {
            _instance = new TradeController();
        }
        return _instance;
    }
    
    private TradeController()
    {
        _lists = new FastMap<Integer, L2TradeList>();
        java.sql.Connection con = null;
        
        /*
         * Initialize Shop buylist
         */
        try
        {
            con = L2DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement1 = con.prepareStatement("SELECT  shop_id, npc_id FROM merchant_shopids");
            ResultSet rset1 = statement1.executeQuery();
            
            int itemId, price, maxCount, currentCount, time;
            long saveTimer;
            while (rset1.next())
            {
                PreparedStatement statement = con.prepareStatement("SELECT item_id, price, shop_id, "+L2DatabaseFactory.getInstance().safetyString("order")+", count, currentCount, time, savetimer FROM merchant_buylists WHERE shop_id=? ORDER BY "+L2DatabaseFactory.getInstance().safetyString("order")+" ASC");
                statement.setString(1, String.valueOf(rset1.getInt("shop_id")));
                ResultSet rset = statement.executeQuery();
                L2TradeList buy1 = new L2TradeList(rset1.getInt("shop_id"));
                
                while (rset.next())
                {
                    itemId = rset.getInt("item_id");
                    price = rset.getInt("price");
                    maxCount = rset.getInt("count");
                    currentCount = rset.getInt("currentCount");
                    time = rset.getInt("time");
                    saveTimer = rset.getLong("saveTimer");
                    
                    L2TradeItem item = new L2TradeItem(itemId);
                    if (ItemTable.getInstance().getTemplate(itemId) == null)
                    {
                        _log.warning("Skipping itemId: "+itemId+" on buylistId: "+buy1.getListId()+", missing data for that item.");
                        continue;
                    }
                    
                    if (price <= -1)
                    {
                        price = ItemTable.getInstance().getTemplate(itemId).getReferencePrice();
                    }
                    
                    if (Config.DEBUG)
                    {
                        // debug
                        double diff = ((double) (price)) / ItemTable.getInstance().getTemplate(itemId).getReferencePrice();
                        if (diff < 0.8 || diff > 1.2)
                        {
                            _log.severe("PRICING DEBUG: TradeListId: "+buy1.getListId()+" -  ItemId: "+itemId+" ("+ItemTable.getInstance().getTemplate(itemId).getName()+") diff: "+diff+" - Price: "+price+" - Reference: "+ItemTable.getInstance().getTemplate(itemId).getReferencePrice());
                        }
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
                
                rset.close();
                statement.close();
            }
            rset1.close();
            statement1.close();
            
            _log.config("TradeController: Loaded " + _lists.size() + " Buylists.");
        }
        catch (Exception e)
        {
            // problem with initializing spawn, go to next one
            _log.warning("TradeController: Buylists could not be initialized.");
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
        
        /*
         * If enabled, initialize the custom buylist
         */
        if (Config.CUSTOM_MERCHANT_TABLES)
        {
            try
            {
                int initialSize = _lists.size();
                con = L2DatabaseFactory.getInstance().getConnection();
                PreparedStatement statement1 = con.prepareStatement("SELECT  shop_id, npc_id FROM custom_merchant_shopids");
                ResultSet rset1 = statement1.executeQuery();
                
                int itemId, price, maxCount, currentCount, time;
                long saveTimer;
                while (rset1.next())
                {
                    PreparedStatement statement = con.prepareStatement("SELECT item_id, price, shop_id, "+L2DatabaseFactory.getInstance().safetyString("order")+", count, currentCount, time, savetimer FROM custom_merchant_buylists WHERE shop_id=? ORDER BY "+L2DatabaseFactory.getInstance().safetyString("order")+" ASC");
                    statement.setString(1, String.valueOf(rset1.getInt("shop_id")));
                    ResultSet rset = statement.executeQuery();
                    L2TradeList buy1 = new L2TradeList(rset1.getInt("shop_id"));
                    
                    while (rset.next())
                    {
                        itemId = rset.getInt("item_id");
                        price = rset.getInt("price");
                        maxCount = rset.getInt("count");
                        currentCount = rset.getInt("currentCount");
                        time = rset.getInt("time");
                        saveTimer = rset.getLong("saveTimer");
                        
                        L2TradeItem item = new L2TradeItem(itemId);
                        if (ItemTable.getInstance().getTemplate(itemId) == null)
                        {
                            _log.warning("Skipping itemId: "+itemId+" on buylistId: "+buy1.getListId()+", missing data for that item.");
                            continue;
                        }
                        
                        if (price <= -1)
                        {
                            price = ItemTable.getInstance().getTemplate(itemId).getReferencePrice();
                        }
                        
                        if (Config.DEBUG)
                        {
                            // debug
                            double diff = ((double) (price)) / ItemTable.getInstance().getTemplate(itemId).getReferencePrice();
                            if (diff < 0.8 || diff > 1.2)
                            {
                                _log.severe("PRICING DEBUG: TradeListId: "+buy1.getListId()+" -  ItemId: "+itemId+" ("+ItemTable.getInstance().getTemplate(itemId).getName()+") diff: "+diff+" - Price: "+price+" - Reference: "+ItemTable.getInstance().getTemplate(itemId).getReferencePrice());
                            }
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
                    
                    rset.close();
                    statement.close();
                }
                rset1.close();
                statement1.close();
                
                _log.config("TradeController: Loaded " + (_lists.size() - initialSize) + " Custom Buylists.");

            }
            catch (Exception e)
            {
                // problem with initializing spawn, go to next one
                _log.warning("TradeController: Buylists could not be initialized.");
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
    }
    
    public L2TradeList getBuyList(int listId)
    {
        return _lists.get(listId);
    }
    
    public List<L2TradeList> getBuyListByNpcId(int npcId)
    {
        List<L2TradeList> lists = new FastList<L2TradeList>();
        Collection<L2TradeList> values = _lists.values();
        
        for (L2TradeList list : values)
        {
            String tradeNpcId = list.getNpcId();
            if (tradeNpcId.startsWith("gm"))
                continue;
            if (npcId == Integer.parseInt(tradeNpcId))
                lists.add(list);
        }
        return lists;
    }
    
    
    
    public void dataCountStore()
    {
        java.sql.Connection con = null;
        PreparedStatement statement;
        
        try
        {
            con = L2DatabaseFactory.getInstance().getConnection();
            
            for (L2TradeList list : _lists.values())
            {
                if (list.hasLimitedStockItem())
                {
                    
                    int listId = list.getListId();
                    
                    for (L2TradeItem item :list.getItems())
                    {
                        if (item.hasLimitedStock() && item.getCurrentCount() < item.getMaxCount()) //needed?
                        {
                            statement = con.prepareStatement("UPDATE merchant_buylists SET currentCount=? WHERE item_id=? AND shop_id=?");
                            statement.setInt(1, item.getCurrentCount());
                            statement.setInt(2, item.getItemId());
                            statement.setInt(3, listId);
                            statement.executeUpdate();
                            statement.close();
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            _log.log(Level.SEVERE, "TradeController: Could not store Count Item" );
        }
        finally
        {
            try { con.close(); } catch (Exception e) { e.printStackTrace(); }
        }
    }
    /**
     * @return
     */
    public synchronized int getNextId()
    {
        return _nextListId++;
    }
}
