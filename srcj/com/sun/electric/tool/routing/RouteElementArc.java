package com.sun.electric.tool.routing;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.Dimension2D;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.Job;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: root
 * Date: Jul 19, 2004
 * Time: 9:16:17 PM
 * To change this template use File | Settings | File Templates.
 */
public class RouteElementArc extends RouteElement {

    // ---- New Arc info ----
    /** Arc type to create */                       private ArcProto arcProto;
    /** width of arc */                             private double arcWidth;
    /** Head of arc */                              private RouteElementPort headRE;
    /** Tail of arc */                              private RouteElementPort tailRE;
    /** Head connecting point */                    private Point2D headConnPoint;
    /** Tail connecting point */                    private Point2D tailConnPoint;
    /** Name of arc */                              private String arcName;
    /** Angle of arc */                             private int arcAngle;

    /** This contains the newly create instance, or the instance to delete */ private ArcInst arcInst;

    /**
     * Private Constructor
     * @param action the action this RouteElementAction will do.
     */
    private RouteElementArc(RouteElementAction action, Cell cell) { super(action, cell); }

    /**
     * Factory method for making a newArc RouteElement
     * @param ap Type of ArcInst to make
     * @param headRE RouteElement (must be newNode or existingPortInst) at head of arc
     * @param tailRE RouteElement (must be newNode or existingPortInst) at tail or arc
     */
    public static RouteElementArc newArc(Cell cell, ArcProto ap, double arcWidth, RouteElementPort headRE, RouteElementPort tailRE,
                                         Point2D headConnPoint, Point2D tailConnPoint, String name) {
        RouteElementArc e = new RouteElementArc(RouteElementAction.newArc, cell);
        e.arcProto = ap;
        e.arcWidth = arcWidth;
        e.headRE = headRE;
        e.tailRE = tailRE;
        e.arcName = name;
        if (headRE.getAction() != RouteElement.RouteElementAction.newNode &&
            headRE.getAction() != RouteElement.RouteElementAction.existingPortInst)
            System.out.println("  ERROR: headRE of newArc RouteElementArc must be newNode or existingPortInst");
        if (tailRE.getAction() != RouteElement.RouteElementAction.newNode &&
            tailRE.getAction() != RouteElement.RouteElementAction.existingPortInst)
            System.out.println("  ERROR: tailRE of newArc RouteElementArc must be newNode or existingPortInst");
        headRE.addConnectingNewArc(e);
        tailRE.addConnectingNewArc(e);
        e.headConnPoint = headConnPoint;
        e.tailConnPoint = tailConnPoint;
        assert(e.headConnPoint != null);
        assert(e.tailConnPoint != null);
        e.arcAngle = 0;
        e.arcInst = null;
        return e;
    }

    /**
     * Factory method for making a deleteArc RouteElement
     * @param arcInstToDelete the arcInst to delete
     */
    public static RouteElementArc deleteArc(ArcInst arcInstToDelete) {
        RouteElementArc e = new RouteElementArc(RouteElementAction.deleteArc, arcInstToDelete.getParent());
        e.arcProto = arcInstToDelete.getProto();
        e.arcWidth = arcInstToDelete.getWidth();
        e.headRE = RouteElementPort.existingPortInst(arcInstToDelete.getHead().getPortInst());
        e.tailRE = RouteElementPort.existingPortInst(arcInstToDelete.getTail().getPortInst());
        e.arcName = arcInstToDelete.getName();
        e.headConnPoint = arcInstToDelete.getHead().getLocation();
        e.tailConnPoint = arcInstToDelete.getTail().getLocation();
        e.arcAngle = 0;
        e.arcInst = arcInstToDelete;
        return e;
    }

    /**
     * Get the arc proto to be created/deleted.
     * @return the arc proto.
     */
    public ArcProto getArcProto() { return arcProto; }

    public RouteElementPort getHead() { return headRE; }
    public RouteElementPort getTail() { return tailRE; }
    public Point2D getHeadConnPoint() { return headConnPoint; }
    public Point2D getTailConnPoint() { return tailConnPoint; }

    /**
     * Return arc width
     */
    public double getArcWidth() { return arcWidth; }

