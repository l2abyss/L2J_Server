/*
 * $Header: Util.java, 14-Jul-2005 03:27:51 luisantonioa Exp $
 * 
 * $Author: luisantonioa $ $Date: 14-Jul-2005 03:27:51 $ $Revision: 1 $ $Log:
 * Util.java,v $ Revision 1 14-Jul-2005 03:27:51 luisantonioa Added copyright
 * notice
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
package com.l2jserver.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

/**
 * This class ...
 * 
 * @version $Revision: 1.2 $ $Date: 2004/06/27 08:12:59 $
 */

public class Util
{
	/**
	 * Checks if a host name is internal
	 * 
	 * @param host
	 * 	the host name to check
	 * @return
	 * 	true: host name is internal<br>
	 * 	false: host name is external
	 */
	public static boolean isInternalHostname(String host)
	{
		try
		{
			InetAddress addr = InetAddress.getByName(host);
			return addr.isSiteLocalAddress() || addr.isLoopbackAddress();
		}
		catch (UnknownHostException e)
		{
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Method to generate the hexadecimal representation of a byte array.<br>
	 * 16 bytes per row, while ascii chars or "." is shown at the end of the line.
	 * 
	 * @param data
	 * 	the byte array to be represented in hexadecimal representation
	 * @param len
	 * 	the number of bytes to represent in hexadecimal representation
	 * @return
	 * 	byte array represented in hexadecimal format
	 */
	public static String printData(byte[] data, int len)
	{
		final StringBuilder result = new StringBuilder(len * 4);
		
		int counter = 0;
		
		for (int i = 0; i < len; i++)
		{
			if (counter % 16 == 0)
			{
				result.append(fillHex(i, 4) + ": ");
			}
			
			result.append(fillHex(data[i] & 0xff, 2) + " ");
			counter++;
			if (counter == 16)
			{
				result.append("   ");
				
				int charpoint = i - 15;
				for (int a = 0; a < 16; a++)
				{
					int t1 = 0xFF & data[charpoint++];
					if (t1 > 0x1f && t1 < 0x80)
					{
						result.append((char) t1);
					}
					else
					{
						result.append('.');
					}
				}
				
				result.append("\n");
				counter = 0;
			}
		}
		
		int rest = data.length % 16;
		if (rest > 0)
		{
			for (int i = 0; i < 17 - rest; i++)
			{
				result.append("   ");
			}
			
			int charpoint = data.length - rest;
			for (int a = 0; a < rest; a++)
			{
				int t1 = 0xFF & data[charpoint++];
				if (t1 > 0x1f && t1 < 0x80)
				{
					result.append((char) t1);
				}
				else
				{
					result.append('.');
				}
			}
			
			result.append("\n");
		}
		
		return result.toString();
	}
	
	private static String fillHex(int data, int digits)
	{
		String number = Integer.toHexString(data);
		
		for (int i = number.length(); i < digits; i++)
		{
			number = "0" + number;
		}
		
		return number;
	}
	
	/**
	 * This call is equivalent to Util.printData(data, data.length)
	 * @see Util#printData(byte[],int)
	 * 
	 * @param data
	 * 	data to represent in hexadecimal
	 * @return
	 * 	byte array represented in hexadecimal format
	 */
	public static String printData(byte[] data)
	{
		return printData(data, data.length);
	}

	/**
	 * Method to represent the remaining bytes of a ByteBuffer as hexadecimal
	 * 
	 * @param buf
	 * 	ByteBuffer to represent the remaining bytes of as hexadecimal
	 * @return
	 * 	hexadecimal representation of remaining bytes of the ByteBuffer
	 */
	public static String printData(ByteBuffer buf)
	{
		byte[] data = new byte[buf.remaining()];
		buf.get(data);
		String hex = Util.printData(data, data.length);
		buf.position(buf.position() - data.length);
		return hex;
	}

	/**
	 * Method to generate a random sequence of bytes returned as byte array
	 * 
	 * @param size
	 * 	number of random bytes to generate
	 * @return
	 * 	byte array with sequence of random bytes
	 */
	public static byte[] generateHex(int size)
	{
		byte[] array = new byte[size];
		Rnd.nextBytes(array);
		return array;
	}

	/**
	 * Method to get the stack trace of a Throwable into a String
	 * 
	 * @param t
	 * 	Throwable to get the stacktrace from
	 * @return
	 * 	stack trace from Throwable as String
	 */
	public static String getStackTrace(Throwable t)
	{
		StringWriter sw = new StringWriter();
		t.printStackTrace(new PrintWriter(sw));
		return sw.toString();
	}
}
