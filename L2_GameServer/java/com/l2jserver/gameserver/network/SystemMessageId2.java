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
package com.l2jserver.gameserver.network;

public enum SystemMessageId2
{
	/**
	 * ID: 1987<br>
	 * Message: $s1
	 */
	S1(1987),
	
	/**
	 * ID: 2701
	 * Message: The match is being prepared. Please try again later.
	 */
	MATCH_BEING_PREPARED_TRY_LATER(2701),
	
	/**
	 * ID: 2702
	 * Message: You were excluded from the match because the registration count was not correct.
	 */
	EXCLUDED_FROM_MATCH_DUE_INCORRECT_COUNT(2702),
	
	/**
	 * ID: 2703
	 * Message: The team was adjusted because the population ratio was not correct.
	 */
	TEAM_ADJUSTED_BECAUSE_WRONG_POPULATION_RATIO(2703),
 		
 	/**
	 * ID: 2704<br>
	 * Message: You cannot register because capacity has been exceeded.
	 */
	CANNOT_REGISTER_CAUSE_QUEUE_FULL(2704),
	
	/**
	 * ID: 2705
	 * Message: The match waiting time was extended by 1 minute.
	 */
	MATCH_WAITING_TIME_EXTENDED(2705),

	/**
	 * ID: 2706
	 * Message: You cannot enter because you do not meet the requirements. 
	 */
	CANNOT_ENTER_CAUSE_DONT_MATCH_REQUIREMENTS(2706),
	
	/**
	 * ID: 2707
	 * Message: You cannot make another request for 10 seconds after cancelling a match registration.
	 */
	CANNOT_REQUEST_REGISTRATION_10_SECS_AFTER(2707),
	
	/**
	 * ID: 2708<br>
	 * Message: You cannot register while possessing a cursed weapon.
	 */
	CANNOT_REGISTER_PROCESSING_CURSED_WEAPON(2708),
	
	/**
	 * ID: 2709<br>
	 * Message: Applicants for the Olympiad, Underground Coliseum, or Kratei's Cube matches cannot register.
	 */
	COLISEUM_OLYMPIAD_KRATEIS_APPLICANTS_CANNOT_PARTICIPATE(2709),
	
 	/**
	 * ID: 2922<br>
	 * Message: Block Checker will end in 5 seconds! 
	 */
	BLOCK_CHECKER_ENDS_5(2922),
	
	/**
	 * ID: 2923<br>
	 * Message: Block Checker will end in 4 seconds!! 
	 */
	BLOCK_CHECKER_ENDS_4(2923),
	
	/**
	 * ID: 2925<br>
	 * Message: Block Checker will end in 3 seconds!!! 
	 */
	BLOCK_CHECKER_ENDS_3(2925),

	/**
	 * ID: 2926<br>
	 * Message: Block Checker will end in 2 seconds!!!! 
	 */
	BLOCK_CHECKER_ENDS_2(2926),

	/**
	 * ID: 2927<br>
	 * Message: Block Checker will end in 1 second!!!!! 
	 */
	BLOCK_CHECKER_ENDS_1(2927),
	
	/**
 	* ID: 2924<br>
 	* Message: You cannot enter a Seed while in a flying transformation state.
 	*/
 	YOU_CANNOT_ENTER_SEED_IN_FLYING_TRANSFORM(2924),
 
 	/**
	 * ID: 2928<br>
	 * Message: The $c1 team has won.
	 */
	TEAM_C1_WON(2928);
	
	private int _id;
	
	private SystemMessageId2(int id)
	{
		_id = id;
	}
	
	public int getId()
	{
		return _id;
	}
	
	public static final SystemMessageId2 getSystemMessageId(int id)
	{
		for (SystemMessageId2 sysmsgid : SystemMessageId2.values())
			if (sysmsgid.getId() == id)
				return sysmsgid;
		
		return SystemMessageId2.S1;
	}

}
