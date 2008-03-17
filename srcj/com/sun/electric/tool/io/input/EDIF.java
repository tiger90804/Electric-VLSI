/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: EDIF.java
 * Input/output tool: EDIF 2.0.0 input
 * Original EDIF reader by Glen Lawson.
 * Translated into Java by Steven M. Rubin, Sun Microsystems.
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
package com.sun.electric.tool.io.input;

import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.GenMath.MutableInteger;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.topology.RTBounds;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.io.IOTool;
import com.sun.electric.tool.io.output.EDIFEquiv;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ViewChanges;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class reads files in EDIF files.
 * <BR>
 * Notes:
 *	I have tried EDIF files from CADENCE and VALID only.
 *	Does not fully support portbundles
 *	Multiple ports of the same name are named port_x (x is 1 to n duplicate)
 *	Keywords such as ARRAY have unnamed parameters, ie (array (name..) 5 6)
 *	this is handled in the processInteger function called by getKeyword,
 *	this is a hack to fix this problem, a real table driven parser should be used.
 *	Use circle arcs instead of splines.
 *	Support text justifications and text height
 * 	Better NAME/RENAME/STRINGDISPLAY/ANNOTATE text handling.
 *  ANSI prototypes
 *	Changed arcs to simple polygons plus ARC attribute
 *  Can read NETLIST views
 */
public class EDIF extends Input
{
	private static final double INCH = 10;
	private static final int SHEETWIDTH = 44;		// was 32
	private static final int SHEETHEIGHT = 20;

	// name table for layers
	private static class NameEntry
	{
		/** the original MASK layer */	private String original;
		/** the replacement layer */	private String replace;
		/** the basic electric node */	private NodeProto node;
		/** default text height */		private int textHeight;
		/** default justification */	private TextDescriptor.Position justification;
		/** layer is visible */			private boolean visible;
	};

	// Edif viewtypes ...
	private static class ViewType
	{
		private String name;
		ViewType(String name) { this.name = name; }
		public String toString() { return "VIEWTYPE "+name; }
	}
	private static final ViewType VNULL       = new ViewType("Null");
	private static final ViewType VBEHAVIOR   = new ViewType("Behavior");
	private static final ViewType VDOCUMENT   = new ViewType("Document");
	private static final ViewType VGRAPHIC    = new ViewType("Graphic");
	private static final ViewType VLOGICMODEL = new ViewType("LogicModel");
	private static final ViewType VMASKLAYOUT = new ViewType("MaskLayout");
	private static final ViewType VNETLIST    = new ViewType("Netlist");
	private static final ViewType VPCBLAYOUT  = new ViewType("PCBLayout");
	private static final ViewType VSCHEMATIC  = new ViewType("Schematic");
	private static final ViewType VSTRANGER   = new ViewType("Stranger");
	private static final ViewType VSYMBOLIC   = new ViewType("Symbolic");
	private static final ViewType VSYMBOL     = new ViewType("Symbol");	/* not a real EDIF view, but electric has one */

	// Edif geometry types ...
	private static class GeometryType {}
	private static final GeometryType GUNKNOWN   = new GeometryType();
//	private static final GeometryType GRECTANGLE = new GeometryType();
//	private static final GeometryType GPOLYGON   = new GeometryType();
//	private static final GeometryType GSHAPE     = new GeometryType();
//	private static final GeometryType GOPENSHAPE = new GeometryType();
//	private static final GeometryType GTEXT      = new GeometryType();
//	private static final GeometryType GPATH      = new GeometryType();
	private static final GeometryType GINSTANCE  = new GeometryType();
//	private static final GeometryType GCIRCLE    = new GeometryType();
//	private static final GeometryType GARC       = new GeometryType();
	private static final GeometryType GPIN       = new GeometryType();
	private static final GeometryType GNET       = new GeometryType();
	private static final GeometryType GBUS       = new GeometryType();

	// 8 standard orientations
	private static final Orientation OR0      = Orientation.fromC(0, false);
	private static final Orientation OR90     = Orientation.fromC(900, false);
	private static final Orientation OR180    = Orientation.fromC(1800, false);
	private static final Orientation OR270    = Orientation.fromC(2700, false);
	private static final Orientation OMX      = Orientation.fromC(2700, true);
	private static final Orientation OMY      = Orientation.fromC(900, true);
	private static final Orientation OMYR90   = Orientation.fromC(0, true);
	private static final Orientation OMXR90   = Orientation.fromC(1800, true);

	private static class EDIFPort
	{
		private String name;
		private String reference;
		private PortCharacteristic direction;
		private EDIFPort next;
	};

	private static class VendorType {}
	private static final VendorType EVUNKNOWN    = new VendorType();
	private static final VendorType EVCADENCE    = new VendorType();
	private static final VendorType EVVIEWLOGIC  = new VendorType();

	private static class EDIFProperty
	{
		private String name;
		private Object val;
		private EDIFProperty next;
	}

	// view information ...
	/** the schematic page number */			private int pageNumber;
	/** indicates we are in a NETLIST view */	private ViewType activeView;
	/** the current vendor type */				private VendorType curVendor;

	// parser variables ...
	/** the current parser state */				private EDIFKEY curKeyword;
	/** the read buffer */						private String inputBuffer;
	/** the position within the buffer */		private int inputBufferPos;
	/** no update flag */						private boolean ignoreBlock;
	/** no update flag */						private boolean ignoreHigherBlock;
	/** load status */							private int errorCount, warningCount;

	// electric context data ...
	/** the new library */						private Library curLibrary;
	/** the active technology */				private Technology curTechnology;
	/** the current active cell */				private Cell curCell;
	/** the current page in cell */				private int curCellPage;
	/** the parameter position in cell */		private int curCellParameterOff;
	/** the current active instance */			private NodeInst curNode;
	/** the current active arc */				private ArcInst curArc;
	/** the current figure group node */		private NodeProto curFigureGroup;
	/** the cellRef type */						private NodeProto cellRefProto;
	/** the cellRef tech bits if primitive */	private int cellRefProtoTechBits;
	/** the cellRef addt'l rotation (in degrees) */ private int cellRefProtoRotation;
	/** the cellRef offset when mapped */		private double cellRefOffsetX, cellRefOffsetY;
	/** the current port proto */				private PortProto curPort;

	// general geometry information ...
	/** the current geometry type */			private GeometryType curGeometryType;
	/** the list of points */					private List<Point2D> curPoints;
	/** the orientation of the structure */		private Orientation curOrientation;
	/** port direction */						private PortCharacteristic curDirection;

	// geometric path constructors ...
	/** the width of the path */				private int pathWidth;
	/** extend path end flag */					private boolean extendEnd;

	// array variables ...
	/** set if truely an array */				private boolean isArray;
	/** the bounds of the array */				private int arrayXVal, arrayYVal;
	/** offsets in x and y for X increment */	private double deltaPointXX, deltaPointXY;
	/** offsets in x and y for Y increment */	private double deltaPointYX, deltaPointYY;
	/** which offset flag */					private boolean deltaPointsSet;
	/** the element of the array */				private int memberXVal, memberYVal;

	// text variables ...
	/** Text string */							private String textString;
	/** the current default text height */		private int textHeight;
	/** the current text justificaton */		private TextDescriptor.Position textJustification;
	/** is stringdisplay visible */				private boolean textVisible;
	/** origin x and y */						private List<Point2D> saveTextPoints;
	/** the height of text */					private int saveTextHeight;
	/** justification of the text */			private TextDescriptor.Position saveTextJustification;

	// technology data ...
	/** scaling value */						private double inputScale;

	// current name and rename of EDIF objects ...
	/** the current cell name */				private String cellReference;
	/** the current cell name (original) */		private String cellName;
	/** the current port name */				private String portReference;
	/** the current port name (original) */		private String portName;
	/** the current instance name */			private String instanceReference;
	/** the current instance name (original) */	private String instanceName;
	/** the current bundle name */				private String bundleReference;
	/** the current bundle name (original) */	private String bundleName;
	/** the current net name */					private String netReference;
	/** the current net name (original) */		private String netName;
	/** the current property name */			private String propertyReference;
	/** the current viewRef name */				private String viewRef;
	/** the current cellRef name */				private String cellRef;
	/** the current libraryRef name */			private String libraryRef;

	/** property value */						private Object propertyValue;

	/** the name of the object */				private String objectName;
	/** the original name of the object */		private String originalName;

	/** cells that have been built so far */	private Set<Cell> builtCells;

	// layer or figure information ...
	/** the current name mapping table */		private List<NameEntry> nameEntryList;
	/** the current name table entry */			private NameEntry curNameEntry;

	/** cell name lookup (from rename) */ 		private Map<String,NameEntry> cellTable;

	// port data for port exporting
	/** active port list */						private EDIFPort portsListHead;

	// property data for all objects
	/** active property list */					private EDIFProperty propertiesListHead;

	// view NETLIST layout
	/** current sheet position */				private int sheetXPos, sheetYPos;

	/** stack of keywords at this point */		private EDIFKEY [] keyStack = new EDIFKEY[1000];
	/** depth of keyword stack */				private int keyStackDepth;

	/** Edif equivs for primitives */			private EDIFEquiv equivs;
	/** List of ports in net -> joined list */  private List<PortInst> netPortRefs;
	/** mapping of renamed objects */			private Map<String,String> renamedObjects;
	/** list of offpage nodes in the cell */	private List<NodeInst> offpageNodes;
	/** list of nodes that were equived */		private Map<String,EDIFEquiv.NodeEquivalence> mappedNodes;
	/** mapping of named arcs in each cell */	private Map<Cell,Map<String,List<ArcInst>>> namedArcs = new HashMap<Cell,Map<String,List<ArcInst>>>();
	/** export names needed on current net */	private Set<String> exportsOnNet;
	/** arcs placed on current net */			private List<ArcInst> arcsOnNet;

	// some standard artwork primitivies
	private PortProto defaultPort;
	private PortProto defaultIconPort;
	private PortProto defaultBusPort;
	private PortProto defaultInput;
	private PortProto defaultOutput;

	/** all keywords for parsing */				private static Map<String,EDIFKEY> edifKeys = new HashMap<String,EDIFKEY>();

	/**************************************** MAIN CONTROL ****************************************/

	/**
	 * Method to import a library from disk.
	 * @param lib the library to fill
	 * @return the created library (null on error).
	 */
	protected Library importALibrary(Library lib)
	{
		// setup keyword prerequisites
		KARRAY.stateArray = new EDIFKEY [] {KINSTANCE, KPORT, KNET};
		KAUTHOR.stateArray = new EDIFKEY [] {KWRITTEN};
		KBOUNDINGBOX.stateArray = new EDIFKEY [] {KSYMBOL, KCONTENTS};
		KCELL.stateArray = new EDIFKEY [] {KEXTERNAL, KLIBRARY};
		KCELLREF.stateArray = new EDIFKEY [] {KDESIGN, KVIEWREF, KINSTANCE};
		KCELLTYPE.stateArray = new EDIFKEY [] {KCELL};
		KCONTENTS.stateArray = new EDIFKEY [] {KVIEW};
		KDESIGN.stateArray = new EDIFKEY [] {KEDIF};
		KDIRECTION.stateArray = new EDIFKEY [] {KPORT};
		KEDIF.stateArray = new EDIFKEY [] {KINIT};
		KEDIFLEVEL.stateArray = new EDIFKEY [] {KEDIF, KEXTERNAL, KLIBRARY};
		KEXTERNAL.stateArray = new EDIFKEY [] {KEDIF};
		KINSTANCE.stateArray = new EDIFKEY [] {KCONTENTS, KPAGE, KPORTIMPLEMENTATION, KCOMMENTGRAPHICS};
		KINSTANCEREF.stateArray = new EDIFKEY [] {KINSTANCEREF, KPORTREF};
		KINTERFACE.stateArray = new EDIFKEY [] {KVIEW};
		KJOINED.stateArray = new EDIFKEY [] {KINTERFACE, KNET};
		KEDIFKEYMAP.stateArray = new EDIFKEY [] {KEDIF};
		KLIBRARYREF.stateArray = new EDIFKEY [] {KCELLREF};
		KLISTOFNETS.stateArray = new EDIFKEY [] {KNETBUNDLE};
		KNET.stateArray = new EDIFKEY [] {KCONTENTS, KPAGE, KLISTOFNETS};
		KNETBUNDLE.stateArray = new EDIFKEY [] {KCONTENTS, KPAGE};
		KNUMBERDEFINITION.stateArray = new EDIFKEY [] {KTECHNOLOGY};
		KPORT.stateArray = new EDIFKEY [] {KINTERFACE, KLISTOFPORTS};
		KSCALE.stateArray = new EDIFKEY [] {KNUMBERDEFINITION};
		KSTATUS.stateArray = new EDIFKEY [] {KCELL, KDESIGN, KEDIF, KEXTERNAL, KLIBRARY, KVIEW};
		KSYMBOL.stateArray = new EDIFKEY [] {KINTERFACE};
		KTECHNOLOGY.stateArray = new EDIFKEY [] {KEXTERNAL, KLIBRARY};
		KTIMESTAMP.stateArray = new EDIFKEY [] {KWRITTEN};
		KUNIT.stateArray = new EDIFKEY [] {KPROPERTY, KSCALE};
		KVIEW.stateArray = new EDIFKEY [] {KPROPERTY, KCELL};
		KVIEWREF.stateArray = new EDIFKEY [] {KINSTANCE, KINSTANCEREF, KPORTREF};
		KVIEWTYPE.stateArray = new EDIFKEY [] {KVIEW};
		KWRITTEN.stateArray = new EDIFKEY [] {KSTATUS};

		// inits
		propertyValue = null;
		curPoints = new ArrayList<Point2D>();
		saveTextPoints = new ArrayList<Point2D>();

		// parser inits
		curKeyword = KINIT;
		inputBuffer = "";
		inputBufferPos = 0;
		errorCount = warningCount = 0;
		ignoreBlock = ignoreHigherBlock = false;
		curVendor = EVUNKNOWN;
		builtCells = new HashSet<Cell>();

		// general inits
		inputScale = IOTool.getEDIFInputScale();
		curLibrary = lib;
		curTechnology = Schematics.tech();
		cellTable = new HashMap<String,NameEntry>();
		portsListHead = null;
		propertiesListHead = null;

		// active database inits
		curCell = null;
		curCellPage = 0;
		curCellParameterOff = 0;
		curNode = null;
		curArc = null;
		curPort = null;
		curFigureGroup = null;
		cellRefProto = null;

		// name inits
		cellReference = "";
		portReference = "";
		instanceReference = "";
		bundleReference = "";
		netReference = "";
		propertyReference = "";

		// geometry inits
		nameEntryList = new ArrayList<NameEntry>();
		curNameEntry = null;
		freePointList();

		// text inits
		textHeight = 0;
		textJustification = TextDescriptor.Position.DOWNRIGHT;
		textVisible = true;
		freeSavedPointList();

		sheetXPos = -1;
		sheetYPos = -1;

		keyStackDepth = 0;

		defaultPort = Schematics.tech().wirePinNode.findPortProto("wire");
		defaultIconPort = Schematics.tech().wirePinNode.getPort(0);
		defaultBusPort = Schematics.tech().busPinNode.findPortProto("bus");
		defaultInput = Schematics.tech().offpageNode.findPortProto("y");
		defaultOutput = Schematics.tech().offpageNode.findPortProto("a");

		equivs = new EDIFEquiv();
		netPortRefs = new ArrayList<PortInst>();
		renamedObjects = new HashMap<String,String>();

		// parse the file
		try
		{
			loadEDIF();
		} catch (IOException e)
		{
			System.out.println("line " + lineReader.getLineNumber() + ": " + e.getMessage());
			return null;
		}

		if (errorCount != 0 || warningCount != 0)
			System.out.println("A total of " + errorCount + " errors, and " + warningCount + " warnings encountered during load");

		if (Job.getUserInterface().getCurrentCell(curLibrary) == null && curLibrary.getCells().hasNext())
			Job.getUserInterface().setCurrentCell(curLibrary, curLibrary.getCells().next());
		return curLibrary;
	}

	/**
	 * Method to load the edif netlist into memory
	 * Does a simple keyword lookup to load the lisp structured
	 * EDIF language into Electric's database
	 */
	private void loadEDIF()
		throws IOException
	{
		int saveStack = -1;

		// now read and parse the edif netlist
		for(;;)
		{
			String token = getKeyword();
			if (token == null) break;

			// locate the keyword
			EDIFKEY key = edifKeys.get(TextUtils.canonicString(token));
			if (key == null)
			{
				System.out.println("Warning, line " + lineReader.getLineNumber() + ": unknown keyword <" + token + ">");
				warningCount++;
				keyStack[keyStackDepth++] = curKeyword;
				curKeyword = KUNKNOWN;
				continue;
			}

			// found the keyword, check state
			if (key.stateArray != null && curKeyword != KUNKNOWN)
			{
				boolean found = false;
				for(int i=0; i<key.stateArray.length; i++)
					if (key.stateArray[i] == curKeyword) { found = true;   break; }
				if (!found)
				{
					System.out.println("Error, line " + lineReader.getLineNumber() + ": illegal state (" + curKeyword.name + ") for keyword <" + token + ">");
					errorCount++;
				}
			}

			// call the function
			keyStack[keyStackDepth++] = curKeyword;
			curKeyword = key;
			if (saveStack >= keyStackDepth)
			{
				saveStack = -1;
				ignoreBlock = false;
			}
			if (!ignoreBlock)
			{
				key.push();
				if (ignoreHigherBlock)
				{
					ignoreHigherBlock = false;
					ignoreBlock = true;
					saveStack = keyStackDepth-1;
				}
			}
			if (ignoreBlock)
			{
				if (saveStack == -1) saveStack = keyStackDepth;
			}
		}
		if (curKeyword != KINIT)
		{
			System.out.println("Error, line " + lineReader.getLineNumber() + ": unexpected end-of-file encountered");
			errorCount++;
		}
		cleanupAtEnd();
	}

