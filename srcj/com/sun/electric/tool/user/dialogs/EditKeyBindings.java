/*
 * EditKeyBindings.java
 *
 * Created on January 14, 2004, 12:40 PM
 */

package com.sun.electric.tool.user.dialogs;

import com.sun.electric.tool.user.MenuCommands;
import com.sun.electric.tool.user.KeyBindingManager;
import com.sun.electric.tool.user.ui.Menu;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.InputMethodListener;
import java.awt.event.InputMethodEvent;
import java.util.List;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.tree.TreePath;



/**
 *
 * @author  gainsley
 */
public class EditKeyBindings extends javax.swing.JDialog implements TreeSelectionListener {
    
    /** MenuBar for building dialog tree */                 JMenuBar menuBar;
    
    /** class to encapsulate a tree node for displaying key bindings.
     * The toString() method is overridden to show the key binding next to the
     * command name.  This class encapsulates both JMenuItem and Menu, note
     * that both extend JMenuItem.
     */
    private class KeyBoundTreeNode
    {
        private JMenuItem menuItem;
        
        KeyBoundTreeNode(JMenuItem menuItem) {
            this.menuItem = menuItem;
        }
        
        public JMenuItem getMenuItem() { return menuItem; }
        
        /** 
         * Convert to parsable String representing KeyStroke.
         * Convert back to KeyStroke using KeyStroke.getKeyStroke(string).
         */
        public String toString() {
            if (menuItem != null) {
                return menuItem.getText();
                /*
                StringBuffer buf = new StringBuffer(menuItem.getText());
                KeyStroke key = menuItem.getAccelerator();
                if (key != null) {
                    int mods = key.getModifiers();
                    buf.append("   [ ");
                    if (mods != 0)
                        buf.append(KeyEvent.getKeyModifiersText(mods)+"+");
                    buf.append(KeyEvent.getKeyText(key.getKeyCode()));
                    buf.append(" ]");
                }
                return buf.toString();
                */
            }
            return "---------------";               // separator
        }
    }

