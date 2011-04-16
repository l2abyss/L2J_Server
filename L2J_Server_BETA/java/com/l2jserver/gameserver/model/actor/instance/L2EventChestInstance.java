package com.l2jserver.gameserver.model.actor.instance;

import java.util.Collection;

import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.network.serverpackets.AbstractNpcInfo;
import com.l2jserver.gameserver.network.serverpackets.L2GameServerPacket;
import com.l2jserver.gameserver.templates.chars.L2NpcTemplate;

/**
 ** @author Gnacik
 **
 */
public final class L2EventChestInstance extends L2EventMonsterInstance
{
	private boolean _isVisible = false;
	private boolean _isTriggered = false;
	
	public L2EventChestInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
		
		setIsNoRndWalk(true);
		disableCoreAI(true);
		
		eventSetDropOnGround(true);
		eventSetBlockOffensiveSkills(true);
	}
	
	public boolean canSee(L2Character cha)
	{
		if(cha == null)
			return false;
		if(cha.isGM())
			return true;
		return _isVisible;
	}
	
	public void trigger()
	{
		_isTriggered = true;
		broadcastPacket(new AbstractNpcInfo.NpcInfo(this, null));
	}
	
	@Override
	public void sendInfo(L2PcInstance activeChar)
	{
		if (_isTriggered || canSee(activeChar))
			activeChar.sendPacket(new AbstractNpcInfo.NpcInfo(this, activeChar));
	}
	
	@Override
	public void broadcastPacket(L2GameServerPacket mov)
	{
		Collection<L2PcInstance> plrs = getKnownList().getKnownPlayers().values();
		for (L2PcInstance player : plrs)
			if (player != null && (_isTriggered || canSee(player)))
				player.sendPacket(mov);
	}
	
	@Override
	public void broadcastPacket(L2GameServerPacket mov, int radiusInKnownlist)
	{
		Collection<L2PcInstance> plrs = getKnownList().getKnownPlayers().values();
		for (L2PcInstance player : plrs)
		{
			if (player == null)
				continue;
			if (isInsideRadius(player, radiusInKnownlist, false, false))
				if (_isTriggered || canSee(player))
					player.sendPacket(mov);
		}
	}
	
	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		return !canSee(attacker);
	}
}