/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ArcInst.java
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
package com.sun.electric.database.topology;

import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.PrimitiveArc;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * An ArcInst is an instance of an ArcProto (a wire type)
 * An ArcInst points to its prototype, the Cell on which it has been
 * instantiated, and the connection at either end of the wire.
 * The geometry of the wire (width and length) is captured in the
 * bounds of the Geometric portion of this object.
 * <P>
 * ArcInst objects have properties that constrain them.  Here is the notion of "Fixed angle":
 * <P>
 * <CENTER><IMG SRC="doc-files/ArcInst-1.gif"></CENTER>
 * <P>
 * Here is the notion of rigid arcs:
 * <P>
 * <CENTER><IMG SRC="doc-files/ArcInst-2.gif"></CENTER>
 * <P>
 * Here is the notion of slidable arcs:
 * <P>
 * <CENTER><IMG SRC="doc-files/ArcInst-3.gif"></CENTER>
 * <P>
 * Constraints propagate hierarchically:
 * <P>
 * <CENTER><IMG SRC="doc-files/ArcInst-4.gif"></CENTER>
 */
public class ArcInst extends Geometric /*implements Networkable*/
{
	// -------------------------- private data ----------------------------------

	/** fixed-length arc */								private static final int FIXED =                     01;
	/** fixed-angle arc */								private static final int FIXANG =                    02;
	/** arc has text that is far away */				private static final int AHASFARTEXT =               04;
//	/** arc is not in use */							private static final int DEADA =                    020;
	/** angle of arc from end 0 to end 1 */				private static final int AANGLE =                037740;
	/** bits of right shift for AANGLE field */			private static final int AANGLESH =                   5;
	/** set if arc is to be drawn shortened */			private static final int ASHORT =                040000;
	/** set if ends do not extend by half width */		private static final int NOEXTEND =             0400000;
	/** set if ends are negated */						private static final int ISNEGATED =           01000000;
	/** set if arc aims from end 0 to end 1 */			private static final int ISDIRECTIONAL =       02000000;
	/** no extension/negation/arrows on end 0 */		private static final int NOTEND0 =             04000000;
	/** no extension/negation/arrows on end 1 */		private static final int NOTEND1 =            010000000;
	/** reverse extension/negation/arrow ends */		private static final int REVERSEEND =         020000000;
	/** set if arc can't slide around in ports */		private static final int CANTSLIDE =          040000000;
//	/** if on, this arcinst is marked for death */		private static final int KILLA =             0200000000;
//	/** arcinst re-drawing is scheduled */				private static final int REWANTA =           0400000000;
//	/** only local arcinst re-drawing desired */		private static final int RELOCLA =          01000000000;
//	/**transparent arcinst re-draw is done */			private static final int RETDONA =          02000000000;
//	/** opaque arcinst re-draw is done */				private static final int REODONA =          04000000000;
//	/** general flag for spreading and highlighting */	private static final int ARCFLAGBIT =      010000000000;
	/** set if hard to select */						private static final int HARDSELECTA =     020000000000;

	// Name of the variable holding the ArcInst's name.
	private static final String VAR_ARC_NAME = "ARC_name";

	/** flags for this arc instance */					private int userBits;
	/** width of this arc instance */					private double arcWidth;
	/** prototype of this arc instance */				private ArcProto protoType;
	/** end connections of this arc instance */			private Connection head, tail;

	// -------------------- private and protected methods ------------------------

	/**
	 * The constructor is never called.  Use the factory "newInstance" instead.
	 */
	private ArcInst()
	{
		this.userBits = 0;
	}

	/**
	 * Routine to recompute the Geometric information on this ArcInst.
	 */
	void updateGeometric()
	{
		Point2D.Double p1 = head.getLocation();
		Point2D.Double p2 = tail.getLocation();
		double dx = p2.x - p1.x;
		double dy = p2.y - p1.y;
		double len = Math.sqrt(dx * dx + dy * dy);
		this.sY = arcWidth;
		this.sX = len;
		this.cX = (p1.x + p2.x) / 2;
		this.cY = (p1.y + p2.y) / 2;
		this.angle = Math.atan2(dy, dx);
		updateGeometricBounds();
	}

