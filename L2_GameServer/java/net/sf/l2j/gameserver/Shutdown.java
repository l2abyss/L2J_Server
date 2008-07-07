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
package net.sf.l2j.gameserver;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.gameserverpackets.ServerStatus;
import net.sf.l2j.gameserver.instancemanager.CastleManorManager;
import net.sf.l2j.gameserver.instancemanager.CursedWeaponsManager;
import net.sf.l2j.gameserver.instancemanager.GrandBossManager;
import net.sf.l2j.gameserver.instancemanager.ItemsOnGroundManager;
import net.sf.l2j.gameserver.instancemanager.QuestManager;
import net.sf.l2j.gameserver.instancemanager.RaidBossSpawnManager;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.L2GameClient;
import net.sf.l2j.gameserver.serverpackets.ServerClose;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.util.Broadcast;

/**
 *
 * This class provides the functions for shutting down and restarting the server
 * It closes all open clientconnections and saves all data.
 *
 * @version $Revision: 1.2.4.5 $ $Date: 2005/03/27 15:29:09 $
 */
public class Shutdown extends Thread
{
	private static Logger _log = Logger.getLogger(Shutdown.class.getName());
	private static Shutdown _instance;
	private static Shutdown _counterInstance = null;

	private int _secondsShut;
	private int _shutdownMode;
	public static final int SIGTERM = 0;
	public static final int GM_SHUTDOWN = 1;
	public static final int GM_RESTART = 2;
	public static final int ABORT = 3;
	private static final String[] MODE_TEXT = {"SIGTERM", "shutting down", "restarting", "aborting"};

    /**
     * This function starts a shutdown countdown from Telnet (Copied from Function startShutdown())
     *
     * @param ip            IP Which Issued shutdown command
     * @param seconds       seconds untill shutdown
     * @param restart       true if the server will restart after shutdown
     */
		@SuppressWarnings("deprecation")
        private void SendServerQuit(int seconds)
		{
			SystemMessage sysm = new SystemMessage(1);
			sysm.addNumber(seconds);
			Broadcast.toAllOnlinePlayers(sysm);
		}

    public void startTelnetShutdown(String IP, int seconds, boolean restart)
    {
        _log.warning("IP: " + IP + " issued shutdown command. " + MODE_TEXT[_shutdownMode] + " in "+seconds+ " seconds!");
        //_an.announceToAll("Server is " + _modeText[shutdownMode] + " in "+seconds+ " seconds!");

        if (restart) {
            _shutdownMode = GM_RESTART;
        } else {
            _shutdownMode = GM_SHUTDOWN;
        }

        if(_shutdownMode > 0)
        {
			switch (seconds)
			{
			    case 540:
			    case 480:
			    case 420:
			    case 360:
				case 300:
				case 240:
				case 180:
				case 120:
				case 60:
				case 30:
				case 10:
				case 5:
				case 4:
				case 3:
				case 2:
				case 1:
					break;
				default:
					SendServerQuit(seconds);
			}
        }

        if (_counterInstance != null) {
            _counterInstance._abort();
        }
        _counterInstance = new Shutdown(seconds, restart);
        _counterInstance.start();
    }

    /**
     * This function aborts a running countdown
     *
     * @param IP            IP Which Issued shutdown command
     */
    public void telnetAbort(String IP) {
        _log.warning("IP: " + IP + " issued shutdown ABORT. " + MODE_TEXT[_shutdownMode] + " has been stopped!");

        if (_counterInstance != null) {
            _counterInstance._abort();
			Announcements _an = Announcements.getInstance();
			_an.announceToAll("Server aborts " + MODE_TEXT[_shutdownMode] + " and continues normal operation!");
        }
    }

	/**
	 * Default constucter is only used internal to create the shutdown-hook instance
	 *
	 */
	public Shutdown() {
		_secondsShut = -1;
		_shutdownMode = SIGTERM;
	}

	/**
	 * This creates a countdown instance of Shutdown.
	 *
	 * @param seconds	how many seconds until shutdown
	 * @param restart	true is the server shall restart after shutdown
	 *
	 */
	public Shutdown(int seconds, boolean restart) {
		if (seconds < 0) {
			seconds = 0;
		}
		_secondsShut = seconds;
		if (restart) {
			_shutdownMode = GM_RESTART;
		} else {
			_shutdownMode = GM_SHUTDOWN;
		}
	}

	/**
	 * get the shutdown-hook instance
	 * the shutdown-hook instance is created by the first call of this function,
	 * but it has to be registrered externaly.
	 *
	 * @return	instance of Shutdown, to be used as shutdown hook
	 */
	public static Shutdown getInstance()
	{
		if (_instance == null)
		{
			_instance = new Shutdown();
		}
		return _instance;
	}

