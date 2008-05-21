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

import java.awt.Frame;

import javax.swing.JPanel;

/**
 * Class to handle the "Skill" tab of the Preferences dialog.
 */
public class SkillTab extends PreferencePanel
{
	/** Creates new form SkillTab */
	public SkillTab(Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
	}

	/** return the panel to use for this preferences tab. */
	public JPanel getPanel() { return skill; }

	/** return the name of this preferences tab. */
	public String getName() { return "Skill"; }

	/**
	 * Method called at the start of the dialog.
	 * Caches current values and displays them in the Skill tab.
	 */
	public void init()
	{
		skillNoSubCells.setSelected(IOTool.isSkillExcludesSubcells());
		skillFlattenHierarchy.setSelected( IOTool.isSkillFlattensHierarchy());
        skillGDSNameLimit.setSelected(IOTool.isSkillGDSNameLimit());
		if (!IOTool.hasSkill())
		{
			skillNoSubCells.setEnabled(false);
			skillFlattenHierarchy.setEnabled(false);
	        skillGDSNameLimit.setEnabled(false);
		}
	}

	/**
	 * Method called when the "OK" panel is hit.
	 * Updates any changed fields in the Skill tab.
	 */
	public void term()
	{
		boolean currBoolean = skillNoSubCells.isSelected();
		if (currBoolean != IOTool.isSkillExcludesSubcells())
			IOTool.setSkillExcludesSubcells(currBoolean);

		currBoolean = skillFlattenHierarchy.isSelected();
		if (currBoolean !=  IOTool.isSkillFlattensHierarchy())
			IOTool.setSkillFlattensHierarchy(currBoolean);

        currBoolean = skillGDSNameLimit.isSelected();
        if (currBoolean != IOTool.isSkillGDSNameLimit())
            IOTool.setSkillGDSNameLimit(currBoolean);
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

        skill = new javax.swing.JPanel();
        skillNoSubCells = new javax.swing.JCheckBox();
        skillFlattenHierarchy = new javax.swing.JCheckBox();
        skillGDSNameLimit = new javax.swing.JCheckBox();

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

        skill.setLayout(new java.awt.GridBagLayout());

        skillNoSubCells.setText("Do not include subcells");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        skill.add(skillNoSubCells, gridBagConstraints);

        skillFlattenHierarchy.setText("Flatten hierarchy");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        skill.add(skillFlattenHierarchy, gridBagConstraints);

        skillGDSNameLimit.setText("GDS name limit (32 chars)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        skill.add(skillGDSNameLimit, gridBagConstraints);

        getContentPane().add(skill, new java.awt.GridBagConstraints());

        pack();
    }// </editor-fold>//GEN-END:initComponents

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel skill;
    private javax.swing.JCheckBox skillFlattenHierarchy;
    private javax.swing.JCheckBox skillGDSNameLimit;
    private javax.swing.JCheckBox skillNoSubCells;
    // End of variables declaration//GEN-END:variables

}
