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
package com.l2jserver.gameserver.datatables;

import static com.l2jserver.gameserver.model.itemcontainer.PcInventory.ADENA_ID;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javolution.util.FastMap;

import com.l2jserver.Config;
import com.l2jserver.L2DatabaseFactory;
import com.l2jserver.gameserver.Item;
import com.l2jserver.gameserver.ThreadPoolManager;
import com.l2jserver.gameserver.idfactory.IdFactory;
import com.l2jserver.gameserver.model.L2ItemInstance;
import com.l2jserver.gameserver.model.L2ItemInstance.ItemLocation;
import com.l2jserver.gameserver.model.L2Object;
import com.l2jserver.gameserver.model.L2World;
import com.l2jserver.gameserver.model.actor.L2Attackable;
import com.l2jserver.gameserver.model.actor.instance.L2EventMonsterInstance;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.skills.SkillsEngine;
import com.l2jserver.gameserver.templates.item.L2Armor;
import com.l2jserver.gameserver.templates.item.L2ArmorType;
import com.l2jserver.gameserver.templates.item.L2EtcItem;
import com.l2jserver.gameserver.templates.item.L2Item;
import com.l2jserver.gameserver.templates.item.L2Weapon;
import com.l2jserver.gameserver.templates.item.L2WeaponType;
import com.l2jserver.gameserver.util.GMAudit;

/**
 * This class ...
 *
 * @version $Revision: 1.9.2.6.2.9 $ $Date: 2005/04/02 15:57:34 $
 */
public class ItemTable
{
	private static Logger _log = Logger.getLogger(ItemTable.class.getName());
	private static Logger _logItems = Logger.getLogger("item");
	
	public static final Map<String, Integer> _materials = new FastMap<String, Integer>();
	public static final Map<String, Integer> _crystalTypes = new FastMap<String, Integer>();
	public static final Map<String, Integer> _slots = new FastMap<String, Integer>();
	public static final Map<String, L2WeaponType> _weaponTypes = new FastMap<String, L2WeaponType>();
	public static final Map<String, L2ArmorType> _armorTypes = new FastMap<String, L2ArmorType>();
	
	
	private L2Item[] _allTemplates;
	private Map<Integer, L2EtcItem> _etcItems;
	private Map<Integer, L2Armor> _armors;
	private Map<Integer, L2Weapon> _weapons;
	
