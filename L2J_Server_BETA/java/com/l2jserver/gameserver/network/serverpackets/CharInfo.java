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
package com.l2jserver.gameserver.network.serverpackets;

import com.l2jserver.Config;
import com.l2jserver.gameserver.datatables.NpcTable;
import com.l2jserver.gameserver.instancemanager.CursedWeaponsManager;
import com.l2jserver.gameserver.model.PcCondOverride;
import com.l2jserver.gameserver.model.actor.L2Decoy;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.actor.templates.L2NpcTemplate;
import com.l2jserver.gameserver.model.effects.AbnormalEffect;
import com.l2jserver.gameserver.model.itemcontainer.Inventory;

public class CharInfo extends L2GameServerPacket
{
	private final L2PcInstance _activeChar;
	private final Inventory _inv;
	private int _objId;
	private int _x, _y, _z, _heading;
	private final int _mAtkSpd, _pAtkSpd;
	
	/**
	 * Run speed, swimming run speed and flying run speed
	 */
	private final int _runSpd;
	/**
	 * Walking speed, swimming walking speed and flying walking speed
	 */
	private final int _walkSpd;
	private final float _moveMultiplier, _attackSpeedMultiplier;
	
	private int _vehicleId, _airShipHelm;
	
	/**
	 * @param cha
	 */
	public CharInfo(L2PcInstance cha)
	{
		_activeChar = cha;
		_objId = cha.getObjectId();
		_inv = cha.getInventory();
		if ((_activeChar.getVehicle() != null) && (_activeChar.getInVehiclePosition() != null))
		{
			_x = _activeChar.getInVehiclePosition().getX();
			_y = _activeChar.getInVehiclePosition().getY();
			_z = _activeChar.getInVehiclePosition().getZ();
			_vehicleId = _activeChar.getVehicle().getObjectId();
			if (_activeChar.isInAirShip() && _activeChar.getAirShip().isCaptain(_activeChar))
			{
				_airShipHelm = _activeChar.getAirShip().getHelmItemId();
			}
			else
			{
				_airShipHelm = 0;
			}
		}
		else
		{
			_x = _activeChar.getX();
			_y = _activeChar.getY();
			_z = _activeChar.getZ();
			_vehicleId = 0;
			_airShipHelm = 0;
		}
		_heading = _activeChar.getHeading();
		_mAtkSpd = _activeChar.getMAtkSpd();
		_pAtkSpd = _activeChar.getPAtkSpd();
		_moveMultiplier = _activeChar.getMovementSpeedMultiplier();
		_attackSpeedMultiplier = _activeChar.getAttackSpeedMultiplier();
		_runSpd = (int) (_activeChar.getRunSpeed() / _moveMultiplier);
		_walkSpd = (int) (_activeChar.getWalkSpeed() / _moveMultiplier);
		_invisible = cha.getAppearance().getInvisible();
	}
	
	public CharInfo(L2Decoy decoy)
	{
		this(decoy.getActingPlayer()); // init
		_vehicleId = 0;
		_airShipHelm = 0;
		_objId = decoy.getObjectId();
		_x = decoy.getX();
		_y = decoy.getY();
		_z = decoy.getZ();
		_heading = decoy.getHeading();
	}
	
