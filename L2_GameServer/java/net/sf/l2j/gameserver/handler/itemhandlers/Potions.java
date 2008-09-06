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
package net.sf.l2j.gameserver.handler.itemhandlers;

import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PlayableInstance;
import net.sf.l2j.gameserver.model.entity.TvTEvent;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 * This class ...
 *
 * @version $Revision: 1.2.4.4 $ $Date: 2005/03/27 15:30:07 $
 */

public class Potions implements IItemHandler
{
	protected static final Logger _log = Logger.getLogger(Potions.class.getName());
	private int _herbstask = 0;
	
	/** Task for Herbs */
	private class HerbTask implements Runnable
	{
		private L2PlayableInstance _activeChar;
		private int _magicId;
		private int _level;
		
		HerbTask(L2PlayableInstance activeChar, int magicId, int level)
		{
			_activeChar = activeChar;
			_magicId = magicId;
			_level = level;
		}
		
		public void run()
		{
			try
			{
				usePotion(_activeChar, _magicId, _level);
			}
			catch (Throwable t)
			{
				_log.log(Level.WARNING, "", t);
			}
		}
	}
	private static final int[] ITEM_IDS =
	{
		65, 725, 726, 727, 728, 734, 735, 1060, 1061, 1073,
		1374, 1375, 1539, 1540, 5591, 5592, 6035, 6036,
		6652, 6553, 6554, 6555, 8193, 8194, 8195, 8196,
		8197, 8198, 8199, 8200, 8201, 8202, 8600, 8601,
		8602, 8603, 8604, 8605, 8606, 8607,
		8608, 8609, 8610, 8611, 8612, 8613, 8614, 10155, 10157,
		//Attribute Potion
		9997, 9998, 9999, 10000, 10001, 10002,
		//elixir of life
		8622, 8623, 8624, 8625, 8626, 8627,
		//elixir of Strength
		8628, 8629, 8630, 8631, 8632, 8633,
		//elixir of cp
		8634, 8635, 8636, 8637, 8638, 8639,
		// Endeavor Potion
		733,
		// Juices
		10260, 10261, 10262, 10263, 10264, 10265, 10266, 10267, 10268, 10269, 10270,
		// CT2 herbs
		10655, 10656, 10657
	};
	
