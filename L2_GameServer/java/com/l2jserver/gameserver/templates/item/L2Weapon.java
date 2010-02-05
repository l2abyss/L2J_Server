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
package com.l2jserver.gameserver.templates.item;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.l2jserver.gameserver.datatables.SkillTable;
import com.l2jserver.gameserver.handler.ISkillHandler;
import com.l2jserver.gameserver.handler.SkillHandler;
import com.l2jserver.gameserver.model.L2Effect;
import com.l2jserver.gameserver.model.L2ItemInstance;
import com.l2jserver.gameserver.model.L2Object;
import com.l2jserver.gameserver.model.L2Skill;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.quest.Quest;
import com.l2jserver.gameserver.skills.Env;
import com.l2jserver.gameserver.skills.Formulas;
import com.l2jserver.gameserver.skills.conditions.Condition;
import com.l2jserver.gameserver.skills.conditions.ConditionGameChance;
import com.l2jserver.gameserver.skills.funcs.Func;
import com.l2jserver.gameserver.skills.funcs.FuncTemplate;
import com.l2jserver.gameserver.templates.StatsSet;
import com.l2jserver.gameserver.templates.skills.L2SkillType;

import javolution.util.FastList;

/**
 * This class is dedicated to the management of weapons.
 *
 * @version $Revision: 1.4.2.3.2.5 $ $Date: 2005/04/02 15:57:51 $
 */
public final class L2Weapon extends L2Item
{
	private final int _soulShotCount;
	private final int _spiritShotCount;
	private final int _pDam;
	private final int _rndDam;
	private final int _critical;
	private final double _hitModifier;
	private final int _avoidModifier;
	private final int _shieldDef;
	private final double _shieldDefRate;
	private final int _atkSpeed;
	private final int _atkReuse;
	private final int _mpConsume;
	private final int _mDam;
	private L2Skill _enchant4Skill = null; // skill that activates when item is enchanted +4 (for duals)
	private final int _changeWeaponId;
	private final String[] _skill;
	
	// Attached skills for Special Abilities
	protected L2Skill _skillsOnCast;
	protected Condition _skillsOnCastCondition;
	protected L2Skill _skillsOnCrit;
	protected Condition _skillsOnCritCondition;
	
	/**
	 * Constructor for Weapon.<BR><BR>
	 * <U><I>Variables filled :</I></U><BR>
	 * <LI>_soulShotCount & _spiritShotCount</LI>
	 * <LI>_pDam & _mDam & _rndDam</LI>
	 * <LI>_critical</LI>
	 * <LI>_hitModifier</LI>
	 * <LI>_avoidModifier</LI>
	 * <LI>_shieldDes & _shieldDefRate</LI>
	 * <LI>_atkSpeed & _AtkReuse</LI>
	 * <LI>_mpConsume</LI>
	 * @param type : L2ArmorType designating the type of armor
	 * @param set : StatsSet designating the set of couples (key,value) caracterizing the armor
	 * @see L2Item constructor
	 */
	public L2Weapon(L2WeaponType type, StatsSet set)
	{
		super(type, set);
		_soulShotCount = set.getInteger("soulshots");
		_spiritShotCount = set.getInteger("spiritshots");
		_pDam = set.getInteger("p_dam");
		_rndDam = set.getInteger("rnd_dam");
		_critical = set.getInteger("critical");
		_hitModifier = set.getDouble("hit_modify");
		_avoidModifier = set.getInteger("avoid_modify");
		_shieldDef = set.getInteger("shield_def");
		_shieldDefRate = set.getDouble("shield_def_rate");
		_atkSpeed = set.getInteger("atk_speed");
		_atkReuse = set.getInteger("atk_reuse", (type == L2WeaponType.BOW) ? 1500 : (type == L2WeaponType.CROSSBOW) ? 1200 : 0);
		_mpConsume = set.getInteger("mp_consume");
		_mDam = set.getInteger("m_dam");
		
		_skill = set.getString("skill").split(";");
		
		int sId = set.getInteger("enchant4_skill_id");
		int sLv = set.getInteger("enchant4_skill_lvl");
		if (sId > 0 && sLv > 0)
			_enchant4Skill = SkillTable.getInstance().getInfo(sId, sLv);
		
		sId = set.getInteger("onCast_skill_id");
		sLv = set.getInteger("onCast_skill_lvl");
		int sCh = set.getInteger("onCast_skill_chance");
		if (sId > 0 && sLv > 0 && sCh > 0)
		{
			L2Skill skill = SkillTable.getInstance().getInfo(sId, sLv);
			//skill.attach(new ConditionGameChance(sCh),true);
			attachOnCast(skill, sCh);
		}
		
		sId = set.getInteger("onCrit_skill_id");
		sLv = set.getInteger("onCrit_skill_lvl");
		sCh = set.getInteger("onCrit_skill_chance");
		if (sId > 0 && sLv > 0 && sCh > 0)
		{
			L2Skill skill = SkillTable.getInstance().getInfo(sId, sLv);
			//skill.attach(new ConditionGameChance(sCh),true);
			attachOnCrit(skill, sCh);
		}
		_changeWeaponId = set.getInteger("change_weaponId");
	}
	
