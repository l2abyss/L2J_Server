/*
 * Copyright (C) 2004-2013 L2J Server
 * 
 * This file is part of L2J Server.
 * 
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.l2jserver.gameserver.model;

import java.util.Map;

import javolution.util.FastMap;

import com.l2jserver.gameserver.enums.InstanceType;
import com.l2jserver.gameserver.enums.ShotType;
import com.l2jserver.gameserver.handler.ActionHandler;
import com.l2jserver.gameserver.handler.ActionShiftHandler;
import com.l2jserver.gameserver.handler.IActionHandler;
import com.l2jserver.gameserver.idfactory.IdFactory;
import com.l2jserver.gameserver.instancemanager.InstanceManager;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.actor.knownlist.ObjectKnownList;
import com.l2jserver.gameserver.model.actor.poly.ObjectPoly;
import com.l2jserver.gameserver.model.actor.position.ObjectPosition;
import com.l2jserver.gameserver.model.entity.Instance;
import com.l2jserver.gameserver.model.interfaces.IIdentifiable;
import com.l2jserver.gameserver.model.interfaces.INamable;
import com.l2jserver.gameserver.model.interfaces.ISpawnable;
import com.l2jserver.gameserver.model.zone.ZoneId;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.ActionFailed;
import com.l2jserver.gameserver.network.serverpackets.ExSendUIEvent;
import com.l2jserver.gameserver.network.serverpackets.L2GameServerPacket;

/**
 * Mother class of all objects in the world which ones is it possible to interact (PC, NPC, Item...)<BR>
 * <BR>
 * L2Object :<BR>
 * <BR>
 * <li>L2Character</li> <li>L2ItemInstance</li>
 */
public abstract class L2Object extends ObjectPosition implements IIdentifiable, INamable, ISpawnable
{
	private boolean _isVisible; // Object visibility
	private ObjectKnownList _knownList;
	private String _name;
	private int _objectId; // Object identifier
	private ObjectPoly _poly;
	
	private InstanceType _instanceType = null;
	private volatile Map<String, Object> _scripts;
	
	public L2Object(int objectId)
	{
		super();
		setInstanceType(InstanceType.L2Object);
		_objectId = objectId;
		initKnownList();
		setActiveObject(this);
	}
	
	protected final void setInstanceType(InstanceType i)
	{
		_instanceType = i;
	}
	
	public final InstanceType getInstanceType()
	{
		return _instanceType;
	}
	
	public final boolean isInstanceType(InstanceType i)
	{
		return _instanceType.isType(i);
	}
	
	public final boolean isInstanceTypes(InstanceType... i)
	{
		return _instanceType.isTypes(i);
	}
	
	public final void onAction(L2PcInstance player)
	{
		onAction(player, true);
	}
	
