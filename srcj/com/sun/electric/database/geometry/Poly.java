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

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/** Represents a transformable Polygon with floating-point
 * coordinates, and a specific Layer. */
public class Poly implements Shape
{
	/**
	 * Type is a typesafe enum class that describes the function of an Poly.
	 */
	public static class Type
	{
		private Type()
		{
		}

		public String toString() { return "Polygon type"; }

		// polygons ************
		/** closed polygon, filled in */					public static final Type FILLED =         new Type();
		/** closed polygon, outline  */						public static final Type CLOSED =         new Type();
		// rectangles ************
//		/** closed rectangle, filled in */					public static final Type FILLEDRECT =     new Type();
//		/** closed rectangle, outline */					public static final Type CLOSEDRECT =     new Type();
		/** closed rectangle, outline crossed */			public static final Type CROSSED =        new Type();
		// lines ************
		/** open outline, solid */							public static final Type OPENED =         new Type();
		/** open outline, dotted */							public static final Type OPENEDT1 =       new Type();
		/** open outline, dashed  */						public static final Type OPENEDT2 =       new Type();
		/** open outline, thicker */						public static final Type OPENEDT3 =       new Type();
		/** open outline pushed by 1 */						public static final Type OPENEDO1 =       new Type();
		/** vector endpoint pairs, solid */					public static final Type VECTORS =        new Type();
		// curves ************
		/** circle at [0] radius to [1] */					public static final Type CIRCLE =         new Type();
		/** thick circle at [0] radius to [1] */			public static final Type THICKCIRCLE =    new Type();
		/** filled circle */								public static final Type DISC =           new Type();
		/** arc of circle at [0] ends [1] and [2] */		public static final Type CIRCLEARC =      new Type();
		/** thick arc of circle at [0] ends [1] and [2] */	public static final Type THICKCIRCLEARC = new Type();
		// text ************
		/** text at center */								public static final Type TEXTCENT =       new Type();
		/** text below top edge */							public static final Type TEXTTOP =        new Type();
		/** text above bottom edge */						public static final Type TEXTBOT =        new Type();
		/** text to right of left edge */					public static final Type TEXTLEFT =       new Type();
		/** text to left of right edge */					public static final Type TEXTRIGHT =      new Type();
		/** text to lower-right of top-left corner */		public static final Type TEXTTOPLEFT =    new Type();
		/** text to upper-right of bottom-left corner */	public static final Type TEXTBOTLEFT =    new Type();
		/** text to lower-left of top-right corner */		public static final Type TEXTTOPRIGHT =   new Type();
		/** text to upper-left of bottom-right corner */	public static final Type TEXTBOTRIGHT =   new Type();
		/** text that fits in box (may shrink) */			public static final Type TEXTBOX =        new Type();
		// miscellaneous ************
		/** grid dots in the window */						public static final Type GRIDDOTS =       new Type();
		/** cross */										public static final Type CROSS =          new Type();
		/** big cross */									public static final Type BIGCROSS =       new Type();
	}

	private Layer layer;
	private Point2D points[];
	private Rectangle2D.Double bounds;
	private Poly.Type style;
	private String string;

	/* text font sizes (in VARIABLE, NODEINST, PORTPROTO, and POLYGON->textdescription) */
	/** points from 1 to TXTMAXPOINTS */					public static final int TXTPOINTS =        077;
	/** right-shift of TXTPOINTS */							public static final int TXTPOINTSSH =        0;
	public static final int TXTMAXPOINTS =      63;
//	public static final int TXTSETPOINTS(p)   ((p)<<TXTPOINTSSH)
//	public static final int TXTGETPOINTS(p)   (((p)&TXTPOINTS)>>TXTPOINTSSH)

	public static final int TXTQGRID =    077700;		
	public static final int TXTQGRIDSH =       6;		
	public static final int TXTMAXQGRID =    511;
//	public static final int TXTSETQGRID(ql) ((ql)<<TXTQGRIDSH)
//	public static final int TXTGETQGRIDql) (((ql)&TXTQGRID)>>TXTQGRIDSH)
	/** fixed-width text for text editing */			public static final int TXTEDITOR =     077770;
	/** text for menu selection */						public static final int TXTMENU =       077771;

