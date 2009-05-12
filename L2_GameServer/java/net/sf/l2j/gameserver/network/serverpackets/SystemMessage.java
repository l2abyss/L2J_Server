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
package net.sf.l2j.gameserver.network.serverpackets;

import java.util.Vector;

import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;
import net.sf.l2j.gameserver.templates.item.L2Item;

/**
 * This class ...
 *
 * @version $Revision: 1.18.2.5.2.8 $ $Date: 2005/04/05 19:41:08 $
 */
public final class SystemMessage extends L2GameServerPacket
{
	// d d (d S/d d/d dd)
	//      |--------------> 0 - String  1-number 2-textref npcname (1000000-1002655)  3-textref itemname 4-textref skills 5-??
	private static final int TYPE_ZONE_NAME = 7;
	private static final int TYPE_ITEM_NUMBER = 6;
	private static final int TYPE_FORTRESS = 5; // maybe not only for fortress, rename if needed
	private static final int TYPE_SKILL_NAME = 4;
	private static final int TYPE_ITEM_NAME = 3;
	private static final int TYPE_NPC_NAME = 2;
	private static final int TYPE_NUMBER = 1;
	private static final int TYPE_TEXT = 0;
	private static final String _S__7A_SYSTEMMESSAGE = "[S] 62 SystemMessage";
	private int _messageId;
	private Vector<Integer> _types = new Vector<Integer>();
	private Vector<Object> _values = new Vector<Object>();
	private int _skillLvL = 1;

	public SystemMessage(SystemMessageId messageId)
	{
		_messageId = messageId.getId();
	}

	/**
	 * Use SystemMessage(SystemMessageId messageId) where possible instead
	 */
	public SystemMessage(int messageId)
	{
		_messageId = messageId;
	}

 	public static SystemMessage sendString(String msg)
	{
 		SystemMessage sm = new SystemMessage(SystemMessageId.S1);
 		sm.addString(msg);

 		return sm;
	}

	public SystemMessage addString(String text)
	{
		_types.add(Integer.valueOf(TYPE_TEXT));
		_values.add(text);

		return this;
	}

	public SystemMessage addFortId(int number)
	{
		_types.add(Integer.valueOf(TYPE_FORTRESS));
		_values.add(Integer.valueOf(number));
		return this;
	}

	public SystemMessage addNumber(int number)
	{
		_types.add(Integer.valueOf(TYPE_NUMBER));
		_values.add(Integer.valueOf(number));
		return this;
	}

	public SystemMessage addItemNumber(long number)
	{
		_types.add(Integer.valueOf(TYPE_ITEM_NUMBER));
		_values.add(Long.valueOf(number));
		return this;
	}
	
	public SystemMessage addCharName(L2Character cha)
	{
		if (cha instanceof L2Npc)
		{
			if (((L2Npc)cha).getTemplate().serverSideName)
				return addString(((L2Npc)cha).getTemplate().name);
			else			
				return addNpcName((L2Npc)cha);
		}
		if (cha instanceof L2PcInstance)
			return addPcName((L2PcInstance)cha);
		if (cha instanceof L2Summon)
		{
			if (((L2Summon)cha).getTemplate().serverSideName)
				return addString(((L2Summon)cha).getTemplate().name);
			else			
				return addNpcName((L2Summon)cha);
		}
		return addString(cha.getName());
	}

	public SystemMessage addPcName(L2PcInstance pc)
	{
		return addString(pc.getAppearance().getVisibleName());
	}

	public SystemMessage addNpcName(L2Npc npc)
	{
		return addNpcName(npc.getTemplate());
	}

	public SystemMessage addNpcName(L2Summon npc)
	{
		return addNpcName(npc.getNpcId());
	}

	public SystemMessage addNpcName(L2NpcTemplate tpl)
	{
		if (tpl.isCustom())
			return addString(tpl.name);
		return addNpcName(tpl.npcId);
	}

	public SystemMessage addNpcName(int id)
	{
		_types.add(Integer.valueOf(TYPE_NPC_NAME));
		_values.add(Integer.valueOf(1000000 + id));

		return this;
	}

	public SystemMessage addItemName(L2ItemInstance item)
	{
		return addItemName(item.getItem().getItemId());
	}

	public SystemMessage addItemName(L2Item item)
	{
		// TODO: template id for items
		return addItemName(item.getItemId());
	}

	public SystemMessage addItemName(int id)
	{
		_types.add(Integer.valueOf(TYPE_ITEM_NAME));
		_values.add(Integer.valueOf(id));

		return this;
	}

	public SystemMessage addZoneName(int x, int y, int z)
	{
		_types.add(Integer.valueOf(TYPE_ZONE_NAME));
		int[] coord = {x, y, z};
		_values.add(coord);

		return this;
	}

	public SystemMessage addSkillName(L2Effect effect)
	{
		return addSkillName(effect.getSkill());
	}

	public SystemMessage addSkillName(L2Skill skill)
	{
		if (skill.getId() != skill.getDisplayId()) //custom skill -  need nameId or smth like this.
			return addString(skill.getName());
		return addSkillName(skill.getId(), skill.getLevel());
	}

	public SystemMessage addSkillName(int id)
	{
		return addSkillName(id, 1);
	}

	public SystemMessage addSkillName(int id, int lvl)
	{
		_types.add(Integer.valueOf(TYPE_SKILL_NAME));
		_values.add(Integer.valueOf(id));
		_skillLvL = lvl;

		return this;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x62);

		writeD(_messageId);
		writeD(_types.size());

		for (int i = 0; i < _types.size(); i++)
		{
			int t = _types.get(i).intValue();

			writeD(t);

			switch (t)
			{
				case TYPE_TEXT:
				{
					writeS( (String)_values.get(i));
					break;
				}
				case TYPE_ITEM_NUMBER:
				{
					long t1 = ((Long)_values.get(i)).longValue();
					writeQ(t1);
					break;
				}
				case TYPE_ITEM_NAME:
				case TYPE_FORTRESS:
				case TYPE_NUMBER:
				case TYPE_NPC_NAME:
				{
					int t1 = ((Integer)_values.get(i)).intValue();
					writeD(t1);
					break;
				}
				case TYPE_SKILL_NAME:
				{
					int t1 = ((Integer)_values.get(i)).intValue();
					writeD(t1); // Skill Id
					writeD(_skillLvL); // Skill lvl
					break;
				}
				case TYPE_ZONE_NAME:
				{
					int t1 = ((int[])_values.get(i))[0];
					int t2 = ((int[])_values.get(i))[1];
					int t3 = ((int[])_values.get(i))[2];
					writeD(t1);
					writeD(t2);
					writeD(t3);
					break;
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.serverpackets.ServerBasePacket#getType()
	 */
	@Override
	public String getType()
	{
		return _S__7A_SYSTEMMESSAGE;
	}

	public int getMessageID()
	{
		return _messageId;
	}
}



