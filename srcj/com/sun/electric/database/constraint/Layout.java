/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Layout.java
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
package com.sun.electric.database.constraint;

import com.sun.electric.database.change.Undo;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.FlagSet;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.user.User;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.HashSet;

/**
 * Class to implement the layout-constraint system.
 * Handles the fixed-angle and rigid constraints.
 * Also propagates these constraints up the hierarchy.
 */
public class Layout extends Constraints
{
	private static final Layout layoutConstraint = new Layout();

	private static final boolean DEBUG = false;

	/**
	 * The meaning of changeClock for object modification:
	 *
	 * ai.getChangeClock() <  changeClock-2  unmodified         arcs
	 * ai.getChangeClock() == changeClock-2  unmodified rigid   arcs
	 * ai.getChangeClock() == changeClock-1  unmodified unrigid arcs
	 * ai.getChangeClock() == changeClock      modified rigid   arcs
	 * ai.getChangeClock() == changeClock+1    modified unrigid arcs
	 * ni.getChangeClock() <  changeClock    unmodified         nodes
	 * ni.getChangeClock() == changeClock    modified           nodes
	 */
	private static int changeClock = 10;

	private Layout() {}

	/**
	 * Method to return the current constraint solver.
	 * @return the current constraint solver.
	 */
	public static Layout getConstraint() { return layoutConstraint; }

	/**
	 * Method to start a batch of changes.
	 * @param tool the tool that generated the changes.
	 * @param undoRedo true if these changes are from an undo or redo command.
	 */
	public void startBatch(Tool tool, boolean undoRedo)
	{
		// force every cell to remember its current bounds
		for(Iterator it = Library.getLibraries(); it.hasNext(); )
		{
			Library lib = (Library)it.next();
			for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
			{
				Cell cell = (Cell)cIt.next();
				cell.rememberBounds();
			}
		}
	}

	/**
	 * Method to do hierarchical update on any cells that changed
	 */
	public void endBatch()
	{
		// solve any cells that changed
		List changedCells = new ArrayList();
		for(Iterator it = Undo.ChangeCell.getIterator(); it.hasNext(); )
			changedCells.add(it.next());

		deletedArcs = new HashSet();

		for(Iterator it = changedCells.iterator(); it.hasNext(); )
		{
			Undo.ChangeCell cc = (Undo.ChangeCell)it.next();
			Cell cell = cc.getCell();
			boolean forcedLook = cc.getForcedLook();
			computeCell(cell, forcedLook);
		}

		deletedArcs = null;

		// clear all change objects
		Undo.ChangeBatch curBatch = Undo.getCurrentBatch();
		if (curBatch == null) return;
		for(Iterator it = curBatch.getChanges(); it.hasNext(); )
		{
			Undo.Change c = (Undo.Change)it.next();
			if (c.getType() == Undo.Type.NODEINSTNEW || c.getType() == Undo.Type.NODEINSTKILL ||
				c.getType() == Undo.Type.NODEINSTMOD)
			{
				NodeInst ni = (NodeInst)c.getObject();
				ni.setChange(null);
			} else if (c.getType() == Undo.Type.ARCINSTNEW || c.getType() == Undo.Type.ARCINSTKILL ||
				c.getType() == Undo.Type.ARCINSTMOD)
			{
				ArcInst ai = (ArcInst)c.getObject();
				ai.setChange(null);
			} else if (c.getType() == Undo.Type.EXPORTNEW || c.getType() == Undo.Type.EXPORTKILL ||
				c.getType() == Undo.Type.EXPORTMOD)
			{
				Export pp = (Export)c.getObject();
				pp.setChange(null);
			} else if (c.getType() == Undo.Type.CELLNEW || c.getType() == Undo.Type.CELLKILL ||
				c.getType() == Undo.Type.CELLMOD)
			{
				Cell cell = (Cell)c.getObject();
				cell.setChange(null);
			}
		}
	}

	/**
	 * Method to handle the creation of a new ElectricObject.
	 * @param obj the ElectricObject that was just created.
	 */
	public void newObject(ElectricObject obj)
	{
//		if (obj instanceof Export)
//		{
//			Export pp = (Export)obj;
//			Cell cell = (Cell)pp.getParent();
//			for(Iterator it = cell.getInstancesOf(); it.hasNext(); )
//			{
//				NodeInst ni = (NodeInst)it.next();
//				Undo.redrawObject(ni);
////				ni.modifyInstance(0, 0, 0, 0, 0);
//			}
//		}
	}

	/**
	 * Method to handle the deletion of an Export.
	 * @param pp the Export that was just deleted.
	 * @param oldPortInsts the PortInsts that were on that Export (?).
	 */
	public void killExport(Export pp, Collection oldPortInsts)
	{
// 		Cell cell = (Cell)pp.getParent();
// 		for(Iterator it = cell.getInstancesOf(); it.hasNext(); )
// 		{
// 			NodeInst ni = (NodeInst)it.next();
// 			Undo.redrawObject(ni);
//// 			ni.modifyInstance(0, 0, 0, 0, 0);
// 		}
	}

	/**
	 * Method to handle a new Variable.
	 * @param obj the ElectricObject on which the Variable resides.
	 * @param var the newly created Variable.
	 */
	public void newVariable(ElectricObject obj, Variable var)
	{
//		if (type == VPORTPROTO)
//		{
//			if ((stype&VCREF) != 0)
//			{
//				name = changedvariablename(type, skey, stype);
//				if (estrcmp(name, x_("protoname")) == 0)
//				{
//					pp = (PORTPROTO *)addr;
//					np = pp->parent;
//					for(ni = np->firstinst; ni != NONODEINST; ni = ni->nextinst)
//					{
//						(void)db_change((int)ni, NODEINSTMOD, ni->lowx, ni->lowy,
//							ni->highx, ni->highy, ni->rotation, ni->transpose);
//					}
//				}
//			}
//		}
	}

	/**
	 * Method to set temporary rigidity on an ArcInst.
	 * @param ai the ArcInst to make temporarily rigid/not-rigid.
	 * @param tempRigid true to make the ArcInst temporarily rigid;
	 * false to make it temporarily not-rigid.
	 */
	public static void setTempRigid(ArcInst ai, boolean tempRigid)
	{
		if (tempRigid)
		{
			if (ai.getChangeClock() == changeClock + 2) return;
			ai.setChangeClock(changeClock + 2);
		} else
		{
			if (ai.getChangeClock() == changeClock + 3) return;
			ai.setChangeClock(changeClock + 3);
		}
	}

	private static HashSet deletedArcs;

	/**
	 * Method to handle a change to a NodeInst.
	 * @param ni the NodeInst that was changed.
	 * @param oCX the old X center of the NodeInst.
	 * @param oCY the old Y center of the NodeInst.
	 * @param oSX the old X size of the NodeInst.
	 * @param oSY the old Y size of the NodeInst.
	 * @param oRot the old rotation of the NodeInst.
	 */
	public void modifyNodeInst(NodeInst ni, double dCX, double dCY, double dSX, double dSY, int dRot)
	{
		// advance the change clock
		changeClock += 4;

		// change the nodeinst
		double oldSX = ni.getXSizeWithMirror();
		double oldSY = ni.getYSizeWithMirror();
		if (alterNodeInst(ni, dCX, dCY, dSX, dSY, dRot, false))
			Undo.ChangeCell.forceHierarchicalAnalysis(ni.getParent());

		deletedArcs = new HashSet();

		// change the arcs on the nodeinst
		boolean flipX = (oldSX * ni.getXSizeWithMirror()) < 0;
		boolean flipY = (oldSY * ni.getYSizeWithMirror()) < 0;
		if (modNodeArcs(ni, dRot, dSX, dSY, flipX, flipY))
			Undo.ChangeCell.forceHierarchicalAnalysis(ni.getParent());

		deletedArcs = null;
	}

