/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LENetlister.java
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
 *
 * Created on November 11, 2003, 3:56 PM
 */

package com.sun.electric.tool.logicaleffort;

import com.sun.electric.tool.logicaleffort.*;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.JNetwork;
import com.sun.electric.database.topology.*;
import com.sun.electric.database.prototype.*;
import com.sun.electric.database.variable.*;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.ui.MessagesWindow;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.technology.PrimitiveNode;

import java.awt.geom.AffineTransform;
import java.awt.*;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.*;
import java.util.List;

/**
 * Creates a logical effort netlist to be sized by LESizer.
 * This is so the LESizer is independent of Electric's Database,
 * and can match George Chen's C++ version being developed for 
 * PNP.
 *
 * @author  gainsley
 */
public class LENetlister2 extends HierarchyEnumerator.Visitor implements LENetlister {
    
    // ALL GATES SAME DELAY
    /** global step-up */                       private float su;
    /** wire to gate cap ratio */               private float wireRatio;
    /** convergence criteron */                 private float epsilon;
    /** max number of iterations */             private int maxIterations;
    /** gate cap, in fF/lambda */               private float gateCap;
    /** ratio of diffusion to gate cap */       private float alpha;
    /** ratio of keeper to driver size */       private float keeperRatio;

    /** Map of Cells to CachedCells */          private Map cellMap;
    /** Map of globalID's to LENetworks */      private Map globalNetworks;
    /** List of sizeable unique LENodables */   private List sizableLENodables;
    /** List of all unique LENodables */        private List allLENodables;
    /** Map of Nodables to LENodable definitions */     private Map nodablesDefinitions;

    /** Sizer */                                private LESizer2 sizer;
    /** Job we are part of */                   private Job job;
    /** Where to direct output */               private PrintStream out;

    /** True if we got aborted */               private boolean aborted;
    /** for logging errors */                   private ErrorLogger errorLogger;

    private static final boolean DEBUG = false;
    private static final boolean DISABLE_CACHING = false;

    /** Creates a new instance of LENetlister */
    public LENetlister2(Job job) {
        // get preferences for this package
        Tool leTool = Tool.findTool("logical effort");
        su = (float)LETool.getGlobalFanout();
        epsilon = (float)LETool.getConvergenceEpsilon();
        maxIterations = LETool.getMaxIterations();
        gateCap = (float)LETool.getGateCapacitance();
        wireRatio = (float)LETool.getWireRatio();
        alpha = (float)LETool.getDiffAlpha();
        keeperRatio = (float)LETool.getKeeperRatio();
        
        this.job = job;
        this.cellMap = new HashMap();
        this.globalNetworks = new HashMap();
        this.sizableLENodables = new ArrayList();
        this.allLENodables = new ArrayList();
        this.nodablesDefinitions = new HashMap();
        this.out = new PrintStream((OutputStream)System.out);

        errorLogger = null;
        aborted = false;
    }
    
    // Entry point: This netlists the cell
    public void netlist(Cell cell, VarContext context) {

        //ArrayList connectedPorts = new ArrayList();
        //connectedPorts.add(Schematics.tech.resistorNode.getPortsList());
        if (errorLogger != null) errorLogger.delete();
        errorLogger = ErrorLogger.newInstance("LE Netlister");

        Netlist netlist = cell.getNetlist(true);
        
        // read schematic-specific sizing options
        for (Iterator instIt = cell.getNodes(); instIt.hasNext();) {
            NodeInst ni = (NodeInst)instIt.next();
            if (ni.getVar("ATTR_LESETTINGS") != null) {
                useLESettings(ni, context);            // get settings from object
                break;
            }
        }

        FirstPassEnum firstPass = new FirstPassEnum(this);
        HierarchyEnumerator.enumerateCell(cell, context, netlist, firstPass);
        firstPass.cleanup();
        System.out.println("Cached "+cellMap.size()+" cells");
        HierarchyEnumerator.enumerateCell(cell, context, netlist, this);
    }

