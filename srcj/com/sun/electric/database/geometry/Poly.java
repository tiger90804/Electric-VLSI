/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Poly.java
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
package com.sun.electric.database.geometry;

import com.sun.electric.technology.Layer;
import com.sun.electric.database.geometry.EMath;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.tool.user.ui.EditWindow;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.font.GlyphVector;

/**
 * The Poly class describes an extended set of points
 * that can be outlines, filled shapes, curves, text, and more.
 * The Poly also contains a Layer and some connectivity information.
 */
public class Poly implements Shape
{
	/**
	 * Type is a typesafe enum class that describes the nature of a Poly.
	 */
	public static class Type
	{
		private String name;

		private Type(String name) { this.name = name; }

		public String toString() { return "Poly.Type "+name; }

		// ************************ polygons ************************
		/**
		 * Describes a closed polygon which is filled in.
		 */
		public static final Type FILLED = new Type("filled");
		/**
		 * Describes a closed polygon with only the outline drawn.
		 */
		public static final Type CLOSED = new Type("closed");
		/**
		 * Describes a closed rectangle with the outline drawn and an "X" drawn through it.
		 */
		public static final Type CROSSED = new Type("crossed");

		// ************************ lines ************************
		/**
		 * Describes an open outline.
		 * The last point is not implicitly connected to the first point.
		 */
		public static final Type OPENED = new Type("opened");
		/**
		 * Describes an open outline, drawn with a dotted texture.
		 * The last point is not implicitly connected to the first point.
		 */
		public static final Type OPENEDT1 = new Type("opened-dotted");
		/**
		 * Describes an open outline, drawn with a dashed texture. 
		 * The last point is not implicitly connected to the first point.
		 */
		public static final Type OPENEDT2 = new Type("opened-dashed");
		/**
		 * Describes an open outline, drawn with thicker lines.
		 * The last point is not implicitly connected to the first point.
		 */
		public static final Type OPENEDT3 = new Type("opened-thick");
		/**
		 * Describes an open outline pushed outward by 1 screen pixel.
		 * The last point is not implicitly connected to the first point.
		 * This is useful in highlighting objects where the highlight line belongs outside of the highlighted object.
		 */
		public static final Type OPENEDO1 = new Type("opened-out1");
		/**
		 * Describes a vector endpoint pairs, solid.
		 * There must be an even number of points in the Poly so that vectors can be drawn from point 0 to 1,
		 * then from point 2 to 3, etc.
		 */
		public static final Type VECTORS = new Type("vectors");

		// ************************ curves ************************
		/**
		 * Describes a circle (only the outline is drawn).
		 * The first point is the center of the circle and the second point is on the edge, thus defining the radius.
		 * This second point should be on the same horizontal level as the radius point to make radius computation easier.
		 */
		public static final Type CIRCLE = new Type("circle");
		/**
		 * Describes a circle, drawn with thick lines (only the outline is drawn).
		 * The first point is the center of the circle and the second point is on the edge, thus defining the radius.
		 * This second point should be on the same horizontal level as the radius point to make radius computation easier.
		 */
		public static final Type THICKCIRCLE = new Type("thick-circle");
		/**
		 * Describes a filled circle.
		 * The first point is the center of the circle and the second point is on the edge, thus defining the radius.
		 * This second point should be on the same horizontal level as the radius point to make radius computation easier.
		 */
		public static final Type DISC = new Type("disc");
		/**
		 * Describes an arc of a circle.
		 * The first point is the center of the circle, the second point is the start of the arc, and
		 * the third point is the end of the arc.
		 * The arc will be drawn counter-clockwise from the start point to the end point.
		 */
		public static final Type CIRCLEARC = new Type("circle-arc");
		/**
		 * Describes an arc of a circle, drawn with thick lines.
		 * The first point is the center of the circle, the second point is the start of the arc, and
		 * the third point is the end of the arc.
		 * The arc will be drawn counter-clockwise from the start point to the end point.
		 */
		public static final Type THICKCIRCLEARC = new Type("thick-circle-arc");

