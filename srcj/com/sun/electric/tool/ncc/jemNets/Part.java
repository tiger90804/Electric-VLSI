/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Part.java
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
package com.sun.electric.tool.ncc.jemNets;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.tool.ncc.jemNets.NccNameProxy.PartNameProxy;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.sun.electric.tool.ncc.trees.Circuit;

/** Part is an intermediate abstract sub-class of NetObject.
 * sub-classes of Part include Transistor, Resistor, (Capacitor), but
 * NOT Port. */
public abstract class Part extends NetObject {
	private static final Wire[] DELETED = null;

	protected static final int RESISTOR = 0;
	protected static final int TRANSISTOR = 1;
	protected static final int SUBCIRCUIT = 2;
	protected static final int TYPE_FIELD_WIDTH = 4;
	
    // ---------- private data -------------
	private PartNameProxy nameProxy;
    protected Wire[] pins;

    // ---------- private methods ----------

	/** 
	 * @param name the name of this Part
	 * @param pins terminals of this Part
	 */
    protected Part(PartNameProxy name, Wire[] pins){
    	nameProxy = name;
		this.pins = pins;
		for (int i=0; i<pins.length; i++)  pins[i].add(this);
    }

    // ---------- public methods ----------
    public String getName() {return nameProxy.toString();}
	public Iterator getConnected() {return Arrays.asList(pins).iterator();}
	public PartNameProxy getNameProxy() {return nameProxy;}
    public Type getNetObjType() {return Type.PART;}

	/** 
	 * Here is the accessor for the number of terminals on this Part
	 * @return the number of terminals on this Part, usually a small number.
	 */
	public int numPins() {return pins.length;}

	/** 
	 * Here is an accessor method for the coefficiant array for this
	 * Part.  The terminal coefficients are used to compute new hash
	 * codes.
	 * @return the array of terminal coefficients for this Part
	 */
	public abstract int[] getTermCoefs(); //the terminal coeficients

	/** 
	 * This method attempts to merge this Part in parallel with another Part
	 * @param p the other Part with which to merge
	 * @return true if merge was successful, false otherwise
	 */
	public abstract boolean parallelMerge(Part p);
	/**
	 * Compute a hash code for this part for the purpose of performing
	 * parallel merge. If two parallel Parts should be merged into one then
	 * hashCodeForParallelMerge() must return the same value for both
	 * Parts. 
	 */
	public abstract Integer hashCodeForParallelMerge();
	
    /** Mark this Part deleted and release all storage */
    public void setDeleted() {pins=DELETED;}
    public boolean isDeleted() {return pins==DELETED;}
    
    public int numDistinctWires() {
    	Set wires = new HashSet();
    	for (int i=0; i<pins.length; i++)  wires.add(pins[i]);
    	return wires.size();
    }
    
	/** returns String describing Part's type */
    public abstract String typeString();
    
	/** returns a unique int value for each distinct Part type */
    public abstract int typeCode();
    	
	/** How many pins of this Part are connected to Wire.
	 * @param w the Wire to test
	 * @return number of pins connected to Wire */
	public int numPinsConnected(Wire w) {
		int numConnected = 0;
		for (int i=0; i<pins.length; i++) {
		    if (pins[i]==w) numConnected++;
		}
        return numConnected;
    }

	/** 
	 * A method to test if this Part touches a Wire with a gate connection.
	 * Transistors that have gates must provide touchesAtGate.
	 * @param w the Wire to test
	 * @return true if a gate terminal of this Part touches the Wire
	 */
    public abstract boolean touchesAtGate(Wire w);

	/** 
	 * isThisGate returns true if the terminal number is a gate.
	 * @param x the terminal number to test
	 * @return true if the terminal is a gate, false otherwise
	 */
	protected abstract boolean isThisGate(int x);
	
	/** Return a set of all the different pin types for this part */
	public abstract Set getPinTypes();
	
	public Integer computeHashCode(){
        int sum= 0;
        int codes[]= getTermCoefs();
		for(int i=0; i<pins.length; i++) {
			Wire w = pins[i];
            sum += w.getCode() * codes[i];
        }
        return new Integer(sum);
    }

	/** 
	 * The Part must compute a hash code contribution for a Wire to
	 * use.  because the Wire doesn't know how it's connected to this
	 * Part and multiple connections are allowed.
	 * @param w the Wire for which a hash code is needed
	 * @return an int with the code contribution.
	 */
    public int getHashFor(Wire w){
        int sum= 0;
        int codes[]= getTermCoefs();
		for(int i=0; i<pins.length; i++){
			Wire x= pins[i];
			error(x==null, "null wire?");
			if(x==w) sum += codes[i] * getCode();
        }
        return sum;
    }
	
	/** 
	 * check that this Part is in proper form
	 * complain if it's wrong
	 * @return true if all OK, false if there's a problem
	 */
	public void checkMe(Circuit parent){
		error(parent!=getParent(), "wrong parent");
		for(int i=0; i<pins.length; i++){
		    Wire w= pins[i];
		    error(w==null, "Wire is null");
			error(!w.touches(this), "Wire not connected to Part");
		}
    }
	
	/** 
	 * Get the number of distinct Wires this part is connected to.
	 * For example, if all pins are connected to the same Wire then return 1.
	 * This method is only used for sanity checking by StratCount.
	 * @return the number of distinct Wires to which this Part is connected 
	 */
	public int getNumWiresConnected() {
		HashSet wires = new HashSet();
		for (int i=0; i<pins.length; i++)  wires.add(pins[i]);
		return wires.size();
	}
	
    /** @return a String containing the part type, the Cell containing the part, 
     * and the instance name */
    public String instanceDescription() {
    	// Don't print "Cell instance:" in root Cell where there is no path.
    	String inst = nameProxy.cellInstPath();
    	String instMsg = inst.equals("") ? "" : (" Cell instance: "+inst); 
    	return typeString()+" "+nameProxy.leafName()+" in Cell: "+
		       nameProxy.leafCell().libDescribe()+instMsg;
    }
    
	/** Report the numeric values of this Part,
	 * for example: width, length, resistance.
	 * @return a String describing the Part's numeric values.*/
	public abstract String valueDescription();

	/** comma separated list of pins connected to w */
	public abstract String connectionDescription(Wire w);

}