    /**
     * Size the netlist.
     * @return true on success, false otherwise.
     */
    public boolean size(LESizer.Alg algorithm) {
        //lesizer.printDesign();
        boolean verbose = false;
        // create a new sizer
        sizer = new LESizer2(algorithm, this, job, errorLogger);
        boolean success = sizer.optimizeLoops(epsilon, maxIterations, verbose, alpha, keeperRatio);
        //out.println("---------After optimization:------------");
        //lesizer.printDesign();
        // get rid of the sizer
        sizer = null;
        return success;
    }

    /**
     * Updates the size of all Logical Effort gates
     */
    public void updateSizes() {
        // iterator over all LEGATEs
        for (Iterator cit = getSizeableNodables(); cit.hasNext(); ) {
            LENodable leno = (LENodable)cit.next();
            Nodable no = leno.getNodable();
            NodeInst ni = no.getNodeInst();
            if (ni != null) no = ni;

            // ignore it if not a sizeable gate
            if (!leno.isLeGate()) continue;
            String varName = "LEDRIVE_" + leno.getName();
            no.newVar(varName, new Float(leno.leX));

            if (leno.leX < 1.0f) {
                String msg = "WARNING: Instance "+ni.describe()+" has size "+TextUtils.formatDouble(leno.leX, 3)+" less than 1 ("+leno.getName()+")";
                System.out.println(msg);
                if (ni != null) {
                    ErrorLogger.ErrorLog log = errorLogger.logError(msg, ni.getParent(), 2);
                    log.addGeom(ni, true, ni.getParent(), leno.context);
                }
            }
        }

        printStatistics();
        done();
    }

    public void done() {
        errorLogger.termLogging(true);
        errorLogger = null;
    }

    /** NodeInst should be an LESettings instance */
    private void useLESettings(NodeInst ni, VarContext context) {
        Variable var;
        if ((var = ni.getVar("ATTR_su")) != null) su = VarContext.objectToFloat(context.evalVar(var), su);
        if ((var = ni.getVar("ATTR_wire_ratio")) != null) wireRatio = VarContext.objectToFloat(context.evalVar(var), wireRatio);
        if ((var = ni.getVar("ATTR_epsilon")) != null) epsilon = VarContext.objectToFloat(context.evalVar(var), epsilon);
        if ((var = ni.getVar("ATTR_max_iter")) != null) maxIterations = VarContext.objectToInt(context.evalVar(var), maxIterations);
        if ((var = ni.getVar("ATTR_gate_cap")) != null) gateCap = VarContext.objectToFloat(context.evalVar(var), gateCap);
        if ((var = ni.getVar("ATTR_alpha")) != null) alpha = VarContext.objectToFloat(context.evalVar(var), alpha);
        if ((var = ni.getVar("ATTR_keeper_ratio")) != null) keeperRatio = VarContext.objectToFloat(context.evalVar(var), keeperRatio);
    }

    protected Iterator getSizeableNodables() { return sizableLENodables.iterator(); }

    protected float getGlobalSU() { return su; }

    protected LESizer2 getSizer() { return sizer; }

    protected float getKeeperRatio() { return keeperRatio; }

    private LENetwork getNetwork(int globalID, HierarchyEnumerator.CellInfo info) {
        LENetwork net = (LENetwork)globalNetworks.get(new Integer(globalID));
        if (net == null) {
            String name = (info == null) ? null : info.getUniqueNetName(globalID, ".");
            net = new LENetwork(name);
            globalNetworks.put(new Integer(globalID), net);
        }
        return net;
    }

    // ======================= Hierarchy Enumerator ==============================


    /**
     * The first pass creates the definitions for all LENodables, and sees which
     * Cells can be cached (i.e. do not have parameters that need parent context
     * to evaluate, and do not have sizeable gates in them)
     */
    private static class FirstPassEnum extends HierarchyEnumerator.Visitor {

