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
package com.l2jserver.gameserver.model.itemcontainer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import com.l2jserver.Config;
import com.l2jserver.L2DatabaseFactory;
import com.l2jserver.gameserver.datatables.ArmorSetsTable;
import com.l2jserver.gameserver.datatables.ItemTable;
import com.l2jserver.gameserver.datatables.SkillTable;
import com.l2jserver.gameserver.model.L2ArmorSet;
import com.l2jserver.gameserver.model.L2ItemInstance;
import com.l2jserver.gameserver.model.L2Object;
import com.l2jserver.gameserver.model.L2Skill;
import com.l2jserver.gameserver.model.L2World;
import com.l2jserver.gameserver.model.L2ItemInstance.ItemLocation;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.network.serverpackets.SkillCoolTime;
import com.l2jserver.gameserver.skills.Stats;
import com.l2jserver.gameserver.templates.item.L2Armor;
import com.l2jserver.gameserver.templates.item.L2EtcItem;
import com.l2jserver.gameserver.templates.item.L2EtcItemType;
import com.l2jserver.gameserver.templates.item.L2Item;
import com.l2jserver.gameserver.templates.item.L2Weapon;
import com.l2jserver.gameserver.templates.item.L2WeaponType;

import javolution.util.FastList;

/**
 * This class manages inventory
 *
 * @version $Revision: 1.13.2.9.2.12 $ $Date: 2005/03/29 23:15:15 $
 * rewritten 23.2.2006 by Advi
 */
public abstract class Inventory extends ItemContainer
{
	//protected static final Logger _log = Logger.getLogger(Inventory.class.getName());

	public interface PaperdollListener
	{
		public void notifyEquiped(int slot, L2ItemInstance inst);
		public void notifyUnequiped(int slot, L2ItemInstance inst);
	}

	public static final int PAPERDOLL_UNDER = 0;
	public static final int PAPERDOLL_REAR = 1;
	public static final int PAPERDOLL_LEAR = 2;
	public static final int PAPERDOLL_LREAR = 3;
	public static final int PAPERDOLL_NECK = 4;
	public static final int PAPERDOLL_LFINGER = 5;
	public static final int PAPERDOLL_RFINGER = 6;
	public static final int PAPERDOLL_LRFINGER = 7;
	public static final int PAPERDOLL_HEAD = 8;
	public static final int PAPERDOLL_RHAND = 9;
	public static final int PAPERDOLL_LHAND = 10;
	public static final int PAPERDOLL_GLOVES = 11;
	public static final int PAPERDOLL_CHEST = 12;
	public static final int PAPERDOLL_LEGS = 13;
	public static final int PAPERDOLL_FEET = 14;
	public static final int PAPERDOLL_BACK = 15;
	public static final int PAPERDOLL_LRHAND = 16;
	public static final int PAPERDOLL_FULLARMOR = 17;
	public static final int PAPERDOLL_HAIR = 18;
	public static final int PAPERDOLL_ALLDRESS = 19;
	public static final int PAPERDOLL_HAIR2 = 20;
	public static final int PAPERDOLL_HAIRALL = 21;
	public static final int PAPERDOLL_RBRACELET = 22;
	public static final int PAPERDOLL_LBRACELET = 23;
	public static final int PAPERDOLL_DECO1 = 24;
	public static final int PAPERDOLL_DECO2 = 25;
	public static final int PAPERDOLL_DECO3 = 26;
	public static final int PAPERDOLL_DECO4 = 27;
	public static final int PAPERDOLL_DECO5 = 28;
	public static final int PAPERDOLL_DECO6 = 29;
	public static final int PAPERDOLL_BELT = 30;
	public static final int PAPERDOLL_TOTALSLOTS = 31;

	//Speed percentage mods
	public static final double MAX_ARMOR_WEIGHT = 12000;

	private final L2ItemInstance[] _paperdoll;
	private final List<PaperdollListener> _paperdollListeners;

	// protected to be accessed from child classes only
	protected int _totalWeight;

	// used to quickly check for using of items of special type
	private int _wearedMask;

	final class FormalWearListener implements PaperdollListener
	{
		public void notifyUnequiped(int slot, L2ItemInstance item)
		{
			if (!(getOwner() != null && getOwner() instanceof L2PcInstance))
				return;

			L2PcInstance owner = (L2PcInstance)getOwner();

			if (item.getItemId() == 6408)
				owner.setIsWearingFormalWear(false);
		}

		public void notifyEquiped(int slot, L2ItemInstance item)
		{
			if (!(getOwner() != null && getOwner() instanceof L2PcInstance))
				return;

			L2PcInstance owner = (L2PcInstance)getOwner();

			// If player equips Formal Wear, unequip weapons and abort cast/attack
			if (item.getItemId() == 6408)
				owner.setIsWearingFormalWear(true);
			else
			{
				if (!owner.isWearingFormalWear())
					return;
			}
		}
	}

	// Recorder of alterations in inventory
	public static final class ChangeRecorder implements PaperdollListener
	{
		private final Inventory _inventory;
		private final List<L2ItemInstance> _changed;

		/**
		 * Constructor of the ChangeRecorder
		 * @param inventory
		 */
		ChangeRecorder(Inventory inventory)
		{
			_inventory = inventory;
			_changed = new FastList<L2ItemInstance>();
			_inventory.addPaperdollListener(this);
		}

		/**
		 * Add alteration in inventory when item equiped
		 */
		public void notifyEquiped(int slot, L2ItemInstance item)
		{
			if (!_changed.contains(item))
				_changed.add(item);
		}

		/**
		 * Add alteration in inventory when item unequiped
		 */
		public void notifyUnequiped(int slot, L2ItemInstance item)
		{
			if (!_changed.contains(item))
				_changed.add(item);
		}

		/**
		 * Returns alterations in inventory
		 * @return L2ItemInstance[] : array of alterated items
		 */
		public L2ItemInstance[] getChangedItems()
		{
			return _changed.toArray(new L2ItemInstance[_changed.size()]);
		}
	}

	final class CrossBowListener implements PaperdollListener
	{
		public void notifyUnequiped(int slot, L2ItemInstance item)
		{
			if (slot != PAPERDOLL_RHAND)
				return;

			if (Config.ASSERT) assert null == getPaperdollItem(PAPERDOLL_RHAND);

			if (item.getItemType() == L2WeaponType.CROSSBOW)
			{
				L2ItemInstance bolts = getPaperdollItem(PAPERDOLL_LHAND);

				if (bolts != null)
					setPaperdollItem(PAPERDOLL_LHAND, null);
			}
		}

		public void notifyEquiped(int slot, L2ItemInstance item)
		{
			if (slot != PAPERDOLL_RHAND)
				return;

			if (Config.ASSERT) assert item == getPaperdollItem(PAPERDOLL_RHAND);

			if (item.getItemType() == L2WeaponType.CROSSBOW)
			{
				L2ItemInstance bolts = findBoltForCrossBow(item.getItem());

				if (bolts != null)
					setPaperdollItem(PAPERDOLL_LHAND, bolts);
			}
		}
	}

