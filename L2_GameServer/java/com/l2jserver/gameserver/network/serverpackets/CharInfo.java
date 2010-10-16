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

import java.util.logging.Logger;

import com.l2jserver.Config;
import com.l2jserver.gameserver.datatables.NpcTable;
import com.l2jserver.gameserver.instancemanager.CursedWeaponsManager;
import com.l2jserver.gameserver.model.actor.L2Decoy;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.itemcontainer.Inventory;
import com.l2jserver.gameserver.skills.AbnormalEffect;
import com.l2jserver.gameserver.templates.chars.L2NpcTemplate;


/**
 * 0000: 03 32 15 00 00 44 fe 00 00 80 f1 ff ff 00 00 00    .2...D..........<p>
 * 0010: 00 6b b4 c0 4a 45 00 6c 00 6c 00 61 00 6d 00 69    .k..JE.l.l.a.m.i<p>
 * 0020: 00 00 00 01 00 00 00 01 00 00 00 12 00 00 00 00    ................<p>
 * 0030: 00 00 00 2a 00 00 00 42 00 00 00 71 02 00 00 31    ...*...B...q...1<p>
 * 0040: 00 00 00 18 00 00 00 1f 00 00 00 25 00 00 00 00    ...........%....<p>
 * 0050: 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 f9    ................<p>
 * 0060: 00 00 00 b3 01 00 00 00 00 00 00 00 00 00 00 7d    ...............}<p>
 * 0070: 00 00 00 5a 00 00 00 32 00 00 00 32 00 00 00 00    ...Z...2...2....<p>
 * 0080: 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 67    ...............g<p>
 * 0090: 66 66 66 66 66 f2 3f 5f 63 97 a8 de 1a f9 3f 00    fffff.?_c.....?.<p>
 * 00a0: 00 00 00 00 00 1e 40 00 00 00 00 00 00 37 40 01    .............7..<p>
 * 00b0: 00 00 00 01 00 00 00 01 00 00 00 00 00 c1 0c 00    ................<p>
 * 00c0: 00 00 00 00 00 00 00 00 00 01 01 00 00 00 00 00    ................<p>
 * 00d0: 00 00<p>
 * <p>
 *  dddddSdddddddddddddddddddddddddddffffdddSdddccccccc (h)<p>
 *  dddddSdddddddddddddddddddddddddddffffdddSdddddccccccch
 *  dddddSddddddddddddddddddddddddddddffffdddSdddddccccccch (h) c (dchd) ddc dcc c cddd d
 *  dddddSdddddddddddddddhhhhhhhhhhhhhhhhhhhhhhhhddddddddddddddffffdddSdddddccccccch [h] c (ddhd) ddc c ddc cddd d d dd d d d

 * @version $Revision: 1.7.2.6.2.11 $ $Date: 2005/04/11 10:05:54 $
 */
public class CharInfo extends L2GameServerPacket
{
	private static final Logger _log = Logger.getLogger(CharInfo.class.getName());
	
	private static final String _S__03_CHARINFO = "[S] 31 CharInfo";
	private L2PcInstance _activeChar;
	private Inventory _inv;
	private int _objId;
	private int _x, _y, _z, _heading;
	private int _mAtkSpd, _pAtkSpd;
	
	/**
	 * Run speed, swimming run speed and flying run speed
	 */
	private int _runSpd;
	/**
	 * Walking speed, swimming walking speed and flying walking speed
	 */
	private int _walkSpd;
	private float _moveMultiplier, _attackSpeedMultiplier;
	//private int _territoryId;
	//private boolean _isDisguised;
	
	private int _vehicleId, _airShipHelm;
	