		// ************************ text ************************
		/**
		 * Describes text that should be centered about the Poly point.
		 * Only one point need be specified.
		 */
		public static final Type TEXTCENT = new Type("text-center");
		/**
		 * Describes text that should be placed so that the Poly point is at the top-center.
		 * Only one point need be specified, and the text will be below that point.
		 */
		public static final Type TEXTTOP = new Type("text-top");
		/**
		 * Describes text that should be placed so that the Poly point is at the bottom-center.
		 * Only one point need be specified, and the text will be above that point.
		 */
		public static final Type TEXTBOT = new Type("text-bottom");
		/**
		 * Describes text that should be placed so that the Poly point is at the left-center.
		 * Only one point need be specified, and the text will be to the right of that point.
		 */
		public static final Type TEXTLEFT = new Type("text-left");
		/**
		 * Describes text that should be placed so that the Poly point is at the right-center.
		 * Only one point need be specified, and the text will be to the left of that point.
		 */
		public static final Type TEXTRIGHT = new Type("text-right");
		/**
		 * Describes text that should be placed so that the Poly point is at the upper-left.
		 * Only one point need be specified, and the text will be to the lower-right of that point.
		 */
		public static final Type TEXTTOPLEFT = new Type("text-topleft");
		/**
		 * Describes text that should be placed so that the Poly point is at the lower-left.
		 * Only one point need be specified, and the text will be to the upper-right of that point.
		 * This is the normal starting point for most text.
		 */
		public static final Type TEXTBOTLEFT = new Type("text-botleft");
		/**
		 * Describes text that should be placed so that the Poly point is at the upper-right.
		 * Only one point need be specified, and the text will be to the lower-left of that point.
		 */
		public static final Type TEXTTOPRIGHT = new Type("text-topright");
		/**
		 * Describes text that should be placed so that the Poly point is at the lower-right.
		 * Only one point need be specified, and the text will be to the upper-left of that point.
		 */
		public static final Type TEXTBOTRIGHT = new Type("text-botright");
		/**
		 * Describes text that is centered in the Poly and must remain inside.
		 * If the letters do not fit, a smaller font will be used, and if that still does not work,
		 * any letters that cannot fit are not written.
		 * The Poly coordinates must define an area for the text to live in.
		 */
		public static final Type TEXTBOX = new Type("text-box");

		// ************************ miscellaneous ************************
		/**
		 * Describes a small cross, drawn at the specified location.
		 * Typically there will be only one point in this polygon
		 * but if there are more they are averaged and the cross is drawn in the center.
		 */
		public static final Type CROSS = new Type("cross");
		/**
		 * Describes a big cross, drawn at the specified location.
		 * Typically there will be only one point in this polygon
		 * but if there are more they are averaged and the cross is drawn in the center.
		 */
		public static final Type BIGCROSS = new Type("big-cross");
	}

	/** the layer (used for graphics) */					private Layer layer;
	/** the points */										private Point2D.Double points[];
	/** the bounds of the points */							private Rectangle2D.Double bounds;
	/** the style (outline, text, lines, etc.) */			private Poly.Type style;
	/** the string (if of type TEXT) */						private String string;
	/** the text descriptor (if of type TEXT) */			private TextDescriptor descript;
	/** the variable (if of type TEXT) */					private Variable var;
	/** the PortProto (if from a node or TEXT) */			private PortProto pp;


	/**
	 * The constructor creates a new Poly given an array of points.
	 * @param points the array of coordinates.
	 */
	public Poly(Point2D.Double [] points)
	{
		this.points = points;
		layer = null;
		style = null;
		bounds = null;
	}

	/**
	 * The constructor creates a new Poly that describes a rectangle.
	 * @param cX the center X coordinate of the rectangle.
	 * @param cY the center Y coordinate of the rectangle.
	 * @param width the width of the rectangle.
	 * @param height the height of the rectangle.
	 */
	public Poly(double cX, double cY, double width, double height)
	{
		double halfWidth = width / 2;
		double halfHeight = height / 2;
		this.points = new Point2D.Double[] {
			new Point2D.Double(EMath.smooth(cX-halfWidth), EMath.smooth(cY-halfHeight)),
			new Point2D.Double(EMath.smooth(cX+halfWidth), EMath.smooth(cY-halfHeight)),
			new Point2D.Double(EMath.smooth(cX+halfWidth), EMath.smooth(cY+halfHeight)),
			new Point2D.Double(EMath.smooth(cX-halfWidth), EMath.smooth(cY+halfHeight))};
		layer = null;
		style = null;
		bounds = null;
	}

