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
package net.sf.l2j.gameserver.model.actor.instance;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastList;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.Olympiad;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.handler.SkillHandler;
import net.sf.l2j.gameserver.handler.skillhandlers.Continuous;
import net.sf.l2j.gameserver.handler.skillhandlers.Disablers;
import net.sf.l2j.gameserver.handler.skillhandlers.Mdam;
import net.sf.l2j.gameserver.instancemanager.DuelManager;
import net.sf.l2j.gameserver.model.L2Attackable;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2Party;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Summon;
import net.sf.l2j.gameserver.model.L2Trap;
import net.sf.l2j.gameserver.model.entity.TvTEvent;
import net.sf.l2j.gameserver.model.entity.TvTEventTeam;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.skills.l2skills.L2SkillDrain;
import net.sf.l2j.gameserver.taskmanager.AttackStanceTaskManager;
import net.sf.l2j.util.Rnd;

public class L2CubicInstance
{
    protected static final Logger _log = Logger.getLogger(L2CubicInstance.class.getName());

    // Type of Cubics
    public static final int STORM_CUBIC = 1;
    public static final int VAMPIRIC_CUBIC = 2;
    public static final int LIFE_CUBIC = 3;
    public static final int VIPER_CUBIC = 4;
    public static final int POLTERGEIST_CUBIC = 5;
    public static final int BINDING_CUBIC = 6;
    public static final int AQUA_CUBIC = 7;
    public static final int SPARK_CUBIC = 8;
    public static final int ATTRACT_CUBIC = 9;
    public static final int SMART_CUBIC_EVATEMPLAR = 10;
    public static final int SMART_CUBIC_SHILLIENTEMPLAR = 11;
    public static final int SMART_CUBIC_ARCANALORD = 12;
    public static final int SMART_CUBIC_ELEMENTALMASTER = 13;
    public static final int SMART_CUBIC_SPECTRALMASTER = 14;
    
    // Max range of cubic skills
    // TODO: Check/fix the max range 
    public static final int MAX_MAGIC_RANGE = 900;

    // Cubic skills
    public static final int SKILL_CUBIC_HEAL = 4051;
    public static final int SKILL_CUBIC_CURE = 5579;

    protected L2PcInstance _owner;
    protected L2Character _target;

    protected int _id;
    protected int _matk;
    protected int _activationtime;
    protected int _activationchance;
    protected boolean _active;
    
    protected List<L2Skill> _skills = new FastList<L2Skill>();
    
    private Future<?> _disappearTask;
    private Future<?> _actionTask;
    