        /** LENetlister2 */                 private LENetlister2 netlister;

        private FirstPassEnum(LENetlister2 netlister) {
            this.netlister = netlister;
        }

        /**
         * Override the default Cell info to pass along logical effort specific information
         * @return a LECellInfo
         */
        public HierarchyEnumerator.CellInfo newCellInfo() { return new LECellInfo(); }

        public boolean enterCell(HierarchyEnumerator.CellInfo info) {
            if (netlister.aborted) return false;

            if (((LETool.AnalyzeCell)netlister.job).checkAbort(null)) {
                netlister.aborted = true;
                return false;
            }
            CachedCell cachedCell = (CachedCell)netlister.cellMap.get(info.getCell());
            if (cachedCell == null) {
                cachedCell = new CachedCell(info.getCell(), info.getNetlist());
                netlister.cellMap.put(info.getCell(), cachedCell);
                return true;
            } else {
                // because this cell is already cached, we will not be visiting nodeinsts,
                // and we will not be calling exit cell. So link into parent here, because
                // we won't be linking into parent from exit cell.
                // add this to parent cached cell if any
                HierarchyEnumerator.CellInfo parentInfo = info.getParentInfo();
                if (parentInfo != null) {
                    Cell parent = info.getParentInfo().getCell();
                    CachedCell parentCached = (CachedCell)netlister.cellMap.get(parent);
                    Nodable no = info.getParentInst();
                    parentCached.add(no, (LECellInfo)info.getParentInfo(), cachedCell, (LECellInfo)info, netlister.wireRatio);
                }
            }
            return false;
        }

        public boolean visitNodeInst(Nodable ni, HierarchyEnumerator.CellInfo info) {
            CachedCell cachedCell = (CachedCell)netlister.cellMap.get(info.getCell());

            // see if we can make an LENodable from the nodable
            LENodable.Type type = netlister.getType(ni, info);
            if (type == null) return true;                  // recurse
            LENodable leno = netlister.createLENodable(type, ni, info);
            // if no lenodable, recurse
            if (leno == null) return true;
            cachedCell.add(ni, leno);
            netlister.nodablesDefinitions.put(ni, leno);
            return false;
        }

        public void exitCell(HierarchyEnumerator.CellInfo info) {
            CachedCell cachedCell = (CachedCell)netlister.cellMap.get(info.getCell());

            // add this to parent cached cell if any
            HierarchyEnumerator.CellInfo parentInfo = info.getParentInfo();
            if (parentInfo != null) {
                Cell parent = info.getParentInfo().getCell();
                CachedCell parentCached = (CachedCell)netlister.cellMap.get(parent);
                Nodable no = info.getParentInst();
                parentCached.add(no, (LECellInfo)info.getParentInfo(), cachedCell, (LECellInfo)info, netlister.wireRatio);
            }
        }

        protected void cleanup() {
            // remove all cachedCells that contain sizeable gates or are not context free
            HashMap cachedMap = new HashMap();
            for (Iterator it = netlister.cellMap.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry entry = (Map.Entry)it.next();
                Cell cell = (Cell)entry.getKey();
                CachedCell cachedCell = (CachedCell)entry.getValue();
                if (cachedCell.isContextFree(netlister.wireRatio)) {
                    cachedMap.put(cell, cachedCell);
                }
            }
            netlister.cellMap = cachedMap;
            if (DISABLE_CACHING) netlister.cellMap = new HashMap();
        }
    }

    /**
     * Override the default Cell info to pass along logical effort specific information
     * @return a LECellInfo
     */
    public HierarchyEnumerator.CellInfo newCellInfo() { return new LECellInfo(); }

