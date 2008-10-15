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

/**
 * @author godson
 */

package net.sf.l2j.gameserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

import javolution.util.FastMap;
import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.datatables.DoorTable;
import net.sf.l2j.gameserver.datatables.HeroSkillTable;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.datatables.SpawnTable;
import net.sf.l2j.gameserver.instancemanager.OlympiadStadiaManager;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Party;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.L2Summon;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.model.entity.Hero;
import net.sf.l2j.gameserver.model.itemcontainer.Inventory;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ExAutoSoulShot;
import net.sf.l2j.gameserver.network.serverpackets.ExOlympiadMode;
import net.sf.l2j.gameserver.network.serverpackets.ExOlympiadUserInfoSpectator;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;
import net.sf.l2j.gameserver.templates.StatsSet;
import net.sf.l2j.util.L2FastList;
import net.sf.l2j.util.Rnd;

public class Olympiad
{
	/**
	 *
	 * @author  ascharot
	 * 
	 */
	private class OlympiadGameTask implements Runnable
	{
		public L2OlympiadGame _game = null;
        private SystemMessage _sm;
        private SystemMessage _sm2;
        
		private boolean _terminated = false;
		public boolean isTerminated()
		{
			return _terminated;
		}
		
		private boolean _started = false;
		public boolean isStarted()
		{
			return _started;
		}
		
		public OlympiadGameTask(L2OlympiadGame game)
		{
			_game = game;
		}
		
		protected boolean checkBattleStatus()
    	{
			boolean _pOneCrash = (_game._playerOne == null || _game._playerOneDisconnected);
			boolean _pTwoCrash = (_game._playerTwo == null || _game._playerTwoDisconnected);
			if(_pOneCrash || _pTwoCrash || _game._aborted) {
	    		return false; 
			}
			return true;
    	}
		
		protected boolean checkStatus()
    	{
			boolean _pOneCrash = (_game._playerOne == null || _game._playerOneDisconnected);
			boolean _pTwoCrash = (_game._playerTwo == null || _game._playerTwoDisconnected);
			
			StatsSet playerOneStat;
			StatsSet playerTwoStat;
			
			playerOneStat = _nobles.get(_game._playerOneID);
			playerTwoStat = _nobles.get(_game._playerTwoID);
			
			int playerOnePlayed = playerOneStat.getInteger(COMP_DONE);
			int playerTwoPlayed = playerTwoStat.getInteger(COMP_DONE);
			
			if(_pOneCrash || _pTwoCrash || _game._aborted) {
	    		if (_pOneCrash && !_pTwoCrash){
					try{						
						int playerOnePoints = playerOneStat.getInteger(POINTS);
						int transferPoints = playerOnePoints / 5;
						playerOneStat.set(POINTS, playerOnePoints - transferPoints);
						
						if (Config.DEBUG)
							_log.info("Olympia Result: "+_game._playerOneName+" vs "+_game._playerTwoName+" ... "+_game._playerOneName+" lost "+transferPoints+" points for crash");						
						
						int playerTwoPoints = playerTwoStat.getInteger(POINTS);
						playerTwoStat.set(POINTS, playerTwoPoints + transferPoints);
						
						if (Config.DEBUG)
							_log.info("Olympia Result: "+_game._playerOneName+" vs "+_game._playerTwoName+" ... "+_game._playerTwoName+" Win "+transferPoints+" points");
					
						_sm = new SystemMessage(SystemMessageId.C1_HAS_WON_THE_GAME);
						_sm2 = new SystemMessage(SystemMessageId.S1_HAS_GAINED_S2_OLYMPIAD_POINTS);
						_sm.addString(_game._playerTwoName);
						broadcastMessage(_sm, true);
						_sm2.addString(_game._playerTwoName);
						_sm2.addNumber(transferPoints);
						broadcastMessage(_sm2, true);
					} catch(Exception e){e.printStackTrace();}

	    		}
	    		if (_pTwoCrash && !_pOneCrash){
	    			try{
	    				int playerTwoPoints = playerTwoStat.getInteger(POINTS);
	    				int transferPoints = playerTwoPoints / 5;
	    				playerTwoStat.set(POINTS, playerTwoPoints - transferPoints);
	    				
	    				if (Config.DEBUG)
	    					_log.info("Olympia Result: "+_game._playerTwoName+" vs "+_game._playerOneName+" ... "+_game._playerTwoName+" lost "+transferPoints+" points for crash");
	    				
	    				int playerOnePoints = playerOneStat.getInteger(POINTS);
	    				playerOneStat.set(POINTS, playerOnePoints + transferPoints);
	    				
	    				if (Config.DEBUG)
	    					_log.info("Olympia Result: "+_game._playerTwoName+" vs "+_game._playerOneName+" ... "+_game._playerOneName+" Win "+transferPoints+" points");
					
	    				_sm = new SystemMessage(SystemMessageId.C1_HAS_WON_THE_GAME);
	    				_sm2 = new SystemMessage(SystemMessageId.S1_HAS_GAINED_S2_OLYMPIAD_POINTS);
	    				_sm.addString(_game._playerOneName);
	    				broadcastMessage(_sm, true);
	    				_sm2.addString(_game._playerOneName);
	    				_sm2.addNumber(transferPoints);
	                	broadcastMessage(_sm2, true);
	    			} catch(Exception e){e.printStackTrace();}
	    		}
	    		playerOneStat.set(COMP_DONE, playerOnePlayed + 1);
	    		playerTwoStat.set(COMP_DONE, playerTwoPlayed + 1);
	    		
	    		_terminated = true;
	    		_game._gamestarted = false;
    			_game.closeDoors();    		
    			_game.PlayersStatusBack();
    			try{
    				_game.portPlayersBack();
    			}catch(Exception e){e.printStackTrace();}
                _game = null;
				return false; 
			}
			return true;
    	}
		
		public void run()
		{
			_started = true;
			if(_game != null)
			{
				if(_game._playerOne != null && _game._playerTwo != null)
				{
					//Waiting for teleport to arena
		    		for (int i=120;i>10;i-=5)
		    		{
		    			switch(i) {
		    				case 120:
		    				case 60:
		    				case 30:
		    				case 15: 
		    					_game.sendMessageToPlayers(false,i);
		    					break;
		    			}
						try{ Thread.sleep(5000); }catch (InterruptedException e){}
		    		}
		    		for (int i=5;i>0;i--)
		    		{
		    			_game.sendMessageToPlayers(false,i);		    			
		    			try{ Thread.sleep(1000); }catch (InterruptedException e){}
		    		}
		    		
		    		//Checking for openents and teleporting to arena
		    		if(!checkStatus()) { return; }
			    	_game.closeDoors();
		            _game.removals();
		    		_game.portPlayersToArena();
		    		try{ Thread.sleep(5000); }catch (InterruptedException e){}
		    		
		            synchronized(this){
		            if(!_battleStarted)
			            	_battleStarted = true;
		            }
		    		for (int i=60;i>10;i-=10)
		    		{
	    				_game.sendMessageToPlayers(true,i);	    				
		    			try{ Thread.sleep(10000); }catch (InterruptedException e){}		    			
	    				if (i==20) { 
	    					_game.additions();
	    					_game.openDoors();
	    					_game.sendMessageToPlayers(true,10);
	    					try{ Thread.sleep(5000); }catch (InterruptedException e){}
	    				}
		    		}
		    		for (int i=5;i>0;i--)
		    		{
		    			_game.sendMessageToPlayers(true,i);		    			
		    			try{ Thread.sleep(1000); }catch (InterruptedException e){}
		    		}
		    		
		    		if(!checkStatus()) { return; }
		    		UnSpawnBuffer(_game._spawnOne);
		    		UnSpawnBuffer(_game._spawnTwo);
		    		_game._playerOne.sendPacket(new ExOlympiadUserInfoSpectator(_game._playerTwo, 1));
		    		_game._playerTwo.sendPacket(new ExOlympiadUserInfoSpectator(_game._playerOne, 1));
		            if (_game._spectators != null)
		            {
		                for (L2PcInstance  spec : _game.getSpectators())
		                {
		                    try {         
		                    	spec.sendPacket(new ExOlympiadUserInfoSpectator(_game._playerTwo, 2));
		                    	spec.sendPacket(new ExOlympiadUserInfoSpectator(_game._playerOne, 1));
		                    } catch (NullPointerException e) {}
		                }
		            }
		    		_game.makeCompetitionStart();		    		
		    			
		    		//Wait 3 mins (Battle)
		    		for(int i=0; i<BATTLE_PERIOD; i += 10000)
		    		{
			    		try{
			    			Thread.sleep(10000);
			    			//If game haveWinner thean stop waiting battle_period and validate winner
			    			if(_game.haveWinner() || !checkBattleStatus())
			    				break;
			    		} catch (InterruptedException e){}
		    		}		    		
		    		if(!checkStatus()) { return; }
		    		_terminated = true;
		    		_game._gamestarted = false;
		    		try{
		    			_game.validateWinner();
			    		_game.PlayersStatusBack();
			    		_game.removals();
		    			_game.portPlayersBack();
		    		}catch(Exception e){e.printStackTrace();}
	                _game = null;
				}
			}
		}
        private void broadcastMessage(SystemMessage sm, boolean toAll)
        {
        	try { 
        		_game._playerOne.sendPacket(sm);
        		_game._playerTwo.sendPacket(sm); 
        	} catch (Exception e) {}
        	
            if (toAll && _game._spectators != null)
            {
                for (L2PcInstance spec : _game._spectators)
                {
                    try { spec.sendPacket(sm); } catch (NullPointerException e) {}
                }
            }
        }
	}

	protected static final Logger _log = Logger.getLogger(Olympiad.class.getName());

    private static Olympiad _instance;
    
    protected static Map<Integer, StatsSet> _nobles;
    protected static L2FastList<StatsSet> _heroesToBe;
    protected static L2FastList<L2PcInstance> _nonClassBasedRegisters;
    protected static Map<Integer, L2FastList<L2PcInstance>> _classBasedRegisters;