	/**
	 * Routine to return the connection at an end of this ArcInst.
	 * @param onHead true to get get the connection the head of this ArcInst.
	 * false to get get the connection the tail of this ArcInst.
	 */
	public Connection getConnection(boolean onHead)
	{
		return onHead ? head : tail;
	}

	/**
	 * Routine to return the Connection on the head end of this ArcInst.
	 * @return the Connection on the head end of this ArcInst.
	 */
	public Connection getHead() { return head; }

	/**
	 * Routine to return the Connection on the tail end of this ArcInst.
	 * @return the Connection on the tail end of this ArcInst.
	 */
	public Connection getTail() { return tail; }

	// Remove this ArcInst.  Will also remove the connections on either side.
//	public void remove()
//	{
//		head.remove();
//		tail.remove();
//		getParent().removeArc(this);
//		super.remove();
//	}

	/**
	 * Routine to remove the Connection from an end of this ArcInst.
	 * @param c the Connection to remove.
	 * @param onHead true if the Connection is on the head of this ArcInst.
	 */
	void removeConnection(Connection c, boolean onHead)
	{
		/* safety check */
		if ((onHead ? head : tail) != c)
		{
			System.out.println("Tried to remove the wrong connection from a wire end: "
				+ c + " on " + this);
		}
		if (onHead) head = null; else
			tail = null;
	}

	/*
	 * Routine to write a description of this ArcInst.
	 * Displays the description in the Messages Window.
	 */
	public void getInfo()
	{
		System.out.println("--------- ARC INSTANCE: ---------");
		System.out.println(" ArcProto: " + protoType.describe());
		Point2D loc = head.getLocation();
		System.out.println(" Head on " + head.getPortInst().getNodeInst().getProto().describe() +
			" at (" + loc.getX() + "," + loc.getY() + ")");

		loc = tail.getLocation();
		System.out.println(" Tail on " + tail.getPortInst().getNodeInst().getProto().describe() +
			" at (" + loc.getX() + "," + loc.getY() + ")");
		super.getInfo();
	}

	// -------------------------- public methods -----------------------------

	/**
	 * Low-level access routine to create a ArcInst.
	 * @return the newly created ArcInst.
	 */
	public static ArcInst lowLevelAllocate()
	{
		ArcInst ai = new ArcInst();
		return ai;
	}

	/**
	 * Low-level routine to fill-in the ArcInst information.
	 * @param protoType the ArcProto of this ArcInst.
	 * @param arcWidth the width of this ArcInst.
	 * @param headPort the head end PortInst.
	 * @param headX the X coordinate of the head end PortInst.
	 * @param headY the Y coordinate of the head end PortInst.
	 * @param tailPort the tail end PortInst.
	 * @param tailX the X coordinate of the tail end PortInst.
	 * @param tailY the Y coordinate of the tail end PortInst.
	 * @return true on error.
	 */
	public boolean lowLevelPopulate(ArcProto protoType, double arcWidth,
		PortInst headPort, double headX, double headY, PortInst tailPort, double tailX, double tailY)
	{
		// initialize this object
		this.protoType = protoType;

		if (arcWidth <= 0)
			arcWidth = protoType.getWidth();
		this.arcWidth = arcWidth;

		Cell parent = headPort.getNodeInst().getParent();
		if (parent != tailPort.getNodeInst().getParent())
		{
			System.out.println("ArcProto.newInst: the 2 PortInsts are in different Cells!");
			return true;
		}
		this.parent = parent;

		// make sure the arc can connect to these ports
//		PortProto pa = a.getPortProto();
//		PrimitivePort ppa = (PrimitivePort)(pa.getBasePort());
//		if (!ppa.connectsTo(protoType))
//		{
//			System.out.println("Cannot create " + protoType.describe() + " arc in cell " + parent.describe() +
//				" because it cannot connect to port " + pa.getProtoName());
//			return true;
//		}
//		PortProto pb = b.getPortProto();
//		PrimitivePort ppb = (PrimitivePort)(pb.getBasePort());
//		if (!ppb.connectsTo(protoType))
//		{
//			System.out.println("Cannot create " + protoType.describe() + " arc in cell " + parent.describe() +
//				" because it cannot connect to port " + pb.getProtoName());
//			return true;
//		}

		// create node/arc connections and place them properly
		head = new Connection(this, headPort, headX, headY);
		tail = new Connection(this, tailPort, tailX, tailY);
		
		// fill in the geometry
		updateGeometric();
		linkGeom(parent);

		return false;
	}