	/**
	 * Routine to return the layer associated with this Poly.
	 * @return the layer associated with this Poly.
	 */
	public Layer getLayer() { return layer; }

	/**
	 * Routine to set the layer associated with this Poly.
	 * @param layer the layer associated with this Poly.
	 */
	public void setLayer(Layer layer) { this.layer = layer; }

	/**
	 * Routine to return the style associated with this Poly.
	 * The style controls how the points are interpreted (FILLED, CIRCLE, etc.)
	 * @return the style associated with this Poly.
	 */
	public Poly.Type getStyle() { return style; }

	/**
	 * Routine to set the style associated with this Poly.
	 * The style controls how the points are interpreted (FILLED, CIRCLE, etc.)
	 * @param style the style associated with this Poly.
	 */
	public void setStyle(Poly.Type style) { this.style = style; }

	/**
	 * Routine to return the String associated with this Poly.
	 * This only applies to text Polys which display a message.
	 * @return the String associated with this Poly.
	 */
	public String getString() { return string; }

	/**
	 * Routine to set the String associated with this Poly.
	 * This only applies to text Polys which display a message.
	 * @param string the String associated with this Poly.
	 */
	public void setString(String string) { this.string = string; }

	/**
	 * Routine to return the Text Descriptor associated with this Poly.
	 * This only applies to text Polys which display a message.
	 * Only the size, face, italic, bold, and underline fields are relevant.
	 * @return the Text Descriptor associated with this Poly.
	 */
	public TextDescriptor getTextDescriptor() { return descript; }

	/**
	 * Routine to set the Text Descriptor associated with this Poly.
	 * This only applies to text Polys which display a message.
	 * Only the size, face, italic, bold, and underline fields are relevant.
	 * @param descript the Text Descriptor associated with this Poly.
	 */
	public void setTextDescriptor(TextDescriptor descript) { this.descript = descript; }

	/**
	 * Routine to return the Variable associated with this Poly.
	 * This only applies to text Polys which display a message.
	 * @return the Variable associated with this Poly.
	 */
	public Variable getVariable() { return var; }

	/**
	 * Routine to set the Variable associated with this Poly.
	 * This only applies to text Polys which display a message.
	 * @param var the Variable associated with this Poly.
	 */
	public void setVariable(Variable var) { this.var = var; }

	/**
	 * Routine to return the PortProto associated with this Poly.
	 * This applies to ports on Nodes and Exports on Cells.
	 * @return the PortProto associated with this Poly.
	 */
	public PortProto getPort() { return pp; }

	/**
	 * Routine to set the PortProto associated with this Poly.
	 * This applies to ports on Nodes and Exports on Cells.
	 * @param pp the PortProto associated with this Poly.
	 */
	public void setPort(PortProto pp) { this.pp = pp; }

	/**
	 * Routine to return the points associated with this Poly.
	 * @return the points associated with this Poly.
	 */
	public Point2D.Double [] getPoints() { return points; }

	/**
	 * Routine to transformed the points in this Poly.
	 * @param af transformation to apply.
	 */
	public void transform(AffineTransform af)
	{
		// special case for Poly type CIRCLEARC and THICKCIRCLEARC: if transposing, reverse points
		if (style == Type.CIRCLEARC || style == Type.THICKCIRCLEARC)
		{
			double det = af.getDeterminant();
			if (det < 0) for(int i=0; i<points.length; i += 3)
			{
				double x = points[i+1].getX();
				double y = points[i+1].getY();
				points[i+1].setLocation(points[i+2].getX(), points[i+2].getY());
				points[i+2].setLocation(x, y);
			}
		}
		af.transform(points, 0, points, 0, points.length);
//		af.transform(points, points);

		// smooth the results
		for(int i=0; i<points.length; i++)
			points[i].setLocation(EMath.smooth(points[i].getX()), EMath.smooth(points[i].getY()));
		bounds = null;
	}

