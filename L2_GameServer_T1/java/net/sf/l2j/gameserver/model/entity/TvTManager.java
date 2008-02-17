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
package net.sf.l2j.gameserver.model.entity;

import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.Announcements;
import net.sf.l2j.gameserver.ThreadPoolManager;

/**
 * @author FBIagent
 */
public class TvTManager implements Runnable
{
    protected static final Logger _log = Logger.getLogger(TvTManager.class.getName());
    
	/** The one and only instance of this class<br> */
	private static TvTManager _instance = null;

    private TvTStartTask _task;
    
	/**
	 * New instance only by getInstance()<br>
	 */
	private TvTManager()
	{
		if (Config.TVT_EVENT_ENABLED)
		{
            TvTEvent.init();
            
            this.scheduleEventStart();
			_log.info("TvTEventEngine[TvTManager.TvTManager()]: Started.");
		}
		else
        {
			_log.info("TvTEventEngine[TvTManager.TvTManager()]: Engine is disabled.");
        }
	}

	/**
	 * Initialize new/Returns the one and only instance<br><br>
	 *
	 * @return TvTManager<br>
	 */
	public static TvTManager getInstance()
	{
		if (_instance == null)
			_instance = new TvTManager();

		return _instance;
	}
    
    public void scheduleEventStart()
    {
        _task = new TvTStartTask(System.currentTimeMillis() + Config.TVT_EVENT_INTERVAL*60*1000);
        ThreadPoolManager.getInstance().executeTask(_task);
    }

	/**
	 * The task method to handle cycles of the event<br><br>
	 *
	 * @see java.lang.Runnable#run()<br>
	 */
	public void run()
	{
	    if (!TvTEvent.startParticipation())
	    {
	        Announcements.getInstance().announceToAll("TvT Event: Event was cancelled.");
	        _log.warning("TvTEventEngine[TvTManager.run()]: Error spawning event npc for participation.");
	        
	        this.scheduleEventStart();
	    }
	    else
	    {
	        Announcements.getInstance().announceToAll("TvT Event: Registration opened for " + Config.TVT_EVENT_PARTICIPATION_TIME +  " minute(s).");
            
	        // schedule registration end
	        _task.setStartTime(System.currentTimeMillis() + Config.TVT_EVENT_PARTICIPATION_TIME*60*1000);
	        ThreadPoolManager.getInstance().executeTask(_task);
	    }
	}

    public void startEvent()
    {
        if (!TvTEvent.startFight())
        {
            Announcements.getInstance().announceToAll("TvT Event: Event cancelled due to lack of Participation.");
            _log.info("TvTEventEngine[TvTManager.run()]: Lack of registration, abort event.");
            
            this.scheduleEventStart();
        }
        else
        {
            TvTEvent.sysMsgToAllParticipants("TvT Event: Teleporting participants to an arena in " + Config.TVT_EVENT_START_LEAVE_TELEPORT_DELAY + " second(s).");
            _task.setStartTime(System.currentTimeMillis() + Config.TVT_EVENT_RUNNING_TIME*60*1000);
            ThreadPoolManager.getInstance().executeTask(_task);
        }
    }
    
    public void endEvent()
    {
        Announcements.getInstance().announceToAll(TvTEvent.calculateRewards());
        TvTEvent.sysMsgToAllParticipants("TvT Event: Teleporting back to the registration npc in " + Config.TVT_EVENT_START_LEAVE_TELEPORT_DELAY + " second(s).");
        TvTEvent.stopFight();
        
        this.scheduleEventStart();
    }
    
    class TvTStartTask implements Runnable
    {
        private long _startTime;

        public TvTStartTask(long startTime)
        {
            _startTime = startTime;
        }
        
        public void setStartTime(long startTime)
        {
            _startTime = startTime;
        }
        
        /**
         * @see java.lang.Runnable#run()
         */
        public void run()
        {
            int delay = (int) Math.round((_startTime - System.currentTimeMillis()) / 1000.0);
            
            if (delay > 0)
            {
                this.announce(delay);
            }
            
            int nextMsg = 0;
            if (delay > 3600)
            {
                nextMsg = delay - 3600;
            }
            else if (delay > 1800)
            {
                nextMsg = delay - 1800;
            }
            else if (delay > 900)
            {
                nextMsg = delay - 900;
            }
            else if (delay > 600)
            {
                nextMsg = delay - 600;
            }
            else if (delay > 300)
            {
                nextMsg = delay - 300;
            }
            else if (delay > 60)
            {
                nextMsg = delay - 60;
            }
            else if (delay > 5)
            {
                nextMsg = delay - 5;
            }
            else if (delay > 0)
            {
                nextMsg = delay;
            }
            else
            {
                // start
                if (TvTEvent.isInactive())
                {
                    TvTManager.this.run();
                }
                else if (TvTEvent.isParticipating())
                {
                    TvTManager.this.startEvent();
                }
                else
                {
                    TvTManager.this.endEvent();
                }
            }
            
            if (delay > 0)
            {
                ThreadPoolManager.getInstance().scheduleGeneral(this, nextMsg*1000);
            }
        }
        
        private void announce(long time)
        {
            if (time >= 3600 && time%3600 == 0)
            {
                if (TvTEvent.isParticipating())
                {
                    Announcements.getInstance().announceToAll("TvT Event: "+(time/60/60)+" hour(s) until registration is closed!");
                }
                else if (TvTEvent.isStarted())
                {
                    TvTEvent.sysMsgToAllParticipants("TvT Event: "+(time/60/60)+" hour(s) until event is finished!");
                }
            }
            else if (time >= 60)
            {
                if (TvTEvent.isParticipating())
                {
                    Announcements.getInstance().announceToAll("TvT Event: "+(time/60)+" minute(s) until registration is closed!");
                }
                else if (TvTEvent.isStarted())
                {
                    TvTEvent.sysMsgToAllParticipants("TvT Event: "+(time/60)+" minute(s) until the event is finished!");
                }
            }
            else
            {
                if (TvTEvent.isParticipating())
                {
                    Announcements.getInstance().announceToAll("TvT Event: "+time+" second(s) until registration is closed!");
                }
                else if (TvTEvent.isStarted())
                {
                    TvTEvent.sysMsgToAllParticipants("TvT Event: "+time+" second(s) until the event is finished!");
                }
            }
        }
    }

	/**
	 * This method waits for a period time delay<br><br>
	 *
	 * @param interval<br>
	 */
	void waiter(int seconds)
	{
		while (seconds > 1)
		{
			seconds--; // here because we don't want to see two time announce at the same time

			if (TvTEvent.isParticipating() || TvTEvent.isStarted())
			{
				
			}

			long oneSecWaitStart = System.currentTimeMillis();

			while (oneSecWaitStart + 1000L > System.currentTimeMillis())
			{
				try
				{
					Thread.sleep(1);
				}
				catch (InterruptedException ie)
				{}
			}
		}
	}
}
