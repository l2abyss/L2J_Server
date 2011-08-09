/*
 * $Header: Util.java, 21/10/2005 23:17:40 luisantonioa Exp $
 *
 * $Author: luisantonioa $
 * $Date: 21/10/2005 23:17:40 $
 * $Revision: 1 $
 * $Log: Util.java,v $
 * Revision 1  21/10/2005 23:17:40  luisantonioa
 * Added copyright notice
 *
 *
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
package com.l2jserver.gameserver.util;

import java.io.File;
import java.util.Collection;

import com.l2jserver.Config;
import com.l2jserver.gameserver.ThreadPoolManager;
import com.l2jserver.gameserver.model.L2Object;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;

/**
 * General Utility functions related to Gameserver
 */
public final class Util
{
	public static void handleIllegalPlayerAction(L2PcInstance actor, String message, int punishment)
	{
		ThreadPoolManager.getInstance().scheduleGeneral(new IllegalPlayerAction(actor, message, punishment), 5000);
	}
	
	public static String getRelativePath(File base, File file)
	{
		return file.toURI().getPath().substring(base.toURI().getPath().length());
	}
	
	/**
	 * Return degree value of object 2 to the horizontal line with object 1
	 * being the origin
	 */
	public static double calculateAngleFrom(L2Object obj1, L2Object obj2)
	{
		return calculateAngleFrom(obj1.getX(), obj1.getY(), obj2.getX(), obj2.getY());
	}
	
	/**
	 * Return degree value of object 2 to the horizontal line with object 1
	 * being the origin
	 */
	public final static double calculateAngleFrom(int obj1X, int obj1Y, int obj2X, int obj2Y)
	{
		double angleTarget = Math.toDegrees(Math.atan2(obj2Y - obj1Y, obj2X - obj1X));
		if (angleTarget < 0)
			angleTarget = 360 + angleTarget;
		return angleTarget;
	}
	
	public final static double convertHeadingToDegree(int clientHeading)
	{
		double degree = clientHeading / 182.044444444;
		return degree;
	}
	
	public final static int convertDegreeToClientHeading(double degree)
	{
		if (degree < 0)
			degree = 360 + degree;
		return (int) (degree * 182.044444444);
	}
	
	public final static int calculateHeadingFrom(L2Object obj1, L2Object obj2)
	{
		return calculateHeadingFrom(obj1.getX(), obj1.getY(), obj2.getX(), obj2.getY());
	}
	
	public final static int calculateHeadingFrom(int obj1X, int obj1Y, int obj2X, int obj2Y)
	{
		double angleTarget = Math.toDegrees(Math.atan2(obj2Y - obj1Y, obj2X - obj1X));
		if (angleTarget < 0)
			angleTarget = 360 + angleTarget;
		return (int) (angleTarget * 182.044444444);
	}
	
	public final static int calculateHeadingFrom(double dx, double dy)
	{
		double angleTarget = Math.toDegrees(Math.atan2(dy, dx));
		if (angleTarget < 0)
			angleTarget = 360 + angleTarget;
		return (int) (angleTarget * 182.044444444);
	}
	
	/**
	 * @return the distance between the two coordinates in 2D plane
	 */
	public static double calculateDistance(int x1, int y1, int x2, int y2)
	{
		return calculateDistance(x1, y1, 0, x2, y2, 0, false);
	}
	
	/**
	 * @param includeZAxis - if true, includes also the Z axis in the calculation
	 * @return the distance between the two coordinates
	 */
	public static double calculateDistance(int x1, int y1, int z1, int x2, int y2, int z2, boolean includeZAxis)
	{
		double dx = (double) x1 - x2;
		double dy = (double) y1 - y2;
		
		if (includeZAxis)
		{
			double dz = z1 - z2;
			return Math.sqrt(dx * dx + dy * dy + dz * dz);
		}
		else
			return Math.sqrt(dx * dx + dy * dy);
	}
	
