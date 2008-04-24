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
import net.sf.l2j.gameserver.serverpackets.FlyToLocation;
import net.sf.l2j.gameserver.serverpackets.ValidateLocation;
import net.sf.l2j.gameserver.serverpackets.FlyToLocation.FlyType;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.util.Util;

public class EffectBlink extends L2Effect
{

    private int x, y, z;

    public EffectBlink(Env env, EffectTemplate template)
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
        int _radius = getSkill().getFlyRadius();
        double angle = Util.convertHeadingToDegree(getEffected().getHeading());
        double radian = Math.toRadians(angle);
        int x1 = (int) (Math.sin(radian) * _radius);
        int y1 = (int) (Math.cos(radian) * _radius);

        x = getEffected().getX() - x1;
        y = getEffected().getY() - y1;
        z = getEffected().getZ();

        if (Config.GEODATA > 0)
        {
            Location destiny = GeoData.getInstance().moveCheck(getEffected().getX(), getEffected().getY(), getEffected().getZ(), x, y, z);
            x = destiny.getX();
            y = destiny.getY();
            z = destiny.getZ();
        }
        getEffected().broadcastPacket(new FlyToLocation(getEffected(), x, y, z, FlyType.DUMMY));
        getEffected().abortAttack();
        getEffected().abortCast();
    }

    @Override
    public void onExit()
    {
        // maybe is need force set X,Y,Z
        getEffected().setXYZ(x, y, z);
        getEffected().broadcastPacket(new ValidateLocation(getEffected()));
    }

    @Override
    public boolean onActionTime()
    {
        return false;
    }
}
