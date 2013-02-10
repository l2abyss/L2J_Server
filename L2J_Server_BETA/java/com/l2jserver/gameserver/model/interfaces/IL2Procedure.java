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
package com.l2jserver.gameserver.model.interfaces;

/**
 * Interface for procedures with one Object parameter.
 * @author Battlecruiser
 * @param <T> the type of object on which the procedure will be executed
 */
public interface IL2Procedure<T>
{
	/**
	 * Executes this procedure. A false return value indicates that the application executing this procedure should not invoke this procedure again.
	 * @param arg the object on which the procedure will be executed
	 * @return {@code true} if additional invocations of the procedure are allowed.
	 */
	public boolean execute(T arg);
}
