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
package com.l2jserver.communityserver.util.buffer;

/**
 * @author Forsaiken
 */
public final class SynchronizedBufferedByteReader extends AbstractBufferedByteReader
{	
	public SynchronizedBufferedByteReader(final byte[] data)
	{
		super(data);
	}
	
	@Override
	public final int readC()
	{
		int result;
		
		synchronized(buf)
		{
			result = buf[rIndex++] & 0x000000FF;
		}
		
		return result;
	}
	
	@Override
	public final int readH()
	{
		int result;
		
		synchronized(buf)
		{
			result = buf[rIndex++] & 0x000000FF;
			result |= buf[rIndex++] << 8 & 0x0000FF00;
		}
		
		return result;
	}
	
	@Override
	public final int readD()
	{
		int result;
		
		synchronized(buf)
		{
			result = buf[rIndex++] & 0x000000FF;
			result |= buf[rIndex++] << 8 & 0x0000FF00;
			result |= buf[rIndex++] << 16 & 0x00FF0000;
			result |= buf[rIndex++] << 24 & 0xFF000000;
		}
		
		return result;
	}
	
	@Override
	public final double readF()
	{
		long result;
		
		synchronized(buf)
		{
			result = buf[rIndex++] & 0x00000000000000FF;
			result |= buf[rIndex++] << 8 & 0x000000000000FF00;
			result |= buf[rIndex++] << 16 & 0x0000000000FF0000;
			result |= buf[rIndex++] << 24 & 0x00000000FF000000;
			result |= buf[rIndex++] << 32 & 0x000000FF00000000L;
			result |= buf[rIndex++] << 40 & 0x0000FF0000000000L;
			result |= buf[rIndex++] << 48 & 0x00FF000000000000L;
			result |= buf[rIndex++] << 56 & 0xFF00000000000000L;
		}
		
		return Double.longBitsToDouble(result);
	}
	
	@Override
	public final long readQ()
	{
		int value1, value2;
		
		synchronized(buf)
		{
			value1 = (buf[rIndex++] & 0x000000FF) | (buf[rIndex++] << 8 & 0x0000FF00) | (buf[rIndex++] << 16 & 0x00FF0000) | (buf[rIndex++] << 24 & 0xFF000000);
			value2 = (buf[rIndex++] & 0x000000FF) | (buf[rIndex++] << 8 & 0x0000FF00) | (buf[rIndex++] << 16 & 0x00FF0000) | (buf[rIndex++] << 24 & 0xFF000000);
		}
		
		return (value1 & 0xFFFFFFFFL) | (value2 & 0xFFFFFFFFL) << 32;
	}
	
	@Override
	public final byte[] readB(final int length)
	{
		final byte[] result = new byte[length];
		
		synchronized(buf)
		{
			for (int i = 0; i < length; i++)
			{
				result[i] = buf[rIndex++];
			}
		}
		
		return result;
	}
	
	@Override
	public final String readS()
	{
		String result = null;
		
		try
		{
			synchronized(buf)
			{
				result = new String(buf, rIndex, buf.length - rIndex, "UTF-16LE");
				result = result.substring(0, result.indexOf(0x00));
				rIndex += result.length() * 2 + 2;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		return result;
	}
}