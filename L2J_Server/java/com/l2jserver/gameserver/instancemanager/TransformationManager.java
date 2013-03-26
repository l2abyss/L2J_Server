/*
 * Copyright (C) 2004-2013 L2J Server
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
package com.l2jserver.gameserver.instancemanager;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.l2jserver.gameserver.model.L2Transformation;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author KenM
 */
public class TransformationManager
{
	private static final Logger _log = Logger.getLogger(TransformationManager.class.getName());
	
	private final Map<Integer, L2Transformation> _transformations = new HashMap<>();
	
	protected TransformationManager()
	{
	}
	
	public void report()
	{
		_log.info(getClass().getSimpleName() + ": Loaded: " + _transformations.size() + " transformations.");
	}
	
	public boolean transformPlayer(int id, L2PcInstance player)
	{
		L2Transformation template = getTransformationById(id);
		if (template != null)
		{
			L2Transformation trans = template.createTransformationForPlayer(player);
			trans.start();
			return true;
		}
		return false;
	}
	
	public L2Transformation getTransformationById(int id)
	{
		return _transformations.get(id);
	}
	
	public L2Transformation registerTransformation(L2Transformation transformation)
	{
		return _transformations.put(transformation.getId(), transformation);
	}
	
	public static TransformationManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final TransformationManager _instance = new TransformationManager();
	}
}
