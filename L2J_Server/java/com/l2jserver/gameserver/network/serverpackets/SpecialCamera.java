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
package com.l2jserver.gameserver.network.serverpackets;

public class SpecialCamera extends L2GameServerPacket
{
	private final int _id;
	private final int _dist;
	private final int _yaw;
	private final int _pitch;
	private final int _time;
	private final int _duration;
	private final int _turn;
	private final int _rise;
	private final int _widescreen;
	private final int _unknown;
	
	/**
	 * @param id object Id
	 * @param dist the distance to the object
	 * @param yaw North = 90, South = 270, East = 0, West = 180
	 * @param pitch > 0: looks up, pitch < 0: looks down (angle)
	 * @param time faster if it's smaller
	 * @param duration animation time
	 */
	public SpecialCamera(int id, int dist, int yaw, int pitch, int time, int duration)
	{
		_id = id;
		_dist = dist;
		_yaw = yaw;
		_pitch = pitch;
		_time = time;
		_duration = duration;
		_turn = 0;
		_rise = 0;
		_widescreen = 0;
		_unknown = 0;
	}
	
	/**
	 * @param id object Id
	 * @param dist the distance to the object
	 * @param yaw North = 90, South = 270, East = 0, West = 180
	 * @param pitch > 0: looks up, pitch < 0: looks down (angle)
	 * @param time faster if it's smaller
	 * @param duration animation time
	 * @param turn
	 * @param rise
	 * @param widescreen
	 * @param unk
	 */
	public SpecialCamera(int id, int dist, int yaw, int pitch, int time, int duration, int turn, int rise, int widescreen, int unk)
	{
		_id = id;
		_dist = dist;
		_yaw = yaw;
		_pitch = pitch;
		_time = time;
		_duration = duration;
		_turn = turn;
		_rise = rise;
		_widescreen = widescreen;
		_unknown = unk;
	}
	
	@Override
	public void writeImpl()
	{
		writeC(0xd6);
		writeD(_id);
		writeD(_dist);
		writeD(_yaw);
		writeD(_pitch);
		writeD(_time);
		writeD(_duration);
		writeD(_turn);
		writeD(_rise);
		writeD(_widescreen);
		writeD(_unknown);
	}
}