	final class BowListener implements PaperdollListener
	{
		public void notifyUnequiped(int slot, L2ItemInstance item)
		{
			if (slot != PAPERDOLL_LRHAND)
				return;

			if (Config.ASSERT) assert null == getPaperdollItem(PAPERDOLL_LRHAND);

			if (item.getItemType() == L2WeaponType.BOW)
			{
				L2ItemInstance arrow = getPaperdollItem(PAPERDOLL_LHAND);

				if (arrow != null)
					setPaperdollItem(PAPERDOLL_LHAND, null);
			}
		}

		public void notifyEquiped(int slot, L2ItemInstance item)
		{
			if (slot != PAPERDOLL_LRHAND)
				return;

			if (Config.ASSERT) assert item == getPaperdollItem(PAPERDOLL_LRHAND);

			if (item.getItemType() == L2WeaponType.BOW)
			{
				L2ItemInstance arrow = findArrowForBow(item.getItem());

				if (arrow != null)
					setPaperdollItem(PAPERDOLL_LHAND, arrow);
			}
		}
	}

	final class StatsListener implements PaperdollListener
	{
		public void notifyUnequiped(int slot, L2ItemInstance item)
		{
			if (slot == PAPERDOLL_LRHAND)
				return;
			getOwner().removeStatsOwner(item);
		}

		public void notifyEquiped(int slot, L2ItemInstance item)
		{
			if (slot == PAPERDOLL_LRHAND)
				return;
			getOwner().addStatFuncs(item.getStatFuncs(getOwner()));
		}
	}

	final class ItemSkillsListener implements PaperdollListener
	{
		public void notifyUnequiped(int slot, L2ItemInstance item)
		{
			L2PcInstance player;

			if (getOwner() instanceof L2PcInstance)
				player = (L2PcInstance)getOwner();
			else
				return;

			L2Skill enchant4Skill, itemSkill;
			int skillId, skillLvl;
			L2Item it = item.getItem();
			boolean update = false;

			if (it instanceof L2Weapon)
			{
				// Remove augmentation bonuses on unequip
				if (item.isAugmented())
					item.getAugmentation().removeBonus(player);

				if (item.getElementals() != null)
					item.getElementals().removeBonus(player);
				// Remove skills bestowed from +4 Rapiers/Duals
				if (item.getEnchantLevel() >= 4)
				{
					enchant4Skill = ((L2Weapon)it).getEnchant4Skill();

					if (enchant4Skill != null)
					{
						player.removeSkill(enchant4Skill, false, enchant4Skill.isPassive());
						update = true;
					}
				}

				final String[] skills = ((L2Weapon)it).getSkills();

				if (skills != null)
				{
					for (String skillInfo : skills)
					{
						skillId = 0;
						skillLvl = 0;
						String[] skill = skillInfo.split("-");

						if (skill != null && skill.length == 2)
						{
							try
							{
								skillId = Integer.parseInt(skill[0]);
								skillLvl = Integer.parseInt(skill[1]);
							}
							catch (NumberFormatException e)
							{
								_log.warning("Inventory.ItemSkillsListener.Weapon: Incorrect skill: "+skillInfo+".");
							}
							if (skillId > 0 && skillLvl > 0)
							{
								itemSkill = SkillTable.getInstance().getInfo(skillId, skillLvl);
								
								if (itemSkill != null)
								{
									player.removeSkill(itemSkill, false, itemSkill.isPassive());
									update = true;
								}
								else
									_log.warning("Inventory.ItemSkillsListener.Weapon: Incorrect skill: "+skillInfo+".");
							}
						}
					}
				}
			}
			else if (it instanceof L2Armor)
			{
				// Remove augmentation bonuses on unequip
				if (item.isAugmented())
					item.getAugmentation().removeBonus(player);

				if (item.getElementals() != null)
					item.getElementals().removeBonus(player);
				
				// Remove skills bestowed from +4 armor
				if (item.getEnchantLevel() >= 4)
				{
					enchant4Skill = ((L2Armor)it).getEnchant4Skill();

					if (enchant4Skill != null)
					{
						player.removeSkill(enchant4Skill, false, enchant4Skill.isPassive());
						update = true;
					}
				}
				
				final String[] skills = ((L2Armor)it).getSkills();

				if (skills != null)
				{
					for (String skillInfo : skills)
					{
						skillId = 0;
						skillLvl = 0;
						String[] skill = skillInfo.split("-");

						if (skill != null && skill.length == 2)
						{
							try
							{
								skillId = Integer.parseInt(skill[0]);
								skillLvl = Integer.parseInt(skill[1]);
							}
							catch (NumberFormatException e)
							{
								_log.warning("Inventory.ItemSkillsListener.Armor: Incorrect skill: "+skillInfo+".");								
							}
							if (skillId > 0 && skillLvl > 0)
							{
								itemSkill = SkillTable.getInstance().getInfo(skillId, skillLvl);
								
								if (itemSkill != null)
								{
									player.removeSkill(itemSkill, false, itemSkill.isPassive());
									update = true;
								}
								else
									_log.warning("Inventory.ItemSkillsListener.Armor: Incorrect skill: "+skillInfo+".");								
							}
						}
					}
				}
			}
			if (update)
				player.sendSkillList();
		}

