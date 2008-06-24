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

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.Olympiad;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.ai.L2CharacterAI;
import net.sf.l2j.gameserver.ai.L2SummonAI;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.L2Skill.SkillTargetType;
import net.sf.l2j.gameserver.model.actor.instance.L2DoorInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PlayableInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SummonInstance;
import net.sf.l2j.gameserver.model.actor.knownlist.SummonKnownList;
import net.sf.l2j.gameserver.model.actor.stat.SummonStat;
import net.sf.l2j.gameserver.model.actor.status.SummonStatus;
import net.sf.l2j.gameserver.model.base.Experience;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.serverpackets.ExPartyPetWindowAdd;
import net.sf.l2j.gameserver.serverpackets.ExPartyPetWindowDelete;
import net.sf.l2j.gameserver.serverpackets.ExPartyPetWindowUpdate;
import net.sf.l2j.gameserver.serverpackets.MyTargetSelected;
import net.sf.l2j.gameserver.serverpackets.NpcInfo;
import net.sf.l2j.gameserver.serverpackets.PartySpelled;
import net.sf.l2j.gameserver.serverpackets.PetDelete;
import net.sf.l2j.gameserver.serverpackets.PetInfo;
import net.sf.l2j.gameserver.serverpackets.PetStatusShow;
import net.sf.l2j.gameserver.serverpackets.PetStatusUpdate;
import net.sf.l2j.gameserver.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.taskmanager.DecayTaskManager;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;
import net.sf.l2j.gameserver.templates.L2Weapon;

public abstract class L2Summon extends L2PlayableInstance
{
    //private static Logger _log = Logger.getLogger(L2Summon.class.getName());

	protected int _pkKills;
    private byte _pvpFlag;
    private L2PcInstance _owner;
    private int _karma = 0;
    private int _attackRange = 36; //Melee range
    private boolean _follow = true;
    private boolean _previousFollowStatus = true;
    private int _maxLoad;

    private int _chargedSoulShot;
    private int _chargedSpiritShot;

    // TODO: currently, all servitors use 1 shot.  However, this value
    // should vary depending on the servitor template (id and level)!
    private int _soulShotsPerHit = 1;
    private int _spiritShotsPerHit = 1;
	protected boolean _showSummonAnimation;

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

        _showSummonAnimation = true;
		_owner = owner;
		_ai = new L2SummonAI(new L2Summon.AIAccessor());

