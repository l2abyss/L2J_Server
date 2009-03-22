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
package net.sf.l2j.gameserver.network.serverpackets;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.instancemanager.CursedWeaponsManager;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Decoy;
import net.sf.l2j.gameserver.model.L2Summon;
import net.sf.l2j.gameserver.model.L2Transformation;
import net.sf.l2j.gameserver.model.L2Trap;
import net.sf.l2j.gameserver.model.actor.instance.L2MonsterInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.itemcontainer.Inventory;
/**
 * This class ...
 *
 * @version $Revision: 1.7.2.4.2.9 $ $Date: 2005/04/11 10:05:54 $
 */
public final class NpcInfo extends L2GameServerPacket
{
	//   ddddddddddddddddddffffdddcccccSSddd dddddc
	//   ddddddddddddddddddffffdddcccccSSddd dddddccffd


	private static final String _S__22_NPCINFO = "[S] 0c NpcInfo";
	private L2Character _activeChar;
	private int _x, _y, _z, _heading;
	private int _idTemplate;
	private boolean _isAttackable, _isSummoned;
	private int _mAtkSpd, _pAtkSpd;
	private int _runSpd, _walkSpd, _swimRunSpd, _swimWalkSpd, _flRunSpd, _flWalkSpd, _flyRunSpd, _flyWalkSpd;
	private int _rhand, _lhand, _chest, _val;
    private int _collisionHeight, _collisionRadius;
    private String _name = "";
    private String _title = "";
    private L2Summon _summon;
    private int _form = 0;

	/**
	 * @param _characters
	 */
	public NpcInfo(L2NpcInstance cha, L2Character attacker)
	{
		_activeChar = cha;
		_idTemplate = cha.getTemplate().idTemplate;
		_isAttackable = cha.isAutoAttackable(attacker);
		_rhand = cha.getRightHandItem(); 
		_lhand = cha.getLeftHandItem(); 
		_isSummoned = cha.isShowSummonAnimation();
        _collisionHeight = cha.getCollisionHeight();
        _collisionRadius = cha.getCollisionRadius();
        if (cha.getTemplate().serverSideName)
        	_name = cha.getTemplate().name;

        if(Config.L2JMOD_CHAMPION_ENABLE && cha.isChampion())
            _title = (Config.L2JMOD_CHAMP_TITLE);
        else if (cha.getTemplate().serverSideTitle)
    		_title = cha.getTemplate().title;
    	else
    		_title = cha.getTitle();

        if (Config.SHOW_NPC_LVL && _activeChar instanceof L2MonsterInstance)
	    {
			String t = "Lv " + cha.getLevel() + (cha.getAggroRange() > 0 ? "*" : "");
			if (_title != null)
				t += " " + _title;

			_title = t;
	    }

        _x = _activeChar.getX();
		_y = _activeChar.getY();
		_z = _activeChar.getZ();
		_heading = _activeChar.getHeading();
		_mAtkSpd = _activeChar.getMAtkSpd();
		_pAtkSpd = _activeChar.getPAtkSpd();
		_runSpd = _activeChar.getTemplate().baseRunSpd;
		_walkSpd = _activeChar.getTemplate().baseWalkSpd;
		_swimRunSpd = _flRunSpd = _flyRunSpd = _runSpd;
		_swimWalkSpd = _flWalkSpd = _flyWalkSpd = _walkSpd;
	}