	/**
	 * Routine to return a Rectangle that describes the orthogonal box in this Poly.
	 * @return the Rectangle that describes this Poly.
	 * If the Poly is not an orthogonal box, returns null.
	 */
	public Rectangle2D.Double getBox()
	{
		// closed boxes must have exactly four points
		if (points.length == 4)
		{
			// only closed polygons and text can be boxes
			if (style != Type.FILLED && style != Type.CLOSED && style != Type.TEXTBOX) return null;
		} else if (points.length == 5)
		{
			if (style != Type.OPENED && style != Type.OPENEDT1 && style != Type.OPENEDT2 &&
				style != Type.OPENEDT3 && style != Type.OPENEDO1) return null;
			if (points[0].getX() != points[4].getX() || points[0].getY() != points[4].getY()) return null;
		} else return null;

		// make sure the polygon is rectangular and orthogonal
		if (points[0].getX() == points[1].getX() && points[2].getX() == points[3].getX() &&
			points[0].getY() == points[3].getY() && points[1].getY() == points[2].getY())
		{
			double lX = Math.min(points[2].getX(), points[0].getX());
			double lY = Math.min(points[1].getY(), points[0].getY());
			double sX = Math.abs(points[2].getX() - points[0].getX());
			double sY = Math.abs(points[1].getY() - points[0].getY());
			return new Rectangle2D.Double(EMath.smooth(lX), EMath.smooth(lY), EMath.smooth(sX), EMath.smooth(sY));
		}
		if (points[0].getX() == points[3].getX() && points[1].getX() == points[2].getX() &&
			points[0].getY() == points[1].getY() && points[2].getY() == points[3].getY())
		{
			double lX = Math.min(points[1].getX(), points[0].getX());
			double lY = Math.min(points[2].getY(), points[0].getY());
			double sX = Math.abs(points[1].getX() - points[0].getX());
			double sY = Math.abs(points[2].getY() - points[0].getY());
			return new Rectangle2D.Double(EMath.smooth(lX), EMath.smooth(lY), EMath.smooth(sX), EMath.smooth(sY));
		}
		return null;
	}

	/**
	 * Routine to tell whether a coordinate is inside of this Poly.
	 * @param pt the point in question.
	 * @return true if the point is inside of this Poly.
	 */
	public boolean isInside(Point2D.Double pt)
	{
		if (style == Type.FILLED || style == Type.CLOSED || style == Type.CROSSED ||
			style == Type.TEXTCENT || style == Type.TEXTTOP ||
			style == Type.TEXTBOT || style == Type.TEXTLEFT ||
			style == Type.TEXTRIGHT || style == Type.TEXTTOPLEFT ||
			style == Type.TEXTBOTLEFT || style == Type.TEXTTOPRIGHT ||
			style == Type.TEXTBOTRIGHT || style == Type.TEXTBOX)
		{
			// check rectangular case for containment
			Rectangle2D.Double bounds = getBox();
			if (bounds != null)
			{
				if (EMath.pointInRect(pt, bounds)) return true;
				return false;
			}

			// general polygon containment by summing angles to vertices
			double ang = 0;
			Point2D.Double lastPoint = points[points.length-1];
			if (pt.equals(lastPoint)) return true;
			int lastp = EMath.figureAngle(pt, lastPoint);
			for(int i=0; i<points.length; i++)
			{
				Point2D.Double thisPoint = points[i];
				if (pt.equals(thisPoint)) return true;
				int thisp = EMath.figureAngle(pt, thisPoint);
				int tang = lastp - thisp;
				if (tang < -1800) tang += 3600;
				if (tang > 1800) tang -= 3600;
				ang += tang;
				lastp = thisp;
			}
			if (Math.abs(ang) <= points.length) return false;
			return true;
		}

		if (style == Type.CROSS || style == Type.BIGCROSS)
		{
			if (getCenterX() == pt.getX() && getCenterY() == pt.getY()) return true;
			return false;
		}

		if (style == Type.OPENED || style == Type.OPENEDT1 || style == Type.OPENEDT2 ||
			style == Type.OPENEDT3 || style == Type.VECTORS)
		{
			// first look for trivial inclusion by being a vertex
			for(int i=0; i<points.length; i++)
				if (pt.equals(points[i])) return true;

			// see if the point is on one of the edges
			if (style == Type.VECTORS)
			{
				for(int i=0; i<points.length; i += 2)
					if (EMath.isOnLine(points[i], points[i+1], pt)) return true;
			} else
			{
				for(int i=1; i<points.length; i++)
					if (EMath.isOnLine(points[i-1], points[i], pt)) return true;
			}
			return false;
		}

		if (style == Type.CIRCLE || style == Type.THICKCIRCLE || style == Type.DISC)
		{
			double dist = points[0].distance(points[1]);
			double odist = points[0].distance(pt);
			if (odist < dist) return true;
			return false;
		}

		if (style == Type.CIRCLEARC || style == Type.THICKCIRCLEARC)
		{
			// first see if the point is at the proper angle from the center of the arc
			int ang = EMath.figureAngle(points[0], pt);
			int endangle = EMath.figureAngle(points[0], points[1]);
			int startangle = EMath.figureAngle(points[0], points[2]);
			double angrange;
			if (endangle > startangle)
			{
				if (ang < startangle || ang > endangle) return false;
				angrange = endangle - startangle;
			} else
			{
				if (ang < startangle && ang > endangle) return false;
				angrange = 3600 - startangle + endangle;
			}

			// now see if the point is the proper distance from the center of the arc
			double dist = points[0].distance(pt);
			double wantdist;
			if (ang == startangle || angrange == 0)
			{
				wantdist = points[0].distance(points[1]);
			} else if (ang == endangle)
			{
				wantdist = points[0].distance(points[2]);
			} else
			{
				double startdist = points[0].distance(points[1]);
				double enddist = points[0].distance(points[2]);
				if (enddist == startdist) wantdist = startdist; else
				{
					wantdist = startdist + (ang - startangle) / angrange *
						(enddist - startdist);
				}
			}
			if (dist == wantdist) return true;
			return false;
		}

		// I give up
		return false;
	}

