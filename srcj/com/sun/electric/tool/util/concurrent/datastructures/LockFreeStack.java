/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LockFreeStack.java
 *
 * Copyright (c) 2010 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
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
package com.sun.electric.tool.util.concurrent.datastructures;

import com.sun.electric.tool.util.EmptyException;
import com.sun.electric.tool.util.IStructure;
import java.util.concurrent.atomic.AtomicReference;


/**
 * 
 * Thread safe, lock free, concurrent stack
 * 
 * @param <T>
 */
public class LockFreeStack<T> extends IStructure<T> {

	private AtomicReference<Node<T>> top = new AtomicReference<Node<T>>(null);
	private static final int MIN_DELAY = 10;
	private static final int MAX_DELAY = 100;
	private Backoff backoff = new Backoff(MIN_DELAY, MAX_DELAY);

	@Override
	public void add(T item) {
		Node<T> node = new Node<T>(item);
		while (true) {
			if (tryPush(node)) {
				return;
			} else {
				try {
					backoff.backoff();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

	}

	private boolean tryPush(Node<T> node) {
		Node<T> oldTop = top.get();
		node.next = oldTop;
		return (top.compareAndSet(oldTop, node));
	}

	@Override
	public boolean isEmpty() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public T remove() {
		while (true) {
			Node<T> returnNode;
			try {
				returnNode = tryPop();
			} catch (EmptyException e) {
				return null;
			}
			if (returnNode != null) {
				return returnNode.value;
			} else {
				try {
					backoff.backoff();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private Node<T> tryPop() throws EmptyException {
		Node<T> oldTop = top.get();
		if (oldTop == null) {
			throw new EmptyException();
		}
		Node<T> newTop = oldTop.next;
		if (top.compareAndSet(oldTop, newTop))
			return oldTop;
		else
			return null;
	}

}