/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Verilog.java
 * Input/output tool: Verilog Netlist output
 * Written by Steven M. Rubin, Sun Microsystems.
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.io.output;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.network.Global;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.generator.sclibrary.SCLibraryGen;
import com.sun.electric.tool.io.input.verilog.VerilogData;
import com.sun.electric.tool.io.input.verilog.VerilogReader;
import com.sun.electric.tool.simulation.Simulation;
import com.sun.electric.tool.user.User;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

/**
 * This is the Simulation Interface tool.
 */
public class Verilog extends Topology
{
    /** A set of keywords that are reserved in Verilog */
    private static HashSet<String> reservedWords;
    static
    {
        reservedWords = new HashSet<String>();
        reservedWords.add("always");
        reservedWords.add("and");
        reservedWords.add("assign");
        reservedWords.add("attribute");
        reservedWords.add("begin");
        reservedWords.add("buf");
        reservedWords.add("bufif0");
        reservedWords.add("bufif1");
        reservedWords.add("case");
        reservedWords.add("casex");
        reservedWords.add("casez");
        reservedWords.add("cmos");
        reservedWords.add("deassign");
        reservedWords.add("default");
        reservedWords.add("defpram");
        reservedWords.add("disable");
        reservedWords.add("edge");
        reservedWords.add("else");
        reservedWords.add("end");
        reservedWords.add("endattribute");
        reservedWords.add("endcase");
        reservedWords.add("endfunction");
        reservedWords.add("endmodule");
        reservedWords.add("endprimitive");
        reservedWords.add("endspecify");
        reservedWords.add("endtable");
        reservedWords.add("endtask");
        reservedWords.add("event");
        reservedWords.add("for");
        reservedWords.add("force");
        reservedWords.add("forever");
        reservedWords.add("fork");
        reservedWords.add("function");
        reservedWords.add("highz0");
        reservedWords.add("highz1");
        reservedWords.add("if");
        reservedWords.add("initial");
        reservedWords.add("inout");
        reservedWords.add("input");
        reservedWords.add("integer");
        reservedWords.add("join");
        reservedWords.add("large");
        reservedWords.add("macromodule");
        reservedWords.add("meduim");
        reservedWords.add("module");
        reservedWords.add("nand");
        reservedWords.add("negedge");
        reservedWords.add("nmos");
        reservedWords.add("nor");
        reservedWords.add("not");
        reservedWords.add("notif0");
        reservedWords.add("notif1");
        reservedWords.add("or");
        reservedWords.add("output");
        reservedWords.add("parameter");
        reservedWords.add("pmos");
        reservedWords.add("posedge");
        reservedWords.add("primitive");
        reservedWords.add("pull0");
        reservedWords.add("pull1");
        reservedWords.add("pulldown");
        reservedWords.add("pullup");
        reservedWords.add("rcmos");
        reservedWords.add("real");
        reservedWords.add("realtime");
        reservedWords.add("reg");
        reservedWords.add("release");
        reservedWords.add("repeat");
        reservedWords.add("rtranif1");
        reservedWords.add("scalared");
        reservedWords.add("signed");
        reservedWords.add("small");
        reservedWords.add("specify");
        reservedWords.add("specpram");
        reservedWords.add("strength");
        reservedWords.add("strong0");
        reservedWords.add("strong1");
        reservedWords.add("supply0");
        reservedWords.add("supply1");
        reservedWords.add("table");
        reservedWords.add("task");
        reservedWords.add("time");
        reservedWords.add("tran");
        reservedWords.add("tranif0");
        reservedWords.add("tranif1");
        reservedWords.add("tri");
        reservedWords.add("tri0");
        reservedWords.add("tri1");
        reservedWords.add("triand");
        reservedWords.add("trior");
        reservedWords.add("trireg");
        reservedWords.add("unsigned");
        reservedWords.add("vectored");
        reservedWords.add("wait");
        reservedWords.add("wand");
        reservedWords.add("weak0");
        reservedWords.add("weak1");
        reservedWords.add("while");
        reservedWords.add("wire");
        reservedWords.add("wor");
        reservedWords.add("xnor");
        reservedWords.add("xor");
    }

    /** maximum size of output line */						private static final int MAXDECLARATIONWIDTH = 80;
    /** name of inverters generated from negated wires */	private static final String IMPLICITINVERTERNODENAME = "Imp";
    /** name of signals generated from negated wires */		private static final String IMPLICITINVERTERSIGNAME = "ImpInv";

    /** key of Variable holding verilog code. */			public static final Variable.Key VERILOG_CODE_KEY = Variable.newKey("VERILOG_code");
    /** key of Variable holding verilog declarations. */	public static final Variable.Key VERILOG_DECLARATION_KEY = Variable.newKey("VERILOG_declaration");
    /** key of Variable holding verilog wire time. */		public static final Variable.Key WIRE_TYPE_KEY = Variable.newKey("SIM_verilog_wire_type");
    /** key of Variable holding verilog templates. */		public static final Variable.Key VERILOG_TEMPLATE_KEY = Variable.newKey("ATTR_verilog_template");
    /** key of Variable holding file name with Verilog. */	public static final Variable.Key VERILOG_BEHAVE_FILE_KEY = Variable.newKey("SIM_verilog_behave_file");
    /** those cells that have overridden models */	        private HashSet<Cell> modelOverrides = new HashSet<Cell>();
    /** those cells that have modules defined */            private HashMap<String,String> definedModules = new HashMap<String,String>(); // key: module name, value: source description
    /** those cells that have primitives defined */         private HashMap<Cell,VerilogData.VerilogModule> definedPrimitives = new HashMap<Cell,VerilogData.VerilogModule>(); // key: module name, value: VerilogModule
    /** map of cells that are or contain standard cells */  private SCLibraryGen.StandardCellHierarchy standardCells = new SCLibraryGen.StandardCellHierarchy();

    /**
     * The main entry point for Verilog deck writing.
     * @param cell the top-level cell to write.
     * @param context the hierarchical context to the cell.
     * @param filePath the disk file to create.
     */
    public static void writeVerilogFile(Cell cell, VarContext context, String filePath)
    {
        Verilog out = new Verilog();
        if (out.openTextOutputStream(filePath)) return;
        if (out.writeCell(cell, context)) return;
        if (out.closeTextOutputStream()) return;
        System.out.println(filePath + " written");
    }

    /**
     * Creates a new instance of Verilog
     */
    Verilog()
    {
    }

