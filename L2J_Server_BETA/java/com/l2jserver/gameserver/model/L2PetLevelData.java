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
package com.l2jserver.gameserver.model;

/**
 * Stats definition for each pet level
 * @author JIV
 *
 */
public class L2PetLevelData
{
	private int _ownerExpTaken;
	private long _petMaxExp;
	private float _petMaxHP;
	private float _petMaxMP;
	private float _petPAtk;
	private float _petPDef;
	private float _petMAtk;
	private float _petMDef;
	private int _petMaxFeed;
	private int _petFeedBattle;
	private int _petFeedNormal;
	private float _petRegenHP;
	private float _petRegenMP;
	private short _petSoulShot;
	private short _petSpiritShot;
	
	//  Max Exp
	public long getPetMaxExp()
	{
		return _petMaxExp;
	}
	
	public void setPetMaxExp(long pPetMaxExp)
	{
		_petMaxExp = pPetMaxExp;
	}
	
	public int getOwnerExpTaken()
	{
		return _ownerExpTaken;
	}
	
	public void setOwnerExpTaken(int pOwnerExpTaken)
	{
		_ownerExpTaken = pOwnerExpTaken;
	}
	
	//  Max HP
	public float getPetMaxHP()
	{
		return _petMaxHP;
	}
	
	public void setPetMaxHP(float pPetMaxHP)
	{
		_petMaxHP = pPetMaxHP;
	}
	
	//  Max Mp
	public float getPetMaxMP()
	{
		return _petMaxMP;
	}
	
	public void setPetMaxMP(float pPetMaxMP)
	{
		_petMaxMP = pPetMaxMP;
	}
	
	//  PAtk
	public float getPetPAtk()
	{
		return _petPAtk;
	}
	
	public void setPetPAtk(float pPetPAtk)
	{
		_petPAtk = pPetPAtk;
	}
	
	//  PDef
	public float getPetPDef()
	{
		return _petPDef;
	}
	
	public void setPetPDef(float pPetPDef)
	{
		_petPDef = pPetPDef;
	}
	
	//  MAtk
	public float getPetMAtk()
	{
		return _petMAtk;
	}
	
	public void setPetMAtk(float pPetMAtk)
	{
		_petMAtk = pPetMAtk;
	}
	
	//  MDef
	public float getPetMDef()
	{
		return _petMDef;
	}
	
	public void setPetMDef(float pPetMDef)
	{
		_petMDef = pPetMDef;
	}
	
	//  MaxFeed
	public int getPetMaxFeed()
	{
		return _petMaxFeed;
	}
	
	public void setPetMaxFeed(int pPetMaxFeed)
	{
		_petMaxFeed = pPetMaxFeed;
	}
	
	//  Normal Feed
	public int getPetFeedNormal()
	{
		return _petFeedNormal;
	}
	
	public void setPetFeedNormal(int pPetFeedNormal)
	{
		_petFeedNormal = pPetFeedNormal;
	}
	
	//  Battle Feed
	public int getPetFeedBattle()
	{
		return _petFeedBattle;
	}
	
	public void setPetFeedBattle(int pPetFeedBattle)
	{
		_petFeedBattle = pPetFeedBattle;
	}
	
	//  Regen HP
	public float getPetRegenHP()
	{
		return _petRegenHP;
	}
	
	public void setPetRegenHP(float pPetRegenHP)
	{
		_petRegenHP = pPetRegenHP;
	}
	
	//  Regen MP
	public float getPetRegenMP()
	{
		return _petRegenMP;
	}
	
	public void setPetRegenMP(float pPetRegenMP)
	{
		_petRegenMP = pPetRegenMP;
	}
	
	/**
	 * @return the _petSoulShot
	 */
	public short getPetSoulShot()
	{
		return _petSoulShot;
	}
	/**
	 * @param soulShot the _petSoulShot to set
	 */
	public void setPetSoulShot(short soulShot)
	{
		_petSoulShot = soulShot;
	}
	/**
	 * @return the _petSpiritShot
	 */
	public short getPetSpiritShot()
	{
		return _petSpiritShot;
	}
	/**
	 * @param spiritShot the _petSpiritShot to set
	 */
	public void setPetSpiritShot(short spiritShot)
	{
		_petSpiritShot = spiritShot;
	}
}