	static
	{
		_materials.put("adamantaite", L2Item.MATERIAL_ADAMANTAITE);
		_materials.put("blood_steel", L2Item.MATERIAL_BLOOD_STEEL);
		_materials.put("bone", L2Item.MATERIAL_BONE);
		_materials.put("bronze", L2Item.MATERIAL_BRONZE);
		_materials.put("cloth", L2Item.MATERIAL_CLOTH);
		_materials.put("chrysolite", L2Item.MATERIAL_CHRYSOLITE);
		_materials.put("cobweb", L2Item.MATERIAL_COBWEB);
		_materials.put("cotton", L2Item.MATERIAL_FINE_STEEL);
		_materials.put("crystal", L2Item.MATERIAL_CRYSTAL);
		_materials.put("damascus", L2Item.MATERIAL_DAMASCUS);
		_materials.put("dyestuff", L2Item.MATERIAL_DYESTUFF);
		_materials.put("fine_steel", L2Item.MATERIAL_FINE_STEEL);
		_materials.put("fish", L2Item.MATERIAL_FISH);
		_materials.put("gold", L2Item.MATERIAL_GOLD);
		_materials.put("horn", L2Item.MATERIAL_HORN);
		_materials.put("leather", L2Item.MATERIAL_LEATHER);
		_materials.put("liquid", L2Item.MATERIAL_LIQUID);
		_materials.put("mithril", L2Item.MATERIAL_MITHRIL);
		_materials.put("oriharukon", L2Item.MATERIAL_ORIHARUKON);
		_materials.put("paper", L2Item.MATERIAL_PAPER);
		_materials.put("rune_xp", L2Item.MATERIAL_RUNE_XP);
		_materials.put("rune_sp", L2Item.MATERIAL_RUNE_SP);
		_materials.put("rune_remove_penalty", L2Item.MATERIAL_RUNE_PENALTY);
		_materials.put("scale_of_dragon", L2Item.MATERIAL_SCALE_OF_DRAGON);
		_materials.put("seed", L2Item.MATERIAL_SEED);
		_materials.put("silver", L2Item.MATERIAL_SILVER);
		_materials.put("steel", L2Item.MATERIAL_STEEL);
		_materials.put("wood", L2Item.MATERIAL_WOOD);
		
		_crystalTypes.put("s84", L2Item.CRYSTAL_S84);
		_crystalTypes.put("s80", L2Item.CRYSTAL_S80);
		_crystalTypes.put("s", L2Item.CRYSTAL_S);
		_crystalTypes.put("a", L2Item.CRYSTAL_A);
		_crystalTypes.put("b", L2Item.CRYSTAL_B);
		_crystalTypes.put("c", L2Item.CRYSTAL_C);
		_crystalTypes.put("d", L2Item.CRYSTAL_D);
		_crystalTypes.put("none", L2Item.CRYSTAL_NONE);
		
		// weapon types
		for (L2WeaponType type : L2WeaponType.values())
		{
			_weaponTypes.put(type.toString(), type);
		}
		
		// armor types
		for (L2ArmorType type : L2ArmorType.values())
		{
			_armorTypes.put(type.toString(), type);
		}
		
		_slots.put("shirt", L2Item.SLOT_UNDERWEAR);
		_slots.put("lbracelet", L2Item.SLOT_L_BRACELET);
		_slots.put("rbracelet", L2Item.SLOT_R_BRACELET);
		_slots.put("talisman", L2Item.SLOT_DECO);
		_slots.put("chest", L2Item.SLOT_CHEST);
		_slots.put("fullarmor", L2Item.SLOT_FULL_ARMOR);
		_slots.put("head", L2Item.SLOT_HEAD);
		_slots.put("hair", L2Item.SLOT_HAIR);
		_slots.put("hairall", L2Item.SLOT_HAIRALL);
		_slots.put("underwear", L2Item.SLOT_UNDERWEAR);
		_slots.put("back", L2Item.SLOT_BACK);
		_slots.put("neck", L2Item.SLOT_NECK);
		_slots.put("legs", L2Item.SLOT_LEGS);
		_slots.put("feet", L2Item.SLOT_FEET);
		_slots.put("gloves", L2Item.SLOT_GLOVES);
		_slots.put("chest,legs", L2Item.SLOT_CHEST | L2Item.SLOT_LEGS);
		_slots.put("belt", L2Item.SLOT_BELT);
		_slots.put("rhand", L2Item.SLOT_R_HAND);
		_slots.put("lhand", L2Item.SLOT_L_HAND);
		_slots.put("lrhand", L2Item.SLOT_LR_HAND);
		_slots.put("rear;lear", L2Item.SLOT_R_EAR | L2Item.SLOT_L_EAR);
		_slots.put("rfinger;lfinger", L2Item.SLOT_R_FINGER | L2Item.SLOT_L_FINGER);
		_slots.put("wolf", L2Item.SLOT_WOLF);
		_slots.put("greatwolf", L2Item.SLOT_GREATWOLF);
		_slots.put("hatchling", L2Item.SLOT_HATCHLING);
		_slots.put("strider", L2Item.SLOT_STRIDER);
		_slots.put("babypet", L2Item.SLOT_BABYPET);
		_slots.put("none", L2Item.SLOT_NONE);
		
		//retail compatibility
		_slots.put("onepiece", L2Item.SLOT_FULL_ARMOR);
		_slots.put("hair2", L2Item.SLOT_HAIR2);
		_slots.put("dhair", L2Item.SLOT_HAIRALL);
		_slots.put("alldress", L2Item.SLOT_ALLDRESS);
		_slots.put("deco1", L2Item.SLOT_DECO);
		_slots.put("waist", L2Item.SLOT_BELT);
		
	}
	
	/**
	 * Returns instance of ItemTable
	 * @return ItemTable
	 */
	public static ItemTable getInstance()
	{
		return SingletonHolder._instance;
	}
	
	/**
	 * Returns a new object Item
	 * @return
	 */
	public Item newItem()
	{
		return new Item();
	}
	
	/**
	 * Constructor.
	 */
	private ItemTable()
	{
		_etcItems = new FastMap<Integer, L2EtcItem>();
		_armors = new FastMap<Integer, L2Armor>();
		_weapons = new FastMap<Integer, L2Weapon>();
		load();
	}
	
	private void load()
	{
		int highest = 0;
		_armors.clear();
		_etcItems.clear();
		_weapons.clear();
		for (L2Item item :  SkillsEngine.getInstance().loadItems())
		{
			if (highest < item.getItemId())
				highest = item.getItemId();
			if (item instanceof L2EtcItem)
				_etcItems.put(item.getItemId(), (L2EtcItem) item);
			else if (item instanceof L2Armor)
				_armors.put(item.getItemId(), (L2Armor) item);
			else
				_weapons.put(item.getItemId(), (L2Weapon) item);
		}
		buildFastLookupTable(highest);
	}
	