	/**
	 * Returns the type of Weapon
	 * @return L2WeaponType
	 */
	@Override
	public L2WeaponType getItemType()
	{
		return (L2WeaponType) super._type;
	}
	
	/**
	 * Returns the ID of the Etc item after applying the mask.
	 * @return int : ID of the Weapon
	 */
	@Override
	public int getItemMask()
	{
		return getItemType().mask();
	}
	
	/**
	 * Returns the quantity of SoulShot used.
	 * @return int
	 */
	public int getSoulShotCount()
	{
		return _soulShotCount;
	}
	
	/**
	 * Returns the quatity of SpiritShot used.
	 * @return int
	 */
	public int getSpiritShotCount()
	{
		return _spiritShotCount;
	}
	
	/**
	 * Returns the physical damage.
	 * @return int
	 */
	public int getPDamage()
	{
		return _pDam;
	}
	
	/**
	 * Returns the random damage inflicted by the weapon
	 * @return int
	 */
	public int getRandomDamage()
	{
		return _rndDam;
	}
	
	/**
	 * Returns the attack speed of the weapon
	 * @return int
	 */
	public int getAttackSpeed()
	{
		return _atkSpeed;
	}
	
	/**
	 * Return the Attack Reuse Delay of the L2Weapon.<BR><BR>
	 * @return int
	 */
	public int getAttackReuseDelay()
	{
		return _atkReuse;
	}
	
	/**
	 * Returns the avoid modifier of the weapon
	 * @return int
	 */
	public int getAvoidModifier()
	{
		return _avoidModifier;
	}
	
	/**
	 * Returns the rate of critical hit
	 * @return int
	 */
	public int getCritical()
	{
		return _critical;
	}
	
	/**
	 * Returns the hit modifier of the weapon
	 * @return double
	 */
	public double getHitModifier()
	{
		return _hitModifier;
	}
	
	/**
	 * Returns the magical damage inflicted by the weapon
	 * @return int
	 */
	public int getMDamage()
	{
		return _mDam;
	}
	
	/**
	 * Returns the MP consumption with the weapon
	 * @return int
	 */
	public int getMpConsume()
	{
		return _mpConsume;
	}
	
	/**
	 * Returns the shield defense of the weapon
	 * @return int
	 */
	public int getShieldDef()
	{
		return _shieldDef;
	}
	
	/**
	 * Returns the rate of shield defense of the weapon
	 * @return double
	 */
	public double getShieldDefRate()
	{
		return _shieldDefRate;
	}
	
	/**
	 * Returns passive skill linked to that weapon
	 * @return
	 */
	public String[] getSkills()
	{
		return _skill;
	}
	
	/**
	* Returns skill that player get when has equiped weapon +4  or more  (for duals SA)
	* @return
	*/
	public L2Skill getEnchant4Skill()
	{
		return _enchant4Skill;
	}
	
	/**
	 * Returns the Id in wich weapon this weapon can be changed
	 * @return
	 */
	public int getChangeWeaponId()
	{
		return _changeWeaponId;
	}
	
	/**
	 * Returns array of Func objects containing the list of functions used by the weapon
	 * @param instance : L2ItemInstance pointing out the weapon
	 * @param player : L2Character pointing out the player
	 * @return Func[] : array of functions
	 */
	@Override
	public Func[] getStatFuncs(L2ItemInstance instance, L2Character player)
	{
		if (_funcTemplates != null || _funcTemplates.length == 0)
			return _emptyFunctionSet;
		
		ArrayList<Func> funcs = new ArrayList<Func>(_funcTemplates.length);
		
		Env env = new Env();
		env.player = player;
		env.item = instance;
		Func f;
		
		for (FuncTemplate t : _funcTemplates)
		{
			f = t.getFunc(env, instance);
			if (f != null)
				funcs.add(f);
		}
		
		return funcs.toArray(new Func[funcs.size()]);
	}
	
