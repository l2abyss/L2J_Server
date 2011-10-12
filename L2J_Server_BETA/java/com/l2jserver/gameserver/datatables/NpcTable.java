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
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;

import com.l2jserver.Config;
import com.l2jserver.L2DatabaseFactory;
import com.l2jserver.gameserver.model.Elementals;
import com.l2jserver.gameserver.model.L2DropCategory;
import com.l2jserver.gameserver.model.L2DropData;
import com.l2jserver.gameserver.model.L2MinionData;
import com.l2jserver.gameserver.model.L2NpcAIData;
import com.l2jserver.gameserver.model.L2Skill;
import com.l2jserver.gameserver.model.base.ClassId;
import com.l2jserver.gameserver.model.quest.Quest;
import com.l2jserver.gameserver.model.quest.Quest.QuestEventType;
import com.l2jserver.gameserver.skills.BaseStats;
import com.l2jserver.gameserver.templates.StatsSet;
import com.l2jserver.gameserver.templates.chars.L2NpcTemplate;

public class NpcTable
{
	private static Logger _log = Logger.getLogger(NpcTable.class.getName());
	
	private final TIntObjectHashMap<L2NpcTemplate> _npcs;
	
	public static NpcTable getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private NpcTable()
	{
		_npcs = new TIntObjectHashMap<L2NpcTemplate>();
		
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
		npcDat.set("jClass", NpcData.getString("class"));
		
		npcDat.set("baseShldDef", 0);
		npcDat.set("baseShldRate", 0);
		npcDat.set("baseCritRate", NpcData.getInt("critical"));
		
		npcDat.set("name", NpcData.getString("name"));
		npcDat.set("serverSideName", NpcData.getBoolean("serverSideName"));
		//npcDat.set("name", "");
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
		npcDat.set("aggroRange", NpcData.getInt("aggro"));
		npcDat.set("rhand", NpcData.getInt("rhand"));
		npcDat.set("lhand", NpcData.getInt("lhand"));
		npcDat.set("enchant", NpcData.getInt("enchant"));
		npcDat.set("baseWalkSpd", NpcData.getInt("walkspd"));
		npcDat.set("baseRunSpd", NpcData.getInt("runspd"));
		npcDat.set("targetable", NpcData.getBoolean("targetable"));
		npcDat.set("show_name", NpcData.getBoolean("show_name"));
		
		// constants, until we have stats in DB
		npcDat.safeSet("baseSTR", NpcData.getInt("str"), 0, BaseStats.MAX_STAT_VALUE, "Loading npc template id: "+NpcData.getInt("idTemplate"));
		npcDat.safeSet("baseCON", NpcData.getInt("con"), 0, BaseStats.MAX_STAT_VALUE, "Loading npc template id: "+NpcData.getInt("idTemplate"));
		npcDat.safeSet("baseDEX", NpcData.getInt("dex"), 0, BaseStats.MAX_STAT_VALUE, "Loading npc template id: "+NpcData.getInt("idTemplate"));
		npcDat.safeSet("baseINT", NpcData.getInt("int"), 0, BaseStats.MAX_STAT_VALUE, "Loading npc template id: "+NpcData.getInt("idTemplate"));
		npcDat.safeSet("baseWIT", NpcData.getInt("wit"), 0, BaseStats.MAX_STAT_VALUE, "Loading npc template id: "+NpcData.getInt("idTemplate"));
		npcDat.safeSet("baseMEN", NpcData.getInt("men"), 0, BaseStats.MAX_STAT_VALUE, "Loading npc template id: "+NpcData.getInt("idTemplate"));
		
		npcDat.set("baseHpMax", NpcData.getDouble("hp"));
		npcDat.set("baseCpMax", 0);
		npcDat.set("baseMpMax", NpcData.getDouble("mp"));
		npcDat.set("baseHpReg", NpcData.getFloat("hpreg") > 0 ? NpcData.getFloat("hpreg") : 1.5 + ((level - 1) / 10.0));
		npcDat.set("baseMpReg", NpcData.getFloat("mpreg") > 0 ? NpcData.getFloat("mpreg") : 0.9 + 0.3 * ((level - 1) / 10.0));
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
	
	public void reloadNpc(int id)
	{
		try
		{
			// save a copy of the old data
			L2NpcTemplate old = getTemplate(id);
			
			TIntObjectHashMap<L2Skill> skills = new TIntObjectHashMap<L2Skill>();
			List<L2MinionData> minions = new FastList<L2MinionData>();
			Map<QuestEventType, Quest[]> quests = new FastMap<QuestEventType, Quest[]>();
			ClassId[] classIds = null;
			FastList<L2DropCategory> categories = new FastList<L2DropCategory>();
			
			if (old != null)
			{
				if (old.getSkills() != null)
					skills.putAll(old.getSkills());
				
				if (old.getDropData() != null)
					categories.addAll(old.getDropData());
				
				if (old.getTeachInfo() != null)
					classIds = old.getTeachInfo().clone();
				
				if (old.getMinionData() != null)
					minions.addAll(old.getMinionData());
				
				if (!old.getEventQuests().isEmpty())
					quests.putAll(old.getEventQuests());
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
			
			if (old != null && created != null)
			{
				if (!skills.isEmpty())
				{
					for (L2Skill skill : skills.values(new L2Skill[0]))
						created.addSkill(skill);
				}
				if (classIds != null)
				{
					for (ClassId classId : classIds)
						created.addTeachInfo(classId);
				}
				if (!minions.isEmpty())
				{
					for (L2MinionData minion : minions)
						created.addRaidData(minion);
				}
				
				if (!quests.isEmpty())
				{
					created.getEventQuests().putAll(quests);
				}
			}
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "NPCTable: Could not reload data for NPC " + id + ": " + e.getMessage(), e);
		}
	}
	
	// just wrapper
	public void reloadAllNpc()
	{
		restoreNpcData();
	}
	
	public void saveNpc(StatsSet npc)
	{
		Map<String, Object> set = npc.getSet();
		
		int length = 0;
		
		for (Object obj : set.keySet())
		{
			// 15 is just guessed npc name length
			length += ((String) obj).length() + 7 + 15;
		}
		
		final StringBuilder sbValues = new StringBuilder(length);
		
		for (Object obj : set.keySet())
		{
			final String name = (String) obj;
			
			if (!name.equalsIgnoreCase("npcId"))
			{
				if (sbValues.length() > 0)
				{
					sbValues.append(", ");
				}
				
				sbValues.append(name);
				sbValues.append(" = '");
				sbValues.append(set.get(name));
				sbValues.append('\'');
			}
		}
		
		
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			int updated = 0;
			if (Config.CUSTOM_NPC_TABLE)
			{
				final StringBuilder sbQuery = new StringBuilder(sbValues.length() + 28);
				sbQuery.append("UPDATE custom_npc SET ");
				sbQuery.append(sbValues.toString());
				sbQuery.append(" WHERE id = ?");
				PreparedStatement statement = con.prepareStatement(sbQuery.toString());
				statement.setInt(1, npc.getInteger("npcId"));
				updated = statement.executeUpdate();
				statement.close();
			}
			if (updated == 0)
			{
				final StringBuilder sbQuery = new StringBuilder(sbValues.length() + 28);
				sbQuery.append("UPDATE npc SET ");
				sbQuery.append(sbValues.toString());
				sbQuery.append(" WHERE id = ?");
				PreparedStatement statement = con.prepareStatement(sbQuery.toString());
				statement.setInt(1, npc.getInteger("npcId"));
				statement.executeUpdate();
				statement.close();
			}
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "NPCTable: Could not store new NPC data in database: " + e.getMessage(), e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
	}
	
	public void replaceTemplate(L2NpcTemplate npc)
	{
		_npcs.put(npc.npcId, npc);
	}
	
	public L2NpcTemplate getTemplate(int id)
	{
		return _npcs.get(id);
	}
	
	public L2NpcTemplate getTemplateByName(String name)
	{
		for (L2NpcTemplate npcTemplate : _npcs.valueCollection())
			if (npcTemplate.name.equalsIgnoreCase(name))
				return npcTemplate;
		
		return null;
	}
	
	public L2NpcTemplate[] getAllOfLevel(int lvl)
	{
		List<L2NpcTemplate> list = new FastList<L2NpcTemplate>();
		
		for (L2NpcTemplate t : _npcs.valueCollection())
			if (t.level == lvl)
				list.add(t);
		
		return list.toArray(new L2NpcTemplate[list.size()]);
	}
	
	public L2NpcTemplate[] getAllMonstersOfLevel(int lvl)
	{
		List<L2NpcTemplate> list = new FastList<L2NpcTemplate>();
		
		for (L2NpcTemplate t : _npcs.valueCollection())
			if (t.level == lvl && "L2Monster".equals(t.type))
				list.add(t);
		
		return list.toArray(new L2NpcTemplate[list.size()]);
	}
	
	public L2NpcTemplate[] getAllNpcStartingWith(String letter)
	{
		List<L2NpcTemplate> list = new FastList<L2NpcTemplate>();
		
		for (L2NpcTemplate t : _npcs.valueCollection())
			if (t.name.startsWith(letter) && "L2Npc".equals((t).type))
				list.add(t);
		
		return list.toArray(new L2NpcTemplate[list.size()]);
	}
	
	/**
	 * @param classType
	 * @return
	 */
	public L2NpcTemplate[] getAllNpcOfClassType(String classType)
	{
		List<L2NpcTemplate> list = new FastList<L2NpcTemplate>();
		
		for (L2NpcTemplate t : _npcs.valueCollection())
			if (classType.equals(t.type))
				list.add(t);
		
		return list.toArray(new L2NpcTemplate[list.size()]);
	}
	
	public void loadNpcs(int id)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = null;
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
			
			_log.info("NpcTable: Loaded " + cont + " (Custom: " + cCont + ") NPC template(s).");
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "NPCTable: Error creating NPC table.", e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
	}
	
