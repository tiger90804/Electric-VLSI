/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DRC.java
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
package com.sun.electric.tool.drc;

import com.sun.electric.database.ImmutableArcInst;
import com.sun.electric.database.ImmutableNodeInst;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.GeometryHandler;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.*;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.Listener;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.User;

import java.awt.geom.Rectangle2D;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.prefs.Preferences;

/**
 * This is the Design Rule Checker tool.
 */
public class DRC extends Listener
{
	/** the DRC tool. */								protected static DRC tool = new DRC();
	/** overrides of rules for each technology. */		private static HashMap<Technology,Pref> prefDRCOverride = new HashMap<Technology,Pref>();
	/** map of cells and their objects to DRC */		private static HashMap<Cell,HashSet<Geometric>> cellsToCheck = new HashMap<Cell,HashSet<Geometric>>();
	private static boolean incrementalRunning = false;
    /** key of Variable for last valid DRC date on a Cell. */
    private static final Variable.Key DRC_LAST_GOOD_DATE = Variable.newKey("DRC_last_good_drc_date");
    /** key of Variable for last valid DRC bit on a Cell. */
    private static final Variable.Key DRC_LAST_GOOD_BIT = Variable.newKey("DRC_last_good_drc_bit");
    private static final int DRC_BIT_AREA = 01; /* Min area condition */
    private static final int DRC_BIT_COVERAGE = 02;   /* Coverage DRC condition */
    private static final int DRC_BIT_ST_FOUNDRY = 04; /* For ST foundry selection */
    private static final int DRC_BIT_TSMC_FOUNDRY = 010; /* For TSMC foundry selection */
    private static final int DRC_BIT_MOSIS_FOUNDRY = 020; /* For Mosis foundry selection */

    /** Control different level of error checking */
    public enum DRCCheckMode
    {
	    ERROR_CHECK_DEFAULT (0),    /** DRC stops after first error between 2 nodes is found (default) */
        ERROR_CHECK_CELL (1),       /** DRC stops after first error per cell is found */
        ERROR_CHECK_EXHAUSTIVE (2);  /** DRC checks all combinations */
        private final int mode;   // mode
        DRCCheckMode(int mode) {
            this.mode = mode;
        }
        public int mode() { return this.mode; }
        public String toString() {return name();}
    }

    /****************************** DESIGN RULES ******************************/

	/**
	 * Class to define a single rule on a node.
	 */
    public static class NodeSizeRule
	{
		public double sizeX, sizeY;
		public String rule;

		NodeSizeRule(double sizeX, double sizeY, String rule)
		{
			this.sizeX = sizeX;
			this.sizeY = sizeY;
			this.rule = rule;
		}
	}

	/****************************** TOOL CONTROL ******************************/

	/**
	 * The constructor sets up the DRC tool.
	 */
	private DRC()
	{
		super("drc");
	}

    /**
	 * Method to initialize the DRC tool.
	 */
	public void init()
	{
		setOn();
	}

    /**
     * Method to retrieve the singleton associated with the DRC tool.
     * @return the DRC tool.
     */
    public static DRC getDRCTool() { return tool; }

	private static void includeGeometric(Geometric geom)
	{
		if (!isIncrementalDRCOn()) return;
		Cell cell = geom.getParent();
		synchronized (cellsToCheck)
		{
			HashSet<Geometric> cellSet = (HashSet<Geometric>)cellsToCheck.get(cell);
			if (cellSet == null)
			{
				cellSet = new HashSet<Geometric>();
				cellsToCheck.put(cell, cellSet);
			}
			cellSet.add(geom);
		}
	}

	private static void removeGeometric(Geometric geom)
	{
		if (!isIncrementalDRCOn()) return;
		Cell cell = geom.getParent();
		synchronized (cellsToCheck)
		{
			HashSet cellSet = (HashSet)cellsToCheck.get(cell);
			if (cellSet != null) cellSet.remove(geom);
		}
	}