    /**
     * Return arc width.
     * This returns the arc width taking into account any offset
     */
    public double getOffsetArcWidth() {
        return arcWidth - arcProto.getWidthOffset();
    }

    /**
     * Set the arc width if this is a newArc RouteElement, otherwise does nothing.
     * This is the non-offset width (i.e. the bloated width).
     */
    public void setArcWidth(double width) {
        if (getAction() == RouteElementAction.newArc)
            arcWidth = width;
    }

    /**
     * Set the arc width if this is a newArc RouteElement, otherwise does nothing.
     * This is offset arc width (i.e. what the user sees).
     */
    public void setOffsetArcWidth(double width) {
        if (getAction() == RouteElementAction.newArc)
            arcWidth = width + arcProto.getWidthOffset();
    }

    /**
     * Set a newArc's angle. This only does something if both the
     * head and tail of the arc are coincident points. This does
     * nothing if the RouteElement is not a newArc
     * @param angle the angle, in tenth degrees
     */
    public void setArcAngle(int angle) {
        if (getAction() == RouteElementAction.newArc)
            arcAngle = angle;
    }

    /**
     * Return true if the new arc is a vertical arc, false otherwise
     */
    public boolean isArcVertical() {
        Point2D head = headConnPoint;
        Point2D tail = tailConnPoint;
        if (head == null) head = headRE.getLocation();
        if (tail == null) tail = tailRE.getLocation();
        if ((head == null) || (tail == null)) return false;
        if (head.getX() == tail.getX()) return true;
        return false;
    }

    /**
     * Return true if the new arc is a horizontal arc, false otherwise
     */
    public boolean isArcHorizontal() {
        Point2D head = headConnPoint;
        Point2D tail = tailConnPoint;
        if (head == null) head = headRE.getLocation();
        if (tail == null) tail = tailRE.getLocation();
        if ((head == null) || (tail == null)) return false;
        if (head.getY() == tail.getY()) return true;
        return false;
    }

    /**
     * Used to update end points of new arc if they change
     * Only valid if called on newArcs, does nothing otherwise.
     */
    public void replaceArcEnd(RouteElementPort oldEnd, RouteElementPort newEnd) {
        if (getAction() == RouteElementAction.newArc) {
            if (headRE == oldEnd) {
                headRE = newEnd;
                // update book-keeping
                oldEnd.removeConnectingNewArc(this);
                newEnd.addConnectingNewArc(this);
            }
            if (tailRE == oldEnd) {
                tailRE = newEnd;
                // update book-keeping
                oldEnd.removeConnectingNewArc(this);
                newEnd.addConnectingNewArc(this);
            }
        }
    }

    /**
     * Perform the action specified by RouteElementAction <i>action</i>.
     * Note that this method performs database editing, and should only
     * be called from within a Job.
     * @return the object created, or null if deleted or nothing done.
     */
    public ElectricObject doAction() {

        Job.checkChanging();

        if (isDone()) return null;

        if (getAction() == RouteElementAction.newArc) {
            PortInst headPi = headRE.getPortInst();
            PortInst tailPi = tailRE.getPortInst();
            Point2D headPoint = headConnPoint;
            Point2D tailPoint = tailConnPoint;

			// special case when routing to expandable gate (and, or, mux, etc.)
			Poly headPoly = headPi.getPoly();
			if (!headPoly.isInside(headPoint))
			{
				NodeInst headNi = headPi.getNodeInst();
				NodeProto np = headNi.getProto();
				if (np instanceof PrimitiveNode)
				{
					PrimitiveNode pNp = (PrimitiveNode)np;
					Dimension2D autoGrowth = pNp.getAutoGrowth();
					if (autoGrowth != null)
					{
						// grow the node to allow expandable port to fit
						headNi.modifyInstance(0, 0, autoGrowth.getWidth(), autoGrowth.getHeight(), 0);
					}
				}
			}
            headPoly = headPi.getPoly();
            if (!headPoly.isInside(headPoint)) {
                // can't connect
                System.out.println("Arc head point ("+headPoint.getX()+","+headPoint.getY()+") not inside port "+headPi);
                return null;
            }
			Poly tailPoly = tailPi.getPoly();
			if (!tailPoly.isInside(tailPoint))
			{
				NodeInst tailNi = tailPi.getNodeInst();
				NodeProto np = tailNi.getProto();
				if (np instanceof PrimitiveNode)
				{
					PrimitiveNode pNp = (PrimitiveNode)np;
					Dimension2D autoGrowth = pNp.getAutoGrowth();
					if (autoGrowth != null)
					{
						// grow the node to allow expandable port to fit
						tailNi.modifyInstance(0, 0, autoGrowth.getWidth(), autoGrowth.getHeight(), 0);
					}
				}
			}
            tailPoly = tailPi.getPoly();
            if (!tailPoly.isInside(tailPoint)) {
                // can't connect
                System.out.println("Arc tail point ("+tailPoint.getX()+","+tailPoint.getY()+") not inside port "+tailPi);
                return null;
            }

			// now run the arc
            ArcInst newAi = ArcInst.makeInstance(arcProto, arcWidth, headPi, headPoint, tailPi, tailPoint, arcName);
            if (arcAngle != 0)
                newAi.setAngle(arcAngle);
            setDone();
            arcInst = newAi;
            return newAi;
        }
        if (getAction() == RouteElementAction.deleteArc) {
            // delete existing arc
            arcInst.kill();
            setDone();
        }
        return null;
    }