	/**
	 * Routine to convert text Polys to their precise bounds in a given window.
	 * @param wnd the window.
	 */
	public void setExactTextBounds(EditWindow wnd)
	{
		int numLines = 1;
		if (var != null) numLines = var.getLength();

		Rectangle2D.Double bounds = getBounds2DDouble();
		double lX = bounds.getMinX();
		double hX = bounds.getMaxX();
		double lY = bounds.getMinY();
		double hY = bounds.getMaxY();
		GlyphVector gv = wnd.getGlyphs(getString(), getTextDescriptor());
		Rectangle2D glyphBounds = gv.getVisualBounds();
		Point2D corner = wnd.getTextCorner(gv, getStyle(), numLines, lX, hX, lY, hY);
		double cX = corner.getX();
		double cY = corner.getY();

		double textScale = wnd.getTextScale(gv, getStyle(), lX, hX, lY, hY);
		double width = glyphBounds.getWidth() * textScale;
		double height = glyphBounds.getHeight() * textScale * numLines;
		points = new Point2D.Double[] {
			new Point2D.Double(cX, cY),
			new Point2D.Double(cX+width, cY),
			new Point2D.Double(cX+width, cY+height),
			new Point2D.Double(cX, cY+height)};
		this.bounds = null;
	}

	/**
	 * Routine to report the distance of a rectangle or point to this Poly.
	 * @param otherBounds the area to test for distance to the Poly.
	 * @return the distance of the area to the Poly.
	 * The routine returns a negative amount if the point/area is a direct hit on or inside
	 * the polygon (the more negative, the closer to the center).
	 */
	public double polyDistance(Rectangle2D.Double otherBounds)
	{
		// get information about this Poly
		Rectangle2D.Double polyBounds = getBounds2DDouble();
		double polyCX = polyBounds.getCenterX();
		double polyCY = polyBounds.getCenterY();
		Point2D.Double polyCenter = new Point2D.Double(polyCX, polyCY);
		Type localStyle = style;
		boolean thisIsPoint = (polyBounds.width == 0 && polyBounds.height == 0);

		// get information about the other area being tested
		boolean otherIsPoint = (otherBounds.width == 0 && otherBounds.height == 0);
		double otherCX = otherBounds.getCenterX();
		double otherCY = otherBounds.getCenterY();
		Point2D.Double otherPt = new Point2D.Double(otherCX, otherCY);

		// handle single point polygons
		if (thisIsPoint)
		{
			if (otherIsPoint)
			{
				if (polyCX == otherCX && polyCY == otherCY) return Double.MIN_VALUE;
			} else
			{
				if (otherBounds.contains(polyCenter)) return Double.MIN_VALUE;
			}
			return otherPt.distance(polyCenter);
		}

		// handle polygons that are filled in
		if (localStyle == Type.FILLED || localStyle == Type.CROSSED ||
			localStyle == Type.TEXTCENT || localStyle == Type.TEXTTOP ||
			localStyle == Type.TEXTBOT || localStyle == Type.TEXTLEFT ||
			localStyle == Type.TEXTRIGHT || localStyle == Type.TEXTTOPLEFT ||
			localStyle == Type.TEXTBOTLEFT || localStyle == Type.TEXTTOPRIGHT ||
			localStyle == Type.TEXTBOTRIGHT || localStyle == Type.TEXTBOX)
		{
			if (otherIsPoint)
			{
				// give special returned value if point is a direct hit
				if (isInside(otherPt))
				{
					return otherPt.distance(polyCenter) - Double.MAX_VALUE;
				}

				// if polygon is a box, use M.B.R. information
				Rectangle2D.Double box = getBox();
				if (box != null)
				{
					if (otherCX > box.getMaxX()) polyCX = otherCX - box.getMaxX(); else
						if (otherCX < box.getMinX()) polyCX = box.getMinX() - otherCX; else
							polyCX = 0;
					if (otherCY > box.getMaxY()) polyCY = otherCY - box.getMaxY(); else
						if (otherCY < box.getMinY()) polyCY = box.getMinY() - otherCY; else
							polyCY = 0;
					if (polyCX == 0 || polyCY == 0) return polyCX + polyCY;
					polyCenter.setLocation(polyCX, polyCY);
					return polyCenter.distance(new Point2D.Double(0,0));
				}

				// point is outside of irregular polygon: fall into to next case
				localStyle = Type.CLOSED;
			} else
			{
				if (otherBounds.intersects(polyBounds)) return Double.MIN_VALUE;
				return otherPt.distance(polyCenter);
			}
		}

		// handle closed outline figures
		if (localStyle == Type.CLOSED)
		{
			if (otherIsPoint)
			{
				double bestDist = Double.MAX_VALUE;
				Point2D.Double lastPt = points[points.length-1];
				for(int i=0; i<points.length; i++)
				{
					if (i != 0) lastPt = points[i-1];
					Point2D.Double thisPt = points[i];

					// compute distance of close point to "otherPt"
					double dist = EMath.distToLine(lastPt, thisPt, otherPt);
					if (dist < bestDist) bestDist = dist;
				}
				return bestDist;
			} else
			{
				if (otherBounds.intersects(polyBounds)) return Double.MIN_VALUE;
				return otherPt.distance(polyCenter);
			}
		}

		// handle opened outline figures
		if (localStyle == Type.OPENED || localStyle == Type.OPENEDT1 ||
			localStyle == Type.OPENEDT2 || localStyle == Type.OPENEDT3 ||
			localStyle == Type.OPENEDO1)
		{
			if (otherIsPoint)
			{
				double bestDist = Double.MAX_VALUE;
				for(int i=1; i<points.length; i++)
				{
					Point2D.Double lastPt = points[i-1];
					Point2D.Double thisPt = points[i];

					// compute distance of close point to "otherPt"
					double dist = EMath.distToLine(lastPt, thisPt, otherPt);
					if (dist < bestDist) bestDist = dist;
				}
				return bestDist;
			} else
			{
				if (otherBounds.intersects(polyBounds)) return Double.MIN_VALUE;
				return otherPt.distance(polyCenter);
			}
		}

		// handle outline vector lists
		if (localStyle == Type.VECTORS)
		{
			if (otherIsPoint)
			{
				double bestDist = Double.MAX_VALUE;
				for(int i=0; i<points.length; i += 2)
				{
					Point2D.Double lastPt = points[i];
					Point2D.Double thisPt = points[i+1];

					// compute distance of close point to "otherPt"
					double dist = EMath.distToLine(lastPt, thisPt, otherPt);
					if (dist < bestDist) bestDist = dist;
				}
				return bestDist;
			} else
			{
				if (otherBounds.intersects(polyBounds)) return Double.MIN_VALUE;
				return otherPt.distance(polyCenter);
			}
		}

		// handle circular objects
		if (localStyle == Type.CIRCLE || localStyle == Type.THICKCIRCLE || localStyle == Type.DISC)
		{
			if (otherIsPoint)
			{
				double odist = points[0].distance(points[1]);
				double dist = points[0].distance(otherPt);
				if (localStyle == Type.DISC && dist < odist) return dist-Double.MAX_VALUE;
				return Math.abs(dist-odist);
			}
		}
		if (localStyle == Type.CIRCLEARC || localStyle == Type.THICKCIRCLEARC)
		{
			if (otherIsPoint)
			{
				// determine closest point to ends of arc
				double sdist = otherPt.distance(points[1]);
				double edist = otherPt.distance(points[2]);
				double dist = Math.min(sdist, edist);

				// see if the point is in the segment of the arc
				int pang = EMath.figureAngle(points[0], otherPt);
				int sang = EMath.figureAngle(points[0], points[1]);
				int eang = EMath.figureAngle(points[0], points[2]);
				if (eang > sang)
				{
					if (pang < eang && pang > sang) return dist;
				} else
				{
					if (pang < eang || pang > sang) return dist;
				}

				// point in arc: determine distance
				double odist = points[0].distance(points[1]);
				dist = points[0].distance(otherPt);
				return Math.abs(dist-odist);
			}
		}

		// can't figure out others: use distance to polygon center
		return otherPt.distance(polyCenter);
	}