	private static void doIncrementalDRCTask()
	{
		if (!isIncrementalDRCOn()) return;
		if (incrementalRunning) return;

		Library curLib = Library.getCurrent();
		if (curLib == null) return;
		Cell cellToCheck = curLib.getCurCell();
		HashSet<Geometric> cellSet = null;

		// get a cell to check
		synchronized (cellsToCheck)
		{
			if (cellToCheck != null)
				cellSet = (HashSet<Geometric>)cellsToCheck.get(cellToCheck);
			if (cellSet == null && cellsToCheck.size() > 0)
			{
				cellToCheck = (Cell)cellsToCheck.keySet().iterator().next();
				cellSet = (HashSet<Geometric>)cellsToCheck.get(cellToCheck);
			}
			if (cellSet != null)
				cellsToCheck.remove(cellToCheck);
		}

		if (cellToCheck == null) return; // nothing to do

		// don't check if cell not in database anymore
		if (!cellToCheck.isLinked()) return;
		// Handling clipboard case (one type of hidden libraries
		if (cellToCheck.getLibrary().isHidden()) return;

		// if there is a cell to check, do it
		if (cellSet != null)
		{
			Geometric [] objectsToCheck = new Geometric[cellSet.size()];
			int i = 0;
			for(Iterator<Geometric> it = cellSet.iterator(); it.hasNext(); )
				objectsToCheck[i++] = (Geometric)it.next();
			new CheckLayoutIncrementally(cellToCheck, objectsToCheck);
		}
	}

	/**
	 * Method to announce the end of a batch of changes.
	 */
	public void endBatch()
	{
		doIncrementalDRCTask();
	}

	/**
	 * Method to announce a change to a NodeInst.
	 * @param ni the NodeInst that was changed.
	 * @param oD the old contents of the NodeInst.
	 */
	public void modifyNodeInst(NodeInst ni, ImmutableNodeInst oD)
	{
		includeGeometric(ni);
	}

	/**
	 * Method to announce a change to an ArcInst.
	 * @param ai the ArcInst that changed.
     * @param oD the old contents of the ArcInst.
	 */
	public void modifyArcInst(ArcInst ai, ImmutableArcInst oD)
	{
		includeGeometric(ai);
	}

	/**
	 * Method to announce the creation of a new ElectricObject.
	 * @param obj the ElectricObject that was just created.
	 */
	public void newObject(ElectricObject obj)
	{
		if (obj instanceof Geometric)
		{
			includeGeometric((Geometric)obj);
		}
	}

	/**
	 * Method to announce the deletion of an ElectricObject.
	 * @param obj the ElectricObject that was just deleted.
	 */
	public void killObject(ElectricObject obj)
	{
		if (obj instanceof Geometric)
		{
			removeGeometric((Geometric)obj);
		}
	}

	/****************************** DRC INTERFACE ******************************/

	/**
	 * Method to check the current cell hierarchically or
	 * the selected area of the current cell hierarchically if areaCheck is true
	 */
//	public static void checkHierarchically(boolean areaCheck, GeometryHandler.GHMode mode)
//	{
//		Cell curCell = null;
//		Rectangle2D bounds = null;
//		UserInterface ui = Main.getUserInterface();
//		if (!areaCheck)
//		{
//			curCell = ui.needCurrentCell();
//		} else
//		{
//			EditWindow_ wnd = ui.getCurrentEditWindow_();
//			if (wnd == null) return;
//			bounds = wnd.getHighlightedArea();
//			curCell = wnd.getCell();
//		}
//
//        checkDRCHierarchically(curCell, bounds, mode);
//	}

    /**
     * This method generates a DRC job from the GUI or for a bash script.
     */
    public static void checkDRCHierarchically(Cell cell, Rectangle2D bounds, GeometryHandler.GHMode mode)
    {
        if (cell == null) return;
		if (cell.isSchematic() || cell.getTechnology() == Schematics.tech ||
			cell.isIcon() || cell.getTechnology() == Artwork.tech)
		{
			// hierarchical check of schematics
			new CheckSchematicHierarchically(cell);
		} else
		{
			// hierarchical check of layout
            if (mode == null) mode = GeometryHandler.GHMode.ALGO_QTREE;
			new CheckLayoutHierarchically(cell, bounds, mode);
		}
    }

	/**
	 * Base class for checking design rules.
	 *
	 */
	public static class CheckDRCLayoutJob extends Job
	{
		Cell cell;

