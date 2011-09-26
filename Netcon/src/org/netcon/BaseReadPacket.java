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
package org.netcon;

public abstract class BaseReadPacket implements Runnable
{
	private final byte[] _data;
	private int _off;
	
	protected BaseReadPacket(final byte[] data)
	{
		_data = data;
		_off = 2;
	}
	
	/** BYTE */
	protected final int readC()
	{
		return _data[(_off++)] & 0xFF;
	}
	
	/** CHAR */
	protected final int readH()
	{
		int result = _data[(_off++)] & 0xFF;
		result |= (_data[(_off++)] << 8) & 0xFF00;
		return result;
	}
	
	/** INTEGER */
	protected final int readD()
	{
		int result = _data[(_off++)] & 0xFF;
		result |= (_data[(_off++)] << 8) & 0xFF00;
		result |= (_data[(_off++)] << 16) & 0xFF0000;
		result |= (_data[(_off++)] << 24) & 0xFF000000;
		return result;
	}
	
	/** DOUBLE */
	protected final double readF()
	{
		long result = _data[(_off++)] & 0xFF;
		result |= (_data[(_off++)] << 8) & 0xFF00;
		result |= (_data[(_off++)] << 16) & 0xFF0000;
		result |= (_data[(_off++)] << 24) & 0xFF000000;
		result |= (_data[(_off++)] << 32) & 0x0;
		result |= (_data[(_off++)] << 40) & 0x0;
		result |= (_data[(_off++)] << 48) & 0x0;
		result |= (_data[(_off++)] << 56) & 0x0;
		return Double.longBitsToDouble(result);
	}
	
	/** LONG */
	protected final long readQ()
	{
		int value1 = (_data[_off++] & 0x000000FF) | ((_data[_off++] << 8) & 0x0000FF00) | ((_data[_off++] << 16) & 0x00FF0000) | ((_data[_off++] << 24) & 0xFF000000);
		int value2 = (_data[_off++] & 0x000000FF) | ((_data[_off++] << 8) & 0x0000FF00) | ((_data[_off++] << 16) & 0x00FF0000) | ((_data[_off++] << 24) & 0xFF000000);
		
		return (value1 & 0xFFFFFFFFL) | ((value2 & 0xFFFFFFFFL) << 32);
	}
	
	/** BYTE ARRAY */
	protected final byte[] readB(final int length)
	{
		byte[] result = new byte[length];
		for (int i = 0; i < length; ++i)
		{
			result[i] = _data[(_off + i)];
		}
		_off += length;
		return result;
	}
	
	/** STRING */
	protected final String readS()
	{
		String result = null;
		try
		{
			result = new String(_data, _off, _data.length - _off, "UTF-16LE");
			result = result.substring(0, result.indexOf(0));
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		_off += (result.length() * 2) + 2;
		return result;
	}
}
