/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Timer.java
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
package com.sun.electric.tool.util;

import com.sun.electric.database.text.TextUtils;

/**
 * @author Felix Schmidt
 * 
 */
public class Timer {

	private long start = 0;
	private long end = 0;

	public static Timer createInstance() {
		return new Timer();
	}

	public void start() {
		this.start = System.currentTimeMillis();
	}

	public void end() {
		this.end = System.currentTimeMillis();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return TextUtils.getElapsedTime(end - start);
	}

	public void print(String preText) {
		System.out.println(preText + this.toString());
	}

}