    /**
     * Enter cell initializes the LECellInfo.
     * @param info the LECellInfo
     * @return true to process the cell, false to ignore.
     */
    public boolean enterCell(HierarchyEnumerator.CellInfo info) {
        if (aborted) return false;

        if (((LETool.AnalyzeCell)job).checkAbort(null)) {
            aborted = true;
            return false;
        }

        LECellInfo leinfo = (LECellInfo)info;
        leinfo.leInit();

        boolean enter = true;
        // if there is a cachedCell, do not enter
        CachedCell cachedCell = (CachedCell)cellMap.get(info.getCell());
        // if this was a cached cell, link cached networks into global network
        // not that a cached cell cannot by definition contain sizeable LE gates
        if ((cachedCell != null) && (leinfo.getMFactor() == 1f)) {
            for (Iterator it = cachedCell.getLocalNetworks().entrySet().iterator(); it.hasNext(); ) {
                Map.Entry entry = (Map.Entry)it.next();
                JNetwork jnet = (JNetwork)entry.getKey();
                LENetwork subnet = (LENetwork)entry.getValue();
                int globalID = info.getNetID(jnet);
                LENetwork net = (LENetwork)getNetwork(globalID, info);
                if (net == null) continue;
                net.add(subnet);
            }
            for (Iterator it = cachedCell.getAllCachedNodables().iterator(); it.hasNext(); ) {
                allLENodables.add(it.next());
            }
            enter = false;
        }

        return enter;
    }

    /**
     * Visit NodeInst creates a new Logical Effort instance from the
     * parameters found on the Nodable, if that Nodable is an LEGATE.
     * It also creates instances for wire models (LEWIREs).
     * @param ni the Nodable being visited
     * @param info the cell info
     * @return true to push down into the Nodable, false to continue.
     */
    public boolean visitNodeInst(Nodable ni, HierarchyEnumerator.CellInfo info) {
        LECellInfo leinfo = (LECellInfo)info;

        LENodable def = (LENodable)nodablesDefinitions.get(ni);
        if (def == null) return true;
        else {
            // create hierarchical unique instance from definition
            LENetwork outputNet = null;
            if (def.isLeGate()) {
                // get global output network
                JNetwork outJNet = def.getOutputJNet();
                int globalID = info.getNetID(outJNet);
                outputNet = getNetwork(globalID, info);
            }
            float localsu = su;
            if (leinfo.getSU() != -1f) localsu = leinfo.getSU();
            LENodable uniqueLeno = def.createUniqueInstance(info.getContext(), outputNet,
                    leinfo.getMFactor(), localsu, wireRatio);
            if (uniqueLeno.isLeGate())
                sizableLENodables.add(uniqueLeno);
            allLENodables.add(uniqueLeno);
            // add pins to global networks
            for (Iterator pit = uniqueLeno.getPins().iterator(); pit.hasNext(); ) {
                LEPin pin = (LEPin)pit.next();
                int globalID = info.getNetID(pin.getJNetwork());
                LENetwork net = getNetwork(globalID, info);
                net.add(pin);
            }
        }
        return false;
    }

    public void doneVisitNodeInst(Nodable ni, HierarchyEnumerator.CellInfo info) {}

    /**
     * Nothing to do for exitCell
     */
    public void exitCell(HierarchyEnumerator.CellInfo info) {

    }

    /**
     * Logical Effort Cell Info class.  Keeps track of:
     * <p>- M factors
     */
    public static class LECellInfo extends HierarchyEnumerator.CellInfo {

        /** M-factor to be applied to size */       private float mFactor;
        /** SU to be applied to gates in cell */    private float cellsu;
        /** the cached cell */                      private CachedCell cachedCell;

