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
package net.sf.l2j.gameserver.model.zone.type;

import java.util.Collection;
import java.util.concurrent.Future;

import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.zone.L2ZoneType;

/**
 * A damage zone
 *
 * @author  durgus
 */
public class L2DamageZone extends L2ZoneType
{
	private int _damageHPPerSec;
	private int _damageMPPerSec;
	private Future<?> _task;

	public L2DamageZone(int id)
	{
		super(id);

		// Setup default damage
		_damageHPPerSec = 200;
		_damageMPPerSec = 0;
	}

	@Override
	public void setParameter(String name, String value)
	{
		if (name.equals("dmgHPSec"))
		{
			_damageHPPerSec = Integer.parseInt(value);
		}
		else if (name.equals("dmgMPSec"))
		{
			_damageMPPerSec = Integer.parseInt(value);
		}
		else super.setParameter(name, value);
	}

	@Override
	protected void onEnter(L2Character character)
	{
		if (_task == null && (_damageHPPerSec != 0 || _damageMPPerSec != 0))
		{
			_task = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new ApplyDamage(this), 10, 3300);
		}
	}

	@Override
	protected void onExit(L2Character character)
	{
		if (_characterList.isEmpty())
		{
			_task.cancel(true);
			_task = null;
		}
	}

	protected Collection<L2Character> getCharacterList()
	{
		return _characterList.values();
	}

	protected int getHPDamagePerSecond()
	{
		return _damageHPPerSec;
	}

	protected int getMPDamagePerSecond()
	{
		return _damageMPPerSec;
	}

	class ApplyDamage implements Runnable
	{
		private L2DamageZone _dmgZone;
		ApplyDamage(L2DamageZone zone)
		{
			_dmgZone = zone;
		}

		public void run()
		{
			for (L2Character temp : _dmgZone.getCharacterList())
			{
				if (temp != null && !temp.isDead())
				{
					if (getHPDamagePerSecond() != 0)
						temp.reduceCurrentHp(_dmgZone.getHPDamagePerSecond(), null);
					if (getMPDamagePerSecond() != 0)
						temp.reduceCurrentMp(_dmgZone.getMPDamagePerSecond());
				}
			}
		}
	}

	@Override
    public void onDieInside(L2Character character) {}

	@Override
    public void onReviveInside(L2Character character) {}

}