	/**
	 * Method to handle a change to many NodeInsts at once.
	 * @param nis the NodeInsts that were changed.
	 * @param oCX the old X centers of the NodeInsts.
	 * @param oCY the old Y centers of the NodeInsts.
	 * @param oSX the old X sizes of the NodeInsts.
	 * @param oSY the old Y sizes of the NodeInsts.
	 * @param oRot the old rotations of the NodeInsts.
	 */
	public void modifyNodeInsts(NodeInst [] nis, double [] dCX, double [] dCY, double [] dSX, double [] dSY, int [] dRot)
	{
		// advance the change clock
		changeClock += 4;

		// change the nodeinst
		Cell parent = null;
		boolean [] flipX = new boolean[nis.length];
		boolean [] flipY = new boolean[nis.length];
		for(int i=0; i<nis.length; i++)
		{
			double oldSX = nis[i].getXSizeWithMirror();
			double oldSY = nis[i].getYSizeWithMirror();
			if (alterNodeInst(nis[i], dCX[i], dCY[i], dSX[i], dSY[i], dRot[i], false))
				parent = (Cell)nis[i].getParent();
			flipX[i] = (oldSX * nis[i].getXSizeWithMirror()) < 0;
			flipY[i] = (oldSY * nis[i].getYSizeWithMirror()) < 0;
		}

		deletedArcs = new HashSet();

		// change the arcs on the nodeinst
		for(int i=0; i<nis.length; i++)
		{
			if (modNodeArcs(nis[i], dRot[i], dSX[i], dSY[i], flipX[i], flipY[i]))
				parent = nis[i].getParent();
		}
		if (parent != null)
			Undo.ChangeCell.forceHierarchicalAnalysis(parent);

		deletedArcs = null;
	}

	/******************** NODE MODIFICATION CODE *************************/

	/**
	 * Method to modify nodeinst "ni" by "deltalx" in low X, "deltaly" in low Y,
	 * "deltahx" in high X, "deltahy" in high Y, and "dangle" tenth-degrees.  If
	 * "announce" is true, report "start" and "end" changes on the node.
	 * If the nodeinst is a portproto of the current cell and has any arcs
	 * connected to it, the method returns nonzero to indicate that the outer
	 * cell has ports that moved (the nodeinst has exports).
	 */
	private static boolean alterNodeInst(NodeInst ni, double deltaCX, double deltaCY, double deltaSX,
		double deltaSY, int dAngle, boolean announce)
	{
//		// determine whether this is a position or size change
//		int change = -1;
//		if (deltaSX == 0 && deltaSY == 0)
//		{
//			if (deltaCX != 0 || deltaCY != 0 || dAngle != 0) change = 0;
//		}

		// reject if this change has already been done
		if (ni.getChangeClock() >= changeClock) return false;

		// if simple rotation on transposed nodeinst, reverse rotation
		boolean flipX = ni.isMirroredAboutXAxis();
		if ((ni.getXSizeWithMirror() + deltaSX) * ni.getXSizeWithMirror() < 0) flipX = !flipX;
		boolean flipY = ni.isMirroredAboutYAxis();
		if ((ni.getYSizeWithMirror() + deltaSY) * ni.getYSizeWithMirror() < 0) flipY = !flipY;
		if (flipX ^ flipY) dAngle = (3600 - dAngle) % 3600;

		// make changes to the nodeinst
		int oldang = ni.getAngle();
		double oldCX = ni.getAnchorCenterX();
		double oldCY = ni.getAnchorCenterY();
		double oldSX = ni.getXSizeWithMirror();
		double oldSY = ni.getYSizeWithMirror();
		if (DEBUG) System.out.println("Moving node "+ni.describe()+" [is "+oldSX+"x"+oldSY+" at ("+oldCX+","+oldCY+") rot "+oldang+
			"] change is dx="+deltaCX+" dy="+deltaCY+" dwid="+deltaSX+" dhei="+deltaSY+") drot="+dAngle);
		ni.lowLevelModify(deltaCX, deltaCY, deltaSX, deltaSY, dAngle);

		// mark that this nodeinst has changed
		if (ni.getChangeClock() != changeClock)
		{
			Undo.modifyNodeInst(ni, oldCX, oldCY, oldSX, oldSY, oldang);
		}

		ni.setChangeClock(changeClock);

		// see if this nodeinst is a port of the current cell
		if (ni.getNumExports() == 0) return false;
		return true;
	}

	/**
	 * Method to modify all of the arcs connected to a NodeInst.
	 * @param ni the NodeInst being examined.
	 * @param dangle the change in the nodes rotation (in tenth-degrees).
	 * @param dSX the change in the node's X size.
	 * @param dSY the change in the node's Y size.
	 * @param flipX true if the node flipped in X coordinates.
	 * @param flipY true if the node flipped in Y coordinates.
	 * @return true if some exports on the current cell have moved.
	 * This indicates that the cell must be re-examined for export locations.
	 */
	private static boolean modNodeArcs(NodeInst ni, int dangle, double dSX, double dSY, boolean flipX, boolean flipY)
	{
		if (DEBUG) System.out.println("Updating arcs on node "+ni.describe());

		// assume cell needs no further looks
		boolean examineCell = false;

		// next look at arcs that run within this nodeinst
		modWithin(ni, dangle, dSX, dSY, flipX, flipY);

		// next look at the rest of the rigid arcs on this nodeinst
		if (modRigid(ni, dangle, dSX, dSY, flipX, flipY)) examineCell = true;

		// finally, look at rest of the flexible arcs on this nodeinst
		if (modFlex(ni, dangle, dSX, dSY, flipX, flipY)) examineCell = true;

		return examineCell;
	}

	/*
	 * Method to modify the arcs that run within nodeinst "ni"
	 */
	private static void modWithin(NodeInst ni, int dAngle, double dSX, double dSY, boolean flipX, boolean flipY)
	{
		// ignore all this stuff if the node just got created
		Undo.Change change = ni.getChange();
		if (change != null && change.getType() == Undo.Type.NODEINSTNEW) return;

		// build a list of the arcs with both ends on this nodeinst
		List interiorArcs = new ArrayList();
		for(Iterator it = ni.getConnections(); it.hasNext(); )
		{
			Connection con = (Connection)it.next();
			ArcInst ai = con.getArc();

			// ignore if arcinst is not within the node
			if (ai.getHead().getPortInst().getNodeInst() != ai.getTail().getPortInst().getNodeInst()) continue;
			if (ai.getChangeClock() == changeClock) continue;

			// include in the list to be considered here
			interiorArcs.add(ai);
		}

		// look for arcs with both ends on this nodeinst
		for(Iterator it = interiorArcs.iterator(); it.hasNext(); )
		{
			ArcInst ai = (ArcInst)it.next();
			if (deletedArcs.contains(ai)) continue;

			// if arcinst has already been changed check its connectivity
			if (ai.getChangeClock() == changeClock)
			{
				if (DEBUG) System.out.println("    Arc already changed");
				ensureArcInst(ai, 0);
				continue;
			}

			// prepare transformation matrix
			AffineTransform trans = NodeInst.pureRotate(dAngle, flipX, flipY);

			// compute old center of nodeinst
			double ox = change.getA1();
			double oy = change.getA2();

			// determine the new ends of the arcinst
			adjustMatrix(ni, ai.getHead().getPortInst().getPortProto(), trans);
			Point2D newHead = new Point2D.Double();
			Point2D src = new Point2D.Double(ai.getHead().getLocation().getX()-ox, ai.getHead().getLocation().getY()-oy);
			trans.transform(src, newHead);

			adjustMatrix(ni, ai.getTail().getPortInst().getPortProto(), trans);
			Point2D newTail = new Point2D.Double();
			src.setLocation(ai.getTail().getLocation().getX()-ox, ai.getTail().getLocation().getY()-oy);
			trans.transform(src, newTail);

			// move the arcinst
			doMoveArcInst(ai, newHead, newTail, 0);
		}
	}

