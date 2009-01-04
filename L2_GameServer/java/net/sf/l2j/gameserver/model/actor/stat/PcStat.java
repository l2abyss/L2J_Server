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
package net.sf.l2j.gameserver.model.actor.stat;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.model.base.Experience;
import net.sf.l2j.gameserver.model.quest.QuestState;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.PledgeShowMemberListUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;
import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.network.serverpackets.UserInfo;

public class PcStat extends PlayableStat
{
	//private static Logger _log = Logger.getLogger(PcStat.class.getName());

    // =========================================================
    // Data Field

    private int _oldMaxHp;      // stats watch
    private int _oldMaxMp;      // stats watch
    private int _oldMaxCp;		// stats watch

    // =========================================================
    // Constructor
    public PcStat(L2PcInstance activeChar)
    {
        super(activeChar);
    }

    // =========================================================
    // Method - Public
    @Override
	public boolean addExp(long value)
    {
    	L2PcInstance activeChar = getActiveChar();

        // Allowed to gain exp?
    	if (!getActiveChar().getAccessLevel().canGainExp() && getActiveChar().isInParty())
              return false;

		if (!super.addExp(value)) return false;

        // Set new karma
        if (!activeChar.isCursedWeaponEquipped() && activeChar.getKarma() > 0 && (activeChar.isGM() || !activeChar.isInsideZone(L2Character.ZONE_PVP)))
        {
            int karmaLost = activeChar.calculateKarmaLost(value);
            if (karmaLost > 0)
            	activeChar.setKarma(activeChar.getKarma() - karmaLost);
        }

        // EXP status update currently not used in retail
        activeChar.sendPacket(new UserInfo(activeChar));
        return true;
    }

    /**
     * Add Experience and SP rewards to the L2PcInstance, remove its Karma (if necessary) and Launch increase level task.<BR><BR>
     *
     * <B><U> Actions </U> :</B><BR><BR>
     * <li>Remove Karma when the player kills L2MonsterInstance</li>
     * <li>Send a Server->Client packet StatusUpdate to the L2PcInstance</li>
     * <li>Send a Server->Client System Message to the L2PcInstance </li>
     * <li>If the L2PcInstance increases it's level, send a Server->Client packet SocialAction (broadcast) </li>
     * <li>If the L2PcInstance increases it's level, manage the increase level task (Max MP, Max MP, Recommandation, Expertise and beginner skills...) </li>
     * <li>If the L2PcInstance increases it's level, send a Server->Client packet UserInfo to the L2PcInstance </li><BR><BR>
     *
     * @param addToExp The Experience value to add
     * @param addToSp The SP value to add
     */
    @Override
	public boolean addExpAndSp(long addToExp, int addToSp)
    {
    	float ratioTakenByPet = 0;
    	// Allowd to gain exp/sp?
    	L2PcInstance activeChar = getActiveChar();
    	if (!activeChar.getAccessLevel().canGainExp() && activeChar.isInParty())
     	     return false;

    	// if this player has a pet that takes from the owner's Exp, give the pet Exp now

    	if (activeChar.getPet() instanceof L2PetInstance )
    	{
    		L2PetInstance pet = (L2PetInstance) activeChar.getPet();
    		ratioTakenByPet = pet.getPetData().getOwnerExpTaken();

    		// only give exp/sp to the pet by taking from the owner if the pet has a non-zero, positive ratio
    		// allow possible customizations that would have the pet earning more than 100% of the owner's exp/sp
    		if (ratioTakenByPet > 0 && !pet.isDead())
    			pet.addExpAndSp((long)(addToExp*ratioTakenByPet), (int)(addToSp*ratioTakenByPet));
    		// now adjust the max ratio to avoid the owner earning negative exp/sp
    		if (ratioTakenByPet > 1)
    			ratioTakenByPet = 1;
    		addToExp = (long)(addToExp*(1-ratioTakenByPet));
    		addToSp = (int)(addToSp*(1-ratioTakenByPet));
    	}

    	if ( !super.addExpAndSp(addToExp, addToSp) ) return false;

        // Send a Server->Client System Message to the L2PcInstance
    	if ((int)addToExp == 0)
    	{
    		SystemMessage sm = new SystemMessage(SystemMessageId.ACQUIRED_S1_SP);
    		sm.addNumber(addToSp);
    		activeChar.sendPacket(sm);
    	}
    	else
    	{
    		SystemMessage sm = new SystemMessage(SystemMessageId.YOU_EARNED_S1_EXP_AND_S2_SP);
            sm.addNumber((int)addToExp);
            sm.addNumber(addToSp);
            activeChar.sendPacket(sm);
    	}
        return true;
    }

