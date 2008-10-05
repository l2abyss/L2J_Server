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

public final class Elementals
{
	public final static byte NONE = -1;
	public final static byte FIRE = 0;
	public final static byte WATER = 1;
	public final static byte WIND = 2;
	public final static byte EARTH = 3;
	public final static byte HOLY = 4;
	public final static byte DARK = 5;

	public final static int ENCHANT_CHANCE = 50;

	public final static int FIRST_WEAPON_BONUS = 20;
	public final static int NEXT_WEAPON_BONUS = 5;
	public final static int ARMOR_BONUS = 3;

	public final static int[] WEAPON_VALUES =
	{
		0,   // Level 1
		25,  // Level 2
		75,  // Level 3
		150, // Level 4
		175, // Level 5
		225, // Level 6
		300, // Level 7
		325, // Level 8
		375, // Level 9
		450  // Level 10
	};

	public final static int[] ARMOR_VALUES =
	{
		0,  // Level 1
		6,  // Level 2
		15, // Level 3
		30, // Level 4
		36, // Level 5
		45, // Level 6
		60, // Level 7
		66, // Level 8
		75, // Level 9
		90  // Level 10
	};

	public final static int[] STONES = 
	{
		9546,
		9547,
		9549,
		9548,
		9551,
		9550
	};

	public final static int[] CRYSTALS =
	{
		9552,
		9553,
		9555,
		9554,
		9557,
		9556
	};

	public final static int[] JEWELS =
	{
		9558,
		9559,
		9561,
		9560,
		9563,
		9562
	};

	public final static int[] ENERGIES = 
	{
		9564,
		9565,
		9567,
		9566,
		9569,
		9568
	};

	private byte _element = NONE;
	private int _value = 0;

	public byte getElement()
	{
		return _element;
	}

	public void setElement(byte type)
	{
		_element = type;
	}

	public int getValue()
	{
		return _value;
	}

	public void setValue(int val)
	{
		_value = val;
	}

	public static String getElementName(byte element)
	{
		switch(element)
		{
			case FIRE:
				return "Fire";
			case WATER:
				return "Water";
			case WIND:
				return "Wind";
			case EARTH:
				return "Earth";
			case DARK:
				return "Dark";
			case HOLY:
				return "Holy";
		}
		return "None";
	}

	public static byte getElementId(String name)
	{
		String tmp = name.toLowerCase();
		if (tmp.equals("fire"))
			return FIRE;
		if (tmp.equals("water"))
			return WATER;
		if (tmp.equals("wind"))
			return WIND;
		if (tmp.equals("earth"))
			return EARTH;
		if (tmp.equals("dark"))
			return DARK;
		if (tmp.equals("holy"))
			return HOLY;
		return NONE;
	}

	public static byte getOppositeElement(byte element)
	{
		return (byte)((element % 2 == 0) ? (element + 1) : (element - 1));
	}

	@Override
	public String toString()
	{
		return getElementName(_element) + " +" + _value;
	}

	public Elementals(byte type, int value)
	{
		_element = type;
		_value = value;
	}
}
