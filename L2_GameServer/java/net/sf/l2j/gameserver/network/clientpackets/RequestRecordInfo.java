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
package net.sf.l2j.gameserver.network.clientpackets;

import java.util.Collection;

import net.sf.l2j.gameserver.TaskPriority;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2BoatInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2DoorInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2StaticObjectInstance;
import net.sf.l2j.gameserver.network.serverpackets.CharInfo;
import net.sf.l2j.gameserver.network.serverpackets.GetOnVehicle;
import net.sf.l2j.gameserver.network.serverpackets.NpcInfo;
import net.sf.l2j.gameserver.network.serverpackets.PetItemList;
import net.sf.l2j.gameserver.network.serverpackets.RelationChanged;
import net.sf.l2j.gameserver.network.serverpackets.ServerObjectInfo;
import net.sf.l2j.gameserver.network.serverpackets.SpawnItem;
import net.sf.l2j.gameserver.network.serverpackets.StaticObject;
import net.sf.l2j.gameserver.network.serverpackets.UserInfo;
import net.sf.l2j.gameserver.network.serverpackets.VehicleInfo;

public class RequestRecordInfo extends L2GameClientPacket
{
	private static final String _0__CF_REQUEST_RECORD_INFO = "[0] CF RequestRecordInfo";

	/** urgent messages, execute immediatly */
	public TaskPriority getPriority() { return TaskPriority.PR_NORMAL; }

	@Override
	protected void readImpl()
	{
		// trigger
	}

    @Override
	protected void runImpl()
	{
		L2PcInstance _activeChar = getClient().getActiveChar();

		if (_activeChar == null)
			return;

		_activeChar.sendPacket(new UserInfo(_activeChar));

		Collection<L2Object> objs = _activeChar.getKnownList().getKnownObjects().values();
		//synchronized (_activeChar.getKnownList().getKnownObjects())
		{
			for (L2Object object : objs)
			{
				if (object.getPoly().isMorphed()
				        && object.getPoly().getPolyType().equals("item"))
					_activeChar.sendPacket(new SpawnItem(object));
				else
				{
					if (object instanceof L2ItemInstance)
						_activeChar.sendPacket(new SpawnItem(object));
					else if (object instanceof L2DoorInstance)
					{
						_activeChar.sendPacket(new StaticObject((L2DoorInstance) object, false));
					}
					else if (object instanceof L2BoatInstance)
					{
						if (!_activeChar.isInBoat()
						        && object != _activeChar.getBoat())
						{
							_activeChar.sendPacket(new VehicleInfo((L2BoatInstance) object));
							((L2BoatInstance) object).sendVehicleDeparture(_activeChar);
						}
					}
					else if (object instanceof L2StaticObjectInstance)
						_activeChar.sendPacket(new StaticObject((L2StaticObjectInstance) object));
					else if (object instanceof L2NpcInstance)
					{
						if (((L2NpcInstance) object).getRunSpeed() == 0)
							_activeChar.sendPacket(new ServerObjectInfo((L2NpcInstance) object, _activeChar));
						else
							_activeChar.sendPacket(new NpcInfo((L2NpcInstance) object, _activeChar));
					}
					else if (object instanceof L2Summon)
					{
						L2Summon summon = (L2Summon) object;
						
						// Check if the L2PcInstance is the owner of the Pet
						if (_activeChar.equals(summon.getOwner()))
						{
							summon.broadcastStatusUpdate();
							
							if (summon instanceof L2PetInstance)
								_activeChar.sendPacket(new PetItemList((L2PetInstance) summon));
						}
						else
							_activeChar.sendPacket(new NpcInfo(summon, _activeChar,1));
						
						// The PetInfo packet wipes the PartySpelled (list of
						// active spells' icons). Re-add them
						summon.updateEffectIcons(true);
					}
					else if (object instanceof L2PcInstance)
					{
						L2PcInstance otherPlayer = (L2PcInstance) object;
						
						if (otherPlayer.isInBoat())
						{
							otherPlayer.getPosition().setWorldPosition(otherPlayer.getBoat().getPosition().getWorldPosition());
							_activeChar.sendPacket(new CharInfo(otherPlayer));
							int relation = otherPlayer.getRelation(_activeChar);
							
							if (otherPlayer.getPet() != null)
								_activeChar.sendPacket(new RelationChanged(otherPlayer.getPet(), relation, _activeChar.isAutoAttackable(otherPlayer)));
							
							if (otherPlayer.getKnownList().getKnownRelations().get(_activeChar.getObjectId()) != null
							        && otherPlayer.getKnownList().getKnownRelations().get(_activeChar.getObjectId()) != relation)
							{
								_activeChar.sendPacket(new RelationChanged(otherPlayer, relation, _activeChar.isAutoAttackable(otherPlayer)));
								if (otherPlayer.getPet() != null)
									_activeChar.sendPacket(new RelationChanged(otherPlayer.getPet(), relation, _activeChar.isAutoAttackable(otherPlayer)));
							}
							_activeChar.sendPacket(new GetOnVehicle(otherPlayer, otherPlayer.getBoat(), otherPlayer.getInBoatPosition().getX(), otherPlayer.getInBoatPosition().getY(), otherPlayer.getInBoatPosition().getZ()));
						}
						else
						{
							_activeChar.sendPacket(new CharInfo(otherPlayer));
							int relation = otherPlayer.getRelation(_activeChar);
							
							if (otherPlayer.getPet() != null)
								_activeChar.sendPacket(new RelationChanged(otherPlayer.getPet(), relation, _activeChar.isAutoAttackable(otherPlayer)));
							
							if (otherPlayer.getKnownList().getKnownRelations().get(_activeChar.getObjectId()) != null
							        && otherPlayer.getKnownList().getKnownRelations().get(_activeChar.getObjectId()) != relation)
							{
								_activeChar.sendPacket(new RelationChanged(otherPlayer, relation, _activeChar.isAutoAttackable(otherPlayer)));
								if (otherPlayer.getPet() != null)
									_activeChar.sendPacket(new RelationChanged(otherPlayer.getPet(), relation, _activeChar.isAutoAttackable(otherPlayer)));
							}
						}
					}
					
					if (object instanceof L2Character)
					{
						// Update the state of the L2Character object client
						// side by sending Server->Client packet
						// MoveToPawn/CharMoveToLocation and AutoAttackStart to
						// the L2PcInstance
						L2Character obj = (L2Character) object;
						obj.getAI().describeStateToPlayer(_activeChar);
					}
				}
			}
		}
	}

	@Override
	public String getType()
	{
		return _0__CF_REQUEST_RECORD_INFO;
	}
}
