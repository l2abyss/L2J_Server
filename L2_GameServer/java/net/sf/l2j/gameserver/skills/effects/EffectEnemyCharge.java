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
package net.sf.l2j.gameserver.skills.effects;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.GeoData;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.Location;
import net.sf.l2j.gameserver.network.serverpackets.FlyToLocation;
import net.sf.l2j.gameserver.network.serverpackets.ValidateLocation;
import net.sf.l2j.gameserver.network.serverpackets.FlyToLocation.FlyType;
import net.sf.l2j.gameserver.skills.Env;

public class EffectEnemyCharge extends L2Effect
{

	private int	_x, _y, _z;

	public EffectEnemyCharge(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public EffectType getEffectType()
	{
		return EffectType.BUFF;
	}

	@Override
	public void onStart()
	{
		// Get current position of the L2Character
		final int curX = getEffector().getX();
		final int curY = getEffector().getY();
		final int curZ = getEffector().getZ();
		
		// Calculate distance (dx,dy) between current position and destination
		double dx = getEffected().getX() - curX;
		double dy = getEffected().getY() - curY;
		double dz = getEffected().getZ() - curZ;
		double distance = Math.sqrt(dx*dx + dy*dy);
		
		int offset = Math.max((int)distance-getSkill().getFlyRadius(), 30);
		
		double cos;
		double sin;
			
		// approximation for moving closer when z coordinates are different
		// TODO: handle Z axis movement better
		offset -= Math.abs(dz);  
		if (offset < 5) offset = 5;
			
		// If no distance
		if (distance < 1 || distance - offset  <= 0)
			return;
		
		// Calculate movement angles needed
		sin = dy/distance;
		cos = dx/distance;
		
		// Calculate the new destination with offset included
		_x = curX + (int)((distance-offset) * cos);
		_y = curY + (int)((distance-offset) * sin);
		_z = getEffected().getZ();

		if (Config.GEODATA > 0)
		{
			Location destiny = GeoData.getInstance().moveCheck(getEffector().getX(), getEffector().getY(), getEffector().getZ(), _x, _y, _z);
			_x = destiny.getX();
			_y = destiny.getY();
		}
		getEffector().broadcastPacket(new FlyToLocation(getEffector(), _x, _y, _z, FlyType.CHARGE));
		//getEffector().abortAttack();
		//getEffector().abortCast();
	}

	@Override
	public void onExit()
	{
		// maybe is need force set X,Y,Z
		getEffector().setXYZ(_x, _y, _z);
		getEffector().broadcastPacket(new ValidateLocation(getEffector()));
	}

	@Override
	public boolean onActionTime()
	{
		return false;
	}
}