	public NpcInfo(L2Summon cha, L2Character attacker, int val)
	{
		_activeChar = cha;
		_summon = cha;
		_idTemplate = cha.getTemplate().idTemplate;
		_isAttackable = cha.isAutoAttackable(attacker); //(cha.getKarma() > 0);
		_rhand = cha.getWeapon();
		_lhand = 0;
		_chest = cha.getArmor();
		_val = val;
        _collisionHeight = _activeChar.getTemplate().collisionHeight;
        _collisionRadius = _activeChar.getTemplate().collisionRadius;
        _name = cha.getName();
        _title = cha.getOwner() != null ? (cha.getOwner().isOnline() == 0 ? "" : cha.getOwner().getName()) : ""; // when owner online, summon will show in title owner name
        int npcId = _summon.getTemplate().npcId;
        if (npcId == 16041 || npcId == 16042)
        {
        	if(_summon.getLevel() > 84)
        		_form = 3;
        	else if(_summon.getLevel() > 79) 
        		_form = 2;
        	else if(_summon.getLevel() > 74)
        		_form = 1;
        }
        else if (npcId == 16025 || npcId == 16037)
        {
        	if(_summon.getLevel() > 69)
        		_form = 3;
        	else if(_summon.getLevel() > 64) 
        		_form = 2;
        	else if(_summon.getLevel() > 59) 
        		_form = 1;
        }

        _x = _activeChar.getX();
		_y = _activeChar.getY();
		_z = _activeChar.getZ();
		_heading = _activeChar.getHeading();
		_mAtkSpd = _activeChar.getMAtkSpd();
		_pAtkSpd = _activeChar.getPAtkSpd();
		_runSpd = _summon.getPetSpeed();
		_walkSpd = _summon.isMountable() ? 45 : 30;
		_swimRunSpd = _flRunSpd = _flyRunSpd = _runSpd;
		_swimWalkSpd = _flWalkSpd = _flyWalkSpd = _walkSpd;
	}
	
	public NpcInfo(L2Trap cha, L2Character attacker)
	{
		_activeChar = cha;
		_idTemplate = cha.getTemplate().idTemplate;
		_isAttackable = cha.isAutoAttackable(attacker);
		_rhand = 0;
		_lhand = 0;
		_collisionHeight = _activeChar.getTemplate().collisionHeight;
		_collisionRadius = _activeChar.getTemplate().collisionRadius;
		_x = _activeChar.getX();
		_y = _activeChar.getY();
		_z = _activeChar.getZ();
		_title = cha.getOwner().getName();
		_heading = _activeChar.getHeading();
		_mAtkSpd = _activeChar.getMAtkSpd();
		_pAtkSpd = _activeChar.getPAtkSpd();
		_runSpd = _activeChar.getRunSpeed();
		_walkSpd = _activeChar.getWalkSpeed();
		_swimRunSpd = _flRunSpd = _flyRunSpd = _runSpd;
		_swimWalkSpd = _flWalkSpd = _flyWalkSpd = _walkSpd;
	}

    public NpcInfo(L2Decoy cha)
    {
        _idTemplate = cha.getTemplate().idTemplate;
        _activeChar = cha;
        _x = _activeChar.getX();
        _y = _activeChar.getY();
        _z = _activeChar.getZ();
        _heading = cha.getOwner().getHeading();
        _mAtkSpd = cha.getMAtkSpd();
        _pAtkSpd = cha.getOwner().getPAtkSpd();
        _runSpd = cha.getOwner().getRunSpeed();
        _walkSpd = cha.getOwner().getWalkSpeed();
        _swimRunSpd = _flRunSpd = _flyRunSpd = _runSpd;
        _swimWalkSpd = _flWalkSpd = _flyWalkSpd = _walkSpd;
    }
	   
