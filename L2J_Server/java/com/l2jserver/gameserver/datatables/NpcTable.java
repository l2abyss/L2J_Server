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
package com.l2jserver.gameserver.datatables;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2jserver.Config;
import com.l2jserver.L2DatabaseFactory;
import com.l2jserver.gameserver.model.Elementals;
import com.l2jserver.gameserver.model.L2DropData;
import com.l2jserver.gameserver.model.L2MinionData;
import com.l2jserver.gameserver.model.L2NpcAIData;
import com.l2jserver.gameserver.model.StatsSet;
import com.l2jserver.gameserver.model.actor.templates.L2NpcTemplate;
import com.l2jserver.gameserver.model.base.ClassId;
import com.l2jserver.gameserver.model.quest.Quest;
import com.l2jserver.gameserver.model.quest.Quest.QuestEventType;
import com.l2jserver.gameserver.model.skills.L2Skill;
import com.l2jserver.gameserver.model.stats.BaseStats;

public class NpcTable
{
	private static final Logger _log = Logger.getLogger(NpcTable.class.getName());
	
	private final TIntObjectHashMap<L2NpcTemplate> _npcs = new TIntObjectHashMap<>();
	
	protected NpcTable()
	{
		_npcs.clear();
		restoreNpcData();
	}
	
	private void restoreNpcData()
	{
		loadNpcs(0);
		loadNpcsSkills(0);
		loadNpcsDrop(0);
		loadNpcsSkillLearn(0);
		loadMinions(0);
		loadNpcsAI(0);
		loadNpcsElement(0);
	}
	
	/**
	 * @param NpcData
	 * @throws Exception
	 */
	private void fillNpcTable(ResultSet NpcData) throws Exception
	{
		StatsSet npcDat = new StatsSet();
		int id = NpcData.getInt("id");
		int idTemp = NpcData.getInt("idTemplate");
		
		assert idTemp < 1000000;
		
		npcDat.set("npcId", id);
		npcDat.set("idTemplate", idTemp);
		int level = NpcData.getInt("level");
		npcDat.set("level", level);
		npcDat.set("client_class", NpcData.getString("class"));
		
		npcDat.set("baseShldDef", 0);
		npcDat.set("baseShldRate", 0);
		npcDat.set("baseCritRate", NpcData.getInt("critical"));
		
		npcDat.set("name", NpcData.getString("name"));
		npcDat.set("serverSideName", NpcData.getBoolean("serverSideName"));
		npcDat.set("title", NpcData.getString("title"));
		npcDat.set("serverSideTitle", NpcData.getBoolean("serverSideTitle"));
		npcDat.set("collision_radius", NpcData.getDouble("collision_radius"));
		npcDat.set("collision_height", NpcData.getDouble("collision_height"));
		npcDat.set("sex", NpcData.getString("sex"));
		npcDat.set("type", NpcData.getString("type"));
		npcDat.set("baseAtkRange", NpcData.getInt("attackrange"));
		npcDat.set("rewardExp", NpcData.getInt("exp"));
		npcDat.set("rewardSp", NpcData.getInt("sp"));
		npcDat.set("basePAtkSpd", NpcData.getInt("atkspd"));
		npcDat.set("baseMAtkSpd", NpcData.getInt("matkspd"));
		npcDat.set("rhand", NpcData.getInt("rhand"));
		npcDat.set("lhand", NpcData.getInt("lhand"));
		npcDat.set("enchant", NpcData.getInt("enchant"));
		npcDat.set("baseWalkSpd", NpcData.getInt("walkspd"));
		npcDat.set("baseRunSpd", NpcData.getInt("runspd"));
		
		// constants, until we have stats in DB
		npcDat.safeSet("baseSTR", NpcData.getInt("str"), 0, BaseStats.MAX_STAT_VALUE, "Loading npc template id: " + NpcData.getInt("idTemplate"));
		npcDat.safeSet("baseCON", NpcData.getInt("con"), 0, BaseStats.MAX_STAT_VALUE, "Loading npc template id: " + NpcData.getInt("idTemplate"));
		npcDat.safeSet("baseDEX", NpcData.getInt("dex"), 0, BaseStats.MAX_STAT_VALUE, "Loading npc template id: " + NpcData.getInt("idTemplate"));
		npcDat.safeSet("baseINT", NpcData.getInt("int"), 0, BaseStats.MAX_STAT_VALUE, "Loading npc template id: " + NpcData.getInt("idTemplate"));
		npcDat.safeSet("baseWIT", NpcData.getInt("wit"), 0, BaseStats.MAX_STAT_VALUE, "Loading npc template id: " + NpcData.getInt("idTemplate"));
		npcDat.safeSet("baseMEN", NpcData.getInt("men"), 0, BaseStats.MAX_STAT_VALUE, "Loading npc template id: " + NpcData.getInt("idTemplate"));
		
		npcDat.set("baseHpMax", NpcData.getDouble("hp"));
		npcDat.set("baseCpMax", 0);
		npcDat.set("baseMpMax", NpcData.getDouble("mp"));
		npcDat.set("baseHpReg", NpcData.getFloat("hpreg") > 0 ? NpcData.getFloat("hpreg") : 1.5 + ((level - 1) / 10.0));
		npcDat.set("baseMpReg", NpcData.getFloat("mpreg") > 0 ? NpcData.getFloat("mpreg") : 0.9 + (0.3 * ((level - 1) / 10.0)));
		npcDat.set("basePAtk", NpcData.getInt("patk"));
		npcDat.set("basePDef", NpcData.getInt("pdef"));
		npcDat.set("baseMAtk", NpcData.getInt("matk"));
		npcDat.set("baseMDef", NpcData.getInt("mdef"));
		
		npcDat.set("dropHerbGroup", NpcData.getInt("dropHerbGroup"));
		
		// Default element resists
		npcDat.set("baseFireRes", 20);
		npcDat.set("baseWindRes", 20);
		npcDat.set("baseWaterRes", 20);
		npcDat.set("baseEarthRes", 20);
		npcDat.set("baseHolyRes", 20);
		npcDat.set("baseDarkRes", 20);
		
		_npcs.put(id, new L2NpcTemplate(npcDat));
	}
	
