/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Layer.java
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
package com.sun.electric.technology;

import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.prototype.ArcProto.Function;
import com.sun.electric.database.text.Pref;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;

/**
 * The Layer class defines a single layer of material, out of which NodeInst and ArcInst objects are created.
 * The Layers are defined by the PrimitiveNode and PrimitiveArc classes, and are used in the generation of geometry.
 * In addition, layers have extra information that is used for output and behavior.
 */
public class Layer
{
	/**
	 * Function is a typesafe enum class that describes the function of a layer.
	 * Functions are technology-independent and describe the nature of the layer (Metal, Polysilicon, etc.)
	 */
	public static class Function
	{
		/** Describes a P-type layer. */												public static final int PTYPE =          0100;
		/** Describes a N-type layer. */												public static final int NTYPE =          0200;
		/** Describes a depletion layer. */												public static final int DEPLETION =      0400;
		/** Describes a enhancement layer. */											public static final int ENHANCEMENT =   01000;
		/** Describes a light doped layer. */											public static final int LIGHT =         02000;
		/** Describes a heavy doped layer. */											public static final int HEAVY =         04000;
		/** Describes a pseudo layer. */												public static final int PSEUDO =       010000;
		/** Describes a nonelectrical layer (does not carry signals). */				public static final int NONELEC =      020000;
		/** Describes a layer that contacts metal (used to identify contacts/vias). */	public static final int CONMETAL =     040000;
		/** Describes a layer that contacts polysilicon (used to identify contacts). */	public static final int CONPOLY =     0100000;
		/** Describes a layer that contacts diffusion (used to identify contacts). */	public static final int CONDIFF =     0200000;
		/** Describes a layer that is VTH or VTL */								        public static final int HLVT =      010000000;
		/** Describes a layer that is inside transistor. */								public static final int INTRANS =   020000000;
		/** Describes a thick layer. */								                    public static final int THICK =     040000000;

		private final String name;
		private final String constantName;
		private int level;
		private final int height;
		private final int extraBits;
		private static HashMap metalLayers = new HashMap();
		private static HashMap polyLayers = new HashMap();
		private static List allFunctions = new ArrayList();
		private static final int [] extras = {PTYPE, NTYPE, DEPLETION, ENHANCEMENT, LIGHT, HEAVY, PSEUDO, NONELEC, CONMETAL, CONPOLY, CONDIFF, INTRANS, THICK};

		private Function(String name, String constantName, int metalLevel, int polyLevel, int height, int extraBits)
		{
			this.name = name;
			this.constantName = constantName;
			this.height = height;
			this.extraBits = extraBits;
			if (metalLevel != 0) metalLayers.put(new Integer(this.level = metalLevel), this);
			if (polyLevel != 0) polyLayers.put(new Integer(this.level = polyLevel), this);
			allFunctions.add(this);
		}

		/**
		 * Returns a printable version of this Function.
		 * @return a printable version of this Function.
		 */
		public String toString()
		{
			String toStr = name;
			for(int i=0; i<extras.length; i++)
			{
				if ((extraBits & extras[i]) == 0) continue;
				toStr += "," + getExtraName(extras[i]);
			}
			return toStr;
		}

		/**
		 * Returns the name for this Function.
		 * @return the name for this Function.
		 */
		public String getName() { return name; }

		/**
		 * Returns the constant name for this Function.
		 * Constant names are used when writing Java code, so they must be the same as the actual symbol name.
		 * @return the constant name for this Function.
		 */
		public String getConstantName() { return constantName; }

		/**
		 * Method to return a list of all Layer Functions.
		 * @return a list of all Layer Functions.
		 */
		public static List getFunctions() { return allFunctions; }

		/**
		 * Method to return an array of the Layer Function "extra bits".
		 * @return an array of the Layer Function "extra bits".
		 * Each entry in the array is a single "extra bit", but they can be ORed together to combine them.
		 */
		public static int [] getFunctionExtras() { return extras; }

		/**
		 * Method to convert an "extra bits" value to a name.
		 * @param extra the extra bits value (must be a single bit, not an ORed combination).
		 * @return the name of that extra bit.
		 */
		public static String getExtraName(int extra)
		{
			if (extra == PTYPE) return "p-type";
			if (extra == NTYPE) return "n-type";
			if (extra == DEPLETION) return "depletion";
			if (extra == ENHANCEMENT) return "enhancement";
			if (extra == LIGHT) return "light";
			if (extra == HEAVY) return "heavy";
			if (extra == PSEUDO) return "pseudo";
			if (extra == NONELEC) return "nonelectrical";
			if (extra == CONMETAL) return "connects-metal";
			if (extra == CONPOLY) return "connects-poly";
			if (extra == CONDIFF) return "connects-diff";
			if (extra == INTRANS) return "inside-transistor";
			if (extra == THICK) return "thick";
            if (extra == HLVT) return "vt";
			return "";
		}

