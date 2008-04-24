/* This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package net.sf.l2j.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * Fully synchronized version of HashMap class.<br>
 * In addition it`s provide ForEach method and interface that can be used for iterating collection<br>
 *  without using temporary collections. As addition its provide full lock on entire class if needed<br>
 *  (by default {@link ConcurrentHashMap} does not provide this functionality)<br>
 * <br>
 * @author  Julian Version: 1.0.0 <2008-02-07>
 */
public class L2SyncMap<K extends Object, V extends Object> extends ConcurrentHashMap<K, V>
{
    static final long serialVersionUID = 1L;
    
    /**
     * Public inner interface used by ForEach iterations<br>
     *
     * @author  Julian
     */
    public interface I2ForEach<K,V> {
        public boolean forEach(K key, V val);
    }

    /**
     * Public method that iterate entire collection.<br>
     * <br>
     * @param func - a class method that must be executed on every element of collection.<br>
     * @param sync - if set to true, will lock entire collection.<br>
     * @return - returns true if entire collection is iterated, false if it`s been interrupted by<br>
     *             check method (I2ForEach.forEach())<br>
     */
    public final boolean ForEach(I2ForEach<K,V> func, boolean sync) {
        if (sync)
            synchronized (this) { return forEachP(func); }
        else
            return forEachP(func);
    }
    
    // private method that implements forEach iteration
    private final boolean forEachP(I2ForEach<K,V> func) {
        for (Map.Entry<K,V> e: this.entrySet())
            if (!func.forEach(e.getKey(),e.getValue())) return false;
        return true;
    }
}
