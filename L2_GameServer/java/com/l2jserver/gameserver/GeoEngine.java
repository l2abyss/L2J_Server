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
package com.l2jserver.gameserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import com.l2jserver.Config;
import com.l2jserver.gameserver.datatables.DoorTable;
import com.l2jserver.gameserver.model.L2Object;
import com.l2jserver.gameserver.model.L2World;
import com.l2jserver.gameserver.model.Location;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.instance.L2DoorInstance;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.util.Point3D;

/**
 *
 * @author -Nemesiss-
 */
public class GeoEngine extends GeoData
{
	private static Logger _log = Logger.getLogger(GeoData.class.getName());
	private static final byte EAST = 1;
	private static final byte WEST = 2;
	private static final byte SOUTH = 4;
	private static final byte NORTH = 8;
	private static final byte NSWE_ALL = 15;
	private static final byte NSWE_NONE = 0;
	private static final byte BLOCKTYPE_FLAT = 0;
	private static final byte BLOCKTYPE_COMPLEX = 1;
	private static final byte BLOCKTYPE_MULTILEVEL = 2;
	public static final int BLOCKS_IN_MAP = 256 * 256;
	//	private static Map<Short, MappedByteBuffer> _geodata = new FastMap<Short, MappedByteBuffer>();
//	private static Map<Short, IntBuffer> _geodataIndex = new FastMap<Short, IntBuffer>();
	private static final byte[][][][] _geodata = new byte[L2World.WORLD_SIZE_X][L2World.WORLD_SIZE_Y][][];
	private static int _maxLayers = 1;

	public static GeoEngine getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private GeoEngine()
	{
		nInitGeodata();
	}
	
	//Public Methods
	/**
	 * @see com.l2jserver.gameserver.GeoData#getType(int, int)
	 */
	@Override
	public short getType(int x, int y)
	{
		return nGetType((x - L2World.MAP_MIN_X) >> 4, (y - L2World.MAP_MIN_Y) >> 4, 0);
	}
	
	/**
	 * @see com.l2jserver.gameserver.GeoData#getHeight(int, int, int)
	 */
	@Override
	public short getHeight(int x, int y, int z)
	{
		return (short)nGetHeight((x - L2World.MAP_MIN_X) >> 4, (y - L2World.MAP_MIN_Y) >> 4, z, 0);
	}
	
	/**
	 * @see com.l2jserver.gameserver.GeoData#geoPosition(int, int)
	 */
	@Override
	public String geoPosition(int x, int y)
	{
		int gx = (x - L2World.MAP_MIN_X) >> 4;
		int gy = (y - L2World.MAP_MIN_Y) >> 4;
		return "bx: " + getBlock(gx) + " by: " + getBlock(gy) + " cx: " + getCell(gx) + " cy: " + getCell(gy) + "  region offset: "
				+ getRegionOffset(gx, gy);
	}
	
	/**
	 * @see com.l2jserver.gameserver.GeoData#canSeeTarget(L2Object, Point3D)
	 */
	@Override
	public boolean canSeeTarget(L2Object cha, Point3D target)
	{
		if (DoorTable.getInstance().checkIfDoorsBetween(cha.getX(), cha.getY(), cha.getZ(), target.getX(), target.getY(), target.getZ(), cha.getInstanceId()))
			return false;
		if (cha.getZ() >= target.getZ())
			return canSeeTarget(cha.getX(), cha.getY(), cha.getZ(), target.getX(), target.getY(), target.getZ());
		else
			return canSeeTarget(target.getX(), target.getY(), target.getZ(), cha.getX(), cha.getY(), cha.getZ());
		
	}
	
	/**
	 * @see com.l2jserver.gameserver.GeoData#canSeeTarget(com.l2jserver.gameserver.model.L2Object, com.l2jserver.gameserver.model.L2Object)
	 */
	@Override
	public boolean canSeeTarget(L2Object cha, L2Object target)
	{
		return canSeeTarget(cha, target, false);
	}
	
	/**
	 * @see com.l2jserver.gameserver.GeoData#canSeeTargetDebug(com.l2jserver.gameserver.model.actor.instance.L2PcInstance, com.l2jserver.gameserver.model.L2Object)
	 */
	@Override
	public boolean canSeeTargetDebug(L2PcInstance gm, L2Object target)
	{
		if (target instanceof L2DoorInstance)
		{
			gm.sendMessage("door always true");
			return true; // door coordinates are hinge coords..
		}
		
		return canSeeTarget(gm, target);
	}

	/**
	 * @see com.l2jserver.gameserver.GeoData#getNSWE(int, int, int)
	 */
	@Override
	public short getNSWE(int x, int y, int z)
	{
		return nGetNSWE((x - L2World.MAP_MIN_X) >> 4, (y - L2World.MAP_MIN_Y) >> 4, z, 0);
	}
	
