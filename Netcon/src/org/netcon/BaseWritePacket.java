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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public abstract class BaseWritePacket
{
	private final ByteArrayOutputStream _bao;
	
	protected BaseWritePacket()
	{
		_bao = new ByteArrayOutputStream();
	}
	
	protected final void writeC(final int value)
	{
		_bao.write(value & 0xFF);
	}
	
	protected final void writeH(final int value)
	{
		_bao.write(value & 0xFF);
		_bao.write((value >> 8) & 0xFF);
	}
	
	protected final void writeD(final int value)
	{
		_bao.write(value & 0xFF);
		_bao.write((value >> 8) & 0xFF);
		_bao.write((value >> 16) & 0xFF);
		_bao.write((value >> 24) & 0xFF);
	}
	
	protected final void writeF(final double value)
	{
		writeQ(Double.doubleToRawLongBits(value));
	}
	
	protected final void writeQ(final long value)
	{
		_bao.write((byte) (value & 0xFF));
		_bao.write((byte) ((value >> 8) & 0xFF));
		_bao.write((byte) ((value >> 16) & 0xFF));
		_bao.write((byte) ((value >> 24) & 0xFF));
		_bao.write((byte) ((value >> 32) & 0xFF));
		_bao.write((byte) ((value >> 40) & 0xFF));
		_bao.write((byte) ((value >> 48) & 0xFF));
		_bao.write((byte) ((value >> 56) & 0xFF));
	}
	
	protected final void writeS(final String text)
	{
		try
		{
			if (text != null)
			{
				_bao.write(text.getBytes("UTF-16LE"));
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		_bao.write(0);
		_bao.write(0);
	}
	
	protected final void writeB(final byte[] array)
	{
		try
		{
			_bao.write(array);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public final byte[] getContent() throws IOException
	{
		writeD(0x00);
		
		int padding = _bao.size() % 8;
		if (padding != 0)
		{
			for (int i = padding; i < 8; i++)
			{
				writeC(0x00);
			}
		}
		
		return _bao.toByteArray();
	}
}
