/*
 * Copyright (C) 2004-2015 L2J Server
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
package com.l2jserver.gameserver.network.clientpackets;

import java.util.Base64;

import com.l2jserver.Config;
import com.l2jserver.gameserver.LoginServerThread;
import com.l2jserver.gameserver.SevenSigns;
import com.l2jserver.gameserver.cache.HtmCache;
import com.l2jserver.gameserver.data.sql.impl.AnnouncementsTable;
import com.l2jserver.gameserver.data.xml.impl.AdminData;
import com.l2jserver.gameserver.data.xml.impl.SkillTreesData;
import com.l2jserver.gameserver.instancemanager.CHSiegeManager;
import com.l2jserver.gameserver.instancemanager.CastleManager;
import com.l2jserver.gameserver.instancemanager.ClanHallManager;
import com.l2jserver.gameserver.instancemanager.CoupleManager;
import com.l2jserver.gameserver.instancemanager.CursedWeaponsManager;
import com.l2jserver.gameserver.instancemanager.DimensionalRiftManager;
import com.l2jserver.gameserver.instancemanager.FortManager;
import com.l2jserver.gameserver.instancemanager.FortSiegeManager;
import com.l2jserver.gameserver.instancemanager.InstanceManager;
import com.l2jserver.gameserver.instancemanager.MailManager;
import com.l2jserver.gameserver.instancemanager.PetitionManager;
import com.l2jserver.gameserver.instancemanager.SiegeManager;
import com.l2jserver.gameserver.instancemanager.TerritoryWarManager;
import com.l2jserver.gameserver.model.L2Clan;
import com.l2jserver.gameserver.model.L2Object;
import com.l2jserver.gameserver.model.L2World;
import com.l2jserver.gameserver.model.PcCondOverride;
import com.l2jserver.gameserver.model.TeleportWhereType;
import com.l2jserver.gameserver.model.actor.instance.L2ClassMasterInstance;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.entity.Couple;
import com.l2jserver.gameserver.model.entity.Fort;
import com.l2jserver.gameserver.model.entity.FortSiege;
import com.l2jserver.gameserver.model.entity.L2Event;
import com.l2jserver.gameserver.model.entity.Siege;
import com.l2jserver.gameserver.model.entity.TvTEvent;
import com.l2jserver.gameserver.model.entity.clanhall.AuctionableHall;
import com.l2jserver.gameserver.model.entity.clanhall.SiegableHall;
import com.l2jserver.gameserver.model.items.instance.L2ItemInstance;
import com.l2jserver.gameserver.model.quest.Quest;
import com.l2jserver.gameserver.model.quest.QuestState;
import com.l2jserver.gameserver.model.skills.CommonSkill;
import com.l2jserver.gameserver.model.zone.ZoneId;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.ActionFailed;
import com.l2jserver.gameserver.network.serverpackets.Die;
import com.l2jserver.gameserver.network.serverpackets.EtcStatusUpdate;
import com.l2jserver.gameserver.network.serverpackets.ExBasicActionList;
import com.l2jserver.gameserver.network.serverpackets.ExBrPremiumState;
import com.l2jserver.gameserver.network.serverpackets.ExGetBookMarkInfoPacket;
import com.l2jserver.gameserver.network.serverpackets.ExNevitAdventEffect;
import com.l2jserver.gameserver.network.serverpackets.ExNevitAdventPointInfoPacket;
import com.l2jserver.gameserver.network.serverpackets.ExNevitAdventTimeChange;
import com.l2jserver.gameserver.network.serverpackets.ExNoticePostArrived;
import com.l2jserver.gameserver.network.serverpackets.ExNotifyPremiumItem;
import com.l2jserver.gameserver.network.serverpackets.ExShowContactList;
import com.l2jserver.gameserver.network.serverpackets.ExShowScreenMessage;
import com.l2jserver.gameserver.network.serverpackets.ExStorageMaxCount;
import com.l2jserver.gameserver.network.serverpackets.ExVoteSystemInfo;
import com.l2jserver.gameserver.network.serverpackets.FriendList;
import com.l2jserver.gameserver.network.serverpackets.HennaInfo;
import com.l2jserver.gameserver.network.serverpackets.ItemList;
import com.l2jserver.gameserver.network.serverpackets.NpcHtmlMessage;
import com.l2jserver.gameserver.network.serverpackets.PledgeShowMemberListAll;
import com.l2jserver.gameserver.network.serverpackets.PledgeShowMemberListUpdate;
import com.l2jserver.gameserver.network.serverpackets.PledgeSkillList;
import com.l2jserver.gameserver.network.serverpackets.PledgeStatusChanged;
import com.l2jserver.gameserver.network.serverpackets.QuestList;
import com.l2jserver.gameserver.network.serverpackets.ShortCutInit;
import com.l2jserver.gameserver.network.serverpackets.SkillCoolTime;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;

/**
 * Enter World Packet Handler
 * <p>
 * <p>
 * 0000: 03
 * <p>
 * packet format rev87 bddddbdcccccccccccccccccccc
 * <p>
 */
