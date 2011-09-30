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
package com.l2jserver.gameserver.instancemanager;

import com.l2jserver.Config;
import com.l2jserver.gameserver.Announcements;
import com.l2jserver.gameserver.ThreadPoolManager;
import com.l2jserver.gameserver.ai.CtrlIntention;
import com.l2jserver.gameserver.model.L2CharPosition;
import com.l2jserver.gameserver.model.L2NpcWalkerNode;
import com.l2jserver.gameserver.model.L2WalkRoute;
import com.l2jserver.gameserver.model.actor.L2Npc;
import com.l2jserver.gameserver.network.NpcStringId;
import com.l2jserver.util.Rnd;

import java.io.File;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import javolution.util.FastList;
import javolution.util.FastMap;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * This class manages walking monsters.
 * @author GKR
 */
public class WalkingManager
{
	private static final Logger _log = Logger.getLogger(WalkingManager.class.getName());
	
	//Repeat style: 0 - go back, 1 - go to first point (circle style), 2 - teleport to first point (conveyor style), 3 - random walking between points.
	private static final byte REPEAT_GO_BACK = 0;
	private static final byte REPEAT_GO_FIRST = 1;
	private static final byte REPEAT_TELE_FIRST = 2;
	private static final byte REPEAT_RANDOM = 3;
	
	private Map<Integer, L2WalkRoute> _routes; //all available routes
	private Map<Integer, WalkInfo> _activeRoutes; //each record represents NPC, moving by predefined route from _routes, and moving progress
	
	private class WalkInfo
	{
		private ScheduledFuture<?> _walkCheckTask;
		private boolean _blocked = false;
		private boolean _suspended = false;
		private boolean _nodeArrived = false;
		private int _currentNode  = 0;
		private boolean _forward = true; //Determines first --> last or first <-- last direction
		private int _routeId;
		
		public WalkInfo(int routeId)
		{
			_routeId = routeId;
		}
		
		private L2WalkRoute getRoute()
		{
			return _routes.get(_routeId);
		}
		
		private L2NpcWalkerNode getCurrentNode()
		{
			return getRoute().getNodeList().get(_currentNode); 
		}
	}
	
	public static final WalkingManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private WalkingManager()
	{
		_routes = new FastMap<Integer, L2WalkRoute>();
		_activeRoutes = new FastMap<Integer, WalkInfo>();
		load();
	}
	
