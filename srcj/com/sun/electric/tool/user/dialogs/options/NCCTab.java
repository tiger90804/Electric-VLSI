/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: NCCTab.java
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

import javax.swing.JPanel;
import com.sun.electric.tool.ncc.NCC;
import com.sun.electric.database.text.TextUtils;

/**
 * Class to handle the "NCC" tab of the Preferences dialog.
 */
public class NCCTab extends PreferencePanel
{
    private boolean initialEnableSizeChecking;
    private double initialRelativeSizeTolerance;
    private double initialAbsoluteSizeTolerance;
    private boolean initialHaltAfterFindingFirstMismatchedCell;
    
	/** Creates new form NCCTab */
	public NCCTab(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
	}
	public JPanel getPanel() { return ncc; }

	public String getName() { return "NCC"; }

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the NCC tab.
	 */
	public void init()
	{
		initialEnableSizeChecking = NCC.getCheckSizes();
		enableSizeChecking.setSelected(initialEnableSizeChecking);
		
		initialRelativeSizeTolerance = NCC.getRelativeSizeTolerance();
		relativeSizeTolerance.setText(Double.toString(initialRelativeSizeTolerance));
		
		initialAbsoluteSizeTolerance = NCC.getAbsoluteSizeTolerance();
		absoluteSizeTolerance.setText(Double.toString(initialAbsoluteSizeTolerance));
		
		initialHaltAfterFindingFirstMismatchedCell = 
			NCC.getHaltAfterFirstMismatch();
		haltAfterFindingFirstMismatchedCell.
			setSelected(initialHaltAfterFindingFirstMismatchedCell);

		//enableSizeChecking.setEnabled(false);
//		absoluteSizeTolerance.setEditable(false);
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the NCC tab.
	 */
	public void term()
	{
		boolean currentEnableSizeChecking = enableSizeChecking.isSelected();
		if (currentEnableSizeChecking!=initialEnableSizeChecking) {
			NCC.setCheckSizes(currentEnableSizeChecking);
		}
		double currentRelativeSizeTolerance = 
			TextUtils.atof(relativeSizeTolerance.getText(), new Double(initialRelativeSizeTolerance));
		if (currentRelativeSizeTolerance!=initialRelativeSizeTolerance) {
			NCC.setRelativeSizeTolerance(currentRelativeSizeTolerance);
		}
		double currentAbsoluteSizeTolerance = 
			TextUtils.atof(absoluteSizeTolerance.getText(), new Double(initialAbsoluteSizeTolerance));
		if (currentAbsoluteSizeTolerance!=initialAbsoluteSizeTolerance) {
			NCC.setAbsoluteSizeTolerance(currentAbsoluteSizeTolerance);
		}
		boolean currentHaltAfterFindingFirstMismatchedCell = 
			haltAfterFindingFirstMismatchedCell.isSelected();
		if (currentHaltAfterFindingFirstMismatchedCell!=
			initialHaltAfterFindingFirstMismatchedCell ) {
			NCC.setHaltAfterFirstMismatch(currentHaltAfterFindingFirstMismatchedCell);
		}
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        expandGroup = new javax.swing.ButtonGroup();
        mergeParallelGroup = new javax.swing.ButtonGroup();
        mergeSeriesGroup = new javax.swing.ButtonGroup();
        ncc = new javax.swing.JPanel();
        sizeChecking = new javax.swing.JPanel();
        enableSizeChecking = new javax.swing.JCheckBox();
        jLabel75 = new javax.swing.JLabel();
        jLabel76 = new javax.swing.JLabel();
        relativeSizeTolerance = new javax.swing.JTextField();
        absoluteSizeTolerance = new javax.swing.JTextField();
        checkingAllCells = new javax.swing.JPanel();
        haltAfterFindingFirstMismatchedCell = new javax.swing.JCheckBox();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Tool Options");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });

        ncc.setLayout(new java.awt.GridBagLayout());

        sizeChecking.setLayout(new java.awt.GridBagLayout());

        sizeChecking.setBorder(new javax.swing.border.TitledBorder("Size Checking"));
        enableSizeChecking.setText("Check sizes when NCCing a single pair of Cells flat");
        enableSizeChecking.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        sizeChecking.add(enableSizeChecking, gridBagConstraints);

        jLabel75.setText("Relative size tolerance  (%):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        sizeChecking.add(jLabel75, gridBagConstraints);

        jLabel76.setText("Absolute size tolerance (lambda):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        sizeChecking.add(jLabel76, gridBagConstraints);

        relativeSizeTolerance.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        sizeChecking.add(relativeSizeTolerance, gridBagConstraints);

        absoluteSizeTolerance.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        sizeChecking.add(absoluteSizeTolerance, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        ncc.add(sizeChecking, gridBagConstraints);

        checkingAllCells.setLayout(new java.awt.GridBagLayout());

        checkingAllCells.setBorder(new javax.swing.border.TitledBorder("Checking All Cells"));
        haltAfterFindingFirstMismatchedCell.setText("Halt after finding the first mismatched Cell");
        haltAfterFindingFirstMismatchedCell.setActionCommand("Halt on First Mismatched Cell");
        haltAfterFindingFirstMismatchedCell.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        checkingAllCells.add(haltAfterFindingFirstMismatchedCell, new java.awt.GridBagConstraints());

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        ncc.add(checkingAllCells, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        getContentPane().add(ncc, gridBagConstraints);

        pack();
    }//GEN-END:initComponents
	
	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField absoluteSizeTolerance;
    private javax.swing.JPanel checkingAllCells;
    private javax.swing.JCheckBox enableSizeChecking;
    private javax.swing.ButtonGroup expandGroup;
    private javax.swing.JCheckBox haltAfterFindingFirstMismatchedCell;
    private javax.swing.JLabel jLabel75;
    private javax.swing.JLabel jLabel76;
    private javax.swing.ButtonGroup mergeParallelGroup;
    private javax.swing.ButtonGroup mergeSeriesGroup;
    private javax.swing.JPanel ncc;
    private javax.swing.JTextField relativeSizeTolerance;
    private javax.swing.JPanel sizeChecking;
    // End of variables declaration//GEN-END:variables
	
}
