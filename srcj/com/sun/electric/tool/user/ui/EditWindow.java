/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: EditWindow.java
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

import com.sun.electric.database.change.Undo;
import com.sun.electric.database.geometry.EMath;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.user.ErrorLog;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.awt.Font;
import java.awt.BasicStroke;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.Rectangle;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelListener;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.AdjustmentEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.font.GlyphVector;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 * This class defines an editing window for displaying circuitry.
 * It implements WindowContent, which means it can be in the main part of a window
 * (to the right of the explorer panel).
 */
public class EditWindow extends JPanel
	implements WindowContent, MouseMotionListener, MouseListener, MouseWheelListener, KeyListener, ActionListener
{
	/** the window scale */									private double scale;
	/** the window offset */								private double offx = 0, offy = 0;
	/** the window bounds in database units */				private Rectangle2D databaseBounds;
	/** the size of the window (in pixels) */				private Dimension sz;
	/** the cell that is in the window */					private Cell cell;
	/** Cell's VarContext */                                private VarContext cellVarContext;
	/** the window frame containing this editwindow */      private WindowFrame wf;
	/** the offscreen data for rendering */					private PixelDrawing offscreen = null;
	/** the overall panel with disp area and sliders */		private JPanel overall;

	/** the bottom scrollbar on the edit window. */			private JScrollBar bottomScrollBar;
	/** the right scrollbar on the edit window. */			private JScrollBar rightScrollBar;

	/** true if showing grid in this window */				private boolean showGrid = false;
	/** X spacing of grid dots in this window */			private double gridXSpacing;
	/** Y spacing of grid dots in this window */			private double gridYSpacing;

	/** true if doing object-selection drag */				private boolean doingAreaDrag = false;
	/** starting screen point for drags in this window */	private Point startDrag = new Point();
	/** ending screen point for drags in this window */		private Point endDrag = new Point();

	/** true if drawing popup cloud */                      private boolean showPopupCloud = false;
	/** Strings to write to popup cloud */                  private List popupCloudText;
	/** lower left corner of popup cloud */                 private Point2D popupCloudPoint;

	/** list of windows to redraw (gets synchronized) */	private static List redrawThese = new ArrayList();
	/** true if rendering a window now (synchronized) */	private static boolean runningNow = false;

	private static final int SCROLLBARRESOLUTION = 200;

    /** for drawing selection boxes */	private static final BasicStroke selectionLine = new BasicStroke(
    	1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] {2}, 3);

	// ************************************* CONSTRUCTION *************************************

    // constructor
    private EditWindow(Cell cell, WindowFrame wf)
	{
        this.cell = cell;
        this.wf = wf;
		this.gridXSpacing = User.getDefGridXSpacing();
		this.gridYSpacing = User.getDefGridYSpacing();

		sz = new Dimension(500, 500);
		setSize(sz.width, sz.height);
		setPreferredSize(sz);
		databaseBounds = new Rectangle2D.Double();

        cellHistory = new ArrayList();
        cellHistoryLocation = -1;

		// the total panel in the waveform window
		overall = new JPanel();
		overall.setLayout(new GridBagLayout());

		// the horizontal scroll bar
		int thumbSize = SCROLLBARRESOLUTION / 20;
		bottomScrollBar = new JScrollBar(JScrollBar.HORIZONTAL, SCROLLBARRESOLUTION/2, thumbSize, 0, SCROLLBARRESOLUTION+thumbSize);
		bottomScrollBar.setBlockIncrement(SCROLLBARRESOLUTION / 4);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;   gbc.gridy = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		overall.add(bottomScrollBar, gbc);
		bottomScrollBar.addAdjustmentListener(new ScrollAdjustmentListener(this));
		bottomScrollBar.setValue(bottomScrollBar.getMaximum()/2);

		// the vertical scroll bar in the edit window
		rightScrollBar = new JScrollBar(JScrollBar.VERTICAL, SCROLLBARRESOLUTION/2, thumbSize, 0, SCROLLBARRESOLUTION+thumbSize);
		rightScrollBar.setBlockIncrement(SCROLLBARRESOLUTION / 4);
		gbc = new GridBagConstraints();
		gbc.gridx = 1;   gbc.gridy = 0;
		gbc.fill = GridBagConstraints.VERTICAL;
		overall.add(rightScrollBar, gbc);
		rightScrollBar.addAdjustmentListener(new ScrollAdjustmentListener(this));
		rightScrollBar.setValue(rightScrollBar.getMaximum()/2);

		// put this object's display up
		gbc = new GridBagConstraints();
		gbc.gridx = 0;   gbc.gridy = 0;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = gbc.weighty = 1;
		overall.add(this, gbc);

		//setAutoscrolls(true);
        // add listeners --> BE SURE to remove listeners in finished()
		addKeyListener(this);
		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);
		if (wf != null) setCell(cell, VarContext.globalContext);
	}

	/**
	 * Factory method to create a new EditWindow with a given cell, in a given WindowFrame.
	 * @param cell the cell in this EditWindow.
	 * @param wf the WindowFrame that this EditWindow lives in.
	 * @return the new EditWindow.
	 */
	public static EditWindow CreateElectricDoc(Cell cell, WindowFrame wf)
	{
		EditWindow ui = new EditWindow(cell, wf);
		return ui;
	}

	// ************************************* EVENT LISTENERS *************************************

	/** 
	 * Respond to an action performed, in this case change the current cell
	 * when the user clicks on an entry in the upHierarchy popup menu.
	 */
	public void actionPerformed(ActionEvent e)
	{
		JMenuItem source = (JMenuItem)e.getSource();
		// extract library and cell from string
		Cell cell = (Cell)NodeProto.findNodeProto(source.getText());
		if (cell == null) return;
		setCell(cell, VarContext.globalContext);
	}

	// the MouseListener events
	public void mousePressed(MouseEvent evt)
	{
		EditWindow wnd = (EditWindow)evt.getSource();
		WindowFrame.setCurrentWindowFrame(wnd.wf);

		WindowFrame.curMouseListener.mousePressed(evt);
	}

	public void mouseReleased(MouseEvent evt) { WindowFrame.curMouseListener.mouseReleased(evt); }

	public void mouseClicked(MouseEvent evt) { WindowFrame.curMouseListener.mouseClicked(evt); }

	public void mouseEntered(MouseEvent evt)
	{
		showCoordinates(evt);
		WindowFrame.curMouseListener.mouseEntered(evt);
	}

	public void mouseExited(MouseEvent evt) { WindowFrame.curMouseListener.mouseExited(evt); }

	// the MouseMotionListener events
	public void mouseMoved(MouseEvent evt)
	{
		showCoordinates(evt);
		WindowFrame.curMouseMotionListener.mouseMoved(evt);
	}

	public void mouseDragged(MouseEvent evt)
	{
		showCoordinates(evt);
		WindowFrame.curMouseMotionListener.mouseDragged(evt);
	}

	private void showCoordinates(MouseEvent evt)
	{
		EditWindow wnd = (EditWindow)evt.getSource();
		if (wnd.getCell() == null) StatusBar.setCoordinates(null, wnd.wf); else
		{
			Point2D pt = wnd.screenToDatabase(evt.getX(), evt.getY());
			EditWindow.gridAlign(pt);
			StatusBar.setCoordinates("(" + pt.getX() + "," + pt.getY() + ")", wnd.wf);
		}
	}

	// the MouseWheelListener events
	public void mouseWheelMoved(MouseWheelEvent evt) { WindowFrame.curMouseWheelListener.mouseWheelMoved(evt); }

	// the KeyListener events
	public void keyPressed(KeyEvent evt) { WindowFrame.curKeyListener.keyPressed(evt); }

	public void keyReleased(KeyEvent evt) { WindowFrame.curKeyListener.keyReleased(evt); }

	public void keyTyped(KeyEvent evt) { WindowFrame.curKeyListener.keyTyped(evt); }

	// ************************************* INFORMATION *************************************

	/**
	 * Method to return the top-level JPanel for this EditWindow.
	 * The actual EditWindow object is below the top level, surrounded by scroll bars.
	 * @return the top-level JPanel for this EditWindow.
	 */
	public JPanel getPanel() { return overall; }

	/**
	 * Method to return the current EditWindow.
	 * @return the current EditWindow (null if none).
	 */
	public static EditWindow getCurrent()
	{
		WindowFrame wf = WindowFrame.getCurrentWindowFrame();
		if (wf == null) return null;
		if (wf.getContent() instanceof EditWindow) return (EditWindow)wf.getContent();
		return null;
	}

	/**
	 * Method to return the current EditWindow.
	 * @return the current EditWindow.
	 * If there is none, an error message is displayed and it returns null.
	 */
	public static EditWindow needCurrent()
	{
		WindowFrame wf = WindowFrame.getCurrentWindowFrame();
		if (wf != null)
		{
			if (wf.getContent() instanceof EditWindow) return (EditWindow)wf.getContent();
		}
		System.out.println("There is no current window for this operation");
		return null;
	}

	/**
	 * Method to return the cell that is shown in this window.
	 * @return the cell that is shown in this window.
	 */
	public Cell getCell() { return cell; }

	/**
	 * Method to return the WindowFrame in which this EditWindow resides.
	 * @return the WindowFrame in which this EditWindow resides.
	 */
	public WindowFrame getWindowFrame() { return wf; }

	/**
	 * Method to set the cell that is shown in the window to "cell".
	 */
	public void setCell(Cell cell, VarContext context)
	{
		// by default record history and fillscreen
		// However, when navigating through history, don't want to record new
		// history objects.
		setCell(cell, context, true);
	}

	/**
	 * Method to set the cell that is shown in the window to "cell".
	 */
	private void setCell(Cell cell, VarContext context, boolean addToHistory)
	{
		// record current history before switching to new cell
		saveCurrentCellHistoryState();

		// set new values
		this.cell = cell;
		this.cellVarContext = context;
		Library curLib = Library.getCurrent();
		curLib.setCurCell(cell);
		Highlight.clear();
		Highlight.finished();

		setWindowTitle();
		if (wf != null)
		{
			if (cell != null)
			{
				if (wf == WindowFrame.getCurrentWindowFrame())
				{
					// if auto-switching technology, do it
					PaletteFrame.autoTechnologySwitch(cell);
				}
			}
		}
		fillScreen();

		if (addToHistory) {
			addToHistory(cell, context);
		}

		if (cell != null && User.isCheckCellDates()) cell.checkCellDates();
	}

	/**
	 * Method to set the window title.
	 */
	public void setWindowTitle()
	{
		if (wf == null) return;
		if (cell == null)
		{
			wf.setTitle("***NONE***");
			return;
		}

		String title = cell.describe();
		if (cell.getLibrary() != Library.getCurrent())
			title += " - Current library: " + Library.getCurrent().getLibName();
		wf.setTitle(title);
	}

	/**
	 * Method to find an EditWindow that is displaying a given cell.
	 * @param cell the Cell to find.
	 * @return the EditWindow showing that cell, or null if none found.
	 */
	public static EditWindow findWindow(Cell cell)
	{
		for(Iterator it = WindowFrame.getWindows(); it.hasNext(); )
		{
			WindowFrame wf = (WindowFrame)it.next();
			WindowContent content = wf.getContent();
			if (!(content instanceof EditWindow)) continue;
			if (content.getCell() == cell) return (EditWindow)content;
		}
		return null;
	}

	/**
	 * Method to return the PixelDrawing object that represents the offscreen image
	 * of the Cell in this EditWindow.
	 * @return the offscreen object for this window.
	 */
	public PixelDrawing getOffscreen() { return offscreen; }

	public void loadExplorerTree(DefaultMutableTreeNode rootNode)
	{
		wf.libraryExplorerNode = ExplorerTree.makeLibraryTree();
		wf.jobExplorerNode = Job.getExplorerTree();
		wf.errorExplorerNode = ErrorLog.getExplorerTree();
		wf.signalExplorerNode = null;
		rootNode.add(wf.libraryExplorerNode);
		rootNode.add(wf.jobExplorerNode);
		rootNode.add(wf.errorExplorerNode);
	}

	/**
	 * Method to get rid of this EditWindow.  Called by WindowFrame when
	 * that windowFrame gets closed.
	 */
	public void finished()
	{
		//wf = null;                          // clear reference
		//offscreen = null;                   // need to clear this ref, because it points to this

		// remove myself from listener list
		removeKeyListener(this);
		removeMouseListener(this);
		removeMouseMotionListener(this);
		removeMouseWheelListener(this);
	}

	// ************************************* SCROLLING *************************************

	/**
	 * Method to return the scroll bar resolution.
	 * This is the extent of the JScrollBar.
	 * @return the scroll bar resolution.
	 */
	public static int getScrollBarResolution() { return SCROLLBARRESOLUTION; }

	/**
	 * This class handles changes to the edit window scroll bars.
	 */
	static class ScrollAdjustmentListener implements AdjustmentListener
	{
        /** A weak reference to the WindowFrame */
		EditWindow wnd;               

		ScrollAdjustmentListener(EditWindow wnd)
		{
			super();
			this.wnd = wnd;
//			this.wf = new WeakReference(wf);
		}

		public void adjustmentValueChanged(AdjustmentEvent e)
		{
			if (e.getSource() == wnd.getBottomScrollBar() && wnd.getCell() != null)
				wnd.bottomScrollChanged(e.getValue());
			if (e.getSource() == wnd.getRightScrollBar() && wnd.getCell() != null)
				wnd.rightScrollChanged(e.getValue());
		}
	}

	/**
	 * Method to return the horizontal scroll bar at the bottom of the edit window.
	 * @return the horizontal scroll bar at the bottom of the edit window.
	 */
	public JScrollBar getBottomScrollBar() { return bottomScrollBar; }

	/**
	 * Method to return the vertical scroll bar at the right side of the edit window.
	 * @return the vertical scroll bar at the right side of the edit window.
	 */
	public JScrollBar getRightScrollBar() { return rightScrollBar; }

	// ************************************* REPAINT *************************************

	/**
	 * Method to repaint this EditWindow.
	 * Composites the image (taken from the PixelDrawing object)
	 * with the grid, highlight, and any dragging rectangle.
	 */
	public void paint(Graphics g)
	{
		// to enable keys to be received
		if (cell != null && cell == WindowFrame.getCurrentCell())
			requestFocus();

		// redo the explorer tree if it changed
		wf.redoExplorerTreeIfRequested();

		if (offscreen == null || !getSize().equals(sz))
		{
			setScreenSize(getSize());
			repaintContents();
			return;
		}

		// show the image
		Image img = offscreen.getImage();
		synchronized(img) { g.drawImage(img, 0, 0, this); };

		// overlay other things if there is a valid cell
		if (cell != null)
		{
			// add in grid if requested
			if (showGrid) drawGrid(g);

			// add in the frame if present
			drawCellFrame(g);

			// add in highlighting
			for(Iterator it = Highlight.getHighlights(); it.hasNext(); )
			{
				Highlight h = (Highlight)it.next();
				Cell highCell = h.getCell();
				if (highCell != cell) continue;
				h.showHighlight(this, g);
			}

			// add in drag area
			if (doingAreaDrag) showDragBox(g);
			// add in popup cloud
			if (showPopupCloud) drawPopupCloud((Graphics2D)g);
		}
	}

	public void fullRepaint() { repaintContents(); }

	/**
	 * Method requests that every EditWindow be redrawn, including a rerendering of its contents.
	 */
	public static void repaintAllContents()
	{
		for(Iterator it = WindowFrame.getWindows(); it.hasNext(); )
		{
			WindowFrame wf = (WindowFrame)it.next();
			WindowContent content = wf.getContent();
			if (!(content instanceof EditWindow)) continue;
			EditWindow wnd = (EditWindow)content;
			wnd.repaintContents();
		}
	}

	/**
	 * Method requests that every EditWindow be redrawn, without rerendering the offscreen contents.
	 */
	public static void repaintAll()
	{
		for(Iterator it = WindowFrame.getWindows(); it.hasNext(); )
		{
			WindowFrame wf = (WindowFrame)it.next();
			WindowContent content = wf.getContent();
			if (!(content instanceof EditWindow)) continue;
			EditWindow wnd = (EditWindow)content;
			wnd.repaint();
		}
	}

	/**
	 * Method requests that this EditWindow be redrawn, including a rerendering of the contents.
	 */
	public void repaintContents()
	{
		// start rendering thread
		if (offscreen == null) return;

		// do the redraw in the main thread
//		offscreen.drawImage();
//		repaint();
        setScrollPosition();                        // redraw scroll bars

		// do the redraw in a separate thread
		synchronized(redrawThese)
		{
			if (runningNow)
			{
				if (!redrawThese.contains(this))
					redrawThese.add(this);
				return;
			}
			runningNow = true;
		}
		RenderJob renderJob = new RenderJob(this, offscreen);
	}

	/**
	 * This class queues requests to rerender a window.
	 */
	protected static class RenderJob extends Job
	{
		private EditWindow wnd;
		private PixelDrawing offscreen;

		protected RenderJob(EditWindow wnd, PixelDrawing offscreen)
		{
			super("Display", User.tool, Job.Type.EXAMINE, null, null, Job.Priority.USER);
			this.wnd = wnd;
			this.offscreen = offscreen;
			startJob();
		}

		public void doIt()
		{
			// do the hard work of re-rendering the image
			offscreen.drawImage();

			// see if anything else is queued
			synchronized(redrawThese)
			{
				if (redrawThese.size() > 0)
				{
					EditWindow nextWnd = (EditWindow)redrawThese.get(0);
					redrawThese.remove(0);
					RenderJob nextJob = new RenderJob(nextWnd, nextWnd.getOffscreen());
					return;
				}
				runningNow = false;
			}
			wnd.repaint();
		}
	}

	/**
	 * Special "hook" to render a single node.
	 * This is used by the PaletteWindow to draw nodes that aren't really in the database.
	 */
	public Image renderNode(NodeInst ni, double scale)
	{
		offscreen.clearImage(false);
		setScale(scale);
		offscreen.drawNode(ni, EMath.MATID, true);
		return offscreen.composite();
	}

	/**
	 * Special "hook" to render a single arc.
	 * This is used by the PaletteWindow to draw arcs that aren't really in the database.
	 */
	public Image renderArc(ArcInst ai, double scale)
	{
		offscreen.clearImage(false);
		setScale(scale);
		offscreen.drawArc(ai, EMath.MATID);
		return offscreen.composite();
	}

	// ************************************* DRAG BOX *************************************

	public boolean isDoingAreaDrag() { return doingAreaDrag; }

	public void setDoingAreaDrag() { doingAreaDrag = true; }

	public void clearDoingAreaDrag() { doingAreaDrag = false; }

	public Point getStartDrag() { return startDrag; }

	public void setStartDrag(int x, int y) { startDrag.setLocation(x, y); }

	public Point getEndDrag() { return endDrag; }

	public void setEndDrag(int x, int y) { endDrag.setLocation(x, y); }

	private void showDragBox(Graphics g)
	{
		int lX = (int)Math.min(startDrag.getX(), endDrag.getX());
		int hX = (int)Math.max(startDrag.getX(), endDrag.getX());
		int lY = (int)Math.min(startDrag.getY(), endDrag.getY());
		int hY = (int)Math.max(startDrag.getY(), endDrag.getY());
		Graphics2D g2 = (Graphics2D)g;
		g2.setStroke(selectionLine);
		g.setColor(Color.white);
		g.drawLine(lX, lY, lX, hY);
		g.drawLine(lX, hY, hX, hY);
		g.drawLine(hX, hY, hX, lY);
		g.drawLine(hX, lY, lX, lY);
	}

	// ************************************* CELL FRAME *************************************

	private static final double FRAMESCALE = 18.0;
	private static final double HASCHXSIZE = ( 8.5  * FRAMESCALE);
	private static final double HASCHYSIZE = ( 5.5  * FRAMESCALE);
	private static final double ASCHXSIZE  = (11.0  * FRAMESCALE);
	private static final double ASCHYSIZE  = ( 8.5  * FRAMESCALE);
	private static final double BSCHXSIZE  = (17.0  * FRAMESCALE);
	private static final double BSCHYSIZE  = (11.0  * FRAMESCALE);
	private static final double CSCHXSIZE  = (24.0  * FRAMESCALE);
	private static final double CSCHYSIZE  = (17.0  * FRAMESCALE);
	private static final double DSCHXSIZE  = (36.0  * FRAMESCALE);
	private static final double DSCHYSIZE  = (24.0  * FRAMESCALE);
	private static final double ESCHXSIZE  = (48.0  * FRAMESCALE);
	private static final double ESCHYSIZE  = (36.0  * FRAMESCALE);
	private static final double FRAMEWID   = ( 0.15 * FRAMESCALE);
	private static final double XLOGOBOX   = ( 2.0  * FRAMESCALE);
	private static final double YLOGOBOX   = ( 1.0  * FRAMESCALE);

	/**
	 * Method to determine the size of the schematic frame in the current Cell.
	 * @param d a Dimension in which the size (database units) will be placed.
	 * @return 0: there should be a frame whose size is absolute;
	 * 1: there should be a frame but it combines with other stuff in the cell;
	 * 2: there is no frame.
	 */
	public int getCellFrameInfo(Dimension d)
	{
		Variable var = cell.getVar(User.FRAME_SIZE, String.class);
		if (var == null) return 2;
		String frameInfo = (String)var.getObject();
		if (frameInfo.length() == 0) return 2;
		int retval = 0;
		char chr = frameInfo.charAt(0);
		double wid = 0, hei = 0;
		if (chr == 'x')
		{
			wid = XLOGOBOX + FRAMEWID;   hei = YLOGOBOX + FRAMEWID;
			retval = 1;
		} else
		{
			switch (chr)
			{
				case 'h': wid = HASCHXSIZE;  hei = HASCHYSIZE;  break;
				case 'a': wid = ASCHXSIZE;   hei = ASCHYSIZE;   break;
				case 'b': wid = BSCHXSIZE;   hei = BSCHYSIZE;   break;
				case 'c': wid = CSCHXSIZE;   hei = CSCHYSIZE;   break;
				case 'd': wid = DSCHXSIZE;   hei = DSCHYSIZE;   break;
				case 'e': wid = ESCHXSIZE;   hei = ESCHYSIZE;   break;
			}
		}
		if (frameInfo.indexOf("v") >= 0)
		{
			d.setSize(hei, wid);
		} else
		{
			d.setSize(wid, hei);
		}
		return retval;
	}

	private void drawCellFrame(Graphics g)
	{
		Dimension d = new Dimension();
		int frameFactor = getCellFrameInfo(d);
		if (frameFactor == 2) return;

		Variable var = cell.getVar(User.FRAME_SIZE, String.class);
		if (var == null) return;
		String frameInfo = (String)var.getObject();
		double schXSize = d.getWidth();
		double schYSize = d.getHeight();

		boolean drawTitleBox = true;
		int xSections = 8;
		int ySections = 4;
		if (frameFactor == 1)
		{
			xSections = ySections = 0;
		} else
		{
			if (frameInfo.indexOf("v") >= 0)
			{
				xSections = 4;
				ySections = 8;
			}
			if (frameInfo.indexOf("n") >= 0) drawTitleBox = false;
		}

		double xLogoBox = XLOGOBOX;
		double yLogoBox = YLOGOBOX;
		double frameWid = FRAMEWID;

		// draw the frame
		g.setColor(Color.BLACK);
		if (xSections > 0)
		{
			double xSecSize = (schXSize - frameWid*2) / xSections;
			double ySecSize = (schYSize - frameWid*2) / ySections;

			// draw the outer frame
			Point2D point0 = new Point2D.Double(-schXSize/2, -schYSize/2);
			Point2D point1 = new Point2D.Double(-schXSize/2,  schYSize/2);
			Point2D point2 = new Point2D.Double( schXSize/2,  schYSize/2);
			Point2D point3 = new Point2D.Double( schXSize/2, -schYSize/2);
			showFrameLine(g, point0, point1);
			showFrameLine(g, point1, point2);
			showFrameLine(g, point2, point3);
			showFrameLine(g, point3, point0);

			// draw the inner frame
			point0 = new Point2D.Double(-schXSize/2 + frameWid, -schYSize/2 + frameWid);
			point1 = new Point2D.Double(-schXSize/2 + frameWid,  schYSize/2 - frameWid);
			point2 = new Point2D.Double( schXSize/2 - frameWid,  schYSize/2 - frameWid);
			point3 = new Point2D.Double( schXSize/2 - frameWid, -schYSize/2 + frameWid);
			showFrameLine(g, point0, point1);
			showFrameLine(g, point1, point2);
			showFrameLine(g, point2, point3);
			showFrameLine(g, point3, point0);

			Point2D textSize = deltaDatabaseToScreen(frameWid, frameWid);
			int height = (int)Math.abs(textSize.getY()) - 2;

			// tick marks along the top and bottom sides
			for(int i=0; i<xSections; i++)
			{
				double x = i * xSecSize - (schXSize/2 - frameWid);
				if (i > 0)
				{
					point0 = new Point2D.Double(x, schYSize/2 - frameWid);
					point1 = new Point2D.Double(x, schYSize/2 - frameWid/2);
					showFrameLine(g, point0, point1);
					point0 = new Point2D.Double(x, -schYSize/2 + frameWid);
					point1 = new Point2D.Double(x, -schYSize/2 + frameWid/2);
					showFrameLine(g, point0, point1);
				}

				char chr = (char)('1' + xSections - i - 1);
				point0 = new Point2D.Double(x + xSecSize/2, schYSize/2 - frameWid/2);
				showFrameText(g, point0, height, 0, 0, String.valueOf(chr));

				point0 = new Point2D.Double(x + xSecSize/2, -schYSize/2 + frameWid/2);
				showFrameText(g, point0, height, 0, 0, String.valueOf(chr));
			}

			// tick marks along the left and right sides
			for(int i=0; i<ySections; i++)
			{
				double y = i * ySecSize - (schYSize/2 - frameWid);
				if (i > 0)
				{
					point0 = new Point2D.Double(schXSize/2 - frameWid, y);
					point1 = new Point2D.Double(schXSize/2 - frameWid/2, y);
					showFrameLine(g, point0, point1);
					point0 = new Point2D.Double(-schXSize/2 + frameWid, y);
					point1 = new Point2D.Double(-schXSize/2 + frameWid/2, y);
					showFrameLine(g, point0, point1);
				}
				char chr = (char)('A' + i);
				point0 = new Point2D.Double(schXSize/2 - frameWid/2, y + ySecSize/2);
				showFrameText(g, point0, height, 0, 0, String.valueOf(chr));

				point0 = new Point2D.Double(-schXSize/2 + frameWid/2, y + ySecSize/2);
				showFrameText(g, point0, height, 0, 0, String.valueOf(chr));
			}
		}
		if (drawTitleBox)
		{
			Point2D textSize = deltaDatabaseToScreen(yLogoBox*2/15, yLogoBox*2/15);
			int height = (int)Math.abs(textSize.getY());

			Point2D point0 = new Point2D.Double(schXSize/2 - frameWid - xLogoBox, -schYSize/2 + frameWid + yLogoBox);
			Point2D point1 = new Point2D.Double(schXSize/2 - frameWid, -schYSize/2 + frameWid + yLogoBox);
			Point2D point2 = new Point2D.Double(schXSize/2 - frameWid, -schYSize/2 + frameWid);
			Point2D point3 = new Point2D.Double(schXSize/2 - frameWid - xLogoBox, -schYSize/2 + frameWid);
			showFrameLine(g, point0, point1);
			showFrameLine(g, point1, point2);
			showFrameLine(g, point2, point3);
			showFrameLine(g, point3, point0);
	
			point0 = new Point2D.Double(schXSize/2 - frameWid - xLogoBox, -schYSize/2 + frameWid + yLogoBox*2/15);
			point1 = new Point2D.Double(schXSize/2 - frameWid,            -schYSize/2 + frameWid + yLogoBox*2/15);
			showFrameLine(g, point0, point1);

			point0 = new Point2D.Double(schXSize/2 - frameWid - xLogoBox, -schYSize/2 + frameWid + yLogoBox*4/15);
			point1 = new Point2D.Double(schXSize/2 - frameWid,            -schYSize/2 + frameWid + yLogoBox*4/15);
			showFrameLine(g, point0, point1);

			point0 = new Point2D.Double(schXSize/2 - frameWid - xLogoBox, -schYSize/2 + frameWid + yLogoBox*6/15);
			point1 = new Point2D.Double(schXSize/2 - frameWid,            -schYSize/2 + frameWid + yLogoBox*6/15);
			showFrameLine(g, point0, point1);

			point0 = new Point2D.Double(schXSize/2 - frameWid - xLogoBox, -schYSize/2 + frameWid + yLogoBox*9/15);
			point1 = new Point2D.Double(schXSize/2 - frameWid,            -schYSize/2 + frameWid + yLogoBox*9/15);
			showFrameLine(g, point0, point1);

			point0 = new Point2D.Double(schXSize/2 - frameWid - xLogoBox, -schYSize/2 + frameWid + yLogoBox*12/15);
			point1 = new Point2D.Double(schXSize/2 - frameWid,            -schYSize/2 + frameWid + yLogoBox*12/15);
			showFrameLine(g, point0, point1);

			point0 = new Point2D.Double(schXSize/2 - frameWid - xLogoBox/2, -schYSize/2 + frameWid + yLogoBox*13.5/15);
			showFrameText(g, point0, height, xLogoBox, yLogoBox*3/15, "Name: " + cell.describe());

			String projectName = User.getFrameProjectName();
			Variable pVar = cell.getLibrary().getVar(User.FRAME_PROJECT_NAME, String.class);
			if (pVar != null) projectName = (String)pVar.getObject();
			point0 = new Point2D.Double(schXSize/2 - frameWid - xLogoBox/2, -schYSize/2 + frameWid + yLogoBox*10.5/15);
			showFrameText(g, point0, height, xLogoBox, yLogoBox*3/15, projectName);

			String designerName = User.getFrameDesignerName();
			Variable dVar = cell.getLibrary().getVar(User.FRAME_DESIGNER_NAME, String.class);
			if (dVar != null) designerName = (String)dVar.getObject();
			point0 = new Point2D.Double(schXSize/2 - frameWid - xLogoBox/2, -schYSize/2 + frameWid + yLogoBox*7.5/15);
			showFrameText(g, point0, height, xLogoBox, yLogoBox*3/15, designerName);

			String companyName = User.getFrameCompanyName();
			Variable cVar = cell.getLibrary().getVar(User.FRAME_COMPANY_NAME, String.class);
			if (cVar != null) companyName = (String)cVar.getObject();
			point0 = new Point2D.Double(schXSize/2 - frameWid - xLogoBox/2, -schYSize/2 + frameWid + yLogoBox*5/15);
			showFrameText(g, point0, height, xLogoBox, yLogoBox*2/15, companyName);

			point0 = new Point2D.Double(schXSize/2 - frameWid - xLogoBox/2, -schYSize/2 + frameWid + yLogoBox*3/15);
			showFrameText(g, point0, height, xLogoBox, yLogoBox*2/15, "Created: " + TextUtils.formatDate(cell.getCreationDate()));

			point0 = new Point2D.Double(schXSize/2 - frameWid - xLogoBox/2, -schYSize/2 + frameWid + yLogoBox*1/15);
			showFrameText(g, point0, height, xLogoBox, yLogoBox*2/15, "Revised: " + TextUtils.formatDate(cell.getRevisionDate()));
		}
	}

	/**
	 * Method to draw a line directly to the screen.
	 * This is used when drawing the frame around a cell.
	 * @param g the Graphics context in which to draw.
	 * @param from the starting point of the line.
	 * @param to the ending point of the line.
	 */
	private void showFrameLine(Graphics g, Point2D from, Point2D to)
	{
		Point f = databaseToScreen(from);
		Point t = databaseToScreen(to);
		g.drawLine(f.x, f.y, t.x, t.y);
	}

	/**
	 * Method to draw text directly to the screen.
	 * This is used when drawing the frame around a cell.
	 * @param g the Graphics context in which to draw.
	 * @param ctr the center point of the text.
	 * @param initialHeight the text height to use (may get scaled down to fit).
	 * @param maxWid the maximum width of the text (in database units).
	 * @param maxHei the maximum height of the text (in database units).
	 * @param string the text to draw.
	 */
	private void showFrameText(Graphics g, Point2D ctr, int initialHeight, double maxWid, double maxHei, String string)
	{
		Font font = new Font(User.getDefaultFont(), Font.PLAIN, initialHeight);
		g.setFont(font);

		FontRenderContext frc = new FontRenderContext(null, true, true);
		GlyphVector gv = font.createGlyphVector(frc, string);
		LineMetrics lm = font.getLineMetrics(string, frc);
		Rectangle rect = gv.getOutline(0, (float)(lm.getAscent()-lm.getLeading())).getBounds();
		double width = rect.width;
		double height = lm.getHeight();
		Point2D databaseSize = deltaScreenToDatabase((int)width, (int)height);
		double dbWidth = Math.abs(databaseSize.getX());
		double dbHeight = Math.abs(databaseSize.getY());
		if (maxWid > 0 && maxHei > 0 && (dbWidth > maxWid || dbHeight > maxHei))
		{
			double scale = Math.min(maxWid / dbWidth, maxHei / dbHeight);
			font = new Font(User.getDefaultFont(), Font.PLAIN, (int)(initialHeight*scale));
			if (font != null)
			{
				gv = font.createGlyphVector(frc, string);
				lm = font.getLineMetrics(string, frc);
				rect = gv.getOutline(0, (float)(lm.getAscent()-lm.getLeading())).getBounds();
				width = rect.width;
				height = lm.getHeight();
			}
		}

		Graphics2D g2 = (Graphics2D)g;
		Point p = databaseToScreen(ctr);
		g2.drawGlyphVector(gv, (float)(p.x - width/2), (float)(p.y + height/2 - lm.getDescent()));
	}

	// ************************************* GRID *************************************

	/**
	 * Method to set the display of a grid in this window.
	 * @param showGrid true to show the grid.
	 */
	public void setGrid(boolean showGrid)
	{
		this.showGrid = showGrid;
		repaint();
	}

	/**
	 * Method to return the state of grid display in this window.
	 * @return true if the grid is displayed in this window.
	 */
	public boolean isGrid() { return showGrid; }

	/**
	 * Method to return the distance between grid dots in the X direction.
	 * @return the distance between grid dots in the X direction.
	 */
	public double getGridXSpacing() { return gridXSpacing; }
	/**
	 * Method to set the distance between grid dots in the X direction.
	 * @param spacing the distance between grid dots in the X direction.
	 */
	public void setGridXSpacing(double spacing) { gridXSpacing = spacing; }

	/**
	 * Method to return the distance between grid dots in the Y direction.
	 * @return the distance between grid dots in the Y direction.
	 */
	public double getGridYSpacing() { return gridYSpacing; }
	/**
	 * Method to set the distance between grid dots in the Y direction.
	 * @param spacing the distance between grid dots in the Y direction.
	 */
	public void setGridYSpacing(double spacing) { gridYSpacing = spacing; }

	/**
	 * Method to return a rectangle in database coordinates that covers the viewable extent of this window.
	 * @return a rectangle that describes the viewable extent of this window (database coordinates).
	 */
	public Rectangle2D displayableBounds()
	{
		Point2D low = screenToDatabase(0, 0);
		Point2D high = screenToDatabase(sz.width-1, sz.height-1);
		Rectangle2D bounds = new Rectangle2D.Double(low.getX(), high.getY(), high.getX()-low.getX(), low.getY()-high.getY());
		return bounds;
	}

	/**
	 * Method to display the grid.
	 */
	private void drawGrid(Graphics g)
	{
		/* grid spacing */
		int x0 = (int)gridXSpacing;
		int y0 = (int)gridYSpacing;
		if (x0 == 0 || y0 == 0)
		{
			System.out.println("Warning: grid space too small. Please set to 1.");
			return;
		}

		// bold dot spacing
		int xspacing = User.getDefGridXBoldFrequency();
		int yspacing = User.getDefGridYBoldFrequency();

		/* object space extent */
		Rectangle2D displayable = displayableBounds();
		double x4 = displayable.getMinX();  double y4 = displayable.getMaxY();
		double x5 = displayable.getMaxX();  double y5 = displayable.getMinY();

		/* initial grid location */
		int x1 = ((int)x4) / x0 * x0;
		int y1 = ((int)y4) / y0 * y0;

		int xnum = sz.width;
		double xden = x5 - x4;
		int ynum = sz.height;
		double yden = y5 - y4;
		int x10 = x0*xspacing;
		int y10 = y0*yspacing;
		int y1base = y1 - (y1 / y0 * y0);
		int x1base = x1 - (x1 / x0 * x0);

		/* adjust grid placement according to scale */
		boolean fatdots = false;
		if (x0 * xnum / xden < 5 || y0 * ynum / (-yden) < 5)
		{
			x1 = x1base - (x1base - x1) / x10 * x10;   x0 = x10;
			y1 = y1base - (y1base - y1) / y10 * y10;   y0 = y10;
			if (x0 * xnum / xden < 10 || y0 * ynum / (-yden) < 10) return;
		} else if (x0 * xnum / xden > 75 && y0 * ynum / (-yden) > 75)
		{
			fatdots = true;
		}

		/* draw the grid to the offscreen buffer */
		g.setColor(new Color(User.getColorGrid()));
		for(int i = y1; i > y5; i -= y0)
		{
			int y = (int)((i-y4) * ynum / yden);
			if (y < 0 || y > sz.height) continue;
			int y10mod = (i-y1base) % y10;
			for(int j = x1; j < x5; j += x0)
			{
				int x = (int)((j-x4) * xnum / xden);
				boolean everyTen = ((j-x1base)%x10) == 0 && y10mod == 0;
				if (fatdots && everyTen)
				{
					g.fillRect(x-2,y, 5, 1);
					g.fillRect(x,y-2, 1, 5);
					g.fillRect(x-1,y-1, 3, 3);
					continue;
				}

				/* special case every 10 grid points in each direction */
				if (fatdots || everyTen)
				{
					g.fillRect(x-1,y, 3, 1);
					g.fillRect(x,y-1, 1, 3);
					continue;
				}

				// just a single dot
				g.fillRect(x,y, 1, 1);
			}
		}
	}

	// *************************** SEARCHING FOR TEXT ***************************

	/** list of all found strings in the cell */		private List foundInCell;
	/** the currently reported string */				private StringsInCell currentStringInCell;
	/** the currently reported string index */			private int currentFindPosition;

	/**
	 * Class to define a string found in a cell.
	 */
	private static class StringsInCell
	{
		/** the object that the string resides on */	Object object;
		/** the Variable that the string resides on */	Variable.Key key;
		/** the Name that the string resides on */		Name name;
		/** the original string. */						String theLine;
		/** the line number in arrayed variables */		int lineInVariable;
		/** the starting character position */			int startPosition;
		/** the ending character position */			int endPosition;
		/** true if the replacement has been done */	boolean replaced;

		StringsInCell(Object object, Variable.Key key, Name name, int lineInVariable, String theLine, int startPosition, int endPosition)
		{
			this.object = object;
			this.key = key;
			this.name = name;
			this.lineInVariable = lineInVariable;
			this.theLine = theLine;
			this.startPosition = startPosition;
			this.endPosition = endPosition;
			this.replaced = false;
		}

		public String toString() { return "StringsInCell obj="+object+" var="+key+
			" name="+name+" line="+lineInVariable+" start="+startPosition+" end="+endPosition+" msg="+theLine; }
	}

	/**
	 * Method to initialize for a new text search.
	 * @param search the string to locate.
	 * @param caseSensitive true to match only where the case is the same.
	 */
	public void initTextSearch(String search, boolean caseSensitive)
	{
		foundInCell = new ArrayList();
		for(Iterator it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			Name name = ni.getNameKey();
			if (!name.isTempname())
			{
				findAllMatches(ni, null, 0, name, name.toString(), foundInCell, search, caseSensitive);
			}
			addVariableTextToList(ni, foundInCell, search, caseSensitive);
		}
		for(Iterator it = cell.getArcs(); it.hasNext(); )
		{
			ArcInst ai = (ArcInst)it.next();
			Name name = ai.getNameKey();
			if (!name.isTempname())
			{
				findAllMatches(ai, null, 0, name, name.toString(), foundInCell, search, caseSensitive);
			}
			addVariableTextToList(ai, foundInCell, search, caseSensitive);
		}
		for(Iterator it = cell.getPorts(); it.hasNext(); )
		{
			Export pp = (Export)it.next();
			addVariableTextToList(pp, foundInCell, search, caseSensitive);
		}
		for(Iterator it = cell.getVariables(); it.hasNext(); )
		{
			Variable var = (Variable)it.next();
			if (!var.isDisplay()) continue;
			findAllMatches(null, var, -1, null, var.getPureValue(-1, -1), foundInCell, search, caseSensitive);
		}
		currentFindPosition = -1;
		currentStringInCell = null;
	}

	/**
	 * Method to find the next occurrence of a string.
	 * @param reverse true to find in the reverse direction.
	 * @return true if something was found.
	 */
	public boolean findNextText(boolean reverse)
	{
		if (foundInCell == null || foundInCell.size() == 0)
		{
			currentStringInCell = null;
			return false;
		}
		if (reverse)
		{
			currentFindPosition--;
			if (currentFindPosition < 0) currentFindPosition = foundInCell.size()-1;
		} else
		{
			currentFindPosition++;
			if (currentFindPosition >= foundInCell.size()) currentFindPosition = 0;
		}
		currentStringInCell = (StringsInCell)foundInCell.get(currentFindPosition);

		Highlight.clear();
		if (currentStringInCell.object == null)
		{
			Highlight.addText(cell, cell, (Variable)currentStringInCell.object, null);
		} else
		{
			ElectricObject eObj = (ElectricObject)currentStringInCell.object;
			Variable var = eObj.getVar(currentStringInCell.key);
			Highlight.addText(eObj, cell, var, currentStringInCell.name);
		}
		Highlight.finished();
		return true;		
	}

	/**
	 * Method to replace the text that was just selected with findNextText().
	 * @param replace the new text to replace.
	 */
	public void replaceText(String replace)
	{
		if (currentStringInCell == null) return;
		ReplaceTextJob job = new ReplaceTextJob(this, replace);
	}

	/**
	 * Method to replace all selected text.
	 * @param replace the new text to replace everywhere.
	 */
	public void replaceAllText(String replace)
	{
		ReplaceAllTextJob job = new ReplaceAllTextJob(this, replace);
	}

	/**
	 * Method to all all displayable variable strings to the list of strings in the Cell.
	 * @param eObj the ElectricObject on which variables should be examined.
	 * @param foundInCell the list of strings found in the cell.
	 * @param search the string to find on the text.
	 * @param caseSensitive true to do a case-sensitive search.
	 */
	private void addVariableTextToList(ElectricObject eObj, List foundInCell, String search, boolean caseSensitive)
	{
		for(Iterator it = eObj.getVariables(); it.hasNext(); )
		{
			Variable var = (Variable)it.next();
			if (!var.isDisplay()) continue;
			Object obj = var.getObject();
			if (obj instanceof String)
			{
				findAllMatches(eObj, var, -1, null, (String)obj, foundInCell, search, caseSensitive);
			} else if (obj instanceof String[])
			{
				String [] strings = (String [])obj;
				for(int i=0; i<strings.length; i++)
				{
					findAllMatches(eObj, var, i, null, strings[i], foundInCell, search, caseSensitive);
				}
			}
		}
	}

	/**
	 * Method to find all strings on a given database string, and add matches to the list.
	 * @param object the Object on which the string resides.
	 * @param variable the Variable on which the string resides.
	 * @param lineInVariable the line number in arrayed variables.
	 * @param name the name on which the string resides.
	 * @param theLine the actual string from the database.
	 * @param foundInCell the list of found strings.
	 * @param search the string to find.
	 * @param caseSensitive true to do a case-sensitive search.
	 */
	private static void findAllMatches(Object object, Variable variable, int lineInVariable, Name name, String theLine, List foundInCell,
		String search, boolean caseSensitive)
	{
		for(int startPos = 0; ; )
		{
			startPos = TextUtils.findStringInString(theLine, search, startPos, caseSensitive, false);
			if (startPos < 0) break;
			int endPos = startPos + search.length();
			Variable.Key key = null;
			if (variable != null) key = variable.getKey();
			foundInCell.add(new StringsInCell(object, key, name, lineInVariable, theLine, startPos, endPos));
			startPos = endPos;
		}
	}

	/**
	 * Class to change text in a new thread.
	 */
	private static class ReplaceTextJob extends Job
	{
		EditWindow wnd;
		String replace;

		public ReplaceTextJob(EditWindow wnd, String replace)
		{
			super("Replace Text", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.wnd = wnd;
			this.replace = replace;
			startJob();
		}

		public void doIt()
		{
			wnd.changeOneText(wnd.currentStringInCell, replace, wnd.cell);
		}
	}

	/**
	 * Class to change text in a new thread.
	 */
	private static class ReplaceAllTextJob extends Job
	{
		EditWindow wnd;
		String replace;

		public ReplaceAllTextJob(EditWindow wnd, String replace)
		{
			super("Replace All Text", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.wnd = wnd;
			this.replace = replace;
			startJob();
		}

		public void doIt()
		{
			int total = 0;
			for(wnd.currentFindPosition = 0; wnd.currentFindPosition < wnd.foundInCell.size(); wnd.currentFindPosition++)
			{
				wnd.currentStringInCell = (StringsInCell)wnd.foundInCell.get(wnd.currentFindPosition);
				wnd.changeOneText(wnd.currentStringInCell, replace, wnd.cell);
				total++;
			}
			if (total == 0)
			{
				Toolkit.getDefaultToolkit().beep();
			} else
			{
				System.out.println("Replaced " + total + " times");
			}
		}
	}

	/**
	 * Method to change a string to another.
	 * @param sic the string being replaced.
	 * @param rep the new string.
	 * @param cell the Cell in which these strings reside.
	 */
	private void changeOneText(StringsInCell sic, String rep, Cell cell)
	{
		if (sic.replaced) return;
		sic.replaced = true;
		String oldString = sic.theLine;
		String newString = oldString.substring(0, sic.startPosition) + rep + oldString.substring(sic.endPosition);
		if (sic.object == null)
		{
			// cell variable name name
			cell.updateVar(sic.key, newString);
		} else
		{
			if (sic.key == null)
			{
				if (sic.name == null)
				{
					// export name
					Export pp = (Export)sic.object;
					pp.setProtoName(newString);
					Undo.redrawObject(pp.getOriginalPort().getNodeInst());					
				} else
				{
					// node or arc name
					Geometric geom = (Geometric)sic.object;
					geom.setName(newString);
					Undo.redrawObject(geom);					
				}
			} else
			{
				// text on a variable
				ElectricObject base = (ElectricObject)sic.object;
				Variable var = base.getVar(sic.key);
				Object obj = var.getObject();
				Variable newVar = null;
				if (obj instanceof String)
				{
					base.updateVar(sic.key, newString);
				} else if (obj instanceof String[])
				{
					String [] oldLines = (String [])obj;
					String [] newLines = new String[oldLines.length];
					for(int i=0; i<oldLines.length; i++)
					{
						if (i == sic.lineInVariable) newLines[i] = newString; else
							newLines[i] = oldLines[i];
					}
					base.updateVar(sic.key, newLines);
				}
			}
		}

		int delta = rep.length() - (sic.endPosition - sic.startPosition);
		if (delta != 0)
		{
			// because the replacement changes the line length, must update other search strings
			for(Iterator it = foundInCell.iterator(); it.hasNext(); )
			{
				StringsInCell oSIC = (StringsInCell)it.next();
				if (oSIC == sic) continue;
				if (oSIC.object != sic.object) continue;
				if (oSIC.key != sic.key) continue;
				if (oSIC.name != sic.name) continue;
				if (oSIC.lineInVariable != sic.lineInVariable) continue;

				// part of the same string: update it
				oSIC.theLine = newString;
				if (oSIC.startPosition > sic.startPosition)
				{
					oSIC.startPosition += delta;
					oSIC.endPosition += delta;
				}
			}
		}
	}

    // ************************************* POPUP CLOUD *************************************

    public boolean getShowPopupCloud() { return showPopupCloud; }

    public void setShowPopupCloud(List text, Point2D point)
    {
        showPopupCloud = true;
        popupCloudText = text;
        popupCloudPoint = point;
    }

    public void clearShowPopupCloud() { showPopupCloud = false; }

    private void drawPopupCloud(Graphics2D g)
    {
        // JKG NOTE: disabled for now
        // TODO: decide whether or not this is useful
        /*
        if (popupCloudText == null || popupCloudText.size() == 0) return;
        // draw cloud
        float yspacing = 5;
        float x = (float)popupCloudPoint.getX() + 25;
        float y = (float)popupCloudPoint.getY() + 10 + yspacing;
        for (int i=0; i<popupCloudText.size(); i++) {
            GlyphVector glyph = getFont().createGlyphVector(g.getFontRenderContext(), (String)popupCloudText.get(i));
            g.drawGlyphVector(glyph, x, y);
            y += glyph.getVisualBounds().getHeight() + yspacing;
        }
        */
    }

	// ************************************* WINDOW ZOOM AND PAN *************************************

	/**
	 * Method to return the size of this EditWindow.
	 * @return a Dimension with the size of this EditWindow.
	 */
	public Dimension getScreenSize() { return sz; }

	/**
	 * Method to change the size of this EditWindow.
	 * Also reallocates the offscreen data.
	 */
	public void setScreenSize(Dimension sz)
	{
		this.sz = sz;
		offscreen = new PixelDrawing(this);
	}

	/**
	 * Method to return the scale factor for this window.
	 * @return the scale factor for this window.
	 */
	public double getScale() { return scale; }

	/**
	 * Method to set the scale factor for this window.
	 * @param scale the scale factor for this window.
	 */
	public void setScale(double scale)
	{
		this.scale = scale;
		computeDatabaseBounds();
	}

	/**
	 * Method to return the offset factor for this window.
	 * @return the offset factor for this window.
	 */
	public Point2D getOffset() { return new Point2D.Double(offx, offy); }

	/**
	 * Method to set the offset factor for this window.
	 * @param off the offset factor for this window.
	 */
	public void setOffset(Point2D off)
	{
		offx = off.getX();   offy = off.getY();
		computeDatabaseBounds();
	}

	private void computeDatabaseBounds()
	{
		double width = sz.width/scale;
		double height = sz.height/scale;
		databaseBounds.setRect(offx - width/2, offy - height/2, width, height);
	}

	public Rectangle2D getDisplayedBounds() { return databaseBounds; }

	/**
	 * Method called when the bottom scrollbar changes.
	 */
	public void bottomScrollChanged()
	{
		if (cell == null) return;

		// get the bounds of the cell in database coordinates
		Rectangle2D bounds = cell.getBounds();
		double xWidth = bounds.getWidth();
		double xCenter = bounds.getCenterX();

		// get the current thumb position
		int xThumbPos = bottomScrollBar.getValue();

		// figure out what the thumb position SHOULD be
		int scrollBarResolution = EditWindow.getScrollBarResolution();
		double scaleFactor = scrollBarResolution / 4;
		int computedXThumbPos = (int)((offx - xCenter) / xWidth * scaleFactor) + scrollBarResolution/2;

		// adjust the screen if there is change
		if (computedXThumbPos != xThumbPos)
		{
			offx = (xThumbPos-scrollBarResolution/2)/scaleFactor * xWidth + xCenter;
			computeDatabaseBounds();
			repaintContents();
		}
	}

	/**
	 * Method called when the right scrollbar changes.
	 */
	public void rightScrollChanged()
	{
		if (cell == null) return;

		// get the bounds of the cell in database coordinates
		Rectangle2D bounds = cell.getBounds();
		double yHeight = bounds.getHeight();
		double yCenter = bounds.getCenterY();

		// get the current thumb position
		int yThumbPos = rightScrollBar.getValue();
        System.out.println("right.getValue() is "+rightScrollBar.getValue());

		// figure out what the thumb position SHOULD be
		int scrollBarResolution = EditWindow.getScrollBarResolution();
		double scaleFactor = scrollBarResolution / 4;
		int computedYThumbPos = (int)((yCenter - offy) / yHeight * scaleFactor) + scrollBarResolution/2;

		// adjust the screen if there is change
		if (computedYThumbPos != yThumbPos)
		{
			offy = yCenter - (yThumbPos - scrollBarResolution/2) / scaleFactor * yHeight;
			computeDatabaseBounds();
			repaintContents();
		}
	}

	/**
	 * Method to update the scrollbars on the sides of the edit window
	 * so they reflect the true offset of the circuit.
	 */
	private void setScrollPositionOLD()
	{
		rightScrollBar.setEnabled(cell != null);
		bottomScrollBar.setEnabled(cell != null);
		if (cell != null)
		{
			int scrollBarResolution = EditWindow.getScrollBarResolution();
			double scaleFactor = scrollBarResolution / 4;

			Rectangle2D bounds = cell.getBounds();
			double xWidth = bounds.getWidth();
			double xCenter = bounds.getCenterX();
			int xThumbPos = (int)((offx - xCenter) / xWidth * scaleFactor) + scrollBarResolution/2;
			bottomScrollBar.setValue(xThumbPos);

			double yHeight = bounds.getHeight();
			double yCenter = bounds.getCenterY();
			int yThumbPos = (int)((yCenter - offy) / yHeight * scaleFactor) + scrollBarResolution/2;
			rightScrollBar.setValue(yThumbPos);
		}
	}

    private Rectangle2D lastOverallBounds = null;
    private Point2D lastOffset = null;
    private static final double scrollPagePercent = 0.2;
    /**
     * New version of setScrollPosition.  Attempts to provides means of scrolling
     * out of cell bounds.
     */
    public void setScrollPosition()
    {
        bottomScrollBar.setEnabled(cell != null);
        rightScrollBar.setEnabled(cell != null);

        if (cell == null) return;

        Rectangle2D cellBounds = cell.getBounds();
        Rectangle2D viewBounds = displayableBounds();

        // get bounds of cell including what is on-screen
        Rectangle2D overallBounds = cellBounds.createUnion(viewBounds);

        if (lastOverallBounds != null && lastOffset != null) {
            // special case, we don't need to redraw if everything the same
            if (lastOverallBounds.equals(overallBounds) && lastOffset.equals(getOffset()))
                return;
        }

        // adjust scroll bars to reflect new bounds (only if not being adjusted now)
        // newValue, newThumbSize, newMin, newMax
        if (!bottomScrollBar.getValueIsAdjusting()) {
            bottomScrollBar.getModel().setRangeProperties(
                    (int)(offx-0.5*viewBounds.getWidth()),
                    (int)viewBounds.getWidth(),
                    (int)(overallBounds.getX() - scrollPagePercent*overallBounds.getWidth()),
                    (int)((overallBounds.getX()+overallBounds.getWidth()) + scrollPagePercent*overallBounds.getWidth()),
                    false);
            bottomScrollBar.setEnabled(false);
            bottomScrollBar.setUnitIncrement((int)(0.05*viewBounds.getWidth()));
            bottomScrollBar.setBlockIncrement((int)(scrollPagePercent*viewBounds.getWidth()));
            bottomScrollBar.setEnabled(true);
        }
        if (!rightScrollBar.getValueIsAdjusting()) {
            //System.out.println("overallBounds="+overallBounds);
            //System.out.println("cellBounds="+cellBounds);
            //System.out.println("offy="+offy);
            //System.out.print(" value="+(int)(-offy-0.5*cellBounds.getHeight()));
            //System.out.print(" extent="+(int)(cellBounds.getHeight()));
            //System.out.print(" min="+(int)(-((overallBounds.getY()+overallBounds.getHeight()) + 0.125*overallBounds.getHeight())));
            //System.out.println(" max="+(int)(-(overallBounds.getY() - 0.125*overallBounds.getHeight())));
            rightScrollBar.getModel().setRangeProperties(
                    (int)(-offy-0.5*viewBounds.getHeight()),
                    (int)(viewBounds.getHeight()),
                    (int)(-((overallBounds.getY()+overallBounds.getHeight()) + scrollPagePercent*overallBounds.getHeight())),
                    (int)(-(overallBounds.getY() - scrollPagePercent*overallBounds.getHeight())),
                    false);
            //System.out.println("model is "+rightScrollBar.getModel());
            rightScrollBar.setEnabled(false);
            rightScrollBar.setUnitIncrement((int)(0.05*viewBounds.getHeight()));
            rightScrollBar.setBlockIncrement((int)(scrollPagePercent*viewBounds.getHeight()));
            rightScrollBar.setEnabled(true);
        }
        if (!bottomScrollBar.getValueIsAdjusting() && !rightScrollBar.getValueIsAdjusting()) {
            lastOverallBounds = overallBounds;
            lastOffset = getOffset();
        }
    }

    public void bottomScrollChanged(int value)
    {
        Rectangle2D cellBounds = cell.getBounds();
        Rectangle2D viewBounds = displayableBounds();
        double newoffx = value+0.5*viewBounds.getWidth();           // new offset
        double ignoreDelta = 0.03*viewBounds.getWidth();             // ignore delta
        double delta = newoffx - offx;
        //System.out.println("Old offx="+offx+", new offx="+newoffx+", delta="+(newoffx-offx));
        //System.out.println("will ignore delta offset of "+ignoreDelta);
        if (Math.abs(delta) < Math.abs(ignoreDelta)) return;
        Point2D offset = new Point2D.Double(newoffx, offy);
        setOffset(offset);
        repaintContents();
    }

    public void rightScrollChanged(int value)
    {
        Rectangle2D cellBounds = cell.getBounds();
        Rectangle2D viewBounds = displayableBounds();
        double newoffy = -(value+0.5*viewBounds.getHeight());
        // annoying cause +y is down in java
        double ignoreDelta = 0.03*viewBounds.getHeight();             // ignore delta
        double delta = newoffy - offy;
        //System.out.println("Old offy="+offy+", new offy="+newoffy+", deltay="+(newoffy-offy));
        //System.out.println("will ignore delta offset of "+ignoreDelta);
        if (Math.abs(delta) < Math.abs(ignoreDelta)) return;
        Point2D offset = new Point2D.Double(offx, newoffy);
        setOffset(offset);
        repaintContents();
    }

	private void setScreenBounds(Rectangle2D bounds)
	{
		double width = bounds.getWidth();
		double height = bounds.getHeight();
		if (width == 0) width = 2;
		if (height == 0) height = 2;
		double scalex = sz.width/width * 0.9;
		double scaley = sz.height/height * 0.9;
		scale = Math.min(scalex, scaley);
		offx = bounds.getCenterX();
		offy = bounds.getCenterY();
	}

	/**
	 * Method to focus the screen so that an area fills it.
	 * @param bounds the area to make fill the screen.
	 */
	public void focusScreen(Rectangle2D bounds)
	{
		if (bounds == null) return;
		setScreenBounds(bounds);
		setScrollPosition();
		computeDatabaseBounds();
		repaintContents();
	}

	/**
	 * Method to pan and zoom the screen so that the entire cell is displayed.
	 */
	public void fillScreen()
	{
		if (cell != null)
		{
			if (!cell.getView().isTextView())
			{
				Rectangle2D cellBounds = cell.getBounds();
				Dimension d = new Dimension();
				int frameFactor = getCellFrameInfo(d);
				Rectangle2D frameBounds = new Rectangle2D.Double(-d.getWidth()/2, -d.getHeight()/2, d.getWidth(), d.getHeight());
				if (frameFactor == 0)
				{
					cellBounds = frameBounds;
				} else
				{
					if (cellBounds.getWidth() == 0 && cellBounds.getHeight() == 0)
					{
						int defaultCellSize = 60;
						cellBounds = new Rectangle2D.Double(cellBounds.getCenterX()-defaultCellSize/2,
							cellBounds.getCenterY()-defaultCellSize/2, defaultCellSize, defaultCellSize);
					}
		
					// make sure text fits
					setScreenBounds(cellBounds);
					Rectangle2D relativeTextBounds = cell.getRelativeTextBounds(this);
					if (relativeTextBounds != null)
					{
						Rectangle2D newCellBounds = new Rectangle2D.Double();
						Rectangle2D.union(relativeTextBounds, cellBounds, newCellBounds);
						cellBounds = newCellBounds;
					}

					// make sure title box fits (if there is just a title box)
					if (frameFactor == 1)
					{
						Rectangle2D.union(frameBounds, cellBounds, frameBounds);
						cellBounds = frameBounds;
					}
				}
				focusScreen(cellBounds);
				return;
			}
		}
 		repaint();
	}

	public void zoomOutContents()
	{
		double scale = getScale();
		setScale(scale / 2);
		repaintContents();
	}

	public void zoomInContents()
	{
		double scale = getScale();
		setScale(scale * 2);
		repaintContents();
	}

	public void focusOnHighlighted()
	{
		// focus on highlighting
		Rectangle2D bounds = Highlight.getHighlightedArea(this);
		focusScreen(bounds);
	}

    // ************************************* HIERARCHY TRAVERSAL *************************************

    /**
     * Get the window's VarContext
     * @return the current VarContext
     */
    public VarContext getVarContext() { return cellVarContext; }

    /** 
     * Push into an instance (go down the hierarchy)
     */
    public void downHierarchy() {

        // get highlighted
        Highlight h = Highlight.getOneHighlight();
        if (h == null) return;
        ElectricObject eobj = h.getElectricObject();

        NodeInst ni = null;
        PortInst pi = null;
        // see if a nodeinst was highlighted (true if text on nodeinst was highlighted)
        if (eobj instanceof NodeInst) {
            ni = (NodeInst)eobj;
        }
        // see if portinst was highlighted
        if (eobj instanceof PortInst) {
            pi = (PortInst)eobj;
            ni = pi.getNodeInst();
        }
        if (ni == null) {
            System.out.println("Must select a Node to descend into");
            return;
        }
        NodeProto np = ni.getProto();
        if (!(np instanceof Cell)) {
            System.out.println("Can only descend into cell instances");
            return;
        }
        Cell cell = (Cell)np;
        Cell schCell = cell.getEquivalent();
        // special case: if cell is icon of current cell, descend into icon
        if (this.cell == schCell) schCell = cell;
        if (schCell == null) schCell = cell;
        if (pi != null)
            setCell(schCell, cellVarContext.push(pi));
        else
            setCell(schCell, cellVarContext.push(ni));
        // if highlighted was a port inst, then highlight the corresponding export
        if (pi != null) {
            PortInst origPort = schCell.findExport(pi.getPortProto().getProtoName()).getOriginalPort();
            //Highlight.addElectricObject(origPort, schCell);
            Highlight.addElectricObject(origPort.getNodeInst(), schCell);
            Highlight.finished();
        }
    }

    /**
     * Pop out of an instance (go up the hierarchy)
     */
    public void upHierarchy()
	{
        try {
            Nodable no = cellVarContext.getNodable();
			if (no != null)
			{
				Cell parent = no.getParent();
				VarContext context = cellVarContext.pop();
				CellHistory foundHistory = null;
				// see if this was in history, if so, restore offset and scale
				// search backwards to get most recent entry
				// search history **before** calling setCell, otherwise we find
				// the history record for the cell we just switched to
				for (int i=cellHistory.size()-1; i>-1; i--) {
					CellHistory history = (CellHistory)cellHistory.get(i);
					if ((history.cell == parent) && (history.context.equals(context))) {
						foundHistory = history;
						break;
					}
				}
                PortInst pi = cellVarContext.getPortInst();
				setCell(parent, context, true);
				if (foundHistory != null) {
					setOffset(foundHistory.offset);
					setScale(foundHistory.scale);
				}
                // highlight node we came from
                if (pi != null)
                    Highlight.addElectricObject(pi, parent);
                else if (no instanceof NodeInst)
					Highlight.addElectricObject((NodeInst)no, parent);
                // highlight portinst selected at the time, if any
				return;
			}

			// no parent - if icon, go to sch view
			if (cell.getView() == View.ICON)
			{
				Cell schCell = cell.getEquivalent();
				if (schCell != null)
				{
					setCell(schCell, VarContext.globalContext);
					return;
				}
			}            

			// find all possible parents in all libraries
			Set found = new TreeSet();
			for(Iterator it = cell.getInstancesOf(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				Cell parent = ni.getParent();
				found.add(parent.describe());
			}
			if (cell.getView() == View.SCHEMATIC)
			{
				Cell iconView = cell.iconView();
				if (iconView != null)
				{
					for(Iterator it = iconView.getInstancesOf(); it.hasNext(); )
					{
						NodeInst ni = (NodeInst)it.next();
						if (ni.isIconOfParent()) continue;
						Cell parent = ni.getParent();
						found.add(parent.describe());
					}
				}
			}

			// see what was found
			if (found.size() == 0)
			{
				// no parent cell
				System.out.println("Not in any cells");
			} else if (found.size() == 1)
			{
				// just one parent cell: show it
				String cellName = (String)found.iterator().next();
				Cell parent = (Cell)NodeProto.findNodeProto(cellName);
				setCell(parent, VarContext.globalContext);
			} else
			{
				// prompt the user to choose a parent cell
				JPopupMenu parents = new JPopupMenu("parents");
				for(Iterator it = found.iterator(); it.hasNext(); )
				{
					String cellName = (String)it.next();
					JMenuItem menuItem = new JMenuItem(cellName);
					menuItem.addActionListener(this);
					parents.add(menuItem);
				}
				parents.show(this, 0, 0);
			}
        } catch (NullPointerException e)
		{
            e.printStackTrace();
		}
    }

    // ************************** Cell History Traversal  *************************************

    /** List of CellHistory objects */                      private List cellHistory;
    /** Location in history (points to valid location) */   private int cellHistoryLocation;
    /** Property name: go back enabled */                   public static final String propGoBackEnabled = "GoBackEnabled";
    /** Property name: go forward enabled */                public static final String propGoForwardEnabled = "GoForwardEnabled";
    /** History limit */                                    private static final int cellHistoryLimit = 20;

    /**
     * Used to track CellHistory and associated values.
     */
    private static class CellHistory
    {
        /** cell */                     public Cell cell;
        /** context */                  public VarContext context;
        /** offset */                   public Point2D offset;
        /** scale */                    public double scale;
        /** highlights */               public List highlights;
        /** highlight offset*/          public Point2D highlightOffset;
    }

    /**
     * Go back in history list.
     */
    public void cellHistoryGoBack() {
        if (cellHistoryLocation <= 0) return;               // at start of history
        setCellByHistory(cellHistoryLocation-1);
    }

    /**
     * Go forward in history list.
     */
    public void cellHistoryGoForward() {
        if (cellHistoryLocation >= (cellHistory.size() - 1)) return; // at end of history
        setCellByHistory(cellHistoryLocation+1);
    }

    /** Returns true if we can go back in history list, false otherwise */
    public boolean cellHistoryCanGoBack() {
        if (cellHistoryLocation > 0) return true;
        return false;
    }

    /** Returns true if we can go forward in history list, false otherwise */
    public boolean cellHistoryCanGoForward() {
        if (cellHistoryLocation < (cellHistory.size() - 1)) return true;
        return false;
    }

    /**
     * Used when new tool bar is created with existing edit window
     * (when moving windows across displays).  Updates back/forward
     * button states.
     */
    public void fireCellHistoryStatus() {
        if (cellHistoryLocation > 0)
            getPanel().firePropertyChange(propGoBackEnabled, false, true);
        if (cellHistoryLocation < (cellHistory.size() - 1))
            getPanel().firePropertyChange(propGoForwardEnabled, false, true);
    }

    /** Adds to cellHistory record list
     * Should only be called via non-history traversing modifications
     * to history. (such as Edit->New Cell).
     */
    private void addToHistory(Cell cell, VarContext context) {

        if (cell == null) return;

        CellHistory history = new CellHistory();
        history.cell = cell;
        history.context = context;


        // when user has moved back through history, and then edits a new cell,
        // get rid of forward history
        if (cellHistoryLocation < (cellHistory.size() - 1)) {
            // inserting into middle of history: get rid of history forward of this
            for (int i=cellHistoryLocation+1; i<cellHistory.size(); i++) {
                cellHistory.remove(i);
            }
            // disable previously enabled forward button
            getPanel().firePropertyChange(propGoForwardEnabled, true, false);
        }

        // if location is 0, adding should enable back button
        if (cellHistoryLocation == 0)
            getPanel().firePropertyChange(propGoBackEnabled, false, true);

        // update history
        cellHistory.add(history);
        cellHistoryLocation = cellHistory.size() - 1;

        // adjust if we are over the limit
        if (cellHistoryLocation > cellHistoryLimit) {
            cellHistory.remove(0);
            cellHistoryLocation--;
        }

        //System.out.println("Adding to History at location="+cellHistoryLocation+", cellHistory.size()="+cellHistory.size());
    }

    /** Records current cell state into history
     * Assumes record pointed to by cellHistoryLocation is
     * history record for the current cell/context.
     */
    private void saveCurrentCellHistoryState() {

        if (cellHistoryLocation < 0) return;

        CellHistory current = (CellHistory)cellHistory.get(cellHistoryLocation);

        //System.out.println("Updating cell history state of location="+cellHistoryLocation+", cell "+cell);

        current.offset = new Point2D.Double(offx, offy);
        current.scale = scale;
        current.highlights = new ArrayList();
        current.highlights.clear();
        for (Iterator it = Highlight.getHighlights(); it.hasNext(); ) {
            current.highlights.add(it.next());
        }
        current.highlightOffset = Highlight.getHighlightOffset();
    }

    /** Restores cell state from history record */
    private void setCellByHistory(int location) {

        // fire property changes if back/forward buttons should change state
        if (cellHistoryLocation == (cellHistory.size()-1)) {
            // was at end, forward button was disabled
            if (location < (cellHistory.size()-1))
                getPanel().firePropertyChange(propGoForwardEnabled, false, true);
        } else {
            // not at end, forward button was enabled
            if (location == (cellHistory.size()-1))
                getPanel().firePropertyChange(propGoForwardEnabled, true, false);
        }
        if (cellHistoryLocation == 0) {
            // at beginning, back button was disabled
            if (location > 0)
                getPanel().firePropertyChange(propGoBackEnabled, false, true);
        } else {
            // not at beginning, back button was enabled
            if (location == 0)
                getPanel().firePropertyChange(propGoBackEnabled, true, false);
        }

        //System.out.println("Setting cell to location="+location+", cellHistory.size()="+cellHistory.size());

        // get cell history to go to
        CellHistory history = (CellHistory)cellHistory.get(location);

        // see if cell still valid part of database. If not, nullify entry
        if (!cell.isLinked()) {
            history.cell = null;
            history.context = VarContext.globalContext;
            history.offset = new Point2D.Double(0,0);
            history.highlights = new ArrayList();
            history.highlightOffset = new Point2D.Double(0,0);
        }

        // update current cell
        setCell(history.cell, history.context, false);
        setOffset(history.offset);
        setScale(history.scale);
        Highlight.setHighlightList(history.highlights);
        Highlight.setHighlightOffset((int)history.highlightOffset.getX(), (int)history.highlightOffset.getY());

        // point to new location *after* calling setCell, since setCell updates by current location
        cellHistoryLocation = location;

        repaintContents();
    }

    // ************************************* COORDINATES *************************************

	/**
	 * Method to convert a screen coordinate to database coordinates.
	 * @param screenX the X coordinate (on the screen in this EditWindow).
	 * @param screenY the Y coordinate (on the screen in this EditWindow).
	 * @return the coordinate of that point in database units.
	 */
	public Point2D screenToDatabase(int screenX, int screenY)
	{
		double dbX = (screenX - sz.width/2) / scale + offx;
		double dbY = (sz.height/2 - screenY) / scale + offy;
		return new Point2D.Double(dbX, dbY);
	}

	/**
	 * Method to convert a screen distance to a database distance.
	 * @param screenDX the X coordinate change (on the screen in this EditWindow).
	 * @param screenDY the Y coordinate change (on the screen in this EditWindow).
	 * @return the distance in database units.
	 */
	public Point2D deltaScreenToDatabase(int screenDX, int screenDY)
	{
		double dbDX = screenDX / scale;
		double dbDY = (-screenDY) / scale;
		return new Point2D.Double(dbDX, dbDY);
	}


	/**
	 * Method to convert a database X coordinate to screen coordinates.
	 * @param dbX the X coordinate (in database units).
	 * @return the coordinate on the screen.
	 */
	public int databaseToScreenX(double dbX)
	{
		return (int)(sz.width/2 + (dbX - offx) * scale);
	}

	/**
	 * Method to convert a database Y coordinate to screen coordinates.
	 * @param dbY the Y coordinate (in database units).
	 * @return the coordinate on the screen.
	 */
	public int databaseToScreenY(double dbY)
	{
		return (int)(sz.height/2 - (dbY - offy) * scale);
	}

	/**
	 * Method to convert a database coordinate to screen coordinates.
	 * @param dbX the X coordinate (in database units).
	 * @param dbY the Y coordinate (in database units).
	 * @return the coordinate on the screen.
	 */
	public Point databaseToScreen(double dbX, double dbY)
	{
		return new Point(databaseToScreenX(dbX), databaseToScreenY(dbY));
	}

	/**
	 * Method to convert a database coordinate to screen coordinates.
	 * @param db the coordinate (in database units).
	 * @return the coordinate on the screen.
	 */
	public Point databaseToScreen(Point2D db)
	{
		return new Point(databaseToScreenX(db.getX()), databaseToScreenY(db.getY()));
	}

	/**
	 * Method to convert a database rectangle to screen coordinates.
	 * @param db the rectangle (in database units).
	 * @return the rectangle on the screen.
	 */
	public Rectangle databaseToScreen(Rectangle2D db)
	{
		double sLX = sz.width/2 + (db.getMinX() - offx) * scale;
		double sHX = sz.width/2 + (db.getMaxX() - offx) * scale;
		double sLY = sz.height/2 - (db.getMinY() - offy) * scale;
		double sHY = sz.height/2 - (db.getMaxY() - offy) * scale;
		if (sLX < 0) sLX -= 0.5; else sLX += 0.5;
		if (sHX < 0) sHX -= 0.5; else sHX += 0.5;
		if (sLY < 0) sLY -= 0.5; else sLY += 0.5;
		if (sHY < 0) sHY -= 0.5; else sHY += 0.5;
		int screenLX = (int)sLX;
		int screenHX = (int)sHX;
		int screenLY = (int)sLY;
		int screenHY = (int)sHY;
		if (screenHX < screenLX) { int swap = screenHX;   screenHX = screenLX; screenLX = swap; }
		if (screenHY < screenLY) { int swap = screenHY;   screenHY = screenLY; screenLY = swap; }
		return new Rectangle(screenLX, screenLY, screenHX-screenLX, screenHY-screenLY);
	}

	/**
	 * Method to convert a database distance to a screen distance.
	 * @param dbDX the X change (in database units).
	 * @param dbDY the Y change (in database units).
	 * @return the distance on the screen.
	 */
	public Point deltaDatabaseToScreen(double dbDX, double dbDY)
	{
		int screenDX = (int)Math.round(dbDX * scale);
		int screenDY = (int)Math.round(-dbDY * scale);
		return new Point(screenDX, screenDY);
	}

	/**
	 * Method to snap a point to the nearest database-space grid unit.
	 * @param pt the point to be snapped.
	 */
	public static void gridAlign(Point2D pt)
	{
		double alignment = User.getAlignmentToGrid();
		long x = Math.round(pt.getX() / alignment);
		long y = Math.round(pt.getY() / alignment);
		pt.setLocation(x * alignment, y * alignment);
	}

	// ************************************* TEXT *************************************

	/**
	 * Method to find the size in database units for text of a given point size in this EditWindow.
	 * The scale of this EditWindow is used to determine the acutal unit size.
	 * @param pointSize the size of the text in points.
	 * @return the relative size (in units) of the text.
	 */
	public double getTextUnitSize(int pointSize)
	{
		Point2D pt = deltaScreenToDatabase(pointSize, pointSize);
		return pt.getX();
	}

	/**
	 * Method to find the size in database units for text of a given point size in this EditWindow.
	 * The scale of this EditWindow is used to determine the acutal unit size.
	 * @param pointSize the size of the text in points.
	 * @return the relative size (in units) of the text.
	 */
	public int getTextPointSize(double pointSize)
	{
		Point pt = deltaDatabaseToScreen(pointSize, pointSize);
		return pt.x;
	}

	public static int getDefaultFontSize() { return 14; }

	/**
	 * Method to get a Font to use for a given TextDescriptor in this EditWindow.
	 * @param descript the TextDescriptor.
	 * @return the Font to use (returns null if the text is too small to display).
	 */
	public Font getFont(TextDescriptor descript)
	{
		int size = getDefaultFontSize();
		int fontStyle = Font.PLAIN;
		String fontName = User.getDefaultFont();
		if (descript != null)
		{
			size = descript.getTrueSize(this);
			if (size <= 0) size = 1;
			if (descript.isItalic()) fontStyle |= Font.ITALIC;
			if (descript.isBold()) fontStyle |= Font.BOLD;
			int fontIndex = descript.getFace();
			if (fontIndex != 0)
			{
				TextDescriptor.ActiveFont af = TextDescriptor.ActiveFont.findActiveFont(fontIndex);
				if (af != null) fontName = af.getName();
			}
		}
		if (size < PixelDrawing.MINIMUMTEXTSIZE) return null;
		Font font = new Font(fontName, fontStyle, size);
		return font;
	}

	/**
	 * Method to convert a string and descriptor to a GlyphVector.
	 * @param text the string to convert.
	 * @param font the Font to use.
	 * @return a GlyphVector describing the text.
	 */
	public GlyphVector getGlyphs(String text, Font font)
	{
		// make a glyph vector for the desired text
		FontRenderContext frc = new FontRenderContext(null, false, false);
		GlyphVector gv = font.createGlyphVector(frc, text);
		return gv;
	}
}