    public L2CubicInstance(L2PcInstance owner, int id, int level, int mAtk, int activationtime, int activationchance)
    {
        _owner = owner;
        _id = id;
        _matk = mAtk;
        _activationtime = activationtime * 1000;
        _activationchance = activationchance;
        _active = false;

        switch (_id)
        {
            case STORM_CUBIC:
                _skills.add(SkillTable.getInstance().getInfo(4049, level));
                break;
            case VAMPIRIC_CUBIC:
                _skills.add(SkillTable.getInstance().getInfo(4050, level));
                break;
            case LIFE_CUBIC:
                _skills.add(SkillTable.getInstance().getInfo(4051, level));
                _disappearTask = ThreadPoolManager.getInstance().scheduleGeneral(new Disappear(), 3600000); // disappear in 60 mins
                doAction();
                break;
            case VIPER_CUBIC:
                _skills.add(SkillTable.getInstance().getInfo(4052, level));
                break;
            case POLTERGEIST_CUBIC:
                _skills.add(SkillTable.getInstance().getInfo(4053, level));
                _skills.add(SkillTable.getInstance().getInfo(4054, level));
                _skills.add(SkillTable.getInstance().getInfo(4055, level));
                break;
            case BINDING_CUBIC:
                _skills.add(SkillTable.getInstance().getInfo(4164, level));
                break;
            case AQUA_CUBIC:
                _skills.add(SkillTable.getInstance().getInfo(4165, level));
                break;
            case SPARK_CUBIC:
                _skills.add(SkillTable.getInstance().getInfo(4166, level));
                break;
            case ATTRACT_CUBIC:
                _skills.add(SkillTable.getInstance().getInfo(5115, level));
                _skills.add(SkillTable.getInstance().getInfo(5116, level));
                break;
            case SMART_CUBIC_ARCANALORD:
            	// _skills.add(SkillTable.getInstance().getInfo(4049,8)); no animation
            	// _skills.add(SkillTable.getInstance().getInfo(4050,7)); no animation
            	_skills.add(SkillTable.getInstance().getInfo(4051,7)); // have animation
            	// _skills.add(SkillTable.getInstance().getInfo(4052,6)); no animation
            	// _skills.add(SkillTable.getInstance().getInfo(4053,8)); no animation
            	// _skills.add(SkillTable.getInstance().getInfo(4054,8)); no animation
            	// _skills.add(SkillTable.getInstance().getInfo(4055,8)); no animation
            	// _skills.add(SkillTable.getInstance().getInfo(4164,9)); no animation
            	_skills.add(SkillTable.getInstance().getInfo(4165,9)); // have animation
            	// _skills.add(SkillTable.getInstance().getInfo(4166,9)); no animation
            	// _skills.add(SkillTable.getInstance().getInfo(5115,4)); no animation
            	// _skills.add(SkillTable.getInstance().getInfo(5116,4)); no animation
            	// _skills.add(SkillTable.getInstance().getInfo(5579,4)); no need to add to the cubic skills list
            	break;
            case SMART_CUBIC_ELEMENTALMASTER:
            	_skills.add(SkillTable.getInstance().getInfo(4049,8)); // have animation
            	//_skills.add(SkillTable.getInstance().getInfo(4050,7)); no animation
            	// _skills.add(SkillTable.getInstance().getInfo(4051,7)); no animation
            	// _skills.add(SkillTable.getInstance().getInfo(4052,6)); no animation
            	// _skills.add(SkillTable.getInstance().getInfo(4053,8)); no animation
            	// _skills.add(SkillTable.getInstance().getInfo(4054,8)); no animation
            	// _skills.add(SkillTable.getInstance().getInfo(4055,8)); no animation
            	// _skills.add(SkillTable.getInstance().getInfo(4164,9)); no animation
            	// _skills.add(SkillTable.getInstance().getInfo(4165,9)); no animation
            	_skills.add(SkillTable.getInstance().getInfo(4166,9)); // have animation
            	// _skills.add(SkillTable.getInstance().getInfo(5115,4)); no animation
            	// _skills.add(SkillTable.getInstance().getInfo(5116,4)); no animation
            	// _skills.add(SkillTable.getInstance().getInfo(5579,4)); no need to add to the cubic skills list
            	break;
            case SMART_CUBIC_SPECTRALMASTER:
            	_skills.add(SkillTable.getInstance().getInfo(4049,8)); // have animation
            	// _skills.add(SkillTable.getInstance().getInfo(4050,7)); no animation
            	// _skills.add(SkillTable.getInstance().getInfo(4051,7)); no animation
            	_skills.add(SkillTable.getInstance().getInfo(4052,6)); // have animation
            	// _skills.add(SkillTable.getInstance().getInfo(4053,8)); no animation
            	// _skills.add(SkillTable.getInstance().getInfo(4054,8)); no animation
            	// _skills.add(SkillTable.getInstance().getInfo(4055,8)); no animation
            	// _skills.add(SkillTable.getInstance().getInfo(4164,9)); no animation
            	// _skills.add(SkillTable.getInstance().getInfo(4165,9)); no animation
            	// _skills.add(SkillTable.getInstance().getInfo(4166,9)); no animation
            	// _skills.add(SkillTable.getInstance().getInfo(5115,4)); no animation
            	// _skills.add(SkillTable.getInstance().getInfo(5116,4)); no animation
            	// _skills.add(SkillTable.getInstance().getInfo(5579,4)); no need to add to the cubic skills list
            	break;
            case SMART_CUBIC_EVATEMPLAR:
            	// _skills.add(SkillTable.getInstance().getInfo(4049,8)); no animation
            	// _skills.add(SkillTable.getInstance().getInfo(4050,7)); no animation
            	// _skills.add(SkillTable.getInstance().getInfo(4051,7)); no animation
            	// _skills.add(SkillTable.getInstance().getInfo(4052,6)); no animation
            	_skills.add(SkillTable.getInstance().getInfo(4053,8)); // have animation
            	// _skills.add(SkillTable.getInstance().getInfo(4054,8)); no animation
            	// _skills.add(SkillTable.getInstance().getInfo(4055,8)); no animation
            	// _skills.add(SkillTable.getInstance().getInfo(4164,9)); no animation
            	_skills.add(SkillTable.getInstance().getInfo(4165,9)); // have animation
            	// _skills.add(SkillTable.getInstance().getInfo(4166,9)); no animation
            	// _skills.add(SkillTable.getInstance().getInfo(5115,4)); no animation
            	// _skills.add(SkillTable.getInstance().getInfo(5116,4)); no animation
            	// _skills.add(SkillTable.getInstance().getInfo(5579,4)); no need to add to the cubic skills list
            	break;
            case SMART_CUBIC_SHILLIENTEMPLAR:
            	_skills.add(SkillTable.getInstance().getInfo(4049,8)); // have animation
            	// _skills.add(SkillTable.getInstance().getInfo(4050,7)); no animation
            	// _skills.add(SkillTable.getInstance().getInfo(4051,7)); no animation
            	// _skills.add(SkillTable.getInstance().getInfo(4052,6)); no animation
            	// _skills.add(SkillTable.getInstance().getInfo(4053,8)); no animation
            	// _skills.add(SkillTable.getInstance().getInfo(4054,8)); no animation
            	// _skills.add(SkillTable.getInstance().getInfo(4055,8)); no animation
            	// _skills.add(SkillTable.getInstance().getInfo(4164,9)); no animation
            	// _skills.add(SkillTable.getInstance().getInfo(4165,9)); no animation
            	// _skills.add(SkillTable.getInstance().getInfo(4166,9)); no animation
            	_skills.add(SkillTable.getInstance().getInfo(5115,4)); // have animation
            	// _skills.add(SkillTable.getInstance().getInfo(5116,4)); no animation
            	// _skills.add(SkillTable.getInstance().getInfo(5579,4)); no need to add to the cubic skills list
            	break;
        }
        if (_disappearTask == null)
            _disappearTask = ThreadPoolManager.getInstance().scheduleGeneral(new Disappear(), 1200000); // disappear in 20 mins
    }

