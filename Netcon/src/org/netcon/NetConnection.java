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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import org.netcon.crypt.NewCrypt;

/**
 * @author Forsaiken
 */
public abstract class NetConnection extends Thread
{
	private static final NewCrypt INITIAL_CRYPT = new NewCrypt("_;v.]05-31!|+-%xT!^[$\00");
	
	private final NetConnectionConfig _config;
	
	/* TCP */
	private Socket _tcpCon;
	private BufferedInputStream _tcpIn;
	private BufferedOutputStream _tcpOut;
	
	/* CRYPT */
	private NewCrypt _crypt;
	
	protected NetConnection(NetConnectionConfig config)
	{
		_config = config;
	}
	
	public final void connect(final String address, final int port) throws UnknownHostException, IOException
	{
		connect(new Socket(address, port));
	}
	
	public final void connect(final Socket remoteConnection) throws IOException
	{
		if (isConnected())
		{
			throw new IOException("TCP Connect: Allready connected.");
		}
		
		_crypt = INITIAL_CRYPT;
		
		_tcpCon = remoteConnection;
		_tcpOut = new BufferedOutputStream(_tcpCon.getOutputStream(), _config.TCP_SEND_BUFFER_SIZE);
		_tcpIn = new BufferedInputStream(_tcpCon.getInputStream(), _config.TCP_RECEIVE_BUFFER_SIZE);
	}
	
	public final boolean isConnected() throws IOException
	{
		return (_tcpCon != null) && _tcpCon.isConnected();
	}
	
	public final int getConnectionPort() throws IOException
	{
		if (!isConnected())
		{
			throw new IOException("TCP: Not connected.");
		}
		
		return _tcpCon.getPort();
	}
	
	public final String getConnectionAddress() throws IOException
	{
		if (!isConnected())
		{
			throw new IOException("TCP: Not connected.");
		}
		
		return _tcpCon.getInetAddress().getHostAddress();
	}
	
	protected final byte[] read() throws IOException
	{
		if (_tcpCon == null)
		{
			throw new IOException("TCP Read: Not initialized.");
		}
		
		if (_tcpCon.isClosed())
		{
			throw new IOException("TCP Read: Connection closed.");
		}
		
		final int lengthLo = _tcpIn.read();
		final int lengthHi = _tcpIn.read();
		final int length = (lengthHi * 256) + lengthLo;
		
		if (lengthHi < 0)
		{
			throw new IOException("TCP Read: Failed reading.");
		}
		
		final byte[] data = new byte[length - 2];
		
		int receivedBytes = 0;
		int newBytes = 0;
		
		while ((newBytes != -1) && (receivedBytes < (length - 2)))
		{
			newBytes = _tcpIn.read(data, 0, length - 2);
			receivedBytes = receivedBytes + newBytes;
		}
		
		if (receivedBytes != (length - 2))
		{
			throw new IOException("TCP Read: Incomplete Packet recived.");
		}
		
		return decrypt(data);
	}
	
	protected final void write(final BaseWritePacket packet) throws IOException
	{
		if (_tcpCon == null)
		{
			throw new IOException("TCP Write: Not initialized.");
		}
		
		if (_tcpCon.isClosed())
		{
			throw new IOException("TCP Write: Connection closed.");
		}
		
		final byte[] data = crypt(packet.getContent());
		final int len = data.length + 2;
		
		synchronized (_tcpOut)
		{
			_tcpOut.write(len & 0xFF);
			_tcpOut.write((len >> 8) & 0xFF);
			_tcpOut.write(data);
			_tcpOut.flush();
		}
	}
	
	protected final void close(final BaseWritePacket packet) throws IOException
	{
		try
		{
			if (packet != null)
			{
				write(packet);
			}
		}
		finally
		{
			if (_tcpIn != null)
			{
				_tcpIn.close();
				_tcpIn = null;
			}
			
			if (_tcpOut != null)
			{
				_tcpOut.close();
				_tcpOut = null;
			}
			
			if (_tcpCon != null)
			{
				_tcpCon.close();
				_tcpCon = null;
			}
		}
	}
	
	public final void setCrypt(final NewCrypt crypt)
	{
		_crypt = crypt;
	}
	
	private final byte[] decrypt(byte[] data) throws IOException
	{
		data = _crypt.decrypt(data);
		
		if (!NewCrypt.verifyChecksum(data))
		{
			throw new IOException("CRYPT: Incorrect packet checksum.");
		}
		
		return data;
	}
	
	private final byte[] crypt(final byte[] data) throws IOException
	{
		NewCrypt.appendChecksum(data);
		
		return _crypt.crypt(data);
	}
	
	@Override
	public abstract void run();
}
