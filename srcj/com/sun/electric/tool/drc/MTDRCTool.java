/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Quick.java
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.drc;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.Consumer;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.MultiTaskJob;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.DRCRules;

import java.util.Collection;
import java.util.Map;

/**
 * User: gg151869
 * Date: Dec 12, 2007
 */
public abstract class MTDRCTool extends MultiTaskJob<Layer, MTDRCTool.MTDRCResult, MTDRCTool.MTDRCResult>
{
    protected Cell topCell;
    protected long globalStartTime;
    protected CellLayersContainer cellLayersCon = new CellLayersContainer();
    protected final boolean printLog = false;
    protected DRCRules rules;

    public MTDRCTool(String jobName, Cell c, Consumer<MTDRCResult> consumer)
    {
        super(jobName, DRC.getDRCTool(), Job.Type.CHANGE, consumer);
        this.topCell= c;
        // Rules set must be local to avoid concurrency issues with other tasks
        this.rules = topCell.getTechnology().getFactoryDesignRules();
    }

    @Override
    public void prepareTasks()
    {
        Technology tech = topCell.getTechnology();
        cellLayersCon = new CellLayersContainer();
        CheckCellLayerEnumerator layerCellCheck = new CheckCellLayerEnumerator(cellLayersCon);
        HierarchyEnumerator.enumerateCell(topCell, VarContext.globalContext, layerCellCheck);
        Collection<String> layers = cellLayersCon.getLayersSet(topCell);
        globalStartTime = System.currentTimeMillis();
        for (String layerS : layers)
        {
            Layer layer = tech.findLayer(layerS);
            startTask(layer.getName(), layer);
        }
        if (!checkArea())
            startTask("Node Min Size.", null);
    }

    @Override
    public MTDRCResult mergeTaskResults(Map<Layer,MTDRCResult> taskResults)
    {
        int numTE = 0, numTW = 0;
        for (Map.Entry<Layer, MTDRCResult> e : taskResults.entrySet())
        {
            MTDRCResult p = e.getValue();
            numTE += p.numErrors;
            numTW += p.numWarns;
        }
        System.out.println("Total DRC Errors: " + numTE);
        System.out.println("Total DRC Warnings: " + numTW);
        long accuEndTime = System.currentTimeMillis() - globalStartTime;             
        System.out.println("Total Time: " + TextUtils.getElapsedTime(accuEndTime));
        return new MTDRCResult(numTE, numTW);
    }

    @Override
    public MTDRCResult runTask(Layer taskKey)
    {
        if (skipLayer(taskKey))
            return null;
        return runTaskInternal(taskKey);
    }

    abstract MTDRCResult runTaskInternal(Layer taskKey);

    abstract boolean checkArea();

    boolean skipLayer(Layer theLayer)
    {
        if (theLayer == null) return false;
        
        if (theLayer.getFunction().isDiff() && theLayer.getName().toLowerCase().equals("p-active-well"))
            return true; // dirty way to skip the MoCMOS p-active well
        else if (theLayer.getFunction().isGatePoly())
            return true; // check transistor-poly together with polysilicon-1
        else if (checkArea() && theLayer.getFunction().isContact())  // via*, polyCut, activeCut
            return true;
        return false;
    }

    public static class MTDRCResult
    {
        private int numErrors, numWarns;

        MTDRCResult(int numE, int numW)
        {
            this.numErrors = numE;
            this.numWarns = numW;
        }

        public int getNumErrors()
        {
            return numErrors;
        }

        public int getNumWarnings()
        {
            return numWarns;
        }
    }
}
