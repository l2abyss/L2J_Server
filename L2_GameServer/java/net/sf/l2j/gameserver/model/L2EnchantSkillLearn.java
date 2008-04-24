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

import java.util.List;

import javolution.util.FastTable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 * This class ...
 *
 * @version $Revision: 1.2.4.2 $ $Date: 2005/03/27 15:29:33 $
 */
@SuppressWarnings("unchecked")
public final class L2EnchantSkillLearn
{
    private final int _id;
    private final int _baseLvl;
    
    private List<EnchantSkillDetail>[] _enchantDetails = new List[0];

    public L2EnchantSkillLearn(int id, int baseLvl)
    {
        _id = id;
        _baseLvl = baseLvl;
    }

    /**
     * @return Returns the id.
     */
    public int getId()
    {
        return _id;
    }

    /**
     * @return Returns the minLevel.
     */
    public int getBaseLevel()
    {
        return _baseLvl;
    }
    
    @SuppressWarnings("unchecked")
    public void addEnchantDetail(EnchantSkillDetail esd)
    {
        int enchantType = L2EnchantSkillLearn.getEnchantType(esd.getLevel());
        
        if (enchantType < 0)
        {
            throw new IllegalArgumentException("Skill enchantments should have level higher then 100");
        }
        else
        {
            if (enchantType >= _enchantDetails.length)
            {
                List<EnchantSkillDetail>[] newArray = new List[enchantType+1];
                System.arraycopy(_enchantDetails, 0, newArray, 0, _enchantDetails.length);
                _enchantDetails = newArray;
                _enchantDetails[enchantType] = new FastTable<EnchantSkillDetail>();
            }
            int index = L2EnchantSkillLearn.getEnchantIndex(esd.getLevel());
            _enchantDetails[enchantType].add(index, esd);
        }
    }
    
    public List<EnchantSkillDetail>[] getEnchantRoutes()
    {
        return _enchantDetails;
    }
    
    public EnchantSkillDetail getEnchantSkillDetail(int level)
    {
        int enchantType = L2EnchantSkillLearn.getEnchantType(level);
        if (enchantType < 0 || enchantType >= _enchantDetails.length)
        {
            return null;
        }
        int index = L2EnchantSkillLearn.getEnchantIndex(level);
        if (index < 0 || index >= _enchantDetails[enchantType].size())
        {
            return null;
        }
        return _enchantDetails[enchantType].get(index);
    }
    
    public static int getEnchantIndex(int level)
    {
        return (level % 100) - 1;
    }
    
    public static int getEnchantType(int level)
    {
        return ((level - 1) / 100) - 1;
    }
    
    public static class EnchantSkillDetail
    {
        // not needed, just for easier debug
        private final String _name;

        private final int _level;
        private final int _spCost;
        private final int _minSkillLevel;
        private final int _exp;
        private final byte _rate76;
        private final byte _rate77;
        private final byte _rate78;
        
        public EnchantSkillDetail(int lvl, int minSkillLvl, String name, int cost, int exp, byte rate76, byte rate77, byte rate78)
        {
            _level = lvl;
            _minSkillLevel = minSkillLvl;
            _name = name.intern();
            _spCost = cost;
            _exp = exp;
            _rate76 = rate76;
            _rate77 = rate77;
            _rate78 = rate78;
        }
        
        /**
         * @return Returns the level.
         */
        public int getLevel()
        {
            return _level;
        }
        
        /**
         * @return Returns the minSkillLevel.
         */
        public int getMinSkillLevel()
        {
            return _minSkillLevel;
        }

        /**
         * @return Returns the name.
         */
        public String getName()
        {
            return _name;
        }

        /**
         * @return Returns the spCost.
         */
        public int getSpCost()
        {
            return _spCost;
        }
        public int getExp()
        {
            return _exp;
        }

        public byte getRate(L2PcInstance ply)
        {
            byte result;
            switch (ply.getLevel())
            {
                case 76:
                    result = _rate76;
                    break;
                case 77:
                    result = _rate77;
                    break;
                case 78:
                    result = _rate78;
                    break;
                default:
                    result = _rate78;
                break;
            }
            return result;
        }
    }
}