	/**
	 * Method to get a keyword.
	 */
	private String getKeyword()
		throws IOException
	{
		// look for a '(' before the edif keyword
		for(;;)
		{
			String p = getToken((char)0);
			if (p == null) break;
			if (p.equals("(")) break;
			if (p.equals(")"))
			{
				// pop the keyword state stack, called when ')' is encountered.
				if (keyStackDepth != 0)
				{
					if (!ignoreBlock)
					{
						curKeyword.pop();
					}
					if (keyStackDepth != 0) curKeyword = keyStack[--keyStackDepth]; else
						curKeyword = KINIT;
				}
			} else
			{
//				if (TextUtils.isANumber(inputBuffer)) processInteger(TextUtils.atoi(inputBuffer));
				if (TextUtils.isANumber(p)) processInteger(TextUtils.atoi(p));
			}
		}
		return getToken((char)0);
	}

	private void cleanupAtEnd()
	{
		// clean-up schematic cells with isolated pins
		for(Cell cell : builtCells)
		{
			if (cell.isSchematic())
			{
				List<NodeInst> deletePins = new ArrayList<NodeInst>();
				for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
				{
					NodeInst ni = it.next();
					if (ni.getProto() == Schematics.tech().wirePinNode)
					{
						if (!ni.hasConnections() && !ni.hasExports())
							deletePins.add(ni);
					}
				}
				for(NodeInst ni : deletePins)
				{
					ni.kill();
				}
			}
		}

		// put names on named arcs
		for(Cell cell : namedArcs.keySet())
		{
			Netlist nl = cell.acquireUserNetlist();
			Map<String,List<ArcInst>> arcsInCell = namedArcs.get(cell);
			Map<ArcInst,String> arcsToName = new HashMap<ArcInst,String>();
			for(String name : arcsInCell.keySet())
			{
				List<ArcInst> arcsWithName = arcsInCell.get(name);
				Collections.sort(arcsWithName, new ArcsByLength());
				Set<Network> netsNamed = new HashSet<Network>();
				for(int i=0; i<arcsWithName.size(); i++)
				{
					ArcInst ai = arcsWithName.get(i);
					if (!ai.isLinked()) continue;
					Network net = nl.getNetwork(ai, 0);
					if (netsNamed.contains(net)) continue;
					netsNamed.add(net);
					arcsToName.put(ai, name);
				}
			}
			for(ArcInst ai : arcsToName.keySet())
			{
				String arcName = arcsToName.get(ai);
				if (ai.getName().equals(arcName)) continue;
				if (ai.getNameKey().isTempname())
				{
					ai.setName(arcName);
				} else
				{
					// two names on one arc: duplicate the arc
					ArcInst second = ArcInst.newInstanceBase(ai.getProto(), ai.getLambdaBaseWidth(),
						ai.getHeadPortInst(), ai.getTailPortInst(), ai.getHeadLocation(), ai.getTailLocation(),
						arcName, ai.getAngle(), ai.getD().flags);
					TextDescriptor td = TextDescriptor.getArcTextDescriptor().withOff(0, -1);
					second.setTextDescriptor(ArcInst.ARC_NAME, td);
				}
			}
		}
	}

	/**
	 * Comparator class for sorting ArcInst by their length.
	 */
	private static class ArcsByLength implements Comparator<ArcInst>
	{
		/**
		 * Method to sort ArcInst by their length.
		 */
		public int compare(ArcInst a1, ArcInst a2)
		{
			double len1 = a1.getHeadLocation().distance(a1.getTailLocation());
			double len2 = a2.getHeadLocation().distance(a2.getTailLocation());
			if (len1 == len2) return 0;
			if (len1 < len2) return 1;
			return -1;
		}
	}

	/**
	 * Method to do cleanup processing of an integer argument such as in (array (...) 1 2)
	 */
	private void processInteger(int value)
	{
		if (keyStackDepth > 0)
		{
			if (!ignoreBlock)
			{
				if (curKeyword == KARRAY)
				{
					if (arrayXVal == 0) arrayXVal = value;
						else if (arrayYVal == 0) arrayYVal = value;
				} else if (curKeyword == KMEMBER)
				{
					if (memberXVal == -1) memberXVal = value;
						else if (memberYVal == -1) memberYVal = value;
				}
			}
		}
	}

	/**************************************** SUPPORT ****************************************/

	/**
	 * Method to position to the next token
	 * @return true on EOF.
	 */
	private boolean positionToNextToken()
		throws IOException
	{
		for(;;)
		{
			if (inputBuffer == null) return true;
			if (inputBufferPos >= inputBuffer.length())
			{
				inputBuffer = readWholeLine();
				if (inputBuffer == null) return true;
				inputBufferPos = 0;
				continue;
			}
			char chr = inputBuffer.charAt(inputBufferPos);
			if (chr == ' ' || chr == '\t')
			{
				inputBufferPos++;
				continue;
			}
			return false;
		}
	}

	/**
	 * Method to get a delimeter routine
	 */
	private void getDelimeter(char delim)
		throws IOException
	{
		if (positionToNextToken())
		{
			throw new IOException("Unexpected end-of-file");
		}
		char chr = inputBuffer.charAt(inputBufferPos);
		if (chr != delim)
		{
			throw new IOException("Illegal delimeter");
		}
		inputBufferPos++;
	}

	/**
	 * Method to get a token
	 */
	private String getToken(char iDelim)
		throws IOException
	{
		// locate the first non-white space character for non-delimited searches
		char delim = iDelim;
		if (delim == 0)
		{
			if (positionToNextToken()) return null;
		}

		// set up the string copy
		StringBuffer sBuf = new StringBuffer();
		char chr = inputBuffer.charAt(inputBufferPos);
		if (delim == 0 && chr == '"')
		{
			// string search
			delim = '"';
			sBuf.append(chr);
			inputBufferPos++;
		}

		// now locate the next white space or the delimiter
		for(;;)
		{
			if (inputBufferPos >= inputBuffer.length())
			{
				// end of string, get the next line
				inputBuffer = readWholeLine();
				if (inputBuffer == null)
				{
					// end-of-file, if no delimiter return NULL, otherwise the string
					if (delim != 0) break;
					return sBuf.toString();
				}
				inputBufferPos = 0;
			} else if (delim == 0)
			{
				chr = inputBuffer.charAt(inputBufferPos);
				switch (chr)
				{
					case ' ':
					case '\t':
						// skip to the next character
						inputBufferPos++;
						return sBuf.toString();
						// special EDIF delimiters
					case '(':
					case ')':
						if (sBuf.length() == 0)
						{
							sBuf.append(chr);
							inputBufferPos++;
						}
						return sBuf.toString();
					default:
						sBuf.append(chr);
						inputBufferPos++;
						break;
				}
			} else
			{
				// check for user specified delimiter
				chr = inputBuffer.charAt(inputBufferPos);
				if (chr == delim)
				{
					// skip the delimiter, unless string
					if (delim != iDelim) sBuf.append(chr);
					inputBufferPos++;
					return sBuf.toString();
				}
				sBuf.append(chr);
				inputBufferPos++;
			}
		}
		return null;
	}

	private void makeFigure()
		throws IOException
	{
		// get the layer name; check for figuregroup override
		if (positionToNextToken()) return;
		if (inputBuffer.charAt(inputBufferPos) == '(') return;
		String layer = getToken((char)0);

		// now look for this layer in the list of layers
		for(NameEntry nt : nameEntryList)
		{
			if (nt.original.equalsIgnoreCase(layer))
			{
				// found the layer
				curFigureGroup = nt.node;
				textHeight = nt.textHeight;
				textJustification = nt.justification;
				textVisible = nt.visible;

				// allow new definitions
				curNameEntry = nt;
				return;
			}
		}

		NameEntry nt = new NameEntry();
		nameEntryList.add(nt);
		nt.original = layer;
		nt.replace = nt.original;
		curFigureGroup = nt.node = Artwork.tech().boxNode;
		textHeight = nt.textHeight = 0;
		textJustification = nt.justification = TextDescriptor.Position.DOWNRIGHT;
		textVisible = nt.visible = true;

		// allow new definitions
		curNameEntry = nt;
	}

	/**
	 * Method to determine whether a name has multiple signals in it.
	 * @param name the name to check.
	 * @return true if it is a bus name.
	 */
	private boolean isBusName(String name)
	{
		if (name == null || name.length() <= 0) return false;
		Name nn = Name.findName(name);
		if (nn == null) return false;
		return nn.isBus();
	}

	private boolean checkName()
		throws IOException
	{
		positionToNextToken();
		char chr = inputBuffer.charAt(inputBufferPos);
		if (chr != '(' && chr != ')')
		{
			String aName = getToken((char)0);
			objectName = fixLeadingAmpersand(aName);
			return true;
		}
		return false;
	}

	private void freePointList()
	{
		curPoints = new ArrayList<Point2D>();
	}

	private void freeSavedPointList()
	{
		saveTextPoints = new ArrayList<Point2D>();
	}

	private double getNumber()
		throws IOException
	{
		String value = getToken((char)0);
		if (value == null) throw new IOException("No integer value");

		if (value.startsWith("("))
		{
			// must be in e notation
			value = getToken((char)0);
			if (value == null) throw new IOException("Illegal number value");
			if (!value.equalsIgnoreCase("e")) throw new IOException("Illegal number value");

			// now the matissa
			value = getToken((char)0);
			if (value == null) throw new IOException("No matissa value");
			double matissa = TextUtils.atof(value);

			// now the exponent
			value = getToken((char)0);
			if (value == null) throw new IOException("No exponent value");
			double exponent = TextUtils.atof(value);
			getDelimeter(')');
			return matissa * Math.pow(10.0, exponent);
		}
		return TextUtils.atof(value);
	}

	private NodeInst placePin(NodeProto np, double cX, double cY, double sX, double sY, Orientation orient, Cell parent)
	{
		for(Iterator<NodeInst> it = parent.getNodes(); it.hasNext(); )
		{
			NodeInst ni = it.next();
			if (ni.getProto() != np) continue;
			if (!ni.getOrient().equals(orient)) continue;
			if (ni.getAnchorCenterX() != cX) continue;
			if (ni.getAnchorCenterY() != cY) continue;
			return ni;
		}
		NodeInst ni = NodeInst.makeInstance(np, new Point2D.Double(cX, cY), sX, sY, parent, orient, null, 0);
		return ni;
	}

	/**
	 * Method to return the text height to use when it says "textheight" units in the EDIF file.
	 */
	private double convertTextSize(double textHeight)
	{
		double i = textHeight / 4;
		if (i <= 0) i = 0.5;
		return i;
	}

	private void makeArc(double [] x, double [] y)
	{
		GenMath.MutableDouble [] A = new GenMath.MutableDouble[2];
		GenMath.MutableDouble [] B = new GenMath.MutableDouble[2];
		GenMath.MutableDouble [] C = new GenMath.MutableDouble[2];
		for(int i=0; i<2; i++)
		{
			A[i] = new GenMath.MutableDouble(0);
			B[i] = new GenMath.MutableDouble(0);
			C[i] = new GenMath.MutableDouble(0);
		}

		// get line equations of perpendicular bi-sectors of p1 to p2
		double px1 = (x[0] + x[1]) / 2.0;
		double py1 = (y[0] + y[1]) / 2.0;

		// now rotate end point 90 degrees
		double px0 = px1 - (y[0] - py1);
		double py0 = py1 + (x[0] - px1);
		equationOfALine(px0, py0, px1, py1, A[0], B[0], C[0]);

		// get line equations of perpendicular bi-sectors of p2 to p3
		px1 = (x[2] + x[1]) / 2.0;
		py1 = (y[2] + y[1]) / 2.0;

		// now rotate end point 90 degrees
		double px2 = px1 - (y[2] - py1);
		double py2 = py1 + (x[2] - px1);
		equationOfALine(px1, py1, px2, py2, A[1], B[1], C[1]);

		// determine the point of intersection
		Point2D ctr = determineIntersection(A, B, C);

		double ixc = ctr.getX();
		double iyc = ctr.getY();

		double dx = ixc - x[0];
		double dy = iyc - y[0];
		double r = Math.hypot(dx, dy);

		// now calculate the angle to the start and endpoint
		dx = x[0] - ixc;  dy = y[0] - iyc;
		if (dx == 0.0 && dy == 0.0)
		{
			System.out.println("Domain error doing arc computation");
			return;
		}
		double a0 = Math.atan2(dy, dx) * 1800.0 / Math.PI;
		if (a0 < 0.0) a0 += 3600.0;

		dx = x[2] - ixc;  dy = y[2] - iyc;
		if (dx == 0.0 && dy == 0.0)
		{
			System.out.println("Domain error doing arc computation");
			return;
		}
		double a2 = Math.atan2(dy, dx) * 1800.0 / Math.PI;
		if (a2 < 0.0) a2 += 3600.0;

		// determine the angle of rotation, object orientation, and direction
		// calculate x1*y2 + x2*y3 + x3*y1 - y1*x2 - y2*x3 - y3*x1
		double area = x[0]*y[1] + x[1]*y[2] + x[2]*y[0] - y[0]*x[1] - y[1]*x[2] - y[2]*x[0];
		double ar = 0, so = 0;
		int rot = 0;
		boolean trans = false;
		if (area > 0.0)
		{
			// counter clockwise
			if (a2 < a0) a2 += 3600.0;
			ar = a2 - a0;
			rot = (int)a0;
			so = (a0 - rot) * Math.PI / 1800.0;
		} else
		{
			// clockwise
			if (a0 > 2700)
			{
				rot = 3600 - (int)a0 + 2700;
				so = ((3600.0 - a0 + 2700.0) - rot) * Math.PI / 1800.0;
			} else
			{
				rot = 2700 - (int)a0;
				so = ((2700.0 - a0) - rot) * Math.PI / 1800.0;
			}
			if (a0 < a2) a0 += 3600;
			ar = a0 - a2;
			trans = true;
		}

		// get the bounds of the circle
		double sX = r*2;
		double sY = r*2;
		Orientation or = Orientation.fromC(rot, trans);
		if (curCellPage > 0) iyc += (curCellPage-1) * Cell.FrameDescription.MULTIPAGESEPARATION;
		NodeInst ni = NodeInst.makeInstance(curFigureGroup != null && curFigureGroup != Artwork.tech().boxNode ?
			curFigureGroup : Artwork.tech().circleNode, new Point2D.Double(ixc, iyc), sX, sY, curCell, or, null, 0);
		if (ni == null)
		{
			System.out.println("Error, line " + lineReader.getLineNumber() + ": could not create arc");
			errorCount++;
		} else
		{
			// store the angle of the arc
			ni.setArcDegrees(so, ar*Math.PI/1800.0);
		}
	}

	private Export makeExport(Cell cell, PortInst pi, String name)
	{
		Export ppt = Export.newInstance(cell, pi, convertParens(name));
		return ppt;
	}

	private String convertParens(String name)
	{
		return name.replace('(', '[').replace(')', ']');
	}

	/**
	 * Method to determine the intersection of two lines
	 * Inputs: Ax + By + C = 0 form
	 * Outputs: x, y - the point of intersection
	 * returns 1 if found, 0 if non-intersecting
	 */
	private Point2D determineIntersection(GenMath.MutableDouble [] A, GenMath.MutableDouble [] B, GenMath.MutableDouble [] C)
	{
		// check for parallel lines
		double A1B2 = A[0].doubleValue() * B[1].doubleValue();
		double A2B1 = A[1].doubleValue() * B[0].doubleValue();
		if (A1B2 == A2B1)
		{
			// check for coincident lines
			if (C[0].doubleValue() == C[1].doubleValue()) return null;
			return null;
		}
		double A1C2 = A[0].doubleValue() * C[1].doubleValue();
		double A2C1 = A[1].doubleValue() * C[0].doubleValue();

		if (A[0].doubleValue() != 0)
		{
			double Y = (A2C1 - A1C2) / (A1B2 - A2B1);
			double X = -(B[0].doubleValue() * Y + C[0].doubleValue()) / A[0].doubleValue();
			return new Point2D.Double(X, Y);
		}
		double Y = (A1C2 - A2C1) / (A2B1 - A1B2);
		double X = -(B[1].doubleValue() * Y + C[1].doubleValue()) / A[1].doubleValue();
		return new Point2D.Double(X, Y);
	}

	/**
	 * Method to calculate the equation of a line (Ax + By + C = 0)
	 * Inputs:  sx, sy  - Start point
	 *          ex, ey  - End point
	 *          px, py  - Point line should pass through
	 * Outputs: A, B, C - constants for the line
	 */
	private void equationOfALine(double sx, double sy, double ex, double ey, GenMath.MutableDouble A, GenMath.MutableDouble B, GenMath.MutableDouble C)
	{
		if (sx == ex)
		{
			A.setValue(1.0);
			B.setValue(0.0);
			C.setValue(-ex);
		} else if (sy == ey)
		{
			A.setValue(0.0);
			B.setValue(1.0);
			C.setValue(-ey);
		} else
		{
			// let B = 1 then
			B.setValue(1.0);
			if (sx != 0.0)
			{
				// Ax1 + y1 + C = 0    =>    A = -(C + y1) / x1
				// C = -Ax2 - y2       =>    C = (Cx2 + y1x2) / x1 - y2   =>   Cx1 - Cx2 = y1x2 - y2x1
				// C = (y2x1 - y1x2) / (x2 - x1)
				C.setValue((ey * sx - sy * ex) / (ex - sx));
				A.setValue(-(C.doubleValue() + sy) / sx);
			} else
			{
				C.setValue((sy * ex - ey * sx) / (sx - ex));
				A.setValue(-(C.doubleValue() + ey) / ex);
			}
		}
	}