public class EnterWorld extends L2GameClientPacket {
	private static final String _C__11_ENTERWORLD = "[C] 11 EnterWorld";

	private final int[][] tracert = new int[5][4];

	@Override
	protected void readImpl() {
		readB(new byte[32]); // Unknown Byte Array
		readD(); // Unknown Value
		readD(); // Unknown Value
		readD(); // Unknown Value
		readD(); // Unknown Value
		readB(new byte[32]); // Unknown Byte Array
		readD(); // Unknown Value
		for (int i = 0; i < 5; i++) {
			for (int o = 0; o < 4; o++) {
				tracert[i][o] = readC();
			}
		}
	}

	@Override
	protected void runImpl() {
		final L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null) {
			_log.warning("EnterWorld failed! activeChar returned 'null'.");
			getClient().closeNow();
			return;
		}

		String[] adress = new String[5];
		for (int i = 0; i < 5; i++) {
			adress[i] = tracert[i][0] + "." + tracert[i][1] + "."
					+ tracert[i][2] + "." + tracert[i][3];
		}

		LoginServerThread.getInstance().sendClientTracert(
				activeChar.getAccountName(), adress);

		getClient().setClientTracert(tracert);

		// Restore to instanced area if enabled
		if (Config.RESTORE_PLAYER_INSTANCE) {
			activeChar.setInstanceId(InstanceManager.getInstance()
					.getPlayerInstance(activeChar.getObjectId()));
		} else {
			int instanceId = InstanceManager.getInstance().getPlayerInstance(
					activeChar.getObjectId());
			if (instanceId > 0) {
				InstanceManager.getInstance().getInstance(instanceId)
						.removePlayer(activeChar.getObjectId());
			}
		}

		if (L2World.getInstance().findObject(activeChar.getObjectId()) != null) {
			if (Config.DEBUG) {
				_log.warning("User already exists in Object ID map! User "
						+ activeChar.getName() + " is a character clone.");
			}
		}

		// Apply special GM properties to the GM when entering
		if (activeChar.isGM()) {
			if (Config.GM_STARTUP_INVULNERABLE
					&& AdminData.getInstance().hasAccess("admin_invul",
							activeChar.getAccessLevel())) {
				activeChar.setIsInvul(true);
			}

			if (Config.GM_STARTUP_INVISIBLE
					&& AdminData.getInstance().hasAccess("admin_invisible",
							activeChar.getAccessLevel())) {
				activeChar.setInvisible(true);
			}

			if (Config.GM_STARTUP_SILENCE
					&& AdminData.getInstance().hasAccess("admin_silence",
							activeChar.getAccessLevel())) {
				activeChar.setSilenceMode(true);
			}

			if (Config.GM_STARTUP_DIET_MODE
					&& AdminData.getInstance().hasAccess("admin_diet",
							activeChar.getAccessLevel())) {
				activeChar.setDietMode(true);
				activeChar.refreshOverloaded();
			}

			if (Config.GM_STARTUP_AUTO_LIST
					&& AdminData.getInstance().hasAccess("admin_gmliston",
							activeChar.getAccessLevel())) {
				AdminData.getInstance().addGm(activeChar, false);
			} else {
				AdminData.getInstance().addGm(activeChar, true);
			}

			if (Config.GM_GIVE_SPECIAL_SKILLS) {
				SkillTreesData.getInstance().addSkills(activeChar, false);
			}

			if (Config.GM_GIVE_SPECIAL_AURA_SKILLS) {
				SkillTreesData.getInstance().addSkills(activeChar, true);
			}
		}

