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
package com.l2jserver.gameserver.skills;

import java.util.NoSuchElementException;

/**
 *
 * @author  DrHouse
 */
public enum AbnormalEffect
{
	NULL("null", 0x0),
	BLEEDING("bleed", 0x000001),
	POISON("poison", 0x000002),
	REDCIRCLE("redcircle", 0x000004),
	ICE("ice", 0x000008),
	WIND("wind", 0x000010),
	FEAR("fear", 0x000020),
	STUN("stun", 0x000040),
	SLEEP("sleep", 0x000080),
	MUTED("mute", 0x000100),
	ROOT("root", 0x000200),
	HOLD_1("hold1", 0x000400),
	HOLD_2("hold2", 0x000800),
	UNKNOWN_13("unknown13", 0x001000),
	BIG_HEAD("bighead", 0x002000),
	FLAME("flame", 0x004000),
	UNKNOWN_16("unknown16", 0x008000),
	GROW("grow", 0x010000),
	FLOATING_ROOT("floatroot", 0x020000),
	DANCE_STUNNED("dancestun", 0x040000),
	FIREROOT_STUN("firerootstun", 0x080000),
	STEALTH("stealth", 0x100000),
	IMPRISIONING_1("imprison1", 0x200000),
	IMPRISIONING_2("imprison2", 0x400000),
	MAGIC_CIRCLE("magiccircle", 0x800000),
	ICE2("ice2", 0x1000000),
	EARTHQUAKE("earthquake", 0x2000000),
	UNKNOWN_27("unknown27", 0x4000000),
	INVULNERABLE("invulnerable", 0x8000000),
	VITALITY("vitality", 0x10000000),
	REAL_TARGET("realtarget", 0x20000000),
	DEATH_MARK("deathmark", 0x40000000),
	SKULL_FEAR("skull_fear", 0x80000000),
	CONFUSED("confused", 0x0020),
	
	// special effects
	S_INVULNERABLE("invulnerable", 0x000001),
	S_AIR_STUN("redglow", 0x000002),
	S_AIR_ROOT("redglow2", 0x000004),
	S_BAGUETTE_SWORD("baguettesword", 0x000008),
	S_YELLOW_AFFRO("yellowafro", 0x000010),
	S_PINK_AFFRO("pinkafro", 0x000020),
	S_BLACK_AFFRO("blackafro", 0x000040),
	S_UNKNOWN8("unknown8", 0x000080),
	STIGMA_SHILIEN("stigmashilien", 0x000100);
	
	private final int _mask;
	private final String _name;
	
	private AbnormalEffect(String name, int mask)
	{
		_name = name;
		_mask = mask;
	}
	
	public final int getMask()
	{
		return _mask;
	}
	
	public final String getName()
	{
		return _name;
	}
	
	public static AbnormalEffect getByName(String name)
	{
		for (AbnormalEffect eff : AbnormalEffect.values())
		{
			if (eff.getName().equals(name))
				return eff;
		}
		
		throw new NoSuchElementException("AbnormalEffect not found for name: '"+name+ "'.\n Please check "+AbnormalEffect.class.getCanonicalName());
	}
}