	/**
	 * 
	 * @see net.sf.l2j.gameserver.handler.IItemHandler#useItem(net.sf.l2j.gameserver.model.actor.instance.L2PlayableInstance, net.sf.l2j.gameserver.model.L2ItemInstance)
	 */
	public synchronized void useItem(L2PlayableInstance playable, L2ItemInstance item)
	{
		L2PcInstance activeChar;
		boolean res = false;
		if (playable instanceof L2PcInstance)
			activeChar = (L2PcInstance) playable;
		else if (playable instanceof L2PetInstance)
			activeChar = ((L2PetInstance) playable).getOwner();
		else
			return;
		
		if (!TvTEvent.onPotionUse(playable.getObjectId()))
		{
			playable.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (activeChar.isInOlympiadMode())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.THIS_ITEM_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT));
			return;
		}
		
		if (activeChar.isAllSkillsDisabled())
		{
			ActionFailed af = ActionFailed.STATIC_PACKET;
			activeChar.sendPacket(af);
			return;
		}
		
		int itemId = item.getItemId();
		
		switch (itemId)
		{
			// HEALING AND SPEED POTIONS
			case 65: // red_potion, xml: 2001
				res = usePotion(activeChar, 2001, 1);
				break;
			case 725: // healing_drug, xml: 2002
				if (!isEffectReplaceable(activeChar, L2Effect.EffectType.HEAL_OVER_TIME, itemId))
					return;
				res = usePotion(activeChar, 2002, 1);
				break;
			case 727: // _healing_potion, xml: 2032
				if (!isEffectReplaceable(activeChar, L2Effect.EffectType.HEAL_OVER_TIME, itemId))
					return;
				res = usePotion(activeChar, 2032, 1);
				break;
			case 733: // Endeavor Potion, xml: 2010
				res = usePotion(activeChar, 2010, 1);
				break;
			case 734: // quick_step_potion, xml: 2011
				res = usePotion(activeChar, 2011, 1);
				break;
			case 735: // swift_attack_potion, xml: 2012
				res = usePotion(activeChar, 2012, 1);
				break;
			case 1060: // lesser_healing_potion,
			case 1073: // beginner's potion, xml: 2031
				if (!isEffectReplaceable(activeChar, L2Effect.EffectType.HEAL_OVER_TIME, itemId))
					return;
				res = usePotion(activeChar, 2031, 1);
				break;
			case 1061: // healing_potion, xml: 2032
				if (!isEffectReplaceable(activeChar, L2Effect.EffectType.HEAL_OVER_TIME, itemId))
					return;
				res = usePotion(activeChar, 2032, 1);
				break;
			case 10157: // instant haste_potion, xml: 2398
				res = usePotion(activeChar, 2398, 1);
				break;
			case 1374: // adv_quick_step_potion, xml: 2034
				res = usePotion(activeChar, 2034, 1);
				break;
			case 1375: // adv_swift_attack_potion, xml: 2035
				res = usePotion(activeChar, 2035, 1);
				break;
			case 1539: // greater_healing_potion, xml: 2037
				if (!isEffectReplaceable(activeChar, L2Effect.EffectType.HEAL_OVER_TIME, itemId))
					return;
				res = usePotion(activeChar, 2037, 1);
				break;
			case 1540: // quick_healing_potion, xml: 2038
				if (!isEffectReplaceable(activeChar, L2Effect.EffectType.HEAL_OVER_TIME, itemId))
					return;
				res = usePotion(activeChar, 2038, 1);
				break;
			case 5591:
			case 5592: // CP and Greater CP
				if (!isEffectReplaceable(activeChar, L2Effect.EffectType.COMBAT_POINT_HEAL_OVER_TIME, itemId))
					return;
				res = usePotion(activeChar, 2166, (itemId == 5591) ? 1 : 2);
				break;
			case 6035: // Magic Haste Potion, xml: 2169
				res = usePotion(activeChar, 2169, 1);
				break;
			case 6036: // Greater Magic Haste Potion, xml: 2169
				res = usePotion(activeChar, 2169, 2);
				break;
			case 10155: //Mental Potion XML:2396
				res = usePotion(activeChar, 2396, 1);
				break;
			
			// ATTRIBUTE POTION
			case 9997: // Fire Resist Potion, xml: 2335
				res = usePotion(activeChar, 2335, 1);
				break;
			case 9998: // Water Resist Potion, xml: 2336
				res = usePotion(activeChar, 2336, 1);
				break;
			case 9999: // Earth Resist Potion, xml: 2338
				res = usePotion(activeChar, 2338, 1);
				break;
			case 10000: // Wind Resist Potion, xml: 2337
				res = usePotion(activeChar, 2337, 1);
				break;
			case 10001: // Dark Resist Potion, xml: 2340
				res = usePotion(activeChar, 2340, 1);
				break;
			case 10002: // Divine Resist Potion, xml: 2339
				res = usePotion(activeChar, 2339, 1);
				break;
			
			// ELIXIR
			case 8622:
			case 8623:
			case 8624:
			case 8625:
			case 8626:
			case 8627:
			{
				// elixir of Life
				byte expIndex = (byte) activeChar.getExpertiseIndex();
				if ((itemId == 8622 && expIndex == 0) || (itemId == 8623 && expIndex == 1) || (itemId == 8624 && expIndex == 2) || (itemId == 8625 && expIndex == 3) || (itemId == 8626 && expIndex == 4)
						|| (itemId == 8627 && (expIndex == 5 || expIndex == 6)))
					res = usePotion(activeChar, 2287, (expIndex > 5 ? expIndex : expIndex + 1));
				else
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.INCOMPATIBLE_ITEM_GRADE); // INCOMPATIBLE_ITEM_GRADE
					sm.addItemName(item);
					activeChar.sendPacket(sm);
					return;
				}
				break;
			}
			case 8628:
			case 8629:
			case 8630:
			case 8631:
			case 8632:
			case 8633:
			{
				byte expIndex = (byte) activeChar.getExpertiseIndex();
				// elixir of Strength
				if ((itemId == 8628 && expIndex == 0) || (itemId == 8629 && expIndex == 1) || (itemId == 8630 && expIndex == 2) || (itemId == 8631 && expIndex == 3) || (itemId == 8632 && expIndex == 4)
						|| (itemId == 8633 && (expIndex == 5 || expIndex == 6)))
					res = usePotion(activeChar, 2288, (expIndex > 5 ? expIndex : expIndex + 1));
				else
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.INCOMPATIBLE_ITEM_GRADE); // INCOMPATIBLE_ITEM_GRADE
					sm.addItemName(item);
					activeChar.sendPacket(sm);
					return;
				}
				break;
			}
			case 8634:
			case 8635:
			case 8636:
			case 8637:
			case 8638:
			case 8639:
			{
				byte expIndex = (byte) activeChar.getExpertiseIndex();
				// elixir of cp
				if ((itemId == 8634 && expIndex == 0) || (itemId == 8635 && expIndex == 1) || (itemId == 8636 && expIndex == 2) || (itemId == 8637 && expIndex == 3) || (itemId == 8638 && expIndex == 4)
						|| (itemId == 8639 && (expIndex == 5 || expIndex == 6)))
					res = usePotion(activeChar, 2289, (expIndex > 5 ? expIndex : expIndex + 1));
				else
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.INCOMPATIBLE_ITEM_GRADE); // INCOMPATIBLE_ITEM_GRADE
					sm.addItemName(item);
					activeChar.sendPacket(sm);
					return;
				}
				break;
			}
				// VALAKAS AMULETS
			case 6652: // Amulet Protection of Valakas
				res = usePotion(activeChar, 2231, 1);
				break;
			case 6653: // Amulet Flames of Valakas
				res = usePotion(activeChar, 2223, 1);
				break;
			case 6654: // Amulet Flames of Valakas
				res = usePotion(activeChar, 2233, 1);
				break;
			case 6655: // Amulet Slay Valakas
				res = usePotion(activeChar, 2232, 1);
				break;
			
			// HERBS
			case 8600: // Herb of Life
				res = usePotion(playable, 2278, 1);
				break;
			case 8601: // Greater Herb of Life
				res = usePotion(playable, 2278, 2);
				break;
			case 8602: // Superior Herb of Life
				res = usePotion(playable, 2278, 3);
				break;
			case 8603: // Herb of Mana
				res = usePotion(playable, 2279, 1);
				break;
			case 8604: // Greater Herb of Mane
				res = usePotion(playable, 2279, 2);
				break;
			case 8605: // Superior Herb of Mane
				res = usePotion(playable, 2279, 3);
				break;
			case 8606: // Herb of Strength
				res = usePotion(playable, 2280, 1);
				break;
			case 8607: // Herb of Magic
				res = usePotion(playable, 2281, 1);
				break;
			case 8608: // Herb of Atk. Spd.
				res = usePotion(playable, 2282, 1);
				break;
			case 8609: // Herb of Casting Spd.
				res = usePotion(playable, 2283, 1);
				break;
			case 8610: // Herb of Critical Attack
				res = usePotion(playable, 2284, 1);
				break;
			case 8611: // Herb of Speed
				res = usePotion(playable, 2285, 1);
				break;
			case 8612: // Herb of Warrior
				res = usePotion(playable, 2280, 1);// Herb of Strength
				res = usePotion(playable, 2282, 1);// Herb of Atk. Spd
				res = usePotion(playable, 2284, 1);// Herb of Critical Attack
				break;
			case 8613: // Herb of Mystic
				res = usePotion(playable, 2281, 1);// Herb of Magic
				res = usePotion(playable, 2283, 1);// Herb of Casting Spd.
				break;
			case 8614: // Herb of Warrior
				res = usePotion(playable, 2278, 3);// Superior Herb of Life
				res = usePotion(playable, 2279, 3);// Superior Herb of Mana
				break;
			case 10655:
				res = usePotion(playable, 2512, 1);
				break;
			case 10656:
				res = usePotion(playable, 2514, 1);
				break;
			case 10657:
				res = usePotion(playable, 2513, 1);
				break;
			
			// FISHERMAN POTIONS
			case 8193: // Fisherman's Potion - Green
				if (activeChar.getSkillLevel(1315) <= 3)
				{
					playable.destroyItem("Consume", item.getObjectId(), 1, null, false);
					playable.sendPacket(new SystemMessage(SystemMessageId.NOTHING_HAPPENED));
					return;
				}
				res = usePotion(activeChar, 2274, 1);
				break;
			case 8194: // Fisherman's Potion - Jade
				if (activeChar.getSkillLevel(1315) <= 6)
				{
					playable.destroyItem("Consume", item.getObjectId(), 1, null, false);
					playable.sendPacket(new SystemMessage(SystemMessageId.NOTHING_HAPPENED));
					return;
				}
				res = usePotion(activeChar, 2274, 2);
				break;
			case 8195: // Fisherman's Potion - Blue
				if (activeChar.getSkillLevel(1315) <= 9)
				{
					playable.destroyItem("Consume", item.getObjectId(), 1, null, false);
					playable.sendPacket(new SystemMessage(SystemMessageId.NOTHING_HAPPENED));
					return;
				}
				res = usePotion(activeChar, 2274, 3);
				break;
			case 8196: // Fisherman's Potion - Yellow
				if (activeChar.getSkillLevel(1315) <= 12)
				{
					playable.destroyItem("Consume", item.getObjectId(), 1, null, false);
					playable.sendPacket(new SystemMessage(SystemMessageId.NOTHING_HAPPENED));
					return;
				}
				res = usePotion(activeChar, 2274, 4);
				break;
			case 8197: // Fisherman's Potion - Orange
				if (activeChar.getSkillLevel(1315) <= 15)
				{
					playable.destroyItem("Consume", item.getObjectId(), 1, null, false);
					playable.sendPacket(new SystemMessage(SystemMessageId.NOTHING_HAPPENED));
					return;
				}
				res = usePotion(activeChar, 2274, 5);
				break;
			case 8198: // Fisherman's Potion - Purple
				if (activeChar.getSkillLevel(1315) <= 18)
				{
					playable.destroyItem("Consume", item.getObjectId(), 1, null, false);
					playable.sendPacket(new SystemMessage(SystemMessageId.NOTHING_HAPPENED));
					return;
				}
				res = usePotion(activeChar, 2274, 6);
				break;
			case 8199: // Fisherman's Potion - Red
				if (activeChar.getSkillLevel(1315) <= 21)
				{
					playable.destroyItem("Consume", item.getObjectId(), 1, null, false);
					playable.sendPacket(new SystemMessage(SystemMessageId.NOTHING_HAPPENED));
					return;
				}
				res = usePotion(activeChar, 2274, 7);
				break;
			case 8200: // Fisherman's Potion - White
				if (activeChar.getSkillLevel(1315) <= 24)
				{
					playable.destroyItem("Consume", item.getObjectId(), 1, null, false);
					playable.sendPacket(new SystemMessage(SystemMessageId.NOTHING_HAPPENED));
					return;
				}
				res = usePotion(activeChar, 2274, 8);
				break;
			case 8201: // Fisherman's Potion - Black
				res = usePotion(activeChar, 2274, 9);
				break;
			case 8202: // Fishing Potion
				res = usePotion(activeChar, 2275, 1);
				break;
			
			// Juices
			// added by Z0mbie!
			case 10260: // Haste Juice,xml:2429
				res = usePotion(activeChar, 2429, 1);
				break;
			case 10261: // Accuracy Juice,xml:2430
				res = usePotion(activeChar, 2430, 1);
				break;
			case 10262: // Critical Power Juice,xml:2431
				res = usePotion(activeChar, 2431, 1);
				break;
			case 10263: // Critical Attack Juice,xml:2432
				res = usePotion(activeChar, 2432, 1);
				break;
			case 10264: // Casting Speed Juice,xml:2433
				res = usePotion(activeChar, 2433, 1);
				break;
			case 10265: // Evasion Juice,xml:2434
				res = usePotion(activeChar, 2434, 1);
				break;
			case 10266: // Magic Power Juice,xml:2435
				res = usePotion(activeChar, 2435, 1);
				break;
			case 10267: // Power Juice,xml:2436
				res = usePotion(activeChar, 2436, 1);
				break;
			case 10268: // Speed Juice,xml:2437
				res = usePotion(activeChar, 2437, 1);
				break;
			case 10269: // Defense Juice,xml:2438
				res = usePotion(activeChar, 2438, 1);
				break;
			case 10270: // MP Consumption Juice,xml: 2439
				res = usePotion(activeChar, 2439, 1);
				break;
			default:
		}
		
		if (res)
			playable.destroyItem("Consume", item.getObjectId(), 1, null, false);
	}
	
	/**
	 * 
	 * @param activeChar
	 * @param effectType
	 * @param itemId
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private boolean isEffectReplaceable(L2PcInstance activeChar, Enum effectType, int itemId)
	{
		L2Effect[] effects = activeChar.getAllEffects();
		
		if (effects == null)
			return true;
		
		for (L2Effect e : effects)
		{
			if (e.getEffectType() == effectType)
			{
				// One can reuse pots after 2/3 of their duration is over.
				// It would be faster to check if its > 10 but that would screw custom pot durations...
				if (e.getTaskTime() > (e.getSkill().getBuffDuration() * 67) / 100000)
					return true;
				SystemMessage sm = new SystemMessage(SystemMessageId.S1_PREPARED_FOR_REUSE);
				sm.addItemName(itemId);
				activeChar.sendPacket(sm);
				return false;
			}
		}
		return true;
	}
	
	/**
	 * 
	 * @param activeChar
	 * @param magicId
	 * @param level
	 * @return
	 */
	public boolean usePotion(L2PlayableInstance activeChar, int magicId, int level)
	{
		if (activeChar.isCastingNow() && (magicId > 2277 && magicId < 2286 || magicId >= 2512 && magicId <= 2514))
		{
			_herbstask += 100;
			ThreadPoolManager.getInstance().scheduleAi(new HerbTask(activeChar, magicId, level), _herbstask);
		}
		else
		{
			if ((magicId > 2277 && magicId < 2286 || magicId >= 2512 && magicId <= 2514) && _herbstask >= 100)
				_herbstask -= 100;
			
			L2Skill skill = SkillTable.getInstance().getInfo(magicId, level);
			
			if (skill != null)
			{
				// Return false if potion is in reuse
				// so is not destroyed from inventory
				if (activeChar.isSkillDisabled(skill.getId()))
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.S1_PREPARED_FOR_REUSE);
					sm.addSkillName(skill);
					activeChar.sendPacket(sm);
					
					return false;
				}
				
				activeChar.doCast(skill);
				
				if (activeChar instanceof L2PcInstance)
				{
					L2PcInstance player = (L2PcInstance)activeChar;
					//only for Heal potions
					if (magicId == 2031 || magicId == 2032 || magicId == 2037)
					{
						player.shortBuffStatusUpdate(magicId, level, 15);
					}
				
					if (!(player.isSitting() && !skill.isPotion()))
						return true;
				}
				else if (activeChar instanceof L2PetInstance)
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.PET_USES_S1);
					sm.addString(skill.getName());
					((L2PetInstance)(activeChar)).getOwner().sendPacket(sm);
				}
			}
		}
		return false;
	}
	
	/**
	 * 
	 * @see net.sf.l2j.gameserver.handler.IItemHandler#getItemIds()
	 */
	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
}
