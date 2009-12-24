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
package com.l2jserver.communityserver.network.readpackets;

import java.security.GeneralSecurityException;
import java.security.interfaces.RSAPrivateKey;

import javax.crypto.Cipher;

import com.l2jserver.communityserver.network.GameServerThread;
import com.l2jserver.communityserver.network.netcon.BaseReadPacket;
import com.l2jserver.communityserver.network.netcon.crypt.NewCrypt;

/**
 * @author -Wooden-
 */

public class BlowFishKey extends BaseReadPacket
{
	private final RSAPrivateKey _key;
	private final GameServerThread _gst;
	
	public BlowFishKey(final byte[] data, final RSAPrivateKey key, final GameServerThread gst)
	{
		super(data);
		_key = key;
		_gst = gst;
		
		final int size = readD();
		final byte[] tempKey = readB(size);
		try
		{
			final Cipher rsaCipher = Cipher.getInstance("RSA/ECB/nopadding");
	        rsaCipher.init(Cipher.DECRYPT_MODE, _key);
	        final byte[] tempDecryptKey = rsaCipher.doFinal(tempKey);
	        // there are nulls before the key we must remove them
	        int i = 0;
	        for (; i < tempDecryptKey.length; i++)
	        {
	        	if (tempDecryptKey[i] != 0)
	        		break;
	        }
	        final byte key2[] = new byte[tempDecryptKey.length-i];
	        System.arraycopy(tempDecryptKey, i, key2, 0, tempDecryptKey.length - i);
	        
	        _gst.setCrypt(new NewCrypt(key2));
		}
		catch (GeneralSecurityException e)
		{
			e.printStackTrace();
		}
	}
	
	@Override
	public final void run()
	{
		// nothing
	}
}