	private void nameEDIFArc(ArcInst ai)
	{
		if (isArray)
		{
			// name the bus
			if (netReference.length() > 0)
			{
				ai.newVar("EDIF_name", netReference);
			}

			// a typical foreign array bus will have some range value appended
			// to the name. This will cause some odd names with duplicate values
			StringBuffer aName = new StringBuffer();
			for (int iX = 0; iX < arrayXVal; iX++)
			{
				for (int iY = 0; iY < arrayYVal; iY++)
				{
					if (aName.length() > 0) aName.append(',');
					if (arrayXVal > 1)
					{
						if (arrayYVal > 1)
							aName.append(netName + "[" + iX + "," + iY + "]");
						else
							aName.append(netName + "[" + iX + "]");
					}
					else aName.append(netName + "[" + iY + "]");
				}
			}
			putNameOnArcOnce(ai, convertParens(aName.toString()));
		} else
		{
			if (netReference.length() > 0)
			{
				ai.newVar("EDIF_name", netName);
			}
			if (netName.length() > 0)
			{
				// set name of arc but don't display name
				putNameOnArcOnce(ai, convertParens(netName));
			}
		}
	}

	private void putNameOnArcOnce(ArcInst ai, String name)
	{
		if (namedArcs != null)
		{
			Map<String,List<ArcInst>> cellArcNames = namedArcs.get(curCell);
			if (cellArcNames == null)
			{
				cellArcNames = new HashMap<String,List<ArcInst>>();
				namedArcs.put(curCell, cellArcNames);
			}
			List<ArcInst> arcsWithName = cellArcNames.get(name);
			if (arcsWithName == null)
			{
				arcsWithName = new ArrayList<ArcInst>();
				cellArcNames.put(name, arcsWithName);
			}
			arcsWithName.add(ai);
		}
	}

	/**
	 * Method to locate the nodeinstance and portproto corresponding to
	 * to a direct intersection with the given point.
	 * inputs:
	 * cell - cell to search
	 * x, y  - the point to exam
	 * ap    - the arc used to connect port (must match pp)
	 */
	private List<PortInst> findEDIFPort(Cell cell, double x, double y, ArcProto ap)
	{
		List<PortInst> ports = new ArrayList<PortInst>();
		PortInst bestPi = null;
		Point2D pt = new Point2D.Double(x, y);
		double bestDist = Double.MAX_VALUE;
		ArcInst ai = null;
		for(Iterator<RTBounds> sea = cell.searchIterator(new Rectangle2D.Double(x, y, 0, 0)); sea.hasNext(); )
		{
			RTBounds geom = sea.next();

			if (geom instanceof NodeInst)
			{
				// now locate a portproto
				NodeInst ni = (NodeInst)geom;
				if (offpageNodes != null && offpageNodes.contains(ni)) continue;
				for(Iterator<PortInst> it = ni.getPortInsts(); it.hasNext(); )
				{
					PortInst pi = it.next();
					Poly poly = pi.getPoly();
					if (poly.isInside(pt))
					{
						// check if port connects to arc ...*/
						if (pi.getPortProto().getBasePort().connectsTo(ap))
							ports.add(pi);
					} else
					{
						double dist = pt.distance(new Point2D.Double(poly.getCenterX(), poly.getCenterY()));
						if (bestPi == null || dist < bestDist) { bestDist = dist;   bestPi = pi; }
					}
				}
			} else
			{
				// only accept valid wires
				ArcInst oAi = (ArcInst)geom;
				if (oAi.getProto().getTechnology() == Schematics.tech())
				{
					// make sure the point is really on the arc
					if (GenMath.distToLine(oAi.getHeadLocation(), oAi.getTailLocation(), pt) == 0)
						ai = oAi;
				}
			}
		}

		if (ports.size() == 0 && ai != null)
		{
			// direct hit on an arc, verify connection
			ArcProto nAp = ai.getProto();
			PrimitiveNode np = nAp.findPinProto();
			if (np == null) return ports;
			PortProto pp = np.getPort(0);
			if (!pp.getBasePort().connectsTo(ap)) return ports;

			// try to split arc (from us_getnodeonarcinst)*/
			// break is at (prefx, prefy): save information about the arcinst
			PortInst fPi = ai.getHeadPortInst();
			Point2D fPt = ai.getHeadLocation();
			PortInst tPi = ai.getTailPortInst();
			Point2D tPt = ai.getTailLocation();

			// create the splitting pin
			NodeInst ni = placePin(np, x, y, np.getDefWidth(), np.getDefHeight(), Orientation.IDENT, cell);
			if (ni == null)
			{
				System.out.println("Cannot create splitting pin");
				return ports;
			}

			// set the node, and port
			PortInst pi = ni.findPortInstFromProto(pp);
			ports.add(pi);

			// create the two new arcinsts
			ArcInst ar1 = ArcInst.makeInstance(nAp, fPi, pi, fPt, pt, null);
			ArcInst ar2 = ArcInst.makeInstance(nAp, pi, tPi, pt, tPt, null);
			if (ar1 == null || ar2 == null)
			{
				System.out.println("Error creating the split arc parts");
				return ports;
			}
			ar1.copyPropertiesFrom(ai);
			if (ai.isHeadNegated()) ar1.setHeadNegated(true);
			if (ai.isTailNegated()) ar2.setTailNegated(true);

			// delete the old arcinst
			ai.kill();
		}
		return ports;
	}

	private void checkBusNames(ArcInst ai, String base, HashSet<ArcInst> seenArcs)
	{
		if (ai.getProto() != Schematics.tech().bus_arc)
		{
			// verify the name
			String aName = ai.getName();
			{
				if (aName.startsWith(base))
				{
					if (aName.length() > base.length() && TextUtils.isDigit(aName.charAt(base.length())))
					{
						String newName = base + "[" + aName.substring(base.length()) + "]";
						putNameOnArcOnce(ai, newName);
					}
				}
			}
		}
		seenArcs.add(ai);
		for (int i = 0; i < 2; i++)
		{
			NodeInst ni = ai.getPortInst(i).getNodeInst();
			if (ni.getFunction() == PrimitiveNode.Function.PIN)
			{
				// scan through this nodes portarcinst's
				for(Iterator<Connection> it = ni.getConnections(); it.hasNext(); )
				{
					Connection oCon = it.next();
					ArcInst pAi = oCon.getArc();
					if (!seenArcs.contains(pAi))
						checkBusNames(pAi, base, seenArcs);
				}
			}
		}
	}

	private void doPoly()
	{
		if (curPoints.size() == 0) return;

		// get the bounds of the poly
		Point2D p0 = curPoints.get(0);
		double lX = p0.getX();
		double lY = p0.getY();
		double hX = lX;
		double hY = lY;
		for(int i=1; i<curPoints.size(); i++)
		{
			Point2D point = curPoints.get(i);
			if (lX > point.getX()) lX = point.getX();
			if (hX < point.getX()) hX = point.getX();
			if (lY > point.getY()) lY = point.getY();
			if (hY < point.getY()) hY = point.getY();
		}

		NodeProto np = curFigureGroup;
		if (curFigureGroup == null || curFigureGroup == Artwork.tech().boxNode)
		{
			if (curKeyword == KPOLYGON) np = Artwork.tech().closedPolygonNode; else
				np = Artwork.tech().openedPolygonNode;
		}
		double sX = hX - lX;
		double sY = hY - lY;
		double cX = (hX + lX) / 2;
		double cY = (hY + lY) / 2;
		double yPos = cY;
		if (curCellPage > 0) yPos += (curCellPage-1) * Cell.FrameDescription.MULTIPAGESEPARATION;
		NodeInst ni = NodeInst.makeInstance(np, new Point2D.Double(cX, yPos), sX, sY,
			curCell, curOrientation, null, 0);
		if (ni == null)
		{
			System.out.println("Error, line " + lineReader.getLineNumber() + ": could not create polygon");
			errorCount++;
		} else
		{
			EPoint [] trace = new EPoint[curPoints.size()];
			for(int i=0; i<curPoints.size(); i++)
			{
				Point2D point = curPoints.get(i);
				trace[i] = new EPoint(point.getX(), point.getY());
			}

			// store the trace information
			ni.setTrace(trace);
		}
		freePointList();
	}

	/**
	 * Method to fix symbols that start with &<number> by removing the ampersand.
	 * @param inStr the original symbol.
	 * @return the fixed symbol.
	 */
	private String fixLeadingAmpersand(String inStr)
	{
		if (inStr.startsWith("&"))
		{
			if (inStr.length() > 1 && Character.isDigit(inStr.charAt(1)))
				return inStr.substring(1);
		}
		return inStr;
	}

	private String fixAngleBracketBusses(String busName)
	{
		int openAngle = busName.indexOf('<');
		if (openAngle >= 0)
		{
			int closeAngle = busName.indexOf('>', openAngle);
			if (closeAngle >= 0)
			{
				String busPart = busName.substring(openAngle+1, closeAngle);
				if (busPart.startsWith("*")) busPart = ""; else
					busPart = "[" + busPart + "]";
				busName = busName.substring(0, openAngle) + busPart + busName.substring(closeAngle+1);
			}
		}
		return busName;
	}

	/**
	 * Method to find a cell in the current library.
	 * @param name the name of the cell.
	 * @param view the view name of the cell.
	 * @return the cell if it exists; null if not.
	 */
	private Cell findCellInLibrary(String name, String view)
	{
		for(Iterator<Cell> it = curLibrary.getCells(); it.hasNext(); )
		{
			Cell cell = it.next();
			if (cell.getName().equalsIgnoreCase(name) &&
				cell.getView().getAbbreviation().equalsIgnoreCase(view)) return cell;
		}
		return null;
	}

	/**
	 * Method to create a new cell.
	 * @param libName the name of the library in which the cell will be created
	 * (null to use the current library).
	 * @param name the name of the cell.
	 * @param view the view name to create.
	 * @return the newly created cell (or existing one if allowed).
	 */
	private Cell createCell(String libName, String name, String view)
	{
		Library lib = curLibrary;
		if (libName != null)
		{
			Library namedLib = Library.findLibrary(libName);
			if (namedLib != null) lib = namedLib;
		}
		for(Iterator<Cell> it = lib.getCells(); it.hasNext(); )
		{
			Cell cell = it.next();
			if (cell.getName().equalsIgnoreCase(name) &&
				cell.getView().getAbbreviation().equalsIgnoreCase(view)) return cell;
		}
		Cell proto = null;
		if (proto == null)
		{
			String cName = name;
			if (view.length() > 0) cName += "{" + view + "}";
			proto = Cell.makeInstance(lib, cName);
			if (proto != null)
			{
				if (view.equals("ic")) proto.setWantExpanded();
				builtCells.add(proto);
			}
		}
		return proto;
	}

	/**************************************** PARSING TABLE ****************************************/

	private class EDIFKEY
	{
		/** the name of the keyword */	private String name;
		/** edif state */				private EDIFKEY [] stateArray;

		private EDIFKEY(String name)
		{
			this.name = name;
			edifKeys.put(TextUtils.canonicString(name), this);
		}

		protected void push() throws IOException {}

		protected void pop() throws IOException {}
	}

	private EDIFKEY KUNKNOWN = new EDIFKEY("");
	private EDIFKEY KINIT = new EDIFKEY("");
	private EDIFKEY KANNOTATE = new EDIFKEY("annotate");
	private EDIFKEY KARC = new KeyArc();
	private EDIFKEY KARRAY = new KeyArray();
	private EDIFKEY KAUTHOR = new EDIFKEY ("author");
	private EDIFKEY KBOOLEAN = new EDIFKEY ("boolean");
	private EDIFKEY KBORDERPATTERN = new EDIFKEY ("borderpattern");
	private EDIFKEY KBOUNDINGBOX = new KeyBoundingBox();
	private EDIFKEY KCELL = new KeyCell();
	private EDIFKEY KCELLREF = new KeyCellRef();
	private EDIFKEY KCELLTYPE = new EDIFKEY("cellType");
	private EDIFKEY KCIRCLE = new KeyCircle();
	private EDIFKEY KCOLOR = new EDIFKEY("color");
	private EDIFKEY KCOMMENT = new EDIFKEY("comment");
	private EDIFKEY KCOMMENTGRAPHICS = new EDIFKEY("commentGraphics");
	private EDIFKEY KCONNECTLOCATION = new EDIFKEY("connectLocation");
	private EDIFKEY KCONTENTS = new EDIFKEY("contents");
	private EDIFKEY KCORNERTYPE = new KeyCornerType();
	private EDIFKEY KCURVE = new EDIFKEY("curve");
	private EDIFKEY KDATAORIGIN = new EDIFKEY("dataOrigin");
	private EDIFKEY KDCFANOUTLOAD = new EDIFKEY("dcFanoutLoad");
	private EDIFKEY KDCMAXFANOUT = new EDIFKEY("dcMaxFanout");
	private EDIFKEY KDELTA = new KeyDelta();
	private EDIFKEY KDESIGN = new KeyDesign();
	private EDIFKEY KDESIGNATOR = new EDIFKEY("designator");
	private EDIFKEY KDIRECTION = new KeyDirection();
	private EDIFKEY KDISPLAY = new KeyDisplay();
	private EDIFKEY KDOT = new KeyDot();
	private EDIFKEY KSCALEDINTEGER = new EDIFKEY("e");
	private EDIFKEY KEDIF = new EDIFKEY("edif");
	private EDIFKEY KEDIFLEVEL = new EDIFKEY("edifLevel");
	private EDIFKEY KEDIFVERSION = new EDIFKEY("edifVersion");
	private EDIFKEY KENDTYPE = new KeyEndType();
	private EDIFKEY KEXTERNAL = new KeyExternal();
	private EDIFKEY KFABRICATE = new KeyFabricate();
	private EDIFKEY KFALSE = new KeyFalse();
	private EDIFKEY KFIGURE = new KeyFigure();
	private EDIFKEY KFIGUREGROUP = new EDIFKEY("figureGroup");
	private EDIFKEY KFIGUREGROUPOVERRIDE = new KeyFigureGroupOverride();
	private EDIFKEY KFILLPATTERN = new EDIFKEY("fillpattern");
	private EDIFKEY KGRIDMAP = new EDIFKEY("gridMap");
	private EDIFKEY KINSTANCE = new KeyInstance();
	private EDIFKEY KINSTANCEREF = new KeyInstanceRef();
	private EDIFKEY KINTEGER = new KeyInteger();
	private EDIFKEY KINTERFACE = new KeyInterface();
	private EDIFKEY KJOINED = new EDIFKEY("joined");
	private EDIFKEY KJUSTIFY = new KeyJustify();
	private EDIFKEY KKEYWORDDISPLAY = new EDIFKEY("keywordDisplay");
	private EDIFKEY KEDIFKEYLEVEL = new EDIFKEY("keywordLevel");
	private EDIFKEY KEDIFKEYMAP = new EDIFKEY("keywordMap");
	private EDIFKEY KLIBRARY = new KeyLibrary();
	private EDIFKEY KLIBRARYREF = new LibraryRef();
	private EDIFKEY KLISTOFNETS = new EDIFKEY("listOfNets");
	private EDIFKEY KLISTOFPORTS = new EDIFKEY("listOfPorts");
	private EDIFKEY KMEMBER = new KeyMember();
	private EDIFKEY KNAME = new KeyName();
	private EDIFKEY KNET = new KeyNet();
	private EDIFKEY KNETBUNDLE = new KeyNetBundle();
	private EDIFKEY KNUMBER = new KeyNumber();
	private EDIFKEY KNUMBERDEFINITION = new EDIFKEY("numberDefinition");
	private EDIFKEY KOPENSHAPE = new KeyOpenShape();
	private EDIFKEY KORIENTATION = new KeyOrientation();
	private EDIFKEY KORIGIN = new EDIFKEY("origin");
	private EDIFKEY KOWNER = new EDIFKEY("owner");
	private EDIFKEY KPAGE = new KeyPage();
	private EDIFKEY KPAGESIZE = new EDIFKEY("pageSize");
	private EDIFKEY KPATH = new KeyPath();
	private EDIFKEY KPATHWIDTH = new KeyPathWidth();
	private EDIFKEY KPOINT = new EDIFKEY("point");
	private EDIFKEY KPOINTLIST = new EDIFKEY("pointList");
	private EDIFKEY KPOLYGON = new KeyPolygon();
	private EDIFKEY KPORT = new KeyPort();
	private EDIFKEY KPORTBUNDLE = new EDIFKEY("portBundle");
	private EDIFKEY KPORTIMPLEMENTATION = new KeyPortImplementation();
	private EDIFKEY KPORTINSTANCE = new EDIFKEY("portInstance");
	private EDIFKEY KPORTLIST = new EDIFKEY("portList");
	private EDIFKEY KPORTREF = new KeyPortRef();
	private EDIFKEY KPROGRAM = new KeyProgram();
	private EDIFKEY KPROPERTY = new KeyProperty();
	private EDIFKEY KPROPERTYDISPLAY = new EDIFKEY("propertyDisplay");
	private EDIFKEY KPT = new KeyPt();
	private EDIFKEY KRECTANGLE = new KeyRectangle();
	private EDIFKEY KRENAME = new KeyRename();
	private EDIFKEY KSCALE = new EDIFKEY("scale");
	private EDIFKEY KSCALEX = new EDIFKEY("scaleX");
	private EDIFKEY KSCALEY = new EDIFKEY("scaleY");
	private EDIFKEY KSHAPE = new EDIFKEY("shape");
	private EDIFKEY KSTATUS = new EDIFKEY("status");
	private EDIFKEY KSTRING = new KeyString();
	private EDIFKEY KSTRINGDISPLAY = new KeyStringDisplay();
	private EDIFKEY KSYMBOL = new KeySymbol();
	private EDIFKEY KTECHNOLOGY = new KeyTechnology();
	private EDIFKEY KTEXTHEIGHT = new KeyTextHeight();
	private EDIFKEY KTIMESTAMP = new EDIFKEY("timestamp");
	private EDIFKEY KTRANSFORM = new KeyTransform();
	private EDIFKEY KTRUE = new KeyTrue();
	private EDIFKEY KUNIT = new KeyUnit();
	private EDIFKEY KUSERDATA = new EDIFKEY("userData");
	private EDIFKEY KVERSION = new EDIFKEY("version");
	private EDIFKEY KVIEW = new KeyView();
	private EDIFKEY KVIEWREF = new ViewRef();
	private EDIFKEY KVIEWTYPE = new KeyViewType();
	private EDIFKEY KVISIBLE = new EDIFKEY("visible");
	private EDIFKEY KWRITTEN = new EDIFKEY("written");

