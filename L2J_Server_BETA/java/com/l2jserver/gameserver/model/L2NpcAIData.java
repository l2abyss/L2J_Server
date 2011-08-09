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
	private int _primarySkillId;
	private int _minskillChance;
	private int _maxskillChance;
	private int _canMove;
	private int _soulshot;
	private int _spiritshot;
	private int _soulshotChance;
	private int _spiritshotChance;
	private int _isChaos;
	private String _clan = null;
	private int _clanRange;
	private String _enemyClan = null;
	private int _enemyRange;
	//private int _baseShldRate;
	//private int _baseShldDef;
	private int _dodge;
	private int _longRangeSkill;
	private int _shortRangeSkill;
	private int _longRangeChance;
	private int _shortRangeChance;
	private int _switchRangeChance;
	private AIType _aiType = AIType.FIGHTER;
	
	//--------------------------------------------------------------------------------------------------------------
	//Setting....
	//--------------------------------------------------------------------------------------------------------------
	public void setPrimarySkillId(int primarySkillId)
	{
		_primarySkillId = primarySkillId;
	}
	
	public void setMinSkillChance(int skill_chance)
	{	
		_minskillChance = skill_chance;
	}
	
	public void setMaxSkillChance(int skill_chance)
	{
		_maxskillChance = skill_chance;
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
		_soulshotChance = soulshotchance;	
	}
	
	public void setSpiritShotChance(int spiritshotchance)
	{
		_spiritshotChance = spiritshotchance;	
	}
	
	public void setShortRangeSkill(int shortrangeskill)
	{
		_shortRangeSkill = shortrangeskill;
	}
	
	public void setShortRangeChance(int shortrangechance)
	{
		_shortRangeChance = shortrangechance;
	}
	
	public void setLongRangeSkill(int longrangeskill)
	{
		_longRangeSkill = longrangeskill;	
	}
	
	public void setLongRangeChance(int longrangechance)
	{
		_shortRangeChance = longrangechance;
	}
	
	public void setSwitchRangeChance(int switchrangechance)
	{
		_switchRangeChance = switchrangechance;
	}
	
	public void setIsChaos(int ischaos)
	{
		_isChaos = ischaos;
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
	
	public void setBaseShldRate(int baseShldRate)
	{
		_baseShldRate = baseShldRate;	
	}
	
	public void setBaseShldDef(int baseShldDef)
	{
		_baseShldDef = baseShldDef;	
	}
	 */
	
	//--------------------------------------------------------------------------------------------------------------
	//Data Recall....
	//--------------------------------------------------------------------------------------------------------------
	public int getPrimarySkillId()
	{
		return _primarySkillId;
	}
	
	public int getMinSkillChance()
	{
		return _minskillChance;
	}
	
	public int getMaxSkillChance()
	{
		return _maxskillChance;
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
		return _soulshotChance;	
	}
	
	public int getSpiritShotChance()
	{
		return _spiritshotChance;	
	}
	
	public int getShortRangeSkill()
	{
		return _shortRangeSkill;	
	}
	
	public int getShortRangeChance()
	{
		return _shortRangeChance;	
	}
	
	public int getLongRangeSkill()
	{
		return _longRangeSkill;	
	}
	
	public int getLongRangeChance()
	{
		return _longRangeChance;	
	}
	
	public int getSwitchRangeChance()
	{
		return _switchRangeChance;	
	}
	
	public int getIsChaos()
	{
		return _isChaos;	
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
	
	public int getBaseShldRate()
	{
		return _baseShldRate;	
	}
	
	public int getBaseShldDef()
	{
		return _baseShldDef;	
	}
	 */
}
