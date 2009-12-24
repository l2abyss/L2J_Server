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
package com.l2jserver.gameserver.model.zone.type;

import com.l2jserver.Config;
import com.l2jserver.gameserver.datatables.MapRegionTable;
import com.l2jserver.gameserver.datatables.SkillTable;
import com.l2jserver.gameserver.instancemanager.FortManager;
import com.l2jserver.gameserver.instancemanager.FortSiegeManager;
import com.l2jserver.gameserver.model.L2Clan;
import com.l2jserver.gameserver.model.L2Effect;
import com.l2jserver.gameserver.model.L2Skill;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.actor.instance.L2SiegeSummonInstance;
import com.l2jserver.gameserver.model.entity.Fort;
import com.l2jserver.gameserver.model.zone.L2ZoneType;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;

import javolution.util.FastList;

/**
 * A castle zone
 *
 * @author  durgus
 */
public class L2FortZone extends L2ZoneType
{
	private int _fortId;
	private Fort _fort = null;
	private int[] _spawnLoc;
	private boolean _isActiveSiege = false;
	
	public L2FortZone(int id)
	{
		super(id);
		
		_spawnLoc = new int[3];
	}
	
	@Override
	public void setParameter(String name, String value)
	{
		if (name.equals("fortId"))
		{
			_fortId = Integer.parseInt(value);
		}
		else if (name.equals("spawnX"))
		{
			_spawnLoc[0] = Integer.parseInt(value);
		}
		else if (name.equals("spawnY"))
		{
			_spawnLoc[1] = Integer.parseInt(value);
		}
		else if (name.equals("spawnZ"))
		{
			_spawnLoc[2] = Integer.parseInt(value);
		}
		else
			super.setParameter(name, value);
	}
	
	@Override
	protected void onEnter(L2Character character)
	{
		character.setInsideZone(L2Character.ZONE_FORT, true);
		if (getFort() != null && _isActiveSiege)
		{
			character.setInsideZone(L2Character.ZONE_PVP, true);
			character.setInsideZone(L2Character.ZONE_SIEGE, true);
			character.setInsideZone(L2Character.ZONE_NOSUMMONFRIEND, true);
			
			if (character instanceof L2PcInstance)
			{
				((L2PcInstance) character).sendPacket(new SystemMessage(SystemMessageId.ENTERED_COMBAT_ZONE));
				if (((L2PcInstance) character).isRegisteredOnThisSiegeField(getFort().getFortId()))
				{
					((L2PcInstance) character).startFameTask(Config.FORTRESS_ZONE_FAME_TASK_FREQUENCY * 1000, Config.FORTRESS_ZONE_FAME_AQUIRE_POINTS);
					((L2PcInstance) character).setIsInSiege(true);
				}
			}
		}
	}
	
	@Override
	protected void onExit(L2Character character)
	{
		character.setInsideZone(L2Character.ZONE_FORT, false);
		character.setInsideZone(L2Character.ZONE_PVP, false);
		character.setInsideZone(L2Character.ZONE_SIEGE, false);
		character.setInsideZone(L2Character.ZONE_NOSUMMONFRIEND, false);
		if (getFort() != null && _isActiveSiege)
		{
			if (character instanceof L2PcInstance)
			{
				((L2PcInstance) character).sendPacket(new SystemMessage(SystemMessageId.LEFT_COMBAT_ZONE));

				// Set pvp flag
				if (((L2PcInstance) character).getPvpFlag() == 0)
					((L2PcInstance) character).startPvPFlag();
			}
		}
		if (character instanceof L2SiegeSummonInstance)
		{
			((L2SiegeSummonInstance) character).unSummon(((L2SiegeSummonInstance) character).getOwner());
		}
		if (character instanceof L2PcInstance)
		{
			L2PcInstance activeChar = ((L2PcInstance)character);
			
			activeChar.stopFameTask();
			activeChar.setIsInSiege(false);
			
			if (activeChar.getInventory().getItemByItemId(9819) != null)
			{
				Fort fort = FortManager.getInstance().getFort(activeChar);
				if (fort != null)
				{
					FortSiegeManager.getInstance().dropCombatFlag(activeChar);
				}
				else
				{
					int slot = activeChar.getInventory().getSlotFromItem(activeChar.getInventory().getItemByItemId(9819));
					activeChar.getInventory().unEquipItemInBodySlotAndRecord(slot);
					activeChar.destroyItem("CombatFlag", activeChar.getInventory().getItemByItemId(9819), null, true);
				}
			}
		}
	}
	
