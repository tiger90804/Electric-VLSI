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

import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.lib.LibFile;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.erc.ERC;
import com.sun.electric.tool.io.output.Spice;
import com.sun.electric.tool.io.output.Verilog;
import com.sun.electric.tool.logicaleffort.LETool;
import com.sun.electric.tool.routing.Routing;
import com.sun.electric.tool.simulation.Simulation;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.dialogs.EDialog;
import com.sun.electric.tool.user.ui.TopLevel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.util.Iterator;
import java.util.HashMap;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;
import javax.swing.DefaultListModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.Document;
import javax.swing.text.BadLocationException;


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
	public JPanel getPanel() { return drc; }

	public String getName() { return "DRC"; }

	private boolean initialDRCIncrementalOn;
	private boolean initialDRCOneErrorPerCell;
	private boolean initialDRCUseMultipleThreads;
	private boolean initialDRCIgnoreCenterCuts;
	private int initialDRCNumberOfThreads;
	private boolean requestedDRCClearDates;

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the DRC tab.
	 */
	public void init()
	{
		initialDRCIncrementalOn = DRC.isIncrementalDRCOn();
		drcIncrementalOn.setSelected(initialDRCIncrementalOn);

		initialDRCOneErrorPerCell = DRC.isOneErrorPerCell();
		drcOneErrorPerCell.setSelected(initialDRCOneErrorPerCell);

		initialDRCUseMultipleThreads = DRC.isUseMultipleThreads();
		drcUseMultipleThreads.setSelected(initialDRCUseMultipleThreads);

		initialDRCNumberOfThreads = DRC.getNumberOfThreads();
		drcNumberOfThreads.setText(Integer.toString(initialDRCNumberOfThreads));

		initialDRCIgnoreCenterCuts = DRC.isIgnoreCenterCuts();
		drcIgnoreCenterCuts.setSelected(initialDRCIgnoreCenterCuts);

		requestedDRCClearDates = false;
		drcClearValidDates.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				drcClearValidDates.setEnabled(false);		
				requestedDRCClearDates = true;
			}
		});

		// not yet
		drcIncrementalOn.setSelected(false);
		drcIncrementalOn.setEnabled(false);
		drcUseMultipleThreads.setEnabled(false);
		drcNumberOfThreads.setEditable(false);
		drcEditRulesDeck.setEnabled(false);		
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the DRC tab.
	 */
	public void term()
	{
		boolean currentIncrementalOn = drcIncrementalOn.isSelected();
		if (currentIncrementalOn != initialDRCIncrementalOn)
			DRC.setIncrementalDRCOn(currentIncrementalOn);

		boolean currentOneErrorPerCell = drcOneErrorPerCell.isSelected();
		if (currentOneErrorPerCell != initialDRCOneErrorPerCell)
			DRC.setOneErrorPerCell(currentOneErrorPerCell);

		boolean currentUseMultipleThreads = drcUseMultipleThreads.isSelected();
		if (currentUseMultipleThreads != initialDRCUseMultipleThreads)
			DRC.setUseMultipleThreads(currentUseMultipleThreads);

		int currentNumberOfThreads = TextUtils.atoi(drcNumberOfThreads.getText());
		if (currentNumberOfThreads != initialDRCNumberOfThreads)
			DRC.setNumberOfThreads(currentNumberOfThreads);

		boolean currentIgnoreCenterCuts = drcIgnoreCenterCuts.isSelected();
		if (currentIgnoreCenterCuts != initialDRCIgnoreCenterCuts)
			DRC.setIgnoreCenterCuts(currentIgnoreCenterCuts);

		if (requestedDRCClearDates) DRC.resetDRCDates();
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    private void initComponents()//GEN-BEGIN:initComponents
    {
        java.awt.GridBagConstraints gridBagConstraints;

        drc = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        drcIncrementalOn = new javax.swing.JCheckBox();
        jPanel4 = new javax.swing.JPanel();
        drcOneErrorPerCell = new javax.swing.JCheckBox();
        drcClearValidDates = new javax.swing.JButton();
        jPanel5 = new javax.swing.JPanel();
        drcUseMultipleThreads = new javax.swing.JCheckBox();
        jLabel33 = new javax.swing.JLabel();
        drcNumberOfThreads = new javax.swing.JTextField();
        drcIgnoreCenterCuts = new javax.swing.JCheckBox();
        jPanel6 = new javax.swing.JPanel();
        drcEditRulesDeck = new javax.swing.JButton();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Tool Options");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
                closeDialog(evt);
            }
        });

        drc.setLayout(new java.awt.GridBagLayout());

        jPanel3.setLayout(new java.awt.GridBagLayout());

        jPanel3.setBorder(new javax.swing.border.TitledBorder("Incremental DRC"));
        drcIncrementalOn.setText("On");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 0);
        jPanel3.add(drcIncrementalOn, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        drc.add(jPanel3, gridBagConstraints);

        jPanel4.setLayout(new java.awt.GridBagLayout());

        jPanel4.setBorder(new javax.swing.border.TitledBorder("Hierarchical DRC"));
        drcOneErrorPerCell.setText("Just 1 error per cell");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 0);
        jPanel4.add(drcOneErrorPerCell, gridBagConstraints);

        drcClearValidDates.setText("Clear valid DRC dates");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 0);
        jPanel4.add(drcClearValidDates, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        drc.add(jPanel4, gridBagConstraints);

        jPanel5.setLayout(new java.awt.GridBagLayout());

        jPanel5.setBorder(new javax.swing.border.TitledBorder("Incremental and Hierarchical"));
        drcUseMultipleThreads.setText("Use multiple threads");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 0);
        jPanel5.add(drcUseMultipleThreads, gridBagConstraints);

        jLabel33.setText("Number of threads:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 0);
        jPanel5.add(jLabel33, gridBagConstraints);

        drcNumberOfThreads.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 9;
        jPanel5.add(drcNumberOfThreads, gridBagConstraints);

        drcIgnoreCenterCuts.setText("Ignore center cuts in large contacts");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 0);
        jPanel5.add(drcIgnoreCenterCuts, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        drc.add(jPanel5, gridBagConstraints);

        jPanel6.setLayout(new java.awt.GridBagLayout());

        jPanel6.setBorder(new javax.swing.border.TitledBorder("Dracula DRC Interface"));
        drcEditRulesDeck.setText("Edit Rules Deck");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 20, 4, 0);
        jPanel6.add(drcEditRulesDeck, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        drc.add(jPanel6, gridBagConstraints);

        getContentPane().add(drc, new java.awt.GridBagConstraints());

        pack();
    }//GEN-END:initComponents
	
	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel drc;
    private javax.swing.JButton drcClearValidDates;
    private javax.swing.JButton drcEditRulesDeck;
    private javax.swing.JCheckBox drcIgnoreCenterCuts;
    private javax.swing.JCheckBox drcIncrementalOn;
    private javax.swing.JTextField drcNumberOfThreads;
    private javax.swing.JCheckBox drcOneErrorPerCell;
    private javax.swing.JCheckBox drcUseMultipleThreads;
    private javax.swing.JLabel jLabel33;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    // End of variables declaration//GEN-END:variables
	
}
