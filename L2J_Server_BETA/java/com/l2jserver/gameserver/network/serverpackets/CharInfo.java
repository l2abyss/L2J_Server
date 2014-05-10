/*
 * Copyright (C) 2004-2014 L2J Server
 * 
 * This file is part of L2J Server.
 * 
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.l2jserver.gameserver.network.serverpackets;

import com.l2jserver.Config;
import com.l2jserver.gameserver.datatables.NpcData;
import com.l2jserver.gameserver.instancemanager.CursedWeaponsManager;
import com.l2jserver.gameserver.model.PcCondOverride;
import com.l2jserver.gameserver.model.actor.L2Decoy;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.actor.templates.L2NpcTemplate;
import com.l2jserver.gameserver.model.itemcontainer.Inventory;
import com.l2jserver.gameserver.model.skills.AbnormalVisualEffect;
import com.l2jserver.gameserver.model.zone.ZoneId;

public class CharInfo extends L2GameServerPacket
{
	private final L2PcInstance _activeChar;
	private int _objId;
	private int _x, _y, _z, _heading;
	private final int _mAtkSpd, _pAtkSpd;
	
	private final int _runSpd, _walkSpd;
	private final int _swimRunSpd;
	private final int _swimWalkSpd;
	private final int _flRunSpd = 0;
	private final int _flWalkSpd = 0;
	private final int _flyRunSpd;
	private final int _flyWalkSpd;
	private final float _moveMultiplier;
	private final float _attackSpeedMultiplier;
	
	private int _vehicleId = 0;
	
	private static final int[] PAPERDOLL_ORDER = new int[]
	{
		Inventory.PAPERDOLL_UNDER,
		Inventory.PAPERDOLL_HEAD,
		Inventory.PAPERDOLL_RHAND,
		Inventory.PAPERDOLL_LHAND,
		Inventory.PAPERDOLL_GLOVES,
		Inventory.PAPERDOLL_CHEST,
		Inventory.PAPERDOLL_LEGS,
		Inventory.PAPERDOLL_FEET,
		Inventory.PAPERDOLL_CLOAK,
		Inventory.PAPERDOLL_RHAND,
		Inventory.PAPERDOLL_HAIR,
		Inventory.PAPERDOLL_HAIR2,
		Inventory.PAPERDOLL_RBRACELET,
		Inventory.PAPERDOLL_LBRACELET,
		Inventory.PAPERDOLL_DECO1,
		Inventory.PAPERDOLL_DECO2,
		Inventory.PAPERDOLL_DECO3,
		Inventory.PAPERDOLL_DECO4,
		Inventory.PAPERDOLL_DECO5,
		Inventory.PAPERDOLL_DECO6,
		Inventory.PAPERDOLL_BELT
	};
	
	public CharInfo(L2PcInstance cha)
	{
		_activeChar = cha;
		_objId = cha.getObjectId();
		if ((_activeChar.getVehicle() != null) && (_activeChar.getInVehiclePosition() != null))
		{
			_x = _activeChar.getInVehiclePosition().getX();
			_y = _activeChar.getInVehiclePosition().getY();
			_z = _activeChar.getInVehiclePosition().getZ();
			_vehicleId = _activeChar.getVehicle().getObjectId();
		}
		else
		{
			_x = _activeChar.getX();
			_y = _activeChar.getY();
			_z = _activeChar.getZ();
		}
		_heading = _activeChar.getHeading();
		_mAtkSpd = _activeChar.getMAtkSpd();
		_pAtkSpd = _activeChar.getPAtkSpd();
		_attackSpeedMultiplier = _activeChar.getAttackSpeedMultiplier();
		_invisible = cha.isInvisible();
		
		_moveMultiplier = cha.getMovementSpeedMultiplier();
		_runSpd = Math.round(cha.getRunSpeed() / _moveMultiplier);
		_walkSpd = Math.round(cha.getWalkSpeed() / _moveMultiplier);
		_swimRunSpd = Math.round(cha.getSwimRunSpeed() / _moveMultiplier);
		_swimWalkSpd = Math.round(cha.getSwimWalkSpeed() / _moveMultiplier);
		_flyRunSpd = cha.isFlying() ? _runSpd : 0;
		_flyWalkSpd = cha.isFlying() ? _walkSpd : 0;
	}
	
	public CharInfo(L2Decoy decoy)
	{
		this(decoy.getActingPlayer()); // init
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
			final L2PcInstance activeChar = getClient().getActiveChar();
			if ((activeChar != null) && activeChar.canOverrideCond(PcCondOverride.SEE_ALL_PLAYERS))
			{
				gmSeeInvis = true;
			}
		}
		
		final L2NpcTemplate template = _activeChar.getPoly().isMorphed() ? NpcData.getInstance().getTemplate(_activeChar.getPoly().getPolyId()) : null;
		if (template != null)
		{
			writeC(0x0C);
			writeD(_objId);
			writeD(template.getId() + 1000000); // npctype id
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
			writeD(_swimRunSpd);
			writeD(_swimWalkSpd);
			writeD(_flRunSpd);
			writeD(_flWalkSpd);
			writeD(_flyRunSpd);
			writeD(_flyWalkSpd);
			writeF(_moveMultiplier);
			writeF(_attackSpeedMultiplier);
			writeF(template.getfCollisionRadius());
			writeF(template.getfCollisionHeight());
			writeD(template.getRHandId()); // right hand weapon
			writeD(template.getChestId()); // chest
			writeD(template.getLHandId()); // left hand weapon
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
			
			writeD(gmSeeInvis ? (_activeChar.getAbnormalVisualEffects() | AbnormalVisualEffect.STEALTH.getMask()) : _activeChar.getAbnormalVisualEffects()); // C2
			
			writeD(_activeChar.getClanId()); // clan id
			writeD(_activeChar.getClanCrestId()); // crest id
			writeD(_activeChar.getAllyId()); // ally id
			writeD(_activeChar.getAllyCrestId()); // all crest
			
			writeC(_activeChar.isFlying() ? 2 : 0); // is Flying
			writeC(_activeChar.getTeam().getId());
			
			writeF(template.getfCollisionRadius());
			writeF(template.getfCollisionHeight());
			
			writeD(0x00); // enchant effect
			writeD(_activeChar.isFlying() ? 2 : 0); // is Flying again?
			
			writeD(0x00);
			
			writeD(0x00); // CT1.5 Pet form and skills, Color effect
			writeC(template.isTargetable() ? 1 : 0); // targetable
			writeC(template.isShowName() ? 1 : 0); // show name
			writeC(_activeChar.getAbnormalVisualEffectSpecial());
			writeD(0x00);
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
			
			for (int slot : getPaperdollOrder())
			{
				writeD(_activeChar.getInventory().getPaperdollItemDisplayId(slot));
			}
			
			for (int slot : getPaperdollOrder())
			{
				writeD(_activeChar.getInventory().getPaperdollAugmentationId(slot));
			}
			
			writeD(_activeChar.getInventory().getTalismanSlots());
			writeD(_activeChar.getInventory().canEquipCloak() ? 1 : 0);
			
			writeD(_activeChar.getPvpFlag());
			writeD(_activeChar.getKarma());
			
			writeD(_mAtkSpd);
			writeD(_pAtkSpd);
			
			writeD(0x00); // ?
			
			writeD(_runSpd);
			writeD(_walkSpd);
			writeD(_swimRunSpd);
			writeD(_swimWalkSpd);
			writeD(_flRunSpd);
			writeD(_flWalkSpd);
			writeD(_flyRunSpd);
			writeD(_flyWalkSpd);
			writeF(_moveMultiplier);
			writeF(_activeChar.getAttackSpeedMultiplier());
			
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
			
			writeC(_activeChar.getMountType().ordinal()); // 1-on Strider, 2-on Wyvern, 3-on Great Wolf, 0-no mount
			writeC(_activeChar.getPrivateStoreType().getId());
			
			writeH(_activeChar.getCubics().size());
			for (int cubicId : _activeChar.getCubics().keySet())
			{
				writeH(cubicId);
			}
			
			writeC(_activeChar.isInPartyMatchRoom() ? 1 : 0);
			
			writeD(gmSeeInvis ? (_activeChar.getAbnormalVisualEffects() | AbnormalVisualEffect.STEALTH.getMask()) : _activeChar.getAbnormalVisualEffects());
			
			writeC(_activeChar.isInsideZone(ZoneId.WATER) ? 1 : _activeChar.isFlyingMounted() ? 2 : 0);
			
			writeH(_activeChar.getRecomHave()); // Blue value for name (0 = white, 255 = pure blue)
			writeD(_activeChar.getMountNpcId() + 1000000);
			writeD(_activeChar.getClassId().getId());
			writeD(0x00); // ?
			writeC(_activeChar.isMounted() ? 0 : _activeChar.getEnchantEffect());
			
			writeC(_activeChar.getTeam().getId());
			
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
			writeD(_activeChar.getTransformationDisplayId());
			writeD(_activeChar.getAgathionId());
			
			// T2
			writeD(0x01);
			
			// T2.3
			writeD(_activeChar.getAbnormalVisualEffectSpecial());
		}
	}
	
	@Override
	protected int[] getPaperdollOrder()
	{
		return PAPERDOLL_ORDER;
	}
}
