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

package net.sf.l2j.gameserver.model;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.serverpackets.MyTargetSelected;
import net.sf.l2j.gameserver.serverpackets.NpcInfo;
import net.sf.l2j.gameserver.taskmanager.DecayTaskManager;
import net.sf.l2j.gameserver.templates.L2CharTemplate;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;
import net.sf.l2j.gameserver.templates.L2Weapon;

public abstract class L2Decoy extends L2Character
{
    private L2PcInstance _owner;
    
    public L2Decoy(int objectId, L2CharTemplate template, L2PcInstance owner)
    {
        super(objectId, template);
        getKnownList();
        getStat();
        getStatus();
        _owner = owner;
        setXYZInvisible(owner.getX(), owner.getY(), owner.getZ());
    }
    
    @Override
    public void onSpawn()
    {
        super.onSpawn();
        this.getOwner().sendPacket(new NpcInfo(this));
    }
    
    @Override
    public void onAction(L2PcInstance player)
    {
        player.setTarget(this);
        MyTargetSelected my = new MyTargetSelected(getObjectId(), player.getLevel()
                - getLevel());
        player.sendPacket(my);
    }
    
    @Override
    public void updateAbnormalEffect()
    {
        for (L2PcInstance player : getKnownList().getKnownPlayers().values())
            player.sendPacket(new NpcInfo(this));
    }
    
    public void stopDecay()
    {
        DecayTaskManager.getInstance().cancelDecayTask(this);
    }
    
    @Override
    public void onDecay()
    {
        deleteMe(_owner);
    }
    
    @Override
    public boolean isAutoAttackable(L2Character attacker)
    {
        return _owner.isAutoAttackable(attacker);
    }
    
    @Override
    public L2ItemInstance getActiveWeaponInstance()
    {
        return null;
    }
    
    @Override
    public L2Weapon getActiveWeaponItem()
    {
        return null;
    }
    
    @Override
    public L2ItemInstance getSecondaryWeaponInstance()
    {
        return null;
    }
    
    @Override
    public L2Weapon getSecondaryWeaponItem()
    {
        return null;
    }
    
    public final int getNpcId()
    {
        return getTemplate().npcId;
    }
    
    @Override
    public int getLevel()
    {
        return getTemplate().level;
    }
    
    public void deleteMe(L2PcInstance owner)
    {
        decayMe();
        getKnownList().removeAllKnownObjects();
        owner.setDecoy(null);
    }
    
    public synchronized void unSummon(L2PcInstance owner)
    {
        
        if (isVisible() && !isDead())
        {
            if (getWorldRegion() != null)
                getWorldRegion().removeFromZones(this);
            owner.setDecoy(null);
            decayMe();
            getKnownList().removeAllKnownObjects();
        }
    }
    
    public final L2PcInstance getOwner()
    {
        return _owner;
    }
    
    @Override
    public L2NpcTemplate getTemplate()
    {
        return (L2NpcTemplate) super.getTemplate();
    }
}
