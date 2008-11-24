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
package net.sf.l2j.gameserver.model.actor.status;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.instancemanager.DuelManager;
import net.sf.l2j.gameserver.model.L2Attackable;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SummonInstance;
import net.sf.l2j.gameserver.model.actor.stat.CharStat;
import net.sf.l2j.gameserver.model.entity.Duel;
import net.sf.l2j.gameserver.model.quest.QuestState;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.util.Rnd;

public class CharStatus
{
    protected static final Logger _log = Logger.getLogger(CharStatus.class.getName());
    
    // =========================================================
    // Data Field
    private L2Character _activeChar;
    
    private double _currentCp = 0; //Current CP of the L2Character
    
    private double _currentHp = 0; //Current HP of the L2Character
    
    private double _currentMp = 0; //Current MP of the L2Character
    
    /** Array containing all clients that need to be notified about hp/mp updates of the L2Character */
    private Set<L2Character> _StatusListener;
    
    private Future<?> _regTask;
    
    private byte _flagsRegenActive = 0;
    
    private static final byte REGEN_FLAG_CP = 4;
    
    private static final byte REGEN_FLAG_HP = 1;
    
    private static final byte REGEN_FLAG_MP = 2;
    
    // =========================================================
    // Constructor
    public CharStatus(L2Character activeChar)
    {
        _activeChar = activeChar;
    }
    
    /**
     * Add the object to the list of L2Character that must be informed of HP/MP updates of this L2Character.<BR><BR>
     *
     * <B><U> Concept</U> :</B><BR><BR>
     * Each L2Character owns a list called <B>_statusListener</B> that contains all L2PcInstance to inform of HP/MP updates.
     * Players who must be informed are players that target this L2Character.
     * When a RegenTask is in progress sever just need to go through this list to send Server->Client packet StatusUpdate.<BR><BR>
     *
     * <B><U> Example of use </U> :</B><BR><BR>
     * <li> Target a PC or NPC</li><BR><BR>
     *
     * @param object L2Character to add to the listener
     *
     */
    public final void addStatusListener(L2Character object)
    {
        if (object == getActiveChar())
            return;
        
        synchronized (getStatusListener())
        {
            getStatusListener().add(object);
        }
    }
    
    public final void reduceCp(int value)
    {
        if (getCurrentCp() > value)
            setCurrentCp(getCurrentCp() - value);
        else
            setCurrentCp(0);
    }
    
    /**
     * Reduce the current HP of the L2Character and launch the doDie Task if necessary.<BR><BR>
     *
     * <B><U> Overridden in </U> :</B><BR><BR>
     * <li> L2Attackable : Update the attacker AggroInfo of the L2Attackable _aggroList</li><BR><BR>
     *
     * @param i The HP decrease value
     * @param attacker The L2Character who attacks
     * @param awake The awake state (If True : stop sleeping)
     *
     */
    public void reduceHp(double value, L2Character attacker)
    {
        reduceHp(value, attacker, true);
    }
    
