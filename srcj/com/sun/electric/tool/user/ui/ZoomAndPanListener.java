/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ZoomAndPanListener.java
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

import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.database.hierarchy.Cell;

import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public class ZoomAndPanListener
	implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener
{
	public static ZoomAndPanListener theOne = new ZoomAndPanListener();
	private int oldx, oldy;
	private ToolBar.CursorMode mode;

	private ZoomAndPanListener() {}

	public void mousePressed(MouseEvent evt)
	{
		oldx = evt.getX();
		oldy = evt.getY();

		mode = ToolBar.getCursorMode();
		if (mode == ToolBar.CursorMode.ZOOM)
		{
			if ((evt.getModifiers()&MouseEvent.SHIFT_MASK) != 0) mode = ToolBar.CursorMode.PAN;
		} else if (mode == ToolBar.CursorMode.PAN)
		{
			if ((evt.getModifiers()&MouseEvent.SHIFT_MASK) != 0) mode = ToolBar.CursorMode.ZOOM;
		}
	}

	public void mouseReleased(MouseEvent evt) {}
	public void mouseClicked(MouseEvent evt) {}
	public void mouseEntered(MouseEvent evt) {}
	public void mouseExited(MouseEvent evt) {}
	public void mouseMoved(MouseEvent evt) {}

	public void mouseDragged(MouseEvent evt)
	{
		int newX = evt.getX();
		int newY = evt.getY();
		EditWindow wnd = (EditWindow)evt.getSource();

		double scale = wnd.getScale();
		if (mode == ToolBar.CursorMode.ZOOM)
		{
			// zooming the window scale
			wnd.setScale(scale * Math.exp((oldy-newY) / 100.0f));
		} else if (mode == ToolBar.CursorMode.PAN)
		{
			// panning the window location
			Point2D pt = wnd.getOffset();
			wnd.setOffset(new Point2D.Double(pt.getX() - (newX - oldx) / scale,
				pt.getY() + (newY - oldy) / scale));
		}
		oldx = newX;
		oldy = newY;
		wnd.repaintContents();
	}

	public void mouseWheelMoved(MouseWheelEvent evt)
	{
		int clicks = evt.getWheelRotation();
		System.out.println("Mouse wheel rolled by " + clicks);
	}

	public void keyPressed(KeyEvent evt) {}
	public void keyReleased(KeyEvent evt) {}
	public void keyTyped(KeyEvent evt) {}

    // ----------------------------------- Zoom commands -----------------------------------

    public static void fullDisplay()
    {
        // get the current window
        EditWindow wnd = EditWindow.getCurrent();
        if (wnd == null) return;

        // make the circuit fill the window
        wnd.fillScreen();
    }

    public static void zoomOutDisplay()
    {
        // get the current window
        EditWindow wnd = EditWindow.getCurrent();
        if (wnd == null) return;

        // zoom out by a factor of two
        double scale = wnd.getScale();
        wnd.setScale(scale / 2);
        wnd.repaintContents();
    }

    public static void zoomInDisplay()
    {
        // get the current window
        EditWindow wnd = EditWindow.getCurrent();
        if (wnd == null) return;

        // zoom in by a factor of two
        double scale = wnd.getScale();
        wnd.setScale(scale * 2);
        wnd.repaintContents();
    }

    public static void focusOnHighlighted()
    {
        // get the current window
        EditWindow wnd = EditWindow.getCurrent();
        if (wnd == null) return;

        // focus on highlighting
        Rectangle2D bounds = Highlight.getHighlightedArea(wnd);
        wnd.focusScreen(bounds);
    }

    // --------------------------- Pan Commands -------------------------------

    /**
     * Pan in X direction.  if ticks is positive, pan right. If ticks is
     * negative, pan left. A ticks value of 1 is a short pan (10% of screen).
     * Make ticks a higher value to pan more.
     * @param wnd the edit window to pan
     * @param ticks the amount and direction of pan
     */
    public static void panX(EditWindow wnd, int ticks) {

        Cell cell = wnd.getCell();
        if (cell == null) return;

        // heuristic: factor in multiplier
        int mult = (int)(80/wnd.getScale());
        if (mult <= 0) mult = 2;
        if (wnd.getScale() > 70) mult = 1; // we're really zoomed in

        Point2D wndOffset = wnd.getOffset();
        Point2D newOffset = new Point2D.Double(wndOffset.getX() - mult*ticks, wndOffset.getY());
        wnd.setOffset(newOffset);
        wnd.repaintContents();
    }

    /**
     * Pan in Y direction.  if ticks is positive, pan up. If ticks is
     * negative, pan down. A ticks value of 1 is a short pan (10% of screen).
     * Make ticks a higher value to pan more.
     * @param wnd the edit window to pan
     * @param ticks the amount and direction of pan
     */
    public static void panY(EditWindow wnd, int ticks) {

        Cell cell = wnd.getCell();
        if (cell == null) return;

        // heuristic: factor in multiplier
        int mult = (int)(80/wnd.getScale());
        if (mult <= 0) mult = 2;
        if (wnd.getScale() > 70) mult = 1; // we're really zoomed in

        Point2D wndOffset = wnd.getOffset();
        Point2D newOffset = new Point2D.Double(wndOffset.getX(), wndOffset.getY() - mult*ticks);
        wnd.setOffset(newOffset);
        wnd.repaintContents();
    }


}
