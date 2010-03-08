/*
 * $Header: /cvsroot/l2j/L2_Gameserver/java/net/sf/l2j/gameserver/model/L2StaticObjectInstance.java,v 1.3.2.2.2.2 2005/02/04 13:05:27 maximas Exp $
 *
 *
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
package com.l2jserver.gameserver.model.actor.instance;

import java.util.logging.Logger;

import com.l2jserver.gameserver.ai.CtrlIntention;
import com.l2jserver.gameserver.ai.L2CharacterAI;
import com.l2jserver.gameserver.cache.HtmCache;
import com.l2jserver.gameserver.model.L2CharPosition;
import com.l2jserver.gameserver.model.L2ItemInstance;
import com.l2jserver.gameserver.model.L2Skill;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.knownlist.StaticObjectKnownList;
import com.l2jserver.gameserver.model.actor.stat.StaticObjStat;
import com.l2jserver.gameserver.model.actor.status.StaticObjStatus;
import com.l2jserver.gameserver.network.serverpackets.ActionFailed;
import com.l2jserver.gameserver.network.serverpackets.MyTargetSelected;
import com.l2jserver.gameserver.network.serverpackets.NpcHtmlMessage;
import com.l2jserver.gameserver.network.serverpackets.ShowTownMap;
import com.l2jserver.gameserver.network.serverpackets.StaticObject;
import com.l2jserver.gameserver.templates.chars.L2CharTemplate;
import com.l2jserver.gameserver.templates.item.L2Weapon;
import com.l2jserver.gameserver.util.StringUtil;

/**
 * GODSON ROX!
 */
public class L2StaticObjectInstance extends L2Character
{
	protected static final Logger log = Logger.getLogger(L2StaticObjectInstance.class.getName());

    /** The interaction distance of the L2StaticObjectInstance */
    public static final int INTERACTION_DISTANCE = 150;

    private int _staticObjectId;
    private int _meshIndex = 0;     // 0 - static objects, alternate static objects
    private int _type = -1;         // 0 - map signs, 1 - throne , 2 - arena signs
    private int _x;
    private int _y;
    private String _texture;

    /** This class may be created only by L2Character and only for AI */
    public class AIAccessor extends L2Character.AIAccessor
    {
        protected AIAccessor() {}
        @Override
		public L2StaticObjectInstance getActor() { return L2StaticObjectInstance.this; }
        @Override
        public void moveTo(int x, int y, int z, int offset) {}
        @Override
        public void moveTo(int x, int y, int z) {}
        @Override
        public void stopMove(L2CharPosition pos) {}
        @Override
        public void doAttack(L2Character target) {}
        @Override
        public void doCast(L2Skill skill) {}
    }

    @Override
	public L2CharacterAI getAI() 
    {
    	return null;
    }
    /**
     * @return Returns the StaticObjectId.
     */
    public int getStaticObjectId()
    {
        return _staticObjectId;
    }

    /**
     */
    public L2StaticObjectInstance(int objectId,L2CharTemplate template, int staticId)
    {
    	super(objectId, template);
    	_staticObjectId = staticId;
    }

    @Override
	public final StaticObjectKnownList getKnownList()
    {
    	return (StaticObjectKnownList)super.getKnownList();
    }
    
	@Override
    public void initKnownList()
    {
		setKnownList(new StaticObjectKnownList(this));
    }

    @Override
	public final StaticObjStat getStat()
    {
    	return (StaticObjStat)super.getStat();
    }
	
	@Override
	public void initCharStat()
	{
		setStat(new StaticObjStat(this));
	}

    @Override
	public final StaticObjStatus getStatus()
    {
    	return (StaticObjStatus)super.getStatus();
    }
	
	@Override
	public void initCharStatus()
	{
		setStatus(new StaticObjStatus(this));
	}

    public int getType()
    {
        return _type;
    }

    public void setType(int type)
    {
        _type = type;
    }

    public void setMap(String texture, int x, int y)
    {
        _texture = "town_map."+texture;
        _x = x;
        _y = y;
    }

    private int getMapX()
    {
	return _x;
    }

    private int getMapY()
    {
	return _y;
    }

    @Override
	public final int getLevel()
    {
        return 1;
    }

    /**
     * Return null.<BR><BR>
     */
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

