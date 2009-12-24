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
package com.l2jserver.communityserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * @author Forsaiken
 */

public final class Config
{
	protected static final Logger _log = Logger.getLogger(Config.class.getName());
	
    /** Properties file for community server configurations */
    public static final String  CONFIGURATION_FILE								= "./config/communityserver.properties";
    /** Properties file for server function configurations */
    public static final String  GENERAL_FILE									= "./config/General.properties";

    /** ************************************************** **/
	/** Server Settings -Begin                             **/
	/** ************************************************** **/
    /** Driver to access to database */
    public static String	DATABASE_DRIVER;
    /** Path to access to database */
    public static String	DATABASE_URL;
    /** Database login */
    public static String	DATABASE_LOGIN;
    /** Database password */
    public static String	DATABASE_PASSWORD;
    /** Maximum number of connections to the database */
    public static int		DATABASE_MAX_CONNECTIONS;
    /** Datapack root directory */
    public static File		DATAPACK_ROOT;
    /** Accept alternate ID for server ? */
    public static boolean	ACCEPT_ALTERNATE_ID;
    /** ID for request to the server */
    public static int		REQUEST_ID;
	
    /** ************************************************** **/
	/** Server Settings -End                           **/
	/** ************************************************** **/

    /** Game Server login port */
    public static int        GAME_SERVER_LOGIN_PORT;
    /** Game Server login Host */
    public static String     GAME_SERVER_LOGIN_HOST;

    /** Accept new game server ? */
    public static boolean ACCEPT_NEW_GAMESERVER;

    public static boolean FLOOD_PROTECTION;
    public static int     FAST_CONNECTION_LIMIT;
    public static int     NORMAL_CONNECTION_TIME;
    public static int     FAST_CONNECTION_TIME;
    public static int     MAX_CONNECTION_PER_IP;
    
    /** General settings */
    public static int     MIN_PLAYER_LVL_FOR_FORUM;
    public static int     MIN_CLAN_LVL_FOR_FORUM;
    public static Long     MAIL_AUTO_DELETION_TIME;
    
    public static final void load()
    {
    	try
		{
			Properties serverSettings			= new Properties();
			InputStream is						= new FileInputStream(new File(CONFIGURATION_FILE));
            serverSettings.load(is);
            is.close();
            
            GAME_SERVER_LOGIN_HOST				= serverSettings.getProperty("CSHostname","*");
            GAME_SERVER_LOGIN_PORT				= Integer.parseInt(serverSettings.getProperty("CSPort","9013"));
            
            ACCEPT_NEW_GAMESERVER				= Boolean.parseBoolean(serverSettings.getProperty("AcceptNewGameServer", "True"));
            REQUEST_ID							= Integer.parseInt(serverSettings.getProperty("RequestServerID", "0"));
            ACCEPT_ALTERNATE_ID					= Boolean.parseBoolean(serverSettings.getProperty("AcceptAlternateID", "True"));
            DATAPACK_ROOT						= new File(serverSettings.getProperty("DatapackRoot", ".")).getCanonicalFile();

            DATABASE_DRIVER						= serverSettings.getProperty("Driver", "com.mysql.jdbc.Driver");
            DATABASE_URL						= serverSettings.getProperty("URL", "jdbc:mysql://localhost/l2jdb");
            DATABASE_LOGIN						= serverSettings.getProperty("Login", "root");
            DATABASE_PASSWORD					= serverSettings.getProperty("Password", "");
            DATABASE_MAX_CONNECTIONS			= Integer.parseInt(serverSettings.getProperty("MaximumDbConnections", "10"));
            
            Properties generalSettings			= new Properties();
			is									= new FileInputStream(new File(GENERAL_FILE));
            generalSettings.load(is);
            is.close();
            
            MIN_PLAYER_LVL_FOR_FORUM			= Integer.parseInt(generalSettings.getProperty("MinPlayerLvLForForum", "1"));
            MIN_CLAN_LVL_FOR_FORUM				= Integer.parseInt(generalSettings.getProperty("MinClanLvLForForum", "2"));
            MAIL_AUTO_DELETION_TIME				= Long.parseLong(generalSettings.getProperty("MailAutoDeletionTime", "90")) * 86400000;
		}
		catch (Exception e)
		{
			
		}
    }
}