	/**
	 * Low-level routine to link the ArcInst into its Cell.
	 * @return true on error.
	 */
	public boolean lowLevelLink()
	{
		// attach this arc to the two nodes it connects
		head.getPortInst().getNodeInst().addConnection(head);
		tail.getPortInst().getNodeInst().addConnection(tail);

		// add this arc to the cell
		Cell parent = getParent();
		parent.addArc(this);
		return false;
	}

	/**
	 * Routine to create a new ArcInst connecting two PortInsts.
	 * Since no coordinates are given, the ArcInst connects to the center of the PortInsts.
	 * @param type the prototype of the new ArcInst.
	 * @param width the width of the new ArcInst.  The width must be > 0.
	 * @param head the head end PortInst.
	 * @param tail the tail end PortInst.
	 * @return the newly created ArcInst, or null if there is an error.
	 */
	public static ArcInst newInstance(ArcProto type, double width, PortInst head, PortInst tail)
	{
		Rectangle2D headBounds = head.getBounds();
		double headX = headBounds.getCenterX();
		double headY = headBounds.getCenterY();
		Rectangle2D tailBounds = tail.getBounds();
		double tailX = tailBounds.getCenterX();
		double tailY = tailBounds.getCenterY();
		return newInstance(type, width, head, headX, headY, tail, tailX, tailY);
	}

	/**
	 * Routine to create a new ArcInst connecting two PortInsts at specified locations.
	 * This is more general than the version that does not take coordinates.
	 * @param type the prototype of the new ArcInst.
	 * @param width the width of the new ArcInst.  The width must be > 0.
	 * @param head the head end PortInst.
	 * @param headX the X coordinate of the head end PortInst.
	 * @param headY the Y coordinate of the head end PortInst.
	 * @param tail the tail end PortInst.
	 * @param tailX the X coordinate of the tail end PortInst.
	 * @param tailY the Y coordinate of the tail end PortInst.
	 * @return the newly created ArcInst, or null if there is an error.
	 */
	public static ArcInst newInstance(ArcProto type, double width,
		PortInst head, double headX, double headY, PortInst tail, double tailX, double tailY)
	{
		ArcInst ai = lowLevelAllocate();
		if (ai.lowLevelPopulate(type, width, head, headX, headY, tail, tailX, tailY)) return null;
		if (ai.lowLevelLink()) return null;
		return ai;
	}

	/**
	 * Routine to set this ArcInst to be rigid.
	 * Rigid arcs cannot change length or the angle of their connection to a NodeInst.
	 */
	public void setRigid() { userBits |= FIXED; }

	/**
	 * Routine to set this ArcInst to be not rigid.
	 * Rigid arcs cannot change length or the angle of their connection to a NodeInst.
	 */
	public void clearRigid() { userBits &= ~FIXED; }

	/**
	 * Routine to tell whether this ArcInst is rigid.
	 * Rigid arcs cannot change length or the angle of their connection to a NodeInst.
	 * @return true if this ArcInst is rigid.
	 */
	public boolean isRigid() { return (userBits & FIXED) != 0; }