	/**
	 * this function is called, when a new thread starts
	 *
	 * if this thread is the thread of getInstance, then this is the shutdown hook
	 * and we save all data and disconnect all clients.
	 *
	 * after this thread ends, the server will completely exit
	 *
	 * if this is not the thread of getInstance, then this is a countdown thread.
	 * we start the countdown, and when we finished it, and it was not aborted,
	 * we tell the shutdown-hook why we call exit, and then call exit
	 *
	 * when the exit status of the server is 1, startServer.sh / startServer.bat
	 * will restart the server.
	 *
	 */
	@Override
	public void run()
	{
		// disallow new logins
		try
		{
            //Doesnt actually do anything
			//Server.gameServer.getLoginController().setMaxAllowedOnlinePlayers(0);
		}
		catch (Throwable t)
		{
			// ignore
		}

		if (this == _instance)
		{
			// ensure all services are stopped
			try
			{
				GameTimeController.getInstance().stopTimer();
			}
			catch (Throwable t)
			{
				// ignore
			}

			// stop all threadpolls
			try
			{
				ThreadPoolManager.getInstance().shutdown();
			}
			catch (Throwable t)
			{
				// ignore
			}

			// last byebye, save all data and quit this server
			// logging doesnt work here :(
			saveData();

			try
			{
				LoginServerThread.getInstance().interrupt();
			}
			catch (Throwable t)
			{
				// ignore
			}

			// saveData sends messages to exit players, so sgutdown selector after it
			try
			{
				GameServer.gameServer.getSelectorThread().shutdown();
				GameServer.gameServer.getSelectorThread().setDaemon(true);
			}
			catch (Throwable t)
			{
				// ignore
			}

			// commit data, last chance
			try
			{
				L2DatabaseFactory.getInstance().shutdown();
			}
			catch (Throwable t)
			{

			}

			// server will quit, when this function ends.
			if (_instance._shutdownMode == GM_RESTART)
			{
				Runtime.getRuntime().halt(2);
			}
			else
			{
				Runtime.getRuntime().halt(0);
			}
		}
		else
		{
			// gm shutdown: send warnings and then call exit to start shutdown sequence
			countdown();
			// last point where logging is operational :(
			_log.warning("GM shutdown countdown is over. " + MODE_TEXT[_shutdownMode] + " NOW!");
			switch (_shutdownMode) {
				case GM_SHUTDOWN:
					_instance.setMode(GM_SHUTDOWN);
					System.exit(0);
					break;
				case GM_RESTART:
					_instance.setMode(GM_RESTART);
					System.exit(2);
					break;
			}
		}
	}

	/**
	 * This functions starts a shutdown countdown
	 *
	 * @param activeChar	GM who issued the shutdown command
	 * @param seconds		seconds until shutdown
	 * @param restart		true if the server will restart after shutdown
	 */
	public void startShutdown(L2PcInstance activeChar, int seconds, boolean restart) {
		_log.warning("GM: "+activeChar.getName()+"("+activeChar.getObjectId()+") issued shutdown command. " + MODE_TEXT[_shutdownMode] + " in "+seconds+ " seconds!");

		if (restart) {
            _shutdownMode = GM_RESTART;
        } else {
            _shutdownMode = GM_SHUTDOWN;
        }

        if(_shutdownMode > 0)
        {
			switch (seconds)
			{
			    case 540:
			    case 480:
			    case 420:
			    case 360:
				case 300:
				case 240:
				case 180:
				case 120:
				case 60:
				case 30:
				case 10:
				case 5:
				case 4:
				case 3:
				case 2:
				case 1:
					break;
				default:
					SendServerQuit(seconds);
			}
        }

		if (_counterInstance != null) {
			_counterInstance._abort();
		}        	

//		 the main instance should only run for shutdown hook, so we start a new instance
		_counterInstance = new Shutdown(seconds, restart);
		_counterInstance.start();
	}

	/**
	 * This function aborts a running countdown
	 *
	 * @param activeChar	GM who issued the abort command
	 */
	public void abort(L2PcInstance activeChar) {
		_log.warning("GM: "+activeChar.getName()+"("+activeChar.getObjectId()+") issued shutdown ABORT. " + MODE_TEXT[_shutdownMode] + " has been stopped!");
		if (_counterInstance != null) {
			_counterInstance._abort();
			Announcements _an = Announcements.getInstance();
			_an.announceToAll("Server aborts " + MODE_TEXT[_shutdownMode] + " and continues normal operation!");
		}
	}

	/**
	 * set the shutdown mode
	 * @param mode	what mode shall be set
	 */
	private void setMode(int mode) {
		_shutdownMode = mode;
	}

	/**
	 * set shutdown mode to ABORT
	 *
	 */
	private void _abort() {
		_shutdownMode = ABORT;
	}

