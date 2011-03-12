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
package com.l2jserver.communityserver.util;

import java.util.Random;

public final class Rnd
{
	private static final Random rnd = new Random();
	
	public static final double get()	// get random number from 0 to 1
	{
        return rnd.nextDouble();
	}
	
	/**
	 * Gets a random number from 0(inclusive) to n(exclusive)
	 *
	 * @param n The superior limit (exclusive)
	 * @return A number from 0 to n-1
	 */
    public static final int get(final int n) // get random number from 0 to n-1
    {
        return (int)(rnd.nextDouble() * n);
    }
    
    public static final int get(int min, int max) // get random number from min to max (not max-1 !)
    {
        return min + (int)Math.floor(rnd.nextDouble() * (max - min + 1));
    }
    
    public static final int nextInt(int n)
    {
        return (int)Math.floor(rnd.nextDouble() * n);
    }

    public static final int nextInt()
    {
        return rnd.nextInt();
    }
    
    public static final double nextDouble()
    {
        return rnd.nextDouble();
    }
    
    public static final double nextGaussian()
    {
        return rnd.nextGaussian();
    }
    
    public static final boolean nextBoolean()
    {
        return rnd.nextBoolean();
    }
    
    public static final void nextBytes(byte[] array)
    {
    	rnd.nextBytes(array);
    }
}