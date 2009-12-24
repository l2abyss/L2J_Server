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
package com.l2jserver.gameserver.model.actor.instance;

import com.l2jserver.Config;
import com.l2jserver.gameserver.model.base.PlayerClass;
import com.l2jserver.gameserver.model.base.Race;
import com.l2jserver.gameserver.model.quest.QuestState;
import com.l2jserver.gameserver.templates.chars.L2NpcTemplate;

public final class L2VillageMasterKamaelInstance extends L2VillageMasterInstance
{
	public L2VillageMasterKamaelInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	protected final String getSubClassMenu(Race pRace)
	{
		if (Config.ALT_GAME_SUBCLASS_EVERYWHERE
				|| pRace == Race.Kamael)
			return "data/html/villagemaster/SubClass.htm";

		return "data/html/villagemaster/SubClass_NoKamael.htm";
	}

	@Override
	protected final String getSubClassFail()
	{
		return "data/html/villagemaster/SubClass_Fail_Kamael.htm";
	}

	@Override
	protected final boolean checkQuests(L2PcInstance player)
	{
		QuestState qs = player.getQuestState("234_FatesWhisper");
		if (qs == null || !qs.isCompleted())
			return false;

		qs = player.getQuestState("236_SeedsOfChaos");
		if (qs == null || !qs.isCompleted())
			return false;

		return true;
	}

	@Override
	protected final boolean checkVillageMasterRace(PlayerClass pclass)
	{
		if (pclass == null)
			return false;

		return pclass.isOfRace(Race.Kamael);
	}
}