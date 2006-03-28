/*
 * CVSTab.java
 *
 * Created on March 10, 2006, 2:16 PM
 */

package com.sun.electric.tool.user.dialogs.options;

import com.sun.electric.tool.cvspm.CVS;
import com.sun.electric.tool.cvspm.Update;
import com.sun.electric.tool.cvspm.CVSLibrary;
import com.sun.electric.database.hierarchy.Library;

import javax.swing.*;
import java.util.Iterator;

/**
 *
 * @author  gainsley
 */
public class CVSTab extends PreferencePanel {
    
    /** Creates new form CVSTab */
    public CVSTab(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
    }

    /** return the panel to use for this preferences tab. */
    public JPanel getPanel() { return cvsPanel; }

    /** return the name of this preferences tab. */
    public String getName() { return "CVS"; }

    /**
     * Method called at the start of the dialog.
     * Caches current values and displays them in the CDL tab.
     */
    public void init() {
        cvsRepository.setText(CVS.getCVSRepository());
        cvsProgram.setText(CVS.getCVSProgram());
        enableCVS.setSelected(CVS.isEnabled());
    }

    /**
     * Method called when the "OK" panel is hit.
     * Updates any changed fields in the CDL tab.
     */
    public void term() {
        String str = cvsRepository.getText();
        if (!str.equals(CVS.getCVSRepository())) CVS.setCVSRepository(str);
        str = cvsProgram.getText();
        if (!str.equals(CVS.getCVSProgram())) CVS.setCVSProgram(str);
        boolean b = enableCVS.isSelected();
        if (b != CVS.isEnabled()) {
            CVS.setEnabled(b);
            if (b) {
                // user turned on CVS, update status of all libraries
                for (Iterator<Library> it = Library.getLibraries(); it.hasNext(); ) {
                    CVSLibrary.addLibrary(it.next());
                }
                Update.updateAllLibraries(Update.STATUS);
            } else {
                for (Iterator<Library> it = Library.getLibraries(); it.hasNext(); ) {
                    CVSLibrary.removeLibrary(it.next());
                }
            }
        }
    }


    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        cvsPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        cvsRepository = new javax.swing.JTextField();
        cvsProgram = new javax.swing.JTextField();
        enableCVS = new javax.swing.JCheckBox();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        cvsPanel.setLayout(new java.awt.GridBagLayout());

        jLabel1.setText("CVS program: ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        cvsPanel.add(jLabel1, gridBagConstraints);

        jLabel2.setText("CVS Repository: ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        cvsPanel.add(jLabel2, gridBagConstraints);

        cvsRepository.setColumns(16);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        cvsPanel.add(cvsRepository, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        cvsPanel.add(cvsProgram, gridBagConstraints);

        enableCVS.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        enableCVS.setText("Enable CVS");
        enableCVS.setMargin(new java.awt.Insets(0, 0, 0, 0));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(12, 4, 12, 4);
        cvsPanel.add(enableCVS, gridBagConstraints);
        enableCVS.getAccessibleContext().setAccessibleName("EnableCVS");

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(cvsPanel, gridBagConstraints);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel cvsPanel;
    private javax.swing.JTextField cvsProgram;
    private javax.swing.JTextField cvsRepository;
    private javax.swing.JCheckBox enableCVS;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    // End of variables declaration//GEN-END:variables
    
}