		protected CheckDRCLayoutJob(String title, Cell cell, Listener tool, Priority priority)
		{
			super(title, tool, Job.Type.EXAMINE, null, null, priority);
			this.cell = cell;

		}
		// never used
		public boolean doIt() throws JobException { return (false);}
	}

	private static class CheckLayoutHierarchically extends CheckDRCLayoutJob
	{
		Rectangle2D bounds;
        private GeometryHandler.GHMode mergeMode; // to select the merge algorithm

        /**
         * Check bounds within cell. If bounds is null, check entire cell.
         * @param cell
         * @param bounds
         */
		protected CheckLayoutHierarchically(Cell cell, Rectangle2D bounds, GeometryHandler.GHMode mode)
		{
			super("Design-Rule Check", cell, tool, Job.Priority.USER);
			this.bounds = bounds;
            this.mergeMode = mode;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			long startTime = System.currentTimeMillis();
            int errorCount = 0, warnCount = 0;
            if (Quick.checkDesignRules(cell, null, null, bounds, this, mergeMode) > 0)
            {
                errorCount = ErrorLogger.getCurrent().getNumErrors();
                warnCount = ErrorLogger.getCurrent().getNumWarnings();
            }
            long endTime = System.currentTimeMillis();
            System.out.println(errorCount + " errors and " + warnCount + " warnings found (took " + TextUtils.getElapsedTime(endTime - startTime) + ")");
            return true;
		}
	}


	private static class CheckLayoutIncrementally extends CheckDRCLayoutJob
	{
		Geometric [] objectsToCheck;

		protected CheckLayoutIncrementally(Cell cell, Geometric [] objectsToCheck)
		{
			super("DRC in " + cell, cell, tool, Job.Priority.ANALYSIS);
			this.objectsToCheck = objectsToCheck;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			incrementalRunning = true;
			int errorsFound = Quick.checkDesignRules(cell, objectsToCheck, null, null, this);
			if (errorsFound > 0)
			{
				System.out.println("Incremental DRC found " + errorsFound + " errors/warnings in "+ cell);
			}
			incrementalRunning = false;
			doIncrementalDRCTask();
			return true;
		}
	}

	private static class CheckSchematicHierarchically extends Job
	{
		Cell cell;
//		Rectangle2D bounds;

        /**
         * Check bounds within Cell.  If bounds is null, check entire cell.
         * @param cell
         */
		protected CheckSchematicHierarchically(Cell cell)
		{
			super("Design-Rule Check " + cell, tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
//			this.bounds = bounds;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			long startTime = System.currentTimeMillis();
			ErrorLogger errorLog = Schematic.doCheck(cell);
			long endTime = System.currentTimeMillis();
			int errorCount = errorLog.getNumErrors();
			System.out.println(errorCount + " errors found (took " + TextUtils.getElapsedTime(endTime - startTime) + ")");
			return true;
		}
	}

	/**
	 * Method to delete all cached date information on all cells.
	 */
	public static void resetDRCDates()
	{
        new ResetDRCDates();
	}

    private static class ResetDRCDates extends Job
    {
        ResetDRCDates()
        {
            super("Resetting DRC Dates", User.getUserTool(), Job.Type.CHANGE, null, null, Priority.USER);
            startJob();
        }

        public boolean doIt() throws JobException
            {
            for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
            {
                Library lib = (Library)it.next();
                for(Iterator<Cell> cIt = lib.getCells(); cIt.hasNext(); )
                {
                    Cell cell = (Cell)cIt.next();
                    cleanDRCDateAndBits(cell);
                }
            }
            return true;
        }
    }

	/****************************** DESIGN RULE CONTROL ******************************/

	/** The Technology whose rules are cached. */		private static Technology currentTechnology = null;

	/**
	 * Method to build a Rules object that contains the current design rules for a Technology.
	 * The DRC dialogs use this to hold the values while editing them.
	 * It also provides a cache for the design rule checker.
	 * @param tech the Technology to examine.
	 * @return a new Rules object with the design rules for the given Technology.
	 */
	public static DRCRules getRules(Technology tech)
	{
        DRCRules currentRules = tech.getCachedRules();
		if (currentRules != null && tech == currentTechnology) return currentRules;

		// constructing design rules: start with factory rules
		currentRules = tech.getFactoryDesignRules(tech.getSelectedFoundry());
		if (currentRules != null)
		{
			// add overrides
			StringBuffer override = getDRCOverrides(tech);
			currentRules.applyDRCOverrides(override.toString(), tech);
		}

		// remember technology whose rules are cached
		currentTechnology = tech;
        tech.setCachedRules(currentRules);
		return currentRules;
	}