    protected void start()
    {
        // parameters to the output-line-length limit and how to break long lines
        setOutputWidth(MAXDECLARATIONWIDTH, false);
        setContinuationString("      ");

        // write header information
        printWriter.println("/* Verilog for " + topCell + " from " + topCell.getLibrary() + " */");
        emitCopyright("/* ", " */");
        if (User.isIncludeDateAndVersionInOutput())
        {
            printWriter.println("/* Created on " + TextUtils.formatDate(topCell.getCreationDate()) + " */");
            printWriter.println("/* Last revised on " + TextUtils.formatDate(topCell.getRevisionDate()) + " */");
            printWriter.println("/* Written on " + TextUtils.formatDate(new Date()) +
                " by Electric VLSI Design System, version " + Version.getVersion() + " */");
        } else
        {
            printWriter.println("/* Written by Electric VLSI Design System */");
        }

        // gather all global signal names
/*
		Netlist netList = getNetlistForCell(topCell);
		Global.Set globals = netList.getGlobals();
        int globalSize = globals.size();

        // see if any globals besides power and ground to write
        ArrayList globalsToWrite = new ArrayList();
        for (int i=0; i<globalSize; i++) {
            Global global = (Global)globals.get(i);
            if (global == Global.power || global == Global.ground) continue;
            globalsToWrite.add(global);
        }

		if (globalsToWrite.size() > 0)
		{
			printWriter.println("\nmodule glbl();");
			for(int i=0; i<globalsToWrite.size(); i++)
			{
				Global global = (Global)globalsToWrite.get(i);
				if (Simulation.getVerilogUseTrireg())
				{
					printWriter.println("    trireg " + global.getName() + ";");
				} else
				{
					printWriter.println("    wire " + global.getName() + ";");
				}
			}
			printWriter.println("endmodule");
		}
*/
        if (Simulation.getVerilogStopAtStandardCells()) {
            // enumerate to find which cells contain standard cells
            HierarchyEnumerator.enumerateCell(topCell, VarContext.globalContext, standardCells);
            for (Cell acell : standardCells.getDoesNotContainStandardCellsInHier()) {
                System.out.println("Warning: Not netlisting cell "+acell.describe(false)+" because it does not contain any standard cells.");
            }
        }
    }

    protected void done()
    {
        Simulation.setVerilogStopAtStandardCells(false);
    }

