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
package net.sf.l2j.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import net.sf.l2j.util.L2FastList.I2ForEach;

/**
 *
 * @author  Julian
 */
public class L2SyncList<T extends Object> implements List<T> 
{
	private final L2FastList<T> _list;
	
	public L2SyncList() {
		_list = new L2FastList<T>();
	}

	public L2SyncList(List<? extends T> list) {
		_list = new L2FastList<T>(list);
	}
	
	public synchronized T get(int index) {
		return _list.get(index);
	}
	
    public synchronized boolean equals(Object o) {
        return _list.equals(o);
    }
    public synchronized int hashCode() {
        return _list.hashCode();
    }

    public synchronized T set(int index, T element) {
        return _list.set(index, element);
    }
    
    public synchronized void add(int index, T element) {
        _list.add(index, element);
    }

    public synchronized boolean add(T element) {
    	return _list.add(element);
    }
    
    public synchronized T remove(int index) {
        return _list.remove(index);
    }

    public synchronized boolean remove(Object value) {
    	return _list.remove(value);
    }
    
    public synchronized boolean removeAll(Collection<?> list) {
    	return _list.removeAll(list);
    }

    public synchronized boolean retainAll(Collection<?> list) {
    	return _list.retainAll(list);
    }
    public synchronized int indexOf(Object o) {
        return _list.indexOf(o);
    }

    public synchronized boolean contains(Object o) {
    	return _list.contains(o);
    }
    
    public synchronized boolean containsAll(Collection<?> list) {
    	return _list.containsAll(list);
    }
    
    public synchronized int lastIndexOf(Object o) {
        return _list.lastIndexOf(o);
    }

    public synchronized boolean addAll(Collection<? extends T> list) {
    	return _list.addAll(list);
    }
    
    public synchronized boolean addAll(int index, Collection<? extends T> c) {
        return _list.addAll(index, c);
    }
    
    public synchronized List<T> subList(int fromIndex, int toIndex) {
        return new L2SyncList<T>(_list.subList(fromIndex, toIndex));
    }

    public synchronized void clear() {
    	_list.clear();
    }

    public synchronized int size() {
    	return _list.size();
    }
    
    public synchronized boolean isEmpty() {
    	return _list.isEmpty();
    }

    public synchronized boolean forEach(I2ForEach<T> func) {
    	return _list.forEach(func);
    }
    
    /**
     * @deprecated
     * @see java.util.List#listIterator()
     */
    public ListIterator<T> listIterator() {
    	throw new UnsupportedOperationException();
    }

    /**
     * @deprecated
     * @see java.util.List#listIterator(int)
     */
    public ListIterator<T> listIterator(int index) {
    	throw new UnsupportedOperationException();
    }

    /**
     * @deprecated
     * @see java.util.List#iterator()
     */
    public Iterator<T> iterator() {
    	throw new UnsupportedOperationException();
    }
    
    /**
     * @deprecated
     * @see java.util.List#toArray()
     */
	public synchronized Object[] toArray() {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * @deprecated
	 * @see java.util.List#toArray(T[])
	 */
	@SuppressWarnings("hiding")
	public synchronized <T> T[] toArray(T[] a) {
		throw new UnsupportedOperationException();
	}
}