		/**
		 * Method to convert an "extra bits" value to a constant name.
		 * Constant names are used when writing Java code, so they must be the same as the actual symbol name.
		 * @param extra the extra bits value (must be a single bit, not an ORed combination).
		 * @return the name of that extra bit's constant.
		 */
		public static String getExtraConstantName(int extra)
		{
			if (extra == PTYPE) return "PTYPE";
			if (extra == NTYPE) return "NTYPE";
			if (extra == DEPLETION) return "DEPLETION";
			if (extra == ENHANCEMENT) return "ENHANCEMENT";
			if (extra == LIGHT) return "LIGHT";
			if (extra == HEAVY) return "HEAVY";
			if (extra == PSEUDO) return "PSEUDO";
			if (extra == NONELEC) return "NONELEC";
			if (extra == CONMETAL) return "CONMETAL";
			if (extra == CONPOLY) return "CONPOLY";
			if (extra == CONDIFF) return "CONDIFF";
			if (extra == INTRANS) return "INTRANS";
			if (extra == THICK) return "THICK";
			return "";
		}

		/**
		 * Method to convert an "extra bits" name to its numeric value.
		 * @param name the name of the bit.
		 * @return the numeric equivalent of that bit.
		 */
		public static int parseExtraName(String name)
		{
			if (name.equalsIgnoreCase("p-type")) return PTYPE;
			if (name.equalsIgnoreCase("n-type")) return NTYPE;
			if (name.equalsIgnoreCase("depletion")) return DEPLETION;
			if (name.equalsIgnoreCase("enhancement")) return ENHANCEMENT;
			if (name.equalsIgnoreCase("light")) return LIGHT;
			if (name.equalsIgnoreCase("heavy")) return HEAVY;
			if (name.equalsIgnoreCase("pseudo")) return PSEUDO;
			if (name.equalsIgnoreCase("nonelectrical")) return NONELEC;
			if (name.equalsIgnoreCase("connects-metal")) return CONMETAL;
			if (name.equalsIgnoreCase("connects-poly")) return CONPOLY;
			if (name.equalsIgnoreCase("connects-diff")) return CONDIFF;
			if (name.equalsIgnoreCase("inside-transistor")) return INTRANS;
			if (name.equalsIgnoreCase("thick")) return THICK;
            if (name.equalsIgnoreCase("vt")) return HLVT;
			return 0;
		}
		/** Describes an unknown layer. */						public static final Function UNKNOWN    = new Function("unknown",    "UNKNOWN",    0, 0, 35, 0);
		/** Describes a metal layer 1. */						public static final Function METAL1     = new Function("metal-1",    "METAL1",     1, 0, 17, 0);
		/** Describes a metal layer 2. */						public static final Function METAL2     = new Function("metal-2",    "METAL2",     2, 0, 19, 0);
		/** Describes a metal layer 3. */						public static final Function METAL3     = new Function("metal-3",    "METAL3",     3, 0, 21, 0);
		/** Describes a metal layer 4. */						public static final Function METAL4     = new Function("metal-4",    "METAL4",     4, 0, 23, 0);
		/** Describes a metal layer 5. */						public static final Function METAL5     = new Function("metal-5",    "METAL5",     5, 0, 25, 0);
		/** Describes a metal layer 6. */						public static final Function METAL6     = new Function("metal-6",    "METAL6",     6, 0, 27, 0);
		/** Describes a metal layer 7. */						public static final Function METAL7     = new Function("metal-7",    "METAL7",     7, 0, 29, 0);
		/** Describes a metal layer 8. */						public static final Function METAL8     = new Function("metal-8",    "METAL8",     8, 0, 31, 0);
		/** Describes a metal layer 9. */						public static final Function METAL9     = new Function("metal-9",    "METAL9",     9, 0, 33, 0);
		/** Describes a metal layer 10. */						public static final Function METAL10    = new Function("metal-10",   "METAL10",   10, 0, 35, 0);
		/** Describes a metal layer 11. */						public static final Function METAL11    = new Function("metal-11",   "METAL11",   11, 0, 37, 0);
		/** Describes a metal layer 12. */						public static final Function METAL12    = new Function("metal-12",   "METAL12",   12, 0, 39, 0);
		/** Describes a polysilicon layer 1. */					public static final Function POLY1      = new Function("poly-1",     "POLY1",      0, 1, 12, 0);
		/** Describes a polysilicon layer 2. */					public static final Function POLY2      = new Function("poly-2",     "POLY2",      0, 2, 13, 0);
		/** Describes a polysilicon layer 3. */					public static final Function POLY3      = new Function("poly-3",     "POLY3",      0, 3, 14, 0);
		/** Describes a polysilicon gate layer. */				public static final Function GATE       = new Function("gate",       "GATE",       0, 0, 15, INTRANS);
		/** Describes a diffusion layer. */						public static final Function DIFF       = new Function("diffusion",  "DIFF",       0, 0, 11, 0);
		/** Describes a P-diffusion layer. */					public static final Function DIFFP      = new Function("p-diffusion","DIFF",       0, 0, 11, PTYPE);
		/** Describes a N-diffusion layer. */					public static final Function DIFFN      = new Function("n-diffusion","DIFF",       0, 0, 11, NTYPE);
		/** Describes an implant layer. */						public static final Function IMPLANT    = new Function("implant",    "IMPLANT",    0, 0, 2, 0);
		/** Describes a P-implant layer. */						public static final Function IMPLANTP   = new Function("p-implant",  "IMPLANT",    0, 0, 2, PTYPE);
		/** Describes an N-implant layer. */					public static final Function IMPLANTN   = new Function("n-implant",  "IMPLANT",    0, 0, 2, NTYPE);
		/** Describes a contact layer 1. */						public static final Function CONTACT1   = new Function("contact-1",  "CONTACT1",   0, 0, 16, 0);
		/** Describes a contact layer 2. */						public static final Function CONTACT2   = new Function("contact-2",  "CONTACT2",   0, 0, 18, 0);
		/** Describes a contact layer 3. */						public static final Function CONTACT3   = new Function("contact-3",  "CONTACT3",   0, 0, 20, 0);
		/** Describes a contact layer 4. */						public static final Function CONTACT4   = new Function("contact-4",  "CONTACT4",   0, 0, 22, 0);
		/** Describes a contact layer 5. */						public static final Function CONTACT5   = new Function("contact-5",  "CONTACT5",   0, 0, 24, 0);
		/** Describes a contact layer 6. */						public static final Function CONTACT6   = new Function("contact-6",  "CONTACT6",   0, 0, 26, 0);
		/** Describes a contact layer 7. */						public static final Function CONTACT7   = new Function("contact-7",  "CONTACT7",   0, 0, 28, 0);
		/** Describes a contact layer 8. */						public static final Function CONTACT8   = new Function("contact-8",  "CONTACT8",   0, 0, 30, 0);
		/** Describes a contact layer 9. */						public static final Function CONTACT9   = new Function("contact-9",  "CONTACT9",   0, 0, 32, 0);
		/** Describes a contact layer 10. */					public static final Function CONTACT10  = new Function("contact-10", "CONTACT10",  0, 0, 34, 0);
		/** Describes a contact layer 11. */					public static final Function CONTACT11  = new Function("contact-11", "CONTACT11",  0, 0, 36, 0);
		/** Describes a contact layer 12. */					public static final Function CONTACT12  = new Function("contact-12", "CONTACT12",  0, 0, 38, 0);
		/** Describes a sinker (diffusion-to-buried plug). */	public static final Function PLUG       = new Function("plug",       "PLUG",       0, 0, 40, 0);
		/** Describes an overglass layer (passivation). */		public static final Function OVERGLASS  = new Function("overglass",  "OVERGLASS",  0, 0, 41, 0);
		/** Describes a resistor layer. */						public static final Function RESISTOR   = new Function("resistor",   "RESISTOR",   0, 0, 4, 0);
		/** Describes a capacitor layer. */						public static final Function CAP        = new Function("capacitor",  "CAP",        0, 0, 5, 0);
		/** Describes a transistor layer. */					public static final Function TRANSISTOR = new Function("transistor", "TRANSISTOR", 0, 0, 3, 0);
		/** Describes an emitter of bipolar transistor. */		public static final Function EMITTER    = new Function("emitter",    "EMITTER",    0, 0, 6, 0);
		/** Describes a base of bipolar transistor. */			public static final Function BASE       = new Function("base",       "BASE",       0, 0, 7, 0);
		/** Describes a collector of bipolar transistor. */		public static final Function COLLECTOR  = new Function("collector",  "COLLECTOR",  0, 0, 8, 0);
		/** Describes a substrate layer. */						public static final Function SUBSTRATE  = new Function("substrate",  "SUBSTRATE",  0, 0, 1, 0);
		/** Describes a well layer. */							public static final Function WELL       = new Function("well",       "WELL",       0, 0, 0, 0);
		/** Describes a P-well layer. */						public static final Function WELLP      = new Function("p-well",     "WELL",       0, 0, 0, PTYPE);
		/** Describes a N-well layer. */						public static final Function WELLN      = new Function("n-well",     "WELL",       0, 0, 0, NTYPE);
		/** Describes a guard layer. */							public static final Function GUARD      = new Function("guard",      "GUARD",      0, 0, 9, 0);
		/** Describes an isolation layer (bipolar). */			public static final Function ISOLATION  = new Function("isolation",  "ISOLATION",  0, 0, 10, 0);
		/** Describes a bus layer. */							public static final Function BUS        = new Function("bus",        "BUS",        0, 0, 42, 0);
		/** Describes an artwork layer. */						public static final Function ART        = new Function("art",        "ART",        0, 0, 43, 0);
		/** Describes a control layer. */						public static final Function CONTROL    = new Function("control",    "CONTROL",    0, 0, 44, 0);

