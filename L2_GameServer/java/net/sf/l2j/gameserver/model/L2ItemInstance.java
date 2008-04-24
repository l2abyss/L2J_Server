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
package net.sf.l2j.gameserver.model;


import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.GeoData;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.instancemanager.ItemsOnGroundManager;
import net.sf.l2j.gameserver.instancemanager.MercTicketManager;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.knownlist.NullKnownList;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.funcs.Func;
import net.sf.l2j.gameserver.templates.L2Armor;
import net.sf.l2j.gameserver.templates.L2EtcItem;
import net.sf.l2j.gameserver.templates.L2Item;
import net.sf.l2j.gameserver.templates.L2Weapon;


/**
 * This class manages items.
 *
 * @version $Revision: 1.4.2.1.2.11 $ $Date: 2005/03/31 16:07:50 $
 */
public final class L2ItemInstance extends L2Object
{
	private static final Logger _log = Logger.getLogger(L2ItemInstance.class.getName());
	private static final Logger _logItems = Logger.getLogger("item");

	/** Enumeration of locations for item */
	public static enum ItemLocation {
		VOID,
		INVENTORY,
		PAPERDOLL,
		WAREHOUSE,
		CLANWH,
		PET,
		PET_EQUIP,
		LEASE,
        FREIGHT,
        NPC
	}

	/** ID of the owner */
	private int _ownerId;

	/** Quantity of the item */
	private int _count;
	/** Initial Quantity of the item */
	private int _initCount;
	/** Time after restore Item count (in Hours) */
	private int _time;
	/** Quantity of the item can decrease */
	private boolean _decrease = false;
	
	/** ID of the item */
	private final int _itemId;
	
	/** Object L2Item associated to the item */
	private final L2Item _item;
	
	/** Location of the item : Inventory, PaperDoll, WareHouse */
	private ItemLocation _loc;
	
	/** Slot where item is stored : Paperdoll slot, inventory order ...*/
	private int _locData;
	
	/** Level of enchantment of the item */
	private int _enchantLevel;
	
	/** Wear Item */
	private boolean _wear;
	
	/** Augmented Item */
	private L2Augmentation _augmentation=null;
	
	/** Shadow item */
	private int _mana=-1;
	private boolean _consumingMana = false;
	private static final int MANA_CONSUMPTION_RATE = 60000;

	/** Custom item types (used loto, race tickets) */
	private int _type1;
	private int _type2;

	private long _dropTime;

	public static final int CHARGED_NONE				=	0;
	public static final int CHARGED_SOULSHOT				=	1;
	public static final int CHARGED_SPIRITSHOT			=	1;
	public static final int CHARGED_BLESSED_SOULSHOT		=	2; // It's a really exists? ;-)
	public static final int CHARGED_BLESSED_SPIRITSHOT		=	2;

	/** Item charged with SoulShot (type of SoulShot) */
	private int				_chargedSoulshot			=	CHARGED_NONE;
	/** Item charged with SpiritShot (type of SpiritShot) */
	private int				_chargedSpiritshot		=	CHARGED_NONE;

	private boolean _chargedFishtshot =	false;

	private boolean _protected;

	public static final int UNCHANGED = 0;
	public static final int ADDED = 1;
	public static final int REMOVED = 3;
	public static final int MODIFIED = 2;
	private int _lastChange = 2;	//1 ??, 2 modified, 3 removed
	private boolean _existsInDb; // if a record exists in DB.
	private boolean _storedInDb; // if DB data is up-to-date.

    private int                 ae_enchantLvl               = 0;
    private int                 ae_enchantElement           = -1;
    private int                 ae_enchantVal               = 0;
    private int                 ad_fire                     = 0;
    private int                 ad_water                    = 0;
    private int                 ad_earth                    = 0;
    private int                 ad_wind                     = 0;
    private int                 ad_holy                     = 0;
    private int                 ad_unholy                   = 0;

	private ScheduledFuture<?> itemLootShedule = null;
	/**
	 * Constructor of the L2ItemInstance from the objectId and the itemId.
	 * @param objectId : int designating the ID of the object in the world
	 * @param itemId : int designating the ID of the item
	 */
	public L2ItemInstance(int objectId, int itemId)
	{
		super(objectId);
		super.setKnownList(new NullKnownList(this));
		_itemId = itemId;
		_item = ItemTable.getInstance().getTemplate(itemId);
		if (_itemId == 0 || _item == null)
			throw new IllegalArgumentException();
		super.setName(_item.getName());
		setCount(1);
		_loc = ItemLocation.VOID;
		_type1 = 0;
		_type2 = 0;
		_dropTime = 0;
		_mana = _item.getDuration();
	}