	/**
	 * Routine to set this ArcInst to be fixed-angle.
	 * Fixed-angle arcs cannot change their angle, so if one end moves,
	 * the other may also adjust to keep the arc angle constant.
	 */
	public void setFixedAngle() { userBits |= FIXANG; }

	/**
	 * Routine to set this ArcInst to be not fixed-angle.
	 * Fixed-angle arcs cannot change their angle, so if one end moves,
	 * the other may also adjust to keep the arc angle constant.
	 */
	public void clearFixedAngle() { userBits &= ~FIXANG; }

	/**
	 * Routine to tell whether this ArcInst is fixed-angle.
	 * Fixed-angle arcs cannot change their angle, so if one end moves,
	 * the other may also adjust to keep the arc angle constant.
	 * @return true if this ArcInst is fixed-angle.
	 */
	public boolean isFixedAngle() { return (userBits & FIXANG) != 0; }

	/**
	 * Routine to set this ArcInst to be slidable.
	 * Arcs that slide will not move their connected NodeInsts if the arc's end is still within the port area.
	 * Arcs that cannot slide will force their NodeInsts to move by the same amount as the arc.
	 * Rigid arcs cannot slide but nonrigid arcs use this state to make a decision.
	 */
	public void setSlidable() { userBits &= ~CANTSLIDE; }

	/**
	 * Routine to set this ArcInst to be not slidable.
	 * Arcs that slide will not move their connected NodeInsts if the arc's end is still within the port area.
	 * Arcs that cannot slide will force their NodeInsts to move by the same amount as the arc.
	 * Rigid arcs cannot slide but nonrigid arcs use this state to make a decision.
	 */
	public void clearSlidable() { userBits |= CANTSLIDE; }

	/**
	 * Routine to tell whether this ArcInst is slidable.
	 * Arcs that slide will not move their connected NodeInsts if the arc's end is still within the port area.
	 * Arcs that cannot slide will force their NodeInsts to move by the same amount as the arc.
	 * Rigid arcs cannot slide but nonrigid arcs use this state to make a decision.
	 * @return true if this ArcInst is slidable.
	 */
	public boolean isSlidable() { return (userBits & CANTSLIDE) == 0; }

	/**
	 * Routine to set this ArcInst to have far-text.
	 * Far text is text that is so far offset from the object that normal searches do not find it.
	 */
	public void setFarText() { userBits |= AHASFARTEXT; }

	/**
	 * Routine to set this ArcInst to not have far-text.
	 * Far text is text that is so far offset from the object that normal searches do not find it.
	 */
	public void clearFarText() { userBits &= ~AHASFARTEXT; }

	/**
	 * Routine to tell whether this ArcInst has far-text.
	 * Far text is text that is so far offset from the object that normal searches do not find it.
	 * @return true if this ArcInst has far-text.
	 */
	public boolean isFarText() { return (userBits & AHASFARTEXT) != 0; }

	/**
	 * Low-level routine to set the ArcInst angle in the "user bits".
	 * This general access to the bits is required because the binary ".elib"
	 * file format stores it this way.
	 * This should not normally be called by any other part of the system.
	 * @param angle the angle of the ArcInst (in degrees).
	 */
	public void lowLevelSetArcAngle(int angle) { userBits = (userBits & ~AANGLE) | (angle << AANGLESH); }

	/**
	 * Low-level routine to get the ArcInst angle from the "user bits".
	 * This general access to the bits is required because the binary ".elib"
	 * file format stores it this way.
	 * This should not normally be called by any other part of the system.
	 * @return the arc angle (in degrees).
	 */
	public int lowLevelGetArcAngle() { return (userBits & AANGLE) >> AANGLESH; }