	/**
	 * Method to modify the rigid arcs connected to a NodeInst.
	 * @param ni the NodeInst being examined.
	 * @param dAngle the change in the nodes rotation (in tenth-degrees).
	 * @param dSX the change in the node's X size.
	 * @param dSY the change in the node's Y size.
	 * @param flipX true if the node flipped in X coordinates.
	 * @param flipY true if the node flipped in Y coordinates.
	 * @return true if any nodes that have exports move.
	 * This indicates that instances of the current cell must be examined for ArcInst motion.
	 */
	private static boolean modRigid(NodeInst ni, int dAngle, double dSX, double dSY, boolean flipX, boolean flipY)
	{
		// build a list of the rigid arcs on this nodeinst
		List rigidArcs = new ArrayList();
		for(Iterator it = ni.getConnections(); it.hasNext(); )
		{
			Connection con = (Connection)it.next();
			ArcInst ai = con.getArc();

			// ignore if arcinst is not flexible
			if (ai.getChangeClock() == changeClock-1 || ai.getChangeClock() == changeClock+1) continue;
			if (ai.getChangeClock() != changeClock-2 && !ai.isRigid()) continue;

			// ignore arcs that connect two ports on the same node
			if (ai.getHead().getPortInst().getNodeInst() == ai.getTail().getPortInst().getNodeInst()) continue;

			// include in the list to be considered here
			rigidArcs.add(ai);
		}
		if (rigidArcs.size() == 0) return false;

		// prepare transformation matrix and angle/transposition information
		AffineTransform trans = NodeInst.pureRotate(dAngle, flipX, flipY);

		// look for rigid arcs on this nodeinst
		boolean examineCell = false;
		for(Iterator it = rigidArcs.iterator(); it.hasNext(); )
		{
			ArcInst ai = (ArcInst)it.next();
			if (deletedArcs.contains(ai)) continue;
			ai.clearRigidModified();
			if (DEBUG) System.out.println("  From node " + ni.describe() + " Modifying Rigid arc "+ai.describe());

			// if rigid arcinst has already been changed check its connectivity
			if (ai.getChangeClock() == changeClock)
			{
				if (DEBUG) System.out.println("    Arc already changed");
				ensureArcInst(ai, 0);
				continue;
			}

			// find out which end of the arcinst is where, ignore internal arcs
			Connection thisEnd = ai.getHead();   int thisEndIndex = 0;
			Connection thatEnd = ai.getTail();   int thatEndIndex = 1;
			if (thatEnd.getPortInst().getNodeInst() == ni)
			{
				thisEnd = ai.getTail();   thisEndIndex = 1;
				thatEnd = ai.getHead();   thatEndIndex = 0;
			}

			NodeInst ono = thatEnd.getPortInst().getNodeInst();
			PortProto opt = thatEnd.getPortInst().getPortProto();

			Undo.Change change = ni.getChange();
			double ox = 0, oy = 0;
			if (change != null && change.getType() != Undo.Type.NODEINSTNEW)
			{
				// node "ni" changed, so adjust the offset
				ox = change.getA1();
				oy = change.getA2();
				adjustMatrix(ni, thisEnd.getPortInst().getPortProto(), trans);
			}

			// create the two points that will be the new ends of this arc
			Point2D [] newPts = new Point2D.Double[2];
			newPts[0] = new Point2D.Double();
			newPts[1] = new Point2D.Double();

			// figure out the new location of this arcinst connection
			Point2D src = new Point2D.Double(thisEnd.getLocation().getX()-ox, thisEnd.getLocation().getY()-oy);
			trans.transform(src, newPts[thisEndIndex]);

			// figure out the new location of that arcinst connection
			src.setLocation(thatEnd.getLocation().getX()-ox, thatEnd.getLocation().getY()-oy);
			trans.transform(src, newPts[thatEndIndex]);

			// see if other nodeinst has changed
			boolean locked = false;
			if (ono.getChangeClock() == changeClock) locked = true; else
			{
				if (ono.isLocked()) locked = true; else
				{
					if (ono.getProto() instanceof Cell)
					{
						if (ono.getParent().isInstancesLocked()) locked = true;
					} else
					{
						if (User.isDisallowModificationLockedPrims() &&
							((PrimitiveNode)ono.getProto()).isLockedPrim()) locked = true;
					}
				}
			}
			if (!locked)
			{
				// compute port motion within the other nodeinst (is this right? !!!)
				Poly oldPoly = oldPortPosition(ono, opt);
				//Point2D onoPt = oldPortPosition(ono, opt);
				double oldX = oldPoly.getCenterX();
				double oldY = oldPoly.getCenterY();
				Poly oPoly = thatEnd.getPortInst().getPoly();
				double dx = oPoly.getCenterX();   double dy = oPoly.getCenterY();
				double othX = dx - oldX;
				double othY = dy - oldY;

				// figure out the new location of the other nodeinst
				src.setLocation(ono.getAnchorCenterX()-ox, ono.getAnchorCenterY()-oy);
				Point2D ptD = new Point2D.Double();
				trans.transform(src, ptD);
				dx = ptD.getX();   dy = ptD.getY();
				dx = dx - ono.getAnchorCenterX() - othX;
				dy = dy - ono.getAnchorCenterY() - othY;

				// move the other nodeinst
				int nextAngle = dAngle;

				boolean thisWasTranspose = false;
				if (change != null && change.getType() != Undo.Type.NODEINSTNEW)
				{
					thisWasTranspose = (change.getA3() < 0) ^ (change.getA4() < 0);
				}
				boolean onoTranspose = ono.isMirroredAboutXAxis() ^ ono.isMirroredAboutYAxis();
				boolean dTrans = flipX ^ flipY;
				boolean oFlipX = ono.isMirroredAboutXAxis(); if (flipX) oFlipX = !oFlipX;
				boolean oFlipY = ono.isMirroredAboutYAxis(); if (flipY) oFlipY = !oFlipY;
				if (dTrans && (onoTranspose != thisWasTranspose))
				{
					nextAngle = (3600 - nextAngle) % 3600;
				}

				// ignore null motion on nodes that have already been examined
				if (dx != 0 || dy != 0 || nextAngle != 0 || ono.getChangeClock() != changeClock)
				{
					ai.setRigidModified();
					if (DEBUG) System.out.println("    Moving node "+ono.describe()+" at other end by ("+dx+","+dy+")");
					double changeSX = 0, changeSY = 0;
					if (oFlipX) changeSX = -ono.getXSizeWithMirror() * 2;
					if (oFlipY) changeSY = -ono.getYSizeWithMirror() * 2;
					if (alterNodeInst(ono, dx, dy, changeSX, changeSY, nextAngle, true))
						examineCell = true;
				}
			}

			// move the arcinst
			if (DEBUG) System.out.println("    Altering arc, end moves to "+newPts[0]+" tail moves to "+newPts[1]);
			doMoveArcInst(ai, newPts[0], newPts[1], 0);
		}

		// re-scan rigid arcs and recursively modify arcs on other nodes
		for(Iterator it = rigidArcs.iterator(); it.hasNext(); )
		{
			ArcInst ai = (ArcInst)it.next();
			if (deletedArcs.contains(ai)) continue;

			// only want arcinst that was just explored
			if (!ai.isRigidModified()) continue;

			// get the other nodeinst
			Connection thisEnd = ai.getHead();   int thisEndIndex = 0;
			Connection thatEnd = ai.getTail();   int thatEndIndex = 1;
			NodeInst ono;
			if (ai.getTail().getPortInst().getNodeInst() == ni) ono = ai.getHead().getPortInst().getNodeInst(); else
				ono = ai.getTail().getPortInst().getNodeInst();

			int nextAngle = dAngle;
			if (ono.isMirroredAboutXAxis() ^ ono.isMirroredAboutYAxis()) nextAngle = (3600 - nextAngle) % 3600;
			if (DEBUG) System.out.println("  Node " + ni.describe() + " re-examining arc " + ai.describe() + " to other node "+ono.describe());
			if (modNodeArcs(ono, nextAngle, 0, 0, flipX, flipY)) examineCell = true;
		}
		return examineCell;
	}