	/**
	 * Constructor of the L2ItemInstance from the objetId and the description of the item given by the L2Item.
	 * @param objectId : int designating the ID of the object in the world
	 * @param item : L2Item containing informations of the item
	 */
	public L2ItemInstance(int objectId, L2Item item)
	{
		super(objectId);
		super.setKnownList(new NullKnownList(this));
		_itemId = item.getItemId();
		_item = item;
		if (_itemId == 0 || _item == null)
			throw new IllegalArgumentException();
		super.setName(_item.getName());
		setCount(1);
		_loc = ItemLocation.VOID;
		_mana = _item.getDuration();
	}

	/**
	 * Sets the ownerID of the item
	 * @param process : String Identifier of process triggering this action
	 * @param owner_id : int designating the ID of the owner
	 * @param creator : L2PcInstance Player requesting the item creation
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 */
	public void setOwnerId(String process, int owner_id, L2PcInstance creator, L2Object reference)
	{
		setOwnerId(owner_id);

		if (Config.LOG_ITEMS)
		{
			LogRecord record = new LogRecord(Level.INFO, "CHANGE:" + process);
			record.setLoggerName("item");
			record.setParameters(new Object[]{this, creator, reference});
			_logItems.log(record);
		}
	}

	/**
	 * Sets the ownerID of the item
	 * @param owner_id : int designating the ID of the owner
	 */
	public void setOwnerId(int owner_id)
	{
		if (owner_id == _ownerId) return;

		_ownerId = owner_id;
		_storedInDb = false;
	}

	/**
	 * Returns the ownerID of the item
	 * @return int : ownerID of the item
	 */
	public int getOwnerId()
	{
		return _ownerId;
	}

	/**
	 * Sets the location of the item
	 * @param loc : ItemLocation (enumeration)
	 */
	public void setLocation(ItemLocation loc)
	{
		setLocation(loc, 0);
	}

	/**
	 * Sets the location of the item.<BR><BR>
	 * <U><I>Remark :</I></U> If loc and loc_data different from database, say datas not up-to-date
	 * @param loc : ItemLocation (enumeration)
	 * @param loc_data : int designating the slot where the item is stored or the village for freights
	 */
	public void setLocation(ItemLocation loc, int loc_data)
	{
		if (loc == _loc && loc_data == _locData)
			return;
		_loc = loc;
		_locData = loc_data;
		_storedInDb = false;
	}

	public ItemLocation getLocation()
	{
		return _loc;
	}
    
    /**
     * Sets the quantity of the item.<BR><BR>
     * @param count the new count to set
     */
    public void setCount(int count)
    {
        if (getCount() == count) 
        {
            return;
        }
        
        _count = count >= -1 ? count : 0;
        _storedInDb = false;
    }

    /**
     * @return Returns the count.
     */
    public int getCount()
    {
        return _count;
    }

    /**
	 * Sets the quantity of the item.<BR><BR>
	 * <U><I>Remark :</I></U> If loc and loc_data different from database, say datas not up-to-date
	 * @param process : String Identifier of process triggering this action
	 * @param count : int
	 * @param creator : L2PcInstance Player requesting the item creation
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 */
	public void changeCount(String process, int count, L2PcInstance creator, L2Object reference)
	{
        if (count == 0) 
        {
            return;
        }
        
        if ( count > 0 && getCount() > Integer.MAX_VALUE - count) 
        {
            setCount(Integer.MAX_VALUE);
        }
        else 
        {
            setCount(getCount() + count);
        }
        
        if (getCount() < 0) 
        {
            setCount(0);
        }
        
        _storedInDb = false;

		if (Config.LOG_ITEMS && process != null)
		{
			LogRecord record = new LogRecord(Level.INFO, "CHANGE:" + process);
			record.setLoggerName("item");
			record.setParameters(new Object[]{this, creator, reference});
			_logItems.log(record);
		}
	}

	// No logging (function designed for shots only)
	public void changeCountWithoutTrace(int count, L2PcInstance creator, L2Object reference)
	{
        this.changeCount(null, count, creator, reference);
	}

	

	/**
	 * Returns if item is equipable
	 * @return boolean
	 */
	public boolean isEquipable()
	{
		return !(_item.getBodyPart() == 0 || _item instanceof L2EtcItem );
	}

	/**
	 * Returns if item is equipped
	 * @return boolean
	 */
	public boolean isEquipped()
	{
		return _loc == ItemLocation.PAPERDOLL || _loc == ItemLocation.PET_EQUIP;
	}