	/**
	 * Builds a variable in which all items are putting in in function of their ID.
	 */
	private void buildFastLookupTable(int size)
	{
		// Create a FastLookUp Table called _allTemplates of size : value of the highest item ID
		_log.info("Highest item id used:" + size);
		_allTemplates = new L2Item[size + 1];
		
		// Insert armor item in Fast Look Up Table
		for (L2Armor item : _armors.values())
		{
			_allTemplates[item.getItemId()] = item;
		}
		
		// Insert weapon item in Fast Look Up Table
		for (L2Weapon item : _weapons.values())
		{
			_allTemplates[item.getItemId()] = item;
		}
		
		// Insert etcItem item in Fast Look Up Table
		for (L2EtcItem item : _etcItems.values())
		{
			_allTemplates[item.getItemId()] = item;
		}
	}
	
	/**
	 * Returns the item corresponding to the item ID
	 * @param id : int designating the item
	 * @return L2Item
	 */
	public L2Item getTemplate(int id)
	{
		if (id >= _allTemplates.length)
			return null;
		
		return _allTemplates[id];
	}
	
	/**
	 * Create the L2ItemInstance corresponding to the Item Identifier and quantitiy add logs the activity.<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Create and Init the L2ItemInstance corresponding to the Item Identifier and quantity </li>
	 * <li>Add the L2ItemInstance object to _allObjects of L2world </li>
	 * <li>Logs Item creation according to log settings</li><BR><BR>
	 *
	 * @param process : String Identifier of process triggering this action
	 * @param itemId : int Item Identifier of the item to be created
	 * @param count : int Quantity of items to be created for stackable items
	 * @param actor : L2PcInstance Player requesting the item creation
	 * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the new item
	 */
	public L2ItemInstance createItem(String process, int itemId, long count, L2PcInstance actor, Object reference)
	{
		// Create and Init the L2ItemInstance corresponding to the Item Identifier
		L2ItemInstance item = new L2ItemInstance(IdFactory.getInstance().getNextId(), itemId);
		
		if (process.equalsIgnoreCase("loot"))
		{
			ScheduledFuture<?> itemLootShedule;
			if (reference instanceof L2Attackable && ((L2Attackable) reference).isRaid()) // loot privilege for raids
			{
				L2Attackable raid = (L2Attackable) reference;
				// if in CommandChannel and was killing a World/RaidBoss
				if (raid.getFirstCommandChannelAttacked() != null && !Config.AUTO_LOOT_RAIDS)
				{
					item.setOwnerId(raid.getFirstCommandChannelAttacked().getChannelLeader().getObjectId());
					itemLootShedule = ThreadPoolManager.getInstance().scheduleGeneral(new ResetOwner(item), Config.LOOT_RAIDS_PRIVILEGE_INTERVAL);
					item.setItemLootShedule(itemLootShedule);
				}
			}
			else if (!Config.AUTO_LOOT || (reference instanceof L2EventMonsterInstance && ((L2EventMonsterInstance)reference).eventDropOnGround()))
			{
				item.setOwnerId(actor.getObjectId());
				itemLootShedule = ThreadPoolManager.getInstance().scheduleGeneral(new ResetOwner(item), 15000);
				item.setItemLootShedule(itemLootShedule);
			}
		}
		
		if (Config.DEBUG)
			_log.fine("ItemTable: Item created  oid:" + item.getObjectId() + " itemid:" + itemId);
		
		// Add the L2ItemInstance object to _allObjects of L2world
		L2World.getInstance().storeObject(item);
		
		// Set Item parameters
		if (item.isStackable() && count > 1)
			item.setCount(count);
		
		if (Config.LOG_ITEMS && !process.equals("Reset"))
		{
			if (!Config.LOG_ITEMS_SMALL_LOG || (Config.LOG_ITEMS_SMALL_LOG && (item.isEquipable() || item.getItemId() == ADENA_ID)))
			{
				LogRecord record = new LogRecord(Level.INFO, "CREATE:" + process);
				record.setLoggerName("item");
				record.setParameters(new Object[] { item, actor, reference });
				_logItems.log(record);
			}
		}
		
		if (actor != null)
		{
			if (actor.isGM())
			{
				String referenceName = "no-reference";
				if (reference instanceof L2Object)
				{
					referenceName = (((L2Object)reference).getName() != null?((L2Object)reference).getName():"no-name");
				}
				else if (reference instanceof String)
					referenceName = (String)reference;
				String targetName = (actor.getTarget() != null ? actor.getTarget().getName() : "no-target");
				if (Config.GMAUDIT)
					GMAudit.auditGMAction(actor.getName()+" ["+actor.getObjectId()+"]", process + "(id: " + itemId + " count: " + count + " name: " + item.getItemName()
							+ " objId: " + item.getObjectId() + ")", targetName, "L2Object referencing this action is: " + referenceName);
			}
		}
		
		return item;
	}
	
