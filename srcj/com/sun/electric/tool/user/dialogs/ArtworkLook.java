/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ArtworkLook.java
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
package com.sun.electric.tool.user.dialogs;

import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.CircuitChanges;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.user.ui.TopLevel;

import java.awt.GridBagConstraints;

/**
 * Class to handle the "Artwork Look" dialog.
 */
public class ArtworkLook extends EDialog
{
	public static void showArtworkLookDialog()
	{
		// see if there is a piece of artwork selected]
		Highlight h = Highlight.getOneHighlight();
		if (h == null) return;
		if (h.getType() != Highlight.Type.EOBJ)
		{
			System.out.println("Must select a single artwork node or arc");
			return;
		}
		ElectricObject eObj = h.getElectricObject();
		if (eObj instanceof PortInst)
		{
			eObj = ((PortInst)eObj).getNodeInst();
		}
		boolean foundArt = false;
		if (eObj instanceof NodeInst)
		{
			NodeInst ni = (NodeInst)eObj;
			if (ni.getProto() instanceof PrimitiveNode &&
				ni.getProto().getTechnology() == Artwork.tech)
					foundArt = true;
		} else if (eObj instanceof ArcInst)
		{
			ArcInst ai = (ArcInst)eObj;
			if (ai.getProto().getTechnology() == Artwork.tech) foundArt = true;
		}
		if (!foundArt)
		{
			System.out.println("Selected object must be from the Artwork technology");
			return;
		}
		ArtworkLook dialog = new ArtworkLook(TopLevel.getCurrentJFrame(), true, eObj);
		dialog.setVisible(true);
	}

	ColorPatternPanel.Info li;
	ElectricObject eObj;

	/** Creates new form ArtworkLook */
	public ArtworkLook(java.awt.Frame parent, boolean modal, ElectricObject eObj)
	{
		super(parent, modal);
		initComponents();
		this.eObj = eObj;
        getRootPane().setDefaultButton(ok);

		// make the color/pattern panel
		ColorPatternPanel colorPatternPanel = new ColorPatternPanel(false);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;      gbc.gridy = 0;
		gbc.gridwidth = 2;  gbc.gridheight = 1;
		gbc.weightx = 1;    gbc.weighty = 1;
		gbc.insets = new java.awt.Insets(4, 4, 4, 4);
		getContentPane().add(colorPatternPanel, gbc);
		pack();

		EGraphics graphics = Artwork.makeGraphics(eObj);
		if (graphics == null)
		{
			graphics = new EGraphics(EGraphics.SOLID, EGraphics.SOLID, 0, 0,0,0, 0.8,1,
				new int[] {0xffff, 0xffff, 0xffff, 0xffff, 0xffff, 0xffff, 0xffff, 0xffff,
					0xffff, 0xffff, 0xffff, 0xffff, 0xffff, 0xffff, 0xffff, 0xffff});
		}
		li = new ColorPatternPanel.Info(graphics);
		colorPatternPanel.setColorPattern(li);
	}

	protected void escapePressed() { cancel(null); }

	private void applyDialog()
	{
		if (li.updateGraphics())
		{
			ApplyChanges job = new ApplyChanges(this);
		}
	}

	/**
	 * Class to update graphics on an artwork node or arc.
	 */
	private static class ApplyChanges extends Job
	{
		ArtworkLook dialog;

		protected ApplyChanges(ArtworkLook dialog)
		{
			super("Update Edit Options", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.dialog = dialog;
			startJob();
		}

		public boolean doIt()
		{
			Artwork.setGraphics(dialog.li.graphics, dialog.eObj);
			return true;
		}
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    private void initComponents()//GEN-BEGIN:initComponents
    {
        java.awt.GridBagConstraints gridBagConstraints;

        cancel = new javax.swing.JButton();
        ok = new javax.swing.JButton();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Appearance of Artwork");
        setName("");
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
                closeDialog(evt);
            }
        });

        cancel.setText("Cancel");
        cancel.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                cancel(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(cancel, gridBagConstraints);

        ok.setText("OK");
        ok.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                ok(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        getContentPane().add(ok, gridBagConstraints);

        pack();
    }//GEN-END:initComponents

	private void cancel(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cancel
	{//GEN-HEADEREND:event_cancel
		closeDialog(null);
	}//GEN-LAST:event_cancel

	private void ok(java.awt.event.ActionEvent evt)//GEN-FIRST:event_ok
	{//GEN-HEADEREND:event_ok
		applyDialog();
		closeDialog(null);
	}//GEN-LAST:event_ok

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancel;
    private javax.swing.JButton ok;
    // End of variables declaration//GEN-END:variables
}
