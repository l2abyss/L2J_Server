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

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.L2Attackable;
import net.sf.l2j.gameserver.model.L2CharPosition;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.actor.knownlist.CommanderKnownList;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;

public class L2CommanderInstance extends L2Attackable
{
    private int _homeX;
    private int _homeY;
    private int _homeZ;

    public L2CommanderInstance(int objectId, L2NpcTemplate template)
    {
        super(objectId, template);
        getKnownList(); // init knownlist
    }

    /**
     * Return True if a siege is in progress and the L2Character attacker isn't a Defender.<BR><BR>
     *
     * @param attacker The L2Character that the L2CommanderInstance try to attack
     *
     */
    @Override
    public boolean isAutoAttackable(L2Character attacker)
    {

        boolean isFort = ( getFort() != null && getFort().getFortId() > 0 
                && getFort().getSiege().getIsInProgress()
                && !getFort().getSiege().checkIsDefender(((L2PcInstance)attacker).getClan()));
        
        // Attackable during siege by all except defenders
        return (attacker != null
                && attacker instanceof L2PcInstance
                && isFort );
    }
    
    @Override
    public final CommanderKnownList getKnownList()
    {
        if(super.getKnownList() == null || !(super.getKnownList() instanceof CommanderKnownList))
            setKnownList(new CommanderKnownList(this));
        return (CommanderKnownList)super.getKnownList();
    }
    
    @Override
    public void addDamageHate(L2Character attacker, int damage, int aggro)
    {
        if (attacker == null)
            return;

        if (!(attacker instanceof L2CommanderInstance))
        {
            super.addDamageHate(attacker, damage, aggro);
        }
    }
    
    @Override
    public boolean doDie(L2Character killer)
    {
        if (!super.doDie(killer))
            return false;
        
        if (getFort().getSiege().getIsInProgress())
        {
            getFort().getSiege().killedCommander(this);

        }
		
        return true;
    }
    
    /**
     * Sets home location of guard. Guard will always try to return to this location after
     * it has killed all PK's in range.
     *
     */
    public void getHomeLocation()
    {
        _homeX = getX();
        _homeY = getY();
        _homeZ = getZ();

        if (Config.DEBUG)
            _log.finer(getObjectId()+": Home location set to"+
                    " X:" + _homeX + " Y:" + _homeY + " Z:" + _homeZ);
    }

    public int getHomeX()
    {
        return _homeX;
    }

    public int getHomeY()
    {
        return _homeY;
    }

    /**
     * This method forces guard to return to home location previously set
     *
     */
    public void returnHome()
    {
        if (!isInsideRadius(_homeX, _homeY, 40, false))
        {
            if (Config.DEBUG) _log.fine(getObjectId()+": moving home");
            setisReturningToSpawnPoint(true);    
            clearAggroList();
            
            if (hasAI())
                getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition(_homeX, _homeY, _homeZ, 0));
        }
    }
    
}
