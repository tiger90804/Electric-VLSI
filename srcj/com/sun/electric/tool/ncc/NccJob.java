/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: NccJob.java
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
package com.sun.electric.tool.ncc;
import java.util.Date;
import java.util.List;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.generator.layout.LayoutLib;
import com.sun.electric.tool.ncc.basic.CellContext;
import com.sun.electric.tool.ncc.basic.NccUtils;
import com.sun.electric.tool.user.User;

public class NccJob extends Job {
	private final int numWindows;
	private final int operation;
	
	private CellContext[] getSchemLayFromCurrentWindow() {
		CellContext curCellCtxt = NccUtils.getCurrentCellContext();
		if (curCellCtxt==null) {
			System.out.println("Please open the Cell you wish to NCC");
			return null;
		} 
		Cell[] schLay = NccUtils.findSchematicAndLayout(curCellCtxt.cell);
		if (schLay==null) {
			System.out.println("current Cell Group doesn't have both schematic and layout Cells");
			return null;
		}
		CellContext[] cc = {
			new CellContext(schLay[0], curCellCtxt.context),
			new CellContext(schLay[1], curCellCtxt.context)
		};
		return cc;
	}
	
	private boolean isSchemOrLay(CellContext cc) {
		Cell c = cc.cell;
		View v = c.getView();
		boolean ok = v==View.SCHEMATIC || v==View.LAYOUT;
		if (!ok) System.out.println("Cell: "+NccUtils.fullName(c)+
									" isn't schematic or layout");
		return ok;
	}

	private boolean isSchem(CellContext cc) {
		return cc.cell.getView()==View.SCHEMATIC;
	}
	private void prln(String s) {System.out.println(s);}

	/** If one Cell is a schematic then put it first
	 * @return null if not schematic or layout Cells */ 
	private CellContext[] getTwoCellsFromTwoWindows() {
		List cellCtxts = NccUtils.getCellContextsFromWindows();
		if (cellCtxts.size()<2) {
			System.out.println("Two Cells aren't open in two windows");
			return null;
		} 
		if (cellCtxts.size()>2) {
			prln("More than two Cells are open in windows. Could you please");
			prln("close windows until only two Cells are open. (Sorry JonL.)");
			return null;
		}
		CellContext[] cellContexts = new CellContext[2];
		cellContexts[0] = (CellContext) cellCtxts.get(0);
		cellContexts[1] = (CellContext) cellCtxts.get(1);
	
		if (!isSchemOrLay(cellContexts[0]) || 
		    !isSchemOrLay(cellContexts[1])) return null;

		// Try to put schematic first
		if (!isSchem(cellContexts[0]) && isSchem(cellContexts[1])) {
			CellContext cc = cellContexts[0];
			cellContexts[0] = cellContexts[1];
			cellContexts[1] = cc;
		}

	    return cellContexts;
	}

	/** @return array of two CellContexts to compare */
	private CellContext[] getCellsFromWindows(int numWindows) {
		if (numWindows==2) return getTwoCellsFromTwoWindows();
		else return getSchemLayFromCurrentWindow();
	}
	
	private NccOptions getOptionsFromNccConfigDialog() {
		NccOptions options = new NccOptions();
		options.verbose = false;
		options.checkSizes = NCC.getCheckSizes();
		// convert percent to fraction
		options.relativeSizeTolerance = NCC.getRelativeSizeTolerance()/100;
		options.absoluteSizeTolerance = NCC.getAbsoluteSizeTolerance();
		options.haltAfterFirstMismatch = NCC.getHaltAfterFirstMismatch();
		options.maxMatchedEquivRecsToPrint = NCC.getMaxMatchedClasses();
		options.maxMismatchedEquivRecsToPrint = NCC.getMaxMismatchedClasses();
		options.maxEquivRecMembersToPrint = NCC.getMaxClassMembers();
		
		return options;
	}

    public boolean doIt() {
		CellContext[] cellCtxts = getCellsFromWindows(numWindows);

		boolean skipPassed = NCC.getSkipPassed();
		NccOptions options = getOptionsFromNccConfigDialog();

		if (cellCtxts==null) {
			return false;
		} else if (operation==NCC.LIST_ANNOTATIONS) {
			ListNccAnnotations.doYourJob(cellCtxts);
			return true;
		} else {
			NccResult result;
	    	Date before = new Date();
			if (operation==NCC.FLAT_TOP_CELL) {
				prln("Flat NCC top cell");
				result = NccUtils.compareAndPrintStatus(cellCtxts[0], cellCtxts[1], 
			                                            null, options);
			} else if (operation==NCC.FLAT_EACH_CELL) {
				prln("Flat NCC every cell in the design");
				result = NccBottomUp.compare(cellCtxts[0].cell, cellCtxts[1].cell, 
				                             false, skipPassed, options);
			} else if (operation==NCC.HIER_EACH_CELL) {
				prln("Hierarchical NCC every cell in the design");
				result = NccBottomUp.compare(cellCtxts[0].cell, cellCtxts[1].cell, 
	                                         true, skipPassed, options);
			} else {
				LayoutLib.error(true, "bad operation: "+operation);
				return false;
			}
			System.out.println("Summary for all cells "+result.summary(options.checkSizes));
			Date after = new Date();
			System.out.println("NCC command completed in: "+
			                   NccUtils.hourMinSec(before, after)+".");
			return result.match();
		}
    }

