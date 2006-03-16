/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ExplorerTree.java
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
package com.sun.electric.tool.user.ui;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.EpicAnalysis;
import com.sun.electric.tool.project.AddCellJob;
import com.sun.electric.tool.project.AddLibraryJob;
import com.sun.electric.tool.project.CancelCheckOutJob;
import com.sun.electric.tool.project.CheckInJob;
import com.sun.electric.tool.project.CheckOutJob;
import com.sun.electric.tool.project.DeleteCellJob;
import com.sun.electric.tool.project.HistoryDialog;
import com.sun.electric.tool.project.Project;
import com.sun.electric.tool.project.UpdateJob;
import com.sun.electric.tool.simulation.AnalogSignal;
import com.sun.electric.tool.simulation.Analysis;
import com.sun.electric.tool.simulation.Signal;
import com.sun.electric.tool.user.CircuitChanges;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.Resources;
import com.sun.electric.tool.user.ViewChanges;
import com.sun.electric.tool.user.dialogs.ChangeCellGroup;
import com.sun.electric.tool.user.dialogs.NewCell;
import com.sun.electric.tool.user.menus.CellMenu;
import com.sun.electric.tool.user.menus.FileMenu;
import com.sun.electric.tool.user.tecEdit.Manipulate;
import com.sun.electric.tool.user.waveform.SweepSignal;
import com.sun.electric.tool.user.waveform.WaveformWindow;

import java.awt.Component;
import java.awt.Font;
import java.awt.Image;
import java.awt.Point;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragGestureRecognizer;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.*;

/**
 * Class to display a cell explorer tree-view of the database.
 */
public class ExplorerTree extends JTree implements DragGestureListener, DragSourceListener
{
    private final static TreePath[] NULL_TREE_PATH_ARRAY = {};
    
    private TreeHandler handler = null;
	private final String rootNode;
	private Object [] currentSelectedObjects = NULL_TREE_PATH_ARRAY;
    private TreePath [] currentSelectedPaths = new TreePath[0];

	private static class IconGroup
	{
		/** the icon for a normal cell */					private ImageIcon regular;
		/** the icon for an old version of a cell */		private ImageIcon old;
		/** the icon for a checked-in cell */				private ImageIcon available;
		/** the icon for a cell checked-out to others */	private ImageIcon locked;
		/** the icon for a cell checked-out to you */		private ImageIcon unlocked;
	}
	private static ImageIcon iconLibrary = null;
	private static ImageIcon iconGroup = null;
	private static ImageIcon iconJobs = null;
	private static ImageIcon iconLibraries = null;
	private static ImageIcon iconErrors = null;
	private static ImageIcon iconErrorMsg = null;
    private static ImageIcon iconWarnMsg = null;
	private static ImageIcon iconSignals = null;
	private static ImageIcon iconSweeps = null;
	private static ImageIcon iconMeasurements = null;
	private static ImageIcon iconViewMultiPageSchematics = null;
	private static ImageIcon iconSpiderWeb = null;
	private static ImageIcon iconLocked = null;
	private static ImageIcon iconUnlocked = null;
	private static ImageIcon iconAvailable = null;

	/**
	 * Constructor to create a new ExplorerTree.
	 * @param contentNodes the tree to display.
	 */
	ExplorerTree(List<MutableTreeNode> contentNodes)
	{
		super((TreeModel)null);
        setModel(new ExplorerTreeModel());
        rootNode = ExplorerTreeModel.rootNode;
//		ErrorLoggerTree.updateExplorerTree(model().errorExplorerNode);
        redoContentTrees(contentNodes);

		initDND();

        // Starting Job explorer tree expanded
        expandPath(new TreePath(model().jobPath));

		// arbitrary selection in the explorer
		getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);

		// do not show top-level
		setRootVisible(false);
		setShowsRootHandles(true);
		setToggleClickCount(3);

		// enable tool tips - we'll use these to display useful info
		ToolTipManager.sharedInstance().registerComponent(this);

