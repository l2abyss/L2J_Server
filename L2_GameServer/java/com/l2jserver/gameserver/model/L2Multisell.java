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
package com.l2jserver.gameserver.model;

import gnu.trove.TIntObjectHashMap;

import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;

import javolution.util.FastList;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.l2jserver.Config;
import com.l2jserver.gameserver.datatables.ItemTable;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.itemcontainer.PcInventory;
import com.l2jserver.gameserver.network.serverpackets.MultiSellList;
import com.l2jserver.gameserver.templates.item.L2Armor;
import com.l2jserver.gameserver.templates.item.L2Item;
import com.l2jserver.gameserver.templates.item.L2Weapon;

/**
 * Multisell list manager.
 */
public class L2Multisell
{
	private static final Logger _log = Logger.getLogger(L2Multisell.class.getName());
	private final TIntObjectHashMap<MultiSellListContainer> _entries = new TIntObjectHashMap<MultiSellListContainer>();
	
	/**
	 * Instantiates a new l2 multisell.
	 */
	private L2Multisell()
	{
		reload();
	}
	
	/**
	 * Gets the single instance of L2Multisell.
	 *
	 * @return single instance of L2Multisell
	 */
	public static L2Multisell getInstance()
	{
		return SingletonHolder._instance;
	}
	
	/**
	 * Reload.
	 */
	public void reload()
	{
		_entries.clear();
		parse();
		_log.info("L2Multisell: Loaded " + _entries.size() + " lists.");
	}
	
	/**
	 * Gets the list.
	 *
	 * @param id the id
	 * @return the list
	 */
	public MultiSellListContainer getList(int id)
	{
		final MultiSellListContainer list = _entries.get(id);
		
		if (list != null)
			return list;
		
		_log.warning("[L2Multisell] can't find list with id: " + id);
		return null;
	}
	