	/**
	 * Method to load a full set of design rules for a Technology.
	 * @param tech the Technology to load.
	 * @param newRules a complete design rules object.
	 */
	public static void setRules(Technology tech, DRCRules newRules)
	{
		// get factory design rules
		DRCRules factoryRules = tech.getFactoryDesignRules(tech.getSelectedFoundry());

		// determine override differences from the factory rules
		StringBuffer changes = Technology.getRuleDifferences(factoryRules, newRules);

        if (Job.LOCALDEBUGFLAG)
            System.out.println("This function needs attention");

		// get current overrides of factory rules
		StringBuffer override = getDRCOverrides(tech);

		// if the differences are the same as before, stop
		if (changes.toString().equals(override.toString())) return;

		// update the preference for the rule overrides
		setDRCOverrides(changes, tech);

		// update variables on the technology
		tech.setRuleVariables(newRules);

		// flush the cache of rules
		if (currentTechnology == tech) currentTechnology = null;
	}

	/****************************** INDIVIDUAL DESIGN RULES ******************************/

	/**
	 * Method to find the worst spacing distance in the design rules.
	 * Finds the largest spacing rule in the Technology.
	 * @param tech the Technology to examine.
	 * @return the largest spacing distance in the Technology. Zero if nothing found
	 */
	public static double getWorstSpacingDistance(Technology tech)
	{
		DRCRules rules = getRules(tech);
		if (rules == null)
        {
            //if (Main.getDebug()) System.out.println("Is -1 a valid number? in DRC::getWorstSpacingDistance. Zero now!");
            return 0;
        }
		return (rules.getWorstSpacingDistance());
	}

	/**
	 * Method to find the maximum design-rule distance around a layer.
	 * @param layer the Layer to examine.
	 * @return the maximum design-rule distance around the layer. -1 if nothing found.
	 */
	public static double getMaxSurround(Layer layer, double maxSize)
	{
		Technology tech = layer.getTechnology();
        if (tech == null) return -1; // case when layer is a Graphics
		DRCRules rules = getRules(tech);
		if (rules == null) return -1;

        return (rules.getMaxSurround(tech, layer, maxSize));
	}

	/**
	 * Method to find the edge spacing rule between two layer.
	 * @param layer1 the first layer.
	 * @param layer2 the second layer.
	 * @return the edge rule distance between the layers.
	 * Returns null if there is no edge spacing rule.
	 */
	public static DRCTemplate getEdgeRule(Layer layer1, Layer layer2)
	{
		Technology tech = layer1.getTechnology();
		DRCRules rules = getRules(tech);
		if (rules == null) return null;

		return (rules.getEdgeRule(tech, layer1, layer2));
	}

	/**
	 * Method to find the spacing rule between two layer.
	 * @param layer1 the first layer.
     * @param geo1
	 * @param layer2 the second layer.
     * @param geo2
	 * @param connected true to find the distance when the layers are connected.
	 * @param multiCut true to find the distance when this is part of a multicut contact.
     * @param wideS widest polygon
     * @param length length of the intersection
	 * @return the spacing rule between the layers.
	 * Returns null if there is no spacing rule.
	 */
	public static DRCTemplate getSpacingRule(Layer layer1, Geometric geo1, Layer layer2, Geometric geo2,
                                             boolean connected, int multiCut, double wideS, double length)
	{
		Technology tech = layer1.getTechnology();
		DRCRules rules = getRules(tech);
		if (rules == null) return null;
        return (rules.getSpacingRule(tech, layer1, geo1, layer2, geo2, connected, multiCut, wideS, length));
	}

