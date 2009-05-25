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
package net.sf.l2j.gameserver.skills.l2skills;

import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.templates.StatsSet;
import net.sf.l2j.gameserver.templates.item.L2WeaponType;

public class L2SkillChargeDmg extends L2Skill
{

	public L2SkillChargeDmg(StatsSet set)
    {
		super(set);
	}

	@Override
	public void useSkill(L2Character caster, L2Object[] targets)
    {
		if (caster.isAlikeDead())
        {
			return;
        }

		double modifier = 0;
		if (caster instanceof L2PcInstance)
        {
			// thanks Diego Vargas of L2Guru: 70*((0.8+0.201*No.Charges) * (PATK+POWER)) / PDEF
			modifier = 0.8+0.201*(getNumCharges()+((L2PcInstance)caster).getCharges());
        }
		for (L2Character target: (L2Character[]) targets)
		{
			L2ItemInstance weapon = caster.getActiveWeaponInstance();
			if (target.isAlikeDead())
				continue;
        	
			//	Calculate skill evasion
			boolean skillIsEvaded = Formulas.calcPhysicalSkillEvasion(target, this);
			if(skillIsEvaded)
			{
				if (caster instanceof L2PcInstance)
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.C1_DODGES_ATTACK);
					sm.addString(target.getName());
					((L2PcInstance) caster).sendPacket(sm);
				}
				if (target instanceof L2PcInstance)
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.AVOIDED_C1_ATTACK2);
					sm.addString(caster.getName());
					((L2PcInstance) target).sendPacket(sm);
				}
				
				//no futher calculations needed. 
				continue;
			}
        	
        	
			// TODO: should we use dual or not?
			// because if so, damage are lowered but we don't do anything special with dual then
			// like in doAttackHitByDual which in fact does the calcPhysDam call twice
			//boolean dual  = caster.isUsingDualWeapon();
			byte shld = Formulas.calcShldUse(caster, target);
			boolean crit = false;
			if (this.getBaseCritRate() > 0)
				crit = Formulas.calcCrit(this.getBaseCritRate() * 10 * Formulas.getSTRBonus(caster), target);
			boolean soul = (weapon != null
							&& weapon.getChargedSoulshot() == L2ItemInstance.CHARGED_SOULSHOT
							&& weapon.getItemType() != L2WeaponType.DAGGER );

			// damage calculation, crit is static 2x
			int damage = (int)Formulas.calcPhysDam(caster, target, this, shld, false, false, soul);
			if (crit) damage *= 2;

			if (damage > 0)
            {
                double finalDamage = damage;
                finalDamage = finalDamage*modifier;
				target.reduceCurrentHp(finalDamage, caster, this);
				
				// vengeance reflected damage
				if ((Formulas.calcSkillReflect(target, this) & Formulas.SKILL_REFLECT_VENGEANCE) != 0)
					caster.reduceCurrentHp(damage, target, this);

				caster.sendDamageMessage(target, (int)finalDamage, false, crit, false);

				if (soul && weapon!= null)
					weapon.setChargedSoulshot(L2ItemInstance.CHARGED_NONE);
			}
            else
            {
				caster.sendDamageMessage(target, 0, false, false, true);
			}
		}
        // effect self :]
        L2Effect seffect = caster.getFirstEffect(getId());
        if (seffect != null && seffect.isSelfEffect())
        {
            //Replace old effect with new one.
            seffect.exit();
        }
        // cast self effect if any
        getEffectsSelf(caster);
	}

}