		public void notifyEquiped(int slot, L2ItemInstance item)
		{
			L2PcInstance player;

			if (getOwner() instanceof L2PcInstance)
				player = (L2PcInstance)getOwner();
			else 
				return;

			L2Skill enchant4Skill, itemSkill;
			int skillId, skillLvl;
			L2Item it = item.getItem();
			boolean update = false;
			boolean updateTimeStamp = false;

			if (it instanceof L2Weapon)
			{
				// Apply augmentation bonuses on equip
				if (item.isAugmented()) 
					item.getAugmentation().applyBonus(player);
				
				if (item.getElementals() != null)
					item.getElementals().applyBonus(player, false);

				// Add skills bestowed from +4 Rapiers/Duals
				if (item.getEnchantLevel() >= 4)
				{
					enchant4Skill= ((L2Weapon)it).getEnchant4Skill();

					if (enchant4Skill != null)
					{
						player.addSkill(enchant4Skill, false);
						update = true;
					}
				}

				final String[] skills = ((L2Weapon)it).getSkills();

				if (skills != null)
				{
					for (String skillInfo : skills)
					{
						skillId = 0;
						skillLvl = 0;
						String[] skill = skillInfo.split("-");

						if (skill != null && skill.length == 2)
						{
							try
							{
								skillId = Integer.parseInt(skill[0]);
								skillLvl = Integer.parseInt(skill[1]);
							}
							catch (NumberFormatException e)
							{
								_log.warning("Inventory.ItemSkillsListener.Weapon: Incorrect skill: "+skillInfo+".");								
							}
							if (skillId > 0 && skillLvl > 0)
							{
								itemSkill = SkillTable.getInstance().getInfo(skillId, skillLvl);
								
								if (itemSkill != null)
								{
									player.addSkill(itemSkill, false);

									if (itemSkill.isActive())
									{
										if (player.getReuseTimeStamp().isEmpty() || !player.getReuseTimeStamp().containsKey(itemSkill.getId()))
										{
											int equipDelay = itemSkill.getEquipDelay();

											if (equipDelay > 0)
											{
												player.addTimeStamp(itemSkill.getId(), equipDelay);
												player.disableSkill(itemSkill.getId(), equipDelay);
											}
										}
										updateTimeStamp = true;
									}
									update = true;
								}
								else
									_log.warning("Inventory.ItemSkillsListener.Weapon: Incorrect skill: "+skillInfo+".");
							}
						}
					}
				}
			}
			else if (it instanceof L2Armor)
			{
				// Apply augmentation bonuses on equip
				if (item.isAugmented()) 
					item.getAugmentation().applyBonus(player); 
				
				if (item.getElementals() != null)
					item.getElementals().applyBonus(player, true);
				
				// Add skills bestowed from +4 armor
				if (item.getEnchantLevel() >= 4)
				{
					enchant4Skill = ((L2Armor)it).getEnchant4Skill();

					if (enchant4Skill != null)
					{
						player.addSkill(enchant4Skill, false);
						update = true;
					}
				}
				
				final String[] skills = ((L2Armor)it).getSkills();

				if (skills != null)
				{
					for (String skillInfo : skills)
					{
						skillId = 0;
						skillLvl = 0;
						String[] skill = skillInfo.split("-");

						if (skill != null && skill.length == 2)
						{
							try
							{
								skillId = Integer.parseInt(skill[0]);
								skillLvl = Integer.parseInt(skill[1]);
							}
							catch (NumberFormatException e)
							{
								_log.warning("Inventory.ItemSkillsListener.Armor: Incorrect skill: "+skillInfo+".");								
							}
							if (skillId > 0 && skillLvl > 0)
							{
								itemSkill = SkillTable.getInstance().getInfo(skillId, skillLvl);
								
								if (itemSkill != null)
								{
									player.addSkill(itemSkill, false);

									if (itemSkill.isActive())
									{
										if (player.getReuseTimeStamp().isEmpty() || !player.getReuseTimeStamp().containsKey(itemSkill.getId()))
										{
											int equipDelay = itemSkill.getEquipDelay();

											if (equipDelay > 0)
											{
												player.addTimeStamp(itemSkill.getId(), itemSkill.getEquipDelay());
												player.disableSkill(itemSkill.getId(), itemSkill.getEquipDelay());
											}
										}
										updateTimeStamp = true;
									}
									update = true;
								}
								else
									_log.warning("Inventory.ItemSkillsListener.Armor: Incorrect skill: "+skillInfo+".");
							}
						}
					}
				}
			}
			if (update)
			{
				player.sendSkillList();

				if (updateTimeStamp)
					player.sendPacket(new SkillCoolTime(player));
			}
		}
	}

	final class ArmorSetListener implements PaperdollListener
	{
		public void notifyEquiped(int slot, L2ItemInstance item)
		{
			if (!(getOwner() instanceof L2PcInstance))
				return;

			L2PcInstance player = (L2PcInstance)getOwner();

			// Checks if player is wearing a chest item
			L2ItemInstance chestItem = getPaperdollItem(PAPERDOLL_CHEST); 

			if (chestItem == null)
				return;

			// Checks for armorset for the equiped chest
			L2ArmorSet armorSet = ArmorSetsTable.getInstance().getSet(chestItem.getItemId());

			if (armorSet == null)
				return;

			boolean update = false;
			boolean updateTimeStamp = false;
			// Checks if equiped item is part of set
			if (armorSet.containItem(slot, item.getItemId()))
			{
				if (armorSet.containAll(player))
				{
					int skillId, skillLvl;
					L2Skill itemSkill;
					final String[] skills = armorSet.getSkills();

					if (skills != null)
					{
						for (String skillInfo : skills)
						{
							skillId = 0;
							skillLvl = 0;
							String[] skill = skillInfo.split("-");
							if (skill != null && skill.length == 2)
							{
								try
								{
									skillId = Integer.parseInt(skill[0]);
									skillLvl = Integer.parseInt(skill[1]);
								}
								catch (NumberFormatException e)
								{
									_log.warning("Inventory.ArmorSetListener: Incorrect skill: "+skillInfo+".");
								}
								if (skillId > 0 && skillLvl > 0)
								{
									itemSkill = SkillTable.getInstance().getInfo(skillId, skillLvl);
									if (itemSkill != null)
									{
										player.addSkill(itemSkill, false);

										if (itemSkill.isActive())
										{
											if (player.getReuseTimeStamp().isEmpty() || !player.getReuseTimeStamp().containsKey(itemSkill.getId()))
											{
												int equipDelay = itemSkill.getEquipDelay();

												if (equipDelay > 0)
												{
													player.addTimeStamp(itemSkill.getId(), itemSkill.getEquipDelay());
													player.disableSkill(itemSkill.getId(), itemSkill.getEquipDelay());
												}
											}
											updateTimeStamp = true;
										}
										update = true;
									}
									else
									{
										_log.warning("Inventory.ArmorSetListener: Incorrect skill: "+skillInfo+".");
									}
								}
							}
						}
					}

					if (armorSet.containShield(player)) // has shield from set
					{
						final L2Skill shieldSkill = SkillTable.getInstance().getInfo(armorSet.getShieldSkillId(),1);

						if (shieldSkill != null)
						{
							player.addSkill(shieldSkill, false);
							update = true;
						}
						else
							_log.warning("Inventory.ArmorSetListener: Incorrect skill: "+armorSet.getShieldSkillId()+".");
					}

					if (armorSet.isEnchanted6(player)) // has all parts of set enchanted to 6 or more
					{
						final int skillId6 = armorSet.getEnchant6skillId();

						if (skillId6 > 0)
						{
							L2Skill skille = SkillTable.getInstance().getInfo(skillId6,1);

							if (skille != null)
							{
								player.addSkill(skille, false);
								update = true;
							}
							else
								_log.warning("Inventory.ArmorSetListener: Incorrect skill: "+armorSet.getEnchant6skillId()+".");
						}
					}
				}
			}
			else if (armorSet.containShield(item.getItemId()))
			{
				if (armorSet.containAll(player))
				{
					final L2Skill shieldSkill = SkillTable.getInstance().getInfo(armorSet.getShieldSkillId(),1);

					if (shieldSkill != null)
					{
						player.addSkill(shieldSkill, false);
						update = true;
					}
					else
						_log.warning("Inventory.ArmorSetListener: Incorrect skill: "+armorSet.getShieldSkillId()+".");
				}
			}

			if (update)
			{
				player.sendSkillList();

				if (updateTimeStamp)
					player.sendPacket(new SkillCoolTime(player));
			}
		}