	@Override
	protected final void writeImpl()
	{
	    if (_idTemplate > 13070 && _idTemplate < 13077)
        {
                
	        writeC(0x31);
            writeD(_x);
            writeD(_y);
            writeD(_z);
            writeD(_heading);
            writeD(_activeChar.getObjectId());
            writeS(((L2Decoy)_activeChar).getOwner().getAppearance().getVisibleName());
            writeD(((L2Decoy)_activeChar).getOwner().getRace().ordinal());
            writeD(((L2Decoy)_activeChar).getOwner().getAppearance().getSex()? 1 : 0);

            if (((L2Decoy)_activeChar).getOwner().getClassIndex() == 0)
                writeD(((L2Decoy)_activeChar).getOwner().getClassId().getId());
            else
                writeD(((L2Decoy)_activeChar).getOwner().getBaseClass());

            writeD(((L2Decoy)_activeChar).getOwner().getInventory().getPaperdollItemId(Inventory.PAPERDOLL_HAIRALL));
            writeD(((L2Decoy)_activeChar).getOwner().getInventory().getPaperdollItemId(Inventory.PAPERDOLL_HEAD));
            writeD(((L2Decoy)_activeChar).getOwner().getInventory().getPaperdollItemId(Inventory.PAPERDOLL_RHAND));
            writeD(((L2Decoy)_activeChar).getOwner().getInventory().getPaperdollItemId(Inventory.PAPERDOLL_LHAND));
            writeD(((L2Decoy)_activeChar).getOwner().getInventory().getPaperdollItemId(Inventory.PAPERDOLL_GLOVES));
            writeD(((L2Decoy)_activeChar).getOwner().getInventory().getPaperdollItemId(Inventory.PAPERDOLL_CHEST));
            writeD(((L2Decoy)_activeChar).getOwner().getInventory().getPaperdollItemId(Inventory.PAPERDOLL_LEGS));
            writeD(((L2Decoy)_activeChar).getOwner().getInventory().getPaperdollItemId(Inventory.PAPERDOLL_FEET));
            writeD(((L2Decoy)_activeChar).getOwner().getInventory().getPaperdollItemId(Inventory.PAPERDOLL_BACK));
            writeD(((L2Decoy)_activeChar).getOwner().getInventory().getPaperdollItemId(Inventory.PAPERDOLL_LRHAND));
            writeD(((L2Decoy)_activeChar).getOwner().getInventory().getPaperdollItemId(Inventory.PAPERDOLL_HAIR));
            writeD(((L2Decoy)_activeChar).getOwner().getInventory().getPaperdollItemId(Inventory.PAPERDOLL_HAIR2));
            
             // T1 new d's 
            writeD(0x00); 
            writeD(0x00); 
            writeD(0x00); 
            writeD(0x00); 
            writeD(0x00); 
            writeD(0x00); 
            writeD(0x00); 
            writeD(0x00); 
             // end of t1 new d's 

            // c6 new h's
            writeH(0x00);
            writeH(0x00);
            writeH(0x00);
            writeH(0x00);
            writeD(((L2Decoy)_activeChar).getOwner().getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_RHAND));
            writeH(0x00);
            writeH(0x00);
            writeH(0x00);
            writeH(0x00);
            writeH(0x00);
            writeH(0x00);
            writeH(0x00);
            writeH(0x00);
            writeH(0x00);
            writeH(0x00);
            writeH(0x00);
            writeH(0x00);
            writeD(((L2Decoy)_activeChar).getOwner().getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_LRHAND));
            writeH(0x00);
            writeH(0x00);
            writeH(0x00);
            writeH(0x00);

            
            // T1 new h's 
            writeH(0x00); 
            writeH(0x00); 
            writeH(0x00); 
            writeH(0x00); 
            writeH(0x00); 
            writeH(0x00); 
            writeH(0x00); 
            writeH(0x00); 
            writeH(0x00); 
            writeH(0x00); 
            writeH(0x00); 
            writeH(0x00); 
            writeH(0x00); 
            writeH(0x00); 
            writeH(0x00); 
            writeH(0x00); 

            // end of t1 new h's 
            
            
            writeD(((L2Decoy)_activeChar).getOwner().getPvpFlag());
            writeD(((L2Decoy)_activeChar).getOwner().getKarma());

            writeD(_mAtkSpd);
            writeD(_pAtkSpd);

            writeD(((L2Decoy)_activeChar).getOwner().getPvpFlag());
            writeD(((L2Decoy)_activeChar).getOwner().getKarma());