    /**
     * Adds RouteElement to highlights
     */
    public void addHighlightArea() {

        if (!isShowHighlight()) return;

        if (getAction() == RouteElementAction.newArc) {
            // figure out highlight area based on arc width and start and end locations
            Point2D headPoint = headConnPoint;
            Point2D tailPoint = tailConnPoint;
            boolean endsExtend = ((PrimitiveArc)arcProto).isExtended();
            double offset = 0.5*getOffsetArcWidth();
            double offsetX, offsetY;
            double offsetEnds = endsExtend ? offset : 0;
            Point2D head1, head2, tail1, tail2;
            if (headPoint.getX() == tailPoint.getX()) {
                // vertical arc
                if (headPoint.getY() > tailPoint.getY()) {
                    offsetX = offset;
                    offsetY = offsetEnds;
                } else {
                    offsetX = offset;
                    offsetY = -offsetEnds;
                }
                head1 = new Point2D.Double(headPoint.getX()+offsetX, headPoint.getY()+offsetY);
                head2 = new Point2D.Double(headPoint.getX()-offsetX, headPoint.getY()+offsetY);
                tail1 = new Point2D.Double(tailPoint.getX()+offsetX, tailPoint.getY()-offsetY);
                tail2 = new Point2D.Double(tailPoint.getX()-offsetX, tailPoint.getY()-offsetY);
            } else {
                assert(headPoint.getY() == tailPoint.getY());
                if (headPoint.getX() > tailPoint.getX()) {
                    offsetX = offsetEnds;
                    offsetY = offset;
                } else {
                    offsetX = -offsetEnds;
                    offsetY = offset;
                }
                head1 = new Point2D.Double(headPoint.getX()+offsetX, headPoint.getY()+offsetY);
                head2 = new Point2D.Double(headPoint.getX()+offsetX, headPoint.getY()-offsetY);
                tail1 = new Point2D.Double(tailPoint.getX()-offsetX, tailPoint.getY()+offsetY);
                tail2 = new Point2D.Double(tailPoint.getX()-offsetX, tailPoint.getY()-offsetY);
            }
            Cell cell = getCell();
            Highlight.addLine(head1, tail1, cell);
            //Highlight.addLine(headPoint, tailPoint, cell);
            Highlight.addLine(head2, tail2, cell);
            Highlight.addLine(head1, head2, cell);
            Highlight.addLine(tail1, tail2, cell);
        }
        if (getAction() == RouteElementAction.deleteArc) {
            Highlight.addElectricObject(arcInst, getCell());
        }
    }

    /** Return string decribing the RouteElement */
    public String toString() {
        if (getAction() == RouteElementAction.newArc) {
            return "RouteElementArc newArc "+arcProto+",\nhead: "+headRE+"\ntail: "+tailRE;
        }
        else if (getAction() == RouteElementAction.deleteArc) {
            return "RouteElement deleteArc "+arcInst;
        }
        return "RouteElement bad action";
    }

}
