/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Technology.java
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

import com.sun.electric.Main;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.CMOS;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.MoCMOS;
import com.sun.electric.technology.technologies.MoCMOSOld;
import com.sun.electric.technology.technologies.MoCMOSSub;
import com.sun.electric.technology.technologies.RCMOS;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.technology.technologies.nMOS;
import com.sun.electric.tool.user.ActivityLogger;
import com.sun.electric.tool.user.ui.EditWindow;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Technology is the base class for all of the specific technologies in Electric.
 *
 * It is organized into two main areas: nodes and arcs.
 * Both nodes and arcs are composed of Layers.
 *<P>
 * Subclasses of Technology usually start by defining the Layers (such as Metal-1, Metal-2, etc.)
 * Then the PrimitiveArc objects are created, built entirely from Layers.
 * Next PrimitiveNode objects are created, and they have Layers as well as connectivity to the PrimitiveArcs.
 * The Technology concludes with miscellaneous data assignments of technology-wide information.
 * <P>
 * Here are the nodes in a sample CMOS technology.
 * Note that there are two types of transistors and diffusion contacts, one for Well and one for Substrate.
 * Each layer that can exist as a wire must have a pin node (in this case, metal, polysilicon, and two flavors of diffusion.
 * Note that there are pure-layer nodes at the bottom which allow arbitrary geometry to be constructed.
 * <CENTER><IMG SRC="doc-files/Technology-1.gif"></CENTER>
 * <P>
 * The Schematic technology has some unusual features.
 * <CENTER><IMG SRC="doc-files/Technology-2.gif"></CENTER>
 * <P>
 * Conceptually, a Technology has 3 types of information:
 * <UL><LI><I>Geometry</I>.  Each node and arc can be described in terms of polygons on differnt Layers.
 * The ArcLayer and NodeLayer subclasses help define those polygons.
 * <LI><I>Connectivity</I>.  The very structure of the nodes and arcs establisheds a set of rules of connectivity.
 * Examples include the list of allowable arc types that may connect to each port, and the use of port "network numbers"
 * to identify those that are connected internally.
 * <LI><I>Behavior</I>.  Behavioral information takes many forms, but they can all find a place here.
 * For example, each layer, node, and arc has a "function" that describes its general behavior.
 * Some information applies to the technology as a whole, for example SPICE model cards.
 * Other examples include Design Rules and technology characteristics.
 * </UL>
 * @author Steven M. Rubin
 */
public class Technology extends ElectricObject
{

    /**
	 * Method to determine the index in the upper-left triangle array for two layers.
	 * @param layer1Index the first layer index.
	 * @param layer2Index the second layer index.
	 * @return the index in the array that corresponds to these two layers.
	 */
	public int getLayerIndex(int layer1Index, int layer2Index)
	{
		if (layer1Index > layer2Index) { int temp = layer1Index; layer1Index = layer2Index;  layer2Index = temp; }
		int pIndex = (layer1Index+1) * (layer1Index/2) + (layer1Index&1) * ((layer1Index+1)/2);
		pIndex = layer2Index + getNumLayers() * layer1Index - pIndex;
		return pIndex;
	}

    public static Layer getLayerFromOverride(String override, int startPos, char endChr, Technology tech)
    {
        int endPos = override.indexOf(endChr, startPos);
        if (endPos < 0) return null;
        String layerName = override.substring(startPos, endPos);
        Layer layer = tech.findLayer(layerName);
        return layer;
    }

    /**
	 * Defines a single layer of a PrimitiveArc.
	 * A PrimitiveArc has a list of these ArcLayer objects, one for
	 * each layer in a typical ArcInst.
	 * Each PrimitiveArc is composed of a number of ArcLayer descriptors.
	 * A descriptor converts a specific ArcInst into a polygon that describe this particular layer.
	 */
	public static class ArcLayer
	{
		private Layer layer;
		private double offset;
		private Poly.Type style;

		/**
		 * Constructs an <CODE>ArcLayer</CODE> with the specified description.
		 * @param layer the Layer of this ArcLayer.
		 * @param offset the distance from the outside of the ArcInst to this ArcLayer.
		 * @param style the Poly.Style of this ArcLayer.
		 */
		public ArcLayer(Layer layer, double offset, Poly.Type style)
		{
			this.layer = layer;
			this.offset = offset;
			this.style = style;
		}

		/**
		 * Returns the Layer from the Technology to be used for this ArcLayer.
		 * @return the Layer from the Technology to be used for this ArcLayer.
		 */
		public Layer getLayer() { return layer; }

		/**
		 * Returns the distance from the outside of the ArcInst to this ArcLayer.
		 * This is the difference between the width of this layer and the overall width of the arc.
		 * For example, a value of 4 on an arc that is 6 wide indicates that this layer should be only 2 wide.
		 * @return the distance from the outside of the ArcInst to this ArcLayer.
		 */
		public double getOffset() { return offset; }

		/**
		 * Sets the distance from the outside of the ArcInst to this ArcLayer.
		 * This is the difference between the width of this layer and the overall width of the arc.
		 * For example, a value of 4 on an arc that is 6 wide indicates that this layer should be only 2 wide.
		 * @param offset the distance from the outside of the ArcInst to this ArcLayer.
		 */
		public void setOffset(double offset) { this.offset = offset; }

		/**
		 * Returns the Poly.Style of this ArcLayer.
		 * @return the Poly.Style of this ArcLayer.
		 */
		public Poly.Type getStyle() { return style; }
	}

	/**
	 * Defines a point in space that is relative to a NodeInst's bounds.
	 * The TechPoint has two coordinates: X and Y.
	 * Each of these coordinates is represented by an Edge class (EdgeH for X
	 * and EdgeV for Y).
	 * The Edge classes have two numbers: a multiplier and an adder.
	 * The desired coordinate takes the NodeInst's center, adds in the
	 * product of the Edge multiplier and the NodeInst's size, and then adds
	 * in the Edge adder.
	 * <P>
	 * Arrays of TechPoint objects can be used to describe the bounds of
	 * a particular layer in a NodeInst.  Typically, four TechPoint objects
	 * can describe a rectangle.  Circles only need two (center and edge).
	 * The <CODE>Poly.Style</CODE> class defines the possible types of
	 * geometry.
	 * @see EdgeH
	 * @see EdgeV
	 */
	public static class TechPoint
	{
		private EdgeH x;
		private EdgeV y;

		/**
		 * Constructs a <CODE>TechPoint</CODE> with the specified description.
		 * @param x the EdgeH that converts a NodeInst into an X coordinate on that NodeInst.
		 * @param y the EdgeV that converts a NodeInst into a Y coordinate on that NodeInst.
		 */
		public TechPoint(EdgeH x, EdgeV y)
		{
			this.x = x;
			this.y = y;
		}

		/**
		 * Method to make a copy of this TechPoint, with all newly allocated parts.
		 * @return a new TechPoint with the values in this one.
		 */
		public TechPoint duplicate()
		{
			TechPoint newTP = new TechPoint(new EdgeH(x.getMultiplier(), x.getAdder()), new EdgeV(y.getMultiplier(), y.getAdder()));
			return newTP;
		}

		/**
		 * Method to make a 2-long TechPoint array that describes a point at the center of the node.
		 * @return a new TechPoint array that describes a point at the center of the node.
		 */
		public static TechPoint [] makeCenterBox()
		{
			return new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromCenter(0), EdgeV.fromCenter(0)),
					new Technology.TechPoint(EdgeH.fromCenter(0), EdgeV.fromCenter(0))};
		}

		/**
		 * Method to make a 2-long TechPoint array that describes a box that fills the node.
		 * @return a new TechPoint array that describes a box that fills the node.
		 */
		public static TechPoint [] makeFullBox()
		{
			return makeIndented(0);
		}

		/**
		 * Method to make a 2-long TechPoint array that describes indentation by a specified amount.
		 * @param amount the amount to indent the box.
		 * @return a new TechPoint array that describes this indented box.
		 */
		public static TechPoint [] makeIndented(double amount)
		{
			return new Technology.TechPoint [] {
					new Technology.TechPoint(EdgeH.fromLeft(amount), EdgeV.fromBottom(amount)),
					new Technology.TechPoint(EdgeH.fromRight(amount), EdgeV.fromTop(amount))};
		}

		/**
		 * Returns the EdgeH that converts a NodeInst into an X coordinate on that NodeInst.
		 * @return the EdgeH that converts a NodeInst into an X coordinate on that NodeInst.
		 */
		public EdgeH getX() { return x; }

		/**
		 * Returns the EdgeV that converts a NodeInst into a Y coordinate on that NodeInst.
		 * @return the EdgeV that converts a NodeInst into a Y coordinate on that NodeInst.
		 */
		public EdgeV getY() { return y; }
	}

	/**
	 * Defines a single layer of a PrimitiveNode.
	 * A PrimitiveNode has a list of these NodeLayer objects, one for
	 * each layer in a typical NodeInst.
	 * Each PrimitiveNode is composed of a number of NodeLayer descriptors.
	 * A descriptor converts a specific NodeInst into a polygon that describe this particular layer.
	 */
	public static class NodeLayer
	{
		private Layer layer;
		private int portNum;
		private Poly.Type style;
		private int representation;
		private TechPoint [] points;
		private String message;
		private TextDescriptor descriptor;
		private double lWidth, rWidth, extentT, extendB;

		// the meaning of "representation"
		/**
		 * Indicates that the "points" list defines scalable points.
		 * Each point here becomes a point on the Poly.
		 */
		public static final int POINTS = 0;

		/**
		 * Indicates that the "points" list defines a rectangle.
		 * It contains two diagonally opposite points.
		 */
		public static final int BOX = 1;

		/**
		 * Indicates that the "points" list defines a minimum sized rectangle.
		 * It contains two diagonally opposite points, like BOX,
		 * and also contains a minimum box size beyond which the polygon will not shrink
		 * (again, two diagonally opposite points).
		 */
		public static final int MINBOX = 2;

		/**
		 * Constructs a <CODE>NodeLayer</CODE> with the specified description.
		 * @param layer the <CODE>Layer</CODE> this is on.
		 * @param portNum a 0-based index of the port (from the actual NodeInst) on this layer.
		 * A negative value indicates that this layer is not connected to an electrical layer.
		 * @param style the Poly.Type this NodeLayer will generate (polygon, circle, text, etc.).
		 * @param representation tells how to interpret "points".  It can be POINTS, BOX, or MINBOX.
		 * @param points the list of coordinates (stored as TechPoints) associated with this NodeLayer.
		 */
		public NodeLayer(Layer layer, int portNum, Poly.Type style, int representation, TechPoint [] points)
		{
			this.layer = layer;
			this.portNum = portNum;
			this.style = style;
			this.representation = representation;
			this.points = points;
			this.lWidth = this.rWidth = this.extentT = this.extendB = 0;
		}

		/**
		 * Constructs a <CODE>NodeLayer</CODE> with the specified description.
		 * @param layer the <CODE>Layer</CODE> this is on.
		 * @param portNum a 0-based index of the port (from the actual NodeInst) on this layer.
		 * A negative value indicates that this layer is not connected to an electrical layer.
		 * @param style the Poly.Type this NodeLayer will generate (polygon, circle, text, etc.).
		 * @param representation tells how to interpret "points".  It can be POINTS, BOX, or MINBOX.
		 * @param points the list of coordinates (stored as TechPoints) associated with this NodeLayer.
		 * @param lWidth the extension to the left of this layer (serpentine transistors only).
		 * @param rWidth the extension to the right of this layer (serpentine transistors only).
		 * @param extentT the extension to the top of this layer (serpentine transistors only).
		 * @param extendB the extension to the bottom of this layer (serpentine transistors only).
		 */
		public NodeLayer(Layer layer, int portNum, Poly.Type style, int representation, TechPoint [] points,
			double lWidth, double rWidth, double extentT, double extendB)
		{
			this.layer = layer;
			this.portNum = portNum;
			this.style = style;
			this.representation = representation;
			this.points = points;
			this.lWidth = lWidth;
			this.rWidth = rWidth;
			this.extentT = extentT;
			this.extendB = extendB;
		}

		/**
		 * Returns the <CODE>Layer</CODE> object associated with this NodeLayer.
		 * @return the <CODE>Layer</CODE> object associated with this NodeLayer.
		 */
		public Layer getLayer() { return layer; }

		/**
		 * Returns the 0-based index of the port associated with this NodeLayer.
		 * @return the 0-based index of the port associated with this NodeLayer.
		 */
		public int getPortNum() { return portNum; }

		/**
		 * Returns the Poly.Type this NodeLayer will generate.
		 * @return the Poly.Type this NodeLayer will generate.
		 * Examples are polygon, lines, splines, circle, text, etc.
		 */
		public Poly.Type getStyle() { return style; }

		/**
		 * Returns the method of interpreting "points".
		 * @return the method of interpreting "points".
		 * It can be POINTS, BOX, ABSPOINTS, or MINBOX.
		 */
		public int getRepresentation() { return representation; }

		/**
		 * Returns the list of coordinates (stored as TechPoints) associated with this NodeLayer.
		 * @return the list of coordinates (stored as TechPoints) associated with this NodeLayer.
		 */
		public TechPoint [] getPoints() { return points; }

		/**
		 * Returns the left edge coordinate (a scalable EdgeH object) associated with this NodeLayer.
		 * @return the left edge coordinate associated with this NodeLayer.
		 * It only makes sense if the representation is BOX or MINBOX.
		 * The returned coordinate is a scalable EdgeH object.
		 */
		public EdgeH getLeftEdge() { return points[0].getX(); }

		/**
		 * Returns the bottom edge coordinate (a scalable EdgeV object) associated with this NodeLayer.
		 * @return the bottom edge coordinate associated with this NodeLayer.
		 * It only makes sense if the representation is BOX or MINBOX.
		 * The returned coordinate is a scalable EdgeV object.
		 */
		public EdgeV getBottomEdge() { return points[0].getY(); }

		/**
		 * Returns the right edge coordinate (a scalable EdgeH object) associated with this NodeLayer.
		 * @return the right edge coordinate associated with this NodeLayer.
		 * It only makes sense if the representation is BOX or MINBOX.
		 * The returned coordinate is a scalable EdgeH object.
		 */
		public EdgeH getRightEdge() { return points[1].getX(); }

		/**
		 * Returns the top edge coordinate (a scalable EdgeV object) associated with this NodeLayer.
		 * @return the top edge coordinate associated with this NodeLayer.
		 * It only makes sense if the representation is BOX or MINBOX.
		 * The returned coordinate is a scalable EdgeV object.
		 */
		public EdgeV getTopEdge() { return points[1].getY(); }

		/**
		 * Returns the text message associated with this list NodeLayer.
		 * @return the text message associated with this list NodeLayer.
		 * This only makes sense if the style is one of the TEXT types.
		 */
		public String getMessage() { return message; }

		/**
		 * Sets the text to be drawn by this NodeLayer.
		 * @param message the text to be drawn by this NodeLayer.
		 * This only makes sense if the style is one of the TEXT types.
		 */
		public void setMessage(String message) { this.message = message; }

		/**
		 * Returns the text descriptor associated with this list NodeLayer.
		 * @return the text descriptor associated with this list NodeLayer.
		 * This only makes sense if the style is one of the TEXT types.
		 */
		public TextDescriptor getDescriptor() { return descriptor; }

		/**
		 * Sets the text descriptor to be drawn by this NodeLayer.
		 * @param descriptor the text descriptor to be drawn by this NodeLayer.
		 * This only makes sense if the style is one of the TEXT types.
		 */
		public void setDescriptor(TextDescriptor descriptor) { this.descriptor = descriptor; }

		/**
		 * Returns the left extension of this layer.
		 * Only makes sense when this is a layer in a serpentine transistor.
		 * @return the left extension of this layer.
		 */
		public double getSerpentineLWidth() { return lWidth; }
		/**
		 * Sets the left extension of this layer.
		 * Only makes sense when this is a layer in a serpentine transistor.
		 * @param lWidth the left extension of this layer.
		 */
		public void setSerpentineLWidth(double lWidth) { this.lWidth = lWidth; }

		/**
		 * Returns the right extension of this layer.
		 * Only makes sense when this is a layer in a serpentine transistor.
		 * @return the right extension of this layer.
		 */
		public double getSerpentineRWidth() { return rWidth; }
		/**
		 * Sets the right extension of this layer.
		 * Only makes sense when this is a layer in a serpentine transistor.
		 * @param rWidth the right extension of this layer.
		 */
		public void setSerpentineRWidth(double rWidth) { this.rWidth = rWidth; }

		/**
		 * Returns the top extension of this layer.
		 * Only makes sense when this is a layer in a serpentine transistor.
		 * @return the top extension of this layer.
		 */
		public double getSerpentineExtentT() { return extentT; }
		/**
		 * Sets the top extension of this layer.
		 * Only makes sense when this is a layer in a serpentine transistor.
		 * @param extentT the top extension of this layer.
		 */
		public void setSerpentineExtentT(double extentT) { this.extentT = extentT; }

		/**
		 * Returns the bottom extension of this layer.
		 * Only makes sense when this is a layer in a serpentine transistor.
		 * @return the bottom extension of this layer.
		 */
		public double getSerpentineExtentB() { return extendB; }
		/**
		 * Sets the bottom extension of this layer.
		 * Only makes sense when this is a layer in a serpentine transistor.
		 * @param extendB the bottom extension of this layer.
		 */
		public void setSerpentineExtentB(double extendB) { this.extendB = extendB; }
	}

	/** technology is not electrical */									private static final int NONELECTRICAL =       01;
	/** has no directional arcs */										private static final int NODIRECTIONALARCS =   02;
	/** has no negated arcs */											private static final int NONEGATEDARCS =       04;
	/** nonstandard technology (cannot be edited) */					private static final int NONSTANDARD =        010;
	/** statically allocated (don't deallocate memory) */				private static final int STATICTECHNOLOGY =   020;
	/** no primitives in this technology (don't auto-switch to it) */	private static final int NOPRIMTECHNOLOGY =   040;

	/** preferences for all technologies */					private static Preferences prefs = null;
	/** static list of all Technologies in Electric */		private static List technologies = new ArrayList();
	/** the current technology in Electric */				private static Technology curTech = null;
	/** the current tlayout echnology in Electric */		private static Technology curLayoutTech = null;
	/** counter for enumerating technologies */				private static int techNumber = 0;

	/** name of this technology */							private String techName;
	/** short, readable name of this technology */			private String techShortName;
	/** full description of this technology */				private String techDesc;
	/** flags for this technology */						private int userBits;
	/** 0-based index of this technology */					private int techIndex;
	/** critical dimensions for this technology */			private double scale;
	/** true if "scale" is relevant to this technology */	private boolean scaleRelevant;
	/** number of transparent layers in technology */		private int transparentLayers;
	/** the saved transparent colors for this technology */	private Pref [] transparentColorPrefs;
	/** the color map for this technology */				private Color [] colorMap;
	/** list of layers in this technology */				private List layers;
	/** count of layers in this technology */				private int layerIndex = 0;
	/** list of primitive nodes in this technology */		private List nodes;
	/** list of arcs in this technology */					private List arcs;
    /** list of PortProtos in this Technology. */			private List ports;
	/** minimum resistance in this Technology. */			private double minResistance;
	/** minimum capacitance in this Technology. */			private double minCapacitance;
	/** true if parasitic overrides were examined. */		private boolean parasiticOverridesGathered = false;
	/** Spice header cards, level 1. */						private String [] spiceHeaderLevel1;
	/** Spice header cards, level 2. */						private String [] spiceHeaderLevel2;
	/** Spice header cards, level 3. */						private String [] spiceHeaderLevel3;
	/** scale for this Technology. */						private Pref prefScale;
	/** Minimum resistance for this Technology. */			private Pref prefMinResistance;
	/** Minimum capacitance for this Technology. */			private Pref prefMinCapacitance;

	/****************************** CONTROL ******************************/

	/**
	 * Constructs a <CODE>Technology</CODE>.
	 * This should not be called directly, but instead is invoked through each subclass's factory.
	 */
	protected Technology()
	{
		this.layers = new ArrayList();
		this.nodes = new ArrayList();
		this.arcs = new ArrayList();
        this.ports = new ArrayList();
		this.scale = 1.0;
		this.scaleRelevant = true;
		this.techIndex = techNumber++;
		userBits = 0;
		if (prefs == null) prefs = Preferences.userNodeForPackage(getClass());

		// add the technology to the global list
		technologies.add(this);
	}

	private static final String [] extraTechnologies = {"tsmc90.TSMC90"};

	/**
	 * This is called once, at the start of Electric, to initialize the technologies.
	 * Because of Java's "lazy evaluation", the only way to force the technology constructors to fire
	 * and build a proper list of technologies, is to call each class.
	 * So, each technology is listed here.  If a new technology is created, this must be added to this list.
	 */
	public static void initAllTechnologies()
	{
		// Because of lazy evaluation, technologies aren't initialized unless they're referenced here
		Artwork.tech.setup();
		CMOS.tech.setup();
		MoCMOS.tech.setup();
		MoCMOSOld.tech.setup();
		MoCMOSSub.tech.setup();
		nMOS.tech.setup();
		RCMOS.tech.setup();
		Schematics.tech.setup();
		Generic.tech.setup();

		// initialize technologies that may not be present
		for(int i=0; i<extraTechnologies.length; i++)
		{
			try
			{
				Class extraTechClass = Class.forName("com.sun.electric.plugins." + extraTechnologies[i]);
				extraTechClass.getMethod("setItUp", (Class[])null).invoke(null, (Object[])null);
	 		} catch (Exception e)
            {
                System.out.println("Exceptions while importing extra technologies");
                ActivityLogger.logException(e);
            }
		}

		// set the current technology
		MoCMOS.tech.setCurrent();

		// setup the generic technology to handle all connections
		Generic.tech.makeUnivList();
	}

	/**
	 * Method to initialize a technology.
	 * Calls the technology's specific "init()" method (if any).
	 * Also sets up mappings from pseudo-layers to real layers.
	 */
	protected void setup()
	{
		// do any specific intialization
		init();

		// setup mapping from pseudo-layers to real layers
		for(Iterator it = this.getLayers(); it.hasNext(); )
		{
			Layer layer = (Layer)it.next();
			int extras = layer.getFunctionExtras();
			if ((extras & Layer.Function.PSEUDO) == 0) continue;
			Layer.Function fun = layer.getFunction();
			for(Iterator oIt = this.getLayers(); oIt.hasNext(); )
			{
				Layer oLayer = (Layer)oIt.next();
				int oExtras = oLayer.getFunctionExtras();
				Layer.Function oFun = oLayer.getFunction();
				if (oFun == fun && (oExtras == (extras & ~Layer.Function.PSEUDO)))
				{
					layer.setNonPseudoLayer(oLayer);
					break;
				}
			}
		}
	}

	/**
	 * Method to initialize a technology.
	 * It gets overridden by individual technologies.
	 */
	public void init() {}

	/**
	 * Returns the current Technology.
	 * @return the current Technology.
	 * The current technology is maintained by the system as a default
	 * in situations where a technology cannot be determined.
	 */
	public static Technology getCurrent() { return curTech; }

	/**
	 * Set this to be the current Technology
	 * The current technology is maintained by the system as a default
	 * in situations where a technology cannot be determined.
	 */
	public void setCurrent()
	{
		curTech = this;
		if (this != Generic.tech && this != Schematics.tech && this != Artwork.tech)
			curLayoutTech = this;
	}

	/**
	 * Returns the total number of Technologies currently in Electric.
	 * @return the total number of Technologies currently in Electric.
	 */
	public static int getNumTechnologies()
	{
		return technologies.size();
	}

	/**
	 * Find the Technology with a particular name.
	 * @param name the name of the desired Technology
	 * @return the Technology with the same name, or null if no 
	 * Technology matches.
	 */
	public static Technology findTechnology(String name)
	{
		for (int i = 0; i < technologies.size(); i++)
		{
			Technology t = (Technology) technologies.get(i);
			if (t.techName.equalsIgnoreCase(name))
				return t;
		}
		return null;
	}

	/**
	 * Get an iterator over all of the Technologies.
	 * @return an iterator over all of the Technologies.
	 */
	public static Iterator getTechnologies()
	{
		return technologies.iterator();
	}

	/**
	 * Method to return an iterator over all libraries.
	 * @return an iterator over all libraries.
	 */
	public static List getTechnologiesSortedByName()
	{
		List sortedList = new ArrayList();
		for(Iterator it = getTechnologies(); it.hasNext(); )
			sortedList.add(it.next());
		Collections.sort(sortedList, new TextUtils.TechnologiesByName());
		return sortedList;
	}

	/****************************** LAYERS ******************************/

	/**
	 * Returns an Iterator on the Layers in this Technology.
	 * @return an Iterator on the Layers in this Technology.
	 */
	public Iterator getLayers()
	{
		return layers.iterator();
	}

	/**
	 * Returns a specific Layer number in this Technology.
	 * @param index the index of the desired Layer.
	 * @return the indexed Layer in this Technology.
	 */
	public Layer getLayer(int index)
	{
		return (Layer)layers.get(index);
	}

	/**
	 * Returns the number of Layers in this Technology.
	 * @return the number of Layers in this Technology.
	 */
	public int getNumLayers()
	{
		return layers.size();
	}

	/**
	 * Method to find a Layer with a given name.
	 * @param layerName the name of the desired Layer.
	 * @return the Layer with that name (null if none found).
	 */
	public Layer findLayer(String layerName)
	{
		for(Iterator it = getLayers(); it.hasNext(); )
		{
			Layer layer = (Layer)it.next();
			if (layer.getName().equalsIgnoreCase(layerName)) return layer;
		}
		return null;
	}

	/**
	 * Method to add a new Layer to this Technology.
	 * This is usually done during initialization.
	 * @param layer the Layer to be added to this Technology.
	 */
	public void addLayer(Layer layer)
	{
		layer.setIndex(layerIndex++);
		layers.add(layer);
	}

	/**
	 * Method to tell whether two layers should be considered equivalent for the purposes of cropping.
	 * The method is overridden by individual technologies to provide specific answers.
	 * @param layer1 the first Layer.
	 * @param layer2 the second Layer.
	 * @return true if the layers are equivalent.
	 */
	public boolean sameLayer(Layer layer1, Layer layer2)
	{
		return layer1 == layer2;
	}

	/**
	 * Method to make a sorted list of layers in this Technology.
	 * The list is sorted by depth (from bottom to top).
	 * @return a sorted list of Layers in this Technology.
	 */
	public List getLayersSortedByHeight()
	{
		// determine order of overlappable layers in current technology
		List layerList = new ArrayList();
		for(Iterator it = getLayers(); it.hasNext(); )
		{
			layerList.add(it.next());
		}
		Collections.sort(layerList, new LayerHeight());
		return(layerList);
	}

	private static class LayerHeight implements Comparator
	{
		public int compare(Object o1, Object o2)
		{
			Layer l1 = (Layer)o1;
			Layer l2 = (Layer)o2;
			int h1 = l1.getFunction().getHeight();
			int h2 = l2.getFunction().getHeight();
			return h1 - h2;
		}
	}

	/****************************** ARCS ******************************/

	/**
	 * Returns the PrimitiveArc in this technology with a particular name.
	 * @param name the name of the PrimitiveArc.
	 * @return the PrimitiveArc in this technology with that name.
	 */
	public PrimitiveArc findArcProto(String name)
	{
		for (int i = 0; i < arcs.size(); i++)
		{
			PrimitiveArc ap = (PrimitiveArc) arcs.get(i);
			if (ap.getName().equalsIgnoreCase(name))
				return ap;
		}
		return null;
	}

	/**
	 * Returns an Iterator on the PrimitiveArc objects in this technology.
	 * @return an Iterator on the PrimitiveArc objects in this technology.
	 */
	public Iterator getArcs()
	{
		return arcs.iterator();
	}

	/**
	 * Returns the number of PrimitiveArc objects in this technology.
	 * @return the number of PrimitiveArc objects in this technology.
	 */
	public int getNumArcs()
	{
		return arcs.size();
	}

	/**
	 * Method to add a new PrimitiveArc to this Technology.
	 * This is usually done during initialization.
	 * @param ap the PrimitiveArc to be added to this Technology.
	 */
	public void addArcProto(PrimitiveArc ap)
	{
		arcs.add(ap);
	}

	/**
	 * Sets the technology to have no directional arcs.
	 * Users should never call this method.
	 * It is set once by the technology during initialization.
	 * Directional arcs are those with arrows on them, indicating (only graphically) the direction of flow through the arc.
	 * @see ArcInst#setDirectional
	 * @see ArcProto#setDirectional
	 */
	protected void setNoDirectionalArcs() { userBits |= NODIRECTIONALARCS; }

	/**
	 * Returns true if this technology does not have directional arcs.
	 * @return true if this technology does not have directional arcs.
	 * Directional arcs are those with arrows on them, indicating (only graphically) the direction of flow through the arc.
	 * @see ArcInst#setDirectional
	 * @see ArcProto#setDirectional
	 */
	public boolean isNoDirectionalArcs() { return (userBits & NODIRECTIONALARCS) != 0; }

	/**
	 * Sets the technology to have no negated arcs.
	 * Users should never call this method.
	 * It is set once by the technology during initialization.
	 * Negated arcs have bubbles on them to graphically indicated negation.
	 * Only Schematics and related technologies allow negated arcs.
	 */
	protected void setNoNegatedArcs() { userBits |= NONEGATEDARCS; }

	/**
	 * Returns true if this technology does not have negated arcs.
	 * @return true if this technology does not have negated arcs.
	 * Negated arcs have bubbles on them to graphically indicated negation.
	 * Only Schematics and related technologies allow negated arcs.
	 */
	public boolean isNoNegatedArcs() { return (userBits & NONEGATEDARCS) != 0; }

	/**
	 * Returns the polygons that describe arc "ai".
	 * @param ai the ArcInst that is being described.
	 * @return an array of Poly objects that describes this ArcInst graphically.
	 * This array includes displayable variables on the ArcInst.
	 */
	public Poly [] getShapeOfArc(ArcInst ai)
	{
		return getShapeOfArc(ai, null, null);
	}

	/**
	 * Returns the polygons that describe arc "ai".
	 * @param ai the ArcInst that is being described.
	 * @param wnd the window in which this arc is being displayed.
	 * @return an array of Poly objects that describes this ArcInst graphically.
	 * This array includes displayable variables on the ArcInst.
	 */
	public Poly [] getShapeOfArc(ArcInst ai, EditWindow wnd)
	{
		return getShapeOfArc(ai, wnd, null);
	}

	/**
	 * Returns the polygons that describe arc "ai".
	 * @param ai the ArcInst that is being described.
	 * @param wnd the window in which this arc is being displayed.
	 * @param layerOverride the layer to use for all generated polygons (if not null).
	 * @return an array of Poly objects that describes this ArcInst graphically.
	 * This array includes displayable variables on the ArcInst.
	 */
	public Poly [] getShapeOfArc(ArcInst ai, EditWindow wnd, Layer layerOverride)
	{
		// get information about the arc
		PrimitiveArc ap = (PrimitiveArc)ai.getProto();
		Technology tech = ap.getTechnology();
		Technology.ArcLayer [] primLayers = ap.getLayers();

		// see how many polygons describe this arc
		boolean addArrow = false;
		if (!tech.isNoDirectionalArcs() && ai.isDirectional()) addArrow = true;
		int numDisplayable = ai.numDisplayableVariables(true);
		if (wnd == null) numDisplayable = 0;
		int maxPolys = primLayers.length + numDisplayable;
		if (addArrow) maxPolys++;
		Poly [] polys = new Poly[maxPolys];
		int polyNum = 0;

		// construct the polygons that describe the basic arc
		Layer lastLayer = null;
		for(int i = 0; i < primLayers.length; i++)
		{
			Technology.ArcLayer primLayer = primLayers[i];
			polys[polyNum] = ai.makePoly(ai.getLength(), ai.getWidth() - primLayer.getOffset(), primLayer.getStyle());
			if (polys[polyNum] == null) return null;
			if (layerOverride != null) polys[polyNum].setLayer(layerOverride); else
			{
				lastLayer = primLayer.getLayer();
				polys[polyNum].setLayer(lastLayer);
			}
			polyNum++;
		}

		// add an arrow to the arc description
		if (addArrow)
		{
			Point2D headLoc = ai.getHead().getLocation();
			Point2D tailLoc = ai.getTail().getLocation();
			double headX = headLoc.getX();   double headY = headLoc.getY();
			double tailX = tailLoc.getX();   double tailY = tailLoc.getY();
			int angle = ai.getAngle();
			if (ai.isReverseEnds())
			{
				double swap = headX;   headX = tailX;   tailX = swap;
				swap = headY;   headY = tailY;   tailY = swap;
			} else
			{
				angle += 1800;
			}
			Point2D [] points = null;
			
			if (ai.isSkipHead())
			{
				points = new Point2D.Double[2];
				points[0] = new Point2D.Double(headX, headY);
				points[1] = new Point2D.Double(tailX, tailY);
			} else
			{
				points = new Point2D.Double[6];
				int angleOfArrow = 300;		// 30 degrees
				int backAngle1 = angle - angleOfArrow;
				int backAngle2 = angle + angleOfArrow;
				points[0] = new Point2D.Double(headX, headY);
				points[1] = new Point2D.Double(tailX, tailY);
				points[2] = new Point2D.Double(headX, headY);
				points[3] = new Point2D.Double(headX + DBMath.cos(backAngle1), headY + DBMath.sin(backAngle1));
				points[4] = points[0];
				points[5] = new Point2D.Double(headX + DBMath.cos(backAngle2), headY + DBMath.sin(backAngle2));
			}
			polys[polyNum] = new Poly(points);
			polys[polyNum].setStyle(Poly.Type.VECTORS);
			polys[polyNum].setLayer(Generic.tech.universal_lay);
			polyNum++;
		}
		
		// add in the displayable variables
		if (numDisplayable > 0)
		{
			Rectangle2D rect = ai.getBounds();
			ai.addDisplayableVariables(rect, polys, polyNum, wnd, true);
		}

		return polys;
	}

	/**
	 * Method to convert old primitive arc names to their proper ArcProtos.
	 * This method is overridden by those technologies that have any special arc name conversion issues.
	 * By default, there is nothing to be done, because by the time this
	 * method is called, normal searches have failed.
	 * @param name the unknown arc name, read from an old Library.
	 * @return the proper PrimitiveArc to use for this name.
	 */
	public PrimitiveArc convertOldArcName(String name) { return null; }

	/****************************** NODES ******************************/

	/**
	 * Returns the PrimitiveNode in this technology with a particular name.
	 * @param name the name of the PrimitiveNode.
	 * @return the PrimitiveNode in this technology with that name.
	 */
	public PrimitiveNode findNodeProto(String name)
	{
		for (int i = 0; i < nodes.size(); i++)
		{
			PrimitiveNode pn = (PrimitiveNode) nodes.get(i);
			if (pn.getName().equalsIgnoreCase(name))
				return pn;
		}
		return null;
	}

	/**
	 * Returns an Iterator on the PrimitiveNode objects in this technology.
	 * @return an Iterator on the PrimitiveNode objects in this technology.
	 */
	public Iterator getNodes()
	{
		return nodes.iterator();
	}

	/**
	 * Returns the number of PrimitiveNodes objects in this technology.
	 * @return the number of PrimitiveNodes objects in this technology.
	 */
	public int getNumNodes()
	{
		return nodes.size();
	}

	/**
	 * Method to add a new PrimitiveNode to this Technology.
	 * This is usually done during initialization.
	 * @param np the PrimitiveNode to be added to this Technology.
	 */
	public void addNodeProto(PrimitiveNode np)
	{
		nodes.add(np);
	}

    /**
     * Returns an Iterator on the PrimitivePort objects in this technology.
     * @return an Iterator on the PrimitivePort objects in this technology.
     */
    public Iterator getPorts()
    {
        return ports.iterator();
    }

    /**
     * Returns the number of PrimitivePorts objects in this technology.
     * @return the number of PrimitivePorts objects in this technology.
     */
    public int getNumPorts()
    {
        return ports.size();
    }

    /**
     * Method to add a new PrimitivePort to this Technology.
     * This is usually done during initialization.
     * @param pp the PrimitivePort to be added to this Technology.
     */
    public void addPortProto(PrimitivePort pp)
    {
        ports.add(pp);
    }

	/**
	 * Method to return the pure "NodeProto Function" a primitive NodeInst in this Technology.
	 * This method is overridden by technologies (such as Schematics) that know the node's function.
	 * @param ni the NodeInst to check.
	 * @return the NodeProto.Function that describes the NodeInst.
	 */
	public NodeProto.Function getPrimitiveFunction(NodeInst ni) { return ni.getProto().getFunction(); }

	/**
	 * Method to return the size of a transistor NodeInst in this Technology.
     * You should most likely be calling NodeInst.getTransistorSize instead of this.
	 * @param ni the NodeInst.
     * @param context the VarContext in which any vars will be evaluated,
     * pass in VarContext.globalContext if no context needed, or set to null
     * to avoid evaluation of variables (if any).
	 * @return the size of the NodeInst.
	 */
	public TransistorSize getTransistorSize(NodeInst ni, VarContext context)
	{
		SizeOffset so = ni.getSizeOffset();
		double width = ni.getXSize() - so.getLowXOffset() - so.getHighXOffset();
		double height = ni.getYSize() - so.getLowYOffset() - so.getHighYOffset();
		//Dimension2D dim = new Dimension2D.Double(width, height);
		//return dim;
        PrimitiveNode np = (PrimitiveNode)ni.getProto();
        int specialType = np.getSpecialType();
        if (specialType == PrimitiveNode.SERPTRANS) {
            // get transistor width from distance of trace
            Variable trace = ni.getVar("trace");
            if (trace != null) {
                width = 0;
                Point2D [] tracePts = (Point2D [])trace.getObject();
                for (int i = 1; i<tracePts.length; i++) {
                    Point2D p1 = tracePts[i-1];
                    Point2D p2 = tracePts[i];
                    width += p1.distance(p2);
                }
                height = 2;
            }
        }
		TransistorSize size = new TransistorSize(new Double(width), new Double(height));
		return size;
	}

    /**
     * Method to set the size of a transistor NodeInst in this Technology.
     * You should be calling NodeInst.setTransistorSize instead of this.
     * @param ni the NodeInst
     * @param width the new width (positive values only)
     * @param length the new length (positive values only)
     */
    public void setTransistorSize(NodeInst ni, double width, double length)
    {
        SizeOffset so = ni.getSizeOffset();
        double oldWidth = ni.getXSize() - so.getLowXOffset() - so.getHighXOffset();
        double oldLength = ni.getYSize() - so.getLowYOffset() - so.getHighYOffset();
        double dW = width - oldWidth;
        double dL = length - oldLength;
        if (ni.getXSizeWithMirror() < 0) dW = -dW;
        if (ni.getYSizeWithMirror() < 0) dL = -dL;
        ni.modifyInstance(0, 0, dW, dL, 0);
    }

    /**
     * Method to return a gate PortInst for this transistor NodeInst.
     * Implementation Note: May want to make this a more general
     * method, getPrimitivePort(PortType), if the number of port
     * types increases.  Note: You should be calling 
     * NodeInst.getTransistorGatePort() instead of this, most likely.
     * @param ni the NodeInst
     * @return a PortInst for the gate of the transistor
     */
	public PortInst getTransistorGatePort(NodeInst ni) { return ni.getPortInst(0); }
    
    /**
     * Method to return a source PortInst for this transistor NodeInst.
     * Implementation Note: May want to make this a more general
     * method, getPrimitivePort(PortType), if the number of port
     * types increases.  Note: You should be calling 
     * NodeInst.getTransistorSourcePort() instead of this, most likely.
     * @param ni the NodeInst
     * @return a PortInst for the source of the transistor
     */
	public PortInst getTransistorSourcePort(NodeInst ni) { return ni.getPortInst(1); }

    /**
     * Method to return a drain PortInst for this transistor NodeInst.
     * Implementation Note: May want to make this a more general
     * method, getPrimitivePort(PortType), if the number of port
     * types increases.  Note: You should be calling 
     * NodeInst.getTransistorDrainPort() instead of this, most likely.
     * @param ni the NodeInst
     * @return a PortInst for the drain of the transistor
     */
	public PortInst getTransistorDrainPort(NodeInst ni)
	{
		if (ni.getProto().getTechnology() == Schematics.tech) return ni.getPortInst(2);
		return ni.getPortInst(3);
	}

    /**
     * Method to return a bias PortInst for this transistor NodeInst.
     * Implementation Note: May want to make this a more general
     * method, getPrimitivePort(PortType), if the number of port
     * types increases.  Note: You should be calling 
     * NodeInst.getTransistorBiasPort() instead of this, most likely.
     * @param ni the NodeInst
     * @return a PortInst for the bias of the transistor
     */
	public PortInst getTransistorBiasPort(NodeInst ni)
	{
		if (ni.getNumPortInsts() < 4) return null;
		if (ni.getProto().getTechnology() != Schematics.tech) return null;
		return ni.getPortInst(3);
	}

    /**
	 * Method to set the pure "NodeProto Function" for a primitive NodeInst in this Technology.
	 * This method is overridden by technologies (such as Schematics) that can change a node's function.
	 * @param ni the NodeInst to check.
	 * @param function the NodeProto.Function to set on the NodeInst.
	 */
	public void setPrimitiveFunction(NodeInst ni, NodeProto.Function function) {}

	/**
	 * Sets the technology to have no primitives.
	 * Users should never call this method.
	 * It is set once by the technology during initialization.
	 * This indicates to the user interface that it should not switch to this technology.
	 * The FPGA technology has this bit set because it initially contains no primitives,
	 * and they are only created dynamically.
	 */
	public void setNoPrimitiveNodes() { userBits |= NOPRIMTECHNOLOGY; }

	/**
	 * Returns true if this technology has no primitives.
	 * @return true if this technology has no primitives.
	 * This indicates to the user interface that it should not switch to this technology.
	 * The FPGA technology has this bit set because it initially contains no primitives,
	 * and they are only created dynamically.
	 */
	public boolean isNoPrimitiveNodes() { return (userBits & NOPRIMTECHNOLOGY) != 0; }

    /**
	 * Method to set default outline information on a NodeInst.
	 * Very few primitives have default outline information (usually just in the Artwork Technology).
	 * This method is overridden by the appropriate technology.
	 * @param ni the NodeInst to load with default outline information.
	 */
	public void setDefaultOutline(NodeInst ni) {}

	/**
	 * Method to get the SizeOffset associated with a NodeInst in this Technology.
	 * By having this be a method of Technology, it can be overridden by
	 * individual Technologies that need to make special considerations.
	 * @param ni the NodeInst to query.
	 * @return the SizeOffset object for the NodeInst.
	 */
	public SizeOffset getNodeInstSizeOffset(NodeInst ni)
	{
		NodeProto np = ni.getProto();
		return np.getProtoSizeOffset();
	}

	private static final Technology.NodeLayer [] nullPrimLayers = new Technology.NodeLayer [0];

	/**
	 * Returns the polygons that describe node "ni".
	 * @param ni the NodeInst that is being described.
	 * The prototype of this NodeInst must be a PrimitiveNode and not a Cell.
	 * @return an array of Poly objects that describes this NodeInst graphically.
	 * This array includes displayable variables on the NodeInst.
	 */
	public Poly [] getShapeOfNode(NodeInst ni)
	{
		return getShapeOfNode(ni, null, false, false);
	}

	/**
	 * Returns the polygons that describe node "ni".
	 * @param ni the NodeInst that is being described.
	 * The prototype of this NodeInst must be a PrimitiveNode and not a Cell.
	 * @param wnd the window in which this node will be drawn (null if no window scaling should be done).
	 * @return an array of Poly objects that describes this NodeInst graphically.
	 * This array includes displayable variables on the NodeInst.
	 */
	public Poly [] getShapeOfNode(NodeInst ni, EditWindow wnd)
	{
		return getShapeOfNode(ni, wnd, false, false);
	}

	/**
	 * Returns the polygons that describe node "ni".
	 * @param ni the NodeInst that is being described.
	 * The prototype of this NodeInst must be a PrimitiveNode and not a Cell.
	 * @param wnd the window in which this node will be drawn (null if no window scaling should be done).
	 * @param electrical true to get the "electrical" layers.
	 * When electrical layers are requested, each layer is tied to a specific port on the node.
	 * If any piece of geometry covers more than one port,
	 * it must be split for the purposes of an "electrical" description.
	 * For example, the MOS transistor has 2 layers: Active and Poly.
	 * But it has 3 electrical layers: Active, Active, and Poly.
	 * The active must be split since each half corresponds to a different PrimitivePort on the PrimitiveNode.
	 * @param reasonable true to get only a minimal set of contact cuts in large contacts.
	 * The minimal set covers all edge contacts, but ignores the inner cuts in large contacts.
	 * @return an array of Poly objects that describes this NodeInst graphically.
	 * This array includes displayable variables on the NodeInst.
	 */
	public Poly [] getShapeOfNode(NodeInst ni, EditWindow wnd, boolean electrical, boolean reasonable)
	{
		NodeProto prototype = ni.getProto();
		if (!(prototype instanceof PrimitiveNode)) return null;

		PrimitiveNode np = (PrimitiveNode)prototype;
		Technology.NodeLayer [] primLayers = np.getLayers();
		if (electrical)
		{
			Technology.NodeLayer [] eLayers = np.getElectricalLayers();
			if (eLayers != null) primLayers = eLayers;
		}

		// if node is erased, remove layers
		if (ni.isWiped()) primLayers = nullPrimLayers; else
		{
			if (np.isWipeOn1or2())
			{
				if (ni.pinUseCount()) primLayers = nullPrimLayers;
			}
		}
		return getShapeOfNode(ni, wnd, electrical, reasonable, primLayers, null);
	}

	/**
	 * Returns the polygons that describe node "ni", given a set of
	 * NodeLayer objects to use.
	 * @param ni the NodeInst that is being described.
	 * @param wnd the window in which this node will be drawn.
	 * If this is null, no window scaling can be done, so no text is included in the returned results.
	 * @param electrical true to get the "electrical" layers
	 * Like the list returned by "getLayers", the results describe this PrimitiveNode,
	 * but each layer is tied to a specific port on the node.
	 * If any piece of geometry covers more than one port,
	 * it must be split for the purposes of an "electrical" description.<BR>
	 * For example, the MOS transistor has 2 layers: Active and Poly.
	 * But it has 3 electrical layers: Active, Active, and Poly.
	 * The active must be split since each half corresponds to a different PrimitivePort on the PrimitiveNode.
	 * @param reasonable true to get only a minimal set of contact cuts in large contacts.
	 * The minimal set covers all edge contacts, but ignores the inner cuts in large contacts.
	 * @param primLayers an array of NodeLayer objects to convert to Poly objects.
	 * @param layerOverride the layer to use for all generated polygons (if not null).
	 * The prototype of this NodeInst must be a PrimitiveNode and not a Cell.
	 * @return an array of Poly objects that describes this NodeInst graphically.
	 * This array includes displayable variables on the NodeInst (if wnd != null).
	 */
	public Poly [] getShapeOfNode(NodeInst ni, EditWindow wnd, boolean electrical, boolean reasonable, Technology.NodeLayer [] primLayers, Layer layerOverride)
	{
		PrimitiveNode np = (PrimitiveNode)ni.getProto();
		int specialType = np.getSpecialType();
		if (specialType != PrimitiveNode.SERPTRANS && np.isHoldsOutline())
		{
			Point2D [] outline = ni.getTrace();
			if (outline != null)
			{
				int numPolys = 1;
				if (wnd != null) numPolys += ni.numDisplayableVariables(true);
				Poly [] polys = new Poly[numPolys];
				Point2D [] pointList = new Point2D.Double[outline.length];
				for(int i=0; i<outline.length; i++)
				{
					pointList[i] = new Point2D.Double(ni.getTrueCenterX() + outline[i].getX(),
						ni.getTrueCenterY() + outline[i].getY());
				}
				polys[0] = new Poly(pointList);
				Technology.NodeLayer primLayer = primLayers[0];
				polys[0].setStyle(primLayer.getStyle());
				if (layerOverride != null) polys[0].setLayer(layerOverride); else
					polys[0].setLayer(primLayer.getLayer());
				Rectangle2D rect = ni.getBounds();
				if (wnd != null) ni.addDisplayableVariables(rect, polys, 1, wnd, true);
				return polys;
			}
		}

		// determine the number of polygons (considering that it may be "wiped")
		int numBasicLayers = primLayers.length;

		// if a MultiCut contact, determine the number of extra cuts
		int numExtraLayers = 0;
		MultiCutData mcd = null;
		SerpentineTrans std = null;
		if (specialType == PrimitiveNode.MULTICUT)
		{
			mcd = new MultiCutData(ni, np.getSpecialValues());
			if (reasonable) numExtraLayers = mcd.cutsReasonable; else
			numExtraLayers = mcd.cutsTotal;
			numBasicLayers--;
		} else if (specialType == PrimitiveNode.SERPTRANS)
		{
			std = new SerpentineTrans(ni);
			if (std.layersTotal > 0)
			{
				numExtraLayers = std.layersTotal;
				numBasicLayers = 0;
			}
		}

		// determine the number of negating bubbles
		int numNegatingBubbles = 0;
		for(Iterator it = ni.getConnections(); it.hasNext(); )
		{
			Connection con = (Connection)it.next();
			if (con.isNegated()) numNegatingBubbles++;
		}

		// construct the polygon array
		int numPolys = numBasicLayers + numExtraLayers + numNegatingBubbles;
		if (wnd != null) numPolys += ni.numDisplayableVariables(true);
		Poly [] polys = new Poly[numPolys];
		
		// add in the basic polygons
		int fillPoly = 0;
		for(int i = 0; i < numBasicLayers; i++)
		{
			double xCenter = ni.getTrueCenterX();
			double yCenter = ni.getTrueCenterY();
			double xSize = ni.getXSize();
			double ySize = ni.getYSize();
			Technology.NodeLayer primLayer = primLayers[i];
			int representation = primLayer.getRepresentation();
			if (representation == Technology.NodeLayer.BOX || representation == Technology.NodeLayer.MINBOX)
			{
				EdgeH leftEdge = primLayer.getLeftEdge();
				EdgeH rightEdge = primLayer.getRightEdge();
				EdgeV topEdge = primLayer.getTopEdge();
				EdgeV bottomEdge = primLayer.getBottomEdge();
				double portLowX = xCenter + leftEdge.getMultiplier() * xSize + leftEdge.getAdder();
				double portHighX = xCenter + rightEdge.getMultiplier() * xSize + rightEdge.getAdder();
				double portLowY = yCenter + bottomEdge.getMultiplier() * ySize + bottomEdge.getAdder();
				double portHighY = yCenter + topEdge.getMultiplier() * ySize + topEdge.getAdder();
				Point2D [] pointList = Poly.makePoints(portLowX, portHighX, portLowY, portHighY);
				polys[fillPoly] = new Poly(pointList);
			} else if (representation == Technology.NodeLayer.POINTS)
			{
				TechPoint [] points = primLayer.getPoints();
				Point2D [] pointList = new Point2D.Double[points.length];
				for(int j=0; j<points.length; j++)
				{
					EdgeH xFactor = points[j].getX();
					EdgeV yFactor = points[j].getY();
					double x = 0, y = 0;
					if (xFactor != null && yFactor != null)
					{
						x = xCenter + xFactor.getMultiplier() * xSize + xFactor.getAdder();
						y = yCenter + yFactor.getMultiplier() * ySize + yFactor.getAdder();
					}
					pointList[j] = new Point2D.Double(x, y);
				}
				polys[fillPoly] = new Poly(pointList);
			}
			Poly.Type style = primLayer.getStyle();
			if (style.isText())
			{
				polys[fillPoly].setString(primLayer.getMessage());
				polys[fillPoly].setTextDescriptor(primLayer.getDescriptor());
			}
			polys[i].setStyle(style);
			if (layerOverride != null) polys[i].setLayer(layerOverride); else
				polys[fillPoly].setLayer(primLayer.getLayer());
			if (electrical)
			{
				int portIndex = primLayer.getPortNum();
				PortProto port = null;
				if (portIndex >= 0) port = np.getPort(portIndex);
				polys[fillPoly].setPort(port);
			}
			fillPoly++;
		}

		// add in the extra contact cuts
		if (mcd != null)
		{
			Technology.NodeLayer primLayer = primLayers[numBasicLayers];
			Poly.Type style = primLayer.getStyle();
			PortProto port = null;
			if (electrical) port = np.getPort(0);
			for(int i = 0; i < numExtraLayers; i++)
			{
				polys[fillPoly] = mcd.fillCutPoly(ni, i);
				polys[fillPoly].setStyle(style);
				polys[fillPoly].setLayer(primLayer.getLayer());
				polys[fillPoly].setPort(port);
				fillPoly++;
			}
		}

		// add in negating bubbles
		if (numNegatingBubbles > 0)
		{
			double bubbleRadius = Schematics.getNegatingBubbleSize() / 2;
			for(Iterator it = ni.getConnections(); it.hasNext(); )
			{
				Connection con = (Connection)it.next();
				if (!con.isNegated()) continue;

				// add a negating bubble
				AffineTransform trans = ni.rotateIn();
				Point2D portLocation = new Point2D.Double(con.getLocation().getX(), con.getLocation().getY());
				trans.transform(portLocation, portLocation);
				double x = portLocation.getX();
				double y = portLocation.getY();
				PrimitivePort pp = (PrimitivePort)con.getPortInst().getPortProto();
				int angle = pp.getAngle() * 10;
				double dX = DBMath.cos(angle) * bubbleRadius;
				double dY = DBMath.sin(angle) * bubbleRadius;
				Point2D [] points = new Point2D[2];
				points[0] = new Point2D.Double(x+dX, y+dY);
				points[1] = new Point2D.Double(x, y);
				polys[fillPoly] = new Poly(points);
				polys[fillPoly].setStyle(Poly.Type.CIRCLE);
				polys[fillPoly].setLayer(Schematics.tech.node_lay);
				fillPoly++;
			}
		}

		// add in the extra transistor layers
		if (std != null)
		{
			for(int i = 0; i < numExtraLayers; i++)
			{
				polys[fillPoly] = std.fillTransPoly(i);
				fillPoly++;
			}
		}

		// add in the displayable variables
		if (wnd != null)
		{
			Rectangle2D rect = ni.getBounds();
			ni.addDisplayableVariables(rect, polys, fillPoly, wnd, true);
		}
		return polys;
	}

	/**
	 * Class MultiCutData determines the locations of cuts in a multi-cut contact node.
	 */
	public static class MultiCutData
	{
		/** the size of each cut */													private double cutSizeX, cutSizeY;
		/** the separation between cuts */											private double cutSep;
		/** the separation between cuts */											private double cutSep1D;
		/** the separation between cuts in 3-neiboring or more cases*/				private double cutSep2D;
		/** the indent of the edge cuts to the node along X */						private double cutIndentX;
		/** the indent of the edge cuts to the node along Y */						private double cutIndentY;
		/** the number of cuts in X and Y */										private int cutsX, cutsY;
		/** the total number of cuts */												private int cutsTotal;
		/** the "reasonable" number of cuts (around the outside only) */			private int cutsReasonable;
		/** the X coordinate of the leftmost cut's center */						private double cutBaseX;
		/** the Y coordinate of the topmost cut's center */							private double cutBaseY;
		/** cut position of last top-edge cut (for interior-cut elimination) */		private double cutTopEdge;
		/** cut position of last left-edge cut  (for interior-cut elimination) */	private double cutLeftEdge;
		/** cut position of last right-edge cut  (for interior-cut elimination) */	private double cutRightEdge;

		/**
		 * Constructor to initialize for multiple cuts.
		 * @param ni the NodeInst with multiple cuts.
		 * @param specialValues the array of special values for the NodeInst.
		 * The values in "specialValues" are:
		 *     cuts sized "cutSizeX" x "cutSizeY" (specialValues[0] x specialValues[1])
		 *     cuts indented at least "cutIndent" from the node edge (specialValues[2])
		 *     cuts separated by "cutSep" (specialValues[3])
		 */
		public MultiCutData(NodeInst ni, double [] specialValues)
		{
			cutSizeX = specialValues[0];
			cutSizeY = specialValues[1];
			cutIndentX = specialValues[2];
			cutIndentY = specialValues[3];
			cutSep1D = specialValues[4];
            cutSep2D = specialValues[5];

			// determine the actual node size
			PrimitiveNode np = (PrimitiveNode)ni.getProto();
			SizeOffset so = ni.getSizeOffset();
			double cutLX = so.getLowXOffset();
			double cutHX = so.getHighXOffset();
			double cutLY = so.getLowYOffset();
			double cutHY = so.getHighYOffset();
			double cutAreaWidth = ni.getXSize() - cutLX - cutHX;
			double cutAreaHeight = ni.getYSize() - cutLY - cutHY;

			// number of cuts depends on the size
			// Checking first if configuration gives 1D cuts
			int oneDcutsX = (int)((cutAreaWidth-cutIndentX*2+cutSep1D) / (cutSizeX+cutSep1D));
			int oneDcutsY = (int)((cutAreaHeight-cutIndentY*2+cutSep1D) / (cutSizeY+cutSep1D));

			cutSep = cutSep1D;
			if (cutSep1D != cutSep2D &&
			        ((oneDcutsX > 2 && oneDcutsY > 1) ||
			        (oneDcutsY > 2 && oneDcutsX > 1)))
			{
				// 2D cutspace active
				cutSep = cutSep2D;
			}
			cutsX = (int)((cutAreaWidth-cutIndentX*2+cutSep) / (cutSizeX+cutSep));
			cutsY = (int)((cutAreaHeight-cutIndentY*2+cutSep) / (cutSizeY+cutSep));
			if (cutsX <= 0) cutsX = 1;
			if (cutsY <= 0) cutsY = 1;
			cutsReasonable = cutsTotal = cutsX * cutsY;
			if (cutsTotal != 1)
			{
				// prepare for the multiple contact cut locations
				cutBaseX = (cutAreaWidth-cutIndentX*2 - cutSizeX*cutsX -
					cutSep*(cutsX-1)) / 2 + (cutLX + cutIndentX + cutSizeX/2) + ni.getAnchorCenterX() - ni.getXSize() / 2;
				cutBaseY = (cutAreaHeight-cutIndentY*2 - cutSizeY*cutsY -
					cutSep*(cutsY-1)) / 2 + (cutLY + cutIndentY + cutSizeY/2) + ni.getAnchorCenterY() - ni.getYSize() / 2;
				if (cutsX > 2 && cutsY > 2)
				{
					cutsReasonable = cutsX * 2 + (cutsY-2) * 2;
					cutTopEdge = cutsX*2;
					cutLeftEdge = cutsX*2 + cutsY-2;
					cutRightEdge = cutsX*2 + (cutsY-2)*2;
				}
			}
		}

		/**
		 * Method to return the number of cuts in the contact node.
		 * @return the number of cuts in the contact node.
		 */
		public int numCuts() { return cutsTotal; }

		/**
		 * Method to fill in the contact cuts of a MOS contact when there are
		 * multiple cuts.  Node is in "ni" and the contact cut number (0 based) is
		 * in "cut".
		 */
		private Poly fillCutPoly(NodeInst ni, int cut)
		{
			if (cutsX > 2 && cutsY > 2)
			{
				// rearrange cuts so that the initial ones go around the outside
				if (cut < cutsX)
				{
					// bottom edge: it's ok as is
				} else if (cut < cutTopEdge)
				{
					// top edge: shift up
					cut += cutsX * (cutsY-2);
				} else if (cut < cutLeftEdge)
				{
					// left edge: rearrange
					cut = (int)((cut - cutTopEdge) * cutsX + cutsX);
				} else if (cut < cutRightEdge)
				{
					// right edge: rearrange
					cut = (int)((cut - cutLeftEdge) * cutsX + cutsX*2-1);
				} else
				{
					// center: rearrange and scale down
					cut = cut - (int)cutRightEdge;
					int cutx = cut % (cutsX-2);
					int cuty = cut / (cutsX-2);
					cut = cuty * cutsX + cutx+cutsX+1;
				}
			}

			// locate the X center of the cut
			double cX;
			if (cutsX == 1)
			{
				cX = ni.getTrueCenterX();
			} else
			{
				cX = cutBaseX + (cut % cutsX) * (cutSizeX + cutSep);
			}

			// locate the Y center of the cut
			double cY;
			if (cutsY == 1)
			{
				cY = ni.getTrueCenterY();
			} else
			{
				cY = cutBaseY + (cut / cutsX) * (cutSizeY + cutSep);
			}
			return new Poly(cX, cY, cutSizeX, cutSizeY);
		}
	}

	/**
	 * Class SerpentineTrans here.
	 */
	private static class SerpentineTrans
	{
		/** the NodeInst that is this serpentine transistor */					private NodeInst theNode;
		/** the prototype of this serpentine transistor */						private PrimitiveNode theProto;
		/** the number of polygons that make up this serpentine transistor */	private int layersTotal;
		/** the number of segments in this serpentine transistor */				private int numSegments;
		/** the extra gate width of this serpentine transistor */				private double extraScale;
		/** the node layers that make up this serpentine transistor */			private Technology.NodeLayer [] primLayers;
		/** the gate coordinates for this serpentine transistor */				private Point2D [] points;

		/**
		 * Constructor throws initialize for a serpentine transistor.
		 * @param ni the NodeInst with a serpentine transistor.
		 */
		public SerpentineTrans(NodeInst ni)
		{
			theNode = ni;
			layersTotal = 0;
			points = ni.getTrace();
			if (points != null)
			{
				if (points.length < 2) points = null;
			}
			if (points != null)
			{
				theProto = (PrimitiveNode)ni.getProto();
				primLayers = theProto.getLayers();
				int count = primLayers.length;
				numSegments = points.length - 1;
				layersTotal = count * numSegments;

				extraScale = 0;
				Variable varw = ni.getVar("transistor_width", Integer.class);
				if (varw != null)
				{
					Object obj = varw.getObject();
					extraScale = ((Integer)obj).intValue() / 120 / 2;
				}
			}
		}

		/**
		 * Method to tell whether this SerpentineTrans object has valid outline information.
		 * @return true if the data exists.
		 */
		public boolean hasValidData() { return points != null; }

		private static final int LEFTANGLE =  900;
		private static final int RIGHTANGLE =  2700;

		/**
		 * Method to describe a box of a serpentine transistor.
		 * If the variable "trace" exists on the node, get that
		 * x/y/x/y information as the centerline of the serpentine path.  The outline is
		 * placed in the polygon "poly".
		 * NOTE: For each trace segment, the left hand side of the trace
		 * will contain the polygons that appear ABOVE the gate in the node
		 * definition. That is, the "top" port and diffusion will be above a
		 * gate segment that extends from left to right, and on the left of a
		 * segment that goes from bottom to top.
		 */
		private Poly fillTransPoly(int box)
		{
			// compute the segment (along the serpent) and element (of transistor)
			int segment = box % numSegments;
			int element = box / numSegments;

			// see if nonstandard width is specified
			double lwid = primLayers[element].getSerpentineLWidth();
			double rwid = primLayers[element].getSerpentineRWidth();
			double extendt = primLayers[element].getSerpentineExtentT();
			double extendb = primLayers[element].getSerpentineExtentB();
			lwid += extraScale;
			rwid += extraScale;

			// prepare to fill the serpentine transistor
			double xoff = theNode.getTrueCenterX();
			double yoff = theNode.getTrueCenterY();
			int thissg = segment;   int next = segment+1;
			Point2D thisPt = points[thissg];
			Point2D nextPt = points[next];
			int angle = DBMath.figureAngle(thisPt, nextPt);

			// push the points at the ends of the transistor
			if (thissg == 0)
			{
				// extend "thissg" 180 degrees back
				int ang = angle+1800;
				thisPt = DBMath.addPoints(thisPt, DBMath.cos(ang) * extendt, DBMath.sin(ang) * extendt);
			}
			if (next == numSegments)
			{
				// extend "next" 0 degrees forward
				nextPt = DBMath.addPoints(nextPt, DBMath.cos(angle) * extendb, DBMath.sin(angle) * extendb);
			}

			// compute endpoints of line parallel to and left of center line
			int ang = angle+LEFTANGLE;
			double sin = DBMath.sin(ang) * lwid;
			double cos = DBMath.cos(ang) * lwid;
			Point2D thisL = DBMath.addPoints(thisPt, cos, sin);
			Point2D nextL = DBMath.addPoints(nextPt, cos, sin);

			// compute endpoints of line parallel to and right of center line
			ang = angle+RIGHTANGLE;
			sin = DBMath.sin(ang) * rwid;
			cos = DBMath.cos(ang) * rwid;
			Point2D thisR = DBMath.addPoints(thisPt, cos, sin);
			Point2D nextR = DBMath.addPoints(nextPt, cos, sin);

			// determine proper intersection of this and the previous segment
			if (thissg != 0)
			{
				Point2D otherPt = points[thissg-1];
				int otherang = DBMath.figureAngle(otherPt, thisPt);
				if (otherang != angle)
				{
					ang = otherang + LEFTANGLE;
					thisL = DBMath.intersect(DBMath.addPoints(thisPt, DBMath.cos(ang)*lwid, DBMath.sin(ang)*lwid),
						otherang, thisL,angle);
					ang = otherang + RIGHTANGLE;
					thisR = DBMath.intersect(DBMath.addPoints(thisPt, DBMath.cos(ang)*rwid, DBMath.sin(ang)*rwid),
						otherang, thisR,angle);
				}
			}

			// determine proper intersection of this and the next segment
			if (next != numSegments)
			{
				Point2D otherPt = points[next+1];
				int otherang = DBMath.figureAngle(nextPt, otherPt);
				if (otherang != angle)
				{
					ang = otherang + LEFTANGLE;
					Point2D newPtL = DBMath.addPoints(nextPt, DBMath.cos(ang)*lwid, DBMath.sin(ang)*lwid);
					nextL = DBMath.intersect(newPtL, otherang, nextL,angle);
					ang = otherang + RIGHTANGLE;
					Point2D newPtR = DBMath.addPoints(nextPt, DBMath.cos(ang)*rwid, DBMath.sin(ang)*rwid);
					nextR = DBMath.intersect(newPtR, otherang, nextR,angle);
				}
			}

			// fill the polygon
			Point2D [] points = new Point2D.Double[4];
			points[0] = DBMath.addPoints(thisL, xoff, yoff);
			points[1] = DBMath.addPoints(thisR, xoff, yoff);
			points[2] = DBMath.addPoints(nextR, xoff, yoff);
			points[3] = DBMath.addPoints(nextL, xoff, yoff);
			Poly retPoly = new Poly(points);

			// see if the sides of the polygon intersect
//			ang = figureangle(poly->xv[0], poly->yv[0], poly->xv[1], poly->yv[1]);
//			angle = figureangle(poly->xv[2], poly->yv[2], poly->xv[3], poly->yv[3]);
//			if (intersect(poly->xv[0], poly->yv[0], ang, poly->xv[2], poly->yv[2], angle, &x, &y) >= 0)
//			{
//				// lines intersect, see if the point is on one of the lines
//				if (x >= mini(poly->xv[0], poly->xv[1]) && x <= maxi(poly->xv[0], poly->xv[1]) &&
//					y >= mini(poly->yv[0], poly->yv[1]) && y <= maxi(poly->yv[0], poly->yv[1]))
//				{
//					if (abs(x-poly->xv[0])+abs(y-poly->yv[0]) > abs(x-poly->xv[1])+abs(y-poly->yv[1]))
//					{
//						poly->xv[1] = x;   poly->yv[1] = y;
//						poly->xv[2] = poly->xv[3];   poly->yv[2] = poly->yv[3];
//					} else
//					{
//						poly->xv[0] = x;   poly->yv[0] = y;
//					}
//					poly->count = 3;
//				}
//			}

			Technology.NodeLayer primLayer = primLayers[element];
			retPoly.setStyle(primLayer.getStyle());
			retPoly.setLayer(primLayer.getLayer());
			return retPoly;
		}

		/**
		 * Method to describe a port in a transistor that is part of a serpentine path.
		 * The port path is shrunk by "diffInset" in the length and is pushed "diffExtend" from the centerline.
		 * The default width of the transistor is "defWid".
		 * The assumptions about directions are:
		 * Segments have port 1 to the left, and port 3 to the right of the gate trace.
		 * Port 0, the "left-hand" end of the gate, appears at the starting
		 * end of the first trace segment; port 2, the "right-hand" end of the gate,
		 * appears at the end of the last trace segment.  Port 3 is drawn as a
		 * reflection of port 1 around the trace.
		 * The poly ports are extended "polyExtend" beyond the appropriate end of the trace
		 * and are inset by "polyInset" from the polysilicon edge.
		 * The diffusion ports are extended "diffExtend" from the polysilicon edge
		 * and set in "diffInset" from the ends of the trace segment.
		 */
		private Poly fillTransPort(PortProto pp)
		{
			double [] specialValues = theProto.getSpecialValues();
			double diffInset = specialValues[1];
			double diffExtend = specialValues[2];
			double defWid = specialValues[3] + extraScale;
			double polyInset = specialValues[4];
			double polyExtend = specialValues[5];

			// prepare to fill the serpentine transistor port
			double xOff = theNode.getTrueCenterX();
			double yOff = theNode.getTrueCenterY();
			int total = points.length;
			AffineTransform trans = theNode.rotateOut();

			// determine which port is being described
			int which = 0;
			for(Iterator it = theProto.getPorts(); it.hasNext(); )
			{
				PortProto lpp = (PortProto)it.next();
				if (lpp == pp) break;
				which++;
			}

			// ports 0 and 2 are poly (simple)
			if (which == 0)
			{
				Point2D thisPt = new Point2D.Double(points[0].getX(), points[0].getY());
				Point2D nextPt = new Point2D.Double(points[1].getX(), points[1].getY());
				int angle = DBMath.figureAngle(thisPt, nextPt);
				int ang = (angle+1800) % 3600;
				thisPt.setLocation(thisPt.getX() + DBMath.cos(ang) * polyExtend + xOff,
					thisPt.getY() + DBMath.sin(ang) * polyExtend + yOff);

				ang = (angle+LEFTANGLE) % 3600;
				Point2D end1 = new Point2D.Double(thisPt.getX() + DBMath.cos(ang) * (defWid/2-polyInset),
					thisPt.getY() + DBMath.sin(ang) * (defWid/2-polyInset));

				ang = (angle+RIGHTANGLE) % 3600;
				Point2D end2 = new Point2D.Double(thisPt.getX() + DBMath.cos(ang) * (defWid/2-polyInset),
					thisPt.getY() + DBMath.sin(ang) * (defWid/2-polyInset));

				Point2D [] portPoints = new Point2D.Double[2];
				portPoints[0] = end1;
				portPoints[1] = end2;
				trans.transform(portPoints, 0, portPoints, 0, 2);
				Poly retPoly = new Poly(portPoints);
				retPoly.setStyle(Poly.Type.OPENED);
				return retPoly;
			}
			if (which == 2)
			{
				Point2D thisPt = new Point2D.Double(points[total-1].getX(), points[total-1].getY());
				Point2D nextPt = new Point2D.Double(points[total-2].getX(), points[total-2].getY());
				int angle = DBMath.figureAngle(thisPt, nextPt);
				int ang = (angle+1800) % 3600;
				thisPt.setLocation(thisPt.getX() + DBMath.cos(ang) * polyExtend + xOff,
					thisPt.getY() + DBMath.sin(ang) * polyExtend + yOff);

				ang = (angle+LEFTANGLE) % 3600;
				Point2D end1 = new Point2D.Double(thisPt.getX() + DBMath.cos(ang) * (defWid/2-polyInset),
					thisPt.getY() + DBMath.sin(ang) * (defWid/2-polyInset));

				ang = (angle+RIGHTANGLE) % 3600;
				Point2D end2 = new Point2D.Double(thisPt.getX() + DBMath.cos(ang) * (defWid/2-polyInset),
					thisPt.getY() + DBMath.sin(ang) * (defWid/2-polyInset));

				Point2D [] portPoints = new Point2D.Double[2];
				portPoints[0] = end1;
				portPoints[1] = end2;
				trans.transform(portPoints, 0, portPoints, 0, 2);
				Poly retPoly = new Poly(portPoints);
				retPoly.setStyle(Poly.Type.OPENED);
				return retPoly;
			}

			// port 3 is the negated path side of port 1
			if (which == 3)
			{
				diffExtend = -diffExtend;
				defWid = -defWid;
			}

			// extra port on some n-transistors
			if (which == 4) diffExtend = defWid = 0;

			Point2D [] portPoints = new Point2D.Double[total];
			Point2D lastPoint = null;
			int lastAngle = 0;
			for(int nextIndex=1; nextIndex<total; nextIndex++)
			{
				int thisIndex = nextIndex-1;
				Point2D thisPt = new Point2D.Double(points[thisIndex].getX() + xOff, points[thisIndex].getY() + yOff);
				Point2D nextPt = new Point2D.Double(points[nextIndex].getX() + xOff, points[nextIndex].getY() + yOff);
				int angle = DBMath.figureAngle(thisPt, nextPt);

				// determine the points
				if (thisIndex == 0)
				{
					// extend "this" 0 degrees forward
					thisPt.setLocation(thisPt.getX() + DBMath.cos(angle) * diffInset,
						thisPt.getY() + DBMath.sin(angle) * diffInset);
				}
				if (nextIndex == total-1)
				{
					// extend "next" 180 degrees back
					int backAng = (angle+1800) % 3600;
					nextPt.setLocation(nextPt.getX() + DBMath.cos(backAng) * diffInset,
						nextPt.getY() + DBMath.sin(backAng) * diffInset);
				}

				// compute endpoints of line parallel to center line
				int ang = (angle+LEFTANGLE) % 3600;
				double sine = DBMath.sin(ang);
				double cosine = DBMath.cos(ang);
				thisPt.setLocation(thisPt.getX() + cosine * (defWid/2+diffExtend),
					thisPt.getY() + sine * (defWid/2+diffExtend));
				nextPt.setLocation(nextPt.getX() + cosine * (defWid/2+diffExtend),
					nextPt.getY() + sine * (defWid/2+diffExtend));

				if (thisIndex != 0)
				{
					// compute intersection of this and previous line
					thisPt = DBMath.intersect(lastPoint, lastAngle, thisPt, angle);
				}
				portPoints[thisIndex] = thisPt;
				lastPoint = thisPt;
				lastAngle = angle;
				if (nextIndex == total-1)
					portPoints[nextIndex] = nextPt;
			}
			trans.transform(portPoints, 0, portPoints, 0, total);
			Poly retPoly = new Poly(portPoints);
			retPoly.setStyle(Poly.Type.OPENED);
			return retPoly;
		}
	}

	/**
	 * Method to convert old primitive node names to their proper NodeProtos.
	 * This method is overridden by those technologies that have any special node name conversion issues.
	 * By default, there is nothing to be done, because by the time this
	 * method is called, normal searches have failed.
	 * @param name the unknown node name, read from an old Library.
	 * @return the proper PrimitiveNode to use for this name.
	 */
	public PrimitiveNode convertOldNodeName(String name) { return null; }

	/****************************** PORTS ******************************/

	/**
	 * Returns a polygon that describes a particular port on a NodeInst.
	 * @param ni the NodeInst that has the port of interest.
	 * The prototype of this NodeInst must be a PrimitiveNode and not a Cell.
	 * @param pp the PrimitivePort on that NodeInst that is being described.
	 * @return a Poly object that describes this PrimitivePort graphically.
	 */
	public Poly getShapeOfPort(NodeInst ni, PrimitivePort pp)
	{
		return getShapeOfPort(ni, pp, null);
	}

	/**
	 * Returns a polygon that describes a particular port on a NodeInst.
	 * @param ni the NodeInst that has the port of interest.
	 * The prototype of this NodeInst must be a PrimitiveNode and not a Cell.
	 * @param pp the PrimitivePort on that NodeInst that is being described.
	 * @param selectPt if not null, it requests a new location on the port,
	 * away from existing arcs, and close to this point.
	 * This is useful for "area" ports such as the left side of AND and OR gates.
	 * @return a Poly object that describes this PrimitivePort graphically.
	 */
	public Poly getShapeOfPort(NodeInst ni, PrimitivePort pp, Point2D selectPt)
	{
		PrimitiveNode np = (PrimitiveNode)ni.getProto();
		if (np.getSpecialType() == PrimitiveNode.SERPTRANS)
		{
			// serpentine transistors use a more complex port determination
			SerpentineTrans std = new SerpentineTrans(ni);
			if (std.hasValidData())
				return std.fillTransPort(pp);
		}

		// standard port determination, see if there is outline information
		if (np.isHoldsOutline())
		{
			// outline may determine the port
			Point2D [] outline = ni.getTrace();
			if (outline != null)
			{
				double cX = ni.getTrueCenterX();
				double cY = ni.getTrueCenterY();
				Point2D [] pointList = new Point2D.Double[outline.length];
				for(int i=0; i<outline.length; i++)
				{
					pointList[i] = new Point2D.Double(cX + outline[i].getX(), cY + outline[i].getY());
				}
				Poly portPoly = new Poly(pointList);
				if (ni.getFunction() == NodeProto.Function.NODE)
				{
					portPoly.setStyle(Poly.Type.FILLED);
				} else
				{
					portPoly.setStyle(Poly.Type.OPENED);
				}
				portPoly.setTextDescriptor(pp.getTextDescriptor());
				return portPoly;
			}
		}

		// standard port computation
		double portLowX = ni.getTrueCenterX() + pp.getLeft().getMultiplier() * ni.getXSize() + pp.getLeft().getAdder();
		double portHighX = ni.getTrueCenterX() + pp.getRight().getMultiplier() * ni.getXSize() + pp.getRight().getAdder();
		double portLowY = ni.getTrueCenterY() + pp.getBottom().getMultiplier() * ni.getYSize() + pp.getBottom().getAdder();
		double portHighY = ni.getTrueCenterY() + pp.getTop().getMultiplier() * ni.getYSize() + pp.getTop().getAdder();
		double portX = (portLowX + portHighX) / 2;
		double portY = (portLowY + portHighY) / 2;
		Poly portPoly = new Poly(portX, portY, portHighX-portLowX, portHighY-portLowY);
		portPoly.setStyle(Poly.Type.FILLED);
		portPoly.setTextDescriptor(pp.getTextDescriptor());
		return portPoly;
	}

	/**
	 * Method to convert old primitive port names to their proper PortProtos.
	 * This method is overridden by those technologies that have any special port name conversion issues.
	 * By default, there is little to be done, because by the time this
	 * method is called, normal searches have failed.
	 * @param portName the unknown port name, read from an old Library.
	 * @param np the PrimitiveNode on which this port resides.
	 * @return the proper PrimitivePort to use for this name.
	 */
	public PrimitivePort convertOldPortName(String portName, PrimitiveNode np)
	{
		// some technologies switched from ports ending in "-bot" to the ending "-bottom"
		int len = portName.length() - 4;
		if (len > 0 && portName.substring(len).equals("-bot"))
		{
			PrimitivePort pp = (PrimitivePort)np.findPortProto(portName + "tom");
			if (pp != null) return pp;
		}
		return null;
	}

	/****************************** PARASITICS ******************************/

	private Pref getParasiticPref(String what, Pref pref, double factory)
	{
		if (pref == null)
		{
			pref = Pref.makeDoublePref("Mininum" + what + "IN" + getTechName(), prefs, factory);
			pref.attachToObject(this, "Tools/Spice tab", getTechShortName() + " Min. " + what);
		}
		return pref;
	}

	/**
	 * Method to return the Pref object associated with all Technologies.
	 * The Pref object is used to save option information.
	 * Since preferences are organized by package, there is only one for
	 * the technologies (they are all in the same package).
	 * @return the Pref object associated with all Technologies.
	 */
	public static Preferences getTechnologyPreferences() { return prefs; }

	/**
	 * Returns the minimum resistance of this Technology.
	 * @return the minimum resistance of this Technology.
	 */
	public double getMinResistance()
	{
		prefMinResistance = getParasiticPref("Resistance", prefMinResistance, minResistance);
		return prefMinResistance.getDouble();
	}

	/**
	 * Sets the minimum resistance of this Technology.
	 * @param minResistance the minimum resistance of this Technology.
	 */
	public void setMinResistance(double minResistance)
	{
		prefMinResistance = getParasiticPref("Resistance", prefMinResistance, this.minResistance);
		prefMinResistance.setDouble(minResistance);
	}

	/**
	 * Returns the minimum capacitance of this Technology.
	 * @return the minimum capacitance of this Technology.
	 */
	public double getMinCapacitance()
	{
		prefMinCapacitance = getParasiticPref("Capacitance", prefMinCapacitance, minCapacitance);
		return prefMinCapacitance.getDouble();
	}

	/**
	 * Sets the minimum capacitance of this Technology.
	 * @param minCapacitance the minimum capacitance of this Technology.
	 */
	public void setMinCapacitance(double minCapacitance)
	{
		prefMinCapacitance = getParasiticPref("Capacitance", prefMinCapacitance, this.minCapacitance);
		prefMinCapacitance.setDouble(minCapacitance);
	}

	/**
	 * Method to set default parasitic values on this Technology.
	 * These values are not saved in the options.
	 * @param minResistance the minimum resistance in this Technology.
	 * @param minCapacitance the minimum capacitance in this Technology.
	 */
	public void setFactoryParasitics(double minResistance, double minCapacitance)
	{
		prefMinResistance = getParasiticPref("Resistance", prefMinResistance, this.minResistance = minResistance);
		prefMinCapacitance = getParasiticPref("Capacitance", prefMinCapacitance, this.minCapacitance = minCapacitance);
	}

	/**
	 * Method to return the level-1 header cards for SPICE in this Technology.
	 * The default is [""].
	 * @return the level-1 header cards for SPICE in this Technology.
	 */
	public String [] getSpiceHeaderLevel1() { return spiceHeaderLevel1; }

	/**
	 * Method to set the level-1 header cards for SPICE in this Technology.
	 * @param lines the level-1 header cards for SPICE in this Technology.
	 */
	public void setSpiceHeaderLevel1(String [] lines) { spiceHeaderLevel1 = lines; }

	/**
	 * Method to return the level-2 header cards for SPICE in this Technology.
	 * The default is [""].
	 * @return the level-2 header cards for SPICE in this Technology.
	 */
	public String [] getSpiceHeaderLevel2() { return spiceHeaderLevel2; }

	/**
	 * Method to set the level-2 header cards for SPICE in this Technology.
	 * @param lines the level-2 header cards for SPICE in this Technology.
	 */
	public void setSpiceHeaderLevel2(String [] lines) { spiceHeaderLevel2 = lines; }

	/**
	 * Method to return the level-3 header cards for SPICE in this Technology.
	 * The default is [""].
	 * @return the level-3 header cards for SPICE in this Technology.
	 */
	public String [] getSpiceHeaderLevel3() { return spiceHeaderLevel3; }

	/**
	 * Method to set the level-3 header cards for SPICE in this Technology.
	 * @param lines the level-3 header cards for SPICE in this Technology.
	 */
	public void setSpiceHeaderLevel3(String [] lines) { spiceHeaderLevel3 = lines; }

	/****************************** MISCELANEOUS ******************************/

	/**
	 * Sets the technology to be "non-electrical".
	 * Users should never call this method.
	 * It is set once by the technology during initialization.
	 * Examples of non-electrical technologies are "Artwork" and "Gem".
	 */
	protected void setNonElectrical() { userBits |= NONELECTRICAL; }

	/**
	 * Returns true if this technology is "non-electrical".
	 * @return true if this technology is "non-electrical".
	 * Examples of non-electrical technologies are "Artwork" and "Gem".
	 */
	public boolean isNonElectrical() { return (userBits & NONELECTRICAL) != 0; }

	/**
	 * Sets the technology to be non-standard.
	 * Users should never call this method.
	 * It is set once by the technology during initialization.
	 * A non-standard technology cannot be edited in the technology editor.
	 * Examples are Schematics and Artwork, which have more complex graphics.
	 */
	protected void setNonStandard() { userBits |= NONSTANDARD; }

	/**
	 * Returns true if this technology is non-standard.
	 * @return true if this technology is non-standard.
	 * A non-standard technology cannot be edited in the technology editor.
	 * Examples are Schematics and Artwork, which have more complex graphics.
	 */
	public boolean isNonStandard() { return (userBits & NONSTANDARD) != 0; }

	/**
	 * Sets the technology to be "static".
	 * Users should never call this method.
	 * It is set once by the technology during initialization.
	 * Static technologies are the core set of technologies in Electric that are
	 * essential, and cannot be deleted.
	 * The technology-editor can create others later, and they can be deleted.
	 */
	protected void setStaticTechnology() { userBits |= NONSTANDARD; }

	/**
	 * Returns true if this technoology is "static" (cannot be deleted).
	 * @return true if this technoology is "static" (cannot be deleted).
	 * Static technologies are the core set of technologies in Electric that are
	 * essential, and cannot be deleted.
	 * The technology-editor can create others later, and they can be deleted.
	 */
	public boolean isStaticTechnology() { return (userBits & NONSTANDARD) != 0; }

	/**
	 * Returns the name of this technology.
	 * Each technology has a unique name, such as "mocmos" (MOSIS CMOS).
	 * @return the name of this technology.
	 */
	public String getTechName() { return techName; }

	/**
	 * Sets the name of this technology.
	 * Technology names must be unique.
	 */
	protected void setTechName(String techName) { this.techName = techName; }

	/**
	 * Returns the short name of this technology.
	 * The short name is user readable ("MOSIS CMOS" instead of "mocmos")
	 * but is shorter than the "description" which often includes options.
	 * @return the short name of this technology.
	 */
	public String getTechShortName() { return techShortName; }

	/**
	 * Sets the short name of this technology.
	 * The short name is user readable ("MOSIS CMOS" instead of "mocmos")
	 * but is shorter than the "description" which often includes options.
	 * @param techShortName the short name for this technology.
	 */
	protected void setTechShortName(String techShortName) { this.techShortName = techShortName; }

	/**
	 * Returns the full description of this Technology.
	 * Full descriptions go beyond the one-word technology name by including such
	 * information as foundry, nuumber of available layers, and process specifics.
	 * For example, "Complementary MOS (from MOSIS, Submicron, 2-6 metals [4], double poly)".
	 * @return the full description of this Technology.
	 */
	public String getTechDesc() { return techDesc; }

	/**
	 * Sets the full description of this Technology.
	 * Full descriptions go beyond the one-word technology name by including such
	 * information as foundry, nuumber of available layers, and process specifics.
	 * For example, "Complementary MOS (from MOSIS, Submicron, 2-6 metals [4], double poly)".
	 */
	public void setTechDesc(String techDesc) { this.techDesc = techDesc; }

	/**
	 * Returns the scale for this Technology.
	 * The technology's scale is for manufacturing output, which must convert
	 * the unit-based values in Electric to real-world values (in nanometers).
	 * @return the scale for this Technology.
	 */
	public double getScale()
	{
		return prefScale.getDouble();
	}

	/**
	 * Method to obtain the Variable name for scaling this Technology.
	 * Do not use this for arbitrary use.
	 * The method exists so that ELIB readers can handle the unusual location
	 * of scale information in the ELIB files.
	 * @return the Variable name for scaling this Technology.
	 */
	public String getScaleVariableName()
	{
		return "ScaleFOR" + getTechName();
	}

	/**
	 * Sets the factory scale of this technology.
	 * The technology's scale is for manufacturing output, which must convert
	 * the unit-based values in Electric to real-world values (in nanometers).
	 * @param factory the factory scale between this technology and the real units.
	 */
	protected void setFactoryScale(double factory, boolean scaleRelevant)
	{
		this.scaleRelevant = scaleRelevant;
		prefScale = Pref.makeDoublePref(getScaleVariableName(), prefs, factory);
		Pref.Meaning meaning = prefScale.attachToObject(this, "Technology/Scale tab", getTechShortName() + " scale");
		meaning.setValidOption(isScaleRelevant());
	}

	/**
	 * Sets the scale of this technology.
	 * The technology's scale is for manufacturing output, which must convert
	 * the unit-based values in Electric to real-world values (in nanometers).
	 * @param scale the new scale between this technology and the real units.
	 */
	public void setScale(double scale)
	{
		if (scale == 0) return;
		prefScale.setDouble(scale);
	}

	/**
	 * Method to tell whether scaling is relevant for this Technology.
	 * Most technolgies produce drawings that are exact images of a final product.
	 * For these technologies (CMOS, bipolar, etc.) the "scale" from displayed grid
	 * units to actual dimensions is a relevant factor.
	 * Other technologies, such as schematics, artwork, and generic,
	 * are not converted to physical objects, and "scale" is not relevant no meaning for them.
	 * @return true if scaling is relevant for this Technology.
	 */
	public boolean isScaleRelevant() { return scaleRelevant; }

	/**
	 * Sets the color map for transparent layers in this technology.
	 * Users should never call this method.
	 * It is set once by the technology during initialization.
	 * @param layers is an array of colors, one per transparent layer.
	 * This is expanded to a map that is 2 to the power "getNumTransparentLayers()".
	 * Color merging is computed automatically.
	 */
	protected void setFactoryTransparentLayers(Color [] layers)
	{
		// pull these values from preferences
		transparentLayers = layers.length;
		transparentColorPrefs = new Pref[transparentLayers];
		for(int i=0; i<layers.length; i++)
		{
			transparentColorPrefs[i] = Pref.makeIntPref("TransparentLayer"+(i+1)+"For"+techName, prefs, layers[i].getRGB());
			layers[i] = new Color(transparentColorPrefs[i].getInt());
		}
		setColorMapFromLayers(layers);
	}

	/**
	 * Returns the number of transparent layers in this technology.
	 * Informs the display system of the number of overlapping or transparent layers
	 * in use.
	 * @return the number of transparent layers in this technology.
	 * There may be 0 transparent layers in technologies that don't do overlapping,
	 * such as Schematics.
	 */
	public int getNumTransparentLayers() { return transparentLayers; }

	/**
	 * Sets the color map for transparent layers in this technology.
	 * @param map the color map for transparent layers in this technology.
	 * There must be a number of entries in this map equal to 2 to the power "getNumTransparentLayers()".
	 */
	public void setColorMap(Color [] map)
	{
		colorMap = map;
	}

	/**
	 * Method to normalize a color stored in a 3-long array.
	 * @param a the array of 3 doubles that holds the color.
	 * All values range from 0 to 1.
	 * The values are adjusted so that they are normalized.
	 */
	private void normalizeColor(double [] a)
	{
		double mag = Math.sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2]);
		if (mag > 1.0e-11f)
		{
			a[0] /= mag;
			a[1] /= mag;
			a[2] /= mag;
		}
	}

	/**
	 * Sets the color map from transparent layers in this technology.
	 * @param layers an array of colors, one per transparent layer.
	 * This is expanded to a map that is 2 to the power "getNumTransparentLayers()".
	 * Color merging is computed automatically.
	 */
	public void setColorMapFromLayers(Color [] layers)
	{
		// update preferences
		if (transparentColorPrefs != null)
		{
			for(int i=0; i<layers.length; i++)
			{
				Pref pref = transparentColorPrefs[i];
				pref.setInt(layers[i].getRGB());
			}
		}

		int numEntries = 1 << transparentLayers;
		Color [] map = new Color[numEntries];
		for(int i=0; i<numEntries; i++)
		{
			int r=200, g=200, b=200;
			boolean hasPrevious = false;
			for(int j=0; j<transparentLayers; j++)
			{
				if ((i & (1<<j)) == 0) continue;
				if (hasPrevious)
				{
					// get the previous color
					double [] lastColor = new double[3];
					lastColor[0] = r / 255.0;
					lastColor[1] = g / 255.0;
					lastColor[2] = b / 255.0;
					normalizeColor(lastColor);

//					// dim the previous color
//					for(int k=0; k<3; k++) lastColor[k] *= 0.5;

					// get the current color
					double [] curColor = new double[3];
					curColor[0] = layers[j].getRed() / 255.0;
					curColor[1] = layers[j].getGreen() / 255.0;
					curColor[2] = layers[j].getBlue() / 255.0;
					normalizeColor(curColor);

					// combine them
					for(int k=0; k<3; k++) curColor[k] += lastColor[k];
					normalizeColor(curColor);
					r = (int)(curColor[0] * 255.0);
					g = (int)(curColor[1] * 255.0);
					b = (int)(curColor[2] * 255.0);
				} else
				{
					r = layers[j].getRed();
					g = layers[j].getGreen();
					b = layers[j].getBlue();
					hasPrevious = true;
				}
			}
			map[i] = new Color(r, g, b);
		}
		setColorMap(map);
	}

	/**
	 * Method to get the factory design rules.
	 * Individual technologies subclass this to create their own rules.
	 * @return the design rules for this Technology.
	 * Returns null if there are no design rules in this Technology.
	 */
	public DRCRules getFactoryDesignRules()
	{
		return null;
	}

	/**
	 * Method to compare a Rules set with the "factory" set and construct an override string.
	 * @param origRules
	 * @param newRules
	 * @return a StringBuffer that describes any overrides.  Returns "" if there are none.
	 */
	public static StringBuffer getRuleDifferences(DRCRules origRules, DRCRules newRules)
	{
		return (new StringBuffer(""));
	}

	/**
	 * Method to be called from DRC:setRules
	 * @param newRules
	 */
	public void setRuleVariables(DRCRules newRules) {;}

	/**
	 * Method to create a set of Design Rules from some simple spacing arrays.
	 * Used by simpler technologies that do not have full-sets of design rules.
	 * @param conDist an upper-diagonal array of layer-to-layer distances (when connected).
	 * @param unConDist an upper-diagonal array of layer-to-layer distances (when unconnected).
	 * @return a set of design rules for the Technology.
	 */
	public static DRCRules makeSimpleRules(double [] conDist, double [] unConDist)
	{
		return null;
	}

	/**
	 * Returns the color map for transparent layers in this technology.
	 * @return the color map for transparent layers in this technology.
	 * The number of entries in this map equals 2 to the power "getNumTransparentLayers()".
	 */
	public Color [] getColorMap() { return colorMap; }

	/**
	 * Returns the 0-based index of this Technology.
	 * Each Technology has a unique index that can be used for array lookup.
	 * @return the index of this Technology.
	 */
	public int getIndex() { return techIndex; }

	/**
	 * Method to determine whether a new technology with the given name would be legal.
	 * All technology names must be unique, so the name cannot already be in use.
	 * @param techName the name of the new technology that will be created.
	 * @return true if the name is valid.
	 */
	private static boolean validTechnology(String techName)
	{
		if (Technology.findTechnology(techName) != null)
		{
			System.out.println("ERROR: Multiple technologies named " + techName);
			return false;
		}
		return true;
	}

