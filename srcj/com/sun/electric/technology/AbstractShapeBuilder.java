/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: AbstractShapeBuilder.java
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

import com.sun.electric.database.CellBackup;
import com.sun.electric.database.variable.EditWindow0;
import com.sun.electric.database.variable.VarContext;

/**
 * A support class to build shapes of arcs and nodes.
 */
public abstract class AbstractShapeBuilder {
    protected EditWindow0 wnd;
    protected Layer.Function.Set onlyTheseLayers;
    protected VarContext varContext;
    protected boolean reasonable;
    protected boolean electrical;
    
    private long[] points = new long[10];
    private CellBackup.Memoization m;
    
    /** Creates a new instance of AbstractShapeBuilder */
    public AbstractShapeBuilder() {
    }
    
    public void setEditWindow(EditWindow0 wnd) { this.wnd = wnd; }
    public void setOnlyTheseLayers(Layer.Function.Set onlyTheseLayers) { this.onlyTheseLayers = onlyTheseLayers; }
    public void setVarContext(VarContext varContext) { this.varContext = varContext; }
    public void setReasonable(boolean b) { reasonable = b; }
    public void setElectrical(boolean b) { electrical = b; }
    
    public long[] getPoints(int count) {
        if (count*2 > points.length)
            points = new long[Math.max(count*2, points.length*2)];
        return points;
    }
    
    public CellBackup.Memoization getMemoization() {
        return m;
    }
}