		public void notifyUnequiped(int slot, L2ItemInstance item)
		{
			if (!(getOwner() instanceof L2PcInstance))
				return;

			L2PcInstance player = (L2PcInstance)getOwner();

			boolean remove = false;
			int skillId, skillLvl;
			L2Skill itemSkill;
			String[] skills = null;
			int shieldSkill = 0; // shield skill
			int skillId6 = 0; // enchant +6 skill

			if (slot == PAPERDOLL_CHEST)
			{
				L2ArmorSet armorSet = ArmorSetsTable.getInstance().getSet(item.getItemId());
				if (armorSet == null)
					return;

				remove = true;
				skills = armorSet.getSkills();
				shieldSkill = armorSet.getShieldSkillId();
				skillId6 = armorSet.getEnchant6skillId();
			}
			else
			{
				L2ItemInstance chestItem = getPaperdollItem(PAPERDOLL_CHEST); 
				if (chestItem == null)
					return;

				L2ArmorSet armorSet = ArmorSetsTable.getInstance().getSet(chestItem.getItemId());
				if (armorSet == null)
					return;

				if (armorSet.containItem(slot, item.getItemId())) // removed part of set
				{
					remove = true;
					skills = armorSet.getSkills();
					shieldSkill = armorSet.getShieldSkillId();
					skillId6 = armorSet.getEnchant6skillId();
				}
				else if (armorSet.containShield(item.getItemId())) // removed shield
				{
					remove = true;
					shieldSkill = armorSet.getShieldSkillId();
				}
			}

			if (remove)
			{
				if (skills != null)
				{
					for (String skillInfo : skills)
					{
						skillId = 0;
						skillLvl = 0;
						String[] skill = skillInfo.split("-");
						if (skill != null && skill.length == 2)
						{
							try
							{
								skillId = Integer.parseInt(skill[0]);
								skillLvl = Integer.parseInt(skill[1]);
							}
							catch (NumberFormatException e)
							{
								_log.warning("Inventory.ArmorSetListener: Incorrect skill: "+skillInfo+".");
							}
							if (skillId > 0 && skillLvl >0)
							{
								itemSkill = SkillTable.getInstance().getInfo(skillId, skillLvl);
								if (itemSkill != null)
									player.removeSkill(itemSkill, false, itemSkill.isPassive());
								else
									_log.warning("Inventory.ArmorSetListener: Incorrect skill: "+skillInfo+".");
							}
						}
					}
				}

				if (shieldSkill != 0)
				{
					L2Skill skill = SkillTable.getInstance().getInfo(shieldSkill,1);
					if (skill != null)
						player.removeSkill(skill, false, skill.isPassive());
					else
						_log.warning("Inventory.ArmorSetListener: Incorrect skill: "+shieldSkill+".");
				}

				if (skillId6 != 0)
				{
					L2Skill skill = SkillTable.getInstance().getInfo(skillId6,1);
					if (skill != null)
						player.removeSkill(skill, false, skill.isPassive());
					else
						_log.warning("Inventory.ArmorSetListener: Incorrect skill: "+skillId6+".");
				}

				player.checkItemRestriction();
				player.sendSkillList();
			}
		}
	}

	final class BraceletListener implements PaperdollListener
	{
		public void notifyUnequiped(int slot, L2ItemInstance item)
		{
			if (item.getItem().getBodyPart() == L2Item.SLOT_R_BRACELET)
			{
				unEquipItemInSlot(PAPERDOLL_DECO1);
				unEquipItemInSlot(PAPERDOLL_DECO2);
				unEquipItemInSlot(PAPERDOLL_DECO3);
				unEquipItemInSlot(PAPERDOLL_DECO4);
				unEquipItemInSlot(PAPERDOLL_DECO5);
				unEquipItemInSlot(PAPERDOLL_DECO6);
			}
		}

		// Note (April 3, 2009): Currently on equip, talismans do not display properly, do we need checks here to fix this?
		public void notifyEquiped(int slot, L2ItemInstance item)
		{
		}
	}

	/**
	 * Constructor of the inventory
	 */
	protected Inventory()
	{
		_paperdoll = new L2ItemInstance[31];
		_paperdollListeners = new FastList<PaperdollListener>();
		addPaperdollListener(new ArmorSetListener());
		addPaperdollListener(new CrossBowListener());
		addPaperdollListener(new BowListener());
		addPaperdollListener(new ItemSkillsListener());
		addPaperdollListener(new StatsListener());
		//addPaperdollListener(new FormalWearListener());
		addPaperdollListener(new BraceletListener());
	}

	protected abstract ItemLocation getEquipLocation();

	/**
	 * Returns the instance of new ChangeRecorder 
	 * @return ChangeRecorder
	 */
	public ChangeRecorder newRecorder()
	{
		return new ChangeRecorder(this);
	}

	/**
	 * Drop item from inventory and updates database
	 * @param process : String Identifier of process triggering this action
	 * @param item : L2ItemInstance to be dropped
	 * @param actor : L2PcInstance Player requesting the item drop
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the destroyed item or the updated item in inventory
	 */
	public L2ItemInstance dropItem(String process, L2ItemInstance item, L2PcInstance actor, L2Object reference)
	{
		if (item == null)
			return null;

		synchronized (item)
		{
			if (!_items.contains(item))
				return null;

			removeItem(item);
			item.setOwnerId(process, 0, actor, reference);
			item.setLocation(ItemLocation.VOID);
			item.setLastChange(L2ItemInstance.REMOVED);

			item.updateDatabase();
			refreshWeight();
		}
		return item;
	}

	/**
	 * Drop item from inventory by using its <B>objectID</B> and updates database
	 * @param process : String Identifier of process triggering this action
	 * @param objectId : int Item Instance identifier of the item to be dropped
	 * @param count : int Quantity of items to be dropped
	 * @param actor : L2PcInstance Player requesting the item drop
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the destroyed item or the updated item in inventory
	 */
	public L2ItemInstance dropItem(String process, int objectId, long count, L2PcInstance actor, L2Object reference)
	{
		L2ItemInstance item = getItemByObjectId(objectId);
		if (item == null)
			return null;

		synchronized (item)
		{
			if (!_items.contains(item))
				return null;

			// Adjust item quantity and create new instance to drop
			// Directly drop entire item
			if (item.getCount() > count)
			{
				item.changeCount(process, -count, actor, reference);
				item.setLastChange(L2ItemInstance.MODIFIED);
				item.updateDatabase();

				item = ItemTable.getInstance().createItem(process, item.getItemId(), count, actor, reference);
				item.updateDatabase();
				refreshWeight();
				return item;
			}
		}
		return dropItem(process, item, actor, reference);
	}

	/**
	 * Adds item to inventory for further adjustments and Equip it if necessary (itemlocation defined)<BR><BR>
	 * 
	 * @param item : L2ItemInstance to be added from inventory
	 */
	@Override
	protected void addItem(L2ItemInstance item)
	{
		super.addItem(item);
		if (item.isEquipped())
			equipItem(item);
	}