	/** Create a new Poly given (x,y) points and a specific Layer */
	public Poly(Point2D [] points)
	{
		this.points = points;
		layer = null;
		style = null;
		bounds = null;
	}

	/** Create a new rectangular Poly given a specific Layer */
	public Poly(double cX, double cY, double width, double height)
	{
		double halfWidth = width / 2;
		double halfHeight = height / 2;
		this.points = new Point2D[] {
			new Point2D.Double(cX-halfWidth, cY-halfHeight),
			new Point2D.Double(cX+halfWidth, cY-halfHeight),
			new Point2D.Double(cX+halfWidth, cY+halfHeight),
			new Point2D.Double(cX-halfWidth, cY+halfHeight)};
		layer = null;
		style = null;
		bounds = null;
	}

	public Layer getLayer() { return layer; }
	public void setLayer(Layer layer) { this.layer = layer; }

	public Poly.Type getStyle() { return style; }
	public void setStyle(Poly.Type style) { this.style = style; }

	public String getString() { return string; }
	public void setString(String string) { this.string = string; }

	public Point2D [] getPoints() { return points; }

	/** Get a transformed copy of this polygon, including scale, offset,
	 * and rotation.
	 * @param af transformation to apply */
	public void transform(AffineTransform af)
	{
		af.transform(points, 0, points, 0, points.length);
//		af.transform(points, points);
		bounds = null;
	}

	// SHAPE REQUIREMENTS:
	/** TODO: write contains(double, double); */
	public boolean contains(double x, double y)
	{
		return false;
	}

	/** TODO: write contains(Point2D); */
	public boolean contains(Point2D p)
	{
		return contains(p.getX(), p.getY());
	}

	/** TODO: write contains(double, double, double, double); */
	public boolean contains(double x, double y, double w, double h)
	{
		return false;
	}

	/** TODO: write contains(Rectangle2D); */
	public boolean contains(Rectangle2D r)
	{
		return contains(r.getX(), r.getY(), r.getWidth(), r.getHeight());
	}

	/** TODO: write intersects(double, double, double, double); */
	public boolean intersects(double x, double y, double w, double h)
	{
		return false;
	}

	/** TODO: write intersects(Rectangle2D); */
	public boolean intersects(Rectangle2D r)
	{
		return intersects(r.getX(), r.getY(), r.getWidth(), r.getHeight());
	}

	/** Get the x coordinate of the center of this poly */
	public double getCenterX()
	{
		Rectangle2D b = getBounds2D();
		return b.getCenterX();
	}

	/** Get the y coordinate of the center of this poly */
	public double getCenterY()
	{
		Rectangle2D b = getBounds2D();
		return b.getCenterY();
	}

	/** Get the bounds of this poly, as a Rectangle2D */
	public Rectangle2D getBounds2D()
	{
		if (bounds == null) calcBounds();
		return bounds;
	}

	/** Get the bounds of this poly, as a Rectangle2D */
	public Rectangle2D.Double getBounds2DDouble()
	{
		if (bounds == null) calcBounds();
		return bounds;
	}

	/** Get the bounds of this poly, as a Rectangle */
	public Rectangle getBounds()
	{
		if (bounds == null) calcBounds();
		Rectangle2D r = getBounds2D();
		return new Rectangle((int)r.getX(), (int)r.getY(), (int)r.getWidth(), (int)r.getHeight());
	}

	protected void calcBounds()
	{
		double lx, ly, hx, hy;
		Rectangle2D sum;

		bounds = new Rectangle2D.Double();
		for (int i = 0; i < points.length; i++)
		{
			if (i == 0) bounds.setRect(points[0].getX(), points[0].getY(), 0, 0); else
				bounds.add(points[i]);
		}
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

	/** Get a PathIterator for this poly after a transform */
	public PathIterator getPathIterator(AffineTransform at)
	{
		return new PolyPathIterator(this, at);
	}

	/** Get a PathIterator for this poly after a transform, with a particular
	 * flatness */
	public PathIterator getPathIterator(AffineTransform at, double flatness)
	{
		return getPathIterator(at);
	}
}