    /** Creates new form EditKeyBindings */
    public EditKeyBindings(java.awt.Frame parent, boolean modal) {
		super(parent, modal);
        setLocation(300, 100);
        
        initComponents();
        buildCommandsTree();
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        commandsTree = new javax.swing.JTree();
        jPanel2 = new javax.swing.JPanel();
        add = new javax.swing.JButton();
        remove = new javax.swing.JButton();
        resetitem = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        bindingsJList = new javax.swing.JList();
        jPanel3 = new javax.swing.JPanel();
        done = new javax.swing.JButton();
        reset = new javax.swing.JButton();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Edit Key Bindings");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                exitForm(evt);
            }
        });

        jPanel1.setLayout(new java.awt.GridLayout(1, 0));

        jScrollPane1.setViewportView(commandsTree);

        jPanel1.add(jScrollPane1);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        getContentPane().add(jPanel1, gridBagConstraints);

        jPanel2.setLayout(new java.awt.GridBagLayout());

        jPanel2.setBorder(new javax.swing.border.EtchedBorder());
        add.setText("Add");
        add.setToolTipText("add a shortcut");
        add.setPreferredSize(new java.awt.Dimension(68, 28));
        add.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        jPanel2.add(add, gridBagConstraints);

        remove.setText("Remove");
        remove.setToolTipText("remove a shortcut");
        remove.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        jPanel2.add(remove, gridBagConstraints);

        resetitem.setText("Reset");
        resetitem.setToolTipText("reset to default setting");
        resetitem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetitemActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 4);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        jPanel2.add(resetitem, gridBagConstraints);

        jPanel4.setLayout(new java.awt.GridBagLayout());

        jLabel1.setText("Shortcuts:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(3, 3, 3, 3);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel4.add(jLabel1, gridBagConstraints);

        bindingsJList.setBorder(new javax.swing.border.EtchedBorder());
        bindingsJList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel4.add(bindingsJList, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        jPanel2.add(jPanel4, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        getContentPane().add(jPanel2, gridBagConstraints);

        jPanel3.setLayout(new java.awt.GridBagLayout());

        done.setText("         Done         ");
        done.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                doneActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 0, 8);
        jPanel3.add(done, gridBagConstraints);

        reset.setText("Reset All to Defaults");
        reset.setToolTipText("reset all to default settings");
        reset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 0, 8);
        jPanel3.add(reset, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        getContentPane().add(jPanel3, gridBagConstraints);

        pack();
    }//GEN-END:initComponents

    private void resetitemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resetitemActionPerformed
        // get currently selected node
        KeyBoundTreeNode treeNode = getSelected();
        if (treeNode == null) return;
        if (treeNode.getMenuItem() == null) return;
        Menu.resetToDefaultKeyStroke(treeNode.getMenuItem());
        DefaultTreeModel model = (DefaultTreeModel)commandsTree.getModel();
        model.reload(getSelectedTreeNode());
        //instructions.setText(treeNode.toString());
    }//GEN-LAST:event_resetitemActionPerformed

    private void removeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeActionPerformed
        // get currently selected node
        KeyBoundTreeNode treeNode = getSelected();
        if (treeNode == null) return;
        if (treeNode.getMenuItem() == null) return;
        // set key binding in prefs
        Menu.setUserMenuItemKeyStroke(treeNode.getMenuItem(), null);
        // set key binding to show up on dialog
        treeNode.menuItem.setAccelerator(null);
        DefaultTreeModel model = (DefaultTreeModel)commandsTree.getModel();
        model.reload(getSelectedTreeNode());
        //instructions.setText(treeNode.toString());
    }//GEN-LAST:event_removeActionPerformed

    /** Resets all Key Bindings to their default values */
    private void resetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resetActionPerformed
        for (int i=0; i<menuBar.getMenuCount(); i++) {
            Menu menu = (Menu)menuBar.getMenu(i);
            resetMenuKeys(menu);
        }
        DefaultTreeModel model = (DefaultTreeModel)commandsTree.getModel();
        model.reload();
    }//GEN-LAST:event_resetActionPerformed

    private void addActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addActionPerformed
        KeyBoundTreeNode treeNode = getSelected();
        if (treeNode == null) return;
        if (treeNode.getMenuItem() == null) return;
        // get text from text box
        //String text = keyinput.getText();
        KeyStroke newKey = null;//KeyStroke.getKeyStroke(text);
        if (newKey == null) {
            //instructions.setText("bad key combination");
            return;
        }
        // set key binding in prefs
        Menu.setUserMenuItemKeyStroke(treeNode.getMenuItem(), newKey);
        // set key binding to show up on dialog
        treeNode.getMenuItem().setAccelerator(newKey);
        DefaultTreeModel model = (DefaultTreeModel)commandsTree.getModel();
        model.reload(getSelectedTreeNode());
        //instructions.setText(treeNode.toString());
        // check if KeyStroke assigned to another MenuItem; display message
        JMenuItem conflictItem = checkKeyStrokeConflict(treeNode.getMenuItem());
        if (conflictItem != null ) {
            KeyBoundTreeNode tnode = new KeyBoundTreeNode(conflictItem);
            //instructions.setText("Warning: Duplicate assignment: "+tnode.toString());
        }
    }//GEN-LAST:event_addActionPerformed

    private void doneActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_doneActionPerformed
        setVisible(false);
    }//GEN-LAST:event_doneActionPerformed
    
    /** Exit the Application */
    private void exitForm(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_exitForm

    }//GEN-LAST:event_exitForm
    
    /** Build tree of menu commands */
    private void buildCommandsTree() {
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode();
        
        menuBar = MenuCommands.createMenuBar();
        // now convert menuBar to tree
        for (int i=0; i<menuBar.getMenuCount(); i++) {
            Menu menu = (Menu)menuBar.getMenu(i);
            DefaultMutableTreeNode menuNode = new DefaultMutableTreeNode(new KeyBoundTreeNode(menu));
            rootNode.add(menuNode);
            addMenu(menuNode, menu);
        }
        
        commandsTree.setModel(new DefaultTreeModel(rootNode));
        // single selection as default
		commandsTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

		// do not show top-level
		commandsTree.setRootVisible(false);
		commandsTree.setShowsRootHandles(true);
		commandsTree.setToggleClickCount(3);
        commandsTree.addTreeSelectionListener(this);
    }
    
    /** Adds menu items to parentNode, which represents Menu menu. */
    private void addMenu(DefaultMutableTreeNode parentNode, Menu menu) {
        for (int i=0; i<menu.getItemCount(); i++) {
            JMenuItem menuItem = menu.getItem(i);
            DefaultMutableTreeNode menuItemNode = new DefaultMutableTreeNode(new KeyBoundTreeNode(menuItem));
            parentNode.add(menuItemNode);
            if (menuItem instanceof JMenu)
                addMenu(menuItemNode, (Menu)menuItem);              // recurse
        }
    }

    /** Reset menu keys under Menu.  Recursive method */
    private void resetMenuKeys(Menu menu) {
        for (int i=0; i<menu.getItemCount(); i++) {
            JMenuItem menuItem = menu.getItem(i);
            if (menuItem == null) continue; // separator
            if (menuItem instanceof Menu)
                resetMenuKeys((Menu)menuItem);                     // recurse
            else
                Menu.resetToDefaultKeyStroke(menuItem);
        }
    }
        
    /** Called when selection of Node in tree changes */
    public void valueChanged(javax.swing.event.TreeSelectionEvent e) {
        TreePath path = e.getPath();
        if (path == null) return;
        Object obj = path.getLastPathComponent();
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)obj;
        Object n = node.getUserObject();
        if (!(n instanceof KeyBoundTreeNode)) return;
        KeyBoundTreeNode treeNode = (KeyBoundTreeNode)n;
        if (treeNode.getMenuItem() == null) {
            bindingsJList.setListData(new Object [] {});
            return;          // separator
        }
        List bindings = Menu.getKeyBindingsFor(treeNode.getMenuItem());
        if (bindings == null) {
            bindingsJList.setListData(new Object [] {});
            return;          // separator
        }
        bindingsJList.setListData(bindings.toArray());
        //instructions.setText(treeNode.toString());
    }
    

    //-----------------------Private Utility Methods--------------------------
    
    /** Get selected KeyBoundTreeNode.  
     * Returns null if no valid KeyBoundTreeNode selected in tree. 
     */
    private KeyBoundTreeNode getSelected() {
        DefaultMutableTreeNode node = getSelectedTreeNode();
        Object obj = node.getUserObject();
        if (!(obj instanceof KeyBoundTreeNode)) return null;
        KeyBoundTreeNode treeNode = (KeyBoundTreeNode)obj;
        return treeNode;
    }
    
    /** get selected DefaultMutableTreeNode.
     * Returns null if no valid DefaultMutableTree node selected in tree.
     */
    private DefaultMutableTreeNode getSelectedTreeNode() {
        TreePath path = commandsTree.getSelectionPath();
        if (path == null) return null;
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
        return node;
    }

    /** Check if another MenuItem has key assigned to it.
     * If so, return that MenuItem */
    private JMenuItem checkKeyStrokeConflict(JMenuItem origItem) {
        for (int i=0; i<menuBar.getMenuCount(); i++) {
            Menu menu = (Menu)menuBar.getMenu(i);
            JMenuItem conflict = checkKeyStrokeConflict(menu, origItem);
            if (conflict != null) return conflict;
        }
        return null;
    }
    
    /** Recursive method to check if a MenuItem in menu has been assigned to the
     * KeyStroke key.  Return a MenuItem if conflict found, otherwise return null. */
    private JMenuItem checkKeyStrokeConflict(Menu menu, JMenuItem origItem) {
        for (int i=0; i<menu.getItemCount(); i++) {
            JMenuItem conflict = null;
            JMenuItem menuItem = menu.getItem(i);
            if (menuItem == null) continue;                     // separator
            if (menuItem instanceof Menu) {                       // recurse
                conflict = checkKeyStrokeConflict((Menu)menuItem, origItem);
                if (conflict != null) return conflict;
            } else {
                if (origItem == menuItem) continue;
                String origKey = KeyBindingManager.keyStrokeToString(origItem.getAccelerator());
                String menuKey = KeyBindingManager.keyStrokeToString(menuItem.getAccelerator());
                if (origKey.equals(menuKey)) return menuItem;
            }
        }
        return null;
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton add;
    private javax.swing.JTree commandsTree;
    private javax.swing.JButton done;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JList bindingsJList;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JButton remove;
    private javax.swing.JButton reset;
    private javax.swing.JButton resetitem;
    // End of variables declaration//GEN-END:variables
    
}