	/**
	 * This will generate the multisell list for the items.  There exist various
	 * parameters in multisells that affect the way they will appear:
	 * 1) inventory only:
	 * * if true, only show items of the multisell for which the
	 * "primary" ingredients are already in the player's inventory.  By "primary"
	 * ingredients we mean weapon and armor.
	 * * if false, show the entire list.
	 * 2) maintain enchantment: presumably, only lists with "inventory only" set to true
	 * should sometimes have this as true.  This makes no sense otherwise...
	 * * If true, then the product will match the enchantment level of the ingredient.
	 * if the player has multiple items that match the ingredient list but the enchantment
	 * levels differ, then the entries need to be duplicated to show the products and
	 * ingredients for each enchantment level.
	 * For example: If the player has a crystal staff +1 and a crystal staff +3 and goes
	 * to exchange it at the mammon, the list should have all exchange possibilities for
	 * the +1 staff, followed by all possibilities for the +3 staff.
	 * * If false, then any level ingredient will be considered equal and product will always
	 * be at +0
	 * 3) apply taxes: Uses the "taxIngredient" entry in order to add a certain amount of adena to the ingredients
	 *
	 * @param listId the list id
	 * @param inventoryOnly the inventory only
	 * @param player the player
	 * @param npcId the npc id
	 * @param taxRate the tax rate
	 * @return the multisell list container
	 * @see com.l2jserver.util.network.BaseSendablePacket.ServerBasePacket#runImpl()
	 */
	private MultiSellListContainer generateMultiSell(int listId, boolean inventoryOnly, L2PcInstance player, int npcId, double taxRate)
	{
		MultiSellListContainer listTemplate = getList(listId);
		MultiSellListContainer list = new MultiSellListContainer();
		if (listTemplate == null)
			return list;
		list.setListId(listId);
		if (npcId != 0 && !listTemplate.checkNpcId(npcId))
			listTemplate.addNpcId(npcId);
		
		if (inventoryOnly)
		{
			if (player == null)
				return list;
			
			L2ItemInstance[] items;
			if (listTemplate.getMaintainEnchantment())
				items = player.getInventory().getUniqueItemsByEnchantLevel(false, false, false);
			else
				items = player.getInventory().getUniqueItems(false, false, false);
			
			int enchantLevel, elementId, elementValue, augmentId, fireVal, waterVal, windVal, earthVal, holyVal, darkVal;
			for (L2ItemInstance item : items)
			{
				// only do the matchup on equipable items that are not currently equipped
				// so for each appropriate item, produce a set of entries for the multisell list.
				if (!item.isEquipped() && ((item.getItem() instanceof L2Armor) || (item.getItem() instanceof L2Weapon)))
				{
					enchantLevel = (listTemplate.getMaintainEnchantment() ? item.getEnchantLevel() : 0);
					augmentId = (listTemplate.getMaintainEnchantment() ? (item.getAugmentation() != null ? item.getAugmentation().getAugmentationId() : 0) : 0);
					elementId = (listTemplate.getMaintainEnchantment() ? item.getAttackElementType() : -2);
					elementValue = (listTemplate.getMaintainEnchantment() ? item.getAttackElementPower() : 0);
					fireVal = (listTemplate.getMaintainEnchantment() ? item.getElementDefAttr(Elementals.FIRE) : 0);
					waterVal = (listTemplate.getMaintainEnchantment() ? item.getElementDefAttr(Elementals.WATER) : 0);
					windVal = (listTemplate.getMaintainEnchantment() ? item.getElementDefAttr(Elementals.WIND) : 0);
					earthVal = (listTemplate.getMaintainEnchantment() ? item.getElementDefAttr(Elementals.EARTH) : 0);
					holyVal = (listTemplate.getMaintainEnchantment() ? item.getElementDefAttr(Elementals.HOLY) : 0);
					darkVal = (listTemplate.getMaintainEnchantment() ? item.getElementDefAttr(Elementals.DARK) : 0);
					// loop through the entries to see which ones we wish to include
					for (MultiSellEntry ent : listTemplate.getEntries())
					{
						boolean doInclude = false;
						
						// check ingredients of this entry to see if it's an entry we'd like to include.
						for (MultiSellIngredient ing : ent.getIngredients())
						{
							if (item.getItemId() == ing.getItemId())
							{
								doInclude = true;
								break;
							}
						}
						
						// manipulate the ingredients of the template entry for this particular instance shown
						// i.e: Assign enchant levels and/or apply taxes as needed.
						if (doInclude)
							list.addEntry(prepareEntry(ent, listTemplate.getApplyTaxes(), listTemplate.getMaintainEnchantment(), enchantLevel, augmentId, elementId, elementValue, fireVal, waterVal, windVal, earthVal, holyVal, darkVal, taxRate));
					}
				}
			} // end for each inventory item.
		} // end if "inventory-only"
		else
		// this is a list-all type
		{
			// if no taxes are applied, no modifications are needed
			for (MultiSellEntry ent : listTemplate.getEntries())
				list.addEntry(prepareEntry(ent, listTemplate.getApplyTaxes(), false, 0, 0, -2, 0, 0, 0, 0, 0, 0, 0, taxRate));
		}
		
		return list;
	}
	