	/**
	 * Returns the slot where the item is stored
	 * @return int
	 */
	public int getLocationSlot()
	{
		if (Config.ASSERT) assert _loc == ItemLocation.PAPERDOLL || _loc == ItemLocation.PET_EQUIP || _loc == ItemLocation.FREIGHT || _loc == ItemLocation.INVENTORY;
		return _locData;
	}

	/**
	 * Returns the characteristics of the item
	 * @return L2Item
	 */
	public L2Item getItem()
	{
		return _item;
	}

	public int getCustomType1()
	{
		return _type1;
	}
	public int getCustomType2()
	{
		return _type2;
	}
	public void setCustomType1(int newtype)
	{
		_type1=newtype;
	}
	public void setCustomType2(int newtype)
	{
		_type2=newtype;
	}
	public void setDropTime(long time)
	{
		_dropTime=time;
	}
	public long getDropTime()
	{
		return _dropTime;
	}

	public boolean isWear()
	{
		return _wear;
	}

	public void setWear(boolean newwear)
	{
		_wear=newwear;
	}
	/**
	 * Returns the type of item
	 * @return Enum
	 */
	@SuppressWarnings("unchecked")
    public Enum getItemType()
	{
		return _item.getItemType();
	}

	/**
	 * Returns the ID of the item
	 * @return int
	 */
	public int getItemId()
	{
		return _itemId;
	}

	/**
	 * Returns true if item is an EtcItem
	 * @return boolean
	 */
	public boolean isEtcItem()
	{
		return (_item instanceof L2EtcItem);
	}
	
	/**
	 * Returns true if item is a Weapon/Shield
	 * @return boolean
	 */
	public boolean isWeapon()
	{
		return (_item instanceof L2Weapon);
	}
	
	/**
	 * Returns true if item is an Armor
	 * @return boolean
	 */
	public boolean isArmor()
	{
		return (_item instanceof L2Armor);
	}
	
	/**
	 * Returns the characteristics of the L2EtcItem
	 * @return L2EtcItem
	 */
	public L2EtcItem getEtcItem()
	{
		if (_item instanceof L2EtcItem)
		{
			return (L2EtcItem) _item;
		}
		return null;
	}
	
	/**
	 * Returns the characteristics of the L2Weapon
	 * @return L2Weapon
	 */
	public L2Weapon getWeaponItem()
	{
		if (_item instanceof L2Weapon)
		{
			return (L2Weapon) _item;
		}
		return null;
	}
	
	/**
	 * Returns the characteristics of the L2Armor
	 * @return L2Armor
	 */
	public L2Armor getArmorItem()
	{
		if (_item instanceof L2Armor)
		{
			return (L2Armor) _item;
		}
		return null;
	}

    /**
	 * Returns the quantity of crystals for crystallization
	 * 
	 * @return int
	 */
    public final int getCrystalCount()
    {
        return _item.getCrystalCount(_enchantLevel);
    }

	/**
	 * Returns the reference price of the item
	 * @return int
	 */
	public int getReferencePrice()
	{
		return _item.getReferencePrice();
	}

	/**
	 * Returns the name of the item
	 * @return String
	 */
	public String getItemName()
	{
		return _item.getName();
	}

	/**
	 * Returns the last change of the item
	 * @return int
	 */
	public int getLastChange()
	{
		return _lastChange;
	}

	/**
	 * Sets the last change of the item
	 * @param lastChange : int
	 */
	public void setLastChange(int lastChange)
	{
		_lastChange = lastChange;
	}

	/**
	 * Returns if item is stackable
	 * @return boolean
	 */
	public boolean isStackable()
	{
		return _item.isStackable();
	}

	/**
	 * Returns if item is dropable
	 * @return boolean
	 */
	public boolean isDropable()
	{
		return isAugmented() ? false : _item.isDropable();
	}

	/**
	 * Returns if item is destroyable
	 * @return boolean
	 */
	public boolean isDestroyable()
	{
		return _item.isDestroyable();
	}

	/**
	 * Returns if item is tradeable
	 * @return boolean
	 */
	public boolean isTradeable()
	{
		return isAugmented() ? false : _item.isTradeable();
	}

    /**
     * Returns if item is consumable
     * @return boolean
     */
    public boolean isConsumable()
    {
        return _item.isConsumable();
    }
    
    public boolean isHeroItem()
    {
        return ((_itemId >= 6611 && _itemId <= 6621) || (_itemId >= 9388 && _itemId <= 9390) || _itemId == 6842);
    }
    