    protected boolean skipCellAndSubcells(Cell cell) {
        // do not write modules for cells with verilog_templates defined
        // also skip their subcells
        if (cell.getVar(VERILOG_TEMPLATE_KEY) != null) {
            return true;
        }
        // also skip subcells if a behavioral file specified.
        // If one specified, write it out here and skip both cell and subcells
        if (CellModelPrefs.verilogModelPrefs.isUseModelFromFile(cell)) {
            String fileName = CellModelPrefs.verilogModelPrefs.getModelFile(cell);
            // check that data from file is consistent
            VerilogReader reader = new VerilogReader();
            VerilogData data = reader.parseVerilog(fileName, true);
            if (data == null) {
                System.out.println("Error reading include file: "+fileName);
                return false;
            }
            if (!checkIncludedData(data, cell, fileName))
                return false;

            if (!modelOverrides.contains(cell))
            {
                printWriter.println("`include \"" + fileName + "\"");
                modelOverrides.add(cell);
            }
            return true;
        }
/*
        Variable behaveFileVar = cell.getVar(VERILOG_BEHAVE_FILE_KEY);
        if (behaveFileVar != null)
        {
            String fileName = behaveFileVar.getObject().toString();
            if (fileName.length() > 0)
            {
                printWriter.println("`include \"" + fileName + "\"");
                return true;
            }
        }
*/

        // use library behavior if it is available
        Cell verViewCell = cell.otherView(View.VERILOG);
        if (verViewCell != null)
        {
            String [] stringArray = verViewCell.getTextViewContents();
            if (stringArray != null)
            {
                if (stringArray.length > 0) {
                    String line = stringArray[0].toLowerCase();
                    if (line.startsWith("do not use")) {
                        return false;
                    }
                }
                VerilogReader reader = new VerilogReader();
                VerilogData data = reader.parseVerilog(stringArray, cell.getLibrary().getName());
                if (data == null) {
                    System.out.println("Error parsing Verilog View for cell "+cell.describe(false));
                    return false;
                }
                if (!checkIncludedData(data, cell, null))
                    return false;

                // write to output file
                System.out.println("Info: Netlisting Verilog view of "+cell.describe(false));
                printWriter.println();
                printWriter.println("/* Verilog view of "+verViewCell.libDescribe()+" */");
                for(int i=0; i<stringArray.length; i++)
                    printWriter.println(stringArray[i]);
            }
            return true;
        }

        if (Simulation.getVerilogStopAtStandardCells()) {
            // do not netlist contents of standard cells
            if (!standardCells.containsStandardCell(cell)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Method to write cellGeom
     */
    protected void writeCellTopology(Cell cell, CellNetInfo cni, VarContext context, Topology.MyCellInfo info)
    {
        if (cell == topCell) {
            // gather all global signal names
            Netlist netList = cni.getNetList();
            Global.Set globals = netList.getGlobals();
            int globalSize = globals.size();

            // see if any globals besides power and ground to write
            ArrayList<Global> globalsToWrite = new ArrayList<Global>();
            for (int i=0; i<globalSize; i++) {
                Global global = globals.get(i);
                if (global == Global.power || global == Global.ground) continue;
                globalsToWrite.add(global);
            }

            if (globalsToWrite.size() > 0)
            {
                printWriter.println("\nmodule glbl();");
                for(int i=0; i<globalsToWrite.size(); i++)
                {
                    Global global = globalsToWrite.get(i);
                    if (Simulation.getVerilogUseTrireg())
                    {
                        printWriter.println("    trireg " + global.getName() + ";");
                    } else
                    {
                        printWriter.println("    wire " + global.getName() + ";");
                    }
                }
                printWriter.println("endmodule");
            }
        }

        // if verilog template specified, don't need to write out this cell
/*        Variable varT = cell.getVar(VERILOG_TEMPLATE_KEY);
        if (varT != null) {
            return;
        } */

        // prepare arcs to store implicit inverters
        Map<ArcInst,Integer> implicitHeadInverters = new HashMap<ArcInst,Integer>();
        Map<ArcInst,Integer> implicitTailInverters = new HashMap<ArcInst,Integer>();
        int impInvCount = 0;
        for(Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
        {
            ArcInst ai = it.next();
            for(int e=0; e<2; e++)
            {
                if (!ai.isNegated(e)) continue;
                PortInst pi = ai.getPortInst(e);
                NodeInst ni = pi.getNodeInst();
                if (ni.getProto() == Schematics.tech().bufferNode || ni.getProto() == Schematics.tech().andNode ||
                    ni.getProto() == Schematics.tech().orNode || ni.getProto() == Schematics.tech().xorNode)
                {
                    if (Simulation.getVerilogUseAssign()) continue;
                    if (pi.getPortProto().getName().equals("y")) continue;
                }

                // must create implicit inverter here
                if (e == ArcInst.HEADEND) implicitHeadInverters.put(ai, new Integer(impInvCount)); else
                	implicitTailInverters.put(ai, new Integer(impInvCount));
                if (ai.getProto() != Schematics.tech().bus_arc) impInvCount++; else
                {
                    int wid = cni.getNetList().getBusWidth(ai);
                    impInvCount += wid;
                }
            }
        }

        // gather networks in the cell
        Netlist netList = cni.getNetList();

        // write the module header
        printWriter.println();
        StringBuffer sb = new StringBuffer();
        sb.append("module " + cni.getParameterizedName() + "(");
        boolean first = true;
        for(Iterator<CellAggregateSignal> it = cni.getCellAggregateSignals(); it.hasNext(); )
        {
            CellAggregateSignal cas = it.next();
            if (cas.getExport() == null) continue;
            if (!first) sb.append(", ");
            sb.append(cas.getName());
            first = false;
        }
        sb.append(");\n");
        writeWidthLimited(sb.toString());
        definedModules.put(cni.getParameterizedName(), "Cell "+cell.libDescribe());

        // look for "wire/trireg" overrides
        for(Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
        {
            ArcInst ai = it.next();
            Variable var = ai.getVar(WIRE_TYPE_KEY);
            if (var == null) continue;
            String wireType = var.getObject().toString();
            int overrideValue = 0;
            if (wireType.equalsIgnoreCase("wire")) overrideValue = 1; else
                if (wireType.equalsIgnoreCase("trireg")) overrideValue = 2;
            int busWidth = netList.getBusWidth(ai);
            for(int i=0; i<busWidth; i++)
            {
                Network net = netList.getNetwork(ai, i);
                CellSignal cs = cni.getCellSignal(net);
                if (cs == null) continue;
                cs.getAggregateSignal().setFlags(overrideValue);
            }
        }

        // write description of formal parameters to module
        first = true;
        for(Iterator<CellAggregateSignal> it = cni.getCellAggregateSignals(); it.hasNext(); )
        {
            CellAggregateSignal cas = it.next();
            Export pp = cas.getExport();
            if (pp == null) continue;

            String portType = "input";
            if (pp.getCharacteristic() == PortCharacteristic.OUT)
                portType = "output";
            else if (pp.getCharacteristic() == PortCharacteristic.BIDIR)
                portType = "inout";

            sb = new StringBuffer();
            sb.append("  " + portType);
            if (cas.getLowIndex() > cas.getHighIndex())
            {
            	sb.append(" " + cas.getName() + ";");
            } else
            {
            	int [] indices = cas.getIndices();
            	if (indices != null)
            	{
            		for(int i=0; i<indices.length; i++)
            		{
            			int ind = i;
            			if (cas.isDescending()) ind = indices.length - i - 1;
            			if (i != 0) sb.append(",");
            			sb.append(" \\" + cas.getName() + "[" + indices[ind] + "] ");
            		}
            		sb.append(";");
            	} else
            	{
	                int low = cas.getLowIndex(), high = cas.getHighIndex();
	                if (cas.isDescending())
	                {
	                    low = cas.getHighIndex();   high = cas.getLowIndex();
	                }
	                sb.append(" [" + low + ":" + high + "] " + cas.getName() + ";");
            	}
            }
            if (cas.getFlags() != 0)
            {
                if (cas.getFlags() == 1) sb.append("  wire"); else
                	sb.append("  trireg");
                sb.append(" " + cas.getName() + ";");
            }
            sb.append("\n");
            writeWidthLimited(sb.toString());
            first = false;
        }
        if (!first) printWriter.println();

        // describe power and ground nets
        if (cni.getPowerNet() != null) printWriter.println("  supply1 vdd;");
        if (cni.getGroundNet() != null) printWriter.println("  supply0 gnd;");

        // determine whether to use "wire" or "trireg" for networks
        String wireType = "wire";
        if (Simulation.getVerilogUseTrireg()) wireType = "trireg";

        // write "wire/trireg" declarations for internal single-wide signals
        int localWires = 0;
        for(int wt=0; wt<2; wt++)
        {
            first = true;
            for(Iterator<CellAggregateSignal> it = cni.getCellAggregateSignals(); it.hasNext(); )
            {
                CellAggregateSignal cas = it.next();
                if (cas.getExport() != null) continue;
                if (cas.isSupply()) continue;
                if (cas.getLowIndex() <= cas.getHighIndex()) continue;
                if (cas.isGlobal()) continue;

                String impSigName = wireType;
                if (cas.getFlags() != 0)
                {
                    if (cas.getFlags() == 1) impSigName = "wire"; else
                        impSigName = "trireg";
                }
                if ((wt == 0) ^ !wireType.equals(impSigName))
                {
                    if (first)
                    {
                        initDeclaration("  " + impSigName);
                    }
                    addDeclaration(cas.getName());
                    localWires++;
                    first = false;
                }
            }
            if (!first) termDeclaration();
        }

        // write "wire/trireg" declarations for internal busses
        for(Iterator<CellAggregateSignal> it = cni.getCellAggregateSignals(); it.hasNext(); )
        {
            CellAggregateSignal cas = it.next();
            if (cas.getExport() != null) continue;
            if (cas.isSupply()) continue;
            if (cas.getLowIndex() > cas.getHighIndex()) continue;
            if (cas.isGlobal()) continue;

        	int [] indices = cas.getIndices();
        	if (indices != null)
        	{
        		for(int i=0; i<indices.length; i++)
        		{
        			int ind = i;
        			if (cas.isDescending()) ind = indices.length - i - 1;
        			printWriter.println("  " + wireType + " \\" + cas.getName() + "[" + indices[ind] + "] ;");
        		}
        	} else
        	{
	            if (cas.isDescending())
	            {
	                printWriter.println("  " + wireType + " [" + cas.getHighIndex() + ":" + cas.getLowIndex() + "] " + cas.getName() + ";");
	            } else
	            {
	                printWriter.println("  " + wireType + " [" + cas.getLowIndex() + ":" + cas.getHighIndex() + "] " + cas.getName() + ";");
	            }
        	}
            localWires++;
        }
        if (localWires != 0) printWriter.println();

        // add "wire" declarations for implicit inverters
        if (impInvCount > 0)
        {
            initDeclaration("  " + wireType);
            for(int i=0; i<impInvCount; i++)
            {
                String impsigname = IMPLICITINVERTERSIGNAME + i;
                addDeclaration(impsigname);
            }
            termDeclaration();
        }

        // add in any user-specified declarations and code
        first = includeTypedCode(cell, VERILOG_DECLARATION_KEY, "declarations");
        first |= includeTypedCode(cell, VERILOG_CODE_KEY, "code");
        if (!first)
            printWriter.println("  /* automatically generated Verilog */");

        // look at every node in this cell
        for(Iterator<Nodable> nIt = netList.getNodables(); nIt.hasNext(); )
        {
            Nodable no = nIt.next();
            NodeProto niProto = no.getProto();

            // not interested in passive nodes (ports electrically connected)
            PrimitiveNode.Function nodeType = PrimitiveNode.Function.UNKNOWN;
            if (!no.isCellInstance())
            {
                NodeInst ni = (NodeInst)no;
                Iterator<PortInst> pIt = ni.getPortInsts();
                if (pIt.hasNext())
                {
                    boolean allConnected = true;
                    PortInst firstPi = pIt.next();
                    Network firstNet = netList.getNetwork(firstPi);
                    for( ; pIt.hasNext(); )
                    {
                        PortInst pi = pIt.next();
                        Network thisNet = netList.getNetwork(pi);
                        if (thisNet != firstNet) { allConnected = false;   break; }
                    }
                    if (allConnected) continue;
                }
                nodeType = ni.getFunction();

                // special case: verilog should ignore R L C etc.
                if (nodeType.isResistor() || // == PrimitiveNode.Function.RESIST ||
                    nodeType.isCapacitor() ||  // == PrimitiveNode.Function.CAPAC || nodeType == PrimitiveNode.Function.ECAPAC ||
                    nodeType == PrimitiveNode.Function.INDUCT ||
                    nodeType == PrimitiveNode.Function.DIODE || nodeType == PrimitiveNode.Function.DIODEZ)
                        continue;
            }

            // look for a Verilog template on the prototype
            if (no.isCellInstance())
            {
                Variable varTemplate = ((Cell)niProto).getVar(VERILOG_TEMPLATE_KEY);
                if (varTemplate != null)
                {
                    if (varTemplate.getObject() instanceof String []) {
                        String [] lines = (String [])varTemplate.getObject();
                        writeWidthLimited("  /* begin Verilog_template for "+no.getProto().describe(false)+"*/\n");
                        for (int i=0; i<lines.length; i++) {
                            writeTemplate(lines[i], no, cni, context);
                        }
                        writeWidthLimited("  // end Verilog_template\n");
                    } else {
                        // special case: do not write out string "//"
                        if (!((String)varTemplate.getObject()).equals("//")) {
                            writeWidthLimited("  /* begin Verilog_template for "+no.getProto().describe(false)+"*/\n");
                            writeTemplate((String)varTemplate.getObject(), no, cni, context);
                            writeWidthLimited("  // end Verilog_template\n");
                        }
                    }
                    continue;
                }

                // if writing standard cell netlist, do not write out cells that
                // do not contain standard cells
                if (Simulation.getVerilogStopAtStandardCells()) {
                    if (!standardCells.containsStandardCell((Cell)niProto) &&
                            !SCLibraryGen.isStandardCell((Cell)niProto)) {
                    //if (!SCLibraryGen.isStandardCell((Cell)niProto)) {
                        continue;
                    }
                }
            }

            // use "assign" statement if requested
            if (Simulation.getVerilogUseAssign())
            {
                if (nodeType == PrimitiveNode.Function.GATEAND || nodeType == PrimitiveNode.Function.GATEOR ||
                    nodeType == PrimitiveNode.Function.GATEXOR || nodeType == PrimitiveNode.Function.BUFFER)
                {
                    // assign possible: determine operator
                    String op = "";
                    if (nodeType == PrimitiveNode.Function.GATEAND) op = " & "; else
                    if (nodeType == PrimitiveNode.Function.GATEOR) op = " | "; else
                    if (nodeType == PrimitiveNode.Function.GATEXOR) op = " ^ ";

                    // write a line describing this signal
                    StringBuffer infstr = new StringBuffer();
                    boolean wholeNegated = false;
                    first = true;
                    NodeInst ni = (NodeInst)no;
                    for(int i=0; i<2; i++)
                    {
                        for(Iterator<Connection> cIt = ni.getConnections(); cIt.hasNext(); )
                        {
                            Connection con = cIt.next();
                            PortInst pi = con.getPortInst();
                            if (i == 0)
                            {
                                if (!pi.getPortProto().getName().equals("y")) continue;
                            } else
                            {
                                if (!pi.getPortProto().getName().equals("a")) continue;
                            }

                            // determine the network name at this port
                            ArcInst ai = con.getArc();
                            Network net = netList.getNetwork(ai, 0);
                            CellSignal cs = cni.getCellSignal(net);
                            String sigName = getSignalName(cs);

                            // see if this end is negated
                            boolean isNegated = false;
                            if (ai.isTailNegated() || ai.isHeadNegated()) isNegated = true;

                            // write the port name
                            if (i == 0)
                            {
                                // got the output port: do the left-side of the "assign"
                                infstr.append("assign " + sigName + " = ");
                                if (isNegated)
                                {
                                    infstr.append("~(");
                                    wholeNegated = true;
                                }
                                break;
                            }

                            if (!first)
                                infstr.append(op);
                            first = false;
                            if (isNegated) infstr.append("~");
                            infstr.append(sigName);
                        }
                    }
                    if (wholeNegated)
                        infstr.append(")");
                    infstr.append(";\n");
                    writeWidthLimited(infstr.toString());
                    continue;
                }
            }

            // get the name of the node
            int implicitPorts = 0;
            boolean dropBias = false;
            String nodeName = "";
            if (no.isCellInstance())
            {
                // make sure there are contents for this cell instance
                if (((Cell)niProto).getView() == View.ICON) continue;

                nodeName = parameterizedName(no, context);
                // cells defined as "primitives" in Verilog View must have implicit port ordering
                if (definedPrimitives.containsKey(niProto)) {
                    implicitPorts = 3;
                }
            } else
            {
                // convert 4-port transistors to 3-port
                if (nodeType == PrimitiveNode.Function.TRA4NMOS)
                {
                    nodeType = PrimitiveNode.Function.TRANMOS;  dropBias = true;
                } else if (nodeType == PrimitiveNode.Function.TRA4PMOS)
                {
                    nodeType = PrimitiveNode.Function.TRAPMOS;  dropBias = true;
                }

                if (nodeType == PrimitiveNode.Function.TRANMOS)
                {
                    implicitPorts = 2;
                    nodeName = "tranif1";
                    Variable varWeakNode = ((NodeInst)no).getVar(Simulation.WEAK_NODE_KEY);
                    if (varWeakNode != null) nodeName = "rtranif1";
                } else if (nodeType == PrimitiveNode.Function.TRAPMOS)
                {
                    implicitPorts = 2;
                    nodeName = "tranif0";
                    Variable varWeakNode = ((NodeInst)no).getVar(Simulation.WEAK_NODE_KEY);
                    if (varWeakNode != null) nodeName = "rtranif0";
                } else if (nodeType == PrimitiveNode.Function.GATEAND)
                {
                    implicitPorts = 1;
                    nodeName = chooseNodeName((NodeInst)no, "and", "nand");
                } else if (nodeType == PrimitiveNode.Function.GATEOR)
                {
                    implicitPorts = 1;
                    nodeName = chooseNodeName((NodeInst)no, "or", "nor");
                } else if (nodeType == PrimitiveNode.Function.GATEXOR)
                {
                    implicitPorts = 1;
                    nodeName = chooseNodeName((NodeInst)no, "xor", "xnor");
                } else if (nodeType == PrimitiveNode.Function.BUFFER)
                {
                    implicitPorts = 1;
                    nodeName = chooseNodeName((NodeInst)no, "buf", "not");
                }
            }
            if (nodeName.length() == 0) continue;

            // write the type of the node
            StringBuffer infstr = new StringBuffer();
            infstr.append("  " + nodeName + " " + nameNoIndices(no.getName()) + "(");

            // write the rest of the ports
            first = true;
            NodeInst ni = null;
            switch (implicitPorts)
            {
                case 0:		// explicit ports (for cell instances)
                    CellNetInfo subCni = getCellNetInfo(nodeName);
                    for(Iterator<CellAggregateSignal> sIt = subCni.getCellAggregateSignals(); sIt.hasNext(); )
                    {
                        CellAggregateSignal cas = sIt.next();

                        // ignore networks that aren't exported
                        PortProto pp = cas.getExport();
                        if (pp == null) continue;

                        if (first) first = false; else
                            infstr.append(", ");
                        if (cas.getLowIndex() > cas.getHighIndex())
                        {
                            // single signal
                            infstr.append("." + cas.getName() + "(");
                            Network net = netList.getNetwork(no, pp, cas.getExportIndex());
                            CellSignal cs = cni.getCellSignal(net);
                           	infstr.append(getSignalName(cs));
                            infstr.append(")");
                        } else
                        {
                            int total = cas.getNumSignals();
                            CellSignal [] outerSignalList = new CellSignal[total];
                            for(int j=0; j<total; j++)
                            {
                                CellSignal cInnerSig = cas.getSignal(j);
                                Network net = netList.getNetwork(no, cas.getExport(), cInnerSig.getExportIndex());
                                outerSignalList[j] = cni.getCellSignal(net);
                            }
                            writeBus(outerSignalList, total, cas.isDescending(),
                                cas.getName(), cni.getPowerNet(), cni.getGroundNet(), infstr);
                        }
                    }
                    infstr.append(");");
                    break;

                case 1:		// and/or gate: write ports in the proper order
                    ni = (NodeInst)no;
                    for(int i=0; i<2; i++)
                    {
                        for(Iterator<Connection> cIt = ni.getConnections(); cIt.hasNext(); )
                        {
                            Connection con = cIt.next();
                            PortInst pi = con.getPortInst();
                            if (i == 0)
                            {
                                if (!pi.getPortProto().getName().equals("y")) continue;
                            } else
                            {
                                if (!pi.getPortProto().getName().equals("a")) continue;
                            }
                            if (first) first = false; else
                                infstr.append(", ");
                            ArcInst ai = con.getArc();
                            Network net = netList.getNetwork(ai, 0);
                            CellSignal cs = cni.getCellSignal(net);
                            if (cs == null) continue;
                            String sigName = getSignalName(cs);
                            if (i != 0 && con.isNegated())
                            {
                                // this input is negated: write the implicit inverter
                                Integer invIndex;
                                if (con.getEndIndex() == ArcInst.HEADEND) invIndex = implicitHeadInverters.get(ai); else
                                	invIndex = implicitTailInverters.get(ai);
                                if (invIndex != null)
                                {
                                    String invSigName = IMPLICITINVERTERSIGNAME + invIndex.intValue();
                                    printWriter.println("  inv " + IMPLICITINVERTERNODENAME +
                                        invIndex.intValue() + " (" + invSigName + ", " + sigName + ");");
                                    sigName = invSigName;
                                }
                            }
                            infstr.append(sigName);
                        }
                    }
                    infstr.append(");");
                    break;

                case 2:		// transistors: write ports in the proper order: s/d/g
                    ni = (NodeInst)no;
                    Network gateNet = netList.getNetwork(ni.getTransistorGatePort());
                    for(int i=0; i<2; i++)
                    {
                    	boolean didGate = false;
                        for(Iterator<PortInst> pIt = ni.getPortInsts(); pIt.hasNext(); )
                        {
                            PortInst pi = pIt.next();
                            Network net = netList.getNetwork(pi);
                            if (dropBias && pi.getPortProto().getName().equals("b")) continue;
                            if (i == 0)
                            {
                                if (net == gateNet) continue;
                            } else
                            {
                                if (net != gateNet) continue;
                                if (didGate) continue;
                                didGate = true;
                            }
                            if (first) first = false; else
                                infstr.append(", ");

                            CellSignal cs = cni.getCellSignal(net);
                            String sigName = getSignalName(cs);
                            infstr.append(sigName);
                        }
                    }
                    infstr.append(");");
                    break;
                case 3:         // implicit ports ordering for cells defined as primitives in Verilog View
                    ni = no.getNodeInst();
                    VerilogData.VerilogModule module = definedPrimitives.get(niProto);
                    if (module == null) break;
                    System.out.print(cell.getName()+" ports: ");
                    for (VerilogData.VerilogPort port : module.getPorts()) {
                        List<String> portnames = port.getPinNames(true);
                        if (portnames.size() == 0) {
                            // continue
                        } else if (portnames.size() > 1) {
                            // Bill thinks bussed ports are not allowed for primitives
                            System.out.println("Error: bussed ports not allowed on Verilog primitives: "+niProto.getName());
                        } else {
                            String portname = portnames.get(0);
                            PortInst pi = ni.findPortInst(portname);
                            Network net = netList.getNetwork(no, pi.getProtoEquivalent(), 0);
                            CellSignal cs = cni.getCellSignal(net);
                            String sigName = getSignalName(cs);

                            if (first) first = false; else
                                infstr.append(", ");
                            infstr.append(sigName);
                            System.out.print(portname+" ");
                        }
                    }
                    System.out.println();
                    infstr.append(");");
                    break;
            }
            infstr.append("\n");
            writeWidthLimited(infstr.toString());
        }
        printWriter.println("endmodule   /* " + cni.getParameterizedName() + " */");
    }

    private String getSignalName(CellSignal cs)
    {
    	CellAggregateSignal cas = cs.getAggregateSignal();
    	if (cas == null) return cs.getName();
    	if (cas.getIndices() == null) return cs.getName();
    	return " \\" + cs.getName() + " ";
    	
    }

    private String chooseNodeName(NodeInst ni, String positive, String negative)
    {
        for(Iterator<Connection> aIt = ni.getConnections(); aIt.hasNext(); )
        {
            Connection con = aIt.next();
            if (con.isNegated() &&
                con.getPortInst().getPortProto().getName().equals("y"))
                    return negative;
        }
        return positive;
    }

    private void writeTemplate(String line, Nodable no, CellNetInfo cni, VarContext context)
    {
        // special case for Verilog templates
        Netlist netList = cni.getNetList();
        StringBuffer infstr = new StringBuffer();
        infstr.append("  ");
        for(int pt = 0; pt < line.length(); pt++)
        {
            char chr = line.charAt(pt);
            if (chr != '$' || pt+1 >= line.length() || line.charAt(pt+1) != '(')
            {
                // process normal character
                infstr.append(chr);
                continue;
            }

            int startpt = pt + 2;
            for(pt = startpt; pt < line.length(); pt++)
                if (line.charAt(pt) == ')') break;
            String paramName = line.substring(startpt, pt);
            PortProto pp = no.getProto().findPortProto(paramName);
            String nodeName = parameterizedName(no, context);
            CellNetInfo subCni = getCellNetInfo(nodeName);

            // see if aggregate signal matches pp
            CellAggregateSignal cas = null;
            CellSignal netcs = null;

            if (pp != null) {
                // it matches the whole port (may be a bussed port)
                for (Iterator<CellAggregateSignal> it = subCni.getCellAggregateSignals(); it.hasNext(); ) {
                    CellAggregateSignal cas2 = it.next();
                    if (cas2.getExport() == pp) {
                        cas = cas2;
                        break;
                    }
                    if (netList.sameNetwork(no, pp, no, cas2.getExport())) {
                        // this will be true if there are two exports connected together
                        // in the subcell, and we are searching for the export name that is not used.
                        cas = cas2;
                        break;
                    }
                }
            } else {
                // maybe it is a single bit in an bussed export
                for (Iterator<PortProto> it = no.getProto().getPorts(); it.hasNext(); ) {
                    PortProto ppcheck = it.next();
                    for (int i=0; i<ppcheck.getNameKey().busWidth(); i++) {
                        if (paramName.equals(ppcheck.getNameKey().subname(i).toString())) {
                            Network net = netList.getNetwork(no, ppcheck, i);
                            netcs = cni.getCellSignal(net);
                            break;
                        }
                    }
                }
            }

            if (cas != null) {
                // this code is copied from instantiated
                if (cas.getLowIndex() > cas.getHighIndex())
                {
                    // single signal
                    Network net = netList.getNetwork(no, pp, cas.getExportIndex());
                    CellSignal cs = cni.getCellSignal(net);
                    infstr.append(getSignalName(cs));
                } else
                {
                    int total = cas.getNumSignals();
                    CellSignal [] outerSignalList = new CellSignal[total];
                    for(int j=0; j<total; j++)
                    {
                        CellSignal cInnerSig = cas.getSignal(j);
                        Network net = netList.getNetwork(no, cas.getExport(), cInnerSig.getExportIndex());
                        outerSignalList[j] = cni.getCellSignal(net);
                    }
                    writeBus(outerSignalList, total, cas.isDescending(),
                        null, cni.getPowerNet(), cni.getGroundNet(), infstr);
                }
            } else if (netcs != null) {
                infstr.append(getSignalName(netcs));
            } else if (paramName.equalsIgnoreCase("node_name"))
            {
                infstr.append(getSafeNetName(no.getName(), true));
            } else
            {
                // no port name found, look for variable name
                Variable var = null;
                Variable.Key varKey = Variable.findKey("ATTR_" + paramName);
                if (varKey != null) {
                    var = no.getVar(varKey);
                    if (var == null) var = no.getParameter(varKey);
                }
                if (var == null) infstr.append("??"); else
                {
                    infstr.append(context.evalVar(var));
                }
            }
        }
        infstr.append("\n");
        writeWidthLimited(infstr.toString());
    }

    /**
     * Method to add a bus of signals named "name" to the infinite string "infstr".  If "name" is zero,
     * do not include the ".NAME()" wrapper.  The signals are in "outerSignalList" and range in index from
     * "lowindex" to "highindex".  They are described by a bus with characteristic "tempval"
     * (low bit is on if the bus descends).  Any unconnected networks can be numbered starting at
     * "*unconnectednet".  The power and grounds nets are "pwrnet" and "gndnet".
     */
    private void writeBus(CellSignal [] outerSignalList, int total, boolean descending,
                          String name, Network pwrNet, Network gndNet, StringBuffer infstr)
    {
        // array signal: see if it gets split out
        boolean breakBus = false;

        // bus cannot have pwr/gnd, must be connected
        int numExported = 0, numInternal = 0;
        for(int j=0; j<total; j++)
        {
            CellSignal cs = outerSignalList[j];
            CellAggregateSignal cas = cs.getAggregateSignal();
            if (cas != null && cas.getIndices() != null) { breakBus = true;   break; }
            if (cs.isPower() || cs.isGround()) { breakBus = true;   break; }
            if (cs.isExported()) numExported++; else
                numInternal++;
        }

        // must be all exported or all internal, not a mix
        if (numExported > 0 && numInternal > 0) breakBus = true;

        if (!breakBus)
        {
            // see if all of the nets on this bus are distinct
            int j = 1;
            for( ; j<total; j++)
            {
                CellSignal cs = outerSignalList[j];
                int k = 0;
                for( ; k<j; k++)
                {
                    CellSignal oCs = outerSignalList[k];
                    if (cs == oCs) break;
                }
                if (k < j) break;
            }
            if (j < total) breakBus = true; else
            {
                // bus entries must have the same root name and go in order
                String lastnetname = null;
                for(j=0; j<total; j++)
                {
                    CellSignal wl = outerSignalList[j];
                    String thisnetname = getSignalName(wl);
                    if (wl.isDescending())
                    {
                        if (!descending) break;
                    } else
                    {
                        if (descending) break;
                    }

                    int openSquare = thisnetname.indexOf('[');
                    if (openSquare < 0) break;
                    if (j > 0)
                    {
                        int li = 0;
                        for( ; li < lastnetname.length(); li++)
                        {
                            if (thisnetname.charAt(li) != lastnetname.charAt(li)) break;
                            if (lastnetname.charAt(li) == '[') break;
                        }
                        if (lastnetname.charAt(li) != '[' || thisnetname.charAt(li) != '[') break;
                        int thisIndex = TextUtils.atoi(thisnetname.substring(li+1));
                        int lastIndex = TextUtils.atoi(lastnetname.substring(li+1));
                        if (thisIndex != lastIndex + 1) break;
                    }
                    lastnetname = thisnetname;
                }
                if (j < total) breakBus = true;
            }
        }

        if (name != null) infstr.append("." + name + "(");
        if (breakBus)
        {
            infstr.append("{");
            int start = 0, end = total-1;
            int order = 1;
            if (descending)
            {
                start = total-1;
                end = 0;
                order = -1;
            }
            for(int j=start; ; j += order)
            {
                if (j != start)
                    infstr.append(", ");
                CellSignal cs = outerSignalList[j];
                infstr.append(getSignalName(cs));
                if (j == end) break;
            }
            infstr.append("}");
        } else
        {
            CellSignal lastCs = outerSignalList[0];
            String lastNetName = getSignalName(lastCs);
            int openSquare = lastNetName.indexOf('[');
            CellSignal cs = outerSignalList[total-1];
            String netName = getSignalName(cs);
            int i = 0;
            for( ; i<netName.length(); i++)
            {
                if (netName.charAt(i) == '[') break;
                infstr.append(netName.charAt(i));
            }
            if (descending)
            {
                int first = TextUtils.atoi(netName.substring(i+1));
                int second = TextUtils.atoi(lastNetName.substring(openSquare+1));
                infstr.append("[" + first + ":" + second + "]");
            } else
            {
                int first = TextUtils.atoi(netName.substring(i+1));
                int second = TextUtils.atoi(lastNetName.substring(openSquare+1));
                infstr.append("[" + second + ":" + first + "]");
            }
        }
        if (name != null) infstr.append(")");
    }

    /**
     * Method to add text from all nodes in cell "np"
     * (which have "verilogkey" text on them)
     * to that text to the output file.  Returns true if anything
     * was found.
     */
    private boolean includeTypedCode(Cell cell, Variable.Key verilogkey, String descript)
    {
        // write out any directly-typed Verilog code
        boolean first = true;
        for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
        {
            NodeInst ni = it.next();
            if (ni.getProto() != Generic.tech().invisiblePinNode) continue;
            Variable var = ni.getVar(verilogkey);
            if (var == null) continue;
            if (!var.isDisplay()) continue;
            Object obj = var.getObject();
            if (!(obj instanceof String) && !(obj instanceof String [])) continue;
            if (first)
            {
                first = false;
                printWriter.println("  /* user-specified Verilog " + descript + " */");
            }
            if (obj instanceof String)
            {
                printWriter.println("  " + (String)obj);
            } else
            {
                String [] stringArray = (String [])obj;
                int len = stringArray.length;
                for(int i=0; i<len; i++)
                    printWriter.println("  " + stringArray[i]);
            }
        }
        if (!first) printWriter.println();
        return first;
    }

    private StringBuffer sim_verDeclarationLine;
    private int sim_verdeclarationprefix;

    /**
     * Method to initialize the collection of signal names in a declaration.
     * The declaration starts with the string "header".
     */
    private void initDeclaration(String header)
    {
        sim_verDeclarationLine = new StringBuffer();
        sim_verDeclarationLine.append(header);
        sim_verdeclarationprefix = header.length();
    }

    /**
     * Method to add "signame" to the collection of signal names in a declaration.
     */
    private void addDeclaration(String signame)
    {
        if (sim_verDeclarationLine.length() + signame.length() + 3 > MAXDECLARATIONWIDTH)
        {
            printWriter.println(sim_verDeclarationLine.toString() + ";");
            sim_verDeclarationLine.delete(sim_verdeclarationprefix, sim_verDeclarationLine.length());
        }
        if (sim_verDeclarationLine.length() != sim_verdeclarationprefix)
            sim_verDeclarationLine.append(",");
        sim_verDeclarationLine.append(" " + signame);
    }

    /**
     * Method to terminate the collection of signal names in a declaration
     * and write the declaration to the Verilog file.
     */
    private void termDeclaration()
    {
        printWriter.println(sim_verDeclarationLine.toString() + ";");
    }

    /**
     * Method to adjust name "p" and return the string.
     * This code removes all index indicators and other special characters, turning
     * them into "_".
     */
    private String nameNoIndices(String p)
    {
        StringBuffer sb = new StringBuffer();
        if (TextUtils.isDigit(p.charAt(0))) sb.append('_');
        for(int i=0; i<p.length(); i++)
        {
            char chr = p.charAt(i);
            if (!TextUtils.isLetterOrDigit(chr) && chr != '_' && chr != '$') chr = '_';
            sb.append(chr);
        }
        return sb.toString();
    }

    /****************************** SUBCLASSED METHODS FOR THE TOPOLOGY ANALYZER ******************************/

    /**
     * Method to adjust a cell name to be safe for Verilog output.
     * @param name the cell name.
     * @return the name, adjusted for Verilog output.
     */
    protected String getSafeCellName(String name)
    {
        String n = getSafeNetName(name, false);
        // [ and ] are not allowed in cell names
        return n.replaceAll("[\\[\\]]", "_");
    }

    /** Method to return the proper name of Power */
    protected String getPowerName(Network net) { return "vdd"; }

    /** Method to return the proper name of Ground */
    protected String getGroundName(Network net) { return "gnd"; }

    /** Method to return the proper name of a Global signal */
    protected String getGlobalName(Global glob) { return "glbl." + glob.getName(); }

    /** Method to report that export names DO take precedence over
     * arc names when determining the name of the network. */
    protected boolean isNetworksUseExportedNames() { return true; }

    /** Method to report that library names ARE always prepended to cell names. */
    protected boolean isLibraryNameAlwaysAddedToCellName() { return true; }

    /** Method to report that aggregate names (busses) ARE used. */
    protected boolean isAggregateNamesSupported() { return true; }

	/** Abstract method to decide whether aggregate names (busses) can have gaps in their ranges. */
	protected boolean isAggregateNameGapsSupported() { return true; }

    /** Method to report whether input and output names are separated. */
    protected boolean isSeparateInputAndOutput() { return true; }

    /**
     * Method to adjust a network name to be safe for Verilog output.
     * Verilog does permit a digit in the first location; prepend a "_" if found.
     * Verilog only permits the "_" and "$" characters: all others are converted to "_".
     * Verilog does not permit nonnumeric indices, so "P[A]" is converted to "P_A_"
     * Verilog does not permit multidimensional arrays, so "P[1][2]" is converted to "P_1_[2]"
     *   and "P[1][T]" is converted to "P_1_T_"
     * @param bus true if this is a bus name.
     */
    protected String getSafeNetName(String name, boolean bus)
    {
        // simple names are trivially accepted as is
        boolean allAlnum = true;
        int len = name.length();
        if (len == 0) return name;
        int openSquareCount = 0;
        int openSquarePos = 0;
        for(int i=0; i<len; i++)
        {
            char chr = name.charAt(i);
            if (chr == '[') { openSquareCount++;   openSquarePos = i; }
            if (!TextUtils.isLetterOrDigit(chr)) allAlnum = false;
            if (i == 0 && TextUtils.isDigit(chr)) allAlnum = false;
        }
        if (!allAlnum || !Character.isLetter(name.charAt(0)))
        {
            // if there are indexed values, make sure they are numeric
            if (openSquareCount == 1)
            {
                if (openSquarePos+1 >= name.length() ||
                    !Character.isDigit(name.charAt(openSquarePos+1))) openSquareCount = 0;
            }
            if (bus) openSquareCount = 0;

            StringBuffer sb = new StringBuffer();
            for(int t=0; t<name.length(); t++)
            {
                char chr = name.charAt(t);
                if (chr == '[' || chr == ']')
                {
                    if (openSquareCount == 1) sb.append(chr); else
                    {
                        sb.append('_');
                        if (t+1 < name.length() && chr == ']' && name.charAt(t+1) == '[') t++;
                    }
                } else
                {
                    if (t == 0 && TextUtils.isDigit(chr)) sb.append('_');
                    if (TextUtils.isLetterOrDigit(chr) || chr == '$')
                        sb.append(chr); else
                            sb.append('_');
                }
            }
            name = sb.toString();
        }

        // make sure it isn't a reserved word
        if (reservedWords.contains(name)) name = "_" + name;

        return name;
    }

    /** Tell the Hierarchy enumerator how to short resistors */
    @Override
    protected Netlist.ShortResistors getShortResistors() { return Netlist.ShortResistors.PARASITIC; }
    
    /**
     * Method to tell whether the topological analysis should mangle cell names that are parameterized.
     */
    protected boolean canParameterizeNames() {
        if (Simulation.getVerilogStopAtStandardCells())
            return false;
        return true;
    }


    private static final String [] verilogGates = new String [] {
        "and",
        "nand",
        "or",
        "nor",
        "xor",
        "xnor",
        "buf",
        "bufif0",
        "bufif1",
        "not",
        "notif0",
        "notif1",
        "pulldown",
        "pullup",
        "nmos",
        "rnmos",
        "pmos",
        "rpmos",
        "cmos",
        "rcmos",
        "tran",
        "rtran",
        "tranif0",
        "rtranif0",
        "tranif1",
        "rtranif1",
    };

    /**
     * Perform some checks on parsed Verilog data, including the
     * check that module is defined for the cell it is replacing.
     * Some checks are different depending upon if the source is
     * a Verilog View or an included external file.
     * @param data the parsed Verilog data
     * @param cell the cell to be replaced (must be verilog view if replacing Verilog View)
     * @param includeFile the included file (null if replacing Verilog View)
     * @return false if there was an error
     */
    private boolean checkIncludedData(VerilogData data, Cell cell, String includeFile) {
        // make sure there is module for current cell
        Collection<VerilogData.VerilogModule> modules = data.getModules();
        VerilogData.VerilogModule main = null, alternative = null;
        for (VerilogData.VerilogModule mod : modules) {
            if (mod.getName().equals(getVerilogName(cell)))
                main = mod;
            else {
                Cell.CellGroup grp = cell.getCellGroup(); // if cell group is available
                if (grp != null && mod.getName().equals(grp.getName()))
                    alternative = mod;
            }
        }
        if (main == null) main = alternative;
        if (main == null) {
            System.out.println("Error! Expected Verilog module definition '"+getVerilogName(cell)+
            " in Verilog View: "+cell.libDescribe());
            return false;
        }
        definedPrimitives.put(cell, main);

        String source = includeFile == null ? "Verilog View for "+cell.libDescribe() :
                "Include file: "+includeFile;
        // check that modules have not already been defined
        for (VerilogData.VerilogModule mod : modules) {
            String prevSource = definedModules.get(mod.getName());
            if (mod.isValid()) {
                if (prevSource != null) {
                    System.out.println("Error, module "+mod.getName()+" already defined from: "+prevSource);
                }
                else
                    definedModules.put(mod.getName(), source);
            }
        }

        // make sure ports for module match ports for cell
//        Collection<VerilogData.VerilogPort> ports = main.getPorts();
        // not sure how to do this right now

        // check for undefined instances, search libraries for them
        for (VerilogData.VerilogModule mod : modules) {
            for (VerilogData.VerilogInstance inst : mod.getInstances()) {
                VerilogData.VerilogModule instMod = inst.getModule();
                if (instMod.isValid())
                    continue;       // found in file, continue
                // check if primitive
                boolean primitiveGate = false;
                for (String s : verilogGates) {
                    if (s.equals(instMod.getName().toLowerCase())) {
                        primitiveGate = true;
                        break;
                    }
                }
                if (primitiveGate) continue;

                // undefined, look in modules already written
                String moduleName = instMod.getName();
                String found = definedModules.get(moduleName);
                if (found == null && includeFile == null) {
                    // search libraries for it
                    Cell missingCell = findCell(moduleName, View.VERILOG);
                    if (missingCell == null) {
                        System.out.println("Error: Undefined reference to module "+moduleName+", and no matching cell found");
                        continue;
                    }
                    // hmm...name map might be wrong at for this new enumeration
                    System.out.println("Info: Netlisting cell "+missingCell.libDescribe()+" as instanced in: "+source);
                    HierarchyEnumerator.enumerateCell(missingCell, VarContext.globalContext,
                            new Visitor(this), getShortResistors());
                }
            }
        }
        return true;
    }

    /**
     * Get the Verilog-style name for a cell.
     * @param cell
     * @return the name of the cell
     */
    public static String getVerilogName(Cell cell) {
        final Verilog v = new Verilog();
        return cell.getLibrary().getName() + "__" + v.getSafeCellName(cell.getName());
    }

    /**
     * Find a cell corresponding to the Verilog-style name
     * of a cell. See
     * {@link Verilog#getVerilogName(com.sun.electric.database.hierarchy.Cell)}.
     * @param verilogName the Verilog-style name of the cell
     * @param preferredView the preferred cell view. Schematic if null.
     * @return the cell, or null if not found
     */
    public static Cell findCell(String verilogName, View preferredView) {
        String [] parts = verilogName.split("__");
        if (parts.length != 2) {
            //System.out.println("Invalid Verilog-style module name: "+verilogName);
            return null;
        }
        Library lib = Library.findLibrary(parts[0]);
        if (lib == null) {
            System.out.println("Cannot find library "+parts[0]+" for Verilog-style module name: "+verilogName);
            return null;
        }
        if (preferredView == null) preferredView = View.SCHEMATIC;
        Cell cell = lib.findNodeProto(parts[1]);
        if (cell == null) {
            System.out.println("Cannot find Cell "+parts[1]+" in Library "+parts[0]+" for Verilog-style module name: "+verilogName);
            return null;
        }
        Cell preferred = cell.otherView(preferredView);
        if (preferred != null) return preferred;

        preferred = cell.getCellGroup().getMainSchematics();
        if (preferred != null) return preferred;

        return cell;
    }
}