            writeD(_runSpd);
            writeD(_walkSpd);
            writeD(50);  // swimspeed
            writeD(50);  // swimspeed
            writeD(_flRunSpd);
            writeD(_flWalkSpd);
            writeD(_flyRunSpd);
            writeD(_flyWalkSpd);
            writeF(((L2Decoy)_activeChar).getOwner().getMovementSpeedMultiplier()); // _activeChar.getProperMultiplier()
            writeF(((L2Decoy)_activeChar).getOwner().getAttackSpeedMultiplier()); // _activeChar.getAttackSpeedMultiplier()
             L2Summon pet = _activeChar.getPet(); 
                L2Transformation trans; 
                if (((L2Decoy)_activeChar).getOwner().getMountType() != 0 && pet != null) 
                { 
                    writeF(pet.getTemplate().collisionRadius); 
                    writeF(pet.getTemplate().collisionHeight); 
                } 
                else if ((trans = ((L2Decoy)_activeChar).getOwner().getTransformation()) != null) 
                { 
                    writeF(trans.getCollisionRadius()); 
                    writeF(trans.getCollisionHeight()); 
                } 
                else 
                { 
                    writeF(((L2Decoy)_activeChar).getOwner().getBaseTemplate().collisionRadius); 
                    writeF(((L2Decoy)_activeChar).getOwner().getBaseTemplate().collisionHeight); 
                } 

            writeD(((L2Decoy)_activeChar).getOwner().getAppearance().getHairStyle());
            writeD(((L2Decoy)_activeChar).getOwner().getAppearance().getHairColor());
            writeD(((L2Decoy)_activeChar).getOwner().getAppearance().getFace());

            writeS(((L2Decoy)_activeChar).getOwner().getAppearance().getVisibleTitle());

            writeD(((L2Decoy)_activeChar).getOwner().getClanId());
            writeD(((L2Decoy)_activeChar).getOwner().getClanCrestId());
            writeD(((L2Decoy)_activeChar).getOwner().getAllyId());
            writeD(((L2Decoy)_activeChar).getOwner().getAllyCrestId());
            // In UserInfo leader rights and siege flags, but here found nothing??
            // Therefore RelationChanged packet with that info is required
            writeD(0);

            writeC(((L2Decoy)_activeChar).getOwner().isSitting() ? 0 : 1);    // standing = 1  sitting = 0
            writeC(((L2Decoy)_activeChar).getOwner().isRunning() ? 1 : 0);    // running = 1   walking = 0
            writeC(((L2Decoy)_activeChar).getOwner().isInCombat() ? 1 : 0);
            writeC(((L2Decoy)_activeChar).getOwner().isAlikeDead() ? 1 : 0);

            writeC(((L2Decoy)_activeChar).getOwner().getAppearance().getInvisible() ? 1 : 0); // invisible = 1  visible =0

            writeC(((L2Decoy)_activeChar).getOwner().getMountType()); // 1 on strider   2 on wyvern  3 on Great Wolf  0 no mount
            writeC(((L2Decoy)_activeChar).getOwner().getPrivateStoreType());   //  1 - sellshop

            writeH(((L2Decoy)_activeChar).getOwner().getCubics().size());
            for (int id : ((L2Decoy)_activeChar).getOwner().getCubics().keySet())
                writeH(id);

            writeC(0x00);   // find party members

            writeD(((L2Decoy)_activeChar).getOwner().getAbnormalEffect());

            writeC(((L2Decoy)_activeChar).getOwner().getRecomLeft());                       //Changed by Thorgrim
            writeH(((L2Decoy)_activeChar).getOwner().getRecomHave()); //Blue value for name (0 = white, 255 = pure blue)
            writeD(((L2Decoy)_activeChar).getOwner().getClassId().getId());

            writeD(((L2Decoy)_activeChar).getOwner().getMaxCp());
            writeD((int) ((L2Decoy)_activeChar).getOwner().getCurrentCp());
            writeC(((L2Decoy)_activeChar).getOwner().isMounted() ? 0 : ((L2Decoy)_activeChar).getOwner().getEnchantEffect());

            if(((L2Decoy)_activeChar).getOwner().getTeam()==1)
                writeC(0x01); //team circle around feet 1= Blue, 2 = red
            else if(((L2Decoy)_activeChar).getOwner().getTeam()==2)
                writeC(0x02); //team circle around feet 1= Blue, 2 = red
            else
                writeC(0x00); //team circle around feet 1= Blue, 2 = red

