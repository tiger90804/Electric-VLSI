/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: AbstractDrawing.java
 *
 * Copyright (c) 2008 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
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
package com.sun.electric.tool.user.redisplay;

import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.technology.Layer;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

/**
 * Base class for redisplay algorithms
 */
public abstract class AbstractDrawing {

    public final EditWindow wnd;
    public WindowFrame.DisplayAttributes da;

    protected AbstractDrawing(EditWindow wnd) {
        this.wnd = wnd;
    }

    public static AbstractDrawing createDrawing(EditWindow wnd, AbstractDrawing drawing, Cell cell) {
        boolean isLayerDrawing = User.getDisplayAlgorithm() == 2 && cell != null && cell.getTechnology().isLayout();
        if (isLayerDrawing && !(drawing instanceof LayerDrawing.Drawing))
            drawing = new LayerDrawing.Drawing(wnd);
        else if (!isLayerDrawing && !(drawing instanceof PixelDrawing.Drawing))
            drawing = new PixelDrawing.Drawing(wnd);
        return drawing;
    }

    public abstract boolean paintComponent(Graphics2D g, Dimension sz);

    public abstract void render(Dimension sz, WindowFrame.DisplayAttributes da, boolean fullInstantiate, Rectangle2D bounds);

    public void abortRendering() {
    }

    public void opacityChanged() {
    }
    
    public boolean hasOpacity() { return false; }

    public void testJogl() {
    }
    
	/**
	 * Method to clear the cache of expanded subcells.
	 * This is used by layer visibility which, when changed, causes everything to be redrawn.
	 */
	public static void clearSubCellCache()
	{
        PixelDrawing.clearSubCellCache();
        LayerDrawing.clearSubCellCache();
	}

	public static void forceRedraw(Cell cell)
	{
        PixelDrawing.forceRedraw(cell);
        LayerDrawing.forceRedraw(cell);
	}

	/**
	 * Method to draw polygon "poly", transformed through "trans".
	 */
    public static void drawShapes(Graphics2D g, int imgX, int imgY, double scale, VectorCache.VectorBase[] shapes,
            PixelDrawing offscreen, Rectangle entryRect) {
        if (User.getDisplayAlgorithm() < 2 || User.isLegacyComposite()) {
            offscreen.initDrawing(scale);
            VectorDrawing vd = new VectorDrawing();
            vd.render(offscreen, scale, EPoint.ORIGIN, shapes, true);
            Image img = offscreen.composite(null);
            g.drawImage(img, imgX, imgY, null);
        } else {
            LayerDrawing.drawTechPalette(g, imgX, imgY, entryRect, scale, shapes);
        }
    }
    
    public static class LayerColor {

        public final Layer layer;
        // nextRgb = inverseAlpha*prevRgb + premultipliedRgb
        public final float premultipliedRed;
        public final float premultipliedGreen;
        public final float premultipliedBlue;
        public final float inverseAlpha;

        public LayerColor(Layer layer, float premultipliedRed, float premultipliedGreen, float premultipliedBlue, float inverseAlpha) {
            this.layer = layer;
            this.premultipliedRed = premultipliedRed;
            this.premultipliedGreen = premultipliedGreen;
            this.premultipliedBlue = premultipliedBlue;
            this.inverseAlpha = inverseAlpha;
        }
    }
}
