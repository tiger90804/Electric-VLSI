/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Network.java
 * Written by: Dmitry Nadezhin, Sun Microsystems.
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
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
package com.sun.electric.database.network;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.text.ArrayIterator;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/** Networks represent connectivity.
 *
 * <p> For a Cell, each Network represents a collection of PortInsts
 * that are electrically connected.
 */
public class Network {
    private static final String[] NULL_STRING_ARRAY = {};
    
    // ------------------------- private data ------------------------------
    private Netlist netlist; // Cell that owns this Network
    private int netIndex; // Index of this Network in Netlist.
    /**
     * Array of names.
     * First names are exported names in STRING_NUMBER_ORDER,
     * Then internal user-defined names in STRING_NUMBER_ORDER.
     * If this net has no exported or user-defined names, then this
     * list contains one of temporary names.
     * Hence the first name in the list are most appropriate.
     **/
    private String[] names = NULL_STRING_ARRAY;
    /**
     * Number of export names.
     */
    private int exportedNamesCount;
    /**
     * True if this net has user-defiend names/
     */
    private boolean isUsernamed;
    
    // ----------------------- protected and private methods -----------------
    
    /**
     * Creates Network in a given netlist with specified index.
     * @param netlist Netlist where Network lives.
     * @param netIndex index of Network.
     */
    public Network(Netlist netlist, int netIndex) {
        this.netlist = netlist;
        this.netIndex = netIndex;
    }
    
    /**
     * Add user name to list of names of this Network.
     * @param nameKey name key to add.
     * @param exported true if name is exported.
     */
    public void addUserName(Name nameKey, boolean exported) {
        assert !nameKey.isTempname();
        String name = nameKey.toString();
        if (exported)
            assert exportedNamesCount == names.length;
        int i = 0;
        for (; i < names.length; i++) {
            String n = names[i];
            int cmp = TextUtils.STRING_NUMBER_ORDER.compare(name, n);
            if (cmp == 0 && !exported) return;
            if (cmp > 0 && (exported || i >= exportedNamesCount)) break;
        }
        if (names.length == 0) {
            names = new String[] { name };
        } else {
            String[] newNames = new String[names.length + 1];
            System.arraycopy(names, 0, newNames, 0, i);
            newNames[i] = name;
            System.arraycopy(names, i, newNames, i + 1, names.length - i);
            names = newNames;
        }
        if (exported)
            exportedNamesCount++;
        isUsernamed = true;
    }
    
    /**
     * Add temporary name to list of names of this Network.
     * @param name name to add.
     */
    public void addTempName(String name) {
        if (isUsernamed || names.length > 0) return;
        names = new String[] { name };
    }
    
    // --------------------------- public methods ------------------------------
    
    /** Returns the Netlist of this Network.
     * @return Netlist of this Network.
     */
    public Netlist getNetlist() {
        return netlist;
    }
    
    /** Returns parent cell of this Network.
     * @return parent cell of this Network.
     */
    public Cell getParent() {
        return netlist.netCell.cell;
    }
    
    /** Returns index of this Network in netlist. */
    public int getNetIndex() { return netIndex; }
    
    /** A net can have multiple names. Return alphabetized list of names. */
    public Iterator getNames() {
        return ArrayIterator.iterator(names);
    }
    
    /** A net can have multiple names. Return alphabetized list of names. */
    public Iterator getExportedNames() {
        return ArrayIterator.iterator(names, 0, exportedNamesCount);
    }
    
    /**
     * Returns most appropriate name of the net.
     * Intitialized net has at least one name - user-defiend or temporary.
     */
    public String getName() {
        return names[0];
    }
    
    /** Returns true if Network has names */
    boolean hasNames() {
        return names.length > 0;
    }
    
    /** Returns true if nm is one of Network's names */
    public boolean hasName(String nm) {
        for (int i = 0; i < names.length; i++)
            if (names[i].equals(nm)) return true;
        return false;
    }
    
    /**
     * Add names of this net to two Collections. One for exported, and other for unexported names.
     * @exportedNames Collection for exported names.
     * @unexportedNames Collection for unexported names.
     */
    public void fillNames(Collection exportedNames, Collection unexportedNames) {
        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            (i < exportedNamesCount ? exportedNames : unexportedNames).add(name);
        }
    }
    
    /** Get iterator over all PortInsts on Network.  Note that the
     * PortFilter class is useful for filtering out frequently excluded
     * PortInsts.  */
    public Iterator getPorts() {
        ArrayList ports = new ArrayList();
        for (Iterator it = getParent().getNodes(); it.hasNext(); ) {
            NodeInst ni = (NodeInst)it.next();
            for (Iterator pit = ni.getPortInsts(); pit.hasNext(); ) {
                PortInst pi = (PortInst)pit.next();
                if (netlist.getNetwork(pi) == this)
                    ports.add(pi);
            }
        }
        return ports.iterator();
    }
    
    /** Get iterator over all Exports on Network */
    public Iterator getExports() {
        ArrayList exports = new ArrayList();
        for (Iterator it = getParent().getPorts(); it.hasNext();) {
            Export e = (Export) it.next();
            int busWidth = netlist.getBusWidth(e);
            for (int i = 0; i < busWidth; i++) {
                if (netlist.getNetwork(e, i) == this)
                    exports.add(e);
            }
        }
        return exports.iterator();
    }
    
    /** Get iterator over all ArcInsts on Network */
    public Iterator getArcs() {
        ArrayList arcs = new ArrayList();
        for (Iterator it = getParent().getArcs(); it.hasNext();) {
            ArcInst ai = (ArcInst) it.next();
            if (netlist.getNetwork(ai, 0) == this) {
                arcs.add(ai);
            }
        }
        return arcs.iterator();
    }
    
    /**
     * Method to tell whether this network has any exports on it.
     * @return true if there are exports on this Network.
     */
    public boolean isExported() { return exportedNamesCount > 0; }
    
    /**
     * Method to tell whether this network has user-defined name.
     * @return true if this Network has user-defined name.
     */
    public boolean isUsernamed() { return isUsernamed; }
    
    /**
     * Method to describe this Network as a string.
     * @param withQuotes to wrap description between quotes
     * @return a String describing this Network.
     */
    public String describe(boolean withQuotes) {
        Iterator it = getNames();
        String name = (String)it.next();
        while (it.hasNext())
            name += "/" + (String)it.next();
        if (withQuotes) name = "'"+name+"'";
        return name;
    }
    
    /**
     * Returns a printable version of this Network.
     * @return a printable version of this Network.
     */
    public String toString() {
        return "network "+describe(true);
    }
}