	/**
	 * @param includeZAxis - if true, includes also the Z axis in the calculation
	 * @return the distance between the two objects
	 */
	public static double calculateDistance(L2Object obj1, L2Object obj2, boolean includeZAxis)
	{
		if (obj1 == null || obj2 == null)
			return 1000000;
		
		return calculateDistance(obj1.getPosition().getX(), obj1.getPosition().getY(), obj1.getPosition().getZ(), obj2.getPosition().getX(), obj2.getPosition().getY(), obj2.getPosition().getZ(), includeZAxis);
	}
	
	/**
	 * (Based on ucfirst() function of PHP)
	 *
	 * @param str - the string whose first letter to capitalize
	 * @return a string with the first letter of the {@code str} capitalized
	 */
	public static String capitalizeFirst(String str)
	{
		str = str.trim();
		
		if (str.length() > 0 && Character.isLetter(str.charAt(0)))
			return str.substring(0, 1).toUpperCase() + str.substring(1);
		
		return str;
	}
	
	/**
	 * (Based on ucwords() function of PHP)
	 *
	 * @param str - the string to capitalize
	 * @return a string with the first letter of every word in {@code str} capitalized
	 */
	public static String capitalizeWords(String str)
	{
		char[] charArray = str.toCharArray();
		StringBuilder result = new StringBuilder();
		
		// Capitalize the first letter in the given string!
		charArray[0] = Character.toUpperCase(charArray[0]);
		
		for (int i = 0; i < charArray.length; i++)
		{
			if (Character.isWhitespace(charArray[i]))
				charArray[i + 1] = Character.toUpperCase(charArray[i + 1]);
			
			result.append(charArray[i]);
		}
		
		return result.toString();
	}
	
	/**
	 * @return {@code true} if the two objects are within specified range between each other, {@code false} otherwise
	 */
	public static boolean checkIfInRange(int range, L2Object obj1, L2Object obj2, boolean includeZAxis)
	{
		if (obj1 == null || obj2 == null)
			return false;
		if (obj1.getInstanceId() != obj2.getInstanceId())
			return false;
		if (range == -1)
			return true; // not limited
			
		int rad = 0;
		if (obj1 instanceof L2Character)
			rad += ((L2Character) obj1).getTemplate().collisionRadius;
		if (obj2 instanceof L2Character)
			rad += ((L2Character) obj2).getTemplate().collisionRadius;
		
		double dx = obj1.getX() - obj2.getX();
		double dy = obj1.getY() - obj2.getY();
		
		if (includeZAxis)
		{
			double dz = obj1.getZ() - obj2.getZ();
			double d = dx * dx + dy * dy + dz * dz;
			
			return d <= range * range + 2 * range * rad + rad * rad;
		}
		else
		{
			double d = dx * dx + dy * dy;
			
			return d <= range * range + 2 * range * rad + rad * rad;
		}
	}
	
	/**
	 *  Checks if object is within short (sqrt(int.max_value)) radius, not using collisionRadius.
	 *  Faster calculation than checkIfInRange if distance is short and collisionRadius isn't needed.
	 *  Not for long distance checks (potential teleports, far away castles etc).
	 * @param range - the maximum range between the two objects
	 *  @param includeZAxis - if true, check also Z axis (3-dimensional check), otherwise only 2D
	 * @return {@code true} if objects are within specified range between each other, {@code false} otherwise
	 */
	public static boolean checkIfInShortRadius(int radius, L2Object obj1, L2Object obj2, boolean includeZAxis)
	{
		if (obj1 == null || obj2 == null)
			return false;
		if (radius == -1)
			return true; // not limited
			
		int dx = obj1.getX() - obj2.getX();
		int dy = obj1.getY() - obj2.getY();
		
		if (includeZAxis)
		{
			int dz = obj1.getZ() - obj2.getZ();
			return dx * dx + dy * dy + dz * dz <= radius * radius;
		}
		else
			return dx * dx + dy * dy <= radius * radius;
	}
	
