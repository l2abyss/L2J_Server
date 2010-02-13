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
package com.l2jserver.gameserver.network.clientpackets;

import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.l2jserver.Config;
import com.l2jserver.gameserver.datatables.CharNameTable;
import com.l2jserver.gameserver.datatables.CharTemplateTable;
import com.l2jserver.gameserver.datatables.SkillTable;
import com.l2jserver.gameserver.datatables.SkillTreeTable;
import com.l2jserver.gameserver.idfactory.IdFactory;
import com.l2jserver.gameserver.instancemanager.QuestManager;
import com.l2jserver.gameserver.model.L2ItemInstance;
import com.l2jserver.gameserver.model.L2ShortCut;
import com.l2jserver.gameserver.model.L2SkillLearn;
import com.l2jserver.gameserver.model.L2World;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.actor.stat.PcStat;
import com.l2jserver.gameserver.model.quest.Quest;
import com.l2jserver.gameserver.model.quest.QuestState;
import com.l2jserver.gameserver.model.quest.State;
import com.l2jserver.gameserver.network.L2GameClient;
import com.l2jserver.gameserver.network.serverpackets.CharCreateFail;
import com.l2jserver.gameserver.network.serverpackets.CharCreateOk;
import com.l2jserver.gameserver.network.serverpackets.CharSelectionInfo;
import com.l2jserver.gameserver.templates.chars.L2PcTemplate;
import com.l2jserver.gameserver.templates.chars.L2PcTemplate.PcTemplateItem;
import com.l2jserver.gameserver.util.Util;


@SuppressWarnings("unused")
public final class CharacterCreate extends L2GameClientPacket
{
	private static final String _C__0B_CHARACTERCREATE = "[C] 0B CharacterCreate";
	private static final Logger _log = Logger.getLogger(CharacterCreate.class.getName());
	protected static final Logger _logAccounting = Logger.getLogger("accounting");
	
	// cSdddddddddddd
	private String _name;
	private int _race;
	private byte _sex;
	private int _classId;
	private int _int;
	private int _str;
	private int _con;
	private int _men;
	private int _dex;
	private int _wit;
	private byte _hairStyle;
	private byte _hairColor;
	private byte _face;
	
	@Override
	protected void readImpl()
	{
		_name = readS();
		_race = readD();
		_sex = (byte) readD();
		_classId = readD();
		_int = readD();
		_str = readD();
		_con = readD();
		_men = readD();
		_dex = readD();
		_wit = readD();
		_hairStyle = (byte) readD();
		_hairColor = (byte) readD();
		_face = (byte) readD();
	}
	
	@Override
	protected void runImpl()
	{
		// Last Verified: May 30, 2009 - Gracia Final - Players are able to create characters with names consisting of as little as 1,2,3 letter/number combinations.
		if ((_name.length() < 1) || (_name.length() > 16))
		{
			if (Config.DEBUG)
				_log.fine("Character Creation Failure: Character name " + _name + " is invalid. Message generated: Your title cannot exceed 16 characters in length. Please try again.");
			
			sendPacket(new CharCreateFail(CharCreateFail.REASON_16_ENG_CHARS));
			return;
		}
		
		// Last Verified: May 30, 2009 - Gracia Final
		if (!Util.isAlphaNumeric(_name) || !isValidName(_name))
		{
			if (Config.DEBUG)
				_log.fine("Character Creation Failure: Character name " + _name + " is invalid. Message generated: Incorrect name. Please try again.");
			
			sendPacket(new CharCreateFail(CharCreateFail.REASON_INCORRECT_NAME));
			return;
		}
		
		if (_face > 2 || _face < 0)
		{
			_log.warning("Character Creation Failure: Character face " + _face + " is invalid. Possible client hack. "+getClient());
			
			sendPacket(new CharCreateFail(CharCreateFail.REASON_CREATION_FAILED));
			return;
		}
		
		if (_hairStyle < 0 || (_sex == 0 && _hairStyle > 4) || (_sex != 0 && _hairStyle > 6))
		{
			_log.warning("Character Creation Failure: Character hair style " + _hairStyle + " is invalid. Possible client hack. "+getClient());
			
			sendPacket(new CharCreateFail(CharCreateFail.REASON_CREATION_FAILED));
			return;
		}
		
		if (_hairColor > 3 || _hairColor < 0)
		{
			_log.warning("Character Creation Failure: Character hair color " + _hairColor + " is invalid. Possible client hack. "+getClient());
			
			sendPacket(new CharCreateFail(CharCreateFail.REASON_CREATION_FAILED));
			return;
		}
		
		L2PcInstance newChar = null;
		L2PcTemplate template = null;
		
		/*
		 * DrHouse: Since checks for duplicate names are done using SQL, lock must be held until data is written to DB as well.
		 */
		synchronized (CharNameTable.getInstance())
		{
			if (CharNameTable.getInstance().accountCharNumber(getClient().getAccountName()) >= Config.MAX_CHARACTERS_NUMBER_PER_ACCOUNT && Config.MAX_CHARACTERS_NUMBER_PER_ACCOUNT != 0)
			{
				if (Config.DEBUG)
					_log.fine("Max number of characters reached. Creation failed.");
				
				sendPacket(new CharCreateFail(CharCreateFail.REASON_TOO_MANY_CHARACTERS));
				return;
			}
			else if (CharNameTable.getInstance().doesCharNameExist(_name))
			{
				if (Config.DEBUG)
					_log.fine("Character Creation Failure: Message generated: You cannot create another character. Please delete the existing character and try again.");
				
				sendPacket(new CharCreateFail(CharCreateFail.REASON_NAME_ALREADY_EXISTS));
				return;
			}
			
			template = CharTemplateTable.getInstance().getTemplate(_classId);
			
			if (template == null || template.classBaseLevel > 1)
			{
				if (Config.DEBUG)
					_log.fine("Character Creation Failure: " + _name + " classId: " + _classId + " Template: " + template + " Message generated: Your character creation has failed.");
				
				sendPacket(new CharCreateFail(CharCreateFail.REASON_CREATION_FAILED));
				return;
			}
			
			int objectId = IdFactory.getInstance().getNextId();
			newChar = L2PcInstance.create(objectId, template, getClient().getAccountName(), _name, _hairStyle, _hairColor, _face, _sex != 0);
		}
		
		newChar.setCurrentHp(template.baseHpMax);
		newChar.setCurrentCp(template.baseCpMax);
		newChar.setCurrentMp(template.baseMpMax);
		// newChar.setMaxLoad(template.baseLoad);
		
		CharCreateOk cco = new CharCreateOk();
		sendPacket(cco);
		
		initNewChar(getClient(), newChar);

    	LogRecord record = new LogRecord(Level.INFO, "Created new character");
    	record.setParameters(new Object[]{newChar, this.getClient()});
		_logAccounting.log(record);
	}
	