	private class KeyArc extends EDIFKEY
	{
		private KeyArc() { super("arc"); }
		protected void pop()
		{
			// set array values
			if (curPoints.size() >= 3)
			{
				double [] x = new double[3];
				double [] y = new double[3];
				Point2D p0 = curPoints.get(0);
				Point2D p1 = curPoints.get(1);
				Point2D p2 = curPoints.get(2);
				x[0] = p0.getX();   y[0] = p0.getY();
				x[1] = p1.getX();   y[1] = p1.getY();
				x[2] = p2.getX();   y[2] = p2.getY();
				makeArc(x, y);
			}
			freePointList();
		}
	}

	private class KeyArray extends EDIFKEY
	{
		private KeyArray() { super("array"); }
		protected void push()
			throws IOException
		{
			// note array is a special process integer value function
			isArray = true;
			arrayXVal = arrayYVal = 0;
			deltaPointXX = deltaPointXY = 0;
			deltaPointYX = deltaPointYY = 0;
			deltaPointsSet = false;
			if (checkName())
			{
				if (keyStack[keyStackDepth-1] == KCELL)
				{
					cellName = cellReference = fixLeadingAmpersand(objectName);
				} else if (keyStack[keyStackDepth-1] == KPORT)
				{
					portName = portReference = fixLeadingAmpersand(objectName);
				} else if (keyStack[keyStackDepth-1] == KINSTANCE)
				{
					instanceName = instanceReference = objectName;
				} else if (keyStack[keyStackDepth-1] == KNET)
				{
					netReference = netName = fixLeadingAmpersand(objectName);
				} else if (keyStack[keyStackDepth-1] == KPROPERTY)
				{
					propertyReference = objectName;
				}
			}
		}

		protected void pop()
		{
			if (arrayXVal == 0) arrayXVal = 1;
			if (arrayYVal == 0) arrayYVal = 1;
		}
	}

	private class KeyBoundingBox extends EDIFKEY
	{
		private KeyBoundingBox() { super("boundingBox"); }
		protected void push()
		{
			curFigureGroup = Artwork.tech().openedDottedPolygonNode;
		}

		protected void pop()
		{
			curFigureGroup = null;
		}
	}

	private class KeyCell extends EDIFKEY
	{
		private KeyCell() { super("cell"); }
		protected void push()
			throws IOException
		{
			activeView = VNULL;
			objectName = "";   originalName = "";
			pageNumber = 0;
			sheetXPos = sheetYPos = -1;
			if (checkName())
			{
				cellName = cellReference = fixLeadingAmpersand(objectName);
			}
		}
	}

	private class KeyCellRef extends EDIFKEY
	{
		private KeyCellRef() { super("cellRef"); }
		protected void push()
			throws IOException
		{
			cellRef = fixLeadingAmpersand(getToken((char)0));
			libraryRef = null;
		}

		protected void pop()
		{
			// get the name of the cell
			String aName = cellRef;
			cellRefProtoTechBits = 0;
			cellRefProtoRotation = 0;
			cellRefOffsetX = cellRefOffsetY = 0;
			String view = "lay";
			if (activeView != VMASKLAYOUT)
			{
				if (keyStack[keyStackDepth - 1] == KDESIGN) view = "sch"; else
					view = "ic";
			}
			if (curVendor == EVVIEWLOGIC && aName.equalsIgnoreCase("SPLITTER"))
			{
				cellRefProto = null;
				return;
			}

			// look for an equivalent primitive
			EDIFEquiv.NodeEquivalence ne = equivs.getNodeEquivalence(libraryRef, cellRef, viewRef);
			if (ne != null && ne.np != null)
			{
				mappedNodes.put(instanceName, ne);
				cellRefProto = ne.np;
				cellRefProtoTechBits = 0;
				cellRefProtoRotation = ne.rotation;

				// determine the offset
				cellRefOffsetX = ne.xOffset;
				cellRefOffsetY = ne.yOffset;
				if (cellRefProto instanceof PrimitiveNode) {
					Technology tech = cellRefProto.getTechnology();
					if (tech instanceof Schematics) {
						cellRefProtoTechBits = Schematics.getPrimitiveFunctionBits(ne.function);
					}
				}
				return;
			}

			// look for this cell name in the cell list
			NameEntry nt = cellTable.get(aName);
			if (nt == null)
			{
				String alternateName = renamedObjects.get(aName);
				if (alternateName != null)
					nt = cellTable.get(alternateName);
			}
			if (nt != null)
			{
				aName = nt.replace;
			} else
			{
				System.out.println("could not find cellRef <" + aName + ">");
			}

			// create cell if not already there
			Cell proto = createCell(libraryRef, aName, view);
			if (proto == null)
				System.out.println("Error, cannot create cell "+aName+" in library "+libraryRef);

			// set the parent
			cellRefProto = proto;
		}
	}

	private class KeyCircle extends EDIFKEY
	{
		private KeyCircle() { super("circle"); }
		protected void push()
		{
			freePointList();
			curOrientation = OR0;
		}

		protected void pop()
		{
			if (curPoints.size() == 2)
			{
				Point2D p0 = curPoints.get(0);
				Point2D p1 = curPoints.get(1);
				double lX = Math.min(p0.getX(), p1.getX());
				double hX = Math.max(p0.getX(), p1.getX());
				double lY = Math.min(p0.getY(), p1.getY());
				double hY = Math.max(p0.getY(), p1.getY());
				if (lX == hX)
				{
					lX -= (hY - lY) / 2;
					hX += (hY - lY) / 2;
				} else
				{
					lY -= (hX - lX) / 2;
					hY += (hX - lX) / 2;
				}
				double sX = hX - lX;
				double sY = hY - lY;

				// create the node instance
				double cX = (hX + lX) / 2;
				double cY = (hY + lY) / 2;
				if (curCellPage > 0) cY += (curCellPage-1) * Cell.FrameDescription.MULTIPAGESEPARATION;
				NodeInst ni = NodeInst.makeInstance(Artwork.tech().circleNode, new Point2D.Double(cX, cY),
					sX, sY, curCell, curOrientation, null, 0);
				if (ni == null)
				{
					System.out.println("Error, line " + lineReader.getLineNumber() + ": could not create circle");
					errorCount++;
				}
			}
			freePointList();
		}
	}

	private class KeyCornerType extends EDIFKEY
	{
		private KeyCornerType() { super("cornerType"); }
		protected void push()
			throws IOException
		{
			// get the endtype
			getToken((char)0);
		}
	}

	private class KeyDelta extends EDIFKEY
	{
		private KeyDelta() { super("delta"); }
		protected void push()
		{
			deltaPointXX = deltaPointXY = 0;
			deltaPointYX = deltaPointYY = 0;
			deltaPointsSet = false;
		}
	}

	private class KeyDesign extends EDIFKEY
	{
		private KeyDesign() { super("design"); }
		protected void push()
			throws IOException
		{
			// ignore the name of the cell
			getToken((char)0);
		}

		protected void pop()
		{
			if (cellRefProto != null)
			{
				Job.getUserInterface().setCurrentCell(curLibrary, (Cell)cellRefProto);
			}
		}
	}

	private class KeyDirection extends EDIFKEY
	{
		private KeyDirection() { super("direction"); }
		protected void push()
			throws IOException
		{
			// get the direction
			String aName = getToken((char)0);

			if (aName.equalsIgnoreCase("INPUT")) curDirection = PortCharacteristic.IN; else
				if (aName.equalsIgnoreCase("INOUT")) curDirection = PortCharacteristic.BIDIR; else
					if (aName.equalsIgnoreCase("OUTPUT")) curDirection = PortCharacteristic.OUT;
		}
	}

	private class KeyDisplay extends EDIFKEY
	{
		private KeyDisplay() { super("display"); }
		protected void push()
			throws IOException
		{
			makeFigure();
		}
	}

	private class KeyDot extends EDIFKEY
	{
		private KeyDot() { super("dot"); }
		protected void push()
		{
			freePointList();
			curOrientation = OR0;
		}

		protected void pop()
		{
			if (curGeometryType == GPIN)
			{
				// inside a PortImplementation
				EDIFPort ePort = null;
				for (EDIFPort e = portsListHead; e != null; e = e.next)
				{
					if (e.reference.equalsIgnoreCase(objectName)) { ePort = e;   break; }
				}
				if (ePort != null)
				{
					String pName = ePort.name;
					Export ppt = (Export)curCell.findPortProto(pName);
					if (ppt == null)
					{
						// create a internal wire port using the pin-proto, and create the port and export
						Point2D p0 = curPoints.get(0);
						double xPos = p0.getX();
						double yPos = p0.getY();
						if (curCellPage > 0) yPos += (curCellPage-1) * Cell.FrameDescription.MULTIPAGESEPARATION;
						NodeInst ni = placePin(cellRefProto, xPos, yPos, cellRefProto.getDefWidth(), cellRefProto.getDefHeight(), curOrientation, curCell);
						if (ni == null)
						{
							System.out.println("Error, line " + lineReader.getLineNumber() + ": could not create pin");
							errorCount++;
						}
						PortInst pi = ni.findPortInstFromProto(defaultIconPort);
						ppt = makeExport(curCell, pi, pName);
						if (ppt == null)
						{
							System.out.println("Error, line " + lineReader.getLineNumber() + ": could not create port <" + pName + ">");
							errorCount++;
						} else
						{
							// set the direction
							ppt.setCharacteristic(ePort.direction);
						}
					}
				}
			} else
			{
				// create the node instance
				Point2D p0 = curPoints.get(0);
				double xPos = p0.getX();
				double yPos = p0.getY();
				if (curCellPage > 0) yPos += (curCellPage-1) * Cell.FrameDescription.MULTIPAGESEPARATION;
				NodeInst ni = NodeInst.makeInstance(curFigureGroup != null ? curFigureGroup : Artwork.tech().boxNode,
					new Point2D.Double(xPos, yPos), 0, 0, curCell);
				if (ni == null)
				{
					System.out.println("Error, line " + lineReader.getLineNumber() + ": could not create rectangle");
					errorCount++;
				}
			}
			freePointList();
		}
	}

	private class KeyEndType extends EDIFKEY
	{
		private KeyEndType() { super("endType"); }
		protected void push()
			throws IOException
		{
			// get the endtype
			String type = getToken((char)0);

			if (type.equalsIgnoreCase("EXTEND")) extendEnd = true;
		}
	}

	private class KeyExternal extends EDIFKEY
	{
		private KeyExternal() { super("external"); }
		protected void push()
			throws IOException
		{
			// ignore the name of the library
			getToken((char)0);
		}
	}

	private class KeyFabricate extends EDIFKEY
	{
		private KeyFabricate() { super("fabricate"); }
		protected void push()
			throws IOException
		{
			NameEntry nt = new NameEntry();
			nameEntryList.add(nt);

			// first get the original and replacement layers
			nt.original = getToken((char) 0);
			nt.replace = getToken((char) 0);
			nt.textHeight = 0;
			nt.justification = TextDescriptor.Position.DOWNRIGHT;
			nt.visible = true;
		}
	}

	private class KeyFalse extends EDIFKEY
	{
		private KeyFalse() { super("false"); }
		protected void push()
		{
			// check previous keyword
			if (keyStackDepth > 1 && keyStack[keyStackDepth-1] == KVISIBLE)
			{
				textVisible = false;
				if (curNameEntry != null)
					curNameEntry.visible = false;
			}
		}
	}

	private class KeyFigure extends EDIFKEY
	{
		private KeyFigure() { super("figure"); }
		protected void push()
			throws IOException
		{
			makeFigure();
		}

		protected void pop()
		{
			curNameEntry = null;
			textVisible = true;
			textJustification = TextDescriptor.Position.DOWNRIGHT;
			textHeight = 0;
		}
	}

	private class KeyFigureGroupOverride extends EDIFKEY
	{
		private KeyFigureGroupOverride() { super("figureGroupOverride"); }
		protected void push()
			throws IOException
		{
			// get the layer name
			String layer = getToken((char)0);

			// now look for this layer in the list of layers
			for(NameEntry nt : nameEntryList)
			{
				if (nt == null) continue;
				if (nt.original.equalsIgnoreCase(layer))
				{
					// found the layer
					curFigureGroup = nt.node;
					textHeight = nt.textHeight;
					textJustification = nt.justification;
					textVisible = nt.visible;
					return;
				}
			}

			// insert and resort the list
			NameEntry nt = new NameEntry();
			nameEntryList.add(nt);
			nt.original = layer;

			nt.replace = nt.original;
			curFigureGroup = nt.node = Artwork.tech().boxNode;
			textHeight = nt.textHeight = 0;
			textJustification = nt.justification = TextDescriptor.Position.DOWNRIGHT;
			textVisible = nt.visible = true;
		}
	}

	private class KeyInstance extends EDIFKEY
	{
		private KeyInstance() { super("instance"); }
		protected void push()
			throws IOException
		{
			// set the current geometry type
			freePointList();
			cellRefProto = null;
			curGeometryType = GINSTANCE;
			curOrientation = OR0;
			isArray = false;
			arrayXVal = arrayYVal = 1;
			objectName = "";   originalName = "";
			instanceReference = "";
			curNode = null;
			if (checkName())
			{
				instanceReference = objectName;
				instanceName = objectName;
			}
		}

		private int sheetOffset = 0;
		protected void pop()
			throws IOException
		{
			if (activeView == VNETLIST)
			{
				for (int iX = 0; iX < arrayXVal; iX++)
				{
					for (int iY = 0; iY < arrayYVal; iY++)
					{
						// create this instance in the current sheet
						double width = cellRefProto.getDefWidth();
						double height = cellRefProto.getDefHeight();
						width = (width + INCH - 1) / INCH;
						height = (height + INCH - 1) / INCH;

						// verify room for the icon
						if (sheetXPos != -1)
						{
							if ((sheetYPos + (height + 1)) >= SHEETHEIGHT)
							{
								sheetYPos = 1;
								if ((sheetXPos += sheetOffset) >= SHEETWIDTH)
									sheetXPos = sheetYPos = -1; else
										sheetOffset = 2;
							}
						}
						if (sheetXPos == -1)
						{
							// create the new page
							curCellPage = ++pageNumber;
							if (curCellPage == 1)
							{
								curCell = createCell(null, cellName, "sch");
								if (curCell == null) throw new IOException("Error creating cell");
								curCell.setMultiPage(true);
								curCell.newVar(User.FRAME_SIZE, "d");
							} else
							{
								String nodeName = cellName + "{sch}";
								curCell = curLibrary.findNodeProto(nodeName);
								if (curCell == null) throw new IOException("Error finding cell");
							}
							sheetXPos = sheetYPos = 1;
							sheetOffset = 2;
						}

						// create this instance
						// find out where true center moves
						double cX = ((sheetXPos - (SHEETWIDTH / 2)) * INCH);
						double cY = ((sheetYPos - (SHEETHEIGHT / 2)) * INCH);
						if (curCellPage > 0) cY += (curCellPage-1) * Cell.FrameDescription.MULTIPAGESEPARATION;
						Orientation orient = curOrientation.concatenate(Orientation.fromAngle(cellRefProtoRotation*10));
						NodeInst ni = NodeInst.makeInstance(cellRefProto, new Point2D.Double(cX+cellRefOffsetX, cY+cellRefOffsetY),
							cellRefProto.getDefWidth(), cellRefProto.getDefHeight(), curCell, orient, null, cellRefProtoTechBits);
						curNode = ni;
						if (ni == null)
						{
							System.out.println("Error, line " + lineReader.getLineNumber() + ": could not create instance");
							errorCount++;
							break;
						}

						if (cellRefProto instanceof Cell)
						{
							if (((Cell)cellRefProto).isWantExpanded())
								ni.setExpanded();
						}

						// update the current position
						if ((width + 2) > sheetOffset)
							sheetOffset = (int)width + 2;
						if ((sheetYPos += (height + 1)) >= SHEETHEIGHT)
						{
							sheetYPos = 1;
							if ((sheetXPos += sheetOffset) >= SHEETWIDTH)
								sheetXPos = sheetYPos = -1; else
									sheetOffset = 2;
						}

						// name the instance
						if (instanceReference.length() > 0)
						{
							// if single element or array with no offset
							// construct the representative extended EDIF name (includes [...])
							String nodeName = instanceReference;
							if ((arrayXVal > 1 || arrayYVal > 1) &&
								(deltaPointXX != 0 || deltaPointXY != 0 || deltaPointYX != 0 || deltaPointYY != 0))
							{
								// if array in the x dimension
								if (arrayXVal > 1)
								{
									if (arrayYVal > 1)
										nodeName = instanceReference + "[" + iX + "," + iY + "]";
									else
										nodeName = instanceReference + "[" + iX + "]";
								} else if (arrayYVal > 1)
									nodeName = instanceReference + "[" + iY + "]";
							}

							// check for array element descriptor
							if (arrayXVal > 1 || arrayYVal > 1)
							{
								// array descriptor is of the form index:index:range index:index:range
								String baseName = iX + ":" + ((deltaPointXX == 0 && deltaPointYX == 0) ? arrayXVal-1:iX) + ":" + arrayXVal +
									" " + iY + ":" + ((deltaPointXY == 0 && deltaPointYY == 0) ? arrayYVal-1:iY) + ":" + arrayYVal;
								ni.newVar("EDIF_array", baseName);
							}

							/* now set the name of the component (note that Electric allows any string
							 * of characters as a name, this name is open to the user, for ECO and other
							 * consistancies, the EDIF_name is saved on a variable)
							 */
							if (instanceReference.equalsIgnoreCase(instanceName))
							{
								ni.setName(convertParens(nodeName));
							} else
							{
								// now add the original name as the displayed name (only to the first element)
								if (iX == 0 && iY == 0)
								{
									ni.setName(convertParens(instanceName));
								}

								// now save the EDIF name (not displayed)
								ni.newVar("EDIF_name", nodeName);
							}
						}
					}
				}
			}

			// move the property list to the free list
			for (EDIFProperty property = propertiesListHead; property != null; property = property.next)
			{
				if (curNode != null)
				{
					String varName = property.name;
					Object varValue = property.val;
					String varNameLookup = varName;
					if (varNameLookup.startsWith("ATTR_")) varNameLookup = varNameLookup.substring(5);
					if (isAcceptedParameter(varNameLookup))
					{
						EDIFEquiv.VariableEquivalence ve = equivs.getExternalVariableEquivalence(varNameLookup);
						if (ve != null)
						{
							varName = "ATTR_" + ve.elecVarName;
							if (ve.appendElecOutput.length() > 0)
							{
								String varValueString = varValue.toString();
								if (varValueString.endsWith(ve.appendElecOutput))
								{
									varValue = varValueString.substring(0, varValueString.length() - ve.appendElecOutput.length());
								}
							}
							if (ve.scale != 1)
							{
								String varValueString = varValue.toString();
								double newValue = TextUtils.atof(varValueString) / ve.scale;
								varValue = Double.toString(newValue);
							}
						}
						TextDescriptor td = TextDescriptor.getNodeTextDescriptor().withDisplay(true);
						Cell parent = (Cell)curNode.getProto();
						for(Iterator<Variable> it = parent.getParameters(); it.hasNext(); )
						{
							Variable var = it.next();
							if (var.getKey().getName().equals(varName))
							{
								td = td.withOff(var.getXOff(), var.getYOff());
								break;
							}
						}
						curNode.newVar(Variable.newKey(varName), varValue, td);
					}
				}
			}
			propertiesListHead = null;
			instanceReference = "";
			curNode = null;
			freeSavedPointList();
		}
	}