	/**
	 * Removes item from inventory for further adjustments.
	 * @param item : L2ItemInstance to be removed from inventory
	 */
	@Override
	protected boolean removeItem(L2ItemInstance item)
	{
		// Unequip item if equiped
		for (int i = 0; i < _paperdoll.length; i++)
		{
			if (_paperdoll[i] == item)
				unEquipItemInSlotAndRecord(i);
		}
		return super.removeItem(item);
	}

	/**
	 * Returns the item in the paperdoll slot
	 * @return L2ItemInstance
	 */
	public L2ItemInstance getPaperdollItem(int slot)
	{
		return _paperdoll[slot];
	}

	/**
	 * Returns the item in the paperdoll L2Item slot
	 * @param L2Item slot identifier
	 * @return L2ItemInstance
	 */
	public L2ItemInstance getPaperdollItemByL2ItemId(int slot)
	{
		switch (slot)
		{
			case 0x01:
				return _paperdoll[0];
			case 0x02:
				return _paperdoll[1];
			case 0x04:
				return _paperdoll[2];
			case 0x06:
				return _paperdoll[3];
			case 0x08:
				return _paperdoll[4];
			case 0x10:
				return _paperdoll[5];
			case 0x20:
				return _paperdoll[6];
			case 0x30:
				return _paperdoll[7];
			case 0x040:
				return _paperdoll[8];
			case 0x080:
				return _paperdoll[9];
			case 0x0100:
				return _paperdoll[10];
			case 0x0200:
				return _paperdoll[11];
			case 0x0400:
				return _paperdoll[12];
			case 0x0800:
				return _paperdoll[13];
			case 0x1000:
				return _paperdoll[14];
			case 0x2000:
				return _paperdoll[15];
			case 0x4000:
				return _paperdoll[16];
			case 0x8000:
				return _paperdoll[17];
			case 0x010000:
				return _paperdoll[18];
			case 0x020000:
				return _paperdoll[19];
			case 0x040000:
				return _paperdoll[20];
			case 0x080000:
				return _paperdoll[21];
			case 0x100000:
				return _paperdoll[22];
			case 0x200000:
				return _paperdoll[23];
			case 0x400000:
				return _paperdoll[24];
			case 0x10000000:
				return _paperdoll[30];
		}
		return null;
	}

	/**
	 * Returns the ID of the item in the paperdol slot
	 * @param slot : int designating the slot
	 * @return int designating the ID of the item
	 */
	public int getPaperdollItemId(int slot)
	{
		L2ItemInstance item = _paperdoll[slot]; 
		if (item != null)
			return item.getItemId();
		else if (slot == PAPERDOLL_HAIR)
		{
			item = _paperdoll[PAPERDOLL_HAIRALL]; 
			if (item != null)
				return item.getItemId();
		}
		return 0;
	}

	public int getPaperdollAugmentationId(int slot)
	{
		L2ItemInstance item = _paperdoll[slot];
		if (item != null)
		{
			if (item.getAugmentation() != null)
				return item.getAugmentation().getAugmentationId();
			else
				return 0;
		}
		return 0;
	}

	/**
	 * Returns the objectID associated to the item in the paperdoll slot
	 * @param slot : int pointing out the slot
	 * @return int designating the objectID
	 */
	public int getPaperdollObjectId(int slot)
	{
		L2ItemInstance item = _paperdoll[slot]; 
		if (item != null)
			return item.getObjectId();
		else if (slot == PAPERDOLL_HAIR)
		{
			item = _paperdoll[PAPERDOLL_HAIRALL]; 
			if (item != null)
				return item.getObjectId();
		}
		return 0;
	}

	/** 
	 * Adds new inventory's paperdoll listener
	 * @param PaperdollListener pointing out the listener
	 */
	public synchronized void addPaperdollListener(PaperdollListener listener)
	{
		if (Config.ASSERT) assert !_paperdollListeners.contains(listener);
			_paperdollListeners.add(listener);
	}

	/** 
	 * Removes a paperdoll listener
	 * @param PaperdollListener pointing out the listener to be deleted 
	 */
	public synchronized void removePaperdollListener(PaperdollListener listener)
	{
		_paperdollListeners.remove(listener);
	}

	/**
	 * Equips an item in the given slot of the paperdoll.
	 * <U><I>Remark :</I></U> The item <B>HAS TO BE</B> already in the inventory
	 * @param slot : int pointing out the slot of the paperdoll
	 * @param item : L2ItemInstance pointing out the item to add in slot
	 * @return L2ItemInstance designating the item placed in the slot before
	 */
	public synchronized L2ItemInstance setPaperdollItem(int slot, L2ItemInstance item)
	{
		L2ItemInstance old = _paperdoll[slot]; 
		if (old != item)
		{
			if (old != null)
			{
				_paperdoll[slot] = null;
				// Put old item from paperdoll slot to base location
				old.setLocation(getBaseLocation());
				old.setLastChange(L2ItemInstance.MODIFIED);
				// Get the mask for paperdoll
				int mask = 0;
				for (int i=0; i < PAPERDOLL_LRHAND; i++)
				{
					L2ItemInstance pi = _paperdoll[i]; 
					if (pi != null)
						mask |= pi.getItem().getItemMask(); 
				}
				_wearedMask = mask;
				// Notify all paperdoll listener in order to unequip old item in slot
				for (PaperdollListener listener : _paperdollListeners)
				{
					if (listener == null)
						continue;

					listener.notifyUnequiped(slot, old);
				}
				old.updateDatabase();
			}
			// Add new item in slot of paperdoll
			if (item != null)
			{
				_paperdoll[slot] = item;
				item.setLocation(getEquipLocation(), slot);
				item.setLastChange(L2ItemInstance.MODIFIED);
				_wearedMask |= item.getItem().getItemMask();
				for (PaperdollListener listener : _paperdollListeners)
				{
					if (listener == null)
						continue;
					
					listener.notifyEquiped(slot, item);
				}
				item.updateDatabase();
			}
		}
		return old;
	}

	/**
	 * Return the mask of weared item
	 * @return int
	 */
	public int getWearedMask()
	{
		return _wearedMask;
	}