		// register our own extended renderer for custom icons and tooltips
		setCellRenderer(new MyRenderer());
        handler = new TreeHandler();
		addMouseListener(handler);
		addTreeSelectionListener(handler);
	}

	/**
	 * Method to return the currently selected objects in the explorer tree.
	 * @return the currently selected objects in the explorer tree.
	 */
	public Object [] getCurrentlySelectedObject() {
        Object[] selectedObjects = new Object[numCurrentlySelectedObjects()];
        for (int i = 0; i < selectedObjects.length; i++)
            selectedObjects[i] = getCurrentlySelectedObject(i);
        return selectedObjects;
    }

	/**
	 * Method to return the number of currently selected objects in the explorer tree.
	 * @return the number of currently selected objects in the explorer tree.
	 */
	public int numCurrentlySelectedObjects() { return currentSelectedPaths.length; }

	/**
	 * Method to return the currently selected object in the explorer tree.
     * @param i index of currently selected object.
	 * @return the currently selected object in the explorer tree.
	 */
	public Object getCurrentlySelectedObject(int i) {
        if (i >= currentSelectedPaths.length) return null;
        TreePath treePath = currentSelectedPaths[i];
        if (treePath == null) return null;
        Object obj = treePath.getLastPathComponent();
        if (obj instanceof DefaultMutableTreeNode)
            return ((DefaultMutableTreeNode)obj).getUserObject();
        if (obj instanceof EpicAnalysis.EpicTreeNode) {
            Signal sig = EpicAnalysis.getSignal(treePath);
            if (sig != null)
                return sig;
        }
        return obj;
    }

	/**
	 * Method to set the currently selected object in the explorer tree.
	 * param obj the currently selected object in the explorer tree.
	 */
	public void clearCurrentlySelectedObjects()
	{
		currentSelectedPaths = NULL_TREE_PATH_ARRAY;
	}

	/**
	 * Method to return the currently selected object in the explorer tree.
	 * @return the currently selected object in the explorer tree.
	 */
	public ExplorerTreeModel model() { return (ExplorerTreeModel)treeModel; }

	/**
	 * Method to force the explorer tree to show the current library or signals list.
     * @param lib library to expand
     * @param cell
     * @param openLib true to open the current library, false to open the signals list.
     */
	void openLibraryInExplorerTree(Library lib, Cell cell, boolean openLib) {
        int count = -1; // starts from EXPLORER node
        openLibraryInExplorerTree(lib, cell, new TreePath(rootNode), openLib, count);
    }

    /**
     * Method to count rows to given cell considering possible cell groups and versions.
     * @param cell
     * @param treeModel
     * @param path
     * @param node
     * @param count
     * @return
     */
    private int countChildrenAndExpandInPath(Cell cell, TreeModel treeModel, TreePath path, Object node, int count)
    {
        int numChildren = treeModel.getChildCount(node);
        for (int i = 0; i < numChildren; i++)
        {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode)treeModel.getChild(node, i);
            count++;
            Object obj = child.getUserObject();

            if (obj == cell)
                return count; // found location in library

            // Obj represents the latest version of the given cell and the given cell is an older version.
            // treeModel.getChildCount(child) > 0 otherwise it will go down every single version.
            if (treeModel.getChildCount(child) > 0 && obj instanceof Cell && ((Cell)obj).getCellGroup() == cell.getCellGroup())
            {
                TreePath descentPath = path.pathByAddingChild(child);
                expandPath(descentPath);
                return countChildrenAndExpandInPath(cell, treeModel, descentPath, child, count);
            }
            // Obj represents the cell group
            if (obj == cell.getCellGroup())
            {
                TreePath descentPath = path.pathByAddingChild(child);
                expandPath(descentPath);
                return countChildrenAndExpandInPath(cell, treeModel, descentPath, child, count);
            }

        }
        return count;
    }

	/**
	 * Method to recursively scan the explorer tree and open the current library or signals list.
     * @param library the library to open
     * @param cell
     * @param path the current position in the explorer tree.
     * @param openLib true for libraries, false for waveforms
     */
	private boolean openLibraryInExplorerTree(Library library, Cell cell, TreePath path, boolean openLib, int count)
	{
        TreeModel treeModel = model();
        Object node = path.getLastPathComponent();
        Object obj = node;
        if (obj instanceof DefaultMutableTreeNode)
            obj = ((DefaultMutableTreeNode)obj).getUserObject();
		int numChildren = treeModel.getChildCount(node);
		if (numChildren == 0) return false;

		if (openLib && (obj instanceof Library))
		{
			Library lib = (Library)obj;
            // Only expands library and its node. Doesn't contineu with rest of nodes in Explorer
			if (lib == library)
            {
                expandPath(path);
                // Counting position from library to cell selected
                if (cell != null)
                {
                    count = countChildrenAndExpandInPath(cell, treeModel, path, node, count);
                }
                setSelectionRow(count);
                return true; // found location in explorer
            }
		} else if (obj instanceof String)
		{
			String msg = (String)obj;
			if ((msg.equalsIgnoreCase("libraries") && openLib) ||
				(msg.equalsIgnoreCase("signals") && !openLib))
					expandPath(path);
		}

		// now recurse
		for(int i=0; i<numChildren; i++)
		{
            Object child = treeModel.getChild(node, i);
            if (!(child instanceof DefaultMutableTreeNode)) continue;
			TreePath descentPath = path.pathByAddingChild(child);
			if (descentPath == null) continue;
            count++;
			if (openLibraryInExplorerTree(library, cell, descentPath, openLib, count))
                return true;
		}
        return false;
	}

	void redoContentTrees(List<MutableTreeNode> contentNodes)
	{
        assert SwingUtilities.isEventDispatchThread();

		// remember the state of the tree
		ArrayList<TreePath> expanded = new ArrayList<TreePath>();
		recursivelyCacheExpandedPaths(expanded, new TreePath(rootNode));
        model().updateContentExplorerNodes(contentNodes);
        expandCachedPaths(expanded);
	}

    /**
     * Recursively
     */
    private void recursivelyCacheExpandedPaths(ArrayList<TreePath> expanded, TreePath path) {
        
        Object node = path.getLastPathComponent();
        if (!isExpanded(path))
            return;
        expanded.add(path);
        
        // now recurse
        for(int i=0; i< treeModel.getChildCount(node); i++) {
            Object child = treeModel.getChild(node, i);
            if (treeModel.isLeaf(child)) continue;
            TreePath descentPath = path.pathByAddingChild(child);
            if (descentPath == null) continue;
            recursivelyCacheExpandedPaths(expanded, descentPath);
        }
    }
    
    private void expandCachedPaths(ArrayList<TreePath> expanded) {
        for (TreePath path: expanded)
            expandCachedPath(path);
    }
    
    private void expandCachedPath(TreePath oldPath) {
        Object[] path = oldPath.getPath();
        TreePath newPath = new TreePath(rootNode);
        Object topNode = rootNode;
        for (int i = 1; i < path.length; i++) {
            Object oldChild = path[i];
            Object newChild = null;
            if (oldChild instanceof DefaultMutableTreeNode)
                newChild = findChildByUserObject(topNode, ((DefaultMutableTreeNode)oldChild).getUserObject());
            if (newChild == null) {
                int k = treeModel.getIndexOfChild(topNode, oldChild);
                if (k >= 0)
                    newChild = treeModel.getChild(topNode, k);
            }
            if (newChild == null) return;
            topNode = newChild;
            newPath = newPath.pathByAddingChild(topNode);
            expandPath(newPath);
        }
    }
    
    private Object findChildByUserObject(Object parent, Object userObject) {
        for (int i = 0, childCount = treeModel.getChildCount(parent); i < childCount; i++) {
            Object newChild = treeModel.getChild(parent, i);
            if (!(newChild instanceof DefaultMutableTreeNode)) continue;
            if (((DefaultMutableTreeNode)newChild).getUserObject().equals(userObject))
                return newChild;
        }
        return null;
    }

	public String convertValueToText(Object value, boolean selected, boolean expanded, boolean leaf,
		int row, boolean hasFocus)
	{
        if (!(value instanceof DefaultMutableTreeNode))
            return value.toString();
		Object nodeInfo = ((DefaultMutableTreeNode)value).getUserObject();
		if (nodeInfo instanceof Cell)
		{
			Cell cell = (Cell)nodeInfo;
			if (cell.isSchematic())
			{
				Cell.CellGroup group = cell.getCellGroup();
				Cell mainSchematic = group.getMainSchematics();
				int numSchematics = 0;
				for(Iterator<Cell> gIt = group.getCells(); gIt.hasNext(); )
				{
					Cell cellInGroup = gIt.next();
					if (cellInGroup.isSchematic()) numSchematics++;
				}
				if (numSchematics > 1 && cell == mainSchematic)
					return cell.noLibDescribe() + " **";
			}
			return cell.noLibDescribe();
		}
		if (nodeInfo instanceof Library)
		{
			Library lib = (Library)nodeInfo;
			String nodeName = lib.getName();
			if (lib == Library.getCurrent() && Library.getNumLibraries() > 1)
			{
				nodeName += " [Current]";
				iconLibrary = Resources.getResource(getClass(), "IconLibraryCheck.gif");
			}
			else
			{
				iconLibrary = Resources.getResource(getClass(), "IconLibrary.gif");
			}
			return nodeName;
		}
		if (nodeInfo instanceof Cell.CellGroup)
		{
			Cell.CellGroup group = (Cell.CellGroup)nodeInfo;
            return group.getName();
		}
		if (nodeInfo instanceof ErrorLoggerTree.ErrorLoggerTreeNode)
		{
            ErrorLoggerTree.ErrorLoggerTreeNode node = (ErrorLoggerTree.ErrorLoggerTreeNode)nodeInfo;
			ErrorLogger el = (ErrorLogger)node.getLogger();
			String s = el.getSystem();
            if (ErrorLoggerTree.currentLogger != null && node == ErrorLoggerTree.currentLogger.getUserObject())
                s += " [Current]";
            return s;
		}
        if (nodeInfo instanceof ErrorLogger.MessageLog)
        {
            ErrorLogger.MessageLog el = (ErrorLogger.MessageLog)nodeInfo;
            return el.getMessage();
        }
		if (nodeInfo instanceof Signal)
		{
			Signal sig = (Signal)nodeInfo;
			return sig.getSignalName();
		}
		if (nodeInfo == null) return "";
		return nodeInfo.toString();
	}

	// *********************************** DRAG AND DROP ***********************************

	/** Variables needed for DnD */
	private DragSource dragSource = null;