		// Set dead status if applies
		if (activeChar.getCurrentHp() < 0.5) {
			activeChar.setIsDead(true);
		}

		boolean showClanNotice = false;

		// Clan related checks are here
		if (activeChar.getClan() != null) {
			activeChar.sendPacket(new PledgeSkillList(activeChar.getClan()));

			notifyClanMembers(activeChar);

			notifySponsorOrApprentice(activeChar);

			AuctionableHall clanHall = ClanHallManager.getInstance()
					.getClanHallByOwner(activeChar.getClan());

			if (clanHall != null) {
				if (!clanHall.getPaid()) {
					activeChar
							.sendPacket(SystemMessageId.PAYMENT_FOR_YOUR_CLAN_HALL_HAS_NOT_BEEN_MADE_PLEASE_MAKE_PAYMENT_TO_YOUR_CLAN_WAREHOUSE_BY_S1_TOMORROW);
				}
			}

			for (Siege siege : SiegeManager.getInstance().getSieges()) {
				if (!siege.isInProgress()) {
					continue;
				}

				if (siege.checkIsAttacker(activeChar.getClan())) {
					activeChar.setSiegeState((byte) 1);
					activeChar.setSiegeSide(siege.getCastle().getResidenceId());
				}

				else if (siege.checkIsDefender(activeChar.getClan())) {
					activeChar.setSiegeState((byte) 2);
					activeChar.setSiegeSide(siege.getCastle().getResidenceId());
				}
			}

			for (FortSiege siege : FortSiegeManager.getInstance().getSieges()) {
				if (!siege.isInProgress()) {
					continue;
				}

				if (siege.checkIsAttacker(activeChar.getClan())) {
					activeChar.setSiegeState((byte) 1);
					activeChar.setSiegeSide(siege.getFort().getResidenceId());
				}

				else if (siege.checkIsDefender(activeChar.getClan())) {
					activeChar.setSiegeState((byte) 2);
					activeChar.setSiegeSide(siege.getFort().getResidenceId());
				}
			}

			for (SiegableHall hall : CHSiegeManager.getInstance()
					.getConquerableHalls().values()) {
				if (!hall.isInSiege()) {
					continue;
				}

				if (hall.isRegistered(activeChar.getClan())) {
					activeChar.setSiegeState((byte) 1);
					activeChar.setSiegeSide(hall.getId());
					activeChar.setIsInHideoutSiege(true);
				}
			}

			sendPacket(new PledgeShowMemberListAll(activeChar.getClan(),
					activeChar));
			sendPacket(new PledgeStatusChanged(activeChar.getClan()));

			// Residential skills support
			if (activeChar.getClan().getCastleId() > 0) {
				CastleManager.getInstance()
						.getCastleByOwner(activeChar.getClan())
						.giveResidentialSkills(activeChar);
			}

			if (activeChar.getClan().getFortId() > 0) {
				FortManager.getInstance().getFortByOwner(activeChar.getClan())
						.giveResidentialSkills(activeChar);
			}

			showClanNotice = activeChar.getClan().isNoticeEnabled();
		}

		if (TerritoryWarManager.getInstance().getRegisteredTerritoryId(
				activeChar) > 0) {
			if (TerritoryWarManager.getInstance().isTWInProgress()) {
				activeChar.setSiegeState((byte) 1);
			}
			activeChar.setSiegeSide(TerritoryWarManager.getInstance()
					.getRegisteredTerritoryId(activeChar));
		}

		// Updating Seal of Strife Buff/Debuff
		if (SevenSigns.getInstance().isSealValidationPeriod()
				&& (SevenSigns.getInstance().getSealOwner(
						SevenSigns.SEAL_STRIFE) != SevenSigns.CABAL_NULL)) {
			int cabal = SevenSigns.getInstance().getPlayerCabal(
					activeChar.getObjectId());
			if (cabal != SevenSigns.CABAL_NULL) {
				if (cabal == SevenSigns.getInstance().getSealOwner(
						SevenSigns.SEAL_STRIFE)) {
					activeChar.addSkill(CommonSkill.THE_VICTOR_OF_WAR
							.getSkill());
				} else {
					activeChar.addSkill(CommonSkill.THE_VANQUISHED_OF_WAR
							.getSkill());
				}
			}
		} else {
			activeChar.removeSkill(CommonSkill.THE_VICTOR_OF_WAR.getSkill());
			activeChar
					.removeSkill(CommonSkill.THE_VANQUISHED_OF_WAR.getSkill());
		}