	@Override
	public boolean canMoveFromToTarget(int x, int y, int z, int tx, int ty, int tz, int instanceId)
	{
		Location destiny = moveCheck(x, y, z, tx, ty, tz, instanceId);
		return (destiny.getX() == tx && destiny.getY() == ty && destiny.getZ() == tz);
	}
	
	@Override
	public boolean canSeeTarget(int x, int y, int z, int tx, int ty, int tz)
	{
		return canSeeCoord(x, y, z, tx, ty, tz, false, 0);
	}
	
	public boolean hasGeo(int x, int y)
	{
		return getGeoBlockFromGeoCoords((x - L2World.MAP_MIN_X) >> 4, (y - L2World.MAP_MIN_Y) >> 4, 0) != null;
	}

	private static final boolean canSeeTarget(L2Object actor, L2Object target, boolean air)
	{
		if (target == null)
			return false;

		int z = target.getZ() + 64;
		if (target instanceof L2Character && ((L2Character)target).getTemplate() != null)
			z += ((L2Character)target).getTemplate().collisionHeight;

		return canSeeCoord(actor, target.getX(), target.getY(), z, air);
	}

	//
	private static final boolean canSeeCoord(L2Object actor, int tx, int ty, int tz, boolean air)
	{
		if (actor == null)
			return false;

		int z = actor.getZ() + 64; // Config
		if (actor instanceof L2Character && ((L2Character)actor).getTemplate() != null)
			z += ((L2Character)actor).getTemplate().collisionHeight;

		return canSeeCoord(actor.getX(), actor.getY(), z, tx, ty, tz, air, actor.getInstanceId());
	}

	//
	private static final boolean canSeeCoord(int x, int y, int z, int tx, int ty, int tz, boolean air, int instanceId)
	{
		final int mX = x - L2World.MAP_MIN_X >> 4;
		final int mY = y - L2World.MAP_MIN_Y >> 4;
		final int tmX = tx - L2World.MAP_MIN_X >> 4;
		final int tmY = ty - L2World.MAP_MIN_Y >> 4;
		return canSee(mX, mY, z, tmX, tmY, tz, air, instanceId).equals(tmX, tmY, tz)
		&& canSee(tmX, tmY, tz, mX, mY, z, air, instanceId).equals(mX, mY, z);
	}

	//
	private static final int FindNearestLowerLayer(short[] layers, int z)
	{
		short h = Short.MIN_VALUE;
		short nearest_layer_h = Short.MIN_VALUE;
		int nearest_layer = Integer.MIN_VALUE;

		for (int i = 1; i <= layers[0]; i++)
		{
			h = (short) ((short) (layers[i] & 0x0fff0) >> 1);
			if (h < z && nearest_layer_h < h)
			{
				nearest_layer_h = h;
				nearest_layer = layers[i];
			}
		}
		return nearest_layer;
	}

	//
	private static final short CheckNoOneLayerInRangeAndFindNearestLowerLayer(short[] layers, int z0, int z1)
	{
		final int z_min, z_max;
		if (z0 > z1)
		{
			z_min = z1;
			z_max = z0;
		}
		else
		{
			z_min = z0;
			z_max = z1;
		}

		short h;
		short nearest_layer = Short.MIN_VALUE;
		short nearest_layer_h = Short.MIN_VALUE;
		for (int i = 1; i <= layers[0]; i++)
		{
			h = (short) ((short) (layers[i] & 0x0fff0) >> 1);
			if (z_min <= h && h <= z_max)
				return Short.MIN_VALUE;
			if (h < z0 && nearest_layer_h < h)
			{
				nearest_layer_h = h;
				nearest_layer = layers[i];
			}
		}
		return nearest_layer;
	}

	//
	private static final byte[] getGeoBlockFromGeoCoords(int geoX, int geoY, int instanceId)
	{
		final int ix = geoX >> 11;
		final int iy = geoY >> 11;

		if (ix < 0 || ix >= L2World.WORLD_SIZE_X || iy < 0 || iy >= L2World.WORLD_SIZE_Y)
			return null;

		final byte[][] region = _geodata[ix][iy];
		if (region == null)
			return null;

//		if (region.length <= instanceId || instanceId < 0)
//			instanceId = 0;

		return region[getBlockIndex(getBlock(geoX), getBlock(geoY))];//[instanceId];
	}

	//
	private static final boolean canSeeWallCheck(Layer layer, Layer nearest_lower_neighbor, byte directionNSWE)
	{
		return (layer.nswe & directionNSWE) != 0 || layer.height <= nearest_lower_neighbor.height || Math.abs(layer.height - nearest_lower_neighbor.height) < 64; // Config
	}