	public int getSlotFromItem(L2ItemInstance item)
	{
		int slot = -1;
		int location = item.getLocationSlot();

		switch(location)
		{
			case PAPERDOLL_UNDER:		slot = L2Item.SLOT_UNDERWEAR;
				break;
			case PAPERDOLL_LEAR:		slot = L2Item.SLOT_L_EAR;
				break;
			case PAPERDOLL_REAR:		slot = L2Item.SLOT_R_EAR;
				break;
			case PAPERDOLL_NECK:		slot = L2Item.SLOT_NECK;
				break;
			case PAPERDOLL_RFINGER:		slot = L2Item.SLOT_R_FINGER;
				break;
			case PAPERDOLL_LFINGER:		slot = L2Item.SLOT_L_FINGER;
				break;
			case PAPERDOLL_HAIR:		slot = L2Item.SLOT_HAIR;
				break;
			case PAPERDOLL_HAIR2:		slot = L2Item.SLOT_HAIR2;
				break;
			case PAPERDOLL_HAIRALL:		slot = L2Item.SLOT_HAIRALL;
				break;
			case PAPERDOLL_HEAD:		slot = L2Item.SLOT_HEAD;
				break;
			case PAPERDOLL_RHAND:		slot = L2Item.SLOT_R_HAND;
				break;
			case PAPERDOLL_LHAND:		slot = L2Item.SLOT_L_HAND;
				break;
			case PAPERDOLL_GLOVES:		slot = L2Item.SLOT_GLOVES;
				break;
			case PAPERDOLL_CHEST:		slot = item.getItem().getBodyPart();
				break;
			case PAPERDOLL_LEGS:		slot = L2Item.SLOT_LEGS;
				break;
			case PAPERDOLL_BACK:		slot = L2Item.SLOT_BACK;
				break;
			case PAPERDOLL_FEET:		slot = L2Item.SLOT_FEET;
				break;
			case PAPERDOLL_LRHAND:		slot = L2Item.SLOT_LR_HAND;
				break;
			case PAPERDOLL_LBRACELET:	 slot = L2Item.SLOT_L_BRACELET;
				break;
			case PAPERDOLL_RBRACELET:	 slot = L2Item.SLOT_R_BRACELET;
				break;
			case PAPERDOLL_DECO1:
			case PAPERDOLL_DECO2:
			case PAPERDOLL_DECO3:
			case PAPERDOLL_DECO4:
			case PAPERDOLL_DECO5:
			case PAPERDOLL_DECO6:		slot = L2Item.SLOT_DECO;
				break;
			case PAPERDOLL_BELT:		slot = L2Item.SLOT_BELT;
				break;
		}
		return slot;
	}

	/**
	 * Unequips item in body slot and returns alterations.
	 * @param slot : int designating the slot of the paperdoll
	 * @return L2ItemInstance[] : list of changes
	 */
	public L2ItemInstance[] unEquipItemInBodySlotAndRecord(int slot)
	{
		Inventory.ChangeRecorder recorder = newRecorder(); 

		try
		{
			unEquipItemInBodySlot(slot); 
			if (getOwner() instanceof L2PcInstance)
				((L2PcInstance)getOwner()).refreshExpertisePenalty();
		}
		finally
		{
			removePaperdollListener(recorder);
		}
		return recorder.getChangedItems();
	}

	/**
	 * Sets item in slot of the paperdoll to null value
	 * @param pdollSlot : int designating the slot
	 * @return L2ItemInstance designating the item in slot before change
	 */
	public L2ItemInstance unEquipItemInSlot(int pdollSlot)
	{
		return setPaperdollItem(pdollSlot, null);
	}

	/**
	 * Unepquips item in slot and returns alterations
	 * @param slot : int designating the slot
	 * @return L2ItemInstance[] : list of items altered
	 */
	public L2ItemInstance[] unEquipItemInSlotAndRecord(int slot)
	{
	Inventory.ChangeRecorder recorder = newRecorder(); 

	try
	{
		unEquipItemInSlot(slot);
		if (getOwner() instanceof L2PcInstance)
			((L2PcInstance)getOwner()).refreshExpertisePenalty();
	}
	finally
	{
		removePaperdollListener(recorder);
	}
	return recorder.getChangedItems();
	}

	/**
	 * Unequips item in slot (i.e. equips with default value)
	 * @param slot : int designating the slot
	 */
	private void unEquipItemInBodySlot(int slot)
	{
		if (Config.DEBUG)
			_log.fine("--- unequip body slot:" + slot);

		int pdollSlot = -1;

		switch (slot)
		{
			case L2Item.SLOT_L_EAR:		pdollSlot = PAPERDOLL_LEAR;
				break;
			case L2Item.SLOT_R_EAR:		pdollSlot = PAPERDOLL_REAR;
				break;
			case L2Item.SLOT_NECK:		pdollSlot = PAPERDOLL_NECK;
				break;
			case L2Item.SLOT_R_FINGER:	pdollSlot = PAPERDOLL_RFINGER;
				break;
			case L2Item.SLOT_L_FINGER:	pdollSlot = PAPERDOLL_LFINGER;
				break;
			case L2Item.SLOT_HAIR:		pdollSlot = PAPERDOLL_HAIR;
				break;
			case L2Item.SLOT_HAIR2:		pdollSlot = PAPERDOLL_HAIR2;
				break;
			case L2Item.SLOT_HAIRALL:
				setPaperdollItem(PAPERDOLL_HAIR, null);
				setPaperdollItem(PAPERDOLL_HAIR2, null);// this should be the same as in DHAIR
				pdollSlot = PAPERDOLL_HAIRALL;
				break;
			case L2Item.SLOT_HEAD:		pdollSlot = PAPERDOLL_HEAD;
				break;
			case L2Item.SLOT_R_HAND:	pdollSlot = PAPERDOLL_RHAND;
				break;
			case L2Item.SLOT_L_HAND:	pdollSlot = PAPERDOLL_LHAND;
				break;
			case L2Item.SLOT_GLOVES:	pdollSlot = PAPERDOLL_GLOVES;
				break;
			case L2Item.SLOT_CHEST:
			case L2Item.SLOT_FULL_ARMOR:pdollSlot = PAPERDOLL_CHEST;
				break;
			case L2Item.SLOT_LEGS:		pdollSlot = PAPERDOLL_LEGS;
				break;
			case L2Item.SLOT_BACK:		pdollSlot = PAPERDOLL_BACK;
				break;
			case L2Item.SLOT_FEET:		pdollSlot = PAPERDOLL_FEET;
				break;
			case L2Item.SLOT_UNDERWEAR:	pdollSlot = PAPERDOLL_UNDER;
				break;
			case L2Item.SLOT_LR_HAND:
				setPaperdollItem(PAPERDOLL_LHAND, null);
				setPaperdollItem(PAPERDOLL_RHAND, null);// this should be the same as in LRHAND
				pdollSlot = PAPERDOLL_LRHAND;
				break;
			case L2Item.SLOT_L_BRACELET:	pdollSlot = PAPERDOLL_LBRACELET;
				break;
			case L2Item.SLOT_R_BRACELET:	pdollSlot = PAPERDOLL_RBRACELET;
				break;
			case L2Item.SLOT_BELT:			pdollSlot = PAPERDOLL_BELT;
				break;
		}
		if (pdollSlot >= 0)
			setPaperdollItem(pdollSlot, null);
	}

	/**
	 * Equips item and returns list of alterations
	 * @param item : L2ItemInstance corresponding to the item
	 * @return L2ItemInstance[] : list of alterations
	 */
	public L2ItemInstance[] equipItemAndRecord(L2ItemInstance item)
	{
		Inventory.ChangeRecorder recorder = newRecorder();

		try
		{
			equipItem(item); 
		}
		finally
		{
			removePaperdollListener(recorder);
		}
		return recorder.getChangedItems();
	}