		setXYZInvisible(owner.getX()+50, owner.getY()+100, owner.getZ()+100);
	}
    
    @Override
    public void onSpawn()
    {
        super.onSpawn();
        
        this.setFollowStatus(true);
        this.setShowSummonAnimation(false); // addVisibleObject created the info packets with summon animation
                                              // if someone comes into range now, the animation shouldnt show any more
        this.getOwner().sendPacket(new PetInfo(this));
        
        L2Party party = this.getOwner().getParty();
        if (party != null)
        {
            party.broadcastToPartyMembers(this.getOwner(), new ExPartyPetWindowAdd(this));
        }
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
		if (_ai == null)
		{
			synchronized(this)
			{
				if (_ai == null)
					_ai = new L2SummonAI(new L2Summon.AIAccessor());
			}
		}

		return _ai;
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
		for (L2PcInstance player : getKnownList().getKnownPlayers().values())
			player.sendPacket(new NpcInfo(this, player));
    }

    /**
     * @return Returns the mountable.
     */
    public boolean isMountable()
    {
        return false;
    }
    
    public boolean isMountableOverTime()
    {
    	return false;
    }

	@Override
	public void onAction(L2PcInstance player)
    {
        if (player == _owner && player.getTarget() == this)
        {
            player.sendPacket(new PetStatusShow(this));
            player.sendPacket(ActionFailed.STATIC_PACKET);
        }
        else
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
        return _karma;
    }

    public void setKarma(int karma)
    {
        _karma = karma;
    }

    public final L2PcInstance getOwner()
    {
        return _owner;
    }

    public final int getNpcId()
    {
        return getTemplate().npcId;
    }

    public void setPvpFlag(byte pvpFlag)
    {
        _pvpFlag = pvpFlag;
    }

    public byte getPvpFlag()
    {
        return _pvpFlag;
    }

    public void setPkKills(int pkKills)
    {
        _pkKills = pkKills;
    }

    public final int getPkKills()
    {
        return _pkKills;
    }

    public final int getMaxLoad()
    {
        return _maxLoad;
    }

    public final int getSoulShotsPerHit()
    {
        return _soulShotsPerHit;
    }

    public final int getSpiritShotsPerHit()
    {
        return _spiritShotsPerHit;
    }

    public void setMaxLoad(int maxLoad)
    {
        _maxLoad = maxLoad;
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
        super.broadcastStatusUpdate();

        if (isVisible())
        {
            getOwner().sendPacket(new PetStatusUpdate(this));
            
            L2Party party = this.getOwner().getParty();
            if (party != null)
            {
                party.broadcastToPartyMembers(this.getOwner(), new ExPartyPetWindowUpdate(this));
            }
        }
        
    }
    
    @Override
    public void updateEffectIcons(boolean partyOnly)
    {
        PartySpelled ps = new PartySpelled(this);
        
        // Go through all effects if any
        L2Effect[] effects = getAllEffects();
        if (effects != null && effects.length > 0)
        {
            for (int i = 0; i < effects.length; i++)
            {
                L2Effect effect = effects[i];

                if (effect == null)
                    continue;
                
                if (effect.getInUse())
                {
                    effect.addPartySpelledIcon(ps);
                }
            }
        }
        
        L2Party party = this.getOwner().getParty();
        if (party != null)
        {
            // tell everyone about the summon effect
            party.broadcastToPartyMembers(ps);
        }
        else
        {
            // tell only the owner
            this.getOwner().sendPacket(ps);
        }
    }

    public void deleteMe(L2PcInstance owner)
    {
        getAI().stopFollow();
        owner.sendPacket(new PetDelete(getObjectId(), 2));

        //FIXME: I think it should really drop items to ground and only owner can take for a while
        giveAllToOwner();
        decayMe();
        getKnownList().removeAllKnownObjects();
        owner.setPet(null);
    }
    
    public void onSummon()
    {
        
    }

    public synchronized void unSummon(L2PcInstance owner)
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
	        
	        L2WorldRegion oldRegion = getWorldRegion();
		    decayMe();
		    if (oldRegion != null) oldRegion.removeFromZones(this);
            getKnownList().removeAllKnownObjects();
	        owner.setPet(null);
	        setTarget(null);
	        
	        if (this instanceof L2PetInstance)
	        	((L2PetInstance)this).setIsMountableOverTime(false);
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
        return;
    }

    public void giveAllToOwner()
    {
        return;
    }

    public void store()
    {
        return;
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

        // Check if this skill is enabled (ex : reuse time)
        if (isSkillDisabled(skill.getId())
        		&& getOwner() != null
        		&& (!getOwner().getAccessLevel().allowPeaceAttack()))
        {
            SystemMessage sm = new SystemMessage(SystemMessageId.S1_PREPARED_FOR_REUSE);
            sm.addSkillName(skill);
            getOwner().sendPacket(sm);
            return;
        }

        // Check if all skills are disabled
        if (isAllSkillsDisabled()
        		&& getOwner() != null
        		&& (!getOwner().getAccessLevel().allowPeaceAttack()))
        {
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
	public final void sendDamageMessage(L2Character target, int damage, boolean mcrit, boolean pcrit, boolean miss)
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
				Olympiad.getInstance().notifyCompetitorDamage(getOwner().getObjectId(), damage, getOwner().getOlympiadGameId());
			}

			SystemMessage sm;
			if (this instanceof L2SummonInstance)
				sm = new SystemMessage(SystemMessageId.SUMMON_GAVE_DAMAGE_S1);
			else
				sm = new SystemMessage(SystemMessageId.PET_HIT_FOR_S1_DAMAGE);
			sm.addNumber(damage);
			getOwner().sendPacket(sm);
		}
	}

	public void reduceCurrentHp(int damage, L2Character attacker)
	{
		super.reduceCurrentHp(damage, attacker);
		SystemMessage sm;
		if (this instanceof L2SummonInstance)
			sm = new SystemMessage(SystemMessageId.SUMMON_RECEIVED_DAMAGE_S2_BY_S1);
		else
			sm = new SystemMessage(SystemMessageId.PET_RECEIVED_S2_DAMAGE_BY_S1);

		sm.addCharName(attacker);
		sm.addNumber(damage);
		getOwner().sendPacket(sm);
    }

	/**
	 * @return Returns the showSummonAnimation.
	 */
	public boolean isShowSummonAnimation()
	{
	    return _showSummonAnimation;
	}

	/**
	 * @param showSummonAnimation The showSummonAnimation to set.
	 */
	public void setShowSummonAnimation(boolean showSummonAnimation)
	{
	    _showSummonAnimation = showSummonAnimation;
	}

	/**
	 * Servitors' skills automatically change their level based on the servitor's level.
	 * Until level 70, the servitor gets 1 lv of skill per 10 levels. After that, it is 1
	 * skill level per 5 servitor levels.  If the resulting skill level doesn't exist use
	 * the max that does exist!
	 *
	 * @see net.sf.l2j.gameserver.model.L2Character#doCast(net.sf.l2j.gameserver.model.L2Skill)
	 */
	@Override
	public void doCast(L2Skill skill)
	{
		int petLevel = getLevel();
		int skillLevel = petLevel/10;
    	if(petLevel >= 70)
    		skillLevel += (petLevel-65)/10;

    	// adjust the level for servitors less than lv 10
    	if (skillLevel < 1)
    		skillLevel = 1;

    	L2Skill skillToCast = SkillTable.getInstance().getInfo(skill.getId(),skillLevel);

    	if (skillToCast != null)
    		super.doCast(skillToCast);
    	else
    		super.doCast(skill);
	}
}