	private class KeyInstanceRef extends EDIFKEY
	{
		private KeyInstanceRef() { super("instanceRef"); }
		protected void push()
			throws IOException
		{
			instanceReference = "";
			if (checkName())
			{
				instanceReference = objectName;
			}
		}
	}

	private class KeyInteger extends EDIFKEY
	{
		private KeyInteger() { super("integer"); }
		protected void push()
			throws IOException
		{
			String value = getToken((char)0);

			propertyValue = new Integer(TextUtils.atoi(value));
		}
	}

	private class KeyInterface extends EDIFKEY
	{
		private KeyInterface() { super("interface"); }
		protected void push()
			throws IOException
		{
			// create schematic page 1 to represent all I/O for this schematic; locate this in the list of cells
			curCell = createCell(null, cellName, activeView == VGRAPHIC ? "ic" : "sch");
			if (curCell == null) throw new IOException("Error creating cell");
			curCellPage = 0;
		}

		protected void pop()
		{
			if (activeView == VNETLIST)
			{
				// create a black-box symbol at the current scale
				Cell nnp = null;
				try
				{
					ViewChanges.IconParameters ip = new ViewChanges.IconParameters();
					ip.initFromUserDefaults();
					nnp = ip.makeIconForCell(curCell);
				} catch (JobException e)
				{
				}
				if (nnp == null)
				{
					System.out.println("Error, line " + lineReader.getLineNumber() + ": could not create icon <" + curCell.describe(true) + ">");
					errorCount++;
				}
			}
		}
	}

	private class KeyJustify extends EDIFKEY
	{
		private KeyJustify() { super("justify"); }
		protected void push()
			throws IOException
		{
			// get the textheight value of the point
			String val = getToken((char)0);
			if (val.equalsIgnoreCase("UPPERLEFT")) textJustification = TextDescriptor.Position.DOWNRIGHT;
			else if (val.equalsIgnoreCase("UPPERCENTER")) textJustification = TextDescriptor.Position.DOWN;
			else if (val.equalsIgnoreCase("UPPERRIGHT")) textJustification = TextDescriptor.Position.DOWNLEFT;
			else if (val.equalsIgnoreCase("CENTERLEFT")) textJustification = TextDescriptor.Position.RIGHT;
			else if (val.equalsIgnoreCase("CENTERCENTER")) textJustification = TextDescriptor.Position.CENT;
			else if (val.equalsIgnoreCase("CENTERRIGHT")) textJustification = TextDescriptor.Position.LEFT;
			else if (val.equalsIgnoreCase("LOWERLEFT")) textJustification = TextDescriptor.Position.UPRIGHT;
			else if (val.equalsIgnoreCase("LOWERCENTER")) textJustification = TextDescriptor.Position.UP;
			else if (val.equalsIgnoreCase("LOWERRIGHT")) textJustification = TextDescriptor.Position.UPLEFT;
			else
			{
				System.out.println("Warning, line " + lineReader.getLineNumber() + ": unknown keyword <" + val + ">");
				warningCount++;
				return;
			}

			if (curNameEntry != null)
				curNameEntry.justification = textJustification;
		}
	}

	private class KeyLibrary extends EDIFKEY
	{
		private KeyLibrary() { super("library"); }
		protected void push()
			throws IOException
		{
			// get the name of the library
			String libName = getToken((char)0);
			curLibrary = Library.findLibrary(libName);
			if (curLibrary == null)
			{
				curLibrary = Library.newInstance(libName, null);
			}
		}
	}

	private class LibraryRef extends EDIFKEY
	{
		private LibraryRef() { super ("libraryRef"); }
		protected void push() throws IOException {
			// get the name of the library
			libraryRef = getToken((char)0);
		}
	}

	private class KeyMember extends EDIFKEY
	{
		private KeyMember() { super("member"); }
		protected void push()
			throws IOException
		{
			memberXVal = -1; // no member
			memberYVal = -1; // no member
			if (checkName())
			{
				if (keyStack[keyStackDepth-1] == KPORTREF) portReference = objectName; else
					if (keyStack[keyStackDepth-1] == KINSTANCEREF) instanceReference = objectName;
			}
		}

		protected void pop()
		{
			if (memberXVal != -1)
			{
				// adjust the name of the current INSTANCE/NET/PORT(/VALUE)
				if (keyStack[keyStackDepth-1] == KINSTANCEREF)
				{
					instanceReference = getMemberName(instanceReference, memberXVal, memberYVal);
				} else if (keyStack[keyStackDepth-1] == KPORTREF)
				{
					portReference = getMemberName(portReference, memberXVal, memberYVal);
				}
			}
		}

		/**
		 * Method to convert a bus name to an individual member name.
		 * @param name the bus name.
		 * @param ind1 the index of the first entry.
		 * @param ind2 the index of the second entry.
		 * @return the specified member name.
		 */
		private String getMemberName(String name, int ind1, int ind2)
		{
			// get true name of array specification
			String baseName = renamedObjects.get(name);
			if (baseName == null) baseName = name;

			// find first index entry
			int openSq = baseName.indexOf('[');
			if (openSq < 0) return baseName;
			int closeSq = baseName.indexOf(']', openSq);
			if (closeSq < 0) closeSq = baseName.length();
			int comma = baseName.indexOf(',', openSq);
			if (comma < 0) comma = baseName.length();
			int endInd1 = Math.min(closeSq, comma);
			if (endInd1 < 0) return baseName;
			String index1 = baseName.substring(openSq+1, endInd1);
			String index2 = null;
			String restOfLine = baseName.substring(endInd1);

			if (comma >= 0 && comma < baseName.length())
			{
				closeSq = baseName.indexOf(']', comma);
				if (closeSq >= 0)
				{
					index2 = baseName.substring(comma+1, closeSq);
					restOfLine = baseName.substring(closeSq);
				}				
			}

			int [] indices = getIndices(index1);
			if (ind1 >= 0 && ind1 < indices.length)
				baseName = baseName.substring(0, openSq+1) + indices[ind1];
			if (index2 != null)
			{
				int [] indices2 = getIndices(index2);
				if (ind2 >= 0 && ind2 < indices2.length)
					baseName += "," + indices2[ind2];
			}
			baseName += restOfLine;
			return baseName;
		}

		/**
		 * Method to examine an array specification and return all of the indices.
		 * @param index the array specification, for example "1:5" or "4:2".
		 * @return an array of indices, for example [1,2,3,4,5] or [4,3,2].
		 */
		private int [] getIndices(String index)
		{
			List<Integer> indices = new ArrayList<Integer>();
			for(int pos=0; pos<index.length(); )
			{
				if (Character.isWhitespace(index.charAt(pos))) { pos++;   continue; }
				if (Character.isDigit(index.charAt(pos)))
				{
					int val = TextUtils.atoi(index.substring(pos));
					int valEnd = val;
					while (pos < index.length() && Character.isDigit(index.charAt(pos))) pos++;
					while (pos < index.length() && Character.isWhitespace(index.charAt(pos))) pos++;
					if (pos < index.length() && index.charAt(pos) == ':')
					{
						pos++;
						while (pos < index.length() && Character.isWhitespace(index.charAt(pos))) pos++;
						if (Character.isDigit(index.charAt(pos)))
						{
							valEnd = TextUtils.atoi(index.substring(pos));
							while (pos < index.length() && Character.isDigit(index.charAt(pos))) pos++;
							while (pos < index.length() && Character.isWhitespace(index.charAt(pos))) pos++;
						}
					}
					if (valEnd < val)
					{
						for(int i=val; i>=valEnd; i--) indices.add(new Integer(i));
					} else
					{
						for(int i=val; i<=valEnd; i++) indices.add(new Integer(i));
					}
					continue;
				}
				break;
			}
			int [] retInd = new int[indices.size()];
			for(int i=0; i<indices.size(); i++) retInd[i] = indices.get(i);
			return retInd;
		}
	}

	private class KeyName extends EDIFKEY
	{
		private KeyName() { super("name"); }
		protected void push()
			throws IOException
		{
			int kPtr = keyStackDepth - 1;
			if (keyStack[keyStackDepth-1] == KARRAY || keyStack[keyStackDepth-1] == KMEMBER) kPtr = keyStackDepth-2;
			if (checkName())
			{
				if (keyStack[kPtr] == KCELL)
				{
					cellName = cellReference = fixLeadingAmpersand(objectName);
				} else if (keyStack[kPtr] == KPORTIMPLEMENTATION || keyStack[kPtr] == KPORT)
				{
					portName = portReference = fixLeadingAmpersand(objectName);
				} else if (keyStack[kPtr] == KPORTREF)
				{
					portReference = fixLeadingAmpersand(objectName);
				} else if (keyStack[kPtr] == KINSTANCE)
				{
					instanceName = instanceReference = objectName;
				} else if (keyStack[kPtr] == KINSTANCEREF)
				{
					instanceReference = objectName;
				} else if (keyStack[kPtr] == KNET)
				{
					netReference = netName = fixLeadingAmpersand(objectName);
				} else if (keyStack[kPtr] == KPROPERTY)
				{
					propertyReference = objectName;
				}

				// init the point lists
				freePointList();
				curOrientation = OR0;
				textVisible = true;
				textJustification = TextDescriptor.Position.DOWNRIGHT;
				textHeight = 0;
				textString = objectName;
			}
		}

		protected void pop()
		{
			// save the data and break
			freeSavedPointList();
			textString = "";
			saveTextPoints = curPoints;
			curPoints = new ArrayList<Point2D>();
			saveTextHeight = textHeight;
			textHeight = 0;
			saveTextJustification = textJustification;
			textJustification = TextDescriptor.Position.DOWNRIGHT;
			curOrientation = OR0;
			textVisible = true;
		}
	}

	/**
	 * net:  Define a net name followed by edifgbl and local pin list
	 *		(net NAME
	 *			(joined
	 *			(portRef NAME (instanceRef NAME))
	 *			(portRef NAME)))
	 */
	private class KeyNet extends EDIFKEY
	{
		private KeyNet() { super("net"); }
		protected void push()
			throws IOException
		{
			netReference = "";
			objectName = "";   originalName = "";
			curArc = null;
			curNode = null;
			curPort = null;
			isArray = false;
			arrayXVal = arrayYVal = 1;
			if (keyStack[keyStackDepth-2] != KNETBUNDLE) curGeometryType = GNET;
			if (checkName())
			{
				netReference = objectName;
				netName = objectName;
			}
			exportsOnNet = new HashSet<String>();
			arcsOnNet = new ArrayList<ArcInst>();
		}

		protected void pop()
		{
			// move the property list to the free list
			for (EDIFProperty property = propertiesListHead; property != null; property = property.next)
			{
				if (curArc != null)
					curArc.newVar(property.name, property.val);
			}
			propertiesListHead = null;
			netReference = "";
			curArc = null;
			if (curGeometryType != GBUS) curGeometryType = GUNKNOWN;
			freeSavedPointList();
			netPortRefs.clear();

			// make sure all exports are on some arc
			for(String exportName : exportsOnNet)
			{
				ArcInst unNamedArc = null;
				boolean nameFound = false;
				for(ArcInst ai : arcsOnNet)
				{
					String arcName = ai.getName();
					if (arcName.equals(exportName)) nameFound = true;
					if (ai.getNameKey().isTempname()) unNamedArc = ai;
				}
				if (nameFound) continue;
				if (unNamedArc != null)
				{
					unNamedArc.setName(exportName);
				} else
				{
					// two names on one arc: duplicate the arc
					if (arcsOnNet.size() > 0)
					{
						ArcInst ai = arcsOnNet.get(0);
						ArcInst second = ArcInst.newInstanceBase(ai.getProto(), ai.getLambdaBaseWidth(),
							ai.getHeadPortInst(), ai.getTailPortInst(), ai.getHeadLocation(), ai.getTailLocation(),
							exportName, ai.getAngle(), ai.getD().flags);
						TextDescriptor td = TextDescriptor.getArcTextDescriptor().withOff(0, -1);
						second.setTextDescriptor(ArcInst.ARC_NAME, td);
					} else
					{
						System.out.println("ERROR: Unable to connect export " + exportName + " to circuitry in cell " +
							curCell.describe(false));
					}
				}
			}
			exportsOnNet = null;
			arcsOnNet = null;
		}
	}

	private class KeyNetBundle extends EDIFKEY
	{
		private KeyNetBundle() { super("netBundle"); }
		protected void push()
			throws IOException
		{
			curGeometryType = GBUS;
			bundleReference = "";
			objectName = "";   originalName = "";
			isArray = false;
			if (checkName())
			{
				bundleReference = objectName;
				bundleName = objectName;
			}
		}

		protected void pop()
		{
			bundleReference = "";
			curArc = null;
			curGeometryType = GUNKNOWN;
			freeSavedPointList();
		}
	}

	private class KeyNumber extends EDIFKEY
	{
		private KeyNumber() { super("number"); }
		protected void push()
			throws IOException
		{
			propertyValue = new Double(getNumber());
		}
	}

	private class KeyOpenShape extends EDIFKEY
	{
		private KeyOpenShape() { super("openShape"); }
		protected void push()
		{
			freePointList();
			curOrientation = OR0;
		}

		protected void pop()
		{
			doPoly();
		}
	}

	private class KeyOrientation extends EDIFKEY
	{
		private KeyOrientation() { super("orientation"); }
		protected void push()
			throws IOException
		{
			// get the orientation keyword
			String orient = getToken((char)0);

			if (orient.equalsIgnoreCase("R0")) curOrientation = OR0;
			else if (orient.equalsIgnoreCase("R90")) curOrientation = OR90;
			else if (orient.equalsIgnoreCase("R180")) curOrientation = OR180;
			else if (orient.equalsIgnoreCase("R270")) curOrientation = OR270;
			else if (orient.equalsIgnoreCase("MY")) curOrientation = OMY;
			else if (orient.equalsIgnoreCase("MX")) curOrientation = OMX;
			else if (orient.equalsIgnoreCase("MYR90")) curOrientation = OMYR90;
			else if (orient.equalsIgnoreCase("MXR90")) curOrientation = OMXR90;
			else
			{
				System.out.println("Warning, line " + lineReader.getLineNumber() + ": unknown orientation value <" + orient + ">");
				warningCount++;
			}
		}
	}

	private class KeyPage extends EDIFKEY
	{
		private KeyPage() { super("page"); }
		protected void push()
			throws IOException
		{
			// check for the name
			checkName();

			curCellPage = ++pageNumber;

			// locate this in the list of cells
			Cell proto = null;
			for(Iterator<Cell> it = curLibrary.getCells(); it.hasNext(); )
			{
				Cell cell = it.next();
				if (cell.getName().equalsIgnoreCase(cellName) && cell.isSchematic())
					{ proto = cell;   break; }
			}
			if (proto == null)
			{
				// allocate the cell
				proto = createCell(null, cellName, "sch");
				if (proto == null) throw new IOException("Error creating cell");
				proto.setMultiPage(true);
				proto.newVar(User.FRAME_SIZE, "d");
			} else
			{
				if (!builtCells.contains(proto)) ignoreBlock = true;
			}

			curCell = proto;
		}

