/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Resistor.java
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
//	Updated 4 October 2003

package com.sun.electric.tool.ncc.jemNets;
import com.sun.electric.tool.ncc.basicA.Name;
import com.sun.electric.tool.ncc.basicA.Messenger;
import com.sun.electric.tool.ncc.jemNets.Part;
import com.sun.electric.tool.ncc.jemNets.Wire;

public class Resistor extends Part{
    // ---------- private data -------------
    private static int numCon= 2;
    private static int termCoefs[] = {17,17}; //resistors are symmetric
    private float myValue= 0; //resistance

    // ---------- private methods ----------
    private Resistor(Name n){super(n, numCon);}

    private void flip(){
        Wire w = pins[0];
        pins[0] = pins[1];
        pins[1] = w;
    }

    // ---------- public methods ----------

    public static Resistor please() {return new Resistor(null);}

    public static Resistor please(Name n){return new Resistor(n);}


    // ---------- abstract commitment ----------

	public boolean isThisGate(int x){return false;}
    public int getNumCon(){return numCon;}
    public int[] getTermCoefs(){return termCoefs;} //the terminal coeficients
	public String valueString(){
		String sz= "R= " + myValue;
		return sz;
	} // end of valueString
	
    // ---------- public methods ----------

    public float getValue(){return myValue;}
    public void setValue(float v){myValue= v;}

	/** A method to test if this Part touches a Wire with a gate connection.
		* @param the Wire to test
		* @return false because Resistors don't have gates.
		*/
    public boolean touchesAtGate(Wire w){return false;}
	
    public void connect(Wire ss, Wire ee){
        pins[0] = ss;
        pins[1] = ee;
		ss.add(this);
		ee.add(this);
    }
	
    //merge with this resistor
    public boolean parallelMerge(Part p){
        if(p.getClass() != getClass()) return false;
        if(this == p)return false;
        //its the same class but a different one
        Resistor r= (Resistor)p;
        if(pins[0]!=r.pins[0])  r.flip();
		if(pins[0]!=r.pins[0] || pins[1]!=r.pins[1])  return false;

        //OK to merge
        float ff= 0;
        float pp= r.getValue();
        float mm= getValue();
        if(pp != 0 && mm != 0)ff= (ff * mm)/(ff + mm);
        myValue= ff;
        r.deleteMe();
        return true; //return true if merged
    }


    // ---------- printing methods ----------

    public String nameString(){
		return ("Resistor " + getStringName());
    }

    public String connectionString(int n){
		String s = pins[0].getStringName();
		String e = pins[1].getStringName();
		return ("S= " + s + " E= " + e);
    }

}

