/*
 * Copyright (C) 2004-2013 L2J Server
 * 
 * This file is part of L2J Server.
 * 
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.l2jserver.gameserver.instancemanager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.l2jserver.gameserver.ThreadPoolManager;
import com.l2jserver.gameserver.ai.CtrlIntention;
import com.l2jserver.gameserver.engines.DocumentParser;
import com.l2jserver.gameserver.model.L2CharPosition;
import com.l2jserver.gameserver.model.L2NpcWalkerNode;
import com.l2jserver.gameserver.model.L2WalkRoute;
import com.l2jserver.gameserver.model.Location;
import com.l2jserver.gameserver.model.WalkInfo;
import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.model.actor.instance.L2MonsterInstance;
import com.l2jserver.gameserver.model.actor.tasks.npc.walker.ArrivedTask;
import com.l2jserver.gameserver.model.holders.NpcRoutesHolder;
import com.l2jserver.gameserver.model.quest.Quest;
import com.l2jserver.gameserver.network.NpcStringId;
import com.l2jserver.gameserver.network.clientpackets.Say2;
import com.l2jserver.gameserver.network.serverpackets.NpcSay;
import com.l2jserver.gameserver.util.Broadcast;

/**
 * This class manages walking monsters.
 * @author GKR
 */
public class WalkingManager extends DocumentParser
{
	// Repeat style:
	// 0 - go back
	// 1 - go to first point (circle style)
	// 2 - teleport to first point (conveyor style)
	// 3 - random walking between points.
	public static final byte REPEAT_GO_BACK = 0;
	public static final byte REPEAT_GO_FIRST = 1;
	public static final byte REPEAT_TELE_FIRST = 2;
	public static final byte REPEAT_RANDOM = 3;
	
	private final Map<String, L2WalkRoute> _routes = new HashMap<>(); // all available routes
	private final Map<Integer, WalkInfo> _activeRoutes = new HashMap<>(); // each record represents NPC, moving by predefined route from _routes, and moving progress
	private final Map<Integer, NpcRoutesHolder> _routesToAttach = new HashMap<>(); // each record represents NPC and all available routes for it
	
	protected WalkingManager()
	{
		load();
	}
	
	@Override
	public final void load()
	{
		parseDatapackFile("data/Routes.xml");
		_log.info(getClass().getSimpleName() + ": Loaded " + _routes.size() + " walking routes.");
	}
	