		if (Config.ENABLE_VITALITY && Config.RECOVER_VITALITY_ON_RECONNECT) {
			float points = (Config.RATE_RECOVERY_ON_RECONNECT * (System
					.currentTimeMillis() - activeChar.getLastAccess())) / 60000;
			if (points > 0) {
				activeChar.updateVitalityPoints(points, false, true);
			}
		}

		activeChar.checkRecoBonusTask();
		activeChar.checkHuntingBonusTask();
		activeChar.checkVipTask();

		activeChar.broadcastUserInfo();

		// Send Macro List
		activeChar.getMacros().sendUpdate();

		// Send Item List
		sendPacket(new ItemList(activeChar, false));

		// Send GG check
		activeChar.queryGameGuard();

		// Send Teleport Bookmark List
		sendPacket(new ExGetBookMarkInfoPacket(activeChar));

		// Send Shortcuts
		sendPacket(new ShortCutInit(activeChar));

		// Send Action list
		activeChar.sendPacket(ExBasicActionList.STATIC_PACKET);

		// Send Skill list
		activeChar.sendSkillList();

		// Send Dye Information
		activeChar.sendPacket(new HennaInfo(activeChar));

		Quest.playerEnter(activeChar);

		if (!Config.DISABLE_TUTORIAL) {
			loadTutorial(activeChar);
		}

		activeChar.sendPacket(new QuestList());

		if (Config.PLAYER_SPAWN_PROTECTION > 0) {
			activeChar.setProtection(true);
		}

		activeChar.spawnMe(activeChar.getX(), activeChar.getY(),
				activeChar.getZ());

		activeChar.getInventory().applyItemSkills();

		if (L2Event.isParticipant(activeChar)) {
			L2Event.restorePlayerEventStatus(activeChar);
		}

		// Wedding Checks
		if (Config.L2JMOD_ALLOW_WEDDING) {
			engage(activeChar);
			notifyPartner(activeChar, activeChar.getPartnerId());
		}

		if (activeChar.isCursedWeaponEquipped()) {
			CursedWeaponsManager.getInstance()
					.getCursedWeapon(activeChar.getCursedWeaponEquippedId())
					.cursedOnLogin();
		}

		activeChar.updateEffectIcons();

		activeChar.sendPacket(new EtcStatusUpdate(activeChar));

		// Expand Skill
		activeChar.sendPacket(new ExStorageMaxCount(activeChar));

		sendPacket(new FriendList(activeChar));

		SystemMessage sm = SystemMessage
				.getSystemMessage(SystemMessageId.FRIEND_S1_HAS_LOGGED_IN);
		sm.addString(activeChar.getName());
		for (int id : activeChar.getFriendList()) {
			L2Object obj = L2World.getInstance().findObject(id);
			if (obj != null) {
				obj.sendPacket(sm);
			}
		}

		activeChar.sendPacket(SystemMessageId.WELCOME_TO_LINEAGE);

		/**
		 * L2Abyss hide l2j sys msg This is the system message (L2J credits)
		 * sent to the player that has just logged in
		 */

		// activeChar.sendMessage(getText("VGhpcyBTZXJ2ZXIgdXNlcyBMMkosIGEgUHJvamVjdCBmb3VuZGVkIGJ5IEwyQ2hlZg=="));
		// activeChar.sendMessage(getText("YW5kIGRldmVsb3BlZCBieSBMMkogVGVhbSBhdCB3d3cubDJqc2VydmVyLmNvbQ=="));
		// activeChar.sendMessage(getText("Q29weXJpZ2h0IDIwMDQtMjAxNQ=="));
		// activeChar.sendMessage(getText("VGhhbmsgeW91IGZvciAxMSB5ZWFycyE="));

		SevenSigns.getInstance().sendCurrentPeriodMsg(activeChar);
		AnnouncementsTable.getInstance().showAnnouncements(activeChar);