		/**
		 * Method to get the level of this Layer.
		 * The level applies to metal and polysilicon functions, and gives the layer number
		 * (i.e. Metal-2 is level 2).
		 * @return the level of this Layer.
		 */
		public int getLevel() { return level; }

		/**
		 * Method to find the Function that corresponds to Metal on a given layer.
		 * @param level the layer (starting at 1 for Metal-1).
		 * @return the Function that represents that Metal layer.
		 */
		public static Function getMetal(int level)
		{
			Function func = (Function)metalLayers.get(new Integer(level));
			return func;
		}

		/**
		 * Method to find the Function that corresponds to Polysilicon on a given layer.
		 * @param level the layer (starting at 1 for Polysilicon-1).
		 * @return the Function that represents that Polysilicon layer.
		 */
		public static Function getPoly(int level)
		{
			Function func = (Function)polyLayers.get(new Integer(level));
			return func;
		}

		/**
		 * Method to tell whether this layer function is metal.
		 * @return true if this layer function is metal.
		 */
		public boolean isMetal()
		{
			if (this == METAL1  || this == METAL2  || this == METAL3 ||
				this == METAL4  || this == METAL5  || this == METAL6 ||
				this == METAL7  || this == METAL8  || this == METAL9 ||
				this == METAL10 || this == METAL11 || this == METAL12) return true;
			return false;
		}