	/**
	 * Routine to set this ArcInst to be shortened.
	 * Arcs that meet at angles which are not multiples of 90 degrees will have
	 * extra tabs emerging from the connection site if they are not shortened.
	 * Therefore, shortened arcs reduce the amount they extend their ends.
	 */
	public void setShortened() { userBits |= ASHORT; }

	/**
	 * Routine to set this ArcInst to be not shortened.
	 * Arcs that meet at angles which are not multiples of 90 degrees will have
	 * extra tabs emerging from the connection site if they are not shortened.
	 * Therefore, shortened arcs reduce the amount they extend their ends.
	 */
	public void clearShortened() { userBits &= ~ASHORT; }

	/**
	 * Routine to tell whether this ArcInst is shortened.
	 * Arcs that meet at angles which are not multiples of 90 degrees will have
	 * extra tabs emerging from the connection site if they are not shortened.
	 * Therefore, shortened arcs reduce the amount they extend their ends.
	 * @return true if this ArcInst is shortened.
	 */
	public boolean isShortened() { return (userBits & ASHORT) != 0; }

	/**
	 * Routine to set this ArcInst to have its ends extended.
	 * End-extension causes an arc to extend past its endpoint by half of its width.
	 * Most layout arcs want this so that they make clean connections to orthogonal arcs.
	 */
	public void setExtended() { userBits &= ~NOEXTEND; }

	/**
	 * Routine to set this ArcInst to have its ends not extended.
	 * End-extension causes an arc to extend past its endpoint by half of its width.
	 * Most layout arcs want this so that they make clean connections to orthogonal arcs.
	 */
	public void clearExtended() { userBits |= NOEXTEND; }

	/**
	 * Routine to tell whether this ArcInst has its ends extended.
	 * End-extension causes an arc to extend past its endpoint by half of its width.
	 * Most layout arcs want this so that they make clean connections to orthogonal arcs.
	 * @return true if this ArcInst has its ends extended.
	 */
	public boolean isExtended() { return (userBits & NOEXTEND) == 0; }

	/**
	 * Routine to set this ArcInst to be negated.
	 * Negated arcs have a bubble drawn on their tail end to indicate negation.
	 * If the arc is reversed, then the bubble appears on the head.
	 * This is used only in Schematics technologies to place negating bubbles on any node.
	 * @see ArcInst#setReverseEnds
	 */
	public void setNegated() { userBits |= ISNEGATED; }

	/**
	 * Routine to set this ArcInst to be not negated.
	 * Negated arcs have a bubble drawn on their tail end to indicate negation.
	 * If the arc is reversed, then the bubble appears on the head.
	 * This is used only in Schematics technologies to place negating bubbles on any node.
	 * @see ArcInst#setReverseEnds
	 */
	public void clearNegated() { userBits &= ~ISNEGATED; }

	/**
	 * Routine to tell whether this ArcInst is negated.
	 * Negated arcs have a bubble drawn on their tail end to indicate negation.
	 * If the arc is reversed, then the bubble appears on the head.
	 * This is used only in Schematics technologies to place negating bubbles on any node.
	 * @return true if this ArcInst is negated.
	 * @see ArcInst#setReverseEnds
	 */
	public boolean isNegated() { return (userBits & ISNEGATED) != 0; }

	/**
	 * Routine to set this ArcInst to be directional.
	 * Directional arcs have an arrow drawn on them to indicate flow.
	 * The arrow head is on the arc's head end, unless the arc is reversed.
	 * It is only for documentation purposes and does not affect the circuit.
	 * @see ArcInst#setReverseEnds
	 */
	public void setDirectional() { userBits |= ISDIRECTIONAL; }

	/**
	 * Routine to set this ArcInst to be not directional.
	 * Directional arcs have an arrow drawn on them to indicate flow.
	 * The arrow head is on the arc's head end, unless the arc is reversed.
	 * It is only for documentation purposes and does not affect the circuit.
	 * @see ArcInst#setReverseEnds
	 */
	public void clearDirectional() { userBits &= ~ISDIRECTIONAL; }