	/**
	 * Returns effects of skills associated with the item to be triggered onHit.
	 * @param caster : L2Character pointing out the caster
	 * @param target : L2Character pointing out the target
	 * @param crit : boolean tells whether the hit was critical
	 * @return L2Effect[] : array of effects generated by the skill
	 */
	public L2Effect[] getSkillEffects(L2Character caster, L2Character target, boolean crit)
	{
		if (_skillsOnCrit == null || !crit)
			return _emptyEffectSet;
		List<L2Effect> effects = new FastList<L2Effect>();
		Env env = new Env();
		env.player = caster;
		env.target = target;
		env.skill = _skillsOnCrit;
		if (!_skillsOnCritCondition.test(env))
			return _emptyEffectSet; // Skill condition not met
			
		byte shld = Formulas.calcShldUse(caster, target, _skillsOnCrit);
		if (!Formulas.calcSkillSuccess(caster, target, _skillsOnCrit, shld, false, false, false))
			return _emptyEffectSet; // These skills should not work on RaidBoss
		if (target.getFirstEffect(_skillsOnCrit.getId()) != null)
			target.getFirstEffect(_skillsOnCrit.getId()).exit();
		for (L2Effect e : _skillsOnCrit.getEffects(caster, target, new Env(shld, false, false, false)))
			effects.add(e);
		if (effects.isEmpty())
			return _emptyEffectSet;
		return effects.toArray(new L2Effect[effects.size()]);
	}
	
	/**
	 * Returns effects of skills associated with the item to be triggered onCast.
	 * @param caster : L2Character pointing out the caster
	 * @param target : L2Character pointing out the target
	 * @param trigger : L2Skill pointing out the skill triggering this action
	 * @return L2Effect[] : array of effects generated by the skill
	 */
	public L2Effect[] getSkillEffects(L2Character caster, L2Character target, L2Skill trigger)
	{
		if (_skillsOnCast == null)
			return _emptyEffectSet;
		if (trigger.isOffensive() != _skillsOnCast.isOffensive())
			return _emptyEffectSet; // Trigger only same type of skill
		if (trigger.isToggle() && _skillsOnCast.getSkillType() == L2SkillType.BUFF)
			return _emptyEffectSet; // No buffing with toggle skills
		if (!trigger.isMagic() && _skillsOnCast.getSkillType() == L2SkillType.BUFF)
			return _emptyEffectSet; // No buffing with not magic skills
		
		Env env = new Env();
		env.player = caster;
		env.target = target;
		env.skill = _skillsOnCast;
		if (!_skillsOnCastCondition.test(env))
			return _emptyEffectSet;
		
		byte shld = Formulas.calcShldUse(caster, target, _skillsOnCast);
		if (_skillsOnCast.isOffensive() && !Formulas.calcSkillSuccess(caster, target, _skillsOnCast, shld, false, false, false))
			return _emptyEffectSet;
		
		try
		{
			// Get the skill handler corresponding to the skill type
			ISkillHandler handler = SkillHandler.getInstance().getSkillHandler(_skillsOnCast.getSkillType());
			
			L2Character[] targets = new L2Character[1];
			targets[0] = target;
			
			// Launch the magic skill and calculate its effects
			if (handler != null)
				handler.useSkill(caster, _skillsOnCast, targets);
			else
				_skillsOnCast.useSkill(caster, targets);
			
			// notify quests of a skill use
			if (caster instanceof L2PcInstance)
			{
				// Mobs in range 1000 see spell
				Collection<L2Object> objs = caster.getKnownList().getKnownObjects().values();
				//synchronized (caster.getKnownList().getKnownObjects())
				{
					for (L2Object spMob : objs)
					{
						if (spMob instanceof L2Npc)
						{
							L2Npc npcMob = (L2Npc) spMob;
							
							if (npcMob.getTemplate().getEventQuests(Quest.QuestEventType.ON_SKILL_SEE) != null)
								for (Quest quest : npcMob.getTemplate().getEventQuests(Quest.QuestEventType.ON_SKILL_SEE))
									quest.notifySkillSee(npcMob, (L2PcInstance) caster, _skillsOnCast, targets, false);
						}
					}
				}
			}
		}
		catch (IOException e)
		{
			e.printStackTrace(); // IO ?!
		}
		return _emptyEffectSet;
	}
	
	/**
	 * Add the L2Skill skill to the list of skills generated by the item triggered by critical hit
	 * @param skill : L2Skill
	 */
	public void attachOnCrit(L2Skill skill, int chance)
	{
		if (_skillsOnCrit == null)
		{
			_skillsOnCrit = skill;
			_skillsOnCritCondition = new ConditionGameChance(chance);
		}
	}
	
	/**
	 * Add the L2Skill skill to the list of skills generated by the item triggered by casting spell
	 * @param skill : L2Skill
	 */
	public void attachOnCast(L2Skill skill, int chance)
	{
		_skillsOnCast = skill;
		_skillsOnCastCondition = new ConditionGameChance(chance);
	}
}