		/**
		 * Method to tell whether this layer function is diffusion (active).
		 * @return true if this layer function is diffusion (active).
		 */
		public boolean isDiff()
		{
			if (this == DIFF || this == DIFFP || this == DIFFN) return true;
			return false;
		}

		/**
		 * Method to tell whether this layer function is polysilicon.
		 * @return true if this layer function is polysilicon.
		 */
		public boolean isPoly()
		{
			if (this == POLY1 || this == POLY2 || this == POLY3 || this == GATE) return true;
			return false;
		}

		/**
		 * Method to tell whether this layer function is polysilicon in the gate of a transistor.
		 * @return true if this layer function is the gate of a transistor.
		 */
		public boolean isGatePoly()
		{
			if (isPoly() && (extraBits&INTRANS) != 0) return true;
			return false;
		}

		/**
		 * Method to tell whether this layer function is a contact.
		 * @return true if this layer function is contact.
		 */
		public boolean isContact()
		{
			if (this == CONTACT1 || this == CONTACT2 || this == CONTACT3 ||
				this == CONTACT4 || this == CONTACT5 || this == CONTACT6 ||
				this == CONTACT7 || this == CONTACT8 || this == CONTACT9 ||
				this == CONTACT10 || this == CONTACT11 || this == CONTACT12)  return true;
			return false;
		}

		/**
		 * Method to tell whether this layer function is substrate.
		 * @return true if this layer function is substrate.
		 */
		public boolean isSubstrate()
		{
			if (this == SUBSTRATE ||
				this == WELL || this == WELLP || this == WELLN ||
				this == IMPLANT || this == IMPLANTN || this == IMPLANTP)  return true;
			return false;
		}

		/**
		 * Method to tell whether this layer function is implant.
		 * @return true if this layer function is implant.
		 */
		public boolean isImplant()
		{
			return (this == IMPLANT || this == IMPLANTN || this == IMPLANTP);
		}

		/**
		 * Method to tell the distance of this layer function.
		 * @return the distance of this layer function.
		 */
		public int getHeight() { return height; }
	}