	//
	private static final boolean canSeeWallCheck(short layer, short nearest_lower_neighbor, byte directionNSWE, int curr_z, boolean air)
	{
		short nearest_lower_neighborh = (short) ((short) (nearest_lower_neighbor & 0x0fff0) >> 1);
		if(air)
			return nearest_lower_neighborh < curr_z;
		short layerh = (short) ((short) (layer & 0x0fff0) >> 1);
		int zdiff = nearest_lower_neighborh - layerh;
		return (layer & 0x0F & directionNSWE) != 0 || zdiff > -64 && zdiff != 0; // Config
	}

	//
	private static final Location canSee(int x, int y, int z, int tx, int ty, int tz, boolean air, int instanceId)
	{
		final int diffX = tx - x;
		final int diffY = ty - y;
		final int diffZ = tz - z;
		final int dx = Math.abs(diffX);
		final int dy = Math.abs(diffY);

		final float steps = Math.max(dx, dy);
		int currX = x;
		int currY = y;
		int currZ = z;
		short[] currLayers = new short[_maxLayers + 1];
		nGetLayers(currX, currY, currLayers, instanceId);

		Location result = new Location(x, y, z, -1);

		if (steps == 0)
		{
			if (CheckNoOneLayerInRangeAndFindNearestLowerLayer(currLayers, currZ, currZ + diffZ) != Short.MIN_VALUE)
				result.set(tx, ty, tz, 1);

			return result;
		}

		float stepX = diffX / steps;
		float stepY = diffY / steps;
		float stepZ = diffZ / steps;
		int halfStepZ = (int) (stepZ / 2);

		float nextX = currX;
		float nextY = currY;
		float nextZ = currZ;

		int iNextX, iNextY, iNextZ, middleZ;
		short[] tmpLayers = new short[_maxLayers + 1];
		short srcNearestLowerLayer, dstNearestLowerLayer, tmpNearestLowerLayer;

		for (int i = 0; i < steps; i++)
		{
			if (currLayers[0] == 0)
			{
				result.set(tx, ty, tz, 0); // No geodata here
				return result;
			}

			nextX += stepX;
			nextY += stepY;
			nextZ += stepZ;
			iNextX = (int) (nextX + 0.5f);
			iNextY = (int) (nextY + 0.5f);
			iNextZ = (int) (nextZ + 0.5f);
			middleZ = currZ + halfStepZ;

			srcNearestLowerLayer = CheckNoOneLayerInRangeAndFindNearestLowerLayer(currLayers, currZ, middleZ);
			if (srcNearestLowerLayer == Short.MIN_VALUE)
			{
				result.setHeading(-10);
				return result;
			}

			nGetLayers(currX, currY, currLayers, instanceId);
			if(currLayers[0] == 0)
			{
				result.set(tx, ty, tz, 0);
				return result;
			}

			dstNearestLowerLayer = CheckNoOneLayerInRangeAndFindNearestLowerLayer(currLayers, iNextZ, middleZ);
			if (dstNearestLowerLayer == Short.MIN_VALUE)
			{
				result.setHeading(-11);
				return result;
			}

			if (currX == iNextX)
			{
				if (!canSeeWallCheck(srcNearestLowerLayer, dstNearestLowerLayer, iNextY > currY ? SOUTH : NORTH, currZ, air))
				{
					result.setHeading(-20);
					return result;
				}
			}
			else if (currY == iNextY)
			{
				if (!canSeeWallCheck(srcNearestLowerLayer, dstNearestLowerLayer, iNextX > currX ? EAST : WEST, currZ, air))
				{
					result.setHeading(-21);
					return result;
				}
			}
			else
			{
				nGetLayers(currX, iNextY, tmpLayers, instanceId);
				if (tmpLayers[0] == 0)
				{
					result.set(tx, ty, tz, 0); // No geodata here
					return result;
				}

				tmpNearestLowerLayer = CheckNoOneLayerInRangeAndFindNearestLowerLayer(tmpLayers, iNextZ, middleZ);
				if (tmpNearestLowerLayer == Short.MIN_VALUE)
				{
					result.setHeading(-30);
					return result;
				}

				if (!(canSeeWallCheck(srcNearestLowerLayer, tmpNearestLowerLayer, iNextY > currY ? SOUTH : NORTH, currZ, air)
						&& canSeeWallCheck(tmpNearestLowerLayer, dstNearestLowerLayer, iNextX > currX ? EAST : WEST, currZ, air)))
				{
					nGetLayers(iNextX, currY, tmpLayers, instanceId);
					if (tmpLayers[0] == 0)
					{
						result.set(tx, ty, tz, 0); // No geodata here
						return result;
					}

					tmpNearestLowerLayer = CheckNoOneLayerInRangeAndFindNearestLowerLayer(tmpLayers, iNextZ, middleZ);
					if (tmpNearestLowerLayer == Short.MIN_VALUE)
					{
						result.setHeading(-31);
						return result;
					}
					if (!canSeeWallCheck(srcNearestLowerLayer, tmpNearestLowerLayer, iNextX > currX ? EAST : WEST, currZ, air))
					{
						result.setHeading(-32);
						return result;
					}
					if(!canSeeWallCheck(tmpNearestLowerLayer, dstNearestLowerLayer, iNextX > currX ? EAST : WEST, currZ, air))
					{
						result.setHeading(-33);
						return result;
					}
				}
			}

			result.set(currX, currY, currZ);
			currX = iNextX;
			currY = iNextY;
			currZ = iNextZ;
		}

		result.set(tx, ty, tz, 0xFF);
		return result;
	}

