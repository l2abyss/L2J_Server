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
package com.l2jserver.gameserver.model.entity.clanhall;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

import javolution.util.FastList;
import javolution.util.FastMap;

import com.l2jserver.L2DatabaseFactory;
import com.l2jserver.gameserver.datatables.DoorTable;
import com.l2jserver.gameserver.model.L2Clan;
import com.l2jserver.gameserver.model.L2SiegeClan;
import com.l2jserver.gameserver.model.L2SiegeClan.SiegeClanType;
import com.l2jserver.gameserver.model.actor.instance.L2DoorInstance;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.entity.ClanHall;
import com.l2jserver.gameserver.model.zone.type.L2SiegeZone;
import com.l2jserver.gameserver.network.serverpackets.SiegeInfo;
import com.l2jserver.gameserver.templates.StatsSet;

/**
 * @author BiggBoss
 */
public final class SiegableHall extends ClanHall
{	
	private static final String SQL_SAVE = "UPDATE siegable_clanhall SET ownerId=?, nextSiege=? WHERE clanHallId=?";
		
	protected List<String> _doorDefault;
	
	private Calendar _nextSiege;
	private long _siegeLength;
	private int[] _scheduleConfig = {7,0,0,12,0};
	
	private SiegeStatus _status = SiegeStatus.REGISTERING;
	private L2SiegeZone _siegeZone;
	
	private ClanHallSiegeEngine _siege;
	
	public SiegableHall(StatsSet set)
	{
		super(set);
		_doorDefault = new FastList<String>();
		_siegeLength = set.getLong("siegeLenght");
		String[] rawSchConfig = set.getString("scheduleConfig").split(";");
		if(rawSchConfig.length == 5)
		{
			for(int i = 0; i < 5; i++)
			{
				try
				{
					_scheduleConfig[i] = Integer.parseInt(rawSchConfig[i]);
				}
				catch(Exception e)
				{
					_log.warning("SiegableHall - "+getName()+": Wrong schedule_config parameters!");
				}
			}
		}
		else
			_log.warning(getName()+": Wrong schedule_config value in siegable_halls table, using default (7 days)");
		
		_nextSiege = Calendar.getInstance();
		long nextSiege = set.getLong("nextSiege");
		if(nextSiege - System.currentTimeMillis() < 0)
			updateNextSiege();
		else
			_nextSiege.setTimeInMillis(nextSiege);
		_log.config(getName()+" siege scheduled for: "+_nextSiege.getTime());		
	}
			
	public List<String> getDoorDefault()
	{
		return _doorDefault;
	}
	
	public void spawnDoor()
	{
		spawnDoor(false);
	}
	
	public void spawnDoor(boolean isDoorWeak)
	{
		for (int i = 0; i < getDoors().size(); i++)
		{
			L2DoorInstance door = getDoors().get(i);
			if (door.getCurrentHp() <= 0)
			{
				door.decayMe(); // Kill current if not killed already
				door = DoorTable.parseList(_doorDefault.get(i), false);
				DoorTable.getInstance().putDoor(door); //Readd the new door to the DoorTable By Erb
				if (isDoorWeak)
					door.setCurrentHp(door.getMaxHp() / 2);
				door.spawnMe(door.getX(), door.getY(), door.getZ());
				getDoors().set(i, door);
			}
			else if (door.getOpen())
				door.closeMe();
		}
	}
		
	@Override
	public final void updateDb()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			
			statement = con.prepareStatement(SQL_SAVE);
			statement.setInt(1, getOwnerId());
			statement.setLong(2, getNextSiegeTime());
			statement.setInt(3, getId());
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Exception: SiegableHall.updateDb(): " + e.getMessage(), e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
	}
	
	public final void setSiege(final ClanHallSiegeEngine siegable)
	{
		_siege = siegable;
		_siegeZone.setSiegeInstance(siegable);
	}
	
	public final ClanHallSiegeEngine getSiege()
	{
		return _siege;
	}
	
	public final Calendar getSiegeDate()
	{
		return _nextSiege;
	}
	
	public final long getNextSiegeTime()
	{
		return _nextSiege.getTimeInMillis();
	}
	
	public long getSiegeLenght()
	{
		return _siegeLength;
	}
	
	public final void setNextSiegeDate(long date)
	{
		_nextSiege.setTimeInMillis(date);
	}
	
	public final void setNextSiegeDate(final Calendar c)
	{
		_nextSiege = c;
	}
	
	public final void updateNextSiege()
	{
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DAY_OF_YEAR, _scheduleConfig[0]);
		c.add(Calendar.MONTH, _scheduleConfig[1]);
		c.add(Calendar.YEAR, _scheduleConfig[2]);
		c.set(Calendar.HOUR_OF_DAY, _scheduleConfig[3]);
		c.set(Calendar.MINUTE, _scheduleConfig[4]);
		c.set(Calendar.SECOND, 0);
		setNextSiegeDate(c);
		updateDb();
	}
	
	public final FastMap<Integer, L2SiegeClan> getAttackersList()
	{
		return getSiege().getAttackers();
	}
	
	public final Collection<L2SiegeClan> getAttackers()
	{
		return getSiege().getAttackers().values();
	}
	
	public final void addAttacker(final L2Clan clan)
	{
		getSiege().getAttackers().put(clan.getClanId(), new L2SiegeClan(clan.getClanId(), SiegeClanType.ATTACKER));
	}
	
	public final void removeAttacker(final L2Clan clan)
	{
		getSiege().getAttackers().remove(clan.getClanId());
	}
	
	public final boolean isRegistered(L2Clan clan)
	{
		if(getSiege() == null)
			return false;
		
		return getSiege().checkIsAttacker(clan);
	}
	
	public final void clearAttackers()
	{
		getSiege().getAttackers().clear();
	}
	
	public final boolean isRegistering() 
	{ 
		return _status == SiegeStatus.REGISTERING; 
	}
	
	public final boolean isInSiege() 
	{ 
		return _status == SiegeStatus.RUNNING; 
	}
	
	public final boolean isWaitingBattle() 
	{ 
		return _status == SiegeStatus.WAITING_BATTLE; 
	}
	
	public final void updateSiegeStatus(SiegeStatus status)
	{
		_status = status;
	}
	
	public final L2SiegeZone getSiegeZone()
	{
		return _siegeZone;
	}
	
	public final void setSiegeZone(L2SiegeZone zone)
	{
		_siegeZone = zone;
	}

	public final void updateSiegeZone(boolean active)
	{
		_siegeZone.setIsActive(active);
	}	

	public final void showSiegeInfo(L2PcInstance player)
	{
		player.sendPacket(new SiegeInfo(this));
	}
			
	@Override
	public final boolean isSiegableHall()
	{
		return true;
	}
}