	/**
	 * Method to modify the flexible arcs connected to a NodeInst.
	 * @param ni the NodeInst being examined.
	 * @param dAngle the change in the nodes rotation (in tenth-degrees).
	 * @param dSX the change in the node's X size.
	 * @param dSY the change in the node's Y size.
	 * @param flipX true if the node flipped in X coordinates.
	 * @param flipY true if the node flipped in Y coordinates.
	 * @return true if any nodes that have exports move.
	 * This indicates that instances of the current cell must be examined for ArcInst motion.
	 */
	private static boolean modFlex(NodeInst ni, int dAngle, double dSX, double dSY, boolean flipX, boolean flipY)
	{
		// build a list of the flexible arcs on this nodeinst
		List flexArcs = new ArrayList();
		for(Iterator it = ni.getConnections(); it.hasNext(); )
		{
			Connection con = (Connection)it.next();
			ArcInst ai = con.getArc();

			// ignore if arcinst is not flexible
			if (ai.getChangeClock() == changeClock-2 || ai.getChangeClock() == changeClock) continue;
			if (ai.getChangeClock() != changeClock-1 && ai.isRigid()) continue;

			// ignore arcs that connect two ports on the same node
			if (ai.getHead().getPortInst().getNodeInst() == ai.getTail().getPortInst().getNodeInst()) continue;

			// include in the list to be considered here
			flexArcs.add(ai);
		}
		if (flexArcs.size() == 0) return false;

		// if simple rotation on transposed nodeinst, reverse rotation
		int nextAngle = dAngle;
		if (ni.isMirroredAboutXAxis() ^ ni.isMirroredAboutYAxis()) nextAngle = (3600 - dAngle) % 3600;

		// prepare transformation matrix and angle/transposition information
		AffineTransform trans = NodeInst.pureRotate(nextAngle, flipX, flipY);

		// look at all of the flexible arcs on this nodeinst
		boolean examineCell = false;
		for(Iterator it = flexArcs.iterator(); it.hasNext(); )
		{
			ArcInst ai = (ArcInst)it.next();
			if (deletedArcs.contains(ai)) continue;
			if (DEBUG) System.out.println("  Modifying fixed-angle arc "+ai.describe());

			// if flexible arcinst has been changed, verify its connectivity
			if (ai.getChangeClock() >= changeClock+1)
			{
				if (DEBUG) System.out.println("   Arc already changed");
				ensureArcInst(ai, 1);
				continue;
			}

			// figure where each end of the arcinst is
			Connection thisEnd = ai.getHead();   int thisEndIndex = 0;
			Connection thatEnd = ai.getTail();   int thatEndIndex = 1;
			if (thatEnd.getPortInst().getNodeInst() == ni)
			{
				thisEnd = ai.getTail();   thisEndIndex = 1;
				thatEnd = ai.getHead();   thatEndIndex = 0;
			}

			// if nodeinst motion stays within port area, ignore the arcinst
			if (ai.isSlidable() && ai.stillInPort(thisEnd, thisEnd.getLocation(), true))
				continue;

			Undo.Change change = ni.getChange();
			double ox = 0, oy = 0;
			if (change != null && change.getType() == Undo.Type.NODEINSTMOD)
			{
				ox = change.getA1();
				oy = change.getA2();
				adjustMatrix(ni, thisEnd.getPortInst().getPortProto(), trans);
			}

			// create the two points that will be the new ends of this arc
			Point2D [] newPts = new Point2D.Double[2];
			newPts[thisEndIndex] = new Point2D.Double();
			newPts[thatEndIndex] = new Point2D.Double();

			// figure out the new location of this arcinst connection
			Point2D src = new Point2D.Double(thisEnd.getLocation().getX()-ox, thisEnd.getLocation().getY()-oy);
			trans.transform(src, newPts[thisEndIndex]);
			newPts[thisEndIndex].setLocation(DBMath.round(newPts[thisEndIndex].getX()),
				DBMath.round(newPts[thisEndIndex].getY()));

			// make sure the arc end is still in the port
			Poly poly = thisEnd.getPortInst().getPoly();
			if (poly.isInside(newPts[thisEndIndex]))
			{
				Rectangle2D bbox = poly.getBox();
				if (newPts[thisEndIndex].getY() >= bbox.getMinY() && newPts[thisEndIndex].getY() <= bbox.getMaxY())
				{
					// extend arc horizontally to fit in port
					if (newPts[thisEndIndex].getX() < bbox.getMinX())
					{
						newPts[thisEndIndex].setLocation(bbox.getMinX(), newPts[thisEndIndex].getY());
					} else if (newPts[thisEndIndex].getX() > bbox.getMaxX())
					{
						newPts[thisEndIndex].setLocation(bbox.getMaxX(), newPts[thisEndIndex].getY());
					}
				} else if (newPts[thisEndIndex].getX() >= bbox.getMinX() && newPts[thisEndIndex].getX() <= bbox.getMaxX())
				{
					// extend arc vertically to fit in port
					if (newPts[thisEndIndex].getY() < bbox.getMinY())
					{
						newPts[thisEndIndex].setLocation(newPts[thisEndIndex].getX(), bbox.getMinY());
					} else if (newPts[thisEndIndex].getY() > bbox.getMaxY())
					{
						newPts[thisEndIndex].setLocation(newPts[thisEndIndex].getX(), bbox.getMaxY());
					}
				} else
				{
					// extend arc arbitrarily to fit in port
					Point2D pt = poly.closestPoint(newPts[thisEndIndex]);
					newPts[thisEndIndex].setLocation(pt);
				}
			}

			// get other end of arcinst and its position
			NodeInst ono = thatEnd.getPortInst().getNodeInst();
			newPts[thatEndIndex].setLocation(thatEnd.getLocation());

			// see if other nodeinst has changed
			boolean mangle = true;
			if (!ai.isFixedAngle()) mangle = false; else
			{
				if (ono.isLocked()) mangle = false; else
				{
					if (ono.getProto() instanceof Cell)
					{
						if (ono.getParent().isInstancesLocked()) mangle = false;
					} else
					{
						if (User.isDisallowModificationLockedPrims() &&
							((PrimitiveNode)ono.getProto()).isLockedPrim()) mangle = false;
					}
				}
			}
			if (mangle)
			{
				// other nodeinst untouched, mangle it
				double dx = newPts[thisEndIndex].getX() - thisEnd.getLocation().getX();
				double dy = newPts[thisEndIndex].getY() - thisEnd.getLocation().getY();
				double odx = newPts[thatEndIndex].getX() - thatEnd.getLocation().getX();
				double ody = newPts[thatEndIndex].getY() - thatEnd.getLocation().getY();
				if (DBMath.doublesEqual(thisEnd.getLocation().getX(), thatEnd.getLocation().getX()))
				{
					// null arcinst must not be explicitly horizontal
					if (!DBMath.doublesEqual(thisEnd.getLocation().getY(), thatEnd.getLocation().getY()) ||
						ai.getAngle() == 900 || ai.getAngle() == 2700)
					{
						// vertical arcinst: see if it really moved in X
						if (dx == odx) dx = odx = 0;

						// move horizontal, shrink vertical
						newPts[thatEndIndex].setLocation(newPts[thatEndIndex].getX() + dx-odx, newPts[thatEndIndex].getY());

						// see if next nodeinst need not be moved
						if (!DBMath.doublesEqual(dx, odx) && ai.isSlidable() && ai.stillInPort(thatEnd, newPts[thatEndIndex], true))
							dx = odx = 0;

						// if other node already moved, don't move it any more
						if (ono.getChangeClock() == changeClock) dx = odx = 0;

						if (dx != odx)
						{
							double xAmount = DBMath.round(dx-odx);
							if (DEBUG) System.out.println("  Moving node "+ono.describe()+" by ("+xAmount+",0)");
							if (alterNodeInst(ono, xAmount, 0, 0, 0, 0, true))
								examineCell = true;
						}
						if (DEBUG) System.out.println("  Moving vertical arc so head=("+newPts[0].getX()+","+newPts[0].getY()+
							") and tail=("+newPts[1].getX()+","+newPts[1].getY()+")");
						doMoveArcInst(ai, newPts[0], newPts[1], 1);
						if (!DBMath.doublesEqual(dx, odx))
							if (modNodeArcs(ono, 0, 0, 0, false, false)) examineCell = true;
						continue;
					}
				}
				if (DBMath.doublesEqual(thisEnd.getLocation().getY(), thatEnd.getLocation().getY()))
				{
					// horizontal arcinst: see if it really moved in Y
					if (DBMath.doublesEqual(dy, ody)) dy = ody = 0;

					// shrink horizontal, move vertical
					newPts[thatEndIndex].setLocation(newPts[thatEndIndex].getX(), newPts[thatEndIndex].getY() + dy-ody);

					// see if next nodeinst need not be moved
					if (!DBMath.doublesEqual(dy, ody) && ai.isSlidable() &&
						ai.stillInPort(thatEnd, newPts[thatEndIndex], true))
							dy = ody = 0;

					// if other node already moved, don't move it any more
					if (ono.getChangeClock() == changeClock) dx = odx = 0;

					if (!DBMath.doublesEqual(dy, ody))
					{
						if (DEBUG) System.out.println("  Moving node "+ono.describe()+" by (0,"+(dy-ody)+")");
						if (alterNodeInst(ono, 0, dy-ody, 0, 0, 0, true))
							examineCell = true;
					}
					if (DEBUG) System.out.println("  Moving horizontal arc so head=("+newPts[0].getX()+","+newPts[0].getY()+
						") and tail=("+newPts[1].getX()+","+newPts[1].getY()+")");
					doMoveArcInst(ai, newPts[0], newPts[1], 1);
					if (!DBMath.doublesEqual(dy, ody))
						if (modNodeArcs(ono, 0, 0, 0, false, false)) examineCell = true;
					continue;
				}

				/***** THIS CODE HANDLES ALL-ANGLE RIGIDITY WITH THE FIXED-ANGLE CONSTRAINT *****/

				// special code to handle nonorthogonal fixed-angles
				nonOrthogFixAng(ai, thisEnd, thisEndIndex, thatEnd, thatEndIndex, ono, newPts);
				dx = newPts[thatEndIndex].getX() - thatEnd.getLocation().getX();
				dy = newPts[thatEndIndex].getY() - thatEnd.getLocation().getY();

				// change the arc
				updateArc(ai, newPts[0], newPts[1], 1);

				// if other node already moved, don't move it any more
				if (ono.getChangeClock() == changeClock) dx = dy = 0;

				if (dx != 0 || dy != 0)
				{
					if (DEBUG) System.out.println("  Moving node "+ono.describe()+" by ("+dx+","+dy+")");
					if (alterNodeInst(ono, dx, dy, 0, 0, 0, true))
						examineCell = true;
					if (modNodeArcs(ono, 0, 0, 0, false, false)) examineCell = true;
				}
				continue;
			}

			// other node has changed or arc is funny, just use its position
			if (DEBUG) System.out.println("  Moving nonmanhattan arc so head=("+newPts[0].getX()+","+newPts[0].getY()+
				") and tail=("+newPts[1].getX()+","+newPts[1].getY()+")");
			doMoveArcInst(ai, newPts[0], newPts[1], 1);
		}

		return examineCell;
	}

