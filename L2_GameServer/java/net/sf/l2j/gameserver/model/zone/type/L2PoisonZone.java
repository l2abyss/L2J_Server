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
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.zone.L2ZoneType;
import net.sf.l2j.util.Rnd;

/**
 * another type of damage zone with skills
 *
 * @author  kerberos
 */
public class L2PoisonZone extends L2ZoneType
{
	private int _skillId;

	private Future<?> _task;

	private int _chance;

	private int _initialDelay;

	public L2PoisonZone(int id)
	{
		super(id);

		// Setup default skill
		_skillId = 4070;
		_chance = 100;
		_initialDelay = 0;
	}

	@Override
	public void setParameter(String name, String value)
	{
		if (name.equals("skillId"))
		{
			_skillId = Integer.parseInt(value);
		}
		else if (name.equals("chance"))
		{
			_chance = Integer.parseInt(value);
		}
		else if (name.equals("initialDelay"))
		{
			_initialDelay = Integer.parseInt(value);
		}
		else super.setParameter(name, value);
	}

	@Override
	protected void onEnter(L2Character character)
	{
		if (_task == null && Rnd.get(100) < _chance)
		{
			_task = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new ApplySkill(this), _initialDelay, 10000);
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

	public int getSkillId()
	{
		return _skillId;
	}
	protected Collection<L2Character> getCharacterList()
	{
		return _characterList.values();
	}

	class ApplySkill implements Runnable
	{
		private L2PoisonZone _poisonZone;
		ApplySkill(L2PoisonZone zone)
		{
			_poisonZone = zone;
		}

		public void run()
		{
			for (L2Character temp : _poisonZone.getCharacterList())
			{
				if (temp != null && !temp.isDead())
				{
                    L2Effect[] effects = temp.getAllEffects();
                    for (L2Effect e : effects)
                    {
                    	if (e.getSkill().getId() != getSkillId())
                    		e.getSkill().getEffects(temp, temp);
                    }
				}
			}
		}
	}

	@Override
	protected void onDieInside(L2Character character) {}

	@Override
	protected void onReviveInside(L2Character character) {}

}
