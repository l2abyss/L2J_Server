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
package net.sf.l2j.gameserver.network.clientpackets;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.EnchantResult;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;
import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.util.Util;
import net.sf.l2j.util.Rnd;

public final class RequestEnchantItem extends AbstractEnchantPacket
{
	protected static final Logger _log = Logger.getLogger(RequestEnchantItem.class.getName());
	protected static final Logger _logEnchant = Logger.getLogger("enchant");

	private static final String _C__58_REQUESTENCHANTITEM = "[C] 58 RequestEnchantItem";

	private int _objectId = 0;

	@Override
	protected void readImpl()
	{
		_objectId = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();

		if (activeChar == null || _objectId == 0)
			return;

		if (activeChar.isOnline() == 0 || getClient().isDetached())
		{
			activeChar.setActiveEnchantItem(null);
			return;
		}

		if (activeChar.isProcessingTransaction() || activeChar.isInStoreMode())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_ENCHANT_WHILE_STORE));
			activeChar.setActiveEnchantItem(null);
			return;
		}

		L2ItemInstance item = activeChar.getInventory().getItemByObjectId(_objectId);
		L2ItemInstance scroll = activeChar.getActiveEnchantItem();
		L2ItemInstance support = activeChar.getActiveEnchantSupportItem();

		if (item == null || scroll == null)
		{
			activeChar.setActiveEnchantItem(null);
			return;
		}

		// template for scroll
		EnchantScroll scrollTemplate = getEnchantScroll(scroll);

		// scroll not found in list
		if (scrollTemplate == null)
			return;

		// template for support item, if exist
		EnchantItem supportTemplate = null;
		if (support != null)
			supportTemplate = getSupportItem(support);

		// first validation check
		if (!scrollTemplate.isValid(item, supportTemplate) || !isEnchantable(item))
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.INAPPROPRIATE_ENCHANT_CONDITION));
			activeChar.setActiveEnchantItem(null);
			activeChar.sendPacket(new EnchantResult(2, 0, 0));
			return;
		}

		// fast auto-enchant cheat check
		if (activeChar.getActiveEnchantTimestamp() == 0 || System.currentTimeMillis() - activeChar.getActiveEnchantTimestamp() < 2000)
		{
			Util.handleIllegalPlayerAction(activeChar, "Player " + activeChar.getName() + " use autoenchant program ", Config.DEFAULT_PUNISH);
			activeChar.setActiveEnchantItem(null);
			activeChar.sendPacket(new EnchantResult(2, 0, 0));
			return;
		}

		// attempting to destroy scroll
		scroll = activeChar.getInventory().destroyItem("Enchant", scroll.getObjectId(), 1, activeChar, item);
		if (scroll == null)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
			Util.handleIllegalPlayerAction(activeChar, "Player " + activeChar.getName() + " tried to enchant with a scroll he doesn't have", Config.DEFAULT_PUNISH);
			activeChar.setActiveEnchantItem(null);
			activeChar.sendPacket(new EnchantResult(2, 0, 0));
			return;
		}

		// attempting to destroy support if exist
		if (support != null)
		{
			support = activeChar.getInventory().destroyItem("Enchant", support.getObjectId(), 1, activeChar, item);
			if (support == null)
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
				Util.handleIllegalPlayerAction(activeChar, "Player " + activeChar.getName() + " tried to enchant with a support item he doesn't have", Config.DEFAULT_PUNISH);
				activeChar.setActiveEnchantItem(null);
				activeChar.sendPacket(new EnchantResult(2, 0, 0));
				return;
			}
		}

		SystemMessage sm;

		synchronized (item)
		{
			int chance = scrollTemplate.getChance(item, supportTemplate);

			// last validation check
			if (item.getOwnerId() != activeChar.getObjectId()
					|| !isEnchantable(item)
					|| chance < 0)
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.INAPPROPRIATE_ENCHANT_CONDITION));
				activeChar.setActiveEnchantItem(null);
				activeChar.sendPacket(new EnchantResult(2, 0, 0));
				return;
			}

			if (Rnd.get(100) < chance)
			{
				// success
				item.setEnchantLevel(item.getEnchantLevel() + 1);
				item.updateDatabase();
				activeChar.sendPacket(new EnchantResult(0, 0, 0));

				if (Config.LOG_ITEM_ENCHANTS)
				{
					LogRecord record = new LogRecord(Level.INFO, "Success");
					record.setParameters(new Object[]{activeChar, item, scroll, support, chance});
					record.setLoggerName("item");
					_logEnchant.log(record);
				}
			}
			else
			{
				if (scrollTemplate.isSafe())
				{
					// safe enchant - remain old value
					// need retail message
					activeChar.sendPacket(new EnchantResult(5, 0, 0));

					if (Config.LOG_ITEM_ENCHANTS)
					{
						LogRecord record = new LogRecord(Level.INFO, "Safe Fail");
						record.setParameters(new Object[]{activeChar, item, scroll, support, chance});
						record.setLoggerName("item");
						_logEnchant.log(record);
					}
				}
				else
				{
					// unequip item on enchant failure to avoid item skills stack
					if (item.isEquipped())
					{
						if (item.getEnchantLevel() > 0)
						{
							sm = new SystemMessage(SystemMessageId.EQUIPMENT_S1_S2_REMOVED);
							sm.addNumber(item.getEnchantLevel());
							sm.addItemName(item);
							activeChar.sendPacket(sm);
						}
						else
						{
							sm = new SystemMessage(SystemMessageId.S1_DISARMED);
							sm.addItemName(item);
							activeChar.sendPacket(sm);
						}

						L2ItemInstance[] unequiped = activeChar.getInventory().unEquipItemInSlotAndRecord(item.getLocationSlot());
						InventoryUpdate iu = new InventoryUpdate();
						for (L2ItemInstance itm : unequiped)
						{
							iu.addModifiedItem(itm);
						}
						activeChar.sendPacket(iu);
						activeChar.broadcastUserInfo();
					}

					if (scrollTemplate.isBlessed())
					{
						// blessed enchant - clear enchant value
						sm = new SystemMessage(SystemMessageId.BLESSED_ENCHANT_FAILED);
						activeChar.sendPacket(sm);

						item.setEnchantLevel(0);
						item.updateDatabase();
						activeChar.sendPacket(new EnchantResult(3, 0, 0));

						if (Config.LOG_ITEM_ENCHANTS)
						{
							LogRecord record = new LogRecord(Level.INFO, "Blessed Fail");
							record.setParameters(new Object[]{activeChar, item, scroll, support, chance});
							record.setLoggerName("item");
							_logEnchant.log(record);
						}
					}
					else 
					{
						// enchant failed, destroy item
						int crystalId = item.getItem().getCrystalItemId();
						int count = item.getCrystalCount() - (item.getItem().getCrystalCount() + 1) / 2;
						if (count < 1)
							count = 1;

						L2ItemInstance destroyItem = activeChar.getInventory().destroyItem("Enchant", item, activeChar, null);
						if (destroyItem == null)
						{
							// unable to destroy item, cheater ?
							Util.handleIllegalPlayerAction(activeChar, "Unable to delete item on enchant failure from player " + activeChar.getName() + ", possible cheater !", Config.DEFAULT_PUNISH);
							activeChar.setActiveEnchantItem(null);
							activeChar.sendPacket(new EnchantResult(2, 0, 0));

							if (Config.LOG_ITEM_ENCHANTS)
							{
								LogRecord record = new LogRecord(Level.INFO, "Unable to destroy");
								record.setParameters(new Object[]{activeChar, item, scroll, support, chance});
								record.setLoggerName("item");
								_logEnchant.log(record);
							}
							return;
						}

						L2ItemInstance crystals = null;
						if (crystalId != 0)
						{
							crystals = activeChar.getInventory().addItem("Enchant", crystalId, count, activeChar, destroyItem);

							sm = new SystemMessage(SystemMessageId.EARNED_S2_S1_S);
							sm.addItemName(crystals);
							sm.addItemNumber(count);
							activeChar.sendPacket(sm);
						}

						if (!Config.FORCE_INVENTORY_UPDATE)
						{
							InventoryUpdate iu = new InventoryUpdate();
							if (destroyItem.getCount() == 0)
								iu.addRemovedItem(destroyItem);
							else
								iu.addModifiedItem(destroyItem);
							if (crystals != null)
								iu.addItem(crystals);

							activeChar.sendPacket(iu);
						}
						else
							activeChar.sendPacket(new ItemList(activeChar, true));

						L2World world = L2World.getInstance();
						world.removeObject(destroyItem);
						if (crystalId == 0)
							activeChar.sendPacket(new EnchantResult(4, 0, 0));
						else
							activeChar.sendPacket(new EnchantResult(1, crystalId, count));

						if (Config.LOG_ITEM_ENCHANTS)
						{
							LogRecord record = new LogRecord(Level.INFO, "Fail");
							record.setParameters(new Object[]{activeChar, item, scroll, support, chance});
							record.setLoggerName("item");
							_logEnchant.log(record);
						}
					}
				}
			}
			sm = null;

			StatusUpdate su = new StatusUpdate(activeChar.getObjectId());
			su.addAttribute(StatusUpdate.CUR_LOAD, activeChar.getCurrentLoad());
			activeChar.sendPacket(su);
			su = null;

			activeChar.sendPacket(new ItemList(activeChar, false));
			activeChar.broadcastUserInfo();
			activeChar.setActiveEnchantItem(null);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _C__58_REQUESTENCHANTITEM;
	}
}