	@Override
	protected void parseDocument()
	{
		Node n = getCurrentDocument().getFirstChild();
		for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
		{
			if (d.getNodeName().equals("route"))
			{
				final String routeName = parseString(d.getAttributes(), "name");
				boolean repeat = parseBoolean(d.getAttributes(), "repeat");
				String repeatStyle = d.getAttributes().getNamedItem("repeatStyle").getNodeValue();
				byte repeatType;
				if (repeatStyle.equalsIgnoreCase("back"))
				{
					repeatType = REPEAT_GO_BACK;
				}
				else if (repeatStyle.equalsIgnoreCase("cycle"))
				{
					repeatType = REPEAT_GO_FIRST;
				}
				else if (repeatStyle.equalsIgnoreCase("conveyor"))
				{
					repeatType = REPEAT_TELE_FIRST;
				}
				else if (repeatStyle.equalsIgnoreCase("random"))
				{
					repeatType = REPEAT_RANDOM;
				}
				else
				{
					repeatType = -1;
				}
				
				final List<L2NpcWalkerNode> list = new ArrayList<>();
				for (Node r = d.getFirstChild(); r != null; r = r.getNextSibling())
				{
					if (r.getNodeName().equals("point"))
					{
						NamedNodeMap attrs = r.getAttributes();
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
									_log.warning(getClass().getSimpleName() + ": Unknown npcstring '" + node.getNodeValue() + ".");
									continue;
								}
							}
							else
							{
								node = attrs.getNamedItem("npcStringId");
								if (node != null)
								{
									npcString = NpcStringId.getNpcStringId(Integer.parseInt(node.getNodeValue()));
									if (npcString == null)
									{
										_log.warning(getClass().getSimpleName() + ": Unknown npcstring '" + node.getNodeValue() + ".");
										continue;
									}
								}
							}
						}
						list.add(new L2NpcWalkerNode(0, npcString, chatString, x, y, z, delay, parseBoolean(attrs, "run")));
					}
					
					else if (r.getNodeName().equals("target"))
					{
						NamedNodeMap attrs = r.getAttributes();
						try
						{
							int npcId = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
							int x = 0, y = 0, z = 0;
							
							x = Integer.parseInt(attrs.getNamedItem("spawnX").getNodeValue());
							y = Integer.parseInt(attrs.getNamedItem("spawnY").getNodeValue());
							z = Integer.parseInt(attrs.getNamedItem("spawnZ").getNodeValue());
							
							NpcRoutesHolder holder = _routesToAttach.containsKey(npcId) ? _routesToAttach.get(npcId) : new NpcRoutesHolder();
							holder.addRoute(routeName, new Location(x, y, z));
							_routesToAttach.put(npcId, holder);
						}
						catch (Exception e)
						{
							_log.warning("Walking Manager: Error in target definition for route : " + routeName);
						}
					}
				}
				_routes.put(routeName, new L2WalkRoute(routeName, list, repeat, false, repeatType));
			}
		}
	}
	
	/**
	 * @param npc NPC to check
	 * @return {@code true} if given NPC, or its leader is controlled by Walking Manager and moves currently.
	 */
	public boolean isOnWalk(L2Npc npc)
	{
		L2MonsterInstance monster = null;
		
		if (npc.isMonster())
		{
			if (((L2MonsterInstance) npc).getLeader() == null)
			{
				monster = (L2MonsterInstance) npc;
			}
			else
			{
				monster = ((L2MonsterInstance) npc).getLeader();
			}
		}
		
		if (((monster != null) && !isRegistered(monster)) || !isRegistered(npc))
		{
			return false;
		}
		
		WalkInfo walk = monster != null ? _activeRoutes.get(monster.getObjectId()) : _activeRoutes.get(npc.getObjectId());
		if (walk.isStoppedByAttack() || walk.isSuspended())
		{
			return false;
		}
		return true;
	}
	
	public L2WalkRoute getRoute(String route)
	{
		return _routes.get(route);
	}
	
	/**
	 * @param npc NPC to check
	 * @return {@code true} if given NPC controlled by Walking Manager.
	 */
	public boolean isRegistered(L2Npc npc)
	{
		return _activeRoutes.containsKey(npc.getObjectId());
	}
	
	/**
	 * @param npc
	 * @return name of route
	 */
	public String getRouteName(L2Npc npc)
	{
		return _activeRoutes.containsKey(npc.getObjectId()) ? _activeRoutes.get(npc.getObjectId()).getRoute().getName() : "";
	}
	
	/**
	 * Start to move given NPC by given route
	 * @param npc NPC to move
	 * @param routeName name of route to move by
	 */
	public void startMoving(final L2Npc npc, final String routeName)
	{
		if (_routes.containsKey(routeName) && (npc != null) && !npc.isDead()) // check, if these route and NPC present
		{
			if (!_activeRoutes.containsKey(npc.getObjectId())) // new walk task
			{
				// only if not already moved / not engaged in battle... should not happens if called on spawn
				if ((npc.getAI().getIntention() == CtrlIntention.AI_INTENTION_ACTIVE) || (npc.getAI().getIntention() == CtrlIntention.AI_INTENTION_IDLE))
				{
					WalkInfo walk = new WalkInfo(routeName);
					
					if (npc.isDebug())
					{
						walk.setLastAction(System.currentTimeMillis());
					}
					
					L2NpcWalkerNode node = walk.getCurrentNode();
					
					// adjust next waypoint, if NPC spawns at first waypoint
					if ((npc.getX() == node.getMoveX()) && (npc.getY() == node.getMoveY()))
					{
						walk.calculateNextNode(npc);
						node = walk.getCurrentNode();
						npc.sendDebugMessage("Route " + routeName + ", spawn point is same with first waypoint, adjusted to next");
					}
					
					if (!npc.isInsideRadius(node.getMoveX(), node.getMoveY(), node.getMoveZ(), 3000, true, false))
					{
						npc.sendDebugMessage("Route " + routeName + ", NPC is too far from starting point, walking will no start");
						return;
					}
					
					npc.sendDebugMessage("Starting to move at route " + routeName);
					npc.setIsRunning(node.getRunning());
					npc.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition(node.getMoveX(), node.getMoveY(), node.getMoveZ(), 0));
					walk.setWalkCheckTask(ThreadPoolManager.getInstance().scheduleAiAtFixedRate(new Runnable()
					{
						@Override
						public void run()
						{
							startMoving(npc, routeName);
						}
					}, 60000, 60000)); // start walk check task, for resuming walk after fight
					
					npc.getKnownList().startTrackingTask();
					
					_activeRoutes.put(npc.getObjectId(), walk); // register route
				}
				else
				{
					npc.sendDebugMessage("Trying to start move at route " + routeName + ", but cannot now, scheduled");
					ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
					{
						@Override
						public void run()
						{
							startMoving(npc, routeName);
						}
					}, 60000);
				}
			}
			else
			// walk was stopped due to some reason (arrived to node, script action, fight or something else), resume it
			{
				if (_activeRoutes.containsKey(npc.getObjectId()) && ((npc.getAI().getIntention() == CtrlIntention.AI_INTENTION_ACTIVE) || (npc.getAI().getIntention() == CtrlIntention.AI_INTENTION_IDLE)))
				{
					WalkInfo walk = _activeRoutes.get(npc.getObjectId());
					if (walk == null)
					{
						return;
					}
					
					// Prevent call simultaneously from scheduled task and onArrived() or temporarily stop walking for resuming in future
					if (walk.isBlocked() || walk.isSuspended())
					{
						npc.sendDebugMessage("Trying continue to move at route " + routeName + ", but cannot now (operation is blocked)");
						return;
					}
					
					walk.setBlocked(true);
					L2NpcWalkerNode node = walk.getCurrentNode();
					npc.sendDebugMessage("Route id: " + routeName + ", continue to node " + walk.getCurrentNodeId());
					npc.setIsRunning(node.getRunning());
					npc.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition(node.getMoveX(), node.getMoveY(), node.getMoveZ(), 0));
					walk.setBlocked(false);
					walk.setStoppedByAttack(false);
				}
				else
				{
					npc.sendDebugMessage("Trying continue to move at route " + routeName + ", but cannot now (wrong AI state)");
				}
			}
		}
	}
	
	/**
	 * Cancel NPC moving permanently
	 * @param npc NPC to cancel
	 */
	public synchronized void cancelMoving(L2Npc npc)
	{
		if (_activeRoutes.containsKey(npc.getObjectId()))
		{
			final WalkInfo walk = _activeRoutes.remove(npc.getObjectId());
			walk.getWalkCheckTask().cancel(true);
			npc.getKnownList().stopTrackingTask();
		}
	}
	
	/**
	 * Resumes previously stopped moving
	 * @param npc NPC to resume
	 */
	public void resumeMoving(final L2Npc npc)
	{
		if (!_activeRoutes.containsKey(npc.getObjectId()))
		{
			return;
		}
		
		WalkInfo walk = _activeRoutes.get(npc.getObjectId());
		walk.setSuspended(false);
		walk.setStoppedByAttack(false);
		startMoving(npc, walk.getRoute().getName());
	}
	
	/**
	 * Pause NPC moving until it will be resumed
	 * @param npc NPC to pause moving
	 * @param suspend {@code true} if moving was temporarily suspended for some reasons of AI-controlling script
	 * @param stoppedByAttack {@code true} if moving was suspended because of NPC was attacked or desired to attack
	 */
	public void stopMoving(L2Npc npc, boolean suspend, boolean stoppedByAttack)
	{
		L2MonsterInstance monster = null;
		
		if (npc.isMonster())
		{
			if (((L2MonsterInstance) npc).getLeader() == null)
			{
				monster = (L2MonsterInstance) npc;
			}
			else
			{
				monster = ((L2MonsterInstance) npc).getLeader();
			}
		}
		
		if (((monster != null) && !isRegistered(monster)) || !isRegistered(npc))
		{
			return;
		}
		
		WalkInfo walk = monster != null ? _activeRoutes.get(monster.getObjectId()) : _activeRoutes.get(npc.getObjectId());
		
		walk.setSuspended(suspend);
		walk.setStoppedByAttack(stoppedByAttack);
		
		if (monster != null)
		{
			monster.stopMove(null);
			monster.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
		}
		else
		{
			npc.stopMove(null);
			npc.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
		}
	}
	
	/**
	 * Manage "node arriving"-related tasks: schedule move to next node; send ON_NODE_ARRIVED event to Quest script
	 * @param npc NPC to manage
	 */
	public void onArrived(final L2Npc npc)
	{
		if (_activeRoutes.containsKey(npc.getObjectId()))
		{
			// Notify quest
			if (npc.getTemplate().getEventQuests(Quest.QuestEventType.ON_NODE_ARRIVED) != null)
			{
				for (Quest quest : npc.getTemplate().getEventQuests(Quest.QuestEventType.ON_NODE_ARRIVED))
				{
					quest.notifyNodeArrived(npc);
				}
			}
			
			WalkInfo walk = _activeRoutes.get(npc.getObjectId());
			
			// Opposite should not happen... but happens sometime
			if ((walk.getCurrentNodeId() >= 0) && (walk.getCurrentNodeId() < walk.getRoute().getNodesCount()))
			{
				L2NpcWalkerNode node = walk.getRoute().getNodeList().get(walk.getCurrentNodeId());
				if (npc.isInsideRadius(node.getMoveX(), node.getMoveY(), node.getMoveZ(), 10, false, false))
				{
					npc.sendDebugMessage("Route: " + walk.getRoute().getName() + ", arrived to node " + walk.getCurrentNodeId());
					npc.sendDebugMessage("Done in " + ((System.currentTimeMillis() - walk.getLastAction()) / 1000) + " s.");
					walk.calculateNextNode(npc);
					int delay = node.getDelay();
					walk.setBlocked(true); // prevents to be ran from walk check task, if there is delay in this node.
					
					if (node.getNpcString() != null)
					{
						Broadcast.toKnownPlayers(npc, new NpcSay(npc, Say2.NPC_ALL, node.getNpcString()));
					}
					else
					{
						final String text = node.getChatText();
						if ((text != null) && !text.isEmpty())
						{
							Broadcast.toKnownPlayers(npc, new NpcSay(npc, Say2.NPC_ALL, text));
						}
					}
					
					if (npc.isDebug())
					{
						walk.setLastAction(System.currentTimeMillis());
					}
					ThreadPoolManager.getInstance().scheduleGeneral(new ArrivedTask(npc, walk), 100 + (delay * 1000L));
				}
			}
		}
	}
	
	/**
	 * Manage "on death"-related tasks: permanently cancel moving of died NPC
	 * @param npc NPC to manage
	 */
	public void onDeath(L2Npc npc)
	{
		cancelMoving(npc);
	}
	
	/**
	 * Manage "on spawn"-related tasks: start NPC moving, if there is route attached to its spawn point
	 * @param npc NPC to manage
	 */
	public void onSpawn(L2Npc npc)
	{
		if (_routesToAttach.containsKey(npc.getNpcId()))
		{
			final String routeName = _routesToAttach.get(npc.getNpcId()).getRouteName(npc);
			if (!routeName.isEmpty())
			{
				startMoving(npc, routeName);
			}
		}
	}
	
	public static final WalkingManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final WalkingManager _instance = new WalkingManager();
	}
}