		protected void pop()
		{
			activeView = VNULL;
		}
	}

	private class KeyPath extends EDIFKEY
	{
		private KeyPath() { super("path"); }
		protected void push()
		{
			freePointList();
			curOrientation = OR0;
			pathWidth = 0;
		}

		protected void pop()
		{
			// check for openShape type path
			if (pathWidth == 0 && curGeometryType != GNET && curGeometryType != GBUS)
			{
				doPoly();
				return;
			}
			List<PortInst> fList = new ArrayList<PortInst>();
			NodeProto np = Schematics.tech().wirePinNode;
			if (curGeometryType == GBUS || isArray) np = Schematics.tech().busPinNode;
			if (curGeometryType == GNET)
			{
				if (isBusName(netName))
					np = Schematics.tech().busPinNode;
			}
			for(int i=0; i<curPoints.size()-1; i++)
			{
				Point2D fPoint = curPoints.get(i);
				Point2D tPoint = curPoints.get(i+1);
				double fX = fPoint.getX();   double fY = fPoint.getY();
				double tX = tPoint.getX();   double tY = tPoint.getY();
				if (curCellPage > 0)
				{
					fY += (curCellPage-1) * Cell.FrameDescription.MULTIPAGESEPARATION;
					tY += (curCellPage-1) * Cell.FrameDescription.MULTIPAGESEPARATION;
				}
				if (curGeometryType == GNET || curGeometryType == GBUS)
				{
					// create a pin to pin connection
					if (fList.size() == 0)
					{
						// look for the first pin
						fList = findEDIFPort(curCell, fX, fY, Schematics.tech().wire_arc);
						if (fList.size() == 0)
						{
							// check for NodeEquivalences with translated ports
							for (PortInst pi : netPortRefs)
							{
								EDIFEquiv.NodeEquivalence ne = equivs.getNodeEquivalence(pi.getNodeInst());
								if (ne != null)
								{
									Point2D curPoint = new Point2D.Double(fX, fY);
									String orientation = com.sun.electric.tool.io.output.EDIF.getOrientation(pi.getNodeInst(), 0);
									String port = ne.getPortEquivElec(pi.getPortProto().getName()).getExtPort().name;
									curPoint = equivs.translatePortConnection(curPoint, ne.externalLib, ne.externalCell,
										ne.externalView, port, orientation);
									if ((curPoint.getX() != fX) || (curPoint.getY() != fY))
									{
										fList = findEDIFPort(curCell, curPoint.getX(), curPoint.getY(), Schematics.tech().wire_arc);
										if (fList.size() != 0)
										{
											// found exact match, move port
											fX = curPoint.getX();
											fY = curPoint.getY();
											break;
										}
									}
								}
							}
						}
						if (fList.size() == 0)
						{
							// create the "from" pin
							NodeInst ni = placePin(np, fX, fY, np.getDefWidth(), np.getDefHeight(), Orientation.IDENT, curCell);
							if (ni != null) fList.add(ni.getOnlyPortInst());
						}
					}

					// now the second ...
					List<PortInst> tList = findEDIFPort(curCell, tX, tY, Schematics.tech().wire_arc);
					if (tList.size() == 0)
					{
						// check for NodeEquivalences with translated ports
						for (PortInst pi : netPortRefs)
						{
							EDIFEquiv.NodeEquivalence ne = equivs.getNodeEquivalence(pi.getNodeInst());
							if (ne != null)
							{
								Point2D curPoint = new Point2D.Double(tX, tY);
								String orientation = com.sun.electric.tool.io.output.EDIF.getOrientation(pi.getNodeInst(), 0);
								String port = ne.getPortEquivElec(pi.getPortProto().getName()).getExtPort().name;
								curPoint = equivs.translatePortConnection(curPoint, ne.externalLib, ne.externalCell,
									ne.externalView, port, orientation);
								if (curPoint.getX() != tX || curPoint.getY() != tY)
								{
									tList = findEDIFPort(curCell, curPoint.getX(), curPoint.getY(), Schematics.tech().wire_arc);
									if (tList.size() != 0)
									{
										// found exact match, move port
										tX = curPoint.getX();
										tY = curPoint.getY();
										break;
									}
								}
							}
						}
					}
					if (tList.size() == 0)
					{
						// create the "to" pin
						if (curCellPage > 0) tY += (curCellPage-1) * Cell.FrameDescription.MULTIPAGESEPARATION;
						NodeInst ni = placePin(np, tX, tY, np.getDefWidth(), np.getDefHeight(), Orientation.IDENT, curCell);
						if (ni != null) tList.add(ni.getOnlyPortInst());
					}

					if (fList.size() == 0 || tList.size() == 0)
					{
						System.out.println("Error, line " + lineReader.getLineNumber() + ": could not create path");
						errorCount++;
					} else
					{
						// connect it
						ArcInst ai = null;
						PortInst fPi = null;
						PortInst tPi = null;
						for (int count = 0; count < fList.size() || count < tList.size(); count++)
						{
							boolean fbus = false;
							boolean tbus = false;
							if (count < fList.size())
							{
								fPi = fList.get(count);

								// check node for array variable
								NodeInst ni = fPi.getNodeInst();
								Variable var = ni.getVar("EDIF_array");
								if (var != null) fbus = true; else
								{
									if (isBusName(fPi.getPortProto().getName())) fbus = true;
								}
							}
							if (count < tList.size())
							{
								tPi = tList.get(count);

								// check node for array variable
								NodeInst ni = tPi.getNodeInst();
								Variable var = ni.getVar("EDIF_array");
								if (var != null) tbus = true; else
								{
									if (isBusName(tPi.getPortProto().getName())) tbus = true;
								}
							}

							// if bus to bus
							ArcProto ap = Schematics.tech().wire_arc;
							if ((fPi.getPortProto() == defaultBusPort || fbus) &&
								(tPi.getPortProto() == defaultBusPort || tbus))
							{
								ap = Schematics.tech().bus_arc;
							} else
							{
								if (curGeometryType == GNET)
								{
									if (isBusName(netName)) ap = Schematics.tech().bus_arc;
								} else if (curGeometryType == GBUS)
								{
									if (isBusName(bundleName)) ap = Schematics.tech().bus_arc;
								}
							}
							Point2D fromPoint = new Point2D.Double(fX, fY);
							if (!fPi.getPoly().contains(fromPoint)) fromPoint = fPi.getPoly().getCenter();
							Point2D toPoint = new Point2D.Double(tX, tY);
							if (!tPi.getPoly().contains(toPoint)) toPoint = tPi.getPoly().getCenter();

							// check for wire connecting to a bus port: insert a Join
							if (ap == Schematics.tech().wire_arc)
							{
								if (isBusName(fPi.getPortProto().getName()))
								{
									PrimitiveNode joinNode = Schematics.tech().wireConNode;
									Point2D ctrPoint = new Point2D.Double(fromPoint.getX() + (toPoint.getX()-fromPoint.getX())/3,
										fromPoint.getY() + (toPoint.getY()-fromPoint.getY())/3);
									NodeInst ni = NodeInst.makeInstance(joinNode, ctrPoint,
										joinNode.getDefWidth(), joinNode.getDefHeight(), curCell);
									PortInst newFrom = ni.getOnlyPortInst();
									ArcInst.makeInstance(Schematics.tech().bus_arc, fPi, newFrom);
									fPi = newFrom;
									fromPoint = ctrPoint;
								}
								if (isBusName(tPi.getPortProto().getName()))
								{
									PrimitiveNode joinNode = Schematics.tech().wireConNode;
									Point2D ctrPoint = new Point2D.Double(toPoint.getX() + (fromPoint.getX()-toPoint.getX())/3,
										toPoint.getY() + (fromPoint.getY()-toPoint.getY())/3);
									NodeInst ni = NodeInst.makeInstance(joinNode, ctrPoint,
										joinNode.getDefWidth(), joinNode.getDefHeight(), curCell);
									PortInst newFrom = ni.getOnlyPortInst();
									ArcInst.makeInstance(Schematics.tech().bus_arc, tPi, newFrom);
									tPi = newFrom;
									toPoint = ctrPoint;
								}
							}
							ai = ArcInst.makeInstance(ap, fPi, tPi, fromPoint, toPoint, null);
							if (ai == null)
							{
								System.out.println("Error, line " + lineReader.getLineNumber() + ": could not create path (arc)");
								errorCount++;
							} else
							{
								if (arcsOnNet != null) arcsOnNet.add(ai);
								if (curGeometryType == GNET && i == 0)
								{
									if (netReference.length() > 0) ai.newVar("EDIF_name", netReference);
									if (netName.length() > 0) putNameOnArcOnce(ai, convertParens(netName));
								} else if (curGeometryType == GBUS && i == 0)
								{
									if (bundleReference.length() > 0) ai.newVar("EDIF_name", bundleReference);
									if (bundleName.length() > 0) putNameOnArcOnce(ai, convertParens(bundleName));
								}
							}
						}
						if (ai != null) curArc = ai;
						fList = tList;
					}
				} else
				{
					// rectalinear paths with some width
					// create a path from here to there (orthogonal only now)
					double lX = fX;
					double lY = fY;
					double hX = lX;
					double hY = lY;
					if (lX > tX) lX = tX;
					if (hX < tX) hX = tX;
					if (lY > tY) lY = tY;
					if (hY < tY) hY = tY;
					if (lY == hY || extendEnd)
					{
						lY -= pathWidth/2;
						hY += pathWidth/2;
					}
					if (lX == hX || extendEnd)
					{
						lX -= pathWidth/2;
						hX += pathWidth/2;
					}
					double cY = (lY+hY)/2;
					double cX = (lX+hX)/2;
					if (curCellPage > 0) cY += (curCellPage-1) * Cell.FrameDescription.MULTIPAGESEPARATION;
					NodeInst ni = placePin(curFigureGroup, cX, cY, hX-lX, hY-lY,
						curOrientation, curCell);
					if (ni == null)
					{
						System.out.println("Error, line " + lineReader.getLineNumber() + ": could not create path");
						errorCount++;
					}
				}
			}
			freePointList();
		}
	}

	private class KeyPathWidth extends EDIFKEY
	{
		private KeyPathWidth() { super("pathWidth"); }
		protected void push()
			throws IOException
		{
			// get the width string
			String width = getToken((char)0);
			pathWidth = TextUtils.atoi(width);
		}
	}

	private class KeyPolygon extends EDIFKEY
	{
		private KeyPolygon() { super("polygon"); }
		protected void push()
			throws IOException
		{
			freePointList();
			curOrientation = OR0;
		}

		protected void pop()
		{
			doPoly();
		}
	}

	private class KeyPort extends EDIFKEY
	{
		private KeyPort() { super("port"); }
		protected void push()
			throws IOException
		{
			objectName = "";   originalName = "";
			curDirection = PortCharacteristic.IN;
			portReference = "";
			isArray = false;
			arrayXVal = arrayYVal = 1;
			if (checkName())
			{
				portReference = objectName;
				portName = objectName;
			}
		}

		protected void pop()
		{
			EDIFPort port = new EDIFPort();
			port.next = portsListHead;
			portsListHead = port;
			port.reference = portReference;
			port.name = portName;
			port.direction = curDirection;

			double cX = 0, cY = 0;
			PortProto fPp = defaultInput;
			if (portsListHead.direction == PortCharacteristic.OUT)
				fPp = defaultOutput;

			// now create the off-page reference
			double psX = Schematics.tech().offpageNode.getDefWidth();
			double psY = Schematics.tech().offpageNode.getDefHeight();
			if (curCellPage > 0) cY += (curCellPage-1) * Cell.FrameDescription.MULTIPAGESEPARATION;
			NodeInst ni = NodeInst.makeInstance(Schematics.tech().offpageNode, new Point2D.Double(cX, cY), psX, psY, curCell);
			if (ni == null)
			{
				System.out.println("Error, line " + lineReader.getLineNumber() + ": could not create external port");
				errorCount++;
				return;
			}
			if (offpageNodes != null) offpageNodes.add(ni);

			// now create the port
			PortInst pi = ni.findPortInstFromProto(fPp);
			Export ppt = makeExport(curCell, pi, portsListHead.name);
			if (ppt == null)
			{
				System.out.println("Error, line " + lineReader.getLineNumber() + ": could not create port <" + portsListHead.name + ">");
				errorCount++;
			} else
			{
				ppt.setCharacteristic(portsListHead.direction);
			}
			portReference = "";

			// move the property list to the free list
			for (EDIFProperty property = propertiesListHead; property != null; property = property.next)
				ppt.newVar(property.name, property.val);
			propertiesListHead = null;
		}
	}

	private class KeyPortImplementation extends EDIFKEY
	{
		private KeyPortImplementation() { super("portImplementation"); }
		protected void push()
			throws IOException
		{
			// set the current geometry type
			freePointList();
			cellRefProto = Schematics.tech().busPinNode;
			curGeometryType = GPIN;
			curOrientation = OR0;
			isArray = false;
			arrayXVal = arrayYVal = 1;
			objectName = "";   originalName = "";
			checkName();
		}

		protected void pop()
		{
			curGeometryType = GUNKNOWN;
		}
	}

	private class KeyPortRef extends EDIFKEY
	{
		private KeyPortRef() { super("portRef"); }
		protected void push()
			throws IOException
		{
			portReference = "";
			instanceReference = "";
			if (checkName())
			{
				portReference = objectName;
			}
		}

