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

import com.l2jserver.communityserver.util.buffer.AbstractBufferedByteReader;
import com.l2jserver.communityserver.util.buffer.BufferedByteReader;

public abstract class BaseReadPacket implements Runnable
{
	private final AbstractBufferedByteReader _buf;
	
	protected BaseReadPacket(final byte[] data)
	{
		_buf = new BufferedByteReader(data);
		_buf.readH();
	}
	
	/** BYTE */
	protected final int readC()
	{
		return _buf.readC();
	}
	
	/** CHAR */
	protected final int readH()
	{
		return _buf.readH();
	}
	
	/** INTEGER */
	protected final int readD()
	{
		return _buf.readD();
	}
	
	/** DOUBLE */
	protected final double readF()
	{
		return _buf.readF();
	}
	
	/** LONG */
	protected final long readQ()
	{
		return _buf.readQ();
	}
	
	/** BYTE ARRAY */
	protected final byte[] readB(final int length)
	{
		return _buf.readB(length);
	}
	
	/** STRING */
	protected final String readS()
	{
		return _buf.readS();
	}
}