	// Regarding taxation, the following is the case:
	// a) The taxes come out purely from the adena TaxIngredient
	// b) If the entry has no adena ingredients other than the taxIngredient, the resulting
	// amount of adena is appended to the entry
	// c) If the entry already has adena as an entry, the taxIngredient is used in order to increase
	//	  the count for the existing adena ingredient
	/**
	 * Prepare entry.
	 *
	 * @param templateEntry the template entry
	 * @param applyTaxes the apply taxes
	 * @param maintainEnchantment the maintain enchantment
	 * @param enchantLevel the enchant level
	 * @param augmentId the augment id
	 * @param elementId the element id
	 * @param elementValue the element value
	 * @param fireValue the fire value
	 * @param waterValue the water value
	 * @param windValue the wind value
	 * @param earthValue the earth value
	 * @param holyValue the holy value
	 * @param darkValue the dark value
	 * @param taxRate the tax rate
	 * @return the multisell entry
	 */
	private MultiSellEntry prepareEntry(MultiSellEntry templateEntry, boolean applyTaxes, boolean maintainEnchantment, int enchantLevel, int augmentId, int elementId, int elementValue, int fireValue, int waterValue, int windValue, int earthValue, int holyValue, int darkValue, double taxRate)
	{
		MultiSellEntry newEntry = new MultiSellEntry();
		newEntry.setEntryId(templateEntry.getEntryId() * 100000 + enchantLevel);
		long adenaAmount = 0;
		
		for (MultiSellIngredient ing : templateEntry.getIngredients())
		{
			// load the ingredient from the template
			MultiSellIngredient newIngredient = new MultiSellIngredient(ing);
			
			// if taxes are to be applied, modify/add the adena count based on the template adena/ancient adena count
			if (ing.getItemId() == PcInventory.ADENA_ID && ing.isTaxIngredient())
			{
				if (applyTaxes)
					adenaAmount += Math.round(ing.getItemCount() * taxRate);
				continue; // do not adena yet, as non-taxIngredient adena entries might occur next (order not guaranteed)
			}
			else if (ing.getItemId() == PcInventory.ADENA_ID) // && !ing.isTaxIngredient()
			{
				adenaAmount += ing.getItemCount();
				continue; // do not adena yet, as taxIngredient adena entries might occur next (order not guaranteed)
			}
			// if it is an armor/weapon, modify the enchantment level appropriately, if necessary
			// not used for clan reputation and fame
			else if (maintainEnchantment && newIngredient.getItemId() > 0)
			{
				L2Item tempItem = ItemTable.getInstance().createDummyItem(ing.getItemId()).getItem();
				if ((tempItem instanceof L2Armor) || (tempItem instanceof L2Weapon))
				{
					newIngredient.setEnchantmentLevel(enchantLevel);
					newIngredient.setAugmentId(augmentId);
					newIngredient.setElementId(elementId);
					newIngredient.setElementValue(elementValue);
					newIngredient.setFireValue(fireValue);
					newIngredient.setWaterValue(waterValue);
					newIngredient.setWindValue(windValue);
					newIngredient.setEarthValue(earthValue);
					newIngredient.setHolyValue(holyValue);
					newIngredient.setDarkValue(darkValue);
				}
			}
			
			// finally, add this ingredient to the entry
			newEntry.addIngredient(newIngredient);
		}
		// now add the adena, if any.
		if (adenaAmount > 0)
		{
			newEntry.addIngredient(new MultiSellIngredient(PcInventory.ADENA_ID, adenaAmount, 0, 0, -2, 0, 0, 0, 0, 0, 0, 0, false, false));
		}
		// Now modify the enchantment level of products, if necessary
		for (MultiSellIngredient ing : templateEntry.getProducts())
		{
			// load the ingredient from the template
			MultiSellIngredient newIngredient = new MultiSellIngredient(ing);
			
			if (maintainEnchantment)
			{
				// if it is an armor/weapon, modify the enchantment level appropriately
				// (note, if maintain enchantment is "false" this modification will result to a +0)
				L2Item tempItem = ItemTable.getInstance().createDummyItem(ing.getItemId()).getItem();
				if ((tempItem instanceof L2Armor) || (tempItem instanceof L2Weapon))
				{
					newIngredient.setEnchantmentLevel(enchantLevel);
					newIngredient.setAugmentId(augmentId);
					newIngredient.setElementId(elementId);
					newIngredient.setElementValue(elementValue);
					newIngredient.setFireValue(fireValue);
					newIngredient.setWaterValue(waterValue);
					newIngredient.setWindValue(windValue);
					newIngredient.setEarthValue(earthValue);
					newIngredient.setHolyValue(holyValue);
					newIngredient.setDarkValue(darkValue);
				}
			}
			newEntry.addProduct(newIngredient);
		}
		return newEntry;
	}
	
	/**
	 * Separate and send.
	 *
	 * @param listId the list id
	 * @param player the player
	 * @param npcId the npc id
	 * @param inventoryOnly the inventory only
	 * @param taxRate the tax rate
	 */
	public void separateAndSend(int listId, L2PcInstance player, int npcId, boolean inventoryOnly, double taxRate)
	{
		MultiSellListContainer list = generateMultiSell(listId, inventoryOnly, player, npcId, taxRate);
		MultiSellListContainer temp = new MultiSellListContainer();
		int page = 1;
		
		temp.setListId(list.getListId());
		
		for (MultiSellEntry e : list.getEntries())
		{
			if (temp.getEntries().size() == 40)
			{
				player.sendPacket(new MultiSellList(temp, page++, 0));
				temp = new MultiSellListContainer();
				temp.setListId(list.getListId());
			}
			temp.addEntry(e);
		}
		player.sendPacket(new MultiSellList(temp, page, 1));
	}
	
	/**
	 * The Class MultiSellEntry.
	 */
	public class MultiSellEntry
	{
		private int _entryId;
		