	private boolean isValidName(String text)
	{
		boolean result = true;
		String test = text;
		Pattern pattern;
		try
		{
			pattern = Pattern.compile(Config.CNAME_TEMPLATE);
		}
		catch (PatternSyntaxException e) // case of illegal pattern
		{
			_log.warning("ERROR : Character name pattern of config is wrong!");
			pattern = Pattern.compile(".*");
		}
		Matcher regexp = pattern.matcher(test);
		if (!regexp.matches())
		{
			result = false;
		}
		return result;
	}
	
	private void initNewChar(L2GameClient client, L2PcInstance newChar)
	{
		if (Config.DEBUG)
			_log.fine("Character init start");
		
		L2World.getInstance().storeObject(newChar);
		
		L2PcTemplate template = newChar.getTemplate();
		
		newChar.addAdena("Init", Config.STARTING_ADENA, null, false);
		
		newChar.setXYZInvisible(template.spawnX, template.spawnY, template.spawnZ);
		newChar.setTitle("");

		newChar.setVitalityPoints(PcStat.MAX_VITALITY_POINTS, true);

		if (Config.STARTING_LEVEL > 1)
		{
			newChar.getStat().addLevel((byte)(Config.STARTING_LEVEL - 1));
		}
		if (Config.STARTING_SP > 0)
		{
			newChar.getStat().addSp(Config.STARTING_SP);
		}

		L2ShortCut shortcut;
		// add attack shortcut
		shortcut = new L2ShortCut(0, 0, 3, 2, 0, 1);
		newChar.registerShortCut(shortcut);
		// add take shortcut
		shortcut = new L2ShortCut(3, 0, 3, 5, 0, 1);
		newChar.registerShortCut(shortcut);
		// add sit shortcut
		shortcut = new L2ShortCut(10, 0, 3, 0, 0, 1);
		newChar.registerShortCut(shortcut);
		
		for (PcTemplateItem ia : template.getItems())
		{
			L2ItemInstance item = newChar.getInventory().addItem("Init", ia.getItemId(), ia.getAmount(), newChar, null);
			
			// add tutbook shortcut
			if (item.getItemId() == 5588)
			{
				shortcut = new L2ShortCut(11, 0, 1, item.getObjectId(), 0, 1);
				newChar.registerShortCut(shortcut);
			}
			
			if (item.isEquipable() && ia.isEquipped())
			{
				newChar.getInventory().equipItemAndRecord(item);
			}
		}
		
		for (L2SkillLearn skill : SkillTreeTable.getInstance().getAvailableSkills(newChar, newChar.getClassId()))
		{
			newChar.addSkill(SkillTable.getInstance().getInfo(skill.getId(), skill.getLevel()), true);
			if (skill.getId() == 1001 || skill.getId() == 1177)
			{
				shortcut = new L2ShortCut(1, 0, 2, skill.getId(), skill.getLevel(), 1);
				newChar.registerShortCut(shortcut);
			}
			if (skill.getId() == 1216)
			{
				shortcut = new L2ShortCut(10, 0, 2, skill.getId(), skill.getLevel(), 1);
				newChar.registerShortCut(shortcut);
			}
			if (Config.DEBUG)
				_log.fine("Adding starter skill:" + skill.getId() + " / " + skill.getLevel());
		}

		if (!Config.DISABLE_TUTORIAL)
			startTutorialQuest(newChar);

		newChar.logout();
		
		CharSelectionInfo cl = new CharSelectionInfo(client.getAccountName(), client.getSessionId().playOkID1);
		client.getConnection().sendPacket(cl);
		client.setCharSelection(cl.getCharInfo());
		
		if (Config.DEBUG)
			_log.fine("Character init end");
	}
	
	public void startTutorialQuest(L2PcInstance player)
	{
		QuestState qs = player.getQuestState("255_Tutorial");
		Quest q = null;
		if (qs == null)
			q = QuestManager.getInstance().getQuest("255_Tutorial");
		if (q != null)
			q.newQuestState(player).setState(State.STARTED);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.l2jserver.gameserver.clientpackets.ClientBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _C__0B_CHARACTERCREATE;
	}
}
