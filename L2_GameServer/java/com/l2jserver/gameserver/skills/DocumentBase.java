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
package com.l2jserver.gameserver.skills;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.l2jserver.Config;
import com.l2jserver.gameserver.datatables.SkillTable;
import com.l2jserver.gameserver.model.ChanceCondition;
import com.l2jserver.gameserver.model.L2Skill;
import com.l2jserver.gameserver.model.base.PlayerState;
import com.l2jserver.gameserver.model.base.Race;
import com.l2jserver.gameserver.skills.conditions.*;
import com.l2jserver.gameserver.skills.conditions.ConditionGameTime.CheckGameTime;
import com.l2jserver.gameserver.skills.effects.EffectChanceSkillTrigger;
import com.l2jserver.gameserver.skills.funcs.FuncTemplate;
import com.l2jserver.gameserver.skills.funcs.Lambda;
import com.l2jserver.gameserver.skills.funcs.LambdaCalc;
import com.l2jserver.gameserver.skills.funcs.LambdaConst;
import com.l2jserver.gameserver.skills.funcs.LambdaStats;
import com.l2jserver.gameserver.templates.StatsSet;
import com.l2jserver.gameserver.templates.effects.EffectTemplate;
import com.l2jserver.gameserver.templates.item.L2ArmorType;
import com.l2jserver.gameserver.templates.item.L2Item;
import com.l2jserver.gameserver.templates.item.L2Weapon;
import com.l2jserver.gameserver.templates.item.L2WeaponType;
import com.l2jserver.gameserver.templates.skills.L2SkillType;