		private List<MultiSellIngredient> _products = new FastList<MultiSellIngredient>();
		private List<MultiSellIngredient> _ingredients = new FastList<MultiSellIngredient>();
		
		/**
		 * Sets the entry id.
		 *
		 * @param entryId The entryId to set.
		 */
		public void setEntryId(int entryId)
		{
			_entryId = entryId;
		}
		
		/**
		 * Gets the entry id.
		 *
		 * @return Returns the entryId.
		 */
		public int getEntryId()
		{
			return _entryId;
		}
		
		/**
		 * Adds the product.
		 *
		 * @param product The product to add.
		 */
		public void addProduct(MultiSellIngredient product)
		{
			_products.add(product);
		}
		
		/**
		 * Gets the products.
		 *
		 * @return Returns the products.
		 */
		public List<MultiSellIngredient> getProducts()
		{
			return _products;
		}
		
		/**
		 * Adds the ingredient.
		 *
		 * @param ingredient The ingredient to add to ingredients.
		 */
		public void addIngredient(MultiSellIngredient ingredient)
		{
			_ingredients.add(ingredient);
		}
		
		/**
		 * Gets the ingredients.
		 *
		 * @return Returns the ingredients.
		 */
		public List<MultiSellIngredient> getIngredients()
		{
			return _ingredients;
		}
		
		/**
		 * Stackable.
		 *
		 * @return the int
		 */
		public int stackable()
		{
			for (MultiSellIngredient p : _products)
			{
				if (p.getItemId() > 0)
				{
					L2Item template = ItemTable.getInstance().getTemplate(p.getItemId());
					if (template != null && !template.isStackable())
						return 0;
				}
			}
			return 1;
		}
	}
	
	/**
	 * The Class MultiSellIngredient.
	 */
	public class MultiSellIngredient
	{
		private int _itemId, _enchantmentLevel, _element, _elementVal, _augment, _fireVal, _waterVal, _windVal, _earthVal, _holyVal, _darkVal;
		private long _itemCount;
		private boolean _isTaxIngredient, _maintainIngredient;
		
		/**
		 * Instantiates a new multisell ingredient.
		 *
		 * @param itemId the item id
		 * @param itemCount the item count
		 * @param isTaxIngredient the is tax ingredient
		 * @param maintainIngredient the maintain ingredient
		 */
		public MultiSellIngredient(int itemId, long itemCount, boolean isTaxIngredient, boolean maintainIngredient)
		{
			this(itemId, itemCount, 0, 0, -2, 0, 0, 0, 0, 0, 0, 0, isTaxIngredient, maintainIngredient);
		}
		
		/**
		 * Instantiates a new multisell ingredient.
		 *
		 * @param itemId the item id
		 * @param itemCount the item count
		 * @param enchantmentLevel the enchantment level
		 * @param augmentId the augment id
		 * @param elementId the element id
		 * @param elementVal the element val
		 * @param fireVal the fire val
		 * @param waterVal the water val
		 * @param windVal the wind val
		 * @param earthVal the earth val
		 * @param holyVal the holy val
		 * @param darkVal the dark val
		 * @param isTaxIngredient the is tax ingredient
		 * @param maintainIngredient the maintain ingredient
		 */
		public MultiSellIngredient(int itemId, long itemCount, int enchantmentLevel, int augmentId, int elementId, int elementVal, int fireVal, int waterVal, int windVal, int earthVal, int holyVal, int darkVal, boolean isTaxIngredient, boolean maintainIngredient)
		{
			setItemId(itemId);
			setItemCount(itemCount);
			setEnchantmentLevel(enchantmentLevel);
			setAugmentId(augmentId);
			setElementId(elementId);
			setElementValue(elementVal);
			setFireValue(fireVal);
			setWaterValue(waterVal);
			setWindValue(windVal);
			setEarthValue(earthVal);
			setHolyValue(holyVal);
			setDarkValue(darkVal);
			setIsTaxIngredient(isTaxIngredient);
			setMaintainIngredient(maintainIngredient);
		}
		
