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

/**
 * @author FBIagent<br>
 */
public class AdminCommandAccessRight {
	/** The admin command<br> */
	private String _adminCommand = null;
	/** The access levels which can use the admin command<br> */
	private AccessLevel[] _accessLevels = null;

	/**
	 * Initialized members
	 * 
	 * @param adminCommand as String
	 * @param accessLevels as String
	 */
	public AdminCommandAccessRight( String adminCommand, String accessLevels ) {
		_adminCommand = adminCommand;

		String[] accessLevelsSplit = accessLevels.split( "," );
		int numLevels = accessLevelsSplit.length;

		_accessLevels = new AccessLevel[ numLevels ];

		for ( int i = 0;i < numLevels;++ i ) {
			try {
				_accessLevels[ i ] = AccessLevels.getInstance().getAccessLevel( Integer.valueOf( accessLevelsSplit[ i ] ) );
			} catch ( NumberFormatException nfe ) {
				_accessLevels[ i ] = null;
			}
		}
	}

	/**
	 * Returns the admin command the access right belongs to<br><br>
	 * 
	 * @return String: the admin command the access right belongs to<br>
	 */
	public String getAdminCommand() {
		return _adminCommand;
	}

	/**
	 * Checks if the given characterAccessLevel is allowed to use the admin command which belongs to this access right<br><br>
	 * 
	 * @param characterAccessLevel<br><br>
	 * 
	 * @return boolean: true if characterAccessLevel is allowed to use the admin command which belongs to this access right, otherwise false<br>
	 */
	public boolean hasAccess( AccessLevel characterAccessLevel ) {
		for ( int i = 0;i < _accessLevels.length;++ i ) {
			AccessLevel accessLevel = _accessLevels[ i ];

			if ( accessLevel != null && ( accessLevel.getLevel() == characterAccessLevel.getLevel() || characterAccessLevel.hasChildAccess( accessLevel ) ) ) {
				return true;
			}
		}

		return false;
	}
}