	/**
	 * Routine to tell whether a point is inside of this Poly.
	 * This method is a requirement of the Shape implementation.
	 * @param x the X coordinate of the point.
	 * @param y the Y coordinate of the point.
	 * @return true if the point is inside the Poly.
	 */
	public boolean contains(double x, double y)
	{
		return isInside(new Point2D.Double(x, y));
	}

	/**
	 * Routine to tell whether a point is inside of this Poly.
	 * This method is a requirement of the Shape implementation.
	 * @param p the point.
	 * @return true if the point is inside the Poly.
	 */
	public boolean contains(Point2D p)
	{
		return isInside(new Point2D.Double(p.getX(), p.getY()));
	}

	/**
	 * Routine to tell whether a rectangle is inside of this Poly.
	 * This method is a requirement of the Shape implementation.
	 * THIS METHOD HAS NOT BEEN WRITTEN YET!!!
	 * @param x the X corner of the rectangle.
	 * @param y the Y corner of the rectangle.
	 * @param w the width of the rectangle.
	 * @param h the height of the rectangle.
	 * @return true if the rectangle is inside the Poly.
	 */
	public boolean contains(double x, double y, double w, double h)
	{
		return false;
	}

	/**
	 * Routine to tell whether a rectangle is inside of this Poly.
	 * This method is a requirement of the Shape implementation.
	 * THIS METHOD HAS NOT BEEN WRITTEN YET!!!
	 * @param r the rectangle.
	 * @return true if the rectangle is inside the Poly.
	 */
	public boolean contains(Rectangle2D r)
	{
		return contains(r.getX(), r.getY(), r.getWidth(), r.getHeight());
	}

