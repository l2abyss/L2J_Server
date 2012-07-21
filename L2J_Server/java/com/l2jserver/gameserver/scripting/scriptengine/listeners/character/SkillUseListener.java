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
package com.l2jserver.gameserver.scripting.scriptengine.listeners.character;

import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.scripting.scriptengine.events.SkillUseEvent;
import com.l2jserver.gameserver.scripting.scriptengine.impl.L2JListener;

/**
 * @author TheOne
 */
public abstract class SkillUseListener extends L2JListener
{
	private L2Character _character = null;
	private int _skillId = -1;
	private int _npcId = -1;
	private boolean _characterSpecific = false;
	
	/**
	 * constructor L2Character specific, will only be fired when this L2Character uses the specified skill Use skillId = -1 to be notified of all skills used
	 * @param character
	 * @param skillId
	 */
	public SkillUseListener(L2Character character, int skillId)
	{
		_skillId = skillId;
		_character = character;
		_characterSpecific = true;
		register();
	}
	
	/**
	 * constructor NPC specific, will only be triggered when npc with the given ID uses the correct skill Use skillId = -1 to be notified of all skills used Use npcId = -1 to be notified for all NPCs use npcId = -2 to be notified for all players use npcId = -3 to be notified for all L2Characters
	 * @param npcId
	 * @param skillId
	 */
	public SkillUseListener(int npcId, int skillId)
	{
		_skillId = skillId;
		_npcId = npcId;
		register();
	}
	
	/**
	 * A L2Character just cast a skill
	 * @param event
	 * @return
	 */
	public abstract boolean onSkillUse(SkillUseEvent event);
	
	@Override
	public void register()
	{
		if (!_characterSpecific)
		{
			L2Character.addGlobalSkillUseListener(this);
		}
		else
		{
			_character.addSkillUseListener(this);
		}
	}
	
	@Override
	public void unregister()
	{
		if (!_characterSpecific)
		{
			L2Character.removeGlobalSkillUseListener(this);
		}
		else
		{
			_character.removeSkillUseListener(this);
		}
	}
	
	/**
	 * Returns the npcId this listener will be triggered for
	 * @return
	 */
	public int getNpcId()
	{
		return _npcId;
	}
	
	/**
	 * Returns the skillId this listener will be triggered for
	 * @return
	 */
	public int getSkillId()
	{
		return _skillId;
	}
	
	/**
	 * Returns the L2Character this listener is attached to
	 * @return
	 */
	public L2Character getCharacter()
	{
		return _character;
	}
}
