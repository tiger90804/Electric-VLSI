/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DebugMenus.java
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

package com.sun.electric.tool.user.menus;

import com.sun.electric.tool.logicaleffort.LENetlister;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.ZoomAndPanListener;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.geometry.PolyQTree;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.PolyMerge;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.Main;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: gainsley
 * Date: Jun 23, 2004
 * Time: 11:46:44 AM
 * To change this template use File | Settings | File Templates.
 */
public class DebugMenus {

    protected static void addDebugMenus(MenuBar menuBar) {
        MenuBar.MenuItem m;
		int buckyBit = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

        /****************************** Russell's TEST MENU ******************************/

        MenuBar.Menu russMenu = new MenuBar.Menu("Russell", 'R');
        menuBar.add(russMenu);
        russMenu.addMenuItem("Gate Generator Regression", null,
                             new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new com.sun.electric.tool.generator.layout.GateRegression();
            }
        });
        russMenu.addMenuItem("Jemini", null, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new com.sun.electric.tool.ncc.NccJob();
            }
        });
        russMenu.addMenuItem("create flat netlists for Ivan", null, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new com.sun.electric.tool.generator.layout.IvanFlat();
            }
        });
        russMenu.addMenuItem("layout flat", null, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new com.sun.electric.tool.generator.layout.LayFlat();
            }
        });
        russMenu.addMenuItem("Random Test", null, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new com.sun.electric.tool.generator.layout.Test();
            }
        });

        /****************************** Jon's TEST MENU ******************************/

        MenuBar.Menu jongMenu = new MenuBar.Menu("JonG", 'J');
        menuBar.add(jongMenu);
        jongMenu.addMenuItem("Describe Vars", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { listVarsOnObject(false); }});
        jongMenu.addMenuItem("Describe Proto Vars", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { listVarsOnObject(true); }});
        jongMenu.addMenuItem("Describe Current Library Vars", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { listLibVars(); }});
        jongMenu.addMenuItem("Eval Vars", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { evalVarsOnObject(); }});
        jongMenu.addMenuItem("LE test1", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { LENetlister.test1(); }});
        jongMenu.addMenuItem("Display shaker", null,
            new ActionListener() { public void actionPerformed(ActionEvent e) { shakeDisplay(); }});

        /****************************** Gilda's TEST MENU ******************************/
        // Only active in debug mode. Doesn't work
        //if (Main.getDebug())
        {
            MenuBar.Menu gildaMenu = new MenuBar.Menu("Gilda", 'G');
            menuBar.add(gildaMenu);
            gildaMenu.addMenuItem("Merge Polyons", null,
                    new ActionListener() { public void actionPerformed(ActionEvent e) {ToolMenu.layerCoverageCommand(Job.Type.CHANGE, ToolMenu.LayerCoverageJob.MERGE, true);}});
            gildaMenu.addMenuItem("Covering Implants", null,
                    new ActionListener() { public void actionPerformed(ActionEvent e) {ToolMenu.layerCoverageCommand(Job.Type.CHANGE, ToolMenu.LayerCoverageJob.IMPLANT, true);}});
            gildaMenu.addMenuItem("Covering Implants Old", null,
                    new ActionListener() { public void actionPerformed(ActionEvent e) {implantGeneratorCommand(false, false);}});
            gildaMenu.addMenuItem("List Layer Coverage", null,
                new ActionListener() { public void actionPerformed(ActionEvent e) { ToolMenu.layerCoverageCommand(Job.Type.EXAMINE, ToolMenu.LayerCoverageJob.AREA, true); } });
            gildaMenu.addMenuItem("3D View", null,
                    new ActionListener() { public void actionPerformed(ActionEvent e) { create3DViewCommand(Job.Type.EXAMINE); } });
        }

    }

    // ---------------------- Gilda's Stuff MENU -----------------

    public static void create3DViewCommand(Job.Type jobType)
    {
	    Cell curCell = WindowFrame.needCurCell();

	    if (curCell == null) return;
	    WindowFrame.create3DViewtWindow(curCell);
    }

	/**
	 * First attempt for coverage implant
	 * @param newIdea
	 * @param test
	 */
	public static void implantGeneratorCommand(boolean newIdea, boolean test) {
		Cell curCell = WindowFrame.needCurCell();
		if (curCell == null) return;
        Job job = null;

	    if (newIdea)
	    {
			job = new CoverImplant(curCell, test);
	    }
	    else
	    {
		    job = new CoverImplantOld(curCell);
	    }
	}

	private static class CoverImplant extends Job
	{
		private Cell curCell;
        private boolean testMerge = false;

		protected CoverImplant(Cell cell, boolean test)
		{
			super("Coverage Implant", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.curCell = cell;
            this.testMerge = test;
			setReportExecutionFlag(true);
			startJob();
		}

		public boolean doIt()
		{
			java.util.List deleteList = new ArrayList(); // New coverage implants are pure primitive nodes
            PolyQTree tree = new PolyQTree(curCell.getBounds());

			// Traversing arcs
			for (Iterator it = curCell.getArcs(); it.hasNext(); )
			{
				ArcInst arc = (ArcInst)it.next();
				ArcProto arcType = arc.getProto();
				Technology tech = arcType.getTechnology();
				Poly[] polyList = tech.getShapeOfArc(arc);

				// Treating the arcs associated to each node
				// Arcs don't need to be rotated
				for (int i = 0; i < polyList.length; i++)
				{
					Poly poly = polyList[i];
					Layer layer = poly.getLayer();
					Layer.Function func = layer.getFunction();

					if (Main.getDebug() || func.isSubstrate())
					{
						//Area bounds = new PolyQTree.PolyNode(poly.getBounds2D());
						tree.insert((Object)layer, new PolyQTree.PolyNode(poly.getBounds2D()));
					}
				}
			}
			// Traversing nodes
			for (Iterator it = curCell.getNodes(); it.hasNext(); )
			{
				NodeInst node = (NodeInst)it .next();

				// New coverage implants are pure primitive nodes
				// and previous get deleted and ignored.
				if (!Main.getDebug() && node.getFunction() == NodeProto.Function.NODE)
				{
					deleteList.add(node);
					continue;
				}

				NodeProto protoType = node.getProto();
				if (protoType instanceof Cell) continue;

				Technology tech = protoType.getTechnology();
				Poly[] polyList = tech.getShapeOfNode(node);
				AffineTransform transform = node.rotateOut();

				for (int i = 0; i < polyList.length; i++)
				{
					Poly poly = polyList[i];
					Layer layer = poly.getLayer();
					Layer.Function func = layer.getFunction();

                    // Only substrate layers, skipping center information
					if (Main.getDebug() || func.isSubstrate())
					{
						poly.transform(transform);
						//Area bounds = new PolyQTree.PolyNode(poly.getBounds2D());
						tree.insert((Object)layer, new PolyQTree.PolyNode(poly.getBounds2D()));
					}
				}
			}

			// tree.print();

			// With polygons collected, new geometries are calculated
			Highlight.clear();
			boolean noNewNodes = true;

			// Need to detect if geometry was really modified
			for(Iterator it = tree.getKeyIterator(); it.hasNext(); )
			{
				Layer layer = (Layer)it.next();
				Set set = tree.getObjects(layer, true);

				// Ready to create new implants.
				for (Iterator i = set.iterator(); i.hasNext(); )
				{
					PolyQTree.PolyNode qNode = (PolyQTree.PolyNode)i.next();
					Rectangle2D rect = qNode.getBounds2D();
					Point2D center = new Point2D.Double(rect.getCenterX(), rect.getCenterY());
					PrimitiveNode priNode = layer.getPureLayerNode();
					// Adding the new implant. New implant not assigned to any local variable                                .
					NodeInst node = NodeInst.makeInstance(priNode, center, rect.getWidth(), rect.getHeight(), 0, curCell, null);
					Highlight.addElectricObject(node, curCell);

					if ( testMerge )
					{
					        Point2D [] points = qNode.getPoints();
					        node.newVar(NodeInst.TRACE, points);
					}
					else
					{
					        // New implant can't be selected again
					        node.setHardSelect();
					}
					noNewNodes = false;
				}
			}
			Highlight.finished();
			for (Iterator it = deleteList.iterator(); it.hasNext(); )
			{
				NodeInst node = (NodeInst)it .next();
				node.kill();
			}
			if (noNewNodes)
				System.out.println("No implant areas added");
			return true;
		}
	}

	private static class CoverImplantOld extends Job
	{
		private Cell curCell;

		protected CoverImplantOld(Cell cell)
		{
			super("Coverage Implant Old", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.curCell = cell;
			setReportExecutionFlag(true);
			startJob();
		}

		public boolean doIt()
		{
			PolyMerge merge = new PolyMerge();
			java.util.List deleteList = new ArrayList(); // New coverage implants are pure primitive nodes
			HashMap allLayers = new HashMap();

			// Traversing arcs
			for(Iterator it = curCell.getArcs(); it.hasNext(); )
			{
				ArcInst arc = (ArcInst)it.next();
				ArcProto arcType = arc.getProto();
				Technology tech = arcType.getTechnology();
				Poly[] polyList = tech.getShapeOfArc(arc);

				// Treating the arcs associated to each node
				// Arcs don't need to be rotated
				for (int i = 0; i < polyList.length; i++)
				{
					Poly poly = polyList[i];
					Layer layer = poly.getLayer();
					Layer.Function func = layer.getFunction();

					if ( func.isSubstrate() )
					{
						merge.addPolygon(layer, poly);
						java.util.List rectList = (java.util.List)allLayers.get(layer);

						if ( rectList == null )
						{
							rectList = new ArrayList();
							allLayers.put(layer, rectList);
						}
						rectList.add(poly);
					}
				}
			}
			// Traversing nodes
			for(Iterator it = curCell.getNodes(); it.hasNext(); )
			{
				NodeInst node = (NodeInst)it .next();

				// New coverage implants are pure primitive nodes
				// and previous get deleted and ignored.
				if ( node.getFunction() == NodeProto.Function.NODE )
				{
					deleteList.add(node);
					continue;
				}

				NodeProto protoType = node.getProto();
				if (protoType instanceof Cell) continue;

				Technology tech = protoType.getTechnology();
				Poly[] polyList = tech.getShapeOfNode(node);
				AffineTransform transform = node.rotateOut();

				for (int i = 0; i < polyList.length; i++)
				{
					Poly poly = polyList[i];
					Layer layer = poly.getLayer();
					Layer.Function func = layer.getFunction();

                    // Only substrate layers, skipping center information
					if ( func.isSubstrate() )
					{
						poly.transform(transform);
						merge.addPolygon(layer, poly);
						java.util.List rectList = (java.util.List)allLayers.get(layer);

						if ( rectList == null )
						{
							rectList = new ArrayList();
							allLayers.put(layer, rectList);
						}
						rectList.add(poly);
					}
				}
			}

			// With polygons collected, new geometries are calculated
			Highlight.clear();
			java.util.List nodesList = new ArrayList();

			// Need to detect if geometry was really modified
			for(Iterator it = merge.getLayersUsed(); it.hasNext(); )
			{
				Layer layer = (Layer)it.next();
				java.util.List list = merge.getMergedPoints(layer) ;

				// Temp solution until qtree implementation is ready
				// delete uncessary polygons. Doesn't insert poly if identical
				// to original. Very ineficient!!
				java.util.List rectList = (java.util.List)allLayers.get(layer);
				java.util.List delList = new ArrayList();

				for (Iterator iter = rectList.iterator(); iter.hasNext();)
				{
					Poly p = (Poly)iter.next();
					Rectangle2D rect = p.getBounds2D();

					for (Iterator i = list.iterator(); i.hasNext();)
					{
						Poly poly = (Poly)i.next();
						Rectangle2D r = poly.getBounds2D();

						if (r.equals(rect))
						{
							delList.add(poly);
						}
					}
				}
				for (Iterator iter = delList.iterator(); iter.hasNext();)
				{
					list.remove(iter.next());
				}

				// Ready to create new implants.
				for(Iterator i = list.iterator(); i.hasNext(); )
				{
					Poly poly = (Poly)i.next();
					Rectangle2D rect = poly.getBounds2D();
					Point2D center = new Point2D.Double(rect.getCenterX(), rect.getCenterY());
					PrimitiveNode priNode = layer.getPureLayerNode();
					// Adding the new implant. New implant not assigned to any local variable                                .
					NodeInst node = NodeInst.makeInstance(priNode, center, rect.getWidth(), rect.getHeight(), 0, curCell, null);
					Highlight.addElectricObject(node, curCell);
					// New implant can't be selected again
					node.setHardSelect();
					nodesList.add(node);
				}
			}
			Highlight.finished();
			for (Iterator it = deleteList.iterator(); it.hasNext(); )
			{
				NodeInst node = (NodeInst)it .next();
				node.kill();
			}
			if ( nodesList.isEmpty() )
				System.out.println("No implant areas added");
			return true;
		}
	}
	// ---------------------- THE JON GAINSLEY MENU -----------------

	public static void listVarsOnObject(boolean useproto) {
		if (Highlight.getNumHighlights() == 0) {
			// list vars on cell
			WindowFrame wf = WindowFrame.getCurrentWindowFrame();
			if (wf == null) return;
            Cell cell = wf.getContent().getCell();
            cell.getInfo();
			return;
		}
		for (Iterator it = Highlight.getHighlights(); it.hasNext();) {
			Highlight h = (Highlight)it.next();
			if (h.getType() != Highlight.Type.EOBJ) continue;
			ElectricObject eobj = h.getElectricObject();
            if (eobj instanceof PortInst) {
                PortInst pi = (PortInst)eobj;
                pi.getInfo();
                eobj = pi.getNodeInst();
            }
			if (eobj instanceof NodeInst) {
				NodeInst ni = (NodeInst)eobj;
				if (useproto) {
					System.out.println("using prototype");
					((ElectricObject)ni.getProto()).getInfo();
				} else {
					ni.getInfo();
				}
			}
		}
	}

	public static void evalVarsOnObject() {
		EditWindow curEdit = EditWindow.needCurrent();
		if (Highlight.getNumHighlights() == 0) return;
		for (Iterator it = Highlight.getHighlights(); it.hasNext();) {
			Highlight h = (Highlight)it.next();
			if (h.getType() != Highlight.Type.EOBJ) continue;
			ElectricObject eobj = h.getElectricObject();
			Iterator itVar = eobj.getVariables();
			while(itVar.hasNext()) {
				Variable var = (Variable)itVar.next();
				Object obj = curEdit.getVarContext().evalVar(var);
				System.out.print(var.getKey().getName() + ": ");
				System.out.println(obj);
			}
		}
	}

	public static void listLibVars() {
		Library lib = Library.getCurrent();
		Iterator itVar = lib.getVariables();
		System.out.println("----------"+lib+" Vars-----------");
		while(itVar.hasNext()) {
			Variable var = (Variable)itVar.next();
			Object obj = VarContext.globalContext.evalVar(var);
			System.out.println(var.getKey().getName() + ": " +obj);
		}
	}

    public static void shakeDisplay() {
        //RedisplayTest job = new RedisplayTest(50);
        //RedrawTest test = new RedrawTest();
        long startTime = System.currentTimeMillis();

        EditWindow wnd = EditWindow.getCurrent();
        for (int i=0; i<100; i++) {
            //wnd.redrawTestOnly();
            //doWait();
        }
        long endTime = System.currentTimeMillis();

        StringBuffer buf = new StringBuffer();
        Date start = new Date(startTime);
        buf.append("  start time: "+start+"\n");
        Date end = new Date(endTime);
        buf.append("  end time: "+end+"\n");
        long time = endTime - startTime;
        buf.append("  time taken: "+TextUtils.getElapsedTime(time)+"\n");
        System.out.println(buf.toString());

    }

    private static class RedrawTest extends Job {

        private RedrawTest() {
            super("RedrawTest", User.tool, Job.Type.EXAMINE, null, null, Job.Priority.USER);
            startJob();
        }

        public boolean doIt() {
            long startTime = System.currentTimeMillis();

            EditWindow wnd = EditWindow.getCurrent();
            for (int i=0; i<100; i++) {
                if (getScheduledToAbort()) return false;
                //wnd.redrawTestOnly();
                //doWait();
            }
            long endTime = System.currentTimeMillis();

            StringBuffer buf = new StringBuffer();
            Date start = new Date(startTime);
            buf.append("  start time: "+start+"\n");
            Date end = new Date(endTime);
            buf.append("  end time: "+end+"\n");
            long time = endTime - startTime;
            buf.append("  time taken: "+TextUtils.getElapsedTime(time)+"\n");
            System.out.println(buf.toString());

            return true;
        }

        private void doWait() {
            try {
                boolean donesleeping = false;
                while (!donesleeping) {
                    Thread.sleep(100);
                    donesleeping = true;
                }
            } catch (InterruptedException e) {}
        }
    }

    private static class RedisplayTest extends Job {

        private long delayTimeMS;

        private RedisplayTest(long delayTimeMS) {
            super("RedisplayTest", User.tool, Job.Type.EXAMINE, null, null, Job.Priority.USER);
            this.delayTimeMS = delayTimeMS;
            startJob();
        }

        public boolean doIt() {
            Random rand = new Random(143137493);

            for (int i=0; i<200; i++) {
                if (getScheduledToAbort()) return false;

                WindowFrame wf = WindowFrame.getCurrentWindowFrame();
                //int next = rand.nextInt(4);
                int next = i % 4;
                switch(next) {
                    case 0: { ZoomAndPanListener.panX(wf, 1); break; }
                    case 1: { ZoomAndPanListener.panY(wf, 1); break; }
                    case 2: { ZoomAndPanListener.panX(wf, -1); break; }
                    case 3: { ZoomAndPanListener.panY(wf, -1); break; }
                }
                doWait();
            }
            System.out.println(getInfo());
            return true;
        }

        private void doWait() {
            try {
                boolean donesleeping = false;
                while (!donesleeping) {
                    Thread.sleep(delayTimeMS);
                    donesleeping = true;
                }
            } catch (InterruptedException e) {}
        }
    }


    public static void whitDiffieCommand()
    {
        MakeWhitDesign job = new MakeWhitDesign();
    }

    private static class MakeWhitDesign extends Job
    {
        protected MakeWhitDesign()
        {
            super("Make Whit design", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
            startJob();
        }

        public boolean doIt()
        {
            String [] theStrings =
            {
                // correct data
//				"a1f0f1k0p0", "a2f1f2k1p1", "a3f2f3k2p2", "a0a4f0f3f4k3p3", "a0a5f0f4f5k4p4", "a6f5f6k5p5", "a0a7f0f6f7k6p6", "a0f0f7k7p7",
//				"a0f1k0k1p0", "a1f2k1k2p1", "a2f3k2k3p2", "a3f0f4k0k3k4p3", "a4f0f5k0k4k5p4", "a5f6k5k6p5", "a6f0f7k0k6k7p6", "a7f0k0k7p7",
//				"a0f0k1p0p1", "a1f1k2p1p2", "a2f2k3p2p3", "a3f3k0k4p0p3p4", "a4f4k0k5p0p4p5", "a5f5k6p5p6", "a6f6k0k7p0p6p7", "a7f7k0p0p7",
//				"a0a1f0k0p1", "a1a2f1k1p2", "a2a3f2k2p3", "a0a3a4f3k3p0p4", "a0a4a5f4k4p0p5", "a5a6f5k5p6", "a0a6a7f6k6p0p7", "a0a7f7k7p0",
//				"e1j0j1o0d0", "e2j1j2o1d1", "e3j2j3o2d2", "e0e4j0j3j4o3d3", "e0e5j0j4j5o4d4", "e6j5j6o5d5", "e0e7j0j6j7o6d6", "e0j0j7o7d7",
//				"e0j1o0o1d0", "e1j2o1o2d1", "e2j3o2o3d2", "e3j0j4o0o3o4d3", "e4j0j5o0o4o5d4", "e5j6o5o6d5", "e6j0j7o0o6o7d6", "e7j0o0o7d7",
//				"e0j0o1d0d1", "e1j1o2d1d2", "e2j2o3d2d3", "e3j3o0o4d0d3d4", "e4j4o0o5d0d4d5", "e5j5o6d5d6", "e6j6o0o7d0d6d7", "e7j7o0d0d7",
//				"e0e1j0o0d1", "e1e2j1o1d2", "e2e3j2o2d3", "e0e3e4j3o3d0d4", "e0e4e5j4o4d0d5", "e5e6j5o5d6", "e0e6e7j6o6d0d7", "e0e7j7o7d0",
//				"i1n0n1c0h0", "i2n1n2c1h1", "i3n2n3c2h2", "i0i4n0n3n4c3h3", "i0i5n0n4n5c4h4", "i6n5n6c5h5", "i0i7n0n6n7c6h6", "i0n0n7c7h7",
//				"i0n1c0c1h0", "i1n2c1c2h1", "i2n3c2c3h2", "i3n0n4c0c3c4h3", "i4n0n5c0c4c5h4", "i5n6c5c6h5", "i6n0n7c0c6c7h6", "i7n0c0c7h7",
//				"i0n0c1h0h1", "i1n1c2h1h2", "i2n2c3h2h3", "i3n3c0c4h0h3h4", "i4n4c0c5h0h4h5", "i5n5c6h5h6", "i6n6c0c7h0h6h7", "i7n7c0h0h7",
//				"i0i1n0c0h1", "i1i2n1c1h2", "i2i3n2c2h3", "i0i3i4n3c3h0h4", "i0i4i5n4c4h0h5", "i5i6n5c5h6", "i0i6i7n6c6h0h7", "i0i7n7c7h0",
//				"m1b0b1g0l0", "m2b1b2g1l1", "m3b2b3g2l2", "m0m4b0b3b4g3l3", "m0m5b0b4b5g4l4", "m6b5b6g5l5", "m0m7b0b6b7g6l6", "m0b0b7g7l7",
//				"m0b1g0g1l0", "m1b2g1g2l1", "m2b3g2g3l2", "m3b0b4g0g3g4l3", "m4b0b5g0g4g5l4", "m5b6g5g6l5", "m6b0b7g0g6g7l6", "m7b0g0g7l7",
//				"m0b0g1l0l1", "m1b1g2l1l2", "m2b2g3l2l3", "m3b3g0g4l0l3l4", "m4b4g0g5l0l4l5", "m5b5g6l5l6", "m6b6g0g7l0l6l7", "m7b7g0l0l7",
//				"m0m1b0g0l1", "m1m2b1g1l2", "m2m3b2g2l3", "m0m3m4b3g3l0l4", "m0m4m5b4g4l0l5", "m5m6b5g5l6", "m0m6m7b6g6l0l7", "m0m7b7g7l0"

                // sorted data
//				"a1f0f1k0p0", "a2f1f2k1p1", "a3f2f3k2p2", "a0a4f0f3f4k3p3", "a0a5f0f4f5k4p4", "a6f5f6k5p5", "a0a7f0f6f7k6p6", "a0f0f7k7p7",
//				"a0f1k0k1p0", "a1f2k1k2p1", "a2f3k2k3p2", "a3f0f4k0k3k4p3", "a4f0f5k0k4k5p4", "a5f6k5k6p5", "a6f0f7k0k6k7p6", "a7f0k0k7p7",
//				"a0f0k1p0p1", "a1f1k2p1p2", "a2f2k3p2p3", "a3f3k0k4p0p3p4", "a4f4k0k5p0p4p5", "a5f5k6p5p6", "a6f6k0k7p0p6p7", "a7f7k0p0p7",
//				"a0a1f0k0p1", "a1a2f1k1p2", "a2a3f2k2p3", "a0a3a4f3k3p0p4", "a0a4a5f4k4p0p5", "a5a6f5k5p6", "a0a6a7f6k6p0p7", "a0a7f7k7p0",
//				"d0e1j0j1o0", "d1e2j1j2o1", "d2e3j2j3o2", "d3e0e4j0j3j4o3", "d4e0e5j0j4j5o4", "d5e6j5j6o5", "d6e0e7j0j6j7o6", "d7e0j0j7o7",
//				"d0e0j1o0o1", "d1e1j2o1o2", "d2e2j3o2o3", "d3e3j0j4o0o3o4", "d4e4j0j5o0o4o5", "d5e5j6o5o6", "d6e6j0j7o0o6o7", "d7e7j0o0o7",
//				"d0d1e0j0o1", "d1d2e1j1o2", "d2d3e2j2o3", "d0d3d4e3j3o0o4", "d0d4d5e4j4o0o5", "d5d6e5j5o6", "d0d6d7e6j6o0o7", "d0d7e7j7o0",
//				"d1e0e1j0o0", "d2e1e2j1o1", "d3e2e3j2o2", "d0d4e0e3e4j3o3", "d0d5e0e4e5j4o4", "d6e5e6j5o5", "d0d7e0e6e7j6o6", "d0e0e7j7o7",
//				"c0h0i1n0n1", "c1h1i2n1n2", "c2h2i3n2n3", "c3h3i0i4n0n3n4", "c4h4i0i5n0n4n5", "c5h5i6n5n6", "c6h6i0i7n0n6n7", "c7h7i0n0n7",
//				"c0c1h0i0n1", "c1c2h1i1n2", "c2c3h2i2n3", "c0c3c4h3i3n0n4", "c0c4c5h4i4n0n5", "c5c6h5i5n6", "c0c6c7h6i6n0n7", "c0c7h7i7n0",
//				"c1h0h1i0n0", "c2h1h2i1n1", "c3h2h3i2n2", "c0c4h0h3h4i3n3", "c0c5h0h4h5i4n4", "c6h5h6i5n5", "c0c7h0h6h7i6n6", "c0h0h7i7n7",
//				"c0h1i0i1n0", "c1h2i1i2n1", "c2h3i2i3n2", "c3h0h4i0i3i4n3", "c4h0h5i0i4i5n4", "c5h6i5i6n5", "c6h0h7i0i6i7n6", "c7h0i0i7n7",
//				"b0b1g0l0m1", "b1b2g1l1m2", "b2b3g2l2m3", "b0b3b4g3l3m0m4", "b0b4b5g4l4m0m5", "b5b6g5l5m6", "b0b6b7g6l6m0m7", "b0b7g7l7m0",
//				"b1g0g1l0m0", "b2g1g2l1m1", "b3g2g3l2m2", "b0b4g0g3g4l3m3", "b0b5g0g4g5l4m4", "b6g5g6l5m5", "b0b7g0g6g7l6m6", "b0g0g7l7m7",
//				"b0g1l0l1m0", "b1g2l1l2m1", "b2g3l2l3m2", "b3g0g4l0l3l4m3", "b4g0g5l0l4l5m4", "b5g6l5l6m5", "b6g0g7l0l6l7m6", "b7g0l0l7m7",
//				"b0g0l1m0m1", "b1g1l2m1m2", "b2g2l3m2m3", "b3g3l0l4m0m3m4", "b4g4l0l5m0m4m5", "b5g5l6m5m6", "b6g6l0l7m0m6m7", "b7g7l0m0m7"

                // original data
//				"a1f0f1k0p0", "a2f1f2k1p1", "a3f2f3k2p2", "a0a4f0f3f4k3p3", "a0a5f0f4f5k4p4", "a6f5f6k5p5", "a0a7f0f6f7k6p6", "a0f0f7k7p7",
//				"a0f1k0k1p0", "a1f2k1k2p1", "a2f3k2k3p2", "a3f0f4k0k3k4p3", "a4f0f5k0k4k5p4", "a5f6k5k6p5", "a6f0f7k0k6k7p6", "a7f0k0k7p7",
//				"a0f0k1p0p1", "a1f1k2p1p2", "a2f2k3p2p3", "a3f3k0k4p0p3p4", "a4f4k0k5p0p4p5", "a5f5k6p5p6", "a6f6k0k7p0p6p7", "a7f7k0p0p7",
//				"a0a1f0k0p1", "a1a2f1k1p2", "a2a3f2k2p3", "a0a3a4f3k3p0p4", "a0a4a5f4k4p0p5", "a5a6f5k5p6", "a0a6a7f6k6p0p7", "a0a7f7k7p0",
//				"b1g0g1l0m0", "b2g1g2l1m1", "b3g2g3l2m2", "b0b4g0g3g4l3m3", "b0b5g0g4g5l4m4", "b6g5g6l5m5", "b0b7g0g6g7l6m6", "b0g0g7l7m7",
//				"b0g1l0l1m0", "b1g2l1l2m1", "b2g3l2l3m2", "b3g0g4l0l3l4m3", "b4g0g5l0l4l5m4", "b5g6l5l6m5", "b6g0g7l0l6l7m6", "b7g0l0l7m7",
//				"b0g0l1m0m1", "b1g1l2m1m2", "b2g2l3m2m3", "b3g3l0l4m0m3m4", "b4g4l0l5m0m4m5", "b5g5l6m5m6", "b6g6l0l7m0m6m7", "b7g7l0m0m7",
//				"b0b1g0l0m1", "b1b2g1l1m2", "b2b3g2l2m3", "b0b3b4g3l3m0m4", "b0b4b5g4l4m0m5", "b5b6g5l5m6", "b0b6b7g6l6m0m7", "b0b7g7l7m0",
//				"c1h0h1i0n0", "c2h1h2i1n1", "c3h2h3i2n2", "c0c4h0h3h4i3n3", "c0c5h0h4h5i4n4", "c6h5h6i5n5", "c0c7h0h6h7i6n6", "c0h0h7i7n7",
//				"c0h1i0i1n0", "c1h2i1i2n1", "c2h3i2i3n2", "c3h0h4i0i3i4n3", "c4h0h5i0i4i5n4", "c5h6i5i6n5", "c6h0h7i0i6i7n6", "c7h0i0i7n7",
//				"c0h0i1n0n1", "c1h1i2n1n2", "c2h2i3n2n3", "c3h3i0i4n0n3n4", "c4h4i0i5n0n4n5", "c5h5i6n5n6", "c6h6i0i7n0n6n7", "c7h7i0n0n7",
//				"c0c1h0i0n1", "c1c2h1i1n2", "c2c3h2i2n3", "c0c3c4h3i3n0n4", "c0c4c5h4i4n0n5", "c5c6h5i5n6", "c0c6c7h6i6n0n7", "c0c7h7i7n0",
//				"d1e0e1j0o0", "d2e1e2j1o1", "d3e2e3j2o2", "d0d4e0e3e4j3o3", "d0d5e0e4e5j4o4", "d6e5e6j5o5", "d0d7e0e6e7j6o6", "d0e0e7j7o7",
//				"d0e1j0j1o0", "d1e2j1j2o1", "d2e3j2j3o2", "d3e0e4j0j3j4o3", "d4e0e5j0j4j5o4", "d5e6j5j6o5", "d6e0e7j0j6j7o6", "d7e0j0j7o7",
//				"d0e0j1o0o1", "d1e1j2o1o2", "d2e2j3o2o3", "d3e3j0j4o0o3o4", "d4e4j0j5o0o4o5", "d5e5j6o5o6", "d6e6j0j7o0o6o7", "d7e7j0o0o7",
//				"d0d1e0j0o1", "d1d2e1j1o2", "d2d3e2j2o3", "d0d3d4e3j3o0o4", "d0d4d5e4j4o0o5", "d5d6e5j5o6", "d0d6d7e6j6o0o7", "d0d7e7j7o0"

                // bad data
                "a1f0f1k0p0", "a2f1f2k1p1", "a3f2f3k2p2", "a0a4f0f3f4k3p3", "a0a5f0f4f5k4p4", "a6f5f6k5p5", "a0a7f0f6f7k6p6", "a0f0f7k7p7",
                "a0f1k0k1p0", "a1f2k1k2p1", "a2f3k2k3p2", "a3f0f4k0k3k4p3", "a4f0f5k0k4k5p4", "a5f6k5k6p5", "a6f0f7k0k6k7p6", "a7f0k0k7p7",
                "a0f0k1p0p1", "a1f1k2p1p2", "a2f2k3p2p3", "a3f3k0k4p0p3p4", "a4f4k0k5p0p4p5", "a5f5k6p5p6", "a6f6k0k7p0p6p7", "a7f7k0p0p7",
                "a0a1f0k0p1", "a1a2f1k1p2", "a2a3f2k2p3", "a0a3a4f3k3p0p4", "a0a4a5f4k4p0p5", "a5a6f5k5p6", "a0a6a7f6k6p0p7", "a0a7f7k7p0",
                "b1g0g1l0m0", "b2g1g2l1m1", "b3g2g3l2m2", "b0b4g0g3g4l3m3", "b0b5g0g4g5l4m4", "b6g5g6l5m5", "b0b7g0g6g7l6m6", "b0g0g7l7m7",
                "b0g1l0l1m0", "b1g2l1l2m1", "b2g3l2l3m2", "b3g0g4l0l3l4m3", "b4g0g5l0l4l5m4", "b5g6l5l6m5", "b6g0g7l0l6l7m6", "b7g0l0l7m7",
                "b0g0l1m0m1", "b1g1l2m1m2", "b2g2l3m2m3", "b3g3l0l4m0m3m4", "b4g4l0l5m0m4m5", "b5g5l6m5m6", "b6g6l0l7m0m6m7", "b7g7l0m0m7",
                "b0b1g0l0m1", "b1b2g1l1m2", "b2b3g2l2m3", "b0b3b4g3l3m0m4", "b0b4b5g4l4m0m5", "b5b6g5l5m6", "b0b6b7g6l6m0m7", "b0b7g7l7m0",
                "c1h0h1i0n0", "c2h1h2i1n1", "c3h2h3i2n2", "c0c4h0h3h4i3n3", "c0c5h0h4h5i4n4", "c6h5h6i5n5", "c0c7h0h6h7i6n6", "c0h0h7i7n7",
                "c0h1i0i1n0", "c1h2i1i2n1", "c2h3i2i3n2", "c3h0h4i0i3i4n3", "c4h0h5i0i4i5n4", "c5h6i5i6n5", "c6h0h7i0i6i7n6", "c7h0i0i7n7",
                "c0h0i1n0n1", "c1h1i2n1n2", "c2h2i3n2n3", "c3h3i0i4n0n3n4", "c4h4i0i5n0n4n5", "c5h5i6n5n6", "c6h6i0i7n0n6n7", "c7h7i0n0n7",
                "c0c1h0i0n1", "c1c2h1i1n2", "c2c3h2i2n3", "c0c3c4h3i3n0n4", "c0c4c5h4i4n0n5", "c5c6h5i5n6", "c0c6c7h6i6n0n7", "c0c7h7i7n0",
                "d1e0e1j0o0", "d2e1e2j1o1", "d3e2e3j2o2", "d0d4e0e3e4j3o3", "d0d5e0e4e5j4o4", "d6e5e6j5o5", "d0d7e0e6e7j6o6", "d0e0e7j7o7",
                "d0e1j0j1o0", "d1e2j1j2o1", "d2e3j2j3o2", "d3e0e4j0j3j4o3", "d4e0e5j0j4j5o4", "d5e6j5j6o5", "d6e0e7j0j6j7o6", "d7e0j0j7o7",
                "d0e0j1o0o1", "d1e1j2o1o2", "d2e2j3o2o3", "d3e3j0j4o0o3o4", "d4e4j0j5o0o4o5", "d5e5j6o5o6", "d6e6j0j7o0o6o7", "d7e7j0o0o7",
                "d0d1e0j0o1", "d1d2e1j1o2", "d2d3e2j2o3", "d0d3d4e3j3o0o4", "d0d4d5e4j4o0o5", "d5d6e5j5o6", "d0d6d7e6j6o0o7", "d0d7e7j7o0"
            };

            boolean threePage = false;
            for(int v=0; v<33; v++)
            {
                if (v != 32) continue;
                String title = "whit";
                if (v < 16) title += "Input" + (v+1); else
                    if (v < 32) title += "Output" + (v-15);
                Cell myCell = Cell.newInstance(Library.getCurrent(), title+"{sch}");

                // create the input and output pins
                NodeProto pinNp = com.sun.electric.technology.technologies.Generic.tech.universalPinNode;
                NodeInst [] inputs = new NodeInst[128];
                NodeInst [] outputs = new NodeInst[128];
                NodeInst [] inputsAbove = new NodeInst[128];
                NodeInst [] outputsAbove = new NodeInst[128];
                NodeInst [] inputsBelow = new NodeInst[128];
                NodeInst [] outputsBelow = new NodeInst[128];
                for(int j=0; j<3; j++)
                {
                    if (!threePage && j != 1) continue;
                    for(int i=0; i<128; i++)
                    {
                        // the input side
                        int index = i;
                        if (j == 0) index += 128; else
                            if (j == 2) index -= 128;
                        NodeInst in = NodeInst.newInstance(pinNp, new Point2D.Double(-200.0, index*5), 1, 1, 0, myCell, null);
                        switch (j)
                        {
                            case 0: inputsAbove[i] = in;   break;
                            case 1: inputs[i] = in;        break;
                            case 2: inputsBelow[i] = in;   break;
                        }
                        if (j == 1)
                        {
                            NodeInst leftArrow = NodeInst.newInstance(com.sun.electric.technology.technologies.Artwork.tech.boxNode,
                                new Point2D.Double(-267, i*5), 10, 0, 0, myCell, null);
                            NodeInst leftArrowHead = NodeInst.newInstance(com.sun.electric.technology.technologies.Artwork.tech.arrowNode,
                                new Point2D.Double(-264, i*5), 4, 4, 0, myCell, null);
                        }

                        // the output side
                        NodeInst out = NodeInst.newInstance(pinNp, new Point2D.Double(200.0, index*5), 0, 0, 0, myCell, null);
                        switch (j)
                        {
                            case 0: outputsAbove[i] = out;   break;
                            case 1: outputs[i] = out;        break;
                            case 2: outputsBelow[i] = out;   break;
                        }
                        if (j == 1)
                        {
                            NodeInst circle = NodeInst.newInstance(com.sun.electric.technology.technologies.Artwork.tech.circleNode, new Point2D.Double(202.5, i*5), 5, 5, 0, myCell, null);
                            NodeInst horiz = NodeInst.newInstance(com.sun.electric.technology.technologies.Artwork.tech.boxNode,
                                new Point2D.Double(202.5, i*5), 4, 0, 0, myCell, null);
                            NodeInst vert = NodeInst.newInstance(com.sun.electric.technology.technologies.Artwork.tech.boxNode,
                                new Point2D.Double(202.5, i*5), 0, 4, 0, myCell, null);
                            NodeInst rightArrow = NodeInst.newInstance(com.sun.electric.technology.technologies.Artwork.tech.boxNode,
                                new Point2D.Double(210, i*5), 10, 0, 0, myCell, null);
                            NodeInst rightArrowHead = NodeInst.newInstance(com.sun.electric.technology.technologies.Artwork.tech.arrowNode,
                                new Point2D.Double(213, i*5), 4, 4, 0, myCell, null);
                        }
                    }
                }
                for(int i=0; i<16; i++)
                {
                    NodeInst inputBox = NodeInst.newInstance(com.sun.electric.technology.technologies.Artwork.tech.boxNode,
                        new Point2D.Double(-222.0, i*5*8+20-2.5), 40, 40, 0, myCell, null);
                    Variable inVar = inputBox.newVar("label", "S-box");
                    inVar.setDisplay();
                    inVar.getTextDescriptor().setRelSize(12);
                }
                NodeInst inputBox1 = NodeInst.newInstance(com.sun.electric.technology.technologies.Artwork.tech.boxNode,
                    new Point2D.Double(-252.0, 320-2.5), 20, 640, 0, myCell, null);
                Variable inVar1 = inputBox1.newVar("label", "Keying");
                inVar1.setDisplay();
                inVar1.getTextDescriptor().setRotation(TextDescriptor.Rotation.ROT90);
                inVar1.getTextDescriptor().setRelSize(15);
                NodeInst inputBox2 = NodeInst.newInstance(com.sun.electric.technology.technologies.Artwork.tech.boxNode,
                    new Point2D.Double(-282.0, 320-2.5), 20, 640, 0, myCell, null);
                Variable inVar2 = inputBox2.newVar("label", "Input");
                inVar2.setDisplay();
                inVar2.getTextDescriptor().setRotation(TextDescriptor.Rotation.ROT90);
                inVar2.getTextDescriptor().setRelSize(15);

                NodeInst outputBox = NodeInst.newInstance(com.sun.electric.technology.technologies.Artwork.tech.boxNode,
                    new Point2D.Double(225.0, 320-2.5), 20, 640, 0, myCell, null);
                Variable inVar3 = outputBox.newVar("label", "Output");
                inVar3.setDisplay();
                inVar3.getTextDescriptor().setRotation(TextDescriptor.Rotation.ROT90);
                inVar3.getTextDescriptor().setRelSize(15);

                NodeInst titleBox = NodeInst.newInstance(com.sun.electric.technology.technologies.Generic.tech.invisiblePinNode,
                    new Point2D.Double(0, 670), 0, 0, 0, myCell, null);
                Variable inVar4 = titleBox.newVar("label", "One Round of AES");
                inVar4.setDisplay();
                inVar4.getTextDescriptor().setRelSize(24);

                // wire them together
                ArcProto wire = com.sun.electric.technology.technologies.Generic.tech.universal_arc;
                for(int i=0; i<theStrings.length; i++)
                {
                    int len = theStrings[i].length();
                    for(int j=0; j<len; j+=2)
                    {
                        char letter = theStrings[i].charAt(j);
                        char number = theStrings[i].charAt(j+1);
                        int index = (letter - 'a')*8 + (number - '0');
                        if (v < 16)
                        {
                            // only interested in the proper letter
                            if (v + 'a' != letter) continue;
                        } else if (v < 32)
                        {
                            if (i/8 != v-16) continue;
                        }

                        // handle wrapping
                        if (threePage && Math.abs(index - i) > 64)
                        {
                            if (i < 64)
                            {
                                PortInst inPort = inputsBelow[index].getOnlyPortInst();
                                PortInst outPort = outputs[i].getOnlyPortInst();
                                ArcInst.newInstance(wire, 0, inPort, outPort, null);
                                inPort = inputs[index].getOnlyPortInst();
                                outPort = outputsAbove[i].getOnlyPortInst();
                                ArcInst.newInstance(wire, 0, inPort, outPort, null);
                            } else
                            {
                                PortInst inPort = inputsAbove[index].getOnlyPortInst();
                                PortInst outPort = outputs[i].getOnlyPortInst();
                                ArcInst.newInstance(wire, 0, inPort, outPort, null);
                                inPort = inputs[index].getOnlyPortInst();
                                outPort = outputsBelow[i].getOnlyPortInst();
                                ArcInst.newInstance(wire, 0, inPort, outPort, null);
                            }
                        } else
                        {
                            PortInst inPort = inputs[index].getOnlyPortInst();
                            PortInst outPort = outputs[i].getOnlyPortInst();
                            ArcInst.newInstance(wire, 0, inPort, outPort, null);
                        }
                    }
                }

                // display the full drawing
                if (v == 32) WindowFrame.createEditWindow(myCell);
            }
            return true;
        }
    }


}