	private final void load()
	{
		_log.info("WalkingManager: Loading walking routes...");
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setIgnoringComments(true);
		File file = new File(Config.DATAPACK_ROOT, "data/Routes.xml");
		Document doc = null;
		if (file.exists())
		{
			try
			{
				doc = factory.newDocumentBuilder().parse(file);
			}
			catch (Exception e)
			{
				_log.log(Level.WARNING, "Could not parse Routes.xml file: " + e.getMessage(), e);
			}
			
			Node n = doc.getFirstChild();
			for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
			{
				if (d.getNodeName().equals("route"))
				{
					boolean debug = false;
					int routeId = Integer.parseInt(d.getAttributes().getNamedItem("id").getNodeValue());
					boolean repeat = Boolean.parseBoolean(d.getAttributes().getNamedItem("repeat").getNodeValue());
					String repeatStyle = d.getAttributes().getNamedItem("repeatStyle").getNodeValue();
					byte repeatType;
					
					if (repeatStyle.equalsIgnoreCase("back"))
						repeatType = REPEAT_GO_BACK;
					else if (repeatStyle.equalsIgnoreCase("cycle"))
						repeatType = REPEAT_GO_FIRST;
					else if (repeatStyle.equalsIgnoreCase("conveyor"))
						repeatType = REPEAT_TELE_FIRST;
					else if (repeatStyle.equalsIgnoreCase("random"))
						repeatType = REPEAT_RANDOM;
					else
						repeatType = -1;
					
					List<L2NpcWalkerNode> list = new FastList<L2NpcWalkerNode>();
					for (Node r = d.getFirstChild(); r != null; r = r.getNextSibling())
					{
						if (r.getNodeName().equals("point"))
						{
							NamedNodeMap attrs = r.getAttributes();
							int x = Integer.parseInt(attrs.getNamedItem("X").getNodeValue());
							int y = Integer.parseInt(attrs.getNamedItem("Y").getNodeValue());
							int z = Integer.parseInt(attrs.getNamedItem("Z").getNodeValue());
							int delay = Integer.parseInt(attrs.getNamedItem("delay").getNodeValue());
							
							String chatString = null;
							NpcStringId npcString = null;
							Node node = attrs.getNamedItem("string");
							if (node != null)
								chatString = node.getNodeValue();
							else
							{
								node = attrs.getNamedItem("npcString");
								if (node != null)
								{
									npcString = NpcStringId.getNpcStringId(node.getNodeValue());
									if (npcString == null)
									{
										_log.log(Level.WARNING, "NpcWalkerRoutersTable: Unknown npcstring '" + node.getNodeValue() + ".");
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
											_log.log(Level.WARNING, "NpcWalkerRoutersTable: Unknown npcstring '" + node.getNodeValue() + ".");
											continue;
										}
									}
								}
							}
							
							boolean running = Boolean.parseBoolean(attrs.getNamedItem("run").getNodeValue());
							list.add(new L2NpcWalkerNode(0, npcString, chatString, x, y, z, delay, running));
						}
						else if (r.getNodeName().equals("stat"))
						{
							NamedNodeMap attrs = r.getAttributes();
							String name = attrs.getNamedItem("name").getNodeValue();
							String val = attrs.getNamedItem("val").getNodeValue();
							
							if (name.equalsIgnoreCase("debug"))
								debug = Boolean.parseBoolean(val);
						}
					
					}
					L2WalkRoute newRoute = new L2WalkRoute(routeId, list, repeat, false, repeatType);
					newRoute.setDebug(debug);
					_routes.put(routeId, newRoute);
				}
			}
		}
		_log.info("WalkingManager: loaded " + _routes.size() + " walking routes.");
	}
	
	public boolean isRegistered(L2Npc npc)
	{
		return _activeRoutes.containsKey(npc.getObjectId());
	}
	
	public void startMoving(final L2Npc npc, final int routeId)
	{
		if (_routes.containsKey(routeId) && npc != null && !npc.isDead()) //check, if these route and NPC present
		{
			if (!_activeRoutes.containsKey(npc.getObjectId())) //new walk task
			{
 				//only if not already moved / not engaged in battle... should not happens if called on spawn
				if (npc.getAI().getIntention() == CtrlIntention.AI_INTENTION_ACTIVE || npc.getAI().getIntention() == CtrlIntention.AI_INTENTION_IDLE)
				{
					WalkInfo walk = new WalkInfo(routeId);
					//walk._lastActionTime = System.currentTimeMillis();
					L2NpcWalkerNode node = walk.getCurrentNode();
					
					if (!npc.isInsideRadius(node.getMoveX(), node.getMoveY(), node.getMoveZ(), 3000, true, false)) //too far from first point, decline further operations
						return;
					
					//Announcements.getInstance().announceToAll("Start to move!");
					npc.setIsRunning(node.getRunning());
					npc.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition(node.getMoveX(), node.getMoveY(), node.getMoveZ(), 0));
					walk._walkCheckTask = ThreadPoolManager.getInstance().scheduleAiAtFixedRate(new Runnable() {
					@Override
					public void run() 
					{
						startMoving(npc, routeId);
					}
					}, 60000, 60000); //start walk check task, for resuming walk after fight
					
					npc.getKnownList().startTrackingTask();
					
					_activeRoutes.put(npc.getObjectId(), walk); //register route
				}
				else //try a bit later
					ThreadPoolManager.getInstance().scheduleGeneral(new Runnable() {
					@Override
					public void run() 
					{
						startMoving(npc, routeId);
					}
					}, 60000);
			}
			else //walk was stopped due to some reason (arrived to node, script action, fight or something else), resume it
			{
				//Announcements.getInstance().announceToAll("Here_1!");
				if (npc.getAI().getIntention() == CtrlIntention.AI_INTENTION_ACTIVE || npc.getAI().getIntention() == CtrlIntention.AI_INTENTION_IDLE)
				{
					//Announcements.getInstance().announceToAll("Here_2!");
					WalkInfo walk = _activeRoutes.get(npc.getObjectId());
					//Announcements.getInstance().announceToAll("X = " + Integer.toString(npc.getX()) + ", Y = " +  Integer.toString(npc.getY()) + ", node = " + Integer.toString(walk._currentNode));
					
					//Prevent call simultaneosly from scheduled task and onArrived() or temporarily stop walking for resuming in future
					if (walk._blocked || walk._suspended)
						return;
					
					//Announcements.getInstance().announceToAll("Continue move!");
					walk._blocked = true;
					//Check this first, within the bounds of random moving, we have no conception of "first" or "last" node
					if (walk.getRoute().getRepeatType() == REPEAT_RANDOM && walk._nodeArrived)
					{
						int newNode = walk._currentNode;
						
						while (newNode == walk._currentNode)
							newNode = Rnd.get(walk.getRoute().getNodesCount());
						
						walk._currentNode = newNode;
						walk._nodeArrived = false;
					}
					
					else if (walk._currentNode == walk.getRoute().getNodesCount()) //Last node arrived
					{
						if (walk.getRoute().debug())
							Announcements.getInstance().announceToAll("Last node arrived!");
						
						if (!walk.getRoute().repeatWalk())
						{
							//Announcements.getInstance().announceToAll("Stoppping!");
							cancelMoving(npc);
							return;
						}
						
						switch (walk.getRoute().getRepeatType())
						{
							case REPEAT_GO_BACK:
								walk._forward = false;
								walk._currentNode -=2;
								break;
							case REPEAT_GO_FIRST:
								walk._currentNode = 0;
								break;
							case REPEAT_TELE_FIRST:
								npc.teleToLocation(npc.getSpawn().getLocx(),npc.getSpawn().getLocy(),npc.getSpawn().getLocz());
								walk._currentNode = 0;
						}
					}
					
					else if (walk._currentNode == -1) //First node arrived, when direction is first <-- last
					{
						walk._currentNode = 1;
						walk._forward = true;
					}
					
					L2NpcWalkerNode node = walk.getCurrentNode();
					npc.setIsRunning(node.getRunning());
					
					if (walk.getRoute().debug())
						Announcements.getInstance().announceToAll("Continue to node " + Integer.toString(walk._currentNode));
					
					npc.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition(node.getMoveX(), node.getMoveY(), node.getMoveZ(), 0));
					walk._blocked = false;
				}
			}
		}
	}
	
	public void cancelMoving(L2Npc npc)
	{
		if (_activeRoutes.containsKey(npc.getObjectId()))
		{
			_activeRoutes.get(npc.getObjectId())._walkCheckTask.cancel(true);
			_activeRoutes.remove(npc.getObjectId());
			npc.getKnownList().stopTrackingTask();
			//Announcements.getInstance().announceToAll("Moving cancelled!");
		}
	}
	
	public void resumeMoving(final L2Npc npc)
	{
		if (!_activeRoutes.containsKey(npc.getObjectId()))
			return;
		
		WalkInfo walk = _activeRoutes.get(npc.getObjectId());
		walk._suspended = false;
		startMoving(npc, walk.getRoute().getId());
	}
	
	public void stopMoving(L2Npc npc, boolean suspend)
	{
		if (!_activeRoutes.containsKey(npc.getObjectId()))
			return; 
		
		WalkInfo walk = _activeRoutes.get(npc.getObjectId());
		walk._suspended = suspend;
		npc.stopMove(null);
		npc.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
	}
	
	public void onArrived(final L2Npc npc)
	{
		if (_activeRoutes.containsKey(npc.getObjectId()))
		{
			WalkInfo walk = _activeRoutes.get(npc.getObjectId());
			
			//Opposite should not happen... but happens sometime
			if (walk._currentNode >= 0 && walk._currentNode < walk.getRoute().getNodesCount())
			{
				L2NpcWalkerNode node = walk.getRoute().getNodeList().get(walk._currentNode);
				if (node.getMoveX() == npc.getX() && node.getMoveY() == npc.getY())
				{ 
					if (walk.getRoute().debug())
					{
						Announcements.getInstance().announceToAll("Arrived to node " + Integer.toString(walk._currentNode));
						//Announcements.getInstance().announceToAll("Done in " + Long.toString((System.currentTimeMillis() - walk._lastActionTime) / 1000) + " s.");
					}
					
					walk._nodeArrived = true;
					if (walk.getRoute().getRepeatType() != REPEAT_RANDOM)
					{
						if (walk._forward)
							walk._currentNode++;
						else
							walk._currentNode--;
					}
					
					int delay;
					
					if (walk._currentNode >= walk.getRoute().getNodesCount())
						delay = walk.getRoute().getLastNode().getDelay();
					else if (walk._currentNode < 0)
						delay = walk.getRoute().getNodeList().get(0).getDelay();
					else
						delay = walk.getCurrentNode().getDelay();
					
					walk._blocked = true; //prevents to be ran from walk check task, if there is delay in this node.
					//walk._lastActionTime = System.currentTimeMillis();
					ThreadPoolManager.getInstance().scheduleGeneral(new ArrivedTask(npc, walk), 100 + delay * 1000L);
				}
			}
		}
	}
	
	public void onDeath(L2Npc npc)
	{
		if (_activeRoutes.containsKey(npc.getObjectId()))
			cancelMoving(npc);
	}
	
	private class ArrivedTask implements Runnable
	{
		WalkInfo _walk;
		L2Npc _npc;
		
		public ArrivedTask(L2Npc npc, WalkInfo walk)
		{
			_npc = npc;
			_walk = walk;
		}
		
		@Override
		public void run() 
		{
			_walk._blocked = false;
			startMoving(_npc,_walk. getRoute().getId());
		}
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final WalkingManager _instance = new WalkingManager();
	}
}