/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DRCTab.java
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

import com.sun.electric.tool.drc.DRC;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JPanel;

/**
 * Class to handle the "DRC" tab of the Preferences dialog.
 */
public class DRCTab extends PreferencePanel
{
	/** Creates new form DRCTab */
	public DRCTab(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
	}

	/** return the panel to use for this preferences tab. */
	public JPanel getPanel() { return drc; }

	/** return the name of this preferences tab. */
	public String getName() { return "DRC"; }

	private boolean requestedDRCClearDates;

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the DRC tab.
	 */
	public void init()
	{
		drcIncrementalOn.setSelected(DRC.isIncrementalDRCOn());
		drcInteractiveDrag.setSelected(DRC.isInteractiveDRCDragOn());
        switch (DRC.getErrorType())
        {
            case ERROR_CHECK_DEFAULT: drcErrorDefault.setSelected(true);   break;
			case ERROR_CHECK_CELL: drcErrorCell.setSelected(true);      break;
			case ERROR_CHECK_EXHAUSTIVE: drcErrorExaustive.setSelected(true);        break;
        }
        // Setting looging type
        loggingCombo.removeAllItems();
        for (DRC.DRCCheckLogging type : DRC.DRCCheckLogging.values())
             loggingCombo.addItem(type);
        loggingCombo.setSelectedItem(DRC.getErrorLoggingType());

        drcIgnoreCenterCuts.setSelected(DRC.isIgnoreCenterCuts());

        // MinArea rules
		drcIgnoreArea.setSelected(DRC.isIgnoreAreaChecking());

        // PolySelec rule
		drcIgnoreExtensionRules.setSelected(DRC.isIgnoreExtensionRuleChecking());

        // DRC dates in memory stored
        drcDateOnCells.setSelected(!DRC.isDatesStoredInMemory());

        // Interactive logging
        drcInteractive.setSelected(DRC.isInteractiveLoggingOn());

		requestedDRCClearDates = false;
		drcClearValidDates.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				drcClearValidDates.setEnabled(false);
				requestedDRCClearDates = true;
			}
		});
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the DRC tab.
	 */
	public void term()
	{
		boolean currentValue = drcIncrementalOn.isSelected();
		if (currentValue != DRC.isIncrementalDRCOn())
			DRC.setIncrementalDRCOn(currentValue);
		currentValue = drcInteractiveDrag.isSelected();
		if (currentValue != DRC.isInteractiveDRCDragOn())
			DRC.setInteractiveDRCDragOn(currentValue);

		if (drcErrorDefault.isSelected())
            DRC.setErrorType(DRC.DRCCheckMode.ERROR_CHECK_DEFAULT);
        else if (drcErrorCell.isSelected())
            DRC.setErrorType(DRC.DRCCheckMode.ERROR_CHECK_CELL);
        else if (drcErrorExaustive.isSelected())
            DRC.setErrorType(DRC.DRCCheckMode.ERROR_CHECK_EXHAUSTIVE);

        // Checking the logging type
        if (loggingCombo.getSelectedItem() != DRC.getErrorLoggingType())
            DRC.setErrorLoggingType((DRC.DRCCheckLogging)loggingCombo.getSelectedItem());
        
        // Checking center cuts
        currentValue = drcIgnoreCenterCuts.isSelected();
		if (currentValue != DRC.isIgnoreCenterCuts())
			DRC.setIgnoreCenterCuts(currentValue);

        // For min area rules
        currentValue = drcIgnoreArea.isSelected();
		if (currentValue != DRC.isIgnoreAreaChecking())
			DRC.setIgnoreAreaChecking(currentValue);

        // Poly Select rule
        currentValue = drcIgnoreExtensionRules.isSelected();
		if (currentValue != DRC.isIgnoreExtensionRuleChecking())
			DRC.setIgnoreExtensionRuleChecking(currentValue);

        // DRC dates in memory
        currentValue = !drcDateOnCells.isSelected();
		if (currentValue != DRC.isDatesStoredInMemory())
			DRC.setDatesStoredInMemory(currentValue);

        // Interactive logging
        currentValue = drcInteractive.isSelected();
		if (currentValue != DRC.isInteractiveLoggingOn())
			DRC.setInteractiveLogging(currentValue);

		if (requestedDRCClearDates) DRC.resetDRCDates(true);
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        errorTypeGroup = new javax.swing.ButtonGroup();
        drc = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        drcIncrementalOn = new javax.swing.JCheckBox();
        drcInteractiveDrag = new javax.swing.JCheckBox();
        jPanel4 = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        drcErrorExaustive = new javax.swing.JRadioButton();
        drcErrorDefault = new javax.swing.JRadioButton();
        drcErrorCell = new javax.swing.JRadioButton();
        loggingLabel = new javax.swing.JLabel();
        loggingCombo = new javax.swing.JComboBox();
        jPanel5 = new javax.swing.JPanel();
        drcIgnoreCenterCuts = new javax.swing.JCheckBox();
        drcIgnoreExtensionRules = new javax.swing.JCheckBox();
        drcIgnoreArea = new javax.swing.JCheckBox();
        drcDateOnCells = new javax.swing.JCheckBox();
        drcInteractive = new javax.swing.JCheckBox();
        drcClearValidDates = new javax.swing.JButton();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Tool Options");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });

        drc.setLayout(new java.awt.GridBagLayout());

        jPanel3.setLayout(new java.awt.GridBagLayout());

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Incremental DRC"));
        drcIncrementalOn.setText("On");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 2, 4);
        jPanel3.add(drcIncrementalOn, gridBagConstraints);

        drcInteractiveDrag.setText("Show worst violation while moving nodes and arcs");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 4, 4);
        jPanel3.add(drcInteractiveDrag, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        drc.add(jPanel3, gridBagConstraints);

        jPanel4.setLayout(new java.awt.GridBagLayout());

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("Hierarchical DRC"));
        jPanel1.setLayout(new java.awt.GridBagLayout());

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Report Type"));
        errorTypeGroup.add(drcErrorExaustive);
        drcErrorExaustive.setText("Report all errors");
        drcErrorExaustive.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 4, 4);
        jPanel1.add(drcErrorExaustive, gridBagConstraints);

        errorTypeGroup.add(drcErrorDefault);
        drcErrorDefault.setSelected(true);
        drcErrorDefault.setText("Report just 1 error per pair of geometries");
        drcErrorDefault.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 1, 4);
        jPanel1.add(drcErrorDefault, gridBagConstraints);

        errorTypeGroup.add(drcErrorCell);
        drcErrorCell.setText("Report just 1 error per cell");
        drcErrorCell.setAutoscrolls(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 1, 4);
        jPanel1.add(drcErrorCell, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        jPanel4.add(jPanel1, gridBagConstraints);

        loggingLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        loggingLabel.setText("Logging Type: ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel4.add(loggingLabel, gridBagConstraints);

        loggingCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        loggingCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loggingComboActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel4.add(loggingCombo, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        drc.add(jPanel4, gridBagConstraints);

        jPanel5.setLayout(new java.awt.GridBagLayout());

        jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder("Incremental and Hierarchical"));
        drcIgnoreCenterCuts.setText("Ignore center cuts in large contacts");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 1, 4);
        jPanel5.add(drcIgnoreCenterCuts, gridBagConstraints);

        drcIgnoreExtensionRules.setText("Ignore extension rules");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 1, 4);
        jPanel5.add(drcIgnoreExtensionRules, gridBagConstraints);

        drcIgnoreArea.setText("Ignore area checking");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 1, 4);
        jPanel5.add(drcIgnoreArea, gridBagConstraints);

        drcDateOnCells.setText("Save valid DRC dates with cells");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 1, 4);
        jPanel5.add(drcDateOnCells, gridBagConstraints);

        drcInteractive.setText("Interactive Logging");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 4, 4);
        jPanel5.add(drcInteractive, gridBagConstraints);

        drcClearValidDates.setText("Clear valid DRC dates");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 30, 4, 4);
        jPanel5.add(drcClearValidDates, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        drc.add(jPanel5, gridBagConstraints);

        getContentPane().add(drc, new java.awt.GridBagConstraints());

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void loggingComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loggingComboActionPerformed
// TODO add your handling code here:
    }//GEN-LAST:event_loggingComboActionPerformed

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel drc;
    private javax.swing.JButton drcClearValidDates;
    private javax.swing.JCheckBox drcDateOnCells;
    private javax.swing.JRadioButton drcErrorCell;
    private javax.swing.JRadioButton drcErrorDefault;
    private javax.swing.JRadioButton drcErrorExaustive;
    private javax.swing.JCheckBox drcIgnoreArea;
    private javax.swing.JCheckBox drcIgnoreCenterCuts;
    private javax.swing.JCheckBox drcIgnoreExtensionRules;
    private javax.swing.JCheckBox drcIncrementalOn;
    private javax.swing.JCheckBox drcInteractive;
    private javax.swing.JCheckBox drcInteractiveDrag;
    private javax.swing.ButtonGroup errorTypeGroup;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JComboBox loggingCombo;
    private javax.swing.JLabel loggingLabel;
    // End of variables declaration//GEN-END:variables

}