    public boolean isOlyRestrictedItem()
    {
        return Config.LIST_OLY_RESTRICTED_ITEMS.contains(_itemId);
    }

    /**
     * Returns if item is available for manipulation
     * @return boolean
     */
    public boolean isAvailable(L2PcInstance player, boolean allowAdena)
    {
    	return (
		(!isEquipped()) // Not equipped
    		&& (getItem().getType2() != 3) // Not Quest Item
    		&& (getItem().getType2() != 4 || getItem().getType1() != 1) // TODO: what does this mean?
    		&& (player.getPet() == null || getObjectId() != player.getPet().getControlItemId()) // Not Control item of currently summoned pet
    		&& (player.getActiveEnchantItem() != this) // Not momentarily used enchant scroll
    		&& (allowAdena || getItemId() != 57) // Not adena
    		&& (player.getCurrentSkill() == null || player.getCurrentSkill().getSkill().getItemConsumeId() != getItemId())
		&& (isTradeable())
    		);
    }

    /* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.model.L2Object#onAction(net.sf.l2j.gameserver.model.L2PcInstance)
	 * also check constraints: only soloing castle owners may pick up mercenary tickets of their castle
	 */
	@Override
	public void onAction(L2PcInstance player)
	{
		// this causes the validate position handler to do the pickup if the location is reached.
		// mercenary tickets can only be picked up by the castle owner.
        int castleId = MercTicketManager.getInstance().getTicketCastleId(_itemId);
        
        if (castleId > 0 && 
                (!player.isCastleLord(castleId) || player.isInParty()))
        {
            if  (player.isInParty())    //do not allow owner who is in party to pick tickets up
                player.sendMessage("You cannot pickup mercenaries while in a party.");
            else
                player.sendMessage("Only the castle lord can pickup mercenaries.");

            player.setTarget(this);
            player.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
            // Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
            player.sendPacket(ActionFailed.STATIC_PACKET);
        }
		else
			player.getAI().setIntention(CtrlIntention.AI_INTENTION_PICK_UP, this);
	}
	/**
	 * Returns the level of enchantment of the item
	 * @return int
	 */
	public int getEnchantLevel()
	{
		return _enchantLevel;
	}

	/**
	 * Sets the level of enchantment of the item
	 * @param int
	 */
	public void setEnchantLevel(int enchantLevel)
	{
		if (_enchantLevel == enchantLevel)
			return;
		_enchantLevel = enchantLevel;
		_storedInDb = false;
	}

	/**
	 * Returns the physical defense of the item
	 * @return int
	 */
	public int getPDef()
	{
		if (_item instanceof L2Armor)
			return ((L2Armor)_item).getPDef();
		return 0;
	}

	/**
	 * Returns whether this item is augmented or not
	 * @return true if augmented
	 */
	public boolean isAugmented()
	{
		return _augmentation == null ? false : true;
	}

	/**
	 * Returns the augmentation object for this item
	 * @return augmentation
	 */
	public L2Augmentation getAugmentation()
	{
		return _augmentation;
	}

	/**
	 * Sets a new augmentation
	 * @param augmentation
	 * @return return true if sucessfull
	 */
	public boolean setAugmentation(L2Augmentation augmentation)
	{
		// there shall be no previous augmentation..
		if (_augmentation != null) return false;
		_augmentation = augmentation;
		return true;
	}

	/**
	 * Remove the augmentation
	 *
	 */
	public void removeAugmentation()
	{
		if (_augmentation == null) return;
		_augmentation.deleteAugmentationData();
		_augmentation = null;
	}


	/**
	 * Used to decrease mana
	 * (mana means life time for shadow items)
	 */
	public class ScheduleConsumeManaTask implements Runnable
	{
		private L2ItemInstance _shadowItem;

		public ScheduleConsumeManaTask(L2ItemInstance item)
		{
			_shadowItem = item;
		}

		public void run()
		{
			try
			{
				// decrease mana
				if (_shadowItem != null) _shadowItem.decreaseMana(true);
			}
			catch (Throwable t)
			{
			}
		}
	}


	/**
	 * Returns true if this item is a shadow item
	 * Shadow items have a limited life-time
	 * @return
	 */
	public boolean isShadowItem()
	{
		return (_mana >= 0);
	}

	/**
	 * Sets the mana for this shadow item
	 * <b>NOTE</b>: does not send an inventory update packet
	 * @param mana
	 */
	public void setMana(int mana)
	{
		_mana = mana;
	}

	/**
	 * Returns the remaining mana of this shadow item
	 * @return lifeTime
	 */
	public int getMana()
	{
		return _mana;
	}