	public void onAction(L2PcInstance player, boolean interact)
	{
		IActionHandler handler = ActionHandler.getInstance().getHandler(getInstanceType());
		if (handler != null)
		{
			handler.action(player, this, interact);
		}
		
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	public void onActionShift(L2PcInstance player)
	{
		IActionHandler handler = ActionShiftHandler.getInstance().getHandler(getInstanceType());
		if (handler != null)
		{
			handler.action(player, this, true);
		}
		
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	public void onForcedAttack(L2PcInstance player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	/**
	 * Do Nothing.<BR>
	 * <BR>
	 * <B><U> Overridden in </U> :</B><BR>
	 * <BR>
	 * <li>L2GuardInstance : Set the home location of its L2GuardInstance</li> <li>L2Attackable : Reset the Spoiled flag</li><BR>
	 * <BR>
	 */
	public void onSpawn()
	{
	}
	
	/**
	 * UnAfraid: TODO: Add listener here.
	 * @param instanceId The id of the instance zone the object is in - id 0 is global
	 */
	@Override
	public void setInstanceId(int instanceId)
	{
		if ((instanceId < 0) || (getInstanceId() == instanceId))
		{
			return;
		}
		
		Instance oldI = InstanceManager.getInstance().getInstance(getInstanceId());
		Instance newI = InstanceManager.getInstance().getInstance(instanceId);
		
		if (newI == null)
		{
			return;
		}
		
		if (isPlayer())
		{
			L2PcInstance player = getActingPlayer();
			if ((getInstanceId() > 0) && (oldI != null))
			{
				oldI.removePlayer(getObjectId());
				if (oldI.isShowTimer())
				{
					int startTime = (int) ((System.currentTimeMillis() - oldI.getInstanceStartTime()) / 1000);
					int endTime = (int) ((oldI.getInstanceEndTime() - oldI.getInstanceStartTime()) / 1000);
					if (oldI.isTimerIncrease())
					{
						sendPacket(new ExSendUIEvent(getActingPlayer(), true, true, startTime, endTime, oldI.getTimerText()));
					}
					else
					{
						sendPacket(new ExSendUIEvent(getActingPlayer(), true, false, endTime - startTime, 0, oldI.getTimerText()));
					}
				}
			}
			if (instanceId > 0)
			{
				newI.addPlayer(getObjectId());
				if (newI.isShowTimer())
				{
					int startTime = (int) ((System.currentTimeMillis() - newI.getInstanceStartTime()) / 1000);
					int endTime = (int) ((newI.getInstanceEndTime() - newI.getInstanceStartTime()) / 1000);
					if (newI.isTimerIncrease())
					{
						sendPacket(new ExSendUIEvent(getActingPlayer(), false, true, startTime, endTime, newI.getTimerText()));
					}
					else
					{
						sendPacket(new ExSendUIEvent(getActingPlayer(), false, false, endTime - startTime, 0, newI.getTimerText()));
					}
				}
			}
			
			if (player.hasSummon())
			{
				player.getSummon().setInstanceId(instanceId);
			}
		}
		else if (isNpc())
		{
			L2Npc npc = (L2Npc) this;
			if ((getInstanceId() > 0) && (oldI != null))
			{
				oldI.removeNpc(npc);
			}
			if (instanceId > 0)
			{
				newI.addNpc(npc);
			}
		}
		
		super.setInstanceId(instanceId);
		
		// If we change it for visible objects, me must clear & revalidates knownlists
		if (_isVisible && (_knownList != null))
		{
			if (isPlayer())
			{
				// We don't want some ugly looking disappear/appear effects, so don't update
				// the knownlist here, but players usually enter instancezones through teleporting
				// and the teleport will do the revalidation for us.
			}
			else
			{
				decayMe();
				spawnMe();
			}
		}
	}
	
	/**
	 * Remove a L2Object from the world.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Remove the L2Object from the world</li><BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T REMOVE the object from _allObjects of L2World </B></FONT><BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T SEND Server->Client packets to players</B></FONT><BR>
	 * <BR>
	 * <B><U> Assert </U> :</B><BR>
	 * <BR>
	 * <li>_worldRegion != null <I>(L2Object is visible at the beginning)</I></li><BR>
	 * <BR>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li>Delete NPC/PC or Unsummon</li><BR>
	 * <BR>
	 */
	public void decayMe()
	{
		assert getWorldRegion() != null;
		
		L2WorldRegion reg = getWorldRegion();
		
		synchronized (this)
		{
			_isVisible = false;
			setWorldRegion(null);
		}
		
		// this can synchronize on others instances, so it's out of
		// synchronized, to avoid deadlocks
		// Remove the L2Object from the world
		L2World.getInstance().removeVisibleObject(this, reg);
		L2World.getInstance().removeObject(this);
	}
	
	public void refreshID()
	{
		L2World.getInstance().removeObject(this);
		IdFactory.getInstance().releaseId(getObjectId());
		_objectId = IdFactory.getInstance().getNextId();
	}
	
	/**
	 * Init the position of a L2Object spawn and add it in the world as a visible object.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Set the x,y,z position of the L2Object spawn and update its _worldregion</li> <li>Add the L2Object spawn in the _allobjects of L2World</li> <li>Add the L2Object spawn to _visibleObjects of its L2WorldRegion</li> <li>Add the L2Object spawn in the world as a <B>visible</B> object</li><BR>
	 * <BR>
	 * <B><U> Assert </U> :</B><BR>
	 * <BR>
	 * <li>_worldRegion == null <I>(L2Object is invisible at the beginning)</I></li><BR>
	 * <BR>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li>Create Door</li> <li>Spawn : Monster, Minion, CTs, Summon...</li><BR>
	 */
	@Override
	public final boolean spawnMe()
	{
		assert (getWorldRegion() == null) && (getWorldPosition().getX() != 0) && (getWorldPosition().getY() != 0) && (getWorldPosition().getZ() != 0);
		
		synchronized (this)
		{
			// Set the x,y,z position of the L2Object spawn and update its _worldregion
			_isVisible = true;
			setWorldRegion(L2World.getInstance().getRegion(getWorldPosition()));
			
			// Add the L2Object spawn in the _allobjects of L2World
			L2World.getInstance().storeObject(this);
			
			// Add the L2Object spawn to _visibleObjects and if necessary to _allplayers of its L2WorldRegion
			getWorldRegion().addVisibleObject(this);
		}
		
		// this can synchronize on others instances, so it's out of synchronized, to avoid deadlocks
		// Add the L2Object spawn in the world as a visible object
		L2World.getInstance().addVisibleObject(this, getWorldRegion());
		
		onSpawn();
		
		return true;
	}
	
	public final void spawnMe(int x, int y, int z)
	{
		assert getWorldRegion() == null;
		
		synchronized (this)
		{
			// Set the x,y,z position of the L2Object spawn and update its _worldregion
			_isVisible = true;
			
			if (x > L2World.MAP_MAX_X)
			{
				x = L2World.MAP_MAX_X - 5000;
			}
			if (x < L2World.MAP_MIN_X)
			{
				x = L2World.MAP_MIN_X + 5000;
			}
			if (y > L2World.MAP_MAX_Y)
			{
				y = L2World.MAP_MAX_Y - 5000;
			}
			if (y < L2World.MAP_MIN_Y)
			{
				y = L2World.MAP_MIN_Y + 5000;
			}
			
			setXYZ(x, y, z);
			setWorldRegion(L2World.getInstance().getRegion(getWorldPosition()));
			
			// Add the L2Object spawn in the _allobjects of L2World
		}
		
		L2World.getInstance().storeObject(this);
		
		// these can synchronize on others instances, so they're out of
		// synchronized, to avoid deadlocks
		
		// Add the L2Object spawn to _visibleObjects and if necessary to _allplayers of its L2WorldRegion
		getWorldRegion().addVisibleObject(this);
		
		// Add the L2Object spawn in the world as a visible object
		L2World.getInstance().addVisibleObject(this, getWorldRegion());
		
		onSpawn();
	}
	
	public void toggleVisible()
	{
		if (isVisible())
		{
			decayMe();
		}
		else
		{
			spawnMe();
		}
	}
	
	public boolean isAttackable()
	{
		return false;
	}
	
	public abstract boolean isAutoAttackable(L2Character attacker);
	
	/**
	 * Return the visibility state of the L2Object. <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * A L2Object is visible if <B>__IsVisible</B>=true and <B>_worldregion</B>!=null <BR>
	 * <BR>
	 * @return
	 */
	public final boolean isVisible()
	{
		return getWorldRegion() != null;
	}
	
	public final void setIsVisible(boolean value)
	{
		_isVisible = value;
		if (!_isVisible)
		{
			setWorldRegion(null);
		}
	}
	
	public ObjectKnownList getKnownList()
	{
		return _knownList;
	}
	
	/**
	 * Initializes the KnownList of the L2Object, is overwritten in classes that require a different knownlist Type. Removes the need for instanceof checks.
	 */
	public void initKnownList()
	{
		_knownList = new ObjectKnownList(this);
	}
	
	public final void setKnownList(ObjectKnownList value)
	{
		_knownList = value;
	}
	
	@Override
	public final String getName()
	{
		return _name;
	}
	
	public void setName(String value)
	{
		_name = value;
	}
	
	public final int getObjectId()
	{
		return _objectId;
	}
	
	public final ObjectPoly getPoly()
	{
		if (_poly == null)
		{
			_poly = new ObjectPoly(this);
		}
		return _poly;
	}
	
	public L2PcInstance getActingPlayer()
	{
		return null;
	}
	
	/**
	 * Sends the Server->Client info packet for the object. <br>
	 * Is Overridden in:
	 * <ul>
	 * <li>L2AirShipInstance</li>
	 * <li>L2BoatInstance</li>
	 * <li>L2DoorInstance</li>
	 * <li>L2PcInstance</li>
	 * <li>L2StaticObjectInstance</li>
	 * <li>L2Decoy</li>
	 * <li>L2Npc</li>
	 * <li>L2Summon</li>
	 * <li>L2Trap</li>
	 * <li>L2ItemInstance</li>
	 * </ul>
	 * @param activeChar
	 */
	public void sendInfo(L2PcInstance activeChar)
	{
		
	}
	
	/**
	 * Not Implemented.<BR>
	 * <BR>
	 * <B><U> Overridden in </U> :</B><BR>
	 * <BR>
	 * <li>L2PcInstance</li><BR>
	 * <BR>
	 * @param mov
	 */
	public void sendPacket(L2GameServerPacket mov)
	{
		// default implementation
	}
	
	/**
	 * Not Implemented.<BR>
	 * <BR>
	 * <B><U> Overridden in </U> :</B><BR>
	 * <BR>
	 * <li>L2PcInstance</li><BR>
	 * <BR>
	 * @param id
	 */
	public void sendPacket(SystemMessageId id)
	{
		// default implementation
	}
	
	/**
	 * @return {@code true} if object is instance of L2PcInstance
	 */
	public boolean isPlayer()
	{
		return false;
	}
	
	/**
	 * @return {@code true} if object is instance of L2Playable
	 */
	public boolean isPlayable()
	{
		return false;
	}
	
	/**
	 * @return {@code true} if object is instance of L2Summon
	 */
	public boolean isSummon()
	{
		return false;
	}
	
	/**
	 * @return {@code true} if object is instance of L2PetInstance
	 */
	public boolean isPet()
	{
		return false;
	}
	
	/**
	 * @return {@code true} if object is instance of L2ServitorInstance
	 */
	public boolean isServitor()
	{
		return false;
	}
	
	/**
	 * @return {@code true} if object is instance of L2Attackable
	 */
	public boolean isCharacter()
	{
		return false;
	}
	
	/**
	 * @return {@code true} if object is instance of L2DoorInstance
	 */
	public boolean isDoor()
	{
		return false;
	}
	
	/**
	 * @return {@code true} if object is instance of L2Npc
	 */
	public boolean isNpc()
	{
		return false;
	}
	
	/**
	 * @return {@code true} if object is instance of L2Attackable
	 */
	public boolean isL2Attackable()
	{
		return false;
	}
	
	/**
	 * @return {@code true} if object is instance of L2MonsterInstance
	 */
	public boolean isMonster()
	{
		return false;
	}
	
	/**
	 * @return {@code true} if object is instance of L2TrapInstance
	 */
	public boolean isTrap()
	{
		return false;
	}
	
	/**
	 * @return {@code true} if object is instance of L2ItemInstance
	 */
	public boolean isItem()
	{
		return false;
	}
	
	/**
	 * @return {@code true} if object Npc Walker or Vehicle
	 */
	public boolean isWalker()
	{
		return false;
	}
	
	/**
	 * @return {@code true} if object Can be targeted
	 */
	public boolean isTargetable()
	{
		return true;
	}
	
	/**
	 * Check if the object is in the given zone Id.
	 * @param zone the zone Id to check
	 * @return {@code true} if the object is in that zone Id
	 */
	public boolean isInsideZone(ZoneId zone)
	{
		return false;
	}
	
	/**
	 * Check if current object has charged shot.
	 * @param type of the shot to be checked.
	 * @return {@code true} if the object has charged shot
	 */
	public boolean isChargedShot(ShotType type)
	{
		return false;
	}
	
	/**
	 * Charging shot into the current object.
	 * @param type of the shot to be charged.
	 * @param charged
	 */
	public void setChargedShot(ShotType type, boolean charged)
	{
	}
	
	/**
	 * Try to recharge a shot.
	 * @param physical skill are using Soul shots.
	 * @param magical skill are using Spirit shots.
	 */
	public void rechargeShots(boolean physical, boolean magical)
	{
	}
	
	/**
	 * @param <T>
	 * @param script
	 * @return
	 */
	public final <T> T addScript(T script)
	{
		if (_scripts == null)
		{
			// Double-checked locking
			synchronized (this)
			{
				if (_scripts == null)
				{
					_scripts = new FastMap<String, Object>().shared();
				}
			}
		}
		_scripts.put(script.getClass().getName(), script);
		return script;
	}
	
	/**
	 * @param <T>
	 * @param script
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public final <T> T removeScript(Class<T> script)
	{
		if (_scripts == null)
		{
			return null;
		}
		return (T) _scripts.remove(script.getName());
	}
	
	/**
	 * @param <T>
	 * @param script
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public final <T> T getScript(Class<T> script)
	{
		if (_scripts == null)
		{
			return null;
		}
		return (T) _scripts.get(script.getName());
	}
	
	public void removeStatusListener(L2Character object)
	{
		
	}
	
	@Override
	public String toString()
	{
		return (getClass().getSimpleName() + ":" + getName() + "[" + getObjectId() + "]");
	}
}