        /** initialize LECellInfo: assumes CellInfo.init() has been called */
        protected void leInit() {

            HierarchyEnumerator.CellInfo parent = getParentInfo();

            // check for M-Factor from parent
            if (parent == null) mFactor = 1f;
            else mFactor = ((LECellInfo)parent).getMFactor();

            // check for su from parent
            if (parent == null) cellsu = -1f;
            else cellsu = ((LECellInfo)parent).getSU();

            // get info from node we pushed into
            Nodable ni = getContext().getNodable();
            if (ni == null) return;

            // get mfactor from instance we pushed into
            Variable mvar = ni.getVar("ATTR_M");
            if (mvar != null) {
                Object mval = getContext().evalVar(mvar, null);
                if (mval != null)
                    mFactor = mFactor * VarContext.objectToFloat(mval, 1f);
            }

            // get su from instance we pushed into
            Variable suvar = ni.getVar("ATTR_su");
            if (suvar != null) {
                float su = VarContext.objectToFloat(getContext().evalVar(suvar, null), -1f);
                if (su != -1f) cellsu = su;
            }
        }
        
        /** get mFactor */
        protected float getMFactor() { return mFactor; }

        protected float getSU() { return cellsu; }

        protected void setCachedCell(CachedCell c) { cachedCell = c; }
        protected CachedCell getCachedCell() { return cachedCell; }
    }


    /**
     * Get the LENodable type of this Nodable. If it is not a valid type, return null.
     * @param ni the Nodable to examine
     * @param info the current info
     * @return the LENodable type, or null if not an LENodable
     */
    private LENodable.Type getType(Nodable ni, HierarchyEnumerator.CellInfo info) {

        Variable var = null;
        if ((var = ni.getParameter("ATTR_LEGATE")) != null) {
            // assume it is LEGATE if can't resolve value
            int gate = VarContext.objectToInt(info.getContext().evalVar(var), 1);
            if (gate == 1)
                return LENodable.Type.LEGATE;
        }
        else if ((var = ni.getParameter("ATTR_LEKEEPER")) != null) {
            // assume it is LEKEEPER if can't resolve value
            int gate = VarContext.objectToInt(info.getContext().evalVar(var), 1);
            if (gate == 1)
                return LENodable.Type.LEKEEPER;
        }
        else if (ni.getParameter("ATTR_LEWIRE") != null) {
            return LENodable.Type.WIRE;
        }
        else if ((ni.getProto() != null) && (ni.getProto().getFunction().isTransistor())) {
            return LENodable.Type.TRANSISTOR;
        }
        return null;
    }

    /**
     * Create an LENodable of the given type for the Nodable
     * @param type the type to create
     * @param ni the source nodable
     * @param info the current info
     * @return an LENodable, or null if error
     */
    private LENodable createLENodable(LENodable.Type type, Nodable ni, HierarchyEnumerator.CellInfo info) {
        if (type == null) return null;
        Variable var = null;

        if (DEBUG) System.out.println("------------------------------------");

        // Build an LENodable.
        LENodable lenodable = new LENodable(ni, type, ni.getParameter("ATTR_M"), ni.getParameter("ATTR_su"), ni.getParameter("ATTR_LEPARALLGRP"));
        JNetwork outputJNet = null;

		Netlist netlist = info.getNetlist();
		for (Iterator ppIt = ni.getProto().getPorts(); ppIt.hasNext();) {
			PortProto pp = (PortProto)ppIt.next();
            var = pp.getVar("ATTR_le");
            // Note: default 'le' value should be one
            float le = 1.0f;
            if (var != null) le = VarContext.objectToFloat(info.getContext().evalVar(var), (float)1.0);
            JNetwork jnet = netlist.getNetwork(ni, pp, 0);
            LEPin.Dir dir = LEPin.Dir.INPUT;
            // if it's not an output, it doesn't really matter what it is.
            if (pp.getCharacteristic() == PortProto.Characteristic.OUT) {
                dir = LEPin.Dir.OUTPUT;
                // set output net
                if ((type == LENodable.Type.LEGATE || type == LENodable.Type.LEKEEPER) && outputJNet != null) {
                    System.out.println("Error: Sizable gate "+ni.getNodeInst().describe()+" has more than one output port!! Ignoring Gate");
                    return null;
                }
                outputJNet = jnet;
                lenodable.setOutputJNet(jnet);
            }
            if (type == LENodable.Type.TRANSISTOR) {
                // primitive Electric Transistors have their source and drain set to BIDIR, we
                // want them set to OUTPUT so that they count as diffusion capacitance
                if (pp.getCharacteristic() == PortProto.Characteristic.BIDIR) dir = LEPin.Dir.OUTPUT;
            }
            lenodable.addPort(pp.getName(), dir, le, jnet);
            if (DEBUG) System.out.println("    Added "+dir+" pin "+pp.getName()+", le: "+le+", JNetwork: "+jnet);
            if (type == LENodable.Type.WIRE) break;    // this is LEWIRE, only add one pin of it
        }

        return lenodable;
    }