	/**
	 * Decreases the mana of this shadow item,
	 * sends a inventory update
	 * schedules a new consumption task if non is running
	 * optionally one could force a new task
	 * @param forces a new consumption task if item is equipped
	 */
	public void decreaseMana(boolean resetConsumingMana)
	{
		if (!isShadowItem()) return;

		if (_mana > 0) _mana--;

		if (_storedInDb) _storedInDb = false;
		if (resetConsumingMana) _consumingMana = false;

		L2PcInstance player = ((L2PcInstance)L2World.getInstance().findObject(getOwnerId()));
		if (player != null)
		{
			SystemMessage sm;
			switch (_mana)
			{
				case 10:
					sm = new SystemMessage(SystemMessageId.S1S_REMAINING_MANA_IS_NOW_10);
					sm.addString(getItemName());
					player.sendPacket(sm);
					break;
				case 5:
					sm = new SystemMessage(SystemMessageId.S1S_REMAINING_MANA_IS_NOW_5);
					sm.addString(getItemName());
					player.sendPacket(sm);
					break;
				case 1:
					sm = new SystemMessage(SystemMessageId.S1S_REMAINING_MANA_IS_NOW_1);
					sm.addString(getItemName());
					player.sendPacket(sm);
					break;
			}

			if (_mana == 0) // The life time has expired
			{
				sm = new SystemMessage(SystemMessageId.S1S_REMAINING_MANA_IS_NOW_0);
				sm.addString(getItemName());
				player.sendPacket(sm);

				// unequip
				if (isEquipped())
				{
					L2ItemInstance[] unequiped = player.getInventory().unEquipItemInSlotAndRecord(getLocationSlot());
					InventoryUpdate iu = new InventoryUpdate();
					for (int i = 0; i < unequiped.length; i++)
					{
						player.checkSSMatch(null, unequiped[i]);
						iu.addModifiedItem(unequiped[i]);
					}
					player.sendPacket(iu);
				}

				if (getLocation() != ItemLocation.WAREHOUSE)
				{
					// destroy
					player.getInventory().destroyItem("L2ItemInstance", this, player, null);

					// send update
					InventoryUpdate iu = new InventoryUpdate();
					iu.addRemovedItem(this);
					player.sendPacket(iu);

					StatusUpdate su = new StatusUpdate(player.getObjectId());
					su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
					player.sendPacket(su);

				}
				else
				{
					player.getWarehouse().destroyItem("L2ItemInstance", this, player, null);
				}

				// delete from world
				L2World.getInstance().removeObject(this);
			}
			else
			{
				// Reschedule if still equipped
				if (!_consumingMana && isEquipped())
				{
					scheduleConsumeManaTask();
				}
				if (getLocation() != ItemLocation.WAREHOUSE)
				{
					InventoryUpdate iu = new InventoryUpdate();
					iu.addModifiedItem(this);
					player.sendPacket(iu);
				}
			}
		}
	}

