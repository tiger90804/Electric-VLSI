/* -*- tab-width: 4 -*-
*
* Electric(tm) VLSI Design System
*
* File: NccMsgsFrame.java
*
* Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.electric.tool.user.ncc;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.beans.PropertyVetoException;
import java.util.List;

import javax.swing.*;

import com.sun.electric.Main;
import com.sun.electric.tool.ncc.NccOptions;
import com.sun.electric.tool.user.menus.WindowMenu;

/**
 * This is the top-level class of NCC GUI.
 * Call display() to display the frame.
 */
public class NccMsgsFrame {

    // GUI variables
    /** current frame of NCC GUI    */ protected static Container frame;
    /** first-time placement flag   */ private static boolean placed = false;
    /** the top-level split-pane    */ private ComparisonsPane comparPane;

    // data variables
    /** list of NccComparisonResult */ private List<NccGuiInfo> mismatches;
    /** NCC options                 */ private NccOptions nccOptions;

    public NccMsgsFrame() {
        Dimension scrnSize = Main.getScreenSize();
        Container contentPane;

        JInternalFrame jInternalFrame = new JInternalFrame("NCC Messages", true, true, true, true);
        jInternalFrame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        frame = jInternalFrame;
        contentPane = jInternalFrame.getContentPane();
        jInternalFrame.setFrameIcon(Main.getFrameIcon());

        comparPane = new ComparisonsPane();
        contentPane.add(comparPane);
        comparPane.setPreferredSize(new Dimension(scrnSize.width/3*2, scrnSize.height/3*2));
        frame.setLocation(scrnSize.width/6, scrnSize.height/6);
        frame.addKeyListener(new java.awt.event.KeyListener()
		{
            public void keyPressed(KeyEvent event)
            {
                if (WindowMenu.getCloseWindowAccelerator() == KeyStroke.getKeyStrokeForEvent(event))
                    frame.setVisible(false);
            }

            public void keyTyped(KeyEvent event) {;}

            public void keyReleased(KeyEvent event) {;}
		});
    }

    /**
     * Method returns the current Frame of NCC GUI.
     * No more than one NCC GUI Frame exists at any time.
     * @return current NCC GUI Frame. The retuned frame is an instance
     * of either JInternalFrame (if in MDI mode) or of JFrame (otherwise)
     */
    public static Container getCurrentFrame() { return frame; }

    /**
     * Method to update the list of mismatched NCC comparisons
     * @param misms  list of mismatches
     * @param options  NCC options
     */
    public void setMismatches(List<NccGuiInfo> misms, NccOptions options) {
        mismatches = misms;
        nccOptions = options;
    }

    /**
     * Display NCC window.
     * If no errors were found, only a small message window will appear.
     */
    public void display() {
        // display small info message if no errors found
        if (mismatches.size() == 0) {
            frame.setVisible(false);
        	/* removed as a result of the request Bug #1859
            StringBuffer msg = new StringBuffer(100);
            msg.append("No errors found\n");
            if (nccOptions.checkSizes)
                msg.append("Exports, Topology and Sizes checked");
            else
                msg.append("Exports and Topology checked, Sizes not checked");
            JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(), msg,
                       "NCC Message", JOptionPane.INFORMATION_MESSAGE);
            */
            return;
        }

        comparPane.setMismatches(mismatches);
        JInternalFrame jif = (JInternalFrame)frame;
        if (!placed) {
            placed = true;
            jif.pack();
            Main.addToDesktop(jif);
        }
        try {
            jif.setIcon(false);
            jif.setSelected(true);
        } catch (PropertyVetoException e) {}
        if (!jif.isVisible()) {
            jif.setVisible(true);
            jif.show();
        }
        jif.toFront();
        jif.requestFocusInWindow();
    }
}