	/**
	 * Method to find the extension rule between two layer.
	 * @param layer1 the first layer.
	 * @param layer2 the second layer.
     * @param isGateExtension to decide between the rule EXTENSIONGATE or EXTENSION
	 * @return the extension rule between the layers.
	 * Returns null if there is no extension rule.
	 */
	public static DRCTemplate getExtensionRule(Layer layer1, Layer layer2, boolean isGateExtension)
	{
		Technology tech = layer1.getTechnology();
		DRCRules rules = getRules(tech);
		if (rules == null) return null;
        return (rules.getExtensionRule(tech, layer1, layer2, isGateExtension));
	}

	/**
	 * Method to tell whether there are any design rules between two layers.
	 * @param layer1 the first Layer to check.
	 * @param layer2 the second Layer to check.
	 * @return true if there are design rules between the layers.
	 */
	public static boolean isAnyRule(Layer layer1, Layer layer2)
	{
		Technology tech = layer1.getTechnology();
		DRCRules rules = getRules(tech);
		if (rules == null) return false;
        return (rules.isAnyRule(tech, layer1, layer2));
	}

	/**
	 * Method to get the minimum <type> rule for a Layer
	 * where <type> is the rule type. E.g. MinWidth or Area
	 * @param layer the Layer to examine.
	 * @param type rule type
	 * @return the minimum width rule for the layer.
	 * Returns null if there is no minimum width rule.
	 */
	public static DRCTemplate getMinValue(Layer layer, DRCTemplate.DRCRuleType type)
	{
		Technology tech = layer.getTechnology();
		if (tech == null) return null;
		DRCRules rules = getRules(tech);
		if (rules == null) return null;
        return (rules.getMinValue(layer, type));
	}

    /**
     * Determine if node represented by index in DRC mapping table is forbidden under
     * this foundry.
     */
    public static boolean isForbiddenNode(int elemIndex, DRCTemplate.DRCRuleType type, Technology tech)
    {
        DRCRules rules = getRules(tech);
        if (rules == null) return false;
        int index = elemIndex;
        if (type == DRCTemplate.DRCRuleType.FORBIDDEN) index += tech.getNumLayers();
        return (rules.isForbiddenNode(index, type));
    }

	/**
	 * Method to get the minimum size rule for a NodeProto.
	 * @param np the NodeProto to examine.
	 * @return the minimum size rule for the NodeProto.
	 * Returns null if there is no minimum size rule.
	 */
	public static NodeSizeRule getMinSize(NodeProto np)
	{
		if (np instanceof Cell) return null;
		PrimitiveNode pnp = (PrimitiveNode)np;
		if (pnp.getMinWidth() < 0 && pnp.getMinHeight() < 0) return null;
		return new NodeSizeRule(pnp.getMinWidth(), pnp.getMinHeight(), pnp.getMinSizeRule());
	}

	/****************************** SUPPORT FOR DESIGN RULES ******************************/

	/**
	 * Method to get the DRC overrides from the preferences for a given technology.
	 * @param tech the Technology on which to get overrides.
	 * @return a Pref describing DRC overrides for the Technology.
	 */
	private static StringBuffer getDRCOverrides(Technology tech)
	{
		Pref pref = (Pref)prefDRCOverride.get(tech);
		if (pref == null)
		{
			pref = Pref.makeStringPref("DRCOverridesFor" + tech.getTechName(), tool.prefs, "");
			prefDRCOverride.put(tech, pref);
		}
		StringBuffer sb = new StringBuffer();
		sb.append(pref.getString());
		return sb;
	}

	/**
	 * Method to set the DRC overrides for a given technology.
	 * @param sb the overrides (a StringBuffer).
	 * @param tech the Technology on which to get overrides.
	 */
	private static void setDRCOverrides(StringBuffer sb, Technology tech)
	{
		if (sb.length() >= Preferences.MAX_VALUE_LENGTH)
		{
			System.out.println("Warning: Design rule overrides are too complex to be saved (are " +
				sb.length() + " long which is more than the limit of " + Preferences.MAX_VALUE_LENGTH + ")");
		}
		Pref pref = (Pref)prefDRCOverride.get(tech);
		if (pref == null)
		{
			pref = Pref.makeStringPref("DRCOverridesFor" + tech.getTechName(), tool.prefs, "");
			prefDRCOverride.put(tech, pref);
		}
		pref.setString(sb.toString());
	}

