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
package com.l2jserver.gameserver.network.clientpackets;

import java.util.logging.Logger;

import com.l2jserver.Config;
import com.l2jserver.gameserver.model.L2World;
import com.l2jserver.gameserver.model.TradeList;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;

/**
 * This class ...
 *
 * @version $Revision: 1.6.2.2.2.2 $ $Date: 2005/03/27 15:29:30 $
 */
public final class TradeDone extends L2GameClientPacket
{
	private static final String _C__17_TRADEDONE = "[C] 17 TradeDone";
	private static Logger _log = Logger.getLogger(TradeDone.class.getName());
	
	private int _response;
	
	@Override
	protected void readImpl()
	{
		_response = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final L2PcInstance player = getClient().getActiveChar();
		if (player == null)
			return;
		
		if (!getClient().getFloodProtectors().getTransaction().tryPerformAction("trade"))
		{
			player.sendMessage("You trading too fast.");
			return;
		}
		
		final TradeList trade = player.getActiveTradeList();
		if (trade == null)
		{
			if (Config.DEBUG)
				_log.warning("player.getTradeList == null in " + getType() + " for player " + player.getName());
			return;
		}
		if (trade.isLocked())
			return;
		
		if (_response == 1)
		{
			if (trade.getPartner() == null || L2World.getInstance().getPlayer(trade.getPartner().getObjectId()) == null)
			{
				// Trade partner not found, cancel trade
				player.cancelActiveTrade();
				player.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_NOT_FOUND_IN_THE_GAME));
				return;
			}
			
			if (trade.getOwner().getActiveEnchantItem() != null || trade.getPartner().getActiveEnchantItem() != null)
				return;
			
			if (!player.getAccessLevel().allowTransaction())
			{
				player.cancelActiveTrade();
				player.sendPacket(new SystemMessage(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT));
				return;
			}
			
			if (player.getInstanceId() != trade.getPartner().getInstanceId() && player.getInstanceId() != -1)
			{
				player.cancelActiveTrade();
				return;
			}
			
			trade.confirm();
		}
		else
			player.cancelActiveTrade();
	}
	
	/* (non-Javadoc)
	 * @see com.l2jserver.gameserver.clientpackets.ClientBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _C__17_TRADEDONE;
	}
}