	/**
	 * Method to determine the motion of a nonorthogonal ArcInst given that one end has moved.
	 * The end that is "thisEnd" has moved to (ax[thisEndIndex],ay[thisEndIndex]), so this method
	 * must determine the coordinates of the other end and set (ax[thatEndIndex],ay[thatEndIndex]).
	 * @param ai the nonorthogonal ArcInst that is adjusting.
	 * @param thisEnd the Connection at one end of the ArcInst.
	 * @param thisEndIndex the index (0 or 1) of "thisEnd" of the ArcInst.
	 * @param thatEnd the Connection at the other end of the ArcInst.
	 * @param thatEndIndex the index (0 or 1) of "thatEnd" of the ArcInst.
	 * @param ono the node at the other end ("thatEnd").
	 * @param newPts an array of 2 points that defines the coordinates of the two ends (0: head, 1: tail).
	 */
	private static void nonOrthogFixAng(ArcInst ai, Connection thisEnd, int thisEndIndex, Connection thatEnd, int thatEndIndex,
		NodeInst ono, Point2D [] newPts)
	{
		// look for longest other arc on "ono" to determine proper end position
		double bestDist = Double.MIN_VALUE;
		ArcInst bestAI = null;
		for(Iterator it = ono.getConnections(); it.hasNext(); )
		{
			Connection con = (Connection)it.next();
			ArcInst oai = con.getArc();
			if (oai == ai) continue;
			if (oai.getLength() < bestDist) continue;
			bestDist = oai.getLength();
			bestAI = oai;
		}

		// if no other arcs, allow that end to move the same as this end
		if (bestAI == null)
		{
			newPts[thatEndIndex].setLocation(
				newPts[thatEndIndex].getX() + newPts[thisEndIndex].getX() - thisEnd.getLocation().getX(),
				newPts[thatEndIndex].getY() + newPts[thisEndIndex].getY() - thisEnd.getLocation().getY());
			return;
		}

		// compute intersection of arc "bestai" with new moved arc "ai"
		Point2D inter = DBMath.intersect(newPts[thisEndIndex], ai.getAngle(),
			bestAI.getHead().getLocation(), bestAI.getAngle());
		if (inter == null)
		{
			newPts[thatEndIndex].setLocation(
				newPts[thatEndIndex].getX() + newPts[thisEndIndex].getX() - thisEnd.getLocation().getX(),
				newPts[thatEndIndex].getY() + newPts[thisEndIndex].getY() - thisEnd.getLocation().getY());
			return;
		}
		newPts[thatEndIndex].setLocation(inter);
	}

