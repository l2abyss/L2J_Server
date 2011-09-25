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
package com.l2jserver.gameserver.templates.item;

import java.util.ArrayList;
import java.util.List;

import com.l2jserver.gameserver.model.L2ExtractableProduct;
import com.l2jserver.gameserver.model.itemcontainer.PcInventory;
import com.l2jserver.gameserver.templates.StatsSet;
import com.l2jserver.util.StringUtil;

/**
 * This class is dedicated to the management of EtcItem.
 *
 * @version $Revision: 1.2.2.1.2.3 $ $Date: 2005/03/27 15:30:10 $
 */
public final class L2EtcItem  extends L2Item
{
	// private final String[] _skill;
	private String _handler;
	private final int _sharedReuseGroup;
	private L2EtcItemType _type;
	private final boolean _isBlessed;
	private final List<L2ExtractableProduct> _extractableItems;
	
	/**
	 * Constructor for EtcItem.
	 * @see L2Item constructor
	 * @param set : StatsSet designating the set of couples (key,value) for description of the Etc
	 */
	public L2EtcItem(StatsSet set)
	{
		super(set);
		_type = L2EtcItemType.valueOf(set.getString("etcitem_type", "none").toUpperCase());
		
		// l2j custom - L2EtcItemType.SHOT
		switch (getDefaultAction())
		{
			case soulshot:
			case summon_soulshot:
			case summon_spiritshot:
			case spiritshot:
			{
				_type = L2EtcItemType.SHOT;
				break;
			}
		}
		
		if (is_ex_immediate_effect())
			_type = L2EtcItemType.HERB;
		
		_type1 = L2Item.TYPE1_ITEM_QUESTITEM_ADENA;
		_type2 = L2Item.TYPE2_OTHER; // default is other
		
		if (isQuestItem())
			_type2 = L2Item.TYPE2_QUEST;
		else if (getItemId() == PcInventory.ADENA_ID || getItemId() == PcInventory.ANCIENT_ADENA_ID)
			_type2 = L2Item.TYPE2_MONEY;
		
		_handler = set.getString("handler", null);  // ! null !
		_sharedReuseGroup = set.getInteger("shared_reuse_group", -1);
		_isBlessed = set.getBool("blessed", false);
		
		//extractable
		String capsuled_items = set.getString("capsuled_items", null);
		if (capsuled_items != null)
		{
			String[] split = capsuled_items.split(";");
			_extractableItems = new ArrayList<L2ExtractableProduct>(split.length);
			for (String part : split)
			{
				if (part.trim().isEmpty())
					continue;
				String[] data =  part.split(",");
				if (data.length != 4)
				{
					_log.info(StringUtil.concat("> Couldnt parse ", part, " in capsuled_items! item ", this.toString()));
					continue;
				}
				int itemId = Integer.parseInt(data[0]);
				int min = Integer.parseInt(data[1]);
				int max = Integer.parseInt(data[2]);
				double chance = Double.parseDouble(data[3]);
				if (max < min)
				{
					_log.info(StringUtil.concat("> Max amount < Min amount in ", part, ", item ",this.toString()));
					continue;
				}
				L2ExtractableProduct product = new L2ExtractableProduct(itemId, min, max, chance);
				_extractableItems.add(product);
			}
			((ArrayList<?>) _extractableItems).trimToSize();
			
			//check for handler
			if (_handler == null)
				//_log.warning("Item "+this+ " define capsuled_items but missing handler.");
				_handler = "ExtractableItems";
		}
		else 
			_extractableItems = null;
	}
	
	/**
	 * Returns the type of Etc Item
	 * @return L2EtcItemType
	 */
	@Override
	public L2EtcItemType getItemType()
	{
		return _type;
	}
	
	/**
	 * Returns if the item is consumable
	 * @return boolean
	 */
	@Override
	public final boolean isConsumable()
	{
		return ((getItemType() == L2EtcItemType.SHOT) || (getItemType() == L2EtcItemType.POTION)); // || (type == L2EtcItemType.SCROLL));
	}
	
	/**
	 * Returns the ID of the Etc item after applying the mask.
	 * @return int : ID of the EtcItem
	 */
	@Override
	public int getItemMask()
	{
		return getItemType().mask();
	}
	
	/**
	 * Return handler name. null if no handler for item
	 * @return String
	 */
	public String getHandlerName()
	{
		return _handler;
	}
	
	/**
	 * 
	 * @return
	 */
	public int getSharedReuseGroup()
	{
		return _sharedReuseGroup;
	}
	
	/**
	 * 
	 * @return
	 */
	public final boolean isBlessed()
	{
		return _isBlessed;
	}

	/**
	 * @return the _extractable_items
	 */
	public List<L2ExtractableProduct> getExtractableItems()
	{
		return _extractableItems;
	}
}