//	/**
//	 * Method to write a description of this Technology.
//	 * Displays the description in the Messages Window.
//	 */
//	public void getInfo()
//	{
//		System.out.println(" Name: " + techName);
//		System.out.println(" Description: " + techDesc);
//		System.out.println(" Nodes (" + nodes.size() + ")");
//		for (int i = 0; i < nodes.size(); i++)
//		{
//			System.out.println("     " + nodes.get(i));
//		}
//		System.out.println(" Arcs (" + arcs.size() + ")");
//		for (int i = 0; i < arcs.size(); i++)
//		{
//			System.out.println("     " + arcs.get(i));
//		}
//		super.getInfo();
//	}

	/**
	 * Method to determine the appropriate Technology to use for a Cell.
	 * @param cell the Cell to examine.
	 * @return the Technology for that cell.
	 */
	public static Technology whatTechnology(NodeProto cell)
	{
		Technology tech = whatTechnology(cell, null, 0, 0, null, 0, 0);
		return tech;
	}

	/**
	 * Method to determine the appropriate technology to use for a cell.
	 * The contents of the cell can be defined by the lists of NodeInsts and ArcInsts, or
	 * if they are null, then by the contents of the Cell.
	 * @param cellOrPrim the Cell to examine.
	 * @param nodeProtoList the list of prototypes of NodeInsts in the Cell.
	 * @param startNodeProto the starting point in the "nodeProtoList" array.
	 * @param endNodeProto the ending point in the "nodeProtoList" array.
	 * @param arcProtoList the list of prototypes of ArcInsts in the Cell.
	 * @param startArcProto the starting point in the "arcProtoList" array.
	 * @param endArcProto the ending point in the "arcProtoList" array.
	 * @return the Technology for that cell.
	 */
	public static Technology whatTechnology(NodeProto cellOrPrim, NodeProto [] nodeProtoList, int startNodeProto, int endNodeProto,
		ArcProto [] arcProtoList, int startArcProto, int endArcProto)
	{
		// primitives know their technology
		if (cellOrPrim instanceof PrimitiveNode)
			return(((PrimitiveNode)cellOrPrim).getTechnology());
		Cell cell = (Cell)cellOrPrim;

		// count the number of technologies
		int maxTech = 0;
		for(Iterator it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();
			if (tech.getIndex() > maxTech) maxTech = tech.getIndex();
		}
		maxTech++;

		// create an array of counts for each technology
		int [] useCount = new int[maxTech];
		for(int i=0; i<maxTech; i++) useCount[i] = 0;

		// count technologies of all primitive nodes in the cell
		if (nodeProtoList != null)
		{
			// iterate over the NodeProtos in the list
			for(int i=startNodeProto; i<endNodeProto; i++)
			{
				NodeProto np = nodeProtoList[i];
				if (np == null) continue;
				Technology nodeTech = np.getTechnology();
				if (np instanceof Cell)
				{
					Cell subCell = (Cell)np;
					if (subCell.getView() == View.ICON)
						nodeTech = Schematics.tech;
				}
				if (nodeTech != null) useCount[nodeTech.getIndex()]++;
			}
		} else
		{
			for(Iterator it = cell.getNodes(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				NodeProto np = ni.getProto();
				Technology nodeTech = np.getTechnology();
				if (np instanceof Cell)
				{
					Cell subCell = (Cell)np;
					if (subCell.getView() == View.ICON)
						nodeTech = Schematics.tech;
				}
				if (nodeTech != null) useCount[nodeTech.getIndex()]++;
			}
		}

		// count technologies of all arcs in the cell
		if (arcProtoList != null)
		{
			// iterate over the arcprotos in the list
			for(int i=startArcProto; i<endArcProto; i++)
			{
				ArcProto ap = arcProtoList[i];
				if (ap == null) continue;
				useCount[ap.getTechnology().getIndex()]++;
			}
		} else
		{
			for(Iterator it = cell.getArcs(); it.hasNext(); )
			{
				ArcInst ai = (ArcInst)it.next();
				ArcProto ap = ai.getProto();
				useCount[ap.getTechnology().getIndex()]++;
			}
		}

		// find a concensus
		int best = 0;         Technology bestTech = null;
		int bestLayout = 0;   Technology bestLayoutTech = null;
		for(Iterator it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();

			// always ignore the generic technology
			if (tech == Generic.tech) continue;

			// find the most popular of ALL technologies
			if (useCount[tech.getIndex()] > best)
			{
				best = useCount[tech.getIndex()];
				bestTech = tech;
			}

			// find the most popular of the layout technologies
			if (tech == Schematics.tech || tech == Artwork.tech) continue;
			if (useCount[tech.getIndex()] > bestLayout)
			{
				bestLayout = useCount[tech.getIndex()];
				bestLayoutTech = tech;
			}
		}

		Technology retTech = null;
		if (cell.getView() == View.ICON)
		{
			// in icons, if there is any artwork, use it
			if (useCount[Artwork.tech.getIndex()] > 0) return(Artwork.tech);

			// in icons, if there is nothing, presume artwork
			if (bestTech == null) return(Artwork.tech);

			// use artwork as a default
			retTech = Artwork.tech;
		} else if (cell.getView() == View.SCHEMATIC || cell.getView().isMultiPageView())
		{
			// in schematic, if there are any schematic components, use it
			if (useCount[Schematics.tech.getIndex()] > 0) return(Schematics.tech);

			// in schematic, if there is nothing, presume schematic
			if (bestTech == null) return(Schematics.tech);

			// use schematic as a default
			retTech = Schematics.tech;
		} else
		{
			// use the current layout technology as the default
			retTech = curLayoutTech;
		}

		// if a layout technology was voted the most, return it
		if (bestLayoutTech != null) retTech = bestLayoutTech; else
		{
			// if any technology was voted the most, return it
			if (bestTech != null) retTech = bestTech; else
			{
//				// if this is an icon, presume the technology of its contents
//				cv = contentsview(cell);
//				if (cv != NONODEPROTO)
//				{
//					if (cv->tech == NOTECHNOLOGY)
//						cv->tech = whattech(cv);
//					retTech = cv->tech;
//				} else
//				{
//					// look at the contents of the sub-cells
//					foundicons = FALSE;
//					for(ni = cell->firstnodeinst; ni != NONODEINST; ni = ni->nextnodeinst)
//					{
//						np = ni->proto;
//						if (np == NONODEPROTO) continue;
//						if (np->primindex != 0) continue;
//
//						// ignore recursive references (showing icon in contents)
//						if (isiconof(np, cell)) continue;
//
//						// see if the cell has an icon
//						if (np->cellview == el_iconview) foundicons = TRUE;
//
//						// do not follow into another library
//						if (np->lib != cell->lib) continue;
//						onp = contentsview(np);
//						if (onp != NONODEPROTO) np = onp;
//						tech = whattech(np);
//						if (tech == gen_tech) continue;
//						retTech = tech;
//						break;
//					}
//					if (ni == NONODEINST)
//					{
//						// could not find instances that give information: were there icons?
//						if (foundicons) retTech = sch_tech;
//					}
//				}
			}
		}

		// give up and report the generic technology
//System.out.println("Cell "+cell.describe()+" is "+retTech.getTechName()+" bestlayout="+bestLayoutTech+" best="+bestTech);
//if (retTech == Artwork.tech)
//{
//	if (nodeProtoList != null)
//	{
//		// iterate over the NodeProtos in the list
//		for(int i=startNodeProto; i<endNodeProto; i++)
//		{
//			NodeProto np = nodeProtoList[i];
//			if (np == null) continue;
//			Technology nodeTech = np.getTechnology();
//			if (np instanceof Cell)
//			{
//				Cell subCell = (Cell)np;
//				if (subCell.getView() == View.ICON)
//					nodeTech = Schematics.tech;
//			}
//System.out.println("  has node "+np.describe()+" which is technology "+nodeTech);
//		}
//	} else
//	{
//		for(Iterator it = ((Cell)cell).getNodes(); it.hasNext(); )
//		{
//			NodeInst ni = (NodeInst)it.next();
//			NodeProto np = ni.getProto();
//			Technology nodeTech = np.getTechnology();
//			if (np instanceof Cell)
//			{
//				Cell subCell = (Cell)np;
//				if (subCell.getView() == View.ICON)
//					nodeTech = Schematics.tech;
//			}
//			System.out.println("  has nodeinst "+ni.describe()+" which is technology "+nodeTech);
//		}
//	}
//}
		return retTech;
	}

	/**
	 * Returns a printable version of this Technology.
	 * @return a printable version of this Technology.
	 */
	public String toString()
	{
		return "Technology " + techName;
	}

	///////////////////// Generic methods //////////////////////////////////////////////////////////////
	/**
	 * Method to set the surround distance of layer "outerlayer" from layer "innerlayer"
	 * in node "nty" to "surround".  The array "minsize" is the minimum size of each layer.
	 */
	protected void setLayerSurroundLayer(PrimitiveNode nty, Layer outerLayer, Layer innerLayer,
	                                     double surround, double minSizeValue)
	{
		// find the inner layer
		Technology.NodeLayer inLayer = nty.findNodeLayer(innerLayer);
		if (inLayer == null)
		{
			System.out.println("Internal error in " + getTechDesc() + " surround computation. Layer '" +
                    innerLayer.getName() + "' is not valid in '" + nty.getName() + "'");
			return;
		}

		// find the outer layer
		Technology.NodeLayer outLayer = nty.findNodeLayer(outerLayer);
		if (outLayer == null)
		{
            System.out.println("Internal error in " + getTechDesc() + " surround computation. Layer '" +
                    outerLayer.getName() + "' is not valid in '" + nty.getName() + "'");
			return;
		}

		// determine if minimum size design rules are met
		TechPoint [] inPoints = inLayer.getPoints();
		EdgeH inLeft = inPoints[0].getX();
		EdgeH inRight = inPoints[1].getX();
		EdgeV inBottom = inPoints[0].getY();
		EdgeV inTop = inPoints[1].getY();
		double leftIndent = inLeft.getAdder() - surround;
		double rightIndent = inRight.getAdder() + surround;
		double bottomIndent = inBottom.getAdder() - surround;
		double topIndent = inTop.getAdder() + surround;
		double xSize = nty.getDefWidth() - leftIndent - rightIndent;
		double ySize = nty.getDefHeight() - bottomIndent - topIndent;
		//int outerLayerIndex = outerLayer.getIndex();
		//double minSizeValue = minSize[outerLayerIndex].doubleValue();
        //double minSizeValue = minSize[outerLayerIndex].doubleValue();
		if (xSize < minSizeValue || ySize < minSizeValue)
		{
			// make it irregular to force the proper minimum size
			if (xSize < minSizeValue) rightIndent -= minSizeValue - xSize;
			if (ySize < minSizeValue) topIndent -= minSizeValue - ySize;
		}

		TechPoint [] outPoints = outLayer.getPoints();
		EdgeH outLeft = outPoints[0].getX();
		EdgeH outRight = outPoints[1].getX();
		EdgeV outBottom = outPoints[0].getY();
		EdgeV outTop = outPoints[1].getY();
		boolean hasChanged = false;
		// describe the error
		String errorMessage = "Layer surround error of outer layer '" + outerLayer.getName()
		        + "' and inner layer '" + innerLayer.getName() + "'in '" + getTechDesc() + "':";

        leftIndent = DBMath.round(leftIndent);
        rightIndent = DBMath.round(rightIndent);
        topIndent = DBMath.round(topIndent);
        bottomIndent = DBMath.round(bottomIndent);
		if (!DBMath.areEquals(outLeft.getAdder(), leftIndent))
		{
			outLeft.setAdder(leftIndent);
			hasChanged = true;
			errorMessage += " left=" + leftIndent;
		}
		if (!DBMath.areEquals(outRight.getAdder(), rightIndent))
		{
			outRight.setAdder(rightIndent);
			hasChanged = true;
			errorMessage += " right=" + rightIndent;
		}
		if (!DBMath.areEquals(outTop.getAdder(), topIndent))
		{
			outTop.setAdder(topIndent);
			hasChanged = true;
			errorMessage += " top=" + topIndent;
		}
		if (!DBMath.areEquals(outBottom.getAdder(), bottomIndent))
		{
			outBottom.setAdder(bottomIndent);
			hasChanged = true;
			errorMessage += " bottom=" + bottomIndent;
		}
		if (hasChanged && Main.getDebug()) System.out.println(errorMessage);
	}

	/**
	 * Method to set the surround distance of layer "outerlayer" from layer "innerlayer"
	 * in arc "aty" to "surround".
	 */
	protected void setArcLayerSurroundLayer(PrimitiveArc aty, Layer outerLayer, Layer innerLayer,
	                                        double surround)
	{
		// find the inner layer
		Technology.ArcLayer inLayer = aty.findArcLayer(innerLayer);
		if (inLayer == null)
		{
		    System.out.println("Internal error in " + getTechDesc() + " surround computation. Arc layer '" +
                    inLayer.getLayer().getName() + "' is not valid in '" + aty.getName() + "'");
			return;
		}

		// find the outer layer
		Technology.ArcLayer outLayer = aty.findArcLayer(outerLayer);
		if (outLayer == null)
		{
            System.out.println("Internal error in " + getTechDesc() + " surround computation. Arc layer '" +
                    inLayer.getLayer().getName() + "' is not valid in '" + aty.getName() + "'");
			return;
		}

		// compute the indentation of the outer layer
		double indent = inLayer.getOffset() - surround*2;
		outLayer.setOffset(indent);
	}

	/**
	 * Method to change the design rules for layer "layername" layers so that
	 * the layers are at least "width" wide.  Affects the default arc width
	 * and the default pin size.
	 */
	protected void setLayerMinWidth(String layername, String rulename, double width)
	{
		// find the arc and set its default width
		PrimitiveArc ap = findArcProto(layername);
		if (ap == null) return;

		boolean hasChanged = false;

		hasChanged = (!ap.setDefaultWidth(width + ap.getWidthOffset())) ? hasChanged : true;

		// find the arc's pin and set its size and port offset
		PrimitiveNode np = ap.findPinProto();
		if (np == null) return;
		SizeOffset so = np.getProtoSizeOffset();
		double newWidth = width + so.getLowXOffset() + so.getHighXOffset();
		double newHeight = width + so.getLowYOffset() + so.getHighYOffset();

		hasChanged = (!np.setDefSize(newWidth, newHeight)) ? hasChanged : true;

		PrimitivePort pp = (PrimitivePort)np.getPorts().next();
		EdgeH left = pp.getLeft();
		EdgeH right = pp.getRight();
		EdgeV bottom = pp.getBottom();
		EdgeV top = pp.getTop();
		double indent = newWidth / 2;

		hasChanged = (!left.setAdder(indent)) ? hasChanged : true;
		hasChanged = (!right.setAdder(-indent)) ? hasChanged : true;
		hasChanged = (!top.setAdder(-indent)) ? hasChanged : true;
		hasChanged = (!bottom.setAdder(indent)) ? hasChanged : true;
		if (hasChanged)
		{
			// describe the error
			String errorMessage = "Layer Minimum Size correction of " + indent + " done in '"
					+ layername + ":" + getTechDesc() + "' by rule " + rulename;
			if (Main.getDebug()) System.out.println(errorMessage);
		}
	}

	/**
	* Method to set the true node size (the highlighted area) of node "nodename" to "wid" x "hei".
	*/
	protected void setDefNodeSize(PrimitiveNode nty, double wid, double hei, DRCRules rules)
	{
		//SizeOffset so = nty.getProtoSizeOffset();
		double xindent = (nty.getDefWidth() - wid) / 2;
		double yindent = (nty.getDefHeight() - hei) / 2;
		nty.setSizeOffset(new SizeOffset(xindent, xindent, yindent, yindent));

		int index = 0;
		for(Iterator it = getNodes(); it.hasNext(); )
		{
			PrimitiveNode np = (PrimitiveNode)it.next();
			if (np == nty) break;
			index++;
		}
		rules.setMinNodeSize(index*2, wid);
		rules.setMinNodeSize(index*2+1, hei);
		/*
		rules.minNodeSize[index*2] = new Double(wid);
		rules.minNodeSize[index*2+1] = new Double(hei);
		*/
	}

	/**
	 * Method to set the surround distance of layer "layer" from the via in node "nodename" to "surround".
	 */
	protected void setLayerSurroundVia(PrimitiveNode nty, Layer layer, double surround)
	{
		// find the via size
		double [] specialValues = nty.getSpecialValues();
		double viasize = specialValues[0];
		double layersize = viasize + surround*2;
		double indent = (nty.getDefWidth() - layersize) / 2;

		Technology.NodeLayer oneLayer = nty.findNodeLayer(layer);
		if (oneLayer != null)
		{
			TechPoint [] points = oneLayer.getPoints();
			EdgeH left = points[0].getX();
			EdgeH right = points[1].getX();
			EdgeV bottom = points[0].getY();
			EdgeV top = points[1].getY();
			left.setAdder(indent);
			right.setAdder(-indent);
			top.setAdder(-indent);
			bottom.setAdder(indent);
		}
	}
}
