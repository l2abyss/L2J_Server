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
package com.l2jserver.communityserver;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.RSAKeyGenParameterSpec;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.Logger;

import javolution.util.FastMap;
import com.l2jserver.communityserver.util.Rnd;

public final class GameServerRegistrationTable
{
	private static Logger _log = Logger.getLogger(GameServerRegistrationTable.class.getName());
	private static final int KEYS_SIZE = 10;
	
	private static GameServerRegistrationTable _instance;
	
	public static GameServerRegistrationTable getInstance()
	{
		if (_instance == null)
			_instance = new GameServerRegistrationTable();
		return _instance;
	}
	
	private final Map<byte[], Boolean> _registeredGameServers;
	private final KeyPair[] _keyPairs;
	
	public GameServerRegistrationTable()
	{
		_registeredGameServers = loadRegisteredGameServers();
		_log.info("Loaded "+_registeredGameServers.size()+" registered GameServers");
		
		_keyPairs = loadRSAKeys();
		_log.info("Cached "+_keyPairs.length+" RSA keys for GameServer communication.");
	}
	
	private final KeyPair[] loadRSAKeys()
	{
		final KeyPair[] keyPairs = new KeyPair[KEYS_SIZE];
		
		try
		{
			final KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
			final RSAKeyGenParameterSpec spec = new RSAKeyGenParameterSpec(512, RSAKeyGenParameterSpec.F4);
			keyGen.initialize(spec);
			
			for (int i = 0; i < KEYS_SIZE; i++)
			{
				keyPairs[i] = keyGen.genKeyPair();
			}
		}
		catch (Exception e)
		{
			
		}
		
		return keyPairs;
	}
	
	private final Map<byte[], Boolean> loadRegisteredGameServers()
	{
		final Map<byte[], Boolean> registeredGameServers = new FastMap<byte[], Boolean>();
		
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT hex_id FROM registered_gameservers");
			rset = statement.executeQuery();
			
			while (rset.next())
			{
				// for (byte b : stringToHex(rset.getString("hex_id")))
				registeredGameServers.put(stringToHex(rset.getString("hex_id")), false);
			}
			
		}
		catch (SQLException e)
		{
			
		}
		finally
		{
			try
			{
				con.close();
				statement.close();
				rset.close();
			}
			catch (Exception e)
			{
				
			}
		}
		
		return registeredGameServers;
	}
	
	private final byte[] stringToHex(final String string)
	{
		return new BigInteger(string, 16).toByteArray();
	}
	
	public final KeyPair getRandomKeyPair()
	{
		return _keyPairs[Rnd.get(_keyPairs.length)];
	}
	
	public final boolean isHexIdOk(final byte[] hexId)
	{
		for (final byte[] hex : _registeredGameServers.keySet())
		{
			if (Arrays.equals(hex, hexId))
				return true;
		}
		
		return false;
	}
	
	public final boolean isHexIdInUse(final byte[] hexId)
	{
		for (Map.Entry<byte[], Boolean> entry : _registeredGameServers.entrySet())
		{
			if (entry.getValue() && Arrays.equals(entry.getKey(), hexId))
				return true;
		}
		
		return false;
	}
	
	public final void setHexIdInUse(final byte[] hexId)
	{
		for (Map.Entry<byte[], Boolean> entry : _registeredGameServers.entrySet())
		{
			if (Arrays.equals(entry.getKey(), hexId))
			{
				entry.setValue(false);
				return;
			}
		}
	}
}