	private void scheduleConsumeManaTask()
	{
		_consumingMana = true;
		ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleConsumeManaTask(this), MANA_CONSUMPTION_RATE);
	}

	/**
	 * Returns false cause item can't be attacked
	 * @return boolean false
	 */
    @Override
	public boolean isAutoAttackable(@SuppressWarnings("unused") L2Character attacker)
    {
        return false;
    }

	/**
	 * Returns the type of charge with SoulShot of the item.
	 * @return int (CHARGED_NONE, CHARGED_SOULSHOT)
	 */
	public int getChargedSoulshot()
	{
		return 	_chargedSoulshot;
	}

	/**
	 * Returns the type of charge with SpiritShot of the item
	 * @return int (CHARGED_NONE, CHARGED_SPIRITSHOT, CHARGED_BLESSED_SPIRITSHOT)
	 */
	public int getChargedSpiritshot()
	{
		return _chargedSpiritshot;
	}
	public boolean getChargedFishshot()
	{
		return _chargedFishtshot;
	}

	/**
	 * Sets the type of charge with SoulShot of the item
	 * @param type : int (CHARGED_NONE, CHARGED_SOULSHOT)
	 */
	public void setChargedSoulshot(int type)
	{
		_chargedSoulshot = type;
	}

	/**
	 * Sets the type of charge with SpiritShot of the item
	 * @param type : int (CHARGED_NONE, CHARGED_SPIRITSHOT, CHARGED_BLESSED_SPIRITSHOT)
	 */
	public void setChargedSpiritshot(int type)
	{
		_chargedSpiritshot = type;
	}
	public void setChargedFishshot(boolean type)
	{
		_chargedFishtshot = type;
	}

    /**
     * This function basically returns a set of functions from
     * L2Item/L2Armor/L2Weapon, but may add additional
     * functions, if this particular item instance is enhanched
     * for a particular player.
     * @param player : L2Character designating the player
     * @return Func[]
     */
    public Func[] getStatFuncs(L2Character player)
    {
    	return getItem().getStatFuncs(this, player);
    }
    
    /**
     * Updates the database.<BR>
     */
    public void updateDatabase()
    {
        this.updateDatabase(false);
    }
    
    /**
     * Updates the database.<BR>
     * 
     * @param force if the update should necessarilly be done.
     */
	public void updateDatabase(boolean force)
	{
		if (isWear()) //avoid saving weared items
		{
			return;
		}
        
		if (_existsInDb)
        {
			if (_ownerId == 0 || _loc == ItemLocation.VOID || (getCount() == 0 && _loc != ItemLocation.LEASE))
            {
				removeFromDb();
            }
			else if (!Config.LAZY_ITEMS_UPDATE || force)
            {
				updateInDb();
            }
		} 
        else
        {
			if (getCount() == 0 && _loc != ItemLocation.LEASE)
            {
				return;
            }
			if (_loc == ItemLocation.VOID || _loc == ItemLocation.NPC || _ownerId == 0)
            {
				return;
            }
			insertIntoDb();
		}
	}

	/**
	 * Returns a L2ItemInstance stored in database from its objectID
	 * @param objectId : int designating the objectID of the item
	 * @return L2ItemInstance
	 */
	public static L2ItemInstance restoreFromDb(int ownerId, ResultSet rs) 
	{
		L2ItemInstance inst = null;
		int objectId, item_id, count, loc_data, enchant_level, custom_type1, custom_type2, manaLeft;
		ItemLocation loc;
		try
		{
		    objectId = rs.getInt(1);
		    item_id = rs.getInt("item_id");
		    count = rs.getInt("count");
		    loc = ItemLocation.valueOf(rs.getString("loc"));
		    loc_data = rs.getInt("loc_data");
		    enchant_level = rs.getInt("enchant_level");
		    custom_type1 =  rs.getInt("custom_type1");
		    custom_type2 =  rs.getInt("custom_type2");
		    manaLeft = rs.getInt("mana_left");
		} catch (Exception e) {
		    _log.log(Level.SEVERE, "Could not restore an item owned by "+ownerId+" from DB:", e);
		    return null;
		}
		L2Item item = ItemTable.getInstance().getTemplate(item_id);
		if (item == null) {
		    _log.severe("Item item_id="+item_id+" not known, object_id="+objectId);
		    return null;
		}
		inst = new L2ItemInstance(objectId, item);
		inst._ownerId = ownerId;
		inst.setCount(count);
		inst._enchantLevel = enchant_level;
		inst._type1 = custom_type1;
		inst._type2 = custom_type2;
		inst._loc = loc;
		inst._locData = loc_data;
        inst._existsInDb = true;
        inst._storedInDb = true;
		
		// Setup life time for shadow weapons
		inst._mana = manaLeft;

		// consume 1 mana
		if (inst._mana > 0 && inst.getLocation() == ItemLocation.PAPERDOLL)
		    inst.decreaseMana(false);

		// if mana left is 0 delete this item
		if (inst._mana == 0)
		{
		    inst.removeFromDb();
		    return null;
		}
		else if (inst._mana > 0 && inst.getLocation() == ItemLocation.PAPERDOLL)
		    inst.scheduleConsumeManaTask();

		//load augmentation
		if (inst.isEquipable())
		{
		    java.sql.Connection con = null;
		    try
		    {
		        con = L2DatabaseFactory.getInstance().getConnection();
		        PreparedStatement statement = con.prepareStatement("SELECT attributes,skill,level FROM augmentations WHERE item_id=?");
                statement.setInt(1, objectId);
                rs = statement.executeQuery();
                if (rs.next())
                {
                    inst._augmentation = new L2Augmentation(inst, rs.getInt("attributes"), rs.getInt("skill"), rs.getInt("level"), false);
                }

                rs.close();
                statement.close();
		    }
            catch (Exception e)
            {
		        _log.log(Level.SEVERE, "Could not restore augmentation for item "+objectId+" from DB: "+e.getMessage(), e);
		    }
            finally
            {
		        try { con.close(); } catch (Exception e) {}
		    }
		}
        
		return inst;
	}

    /**
     * Init a dropped L2ItemInstance and add it in the world as a visible object.<BR><BR>
     *
     * <B><U> Actions</U> :</B><BR><BR>
     * <li>Set the x,y,z position of the L2ItemInstance dropped and update its _worldregion </li>
     * <li>Add the L2ItemInstance dropped to _visibleObjects of its L2WorldRegion</li>
     * <li>Add the L2ItemInstance dropped in the world as a <B>visible</B> object</li><BR><BR>
     *
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T ADD the object to _allObjects of L2World </B></FONT><BR><BR>
     *
     * <B><U> Assert </U> :</B><BR><BR>
     * <li> _worldRegion == null <I>(L2Object is invisible at the beginning)</I></li><BR><BR>
     *
     * <B><U> Example of use </U> :</B><BR><BR>
     * <li> Drop item</li>
     * <li> Call Pet</li><BR>
     *
     */
    public final void dropMe(L2Character dropper, int x, int y, int z)
    {
        if (Config.ASSERT) assert getPosition().getWorldRegion() == null;

        if (Config.GEODATA > 0 && dropper != null)
        {
            Location dropDest = GeoData.getInstance().moveCheck(dropper.getX(), dropper.getY(), dropper.getZ(), x, y, z);
            x = dropDest.getX();
            y = dropDest.getY();
            z = dropDest.getZ();
        }

        synchronized (this)
        {
            // Set the x,y,z position of the L2ItemInstance dropped and update its _worldregion
            setIsVisible(true);
            getPosition().setWorldPosition(x, y ,z);
            getPosition().setWorldRegion(L2World.getInstance().getRegion(getPosition().getWorldPosition()));

            // Add the L2ItemInstance dropped to _visibleObjects of its L2WorldRegion
            getPosition().getWorldRegion().addVisibleObject(this);
        }
        setDropTime(System.currentTimeMillis());

        // this can synchronize on others instancies, so it's out of
        // synchronized, to avoid deadlocks
        // Add the L2ItemInstance dropped in the world as a visible object
        L2World.getInstance().addVisibleObject(this, getPosition().getWorldRegion(), dropper);
        if (Config.SAVE_DROPPED_ITEM)
        	ItemsOnGroundManager.getInstance().save(this);
    }

	/**
	 * Update the database with values of the item
	 */
	private void updateInDb()
    {
        if (Config.ASSERT) assert _existsInDb;
        
		if (_wear)
			return;
        
		if (_storedInDb)
			return;

		java.sql.Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(
					"UPDATE items SET owner_id=?,count=?,loc=?,loc_data=?,enchant_level=?,custom_type1=?,custom_type2=?,mana_left=? " +
					"WHERE object_id = ?");
			statement.setInt(1, _ownerId);
			statement.setInt(2, getCount());
			statement.setString(3, _loc.name());
			statement.setInt(4, _locData);
			statement.setInt(5, getEnchantLevel());
			statement.setInt(6, getCustomType1());
			statement.setInt(7, getCustomType2());
			statement.setInt(8, getMana());
			statement.setInt(9, getObjectId());
			statement.executeUpdate();
			_existsInDb = true;
			_storedInDb = true;
            statement.close();
        }
        catch (Exception e)
        {
			_log.log(Level.SEVERE, "Could not update item "+getObjectId()+" in DB: Reason: "+e.getMessage(), e);
		}
        finally
        {
			try { con.close(); } catch (Exception e) {}
		}
	}

	/**
	 * Insert the item in database
	 */
	private void insertIntoDb() {
		if (_wear)
			return;
		if (Config.ASSERT) assert !_existsInDb && getObjectId() != 0;
		java.sql.Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(
					"INSERT INTO items (owner_id,item_id,count,loc,loc_data,enchant_level,object_id,custom_type1,custom_type2,mana_left) " +
					"VALUES (?,?,?,?,?,?,?,?,?,?)");
			statement.setInt(1, _ownerId);
			statement.setInt(2, _itemId);
			statement.setInt(3, getCount());
			statement.setString(4, _loc.name());
			statement.setInt(5, _locData);
			statement.setInt(6, getEnchantLevel());
			statement.setInt(7, getObjectId());
			statement.setInt(8, _type1);
			statement.setInt(9, _type2);
			statement.setInt(10, getMana());

			statement.executeUpdate();
			_existsInDb = true;
			_storedInDb = true;
            statement.close();
        }
        catch (Exception e)
        {
			_log.log(Level.SEVERE, "Could not insert item "+getObjectId()+" into DB: Reason: "+e.getMessage(), e);
		}
        finally
        {
			try { con.close(); } catch (Exception e) {}
		}
	}

	/**
	 * Delete item from database
	 */
	private void removeFromDb()
    {
		if (_wear)
			return;
		if (Config.ASSERT) assert _existsInDb;

		// delete augmentation data
		if (isAugmented()) _augmentation.deleteAugmentationData();

		java.sql.Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(
					"DELETE FROM items WHERE object_id=?");
			statement.setInt(1, getObjectId());
			statement.executeUpdate();
			_existsInDb = false;
			_storedInDb = false;
            statement.close();
        }
        catch (Exception e)
        {
			_log.log(Level.SEVERE, "Could not delete item "+getObjectId()+" in DB: "+e.getMessage(), e);
		}
        finally
        {
			try { con.close(); } catch (Exception e) {}
		}
	}

	/**
	 * Returns the item in String format
	 * @return String
	 */
	@Override
	public String toString()
	{
		return ""+_item;
	}
    public void resetOwnerTimer()
    {
    	if(itemLootShedule != null)
    		itemLootShedule.cancel(true);
    	itemLootShedule = null;
    }
    public void setItemLootShedule(ScheduledFuture<?> sf)
    {
    	itemLootShedule = sf;
    }
    public ScheduledFuture<?> getItemLootShedule()
    {
    	return itemLootShedule;
    }
    public void setProtected(boolean is_protected)
    {
    	_protected = is_protected;
    }
    public boolean isProtected()
    {
    	return _protected;
    }
    public boolean isNightLure()
    {
    	return ((_itemId >= 8505 && _itemId <= 8513) || _itemId == 8485);
    }
    
    public void setCountDecrease(boolean decrease)
    {
    	_decrease = decrease;
    }
    
    public boolean getCountDecrease()
    {
    	return _decrease;
    }
    
    public void setInitCount(int InitCount)
    {
    	_initCount = InitCount;
    }
    
    public int getInitCount()
    {
    	return _initCount;
    }
    
    public void restoreInitCount()
    {
    	if(_decrease)
    		setCount(_initCount);
    }
    
    public void setTime(int time)
    {
    	_time = time > 0 ?  time : 0;
    }
    
    public int getTime()
    {
    	return _time;
    }
    
    public int getAttackAttrElement()
    {
        return ae_enchantElement;
    }
    
    public int getAttackAttrElementVal() 
    {
        return ae_enchantVal;
    }
    
    public int getDefAttrFire()
    {
        return ad_fire;  
    }
    
    public int getDefAttrWater()
    {
        return ad_water;
    }
    
    public int getDefAttrEarth()
    {
        return ad_earth;
    }
    
    public int getDefAttrWind()
    {
        return ad_wind;
    }
    
    public int getDefAttrHoly()
    {
        return ad_holy;
    }
    
    public int getDefAttrUnholy()
    {
        return ad_unholy;
    }
    
    public int getEnchantAttrLevel()
    {
        return ae_enchantLvl;
    }
    
    public boolean canBeAttrEnchanted(int element)
    {
        return ae_enchantElement == -1 || ae_enchantElement == element;
    }
    
    public void setEnchantAttrLevel(int lv)
    {
        ae_enchantLvl = lv;
    }
    
    public void setEnchantAttrElement(int element)
    {
        ae_enchantElement = element;
    }
    
    public int getEnchantAttrElement()
    {
        return ae_enchantElement;
    }
    
    public void setEnchantAttrValue(int val)
    {
        ae_enchantVal = val;
    }
    
    public int getEnchantAttrValue() 
    {
        return ae_enchantVal;
    }
    
    public int getEnchantAttrDef(int element)
    {
        int value = 0;
        switch (element)
        {
            case 0:
                value = ad_fire;
                break;
            case 1:
                value = ad_water;
                break;
            case 2:
                value = ad_wind;
                break;
            case 3:
                value = ad_earth;
                break;
            case 4:
                value = ad_holy;
                break;
            case 5:
                value = ad_unholy;
                break;
            default:
                break;
        }
        return value;
    }
    public void setEnchantAttrDef(int element, int value)
    {
        switch (element)
        {
            case 0:
                ad_fire = value;
                break;
            case 1:
                ad_water = value;
                break;
            case 2:
                ad_wind = value;
                break;
            case 3:
                ad_earth = value;
                break;
            case 4:
                ad_holy = value;
                break;
            case 5:
                ad_unholy = value;
                break;
            default:
                break;
        }
    }
    public void clearEnchantAttr()
    {
        ae_enchantLvl = 0;
        ae_enchantElement = -1;
        ae_enchantVal = 0;
        ad_fire = 0;
        ad_water = 0;
        ad_wind = 0;
        ad_earth = 0;
        ad_holy = 0;
        ad_unholy = 0;
    }
}
