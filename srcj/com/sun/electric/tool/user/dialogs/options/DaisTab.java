/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SkillTab.java
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.user.dialogs.options;

import com.sun.electric.tool.io.IOTool;

import java.awt.Frame;

import javax.swing.JPanel;

/**
 * Class to handle the "Dais" tab of the Preferences dialog.
 */
public class DaisTab extends PreferencePanel
{
	/** Creates new form SkillTab */
	public DaisTab(Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
	}

	/** return the panel to use for this preferences tab. */
	public JPanel getPanel() { return dais; }

	/** return the name of this preferences tab. */
	public String getName() { return "Dais"; }

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Dais tab.
	 */
	public void init()
	{
		if (!IOTool.hasSkill())
		{
			displayOnly.setEnabled(false);
			readCellInstances.setEnabled(false);
			readDetailWires.setEnabled(false);
			readGlobalWires.setEnabled(false);
			readPowerAndGround.setEnabled(false);
		} else
		{
			displayOnly.setSelected(IOTool.isDaisDisplayOnly());
			readCellInstances.setSelected(IOTool.isDaisReadCellInstances());
			readDetailWires.setSelected(IOTool.isDaisReadDetailWires());
			readGlobalWires.setSelected(IOTool.isDaisReadGlobalWires());
			readPowerAndGround.setSelected(IOTool.isDaisReadPowerAndGround());
		}
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Skill tab.
	 */
	public void term()
	{
		boolean currBoolean = displayOnly.isSelected();
		if (currBoolean != IOTool.isDaisDisplayOnly())
			IOTool.setDaisDisplayOnly(currBoolean);

		currBoolean = readCellInstances.isSelected();
		if (currBoolean !=  IOTool.isDaisReadCellInstances())
			IOTool.setDaisReadCellInstances(currBoolean);

		currBoolean = readDetailWires.isSelected();
		if (currBoolean !=  IOTool.isDaisReadDetailWires())
			IOTool.setDaisReadDetailWires(currBoolean);

		currBoolean = readGlobalWires.isSelected();
		if (currBoolean !=  IOTool.isDaisReadGlobalWires())
			IOTool.setDaisReadGlobalWires(currBoolean);

		currBoolean = readPowerAndGround.isSelected();
		if (currBoolean !=  IOTool.isDaisReadPowerAndGround())
			IOTool.setDaisReadPowerAndGround(currBoolean);
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        dais = new javax.swing.JPanel();
        readCellInstances = new javax.swing.JCheckBox();
        readPowerAndGround = new javax.swing.JCheckBox();
        readGlobalWires = new javax.swing.JCheckBox();
        readDetailWires = new javax.swing.JCheckBox();
        displayOnly = new javax.swing.JCheckBox();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("IO Options");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });

        dais.setLayout(new java.awt.GridBagLayout());

        readCellInstances.setText("Read cell instances");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        dais.add(readCellInstances, gridBagConstraints);

        readPowerAndGround.setText("Read power and ground wires");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        dais.add(readPowerAndGround, gridBagConstraints);

        readGlobalWires.setText("Read global wires");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        dais.add(readGlobalWires, gridBagConstraints);

        readDetailWires.setText("Read detail wires");
        readDetailWires.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        readDetailWires.setMargin(new java.awt.Insets(0, 0, 0, 0));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        dais.add(readDetailWires, gridBagConstraints);

        displayOnly.setText("Display-only");
        displayOnly.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        displayOnly.setMargin(new java.awt.Insets(0, 0, 0, 0));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        dais.add(displayOnly, gridBagConstraints);

        jLabel1.setText("Runs faster and uses less memory");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 4);
        dais.add(jLabel1, gridBagConstraints);

        jLabel2.setText("But is not editable, only displayable");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 4);
        dais.add(jLabel2, gridBagConstraints);

        getContentPane().add(dais, new java.awt.GridBagConstraints());

        pack();
    }// </editor-fold>//GEN-END:initComponents

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel dais;
    private javax.swing.JCheckBox displayOnly;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JCheckBox readCellInstances;
    private javax.swing.JCheckBox readDetailWires;
    private javax.swing.JCheckBox readGlobalWires;
    private javax.swing.JCheckBox readPowerAndGround;
    // End of variables declaration//GEN-END:variables

}