	//
	private static final boolean nCanMoveNextExCheck(int x, int y, int z, int nextX, int nextY, int hextZ, short[] tempLayers, int instanceId)
	{
		nGetLayers(x, y, tempLayers, instanceId);
		if (tempLayers[0] == 0)
			return true;

		final int tempLayer = FindNearestLowerLayer(tempLayers, z + 64); // Config
		if (tempLayer == Integer.MIN_VALUE)
			return false;

		final short tempLayerH = (short) ((short) (tempLayer & 0x0fff0) >> 1);
		if(Math.abs(tempLayerH - hextZ) >= 64 || Math.abs(tempLayerH - z) >= 64) //Config
			return false;

		return checkNSWE((byte) (tempLayer & 0x0F), x, y, nextX, nextY);
	}

	//
	public static final int canMove(int x, int y, int z, int tx, int ty, int tz, boolean withCollision, int instanceId)
	{
		final int geoX = (x - L2World.MAP_MIN_X) >> 4;
		final int geoY = (y - L2World.MAP_MIN_Y) >> 4;
		final int geoTX = (tx - L2World.MAP_MIN_X) >> 4;
		final int geoTY = (ty - L2World.MAP_MIN_Y) >> 4;
		final int diffX = geoTX - geoX;
		final int diffY = geoTY - geoY;
		int diffZ = tz - z;
		final int dX = Math.abs(diffX);
		final int dY = Math.abs(diffY);
		int dZ = Math.abs(diffZ);

		final float steps = Math.max(dX, dY);
		if (steps == 0)
			return -5;

		int currX = geoX;
		int currY = geoY;
		int currZ = z;
		short[] currLayers = new short[_maxLayers + 1];
		nGetLayers(currX, currY, currLayers, instanceId);
		if (currLayers[0] == 0)
			return 0;

		final float stepX = diffX / steps;
		final float stepY = diffY / steps;
		float nextX = currX;
		float nextY = currY;
		int iNextX, iNextY;

		short[] nextLayers = new short[_maxLayers + 1];
		short[] tempLayers = new short[_maxLayers + 1];
		short[] currNextSwitcher;

		for(int i = 0; i < steps; i++)
		{
			nextX += stepX;
			nextY += stepY;
			iNextX = (int) (nextX + 0.5f);
			iNextY = (int) (nextY + 0.5f);
			nGetLayers(iNextX, iNextY, nextLayers, instanceId);
			currZ = nCanMoveNext(currX, currY, currZ, currLayers, iNextX, iNextY, nextLayers, tempLayers, withCollision, instanceId);
			if (currZ == Integer.MIN_VALUE)
				return 1;

			currNextSwitcher = currLayers;
			currLayers = nextLayers;
			nextLayers = currNextSwitcher;
			currX = iNextX;
			currY = iNextY;
		}
		diffZ = currZ - tz;
		dZ = Math.abs(diffZ);
		return diffZ < 64 ? 0 : diffZ * 10000; // Config
		// return dZ > 64 ? dZ * 1000 : 0;
	}

