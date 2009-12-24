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

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

/**
 * @author Forsaiken
 */
public final class BufferedByteWriter extends AbstractBufferedByteWriter
{
	public BufferedByteWriter()
	{
		super(32);
	}
	
	public BufferedByteWriter(final int size)
	{
		super(size);
	}
	
	@Override
	public final void writeC(final int value)
	{
		expand(1);
		buf[wIndex++] = (byte)(value & 0x000000FF);
	}
	
	@Override
	public final void writeH(final int value)
	{
		expand(2);
		buf[wIndex++] = (byte)(value & 0x000000FF);
		buf[wIndex++] = (byte)(value >> 8 & 0x000000FF);
	}
	
	@Override
	public final void writeD(final int value)
	{
		expand(4);
		buf[wIndex++] = (byte)(value & 0x000000FF);
		buf[wIndex++] = (byte)(value >> 8 & 0x000000FF);
		buf[wIndex++] = (byte)(value >> 16 & 0x000000FF);
		buf[wIndex++] = (byte)(value >> 24 & 0x000000FF);
	}
	
	@Override
	public final void writeF(final double value)
	{
		writeQ(Double.doubleToRawLongBits(value));
	}
	
	@Override
	public final void writeQ(final long value)
	{
		expand(8);
		buf[wIndex++] = (byte)(value & 0x000000FF);
		buf[wIndex++] = (byte)(value >> 8 & 0x000000FF);
		buf[wIndex++] = (byte)(value >> 16 & 0x000000FF);
		buf[wIndex++] = (byte)(value >> 24 & 0x000000FF);
		buf[wIndex++] = (byte)(value >> 32 & 0x000000FF);
		buf[wIndex++] = (byte)(value >> 40 & 0x000000FF);
		buf[wIndex++] = (byte)(value >> 48 & 0x000000FF);
		buf[wIndex++] = (byte)(value >> 56 & 0x000000FF);
	}
	
	@Override
	public final void writeS(final String text)
	{
		if (text != null)
		{
			try
			{
				final byte[] data = text.getBytes("UTF-16LE");
				
				expand(data.length + 2);
				
				System.arraycopy(data, 0, buf, wIndex, data.length);
				
				wIndex += data.length + 2;
			}
			catch (UnsupportedEncodingException e)
			{
				e.printStackTrace();
			}
		}
		else
		{
			expand(2);
			wIndex += 2;
		}
	}
	
	@Override
	public final void writeB(final byte[] data)
	{
		try
		{
			expand(data.length);
			System.arraycopy(data, 0, buf, wIndex, data.length);
			wIndex += data.length;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private synchronized void expand(final int expand)
	{
		final int newSize = wIndex + expand;
		if (newSize > buf.length)
			buf = Arrays.copyOf(buf, Math.max(buf.length << 1, newSize));
	}
}