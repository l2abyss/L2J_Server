/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package com.l2jserver.gameserver.model;

import com.l2jserver.gameserver.templates.chars.L2NpcTemplate.AIType;

/**
 * Author: ShanSoft
 * By L2JTW
 * 
 * 
 */

// This Data is for NPC Attributes and AI relate stuffs...
// Still need to finish...Update later...
public class L2NpcAIData
{
	
	//Basic AI
	private int _primary_attack;
	private int _skill_chance;
	private int _canMove;
	private int _soulshot;
	private int _spiritshot;
	private int _soulshotchance;
	private int _spiritshotchance;
	private int _ischaos;
	private String _clan = null;
	private int _clanRange;
	private String _enemyClan = null;
	private int _enemyRange;
	//private int _baseShldRate;
	//private int _baseShldDef;
	private int _dodge;
	private int _longrangeskill;
	private int _shortrangeskill;
	private int _longrangechance;
	private int _shortrangechance;
	private int _switchrangechance;
	private AIType _aiType = AIType.FIGHTER;
	
	//--------------------------------------------------------------------------------------------------------------
	//Setting....
	//--------------------------------------------------------------------------------------------------------------
	public void setPrimaryAttack(int primaryattack)
	{
		
		_primary_attack = primaryattack;
		
	}
	
	public void setSkillChance(int skill_chance)
	{
		
		_skill_chance = skill_chance;
		
	}
	
	public void setCanMove(int canMove)
	{
		
		_canMove = canMove;
		
	}
	
	public void setSoulShot(int soulshot)
	{
		
		_soulshot = soulshot;
		
	}
	
	public void setSpiritShot(int spiritshot)
	{
		
		_spiritshot = spiritshot;
		
	}
	
	public void setSoulShotChance(int soulshotchance)
	{
		
		_soulshotchance = soulshotchance;
		
	}
	
	public void setSpiritShotChance(int spiritshotchance)
	{
		
		_spiritshotchance = spiritshotchance;
		
	}
	
	public void setShortRangeSkill(int shortrangeskill)
	{
		
		_shortrangeskill = shortrangeskill;
		
	}
	
	public void setShortRangeChance(int shortrangechance)
	{
		
		_shortrangechance = shortrangechance;
		
	}
	
	public void setLongRangeSkill(int longrangeskill)
	{
		
		_longrangeskill = longrangeskill;
		
	}
	
	public void setLongRangeChance(int longrangechance)
	{
		
		_shortrangechance = longrangechance;
		
	}
	
	public void setSwitchRangeChance(int switchrangechance)
	{
		
		_switchrangechance = switchrangechance;
		
	}
	
	public void setIsChaos(int ischaos)
	{
		_ischaos = ischaos;
		
	}
	
	public void setClan(String clan)
	{
		if (clan != null && !clan.equals("") && !clan.equalsIgnoreCase("null"))
			_clan = clan.intern();
	}
	
	public void setClanRange(int clanRange)
	{
		_clanRange = clanRange;
	}
	
	public void setEnemyClan(String enemyClan)
	{
		if (enemyClan != null && !enemyClan.equals("") && !enemyClan.equalsIgnoreCase("null"))
			_enemyClan = enemyClan.intern();
	}
	
	public void setEnemyRange(int enemyRange)
	{
		
		_enemyRange = enemyRange;
		
	}
	
	public void setDodge(int dodge)
	{
		_dodge = dodge;
	}
	
	public void setAi(String ai)
	{
		if (ai.equalsIgnoreCase("archer"))
			_aiType = AIType.ARCHER;
		else if (ai.equalsIgnoreCase("balanced"))
			_aiType = AIType.BALANCED;
		else if (ai.equalsIgnoreCase("mage"))
			_aiType = AIType.MAGE;
		else if (ai.equalsIgnoreCase("healer"))
			_aiType = AIType.HEALER;
		else if (ai.equalsIgnoreCase("corpse"))
			_aiType = AIType.CORPSE;
		else
			_aiType = AIType.FIGHTER;
	}
	
	/*
	
	public void setBaseShldRate (int baseShldRate)
	{
		
		_baseShldRate = baseShldRate;
		
	}
	
	public void setBaseShldDef (int baseShldDef)
	{
		
		_baseShldDef = baseShldDef;
		
	}
	*/

	//--------------------------------------------------------------------------------------------------------------
	//Data Recall....
	//--------------------------------------------------------------------------------------------------------------
	public int getPrimaryAttack()
	{
		
		return _primary_attack;
		
	}
	
	public int getSkillChance()
	{
		
		return _skill_chance;
		
	}
	
	public int getCanMove()
	{
		
		return _canMove;
		
	}
	
	public int getSoulShot()
	{
		
		return _soulshot;
		
	}
	
	public int getSpiritShot()
	{
		
		return _spiritshot;
		
	}
	
	public int getSoulShotChance()
	{
		
		return _soulshotchance;
		
	}
	
	public int getSpiritShotChance()
	{
		
		return _spiritshotchance;
		
	}
	
	public int getShortRangeSkill()
	{
		
		return _shortrangeskill;
		
	}
	
	public int getShortRangeChance()
	{
		
		return _shortrangechance;
		
	}
	
	public int getLongRangeSkill()
	{
		
		return _longrangeskill;
		
	}
	
	public int getLongRangeChance()
	{
		
		return _longrangechance;
		
	}
	
	public int getSwitchRangeChance()
	{
		
		return _switchrangechance;
		
	}
	
	public int getIsChaos()
	{
		
		return _ischaos;
		
	}
	
	public String getClan()
	{
		
		return _clan;
		
	}
	
	public int getClanRange()
	{
		return _clanRange;
	}
	
	public String getEnemyClan()
	{
		
		return _enemyClan;
		
	}
	
	public int getEnemyRange()
	{
		
		return _enemyRange;
		
	}
	
	public int getDodge()
	{
		
		return _dodge;
		
	}
	
	public AIType getAiType()
	{
		return _aiType;	
	}
	
	/*
	
	public int getBaseShldRate ()
	{
		
		return _baseShldRate;
		
	}
	
	public int getBaseShldDef ()
	{
		
		return _baseShldDef;
		
	}
	*/
}