		protected void pop()
		{
			// check for the last pin
			NodeInst fNi = curNode;
			PortProto fPp = curPort;
			if (portReference.length() > 0)
			{
				// For internal pins of an instance, determine the base port location and other pin assignments
				ArcProto ap = Generic.tech().unrouted_arc;
				NodeInst ni = null;
				PortProto pp = null;
				if (instanceReference.length() > 0)
				{
					String nodeName = instanceReference;

					// locate the node and and port
					if (activeView == VNETLIST)
					{
						// scan all pages for this nodeinst
						for(Iterator<Cell> it = curLibrary.getCells(); it.hasNext(); )
						{
							Cell cell = it.next();
							if (!cell.getName().equalsIgnoreCase(cellName)) continue;
							for(Iterator<NodeInst> nIt = cell.getNodes(); nIt.hasNext(); )
							{
								NodeInst oNi = nIt.next();
								Variable var = oNi.getVar("EDIF_name");
								if (var != null && var.getPureValue(-1).equalsIgnoreCase(nodeName)) { ni = oNi;   break; }
								String name = oNi.getName();
								if (name.equalsIgnoreCase(nodeName)) { ni = oNi;   break; }
							}
							if (ni != null) break;
						}
						if (ni == null)
						{
							System.out.println("error, line " + lineReader.getLineNumber() + ": could not locate netlist node (" + nodeName + ")");
							return;
						}
					} else
					{
						// net always references the current page
						for(Iterator<NodeInst> nIt = curCell.getNodes(); nIt.hasNext(); )
						{
							NodeInst oNi = nIt.next();
							Variable var = oNi.getVar("EDIF_name");
							if (var != null && var.getPureValue(-1).equalsIgnoreCase(nodeName)) { ni = oNi;   break; }
							String name = oNi.getName();
							if (name.equalsIgnoreCase(nodeName)) { ni = oNi;   break; }
						}
						if (ni == null)
						{
							String alternateName = renamedObjects.get(nodeName);
							for(Iterator<NodeInst> nIt = curCell.getNodes(); nIt.hasNext(); )
							{
								NodeInst oNi = nIt.next();
								String name = oNi.getName();
								if (name.equalsIgnoreCase(alternateName)) { ni = oNi;   break; }
							}
						}
						if (ni == null)
						{
							System.out.println("error, line " + lineReader.getLineNumber() + ": could not locate schematic node '" +
								nodeName + "' in " + curCell);
							return;
						}
						if (!isArray) ap = Schematics.tech().wire_arc; else
							ap = Schematics.tech().bus_arc;
					}

					// locate the port for this portref
					pp = ni.getProto().findPortProto(portReference);
					if (pp == null)
					{
						String alternateName = renamedObjects.get(portReference);
						if (alternateName != null)
						{
							alternateName = convertParens(alternateName);
							pp = ni.getProto().findPortProto(alternateName);
						}
						if (pp == null)
						{
							EDIFEquiv.NodeEquivalence ne = mappedNodes.get(nodeName);
							if (ne != null)
							{
								for(EDIFEquiv.PortEquivalence pe : ne.portEquivs)
								{
									if (pe.getExtPort().name.equals(portReference))
									{
										pp = ni.getProto().findPortProto(pe.getElecPort().name);
										break;
									}
								}
							}
						}
						if (pp == null)
						{
							String errorMsg = "error, line " + lineReader.getLineNumber() +
								": could not locate port '" + portReference;
							if (alternateName != null) errorMsg += "' or '" + alternateName;
							errorMsg += "' on node '" + nodeName + "' in cell " + curCell.describe(false);
							System.out.println(errorMsg);
							return;
						}
					}

					// we have both, set global variable
					curNode = ni;
					curPort = pp;
					Cell np = ni.getParent();
					netPortRefs.add(ni.findPortInstFromProto(pp));

					// create extensions for net labels on single pin nets (externals), and placeholder for auto-routing later
					if (activeView == VNETLIST)
					{
						// route all pins with an extension
						if (ni != null)
						{
							Poly portPoly = ni.findPortInstFromProto(pp).getPoly();
							double hX = portPoly.getCenterX();
							double hY = portPoly.getCenterY();
							double lX = hX;
							double lY = hY;
							if (pp.getCharacteristic() == PortCharacteristic.IN) lX = hX - (INCH/10); else
								if (pp.getCharacteristic() == PortCharacteristic.BIDIR) lY = hY - (INCH/10); else
									if (pp.getCharacteristic() == PortCharacteristic.OUT)
									{
										lX = hX;
										hX = hX + (INCH/10);
									}

							// need to create a destination for the wire
							ArcProto lAp = Schematics.tech().bus_arc;
							PortProto lPp = defaultBusPort;
							NodeInst lNi = null;
							if (isArray)
							{
								lNi = placePin(Schematics.tech().busPinNode, (lX+hX)/2, (lY+hY)/2, hX-lX, hY-lY, Orientation.IDENT, np);
								if (lNi == null)
								{
									System.out.println("error, line " + lineReader.getLineNumber() + ": could not create bus pin");
									return;
								}
							} else
							{
								lNi = placePin(Schematics.tech().wirePinNode, (lX+hX)/2, (lY+hY)/2, hX-lX, hY-lY, Orientation.IDENT, np);
								if (lNi == null)
								{
									System.out.println("error, line " + lineReader.getLineNumber() + ": could not create wire pin");
									return;
								}
								lPp = defaultPort;
								lAp = Schematics.tech().wire_arc;
							}
							PortInst head = lNi.findPortInstFromProto(lPp);
							PortInst tail = ni.findPortInstFromProto(pp);
							curArc = ArcInst.makeInstance(lAp, head, tail);
							if (curArc == null)
								System.out.println("error, line " + lineReader.getLineNumber() + ": could not create auto-path");
							else
								nameEDIFArc(curArc);
						}
					}
				} else
				{
					// external port reference, look for a off-page reference in {sch} with this port name
					if (exportsOnNet != null)
					{
						String alternateName = renamedObjects.get(portReference);
						if (alternateName == null) alternateName = portReference;
						exportsOnNet.add(alternateName);
					}
					if (activeView == VNETLIST)
					{
						Cell np = null;
						for(Iterator<Cell> it = curLibrary.getCells(); it.hasNext(); )
						{
							Cell cell = it.next();
							if (cell.getName().equalsIgnoreCase(cellName) && cell.isSchematic())
							{
								np = cell;
								break;
							}
						}
						if (np == null)
						{
							System.out.println("error, line " + lineReader.getLineNumber() + ": could not locate top level schematic");
							return;
						}

						// now look for an instance with the correct port name
						pp = np.findPortProto(portReference);
						if (pp == null)
						{
							if (originalName.length() > 0)
								pp = np.findPortProto(originalName);
							if (pp == null)
							{
								String alternateName = renamedObjects.get(portReference);
								if (alternateName != null)
									pp = np.findPortProto(alternateName);
							}
							if (pp == null)
							{
								System.out.println("error, line " + lineReader.getLineNumber() + ": could not locate port '" +
									portReference + "' on cell " + np.describe(false));
								return;
							}
						}

						fNi = ((Export)pp).getOriginalPort().getNodeInst();
						fPp = ((Export)pp).getOriginalPort().getPortProto();
						Poly portPoly = fNi.findPortInstFromProto(fPp).getPoly();
						double lX = portPoly.getCenterX();
						double lY = portPoly.getCenterY();

						// determine x position by original placement
						if (pp.getCharacteristic() == PortCharacteristic.IN) lX = 1*INCH; else
							if (pp.getCharacteristic() == PortCharacteristic.BIDIR) lY = 4*INCH; else
								if (pp.getCharacteristic() == PortCharacteristic.OUT) lX = 5*INCH;

						// need to create a destination for the wire
						if (isArray)
						{
							ni = placePin(Schematics.tech().busPinNode, lX, lY,
								Schematics.tech().busPinNode.getDefWidth(), Schematics.tech().busPinNode.getDefHeight(), Orientation.IDENT, np);
							if (ni == null)
							{
								System.out.println("error, line " + lineReader.getLineNumber() + ": could not create bus pin");
								return;
							}
							pp = defaultBusPort;
						} else
						{
							ni = placePin(Schematics.tech().wirePinNode, lX, lY,
								Schematics.tech().wirePinNode.getDefWidth(), Schematics.tech().wirePinNode.getDefHeight(), Orientation.IDENT, np);
							if (ni == null)
							{
								System.out.println("error, line " + lineReader.getLineNumber() + ": could not create wire pin");
								return;
							}
							pp = defaultPort;
						}
						if (!isArray) ap = Schematics.tech().wire_arc; else
							ap = Schematics.tech().bus_arc;
					}
				}

				// now connect if we have from node and port
				if (fNi != null && fPp != null)
				{
					if (fNi.getParent() != ni.getParent())
					{
						System.out.println("error, line " + lineReader.getLineNumber() + ": could not create path (arc) between " +
							fNi.getParent() + " and " + ni.getParent());
					} else
					{
						// locate the position of the new ports
						Poly fPortPoly = fNi.findPortInstFromProto(fPp).getPoly();
						double lX = fPortPoly.getCenterX();
						double lY = fPortPoly.getCenterY();
						Poly portPoly = ni.findPortInstFromProto(pp).getPoly();
						double hX = portPoly.getCenterX();
						double hY = portPoly.getCenterY();

						// for nets with no physical representation ...
						curArc = null;
						PortInst head = fNi.findPortInstFromProto(fPp);
						Point2D headPt = new Point2D.Double(lX, lY);
						PortInst tail = ni.findPortInstFromProto(pp);
						Point2D tailPt = new Point2D.Double(hX, hY);
						double dist = 0;
						if (lX != hX || lY != hY) dist = headPt.distance(tailPt);
						if (dist <= 1)
						{
							curArc = ArcInst.makeInstance(ap, head, tail, headPt, tailPt, null);
							if (curArc == null)
							{
								System.out.println("error, line " + lineReader.getLineNumber() + ": could not create path (arc) among cells");
							}
						} else if (activeView == VNETLIST)
						{
							// use unrouted connection for NETLIST views
							curArc = ArcInst.makeInstance(ap, head, tail, headPt, tailPt, null);
							if (curArc == null)
							{
								System.out.println("error, line " + lineReader.getLineNumber() + ": could not create auto-path in portRef");
							}
						}

						// add the net name
						if (curArc != null)
							nameEDIFArc(curArc);
					}
				}
			}
		}
	}

	private class KeyProgram extends EDIFKEY
	{
		private KeyProgram() { super("program"); }
		protected void push()
			throws IOException
		{
			String program = getToken((char)0);
			if (program.substring(1).startsWith("VIEWlogic"))
			{
				curVendor = EVVIEWLOGIC;
			} else if (program.substring(1).startsWith("edifout"))
			{
				curVendor = EVCADENCE;
			}
		}
	}

	private class KeyProperty extends EDIFKEY
	{
		private KeyProperty() { super("property"); }
		protected void push()
			throws IOException
		{
			propertyReference = "";
			objectName = "";   originalName = "";
			propertyValue = null;
			if (checkName())
			{
				propertyReference = objectName;
			}
		}

		protected void pop()
			throws IOException
		{
//			if (activeView == VNETLIST || activeView == VSCHEMATIC)
			{
				// add as a variable to the current object
				Cell np = null;
				if (keyStack[keyStackDepth - 1] == KINTERFACE)
				{
					// add to the {sch} view nodeproto
					String desiredName = cellName + "{sch}";
					np = curLibrary.findNodeProto(desiredName);
					if (np == null)
					{
						// allocate the cell
						np = createCell(null, cellName, "sch");
						if (np == null) throw new IOException("Error creating cell");
						np.newVar(propertyReference, propertyValue);
					}
				} else if (keyStack[keyStackDepth - 1] == KINSTANCE || keyStack[keyStackDepth - 1] == KNET ||
					keyStack[keyStackDepth - 1] == KPORT)
				{
					// add to the current property list, will be added latter
					EDIFProperty property = new EDIFProperty();
					property.next = propertiesListHead;
					propertiesListHead = property;
					property.name = "ATTR_" + propertyReference;
					property.val = propertyValue;
				} else if (keyStack[keyStackDepth - 1] == KCELL)
				{
					if (isAcceptedParameter(propertyReference))
					{
						curCellParameterOff++;
						TextDescriptor td = TextDescriptor.getCellTextDescriptor().withDispPart(TextDescriptor.DispPos.NAMEVALUE).
							withInherit(true).withParam(true).withOff(0, curCellParameterOff);
						curCell.newVar(Variable.newKey("ATTR_" + propertyReference), propertyValue, td);
					}					
				}
			}
			propertyReference = "";
			freeSavedPointList();
		}
	}

	/**
	 * Method to see if a parameter name is acceptable for placement in the circuit.
	 * @param param the parameter name.
	 * @return true if the name should be placed in the circuit.
	 */
	private boolean isAcceptedParameter(String param)
	{
		String acceptedParameters = IOTool.getEDIFAcceptedParameters();
		if (acceptedParameters.length() == 0) return false;
		String [] params = acceptedParameters.split("/");
		for(int i=0; i<params.length; i++)
			if (param.equalsIgnoreCase(params[i])) return true;
		return false;
	}

	private class KeyPt extends EDIFKEY
	{
		private KeyPt() { super("pt"); }
		protected void push()
			throws IOException
		{
			// get the x and y values of the point
			String xStr = getToken((char)0);
			if (xStr == null) throw new IOException("Unexpected end-of-file");
			String yStr = getToken((char)0);
			if (yStr == null) throw new IOException("Unexpected end-of-file");

			if (keyStackDepth > 1 && keyStack[keyStackDepth-1] == KDELTA)
			{
				double x = TextUtils.atof(xStr) * inputScale;
				double y = TextUtils.atof(yStr) * inputScale;

				// transform the points per orientation
				if (curOrientation == OR90)   { double s = x;   x = -y;  y = s; } else
				if (curOrientation == OR180)  { x = -x;  y = -y; } else
				if (curOrientation == OR270)  { double s = x;   x = y;   y = -s; } else
				if (curOrientation == OMY)    { x = -x; } else
				if (curOrientation == OMX)    { y = -y; } else
				if (curOrientation == OMYR90) { double s = y;   y = -x;  x = -s; } else
				if (curOrientation == OMXR90) { double s = x;   x = y;   y = s; }

				// set the array deltas
				if (!deltaPointsSet)
				{
					deltaPointXX = x;
					deltaPointXY = y;
				} else
				{
					deltaPointYX = x;
					deltaPointYY = y;
				}
				deltaPointsSet = true;
			} else
			{
				// allocate a point to read
				Point2D point = new Point2D.Double(TextUtils.atof(xStr) * inputScale, TextUtils.atof(yStr) * inputScale);

				// add it to the list of points
				curPoints.add(point);
			}
		}
	}

	private class KeyRectangle extends EDIFKEY
	{
		private KeyRectangle() { super("rectangle"); }
		protected void push()
		{
			freePointList();
			curOrientation = OR0;
		}

		protected void pop()
		{
			if (keyStackDepth > 1 && (keyStack[keyStackDepth-1] == KPAGESIZE ||
					keyStack[keyStackDepth-1] == KBOUNDINGBOX)) return;
			if (curPoints.size() == 2)
			{
				// create the node instance
				Point2D p0 = curPoints.get(0);
				Point2D p1 = curPoints.get(1);
				double hX = p1.getX();
				double lX = p0.getX();
				if (p0.getX() > p1.getX())
				{
					lX = p1.getX();
					hX = p0.getX();
				}
				double hY = p1.getY();
				double lY = p0.getY();
				if (p0.getY() > p1.getY())
				{
					lY = p1.getY();
					hY = p0.getY();
				}
				double sX = hX - lX;
				double sY = hY - lY;
				double yPos = (lY+hY)/2;
				double xPos = (lX+hX)/2;
				if (curCellPage > 0) yPos += (curCellPage-1) * Cell.FrameDescription.MULTIPAGESEPARATION;
				NodeInst ni = NodeInst.makeInstance(curFigureGroup != null ? curFigureGroup : Artwork.tech().boxNode,
					new Point2D.Double(xPos, yPos), sX, sY, curCell, curOrientation, null, 0);
				if (ni == null)
				{
					System.out.println("Error, line " + lineReader.getLineNumber() + ": could not create rectangle");
					errorCount++;
				} else if (curFigureGroup == Artwork.tech().openedDottedPolygonNode)
				{
					EPoint [] pts = new EPoint[5];
					pts[0] = new EPoint(p0.getX(), p0.getY());
					pts[1] = new EPoint(p0.getX(), p1.getY());
					pts[2] = new EPoint(p1.getX(), p1.getY());
					pts[3] = new EPoint(p1.getX(), p0.getY());
					pts[4] = new EPoint(p0.getX(), p0.getY());

					// store the trace information
					ni.setTrace(pts);
				}
				else if (curGeometryType == GPIN)
				{
					// create a rectangle using the pin-proto, and create the port and export ensure full sized port
					double cX = (lX+hX) / 2;
					double cY = (lY+hY) / 2;
					if (curCellPage > 0) cY += (curCellPage-1) * Cell.FrameDescription.MULTIPAGESEPARATION;
					ni = placePin(cellRefProto, cX, cY, cellRefProto.getDefWidth(), cellRefProto.getDefHeight(),
						curOrientation, curCell);
					if (ni == null)
					{
						System.out.println("Error, line " + lineReader.getLineNumber() + ": could not create pin");
						errorCount++;
					}
					String exportName = renamedObjects.get(objectName);
					if (exportName == null) exportName = objectName;
					PortProto ppt = curCell.findPortProto(exportName);
					if (ppt == null)
					{
						PortInst pi = ni.findPortInstFromProto(defaultIconPort);
						ppt = makeExport(curCell, pi, exportName);
					}
					if (ppt == null)
					{
						System.out.println("Error, line " + lineReader.getLineNumber() + ": could not create port <" + exportName + ">");
						errorCount++;
					}
				}
			}
			freePointList();
		}
	}

	private class KeyRename extends EDIFKEY
	{
		private KeyRename() { super("rename"); }
		protected void push()
			throws IOException
		{
			// get the name of the object
			String aName = getToken((char)0);
			objectName = aName;

			// and the original name
			positionToNextToken();
			char chr = inputBuffer.charAt(inputBufferPos);
			if (chr == '(')
			{
				// must be stringDisplay, copy name to original
				originalName = objectName;
			} else
			{
				aName = getToken((char)0);

				// copy name without quotes
				originalName = aName.substring(1, aName.length()-1);
			}
			originalName = fixAngleBracketBusses(fixLeadingAmpersand(originalName));
			int kPtr = keyStackDepth;
			if (keyStack[keyStackDepth - 1] == KNAME) kPtr = keyStackDepth - 1;
			if (keyStack[kPtr - 1] == KARRAY) kPtr = kPtr - 2; else
				kPtr = kPtr -1;
			if (keyStack[kPtr] == KCELL)
			{
				cellName = cellReference = originalName;
			} else if (keyStack[kPtr] == KPORT)
			{
				// convert <> to [] in port names
				portReference = portName = originalName;
			} else if (keyStack[kPtr] == KINSTANCE)
			{
				instanceReference = instanceName = originalName;
			} else if (keyStack[kPtr] == KNETBUNDLE)
			{
				bundleReference = bundleName = originalName;
			} else if (keyStack[kPtr] == KNET)
			{
				netReference = netName = originalName;
			} else if (keyStack[kPtr] == KPROPERTY)
			{
				propertyReference = objectName;
			}
			if (!objectName.equals(originalName))
				renamedObjects.put(objectName, originalName);
		}
	}

	private class KeyString extends EDIFKEY
	{
		private KeyString() { super("string"); }
		protected void push()
			throws IOException
		{
			positionToNextToken();
			char chr = inputBuffer.charAt(inputBufferPos);

			if (chr != '(' && chr != ')')
			{
				String value = getToken((char)0);
				if (value == null) throw new IOException("Unexpected end-of-file");

				propertyValue = value.substring(1, value.length()-1);
			}
		}
	}

	private class KeyStringDisplay extends EDIFKEY
	{
		private KeyStringDisplay() { super("stringDisplay"); }
		protected void push()
			throws IOException
		{
			// init the point lists
			freePointList();
			curOrientation = OR0;
			textVisible = true;
			textJustification = TextDescriptor.Position.DOWNRIGHT;
			textHeight = 0;

			// get the string, remove the quote
			getDelimeter('\"');
			textString = getToken('\"');
			if (textString == null) throw new IOException("Unexpected end-of-file");

			// check for RENAME
			if (keyStack[keyStackDepth-1] != KRENAME) return;
			originalName = fixAngleBracketBusses(textString);
			int kPtr = keyStackDepth - 2;
			if (keyStack[keyStackDepth - 2] == KARRAY) kPtr = keyStackDepth - 3;
			if (keyStack[kPtr] == KCELL)
			{
				cellName = fixLeadingAmpersand(originalName);
			} else if (keyStack[kPtr] == KPORT)
			{
				portName = fixLeadingAmpersand(originalName);
			} else if (keyStack[kPtr] == KINSTANCE)
			{
				instanceName = originalName;
			} else if (keyStack[kPtr] == KNET)
			{
				netName = fixLeadingAmpersand(originalName);
			} else if (keyStack[kPtr] == KNETBUNDLE)
			{
				bundleName = originalName;
			}
		}

