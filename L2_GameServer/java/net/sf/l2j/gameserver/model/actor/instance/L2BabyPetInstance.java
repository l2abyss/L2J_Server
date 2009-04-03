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
package net.sf.l2j.gameserver.model.actor.instance;

import java.util.concurrent.Future;

import javolution.util.FastList;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.PetSkillsTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;
import net.sf.l2j.util.Rnd;

/**
 *
 * This class ...
 *
 * @version $Revision: 1.15.2.10.2.16 $ $Date: 2005/04/06 16:13:40 $
 */
public final class L2BabyPetInstance extends L2PetInstance
{
	private FastList<Integer> _skillIds = new FastList<Integer>();
    private Future<?> _healingTask;
    protected static int _strongHeal = 4718;
    protected static int _weakHeal = 4717;

	public L2BabyPetInstance(int objectId, L2NpcTemplate template, L2PcInstance owner, L2ItemInstance control)
	{
		super(objectId, template, owner, control);

		FastList<Integer> skillIds = PetSkillsTable.getInstance().getAvailableSkills(this);
		_skillIds.addAll(skillIds);

		// start the healing task
		_healingTask = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new Heal(this), 3000, 1000);
	}

	@Override
	public boolean doDie(L2Character killer) {

		if (!super.doDie(killer))
			return false;

		if (_healingTask != null)
		{
			_healingTask.cancel(false);
			_healingTask = null;
		}
		return true;
	}

	@Override
	public synchronized void unSummon (L2PcInstance owner)
    {
		super.unSummon(owner);

		if (_healingTask != null)
		{
			_healingTask.cancel(false);
			_healingTask = null;
		}
    }

    @Override
	public void doRevive()
    {
    	super.doRevive();
		if (_healingTask == null)
			_healingTask = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new Heal(this), 0, 1000);
    }

    private class Heal implements Runnable
    {
    	private L2BabyPetInstance _baby;
    	public Heal(L2BabyPetInstance baby)
    	{
    		_baby = baby;
    	}

        public void run()
        {
        	L2PcInstance owner = _baby.getOwner();

        	// if the owner is dead, merely wait for the owner to be resurrected
        	// if the pet is still casting from the previous iteration, allow the cast to complete...
            if (!owner.isDead() && !_baby.isCastingNow() && !_baby.isBetrayed())
            {
            	// casting automatically stops any other action (such as autofollow or a move-to).
            	// We need to gather the necessary info to restore the previous state.
            	boolean previousFollowStatus = _baby.getFollowStatus();

            	// if the owner's HP is more than 80%, do nothing.
            	// if the owner's HP is very low (less than 20%) have a high chance for strong heal
            	// otherwise, have a low chance for weak heal
            	L2Skill skill = null;
            	if ((owner.getCurrentHp()/owner.getMaxHp() < 0.15) && Rnd.get(100) <= 75)
	        		skill = SkillTable.getInstance().getInfo(_strongHeal, PetSkillsTable.getInstance().getAvailableLevel(L2BabyPetInstance.this, _strongHeal));
            	if (((owner.getCurrentHp()/owner.getMaxHp() < 0.8) && Rnd.get(100) <= 25)&& skill == null)
            		skill = SkillTable.getInstance().getInfo(_weakHeal, PetSkillsTable.getInstance().getAvailableLevel(L2BabyPetInstance.this, _weakHeal));
            	if (skill != null)
            		_baby.useMagic(skill,false,false);
            	// calling useMagic changes the follow status, if the babypet actually casts
            	// (as opposed to failing due some factors, such as too low MP, etc).
            	// if the status has actually been changed, revert it.  Else, allow the pet to
            	// continue whatever it was trying to do.
            	// NOTE: This is important since the pet may have been told to attack a target.
            	// reverting the follow status will abort this attack!  While aborting the attack
            	// in order to heal is natural, it is not acceptable to abort the attack on its own,
            	// merely because the timer stroke and without taking any other action...
            	if(previousFollowStatus != _baby.getFollowStatus())
            		setFollowStatus(previousFollowStatus);
            }
        }
    }
}
