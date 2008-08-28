/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: GDSTab.java
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

import com.sun.electric.tool.io.IOTool;
import com.sun.electric.tool.Job;

import java.awt.Frame;

import javax.swing.JPanel;

/**
 * Class to handle the "GDS" tab of the Preferences dialog.
 */
public class GDSTab extends PreferencePanel
{
	/** Creates new form GDSTab */
	public GDSTab(Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
	}

	/** return the panel to use for this preferences tab. */
	public JPanel getPanel() { return gds; }

	/** return the name of this preferences tab. */
	public String getName() { return "GDS"; }

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the GDS tab.
	 */
	public void init()
	{
        gdsConvertNCCExportsConnectedByParentPins.setSelected(IOTool.getGDSConvertNCCExportsConnectedByParentPins());
        gdsInputMergesBoxes.setSelected(IOTool.isGDSInMergesBoxes());
		gdsInputIncludesText.setSelected(IOTool.isGDSInIncludesText());
		gdsInputExpandsCells.setSelected(IOTool.isGDSInExpandsCells());
		gdsInputInstantiatesArrays.setSelected(IOTool.isGDSInInstantiatesArrays());
		gdsInputIgnoresUnknownLayers.setSelected(IOTool.isGDSInIgnoresUnknownLayers());
        gdsSimplifyCells.setSelected(IOTool.isGDSInSimplifyCells());
        gdsColapseNames.setSelected(IOTool.isGDSColapseVddGndPinNames());
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the GDS tab.
	 */
	public void term()
	{
		boolean currentValue = gdsConvertNCCExportsConnectedByParentPins.isSelected();
        if (currentValue != IOTool.getGDSConvertNCCExportsConnectedByParentPins())
            IOTool.setGDSConvertNCCExportsConnectedByParentPins(currentValue);

		currentValue = gdsInputMergesBoxes.isSelected();
		if (currentValue != IOTool.isGDSInMergesBoxes())
			IOTool.setGDSInMergesBoxes(currentValue);
		currentValue = gdsInputIncludesText.isSelected();
		if (currentValue != IOTool.isGDSInIncludesText())
			IOTool.setGDSInIncludesText(currentValue);
		currentValue = gdsInputExpandsCells.isSelected();
		if (currentValue != IOTool.isGDSInExpandsCells())
			IOTool.setGDSInExpandsCells(currentValue);
		currentValue = gdsInputInstantiatesArrays.isSelected();
		if (currentValue != IOTool.isGDSInInstantiatesArrays())
			IOTool.setGDSInInstantiatesArrays(currentValue);
		currentValue = gdsInputIgnoresUnknownLayers.isSelected();
		if (currentValue != IOTool.isGDSInIgnoresUnknownLayers())
			IOTool.setGDSInIgnoresUnknownLayers(currentValue);
        currentValue = gdsSimplifyCells.isSelected();
        if (currentValue != IOTool.isGDSInSimplifyCells())
			IOTool.setGDSInSimplifyCells(currentValue);
        currentValue = gdsColapseNames.isSelected();
        if (currentValue != IOTool.isGDSColapseVddGndPinNames())
			IOTool.setGDSColapseVddGndPinNames(currentValue);
	}

	/**
	 * Method called when the factory reset is requested.
	 */
	public void reset()
	{
		System.out.println("CANNOT RESET GDS PREFERENCES YET");
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

        gds = new javax.swing.JPanel();
        gdsInputIncludesText = new javax.swing.JCheckBox();
        gdsInputExpandsCells = new javax.swing.JCheckBox();
        gdsInputInstantiatesArrays = new javax.swing.JCheckBox();
        gdsInputIgnoresUnknownLayers = new javax.swing.JCheckBox();
        gdsConvertNCCExportsConnectedByParentPins = new javax.swing.JCheckBox();
        gdsInputMergesBoxes = new javax.swing.JCheckBox();
        gdsSimplifyCells = new javax.swing.JCheckBox();
        gdsColapseNames = new javax.swing.JCheckBox();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();

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

        gds.setLayout(new java.awt.GridBagLayout());

        gdsInputIncludesText.setText("Input includes text");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        gds.add(gdsInputIncludesText, gridBagConstraints);

        gdsInputExpandsCells.setText("Input expands cells");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        gds.add(gdsInputExpandsCells, gridBagConstraints);

        gdsInputInstantiatesArrays.setText("Input instantiates arrays");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        gds.add(gdsInputInstantiatesArrays, gridBagConstraints);

        gdsInputIgnoresUnknownLayers.setText("Input ignores unknown layers");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        gds.add(gdsInputIgnoresUnknownLayers, gridBagConstraints);

        gdsConvertNCCExportsConnectedByParentPins.setText("Use NCC annotations for exports");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        gds.add(gdsConvertNCCExportsConnectedByParentPins, gridBagConstraints);

        gdsInputMergesBoxes.setText("Input merges boxes (slow)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        gds.add(gdsInputMergesBoxes, gridBagConstraints);

        gdsSimplifyCells.setText("Input simplifies contact vias");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        gds.add(gdsSimplifyCells, gridBagConstraints);

        gdsColapseNames.setText("Collapse VDD/GND pin names");
        gdsColapseNames.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                gdsColapseNamesActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        gds.add(gdsColapseNames, gridBagConstraints);

        jLabel1.setText("GDS Output:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        gds.add(jLabel1, gridBagConstraints);

        jLabel2.setText("GDS Input:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        gds.add(jLabel2, gridBagConstraints);

        getContentPane().add(gds, new java.awt.GridBagConstraints());

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void gdsColapseNamesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gdsColapseNamesActionPerformed
// TODO add your handling code here:
    }//GEN-LAST:event_gdsColapseNamesActionPerformed

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel gds;
    private javax.swing.JCheckBox gdsColapseNames;
    private javax.swing.JCheckBox gdsConvertNCCExportsConnectedByParentPins;
    private javax.swing.JCheckBox gdsInputExpandsCells;
    private javax.swing.JCheckBox gdsInputIgnoresUnknownLayers;
    private javax.swing.JCheckBox gdsInputIncludesText;
    private javax.swing.JCheckBox gdsInputInstantiatesArrays;
    private javax.swing.JCheckBox gdsInputMergesBoxes;
    private javax.swing.JCheckBox gdsSimplifyCells;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    // End of variables declaration//GEN-END:variables
}
