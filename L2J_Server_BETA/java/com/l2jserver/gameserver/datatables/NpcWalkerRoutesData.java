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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.l2jserver.Config;
import com.l2jserver.gameserver.engines.DocumentParser;
import com.l2jserver.gameserver.model.L2NpcWalkerNode;
import com.l2jserver.gameserver.network.NpcStringId;

/**
 * Main Table to Load Npc Walkers Routes and Chat.
 * @author Rayan, JIV
 */
public class NpcWalkerRoutesData extends DocumentParser
{
	private static final Map<Integer, List<L2NpcWalkerNode>> _routes = new HashMap<>();
	
	protected NpcWalkerRoutesData()
	{
		if (Config.ALLOW_NPC_WALKERS)
		{
			load();
		}
	}
	
	@Override
	public void load()
	{
		_routes.clear();
		parseDatapackFile("data/WalkerRoutes.xml");
		_log.info(getClass().getSimpleName() + ": Loaded " + _routes.size() + " Npc Walker Routes.");
	}
	
	@Override
	protected void parseDocument()
	{
		final Node n = getCurrentDocument().getFirstChild();
		for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
		{
			if (d.getNodeName().equals("walker"))
			{
				List<L2NpcWalkerNode> list = new ArrayList<>(5);
				final Integer npcId = parseInteger(d.getAttributes(), "npcId");
				for (Node r = d.getFirstChild(); r != null; r = r.getNextSibling())
				{
					if (r.getNodeName().equals("route"))
					{
						NamedNodeMap attrs = r.getAttributes();
						int id = parseInt(attrs, "id");
						int x = parseInt(attrs, "X");
						int y = parseInt(attrs, "Y");
						int z = parseInt(attrs, "Z");
						int delay = parseInt(attrs, "delay");
						String chatString = null;
						NpcStringId npcString = null;
						Node node = attrs.getNamedItem("string");
						if (node != null)
						{
							chatString = node.getNodeValue();
						}
						else
						{
							node = attrs.getNamedItem("npcString");
							if (node != null)
							{
								npcString = NpcStringId.getNpcStringId(node.getNodeValue());
								if (npcString == null)
								{
									_log.log(Level.WARNING, getClass().getSimpleName() + ": Unknown npcstring '" + node.getNodeValue() + ".");
									continue;
								}
							}
							else
							{
								node = attrs.getNamedItem("npcStringId");
								if (node != null)
								{
									npcString = NpcStringId.getNpcStringId(parseInt(node));
									if (npcString == null)
									{
										_log.log(Level.WARNING, getClass().getSimpleName() + ": Unknown npcstring '" + node.getNodeValue() + ".");
										continue;
									}
								}
							}
						}
						
						list.add(new L2NpcWalkerNode(id, npcString, chatString, x, y, z, delay, parseBoolean(attrs, "run")));
					}
				}
				_routes.put(npcId, list);
			}
		}
	}
	
	public List<L2NpcWalkerNode> getRouteForNpc(int id)
	{
		return _routes.get(id);
	}
	
	public static NpcWalkerRoutesData getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final NpcWalkerRoutesData _instance = new NpcWalkerRoutesData();
	}
}