	/**
	 * @param _characters
	 */
	public CharInfo(L2PcInstance cha)
	{
		_activeChar = cha;
		_objId = cha.getObjectId();
		_inv = cha.getInventory();
		if (_activeChar.getVehicle() != null && _activeChar.getInVehiclePosition() != null)
		{
			_x = _activeChar.getInVehiclePosition().getX();
			_y = _activeChar.getInVehiclePosition().getY();
			_z = _activeChar.getInVehiclePosition().getZ();
			_vehicleId = _activeChar.getVehicle().getObjectId();
			if (_activeChar.isInAirShip() && _activeChar.getAirShip().isCaptain(_activeChar))
				_airShipHelm = _activeChar.getAirShip().getHelmItemId();
			else
				_airShipHelm = 0;
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
		_moveMultiplier  = _activeChar.getMovementSpeedMultiplier();
		_attackSpeedMultiplier = _activeChar.getAttackSpeedMultiplier();
		_runSpd = (int)(_activeChar.getRunSpeed()/_moveMultiplier);
		_walkSpd = (int)(_activeChar.getWalkSpeed()/_moveMultiplier);
		_invisible = cha.getAppearance().getInvisible();
		//_territoryId = TerritoryWarManager.getInstance().getRegisteredTerritoryId(cha);
		//_isDisguised = TerritoryWarManager.getInstance().isDisguised(cha.getObjectId());
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
			if (tmp != null && tmp.isGM())
				gmSeeInvis = true;
		}
		
		if (_activeChar.getPoly().isMorphed())
		{
			L2NpcTemplate template = NpcTable.getInstance().getTemplate(_activeChar.getPoly().getPolyId());
			
			if (template != null)
			{
				writeC(0x0c);
				writeD(_objId);
				writeD(_activeChar.getPoly().getPolyId()+1000000);  // npctype id
				writeD(_activeChar.getKarma() > 0 ? 1 : 0);
				writeD(_x);
				writeD(_y);
				writeD(_z);
				writeD(_heading);
				writeD(0x00);
				writeD(_mAtkSpd);
				writeD(_pAtkSpd);
				writeD(_runSpd); // TODO: the order of the speeds should be confirmed
				writeD(_walkSpd);
				writeD(_runSpd); // swim run speed
				writeD(_walkSpd); // swim walk speed
				writeD(_runSpd); // fly run speed
				writeD(_walkSpd); // fly walk speed
				writeD(_runSpd); // fly run speed ?
				writeD(_walkSpd); // fly walk speed ?
				writeF(_moveMultiplier);
				writeF(_attackSpeedMultiplier);
				writeF(template.fCollisionRadius);
				writeF(template.fCollisionHeight);
				writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_RHAND)); // right hand weapon
				writeD(0);
				writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_LHAND)); // left hand weapon
				writeC(1);	// name above char 1=true ... ??
				writeC(_activeChar.isRunning() ? 1 : 0);
				writeC(_activeChar.isInCombat() ? 1 : 0);
				writeC(_activeChar.isAlikeDead() ? 1 : 0);
				
				if (gmSeeInvis)
				{
					writeC(0);
				}
				else
				{
					writeC(_invisible? 1 : 0); // invisible ?? 0=false  1=true   2=summoned (only works if model has a summon animation)
				}
				
				writeS(_activeChar.getAppearance().getVisibleName());
				
				if (gmSeeInvis)
				{
					writeS("Invisible");
				}
				else
				{
					writeS(_activeChar.getAppearance().getVisibleTitle());
				}
				
				writeD(0);
				writeD(0);
				writeD(0000);  // hmm karma ??
				
				if (gmSeeInvis)
				{
					writeD( (_activeChar.getAbnormalEffect() | AbnormalEffect.STEALTH.getMask()) );
				}
				else
				{
					writeD(_activeChar.getAbnormalEffect());  // C2
				}
				
				writeD(_activeChar.getClanId()); //clan id
				writeD(_activeChar.getClanCrestId()); //crest id
				writeD(0);  // C2
				writeD(0);  // C2
				writeC(0);  // C2
				writeC(0x00);  // C3  team circle 1-blue, 2-red
				writeF(template.fCollisionRadius);
				writeF(template.fCollisionHeight);
				writeD(0x00);  // C4
				writeD(0x00);  // C6
				writeD(0x00);
				writeD(0x00);
				writeC(0x01);
				writeC(0x01);
				writeD(0x00);
			}
			else
			{
				_log.warning("Character "+_activeChar.getName()+" ("+_activeChar.getObjectId()+") morphed in a Npc ("+_activeChar.getPoly().getPolyId()+") w/o template.");
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
			
			if (_activeChar.getClassIndex() == 0)
				writeD(_activeChar.getClassId().getId());
			else
				writeD(_activeChar.getBaseClass());
			
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_UNDER));
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_HEAD));
			if (_airShipHelm == 0)
			{
				writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_RHAND));
				writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_LHAND));
			}
			else
			{
				writeD(_airShipHelm);
				writeD(0);
			}
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_GLOVES));
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_CHEST));
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_LEGS));
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_FEET));
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_BACK));
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_LRHAND));
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_HAIR));
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_HAIR2));
			// T1 new d's
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_RBRACELET));
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_LBRACELET));
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_DECO1));
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_DECO2));
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_DECO3));
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_DECO4));
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_DECO5));
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_DECO6));
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_BELT));
			// end of t1 new d's
			
			// c6 new h's
			writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_UNDER));
			writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_HEAD));
			if (_airShipHelm == 0)
			{
				writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_RHAND));
				writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_LHAND));
			}
			else
			{
				writeD(0);
				writeD(0);
			}
			writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_GLOVES));
			writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_CHEST));
			writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_LEGS));
			writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_FEET));
			writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_BACK));
			writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_LRHAND));
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
			
			writeD(0x00);
			writeD(0x01);
			// end of t1 new h's
			
			writeD(_activeChar.getPvpFlag());
			writeD(_activeChar.getKarma());
			
			writeD(_mAtkSpd);
			writeD(_pAtkSpd);
			
			writeD(0x00);
			
			writeD(_runSpd); // TODO: the order of the speeds should be confirmed
			writeD(_walkSpd);
			writeD(_runSpd); // swim run speed
			writeD(_walkSpd); // swim walk speed
			writeD(_runSpd); // fly run speed
			writeD(_walkSpd); // fly walk speed
			writeD(_runSpd); // fly run speed ?
			writeD(_walkSpd); // fly walk speed ?
			writeF(_activeChar.getMovementSpeedMultiplier()); // _activeChar.getProperMultiplier()
			writeF(_activeChar.getAttackSpeedMultiplier()); // _activeChar.getAttackSpeedMultiplier()
			
			if (_activeChar.getMountType() != 0)
			{
				writeF(NpcTable.getInstance().getTemplate(_activeChar.getMountNpcId()).fCollisionRadius);
				writeF(NpcTable.getInstance().getTemplate(_activeChar.getMountNpcId()).fCollisionHeight);
			}
			else if (_activeChar.getTransformation() != null)
			{
				writeF(_activeChar.getTransformation().getCollisionRadius());
				writeF(_activeChar.getTransformation().getCollisionHeight());
			}
			else
			{
				writeF(_activeChar.getCollisionRadius());
				writeF(_activeChar.getCollisionHeight());
			}
			
			writeD(_activeChar.getAppearance().getHairStyle());
			writeD(_activeChar.getAppearance().getHairColor());
			writeD(_activeChar.getAppearance().getFace());
			
			if (gmSeeInvis)
			{
				writeS("Invisible");
			}
			else
			{
				writeS(_activeChar.getAppearance().getVisibleTitle());
			}
			
			if (!_activeChar.isCursedWeaponEquipped())
			{
				writeD(_activeChar.getClanId());
				writeD(_activeChar.getClanCrestId());
				writeD(_activeChar.getAllyId());
				writeD(_activeChar.getAllyCrestId());
			}
			else
			{
				writeD(0);
				writeD(0);
				writeD(0);
				writeD(0);
			}
			
			writeC(_activeChar.isSitting() ? 0 : 1);	// standing = 1  sitting = 0
			writeC(_activeChar.isRunning() ? 1 : 0);	// running = 1   walking = 0
			writeC(_activeChar.isInCombat() ? 1 : 0);
			
			if (_activeChar.isInOlympiadMode())
				writeC(0);
			else
				writeC(_activeChar.isAlikeDead() ? 1 : 0);
			
			if (gmSeeInvis)
			{
				writeC(0);
			}
			else
			{
				writeC(_invisible ? 1 : 0);	// invisible = 1  visible =0
			}
			
			writeC(_activeChar.getMountType());	// 1-on Strider, 2-on Wyvern, 3-on Great Wolf, 0-no mount
			writeC(_activeChar.getPrivateStoreType());   //  1 - sellshop
			
			writeH(_activeChar.getCubics().size());
			for (int id : _activeChar.getCubics().keySet())
				writeH(id);
			
			writeC(_activeChar.isInPartyMatchRoom() ? 1 : 0);
			
			if (gmSeeInvis)
			{
				writeD( (_activeChar.getAbnormalEffect() | AbnormalEffect.STEALTH.getMask()) );
			}
			else
			{
				writeD(_activeChar.getAbnormalEffect());
			}
			
			writeC(_activeChar.isFlyingMounted() ? 2 : 0);
			
			writeH(_activeChar.getRecomHave()); //Blue value for name (0 = white, 255 = pure blue)
			writeD(_activeChar.getMountNpcId() + 1000000);
			writeD(_activeChar.getClassId().getId());
			writeD(0x00); //?
			writeC(_activeChar.isMounted() || _airShipHelm != 0 ? 0 : _activeChar.getEnchantEffect());
			
			if(_activeChar.getTeam()==1)
				writeC(0x01); //team circle around feet 1= Blue, 2 = red
			else if(_activeChar.getTeam()==2)
				writeC(0x02); //team circle around feet 1= Blue, 2 = red
			else
				writeC(0x00); //team circle around feet 1= Blue, 2 = red
			
			writeD(_activeChar.getClanCrestLargeId());
			writeC(_activeChar.isNoble() ? 1 : 0); // Symbol on char menu ctrl+I
			writeC(_activeChar.isHero() || (_activeChar.isGM() && Config.GM_HERO_AURA) ? 1 : 0); // Hero Aura
			
			writeC(_activeChar.isFishing() ? 1 : 0); //0x01: Fishing Mode (Cant be undone by setting back to 0)
			writeD(_activeChar.getFishx());
			writeD(_activeChar.getFishy());
			writeD(_activeChar.getFishz());
			
			writeD(_activeChar.getAppearance().getNameColor());
			
			writeD(_heading);
			
			writeD(_activeChar.getPledgeClass());
			writeD(_activeChar.getPledgeType());
			
			writeD(_activeChar.getAppearance().getTitleColor());
			
			if (_activeChar.isCursedWeaponEquipped())
				writeD(CursedWeaponsManager.getInstance().getLevel(_activeChar.getCursedWeaponEquippedId()));
			else
				writeD(0x00);
			
			if (_activeChar.getClanId() > 0)
				writeD(_activeChar.getClan().getReputationScore());
			else
				writeD(0x00);
			
			// T1
			writeD(_activeChar.getTransformationId());
			writeD(_activeChar.getAgathionId());
			
			// T2
			writeD(0x01);
			
			// T2.3
			writeD(_activeChar.getSpecialEffect());
		}
	}
	
	/* (non-Javadoc)
	 * @see com.l2jserver.gameserver.serverpackets.ServerBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__03_CHARINFO;
	}
}