		if (showClanNotice) {
			final NpcHtmlMessage notice = new NpcHtmlMessage();
			notice.setFile(activeChar.getHtmlPrefix(),
					"data/html/clanNotice.htm");
			notice.replace("%clan_name%", activeChar.getClan().getName());
			notice.replace("%notice_text%", activeChar.getClan().getNotice());
			notice.disableValidation();
			sendPacket(notice);
		} else if (Config.SERVER_NEWS) {
			// only for new characters
			if (activeChar.getLevel() == 1) {
				String serverNews = HtmCache.getInstance().getHtm(
						activeChar.getHtmlPrefix(), "data/html/servnews.htm");
				if (serverNews != null) {
					sendPacket(new NpcHtmlMessage(serverNews));
				}
			}
		}

		if (Config.PETITIONING_ALLOWED) {
			PetitionManager.getInstance().checkPetitionMessages(activeChar);
		}

		if (activeChar.isAlikeDead()) // dead or fake dead
		{
			// no broadcast needed since the player will already spawn dead to
			// others
			sendPacket(new Die(activeChar));
		}

		activeChar.onPlayerEnter();

		sendPacket(new SkillCoolTime(activeChar));
		
		// Premium status
		sendPacket(new ExBrPremiumState(activeChar.getObjectId(), activeChar.isVip() ? 1 : 0));
		
		// Nevit system
		sendPacket(new ExVoteSystemInfo(activeChar));
		// Hunting bonus
		sendPacket(new ExNevitAdventPointInfoPacket(
				activeChar.getHuntingBonusPoints()));
		sendPacket(new ExNevitAdventEffect(activeChar.getHuntingBonusTime()));
		sendPacket(new ExNevitAdventTimeChange(-1)); // set paused
		
		sendPacket(new ExShowContactList(activeChar));

		for (L2ItemInstance i : activeChar.getInventory().getItems()) {
			if (i.isTimeLimitedItem()) {
				i.scheduleLifeTimeTask();
			}
			if (i.isShadowItem() && i.isEquipped()) {
				i.decreaseMana(false);
			}
		}

		for (L2ItemInstance i : activeChar.getWarehouse().getItems()) {
			if (i.isTimeLimitedItem()) {
				i.scheduleLifeTimeTask();
			}
		}

		if (DimensionalRiftManager.getInstance().checkIfInRiftZone(
				activeChar.getX(), activeChar.getY(), activeChar.getZ(), false)) {
			DimensionalRiftManager.getInstance().teleportToWaitingRoom(
					activeChar);
		}

		if (activeChar.getClanJoinExpiryTime() > System.currentTimeMillis()) {
			activeChar.sendPacket(SystemMessageId.CLAN_MEMBERSHIP_TERMINATED);
		}

		// remove combat flag before teleporting
		if (activeChar.getInventory().getItemByItemId(9819) != null) {
			Fort fort = FortManager.getInstance().getFort(activeChar);

			if (fort != null) {
				FortSiegeManager.getInstance().dropCombatFlag(activeChar,
						fort.getResidenceId());
			} else {
				int slot = activeChar.getInventory().getSlotFromItem(
						activeChar.getInventory().getItemByItemId(9819));
				activeChar.getInventory().unEquipItemInBodySlot(slot);
				activeChar.destroyItem("CombatFlag", activeChar.getInventory()
						.getItemByItemId(9819), null, true);
			}
		}

		// Attacker or spectator logging in to a siege zone.
		// Actually should be checked for inside castle only?
		if (!activeChar.canOverrideCond(PcCondOverride.ZONE_CONDITIONS)
				&& activeChar.isInsideZone(ZoneId.SIEGE)
				&& (!activeChar.isInSiege() || (activeChar.getSiegeState() < 2))) {
			activeChar.teleToLocation(TeleportWhereType.TOWN);
		}

		if (Config.ALLOW_MAIL) {
			if (MailManager.getInstance().hasUnreadPost(activeChar)) {
				sendPacket(ExNoticePostArrived.valueOf(false));
			}
		}

		TvTEvent.onLogin(activeChar);

		if (Config.WELCOME_MESSAGE_ENABLED) {
			activeChar.sendPacket(new ExShowScreenMessage(
					Config.WELCOME_MESSAGE_TEXT, Config.WELCOME_MESSAGE_TIME));
		}
		
		if (Config.VOTE_REWARD_SYSTEM_ACTIVE && activeChar.hasReward()) {
			activeChar.reward();
		}

