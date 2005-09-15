/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ArrayIterarator.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Electric(tm) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Electric(tm); see the file COPYING.  If not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, Mass 02111-1307, USA.
 */
package com.sun.electric.database.text;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An iterator over an array.
 */
public class ArrayIterator/*<E>*/ implements Iterator/*<E>*/ {
	private final Object/*E*/[] array;
    private int limit;
	private int cursor;

	/**
	 * Null iterator which has no elements.
	 */
	public static final ArrayIterator/*<E>*/ NULL_ITERATOR = new ArrayIterator/*<E>*/(new Object/*E*/[0]);

	private ArrayIterator/*E*/(Object/*E*/[] array) {
        this.array = array;
        limit = array.length;
        cursor = 0;
    }

    private ArrayIterator/*E*/(Object/*E*/[] array, int start, int limit)
	{
        if (start < 0 || start > limit || limit > array.length)
            throw new IndexOutOfBoundsException();
		this.array = array;
        this.limit = limit;
        cursor = start;
	}

	/**
	 * Returns iterator over elements of array.
	 * @param array array with elements or null.
	 * @return iterator over elements of the array or NULL_ITERATOR.
	 */
	public static Iterator iterator(Object/*E*/[] array)
	{
		return array != null && array.length > 0 ? new ArrayIterator(array) : NULL_ITERATOR;
	}

	/**
	 * Returns iterator over range [start,limit) of elements of array.
	 * @param array array with elements or null.
     * @param start start index of the range.
     * @param limit limit of the range
	 * @return iterator over range of elements of the array or NULL_ITERATOR.
     * @throws IndexOutOfBoundsException if start or limit are not correct
	 */
	public static Iterator iterator(Object/*E*/[] array, int start, int limit)
	{
        if (array != null) {
            return new ArrayIterator(array, start, limit);
        } else {
            if (start != 0 || limit != 0)
                throw new IndexOutOfBoundsException();
            return NULL_ITERATOR;
        }
	}

	/**
	 * Returns <tt>true</tt> if the iteration has more elements. (In other
	 * words, returns <tt>true</tt> if <tt>next</tt> would return an element
	 * rather than throwing an exception.)
	 *
	 * @return <tt>true</tt> if the iterator has more elements.
	 */
	public boolean hasNext()
	{
		return cursor < limit;
	}

	/**
	 * Returns the next element in the iteration.  Calling this method
	 * repeatedly until the {@link #hasNext()} method returns false will
	 * return each element in the underlying collection exactly once.
	 *
	 * @return the next element in the iteration.
	 * @exception NoSuchElementException iteration has no more elements.
	 */
	public Object/*E*/ next()
	{
        if (cursor >= limit)
			throw new NoSuchElementException();
		Object/*E*/ next = array[cursor];
		cursor++;
		return next;
    }

	/**
	 * Removes from the underlying collection the last element returned by the
	 * iterator (unsupported operation).
	 *
	 * @exception UnsupportedOperationException 
	 */
    public void remove()
    {
		throw new UnsupportedOperationException();
    }
}