	/**
	 * Routine to tell whether this ArcInst is directional.
	 * Directional arcs have an arrow drawn on them to indicate flow.
	 * The arrow head is on the arc's head end, unless the arc is reversed.
	 * It is only for documentation purposes and does not affect the circuit.
	 * @return true if this ArcInst is directional.
	 * @see ArcInst#setReverseEnds
	 */
	public boolean isDirectional() { return (userBits & ISDIRECTIONAL) != 0; }

	/**
	 * Routine to set this ArcInst to have its head skipped.
	 * Skipping the head causes any special actions that are normally applied to the
	 * head to be ignored.  For example, the directional arrow is on the arc head,
	 * so skipping the head will remove the arrow-head, but not the body of the arrow.
	 */
	public void setSkipHead() { userBits |= NOTEND0; }

	/**
	 * Routine to set this ArcInst to have its head not skipped.
	 * Skipping the head causes any special actions that are normally applied to the
	 * head to be ignored.  For example, the directional arrow is on the arc head,
	 * so skipping the head will remove the arrow-head, but not the body of the arrow.
	 */
	public void clearSkipHead() { userBits &= ~NOTEND0; }

	/**
	 * Routine to tell whether this ArcInst has its head skipped.
	 * Skipping the head causes any special actions that are normally applied to the
	 * head to be ignored.  For example, the directional arrow is on the arc head,
	 * so skipping the head will remove the arrow-head, but not the body of the arrow.
	 * @return true if this ArcInst has its head skipped.
	 */
	public boolean isSkipHead() { return (userBits & NOTEND0) != 0; }

	/**
	 * Routine to set this ArcInst to have its tail skipped.
	 * Skipping the tail causes any special actions that are normally applied to the
	 * tail to be ignored.  For example, the negating bubble is on the arc tail,
	 * so skipping the tail will remove the bubble.
	 */
	public void setSkipTail() { userBits |= NOTEND1; }

	/**
	 * Routine to set this ArcInst to have its tail not skipped.
	 * Skipping the tail causes any special actions that are normally applied to the
	 * tail to be ignored.  For example, the negating bubble is on the arc tail,
	 * so skipping the tail will remove the bubble.
	 */
	public void clearSkipTail() { userBits &= ~NOTEND1; }

	/**
	 * Routine to tell whether this ArcInst has its tail skipped.
	 * Skipping the tail causes any special actions that are normally applied to the
	 * tail to be ignored.  For example, the negating bubble is on the arc tail,
	 * so skipping the tail will remove the bubble.
	 * @return true if this ArcInst has its tail skipped.
	 */
	public boolean isSkipTail() { return (userBits & NOTEND1) != 0; }

	/**
	 * Routine to reverse the ends of this ArcInst.
	 * A reversed arc switches its head and tail.
	 * This is useful if the negating bubble appears on the wrong end.
	 */
	public void setReverseEnds() { userBits |= REVERSEEND; }

	/**
	 * Routine to restore the proper ends of this ArcInst.
	 * A reversed arc switches its head and tail.
	 * This is useful if the negating bubble appears on the wrong end.
	 */
	public void clearReverseEnds() { userBits &= ~REVERSEEND; }

	/**
	 * Routine to tell whether this ArcInst has been reversed.
	 * A reversed arc switches its head and tail.
	 * This is useful if the negating bubble appears on the wrong end.
	 * @return true if this ArcInst has been reversed.
	 */
	public boolean isReverseEnds() { return (userBits & REVERSEEND) != 0; }

	/**
	 * Routine to set this ArcInst to be hard-to-select.
	 * Hard-to-select ArcInsts cannot be selected by clicking on them.
	 * Instead, the "special select" command must be given.
	 */
	public void setHardSelect() { userBits |= HARDSELECTA; }