		/**
		 * Instantiates a new multisell ingredient.
		 *
		 * @param e the e
		 */
		public MultiSellIngredient(MultiSellIngredient e)
		{
			_itemId = e.getItemId();
			_itemCount = e.getItemCount();
			_enchantmentLevel = e.getEnchantmentLevel();
			_isTaxIngredient = e.isTaxIngredient();
			_maintainIngredient = e.getMaintainIngredient();
			_augment = e.getAugmentId();
			_element = e.getElementId();
			_elementVal = e.getElementVal();
			_fireVal = e.getFireVal();
			_waterVal = e.getWaterVal();
			_windVal = e.getWindVal();
			_earthVal = e.getEarthVal();
			_holyVal = e.getHolyVal();
			_darkVal = e.getDarkVal();
		}
		
		/**
		 * Sets the augment id.
		 *
		 * @param augment the new augment id
		 */
		public void setAugmentId(int augment)
		{
			_augment = augment;
		}
		
		/**
		 * Sets the element id.
		 *
		 * @param element the new element id
		 */
		public void setElementId(int element)
		{
			_element = element;
		}
		
		/**
		 * Sets the element value.
		 *
		 * @param elementVal the new element value
		 */
		public void setElementValue(int elementVal)
		{
			_elementVal = elementVal;
		}
		
		/**
		 * Sets the fire value.
		 *
		 * @param val the new fire value
		 */
		public void setFireValue(int val)
		{
			_fireVal = val;
		}
		
		/**
		 * Sets the water value.
		 *
		 * @param val the new water value
		 */
		public void setWaterValue(int val)
		{
			_waterVal = val;
		}
		
		/**
		 * Sets the wind value.
		 *
		 * @param val the new wind value
		 */
		public void setWindValue(int val)
		{
			_windVal = val;
		}
		
		/**
		 * Sets the earth value.
		 *
		 * @param val the new earth value
		 */
		public void setEarthValue(int val)
		{
			_earthVal = val;
		}
		
		/**
		 * Sets the holy value.
		 *
		 * @param val the new holy value
		 */
		public void setHolyValue(int val)
		{
			_holyVal = val;
		}
		
		/**
		 * Sets the dark value.
		 *
		 * @param val the new dark value
		 */
		public void setDarkValue(int val)
		{
			_darkVal = val;
		}
		
		/**
		 * Gets the augment id.
		 *
		 * @return the augment id
		 */
		public int getAugmentId()
		{
			return _augment;
		}
		
		/**
		 * Gets the element id.
		 *
		 * @return the element id
		 */
		public int getElementId()
		{
			return _element;
		}
		
		/**
		 * Gets the element val.
		 *
		 * @return the element val
		 */
		public int getElementVal()
		{
			return _elementVal;
		}
		
		/**
		 * Gets the fire val.
		 *
		 * @return the fire val
		 */
		public int getFireVal()
		{
			return _fireVal;
		}
		
		/**
		 * Gets the water val.
		 *
		 * @return the water val
		 */
		public int getWaterVal()
		{
			return _waterVal;
		}
		
		/**
		 * Gets the wind val.
		 *
		 * @return the wind val
		 */
		public int getWindVal()
		{
			return _windVal;
		}
		
		/**
		 * Gets the earth val.
		 *
		 * @return the earth val
		 */
		public int getEarthVal()
		{
			return _earthVal;
		}
		
		/**
		 * Gets the holy val.
		 *
		 * @return the holy val
		 */
		public int getHolyVal()
		{
			return _holyVal;
		}
		
		/**
		 * Gets the dark val.
		 *
		 * @return the dark val
		 */
		public int getDarkVal()
		{
			return _darkVal;
		}
		
		/**
		 * Sets the item id.
		 *
		 * @param itemId The itemId to set.
		 */
		public void setItemId(int itemId)
		{
			_itemId = itemId;
		}
		
		/**
		 * Gets the item id.
		 *
		 * @return Returns the itemId.
		 */
		public int getItemId()
		{
			return _itemId;
		}
		
		/**
		 * Sets the item count.
		 *
		 * @param itemCount The itemCount to set.
		 */
		public void setItemCount(long itemCount)
		{
			_itemCount = itemCount;
		}
		
		/**
		 * Gets the item count.
		 *
		 * @return Returns the itemCount.
		 */
		public long getItemCount()
		{
			return _itemCount;
		}
		
		/**
		 * Sets the enchantment level.
		 *
		 * @param enchantmentLevel The enchantmentLevel to set.
		 */
		public void setEnchantmentLevel(int enchantmentLevel)
		{
			_enchantmentLevel = enchantmentLevel;
		}
		
