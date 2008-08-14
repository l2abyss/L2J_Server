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

import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.templates.L2Item;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;

/**
 * @author kombat
 * Format: cd d[d s/d/dd/ddd]
 */
public class ConfirmDlg extends L2GameServerPacket
{
	private static final String _S__ED_CONFIRMDLG = "[S] f3 ConfirmDlg";
	private int _messageId;

	private int _skillLvL = 1;

	private static final int TYPE_ZONE_NAME = 7;
	private static final int TYPE_SKILL_NAME = 4;
	private static final int TYPE_ITEM_NAME = 3;
	private static final int TYPE_NPC_NAME = 2;
	private static final int TYPE_NUMBER = 1;
	private static final int TYPE_TEXT = 0;

	private Vector<Integer> _types = new Vector<Integer>();
	private Vector<Object> _values = new Vector<Object>();


	public ConfirmDlg(int messageId)
	{
		_messageId = messageId;
	}

	public ConfirmDlg addString(String text)
	{
		_types.add(new Integer(TYPE_TEXT));
		_values.add(text);
		return this;
	}

	public ConfirmDlg addNumber(int number)
	{
		_types.add(new Integer(TYPE_NUMBER));
		_values.add(new Integer(number));
		return this;
	}

	public ConfirmDlg addCharName(L2Character cha)
	{
		if (cha instanceof L2NpcInstance)
			return addNpcName((L2NpcInstance)cha);
		if (cha instanceof L2PcInstance)
			return addPcName((L2PcInstance)cha);
		if (cha instanceof L2Summon)
			return addNpcName((L2Summon)cha);
		return addString(cha.getName());
	}

	public ConfirmDlg addPcName(L2PcInstance pc)
	{
		return addString(pc.getAppearance().getVisibleName());
	}

	public ConfirmDlg addNpcName(L2NpcInstance npc)
	{
		return addNpcName(npc.getTemplate());
	}

	public ConfirmDlg addNpcName(L2Summon npc)
	{
		return addNpcName(npc.getNpcId());
	}

	public ConfirmDlg addNpcName(L2NpcTemplate tpl)
	{
		if (tpl.isCustom())
			return addString(tpl.name);
		return addNpcName(tpl.npcId);
	}

	public ConfirmDlg addNpcName(int id)
	{
		_types.add(new Integer(TYPE_NPC_NAME));
		_values.add(new Integer(1000000 + id));
		return this;
	}

	public ConfirmDlg addItemName(L2ItemInstance item)
	{
		return addItemName(item.getItem().getItemId());
	}

	public ConfirmDlg addItemName(L2Item item)
	{
		// TODO: template id for items
		return addItemName(item.getItemId());
	}

	public ConfirmDlg addItemName(int id)
	{
		_types.add(new Integer(TYPE_ITEM_NAME));
		_values.add(new Integer(id));
		return this;
	}

	public ConfirmDlg addZoneName(int x, int y, int z)
	{
		_types.add(new Integer(TYPE_ZONE_NAME));
		int[] coord = {x, y, z};
		_values.add(coord);
		return this;
	}

	public ConfirmDlg addSkillName(L2Effect effect)
	{
		return addSkillName(effect.getSkill());
	}

	public ConfirmDlg addSkillName(L2Skill skill)
	{
		if (skill.getId() != skill.getDisplayId()) //custom skill -  need nameId or smth like this.
			return addString(skill.getName());
		return addSkillName(skill.getId(), skill.getLevel());
	}

	public ConfirmDlg addSkillName(int id)
	{
		return addSkillName(id, 1);
	}

	public ConfirmDlg addSkillName(int id, int lvl)
	{
		_types.add(new Integer(TYPE_SKILL_NAME));
		_values.add(new Integer(id));
		_skillLvL = lvl;
		return this;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xf3);
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
				case TYPE_NUMBER:
				case TYPE_NPC_NAME:
				case TYPE_ITEM_NAME:
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
		return _S__ED_CONFIRMDLG;
	}
}
