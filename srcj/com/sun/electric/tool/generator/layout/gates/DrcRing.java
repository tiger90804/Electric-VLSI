/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DrcRing.java
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
package com.sun.electric.tool.generator.layout.gates;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.tool.generator.layout.LayoutLib;
import com.sun.electric.tool.generator.layout.StdCellParams;
import com.sun.electric.tool.generator.layout.Tech;

/** Create a ring in layers p1 and m1 - m5.  This ring is useful for
 * testing DRC correctness of my gate libraries.  I will draw a ring
 * around every gate and then run DRC. */
public class DrcRing {
  private static void error(boolean pred, String msg) {
    LayoutLib.error(pred, msg);
  }

  /** Draw a rectangular ring from arc.  The space inside the ring has
   * width w and height h.  Position the ring so that the bottom left
   * inside corner is at (0, 0).  All edges are on a 0.5 lambda grid
   * as long as w and h are multiples of 0.5 lambda. */
  private static void drawRing(ArcProto arc, double w, double h, Cell f) {
    // metal-5 minimum width is 6 lambda
    double arcW = 6;
    
    NodeProto pin = ((PrimitiveArc)arc).findOverridablePinProto();
    double pinLoX = - arcW/2;
    double pinHiX = w + arcW/2;
    double pinLoY = - arcW/2;
    double pinHiY = h + arcW/2;
    /*
    double pinLoX = -w/2 - arcW/2;
    double pinHiX = w/2 + arcW/2;
    double pinLoY = -h/2 - arcW/2;
    double pinHiY = h/2 + arcW/2;
    */
	double defSz = LayoutLib.DEF_SIZE;
    PortInst blPort = LayoutLib.newNodeInst(pin, pinLoX, pinLoY, defSz, defSz,
                                            0, f).getOnlyPortInst();
    PortInst brPort = LayoutLib.newNodeInst(pin, pinHiX, pinLoY, defSz, defSz, 
                                            0, f).getOnlyPortInst();
    PortInst tlPort = LayoutLib.newNodeInst(pin, pinLoX, pinHiY, defSz, defSz, 
                                            0, f).getOnlyPortInst();
    PortInst trPort = LayoutLib.newNodeInst(pin, pinHiX, pinHiY, defSz, defSz,
                                            0, f).getOnlyPortInst();
    LayoutLib.newArcInst(arc, arcW, blPort, brPort);
    LayoutLib.newArcInst(arc, arcW, tlPort, trPort);
    LayoutLib.newArcInst(arc, arcW, blPort, tlPort);
    LayoutLib.newArcInst(arc, arcW, brPort, trPort);
  }

  /** Draw rings with inside width w and inside height h. The ring
   * position places the bottom left inside corner at (0, 0) */
  public static Cell makePart(double w, double h, StdCellParams stdCell) {
    String nm = "drcRing_W"+w+"_H"+h+"{lay}";

    Cell ring = stdCell.findPart(nm);
    if (ring!=null) return ring;
    ring = stdCell.newPart(nm);

    drawRing(Tech.p1, w, h, ring);
    drawRing(Tech.m1, w, h, ring);
    drawRing(Tech.m2, w, h, ring);
    drawRing(Tech.m3, w, h, ring);
    drawRing(Tech.m4, w, h, ring);
    drawRing(Tech.m5, w, h, ring);

    return ring;
  }
}