		/**
		 * Gets the enchantment level.
		 *
		 * @return Returns the enchantmentLevel.
		 */
		public int getEnchantmentLevel()
		{
			return _enchantmentLevel;
		}
		
		/**
		 * Sets the checks if is tax ingredient.
		 *
		 * @param isTaxIngredient the new checks if is tax ingredient
		 */
		public void setIsTaxIngredient(boolean isTaxIngredient)
		{
			_isTaxIngredient = isTaxIngredient;
		}
		
		/**
		 * Checks if is tax ingredient.
		 *
		 * @return true, if is tax ingredient
		 */
		public boolean isTaxIngredient()
		{
			return _isTaxIngredient;
		}
		
		/**
		 * Sets the maintain ingredient.
		 *
		 * @param maintainIngredient the new maintain ingredient
		 */
		public void setMaintainIngredient(boolean maintainIngredient)
		{
			_maintainIngredient = maintainIngredient;
		}
		
		/**
		 * Gets the maintain ingredient.
		 *
		 * @return the maintain ingredient
		 */
		public boolean getMaintainIngredient()
		{
			return _maintainIngredient;
		}
	}
	
	/**
	 * The Class MultiSellListContainer.
	 */
	public class MultiSellListContainer
	{
		private int _listId;
		private boolean _applyTaxes = false;
		private boolean _maintainEnchantment = false;
		private List<Integer> _npcIds;
		
		List<MultiSellEntry> _entriesC;
		
		/**
		 * Instantiates a new multisell list container.
		 */
		public MultiSellListContainer()
		{
			_entriesC = new FastList<MultiSellEntry>();
		}
		
		/**
		 * Sets the list id.
		 *
		 * @param listId The listId to set.
		 */
		public void setListId(int listId)
		{
			_listId = listId;
		}
		
		/**
		 * Sets the apply taxes.
		 *
		 * @param applyTaxes the new apply taxes
		 */
		public void setApplyTaxes(boolean applyTaxes)
		{
			_applyTaxes = applyTaxes;
		}
		
		/**
		 * Sets the maintain enchantment.
		 *
		 * @param maintainEnchantment the new maintain enchantment
		 */
		public void setMaintainEnchantment(boolean maintainEnchantment)
		{
			_maintainEnchantment = maintainEnchantment;
		}
		
		/**
		 * Adds the npc id.
		 *
		 * @param objId the obj id
		 */
		public void addNpcId(int objId)
		{
			_npcIds.add(objId);
		}
		
		/**
		 * Gets the list id.
		 *
		 * @return Returns the listId.
		 */
		public int getListId()
		{
			return _listId;
		}
		
		/**
		 * Gets the apply taxes.
		 *
		 * @return the apply taxes
		 */
		public boolean getApplyTaxes()
		{
			return _applyTaxes;
		}
		
		/**
		 * Gets the maintain enchantment.
		 *
		 * @return the maintain enchantment
		 */
		public boolean getMaintainEnchantment()
		{
			return _maintainEnchantment;
		}
		
		/**
		 * Check npc id.
		 *
		 * @param npcId the npc id
		 * @return true, if successful
		 */
		public boolean checkNpcId(int npcId)
		{
			if (_npcIds == null)
			{
				synchronized (this)
				{
					if (_npcIds == null)
						_npcIds = new FastList<Integer>();
				}
				return false;
			}
			
			return _npcIds.contains(npcId);
		}
		
		/**
		 * Adds the entry.
		 *
		 * @param e the e
		 */
		public void addEntry(MultiSellEntry e)
		{
			_entriesC.add(e);
		}
		
		/**
		 * Gets the entries.
		 *
		 * @return the entries
		 */
		public List<MultiSellEntry> getEntries()
		{
			return _entriesC;
		}
	}
	
	/**
	 * Hash files.
	 *
	 * @param dirname the dirname
	 * @param hash the hash
	 */
	private void hashFiles(String dirname, List<File> hash)
	{
		File dir = new File(Config.DATAPACK_ROOT, "data/" + dirname);
		if (!dir.exists())
		{
			_log.warning("Dir " + dir.getAbsolutePath() + " not exists");
			return;
		}
		File[] files = dir.listFiles();
		for (File f : files)
		{
			if (f.getName().endsWith(".xml"))
				hash.add(f);
		}
	}
	