    /**
     * Get the leX size for the given LENodable
     * @param type the lenodable type
     * @param ni the nodable
     * @param info the current info
     * @return the leX size
     */
    private float getLeX(LENodable.Type type, Nodable ni, HierarchyEnumerator.CellInfo info) {
        float leX = (float)0.0;

        Variable var = null;
        if (type == LENodable.Type.WIRE) {
            // Note that if inst is an LEWIRE, it will have no 'le' attributes.
            // we therefore assign pins to have default 'le' values of one.
            // This creates an instance which has Type LEWIRE, but has
            // boolean leGate set to false; it will not be sized
            var = ni.getVar("ATTR_L");
            float len = VarContext.objectToFloat(info.getContext().evalVar(var), 0.0f);
            var = ni.getVar("ATTR_width");
            float width = VarContext.objectToFloat(info.getContext().evalVar(var), 3.0f);
            leX = (float)(0.95f*len + 0.05f*len*(width/3.0f))*wireRatio;  // equivalent lambda of gate
            leX = leX/9.0f;                         // drive strength X=1 is 9 lambda of gate
        }
        else if (type == LENodable.Type.TRANSISTOR) {
            var = ni.getVar("ATTR_width");
            if (var == null) {
                System.out.println("Error: transistor "+ni+" has no width in Cell "+info.getCell());
                ErrorLogger.ErrorLog log = errorLogger.logError("Error: transistor "+ni+" has no width in Cell "+info.getCell(), info.getCell(), 0);
                log.addGeom(ni.getNodeInst(), true, info.getCell(), info.getContext());
                return 0;
            }
            float width = VarContext.objectToFloat(info.getContext().evalVar(var), (float)3.0);
            var = ni.getVar("ATTR_length");
            if (var == null) {
                System.out.println("Error: transistor "+ni+" has no length in Cell "+info.getCell());
                ErrorLogger.ErrorLog log = errorLogger.logError("Error: transistor "+ni+" has no length in Cell "+info.getCell(), info.getCell(), 0);
                log.addGeom(ni.getNodeInst(), true, info.getCell(), info.getContext());
                return 0;
            }
            float length = VarContext.objectToFloat(info.getContext().evalVar(var), (float)2.0);
            // not exactly correct because assumes all cap is area cap, which it isn't
            leX = (float)(width*length/2.0f);
            leX = leX/9.0f;
        }
        return leX;
    }

    // =============================== Statistics ==================================


    public void printStatistics() {
        float totalsize = 0f;
        float instsize = 0f;
        int numLEGates = 0;
        int numLEWires = 0;
        // iterator over all LEGATEs
        for (Iterator cit = allLENodables.iterator(); cit.hasNext(); ) {
            LENodable leno = (LENodable)cit.next();
            // ignore it if not a sizeable gate
            if (leno.isLeGate()) {
                numLEGates++;
                instsize += leno.leX;
            }
            if (leno.getType() == LENodable.Type.WIRE)
                numLEWires++;
            totalsize += leno.leX;
        }
        System.out.println("Number of LEGATEs: "+numLEGates);
        System.out.println("Number of Wires: "+numLEWires);
        System.out.println("Total size of all LEGATEs: "+instsize);
        System.out.println("Total size of all instances (sized and loads): "+totalsize);
    }

