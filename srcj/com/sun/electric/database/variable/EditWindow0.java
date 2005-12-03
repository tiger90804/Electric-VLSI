/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: EditWindow0.java
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
package com.sun.electric.database.variable;

import com.sun.electric.database.hierarchy.Cell;
import java.awt.Font;
import java.awt.font.GlyphVector;

/**
 * This interface gives a limited access to EditWindow necessary
 * for calculating a shape of some primitives.
 */
public interface EditWindow0 {
    
	/**
	 * Method to return the cell that is shown in this window.
	 * @return the cell that is shown in this window.
	 */
	public Cell getCell();
    
    /**
     * Get the window's VarContext
     * @return the current VarContext
     */
    public VarContext getVarContext();
    
	/**
	 * Method to return the scale factor for this window.
	 * @return the scale factor for this window.
	 */
	public double getScale();

	/**
	 * Method to find the size in points (actual screen units) for text of a given database size in this EditWindow0.
	 * The scale of this EditWindow0 is used to determine the acutal screen size.
	 * @param dbSize the size of the text in database grid-units.
	 * @return the screen size (in points) of the text.
	 */
	public double getTextScreenSize(double dbSize);
    
	/**
	 * Method to find the size in database units for text of a given point size in this EditWindow0.
	 * The scale of this EditWindow0 is used to determine the acutal unit size.
	 * @param pointSize the size of the text in points.
	 * @return the database size (in grid units) of the text.
	 */
	public double getTextUnitSize(double pointSize);

	/**
	 * Method to get the height of text given a TextDescriptor in this EditWindow0.
	 * @param descript the TextDescriptor.
	 * @return the height of the text.
	 */
	public double getFontHeight(TextDescriptor descript);

	/**
	 * Method to get a Font to use for a given TextDescriptor in this EditWindow0.
	 * @param descript the TextDescriptor.
	 * @return the Font to use (returns null if the text is too small to display).
	 */
	public Font getFont(TextDescriptor descript);
    
	/**
	 * Method to convert a string and descriptor to a GlyphVector.
	 * @param text the string to convert.
	 * @param font the Font to use.
	 * @return a GlyphVector describing the text.
	 */
	public GlyphVector getGlyphs(String text, Font font);
}