	/**
	 * @param str - the String to count
	 * @return the number of "words" in a given string.
	 */
	public static int countWords(String str)
	{
		return str.trim().split("\\s+").length;
	}
	
	/**
	 * (Based on implode() in PHP)
	 * @param strArray - an array of strings to concatenate
	 * @param strDelim - the delimiter to put between the strings
	 * @return a delimited string for a given array of string elements.
	 */
	public static String implodeString(String[] strArray, String strDelim)
	{
		StringBuilder result = new StringBuilder();
		
		for (String strValue : strArray)
		{
			result.append(strValue);
			result.append(strDelim);
		}
		
		return result.toString();
	}
	
	/**
	 * @param strCollection - a collection of strings to concatenate
	 * @param strDelim - the delimiter to put between the strings
	 * @see #implodeString(String[] strArray, String strDelim)
	 */
	public static String implodeString(Collection<String> strCollection, String strDelim)
	{
		return implodeString(strCollection.toArray(new String[strCollection.size()]), strDelim);
	}
	
	/**
	 * (Based on round() in PHP)
	 * @param number - the number to round
	 * @param numPlaces - how many digits after decimal point to leave intact
	 * @return the value of {@code number} rounded to specified number of digits after the decimal point.
	 */
	public static float roundTo(float number, int numPlaces)
	{
		if (numPlaces <= 1)
			return Math.round(number);
		
		float exponent = (float) Math.pow(10, numPlaces);
		
		return Math.round(number * exponent) / exponent;
	}
	
	/**
	 * @param text - the text to check
	 * @return {@code true} if {@code text} contains only numbers, {@code false} otherwise
	 */
	public static boolean isDigit(String text)
	{
		if (text == null)
			return false;
		return text.matches("[0-9]+");
	}
	
	/**
	 * @param text - the text to check
	 * @return {@code true} if {@code text} contains only letters and/or numbers, {@code false} otherwise
	 */
	public static boolean isAlphaNumeric(String text)
	{
		if (text == null || text.isEmpty())
			return false;
		for (char c : text.toCharArray())
			if (!Character.isLetterOrDigit(c))
				return false;
		return true;
	}
	
	/**
	 * Format the specified digit using the digit grouping symbol "," (comma).
	 * For example, 123456789 becomes 123,456,789.
	 * @param amount - the amount of adena
	 * @return the formatted adena amount
	 */
	public static String formatAdena(long amount)
	{
		String s = "";
		long rem = amount % 1000;
		s = Long.toString(rem);
		amount = (amount - rem) / 1000;
		while (amount > 0)
		{
			if (rem < 99)
				s = '0' + s;
			if (rem < 9)
				s = '0' + s;
			rem = amount % 1000;
			s = Long.toString(rem) + "," + s;
			amount = (amount - rem) / 1000;
		}
		return s;
	}
	
	/**
	 * @param array - the array to look into
	 * @param obj - the object to search for
	 * @return {@code true} if the {@code array} contains the {@code obj}, {@code false} otherwise
	 */
	public static <T> boolean contains(T[] array, T obj)
	{
		for (T element : array)
			if (element == obj)
				return true;
		return false;
	}
	
	/**
	 * @param array - the array to look into
	 * @param obj - the integer to search for
	 * @return {@code true} if the {@code array} contains the {@code obj}, {@code false} otherwise
	 */
	public static boolean contains(int[] array, int obj)
	{
		for (int element : array)
			if (element == obj)
				return true;
		return false;
	}

	public static File[] getDatapackFiles(String dirname, String extention)
	{
		File dir = new File(Config.DATAPACK_ROOT, "data/" + dirname);
		if (!dir.exists())
			return null;

		CustomFileNameFilter filter = new CustomFileNameFilter(extention);

		return dir.listFiles(filter);
	}
}