	public void loadNpcsSkills(int id)
	{
		Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = null;
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
					_log.warning("NPCTable: Skill data for undefined NPC. npcId: " + mobId);
					continue;
				}
				
				int skillId = rset.getInt("skillid");
				int level = rset.getInt("level");
				
				if (npcDat.race == null && skillId == 4416)
				{
					npcDat.setRace(level);
					continue;
				}
				
				npcSkill = SkillTable.getInstance().getInfo(skillId, level);
				
				if (npcSkill == null)
					continue;
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
						_log.warning("Custom NPCTable: Skill data for undefined NPC. npcId: " + mobId);
						continue;
					}
					
					int skillId = rset.getInt("skillid");
					int level = rset.getInt("level");
					
					if (npcDat.race == null && skillId == 4416)
					{
						npcDat.setRace(level);
						continue;
					}
					
					npcSkill = SkillTable.getInstance().getInfo(skillId, level);
					
					if (npcSkill == null)
						continue;
					
					cCont++;
					npcDat.addSkill(npcSkill);
				}
				
				rset.close();
				statement.close();
			}
			
			_log.info("NpcTable: Loaded " + cont + " (Custom: " + cCont + ") npc skills.");
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "NPCTable: Error reading NPC skills table.", e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
	}
	
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
					_log.warning("NPCTable: Drop data for undefined NPC. npcId: " + mobId);
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
					_log.warning("Drop data for undefined item template! NpcId: " + mobId+" itemId: "+dropDat.getItemId());
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
			
				while (rset.next())
				{
					int mobId = rset.getInt("mobId");
					npcDat = _npcs.get(mobId);
					if (npcDat == null)
					{
						_log.warning("NPCTable: CUSTOM DROPLIST: Drop data for undefined NPC. npcId: " + mobId);
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
						_log.warning("Custom drop data for undefined item template! NpcId: " + mobId+" itemId: "+dropDat.getItemId());
						continue;
					}
					
					npcDat.addDropData(dropDat, category);
					cCont++;
				}
				rset.close();
				statement.close();
			}
			_log.info("NpcTable: Loaded " + cont + " (Custom: " + cCont + ") drops.");
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "NPCTable: Error reading NPC dropdata. ", e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
	}
	
	private void loadNpcsSkillLearn(int id)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = null;
			
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
			
			while (rset.next())
			{
				int npcId = rset.getInt("npc_id");
				int classId = rset.getInt("class_id");
				L2NpcTemplate npc = getTemplate(npcId);
				
				if (npc == null)
				{
					_log.warning("NPCTable: Error getting NPC template ID " + npcId + " while trying to load skill trainer data.");
					continue;
				}
				cont++;
				npc.addTeachInfo(ClassId.values()[classId]);
			}
			
			rset.close();
			statement.close();
			
			_log.info("NpcTable: Loaded " + cont + " Skill Learn.");
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "NPCTable: Error reading NPC trainer data.", e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
	}
	
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
			
			while (rset.next())
			{
				int raidId = rset.getInt("boss_id");
				npcDat = _npcs.get(raidId);
				if (npcDat == null)
				{
					_log.warning("Minion references undefined boss NPC. Boss NpcId: " + raidId);
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
			_log.info("NpcTable: Loaded " + cnt + " Minions.");
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "NPCTable: Error loading minion data.", e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
	}
	
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
			
			while (rset.next())
			{
				int npc_id = rset.getInt("npcId");
				npcDat = _npcs.get(npc_id);
				if (npcDat == null)
				{
					_log.severe("NPCTable: AI Data Error with id : " + npc_id);
					continue;
				}
				npcAIDat = new L2NpcAIData();
				
				npcAIDat.setPrimarySkillId(rset.getInt("primarySkillId"));
				npcAIDat.setMinSkillChance(rset.getInt("minSkillChance"));
				npcAIDat.setMaxSkillChance(rset.getInt("maxSkillChance"));
				npcAIDat.setCanMove(rset.getInt("canMove"));
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
					int npc_id = rset.getInt("npcId");
					npcDat = _npcs.get(npc_id);
					if (npcDat == null)
					{
						_log.severe("NPCTable: Custom AI Data Error with id : " + npc_id);
						continue;
					}
					npcAIDat = new L2NpcAIData();
					
					npcAIDat.setPrimarySkillId(rset.getInt("primarySkillId"));
					npcAIDat.setMinSkillChance(rset.getInt("minSkillChance"));
					npcAIDat.setMaxSkillChance(rset.getInt("maxSkillChance"));
					npcAIDat.setCanMove(rset.getInt("canMove"));
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
			
			_log.info("NpcTable: Loaded " + cont + " (Custom: " + cCont + ") AI Data.");
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "NPCTable: Error reading NPC AI Data: " + e.getMessage(), e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
	}
	
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
			
			while (rset.next())
			{
				int npc_id = rset.getInt("npc_id");
				npcDat = _npcs.get(npc_id);
				if (npcDat == null)
				{
					_log.severe("NPCElementals: Elementals Error with id : " + npc_id);
					continue;
				}
				switch(rset.getByte("elemAtkType"))
				{
					case Elementals.FIRE:
						npcDat.baseFire = rset.getInt("elemAtkValue");
						break;
					case Elementals.WATER:
						npcDat.baseWater = rset.getInt("elemAtkValue");
						break;
					case Elementals.EARTH:
						npcDat.baseEarth = rset.getInt("elemAtkValue");
						break;
					case Elementals.WIND:
						npcDat.baseWind = rset.getInt("elemAtkValue");
						break;
					case Elementals.HOLY:
						npcDat.baseHoly = rset.getInt("elemAtkValue");
						break;
					case Elementals.DARK:
						npcDat.baseDark = rset.getInt("elemAtkValue");
						break;
					default:
						_log.severe("NPCElementals: Elementals Error with id : " + npc_id + "; unknown elementType: " + rset.getByte("elemAtkType"));
						continue;
				}
				npcDat.baseFireRes = rset.getInt("fireDefValue");
				npcDat.baseWaterRes = rset.getInt("waterDefValue");
				npcDat.baseEarthRes = rset.getInt("earthDefValue");
				npcDat.baseWindRes = rset.getInt("windDefValue");
				npcDat.baseHolyRes = rset.getInt("holyDefValue");
				npcDat.baseDarkRes = rset.getInt("darkDefValue");
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
					int npc_id = rset.getInt("npc_id");
					npcDat = _npcs.get(npc_id);
					if (npcDat == null)
					{
						_log.severe("NPCElementals: Custom Elementals Error with id : " + npc_id);
						continue;
					}
					switch(rset.getByte("elemAtkType"))
					{
						case Elementals.FIRE:
							npcDat.baseFire = rset.getInt("elemAtkValue");
							break;
						case Elementals.WATER:
							npcDat.baseWater = rset.getInt("elemAtkValue");
							break;
						case Elementals.EARTH:
							npcDat.baseEarth = rset.getInt("elemAtkValue");
							break;
						case Elementals.WIND:
							npcDat.baseWind = rset.getInt("elemAtkValue");
							break;
						case Elementals.HOLY:
							npcDat.baseHoly = rset.getInt("elemAtkValue");
							break;
						case Elementals.DARK:
							npcDat.baseDark = rset.getInt("elemAtkValue");
							break;
						default:
							_log.severe("NPCElementals: Custom Elementals Error with id : " + npc_id + "; unknown elementType: " + rset.getByte("elemAtkType"));
							continue;
					}
					npcDat.baseFireRes = rset.getInt("fireDefValue");
					npcDat.baseWaterRes = rset.getInt("waterDefValue");
					npcDat.baseEarthRes = rset.getInt("earthDefValue");
					npcDat.baseWindRes = rset.getInt("windDefValue");
					npcDat.baseHolyRes = rset.getInt("holyDefValue");
					npcDat.baseDarkRes = rset.getInt("darkDefValue");
					cont++;
				}
				rset.close();
				statement.close();
			}
			
			_log.info("NpcTable: Loaded " + cont + " (Custom: " + cCount + ") elementals Data.");
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "NPCTable: Error reading NPC Elementals Data: " + e.getMessage(), e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final NpcTable _instance = new NpcTable();
	}
}