	/**
	 * Method to ensure that an ArcInst is still connected properly at each end.
	 * If it is not, the ArcInst must be jogged or adjusted.
	 * @param ai the ArcInst to check.
	 * @param arctyp the nature of the arc: 0 for rigid, 1 for flexible.
	 */
	private static void ensureArcInst(ArcInst ai, int arctyp)
	{
		// if nothing is outside port, quit
		Connection head = ai.getHead();
		Point2D headPoint = head.getLocation();
		boolean inside0 = ai.stillInPort(head, headPoint, true);
		Connection tail = ai.getTail();
		Point2D tailPoint = tail.getLocation();
		boolean inside1 = ai.stillInPort(tail, tailPoint, true);
		if (inside0 && inside1) return;

		// get area of the ports
		Poly headPoly = head.getPortInst().getPoly();
		Poly tailPoly = tail.getPortInst().getPoly();

		// if arcinst is not fixed-angle, run it directly to the port centers
		if (!ai.isFixedAngle())
		{
			double fx = headPoly.getCenterX();   double fy = headPoly.getCenterY();
			double tx = tailPoly.getCenterX();   double ty = tailPoly.getCenterY();
			doMoveArcInst(ai, new Point2D.Double(fx, fy), new Point2D.Double(tx, ty), arctyp);
			return;
		}

		// get bounding boxes of polygons
		Rectangle2D headBounds = headPoly.getBounds2D();
		Rectangle2D tailBounds = tailPoly.getBounds2D();
		double lx0 = headBounds.getMinX();   double hx0 = headBounds.getMaxX();
		double ly0 = headBounds.getMinY();   double hy0 = headBounds.getMaxY();
		double lx1 = tailBounds.getMinX();   double hx1 = tailBounds.getMaxX();
		double ly1 = tailBounds.getMinY();   double hy1 = tailBounds.getMaxY();

		// if manhattan path runs between the ports, adjust the arcinst
		if (lx0 <= hx1 && lx1 <= hx0)
		{
			// arcinst runs vertically
			double tx = (Math.max(lx0,lx1) + Math.min(hx0,hx1)) / 2;
			double fx = tx;
			double fy = (ly0+hy0) / 2;   double ty = (ly1+hy1) / 2;
			Point2D fPt = headPoly.closestPoint(new Point2D.Double(fx, fy));
			Point2D tPt = tailPoly.closestPoint(new Point2D.Double(tx, ty));
			doMoveArcInst(ai, fPt, tPt, arctyp);
			return;
		}
		if (ly0 <= hy1 && ly1 <= hy0)
		{
			// arcinst runs horizontally
			double ty = (Math.max(ly0,ly1) + Math.min(hy0,hy1)) / 2;
			double fy = ty;
			double fx = (lx0+hx0) / 2;   double tx = (lx1+hx1) / 2;
			Point2D fPt = headPoly.closestPoint(new Point2D.Double(fx, fy));
			Point2D tPt = tailPoly.closestPoint(new Point2D.Double(tx, ty));
			doMoveArcInst(ai, fPt, tPt, arctyp);
			return;
		}

		// give up and jog the arcinst
		double fx = headPoly.getCenterX();   double fy = headPoly.getCenterY();
		double tx = tailPoly.getCenterX();   double ty = tailPoly.getCenterY();
		doMoveArcInst(ai, new Point2D.Double(fx, fy), new Point2D.Double(tx, ty), arctyp);
	}

	/**
	 * Method to update the coordinates of the ends of an ArcInst.
	 * @param ai the ArcInst to adjust
	 * @param headPt the new coordinates of the head of the ArcInst.
	 * @param tailPt the new coordinates of the tail of the ArcInst.
	 * @param arctyp the nature of the arc: 0 for rigid, 1 for flexible.
	 */
	private static void updateArc(ArcInst ai, Point2D headPt, Point2D tailPt, int arctyp)
	{
		// set the proper arcinst position
		Point2D oldHeadPt = ai.getHead().getLocation();
		Point2D oldTailPt = ai.getTail().getLocation();
		double oldHeadX = oldHeadPt.getX();   double oldHeadY = oldHeadPt.getY();
		double oldTailX = oldTailPt.getX();   double oldTailY = oldTailPt.getY();
		ai.lowLevelModify(0, headPt.getX() - oldHeadX, headPt.getY() - oldHeadY, tailPt.getX() - oldTailX, tailPt.getY() - oldTailY);
		if (DEBUG) System.out.println("Arc " + ai.describe() + " now runs from ("+
			ai.getTail().getLocation().getX()+","+ai.getTail().getLocation().getY()+") to ("+
			ai.getHead().getLocation().getX()+","+ai.getHead().getLocation().getY()+")");

		// if the arc hasn't changed yet, record this change
		if (ai.getChange() == null)
		{
			Undo.modifyArcInst(ai, oldHeadX, oldHeadY, oldTailX, oldTailY, ai.getWidth());
			ai.setChangeClock(changeClock + arctyp);
		}
	}

	/**
	 * Method to move the coordinates of the ends of an ArcInst.
	 * If the arc cannot be moved in this way, it will be broken up into 3 jogged arcs.
	 * @param ai the ArcInst to adjust
	 * @param headPt the new coordinates of the head of the ArcInst.
	 * @param tailPt the new coordinates of the tail of the ArcInst.
	 * @param arctyp the nature of the arc: 0 for rigid, 1 for flexible.
	 */
	private static void doMoveArcInst(ArcInst ai, Point2D headPt, Point2D tailPt, int arctyp)
	{
		// check for null arcinst motion
		Connection head = ai.getHead();
		Connection tail = ai.getTail();
		if (headPt.equals(head.getLocation()) && tailPt.equals(tail.getLocation()))
		{
			// only ignore null motion on fixed-angle requests
			if (arctyp != 0) return;
		}

		// if the angle is the same or doesn't need to be, simply make the change
		if (!ai.isFixedAngle() ||
			(ai.isRigid() && ai.getChangeClock() != changeClock-1) ||
			ai.getChangeClock() == changeClock-2 ||
			headPt.equals(tailPt) ||
			(ai.getAngle() % 1800) == (DBMath.figureAngle(tailPt, headPt) % 1800))
		{
			updateArc(ai, headPt, tailPt, arctyp);
			return;
		}

		// manhattan arcinst becomes nonmanhattan: remember facts about it
		if (DEBUG) System.out.println("Jogging arc");
		PortInst fpi = head.getPortInst();
		NodeInst fno = fpi.getNodeInst();   PortProto fpt = fpi.getPortProto();
		PortInst tpi = tail.getPortInst();
		NodeInst tno = tpi.getNodeInst();   PortProto tpt = tpi.getPortProto();

		ArcProto ap = ai.getProto();   Cell pnt = ai.getParent();   double wid = ai.getWidth();

		// figure out what nodeinst proto connects these arcs
		PrimitiveNode np = ((PrimitiveArc)ap).findOverridablePinProto();
		double psx = np.getDefWidth();
		double psy = np.getDefHeight();

		// replace it with three arcs and two nodes
		NodeInst no1 = null, no2 = null;
		if (DBMath.doublesEqual(head.getLocation().getX(), tail.getLocation().getX()))
		{
			// arcinst was vertical
			double oldyA = (tailPt.getY()+headPt.getY()) / 2;
			double oldyB = oldyA;
			double oldxA = headPt.getX();   double oldxB = tailPt.getX();
			no1 = NodeInst.newInstance(np, new Point2D.Double(oldxB, oldyB),psx, psy, pnt);
			no2 = NodeInst.newInstance(np, new Point2D.Double(oldxA, oldyA),psx, psy, pnt);
		} else
		{
			// assume horizontal arcinst
			double oldyA = headPt.getY();   double oldyB = tailPt.getY();
			double oldxA = (tailPt.getX()+headPt.getX()) / 2;
			double oldxB = oldxA;
			no1 = NodeInst.newInstance(np, new Point2D.Double(oldxB, oldyB),psx, psy, pnt);
			no2 = NodeInst.newInstance(np, new Point2D.Double(oldxA, oldyA),psx, psy, pnt);
		}
		if (no1 == null || no2 == null)
		{
			System.out.println("Problem creating jog pins");
			return;
		}

		PortInst no1pi = no1.getOnlyPortInst();
		Rectangle2D no1Bounds = no1pi.getPoly().getBounds2D();
		Point2D no1Pt = new Point2D.Double(no1Bounds.getCenterX(), no1Bounds.getCenterY());

		PortInst no2pi = no2.getOnlyPortInst();
		Rectangle2D no2Bounds = no2pi.getPoly().getBounds2D();
		Point2D no2Pt = new Point2D.Double(no2Bounds.getCenterX(), no2Bounds.getCenterY());

		ArcInst ar1 = ArcInst.newInstance(ap, wid, fpi, no2pi, headPt, no2Pt, null, 0);
		if (ar1 == null) return;
		ar1.copyStateBits(ai);
		if (ai.getHead().isNegated()) ar1.getHead().setNegated(true);
		ArcInst ar2 = ArcInst.newInstance(ap, wid, no2pi, no1pi, no2Pt, no1Pt, null, 0);
		if (ar2 == null) return;
		ar2.copyStateBits(ai);
		ArcInst ar3 = ArcInst.newInstance(ap, wid, no1pi, tpi, no1Pt, tailPt, null, 0);
		if (ar3 == null) return;
		ar3.copyStateBits(ai);
		if (ai.getTail().isNegated()) ar3.getTail().setNegated(true);
		if (ar1 == null || ar2 == null || ar3 == null)
		{
			System.out.println("Problem creating jog arcs");
			return;
		}
		ar2.copyVarsFrom(ai);
		ar2.setNameTextDescriptor(ai.getNameTextDescriptor());
		ar1.setChangeClock(changeClock + arctyp);
		ar2.setChangeClock(changeClock + arctyp);
		ar3.setChangeClock(changeClock + arctyp);

		// now kill the arcinst
//		if ((CHANGE *)ai->changeaddr != NOCHANGE)
//		{
//			ai->end[0].xpos = ((CHANGE *)ai->changeaddr)->p1;
//			ai->end[0].ypos = ((CHANGE *)ai->changeaddr)->p2;
//			ai->end[1].xpos = ((CHANGE *)ai->changeaddr)->p3;
//			ai->end[1].ypos = ((CHANGE *)ai->changeaddr)->p4;
//			ai->length = computedistance(ai->end[0].xpos, ai->end[0].ypos,
//				ai->end[1].xpos, ai->end[1].ypos);
//			ai->width = ((CHANGE *)ai->changeaddr)->p5;
//			determineangle(ai);
//		}
		deletedArcs.add(ai);
		ar2.setNameTextDescriptor(ai.getNameTextDescriptor());
		ai.kill();
		String oldName = ai.getName();
		if (oldName != null) ar2.setName(oldName);
	}