	@Override
	public void onDieInside(L2Character character)
	{
		if (getFort() != null && _isActiveSiege)
		{
			// debuff participants only if they die inside siege zone
			if (character instanceof L2PcInstance && ((L2PcInstance)character).isRegisteredOnThisSiegeField(getFort().getFortId()))
			{
				int lvl = 1;
				for (L2Effect effect: character.getAllEffects())
				{
					if (effect != null && effect.getSkill().getId() == 5660)
					{
						lvl = lvl+effect.getLevel();
						if (lvl > 5)
							lvl = 5;
						break;
					}
				}
				L2Skill skill = SkillTable.getInstance().getInfo(5660, lvl);
				if (skill != null)
					skill.getEffects(character, character);
			}
		}
	}

	@Override
	public void onReviveInside(L2Character character) {}

	public void updateZoneStatusForCharactersInside()
	{
		if (getFort() != null && _isActiveSiege)
		{
			for (L2Character character : _characterList.values())
			{
				try
				{
					onEnter(character);
				}
				catch (NullPointerException e)
				{
					e.printStackTrace();
				}
			}
		}
		else
		{
			for (L2Character character : _characterList.values())
			{
				try
				{
					character.setInsideZone(L2Character.ZONE_PVP, false);
					character.setInsideZone(L2Character.ZONE_SIEGE, false);
					character.setInsideZone(L2Character.ZONE_NOSUMMONFRIEND, false);
					
					if (character instanceof L2PcInstance)
					{
						((L2PcInstance) character).sendPacket(new SystemMessage(SystemMessageId.LEFT_COMBAT_ZONE));
						((L2PcInstance) character).stopFameTask();
					}
					if (character instanceof L2SiegeSummonInstance)
					{
						((L2SiegeSummonInstance) character).unSummon(((L2SiegeSummonInstance) character).getOwner());
					}
				}
				catch (NullPointerException e)
				{
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * Removes all foreigners from the fort
	 * @param owningClan
	 */
	public void banishForeigners(L2Clan owningClan)
	{
		for (L2Character temp : _characterList.values())
		{
			if (!(temp instanceof L2PcInstance))
				continue;
			if (((L2PcInstance) temp).getClan() == owningClan)
				continue;
			
			((L2PcInstance) temp).teleToLocation(MapRegionTable.TeleportWhereType.Town); // TODO: shouldnt be town, its outside of fort
		}
	}
	
	/**
	 * Sends a message to all players in this zone
	 * @param message
	 */
	public void announceToPlayers(String message)
	{
		for (L2Character temp : _characterList.values())
		{
			if (temp instanceof L2PcInstance)
				((L2PcInstance) temp).sendMessage(message);
		}
	}
	
	/**
	 * Returns all players within this zone
	 * @return
	 */
	public FastList<L2PcInstance> getAllPlayers()
	{
		FastList<L2PcInstance> players = new FastList<L2PcInstance>();
		
		for (L2Character temp : _characterList.values())
		{
			if (temp instanceof L2PcInstance)
				players.add((L2PcInstance) temp);
		}
		
		return players;
	}
	
	public int getFortId()
	{
		return _fortId;
	}
	
	private final Fort getFort()
	{
		if (_fort == null)
			_fort = FortManager.getInstance().getFortById(_fortId);
		return _fort;
	}
	
	public boolean isActive()
	{
		return _isActiveSiege;
	}
	
	public void setIsActive(boolean val)
	{
		_isActiveSiege = val;
	}
	/**
	 * Get the forts defender spawn
	 * @return
	 */
	public int[] getSpawn()
	{
		return _spawnLoc;
	}
}
