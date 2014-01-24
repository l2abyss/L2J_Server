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
package com.l2jserver.util;

import java.util.logging.Level;
import java.util.logging.Logger;

public class StackTrace
{
	private static final Logger _log = Logger.getLogger(StackTrace.class.getName());
	
	public static boolean displayStackTraceInformation(Throwable ex)
	{
		return displayStackTraceInformation(ex, false);
	}
	
	public static boolean displayStackTraceInformation(Throwable ex, boolean displayAll)
	{
		if (ex == null)
		{
			return false;
		}
		
		_log.log(Level.INFO, "", ex);
		
		if (!displayAll)
		{
			return true;
		}
		
		StackTraceElement[] stackElements = ex.getStackTrace();
		
		_log.log(Level.INFO, "The " + stackElements.length + " element" + ((stackElements.length == 1) ? "" : "s") + " of the stack trace:\n");
		
		for (StackTraceElement stackElement : stackElements)
		{
			_log.log(Level.INFO, "File name: " + stackElement.getFileName());
			_log.log(Level.INFO, "Line number: " + stackElement.getLineNumber());
			
			String className = stackElement.getClassName();
			String packageName = extractPackageName(className);
			String simpleClassName = extractSimpleClassName(className);
			
			_log.log(Level.INFO, "Package name: " + ("".equals(packageName) ? "[default package]" : packageName));
			_log.log(Level.INFO, "Full class name: " + className);
			_log.log(Level.INFO, "Simple class name: " + simpleClassName);
			_log.log(Level.INFO, "Unmunged class name: " + unmungeSimpleClassName(simpleClassName));
			_log.log(Level.INFO, "Direct class name: " + extractDirectClassName(simpleClassName));
			
			_log.log(Level.INFO, "Method name: " + stackElement.getMethodName());
			_log.log(Level.INFO, "Native method?: " + stackElement.isNativeMethod());
			
			_log.log(Level.INFO, "toString(): " + stackElement.toString());
			_log.log(Level.INFO, "");
		}
		_log.log(Level.INFO, "");
		
		return true;
	}
	
	private static String extractPackageName(String fullClassName)
	{
		if ((null == fullClassName) || fullClassName.isEmpty())
		{
			return "";
		}
		final int lastDot = fullClassName.lastIndexOf('.');
		if (0 >= lastDot)
		{
			return "";
		}
		return fullClassName.substring(0, lastDot);
	}
	
	private static String extractSimpleClassName(String fullClassName)
	{
		if ((null == fullClassName) || fullClassName.isEmpty())
		{
			return "";
		}
		
		int lastDot = fullClassName.lastIndexOf('.');
		if (0 > lastDot)
		{
			return fullClassName;
		}
		return fullClassName.substring(++lastDot);
	}
	
	private static String extractDirectClassName(String simpleClassName)
	{
		if ((null == simpleClassName) || simpleClassName.isEmpty())
		{
			return "";
		}
		
		int lastSign = simpleClassName.lastIndexOf('$');
		if (0 > lastSign)
		{
			return simpleClassName;
		}
		return simpleClassName.substring(++lastSign);
	}
	
	private static String unmungeSimpleClassName(String simpleClassName)
	{
		if ((null == simpleClassName) || simpleClassName.isEmpty())
		{
			return "";
		}
		return simpleClassName.replace('$', '.');
	}
}