	/**
	 * Method to adjust the transformation matrix "trans" by placing translation
	 * information for nodeinst "ni", port "pp".
	 *
	 * there are only two types of nodeinst changes: internal and external.
	 * The internal changes are scaling and port motion changes that
	 * are usually caused by other changes within the cell.  The external
	 * changes are rotation and transposition.  These two changes never
	 * occur at the same time.  There is also translation change that
	 * can occur at any time and is of no importance here.  What is
	 * important is that the transformation matrix "trans" handles
	 * the external changes and internal changes.  External changes are already
	 * set by the "makeangle" method and internal changes are
	 * built into the matrix here.
	 */
	private static void adjustMatrix(NodeInst ni, PortProto pp, AffineTransform trans)
	{
		double m00 = trans.getScaleX();
		double m01 = trans.getShearX();
		double m11 = trans.getScaleY();
		double m10 = trans.getShearY();
		double m02 = ni.getAnchorCenterX();
		double m12 = ni.getAnchorCenterY();
		Undo.Change change = ni.getChange();
		if (change.getA3() * ni.getXSizeWithMirror() >= 0 && change.getA4() * ni.getYSizeWithMirror() >= 0 && change.getI1() == ni.getAngle())
		{
			// nodeinst did not rotate or mirror: adjust for port motion
			Poly oldPoly = oldPortPosition(ni, pp);
			Point2D ono = new Point2D.Double(oldPoly.getCenterX(), oldPoly.getCenterY());
			Poly curPoly = ni.getShapeOfPort(pp);
			double dx = curPoly.getCenterX();
			double dy = curPoly.getCenterY();
			double ox = change.getA1();
			double oy = change.getA2();

			// Zero means flat port or artwork. Valid for new technology
			if (oldPoly.getBounds2D().getWidth() > 0)
				m00 = curPoly.getBounds2D().getWidth() / oldPoly.getBounds2D().getWidth();
			if (oldPoly.getBounds2D().getHeight() >0)
				m11 = curPoly.getBounds2D().getHeight() / oldPoly.getBounds2D().getHeight();
			//m00 = curPoly.getBounds2D().getWidth() / oldPoly.getBounds2D().getWidth();
			//m11 = curPoly.getBounds2D().getHeight() / oldPoly.getBounds2D().getHeight();
			m02 = dx - ono.getX() + ox;   m12 = dy - ono.getY() + oy;
		}
		trans.setTransform(m00, m10, m01, m11, m02, m12);
	}

	/**
	 * Method to compute the position of portproto "pp" on nodeinst "ni" and
	 * place the center of the area in the parameters "x" and "y".  The position
	 * is the "old" position, as determined by any changes that may have occured
	 * to the nodeinst (and any sub-nodes).
	 */
	private static Poly oldPortPosition(NodeInst ni, PortProto pp)
	{
		// descend to the primitive node
		AffineTransform subrot = makeOldRot(ni);
		NodeInst bottomNi = ni;
		PortProto bottomPP = pp;
		while (bottomNi.getProto() instanceof Cell)
		{
			AffineTransform localtran = makeOldTrans(bottomNi);
			subrot.concatenate(localtran);

			Undo.Change change = ((Export)bottomPP).getChange();
			if (change != null && change.getType() == Undo.Type.EXPORTMOD)
			{
				PortInst bottomPi = (PortInst)change.getO1();
				bottomNi = bottomPi.getNodeInst();
				bottomPP = bottomPi.getPortProto();
			} else
			{
				bottomNi = ((Export)bottomPP).getOriginalPort().getNodeInst();
				bottomPP = ((Export)bottomPP).getOriginalPort().getPortProto();
			}
			localtran = makeOldRot(bottomNi);
			subrot.concatenate(localtran);
		}

		// if the node hasn't changed, use its current values
		Undo.Change change = bottomNi.getChange();
		if (change != null && change.getType() == Undo.Type.NODEINSTMOD)
		{
			// get the old values
			double cX = change.getA1();
			double cY = change.getA2();
			double sX = change.getA3();
			double sY = change.getA4();
			int angle = change.getI1();

			// create a fake node with these values
			NodeInst oldNi = NodeInst.lowLevelAllocate();
			oldNi.lowLevelPopulate(bottomNi.getProto(), new Point2D.Double(cX, cY), sX, sY, angle, bottomNi.getParent());
			bottomNi = oldNi;
		}
		PrimitiveNode np = (PrimitiveNode)bottomNi.getProto();
		Technology tech = np.getTechnology();
		Poly poly = tech.getShapeOfPort(bottomNi, (PrimitivePort)bottomPP);
		poly.transform(subrot);
		return (poly);
	}

	private static AffineTransform makeOldRot(NodeInst ni)
	{
		// if the node has not been modified, just use the current transformation
		Undo.Change change = ni.getChange();
		if (change == null || change.getType() != Undo.Type.NODEINSTMOD)
			return ni.rotateOut();

		// get the old values
		double cX = change.getA1();
		double cY = change.getA2();
		double sX = change.getA3();
		double sY = change.getA4();
		int angle = change.getI1();

		// create a fake node with these values
		NodeInst oldNi = NodeInst.lowLevelAllocate();
		oldNi.lowLevelPopulate(ni.getProto(), new Point2D.Double(cX, cY), sX, sY, angle, ni.getParent());

		// use the fake node to determine the former transformation matrix
		return oldNi.rotateOut();
	}

	private static AffineTransform makeOldTrans(NodeInst ni)
	{
		// get current values
		double cX = ni.getAnchorCenterX();
		double cY = ni.getAnchorCenterY();

		// set to previous values if they changed
		Undo.Change change = ni.getChange();
		if (change != null && change.getType() == Undo.Type.NODEINSTMOD)
		{
			cX = change.getA1();
			cY = change.getA2();
		}

		// create the former translation matrix
		AffineTransform transform = new AffineTransform();
		transform.translate(cX, cY);
		return transform;
	}

	FlagSet cellModFlag;
	FlagSet cellNoModFlag;
	FlagSet markNode;
	FlagSet touchNode;

