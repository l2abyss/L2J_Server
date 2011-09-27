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
package com.l2jserver.gameserver.model.actor.stat;

import com.l2jserver.Config;
import com.l2jserver.gameserver.datatables.NpcTable;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.instance.L2ClassMasterInstance;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.actor.instance.L2PetInstance;
import com.l2jserver.gameserver.model.base.Experience;
import com.l2jserver.gameserver.model.entity.RecoBonus;
import com.l2jserver.gameserver.model.quest.QuestState;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.ExBrExtraUserInfo;
import com.l2jserver.gameserver.network.serverpackets.ExVitalityPointInfo;
import com.l2jserver.gameserver.network.serverpackets.ExVoteSystemInfo;
import com.l2jserver.gameserver.network.serverpackets.PledgeShowMemberListUpdate;
import com.l2jserver.gameserver.network.serverpackets.SocialAction;
import com.l2jserver.gameserver.network.serverpackets.StatusUpdate;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;
import com.l2jserver.gameserver.network.serverpackets.UserInfo;
import com.l2jserver.gameserver.skills.Stats;

public class PcStat extends PlayableStat
{
	//private static Logger _log = Logger.getLogger(PcStat.class.getName());
	
	// =========================================================
	// Data Field
	
	private int _oldMaxHp; // stats watch
	private int _oldMaxMp; // stats watch
	private int _oldMaxCp; // stats watch
	private float _vitalityPoints = 1;
	private byte _vitalityLevel = 0;
	
	public static final int VITALITY_LEVELS[] = { 240, 2000, 13000, 17000, 20000 };
	public static final int MAX_VITALITY_POINTS = VITALITY_LEVELS[4];
	public static final int MIN_VITALITY_POINTS = 1;
	
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
		if (!getActiveChar().getAccessLevel().canGainExp())
			return false;
		
		if (!super.addExp(value))
			return false;
		
		// Set new karma
		if (!activeChar.isCursedWeaponEquipped() && activeChar.getKarma() > 0 && (activeChar.isGM() || !activeChar.isInsideZone(L2Character.ZONE_PVP)))
		{
			int karmaLost = activeChar.calculateKarmaLost(value);
			if (karmaLost > 0)
				activeChar.setKarma(activeChar.getKarma() - karmaLost);
		}
		
		// EXP status update currently not used in retail
		activeChar.sendPacket(new UserInfo(activeChar));
		activeChar.sendPacket(new ExBrExtraUserInfo(activeChar));
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
		float ratioTakenByPlayer = 0;
		// Allowed to gain exp/sp?
		L2PcInstance activeChar = getActiveChar();
		if (!activeChar.getAccessLevel().canGainExp())
			return false;
		
		// if this player has a pet that takes from the owner's Exp, give the pet Exp now
		
		if (activeChar.getPet() instanceof L2PetInstance)
		{
			L2PetInstance pet = (L2PetInstance) activeChar.getPet();
			ratioTakenByPlayer = pet.getPetLevelData().getOwnerExpTaken() / 100f;
			
			// only give exp/sp to the pet by taking from the owner if the pet has a non-zero, positive ratio
			// allow possible customizations that would have the pet earning more than 100% of the owner's exp/sp
			if (ratioTakenByPlayer > 1)
				ratioTakenByPlayer = 1;
			if (!pet.isDead())
				pet.addExpAndSp((long) (addToExp * (1 - ratioTakenByPlayer)), (int) (addToSp * (1 - ratioTakenByPlayer)));
			// now adjust the max ratio to avoid the owner earning negative exp/sp
			addToExp = (long) (addToExp * ratioTakenByPlayer);
			addToSp = (int) (addToSp * ratioTakenByPlayer);
		}
		
		if (!super.addExpAndSp(addToExp, addToSp))
			return false;
		
