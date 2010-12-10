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

import java.util.ArrayList;
import java.util.List;

import com.l2jserver.gameserver.ThreadPoolManager;
import com.l2jserver.gameserver.datatables.MapRegionTable;
import com.l2jserver.gameserver.model.L2Spawn;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.L2Playable;
import com.l2jserver.gameserver.model.actor.L2Summon;
import com.l2jserver.gameserver.model.actor.instance.L2DoorInstance;
import com.l2jserver.gameserver.model.actor.instance.L2OlympiadManagerInstance;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.olympiad.OlympiadGameTask;
import com.l2jserver.gameserver.model.zone.L2SpawnZone;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.ExOlympiadMatchEnd;
import com.l2jserver.gameserver.network.serverpackets.ExOlympiadUserInfo;
import com.l2jserver.gameserver.network.serverpackets.L2GameServerPacket;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;

/**
 * An olympiad stadium
 *
 * @author  durgus, DS
 */
public class L2OlympiadStadiumZone extends L2SpawnZone
{
	private final List<L2DoorInstance> _doors;
	private final List<L2Spawn> _buffers;

	OlympiadGameTask _task = null;

	public L2OlympiadStadiumZone(int id)
	{
		super(id);
		_buffers = new ArrayList<L2Spawn>(2);
		_doors = new ArrayList<L2DoorInstance>(2);
	}
	
	public final void registerTask(OlympiadGameTask task)
	{
		_task = task;
	}

	public final void openDoors()
	{
		for (L2DoorInstance door : _doors)
		{
			if (door != null && !door.getOpen())
				door.openMe();
		}
	}

	public final void closeDoors()
	{
		for (L2DoorInstance door : _doors)
		{
			if (door != null && door.getOpen())
				door.closeMe();
		}
	}

	public final void spawnBuffers()
	{
		for (L2Spawn spawn : _buffers)
		{
			spawn.startRespawn();
			spawn.respawnNpc(spawn.getLastSpawn());
			spawn.stopRespawn();
		}
	}

	public final void deleteBuffers()
	{
		for (L2Spawn spawn : _buffers)
		{
			if (spawn.getLastSpawn().isVisible())
				spawn.getLastSpawn().deleteMe();
		}
	}

	public final void broadcastStatusUpdate(L2PcInstance player)
	{
		final ExOlympiadUserInfo packet = new ExOlympiadUserInfo(player);
		for (L2Character character : _characterList.values())
		{
			if (character instanceof L2PcInstance)
			{
				if (((L2PcInstance)character).inObserverMode()
						|| ((L2PcInstance)character).getOlympiadSide() != player.getOlympiadSide())
					character.sendPacket(packet);
			}
		}
	}

	public final void broadcastPacketToObservers(L2GameServerPacket packet)
	{
		for (L2Character character : _characterList.values())
		{
			if (character instanceof L2PcInstance
					&& ((L2PcInstance)character).inObserverMode())
				character.sendPacket(packet);
		}
	}

	@Override
	protected final void onEnter(L2Character character)
	{
		character.setInsideZone(L2Character.ZONE_NOSUMMONFRIEND, true);

		if (_task != null)
		{
			if (_task.isBattleStarted())
			{
				character.setInsideZone(L2Character.ZONE_PVP, true);
				if (character instanceof L2PcInstance)
				{
					character.sendPacket(new SystemMessage(SystemMessageId.ENTERED_COMBAT_ZONE));
					_task.getGame().sendOlympiadInfo(character);
				}
			}
		}

		if (character instanceof L2Playable)
		{
			final L2PcInstance player = character.getActingPlayer();
			if (player != null)
			{
				// only participants, observers and GMs allowed
				if (!player.isGM() && !player.isInOlympiadMode() && !player.inObserverMode())
					ThreadPoolManager.getInstance().executeTask(new KickPlayer(player));
			}
		}
		else if (character instanceof L2OlympiadManagerInstance)
		{
			final L2Spawn spawn = ((L2OlympiadManagerInstance) character).getSpawn();
			if (spawn != null && !_buffers.contains(spawn))
			{
				_buffers.add(spawn);
				spawn.stopRespawn();
				character.deleteMe();
			}
		}
		else if (character instanceof L2DoorInstance)
		{
			for (L2DoorInstance door: _doors)
			{
				if (door.getDoorId() == ((L2DoorInstance)character).getDoorId())
				{
					_doors.remove(door);
					break;
				}
			}
			_doors.add((L2DoorInstance)character);
		}
	}

	@Override
	protected final void onExit(L2Character character)
	{
		character.setInsideZone(L2Character.ZONE_NOSUMMONFRIEND, false);

		if (_task != null)
		{
			if (_task.isBattleStarted())
			{
				character.setInsideZone(L2Character.ZONE_PVP, false);
				if (character instanceof L2PcInstance)
				{
					character.sendPacket(new SystemMessage(SystemMessageId.LEFT_COMBAT_ZONE));
					character.sendPacket(ExOlympiadMatchEnd.STATIC_PACKET);
				}
			}
		}

		if (character instanceof L2DoorInstance)
			_doors.remove(character);
	}
	
	public final void updateZoneStatusForCharactersInside()
	{
		if (_task == null)
			return;

		final boolean battleStarted = _task.isBattleStarted();
		final SystemMessage sm;
		if (battleStarted)
			sm = new SystemMessage(SystemMessageId.ENTERED_COMBAT_ZONE);
		else
			sm = new SystemMessage(SystemMessageId.LEFT_COMBAT_ZONE);

		for (L2Character character : _characterList.values())
		{
			if (character == null)
				continue;

			if (battleStarted)
			{
				character.setInsideZone(L2Character.ZONE_PVP, true);
				if (character instanceof L2PcInstance)
					character.sendPacket(sm);
			}
			else
			{
				character.setInsideZone(L2Character.ZONE_PVP, false);
				if (character instanceof L2PcInstance)
				{
					character.sendPacket(sm);
					character.sendPacket(ExOlympiadMatchEnd.STATIC_PACKET);
				}
			}
		}
	}

	@Override
	public void onDieInside(L2Character character)
	{
	}
	
	@Override
	public void onReviveInside(L2Character character)
	{
	}
	
	private static final class KickPlayer implements Runnable
	{
		private L2PcInstance _player;

		public KickPlayer(L2PcInstance player)
		{
			_player = player;
		}

		public void run()
		{
			if (_player != null)
			{
				final L2Summon summon = _player.getPet();
				if (summon != null)
					summon.unSummon(_player);

				_player.teleToLocation(MapRegionTable.TeleportWhereType.Town);
				_player = null;
			}
		}
	}
}