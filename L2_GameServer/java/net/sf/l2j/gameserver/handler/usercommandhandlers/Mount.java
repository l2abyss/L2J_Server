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

package net.sf.l2j.gameserver.handler.usercommandhandlers;

import net.sf.l2j.gameserver.handler.IUserCommandHandler;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;

/**
 * Support for /mount command.
 * @author Tempy
 */
public class Mount implements IUserCommandHandler
{
    private static final int[] COMMAND_IDS = { 61 };
    
    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.handler.IUserCommandHandler#useUserCommand(int, net.sf.l2j.gameserver.model.L2PcInstance)
     */
    public synchronized boolean useUserCommand(int id, L2PcInstance activeChar)
    {
        if (id != COMMAND_IDS[0])
            return false;
        
        L2Summon pet = activeChar.getPet();
        
        if ((pet != null) && pet.isMountable() && !activeChar.isMounted())
        {
            if (activeChar.isTransformed())
            {
                // You cannot mount a steed while transformed.
                SystemMessage msg = new SystemMessage(SystemMessageId.YOU_CANNOT_MOUNT_A_STEED_WHILE_TRANSFORMED);
                activeChar.sendPacket(msg);
                return false;
            }
            else if (activeChar.isParalyzed())
            {
                // You cannot mount a steed while petrified.
                SystemMessage msg = new SystemMessage(SystemMessageId.YOU_CANNOT_MOUNT_A_STEED_WHILE_PETRIFIED);
                activeChar.sendPacket(msg);
                return false;
            }
            else if (activeChar.isDead())
            {
                // You cannot mount a steed while dead.
                SystemMessage msg = new SystemMessage(SystemMessageId.YOU_CANNOT_MOUNT_A_STEED_WHILE_DEAD);
                activeChar.sendPacket(msg);
                return false;
            }
            else if (activeChar.isFishing())
            {
                // You cannot mount a steed while fishing.
                SystemMessage msg = new SystemMessage(SystemMessageId.YOU_CANNOT_MOUNT_A_STEED_WHILE_FISHING);
                activeChar.sendPacket(msg);
                return false;
            }
            else if (activeChar.isInDuel())
            {
                // You cannot mount a steed while in a duel.
                SystemMessage msg = new SystemMessage(SystemMessageId.YOU_CANNOT_MOUNT_A_STEED_WHILE_IN_A_DUEL);
                activeChar.sendPacket(msg);
                return false;
            }
            else if (activeChar.isSitting())
            {
                // You cannot mount a steed while sitting.
                SystemMessage msg = new SystemMessage(SystemMessageId.YOU_CANNOT_MOUNT_A_STEED_WHILE_SITTING);
                activeChar.sendPacket(msg);
                return false;
            }
            else if (activeChar.isCastingNow())
            {
                // You cannot mount a steed while skill casting.
                SystemMessage msg = new SystemMessage(SystemMessageId.YOU_CANNOT_MOUNT_A_STEED_WHILE_SKILL_CASTING);
                activeChar.sendPacket(msg);
                return false;
            }
            else if (activeChar.isCursedWeaponEquipped())
            {
                // You cannot mount a steed while a cursed weapon is equipped.
                SystemMessage msg = new SystemMessage(SystemMessageId.YOU_CANNOT_MOUNT_A_STEED_WHILE_A_CURSED_WEAPON_IS_EQUIPPED);
                activeChar.sendPacket(msg);
                return false;
            }
            /** 
             * TODO: Add Siege Flag Restriction, 
             else if (activeChar.isFlagEquipped())
            {
                // You cannot mount a steed while holding a flag.
                SystemMessage msg = new SystemMessage(SystemMessageId.YOU_CANNOT_MOUNT_A_STEED_WHILE_HOLDING_A_FLAG);
                activeChar.sendPacket(msg);
            }
             */
            else if (activeChar.isInCombat() || activeChar.getPvpFlag() != 0)
            {
                // A pet cannot be ridden while player is in battle.
                SystemMessage msg = new SystemMessage(SystemMessageId.STRIDER_CANT_BE_RIDDEN_WHILE_IN_BATTLE);
                activeChar.sendPacket(msg);
                return false;
            }
            else if (activeChar.isRentedPet())
            {
                activeChar.stopRentPet();
                return false;
            }
            else if (activeChar.isMoving()
                    || activeChar.isInsideZone(L2Character.ZONE_WATER))
            {
                // A strider can be ridden only when player is standing.
                SystemMessage msg = new SystemMessage(SystemMessageId.STRIDER_CAN_BE_RIDDEN_ONLY_WHILE_STANDING);
                activeChar.sendPacket(msg);
                return false;
            }
            else if (pet.isInCombat())
            {
                // A strider in battle cannot be ridden.
                SystemMessage msg = new SystemMessage(SystemMessageId.STRIDER_IN_BATLLE_CANT_BE_RIDDEN);
                activeChar.sendPacket(msg);
                return false;
            }
            else if (pet.isDead())
            {
                // A dead strider cannot be ridden.
                SystemMessage msg = new SystemMessage(SystemMessageId.DEAD_STRIDER_CANT_BE_RIDDEN);
                activeChar.sendPacket(msg);
                return false;
            }
            else
                activeChar.mount(pet);
        }
        
        return true;
    }
    
    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.handler.IUserCommandHandler#getUserCommandList()
     */
    public int[] getUserCommandList()
    {
        return COMMAND_IDS;
    }
}
