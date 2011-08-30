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

import java.util.Collection;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastList;

import com.l2jserver.Config;
import com.l2jserver.gameserver.ThreadPoolManager;
import com.l2jserver.gameserver.ai.L2CharacterAI;
import com.l2jserver.gameserver.ai.L2DoorAI;
import com.l2jserver.gameserver.instancemanager.CastleManager;
import com.l2jserver.gameserver.instancemanager.FortManager;
import com.l2jserver.gameserver.instancemanager.TerritoryWarManager;
import com.l2jserver.gameserver.model.L2CharPosition;
import com.l2jserver.gameserver.model.L2Clan;
import com.l2jserver.gameserver.model.L2ItemInstance;
import com.l2jserver.gameserver.model.L2Object;
import com.l2jserver.gameserver.model.L2Skill;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.L2Playable;
import com.l2jserver.gameserver.model.actor.knownlist.DoorKnownList;
import com.l2jserver.gameserver.model.actor.stat.DoorStat;
import com.l2jserver.gameserver.model.actor.status.DoorStatus;
import com.l2jserver.gameserver.model.entity.Castle;
import com.l2jserver.gameserver.model.entity.ClanHall;
import com.l2jserver.gameserver.model.entity.Fort;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.DoorStatusUpdate;
import com.l2jserver.gameserver.network.serverpackets.OnEventTrigger;
import com.l2jserver.gameserver.network.serverpackets.StaticObject;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;
import com.l2jserver.gameserver.templates.chars.L2CharTemplate;
import com.l2jserver.gameserver.templates.item.L2Weapon;

/**
 * This class ...
 *
 * @version $Revision: 1.3.2.2.2.5 $ $Date: 2005/03/27 15:29:32 $
 */
public class L2DoorInstance extends L2Character
{
	protected static final Logger log = Logger.getLogger(L2DoorInstance.class.getName());
	
	/** The castle index in the array of L2Castle this L2NpcInstance belongs to */
	private int _castleIndex = -2;
	private int _mapRegion = -1;
	/** The fort index in the array of L2Fort this L2NpcInstance belongs to */
	private int _fortIndex = -2;
	
	// when door is closed, the dimensions are
	private int _rangeXMin = 0;
	private int _rangeYMin = 0;
	private int _rangeZMin = 0;
	private int _rangeXMax = 0;
	private int _rangeYMax = 0;
	private int _rangeZMax = 0;
	
	// these variables assist in see-through calculation only
	private int _A = 0;
	private int _B = 0;
	private int _C = 0;
	private int _D = 0;
	
	protected final int _doorId;
	protected final String _name;
	private boolean _open;
	private boolean _isCommanderDoor;
	private final boolean _unlockable;
	private boolean _isAttackableDoor = false;
	private boolean _isWall = false; // is castle wall ?
	private boolean _ShowHp = false;
	private int _meshindex = 1;
	private int _emitter = 0;
	private boolean _targetable = true;
	
	private ClanHall _clanHall;
	
	protected int _autoActionDelay = -1;
	private ScheduledFuture<?> _autoActionTask;
	
	/** This class may be created only by L2Character and only for AI */
	public class AIAccessor extends L2Character.AIAccessor
	{
		protected AIAccessor()
		{
		}
		
		@Override
		public L2DoorInstance getActor()
		{
			return L2DoorInstance.this;
		}
		
		@Override
		public void moveTo(int x, int y, int z, int offset)
		{
		}
		
		@Override
		public void moveTo(int x, int y, int z)
		{
		}
		
		@Override
		public void stopMove(L2CharPosition pos)
		{
		}
		
		@Override
		public void doAttack(L2Character target)
		{
		}
		
		@Override
		public void doCast(L2Skill skill)
		{
		}
	}
	
	@Override
	public L2CharacterAI getAI()
	{
		L2CharacterAI ai = _ai; // copy handle
		if (ai == null)
		{
			synchronized (this)
			{
				if (_ai == null)
					_ai = new L2DoorAI(new AIAccessor());
				return _ai;
			}
		}
		return ai;
	}
	