	public L2ItemInstance createItem(String process, int itemId, int count, L2PcInstance actor)
	{
		return createItem(process, itemId, count, actor, null);
	}
	
	/**
	 * Returns a dummy (fr = factice) item.<BR><BR>
	 * <U><I>Concept :</I></U><BR>
	 * Dummy item is created by setting the ID of the object in the world at null value
	 * @param itemId : int designating the item
	 * @return L2ItemInstance designating the dummy item created
	 */
	public L2ItemInstance createDummyItem(int itemId)
	{
		L2Item item = getTemplate(itemId);
		if (item == null)
			return null;
		L2ItemInstance temp = new L2ItemInstance(0, item);
		return temp;
	}
	
	/**
	 * Destroys the L2ItemInstance.<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Sets L2ItemInstance parameters to be unusable </li>
	 * <li>Removes the L2ItemInstance object to _allObjects of L2world </li>
	 * <li>Logs Item delettion according to log settings</li><BR><BR>
	 *
	 * @param process : String Identifier of process triggering this action
	 * @param itemId : int Item Identifier of the item to be created
	 * @param actor : L2PcInstance Player requesting the item destroy
	 * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
	 */
	public void destroyItem(String process, L2ItemInstance item, L2PcInstance actor, Object reference)
	{
		synchronized (item)
		{
			long old = item.getCount();
			item.setCount(0);
			item.setOwnerId(0);
			item.setLocation(ItemLocation.VOID);
			item.setLastChange(L2ItemInstance.REMOVED);
			
			L2World.getInstance().removeObject(item);
			IdFactory.getInstance().releaseId(item.getObjectId());
			
			if (Config.LOG_ITEMS)
			{
				if (!Config.LOG_ITEMS_SMALL_LOG || (Config.LOG_ITEMS_SMALL_LOG && (item.isEquipable() || item.getItemId() == ADENA_ID)))
				{
					LogRecord record = new LogRecord(Level.INFO, "DELETE:" + process);
					record.setLoggerName("item");
					record.setParameters(new Object[] { item, "PrevCount("+old+")", actor, reference });
					_logItems.log(record);
				}
			}
			
			if (actor != null)
			{
				if (actor.isGM())
				{
					String referenceName = "no-reference";
					if (reference instanceof L2Object)
					{
						referenceName = (((L2Object)reference).getName() != null?((L2Object)reference).getName():"no-name");
					}
					else if (reference instanceof String)
						referenceName = (String)reference;
					String targetName = (actor.getTarget() != null ? actor.getTarget().getName() : "no-target");
					if (Config.GMAUDIT)
						GMAudit.auditGMAction(actor.getName()+" ["+actor.getObjectId()+"]", process + "(id: " + item.getItemId() + " count: " + item.getCount()
								+ " itemObjId: " + item.getObjectId() + ")", targetName, "L2Object referencing this action is: "
								+ referenceName);
				}
			}
			
			// if it's a pet control item, delete the pet as well
			if (PetDataTable.isPetItem(item.getItemId()))
			{
				Connection con = null;
				try
				{
					// Delete the pet in db
					con = L2DatabaseFactory.getInstance().getConnection();
					PreparedStatement statement = con.prepareStatement("DELETE FROM pets WHERE item_obj_id=?");
					statement.setInt(1, item.getObjectId());
					statement.execute();
					statement.close();
				}
				catch (Exception e)
				{
					_log.log(Level.WARNING, "could not delete pet objectid:", e);
				}
				finally
				{
					L2DatabaseFactory.close(con);
				}
			}
		}
	}
	
	public void reload()
	{
		load();
		EnchantHPBonusData.getInstance().reload();
	}
	
	protected static class ResetOwner implements Runnable
	{
		L2ItemInstance _item;
		
		public ResetOwner(L2ItemInstance item)
		{
			_item = item;
		}
		
		public void run()
		{
			_item.setOwnerId(0);
			_item.setItemLootShedule(null);
		}
	}
	
	public Set<Integer> getAllArmorsId()
	{
		return _armors.keySet();
	}
	
	public Set<Integer> getAllWeaponsId()
	{
		return _weapons.keySet();
	}
	
	public int getArraySize()
	{
		return _allTemplates.length;
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final ItemTable _instance = new ItemTable();
	}
}