    public void doAction()
    {
    	if (_active) return;
        _active = true;
        
        switch (_id)
        {
        	case AQUA_CUBIC:
        	case BINDING_CUBIC:
            case SPARK_CUBIC:
            case STORM_CUBIC:
            case POLTERGEIST_CUBIC:
            case VAMPIRIC_CUBIC:
            case VIPER_CUBIC:
            case ATTRACT_CUBIC:
            case SMART_CUBIC_ARCANALORD:
            case SMART_CUBIC_ELEMENTALMASTER:
            case SMART_CUBIC_SPECTRALMASTER:
            case SMART_CUBIC_EVATEMPLAR:
            case SMART_CUBIC_SHILLIENTEMPLAR:            	
            	_actionTask = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new Action(_activationchance), 0, _activationtime);
            	break;
            case LIFE_CUBIC:
                _actionTask = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new Heal(13,33,53), 0, _activationtime);
                break;
        }
    }

    public int getId()
    {
        return _id;
    }

    public L2PcInstance getOwner(){
    	return _owner;
    }
    
    public final int getMCriticalHit(L2Character target, L2Skill skill){
    	// TODO: Temporary now mcrit for cubics is the baseMCritRate of its owner 
    	return _owner.getTemplate().baseMCritRate; 
    }
    
    public int getMAtk(){ 
    	return _matk; 
    }

    public void stopAction()
    {
        _target = null;
        _active = false;
        if (_actionTask != null)
        {
            _actionTask.cancel(true);
            _actionTask = null;
        }
    }

    public void cancelDisappear()
    {
        if (_disappearTask != null)
        {
            _disappearTask.cancel(true);
            _disappearTask = null;
        }
    }

    /** this sets the enemy target for a cubic */
    public void getCubicTarget()
    {
    	try
        {
    		// TvT event targeting 
    		if (TvTEvent.isStarted() && TvTEvent.isPlayerParticipant(_owner.getObjectId()))
    		{
    			TvTEventTeam enemyTeam = TvTEvent.getParticipantEnemyTeam(_owner.getObjectId());
    			
    			if (_owner.getTarget() instanceof L2PcInstance)
    			{
    				if (enemyTeam.containsPlayer(_owner.getTargetId()) && !((L2PcInstance)_owner.getTarget()).isDead())
    				{
    					_target = (L2Character) _owner.getTarget();
    					return;
    				}
    			}
    			else if (_owner.getTarget() instanceof L2Summon)
    			{
    				if (enemyTeam.containsPlayer(((L2Summon)_owner.getTarget()).getOwner().getObjectId())
    						&& !((L2Summon)_owner.getTarget()).isDead())
    				{
    					_target = (L2Character) _owner.getTarget();
    					return;
    				}
    			} 
    			else if (_owner.getTarget() instanceof L2Trap)
    			{
    				if (enemyTeam.containsPlayer(((L2Trap)_owner.getTarget()).getOwner().getObjectId())
    						&& !((L2Trap)_owner.getTarget()).isDead())
    				{
    					_target = (L2Character) _owner.getTarget();
    					return;
    				}
    			}
    			List<L2Character> potentialTarget = new FastList<L2Character>();
    			
    			for (L2PcInstance enemy : enemyTeam.getParticipatedPlayers().values())
    			{
    				if (isInCubicRange(_owner,enemy) && !enemy.isDead())
    					potentialTarget.add(enemy);
    				if (enemy.getPet() != null)
    					if (isInCubicRange(_owner,enemy.getPet()) && !enemy.getPet().isDead())
    						potentialTarget.add(enemy.getPet());
    				if (enemy.getTrap() != null)
    					if (isInCubicRange(_owner,enemy.getTrap()) && !enemy.getTrap().isDead())
    						potentialTarget.add(enemy.getTrap());
    			}
    			if (potentialTarget.size() == 0)
    			{
    				_target = null;
    				return;
    			}
    			else
    			{
    				int choice = Rnd.nextInt(potentialTarget.size());
       		   		_target = potentialTarget.get(choice);
       		   		return;
    			}
    		}
    		// Duel targeting
    		if (_owner.isInDuel())
    		{
    			L2PcInstance PlayerA = DuelManager.getInstance().getDuel(_owner.getDuelId()).getPlayerA();
    			L2PcInstance PlayerB = DuelManager.getInstance().getDuel(_owner.getDuelId()).getPlayerB();
    			_target = null;
    			
    			if (DuelManager.getInstance().getDuel(_owner.getDuelId()).isPartyDuel())
    			{
    				L2Party partyA = PlayerA.getParty();
    				L2Party partyB = PlayerB.getParty();
    				L2Party partyEnemy = null;
    				
    				if (partyA != null)
    				{
    					if (partyA.getPartyMembers().contains(_owner))
    						if (partyB != null)
    							partyEnemy = partyB;
    						else
    							_target = PlayerB;
    					else
    						partyEnemy = partyA;
    				}
    				else
    				{
    					if (PlayerA == _owner)
    						if (partyB != null)
    							partyEnemy = partyB;
    						else
    							_target = PlayerB;
    					else
    						_target = PlayerA;
    				}
    				if (_target == PlayerA || _target == PlayerB) return;
   					if (partyEnemy != null)
   					{
   						if (partyEnemy.getPartyMembers().contains(_owner.getTarget()))
   							_target = (L2Character) _owner.getTarget();
   						else
   							_target = partyEnemy.getPartyMembers().get(Rnd.get(partyEnemy.getPartyMembers().size()));
   						return;
   					}
    			}
    			if (PlayerA != _owner)
    			{
    				_target = PlayerA;
    				return;
    			}
    			else
    			{
    				_target = PlayerB;
    				return;
    			}
    		}
    		// Olympiad targeting
    		if (_owner.isInOlympiadMode())
    		{
    			_target = null;
    			if (_owner.isOlympiadStart())
    			{
    				L2PcInstance[] players = Olympiad.getInstance().getPlayers(_owner.getOlympiadGameId());
    				if (players != null)
    				{
    					if (_owner.getOlympiadSide() == 1)
    						_target = players[1];
    					else
    						_target = players[0];
    				}
    			}
    			return;
    		}
    		L2PcInstance enemy = null;
    		// test owners target if it is valid then use it
    		if (_owner.getTarget() != null)
    			if (_owner.getTarget() instanceof L2Character && _owner.getTarget() != _owner.getPet() && _owner.getTarget() != _owner)
    			{
    				// target mob which has aggro on you or your summon
    				if (_owner.getTarget() instanceof L2Attackable)
    				{
    					if (((L2Attackable)_owner.getTarget()).getAggroListRP().get(_owner) != null && !((L2Attackable)_owner.getTarget()).isDead())
    					{
    						_target = (L2Character) _owner.getTarget();
    						return;
    					}
    					if (_owner.getPet() != null)
    						if (((L2Attackable)_owner.getTarget()).getAggroListRP().get(_owner.getPet()) != null && !((L2Attackable)_owner.getTarget()).isDead())
    						{
    							_target = (L2Character) _owner.getTarget();
    							return;
    						}
    				}
    			
    				// get target in pvp or in siege
    				enemy = null;

    				if ((_owner.getPvpFlag() > 0 && !_owner.isInsideZone(L2Character.ZONE_PEACE)) || _owner.isInsideZone(L2Character.ZONE_SIEGE) || _owner.isInsideZone(L2Character.ZONE_PVP))
    				{
    					if (_owner.getTarget() instanceof L2Summon)
    					{
    						if (!((L2Summon)_owner.getTarget()).isDead())
    							enemy = ((L2Summon)_owner.getTarget()).getOwner();
    					}
    					else if (_owner.getTarget() instanceof L2Trap)
    					{
    						if (!((L2Trap)_owner.getTarget()).isDead())
    							enemy = ((L2Trap)_owner.getTarget()).getOwner();
    					}
    					else if (_owner.getTarget() instanceof L2PcInstance)
    					{
    						if (!((L2PcInstance)_owner.getTarget()).isDead())
    							enemy = (L2PcInstance) _owner.getTarget();
    					}
    			
    					if (enemy != null)
    					{
    						boolean targetIt = true;
    						
    						if (_owner.getParty() != null)
    						{
    							if (_owner.getParty().getPartyMembers().contains(enemy))
    								targetIt = false;
    							else if (_owner.getParty().getCommandChannel() != null)
    							{
    								if (_owner.getParty().getCommandChannel().getMembers().contains(enemy))
    									targetIt = false;
    							}
    						}
    						if (_owner.getClan() != null && !_owner.isInsideZone(L2Character.ZONE_PVP))
    						{
    							if (_owner.getClan().isMember(enemy.getObjectId()))
    								targetIt = false;
    							if (_owner.getAllyId() > 0 && enemy.getAllyId() > 0)
    							{
    								if (_owner.getAllyId() == enemy.getAllyId())
    									targetIt = false;
    							}
    						}
    						if (enemy.getPvpFlag() == 0 && !enemy.isInsideZone(L2Character.ZONE_SIEGE) && !enemy.isInsideZone(L2Character.ZONE_PVP))
    							targetIt = false;
    						if (enemy.isInsideZone(L2Character.ZONE_PEACE))
    							targetIt = false;
    						if (_owner.getSiegeState() == enemy.getSiegeState() && _owner.getSiegeState() > 0)
    							targetIt = false;

    						if (targetIt)    					
   							{
   								_target = (L2Character) _owner.getTarget();
   								return;
   							}
    					}
    				}
    			}
    		// cubic can not attack owner target, so now we look for a random target 
    		List<L2Character> potentialTarget = new FastList<L2Character>();
    		List<L2Character> potentialPvPTarget = new FastList<L2Character>();
    		Collection<L2Character> knownTarget = _owner.getKnownList().getKnownCharactersInRadius(MAX_MAGIC_RANGE);
   		
   		   	for (L2Character tgMob : knownTarget)
   			{
   		   		// get the mobs which have aggro on the owner or his/her summon
        		if (tgMob instanceof L2Attackable)
				{
        			if (((L2Attackable)tgMob).isDead()) continue;
        			if (((L2Attackable)tgMob).getAggroListRP().get(_owner) != null)
        				potentialTarget.add(tgMob);
        			if (_owner.getPet() != null)
        				if (((L2Attackable)tgMob).getAggroListRP().get(_owner.getPet()) != null)
        					potentialTarget.add(tgMob);
				}
        		// get enemy pvp targets
        		else if ((_owner.getPvpFlag() > 0 && !_owner.isInsideZone(L2Character.ZONE_PEACE)) || _owner.isInsideZone(L2Character.ZONE_SIEGE) || _owner.isInsideZone(L2Character.ZONE_PVP))
				{
        			enemy = null;
					if (tgMob instanceof L2Summon)
					{
						if (!((L2Summon)tgMob).isDead())
							enemy = ((L2Summon)tgMob).getOwner();
					}
					else if (tgMob instanceof L2Trap)
					{
						if (!((L2Trap)tgMob).isDead())
							enemy = ((L2Trap)tgMob).getOwner();
					}
					else if (tgMob instanceof L2PcInstance)
					{
						if (!((L2PcInstance)tgMob).isDead())
							enemy = (L2PcInstance) tgMob;
					}
			
					if (enemy != null)
					{
						boolean targetIt = true;
						if (_owner.getParty() != null)
						{
							if (_owner.getParty().getPartyMembers().contains(enemy))
								targetIt = false;							
							else if (_owner.getParty().getCommandChannel() != null)
							{
								if (_owner.getParty().getCommandChannel().getMembers().contains(enemy))
									targetIt = false;
							}							
						}
						
						if (_owner.getClan() != null && !_owner.isInsideZone(L2Character.ZONE_PVP))
						{
							if (_owner.getClan().isMember(enemy.getObjectId()))
								targetIt = false;
							if (_owner.getAllyId() > 0 && enemy.getAllyId() > 0)
							{
								if (_owner.getAllyId() == enemy.getAllyId())
									targetIt = false;
							}
						}
						if (enemy.getPvpFlag() == 0 && !enemy.isInsideZone(L2Character.ZONE_SIEGE) && !enemy.isInsideZone(L2Character.ZONE_PVP))
							targetIt = false;
						if (enemy.isInsideZone(L2Character.ZONE_PEACE))
							targetIt = false;
						if (_owner.getSiegeState() == enemy.getSiegeState() && _owner.getSiegeState() > 0)
							targetIt = false;

						if (targetIt)    					
							potentialPvPTarget.add(tgMob);
					}
				}
			}
   		   	if (potentialPvPTarget.size() > 0) // PvP target were found, higher priority then PvE target
   		   	{
   		   		// we choose a random PvP target
   		   		int choice = Rnd.nextInt(potentialPvPTarget.size());
   		   		_target = potentialPvPTarget.get(choice);
   		   		return;
   		   	}
   		   	if (potentialTarget.size() == 0) // no PvE target were found
   		   	{
   		   		_target = null;
   		   		return;
   		   	}        
   		   	// we choose a random PvE target
   		   	int choice = Rnd.nextInt(potentialTarget.size());
   		   	_target = potentialTarget.get(choice);
        }
        catch (Exception e)
        {
            _log.log(Level.SEVERE, "", e);
        }
    }

    private class Action implements Runnable
    {
        private int _chance;

        Action(int chance)
        {
            _chance = chance;
            // run task
        }

        public void run()
        {
            try
            {
            	if (_owner.isDead() && _owner.isOnline() == 0)
            	{
            		stopAction();
            		_owner.delCubic(_id);
            		_owner.broadcastUserInfo();
            		cancelDisappear();
            		return;
            	}
            	if (!AttackStanceTaskManager.getInstance().getAttackStanceTask(_owner))
            	{
            		if (_owner.getPet() != null)
            		{
            			if (!AttackStanceTaskManager.getInstance().getAttackStanceTask(_owner.getPet()))
            			{
            				stopAction();
            				return;
            			}
            		}
            		else
            		{
            			stopAction();
            			return;
            		}
            	}
            	// Smart Cubic debuff cancel is 100%
            	boolean UseCubicCure = false;
            	L2Skill skill = null;
        	
            	if (_id >= SMART_CUBIC_EVATEMPLAR && _id <= SMART_CUBIC_SPECTRALMASTER)
            	{
            		L2Effect[] effects = _owner.getAllEffects();
       			
            		for (L2Effect e : effects)
            		{
            			if (e.getSkill().isDebuff())
            			{
            				UseCubicCure = true;
            				e.exit();
            			}
            		}
            	}
            	
            	if (UseCubicCure)
            	{
                	// Smart Cubic debuff cancel is needed, no other skill is used in this activation period
            		MagicSkillUse msu = new MagicSkillUse(_owner, _owner, SKILL_CUBIC_CURE, 1, 0, 0);
            		_owner.broadcastPacket(msu);
            	}
            	else if (Rnd.get(1, 100) < _chance)
            	{
            		skill = _skills.get(Rnd.get(_skills.size()));
            		if (skill != null)
            		{
            			if (skill.getId() == SKILL_CUBIC_HEAL)
            			{
            				// friendly skill, so we look a target in owner's party
            				CubicTargetForHeal();
            			}
            			else
            			{
            				// offensive skill, we look for an enemy target
            				getCubicTarget();
            				if (!isInCubicRange(_owner,_target)) _target = null;
            			}
            			if ((_target != null) && (!_target.isDead()))
            			{
            				if (Config.DEBUG)
            				{
            					_log.info("L2CubicInstance: Action.run();");
            					_log.info("Cubic Id: " + _id + " Target: " + _target.getName() + " distance: " + Math.sqrt(_target.getDistanceSq(_owner.getX(), _owner.getY(), _owner.getZ())));
            				}

            				L2Skill.SkillType type = skill.getSkillType();
            				ISkillHandler handler = SkillHandler.getInstance().getSkillHandler(skill.getSkillType());
            				L2Character[] targets = {_target};

            				if (handler != null)
            				{
            					if((type == L2Skill.SkillType.PARALYZE) || (type == L2Skill.SkillType.STUN))
            					{
            						if (Config.DEBUG)
            							_log.info("L2CubicInstance: Action.run() handler " + type);
            						((Disablers)handler).useCubicSkill(L2CubicInstance.this, skill, targets);
            					}
            					else if(type == L2Skill.SkillType.MDAM){
            						if (Config.DEBUG)
            							_log.info("L2CubicInstance: Action.run() handler " + type);
            						((Mdam)handler).useCubicSkill(L2CubicInstance.this, skill, targets);
            					}
            					else if((type == L2Skill.SkillType.POISON) || (type == L2Skill.SkillType.DEBUFF) || (type == L2Skill.SkillType.DOT) ){
            						if (Config.DEBUG)
            							_log.info("L2CubicInstance: Action.run() handler " + type);
            						((Continuous)handler).useCubicSkill(L2CubicInstance.this, skill, targets);
            					}
            					else{
            						handler.useSkill(_owner, skill, targets);
            						if (Config.DEBUG)
            							_log.info("L2CubicInstance: Action.run(); other handler");
            					}
            				}
            				else
            				{
            					if(type == L2Skill.SkillType.DRAIN){
            						if (Config.DEBUG)
            							_log.info("L2CubicInstance: Action.run() skill " + type);
            						((L2SkillDrain)skill).useCubicSkill(L2CubicInstance.this, targets);
            					}
            					else{
            						skill.useSkill(_owner, targets);
            						if (Config.DEBUG)
            							_log.info("L2CubicInstance: Action.run(); other skill");
            					}
            				}

            				MagicSkillUse msu = new MagicSkillUse(_owner, _target, skill.getId(),
                        											skill.getLevel(), 0, 0);
            				_owner.broadcastPacket(msu);
            			}
            		}
            	}
            }
            catch (Exception e)
            {
            	_log.log(Level.SEVERE, "", e);
            }
        }
    }
    
    /** returns true if the target is inside of the owner's max Cubic range */
    public boolean isInCubicRange(L2Character owner,L2Character target)
    {
    	if (owner == null || target == null) return false;
    	
        int x, y, z;
        // temporary range check until real behavior of cubics is known/coded
        int range = MAX_MAGIC_RANGE; 

        x = (owner.getX() - target.getX());
        y = (owner.getY() - target.getY());
        z = (owner.getZ() - target.getZ());

        return ((x * x) + (y * y) + (z * z) <= (range * range));
    }

    /** this sets the friendly target for a cubic */    
    public void CubicTargetForHeal()
    {
        L2Character target = null;
        double percentleft = 100.0;
        L2Party party = _owner.getParty();
        
        // if owner is in a duel but not in a party duel, then it is the same as he does not have a party   
        if (_owner.isInDuel())
        	if (!DuelManager.getInstance().getDuel(_owner.getDuelId()).isPartyDuel())
        		party = null;
        
        if (party != null && !_owner.isInOlympiadMode())
        {
            // Get all visible objects in a spheric area near the L2Character
            // Get a list of Party Members
            List<L2PcInstance> partyList = party.getPartyMembers();
            for (L2Character partyMember : partyList)
            {
                if (!partyMember.isDead())
                {
                    //if party member not dead, check if he is in castrange of heal cubic
                    if (isInCubicRange(_owner,partyMember))
                    {
                        //member is in cubic casting range, check if he need heal and if he have the lowest HP                               
                        if (partyMember.getCurrentHp() < partyMember.getMaxHp())
                        {
                        	if (percentleft > (partyMember.getCurrentHp() / partyMember.getMaxHp()))
                        	{
                        		percentleft = (partyMember.getCurrentHp() / partyMember.getMaxHp());
                        		target = partyMember;
                        	}
                        }
                    }
                }
                if (partyMember.getPet() != null)
                {
                   	if (partyMember.getPet().isDead()) continue;
                    	
                    //if party member's pet not dead, check if it is in castrange of heal cubic
                    if (!isInCubicRange(_owner,partyMember.getPet())) continue;

                    // member's pet is in cubic casting range, check if he need heal and if he have the lowest HP                               
                    if (partyMember.getPet().getCurrentHp() < partyMember.getPet().getMaxHp())
                    {
                        if (percentleft > (partyMember.getPet().getCurrentHp() / partyMember.getPet().getMaxHp()))
                        {
                            percentleft = (partyMember.getPet().getCurrentHp() / partyMember.getPet().getMaxHp());
                            target = partyMember.getPet();
                        }
                    }
                }
            }
        }
        else
        {
            if (_owner.getCurrentHp() < _owner.getMaxHp())
            {
            	percentleft = (_owner.getCurrentHp() / _owner.getMaxHp());
            	target = _owner;
            }
            if (_owner.getPet() != null)
                if (!_owner.getPet().isDead() && _owner.getPet().getCurrentHp() < _owner.getPet().getMaxHp() 
                		&& percentleft > (_owner.getPet().getCurrentHp() / _owner.getPet().getMaxHp())
                		&& isInCubicRange(_owner,_owner.getPet()))
                {
                	target = _owner.getPet();
                	percentleft = (_owner.getPet().getCurrentHp() / _owner.getPet().getMaxHp());
                }
        }

        _target = target;
    }

    private class Heal implements Runnable
    {
        //private int _chance;
        private int _chanceAb60;
        private int _chance3060;
        private int _chanceBe30;

        Heal(int chance60, int chance3060, int chance30)
        {
            _chanceAb60 = chance60;
            _chance3060 = chance3060;
            _chanceBe30 = chance30;
            // run task
        }

        public void run()
        {
            if (_owner.isDead() && _owner.isOnline() == 0)
            {
                stopAction();
                _owner.delCubic(_id);
                _owner.broadcastUserInfo();
                cancelDisappear();
                return;
            }
            try
            {
            	L2Skill skill = null;
            	for (L2Skill sk : _skills)
            	{
           			if (sk.getId() == SKILL_CUBIC_HEAL)
           			{
           				skill = sk;
           				break;           				
           			}
            	}
                
                if (skill != null)
                {
                    CubicTargetForHeal();
                    L2Character target = _target;
                    if (target != null && !target.isDead())
                    {
                    	double percentleft = (target.getCurrentHp() / target.getMaxHp())* 100.0;
                    
                    	int typeHeal = 1; //1 = 60%+; 2= 30-60%; 3=30%-
                    
                    	if((percentleft <= 60) && (percentleft > 30))
                    		typeHeal = 2;
                    	else if(percentleft <= 30)
                    		typeHeal = 3;
                    
                    	int chance;
                    	switch(typeHeal){
                    		case 1: chance = _chanceAb60;
                    				break;
                    		case 2: chance = _chance3060;
                    				break;
                    		case 3: chance = _chanceBe30;
                    				break;
                    		default:chance = _chanceAb60;
                    	}
                    
                    	if (Rnd.get(1,100) < chance)
                    	{
                   			L2Character[] targets = {target};
                   			ISkillHandler handler = SkillHandler.getInstance().getSkillHandler(skill.getSkillType());
                   			if (handler != null)
                   			{
                   				handler.useSkill(_owner, skill, targets);
                   			}
                   			else
                   			{
                   				skill.useSkill(_owner, targets);
                   			}
                   			
                   			MagicSkillUse msu = new MagicSkillUse(_owner, target, skill.getId(),
                   					skill.getLevel(), 0, 0);
                   			_owner.broadcastPacket(msu);
                   		}
                    }
                }
            }
            catch (Exception e)
            {
            	_log.log(Level.SEVERE, "", e);
            }
        }
    }

    private class Disappear implements Runnable
    {
        Disappear()
        {
            // run task
        }

        public void run()
        {
            stopAction();
            _owner.delCubic(_id);
            _owner.broadcastUserInfo();
        }
    }
}
