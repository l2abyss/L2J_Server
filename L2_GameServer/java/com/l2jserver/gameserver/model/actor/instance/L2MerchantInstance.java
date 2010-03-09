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
package com.l2jserver.gameserver.model.actor.instance;

import com.l2jserver.Config;
import com.l2jserver.gameserver.TradeController;
import com.l2jserver.gameserver.datatables.MerchantPriceConfigTable;
import com.l2jserver.gameserver.datatables.MerchantPriceConfigTable.MerchantPriceConfig;
import com.l2jserver.gameserver.model.L2TradeList;
import com.l2jserver.gameserver.network.serverpackets.ActionFailed;
import com.l2jserver.gameserver.network.serverpackets.ExBuySellListPacket;
import com.l2jserver.gameserver.network.serverpackets.MyTargetSelected;
import com.l2jserver.gameserver.network.serverpackets.NpcHtmlMessage;
import com.l2jserver.gameserver.network.serverpackets.StatusUpdate;
import com.l2jserver.gameserver.templates.chars.L2NpcTemplate;
import com.l2jserver.gameserver.util.StringUtil;

/**
 * This class ...
 *
 * @version $Revision: 1.10.4.9 $ $Date: 2005/04/11 10:06:08 $
 */
public class L2MerchantInstance extends L2NpcInstance
{
	private MerchantPriceConfig _mpc;

	/**
	 * @param template
	 */
	public L2MerchantInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
		setInstanceType(InstanceType.L2MerchantInstance);
	}

	@Override
	public void onSpawn()
	{
		super.onSpawn();
		_mpc = MerchantPriceConfigTable.getInstance().getMerchantPriceConfig(this);
	}

	@Override
	public String getHtmlPath(int npcId, int val)
	{
		String pom = "";

		if (val == 0) pom = "" + npcId;
		else pom = npcId + "-" + val;

		return "data/html/merchant/" + pom + ".htm";
	}

	/**
	 * @return Returns the mpc.
	 */
	public MerchantPriceConfig getMpc()
	{
		return _mpc;
	}

	public final void showBuyWindow(L2PcInstance player, int val)
	{
		double taxRate = 0;

		taxRate = getMpc().getTotalTaxRate();

		player.tempInventoryDisable();

		if (Config.DEBUG)
			_log.fine("Showing buylist");

		L2TradeList list = TradeController.getInstance().getBuyList(val);

		if (list != null && list.getNpcId().equals(String.valueOf(getNpcId())))
			player.sendPacket(new ExBuySellListPacket(player, list, taxRate, false));
		else
		{
			_log.warning("possible client hacker: "+player.getName()+" attempting to buy from GM shop! < Ban him!");
			_log.warning("buylist id:" + val);
		}

		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	@Override
	public final void onActionShift(L2PcInstance player)
	{
		if (player == null)
			return;

		if (player.isGM())
		{
			player.setTarget(this);

			MyTargetSelected my = new MyTargetSelected(getObjectId(), player.getLevel() - getLevel());
			player.sendPacket(my);

			if (isAutoAttackable(player))
			{
				StatusUpdate su = new StatusUpdate(getObjectId());
				su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
				su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
				player.sendPacket(su);
			}

			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			final StringBuilder html1 = StringUtil.startAppend(2000,
					"<html><body><center><font color=\"LEVEL\">Merchant Info</font></center><br><table border=0><tr><td>Object ID: </td><td>",
					String.valueOf(getObjectId()),
					"</td></tr><tr><td>Template ID: </td><td>",
					String.valueOf(getTemplate().npcId),
					"</td></tr><tr><td><br></td></tr><tr><td>HP: </td><td>",
					String.valueOf(getCurrentHp()),
					"</td></tr><tr><td>MP: </td><td>",
					String.valueOf(getCurrentMp()),
					"</td></tr><tr><td>Level: </td><td>",
					String.valueOf(getLevel()),
					"</td></tr><tr><td><br></td></tr><tr><td>Class: </td><td>",
					getClass().getSimpleName(),
					"</td></tr><tr><td><br></td></tr></table><table><tr><td><button value=\"Edit NPC\" action=\"bypass -h admin_edit_npc ",
					String.valueOf(getTemplate().npcId),
					"\" width=100 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>" +
					"<td><button value=\"Kill\" action=\"bypass -h admin_kill\" width=40 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr>" +
					"<tr><td><button value=\"Show DropList\" action=\"bypass -h admin_show_droplist ",
					String.valueOf(getTemplate().npcId),
					"\" width=100 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr>" +
					"<td><button value=\"Delete\" action=\"bypass -h admin_delete\" width=40 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr>" +
					"<tr><td><button value=\"View Shop\" action=\"bypass -h admin_showShop ",
					String.valueOf(getTemplate().npcId),
					"\" width=100 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr></table>"
			);

			/** Lease doesn't work at all for now!!!
			StringUtil.append(html1,
				"<button value=\"Lease next week\" action=\"bypass -h npc_",
				String.valueOf(getObjectId()),
				"_Lease\" width=100 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">" +
				"<button value=\"Abort current leasing\" action=\"bypass -h npc_",
				String.valueOf(getObjectId()),
				"_Lease next\" width=100 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">" +
				"<button value=\"Manage items\" action=\"bypass -h npc_",
				String.valueOf(getObjectId()),
				"_Lease manage\" width=100 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">"
			);
			 */

			html1.append("</body></html>");

			html.setHtml(html1.toString());
			player.sendPacket(html);
		}

		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
}
