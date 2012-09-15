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
package com.l2jserver.gameserver.model.actor.templates;

import java.util.List;

import com.l2jserver.gameserver.datatables.InitialEquipmentData;
import com.l2jserver.gameserver.model.StatsSet;
import com.l2jserver.gameserver.model.base.ClassId;
import com.l2jserver.gameserver.model.base.ClassInfo;
import com.l2jserver.gameserver.model.base.Race;
import com.l2jserver.gameserver.model.items.PcItemTemplate;

/**
 * @author mkizub, Zoey76
 */
public class L2PcTemplate extends L2CharTemplate
{
	private final ClassId _classId;
	private final Race _race;
	private final String _className;
	
	private final int _spawnX;
	private final int _spawnY;
	private final int _spawnZ;
	
	private final int _classBaseLevel;
	private final float _lvlHpAdd;
	private final float _lvlHpMod;
	private final float _lvlCpAdd;
	private final float _lvlCpMod;
	private final float _lvlMpAdd;
	private final float _lvlMpMod;
	
	private final double _fCollisionHeightMale;
	private final double _fCollisionRadiusMale;
	
	private final double _fCollisionHeightFemale;
	private final double _fCollisionRadiusFemale;
	
	private final int _fallHeight;
	
	private final List<PcItemTemplate> _initialEquipment;
	
	public L2PcTemplate(StatsSet set)
	{
		super(set);
		_classId = ClassId.getClassId(set.getInteger("classId"));
		_race = Race.values()[set.getInteger("raceId")];
		_className = set.getString("className");
		
		_spawnX = set.getInteger("spawnX");
		_spawnY = set.getInteger("spawnY");
		_spawnZ = set.getInteger("spawnZ");
		
		_classBaseLevel = set.getInteger("classBaseLevel");
		_lvlHpAdd = set.getFloat("lvlHpAdd");
		_lvlHpMod = set.getFloat("lvlHpMod");
		_lvlCpAdd = set.getFloat("lvlCpAdd");
		_lvlCpMod = set.getFloat("lvlCpMod");
		_lvlMpAdd = set.getFloat("lvlMpAdd");
		_lvlMpMod = set.getFloat("lvlMpMod");
		
		_fCollisionRadiusMale = set.getDouble("collision_radius");
		_fCollisionHeightMale = set.getDouble("collision_height");
		
		_fCollisionRadiusFemale = set.getDouble("collision_radius_female");
		_fCollisionHeightFemale = set.getDouble("collision_height_female");
		
		_fallHeight = 333; // TODO: Unhardcode it.
		
		_initialEquipment = InitialEquipmentData.getInstance().getEquipmentList(_classId);
	}
	
	/**
	 * @return the template class Id.
	 */
	public ClassId getClassId()
	{
		return _classId;
	}
	
	/**
	 * @return the template race.
	 */
	public Race getRace()
	{
		return _race;
	}
	
	/**
	 * @return the template server side class name.
	 * @deprecated replaced by {@link ClassInfo#getClassName()}
	 */
	@Deprecated
	public String getClassName()
	{
		return _className;
	}
	
	/**
	 * @return the template X spawn coordinate.
	 */
	public int getSpawnX()
	{
		return _spawnX;
	}
	
	/**
	 * @return the template Y spawn coordinate.
	 */
	public int getSpawnY()
	{
		return _spawnY;
	}
	
	/**
	 * @return the template Z spawn coordinate.
	 */
	public int getSpawnZ()
	{
		return _spawnZ;
	}
	
	/**
	 * @return the template class base level.
	 */
	public int getClassBaseLevel()
	{
		return _classBaseLevel;
	}
	
	/**
	 * @return the template level Hp add.
	 */
	public float getLvlHpAdd()
	{
		return _lvlHpAdd;
	}
	
	/**
	 * @return the template level Hp mod.
	 */
	public float getLvlHpMod()
	{
		return _lvlHpMod;
	}
	
	/**
	 * @return the template level Cp add.
	 */
	public float getLvlCpAdd()
	{
		return _lvlCpAdd;
	}
	
	/**
	 * @return the template level Cp mod.
	 */
	public float getLvlCpMod()
	{
		return _lvlCpMod;
	}
	
	/**
	 * @return the template level Mp add.
	 */
	public float getLvlMpAdd()
	{
		return _lvlMpAdd;
	}
	
	/**
	 * @return the template level Mp mod.
	 */
	public float getLvlMpMod()
	{
		return _lvlMpMod;
	}
	
	/**
	 * @return the template collision height for male characters.
	 */
	public double getFCollisionHeightMale()
	{
		return _fCollisionHeightMale;
	}
	
	/**
	 * @return the template collision radius for male characters.
	 */
	public double getFCollisionRadiusMale()
	{
		return _fCollisionRadiusMale;
	}
	
	/**
	 * @return the template collision height for female characters.
	 */
	public double getFCollisionHeightFemale()
	{
		return _fCollisionHeightFemale;
	}
	
	/**
	 * @return the template collision radius for female characters.
	 */
	public double getFCollisionRadiusFemale()
	{
		return _fCollisionRadiusFemale;
	}
	
	/**
	 * @return the fall height.
	 */
	public int getFallHeight()
	{
		return _fallHeight;
	}
	
	/**
	 * @return the initial equipment for this Pc template.
	 */
	public List<PcItemTemplate> getInitialEquipment()
	{
		return _initialEquipment;
	}
	
	/**
	 * @return {@code true} if this Pc template has an initial equipment associated, {@code false} otherwise.
	 */
	public boolean hasInitialEquipment()
	{
		return _initialEquipment != null;
	}
}
