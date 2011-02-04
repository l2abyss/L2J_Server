/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package com.l2jserver.loginserver;

import java.nio.channels.SocketChannel;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.mmocore.network.IAcceptFilter;
import org.mmocore.network.IClientFactory;
import org.mmocore.network.IMMOExecutor;
import org.mmocore.network.MMOConnection;
import org.mmocore.network.ReceivablePacket;

import com.l2jserver.loginserver.serverpackets.Init;
import com.l2jserver.util.IPv4Filter;

/**
 * 
 * @author KenM
 */
public class SelectorHelper implements IMMOExecutor<L2LoginClient>,
IClientFactory<L2LoginClient>, IAcceptFilter
{
	private ThreadPoolExecutor _generalPacketsThreadPool;
	private IPv4Filter _ipv4filter;
	
	public SelectorHelper()
	{
		_generalPacketsThreadPool = new ThreadPoolExecutor(4, 6, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
		_ipv4filter = new IPv4Filter();
	}
	
	/**
	 * 
	 * @see org.mmocore.network.IMMOExecutor#execute(org.mmocore.network.ReceivablePacket)
	 */
	public void execute(ReceivablePacket<L2LoginClient> packet)
	{
		_generalPacketsThreadPool.execute(packet);
	}
	
	/**
	 * 
	 * @see org.mmocore.network.IClientFactory#create(org.mmocore.network.MMOConnection)
	 */
	public L2LoginClient create(MMOConnection<L2LoginClient> con)
	{
		L2LoginClient client = new L2LoginClient(con);
		client.sendPacket(new Init(client));
		return client;
	}
	
	/**
	 * 
	 * @see org.mmocore.network.IAcceptFilter#accept(java.nio.channels.SocketChannel)
	 */
	public boolean accept(SocketChannel sc)
	{
		return _ipv4filter.accept(sc) && !LoginController.getInstance().isBannedAddress(sc.socket().getInetAddress());
	}
}