    @Override
	public boolean removeExpAndSp(long addToExp, int addToSp)
    {
        if (!super.removeExpAndSp(addToExp, addToSp)) return false;

        // Send a Server->Client System Message to the L2PcInstance
        SystemMessage sm = new SystemMessage(SystemMessageId.EXP_DECREASED_BY_S1);
        sm.addNumber((int)addToExp);
        getActiveChar().sendPacket(sm);
        sm = new SystemMessage(SystemMessageId.SP_DECREASED_S1);
        sm.addNumber(addToSp);
        getActiveChar().sendPacket(sm);
        return true;
    }

    @Override
	public final boolean addLevel(byte value)
    {
		if (getLevel() + value > Experience.MAX_LEVEL - 1) return false;

        boolean levelIncreased = super.addLevel(value);

        if (levelIncreased)
        {
        	QuestState qs = getActiveChar().getQuestState("255_Tutorial"); 
        		if (qs != null)
        			qs.getQuest().notifyEvent("CE40", null, getActiveChar());

        	getActiveChar().setCurrentCp(getMaxCp());
            getActiveChar().broadcastPacket(new SocialAction(getActiveChar().getObjectId(), 15));
            getActiveChar().sendPacket(new SystemMessage(SystemMessageId.YOU_INCREASED_YOUR_LEVEL));
        }

        getActiveChar().rewardSkills(); // Give Expertise skill of this level
        if (getActiveChar().getClan() != null)
        {
        	getActiveChar().getClan().updateClanMember(getActiveChar());
        	getActiveChar().getClan().broadcastToOnlineMembers(new PledgeShowMemberListUpdate(getActiveChar()));
        }
        if (getActiveChar().isInParty()) getActiveChar().getParty().recalculatePartyLevel(); // Recalculate the party level

        if (getActiveChar().isTransformed())
        	getActiveChar().getTransformation().onLevelUp();
        
        StatusUpdate su = new StatusUpdate(getActiveChar().getObjectId());
        su.addAttribute(StatusUpdate.LEVEL, getLevel());
        su.addAttribute(StatusUpdate.MAX_CP, getMaxCp());
        su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
        su.addAttribute(StatusUpdate.MAX_MP, getMaxMp());
        getActiveChar().sendPacket(su);

        // Update the overloaded status of the L2PcInstance
        getActiveChar().refreshOverloaded();
        // Update the expertise status of the L2PcInstance
        getActiveChar().refreshExpertisePenalty();
        // Send a Server->Client packet UserInfo to the L2PcInstance
        getActiveChar().sendPacket(new UserInfo(getActiveChar()));

        return levelIncreased;
    }

    @Override
	public boolean addSp(int value)
    {
        if (!super.addSp(value)) return false;

        StatusUpdate su = new StatusUpdate(getActiveChar().getObjectId());
        su.addAttribute(StatusUpdate.SP, getSp());
        getActiveChar().sendPacket(su);

        return true;
    }

    @Override
	public final long getExpForLevel(int level) { return Experience.LEVEL[level]; }

    // =========================================================
    // Method - Private

    // =========================================================
    // Property - Public
    @Override
	public final L2PcInstance getActiveChar() { return (L2PcInstance)super.getActiveChar(); }

    @Override
	public final long getExp()
    {
        if (getActiveChar().isSubClassActive()) 
	        return getActiveChar().getSubClasses().get(getActiveChar().getClassIndex()).getExp();
        
        return super.getExp();
    }
    
    @Override
	public final void setExp(long value)
    {
        if (getActiveChar().isSubClassActive())
            getActiveChar().getSubClasses().get(getActiveChar().getClassIndex()).setExp(value);
        else
            super.setExp(value);
    }