/**
 * @author mkizub
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
abstract class DocumentBase
{
    static Logger _log = Logger.getLogger(DocumentBase.class.getName());

    private File _file;
    protected Map<String, String[]> _tables;

    DocumentBase(File pFile)
    {
        _file = pFile;
        _tables = new FastMap<String, String[]>();
    }

    Document parse()
    {
        Document doc;
        try
        {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            factory.setIgnoringComments(true);
            doc = factory.newDocumentBuilder().parse(_file);
        }
        catch (Exception e)
        {
            _log.log(Level.SEVERE, "Error loading file " + _file, e);
            return null;
        }
        try
        {
            parseDocument(doc);
        }
        catch (Exception e)
        {
            _log.log(Level.SEVERE, "Error in file " + _file, e);
            return null;
        }
        return doc;
    }

    protected abstract void parseDocument(Document doc);

    protected abstract StatsSet getStatsSet();

    protected abstract String getTableValue(String name);

    protected abstract String getTableValue(String name, int idx);

    protected void resetTable()
    {
        _tables = new FastMap<String, String[]>();
    }

    protected void setTable(String name, String[] table)
    {
        _tables.put(name, table);
    }

    protected void parseTemplate(Node n, Object template)
    {
        Condition condition = null;
        n = n.getFirstChild();
        if (n == null) return;
        if ("cond".equalsIgnoreCase(n.getNodeName()))
        {
            condition = parseCondition(n.getFirstChild(), template);
			Node msg = n.getAttributes().getNamedItem("msg");
			Node msgId = n.getAttributes().getNamedItem("msgId");
			if (condition != null && msg != null)
				condition.setMessage(msg.getNodeValue());
			else if (condition != null && msgId != null)
			{
				condition.setMessageId(Integer.decode(getValue(msgId.getNodeValue(), null)));
				Node addName = n.getAttributes().getNamedItem("addName");
				if (addName != null && Integer.decode(getValue(msgId.getNodeValue(), null)) > 0)
					condition.addName();
			}
            n = n.getNextSibling();
        }
        for (; n != null; n = n.getNextSibling())
        {
            if ("add".equalsIgnoreCase(n.getNodeName())) attachFunc(n, template, "Add", condition);
            else if ("sub".equalsIgnoreCase(n.getNodeName())) attachFunc(n, template, "Sub", condition);
            else if ("mul".equalsIgnoreCase(n.getNodeName())) attachFunc(n, template, "Mul", condition);
            else if ("basemul".equalsIgnoreCase(n.getNodeName())) attachFunc(n, template, "BaseMul", condition);
            else if ("div".equalsIgnoreCase(n.getNodeName())) attachFunc(n, template, "Div", condition);
            else if ("set".equalsIgnoreCase(n.getNodeName())) attachFunc(n, template, "Set", condition);
            else if ("enchant".equalsIgnoreCase(n.getNodeName())) attachFunc(n, template, "Enchant", condition);
            else if ("enchanthp".equalsIgnoreCase(n.getNodeName())) attachFunc(n, template, "EnchantHp", condition);
            //else if ("skill".equalsIgnoreCase(n.getNodeName())) attachSkill(n, template, condition);
            else if ("effect".equalsIgnoreCase(n.getNodeName()))
            {
                if (template instanceof EffectTemplate) throw new RuntimeException("Nested effects");
                attachEffect(n, template, condition);
            }
        }
    }

    protected void attachFunc(Node n, Object template, String name, Condition attachCond)
    {
        Stats stat = Stats.valueOfXml(n.getAttributes().getNamedItem("stat").getNodeValue());
        String order = n.getAttributes().getNamedItem("order").getNodeValue();
        Lambda lambda = getLambda(n, template);
        int ord = Integer.decode(getValue(order, template));
        Condition applayCond = parseCondition(n.getFirstChild(), template);
        FuncTemplate ft = new FuncTemplate(attachCond, applayCond, name, stat, ord, lambda);
        if (template instanceof L2Item) ((L2Item) template).attach(ft);
        else if (template instanceof L2Skill) ((L2Skill) template).attach(ft);
        else if (template instanceof EffectTemplate) ((EffectTemplate) template).attach(ft);
    }

    protected void attachLambdaFunc(Node n, Object template, LambdaCalc calc)
    {
        String name = n.getNodeName();
        final StringBuilder sb = new StringBuilder(name);
        sb.setCharAt(0, Character.toUpperCase(name.charAt(0)));
        name = sb.toString();
        Lambda lambda = getLambda(n, template);
        FuncTemplate ft = new FuncTemplate(null, null, name, null, calc.funcs.length, lambda);
        calc.addFunc(ft.getFunc(new Env(), calc));
    }

    protected void attachEffect(Node n, Object template, Condition attachCond)
    {
        NamedNodeMap attrs = n.getAttributes();
        String name = attrs.getNamedItem("name").getNodeValue().intern();
        
        /**
         * Keep this values as default ones, DP needs it
         */
        int time = 1;
        int count = 1;
        
        if (attrs.getNamedItem("count") != null)
        {
            count = Integer.decode(getValue(attrs.getNamedItem("count").getNodeValue(), template));
        }
        if (attrs.getNamedItem("time") != null)
        {
        	time = Integer.decode(getValue(attrs.getNamedItem("time").getNodeValue(),template));
            if (Config.ENABLE_MODIFY_SKILL_DURATION)
            {
                if (Config.SKILL_DURATION_LIST.containsKey(((L2Skill) template).getId()))
                {
                    if (((L2Skill) template).getLevel() < 100)
                        time = Config.SKILL_DURATION_LIST.get(((L2Skill) template).getId());
                    else if ((((L2Skill) template).getLevel() >= 100) && (((L2Skill) template).getLevel() < 140))
                        time += Config.SKILL_DURATION_LIST.get(((L2Skill) template).getId());
                    else if (((L2Skill) template).getLevel() > 140)
                        time = Config.SKILL_DURATION_LIST.get(((L2Skill) template).getId());
                    if (Config.DEBUG)
                        _log.info("*** Skill " + ((L2Skill) template).getName() + " (" + ((L2Skill) template).getLevel() + ") changed duration to " + time + " seconds.");
                }
            }
        }
        else if (((L2Skill) template).getBuffDuration() > 0)
        	time = ((L2Skill) template).getBuffDuration() / 1000 / count;
        
        	boolean self = false;
        if (attrs.getNamedItem("self") != null)
        {
            if (Integer.decode(getValue(attrs.getNamedItem("self").getNodeValue(),template)) == 1)
                self = true;
        }
        boolean icon = true;
        if (attrs.getNamedItem("noicon") !=null)
        {
        	if (Integer.decode(getValue(attrs.getNamedItem("noicon").getNodeValue(),template)) == 1)
        		icon = false;        		
        }
        Lambda lambda = getLambda(n, template);
        Condition applayCond = parseCondition(n.getFirstChild(), template);
        AbnormalEffect abnormal = AbnormalEffect.NULL;
        if (attrs.getNamedItem("abnormal") != null)
        {
            String abn = attrs.getNamedItem("abnormal").getNodeValue();            
            abnormal = AbnormalEffect.getByName(abn);
        }
        AbnormalEffect special = AbnormalEffect.NULL;
        if (attrs.getNamedItem("special") != null)
        {
            String spc = attrs.getNamedItem("special").getNodeValue();
            special = AbnormalEffect.getByName(spc);
        }
        float stackOrder = 0;
        String stackType = "none";
        if (attrs.getNamedItem("stackType") != null)
        {
            stackType = attrs.getNamedItem("stackType").getNodeValue();
        }
        if (attrs.getNamedItem("stackOrder") != null)
        {
            stackOrder = Float.parseFloat(getValue(attrs.getNamedItem("stackOrder").getNodeValue(), template));
        }
        
        double effectPower = -1;
        if (attrs.getNamedItem("effectPower") != null)
	        effectPower = Double.parseDouble( getValue(attrs.getNamedItem("effectPower").getNodeValue(), template));
        
        L2SkillType type = null;
        if (attrs.getNamedItem("effectType") != null)
        {
        	String typeName = getValue(attrs.getNamedItem("effectType").getNodeValue(), template);
        	
        	try
        	{
    			type = Enum.valueOf(L2SkillType.class, typeName);
    		} 
        	catch (Exception e)
        	{
    			throw new IllegalArgumentException("Not skilltype found for: "+typeName);
    		}
        }
        
        if (effectPower > -1 && type == null && _log.isLoggable(Level.WARNING))
        	_log.log(Level.WARNING, "Missing effectType for effect: "+name);
        
        EffectTemplate lt;
        
        final boolean isChanceSkillTrigger = (name == EffectChanceSkillTrigger.class.getName());
        int trigId = 0;
		if (attrs.getNamedItem("triggeredId") != null)
			trigId = Integer.parseInt(getValue(attrs.getNamedItem("triggeredId").getNodeValue(), template));
		else if (isChanceSkillTrigger)
			throw new NoSuchElementException(name + " requires triggerId");
		
		int trigLvl = 1;
		if (attrs.getNamedItem("triggeredLevel") != null)
			trigLvl = Integer.parseInt(getValue(attrs.getNamedItem("triggeredLevel").getNodeValue(), template));
		
		String chanceCond = null;
		if (attrs.getNamedItem("chanceType") != null)
			chanceCond = getValue(attrs.getNamedItem("chanceType").getNodeValue(), template);
		else if (isChanceSkillTrigger)
			throw new NoSuchElementException(name + " requires chanceType");
		
		int activationChance = -1;
		if (attrs.getNamedItem("activationChance") != null)
			activationChance = Integer.parseInt(getValue(attrs.getNamedItem("activationChance").getNodeValue(), template));
		String activationElements = null;
		if (attrs.getNamedItem("activationElements") != null)
			activationElements = getValue(attrs.getNamedItem("activationElements").getNodeValue(), template);
		boolean pvpOnly = false;
		if (attrs.getNamedItem("pvpChanceOnly") != null)
			pvpOnly = Boolean.parseBoolean(getValue(attrs.getNamedItem("pvpChanceOnly").getNodeValue(), template));
		
		ChanceCondition chance = ChanceCondition.parse(chanceCond, activationChance, activationElements, pvpOnly);
		
		if (chance == null && isChanceSkillTrigger)
			throw new NoSuchElementException("Invalid chance condition: " + chanceCond + " "
			        + activationChance);        	
        	
        lt = new EffectTemplate(attachCond, applayCond, name, lambda, count, time, abnormal, special, stackType, stackOrder, icon, effectPower, type, trigId, trigLvl, chance);
		parseTemplate(n, lt);
		if (template instanceof L2Item)
			((L2Item) template).attach(lt);
		else if (template instanceof L2Skill)
		{
			if (self)
				((L2Skill) template).attachSelf(lt);
			else
				((L2Skill) template).attach(lt);
		}
    }

    protected void attachSkill(Node n, Object template, Condition attachCond)
    {
        NamedNodeMap attrs = n.getAttributes();
        int id = 0, lvl = 1;
        if (attrs.getNamedItem("id") != null)
        {
            id = Integer.decode(getValue(attrs.getNamedItem("id").getNodeValue(), template));
        }
        if (attrs.getNamedItem("lvl") != null)
        {
            lvl = Integer.decode(getValue(attrs.getNamedItem("lvl").getNodeValue(), template));
        }
        L2Skill skill = SkillTable.getInstance().getInfo(id, lvl);
        if (attrs.getNamedItem("chance") != null)
        {
        	if (template instanceof L2Weapon || template instanceof L2Item)
        	{
        		skill.attach(new ConditionGameChance(Integer.decode(getValue(attrs.getNamedItem("chance").getNodeValue(), template))), true);
        	}
        	else
        	{
        		skill.attach(new ConditionGameChance(Integer.decode(getValue(attrs.getNamedItem("chance").getNodeValue(), template))), false);
        	}
        }
        if (template instanceof L2Weapon)
        {
            if (attrs.getNamedItem("onUse") != null
                || (attrs.getNamedItem("onCrit") == null && attrs.getNamedItem("onCast") == null))
                ((L2Weapon) template).attach(skill); // Attach as skill triggered on use
            //if (attrs.getNamedItem("onCrit") != null) ((L2Weapon) template).attachOnCrit(skill); // Attach as skill triggered on critical hit
            //if (attrs.getNamedItem("onCast") != null) ((L2Weapon) template).attachOnCast(skill); // Attach as skill triggered on cast
        }
        else if (template instanceof L2Item)
        {
            ((L2Item) template).attach(skill); // Attach as skill triggered on use
        }
    }

    protected Condition parseCondition(Node n, Object template)
    {
        while (n != null && n.getNodeType() != Node.ELEMENT_NODE)
            n = n.getNextSibling();
        if (n == null) return null;
        if ("and".equalsIgnoreCase(n.getNodeName())) return parseLogicAnd(n, template);
        if ("or".equalsIgnoreCase(n.getNodeName())) return parseLogicOr(n, template);
        if ("not".equalsIgnoreCase(n.getNodeName())) return parseLogicNot(n, template);
        if ("player".equalsIgnoreCase(n.getNodeName())) return parsePlayerCondition(n, template);
        if ("target".equalsIgnoreCase(n.getNodeName())) return parseTargetCondition(n, template);
        if ("skill".equalsIgnoreCase(n.getNodeName())) return parseSkillCondition(n);
        if ("using".equalsIgnoreCase(n.getNodeName())) return parseUsingCondition(n);
        if ("game".equalsIgnoreCase(n.getNodeName())) return parseGameCondition(n);
        return null;
    }

    protected Condition parseLogicAnd(Node n, Object template)
    {
        ConditionLogicAnd cond = new ConditionLogicAnd();
        for (n = n.getFirstChild(); n != null; n = n.getNextSibling())
        {
            if (n.getNodeType() == Node.ELEMENT_NODE) cond.add(parseCondition(n, template));
        }
        if (cond.conditions == null || cond.conditions.length == 0)
            _log.severe("Empty <and> condition in " + _file);
        return cond;
    }

    protected Condition parseLogicOr(Node n, Object template)
    {
        ConditionLogicOr cond = new ConditionLogicOr();
        for (n = n.getFirstChild(); n != null; n = n.getNextSibling())
        {
            if (n.getNodeType() == Node.ELEMENT_NODE) cond.add(parseCondition(n, template));
        }
        if (cond.conditions == null || cond.conditions.length == 0)
            _log.severe("Empty <or> condition in " + _file);
        return cond;
    }

    protected Condition parseLogicNot(Node n, Object template)
    {
        for (n = n.getFirstChild(); n != null; n = n.getNextSibling())
        {
            if (n.getNodeType() == Node.ELEMENT_NODE)
            {
                return new ConditionLogicNot(parseCondition(n, template));
            }
        }
        _log.severe("Empty <not> condition in " + _file);
        return null;
    }

    protected Condition parsePlayerCondition(Node n, Object template)
    {
        Condition cond = null;
        byte[] forces = new byte[2];
        NamedNodeMap attrs = n.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++)
        {
            Node a = attrs.item(i);
            if ("race".equalsIgnoreCase(a.getNodeName()))
            {
                Race race = Race.valueOf(a.getNodeValue());
                cond = joinAnd(cond, new ConditionPlayerRace(race));
            }
            else if ("level".equalsIgnoreCase(a.getNodeName()))
            {
                int lvl = Integer.decode(getValue(a.getNodeValue(), template));
                cond = joinAnd(cond, new ConditionPlayerLevel(lvl));
            }
            else if ("resting".equalsIgnoreCase(a.getNodeName()))
            {
                boolean val = Boolean.valueOf(a.getNodeValue());
                cond = joinAnd(cond, new ConditionPlayerState(PlayerState.RESTING, val));
            }
            else if ("flying".equalsIgnoreCase(a.getNodeName()))
            {
                boolean val = Boolean.valueOf(a.getNodeValue());
                cond = joinAnd(cond, new ConditionPlayerState(PlayerState.FLYING, val));
            }
            else if ("moving".equalsIgnoreCase(a.getNodeName()))
            {
                boolean val = Boolean.valueOf(a.getNodeValue());
                cond = joinAnd(cond, new ConditionPlayerState(PlayerState.MOVING, val));
            }
            else if ("running".equalsIgnoreCase(a.getNodeName()))
            {
                boolean val = Boolean.valueOf(a.getNodeValue());
                cond = joinAnd(cond, new ConditionPlayerState(PlayerState.RUNNING, val));
            }
            else if ("behind".equalsIgnoreCase(a.getNodeName()))
            {
                boolean val = Boolean.valueOf(a.getNodeValue());
                cond = joinAnd(cond, new ConditionPlayerState(PlayerState.BEHIND, val));
            }
            else if ("front".equalsIgnoreCase(a.getNodeName()))
            {
                boolean val = Boolean.valueOf(a.getNodeValue());
                cond = joinAnd(cond, new ConditionPlayerState(PlayerState.FRONT, val));
            }
            else if ("chaotic".equalsIgnoreCase(a.getNodeName()))
            {
                boolean val = Boolean.valueOf(a.getNodeValue());
                cond = joinAnd(cond, new ConditionPlayerState(PlayerState.CHAOTIC, val));
            }
            else if ("olympiad".equalsIgnoreCase(a.getNodeName()))
            {
                boolean val = Boolean.valueOf(a.getNodeValue());
                cond = joinAnd(cond, new ConditionPlayerState(PlayerState.OLYMPIAD, val));
            }
            else if ("ishero".equalsIgnoreCase(a.getNodeName()))
            {
                boolean val = Boolean.valueOf(a.getNodeValue());
                cond = joinAnd(cond, new ConditionPlayerIsHero(val));
            }
            else if ("hp".equalsIgnoreCase(a.getNodeName()))
            {
                int hp = Integer.decode(getValue(a.getNodeValue(), null));
                cond = joinAnd(cond, new ConditionPlayerHp(hp));
            }
            else if ("mp".equalsIgnoreCase(a.getNodeName()))
            {
                int hp = Integer.decode(getValue(a.getNodeValue(), null));
                cond = joinAnd(cond, new ConditionPlayerMp(hp));
            }
            else if ("cp".equalsIgnoreCase(a.getNodeName()))
            {
            	int cp = Integer.decode(getValue(a.getNodeValue(), null));
            	cond = joinAnd(cond, new ConditionPlayerCp(cp));
            }
            else if ("grade".equalsIgnoreCase(a.getNodeName()))
            {
            	int expIndex = Integer.decode(getValue(a.getNodeValue(), template));
            	cond = joinAnd(cond, new ConditionPlayerGrade(expIndex));
            }
            else if ("pkCount".equalsIgnoreCase(a.getNodeName()))
            {
            	int expIndex = Integer.decode(getValue(a.getNodeValue(), template));
            	cond = joinAnd(cond, new ConditionPlayerPkCount(expIndex));
            }
            else if ("siegezone".equalsIgnoreCase(a.getNodeName()))
            {
                int value = Integer.decode(getValue(a.getNodeValue(), null));
                cond = joinAnd(cond, new ConditionSiegeZone(value, true));
            }
            else if ("battle_force".equalsIgnoreCase(a.getNodeName()))
            {
                forces[0] = Byte.decode(getValue(a.getNodeValue(), null));
            }
            else if ("spell_force".equalsIgnoreCase(a.getNodeName()))
            {
                forces[1] = Byte.decode(getValue(a.getNodeValue(), null));
            }
            else if ("charges".equalsIgnoreCase(a.getNodeName()))
            {
                int value = Integer.decode(getValue(a.getNodeValue(), template));
                cond = joinAnd(cond, new ConditionPlayerCharges(value));
            }
            else if ("souls".equalsIgnoreCase(a.getNodeName()))
            {
                int value = Integer.decode(getValue(a.getNodeValue(), template));
                cond = joinAnd(cond, new ConditionPlayerSouls(value));
            }
            else if ("weight".equalsIgnoreCase(a.getNodeName()))
            {
                int weight = Integer.decode(getValue(a.getNodeValue(), null));
                cond = joinAnd(cond, new ConditionPlayerWeight(weight));
            }
            else if ("invSize".equalsIgnoreCase(a.getNodeName()))
            {
                int size = Integer.decode(getValue(a.getNodeValue(), null));
                cond = joinAnd(cond, new ConditionPlayerInvSize(size));
            }
            else if ("isClanLeader".equalsIgnoreCase(a.getNodeName()))
            {
            	boolean val = Boolean.valueOf(a.getNodeValue());
                cond = joinAnd(cond, new ConditionPlayerIsClanLeader(val));
            }
            else if ("onTvTEvent".equalsIgnoreCase(a.getNodeName()))
            {
            	boolean val = Boolean.valueOf(a.getNodeValue());
                cond = joinAnd(cond, new ConditionPlayerTvTEvent(val));
            }
            else if ("pledgeClass".equalsIgnoreCase(a.getNodeName()))
            {
                int pledgeClass = Integer.decode(getValue(a.getNodeValue(), null));
                cond = joinAnd(cond, new ConditionPlayerPledgeClass(pledgeClass));
            }
            else if ("clanHall".equalsIgnoreCase(a.getNodeName()))
            {
            	FastList<Integer> array = new FastList<Integer>();
            	StringTokenizer st = new StringTokenizer(a.getNodeValue(), ",");
            	while (st.hasMoreTokens())
                {
                    String item = st.nextToken().trim();
                    array.add(Integer.decode(getValue(item, null)));
                }
                cond = joinAnd(cond, new ConditionPlayerHasClanHall(array));
            }
            else if ("fort".equalsIgnoreCase(a.getNodeName()))
            {
                int fort = Integer.decode(getValue(a.getNodeValue(), null));
                cond = joinAnd(cond, new ConditionPlayerHasFort(fort));
            }
            else if ("castle".equalsIgnoreCase(a.getNodeName()))
            {
                int castle = Integer.decode(getValue(a.getNodeValue(), null));
                cond = joinAnd(cond, new ConditionPlayerHasCastle(castle));
            }
            else if ("sex".equalsIgnoreCase(a.getNodeName()))
            {
                int sex = Integer.decode(getValue(a.getNodeValue(), null));
                cond = joinAnd(cond, new ConditionPlayerSex(sex));
            }
            else if ("flyMounted".equalsIgnoreCase(a.getNodeName()))
            {
            	boolean val = Boolean.valueOf(a.getNodeValue());
                cond = joinAnd(cond, new ConditionPlayerFlyMounted(val));
            }
            else if ("landingZone".equalsIgnoreCase(a.getNodeName()))
            {
            	boolean val = Boolean.valueOf(a.getNodeValue());
                cond = joinAnd(cond, new ConditionPlayerLandingZone(val));
            }
            else if ("active_effect_id".equalsIgnoreCase(a.getNodeName()))
            {
            	int effect_id = Integer.decode(getValue(a.getNodeValue(), template));
                cond = joinAnd(cond, new ConditionPlayerActiveEffectId(effect_id));
            }
            else if ("active_effect_id_lvl".equalsIgnoreCase(a.getNodeName()))
            {
            	String val = getValue(a.getNodeValue(), template);
            	int effect_id = Integer.decode(getValue(val.split(",")[0], template));
        		int effect_lvl = Integer.decode(getValue(val.split(",")[1], template));
        		cond = joinAnd(cond, new ConditionPlayerActiveEffectId(effect_id, effect_lvl));
            }
            else if ("active_skill_id".equalsIgnoreCase(a.getNodeName()))
            {
                int skill_id = Integer.decode(getValue(a.getNodeValue(), template));
                cond = joinAnd(cond, new ConditionPlayerActiveSkillId(skill_id));
            }
            else if ("active_skill_id_lvl".equalsIgnoreCase(a.getNodeName()))
            {
            	String val = getValue(a.getNodeValue(), template);
            	int skill_id = Integer.decode(getValue(val.split(",")[0], template));
        		int skill_lvl = Integer.decode(getValue(val.split(",")[1], template));
                cond = joinAnd(cond, new ConditionPlayerActiveSkillId(skill_id, skill_lvl));
            }
            else if ("class_id_restriction".equalsIgnoreCase(a.getNodeName()))
            {
            	FastList<Integer> array = new FastList<Integer>();
            	StringTokenizer st = new StringTokenizer(a.getNodeValue(), ",");
            	while (st.hasMoreTokens())
                {
                    String item = st.nextToken().trim();
                    array.add(Integer.decode(getValue(item, null)));
                }
            	cond = joinAnd(cond, new ConditionPlayerClassIdRestriction(array));
            }
			else if ("subclass".equalsIgnoreCase(a.getNodeName()))
			{
				boolean val = Boolean.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionPlayerSubclass(val));
			}
			else if ("instanceid".equalsIgnoreCase(a.getNodeName()))
			{
            	FastList<Integer> array = new FastList<Integer>();
            	StringTokenizer st = new StringTokenizer(a.getNodeValue(), ",");
            	while (st.hasMoreTokens())
                {
                    String item = st.nextToken().trim();
                    array.add(Integer.decode(getValue(item, null)));
                }
            	cond = joinAnd(cond, new ConditionPlayerInstanceId(array));
			}
			else if ("agathionId".equalsIgnoreCase(a.getNodeName()))
			{
				int agathionId = Integer.decode(a.getNodeValue());
				cond = joinAnd(cond, new ConditionPlayerAgathionId(agathionId));
			}
			else if ("cloakStatus".equalsIgnoreCase(a.getNodeName()))
			{
				int val = Integer.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionPlayerCloakStatus(val));
			}
        }

        if (forces[0] + forces[1] > 0)
            cond = joinAnd(cond, new ConditionForceBuff(forces));

        if (cond == null) _log.severe("Unrecognized <player> condition in " + _file);
        return cond;
    }

    protected Condition parseTargetCondition(Node n, Object template)
    {
        Condition cond = null;
        NamedNodeMap attrs = n.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++)
        {
            Node a = attrs.item(i);
            if ("aggro".equalsIgnoreCase(a.getNodeName()))
            {
                boolean val = Boolean.valueOf(a.getNodeValue());
                cond = joinAnd(cond, new ConditionTargetAggro(val));
            }
            else if ("siegezone".equalsIgnoreCase(a.getNodeName()))
            {
                int value = Integer.decode(getValue(a.getNodeValue(), null));
                cond = joinAnd(cond, new ConditionSiegeZone(value, false));
            }
            else if ("level".equalsIgnoreCase(a.getNodeName()))
            {
                int lvl = Integer.decode(getValue(a.getNodeValue(), template));
                cond = joinAnd(cond, new ConditionTargetLevel(lvl));
            }
            else if ("class_id_restriction".equalsIgnoreCase(a.getNodeName()))
            {
            	FastList<Integer> array = new FastList<Integer>();
            	StringTokenizer st = new StringTokenizer(a.getNodeValue(), ",");
            	while (st.hasMoreTokens())
                {
                    String item = st.nextToken().trim();
                    array.add(Integer.decode(getValue(item, null)));
                }
            	cond = joinAnd(cond, new ConditionTargetClassIdRestriction(array));
            }
            else if ("active_effect_id".equalsIgnoreCase(a.getNodeName()))
            {
            	int effect_id = Integer.decode(getValue(a.getNodeValue(), template));
                cond = joinAnd(cond, new ConditionTargetActiveEffectId(effect_id));
            }
            else if ("active_effect_id_lvl".equalsIgnoreCase(a.getNodeName()))
            {
            	String val = getValue(a.getNodeValue(), template);
            	int effect_id = Integer.decode(getValue(val.split(",")[0], template));
        		int effect_lvl = Integer.decode(getValue(val.split(",")[1], template));
        		cond = joinAnd(cond, new ConditionTargetActiveEffectId(effect_id, effect_lvl));
            }
            else if ("active_skill_id".equalsIgnoreCase(a.getNodeName()))
            {
                int skill_id = Integer.decode(getValue(a.getNodeValue(), template));
                cond = joinAnd(cond, new ConditionTargetActiveSkillId(skill_id));
            }
			else if ("active_skill_id_lvl".equalsIgnoreCase(a.getNodeName()))
			{
				String val = getValue(a.getNodeValue(), template);
				int skill_id = Integer.decode(getValue(val.split(",")[0], template));
				int skill_lvl = Integer.decode(getValue(val.split(",")[1], template));
				cond = joinAnd(cond, new ConditionTargetActiveSkillId(skill_id, skill_lvl));
			}
			else if ("abnormal".equalsIgnoreCase(a.getNodeName()))
			{
				int abnormalId = Integer.decode(getValue(a.getNodeValue(), template));
				cond = joinAnd(cond, new ConditionTargetAbnormal(abnormalId));
			}
            else if("mindistance".equalsIgnoreCase(a.getNodeName()))
            {
            	int distance = Integer.decode(getValue(a.getNodeValue(),null));
            	cond = joinAnd(cond, new ConditionMinDistance(distance*distance));
            }
            // used for npc race
            else if ("race_id".equalsIgnoreCase(a.getNodeName()))
            {
            	ArrayList<Integer> array = new ArrayList<Integer>();
            	StringTokenizer st = new StringTokenizer(a.getNodeValue(), ",");
            	while (st.hasMoreTokens())
            	{
            		String item = st.nextToken().trim();
            		array.add(Integer.decode(getValue(item, null)));
            	}
            	cond = joinAnd(cond, new ConditionTargetRaceId(array));
            }
            // used for pc race
            else if ("race".equalsIgnoreCase(a.getNodeName()))
            {
                Race race = Race.valueOf(a.getNodeValue());
                cond = joinAnd(cond, new ConditionTargetRace(race));
            }
            else if ("using".equalsIgnoreCase(a.getNodeName()))
            {
                int mask = 0;
                StringTokenizer st = new StringTokenizer(a.getNodeValue(), ",");
                while (st.hasMoreTokens())
                {
                    String item = st.nextToken().trim();
                    for (L2WeaponType wt : L2WeaponType.values())
                    {
                        if (wt.toString().equals(item))
                        {
                            mask |= wt.mask();
                            break;
                        }
                    }
                    for (L2ArmorType at : L2ArmorType.values())
                    {
                        if (at.toString().equals(item))
                        {
                            mask |= at.mask();
                            break;
                        }
                    }
                }
                cond = joinAnd(cond, new ConditionTargetUsesWeaponKind(mask));
            }
            else if ("npcId".equalsIgnoreCase(a.getNodeName()))
            {
            	ArrayList<Integer> array = new ArrayList<Integer>();
            	StringTokenizer st = new StringTokenizer(a.getNodeValue(), ",");
            	while (st.hasMoreTokens())
            	{
            		String item = st.nextToken().trim();
            		array.add(Integer.decode(getValue(item, null)));
            	}
            	cond = joinAnd(cond, new ConditionTargetNpcId(array));
            }
            else if ("npcType".equalsIgnoreCase(a.getNodeName()))
            {
            	String values = getValue(a.getNodeValue(), template).trim();
                String[] valuesSplit = values.split(" ");
                cond = joinAnd(cond, new ConditionTargetNpcType(valuesSplit));
            }
        }
        if (cond == null) _log.severe("Unrecognized <target> condition in " + _file);
        return cond;
    }

    protected Condition parseSkillCondition(Node n)
    {
        NamedNodeMap attrs = n.getAttributes();
        Stats stat = Stats.valueOfXml(attrs.getNamedItem("stat").getNodeValue());
        return new ConditionSkillStats(stat);
    }

    protected Condition parseUsingCondition(Node n)
    {
        Condition cond = null;
        NamedNodeMap attrs = n.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++)
        {
            Node a = attrs.item(i);
            if ("kind".equalsIgnoreCase(a.getNodeName()))
            {
                int mask = 0;
                StringTokenizer st = new StringTokenizer(a.getNodeValue(), ",");
                while (st.hasMoreTokens())
                {
                    String item = st.nextToken().trim();
                    for (L2WeaponType wt : L2WeaponType.values())
                    {
                        if (wt.toString().equals(item))
                        {
                            mask |= wt.mask();
                            break;
                        }
                    }
                    for (L2ArmorType at : L2ArmorType.values())
                    {
                        if (at.toString().equals(item))
                        {
                            mask |= at.mask();
                            break;
                        }
                    }
                }
                cond = joinAnd(cond, new ConditionUsingItemType(mask));
            }
            else if ("skill".equalsIgnoreCase(a.getNodeName()))
            {
                int id = Integer.parseInt(a.getNodeValue());
                cond = joinAnd(cond, new ConditionUsingSkill(id));
            }
            else if ("slotitem".equalsIgnoreCase(a.getNodeName()))
            {
                StringTokenizer st = new StringTokenizer(a.getNodeValue(), ";");
                int id = Integer.parseInt(st.nextToken().trim());
                int slot = Integer.parseInt(st.nextToken().trim());
                int enchant = 0;
                if (st.hasMoreTokens()) enchant = Integer.parseInt(st.nextToken().trim());
                cond = joinAnd(cond, new ConditionSlotItemId(slot, id, enchant));
            }
            else if ("weaponChange".equalsIgnoreCase(a.getNodeName()))
            {
                boolean val = Boolean.valueOf(a.getNodeValue());
                cond = joinAnd(cond, new ConditionChangeWeapon(val));
            }
        }
        if (cond == null) _log.severe("Unrecognized <using> condition in " + _file);
        return cond;
    }

    protected Condition parseGameCondition(Node n)
    {
        Condition cond = null;
        NamedNodeMap attrs = n.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++)
        {
            Node a = attrs.item(i);
            if ("skill".equalsIgnoreCase(a.getNodeName()))
            {
                boolean val = Boolean.valueOf(a.getNodeValue());
                cond = joinAnd(cond, new ConditionWithSkill(val));
            }
            if ("night".equalsIgnoreCase(a.getNodeName()))
            {
                boolean val = Boolean.valueOf(a.getNodeValue());
                cond = joinAnd(cond, new ConditionGameTime(CheckGameTime.NIGHT, val));
            }
            if ("chance".equalsIgnoreCase(a.getNodeName()))
            {
                int val = Integer.decode(getValue(a.getNodeValue(), null));
                cond = joinAnd(cond, new ConditionGameChance(val));
            }
        }
        if (cond == null) _log.severe("Unrecognized <game> condition in " + _file);
        return cond;
    }

    protected void parseTable(Node n)
    {
        NamedNodeMap attrs = n.getAttributes();
        String name = attrs.getNamedItem("name").getNodeValue();
        if (name.charAt(0) != '#') throw new IllegalArgumentException("Table name must start with #");
        StringTokenizer data = new StringTokenizer(n.getFirstChild().getNodeValue());
        List<String> array = new FastList<String>();
        while (data.hasMoreTokens())
            array.add(data.nextToken());
        String[] res = new String[array.size()];
        int i = 0;
        for (String str : array)
        {
            res[i++] = str;
        }
        setTable(name, res);
    }

    protected void parseBeanSet(Node n, StatsSet set, Integer level)
    {
        String name = n.getAttributes().getNamedItem("name").getNodeValue().trim();
        String value = n.getAttributes().getNamedItem("val").getNodeValue().trim();
        char ch = value.length() == 0 ? ' ' : value.charAt(0);
        if (ch == '#' || ch == '-' || Character.isDigit(ch)) set.set(name,
                                                                     String.valueOf(getValue(value,
                                                                                              level)));
        else set.set(name, value);
    }

    protected Lambda getLambda(Node n, Object template)
    {
        Node nval = n.getAttributes().getNamedItem("val");
        if (nval != null)
        {
            String val = nval.getNodeValue();
            if (val.charAt(0) == '#')
            { // table by level
                return new LambdaConst(Double.parseDouble(getTableValue(val)));
            }
            else if (val.charAt(0) == '$')
            {
                if (val.equalsIgnoreCase("$player_level"))
                    return new LambdaStats(LambdaStats.StatsType.PLAYER_LEVEL);
                if (val.equalsIgnoreCase("$target_level"))
                    return new LambdaStats(LambdaStats.StatsType.TARGET_LEVEL);
                if (val.equalsIgnoreCase("$player_max_hp"))
                    return new LambdaStats(LambdaStats.StatsType.PLAYER_MAX_HP);
                if (val.equalsIgnoreCase("$player_max_mp"))
                    return new LambdaStats(LambdaStats.StatsType.PLAYER_MAX_MP);
                // try to find value out of item fields
                StatsSet set = getStatsSet();
                String field = set.getString(val.substring(1));
                if (field != null)
                {
                	return new LambdaConst(Double.parseDouble(getValue(field, template)));
                }
                // failed
                throw new IllegalArgumentException("Unknown value " + val);
            }
            else
            {
                return new LambdaConst(Double.parseDouble(val));
            }
        }
        LambdaCalc calc = new LambdaCalc();
        n = n.getFirstChild();
        while (n != null && n.getNodeType() != Node.ELEMENT_NODE)
            n = n.getNextSibling();
        if (n == null || !"val".equals(n.getNodeName()))
            throw new IllegalArgumentException("Value not specified");

        for (n = n.getFirstChild(); n != null; n = n.getNextSibling())
        {
            if (n.getNodeType() != Node.ELEMENT_NODE) continue;
            attachLambdaFunc(n, template, calc);
        }
        return calc;
    }

    protected String getValue(String value, Object template)
    {
    	// is it a table?
        if (value.charAt(0) == '#')
        {
            if (template instanceof L2Skill) return getTableValue(value);
            else if (template instanceof Integer) return getTableValue(value, ((Integer) template).intValue());
            else throw new IllegalStateException();
        }
        return value;
    }

    protected Condition joinAnd(Condition cond, Condition c)
    {
        if (cond == null) return c;
        if (cond instanceof ConditionLogicAnd)
        {
            ((ConditionLogicAnd) cond).add(c);
            return cond;
        }
        ConditionLogicAnd and = new ConditionLogicAnd();
        and.add(cond);
        and.add(c);
        return and;
    }
}
