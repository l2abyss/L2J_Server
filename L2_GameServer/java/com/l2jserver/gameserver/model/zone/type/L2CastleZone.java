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
import com.l2jserver.gameserver.instancemanager.CastleManager;
import com.l2jserver.gameserver.model.L2Effect;
import com.l2jserver.gameserver.model.L2Skill;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.actor.instance.L2SiegeSummonInstance;
import com.l2jserver.gameserver.model.entity.Castle;
import com.l2jserver.gameserver.model.zone.L2ZoneType;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;

import javolution.util.FastList;

/**
 * A castle zone
 *
 * @author  durgus
 */
public class L2CastleZone extends L2ZoneType
{
	private int _castleId;
	private Castle _castle = null;
	private int[] _spawnLoc;
	private boolean _isActiveSiege = false;
	
	public L2CastleZone(int id)
	{
		super(id);
		
		_spawnLoc = new int[3];
	}
	
	@Override
	public void setParameter(String name, String value)
	{
		if (name.equals("castleId"))
		{
			_castleId = Integer.parseInt(value);
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
		if (getCastle() != null)
		{
			if (_isActiveSiege)
			{
				character.setInsideZone(L2Character.ZONE_PVP, true);
				character.setInsideZone(L2Character.ZONE_SIEGE, true);
				character.setInsideZone(L2Character.ZONE_NOSUMMONFRIEND, true);
				
				if (character instanceof L2PcInstance)
				{
					if (((L2PcInstance) character).isRegisteredOnThisSiegeField(getCastle().getCastleId()))
					{
						((L2PcInstance) character).setIsInSiege(true);
						((L2PcInstance) character).startFameTask(Config.CASTLE_ZONE_FAME_TASK_FREQUENCY * 1000, Config.CASTLE_ZONE_FAME_AQUIRE_POINTS);
					}
					((L2PcInstance) character).sendPacket(new SystemMessage(SystemMessageId.ENTERED_COMBAT_ZONE));
				}
			}
			character.setInsideZone(L2Character.ZONE_CASTLE, true);
		}
	}
	
	@Override
	protected void onExit(L2Character character)
	{
		if (getCastle() != null)
		{
			if (_isActiveSiege)
			{
				
				if (character instanceof L2PcInstance)
				{
					((L2PcInstance) character).sendPacket(new SystemMessage(SystemMessageId.LEFT_COMBAT_ZONE));
					// Set pvp flag
					if (((L2PcInstance) character).getPvpFlag() == 0)
						((L2PcInstance) character).startPvPFlag();
				}
			}
			if (character instanceof L2PcInstance)
			{
				((L2PcInstance) character).stopFameTask();
				((L2PcInstance) character).setIsInSiege(false);
			}
			character.setInsideZone(L2Character.ZONE_PVP, false);
			character.setInsideZone(L2Character.ZONE_SIEGE, false);
			character.setInsideZone(L2Character.ZONE_CASTLE, false);
			character.setInsideZone(L2Character.ZONE_NOSUMMONFRIEND, false);
		}
		if (character instanceof L2SiegeSummonInstance)
		{
			((L2SiegeSummonInstance) character).unSummon(((L2SiegeSummonInstance) character).getOwner());
		}
	}
	
	@Override
	public void onDieInside(L2Character character)
	{
		if (getCastle() != null && _isActiveSiege)
		{
			// debuff participants only if they die inside siege zone
			if (character instanceof L2PcInstance && ((L2PcInstance) character).isRegisteredOnThisSiegeField(getCastle().getCastleId()))
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
				L2Skill skill;
				skill = SkillTable.getInstance().getInfo(5660, lvl);
				if (skill != null)
					skill.getEffects(character, character);
			}
		}
	}
	
	@Override
	public void onReviveInside(L2Character character)
	{
	}
	
	public void updateZoneStatusForCharactersInside()
	{
		if (getCastle() != null && _isActiveSiege)
		{
			for (L2Character character : _characterList.values())
			{
				try
				{
					onEnter(character);
				}
				catch (NullPointerException e)
				{
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
				}
			}
		}
	}
	
	/**
	 * Removes all foreigners from the castle
	 * @param owningClanId
	 */
	public void banishForeigners(int owningClanId)
	{
		for (L2Character temp : _characterList.values())
		{
			if (!(temp instanceof L2PcInstance))
				continue;
			if (((L2PcInstance) temp).getClanId() == owningClanId)
				continue;
			
			((L2PcInstance) temp).teleToLocation(MapRegionTable.TeleportWhereType.Town);
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
	
	public int getCastleId()
	{
		return _castleId;
	}
	
	public boolean isActive()
	{
		return _isActiveSiege;
	}
	
	public void setIsActive(boolean val)
	{
		_isActiveSiege = val;
	}
	
	private final Castle getCastle()
	{
		if (_castle == null)
			_castle = CastleManager.getInstance().getCastleById(_castleId);
		return _castle;
	}
	
	/**
	 * Get the castles defender spawn
	 * @return
	 */
	public int[] getSpawn()
	{
		return _spawnLoc;
	}
}