            writeD(((L2Decoy)_activeChar).getOwner().getClanCrestLargeId());
            writeC(((L2Decoy)_activeChar).getOwner().isNoble() ? 1 : 0); // Symbol on char menu ctrl+I
            writeC(((L2Decoy)_activeChar).getOwner().isHero() ? 1 : 0); // Hero Aura

            writeC(((L2Decoy)_activeChar).getOwner().isFishing() ? 1 : 0); //0x01: Fishing Mode (Cant be undone by setting back to 0)
            writeD(((L2Decoy)_activeChar).getOwner().getFishx());
            writeD(((L2Decoy)_activeChar).getOwner().getFishy());
            writeD(((L2Decoy)_activeChar).getOwner().getFishz());

            writeD(((L2Decoy)_activeChar).getOwner().getAppearance().getNameColor());

            writeD(0x00); // isRunning() as in UserInfo?

            writeD(((L2Decoy)_activeChar).getOwner().getPledgeClass());
            writeD(0x00); // ??

            writeD(((L2Decoy)_activeChar).getOwner().getAppearance().getTitleColor());

            //writeD(0x00); // ??

            if (((L2Decoy)_activeChar).getOwner().isCursedWeaponEquipped())
                writeD(CursedWeaponsManager.getInstance().getLevel(((L2Decoy)_activeChar).getOwner().getCursedWeaponEquippedId()));
            else
                writeD(0x00);
            
         // T1 
            writeD(0x00); 
            
            writeD(((L2Decoy)_activeChar).getOwner().getTransformationId()); 
        }
        else
	    {
        if (_activeChar instanceof L2Summon)
            if (((L2Summon)_activeChar).getOwner() != null
                    && ((L2Summon)_activeChar).getOwner().getAppearance().getInvisible())
                return;
        
		writeC(0x0c);
		writeD(_activeChar.getObjectId());
		writeD(_idTemplate+1000000);  // npctype id
		writeD(_isAttackable ? 1 : 0);
		writeD(_x);
		writeD(_y);
		writeD(_z);
		writeD(_heading);
		writeD(0x00);
		writeD(_mAtkSpd);
		writeD(_pAtkSpd);
		writeD(_runSpd);
		writeD(_walkSpd);
		writeD(_swimRunSpd);  // swimspeed
		writeD(_swimWalkSpd);  // swimspeed
		writeD(_flRunSpd);
		writeD(_flWalkSpd);
		writeD(_flyRunSpd);
		writeD(_flyWalkSpd);
		writeF(_activeChar.getMovementSpeedMultiplier());
		writeF(_activeChar.getAttackSpeedMultiplier());
		writeF(_collisionRadius);
		writeF(_collisionHeight);
		writeD(_rhand); // right hand weapon
		writeD(_chest);
		writeD(_lhand); // left hand weapon
		writeC(1);	// name above char 1=true ... ??
		writeC(1); // char always running
		writeC(_activeChar.isInCombat() ? 1 : 0);
		writeC(_activeChar.isAlikeDead() ? 1 : 0);
		writeC(_isSummoned ? 2 : _val); //  0=teleported  1=default   2=summoned
		writeS(_name);
		writeS(_title);
		if (_activeChar instanceof L2Summon){  
			writeD(0x01);// Title color 0=client default  
		}else{  
			writeD(0x00);  
		}  

		writeD(0);
		writeD(0000);  // hmm karma ??

		writeD(_activeChar.getAbnormalEffect());  // C2
		writeD(0000);  // C2
		writeD(0000);  // C2
		writeD(0000);  // C2
		writeD(0000);  // C2
		writeC(0000);  // C2

		if (_activeChar instanceof L2Summon){  
			writeC(_summon.getOwner().getTeam());// Title color 0=client default  
		}else{  
			writeC(0x00);  
		}  
		writeF(_collisionRadius);
		writeF(_collisionHeight);
		writeD(0x00);  // C4
		writeD(0x00);  // C6
		writeD(0x00);
        writeD(_form);//CT1.5 Pet form and skills
	    }
	}

	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.serverpackets.ServerBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__22_NPCINFO;
	}
}