    public void reduceHp(double value, L2Character attacker, boolean awake)
    {
        if (getActiveChar().isInvul())
            return;
        if (getActiveChar().isDead())
            return;
        
        if (attacker instanceof L2PcInstance)
		{
			L2PcInstance pcInst = (L2PcInstance)attacker;
			if (pcInst.isGM() && !pcInst.getAccessLevel().canGiveDamage())
				return ;
		}

        if (getActiveChar() instanceof L2PcInstance)
        {
            if (((L2PcInstance) getActiveChar()).isInDuel())
            {
                // the duel is finishing - players do not recive damage
                if (((L2PcInstance) getActiveChar()).getDuelState() == Duel.DUELSTATE_DEAD)
                    return;
                else if (((L2PcInstance) getActiveChar()).getDuelState() == Duel.DUELSTATE_WINNER)
                    return;
                
                // cancel duel if player got hit by another player, that is not part of the duel or a monster
                if (!(attacker instanceof L2SummonInstance)
                        && !(attacker instanceof L2PcInstance && ((L2PcInstance) attacker).getDuelId() == ((L2PcInstance) getActiveChar()).getDuelId()))
                {
                    ((L2PcInstance) getActiveChar()).setDuelState(Duel.DUELSTATE_INTERRUPTED);
                }
            }
        }
        else
        {
            if (attacker instanceof L2PcInstance
                    && ((L2PcInstance) attacker).isInDuel()
                    && !(getActiveChar() instanceof L2SummonInstance && ((L2SummonInstance) getActiveChar()).getOwner().getDuelId() == ((L2PcInstance) attacker).getDuelId())) // Duelling player attacks mob
            {
                ((L2PcInstance) attacker).setDuelState(Duel.DUELSTATE_INTERRUPTED);
            }
        }
        if (awake && getActiveChar().isSleeping())
            getActiveChar().stopSleeping(null);
        if (getActiveChar().isStunned() && Rnd.get(10) == 0)
            getActiveChar().stopStunning(null);
        if (getActiveChar().isImmobileUntilAttacked())
            getActiveChar().stopImmobileUntilAttacked(null);
        
        // Add attackers to npc's attacker list
        if (getActiveChar() instanceof L2NpcInstance)
            getActiveChar().addAttackerToAttackByList(attacker);
        
        if (value > 0) // Reduce Hp if any
        {
            // If we're dealing with an L2Attackable Instance and the attacker hit it with an over-hit enabled skill, set the over-hit values.
            // Anything else, clear the over-hit flag
            if (getActiveChar() instanceof L2Attackable)
            {
                if (((L2Attackable) getActiveChar()).isOverhit())
                    ((L2Attackable) getActiveChar()).setOverhitValues(attacker, value);
                else
                    ((L2Attackable) getActiveChar()).overhitEnabled(false);
            }
            value = getCurrentHp() - value; // Get diff of Hp vs value
            if (value <= 0)
            {
                // is the dying a duelist? if so, change his duel state to dead
                if (getActiveChar() instanceof L2PcInstance
                        && ((L2PcInstance) getActiveChar()).isInDuel())
                {
                    getActiveChar().disableAllSkills();
                    stopHpMpRegeneration();
                    attacker.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
                    attacker.sendPacket(ActionFailed.STATIC_PACKET);
                    
                    // let the DuelManager know of his defeat
                    DuelManager.getInstance().onPlayerDefeat((L2PcInstance) getActiveChar());
                    value = 1;
                }
                else
                    value = 0; // Set value to 0 if Hp < 0
            }
            setCurrentHp(value); // Set Hp
        }
        else
        {
            // If we're dealing with an L2Attackable Instance and the attacker's hit didn't kill the mob, clear the over-hit flag
            if (getActiveChar() instanceof L2Attackable)
            {
                ((L2Attackable) getActiveChar()).overhitEnabled(false);
            }
        }
        
        if (getActiveChar().getCurrentHp() < 0.5) // Die
        {
            getActiveChar().abortAttack();
            getActiveChar().abortCast();
            
            if (getActiveChar() instanceof L2PcInstance)
            {
                if (((L2PcInstance) getActiveChar()).isInOlympiadMode())
                {
                    stopHpMpRegeneration();
                    getActiveChar().setIsDead(true);
                    getActiveChar().setIsPendingRevive(true);
                    return;
                }
            }
            
            // first die (and calculate rewards), if currentHp < 0,
            // then overhit may be calculated
            if (Config.DEBUG)
                _log.fine("char is dead.");
            
            // Start the doDie process
            getActiveChar().doDie(attacker);
            
            if (getActiveChar() instanceof L2PcInstance) 
            { 
            	QuestState qs = ((L2PcInstance) getActiveChar()).getQuestState("255_Tutorial"); 
            	if (qs != null) 
            		qs.getQuest().notifyEvent("CE30", null, ((L2PcInstance) getActiveChar())); 
            } 
        }
        else
        {
            // If we're dealing with an L2Attackable Instance and the attacker's hit didn't kill the mob, clear the over-hit flag
            if (getActiveChar() instanceof L2Attackable)
            {
                ((L2Attackable) getActiveChar()).overhitEnabled(false);
            }
        }
    }
    
    public void reduceMp(double value)
    {
        value = getCurrentMp() - value;
        if (value < 0)
            value = 0;
        setCurrentMp(value);
    }
    
    /**
     * Remove the object from the list of L2Character that must be informed of HP/MP updates of this L2Character.<BR><BR>
     *
     * <B><U> Concept</U> :</B><BR><BR>
     * Each L2Character owns a list called <B>_statusListener</B> that contains all L2PcInstance to inform of HP/MP updates.
     * Players who must be informed are players that target this L2Character.
     * When a RegenTask is in progress sever just need to go through this list to send Server->Client packet StatusUpdate.<BR><BR>
     *
     * <B><U> Example of use </U> :</B><BR><BR>
     * <li> Untarget a PC or NPC</li><BR><BR>
     *
     * @param object L2Character to add to the listener
     *
     */
    public final void removeStatusListener(L2Character object)
    {
        synchronized (getStatusListener())
        {
            getStatusListener().remove(object);
        }
    }
    