	//
	public static final int nCanMoveNext(int x, int y, int z, short[] layers, int nextX, int nextY, short[] nextLayers, short[] tempLayers, boolean withCollision, int instanceId)
	{
		if(layers[0] == 0 || nextLayers[0] == 0)
			return z;

		final int layer = FindNearestLowerLayer(layers, z + 64); //Config
		if (layer == Integer.MIN_VALUE)
			return Integer.MIN_VALUE;

		final byte layerNSWE = (byte) (layer & 0x0F);
		if(!checkNSWE(layerNSWE, x, y, nextX, nextY))
			return Integer.MIN_VALUE;

		final short layerH = (short) ((short) (layer & 0x0fff0) >> 1);
		final int nextLayer = FindNearestLowerLayer(nextLayers, layerH + 64); // Config
		if (nextLayer == Integer.MIN_VALUE)
			return Integer.MIN_VALUE;

		final short nextLayerH = (short) ((short) (nextLayer & 0x0fff0) >> 1);

		if (x == nextX || y == nextY)
		{
			if (withCollision)
			{
				if (x == nextX)
				{
					nGetHeightAndNSWE(x - 1, y, layerH, tempLayers, instanceId);
					if (Math.abs(tempLayers[0] - layerH) > 15
							|| !checkNSWE(layerNSWE, x - 1, y, x, y)
							|| !checkNSWE((byte) tempLayers[1], x - 1, y, x - 1, nextY))
						return Integer.MIN_VALUE;

					nGetHeightAndNSWE(x + 1, y, layerH, tempLayers, instanceId);
					if (Math.abs(tempLayers[0] - layerH) > 15
							|| !checkNSWE(layerNSWE, x + 1, y, x, y)
							|| !checkNSWE((byte) tempLayers[1], x + 1, y, x + 1, nextY))
						return Integer.MIN_VALUE;

					return nextLayerH;
				}

				nGetHeightAndNSWE(x, y - 1, layerH, tempLayers, instanceId);
				if (Math.abs(tempLayers[0] - layerH) >= 64 // Config
						|| !checkNSWE(layerNSWE, x, y - 1, x, y)
						|| !checkNSWE((byte) tempLayers[1], x, y - 1, nextX, y - 1))
					return Integer.MIN_VALUE;

				nGetHeightAndNSWE(x, y + 1, layerH, tempLayers, instanceId);
				if (Math.abs(tempLayers[0] - layerH) >= 64 // Config
						|| !checkNSWE(layerNSWE, x, y + 1, x, y)
						|| !checkNSWE((byte) tempLayers[1], x, y + 1, nextX, y + 1))
					return Integer.MIN_VALUE;
			}

			return nextLayerH;
		}

		if (!nCanMoveNextExCheck(x, nextY, layerH, nextX, nextY, nextLayerH, tempLayers, instanceId))
			return Integer.MIN_VALUE;
		if (!nCanMoveNextExCheck(nextX, y, layerH, nextX, nextY, nextLayerH, tempLayers, instanceId))
			return Integer.MIN_VALUE;

		return nextLayerH;
	}

	private static byte sign(int x)
	{
		if (x >= 0)
			return +1;
		else
			return -1;
	}
	