	/**
	 * Parses the.
	 */
	private void parse()
	{
		Document doc = null;
		int id = 0;
		List<File> files = new FastList<File>();
		hashFiles("multisell", files);
		
		for (File f : files)
		{
			try
			{
				id = Integer.parseInt(f.getName().replaceAll(".xml", ""));
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				factory.setValidating(false);
				factory.setIgnoringComments(true);
				doc = factory.newDocumentBuilder().parse(f);
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "Error loading file " + f, e);
				continue;
			}
			try
			{
				MultiSellListContainer list = parseDocument(doc);
				list.setListId(id);
				_entries.put(id, list);
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "Error in file " + f, e);
			}
		}
	}
	
	/**
	 * Parses the document.
	 *
	 * @param doc the doc
	 * @return the multisell list container
	 */
	protected MultiSellListContainer parseDocument(Document doc)
	{
		MultiSellListContainer list = new MultiSellListContainer();
		
		int entryId = 1;
		for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				if (n.getAttributes() != null)
				{
					Node attribute = n.getAttributes().getNamedItem("applyTaxes");
					if (attribute == null)
						list.setApplyTaxes(false);
					else
						list.setApplyTaxes(Boolean.parseBoolean(attribute.getNodeValue()));
					
					attribute = n.getAttributes().getNamedItem("maintainEnchantment");
					if (attribute == null)
						list.setMaintainEnchantment(false);
					else
						list.setMaintainEnchantment(Boolean.parseBoolean(attribute.getNodeValue()));
					
					for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					{
						if ("item".equalsIgnoreCase(d.getNodeName()))
						{
							MultiSellEntry e = parseEntry(d, entryId++);
							list.addEntry(e);
						}
					}
				}
			}
			else if ("item".equalsIgnoreCase(n.getNodeName()))
			{
				MultiSellEntry e = parseEntry(n, entryId++);
				list.addEntry(e);
			}
		}
		
		return list;
	}
	
	/**
	 * Parses the entry.
	 *
	 * @param n the n
	 * @param entryId the entry id
	 * @return the multisell entry
	 */
	protected MultiSellEntry parseEntry(Node n, int entryId)
	{
		Node first = n.getFirstChild();
		MultiSellEntry entry = new MultiSellEntry();
		
		for (n = first; n != null; n = n.getNextSibling())
		{
			if ("ingredient".equalsIgnoreCase(n.getNodeName()))
			{
				Node attribute;
				
				int id = Integer.parseInt(n.getAttributes().getNamedItem("id").getNodeValue());
				long count = Long.parseLong(n.getAttributes().getNamedItem("count").getNodeValue());
				boolean isTaxIngredient = false, maintainIngredient = false;
				
				attribute = n.getAttributes().getNamedItem("isTaxIngredient");
				
				if (attribute != null)
					isTaxIngredient = Boolean.parseBoolean(attribute.getNodeValue());
				
				attribute = n.getAttributes().getNamedItem("maintainIngredient");
				
				if (attribute != null)
					maintainIngredient = Boolean.parseBoolean(attribute.getNodeValue());
				
				MultiSellIngredient e = new MultiSellIngredient(id, count, isTaxIngredient, maintainIngredient);
				entry.addIngredient(e);
				
				validateItemId(id);
			}
			else if ("production".equalsIgnoreCase(n.getNodeName()))
			{
				int id = Integer.parseInt(n.getAttributes().getNamedItem("id").getNodeValue());
				long count = Long.parseLong(n.getAttributes().getNamedItem("count").getNodeValue());
				
				MultiSellIngredient e = new MultiSellIngredient(id, count, false, false);
				entry.addProduct(e);
				
				validateItemId(id);
			}
		}
		
		entry.setEntryId(entryId);
		
		return entry;
	}
	
	/**
	 * Validate item id.
	 *
	 * @param itemId the item id
	 */
	private void validateItemId(int itemId)
	{
		switch (itemId)
		{
			case -200: // Clan Reputation Score
			case -300: // Player Fame
			{
				break;
			}
			default:
			{
				L2Item template = ItemTable.getInstance().getTemplate(itemId);
				if (template == null)
					_log.warning("[L2Multisell] can't find item with itemId: " + itemId);
			}
		}
	}
	
	/**
	 * The Class SingletonHolder.
	 */
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final L2Multisell _instance = new L2Multisell();
	}
}