	/**
	 * this counts the countdown and reports it to all players
	 * countdown is aborted if mode changes to ABORT
	 */
	private void countdown() {

		try {
			while (_secondsShut > 0) {

				switch (_secondsShut)
				{
				    case 540:SendServerQuit(540);break;
				    case 480:SendServerQuit(480);break;
				    case 420:SendServerQuit(420);break;
				    case 360:SendServerQuit(360);break;
					case 300:SendServerQuit(300);break;
					case 240:SendServerQuit(240);break;
					case 180:SendServerQuit(180);break;
					case 120:SendServerQuit(120);break;
					case 60:
						LoginServerThread.getInstance().setServerStatus(ServerStatus.STATUS_DOWN); //avoids new players from logging in
						SendServerQuit(60);break;
					case 30:SendServerQuit(30);break;
					case 10:SendServerQuit(10);break;
					case 5:SendServerQuit(5);break;
					case 4:SendServerQuit(4);break;
					case 3:SendServerQuit(3);break;
					case 2:SendServerQuit(2);break;
					case 1:SendServerQuit(1);break;
				}

				_secondsShut--;

				int delay = 1000; //milliseconds
				Thread.sleep(delay);

				if(_shutdownMode == ABORT) break;
			}
		} catch (InterruptedException e) {
			//this will never happen
		}
	}

	/**
	 * this sends a last byebye, disconnects all players and saves data
	 *
	 */
	private void saveData() {
		switch (_shutdownMode)
		{
			case SIGTERM:
				System.err.println("SIGTERM received. Shutting down NOW!");
				break;
			case GM_SHUTDOWN:
				System.err.println("GM shutdown received. Shutting down NOW!");
				break;
			case GM_RESTART:
				System.err.println("GM restart received. Restarting NOW!");
				break;

		}
		if (Config.ACTIVATE_POSITION_RECORDER)
			Universe.getInstance().implode(true);
		try
		{
		
		} catch (Throwable t) {
			_log.log(Level.INFO, "", t);
		}

		// we cannt abort shutdown anymore, so i removed the "if"
		disconnectAllCharacters();

        // Seven Signs data is now saved along with Festival data.
        if (!SevenSigns.getInstance().isSealValidationPeriod())
            SevenSignsFestival.getInstance().saveFestivalData(false);

        // Save Seven Signs data before closing. :)
        SevenSigns.getInstance().saveSevenSignsData(null, true);

        // Save all raidboss and GrandBoss status ^_^
        RaidBossSpawnManager.getInstance().cleanUp();
        System.err.println("RaidBossSpawnManager: All raidboss info saved!!");
        GrandBossManager.getInstance().cleanUp();
        System.err.println("GrandBossManager: All Grand Boss info saved!!");
        TradeController.getInstance().dataCountStore();
        System.err.println("TradeController: All count Item Saved");
        try
        {
            Olympiad.getInstance().save();
        }
        catch(Exception e){e.printStackTrace();}
        System.err.println("Olympiad System: Data saved!!");
        
        // Save Cursed Weapons data before closing.
        CursedWeaponsManager.getInstance().saveData();
        
        // Save all manor data
        CastleManorManager.getInstance().save();
        
        // Save all global (non-player specific) Quest data that needs to persist after reboot
        QuestManager.getInstance().save();
        
        //Save items on ground before closing
        if(Config.SAVE_DROPPED_ITEM){
        ItemsOnGroundManager.getInstance().saveInDb();        
        ItemsOnGroundManager.getInstance().cleanUp();
        System.err.println("ItemsOnGroundManager: All items on ground saved!!");
        }
		System.err.println("Data saved. All players disconnected, shutting down.");
		
		try {
			int delay = 5000;
			Thread.sleep(delay);
		} 
		catch (InterruptedException e) {
			//never happens :p
		}
	}

	/**
	 * this disconnects all clients from the server
	 *
	 */
	@SuppressWarnings("deprecation")
    private void disconnectAllCharacters()
	{
		SystemMessage sysm = new SystemMessage(0);
		ServerClose ql = new ServerClose();
		Collection<L2PcInstance> pls = L2World.getInstance().getAllPlayers().values();
		synchronized (L2World.getInstance().getAllPlayers())
		{
			for (L2PcInstance player : pls)
			{
				player.sendPacket(sysm);
				//Logout Character
				try {
					L2GameClient.saveCharToDisk(player);
					//SystemMessage sm = new SystemMessage(SystemMessage.YOU_HAVE_WON_THE_WAR_OVER_THE_S1_CLAN);
					//player.sendPacket(sm);
					player.sendPacket(ql);
				} catch (Throwable t)	{}
			}
		}
		try { Thread.sleep(1000); } catch (Throwable t) {_log.log(Level.INFO, "", t);}
		

		pls = L2World.getInstance().getAllPlayers().values();
		synchronized (L2World.getInstance().getAllPlayers())
		{
			for (L2PcInstance player : pls)
			{
				try {
					player.closeNetConnection();
				} catch (Throwable t)	{
				// just to make sure we try to kill the connection 
				}				
			}
		}
	}

}