    public static final LayerSort layerSort = new LayerSort(); // No need to create a new one every time
	public static class LayerSort implements Comparator
	{
        public static int compareStatic(Object o1, Object o2)
        {
			String s1 = ((Layer)o1).getName();
			String s2 = ((Layer)o2).getName();;
			return s1.compareToIgnoreCase(s2);

        }

		public int compare(Object o1, Object o2)
		{
			return compareStatic(o1, o2);
		}
	}

	private String name;
	private int index;
	private Technology tech;
	private EGraphics graphics;
	private Function function;
	private int functionExtras;
	private String cifLayer;
	private String dxfLayer;
	private String gdsLayer;
	private String skillLayer;
	private double thickness, distance, areaCoverage;
	private double resistance, capacitance, edgeCapacitance;
	/** the "real" layer (if this one is pseudo) */							private Layer nonPseudoLayer;
	/** true if this layer is invisible */									private boolean visible;
	/** true if dimmed (drawn darker) undimmed layers are highlighted */	private boolean dimmed;
	/** the pure-layer node that contains just this layer */				private PrimitiveNode pureLayerNode;

	private static HashMap cifLayerPrefs = new HashMap();
	private static HashMap gdsLayerPrefs = new HashMap();
	private static HashMap dxfLayerPrefs = new HashMap();
	private static HashMap skillLayerPrefs = new HashMap();
	private static HashMap resistanceParasiticPrefs = new HashMap();
	private static HashMap capacitanceParasiticPrefs = new HashMap();
	private static HashMap edgeCapacitanceParasiticPrefs = new HashMap();
    private static final HashMap layerVisibilityPrefs = new HashMap();

    // 3D options
	private static final HashMap layer3DThicknessPrefs = new HashMap();
	private static final HashMap layer3DDistancePrefs = new HashMap();

    private static final HashMap areaCoveragePrefs = new HashMap();  // Used by area coverage tool

	private Layer(String name, Technology tech, EGraphics graphics)
	{
		this.name = name;
		this.tech = tech;
		this.graphics = graphics;
		this.nonPseudoLayer = this;
		this.visible = true;
		this.dimmed = false;
		this.function = Function.UNKNOWN;
        this.areaCoverage = 10; // 10% as default
	}

	/**
	 * Method to create a new layer with the given name and graphics.
	 * @param tech the Technology that this layer belongs to.
	 * @param name the name of the layer.
	 * @param graphics the appearance of the layer.
	 * @return the Layer object.
	 */
	public static Layer newInstance(Technology tech, String name, EGraphics graphics)
	{
		Layer layer = new Layer(name, tech, graphics);
		graphics.setLayer(layer);
		if (tech != null) tech.addLayer(layer);
		return layer;
	}

	/**
	 * Method to return the name of this Layer.
	 * @return the name of this Layer.
	 */
	public String getName() { return name; }

	/**
	 * Method to return the index of this Layer.
	 * The index is 0-based.
	 * @return the index of this Layer.
	 */
	public int getIndex() { return index; }

	/**
	 * Method to set the index of this Layer.
	 * The index is 0-based.
	 * @param index the index of this Layer.
	 */
	public void setIndex(int index) { this.index = index; }

	/**
	 * Method to return the Technology of this Layer.
	 * @return the Technology of this Layer.
	 */
	public Technology getTechnology() { return tech; }

	/**
	 * Method to return the graphics description of this Layer.
	 * @return the graphics description of this Layer.
	 */
	public EGraphics getGraphics() { return graphics; }

	/**
	 * Method to set the Function of this Layer.
	 * @param function the Function of this Layer.
	 */
	public void setFunction(Function function)
	{
		this.function = function;
		this.functionExtras = 0;
	}

	/**
	 * Method to set the Function of this Layer when the function is complex.
	 * Some layer functions have extra bits of information to describe them.
	 * For example, P-Type Diffusion has the Function DIFF but the extra bits PTYPE.
	 * @param function the Function of this Layer.
	 * @param functionExtras extra bits to describe the Function of this Layer.
	 */
	public void setFunction(Function function, int functionExtras)
	{
		this.function = function;
		this.functionExtras = functionExtras;
	}

	/**
	 * Method to return the Function of this Layer.
	 * @return the Function of this Layer.
	 */
	public Function getFunction() { return function; }

	/**
	 * Method to return the Function "extras" of this Layer.
	 * The "extras" are a set of modifier bits, such as "p-type".
	 * @return the Function extras of this Layer.
	 */
	public int getFunctionExtras() { return functionExtras; }

	/**
	 * Method to set the Pure Layer Node associated with this Layer.
	 * @param pln the Pure Layer PrimitiveNode to use for this Layer.
	 */
	public void setPureLayerNode(PrimitiveNode pln) { pureLayerNode = pln; }