		// Send a Server->Client System Message to the L2PcInstance
		if (addToExp == 0 && addToSp != 0)
		{
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.ACQUIRED_S1_SP);
			sm.addNumber(addToSp);
			activeChar.sendPacket(sm);
		}
		else if (addToSp == 0 && addToExp != 0)
		{
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.EARNED_S1_EXPERIENCE);
			sm.addNumber((int) addToExp);
			activeChar.sendPacket(sm);
		}
		else
		{
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_EARNED_S1_EXP_AND_S2_SP);
			sm.addNumber((int) addToExp);
			sm.addNumber(addToSp);
			activeChar.sendPacket(sm);
		}
		return true;
	}
	
	public boolean addExpAndSp(long addToExp, int addToSp, boolean useBonuses)
	{
		if (useBonuses)
		{
			if (Config.ENABLE_VITALITY)
			{
				switch (_vitalityLevel)
				{
					case 1:
						addToExp *= Config.RATE_VITALITY_LEVEL_1;
						addToSp *= Config.RATE_VITALITY_LEVEL_1;
						break;
					case 2:
						addToExp *= Config.RATE_VITALITY_LEVEL_2;
						addToSp *= Config.RATE_VITALITY_LEVEL_2;
						break;
					case 3:
						addToExp *= Config.RATE_VITALITY_LEVEL_3;
						addToSp *= Config.RATE_VITALITY_LEVEL_3;
						break;
					case 4:
						addToExp *= Config.RATE_VITALITY_LEVEL_4;
						addToSp *= Config.RATE_VITALITY_LEVEL_4;
						break;
				}
			}
			// Apply recommendation bonus
			addToExp *= RecoBonus.getRecoMultiplier(getActiveChar());
			addToSp  *= RecoBonus.getRecoMultiplier(getActiveChar());
		}
		return addExpAndSp(addToExp, addToSp);
	}
	
	@Override
	public boolean removeExpAndSp(long addToExp, int addToSp)
	{
		return removeExpAndSp(addToExp, addToSp, true);
	}
	
	public boolean removeExpAndSp(long addToExp, int addToSp, boolean sendMessage)
	{
		int level = getLevel();
		if (!super.removeExpAndSp(addToExp, addToSp))
			return false;
		
		if (sendMessage)
		{
			// Send a Server->Client System Message to the L2PcInstance
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.EXP_DECREASED_BY_S1);
			sm.addNumber((int) addToExp);
			getActiveChar().sendPacket(sm);
			sm = SystemMessage.getSystemMessage(SystemMessageId.SP_DECREASED_S1);
			sm.addNumber(addToSp);
			getActiveChar().sendPacket(sm);
			if (getLevel()<level)
				getActiveChar().broadcastStatusUpdate();
		}
		return true;
	}
	
	@Override
	public final boolean addLevel(byte value)
	{
		if (getLevel() + value > Experience.MAX_LEVEL - 1)
			return false;
		
		boolean levelIncreased = super.addLevel(value);
		
		if (levelIncreased)
		{
			if (!Config.DISABLE_TUTORIAL)
			{
				QuestState qs = getActiveChar().getQuestState("255_Tutorial");
				if (qs != null)
					qs.getQuest().notifyEvent("CE40", null, getActiveChar());
			}
			
			getActiveChar().setCurrentCp(getMaxCp());
			getActiveChar().broadcastPacket(new SocialAction(getActiveChar(), SocialAction.LEVEL_UP));
			getActiveChar().sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_INCREASED_YOUR_LEVEL));
			
			L2ClassMasterInstance.showQuestionMark(getActiveChar());
		}
		
		//Give AutoGet skills and all normal skills if Auto-Learn is activated.
		getActiveChar().rewardSkills();
		
		if (getActiveChar().getClan() != null)
		{
			getActiveChar().getClan().updateClanMember(getActiveChar());
			getActiveChar().getClan().broadcastToOnlineMembers(new PledgeShowMemberListUpdate(getActiveChar()));
		}
		if (getActiveChar().isInParty())
			getActiveChar().getParty().recalculatePartyLevel(); // Recalculate the party level
		
		if (getActiveChar().isTransformed() || getActiveChar().isInStance())
			getActiveChar().getTransformation().onLevelUp();
		
		StatusUpdate su = new StatusUpdate(getActiveChar());
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
		getActiveChar().sendPacket(new ExBrExtraUserInfo(getActiveChar()));
		getActiveChar().sendPacket(new ExVoteSystemInfo(getActiveChar()));
		
		return levelIncreased;
	}
	
	@Override
	public boolean addSp(int value)
	{
		if (!super.addSp(value))
			return false;
		
		StatusUpdate su = new StatusUpdate(getActiveChar());
		su.addAttribute(StatusUpdate.SP, getSp());
		getActiveChar().sendPacket(su);
		
		return true;
	}
	
	@Override
	public final long getExpForLevel(int level)
	{
		return Experience.LEVEL[level];
	}
	
	// =========================================================
	// Method - Private
	
	// =========================================================
	// Property - Public
	@Override
	public final L2PcInstance getActiveChar()
	{
		return (L2PcInstance) super.getActiveChar();
	}
	
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
			if (getActiveChar().getStatus().getCurrentCp() != val)
				getActiveChar().getStatus().setCurrentCp(getActiveChar().getStatus().getCurrentCp()); // trigger start of regeneration
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
			if (getActiveChar().getStatus().getCurrentHp() != val)
				getActiveChar().getStatus().setCurrentHp(getActiveChar().getStatus().getCurrentHp()); // trigger start of regeneration
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
	public int getRunSpeed()
	{
		if (getActiveChar() == null)
			return 1;
		
		int val;
		
		L2PcInstance player = getActiveChar();
		if (player.isMounted())
		{
			int baseRunSpd = NpcTable.getInstance().getTemplate(getActiveChar().getMountNpcId()).baseRunSpd;
			val = (int) Math.round(calcStat(Stats.RUN_SPEED, baseRunSpd, null, null));
		}
		else
			val = super.getRunSpeed();
		
		val += Config.RUN_SPD_BOOST;
		
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
			return Config.MAX_EVASION;
		
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
	
	@Override
	public float getMovementSpeedMultiplier()
	{
		if (getActiveChar() == null)
			return 1;
		
		if (getActiveChar().isMounted())
			return getRunSpeed() * 1f / NpcTable.getInstance().getTemplate(getActiveChar().getMountNpcId()).baseRunSpd;
		
		return super.getMovementSpeedMultiplier();
	}
	
	@Override
	public int getWalkSpeed()
	{
		if (getActiveChar() == null)
			return 1;
		
		return (getRunSpeed() * 70) / 100;
	}
	
	private void updateVitalityLevel(boolean quiet)
	{
		final byte level;
		
		if (_vitalityPoints <= VITALITY_LEVELS[0])
			level = 0;
		else if (_vitalityPoints <= VITALITY_LEVELS[1])
			level = 1;
		else if (_vitalityPoints <= VITALITY_LEVELS[2])
			level = 2;
		else if (_vitalityPoints <= VITALITY_LEVELS[3])
			level = 3;
		else
			level = 4;
		
		if (!quiet && level != _vitalityLevel)
		{
			if (level < _vitalityLevel)
				getActiveChar().sendPacket(SystemMessage.getSystemMessage(SystemMessageId.VITALITY_HAS_DECREASED));
			else
				getActiveChar().sendPacket(SystemMessage.getSystemMessage(SystemMessageId.VITALITY_HAS_INCREASED));
			if (level == 0)
				getActiveChar().sendPacket(SystemMessage.getSystemMessage(SystemMessageId.VITALITY_IS_EXHAUSTED));
			else if (level == 4)
				getActiveChar().sendPacket(SystemMessage.getSystemMessage(SystemMessageId.VITALITY_IS_AT_MAXIMUM));
		}
		
		_vitalityLevel = level;
	}
	
	/*
	 * Return current vitality points in integer format
	 */
	public int getVitalityPoints()
	{
		return (int) _vitalityPoints;
	}
	
	/*
	 * Set current vitality points to this value
	 * 
	 * if quiet = true - does not send system messages
	 */
	public void setVitalityPoints(int points, boolean quiet)
	{
		points = Math.min(Math.max(points, MIN_VITALITY_POINTS), MAX_VITALITY_POINTS);
		if (points == _vitalityPoints)
			return;
		
		_vitalityPoints = points;
		updateVitalityLevel(quiet);
		getActiveChar().sendPacket(new ExVitalityPointInfo(getVitalityPoints()));
	}
	
	public synchronized void updateVitalityPoints(float points, boolean useRates, boolean quiet)
	{
		if (points == 0 || !Config.ENABLE_VITALITY)
			return;
		
		if (useRates)
		{
			if (getActiveChar().isLucky())
				return;
			
			if (points < 0) // vitality consumed
			{
				int stat = (int) calcStat(Stats.VITALITY_CONSUME_RATE, 1, getActiveChar(), null);
				
				if (stat == 0) // is vitality consumption stopped ?
					return;
				if (stat < 0) // is vitality gained ?
					points = -points;
			}
			
			if (points > 0)
			{
				// vitality increased
				points *= Config.RATE_VITALITY_GAIN;
			}
			else
			{
				// vitality decreased
				points *= Config.RATE_VITALITY_LOST;
			}
		}
		
		if (points > 0)
		{
			points = Math.min(_vitalityPoints + points, MAX_VITALITY_POINTS);
		}
		else
		{
			points = Math.max(_vitalityPoints + points, MIN_VITALITY_POINTS);
		}
		
		if (points == _vitalityPoints)
			return;
		
		_vitalityPoints = points;
		updateVitalityLevel(quiet);
	}
	
	/**
	 * @return the _vitalityLevel
	 */
	public byte getVitalityLevel()
	{
		return _vitalityLevel;
	}
}
