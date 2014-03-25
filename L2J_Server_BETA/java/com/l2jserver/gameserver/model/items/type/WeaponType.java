/*
 * Copyright (C) 2004-2014 L2J Server
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
package com.l2jserver.gameserver.model.items.type;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2jserver.gameserver.model.stats.TraitType;

/**
 * @author mkizub <BR>
 *         Description of Weapon Type
 */
public enum WeaponType implements ItemType
{
	SWORD("Sword", TraitType.SWORD),
	BLUNT("Blunt", TraitType.BLUNT),
	DAGGER("Dagger", TraitType.DAGGER),
	BOW("Bow", TraitType.BOW),
	POLE("Pole", TraitType.POLE),
	NONE("None", TraitType.NONE),
	DUAL("Dual Sword", TraitType.DUAL),
	ETC("Etc", TraitType.ETC),
	FIST("Fist", TraitType.FIST),
	DUALFIST("Dual Fist", TraitType.DUALFIST),
	FISHINGROD("Rod", TraitType.NONE),
	RAPIER("Rapier", TraitType.RAPIER),
	ANCIENTSWORD("Ancient", TraitType.ANCIENTSWORD),
	CROSSBOW("Crossbow", TraitType.CROSSBOW),
	FLAG("Flag", TraitType.NONE),
	OWNTHING("Ownthing", TraitType.NONE),
	DUALDAGGER("Dual Dagger", TraitType.DUALDAGGER);
	
	private static final Logger _log = Logger.getLogger(WeaponType.class.getName());
	private final int _mask;
	private final String _name;
	private final TraitType _traitType;
	
	/**
	 * Constructor of the WeaponType.
	 * @param name : String designating the name of the WeaponType
	 * @param traitType
	 */
	private WeaponType(String name, TraitType traitType)
	{
		_mask = 1 << ordinal();
		_name = name;
		_traitType = traitType;
	}
	
	/**
	 * @return the ID of the item after applying the mask.
	 */
	@Override
	public int mask()
	{
		return _mask;
	}
	
	/**
	 * @return the name of the WeaponType
	 */
	@Override
	public String getName()
	{
		return _name;
	}
	
	/**
	 * @return L2TraitType the type of the WeaponType
	 */
	public TraitType getTraitType()
	{
		return _traitType;
	}
	
	public static WeaponType findByName(String name)
	{
		if (name.equalsIgnoreCase("DUAL"))
		{
			name = "Dual Sword";
		}
		else if (name.equalsIgnoreCase("DUALFIST"))
		{
			name = "Dual Fist";
		}
		for (WeaponType type : values())
		{
			if (type.getName().equalsIgnoreCase(name))
			{
				return type;
			}
		}
		_log.log(Level.WARNING, WeaponType.class.getSimpleName() + ": Requested unexistent enum member: " + name, new IllegalStateException());
		return FIST;
	}
}