    public boolean printResults(Nodable no, VarContext context) {
        // if this is a NodeInst, convert to Nodable
        if (no instanceof NodeInst) {
            no = Netlist.getNodableFor((NodeInst)no, 0);
        }
        LENodable leno = null;
        for (Iterator it = allLENodables.iterator(); it.hasNext(); ) {
            LENodable aleno = (LENodable)it.next();
            if (aleno.getNodable() == no && (aleno.context == context)) {
                leno = aleno;
                break;
            }
        }
        if (leno == null) return false;

        // print netlister info
        System.out.println("Netlister: Gate Cap="+gateCap+", Alpha="+alpha);

        // print instance info
        leno.print();

        // collect info about what is driven
        LENetwork outputNet = leno.outputNetwork;

        ArrayList gatesDrivenPins = new ArrayList();
        ArrayList loadsDrivenPins = new ArrayList();
        ArrayList wiresDrivenPins = new ArrayList();
        ArrayList gatesFightingPins = new ArrayList();

        for (Iterator it = outputNet.getAllPins().iterator(); it.hasNext(); ) {
            LEPin pin = (LEPin)it.next();
            LENodable loopLeno = pin.getInstance();
            if (pin.getDir() == LEPin.Dir.INPUT) {
                if (loopLeno.isGate()) gatesDrivenPins.add(pin);
                if (loopLeno.getType() == LENodable.Type.LOAD) loadsDrivenPins.add(pin);
                if (loopLeno.getType() == LENodable.Type.TRANSISTOR) loadsDrivenPins.add(pin);
                if (loopLeno.getType() == LENodable.Type.WIRE) wiresDrivenPins.add(pin);
            }
            if (pin.getDir() == LEPin.Dir.OUTPUT) {
                if (loopLeno.isGate()) gatesFightingPins.add(pin);
            }
        }
        System.out.println("Note: Load = Size * LE * M");
        System.out.println("Note: Load = Size * LE * M * Alpha, for Gates Fighting");

        float totalLoad = 0f;
        System.out.println("  -------------------- Gates Driven ("+gatesDrivenPins.size()+") --------------------");
        for (Iterator it = gatesDrivenPins.iterator(); it.hasNext(); ) {
            LEPin pin = (LEPin)it.next(); totalLoad += pin.getInstance().printLoadInfo(pin, alpha);
        }
        System.out.println("  -------------------- Loads Driven ("+loadsDrivenPins.size()+") --------------------");
        for (Iterator it = loadsDrivenPins.iterator(); it.hasNext(); ) {
            LEPin pin = (LEPin)it.next(); totalLoad += pin.getInstance().printLoadInfo(pin, alpha);
        }
        System.out.println("  -------------------- Wires Driven ("+wiresDrivenPins.size()+") --------------------");
        for (Iterator it = wiresDrivenPins.iterator(); it.hasNext(); ) {
            LEPin pin = (LEPin)it.next(); totalLoad += pin.getInstance().printLoadInfo(pin, alpha);
        }
        System.out.println("  -------------------- Gates Fighting ("+gatesFightingPins.size()+") --------------------");
        for (Iterator it = gatesFightingPins.iterator(); it.hasNext(); ) {
            LEPin pin = (LEPin)it.next(); totalLoad += pin.getInstance().printLoadInfo(pin, alpha);
        }
        System.out.println("*** Total Load: "+TextUtils.formatDouble(totalLoad, 2));
        //msgs.setFont(oldFont);
        return true;
    }

    // ---- TEST STUFF -----  REMOVE LATER ----
    public static void test1() {
        LESizer.test1();
    }
    
}