	/**
	 * Method to return the Pure Layer Node associated with this Layer.
	 * @return the Pure Layer Node associated with this Layer.
	 */
	public PrimitiveNode getPureLayerNode() { return pureLayerNode; }

	/**
	 * Method to tell whether this layer function is non-electrical.
	 * Non-electrical layers do not carry any signal (for example, artwork, text).
	 * @return true if this layer function is non-electrical.
	 */
	public boolean isNonElectrical()
	{
		return (functionExtras&Function.NONELEC) != 0;
	}

    /**
     * Method to determine if the layer function corresponds to a diffusion layer.
     * Used in parasitic calculation
     * @return
     */
    public boolean isDiffusionLayer()
    {
        int extras = getFunctionExtras();
		if ((extras&Layer.Function.PSEUDO) == 0)
		{
			if (getFunction().isDiff()) return true;
		}
		return false;
    }

	/**
	 * Method to return the non-pseudo layer associated with this pseudo-Layer.
	 * Pseudo layers are those used in pins, and have no real geometry.
	 * @return the non-pseudo layer associated with this pseudo-Layer.
	 * If this layer is already not pseudo, this layer is returned.
	 */
	public Layer getNonPseudoLayer() { return nonPseudoLayer; }

	/**
	 * Method to set the non-pseudo layer associated with this pseudo-Layer.
	 * Pseudo layers are those used in pins, and have no real geometry.
	 * @param nonPseudoLayer the non-pseudo layer associated with this pseudo-Layer.
	 */
	public void setNonPseudoLayer(Layer nonPseudoLayer) { this.nonPseudoLayer = nonPseudoLayer; }

	/**
	 * Method to tell whether this Layer is visible.
	 * @return true if this Layer is visible.
	 */
    public boolean isVisible()
    {
    	if (tech == null) return true;
    	return getBooleanPref("Visibility", layerVisibilityPrefs, visible).getBoolean();
    }

	/**
	 * Method to set whether this Layer is visible.
	 * @param visible true if this Layer is to be visible.
	 */
    public void setVisible(boolean visible) { getBooleanPref("Visibility", layerVisibilityPrefs, visible).setBoolean(visible); }

	/**
	 * Method to tell whether this Layer is dimmed.
	 * Dimmed layers are drawn darker so that undimmed layers can be highlighted.
	 * @return true if this Layer is dimmed.
	 */
	public boolean isDimmed() { return dimmed; }

	/**
	 * Method to set whether this Layer is dimmed.
	 * Dimmed layers are drawn darker so that undimmed layers can be highlighted.
	 * @param dimmed true if this Layer is to be dimmed.
	 */
	public void setDimmed(boolean dimmed) { this.dimmed = dimmed; }

	private Pref getLayerPref(String what, HashMap map, String factory)
	{
		Pref pref = (Pref)map.get(this);
		if (pref == null)
		{
			if (factory == null) factory = "";
			pref = Pref.makeStringPref(what + "LayerFor" + name + "IN" + tech.getTechName(), Technology.getTechnologyPreferences(), factory);
			pref.attachToObject(tech, "IO/" + what + " tab", what + " for layer " + name + " in technology " + tech.getTechName());
			map.put(this, pref);
		}
		return pref;
	}

	private Pref getParasiticPref(String what, HashMap map, double factory)
	{
		Pref pref = (Pref)map.get(this);
		if (pref == null)
		{
			pref = Pref.makeDoublePref(what + "ParasiticFor" + name + "IN" + tech.getTechName(), Technology.getTechnologyPreferences(), factory);
			pref.attachToObject(tech, "Tools/Spice tab", "Technology " + tech.getTechName() + ", " + what + " for layer " + name);
			map.put(this, pref);
		}
		return pref;
	}

    public Pref getBooleanPref(String what, HashMap map, boolean factory)
	{
		Pref pref = (Pref)map.get(this);
		if (pref == null)
		{
			pref = Pref.makeBooleanPref(what + "Of" + name + "IN" + tech.getTechName(), Technology.getTechnologyPreferences(), factory);
			map.put(this, pref);
		}
		return pref;
	}

	public Pref getDoublePref(String what, HashMap map, double factory)
	{
		Pref pref = (Pref)map.get(this);
		if (pref == null)
		{
			pref = Pref.makeDoublePref(what + "Of" + name + "IN" + tech.getTechName(), Technology.getTechnologyPreferences(), factory);
			map.put(this, pref);
		}
		return pref;
	}

