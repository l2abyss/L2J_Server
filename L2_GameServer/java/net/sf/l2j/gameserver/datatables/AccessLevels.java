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
package net.sf.l2j.gameserver.datatables;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.logging.Logger;
import net.sf.l2j.L2DatabaseFactory;

import javolution.util.FastMap;

/**
 * @author FBIagent<br>
 */
public class AccessLevels {
	/** The logger<br> */
	private static Logger _log = Logger.getLogger( AccessLevels.class.getName() );
	/** The one and only instance of this class, retriveable by getInstance()<br> */
	private static AccessLevels _instance = null;
	/** Reserved master access level<br> */
	public static final int _masterAccessLevelNum = 127;
	/** The master access level which can use everything<br> */
	public static AccessLevel _masterAccessLevel = new AccessLevel( _masterAccessLevelNum, "Master Access", Integer.decode( "0x000000" ), Integer.decode( "0x000000" ), null, true, true, true, true, true, true, true, true );
	/** Reserved user access level<br> */
	public static final int _userAccessLevelNum = 0;
	/** The user access level which can do no administrative tasks<br> */
	public static AccessLevel _userAccessLevel = new AccessLevel( _userAccessLevelNum, "User", Integer.decode( "0xFFFFFF" ), Integer.decode( "0xFFFFFF" ), null, false, false, false, true, false, true, true, true );
	/** FastMap of access levels defined in database<br> */
	private Map< Integer, AccessLevel > _accessLevels = new FastMap< Integer, AccessLevel >();

	/**
	 * Loads the access levels from database<br>
	 */
	private AccessLevels() {
		java.sql.Connection con = null;

		try {
			con = L2DatabaseFactory.getInstance().getConnection();

			PreparedStatement stmt = con.prepareStatement( "SELECT * FROM `access_levels` ORDER BY `access_level` DESC" );
			ResultSet rset = stmt.executeQuery();
			int accessLevel = 0;
			String name = null;
			int nameColor = 0;
			int titleColor = 0;
			String childs = null;
			boolean isGm = false;
			boolean allowPeaceAttack = false;
			boolean allowFixedRes = false;
			boolean allowTransaction = false;
			boolean allowAltG = false;
			boolean giveDamage = false;
			boolean takeAggro = false;
			boolean gainExp = false;

			while ( rset.next() ) {
				accessLevel = rset.getInt( "access_level" );
				name = rset.getString( "name" );

				if ( accessLevel == _userAccessLevelNum ) {
					_log.warning( "AccessLevels: Access level with name " + name + " is using reserved user access level " + _userAccessLevelNum + ". Ignoring it!" );
					continue;
				} else if ( accessLevel == _masterAccessLevelNum ) {
					_log.warning( "AccessLevels: Access level with name " + name + " is using reserved master access level " + _masterAccessLevelNum + ". Ignoring it!" );
					continue;
				} else if ( accessLevel < 0 ) {
					_log.warning( "AccessLevels: Access level with name " + name + " is using banned access level state(below 0). Ignoring it!" );
					continue;
				}
				
				try {
					nameColor = Integer.decode( "0x" + rset.getString( "name_color" ) );
				} catch ( NumberFormatException nfe ) {
					try {
						nameColor = Integer.decode( "0xFFFFFF" );
					} catch ( NumberFormatException nfe2 ) {
					}
				}

				try {
					titleColor = Integer.decode( "0x" + rset.getString( "title_color" ) );
				} catch ( NumberFormatException nfe ) {
					try {
						titleColor = Integer.decode( "0xFFFFFF" );
					} catch ( NumberFormatException nfe2 ) {
					}
				}

				childs = rset.getString( "child_access" );
				isGm = rset.getBoolean( "is_gm" );
				allowPeaceAttack = rset.getBoolean( "allow_peace_attack" );
				allowFixedRes = rset.getBoolean( "allow_fixed_res" );
				allowTransaction = rset.getBoolean( "allow_transaction" );
				allowAltG = rset.getBoolean( "allow_altg" );
				giveDamage = rset.getBoolean( "give_damage" );
				takeAggro = rset.getBoolean( "take_aggro" );
				gainExp = rset.getBoolean( "gain_exp" );

				_accessLevels.put( accessLevel, new AccessLevel( accessLevel, name, nameColor, titleColor, childs.equals( "" ) ? null : childs, isGm, allowPeaceAttack, allowFixedRes, allowTransaction, allowAltG, giveDamage, takeAggro, gainExp  ) );
			}

			rset.close();
			stmt.close();
		}
		catch ( SQLException e ) {
			_log.warning( "AccessLevels: Error loading from database:" + e );
		}
		finally {
			try {
				con.close();
			} catch ( Exception e ) {
			}
		}

		_log.info( "AccessLevels: Loaded " + _accessLevels.size() + " from database." );
	}

	/**
	 * Returns the one and only instance of this class<br><br>
	 * 
	 * @return AccessLevels: the one and only instance of this class<br>
	 */
	public static AccessLevels getInstance() {
		return _instance == null ? ( _instance = new AccessLevels() ) : _instance;
	}

	/**
	 * Returns the access level by characterAccessLevel<br><br>
	 * 
	 * @param accessLevelNum as int<br><br>
	 *
	 * @return AccessLevel: AccessLevel instance by char access level<br>
	 */
	public AccessLevel getAccessLevel( int accessLevelNum ) {
		AccessLevel accessLevel = null;

		synchronized ( _accessLevels ) {
			accessLevel = _accessLevels.get( accessLevelNum );
		}

		return accessLevel;
	}

	public void addBanAccessLevel( int accessLevel ) {
		synchronized ( _accessLevels ) {
			if ( accessLevel > -1 ) {
				return;
			}

			_accessLevels.put( accessLevel, new AccessLevel( accessLevel, "Banned", Integer.decode( "0x000000" ), Integer.decode( "0x000000" ), null, false, false, false, false, false, false, false, false ) );
		}
	}
}