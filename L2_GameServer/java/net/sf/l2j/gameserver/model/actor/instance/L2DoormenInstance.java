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
package net.sf.l2j.gameserver.model.actor.instance;

import java.util.Arrays;
import java.util.StringTokenizer;

import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.instancemanager.ClanHallManager;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.entity.ClanHall;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.MyTargetSelected;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.ValidateLocation;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;
import net.sf.l2j.gameserver.util.Evolve;

/**
 * This class ...
 *
 * @version $Revision$ $Date$
 */
public class L2DoormenInstance extends L2NpcInstance
{
	private ClanHall _clanHall;
	private static final int COND_ALL_FALSE = 0;
	private static final int COND_BUSY_BECAUSE_OF_SIEGE = 1;
	private static final int COND_CASTLE_OWNER = 2;
	private static final int COND_HALL_OWNER = 3;
	private static final int COND_FORT_OWNER = 4;

	// list of clan halls with evolve function, should be sorted
	private static final int[] CH_WITH_EVOLVE = {36, 37, 38, 39, 40, 41, 51, 52, 53, 54, 55, 56, 57};

	/**
	 * @param template
	 */
	public L2DoormenInstance(int objectID, L2NpcTemplate template)
	{
		super(objectID, template);
	}

	public final ClanHall getClanHall()
	{
		if (_clanHall == null)
			_clanHall = ClanHallManager.getInstance().getNearbyClanHall(getX(), getY(), 500);
		return _clanHall;
	}