	//GeoEngine
	private static void nInitGeodata()
	{
		LineNumberReader lnr = null;
		try
		{
			_log.info("Geo Engine: - Loading Geodata...");
			File Data = new File("./data/geodata/geo_index.txt");
			if (!Data.exists())
				return;
			
			lnr = new LineNumberReader(new BufferedReader(new FileReader(Data)));
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new Error("Failed to Load geo_index File.");
		}
		String line;
		try
		{
			while ((line = lnr.readLine()) != null)
			{
				if (line.trim().length() == 0)
					continue;
				StringTokenizer st = new StringTokenizer(line, "_");
				byte rx = Byte.parseByte(st.nextToken());
				byte ry = Byte.parseByte(st.nextToken());
				loadGeodataFile(rx, ry);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new Error("Failed to Read geo_index File.");
		}
		finally
		{
			try
			{
				lnr.close();
			}
			catch (Exception e)
			{
			}
		}
	}
	
	public static void unloadGeodata(byte rx, byte ry)
	{
		short regionoffset = (short) ((rx << 5) + ry);
//		_geodata.remove(regionoffset);
	}
	
	public static boolean loadGeodataFile(byte rx, byte ry)
	{
		if (rx < Config.WORLD_X_MIN || rx > Config.WORLD_X_MAX || ry < Config.WORLD_Y_MIN || ry > Config.WORLD_Y_MAX)
		{
			_log.warning("Failed to Load GeoFile: invalid region " + rx +","+ ry + "\n");
			return false;
		}

		String fname = "./data/geodata/" + rx + "_" + ry + ".l2j";
		_log.info("Geo Engine: - Loading: " + fname + " -> X: " + rx + " Y: " + ry);
		File Geo = new File(fname);
		int size, index = 0, block = 0, flor = 0;
		FileChannel roChannel = null;
		byte[] geo;
		try
		{
			// Create a read-only memory-mapped file
			roChannel = new RandomAccessFile(Geo, "r").getChannel();
			size = (int) roChannel.size();
			ByteBuffer buffer = roChannel.map(FileChannel.MapMode.READ_ONLY, 0, size);
			buffer.order(ByteOrder.LITTLE_ENDIAN);
			geo = new byte[buffer.remaining()];
			buffer.get(geo, 0, geo.length);
			
			if (size > BLOCKS_IN_MAP * 3) // 196608
			{
				byte[][] blocks = new byte[BLOCKS_IN_MAP][1];
				// Indexing geo files, so we will know where each block starts
				for (block = 0; block < blocks.length; block++)
				{
					byte[] geoBlock = null;
					final byte type = geo[index++];
					switch (type)
					{
						case BLOCKTYPE_FLAT:
							geoBlock = new byte[2 + 1];
							geoBlock[0] = type;
							geoBlock[1] = geo[index++];
							geoBlock[2] = geo[index++];
							break;
						case BLOCKTYPE_COMPLEX:
							geoBlock = new byte[128 + 1];
							geoBlock[0] = type;
							System.arraycopy(geo, index, geoBlock, 1, 128);
							index += 128;
							break;
						case BLOCKTYPE_MULTILEVEL:
							final int orgIndex = index;
							for (int b = 0; b < 64; b++)
							{
								final byte layers = geo[index++];
								flor = Math.max(flor, layers);
								_maxLayers = Math.max(_maxLayers, layers);
								index += (layers << 1);
							}
							final int diff = index - orgIndex;
							geoBlock = new byte[diff + 1];
							geoBlock[0] = type;
							System.arraycopy(geo, orgIndex, geoBlock, 1, diff);
							break;
						default:
							_log.severe("GeoEngine: invalid block type: " + type);
					}
					//blocks[block][0] = geoBlock;
					blocks[block] = geoBlock;
				}
				_geodata[rx - Config.WORLD_X_MIN][ry - Config.WORLD_Y_MIN] = blocks;
				_log.info("Geo Engine: - Max Layers: " + flor + " Size: " + size + " Loaded: " + index);
				return true;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			_log.warning("Failed to Load GeoFile at block: " + block + "\n");
			return false;
		}
		finally
		{
			try
			{
				roChannel.close();
			}
			catch (Exception e)
			{
			}
		}
		return false;
	}
	
	//Geodata Methods
	/**
	 * @param x
	 * @param y
	 * @return Region Offset
	 */
	private static short getRegionOffset(int x, int y)
	{
		int rx = x >> 11; // =/(256 * 8)
		int ry = y >> 11;
		return (short) (((rx + Config.WORLD_X_MIN) << 5) + (ry + Config.WORLD_Y_MIN));
	}

	private static final short makeShort(byte b1, byte b0)
	{
		return (short)(b1 << 8 | b0 & 0xFF);
	}

	/**
	 * @param pos
	 * @return Block Index: 0-255
	 */
	private static int getBlock(int geo_pos)
	{
		return (geo_pos >> 3) % 256;
	}
	
	/**
	 * @param pos
	 * @return Cell Index: 0-7
	 */
	private static int getCell(int geo_pos)
	{
		return geo_pos % 8;
	}

	private static int getBlockIndex(int blockX, int blockY)
	{
		return (blockX << 8) + blockY;
	}

	//Geodata Functions
	
	// 
	public static final void nGetLayers(int geoX, int geoY, short[] result, int instanceId)
	{
		result[0] = 0;
		final byte[] block = getGeoBlockFromGeoCoords(geoX, geoY, instanceId);
		if(block == null)
			return;

		final int cellX, cellY;
		int index = 0;
		switch (block[index++])
		{
			case BLOCKTYPE_FLAT:
				short height = makeShort(block[index + 1], block[index]);
				height = (short) (height & 0x0fff0);
				result[0]++;
				result[1] = (short) ((short) (height << 1) | NSWE_ALL);
				break;
			case BLOCKTYPE_COMPLEX:
				cellX = getCell(geoX);
				cellY = getCell(geoY);
				index += (cellX << 3) + cellY << 1;
				height = makeShort(block[index + 1], block[index]);
				result[0]++;
				result[1] = height;
				break;
			case BLOCKTYPE_MULTILEVEL:
				cellX = getCell(geoX);
				cellY = getCell(geoY);
				int offset = (cellX << 3) + cellY;
				while (offset > 0)
				{
					byte lc = block[index];
					index += (lc << 1) + 1;
					offset--;
				}
				byte layer_count = block[index++];
				if (layer_count <= 0 || layer_count > _maxLayers)
					break;
				result[0] = layer_count;
				while (layer_count > 0)
				{
					result[layer_count] = makeShort(block[index + 1], block[index]);
					layer_count--;
					index += 2;
				}
				break;
			default:
				_log.severe("GeoEngine: Unknown block type");
		}
	}

	// 
	public static final Layer[] nGetLayers(int geoX, int geoY, int instanceId)
	{
		final byte[] block = getGeoBlockFromGeoCoords(geoX, geoY, instanceId);
		if (block == null)
			return new Layer[0];

		final int cellX, cellY;
		int index = 0;
		switch (block[index++])
		{
			case BLOCKTYPE_FLAT:
				short height = makeShort(block[index + 1], block[index]);
				height = (short) (height & 0x0fff0);
				return new Layer[] { new Layer(height, NSWE_ALL) };
			case BLOCKTYPE_COMPLEX:
				cellX = getCell(geoX);
				cellY = getCell(geoY);
				index += (cellX << 3) + cellY << 1;
				height = makeShort(block[index + 1], block[index]);
				return new Layer[] { new Layer((short) ((short) (height & 0x0fff0) >> 1), (byte) (height & 0x0F)) };
			case BLOCKTYPE_MULTILEVEL:
				cellX = getCell(geoX);
				cellY = getCell(geoY);
				int offset = (cellX << 3) + cellY;
				while (offset > 0)
				{
					byte lc = block[index];
					index += (lc << 1) + 1;
					offset--;
				}
				byte layer_count = block[index++];
				if (layer_count <= 0 || layer_count > _maxLayers)
					return new Layer[0];
				Layer[] layers = new Layer[layer_count];
				while (layer_count > 0)
				{
					height = makeShort(block[index + 1], block[index]);
					layer_count--;
					layers[layer_count] = new Layer((short) ((short) (height & 0x0fff0) >> 1), (byte) (height & 0x0F));
					index += 2;
				}
				return layers;
			default:
				_log.severe("GeoEngine: Unknown block type");
				return new Layer[0];
		}
	}

	//
	private static final short nGetType(int geoX, int geoY, int instanceId)
	{
		final byte[] block = getGeoBlockFromGeoCoords(geoX, geoY, instanceId);
		if (block == null)
			return 0;

		return block[0];
	}

	// 
	private static final int nGetHeight(int geoX, int geoY, int z, int instanceId)
	{
		final byte[] block = getGeoBlockFromGeoCoords(geoX, geoY, instanceId);
		if (block == null)
			return z;

		final int cellX, cellY;
		int index = 0;
		short height;
		switch (block[index++])
		{
			case BLOCKTYPE_FLAT:
				height = makeShort(block[index + 1], block[index]);
				return (short) (height & 0x0fff0);
			case BLOCKTYPE_COMPLEX:
				cellX = getCell(geoX);
				cellY = getCell(geoY);
				index += (cellX << 3) + cellY << 1;
				height = makeShort(block[index + 1], block[index]);
				return (short) ((short) (height & 0x0fff0) >> 1); // height / 2
			case BLOCKTYPE_MULTILEVEL:
				cellX = getCell(geoX);
				cellY = getCell(geoY);
				int offset = (cellX << 3) + cellY;
				while(offset > 0)
				{
					byte lc = block[index];
					index += (lc << 1) + 1;
					offset--;
				}
				byte layers = block[index++];
				if (layers <= 0 || layers > _maxLayers)
					return z;

				int z_nearest_lower_limit = z + 64; // Config
				int z_nearest_lower = Integer.MIN_VALUE;
				int z_nearest = Integer.MIN_VALUE;

				while (layers > 0)
				{
					height = (short) ((short) (makeShort(block[index + 1], block[index]) & 0x0fff0) >> 1);
					if (height < z_nearest_lower_limit)
						z_nearest_lower = Math.max(z_nearest_lower, height);
					else if (Math.abs(z - height) < Math.abs(z - z_nearest))
						z_nearest = height;
					layers--;
					index += 2;
				}
				return z_nearest_lower != Integer.MIN_VALUE ? z_nearest_lower : z_nearest;
			default:
				_log.severe("GeoEngine: Unknown blockType");
				return z;
		}
	}

	//
	private static final byte nGetNSWE(int geoX, int geoY, int z, int instanceId)
	{
		final byte[] block = getGeoBlockFromGeoCoords(geoX, geoY, instanceId);
		if (block == null)
			return NSWE_ALL;

		final int cellX, cellY;
		int index = 0;
		switch (block[index++])
		{
			case BLOCKTYPE_FLAT:
				return NSWE_ALL;
			case BLOCKTYPE_COMPLEX:
				cellX = getCell(geoX);
				cellY = getCell(geoY);
				index += (cellX << 3) + cellY << 1;
				short height = makeShort(block[index + 1], block[index]);
				return (byte) (height & 0x0F);
			case BLOCKTYPE_MULTILEVEL:
				cellX = getCell(geoX);
				cellY = getCell(geoY);
				int offset = (cellX << 3) + cellY;
				while (offset > 0)
				{
					byte lc = block[index];
					index += (lc << 1) + 1;
					offset--;
				}
				byte layers = block[index++];
				if (layers <= 0 || layers > _maxLayers)
					return NSWE_ALL;

				short tempz1 = Short.MIN_VALUE;
				short tempz2 = Short.MIN_VALUE;
				int index_nswe1 = NSWE_NONE;
				int index_nswe2 = NSWE_NONE;
				int z_nearest_lower_limit = z + 64; // Config

				while (layers > 0)
				{
					height = (short) ((short) (makeShort(block[index + 1], block[index]) & 0x0fff0) >> 1); // height / 2
					if (height < z_nearest_lower_limit)
					{
						if (height > tempz1)
						{
							tempz1 = height;
							index_nswe1 = index;
						}
					}
					else if (Math.abs(z - height) < Math.abs(z - tempz2))
					{
						tempz2 = height;
						index_nswe2 = index;
					}
					layers--;
					index += 2;
				}

				if (index_nswe1 > 0)
					return (byte) (makeShort(block[index_nswe1 + 1], block[index_nswe1]) & 0x0F);
				if (index_nswe2 > 0)
					return (byte) (makeShort(block[index_nswe2 + 1], block[index_nswe2]) & 0x0F);

				return NSWE_ALL;
			default:
				_log.severe("GeoEngine: Unknown block type.");
				return NSWE_ALL;
		}
	}

	// 
	public static final void nGetHeightAndNSWE(int geoX, int geoY, short z, short[] result, int instanceId)
	{
		final byte[] block = getGeoBlockFromGeoCoords(geoX, geoY, instanceId);
		if (block == null)
		{
			result[0] = z;
			result[1] = NSWE_ALL;
			return;
		}

		final int cellX, cellY;
		int index = 0;
		short height;
		short NSWE = NSWE_ALL;

		switch(block[index++])
		{
			case BLOCKTYPE_FLAT:
				height = makeShort(block[index + 1], block[index]);
				result[0] = (short) (height & 0x0fff0);
				result[1] = NSWE_ALL;
				break;
			case BLOCKTYPE_COMPLEX:
				cellX = getCell(geoX);
				cellY = getCell(geoY);
				index += (cellX << 3) + cellY << 1;
				height = makeShort(block[index + 1], block[index]);
				result[0] = (short) ((short) (height & 0x0fff0) >> 1); // height / 2
				result[1] = (short) (height & 0x0F);
				break;
			case BLOCKTYPE_MULTILEVEL:
				cellX = getCell(geoX);
				cellY = getCell(geoY);
				int offset = (cellX << 3) + cellY;
				while (offset > 0)
				{
					byte lc = block[index];
					index += (lc << 1) + 1;
					offset--;
				}
				byte layers = block[index++];
				if (layers <= 0 || layers > _maxLayers)
				{
					result[0] = z;
					result[1] = NSWE_ALL;
					break;
				}

				short tempz1 = Short.MIN_VALUE;
				short tempz2 = Short.MIN_VALUE;
				int indexNSWE1 = 0;
				int indexNSWE2 = 0;
				int zNearestLowerLimit = z + 64; // Config

				while (layers > 0)
				{
					height = (short) ((short) (makeShort(block[index + 1], block[index]) & 0x0fff0) >> 1); // height / 2
					if (height < zNearestLowerLimit)
					{
						if (height > tempz1)
						{
							tempz1 = height;
							indexNSWE1 = index;
						}
					}
					else if (Math.abs(z - height) < Math.abs(z - tempz2))
					{
						tempz2 = height;
						indexNSWE2 = index;
					}

					layers--;
					index += 2;
				}

				if (indexNSWE1 > 0)
				{
					NSWE = makeShort(block[indexNSWE1 + 1], block[indexNSWE1]);
					NSWE = (short) (NSWE & 0x0F);
				}
				else if (indexNSWE2 > 0)
				{
					NSWE = makeShort(block[indexNSWE2 + 1], block[indexNSWE2]);
					NSWE = (short) (NSWE & 0x0F);
				}
				result[0] = tempz1 > Short.MIN_VALUE ? tempz1 : tempz2;
				result[1] = NSWE;
				break;
			default:
				_log.severe("GeoEngine: Unknown block type.");
				result[0] = z;
				result[1] = NSWE_ALL;
				break;
		}
	}

	/**
	 * @param NSWE
	 * @param x
	 * @param y
	 * @param tx
	 * @param ty
	 * @return True if NSWE dont block given direction
	 */
	private static boolean checkNSWE(short NSWE, int x, int y, int tx, int ty)
	{
		//Check NSWE
		if (NSWE == NSWE_ALL)
			return true;
		if (NSWE == NSWE_NONE)
			return false;
		if (tx > x)//E
		{
			if ((NSWE & EAST) == 0)
				return false;
		}
		else if (tx < x)//W
		{
			if ((NSWE & WEST) == 0)
				return false;
		}
		if (ty > y)//S
		{
			if ((NSWE & SOUTH) == 0)
				return false;
		}
		else if (ty < y)//N
		{
			if ((NSWE & NORTH) == 0)
				return false;
		}
		return true;
	}
	
	public static class Layer
	{
		public short height;
		public byte nswe;

		public Layer(short h, byte n)
		{
			height = h;
			nswe = n;
		}
	}

	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final GeoEngine _instance = new GeoEngine();
	}
}
