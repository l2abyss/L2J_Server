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
public abstract class AbstractBufferedByteReader
{
	protected final byte[] buf;
	protected int rIndex;
	
	protected AbstractBufferedByteReader(final byte[] data)
	{
		buf = data;
	}
	
	public abstract int readC();
	public abstract int readH();
	public abstract int readD();
	public abstract double readF();
	public abstract long readQ();
	public abstract String readS();
	public abstract byte[] readB(final int length);
}