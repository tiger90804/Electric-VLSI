/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: WellCheckTab.java
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

import com.sun.electric.tool.erc.ERC;

import javax.swing.JPanel;

/**
 * Class to handle the "Well Check" tab of the Preferences dialog.
 */
public class WellCheckTab extends PreferencePanel
{
	/** Creates new form WellCheckTab */
	public WellCheckTab(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
	}
	public JPanel getPanel() { return wellCheck; }

	public String getName() { return "Well Check"; }

	//private boolean initialWellCheckNWellConnectToPower;

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Well Check tab.
	 */
	public void init()
	{
		switch (ERC.getPWellCheck())
		{
			case 0: wellPMustHaveAllContacts.setSelected(true);   break;
			case 1: wellPMustHave1Contact.setSelected(true);      break;
			case 2: wellPNoContactCheck.setSelected(true);        break;
		}
		wellPMustConnectGround.setSelected(ERC.isMustConnectPWellToGround());

		switch (ERC.getNWellCheck())
		{
			case 0: wellNMustHaveAllContacts.setSelected(true);   break;
			case 1: wellNMustHave1Contact.setSelected(true);      break;
			case 2: wellNNoContactCheck.setSelected(true);        break;
		}
		wellNMustConnectPower.setSelected(ERC.isMustConnectNWellToPower());

		wellFindFarthestDistance.setSelected(ERC.isFindWorstCaseWell());
		drcCheck.setSelected(ERC.isDRCCheck());
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Well Check tab.
	 */
	public void term()
	{
		int currentPWellRule = 0;
		if (wellPMustHave1Contact.isSelected()) currentPWellRule = 1; else
			if (wellPNoContactCheck.isSelected()) currentPWellRule = 2;
		if (currentPWellRule != ERC.getPWellCheck())
			ERC.setPWellCheck(currentPWellRule);

		boolean check = wellPMustConnectGround.isSelected();
		if (check != ERC.isMustConnectPWellToGround())
			ERC.setMustConnectPWellToGround(check);

		int currentNWellRule = 0;
		if (wellNMustHave1Contact.isSelected()) currentNWellRule = 1; else
			if (wellNNoContactCheck.isSelected()) currentNWellRule = 2;
		if (currentNWellRule != ERC.getNWellCheck())
			ERC.setNWellCheck(currentNWellRule);

		check = wellNMustConnectPower.isSelected();
		if (check != ERC.isMustConnectNWellToPower())
			ERC.setMustConnectNWellToPower(check);

		check = wellFindFarthestDistance.isSelected();
		if (check != ERC.isFindWorstCaseWell())
			ERC.setFindWorstCaseWell(check);

		check = drcCheck.isSelected();
		if (check != ERC.isDRCCheck())
			ERC.setDRCCheck(check);
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        wellCheckPWell = new javax.swing.ButtonGroup();
        wellCheckNWell = new javax.swing.ButtonGroup();
        wellCheck = new javax.swing.JPanel();
        wellFindFarthestDistance = new javax.swing.JCheckBox();
        jPanel1 = new javax.swing.JPanel();
        wellPMustConnectGround = new javax.swing.JCheckBox();
        jPanel3 = new javax.swing.JPanel();
        wellPNoContactCheck = new javax.swing.JRadioButton();
        wellPMustHave1Contact = new javax.swing.JRadioButton();
        wellPMustHaveAllContacts = new javax.swing.JRadioButton();
        jPanel2 = new javax.swing.JPanel();
        wellNMustConnectPower = new javax.swing.JCheckBox();
        jPanel4 = new javax.swing.JPanel();
        wellNMustHaveAllContacts = new javax.swing.JRadioButton();
        wellNMustHave1Contact = new javax.swing.JRadioButton();
        wellNNoContactCheck = new javax.swing.JRadioButton();
        drcCheck = new javax.swing.JCheckBox();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Tool Options");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });

        wellCheck.setLayout(new java.awt.GridBagLayout());

        wellFindFarthestDistance.setText("Find farthest distance from contact to edge");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        wellCheck.add(wellFindFarthestDistance, gridBagConstraints);

        jPanel1.setLayout(new java.awt.GridBagLayout());

        jPanel1.setBorder(new javax.swing.border.TitledBorder("For P-Well"));
        wellPMustConnectGround.setText("Must connect to Ground");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel1.add(wellPMustConnectGround, gridBagConstraints);

        jPanel3.setLayout(new java.awt.GridBagLayout());

        jPanel3.setBorder(new javax.swing.border.EmptyBorder(new java.awt.Insets(1, 1, 1, 1)));
        wellPNoContactCheck.setText("Do not check for contacts");
        wellCheckPWell.add(wellPNoContactCheck);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel3.add(wellPNoContactCheck, gridBagConstraints);

        wellPMustHave1Contact.setText("Must have at least 1 contact");
        wellCheckPWell.add(wellPMustHave1Contact);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel3.add(wellPMustHave1Contact, gridBagConstraints);

        wellPMustHaveAllContacts.setText("Must have contact in every area");
        wellCheckPWell.add(wellPMustHaveAllContacts);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel3.add(wellPMustHaveAllContacts, gridBagConstraints);

        jPanel1.add(jPanel3, new java.awt.GridBagConstraints());

        wellCheck.add(jPanel1, new java.awt.GridBagConstraints());
        jPanel1.getAccessibleContext().setAccessibleDescription("");

        jPanel2.setLayout(new java.awt.GridBagLayout());

        jPanel2.setBorder(new javax.swing.border.TitledBorder("For N-Well"));
        wellNMustConnectPower.setText("Must connect to Power");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel2.add(wellNMustConnectPower, gridBagConstraints);

        jPanel4.setLayout(new java.awt.GridBagLayout());

        jPanel4.setBorder(new javax.swing.border.EmptyBorder(new java.awt.Insets(1, 1, 1, 1)));
        wellNMustHaveAllContacts.setText("Must have contact in every area");
        wellCheckNWell.add(wellNMustHaveAllContacts);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel4.add(wellNMustHaveAllContacts, gridBagConstraints);

        wellNMustHave1Contact.setText("Must have at least 1 contact");
        wellCheckNWell.add(wellNMustHave1Contact);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel4.add(wellNMustHave1Contact, gridBagConstraints);

        wellNNoContactCheck.setText("Do not check for contacts");
        wellCheckNWell.add(wellNNoContactCheck);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel4.add(wellNNoContactCheck, gridBagConstraints);

        jPanel2.add(jPanel4, new java.awt.GridBagConstraints());

        wellCheck.add(jPanel2, new java.awt.GridBagConstraints());

        drcCheck.setText("Check DRC Spacing Rule");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        wellCheck.add(drcCheck, gridBagConstraints);

        getContentPane().add(wellCheck, new java.awt.GridBagConstraints());

        pack();
    }//GEN-END:initComponents
	
	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox drcCheck;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel wellCheck;
    private javax.swing.ButtonGroup wellCheckNWell;
    private javax.swing.ButtonGroup wellCheckPWell;
    private javax.swing.JCheckBox wellFindFarthestDistance;
    private javax.swing.JCheckBox wellNMustConnectPower;
    private javax.swing.JRadioButton wellNMustHave1Contact;
    private javax.swing.JRadioButton wellNMustHaveAllContacts;
    private javax.swing.JRadioButton wellNNoContactCheck;
    private javax.swing.JCheckBox wellPMustConnectGround;
    private javax.swing.JRadioButton wellPMustHave1Contact;
    private javax.swing.JRadioButton wellPMustHaveAllContacts;
    private javax.swing.JRadioButton wellPNoContactCheck;
    // End of variables declaration//GEN-END:variables
	
}
