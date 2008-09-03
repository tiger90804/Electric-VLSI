/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DXFTab.java
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
package com.sun.electric.tool.user.dialogs.options;

import com.sun.electric.database.text.TextUtils;
import com.sun.electric.technology.Layer;
import com.sun.electric.tool.io.IOTool;

import java.awt.Frame;

import javax.swing.JPanel;

/**
 * Class to handle the "DXF" tab of the Preferences dialog.
 */
public class DXFTab extends PreferencePanel
{
	/** Creates new form DXFTab */
	public DXFTab(Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
	}

	/** return the panel to use for this preferences tab. */
	public JPanel getPanel() { return dxf; }

	/** return the name of this preferences tab. */
	public String getName() { return "DXF"; }

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the DXF tab.
	 */
	public void init()
	{
		dxfInputFlattensHierarchy.setSelected(IOTool.isDXFInputFlattensHierarchy());
		dxfInputReadsAllLayers.setSelected(IOTool.isDXFInputReadsAllLayers());
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the DXF tab.
	 */
	public void term()
	{
		boolean currentValue = dxfInputFlattensHierarchy.isSelected();
		if (currentValue != IOTool.isDXFInputFlattensHierarchy())
			IOTool.setDXFInputFlattensHierarchy(currentValue);

		currentValue = dxfInputReadsAllLayers.isSelected();
		if (currentValue != IOTool.isDXFInputReadsAllLayers())
			IOTool.setDXFInputReadsAllLayers(currentValue);
	}

	/**
	 * Method called when the factory reset is requested.
	 */
	public void reset()
	{
		if (IOTool.isFactoryDXFInputFlattensHierarchy() != IOTool.isDXFInputFlattensHierarchy())
			IOTool.setDXFInputFlattensHierarchy(IOTool.isFactoryDXFInputFlattensHierarchy());
		if (IOTool.isFactoryDXFInputReadsAllLayers() != IOTool.isDXFInputReadsAllLayers())
			IOTool.setDXFInputReadsAllLayers(IOTool.isFactoryDXFInputReadsAllLayers());
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents()
    {
        java.awt.GridBagConstraints gridBagConstraints;

        dxf = new javax.swing.JPanel();
        dxfInputFlattensHierarchy = new javax.swing.JCheckBox();
        dxfInputReadsAllLayers = new javax.swing.JCheckBox();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("IO Options");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
                closeDialog(evt);
            }
        });

        dxf.setLayout(new java.awt.GridBagLayout());

        dxfInputFlattensHierarchy.setText("Input flattens hierarchy");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        dxf.add(dxfInputFlattensHierarchy, gridBagConstraints);

        dxfInputReadsAllLayers.setText("Input reads all layers");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        dxf.add(dxfInputReadsAllLayers, gridBagConstraints);

        getContentPane().add(dxf, new java.awt.GridBagConstraints());

        pack();
    }// </editor-fold>//GEN-END:initComponents

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel dxf;
    private javax.swing.JCheckBox dxfInputFlattensHierarchy;
    private javax.swing.JCheckBox dxfInputReadsAllLayers;
    // End of variables declaration//GEN-END:variables

}