	/**
	 * Routine to set this ArcInst to be easy-to-select.
	 * Hard-to-select ArcInsts cannot be selected by clicking on them.
	 * Instead, the "special select" command must be given.
	 */
	public void clearHardSelect() { userBits &= ~HARDSELECTA; }

	/**
	 * Routine to tell whether this ArcInst is hard-to-select.
	 * Hard-to-select ArcInsts cannot be selected by clicking on them.
	 * Instead, the "special select" command must be given.
	 * @return true if this ArcInst is hard-to-select.
	 */
	public boolean isHardSelect() { return (userBits & HARDSELECTA) != 0; }

	/**
	 * Low-level routine to get the user bits.
	 * The "user bits" are a collection of flags that are more sensibly accessed
	 * through special methods.
	 * This general access to the bits is required because the binary ".elib"
	 * file format stores it as a full integer.
	 * This should not normally be called by any other part of the system.
	 * @return the "user bits".
	 */
	public int lowLevelGetUserbits() { return userBits; }

	/**
	 * Low-level routine to set the user bits.
	 * The "user bits" are a collection of flags that are more sensibly accessed
	 * through special methods.
	 * This general access to the bits is required because the binary ".elib"
	 * file format stores it as a full integer.
	 * This should not normally be called by any other part of the system.
	 * @param userBits the new "user bits".
	 */
	public void lowLevelSetUserbits(int userBits) { this.userBits = userBits; }

	/**
	 * Routine to return the width of this ArcInst.
	 * Note that this call excludes material surrounding this ArcInst.
	 * For example, if this is a diffusion ArcInst then return
	 * the width of the diffusion and ignore the width of well and select.
	 * @return the width of this ArcInst.
	 */
	public double getWidth()
	{
		return arcWidth - ((PrimitiveArc)protoType).getWidthOffset();
	}

	/**
	 * Routine to change the width of this ArcInst.
	 * @param dWidth the change to the ArcInst width.
	 */
	public void modify(double dWidth)
	{
		arcWidth += dWidth;
		updateGeometric();
//		Electric.modifyArcInst(getAddr(), dWidth);
	}

	/**
	 * Routine to return the prototype of this ArcInst.
	 * @return the prototype of this ArcInst.
	 */
	public ArcProto getProto()
	{
		return protoType;
	}

	/**
	 * Routine to set the name of this ArcInst.
	 * The name is a local string that can be set by the user.
	 * @param name the new name of this ArcInst.
	 */
	public Variable setName(String name)
	{
		Variable var = setVal(VAR_ARC_NAME, name);
		if (var != null) var.setDisplay();
		return var;
	}

	/**
	 * Routine to return the name of this ArcInst.
	 * The name is a local string that can be set by the user.
	 * @return the name of this ArcInst, null if there is no name.
	 */
	public String getName()
	{
		Variable var = getVal(VAR_ARC_NAME, String.class);
		if (var == null) return null;
		return (String) var.getObject();
	}

	/**
	 * Routine to create a Poly object that describes an ArcInst.
	 * The ArcInst is described by its length, width and style.
	 * @param length the length of the ArcInst.
	 * @param width the width of the ArcInst.
	 * @param style the style of the ArcInst.
	 * @return a Poly that describes the ArcInst.
	 */
	public Poly makePoly(double length, double width, Poly.Type style)
	{
		Point2D.Double end1 = head.getLocation();
		Point2D.Double end2 = tail.getLocation();

		// zero-width polygons are simply lines
		if (width == 0)
		{
			Poly poly = new Poly(new Point2D.Double[]{end1, end2});
			if (style == Poly.Type.FILLED) style = Poly.Type.OPENED;
			poly.setStyle(style);
			return poly;
		}

		// determine the end extension on each end
		double e1 = width/2;
		double e2 = width/2;
		if (!isExtended())
		{
			// nonextension arc: set extension to zero for all included ends
			if (!isSkipTail()) e1 = 0;
			if (!isSkipHead()) e2 = 0;
//		} else if (isShortened())
//		{
//			// shortened arc: compute variable extension
//			e1 = tech_getextendfactor(width, ai->endshrink&0xFFFF);
//			e2 = tech_getextendfactor(width, (ai->endshrink>>16)&0xFFFF);
		}

		// make the polygon
		Poly poly = makeEndPointPoly(length, width, getAngle(), end1, e1, end2, e2);
		if (poly != null) poly.setStyle(style);
		return poly;
	}