	/**
	 * @param id of the NPC to reload.
	 */
	public void reloadNpc(int id)
	{
		Map<QuestEventType, List<Quest>> quests = null;
		try
		{
			// save a copy of the old data
			L2NpcTemplate old = getTemplate(id);
			
			if (old != null)
			{
				quests = old.getEventQuests();
			}
			
			loadNpcs(id);
			loadNpcsSkills(id);
			loadNpcsDrop(id);
			loadNpcsSkillLearn(id);
			loadMinions(id);
			loadNpcsAI(id);
			loadNpcsElement(id);
			
			// restore additional data from saved copy
			L2NpcTemplate created = getTemplate(id);
			
			if ((old != null) && (created != null))
			{
				created.getEventQuests().putAll(quests);
			}
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, getClass().getSimpleName() + ": Could not reload data for NPC " + id + ": " + e.getMessage(), e);
		}
	}
	
	/**
	 * Just wrapper.
	 */
	public void reloadAllNpc()
	{
		restoreNpcData();
	}
	
	/**
	 * @param npc
	 */
	public void saveNpc(StatsSet npc)
	{
		final Map<String, Object> set = npc.getSet();
		int length = 0;
		for (Object obj : set.keySet())
		{
			// 15 is just guessed npc name length
			length += ((String) obj).length() + 7 + 15;
		}
		
		final StringBuilder npcSb = new StringBuilder(length);
		final StringBuilder npcAiSb = new StringBuilder(30);
		String attribute;
		String value;
		for (Entry<String, Object> entry : set.entrySet())
		{
			attribute = entry.getKey();
			value = String.valueOf(entry.getValue());
			switch (attribute)
			{
				case "npcId":
					break;
				case "aggro":
				case "showName":
				case "targetable":
				{
					appendEntry(npcAiSb, attribute, value);
					break;
				}
				default:
				{
					appendEntry(npcSb, attribute, value);
				}
			}
		}
		
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			int updated = 0;
			final int npcId = npc.getInteger("npcId");
			if (Config.CUSTOM_NPC_TABLE)
			{
				updated = performUpdate(npcSb, "custom_npc", "id", npcId, con);
				performUpdate(npcAiSb, "custom_npcaidata", "npcId", npcId, con);
			}
			
			if (updated == 0)
			{
				performUpdate(npcSb, "npc", "id", npcId, con);
				performUpdate(npcAiSb, "npcaidata", "npcId", npcId, con);
			}
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, getClass().getSimpleName() + ": Could not store new NPC data in database: " + e.getMessage(), e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
	}
	
	/**
	 * @param sb the string builder to append the attribute and value.
	 * @param attribute the attribute to append.
	 * @param value the value to append.
	 */
	private final void appendEntry(StringBuilder sb, String attribute, String value)
	{
		if (sb.length() > 0)
		{
			sb.append(", ");
		}
		
		sb.append(attribute);
		sb.append(" = '");
		sb.append(value);
		sb.append('\'');
	}
	
	/**
	 * @param sb the string builder with the parameters
	 * @param table the table to update.
	 * @param key the key of the table.
	 * @param npcId the Npc Id.
	 * @param con the current database connection.
	 * @return the count of updated NPCs.
	 * @throws SQLException the SQL exception.
	 */
	private final int performUpdate(StringBuilder sb, String table, String key, int npcId, Connection con) throws SQLException
	{
		int updated = 0;
		if ((sb != null) && !sb.toString().isEmpty())
		{
			final StringBuilder sbQuery = new StringBuilder(sb.length() + 28);
			sbQuery.append("UPDATE ");
			sbQuery.append(table);
			sbQuery.append(" SET ");
			sbQuery.append(sb.toString());
			sbQuery.append(" WHERE ");
			sbQuery.append(key);
			sbQuery.append(" = ?");
			final PreparedStatement statement = con.prepareStatement(sbQuery.toString());
			statement.setInt(1, npcId);
			updated = statement.executeUpdate();
			statement.close();
		}
		return updated;
	}
	
	/**
	 * @param id the template Id to get.
	 * @return the template for the given id.
	 */
	public L2NpcTemplate getTemplate(int id)
	{
		return _npcs.get(id);
	}
	
	/**
	 * @param name of the template to get.
	 * @return the template for the given name.
	 */
	public L2NpcTemplate getTemplateByName(String name)
	{
		for (L2NpcTemplate npcTemplate : _npcs.values(new L2NpcTemplate[0]))
		{
			if (npcTemplate.getName().equalsIgnoreCase(name))
			{
				return npcTemplate;
			}
		}
		return null;
	}
	
	/**
	 * @param lvls of all the templates to get.
	 * @return the template list for the given level.
	 */
	public List<L2NpcTemplate> getAllOfLevel(int... lvls)
	{
		final List<L2NpcTemplate> list = new ArrayList<>();
		for (int lvl : lvls)
		{
			for (L2NpcTemplate t : _npcs.values(new L2NpcTemplate[0]))
			{
				if (t.getLevel() == lvl)
				{
					list.add(t);
				}
			}
		}
		return list;
	}
	
	/**
	 * @param lvls of all the monster templates to get.
	 * @return the template list for the given level.
	 */
	public List<L2NpcTemplate> getAllMonstersOfLevel(int... lvls)
	{
		final List<L2NpcTemplate> list = new ArrayList<>();
		for (int lvl : lvls)
		{
			for (L2NpcTemplate t : _npcs.values(new L2NpcTemplate[0]))
			{
				if ((t.getLevel() == lvl) && t.isType("L2Monster"))
				{
					list.add(t);
				}
			}
		}
		return list;
	}
	
	/**
	 * @param letters of all the NPC templates which its name start with.
	 * @return the template list for the given letter.
	 */
	public List<L2NpcTemplate> getAllNpcStartingWith(String... letters)
	{
		final List<L2NpcTemplate> list = new ArrayList<>();
		for (String letter : letters)
		{
			for (L2NpcTemplate t : _npcs.values(new L2NpcTemplate[0]))
			{
				if (t.getName().startsWith(letter) && t.isType("L2Npc"))
				{
					list.add(t);
				}
			}
		}
		return list;
	}
	
	/**
	 * @param classTypes of all the templates to get.
	 * @return the template list for the given class type.
	 */
	public List<L2NpcTemplate> getAllNpcOfClassType(String... classTypes)
	{
		final List<L2NpcTemplate> list = new ArrayList<>();
		for (String classType : classTypes)
		{
			for (L2NpcTemplate t : _npcs.values(new L2NpcTemplate[0]))
			{
				if (t.isType(classType))
				{
					list.add(t);
				}
			}
		}
		return list;
	}
	
	/**
	 * Id equals to zero or lesser means all.
	 * @param id of the NPC to load.
	 */
	public void loadNpcs(int id)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			if (id > 0)
			{
				statement = con.prepareStatement("SELECT * FROM npc WHERE id = ?");
				statement.setInt(1, id);
			}
			else
			{
				statement = con.prepareStatement("SELECT * FROM npc ORDER BY id");
			}
			ResultSet rset = statement.executeQuery();
			
			int cont = 0;
			int cCont = 0;
			
			while (rset.next())
			{
				fillNpcTable(rset);
				cont++;
			}
			
			rset.close();
			statement.close();
			
			if (Config.CUSTOM_NPC_TABLE)
			{
				if (id > 0)
				{
					statement = con.prepareStatement("SELECT * FROM custom_npc WHERE id = ?");
					statement.setInt(1, id);
				}
				else
				{
					statement = con.prepareStatement("SELECT * FROM custom_npc ORDER BY id");
				}
				rset = statement.executeQuery();
				
				while (rset.next())
				{
					fillNpcTable(rset);
					cCont++;
				}
				
				rset.close();
				statement.close();
			}
			
			_log.info(getClass().getSimpleName() + ": Loaded " + cont + " (Custom: " + cCont + ") NPC template(s).");
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, getClass().getSimpleName() + ": Error creating NPC table.", e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
	}
	
	/**
	 * Id equals to zero or lesser means all.
	 * @param id of the NPC to load it's skills.
	 */
	public void loadNpcsSkills(int id)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			if (id > 0)
			{
				statement = con.prepareStatement("SELECT * FROM npcskills WHERE npcid = ?");
				statement.setInt(1, id);
			}
			else
			{
				statement = con.prepareStatement("SELECT * FROM npcskills ORDER BY npcid");
			}
			
			ResultSet rset = statement.executeQuery();
			
			int cont = 0;
			int cCont = 0;
			
			L2NpcTemplate npcDat = null;
			L2Skill npcSkill = null;
			
			while (rset.next())
			{
				int mobId = rset.getInt("npcid");
				npcDat = _npcs.get(mobId);
				
				if (npcDat == null)
				{
					_log.warning(getClass().getSimpleName() + ": Skill data for undefined NPC. npcId: " + mobId);
					continue;
				}
				
				int skillId = rset.getInt("skillid");
				int level = rset.getInt("level");
				
				if (skillId == L2Skill.SKILL_NPC_RACE)
				{
					npcDat.setRace(level);
					continue;
				}
				
				npcSkill = SkillTable.getInstance().getInfo(skillId, level);
				if (npcSkill == null)
				{
					continue;
				}
				cont++;
				npcDat.addSkill(npcSkill);
			}
			
			rset.close();
			statement.close();
			
			if (Config.CUSTOM_NPC_SKILLS_TABLE)
			{
				if (id > 0)
				{
					statement = con.prepareStatement("SELECT * FROM custom_npcskills WHERE npcid = ?");
					statement.setInt(1, id);
				}
				else
				{
					statement = con.prepareStatement("SELECT * FROM custom_npcskills ORDER BY npcid");
				}
				rset = statement.executeQuery();
				
				while (rset.next())
				{
					int mobId = rset.getInt("npcid");
					npcDat = _npcs.get(mobId);
					
					if (npcDat == null)
					{
						_log.warning("Custom " + getClass().getSimpleName() + ": Skill data for undefined NPC. npcId: " + mobId);
						continue;
					}
					
					int skillId = rset.getInt("skillid");
					int level = rset.getInt("level");
					
					if ((npcDat.getRace() == null) && (skillId == L2Skill.SKILL_NPC_RACE))
					{
						npcDat.setRace(level);
						continue;
					}
					
					npcSkill = SkillTable.getInstance().getInfo(skillId, level);
					
					if (npcSkill == null)
					{
						continue;
					}
					
					cCont++;
					npcDat.addSkill(npcSkill);
				}
				
				rset.close();
				statement.close();
			}
			
			_log.info(getClass().getSimpleName() + ": Loaded " + cont + " (Custom: " + cCont + ") npc skills.");
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, getClass().getSimpleName() + ": Error reading NPC skills table.", e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
	}
	
	/**
	 * Id equals to zero or lesser means all.
	 * @param id of the NPC to load it's drops.
	 */
	public void loadNpcsDrop(int id)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = null;
			
			if (id > 0)
			{
				statement = con.prepareStatement("SELECT * FROM droplist WHERE mobId = ? ORDER BY mobId, chance DESC");
				statement.setInt(1, id);
			}
			else
			{
				statement = con.prepareStatement("SELECT * FROM droplist ORDER BY mobId, chance DESC");
			}
			
			ResultSet rset = statement.executeQuery();
			L2DropData dropDat = null;
			L2NpcTemplate npcDat = null;
			
			int cont = 0;
			int cCont = 0;
			
			while (rset.next())
			{
				int mobId = rset.getInt("mobId");
				npcDat = _npcs.get(mobId);
				if (npcDat == null)
				{
					_log.warning(getClass().getSimpleName() + ": Drop data for undefined NPC. npcId: " + mobId);
					continue;
				}
				dropDat = new L2DropData();
				
				dropDat.setItemId(rset.getInt("itemId"));
				dropDat.setMinDrop(rset.getInt("min"));
				dropDat.setMaxDrop(rset.getInt("max"));
				dropDat.setChance(rset.getInt("chance"));
				
				int category = rset.getInt("category");
				
				if (ItemTable.getInstance().getTemplate(dropDat.getItemId()) == null)
				{
					_log.warning(getClass().getSimpleName() + ": Drop data for undefined item template! NpcId: " + mobId + " itemId: " + dropDat.getItemId());
					continue;
				}
				cont++;
				npcDat.addDropData(dropDat, category);
			}
			
			rset.close();
			statement.close();
			
			if (Config.CUSTOM_DROPLIST_TABLE)
			{
				if (id > 0)
				{
					statement = con.prepareStatement("SELECT * FROM custom_droplist WHERE mobId = ? ORDER BY mobId, chance DESC");
					statement.setInt(1, id);
				}
				else
				{
					statement = con.prepareStatement("SELECT * FROM custom_droplist ORDER BY mobId, chance DESC");
				}
				
				rset = statement.executeQuery();
				int mobId;
				int category;
				while (rset.next())
				{
					mobId = rset.getInt("mobId");
					npcDat = _npcs.get(mobId);
					if (npcDat == null)
					{
						_log.warning(getClass().getSimpleName() + ": CUSTOM DROPLIST: Drop data for undefined NPC. npcId: " + mobId);
						continue;
					}
					
					dropDat = new L2DropData();
					dropDat.setItemId(rset.getInt("itemId"));
					dropDat.setMinDrop(rset.getInt("min"));
					dropDat.setMaxDrop(rset.getInt("max"));
					dropDat.setChance(rset.getInt("chance"));
					category = rset.getInt("category");
					
					if (ItemTable.getInstance().getTemplate(dropDat.getItemId()) == null)
					{
						_log.warning(getClass().getSimpleName() + ": Custom drop data for undefined item template! NpcId: " + mobId + " itemId: " + dropDat.getItemId());
						continue;
					}
					
					npcDat.addDropData(dropDat, category);
					cCont++;
				}
				rset.close();
				statement.close();
			}
			_log.info(getClass().getSimpleName() + ": Loaded " + cont + " (Custom: " + cCont + ") drops.");
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, getClass().getSimpleName() + ": Error reading NPC dropdata. ", e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
	}
	
	/**
	 * Id equals to zero or lesser means all.
	 * @param id of the NPC to load it's skill learn list.
	 */
	private void loadNpcsSkillLearn(int id)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			if (id > 0)
			{
				statement = con.prepareStatement("SELECT * FROM skill_learn WHERE npc_id = ?");
				statement.setInt(1, id);
			}
			else
			{
				statement = con.prepareStatement("SELECT * FROM skill_learn");
			}
			
			ResultSet rset = statement.executeQuery();
			
			int cont = 0;
			int npcId;
			int classId;
			L2NpcTemplate npc;
			while (rset.next())
			{
				npcId = rset.getInt("npc_id");
				classId = rset.getInt("class_id");
				npc = getTemplate(npcId);
				if (npc == null)
				{
					_log.warning(getClass().getSimpleName() + ": Error getting NPC template ID " + npcId + " while trying to load skill trainer data.");
					continue;
				}
				
				cont++;
				npc.addTeachInfo(ClassId.getClassId(classId));
			}
			
			rset.close();
			statement.close();
			
			_log.info(getClass().getSimpleName() + ": Loaded " + cont + " Skill Learn.");
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, getClass().getSimpleName() + ": Error reading NPC trainer data.", e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
	}
	
	/**
	 * Id equals to zero or lesser means all.
	 * @param id of the NPC to load it's minions.
	 */
	public void loadMinions(int id)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = null;
			
			if (id > 0)
			{
				statement = con.prepareStatement("SELECT * FROM minions WHERE boss_id = ?");
				statement.setInt(1, id);
			}
			else
			{
				statement = con.prepareStatement("SELECT * FROM minions ORDER BY boss_id");
			}
			
			ResultSet rset = statement.executeQuery();
			
			L2MinionData minionDat = null;
			L2NpcTemplate npcDat = null;
			int cnt = 0;
			int raidId;
			while (rset.next())
			{
				raidId = rset.getInt("boss_id");
				npcDat = _npcs.get(raidId);
				if (npcDat == null)
				{
					_log.warning(getClass().getSimpleName() + ": Minion references undefined boss NPC. Boss NpcId: " + raidId);
					continue;
				}
				
				minionDat = new L2MinionData();
				minionDat.setMinionId(rset.getInt("minion_id"));
				minionDat.setAmountMin(rset.getInt("amount_min"));
				minionDat.setAmountMax(rset.getInt("amount_max"));
				npcDat.addRaidData(minionDat);
				cnt++;
			}
			
			rset.close();
			statement.close();
			_log.info(getClass().getSimpleName() + ": Loaded " + cnt + " Minions.");
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, getClass().getSimpleName() + ": Error loading minion data.", e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
	}
	
	/**
	 * Id equals to zero or lesser means all.
	 * @param id of the NPC to load it's AI data.
	 */
	public void loadNpcsAI(int id)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = null;
			
			if (id > 0)
			{
				statement = con.prepareStatement("SELECT * FROM npcaidata WHERE npcId = ?");
				statement.setInt(1, id);
			}
			else
			{
				statement = con.prepareStatement("SELECT * FROM npcaidata ORDER BY npcId");
			}
			
			ResultSet rset = statement.executeQuery();
			
			L2NpcAIData npcAIDat = null;
			L2NpcTemplate npcDat = null;
			
			int cont = 0;
			int cCont = 0;
			int npcId;
			while (rset.next())
			{
				npcId = rset.getInt("npcId");
				npcDat = _npcs.get(npcId);
				if (npcDat == null)
				{
					_log.severe(getClass().getSimpleName() + ": AI Data Error with id : " + npcId);
					continue;
				}
				npcAIDat = new L2NpcAIData();
				
				npcAIDat.setPrimarySkillId(rset.getInt("primarySkillId"));
				npcAIDat.setMinSkillChance(rset.getInt("minSkillChance"));
				npcAIDat.setMaxSkillChance(rset.getInt("maxSkillChance"));
				npcAIDat.setAggro(rset.getInt("aggro"));
				npcAIDat.setCanMove(rset.getInt("canMove"));
				npcAIDat.setShowName(rset.getBoolean("showName"));
				npcAIDat.setTargetable(rset.getBoolean("targetable"));
				npcAIDat.setSoulShot(rset.getInt("soulshot"));
				npcAIDat.setSpiritShot(rset.getInt("spiritshot"));
				npcAIDat.setSoulShotChance(rset.getInt("ssChance"));
				npcAIDat.setSpiritShotChance(rset.getInt("spsChance"));
				npcAIDat.setIsChaos(rset.getInt("isChaos"));
				npcAIDat.setShortRangeSkill(rset.getInt("minRangeSkill"));
				npcAIDat.setShortRangeChance(rset.getInt("minRangeChance"));
				npcAIDat.setLongRangeSkill(rset.getInt("maxRangeSkill"));
				npcAIDat.setLongRangeChance(rset.getInt("maxRangeChance"));
				npcAIDat.setClan(rset.getString("clan"));
				npcAIDat.setClanRange(rset.getInt("clanRange"));
				npcAIDat.setEnemyClan(rset.getString("enemyClan"));
				npcAIDat.setEnemyRange(rset.getInt("enemyRange"));
				npcAIDat.setDodge(rset.getInt("dodge"));
				npcAIDat.setAi(rset.getString("aiType"));
				
				npcDat.setAIData(npcAIDat);
				cont++;
			}
			
			rset.close();
			statement.close();
			
			if (Config.CUSTOM_NPC_TABLE)
			{
				if (id > 0)
				{
					statement = con.prepareStatement("SELECT * FROM custom_npcaidata WHERE npcId = ?");
					statement.setInt(1, id);
				}
				else
				{
					statement = con.prepareStatement("SELECT * FROM custom_npcaidata ORDER BY npcId");
				}
				
				rset = statement.executeQuery();
				while (rset.next())
				{
					npcId = rset.getInt("npcId");
					npcDat = _npcs.get(npcId);
					if (npcDat == null)
					{
						_log.severe(getClass().getSimpleName() + ": Custom AI Data Error with id : " + npcId);
						continue;
					}
					npcAIDat = new L2NpcAIData();
					
					npcAIDat.setPrimarySkillId(rset.getInt("primarySkillId"));
					npcAIDat.setMinSkillChance(rset.getInt("minSkillChance"));
					npcAIDat.setMaxSkillChance(rset.getInt("maxSkillChance"));
					npcAIDat.setAggro(rset.getInt("aggro"));
					npcAIDat.setCanMove(rset.getInt("canMove"));
					npcAIDat.setShowName(rset.getBoolean("showName"));
					npcAIDat.setTargetable(rset.getBoolean("targetable"));
					npcAIDat.setSoulShot(rset.getInt("soulshot"));
					npcAIDat.setSpiritShot(rset.getInt("spiritshot"));
					npcAIDat.setSoulShotChance(rset.getInt("ssChance"));
					npcAIDat.setSpiritShotChance(rset.getInt("spsChance"));
					npcAIDat.setIsChaos(rset.getInt("isChaos"));
					npcAIDat.setShortRangeSkill(rset.getInt("minRangeSkill"));
					npcAIDat.setShortRangeChance(rset.getInt("minRangeChance"));
					npcAIDat.setLongRangeSkill(rset.getInt("maxRangeSkill"));
					npcAIDat.setLongRangeChance(rset.getInt("maxRangeChance"));
					npcAIDat.setClan(rset.getString("clan"));
					npcAIDat.setClanRange(rset.getInt("clanRange"));
					npcAIDat.setEnemyClan(rset.getString("enemyClan"));
					npcAIDat.setEnemyRange(rset.getInt("enemyRange"));
					npcAIDat.setDodge(rset.getInt("dodge"));
					npcAIDat.setAi(rset.getString("aiType"));
					
					npcDat.setAIData(npcAIDat);
					cCont++;
				}
				
				rset.close();
				statement.close();
			}
			
			_log.info(getClass().getSimpleName() + ": Loaded " + cont + " (Custom: " + cCont + ") AI Data.");
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, getClass().getSimpleName() + ": Error reading NPC AI Data: " + e.getMessage(), e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
	}
	
	/**
	 * Id equals to zero or lesser means all.
	 * @param id of the NPC to load it's element data.
	 */
	public void loadNpcsElement(int id)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = null;
			if (id > 0)
			{
				statement = con.prepareStatement("SELECT * FROM npc_elementals WHERE npc_id = ?");
				statement.setInt(1, id);
			}
			else
			{
				statement = con.prepareStatement("SELECT * FROM npc_elementals ORDER BY npc_id");
			}
			
			ResultSet rset = statement.executeQuery();
			L2NpcTemplate npcDat = null;
			
			int cont = 0;
			int cCount = 0;
			int npcId;
			while (rset.next())
			{
				npcId = rset.getInt("npc_id");
				npcDat = _npcs.get(npcId);
				if (npcDat == null)
				{
					_log.severe("NPCElementals: Elementals Error with id : " + npcId);
					continue;
				}
				switch (rset.getByte("elemAtkType"))
				{
					case Elementals.FIRE:
						npcDat.setBaseFire(rset.getInt("elemAtkValue"));
						break;
					case Elementals.WATER:
						npcDat.setBaseWater(rset.getInt("elemAtkValue"));
						break;
					case Elementals.EARTH:
						npcDat.setBaseEarth(rset.getInt("elemAtkValue"));
						break;
					case Elementals.WIND:
						npcDat.setBaseWind(rset.getInt("elemAtkValue"));
						break;
					case Elementals.HOLY:
						npcDat.setBaseHoly(rset.getInt("elemAtkValue"));
						break;
					case Elementals.DARK:
						npcDat.setBaseDark(rset.getInt("elemAtkValue"));
						break;
					default:
						_log.severe("NPCElementals: Elementals Error with id : " + npcId + "; unknown elementType: " + rset.getByte("elemAtkType"));
						continue;
				}
				npcDat.setBaseFireRes(rset.getInt("fireDefValue"));
				npcDat.setBaseWaterRes(rset.getInt("waterDefValue"));
				npcDat.setBaseEarthRes(rset.getInt("earthDefValue"));
				npcDat.setBaseWindRes(rset.getInt("windDefValue"));
				npcDat.setBaseHolyRes(rset.getInt("holyDefValue"));
				npcDat.setBaseDarkRes(rset.getInt("darkDefValue"));
				cont++;
			}
			
			rset.close();
			statement.close();
			
			if (Config.CUSTOM_NPC_TABLE)
			{
				if (id > 0)
				{
					statement = con.prepareStatement("SELECT * FROM custom_npc_elementals WHERE npc_id = ?");
					statement.setInt(1, id);
				}
				else
				{
					statement = con.prepareStatement("SELECT * FROM custom_npc_elementals ORDER BY npc_id");
				}
				
				rset = statement.executeQuery();
				
				while (rset.next())
				{
					npcId = rset.getInt("npc_id");
					npcDat = _npcs.get(npcId);
					if (npcDat == null)
					{
						_log.severe("NPCElementals: Custom Elementals Error with id : " + npcId);
						continue;
					}
					switch (rset.getByte("elemAtkType"))
					{
						case Elementals.FIRE:
							npcDat.setBaseFire(rset.getInt("elemAtkValue"));
							break;
						case Elementals.WATER:
							npcDat.setBaseWater(rset.getInt("elemAtkValue"));
							break;
						case Elementals.EARTH:
							npcDat.setBaseEarth(rset.getInt("elemAtkValue"));
							break;
						case Elementals.WIND:
							npcDat.setBaseWind(rset.getInt("elemAtkValue"));
							break;
						case Elementals.HOLY:
							npcDat.setBaseHoly(rset.getInt("elemAtkValue"));
							break;
						case Elementals.DARK:
							npcDat.setBaseDark(rset.getInt("elemAtkValue"));
							break;
						default:
							_log.severe("NPCElementals: Custom Elementals Error with id : " + npcId + "; unknown elementType: " + rset.getByte("elemAtkType"));
							continue;
					}
					npcDat.setBaseFireRes(rset.getInt("fireDefValue"));
					npcDat.setBaseWaterRes(rset.getInt("waterDefValue"));
					npcDat.setBaseEarthRes(rset.getInt("earthDefValue"));
					npcDat.setBaseWindRes(rset.getInt("windDefValue"));
					npcDat.setBaseHolyRes(rset.getInt("holyDefValue"));
					npcDat.setBaseDarkRes(rset.getInt("darkDefValue"));
					cont++;
				}
				rset.close();
				statement.close();
			}
			
			_log.info(getClass().getSimpleName() + ": Loaded " + cont + " (Custom: " + cCount + ") elementals Data.");
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, getClass().getSimpleName() + ": Error reading NPC Elementals Data: " + e.getMessage(), e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
	}
	
	public static NpcTable getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final NpcTable _instance = new NpcTable();
	}
}