//	private DefaultMutableTreeNode selectedNode;

	private void initDND()
	{
		dragSource = DragSource.getDefaultDragSource();
		DragGestureRecognizer dgr = dragSource.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_LINK, this);
	}

	public void dragGestureRecognized(DragGestureEvent e)
	{
		if (numCurrentlySelectedObjects() == 0) return;

		// handle signal dragging when in a WaveformWindow setting
		if (getCurrentlySelectedObject(0) instanceof Signal)
		{
			// Get the Transferable Object
			StringBuffer buf = new StringBuffer();
			for(int i=0; i<numCurrentlySelectedObjects(); i++)
			{
				Signal sSig = (Signal)getCurrentlySelectedObject(i);
				String sigName = sSig.getFullName();
				if (sSig instanceof AnalogSignal)
				{
					AnalogSignal as = (AnalogSignal)sSig;
					if (as.getAnalysis().getAnalysisType() == Analysis.ANALYSIS_TRANS) sigName = "TRANS " + sigName; else
						if (as.getAnalysis().getAnalysisType() == Analysis.ANALYSIS_AC) sigName = "AC " + sigName; else
							if (as.getAnalysis().getAnalysisType() == Analysis.ANALYSIS_DC) sigName = "DC " + sigName; else
								if (as.getAnalysis().getAnalysisType() == Analysis.ANALYSIS_MEAS) sigName = "MEASUREMENT " + sigName;
				}
				buf.append(sigName);
				buf.append("\n");
			}
			Transferable transferable = new StringSelection(buf.toString());

			// begin the drag
			dragSource.startDrag(e, DragSource.DefaultLinkDrop, transferable, this);
			return;
		}

		if (numCurrentlySelectedObjects() > 1)
		{
			Job.getUserInterface().showErrorMessage("Can drag only one selected object", "Too Much Selected");
			return;
		}

		// Drag cell name to edit window
		if (getCurrentlySelectedObject(0) instanceof Cell)
		{
			// make a Transferable Object
			Cell cell = (Cell)getCurrentlySelectedObject(0);
			EditWindow.NodeProtoTransferable transferable = new EditWindow.NodeProtoTransferable(cell);

			// begin the drag
            if (TopLevel.getOperatingSystem() == TopLevel.OS.MACINTOSH)
            {
                // OS X has problems creating DefaultDragImage
                Image img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
			    dragSource.startDrag(e, DragSource.DefaultLinkDrop, img, new Point(0,0), transferable, this);
            }
            else
			    dragSource.startDrag(e, DragSource.DefaultLinkDrop, transferable, this);
			return;
		}
	}

	public void dragEnter(DragSourceDragEvent e) {}

	public void dragOver(DragSourceDragEvent e) {}

	public void dragExit(DragSourceEvent e) {}

	public void dragDropEnd(DragSourceDropEvent e) {}

	public void dropActionChanged (DragSourceDragEvent e) {}

    
	private class MyRenderer extends DefaultTreeCellRenderer
	{
		private Font plainFont, boldFont, italicFont;

		public MyRenderer()
		{
			plainFont = new Font("arial", Font.PLAIN, 11);
			boldFont = new Font("arial", Font.BOLD, 11);
            italicFont = new Font("arial", Font.ITALIC, 11);
		}

		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
			boolean expanded, boolean leaf, int row, boolean hasFocus)
		{
			super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
			// setIcon(icon)
			//setToolTipText(value.toString());
			setFont(plainFont);
            if (!(value instanceof DefaultMutableTreeNode))
                return this;
            DefaultMutableTreeNode node = (DefaultMutableTreeNode)value;
			Object nodeInfo = node.getUserObject();
			if (nodeInfo instanceof Library)
			{
				Library lib = (Library)nodeInfo;
				if (iconLibrary == null)
					iconLibrary = Resources.getResource(getClass(), "IconLibrary.gif");
				if (lib.isChanged()) setFont(boldFont);
				setIcon(iconLibrary);
			}
			if (nodeInfo instanceof ExplorerTreeModel.CellAndCount)
			{
				ExplorerTreeModel.CellAndCount cc = (ExplorerTreeModel.CellAndCount)nodeInfo;
				nodeInfo = cc.getCell();
                if (cc.getCell().isModified(true)) setFont(boldFont);
                else if (cc.getCell().isModified(false)) setFont(italicFont);
			}
			if (nodeInfo instanceof Cell)
			{
				Cell cell = (Cell)nodeInfo;
                if (cell.isModified(true)) setFont(boldFont);
                else if (cell.isModified(false)) setFont(italicFont);
				IconGroup ig;
				if (cell.isIcon()) ig = findIconGroup(View.ICON); else
					if (cell.getView() == View.LAYOUT) ig = findIconGroup(View.LAYOUT); else
						if (cell.isSchematic()) ig = findIconGroup(View.SCHEMATIC); else
							if (cell.getView().isTextView()) ig = findIconGroup(View.DOC); else
								ig = findIconGroup(View.UNKNOWN);
				if (cell.getNewestVersion() != cell) setIcon(ig.old); else
				{
					switch (Project.getCellStatus(cell))
					{
						case Project.NOTMANAGED:         setIcon(ig.regular);     break;
						case Project.CHECKEDIN:          setIcon(ig.available);   break;
						case Project.CHECKEDOUTTOOTHERS: setIcon(ig.locked);      break;
						case Project.CHECKEDOUTTOYOU:    setIcon(ig.unlocked);    break;
					}
				}
			}
			if (nodeInfo instanceof ExplorerTreeModel.MultiPageCell)
			{
				if (iconViewMultiPageSchematics == null)
					iconViewMultiPageSchematics = Resources.getResource(getClass(), "IconViewMultiPageSchematics.gif");
				setIcon(iconViewMultiPageSchematics);
			}
			if (nodeInfo instanceof Cell.CellGroup)
			{
                Cell.CellGroup cg = (Cell.CellGroup)nodeInfo;
                int status = -1; // hasn't changed , status = 1 -> major change, status = 0 -> minor change
                for (Iterator<Cell> it = cg.getCells(); status != 1 && it.hasNext();)
                {
                    Cell c = it.next();
                    if (c.isModified(true))
                    {
                        status = 1;
                        break;  // no need of checking the rest
                    }
                    else if (c.isModified(false)) status = 0;
                }
                if (status == 1) setFont(boldFont);
                else if (status == 0) setFont(italicFont);
				if (iconGroup == null)
					iconGroup = Resources.getResource(getClass(), "IconGroup.gif");
				setIcon(iconGroup);
			}
			if (nodeInfo instanceof String)
			{
				String theString = (String)nodeInfo;
				if (theString.equalsIgnoreCase("jobs"))
				{
					if (iconJobs == null)
						iconJobs = Resources.getResource(getClass(), "IconJobs.gif");
					setIcon(iconJobs);
				} else if (theString.equalsIgnoreCase("libraries"))
				{
					if (iconLibraries == null)
						iconLibraries = Resources.getResource(getClass(), "IconLibraries.gif");
					setIcon(iconLibraries);
				} else if (theString.equalsIgnoreCase("errors"))
				{
					if (iconErrors == null)
						iconErrors = Resources.getResource(getClass(), "IconErrors.gif");
					setIcon(iconErrors);
				} else if (theString.equalsIgnoreCase("trans signals") || theString.equalsIgnoreCase("ac signals") ||
					theString.equalsIgnoreCase("dc signals"))
				{
					if (iconSignals == null)
						iconSignals = Resources.getResource(getClass(), "IconSignals.gif");
					setIcon(iconSignals);
				} else if (theString.equalsIgnoreCase("trans sweeps") || theString.equalsIgnoreCase("ac sweeps") ||
					theString.equalsIgnoreCase("dc sweeps"))
				{
					if (iconSweeps == null)
						iconSweeps = Resources.getResource(getClass(), "IconSweeps.gif");
					setIcon(iconSweeps);
				} else if (theString.equalsIgnoreCase("measurements"))
				{
					if (iconMeasurements == null)
						iconMeasurements = Resources.getResource(getClass(), "IconMeasurement.gif");
					setIcon(iconMeasurements);
				}
			}
            if (nodeInfo instanceof ErrorLogger.MessageLog)
            {
                ErrorLogger.MessageLog theLog = (ErrorLogger.MessageLog)nodeInfo;
                // Error   WarningLog
                if (theLog instanceof ErrorLogger.WarningLog)
                {
                    if (iconWarnMsg == null)
                        iconWarnMsg = Resources.getResource(getClass(), "IconWarningLog.gif");
                    setIcon(iconWarnMsg);
                } else // warning
                {
                    if (iconErrorMsg == null)
                        iconErrorMsg = Resources.getResource(getClass(), "IconErrorLog.gif");
                    setIcon(iconErrorMsg);
                }
            }
			if (nodeInfo instanceof JobTree.JobTreeNode)
			{
				JobTree.JobTreeNode j = (JobTree.JobTreeNode)nodeInfo;
				//setToolTipText(j.getToolTip());
				//System.out.println("set tool tip to "+j.getToolTip());
			}
			return this;
		}

		private HashMap<View,IconGroup> iconGroups = new HashMap<View,IconGroup>();

		private IconGroup findIconGroup(View view)
		{
			IconGroup ig = iconGroups.get(view);
			if (ig == null)
			{
				ig = new IconGroup();

				// get the appropriate background icon
				if (view == View.LAYOUT) ig.regular = Resources.getResource(getClass(), "IconViewLayout.gif"); else
				if (view == View.SCHEMATIC) ig.regular = Resources.getResource(getClass(), "IconViewSchematics.gif"); else
				if (view == View.ICON) ig.regular = Resources.getResource(getClass(), "IconViewIcon.gif"); else
				if (view == View.DOC) ig.regular = Resources.getResource(getClass(), "IconViewText.gif"); else
				ig.regular = Resources.getResource(getClass(), "IconViewMisc.gif");

				// make sure the overlay icons have been read
				if (iconSpiderWeb == null) iconSpiderWeb = Resources.getResource(getClass(), "IconSpiderWeb.gif");
				if (iconLocked == null) iconLocked = Resources.getResource(getClass(), "IconLocked.gif");
				if (iconUnlocked == null) iconUnlocked = Resources.getResource(getClass(), "IconUnlocked.gif");
				if (iconAvailable == null) iconAvailable = Resources.getResource(getClass(), "IconAvailable.gif");

				ig.old = buildIcon(iconSpiderWeb, ig.regular);
				ig.available = buildIcon(iconAvailable, ig.regular);
				ig.locked = buildIcon(iconLocked, ig.regular);
				ig.unlocked = buildIcon(iconUnlocked, ig.regular);
				iconGroups.put(view, ig);
			}
			return ig;
		}

		private ImageIcon buildIcon(ImageIcon fg, ImageIcon bg)
		{
			// overlay and create the other icons for this view
			int wid = fg.getIconWidth();
			int hei = fg.getIconHeight();
			BufferedImage bi = new BufferedImage(wid, hei, BufferedImage.TYPE_INT_RGB);

			int [] backgroundValues = new int[wid*hei];
			PixelGrabber background = new PixelGrabber(bg.getImage(), 0, 0, wid, hei, backgroundValues, 0, wid);
			int [] foregroundValues = new int[wid*hei];
			PixelGrabber foreground = new PixelGrabber(fg.getImage(), 0, 0, wid, hei, foregroundValues, 0, wid);
			try
			{
				background.grabPixels();
				foreground.grabPixels();
			} catch (InterruptedException e) {}
			for(int y=0; y<hei; y++)
			{
				for(int x=0; x<wid; x++)
				{
					int bCol = backgroundValues[y*wid+x];
					int fCol = foregroundValues[y*wid+x];
					if ((fCol&0xFFFFFF) != 0xFFFFFF) bCol = fCol;
					bi.setRGB(x, y, bCol);
				}
			}
			return new ImageIcon(bi);
		}
	}

	private class TreeHandler implements MouseListener, MouseMotionListener, TreeSelectionListener
	{
		private Cell originalCell;
		private boolean draggingCell;
		private MouseEvent currentMouseEvent;
		private TreePath [] currentPaths;
		private TreePath [] originalPaths;

		public void mouseClicked(MouseEvent e) {}

		public void mouseEntered(MouseEvent e) {}

		public void mouseExited(MouseEvent e) {}

		public void mouseMoved(MouseEvent e) {}

		public void mousePressed(MouseEvent e)
		{
			draggingCell = false;

			// popup menu event (right click)
			if (e.isPopupTrigger())
			{
            	selectTreeElement(e.getX(), e.getY());
                cacheEvent(e);
				doContextMenu();
				return;
			}

            cacheEvent(e);
			WindowFrame wf = WindowFrame.getCurrentWindowFrame();

			// double click
			if (e.getClickCount() == 2)
			{
				// handle things that can accomodate multiple selections
				boolean didSomething = false;
				for(int i=0; i<numCurrentlySelectedObjects(); i++)
				{
					if (getCurrentlySelectedObject(i) instanceof Signal)
					{
						Signal sig = (Signal)getCurrentlySelectedObject(i);
						if (wf.getContent() instanceof WaveformWindow)
						{
							WaveformWindow ww = (WaveformWindow)wf.getContent();
							ww.addSignal(sig);
						}
						didSomething = true;
					}
	
					if (getCurrentlySelectedObject(i) instanceof SweepSignal)
					{
						SweepSignal ss = (SweepSignal)getCurrentlySelectedObject(i);
						if (ss == null) return;
						ss.setIncluded(!ss.isIncluded());
						didSomething = true;
					}
				}
				if (didSomething) return;

				// must have only 1 selection
				if (numCurrentlySelectedObjects() != 1)
				{
					Job.getUserInterface().showErrorMessage("Must select just one entry in the explorer tree", "Too Much Selected");
					return;
				}
				Object nodeObj = getCurrentlySelectedObject(0);

				if (nodeObj instanceof ExplorerTreeModel.CellAndCount)
				{
					ExplorerTreeModel.CellAndCount cc = (ExplorerTreeModel.CellAndCount)nodeObj;
					wf.setCellWindow(cc.getCell());
					return;
				}

				if (nodeObj instanceof Cell)
				{
					Cell cell = (Cell)nodeObj;
					wf.setCellWindow(cell);
					return;
				}
				if (nodeObj instanceof ExplorerTreeModel.MultiPageCell)
				{
					ExplorerTreeModel.MultiPageCell mpc = (ExplorerTreeModel.MultiPageCell)nodeObj;
					Cell cell = mpc.getCell();
					wf.setCellWindow(cell);
					if (wf.getContent() instanceof EditWindow)
					{
						EditWindow wnd = (EditWindow)wf.getContent();
						wnd.setMultiPageNumber(mpc.getPageNo());
					}
					return;
				}

				if (nodeObj instanceof Library || nodeObj instanceof Cell.CellGroup ||
					nodeObj instanceof String || nodeObj instanceof ErrorLoggerTree.ErrorLoggerTreeNode)
				{
					for(int i=0; i<currentPaths.length; i++)
					{
						if (isExpanded(currentPaths[i])) collapsePath(currentPaths[i]); else
							expandPath(currentPaths[i]);
					}
					return;
				}

				if (nodeObj instanceof JobTree.JobTreeNode)
				{
					System.out.println(((JobTree.JobTreeNode)nodeObj).getInfo());
					return;
				}

				if (nodeObj instanceof ErrorLogger.MessageLog)
				{
					ErrorLogger.MessageLog el = (ErrorLogger.MessageLog)nodeObj;
					String msg = Job.getUserInterface().reportLog(el,true, null);
					System.out.println(msg);
					return;
				}

				// dragging: remember original object
				if (nodeObj instanceof Cell)
				{
					Cell cell = (Cell)nodeObj;
					if (cell.getNewestVersion() == cell)
					{
						originalCell = cell;
						originalPaths = new TreePath[currentPaths.length];
						for(int i=0; i<currentPaths.length; i++)
							originalPaths[i] = new TreePath(currentPaths[i].getPath());
						draggingCell = true;
					}
				}
			}
		}

		public void mouseReleased(MouseEvent e)
		{
            // popup menu event (right click)
            if (e.isPopupTrigger())
            {
            	selectTreeElement(e.getX(), e.getY());
                cacheEvent(e);
                doContextMenu();
            }
		}

		public void mouseDragged(MouseEvent e)
		{
			if (!draggingCell) return;
			cacheEvent(e);
			clearSelection();
			for(int i=0; i<originalPaths.length; i++)
				addSelectionPath(originalPaths[i]);
			for(int i=0; i<currentPaths.length; i++)
				addSelectionPath(currentPaths[i]);
			updateUI();
		}

		public void valueChanged(TreeSelectionEvent e)
		{
			currentPaths = getSelectionPaths();
			if (currentPaths == null) currentPaths = new TreePath[0];
			currentSelectedPaths = new TreePath[currentPaths.length];
			for(int i=0; i<currentPaths.length; i++)
			{
                currentSelectedPaths[i] = currentPaths[i];
//                Object obj = currentPaths[i].getLastPathComponent();
//                if (obj instanceof DefaultMutableTreeNode)
//                    obj = ((DefaultMutableTreeNode)obj).getUserObject();
//                currentSelectedObjects[i] = obj;
//				DefaultMutableTreeNode node = (DefaultMutableTreeNode)currentPaths[i].getLastPathComponent();
//				currentSelectedObjects[i] = node.getUserObject();
			}

			// update highlighting to match this selection
			for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
			{
				WindowFrame wf = it.next();
				if (wf.getExplorerTab() == ExplorerTree.this)
				{
					// initiate crossprobing from WaveformWindow 
					if (wf.getContent() instanceof WaveformWindow)
					{
						WaveformWindow ww = (WaveformWindow)wf.getContent();
						ww.crossProbeWaveformToEditWindow();
					}
				}
			}
		}
		
		private void selectTreeElement(int x, int y)
		{
			TreePath cp = getPathForLocation(x, y);
			if (cp != null)
			{
                Object obj = cp.getLastPathComponent();
                if (obj instanceof DefaultMutableTreeNode)
                    obj = ((DefaultMutableTreeNode)obj).getUserObject();
//                DefaultMutableTreeNode node = (DefaultMutableTreeNode)cp.getLastPathComponent();
//                Object obj = node.getUserObject();
				boolean selected = false;
				for(int i=0; i<numCurrentlySelectedObjects(); i++)
				{
					if (getCurrentlySelectedObject(i) == obj) { selected = true;   break; }
				}
				if (!selected)
				{
					currentSelectedPaths = new TreePath[1];
					currentSelectedPaths[0] = cp;
					clearSelection();
					addSelectionPath(cp);
					updateUI();
				}
			}
		}

		private void cacheEvent(MouseEvent e)
		{
//			currentPath = getPathForLocation(e.getX(), e.getY());
//			if (currentPath == null) { currentSelectedObject = null;   return; }
//			setSelectionPath(currentPath);
//			for(int i=0; i<currentPaths.length; i++)
//			{
//				DefaultMutableTreeNode node = (DefaultMutableTreeNode)currentPaths[i].getLastPathComponent();
//				if (i == 0) selectedNode = node;
//				Object newSelection = node.getUserObject();
//				if (newSelection != currentSelectedObject)
//				{
//					currentSelectedObject = newSelection;
//					// update highlighting to match this selection
//					for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
//					{
//						WindowFrame wf = it.next();
//						if (wf.getExplorerTab() == tree)
//						{
//							// initiate crossprobing from WaveformWindow 
//							if (wf.getContent() instanceof WaveformWindow)
//							{
//								WaveformWindow ww = (WaveformWindow)wf.getContent();
//								ww.crossProbeWaveformToEditWindow();
//							}
//						}
//					}
//				}
//			}

			
			// determine the source of this event
			currentMouseEvent = e;
		}

		private void doContextMenu()
		{
			// see what is selected
			Object selectedObject = null;
			for(int i=0; i<numCurrentlySelectedObjects(); i++)
			{
				if (selectedObject == null) selectedObject = getCurrentlySelectedObject(i); else
				{
					Class clz = selectedObject.getClass();
					if (!clz.isInstance(getCurrentlySelectedObject(i)))
					{
						Job.getUserInterface().showErrorMessage("All selected entries must be of the same type", "Incorrect Selection");
						return;
					}
				}
			}

			// handle actions that allow multiple selections
			if (selectedObject instanceof SweepSignal)
			{
				JPopupMenu menu = new JPopupMenu("Sweep Signal");

				JMenuItem menuItem = new JMenuItem("Include");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { setSweepAction(true); } });

				menuItem = new JMenuItem("Exclude");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { setSweepAction(false); } });

				menuItem = new JMenuItem("Highlight");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { highlightSweepAction(); } });

				menu.show((Component)currentMouseEvent.getSource(), currentMouseEvent.getX(), currentMouseEvent.getY());
				return;
			}

			if (selectedObject instanceof Signal)
			{
				JPopupMenu menu = new JPopupMenu("Signals");

				JMenuItem menuItem = new JMenuItem("Add to current panel");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { addToWaveform(false); } });

				menuItem = new JMenuItem("Add in new panel");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { addToWaveform(true); } });

				menu.show((Component)currentMouseEvent.getSource(), currentMouseEvent.getX(), currentMouseEvent.getY());
				return;
			}

			// restricted set of options when multiple things are selected
			if (numCurrentlySelectedObjects() > 1)
			{
				if (selectedObject instanceof ExplorerTreeModel.CellAndCount)
				{
					ExplorerTreeModel.CellAndCount cc = (ExplorerTreeModel.CellAndCount)selectedObject;
					selectedObject = cc.getCell();
				}
				if (selectedObject instanceof Cell)
				{
					Cell cell = (Cell)selectedObject;
					JPopupMenu menu = new JPopupMenu("Cell");

					JMenuItem menuItem = new JMenuItem("Delete Cell");
					menu.add(menuItem);
					menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { deleteCellAction(); } });

					JMenu subMenu = new JMenu("Change View");
					menu.add(subMenu);
					for(View view : View.getOrderedViews())
					{
						if (cell.getView() == view) continue;
						JMenuItem subMenuItem = new JMenuItem(view.getFullName());
						subMenu.add(subMenuItem);
						subMenuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { reViewCellAction(e); } });
					}
					menu.show((Component)currentMouseEvent.getSource(), currentMouseEvent.getX(), currentMouseEvent.getY());
					return;
				}
			}

			// restrict to a single selection
			if (numCurrentlySelectedObjects() != 1)
			{
				Job.getUserInterface().showErrorMessage("Must select just one entry in the explorer tree", "Too Much Selected");
				return;
			}

			// show Job menu if user clicked on a Job
			if (selectedObject instanceof JobTree.JobTreeNode)
			{
				JobTree.JobTreeNode job = (JobTree.JobTreeNode)selectedObject;
				JPopupMenu popup = JobTree.getPopupStatus(job);
				popup.show((Component)currentMouseEvent.getSource(), currentMouseEvent.getX(), currentMouseEvent.getY());
				return;
			}
			if (selectedObject instanceof ExplorerTreeModel.CellAndCount)
			{
				ExplorerTreeModel.CellAndCount cc = (ExplorerTreeModel.CellAndCount)selectedObject;
				selectedObject = cc.getCell();
			}
			if (selectedObject instanceof Cell)
			{
				Cell cell = (Cell)selectedObject;
				JPopupMenu menu = new JPopupMenu("Cell");

				JMenuItem menuItem = new JMenuItem("Edit");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { editCellAction(false); } });

				menuItem = new JMenuItem("Edit in New Window");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { editCellAction(true); } });

				int projStatus = Project.getCellStatus(cell);
				if (projStatus != Project.OLDVERSION &&
					(projStatus != Project.NOTMANAGED || Project.isLibraryManaged(cell.getLibrary())))
				{
					menu.addSeparator();

					if (projStatus == Project.CHECKEDIN)
					{
						menuItem = new JMenuItem("Check Out");
						menu.add(menuItem);
						menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { CheckOutJob.checkOut((Cell)getCurrentlySelectedObject(0)); } });
					}
					if (projStatus == Project.CHECKEDOUTTOYOU)
					{
						menuItem = new JMenuItem("Check In...");
						menu.add(menuItem);
						menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { CheckInJob.checkIn((Cell)getCurrentlySelectedObject(0)); } });

						menuItem = new JMenuItem("Rollback and Release Check-Out");
						menu.add(menuItem);
						menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { CancelCheckOutJob.cancelCheckOut((Cell)getCurrentlySelectedObject(0)); } });
					}
					if (projStatus == Project.NOTMANAGED)
					{
						menuItem = new JMenuItem("Add To Repository");
						menu.add(menuItem);
						menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { AddCellJob.addCell((Cell)getCurrentlySelectedObject(0)); } });
					} else
					{
						menuItem = new JMenuItem("Show History of This Cell...");
						menu.add(menuItem);
						menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { HistoryDialog.examineHistory((Cell)getCurrentlySelectedObject(0)); } });
					}
					if (projStatus == Project.CHECKEDIN || projStatus == Project.CHECKEDOUTTOYOU)
					{
						menuItem = new JMenuItem("Remove From Repository");
						menu.add(menuItem);
						menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { DeleteCellJob.removeCell((Cell)getCurrentlySelectedObject(0)); } });
					}
				}

				menu.addSeparator();

				menuItem = new JMenuItem("Place Instance of Cell");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { newCellInstanceAction(); } });

				menuItem = new JMenuItem("Create New Cell");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { newCellAction(); } });

				menu.addSeparator();

				menuItem = new JMenuItem("Create New Version of Cell");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { newCellVersionAction(); } });

				menuItem = new JMenuItem("Duplicate Cell");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { duplicateCellAction(); } });

				menuItem = new JMenuItem("Delete Cell");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { deleteCellAction(); } });

				menu.addSeparator();

				menuItem = new JMenuItem("Rename Cell");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { renameCellAction(); } });

				JMenu subMenu = new JMenu("Change View");
				menu.add(subMenu);
				for(View view : View.getOrderedViews())
				{
					if (cell.getView() == view) continue;
					JMenuItem subMenuItem = new JMenuItem(view.getFullName());
					subMenu.add(subMenuItem);
					subMenuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { reViewCellAction(e); } });
				}

                menu.addSeparator();

				if (cell.isSchematic() && cell.getNewestVersion() == cell &&
					cell.getCellGroup().getMainSchematics() != cell)
				{
					menuItem = new JMenuItem("Make This the Main Schematic");
					menu.add(menuItem);
					menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { makeCellMainSchematic(); }});
				}

                menuItem = new JMenuItem("Change Cell Group...");
                menu.add(menuItem);
                menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { changeCellGroupAction(); }});

				menu.show((Component)currentMouseEvent.getSource(), currentMouseEvent.getX(), currentMouseEvent.getY());
				return;
			}
			if (selectedObject instanceof ExplorerTreeModel.MultiPageCell)
			{
				ExplorerTreeModel.MultiPageCell mpc = (ExplorerTreeModel.MultiPageCell)selectedObject;
				Cell cell = mpc.getCell();
				JPopupMenu menu = new JPopupMenu("Cell");

				JMenuItem menuItem = new JMenuItem("Edit");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { editCellAction(false); } });

				menuItem = new JMenuItem("Edit in New Window");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { editCellAction(true); } });

				menu.addSeparator();

				menuItem = new JMenuItem("Make New Page");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { makeNewSchematicPage(); } });

				menuItem = new JMenuItem("Delete This Page");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { deleteSchematicPage(); } });

				menu.show((Component)currentMouseEvent.getSource(), currentMouseEvent.getX(), currentMouseEvent.getY());
				return;
			}
			if (selectedObject instanceof Library)
			{
				Library lib = (Library)selectedObject;
				JPopupMenu menu = new JPopupMenu("Library");

				JMenuItem menuItem = new JMenuItem("Open");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { openAction(); } });

				menuItem = new JMenuItem("Open all below here");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { recursiveOpenAction(); } });

				menuItem = new JMenuItem("Close all below here");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { recursiveCloseAction(); } });

				if (lib != Library.getCurrent())
				{
					menu.addSeparator();

					menuItem = new JMenuItem("Make This the Current Library");
					menu.add(menuItem);
					menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { setCurLibAction(); } });
				}

				menu.addSeparator();

				if (Project.isLibraryManaged(lib))
				{
					menuItem = new JMenuItem("Update from Repository");
					menu.add(menuItem);
					menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { UpdateJob.updateProject(); } });
				} else
				{
					menuItem = new JMenuItem("Add to Project Management Repository");
					menu.add(menuItem);
					menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { AddLibraryJob.addLibrary((Library)getCurrentlySelectedObject(0)); } });
				}

				menu.addSeparator();

				menuItem = new JMenuItem("Create New Cell");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { newCellAction(); } });

				menu.addSeparator();

				menuItem = new JMenuItem("Rename Library");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { renameLibraryAction(); } });

                menuItem = new JMenuItem("Save Library");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { saveLibraryAction(); } });

				menuItem = new JMenuItem("Close Library");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { closeLibraryAction(); } });

				menu.show((Component)currentMouseEvent.getSource(), currentMouseEvent.getX(), currentMouseEvent.getY());
				return;
			}
			if (selectedObject instanceof Cell.CellGroup)
			{
				JPopupMenu menu = new JPopupMenu("CellGroup");

				JMenuItem menuItem = new JMenuItem("Open");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { openAction(); } });

				menuItem = new JMenuItem("Open all below here");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { recursiveOpenAction(); } });

				menuItem = new JMenuItem("Close all below here");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { recursiveCloseAction(); } });

				menu.addSeparator();

				menuItem = new JMenuItem("Create New Cell");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { newCellAction(); } });

				menuItem = new JMenuItem("Rename Cells in Group");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { renameGroupAction(); } });

				menu.show((Component)currentMouseEvent.getSource(), currentMouseEvent.getX(), currentMouseEvent.getY());
				return;
			}
			if (selectedObject instanceof String)
			{
				String msg = (String)selectedObject;
				if (msg.toLowerCase().endsWith("sweeps"))
				{
					JPopupMenu menu = new JPopupMenu("All Sweeps");

					JMenuItem menuItem = new JMenuItem("Include All");
					menu.add(menuItem);
					menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { setAllSweepsAction(true); } });

					menuItem = new JMenuItem("Exclude All");
					menu.add(menuItem);
					menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { setAllSweepsAction(false); } });

					menu.show((Component)currentMouseEvent.getSource(), currentMouseEvent.getX(), currentMouseEvent.getY());
					return;
				}
                if (msg.equalsIgnoreCase("errors"))
				{
					JPopupMenu menu = new JPopupMenu("Errors");

					JMenuItem menuItem = new JMenuItem("Delete All");
					menu.add(menuItem);
					menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { ErrorLoggerTree.deleteAllLoggers(); } });

                    menuItem = new JMenuItem("Load Logger");
					menu.add(menuItem);
					menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { ErrorLoggerTree.load(); } });

                    menu.show((Component)currentMouseEvent.getSource(), currentMouseEvent.getX(), currentMouseEvent.getY());
                    return;
                }
				if (msg.equalsIgnoreCase("libraries"))
				{
					JPopupMenu menu = new JPopupMenu("Libraries");

					JMenuItem menuItem = new JMenuItem("Open");
					menu.add(menuItem);
					menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { openAction(); } });

					menuItem = new JMenuItem("Open all below here");
					menu.add(menuItem);
					menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { recursiveOpenAction(); } });

					menuItem = new JMenuItem("Close all below here");
					menu.add(menuItem);
					menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { recursiveCloseAction(); } });

					menu.addSeparator();

					menuItem = new JMenuItem("Create New Cell");
					menu.add(menuItem);
					menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { newCellAction(); } });

					menu.addSeparator();

					menuItem = new JMenuItem("Show Cells Alphabetically");
					menu.add(menuItem);
					menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { ExplorerTreeModel.showAlphabeticallyAction(); } });

					menuItem = new JMenuItem("Show Cells by Group");
					menu.add(menuItem);
					menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { ExplorerTreeModel.showByGroupAction(); } });

					menuItem = new JMenuItem("Show Cells by Hierarchy");
					menu.add(menuItem);
					menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { ExplorerTreeModel.showByHierarchyAction(); } });

					menu.addSeparator();

                    menuItem = new JMenuItem("Search");
                    menu.add(menuItem);
                    menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { searchAction(); } });

					menu.show((Component)currentMouseEvent.getSource(), currentMouseEvent.getX(), currentMouseEvent.getY());
					return;
				}
				if (msg.equalsIgnoreCase("TECHNOLOGY LAYERS"))
				{
					JPopupMenu menu = new JPopupMenu("Technology Layers");

					JMenuItem menuItem = new JMenuItem("Add New Layer");
					menu.add(menuItem);
					menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { Manipulate.makeCell(1); } });

					menuItem = new JMenuItem("Reorder Layers");
					menu.add(menuItem);
					menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { Manipulate.reorderPrimitives(1); } });

					menu.show((Component)currentMouseEvent.getSource(), currentMouseEvent.getX(), currentMouseEvent.getY());
					return;
				}
				if (msg.equalsIgnoreCase("TECHNOLOGY ARCS"))
				{
					JPopupMenu menu = new JPopupMenu("Technology Arcs");

					JMenuItem menuItem = new JMenuItem("Add New Arc");
					menu.add(menuItem);
					menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { Manipulate.makeCell(2); } });

					menuItem = new JMenuItem("Reorder Arcs");
					menu.add(menuItem);
					menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { Manipulate.reorderPrimitives(2); } });

					menu.show((Component)currentMouseEvent.getSource(), currentMouseEvent.getX(), currentMouseEvent.getY());
					return;
				}
				if (msg.equalsIgnoreCase("TECHNOLOGY NODES"))
				{
					JPopupMenu menu = new JPopupMenu("Technology Nodes");

					JMenuItem menuItem = new JMenuItem("Add New Node");
					menu.add(menuItem);
					menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { Manipulate.makeCell(3); } });

					menuItem = new JMenuItem("Reorder Nodes");
					menu.add(menuItem);
					menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { Manipulate.reorderPrimitives(3); } });

					menu.show((Component)currentMouseEvent.getSource(), currentMouseEvent.getX(), currentMouseEvent.getY());
					return;
				}
			}
            if (selectedObject instanceof ErrorLoggerTree.ErrorLoggerTreeNode)
            {
                ErrorLoggerTree.ErrorLoggerTreeNode logger = (ErrorLoggerTree.ErrorLoggerTreeNode)selectedObject;
                JPopupMenu p = ErrorLoggerTree.getPopupMenu(logger);
                if (p != null) p.show((Component)currentMouseEvent.getSource(), currentMouseEvent.getX(), currentMouseEvent.getY());
                return;
            }
		}

		private void openAction()
		{
			for(int i=0; i<currentPaths.length; i++)
				expandPath(currentPaths[i]);
		}

		private void recursiveOpenAction()
		{
			for(int i=0; i<currentPaths.length; i++)
				recursivelyOpen(currentPaths[i]);
		}

		private void recursivelyOpen(TreePath path)
		{
			expandPath(path);
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
			int numChildren = node.getChildCount();
			for(int i=0; i<numChildren; i++)
			{
				DefaultMutableTreeNode child = (DefaultMutableTreeNode)node.getChildAt(i);
				TreePath descentPath = path.pathByAddingChild(child);
				recursivelyOpen(descentPath);
			}
		}

		private void recursiveCloseAction()
		{
			for(int i=0; i<currentPaths.length; i++)
				recursivelyClose(currentPaths[i]);
		}

		private void recursivelyClose(TreePath path)
		{
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
			int numChildren = node.getChildCount();
			for(int i=0; i<numChildren; i++)
			{
				DefaultMutableTreeNode child = (DefaultMutableTreeNode)node.getChildAt(i);
				TreePath descentPath = path.pathByAddingChild(child);
				recursivelyClose(descentPath);
			}
			collapsePath(path);
		}

		private void setCurLibAction()
		{
			Library lib = (Library)getCurrentlySelectedObject(0);
			lib.setCurrent();
			WindowFrame.wantToRedoTitleNames();
            WindowFrame.wantToRedoLibraryTree();
			EditWindow.repaintAll();
		}

		private void renameLibraryAction()
		{
			Library lib = (Library)getCurrentlySelectedObject(0);
			CircuitChanges.renameLibrary(lib);
		}

        private void saveLibraryAction()
		{
			Library lib = (Library)getCurrentlySelectedObject(0);
			FileMenu.saveLibraryCommand(lib, FileType.DEFAULTLIB, false, true, false);
		}

		private void closeLibraryAction()
		{
			Library lib = (Library)getCurrentlySelectedObject(0);
			FileMenu.closeLibraryCommand(lib);
		}

		private void renameGroupAction()
		{
			Cell.CellGroup cellGroup = (Cell.CellGroup)getCurrentlySelectedObject(0);
			String defaultName = "";
			if (cellGroup.getNumCells() > 0)
				defaultName = (cellGroup.getCells().next()).getName();
		
			String response = JOptionPane.showInputDialog(ExplorerTree.this, "New name for cells in this group", defaultName);
			if (response == null) return;
			CircuitChanges.renameCellGroupInJob(cellGroup, response);
		}

		private void editCellAction(boolean newWindow)
		{
			Cell cell = null;
			int pageNo = 1;
			if (getCurrentlySelectedObject(0) instanceof Cell)
			{
				cell = (Cell)getCurrentlySelectedObject(0);
			} else if (getCurrentlySelectedObject(0) instanceof ExplorerTreeModel.MultiPageCell)
			{
				ExplorerTreeModel.MultiPageCell mpc = (ExplorerTreeModel.MultiPageCell)getCurrentlySelectedObject(0);
				cell = mpc.getCell();
				pageNo = mpc.getPageNo();
			}
			WindowFrame wf = null;
 			if (newWindow)
			{
				wf = WindowFrame.createEditWindow(cell);
			} else
			{
				wf = WindowFrame.getCurrentWindowFrame();
				wf.setCellWindow(cell);
			}
			if (cell.isMultiPage() && wf.getContent() instanceof EditWindow)
			{
				EditWindow wnd = (EditWindow)wf.getContent();
				wnd.setMultiPageNumber(pageNo);
			}
		}

		private void newCellInstanceAction()
		{
			Cell cell = (Cell)getCurrentlySelectedObject(0);
			if (cell == null) return;
			PaletteFrame.placeInstance(cell, null, false);
		}

		private void newCellAction()
		{
			JFrame jf = TopLevel.getCurrentJFrame();
			NewCell dialog = new NewCell(jf, true);
            assert !Job.BATCHMODE;
			dialog.setVisible(true);
		}

		private void addToWaveform(boolean newPanel)
		{
			WindowFrame wf = WindowFrame.getCurrentWindowFrame();
			if (!(wf.getContent() instanceof WaveformWindow)) return;
			WaveformWindow ww = (WaveformWindow)wf.getContent();
			Signal [] sigs = new Signal[numCurrentlySelectedObjects()];
			for(int i=0; i<numCurrentlySelectedObjects(); i++)
			{
				Signal sig = (Signal)getCurrentlySelectedObject(i);
				sigs[i] = sig;
			}
			ww.showSignals(sigs, newPanel);
		}

		private void setSweepAction(boolean include)
		{
			for(int i=0; i<numCurrentlySelectedObjects(); i++)
			{
				SweepSignal ss = (SweepSignal)getCurrentlySelectedObject(i);
				if (ss == null) continue;
				ss.setIncluded(include);
			}
		}

		private void highlightSweepAction()
		{
			for(int i=0; i<numCurrentlySelectedObjects(); i++)
			{
				SweepSignal ss = (SweepSignal)getCurrentlySelectedObject(i);
				if (ss == null) continue;
				ss.highlight();
			}
		}

		private void setAllSweepsAction(boolean include)
		{
			WindowFrame wf = WindowFrame.getCurrentWindowFrame();
			if (wf == null) return;
			if (wf.getContent() instanceof WaveformWindow)
			{
				WaveformWindow ww = (WaveformWindow)wf.getContent();
				List<SweepSignal> sweeps = ww.getSweepSignals();
				for(SweepSignal ss : sweeps)
				{
					ss.setIncluded(include);
				}
			}
		}

		private void newCellVersionAction()
		{
			Cell cell = (Cell)getCurrentlySelectedObject(0);
			CircuitChanges.newVersionOfCell(cell);
		}

		private void duplicateCellAction()
		{
			Cell cell = (Cell)getCurrentlySelectedObject(0);
            CellMenu.duplicateCell(cell);
		}

		private void deleteCellAction()
		{
			List<Cell> cellsToDelete = new ArrayList<Cell>();
			for(int i=0; i<numCurrentlySelectedObjects(); i++)
			{
				Cell cell = (Cell)getCurrentlySelectedObject(i);
				cellsToDelete.add(cell);
			}
			for(Cell cell : cellsToDelete)
			{
				CircuitChanges.deleteCell(cell, true, false);
			}
		}

		private void renameCellAction()
		{
			Cell cell = (Cell)getCurrentlySelectedObject(0);
			String response = JOptionPane.showInputDialog(ExplorerTree.this, "New name for " + cell, cell.getName());
			if (response == null) return;
			CircuitChanges.renameCellInJob(cell, response);
		}

		private void reViewCellAction(ActionEvent e)
		{
			JMenuItem menuItem = (JMenuItem)e.getSource();
			String viewName = menuItem.getText();
			View newView = View.findView(viewName);
			if (newView != null)
			{
				List<Cell> cellsToReview = new ArrayList<Cell>();
				for(int i=0; i<numCurrentlySelectedObjects(); i++)
				{
					Cell cell = (Cell)getCurrentlySelectedObject(i);
					cellsToReview.add(cell);
				}
				for(Cell cell : cellsToReview)
				{
					ViewChanges.changeCellView(cell, newView);
				}
			}
		}

		private void makeCellMainSchematic()
		{
            Cell cell = (Cell)getCurrentlySelectedObject(0);
            if (cell == null) return;
            cell.getCellGroup().setMainSchematics(cell);
		}

        private void changeCellGroupAction() {
            Cell cell = (Cell)getCurrentlySelectedObject(0);
            if (cell == null) return;
            ChangeCellGroup dialog = new ChangeCellGroup(TopLevel.getCurrentJFrame(), true, cell, cell.getLibrary());
            assert !Job.BATCHMODE;
            dialog.setVisible(true);
        }

        private void makeNewSchematicPage()
        {
        	ExplorerTreeModel.MultiPageCell mpc = (ExplorerTreeModel.MultiPageCell)getCurrentlySelectedObject(0);
            Cell cell = mpc.getCell();
         	if (!cell.isMultiPage())
        	{
        		System.out.println("First turn this cell into a multi-page schematic");
        		return;
        	}
        	int numPages = cell.getNumMultiPages();
    		CellMenu.SetMultiPageJob job = new CellMenu.SetMultiPageJob(cell, numPages+1);
           	EditWindow wnd = EditWindow.needCurrent();
        	if (wnd != null) wnd.setMultiPageNumber(numPages);
        }

        private void deleteSchematicPage()
        {
        	ExplorerTreeModel.MultiPageCell mpc = (ExplorerTreeModel.MultiPageCell)getCurrentlySelectedObject(0);
            Cell cell = mpc.getCell();
         	if (!cell.isMultiPage()) return;
        	int numPages = cell.getNumMultiPages();
         	if (numPages <= 1)
        	{
        		System.out.println("Cannot delete the last page of a multi-page schematic");
        		return;
        	}
         	CellMenu.DeleteMultiPageJob job = new CellMenu.DeleteMultiPageJob(cell, mpc.getPageNo());
        }

        private void searchAction()
        {
            String name = JOptionPane.showInputDialog(ExplorerTree.this, "Name of cell to search","");
			if (name == null) return;
            System.out.println("Searching cell name like " + name);
            Cell cell = Library.findCellInLibraries(name, null, null);
            if (cell != null)
                System.out.println("\t" + cell + " in " + cell.getLibrary());
        }
	}
}
