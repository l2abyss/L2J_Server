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
package com.l2jserver.communityserver.network.netcon;

import java.io.IOException;

import com.l2jserver.communityserver.util.buffer.AbstractBufferedByteWriter;
import com.l2jserver.communityserver.util.buffer.BufferedByteWriter;

public abstract class BaseWritePacket
{
	private final AbstractBufferedByteWriter _buf;
	
	protected BaseWritePacket()
	{
		_buf = new BufferedByteWriter();
	}
	
	protected final void writeC(final int value)
	{
		_buf.writeC(value);
	}
	
	protected final void writeH(final int value)
	{
		_buf.writeH(value);
	}
	
	protected final void writeD(final int value)
	{
		_buf.writeD(value);
	}
	
	protected final void writeF(final double value)
	{
		_buf.writeF(value);
	}
	
	protected final void writeQ(final long value)
	{
		_buf.writeQ(value);
	}
	
	protected final void writeS(final String text)
	{
		_buf.writeS(text);
	}
	
	protected final void writeB(final byte[] array)
	{
		_buf.writeB(array);
	}
	
	public final byte[] getContent() throws IOException
	{
		writeD(0x00);
		
		int padding = _buf.length() % 8;
		if (padding != 0)
		{
			for (int i = padding; i < 8; i++)
			{
				writeC(0x00);
			}
		}
		
		return _buf.toByteArray();
	}
}