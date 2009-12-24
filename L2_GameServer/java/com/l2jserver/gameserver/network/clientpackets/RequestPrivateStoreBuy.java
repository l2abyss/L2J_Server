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
import com.l2jserver.gameserver.model.ItemRequest;
import com.l2jserver.gameserver.model.L2Object;
import com.l2jserver.gameserver.model.L2World;
import com.l2jserver.gameserver.model.TradeList;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.network.serverpackets.ActionFailed;
import com.l2jserver.gameserver.util.Util;


import static com.l2jserver.gameserver.model.actor.L2Npc.INTERACTION_DISTANCE;

/**
 * This class ...
 *
 * @version $Revision: 1.2.2.1.2.5 $ $Date: 2005/03/27 15:29:30 $
 */
public final class RequestPrivateStoreBuy extends L2GameClientPacket
{
	private static final String _C__79_REQUESTPRIVATESTOREBUY = "[C] 79 RequestPrivateStoreBuy";
	private static Logger _log = Logger.getLogger(RequestPrivateStoreBuy.class.getName());

	private static final int BATCH_LENGTH = 20; // length of the one item

	private int _storePlayerId;
	private ItemRequest[] _items = null;

	@Override
	protected void readImpl()
	{
		_storePlayerId = readD();
		int count = readD();
		if (count <= 0
				|| count > Config.MAX_ITEM_IN_PACKET
				|| count * BATCH_LENGTH != _buf.remaining())
		{
			return;
		}
		_items = new ItemRequest[count];

		for (int i = 0; i < count ; i++)
		{
			int objectId = readD();
			long cnt = readQ();
			long price = readQ();

			if (objectId < 1 || cnt < 1 || price < 0)
			{
				_items = null;
				return;
			}
			_items[i] = new ItemRequest(objectId, cnt, price);
		}
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if (player == null)
			return;

		if(_items == null)
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if (!player.getFloodProtectors().getTransaction().tryPerformAction("privatestorebuy"))
		{
			player.sendMessage("You buying items too fast.");
			return;
		}

		L2Object object = L2World.getInstance().findObject(_storePlayerId);
		if (!(object instanceof L2PcInstance))
			return;

		if(player.isCursedWeaponEquipped())
			return;

		L2PcInstance storePlayer = (L2PcInstance)object;
		if (!player.isInsideRadius(storePlayer, INTERACTION_DISTANCE, true, false))
			return;

		if (player.getInstanceId() != storePlayer.getInstanceId()
				&& player.getInstanceId() != -1)
			return;

		if (!(storePlayer.getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_SELL || storePlayer.getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_PACKAGE_SELL))
			return;

		TradeList storeList = storePlayer.getSellList();
		if (storeList == null)
			return;

		if (!player.getAccessLevel().allowTransaction())
		{
			player.sendMessage("Transactions are disable for your Access Level");
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if (storePlayer.getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_PACKAGE_SELL)
		{
			if (storeList.getItemCount() > _items.length)
			{
				String msgErr = "[RequestPrivateStoreBuy] player "+getClient().getActiveChar().getName()+" tried to buy less items then sold by package-sell, ban this player for bot-usage!";
				Util.handleIllegalPlayerAction(getClient().getActiveChar(),msgErr,Config.DEFAULT_PUNISH);
				return;
			}
		}

		if (!storeList.privateStoreBuy(player, _items))
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			_log.warning("PrivateStore buy has failed due to invalid list or request. Player: " + player.getName() + ", Private store of: " + storePlayer.getName());
			return;
		}

		if (storeList.getItemCount() == 0)
		{
			storePlayer.setPrivateStoreType(L2PcInstance.STORE_PRIVATE_NONE);
			storePlayer.broadcastUserInfo();
		}

/*   Lease holders are currently not implemented
		else if (_seller != null)
		{
			// lease shop sell
			L2MerchantInstance seller = (L2MerchantInstance)_seller;
			L2ItemInstance ladena = seller.getLeaseAdena();
			for (TradeItem ti : buyerlist) {
				L2ItemInstance li = seller.getLeaseItemByObjectId(ti.getObjectId());
				if (li == null) {
					if (ti.getObjectId() == ladena.getObjectId())
					{
						buyer.addAdena(ti.getCount());
						ladena.setCount(ladena.getCount()-ti.getCount());
						ladena.updateDatabase();
					}
					continue;
				}
				int cnt = li.getCount();
				if (cnt < ti.getCount())
					ti.setCount(cnt);
				if (ti.getCount() <= 0)
					continue;
				L2ItemInstance inst = ItemTable.getInstance().createItem(li.getItemId());
				inst.setCount(ti.getCount());
				inst.setEnchantLevel(li.getEnchantLevel());
				buyer.getInventory().addItem(inst);
				li.setCount(li.getCount()-ti.getCount());
				li.updateDatabase();
				ladena.setCount(ladena.getCount()+ti.getCount()*ti.getOwnersPrice());
				ladena.updateDatabase();
			}
		}*/
	}

	@Override
	public String getType()
	{
		return _C__79_REQUESTPRIVATESTOREBUY;
	}
}