	// ------------------------- public method --------------------------------
	/**
	 * @param numWindows may be 1 or 2. 1 means compare the schematic and layout 
	 * views of the current window. 2 means compare the 2 Cells open in 2 Windows.
	 */
	public NccJob(int numWindows) {
		super("Run NCC", User.tool, Job.Type.CHANGE, null, null, 
		      Job.Priority.ANALYSIS);
		LayoutLib.error(numWindows!=1 && numWindows!=2, 
                        "numWindows must be 1 or 2");
		this.numWindows = numWindows;
		this.operation = NCC.getOperation();
		startJob();
	}
}

//String homeDir;
//if (!LayoutLib.osIsWindows()) {
//	homeDir = "/home/rkao/";
//} else {
//	homeDir = "c:/";
//}
//String testDir = homeDir+"ivanTest/qFourP1/electric-final/";
//
//doOneCell(testDir);		
////doLib(testDir, "purple");		
////doLib(testDir, "inv");		
////doLib(testDir, "invDrive");		
////doLib(testDir, "psDrive");
////doLib(testDir, "srNand");
////doLib(testDir, "gasp");
////doLib(testDir, "senseAmp");
////doLib(testDir, "scanChainFour");
////doLib(testDir, "latches");
////doLib(testDir, "gasP_COR");
////doLib(testDir, "rings");
////doLib(testDir, "senseAmp");
////doLib(testDir, "senseReg");
////doLib(testDir, "rxPads");


//private void compareCellToSelf(Cell cell) {
//	  messenger.println("SelfComparison of: "+fullName(cell));
//	  NccOptions options = new NccOptions();
//	  options.checkSizes = false;
//	  options.mergeParallelCells = true;
//
//	  List cells = new ArrayList();
//	  cells.add(cell);  cells.add(cell);
//	  List contexts = new ArrayList();
//	  contexts.add(null);  contexts.add(null);
//	  List netlists = new ArrayList();
//	  Netlist netlist = cell.getNetlist(true);
//	  netlists.add(netlist);  netlists.add(netlist);
//
//	  boolean matched = NccEngine.compare(cells, contexts, netlists, options);
//	  messenger.println("Ncc "+(matched ? "succeeds" : "fails"));
//	  messenger.error(!matched, "Ncc fails");
//}
//	
//private void doOneCell(String testDir) {
////		Library lib = 
////			LayoutLib.openLibForRead("qFourP1", testDir+"qFourP1.elib");
////		Cell cell = lib.findNodeProto("expTail{lay}");
//	  Library lib = 
//		  LayoutLib.openLibForRead("rxPads", testDir+"rings.elib");
//	  Cell cell = lib.findNodeProto("rxPadArray{lay}");
//	  compareCellToSelf(cell);
//}
//	
//	
//private void doLib(String testDir, String libName) {
//	  Library lib = LayoutLib.openLibForRead(libName, 
//											 testDir+libName+".elib");
//	  for (Iterator it=lib.getCells(); it.hasNext();) {
//		  Cell cell = (Cell) it.next();
//		  View view = cell.getView();
//		  if (view==View.ICON)  continue;
//		  // galleries
//		  if (cell.getName().equals("gallery"))  continue;
//		  if (cell.getName().equals("halfKeepIndex"))  continue;
//		  if (cell.getName().equals("aScanIndex"))  continue;
//		  if (cell.getName().equals("aScanIndexB"))  continue;
//			
//		  // paralleled gates need random matching
//		  // These were eliminated by the Cell based parallel merging
////			if (cell.getName().equals("gaspRowC"))  continue;
////			if (cell.getName().equals("gaspRing1"))  continue;
////			if (cell.getName().equals("gaspRowD"))  continue;
//
//		  // paralleling hidden from my merger!
//		  if (cell.getName().equals("ringTx") &&
//			  view==View.LAYOUT)  continue;
//				
//		  // I don't understand this one				
//		  if (cell.getName().equals("rxPadArray") &&
//			  view==View.LAYOUT)  continue;
//			
//		  compareCellToSelf(cell);									
//	  }
//}