	/**
	 * Routine to tell whether a rectangle intersects this Poly.
	 * This method is a requirement of the Shape implementation.
	 * THIS METHOD HAS NOT BEEN WRITTEN YET!!!
	 * @param x the X corner of the rectangle.
	 * @param y the Y corner of the rectangle.
	 * @param w the width of the rectangle.
	 * @param h the height of the rectangle.
	 * @return true if the rectangle intersects the Poly.
	 */
	public boolean intersects(double x, double y, double w, double h)
	{
		return false;
	}

	/**
	 * Routine to tell whether a rectangle intersects this Poly.
	 * This method is a requirement of the Shape implementation.
	 * THIS METHOD HAS NOT BEEN WRITTEN YET!!!
	 * @param r the rectangle.
	 * @return true if the rectangle intersects the Poly.
	 */
	public boolean intersects(Rectangle2D r)
	{
		return intersects(r.getX(), r.getY(), r.getWidth(), r.getHeight());
	}

	/**
	 * Routine to return the X center coordinate of this Poly.
	 * @return the X center coordinate of this Poly.
	 */
	public double getCenterX()
	{
		Rectangle2D b = getBounds2D();
		return EMath.smooth(b.getCenterX());
	}

	/**
	 * Routine to return the Y center coordinate of this Poly.
	 * @return the Y center coordinate of this Poly.
	 */
	public double getCenterY()
	{
		Rectangle2D b = getBounds2D();
		return EMath.smooth(b.getCenterY());
	}