    /**
     * Method to clean those cells that were marked with a valid date due to
     * changes in the DRC rules.
     * @param f
     */
    public static void cleanCellsDueToFoundryChanges(Technology tech, Foundry f)
    {
        // Need to clean cells using this foundry because the rules might have changed.
        System.out.println("Cleaning good DRC dates in cells using '" + f.getType().name() +
                "' in '" + tech.getTechName() + "'");
        HashMap<Cell,Cell> cleanDRCDate = new HashMap<Cell,Cell>();
        int bit = 0;
        switch(f.getType())
        {
            case MOSIS:
                bit = DRC_BIT_MOSIS_FOUNDRY;
                break;
            case TSMC:
                bit = DRC_BIT_TSMC_FOUNDRY;
                break;
            case ST:
                bit = DRC_BIT_ST_FOUNDRY;
                break;
        }

        for (Iterator<Library> it = Library.getLibraries(); it.hasNext();)
        {
            Library lib = it.next();
            for (Iterator<Cell> itC = lib.getCells(); itC.hasNext();)
            {
                Cell cell = itC.next();
                if (cell.getTechnology() != tech) continue;

                int thisByte = getCellGoodDRCBits(cell);

                // It was marked as valid with previous set of rules
                if ((thisByte & bit) != 0)
                    cleanDRCDate.put(cell, cell);
            }
        }
        new DRC.UpdateDRCDates(0, new HashMap<Cell,Date>(), cleanDRCDate);
    }

    /**
     * Method to extract the corresponding DRC bits stored in a given cell
     * for further analysis
     * @param cell
     * @return
     */
    private static int getCellGoodDRCBits(Cell cell)
    {
        Variable varBits = cell.getVar(DRC_LAST_GOOD_BIT, Integer.class);
        int thisByte = 0;
        if (varBits == null) // old Byte class
        {
            varBits = cell.getVar(DRC_LAST_GOOD_BIT, Byte.class);
            if (varBits != null)
                thisByte =((Byte)varBits.getObject()).byteValue();
        }
        else
            thisByte = ((Integer)varBits.getObject()).intValue();
        return thisByte;
    }

    /**
     * Method to tell the date of the last successful DRC of a given Cell.
     * @param cell the cell to query.
     * @return the date of the last successful DRC of that Cell.
     */
    public static Date getLastDRCDateBasedOnBits(Cell cell, int activeBits)
    {
        Variable varDate = cell.getVar(DRC_LAST_GOOD_DATE, Integer[].class);
        if (varDate == null) return null;
        int thisByte = getCellGoodDRCBits(cell);
        boolean area = (thisByte & DRC_BIT_AREA) == (activeBits & DRC_BIT_AREA);
        boolean coverage = (thisByte & DRC_BIT_COVERAGE) == (activeBits & DRC_BIT_COVERAGE);
        // DRC date is invalid if conditions were checked for another foundry
        boolean sameManufacturer = (thisByte & DRC_BIT_TSMC_FOUNDRY) == (activeBits & DRC_BIT_TSMC_FOUNDRY) &&
                (thisByte & DRC_BIT_ST_FOUNDRY) == (activeBits & DRC_BIT_ST_FOUNDRY) &&
                (thisByte & DRC_BIT_MOSIS_FOUNDRY) == (activeBits & DRC_BIT_MOSIS_FOUNDRY);
        if (activeBits != 0 && (!area || !coverage || !sameManufacturer))
            return null;
        Integer [] lastDRCDateAsInts = (Integer [])varDate.getObject();
        long lastDRCDateInSecondsHigh = lastDRCDateAsInts[0].intValue();
        long lastDRCDateInSecondsLow = lastDRCDateAsInts[1].intValue();
        long lastDRCDateInSeconds = (lastDRCDateInSecondsHigh << 32) | (lastDRCDateInSecondsLow & 0xFFFFFFFFL);
        Date lastDRCDate = new Date(lastDRCDateInSeconds);
        return lastDRCDate;
    }

