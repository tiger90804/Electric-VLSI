/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ExportMenu.java
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

import com.sun.electric.tool.user.ExportChanges;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: gainsley
 * Date: Jun 23, 2004
 * Time: 11:41:02 AM
 * To change this template use File | Settings | File Templates.
 */
public class ExportMenu {

    protected static void addExportMenu(MenuBar menuBar) {
        MenuBar.MenuItem m;
		int buckyBit = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

        /****************************** THE EXPORT MENU ******************************/

        MenuBar.Menu exportMenu = new MenuBar.Menu("Export", 'X');
        menuBar.add(exportMenu);

        exportMenu.addMenuItem("Create Export...", KeyStroke.getKeyStroke('E', buckyBit),
            new ActionListener() { public void actionPerformed(ActionEvent e) { ExportChanges.newExportCommand(); } });

        exportMenu.addSeparator();

        exportMenu.addMenuItem("Re-Export Everything", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { ExportChanges.reExportAll(); } });
        exportMenu.addMenuItem("Re-Export Highlighted Area", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { ExportChanges.reExportHighlighted(); } });
        exportMenu.addMenuItem("Re-Export Power and Ground", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { ExportChanges.reExportPowerAndGround(); } });

        exportMenu.addSeparator();

        exportMenu.addMenuItem("Delete Export", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { ExportChanges.deleteExport(); } });
        exportMenu.addMenuItem("Delete Exports on Selected", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { ExportChanges.deleteExportsOnHighlighted(); } });
        exportMenu.addMenuItem("Delete Exports in Highlighted Area", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { ExportChanges.deleteExportsInArea(); } });
        exportMenu.addMenuItem("Move Export", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { ExportChanges.moveExport(); } });
        exportMenu.addMenuItem("Rename Export", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { ExportChanges.renameExport(); } });

        exportMenu.addSeparator();

        exportMenu.addMenuItem("Summarize Exports", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { ExportChanges.describeExports(true); } });
        exportMenu.addMenuItem("List Exports", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { ExportChanges.describeExports(false); } });
        exportMenu.addMenuItem("Show Exports", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { ExportChanges.showExports(); } });

        exportMenu.addSeparator();

        exportMenu.addMenuItem("Show Ports on Node", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { ExportChanges.showPorts(); } });

    }
}