	/**
	 * Equips item in slot of paperdoll.
	 * @param item : L2ItemInstance designating the item and slot used.
	 */
	public void equipItem(L2ItemInstance item)
	{
		if ((getOwner() instanceof L2PcInstance) && ((L2PcInstance)getOwner()).getPrivateStoreType() != 0)
			return;

		if (getOwner() instanceof L2PcInstance)
		{
			L2PcInstance player = (L2PcInstance)getOwner();

			if (!player.isGM() && !player.isHero() && item.isHeroItem())
				return;
		}

		int targetSlot = item.getItem().getBodyPart();

		switch (targetSlot)
		{
			case L2Item.SLOT_LR_HAND:
			{
				if (setPaperdollItem(PAPERDOLL_LHAND, null) != null)
				{
					// exchange 2h for 2h
					setPaperdollItem(PAPERDOLL_RHAND, null);
					setPaperdollItem(PAPERDOLL_LHAND, null);
				}
				else
					setPaperdollItem(PAPERDOLL_RHAND, null);

				setPaperdollItem(PAPERDOLL_RHAND, item);
				setPaperdollItem(PAPERDOLL_LRHAND, item);
				break;
			}
			case L2Item.SLOT_L_HAND:
			{
				if (!(item.getItem() instanceof L2EtcItem) || item.getItem().getItemType() != L2EtcItemType.ARROW)
				{
					L2ItemInstance old1 = setPaperdollItem(PAPERDOLL_LRHAND, null);

					if (old1 != null)
					{
						setPaperdollItem(PAPERDOLL_RHAND, null);
					}
				}

				setPaperdollItem(PAPERDOLL_LHAND, null);
				setPaperdollItem(PAPERDOLL_LHAND, item);
				break;
			}
			case L2Item.SLOT_R_HAND:
			{
				if (_paperdoll[PAPERDOLL_LRHAND] != null)
				{
					setPaperdollItem(PAPERDOLL_LRHAND, null);
					setPaperdollItem(PAPERDOLL_LHAND, null);
					setPaperdollItem(PAPERDOLL_RHAND, null);
				}
				else
					setPaperdollItem(PAPERDOLL_RHAND, null);

				setPaperdollItem(PAPERDOLL_RHAND, item);
				break;
			}
			case L2Item.SLOT_L_EAR:
			case L2Item.SLOT_R_EAR:
			case L2Item.SLOT_L_EAR | L2Item.SLOT_R_EAR:
			{
				if (_paperdoll[PAPERDOLL_LEAR] == null)
					setPaperdollItem(PAPERDOLL_LEAR, item);
				else if (_paperdoll[PAPERDOLL_REAR] == null)
					setPaperdollItem(PAPERDOLL_REAR, item);
				else
				{
					setPaperdollItem(PAPERDOLL_LEAR, null);
					setPaperdollItem(PAPERDOLL_LEAR, item);
				}
				break;
			}
			case L2Item.SLOT_L_FINGER:
			case L2Item.SLOT_R_FINGER:
			case L2Item.SLOT_L_FINGER | L2Item.SLOT_R_FINGER:
			{
				if (_paperdoll[PAPERDOLL_LFINGER] == null)
					setPaperdollItem(PAPERDOLL_LFINGER, item);
				else if (_paperdoll[PAPERDOLL_RFINGER] == null)
					setPaperdollItem(PAPERDOLL_RFINGER, item);
				else
				{
					setPaperdollItem(PAPERDOLL_LFINGER, null);
					setPaperdollItem(PAPERDOLL_LFINGER, item);
				}
				break;
			}
			case L2Item.SLOT_NECK:
				setPaperdollItem(PAPERDOLL_NECK, item);
				break;
			case L2Item.SLOT_FULL_ARMOR:
				setPaperdollItem(PAPERDOLL_CHEST, null);
				setPaperdollItem(PAPERDOLL_LEGS, null);
				setPaperdollItem(PAPERDOLL_CHEST, item);
				break;
			case L2Item.SLOT_CHEST:
				setPaperdollItem(PAPERDOLL_CHEST, item);
				break;
			case L2Item.SLOT_LEGS:
			{
				// handle full armor
				L2ItemInstance chest = getPaperdollItem(PAPERDOLL_CHEST);
				if (chest != null && chest.getItem().getBodyPart() == L2Item.SLOT_FULL_ARMOR)
					setPaperdollItem(PAPERDOLL_CHEST, null);

				setPaperdollItem(PAPERDOLL_LEGS, null);
				setPaperdollItem(PAPERDOLL_LEGS, item);
				break;
			}
			case L2Item.SLOT_FEET:
				setPaperdollItem(PAPERDOLL_FEET, item);
				break;
			case L2Item.SLOT_GLOVES:
				setPaperdollItem(PAPERDOLL_GLOVES, item);
				break;
			case L2Item.SLOT_HEAD:
				setPaperdollItem(PAPERDOLL_HEAD, item);
				break;
			case L2Item.SLOT_HAIR:
				if (setPaperdollItem(PAPERDOLL_HAIRALL, null) != null)
				{
					setPaperdollItem(PAPERDOLL_HAIRALL, null);
					setPaperdollItem(PAPERDOLL_HAIR, null);
					setPaperdollItem(PAPERDOLL_HAIR2, null);
				}
				else
					setPaperdollItem(PAPERDOLL_HAIR, null);

				setPaperdollItem(PAPERDOLL_HAIR, item);
				break;
			case L2Item.SLOT_HAIR2:
				if (setPaperdollItem(PAPERDOLL_HAIRALL, null) != null)
				{
					setPaperdollItem(PAPERDOLL_HAIRALL, null);
					setPaperdollItem(PAPERDOLL_HAIR, null);
					setPaperdollItem(PAPERDOLL_HAIR2, null);
				}
				else
					setPaperdollItem(PAPERDOLL_HAIR2, null);

				setPaperdollItem(PAPERDOLL_HAIR2, item);
				break;
			case L2Item.SLOT_HAIRALL:
				if (setPaperdollItem(PAPERDOLL_HAIR, null) != null)
				{
					setPaperdollItem(PAPERDOLL_HAIR, null);
					setPaperdollItem(PAPERDOLL_HAIR2, null);
				}
				else
				{
					setPaperdollItem(PAPERDOLL_HAIR2, null);
				}

				setPaperdollItem(PAPERDOLL_HAIRALL, item);
				break;
			case L2Item.SLOT_UNDERWEAR:
				setPaperdollItem(PAPERDOLL_UNDER, item);
				break;
			case L2Item.SLOT_BACK:
				setPaperdollItem(PAPERDOLL_BACK, item);
				break;
			case L2Item.SLOT_L_BRACELET:
				setPaperdollItem(PAPERDOLL_LBRACELET, item);
				break;
			case L2Item.SLOT_R_BRACELET:
				setPaperdollItem(PAPERDOLL_RBRACELET, item);
				break;
			case L2Item.SLOT_DECO:
				equipTalisman(item);
				break;
			case L2Item.SLOT_BELT:
				setPaperdollItem(PAPERDOLL_BELT, item);
				break;
			default:
				_log.warning("Unknown body slot "+targetSlot+" for Item ID:"+item.getItemId());
		}
	}