	public final boolean hasEvolve()
	{
		if (getClanHall() == null)
			return false;

		return Arrays.binarySearch(CH_WITH_EVOLVE, getClanHall().getId()) >= 0;
	}

	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		int condition = validateCondition(player);
		if (condition <= COND_ALL_FALSE)
			return;
		if (condition == COND_BUSY_BECAUSE_OF_SIEGE)
			return;
		else if (condition == COND_CASTLE_OWNER || condition == COND_HALL_OWNER || condition == COND_FORT_OWNER)
		{
			if (command.startsWith("Chat"))
			{
				showMessageWindow(player);
				return;
			}
			else if (command.startsWith("open_doors"))
			{
				if (condition == COND_HALL_OWNER)
				{
					getClanHall().openCloseDoors(true);
					NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile("data/html/clanHallDoormen/doormen-opened.htm");
					html.replace("%objectId%", String.valueOf(getObjectId()));
					player.sendPacket(html);
					return;
				}
				else if (condition == COND_CASTLE_OWNER)
				{
					//DoorTable doorTable = DoorTable.getInstance();
					StringTokenizer st = new StringTokenizer(command.substring(10), ", ");
					st.nextToken(); // Bypass first value since its castleid/hallid/fortid

					while (st.hasMoreTokens())
					{
						getCastle().openDoor(player, Integer.parseInt(st.nextToken()));
					}
					return;
				}
				else if (condition == COND_FORT_OWNER)
				{
					//DoorTable doorTable = DoorTable.getInstance();
					StringTokenizer st = new StringTokenizer(command.substring(10), ", ");
					st.nextToken(); // Bypass first value since its castleid/hallid/fortid

					while (st.hasMoreTokens())
					{
						getFort().openDoor(player, Integer.parseInt(st.nextToken()));
					}
					return;
				}
			}
			else if (command.startsWith("close_doors"))
			{
				if (condition == COND_HALL_OWNER)
				{
					getClanHall().openCloseDoors(false);
					NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile("data/html/clanHallDoormen/doormen-closed.htm");
					html.replace("%objectId%", String.valueOf(getObjectId()));
					player.sendPacket(html);
					return;
				}
				else if (condition == COND_CASTLE_OWNER)
				{
					StringTokenizer st = new StringTokenizer(command.substring(11), ", ");
					st.nextToken(); // Bypass first value since its castleid/hallid/fortid

					while (st.hasMoreTokens())
					{
						getCastle().closeDoor(player, Integer.parseInt(st.nextToken()));
					}
					return;
				}
				else if (condition == COND_FORT_OWNER)
				{
					StringTokenizer st = new StringTokenizer(command.substring(11), ", ");
					st.nextToken(); // Bypass first value since its castleid/hallid/fortid

					while (st.hasMoreTokens())
					{
						getFort().closeDoor(player, Integer.parseInt(st.nextToken()));
					}
					return;
				}
			}
			else if (command.startsWith("evolve"))
			{
				StringTokenizer st = new StringTokenizer(command, " ");
				if (st.countTokens() < 2 || !hasEvolve())
					return;
				
				st.nextToken();
				boolean ok = false;
				switch (Integer.parseInt(st.nextToken()))
				{
					case 1:
						ok = Evolve.doEvolve(player, this, 9882, 10307, 55);
						break;
					case 2:
						ok = Evolve.doEvolve(player, this, 4422, 10308, 55);
						break;
					case 3:
						ok = Evolve.doEvolve(player, this, 4423, 10309, 55);
						break;
					case 4:
						ok = Evolve.doEvolve(player, this, 4424, 10310, 55);
						break;
					case 5:
						ok = Evolve.doEvolve(player, this, 10426, 10611, 70);
						break;
				}
				NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				if (ok)
					html.setFile("data/html/clanHallDoormen/evolve-ok.htm");
				else
					html.setFile("data/html/clanHallDoormen/evolve-no.htm");
				player.sendPacket(html);
			}
		}
		super.onBypassFeedback(player, command);
	}

	/**
	* this is called when a player interacts with this NPC
	* @param player
	*/
	@Override
	public void onAction(L2PcInstance player)
	{
		if (!canTarget(player))
			return;

		// Check if the L2PcInstance already target the L2NpcInstance
		if (this != player.getTarget())
		{
			// Set the target of the L2PcInstance player
			player.setTarget(this);

			// Send a Server->Client packet MyTargetSelected to the L2PcInstance player
			MyTargetSelected my = new MyTargetSelected(getObjectId(), 0);
			player.sendPacket(my);

			// Send a Server->Client packet ValidateLocation to correct the L2NpcInstance position and heading on the client
			player.sendPacket(new ValidateLocation(this));
		}
		else
		{
			// Calculate the distance between the L2PcInstance and the L2NpcInstance
			if (!canInteract(player))
			{
				// Notify the L2PcInstance AI with AI_INTENTION_INTERACT
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
			}
			else
			{
				showMessageWindow(player);
			}
		}
		// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	public void showMessageWindow(L2PcInstance player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		String filename = "data/html/doormen/" + getTemplate().npcId + "-no.htm";

		int condition = validateCondition(player);
		if (condition == COND_BUSY_BECAUSE_OF_SIEGE)
			filename = "data/html/doormen/"
				+ getTemplate().npcId + "-busy.htm"; // Busy because of siege
		else if (condition == COND_CASTLE_OWNER || condition == COND_FORT_OWNER) // Clan owns castle or fort
			filename = "data/html/doormen/" + getTemplate().npcId + ".htm"; // Owner message window

		// Prepare doormen for clan hall
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		if (getClanHall() != null)
		{
			L2Clan owner = ClanTable.getInstance().getClan(getClanHall().getOwnerId());
			if (condition == COND_HALL_OWNER)
			{
				if (hasEvolve())
				{
					html.setFile("data/html/clanHallDoormen/doormen2.htm");
					html.replace("%clanname%", owner.getName());
				}
				else
				{
					html.setFile("data/html/clanHallDoormen/doormen1.htm");
					html.replace("%clanname%", owner.getName());
				}
			}
			else
			{
				if (owner != null && owner.getLeader() != null)
				{
					html.setFile("data/html/clanHallDoormen/doormen-no.htm");
					html.replace("%leadername%", owner.getLeaderName());
					html.replace("%clanname%", owner.getName());
				}
				else
				{
					html.setFile("data/html/clanHallDoormen/emptyowner.htm");
					html.replace("%hallname%", getClanHall().getName());
				}
			}
		}
		else
			html.setFile(filename);

		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);
	}

	private int validateCondition(L2PcInstance player)
	{
		if (player.getClan() != null)
		{
			// Prepare doormen for clan hall
			if (getClanHall() != null)
			{
				if (player.getClanId() == getClanHall().getOwnerId())
					return COND_HALL_OWNER;
				else
					return COND_ALL_FALSE;
			}

			if (getCastle() != null && getCastle().getCastleId() > 0)
			{
				if (getCastle().getOwnerId() == player.getClanId()) // Clan owns castle
					return COND_CASTLE_OWNER; // Owner
				else
					return COND_ALL_FALSE;
			}

			if (getFort() != null && getFort().getFortId() > 0)
			{
				if (getFort().getOwnerClan()!= null && getFort().getOwnerClan().getClanId()== player.getClanId()) // Clan owns fortress
					return COND_FORT_OWNER; // Owner
				else
					return COND_ALL_FALSE;
			}
		}

		return COND_ALL_FALSE;
	}
}
