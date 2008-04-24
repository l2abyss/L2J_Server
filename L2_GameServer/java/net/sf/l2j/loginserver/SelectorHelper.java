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
package net.sf.l2j.loginserver;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import net.sf.l2j.loginserver.serverpackets.Init;

import org.mmocore.network.HeaderInfo;
import org.mmocore.network.IAcceptFilter;
import org.mmocore.network.IClientFactory;
import org.mmocore.network.IMMOExecutor;
import org.mmocore.network.MMOConnection;
import org.mmocore.network.ReceivablePacket;
import org.mmocore.network.TCPHeaderHandler;

/**
 *
 * @author  KenM
 */
public class SelectorHelper extends TCPHeaderHandler<L2LoginClient> implements IMMOExecutor<L2LoginClient>, IClientFactory<L2LoginClient>, IAcceptFilter
{
	private ThreadPoolExecutor _generalPacketsThreadPool;

    public SelectorHelper()
	{
        super(null);
		_generalPacketsThreadPool = new ThreadPoolExecutor(4, 6, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
	}

	/**
	 * @see com.l2jserver.mmocore.network.IMMOExecutor#execute(com.l2jserver.mmocore.network.ReceivablePacket)
	 */
	public void execute(ReceivablePacket<L2LoginClient> packet)
	{
		_generalPacketsThreadPool.execute(packet);
	}

	/**
	 * @see com.l2jserver.mmocore.network.IClientFactory#create(com.l2jserver.mmocore.network.MMOConnection)
	 */
	public L2LoginClient create(MMOConnection<L2LoginClient> con)
	{
		L2LoginClient client = new L2LoginClient(con);
		client.sendPacket(new Init(client));
		return client;
	}

	/**
	 * @see com.l2jserver.mmocore.network.IAcceptFilter#accept(java.nio.channels.SocketChannel)
	 */
	public boolean accept(SocketChannel sc)
	{
		return !LoginController.getInstance().isBannedAddress(sc.socket().getInetAddress());
	}

    /**
     * @see org.mmocore.network.TCPHeaderHandler#handleHeader(java.nio.channels.SelectionKey, java.nio.ByteBuffer)
     */
    @SuppressWarnings("unchecked")
    @Override
    public HeaderInfo handleHeader(SelectionKey key, ByteBuffer buf)
    {
        if (buf.remaining() >= 2)
        {
            int dataPending = (buf.getShort() & 0xffff) - 2;
            L2LoginClient client = ((MMOConnection<L2LoginClient>) key.attachment()).getClient(); 
            return this.getHeaderInfoReturn().set(0, dataPending, false, client);
        }
        else
        {
            L2LoginClient client = ((MMOConnection<L2LoginClient>) key.attachment()).getClient(); 
            return this.getHeaderInfoReturn().set(2 - buf.remaining(), 0, false, client);
        }
    }

}
