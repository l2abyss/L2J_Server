/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package net.sf.l2j.gameserver.templates;

import net.sf.l2j.gameserver.model.L2ItemInstance;

/**
 * This class contains L2ItemInstance<BR>
 * Use to sort L2ItemInstance of :
 * <LI>L2Armor</LI>
 * <LI>L2EtcItem</LI>
 * <LI>L2Weapon</LI> 
 * @version $Revision: 1.7.2.2.2.5 $ $Date: 2005/04/06 18:25:18 $
 */
public class L2WarehouseItem
{
    private L2Item _item;
    private int _object;
    private int _count;
    private int _owner;
    private int _enchant;
    private int _grade;
    private boolean _isAugmented;
    private int _augmentationId;
    private int _customType1;
    private int _customType2;
    private int _mana;
    private int _attackAttrElement;
    private int _attackAttrElementVal;
    private int _defAttrFire;
    private int _defAttrWater;
    private int _defAttrWind;
    private int _defAttrEarth;
    private int _defAttrHoly;
    private int _defAttrUnholy;

    public L2WarehouseItem(L2ItemInstance item)
    {
        _item = item.getItem();
        _object = item.getObjectId();
        _count = item.getCount();
        _owner = item.getOwnerId();
        _enchant = item.getEnchantLevel();
        _customType1 = item.getCustomType1();
        _customType2 = item.getCustomType2();
        _grade = item.getItem().getItemGrade();
        if (item.isAugmented())
        {
            _isAugmented = true;
            _augmentationId = item.getAugmentation().getAugmentationId();
        }
        else
            _isAugmented = false;
        _mana = item.getMana();
        _attackAttrElement = item.getAttackAttrElement();
        _attackAttrElementVal = item.getAttackAttrElementVal();
        _defAttrFire = item.getDefAttrFire();
        _defAttrWater = item.getDefAttrWater();
        _defAttrWind = item.getDefAttrWind();
        _defAttrEarth = item.getDefAttrEarth();
        _defAttrHoly = item.getDefAttrHoly();
        _defAttrUnholy = item.getDefAttrUnholy();
    }

    /**
    * Returns the item.
    * @return L2Item
    */
   public L2Item getItem()
   {
       return _item;
   }

   /**
    * Returns the unique objectId
    * @return int
    */
   public final int getObjectId()
   {
       return _object;
   }

    /**
     * Returns the owner
     * @return int
     */
    public final int getOwnerId()
    {
        return _owner;
    }

    /**
     * Returns the count
     * @return int
     */
    public final int getCount()
    {
        return _count;
    }

    /**
     * Returns the first type
     * @return int
     */
    public final int getType1()
    {
        return _item.getType1();
    }

    /**
     * Returns the second type
     * @return int
     */
    public final int getType2()
    {
        return _item.getType2();
    }

    /**
     * Returns the second type
     * @return int
     */
    @SuppressWarnings("unchecked")
    public final Enum getItemType()
    {
        return _item.getItemType();
    }

    /**
     * Returns the ItemId
     * @return int
     */
    public final int getItemId()
    {
        return _item.getItemId();
    }

    /**
     * Returns the part of body used with this item
     * @return int
     */
    public final int getBodyPart()
    {
        return _item.getBodyPart();
    }

    /**
     * Returns the enchant level
     * @return int
     */
    public final int getEnchantLevel()
    {
        return _enchant;
    }

    /**
     * Returns the item grade
     * @return int
     */
    public final int getItemGrade()
    {
        return _grade;
    }

    /**
     * Returns true if it is a weapon
     * @return boolean
     */
    public final boolean isWeapon()
    {
        return (_item instanceof L2Weapon);
    }

    /**
     * Returns true if it is an armor
     * @return boolean
     */
    public final boolean isArmor()
    {
        return (_item instanceof L2Armor);
    }

    /**
     * Returns true if it is an EtcItem
     * @return boolean
     */
    public final boolean isEtcItem()
    {
        return (_item instanceof L2EtcItem);
    }

    /**
     * Returns the name of the item
     * @return String
     */
    public String getItemName()
    {
        return _item.getName();
    }

    public boolean isAugmented()
    {
        return _isAugmented;
    }

    public int getAugmentationId()
    {
        return _augmentationId;
    }

    /**
     * Returns the name of the item
     * @return String
     * @deprecated beware to use getItemName() instead because getName() is final in L2Object and could not be overriden! Allover L2Object.getName() may return null!
     */
    public String getName()
    {
        return _item.getName();
    }

    public final int getCustomType1()
    {
        return _customType1;
    }
    public final int getCustomType2()
    {
        return _customType2;
    }
    public final int getMana()
    {
        return _mana;
    }
    public final int getAttackAttrElement()
    {
        return _attackAttrElement;
    }
    public final int getAttackAttrElementVal()
    {
        return _attackAttrElementVal;
    }
    public final int getDefAttrFire()
    {
        return _defAttrFire;
    }
    public final int getDefAttrWater()
    {
        return _defAttrWater;
    }
    public final int getDefAttrWind()
    {
        return _defAttrWind;
    }
    public final int getDefAttrEarth()
    {
        return _defAttrEarth;
    }
    public final int getDefAttrHoly()
    {
        return _defAttrHoly;
    }
    public final int getDefAttrUnholy()
    {
        return _defAttrUnholy;
    }

    /**
     * Returns the name of the item
     * @return String
     */
   public String toString()
   {
       return _item.toString();
   }
}