	/**
	 * Method to re-compute the bounds of the cell "cell" (because an object
	 * has been added or removed from it) and store these bounds in the nominal
	 * size and the size of each instantiation of the cell.  It is also necessary
	 * to re-position each instantiation of the cell in its proper position list.
	 * If "forcedLook" is true, the cell is re-examined regardless of
	 * whether its size changed.
	 */
	void computeCell(Cell cell, boolean forcedLook)
	{
		// get current boundary of cell
		Rectangle2D oldCellBounds = new Rectangle2D.Double();
		oldCellBounds.setRect(cell.getRememberedBounds());
		Rectangle2D cellBounds = cell.getBounds();

		// quit if it has not changed
		if (oldCellBounds.equals(cellBounds) && !forcedLook) return;

		// advance the change clock
		changeClock += 4;

		// get former size of cell from change information
		double flx = cellBounds.getMinX();   double fhx = cellBounds.getMaxX();
		double fly = cellBounds.getMinY();   double fhy = cellBounds.getMaxY();
		Undo.Change change = cell.getChange();
		if (change != null && change.getType() == Undo.Type.CELLMOD)
		{
			// modification changes carry original size
			flx = change.getA1();   fhx = change.getA2();
			fly = change.getA3();   fhy = change.getA4();
		}

		// update the cell size
		if (change == null) Undo.modifyCell(cell, flx, fhx, fly, fhy);

		// see if all instances of this cell are in the same location
		boolean mixed = false;
		Cell oneParent = null;
		for(Iterator it = cell.getInstancesOf(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			if (oneParent != null && oneParent != ni.getParent()) mixed = true;
			oneParent = ni.getParent();
		}

		// if there are no constrained instances of the cell, no change
		if (oneParent == null) return;

		// if all parent cells the same, make changes to the instances
		if (!mixed && !forcedLook)
		{
			double dlx = cellBounds.getMinX() - flx;   double dhx = cellBounds.getMaxX() - fhx;
			double dly = cellBounds.getMinY() - fly;   double dhy = cellBounds.getMaxY() - fhy;
			for(Iterator it = cell.getInstancesOf(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();

				double dSX = DBMath.round(cellBounds.getWidth() - ni.getXSize());
				double dSY = DBMath.round(cellBounds.getHeight() - ni.getYSize());
				if (ni.isMirroredAboutYAxis()) dSX = -dSX;
				if (ni.isMirroredAboutXAxis()) dSY = -dSY;
				if (alterNodeInst(ni, 0, 0, dSX, dSY, 0, true)) forcedLook = true;
			} 
			for(Iterator it = cell.getInstancesOf(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				if (modNodeArcs(ni, 0, 0, 0, false, false)) forcedLook = true;
			}
			computeCell(oneParent, forcedLook);
			return;
		}

		/*
		 * if instances are scattered or port motion has occured, examine
		 * entire database in proper recursive order and adjust cell sizes
		 */
		cellModFlag = Cell.getFlagSet(1);
		cellNoModFlag = Cell.getFlagSet(1);
		markNode = NodeInst.getFlagSet(1);
		touchNode = NodeInst.getFlagSet(1);
		for(Iterator it = Library.getLibraries(); it.hasNext(); )
		{
			Library lib = (Library)it.next();
			for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
			{
				Cell c = (Cell)cIt.next();
				c.clearBit(cellModFlag);
				c.clearBit(cellNoModFlag);
			}
		}
		cell.setBit(cellModFlag);
		for(Iterator it = Library.getLibraries(); it.hasNext(); )
		{
			Library lib = (Library)it.next();
			for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
			{
				Cell c = (Cell)cIt.next();

				// only want cells with no instances as roots of trees
				Iterator iIt = c.getInstancesOf();
				if (iIt.hasNext()) continue;

				// now look recursively at the nodes in this cell
				lookDown(c);
			}
		}
		touchNode.freeFlagSet();
		markNode.freeFlagSet();
		cellNoModFlag.freeFlagSet();
		cellModFlag.freeFlagSet();
	}

	private boolean lookDown(Cell start)
	{
		// first look recursively to the bottom to see if this cell changed
		for(Iterator it = start.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			if (ni.getProto() instanceof Cell) ni.setBit(markNode); else
				ni.clearBit(markNode);
		}

		boolean foundone = true;
		while (foundone)
		{
			foundone = false;
			for(Iterator it = start.getNodes(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				if (!ni.isBit(markNode)) continue;
				ni.clearBit(markNode);
				Cell subCell = (Cell)ni.getProto();

				// ignore recursive references (showing icon in contents)
				if (ni.isIconOfParent()) continue;

				// if this nodeinst is to change, mark the parent cell also
				if (subCell.isBit(cellModFlag)) start.setBit(cellModFlag);

				// don't look inside if the cell is certified
				if (subCell.isBit(cellModFlag) || subCell.isBit(cellNoModFlag)) continue;

				// look inside nodeinst to see if it changed
				if (lookDown(subCell)) start.setBit(cellModFlag);
				foundone = true;
			}
		}

		// if this cell did not change, certify so and quit
		if (!start.isBit(cellModFlag))
		{
			start.setBit(cellNoModFlag);
			return false;
		}

		// mark those nodes that must change
		for(Iterator it = start.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			NodeProto np = ni.getProto();
			ni.clearBit(markNode);
			ni.clearBit(touchNode);
			if (!(np instanceof Cell)) continue;
			Cell subCell = (Cell)np;
			if (ni.isIconOfParent()) continue;
			if (!subCell.isBit(cellModFlag)) continue;
			ni.setBit(markNode);
			ni.setBit(touchNode);
		}

		// modify the nodes in this cell that changed
		boolean forcedLook = false;
		foundone = true;
		while (foundone)
		{
			foundone = false;
			for(Iterator it = start.getNodes(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				if (!ni.isBit(markNode)) continue;
				ni.clearBit(markNode);
				Cell np = (Cell)ni.getProto();

				// determine original size of cell
				Undo.Change change = np.getChange();
				double flx, fhx, fly, fhy;
				if (change != null && change.getType() == Undo.Type.CELLMOD)
				{
					// modification changes carry original size
					flx = change.getA1();   fhx = change.getA2();
					fly = change.getA3();   fhy = change.getA4();
				} else
				{
					// creation changes have no original size: use current size
					Rectangle2D oldCellBounds = new Rectangle2D.Double();
					flx = oldCellBounds.getMinX();   fhx = oldCellBounds.getMaxX();
					fly = oldCellBounds.getMinY();   fhy = oldCellBounds.getMaxY();
				}

				Rectangle2D cellBounds = np.getBounds();
				double dSX = DBMath.round(cellBounds.getWidth() - ni.getXSize());
				double dSY = DBMath.round(cellBounds.getHeight() - ni.getYSize());
				if (ni.isMirroredAboutYAxis()) dSX = -dSX;
				if (ni.isMirroredAboutXAxis()) dSY = -dSY;
				if (alterNodeInst(ni, 0, 0, dSX, dSY, 0, true)) forcedLook = true;
				foundone = true;
			}
		}

		// now change the arcs in the nodes in this cell that changed
		List nodesThatChanged = new ArrayList();
		for(Iterator it = start.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			if (ni.isBit(touchNode)) nodesThatChanged.add(ni);
		}
		foundone = true;
		while (foundone)
		{
			foundone = false;
			for(Iterator it = nodesThatChanged.iterator(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				if (!ni.isBit(touchNode)) continue;
				ni.clearBit(touchNode);
				if (modNodeArcs(ni, 0, 0, 0, false, false)) forcedLook = true;
				foundone = true;
			}
		}

		// now change the size of this cell
		Rectangle2D oldCellBounds = new Rectangle2D.Double();
		oldCellBounds.setRect(start.getRememberedBounds());
		Rectangle2D cellBounds = start.getBounds();

		// quit if it has not changed
		if (oldCellBounds.equals(cellBounds) && !forcedLook)
		{
			start.setBit(cellModFlag);
			return false;
		}

		// update the cell size
		Undo.modifyCell(start, oldCellBounds.getMinX(), oldCellBounds.getMaxX(), oldCellBounds.getMinY(), oldCellBounds.getMaxY());
		return true;
	}
}
