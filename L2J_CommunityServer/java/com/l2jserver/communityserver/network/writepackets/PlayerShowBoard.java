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
package com.l2jserver.communityserver.network.writepackets;

import java.io.UnsupportedEncodingException;

import javolution.util.FastList;
import com.l2jserver.communityserver.network.netcon.BaseWritePacket;

public final class PlayerShowBoard extends BaseWritePacket
{
	private static final byte[] DEC_HEAD =
	{
		123,	// C 0x7B
		1,		// C 0x01
		98,0,121,0,112,0,97,0,115,0,115,0,32,0,95,0,98,0,98,0,115,0,104,0,111,0,109,0,101,0,0,0,						// S "bypass _bbshome"
		98,0,121,0,112,0,97,0,115,0,115,0,32,0,95,0,98,0,98,0,115,0,103,0,101,0,116,0,102,0,97,0,118,0,0,0,				// S "bypass _bbsgetfav"
		98,0,121,0,112,0,97,0,115,0,115,0,32,0,95,0,98,0,98,0,115,0,108,0,111,0,99,0,0,0,								// S "bypass _bbsloc"
		98,0,121,0,112,0,97,0,115,0,115,0,32,0,95,0,98,0,98,0,115,0,99,0,108,0,97,0,110,0,0,0,							// S "bypass _bbsclan"
		98,0,121,0,112,0,97,0,115,0,115,0,32,0,95,0,98,0,98,0,115,0,109,0,101,0,109,0,111,0,0,0,						// S "bypass _bbsmemo"
		98,0,121,0,112,0,97,0,115,0,115,0,32,0,95,0,98,0,98,0,115,0,109,0,97,0,105,0,108,0,0,0,							// S "bypass _bbsmail"
		98,0,121,0,112,0,97,0,115,0,115,0,32,0,95,0,98,0,98,0,115,0,102,0,114,0,105,0,101,0,110,0,100,0,115,0,0,0,		// S "bypass _bbsfriends"
		98,0,121,0,112,0,97,0,115,0,115,0,32,0,95,0,98,0,98,0,115,0,95,0,97,0,100,0,100,0,95,0,102,0,97,0,118,0,0,0,	// S "bypass _bbs_add_fav"
	};
	
	private static final byte[][] DEC_10X =
	{
		{49,0,48,0,49,0,8,0,0,0,0,0},	// "101"
		{49,0,48,0,50,0,8,0,0,0,0,0},	// "102"
		{49,0,48,0,51,0,8,0,0,0,0,0},	// "103"
		{49,0,48,0,52,0,8,0,0,0,0,0}	// "104"
	};
	
	private PlayerShowBoard(final int playerObjId)
	{
		writeC(0x02);
		writeC(0x00);
		writeD(playerObjId);
	}
	
	/**
	 * ID: 10X
	 * @param playerObjId
	 * @param html
	 * @param id (101 = 0, 102 = 1, 103 = 2, 104 = 3)
	 */
	public PlayerShowBoard(final int playerObjId, final String html, final byte id)
	{
		this(playerObjId);
		try
		{
			final byte[] data = getBytes10X(html, id);
			writeD(DEC_HEAD.length + data.length); // write DEC_HEAD length + html length
			writeB(DEC_HEAD); // write head
			writeB(data); // write html
		}
		catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * ID: 1001
	 * @param playerObjId
	 * @param html
	 */
	public PlayerShowBoard(final int playerObjId, final String html)
	{
		this(playerObjId);
		try
		{
			final byte[] data = getBytes1001(html);
			writeD(DEC_HEAD.length + data.length); // write DEC_HEAD length + html length
			writeB(DEC_HEAD); // write head
			writeB(data); // write html
		}
		catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * ID: 1002
	 * @param playerObjId
	 * @param args (FastList<String>)
	 */
	public PlayerShowBoard(final int playerObjId, final FastList<String> args)
	{
		this(playerObjId);
		try
		{
			final byte[] data = getBytes1002(args);
			writeD(DEC_HEAD.length + data.length); // write DEC_HEAD length + args length
			writeB(DEC_HEAD); // write head
			writeB(data); // write args
		}
		catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
		}
	}
	
	private final byte[] getBytes10X(final String html, final byte id) throws UnsupportedEncodingException
	{
	    if (html == null) return DEC_10X[id];
	    final byte[] dataHtml = html.getBytes("UTF-16LE");
        final byte[] data = new byte[12 + dataHtml.length];
        data[0] = 49;
        data[2] = 48;
        data[4] = (byte)(49 + id);
        data[6] = 8;
        System.arraycopy(dataHtml, 0, data, 8, dataHtml.length);
        return data;
	}
	
	private final byte[] getBytes1001(final String html) throws UnsupportedEncodingException
	{
		final byte[] dataHtml = html.getBytes("UTF-16LE");
        final byte[] data = new byte[14 + dataHtml.length];
        data[0] = 49;
        data[2] = 48;
        data[4] = 48;
        data[6] = 49;
        data[8] = 8;
        System.arraycopy(dataHtml, 0, data, 10, dataHtml.length);
        return data;
	}
	
	private final byte[] getBytes1002(final FastList<String> args) throws UnsupportedEncodingException
	{
		int len = 10;
		for (final String arg : args)
		{
			len += (arg.length() + 4) * 2;
		}
		final byte data[] = new byte[len];
		
		data[0] = 49;
		data[2] = 48;
		data[4] = 48;
		data[6] = 50;
		data[8] = 8;
		
		int i = 10;
		for (final String arg : args)
		{
			final byte[] dataHtml = arg.getBytes("UTF-16LE");
			System.arraycopy(dataHtml, 0, data, i, dataHtml.length);
			i += dataHtml.length;
			data[i] = 0x20;
			i+=2;
			data[i] = 0x08;
			i+=2;
		}
		return data;
	}
}