	/**
	 * Refresh the weight of equipment loaded
	 */
	@Override
	protected void refreshWeight()
	{
		long weight = 0;

		for (L2ItemInstance item : _items)
		{
			if (item != null && item.getItem() != null)
				weight += item.getItem().getWeight() * item.getCount();
		}
		_totalWeight = (int)Math.min(weight, Integer.MAX_VALUE);
	}

	/**
	 * Returns the totalWeight.
	 * @return int
	 */
	public int getTotalWeight()
	{
		return _totalWeight;
	}

	/**
	 * Return the L2ItemInstance of the arrows needed for this bow.<BR><BR>
	 * @param bow : L2Item designating the bow
	 * @return L2ItemInstance pointing out arrows for bow
	 */
	public L2ItemInstance findArrowForBow(L2Item bow)
	{
		if (bow == null)
			return null;

		L2ItemInstance arrow = null;

		switch (bow.getItemGradeSPlus())
		{
			default: 
			case L2Item.CRYSTAL_NONE:
				arrow = getItemByItemId(17);
				break; // Wooden arrow
			case L2Item.CRYSTAL_D:
				arrow = (arrow = getItemByItemId(1341)) != null ? arrow : getItemByItemId(22067);
				break; // Bone arrow
			case L2Item.CRYSTAL_C:
				arrow = (arrow = getItemByItemId(1342)) != null ? arrow : getItemByItemId(22068);
				break; // Fine steel arrow
			case L2Item.CRYSTAL_B:
				arrow = (arrow = getItemByItemId(1343)) != null ? arrow : getItemByItemId(22069);
				break; // Silver arrow
			case L2Item.CRYSTAL_A:
				arrow = (arrow = getItemByItemId(1344)) != null ? arrow : getItemByItemId(22070);
				break; // Mithril arrow
			case L2Item.CRYSTAL_S:
				arrow = (arrow = getItemByItemId(1345)) != null ? arrow : getItemByItemId(22071);
				break; // Shining arrow
		}

		// Get the L2ItemInstance corresponding to the item identifier and return it
		return arrow;
	}

	/**
	 * Return the L2ItemInstance of the bolts needed for this crossbow.<BR><BR>
	 * @param crossbow : L2Item designating the crossbow
	 * @return L2ItemInstance pointing out bolts for crossbow
	 */
	public L2ItemInstance findBoltForCrossBow(L2Item crossbow)
	{
		L2ItemInstance bolt = null;

		switch (crossbow.getItemGradeSPlus())
		{
			default: 
			case L2Item.CRYSTAL_NONE:
				bolt = getItemByItemId(9632);
				break; // Wooden Bolt
			case L2Item.CRYSTAL_D:
				bolt = (bolt = getItemByItemId(9633)) != null ? bolt : getItemByItemId(22144);
				break; // Bone Bolt
			case L2Item.CRYSTAL_C:
				bolt = (bolt = getItemByItemId(9634)) != null ? bolt : getItemByItemId(22145);
				break; // Steel Bolt
			case L2Item.CRYSTAL_B:
				bolt = (bolt = getItemByItemId(9635)) != null ? bolt : getItemByItemId(22146);
				break; // Silver Bolt
			case L2Item.CRYSTAL_A:
				bolt = (bolt = getItemByItemId(9636)) != null ? bolt : getItemByItemId(22147);
				break; // Mithril Bolt
			case L2Item.CRYSTAL_S:
				bolt = (bolt = getItemByItemId(9637)) != null ? bolt : getItemByItemId(22148);
				break; // Shining Bolt
		}

		// Get the L2ItemInstance corresponding to the item identifier and return it
		return bolt;
	}

	/**
	 * Get back items in inventory from database
	 */
	@Override
	public void restore()
	{
		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT object_id, item_id, count, enchant_level, loc, loc_data, custom_type1, custom_type2, mana_left, time FROM items WHERE owner_id=? AND (loc=? OR loc=?) ORDER BY loc_data");
			statement.setInt(1, getOwnerId());
			statement.setString(2, getBaseLocation().name());
			statement.setString(3, getEquipLocation().name());
			ResultSet inv = statement.executeQuery();

			L2ItemInstance item;
			while (inv.next())
			{
				item = L2ItemInstance.restoreFromDb(getOwnerId(), inv);

				if (item == null)
					continue;

				if (getOwner() instanceof L2PcInstance)
				{
					L2PcInstance player = (L2PcInstance)getOwner();

					if (!player.isGM() && !player.isHero() && item.isHeroItem())
						item.setLocation(ItemLocation.INVENTORY);
				}

				L2World.getInstance().storeObject(item);

				// If stackable item is found in inventory just add to current quantity
				if (item.isStackable() && getItemByItemId(item.getItemId()) != null)
					addItem("Restore", item, getOwner().getActingPlayer(), null);
				else
					addItem(item);
			}
			inv.close();
			statement.close();
			refreshWeight();
		}
		catch (Exception e)
		{
			_log.warning("Could not restore inventory : " + e);
		}
		finally 
		{
			try
			{
				con.close();
			}
			catch (Exception e) {}
		}
	}

	public int getMaxTalismanCount()
	{
		return (int)getOwner().getStat().calcStat(Stats.TALISMAN_SLOTS, 0, null, null);
	}

	private void equipTalisman(L2ItemInstance item)
	{
		if (getMaxTalismanCount() == 0)
			return;

		// find same (or incompatible) talisman type
		for (int i = PAPERDOLL_DECO1; i < PAPERDOLL_DECO1 + getMaxTalismanCount(); i++)
		{
			if (_paperdoll[i] != null) 
			{
				if (getPaperdollItemId(i) == item.getItemId())
				{
					// overwtite
					setPaperdollItem(i, item);
					return;
				}
			}
		}

		// no free slot found - put on first free
		for (int i = PAPERDOLL_DECO1; i < PAPERDOLL_DECO1 + getMaxTalismanCount(); i++)
		{
			if (_paperdoll[i] == null)
			{
				setPaperdollItem(i, item);
				return;
			}
		}

		// no free slots - put on first
		setPaperdollItem(PAPERDOLL_DECO1, item);
	}

	public int getCloakStatus()
	{
		return (int)getOwner().getStat().calcStat(Stats.CLOAK_SLOT, 0, null, null);
	}

	/**
	 * Re-notify to paperdoll listeners every equipped item
	 */
	public void reloadEquippedItems() 
	{
		int slot;

		for (L2ItemInstance item: _paperdoll)
		{
			if (item == null)
				continue;

			slot = item.getLocationSlot();

			for (PaperdollListener listener : _paperdollListeners)
			{
				if (listener == null)
					continue;

				listener.notifyUnequiped(slot, item);
				listener.notifyEquiped(slot, item);
			}
		}
	}
}