    /**
     * Method to set the date of the last successful DRC of a given Cell.
     * @param cell the cell to modify.
     * @param date the date of the last successful DRC of that Cell.
     * @param bits extra bits to set
     */
    public static void setLastDRCDateAndBits(Cell cell, Date date, int bits)
    {
        long iVal = date.getTime();
        Integer [] dateArray = new Integer[2];
        dateArray[0] = new Integer((int)(iVal >> 32));
        dateArray[1] = new Integer((int)(iVal & 0xFFFFFFFF));
        cell.newVar(DRC_LAST_GOOD_DATE, dateArray);
        Integer b = new Integer(bits);
        cell.newVar(DRC_LAST_GOOD_BIT, b);
    }

    /**
     * Method to clean any DRC date stored previously
     * @param cell the cell to clean
     */
    private static void cleanDRCDateAndBits(Cell cell)
    {
        cell.delVar(DRC_LAST_GOOD_DATE);
        cell.delVar(DRC_LAST_GOOD_BIT);
    }

    public static int getActiveBits(Technology tech)
    {
        int bits = 0;
        if (!isIgnoreAreaChecking()) bits |= DRC_BIT_AREA;
        if (!isIgnoreExtensionRuleChecking()) bits |= DRC_BIT_COVERAGE;
        // Adding foundry to bits set
        Foundry foundry = tech.getSelectedFoundry();
        switch(foundry.getType())
        {
            case MOSIS:
                bits |= DRC_BIT_MOSIS_FOUNDRY;
                break;
            case TSMC:
                bits |= DRC_BIT_TSMC_FOUNDRY;
                break;
            case ST:
                bits |= DRC_BIT_ST_FOUNDRY;
                break;
        }
        return bits;
    }


    /****************************** OPTIONS ******************************/

	private static Pref cacheIncrementalDRCOn = Pref.makeBooleanPref("IncrementalDRCOn", tool.prefs, true);
	/**
	 * Method to tell whether DRC should be done incrementally.
	 * The default is "true".
	 * @return true if DRC should be done incrementally.
	 */
	public static boolean isIncrementalDRCOn() { return cacheIncrementalDRCOn.getBoolean(); }
	/**
	 * Method to set whether DRC should be done incrementally.
	 * @param on true if DRC should be done incrementally.
	 */
	public static void setIncrementalDRCOn(boolean on) { cacheIncrementalDRCOn.setBoolean(on); }

	private static Pref cacheErrorCheckLevel = Pref.makeIntPref("ErrorCheckLevel", tool.prefs,
            DRCCheckMode.ERROR_CHECK_DEFAULT.mode());
	/**
	 * Method to retrieve checking level in DRC
	 * The default is "ERROR_CHECK_DEFAULT".
	 * @return integer representing error type
	 */
	public static DRC.DRCCheckMode getErrorType()
    {
        int val = cacheErrorCheckLevel.getInt();
        for (DRCCheckMode p : DRCCheckMode.values())
        {
            if (p.mode() == val) return p;
        }
        return null;
    }

	/**
	 * Method to set DRC error type.
	 * @param type representing error level
	 */
	public static void setErrorType(DRC.DRCCheckMode type) { cacheErrorCheckLevel.setInt(type.mode()); }

	private static Pref cacheUseMultipleThreads = Pref.makeBooleanPref("UseMultipleThreads", tool.prefs, false);
	/**
	 * Method to tell whether DRC should use multiple threads.
	 * The default is "false".
	 * @return true if DRC should use multiple threads.
	 */
	public static boolean isUseMultipleThreads() { return cacheUseMultipleThreads.getBoolean(); }
	/**
	 * Method to set whether DRC should use multiple threads.
	 * @param on true if DRC should use multiple threads.
	 */
	public static void setUseMultipleThreads(boolean on) { cacheUseMultipleThreads.setBoolean(on); }

	private static Pref cacheNumberOfThreads = Pref.makeIntPref("NumberOfThreads", tool.prefs, 2);
	/**
	 * Method to return the number of threads to use when running DRC with multiple threads.
	 * The default is 2.
	 * @return the number of threads to use when running DRC with multiple threads.
	 */
	public static int getNumberOfThreads() { return cacheNumberOfThreads.getInt(); }
	/**
	 * Method to set the number of threads to use when running DRC with multiple threads.
	 * @param th the number of threads to use when running DRC with multiple threads.
	 */
	public static void setNumberOfThreads(int th) { cacheNumberOfThreads.setInt(th); }

