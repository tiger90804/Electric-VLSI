/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SizeOffset.java
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
package com.sun.electric.technology;

// In Electric, nonexistant "invisible" material surrounds many
// NodeProtos and ArcProtos.  For example, in the MOCMOS technology,
// if you ask Electric for a 5 square metal-1/metal-2 contact
// you get a 4 square metal-1 and 4 square metal-2
// surrounded by a 1/2 "invisible" perimeter.  This perimeter
// doesn't scale.  If you ask for an 8 square metal-1/metal-2
// contact you get a 9 square metal-1 and metal-2 and a 1/2
// surround.
//
// This invisible surround is a pain for a Jose client to deal
// with. My goal is to hide this from Jose client programs. 
//
// This class encodes the dimensions of the invisible surround.
// NodeProtos and ArcProtos need to pass this information to Geometric
// so it may properly compute NodeInst and ArcInst bounding boxes
// without this ridiculous invisible surround.

public class SizeOffset
{
	final double lx, ly, hx, hy;
	public SizeOffset(double lx, double ly, double hx, double hy)
	{
		this.lx = lx;
		this.ly = ly;
		this.hx = hx;
		this.hy = hy;
	}

	public double getLowXOffset() { return lx; }
	public double getHighXOffset() { return hx; }
	public double getLowYOffset() { return ly; }
	public double getHighYOffset() { return hy; }

	public String toString()
	{
		return "SizeOffset = {\n"
			+ "    x: [" + lx + "-" + hx + "]\n"
			+ "    y: [" + ly + "-" + hy + "]\n"
			+ "}\n";
	}
}