		protected void pop()
		{
			if (keyStackDepth <= 1)
			{
				System.out.println("Error, line " + lineReader.getLineNumber() + ": bad location for \"stringDisplay\"");
				errorCount++;
			} else if (keyStack[keyStackDepth-1] == KRENAME)
			{
				// save the data and break
				freeSavedPointList();
				textString = "";
				saveTextPoints = curPoints;
				curPoints = new ArrayList<Point2D>();
				saveTextHeight = textHeight;
				textHeight = 0;
				saveTextJustification = textJustification;
				textJustification = TextDescriptor.Position.DOWNRIGHT;
				curOrientation = OR0;
				textVisible = true;
			}

			// output the data for annotate (display graphics) and string (on properties)
			else if (keyStack[keyStackDepth-1] == KANNOTATE || keyStack[keyStackDepth-1] == KSTRING)
			{
				boolean thisVisible = false; // textVisible;
				// supress this if it starts with "["
				if (thisVisible && !textString.startsWith("[") && curPoints.size() != 0)
				{
					// see if a pre-existing node or arc exists to add text
					ArcInst ai = null;
					NodeInst ni = null;
					String key = propertyReference;
					Point2D p0 = curPoints.get(0);
					double xOff = 0, yOff = 0;
					if (propertyReference.length() > 0 && curNode != null)
					{
						ni = curNode;
						xOff = p0.getX() - ni.getAnchorCenterX();
						yOff = p0.getY() - ni.getAnchorCenterY();
					}
					else if (propertyReference.length() > 0 && curArc != null)
					{
						ai = curArc;
						xOff = p0.getX() - (ai.getHeadLocation().getX() + ai.getTailLocation().getX()) / 2;
						yOff = p0.getY() - (ai.getHeadLocation().getY() + ai.getTailLocation().getY()) / 2;
					} else
					{
						// create the node instance
						double xPos = p0.getX();
						double yPos = p0.getY();
						if (curCellPage > 0) yPos += (curCellPage-1) * Cell.FrameDescription.MULTIPAGESEPARATION;
						ni = NodeInst.makeInstance(Generic.tech().invisiblePinNode, new Point2D.Double(xPos, yPos), 0, 0, curCell);
						key = Artwork.ART_MESSAGE.getName(); // "EDIF_annotate";
					}
					if (ni != null || ai != null)
					{
						Variable.Key varKey = Variable.newKey(key);
						// determine the size of text, 0.0278 in == 2 points or 36 (2xpixels) == 1 in fonts range from 4 to 31
						double relSize = convertTextSize(textHeight);
						if (ni != null)
						{
							TextDescriptor td = TextDescriptor.getNodeTextDescriptor();
							// now set the position, relative to the center of the current object
							xOff = p0.getX() - ni.getAnchorCenterX();
							yOff = p0.getY() - ni.getAnchorCenterY();
							td = td.withDisplay(true).withRelSize(relSize).withPos(textJustification).withOff(xOff, yOff);
							ni.newVar(varKey, textString, td);
						} else
						{
							TextDescriptor td = TextDescriptor.getArcTextDescriptor();
							// now set the position, relative to the center of the current object
							xOff = p0.getX() - (ai.getHeadLocation().getX() + ai.getTailLocation().getX()) / 2;
							yOff = p0.getY() - (ai.getHeadLocation().getY() + ai.getTailLocation().getY()) / 2;
							td = td.withDisplay(true).withRelSize(relSize).withPos(textJustification).withOff(xOff, yOff);
							ai.newVar(varKey, textString, td);
						}
					} else
					{
						System.out.println("Error, line " + lineReader.getLineNumber() + ": nothing to attach text to");
						errorCount++;
					}
				}
			}

			// clean up DISPLAY attributes
			freePointList();
			curNameEntry = null;
			textVisible = true;
			textJustification = TextDescriptor.Position.DOWNRIGHT;
			textHeight = 0;
		}
	}

	private class KeySymbol extends EDIFKEY
	{
		private KeySymbol() { super("symbol"); }
		protected void push()
			throws IOException
		{
			activeView = VSYMBOL;

			// locate this in the list of cells
			Cell proto = null;
			for(Iterator<Cell> it = curLibrary.getCells(); it.hasNext(); )
			{
				Cell cell = it.next();
				if (cell.getName().equalsIgnoreCase(cellName) &&
					cell.isIcon()) { proto = cell;   break; }
			}
			if (proto == null)
			{
				// allocate the cell
				proto = createCell(null, cellName, "ic");
				if (proto == null) throw new IOException("Error creating cell");
			} else
			{
				if (!builtCells.contains(proto)) ignoreBlock = true;
			}

			curCell = proto;
			curCellPage = 0;
			curCellParameterOff = 0;
			curFigureGroup = null;
		}

		protected void pop()
		{
			activeView = VNULL;
		}
	}

	private class KeyTechnology extends EDIFKEY
	{
		private KeyTechnology() { super("technology"); }
		protected void pop()
		{
			// for all layers assign their GDS number
			for (Map.Entry<Layer,String> e: curTechnology.getGDSLayers().entrySet())
			{
				Layer layer = e.getKey();
				String gdsLayer = e.getValue();

				// search for this layer
				boolean found = false;
				for(NameEntry nt : nameEntryList)
				{
					if (nt.replace.equalsIgnoreCase(layer.getName())) { found = true;   break; }
				}
				if (!found)
				{
					// add to the list
					NameEntry nt = new NameEntry();
					nt.original = "layer_" + gdsLayer;
					nt.replace = layer.getName();
					curFigureGroup = nt.node = Artwork.tech().boxNode;
					textHeight = nt.textHeight = 0;
					textJustification = nt.justification = TextDescriptor.Position.DOWNRIGHT;
					textVisible = nt.visible = true;
				}
			}

			// now look for nodes to map MASK layers to
			for (NameEntry nt : nameEntryList)
			{
				String nodeName = nt.replace + "-node";
				NodeProto np = curTechnology.findNodeProto(nodeName);
				if (np == null) np = Artwork.tech().boxNode;
				nt.node = np;
			}
		}
	}

	private class KeyTextHeight extends EDIFKEY
	{
		private KeyTextHeight() { super("textHeight"); }
		protected void push()
			throws IOException
		{
			// get the textheight value of the point
			String val = getToken((char)0);
			textHeight = (int)(TextUtils.atoi(val) * inputScale);

			if (curNameEntry != null)
				curNameEntry.textHeight = textHeight;
		}
	}

	private class KeyTransform extends EDIFKEY
	{
		private KeyTransform() { super("transform"); }
		protected void pop()
		{
			if (keyStackDepth <= 1 || keyStack[keyStackDepth-1] != KINSTANCE)
			{
				freePointList();
				return;
			}

			// get the corner offset
			double instPtX = 0, instPtY = 0;
			int instCount = curPoints.size();
			if (instCount > 0)
			{
				Point2D point = curPoints.get(0);
				instPtX = point.getX();
				instPtY = point.getY();
			}

			// if no points are specified, presume the origin
			if (instCount == 0) instCount = 1;

			if (instCount == 1 && cellRefProto != null)
			{
				for (int iX = 0; iX < arrayXVal; iX++)
				{
					double lX = instPtX + iX * deltaPointXX;
					double lY = instPtY + iX * deltaPointXY;
					for (int iY = 0; iY < arrayYVal; iY++)
					{
						double yPos = lY;
						if (curCellPage > 0) yPos += (curCellPage-1) * Cell.FrameDescription.MULTIPAGESEPARATION;
						Orientation orient = curOrientation.concatenate(Orientation.fromAngle(cellRefProtoRotation*10));
						NodeInst ni = NodeInst.makeInstance(cellRefProto, new Point2D.Double(lX+cellRefOffsetX, yPos+cellRefOffsetY),
							cellRefProto.getDefWidth(), cellRefProto.getDefHeight(), curCell, orient, null, cellRefProtoTechBits);
						curNode = ni;
						if (ni == null)
						{
							System.out.println("Error, line " + lineReader.getLineNumber() + ": could not create instance");
							errorCount++;

							// and exit for loop
							iX = arrayXVal;
							iY = arrayYVal;
						} else
						{
							if (cellRefProto instanceof Cell)
							{
								if (((Cell)cellRefProto).isWantExpanded())
									ni.setExpanded();
							}
						}
						if (curGeometryType == GPIN && lX == 0 && lY == 0)
						{
							// determine an appropriate port name
							String pName = portName;
							for (int dup = 0; ; dup++)
							{
								PortProto ppt = curCell.findPortProto(pName);
								if (ppt == null) break;
								pName = portName + "_" + (dup+1);
							}

							// only once
							iX = arrayXVal;
							iY = arrayYVal;
							PortInst pi = ni.findPortInstFromProto(defaultIconPort);
							Export ppt = makeExport(curCell, pi, pName);
							if (ppt == null)
							{
								System.out.println("Error, line " + lineReader.getLineNumber() + ": could not create port <" + pName + ">");
								errorCount++;
							} else
							{
								// locate the direction of the port
								for (EDIFPort ePort = portsListHead; ePort != null; ePort = ePort.next)
								{
									if (ePort.reference.equalsIgnoreCase(portReference))
									{
										// set the direction
										ppt.setCharacteristic(ePort.direction);
										break;
									}
								}
							}
						} else
						{
							// name the instance
							if (instanceReference.length() > 0)
							{
								String nodeName = instanceReference;
								if ((arrayXVal > 1 || arrayYVal > 1) &&
									(deltaPointXX != 0 || deltaPointXY != 0 || deltaPointYX != 0 || deltaPointYY != 0))
								{
									// if array in the x dimension
									if (arrayXVal > 1)
									{
										if (arrayYVal > 1) nodeName = instanceReference + "[" + iX + "," + iY + "]"; else
											nodeName = instanceReference + "[" + iX + "]";
									} else if (arrayYVal > 1)
										nodeName = instanceReference + "[" + iY + "]";
								}

								// check for array element descriptor
								if (arrayXVal > 1 || arrayYVal > 1)
								{
									// array descriptor is of the form index:index:range index:index:range
									String baseName = iX + ":" + ((deltaPointXX == 0 && deltaPointYX == 0) ? arrayXVal-1:iX) + ":" + arrayXVal +
										" " + iY + ":" + ((deltaPointXY == 0 && deltaPointYY == 0) ? arrayYVal-1:iY) + ":" + arrayYVal;
									ni.newVar("EDIF_array", baseName);
								}

								// now set the name of the component (note that Electric allows any string
		   						// of characters as a name, this name is open to the user, for ECO and other
								// consistancies, the EDIF_name is saved on a variable)
								Variable.Key varKey;
								if (instanceReference.equalsIgnoreCase(instanceName))
								{
									ni.setName(convertParens(nodeName));
									varKey = NodeInst.NODE_NAME;
								} else
								{
									// now add the original name as the displayed name (only to the first element)
									if (iX == 0 && iY == 0) ni.setName(convertParens(instanceName));

									// now save the EDIF name (not displayed)
									Variable var = ni.newVar("EDIF_name", nodeName);
									varKey = var.getKey();
								}

								// now check for saved name attributes
								if (saveTextPoints.size() != 0)
								{
									// now set the position, relative to the center of the current object
									Point2D sP0 = saveTextPoints.get(0);
									double xOff = sP0.getX() - ni.getTrueCenterX();
									double yOff = sP0.getY() - ni.getTrueCenterY();

									// determine the size of text, 0.0278 in == 2 points or 36 (2xpixels) == 1 in fonts range from 4 to 20 points
									if (varKey != NodeInst.NODE_NAME)
									{
										TextDescriptor td = ni.getTextDescriptor(varKey);
										td = td.withRelSize(convertTextSize(saveTextHeight)).withPos(saveTextJustification).withOff(xOff, yOff);
										ni.setTextDescriptor(varKey, td);
									}
								}
							}
						}
						if (deltaPointYX == 0 && deltaPointYY == 0) break;

						// bump the y delta and x deltas
						lX += deltaPointYX;
						lY += deltaPointYY;
					}
					if (deltaPointXX == 0 && deltaPointXY == 0) break;
				}
			}
			freePointList();
		}
	}

	private class KeyTrue extends EDIFKEY
	{
		private KeyTrue() { super("true"); }
		protected void push()
		{
			// check previous keyword
			if (keyStackDepth > 1 && keyStack[keyStackDepth-1] == KVISIBLE)
			{
				textVisible = true;
				if (curNameEntry != null)
					curNameEntry.visible = true;
			}
		}
	}

	private class KeyUnit extends EDIFKEY
	{
		private KeyUnit() { super("unit"); }
		protected void push()
			throws IOException
		{
			String type = getToken((char)0);
			if (keyStack[keyStackDepth-1] == KSCALE && type.equalsIgnoreCase("DISTANCE"))
			{
				// just make the scale be so that the specified number of database units becomes 1 lambda
				inputScale = 1;
				inputScale *= IOTool.getEDIFInputScale();
			}
		}
	}

	private class KeyView extends EDIFKEY
	{
		private KeyView() { super("view"); }
		protected void push()
		{
			activeView = VNULL;
			offpageNodes = new ArrayList<NodeInst>();
			mappedNodes = new HashMap<String,EDIFEquiv.NodeEquivalence>();
		}

		protected void pop()
		{
			if (curVendor == EVVIEWLOGIC && activeView != VNETLIST)
			{
				// now scan for BUS nets, and verify all wires connected to bus
				HashSet<ArcInst> seenArcs = new HashSet<ArcInst>();
				for(Iterator<ArcInst> it = curCell.getArcs(); it.hasNext(); )
				{
					ArcInst ai = it.next();
					if (!seenArcs.contains(ai) && ai.getProto() == Schematics.tech().bus_arc)
					{
						// get name of arc
						String baseName = ai.getName();
						int openPos = baseName.indexOf('[');
						if (openPos >= 0) baseName = baseName.substring(0, openPos);

						// expand this arc, and locate non-bracketed names
						checkBusNames(ai, baseName, seenArcs);
					}
				}
			}

			// reorganize the offpage nodes to be in a good place
			int inCount = 0, outCount = 0, bidirCount = 0;
			for(NodeInst ni : offpageNodes)
			{
				if (!ni.hasExports()) continue;
				Export e = ni.getExports().next();
				if (e.getCharacteristic() == PortCharacteristic.IN) inCount++; else
					if (e.getCharacteristic() == PortCharacteristic.BIDIR) bidirCount++; else
						if (e.getCharacteristic() == PortCharacteristic.OUT) outCount++;
			}
			double inPos = 0, outPos = 0, bidirPos = 0;
			double bidirOffset = inCount > 0 ? INCH*2 : 0;
			double outOffset = bidirOffset + (bidirCount > 0 ? INCH*2 : 0);
			Rectangle2D cellBounds = curCell.getBounds();
			for(NodeInst ni : offpageNodes)
			{
				if (!ni.hasExports()) continue;
				Export e = ni.getExports().next();
				double cX = cellBounds.getMaxX() + INCH/2;
				double cY = cellBounds.getMinY();
				if (e.getCharacteristic() == PortCharacteristic.IN)
				{
					cY += inPos;
					inPos += INCH/2;
				} else if (e.getCharacteristic() == PortCharacteristic.BIDIR)
				{
					cX += bidirOffset;
					cY += bidirPos;
					bidirPos += INCH/2;
				} else if (e.getCharacteristic() == PortCharacteristic.OUT)
				{
					cX += outOffset;
					cY += outPos;
					outPos += INCH/2;
				}
				ni.modifyInstance(cX-ni.getAnchorCenterX(), cY-ni.getAnchorCenterY(), 0, 0, Orientation.IDENT);
			}
			offpageNodes = null;
			portsListHead = null;
			mappedNodes = null;
		}
	}

	private class ViewRef extends EDIFKEY
	{
		private ViewRef() { super("viewRef"); }
		protected void push()
			throws IOException
		{
			viewRef = getToken((char)0);
		}
		protected void pop()
		{
			viewRef = null;
		}
	}

	/**
	 * viewType:  Indicates the view style for this cell, ie
	 *	BEHAVIOR, DOCUMENT, GRAPHIC, LOGICMODEL, MASKLAYOUT, NETLIST,
	 *  	PCBLAYOUT, SCHEMATIC, STRANGER, SYMBOLIC
	 * we are only concerned about the viewType NETLIST.
	 */
	private class KeyViewType extends EDIFKEY
	{
		private KeyViewType() { super("viewType"); }
		protected void push()
			throws IOException
		{
			// get the viewType
			String aName = getToken((char)0);

			String view = "";
			if (aName.equalsIgnoreCase("BEHAVIOR")) activeView = VBEHAVIOR;
			else if (aName.equalsIgnoreCase("DOCUMENT")) activeView = VDOCUMENT;
			else if (aName.equalsIgnoreCase("GRAPHIC"))
			{
				activeView = VGRAPHIC;
				view = "ic";
			}
			else if (aName.equalsIgnoreCase("LOGICMODEL")) activeView = VLOGICMODEL;
			else if (aName.equalsIgnoreCase("MASKLAYOUT"))
			{
				activeView = VMASKLAYOUT;
				view = "lay";
			}
			else if (aName.equalsIgnoreCase("NETLIST"))
			{
				activeView = VNETLIST;
				view = "sch";
			}
			else if (aName.equalsIgnoreCase("PCBLAYOUT")) activeView = VPCBLAYOUT;
			else if (aName.equalsIgnoreCase("SCHEMATIC"))
			{
				activeView = VSCHEMATIC;
				view = "sch";
			}
			else if (aName.equalsIgnoreCase("STRANGER")) activeView = VSTRANGER;
			else if (aName.equalsIgnoreCase("SYMBOLIC")) activeView = VSYMBOLIC;

			// immediately allocate MASKLAYOUT and VGRAPHIC viewtypes
			if (activeView == VMASKLAYOUT || activeView == VGRAPHIC ||
				activeView == VNETLIST || activeView == VSCHEMATIC)
			{
				Cell proto = findCellInLibrary(cellName, view);
				if (proto == null)
				{
					// create the cell
					proto = createCell(null, cellName, view);
					if (proto == null) throw new IOException("Error creating cell");
				} else
				{
					ignoreHigherBlock = true;
					System.out.println("Warning: Cell " + proto.describe(false) + " exists...EDIF for it is being ignored");
//					if (!builtCells.contains(proto)) ignoreBlock = true;
				}

				curCell = proto;
				curCellPage = 0;
				curCellParameterOff = 0;
			}
			else curCell = null;

			// add the name to the cellTable
			NameEntry nt = new NameEntry();
			nt.original = cellReference;
			nt.replace = cellName;
			cellTable.put(cellReference, nt);
		}
	}
}
