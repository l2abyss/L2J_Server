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
package com.l2jserver.gameserver.model.drops;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Nos
 */
public enum DropListScope
{
	DEATH(DeathDropItem.class, GroupedDeathDropItem.class),
	CORPSE(CorpseDropItem.class, GroupedCorpseDropItem.class);
	
	private static final Logger _log = Logger.getLogger(DropListScope.class.getName());
	
	private final Class<? extends GeneralDropItem> _dropItemClass;
	private final Class<? extends GroupedGeneralDropItem> _groupedDropItemClass;
	
	private DropListScope(Class<? extends GeneralDropItem> dropItemClass, Class<? extends GroupedGeneralDropItem> groupedDropItemClass)
	{
		_dropItemClass = dropItemClass;
		_groupedDropItemClass = groupedDropItemClass;
	}
	
	public IDropItem newDropItem(int itemId, long min, long max, double chance)
	{
		final Constructor<? extends GeneralDropItem> constructor;
		try
		{
			constructor = _dropItemClass.getConstructor(int.class, long.class, long.class, double.class);
		}
		catch (NoSuchMethodException | SecurityException e)
		{
			_log.log(Level.SEVERE, "Constructor(int, long, long, double) not found for " + _dropItemClass.getSimpleName(), e);
			return null;
		}
		
		try
		{
			return constructor.newInstance(itemId, min, max, chance);
		}
		catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
		{
			_log.log(Level.SEVERE, "", e);
			return null;
		}
	}
	
	public GroupedGeneralDropItem newGroupedDropItem(double chance)
	{
		final Constructor<? extends GroupedGeneralDropItem> constructor;
		try
		{
			constructor = _groupedDropItemClass.getConstructor(double.class);
		}
		catch (NoSuchMethodException | SecurityException e)
		{
			_log.log(Level.SEVERE, "Constructor(double) not found for " + _groupedDropItemClass.getSimpleName(), e);
			return null;
		}
		
		try
		{
			return constructor.newInstance(chance);
		}
		catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
		{
			_log.log(Level.SEVERE, "", e);
			return null;
		}
	}
}