	public Pref getIntegerPref(String what, HashMap map, int factory)
	{
		Pref pref = (Pref)map.get(this);
		if (pref == null)
		{
			pref = Pref.makeIntPref(what + "Of" + name + "IN" + tech.getTechName(), Technology.getTechnologyPreferences(), factory);
			map.put(this, pref);
		}
		return pref;
	}

	/**
	 * Method to set the 3D distance and thickness of this Layer.
	 * @param thickness the thickness of this layer.
	 * @param distance the distance of this layer above the ground plane (silicon).
	 * Negative values represent layes in silicon like p++, p well, etc.
	 * The higher the distance value, the farther from the silicon.
	 */
	public void setFactory3DInfo(double thickness, double distance)
	{
		this.thickness = thickness;
		this.distance = distance;
		getDoublePref("Distance", layer3DDistancePrefs, this.distance).setDouble(this.distance);
		getDoublePref("Thickness", layer3DThicknessPrefs, this.thickness).setDouble(this.thickness);
	}

	/**
	 * Method to return the distance of this layer.
	 * The higher the distance value, the farther from the wafer.
	 * @return the distance of this layer above the ground plane.
	 */
	public double getDistance() { return getDoublePref("Distance", layer3DDistancePrefs, distance).getDouble(); }

	/**
	 * Method to set the distance of this layer.
	 * The higher the distance value, the farther from the wafer.
	 * @param distance the distance of this layer above the ground plane.
	 */
	public void setDistance(double distance) { getDoublePref("Distance", layer3DDistancePrefs, this.distance).setDouble(distance); }

	/**
	 * Method to calculate Z value of the upper part of the layer.
	 * Note: not called getHeight to avoid confusion
	 * with getDistance())
	 * @return Height of the layer
	 */
	public double getDepth() { return (distance+thickness); }

	/**
	 * Method to return the thickness of this layer.
	 * Layers can have a thickness of 0, which causes them to be rendered flat.
	 * @return the thickness of this layer.
	 */
	public double getThickness() { return getDoublePref("Thickness", layer3DThicknessPrefs, thickness).getDouble(); }

	/**
	 * Method to set the thickness of this layer.
	 * Layers can have a thickness of 0, which causes them to be rendered flat.
	 * @param thickness the thickness of this layer.
	 */
	public void setThickness(double thickness) { getDoublePref("Thickness", layer3DThicknessPrefs, thickness).setDouble(thickness); }

	/**
	 * Method to set the factory-default CIF name of this Layer.
	 * @param cifLayer the factory-default CIF name of this Layer.
	 */
	public void setFactoryCIFLayer(String cifLayer) { getLayerPref("CIF", cifLayerPrefs, cifLayer); }

	/**
	 * Method to set the CIF name of this Layer.
	 * @param cifLayer the CIF name of this Layer.
	 */
	public void setCIFLayer(String cifLayer) { getLayerPref("CIF", cifLayerPrefs, this.cifLayer).setString(cifLayer); }

	/**
	 * Method to return the CIF name of this layer.
	 * @return the CIF name of this layer.
	 */
	public String getCIFLayer() { return getLayerPref("CIF", cifLayerPrefs, cifLayer).getString(); }

	/**
	 * Method to set the factory-default GDS name of this Layer.
	 * @param gdsLayer the factory-default GDS name of this Layer.
	 */
	public void setFactoryGDSLayer(String gdsLayer) { getLayerPref("GDS", gdsLayerPrefs, gdsLayer); }

	/**
	 * Method to set the GDS name of this Layer.
	 * @param gdsLayer the GDS name of this Layer.
	 */
	public void setGDSLayer(String gdsLayer) { getLayerPref("GDS", gdsLayerPrefs, this.gdsLayer).setString(gdsLayer); }

	/**
	 * Method to return the GDS name of this layer.
	 * @return the GDS name of this layer.
	 */
	public String getGDSLayer() { return getLayerPref("GDS", gdsLayerPrefs, gdsLayer).getString(); }

	/**
	 * Method to set the factory-default DXF name of this Layer.
	 * @param dxfLayer the factory-default DXF name of this Layer.
	 */
	public void setFactoryDXFLayer(String dxfLayer) { getLayerPref("DXF", dxfLayerPrefs, dxfLayer); }

	/**
	 * Method to set the DXF name of this Layer.
	 * @param dxfLayer the DXF name of this Layer.
	 */
	public void setDXFLayer(String dxfLayer) { getLayerPref("DXF", dxfLayerPrefs, this.dxfLayer).setString(dxfLayer); }

	/**
	 * Method to return the DXF name of this layer.
	 * @return the DXF name of this layer.
	 */
	public String getDXFLayer() { return getLayerPref("DXF", dxfLayerPrefs, dxfLayer).getString(); }