	/**
	 * Routine to return the bounds of this Poly.
	 * @return the bounds of this Poly.
	 */
	public Rectangle2D getBounds2D()
	{
		if (bounds == null) calcBounds();
		return bounds;
	}

	/**
	 * Routine to return the bounds of this Poly.
	 * @return the bounds of this Poly.
	 */
	public Rectangle2D.Double getBounds2DDouble()
	{
		if (bounds == null) calcBounds();
		return bounds;
	}

	/**
	 * Routine to return the bounds of this Poly.
	 * @return the bounds of this Poly.
	 */
	public Rectangle getBounds()
	{
		if (bounds == null) calcBounds();
		Rectangle2D r = getBounds2D();
		return new Rectangle((int)r.getX(), (int)r.getY(), (int)r.getWidth(), (int)r.getHeight());
	}

	private void calcBounds()
	{
		double lx, ly, hx, hy;
		Rectangle2D sum;

		bounds = new Rectangle2D.Double();
		for (int i = 0; i < points.length; i++)
		{
			if (i == 0) bounds.setRect(points[0].getX(), points[0].getY(), 0, 0); else
				bounds.add(points[i]);
		}
		bounds.setRect(EMath.smooth(bounds.getMinX()), EMath.smooth(bounds.getMinY()),
			EMath.smooth(bounds.getWidth()), EMath.smooth(bounds.getHeight()));
	}

	class PolyPathIterator implements PathIterator
	{
		int idx = 0;
		AffineTransform trans;

		public PolyPathIterator(Poly p, AffineTransform at)
		{
			this.trans = at;
		}

		public int getWindingRule()
		{
			return WIND_EVEN_ODD;
		}

		public boolean isDone()
		{
			return idx > points.length;
		}

		public void next()
		{
			idx++;
		}

		public int currentSegment(float[] coords)
		{
			if (idx >= points.length)
			{
				return SEG_CLOSE;
			}
			coords[0] = (float) points[idx].getX();
			coords[1] = (float) points[idx].getY();
			if (trans != null)
			{
				trans.transform(coords, 0, coords, 0, 1);
			}
			return (idx == 0 ? SEG_MOVETO : SEG_LINETO);
		}

		public int currentSegment(double[] coords)
		{
			if (idx >= points.length)
			{
				return SEG_CLOSE;
			}
			coords[0] = points[idx].getX();
			coords[1] = points[idx].getY();
			if (trans != null)
			{
				trans.transform(coords, 0, coords, 0, 1);
			}
			return (idx == 0 ? SEG_MOVETO : SEG_LINETO);
		}
	}

	/**
	 * Routine to return a PathIterator for this Poly after a transformation.
	 * This method is a requirement of the Shape implementation.
	 * @param at the transformation to apply.
	 * @return the PathIterator.
	 */
	public PathIterator getPathIterator(AffineTransform at)
	{
		return new PolyPathIterator(this, at);
	}

	/**
	 * Routine to return a PathIterator with a particular flatness for this Poly after a transformation.
	 * This method is a requirement of the Shape implementation.
	 * @param at the transformation to apply.
	 * @param flatness the required flatness.
	 * @return the PathIterator.
	 */
	public PathIterator getPathIterator(AffineTransform at, double flatness)
	{
		return getPathIterator(at);
	}
}
