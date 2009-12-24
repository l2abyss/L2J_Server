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
package com.l2jserver.gameserver.network.communityserver.readpackets;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAKeyGenParameterSpec;
import java.security.spec.RSAPublicKeySpec;

import javax.crypto.Cipher;

import com.l2jserver.gameserver.network.communityserver.CommunityServerThread;
import com.l2jserver.gameserver.network.communityserver.writepackets.BlowFishKey;
import com.l2jserver.gameserver.network.communityserver.writepackets.GameServerAuth;
import com.l2jserver.util.Rnd;

import org.netcon.BaseReadPacket;
import org.netcon.crypt.NewCrypt;

/**
 * @authors  Forsaiken, Gigiikun
 */
public final class InitCS extends BaseReadPacket
{
	private final CommunityServerThread _cst;
	
	public InitCS(final byte[] data, final CommunityServerThread cst)
	{
		super(data);
		_cst = cst;
	}
	
	@Override
	public final void run()
	{
		final int length = super.readD();
		final byte[] key = super.readB(length);
		
		try
		{
			final KeyFactory kfac = KeyFactory.getInstance("RSA");
			final RSAPublicKeySpec kspec1 = new RSAPublicKeySpec(new BigInteger(key), RSAKeyGenParameterSpec.F4);
			final RSAPublicKey publicKey = (RSAPublicKey)kfac.generatePublic(kspec1);
			
			final byte[] privateKey = new byte[40];
			Rnd.nextBytes(privateKey);
			
			final Cipher rsaCipher = Cipher.getInstance("RSA/ECB/nopadding");
			rsaCipher.init(Cipher.ENCRYPT_MODE, publicKey);
			final byte[] tempKey = rsaCipher.doFinal(privateKey);
			
			_cst.sendPacket(new BlowFishKey(tempKey), false);
			_cst.setCrypt(new NewCrypt(privateKey));
			_cst.sendPacket(new GameServerAuth(), false);
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