	private static Pref cacheIgnoreCenterCuts = Pref.makeBooleanPref("IgnoreCenterCuts", tool.prefs, false);
//    static { cacheIgnoreCenterCuts.attachToObject(tool, "Tools/DRC tab", "DRC ignores center cuts in large contacts"); }
	/**
	 * Method to tell whether DRC should ignore center cuts in large contacts.
	 * Only the perimeter of cuts will be checked.
	 * The default is "false".
	 * @return true if DRC should ignore center cuts in large contacts.
	 */
	public static boolean isIgnoreCenterCuts() { return cacheIgnoreCenterCuts.getBoolean(); }
	/**
	 * Method to set whether DRC should ignore center cuts in large contacts.
	 * Only the perimeter of cuts will be checked.
	 * @param on true if DRC should ignore center cuts in large contacts.
	 */
	public static void setIgnoreCenterCuts(boolean on) { cacheIgnoreCenterCuts.setBoolean(on); }

    private static Pref cacheIgnoreAreaChecking = Pref.makeBooleanPref("IgnoreAreaCheck", tool.prefs, false);
//    static { cacheIgnoreAreaChecking.attachToObject(tool, "Tools/DRC tab", "DRC ignores area checking"); }
	/**
	 * Method to tell whether DRC should ignore minimum/enclosed area checking.
	 * The default is "false".
	 * @return true if DRC should ignore minimum/enclosed area checking.
	 */
	public static boolean isIgnoreAreaChecking() { return cacheIgnoreAreaChecking.getBoolean(); }
	/**
	 * Method to set whether DRC should ignore minimum/enclosed area checking.
	 * @param on true if DRC should ignore minimum/enclosed area checking.
	 */
	public static void setIgnoreAreaChecking(boolean on) { cacheIgnoreAreaChecking.setBoolean(on); }

    private static Pref cacheIgnoreExtensionRuleChecking = Pref.makeBooleanPref("IgnoreExtensionRuleCheck", tool.prefs, false);
//    static { cacheIgnoreExtensionRuleChecking.attachToObject(tool, "Tools/DRC tab", "DRC extension rule checking"); }
	/**
	 * Method to tell whether DRC should check extension rules.
	 * The default is "false".
	 * @return true if DRC should check extension rules.
	 */
	public static boolean isIgnoreExtensionRuleChecking() { return cacheIgnoreExtensionRuleChecking.getBoolean(); }
	/**
	 * Method to set whether DRC should check extension rules.
	 * @param on true if DRC should check extension rules.
	 */
	public static void setIgnoreExtensionRuleChecking(boolean on) { cacheIgnoreExtensionRuleChecking.setBoolean(on); }
//	public static final Variable.Key POSTSCRIPT_FILEDATE = Variable.newKey("IO_postscript_filedate");

    /**
	 * Class to save good DRC dates in a new thread.
	 */
	public static class UpdateDRCDates extends Job
	{
		HashMap<Cell,Date> goodDRCDate;
		HashMap<Cell,Cell> cleanDRCDate;
        int activeBits;

		public UpdateDRCDates(int bits, HashMap<Cell,Date> goodDRCDate, HashMap<Cell,Cell> cleanDRCDate)
		{
			super("Remember DRC Successes and/or Delete Obsolete Dates", tool, Type.CHANGE, null, null, Priority.USER);
            this.goodDRCDate = goodDRCDate;
			this.cleanDRCDate = cleanDRCDate;
            this.activeBits = bits;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			for(Iterator<Cell> it = goodDRCDate.keySet().iterator(); it.hasNext(); )
			{
				Cell cell = it.next();
				Date now = goodDRCDate.get(cell);
				setLastDRCDateAndBits(cell, now, activeBits);
			}

			for(Iterator<Cell> it = cleanDRCDate.keySet().iterator(); it.hasNext(); )
			{
				Cell cell = it.next();
                if (!cell.isLinked())
                    new JobException("Cell '" + cell + "' is invalid");
				cleanDRCDateAndBits(cell);
			}
			return true;
		}
	}
}
