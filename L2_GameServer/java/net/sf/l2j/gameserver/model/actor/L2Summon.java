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
package net.sf.l2j.gameserver.model.actor;

import java.util.Collection;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.GeoData;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.ai.L2CharacterAI;
import net.sf.l2j.gameserver.ai.L2SummonAI;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Party;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2WorldRegion;
import net.sf.l2j.gameserver.model.L2Skill.SkillTargetType;
import net.sf.l2j.gameserver.model.actor.L2Attackable.AggroInfo;
import net.sf.l2j.gameserver.model.actor.instance.L2DoorInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2MerchantSummonInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SummonInstance;
import net.sf.l2j.gameserver.model.actor.knownlist.SummonKnownList;
import net.sf.l2j.gameserver.model.actor.stat.SummonStat;
import net.sf.l2j.gameserver.model.actor.status.SummonStatus;
import net.sf.l2j.gameserver.model.base.Experience;
import net.sf.l2j.gameserver.model.itemcontainer.PetInventory;
import net.sf.l2j.gameserver.model.olympiad.Olympiad;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.AbstractNpcInfo;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.ExPartyPetWindowAdd;
import net.sf.l2j.gameserver.network.serverpackets.ExPartyPetWindowDelete;
import net.sf.l2j.gameserver.network.serverpackets.ExPartyPetWindowUpdate;
import net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket;
import net.sf.l2j.gameserver.network.serverpackets.MyTargetSelected;
import net.sf.l2j.gameserver.network.serverpackets.PetDelete;
import net.sf.l2j.gameserver.network.serverpackets.PetInfo;
import net.sf.l2j.gameserver.network.serverpackets.PetStatusShow;
import net.sf.l2j.gameserver.network.serverpackets.PetStatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.RelationChanged;
import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.taskmanager.DecayTaskManager;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;
import net.sf.l2j.gameserver.templates.item.L2Weapon;

public abstract class L2Summon extends L2Playable
{
    //private static Logger _log = Logger.getLogger(L2Summon.class.getName());

    private L2PcInstance _owner;
    private int _attackRange = 36; //Melee range
    private boolean _follow = true;
    private boolean _previousFollowStatus = true;

    private int _chargedSoulShot;
    private int _chargedSpiritShot;

    // TODO: currently, all servitors use 1 shot.  However, this value
    // should vary depending on the servitor template (id and level)!
    private int _soulShotsPerHit = 1;
    private int _spiritShotsPerHit = 1;

	public class AIAccessor extends L2Character.AIAccessor
	{
		protected AIAccessor() {}
		public L2Summon getSummon() { return L2Summon.this; }
		public boolean isAutoFollow() {
			return L2Summon.this.getFollowStatus();
		}
		public void doPickupItem(L2Object object) {
			L2Summon.this.doPickupItem(object);
		}
	}

	public L2Summon(int objectId, L2NpcTemplate template, L2PcInstance owner)
	{
		super(objectId, template);
        getKnownList();	// init knownlist
        getStat();			// init stats
        getStatus();		// init status

        setInstanceId(owner.getInstanceId()); // set instance to same as owner
        
        _showSummonAnimation = true;
		_owner = owner;
		_ai = new L2SummonAI(new L2Summon.AIAccessor());

		setXYZInvisible(owner.getX()+50, owner.getY()+100, owner.getZ()+100);
	}

    @Override
    public void onSpawn()
    {
        super.onSpawn();
        if (!(this instanceof L2MerchantSummonInstance))
        {
        	this.setFollowStatus(true);
        	updateAndBroadcastStatus(0);
        	getOwner().sendPacket(new RelationChanged(this, getOwner().getRelation(getOwner()), false));
            for (L2PcInstance player : getOwner().getKnownList().getKnownPlayersInRadius(800))
            	player.sendPacket(new RelationChanged(this, getOwner().getRelation(player), isAutoAttackable(player)));
            L2Party party = this.getOwner().getParty();
            if (party != null)
            {
                party.broadcastToPartyMembers(this.getOwner(), new ExPartyPetWindowAdd(this));
            }
        }
        setShowSummonAnimation(false); // addVisibleObject created the info packets with summon animation
                                              // if someone comes into range now, the animation shouldnt show any more

    }

    @Override
	public final SummonKnownList getKnownList()
    {
    	if(!(super.getKnownList() instanceof SummonKnownList))
    		setKnownList(new SummonKnownList(this));
    	return (SummonKnownList)super.getKnownList();
    }

    @Override
	public SummonStat getStat()
    {
    	if(!(super.getStat() instanceof SummonStat))
    		setStat(new SummonStat(this));
    	return (SummonStat)super.getStat();
    }

