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

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.l2jserver.Config;
import com.l2jserver.gameserver.GameTimeController;
import com.l2jserver.gameserver.engines.DocumentParser;
import com.l2jserver.gameserver.model.actor.L2Character;

/**
 * This class load, holds and calculates the hit condition bonuses.
 * @author Nik
 */
public final class HitConditionBonus extends DocumentParser
{
	private int frontBonus = 0;
	private int sideBonus = 0;
	private int backBonus = 0;
	private int highBonus = 0;
	private int lowBonus = 0;
	private int darkBonus = 0;
	private int rainBonus = 0;
	
	private HitConditionBonus()
	{
		parseDatapackFile("data/stats/hitConditionBonus.xml");
		_log.info(getClass().getSimpleName() + ": Loaded Hit Condition bonuses.");
		if (Config.DEBUG)
		{
			_log.info(getClass().getSimpleName() + ": Front bonus: " + frontBonus);
			_log.info(getClass().getSimpleName() + ": Side bonus: " + sideBonus);
			_log.info(getClass().getSimpleName() + ": Back bonus: " + backBonus);
			_log.info(getClass().getSimpleName() + ": High bonus: " + highBonus);
			_log.info(getClass().getSimpleName() + ": Low bonus: " + lowBonus);
			_log.info(getClass().getSimpleName() + ": Dark bonus: " + darkBonus);
			_log.info(getClass().getSimpleName() + ": Rain bonus: " + rainBonus);
		}
	}
	
	@Override
	protected void parseDocument(Document doc)
	{
		final Node n = doc.getFirstChild();
		NamedNodeMap attrs;
		for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
		{
			attrs = d.getAttributes();
			switch (d.getNodeName())
			{
				case "front":
					frontBonus = parseInt(attrs, "val");
					break;
				case "side":
					sideBonus = parseInt(attrs, "val");
					break;
				case "back":
					backBonus = parseInt(attrs, "val");
					break;
				case "high":
					highBonus = parseInt(attrs, "val");
					break;
				case "low":
					lowBonus = parseInt(attrs, "val");
					break;
				case "dark":
					darkBonus = parseInt(attrs, "val");
					break;
				case "rain":
					rainBonus = parseInt(attrs, "val");
					break;
			}
		}
	}
	
	/**
	 * @param attacker the attacking character.
	 * @param target the attacked character.
	 * @return the bonus of the attacker against the target.
	 */
	public double getConditionBonus(L2Character attacker, L2Character target)
	{
		double mod = 100;
		// Get high or low bonus
		if ((attacker.getZ() - target.getZ()) > 50)
		{
			mod += highBonus;
		}
		else if ((attacker.getZ() - target.getZ()) < -50)
		{
			mod += lowBonus;
		}
		
		// Get weather bonus
		if (GameTimeController.getInstance().isNowNight())
		{
			mod += darkBonus;
			// else if () No rain support yet.
			// chance += hitConditionBonus.rainBonus;
		}
		
		// Get side bonus
		if (attacker.isBehindTarget())
		{
			mod += backBonus;
		}
		else if (attacker.isInFrontOfTarget())
		{
			mod += frontBonus;
		}
		else
		{
			mod += sideBonus;
		}
		
		// If (mod / 100) is less than 0, return 0, because we can't lower more than 100%.
		return Math.max(mod / 100, 0);
	}
	
	public static HitConditionBonus getInstance()
	{
		return SingletonHolder._instance;
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final HitConditionBonus _instance = new HitConditionBonus();
	}
}