	private Poly makeEndPointPoly(double len, double wid, double angle, Point2D end1, double e1,
		Point2D end2, double e2)
	{
		double temp, xextra, yextra, xe1, ye1, xe2, ye2, w2, sa, ca;

		w2 = wid / 2;
		double x1 = end1.getX();   double y1 = end1.getY();
		double x2 = end2.getX();   double y2 = end2.getY();

		/* somewhat simpler if rectangle is manhattan */
		if (angle < 0) angle += Math.PI * 2;
		if (angle == Math.PI/2 || angle == Math.PI/2*3)
		{
			if (y1 > y2)
			{
				temp = y1;   y1 = y2;   y2 = temp;
				temp = e1;   e1 = e2;   e2 = temp;
			}
			Poly poly = new Poly(new Point2D.Double[] {
				new Point2D.Double(x1 - w2, y1 - e1),
				new Point2D.Double(x1 + w2, y1 - e1),
				new Point2D.Double(x2 + w2, y2 + e2),
				new Point2D.Double(x2 - w2, y2 + e2)});
			return poly;
		}
		if (angle == 0 || angle == Math.PI)
		{
			if (x1 > x2)
			{
				temp = x1;   x1 = x2;   x2 = temp;
				temp = e1;   e1 = e2;   e2 = temp;
			}
			Poly poly = new Poly(new Point2D.Double[] {
				new Point2D.Double(x1 - e1, y1 - w2),
				new Point2D.Double(x1 - e1, y1 + w2),
				new Point2D.Double(x2 + e2, y2 + w2),
				new Point2D.Double(x2 + e2, y2 - w2)});
			return poly;
		}

		/* nonmanhattan arcs cannot have zero length so re-compute it */
//		if (len == 0) len = computedistance(x1,y1, x2,y2);
//		if (len == 0)
//		{
//			sa = sine(angle);
//			ca = cosine(angle);
//			xe1 = x1 - mult(ca, e1);
//			ye1 = y1 - mult(sa, e1);
//			xe2 = x2 + mult(ca, e2);
//			ye2 = y2 + mult(sa, e2);
//			xextra = mult(ca, w2);
//			yextra = mult(sa, w2);
//		} else
//		{
//			/* work out all the math for nonmanhattan arcs */
//			xe1 = x1 - muldiv(e1, (x2-x1), len);
//			ye1 = y1 - muldiv(e1, (y2-y1), len);
//			xe2 = x2 + muldiv(e2, (x2-x1), len);
//			ye2 = y2 + muldiv(e2, (y2-y1), len);
//
//			/* now compute the corners */
//			xextra = muldiv(w2, (x2-x1), len);
//			yextra = muldiv(w2, (y2-y1), len);
//		}
//		Poly poly = new Poly(new Point2D.Double[] {
//			new Point2D.Double(yextra + xe1, ye1 - xextra),
//			new Point2D.Double(xe1 - yextra, xextra + ye1),
//			new Point2D.Double(xe2 - yextra, xextra + ye2),
//			new Point2D.Double(yextra + xe2, ye2 - xextra)});
//		return poly;
		return null;
	}

	/**
	 * Routine to describe this ArcInst as a string.
	 * @return a description of this ArcInst.
	 */
	public String describe()
	{
		return protoType.getProtoName();
	}

	/**
	 * Returns a printable version of this ArcInst.
	 * @return a printable version of this ArcInst.
	 */
	public String toString()
	{
		return "ArcInst " + protoType.getProtoName();
	}

}