		L2ClassMasterInstance.showQuestionMark(activeChar);

		int birthday = activeChar.checkBirthDay();
		if (birthday == 0) {
			activeChar
					.sendPacket(SystemMessageId.YOUR_BIRTHDAY_GIFT_HAS_ARRIVED);
			// activeChar.sendPacket(new ExBirthdayPopup()); Removed in H5?
		} else if (birthday != -1) {
			sm = SystemMessage
					.getSystemMessage(SystemMessageId.THERE_ARE_S1_DAYS_UNTIL_YOUR_CHARACTERS_BIRTHDAY);
			sm.addString(Integer.toString(birthday));
			activeChar.sendPacket(sm);
		}

		if (!activeChar.getPremiumItemList().isEmpty()) {
			activeChar.sendPacket(ExNotifyPremiumItem.STATIC_PACKET);
		}

		// Unstuck players that had client open when server crashed.
		activeChar.sendPacket(ActionFailed.STATIC_PACKET);
	}

	private void engage(L2PcInstance cha) {
		int chaId = cha.getObjectId();

		for (Couple cl : CoupleManager.getInstance().getCouples()) {
			if ((cl.getPlayer1Id() == chaId) || (cl.getPlayer2Id() == chaId)) {
				if (cl.getMaried()) {
					cha.setMarried(true);
				}

				cha.setCoupleId(cl.getId());

				if (cl.getPlayer1Id() == chaId) {
					cha.setPartnerId(cl.getPlayer2Id());
				} else {
					cha.setPartnerId(cl.getPlayer1Id());
				}
			}
		}
	}

	/**
	 * @param cha
	 * @param partnerId
	 */
	private void notifyPartner(L2PcInstance cha, int partnerId) {
		int objId = cha.getPartnerId();
		if (objId != 0) {
			final L2PcInstance partner = L2World.getInstance().getPlayer(objId);
			if (partner != null) {
				partner.sendMessage("Your Partner has logged in.");
			}
		}
	}

	/**
	 * @param activeChar
	 */
	private void notifyClanMembers(L2PcInstance activeChar) {
		final L2Clan clan = activeChar.getClan();
		if (clan != null) {
			clan.getClanMember(activeChar.getObjectId()).setPlayerInstance(
					activeChar);

			final SystemMessage msg = SystemMessage
					.getSystemMessage(SystemMessageId.CLAN_MEMBER_S1_LOGGED_IN);
			msg.addString(activeChar.getName());
			clan.broadcastToOtherOnlineMembers(msg, activeChar);
			clan.broadcastToOtherOnlineMembers(new PledgeShowMemberListUpdate(
					activeChar), activeChar);
		}
	}

	/**
	 * @param activeChar
	 */
	private void notifySponsorOrApprentice(L2PcInstance activeChar) {
		if (activeChar.getSponsor() != 0) {
			final L2PcInstance sponsor = L2World.getInstance().getPlayer(
					activeChar.getSponsor());
			if (sponsor != null) {
				final SystemMessage msg = SystemMessage
						.getSystemMessage(SystemMessageId.YOUR_APPRENTICE_S1_HAS_LOGGED_IN);
				msg.addString(activeChar.getName());
				sponsor.sendPacket(msg);
			}
		} else if (activeChar.getApprentice() != 0) {
			final L2PcInstance apprentice = L2World.getInstance().getPlayer(
					activeChar.getApprentice());
			if (apprentice != null) {
				final SystemMessage msg = SystemMessage
						.getSystemMessage(SystemMessageId.YOUR_SPONSOR_C1_HAS_LOGGED_IN);
				msg.addString(activeChar.getName());
				apprentice.sendPacket(msg);
			}
		}
	}

	/**
	 * @param string
	 * @return
	 */
	@SuppressWarnings("unused")
	private String getText(String string) {
		return new String(Base64.getDecoder().decode(string));
	}

	private void loadTutorial(L2PcInstance player) {
		final QuestState qs = player.getQuestState("255_Tutorial");
		if (qs != null) {
			qs.getQuest().notifyEvent("UC", null, player);
		}
	}

	@Override
	public String getType() {
		return _C__11_ENTERWORLD;
	}

	@Override
	protected boolean triggersOnActionRequest() {
		return false;
	}
}