	class CloseTask implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				onClose();
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "", e);
			}
		}
	}
	
	/**
	 * Manages the auto open and closing of a door.
	 */
	class AutoOpenClose implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				String doorAction;
				
				if (!getOpen())
				{
					doorAction = "opened";
					openMe();
				}
				else
				{
					doorAction = "closed";
					closeMe();
				}
				
				if (Config.DEBUG)
					_log.info("Auto " + doorAction + " door ID " + _doorId + " (" + _name + ") for " + (_autoActionDelay / 60000) + " minute(s).");
			}
			catch (Exception e)
			{
				_log.warning("Could not auto open/close door ID " + _doorId + " (" + _name + ")");
			}
		}
	}
	
	/**
	 */
	public L2DoorInstance(int objectId, L2CharTemplate template, int doorId, String name, boolean unlockable)
	{
		super(objectId, template);
		setInstanceType(InstanceType.L2DoorInstance);
		setIsInvul(false);
		_doorId = doorId;
		_name = name;
		_unlockable = unlockable;
	}
	
	@Override
	public final DoorKnownList getKnownList()
	{
		return (DoorKnownList) super.getKnownList();
	}
	
	@Override
	public void initKnownList()
	{
		setKnownList(new DoorKnownList(this));
	}
	
	@Override
	public final DoorStat getStat()
	{
		return (DoorStat) super.getStat();
	}
	
	@Override
	public void initCharStat()
	{
		setStat(new DoorStat(this));
	}
	
	@Override
	public final DoorStatus getStatus()
	{
		return (DoorStatus) super.getStatus();
	}
	
	@Override
	public void initCharStatus()
	{
		setStatus(new DoorStatus(this));
	}
	
	public final boolean isUnlockable()
	{
		return _unlockable;
	}
	
	@Override
	public final int getLevel()
	{
		return 1;
	}
	
	/**
	 * @return Returns the doorId.
	 */
	public int getDoorId()
	{
		return _doorId;
	}
	
	/**
	 * @return Returns the open.
	 */
	public boolean getOpen()
	{
		return _open;
	}
	
	/**
	 * @param open The open to set.
	 */
	public void setOpen(boolean open)
	{
		_open = open;
	}
	
	/**
	 * @param val Used for Fortresses to determine if doors can be attacked during siege or not
	 */
	public void setIsCommanderDoor(boolean val)
	{
		_isCommanderDoor = val;
	}
	
	/**
	 * @return Doors that cannot be attacked during siege
	 * these doors will be auto opened if u take control of all commanders buildings
	 */
	public boolean getIsCommanderDoor()
	{
		return _isCommanderDoor;
	}
	
	public boolean getIsAttackableDoor()
	{
		return _isAttackableDoor;
	}
	
	public boolean getIsShowHp()
	{
		return _ShowHp;
	}
	
	public void setIsAttackableDoor(boolean val)
	{
		_isAttackableDoor = val;
	}
	
	public void setIsShowHp(boolean val)
	{
		_ShowHp = val;
	}
	
	/**
	 * Sets the delay in milliseconds for automatic opening/closing
	 * of this door instance.
	 * <BR>
	 * <B>Note:</B> A value of -1 cancels the auto open/close task.
	 *
	 * @param int actionDelay
	 */
	public void setAutoActionDelay(int actionDelay)
	{
		if (_autoActionDelay == actionDelay)
			return;
		
		if (actionDelay > -1)
		{
			AutoOpenClose ao = new AutoOpenClose();
			ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(ao, actionDelay, actionDelay);
		}
		else
		{
			if (_autoActionTask != null)
				_autoActionTask.cancel(false);
		}
		
		_autoActionDelay = actionDelay;
	}
	
	public int getDamage()
	{
		int dmg = 6 - (int) Math.ceil(getCurrentHp() / getMaxHp() * 6);
		if (dmg > 6)
			return 6;
		if (dmg < 0)
			return 0;
		return dmg;
	}
	
	public final Castle getCastle()
	{
		if (_castleIndex < 0)
			_castleIndex = CastleManager.getInstance().getCastleIndex(this);
		if (_castleIndex < 0)
			return null;
		return CastleManager.getInstance().getCastles().get(_castleIndex);
	}
	
	public final Fort getFort()
	{
		if (_fortIndex < 0)
			_fortIndex = FortManager.getInstance().getFortIndex(this);
		if (_fortIndex < 0)
			return null;
		return FortManager.getInstance().getForts().get(_fortIndex);
	}
	
	public void setClanHall(ClanHall clanhall)
	{
		_clanHall = clanhall;
	}
	
	public ClanHall getClanHall()
	{
		return _clanHall;
	}
	
	public boolean isEnemy()
	{
		if (getCastle() != null && getCastle().getCastleId() > 0 && getCastle().getZone().isActive())
			return true;
		if (getFort() != null && getFort().getFortId() > 0 && getFort().getZone().isActive() && !getIsCommanderDoor())
			return true;
		return false;
	}
	
	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		if (isUnlockable() && getFort() == null)
			return true;
		
		// Doors can`t be attacked by NPCs
		if (!(attacker instanceof L2Playable))
			return false;
		
		if (getClanHall() != null)
			return false;
		
		// Attackable  only during siege by everyone (not owner)
		boolean isCastle = (getCastle() != null && getCastle().getCastleId() > 0 && getCastle().getZone().isActive());
		boolean isFort = (getFort() != null && getFort().getFortId() > 0 && getFort().getZone().isActive() && !getIsCommanderDoor());
		int activeSiegeId = (getFort() != null ? getFort().getFortId() : (getCastle() != null ? getCastle().getCastleId() : 0));
		L2PcInstance actingPlayer = attacker.getActingPlayer();
		
		if (TerritoryWarManager.getInstance().isTWInProgress())
		{
			if (TerritoryWarManager.getInstance().isAllyField(actingPlayer, activeSiegeId))
				return false;
			return true;
		}
		else if (isFort)
		{
			L2Clan clan = actingPlayer.getClan();
			if (clan != null && clan == getFort().getOwnerClan())
				return false;
		}
		else if (isCastle)
		{
			L2Clan clan = actingPlayer.getClan();
			if (clan != null && clan.getClanId() == getCastle().getOwnerId())
				return false;
		}
		return (isCastle || isFort || getIsAttackableDoor());
	}
	
	public boolean isAttackable(L2Character attacker)
	{
		return isAutoAttackable(attacker);
	}
	
	@Override
	public void updateAbnormalEffect()
	{
	}
	
	public int getDistanceToWatchObject(L2Object object)
	{
		if (!(object instanceof L2PcInstance))
			return 0;
		return 3000;
	}
	
	/**
	 * Return the distance after which the object must be remove from _knownObject according to the type of the object.<BR><BR>
	 *
	 * <B><U> Values </U> :</B><BR><BR>
	 * <li> object is a L2PcInstance : 4000</li>
	 * <li> object is not a L2PcInstance : 0 </li><BR><BR>
	 *
	 */
	public int getDistanceToForgetObject(L2Object object)
	{
		if (!(object instanceof L2PcInstance))
			return 0;
		
		return 4000;
	}
	
	/**
	 * Return null.<BR><BR>
	 */
	@Override
	public L2ItemInstance getActiveWeaponInstance()
	{
		return null;
	}
	
	@Override
	public L2Weapon getActiveWeaponItem()
	{
		return null;
	}
	
	@Override
	public L2ItemInstance getSecondaryWeaponInstance()
	{
		return null;
	}
	
	@Override
	public L2Weapon getSecondaryWeaponItem()
	{
		return null;
	}
	
	@Override
	public void broadcastStatusUpdate()
	{
		Collection<L2PcInstance> knownPlayers = getKnownList().getKnownPlayers().values();
		if (knownPlayers == null || knownPlayers.isEmpty())
			return;
		
		StaticObject su = new StaticObject(this, false);
		DoorStatusUpdate dsu = new DoorStatusUpdate(this);
		OnEventTrigger oe = null;
		if (_emitter > 0)
			oe = new OnEventTrigger(this, getOpen());

		for (L2PcInstance player : knownPlayers)
		{
			if ((getCastle() != null && getCastle().getCastleId() > 0) || (getFort() != null && getFort().getFortId() > 0 && !getIsCommanderDoor()))
				su = new StaticObject(this, true);
			
			player.sendPacket(su);
			player.sendPacket(dsu);
			if (oe != null)
				player.sendPacket(oe);
		}
	}
	
	public void onOpen()
	{
		ThreadPoolManager.getInstance().scheduleGeneral(new CloseTask(), 60000);
	}
	
	public void onClose()
	{
		closeMe();
	}
	
	public final void closeMe()
	{
		setOpen(false);
		broadcastStatusUpdate();
	}
	
	public final void openMe()
	{
		setOpen(true);
		broadcastStatusUpdate();
	}
	
	@Override
	public String toString()
	{
		return "door " + _doorId;
	}
	
	public String getDoorName()
	{
		return _name;
	}
	
	public int getXMin()
	{
		return _rangeXMin;
	}
	
	public int getYMin()
	{
		return _rangeYMin;
	}
	
	public int getZMin()
	{
		return _rangeZMin;
	}
	
	public int getXMax()
	{
		return _rangeXMax;
	}
	
	public int getYMax()
	{
		return _rangeYMax;
	}
	
	public int getZMax()
	{
		return _rangeZMax;
	}
	
	public void setRange(int xMin, int yMin, int zMin, int xMax, int yMax, int zMax)
	{
		_rangeXMin = xMin;
		_rangeYMin = yMin;
		_rangeZMin = zMin;
		
		_rangeXMax = xMax;
		_rangeYMax = yMax;
		_rangeZMax = zMax;
		
		_A = _rangeYMax * (_rangeZMax - _rangeZMin) + _rangeYMin * (_rangeZMin - _rangeZMax);
		_B = _rangeZMin * (_rangeXMax - _rangeXMin) + _rangeZMax * (_rangeXMin - _rangeXMax);
		_C = _rangeXMin * (_rangeYMax - _rangeYMin) + _rangeXMin * (_rangeYMin - _rangeYMax);
		_D = -1 * (_rangeXMin * (_rangeYMax * _rangeZMax - _rangeYMin * _rangeZMax) + _rangeXMax * (_rangeYMin * _rangeZMin - _rangeYMin * _rangeZMax) + _rangeXMin * (_rangeYMin * _rangeZMax - _rangeYMax * _rangeZMin));
	}
	
	public int getMapRegion()
	{
		return _mapRegion;
	}
	
	public void setMapRegion(int region)
	{
		_mapRegion = region;
	}
	
	public Collection<L2DefenderInstance> getKnownDefenders()
	{
		FastList<L2DefenderInstance> result = new FastList<L2DefenderInstance>();
		
		Collection<L2Object> objs = getKnownList().getKnownObjects().values();
		for (L2Object obj : objs)
		{
			if (obj instanceof L2DefenderInstance)
				result.add((L2DefenderInstance) obj);
		}
	
		return result;
	}
	
	public int getA()
	{
		return _A;
	}
	
	public int getB()
	{
		return _B;
	}
	
	public int getC()
	{
		return _C;
	}
	
	public int getD()
	{
		return _D;
	}
	
	/**
	 * Set this door as a castle wall, can be damaged by siege golem only.
	 */
	public void setIsWall(boolean b)
	{
		_isWall = b;
	}
	
	/**
	 * @return true if door is a castle wall and can be damaged by siege golem only.
	 */
	public boolean isWall()
	{
		return _isWall;
	}
	
	public void setMeshIndex(int mesh)
	{
		_meshindex = mesh;
	}
	
	public int getMeshIndex()
	{
		return _meshindex;
	}
	
	public void setEmitter(int emitter)
	{
		_emitter = emitter;
	}
	
	public int getEmitter()
	{
		return _emitter;
	}
	
	public void setTargetable(boolean targetable)
	{
		_targetable = targetable;
	}
	
	public boolean getTargetable()
	{
		return _targetable;
	}
	
	@Override
	public void reduceCurrentHp(double damage, L2Character attacker, boolean awake, boolean isDOT, L2Skill skill)
	{
		if (_isWall && !(attacker instanceof L2SiegeSummonInstance))
			return;
		
		super.reduceCurrentHp(damage, attacker, awake, isDOT, skill);
	}
	
	@Override
	public void reduceCurrentHpByDOT(double i, L2Character attacker, L2Skill skill)
	{
		// doors can't be damaged by DOTs
	}
	
	@Override
	public boolean doDie(L2Character killer)
	{
		if (!super.doDie(killer))
			return false;
		
		boolean isFort = (getFort() != null && getFort().getFortId() > 0 && getFort().getSiege().getIsInProgress()) && !getIsCommanderDoor();
		boolean isCastle = (getCastle() != null	&& getCastle().getCastleId() > 0 && getCastle().getSiege().getIsInProgress());
		
		if (isFort || isCastle)
			broadcastPacket(SystemMessage.getSystemMessage(SystemMessageId.CASTLE_GATE_BROKEN_DOWN));
		return true;
	}
	
	@Override
	public void sendInfo(L2PcInstance activeChar)
	{
		if (_emitter > 0)
			activeChar.sendPacket(new OnEventTrigger(this, getOpen()));
		
		activeChar.sendPacket(new StaticObject(this, false));
	}
}
