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

import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.ncc.NccGuiOptions;
import com.sun.electric.tool.ncc.NccOptions;

/**
 * Class to handle the "NCC" tab of the Preferences dialog.
 */
public class NCCTab extends PreferencePanel
{
	/** Creates new form NCCTab */
	public NCCTab(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
	}

	/** return the panel to use for this preferences tab. */
	public JPanel getPanel() { return ncc; }

	/** return the name of this preferences tab. */
	public String getName() { return "NCC"; }

	private void setOperation(int op) {
        switch (op) {
            case NccOptions.HIER_EACH_CELL: hierAll.setSelected(true); break;
            case NccOptions.FLAT_TOP_CELL: flatTop.setSelected(true); break;
            case NccOptions.LIST_ANNOTATIONS: listAnn.setSelected(true); break;
            default: hierAll.setSelected(true); break;
        }
    }
    private int getOperation() {
        if (hierAll.isSelected()) return NccOptions.HIER_EACH_CELL;
        if (flatTop.isSelected()) return NccOptions.FLAT_TOP_CELL;
        if (listAnn.isSelected()) return NccOptions.LIST_ANNOTATIONS;
        return NccOptions.HIER_EACH_CELL;
    }
    
    /**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the NCC tab.
	 */
	public void init()
	{
		enableSizeChecking.setSelected(NccGuiOptions.getCheckSizes());
		relativeSizeTolerance.setText(Double.toString(NccGuiOptions.getRelativeSizeTolerance()));
		absoluteSizeTolerance.setText(Double.toString(NccGuiOptions.getAbsoluteSizeTolerance()));
		haltAfterFindingFirstMismatchedCell.
			setSelected(NccGuiOptions.getHaltAfterFirstMismatch());
        skipPassed.setSelected(NccGuiOptions.getSkipPassed());
        maxMatched.setText(Integer.toString(NccGuiOptions.getMaxMatchedClasses()));
        maxMismatched.setText(Integer.toString(NccGuiOptions.getMaxMismatchedClasses()));
        maxMembers.setText(Integer.toString(NccGuiOptions.getMaxClassMembers()));
        setOperation(NccGuiOptions.getOperation());
        howMuchStatus.setText(Integer.toString(NccGuiOptions.getHowMuchStatus()));
        backAnnotateLayNetNamesFromSch.setSelected(NccGuiOptions.getBackAnnotateLayoutNetNames());
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the NCC tab.
	 */
	public void term()
	{
		boolean currBoolean = enableSizeChecking.isSelected();
		if (currBoolean!=NccGuiOptions.getCheckSizes()) {
			NccGuiOptions.setCheckSizes(currBoolean);
		}
		double currDouble = TextUtils.atof(relativeSizeTolerance.getText(), new Double(NccGuiOptions.getRelativeSizeTolerance()));
		if (currDouble!=NccGuiOptions.getRelativeSizeTolerance()) {
			NccGuiOptions.setRelativeSizeTolerance(currDouble);
		}
		currDouble = TextUtils.atof(absoluteSizeTolerance.getText(), new Double(NccGuiOptions.getAbsoluteSizeTolerance()));
		if (currDouble!=NccGuiOptions.getAbsoluteSizeTolerance()) {
			NccGuiOptions.setAbsoluteSizeTolerance(currDouble);
		}
		currBoolean = haltAfterFindingFirstMismatchedCell.isSelected();
		if (currBoolean!=
			NccGuiOptions.getHaltAfterFirstMismatch()) {
			NccGuiOptions.setHaltAfterFirstMismatch(currBoolean);
		}
        currBoolean = skipPassed.isSelected();
        if (currBoolean!=NccGuiOptions.getSkipPassed()) {
            NccGuiOptions.setSkipPassed(currBoolean);
        }
        int currInt = Integer.parseInt(maxMatched.getText());
        if (currInt!=NccGuiOptions.getMaxMatchedClasses()) {
            NccGuiOptions.setMaxMatchedClasses(currInt);
        }
        currInt = Integer.parseInt(maxMismatched.getText());
        if (currInt!=NccGuiOptions.getMaxMismatchedClasses()) {
            NccGuiOptions.setMaxMismatchedClasses(currInt);
        }
        currInt = Integer.parseInt(maxMembers.getText());
        if (currInt!=NccGuiOptions.getMaxClassMembers()) {
            NccGuiOptions.setMaxClassMembers(currInt);
        }
        currInt = getOperation();
        if (currInt!=NccGuiOptions.getOperation()) {
            NccGuiOptions.setOperation(currInt);
        }
        currInt = Integer.parseInt(howMuchStatus.getText());
        if (currInt!=NccGuiOptions.getHowMuchStatus()) {
            NccGuiOptions.setHowMuchStatus(currInt);
        }
        currBoolean = backAnnotateLayNetNamesFromSch.isSelected();
        if (currBoolean != NccGuiOptions.getBackAnnotateLayoutNetNames()) {
            NccGuiOptions.setBackAnnotateLayoutNetNames(currBoolean);
        }
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        operationGroup = new javax.swing.ButtonGroup();
        ncc = new javax.swing.JPanel();
        operation = new javax.swing.JPanel();
        hierAll = new javax.swing.JRadioButton();
        flatTop = new javax.swing.JRadioButton();
        listAnn = new javax.swing.JRadioButton();
        sizeChecking = new javax.swing.JPanel();
        enableSizeChecking = new javax.swing.JCheckBox();
        jLabel75 = new javax.swing.JLabel();
        jLabel76 = new javax.swing.JLabel();
        relativeSizeTolerance = new javax.swing.JTextField();
        absoluteSizeTolerance = new javax.swing.JTextField();
        checkingAllCells = new javax.swing.JPanel();
        haltAfterFindingFirstMismatchedCell = new javax.swing.JCheckBox();
        skipPassed = new javax.swing.JCheckBox();
        progressReport = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        howMuchStatus = new javax.swing.JTextField();
        errorReport = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        maxMatched = new javax.swing.JTextField();
        maxMismatched = new javax.swing.JTextField();
        maxMembers = new javax.swing.JTextField();
        misc = new javax.swing.JPanel();
        backAnnotateLayNetNamesFromSch = new javax.swing.JCheckBox();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Tool Options");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });

        ncc.setLayout(new java.awt.GridBagLayout());

        operation.setLayout(new java.awt.GridBagLayout());

        operation.setBorder(new javax.swing.border.TitledBorder("Operation"));
        hierAll.setText("Hierarchical Comparison");
        operationGroup.add(hierAll);
        hierAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                hierAllActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        operation.add(hierAll, gridBagConstraints);

        flatTop.setText("Flat Comparison");
        operationGroup.add(flatTop);
        flatTop.setActionCommand("Flat NCC");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        operation.add(flatTop, gridBagConstraints);

        listAnn.setText("List NCC annotations");
        operationGroup.add(listAnn);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        operation.add(listAnn, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        ncc.add(operation, gridBagConstraints);

        sizeChecking.setLayout(new java.awt.GridBagLayout());

        sizeChecking.setBorder(new javax.swing.border.TitledBorder("Size Checking"));
        enableSizeChecking.setText("Check transistor sizes");
        enableSizeChecking.setActionCommand("Check Transistor Sizes");
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

        jLabel76.setText("Absolute size tolerance (units):");
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
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        ncc.add(sizeChecking, gridBagConstraints);

        checkingAllCells.setLayout(new java.awt.GridBagLayout());

        checkingAllCells.setBorder(new javax.swing.border.TitledBorder("Checking All Cells"));
        haltAfterFindingFirstMismatchedCell.setText("Halt after finding the first mismatched cell");
        haltAfterFindingFirstMismatchedCell.setActionCommand("Halt on First Mismatched Cell");
        haltAfterFindingFirstMismatchedCell.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        checkingAllCells.add(haltAfterFindingFirstMismatchedCell, new java.awt.GridBagConstraints());

        skipPassed.setText("Don't recheck cells that have passed in this Electric run");
        skipPassed.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        skipPassed.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                skipPassedActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        checkingAllCells.add(skipPassed, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        ncc.add(checkingAllCells, gridBagConstraints);

        progressReport.setLayout(new java.awt.GridBagLayout());

        progressReport.setBorder(new javax.swing.border.TitledBorder("Reporting Progress"));
        jLabel4.setText("How many status messages to print (0->few, 2->many):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        progressReport.add(jLabel4, gridBagConstraints);

        howMuchStatus.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        progressReport.add(howMuchStatus, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        ncc.add(progressReport, gridBagConstraints);

        errorReport.setLayout(new java.awt.GridBagLayout());

        errorReport.setBorder(new javax.swing.border.TitledBorder("Error Reporting"));
        jLabel1.setLabelFor(maxMatched);
        jLabel1.setText("Maximum number of matched equivalence classes to print");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 5);
        errorReport.add(jLabel1, gridBagConstraints);

        jLabel2.setLabelFor(maxMismatched);
        jLabel2.setText("Maximum number of mismatched equivalence classes to print");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 5);
        errorReport.add(jLabel2, gridBagConstraints);

        jLabel3.setLabelFor(maxMembers);
        jLabel3.setText("Maximum number of equivalence class members to print");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 5);
        errorReport.add(jLabel3, gridBagConstraints);

        maxMatched.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        errorReport.add(maxMatched, gridBagConstraints);

        maxMismatched.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        errorReport.add(maxMismatched, gridBagConstraints);

        maxMembers.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        errorReport.add(maxMembers, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        ncc.add(errorReport, gridBagConstraints);

        misc.setLayout(new java.awt.GridBagLayout());

        misc.setBorder(new javax.swing.border.TitledBorder("Miscellaneous"));
        backAnnotateLayNetNamesFromSch.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        backAnnotateLayNetNamesFromSch.setLabel("Back-annotate layout nets with schematic net names after matching");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        misc.add(backAnnotateLayNetNamesFromSch, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        ncc.add(misc, gridBagConstraints);
        misc.getAccessibleContext().setAccessibleName("Miscellaneous");

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        getContentPane().add(ncc, gridBagConstraints);

        pack();
    }//GEN-END:initComponents

    private void hierAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hierAllActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_hierAllActionPerformed

    private void skipPassedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_skipPassedActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_skipPassedActionPerformed

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField absoluteSizeTolerance;
    private javax.swing.JCheckBox backAnnotateLayNetNamesFromSch;
    private javax.swing.JPanel checkingAllCells;
    private javax.swing.JCheckBox enableSizeChecking;
    private javax.swing.JPanel errorReport;
    private javax.swing.JRadioButton flatTop;
    private javax.swing.JCheckBox haltAfterFindingFirstMismatchedCell;
    private javax.swing.JRadioButton hierAll;
    private javax.swing.JTextField howMuchStatus;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel75;
    private javax.swing.JLabel jLabel76;
    private javax.swing.JRadioButton listAnn;
    private javax.swing.JTextField maxMatched;
    private javax.swing.JTextField maxMembers;
    private javax.swing.JTextField maxMismatched;
    private javax.swing.JPanel misc;
    private javax.swing.JPanel ncc;
    private javax.swing.JPanel operation;
    private javax.swing.ButtonGroup operationGroup;
    private javax.swing.JPanel progressReport;
    private javax.swing.JTextField relativeSizeTolerance;
    private javax.swing.JPanel sizeChecking;
    private javax.swing.JCheckBox skipPassed;
    // End of variables declaration//GEN-END:variables

}
