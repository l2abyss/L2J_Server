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

import gnu.trove.TIntObjectHashMap;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastList;

import com.l2jserver.Config;
import com.l2jserver.L2DatabaseFactory;
import com.l2jserver.gameserver.model.Elementals;
import com.l2jserver.gameserver.model.L2DropCategory;
import com.l2jserver.gameserver.model.L2DropData;
import com.l2jserver.gameserver.model.L2MinionData;
import com.l2jserver.gameserver.model.L2NpcAIData;
import com.l2jserver.gameserver.model.L2Skill;
import com.l2jserver.gameserver.model.base.ClassId;
import com.l2jserver.gameserver.skills.BaseStats;
import com.l2jserver.gameserver.templates.StatsSet;
import com.l2jserver.gameserver.templates.chars.L2NpcTemplate;

/**
 * This class ...
 *
 * @version $Revision: 1.8.2.6.2.9 $ $Date: 2005/04/06 16:13:25 $
 */
public class NpcTable
{
	private static Logger _log = Logger.getLogger(NpcTable.class.getName());
	
	private TIntObjectHashMap<L2NpcTemplate> _npcs;
	
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
		Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			try
			{
				statement = con.prepareStatement("SELECT "
						+ L2DatabaseFactory.getInstance().safetyString(new String[] { "id", "idTemplate", "name", "serverSideName",
								"title", "serverSideTitle", "class", "collision_radius", "collision_height", "level", "sex", "type",
								"attackrange", "hp", "mp", "hpreg", "mpreg", "str", "con", "dex", "int", "wit", "men", "exp", "sp", "patk",
								"pdef", "matk", "mdef", "atkspd", "critical", "aggro", "matkspd", "rhand", "lhand", "enchant", "walkspd", "runspd",
								"dropHerbGroup" }) + " FROM npc");
				ResultSet npcdata = statement.executeQuery();
				
				fillNpcTable(npcdata, false);
				npcdata.close();
				statement.close();
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "NPCTable: Error creating NPC table.", e);
			}
			if (Config.CUSTOM_NPC_TABLE) // reload certain NPCs
			{
				try
				{
					statement = con.prepareStatement("SELECT "
							+ L2DatabaseFactory.getInstance().safetyString(new String[] { "id", "idTemplate", "name", "serverSideName",
									"title", "serverSideTitle", "class", "collision_radius", "collision_height", "level", "sex", "type",
									"attackrange", "hp", "mp", "hpreg", "mpreg", "str", "con", "dex", "int", "wit", "men", "exp", "sp",
									"patk", "pdef", "matk", "mdef", "atkspd", "critical", "aggro", "matkspd", "rhand", "lhand", "enchant", "walkspd",
									"runspd", "dropHerbGroup" }) + " FROM custom_npc");
					ResultSet npcdata = statement.executeQuery();
					
					fillNpcTable(npcdata, true);
					npcdata.close();
					statement.close();
				}
				catch (Exception e)
				{
					_log.log(Level.SEVERE, "NPCTable: Error creating custom NPC table.", e);
				}
			}
			
			try
			{
				statement = con.prepareStatement("SELECT npcid, skillid, level FROM npcskills");
				ResultSet npcskills = statement.executeQuery();
				L2NpcTemplate npcDat = null;
				L2Skill npcSkill = null;
				
				while (npcskills.next())
				{
					int mobId = npcskills.getInt("npcid");
					npcDat = _npcs.get(mobId);
					
					if (npcDat == null)
					{
						_log.warning("NPCTable: Skill data for undefined NPC. npcId: " + mobId);
						continue;
					}
					
					int skillId = npcskills.getInt("skillid");
					int level = npcskills.getInt("level");
					
					if (npcDat.race == null && skillId == 4416)
					{
						npcDat.setRace(level);
						continue;
					}
					
					npcSkill = SkillTable.getInstance().getInfo(skillId, level);
					
					if (npcSkill == null)
						continue;
					
					npcDat.addSkill(npcSkill);
				}
				
				npcskills.close();
				statement.close();
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "NPCTable: Error reading NPC skills table.", e);
			}
			
			if (Config.CUSTOM_NPC_SKILLS_TABLE)
			{
				try
				{
					statement = con.prepareStatement("SELECT npcid, skillid, level FROM custom_npcskills");
					ResultSet npcskills = statement.executeQuery();
					L2NpcTemplate npcDat = null;
					L2Skill npcSkill = null;
					
					while (npcskills.next())
					{
						int mobId = npcskills.getInt("npcid");
						npcDat = _npcs.get(mobId);
						
						if (npcDat == null)
						{
							_log.warning("Custom NPCTable: Skill data for undefined NPC. npcId: " + mobId);
							continue;
						}
						
						int skillId = npcskills.getInt("skillid");
						int level = npcskills.getInt("level");
						
						if (npcDat.race == null && skillId == 4416)
						{
							npcDat.setRace(level);
							continue;
						}
						
						npcSkill = SkillTable.getInstance().getInfo(skillId, level);
						
						if (npcSkill == null)
							continue;
						
						npcDat.addSkill(npcSkill);
					}
					
					npcskills.close();
					statement.close();
				}
				catch (Exception e)
				{
					_log.log(Level.SEVERE, "Custom NPCTable: Error reading NPC skills table.", e);
				}
			}
			
			try
			{
				statement = con.prepareStatement("SELECT "
						+ L2DatabaseFactory.getInstance().safetyString(new String[] { "mobId", "itemId", "min", "max", "category", "chance" })
						+ " FROM droplist ORDER BY mobId, chance DESC");
				ResultSet dropData = statement.executeQuery();
				L2DropData dropDat = null;
				L2NpcTemplate npcDat = null;
				
				while (dropData.next())
				{
					int mobId = dropData.getInt("mobId");
					npcDat = _npcs.get(mobId);
					if (npcDat == null)
					{
						_log.warning("NPCTable: Drop data for undefined NPC. npcId: " + mobId);
						continue;
					}
					dropDat = new L2DropData();
					
					dropDat.setItemId(dropData.getInt("itemId"));
					dropDat.setMinDrop(dropData.getInt("min"));
					dropDat.setMaxDrop(dropData.getInt("max"));
					dropDat.setChance(dropData.getInt("chance"));
					
					int category = dropData.getInt("category");
					
					if (ItemTable.getInstance().getTemplate(dropDat.getItemId()) == null)
					{
						_log.warning("Drop data for undefined item template! NpcId: " + mobId+" itemId: "+dropDat.getItemId());
						continue;
					}
					
					npcDat.addDropData(dropDat, category);
				}
				
				dropData.close();
				statement.close();
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "NPCTable: Error reading NPC dropdata. ", e);
			}
			
			if (Config.CUSTOM_DROPLIST_TABLE)
			{
				try
				{
					statement = con.prepareStatement("SELECT "
							+ L2DatabaseFactory.getInstance().safetyString(new String[] { "mobId", "itemId", "min", "max", "category",
							"chance" }) + " FROM custom_droplist ORDER BY mobId, chance DESC");
					ResultSet dropData = statement.executeQuery();
					L2DropData dropDat = null;
					L2NpcTemplate npcDat = null;
					int cCount = 0;
					while (dropData.next())
					{
						int mobId = dropData.getInt("mobId");
						npcDat = _npcs.get(mobId);
						if (npcDat == null)
						{
							_log.warning("NPCTable: CUSTOM DROPLIST: Drop data for undefined NPC. npcId: " + mobId);
							continue;
						}
						dropDat = new L2DropData();
						dropDat.setItemId(dropData.getInt("itemId"));
						dropDat.setMinDrop(dropData.getInt("min"));
						dropDat.setMaxDrop(dropData.getInt("max"));
						dropDat.setChance(dropData.getInt("chance"));
						int category = dropData.getInt("category");
						
						if (ItemTable.getInstance().getTemplate(dropDat.getItemId()) == null)
						{
							_log.warning("Custom drop data for undefined item template! NpcId: " + mobId+" itemId: "+dropDat.getItemId());
							continue;
						}
						
						npcDat.addDropData(dropDat, category);
						cCount++;
					}
					dropData.close();
					statement.close();
					_log.info("CustomDropList: Added " + cCount + " custom droplist.");
				}
				catch (Exception e)
				{
					_log.log(Level.SEVERE, "NPCTable: Error reading NPC custom dropdata.", e);
				}
			}
			
			try
			{
				statement = con.prepareStatement("SELECT "
						+ L2DatabaseFactory.getInstance().safetyString(new String[] { "npc_id", "class_id" }) + " FROM skill_learn");
				ResultSet learndata = statement.executeQuery();
				
				while (learndata.next())
				{
					int npcId = learndata.getInt("npc_id");
					int classId = learndata.getInt("class_id");
					L2NpcTemplate npc = getTemplate(npcId);
					
					if (npc == null)
					{
						_log.warning("NPCTable: Error getting NPC template ID " + npcId + " while trying to load skill trainer data.");
						continue;
					}
					
					npc.addTeachInfo(ClassId.values()[classId]);
				}
				
				learndata.close();
				statement.close();
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "NPCTable: Error reading NPC trainer data.", e);
			}
			
			try
			{
				statement = con.prepareStatement("SELECT "
						+ L2DatabaseFactory.getInstance().safetyString(new String[] { "boss_id", "minion_id", "amount_min", "amount_max" })
						+ " FROM minions");
				ResultSet minionData = statement.executeQuery();
				L2MinionData minionDat = null;
				L2NpcTemplate npcDat = null;
				int cnt = 0;
				
				while (minionData.next())
				{
					int raidId = minionData.getInt("boss_id");
					npcDat = _npcs.get(raidId);
					if (npcDat == null)
					{
						_log.warning("Minion references undefined boss NPC. Boss NpcId: " + raidId);
						continue;
					}
					minionDat = new L2MinionData();
					minionDat.setMinionId(minionData.getInt("minion_id"));
					minionDat.setAmountMin(minionData.getInt("amount_min"));
					minionDat.setAmountMax(minionData.getInt("amount_max"));
					npcDat.addRaidData(minionDat);
					cnt++;
				}
				
				minionData.close();
				statement.close();
				_log.info("NpcTable: Loaded " + cnt + " Minions.");
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "NPCTable: Error loading minion data.", e);
			}
			
			//-------------------------------------------------------------------
			//NPC AI Attributes & Data ...
			
			try
			{
				statement = con.prepareStatement("SELECT " + L2DatabaseFactory.getInstance().safetyString(new String[] {"npc_id", "primary_attack","skill_chance","can_move","soulshot","spiritshot","sschance","spschance","minrangeskill","minrangechance","maxrangeskill","maxrangechance","ischaos","clan","clan_range","enemyClan","enemyRange","ai_type","dodge"}) + " FROM npcaidata ORDER BY npc_id");
				ResultSet NpcAIDataTable = statement.executeQuery();
				L2NpcAIData npcAIDat = null;
				L2NpcTemplate npcDat = null;
				int cont=0;
				while (NpcAIDataTable.next())
				{
					int npc_id = NpcAIDataTable.getInt("npc_id");
					npcDat = _npcs.get(npc_id);
					if (npcDat == null)
					{
						_log.severe("NPCTable: AI Data Error with id : " + npc_id);
						continue;
					}
					npcAIDat = new L2NpcAIData();
					
					npcAIDat.setPrimaryAttack(NpcAIDataTable.getInt("primary_attack"));
					npcAIDat.setSkillChance(NpcAIDataTable.getInt("skill_chance"));
					npcAIDat.setCanMove(NpcAIDataTable.getInt("can_move"));
					npcAIDat.setSoulShot(NpcAIDataTable.getInt("soulshot"));
					npcAIDat.setSpiritShot(NpcAIDataTable.getInt("spiritshot"));
					npcAIDat.setSoulShotChance(NpcAIDataTable.getInt("sschance"));
					npcAIDat.setSpiritShotChance(NpcAIDataTable.getInt("spschance"));
					npcAIDat.setIsChaos(NpcAIDataTable.getInt("ischaos"));
					npcAIDat.setShortRangeSkill(NpcAIDataTable.getInt("minrangeskill"));
					npcAIDat.setShortRangeChance(NpcAIDataTable.getInt("minrangechance"));
					npcAIDat.setLongRangeSkill(NpcAIDataTable.getInt("maxrangeskill"));
					npcAIDat.setLongRangeChance(NpcAIDataTable.getInt("maxrangechance"));
					//npcAIDat.setSwitchRangeChance(NpcAIDataTable.getInt("rangeswitchchance"));
					npcAIDat.setClan(NpcAIDataTable.getString("clan"));
					npcAIDat.setClanRange(NpcAIDataTable.getInt("clan_range"));
					npcAIDat.setEnemyClan(NpcAIDataTable.getString("enemyClan"));
					npcAIDat.setEnemyRange(NpcAIDataTable.getInt("enemyRange"));
					npcAIDat.setDodge(NpcAIDataTable.getInt("dodge"));
					npcAIDat.setAi(NpcAIDataTable.getString("ai_type"));
					//npcAIDat.setBaseShldRate(NpcAIDataTable.getInt("baseShldRate"));
					//npcAIDat.setBaseShldDef(NpcAIDataTable.getInt("baseShldDef"));
					
					
					//npcDat.addAIData(npcAIDat);
					npcDat.setAIData(npcAIDat);
					cont++;
				}
				
				NpcAIDataTable.close();
				statement.close();
				_log.info("NPC AI Data Table: Loaded " + cont + " AI Data.");
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "NPCTable: Error reading NPC AI Data: " + e.getMessage(), e);
			}
			
			if (Config.CUSTOM_NPC_TABLE)
			{
				try
				{
					statement = con.prepareStatement("SELECT " + L2DatabaseFactory.getInstance().safetyString(new String[] { "npc_id", "primary_attack", "skill_chance", "can_move", "soulshot", "spiritshot", "sschance", "spschance", "minrangeskill", "minrangechance", "maxrangeskill", "maxrangechance", "ischaos", "clan", "clan_range", "enemyClan", "enemyRange", "ai_type", "dodge" }) + " FROM custom_npcaidata ORDER BY npc_id");
					ResultSet NpcAIDataTable = statement.executeQuery();
					L2NpcAIData npcAIDat = null;
					L2NpcTemplate npcDat = null;
					int cont = 0;
					while (NpcAIDataTable.next())
					{
						int npc_id = NpcAIDataTable.getInt("npc_id");
						npcDat = _npcs.get(npc_id);
						if (npcDat == null)
						{
							_log.severe("NPCTable: Custom AI Data Error with id : " + npc_id);
							continue;
						}
						npcAIDat = new L2NpcAIData();
						
						npcAIDat.setPrimaryAttack(NpcAIDataTable.getInt("primary_attack"));
						npcAIDat.setSkillChance(NpcAIDataTable.getInt("skill_chance"));
						npcAIDat.setCanMove(NpcAIDataTable.getInt("can_move"));
						npcAIDat.setSoulShot(NpcAIDataTable.getInt("soulshot"));
						npcAIDat.setSpiritShot(NpcAIDataTable.getInt("spiritshot"));
						npcAIDat.setSoulShotChance(NpcAIDataTable.getInt("sschance"));
						npcAIDat.setSpiritShotChance(NpcAIDataTable.getInt("spschance"));
						npcAIDat.setIsChaos(NpcAIDataTable.getInt("ischaos"));
						npcAIDat.setShortRangeSkill(NpcAIDataTable.getInt("minrangeskill"));
						npcAIDat.setShortRangeChance(NpcAIDataTable.getInt("minrangechance"));
						npcAIDat.setLongRangeSkill(NpcAIDataTable.getInt("maxrangeskill"));
						npcAIDat.setLongRangeChance(NpcAIDataTable.getInt("maxrangechance"));
						//npcAIDat.setSwitchRangeChance(NpcAIDataTable.getInt("rangeswitchchance"));
						npcAIDat.setClan(NpcAIDataTable.getString("clan"));
						npcAIDat.setClanRange(NpcAIDataTable.getInt("clan_range"));
						npcAIDat.setEnemyClan(NpcAIDataTable.getString("enemyClan"));
						npcAIDat.setEnemyRange(NpcAIDataTable.getInt("enemyRange"));
						npcAIDat.setDodge(NpcAIDataTable.getInt("dodge"));
						npcAIDat.setAi(NpcAIDataTable.getString("ai_type"));
						//npcAIDat.setBaseShldRate(NpcAIDataTable.getInt("baseShldRate"));
						//npcAIDat.setBaseShldDef(NpcAIDataTable.getInt("baseShldDef"));
						
						//npcDat.addAIData(npcAIDat);
						npcDat.setAIData(npcAIDat);
						cont++;
					}
					
					NpcAIDataTable.close();
					statement.close();
					_log.info("NPC AI Data Table: Loaded " + cont + " Custom AI Data.");
				}
				catch (Exception e)
				{
					_log.log(Level.SEVERE, "NPCTable: Error reading NPC Custom AI Data: " + e.getMessage(), e);
				}
			}
			
			try
			{
				statement = con.prepareStatement("SELECT " + L2DatabaseFactory.getInstance().safetyString(new String[] {"npc_id", "elemAtkType","elemAtkValue","fireDefValue","waterDefValue","earthDefValue","windDefValue","holyDefValue","darkDefValue"}) + " FROM npc_elementals ORDER BY npc_id");
				ResultSet NpcElementals = statement.executeQuery();
				L2NpcTemplate npcDat = null;
				int cont=0;
				while (NpcElementals.next())
				{
					int npc_id = NpcElementals.getInt("npc_id");
					npcDat = _npcs.get(npc_id);
					if (npcDat == null)
					{
						_log.severe("NPCElementals: Elementals Error with id : " + npc_id);
						continue;
					}
					switch(NpcElementals.getByte("elemAtkType"))
					{
						case Elementals.FIRE:
							npcDat.baseFire = NpcElementals.getInt("elemAtkValue");
							break;
						case Elementals.WATER:
							npcDat.baseWater = NpcElementals.getInt("elemAtkValue");
							break;
						case Elementals.EARTH:
							npcDat.baseEarth = NpcElementals.getInt("elemAtkValue");
							break;
						case Elementals.WIND:
							npcDat.baseWind = NpcElementals.getInt("elemAtkValue");
							break;
						case Elementals.HOLY:
							npcDat.baseHoly = NpcElementals.getInt("elemAtkValue");
							break;
						case Elementals.DARK:
							npcDat.baseDark = NpcElementals.getInt("elemAtkValue");
							break;
						default:
							_log.severe("NPCElementals: Elementals Error with id : " + npc_id + "; unknown elementType: " + NpcElementals.getByte("elemAtkType"));
							continue;
					}
					npcDat.baseFireRes = NpcElementals.getInt("fireDefValue");
					npcDat.baseWaterRes = NpcElementals.getInt("waterDefValue");
					npcDat.baseEarthRes = NpcElementals.getInt("earthDefValue");
					npcDat.baseWindRes = NpcElementals.getInt("windDefValue");
					npcDat.baseHolyRes = NpcElementals.getInt("holyDefValue");
					npcDat.baseDarkRes = NpcElementals.getInt("darkDefValue");
					cont++;
				}
				NpcElementals.close();
				statement.close();
				_log.info("NPC Elementals Data Table: Loaded " + cont + " elementals Data.");
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "NPCTable: Error reading NPC Elementals Data: " + e.getMessage(), e);
			}
			
			if (Config.CUSTOM_NPC_TABLE)
			{
				try
				{
					statement = con.prepareStatement("SELECT " + L2DatabaseFactory.getInstance().safetyString(new String[] {"npc_id", "elemAtkType","elemAtkValue","fireDefValue","waterDefValue","earthDefValue","windDefValue","holyDefValue","darkDefValue"}) + " FROM custom_npc_elementals ORDER BY npc_id");
					ResultSet NpcElementals = statement.executeQuery();
					L2NpcTemplate npcDat = null;
					int cont=0;
					while (NpcElementals.next())
					{
						int npc_id = NpcElementals.getInt("npc_id");
						npcDat = _npcs.get(npc_id);
						if (npcDat == null)
						{
							_log.severe("NPCElementals: custom Elementals Error with id : " + npc_id);
							continue;
						}
						switch(NpcElementals.getByte("elemAtkType"))
						{
							case Elementals.FIRE:
								npcDat.baseFire = NpcElementals.getInt("elemAtkValue");
								break;
							case Elementals.WATER:
								npcDat.baseWater = NpcElementals.getInt("elemAtkValue");
								break;
							case Elementals.EARTH:
								npcDat.baseEarth = NpcElementals.getInt("elemAtkValue");
								break;
							case Elementals.WIND:
								npcDat.baseWind = NpcElementals.getInt("elemAtkValue");
								break;
							case Elementals.HOLY:
								npcDat.baseHoly = NpcElementals.getInt("elemAtkValue");
								break;
							case Elementals.DARK:
								npcDat.baseDark = NpcElementals.getInt("elemAtkValue");
								break;
							default:
								_log.severe("NPCElementals: custom Elementals Error with id : " + npc_id + "; unknown elementType: " + NpcElementals.getByte("elemAtkType"));
								continue;
						}
						npcDat.baseFireRes = NpcElementals.getInt("fireDefValue");
						npcDat.baseWaterRes = NpcElementals.getInt("waterDefValue");
						npcDat.baseEarthRes = NpcElementals.getInt("earthDefValue");
						npcDat.baseWindRes = NpcElementals.getInt("windDefValue");
						npcDat.baseHolyRes = NpcElementals.getInt("holyDefValue");
						npcDat.baseDarkRes = NpcElementals.getInt("darkDefValue");
						cont++;
					}
					NpcElementals.close();
					statement.close();
					_log.info("NPC Elementals Data Table: Loaded " + cont + " custom elementals Data.");
				}
				catch (Exception e)
				{
					_log.log(Level.SEVERE, "NPCTable: Error reading NPC Custom Elementals Data: " + e.getMessage(), e);
				}
			}
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "NPCTable: Failed loading database connection: " + e.getMessage(), e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
	}
	
	private void fillNpcTable(ResultSet NpcData, boolean customData) throws Exception
	{
		int count = 0;
		while (NpcData.next())
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
			
			L2NpcTemplate template = new L2NpcTemplate(npcDat);
			/*template.addVulnerability(Stats.BOW_WPN_VULN, 1);
			template.addVulnerability(Stats.CROSSBOW_WPN_VULN, 1);
			template.addVulnerability(Stats.BLUNT_WPN_VULN, 1);
			template.addVulnerability(Stats.DAGGER_WPN_VULN, 1);*/
			
			_npcs.put(id, template);
			count++;
		}
		
		if (!customData)
			_log.info("NpcTable: (Re)Loaded " + count + " NPC template(s).");
		else
			_log.info("NpcTable: (Re)Loaded " + count + " custom NPC template(s).");
	}
	
	public void reloadNpc(int id)
	{
		Connection con = null;
		
		try
		{
			// save a copy of the old data
			L2NpcTemplate old = getTemplate(id);
			TIntObjectHashMap<L2Skill> skills = new TIntObjectHashMap<L2Skill>();
			
			if (old.getSkills() != null)
				skills.putAll(old.getSkills());
			
			FastList<L2DropCategory> categories = new FastList<L2DropCategory>();
			
			if (old.getDropData() != null)
				categories.addAll(old.getDropData());
			
			ClassId[] classIds = null;
			
			if (old.getTeachInfo() != null)
				classIds = old.getTeachInfo().clone();
			
			List<L2MinionData> minions = new FastList<L2MinionData>();
			
			if (old.getMinionData() != null)
				minions.addAll(old.getMinionData());
			
			// reload the NPC base data
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement st = con.prepareStatement("SELECT "
					+ L2DatabaseFactory.getInstance().safetyString(new String[] { "id", "idTemplate", "name", "serverSideName", "title",
							"serverSideTitle", "class", "collision_radius", "collision_height", "level", "sex", "type", "attackrange",
							"hp", "mp", "hpreg", "mpreg", "str", "con", "dex", "int", "wit", "men", "exp", "sp", "patk", "pdef", "matk",
							"mdef", "atkspd", "critical", "aggro", "matkspd", "rhand", "lhand", "enchant", "walkspd", "runspd",
							"dropHerbGroup" })
							+ " FROM npc WHERE id=?");
			st.setInt(1, id);
			ResultSet rs = st.executeQuery();
			fillNpcTable(rs, false);
			if (Config.CUSTOM_NPC_TABLE) // reload certain NPCs
			{
				st = con.prepareStatement("SELECT "
						+ L2DatabaseFactory.getInstance().safetyString(new String[] { "id", "idTemplate", "name", "serverSideName",
								"title", "serverSideTitle", "class", "collision_radius", "collision_height", "level", "sex", "type",
								"attackrange", "hp", "mp", "hpreg", "mpreg", "str", "con", "dex", "int", "wit", "men", "exp", "sp", "patk",
								"pdef", "matk", "mdef", "atkspd", "critical", "aggro", "matkspd", "rhand", "lhand", "enchant", "walkspd", "runspd",
								"dropHerbGroup" }) + " FROM custom_npc WHERE id=?");
				st.setInt(1, id);
				rs = st.executeQuery();
				fillNpcTable(rs, true);
			}
			rs.close();
			st.close();
			
			// restore additional data from saved copy
			L2NpcTemplate created = getTemplate(id);
			
			for (L2Skill skill : skills.getValues(new L2Skill[skills.size()]))
				created.addSkill(skill);
			
			if (classIds != null)
				for (ClassId classId : classIds)
					created.addTeachInfo(classId);
			
			for (L2MinionData minion : minions)
				created.addRaidData(minion);
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "NPCTable: Could not reload data for NPC " + id + ": " + e.getMessage(), e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
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
		for (Object npcTemplate : _npcs.getValues())
			if (((L2NpcTemplate)npcTemplate).name.equalsIgnoreCase(name))
				return (L2NpcTemplate) npcTemplate;
		
		return null;
	}
	
	public L2NpcTemplate[] getAllOfLevel(int lvl)
	{
		List<L2NpcTemplate> list = new FastList<L2NpcTemplate>();
		
		for (Object t : _npcs.getValues())
			if (((L2NpcTemplate)t).level == lvl)
				list.add((L2NpcTemplate) t);
		
		return list.toArray(new L2NpcTemplate[list.size()]);
	}
	
	public L2NpcTemplate[] getAllMonstersOfLevel(int lvl)
	{
		List<L2NpcTemplate> list = new FastList<L2NpcTemplate>();
		
		for (Object t : _npcs.getValues())
			if (((L2NpcTemplate)t).level == lvl && "L2Monster".equals(((L2NpcTemplate)t).type))
				list.add((L2NpcTemplate) t);
		
		return list.toArray(new L2NpcTemplate[list.size()]);
	}
	
	public L2NpcTemplate[] getAllNpcStartingWith(String letter)
	{
		List<L2NpcTemplate> list = new FastList<L2NpcTemplate>();
		
		for (Object t : _npcs.getValues())
			if (((L2NpcTemplate)t).name.startsWith(letter) && "L2Npc".equals(((L2NpcTemplate)t).type))
				list.add((L2NpcTemplate) t);
		
		return list.toArray(new L2NpcTemplate[list.size()]);
	}
	
	/**
	 * @param classType
	 * @return
	 */
	public L2NpcTemplate[] getAllNpcOfClassType(String classType)
	{
		List<L2NpcTemplate> list = new FastList<L2NpcTemplate>();
		
		for (Object t : _npcs.getValues())
			if (classType.equals(((L2NpcTemplate)t).type))
				list.add((L2NpcTemplate) t);
		
		return list.toArray(new L2NpcTemplate[list.size()]);
	}
	
	/**
	 * @param class1
	 * @return
	 */
	public Set<Integer> getAllNpcOfL2jClass(Class<?> clazz)
	{
		return null;
	}
	
	/**
	 * @param aiType
	 * @return
	 */
	public Set<Integer> getAllNpcOfAiType(String aiType)
	{
		return null;
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final NpcTable _instance = new NpcTable();
	}
}