	/**
	 * Method to set the factory-default Skill name of this Layer.
	 * @param skillLayer the factory-default Skill name of this Layer.
	 */
	public void setFactorySkillLayer(String skillLayer) { getLayerPref("Skill", skillLayerPrefs, skillLayer); }

	/**
	 * Method to set the Skill name of this Layer.
	 * @param skillLayer the Skill name of this Layer.
	 */
	public void setSkillLayer(String skillLayer) { getLayerPref("Skill", skillLayerPrefs, this.skillLayer).setString(skillLayer); }

	/**
	 * Method to return the Skill name of this layer.
	 * @return the Skill name of this layer.
	 */
	public String getSkillLayer() { return getLayerPref("Skill", skillLayerPrefs, skillLayer).getString(); }

	/**
	 * Method to set the Spice parasitics for this Layer.
	 * This is typically called only during initialization.
	 * It does not set the "option" storage, as "setResistance()",
	 * "setCapacitance()", and ""setEdgeCapacitance()" do.
	 * @param resistance the resistance of this Layer.
	 * @param capacitance the capacitance of this Layer.
	 * @param edgeCapacitance the edge capacitance of this Layer.
	 */
	public void setFactoryParasitics(double resistance, double capacitance, double edgeCapacitance)
	{
		getParasiticPref("Resistance", resistanceParasiticPrefs, this.resistance = resistance);
		getParasiticPref("Capacitance", capacitanceParasiticPrefs, this.capacitance = capacitance);
		getParasiticPref("EdgeCapacitance", edgeCapacitanceParasiticPrefs, this.edgeCapacitance = edgeCapacitance);
	}

	/**
	 * Method to return the resistance for this layer.
	 * @return the resistance for this layer.
	 */
	public double getResistance() { return getParasiticPref("Resistance", resistanceParasiticPrefs, resistance).getDouble(); }

	/**
	 * Method to set the resistance for this Layer.
	 * Also saves this information in the permanent options.
	 * @param resistance the new resistance for this Layer.
	 */
	public void setResistance(double resistance) { getParasiticPref("Resistance", resistanceParasiticPrefs, this.resistance).setDouble(resistance); }

	/**
	 * Method to return the capacitance for this layer.
	 * @return the capacitance for this layer.
	 */
	public double getCapacitance() { return getParasiticPref("Capacitance", capacitanceParasiticPrefs, capacitance).getDouble(); }

	/**
	 * Method to set the capacitance for this Layer.
	 * Also saves this information in the permanent options.
	 * @param capacitance the new capacitance for this Layer.
	 */
	public void setCapacitance(double capacitance) { getParasiticPref("Capacitance", capacitanceParasiticPrefs, this.capacitance).setDouble(capacitance); }

	/**
	 * Method to return the edge capacitance for this layer.
	 * @return the edge capacitance for this layer.
	 */
	public double getEdgeCapacitance() { return getParasiticPref("EdgeCapacitance", edgeCapacitanceParasiticPrefs, edgeCapacitance).getDouble(); }

    /**
     * Method to set the edge capacitance for this Layer.
     * Also saves this information in the permanent options.
     * @param edgeCapacitance the new edge capacitance for this Layer.
     */
    public void setEdgeCapacitance(double edgeCapacitance) { getParasiticPref("EdgeCapacitance", edgeCapacitanceParasiticPrefs, this.edgeCapacitance).setDouble(edgeCapacitance); }

    /**
	 * Method to set the minimum area to cover with this Layer in a particular cell.
	 * @param area the minimum area coverage of this layer.
	 */
	public void setFactoryAreaCoverage(double area)
	{
		this.areaCoverage = area;
		getDoublePref("AreaCoverage", areaCoveragePrefs, this.areaCoverage).setDouble(this.areaCoverage);
	}

    /**
	 * Method to return the minimu area coverage that the layer must reach in the technology.
	 * @return the minimum area coverage (in percentage).
	 */
	public double getAreaCoverage() { return getDoublePref("AreaCoverage", areaCoveragePrefs, areaCoverage).getDouble(); }

    /**
     * Methot to set minimu area coverage that the layer must reach in the technology.
     * @param area the minimum area coverage (in percentage).
     */
	public void setFactoryAreaCoverageInfo(double area) { getDoublePref("AreaCoverage", areaCoveragePrefs, areaCoverage).setDouble(area); }

	/**
	 * Returns a printable version of this Layer.
	 * @return a printable version of this Layer.
	 */
	public String toString()
	{
		return "Layer " + name;
	}
}