    @Override
	public SummonStatus getStatus()
    {
    	if(!(super.getStatus() instanceof SummonStatus))
    		setStatus(new SummonStatus(this));
    	return (SummonStatus)super.getStatus();
    }

	@Override
	public L2CharacterAI getAI()
    {
		L2CharacterAI ai = _ai; // copy handle
		if (ai == null)
		{
			synchronized(this)
			{
				if (_ai == null) _ai = new L2SummonAI(new L2Summon.AIAccessor());
				return _ai;
			}
		}
		return ai;
	}

	@Override
	public L2NpcTemplate getTemplate()
	{
		return (L2NpcTemplate)super.getTemplate();
	}

	// this defines the action buttons, 1 for Summon, 2 for Pets
    public abstract int getSummonType();

	@Override
	public void updateAbnormalEffect()
    {
		Collection<L2PcInstance> plrs = getKnownList().getKnownPlayers().values();
		//synchronized (getKnownList().getKnownPlayers())
		{
			for (L2PcInstance player : plrs)
				player.sendPacket(new AbstractNpcInfo.SummonInfo(this, player,1));
		}
    }

    /**
     * @return Returns the mountable.
     */
    public boolean isMountable()
    {
        return false;
    }

	@Override
	public void onAction(L2PcInstance player)
    {
		// Aggression target lock effect
		if (player.isLockedTarget() && player.getLockedTarget() != this)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.FAILED_CHANGE_TARGET));
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

        if (player == _owner && player.getTarget() == this)
        {
            player.sendPacket(new PetStatusShow(this));
            player.sendPacket(ActionFailed.STATIC_PACKET);
        }
        else if (player.getTarget() != this)
        {
            if (Config.DEBUG) _log.fine("new target selected:"+getObjectId());
            player.setTarget(this);
            MyTargetSelected my = new MyTargetSelected(getObjectId(), player.getLevel() - getLevel());
            player.sendPacket(my);

            //sends HP/MP status of the summon to other characters
			StatusUpdate su = new StatusUpdate(getObjectId());
			su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
			su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
			player.sendPacket(su);
        }
        else if (player.getTarget() == this)
		{
			if (isAutoAttackable(player))
			{
				if (Config.GEODATA > 0)
				{
					if (GeoData.getInstance().canSeeTarget(player, this))
					{
						player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
						player.onActionRequest();
					}
				}
				else
				{
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
					player.onActionRequest();
				}
			}
			else
			{
				// This Action Failed packet avoids player getting stuck when clicking three or more times
				player.sendPacket(ActionFailed.STATIC_PACKET);
				if (Config.GEODATA > 0)
				{
					if (GeoData.getInstance().canSeeTarget(player, this))
						player.getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, this);
				}
				else
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, this);
			}
		}
    }

	public long getExpForThisLevel()
    {
        if(getLevel() >= Experience.LEVEL.length)
        {
            return 0;
        }
		return Experience.LEVEL[getLevel()];
    }

    public long getExpForNextLevel()
    {
        if(getLevel() >= Experience.LEVEL.length - 1)
        {
            return 0;
        }
        return Experience.LEVEL[getLevel()+1];
    }

	public final int getKarma()
    {
        return getOwner()!= null ? getOwner().getKarma() : 0;
    }

    public final L2PcInstance getOwner()
    {
    	return _owner;
    }

    public final int getNpcId()
    {
        return getTemplate().npcId;
    }

    public int getMaxLoad()
    {
        return 0;
    }

    public final int getSoulShotsPerHit()
    {
        return _soulShotsPerHit;
    }

    public final int getSpiritShotsPerHit()
    {
        return _spiritShotsPerHit;
    }

    public void setChargedSoulShot(int shotType)
    {
        _chargedSoulShot = shotType;
    }

    public void setChargedSpiritShot(int shotType)
    {
        _chargedSpiritShot = shotType;
    }

    public void followOwner()
    {
		setFollowStatus(true);
    }

	@Override
	public boolean doDie(L2Character killer)
	{
		if (!super.doDie(killer))
			return false;
		if (this instanceof L2MerchantSummonInstance)
			return true;
		L2PcInstance owner = getOwner();
		
		if (owner != null)
		{
			Collection<L2Character> KnownTarget = this.getKnownList().getKnownCharacters();
			for (L2Character TgMob : KnownTarget)
			{
				// get the mobs which have aggro on the this instance
				if (TgMob instanceof L2Attackable)
				{
					if (((L2Attackable) TgMob).isDead())
						continue;
					
					AggroInfo info = ((L2Attackable) TgMob).getAggroListRP().get(this);
					if (info != null)
						((L2Attackable) TgMob).addDamageHate(owner, info._damage, info._hate);
				}
			}
		}
		
		if (isPhoenixBlessed() && (getOwner() != null))
			getOwner().reviveRequest(getOwner(), null, true);
		
		DecayTaskManager.getInstance().addDecayTask(this);
		return true;
	}

	public boolean doDie(L2Character killer, boolean decayed)
    {
		if (!super.doDie(killer))
			return false;
		if (!decayed)
		{
			DecayTaskManager.getInstance().addDecayTask(this);
		}
		return true;
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
	public void broadcastStatusUpdate()
    {
        //super.broadcastStatusUpdate();
        updateAndBroadcastStatus(1);
    }
    
    public void deleteMe(L2PcInstance owner)
    {
        getAI().stopFollow();
        owner.sendPacket(new PetDelete(getObjectId(), 2));

        //pet will be deleted along with all his items
        if (getInventory() != null)
        {
        	getInventory().destroyAllItems("pet deleted", getOwner(), this);
        }
        decayMe();
        getKnownList().removeAllKnownObjects();
        owner.setPet(null);
    }

    public void unSummon(L2PcInstance owner)
    {
		if (isVisible() && !isDead())
	    {
			getAI().stopFollow();
	        owner.sendPacket(new PetDelete(getObjectId(), 2));
            L2Party party;
            if ((party = owner.getParty()) != null)
            {
                party.broadcastToPartyMembers(owner, new ExPartyPetWindowDelete(this));
            }
            
	        store();
	        giveAllToOwner();
	        owner.setPet(null);

	        stopAllEffects();
	        L2WorldRegion oldRegion = getWorldRegion();
		    decayMe();
		    if (oldRegion != null) oldRegion.removeFromZones(this);
            getKnownList().removeAllKnownObjects();
	        setTarget(null);
	    }
    }

    public int getAttackRange()
    {
        return _attackRange;
    }

    public void setAttackRange(int range)
    {
        if (range < 36)
            range = 36;
        _attackRange = range;
    }

    public void setFollowStatus(boolean state)
    {
        _follow = state;
		if (_follow)
			getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, getOwner());
		else
			getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE, null);
    }

    public boolean getFollowStatus()
    {
        return _follow;
    }


    @Override
	public boolean isAutoAttackable(L2Character attacker)
    {
        return _owner.isAutoAttackable(attacker);
    }

    public int getChargedSoulShot()
    {
        return _chargedSoulShot;
    }

    public int getChargedSpiritShot()
    {
        return _chargedSpiritShot;
    }

    public int getControlItemId()
    {
        return 0;
    }

    public L2Weapon getActiveWeapon()
    {
        return null;
    }

    @Override
    public PetInventory getInventory()
    {
        return null;
    }

	protected void doPickupItem(L2Object object)
    {
    }

    public void giveAllToOwner()
    {
    }

    public void store()
    {
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

	/**
	 * Return True if the L2Summon is invulnerable or if the summoner is in spawn protection.<BR><BR>
	 */
	@Override
	public boolean isInvul()
	{
		return _isInvul  || _isTeleporting ||  getOwner().isSpawnProtected();
	}

	/**
	 * Return the L2Party object of its L2PcInstance owner or null.<BR><BR>
	 */
	@Override
	public L2Party getParty()
	{
		if (_owner == null)
			return null;
		else
			return _owner.getParty();
	}

	/**
	 * Return True if the L2Character has a Party in progress.<BR><BR>
	 */
    @Override
	public boolean isInParty()
	{
    	if (_owner == null)
    		return false;
    	else
    		return _owner.getParty() != null;
	}

	/**
	 * Check if the active L2Skill can be casted.<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
     * <li>Check if the target is correct </li>
	 * <li>Check if the target is in the skill cast range </li>
	 * <li>Check if the summon owns enough HP and MP to cast the skill </li>
	 * <li>Check if all skills are enabled and this skill is enabled </li><BR><BR>
	 * <li>Check if the skill is active </li><BR><BR>
	 * <li>Notify the AI with AI_INTENTION_CAST and target</li><BR><BR>
	 *
	 * @param skill The L2Skill to use
	 * @param forceUse used to force ATTACK on players
	 * @param dontMove used to prevent movement, if not in range
	 *
	 */
	public void useMagic(L2Skill skill, boolean forceUse, boolean dontMove)
	{
		if (skill == null || isDead())
			return;

        // Check if the skill is active
		if (skill.isPassive())
		{
	        // just ignore the passive skill request. why does the client send it anyway ??
			return;
		}

		//************************************* Check Casting in Progress *******************************************

        // If a skill is currently being used
        if (isCastingNow())
		{
            return;
		}

        //************************************* Check Target *******************************************

		// Get the target for the skill
		L2Object target = null;

		switch (skill.getTargetType())
		{
			// OWNER_PET should be cast even if no target has been found
			case TARGET_OWNER_PET:
				target = getOwner();
				break;
			// PARTY, AURA, SELF should be cast even if no target has been found
			case TARGET_PARTY:
			case TARGET_AURA:
			case TARGET_FRONT_AURA:
			case TARGET_BEHIND_AURA:
			case TARGET_SELF:
				target = this;
				break;
			default:
                // Get the first target of the list
			    target = skill.getFirstOfTargetList(this);
			    break;
		}

        // Check the validity of the target
        if (target == null)
        {
        	if (getOwner() != null)
        		getOwner().sendPacket(new SystemMessage(SystemMessageId.TARGET_CANT_FOUND));
            return;
        }

        //************************************* Check skill availability *******************************************

        // Check if this skill is enabled (e.g. reuse time)
        if (isSkillDisabled(skill.getId()))
        {
        	if (getOwner() != null) 
        	{
        		SystemMessage sm = new SystemMessage(SystemMessageId.S1_PREPARED_FOR_REUSE);
                sm.addSkillName(skill);
            	getOwner().sendPacket(sm);
        	}
            return;
        }

        //************************************* Check Consumables *******************************************

        // Check if the summon has enough MP
        if (getCurrentMp() < getStat().getMpConsume(skill) + getStat().getMpInitialConsume(skill))
        {
            // Send a System Message to the caster
        	if (getOwner() != null)
        		getOwner().sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_MP));
            return;
        }

        // Check if the summon has enough HP
        if (getCurrentHp() <= skill.getHpConsume())
        {
            // Send a System Message to the caster
        	if (getOwner() != null)
        		getOwner().sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_HP));
            return;
        }

        //************************************* Check Summon State *******************************************

        // Check if this is offensive magic skill
        if (skill.isOffensive())
		{
			if (isInsidePeaceZone(this, target)
					&& getOwner() != null
					&& (!getOwner().getAccessLevel().allowPeaceAttack()))
			{
				// If summon or target is in a peace zone, send a system message TARGET_IN_PEACEZONE
	        	sendPacket(new SystemMessage(SystemMessageId.TARGET_IN_PEACEZONE));
				return;
			}

			if (getOwner() != null && getOwner().isInOlympiadMode() && !getOwner().isOlympiadStart()){
				// if L2PcInstance is in Olympia and the match isn't already start, send a Server->Client packet ActionFailed
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}

            // Check if the target is attackable
			if (target instanceof L2DoorInstance)
        	{
				if(!((L2DoorInstance)target).isAttackable(getOwner()))
					return;
        	}
			else
			{
				if (!target.isAttackable()
            		&& getOwner() != null
            		&& (!getOwner().getAccessLevel().allowPeaceAttack()))
				{
					return;
				}

				// Check if a Forced ATTACK is in progress on non-attackable target
				if (!target.isAutoAttackable(this) && !forceUse &&
					skill.getTargetType() != SkillTargetType.TARGET_AURA &&
					skill.getTargetType() != SkillTargetType.TARGET_FRONT_AURA &&
					skill.getTargetType() != SkillTargetType.TARGET_BEHIND_AURA &&
					skill.getTargetType() != SkillTargetType.TARGET_CLAN &&
					skill.getTargetType() != SkillTargetType.TARGET_ALLY &&
					skill.getTargetType() != SkillTargetType.TARGET_PARTY &&
					skill.getTargetType() != SkillTargetType.TARGET_SELF)
				{
					return;
				}
			}
		}
        getOwner().setCurrentPetSkill(skill, forceUse, dontMove);
		// Notify the AI with AI_INTENTION_CAST and target
		getAI().setIntention(CtrlIntention.AI_INTENTION_CAST, skill, target);
	}

	@Override
	public void setIsImmobilized(boolean value)
	{
		super.setIsImmobilized(value);

		if (value)
		{
			_previousFollowStatus = getFollowStatus();
			// if immobilized temporarly disable follow mode
			if (_previousFollowStatus)
				setFollowStatus(false);
		}
		else
		{
			// if not more immobilized restore previous follow mode
			setFollowStatus(_previousFollowStatus);
		}
	}

	public void setOwner(L2PcInstance newOwner)
	{
		_owner = newOwner;
	}

	@Override
	public void sendDamageMessage(L2Character target, int damage, boolean mcrit, boolean pcrit, boolean miss)
	{
		if (miss) return;

		// Prevents the double spam of system messages, if the target is the owning player.
		if (target.getObjectId() != getOwner().getObjectId())
		{
			if (pcrit || mcrit)
				if (this instanceof L2SummonInstance)
					getOwner().sendPacket(new SystemMessage(SystemMessageId.CRITICAL_HIT_BY_SUMMONED_MOB));
				else
					getOwner().sendPacket(new SystemMessage(SystemMessageId.CRITICAL_HIT_BY_PET));

			if (getOwner().isInOlympiadMode() &&
					target instanceof L2PcInstance &&
					((L2PcInstance)target).isInOlympiadMode() &&
					((L2PcInstance)target).getOlympiadGameId() == getOwner().getOlympiadGameId())
			{
				Olympiad.getInstance().notifyCompetitorDamage(getOwner(), damage, getOwner().getOlympiadGameId());
			}

			final SystemMessage sm;
			
			if (target.isInvul() && !(target instanceof L2NpcInstance))
				sm = new SystemMessage(SystemMessageId.ATTACK_WAS_BLOCKED);
			else
			{
				if (this instanceof L2SummonInstance)
					sm = new SystemMessage(SystemMessageId.SUMMON_GAVE_DAMAGE_S1);
				else
					sm = new SystemMessage(SystemMessageId.PET_HIT_FOR_S1_DAMAGE);
				
				sm.addNumber(damage);
			}
			
			getOwner().sendPacket(sm);
		}
	}

	public void reduceCurrentHp(int damage, L2Character attacker, L2Skill skill)
	{
		super.reduceCurrentHp(damage, attacker, skill);
		SystemMessage sm;
		if (this instanceof L2SummonInstance)
			sm = new SystemMessage(SystemMessageId.SUMMON_RECEIVED_DAMAGE_S2_BY_S1);
		else
			sm = new SystemMessage(SystemMessageId.PET_RECEIVED_S2_DAMAGE_BY_C1);

		sm.addCharName(attacker);
		sm.addNumber(damage);
		getOwner().sendPacket(sm);
    }

	@Override
	public void doCast(L2Skill skill)
	{
        final L2PcInstance actingPlayer = getActingPlayer();

        if (!actingPlayer.checkPvpSkill(getTarget(), skill)
        		&& !actingPlayer.getAccessLevel().allowPeaceAttack())
        {
            // Send a System Message to the L2PcInstance
            actingPlayer.sendPacket(
                    new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));

            // Send a Server->Client packet ActionFailed to the L2PcInstance
            actingPlayer.sendPacket(ActionFailed.STATIC_PACKET);
            return;
        }

        super.doCast(skill);
	}
	
	public boolean isInCombat()
	{
		return getOwner().isInCombat();
	}

	@Override
	public L2PcInstance getActingPlayer()
	{
		return getOwner();
	}
	
	@Override
	public final void broadcastPacket(L2GameServerPacket mov)
	{
		mov.setInvisible(getOwner().getAppearance().getInvisible());
		super.broadcastPacket(mov);
	}

	@Override
	public final void broadcastPacket(L2GameServerPacket mov, int radiusInKnownlist)
	{
		mov.setInvisible(getOwner().getAppearance().getInvisible());
		super.broadcastPacket(mov, radiusInKnownlist);
	}

	public void updateAndBroadcastStatus(int val)
	{
		getOwner().sendPacket(new PetInfo(this,val));
		getOwner().sendPacket(new PetStatusUpdate(this));
		if (isVisible())
        {
			broadcastNpcInfo(val);
        }
        L2Party party = this.getOwner().getParty();
        if (party != null)
        {
            party.broadcastToPartyMembers(this.getOwner(), new ExPartyPetWindowUpdate(this));
        }
		updateEffectIcons(true);
	}

	public void broadcastNpcInfo(int val)
	{
		Collection<L2PcInstance> plrs = getKnownList().getKnownPlayers().values();
		for (L2PcInstance player : plrs)
		{
			try
			{
				if (player == getOwner() && !(this instanceof L2MerchantSummonInstance))
					continue;
				player.sendPacket(new AbstractNpcInfo.SummonInfo(this,player, val));
			}
			catch (NullPointerException e)
			{
				// ignore it
			}
		}
	}
	public boolean isHungry()
	{
		return false;
	}
	@Override
	public final boolean isAttackingNow()
	{
		return isInCombat();
	}

	public int getWeapon()
	{
		return 0;
	}
	
	public int getArmor()
	{
		return 0;
	}

	public int getPetSpeed()
	{
		return getTemplate().baseRunSpd;
	}
}