    /**
     * this is called when a player interacts with this NPC
     * @param player
     */
    @Override
    public void onAction(L2PcInstance player, boolean interact)
    {
    	if(_type < 0) _log.info("L2StaticObjectInstance: StaticObject with invalid type! StaticObjectId: "+getStaticObjectId());
        // Check if the L2PcInstance already target the L2NpcInstance
        if (this != player.getTarget())
        {
            // Set the target of the L2PcInstance player
            player.setTarget(this);
            player.sendPacket(new MyTargetSelected(getObjectId(), 0));

        }
        else if (interact)
        {
            MyTargetSelected my = new MyTargetSelected(getObjectId(), 0);
            player.sendPacket(my);

            // Calculate the distance between the L2PcInstance and the L2NpcInstance
            if (!player.isInsideRadius(this, INTERACTION_DISTANCE, false, false))
            {
                // Notify the L2PcInstance AI with AI_INTENTION_INTERACT
                player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);

                // Send a Server->Client packet ActionFailed (target is out of interaction range) to the L2PcInstance player
                player.sendPacket(ActionFailed.STATIC_PACKET);
            } else {
			if(_type == 2) {
				String filename = "data/html/signboard.htm";
				String content = HtmCache.getInstance().getHtm(player.getHtmlPrefix(), filename);
				NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());

				if (content == null) html.setHtml("<html><body>Signboard is missing:<br>"+filename+"</body></html>");
				else html.setHtml(content);

				player.sendPacket(html);
				player.sendPacket(ActionFailed.STATIC_PACKET);
			} else if(_type == 0) player.sendPacket(new ShowTownMap(_texture, getMapX(), getMapY()));
                    // Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
                    player.sendPacket(ActionFailed.STATIC_PACKET);
            }
        }

    }

    @Override
    public void onActionShift(L2PcInstance player)
    {
        if (player == null) return;

        if (player.getAccessLevel().isGm())
        {
            player.setTarget(this);
            MyTargetSelected my = new MyTargetSelected(getObjectId(), player
                    .getLevel());
            player.sendPacket(my);

            StaticObject su = new StaticObject(this);

            player.sendPacket(su);

            NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
            final String html1 = StringUtil.concat(
                    "<html><body><center><font color=\"LEVEL\">Static Object Info</font></center><br><table border=0><tr><td>Coords X,Y,Z: </td><td>",
                    String.valueOf(getX()),
                    ", ",
                    String.valueOf(getY()),
                    ", ",
                    String.valueOf(getZ()),
                    "</td></tr><tr><td>Object ID: </td><td>",
                    String.valueOf(getObjectId()),
                    "</td></tr><tr><td>Static Object ID: </td><td>",
                    String.valueOf(getStaticObjectId()),
                    "</td></tr><tr><td>Mesh Index: </td><td>",
                    String.valueOf(getMeshIndex()),
                    "</td></tr><tr><td><br></td></tr><tr><td>Class: </td><td>",
                    getClass().getSimpleName(),
                    "</td></tr></table></body></html>"
                    );
            html.setHtml(html1);
            player.sendPacket(html);
        } else {
            // ATTACK the mob without moving?
        }

        player.sendPacket(ActionFailed.STATIC_PACKET);
    }

    /* (non-Javadoc)
     * @see com.l2jserver.gameserver.model.L2Object#isAttackable()
     */
    @Override
    public boolean isAutoAttackable(L2Character attacker)
    {
        return false;
    }

    /**
     * Set the meshIndex of the object<BR><BR>
     * 
     * <B><U> Values </U> :</B><BR><BR>
     * <li> default textures : 0</li>
     * <li> alternate textures : 1 </li><BR><BR>
     * @param meshIndex
     */
    public void setMeshIndex (int meshIndex)
    {
    	_meshIndex = meshIndex;
    	this.broadcastPacket(new StaticObject(this));
    }

    /**
     * Return the meshIndex of the object.<BR><BR>
     *
     * <B><U> Values </U> :</B><BR><BR>
     * <li> default textures : 0</li>
     * <li> alternate textures : 1 </li><BR><BR>
     *
     */
    public int getMeshIndex()
    {
    	return _meshIndex;
    }

	@Override
	public void updateAbnormalEffect() {}
	
    @Override
    public void sendInfo(L2PcInstance activeChar)
    {
    	activeChar.sendPacket(new StaticObject(this));
    }
}
