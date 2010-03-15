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

import com.l2jserver.gameserver.skills.SkillHolder;
import com.l2jserver.gameserver.templates.StatsSet;
import com.l2jserver.gameserver.util.StringUtil;

/**
 * This class is dedicated to the management of EtcItem.
 *
 * @version $Revision: 1.2.2.1.2.3 $ $Date: 2005/03/27 15:30:10 $
 */
public final class L2EtcItem  extends L2Item
{
	// private final String[] _skill;
	private final String _handler;
	private SkillHolder[] _skillHolder;
	/**
	 * Constructor for EtcItem.
	 * @see L2Item constructor
	 * @param type : L2EtcItemType designating the type of object Etc
	 * @param set : StatsSet designating the set of couples (key,value) for description of the Etc
	 */
	public L2EtcItem(L2EtcItemType type, StatsSet set)
	{
		super(type, set);
		String[] skills = set.getString("skill").split(";");
		
		_skillHolder = new SkillHolder[skills.length];
		byte iterator = 0;
		
		for(String st : skills)
		{
			String[] info = st.split("-");
			
			if(info == null || info.length != 2)
				continue;

			int id = 0;
			int level = 0;
			
			try
			{
				id = Integer.parseInt(info[0]);
				level = Integer.parseInt(info[1]);
			}
			catch(Exception nfe)
			{
				// Incorrect syntax, dont add new skill
				_log.info(StringUtil.concat("> Couldnt parse " , st, " in etcitem skills!"));
				continue;
			}
			
			// If skill can exist, add it
			if(id > 0 && level > 0)
			{
				_skillHolder[iterator] = new SkillHolder(id, level);
				iterator++;
			}
		}
		
		_handler = set.getString("handler");
	}

	/**
	 * Returns the type of Etc Item
	 * @return L2EtcItemType
	 */
	@Override
	public L2EtcItemType getItemType()
	{
		return (L2EtcItemType)super._type;
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
	 * Returns skills linked to that EtcItem
	 * @return
	 */
	public SkillHolder[] getSkills()
	{
		return _skillHolder;
	}
	
	public String getHandlerName()
	{
		return _handler;
	}
}