    private static final String OLYMPIAD_DATA_FILE = "config/olympiad.properties";
    public static final String OLYMPIAD_HTML_FILE = "data/html/olympiad/";
    private static final String OLYMPIAD_LOAD_NOBLES = "SELECT * FROM olympiad_nobles";
    private static final String OLYMPIAD_SAVE_NOBLES = "INSERT INTO olympiad_nobles " +
            "VALUES (?,?,?,?,?)";
    private static final String OLYMPIAD_UPDATE_NOBLES = "UPDATE olympiad_nobles SET " +
            "olympiad_points = ?, competitions_done = ? WHERE charId = ?";
    private static final String OLYMPIAD_GET_HEROS = "SELECT charId, char_name FROM " +
    		"olympiad_nobles WHERE class_id = ? AND competitions_done >= 9 ORDER BY " +
    		"olympiad_points DESC, competitions_done DESC";
    private static final String GET_EACH_CLASS_LEADER = "SELECT char_name FROM " +
    		"olympiad_nobles WHERE class_id = ? ORDER BY olympiad_points DESC, " +
    		"competitions_done DESC";
    private static final String OLYMPIAD_DELETE_ALL = "DELETE FROM olympiad_nobles";
    private static final int[] HERO_IDS = {88,89,90,91,92,93,94,95,96,97,98,99,100,101,102,103,104,105,
        106,107,108,109,110,111,112,113,114,115,116,117,118,131,132,133,134};

    private static final int COMP_START = Config.ALT_OLY_START_TIME; // 6PM
    private static final int COMP_MIN = Config.ALT_OLY_MIN; // 00 mins
    private static final long COMP_PERIOD = Config.ALT_OLY_CPERIOD; // 6 hours
    protected static final long BATTLE_PERIOD = Config.ALT_OLY_BATTLE; // 6 mins
    protected static final long BATTLE_WAIT = Config.ALT_OLY_BWAIT; // 10mins
    protected static final long INITIAL_WAIT = Config.ALT_OLY_IWAIT;  // 5mins
    protected static final long WEEKLY_PERIOD = Config.ALT_OLY_WPERIOD; // 1 week
    protected static final long VALIDATION_PERIOD = Config.ALT_OLY_VPERIOD; // 24 hours

    /* FOR TESTING
    private static final int COMP_START = 8; // 1PM - 2PM
    private static final int COMP_MIN = 15; // 20mins
    private static final long COMP_PERIOD = 7200000; // 2hours
    private static final long BATTLE_PERIOD = 180000; // 3mins
    private static final long BATTLE_WAIT = 600000; // 10mins
    private static final long INITIAL_WAIT = 300000;  // 5mins
    private static final long WEEKLY_PERIOD = 7200000; // 2 hours
    private static final long VALIDATION_PERIOD = 3600000; // 1 hour */


    private static final int DEFAULT_POINTS = 18;
    protected static final int WEEKLY_POINTS = 3;

    public static final String CHAR_ID = "charId";
    public static final String CLASS_ID = "class_id";
    public static final String CHAR_NAME = "char_name";
    public static final String POINTS = "olympiad_points";
    public static final String COMP_DONE = "competitions_done";
    
    public static DoorTable   _doorTable;
    
    protected long _olympiadEnd;
    protected long _validationEnd;
    protected int _period;
    protected long _nextWeeklyChange;
    protected int _currentCycle;
    private long _compEnd;
    private Calendar _compStart;
    protected static boolean _inCompPeriod;
    protected static boolean _isOlympiadEnd;
    protected static boolean _compStarted = false;
    protected static boolean _battleStarted;
    protected static boolean _cycleTerminated;
    protected ScheduledFuture<?> _scheduledCompStart;
    protected ScheduledFuture<?> _scheduledCompEnd;
    protected ScheduledFuture<?> _scheduledOlympiadEnd;
    protected ScheduledFuture<?> _scheduledManagerTask;
    protected ScheduledFuture<?> _scheduledWeeklyTask;
    protected ScheduledFuture<?> _scheduledValdationTask;

    public static class Stadia
    {
    	private boolean _freeToUse = true;
    	public boolean isFreeToUse()
    	{
    		return _freeToUse;
    	}
    	public void setStadiaBusy()
    	{
    		_freeToUse = false;
    	}
    	public void setStadiaFree()
    	{
    		_freeToUse = true;
    	}
    	private int[] _coords = new int[3];
    	private int[] _doors = new int[2];
    	public int[] getCoordinates()
    	{
    		return _coords;
    	}
    	public int[] getDoorID()
    	{
    		return _doors;
    	}
    	public Stadia(int[] coords, int[] doors)
    	{
    		_coords = coords;
    		_doors = doors;
    	}
    	public Stadia(int x, int y, int z, int d1, int d2)
    	{
    		_coords[0] = x;
    		_coords[1] = y;
    		_coords[2] = z;
    		_doors[0] = d1;
    		_doors[1] = d2;
    	}
    }
    
    protected static final Stadia[] STADIUMS = 
    {
    	new Stadia(-20838, -21075, -3028, 19170001, 19170002),
    	new Stadia(-88000, -252637, -3331, 17100001, 17100002),
    	new Stadia(-83760, -252637, -3331, 17100003, 17100004),
    	new Stadia(-79600, -252637, -3331, 17100005, 17100006),
    	new Stadia(-75648, -252637, -3331, 17100007, 17100008),
    	new Stadia(-88000, -249762, -3331, 17100009, 17100010),
    	new Stadia(-83760, -249762, -3331, 17100011, 17100012),
    	new Stadia(-79600, -249762, -3331, 17100013, 17100014),
    	new Stadia(-75648, -249762, -3331, 17100015, 17100016),
    	new Stadia(-88000, -246951, -3331, 17100017, 17100018),
    	new Stadia(-83760, -246951, -3331, 17100019, 17100020),
    	new Stadia(-79600, -246951, -3331, 17100021, 17100022),
    	new Stadia(-75648, -246951, -3331, 17100023, 17100024),
    	new Stadia(-88000, -244290, -3331, 17100025, 17100026),
    	new Stadia(-83760, -244290, -3331, 17100027, 17100028),
    	new Stadia(-79600, -244290, -3331, 17100029, 17100030),
    	new Stadia(-75648, -244290, -3331, 17100031, 17100032),
    	new Stadia(-88000, -241490, -3331, 17100033, 17100034),
    	new Stadia(-83760, -241490, -3331, 17100035, 17100036),
    	new Stadia(-79600, -241490, -3331, 17100037, 17100038),
    	new Stadia(-75648, -241490, -3331, 17100039, 17100040),
    	new Stadia(-88000, -238825, -3331, 17100041, 17100042),
    	new Stadia(-83760, -238825, -3331, 17100043, 17100044)
    };

    private static enum COMP_TYPE
    {
    	CLASSED,
    	NON_CLASSED
    }

    protected static OlympiadManager _manager;

    public static Olympiad getInstance()
    {
        if (_instance == null)
            _instance = new Olympiad();
        return _instance;
    }

