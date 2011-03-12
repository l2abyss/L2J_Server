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

import java.util.Arrays;

/**
 * @author Forsaiken
 */
public abstract class AbstractBufferedByteWriter
{
	protected byte[] buf;
	protected int wIndex;
	
	protected AbstractBufferedByteWriter(final int size)
	{
		buf = new byte[size];
	}
	
	public abstract void writeC(final int value);
	public abstract void writeH(final int value);
	public abstract void writeD(final int value);
	public abstract void writeF(final double value);
	public abstract void writeQ(final long value);
	public abstract void writeS(final String text);
	public abstract void writeB(final byte[] data);
	
	public final int length()
	{
		return wIndex;
	}
	
	public final byte[] toByteArray()
	{
		return Arrays.copyOf(buf, wIndex);
	}
}