	@Override
	protected final void writeImpl()
	{
		boolean gmSeeInvis = false;
		
		if (_invisible)
		{
			L2PcInstance tmp = getClient().getActiveChar();
			if ((tmp != null) && tmp.canOverrideCond(PcCondOverride.SEE_ALL_PLAYERS))
			{
				gmSeeInvis = true;
			}
		}
		
		if (_activeChar.getPoly().isMorphed())
		{
			L2NpcTemplate template = NpcTable.getInstance().getTemplate(_activeChar.getPoly().getPolyId());
			
			if (template != null)
			{
				writeC(0x0c);
				writeD(_objId);
				writeD(template.getNpcId() + 1000000); // npctype id
				writeD(_activeChar.getKarma() > 0 ? 1 : 0);
				writeD(_x);
				writeD(_y);
				writeD(_z);
				writeD(_heading);
				writeD(0x00);
				writeD(_mAtkSpd);
				writeD(_pAtkSpd);
				writeD(_runSpd);
				writeD(_walkSpd);
				writeD(_runSpd); // swim run speed
				writeD(_walkSpd); // swim walk speed
				writeD(_runSpd); // fly run speed
				writeD(_walkSpd); // fly walk speed
				writeD(_runSpd); // fly run speed ?
				writeD(_walkSpd); // fly walk speed ?
				writeF(_moveMultiplier);
				writeF(_attackSpeedMultiplier);
				writeF(template.getfCollisionRadius());
				writeF(template.getfCollisionHeight());
				writeD(template.getRightHand()); // right hand weapon
				writeD(0x00); // chest
				writeD(template.getLeftHand()); // left hand weapon
				writeC(1); // name above char 1=true ... ??
				writeC(_activeChar.isRunning() ? 1 : 0);
				writeC(_activeChar.isInCombat() ? 1 : 0);
				writeC(_activeChar.isAlikeDead() ? 1 : 0);
				writeC(!gmSeeInvis && _invisible ? 1 : 0); // invisible ?? 0=false 1=true 2=summoned (only works if model has a summon animation)
				
				writeD(-1); // High Five NPCString ID
				writeS(_activeChar.getAppearance().getVisibleName());
				writeD(-1); // High Five NPCString ID
				writeS(gmSeeInvis ? "Invisible" : _activeChar.getAppearance().getVisibleTitle());
				
				writeD(_activeChar.getAppearance().getTitleColor()); // Title color 0=client default
				writeD(_activeChar.getPvpFlag()); // pvp flag
				writeD(_activeChar.getKarma()); // karma ??
				
				writeD(gmSeeInvis ? (_activeChar.getAbnormalEffect() | AbnormalEffect.STEALTH.getMask()) : _activeChar.getAbnormalEffect()); // C2
				
				writeD(_activeChar.getClanId()); // clan id
				writeD(_activeChar.getClanCrestId()); // crest id
				writeD(_activeChar.getAllyId()); // ally id
				writeD(_activeChar.getAllyCrestId()); // all crest
				
				writeC(_activeChar.isFlying() ? 2 : 0); // is Flying
				writeC(_activeChar.getTeam()); // C3 team circle 1-blue, 2-red
				
				writeF(template.getfCollisionRadius());
				writeF(template.getfCollisionHeight());
				
				writeD(0x00); // enchant effect
				writeD(_activeChar.isFlying() ? 2 : 0); // is Flying again?
				
				writeD(0x00);
				
				writeD(0x00); // CT1.5 Pet form and skills, Color effect
				writeC(template.getAIDataStatic().isTargetable() ? 0x01 : 0x00); // targetable
				writeC(template.getAIDataStatic().showName() ? 0x01 : 0x00); // show name
				writeC(_activeChar.getSpecialEffect());
				writeD(0x00);
			}
			else
			{
				_log.warning("Character " + _activeChar.getName() + " (" + _activeChar.getObjectId() + ") morphed in a Npc (" + _activeChar.getPoly().getPolyId() + ") w/o template.");
			}
		}
		else
		{
			writeC(0x31);
			writeD(_x);
			writeD(_y);
			writeD(_z);
			writeD(_vehicleId);
			writeD(_objId);
			writeS(_activeChar.getAppearance().getVisibleName());
			writeD(_activeChar.getRace().ordinal());
			writeD(_activeChar.getAppearance().getSex() ? 1 : 0);
			
			writeD(_activeChar.getBaseClass());
			
			writeD(_inv.getPaperdollItemDisplayId(Inventory.PAPERDOLL_UNDER));
			writeD(_inv.getPaperdollItemDisplayId(Inventory.PAPERDOLL_HEAD));
			
			writeD(_airShipHelm == 0 ? _inv.getPaperdollItemDisplayId(Inventory.PAPERDOLL_RHAND) : _airShipHelm);
			writeD(_airShipHelm == 0 ? _inv.getPaperdollItemDisplayId(Inventory.PAPERDOLL_LHAND) : 0);
			
			writeD(_inv.getPaperdollItemDisplayId(Inventory.PAPERDOLL_GLOVES));
			writeD(_inv.getPaperdollItemDisplayId(Inventory.PAPERDOLL_CHEST));
			writeD(_inv.getPaperdollItemDisplayId(Inventory.PAPERDOLL_LEGS));
			writeD(_inv.getPaperdollItemDisplayId(Inventory.PAPERDOLL_FEET));
			writeD(_inv.getPaperdollItemDisplayId(Inventory.PAPERDOLL_CLOAK));
			writeD(_inv.getPaperdollItemDisplayId(Inventory.PAPERDOLL_RHAND));
			writeD(_inv.getPaperdollItemDisplayId(Inventory.PAPERDOLL_HAIR));
			writeD(_inv.getPaperdollItemDisplayId(Inventory.PAPERDOLL_HAIR2));
			// T1 new d's
			writeD(_inv.getPaperdollItemDisplayId(Inventory.PAPERDOLL_RBRACELET));
			writeD(_inv.getPaperdollItemDisplayId(Inventory.PAPERDOLL_LBRACELET));
			writeD(_inv.getPaperdollItemDisplayId(Inventory.PAPERDOLL_DECO1));
			writeD(_inv.getPaperdollItemDisplayId(Inventory.PAPERDOLL_DECO2));
			writeD(_inv.getPaperdollItemDisplayId(Inventory.PAPERDOLL_DECO3));
			writeD(_inv.getPaperdollItemDisplayId(Inventory.PAPERDOLL_DECO4));
			writeD(_inv.getPaperdollItemDisplayId(Inventory.PAPERDOLL_DECO5));
			writeD(_inv.getPaperdollItemDisplayId(Inventory.PAPERDOLL_DECO6));
			writeD(_inv.getPaperdollItemDisplayId(Inventory.PAPERDOLL_BELT));
			// end of t1 new d's
			
			// c6 new h's
			writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_UNDER));
			writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_HEAD));
			
			writeD(_airShipHelm == 0 ? _inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_RHAND) : _airShipHelm);
			writeD(_airShipHelm == 0 ? _inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_LHAND) : 0);
			
			writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_GLOVES));
			writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_CHEST));
			writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_LEGS));
			writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_FEET));
			writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_CLOAK));
			writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_RHAND));
			writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_HAIR));
			writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_HAIR2));
			// T1 new h's
			writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_RBRACELET));
			writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_LBRACELET));
			writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_DECO1));
			writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_DECO2));
			writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_DECO3));
			writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_DECO4));
			writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_DECO5));
			writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_DECO6));
			writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_BELT));
			
			writeD(0x00); // ?
			writeD(0x01); // ?
			// end of t1 new h's
			
			writeD(_activeChar.getPvpFlag());
			writeD(_activeChar.getKarma());
			
			writeD(_mAtkSpd);
			writeD(_pAtkSpd);
			
			writeD(0x00); // ?
			
			writeD(_runSpd);
			writeD(_walkSpd);
			writeD(_runSpd); // swim run speed
			writeD(_walkSpd); // swim walk speed
			writeD(_runSpd); // fly run speed
			writeD(_walkSpd); // fly walk speed
			writeD(_runSpd); // fly run speed ?
			writeD(_walkSpd); // fly walk speed ?
			writeF(_activeChar.getMovementSpeedMultiplier()); // _activeChar.getProperMultiplier()
			writeF(_activeChar.getAttackSpeedMultiplier()); // _activeChar.getAttackSpeedMultiplier()
			
			writeF(_activeChar.getCollisionRadius());
			writeF(_activeChar.getCollisionHeight());
			
			writeD(_activeChar.getAppearance().getHairStyle());
			writeD(_activeChar.getAppearance().getHairColor());
			writeD(_activeChar.getAppearance().getFace());
			
			writeS(gmSeeInvis ? "Invisible" : _activeChar.getAppearance().getVisibleTitle());
			
			if (!_activeChar.isCursedWeaponEquipped())
			{
				writeD(_activeChar.getClanId());
				writeD(_activeChar.getClanCrestId());
				writeD(_activeChar.getAllyId());
				writeD(_activeChar.getAllyCrestId());
			}
			else
			{
				writeD(0x00);
				writeD(0x00);
				writeD(0x00);
				writeD(0x00);
			}
			
			writeC(_activeChar.isSitting() ? 0 : 1); // standing = 1 sitting = 0
			writeC(_activeChar.isRunning() ? 1 : 0); // running = 1 walking = 0
			writeC(_activeChar.isInCombat() ? 1 : 0);
			
			writeC(!_activeChar.isInOlympiadMode() && _activeChar.isAlikeDead() ? 1 : 0);
			
			writeC(!gmSeeInvis && _invisible ? 1 : 0); // invisible = 1 visible =0
			
			writeC(_activeChar.getMountType()); // 1-on Strider, 2-on Wyvern, 3-on Great Wolf, 0-no mount
			writeC(_activeChar.getPrivateStoreType()); // 1 - sellshop
			
			writeH(_activeChar.getCubics().size());
			for (int id : _activeChar.getCubics().keySet())
			{
				writeH(id);
			}
			
			writeC(_activeChar.isInPartyMatchRoom() ? 1 : 0);
			
			writeD(gmSeeInvis ? (_activeChar.getAbnormalEffect() | AbnormalEffect.STEALTH.getMask()) : _activeChar.getAbnormalEffect());
			
			writeC(_activeChar.isFlyingMounted() ? 2 : 0);
			
			writeH(_activeChar.getRecomHave()); // Blue value for name (0 = white, 255 = pure blue)
			writeD(_activeChar.getMountNpcId() + 1000000);
			writeD(_activeChar.getClassId().getId());
			writeD(0x00); // ?
			writeC(_activeChar.isMounted() || (_airShipHelm != 0) ? 0 : _activeChar.getEnchantEffect());
			
			writeC(_activeChar.getTeam()); // team circle around feet 1= Blue, 2 = red
			
			writeD(_activeChar.getClanCrestLargeId());
			writeC(_activeChar.isNoble() ? 1 : 0); // Symbol on char menu ctrl+I
			writeC(_activeChar.isHero() || (_activeChar.isGM() && Config.GM_HERO_AURA) ? 1 : 0); // Hero Aura
			
			writeC(_activeChar.isFishing() ? 1 : 0); // 0x01: Fishing Mode (Cant be undone by setting back to 0)
			writeD(_activeChar.getFishx());
			writeD(_activeChar.getFishy());
			writeD(_activeChar.getFishz());
			
			writeD(_activeChar.getAppearance().getNameColor());
			
			writeD(_heading);
			
			writeD(_activeChar.getPledgeClass());
			writeD(_activeChar.getPledgeType());
			
			writeD(_activeChar.getAppearance().getTitleColor());
			
			writeD(_activeChar.isCursedWeaponEquipped() ? CursedWeaponsManager.getInstance().getLevel(_activeChar.getCursedWeaponEquippedId()) : 0);
			
			writeD(_activeChar.getClanId() > 0 ? _activeChar.getClan().getReputationScore() : 0);
			
			// T1
			writeD(_activeChar.getTransformationId());
			writeD(_activeChar.getAgathionId());
			
			// T2
			writeD(0x01);
			
			// T2.3
			writeD(_activeChar.getSpecialEffect());
		}
	}
}