    public Olympiad()
    {
    	try
        {
            load();
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
        catch(SQLException s)
        {
            s.printStackTrace();
        }

        if (_period == 0) init();
    }

    private void load() throws IOException, SQLException
    {
        _nobles = new FastMap<Integer, StatsSet>();

        Properties OlympiadProperties = new Properties();
        InputStream is =  new FileInputStream(new File("./" + OLYMPIAD_DATA_FILE));
        OlympiadProperties.load(is);
        is.close();

        _currentCycle = Integer.parseInt(OlympiadProperties.getProperty("CurrentCycle", "1"));
        _period = Integer.parseInt(OlympiadProperties.getProperty("Period", "0"));
        _olympiadEnd = Long.parseLong(OlympiadProperties.getProperty("OlympiadEnd", "0"));
        _validationEnd = Long.parseLong(OlympiadProperties.getProperty("ValdationEnd", "0"));
        _nextWeeklyChange = Long.parseLong(OlympiadProperties.getProperty("NextWeeklyChange", "0"));

        switch(_period)
        {
            case 0:
                if (_olympiadEnd == 0 || _olympiadEnd < Calendar.getInstance().getTimeInMillis())
                    setNewOlympiadEnd();
                else
                    _isOlympiadEnd = false;
                break;
            case 1:
                if (_validationEnd > Calendar.getInstance().getTimeInMillis())
                {
                    _isOlympiadEnd = true;

                    _scheduledValdationTask  = ThreadPoolManager.getInstance().scheduleGeneral(new Runnable() {
                        public void run()
                        {
                            _period = 0;
                            _currentCycle++;
                            deleteNobles();
                            setNewOlympiadEnd();
                            init();
                        }
                    }, getMillisToValidationEnd());
                }
                else
                {
                    _currentCycle++;
                    _period = 0;
                    deleteNobles();
                    setNewOlympiadEnd();
                }
                break;
                default:
                    _log.warning("Olympiad System: Omg something went wrong in loading!! Period = " + _period);
                return;
        }

        try
        {
            Connection con = L2DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement = con.prepareStatement(OLYMPIAD_LOAD_NOBLES);
            ResultSet rset = statement.executeQuery();

            while(rset.next())
            {
                StatsSet statDat = new StatsSet();
                int charId = rset.getInt(CHAR_ID);
                statDat.set(CLASS_ID, rset.getInt(CLASS_ID));
                statDat.set(CHAR_NAME, rset.getString(CHAR_NAME));
                statDat.set(POINTS, rset.getInt(POINTS));
                statDat.set(COMP_DONE, rset.getInt(COMP_DONE));
                statDat.set("to_save", false);

                _nobles.put(charId, statDat);
            }

            rset.close();
            statement.close();
            con.close();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        synchronized(this)
        {
            _log.info("Olympiad System: Loading Olympiad System....");
            if (_period == 0)
                _log.info("Olympiad System: Currently in Olympiad Period");
            else
                _log.info("Olympiad System: Currently in Validation Period");

            _log.info("Olympiad System: Period Ends....");

            long milliToEnd;
            if (_period == 0)
                milliToEnd = getMillisToOlympiadEnd();
            else
                milliToEnd = getMillisToValidationEnd();

            double numSecs = (milliToEnd / 1000) % 60;
            double countDown = ((milliToEnd / 1000) - numSecs) / 60;
            int numMins = (int) Math.floor(countDown % 60);
            countDown = (countDown - numMins) / 60;
            int numHours = (int) Math.floor(countDown % 24);
            int numDays = (int) Math.floor((countDown - numHours) / 24);

            _log.info("Olympiad System: In " + numDays + " days, " + numHours
                + " hours and " + numMins + " mins.");

            if (_period == 0)
            {
                _log.info("Olympiad System: Next Weekly Change is in....");

                milliToEnd = getMillisToWeekChange();

                double numSecs2 = (milliToEnd / 1000) % 60;
                double countDown2 = ((milliToEnd / 1000) - numSecs2) / 60;
                int numMins2 = (int) Math.floor(countDown2 % 60);
                countDown2 = (countDown2 - numMins2) / 60;
                int numHours2 = (int) Math.floor(countDown2 % 24);
                int numDays2 = (int) Math.floor((countDown2 - numHours2) / 24);

                _log.info("Olympiad System: " + numDays2 + " days, " + numHours2
                    + " hours and " + numMins2 + " mins.");
            }
        }

        _log.info("Olympiad System: Loaded " + _nobles.size() + " Nobles");

    }

    protected void init()
    {
        if (_period == 1)
            return;
    	_nonClassBasedRegisters = new L2FastList<L2PcInstance>();
        _classBasedRegisters = new FastMap<Integer, L2FastList<L2PcInstance>>();

        _compStart = Calendar.getInstance();
        _compStart.set(Calendar.HOUR_OF_DAY, COMP_START);
        _compStart.set(Calendar.MINUTE, COMP_MIN);
        _compEnd = _compStart.getTimeInMillis() + COMP_PERIOD;

    	_scheduledOlympiadEnd = ThreadPoolManager.getInstance().scheduleGeneral(new Runnable(){
    		public void run()
    		{
                SystemMessage sm = new SystemMessage(SystemMessageId.OLYMPIAD_PERIOD_S1_HAS_ENDED);
                sm.addNumber(_currentCycle);

                Announcements.getInstance().announceToAll(sm);
                Announcements.getInstance().announceToAll("Olympiad Validation Period has began");

    			_isOlympiadEnd = true;
                if (_scheduledManagerTask != null)
                    _scheduledManagerTask.cancel(true);
                if (_scheduledWeeklyTask != null)
                    _scheduledWeeklyTask.cancel(true);

                Calendar validationEnd = Calendar.getInstance();
                _validationEnd = validationEnd.getTimeInMillis() + VALIDATION_PERIOD;

                saveNobleData();

                _period = 1;

                sortHerosToBe();

                giveHeroBonus();

                Hero.getInstance().computeNewHeroes(_heroesToBe);

                try {
                    save();
                }
                catch (Exception e) {
                    _log.warning("Olympiad System: Failed to save Olympiad configuration: " + e);
                }

                _scheduledValdationTask  = ThreadPoolManager.getInstance().scheduleGeneral(new Runnable() {
                    public void run()
                    {
                        Announcements.getInstance().announceToAll("Olympiad Validation Period has ended");
                        _period = 0;
                        _currentCycle++;
                        deleteNobles();
                        setNewOlympiadEnd();
                        init();
                    }
                }, getMillisToValidationEnd());
    		}
    	}, getMillisToOlympiadEnd());

        updateCompStatus();
    	scheduleWeeklyChange();
    }

    public boolean registerNoble(L2PcInstance noble, boolean classBased)
    {
        SystemMessage sm;

        /*
        if (_compStarted)
        {
            noble.sendMessage("Cant Register whilst competition is under way");
            return false;
        }
        */

        if (!_inCompPeriod)
        {
            sm = new SystemMessage(SystemMessageId.THE_OLYMPIAD_GAME_IS_NOT_CURRENTLY_IN_PROGRESS);
            noble.sendPacket(sm);
            return false;
        }

        if (noble.isCursedWeaponEquipped())
        {
        	noble.sendMessage("You can't participate to Olympiad while holding a cursed weapon.");
        	return false;
        }

        if (!noble.isNoble())
        {
            sm = new SystemMessage(SystemMessageId.ONLY_NOBLESS_CAN_PARTICIPATE_IN_THE_OLYMPIAD);
            noble.sendPacket(sm);
        	return false;
        }

        if (noble.getBaseClass() != noble.getClassId().getId())
        {
            sm = new SystemMessage(SystemMessageId.YOU_CANT_JOIN_THE_OLYMPIAD_WITH_A_SUB_JOB_CHARACTER);
            noble.sendPacket(sm);
        	return false;
        }

        if (!_nobles.containsKey(noble.getObjectId()))
        {
        	StatsSet statDat = new StatsSet();
        	statDat.set(CLASS_ID, noble.getClassId().getId());
        	statDat.set(CHAR_NAME, noble.getName());
            statDat.set(POINTS, DEFAULT_POINTS);
            statDat.set(COMP_DONE, 0);
            statDat.set("to_save", true);

            _nobles.put(noble.getObjectId(), statDat);
        }

        if (_classBasedRegisters.containsKey(noble.getClassId().getId()))
        {
        	L2FastList<L2PcInstance> classed = _classBasedRegisters.get(noble.getClassId().getId());
            for (L2PcInstance partecipant: classed)
            {
            	if (partecipant.getObjectId()==noble.getObjectId())
            		{
            		sm = new SystemMessage(SystemMessageId.YOU_ARE_ALREADY_ON_THE_WAITING_LIST_TO_PARTICIPATE_IN_THE_GAME_FOR_YOUR_CLASS);
            		noble.sendPacket(sm);
            		return false;
            		}
            }
        }

        for (L2PcInstance partecipant: _nonClassBasedRegisters)
        {
        	if (partecipant.getObjectId()==noble.getObjectId())
        		{
        		sm = new SystemMessage(SystemMessageId.YOU_ARE_ALREADY_ON_THE_WAITING_LIST_FOR_ALL_CLASSES_WAITING_TO_PARTICIPATE_IN_THE_GAME);
        		noble.sendPacket(sm);
        		return false;
        		}
        }

        for (L2OlympiadGame g : _manager.getOlympiadGames().values())
        {
        	if (g == null || g.getPlayers() == null)
        		continue;
        	
        	for(L2PcInstance player : g.getPlayers())
        	{
        		if(player.getObjectId() == noble.getObjectId())
            	{
            		sm = new SystemMessage(SystemMessageId.YOU_ARE_ALREADY_ON_THE_WAITING_LIST_FOR_ALL_CLASSES_WAITING_TO_PARTICIPATE_IN_THE_GAME);
            		noble.sendPacket(sm);
            		return false;
            	}
        	}
        	
        }
        
        if (classBased && getNoblePoints(noble.getObjectId()) < 3)
        {
            noble.sendMessage("Cant register when you have less than 3 points");
            return false;
        }
        if (!classBased && getNoblePoints(noble.getObjectId()) < 5)
        {
            noble.sendMessage("Cant register when you have less than 5 points");
            return false;
        }

        if (classBased)
        {
            if (_classBasedRegisters.containsKey(noble.getClassId().getId()))
            {
            	L2FastList<L2PcInstance> classed = _classBasedRegisters.get(noble.getClassId().getId());
                classed.add(noble);

                _classBasedRegisters.remove(noble.getClassId().getId());
                _classBasedRegisters.put(noble.getClassId().getId(), classed);

                sm = new SystemMessage(SystemMessageId.YOU_HAVE_BEEN_REGISTERED_IN_A_WAITING_LIST_OF_CLASSIFIED_GAMES);
                noble.sendPacket(sm);
            }
            else
            {
            	L2FastList<L2PcInstance> classed = new L2FastList<L2PcInstance>();
                classed.add(noble);

                _classBasedRegisters.put(noble.getClassId().getId(), classed);

                sm = new SystemMessage(SystemMessageId.YOU_HAVE_BEEN_REGISTERED_IN_A_WAITING_LIST_OF_CLASSIFIED_GAMES);
                noble.sendPacket(sm);

            }
        }
        else
        {
        	_nonClassBasedRegisters.add(noble);
            sm = new SystemMessage(SystemMessageId.YOU_HAVE_BEEN_REGISTERED_IN_A_WAITING_LIST_OF_NO_CLASS_GAMES);
            noble.sendPacket(sm);
        }

        return true;
    }

    public boolean isRegistered(L2PcInstance noble)
    {
    	if(_nonClassBasedRegisters == null) return false;
    	if(_classBasedRegisters == null) return false;
    	if (!_nonClassBasedRegisters.contains(noble))
        {
        	if (!_classBasedRegisters.containsKey(noble.getClassId().getId()))
            {
        		return false;
            }
            else
            {
            	L2FastList<L2PcInstance> classed = _classBasedRegisters.get(noble.getClassId().getId());
                if (!classed.contains(noble))
                {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean unRegisterNoble(L2PcInstance noble)
    {
        SystemMessage sm;
        /*
    	if (_compStarted)
    	{
    		noble.sendMessage("Cant Unregister whilst competition is under way");
    		return false;
    	}
    	*/

        if (!_inCompPeriod)
        {
            sm = new SystemMessage(SystemMessageId.THE_OLYMPIAD_GAME_IS_NOT_CURRENTLY_IN_PROGRESS);
            noble.sendPacket(sm);
            return false;
        }

    	if (!noble.isNoble())
        {
            sm = new SystemMessage(SystemMessageId.ONLY_NOBLESS_CAN_PARTICIPATE_IN_THE_OLYMPIAD);
            noble.sendPacket(sm);
            return false;
        }

    	if (!isRegistered(noble))
    	{
    		sm = new SystemMessage(SystemMessageId.YOU_HAVE_NOT_BEEN_REGISTERED_IN_A_WAITING_LIST_OF_A_GAME);
            noble.sendPacket(sm);
            return false;
        }

        if (_nonClassBasedRegisters.contains(noble))
            _nonClassBasedRegisters.remove(noble);
        else
        {
        	L2FastList<L2PcInstance> classed = _classBasedRegisters.get(noble.getClassId().getId());
            classed.remove(noble);

            _classBasedRegisters.remove(noble.getClassId().getId());
            _classBasedRegisters.put(noble.getClassId().getId(), classed);
        }
        
        for(L2OlympiadGame game: _manager.getOlympiadGames().values())
        {
        	if (game == null)
        		continue;
        	
        	if(game._playerOne.getObjectId() == noble.getObjectId() || game._playerTwo.getObjectId() == noble.getObjectId())
        	{
        		noble.sendMessage("Cant Unregister whilst you are already selected for a game");
        		return false;
        	}
        }

        sm = new SystemMessage(SystemMessageId.YOU_HAVE_BEEN_DELETED_FROM_THE_WAITING_LIST_OF_A_GAME);
        noble.sendPacket(sm);

    	return true;
    }
    
    public void removeDisconnectedCompetitor(L2PcInstance player)
    {
        if (_manager == null || (_manager.getOlympiadInstance(player.getOlympiadGameId()) == null)) return;

        _manager.getOlympiadInstance(player.getOlympiadGameId()).handleDisconnect(player);
    }

    public void notifyCompetitorDamage(int objId, int damage, int gameId)
    {
    	if (_manager.getOlympiadGames().get(gameId) == null) return;

    	if (_manager.getOlympiadGames().get(gameId)._playerOneID == objId)
    		_manager.getOlympiadGames().get(gameId)._damageP1 += damage;
    	else _manager.getOlympiadGames().get(gameId)._damageP2 += damage;
    }

    private void updateCompStatus()
    {
    	//_compStarted = false;

        synchronized(this)
        {
            long milliToStart = getMillisToCompBegin();

            double numSecs = (milliToStart / 1000) % 60;
            double countDown = ((milliToStart / 1000) - numSecs) / 60;
            int numMins = (int) Math.floor(countDown % 60);
            countDown = (countDown - numMins) / 60;
            int numHours = (int) Math.floor(countDown % 24);
            int numDays = (int) Math.floor((countDown - numHours) / 24);

            _log.info("Olympiad System: Competition Period Starts in "
                      + numDays + " days, " + numHours
                + " hours and " + numMins + " mins.");

            _log.info("Olympiad System: Event starts/started : " + _compStart.getTime());
        }

    	_scheduledCompStart = ThreadPoolManager.getInstance().scheduleGeneral(new Runnable(){
    		public void run()
    		{
                if (isOlympiadEnd())
                    return;

    			_inCompPeriod = true;
    			OlympiadManager om = new OlympiadManager();

                Announcements.getInstance().announceToAll(new SystemMessage(SystemMessageId.THE_OLYMPIAD_GAME_HAS_STARTED));
                _log.info("Olympiad System: Olympiad Game Started");

                Thread olyCycle = new Thread(om);
                olyCycle.start();
                
                //_scheduledManagerTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(om, INITIAL_WAIT, BATTLE_WAIT);
   
    			_scheduledCompEnd = ThreadPoolManager.getInstance().scheduleGeneral(new Runnable(){
    				public void run()
    				{
                        if (isOlympiadEnd())
                            return;
                        //_scheduledManagerTask.cancel(true);
    					_inCompPeriod = false;
                        Announcements.getInstance().announceToAll(new SystemMessage(SystemMessageId.THE_OLYMPIAD_GAME_HAS_ENDED));
                        _log.info("Olympiad System: Olympiad Game Ended");

                        try {
                        	while(_battleStarted){
	                        	try{
	                        		//wait 1 minutes for end of pendings games
	        		    			Thread.sleep(60000);
	        		    		}catch (InterruptedException e){}
                        	}
                            save();
                        }
                        catch (Exception e) {
                            _log.warning("Olympiad System: Failed to save Olympiad configuration: " + e);
                        }

                        init();
    				}
    				}, getMillisToCompEnd());
    		}
    		}, getMillisToCompBegin());
    }

    private long getMillisToOlympiadEnd()
    {
        //if (_olympiadEnd > Calendar.getInstance().getTimeInMillis())
            return (_olympiadEnd - Calendar.getInstance().getTimeInMillis());
        //return 10L;
    }

    public void manualSelectHeroes()
    {
        SystemMessage sm = new SystemMessage(SystemMessageId.OLYMPIAD_PERIOD_S1_HAS_ENDED);
        sm.addNumber(_currentCycle);

        Announcements.getInstance().announceToAll(sm);
        Announcements.getInstance().announceToAll("Olympiad Validation Period has began");

        _isOlympiadEnd = true;
        if (_scheduledManagerTask != null)
            _scheduledManagerTask.cancel(true);
        if (_scheduledWeeklyTask != null)
            _scheduledWeeklyTask.cancel(true);
        if(_scheduledOlympiadEnd != null)
            _scheduledOlympiadEnd.cancel(true);

        Calendar validationEnd = Calendar.getInstance();
        _validationEnd = validationEnd.getTimeInMillis() + VALIDATION_PERIOD;

        saveNobleData();

        _period = 1;

        sortHerosToBe();

        giveHeroBonus();

        Hero.getInstance().computeNewHeroes(_heroesToBe);

        try {
            save();
        }
        catch (Exception e) {
            _log.warning("Olympiad System: Failed to save Olympiad configuration: " + e);
        }

        _scheduledValdationTask  = ThreadPoolManager.getInstance().scheduleGeneral(new Runnable() {
            public void run()
            {
                Announcements.getInstance().announceToAll("Olympiad Validation Period has ended");
                _period = 0;
                _currentCycle++;
                deleteNobles();
                setNewOlympiadEnd();
                init();
            }
        }, getMillisToValidationEnd());
    }

    protected long getMillisToValidationEnd()
    {
        if (_validationEnd > Calendar.getInstance().getTimeInMillis())
            return (_validationEnd - Calendar.getInstance().getTimeInMillis());
        return 10L;
    }

    public boolean isOlympiadEnd()
    {
    	return _isOlympiadEnd;
    }

    protected void setNewOlympiadEnd()
    {
        SystemMessage sm = new SystemMessage(SystemMessageId.OLYMPIAD_PERIOD_S1_HAS_STARTED);
        sm.addNumber(_currentCycle);

        Announcements.getInstance().announceToAll(sm);

        Calendar currentTime = Calendar.getInstance();
        currentTime.add(Calendar.MONTH, 1);
        currentTime.set(Calendar.DAY_OF_MONTH, 1);
        currentTime.set(Calendar.AM_PM, Calendar.AM);
        currentTime.set(Calendar.HOUR, 12);
        currentTime.set(Calendar.MINUTE, 0);
        currentTime.set(Calendar.SECOND, 0);
        _olympiadEnd = currentTime.getTimeInMillis();

        Calendar nextChange = Calendar.getInstance();
        _nextWeeklyChange = nextChange.getTimeInMillis() + WEEKLY_PERIOD;

        _isOlympiadEnd = false;
    }

    public boolean inCompPeriod()
    {
    	return _inCompPeriod;
    }

    private long getMillisToCompBegin()
    {
        if (_compStart.getTimeInMillis() < Calendar.getInstance().getTimeInMillis() &&
                _compEnd > Calendar.getInstance().getTimeInMillis())
            return 10L;

        if (_compStart.getTimeInMillis() > Calendar.getInstance().getTimeInMillis())
            return (_compStart.getTimeInMillis() - Calendar.getInstance().getTimeInMillis());

        return setNewCompBegin();
    }

    private long setNewCompBegin()
    {
        _compStart = Calendar.getInstance();
        _compStart.set(Calendar.HOUR_OF_DAY, COMP_START);
        _compStart.set(Calendar.MINUTE, COMP_MIN);
        _compStart.add(Calendar.HOUR_OF_DAY, 24);
        _compEnd = _compStart.getTimeInMillis() + COMP_PERIOD;

        _log.info("Olympiad System: New Schedule @ " + _compStart.getTime());

        return (_compStart.getTimeInMillis() - Calendar.getInstance().getTimeInMillis());
    }

    protected long getMillisToCompEnd()
    {
        //if (_compEnd > Calendar.getInstance().getTimeInMillis())
            return (_compEnd - Calendar.getInstance().getTimeInMillis());
        //return 10L;
    }

    private long getMillisToWeekChange()
    {
        if (_nextWeeklyChange > Calendar.getInstance().getTimeInMillis())
            return (_nextWeeklyChange - Calendar.getInstance().getTimeInMillis());
        return 10L;
    }

    private void scheduleWeeklyChange()
    {
    	_scheduledWeeklyTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new Runnable() {
    		public void run()
    		{
    			addWeeklyPoints();
                _log.info("Olympiad System: Added weekly points to nobles");

                Calendar nextChange = Calendar.getInstance();
                _nextWeeklyChange = nextChange.getTimeInMillis() + WEEKLY_PERIOD;
    		}
    	}, getMillisToWeekChange(), WEEKLY_PERIOD);
    }

    protected synchronized void addWeeklyPoints()
    {
        if (_period == 1)
            return;

        for (Integer nobleId : _nobles.keySet())
        {
            StatsSet nobleInfo = _nobles.get(nobleId);
            int currentPoints = nobleInfo.getInteger(POINTS);
            currentPoints += WEEKLY_POINTS;
            nobleInfo.set(POINTS, currentPoints);

            _nobles.remove(nobleId);
            _nobles.put(nobleId, nobleInfo);
        }
    }

    public String[] getMatchList()
    {
    	return (_manager == null)? null : _manager.getAllTitles();
    }

    // returns the players for the given olympiad game Id 
    public L2PcInstance[] getPlayers(int Id)
    {
        if (_manager == null || (_manager.getOlympiadInstance(Id) == null))
        {
            return null;
        }

        L2PcInstance[] players = _manager.getOlympiadInstance(Id).getPlayers();

        return players;
    }

    public int getCurrentCycle()
    {
        return _currentCycle;
    }

    public void addSpectator(int id, L2PcInstance spectator)
    {
    	for(L2PcInstance player : _nonClassBasedRegisters)
    	{
    		if(spectator.getObjectId() == player.getObjectId())
    		{
    			spectator.sendMessage("You are already registered for a competition");
        		return;
    		}	
    	}
    	for(L2FastList<L2PcInstance> list : _classBasedRegisters.values())
    	{
    		for(L2PcInstance player : list)
    		{
    			if(spectator.getObjectId() == player.getObjectId())
        		{
        			spectator.sendMessage("You are already registered for a competition");
            		return;
        		}	
    		}
    	}
    	if(spectator.getOlympiadGameId() != -1)
    	{
    		spectator.sendMessage("You are already registered for a competition");
    		return;
    	}
        if (_manager == null || (_manager.getOlympiadInstance(id) == null))
        {
            spectator.sendPacket(new SystemMessage(SystemMessageId.THE_OLYMPIAD_GAME_IS_NOT_CURRENTLY_IN_PROGRESS));
            return;
        }

    	L2PcInstance[] players = _manager.getOlympiadInstance(id).getPlayers();

        if (players == null) return;

        spectator.enterOlympiadObserverMode(STADIUMS[id].getCoordinates()[0], STADIUMS[id].getCoordinates()[1], STADIUMS[id].getCoordinates()[2], id);

        _manager.getOlympiadInstance(id).addSpectator(spectator);
    }

	
	public void UnSpawnBuffer(L2Spawn spawn)
	{
		spawn.getLastSpawn().decayMe();
	}

    public void removeSpectator(int id, L2PcInstance spectator)
    {
        if (_manager == null || (_manager.getOlympiadInstance(id) == null)) return;

        _manager.getOlympiadInstance(id).removeSpectator(spectator);
    }

    public L2FastList<L2PcInstance> getSpectators(int id)
    {
        if (_manager == null || _manager.getOlympiadInstance(id) == null) 
            return null;
        return _manager.getOlympiadInstance(id).getSpectators();
    }

    public Map<Integer, L2OlympiadGame> getOlympiadGames()
    {
    	return _manager.getOlympiadGames();
    }

    public boolean playerInStadia(L2PcInstance player)
    {
        return (OlympiadStadiaManager.getInstance().getStadium(player) != null);
    }

    public int[] getWaitingList()
    {
        int[] array = new int[2];

        if (!inCompPeriod())
            return null;

        int classCount = 0;

        if (_classBasedRegisters.size() != 0)
            for (L2FastList<L2PcInstance> classed : _classBasedRegisters.values())
            {
                classCount += classed.size();
            }

        array[0] = classCount;
        array[1] = _nonClassBasedRegisters.size();

        return array;
    }

    protected synchronized void saveNobleData()
    {
        Connection con = null;

        if (_nobles == null)
            return;

        try
        {
            con = L2DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement;

            for (Integer nobleId : _nobles.keySet())
            {
                StatsSet nobleInfo = _nobles.get(nobleId);
                
                if (nobleInfo == null)
                	continue;

                int charId = nobleId;
                int classId = nobleInfo.getInteger(CLASS_ID);
                String charName = nobleInfo.getString(CHAR_NAME);
                int points = nobleInfo.getInteger(POINTS);
                int compDone = nobleInfo.getInteger(COMP_DONE);
                boolean toSave = nobleInfo.getBool("to_save");

                if (toSave)
                {
                    statement = con.prepareStatement(OLYMPIAD_SAVE_NOBLES);
                    statement.setInt(1, charId);
                    statement.setInt(2, classId);
                    statement.setString(3, charName);
                    statement.setInt(4, points);
                    statement.setInt(5, compDone);
                    statement.execute();

                    statement.close();

                    nobleInfo.set("to_save", false);

                    _nobles.remove(nobleId);
                    _nobles.put(nobleId, nobleInfo);
                }
                else
                {
                    statement = con.prepareStatement(OLYMPIAD_UPDATE_NOBLES);
                    statement.setInt(1, points);
                    statement.setInt(2, compDone);
                    statement.setInt(3, charId);
                    statement.execute();
                    statement.close();
                }
            }
        }
        catch(SQLException e) {_log.warning("Olympiad System: Couldnt save nobles info in db");}
        finally
        {
            try{con.close();}catch(Exception e){e.printStackTrace();}
        }
    }

    protected void sortHerosToBe()
    {
        if (_period != 1) return;

         _heroesToBe = new L2FastList<StatsSet>();

         Connection con = null;

         try
         {
             con = L2DatabaseFactory.getInstance().getConnection();
             PreparedStatement statement;
             ResultSet rset;
             StatsSet hero;

             for (int i = 0; i < HERO_IDS.length; i++)
             {
                 statement = con.prepareStatement(OLYMPIAD_GET_HEROS);
                 statement.setInt(1, HERO_IDS[i]);
                 rset = statement.executeQuery();

                 if (rset.next())
                 {
                     hero = new StatsSet();
                     hero.set(CLASS_ID, HERO_IDS[i]);
                     hero.set(CHAR_ID, rset.getInt(CHAR_ID));
                     hero.set(CHAR_NAME, rset.getString(CHAR_NAME));

                     _heroesToBe.add(hero);
                 }

                 statement.close();
                 rset.close();
             }
         }
         catch(SQLException e){_log.warning("Olympiad System: Couldnt heros from db");}
         finally
         {
             try{con.close();}catch(Exception e){e.printStackTrace();}
         }

    }

    public L2FastList<String> getClassLeaderBoard(int classId)
    {
        //if (_period != 1) return;

    	L2FastList<String> names = new L2FastList<String>();

         Connection con = null;

         try
         {
             con = L2DatabaseFactory.getInstance().getConnection();
             PreparedStatement statement;
             ResultSet rset;
             statement = con.prepareStatement(GET_EACH_CLASS_LEADER);
             statement.setInt(1, classId);
             rset = statement.executeQuery();

             while (rset.next())
             {
                 names.add(rset.getString(CHAR_NAME));
             }

             statement.close();
             rset.close();

             return names;
         }
         catch(SQLException e){_log.warning("Olympiad System: Couldnt heros from db");}
         finally
         {
             try{con.close();}catch(Exception e){e.printStackTrace();}
         }

         return names;

    }

    protected void giveHeroBonus()
    {
        if (_heroesToBe.size() == 0)
            return;

        for (StatsSet hero : _heroesToBe)
        {
            int charId = hero.getInteger(CHAR_ID);

            StatsSet noble = _nobles.get(charId);
            int currentPoints = noble.getInteger(POINTS);
            currentPoints += Config.ALT_OLY_HERO_POINTS;
            noble.set(POINTS, currentPoints);

            _nobles.remove(charId);
            _nobles.put(charId, noble);
        }
    }

    public int getNoblessePasses(int objId)
    {
        if (_period != 1 || _nobles.size() == 0)
            return 0;

        StatsSet noble = _nobles.get(objId);
        if (noble == null)
            return 0;
        int points = noble.getInteger(POINTS);
        if (points <= Config.ALT_OLY_MIN_POINT_FOR_EXCH)
            return 0;

        noble.set(POINTS, 0);
        _nobles.remove(objId);
        _nobles.put(objId, noble);

        points *= Config.ALT_OLY_GP_PER_POINT;

        return points;
    }

    public boolean isRegisteredInComp(L2PcInstance player)
    {
        boolean result = false;

        if (_nonClassBasedRegisters != null && _nonClassBasedRegisters.contains(player))
            result = true;

        else if (_classBasedRegisters != null && _classBasedRegisters.containsKey(player.getClassId().getId()))
        {
        	L2FastList<L2PcInstance> classed = _classBasedRegisters.get(player.getClassId().getId());
            if (classed.contains(player))
                result = true;
        }
        if (!_inCompPeriod){}
        else {
        	for(L2OlympiadGame game: _manager.getOlympiadGames().values())
        	{
        		if(game._playerOne.getObjectId() == player.getObjectId() || game._playerTwo.getObjectId() == player.getObjectId())
        		{
        			result = true;
        			break;
        		}
        	}
        }

        return result;
    }

    public int getNoblePoints(int objId)
    {
        if (_nobles.size() ==  0)
            return 0;

        StatsSet noble = _nobles.get(objId);
        if (noble == null)
            return 0;
        int points = noble.getInteger(POINTS);

        return points;
    }

    public int getCompetitionDone(int objId)
    {
        if (_nobles.size() ==  0)
            return 0;

        StatsSet noble = _nobles.get(objId);
        if (noble == null)
            return 0;
        int points = noble.getInteger(COMP_DONE);

        return points;
    }

    protected void deleteNobles()
    {
        Connection con = null;

        try
        {
            con = L2DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement = con.prepareStatement(OLYMPIAD_DELETE_ALL);
            statement.execute();
            statement.close();
        }
        catch(SQLException e){_log.warning("Olympiad System: Couldnt delete nobles from db");}
        finally
        {
            try{con.close();}catch(Exception e){e.printStackTrace();}
        }

        _nobles.clear();
    }

    public void save() throws IOException
    {
        saveNobleData();

        Properties OlympiadProperties = new Properties();
        FileOutputStream fos = new FileOutputStream(new File(Config.DATAPACK_ROOT, OLYMPIAD_DATA_FILE));

        OlympiadProperties.setProperty("CurrentCycle", String.valueOf(_currentCycle));
        OlympiadProperties.setProperty("Period", String.valueOf(_period));
        OlympiadProperties.setProperty("OlympiadEnd", String.valueOf(_olympiadEnd));
        OlympiadProperties.setProperty("ValdationEnd", String.valueOf(_validationEnd));
        OlympiadProperties.setProperty("NextWeeklyChange", String.valueOf(_nextWeeklyChange));

        OlympiadProperties.store(fos, "Olympiad Properties");
        fos.close();
    }

    private class OlympiadManager implements Runnable
    {
    	private Map<Integer, L2OlympiadGame> _olympiadInstances;

    	public OlympiadManager()
    	{
    		_olympiadInstances = new FastMap<Integer, L2OlympiadGame>();
    		_manager = this;
    	}

    	public synchronized void run()
    	{
    		_cycleTerminated = false;
    		if (isOlympiadEnd())
    		{
    			_scheduledManagerTask.cancel(true);
    			_cycleTerminated = true;
    			return;
    		}
    		Map<Integer, OlympiadGameTask> _gamesQueue = new FastMap<Integer, OlympiadGameTask>();
    		while(inCompPeriod())
    		{
    			if (_nobles.size() == 0)
    			{
    				try{
    					wait(60000);
    				}catch(InterruptedException ex){}
    				continue;
    			}
    			
    			//_compStarted = true;
    			int classBasedPgCount = 0;
    			for(L2FastList<L2PcInstance> classList : _classBasedRegisters.values())
    				classBasedPgCount += classList.size();
    			while((_gamesQueue.size() > 0 || classBasedPgCount >= Config.ALT_OLY_CLASSED || _nonClassBasedRegisters.size() >= Config.ALT_OLY_NONCLASSED) && inCompPeriod())
    			{
    				//first cycle do nothing
    				int _gamesQueueSize = 0;
    				_gamesQueueSize = _gamesQueue.size();
    				for(int i=0; i<_gamesQueueSize;i++)
					{
						if(_gamesQueue.get(i) == null || _gamesQueue.get(i).isTerminated() || _gamesQueue.get(i)._game == null)
						{
							if(_gamesQueue.containsKey(i)) {
								//removes terminated games from the queue
									try {
									_olympiadInstances.remove(i);
									_gamesQueue.remove(i);
									STADIUMS[i].setStadiaFree();
								} catch(Exception e) {e.printStackTrace();}
							} else {
								_gamesQueueSize = _gamesQueueSize+1;
							}
						}else if(_gamesQueue.get(i) != null && !_gamesQueue.get(i).isStarted())
						{
							//start new games
							Thread T = new Thread(_gamesQueue.get(i));
							T.start();
						}
					}
    				//set up the games queue   				
    	            for(int i=0; i<STADIUMS.length; i++)
    				{
    	            	if(!existNextOpponents(_nonClassBasedRegisters) && !existNextOpponents(getRandomClassList(_classBasedRegisters)))
						{
							break;
						}
    					if(STADIUMS[i].isFreeToUse())
    					{
    						if (existNextOpponents(_nonClassBasedRegisters))
    		                {	
    							try{
	    		                    _olympiadInstances.put(i, new L2OlympiadGame(i, COMP_TYPE.NON_CLASSED, nextOpponents(_nonClassBasedRegisters), STADIUMS[i].getCoordinates()));
	    		                    _gamesQueue.put(i,new OlympiadGameTask(_olympiadInstances.get(i)));
	    		                    STADIUMS[i].setStadiaBusy();
    							}catch(Exception ex)
    							{
    								if(_olympiadInstances.get(i) != null){
    									for(L2PcInstance player : _olympiadInstances.get(i).getPlayers())
    									{
    										player.sendMessage("Your olympiad registration was canceled due to an error");
    										player.setIsInOlympiadMode(false);
    						    			player.setIsOlympiadStart(false);
    						                player.setOlympiadSide(-1);
    						                player.setOlympiadGameId(-1);
    									}
    									_olympiadInstances.remove(i);
    								}
    								if(_gamesQueue.get(i)!= null)
    									_gamesQueue.remove(i);
    								STADIUMS[i].setStadiaFree();
    								
    								//try to reuse this stadia next time
    								i--;
    							}
    		                }
    						else if (existNextOpponents(getRandomClassList(_classBasedRegisters)))
    		                {
    							try{
    								_olympiadInstances.put(i, new L2OlympiadGame(i, COMP_TYPE.CLASSED, nextOpponents(getRandomClassList(_classBasedRegisters)), STADIUMS[i].getCoordinates()));
	    		                    _gamesQueue.put(i,new OlympiadGameTask(_olympiadInstances.get(i)));
	    		                    STADIUMS[i].setStadiaBusy();
    							}catch(Exception ex)
    							{
    								if(_olympiadInstances.get(i) != null){
    									for(L2PcInstance player : _olympiadInstances.get(i).getPlayers())
    									{
    										player.sendMessage("Your olympiad registration was canceled due to an error");
    										player.setIsInOlympiadMode(false);
    						    			player.setIsOlympiadStart(false);
    						                player.setOlympiadSide(-1);
    						                player.setOlympiadGameId(-1);
    									}
    									_olympiadInstances.remove(i);
    								}
    								if(_gamesQueue.get(i)!= null)
    									_gamesQueue.remove(i);
    								STADIUMS[i].setStadiaFree();
    								
    								//try to reuse this stadia next time
    								i--;
    							}
    		                }
    					}
    				}
    	            //wait 30 sec for !stress the server
    	            try{
    	    			wait(30000);
    	    		}catch (InterruptedException e){}
    			}
    			//wait 30 sec for !stress the server
	            try{
	    			wait(30000);
	    		}catch (InterruptedException e){}
    		}

    		//when comp time finish wait for all games terminated before execute the cleanup code
    		boolean allGamesTerminated = false;
    		//wait for all games terminated
    		while(!allGamesTerminated)
    		{
     			try{
	    			wait(30000);
	    		}catch (InterruptedException e){}
	    		
	    		if(_gamesQueue.size() == 0)
	    		{
	    			allGamesTerminated = true;
	    		}else{
		    		for(OlympiadGameTask game : _gamesQueue.values())
		    		{
		    			allGamesTerminated = allGamesTerminated || game.isTerminated();
		    		}
	    		}
     		}
    		_cycleTerminated = true;
    		//when all games terminated clear all 
    		_gamesQueue.clear();
    		/*_classBasedParticipants.clear();
    		_nonClassBasedParticipants.clear();*/
    		//Wait 20 seconds
    		_olympiadInstances.clear();
            _classBasedRegisters.clear();
            _nonClassBasedRegisters.clear();

            _battleStarted = false;
    		//_compStarted = false;

    	}

    	protected L2OlympiadGame getOlympiadInstance(int index)
    	{
    		if (_olympiadInstances != null && _olympiadInstances.size() > 0)
    		{
    			return _olympiadInstances.get(index);
    		}
    		return null;
    	}

    	protected Map<Integer, L2OlympiadGame> getOlympiadGames()
    	{
    		return (_olympiadInstances == null)? null : _olympiadInstances;
    	}
    	
    	private L2FastList<L2PcInstance> getRandomClassList(Map<Integer, L2FastList<L2PcInstance>> list)
    	{
    		if(list.size() == 0)
    			return null;
    		
    		Map<Integer, L2FastList<L2PcInstance>> tmp = new FastMap<Integer, L2FastList<L2PcInstance>>();
    		int tmpIndex = 0;
    		for(L2FastList<L2PcInstance> l : list.values())
    		{
    			tmp.put(tmpIndex++, l);
    		}
    		
    		L2FastList<L2PcInstance> rndList = new L2FastList<L2PcInstance>();
    		int classIndex = 0;
    		if(tmp.size() == 1)
    			classIndex = 0;
    		else
    			classIndex = Rnd.nextInt(tmp.size());
    		rndList = tmp.get(classIndex);
    		return rndList;
    	}
    	private L2FastList<L2PcInstance> nextOpponents(L2FastList<L2PcInstance> list)
    	{
    		L2FastList<L2PcInstance> opponents = new L2FastList<L2PcInstance>();
            if (list.size() == 0)
                return opponents;
    		int loopCount = (list.size() / 2);
    		
    		int first;
    		int second;
    		
    		if (loopCount < 1)
    			return opponents;
    			
			first = Rnd.nextInt(list.size());
			opponents.add(list.get(first));
			list.remove(first);
			
			second = Rnd.nextInt(list.size());
			opponents.add(list.get(second));
			list.remove(second);
			
			return opponents;
        
        }
    	private boolean existNextOpponents(L2FastList<L2PcInstance> list)
    	{
    		if(list == null)
    			return false;
            if (list.size() == 0)
                return false;
    		int loopCount = list.size() >> 1;
    		
    		if (loopCount < 1)
    			return false;
    		else
    			return true;
    		
        
        }

    	protected String[] getAllTitles()
    	{
    		/*if(!_compStarted)
    			return null;*/

    		String[] msg = new String[_olympiadInstances.size()];
    		int count = 0;
    		int match = 1;
    		int showbattle = 0;

    		for (L2OlympiadGame instance : _olympiadInstances.values())
    		{
    			if (instance._gamestarted == true) { showbattle = 1; } else { showbattle = 0; }
    			msg[count++] = "<"+showbattle+"><"+instance._stadiumID+"> In Progress " + instance.getTitle();
    			match++;
    		}

    		return msg;
    	}
    }

    private class L2OlympiadGame
    {
    	protected COMP_TYPE _type;
    	public boolean _aborted;
    	public boolean _gamestarted;
    	public boolean _playerOneDisconnected;
    	public boolean _playerTwoDisconnected;
    	public String _playerOneName;
    	public String _playerTwoName;
    	public int _playerOneID = 0;
    	public int _playerTwoID = 0;
    	public static final int OLY_BUFFER = 36402;

    	public int _damageP1 = 0;
    	public int _damageP2 = 0;

    	public L2PcInstance _playerOne;
    	public L2PcInstance _playerTwo;
    	public L2Spawn _spawnOne;
    	public L2Spawn _spawnTwo;
    	private L2FastList<L2PcInstance> _players;
        private int[] _stadiumPort;
        private int x1, y1, z1, x2, y2, z2;
        public int _stadiumID;
        public L2FastList<L2PcInstance> _spectators;
        private SystemMessage _sm;
        private SystemMessage _sm2;
        private SystemMessage _sm3;

    	protected L2OlympiadGame(int id, COMP_TYPE type, L2FastList<L2PcInstance> list, int[] stadiumPort)
    	{
    		_aborted = false;
    		_gamestarted = false;
    		_stadiumID = id;
    		_playerOneDisconnected = false;
    		_playerTwoDisconnected = false;
    		_type = type;
            _stadiumPort = stadiumPort;
            _spectators = new L2FastList<L2PcInstance>();

    		if (list != null)
    		{
    			_players = list;
    			_playerOne = list.get(0);
    			_playerTwo = list.get(1);

                try {
                	_playerOneName = _playerOne.getName();
                	_playerTwoName = _playerTwo.getName();
                	_playerOne.setOlympiadGameId(id);
                	_playerTwo.setOlympiadGameId(id);
                	_playerOneID = _playerOne.getObjectId();
                	_playerTwoID = _playerTwo.getObjectId();
                }
                catch (Exception e)  {
                	_aborted = true;
                	clearPlayers();
                }
                
                if (Config.DEBUG)
                	_log.info("Olympiad System: Game - " + id + ": " + _playerOne.getName() + " Vs " + _playerTwo.getName());
    		}
            else {
            	_aborted = true;
            	clearPlayers();
            	return;
            }
    	}
    	
    	public boolean isAborted()
    	{
    	    return _aborted;
    	}
    	
        protected void clearPlayers()
        {
            _playerOne = null;
            _playerTwo = null;
            _players = null;
            _playerOneName = "";
            _playerTwoName = "";
            _playerOneID = 0;
            _playerTwoID = 0;
        }
        
        protected void handleDisconnect(L2PcInstance player)
        {
            if (player == _playerOne)
                _playerOneDisconnected = true;
            else if (player == _playerTwo)
                _playerTwoDisconnected = true;
        }
        
		public void openDoors()
		{
			_doorTable = DoorTable.getInstance();
			try {
				_doorTable.getDoor(STADIUMS[_stadiumID].getDoorID()[0]).openMe();
				_doorTable.getDoor(STADIUMS[_stadiumID].getDoorID()[1]).openMe();
				//_log.info("open doors"+STADIUMS[_stadiumID].getDoorID()[0]+"<>"+STADIUMS[_stadiumID].getDoorID()[1]);
			} catch (Exception e) {}
		}
		
		public void closeDoors()
		{
			_doorTable = DoorTable.getInstance();
			try {
				_doorTable.getDoor(STADIUMS[_stadiumID].getDoorID()[0]).closeMe();
				_doorTable.getDoor(STADIUMS[_stadiumID].getDoorID()[1]).closeMe();
				//_log.info("close doors"+STADIUMS[_stadiumID].getDoorID()[0]+"<>"+STADIUMS[_stadiumID].getDoorID()[1]);
			} catch (Exception e) {}	
		}
		
		public L2Spawn SpawnBuffer(int xPos, int yPos, int zPos, int npcId)
		{
		        L2NpcTemplate template;
		        template = NpcTable.getInstance().getTemplate(npcId);
		        try {
		        	L2Spawn spawn = new L2Spawn(template);
		        	spawn.setLocx(xPos);
		        	spawn.setLocy(yPos);
		        	spawn.setLocz(zPos);
		        	spawn.setAmount(1);
		        	spawn.setHeading(0);
		        	spawn.setRespawnDelay(1);					
		        	SpawnTable.getInstance().addNewSpawn(spawn, false);
		        	spawn.init();
		        	return spawn;
		        } catch (Exception e) { return null; }		     
		}

    	protected void removals()
    	{
    		if (_aborted) return;

            if (_playerOne == null || _playerTwo == null) return;
            if (_playerOneDisconnected  || _playerTwoDisconnected) return;
            
    		for (L2PcInstance player : _players)
    		{
    		  try{
    			//Remove Clan Skills
				if (player.getClan() != null)
				{
					for(L2Skill skill: player.getClan().getAllSkills())
						player.removeSkill(skill,false);
				}
				//Abort casting if player casting  
				if (player.isCastingNow())
				{
					player.abortCast();  
				}
				
				//Force the character to be vissible
				player.getAppearance().setVisible();

				//Remove Hero Skills
				if (player.isHero())
				{
					for(L2Skill skill: HeroSkillTable.getHeroSkills())
						player.removeSkill(skill,false);
				}
				
				// Heal Player fully
				player.setCurrentCp(player.getMaxCp());
				player.setCurrentHp(player.getMaxHp());
				player.setCurrentMp(player.getMaxMp());
				
				//Remove Buffs
				player.stopAllEffects();
				
				//Remove Summon's Buffs
				if (player.getPet() != null)
				{
					L2Summon summon = player.getPet();
					summon.stopAllEffects();
					
					if (summon instanceof L2PetInstance)
						summon.unSummon(player);
		    	}
                
                /*if (player.getCubics() != null)
                {
                    for(L2CubicInstance cubic : player.getCubics().values())
                    {
                        cubic.stopAction();
                        player.delCubic(cubic.getId());
                    }
                    player.getCubics().clear();
                }*/
				
				//Remove player from his party
				if (player.getParty() != null)
				{
					L2Party party = player.getParty();
					party.removePartyMember(player);
				}

				//Remove Hero Weapons
				// check to prevent the using of weapon/shield on strider/wyvern
				L2ItemInstance wpn = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
				if (wpn == null) wpn = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LRHAND);
                if (wpn != null && (wpn.isHeroItem() || wpn.isOlyRestrictedItem()))
                {
					L2ItemInstance[] unequiped = player.getInventory().unEquipItemInBodySlotAndRecord(wpn.getItem().getBodyPart());
					InventoryUpdate iu = new InventoryUpdate();
					for (int i = 0; i < unequiped.length; i++)
						iu.addModifiedItem(unequiped[i]);
					player.sendPacket(iu);
					player.abortAttack();
					player.broadcastUserInfo();

					// this can be 0 if the user pressed the right mouse button twice very fast
					if (unequiped.length > 0)
					{
						if (unequiped[0].isWear())
							return;
						SystemMessage sm = null;
						if (unequiped[0].getEnchantLevel() > 0){
							sm = new SystemMessage(SystemMessageId.EQUIPMENT_S1_S2_REMOVED);
							sm.addNumber(unequiped[0].getEnchantLevel());
							sm.addItemName(unequiped[0]);
						}else{
							sm = new SystemMessage(SystemMessageId.S1_DISARMED);
							sm.addItemName(unequiped[0]);
						}
						player.sendPacket(sm);
					}
				}

				//Remove shot automation
				Map<Integer, Integer> activeSoulShots = player.getAutoSoulShot();
				for (int itemId : activeSoulShots.values())
				{
 					player.removeAutoSoulShot(itemId);
					ExAutoSoulShot atk = new ExAutoSoulShot(itemId, 0);
					player.sendPacket(atk);
				}
				player.sendSkillList();
    		  }
    		  catch (Exception e) {}
    		}
    	}

    	protected boolean portPlayersToArena()
    	{
    		boolean _playerOneCrash = (_playerOne == null || _playerOneDisconnected);
    		boolean _playerTwoCrash = (_playerTwo == null || _playerTwoDisconnected);

    		if (_playerOneCrash || _playerTwoCrash || _aborted)
    		{
    			_playerOne=null;
    			_playerTwo=null;
    			_aborted = true;
    			return false;
    		}

    		try {
    			x1 = _playerOne.getX();
    			y1 = _playerOne.getY();
    			z1 = _playerOne.getZ();

    			x2 = _playerTwo.getX();
    			y2 = _playerTwo.getY();
    			z2 = _playerTwo.getZ();

    			if (_playerOne.isSitting())
    				_playerOne.standUp();

    			if (_playerTwo.isSitting())
    				_playerTwo.standUp();

    			_playerOne.setTarget(null);
    			_playerTwo.setTarget(null);

    			_playerOne.teleToLocation(_stadiumPort[0]+1200, _stadiumPort[1], _stadiumPort[2], false);
    			_playerTwo.teleToLocation(_stadiumPort[0]-1200, _stadiumPort[1], _stadiumPort[2], false);

    			_playerOne.sendPacket(new ExOlympiadMode(2));
    			_playerTwo.sendPacket(new ExOlympiadMode(2));

    			_spawnOne = SpawnBuffer(_stadiumPort[0]+1100, _stadiumPort[1], _stadiumPort[2], OLY_BUFFER);
    			_spawnTwo = SpawnBuffer(_stadiumPort[0]-1100, _stadiumPort[1], _stadiumPort[2], OLY_BUFFER);

    			_playerOne.setIsInOlympiadMode(true);
    			_playerOne.setIsOlympiadStart(false);
    			_playerOne.setOlympiadSide(1);
    			_playerOne.olyBuff = 5;

    			_playerTwo.setIsInOlympiadMode(true);
    			_playerTwo.setIsOlympiadStart(false);
    			_playerTwo.setOlympiadSide(2);
    			_playerTwo.olyBuff = 5;
    			_gamestarted = true;
    		} catch (NullPointerException e)
    		{ 
    		    return false; 
    		}
    		return true;
    	}
    	
    	protected void sendMessageToPlayers(boolean toBattleBegin, int nsecond)
    	{
            if(!toBattleBegin)
                _sm = new SystemMessage(SystemMessageId.YOU_WILL_ENTER_THE_OLYMPIAD_STADIUM_IN_S1_SECOND_S);
            else
                _sm = new SystemMessage(SystemMessageId.THE_GAME_WILL_START_IN_S1_SECOND_S);
            
            _sm.addNumber(nsecond);
            try {    		
            	for (L2PcInstance player : _players)
            		player.sendPacket(_sm);
            } catch (Exception e) {}
    	}
    	
    	protected void portPlayersBack()
    	{
    		if (_playerOne != null)
    			_playerOne.teleToLocation(x1, y1, z1, true);
    		
    		if (_playerTwo != null)
    		_playerTwo.teleToLocation(x2, y2, z2, true);
    	}
    	
    	protected void PlayersStatusBack()
    	{
    		for (L2PcInstance player : _players)
    		{
    		  try {
    			if(player.isDead() == true) { player.setIsDead(false); }
                player.getStatus().startHpMpRegeneration();
                player.setCurrentCp(player.getMaxCp());
                player.setCurrentHp(player.getMaxHp());
                player.setCurrentMp(player.getMaxMp());
    			player.setIsInOlympiadMode(false);
    			player.setIsOlympiadStart(false);
                player.setOlympiadSide(-1);
                player.setOlympiadGameId(-1);
                player.sendPacket(new ExOlympiadMode(0));

                //Add Clan Skills
				if (player.getClan() != null)
				{
					for(L2Skill skill: player.getClan().getAllSkills())
					{
						if ( skill.getMinPledgeClass() <= player.getPledgeClass() )
							player.addSkill(skill,false);
				 	}
				}

                //Add Hero Skills
				if (player.isHero())
				{
					for(L2Skill skill: HeroSkillTable.getHeroSkills())
						player.addSkill(skill,false);
				}
				player.sendSkillList();
    		  } catch (Exception e) {}
    		}
    	}

    	protected boolean haveWinner()
    	{
    		boolean retval = false;
			if (_aborted || _playerOne == null || _playerTwo == null)
			{					
				return true;
			}
 		
			double playerOneHp = 0;

			try{
				if (_playerOne != null && _playerOne.getOlympiadGameId() != -1)
				{
					playerOneHp = _playerOne.getCurrentHp();
				}
			}catch (Exception e){
				playerOneHp = 0;
			}

			double playerTwoHp = 0;
			try{
				if (_playerTwo != null && _playerTwo.getOlympiadGameId()!=-1)
				{
					playerTwoHp = _playerTwo.getCurrentHp();
				}
			}catch (Exception e){
				playerTwoHp = 0;
			}
            
            if (playerTwoHp==0 || playerOneHp==0)
    		{
            	return true;
            }
            
            return retval;
    	}
    	
     	protected void validateWinner()
    	{
			if (_aborted || _playerOne == null || _playerTwo == null || _playerOneDisconnected || _playerTwoDisconnected)
			{					
				return;
			}

    		StatsSet playerOneStat;
    		StatsSet playerTwoStat;
    		
    		playerOneStat = _nobles.get(_playerOneID);
			playerTwoStat = _nobles.get(_playerTwoID);
			
			int _div;
			int _gpreward;
			
			int playerOnePlayed = playerOneStat.getInteger(COMP_DONE);
			int playerTwoPlayed = playerTwoStat.getInteger(COMP_DONE);
			
			int playerOnePoints = playerOneStat.getInteger(POINTS);
			int playerTwoPoints = playerTwoStat.getInteger(POINTS);

			double playerOneHp = 0;
			try
			{
				if (_playerOne != null && !_playerOneDisconnected)
				{
					if (!_playerOne.isDead())
					{
						playerOneHp = _playerOne.getCurrentHp()+_playerOne.getCurrentCp();
					}
				}
			}
			catch (Exception e)
			{
				playerOneHp = 0;
			}

			double playerTwoHp = 0;
			try
			{
				if (_playerTwo != null && !_playerTwoDisconnected)
				{
					if (!_playerTwo.isDead())
					{
						playerTwoHp = _playerTwo.getCurrentHp()+_playerTwo.getCurrentCp();
					}
				}
			}
			catch (Exception e)
			{
				playerTwoHp = 0;
			}

            _sm = new SystemMessage(SystemMessageId.C1_HAS_WON_THE_GAME);
    		_sm2 = new SystemMessage(SystemMessageId.S1_HAS_GAINED_S2_OLYMPIAD_POINTS);
            _sm3 = new SystemMessage(SystemMessageId.S1_HAS_LOST_S2_OLYMPIAD_POINTS);

            String result = "";

            // if players crashed, search if they've relogged
            _playerOne =  L2World.getInstance().getPlayer(_playerOneName);
            _players.set(0, _playerOne);
            _playerTwo =  L2World.getInstance().getPlayer(_playerTwoName);
            _players.set(1, _playerTwo);

            switch(_type){
            	case NON_CLASSED: _div=5; _gpreward=Config.ALT_OLY_NONCLASSED_RITEM_C; break;
            	default: _div=3; _gpreward=Config.ALT_OLY_CLASSED_RITEM_C; break;
            }

            if (_playerOne == null && _playerTwo == null)
            {
            	result=" tie";
    			_sm = new SystemMessage(SystemMessageId.THE_GAME_ENDED_IN_A_TIE);
                broadcastMessage(_sm, true);
            }
            else if (_playerTwo == null || _playerTwo.isOnline() == 0 || (playerTwoHp == 0 && playerOneHp != 0) || (_damageP1 > _damageP2 && playerTwoHp != 0 && playerOneHp != 0))
    		{
            	int pointDiff;
            	pointDiff = playerTwoPoints / _div;
    			playerOneStat.set(POINTS, playerOnePoints + pointDiff);
    			playerTwoStat.set(POINTS, playerTwoPoints - pointDiff);

                _sm.addString(_playerOneName);
                broadcastMessage(_sm, true);
                _sm2.addString(_playerOneName);
                _sm2.addNumber(pointDiff);
                broadcastMessage(_sm2, true);
                _sm3.addString(_playerTwoName);
                _sm3.addNumber(pointDiff);
                broadcastMessage(_sm3, true);

                try {
        			result=" ("+playerOneHp+"hp vs "+playerTwoHp+"hp - "+_damageP1+"dmg vs "+_damageP2+"dmg) "+_playerOneName+" win "+pointDiff+" points";
        			L2ItemInstance item = _playerOne.getInventory().addItem("Olympiad", Config.ALT_OLY_BATTLE_REWARD_ITEM, _gpreward, _playerOne, null);
        			InventoryUpdate iu = new InventoryUpdate();
        			iu.addModifiedItem(item);
        			_playerOne.sendPacket(iu);

        			SystemMessage sm = new SystemMessage(SystemMessageId.EARNED_S2_S1_S);
        			sm.addItemName(item);
        			sm.addNumber(_gpreward);
        			_playerOne.sendPacket(sm);
                } catch (Exception e) { }
    		}
            else if (_playerOne == null || _playerOne.isOnline() == 0 || (playerOneHp == 0 && playerTwoHp != 0) || (_damageP2 > _damageP1 && playerOneHp != 0 && playerTwoHp != 0))
    		{
            	int pointDiff;
            	pointDiff = playerOnePoints / _div;
    			playerTwoStat.set(POINTS, playerTwoPoints + pointDiff);
    			playerOneStat.set(POINTS, playerOnePoints - pointDiff);

                _sm.addString(_playerTwoName);
                broadcastMessage(_sm, true);
                _sm2.addString(_playerTwoName);
                _sm2.addNumber(pointDiff);
                broadcastMessage(_sm2, true);
                _sm3.addString(_playerOneName);
                _sm3.addNumber(pointDiff);
                broadcastMessage(_sm3, true);

                try {
                	result=" ("+playerOneHp+"hp vs "+playerTwoHp+"hp - "+_damageP1+"dmg vs "+_damageP2+"dmg) "+_playerTwoName+" win "+pointDiff+" points";
                	L2ItemInstance item = _playerTwo.getInventory().addItem("Olympiad", Config.ALT_OLY_BATTLE_REWARD_ITEM, _gpreward, _playerTwo, null);
                	InventoryUpdate iu = new InventoryUpdate();
                	iu.addModifiedItem(item);
                	_playerTwo.sendPacket(iu);

                	SystemMessage sm = new SystemMessage(SystemMessageId.EARNED_S2_S1_S);
                	sm.addItemName(item);
        			sm.addNumber(_gpreward);
                	_playerTwo.sendPacket(sm);
                } catch (Exception e) { }
    		}
    		else
    		{
    			result=" tie";
    			_sm = new SystemMessage(SystemMessageId.THE_GAME_ENDED_IN_A_TIE);
                broadcastMessage(_sm, true);
    		}
            
            if (Config.DEBUG)
            	_log.info("Olympia Result: "+_playerOneName+" vs "+_playerTwoName+" ... "+result);
            
    		playerOneStat.set(COMP_DONE, playerOnePlayed + 1);
    		playerTwoStat.set(COMP_DONE, playerTwoPlayed + 1);
    		
    		_nobles.remove(_playerOneID);
    		_nobles.remove(_playerTwoID);
    		
    		_nobles.put(_playerOneID, playerOneStat);
    		_nobles.put(_playerTwoID, playerTwoStat);
            
    		for (int i=40;i>10;i-=10)
    		{
    			_sm = new SystemMessage(SystemMessageId.YOU_WILL_BE_MOVED_TO_TOWN_IN_S1_SECONDS);
    			_sm.addNumber(i);
    			broadcastMessage(_sm, false);
    			try{ Thread.sleep(10000); }catch (InterruptedException e){}
    			if (i==20) {
    				_sm = new SystemMessage(SystemMessageId.YOU_WILL_BE_MOVED_TO_TOWN_IN_S1_SECONDS);
    				_sm.addNumber(10);
    				broadcastMessage(_sm, false);
    				try{ Thread.sleep(5000); }catch (InterruptedException e){}
    			}
    		}
    		for (int i=5;i>0;i--)
    		{
    			_sm = new SystemMessage(SystemMessageId.YOU_WILL_BE_MOVED_TO_TOWN_IN_S1_SECONDS);
    			_sm.addNumber(i);
    			broadcastMessage(_sm, false);
    			try{ Thread.sleep(1000); }catch (InterruptedException e){}
    		}
    	}
    	
    	protected void additions()
    	{
    		for (L2PcInstance player : _players)
    		{
    		  try {
    			//Set HP/CP/MP to Max
    			player.setCurrentCp(player.getMaxCp());
    			player.setCurrentHp(player.getMaxHp());
    			player.setCurrentMp(player.getMaxMp());
    		  } catch (Exception e) { }
    		  finally
    		  {
    			  _damageP1 = 0;
    			  _damageP2 = 0;
    		  }
     		}
     	}

    	protected boolean makePlayersVisible()
    	{
            _sm = new SystemMessage(SystemMessageId.STARTS_THE_GAME);
            try {
            	for (L2PcInstance player : _players)
            	{
            		player.getAppearance().setVisible();
            		player.broadcastUserInfo();
            		player.sendPacket(_sm);
            		if (player.getPet() != null)
            			player.getPet().updateAbnormalEffect();
            	}
            } catch (NullPointerException e) { _aborted = true; return false; }
            return true;
    	}

    	protected boolean makeCompetitionStart()
    	{
    	    if (_aborted) return false;
           
    	    _sm = new SystemMessage(SystemMessageId.STARTS_THE_GAME);
    	    broadcastMessage(_sm, true);
    		try {
    			for (L2PcInstance player : _players)
    			{
    				player.setIsOlympiadStart(true);
    			}
    		} catch (Exception e) { 
    			_aborted = true;
    			return false; 
    		}
    		return true;
    	}

    	protected String getTitle()
    	{
    		String msg = "";
    		msg+= _playerOneName + " : " + _playerTwoName;
    		return msg;
    	}
    	
    	protected L2PcInstance[] getPlayers()
    	{
            L2PcInstance[] players = new L2PcInstance[2];
            
            if (_playerOne == null || _playerTwo == null) return null;
            
    		players[0] = _playerOne;
    		players[1] = _playerTwo;
    		
    		return players;
    	}
        
        protected L2FastList<L2PcInstance> getSpectators()
        {
            return _spectators;
        }
        
        protected void addSpectator(L2PcInstance spec)
        {
            _spectators.add(spec);
        }
        
        protected void removeSpectator(L2PcInstance spec)
        {
            if (_spectators != null && _spectators.contains(spec))
                _spectators.remove(spec);
        }
        
        protected void clearSpectators()
        { 
            if (_spectators != null)
            {
                for (L2PcInstance pc : _spectators)
                {
                    try {
                    	if(!pc.inObserverMode()) continue;
                    	pc.leaveOlympiadObserverMode();
                    } catch (NullPointerException e) {}
                }
                _spectators.clear();
            }
        }
        
        private void broadcastMessage(SystemMessage sm, boolean toAll)
        {
        	try { 
        	    _playerOne.sendPacket(sm);
        	    _playerTwo.sendPacket(sm); 
        	} catch (Exception e) {}
        	
            if (toAll && _spectators != null)
            {
                for (L2PcInstance spec : _spectators)
                {
                    try { spec.sendPacket(sm); } catch (NullPointerException e) {}
                }
            }
        }
    }
}
