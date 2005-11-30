/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: WindowMenu.java
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

package com.sun.electric.tool.user.menus;

import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.user.ActivityLogger;
import com.sun.electric.tool.user.Resources;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.dialogs.SetFocus;
import com.sun.electric.tool.user.ui.ClickZoomWireListener;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.EditWindowFocusBrowser;
import com.sun.electric.tool.user.ui.MessagesWindow;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowContent;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.ui.ZoomAndPanListener;
import com.sun.electric.tool.user.waveform.WaveformWindow;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.lang.reflect.Method;
import java.util.EventListener;
import java.util.Iterator;

import javax.swing.KeyStroke;

/**
 * Class to handle the commands in the "Window" pulldown menu.
 */
public class WindowMenu {
    private static MenuBar.MenuItem closeWindow = null;

    public static MenuBar.MenuItem getCloseWindow() {return closeWindow;}

    protected static void addWindowMenu(MenuBar menuBar) {
        MenuBar.MenuItem m;
		int buckyBit = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

        /****************************** THE WINDOW MENU ******************************/

		// mnemonic keys available: A         K    PQ       Y 
        MenuBar.Menu windowMenu = MenuBar.makeMenu("_Window");
        menuBar.add(windowMenu);

        m = windowMenu.addMenuItem("_Fill Window", KeyStroke.getKeyStroke('9', buckyBit),
            new ActionListener() { public void actionPerformed(ActionEvent e) { fullDisplay(); } });
        menuBar.addDefaultKeyBinding(m, KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD9, buckyBit), null);
        m = windowMenu.addMenuItem("Redisplay _Window", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { ZoomAndPanListener.redrawDisplay(); } });
        m = windowMenu.addMenuItem("Zoom _Out", KeyStroke.getKeyStroke('0', buckyBit),
            new ActionListener() { public void actionPerformed(ActionEvent e) { zoomOutDisplay(); } });
        menuBar.addDefaultKeyBinding(m, KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD0, buckyBit), null);
        m = windowMenu.addMenuItem("Zoom _In", KeyStroke.getKeyStroke('7', buckyBit),
            new ActionListener() { public void actionPerformed(ActionEvent e) { zoomInDisplay(); } });
        menuBar.addDefaultKeyBinding(m, KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD7, buckyBit), null);

		// mnemonic keys available: ABCDEF  IJKLMNOPQRSTUV XY 
        MenuBar.Menu specialZoomSubMenu = MenuBar.makeMenu("Special _Zoom");
        windowMenu.add(specialZoomSubMenu);
        m = specialZoomSubMenu.addMenuItem("Focus on _Highlighted", KeyStroke.getKeyStroke('F', buckyBit),
            new ActionListener() { public void actionPerformed(ActionEvent e) { focusOnHighlighted(); } });
        menuBar.addDefaultKeyBinding(m, KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD5, buckyBit), null);
        m = specialZoomSubMenu.addMenuItem("_Zoom Box", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { zoomBoxCommand(); }});
        specialZoomSubMenu.addMenuItem("Make _Grid Just Visible", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { makeGridJustVisibleCommand(); }});
        specialZoomSubMenu.addMenuItem("Match Other _Window", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { matchOtherWindowCommand(); }});

        windowMenu.addSeparator();

        m = windowMenu.addMenuItem("Pan _Left", KeyStroke.getKeyStroke('4', buckyBit),
            new ActionListener() { public void actionPerformed(ActionEvent e) { ZoomAndPanListener.panXOrY(0, WindowFrame.getCurrentWindowFrame(), 1); }});
        menuBar.addDefaultKeyBinding(m, KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD4, buckyBit), null);
        m = windowMenu.addMenuItem("Pan _Right", KeyStroke.getKeyStroke('6', buckyBit),
            new ActionListener() { public void actionPerformed(ActionEvent e) { ZoomAndPanListener.panXOrY(0, WindowFrame.getCurrentWindowFrame(), -1); }});
        menuBar.addDefaultKeyBinding(m, KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD6, buckyBit), null);
        m = windowMenu.addMenuItem("Pan _Up", KeyStroke.getKeyStroke('8', buckyBit),
            new ActionListener() { public void actionPerformed(ActionEvent e) { ZoomAndPanListener.panXOrY(1, WindowFrame.getCurrentWindowFrame(), -1); }});
        menuBar.addDefaultKeyBinding(m, KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD8, buckyBit), null);
        m = windowMenu.addMenuItem("Pan _Down", KeyStroke.getKeyStroke('2', buckyBit),
            new ActionListener() { public void actionPerformed(ActionEvent e) { ZoomAndPanListener.panXOrY(1, WindowFrame.getCurrentWindowFrame(), 1); }});
        menuBar.addDefaultKeyBinding(m, KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD2, buckyBit), null);

		// mnemonic keys available: AB DEFGHIJKLMNOPQR TUVWXYZ
        MenuBar.Menu centerSubMenu = MenuBar.makeMenu("Cen_ter");
        windowMenu.add(centerSubMenu);
        centerSubMenu.addMenuItem("_Selection", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { ZoomAndPanListener.centerSelection(); }});
        centerSubMenu.addMenuItem("_Cursor", KeyStroke.getKeyStroke('5', buckyBit),
            new ActionListener() { public void actionPerformed(ActionEvent e) { ZoomAndPanListener.centerCursor(e); }});

        //windowMenu.addMenuItem("Saved Views...", null,
        //    new ActionListener() { public void actionPerformed(ActionEvent e) { SavedViews.showSavedViewsDialog(); } });
        windowMenu.addMenuItem("Go To Pre_vious Focus", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { goToPreviousSavedFocus(); } });
        windowMenu.addMenuItem("Go To Ne_xt Focus", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { goToNextSavedFocus(); } });
        windowMenu.addMenuItem("_Set Focus...", null,
			new ActionListener() { public void actionPerformed(ActionEvent e) { SetFocus.showSetFocusDialog(); } });

        windowMenu.addSeparator();

        windowMenu.addMenuItem("Toggle _Grid", KeyStroke.getKeyStroke('G', buckyBit),
            new ActionListener() { public void actionPerformed(ActionEvent e) { toggleGridCommand(); } });

        windowMenu.addSeparator();

		// mnemonic keys available: AB DEFG IJKLMNOPQRSTU WXYZ
        MenuBar.Menu windowPartitionSubMenu = MenuBar.makeMenu("Ad_just Position");
        windowMenu.add(windowPartitionSubMenu);
        windowPartitionSubMenu.addMenuItem("Tile _Horizontally", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { tileHorizontallyCommand(); }});
        windowPartitionSubMenu.addMenuItem("Tile _Vertically", KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0),
            new ActionListener() { public void actionPerformed(ActionEvent e) { tileVerticallyCommand(); }});
        windowPartitionSubMenu.addMenuItem("_Cascade", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { cascadeWindowsCommand(); }});

        closeWindow = windowMenu.addMenuItem("Clos_e Window", KeyStroke.getKeyStroke(KeyEvent.VK_W, buckyBit),
            new ActionListener() { public void actionPerformed(ActionEvent e) { closeWindowCommand(); }});

		if (!TopLevel.isMDIMode()) {
			windowMenu.addSeparator();
			m = windowMenu.addMenuItem("Move to Ot_her Display", null,
				new ActionListener() { public void actionPerformed(ActionEvent e) { moveToOtherDisplayCommand(); } });
			if (getAllGraphicsDevices().length < 2) {
				// only 1 screen, disable menu
				m.setEnabled(false);
			}
			windowMenu.addMenuItem("Remember Locatio_n of Display", null,
				new ActionListener() { public void actionPerformed(ActionEvent e) { rememberDisplayLocation(); } });
		}
        windowMenu.addSeparator();

        // mnemonic keys available: A CDEFGHIJKLMNOPQ STUV XYZ
        MenuBar.Menu colorSubMenu = MenuBar.makeMenu("_Color Schemes");
        windowMenu.add(colorSubMenu);
        colorSubMenu.addMenuItem("_Restore Default Colors", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { defaultBackgroundCommand(); }});
        colorSubMenu.addMenuItem("_Black Background Colors", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { blackBackgroundCommand(); }});
        colorSubMenu.addMenuItem("_White Background Colors", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { whiteBackgroundCommand(); }});

		// mnemonic keys available: AB DE GHIJKLMNOPQR TUVWXYZ
        MenuBar.Menu messagesSubMenu = MenuBar.makeMenu("_Messages Window");
        windowMenu.add(messagesSubMenu);
        messagesSubMenu.addMenuItem("_Save Messages...", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { TopLevel.getMessagesStream().save(); }});
        messagesSubMenu.addMenuItem("_Clear", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { TopLevel.getMessagesWindow().clear(); }});
        messagesSubMenu.addMenuItem("Set F_ont...", null,
             new ActionListener() { public void actionPerformed(ActionEvent e) { TopLevel.getMessagesWindow().selectFont(); }});

        Class plugin3D = Resources.get3DClass("ui.J3DMenu");
        if (plugin3D != null)
        {
            // Adding 3D/Demo menu
            try {
                Method createMethod = plugin3D.getDeclaredMethod("add3DMenus", new Class[] {MenuBar.Menu.class});
                createMethod.invoke(plugin3D, new Object[] {windowMenu});
            } catch (Exception e)
            {
                System.out.println("Can't load 3D sub menu class: " + e.getMessage());
                ActivityLogger.logException(e);
            }
        }
		// mnemonic keys available: ABCDEFGHIJK MNOPQ STUVWXYZ
        MenuBar.Menu sideBarSubMenu = MenuBar.makeMenu("Side _Bar");
        windowMenu.add(sideBarSubMenu);
		sideBarSubMenu.addMenuItem("On _Left", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { WindowFrame.setSideBarLocation(true); }});
		sideBarSubMenu.addMenuItem("On _Right", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { WindowFrame.setSideBarLocation(false); }});
    }

    public static void fullDisplay()
    {
        // get the current frame
        WindowFrame wf = WindowFrame.getCurrentWindowFrame();
        if (wf == null) return;

        // make the circuit fill the window
        wf.getContent().fillScreen();
    }

    public static void zoomOutDisplay()
    {
        // get the current frame
        WindowFrame wf = WindowFrame.getCurrentWindowFrame();
        if (wf == null) return;

        // zoom out
        wf.getContent().zoomOutContents();
    }

    public static void zoomInDisplay()
    {
        // get the current frame
        WindowFrame wf = WindowFrame.getCurrentWindowFrame();
        if (wf == null) return;

        // zoom in
        wf.getContent().zoomInContents();
    }

    public static void zoomBoxCommand()
    {
        // only works with click zoom wire listener
        EventListener oldListener = WindowFrame.getListener();
        WindowFrame.setListener(ClickZoomWireListener.theOne);
        ClickZoomWireListener.theOne.zoomBoxSingleShot(oldListener);
    }

    /**
     * Method to make the current window's grid be just visible.
     * If it is zoomed-out beyond grid visibility, it is zoomed-in so that the grid is shown.
     * If it is zoomed-in such that the grid is not at minimum pitch,
     * it is zoomed-out so that the grid is barely able to fit.
     */
    public static void makeGridJustVisibleCommand()
    {
        EditWindow wnd = EditWindow.needCurrent();
        if (wnd == null) return;
        Rectangle2D displayable = wnd.displayableBounds();
        Dimension sz = wnd.getSize();
        double scaleX = wnd.getGridXSpacing() * sz.width / 5 / displayable.getWidth();
        double scaleY = wnd.getGridYSpacing() * sz.height / 5 / displayable.getHeight();
        double scale = Math.min(scaleX, scaleY);
        wnd.setScale(wnd.getScale() / scale);
        wnd.repaintContents(null, false);
    }

    /**
     * Method to zoom the current window so that its scale matches that of the "other" window.
     * For this to work, there must be exactly one other window shown.
     */
    public static void matchOtherWindowCommand()
    {
        EditWindow wnd = EditWindow.needCurrent();
        if (wnd == null) return;
        int numOthers = 0;
        EditWindow other = null;
        for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
        {
            WindowFrame wf = (WindowFrame)it.next();
            if (wf.getContent() instanceof EditWindow)
            {
                EditWindow wfWnd = (EditWindow)wf.getContent();
                if (wfWnd == wnd) continue;
                numOthers++;
                other = wfWnd;
            }
        }
        if (numOthers != 1)
        {
            System.out.println("There must be exactly two windows in order for one to match the other");
            return;
        }
        wnd.setScale(other.getScale());
        wnd.repaintContents(null, false);
    }

    public static void focusOnHighlighted()
    {
        // get the current frame
        WindowFrame wf = WindowFrame.getCurrentWindowFrame();
        if (wf == null) return;

        // focus on highlighted
        wf.getContent().focusOnHighlighted();
    }

    /**
     * This method implements the command to toggle the display of the grid.
     */
    public static void toggleGridCommand()
    {
        // get the current frame
        WindowFrame wf = WindowFrame.getCurrentWindowFrame();
        if (wf == null) return;
        if (wf.getContent() instanceof EditWindow)
        {
        	EditWindow wnd = (EditWindow)wf.getContent();
	        if (wnd == null) return;
	        wnd.setGrid(!wnd.isGrid());
        } else if (wf.getContent() instanceof WaveformWindow)
        {
        	WaveformWindow ww = (WaveformWindow)wf.getContent();
        	ww.toggleGridPoints();
        } else
        {
        	System.out.println("Cannot draw a grid in this type of window");
        }
    }

    /**
     * This method implements the command to tile the windows horizontally.
     */
    public static void tileHorizontallyCommand()
    {
        // get the overall area in which to work
        Rectangle [] areas = getWindowAreas();

        // tile the windows in each area
        for(int j=0; j<areas.length; j++)
        {
        	Rectangle area = areas[j];

        	// see how many windows are on this screen
        	int count = 0;
			for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
			{
				WindowFrame wf = (WindowFrame)it.next();
				Rectangle wfBounds = wf.getFrame().getBounds();
				int locX = (int)wfBounds.getCenterX();
				int locY = (int)wfBounds.getCenterY();
				if (locX >= area.x && locX < area.x+area.width &&
					locY >= area.y && locY < area.y+area.height) count++;
			}
			if (count == 0) continue;

			int windowHeight = area.height / count;
			count = 0;
			for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
			{
				WindowFrame wf = (WindowFrame)it.next();
				Rectangle wfBounds = wf.getFrame().getBounds();
				int locX = (int)wfBounds.getCenterX();
				int locY = (int)wfBounds.getCenterY();
				if (locX >= area.x && locX < area.x+area.width &&
					locY >= area.y && locY < area.y+area.height)
				{
					Rectangle windowArea = new Rectangle(area.x, area.y + count*windowHeight, area.width, windowHeight);
					count++;
					wf.setWindowSize(windowArea);
				}
			}
        }
    }

    /**
     * This method implements the command to tile the windows vertically.
     */
    public static void tileVerticallyCommand()
    {
		// get the overall area in which to work
		Rectangle [] areas = getWindowAreas();

		// tile the windows in each area
		for(int j=0; j<areas.length; j++)
		{
			Rectangle area = areas[j];

			// see how many windows are on this screen
			int count = 0;
			for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
			{
				WindowFrame wf = (WindowFrame)it.next();
				Rectangle wfBounds = wf.getFrame().getBounds();
				int locX = (int)wfBounds.getCenterX();
				int locY = (int)wfBounds.getCenterY();
				if (locX >= area.x && locX < area.x+area.width &&
					locY >= area.y && locY < area.y+area.height) count++;
			}
			if (count == 0) continue;

			int windowWidth = area.width / count;
			count = 0;
			for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
			{
				WindowFrame wf = (WindowFrame)it.next();
				Rectangle wfBounds = wf.getFrame().getBounds();
				int locX = (int)wfBounds.getCenterX();
				int locY = (int)wfBounds.getCenterY();
				if (locX >= area.x && locX < area.x+area.width &&
					locY >= area.y && locY < area.y+area.height)
				{
					Rectangle windowArea = new Rectangle(area.x + count*windowWidth, area.y, windowWidth, area.height);
					count++;
					wf.setWindowSize(windowArea);
				}
			}
		}
    }

    /**
     * This method implements the command to tile the windows cascaded.
     */
    public static void cascadeWindowsCommand()
    {
		// get the overall area in which to work
		Rectangle [] areas = getWindowAreas();

		// tile the windows in each area
		for(int j=0; j<areas.length; j++)
		{
			Rectangle area = areas[j];

			// see how many windows are on this screen
			int count = 0;
			for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
			{
				WindowFrame wf = (WindowFrame)it.next();
				Rectangle wfBounds = wf.getFrame().getBounds();
				int locX = (int)wfBounds.getCenterX();
				int locY = (int)wfBounds.getCenterY();
				if (locX >= area.x && locX < area.x+area.width &&
					locY >= area.y && locY < area.y+area.height) count++;
			}
			if (count == 0) continue;
			int numRuns = 1;
			int windowXSpacing = 0, windowYSpacing = 0;
			int windowWidth = area.width;
			int windowHeight = area.height;
			if (count > 1)
			{
				windowWidth = area.width * 3 / 4;
				windowHeight = area.height * 3 / 4;
				int windowSpacing = Math.min(area.width - windowWidth, area.height - windowHeight) / (count-1);
				if (windowSpacing < 70)
				{
					numRuns = 70 / windowSpacing;
					if (70 % windowSpacing != 0) numRuns++;
					windowSpacing *= numRuns;
				}
				windowXSpacing = (area.width - windowWidth) / (count-1) * numRuns;
				windowYSpacing = (area.height - windowHeight) / (count-1) * numRuns;
			}

			count = 0;
			for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
			{
				WindowFrame wf = (WindowFrame)it.next();
				Rectangle wfBounds = wf.getFrame().getBounds();
				int locX = (int)wfBounds.getCenterX();
				int locY = (int)wfBounds.getCenterY();
				if (locX >= area.x && locX < area.x+area.width &&
					locY >= area.y && locY < area.y+area.height)
				{
					int index = count / numRuns;
					Rectangle windowArea = new Rectangle(area.x + index*windowXSpacing,
						area.y + index*windowYSpacing, windowWidth, windowHeight);
					count++;
					wf.setWindowSize(windowArea);
				}
			}
		}
    }

	private static void closeWindowCommand()
	{
		WindowFrame curWF = WindowFrame.getCurrentWindowFrame();
		curWF.finished();
	}

	private static Rectangle [] getWindowAreas()
    {
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice [] gs = ge.getScreenDevices();
		Rectangle [] areas = new Rectangle[gs.length];
		for (int j = 0; j < gs.length; j++)
		{
			GraphicsDevice gd = gs[j];
			GraphicsConfiguration gc = gd.getDefaultConfiguration();
			areas[j] = gc.getBounds();
		}

//        // remove the tool palette
//        PaletteFrame pf = TopLevel.getPaletteFrame();
//        Rectangle pb = pf.getPaletteLocation();
//        removeOccludingRectangle(areas, pb);

        // remove the messages window
        MessagesWindow mw = TopLevel.getMessagesWindow();
        Rectangle mb = mw.getMessagesLocation();
        removeOccludingRectangle(areas, mb);
        return areas;
    }

    private static void removeOccludingRectangle(Rectangle [] areas, Rectangle occluding)
    {
		int cX = occluding.x + occluding.width/2;
		int cY = occluding.y + occluding.height/2;
    	for(int i=0; i<areas.length; i++)
    	{
			int lX = (int)areas[i].getMinX();
			int hX = (int)areas[i].getMaxX();
			int lY = (int)areas[i].getMinY();
			int hY = (int)areas[i].getMaxY();
			if (cX > lX && cX < hX && cY > lY && cY < hY)
			{
		        if (occluding.width > occluding.height)
		        {
		            // horizontally occluding window
		            if (occluding.getMaxY() - lY < hY - occluding.getMinY())
		            {
		                // occluding window on top
		                lY = (int)occluding.getMaxY();
		            } else
		            {
		                // occluding window on bottom
		                hY = (int)occluding.getMinY();
		            }
		        } else
		        {
		            if (occluding.getMaxX() - lX < hX - occluding.getMinX())
		            {
		                // occluding window on left
		                lX = (int)occluding.getMaxX();
		            } else
		            {
		                // occluding window on right
		                hX = (int)occluding.getMinX();
		            }
		        }
				areas[i].x = lX;   areas[i].width = hX - lX;
				areas[i].y = lY;   areas[i].height = hY - lY;
			}
    	}
    }

    /**
     * This method implements the command to set default colors.
     */
    public static void defaultBackgroundCommand()
    {
        User.setColorBackground(Color.LIGHT_GRAY.getRGB());
        User.setColorGrid(Color.BLACK.getRGB());
        User.setColorHighlight(Color.WHITE.getRGB());
        User.setColorPortHighlight(Color.YELLOW.getRGB());
        User.setColorText(Color.BLACK.getRGB());
        User.setColorInstanceOutline(Color.BLACK.getRGB());
		User.setColorWaveformBackground(Color.BLACK.getRGB());
		User.setColorWaveformForeground(Color.WHITE.getRGB());
		User.setColorWaveformStimuli(Color.RED.getRGB());

		// change the colors in the "Generic" technology
		Generic.tech.universal_lay.getGraphics().setColor(Color.BLACK);
		Generic.tech.glyph_lay.getGraphics().setColor(Color.BLACK);

        EditWindow.repaintAllContents();
        for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
        {
        	WindowFrame wf = (WindowFrame)it.next();
        	wf.loadComponentMenuForTechnology();
        }
    }

    /**
     * This method implements the command to set colors so that there is a black background.
     */
    public static void blackBackgroundCommand()
    {
        User.setColorBackground(Color.BLACK.getRGB());
        User.setColorGrid(Color.WHITE.getRGB());
        User.setColorHighlight(Color.RED.getRGB());
        User.setColorPortHighlight(Color.YELLOW.getRGB());
        User.setColorText(Color.WHITE.getRGB());
        User.setColorInstanceOutline(Color.WHITE.getRGB());
		User.setColorWaveformBackground(Color.BLACK.getRGB());
		User.setColorWaveformForeground(Color.WHITE.getRGB());
		User.setColorWaveformStimuli(Color.RED.getRGB());

		// change the colors in the "Generic" technology
		Generic.tech.universal_lay.getGraphics().setColor(Color.WHITE);
		Generic.tech.glyph_lay.getGraphics().setColor(Color.WHITE);

		EditWindow.repaintAllContents();
        for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
        {
        	WindowFrame wf = (WindowFrame)it.next();
        	wf.loadComponentMenuForTechnology();
        }
    }

    /**
     * This method implements the command to set colors so that there is a white background.
     */
    public static void whiteBackgroundCommand()
    {
        User.setColorBackground(Color.WHITE.getRGB());
        User.setColorGrid(Color.BLACK.getRGB());
        User.setColorHighlight(Color.RED.getRGB());
        User.setColorPortHighlight(Color.DARK_GRAY.getRGB());
        User.setColorText(Color.BLACK.getRGB());
        User.setColorInstanceOutline(Color.BLACK.getRGB());
		User.setColorWaveformBackground(Color.WHITE.getRGB());
		User.setColorWaveformForeground(Color.BLACK.getRGB());
		User.setColorWaveformStimuli(Color.RED.getRGB());

		// change the colors in the "Generic" technology
		Generic.tech.universal_lay.getGraphics().setColor(Color.BLACK);
		Generic.tech.glyph_lay.getGraphics().setColor(Color.BLACK);

        EditWindow.repaintAllContents();
        for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
        {
        	WindowFrame wf = (WindowFrame)it.next();
        	wf.loadComponentMenuForTechnology();
        }
    }

    private static GraphicsDevice [] getAllGraphicsDevices() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gs = ge.getScreenDevices();
        return gs;
    }

    public static void moveToOtherDisplayCommand()
    {
        // this only works in SDI mode
        if (TopLevel.isMDIMode()) return;

        // find current screen
        WindowFrame curWF = WindowFrame.getCurrentWindowFrame();
        WindowContent content = curWF.getContent();
        GraphicsConfiguration curConfig = content.getPanel().getGraphicsConfiguration();
        GraphicsDevice curDevice = curConfig.getDevice();

        // get all screens
        GraphicsDevice[] gs = getAllGraphicsDevices();
        for (int j=0; j<gs.length; j++) {
            //System.out.println("Found GraphicsDevice: "+gs[j]+", type: "+gs[j].getType());
        }
        // find screen after current screen
        int i;
        for (i=0; i<gs.length; i++) {
            if (gs[i] == curDevice) break;
        }
        if (i == (gs.length - 1)) i = 0; else i++;      // go to next device

        curWF.moveEditWindow(gs[i].getDefaultConfiguration());
    }

	public static void rememberDisplayLocation()
    {
        // this only works in SDI mode
        if (TopLevel.isMDIMode()) return;

        // remember main window information
        WindowFrame curWF = WindowFrame.getCurrentWindowFrame();
		TopLevel tl = curWF.getFrame();
		Point pt = tl.getLocation();
		User.setDefaultWindowPos(pt);
		Dimension sz = tl.getSize();
		User.setDefaultWindowSize(sz);

		// remember messages information
        MessagesWindow mw = TopLevel.getMessagesWindow();
		Rectangle rect = mw.getMessagesLocation();
		User.setDefaultMessagesPos(new Point(rect.x, rect.y));
		User.setDefaultMessagesSize(new Dimension(rect.width, rect.height));
    }

    /**
     * Go to the previous saved view for the current Edit Window
     */
    public static void goToPreviousSavedFocus() {
        EditWindow wnd = EditWindow.needCurrent();
        if (wnd == null) return;
        EditWindowFocusBrowser browser = wnd.getSavedFocusBrowser();
        browser.goBack();
    }

    /**
     * Go to the previous saved view for the current Edit Window
     */
    public static void goToNextSavedFocus() {
        EditWindow wnd = EditWindow.needCurrent();
        if (wnd == null) return;
        EditWindowFocusBrowser browser = wnd.getSavedFocusBrowser();
        browser.goForward();
    }
}