    /**
     * Start the HP/MP/CP Regeneration task.<BR><BR>
     *
     * <B><U> Actions</U> :</B><BR><BR>
     * <li>Calculate the regen task period </li>
     * <li>Launch the HP/MP/CP Regeneration task with Medium priority </li><BR><BR>
     *
     */
    public synchronized final void startHpMpRegeneration()
    {
        if (_regTask == null && !getActiveChar().isDead())
        {
            if (Config.DEBUG)
                _log.fine("HP/MP/CP regen started");
            
            // Get the Regeneration periode
            int period = Formulas.getInstance().getRegeneratePeriod(getActiveChar());
            
            // Create the HP/MP/CP Regeneration task
            _regTask = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new RegenTask(), period, period);
        }
    }
    
    /**
     * Stop the HP/MP/CP Regeneration task.<BR><BR>
     *
     * <B><U> Actions</U> :</B><BR><BR>
     * <li>Set the RegenActive flag to False </li>
     * <li>Stop the HP/MP/CP Regeneration task </li><BR><BR>
     *
     */
    public synchronized final void stopHpMpRegeneration()
    {
        if (_regTask != null)
        {
            if (Config.DEBUG)
                _log.fine("HP/MP/CP regen stop");
            
            // Stop the HP/MP/CP Regeneration task
            _regTask.cancel(false);
            _regTask = null;
            
            // Set the RegenActive flag to false
            _flagsRegenActive = 0;
        }
    }
    
    // =========================================================
    // Method - Private
    
    // =========================================================
    // Property - Public
    public L2Character getActiveChar()
    {
        return _activeChar;
    }
    
    public final double getCurrentCp()
    {
        return _currentCp;
    }
    
    public final void setCurrentCp(double newCp)
    {
        setCurrentCp(newCp, true);
    }
    
    public final void setCurrentCp(double newCp, boolean broadcastPacket)
    {
    	// Get the Max CP of the L2Character
        int maxCp = getActiveChar().getStat().getMaxCp();

    	synchronized (this)
        {
        	if (getActiveChar().isDead()) return;
            
            if (newCp < 0)
                newCp = 0;
            
            if (newCp >= maxCp)
            {
                // Set the RegenActive flag to false
                _currentCp = maxCp;
                _flagsRegenActive &= ~REGEN_FLAG_CP;
                
                // Stop the HP/MP/CP Regeneration task
                if (_flagsRegenActive == 0)
                    stopHpMpRegeneration();
            }
            else
            {
                // Set the RegenActive flag to true
                _currentCp = newCp;
                _flagsRegenActive |= REGEN_FLAG_CP;
                
                // Start the HP/MP/CP Regeneration task with Medium priority
                startHpMpRegeneration();
            }
        }
        
        // Send the Server->Client packet StatusUpdate with current HP and MP to all other L2PcInstance to inform
        if (broadcastPacket)
            getActiveChar().broadcastStatusUpdate();
    }
    
    public final double getCurrentHp()
    {
        return _currentHp;
    }
    
    public final void setCurrentHp(double newHp)
    {
        setCurrentHp(newHp, true);
    }
    
    public final void setCurrentHp(double newHp, boolean broadcastPacket)
    {
        // Get the Max HP of the L2Character
        double maxHp = getActiveChar().getStat().getMaxHp();
        
        synchronized (this)
        {
            if (getActiveChar().isDead()) return;
        	if (newHp >= maxHp)
            {
                // Set the RegenActive flag to false
                _currentHp = maxHp;
                _flagsRegenActive &= ~REGEN_FLAG_HP;
                
                // Stop the HP/MP/CP Regeneration task
                if (_flagsRegenActive == 0)
                    stopHpMpRegeneration();
            }
            else
            {
                // Set the RegenActive flag to true
                _currentHp = newHp;
                _flagsRegenActive |= REGEN_FLAG_HP;
                
                // Start the HP/MP/CP Regeneration task with Medium priority
                startHpMpRegeneration();
            }
        }
        
        if (getActiveChar() instanceof L2PcInstance)
        {
            if (getCurrentHp() <= maxHp * .3)
            {
                QuestState qs = ((L2PcInstance) getActiveChar()).getQuestState("255_Tutorial");
                if (qs != null)
                    qs.getQuest().notifyEvent("CE45", null, ((L2PcInstance) getActiveChar()));
            }
        }
        
        // Send the Server->Client packet StatusUpdate with current HP and MP to all other L2PcInstance to inform
        if (broadcastPacket)
            getActiveChar().broadcastStatusUpdate();
    }
    
    public final void setCurrentHpMp(double newHp, double newMp)
    {
        setCurrentHp(newHp, false);
        setCurrentMp(newMp, true); //send the StatusUpdate only once
    }
    
    public final double getCurrentMp()
    {
        return _currentMp;
    }
    
    public final void setCurrentMp(double newMp)
    {
        setCurrentMp(newMp, true);
    }
    
    public final void setCurrentMp(double newMp, boolean broadcastPacket)
    {
    	// Get the Max MP of the L2Character
        int maxMp = getActiveChar().getStat().getMaxMp();
        
    	synchronized (this)
        {
        	if (getActiveChar().isDead()) return;
            
            if (newMp >= maxMp)
            {
                // Set the RegenActive flag to false
                _currentMp = maxMp;
                _flagsRegenActive &= ~REGEN_FLAG_MP;
                
                // Stop the HP/MP/CP Regeneration task
                if (_flagsRegenActive == 0)
                    stopHpMpRegeneration();
            }
            else
            {
                // Set the RegenActive flag to true
                _currentMp = newMp;
                _flagsRegenActive |= REGEN_FLAG_MP;
                
                // Start the HP/MP/CP Regeneration task with Medium priority
                startHpMpRegeneration();
            }
        }
        
        // Send the Server->Client packet StatusUpdate with current HP and MP to all other L2PcInstance to inform
        if (broadcastPacket)
            getActiveChar().broadcastStatusUpdate();
    }
    
    /**
     * Return the list of L2Character that must be informed of HP/MP updates of this L2Character.<BR><BR>
     *
     * <B><U> Concept</U> :</B><BR><BR>
     * Each L2Character owns a list called <B>_statusListener</B> that contains all L2PcInstance to inform of HP/MP updates.
     * Players who must be informed are players that target this L2Character.
     * When a RegenTask is in progress sever just need to go through this list to send Server->Client packet StatusUpdate.<BR><BR>
     *
     * @return The list of L2Character to inform or null if empty
     *
     */
    public final Set<L2Character> getStatusListener()
    {
        if (_StatusListener == null)
            _StatusListener = new CopyOnWriteArraySet<L2Character>();
        return _StatusListener;
    }
    
    // =========================================================
    // Runnable
    /** Task of HP/MP/CP regeneration */
    class RegenTask implements Runnable
    {
        public void run()
        {
            try
            {
                CharStat charstat = getActiveChar().getStat();
                
                // Modify the current CP of the L2Character and broadcast Server->Client packet StatusUpdate
                if (getCurrentCp() < charstat.getMaxCp())
                    setCurrentCp(getCurrentCp()
                            + Formulas.getInstance().calcCpRegen(getActiveChar()), false);
                
                // Modify the current HP of the L2Character and broadcast Server->Client packet StatusUpdate
                if (getCurrentHp() < charstat.getMaxHp())
                    setCurrentHp(getCurrentHp()
                            + Formulas.getInstance().calcHpRegen(getActiveChar()), false);
                
                // Modify the current MP of the L2Character and broadcast Server->Client packet StatusUpdate
                if (getCurrentMp() < charstat.getMaxMp())
                    setCurrentMp(getCurrentMp()
                            + Formulas.getInstance().calcMpRegen(getActiveChar()), false);
                
                if (!getActiveChar().isInActiveRegion())
                {
                    // no broadcast necessary for characters that are in inactive regions.
                    // stop regeneration for characters who are filled up and in an inactive region.
                    if ((getCurrentCp() == charstat.getMaxCp())
                            && (getCurrentHp() == charstat.getMaxHp())
                            && (getCurrentMp() == charstat.getMaxMp()))
                        stopHpMpRegeneration();
                }
                else
                    getActiveChar().broadcastStatusUpdate(); //send the StatusUpdate packet
            }
            catch (Exception e)
            {
                _log.log(Level.SEVERE, "", e);
            }
        }
    }
}