    @Override
	public final byte getLevel()
    {
        if (getActiveChar().isSubClassActive()) 
        	return getActiveChar().getSubClasses().get(getActiveChar().getClassIndex()).getLevel();
        	
        return super.getLevel();
    }
    @Override
	public final void setLevel(byte value)
    {
		if (value > Experience.MAX_LEVEL - 1) 
			value = Experience.MAX_LEVEL - 1;
        	
        if (getActiveChar().isSubClassActive())
            getActiveChar().getSubClasses().get(getActiveChar().getClassIndex()).setLevel(value);
        else
            super.setLevel(value);
    }

    @Override
    public final int getMaxCp()
    {
    	// Get the Max CP (base+modifier) of the L2PcInstance
    	int val = super.getMaxCp();
    	if (val != _oldMaxCp)
    	{
    		_oldMaxCp = val;
    		
    		// Launch a regen task if the new Max CP is higher than the old one
    		if (getActiveChar().getStatus().getCurrentCp() != val) getActiveChar().getStatus().setCurrentCp(getActiveChar().getStatus().getCurrentCp()); // trigger start of regeneration
    	}
    	return val;
    }
    
    @Override
	public final int getMaxHp()
    {
        // Get the Max HP (base+modifier) of the L2PcInstance
        int val = super.getMaxHp();
        if (val != _oldMaxHp)
        {
            _oldMaxHp = val;

            // Launch a regen task if the new Max HP is higher than the old one
            if (getActiveChar().getStatus().getCurrentHp() != val) getActiveChar().getStatus().setCurrentHp(getActiveChar().getStatus().getCurrentHp()); // trigger start of regeneration
        }

        return val;
    }

    @Override
	public final int getMaxMp()
    {
        // Get the Max MP (base+modifier) of the L2PcInstance
        int val = super.getMaxMp();
        
        if (val != _oldMaxMp)
        {
            _oldMaxMp = val;

            // Launch a regen task if the new Max MP is higher than the old one
            if (getActiveChar().getStatus().getCurrentMp() != val) 
            	getActiveChar().getStatus().setCurrentMp(getActiveChar().getStatus().getCurrentMp()); // trigger start of regeneration
        }

        return val;
    }

    @Override
	public final int getSp()
    {
        if (getActiveChar().isSubClassActive()) 
        	return getActiveChar().getSubClasses().get(getActiveChar().getClassIndex()).getSp();
        	
        return super.getSp();
    }
    @Override
	public final void setSp(int value)
    {
        if (getActiveChar().isSubClassActive())
            getActiveChar().getSubClasses().get(getActiveChar().getClassIndex()).setSp(value);
        else
            super.setSp(value);
    }
    
    @Override
    public final int getRunSpeed()
    {
    	int val = super.getRunSpeed();
    	
    	if (getActiveChar().isFlying())
		{
			val += Config.WYVERN_SPEED;
			return val;
		}
    	else if (getActiveChar().isRidingStrider())
		{
			val += Config.STRIDER_SPEED;
			return val;
		}
    	else if (getActiveChar().isRidingFenrirWolf())
		{
			val += Config.FENRIR_SPEED;
			return val;
		}
    	else if (getActiveChar().isRidingWFenrirWolf())
		{
			val += Config.SNOW_FENRIR_SPEED;
			return val;
		}
    	else if (getActiveChar().isRidingGreatSnowWolf())
		{
			val += Config.GREAT_SNOW_WOLF_SPEED;
			return val;
		}
    	
    	// Apply max run speed cap.
		if (val > Config.MAX_RUN_SPEED && !getActiveChar().isGM())
			return Config.MAX_RUN_SPEED;
    	
    	return val;
    }
    
    @Override
    public int getPAtkSpd()
    {
    	int val = super.getPAtkSpd();
    	
    	if (val > Config.MAX_PATK_SPEED && !getActiveChar().isGM())
			return Config.MAX_PATK_SPEED;
    	
    	return val;
    }
    
    @Override
    public int getEvasionRate(L2Character target)
	{
    	int val = super.getEvasionRate(target);
		
    	if (val > Config.MAX_EVASION && !getActiveChar().isGM())
			return  Config.MAX_EVASION;
		
    	return val;
	}
    
    @Override
    public int getMAtkSpd()
	{
    	int val = super.getMAtkSpd();
		
    	if (val > Config.MAX_MATK_SPEED && !getActiveChar().isGM())
			return Config.MAX_MATK_SPEED;
		
    	return val;
	}
}
